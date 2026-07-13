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

1. **Warps de tubería/puerta, de punta a punta.** El motor ya tiene toda la
   maquinaria (`EngineWarp`, `pendingWarp`/`consumeWarp`) y `SmwWarpTiles` +
   `SmwLevelExits` saben leer las bocas y salidas reales de la ROM, pero nadie
   los conecta: la importación no emite warps (`GameMap.platformWarps` queda
   vacío) y la app nunca consume `pendingWarp` (no hay cambio de sala).
2. **Bundle de sub-niveles** (`SmwLevelBundle`): importa un nivel COMPLETO
   siguiendo sus salidas (BFS de sub-niveles + grafo de warps). Sin consumidor
   en la app; encaja con el punto 1.
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

**Tanda 2 (pendiente — cuerpo por la vía genérica, alas aparte):** Koopas
aladas 0x08/0x09/0x0A/0x0B (las alas son 2 teselas extra en pasada propia,
`DrawWingTiles`). Muy frecuentes: 84/72/58/42 colocaciones.
