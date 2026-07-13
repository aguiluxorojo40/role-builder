# Inventario del pipeline SMW → Role Builder

Estado de TODO lo construido alrededor de Super Mario World: qué está hecho en
`core`, qué está cableado de verdad en la app, y qué queda. Este documento es el
mapa; la guía de uso y compilación está en [GUIA_DEL_PROYECTO.md](GUIA_DEL_PROYECTO.md).

**Rama de trabajo:** `claude/snes-sprite-color-automation-918asc` (desde el merge
de `…-59ybqy`, esta rama contiene TODO el trabajo SMW).

## Cómo leer este inventario

La app tiene **dos rutas de juego** de plataformas, y casi todos los "parcial"
se explican por cuál de las dos usa cada pieza:

- **Ruta ROM directa** — "▶ Jugar un nivel" en el diálogo de importación SNES →
  `PlatformerActivity.buildRomRenderer` → `buildEngine`. Monta el motor leyendo
  la ROM en vivo.
- **Ruta mapa de proyecto** — "Buscar niveles importables" → "Crear mapa" → el
  nivel queda como mapa+tileset del proyecto → `ProjectPlatformer.engine`.

## ✅ Logrado y cableado en la app

| Pieza | Core | En la app |
|---|---|---|
| **Colisión real por celda** (bordes de un sentido, sólidos, cuestas, pinchos; port de la rutina del juego) | `SmwBlockCollision`, `SmwLayer1` | Ambas rutas |
| **Físicas reales del jugador** (salto, gravedad, topes; tablas del banco $00) | `SmwPhysicsReader`, `PlatformerTuning.fromSmw` | Ambas rutas |
| **Punto de inicio real** del nivel | `SmwLevelStartReader` | Ambas rutas |
| **Mario con su sprite real** (GFX32, hoja 128×64, paleta de jugador de la CGRAM; 5 poses, volteo, anclado por los pies) | `SnesGameRecipes.smwMarioSheet` | Ambas (ROM: en vivo; proyecto: `assets/sprites/mario.png` horneado del mismo tamaño) |
| **Enemigos con sprite y color reales** (tabla OAM genérica del banco $01 + sub-paleta del tweaker $166E) — 24 ids curados (tandas 0+1) | `SmwEnemyGraphics` | Ambas rutas (atlas `assets/sprites/enemies.png`); la ROM directa además los SIEMBRA desde la lista real de sprites (`smwLevelEnemies`) |
| **Monedas y bloques `?` interactivos** (clasificación real del "block code": moneda=0x2B, `?`=0x21..0x24) | `SmwBlockBehavior` + `BlockAction` en el motor | Ambas rutas (la ROM directa desde el commit `2434056`) |
| **Powerup: seta → crecer → encoger** (el `?` suelta seta si eres pequeño; crecer cambia la caja a 26 px; un golpe encoge con ~1.5 s de invulnerabilidad) | `PlatformerEngine` (`items`, `playerHeight`, `powerupEvents`/`damageEvents`) | Ambas rutas; parpadeo + SFX en el renderer |
| **SFX reales** (muestras BRR de la ROM: salto, pisotón, moneda, powerup) | `SmwSoundFx`, `SmwSfxCatalog` | `PlatformerAudio.fromRom` (o assets horneados de respaldo) |
| **Música N-SPC sintetizada** (motor N-SPC + S-DSP portados) | `SmwMusicRenderer`, `SmwDsp` | `PlatformerMusic` (pista fija pre-horneada `assets/music/level.aram`) |
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
   suena una pista fija pre-horneada; el ensamblador puede derivar la del nivel
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
- **Mario grande se dibuja estirado** (fotograma pequeño a 1.5 casillas): las
  teselas reales de Mario grande de GFX32 están pendientes de mapear.
- **La seta se dibuja con rectángulos** (sombrero+base): su gráfico real vive
  fuera de la tabla OAM genérica portada.
- **Cuestas**: colisionan como bloque completo (la altura sub-píxel de la
  pendiente es un refinamiento pendiente).
- **Enemigos con rutina de dibujo propia** (Thwomp, Super Koopa, Blurp, Rex
  real 0xAB, Pokey, Wiggler…): fuera del alcance de la tabla OAM genérica
  (ids ≥ 0x54 o multi-tesela); necesitan portar su rutina específica.

## Tandas de enemigos (medidas sobre 316 niveles de la ROM US)

**Tanda 1 — ENTREGADA:** 0x4F Planta Piraña saltarina (24 niveles), 0x37 Boo,
0x3D Rip Van Fish, 0x15/0x16 Cheep-Cheep, 0x2E Spike Top, 0x38/0x39 Eerie,
0x31 Bony Beetle. Verificada visualmente con `--enemies` (voto por mayoría
entre los niveles que contienen cada id). **Descartados** 0x33 Podoboo, 0x30 y
0x32: su entrada de la tabla OAM genérica no es su aspecto real (rutina de
dibujo propia; salían tiles de fuente o basura de forma unánime).

**Tanda 2 — INTENTADA Y REVERTIDA (necesita port de rutina propia):** Koopas
aladas 0x08/0x09/0x0A/0x0B (lo más colocado de la ROM: 84/72/58/42). Se probó
componer cuerpo genérico + ala (`DrawWingTiles`, tesela 0x5D) y la verificación
visual suspendió: la entrada de la tabla genérica da el cuerpo SIN caparazón y
el ala tapa la cabeza. Su aspecto real exige portar su rutina de dibujo
completa (caparazón + alas con sus offsets), no la vía genérica.

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
