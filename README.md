# Role Builder

**Motor y editor de plataformas 2D, 100% aplicación Android.** Carga tu propia ROM
de Super Mario World y la app **la lee**: gráficos, bloques, niveles, colisión,
físicas, enemigos, música y sonidos quedan a tu disposición para construir juegos
nuevos o modificar SMW. Todo en el móvil o la tablet, sin PC y sin motores externos:
editor y motor son la misma app.

Del arranque salen **tres puertas**:

- 🎮 **Jugar Super Mario World** — el juego, desde tu cartucho: pantalla de título
  real, overworld y tres ranuras de guardado en disco.
- 🍄 **Platform Builder** — el editor de verdad. Importa niveles de la ROM como
  mapas y tilesets **tuyos**, editables, y mezcla material de varios para construir
  los propios.
- 🗡️ **Role Builder** — editor de ARPG cenital estilo RPG Maker, con eventos y
  combate en el mapa. **En pausa**: funciona y se mantiene, pero el desarrollo vive
  hoy en el lado de plataformas.

> **Ni un byte de Nintendo en este repositorio.** Los gráficos, la música y los
> sonidos no se empaquetan: se **hornean en tu dispositivo** desde tu propia ROM la
> primera vez que entras. Aquí solo viven las coordenadas y el cómo decodificarlas.

## Leer la ROM

Es lo que hace distinta a esta app: no trae assets imitando a SMW, **lee el juego
real** y te lo sirve como material de trabajo.

- **Decodificador de gráficos de SNES**: formato planar 2/3/4/8bpp y paletas CGRAM,
  con **autodetección de zonas gráficas** para no ir adivinando offsets y
  **descompresión conectable** (LC_LZ2, verificada con SMW) juzgada por coherencia.
- **El color se elige solo**: un **emparejador de paletas** puntúa cada paleta CGRAM
  detectada contra el gráfico concreto —coherencia de tono entre índices vecinos, con
  sub-paletas de 8/4 colores— y aplica la que mejor encaja; el desplegable manual
  queda solo para corregir. Hay **vista en escala de grises** para reconocer la
  *forma* de un tile cuando aún no sabes su paleta, y un **atlas de sprites** que
  agrupa bloques de 8×8 en sprites enteros (2×2 → 16×16, 4×4 → 32×32), porque muchos
  juegos guardan un sprite grande partido en varios 8×8 consecutivos.
- **De Super Mario World se lee mucho más que los gráficos**, y cada offset está
  verificado contra el desensamblado y la decompilación:

  | Qué | De dónde sale |
  |---|---|
  | Niveles enteros (objetos de Layer 1, fondos de Layer 2, bloques Map16) | parser de objetos + atlas Map16 |
  | Colisión real por celda (sólidos, cuestas, bordes de un sentido, pinchos) | port de la rutina del juego |
  | Físicas del jugador (salto, gravedad, topes) | tablas del banco `$00` |
  | Mario con su sprite y su paleta reales | GFX32 + paleta de jugador de la CGRAM |
  | Enemigos con sprite y color reales | tabla OAM del banco `$01` + tweaker `$166E` |
  | Monedas y bloques `?` con su comportamiento | clasificación real del *block code* |
  | Música | motor N-SPC y S-DSP portados |
  | Efectos de sonido | muestras BRR de la ROM |
  | Overworld, nombres de nivel, salidas y tuberías | tablas propias de cada uno |

Guía paso a paso para principiantes en
[`docs/GUIA_EXTRACTOR_SNES.md`](docs/GUIA_EXTRACTOR_SNES.md); el inventario técnico
completo de qué está hecho y qué falta, en
[`docs/INVENTARIO_SMW.md`](docs/INVENTARIO_SMW.md).

## Platform Builder

El editor donde vive el desarrollo hoy.

- **Las dos capas de verdad**: Capa 1 (primer plano jugable) y Capa 2 (fondo) tienen
  su sector en el menú radial, su pincel propio y una barra para ocultarlas o enfocar
  la que estás editando; pintar y borrar actúan sobre la capa activa.
- **Banco de assets entre niveles**: coge teselas de *cualquier* nivel del proyecto
  —con su colisión, su acción de bloque y su animación— y mezcla material de varios
  niveles de la ROM para construir el tuyo. Cada nivel nuevo se lleva **su copia** de
  los gráficos, así que retocar uno no le cambia la paleta al otro.
- **Herramientas de construcción de verdad**: relleno por rectángulo (arrastrando,
  con vista previa) y cubo, **deshacer/rehacer** de 40 pasos agrupados por gesto, y un
  **marco de selección** para copiar, cortar, pegar, mover, duplicar al lado y voltear
  trozos del nivel, con alcance por capas. Los sellos se guardan y se repiten.
- **Tuberías que no se rompen al mover**: si arrastras un trozo con warps dentro, el
  editor pregunta y reapunta bocas y destinos en vez de dejarlos descolgados.
- **Editor de bloques Map16** y colisión por tesela.
- **Dos formas de probar**: *▶ Jugar un nivel* monta el motor leyendo la ROM en vivo;
  *Crear mapa* convierte el nivel en mapa+tileset del proyecto, ya editable.
- Exporta el proyecto como .zip para compartirlo, e importa los de otros.

## Motor (runtime)

- Renderizado **OpenGL ES 3.0** con batch de sprites propio, cámara que sigue al
  jugador, orden por profundidad y filtrado pixel-art.
- Plataformas a 60 fps: AABB, cuestas, enemigos, monedas, bloques `?`, powerup
  (seta → crecer → encoger, con invulnerabilidad) y warps.
- Controles táctiles, audio con los SFX y la música sacados de la ROM.
- Partida guardada en disco con tres ranuras, y overworld con el camino que se abre.

## Role Builder (ARPG) — en pausa

El origen del proyecto y de su nombre. Sigue funcionando y se mantiene, pero el
desarrollo está parado:

- Editor de mapas táctil (2 capas, hasta 200×200), eventos tipo RPG Maker con páginas,
  condiciones y comandos visuales, y base de datos de actores, enemigos, objetos y
  habilidades.
- Runtime con movimiento libre de 8 direcciones, combate de acción con proyectiles e
  IA, intérprete de eventos, inventario, 3 ranuras de guardado y **música chiptune
  procedural** (5 pistas sintetizadas en tiempo real, cero assets de audio).

## Arquitectura

```
core/   Kotlin JVM puro, sin dependencias de Android
  snes/     ~19.000 líneas: el grueso del proyecto. Decodificador planar
            (2/3/4/8bpp), cabecera y paletas CGRAM, hojas ARGB, autodetección
            por coherencia y descompresión conectable (compression/: LC_LZ2).
            De SMW: niveles, colisión, físicas, enemigos, overworld, música
            N-SPC y SFX BRR, con cada offset verificado contra el desensamblado
  engine/
    platformer/  Motor de plataformas a 60 fps: AABB, cuestas, enemigos,
            monedas, bloques ?, powerups y warps
    (raíz)  RpgEngine.tick(dt) del ARPG: movimiento, combate, intérprete — en pausa
  model/    Datos serializables del proyecto (mapas, tilesets, base de datos,
            eventos y comandos como jerarquía sellada)
  io/       Carga/guardado de proyectos y partidas (kotlinx.serialization)
  tools/    CLI de escritorio: extractor de assets y horneado de la plantilla

app/    Aplicación Android (solo UI y render)
  editor/platform/  Platform Builder: lienzo, menú radial, banco de assets, Map16
  editor/snes/      Diálogo de importación SNES: galería, crear mapa, jugar nivel
  editor/           Editor ARPG (mapas, eventos, base de datos) — en pausa
  player/   GLSurfaceView + renderer GLES 3.0: platformer, overworld, título y
            el runtime del ARPG
  project/  Gestor de proyectos en filesDir
```

La regla de oro: **toda la lógica del juego vive en `core`** y se prueba con tests
JVM rápidos (690: serialización, movimiento, intérprete, combate, físicas del
platformer, lectura de la ROM de SMW y el proyecto demo completo). `app` solo
dibuja el estado del motor y le pasa el input.

La cifra de arriba se queda vieja enseguida; para sacarla de verdad, `./gradlew
:core:test` y mirar el informe. Lo que sí conviene saber es cómo está repartida:
`core` tiene ~15.500 líneas de test para ~26.700 de código, y `app` solo ~380 para
~16.700. Ese desequilibrio está medido y explicado en [AUDITORIA.md](AUDITORIA.md).

## Formato de proyecto

Los dos modos comparten esqueleto; el campo `mode` de `project.json` (`PLATFORMER`
o `ARPG`) decide qué editor y qué motor se abren.

```
projects/<id>/
├── project.json      # nombre, modo, inicio, switches/variables, lista de mapas
├── database.json     # tilesets y, en ARPG, actores/enemigos/objetos/habilidades
├── maps/map_1.json   # capas de tiles, colisión, warps, spawns y eventos
├── images/*.png      # atlas de teselas (rejilla 16px); cada nivel, el suyo
└── saves/slot1.json  # partida guardada
```

Todo es JSON legible: los proyectos se pueden compartir, versionar o editar a mano.

Los assets del runtime —Mario, el atlas de enemigos, los SFX y la música— **no**
viven en el proyecto: se hornean desde tu ROM al almacenamiento privado de la app
(`filesDir/smw_assets/`) y se rehornean en cada dispositivo desde la ROM de su
dueño. Por eso no viajan en el .zip.

**Pero el atlas de un nivel importado sí es un PNG dentro de `images/`**, y ese sí
va en el .zip. Dicho claro: compartir un proyecto que importó niveles de SMW es
compartir gráficos derivados de la ROM. Que en el repositorio no haya un byte de
Nintendo no convierte en distribuible lo que la app produce en tu dispositivo.

## Compilar

Requisitos: Android Studio (o un Android SDK instalado). El módulo `:app` se
activa automáticamente cuando hay SDK disponible (`ANDROID_HOME` o
`local.properties`); sin SDK solo se carga `:core`, útil para CI.

```bash
# Tests del motor (no requieren Android SDK)
./gradlew :core:test

# APK de depuración (requiere Android SDK)
./gradlew :app:assembleDebug

# Regenerar los assets de la plantilla (tileset y sprites procedurales + JSON)
./gradlew :core:generateDefaultAssets

# Extraer una hoja de tiles desde una ROM de SNES (PNG + Tileset JSON)
./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out out --offset 0x2000 --format 4bpp --palette-offset 0x100"
# Vista en escala de grises: reconoce la FORMA de los tiles sin conocer la paleta
./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out out --offset 0x2000 --format 4bpp --grayscale"
# Atlas de sprites: agrupa bloques de tiles en sprites enteros (2x2 = 16×16, 4x4 = 32×32)
./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out out --offset 0x2000 --format 4bpp --sprite 2x2"
# Autodetección de bpp: el programa adivina 2/3/4/8bpp por sí solo
./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out out --offset 0x2000 --format auto"
# o probar con una ROM de ejemplo generada al vuelo (sin material con copyright):
./gradlew :core:extractSnesTileset --args="--demo out"
```

- minSdk 26 (Android 8.0), target 34, OpenGL ES 3.0.
- El juego se ejecuta en apaisado; el editor en cualquier orientación.
- CI en GitHub Actions: tests de `core` con cobertura (kover), **mutation testing**
  (pitest: muta el código y re-ejecuta la suite, para cazar tests que pasan aunque
  el código esté roto), análisis estático (detekt, con SARIF a Code Scanning),
  tests JVM de `app` y compilación del APK de depuración. Hay además workflows de
  CodeQL y un informe de deuda técnica. Cada push a `main` refresca la release
  **`apk-latest`**; empujar una etiqueta `v*` congela una release de esa versión.
  Lo que CI **no** puede comprobar es la extracción desde la ROM: en el
  repositorio no hay ROM, así que esas sondas se saltan solas. Ver
  [AUDITORIA.md](AUDITORIA.md).

## Publica tu juego como app independiente

El mismo APK puede convertirse en *tu* juego, sin editor:

1. Exporta tu proyecto como .zip desde la app.
2. Descomprímelo en `app/src/main/assets/standalone_game/` (debe quedar
   `standalone_game/project.json`).
3. Cambia si quieres el `applicationId` y el nombre en `app/build.gradle.kts` y
   `strings.xml`, y compila (`./gradlew :app:assembleRelease`).

Al detectar un juego embebido, la app arranca directamente en su pantalla de
título: el APK resultante es un juego instalable e independiente.

## Cómo se usa

Necesitas una ROM de Super Mario World —un volcado de **tu propio cartucho**—; la
app la pide una vez y la recuerda. Las ROMs nunca se versionan aquí.

**Hacer un nivel**

1. Arranca en **🍄 Platform Builder** y crea un proyecto con **+**.
2. Abre el **diálogo de importación SNES** y carga tu ROM.
3. Busca niveles importables y pulsa **Crear mapa**: ese nivel pasa a ser un mapa y
   un tileset tuyos, con su colisión, sus bloques y sus enemigos.
4. Edítalo. Con **Assets** te traes teselas de otros niveles para mezclar material.
5. Pulsa **▶** para probarlo. Y si solo quieres ver un nivel original tal cual,
   *▶ Jugar un nivel* lo monta leyendo la ROM en vivo, sin tocar tu proyecto.

**Jugar**

Desde **🎮 Jugar Super Mario World**: título, overworld y tres ranuras que sí
guardan en disco, separadas de lo que pruebas en el editor.
