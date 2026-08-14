# Changelog

Registro de lo hecho en **Role Builder** (motor RPG/plataformas 100% Android +
pipeline que extrae contenido REAL de Super Mario World de una ROM). Formato
inspirado en [Keep a Changelog](https://keepachangelog.com/es/); versionado
semántico aproximado.

El mapa técnico completo de qué está hecho y qué falta está en
[`docs/INVENTARIO_SMW.md`](docs/INVENTARIO_SMW.md); la guía de uso y compilación
en [`docs/GUIA_DEL_PROYECTO.md`](docs/GUIA_DEL_PROYECTO.md).

---

## [Sin publicar]

### Cambiado
- **Se retiran los releases rodantes.** Hasta ahora `apk-latest` se sobrescribía en cada
  push a `main` y servía de enlace de descarga estable. Se quita a propósito: un enlace
  que devuelve algo distinto cada día no dice **qué** estás instalando, y desde que cada
  versión publica su propio APK congelado (`v0.13.0`…) dejó de tener sentido mantener las
  dos vías. Para probar un commit concreto sigue estando el **artefacto del workflow**,
  que se sube en cualquier rama. Las guías y el README dejan de apuntar a `apk-latest` y
  mandan a la release de la última versión.
- Las etiquetas `apk-latest` y `apk-snes-latest` quedan como restos históricos: si alguien
  las empuja, el CI avisa de que ya no publican nada.

## [0.13.0] — 2026-08-14 — Mezcla de niveles, el caparazón real y un CI que vigila

Tres frentes. En el editor, por fin se puede construir un nivel con material de
varios niveles de la ROM. En la lectura de la ROM, los ids del Koopa estaban
invertidos y por eso el caparazón no aparecía por ninguna parte. Y el CI pasa de
compilar a vigilar.

### El Platform Builder

#### Corregido
- **Un sello pegado en un nivel más bajo entraba "solo por arriba"**, que parecía un fallo del
  pegado y no lo era: un sello de un nivel de SMW mide 27 filas y un nivel nuevo nacía con 15,
  así que el pegado recortaba. Ahora el nivel nuevo nace con **27 filas** (el alto real de un
  nivel horizontal de SMW) y, cuando algo no cabe, se **dice** en vez de dejarte adivinando.
- **Lo pegado no se podía mover**: pegar con la herramienta Sello no dejaba nada marcado, así
  que no había forma de agarrarlo. Ahora lo pegado **queda marcado** —con Área se mueve,
  voltea o vuelve a copiar— y el panel del Sello lo explica.
- **El tamaño del nivel estaba escondido** dentro de "Ajustes": no había manera de dar con él.
  Ahora el `48 × 15` de la esquina del lienzo **se toca** y abre el tamaño, y hay una acción
  "Tamaño" para el raíl.
- **Arrastrar el marco de selección se habría cortado al primer movimiento**: `areaSel` estaba
  entre las claves del gesto, y cambiarla lo reinicia. Se lee del estado capturado, como ya
  hacían `pan` y `scale`.
- **El banco de assets se cerraba en cada viaje**, así que parecía que solo se podía
  absorber material de UN nivel: para traer de un segundo había que volver a abrirlo, y no
  se veía por ningún sitio. Ahora se queda abierto, dice cuántas teselas lleva traídas y
  puedes cambiar de nivel de origen y seguir. Además el desplegable ya no salta al primer
  nivel después de cada viaje (se recuerda por id, no por objeto).
- **Traer teselas podía no hacer nada, en silencio**: si faltaba el PNG del nivel de origen
  o el atlas no cuadraba, la operación devolvía "cero teselas" y el diálogo se cerraba sin
  decir palabra — indistinguible de "esto no funciona". Ahora el motivo se ve en el diálogo.
- **El copiado bloqueaba el hilo principal.** Traerse un nivel entero son cientos de teselas
  y volver a codificar un PNG grande; ahora esa parte va en `Dispatchers.IO` con su aviso de
  progreso, y solo el alta del tileset vuelve al hilo de interfaz.

#### Añadido
- **Un nivel nuevo se lleva SU COPIA de los gráficos.** Antes compartía el tileset del nivel
  del que los tomaba, así que traer teselas o retocar una colisión en el nivel nuevo le
  cambiaba la paleta también al original. Ahora se duplica el atlas (PNG y metadatos) con id
  propio: cada nivel es dueño de sus gráficos. El precio, dicho: un PNG más en el proyecto y
  que los arreglos hechos en uno no se propagan al otro.
- **Las tuberías al mover un trozo, preguntando**
  ([`MapWarps`](core/src/main/kotlin/com/rolebuilder/core/model/MapWarps.kt)). Un warp tiene
  la **boca** en una casilla de este mapa y el **destino** en unas coordenadas fijas; mover
  el trozo no cambiaba ni una cosa ni la otra, así que la boca se quedaba separada de su
  dibujo y las tuberías que llevaban ahí seguían soltando al jugador en el sitio viejo. Al
  mover un trozo con tuberías en juego, el editor **pregunta** —con el trozo ya movido— si
  ajustarlas: las bocas se van con él y los destinos se reapuntan. Una boca que se saldría
  del mapa se descarta (a una tubería fuera de la rejilla no se puede entrar); un destino
  que se saldría se recorta, porque suelta un poco desviado pero deja la sala alcanzable.
  Si el trozo no tiene tuberías, no hay pregunta.
- **Mover lo seleccionado.** El marco de selección se arrastra: si el dedo baja DENTRO del
  marco se mueve su contenido (se recorta una vez y se va pegando sobre el mapa limpio, así
  que arrastrar no deja copias por el camino); fuera del marco, se marca una selección
  nueva. Y cuatro flechas en el panel para cuadrarlo casilla a casilla, que en una pantalla
  pequeña es más fiable que apuntar con el dedo.
- **Redimensionar el nivel con ANCLAJE**
  ([`MapEdits.resized`](core/src/main/kotlin/com/rolebuilder/core/model/MapEdits.kt)): el
  redimensionado de siempre anclaba arriba-izquierda, así que hacer un nivel de plataformas
  más alto dejaba el suelo flotando con un agujero debajo. Ahora se elige dónde se queda lo
  construido (por defecto, abajo: el alto nuevo se añade por arriba), el punto de inicio se
  mueve con él, y el ancho llega a 512 columnas —lo que traen los niveles largos de la ROM—
  y el alto a 120.

#### Corregido (bis)
- **Redimensionar dejaba warps fuera del mapa**: `GameMap.resized` recortaba eventos,
  enemigos e ítems pero no los warps, así que quedaban bocas de tubería invisibles fuera de
  la rejilla. El nuevo redimensionado los mueve y descarta como a todo lo demás.

#### Añadido (bis)
- **"Traer el nivel entero"** y **"Toda la categoría"** en el banco de assets: absorber todo
  el material de otro nivel es un botón, no ir picando 150 teselas de decorado una a una.
- **Relleno por rectángulo y cubo** ([`MapEdits`](core/src/main/kotlin/com/rolebuilder/core/model/MapEdits.kt)):
  pintar un suelo de 128 columnas casilla a casilla era el cuello de botella real del editor
  en un móvil. El rectángulo se arrastra con vista previa; el cubo rellena la zona contigua
  (4 vecinos, sin colarse en diagonal) sin recursión, que un nivel entero son 8640 celdas.
- **Deshacer / rehacer**, 40 pasos, agrupados **por gesto**: un trazo entero —o un relleno—
  se deshace de una vez. Botones fijos en la barra superior, no en el raíl configurable,
  porque son la red de seguridad de todo lo demás. Es barato porque el modelo es inmutable:
  guardar el estado anterior son tres referencias, no una copia del nivel.
- **Biblioteca de assets: cualquier trozo del proyecto sirve en cualquier mapa.** Un sello o
  un trozo copiado no guarda dibujos, guarda **índices a un atlas concreto**; pegarlo en otro
  nivel pintaba teselas al azar, y sin avisar. Ahora, al traerlo, sus teselas se copian al
  tileset del nivel de destino y el trozo se reescribe con los índices nuevos
  ([`MapRegion.remapped`](core/src/main/kotlin/com/rolebuilder/core/model/MapRegion.kt)). El
  diálogo de Assets pasa a tener dos pestañas —**Teselas** y **Sellos**, con vista previa
  dibujada con el atlas de su nivel— y lo mismo se aplica al pegar en la herramienta Área y
  al elegir un sello: si hace falta, los gráficos viajan solos.
- **Marco de selección** ([`MapRegion`](core/src/main/kotlin/com/rolebuilder/core/model/MapRegion.kt)):
  marcas un trozo del nivel y lo copias, cortas, borras, pegas, **duplicas al lado** (para
  extender un fondo a lo largo del nivel tocando repetido) o lo **volteas** en horizontal y
  vertical. Con **alcance por capas**: recortar un fondo sin llevarte el suelo que tiene
  delante es justo lo que hacía falta para construir fondos. Una selección buena se guarda
  como sello con un botón, y pegar un trozo de un nivel con otros gráficos avisa en vez de
  pintar teselas aleatorias.
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

#### Añadido
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

### La ROM y el CI

#### Corregido
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

#### Añadido
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

#### Notas de investigación
- Se documenta que el **overworld se lee entero pero no se edita**: 6 lectores en `:core` y
  solo consumo de lectura en la app (previsualizar, exportar, recorrer). Es el hueco
  principal para el objetivo de editar el overworld.

### El repositorio

#### Corregido
- **La rama por defecto llevaba parada desde el 5 de julio**, así que la portada enseñaba
  código de hace seis semanas mientras el trabajo real vivía enterrado en una rama. El
  repositorio tenía además **dos historias sin ancestro común**; `main` es ahora la línea
  viva y la rama por defecto.
- **El APK descargable estaba congelado**: la condición del workflow que publica
  `apk-latest` listaba cinco ramas y **ninguna era en la que se trabajaba**. Ahora sale de
  `main` en cada push.
- **El README contaba lo que el proyecto fue, no lo que es.** Vendía "motor de RPG estilo
  RPG Maker" y ponía el editor ARPG por delante, cuando `core/snes` son ~19.000 líneas
  frente a las ~1.600 del motor ARPG. Reescrito: lidera leer la ROM, enumera las tres
  puertas reales y marca el ARPG **en pausa**. Las cifras de tests y líneas de `README` y
  `AUDITORIA` estaban todas desfasadas y se han vuelto a medir.
- **`AUDITORIA.md` afirmaba que exportar un proyecto no reparte material con copyright.**
  Es falso: `SnesImport` guarda el atlas del nivel importado en `images/` y `ZipIo`
  empaqueta la carpeta entera. Ahora se dice con todas las letras.

#### Añadido
- **Mutation testing con pitest** (`:core:pitest`), con su job, resumen y artefacto en CI.
  Primera medida, sin ROM y en condiciones de CI: **16.840 mutantes, 37,1% cazados**. El
  reparto confirma medido lo que la auditoría solo intuía — `core.snes` se queda en 28,2%
  porque sus tests se saltan solos sin la ROM, mientras `Interpreter` llega al 86,8%.
  Detalle en [AUDITORIA.md](AUDITORIA.md) §6.
- **Releases de versión por etiqueta**: empujar `v*` publica una release congelada con su
  APK, mientras `apk-latest` se sigue sobrescribiendo. Y `workflow_dispatch` para lanzar el
  CI a mano.
- **Un guardián para los nombres de etiqueta.** La primera versión de lo anterior fallaba en
  silencio: si la etiqueta no encajaba con `v*` —por ejemplo `0.13.0`, o `V0.13.0` con la V
  que autocapitaliza el teclado del móvil— el workflow ni se disparaba, y la release se
  quedaba sin APK sin un solo mensaje. Ahora el CI escucha **todas** las etiquetas y un job
  las clasifica: la correcta pasa, la mayúscula publica pero avisa, y una que parece versión
  sin la `v` **falla en rojo** diciendo cómo debería llamarse.
- **Social preview del repositorio** y sus fuentes en [`design/`](design/): el PNG se
  genera con un programa, no se retoca a mano.
- Se deja escrito en la guía que **`claude/hd2d-3d-fase10` está aparcada, no olvidada**:
  guarda un renderer 3D con z-buffer que no está en `main`, con lo que costaría traerlo y
  por qué hoy sería una regresión.

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
