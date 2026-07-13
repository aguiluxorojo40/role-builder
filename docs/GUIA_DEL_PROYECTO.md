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

- **`claude/snes-sprite-color-automation-918asc`** ← rama de trabajo con TODO
  el pipeline SMW (tras fusionar la hermana `…-59ybqy`).
- `claude/work-continuation-thread-5jf5kz` — línea HD-2D/diorama del editor ARPG.

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
```

Sin ordenador: cada push compila en GitHub Actions y publica el APK en la
release **`apk-snes-latest`** (o como artefacto del workflow).

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

## 8. Historia corta (para orientarse en el log)

1. Extractor genérico de SNES (tiles, paletas, escaneo, descompresión LC_LZ2).
2. "Recetas" para SMW: galería con color real (CGRAM ensamblada como el juego).
3. Nivel jugable: colisión + físicas + inicio reales → motor de plataformas.
4. Enemigos: lista de sprites del nivel + tweakers + tabla OAM → sprites reales.
5. Audio: SFX BRR + música N-SPC/S-DSP portados.
6. Mario real (GFX32), teselas animadas, bloques `?`/monedas, warps (core).
7. Merge de las dos ramas hermanas + monedas/powerup jugables en ambas rutas.
