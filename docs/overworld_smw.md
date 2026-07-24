# Overworld de SMW — capa estática (extraída de la ROM)

El mapa del mundo es un dominio aparte de los datos de nivel. Aquí van las tablas
estáticas legibles; el tilemap de submapas/caminos (comprimido) es una fase futura.

## Direcciones (SNES, del inventario)
- Evento por translevel (`kLoadLevel_DATA_05D608`): 0x5D608 (256 bytes)
- Star Road warps (`kOwStarPipeWarp_*`): SrcX 0x48431 · SrcY 0x48467 · DstX 0x4849D · DstY 0x484D3 (27×2 bytes cada una)
- Sprites de mapa (`kLoadOverworldSprites_SpriteSlotData`): 0x4F625 (65 bytes)
- Map16 overworld L1: 0x5D000 · tilemap OW L2/eventos: banco $0C (comprimidos)

## Star Road — red de tuberías (27 warps)

Posiciones en coordenadas de casilla del overworld (los altos indican submapa).

| # | Origen (X,Y) | Destino (X,Y) |
|---|--------------|----------------|
| 0 | (0x11, 0x7) | (0x4A8, 0x148) |
| 1 | (0xA, 0x3) | (0x438, 0xB8) |
| 2 | (0x9, 0x10) | (0x908, 0x38) |
| 3 | (0xB, 0xE) | (0x928, 0x18) |
| 4 | (0x12, 0x17) | (0x9C8, 0x98) |
| 5 | (0xA, 0x18) | (0x948, 0x98) |
| 6 | (0x7, 0x12) | (0xD28, 0x1D8) |
| 7 | (0x20A, 0x14) | (0x118, 0x78) |
| 8 | (0x203, 0xB) | (0xA8, 0x38) |
| 9 | (0x410, 0x3) | (0x98, 0x108) |
| 10 | (0x412, 0x1) | (0xB8, 0xE8) |
| 11 | (0x41C, 0x9) | (0x128, 0x178) |
| 12 | (0x414, 0x9) | (0xA8, 0x188) |
| 13 | (0x612, 0x1D) | (0x78, 0x128) |
| 14 | (0x200, 0xE) | (0xD28, 0x188) |
| 15 | (0x612, 0x18) | (0x408, 0xE8) |
| 16 | (0x10, 0xF) | (0xD78, 0x168) |
| 17 | (0x617, 0x16) | (0x108, 0xF8) |
| 18 | (0x14, 0x10) | (0xDC8, 0x188) |
| 19 | (0x61C, 0x18) | (0x148, 0x108) |
| 20 | (0x14, 0x2) | (0xDC8, 0x1D8) |
| 21 | (0x61C, 0x1D) | (0x948, 0x38) |
| 22 | (0x617, 0x18) | (0xB18, 0x138) |
| 23 | (0x511, 0x13) | (0xD78, 0x188) |
| 24 | (0x511, 0x11) | (0x268, 0x78) |
| 25 | (0x414, 0x3) | (0xDC8, 0x1D8) |
| 26 | (0x106, 0x7) | (0xD28, 0x1D8) |

## Evento de mapa por translevel (al superar el nivel)

`evento[translevel]` = nº de evento del overworld que se dispara al pasar ese nivel
(revela caminos/niveles nuevos). Translevel 0x00-0x5F son los niveles del mapa.

    00→FF 01→1F 02→20 03→FF 04→0B 05→0D 06→0E 07→0F 08→28 09→09 0A→10 0B→21 0C→22 0D→23 0E→24 0F→25 
    10→27 11→60 12→FF 13→12 14→02 15→07 16→FF 17→FF 18→4E 19→FF 1A→4D 1B→4A 1C→4C 1D→4B 1E→36 1F→35 
    20→61 21→63 22→62 23→48 24→46 25→06 26→05 27→04 28→00 29→01 2A→03 2B→19 2C→FF 2D→1D 2E→1A 2F→14 
    30→44 31→45 32→42 33→3E 34→40 35→41 36→43 37→3D 38→3B 39→39 3A→38 3B→4F 3C→17 3D→1B 3E→15 3F→29 
    40→1C 41→30 42→2A 43→32 44→2C 45→37 46→34 47→2E 48→6D 49→6C 4A→6B 4B→6A 4C→69 4D→64 4E→65 4F→66 
    50→67 51→68 52→56 53→53 54→54 55→5F 56→57 57→59 58→51 59→5A 5A→5D 5B→50 5C→5C 5D→FF 5E→FF 5F→FF 

(Traducción a texto de nombres y posición en el mapa: requiere el tilemap del
overworld; ver `docs/auditoria_cobertura_smw.md`.)

## Render del worldmap — RECIPE VERIFICADO Y PORTADO A KOTLIN

Reverse-engineering completo, **verificado por render** contra la ROM US y **portado a Kotlin**
(`SnesGameRecipes.renderOverworldMainMap` / `renderOverworldScreen`, con la capa de datos en
`SmwOverworld`). Corrige errores de una versión anterior de este documento (marcados abajo).

**CLAVE (lo que antes estaba mal):** la capa VISIBLE del overworld **NO usa Map16**. Es un
tilemap de **casillas SNES de 8×8 DIRECTAS**. Cada entrada de 16 bits = `vhopppcc cccccccc`
(tesela 10 bits, paleta bits 10-12, flipX bit14, flipY bit15) — se dibuja tesela a tesela, no
por bloques Map16. (El Map16 del overworld, `$05:D000`, es de la capa 1 interactiva: dots de
nivel y caminos, no de la tierra.)

**1. Tilemap visible (8×8 directo)** — `SmwOverworld.overworldTilemap()`:
- DOS tablas RLE, combinadas `entrada = bajo | (alto << 8)`:
  - **teselas** (byte bajo): puntero `word($04:DC72) | byte($04:DC79)<<16` → **$04:A533**.
  - **propiedades** (byte alto: paleta/flip): operando en **$04:DC8D** (`LDA #$C02B`), banco
    reusado de $04:DC79 → **$04:C02B**.
- RLE (`BufferOverworldLayer2Tilemap` $04:DABA): control `c`; `c&0x80` → run de `(c&0x7F)+1`
  del byte siguiente; si no → `c+1` literales. Salen **0x2000 casillas de 8×8**.

**2. Geometría** (`InitializeOverworldTilemaps` $04:D6E9 + `kOwExitLayerPosition_049A0C`):
- 0x2000 casillas = **8 pantallas** de 0x400 (32×32 teselas = 256×256 px).
- **Mapa principal = pantallas 0-3 en 2×2** (512×512 px): 0=arr-izq, 1=arr-der, 2=ab-izq,
  3=ab-der. Da el mapa reconocible (Yoshi's Island, Donut, Star Road, Vanilla, bosque…).
- **Área de submapas = pantallas 4-7, también en 2×2** (512×512). CUIDADO: no son cuatro
  submapas sueltos. Los **6 submapas** del juego son seis VENTANAS de cámara de 256×224 (el
  tamaño de pantalla de la SNES) recortadas sobre esa área, en 2 columnas × 3 filas y
  **solapándose** entre sí. Las esquinas están en `kOwExitLayerPosition_049A0C` ($04:9A0C,
  6 pares x,y CON SIGNO: x ∈ {-17, 240}, y ∈ {-40, 128, 296}) — la misma tabla que usa
  `HandleOverworldPathExits_SetLayerPositions` ($04:9A93) al cambiar de submapa. Por eso
  seis submapas caben en cuatro pantallas.
  (Port: `SmwOverworld.submapCamera` + `SnesGameRecipes.renderOverworldSubmap`.)

**2b. EVENTOS — los caminos que se revelan** (`LoadOverworldLayer2AndEventsTilemaps_04E453`
$04:E453 + `BufferEventTileToLayer2Tilemap` $04:E496). Al superar un nivel se activa su
evento (ver la tabla de arriba) y el mapa se PARCHEA: aparecen caminos, se abren tuberías y
emergen zonas nuevas. Aplicando los 111 eventos sale el mapa **tal y como se ve al 100%**.
- `kLayer2EventData_Ptrs_04E359` = **$04:E359** (121 words): rango de casillas por evento.
- Entradas: puntero largo en **$04:E49F** (vanilla → $04:DD8D). Por casilla `i`, dos words:
  `[2i]` = índice de origen `j`, `[2i+1]` = destino en BYTES dentro del buffer del tilemap.
- Teselas: puntero largo en **$04:EAF5** (vanilla → $0C:8000, 0xD00 B, sin comprimir).
- Props: word en **$04:DD45** + banco en **$04:DD4A** (vanilla → $0C:8D00), RLE con fin en
  `FF FF`; expande a 0xD00 B, emparejado 1:1 con la tabla de teselas.
- Parche de **6×6** si `j < 0x900`, si no de **2×2**. El buffer es intercalado
  (`[2i]`=tesela, `[2i+1]`=props); al pasarse del borde derecho de una pantalla salta a la
  misma fila de la siguiente (`+0x800`), y al pasarse del borde inferior baja **dos**
  pantallas (`+0x1000`) — otra confirmación de que el mapa es de 2 pantallas de ancho.
- Varias casillas del mismo evento escriben al MISMO destino: son ANIMACIONES (p. ej. el
  evento 78 tiene 9 fotogramas de la entrada al Valle de Bowser emergiendo del agua; el
  estado final es el último). Port: `SmwOverworld.overworldTilemapWithEvents(rom, delta,
  eventCount)` — con `eventCount = 0` da el mapa de partida nueva, útil para la PROGRESIÓN.

**2c. SPRITES del mapa** (`LoadOverworldSprites` $04:F675): 13 ranuras de 5 bytes
`(id, x:16, y:16)` en **$04:F625**, en coordenadas del ÁREA DE SUBMAPAS. GFX = conjunto de
sprites **17** (3bpp; los dos primeros ficheros cubren las teselas OAM 0x00-0xFF), paleta =
`kGlobalPalettes_OW_Sprites` (CGRAM 128+). Portados los de dibujo constante y verificado:
**cartel de BOWSER** ($04:FCE1: 4 teselas 111→108 hacia la izquierda, sub-paleta parpadeante
`((frame>>1)&6)`) y **Boos** ($04:FD70: 16×16 desde la tesela 96, sub-paleta 2). Pendientes:
los que dependen de estado de ejecución (Bowser, Koopa Kid, humo) y los decorativos que el
juego genera al vuelo (nubes, pájaros, Lakitu, Cheep-Cheeps).

**3. GFX del OW** (`UploadGraphicsFiles` $00:A9DA indexa `4*misc_level_tileset_setting` SIN
enmascarar; tileset = `kOwSubmapTileset_04DC02[submapa]` = {0x11..0x17}):
- **TODOS los mapas del overworld (tileset 0x11-0x17) usan los MISMOS 4 GFX:
  {0x1C, 0x1D, 0x08, 0x1E}** → slots VRAM 0..3 (teselas 0x000/0x080/0x100/0x180), 3bpp.
  (CORRECCIÓN: el {0x0E,0x0F,0x17,0x17} que ponía antes es el tileset **0x10**, que el
  overworld NO usa. `kUploadGraphicsFiles_FGAndBGGFXList[4*0x11..]` = {0x1C,0x1D,0x08,0x1E}.)
- Se descomprimen (LC_LZ2) igual que en `renderSmwBackground` (`overworldTileVram`).

**4. Paleta del OW** (`BufferPalettesRoutines_Overworld` $00:AD25, `overworldCgram`):
- área: `tt = kBufferPalettesRoutines_DATA_00AD1E[(tileset&0xF)-1]`, `DATA_00AD1E =
  {1,0,3,4,3,5,2}` (submapa 0 → tt=1). Fuente `kGlobalPalettes_OW_Areas` **$00:B3D8**
  (PC 0x33D8) + `tt*28` words → `LoadColors(_, dst=130, cnt=6, rows=3)`.
- objetos: `kGlobalPalettes_OW_Objects` **$00:B528** → `LoadColors(_, 82, 6, 5)`.
- sprites: `kGlobalPalettes_OW_Sprites` **$00:B57C** → `LoadColors(_, 258, 6, 7)`.
- extra: `kGlobalPalettes_B5EC` **$00:B5EC** → `LoadColors(_, 16, 7, 1)`.
- `LoadColors(src, dstByte, cnt, rows)` ($00:ACFF): escribe `cnt+1` colores desde
  `dstByte>>1`, avanzando 16 colores por fila, `rows+1` filas.

**5. CAPA 1 interactiva** (niveles con marcador, castillos, fortalezas, casa de Yoshi): tilemap
Map16 de **$0C:F7DF** (0x800 B, 8 pantallas de 16×16 bloques; bloque **0 = vacío/agua**,
transparente) con las definiciones Map16 del overworld en **$05:D000** (4 tile-words por
bloque). Se dibuja SOBRE la tierra. Port: `drawOverworldLayer1`.

**Verificado**: `renderOverworldMainMap` sale idéntico bit a bit al render de referencia — el
mapa principal completo y reconocible (Yoshi's Island, Donut Plains, Star Road, Vanilla Dome,
bosque) con color real y, con los eventos aplicados, TODOS los caminos abiertos. Los 6 submapas
salen encuadrados como pantallas de juego, cada uno con su paleta de área. La paleta de área es
3bpp, así que un mapa real sale con ~12 colores distintos (no es un fallo: es la paleta del
juego). Test: `SmwOverworldTest` (gated a la ROM local; nunca versiona la imagen).

**Exportación desde la app**: PNG estático, GIF animado (destello real de
`kGlobalPalettes_Flashing`) y "mundo completo" (mapa principal + los 6 submapas + el área),
todo generado en el dispositivo desde la ROM del usuario.

**Pendiente**: la pantalla navegable (mover Mario casilla→casilla, entrar a niveles, progresión
por `translevelEvents` — ya se puede dibujar cualquier estado con `overworldTilemapWithEvents`),
la animación del agua (`OwTileAnimations` $04:80E0: rota bits de las teselas 0x75-0x7F cada 8
fotogramas) y el Layer 3 del overworld (marco/rótulo; el decodificador de *stripe images* ya
está hecho para la pantalla de título).
