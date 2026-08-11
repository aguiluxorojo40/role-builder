# Changelog

Registro de lo hecho en **Role Builder** (motor RPG/plataformas 100% Android +
pipeline que extrae contenido REAL de Super Mario World de una ROM). Formato
inspirado en [Keep a Changelog](https://keepachangelog.com/es/); versionado
semántico aproximado.

El mapa técnico completo de qué está hecho y qué falta está en
[`docs/INVENTARIO_SMW.md`](docs/INVENTARIO_SMW.md); la guía de uso y compilación
en [`docs/GUIA_DEL_PROYECTO.md`](docs/GUIA_DEL_PROYECTO.md).

---

## [Sin publicar] — Assets de cualquier nivel, y las dos capas por fin bien puestas

### Corregido
- **Las capas estaban INTERCAMBIADAS en todos los niveles importados de la ROM.** El
  importador ponía el fondo en la capa 0 y el terreno en la 1 *solo si el nivel traía
  fondo*; si no, el terreno se iba a la capa 0. Y los niveles creados desde el editor
  ("Nuevo") ponían el suelo siempre en la 0. Como los 8 niveles escaparate de la ROM
  **sí** traen fondo, en la práctica las herramientas "Primer plano" y "Fondo" pintaban
  cada una en la capa contraria a su nombre, y un nivel hecho a mano y uno importado
  guardaban el terreno en capas distintas. Ahora el convenio está escrito en un sitio
  ([`PlatformLayers`](core/src/main/kotlin/com/rolebuilder/core/model/PlatformLayers.kt)):
  capa 0 = **Capa 2 · fondo**, capa 1 = **Capa 1 · primer plano jugable**, tenga fondo el
  nivel o no. Los proyectos que ya existen se reparan solos al abrir el editor.
- **Borrar arrasaba las dos capas a la vez** (y los enemigos y los ítems de la casilla).
  Ahora borra en la capa activa; los objetos solo se van si estás borrando en el plano
  jugable, que es donde viven.

### Añadido
- **Banco de assets: teselas de CUALQUIER nivel del proyecto.** Hasta ahora un nivel solo
  podía pintar con el tileset que le tocó al importarlo, así que hacer un nivel propio con
  el castillo de uno y las tuberías de otro era imposible. El nuevo sector **Assets** del
  menú radial (y su botón en el raíl) abre la lista de niveles del proyecto, deja elegir
  teselas por categoría —varias de una vez— y las **copia** al tileset del nivel actual con
  su colisión, su acción de bloque y su animación
  ([`TilesetMerge`](core/src/main/kotlin/com/rolebuilder/core/model/TilesetMerge.kt)). El
  atlas crece por filas, así que las teselas que ya usaban los niveles no se mueven, y una
  tesela repetida se reutiliza en vez de duplicarse.
- **Nuevo nivel: de qué nivel salen los gráficos.** El diálogo de nivel nuevo ya no hereda a
  la fuerza el tileset del nivel abierto; se elige, y el suelo de arranque sale de la
  primera tesela sólida del tileset elegido.
- **Las dos capas, cubiertas en el menú radial y en el lienzo.** Los sectores se llaman
  "Capa 1" y "Capa 2", elegir uno cambia la capa activa, y una **barra de capas** flotante
  permite seleccionar capa, ocultarla (👁) y **enfocar** —atenuar la capa en la que no
  estás—, que es lo que hacía falta para editar el fondo de un nivel importado sin pintar a
  ciegas debajo del terreno. Cada capa recuerda **su** pincel y la paleta abre por la
  categoría que le corresponde (fondo → Decorado).

## [Sin publicar] — Los ids del Koopa estaban invertidos, y auditoría en CI

### Corregido
- **Los ids del Koopa estaban INVERTIDOS**, y era la causa raíz de que el caparazón no
  apareciera por ninguna parte. Los Koopa **con** caparazón son `0x04-0x07`; los `0x00-0x03`
  son los que van **sin** él (el "beach koopa" naranja con los pies de color). Verificado
  por tres vías independientes: el bit `0x40` de `kSprXXX_Generic_Spr0to13Prop` (dibujo de
  dos teselas apiladas = el Koopa alto que lleva caparazón) está en `0x04-0x0B` y no en
  `0x00-0x03`; el bloque de teselas del id `0x00` está documentado como *"Shell-less Koopa"*;
  y el volcado de la propia ROM, donde en `0x00-0x03` no hay caparazón y en `0x05` sí.
- **El giro en el borde** (`turnsAtLedge`) ya no es una lista a mano: sale del bit `0x02` de
  `Spr0to13Prop`, como en el juego. Da `0x01`, `0x02`, `0x05` y `0x06` — el rojo y el azul,
  con y sin caparazón — y **no** las aladas, que era donde estaba puesto antes.
- **Velocidad del Koopa desnudo**: ±4 px/f exactos (`kSprStatus09_Stunned_DATA_0197AD` =
  `{0xC0, 0x40}`), en sentido contrario a Mario. Antes eran 1.5 px/f a ojo.
- **Gracia de contacto al salir del caparazón** (`spr_decrementing_table154c` = 16): el Koopa
  desnudo nace bajo los pies de Mario y sin ella lo mataba en el mismo fotograma del pisotón.

### Añadido
- **El gráfico REAL del caparazón**, en vez del domo de color plano: `SmwEnemyGraphics.
  shellImage()` lo saca de la ROM con la paleta del nivel (port de `StunnedShellGFXRt_01980F`
  + `GenericGFXRtDraw1Tile16x16`), con el fotograma quieto (6) y el ciclo de giro `{6,7,8,7}`.
  **Cableado hasta la pantalla**: `PlatformerActivity` lo hornea para los cuatro colores y
  `PlatformerRenderer` lo dibuja anclado por los pies —quieto o girando al deslizarse—, con
  el domo de siempre como respaldo si no hay ROM. (El color de ese respaldo también estaba
  indexado por los ids equivocados.)
- **Los Koopa `0x04`, `0x06` y `0x07` al catálogo curado** (solo estaba el `0x05`, que por eso
  era el único cuyo caparazón se podía volcar). Se añaden AL FINAL: el orden de `curatedIds`
  fija los fotogramas del atlas horneado.
- **Herramienta `:core:dumpShellFrames`**: vuelca los fotogramas OAM de los Koopas y los
  caparazones desde la ROM, para MIRARLOS en vez de deducirlos. Es la que destapó todo esto.
- **Análisis estático (detekt)** sobre `:core`, con config propia y **baseline** de la deuda
  existente: CI solo se pone rojo con problemas NUEVOS. El informe sube a **GitHub Code
  Scanning** en SARIF.
- **Cobertura de tests (kover)** en CI, con resumen en el job y HTML como artefacto. Punto de
  partida medido: **52.9% de líneas** de `:core`.
- **CodeQL** (`java-kotlin`) y **Dependabot** (Gradle + acciones, agrupado y mensual).

### Notas de investigación
- Se documenta que el **overworld se lee entero pero no se edita**: 6 lectores en `:core` y
  solo consumo de lectura en la app (previsualizar, exportar, recorrer). Es el hueco
  principal para el objetivo de editar el overworld.

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
