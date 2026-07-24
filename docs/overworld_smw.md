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

## Render del worldmap — RECIPE VERIFICADO (fase "tilemap comprimido" resuelta)

Reverse-engineering completo y **verificado por render** contra la ROM US. El overworld
se dibuja como "otro tileset" reutilizando la infra de niveles (`renderSmwBackground`).

**1. Tilemap L2 (mapa visible)** — ya en `SmwOverworld.layer2Tilemap()`:
- Puntero: `word($04:DC72) | byte($04:DC79) << 16`. RLE (`BufferOverworldLayer2Tilemap`
  $04:DABA): control `c`; `c&0x80` → run de `(c&0x7F)+1` del byte siguiente; si no → `c+1`
  literales. Salen **0x2000 índices Map16**, en 8 submapas de 0x400 (32×32) consecutivos
  (submapa 0 = mapa principal).

**2. Casillas-de-nivel** — ya en `SmwOverworld.levelTiles()`:
- `$0C:F7DF`, 0x800 bytes SIN comprimir; casilla-de-nivel = Map16 en **[0x56, 0x80]**
  (`04D7F2`). 92 en la ROM US.

**3. Map16 del overworld**: `kMap16Data_OverworldLayer1` = **$05:D000** (772 words). Cada
bloque = 4 tile-words (formato SNES estándar: `vhopppcc cccccccc`, tesela 10 bits, paleta
bits 10-12, flipX bit14, flipY bit15). Sub-teselas 0=TL,1=BL,2=TR,3=BR.

**4. GFX del OW** (`UploadGraphicsFiles` $00:A9DA indexa `4*misc_level_tileset_setting`
SIN enmascarar): tileset del submapa = `kOwSubmapTileset_04DC02` = {0x11..0x17}. Los 4
ficheros GFX salen de `kUploadGraphicsFiles_FGAndBGGFXList` (= `SMW_FGBG_GFX_TABLE_PC`
0x292B) en `4*tileset`:
- **Mapa principal (tileset 0x11): GFX {0x0E, 0x0F, 0x17, 0x17}** → slots VRAM 0..3.
- Submapas (0x12-0x17): **{0x1C, 0x1D, 0x08, 0x1E}**.
- Se descomprimen (LC_LZ2, 3bpp) a los 4 slots (tesela 0x000/0x080/0x100/0x180), como en
  `renderSmwBackground` (`smwFgbgVramSlot`).

**5. Paleta del OW** (`BufferPalettesRoutines_Overworld` $00:AD25, mismo `LoadColors` que
`build_cgram` de niveles):
- área: `tt = kBufferPalettesRoutines_DATA_00AD1E[(tileset&0xF)-1]` con
  `DATA_00AD1E = {1,0,3,4,3,5,2}` (submapa 0 → tt=1). Fuente
  `kGlobalPalettes_OW_Areas` **$B3D8** + `tt*28` words → `LoadColors(_, dst=130, cnt=6, rows=3)`.
- objetos: `kGlobalPalettes_OW_Objects` **$B528** → `LoadColors(_, 82, 6, 5)`.
- sprites: `kGlobalPalettes_OW_Sprites` **$B57C** → `LoadColors(_, 258, 6, 7)`.
- extra: `kGlobalPalettes_B5EC` **$B5EC** → `LoadColors(_, 16, 7, 1)`.
- `LoadColors(src, dstByte, count, rows)`: escribe `count+1` colores desde `dstByte>>1`,
  avanzando 16 colores por fila, `rows+1` filas (port en `SmwOverworld`/`build_cgram`).

**Verificado**: render de los 8 submapas con estas piezas → mapa principal (Yoshi's Island/
Donut Plains), Vanilla Dome, Star World y el mundo "SPECIAL" reconocibles, con color real.
Pendiente: portar `renderOverworldSubmap` a Kotlin (transcripción de lo anterior, reutilizando
`findSmwGfxTable`/`LcLz2`/`SnesDecoder.decodeTile`/`ArgbImage`) y la pantalla navegable.
