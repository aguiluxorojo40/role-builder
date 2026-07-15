# Changelog

Registro de lo hecho en **Role Builder** (motor RPG/plataformas 100% Android +
pipeline que extrae contenido REAL de Super Mario World de una ROM). Formato
inspirado en [Keep a Changelog](https://keepachangelog.com/es/); versionado
semántico aproximado.

El mapa técnico completo de qué está hecho y qué falta está en
[`docs/INVENTARIO_SMW.md`](docs/INVENTARIO_SMW.md); la guía de uso y compilación
en [`docs/GUIA_DEL_PROYECTO.md`](docs/GUIA_DEL_PROYECTO.md).

---

## [0.12.0] — 2026-07-15 — Audio fiel, casa fantasma y nombres reales

### Corregido
- **Audio: bug del gain del DSP** que reventaba la música. El envolvente `gain`
  del S-DSP es un uint16: al decrecer por debajo de 0 se desborda (~0xFFF8 > 0x7FF)
  y eso apaga la nota. En Kotlin era un `Int` con signo, así que en los estados
  que decrecen (release y gain-mode 0) la nota nunca se silenciaba y su volumen
  crecía en negativo → zumbido digital que tapaba la canción al soltar la primera
  nota. Arreglado enmascarando a 16 bits. Verificado: 0% de saturación y RMS
  estable ~500-800 (antes se disparaba a miles).

### Añadido
- **Casa fantasma: de 16 a 49/56 niveles reconstruidos al 100%.** Port fiel (1:1
  contra SMWDisX) de los objetos Layer 1 que faltaban: estándar `0x20`, `0x31`
  (paneles/ventanas con patrón alternante), `0x32`, `0x3B–0x3F` (vigas y losas), y
  extendidos `0x57–0x5E` (detalles), `0x64/0x65` (bloques 2×2) y `0x49` (el mural
  6×13 de la pared). La Casa Fantasma #1 (nivel 0x4) se reconstruye entera. Global
  Layer 1: 419/477 niveles al 100%.
- **Nombres reales de niveles y sprites** (traídos de la rama `…-wk1dwx`): el
  diálogo de importación y el mapa muestran "YOSHI'S ISLAND 1" o "#1 IGGY'S
  CASTLE" (rutina `UpdateLevelName` del banco $04, verificada 1:1 contra la ROM),
  y `SmwSpriteNames` da el nombre canónico de cada enemigo por id.
- **Herramienta `--scene`** (extractor CLI): renderiza un nivel tal como se importa
  a la app (Layer 2 + Layer 1) a un PNG, para verlo sin abrir la app.
- **Layer 2 (fondo) importada como capa editable**: el fondo real del nivel se
  trocea en teselas y se coloca debajo del primer plano, visible y editable.

### Limitaciones conocidas
- Quedan 7 niveles de casa fantasma con cola de objetos que salen 1-4 veces
  (`ext:97`, `std:2E/30`, `ext:8A-8D/62/63/85`).
- El fondo (Layer 2) scrollea 1:1, sin paralaje.

---

## [0.11.0] — 2026-07-14 — Pipeline SMW → nivel jugable

Hito de la línea SNES (rama `claude/snes-sprite-color-automation-918asc`): un
nivel real de la ROM de SMW se importa y se juega con su tacto, su color, sus
enemigos, su audio y sus warps. Recoge TODO el trabajo acumulado sobre la base
del motor (ver 0.1.0).

### Añadido

**Extracción de niveles**
- Importar un nivel de SMW como **mapa jugable**: atlas Map16 + tilemap +
  colisión real por celda + acciones + enemigos + teselas animadas
  (`extractSmwLevelAsMap`).
- **Layer 2 (fondo) importada como capa editable**: se rasteriza el fondo real
  del nivel y se trocea en teselas 16×16 distintas colocadas DEBAJO del primer
  plano. Visible y editable en el editor y en el juego.
- Parser de objetos de **Layer 1** (port de las rutinas del banco $0D): tablas
  de pradera, castillo, casa fantasma, subterráneo y cuerda. Cobertura actual:
  **379/501 niveles reconstruidos al 100 %**.
- **Colisión real por celda** —bordes de un sentido, sólidos, cuestas y
  pinchos— portada de la rutina del juego (`SmwBlockCollision`).
- **Físicas reales del jugador** (salto, gravedad, topes) leídas de las tablas
  del banco $00 (`SmwPhysicsReader`), y **punto de inicio real** del nivel.
- La app ofrece **TODOS los niveles del juego** (70 reconstruibles), no una
  selección de 8.

**Personaje y jugabilidad**
- **Mario con su sprite real** (GFX32, hoja 128×64, paleta de jugador de la
  CGRAM): 5 poses, volteo, anclado por los pies, cabeza+cuerpo compuestos como
  el juego. Muerte animada.
- **Los cuatro poderes**: seta (crecer/encoger + invulnerabilidad), flor de
  fuego (Mario de fuego + bolas que matan enemigos) y capa (planeo).
- **Enemigos con sprite y color reales** (tabla OAM del banco $01 + sub-paleta
  del tweaker): 24 ids curados, Koopas CON caparazón y Goombas. La ruta ROM los
  siembra desde la lista real de sprites del nivel.
- **Monedas y bloques `?` interactivos** (clasificación real del "block code").

**Audio**
- **SFX reales** de la ROM (muestras BRR: salto, pisotón, moneda, powerup) —
  decodificador BRR + catálogo.
- **Música N-SPC sintetizada**: motores N-SPC + S-DSP portados a Kotlin (BRR,
  interpolación gaussiana, ADSR/GAIN, eco FIR).
- Herramienta CLI **`--music`**: renderiza la música a `.wav` para probarla sin
  la app, con diagnósticos (solape de eco, saturación, RMS).

**Warps y niveles enlazados**
- **Importación de nivel completo (bundle)**: sub-niveles enlazados + warps de
  proyecto (tuberías verticales y puertas) que FUNCIONAN al jugar; la app cambia
  de mapa al entrar. 35 niveles producen bundle multi-mapa jugable.

**Herramientas y documentación**
- **`--enemies`**: regenera el atlas de enemigos (`enemies.png`) de forma
  reproducible desde el catálogo.
- **`--scene`**: renderiza un nivel TAL COMO se importa a la app (fondo Layer 2
  + primer plano Layer 1) a un PNG, para verlo sin abrir la app — el equivalente
  visual de `--music`.
- Documentos `docs/INVENTARIO_SMW.md`, `docs/GUIA_DEL_PROYECTO.md` y
  `docs/GUIA_EXTRACTOR_SNES.md`.

### Cambiado
- Fusionada la rama `…-59ybqy` en `918asc`: unifica enemigos, audio, warps y
  bloques bajo una sola línea de trabajo.
- Blindaje del streaming de audio en Android (buffer 4× + prioridad de hilo de
  audio) para evitar cortes.

### Corregido
- Al importar un bundle se deja abierto el **nivel principal** (antes se abría
  el último sub-nivel, una salita casi vacía, y parecía que no salían las
  capas).
- Bugs visuales de escena: moneda de dragón rosa y tubería agujereada.
- **SFX audibles de verdad**: eran clics de 4 ms (faltaban bucle + envolvente).

### Limitaciones conocidas
- El fondo (Layer 2) scrollea 1:1 con el primer plano, **sin paralaje**.
- Pendientes: tuberías HORIZONTALES (0x3F) y para-Koopas.
- La música de la app es una pista fija pre-horneada; el ensamblador ya puede
  derivar la del nivel desde la ROM en runtime, pero no está cableado.
- Residual de audio de frecuencias altas en las secciones más densas.

---

## [0.1.0] — Base del motor

- Motor RPG/plataformas 100 % Android (`core` en Kotlin + `app` en Compose/OpenGL).
- Editor de mapas por capas, tilesets, warps y conexión de zonas.
- Extractor de assets SNES (gráficos, paletas, tilemaps) y primeras escenas de
  SMW con color real.
