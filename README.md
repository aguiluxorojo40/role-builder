# Role Builder

**Motor de RPG estilo RPG Maker, 100% aplicación Android.** Crea tu RPG de acción
2D (mapas de tiles, eventos visuales, combate en el mapa estilo Zelda) directamente
en tu móvil o tablet, y juégalo en el mismo dispositivo. Sin PC, sin motores
externos: editor y motor son la misma app.

## Características

**Editor (en el dispositivo)**
- Gestor de proyectos: cada juego es una carpeta con JSON + PNG, creada desde una
  plantilla jugable (pradera con NPC, cofre, enemigos y una cabaña con interior).
- Editor de mapas táctil: pinta tiles en 2 capas con paleta, pan/zoom con dos dedos,
  redimensiona mapas (hasta 200×200), coloca eventos, enemigos y el punto de inicio.
- Editor de eventos tipo RPG Maker: páginas con condiciones (switches, self-switches,
  variables, objetos), sprite y disparador (botón de acción, contacto, automático,
  paralelo), y comandos visuales: mostrar texto, elecciones con ramas, condicionales,
  teletransporte, rutas de movimiento, dar/quitar objetos, cambiar HP, sonidos…
- Base de datos: actores, enemigos (comportamiento, visión, drops), objetos,
  habilidades (melee/proyectil) y colisión del tileset.
- Importación de imágenes PNG propias (tilesets y hojas de personaje 3×4).
- **Extracción de assets de ROMs de Super Nintendo**: carga una ROM (.smc/.sfc)
  y decodifica sus gráficos (2bpp/3bpp/4bpp/8bpp) y paletas CGRAM directamente en el
  dispositivo para convertirlos en tilesets del proyecto, con **autodetección de
  zonas gráficas** para no adivinar offsets y **descompresión conectable**
  (LC_LZ2, verificado con Super Mario World) juzgada por coherencia. El color se
  elige solo: un **emparejador de paletas** puntúa cada paleta CGRAM detectada
  contra el gráfico concreto (coherencia de tono entre índices vecinos, con
  sub-paletas de 8/4 colores) y aplica la que mejor encaja, dejando el desplegable
  manual solo para corregir. Incluye una
  **vista en escala de grises** para reconocer la *forma* de los tiles aunque aún
  no conozcas la paleta (con la paleta equivocada, unos gráficos correctos parecen
  ruido de colores) y un **atlas de sprites** que agrupa bloques de tiles de 8×8
  en sprites enteros (2×2 → 16×16, 4×4 → 32×32), porque muchos juegos guardan un
  sprite grande como varios 8×8 consecutivos que, sin agrupar, salen partidos.
  Guía paso a paso para principiantes en
  [`docs/GUIA_EXTRACTOR_SNES.md`](docs/GUIA_EXTRACTOR_SNES.md).
- Exporta tu juego como .zip para compartirlo e importa los de otros.
- Botón ▶ para probar el juego al instante.

**Motor (runtime)**
- Pantalla de título (Nueva partida / Continuar) y **3 ranuras de guardado** con fecha.
- **Música de fondo procedural**: 5 pistas chiptune (título, campo, aldea, mazmorra,
  batalla) sintetizadas en tiempo real; se asignan por mapa o con comandos de evento.
- Renderizado OpenGL ES 3.0 con batch de sprites propio, cámara que sigue al
  jugador, orden de dibujado por profundidad y filtrado pixel-art.
- Movimiento libre de 8 direcciones con colisión por casillas y deslizamiento.
- Combate de acción: golpe cuerpo a cuerpo con retroceso, proyectiles,
  invulnerabilidad temporal, IA de enemigos (quieto / deambula / persigue),
  daño por contacto, drops y game over.
- Intérprete de eventos paso a paso con mensajes, elecciones y cinemáticas.
- Controles táctiles (joystick virtual + botones A/B), HUD de corazones,
  menú de pausa con inventario, guardado y carga de partida.
- Efectos de sonido sintetizados en tiempo de ejecución (cero assets de audio).

## Arquitectura

```
core/   Kotlin JVM puro, sin dependencias de Android
  model/    Datos serializables del proyecto (mapas, tilesets, base de datos,
            eventos y comandos como jerarquía sellada)
  engine/   RpgEngine.tick(dt): movimiento, colisiones, combate, intérprete
  io/       Carga/guardado de proyectos y partidas (kotlinx.serialization)
  snes/     Extracción de assets de ROMs de SNES: decodificador planar
            (2/4/8bpp), lectura de cabecera y paletas, composición de hojas de
            tiles ARGB, autodetección de gráficos por coherencia y framework de
            descompresión conectable (compression/: LC_LZ2 + autodetección)

app/    Aplicación Android (solo UI y render)
  editor/   Editor Compose: mapas, eventos, base de datos, ajustes
  player/   GLSurfaceView + renderer GLES 3.0, controles, HUD, sonido
  project/  Gestor de proyectos en filesDir
```

La regla de oro: **toda la lógica del juego vive en `core`** y se prueba con tests
JVM rápidos (535: serialización, movimiento, intérprete, combate, extracción de
assets de SNES y el proyecto demo completo). `app` solo dibuja el estado del motor
y le pasa el input.

La cifra de arriba se queda vieja enseguida; para sacarla de verdad, `./gradlew
:core:test` y mirar el informe. Lo que sí conviene saber es cómo está repartida:
`core` tiene ~10.600 líneas de test para ~23.600 de código, y `app` solo ~160 para
~13.900. Ese desequilibrio está medido y explicado en [AUDITORIA.md](AUDITORIA.md).

## Formato de proyecto

```
projects/<id>/
├── project.json      # nombre, inicio, switches/variables, lista de mapas
├── database.json     # actores, enemigos, objetos, habilidades, tilesets
├── maps/map_1.json   # capas de tiles, eventos, spawns
├── images/*.png      # tileset (rejilla 16px) y personajes (3 cols × 4 filas)
└── saves/slot1.json  # partida guardada
```

Todo es JSON legible: los proyectos se pueden compartir, versionar o editar a mano.

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
- CI en GitHub Actions: tests de `core`, tests JVM de `app`, análisis estático
  (detekt) y compilación del APK de depuración (descargable como artefacto del
  workflow). Lo que CI **no** puede comprobar es la extracción desde la ROM: en el
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

1. Crea un proyecto con **+** (incluye el juego de ejemplo).
2. En **Mapa**, pinta el terreno; con la herramienta **Evento** toca una casilla
   para crear un NPC, cofre o puerta; con **Enemigo** coloca monstruos; con
   **Inicio** fija dónde aparece el jugador.
3. En **Base de datos** ajusta enemigos, objetos y habilidades.
4. Pulsa **▶** y juega: joystick para moverte, **A** habla/ataca, **B** dispara.
