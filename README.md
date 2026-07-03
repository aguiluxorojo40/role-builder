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
- Botón ▶ para probar el juego al instante.

**Motor (runtime)**
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

app/    Aplicación Android (solo UI y render)
  editor/   Editor Compose: mapas, eventos, base de datos, ajustes
  player/   GLSurfaceView + renderer GLES 3.0, controles, HUD, sonido
  project/  Gestor de proyectos en filesDir
```

La regla de oro: **toda la lógica del juego vive en `core`** y se prueba con tests
JVM rápidos (34 tests: serialización, movimiento, intérprete, combate y el proyecto
demo completo). `app` solo dibuja el estado del motor y le pasa el input.

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
```

- minSdk 26 (Android 8.0), target 34, OpenGL ES 3.0.
- El juego se ejecuta en apaisado; el editor en cualquier orientación.

## Cómo se usa

1. Crea un proyecto con **+** (incluye el juego de ejemplo).
2. En **Mapa**, pinta el terreno; con la herramienta **Evento** toca una casilla
   para crear un NPC, cofre o puerta; con **Enemigo** coloca monstruos; con
   **Inicio** fija dónde aparece el jugador.
3. En **Base de datos** ajusta enemigos, objetos y habilidades.
4. Pulsa **▶** y juega: joystick para moverte, **A** habla/ataca, **B** dispara.
