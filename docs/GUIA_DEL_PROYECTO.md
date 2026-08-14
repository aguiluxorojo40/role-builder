# Guía del proyecto: cómo llegar hasta aquí

Cómo montar el entorno, compilar, probar y jugar lo que este repositorio sabe
hacer con Super Mario World. El **estado** de cada pieza está en
[INVENTARIO_SMW.md](INVENTARIO_SMW.md); el manual de usuario del extractor, en
[GUIA_EXTRACTOR_SNES.md](GUIA_EXTRACTOR_SNES.md).

## 1. El mapa del repositorio

```
core/   Kotlin JVM puro (sin Android): TODA la lógica
  snes/     Lectura de la ROM: gráficos, paletas, niveles, colisión, físicas,
            enemigos, música y SFX (cada offset verificado contra el
            desensamblado/decompilación de SMW)
  engine/platformer/  Motor de plataformas (tick a 60 fps, AABB, enemigos,
            monedas, bloques ?, powerups, warps)
  tools/    CLI de escritorio (extractor y horneado de assets)
app/    Solo UI y render Android (Compose + OpenGL ES 3.0)
  player/   PlatformerActivity/Renderer/Audio/Music
  editor/snes/  Diálogo de importación SNES (galería, crear mapa, jugar nivel)
```

Regla de oro: la lógica vive en `core` y se prueba con tests JVM rápidos;
`app` solo dibuja el estado del motor y le pasa el input.

## 2. Ramas

- **`main`** ← única rama de trabajo. Contiene el pipeline SMW completo y la
  línea HD-2D/diorama del editor ARPG, ya fusionados.
- Las ramas `claude/*` que quedan son históricas: cuelgan de una línea de
  commits paralela, anterior, que `main` ya supersede. No trabajes sobre ellas.

### Una excepción: `claude/hd2d-3d-fase10` está APARCADA, no olvidada

Es la única rama histórica con trabajo que **no está en `main`**: un renderer 3D
en perspectiva real con z-buffer para el diorama del ARPG (commit `230b79c`).
Sustituye la proyección ortográfica con keystone falso por una cámara de verdad.

Lo que trae, y es código sólido: `Camera3D` con `lookAt` y perspectiva, distancia
derivada del FOV, ejes de cámara sacados de las filas de la matriz de vista para
orientar billboards, `discard` del alfa para que los fondos transparentes no
escriban profundidad, y un renderbuffer de profundidad en el FBO de post-procesado.

**Por qué no se ha traído** (revisado en agosto de 2026):

1. **Sería una regresión.** No dibuja parallax ni clima —su propio commit los deja
   "pendientes para M2"— y `main` sí los dibuja. Ganas perspectiva, pierdes la
   Fase 8 y el tiempo atmosférico.
2. **Rompería el platformer.** Reescribe `SpriteBatch` para emitir quads en el
   plano del suelo (Y=0) y borra `Camera2D`; `PlatformerRenderer` usa ambos. Nota:
   `draw()` conserva la firma exacta, así que **compilaría** y fallaría solo al
   dibujar — el nivel saldría tumbado en el suelo visto en picado.
3. **El ARPG está en pausa**, que es lo único a lo que sirve este renderer.

**Si algún día se retoma**, el trabajo está a favor: los cuatro ficheros del
renderer (`GameRenderer`, `SpriteBatch`, `PostProcessor`, `Camera2D`) siguen
**idénticos byte a byte** entre el punto de fork y `main`, porque el renderer del
ARPG lleva congelado desde el 4 de julio de 2026. El camino sería añadir
`Camera3D` y una variante `SpriteBatch3D` **al lado** de las 2D en vez de
sustituirlas, y recuperar parallax y clima antes de darlo por bueno.

No la borres: es el único sitio donde vive ese trabajo.

## 3. Requisitos y ROM

- JDK 17+ y (solo para el APK) un Android SDK. Sin SDK, `:app` se omite solo y
  `:core` compila y testea igualmente.
- Una ROM de SMW (volcado de tu propio cartucho), p. ej. `smw.sfc` en la raíz
  del repo. **Las ROMs nunca se versionan** (`*.sfc`/`*.smc` están en
  `.gitignore`); los tests y la app funcionan sin ROM usando assets horneados.

## 4. Compilar y probar

```bash
# Tests del motor y de todo el pipeline SNES (no requieren Android SDK ni ROM)
./gradlew :core:test

# APK de depuración (requiere Android SDK)
./gradlew :app:assembleDebug

# ¿Los tests detectan código roto? Muta el motor y re-ejecuta la suite
./gradlew :core:pitest
```

Sin ordenador: cada push a `main` compila en GitHub Actions y publica el APK en
la release **`apk-latest`** (o como artefacto del workflow, en cualquier rama).
Para congelar una versión, empuja su etiqueta y el CI publica una release aparte
que ya no se sobrescribe:

```bash
git tag v0.13.0 && git push origin v0.13.0
```

## 5. El extractor por línea de comandos

Todos los modos comparten binario: `./gradlew :core:extractSnesTileset --args="…"`.
Los más útiles con una ROM de SMW (rutas absolutas):

```bash
--rom smw.sfc --recipe                 # galería completa: escenas, fondos, tilesets, sprites
--rom smw.sfc --collision --level 0x106  # colisión del nivel (ASCII + máscara PNG)
--rom smw.sfc --physics                # tablas de físicas del jugador
--rom smw.sfc --mario                  # hoja de Mario (GFX32) coloreada → mario.png
--rom smw.sfc --enemies                # atlas de enemigos del catálogo → enemies.png
--rom smw.sfc --level-info --level 0x106
--rom smw.sfc --sprite-behavior --id 0x0F
--rom smw.sfc --play-sim --level 0x106 # simula el nivel en el motor, sin UI
```

`--mario` y `--enemies` son también el HORNEADO oficial de
`app/src/main/assets/sprites/` — si cambias el catálogo de enemigos, regenera
`enemies.png` con `--enemies` para que atlas y código no se desincronicen.

## 6. Jugar un nivel de SMW en el móvil

1. Instala el APK y abre un proyecto en modo **Plataformas**.
2. En el diálogo de **importación SNES**, carga tu ROM.
3. Dos caminos:
   - **▶ Jugar un nivel** (ruta ROM directa): monta el motor leyendo la ROM en
     vivo — colisión, físicas, inicio, enemigos, monedas, `?`, seta y sonido.
   - **Buscar niveles importables → Crear mapa** (ruta proyecto): el nivel
     queda como mapa+tileset TUYO (editable en el editor) y se juega con los
     gráficos Map16 reales.

## 7. Cómo se verifica cada pieza (el método)

Cada dato que se lee de la ROM sigue la misma disciplina:

1. **Localizar la verdad** en la decompilación (`snesrev/smw`) o el
   desensamblado (SMWDisX): rutina y dirección concretas.
2. **Portar a Kotlin puro** en `core/snes/` con el offset PC documentado en un
   comentario (`$05:B999 → PC 0x2B999`, delta de cabecera SMC aparte).
3. **Testear sin ROM** (datos sintéticos plantados) en `core/src/test/` y,
   cuando hay ROM a mano, verificar visualmente con el modo CLI correspondiente.
4. Si el offset no está confirmado se marca `[PROBABLE]` y el consumidor lleva
   un gate de cordura (mejor caer a un fallback honesto que pintar basura).
5. **Contar solo lo que existe.** La ROM tiene 512 huecos de nivel, pero solo son
   niveles los que el juego referencia: **92** con casilla en el mapa del mundo y
   **215** alcanzables contando sus sub-niveles ([`SmwLevelSet`], derivado de la
   ROM, no una lista a mano). Cualquier recuento, medida de cobertura o listado
   de la UI usa ese conjunto; medir sobre los 512 infla el resultado y llena la
   UI de entradas muertas. Y ojo: el famoso **96 son las SALIDAS** ("96 EXITS"),
   no los niveles — hay niveles con dos salidas (normal + secreta) y otros con
   ninguna (Yoshi's House) o que acaban por jefe/interruptor.

## 8. Historia corta (para orientarse en el log)

1. Extractor genérico de SNES (tiles, paletas, escaneo, descompresión LC_LZ2).
2. "Recetas" para SMW: galería con color real (CGRAM ensamblada como el juego).
3. Nivel jugable: colisión + físicas + inicio reales → motor de plataformas.
4. Enemigos: lista de sprites del nivel + tweakers + tabla OAM → sprites reales.
5. Audio: SFX BRR + música N-SPC/S-DSP portados.
6. Mario real (GFX32), teselas animadas, bloques `?`/monedas, warps (core).
7. Merge de las dos ramas hermanas + monedas/powerup jugables en ambas rutas.
