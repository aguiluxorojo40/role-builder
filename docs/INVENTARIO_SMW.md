# Inventario del pipeline SMW → Role Builder

Estado de TODO lo construido alrededor de Super Mario World: qué está hecho en
`core`, qué está cableado de verdad en la app, y qué queda. Este documento es el
mapa; la guía de uso y compilación está en [GUIA_DEL_PROYECTO.md](GUIA_DEL_PROYECTO.md).

**Rama de trabajo:** `claude/snes-sprite-color-automation-918asc` (desde el merge
de `…-59ybqy`, esta rama contiene TODO el trabajo SMW).

## Los dos oficios de la app: JUGAR y EDITAR

Desde la pantalla de inicio salen **tres puertas**, y la separación es
deliberada: el editor sirve para editar y **probar**, y el juego se juega
aparte, sin que uno pise el estado del otro.

- **🎮 Jugar Super Mario World** — `TitleActivity` (título real desde la ROM y
  tres ranuras) → `OverworldActivity` en `MODE_GAME` → `PlatformerActivity` →
  vuelta al mapa con el camino abierto. La progresión se **guarda en disco**
  (`SmwGameSave` / `SmwSaveIo`, en `filesDir/smw_saves`). La ROM se elige una
  vez y se queda (`GameRom`).
- **🗡️/🍄 Los dos editores** — Role Builder (ARPG) y Platform Builder. Desde el
  diálogo de importación SNES se puede **probar**: "▶ Jugar un nivel" y "🗺️
  Probar el mapa". El mapa se abre en `MODE_TEST`: lo enseña todo abierto y
  **no escribe ninguna partida**.

Dentro del editor hay además **dos rutas de plataformas**, y casi todos los
"parcial" de las tablas se explican por cuál de las dos usa cada pieza:

- **Ruta ROM directa** — "▶ Jugar un nivel" → `PlatformerActivity.buildRomRenderer`
  → `buildEngine`. Monta el motor leyendo la ROM en vivo.
- **Ruta mapa de proyecto** — "Buscar niveles importables" → "Crear mapa" → el
  nivel queda como mapa+tileset del proyecto → `ProjectPlatformer.engine`.

### Ni un byte de Nintendo en el repositorio

Los gráficos, sonidos y música de SMW **no se empaquetan**: se **hornean en el
dispositivo** desde la ROM del usuario la primera vez que entras al título
(`SmwBakedAssets` en core, `SmwAssetStore` en la app, que los guarda en
`filesDir/smw_assets`). En el repositorio van solo las coordenadas y el cómo
decodificar. Donde más abajo se lea "assets horneados", es eso: ficheros del
dispositivo, nunca del repositorio.

## ✅ Logrado y cableado en la app

| Pieza | Core | En la app |
|---|---|---|
| **Colisión real por celda** (bordes de un sentido, sólidos, cuestas, pinchos; port de la rutina del juego) | `SmwBlockCollision`, `SmwLayer1` | Ambas rutas |
| **Físicas reales del jugador** (salto, gravedad, topes; tablas del banco $00) | `SmwPhysicsReader`, `PlatformerTuning.fromSmw` | Ambas rutas |
| **Punto de inicio real** del nivel | `SmwLevelStartReader` | Ambas rutas |
| **Mario con su sprite real** (GFX32, hoja 128×64, paleta de jugador de la CGRAM; 5 poses, volteo, anclado por los pies) | `SnesGameRecipes.smwMarioSheet` | Ambas (ROM: en vivo; proyecto: `sprites/mario.png` horneado en el dispositivo del mismo tamaño) |
| **Enemigos con sprite y color reales** (tabla OAM genérica del banco $01 + sub-paleta del tweaker $166E) — 24 ids curados (tandas 0+1) | `SmwEnemyGraphics` | Ambas rutas (atlas `sprites/enemies.png` horneado en el dispositivo); la ROM directa además los SIEMBRA desde la lista real de sprites (`smwLevelEnemies`) |
| **Monedas y bloques `?` interactivos** (clasificación real del "block code": moneda=0x2B, `?`=0x21..0x24) | `SmwBlockBehavior` + `BlockAction` en el motor | Ambas rutas (la ROM directa desde el commit `2434056`) |
| **Powerup: seta → crecer → encoger** (el `?` suelta seta si eres pequeño; crecer cambia la caja a 26 px; un golpe encoge con ~1.5 s de invulnerabilidad) | `PlatformerEngine` (`items`, `playerHeight`, `powerupEvents`/`damageEvents`) | Ambas rutas; parpadeo + SFX en el renderer |
| **SFX reales** (muestras BRR de la ROM: salto, pisotón, moneda, powerup) | `SmwSoundFx`, `SmwSfxCatalog` | `PlatformerAudio.fromRom` (o los `sfx/*.wav` horneados en el dispositivo) |
| **Música N-SPC sintetizada** (motor N-SPC + S-DSP portados) | `SmwMusicRenderer`, `SmwDsp` | `PlatformerMusic` (pista fija sintetizada desde la ROM) |
| **Importar un nivel como mapa jugable** (atlas Map16 + tilemap + colisión + acciones + enemigos + teselas animadas) | `SnesGameRecipes.extractSmwLevelAsMap` | Diálogo de importación SNES |
| **Escenas/galería de niveles** (tilemap reconstruido + fondos Layer 2 con color real) | `renderSmwLevelScene`, `renderSmwBackground` | Galería del diálogo de importación |
| **Ficha del nivel** (tamaño, modo, música, paletas, tiempo) | `smwLevelInfo` | CLI del extractor |
| **Comportamiento de sprites** (los 6 tweaker bytes por id) | `SmwSpriteBehaviorReader` | Lo usa `SmwEnemyGraphics` para la paleta |

## 🟡 Hecho en core pero NO expuesto (integración pendiente, por valor)

1. **Warps de proyecto: VIVOS.** "Crear mapa" importa el bundle (sub-niveles
   como mapas + `platformWarps` resueltos a ids de mapa) y la app cambia de
   mapa al entrar. Con el parser al nivel actual hay **35 niveles** que
   producen bundle multi-mapa con warps jugables (castillos y casas fantasma
   incluidos). Solo faltan las tuberías HORIZONTALES (0x3F, ver Hallazgos).
3. **Música derivada de la ROM cargada** (`SmwMusic.assembleAram`): hoy la app
   suena una pista fija; el ensamblador puede derivar la del nivel
   desde la ROM del usuario en runtime.
4. **Color de enemigo por nivel en vivo** (`SmwEnemyGraphics.spriteImage`): la
   app usa el atlas horneado (paleta del nivel de referencia); en vivo cada
   nivel podría teñir a sus enemigos con SU sub-paleta.

## 🔴 Deudas y riesgos conocidos

- ~~**`enemies.png` no es reproducible**: ningún tool lo regenera desde
  `curatedIds`; si se reordena el catálogo, el atlas se desincroniza en
  silencio.~~ → Resuelto: `--enemies` en el extractor lo regenera desde el
  catálogo, y el orden lo fija `curatedIds`.
- **Etiquetas de enemigos**: los nombres del catálogo se re-verificaron contra
  el despacho real del juego al ampliar la tanda 1 (antes 0x1C/0x29/0x2A/0x4B
  tenían nombres equivocados).
- ~~**Mario grande se dibuja estirado**~~ → Resuelto: `smwMarioSheet(powerup=1/2/3)`
  compone Mario GRANDE/FUEGO/CAPA con sus teselas reales de GFX32 (16×32 por pose, offset
  de tesela 0x46/0x83 + tablas cabeza/cuerpo). La ruta ROM directa ya los usaba; ahora la
  ruta de PROYECTO también, con las hojas horneadas en el dispositivo `sprites/mario_big|cape|fire.png`
  (regenerables con `extractSnesTileset --mario --powerup big|cape|fire`).
- ~~**La seta se dibuja con rectángulos**~~ → Resuelto: `SmwEnemyGraphics.powerupSheet`
  renderiza SETA/FLOR/PLUMA con sus teselas de sprite REALES (`0x24/0x26/0x0E` de
  `kPowerUpAndItemGFXRt_PowerUpTiles`, `$01:C61A`) y su paleta `$166E`, aunque estén fuera
  de la tabla OAM genérica de enemigos. Horneado en el dispositivo como `sprites/powerups.png` (48×16),
  regenerable con `extractSnesTileset --powerup-sheet`; el renderer lo usa por tipo.
- **Cuestas**: colisionan como bloque completo (la altura sub-píxel de la
  pendiente es un refinamiento pendiente).
- **Enemigos con rutina de dibujo propia**: fuera del alcance de la tabla OAM genérica
  (ids ≥ 0x54 o multi-tesela). Infraestructura: `SmwEnemyGraphics.customSprite`/
  `customEnemyImage` compone su sprite real a partir del layout de teselas de su rutina
  `Spr..._Draw`, horneado en el dispositivo a `sprites/big/big_<id>.png` (auto-cargado por id) con
  `extractSnesTileset --custom-enemy --id 0xNN`.
  - ✅ Hechos: Thwomp (0x26), Pokey (0x70), **Rex (0xAB)**, **Blurp (0xC2)**,
    **Super Koopa suelo (0x73) y capa roja (0x71)** (frame 0 de andar, cuerpo + 3 teselas de
    capa 8×8; paleta de capa por la fórmula real `(Prop|v4)&~2`: 2 en 0x73, 4 en 0x71),
    **PorcuPuffer (0xC3)** (pez globo 32×32 = 4 teselas 16×16, paleta 6), **Fishbone (0xAA)**
    (cabeza 16×16 + 2 teselas de cola 8×8), **Wiggler (0x86)** (oruga de 5 segmentos 16×16
    con arco `WigglerYDisp` + flor 8×8 paleta 5; teselas 0x8c/0xc6/0xc8/0xc4/0x98 de su
    rutina `Spr086_Wiggler`, verificada contra los niveles 0x11e/0x126 que la generan),
    **Swooper (0xBE)** (murciélago, 1 tesela 0xae), **Dino-Rhino (0x6E)** (dino 32×32 = 4
    teselas 16×16, frame 0, paleta 7) y **Blargg (0xA8)** (cabeza de dragón de lava, 5 teselas
    16×16 de `Spr0A8_Blargg_Draw`, paleta 2). Thwomp 0x26, Thwimp 0x27, Grinder 0x24 ya salen
    por el atlas genérico. El compositor soporta paleta por-tesela y volteo H/V (`OamTile`).
    **Reznor (0xA9)** (dino-jefe de castillo 32×32, teselas 0x40/0x42/0x60/0x62 de
    `Spr0A9_Reznor_Draw`, paleta 7) y **Big Boo Boss (0xC5)** (boo gigante ~64×64 = 4×4
    teselas 16×16 + boca/colmillos, frame 0 de `NormalSpriteBooDraw`, v-flip en la mitad
    inferior, paleta 7; su GFX sí está en las ranuras normales de su sala 0xE4). El
    compositor soporta paleta por-tesela y volteo H/V (`OamTile`).
  - 🧩 **Jefes de Modo 7** (GFX que NO está en el ajuste estático del nivel): las salas de
    jefe (modos de nivel 9/11/16) IGNORAN el ajuste de GFX de sprites de la cabecera y lo
    FUERZAN en `GameMode12_PrepareLevel_PrepareMode7Level` ($00:97BC) según el jefe activo
    (`misc_currently_active_boss`): jefe<3 → ajuste 18, jefe==3 → 24, jefe==4 (Reznor) → 19.
    Por eso `CustomEnemy` acepta un `gfxSetting` que fuerza el mismo ajuste que el juego
    (Reznor: 19 → ficheros {0,1,0x25,0x22}; el dino real está en el 0x25, no en el 0x13
    estático). Herramientas de apoyo: `extractSnesTileset --gfx-dump` (vuelca todos los
    ficheros GFX) y `--sprite-sheet [--gfx-setting N]` (vuelca la VRAM de sprites de un
    nivel) para localizar a ojo el GFX real de un jefe.
  - ✅ **Jefes de Modo 7 por EMULADOR** (`scripts/mode7_boss_capture.py`): **Bowser (0xA0)**
    y los **Koopalings Morton/Roy/Ludwig (0x29)** se dibujan como bloque del tilemap de Modo 7
    (`UpdateMode7SpriteAnimations`), no por OAM, así que se capturan de un emulador real
    (stable-retro / núcleo snes9x, headless). El script warpea a cada sala de jefe por RAM
    (game mode $0100=0x11 = `GameMode11_LoadSublevel`, nivel en $0109 = L+0x24 con
    `counter_sublevels_entered`=0), captura el framebuffer y recorta el jefe a
    `big_<id>.png`: Bowser (fondo negro → key por luminancia) y Morton (sala 0x9a, key
    naranja|verde|blanco para descartar el ladrillo). Se auto-cargan como los demás.
    Salas Modo 7 (modo 9/11/16): Bowser 0x9B, Koopalings 0x96-0x9a/0xcc/0xd9, Reznor 0x95/0xd5.
    Los **7 Koopalings** (mismo sprite 0x29, uno por castillo) están capturados y localizados
    (ver `docs/koopalings_mode7.png`): Iggy 0x96, Larry 0x97, Roy 0x99, Morton 0x9a, Ludwig 0xcc
    (recorte limpio) y dos lanzafuegos 0x98/0xd9 (la bola de fuego se pega al recorte). Como el
    motor usa un id = un PNG, `big_29.png` es Morton.
  - 🧪 Referencia/validación: `SmwMode7Boss.kt` decodifica las teselas de carácter 3bpp de
    Modo 7 (port de `_BufferTilemap`, 24 B/tesela) y el extractor tiene `--mode7-boss`
    (`--sheet`/`--all-frames`) para volcarlas; sirve de contraste del GFX del jefe.
  - 🎮 **Jefes JUGABLES en el motor**: `EnemyBehavior.BOSS` (`PlatformerEngine.kt`) para Bowser
    (0xA0), Koopaling (0x29), Reznor (0xA9) y Big Boo (0xC5): enemigo GRANDE con varios puntos
    de vida (`BOSS_HP`), caja de colisión mayor y patrulla lenta; el pisotón/caparazón/bloque/
    fuego le restan HP (con invulnerabilidad entre golpes vía `damageEnemy`), muere al llegar a
    0. Se dibuja con su `big_<id>.png` auto-cargado. **Ataque propio** ([updateBoss]/[bossShoot]):
    - **Reznor (0xA9)**: bola de fuego **1:1** — APUNTADA al jugador y RECTA (sin gravedad), con
      la matemática y velocidad reales (`SmwSpriteAim.aimTowardsPlayer`, puerto de
      `AimTowardsPlayer` $02:D2FB, velocidad 0x10, como `Spr0A9_Reznor_ReznorFireRt`). Test del
      aim contra valores del disassembly en `SmwSpriteAimTest.kt`.
    - **Koopaling (0x29)**: bola de Ludwig (0x34) **1:1** — HORIZONTAL hacia Mario a 0x20
      (2 px/f), recta y sin gravedad (`Spr029..._01D059` → `Spr034_LudwigFireball`,
      `kSpr029..._DATA_01D0BE`).
    - **Big Boo (0xC5)**: FLOTA persiguiendo a Mario como fantasma (sin gravedad/terreno).
    - **Bowser (0xA0)**: conducta MÁS FIEL por FASES ([updateBowser]) — FLOTA en la franja
      superior del nivel (coche-payaso, fila `BOWSER_HOVER_ROW`, sin gravedad) y DERIVA en X
      siguiendo a Mario; alterna una TANDA de Mechakoopas y una TANDA de bolas de bolos
      (`BOWSER_ATTACKS_PER_PHASE` ataques por tanda, `bossPhase`), con sub-sprites reales (los
      jefes los generan vía `pendingSpawns`/`enemies` mutable):
      · **Mechakoopa (0xA2)** — `EnemyBehavior.MECHAKOOPA` (puerto de `Spr0A2_MechaKoopa`): cae
        en arco y luego ANDA a 0.5 px/f re-encarando a Mario cada 0x40 frames, rebota en
        paredes y se cae por los bordes. Al PISARLO se VOLTEA (aturdido, no muere); volteado se
        puede COGER corriendo por encima (como un throw-block) y LANZAR para arrollar a otros
        enemigos, igual que en SMW; si no lo coges a tiempo se endereza (`MECHA_STUN` frames).
      · **Bola de bolos (0xA1)** — `EnemyBehavior.BOWLING_BALL` (puerto de
        `Spr0A1_BowserBowlingBall`): cae (gravedad +3, tope 0x40), REBOTA con la altura real
        (`BounceYSpeed[(caída>>2)+19]`) y RUEDA hacia Mario a 1 px/f; hiere por cualquier lado.
    Con tests de HP y de ataque en `PlatformerEngineTest.kt`.
  - ✅ **Dino-Torch (0x6F)**: el dino que ESCUPE fuego (`Spr06F_DinoTorch_Draw` rama
    `spr_spriteid==111`, $03) — cabeza 16×16 (`kSpr06F_DinoTorch_DinoTorchTiles`, frame 1) +
    LLAMA horizontal de 4 teselas (`DinoFlameTiles` {0x80,0x82,0x84,0x86}, XDisp
    {−40,−32,−20,−8}) con sus paletas reales de fuego (Prop 0x9/0x5 → 4/2). Horneado y
    verificado por render del ROM (`--custom-enemy --id 0x6F`, nivel 0x105, misma paleta que el
    Dino-Rhino 0x6E) → `big_6f.png` auto-cargado.
  - 🟡 Pendientes: **Koopalings lanzafuegos
    0x98/0xd9: DESCARTADO tras verificar por emulador** — warpear a esos niveles (L+0x24) NO
    cae en una arena de Modo 7 con el Koopaling grande, sino en un SUBNIVEL de castillo normal
    (Mario pequeño andando, lava, ventanas), sin Koopaling de Modo 7 que recortar. La suposición
    previa ("el fuego naranja se funde con el cuerpo") no aplica: no hay tal recorte. El asset
    jugable `big_29.png` sigue siendo Morton (sala 0x9a), que es limpio; el motor usa 1 id = 1
    PNG, así que no falta nada para jugar.

## Motor 1:1 (progreso y huecos)

Objetivo: que el substrato del motor sea bit-exacto a SMW, no solo los comportamientos.
- ✅ **Gravedad de sprites EXACTA**: `SPRITE_GRAVITY = DATA_019030 = 3/16 = 0.1875 px/f²`
  y terminal `SPRITE_MAX_FALL = DATA_01902E = 0x40 = 4 px/f` (`SubUpdateSprPos` $01). La
  usan todos los enemigos que caen (andadores, caparazón, saltarina 0x09). Como son
  fracciones n/16 exactas, en float el acumulado es bit-exacto (equivale al subpíxel).
- ✅ Salto/gravedad/caída-terminal/topes de Mario y velocidades de enemigos porteadas.
- ✅ **Aceleración/rozamiento de Mario EXACTOS**: port 1:1 de `HandlePlayerPhysics`
  (`$00:D5F2`) en `SmwPlayerXMovement`, con las tablas REALES de la ROM US: aceleración
  `MarioAccel_` (`$D345`), rozamiento de suelo `$D2CD` / de agua `$D309`, tope por estado
  `$D535`, hielo `$D43D`. Modela andar/correr/despegue (`|v| ≥ 0x23`), **derrape** al
  girar a velocidad (índice `+0x90`), rozamiento hacia 0 (nulo en el aire), hielo y el
  **medidor-P** (`$D5EB` = {−1,−1,+2}, tope `0x70`). Los índices `X`/`Y` son offsets en
  BYTES sobre las tablas de words. La velocidad avanza con acumulador de subpíxeles
  (`bodyStepX`, gemelo de `smwStepX`), así las distancias salen 1:1. Solo en modo ROM
  (`PlatformerEngine(smwPhysics = …)`); los proyectos genéricos conservan el modelo simple.
- ✅ **Colisión por PUNTOS-SONDA de SMW** (modo ROM): port del modelo de
  `RunPlayerBlockCode` (`$00:EADB`) — el jugador muestrea el mapa en puntos de offset fijo
  (sondas laterales para paredes, dos de pies para el suelo, cabeza para el techo) en vez
  de barrer el borde de la caja, fija `blockedFlags` (`player_blocked_flags` `$77`) y
  dispara el block-code (`?`/ladrillo) en el punto de contacto. Reutiliza la clasificación
  1:1 (`SmwBlockCollision`, `$00:EB77`) y el perfil de cuesta sub-píxel (`slopeSurfaceY`).
  Solo en modo ROM; los proyectos sin ROM conservan el AABB por span.
  **Fuera del port** (subsistemas ausentes en el motor): wall-running, cuestas de agua,
  note-blocks, Yoshi y los puntos del overflow de la tabla `E89C`. El aplastamiento no se
  modela (no hay plataformas móviles que lo provoquen).
- 🟢 **Bloques de AGARRAR/LANZAR (estilo SMW, no 1:1)**: las celdas `BlockAction.GRAB` se
  siembran como entidades `GrabBlock`; Mario las coge con el botón de correr estando al
  lado, las lleva y las lanza (arrollan enemigos, se rompen contra pared), o las deja con
  abajo. Es una mecánica *estilo* SMW a nivel de motor —no el port byte-a-byte de
  `RunPlayerBlockCode`—; falta el cableado de extracción para poblarlas desde tiles reales
  del ROM (el tile de throw-block `0x2E` choca con la Dragon Coin y no es desambiguable sin
  verificación en juego). Render en `PlatformerRenderer`.

## Tandas de enemigos (medidas sobre 316 niveles de la ROM US)

**Tanda 1 — ENTREGADA:** 0x4F Planta Piraña saltarina (24 niveles), 0x37 Boo,
0x3D Rip Van Fish, 0x15/0x16 Cheep-Cheep, 0x2E Spike Top, 0x38/0x39 Eerie,
0x31 Bony Beetle. Verificada visualmente con `--enemies` (voto por mayoría
entre los niveles que contienen cada id). **Descartados** 0x33 Podoboo, 0x30 y
0x32: su entrada de la tabla OAM genérica no es su aspecto real (rutina de
dibujo propia; salían tiles de fuente o basura de forma unánime).

**Tanda 2 — ENTREGADA (tras un primer intento revertido):** Koopas aladas
0x08/0x09/0x0A/0x0B (lo más colocado de la ROM: 84/72/58/42). El primer intento
(cuerpo genérico + ala suelta) suspendió la verificación visual (cuerpo SIN
caparazón y ala tapando la cabeza), así que se portó su DIBUJO real:
`SmwEnemyGraphics.wingedKoopaFrames` compone el cuerpo apilado CON caparazón
(misma entrada que las Koopas de caparazón) + el ala de `KoopaWingGfxRt` con sus
dos fotogramas de aleteo (tablas `KoopaWing*`, tesela 0x5D/0xC6). Y su MOVIMIENTO
1:1 en el motor: `PlatformerEngine.updateWingedKoopa` (rutinas ParaKoopa $01) —
0x08 vuela a la izq. (−0.5 px/f con bob), 0x09 rebota (−3 px/f al tocar suelo),
0x0A/0x0B oscilan ±1 px/f (rampa con pausa 0x30) en Y/X respectivamente; el
pisotón les quita las alas y quedan de andador con caparazón. Con tests.

## Comportamiento de las Plantas Piraña (portado al motor)

Los 3 tipos de Planta Piraña tienen sprite fiel (`SmwEnemyGraphics`) **y** conducta
portada del ROM en `PlatformerEngine` (`EnemyBehavior`, `updatePipePiranha` /
`updateJumpingPiranha`). Fuente: `ClassicPiranhas` (banco $01) y `JumpingPiranhaMain`
(banco $02) del disassembly IsoFrieze/SMWDisX.

El movimiento vertical se porta con las UNIDADES EXACTAS de SMW (`smwStepY` replica
`SubSprYPosNoGrvty`/`UpdateYPosNoGrvty`: byte `IIIISSSS`, ÷16 = px/f con acumulador de
subpíxeles), así que las velocidades y distancias salen 1:1 con el ROM.

- **De tubo — `0x1A` recta / `0x2A` cabeza-abajo** (`ClassicPiranhas`): máquina de 4
  estados con velocidad Y por estado (`PIRANHA_SPEED = {0, 0xF0, 0, 0x10}` = 0, −1, 0,
  +1 px/f) durante `PIRANHA_TIME = {0x20,0x30,0x20,0x30}`: metida → saliendo (−1 px/f ·
  48 f = **asoma exactamente 48 px / 3 casillas**) → fuera → entrando (+1 px/f · 48 f =
  vuelve) → repite (~160 f/2.7 s). La `0x2A` cuelga del techo (velocidad negada, como el
  `EOR #$FF : INC A` del juego). **No sale mientras Mario está pegado** (`PIRANHA_NEAR_PX`
  ≈27 px, del chequeo `_F+0x1B < 0x37`): metida no hiere ni se dibuja.
- **Saltarina — `0x4F`** (`JumpingPiranhaMain`): salta con `0xC0` = −4 px/f y frena a
  +0.125 px/f² (`+2` unidades/frame) hasta −1 px/f (estado 1, ~24 f); luego una gravedad
  MUY lenta (`+1` unidad cada 4 frames, tope +0.5 px/f) — el "cae poco a poco" — hasta
  volver al tubo. Medido: **arco de ~95 px / 6 casillas, ~290 f airborne**. Dibujo de 2
  partes (boca + hojas-hélice verdes).
- **Saltarina de fuego — `0x50`**: igual que la `0x4F` y, al valer el temporizador 0x40
  en el descenso, **escupe DOS bolas en "V"** (X ±0x10 = ±1 px/f, Y 0xD0 = −3 px/f), no
  hacia Mario — extended sprite `0x0B`/`Hammer` que ARQUEA (gravedad +0.125 px/f²); aquí
  `EnemyProjectile`. Evento `piranhaFireEvents`.

Las Plantas Piraña **no se pisan** (muerden por cualquier lado, salvo el tobogán). Tests
en `PlatformerEngineTest` (asoma 48 px con Mario lejos, no sale con Mario encima, salta en
arco, escupe fuego).

## Mecánica de caparazón de Koopa (motor)

Los Koopas con caparazón (0x00-0x03, 0x05) portan la mecánica de SMW (estados
`SpriteStatus` 8 normal / 9 quieto / A pateado) en `PlatformerEngine`:
- **Pisar** un Koopa lo mete en su CAPARAZÓN (`shell`, no muere) en vez de matarlo.
- **Tocar de lado** un caparazón quieto lo **PATEA** (`shellMoving`, `SHELL_SPEED` =
  `ShellSpeedX 0x37` = 55/16 = **3.4375 px/f** EXACTO), lejos de Mario; gracia
  `SHELL_KICK_GRACE` = `KickingTimer 0x0C` = **12 frames** (valor exacto de SMW).
- El caparazón deslizándose **arrolla** a los demás enemigos (cadena), rebota en paredes
  y se cae por los bordes.
- **Pisar** un caparazón en marcha lo **para**; de lado, muere Mario.
- v1: el caparazón es persistente (no revive solo; el Koopa-sin-caparazón que corre a por
  el suyo es un refinamiento pendiente). Render: domo del color del Koopa. Tests en
  `PlatformerEngineTest`.

**Koopas ALADAS (Parakoopa 0x08-0x0B):** vuelan con los valores EXACTOS de las rutinas
`GreenParaKoopa`/`RedVertParaKoopa`/`RedHorzParaKoopa` (movidas por `smwStepX`/`smwStepY`
en unidades SMW): **0x08** verde a −0.5 px/f (`F8`) a la izquierda con bob ∓0.25 (`FC`/`04`
según `1570&0x20`); **0x09** saltarina rebota `D0` = −3 px/f al tocar suelo; **0x0A**
vertical y **0x0B** horizontal con la **velocidad OSCILANTE** real (`CODE_018CFD`: rampa
±1 hacia ±0x10 con `1540`/`C2`/`151C`). **Al pisarlas pierden las alas** (`winged=false`)
→ siguiente pisotón = caparazón → patada. Se dibujan como el Koopa de suelo
(`koopaColorId`) al perder las alas.

## Hallazgos de investigación (para retomar sin re-descubrir)

- **Cobertura del parser de objetos de Layer 1 por nivel** (ROM US): **0x105
  Yoshi's Island 1 = 100%** (antes 70%: se portaron StdObj1F tubería fina,
  ExtObj86 cartel de meta, ExtObj8E bloque de interruptor y GrassObj39 tubería
  diagonal, y el gate ahora acepta los tilesets 7/0xC que usan la tabla de
  pradera, como el juego). 0x106 YI2 = 96%, 0x024/0x0C7/0x0C5 = 100%,
  0x022 = 95%, y **0x101 castillo de Iggy = 100%** (antes 19%: portados
  agua/lava 0x18-0x1B, redes trepables 0x1D/0x1E, bloque de piedra de castillo
  0x3C y puerta de red 0x4A). Con ello el PRIMER bundle multi-mapa de la ROM
  se enciende: 0x101 importa 2 mapas (101+1FC) con 3 warps jugables — los
  warps de proyecto ya NO están latentes. Tras portar también la
  tubería doble de castillo (0x34), la puerta del jefe (ext 0x90) y la tabla
  de CASA FANTASMA (0x34-0x3A: salientes sobre columnas, ladrillo/madera,
  troncos, muros/pinchos), el estado global es: **379 de 501 niveles
  parseados al 100%** y **39 bundles multi-mapa con warps jugables** (de 0 a
  39 en una sesión). Portada también la tabla de SUBTERRÁNEO (0x36 suelo 4
  lados, 0x3A/0x3B lava de cueva, 0x3D saliente de techo —el objeto más usado
  de la ROM sin portar, 307×—, 0x3E bordes de techo, 0x3F tierra maciza;
  tilesets 3/9/A/B/E) y el interruptor VERDE (ext 0x87). Galería: 8 niveles.
  Portada también la tabla de CUERDA
  (ts 2/6/8: sombrero/tallo de seta 3C/3D con sus uniones leyendo el buffer,
  puente de troncos 0x32, guías de línea horizontal/vertical — compartidas
  con castillo 0x37/0x38). Pendiente: bosque (ts C: 34/35/37), cintas
  transportadoras (rope 0x36), guías inclinadas (rope 3A/3B) y escaleras
  (castle 3D). El parser REGISTRA los ids sin portar
  (SmwLevelTilemap.unknownIds), así que medir el siguiente nivel es
  inmediato.
- **CASA FANTASMA casi completa (v0.12.0): 49/56 niveles al 100%** (antes 16).
  Portados los objetos que faltaban: estándar 0x20 (tablón), 0x31 (panel
  enmarcado/ventana con patrón alternante), 0x32, 0x3B–0x3F (vigas y losas), y
  extendidos 0x57–0x5E (detalles), 0x64/0x65 (bloques 2×2) y 0x49 (mural 6×13 de
  pared). La Casa Fantasma #1 (nivel 0x4) se reconstruye entera. Global Layer 1:
  **419/477 niveles al 100%**.
- **Cola de ids sueltos: CERRADA.** Portados los que faltaban, todos medidos contra
  la ROM con `ZzLayer1CoverageProbe` (sonda opt-in, se salta sin `SMW_ROM`):
  **+8 niveles al 100%** (018, 0C9, 0CA, 0F8, 104, 12D, 1D7, 1D8). Sobre el
  denominador honesto ([SmwLevelSet], niveles que el juego referencia de verdad)
  la cobertura queda en **146/196**; medido sobre los 512 huecos salía 451/501,
  que es el mismo trabajo con un número inflado. Lo portado:
  - ext 0x61-0x63 — reloj de pared de casa fantasma y sus dos telarañas (3×3).
  - ext 0x85 — CASA DE YOSHI (mural fijo de 16×10).
  - ext 0x8A-0x8D — los cuatro interruptores de palacio (2×2).
  - ext 0x97 — tesela de borde del palacio (1 tesela, y de página 1).
  - casa fantasma 0x2E (línea de pinchos) y 0x30 (repisa de hierba).
  - pradera 0x30 (tubería helada 2 columnas) y 0x31 (bloque giratorio helado).

- **Segunda tanda, atacando lo que más niveles bloqueaba: 146 → 175 de 196.**
  Todo medido con la sonda, que ahora agrupa los ids por FAMILIA de tileset (un
  `std:35` no dice nada sin saber si es pradera, cueva o cuerda):
  - **Bloques de interruptor** ($0D:B920), azules y rojos. Una sola rutina que
    ocupa cinco casillas de las tablas por tileset (castillo 0x39/0x3A, pradera
    0x32/0x38, cueva 0x34/0x35, cuerda 0x34): +19 niveles de golpe.
  - **Pinchos de castillo** 0x3E (horizontales) y 0x3F (verticales).
  - **Bosque** (pradera): suelo 0x35, sus bordes 0x34 y el tronco pequeño 0x37,
    que se encadena con lo que ya hay debajo.
  - **Tubería vertical INVISIBLE** (std 0x0F tipo 5): era un BUG, no una falta.
    `stdVerticalPipes` ya la dibujaba (teselas 0x68/0x69) pero el gate `< 5` la
    mandaba a "desconocida". Del tipo 6 en adelante sí falta el dato y sigue
    declarándose desconocido.

  El denominador afina una vez más: de los 215 alcanzables, **11 son verticales**
  (aún no soportados) y **8 no tienen Layer 1 POR DISEÑO** (modos 9/11/16: salas
  de jefe y Mode 7). Esos 8 salían como "100%" gratis, porque no hay nada que
  reconstruir; ahora se cuentan aparte. Quedan **196** con Layer 1 de verdad.

  Y sí, los niveles están CONECTADOS entre sí, que es de dónde salen los 215:
  28 sub-niveles los comparten dos o más niveles padre, a 27 niveles con casilla
  propia se llega además por la tubería o puerta de otro, y hay cadenas de hasta
  4 saltos (d0=92, d1=83, d2=30, d3=9, d4=1). No es que el cierre se escape: de
  los 215, 196 tienen objetos, 11 son verticales y los 8 restantes son las salas
  sin Layer 1 — ninguno es basura.

  Cola actual (por nº de niveles que arregla): cuestas muy inclinadas de cueva
  (`std:3C`, 5 niveles) — piden una primitiva de cruce de página que NO toque el
  backup del puntero, distinta de la que hay; y el grupo de guías de CUERDA
  (`ext:51-56` + `std:3A`, 3-4 niveles), que van juntas.

  Al hacerlo, el umbral de "objeto específico del tileset" baja de 0x30 a **0x2E**:
  el 0x2E ya difiere por tileset (pinchos en casa fantasma; en el resto, una rutina
  sin usar que NO se porta y sigue contando como desconocida, para no pintar basura).
  Los pinchos solo usan la entrada 0 de su tabla de teselas (0x59); cualquier otro
  índice se declara desconocido en vez de leer datos que no tenemos.
- **Nombres reales de nivel/sprite** (`SmwLevelNames`/`SmwSpriteNames`, banco $04):
  el listado y el mapa muestran "YOSHI'S ISLAND 1" en vez de "Nivel 105".
- **Herramienta `--scene`**: renderiza un nivel importado (Layer 2 + Layer 1) a
  PNG sin la app; clave para verificar el parser sin emulador.

- **Colores animados de la CGRAM** (resuelto): las tablas fijas guardan un
  MAGENTA placeholder en los índices 0x64/0x6D (dorado del brillo de monedas y
  bloques `?`) y 0x7D (rojo ON/OFF); el juego los cicla en vivo desde
  kGlobalPalettes_Flashing ($00:B60C). assembleSmwCgram escribe ahora el frame
  0 — antes las monedas de dragón salían ROSAS. Y drawSmwBlock ya no descarta
  las teselas 0xF8-0xFF (son estáticas de FG2; la tubería diagonal de YI1 las
  usa y el descarte abría huecos transparentes).
- **Tuberías horizontales (0x3F)**: el byte aparece 65.831 veces en las
  rejillas de la ROM US (p. ej. regiones 6×2 en 0x105/0x106 lejos de la salida)
  — NO es solo la boca enterable. En el juego, la entrada por 0x3F está
  condicionada por el punto de colisión que la toca y la geometría
  (`RunPlayerBlockCode_EB77` → `CheckIfEnteringHorizontalPipe`, $00:F3C4).
  Exponerlas requiere portar ese flujo (o la clasificación por puntos), no un
  filtro por byte.
- **Alas de para-Koopa** (`kDrawWingTiles_*`, $01:9E95): tesela 0x5D plegada /
  0xC6 extendida, trasera en (-1,-4) volteada, delantera en (+9,-4), sub-paleta
  3 de sprites fija, dibujadas detrás del cuerpo. Transcrito y listo para
  cuando se porte la rutina completa de la para-Koopa.
