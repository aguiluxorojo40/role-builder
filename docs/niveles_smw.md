# Documentación de niveles de SMW (extraída de la ROM)

Todo sale de `Super Mario World (USA)` leído directamente. Direcciones en PC
(offset de fichero). `[B]` = enemigo con sprite grande reconstruido · `[s]` = roster
pequeño · `[ ]` = aún sin reconstruir. "Usa grandes" = el nivel coloca algún sprite
multi-tesela (los reconstruidos van marcados).

## Tablas de referencia (direcciones PC)

- **Tabla punteros Layer 1 ($05:E000)**: 0x2E000
- **Tabla punteros Layer 2 ($05:E600)**: 0x2E600
- **Tabla punteros sprites ($05:EC00)**: 0x2EC00
- **Tabla GFX sprites SP1-4 ($00:A8C3)**: 0x028C3
- **Tabla GFX FG/BG ($00:A92B)**: 0x0292B
- **Paleta back area ($00:B0A0)**: 0x030A0
- **Paleta BG ($00:B0B0)**: 0x030B0
- **Paleta FG ($00:B190)**: 0x03190
- **Paleta objetos fija ($00:B250)**: 0x03250
- **Paleta jugador ($00:B2C8)**: 0x032C8
- **Paleta sprites ($00:B318)**: 0x03318
- **Map16 FG ($0E:8000)**: 0x68000

Punteros por nivel: Layer1 = tablaL1 + 3·nivel (3 bytes) · Sprites = tablaSpr + 2·nivel
(2 bytes → dirección en banco $07) · GFX sprites = tablaSP + 4·(spriteGfx del nivel).

## Niveles con datos: 501

### Nivel 0x000
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: nivel de MAPA (translevel 0x0)
- **Direcciones**: L1ptr 0x2E000 → header 0x30654 · SprPtr 0x2EC00 → stream 0x3C407 · L2ptr 0x2E600 · GFXslot 0x028DB · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=6) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 7 · tiempo 0 · Layer2 fondo · paletas BG=5 FG=0 SPR=4 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0x7 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **BonusGame** (0x82) ×1: (0,5,7)

### Nivel 0x001 — VANILLA SECRET 2
- **Nombre (overworld)**: VANILLA SECRET 2
- **Tipo**: nivel de MAPA (translevel 0x1)
- **Direcciones**: L1ptr 0x2E003 → header 0x33A69 · SprPtr 0x2EC02 → stream 0x3CE1C · L2ptr 0x2E603 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=6 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=238 SOLID=123 SLOPE=72 SLOPE_STEEP=78
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 17→0x0D8
- **Usa sprites grandes**: no
- **Enemigos (52)**:
    - [ ] **RedParakoopa** (0x9) ×25: (1,16,23) (1,17,22) (1,31,23) (2,36,22) (2,40,21) (3,50,21) …
    - [ ] **BobOmb** (0xB) ×4: (18,295,19) (18,296,19) (18,297,19) (18,298,19)
    - [ ] **KoopaKidBossFight** (0x13) ×11: (10,170,15) (12,207,20) (13,211,22) (14,233,23) (14,234,23) (14,235,23) …
    - [s] **PipeLakitu** (0x4B) ×2: (11,182,21) (17,274,21)
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (8,131,22) (8,139,22)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Sprite 0x92** (0x92) ×1: (17,283,18)
    - [ ] **Sprite 0xCD** (0xCD) ×3: (11,182,0) (12,201,0) (15,251,0)
    - [ ] **Sprite 0xD9** (0xD9) ×3: (8,136,0) (14,225,0) (17,287,0)

### Nivel 0x002 — VANILLA SECRET 3
- **Nombre (overworld)**: VANILLA SECRET 3
- **Tipo**: nivel de MAPA (translevel 0x2)
- **Direcciones**: L1ptr 0x2E006 → header 0x33C33 · SprPtr 0x2EC04 → stream 0x3CEBF · L2ptr 0x2E606 · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=3 backArea=2
- **Colisión**: 256×27 casillas · SOLID=14
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · no-Yoshi · L2scroll 1 L3 2 L1y 2 L2y 2 · secHdr [0x19 0x80 0x8A 0x80]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 15→0x0CB
- **Usa sprites grandes**: no
- **Enemigos (23)**:
    - [ ] **Sprite 0x41** (0x41) ×3: (6,103,24) (7,115,24) (9,159,24)
    - [ ] **Sprite 0x42** (0x42) ×9: (1,18,24) (1,24,24) (1,30,24) (2,36,24) (6,99,24) (7,120,24) …
    - [ ] **Sprite 0x43** (0x43) ×5: (6,106,24) (8,142,24) (10,162,24) (10,170,24) (10,174,24)
    - [ ] **PorcuPuffer** (0xC3) ×1: (6,110,24)
    - [ ] **Sprite 0xCF** (0xCF) ×1: (3,52,0)
    - [ ] **Sprite 0xD0** (0xD0) ×1: (11,185,0)
    - [ ] **Sprite 0xD9** (0xD9) ×3: (1,21,0) (5,86,0) (9,158,0)

### Nivel 0x003 — TOP SECRET AREA
- **Nombre (overworld)**: TOP SECRET AREA
- **Tipo**: nivel de MAPA (translevel 0x3)
- **Direcciones**: L1ptr 0x2E009 → header 0x308BF · SprPtr 0x2EC06 → stream 0x3C4C5 · L2ptr 0x2E609 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 16×27 casillas · LEDGE_TOP=16 SOLID=5
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x1 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **SideExitAndFireplace** (0x8C) ×1: (0,7,22)

### Nivel 0x004 — DONUT GHOST HOUSE
- **Nombre (overworld)**: DONUT GHOST HOUSE
- **Tipo**: nivel de MAPA (translevel 0x4)
- **Direcciones**: L1ptr 0x2E00C → header 0x31807 · SprPtr 0x2EC08 → stream 0x3C7B5 · L2ptr 0x2E60C · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=4 SPR=5 backArea=7
- **Colisión**: 110×27 casillas · SOLID=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x7 (memoria 0x7, buoyancy 0x0)
- **Salidas de pantalla**: pant 4→0x0F9 · pant 6→0x0C4
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **MessageBox** (0xB9) ×1: (0,9,21)
    - [ ] **Sprite 0xE1** (0xE1) ×1: (0,0,16)

### Nivel 0x005 — DONUT PLAINS 3
- **Nombre (overworld)**: DONUT PLAINS 3
- **Tipo**: nivel de MAPA (translevel 0x5)
- **Direcciones**: L1ptr 0x2E00F → header 0x31961 · SprPtr 0x2EC0A → stream 0x3C7D9 · L2ptr 0x2E60F · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=1 SPR=2 backArea=1
- **Colisión**: 305×27 casillas · LEDGE_TOP=41 SOLID=40
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x1 0xAA 0x80]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: pant 14→0x0F4
- **Usa sprites grandes**: sí — GreenParakoopa (0x8)
- **Enemigos (35)**:
    - [s] **RedKoopaNoShell** (0x5) ×1: (8,130,23)
    - [ ] **BlueKoopaNoShell** (0x6) ×1: (3,59,18)
    - [B] **GreenParakoopa** (0x8) ×7: (4,73,17) (4,76,17) (9,145,22) (10,169,16) (13,210,16) (13,222,16) …
    - [ ] **RedParakoopa** (0x9) ×2: (4,67,18) (5,85,17)
    - [ ] **Sprite 0x55** (0x55) ×3: (12,198,24) (13,208,24) (13,218,24)
    - [ ] **Sprite 0x62** (0x62) ×4: (2,39,19) (6,103,18) (8,133,22) (11,179,18)
    - [ ] **Sprite 0x63** (0x63) ×2: (12,197,17) (17,285,19)
    - [ ] **Sprite 0x68** (0x68) ×5: (9,159,14) (10,161,23) (10,161,20) (11,179,21) (17,279,16)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **MessageBox** (0xB9) ×1: (7,122,21)
    - [ ] **Sprite 0xE0** (0xE0) ×8: (1,20,21) (2,46,20) (5,93,20) (7,112,20) (11,189,20) (15,244,20) …

### Nivel 0x006 — DONUT PLAINS 4
- **Nombre (overworld)**: DONUT PLAINS 4
- **Tipo**: nivel de MAPA (translevel 0x6)
- **Direcciones**: L1ptr 0x2E012 → header 0x31BB5 · SprPtr 0x2EC0C → stream 0x3C844 · L2ptr 0x2E612 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 320×27 casillas · LEDGE_TOP=292 SOLID=155 SLOPE=32 SLOPE_STEEP=41
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xAA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x0C3 · pant 7→0x0D2
- **Usa sprites grandes**: sí — ParaGoomba (0x10)
- **Enemigos (45)**:
    - [s] **YellowKoopa** (0x3) ×1: (1,20,23)
    - [s] **RedKoopaNoShell** (0x5) ×2: (2,39,23) (5,93,17)
    - [ ] **BlueKoopaNoShell** (0x6) ×3: (3,48,23) (3,55,23) (6,100,18)
    - [s] **Goomba** (0xF) ×6: (2,33,23) (6,97,23) (6,99,23) (6,101,23) (6,103,23) (6,105,23)
    - [B] **ParaGoomba** (0x10) ×11: (6,107,23) (11,176,24) (11,181,24) (11,184,24) (12,192,23) (14,225,21) …
    - [ ] **ParachuteGoomba** (0x3F) ×4: (13,208,10) (13,219,10) (17,285,12) (18,289,12)
    - [ ] **ShiftingPipe** (0x49) ×2: (3,49,21) (3,61,22)
    - [s] **JumpingPiranhaPlant** (0x4F) ×3: (2,40,21) (3,56,21) (7,115,21)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **ChangingItem** (0x81) ×1: (12,197,17)
    - [ ] **HammerBro** (0x9B) ×4: (4,73,17) (8,136,17) (12,204,17) (17,287,17)
    - [ ] **HammerBroPlatform** (0x9C) ×4: (4,73,17) (8,136,17) (12,204,17) (17,287,17)
    - [ ] **Sprite 0xCC** (0xCC) ×1: (14,227,0)
    - [ ] **Sprite 0xD9** (0xD9) ×1: (17,276,0)
    - [ ] **Sprite 0xDD** (0xDD) ×1: (1,17,23)

### Nivel 0x007 — #2 MORTON'S CASTLE
- **Nombre (overworld)**: #2 MORTON'S CASTLE
- **Tipo**: nivel de MAPA (translevel 0x7)
- **Direcciones**: L1ptr 0x2E015 → header 0x31DC0 · SprPtr 0x2EC0E → stream 0x3C904 · L2ptr 0x2E615 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=47 SOLID=9
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x0E6 · pant 2→0x0E8
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **Thwimp** (0x27) ×2: (0,9,11) (1,16,11)
    - [ ] **BallNChain** (0x9E) ×3: (1,22,18) (1,28,12) (2,34,6)

### Nivel 0x008 — GREEN SWITCH PALACE
- **Nombre (overworld)**: GREEN SWITCH PALACE
- **Tipo**: nivel de MAPA (translevel 0x8)
- **Direcciones**: L1ptr 0x2E018 → header 0x3076E · SprPtr 0x2EC10 → stream 0x3C49D · L2ptr 0x2E618 · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=58
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x27 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x0C9
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [s] **BlueKoopa** (0x2) ×1: (0,8,19)
    - [s] **RedKoopaNoShell** (0x5) ×8: (0,14,23) (1,18,23) (1,22,23) (1,26,23) (1,30,23) (2,34,23) …
    - [s] **PSwitch** (0x3E) ×1: (0,10,15)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (0,6,15)

### Nivel 0x009 — DONUT PLAINS 2
- **Nombre (overworld)**: DONUT PLAINS 2
- **Tipo**: nivel de MAPA (translevel 0x9)
- **Direcciones**: L1ptr 0x2E01B → header 0x3162D · SprPtr 0x2EC12 → stream 0x3C751 · L2ptr 0x2E61B · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=2 SPR=4 backArea=3
- **Colisión**: 192×27 casillas · LEDGE_TOP=100 SOLID=189 SLOPE=42 SLOPE_STEEP=50
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 3 L3 3 L1y 2 L2y 2 · secHdr [0x37 0xC0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 5→0x0E9 · pant 11→0x0FF
- **Usa sprites grandes**: no
- **Enemigos (28)**:
    - [s] **BuzzyBeetle** (0x11) ×10: (1,19,20) (1,27,18) (2,39,18) (4,70,13) (7,113,16) (7,119,16) …
    - [ ] **Sprite 0x2E** (0x2E) ×2: (11,184,17) (11,186,17)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (8,130,14)
    - [ ] **Swooper** (0xBE) ×14: (1,22,16) (1,30,16) (2,47,11) (4,73,6) (4,76,6) (5,82,6) …
    - [ ] **Sprite 0xE8** (0xE8) ×1: (0,8,0)

### Nivel 0x00A — DONUT SECRET 1
- **Nombre (overworld)**: DONUT SECRET 1
- **Tipo**: nivel de MAPA (translevel 0xA)
- **Direcciones**: L1ptr 0x2E01E → header 0x32134 · SprPtr 0x2EC14 → stream 0x3C948 · L2ptr 0x2E61E · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 13 pantallas (208 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=2 SPR=3 backArea=2
- **Colisión**: 194×27 casillas · LEDGE_TOP=26 SOLID=135 SLOPE=3 SLOPE_STEEP=19
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x18 0x38 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x0C2
- **Usa sprites grandes**: no
- **Enemigos (32)**:
    - [ ] **Keyhole** (0xE) ×1: (9,152,20)
    - [ ] **Sprite 0x15** (0x15) ×6: (3,53,23) (7,112,17) (7,114,20) (10,163,19) (10,166,22) (11,176,22)
    - [ ] **VerticalCheepCheep** (0x16) ×6: (1,25,19) (4,76,15) (5,81,16) (5,92,22) (5,94,21) (10,172,25)
    - [ ] **RipVanFish** (0x3D) ×9: (0,12,23) (2,39,22) (2,47,18) (3,61,20) (5,86,20) (6,99,19) …
    - [s] **PSwitch** (0x3E) ×1: (7,116,21)
    - [ ] **GoalTape** (0x7B) ×1: (11,190,23)
    - [ ] **Blurp** (0xC2) ×8: (1,29,23) (3,50,23) (4,68,18) (4,71,21) (7,126,20) (8,129,17) …

### Nivel 0x00B — VANILLA FORTRESS
- **Nombre (overworld)**: VANILLA FORTRESS
- **Tipo**: nivel de MAPA (translevel 0xB)
- **Direcciones**: L1ptr 0x2E021 → header 0x33D0F · SprPtr 0x2EC16 → stream 0x3CF06 · L2ptr 0x2E621 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=2 SPR=1 backArea=2
- **Colisión**: 127×27 casillas · SOLID=22
- **Entrada**: casilla (8,19) px (128,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x39 0xA 0x0]
- **Cabecera sprites**: 0x8E (memoria 0xE, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x0E1 · pant 7→0x0E0
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (23)**:
    - [ ] **ThrowingDryBones** (0x30) ×2: (6,103,23) (7,113,23)
    - [B] **BonyBeetle** (0x31) ×2: (6,96,23) (6,110,23)
    - [ ] **BallNChain** (0x9E) ×3: (1,20,19) (2,33,17) (3,48,14)
    - [ ] **Fishbone** (0xAA) ×10: (1,25,21) (1,29,17) (2,44,15) (3,55,23) (4,69,15) (5,84,21) …
    - [ ] **FallingSpike** (0xB2) ×6: (5,93,16) (6,100,16) (6,101,16) (6,107,16) (6,110,16) (7,115,16)

### Nivel 0x00C — BUTTER BRIDGE 1
- **Nombre (overworld)**: BUTTER BRIDGE 1
- **Tipo**: nivel de MAPA (translevel 0xC)
- **Direcciones**: L1ptr 0x2E024 → header 0x35000 · SprPtr 0x2EC18 → stream 0x3D1F5 · L2ptr 0x2E624 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=6
- **Colisión**: 191×27 casillas · SOLID=47
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x59 0x0 0xA 0x80]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x0F3
- **Usa sprites grandes**: sí — GreenParakoopa (0x8)
- **Enemigos (33)**:
    - [B] **GreenParakoopa** (0x8) ×11: (4,78,10) (5,80,10) (5,82,10) (5,84,10) (5,86,10) (5,88,10) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×3: (2,35,20) (3,57,16) (9,152,9)
    - [ ] **BobOmb** (0xB) ×6: (10,172,14) (10,174,15) (11,176,16) (11,178,17) (11,180,18) (11,182,19)
    - [ ] **ScalePlatform** (0x8F) ×9: (1,23,22) (2,33,20) (2,46,18) (4,67,13) (5,95,13) (6,103,9) …
    - [ ] **GreyFallingPlatform** (0xC4) ×3: (2,41,18) (3,49,17) (10,161,8)
    - [ ] **Sprite 0xE8** (0xE8) ×1: (0,0,0 EE3)

### Nivel 0x00D — BUTTER BRIDGE 2
- **Nombre (overworld)**: BUTTER BRIDGE 2
- **Tipo**: nivel de MAPA (translevel 0xD)
- **Direcciones**: L1ptr 0x2E027 → header 0x350F4 · SprPtr 0x2EC1A → stream 0x3D25A · L2ptr 0x2E627 · GFXslot 0x028D7 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=0 backArea=7
- **Colisión**: 305×27 casillas · LEDGE_TOP=11 SOLID=24
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x80]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x0DD
- **Usa sprites grandes**: no
- **Enemigos (56)**:
    - [s] **GreenKoopa** (0x0) ×1: (10,162,21)
    - [s] **RedKoopa** (0x1) ×3: (2,37,22) (16,259,16) (17,272,20)
    - [s] **BlueKoopa** (0x2) ×7: (6,101,17) (6,111,17) (7,123,16) (8,133,18) (8,134,16) (10,174,21) …
    - [ ] **Sprite 0x72** (0x72) ×32: (1,17,16) (3,52,16) (3,54,14) (4,69,16) (4,75,16) (5,81,16) …
    - [ ] **GroundSuperKoopa** (0x73) ×3: (1,26,23) (7,116,23) (8,129,23)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Sprite 0xD3** (0xD3) ×2: (10,166,0) (13,217,0)
    - [ ] **Sprite 0xD9** (0xD9) ×1: (17,272,0)
    - [ ] **Sprite 0xDA** (0xDA) ×1: (2,45,23)
    - [ ] **Sprite 0xDB** (0xDB) ×2: (6,100,17) (6,109,17)
    - [ ] **Sprite 0xDC** (0xDC) ×3: (7,122,16) (8,131,18) (8,132,16)

### Nivel 0x00E — #4 LUDWIG'S CASTLE
- **Nombre (overworld)**: #4 LUDWIG'S CASTLE
- **Tipo**: nivel de MAPA (translevel 0xE)
- **Direcciones**: L1ptr 0x2E02A → header 0x343A3 · SprPtr 0x2EC1C → stream 0x3D0D7 · L2ptr 0x2E62A · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x1 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=0 FG=3 SPR=1 backArea=3
- **Colisión**: 95×27 casillas · SOLID=11
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x29 0x0 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: pant 4→0x0DC · pant 5→0x0DA
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (9)**:
    - [B] **BonyBeetle** (0x31) ×2: (2,33,24) (2,47,20)
    - [ ] **Sprite 0x74** (0x74) ×1: (5,80,20)
    - [ ] **BallNChain** (0x9E) ×6: (2,37,24) (3,50,16) (3,54,23) (3,58,16) (4,65,22) (4,66,17)

### Nivel 0x00F — CHEESE BRIDGE AREA
- **Nombre (overworld)**: CHEESE BRIDGE AREA
- **Tipo**: nivel de MAPA (translevel 0xF)
- **Direcciones**: L1ptr 0x2E02D → header 0x33EAD · SprPtr 0x2EC1E → stream 0x3CFAF · L2ptr 0x2E62D · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 22 pantallas (352 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=0
- **Colisión**: 337×27 casillas · SOLID=20
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0x9A 0x80]
- **Cabecera sprites**: 0x1 (memoria 0x1, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x0BF
- **Usa sprites grandes**: no
- **Enemigos (42)**:
    - [ ] **Sprite 0x63** (0x63) ×3: (1,19,24) (1,27,21) (2,39,18)
    - [ ] **Sprite 0x64** (0x64) ×6: (10,164,16) (11,186,17) (12,205,18) (13,210,16) (13,216,16) (15,241,16)
    - [ ] **Sprite 0x65** (0x65) ×30: (1,29,24) (2,41,21) (3,59,25) (4,68,22) (4,69,17) (4,77,25) …
    - [ ] **Sprite 0x66** (0x66) ×1: (15,249,13)
    - [ ] **GoalTape** (0x7B) ×2: (18,302,21) (20,334,23 EE1)

### Nivel 0x010 — COOKIE MOUNTAIN
- **Nombre (overworld)**: COOKIE MOUNTAIN
- **Tipo**: nivel de MAPA (translevel 0x10)
- **Direcciones**: L1ptr 0x2E030 → header 0x341C4 · SprPtr 0x2EC20 → stream 0x3D043 · L2ptr 0x2E630 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 21 pantallas (336 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=2
- **Colisión**: 336×27 casillas · LEDGE_TOP=412 SOLID=216 SLOPE=20 SLOPE_STEEP=27
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x7 (memoria 0x7, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x0C1
- **Usa sprites grandes**: no
- **Enemigos (46)**:
    - [s] **Sprite 0x4D** (0x4D) ×12: (0,12,23) (1,18,23) (2,39,23) (5,84,23) (5,84,15) (5,84,19) …
    - [s] **LedgeMontyMole** (0x4E) ×18: (4,78,17) (4,78,21) (5,82,23) (5,82,17) (6,111,20) (7,113,22) …
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (7,118,21) (9,153,22)
    - [ ] **Sprite 0x50** (0x50) ×1: (16,262,22)
    - [ ] **GoalTape** (0x7B) ×1: (19,318,23)
    - [ ] **ChangingItem** (0x81) ×1: (15,248,23)
    - [ ] **Sprite 0x93** (0x93) ×1: (19,308,23)
    - [ ] **SumoBro** (0x9A) ×5: (2,33,18) (8,134,15) (8,139,15) (17,287,19) (18,291,15)
    - [ ] **SlidingNakedBlueKoopa** (0xBD) ×4: (1,16,17) (3,59,15) (4,65,17) (4,68,18)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (4,70,23)

### Nivel 0x011 — SODA LAKE
- **Nombre (overworld)**: SODA LAKE
- **Tipo**: nivel de MAPA (translevel 0x11)
- **Direcciones**: L1ptr 0x2E033 → header 0x34783 · SprPtr 0x2EC22 → stream 0x3D157 · L2ptr 0x2E633 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=4 SPR=3 backArea=2
- **Colisión**: 192×27 casillas · LEDGE_TOP=4 SOLID=77 SLOPE_STEEP=29
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 5 L3 3 L1y 2 L2y 2 · secHdr [0x58 0xF8 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 11→0x0C6
- **Usa sprites grandes**: no
- **Enemigos (52)**:
    - [ ] **Blurp** (0xC2) ×33: (1,19,17) (1,22,15) (2,35,23) (2,35,20) (2,42,23) (2,44,23) …
    - [ ] **Sprite 0xCA** (0xCA) ×19: (1,17,19) (2,38,16) (2,40,17) (3,55,22) (3,57,17) (4,77,15) …

### Nivel 0x012 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x12)
- **Direcciones**: L1ptr 0x2E036 → header 0x30000 · SprPtr 0x2EC24 → stream 0x3E76D · L2ptr 0x2E636 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,11) px (16,176) · pantalla entrada 4 · **VERTICAL** · L2scroll 0 L3 0 L1y 0 L2y 2 · secHdr [0x5 0x0 0x2 0x64]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x013 — DONUT SECRET HOUSE ?BAAIANAXA?A?A?A?A?A?A?A2A?A?ADAHAMA?A?A?A?A?A?A2A!A?AIBTB?B?B?B?B?B?B?B?B?B?B4B9B?BABKBPBVB?B?B?B B?B?B?B?B?B?B?B?B?B??D??Y6?AFCY6EAP?D??A? ?D???L ?D??B??AK??R????????D??????A??A?????????????? ?D??????A?PAK??????????1C?I??B???A??? BD???????I1C?O?? BD??? CD?55??Q3???N?Q??N?B???N?F??N?N?DN?T??N????Y??NQL?CN?N??N??T????Y??N?BN?N???NN?N??NN?N??NFZ??NN?NN?TN?Y??NN?N???F????O????N?R N?T??T?CN?N?KN?T??N???NQ??CN?N??N??NY?? AN?N?? A?L??T??N?BN?N??DN?T??!??M??????????AG?T?H?P?!??!??F?F?EN!????!???AFA??N?Z ?2A?Z ?2??D??????NR?A?KQF6K?A!?Q?QAQ?QAQ?QAI?M??EE??QAQ?I?I?QAQ?E?QAQQIQEQEIEMMEEEEIQ????EQ??EQ??M?QAAQAQQAQA?I?IAQQ?Q??EE??QAQ?QQEQAEQEE??EEQIM?AQ?QQAEQQ?AQAQ?QQAAQAQAQAQAQAQE?EEEEAQAQQAQA?Q?EBABABABABABAABABABABBABAABBABABAABBABABABABABABABABABAABABBAABBAABBABABABABABAABBABABABAABABBABABABABABABAABBABABABABABABABABABABABABABABABABABAABBABABABABABABAABEEEEEEEAAAAAAAAAEAAEEEEAAAAAAAEAAAEAAEEIIIMMIIIIIMMIIIIMIIIMIMUUUEAAAAAAAAAAAEEIAHJKNORXZ??? ????????????I??I?I?I?IEAIEEIEIEAEIEA?IAA?I?IEAEAAAI?IEIE?II?EA??IA????EA??EAIA??EAEAEAIAIAEA????AAAAIAEAEAEA??EAEAEAIA????EAEAEAAAAAEAEAEA??EAIA??????EA??IACCCCCACCCACACACCAAACCCCCAA?N?NAM?K?K?K?E?L?K?KQNBRCR?GHSAUAT?C?K?O?K?C?EAY?E?IQW?W?W?CQV?HAX?W?W?W?D?B?BQB?B?B?N?C?NDN?K?CA?A??Z?JQZAA?Z?Z?Z?Z?WCNENBN?P?F?V?V?V?V?Q!V?V?CLAKAJAIA?CA?A?A??A?C?C?C?C?C?C?C?C?C?C?C???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????A????????????????????????????????????ABS?????T???ABS?????U??'??U???ABS?????T???ABS???????????????AB????AB?9??AB???CABV??JABX??OABW???ABX???ABV???ABW???ABW???ABV???ABW???ABV??BABX???ABW??BABV???ABW???ABX???ABV???ABV??AABX?? ABW???ABV??7ABV???ABW??.ABV??GABW??RABV???ABX???ABV????G?????G?????G?????G???HABP????B?B.???B?B.???AQB?B.?U?ABBCBBFCR????S?CQDRB?BRB?DR?Q?R?BQRJ?BRQJ?EBCDCBC?B??D?BKB?BKBRQJ?A?C?A?C?B??D?BKB?BKB??J?AAB?B??B?BQAD?ARF?BRQI?BR?C6D?R??B?C6AAB?ARD?A?I?BR?B6E??R??B?B6B?6B?A?D?AQJ?ARB?ARC?C???C6B?B6?C6B?.G?B??H?BARD?A?B?D6B?.G?D? GFFDB??D?ARB?A?C6B??E?EBC? ?B?C???B?B?D6D?R?RC?B?QE?O??? ???????R?6?D6A?D?CQ??E?E??? ?B?G????R??E6A?C6D?.?6D?FQR?? ?B?H?????R??E6A?B6D?.?6C?GQR??? ?C?C???B?B?E?????C6D?.?6C?F??? 12E?B?A?D?E??R??B6D?.?6C?D??12B?A?B8F??????BKB?OB?HR??????6C?BARB?B??D5E??R?RBKJ?KP???????B?C?G??RP???B8F?R?A6AB?I?P?0????QC?C?C??RB?B?RC?EA6???B?E???P9B5B9?C?C?CAR?B?JR??6???R?RB?D??K?D5A9CDB?DQR??B?B??C?ARF?DQRK?E5B?A?B?D??12BKBRQH?DQD??B?A9C5D9RA6B?D????BKB??G?AQBPL?0??????R?6?B?DAR12B?CRARE?PQ???O??0??0R?6?6B?D????B?C??RE?E??0?OC?D?0??C6A?B?D????B?A?D?AQBDKR???????R??C6H???R?AR?B?BRQC?A?B?J??0?0??0??C6D?R????A???B?QV?D?2?.D?C?RQQ?HBCDFE???D?DR?RQN?CBCRC?E????BC?BQDBRAQL?BQRE?F???#RQB?B?CQ??L?C??'E5A'C?A?B?C?B?L?B??G5B?BRAB?B?CQRAH?BBCBDCAR'E5E'?R??B?F?QR??QF?BQRC?E?????B?F????6?B?F???6??F?B??BKDR?6?H6B?B?B?B6B??B?B??B?B??BKCR??H6K?6?Q??6??R?F?B??B?D?R??H6B?BRQB?A?BRBACCDFCB??P?B?BR?B?A?D6E?6???B?BRC??RC?BRDAR??D?C?RAE6K?????R??6??C6H????????B?C???D6L??R?????A6??C?C???B6A?E6B?C6L??R???6?6?6?F?C?6?E6C?6?C?ARB?D????B6D?6?RD?BR?B6A?E6C???B?BJB?HRA6?6??RE?DR?6?E6B??BJB?BZBJA?B?B6B?RG?ER????D?ARBZB?BJBZG????6?RI?DR???BJB??BJBPJZYJ????R?RK?CQR?BZBJBZYCJBZC???C?BQD?BQC?BQRBJE?JYZJCYAZBJC??RCDCRARB?AQBRCDER?ZYJBZAJDYAJBZC???C?B?CR??C?CR??CJBYZBJFZA?BJBRQB6B?C.A?B6D??R?CZDYJZYGJBZB??B6B?C.A?C6D???RBJBZBJYBZBYCZBJB??B6B?C?A?B6A?C6A?BZBJBZYBJBYGJ?JYZRAB6F??R?R?C6A?B6G???ZYJZDYBZJBYCJ??C?ARB?ARD?D???RCJCYCZBYJDZB??H?EIHGFRB?EZAYCJBZBJJ?????R????B?B?RCDARB?B?0C?I?0R?6?6?RD?FR?ARP?E?A'B5A'C?D??R?B?C??RF?GA??????BKB?'B5A'D?FR??6?RG?BQRB?DR?O?BKB??B?B?D??R?B6B??B?B??D?BARB?DRA??B?C?0?B?DR???B6I??R??????B?B??B?D???0B?A?B?D?R??D6E?R???B?B??B?B?6B?M6??????????6?C6D?R??F?B?B?6B?A6C?E6A?B6A?B6E?R???F?B?B?6B?D6?6?D6A?C6F???R??H?B?B?6B?A6B?F6E?6??RB?B??H?B?B?6B?D6???D6D???RD?B??H?B?B?6B?C6?RF?ARF?B??H?B?B?6B?I6?DEDEDCBH?B??G?D?Q??B?D????C?C??QG?D????D?E?????B?D??? C?C? ?I?K?????????R?B?D?R? C?C12?L?B??C?C?R?B?F?R? ??B?B??Q?C??RB?GR?12? ?B?A?D?B?K?AAB?ARD?D?12RB?AAD?C?R?D?C?D?C?B?B6?G?A?B6A?D?A?BRA?B?D?R??BDA?B?B6AB?F?????AB6A?D?D????BDGR?????RB?A6B?E6B?B6B?RC?D?R??C?I?????R??6B?E6B?B6B?RB?B?RE?H???????RB?B?E6B?B?ARC?C?R?D?C???C?C?CR??E6C??RE?M?????????????D?ARG?ARF?M?R???????????S?E?R???B?F??????N?R???????R??????????N?E?????B?F?R??R?BRB?ARM?F??????C?A?B?B?6B?B6A?J?I?????????D?B6B?6B?B6A?J?B??E?C???C?B6B?6B?B6A?G?EQR???C8D?BOZBYAZBPFZYJ???G?D????D5D?BOJBYAJB?FJYZRARG?E????'B5A'EJCYAZB?BZCR??H?D??A?B?B?HZC??PBJC?6?H?B??B?B?C???C8D?I??JYZ?6?RG?DARQRB?B??D5D?KOJYZRA6?R??E?D??RQB?CR?'B5B'?CJAYBZH????R???E?E?6?RDBQB??B?B?JCYAZB?DR??RB?B?B??B?BR?B6A?CRB??B?A?DZD?R??B?ARB?B?B??B?BR?C6B??J?G??6?6?RB?B?B??C?BR?D6A?H6A?C6C??RC?B?B??D?A?D?C?6?B?A?D6A?B6B??D?C???E?A?C?D?#6#BKA?C6A?C6B?RD?C???E?B??B?D?#6#BKA?B6H?????QDQC?A?B?C?D?B?H?R?????AB?AAB?HA?AR?A?AF?L??RD?????R??D?B?C?B?C?6?F?L??????R?????D?B?C?B?C?6?C?A?BDL???????????RD?BR?C?F?R?6?RB?P??????????????R?B?A?BRC?BRD?6?RB?PR???????????R??RB?KR?R???R?R?RC?N?????????????RBDA?D?CR?RH?R????????????????R?N?T?????????????????RD?L?T??????????????????R?B?B??H?X????????????????????????H?W??????????????????R????I?T??????????????????R?L?J?????????RD?HR??R????C?B??E?J??????????D6A?BRA?B?G???????E?B??B?BRD?R??D6A?B?C?E?????G?B??B6B?D6???D6F?D????H?B6?B6B?D6???D6E?C???J?D?D?6??B?G??6????M?B?D?D?6??B?G??6?E? CDEFGH??E?B?B?F???6??B?H??6??? ?B?E???? E?B?B?F? ????B?H?????? ?B?E?12? E?B?B?F? ????B?H????R?4?B?C?C14?D?B?H?K? ?34?B?H?43?R1? B?B?E??12KC?B?H??12R?!?B?Q?!?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?BQ?BQC?Q?BQF??????D?EYC?YYE?DQA?CQE?AYE?AYC?DYU??D?FQA?BQB?AQC?A?E?A?B?DYU??C?KQD?EQB?YC?DYU??C?JQB?BQFQB?A?B?DYU??C?FQA?EQB??BQA?DQA?BQE?YU??C?B?QBQB?QDYCQ??CQA?BQAQB?BQC?U?BUC?A?BQD?QQ?CQE?QQ??BQCQ?QB?CQA?C?C?A?CQC?Q?CQBQ?BUH??YQQ?Q?BYBQ?C?C?A?CQA?B?C??YCUB??BUA?CQDYAQC?B?EQB?EUF?BQBQQEYBQAQB?EQC?H?BQ?DQA?CYCQ?QC?DQDQ?Q?G?BQ?IQB?B??B?B?QCQD??QUE?DQA?DQBYQB?B?YC?A?DQB?BUE?DQA?EQB?C?A?B?A?DQB?D?AQB?HQB?A?C?BUE????QBQC?C?DQAYDQDYQ?YC?BUB?Y??AY??BY?V?DQD?AQB?Q?HQD?BQQB?N?JQB??B?CQB?L?BQE?DQB?B?B?CQQ?L?CQEYA?CQA?B?C?BQ?L?BQQFYA?B?B?QB?B?CQ?QH?DYA?BQEYB?QB?AYB?B?QB?BY?F?EYA?HQB?B?YB?FQQ??Y?IYE?????B?F?B?YB?B?YB?BY?GYBYA?B?BY?B?E?HY?Y?????BYA?F?CYB?YC?B?F?B?YB?A?CYB?YDQB?BYA?CYA?CYA?D?EY?YQ?CYB?BY?DQE??YY?DYCY?YE?A?BQA?BYEY??Y?C?BQA?HYB?YD?AYCQB?YBYCY?YDUB?A?B?A?E?B?YC?DQB?F???Y?YF?C??YE?CY?YHQB?YB?BY?BUD?B??B?A?E?A?CQC?Q?BQF?Q???YBUAYD?B?B?YE?AYBQF?Q?Q?QB?BYB?BYUG?C?A?GQF?Q?Q?QB?DYU??BUB?YF?CY?UCQA?CQC??QBQC?CY??BUB?EYD?DQC?Q?BQH?Q?Q?Q?UC?CQ??CYBQ?C?CQA?BQB?CQ?QB?EQ?Q?UC?BQAUBYBQB?QB?DQN?Q?Q?Q?Q?Q?Q??CQA?BUBYQDQCQB?QB?HQ?Q?Q?Q?BQC?B?BYCUA?B?AYBQBQA?BQA?BQK?Q?Q?Q?Q?Q?B?BYCUA?C?A?CQEQ?Q?QB?BQB?FQ?Q?Q?B?BYCUA?B?AYC?BQC?Q?BQG?Q?Q?UQB?B?QC?BUCY??C?A?B?EQA?BQB?QB?CQ?QB?CYU?BUBY?F?DQG?Q?Q?Q?BQD?Q?QB?AYH?D?DQC?Q?BQG?Q?Q?Q?BQC?Y?B?A?CQB?QD?JQB?C???BUD?E???Q?GQBYA?EQB?B?YCUF?A?BQB??CQC?QQBYA?DQB?BY?BUG?DQB?A?BQA?HQB?B?B?UB?BUD?B?QBQA?DQA?EQB?QB?B?AYHUB?A?CQB?YCQA?DQB?A?D?BUA?BUB?BUB?F??Y??YGQB?C???B?DUF?B?E??Y??BYA?E?A?B?A?B?BUA?BUF?B?H??Y??Y??D?BY?B?A?EUH?B?E??Y??BYB??D?B??CUB?BUH?B?H??Y??Y??D?AYCUD?BUH?B?G??Y??Y?GUF?BUH?B?F??Y??YEQB?H?BUG?C?Q?BQB?AYGQA?G?DUD?DU?Q?BQB?HQA?I?BUA?BUJ?U?Q??Y?YQGQA?L?BUC?AQB?BY?IQA?Q?BQA?KQA?D?BY?K?A?CQGQA?BQAQD?AYB?D?BYA?C?B?A?HQA?B?AYD?BYB?B?FYA?B?B??GQAQB?AYD?A?KYA?B?A?B?E?BYB?BYUC?LYB?B??B?E?BYB?BUB?MYB??BUB?E?BYCUC?BYKYC?B?A?E?AYBUE?A?LYD?A?HUF?MYS?BYKYN?BUA?BUA?BYJYN?EUB?BYBYA?BYA?CYM?EUD?CYD??Y?B?AYJ?BUA?FUD?B?DY?Y?B?AYJ?BUE?CUC?B?DY??YB?A?G?DQD?GQC?CQD?Q?UG?DQDYEQGQ?Q?Q?QB?C?QUG?DQAQBYH?Q?Q?Q?QB?DQ?Q?B?AYH?CQCQCQK?Q?Q?Q?Q?QQB?B?YH?HQ?U??YQQD?DQC?QQC?C?YUG?D?Q??BUB?QDYEQAQB?C?Q?DUE?A?BQA?B?C?QQBYA?BQH?Q?Q?Q?YBUA?BUE?B??BQB?DQA?BQB?QB?BQB?BYUB?B?BUB?B??B?BQA?DQA?BQD?Q?QC?C?YUB?B?BUB?B?C?LQB?C?Y?BUB?B?BUC?B?D?A?H?AYC?AYBUC?B?BUD?EYB??CQA?D?A?B?BYUD?A?BUE?EYB??BQB?C?AYC?BUD?CUE?DYCY??BQB??B?BYA?BUBQA?C?AUB?BYA?B?BYCYC?U?BQAQBUH?YYU??QQF?BYB?YB?CYB?A?B?BYB?B?AYB?C??YF?IYC?Y?B?BYB?B?AYB?C??YC?LYC?Y?B?BYBU?B?GY????YUB?LYB?YB?B?YBUA?B?BYUB?A?BUB?LYB?B??CUA?B?A?BUB??BUC?MYA?BYA?D?A?BUH?PYB?N?QYC?Y?L?RYB?B?BUH?RYC?Y?CUH?RYB?A?BUI?RYB?L?IYA?EYBYB?AYBUC?BUE?IYA?D?BYB?BYGUE?BYCYA?BYBY?D?C?Y?CYA?DUG?BY?B?CY??BYA?D?B?DYDUI?AYB?C?Y?BYA?D?B?CYCUJ?DUA?DQC?BQAUBYMUBQDUA?DQC?CQBYFQBYEUBQBUBYA?DQC?CQBYBQB?QDYEUBQBUBYA?DQD?BQBYBQB?QDYEUBQBUBYA?DQB?DQ?QQCYBQ?CQCYDUBQBUQBYA?BQBQB?B?BQCYFQBYA?CUBQBUQBYB?CQC?CQCYAQB?BQ?CQA?CUBQCU?QBQB?BQB?BQFYC?BQ?BQA?CUBQCU?QBYB??BQB?BQQGYB?EQ?Q?QCUBQCU?QBYB?BQB?BQFYCQAQC?DUBQB??DQA?BQB?QHYDQC?DUBQB?CQA?BQC?QQBYAQDYBQHYQ?QQ?UUBUBQIU?Q?QQQ?QBQE?BYHQ?Q?QQY?DUBQBUBQEQ?Q?QFQA?BYBQ?BQDQYQ?DUB?BUDQB?QBYA?EQBYBQ?BQBQB?DUB?BUA?BUBQA?BYA?CQB?QBYDQ?Y?HUB?BUA?BUBQF?Q?Q?QC?FQ?Q?Q?HUB?CU??BUCQQ?BQFQB?DQ?Q?DUAUCUB?AUC?BU?OQAQHUB?CUB?UB?OQHUB?EUB?OQHUB?AQB?GQA?IQA?DQA?BQB?LQA?DQA?GQA?CQC?EQB?QDQE?Q?Q?HQD?BQB?EQA?DQB?BQ?EQA?FQB?BQB?CQB?QB?CQA?BQA?DQBQ?DQA?BQE?DQC?CQA?BQA?BYIQC?QQD?EQC?FQBYGQDQB?QC?GQA?CQA?BQCY??EQCQBQB?QC?FQA?CQB?BQB?A?GQCQC?Q?GQA?HQC?U?EQC?Q?BQA?HQB?FQD???UB?IQA?LQA?WQBUFQB?VQBUBQ?EQA?VQBUBQ?BQCQA?RQB?BQBUBQ?BQD?SQA?BQBUBQ?BQA?B?AQOYGQBUCQB?B?AQOYB?QEQBUBQB?BQB?QOYA?CQCQBUBQB?BQB?QGYA?EYB?YC?AQC?BUBQB?BQB?QGYA?DYB?H?BUBQB?BQD?QY?EYA?CYB?BUG?BUCQA?BQD?QY?DYFYB?UB?F?BUBQB?QB?CYA?DYBY?BYBY?B?BYB?D?BUC?AQEYA?KYA?DYA?C?BUC?AQEYA?EYA?CYA?CYA?FYBUD?Q?QBYBYBY?DYBYCYA?CYA?BYA?CYBUE?Q?Q?BYGYBY?KYB?AYBUC?CQU?CYBQ?GYBQ?BYEQ?YQ?CYBUE?Q?QYB?A?BQB?BYEQA?CQA?BQA?CYBUC?DQUY?DQA?NQA?CQBUC?Q?BUAYXQBU?QB??QB??QB??QB??QB?CQAUBQBUBQCUBQAUBQAUBQAUBQAUIQIUQUQUQUQUCQKUQUQUQUQUQUIQAUCQEUQUQUCQAUCQGUQUQUQUJQAUBQDUQUQCUBQUCQBUQCUBQUKQBUQBUBQAUCQAUCQGUQUQUQUIQEUQUQUCQAUCQKUQUQUQUQUQUGQBQHQCYB?A?DUB?A?BQA?IQA?DQFYA?FYB?CQQ?HQB?KYAUFYCQA?HQB?IYA?BUFYCQA?IQB?FYD?Y?UGYCQA?JQB?DYDY?Y?HYDQA?JQD?YQ?BYB?YB?GYBQQBQA?JQA?BQB?FY?YQ??BYFQB?QOQA?DQA?HQB?Q?QCQHQB?A?B?B?B?B?AQB?B?BQ?B?B?B?B?BQ?BQB?CQB?B?B?B?AQB?B?BQ?B?B?B?B?BQ?BQD?B?A?B?B?B?AQB?B?BQ?B?B?B?A?B?B?AQD?B?EQA?GQA?B?B?CQB?B?AQD?B?B?B?BQ?CQG?Q?Q?Q?BQB?B?D???QD?B?B?B?EQI?Q?Q?Q?Q?B?B?DQD?B?B?B?CQC?Q?GQA?B?B?B?B?QD?B?B?B?CQC?Q?B?B?DQ?Q?B?B?B?B?QD?B?B?B?CQC?Q?B?B?DQ?Q?B?B?B?B?QD?B?B?B?CQC?Q?B?B?DQB?B?DQD?A?B?B?BQA?CQC?Q?BQJ?QU?Q?Q?Q?BQB?CQB?B?BQA?CQC?Q?BQJ?QU?Q?Q?Q?BQBYB?B?A?B?DQA?DQA?BQA?CQA?B?B?DQB?B?BQ?EQA?BQC?Q?BQA?CQA?B?B?DQB?B?FQ?Q?Q?DQA?BQA?BQA?B?B?A?B?BQBYBQ?B?B?DQ?Q?B?B?CQA?BQA?B?B?BQA?EQA?B?B?DQ?Q?B?B?DQ?Q?B?B?A?B?BQA?FQB?B?DQ?Q?B?B?DQ?Q?B?B?BQ?B?B?FQA?CQC?Q?BQA?B?B?BQB?B?BQ?B?B?BQBYBQA?CQC?Q?BQA?B?B?BQ?B?B?BQ?B?B?BQBYAQB?B?BQB?B?AQB?B?BQ?B?B?BQ?HQDUA?DQC?BQPUEQAUBQAUCQCUBQAUBQFUQUQUQCUCQBQ?QB?UDQB?UDQB?UDQB?ULQB?UDQB?UDQB?UDQB?U?QC?BQ?DQB?UDQB?UDQB?UHQAQC?BQ?DQB?UDQB?UDQB?U?QD?AUI?BY?B?B?UJ?BQB?BUG?IU?YQ????UC?AUF?BQA?CUC?B?UC?AYBQB?A?L?BQA?CUC?B?UC?BQAQBQD???UD?B?UC?BQK?AYCQA?BQA?F?B?UC?BQC?HYEQC?Q?GYA?C?BQC?U?BQA?EQA?EQC?Q?EQB?C?BQF?U?QQ?EQA?FQBQ?EQB??C?BQC?A?HQAYDQAYEQC?Q?D?BQE?U??QFQAYFQAYCQD?Q??D?BQC?C?Q?CQC?QYFQAYCQBQ?F?BQCYA?BQAQBQA?BQAYDQAYBQGQ?Q????CYEQC?Q?IQA?DQA?BQA?C?CYDQGY?Q?Q?QEQAQEQA?BQB??B?EQBYBQ?BQB?QFQB?QB?FQC???CYCQBYD?Q?QFQB??CQB?FQA?PQF?????QBQAQB?CQC?Q?HQBQ?BQA?B?D?A?BQB??BQB?DQ?Q?DQAYEQC???C?A?B?A?CQD?AQCQA?DQBYBQB?C???C?A?B?CQA?DQA?BQB??FQC?Q?G?A?B?A?GQA?BQB?JQA?WQBUA??????A?????????????AAAAAA?AA?A??A?QAA?AAA???????A?AA??AAAA???A???????Q?AAAAA?????AA?????AA??AA????AAAAAAAAAAAAA??1????F?F??CCF???N????A??R ?PA?K??AF??ACF???IF??HA??3???Y6QAF???B?4??F?1???Z1????F?F????ANV??W???NX??G??#E?Q??Q.??N????R ?F??NT??CNL?8 A?? ??? ??? Q?? ??? ??? ??? ??? A?? ??? ??? Q#? ??? ?'? ??? ??? A0? ?1? ?3? Q5? ?6? ?8? ??? ??? A?? ??? ??? Q?? ?!? ??? ??? ???5????AA???AFN??FO??FP?AFK??FL??FM?AFE??FF??FG?BAEA??H?AXKXNIQ??AA??E??QR?B?N?AXN????EXK3A??AI?01P??#3P?P?????AAAAAA6E?E?EJE?A2A?A?AFA?A?A?A?A?AHCAA?CSFIG0E?E?G9EMG?G?GAA?E?A?B?B?B?BAA?BEB?BAAAA?G.G?G?G.GAAAA?G?G?GAAEDAAAA?F!F?F?FAA?FAA?FAAAAAAAAAAAA?DAAAAAA?C?CAA?CAAAAAA?E?EAAAAAAAAAAAAAA?F?FAAAAHHAA?B5D2D?DAA?H?H?H?HZHWHTHRHAAAAAAAAAAAA?S?W?S?S???K?K???K?S?W?#?#?K?OAA?O?E?Q?G?Q?U?Y?Y???UAA?A??????????AA???I??AAAA?U???Q?Y??AAAA?I?U?IAA?IAAAA????????AA??AA??AAAAAAAAAAAA??AAAAAA????AA??AAAAAA?A?AAAAAAAAAAAAAAA????AAAA?OAA???Q?K??AA????????????????AAAAAA???????????D??#.??????3456789????????!?0??A????P??A?????P?HA????C ??1E?????A?FE?P??AK??'?E??VA????FG?E??#E?G?Q!??#??#EXE?VA?D?XE?P?!3?Q?G??P??6?I?7??8BYA??A????Q?AFD?A?Q??A A??55?DQ??6#?D??FD??A A??55?DQ.?1OQ?????5???GAY???????????Y???A?AHAAA????FAAA????M??N?Y?I?NN???M?'I?QT?P?Y?Q?NP???U?'Q????M?NN??U?NP??5?K?CA?OM??D?T????Q?KFA?O?Y6A8?A??O????A??1AQD??AZ?E?YF55II??????F???F??AN N???N????R ??C?EN?Z?U?A??NDH?BNEH?DI?GNAG?5??CDEGHJF?5??EN?T??N?????N?R????MY?B??R 'R ?O?R ????EN??????RSTUVWX????N????R ??C?EN?Z?RN?Z?HN?Z?DF??Q?AAK?????B?????A?FA?AA?A??P?AY6IAFA55?AE?9L??H??!?A???M??????8????'?????FA???EFC?Q?A?EO?AA???#?????FA???BA?AA??#???AFP??13P?P?????A??QIECBI?Q???A?G2?FF4?AAGA?5??FB????N????A????R ?H?BY6EFB?A?A??FC???A??FD?CKKK??A??AKK???AFC5K?APFC??2 A1????2 ?1????2 C1????2 ?1???3A?A?????5????Q???ANEC?MFE?AA???'??????C?FF?A?P?C AA??5?FQ??D'?F??FF?C AA?5?FQ!????C??????AJ??EJM?IJ??MJO?QJ??UJQ?MBC??BC??BC??K???K???KG??KE?AJ1??J???J??YJ???J???J9??J??ML??QL9??J9??J5??J1??J???JQQ?J?Q?JMQ?JMH?JMH?JMG?JUQ?JUH?J?H?JMGEJMF?JOF5J?G5J?GYJIG9JSF?J?E?JSEAA?E?AYE?A?D9A?DQA?D?A?DQFYF?J?F?LUH?J?F1J7F5J?F9J?G?J?G?J?F?J?F5B?H?K?P?A?H?A?H?B?H?B?H?J?G?JUG?JUGAJUFEJYHIJ?H?J?HMJ?QQJ?QUJ1Q?J?QYJE??JY?EJ???J???J???J???JO??JQ??J???JM??JM??JM??J???J???J???J??AK1?EK??IK??YJ??YJ???J???J9??J???J???J???J???J???J???J???J???J???J??YJ???J???JM?1JM??JE??J???J??YJ?BML?B?LIJ?L?JQKKJQK?JMKMJMK?JQKOJQK?JMKQJMKYJQKSJQKWJUK?J?D?IYK?J?K?J?J?K?K?K?K?L?K?L?K?M?J?M?K?N?KYK?K?KULQLYLQL?K?L?K?L?K?L?K3L?K5K?J5K?K???K5??K5?YJ???J???J5??K???J???KY??K???K??YJ???JY??K???K???K5?UJ???J???J???J???K???K???K??YJ??YJ??YJ???K#??KY??KY??K???J??1K???JY??JY??JY??JY??JY??K???K???J???J???J???K??YJ??YJ???KQ??KW?9KS??K???KQ??JQ??JQ??K???K??YJU??JY?YJ??YJ??YJ???KQ??J???JQ??JQ??KQ??KO??KK??KC??J???K???JM?YJI?YJE?YJA?QKSYUK??YK???K??UK??YK???K1??K??YKY?YKW?YKU??KQ??K???K???KQ??KQ??KS?YKU?YKW?YKY??K???KQ??LQ??LU??LY??K???K???K???K1??K???K1??KQ??JM??K???K#??K???JY??KQ??KM??JM??KK??KG??KE??JE??K???KM??JI??JE??KA??J?W?J?W?J?W?J?W?J?W?K?V?K?V?K?T?J?U?K?U?K?T?K?T?K?S?B?R?C?R?C?R1C?RIC?R?C?R?C?R?C?RYD?R?D?R?D?R?D?R?KQ??K??1KM?5K??9KU??K???KY??K#?YJ??YJ??YJ??YJ??9J???K5?AL??1K9?1K???K???LQ??LU??LY?EL??IL??ILU?IL??ILM?9JI??B?H?J?Z?L???D???D??UE???E???E??AE???E???E??9E???K???L???L???LY??JU??JU??JU??KU??L???J??YJQ?YJU??JY?YJ??YJ???J??YJ??YJ???J??YJ??YJ???J??YJ??YJ???J??YJ??YJ???JY?YJU?YJQ??JM?YJI??JE?AAAANANAQAVAYA?A?A?A?A?A?A?A?A?A?A?A?A?A?A?A?A?A'A?A4A7A9A?A?A?A!A!ADADAEAOAQASAYAYAYA?A?A?A?A?A?A?A?A?A?A?A?A?A?A2A4A5A?A?A?A?A?A?AABABABABABCBIBPBSBUBWBXB?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B2B5B8B?B?B?B?B?B?B?B?B?B?B?B?B?B?BA??QIECB???P?H??P?????C ??1E?B??PK?????0EN8???0EN???8??Q?W1???8??8????????????8?KK??N'E??P'EFE????FI?MFL???AAFG?AAFJ?AJQG??1??1??2????BAFA?E?BAFM???J A??5?G A???5??K??A?K?K???Y6AI??MQ??E?Y6?AFE??H?KK???Y6AQFE?AQ???FAFA?E?FAFM???J A??5?G A???5??K??A?K?K???Y6AI??MQ??E?Y6?AFE??H?KK???Y6AQFE?AQ???G???GA?288?3?3?7?98896???#?????U???U?Y?O???????Y?????M!????FGAQAFGBBBZE?A?G?B?B?A?G?G?F?F?F?C?CSCYDGDGP???????????C????'0!??A??N?C?D?7??6????7????W?7??H??7??????C ??1E???H?32??T?O??T??N?N?PN?N??Q4?FN?T?AN?N??G??7??!3YKKKKNC?Y??ND???NE???T?Y?F??N???G??C?F?WN????????X??2E?J?Q!?CNG??O?TKK???FM???A?FK??2E????EA?K??2E?G?Q!?E3KN?T?DA?O??2EXK??YY6QA?????2EXK??G??7?K?????0EN8???0EN???8????G?G??G????7??FO???8?KK??N'ENE??P'EFA?? ?Y6A?8FC?A???????ND??A??KKNC????A?FM???FK?E??AJQG?94??4??5??A ?D?KP?D??W1???VN???G???BAFG??D???C ?D?55?AD?D? AFI??A?2IFI?BA?G?IK?8?Y ?D?55?BAFE?A?M?K ?D?55??Y??A???E??IY???Y6AI??C8???Y6AE8 ?D?55?IK?8 ?D?55?EQ??C8Y6?A8FC?A?Y6?AFA??H??Y???Y6AQFA?C8?6?A? ?Y6AI8FC?G?D?.4??FAFG??D???C ?D?55?AL?D? AFI??A?2IFI?GA?O?IK?8??GA?2IFIY ?D?55?FAFE?A?M?K ?D?55??Y??A???E??IY???Y6AI??C8???Y6AE8 ?D?55?IK?8 ?D?55?EQ??C8Y6?A8FC?A?Y6?AFA??H??Y???Y6AQFA?C8?6?A? ?Y6AI8FC?G?D??5?GGGGGGGGUUUUU????SSS????????????BP??9PP????'AAAAAABBABBBBBBBABBAABBBBBBBBABAABBBBBAAAAAAVC?C?C?C2C?CURUQ?A?F?F?H?G?B?B?B?AA?A?QF?B?Y?Y?Z?Z?Z?Y?Y?YXF9??FSCU?????W?U??G?B?BEDED?C5A?A?A?AAABA?J?J3A?J?J?J?JYJYJ?J3A?J?J3A5AAKEKIKYJYJUJYJMK3AED3A?A?K?J?J?J?K6A?A?A?A?A?A?7?FP???15E???Q!?G????G???T?7??H??7??????C  ?1EZC ?? ?6??#?Q6EFCKK????U6EFA??6EFE?C?BA?K?Q?A??1?#7???Q?E?A A??????P??6?VU?UU???DH?HJ??J?Q????Q?DH?HJ?I?.K?6Q?Q?????ANFJ???N!J???6J?DNAG??VU??Q??G?????W1???8??8???????F?DNG???49???MA?GA?E??AJQG?MA?CAGF????FFD?AFC?BZ?D?AAMZ?D?AA???FE?AA????FEJRZ?D?CZ?DY6I5IIII?D???BY6IFB??????Q????E?Q??I??N?R ?D?C?BEK?VU?G?A?K???L7?R?V?R?U?R?U.Q?T!P?S?O!R?N?Q?M?N?K?LBHBHAIAI??RS??QR??QR??PQ??OP??MN??LL??KKAAIIBBHHAAII.?.?.?.??.?.?.?.??????????????????????????????????????????????E?QE?G???E??G??B?Z?Q?F??D?5????#8FC?49????9?Y?F????5????????8FC?49?DFD?A?AY??8ZAC?BY?C8ZBC?CZCC??8ZDC????5?DQ#??E??E??E??E??C??2?FA?D?Y2?FB???FP???A?FN?7???AK??'?E??VA???N??#E?L?Q!?Q?HNG?????BN???G??7???K??'?EKKKKNC??'?E??ND???NE??GZGZG?G?WZWZW?W?G?G?G'G'W?W?W'W'GZGZG?G?WZWZW?W?G?G?G'G'W?W?W'W'IVYVJVZV?R?R?R?R?RQR?RRR?R?R?R?R?R?R?R?RCZSZDZTZ?Z?Z?Z?ZA?Q?B?Q?AUQUBUQ??R?R?R?RC?S?D?T?GZGZG?G?GZGZG?G??R?R?R?R??FP???A?FN?7???AK??'?E??VA???N??#E?D??!??GO?7???AK????EFA?'?E?#?O????#E? A???EFM????9FK?OKKK???D???A ?D?Y6A? FD??AD ?D? HD??K BD????K JD????K DD????K LD???A ND?KY6QAP?D????????FO???8?KK??P'EFA?? ?Y6A?8FC?A?FM???FK?N'E?AJQG?94?8???5??A ?D?KP?D?????BC??BD??BCU?BDU7B7B7?7?7B7B7?7??B?B?B?BKV?VLV?V????FP?EFM???A?FN???FK??T??AK??H2EFA??D????T??A?DA??KKK??A ?D?Y6A? FD?8Y6?A8FA?AD ?D? HD??K BD????K JD????K DD????K LD?KY6QA???T??A?CAQGKKK?AD??A?3???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????A??????A???AA?????U?MQE??T???U???I?5??N???HFA??U??N?X  ????Y  A???Z  ?????  ????A ??? ????A?A? ????I? Y???? ???5?AQ???QR??UY6???QC?AN?U?AO?U?QN?U??U??U??UFP?A#?A????X??X??????Y6B?G??QC?? ???????8??????Y2CFC??8FB???P8Z?D?AZ?D?3Z?D??T?KJ?Z?DY????CZ?E?PY6E??QC?AFP59?UQT??U?FQE??QI?KY6??#????KKKKY?5?? 5??????I?????A?QDJ?I???? ???8Y?A?? A??8??????A?6IAZAA????????GA??V?T?V???V???V????A???N???H?1?1?1?? N?E?5A?A??N??NQB5OK??I??J??T?E???H??H??H?E??ES??D?I?H??I?A??Y?R?ENJ?Y6A8?Q??B????E??E??Q.?Q?J???J??????A??J?????Z?F?QFII??Q??BH????F????V????????E???????C???????????AJW?K???S?Y???AJV???S?Q?S??LAF??A?A??UAF??A?A??PAD??????AD??????ADF?F6??ADF?F6???????????????????NN?N?Q?O??N??NN?N?T?A???N???J??N???????BK?B?9K??F?YNL?OK?K?B???N?V?T??N??QM?Z?N??N??N???????N?C?Q?N?C?K?N?C?K?N?C???CN?EN?E???F??YY??D?P?D????? ?D??IQ?5????N?L??? ?D? ?D???N?L??? ?D? ?D????L??L??Y???K?K????D? ?D??H? ?D??L? ?D??P? ?D?????N?O.KY6K???N?#??XA??C???? ?D?Y  D??AAB?AAAB?AG?B?AH?AKBA?A?AIIBYAJ?B??AAAABA?A?BDAAAAK?AYAK?A?AK?B?B?AABQ??A??QAB?AL???M???W.?5N?B?E?C?F????O?X.??O?Y.?5O?Z.??O??.??OY?6F??Q??N??O??????O?2.?O?N???5???5?QQ0?8???????????A?A????AA??AA?A?B?B?A?B?B?A?B????Y???HFGHEGHF?!?C????? ???T?Q??? ????I!?H???.N? ?A!M? ?IN? ?YN???? QI?? ?EN? YK??BG?C?CG???DG??H?EG??H?FG?GGKY6ENBG?C?2N???F?2N????H???.??OYK?????Y??.????O8?2O????Y?5.????O8??O?Q??EK??N?Q??EGAGB?A??N??OFC??OFD?B??N??O8??O???C??QN#?A??N??N5Z?N????A?????N?N?!O?P???N?I??O?D??O?FQIO?N???AM#??N?O?N?????Q???????!?!!AA??Z??DLU??N??H ???????????????1??????2N??GA????????????Y?????????A??QIEC?2N???FA??T?K?H?5??B?R??N????R ?????A?C?B?BBDBBBBCMMSSSSMMQAIA?A?AQA?AIAQAB?Q?Q??Q?Y?A???????CFE??????G?Q?B5GG?AY???FAQC3B?EY???FC?F6AFD??8??????G??Q???N?????8??????VO?D????FO?B???OY?????O????I?FO?B?FO?????N?EOK?AQK?G?????A?Q?EO?CKQI?I????AA??QO'FO?FO????G??NKY6Q???NK?T???N?FOKK?A?B??VOY???????D?VO?????IHEHABEBBHJHY?A???????A?B?B??VOY???????D?VO??N?Z ??O?? ??O?Q???????VO?CJ??8?????58???????D?????VO?D???Y2AFAQC3B???Y2CFCQC3D???Y??????ABA?P?AAB??0???N????T?6??D????????O????2O?????O?????O????7???JS??N????O?ON???P??O???O??O?1?D????Q???O??O?D??N?????N?I??OQE???Q8Y???T?C??Y?G?????6IFC?C?D??8??O????#????????????AY6IAFA???.85Y?K?????????????I?C????8Y????Z?C8Z?C?B???AZ?C?D?YI?CZ?CY???#??K?DZ?E?IIII??C?VO????O?Q???????AY6?A??B?J?CY6AA??B??QD?2N???!????AY6QAFA????8?????N?K?M?N?A???Q?BBB??PBABICPA??N????T?6??D?'?N.O??O?F????I???O?L??2O?O???O?R???O?C??N???VO??O?????O???FO?????N??O?P?V??N??N?E?VO????O?T????FO?C??Y8?7?G?????FJAN!O??A5AKB7ABCDEDCBABCDEDCBA????????????!!.???N????R K??????O? ??2O?????O?????O?T?P?N??N??MQC?A??N?DFE?TFG1H?????N?##??N?????HY???Y2CFC?C?D?AY???FAQC3BKY6M?Q?P?QG?H6MFH??8???GY6KFG???C??Y?????E????N?????EFE??FF????T??GJ?8?F????A?6IFA?F?E?3?HHDD??B?B?B?B?B?Y5K.I?D?B??Q????????A?VO?C??8?5?G??VO?VO????FOJC?K6Q??????N?VOY?Q??VO????EY?B?Y??N??N??Q?????????N?R ???P?J?H ?S?CGDKK????AY??.FA?CY??.FC?????VO?C??8???G???OFA??OKY6???I???KY6Q??G????E??N??G??A?L???AAAABCCCAABBCCDDIIIIHGFFAAOOMMKK3E????????O???????EQF??Y6IEF?EG?AY???FAQC3B????????#?G?AY???FAQC3B???8?EK?F?K??CY??OFCQC3D??TY????KY6Q??C?????N?C???OFC?C?D?A?2O8??O?????AZAA???KY6???????OQD??OK?6Q??????N?VOKKKKY??O??O?VOI?????A?QDJ?I??O??OY?2O?2O?????G?IA?F?I?IA??K?DN!O??2O8??O??Y6IA??N??X FAQE????FG????O8??O??Y6IA??N??Z FCQE????FI?????G?I????O?FK1F?E?I?E?C?I?G?E?A?G?B?S?VO?VO??O??O????O?EM?E1M?E??A??E??GNF??GANG?777777???U???????B?D???VA??Q???N?A?VO?C??O?E??O?????????????????????????????????????????????????????????????????????????????????LA?A?A?A0A0A??LA?A?A?A??LA0A?I???Q?AA?? A?? A??5?AC????Z?7?????Q?AA?5??5QD?BA?AAY A?? A??5?AC???MF7??Z??Z?A?GN????B???AA?A??D?????C?AC????Z??D???AB??QE???C?8I????Z?????F?F???H??F???F??CCF????9IF??JF??HA??3?3???????????GA???BK??HAFA?.HFZ?P??Y6IA?AQ?????Z??Z??????NN??N????NN??N???????F?F?F?F??8I???AAEDEF????FP???Q?D?5FH???ED???A? ?H??FH?5???ED?F??XN??HQ!??EF?IB???D???5???ED?F??XN???EF???HQ????D???5??????5???????ARFA?AA?A??P?AY6IAFA55?AE?9?????????????????AAAAAAAAAAAAAAAAA?A?????????AAD??????????????????????ZK??FFP?AFE??N?U??N?U????2FC?AAFFA?AAFC??BFN1E1J1L?Q?AA????NFMGMQS???C??P?CY6IAFC??C???A??P?AY6IAFA??553J3L?L?I??1L???A????Z?E?H????N?UN?U????2FC??BK???KFA?DA?AZ?PY6IAFA???Q??9BK??DA?AZ?PY6IAFA???Q???? A?? A?? A?? A?? A?? A?? A?? A?? A?? A#? A?? A?? A?? A?? A1? A3? A5? A7? A9? A?? A?? A?? A?? A.? A?? A?? A?? A??5? A?? A?? A?? A?? A?? A?? A?? A?? A?? A#? A?? A?? A?? A?? A1? A3? A5? A7? A9? A?? A?? A?? A?? A.? A?? A?? A?? A??5?I????Z?0E??B??Z?J???L???Q???A?2???D??F????Z???K???M???N???O???R????????Z??Z?C?X?5Y6FF2?66AF3?7F4??Z??D??Z??AAABBCCDDABAABAAAAAAAAAAAAAAAAAAVVXVVVXVXVVVVVEEVXVVVVVVVVVVVVBCCCACCCACAACACCTTAACCCCCCCCCCCCWV????????????????????????????????AAAAAAAAA?AAAAAA?AAAAAAAAAAAAAAA????????????????????????????????ACDECGBIHDFS?A?2?? ?F'K?????N?Z??2? N?Z???EFF1??EFN?N??EFN?N?!EFF??XEFN?N?XEFF???'?BQD??BF?G??2?????N?Z??2FA??PN?ZK?????H???EF?#NQCJA?#N?CJ?N#N?A?A????JBF???2FA?????????U?N??EFN?P??P??P?A?HN?Z?A?????N?Z??2?PN?ZN?Z?2??K??N?T?2???????D?F?RU?ANSU?2Y6FF2?36AF3??#?G???CQ??K?PFA?L?PFB?K??FBFK?L??FAFL????A?2FK??2FL??2F??YY22F2?36AF3?L????F#?K???F#F#????Z?B??B?D??F?K?PKKKKF??L?PF?F?????Z??AK????AFD???AFG??Z? AK????AFFFI?DFA?GFN??DFB?GFO?AFCFP?K?AK??ZN?ZN??KY??Z??AF8?NF???AF9?NF???AF??NF??K?Q?E393??#?G?0G??G?7G???Q?AA?2???D??F????A?N????P?N?I????H?????B?U??????A????????????H??????A?????????TV?K?CA????V???IF??H?????C?U??????A???????????1H??????A?????????QV?K?CA????V??DIF?8?K?E?K?LI?????B????????????6IAF?YY6XAF??????????????GA???BK??HAFA?.HFZ?P??Y6IA?AQ???H??????????6IAF?YY6XAF??????C?Z??????????6IAF?YY6XAF??YI??????????6IAF?YY6XAF???????Z??GA?JF?JF?JF?KF?KF?JF?JF?KF?KF?KF?KF?KF?JF?KF?JF?JF?KF?JF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?JF?JF????Z??GA?MFNLFNLFNLFNLF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MFNLF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MFNLF????Z??GA?JF?JF?JF?KF?KF?JF?JF?KF?KF?KF?KF?KF?JF?KF?JF?JF?KF?JF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?JF?JF????Z??GA?NFNLFNLFNLFNLF?MF?MF?MF?MF?MF?NF?MF?NF?NF?NFNLF?MF?NF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?NFNLFAA?A?AQAIAEACABAI????Z??AK??????AFK???AFL???AFN???AFO?AFMFP??????PKN2???A???Q?D??AYN1???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAFI?AA?I?8??AFA??FB?AK???PFK?AA?K?3????K?5????K?3????K?5?5555?IY6QAFI??BQ??8I????Z??AK??????AFK???AFL???AFN???AFO?AFMFP?????A???Q?D??AYFA?????DFAN1????DKKKKKKN2???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAKKKKFI?AA?I?8??AFA??FB?AK???PFK?AA?K?3????K?3?55???K?3????K?3?55?I?Y6BAFI?PA?KY???Y6ABFI?I?PB???8I????Z??AK????AA??Z?D?D?AQED?5?AFK?6?AFL?5?AFN?6?AFO?AFMFP??????PKN4???A???Q?D??AYN3???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAFI?AA?I?8??AFA??FB?AK???PFK?AA?KFD?5????KFD?7????KFD?5????KFD?7?5555?IY6QAFI??BQ??8I????Z??AK????AA??Z?D?D?AQED?5?AFK?6?AFL?5?AFN?6?AFO?AFMFP?????A???Q?D??AYFA?????DFAN3????DKKKKKKN4???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAKKKKFI?AA?I?8??AFA??FB?AK???PFK?AA?KFD?5????KFD?5?55???KFD?5????KFD?5?55?I?Y6BAFI?PA?KY???Y6ABFI?I?PB???8I????Z?PKN4?????Z?Q?C??YN3????A?F8?A?F??ARFK??Z??A?Q?8Y6?BF8??Y6?BF?????F???F??NEM????Z?PAFI?AA?I?8??AFA??FB?AKKK??K?5????K?7????K?5????K?7?5555?IY6QAFI??BQ??8????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????VF?VFHQF?VFUSF??F?VF?VFHQF?VF?VF??F?VF?VFHQF?VF?VF?VF?VF?VFHQF?VF?VFHQF?VF?VFHQF?VF?VFX?F?VF?VFHQF?VF?VFHQF?VF?VFHQF?VF?VF?VF?VF?VF??F?GADH?I??SADH?I???ADX?Y???ADH?I???ADX?Y???ADH?I???ADF?G???ADX?Y???ADF?G???ADX?Y??3ADV?W??9ADV?W???ADV?W???ADV?W??EA?A?B?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?B?A???A?Q?R?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?R?Q???ATD?D?D?D?D?D?D?D?D?D???ATE?E?E?E?E?E?E?E?E?E????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU??#ATD?D?D?D?D?D?D?D?D?D???ATE?E?E?E?E?E?E?E?E?E?#EA?Q?R?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?R?Q?#?A?A?B?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?B?A????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A???????????0?1?2?3?4?5?6?7?8?9????????4A?'?????????????????????????????????'???AA?????????????????????????????????????????????????????????????????????O??AA?????????????????????????????????????????????????????????????????????O????AD??????AB?????M????AD??????AB?????M?????I???2AB???7AL??????????????AB?????G???A?I???FAB???KAL??????????????AB?????G????AP????P?P???????????AP??????
- **Nombre (overworld)**: DONUT SECRET HOUSE ?BAAIANAXA?A?A?A?A?A?A?A2A?A?ADAHAMA?A?A?A?A?A?A2A!A?AIBTB?B?B?B?B?B?B?B?B?B?B4B9B?BABKBPBVB?B?B?B B?B?B?B?B?B?B?B?B?B??D??Y6?AFCY6EAP?D??A? ?D???L ?D??B??AK??R????????D??????A??A?????????????? ?D??????A?PAK??????????1C?I??B???A??? BD???????I1C?O?? BD??? CD?55??Q3???N?Q??N?B???N?F??N?N?DN?T??N????Y??NQL?CN?N??N??T????Y??N?BN?N???NN?N??NN?N??NFZ??NN?NN?TN?Y??NN?N???F????O????N?R N?T??T?CN?N?KN?T??N???NQ??CN?N??N??NY?? AN?N?? A?L??T??N?BN?N??DN?T??!??M??????????AG?T?H?P?!??!??F?F?EN!????!???AFA??N?Z ?2A?Z ?2??D??????NR?A?KQF6K?A!?Q?QAQ?QAQ?QAI?M??EE??QAQ?I?I?QAQ?E?QAQQIQEQEIEMMEEEEIQ????EQ??EQ??M?QAAQAQQAQA?I?IAQQ?Q??EE??QAQ?QQEQAEQEE??EEQIM?AQ?QQAEQQ?AQAQ?QQAAQAQAQAQAQAQE?EEEEAQAQQAQA?Q?EBABABABABABAABABABABBABAABBABABAABBABABABABABABABABABAABABBAABBAABBABABABABABAABBABABABAABABBABABABABABABAABBABABABABABABABABABABABABABABABABABAABBABABABABABABAABEEEEEEEAAAAAAAAAEAAEEEEAAAAAAAEAAAEAAEEIIIMMIIIIIMMIIIIMIIIMIMUUUEAAAAAAAAAAAEEIAHJKNORXZ??? ????????????I??I?I?I?IEAIEEIEIEAEIEA?IAA?I?IEAEAAAI?IEIE?II?EA??IA????EA??EAIA??EAEAEAIAIAEA????AAAAIAEAEAEA??EAEAEAIA????EAEAEAAAAAEAEAEA??EAIA??????EA??IACCCCCACCCACACACCAAACCCCCAA?N?NAM?K?K?K?E?L?K?KQNBRCR?GHSAUAT?C?K?O?K?C?EAY?E?IQW?W?W?CQV?HAX?W?W?W?D?B?BQB?B?B?N?C?NDN?K?CA?A??Z?JQZAA?Z?Z?Z?Z?WCNENBN?P?F?V?V?V?V?Q!V?V?CLAKAJAIA?CA?A?A??A?C?C?C?C?C?C?C?C?C?C?C???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????A????????????????????????????????????ABS?????T???ABS?????U??'??U???ABS?????T???ABS???????????????AB????AB?9??AB???CABV??JABX??OABW???ABX???ABV???ABW???ABW???ABV???ABW???ABV??BABX???ABW??BABV???ABW???ABX???ABV???ABV??AABX?? ABW???ABV??7ABV???ABW??.ABV??GABW??RABV???ABX???ABV????G?????G?????G?????G???HABP????B?B.???B?B.???AQB?B.?U?ABBCBBFCR????S?CQDRB?BRB?DR?Q?R?BQRJ?BRQJ?EBCDCBC?B??D?BKB?BKBRQJ?A?C?A?C?B??D?BKB?BKB??J?AAB?B??B?BQAD?ARF?BRQI?BR?C6D?R??B?C6AAB?ARD?A?I?BR?B6E??R??B?B6B?6B?A?D?AQJ?ARB?ARC?C???C6B?B6?C6B?.G?B??H?BARD?A?B?D6B?.G?D? GFFDB??D?ARB?A?C6B??E?EBC? ?B?C???B?B?D6D?R?RC?B?QE?O??? ???????R?6?D6A?D?CQ??E?E??? ?B?G????R??E6A?C6D?.?6D?FQR?? ?B?H?????R??E6A?B6D?.?6C?GQR??? ?C?C???B?B?E?????C6D?.?6C?F??? 12E?B?A?D?E??R??B6D?.?6C?D??12B?A?B8F??????BKB?OB?HR??????6C?BARB?B??D5E??R?RBKJ?KP???????B?C?G??RP???B8F?R?A6AB?I?P?0????QC?C?C??RB?B?RC?EA6???B?E???P9B5B9?C?C?CAR?B?JR??6???R?RB?D??K?D5A9CDB?DQR??B?B??C?ARF?DQRK?E5B?A?B?D??12BKBRQH?DQD??B?A9C5D9RA6B?D????BKB??G?AQBPL?0??????R?6?B?DAR12B?CRARE?PQ???O??0??0R?6?6B?D????B?C??RE?E??0?OC?D?0??C6A?B?D????B?A?D?AQBDKR???????R??C6H???R?AR?B?BRQC?A?B?J??0?0??0??C6D?R????A???B?QV?D?2?.D?C?RQQ?HBCDFE???D?DR?RQN?CBCRC?E????BC?BQDBRAQL?BQRE?F???#RQB?B?CQ??L?C??'E5A'C?A?B?C?B?L?B??G5B?BRAB?B?CQRAH?BBCBDCAR'E5E'?R??B?F?QR??QF?BQRC?E?????B?F????6?B?F???6??F?B??BKDR?6?H6B?B?B?B6B??B?B??B?B??BKCR??H6K?6?Q??6??R?F?B??B?D?R??H6B?BRQB?A?BRBACCDFCB??P?B?BR?B?A?D6E?6???B?BRC??RC?BRDAR??D?C?RAE6K?????R??6??C6H????????B?C???D6L??R?????A6??C?C???B6A?E6B?C6L??R???6?6?6?F?C?6?E6C?6?C?ARB?D????B6D?6?RD?BR?B6A?E6C???B?BJB?HRA6?6??RE?DR?6?E6B??BJB?BZBJA?B?B6B?RG?ER????D?ARBZB?BJBZG????6?RI?DR???BJB??BJBPJZYJ????R?RK?CQR?BZBJBZYCJBZC???C?BQD?BQC?BQRBJE?JYZJCYAZBJC??RCDCRARB?AQBRCDER?ZYJBZAJDYAJBZC???C?B?CR??C?CR??CJBYZBJFZA?BJBRQB6B?C.A?B6D??R?CZDYJZYGJBZB??B6B?C.A?C6D???RBJBZBJYBZBYCZBJB??B6B?C?A?B6A?C6A?BZBJBZYBJBYGJ?JYZRAB6F??R?R?C6A?B6G???ZYJZDYBZJBYCJ??C?ARB?ARD?D???RCJCYCZBYJDZB??H?EIHGFRB?EZAYCJBZBJJ?????R????B?B?RCDARB?B?0C?I?0R?6?6?RD?FR?ARP?E?A'B5A'C?D??R?B?C??RF?GA??????BKB?'B5A'D?FR??6?RG?BQRB?DR?O?BKB??B?B?D??R?B6B??B?B??D?BARB?DRA??B?C?0?B?DR???B6I??R??????B?B??B?D???0B?A?B?D?R??D6E?R???B?B??B?B?6B?M6??????????6?C6D?R??F?B?B?6B?A6C?E6A?B6A?B6E?R???F?B?B?6B?D6?6?D6A?C6F???R??H?B?B?6B?A6B?F6E?6??RB?B??H?B?B?6B?D6???D6D???RD?B??H?B?B?6B?C6?RF?ARF?B??H?B?B?6B?I6?DEDEDCBH?B??G?D?Q??B?D????C?C??QG?D????D?E?????B?D??? C?C? ?I?K?????????R?B?D?R? C?C12?L?B??C?C?R?B?F?R? ??B?B??Q?C??RB?GR?12? ?B?A?D?B?K?AAB?ARD?D?12RB?AAD?C?R?D?C?D?C?B?B6?G?A?B6A?D?A?BRA?B?D?R??BDA?B?B6AB?F?????AB6A?D?D????BDGR?????RB?A6B?E6B?B6B?RC?D?R??C?I?????R??6B?E6B?B6B?RB?B?RE?H???????RB?B?E6B?B?ARC?C?R?D?C???C?C?CR??E6C??RE?M?????????????D?ARG?ARF?M?R???????????S?E?R???B?F??????N?R???????R??????????N?E?????B?F?R??R?BRB?ARM?F??????C?A?B?B?6B?B6A?J?I?????????D?B6B?6B?B6A?J?B??E?C???C?B6B?6B?B6A?G?EQR???C8D?BOZBYAZBPFZYJ???G?D????D5D?BOJBYAJB?FJYZRARG?E????'B5A'EJCYAZB?BZCR??H?D??A?B?B?HZC??PBJC?6?H?B??B?B?C???C8D?I??JYZ?6?RG?DARQRB?B??D5D?KOJYZRA6?R??E?D??RQB?CR?'B5B'?CJAYBZH????R???E?E?6?RDBQB??B?B?JCYAZB?DR??RB?B?B??B?BR?B6A?CRB??B?A?DZD?R??B?ARB?B?B??B?BR?C6B??J?G??6?6?RB?B?B??C?BR?D6A?H6A?C6C??RC?B?B??D?A?D?C?6?B?A?D6A?B6B??D?C???E?A?C?D?#6#BKA?C6A?C6B?RD?C???E?B??B?D?#6#BKA?B6H?????QDQC?A?B?C?D?B?H?R?????AB?AAB?HA?AR?A?AF?L??RD?????R??D?B?C?B?C?6?F?L??????R?????D?B?C?B?C?6?C?A?BDL???????????RD?BR?C?F?R?6?RB?P??????????????R?B?A?BRC?BRD?6?RB?PR???????????R??RB?KR?R???R?R?RC?N?????????????RBDA?D?CR?RH?R????????????????R?N?T?????????????????RD?L?T??????????????????R?B?B??H?X????????????????????????H?W??????????????????R????I?T??????????????????R?L?J?????????RD?HR??R????C?B??E?J??????????D6A?BRA?B?G???????E?B??B?BRD?R??D6A?B?C?E?????G?B??B6B?D6???D6F?D????H?B6?B6B?D6???D6E?C???J?D?D?6??B?G??6????M?B?D?D?6??B?G??6?E? CDEFGH??E?B?B?F???6??B?H??6??? ?B?E???? E?B?B?F? ????B?H?????? ?B?E?12? E?B?B?F? ????B?H????R?4?B?C?C14?D?B?H?K? ?34?B?H?43?R1? B?B?E??12KC?B?H??12R?!?B?Q?!?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?BQ?BQC?Q?BQF??????D?EYC?YYE?DQA?CQE?AYE?AYC?DYU??D?FQA?BQB?AQC?A?E?A?B?DYU??C?KQD?EQB?YC?DYU??C?JQB?BQFQB?A?B?DYU??C?FQA?EQB??BQA?DQA?BQE?YU??C?B?QBQB?QDYCQ??CQA?BQAQB?BQC?U?BUC?A?BQD?QQ?CQE?QQ??BQCQ?QB?CQA?C?C?A?CQC?Q?CQBQ?BUH??YQQ?Q?BYBQ?C?C?A?CQA?B?C??YCUB??BUA?CQDYAQC?B?EQB?EUF?BQBQQEYBQAQB?EQC?H?BQ?DQA?CYCQ?QC?DQDQ?Q?G?BQ?IQB?B??B?B?QCQD??QUE?DQA?DQBYQB?B?YC?A?DQB?BUE?DQA?EQB?C?A?B?A?DQB?D?AQB?HQB?A?C?BUE????QBQC?C?DQAYDQDYQ?YC?BUB?Y??AY??BY?V?DQD?AQB?Q?HQD?BQQB?N?JQB??B?CQB?L?BQE?DQB?B?B?CQQ?L?CQEYA?CQA?B?C?BQ?L?BQQFYA?B?B?QB?B?CQ?QH?DYA?BQEYB?QB?AYB?B?QB?BY?F?EYA?HQB?B?YB?FQQ??Y?IYE?????B?F?B?YB?B?YB?BY?GYBYA?B?BY?B?E?HY?Y?????BYA?F?CYB?YC?B?F?B?YB?A?CYB?YDQB?BYA?CYA?CYA?D?EY?YQ?CYB?BY?DQE??YY?DYCY?YE?A?BQA?BYEY??Y?C?BQA?HYB?YD?AYCQB?YBYCY?YDUB?A?B?A?E?B?YC?DQB?F???Y?YF?C??YE?CY?YHQB?YB?BY?BUD?B??B?A?E?A?CQC?Q?BQF?Q???YBUAYD?B?B?YE?AYBQF?Q?Q?QB?BYB?BYUG?C?A?GQF?Q?Q?QB?DYU??BUB?YF?CY?UCQA?CQC??QBQC?CY??BUB?EYD?DQC?Q?BQH?Q?Q?Q?UC?CQ??CYBQ?C?CQA?BQB?CQ?QB?EQ?Q?UC?BQAUBYBQB?QB?DQN?Q?Q?Q?Q?Q?Q??CQA?BUBYQDQCQB?QB?HQ?Q?Q?Q?BQC?B?BYCUA?B?AYBQBQA?BQA?BQK?Q?Q?Q?Q?Q?B?BYCUA?C?A?CQEQ?Q?QB?BQB?FQ?Q?Q?B?BYCUA?B?AYC?BQC?Q?BQG?Q?Q?UQB?B?QC?BUCY??C?A?B?EQA?BQB?QB?CQ?QB?CYU?BUBY?F?DQG?Q?Q?Q?BQD?Q?QB?AYH?D?DQC?Q?BQG?Q?Q?Q?BQC?Y?B?A?CQB?QD?JQB?C???BUD?E???Q?GQBYA?EQB?B?YCUF?A?BQB??CQC?QQBYA?DQB?BY?BUG?DQB?A?BQA?HQB?B?B?UB?BUD?B?QBQA?DQA?EQB?QB?B?AYHUB?A?CQB?YCQA?DQB?A?D?BUA?BUB?BUB?F??Y??YGQB?C???B?DUF?B?E??Y??BYA?E?A?B?A?B?BUA?BUF?B?H??Y??Y??D?BY?B?A?EUH?B?E??Y??BYB??D?B??CUB?BUH?B?H??Y??Y??D?AYCUD?BUH?B?G??Y??Y?GUF?BUH?B?F??Y??YEQB?H?BUG?C?Q?BQB?AYGQA?G?DUD?DU?Q?BQB?HQA?I?BUA?BUJ?U?Q??Y?YQGQA?L?BUC?AQB?BY?IQA?Q?BQA?KQA?D?BY?K?A?CQGQA?BQAQD?AYB?D?BYA?C?B?A?HQA?B?AYD?BYB?B?FYA?B?B??GQAQB?AYD?A?KYA?B?A?B?E?BYB?BYUC?LYB?B??B?E?BYB?BUB?MYB??BUB?E?BYCUC?BYKYC?B?A?E?AYBUE?A?LYD?A?HUF?MYS?BYKYN?BUA?BUA?BYJYN?EUB?BYBYA?BYA?CYM?EUD?CYD??Y?B?AYJ?BUA?FUD?B?DY?Y?B?AYJ?BUE?CUC?B?DY??YB?A?G?DQD?GQC?CQD?Q?UG?DQDYEQGQ?Q?Q?QB?C?QUG?DQAQBYH?Q?Q?Q?QB?DQ?Q?B?AYH?CQCQCQK?Q?Q?Q?Q?QQB?B?YH?HQ?U??YQQD?DQC?QQC?C?YUG?D?Q??BUB?QDYEQAQB?C?Q?DUE?A?BQA?B?C?QQBYA?BQH?Q?Q?Q?YBUA?BUE?B??BQB?DQA?BQB?QB?BQB?BYUB?B?BUB?B??B?BQA?DQA?BQD?Q?QC?C?YUB?B?BUB?B?C?LQB?C?Y?BUB?B?BUC?B?D?A?H?AYC?AYBUC?B?BUD?EYB??CQA?D?A?B?BYUD?A?BUE?EYB??BQB?C?AYC?BUD?CUE?DYCY??BQB??B?BYA?BUBQA?C?AUB?BYA?B?BYCYC?U?BQAQBUH?YYU??QQF?BYB?YB?CYB?A?B?BYB?B?AYB?C??YF?IYC?Y?B?BYB?B?AYB?C??YC?LYC?Y?B?BYBU?B?GY????YUB?LYB?YB?B?YBUA?B?BYUB?A?BUB?LYB?B??CUA?B?A?BUB??BUC?MYA?BYA?D?A?BUH?PYB?N?QYC?Y?L?RYB?B?BUH?RYC?Y?CUH?RYB?A?BUI?RYB?L?IYA?EYBYB?AYBUC?BUE?IYA?D?BYB?BYGUE?BYCYA?BYBY?D?C?Y?CYA?DUG?BY?B?CY??BYA?D?B?DYDUI?AYB?C?Y?BYA?D?B?CYCUJ?DUA?DQC?BQAUBYMUBQDUA?DQC?CQBYFQBYEUBQBUBYA?DQC?CQBYBQB?QDYEUBQBUBYA?DQD?BQBYBQB?QDYEUBQBUBYA?DQB?DQ?QQCYBQ?CQCYDUBQBUQBYA?BQBQB?B?BQCYFQBYA?CUBQBUQBYB?CQC?CQCYAQB?BQ?CQA?CUBQCU?QBQB?BQB?BQFYC?BQ?BQA?CUBQCU?QBYB??BQB?BQQGYB?EQ?Q?QCUBQCU?QBYB?BQB?BQFYCQAQC?DUBQB??DQA?BQB?QHYDQC?DUBQB?CQA?BQC?QQBYAQDYBQHYQ?QQ?UUBUBQIU?Q?QQQ?QBQE?BYHQ?Q?QQY?DUBQBUBQEQ?Q?QFQA?BYBQ?BQDQYQ?DUB?BUDQB?QBYA?EQBYBQ?BQBQB?DUB?BUA?BUBQA?BYA?CQB?QBYDQ?Y?HUB?BUA?BUBQF?Q?Q?QC?FQ?Q?Q?HUB?CU??BUCQQ?BQFQB?DQ?Q?DUAUCUB?AUC?BU?OQAQHUB?CUB?UB?OQHUB?EUB?OQHUB?AQB?GQA?IQA?DQA?BQB?LQA?DQA?GQA?CQC?EQB?QDQE?Q?Q?HQD?BQB?EQA?DQB?BQ?EQA?FQB?BQB?CQB?QB?CQA?BQA?DQBQ?DQA?BQE?DQC?CQA?BQA?BYIQC?QQD?EQC?FQBYGQDQB?QC?GQA?CQA?BQCY??EQCQBQB?QC?FQA?CQB?BQB?A?GQCQC?Q?GQA?HQC?U?EQC?Q?BQA?HQB?FQD???UB?IQA?LQA?WQBUFQB?VQBUBQ?EQA?VQBUBQ?BQCQA?RQB?BQBUBQ?BQD?SQA?BQBUBQ?BQA?B?AQOYGQBUCQB?B?AQOYB?QEQBUBQB?BQB?QOYA?CQCQBUBQB?BQB?QGYA?EYB?YC?AQC?BUBQB?BQB?QGYA?DYB?H?BUBQB?BQD?QY?EYA?CYB?BUG?BUCQA?BQD?QY?DYFYB?UB?F?BUBQB?QB?CYA?DYBY?BYBY?B?BYB?D?BUC?AQEYA?KYA?DYA?C?BUC?AQEYA?EYA?CYA?CYA?FYBUD?Q?QBYBYBY?DYBYCYA?CYA?BYA?CYBUE?Q?Q?BYGYBY?KYB?AYBUC?CQU?CYBQ?GYBQ?BYEQ?YQ?CYBUE?Q?QYB?A?BQB?BYEQA?CQA?BQA?CYBUC?DQUY?DQA?NQA?CQBUC?Q?BUAYXQBU?QB??QB??QB??QB??QB?CQAUBQBUBQCUBQAUBQAUBQAUBQAUIQIUQUQUQUQUCQKUQUQUQUQUQUIQAUCQEUQUQUCQAUCQGUQUQUQUJQAUBQDUQUQCUBQUCQBUQCUBQUKQBUQBUBQAUCQAUCQGUQUQUQUIQEUQUQUCQAUCQKUQUQUQUQUQUGQBQHQCYB?A?DUB?A?BQA?IQA?DQFYA?FYB?CQQ?HQB?KYAUFYCQA?HQB?IYA?BUFYCQA?IQB?FYD?Y?UGYCQA?JQB?DYDY?Y?HYDQA?JQD?YQ?BYB?YB?GYBQQBQA?JQA?BQB?FY?YQ??BYFQB?QOQA?DQA?HQB?Q?QCQHQB?A?B?B?B?B?AQB?B?BQ?B?B?B?B?BQ?BQB?CQB?B?B?B?AQB?B?BQ?B?B?B?B?BQ?BQD?B?A?B?B?B?AQB?B?BQ?B?B?B?A?B?B?AQD?B?EQA?GQA?B?B?CQB?B?AQD?B?B?B?BQ?CQG?Q?Q?Q?BQB?B?D???QD?B?B?B?EQI?Q?Q?Q?Q?B?B?DQD?B?B?B?CQC?Q?GQA?B?B?B?B?QD?B?B?B?CQC?Q?B?B?DQ?Q?B?B?B?B?QD?B?B?B?CQC?Q?B?B?DQ?Q?B?B?B?B?QD?B?B?B?CQC?Q?B?B?DQB?B?DQD?A?B?B?BQA?CQC?Q?BQJ?QU?Q?Q?Q?BQB?CQB?B?BQA?CQC?Q?BQJ?QU?Q?Q?Q?BQBYB?B?A?B?DQA?DQA?BQA?CQA?B?B?DQB?B?BQ?EQA?BQC?Q?BQA?CQA?B?B?DQB?B?FQ?Q?Q?DQA?BQA?BQA?B?B?A?B?BQBYBQ?B?B?DQ?Q?B?B?CQA?BQA?B?B?BQA?EQA?B?B?DQ?Q?B?B?DQ?Q?B?B?A?B?BQA?FQB?B?DQ?Q?B?B?DQ?Q?B?B?BQ?B?B?FQA?CQC?Q?BQA?B?B?BQB?B?BQ?B?B?BQBYBQA?CQC?Q?BQA?B?B?BQ?B?B?BQ?B?B?BQBYAQB?B?BQB?B?AQB?B?BQ?B?B?BQ?HQDUA?DQC?BQPUEQAUBQAUCQCUBQAUBQFUQUQUQCUCQBQ?QB?UDQB?UDQB?UDQB?ULQB?UDQB?UDQB?UDQB?U?QC?BQ?DQB?UDQB?UDQB?UHQAQC?BQ?DQB?UDQB?UDQB?U?QD?AUI?BY?B?B?UJ?BQB?BUG?IU?YQ????UC?AUF?BQA?CUC?B?UC?AYBQB?A?L?BQA?CUC?B?UC?BQAQBQD???UD?B?UC?BQK?AYCQA?BQA?F?B?UC?BQC?HYEQC?Q?GYA?C?BQC?U?BQA?EQA?EQC?Q?EQB?C?BQF?U?QQ?EQA?FQBQ?EQB??C?BQC?A?HQAYDQAYEQC?Q?D?BQE?U??QFQAYFQAYCQD?Q??D?BQC?C?Q?CQC?QYFQAYCQBQ?F?BQCYA?BQAQBQA?BQAYDQAYBQGQ?Q????CYEQC?Q?IQA?DQA?BQA?C?CYDQGY?Q?Q?QEQAQEQA?BQB??B?EQBYBQ?BQB?QFQB?QB?FQC???CYCQBYD?Q?QFQB??CQB?FQA?PQF?????QBQAQB?CQC?Q?HQBQ?BQA?B?D?A?BQB??BQB?DQ?Q?DQAYEQC???C?A?B?A?CQD?AQCQA?DQBYBQB?C???C?A?B?CQA?DQA?BQB??FQC?Q?G?A?B?A?GQA?BQB?JQA?WQBUA??????A?????????????AAAAAA?AA?A??A?QAA?AAA???????A?AA??AAAA???A???????Q?AAAAA?????AA?????AA??AA????AAAAAAAAAAAAA??1????F?F??CCF???N????A??R ?PA?K??AF??ACF???IF??HA??3???Y6QAF???B?4??F?1???Z1????F?F????ANV??W???NX??G??#E?Q??Q.??N????R ?F??NT??CNL?8 A?? ??? ??? Q?? ??? ??? ??? ??? A?? ??? ??? Q#? ??? ?'? ??? ??? A0? ?1? ?3? Q5? ?6? ?8? ??? ??? A?? ??? ??? Q?? ?!? ??? ??? ???5????AA???AFN??FO??FP?AFK??FL??FM?AFE??FF??FG?BAEA??H?AXKXNIQ??AA??E??QR?B?N?AXN????EXK3A??AI?01P??#3P?P?????AAAAAA6E?E?EJE?A2A?A?AFA?A?A?A?A?AHCAA?CSFIG0E?E?G9EMG?G?GAA?E?A?B?B?B?BAA?BEB?BAAAA?G.G?G?G.GAAAA?G?G?GAAEDAAAA?F!F?F?FAA?FAA?FAAAAAAAAAAAA?DAAAAAA?C?CAA?CAAAAAA?E?EAAAAAAAAAAAAAA?F?FAAAAHHAA?B5D2D?DAA?H?H?H?HZHWHTHRHAAAAAAAAAAAA?S?W?S?S???K?K???K?S?W?#?#?K?OAA?O?E?Q?G?Q?U?Y?Y???UAA?A??????????AA???I??AAAA?U???Q?Y??AAAA?I?U?IAA?IAAAA????????AA??AA??AAAAAAAAAAAA??AAAAAA????AA??AAAAAA?A?AAAAAAAAAAAAAAA????AAAA?OAA???Q?K??AA????????????????AAAAAA???????????D??#.??????3456789????????!?0??A????P??A?????P?HA????C ??1E?????A?FE?P??AK??'?E??VA????FG?E??#E?G?Q!??#??#EXE?VA?D?XE?P?!3?Q?G??P??6?I?7??8BYA??A????Q?AFD?A?Q??A A??55?DQ??6#?D??FD??A A??55?DQ.?1OQ?????5???GAY???????????Y???A?AHAAA????FAAA????M??N?Y?I?NN???M?'I?QT?P?Y?Q?NP???U?'Q????M?NN??U?NP??5?K?CA?OM??D?T????Q?KFA?O?Y6A8?A??O????A??1AQD??AZ?E?YF55II??????F???F??AN N???N????R ??C?EN?Z?U?A??NDH?BNEH?DI?GNAG?5??CDEGHJF?5??EN?T??N?????N?R????MY?B??R 'R ?O?R ????EN??????RSTUVWX????N????R ??C?EN?Z?RN?Z?HN?Z?DF??Q?AAK?????B?????A?FA?AA?A??P?AY6IAFA55?AE?9L??H??!?A???M??????8????'?????FA???EFC?Q?A?EO?AA???#?????FA???BA?AA??#???AFP??13P?P?????A??QIECBI?Q???A?G2?FF4?AAGA?5??FB????N????A????R ?H?BY6EFB?A?A??FC???A??FD?CKKK??A??AKK???AFC5K?APFC??2 A1????2 ?1????2 C1????2 ?1???3A?A?????5????Q???ANEC?MFE?AA???'??????C?FF?A?P?C AA??5?FQ??D'?F??FF?C AA?5?FQ!????C??????AJ??EJM?IJ??MJO?QJ??UJQ?MBC??BC??BC??K???K???KG??KE?AJ1??J???J??YJ???J???J9??J??ML??QL9??J9??J5??J1??J???JQQ?J?Q?JMQ?JMH?JMH?JMG?JUQ?JUH?J?H?JMGEJMF?JOF5J?G5J?GYJIG9JSF?J?E?JSEAA?E?AYE?A?D9A?DQA?D?A?DQFYF?J?F?LUH?J?F1J7F5J?F9J?G?J?G?J?F?J?F5B?H?K?P?A?H?A?H?B?H?B?H?J?G?JUG?JUGAJUFEJYHIJ?H?J?HMJ?QQJ?QUJ1Q?J?QYJE??JY?EJ???J???J???J???JO??JQ??J???JM??JM??JM??J???J???J???J??AK1?EK??IK??YJ??YJ???J???J9??J???J???J???J???J???J???J???J???J???J??YJ???J???JM?1JM??JE??J???J??YJ?BML?B?LIJ?L?JQKKJQK?JMKMJMK?JQKOJQK?JMKQJMKYJQKSJQKWJUK?J?D?IYK?J?K?J?J?K?K?K?K?L?K?L?K?M?J?M?K?N?KYK?K?KULQLYLQL?K?L?K?L?K?L?K3L?K5K?J5K?K???K5??K5?YJ???J???J5??K???J???KY??K???K??YJ???JY??K???K???K5?UJ???J???J???J???K???K???K??YJ??YJ??YJ???K#??KY??KY??K???J??1K???JY??JY??JY??JY??JY??K???K???J???J???J???K??YJ??YJ???KQ??KW?9KS??K???KQ??JQ??JQ??K???K??YJU??JY?YJ??YJ??YJ???KQ??J???JQ??JQ??KQ??KO??KK??KC??J???K???JM?YJI?YJE?YJA?QKSYUK??YK???K??UK??YK???K1??K??YKY?YKW?YKU??KQ??K???K???KQ??KQ??KS?YKU?YKW?YKY??K???KQ??LQ??LU??LY??K???K???K???K1??K???K1??KQ??JM??K???K#??K???JY??KQ??KM??JM??KK??KG??KE??JE??K???KM??JI??JE??KA??J?W?J?W?J?W?J?W?J?W?K?V?K?V?K?T?J?U?K?U?K?T?K?T?K?S?B?R?C?R?C?R1C?RIC?R?C?R?C?R?C?RYD?R?D?R?D?R?D?R?KQ??K??1KM?5K??9KU??K???KY??K#?YJ??YJ??YJ??YJ??9J???K5?AL??1K9?1K???K???LQ??LU??LY?EL??IL??ILU?IL??ILM?9JI??B?H?J?Z?L???D???D??UE???E???E??AE???E???E??9E???K???L???L???LY??JU??JU??JU??KU??L???J??YJQ?YJU??JY?YJ??YJ???J??YJ??YJ???J??YJ??YJ???J??YJ??YJ???J??YJ??YJ???JY?YJU?YJQ??JM?YJI??JE?AAAANANAQAVAYA?A?A?A?A?A?A?A?A?A?A?A?A?A?A?A?A?A'A?A4A7A9A?A?A?A!A!ADADAEAOAQASAYAYAYA?A?A?A?A?A?A?A?A?A?A?A?A?A?A2A4A5A?A?A?A?A?A?AABABABABABCBIBPBSBUBWBXB?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B?B2B5B8B?B?B?B?B?B?B?B?B?B?B?B?B?B?BA??QIECB???P?H??P?????C ??1E?B??PK?????0EN8???0EN???8??Q?W1???8??8????????????8?KK??N'E??P'EFE????FI?MFL???AAFG?AAFJ?AJQG??1??1??2????BAFA?E?BAFM???J A??5?G A???5??K??A?K?K???Y6AI??MQ??E?Y6?AFE??H?KK???Y6AQFE?AQ???FAFA?E?FAFM???J A??5?G A???5??K??A?K?K???Y6AI??MQ??E?Y6?AFE??H?KK???Y6AQFE?AQ???G???GA?288?3?3?7?98896???#?????U???U?Y?O???????Y?????M!????FGAQAFGBBBZE?A?G?B?B?A?G?G?F?F?F?C?CSCYDGDGP???????????C????'0!??A??N?C?D?7??6????7????W?7??H??7??????C ??1E???H?32??T?O??T??N?N?PN?N??Q4?FN?T?AN?N??G??7??!3YKKKKNC?Y??ND???NE???T?Y?F??N???G??C?F?WN????????X??2E?J?Q!?CNG??O?TKK???FM???A?FK??2E????EA?K??2E?G?Q!?E3KN?T?DA?O??2EXK??YY6QA?????2EXK??G??7?K?????0EN8???0EN???8????G?G??G????7??FO???8?KK??N'ENE??P'EFA?? ?Y6A?8FC?A???????ND??A??KKNC????A?FM???FK?E??AJQG?94??4??5??A ?D?KP?D??W1???VN???G???BAFG??D???C ?D?55?AD?D? AFI??A?2IFI?BA?G?IK?8?Y ?D?55?BAFE?A?M?K ?D?55??Y??A???E??IY???Y6AI??C8???Y6AE8 ?D?55?IK?8 ?D?55?EQ??C8Y6?A8FC?A?Y6?AFA??H??Y???Y6AQFA?C8?6?A? ?Y6AI8FC?G?D?.4??FAFG??D???C ?D?55?AL?D? AFI??A?2IFI?GA?O?IK?8??GA?2IFIY ?D?55?FAFE?A?M?K ?D?55??Y??A???E??IY???Y6AI??C8???Y6AE8 ?D?55?IK?8 ?D?55?EQ??C8Y6?A8FC?A?Y6?AFA??H??Y???Y6AQFA?C8?6?A? ?Y6AI8FC?G?D??5?GGGGGGGGUUUUU????SSS????????????BP??9PP????'AAAAAABBABBBBBBBABBAABBBBBBBBABAABBBBBAAAAAAVC?C?C?C2C?CURUQ?A?F?F?H?G?B?B?B?AA?A?QF?B?Y?Y?Z?Z?Z?Y?Y?YXF9??FSCU?????W?U??G?B?BEDED?C5A?A?A?AAABA?J?J3A?J?J?J?JYJYJ?J3A?J?J3A5AAKEKIKYJYJUJYJMK3AED3A?A?K?J?J?J?K6A?A?A?A?A?A?7?FP???15E???Q!?G????G???T?7??H??7??????C  ?1EZC ?? ?6??#?Q6EFCKK????U6EFA??6EFE?C?BA?K?Q?A??1?#7???Q?E?A A??????P??6?VU?UU???DH?HJ??J?Q????Q?DH?HJ?I?.K?6Q?Q?????ANFJ???N!J???6J?DNAG??VU??Q??G?????W1???8??8???????F?DNG???49???MA?GA?E??AJQG?MA?CAGF????FFD?AFC?BZ?D?AAMZ?D?AA???FE?AA????FEJRZ?D?CZ?DY6I5IIII?D???BY6IFB??????Q????E?Q??I??N?R ?D?C?BEK?VU?G?A?K???L7?R?V?R?U?R?U.Q?T!P?S?O!R?N?Q?M?N?K?LBHBHAIAI??RS??QR??QR??PQ??OP??MN??LL??KKAAIIBBHHAAII.?.?.?.??.?.?.?.??????????????????????????????????????????????E?QE?G???E??G??B?Z?Q?F??D?5????#8FC?49????9?Y?F????5????????8FC?49?DFD?A?AY??8ZAC?BY?C8ZBC?CZCC??8ZDC????5?DQ#??E??E??E??E??C??2?FA?D?Y2?FB???FP???A?FN?7???AK??'?E??VA???N??#E?L?Q!?Q?HNG?????BN???G??7???K??'?EKKKKNC??'?E??ND???NE??GZGZG?G?WZWZW?W?G?G?G'G'W?W?W'W'GZGZG?G?WZWZW?W?G?G?G'G'W?W?W'W'IVYVJVZV?R?R?R?R?RQR?RRR?R?R?R?R?R?R?R?RCZSZDZTZ?Z?Z?Z?ZA?Q?B?Q?AUQUBUQ??R?R?R?RC?S?D?T?GZGZG?G?GZGZG?G??R?R?R?R??FP???A?FN?7???AK??'?E??VA???N??#E?D??!??GO?7???AK????EFA?'?E?#?O????#E? A???EFM????9FK?OKKK???D???A ?D?Y6A? FD??AD ?D? HD??K BD????K JD????K DD????K LD???A ND?KY6QAP?D????????FO???8?KK??P'EFA?? ?Y6A?8FC?A?FM???FK?N'E?AJQG?94?8???5??A ?D?KP?D?????BC??BD??BCU?BDU7B7B7?7?7B7B7?7??B?B?B?BKV?VLV?V????FP?EFM???A?FN???FK??T??AK??H2EFA??D????T??A?DA??KKK??A ?D?Y6A? FD?8Y6?A8FA?AD ?D? HD??K BD????K JD????K DD????K LD?KY6QA???T??A?CAQGKKK?AD??A?3???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????A??????A???AA?????U?MQE??T???U???I?5??N???HFA??U??N?X  ????Y  A???Z  ?????  ????A ??? ????A?A? ????I? Y???? ???5?AQ???QR??UY6???QC?AN?U?AO?U?QN?U??U??U??UFP?A#?A????X??X??????Y6B?G??QC?? ???????8??????Y2CFC??8FB???P8Z?D?AZ?D?3Z?D??T?KJ?Z?DY????CZ?E?PY6E??QC?AFP59?UQT??U?FQE??QI?KY6??#????KKKKY?5?? 5??????I?????A?QDJ?I???? ???8Y?A?? A??8??????A?6IAZAA????????GA??V?T?V???V???V????A???N???H?1?1?1?? N?E?5A?A??N??NQB5OK??I??J??T?E???H??H??H?E??ES??D?I?H??I?A??Y?R?ENJ?Y6A8?Q??B????E??E??Q.?Q?J???J??????A??J?????Z?F?QFII??Q??BH????F????V????????E???????C???????????AJW?K???S?Y???AJV???S?Q?S??LAF??A?A??UAF??A?A??PAD??????AD??????ADF?F6??ADF?F6???????????????????NN?N?Q?O??N??NN?N?T?A???N???J??N???????BK?B?9K??F?YNL?OK?K?B???N?V?T??N??QM?Z?N??N??N???????N?C?Q?N?C?K?N?C?K?N?C???CN?EN?E???F??YY??D?P?D????? ?D??IQ?5????N?L??? ?D? ?D???N?L??? ?D? ?D????L??L??Y???K?K????D? ?D??H? ?D??L? ?D??P? ?D?????N?O.KY6K???N?#??XA??C???? ?D?Y  D??AAB?AAAB?AG?B?AH?AKBA?A?AIIBYAJ?B??AAAABA?A?BDAAAAK?AYAK?A?AK?B?B?AABQ??A??QAB?AL???M???W.?5N?B?E?C?F????O?X.??O?Y.?5O?Z.??O??.??OY?6F??Q??N??O??????O?2.?O?N???5???5?QQ0?8???????????A?A????AA??AA?A?B?B?A?B?B?A?B????Y???HFGHEGHF?!?C????? ???T?Q??? ????I!?H???.N? ?A!M? ?IN? ?YN???? QI?? ?EN? YK??BG?C?CG???DG??H?EG??H?FG?GGKY6ENBG?C?2N???F?2N????H???.??OYK?????Y??.????O8?2O????Y?5.????O8??O?Q??EK??N?Q??EGAGB?A??N??OFC??OFD?B??N??O8??O???C??QN#?A??N??N5Z?N????A?????N?N?!O?P???N?I??O?D??O?FQIO?N???AM#??N?O?N?????Q???????!?!!AA??Z??DLU??N??H ???????????????1??????2N??GA????????????Y?????????A??QIEC?2N???FA??T?K?H?5??B?R??N????R ?????A?C?B?BBDBBBBCMMSSSSMMQAIA?A?AQA?AIAQAB?Q?Q??Q?Y?A???????CFE??????G?Q?B5GG?AY???FAQC3B?EY???FC?F6AFD??8??????G??Q???N?????8??????VO?D????FO?B???OY?????O????I?FO?B?FO?????N?EOK?AQK?G?????A?Q?EO?CKQI?I????AA??QO'FO?FO????G??NKY6Q???NK?T???N?FOKK?A?B??VOY???????D?VO?????IHEHABEBBHJHY?A???????A?B?B??VOY???????D?VO??N?Z ??O?? ??O?Q???????VO?CJ??8?????58???????D?????VO?D???Y2AFAQC3B???Y2CFCQC3D???Y??????ABA?P?AAB??0???N????T?6??D????????O????2O?????O?????O????7???JS??N????O?ON???P??O???O??O?1?D????Q???O??O?D??N?????N?I??OQE???Q8Y???T?C??Y?G?????6IFC?C?D??8??O????#????????????AY6IAFA???.85Y?K?????????????I?C????8Y????Z?C8Z?C?B???AZ?C?D?YI?CZ?CY???#??K?DZ?E?IIII??C?VO????O?Q???????AY6?A??B?J?CY6AA??B??QD?2N???!????AY6QAFA????8?????N?K?M?N?A???Q?BBB??PBABICPA??N????T?6??D?'?N.O??O?F????I???O?L??2O?O???O?R???O?C??N???VO??O?????O???FO?????N??O?P?V??N??N?E?VO????O?T????FO?C??Y8?7?G?????FJAN!O??A5AKB7ABCDEDCBABCDEDCBA????????????!!.???N????R K??????O? ??2O?????O?????O?T?P?N??N??MQC?A??N?DFE?TFG1H?????N?##??N?????HY???Y2CFC?C?D?AY???FAQC3BKY6M?Q?P?QG?H6MFH??8???GY6KFG???C??Y?????E????N?????EFE??FF????T??GJ?8?F????A?6IFA?F?E?3?HHDD??B?B?B?B?B?Y5K.I?D?B??Q????????A?VO?C??8?5?G??VO?VO????FOJC?K6Q??????N?VOY?Q??VO????EY?B?Y??N??N??Q?????????N?R ???P?J?H ?S?CGDKK????AY??.FA?CY??.FC?????VO?C??8???G???OFA??OKY6???I???KY6Q??G????E??N??G??A?L???AAAABCCCAABBCCDDIIIIHGFFAAOOMMKK3E????????O???????EQF??Y6IEF?EG?AY???FAQC3B????????#?G?AY???FAQC3B???8?EK?F?K??CY??OFCQC3D??TY????KY6Q??C?????N?C???OFC?C?D?A?2O8??O?????AZAA???KY6???????OQD??OK?6Q??????N?VOKKKKY??O??O?VOI?????A?QDJ?I??O??OY?2O?2O?????G?IA?F?I?IA??K?DN!O??2O8??O??Y6IA??N??X FAQE????FG????O8??O??Y6IA??N??Z FCQE????FI?????G?I????O?FK1F?E?I?E?C?I?G?E?A?G?B?S?VO?VO??O??O????O?EM?E1M?E??A??E??GNF??GANG?777777???U???????B?D???VA??Q???N?A?VO?C??O?E??O?????????????????????????????????????????????????????????????????????????????????LA?A?A?A0A0A??LA?A?A?A??LA0A?I???Q?AA?? A?? A??5?AC????Z?7?????Q?AA?5??5QD?BA?AAY A?? A??5?AC???MF7??Z??Z?A?GN????B???AA?A??D?????C?AC????Z??D???AB??QE???C?8I????Z?????F?F???H??F???F??CCF????9IF??JF??HA??3?3???????????GA???BK??HAFA?.HFZ?P??Y6IA?AQ?????Z??Z??????NN??N????NN??N???????F?F?F?F??8I???AAEDEF????FP???Q?D?5FH???ED???A? ?H??FH?5???ED?F??XN??HQ!??EF?IB???D???5???ED?F??XN???EF???HQ????D???5??????5???????ARFA?AA?A??P?AY6IAFA55?AE?9?????????????????AAAAAAAAAAAAAAAAA?A?????????AAD??????????????????????ZK??FFP?AFE??N?U??N?U????2FC?AAFFA?AAFC??BFN1E1J1L?Q?AA????NFMGMQS???C??P?CY6IAFC??C???A??P?AY6IAFA??553J3L?L?I??1L???A????Z?E?H????N?UN?U????2FC??BK???KFA?DA?AZ?PY6IAFA???Q??9BK??DA?AZ?PY6IAFA???Q???? A?? A?? A?? A?? A?? A?? A?? A?? A?? A#? A?? A?? A?? A?? A1? A3? A5? A7? A9? A?? A?? A?? A?? A.? A?? A?? A?? A??5? A?? A?? A?? A?? A?? A?? A?? A?? A?? A#? A?? A?? A?? A?? A1? A3? A5? A7? A9? A?? A?? A?? A?? A.? A?? A?? A?? A??5?I????Z?0E??B??Z?J???L???Q???A?2???D??F????Z???K???M???N???O???R????????Z??Z?C?X?5Y6FF2?66AF3?7F4??Z??D??Z??AAABBCCDDABAABAAAAAAAAAAAAAAAAAAVVXVVVXVXVVVVVEEVXVVVVVVVVVVVVBCCCACCCACAACACCTTAACCCCCCCCCCCCWV????????????????????????????????AAAAAAAAA?AAAAAA?AAAAAAAAAAAAAAA????????????????????????????????ACDECGBIHDFS?A?2?? ?F'K?????N?Z??2? N?Z???EFF1??EFN?N??EFN?N?!EFF??XEFN?N?XEFF???'?BQD??BF?G??2?????N?Z??2FA??PN?ZK?????H???EF?#NQCJA?#N?CJ?N#N?A?A????JBF???2FA?????????U?N??EFN?P??P??P?A?HN?Z?A?????N?Z??2?PN?ZN?Z?2??K??N?T?2???????D?F?RU?ANSU?2Y6FF2?36AF3??#?G???CQ??K?PFA?L?PFB?K??FBFK?L??FAFL????A?2FK??2FL??2F??YY22F2?36AF3?L????F#?K???F#F#????Z?B??B?D??F?K?PKKKKF??L?PF?F?????Z??AK????AFD???AFG??Z? AK????AFFFI?DFA?GFN??DFB?GFO?AFCFP?K?AK??ZN?ZN??KY??Z??AF8?NF???AF9?NF???AF??NF??K?Q?E393??#?G?0G??G?7G???Q?AA?2???D??F????A?N????P?N?I????H?????B?U??????A????????????H??????A?????????TV?K?CA????V???IF??H?????C?U??????A???????????1H??????A?????????QV?K?CA????V??DIF?8?K?E?K?LI?????B????????????6IAF?YY6XAF??????????????GA???BK??HAFA?.HFZ?P??Y6IA?AQ???H??????????6IAF?YY6XAF??????C?Z??????????6IAF?YY6XAF??YI??????????6IAF?YY6XAF???????Z??GA?JF?JF?JF?KF?KF?JF?JF?KF?KF?KF?KF?KF?JF?KF?JF?JF?KF?JF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?JF?JF????Z??GA?MFNLFNLFNLFNLF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MFNLF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MFNLF????Z??GA?JF?JF?JF?KF?KF?JF?JF?KF?KF?KF?KF?KF?JF?KF?JF?JF?KF?JF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?KF?JF?JF????Z??GA?NFNLFNLFNLFNLF?MF?MF?MF?MF?MF?NF?MF?NF?NF?NFNLF?MF?NF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?MF?NFNLFAA?A?AQAIAEACABAI????Z??AK??????AFK???AFL???AFN???AFO?AFMFP??????PKN2???A???Q?D??AYN1???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAFI?AA?I?8??AFA??FB?AK???PFK?AA?K?3????K?5????K?3????K?5?5555?IY6QAFI??BQ??8I????Z??AK??????AFK???AFL???AFN???AFO?AFMFP?????A???Q?D??AYFA?????DFAN1????DKKKKKKN2???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAKKKKFI?AA?I?8??AFA??FB?AK???PFK?AA?K?3????K?3?55???K?3????K?3?55?I?Y6BAFI?PA?KY???Y6ABFI?I?PB???8I????Z??AK????AA??Z?D?D?AQED?5?AFK?6?AFL?5?AFN?6?AFO?AFMFP??????PKN4???A???Q?D??AYN3???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAFI?AA?I?8??AFA??FB?AK???PFK?AA?KFD?5????KFD?7????KFD?5????KFD?7?5555?IY6QAFI??BQ??8I????Z??AK????AA??Z?D?D?AQED?5?AFK?6?AFL?5?AFN?6?AFO?AFMFP?????A???Q?D??AYFA?????DFAN3????DKKKKKKN4???????B????FAKY2A??KF8?NF??????KF??NF??Q?N??Z?Q?C?FEM?????PAKKKKFI?AA?I?8??AFA??FB?AK???PFK?AA?KFD?5????KFD?5?55???KFD?5????KFD?5?55?I?Y6BAFI?PA?KY???Y6ABFI?I?PB???8I????Z?PKN4?????Z?Q?C??YN3????A?F8?A?F??ARFK??Z??A?Q?8Y6?BF8??Y6?BF?????F???F??NEM????Z?PAFI?AA?I?8??AFA??FB?AKKK??K?5????K?7????K?5????K?7?5555?IY6QAFI??BQ??8????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????VF?VFHQF?VFUSF??F?VF?VFHQF?VF?VF??F?VF?VFHQF?VF?VF?VF?VF?VFHQF?VF?VFHQF?VF?VFHQF?VF?VFX?F?VF?VFHQF?VF?VFHQF?VF?VFHQF?VF?VF?VF?VF?VF??F?GADH?I??SADH?I???ADX?Y???ADH?I???ADX?Y???ADH?I???ADF?G???ADX?Y???ADF?G???ADX?Y??3ADV?W??9ADV?W???ADV?W???ADV?W??EA?A?B?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?B?A???A?Q?R?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?R?Q???ATD?D?D?D?D?D?D?D?D?D???ATE?E?E?E?E?E?E?E?E?E????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU????ST????SU??#ATD?D?D?D?D?D?D?D?D?D???ATE?E?E?E?E?E?E?E?E?E?#EA?Q?R?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?C?R?Q?#?A?A?B?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?S?B?A????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A L?M??????????????????????????????5A ?????????????????????????????????IA J?K???????????????????????????????A Z?????????????????????????????????A???????????0?1?2?3?4?5?6?7?8?9????????4A?'?????????????????????????????????'???AA?????????????????????????????????????????????????????????????????????O??AA?????????????????????????????????????????????????????????????????????O????AD??????AB?????M????AD??????AB?????M?????I???2AB???7AL??????????????AB?????G???A?I???FAB???KAL??????????????AB?????G????AP????P?P???????????AP??????
- **Tipo**: nivel de MAPA (translevel 0x13)
- **Direcciones**: L1ptr 0x2E039 → header 0x322F2 · SprPtr 0x2EC26 → stream 0x3C9CA · L2ptr 0x2E639 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 79×27 casillas · SOLID=2 SLOPE=16 SLOPE_STEEP=16
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x0]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 4→0x0ED
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **BouncingFootball** (0x28) ×1: (3,51,20)
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,41,15)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,42,22)
    - [s] **PSwitch** (0x3E) ×1: (2,46,23)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,26,20)

### Nivel 0x014 — ????? SWITCH PALACE 3
- **Nombre (overworld)**: ????? SWITCH PALACE 3
- **Tipo**: nivel de MAPA (translevel 0x14)
- **Direcciones**: L1ptr 0x2E03C → header 0x3068D · SprPtr 0x2EC28 → stream 0x3C446 · L2ptr 0x2E63C · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=36
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x0CA
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **PSwitch** (0x3E) ×1: (0,8,23)

### Nivel 0x015 — DONUT PLAINS 1
- **Nombre (overworld)**: DONUT PLAINS 1
- **Tipo**: nivel de MAPA (translevel 0x15)
- **Direcciones**: L1ptr 0x2E03F → header 0x311E5 · SprPtr 0x2EC2A → stream 0x3C6D5 · L2ptr 0x2E63F · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=418 SOLID=151 SLOPE=1 SLOPE_STEEP=8
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 7→0x0FD · pant 11→0x0E3
- **Usa sprites grandes**: no
- **Enemigos (38)**:
    - [ ] **RedParakoopa** (0x9) ×2: (6,103,23) (6,107,23)
    - [ ] **Keyhole** (0xE) ×1: (18,289,6)
    - [s] **JumpingPiranhaPlant** (0x4F) ×1: (8,129,21)
    - [ ] **Sprite 0x72** (0x72) ×3: (3,51,10) (10,163,11) (12,195,11)
    - [ ] **GroundSuperKoopa** (0x73) ×16: (1,28,23) (2,35,21) (2,39,19) (4,75,20) (5,80,23) (6,105,17) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Key** (0x80) ×1: (17,285,6)
    - [ ] **Sprite 0x98** (0x98) ×6: (3,60,23) (8,132,23) (13,217,23) (14,232,23) (15,252,19) (18,299,23)
    - [ ] **VolcanoLotus** (0x99) ×4: (5,89,19) (12,203,20) (14,239,19) (18,292,17)
    - [ ] **MessageBox** (0xB9) ×1: (17,278,21)
    - [ ] **InvisibleMushroom** (0xC7) ×2: (2,45,23) (10,165,23)

### Nivel 0x016 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x16)
- **Direcciones**: L1ptr 0x2E042 → header 0x311E5 · SprPtr 0x2EC2C → stream 0x3C6D5 · L2ptr 0x2E642 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=418 SOLID=151 SLOPE=1 SLOPE_STEEP=8
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 7→0x0FD · pant 11→0x0E3
- **Usa sprites grandes**: no
- **Enemigos (38)**:
    - [ ] **RedParakoopa** (0x9) ×2: (6,103,23) (6,107,23)
    - [ ] **Keyhole** (0xE) ×1: (18,289,6)
    - [s] **JumpingPiranhaPlant** (0x4F) ×1: (8,129,21)
    - [ ] **Sprite 0x72** (0x72) ×3: (3,51,10) (10,163,11) (12,195,11)
    - [ ] **GroundSuperKoopa** (0x73) ×16: (1,28,23) (2,35,21) (2,39,19) (4,75,20) (5,80,23) (6,105,17) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Key** (0x80) ×1: (17,285,6)
    - [ ] **Sprite 0x98** (0x98) ×6: (3,60,23) (8,132,23) (13,217,23) (14,232,23) (15,252,19) (18,299,23)
    - [ ] **VolcanoLotus** (0x99) ×4: (5,89,19) (12,203,20) (14,239,19) (18,292,17)
    - [ ] **MessageBox** (0xB9) ×1: (17,278,21)
    - [ ] **InvisibleMushroom** (0xC7) ×2: (2,45,23) (10,165,23)

### Nivel 0x017 — #2 MORTON'S PLAINS 3
- **Nombre (overworld)**: #2 MORTON'S PLAINS 3
- **Tipo**: nivel de MAPA (translevel 0x17)
- **Direcciones**: L1ptr 0x2E045 → header 0x311E5 · SprPtr 0x2EC2E → stream 0x3C6D5 · L2ptr 0x2E645 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=418 SOLID=151 SLOPE=1 SLOPE_STEEP=8
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 7→0x0FD · pant 11→0x0E3
- **Usa sprites grandes**: no
- **Enemigos (38)**:
    - [ ] **RedParakoopa** (0x9) ×2: (6,103,23) (6,107,23)
    - [ ] **Keyhole** (0xE) ×1: (18,289,6)
    - [s] **JumpingPiranhaPlant** (0x4F) ×1: (8,129,21)
    - [ ] **Sprite 0x72** (0x72) ×3: (3,51,10) (10,163,11) (12,195,11)
    - [ ] **GroundSuperKoopa** (0x73) ×16: (1,28,23) (2,35,21) (2,39,19) (4,75,20) (5,80,23) (6,105,17) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Key** (0x80) ×1: (17,285,6)
    - [ ] **Sprite 0x98** (0x98) ×6: (3,60,23) (8,132,23) (13,217,23) (14,232,23) (15,252,19) (18,299,23)
    - [ ] **VolcanoLotus** (0x99) ×4: (5,89,19) (12,203,20) (14,239,19) (18,292,17)
    - [ ] **MessageBox** (0xB9) ×1: (17,278,21)
    - [ ] **InvisibleMushroom** (0xC7) ×2: (2,45,23) (10,165,23)

### Nivel 0x018 — SUNKEN GHOST SHIP
- **Nombre (overworld)**: SUNKEN GHOST SHIP
- **Tipo**: nivel de MAPA (translevel 0x18)
- **Direcciones**: L1ptr 0x2E048 → header 0x38C14 · SprPtr 0x2EC30 → stream 0x3DC2D · L2ptr 0x2E648 · GFXslot 0x028D3 · FGBGslot 0x0295F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 07` (tilesetFG=13)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0xE · música 4 · tiempo 400 · Layer2 fondo · paletas BG=4 FG=4 SPR=3 backArea=3
- **Colisión**: 46×27 casillas · SOLID=26
- **Entrada**: casilla (1,3) px (16,48) · pantalla entrada 0 · L2scroll 5 L3 3 L1y 0 L2y 3 · secHdr [0x51 0xF8 0x3 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 2→0x0F8
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0xC9** (0xC9) ×4: (1,17,10) (1,22,12) (1,24,5) (2,35,14)

### Nivel 0x019 — #2 MORTON'S PLAINS 3
- **Nombre (overworld)**: #2 MORTON'S PLAINS 3
- **Tipo**: nivel de MAPA (translevel 0x19)
- **Direcciones**: L1ptr 0x2E04B → header 0x30000 · SprPtr 0x2EC32 → stream 0x3E76D · L2ptr 0x2E64B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x01A — #6 WENDY'S CASTLE
- **Nombre (overworld)**: #6 WENDY'S CASTLE
- **Tipo**: nivel de MAPA (translevel 0x1A)
- **Direcciones**: L1ptr 0x2E04E → header 0x389CC · SprPtr 0x2EC34 → stream 0x3DBBB · L2ptr 0x2E64E · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 192×27 casillas · LEDGE_TOP=163 SOLID=59 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x4B 0xC0 0xBA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 11→0x0D4
- **Usa sprites grandes**: no
- **Enemigos (20)**:
    - [ ] **PortableSpringboard** (0x2F) ×2: (1,20,24) (1,21,24)
    - [ ] **ThrowingDryBones** (0x30) ×1: (1,17,23)
    - [ ] **Podoboo** (0x33) ×2: (4,66,16) (4,73,15)
    - [ ] **Sprite 0x67** (0x67) ×13: (2,45,22) (3,52,22) (3,57,22) (4,64,22) (4,76,22) (5,85,19) …
    - [ ] **Sprite 0xE9** (0xE9) ×2: (0,8,0) (11,184,0)

### Nivel 0x01B — CHOCOLATE FORTRESS
- **Nombre (overworld)**: CHOCOLATE FORTRESS
- **Tipo**: nivel de MAPA (translevel 0x1B)
- **Direcciones**: L1ptr 0x2E051 → header 0x36E36 · SprPtr 0x2EC36 → stream 0x3D95E · L2ptr 0x2E651 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 9 pantallas (144 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=1 backArea=5
- **Colisión**: 144×27 casillas · LEDGE_TOP=95 SOLID=73 SLOPE_STEEP=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0x8A 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 8→0x0EF
- **Usa sprites grandes**: no
- **Enemigos (27)**:
    - [ ] **Sprite 0x32** (0x32) ×3: (1,26,23) (3,58,23) (6,96,23)
    - [ ] **DownFirstWoodenSpike** (0xAC) ×10: (1,19,16) (1,28,15) (2,39,17) (3,50,14) (3,61,19) (5,80,16) …
    - [ ] **UpDownFirstWoodenSpike** (0xAD) ×9: (1,19,23) (1,28,20) (4,73,22) (6,99,23) (6,100,20) (6,108,20) …
    - [ ] **BowserStatueFire** (0xB3) ×3: (3,62,22) (4,74,23) (5,94,22)
    - [ ] **Sprite 0xD8** (0xD8) ×1: (4,65,0)
    - [ ] **Sprite 0xD9** (0xD9) ×1: (7,126,0)

### Nivel 0x01C — CHOCOLATE ISLAND 5
- **Nombre (overworld)**: CHOCOLATE ISLAND 5
- **Tipo**: nivel de MAPA (translevel 0x1C)
- **Direcciones**: L1ptr 0x2E054 → header 0x386E3 · SprPtr 0x2EC38 → stream 0x3DB0F · L2ptr 0x2E654 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 17 pantallas (272 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=7 SPR=0 backArea=0
- **Colisión**: 272×27 casillas · LEDGE_TOP=154 SOLID=307 SLOPE=2 SLOPE_STEEP=12
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0x7 0x0 0xBA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 2→0x0BD · pant 6→0x0C0
- **Usa sprites grandes**: no
- **Enemigos (44)**:
    - [ ] **KoopaKidBossFight** (0x13) ×13: (0,11,20) (1,17,16) (1,17,23) (1,29,23) (1,29,16) (2,41,23) …
    - [s] **PSwitch** (0x3E) ×2: (1,23,4) (1,26,23)
    - [ ] **ParachuteGoomba** (0x3F) ×2: (7,114,10) (7,122,10)
    - [ ] **Sprite 0x40** (0x40) ×2: (7,118,10) (7,126,10)
    - [ ] **ShiftingPipe** (0x49) ×6: (11,180,21) (11,184,19) (11,188,21) (12,192,18) (12,196,22) (14,224,23)
    - [ ] **Sprite 0x59** (0x59) ×3: (13,208,19) (13,214,17) (13,220,20)
    - [ ] **Sprite 0x74** (0x74) ×1: (2,47,20)
    - [ ] **GoalTape** (0x7B) ×1: (15,254,23)
    - [ ] **ChangingItem** (0x81) ×1: (4,65,23)
    - [ ] **ClappinChuck** (0x95) ×4: (14,231,18) (15,245,23) (15,248,23) (15,251,23)
    - [ ] **Sprite 0xDA** (0xDA) ×3: (9,147,18) (9,156,9) (10,164,9)
    - [ ] **Sprite 0xDB** (0xDB) ×2: (9,150,9) (9,158,9)
    - [ ] **Sprite 0xDC** (0xDC) ×2: (9,152,9) (10,160,9)
    - [ ] **Sprite 0xDD** (0xDD) ×2: (9,154,9) (10,162,9)

### Nivel 0x01D — CHOCOLATE ISLAND 4
- **Nombre (overworld)**: CHOCOLATE ISLAND 4
- **Tipo**: nivel de MAPA (translevel 0x1D)
- **Direcciones**: L1ptr 0x2E057 → header 0x38100 · SprPtr 0x2EC3A → stream 0x3DA93 · L2ptr 0x2E657 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=4 backArea=3
- **Colisión**: 240×27 casillas · LEDGE_TOP=34 SOLID=830 SLOPE=427 SLOPE_STEEP=490
- **Entrada**: casilla (8,6) px (128,96) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 0 L2y 0 · secHdr [0x52 0x1 0x0 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 8→0x0EA
- **Usa sprites grandes**: sí — MegaMole (0xBF)
- **Enemigos (24)**:
    - [ ] **Sprite 0x78** (0x78) ×3: (6,109,19) (6,110,19) (7,112,19)
    - [ ] **GoalTape** (0x7B) ×1: (13,222,23)
    - [ ] **ClappinChuck** (0x95) ×1: (13,218,21)
    - [ ] **CarrotTopLiftUpperRight** (0xB7) ×12: (1,31,14) (3,49,10) (3,53,16) (4,75,15) (4,79,9) (5,85,3) …
    - [ ] **CarrotTopLiftUpperLeft** (0xB8) ×3: (2,47,20) (5,84,24) (5,89,23)
    - [B] **MegaMole** (0xBF) ×3: (10,170,21) (11,178,5) (11,188,21)
    - [ ] **GreyFallingPlatform** (0xC4) ×1: (6,96,23)

### Nivel 0x01E — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x1E)
- **Direcciones**: L1ptr 0x2E05A → header 0x30000 · SprPtr 0x2EC3C → stream 0x3E76D · L2ptr 0x2E65A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x01F — FOREST FORTRESS
- **Nombre (overworld)**: FOREST FORTRESS
- **Tipo**: nivel de MAPA (translevel 0x1F)
- **Direcciones**: L1ptr 0x2E05D → header 0x3620A · SprPtr 0x2EC3E → stream 0x3D648 · L2ptr 0x2E65D · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=7 SPR=1 backArea=4
- **Colisión**: 96×27 casillas · LEDGE_TOP=67 SOLID=34 SLOPE_STEEP=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 2 L1y 2 L2y 2 · secHdr [0x1B 0x80 0xA 0x0]
- **Cabecera sprites**: 0xF (memoria 0xF, buoyancy 0x0)
- **Salidas de pantalla**: pant 5→0x0D6
- **Usa sprites grandes**: no
- **Enemigos (10)**:
    - [ ] **Sprite 0x67** (0x67) ×7: (1,25,21) (2,33,20) (2,43,21) (3,51,20) (4,67,20) (4,77,19) …
    - [ ] **Layer3Smasher** (0x89) ×1: (1,18,0)
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0)

### Nivel 0x020 — #5 ROY'S CASTLE
- **Nombre (overworld)**: #5 ROY'S CASTLE
- **Tipo**: nivel de MAPA (translevel 0x20)
- **Direcciones**: L1ptr 0x2E060 → header 0x359D9 · SprPtr 0x2EC40 → stream 0x3D4CD · L2ptr 0x2E660 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=3
- **Colisión**: 217×27 casillas · SOLID=53
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x1 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 14→0x0CC
- **Usa sprites grandes**: no
- **Enemigos (26)**:
    - [ ] **Podoboo** (0x33) ×4: (2,37,21) (11,181,19) (11,183,18) (11,187,20)
    - [s] **PSwitch** (0x3E) ×1: (10,168,21)
    - [ ] **Sprite 0x5A** (0x5A) ×3: (12,204,22) (13,211,22) (13,218,22)
    - [ ] **Sprite 0x78** (0x78) ×1: (11,188,14)
    - [ ] **CreateEatBlock** (0xB1) ×2: (1,27,24) (2,32,24)
    - [ ] **FallingSpike** (0xB2) ×3: (8,129,14) (8,139,14) (10,168,17)
    - [ ] **Sprite 0xB6** (0xB6) ×8: (3,55,20) (4,78,15) (5,80,21) (5,83,15) (5,88,21) (13,210,25) …
    - [ ] **BowserStatue** (0xBC) ×4: (11,189,14) (12,197,21) (13,215,21) (14,231,21)

### Nivel 0x021 — CHOCO?GHOST HOUSE
- **Nombre (overworld)**: CHOCO?GHOST HOUSE
- **Tipo**: nivel de MAPA (translevel 0x21)
- **Direcciones**: L1ptr 0x2E063 → header 0x367A2 · SprPtr 0x2EC42 → stream 0x3D74C · L2ptr 0x2E663 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 14 pantallas (224 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 218×27 casillas · SOLID=1 SLOPE=12 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x11 (memoria 0x11, buoyancy 0x0)
- **Salidas de pantalla**: pant 13→0x0FC
- **Usa sprites grandes**: no
- **Enemigos (25)**:
    - [ ] **Sprite 0x38** (0x38) ×7: (1,24,23) (1,25,22) (5,80,18) (5,80,23) (8,140,20) (11,188,22) …
    - [ ] **Sprite 0x39** (0x39) ×7: (4,69,21) (4,70,21) (4,71,21) (7,118,22) (11,179,17) (11,179,22) …
    - [ ] **MovingLedgeHole** (0x52) ×8: (1,17,24) (3,58,24) (4,73,24) (5,90,24) (10,174,24) (11,189,24) …
    - [ ] **FishinBoo** (0xAE) ×1: (5,95,12)
    - [ ] **Sprite 0xDE** (0xDE) ×2: (3,59,21) (9,154,17)

### Nivel 0x022 — CHOCOLATE ISLAND 1
- **Nombre (overworld)**: CHOCOLATE ISLAND 1
- **Tipo**: nivel de MAPA (translevel 0x22)
- **Direcciones**: L1ptr 0x2E066 → header 0x36444 · SprPtr 0x2EC44 → stream 0x3D6D9 · L2ptr 0x2E666 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=190 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x80]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x0F6 · pant 15→0x0BE · pant 16→0x0F5
- **Usa sprites grandes**: no
- **Enemigos (34)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (17,283,20)
    - [ ] **PortableSpringboard** (0x2F) ×2: (9,150,23) (18,291,23)
    - [s] **PSwitch** (0x3E) ×1: (10,166,22)
    - [s] **JumpingPiranhaPlant** (0x4F) ×4: (5,93,21) (14,229,21) (15,242,22) (15,254,19)
    - [ ] **DinoRhino** (0x6E) ×6: (1,17,23) (3,50,22) (4,72,22) (8,130,17) (12,199,21) (15,245,23)
    - [ ] **DinoTorch** (0x6F) ×14: (1,27,16) (2,38,16) (3,62,22) (4,65,22) (4,68,21) (5,82,21) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **LeftFlyingBlock** (0x83) ×2: (4,73,18) (12,205,16)
    - [ ] **WarpHole** (0x8E) ×1: (16,266,20)
    - [ ] **ClappinChuck** (0x95) ×1: (10,172,17)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (2,36,16)

### Nivel 0x023 — CHOCOLATE ISLAND 3
- **Nombre (overworld)**: CHOCOLATE ISLAND 3
- **Tipo**: nivel de MAPA (translevel 0x23)
- **Direcciones**: L1ptr 0x2E069 → header 0x36CC9 · SprPtr 0x2EC46 → stream 0x3D8BE · L2ptr 0x2E669 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 24 pantallas (384 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=1 SPR=2 backArea=1
- **Colisión**: 369×27 casillas · SOLID=99
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x80]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: pant 7→0x0D7
- **Usa sprites grandes**: no
- **Enemigos (50)**:
    - [s] **RedKoopaNoShell** (0x5) ×6: (2,35,16) (3,63,19) (5,87,23) (7,112,20) (8,142,17) (11,179,19)
    - [ ] **BlueKoopaNoShell** (0x6) ×11: (3,55,22) (4,74,21) (6,100,21) (7,121,20) (8,132,20) (9,152,15) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×3: (13,208,20) (13,208,18) (16,268,20)
    - [ ] **GoalTape** (0x7B) ×2: (18,302,12) (22,366,23 EE1)
    - [ ] **GreyChainedPlatform** (0xA3) ×22: (1,18,20) (2,35,17) (3,55,23) (3,63,20) (4,74,22) (6,100,22) …
    - [ ] **Sparky** (0xA5) ×6: (12,203,20) (14,230,20) (14,238,19) (15,247,22) (15,255,17) (17,273,20)

### Nivel 0x024 — CHOCOLATE ISLAND 2
- **Nombre (overworld)**: CHOCOLATE ISLAND 2
- **Tipo**: nivel de MAPA (translevel 0x24)
- **Direcciones**: L1ptr 0x2E06C → header 0x36897 · SprPtr 0x2EC48 → stream 0x3D7BF · L2ptr 0x2E66C · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=0 backArea=2
- **Colisión**: 64×27 casillas · LEDGE_TOP=98 SOLID=76 SLOPE=62 SLOPE_STEEP=68
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x0CF
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **PortableSpringboard** (0x2F) ×1: (0,14,19)
    - [ ] **DinoRhino** (0x6E) ×4: (1,17,23) (1,28,8) (1,29,15) (3,54,10)
    - [ ] **DinoTorch** (0x6F) ×6: (0,12,19) (1,27,23) (1,31,9) (2,47,8) (3,49,20) (3,51,14)
    - [ ] **MessageBox** (0xB9) ×1: (0,7,20)

### Nivel 0x025
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E06F → header 0x30000 · SprPtr 0x2EC4A → stream 0x3E76D · L2ptr 0x2E66F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x026
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E072 → header 0x30000 · SprPtr 0x2EC4C → stream 0x3E76D · L2ptr 0x2E672 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x027
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E075 → header 0x30000 · SprPtr 0x2EC4E → stream 0x3E76D · L2ptr 0x2E675 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x028
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E078 → header 0x30000 · SprPtr 0x2EC50 → stream 0x3E76D · L2ptr 0x2E678 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x029
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E07B → header 0x30000 · SprPtr 0x2EC52 → stream 0x3E76D · L2ptr 0x2E67B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E07E → header 0x30000 · SprPtr 0x2EC54 → stream 0x3E76D · L2ptr 0x2E67E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E081 → header 0x30000 · SprPtr 0x2EC56 → stream 0x3E76D · L2ptr 0x2E681 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E084 → header 0x30000 · SprPtr 0x2EC58 → stream 0x3E76D · L2ptr 0x2E684 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E087 → header 0x30000 · SprPtr 0x2EC5A → stream 0x3E76D · L2ptr 0x2E687 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E08A → header 0x30000 · SprPtr 0x2EC5C → stream 0x3E76D · L2ptr 0x2E68A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E08D → header 0x30000 · SprPtr 0x2EC5E → stream 0x3E76D · L2ptr 0x2E68D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x030
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E090 → header 0x30000 · SprPtr 0x2EC60 → stream 0x3E76D · L2ptr 0x2E690 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x031
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E093 → header 0x30000 · SprPtr 0x2EC62 → stream 0x3E76D · L2ptr 0x2E693 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x032
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E096 → header 0x30000 · SprPtr 0x2EC64 → stream 0x3E76D · L2ptr 0x2E696 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x033
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E099 → header 0x30000 · SprPtr 0x2EC66 → stream 0x3E76D · L2ptr 0x2E699 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x034
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E09C → header 0x30000 · SprPtr 0x2EC68 → stream 0x3E76D · L2ptr 0x2E69C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x035
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E09F → header 0x30000 · SprPtr 0x2EC6A → stream 0x3E76D · L2ptr 0x2E69F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x036
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0A2 → header 0x30000 · SprPtr 0x2EC6C → stream 0x3E76D · L2ptr 0x2E6A2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x037
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0A5 → header 0x30000 · SprPtr 0x2EC6E → stream 0x3E76D · L2ptr 0x2E6A5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x038
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0A8 → header 0x30000 · SprPtr 0x2EC70 → stream 0x3E76D · L2ptr 0x2E6A8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x039
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0AB → header 0x30000 · SprPtr 0x2EC72 → stream 0x3E76D · L2ptr 0x2E6AB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0AE → header 0x30000 · SprPtr 0x2EC74 → stream 0x3E76D · L2ptr 0x2E6AE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0B1 → header 0x30000 · SprPtr 0x2EC76 → stream 0x3E76D · L2ptr 0x2E6B1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0B4 → header 0x30000 · SprPtr 0x2EC78 → stream 0x3E76D · L2ptr 0x2E6B4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0B7 → header 0x30000 · SprPtr 0x2EC7A → stream 0x3E76D · L2ptr 0x2E6B7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0BA → header 0x30000 · SprPtr 0x2EC7C → stream 0x3E76D · L2ptr 0x2E6BA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0BD → header 0x30000 · SprPtr 0x2EC7E → stream 0x3E76D · L2ptr 0x2E6BD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x040
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0C0 → header 0x30000 · SprPtr 0x2EC80 → stream 0x3E76D · L2ptr 0x2E6C0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x041
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0C3 → header 0x30000 · SprPtr 0x2EC82 → stream 0x3E76D · L2ptr 0x2E6C3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x042
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0C6 → header 0x30000 · SprPtr 0x2EC84 → stream 0x3E76D · L2ptr 0x2E6C6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x043
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0C9 → header 0x30000 · SprPtr 0x2EC86 → stream 0x3E76D · L2ptr 0x2E6C9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x044
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0CC → header 0x30000 · SprPtr 0x2EC88 → stream 0x3E76D · L2ptr 0x2E6CC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x045
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0CF → header 0x30000 · SprPtr 0x2EC8A → stream 0x3E76D · L2ptr 0x2E6CF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x046
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0D2 → header 0x30000 · SprPtr 0x2EC8C → stream 0x3E76D · L2ptr 0x2E6D2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x047
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0D5 → header 0x30000 · SprPtr 0x2EC8E → stream 0x3E76D · L2ptr 0x2E6D5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x048
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0D8 → header 0x30000 · SprPtr 0x2EC90 → stream 0x3E76D · L2ptr 0x2E6D8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x049
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0DB → header 0x30000 · SprPtr 0x2EC92 → stream 0x3E76D · L2ptr 0x2E6DB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0DE → header 0x30000 · SprPtr 0x2EC94 → stream 0x3E76D · L2ptr 0x2E6DE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0E1 → header 0x30000 · SprPtr 0x2EC96 → stream 0x3E76D · L2ptr 0x2E6E1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0E4 → header 0x30000 · SprPtr 0x2EC98 → stream 0x3E76D · L2ptr 0x2E6E4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0E7 → header 0x30000 · SprPtr 0x2EC9A → stream 0x3E76D · L2ptr 0x2E6E7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0EA → header 0x30000 · SprPtr 0x2EC9C → stream 0x3E76D · L2ptr 0x2E6EA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0ED → header 0x30000 · SprPtr 0x2EC9E → stream 0x3E76D · L2ptr 0x2E6ED · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x050
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0F0 → header 0x30000 · SprPtr 0x2ECA0 → stream 0x3E76D · L2ptr 0x2E6F0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x051
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0F3 → header 0x30000 · SprPtr 0x2ECA2 → stream 0x3E76D · L2ptr 0x2E6F3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x052
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0F6 → header 0x30000 · SprPtr 0x2ECA4 → stream 0x3E76D · L2ptr 0x2E6F6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x053
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0F9 → header 0x30000 · SprPtr 0x2ECA6 → stream 0x3E76D · L2ptr 0x2E6F9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x054
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0FC → header 0x30000 · SprPtr 0x2ECA8 → stream 0x3E76D · L2ptr 0x2E6FC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x055
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E0FF → header 0x30000 · SprPtr 0x2ECAA → stream 0x3E76D · L2ptr 0x2E6FF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x056
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E102 → header 0x30000 · SprPtr 0x2ECAC → stream 0x3E76D · L2ptr 0x2E702 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x057
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E105 → header 0x30000 · SprPtr 0x2ECAE → stream 0x3E76D · L2ptr 0x2E705 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x058
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E108 → header 0x30000 · SprPtr 0x2ECB0 → stream 0x3E76D · L2ptr 0x2E708 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x059
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E10B → header 0x30000 · SprPtr 0x2ECB2 → stream 0x3E76D · L2ptr 0x2E70B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E10E → header 0x30000 · SprPtr 0x2ECB4 → stream 0x3E76D · L2ptr 0x2E70E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E111 → header 0x30000 · SprPtr 0x2ECB6 → stream 0x3E76D · L2ptr 0x2E711 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E114 → header 0x30000 · SprPtr 0x2ECB8 → stream 0x3E76D · L2ptr 0x2E714 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E117 → header 0x30000 · SprPtr 0x2ECBA → stream 0x3E76D · L2ptr 0x2E717 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E11A → header 0x30000 · SprPtr 0x2ECBC → stream 0x3E76D · L2ptr 0x2E71A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E11D → header 0x30000 · SprPtr 0x2ECBE → stream 0x3E76D · L2ptr 0x2E71D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x060
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E120 → header 0x30000 · SprPtr 0x2ECC0 → stream 0x3E76D · L2ptr 0x2E720 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x061
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E123 → header 0x30000 · SprPtr 0x2ECC2 → stream 0x3E76D · L2ptr 0x2E723 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x062
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E126 → header 0x30000 · SprPtr 0x2ECC4 → stream 0x3E76D · L2ptr 0x2E726 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x063
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E129 → header 0x30000 · SprPtr 0x2ECC6 → stream 0x3E76D · L2ptr 0x2E729 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x064
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E12C → header 0x30000 · SprPtr 0x2ECC8 → stream 0x3E76D · L2ptr 0x2E72C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x065
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E12F → header 0x30000 · SprPtr 0x2ECCA → stream 0x3E76D · L2ptr 0x2E72F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x066
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E132 → header 0x30000 · SprPtr 0x2ECCC → stream 0x3E76D · L2ptr 0x2E732 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x067
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E135 → header 0x30000 · SprPtr 0x2ECCE → stream 0x3E76D · L2ptr 0x2E735 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x068
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E138 → header 0x30000 · SprPtr 0x2ECD0 → stream 0x3E76D · L2ptr 0x2E738 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x069
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E13B → header 0x30000 · SprPtr 0x2ECD2 → stream 0x3E76D · L2ptr 0x2E73B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E13E → header 0x30000 · SprPtr 0x2ECD4 → stream 0x3E76D · L2ptr 0x2E73E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E141 → header 0x30000 · SprPtr 0x2ECD6 → stream 0x3E76D · L2ptr 0x2E741 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E144 → header 0x30000 · SprPtr 0x2ECD8 → stream 0x3E76D · L2ptr 0x2E744 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E147 → header 0x30000 · SprPtr 0x2ECDA → stream 0x3E76D · L2ptr 0x2E747 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E14A → header 0x30000 · SprPtr 0x2ECDC → stream 0x3E76D · L2ptr 0x2E74A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E14D → header 0x30000 · SprPtr 0x2ECDE → stream 0x3E76D · L2ptr 0x2E74D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x070
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E150 → header 0x30000 · SprPtr 0x2ECE0 → stream 0x3E76D · L2ptr 0x2E750 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x071
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E153 → header 0x30000 · SprPtr 0x2ECE2 → stream 0x3E76D · L2ptr 0x2E753 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x072
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E156 → header 0x30000 · SprPtr 0x2ECE4 → stream 0x3E76D · L2ptr 0x2E756 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x073
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E159 → header 0x30000 · SprPtr 0x2ECE6 → stream 0x3E76D · L2ptr 0x2E759 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x074
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E15C → header 0x30000 · SprPtr 0x2ECE8 → stream 0x3E76D · L2ptr 0x2E75C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x075
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E15F → header 0x30000 · SprPtr 0x2ECEA → stream 0x3E76D · L2ptr 0x2E75F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x076
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E162 → header 0x30000 · SprPtr 0x2ECEC → stream 0x3E76D · L2ptr 0x2E762 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x077
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E165 → header 0x30000 · SprPtr 0x2ECEE → stream 0x3E76D · L2ptr 0x2E765 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x078
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E168 → header 0x30000 · SprPtr 0x2ECF0 → stream 0x3E76D · L2ptr 0x2E768 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x079
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E16B → header 0x30000 · SprPtr 0x2ECF2 → stream 0x3E76D · L2ptr 0x2E76B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E16E → header 0x30000 · SprPtr 0x2ECF4 → stream 0x3E76D · L2ptr 0x2E76E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E171 → header 0x30000 · SprPtr 0x2ECF6 → stream 0x3E76D · L2ptr 0x2E771 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E174 → header 0x30000 · SprPtr 0x2ECF8 → stream 0x3E76D · L2ptr 0x2E774 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E177 → header 0x30000 · SprPtr 0x2ECFA → stream 0x3E76D · L2ptr 0x2E777 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E17A → header 0x30000 · SprPtr 0x2ECFC → stream 0x3E76D · L2ptr 0x2E77A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E17D → header 0x30000 · SprPtr 0x2ECFE → stream 0x3E76D · L2ptr 0x2E77D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x080
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E180 → header 0x30000 · SprPtr 0x2ED00 → stream 0x3E76D · L2ptr 0x2E780 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x081
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E183 → header 0x30000 · SprPtr 0x2ED02 → stream 0x3E76D · L2ptr 0x2E783 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x082
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E186 → header 0x30000 · SprPtr 0x2ED04 → stream 0x3E76D · L2ptr 0x2E786 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x083
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E189 → header 0x30000 · SprPtr 0x2ED06 → stream 0x3E76D · L2ptr 0x2E789 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x084
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E18C → header 0x30000 · SprPtr 0x2ED08 → stream 0x3E76D · L2ptr 0x2E78C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x085
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E18F → header 0x30000 · SprPtr 0x2ED0A → stream 0x3E76D · L2ptr 0x2E78F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x086
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E192 → header 0x30000 · SprPtr 0x2ED0C → stream 0x3E76D · L2ptr 0x2E792 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x087
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E195 → header 0x30000 · SprPtr 0x2ED0E → stream 0x3E76D · L2ptr 0x2E795 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x088
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E198 → header 0x30000 · SprPtr 0x2ED10 → stream 0x3E76D · L2ptr 0x2E798 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x089
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E19B → header 0x30000 · SprPtr 0x2ED12 → stream 0x3E76D · L2ptr 0x2E79B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E19E → header 0x30000 · SprPtr 0x2ED14 → stream 0x3E76D · L2ptr 0x2E79E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1A1 → header 0x30000 · SprPtr 0x2ED16 → stream 0x3E76D · L2ptr 0x2E7A1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1A4 → header 0x30000 · SprPtr 0x2ED18 → stream 0x3E76D · L2ptr 0x2E7A4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1A7 → header 0x30000 · SprPtr 0x2ED1A → stream 0x3E76D · L2ptr 0x2E7A7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1AA → header 0x30000 · SprPtr 0x2ED1C → stream 0x3E76D · L2ptr 0x2E7AA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1AD → header 0x30000 · SprPtr 0x2ED1E → stream 0x3E76D · L2ptr 0x2E7AD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x090
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1B0 → header 0x30000 · SprPtr 0x2ED20 → stream 0x3E76D · L2ptr 0x2E7B0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x091
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1B3 → header 0x30000 · SprPtr 0x2ED22 → stream 0x3E76D · L2ptr 0x2E7B3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x092
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1B6 → header 0x30000 · SprPtr 0x2ED24 → stream 0x3E76D · L2ptr 0x2E7B6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x093
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1B9 → header 0x30561 · SprPtr 0x2ED26 → stream 0x3C3DB · L2ptr 0x2E7B9 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x1 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,5)
    - [ ] **Sprite 0xB6** (0xB6) ×1: (0,1,16)

### Nivel 0x094
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1BC → header 0x3058B · SprPtr 0x2ED28 → stream 0x3C3E3 · L2ptr 0x2E7BC · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x1 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,6)
    - [ ] **Sprite 0xB6** (0xB6) ×2: (0,1,16) (0,14,16)

### Nivel 0x095
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1BF → header 0x30258 · SprPtr 0x2ED2A → stream 0x3C367 · L2ptr 0x2E7BF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x096
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1C2 → header 0x3025E · SprPtr 0x2ED2C → stream 0x3C359 · L2ptr 0x2E7C2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x92 (memoria 0x12, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,4)
    - [ ] **Podoboo** (0x33) ×3: (0,2,0) (0,7,0) (0,13,0)

### Nivel 0x097
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1C5 → header 0x3025E · SprPtr 0x2ED2E → stream 0x3C354 · L2ptr 0x2E7C5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x92 (memoria 0x12, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,3)

### Nivel 0x098
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1C8 → header 0x30258 · SprPtr 0x2ED30 → stream 0x3C34F · L2ptr 0x2E7C8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,2)

### Nivel 0x099
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1CB → header 0x30258 · SprPtr 0x2ED32 → stream 0x3C34A · L2ptr 0x2E7CB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,1)

### Nivel 0x09A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1CE → header 0x30258 · SprPtr 0x2ED34 → stream 0x3C345 · L2ptr 0x2E7CE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,0)

### Nivel 0x09B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1D1 → header 0x30252 · SprPtr 0x2ED36 → stream 0x3C340 · L2ptr 0x2E7D1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x10 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x10 (memoria 0x10, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **ActivateBowserBattle** (0xA0) ×1: (0,0,0)

### Nivel 0x09C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1D4 → header 0x30000 · SprPtr 0x2ED38 → stream 0x3E76D · L2ptr 0x2E7D4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x09D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1D7 → header 0x30000 · SprPtr 0x2ED3A → stream 0x3E76D · L2ptr 0x2E7D7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x09E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1DA → header 0x30000 · SprPtr 0x2ED3C → stream 0x3E76D · L2ptr 0x2E7DA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x09F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1DD → header 0x30000 · SprPtr 0x2ED3E → stream 0x3E76D · L2ptr 0x2E7DD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1E0 → header 0x30000 · SprPtr 0x2ED40 → stream 0x3E76D · L2ptr 0x2E7E0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1E3 → header 0x30000 · SprPtr 0x2ED42 → stream 0x3E76D · L2ptr 0x2E7E3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1E6 → header 0x30000 · SprPtr 0x2ED44 → stream 0x3E76D · L2ptr 0x2E7E6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1E9 → header 0x30000 · SprPtr 0x2ED46 → stream 0x3E76D · L2ptr 0x2E7E9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1EC → header 0x30000 · SprPtr 0x2ED48 → stream 0x3E76D · L2ptr 0x2E7EC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1EF → header 0x30000 · SprPtr 0x2ED4A → stream 0x3E76D · L2ptr 0x2E7EF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1F2 → header 0x30000 · SprPtr 0x2ED4C → stream 0x3E76D · L2ptr 0x2E7F2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1F5 → header 0x30000 · SprPtr 0x2ED4E → stream 0x3E76D · L2ptr 0x2E7F5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1F8 → header 0x30000 · SprPtr 0x2ED50 → stream 0x3E76D · L2ptr 0x2E7F8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1FB → header 0x30000 · SprPtr 0x2ED52 → stream 0x3E76D · L2ptr 0x2E7FB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E1FE → header 0x30000 · SprPtr 0x2ED54 → stream 0x3E76D · L2ptr 0x2E7FE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E201 → header 0x30000 · SprPtr 0x2ED56 → stream 0x3E76D · L2ptr 0x2E801 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E204 → header 0x30000 · SprPtr 0x2ED58 → stream 0x3E76D · L2ptr 0x2E804 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E207 → header 0x30000 · SprPtr 0x2ED5A → stream 0x3E76D · L2ptr 0x2E807 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E20A → header 0x30000 · SprPtr 0x2ED5C → stream 0x3E76D · L2ptr 0x2E80A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E20D → header 0x30000 · SprPtr 0x2ED5E → stream 0x3E76D · L2ptr 0x2E80D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E210 → header 0x30000 · SprPtr 0x2ED60 → stream 0x3E76D · L2ptr 0x2E810 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E213 → header 0x30000 · SprPtr 0x2ED62 → stream 0x3E76D · L2ptr 0x2E813 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E216 → header 0x30000 · SprPtr 0x2ED64 → stream 0x3E76D · L2ptr 0x2E816 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E219 → header 0x30000 · SprPtr 0x2ED66 → stream 0x3E76D · L2ptr 0x2E819 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E21C → header 0x30000 · SprPtr 0x2ED68 → stream 0x3E76D · L2ptr 0x2E81C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E21F → header 0x30000 · SprPtr 0x2ED6A → stream 0x3E76D · L2ptr 0x2E81F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E222 → header 0x30000 · SprPtr 0x2ED6C → stream 0x3E76D · L2ptr 0x2E822 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E225 → header 0x30000 · SprPtr 0x2ED6E → stream 0x3E76D · L2ptr 0x2E825 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E228 → header 0x30000 · SprPtr 0x2ED70 → stream 0x3E76D · L2ptr 0x2E828 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E22B → header 0x30000 · SprPtr 0x2ED72 → stream 0x3E76D · L2ptr 0x2E82B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E22E → header 0x30000 · SprPtr 0x2ED74 → stream 0x3E76D · L2ptr 0x2E82E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E231 → header 0x30000 · SprPtr 0x2ED76 → stream 0x3E76D · L2ptr 0x2E831 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E234 → header 0x30000 · SprPtr 0x2ED78 → stream 0x3E76D · L2ptr 0x2E834 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E23A → header 0x3676E · SprPtr 0x2ED7C → stream 0x3D741 · L2ptr 0x2E83A · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=1 SPR=3 backArea=6
- **Colisión**: 63×27 casillas · SOLID=77
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 1 L3 2 L1y 2 L2y 2 · secHdr [0x18 0xA0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x0D0
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [ ] **PorcuPuffer** (0xC3) ×2: (0,14,24) (2,33,24)
    - [ ] **Sprite 0xCF** (0xCF) ×1: (0,4,0)

### Nivel 0x0BF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E23D → header 0x34199 · SprPtr 0x2ED7E → stream 0x3D02F · L2ptr 0x2E83D · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=0
- **Colisión**: 63×27 casillas · SOLID=80
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x0BF (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Sprite 0x55** (0x55) ×1: (3,53,23)
    - [ ] **VerticalCheckerboardPlatform** (0x57) ×4: (1,21,23) (1,28,20) (2,35,24) (2,42,21)
    - [ ] **Sprite 0xD7** (0xD7) ×1: (0,8,0)

### Nivel 0x0C0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E240 → header 0x388CB · SprPtr 0x2ED80 → stream 0x3DB95 · L2ptr 0x2E840 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=7 SPR=0 backArea=0
- **Colisión**: 64×27 casillas · LEDGE_TOP=32 SOLID=70 SLOPE_STEEP=1
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x8E (memoria 0xE, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x0C0 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **BubbleWithSprite** (0x9D) ×12: (1,18,18) (1,22,19) (1,27,18) (1,30,16) (2,34,20) (2,38,18) …

### Nivel 0x0C1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E243 → header 0x34375 · SprPtr 0x2ED82 → stream 0x3D0CF · L2ptr 0x2E843 · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=1 SPR=3 backArea=2
- **Colisión**: 52×27 casillas · SOLID=48
- **Entrada**: casilla (1,3) px (16,48) · pantalla entrada 3 · L2scroll 5 L3 2 L1y 2 L2y 2 · secHdr [0x51 0xA0 0xA 0x3]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 0→0x0C1 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **PorcuPuffer** (0xC3) ×2: (1,17,24) (2,34,24)

### Nivel 0x0C2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E246 → header 0x32270 · SprPtr 0x2ED84 → stream 0x3C9AA · L2ptr 0x2E846 · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xA · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,25) px (16,400) · pantalla entrada 4 · **VERTICAL** · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xD 0x18 0xA 0x64]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (10)**:
    - [ ] **BobOmb** (0xB) ×10: (0,15,23) (0,15,14) (1,26,29) (1,26,19) (1,26,11) (2,41,23) …

### Nivel 0x0C3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E249 → header 0x31D83 · SprPtr 0x2ED86 → stream 0x3C8EA · L2ptr 0x2E849 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 64×27 casillas · LEDGE_TOP=26 SOLID=18
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x0C3 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×2: (1,27,21) (2,35,19)
    - [ ] **ShiftingPipe** (0x49) ×6: (0,13,22) (1,17,21) (1,24,22) (1,29,21) (2,37,19) (2,42,23)

### Nivel 0x0C4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E24C → header 0x3194F · SprPtr 0x2ED88 → stream 0x3C3F5 · L2ptr 0x2E84C · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=4 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0C5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E24F → header 0x30603 · SprPtr 0x2ED8A → stream 0x3C441 · L2ptr 0x2E84F · GFXslot 0x028E7 · FGBGslot 0x02947
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 15` (tilesetFG=7)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=0
- **Colisión**: 16×27 casillas · LEDGE_TOP=16
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 1 · secHdr [0x5B 0x1 0x9 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **DisplayMessage** (0x19) ×1: (0,0,0)

### Nivel 0x0C6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E252 → header 0x34949 · SprPtr 0x2ED8C → stream 0x3C3F0 · L2ptr 0x2E852 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=26 SOLID=17 SLOPE_STEEP=1
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x0C7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E255 → header 0x305B5 · SprPtr 0x2ED8E → stream 0x3C427 · L2ptr 0x2E855 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=2 FG=0 SPR=0 backArea=6
- **Colisión**: 128×27 casillas · LEDGE_TOP=124 SOLID=1 SLOPE=11 SLOPE_STEEP=10
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5A 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — Pokey (0x70)
- **Enemigos (8)**:
    - [ ] **GreenKoopaNoShell** (0x4) ×1: (4,67,23)
    - [s] **RedKoopaNoShell** (0x5) ×2: (0,11,22) (4,70,23)
    - [ ] **BlueKoopaNoShell** (0x6) ×1: (4,73,23)
    - [ ] **YellowKoopaNoShell** (0x7) ×1: (4,76,23)
    - [s] **JumpingPiranhaPlant** (0x4F) ×1: (6,107,22)
    - [B] **Pokey** (0x70) ×1: (6,101,19)
    - [ ] **SlidingNakedBlueKoopa** (0xBD) ×1: (3,55,17)

### Nivel 0x0C8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E258 → header 0x3977C · SprPtr 0x2ED90 → stream 0x3DDCF · L2ptr 0x2E858 · GFXslot 0x028CB · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=2 backArea=5
- **Colisión**: 95×27 casillas · SOLID=65
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 6 L3 0 L1y 2 L2y 2 · secHdr [0x6B 0x1 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (16)**:
    - [ ] **Sparky** (0xA5) ×15: (1,23,6) (1,25,11) (1,31,7) (2,36,10) (2,43,16) (2,47,16) …
    - [ ] **Sprite 0xE8** (0xE8) ×1: (0,8,1)

### Nivel 0x0C9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E25B → header 0x3087D · SprPtr 0x2ED92 → stream 0x3C4C0 · L2ptr 0x2E85B · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x0CA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E25E → header 0x307AE · SprPtr 0x2ED94 → stream 0x3C44B · L2ptr 0x2E85E · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x0CB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E261 → header 0x33CEE · SprPtr 0x2ED96 → stream 0x3C3F0 · L2ptr 0x2E861 · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=3 backArea=2
- **Colisión**: 17×27 casillas · SOLID=4
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x18 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x0CC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E264 → header 0x30636 · SprPtr 0x2ED98 → stream 0x3D51D · L2ptr 0x2E864 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,1)

### Nivel 0x0CD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E267 → header 0x36C24 · SprPtr 0x2ED9A → stream 0x3D899 · L2ptr 0x2E867 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=0 SPR=0 backArea=6
- **Colisión**: 64×27 casillas · LEDGE_TOP=33 SOLID=34
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **PSwitch** (0x3E) ×1: (0,8,23)
    - [ ] **GoalTape** (0x7B) ×1: (2,46,23)

### Nivel 0x0CE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E26A → header 0x36B0B · SprPtr 0x2ED9C → stream 0x3D84B · L2ptr 0x2E86A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=0 backArea=2
- **Colisión**: 64×27 casillas · LEDGE_TOP=23 SOLID=122 SLOPE_STEEP=3
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x8E (memoria 0xE, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x0CD
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **BubbleWithSprite** (0x9D) ×11: (1,19,17) (1,23,19) (1,27,16) (1,27,20) (2,35,18) (2,39,17) …

### Nivel 0x0CF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E26D → header 0x36985 · SprPtr 0x2ED9E → stream 0x3D7E5 · L2ptr 0x2E86D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=4 SPR=0 backArea=6
- **Colisión**: 112×27 casillas · LEDGE_TOP=21 SOLID=136 SLOPE=1 SLOPE_STEEP=3
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x0CE
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Feather** (0x77) ×1: (0,4,23)

### Nivel 0x0D0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E270 → header 0x36444 · SprPtr 0x2EDA0 → stream 0x3D6D9 · L2ptr 0x2E870 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=190 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (8,19) px (128,304) · pantalla entrada 16 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x59 0x31 0x9A 0x10]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x0F6 · pant 15→0x0BE · pant 16→0x0F5
- **Usa sprites grandes**: no
- **Enemigos (34)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (17,283,20)
    - [ ] **PortableSpringboard** (0x2F) ×2: (9,150,23) (18,291,23)
    - [s] **PSwitch** (0x3E) ×1: (10,166,22)
    - [s] **JumpingPiranhaPlant** (0x4F) ×4: (5,93,21) (14,229,21) (15,242,22) (15,254,19)
    - [ ] **DinoRhino** (0x6E) ×6: (1,17,23) (3,50,22) (4,72,22) (8,130,17) (12,199,21) (15,245,23)
    - [ ] **DinoTorch** (0x6F) ×14: (1,27,16) (2,38,16) (3,62,22) (4,65,22) (4,68,21) (5,82,21) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **LeftFlyingBlock** (0x83) ×2: (4,73,18) (12,205,16)
    - [ ] **WarpHole** (0x8E) ×1: (16,266,20)
    - [ ] **ClappinChuck** (0x95) ×1: (10,172,17)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (2,36,16)

### Nivel 0x0D1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E273 → header 0x36444 · SprPtr 0x2EDA2 → stream 0x3D6D9 · L2ptr 0x2E873 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=190 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 7 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x58 0x30 0x9A 0x7]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x0F6 · pant 15→0x0BE · pant 16→0x0F5
- **Usa sprites grandes**: no
- **Enemigos (34)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (17,283,20)
    - [ ] **PortableSpringboard** (0x2F) ×2: (9,150,23) (18,291,23)
    - [s] **PSwitch** (0x3E) ×1: (10,166,22)
    - [s] **JumpingPiranhaPlant** (0x4F) ×4: (5,93,21) (14,229,21) (15,242,22) (15,254,19)
    - [ ] **DinoRhino** (0x6E) ×6: (1,17,23) (3,50,22) (4,72,22) (8,130,17) (12,199,21) (15,245,23)
    - [ ] **DinoTorch** (0x6F) ×14: (1,27,16) (2,38,16) (3,62,22) (4,65,22) (4,68,21) (5,82,21) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **LeftFlyingBlock** (0x83) ×2: (4,73,18) (12,205,16)
    - [ ] **WarpHole** (0x8E) ×1: (16,266,20)
    - [ ] **ClappinChuck** (0x95) ×1: (10,172,17)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (2,36,16)

### Nivel 0x0D2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E276 → header 0x31D4C · SprPtr 0x2EDA4 → stream 0x3C8CD · L2ptr 0x2E876 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=21 SOLID=53 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x0D2 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (9)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×9: (0,10,23) (0,10,21) (1,16,23) (1,16,21) (1,21,22) (1,22,22) …

### Nivel 0x0D3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E279 → header 0x38BEA · SprPtr 0x2EDA6 → stream 0x3DC22 · L2ptr 0x2E879 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x1 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,6)
    - [ ] **Sprite 0xB6** (0xB6) ×2: (0,1,16) (0,14,16)

### Nivel 0x0D4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E27C → header 0x38B4A · SprPtr 0x2EDA8 → stream 0x3DBF9 · L2ptr 0x2E87C · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 3 · secHdr [0x49 0xC0 0xB 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 7→0x0D3
- **Usa sprites grandes**: no
- **Enemigos (13)**:
    - [ ] **ThrowingDryBones** (0x30) ×1: (6,104,12)
    - [ ] **Sparky** (0xA5) ×5: (1,22,20) (3,62,19) (5,84,17) (6,108,14) (6,109,23)
    - [ ] **Sprite 0xA6** (0xA6) ×4: (2,38,19) (4,68,19) (5,93,19) (6,109,19)
    - [ ] **MovingCastleStone** (0xBB) ×2: (5,82,19) (5,89,23)
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,0)

### Nivel 0x0D5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E27F → header 0x30636 · SprPtr 0x2EDAA → stream 0x3C414 · L2ptr 0x2E87F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x0D6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E282 → header 0x36307 · SprPtr 0x2EDAC → stream 0x3D668 · L2ptr 0x2E882 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=4
- **Colisión**: 256×27 casillas · LEDGE_TOP=115 SOLID=147 SLOPE=2 SLOPE_STEEP=11
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 8→0x0D5 · pant 15→0x0D5
- **Usa sprites grandes**: no
- **Enemigos (37)**:
    - [ ] **Podoboo** (0x33) ×26: (1,21,19) (1,21,15) (3,51,11) (3,54,11) (4,68,14) (4,73,13) …
    - [ ] **NonLineGuideGrinder** (0xB4) ×11: (1,16,14) (2,33,23) (2,36,23) (4,72,22) (4,79,22) (5,86,22) …

### Nivel 0x0D7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E285 → header 0x36DE7 · SprPtr 0x2EDAE → stream 0x3D956 · L2ptr 0x2E885 · GFXslot 0x028CB · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 7 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=5 SPR=2 backArea=0
- **Colisión**: 32×27 casillas · LEDGE_TOP=17 SOLID=55 SLOPE=10 SLOPE_STEEP=10
- **Entrada**: casilla (1,6) px (16,96) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 0 L2y 3 · secHdr [0x52 0x18 0x3 0x0]
- **Cabecera sprites**: 0x1 (memoria 0x1, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x0D7 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Sprite 0x55** (0x55) ×1: (0,7,5)
    - [ ] **BrownChainedPlatform** (0x5F) ×1: (0,13,5)

### Nivel 0x0D8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E288 → header 0x33BC9 · SprPtr 0x2EDB0 → stream 0x3CEBA · L2ptr 0x2E888 · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 71×27 casillas · LEDGE_TOP=14 SOLID=146 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 4 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x20 0xA 0x4]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x0D8 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **PSwitch** (0x3E) ×1: (3,60,23)

### Nivel 0x0D9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E28B → header 0x30636 · SprPtr 0x2EDB2 → stream 0x3D152 · L2ptr 0x2E88B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,2)

### Nivel 0x0DB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E291 → header 0x34559 · SprPtr 0x2EDB6 → stream 0x3D111 · L2ptr 0x2E891 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xA · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,23) px (128,368) · pantalla entrada 4 · **VERTICAL** · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xC 0x1 0xA 0x64]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (21)**:
    - [ ] **Sprite 0x22** (0x22) ×6: (1,29,4) (2,33,20) (2,38,26) (2,38,10) (2,43,20) (3,52,6)
    - [ ] **Sprite 0x23** (0x23) ×3: (1,29,7) (2,39,7) (3,52,9)
    - [ ] **Grinder** (0x24) ×11: (0,13,3) (1,16,9) (1,19,9) (1,29,12) (2,33,26) (2,33,12) …
    - [ ] **Sprite 0x25** (0x25) ×1: (1,24,3)

### Nivel 0x0DC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E294 → header 0x34495 · SprPtr 0x2EDB8 → stream 0x3D0F4 · L2ptr 0x2E894 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 176×27 casillas · LEDGE_TOP=146 SOLID=39
- **Entrada**: casilla (8,17) px (128,272) · pantalla entrada 9 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x48 0xC1 0xA 0x9]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 0→0x0DB
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (9)**:
    - [B] **BonyBeetle** (0x31) ×2: (6,99,23) (6,108,23)
    - [ ] **Podoboo** (0x33) ×6: (4,76,17) (5,82,17) (5,88,17) (5,94,17) (7,123,17) (8,132,17)
    - [ ] **Sprite 0xF2** (0xF2) ×1: (9,152,0)

### Nivel 0x0DD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E297 → header 0x351D6 · SprPtr 0x2EDBA → stream 0x3D304 · L2ptr 0x2E897 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 7 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=7
- **Colisión**: 64×27 casillas · SOLID=40
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x18 0x20 0xA 0x0]
- **Cabecera sprites**: 0x1 (memoria 0x1, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x0DD (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Sprite 0x63** (0x63) ×1: (0,2,24)
    - [ ] **Sprite 0x64** (0x64) ×1: (0,6,16)

### Nivel 0x0DE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E29A → header 0x3189D · SprPtr 0x2EDBC → stream 0x3C7BD · L2ptr 0x2E89A · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=4 SPR=5 backArea=6
- **Colisión**: 30×27 casillas · SOLID=1 SLOPE=6 SLOPE_STEEP=6
- **Entrada**: casilla (8,21) px (128,336) · pantalla entrada 1 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5A 0x1 0xA 0x1]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x0EB · pant 1→0x0FE
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,6,15) (0,11,24) (1,23,14) (1,28,21)

### Nivel 0x0DF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E29D → header 0x30636 · SprPtr 0x2EDBE → stream 0x3C414 · L2ptr 0x2E89D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x0E0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2A0 → header 0x33DB6 · SprPtr 0x2EDC0 → stream 0x3CF4D · L2ptr 0x2E8A0 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=2 SPR=1 backArea=2
- **Colisión**: 128×27 casillas · LEDGE_TOP=110 SOLID=78 SLOPE_STEEP=17
- **Entrada**: casilla (8,19) px (128,304) · pantalla entrada 4 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x59 0x39 0xA 0x4]
- **Cabecera sprites**: 0x8E (memoria 0xE, buoyancy 0x80)
- **Salidas de pantalla**: pant 7→0x0DF
- **Usa sprites grandes**: sí — Thwomp (0x26), BonyBeetle (0x31)
- **Enemigos (32)**:
    - [B] **Thwomp** (0x26) ×4: (1,23,14) (2,40,14) (3,62,14) (5,84,14)
    - [ ] **ThrowingDryBones** (0x30) ×3: (3,49,18) (5,82,23) (6,107,24)
    - [B] **BonyBeetle** (0x31) ×4: (2,34,23) (2,36,23) (7,117,19) (7,122,19)
    - [ ] **BallNChain** (0x9E) ×7: (2,47,5) (3,54,19) (3,58,5) (4,69,5) (5,80,5) (6,100,16) …
    - [ ] **Fishbone** (0xAA) ×14: (1,20,23) (1,23,21) (1,31,21) (2,39,18) (2,42,7) (3,61,7) …

### Nivel 0x0E1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2A3 → header 0x33DB6 · SprPtr 0x2EDC2 → stream 0x3CF4D · L2ptr 0x2E8A3 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=2 SPR=1 backArea=2
- **Colisión**: 128×27 casillas · LEDGE_TOP=110 SOLID=78 SLOPE_STEEP=17
- **Entrada**: casilla (8,19) px (128,304) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x59 0x39 0xA 0x0]
- **Cabecera sprites**: 0x8E (memoria 0xE, buoyancy 0x80)
- **Salidas de pantalla**: pant 7→0x0DF
- **Usa sprites grandes**: sí — Thwomp (0x26), BonyBeetle (0x31)
- **Enemigos (32)**:
    - [B] **Thwomp** (0x26) ×4: (1,23,14) (2,40,14) (3,62,14) (5,84,14)
    - [ ] **ThrowingDryBones** (0x30) ×3: (3,49,18) (5,82,23) (6,107,24)
    - [B] **BonyBeetle** (0x31) ×4: (2,34,23) (2,36,23) (7,117,19) (7,122,19)
    - [ ] **BallNChain** (0x9E) ×7: (2,47,5) (3,54,19) (3,58,5) (4,69,5) (5,80,5) (6,100,16) …
    - [ ] **Fishbone** (0xAA) ×14: (1,20,23) (1,23,21) (1,31,21) (2,39,18) (2,42,7) (3,61,7) …

### Nivel 0x0E2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2A6 → header 0x30636 · SprPtr 0x2EDC4 → stream 0x3C414 · L2ptr 0x2E8A6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x0E3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2A9 → header 0x31473 · SprPtr 0x2EDC6 → stream 0x3C749 · L2ptr 0x2E8A9 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 14 pantallas (224 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=2
- **Colisión**: 224×27 casillas · LEDGE_TOP=220 SOLID=422 SLOPE=4 SLOPE_STEEP=4
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 13→0x0FD (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Feather** (0x77) ×1: (0,5,23)
    - [ ] **MessageBox** (0xB9) ×1: (0,7,20)

### Nivel 0x0E4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2AC → header 0x3244F · SprPtr 0x2EDC8 → stream 0x3CA0C · L2ptr 0x2E8AC · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 16×27 casillas · SOLID=42
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2A 0x0 0xA 0x0]
- **Cabecera sprites**: 0x9 (memoria 0x9, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [ ] **Sprite 0x37** (0x37) ×2: (0,2,15) (0,10,15)
    - [ ] **BigBooBoss** (0xC5) ×1: (0,11,13)

### Nivel 0x0E5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2AF → header 0x30636 · SprPtr 0x2EDCA → stream 0x3C943 · L2ptr 0x2E8AF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,0)

### Nivel 0x0E7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2B5 → header 0x31F64 · SprPtr 0x2EDCE → stream 0x3C926 · L2ptr 0x2E8B5 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x8 · música 3 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,25) px (16,400) · pantalla entrada 7 · **VERTICAL** · L2scroll 7 L3 3 L1y 0 L2y 3 · secHdr [0x7D 0xC0 0x3 0x67]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (9)**:
    - [ ] **PortableSpringboard** (0x2F) ×1: (5,85,4)
    - [ ] **Sprite 0x32** (0x32) ×7: (0,9,11) (2,40,13) (2,40,9) (2,45,13) (3,53,3) (4,68,10) …
    - [ ] **Sprite 0xEF** (0xEF) ×1: (7,119,0)

### Nivel 0x0E8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2B8 → header 0x31E2E · SprPtr 0x2EDD0 → stream 0x3C915 · L2ptr 0x2E8B8 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=52 SOLID=43 SLOPE_STEEP=8
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 2 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x1 0xA 0x2]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 0→0x0E7
- **Usa sprites grandes**: sí — Thwomp (0x26)
- **Enemigos (5)**:
    - [B] **Thwomp** (0x26) ×3: (1,18,3) (1,28,11) (2,35,14)
    - [ ] **Sprite 0x32** (0x32) ×1: (0,2,5)
    - [ ] **Podoboo** (0x33) ×1: (1,22,8)

### Nivel 0x0E9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2BB → header 0x3178E · SprPtr 0x2EDD2 → stream 0x3C7A7 · L2ptr 0x2E8BB · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=4
- **Colisión**: 64×27 casillas · LEDGE_TOP=54 SOLID=76 SLOPE_STEEP=11
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x0FF
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Keyhole** (0xE) ×1: (2,37,6)
    - [ ] **Key** (0x80) ×1: (2,40,6)
    - [ ] **Sprite 0x97** (0x97) ×1: (1,21,23)
    - [ ] **Sprite 0xDC** (0xDC) ×1: (3,52,23)

### Nivel 0x0EA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2BE → header 0x385B4 · SprPtr 0x2EDD4 → stream 0x3DADD · L2ptr 0x2E8BE · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0xA · música 7 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=4 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,0) px (128,0) · pantalla entrada 0 · **VERTICAL** · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0x0 0x1 0x0 0x60]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (16)**:
    - [ ] **PortableSpringboard** (0x2F) ×1: (11,188,22)
    - [s] **PSwitch** (0x3E) ×1: (0,10,6)
    - [ ] **Sprite 0x74** (0x74) ×1: (5,95,21)
    - [ ] **Sprite 0x75** (0x75) ×1: (8,143,24)
    - [ ] **Star** (0x76) ×1: (4,79,19)
    - [ ] **Feather** (0x77) ×2: (1,31,17) (7,127,24)
    - [ ] **Sprite 0x78** (0x78) ×9: (3,63,19) (6,111,26) (6,111,25) (6,111,24) (6,111,23) (6,111,22) …

### Nivel 0x0EB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2C1 → header 0x30621 · SprPtr 0x2EDD6 → stream 0x3C40C · L2ptr 0x2E8C1 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23 EE1)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0EC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2C4 → header 0x322F2 · SprPtr 0x2EDD8 → stream 0x3C9CA · L2ptr 0x2E8C4 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 79×27 casillas · SOLID=2 SLOPE=16 SLOPE_STEEP=16
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 2 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x2]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 4→0x0ED
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **BouncingFootball** (0x28) ×1: (3,51,20)
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,41,15)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,42,22)
    - [s] **PSwitch** (0x3E) ×1: (2,46,23)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,26,20)

### Nivel 0x0ED
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2C7 → header 0x32374 · SprPtr 0x2EDDA → stream 0x3C9DB · L2ptr 0x2E8C7 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 74×27 casillas · SOLID=11
- **Entrada**: casilla (14,22) px (224,352) · pantalla entrada 4 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x3 0xA 0x4]
- **Cabecera sprites**: 0x7 (memoria 0x7, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x0EC · pant 1→0x0F1 · pant 2→0x0F0 · pant 3→0x0E4
- **Usa sprites grandes**: no
- **Enemigos (7)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,12,22) (1,18,21) (2,37,20) (3,62,18)
    - [s] **PSwitch** (0x3E) ×1: (0,6,18)
    - [ ] **MessageBox** (0xB9) ×1: (1,20,21)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (3,49,20)

### Nivel 0x0EE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2CA → header 0x322F2 · SprPtr 0x2EDDC → stream 0x3C9CA · L2ptr 0x2E8CA · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 79×27 casillas · SOLID=2 SLOPE=16 SLOPE_STEEP=16
- **Entrada**: casilla (14,22) px (224,352) · pantalla entrada 4 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x3 0xA 0x4]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 4→0x0ED
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **BouncingFootball** (0x28) ×1: (3,51,20)
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,41,15)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,42,22)
    - [s] **PSwitch** (0x3E) ×1: (2,46,23)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,26,20)

### Nivel 0x0EF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2CD → header 0x36EFD · SprPtr 0x2EDDE → stream 0x3D9B1 · L2ptr 0x2E8CD · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=1 backArea=5
- **Colisión**: 192×27 casillas · LEDGE_TOP=160 SOLID=68 SLOPE_STEEP=28
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x17 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x0E2
- **Usa sprites grandes**: sí — Thwomp (0x26)
- **Enemigos (20)**:
    - [B] **Thwomp** (0x26) ×12: (0,12,12) (1,25,14) (2,35,14) (3,52,15) (3,57,15) (4,71,14) …
    - [ ] **Thwimp** (0x27) ×8: (2,37,23) (4,69,23) (5,83,23) (6,101,22) (6,103,22) (6,108,22) …

### Nivel 0x0F0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2D0 → header 0x30621 · SprPtr 0x2EDE0 → stream 0x3C3F5 · L2ptr 0x2E8D0 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0F1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2D3 → header 0x32420 · SprPtr 0x2EDE2 → stream 0x3C9F2 · L2ptr 0x2E8D3 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 47×27 casillas · SOLID=1
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 1 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x1 0xA 0x1]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x0EE · pant 2→0x0F2
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **Sprite 0x37** (0x37) ×5: (0,4,17) (0,8,21) (0,14,20) (1,18,17) (1,30,17)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,35,20)
    - [ ] **Sprite 0x39** (0x39) ×2: (2,41,16) (2,44,22)

### Nivel 0x0F2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2D6 → header 0x32374 · SprPtr 0x2EDE4 → stream 0x3C9DB · L2ptr 0x2E8D6 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 74×27 casillas · SOLID=11
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x58 0x0 0xA 0x0]
- **Cabecera sprites**: 0x7 (memoria 0x7, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x0EC · pant 1→0x0F1 · pant 2→0x0F0 · pant 3→0x0E4
- **Usa sprites grandes**: no
- **Enemigos (7)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,12,22) (1,18,21) (2,37,20) (3,62,18)
    - [s] **PSwitch** (0x3E) ×1: (0,6,18)
    - [ ] **MessageBox** (0xB9) ×1: (1,20,21)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (3,49,20)

### Nivel 0x0F3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2D9 → header 0x350DC · SprPtr 0x2EDE6 → stream 0x3C3F0 · L2ptr 0x2E8D9 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=6
- **Colisión**: 17×27 casillas · SOLID=4
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x18 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x0F5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2DF → header 0x365D0 · SprPtr 0x2EDEA → stream 0x3D6D9 · L2ptr 0x2E8DF · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=200 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (8,19) px (128,304) · pantalla entrada 16 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x59 0x31 0xA 0x10]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x0D1 · pant 15→0x0BE · pant 16→0x0D0
- **Usa sprites grandes**: no
- **Enemigos (34)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (17,283,20)
    - [ ] **PortableSpringboard** (0x2F) ×2: (9,150,23) (18,291,23)
    - [s] **PSwitch** (0x3E) ×1: (10,166,22)
    - [s] **JumpingPiranhaPlant** (0x4F) ×4: (5,93,21) (14,229,21) (15,242,22) (15,254,19)
    - [ ] **DinoRhino** (0x6E) ×6: (1,17,23) (3,50,22) (4,72,22) (8,130,17) (12,199,21) (15,245,23)
    - [ ] **DinoTorch** (0x6F) ×14: (1,27,16) (2,38,16) (3,62,22) (4,65,22) (4,68,21) (5,82,21) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **LeftFlyingBlock** (0x83) ×2: (4,73,18) (12,205,16)
    - [ ] **WarpHole** (0x8E) ×1: (16,266,20)
    - [ ] **ClappinChuck** (0x95) ×1: (10,172,17)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (2,36,16)

### Nivel 0x0F6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2E2 → header 0x365D0 · SprPtr 0x2EDEC → stream 0x3D6D9 · L2ptr 0x2E8E2 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=200 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 7 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x58 0x30 0xA 0x7]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x0D1 · pant 15→0x0BE · pant 16→0x0D0
- **Usa sprites grandes**: no
- **Enemigos (34)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (17,283,20)
    - [ ] **PortableSpringboard** (0x2F) ×2: (9,150,23) (18,291,23)
    - [s] **PSwitch** (0x3E) ×1: (10,166,22)
    - [s] **JumpingPiranhaPlant** (0x4F) ×4: (5,93,21) (14,229,21) (15,242,22) (15,254,19)
    - [ ] **DinoRhino** (0x6E) ×6: (1,17,23) (3,50,22) (4,72,22) (8,130,17) (12,199,21) (15,245,23)
    - [ ] **DinoTorch** (0x6F) ×14: (1,27,16) (2,38,16) (3,62,22) (4,65,22) (4,68,21) (5,82,21) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **LeftFlyingBlock** (0x83) ×2: (4,73,18) (12,205,16)
    - [ ] **WarpHole** (0x8E) ×1: (16,266,20)
    - [ ] **ClappinChuck** (0x95) ×1: (10,172,17)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (2,36,16)

### Nivel 0x0F7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2E5 → header 0x38DAB · SprPtr 0x2EDEE → stream 0x3DC61 · L2ptr 0x2E8E5 · GFXslot 0x028CB · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 26 pantallas (416 casillas) · modo 0xA · música 4 · tiempo 400 · Layer2 fondo · paletas BG=5 FG=4 SPR=2 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,3) px (16,48) · pantalla entrada 0 · **VERTICAL** · L2scroll 0 L3 0 L1y 0 L2y 3 · secHdr [0x1 0x20 0x3 0x60]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (59)**:
    - [ ] **GreenKoopaNoShell** (0x4) ×5: (4,70,25) (5,84,8) (8,128,26) (8,128,6) (8,128,16)
    - [s] **RedKoopaNoShell** (0x5) ×6: (2,39,9) (3,55,10) (5,84,28) (5,84,3) (7,120,19) (7,120,13)
    - [ ] **BlueKoopaNoShell** (0x6) ×2: (5,84,13) (7,118,16)
    - [ ] **YellowKoopaNoShell** (0x7) ×3: (5,84,18) (7,124,22) (7,124,10)
    - [ ] **BobOmb** (0xB) ×24: (1,26,11) (1,31,14) (2,36,19) (2,46,21) (2,46,19) (2,46,17) …
    - [ ] **GoalSphere** (0x4A) ×1: (24,388,22)
    - [ ] **Sprite 0x74** (0x74) ×1: (5,84,23)
    - [ ] **Star** (0x76) ×1: (0,7,2)
    - [ ] **Sprite 0x78** (0x78) ×3: (25,400,2) (25,402,28) (25,402,29)
    - [ ] **Sprite 0xA4** (0xA4) ×13: (4,76,9) (10,162,16) (12,202,28) (12,202,22) (12,202,10) (12,202,3) …

### Nivel 0x0F8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2E8 → header 0x38CC6 · SprPtr 0x2EDF0 → stream 0x3DC3B · L2ptr 0x2E8E8 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0xC · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 127×27 casillas · SOLID=18
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x17 0x38 0xA 0x0]
- **Cabecera sprites**: 0x87 (memoria 0x7, buoyancy 0x80)
- **Salidas de pantalla**: pant 7→0x0F7
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Sprite 0x37** (0x37) ×2: (6,101,20) (6,107,14)
    - [ ] **Sprite 0x38** (0x38) ×3: (0,15,23) (1,18,19) (1,25,21)
    - [ ] **Sprite 0x39** (0x39) ×3: (5,87,23) (5,93,17) (6,96,23)
    - [ ] **Sprite 0xD2** (0xD2) ×1: (5,95,0)
    - [ ] **Sprite 0xE2** (0xE2) ×2: (6,110,22) (7,124,14)
    - [ ] **Sprite 0xE5** (0xE5) ×1: (2,33,0)

### Nivel 0x0F9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2EB → header 0x3189D · SprPtr 0x2EDF2 → stream 0x3C7BD · L2ptr 0x2E8EB · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=4 SPR=5 backArea=6
- **Colisión**: 30×27 casillas · SOLID=1 SLOPE=6 SLOPE_STEEP=6
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x0EB · pant 1→0x0FE
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,6,15) (0,11,24) (1,23,14) (1,28,21)

### Nivel 0x0FB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2F1 → header 0x30621 · SprPtr 0x2EDF6 → stream 0x3C3F5 · L2ptr 0x2E8F1 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0FC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2F4 → header 0x36815 · SprPtr 0x2EDF8 → stream 0x3D799 · L2ptr 0x2E8F4 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 95×27 casillas · SOLID=2
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x0FB
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Sprite 0xAF** (0xAF) ×6: (1,16,24) (1,17,24) (1,18,24) (5,81,22) (5,89,14) (5,91,19)
    - [ ] **Sprite 0xB0** (0xB0) ×6: (2,32,20) (3,49,18) (3,55,17) (4,68,20) (4,73,13) (5,88,22)

### Nivel 0x0FE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2FA → header 0x318F0 · SprPtr 0x2EDFC → stream 0x3C7CB · L2ptr 0x2E8FA · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=4 SPR=5 backArea=6
- **Colisión**: 30×27 casillas · SOLID=1 SLOPE=6 SLOPE_STEEP=6
- **Entrada**: casilla (8,21) px (128,336) · pantalla entrada 1 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5A 0x1 0xA 0x1]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x0FA · pant 1→0x0DE
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,6,15) (0,11,24) (1,23,14) (1,28,21)

### Nivel 0x0FF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E2FD → header 0x3063C · SprPtr 0x2EDFE → stream 0x3C3F0 · L2ptr 0x2E8FD · GFXslot 0x028CF · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=6
- **Colisión**: 32×27 casillas · LEDGE_TOP=31 SOLID=11 SLOPE_STEEP=1
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x100
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E300 → header 0x30654 · SprPtr 0x2EE00 → stream 0x3C407 · L2ptr 0x2E900 · GFXslot 0x028DB · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=6) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 7 · tiempo 0 · Layer2 fondo · paletas BG=5 FG=0 SPR=4 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0x7 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **BonusGame** (0x82) ×1: (0,5,7)

### Nivel 0x101 — #1 IGGY'S CASTLE
- **Nombre (overworld)**: #1 IGGY'S CASTLE
- **Tipo**: nivel de MAPA (translevel 0x25)
- **Direcciones**: L1ptr 0x2E303 → header 0x30FFD · SprPtr 0x2EE02 → stream 0x3C66F · L2ptr 0x2E903 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=86 SOLID=29
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0x7A 0x0]
- **Cabecera sprites**: 0x82 (memoria 0x2, buoyancy 0x80)
- **Salidas de pantalla**: pant 7→0x1FC
- **Usa sprites grandes**: no
- **Enemigos (26)**:
    - [ ] **Sprite 0x22** (0x22) ×7: (1,30,20) (2,42,20) (3,54,20) (4,78,20) (5,90,18) (5,92,21) …
    - [ ] **Grinder** (0x24) ×6: (1,17,19) (1,21,18) (2,37,16) (3,59,18) (5,81,16) (5,92,16)
    - [ ] **Sprite 0x25** (0x25) ×4: (1,25,16) (3,50,19) (4,68,21) (6,100,21)
    - [ ] **Podoboo** (0x33) ×1: (4,66,20)
    - [s] **PSwitch** (0x3E) ×1: (2,42,23)
    - [ ] **ClimbingNetDoor** (0x54) ×3: (5,80,17) (6,96,19) (6,106,19)
    - [ ] **MessageBox** (0xB9) ×2: (0,6,21) (7,115,21)
    - [ ] **Sprite 0xE6** (0xE6) ×2: (0,0,0) (7,112,0)

### Nivel 0x102 — YOSHI'S ISLAND 4
- **Nombre (overworld)**: YOSHI'S ISLAND 4
- **Tipo**: nivel de MAPA (translevel 0x26)
- **Direcciones**: L1ptr 0x2E306 → header 0x30EAD · SprPtr 0x2EE04 → stream 0x3C5F4 · L2ptr 0x2E906 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=1 SPR=2 backArea=6
- **Colisión**: 176×27 casillas · SOLID=47
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · no-Yoshi · L2scroll 1 L3 2 L1y 2 L2y 2 · secHdr [0x18 0x80 0xA 0x80]
- **Cabecera sprites**: 0xC0 (memoria 0x0, buoyancy 0xC0)
- **Salidas de pantalla**: pant 3→0x1BE · pant 10→0x1FF
- **Usa sprites grandes**: no
- **Enemigos (33)**:
    - [s] **RedKoopaNoShell** (0x5) ×3: (5,84,12) (5,88,12) (5,92,12)
    - [ ] **Sprite 0x15** (0x15) ×2: (5,83,25) (5,93,25)
    - [ ] **SurfaceJumpingCheepCheep** (0x18) ×1: (10,167,25)
    - [s] **PSwitch** (0x3E) ×1: (4,72,13)
    - [ ] **SwimmingAndJumpingCheepCheep** (0x47) ×3: (0,5,25) (0,9,24) (0,13,25)
    - [ ] **Sprite 0x5D** (0x5D) ×13: (1,16,23) (1,27,23) (2,40,23) (2,46,23) (3,60,23) (4,66,23) …
    - [ ] **Sprite 0xA4** (0xA4) ×9: (3,57,25) (4,69,23) (7,113,25) (7,118,23) (8,133,25) (8,139,23) …
    - [ ] **Sprite 0xDB** (0xDB) ×1: (4,76,21)

### Nivel 0x103 — YOSHI'S ISLAND 3
- **Nombre (overworld)**: YOSHI'S ISLAND 3
- **Tipo**: nivel de MAPA (translevel 0x27)
- **Direcciones**: L1ptr 0x2E309 → header 0x30BDE · SprPtr 0x2EE06 → stream 0x3C593 · L2ptr 0x2E909 · GFXslot 0x028CB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 21 pantallas (336 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=1 SPR=2 backArea=2
- **Colisión**: 336×27 casillas · LEDGE_TOP=170 SOLID=172 SLOPE_STEEP=7
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x80]
- **Cabecera sprites**: 0x1 (memoria 0x1, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x1FD
- **Usa sprites grandes**: no
- **Enemigos (30)**:
    - [s] **RedKoopaNoShell** (0x5) ×2: (6,106,8) (10,172,19)
    - [ ] **RedParakoopa** (0x9) ×1: (9,159,23)
    - [ ] **BobOmb** (0xB) ×4: (4,79,5) (5,87,8) (15,250,3) (15,250,14)
    - [ ] **Sprite 0x55** (0x55) ×2: (5,85,13) (15,248,12)
    - [ ] **VerticalCheckerboardPlatform** (0x57) ×2: (5,91,13) (11,180,16)
    - [ ] **Sprite 0x59** (0x59) ×3: (12,204,12) (13,214,12) (14,224,7)
    - [ ] **Sprite 0x5A** (0x5A) ×5: (2,44,13) (3,50,10) (13,209,14) (13,219,10) (14,230,7)
    - [ ] **BrownChainedPlatform** (0x5F) ×8: (1,22,10) (3,62,10) (6,111,9) (7,123,13) (12,193,10) (15,254,13) …
    - [ ] **GoalTape** (0x7B) ×1: (19,318,23)
    - [ ] **MessageBox** (0xB9) ×2: (0,15,20) (10,162,20)

### Nivel 0x104 — YOSHI'S HOUSE
- **Nombre (overworld)**: YOSHI'S HOUSE
- **Tipo**: nivel de MAPA (translevel 0x28)
- **Direcciones**: L1ptr 0x2E30C → header 0x3802D · SprPtr 0x2EE08 → stream 0x3E759 · L2ptr 0x2E90C · GFXslot 0x028E7 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=0
- **Colisión**: 15×27 casillas · 
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x1 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Bird** (0x8A) ×4: (0,4,14) (0,5,14) (0,6,14) (0,7,14)
    - [ ] **SideExitAndFireplace** (0x8C) ×1: (0,8,7)
    - [ ] **MessageBox** (0xB9) ×1: (0,8,21)

### Nivel 0x105 — YOSHI'S ISLAND 1
- **Nombre (overworld)**: YOSHI'S ISLAND 1
- **Tipo**: nivel de MAPA (translevel 0x29)
- **Direcciones**: L1ptr 0x2E30F → header 0x308DD · SprPtr 0x2EE0A → stream 0x3C4CA · L2ptr 0x2E90F · GFXslot 0x028E3 · FGBGslot 0x02947
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 15` (tilesetFG=7)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=2
- **Colisión**: 320×27 casillas · LEDGE_TOP=349 SOLID=72 SLOPE=6 SLOPE_STEEP=14
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x4 (memoria 0x4, buoyancy 0x0)
- **Salidas de pantalla**: pant 7→0x1CB
- **Usa sprites grandes**: sí — BanzaiBill (0x9F)
- **Enemigos (34)**:
    - [s] **JumpingPiranhaPlant** (0x4F) ×3: (7,113,21) (8,139,20) (17,284,20)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (2,37,20)
    - [ ] **WarpHole** (0x8E) ×1: (8,131,20)
    - [ ] **ClappinChuck** (0x95) ×1: (18,298,23)
    - [B] **BanzaiBill** (0x9F) ×4: (1,31,19) (12,202,20) (14,226,20) (17,279,20)
    - [ ] **Rex** (0xAB) ×18: (2,33,23) (2,47,23) (3,55,18) (4,72,19) (5,82,23) (5,89,23) …
    - [ ] **MessageBox** (0xB9) ×2: (10,162,20) (12,207,21)
    - [ ] **SlidingNakedBlueKoopa** (0xBD) ×1: (0,13,17)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (6,102,23)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (13,209,23)

### Nivel 0x106 — YOSHI'S ISLAND 2
- **Nombre (overworld)**: YOSHI'S ISLAND 2
- **Tipo**: nivel de MAPA (translevel 0x2A)
- **Direcciones**: L1ptr 0x2E312 → header 0x30A2F · SprPtr 0x2EE0C → stream 0x3C532 · L2ptr 0x2E912 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=373 SOLID=71 SLOPE_STEEP=6
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 15→0x1CA
- **Usa sprites grandes**: sí — CharginChuck (0x91)
- **Enemigos (25)**:
    - [s] **GreenKoopa** (0x0) ×1: (6,106,23)
    - [s] **RedKoopa** (0x1) ×1: (5,84,23)
    - [s] **RedKoopaNoShell** (0x5) ×8: (1,27,20) (1,28,20) (1,29,20) (1,30,20) (1,31,20) (2,32,20) …
    - [s] **PSwitch** (0x3E) ×1: (17,282,17)
    - [s] **Sprite 0x4D** (0x4D) ×2: (14,235,23) (15,247,23)
    - [s] **LedgeMontyMole** (0x4E) ×3: (12,193,19) (12,198,22) (13,214,20)
    - [s] **JumpingPiranhaPlant** (0x4F) ×1: (17,273,21)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [B] **CharginChuck** (0x91) ×2: (8,141,19) (18,293,23)
    - [ ] **MessageBox** (0xB9) ×2: (4,64,21) (9,157,21)
    - [ ] **Sprite 0xDA** (0xDA) ×1: (6,104,23)
    - [ ] **Sprite 0xDB** (0xDB) ×2: (1,17,23) (5,82,23)

### Nivel 0x107 — VANILLA GHOST HOUSE
- **Nombre (overworld)**: VANILLA GHOST HOUSE
- **Tipo**: nivel de MAPA (translevel 0x2B)
- **Direcciones**: L1ptr 0x2E315 → header 0x32D09 · SprPtr 0x2EE0E → stream 0x3CBDC · L2ptr 0x2E915 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 176×27 casillas · SOLID=38 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x0]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 10→0x1EA
- **Usa sprites grandes**: no
- **Enemigos (17)**:
    - [ ] **BouncingFootball** (0x28) ×2: (7,115,18) (8,142,20)
    - [ ] **Sprite 0x37** (0x37) ×9: (3,49,22) (3,61,23) (4,66,12) (4,72,22) (4,79,21) (5,90,16) …
    - [ ] **Sprite 0x38** (0x38) ×4: (1,18,22) (1,18,17) (9,156,4) (10,171,4)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (10,160,20)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,31,20)

### Nivel 0x108 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x2C)
- **Direcciones**: L1ptr 0x2E318 → header 0x380C3 · SprPtr 0x2EE10 → stream 0x3E76D · L2ptr 0x2E918 · GFXslot 0x028C7 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0xA · música 3 · tiempo 200 · Layer2 fondo · paletas BG=3 FG=2 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x109 — VANILLA SECRET 1
- **Nombre (overworld)**: VANILLA SECRET 1
- **Tipo**: nivel de MAPA (translevel 0x2D)
- **Direcciones**: L1ptr 0x2E31B → header 0x33817 · SprPtr 0x2EE12 → stream 0x3CDC8 · L2ptr 0x2E91B · GFXslot 0x028CB · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0xA · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=2 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,25) px (128,400) · pantalla entrada 6 · **VERTICAL** · L2scroll 0 L3 0 L1y 0 L2y 2 · secHdr [0xD 0x1 0x2 0x66]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (22)**:
    - [s] **RedKoopaNoShell** (0x5) ×2: (5,85,30) (6,102,23)
    - [ ] **RedParakoopa** (0x9) ×1: (6,104,18)
    - [ ] **BobOmb** (0xB) ×8: (2,47,24) (2,47,23) (2,47,22) (2,47,19) (2,47,18) (2,47,17) …
    - [ ] **PortableSpringboard** (0x2F) ×2: (2,44,26) (3,54,18)
    - [ ] **Sprite 0x6B** (0x6B) ×5: (1,20,1) (1,28,1) (4,65,1) (4,70,1) (4,75,1)
    - [ ] **RightWallSpringboard** (0x6C) ×4: (1,16,7) (1,24,7) (5,89,15) (5,94,15)

### Nivel 0x10A — VANILLA DOME 3
- **Nombre (overworld)**: VANILLA DOME 3
- **Tipo**: nivel de MAPA (translevel 0x2E)
- **Direcciones**: L1ptr 0x2E31E → header 0x32E7D · SprPtr 0x2EE14 → stream 0x3CC25 · L2ptr 0x2E91E · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 32 pantallas (512 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: 512×27 casillas · LEDGE_TOP=211 SOLID=569 SLOPE=36 SLOPE_STEEP=61
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x58 0x0 0xFA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x1F7 · pant 14→0x1C2
- **Usa sprites grandes**: sí — Blargg (0xA8)
- **Enemigos (49)**:
    - [s] **BlueKoopa** (0x2) ×1: (16,268,23)
    - [s] **BuzzyBeetle** (0x11) ×10: (3,53,15) (11,176,19) (11,186,4) (11,188,4) (13,219,21) (24,399,23) …
    - [s] **GreenShell** (0x2A) ×8: (11,187,20) (11,190,21) (12,196,21) (13,220,19) (28,455,19) (28,459,19) …
    - [ ] **Sprite 0x2E** (0x2E) ×6: (23,376,15) (23,378,23) (23,381,15) (24,384,23) (24,387,15) (24,390,23)
    - [s] **PSwitch** (0x3E) ×1: (30,482,6)
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (2,46,22) (3,50,18)
    - [ ] **SkullRaft** (0x61) ×6: (0,10,24) (3,63,24) (9,145,24) (12,204,24) (17,286,24) (27,447,24)
    - [ ] **GoalTape** (0x7B) ×1: (30,494,23)
    - [ ] **LeftFlyingBlock** (0x83) ×3: (2,32,22) (2,35,18) (4,72,21)
    - [B] **Blargg** (0xA8) ×10: (1,22,25) (2,36,25) (5,95,19) (6,99,19) (6,106,19) (9,154,25) …
    - [ ] **Sprite 0xDB** (0xDB) ×1: (16,267,23)

### Nivel 0x10B — DONUT SECRET 2
- **Nombre (overworld)**: DONUT SECRET 2
- **Tipo**: nivel de MAPA (translevel 0x2F)
- **Direcciones**: L1ptr 0x2E321 → header 0x32461 · SprPtr 0x2EE16 → stream 0x3CA17 · L2ptr 0x2E921 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 240×27 casillas · LEDGE_TOP=228 SOLID=196 SLOPE=13 SLOPE_STEEP=20
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x28 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 9→0x1C6
- **Usa sprites grandes**: no
- **Enemigos (28)**:
    - [ ] **GreenKoopaNoShell** (0x4) ×2: (1,19,17) (11,183,23)
    - [ ] **GreenFlyingParakoopa** (0xA) ×9: (10,165,20) (13,208,16) (13,208,17) (13,208,18) (13,208,19) (13,208,20) …
    - [ ] **BobOmb** (0xB) ×1: (5,88,16)
    - [ ] **Sprite 0x2E** (0x2E) ×12: (3,53,23) (3,58,23) (4,66,16) (6,100,23) (6,110,23) (9,154,14) …
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,34,23)
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (8,128,20) (8,133,21)
    - [ ] **GoalTape** (0x7B) ×1: (13,222,23)

### Nivel 0x10C — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x30)
- **Direcciones**: L1ptr 0x2E324 → header 0x30000 · SprPtr 0x2EE18 → stream 0x3E76D · L2ptr 0x2E924 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x10D — FRONT DOOR
- **Nombre (overworld)**: FRONT DOOR
- **Tipo**: nivel de MAPA (translevel 0x31)
- **Direcciones**: L1ptr 0x2E327 → header 0x3A600 · SprPtr 0x2EE1A → stream 0x3C422 · L2ptr 0x2E927 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=2 FG=3 SPR=1 backArea=7
- **Colisión**: 87×27 casillas · SOLID=64
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1A 0x0 0xA 0x0]
- **Cabecera sprites**: 0xF (memoria 0xF, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x1D4 · pant 2→0x1D3 · pant 3→0x1D2 · pant 5→0x1D1
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)

### Nivel 0x10E — BACK DOOR
- **Nombre (overworld)**: BACK DOOR
- **Tipo**: nivel de MAPA (translevel 0x32)
- **Direcciones**: L1ptr 0x2E32A → header 0x3ABF9 · SprPtr 0x2EE1C → stream 0x3E19D · L2ptr 0x2E92A · GFXslot 0x028F3 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 24 0E` (spriteGfx=12) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x11 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=85 SOLID=67 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0x6A 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 7→0x1C7
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Ninji** (0x51) ×6: (2,43,17) (3,59,23) (4,70,23) (4,73,23) (5,87,23) (5,89,23)
    - [ ] **MechaKoopa** (0xA2) ×2: (4,64,21) (5,83,23)
    - [ ] **Spotlight** (0xC6) ×2: (1,24,0) (6,97,0)
    - [ ] **LightSwitch** (0xC8) ×1: (2,33,19)

### Nivel 0x10F — VALLEY OF BOWSER 4
- **Nombre (overworld)**: VALLEY OF BOWSER 4
- **Tipo**: nivel de MAPA (translevel 0x33)
- **Direcciones**: L1ptr 0x2E32D → header 0x39B58 · SprPtr 0x2EE1E → stream 0x3DF08 · L2ptr 0x2E92D · GFXslot 0x028CB · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 21 pantallas (336 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=2 backArea=3
- **Colisión**: 336×27 casillas · LEDGE_TOP=159 SOLID=255 SLOPE=58 SLOPE_STEEP=74
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x58 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 5→0x1BF
- **Usa sprites grandes**: no
- **Enemigos (46)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×3: (4,68,24) (5,82,24) (8,132,24)
    - [ ] **BobOmb** (0xB) ×3: (1,28,21) (4,77,21) (16,271,20)
    - [ ] **Keyhole** (0xE) ×1: (19,310,20)
    - [ ] **PortableSpringboard** (0x2F) ×1: (18,289,22)
    - [ ] **DigginChuck** (0x46) ×9: (1,18,15) (2,47,18) (9,146,15) (10,173,21) (12,200,16) (13,222,14) …
    - [ ] **DigginChuckRock** (0x48) ×9: (1,16,15) (2,44,18) (8,143,15) (10,171,21) (12,198,16) (13,220,14) …
    - [ ] **GoalTape** (0x7B) ×1: (19,318,23)
    - [ ] **Key** (0x80) ×1: (19,309,23)
    - [ ] **SinkingLavaPlatform** (0xC0) ×18: (1,24,24) (3,63,24) (4,70,24) (4,77,24) (6,109,24) (7,123,24) …

### Nivel 0x110 — #7 LARRY'S CASTLE
- **Nombre (overworld)**: #7 LARRY'S CASTLE
- **Tipo**: nivel de MAPA (translevel 0x34)
- **Direcciones**: L1ptr 0x2E330 → header 0x39DE2 · SprPtr 0x2EE20 → stream 0x3DFB1 · L2ptr 0x2E930 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 126×27 casillas · LEDGE_TOP=29 SOLID=68 SLOPE_STEEP=12
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x1 0x7A 0x0]
- **Cabecera sprites**: 0x8E (memoria 0xE, buoyancy 0x80)
- **Salidas de pantalla**: pant 6→0x1FE · pant 7→0x1FE
- **Usa sprites grandes**: no
- **Enemigos (15)**:
    - [ ] **Sprite 0x74** (0x74) ×1: (7,118,23)
    - [ ] **BallNChain** (0x9E) ×12: (1,31,18) (2,37,11) (2,43,18) (3,49,11) (3,55,18) (3,61,11) …
    - [ ] **CreateEatBlock** (0xB1) ×2: (0,11,24) (1,20,24)

### Nivel 0x111 — VALLEY FORTRESS
- **Nombre (overworld)**: VALLEY FORTRESS
- **Tipo**: nivel de MAPA (translevel 0x35)
- **Direcciones**: L1ptr 0x2E333 → header 0x3A028 · SprPtr 0x2EE22 → stream 0x3E032 · L2ptr 0x2E933 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=7 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x48 0xC0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 14→0x1DE
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (17)**:
    - [ ] **ThrowingDryBones** (0x30) ×2: (4,69,23) (5,82,23)
    - [B] **BonyBeetle** (0x31) ×2: (4,78,23) (10,169,21)
    - [ ] **Sprite 0x32** (0x32) ×2: (9,150,19) (9,159,17)
    - [ ] **Podoboo** (0x33) ×7: (9,144,16) (9,153,14) (10,162,14) (10,164,14) (10,172,15) (12,192,15) …
    - [ ] **FallingSpike** (0xB2) ×3: (4,64,17) (4,78,17) (5,82,17)
    - [ ] **Sprite 0xE9** (0xE9) ×1: (0,8,0 EE1)

### Nivel 0x112
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: nivel de MAPA (translevel 0x36)
- **Direcciones**: L1ptr 0x2E336 → header 0x30000 · SprPtr 0x2EE24 → stream 0x3E76D · L2ptr 0x2E936 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x113 — VALLEY OF BOWSER 3
- **Nombre (overworld)**: VALLEY OF BOWSER 3
- **Tipo**: nivel de MAPA (translevel 0x37)
- **Direcciones**: L1ptr 0x2E339 → header 0x399D6 · SprPtr 0x2EE26 → stream 0x3DE4F · L2ptr 0x2E939 · GFXslot 0x028E3 · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 305×27 casillas · LEDGE_TOP=16 SOLID=89 SLOPE=36 SLOPE_STEEP=36
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0x8A 0x80]
- **Cabecera sprites**: 0x4 (memoria 0x4, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x1BB
- **Usa sprites grandes**: sí — BanzaiBill (0x9F)
- **Enemigos (61)**:
    - [s] **RedKoopaNoShell** (0x5) ×1: (6,108,20)
    - [ ] **RedParakoopa** (0x9) ×9: (0,12,22) (0,13,23) (0,14,22) (1,28,23) (2,38,23) (9,154,21) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×8: (3,49,22) (3,60,23) (4,75,19) (5,84,21) (7,117,20) (11,176,21) …
    - [ ] **BobOmb** (0xB) ×1: (4,70,16)
    - [ ] **PortableSpringboard** (0x2F) ×1: (8,134,23)
    - [ ] **Sprite 0x78** (0x78) ×1: (17,284,15)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [B] **BanzaiBill** (0x9F) ×3: (13,222,20) (15,251,16) (17,274,19)
    - [ ] **CarrotTopLiftUpperRight** (0xB7) ×1: (13,214,13)
    - [ ] **CarrotTopLiftUpperLeft** (0xB8) ×1: (10,173,19)
    - [ ] **TimedPlatform** (0xBA) ×34: (1,23,19) (1,31,19) (2,43,20) (3,51,23) (3,54,18) (3,57,22) …

### Nivel 0x114 — VALLEY GHOST HOUSE
- **Nombre (overworld)**: VALLEY GHOST HOUSE
- **Tipo**: nivel de MAPA (translevel 0x38)
- **Direcciones**: L1ptr 0x2E33C → header 0x39803 · SprPtr 0x2EE28 → stream 0x3DE01 · L2ptr 0x2E93C · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=4 SPR=5 backArea=7
- **Colisión**: 64×27 casillas · SOLID=1 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xA 0x0 0xA 0x0]
- **Cabecera sprites**: 0xD (memoria 0xD, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1DD
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **GreenGasBubble** (0x90) ×4: (1,21,18) (1,29,19) (2,40,19) (3,52,18)

### Nivel 0x115 — VALLEY OF BOWSER 2
- **Nombre (overworld)**: VALLEY OF BOWSER 2
- **Tipo**: nivel de MAPA (translevel 0x39)
- **Direcciones**: L1ptr 0x2E33F → header 0x392CA · SprPtr 0x2EE2A → stream 0x3DD7B · L2ptr 0x2E93F · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=50 SOLID=212 SLOPE_STEEP=3
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x4B 0xC0 0x7A 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 7→0x1E3
- **Usa sprites grandes**: no
- **Enemigos (18)**:
    - [s] **YellowKoopa** (0x3) ×1: (3,60,15)
    - [ ] **GreenKoopaNoShell** (0x4) ×5: (2,34,15) (3,51,22) (5,80,15) (6,98,16) (6,106,23)
    - [ ] **Swooper** (0xBE) ×9: (1,19,7) (1,27,7) (2,36,7) (3,52,7) (4,70,7) (5,83,7) …
    - [ ] **Sprite 0xDD** (0xDD) ×1: (3,58,15)
    - [ ] **Sprite 0xEA** (0xEA) ×2: (0,8,0 EE2) (7,120,0 EE2)

### Nivel 0x116 — VALLEY OF BOWSER 1
- **Nombre (overworld)**: VALLEY OF BOWSER 1
- **Tipo**: nivel de MAPA (translevel 0x3A)
- **Direcciones**: L1ptr 0x2E342 → header 0x38EA4 · SprPtr 0x2EE2C → stream 0x3DD14 · L2ptr 0x2E942 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 18 pantallas (288 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 288×27 casillas · LEDGE_TOP=343 SOLID=519 SLOPE=52 SLOPE_STEEP=124
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 15→0x1E4 · pant 16→0x1E5
- **Usa sprites grandes**: sí — MegaMole (0xBF), CharginChuck (0x91)
- **Enemigos (32)**:
    - [B] **CharginChuck** (0x91) ×12: (1,26,16) (3,58,23) (6,106,19) (6,110,21) (7,115,23) (10,165,23) …
    - [B] **MegaMole** (0xBF) ×20: (1,17,22) (3,53,14) (4,65,18) (4,65,7) (5,83,6) (5,87,11) …

### Nivel 0x117 — CHOCOLATE SECRET
- **Nombre (overworld)**: CHOCOLATE SECRET
- **Tipo**: nivel de MAPA (translevel 0x3B)
- **Direcciones**: L1ptr 0x2E345 → header 0x3705D · SprPtr 0x2EE2E → stream 0x3D9EF · L2ptr 0x2E945 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=76 SOLID=130 SLOPE=32 SLOPE_STEEP=42
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x0 0x7A 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 5→0x1C0 · pant 7→0x1ED
- **Usa sprites grandes**: sí — Blargg (0xA8)
- **Enemigos (11)**:
    - [s] **BuzzyBeetle** (0x11) ×4: (1,28,20) (2,37,18) (3,53,23) (3,56,23)
    - [ ] **PortableSpringboard** (0x2F) ×1: (0,8,23)
    - [ ] **Sprite 0x97** (0x97) ×5: (3,51,17) (3,60,15) (5,83,16) (6,101,21) (7,113,20)
    - [B] **Blargg** (0xA8) ×1: (0,8,25)

### Nivel 0x118 — VANILLA DOME 2
- **Nombre (overworld)**: VANILLA DOME 2
- **Tipo**: nivel de MAPA (translevel 0x3C)
- **Direcciones**: L1ptr 0x2E348 → header 0x3295F · SprPtr 0x2EE30 → stream 0x3CB2A · L2ptr 0x2E948 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 320×27 casillas · LEDGE_TOP=282 SOLID=342 SLOPE=113 SLOPE_STEEP=153
- **Entrada**: casilla (8,6) px (128,96) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 0 L2y 0 · secHdr [0x52 0x1 0xA0 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 13→0x1C3
- **Usa sprites grandes**: sí — CharginChuck (0x91)
- **Enemigos (51)**:
    - [ ] **Keyhole** (0xE) ×1: (5,85,24)
    - [s] **BuzzyBeetle** (0x11) ×14: (2,34,15) (4,73,8) (5,82,3) (5,86,9) (7,125,6) (8,133,7) …
    - [ ] **Sprite 0x15** (0x15) ×11: (1,24,20) (3,60,20) (4,79,24) (5,82,22) (5,85,20) (6,108,19) …
    - [ ] **VerticalCheepCheep** (0x16) ×5: (2,44,23) (4,68,24) (5,94,24) (7,117,21) (7,121,21)
    - [ ] **Sprite 0x2E** (0x2E) ×1: (18,289,20)
    - [s] **PSwitch** (0x3E) ×2: (7,120,6) (12,200,13)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Key** (0x80) ×1: (4,74,16)
    - [ ] **ChangingItem** (0x81) ×1: (11,179,4)
    - [B] **CharginChuck** (0x91) ×5: (9,155,4) (10,168,15) (10,175,5) (13,211,18) (15,247,19)
    - [ ] **Sprite 0x92** (0x92) ×1: (17,283,19)
    - [ ] **Swooper** (0xBE) ×8: (15,245,14) (15,250,14) (15,252,14) (16,259,14) (16,263,14) (16,266,14) …

### Nivel 0x119 — VANILLA DOME 4
- **Nombre (overworld)**: VANILLA DOME 4
- **Tipo**: nivel de MAPA (translevel 0x3D)
- **Direcciones**: L1ptr 0x2E34B → header 0x332D1 · SprPtr 0x2EE32 → stream 0x3CCD4 · L2ptr 0x2E94B · GFXslot 0x028CB · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=1 SPR=2 backArea=4
- **Colisión**: 305×27 casillas · SOLID=32
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · no-Yoshi · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x17 0x0 0x9A 0x80]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x1F5
- **Usa sprites grandes**: sí — GreenParakoopa (0x8)
- **Enemigos (47)**:
    - [ ] **GreenKoopaNoShell** (0x4) ×2: (12,194,15) (16,259,19)
    - [s] **RedKoopaNoShell** (0x5) ×3: (1,31,17) (7,116,23) (15,241,18)
    - [B] **GreenParakoopa** (0x8) ×1: (5,84,21)
    - [ ] **GreenFlyingParakoopa** (0xA) ×4: (2,37,22) (9,153,18) (17,279,22) (18,289,21)
    - [ ] **BobOmb** (0xB) ×2: (8,139,16) (8,139,21)
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (10,168,25) (11,177,24)
    - [ ] **Sprite 0x6B** (0x6B) ×10: (2,36,23) (4,76,24) (5,83,24) (5,91,24) (10,174,23) (13,218,25) …
    - [ ] **RightWallSpringboard** (0x6C) ×15: (1,21,20) (2,41,19) (5,81,24) (5,89,24) (6,97,24) (7,120,20) …
    - [ ] **Sprite 0x74** (0x74) ×1: (2,40,24)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Sprite 0xD5** (0xD5) ×1: (1,17,0)
    - [ ] **Sprite 0xD6** (0xD6) ×2: (10,164,0) (15,249,0)
    - [ ] **Sprite 0xD9** (0xD9) ×3: (9,148,0) (14,234,0) (17,276,0)

### Nivel 0x11A — VANILLA DOME 1
- **Nombre (overworld)**: VANILLA DOME 1
- **Tipo**: nivel de MAPA (translevel 0x3E)
- **Direcciones**: L1ptr 0x2E34E → header 0x32600 · SprPtr 0x2EE34 → stream 0x3CA87 · L2ptr 0x2E94E · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 27 pantallas (432 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 432×27 casillas · LEDGE_TOP=316 SOLID=345 SLOPE=4 SLOPE_STEEP=45
- **Entrada**: casilla (1,14) px (16,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x0 0xFA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 8→0x1EF
- **Usa sprites grandes**: no
- **Enemigos (40)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×2: (7,117,24) (21,337,21)
    - [ ] **Keyhole** (0xE) ×1: (7,116,8)
    - [s] **BuzzyBeetle** (0x11) ×16: (2,34,20) (3,61,23) (4,69,23) (4,74,17) (4,79,23) (5,80,17) …
    - [ ] **Sprite 0x2E** (0x2E) ×6: (1,19,23) (18,292,19) (18,294,19) (18,301,20) (18,303,20) (19,305,20)
    - [s] **JumpingPiranhaPlant** (0x4F) ×1: (20,333,21)
    - [ ] **GoalTape** (0x7B) ×1: (25,414,23)
    - [ ] **Key** (0x80) ×1: (7,113,8)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (2,37,17)
    - [ ] **Sprite 0x97** (0x97) ×1: (25,410,23)
    - [ ] **Swooper** (0xBE) ×5: (1,22,13) (1,26,13) (2,35,13) (2,42,13) (2,45,13)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (5,81,23)
    - [ ] **Sprite 0xDA** (0xDA) ×1: (23,374,23)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (3,57,13)
    - [ ] **Sprite 0xDC** (0xDC) ×2: (3,57,17) (24,388,17)

### Nivel 0x11B — RED SWITCH PALACE
- **Nombre (overworld)**: RED SWITCH PALACE
- **Tipo**: nivel de MAPA (translevel 0x3F)
- **Direcciones**: L1ptr 0x2E351 → header 0x306D0 · SprPtr 0x2EE36 → stream 0x3C450 · L2ptr 0x2E951 · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=14
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x1D8
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [s] **YellowKoopa** (0x3) ×1: (0,9,23)
    - [ ] **GreenKoopaNoShell** (0x4) ×3: (0,13,23) (1,25,23) (2,37,23)
    - [s] **RedKoopaNoShell** (0x5) ×3: (1,17,23) (1,29,23) (2,41,23)
    - [ ] **BlueKoopaNoShell** (0x6) ×2: (1,21,23) (2,33,23)
    - [s] **PSwitch** (0x3E) ×1: (0,4,23)
    - [ ] **Sprite 0xDD** (0xDD) ×1: (0,8,23)

### Nivel 0x11C — #3 LEMMY'S CASTLE
- **Nombre (overworld)**: #3 LEMMY'S CASTLE
- **Tipo**: nivel de MAPA (translevel 0x40)
- **Direcciones**: L1ptr 0x2E354 → header 0x334E0 · SprPtr 0x2EE38 → stream 0x3CD68 · L2ptr 0x2E954 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 160×27 casillas · LEDGE_TOP=25 SOLID=187 SLOPE_STEEP=4
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x17 0x1 0x9A 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 6→0x1F4 · pant 9→0x1F3
- **Usa sprites grandes**: sí — MagiKoopa (0x1F)
- **Enemigos (14)**:
    - [B] **MagiKoopa** (0x1F) ×1: (1,22,21)
    - [ ] **Podoboo** (0x33) ×7: (1,20,19) (2,32,19) (2,43,19) (7,118,17) (8,132,14) (8,138,18) …
    - [s] **PSwitch** (0x3E) ×1: (5,84,14)
    - [ ] **Sprite 0x5B** (0x5B) ×5: (4,72,24) (4,77,24) (5,83,24) (5,89,24) (6,96,24)

### Nivel 0x11D — FOREST GHOST HOUSE
- **Nombre (overworld)**: FOREST GHOST HOUSE
- **Tipo**: nivel de MAPA (translevel 0x41)
- **Direcciones**: L1ptr 0x2E357 → header 0x35ABE · SprPtr 0x2EE3A → stream 0x3D522 · L2ptr 0x2E957 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x1 · música 4 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 170×27 casillas · SOLID=2
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x29 0x0 0xA 0x0]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x1E6 · pant 3→0x1E7 · pant 10→0x1FA
- **Usa sprites grandes**: no
- **Enemigos (24)**:
    - [ ] **BouncingFootball** (0x28) ×2: (1,17,14) (2,43,17)
    - [ ] **Sprite 0x37** (0x37) ×13: (1,25,17) (1,28,24) (3,51,16) (3,60,24) (4,66,18) (4,74,21) …
    - [ ] **Sprite 0x38** (0x38) ×1: (2,37,20)
    - [ ] **Sprite 0x39** (0x39) ×7: (8,128,20) (8,133,18) (8,133,19) (8,133,20) (9,155,16) (10,160,17) …
    - [ ] **Sprite 0xDE** (0xDE) ×1: (6,98,13)

### Nivel 0x11E — FOREST OF??????ON 1
- **Nombre (overworld)**: FOREST OF??????ON 1
- **Tipo**: nivel de MAPA (translevel 0x42)
- **Direcciones**: L1ptr 0x2E35A → header 0x3523A · SprPtr 0x2EE3C → stream 0x3D30C · L2ptr 0x2E95A · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=1
- **Colisión**: 305×27 casillas · SOLID=28
- **Entrada**: casilla (1,23) px (16,368) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1C 0x0 0x9A 0x0]
- **Cabecera sprites**: 0xA (memoria 0xA, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — ParaGoomba (0x10)
- **Enemigos (38)**:
    - [s] **RedKoopaNoShell** (0x5) ×4: (1,30,24) (2,41,24) (2,46,24) (10,162,24)
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (14,228,26)
    - [ ] **Keyhole** (0xE) ×1: (13,223,24)
    - [s] **Goomba** (0xF) ×2: (3,63,22) (4,65,22)
    - [B] **ParaGoomba** (0x10) ×1: (4,69,22)
    - [ ] **PortableSpringboard** (0x2F) ×1: (5,80,22)
    - [ ] **ExplodingBlock** (0x4C) ×9: (11,178,18) (11,181,17) (11,189,18) (11,191,16) (12,193,18) (13,219,15) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **ChangingItem** (0x81) ×1: (9,159,21)
    - [ ] **Wiggler** (0x86) ×15: (1,16,24) (3,58,22) (4,79,22) (6,97,24) (7,118,22) (7,124,22) …
    - [ ] **HammerBro** (0x9B) ×1: (17,280,16)
    - [ ] **HammerBroPlatform** (0x9C) ×1: (17,280,16)

### Nivel 0x11F — FOREST OF??????ON 4
- **Nombre (overworld)**: FOREST OF??????ON 4
- **Tipo**: nivel de MAPA (translevel 0x43)
- **Direcciones**: L1ptr 0x2E35D → header 0x35F5B · SprPtr 0x2EE3E → stream 0x3D577 · L2ptr 0x2E95D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=0 backArea=1
- **Colisión**: 320×27 casillas · LEDGE_TOP=261 SOLID=253 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x0]
- **Cabecera sprites**: 0xC (memoria 0xC, buoyancy 0x0)
- **Salidas de pantalla**: pant 11→0x1DF · pant 13→0x1C1
- **Usa sprites grandes**: sí — CharginChuck (0x91)
- **Enemigos (26)**:
    - [s] **RedKoopa** (0x1) ×2: (2,34,23) (15,248,23)
    - [ ] **RedParakoopa** (0x9) ×2: (1,18,23) (12,206,16)
    - [ ] **Lakitu** (0x1E) ×2: (1,17,13) (13,220,13)
    - [s] **PSwitch** (0x3E) ×1: (11,186,23)
    - [ ] **ShiftingPipe** (0x49) ×1: (7,126,21)
    - [s] **PipeLakitu** (0x4B) ×7: (3,53,21) (5,89,19) (6,104,20) (7,119,21) (8,140,20) (10,166,20) …
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (17,283,21) (18,293,21)
    - [ ] **Sprite 0x74** (0x74) ×1: (2,37,15)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **ChangingItem** (0x81) ×1: (4,70,20)
    - [B] **CharginChuck** (0x91) ×1: (18,290,23)
    - [ ] **Sprite 0xD2** (0xD2) ×2: (9,145,0) (18,294,0)
    - [ ] **Sprite 0xDB** (0xDB) ×2: (2,33,23) (15,247,23)
    - [ ] **Sprite 0xDC** (0xDC) ×1: (14,234,23)

### Nivel 0x120 — FOREST OF??????ON 2
- **Nombre (overworld)**: FOREST OF??????ON 2
- **Tipo**: nivel de MAPA (translevel 0x44)
- **Direcciones**: L1ptr 0x2E360 → header 0x3540B · SprPtr 0x2EE40 → stream 0x3D380 · L2ptr 0x2E960 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 14 pantallas (224 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=7 SPR=3 backArea=5
- **Colisión**: 224×27 casillas · LEDGE_TOP=219 SOLID=225 SLOPE_STEEP=90
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x59 0x38 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (65)**:
    - [ ] **Keyhole** (0xE) ×1: (8,131,22)
    - [ ] **Sprite 0x15** (0x15) ×3: (4,74,15) (4,74,11) (4,74,7)
    - [ ] **Sprite 0x3A** (0x3A) ×4: (1,31,10) (2,33,18) (5,83,12) (5,85,21)
    - [ ] **Sprite 0x3B** (0x3B) ×11: (1,18,14) (1,24,16) (2,42,4) (5,92,3) (5,94,7) (6,106,8) …
    - [ ] **Sprite 0x3C** (0x3C) ×6: (3,52,14) (3,53,7) (7,117,5) (7,118,12) (7,124,4) (9,156,15)
    - [ ] **RipVanFish** (0x3D) ×16: (4,78,22) (6,100,5) (6,104,17) (6,110,23) (8,143,24) (10,160,12) …
    - [ ] **GoalTape** (0x7B) ×1: (12,206,23)
    - [ ] **Key** (0x80) ×1: (8,134,24)
    - [ ] **Sprite 0x94** (0x94) ×1: (11,186,23)
    - [ ] **Blurp** (0xC2) ×21: (1,16,19) (1,21,16) (1,23,19) (2,35,22) (2,40,18) (2,41,19) …

### Nivel 0x121 — BLUE SWITCH PALACE
- **Nombre (overworld)**: BLUE SWITCH PALACE
- **Tipo**: nivel de MAPA (translevel 0x45)
- **Direcciones**: L1ptr 0x2E363 → header 0x3072B · SprPtr 0x2EE42 → stream 0x3C478 · L2ptr 0x2E963 · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=50
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x1D7
- **Usa sprites grandes**: no
- **Enemigos (10)**:
    - [ ] **KoopaKidBossFight** (0x13) ×8: (0,7,14) (0,12,14) (1,17,14) (1,22,14) (1,27,14) (2,32,14) …
    - [s] **PSwitch** (0x3E) ×2: (0,6,23) (0,11,23)

### Nivel 0x122 — FOREST SECRET AREA
- **Nombre (overworld)**: FOREST SECRET AREA
- **Tipo**: nivel de MAPA (translevel 0x46)
- **Direcciones**: L1ptr 0x2E366 → header 0x36183 · SprPtr 0x2EE44 → stream 0x3D5F5 · L2ptr 0x2E966 · GFXslot 0x028CB · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=0 SPR=2 backArea=1
- **Colisión**: 145×27 casillas · SOLID=3
- **Entrada**: casilla (1,12) px (16,192) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 1 L2y 1 · secHdr [0x56 0x0 0x5 0x80]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — GreenParakoopa (0x8)
- **Enemigos (27)**:
    - [B] **GreenParakoopa** (0x8) ×14: (2,38,13) (2,39,13) (2,40,13) (2,47,17) (3,63,21) (4,78,19) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×5: (1,24,19) (1,27,11) (3,55,15) (5,80,9) (6,105,21)
    - [ ] **BobOmb** (0xB) ×1: (4,69,12)
    - [ ] **Sprite 0x78** (0x78) ×3: (9,147,17) (9,148,17) (9,149,17)
    - [ ] **GoalTape** (0x7B) ×1: (8,142,17)
    - [ ] **WingedPlatform** (0xC1) ×2: (0,12,14) (1,17,14)
    - [ ] **Sprite 0xF4** (0xF4) ×1: (0,8,0)

### Nivel 0x123 — FOREST OF??????ON 3
- **Nombre (overworld)**: FOREST OF??????ON 3
- **Tipo**: nivel de MAPA (translevel 0x47)
- **Direcciones**: L1ptr 0x2E369 → header 0x356F3 · SprPtr 0x2EE46 → stream 0x3D445 · L2ptr 0x2E969 · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=0 SPR=0 backArea=0
- **Colisión**: 305×27 casillas · SOLID=176
- **Entrada**: casilla (1,23) px (16,368) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1C 0x0 0x9A 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1BC · pant 17→0x1F8
- **Usa sprites grandes**: no
- **Enemigos (42)**:
    - [ ] **PortableSpringboard** (0x2F) ×2: (3,48,24) (5,95,15)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Sprite 0x92** (0x92) ×1: (17,278,24)
    - [ ] **BubbleWithSprite** (0x9D) ×31: (1,17,22) (1,28,21) (2,41,23) (2,44,21) (3,57,19) (3,61,23) …
    - [ ] **Sprite 0xD4** (0xD4) ×4: (12,206,0) (13,217,0) (17,272,0) (17,283,0)
    - [ ] **Sprite 0xD9** (0xD9) ×2: (11,189,0) (18,300,0)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (5,95,19)

### Nivel 0x124 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x48)
- **Direcciones**: L1ptr 0x2E36C → header 0x30000 · SprPtr 0x2EE48 → stream 0x3E76D · L2ptr 0x2E96C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x125 — FUNKY
- **Nombre (overworld)**: FUNKY
- **Tipo**: nivel de MAPA (translevel 0x49)
- **Direcciones**: L1ptr 0x2E36F → header 0x3BF65 · SprPtr 0x2EE4A → stream 0x3E6F4 · L2ptr 0x2E96F · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 32 pantallas (512 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=5
- **Colisión**: 512×27 casillas · LEDGE_TOP=400 SOLID=325 SLOPE_STEEP=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x7 (memoria 0x7, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — ParaGoomba (0x10)
- **Enemigos (33)**:
    - [s] **RedKoopaNoShell** (0x5) ×3: (4,67,23) (15,254,22) (16,264,23)
    - [ ] **BlueKoopaNoShell** (0x6) ×5: (2,44,23) (3,49,23) (5,89,23) (5,94,23) (13,223,23)
    - [ ] **RedParakoopa** (0x9) ×3: (1,29,23) (19,318,23) (20,330,22)
    - [ ] **BulletBillGenerator** (0xC) ×1: (4,71,23)
    - [B] **ParaGoomba** (0x10) ×2: (17,283,22) (17,285,22)
    - [ ] **Sprite 0x50** (0x50) ×1: (1,16,22)
    - [ ] **GoalTape** (0x7B) ×1: (30,494,23)
    - [ ] **Sprite 0x84** (0x84) ×1: (21,343,15)
    - [ ] **Sprite 0x94** (0x94) ×2: (9,155,23) (15,241,23)
    - [ ] **Sprite 0x98** (0x98) ×5: (9,146,23) (19,307,19) (22,360,23) (22,361,20) (22,362,17)
    - [ ] **SumoBro** (0x9A) ×7: (0,4,17) (2,34,15) (4,76,18) (6,108,14) (7,116,19) (11,187,18) …
    - [ ] **Sprite 0xD9** (0xD9) ×2: (12,199,0) (19,316,0)

### Nivel 0x126 — OUTRAGEOUS
- **Nombre (overworld)**: OUTRAGEOUS
- **Tipo**: nivel de MAPA (translevel 0x4A)
- **Direcciones**: L1ptr 0x2E372 → header 0x3BDE5 · SprPtr 0x2EE4C → stream 0x3E650 · L2ptr 0x2E972 · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=1 SPR=0 backArea=1
- **Colisión**: 305×27 casillas · SOLID=158
- **Entrada**: casilla (1,23) px (16,368) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1C 0x0 0x9A 0x0]
- **Cabecera sprites**: 0xA (memoria 0xA, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (54)**:
    - [ ] **HoppingFlame** (0x1D) ×9: (2,34,24) (2,40,24) (3,48,24) (4,76,24) (6,110,24) (11,186,24) …
    - [ ] **PortableSpringboard** (0x2F) ×2: (3,58,24) (7,112,24)
    - [ ] **ExplodingBlock** (0x4C) ×4: (11,180,18) (11,184,20) (11,185,17) (11,189,19)
    - [s] **JumpingPiranhaPlant** (0x4F) ×1: (6,100,23)
    - [ ] **Sprite 0x50** (0x50) ×5: (12,195,22) (12,204,22) (13,211,21) (15,254,22) (16,263,21)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Wiggler** (0x86) ×10: (1,18,24) (2,45,24) (5,87,24) (7,120,24) (8,130,24) (9,150,24) …
    - [ ] **HammerBro** (0x9B) ×3: (10,162,17) (14,230,17) (17,284,17)
    - [ ] **HammerBroPlatform** (0x9C) ×3: (10,162,17) (14,230,17) (17,284,17)
    - [ ] **Sprite 0xC9** (0xC9) ×16: (3,59,22) (4,68,20) (4,77,21) (5,89,22) (5,91,19) (5,94,23) …

### Nivel 0x127 — MONDO
- **Nombre (overworld)**: MONDO
- **Tipo**: nivel de MAPA (translevel 0x4B)
- **Direcciones**: L1ptr 0x2E375 → header 0x3BC11 · SprPtr 0x2EE4E → stream 0x3E5DF · L2ptr 0x2E975 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 256×27 casillas · LEDGE_TOP=254 SOLID=286 SLOPE_STEEP=28
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 1 L1y 2 L2y 2 · secHdr [0x1B 0x40 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 7→0x1E0 · pant 15→0x1E1
- **Usa sprites grandes**: no
- **Enemigos (37)**:
    - [s] **RedKoopaNoShell** (0x5) ×4: (7,113,19) (7,117,23) (11,189,21) (11,191,21)
    - [ ] **Sprite 0x15** (0x15) ×16: (2,40,18) (3,52,19) (3,59,18) (4,75,21) (4,78,17) (5,95,21) …
    - [ ] **VerticalCheepCheep** (0x16) ×9: (1,19,23) (1,25,23) (4,68,22) (5,81,23) (6,101,22) (9,155,21) …
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (2,44,20) (8,130,22)
    - [ ] **HammerBro** (0x9B) ×3: (1,24,16) (4,71,16) (14,239,15)
    - [ ] **HammerBroPlatform** (0x9C) ×3: (1,24,16) (4,71,16) (14,239,15)

### Nivel 0x128 — GROOVY
- **Nombre (overworld)**: GROOVY
- **Tipo**: nivel de MAPA (translevel 0x4C)
- **Direcciones**: L1ptr 0x2E378 → header 0x3BABE · SprPtr 0x2EE50 → stream 0x3E574 · L2ptr 0x2E978 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=296 SOLID=220 SLOPE=14 SLOPE_STEEP=24
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1A 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — Pokey (0x70)
- **Enemigos (35)**:
    - [ ] **GreenKoopaNoShell** (0x4) ×1: (4,67,23)
    - [s] **RedKoopaNoShell** (0x5) ×2: (0,11,22) (4,70,23)
    - [ ] **BlueKoopaNoShell** (0x6) ×1: (4,73,23)
    - [ ] **YellowKoopaNoShell** (0x7) ×1: (4,76,23)
    - [s] **JumpingPiranhaPlant** (0x4F) ×3: (6,107,23) (8,134,20) (15,252,16)
    - [ ] **Sprite 0x50** (0x50) ×1: (13,220,24)
    - [B] **Pokey** (0x70) ×15: (6,101,19) (9,152,19) (10,165,19) (10,173,18) (11,180,19) (11,187,18) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **ChangingItem** (0x81) ×1: (8,131,17)
    - [ ] **Sprite 0x98** (0x98) ×3: (16,269,17) (18,291,20) (18,295,18)
    - [ ] **VolcanoLotus** (0x99) ×5: (7,120,14) (11,178,14) (12,196,14) (12,196,22) (17,282,23)
    - [ ] **SlidingNakedBlueKoopa** (0xBD) ×1: (3,55,17)

### Nivel 0x129 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x4D)
- **Direcciones**: L1ptr 0x2E37B → header 0x30000 · SprPtr 0x2EE52 → stream 0x3E76D · L2ptr 0x2E97B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x12A — GNARLY
- **Nombre (overworld)**: GNARLY
- **Tipo**: nivel de MAPA (translevel 0x4E)
- **Direcciones**: L1ptr 0x2E37E → header 0x3B26B · SprPtr 0x2EE54 → stream 0x3E3DC · L2ptr 0x2E97E · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xA · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=6
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,11) px (128,176) · pantalla entrada 4 · **VERTICAL** · no-Yoshi · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0x5 0x1 0xA 0xE4]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **BlueKoopaNoShell** (0x6) ×1: (4,76,27)
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (0,10,25)
    - [ ] **BobOmb** (0xB) ×1: (1,30,10)
    - [ ] **Sprite 0x64** (0x64) ×1: (3,61,13)
    - [ ] **Sprite 0x6B** (0x6B) ×2: (2,35,7) (4,68,9)
    - [ ] **RightWallSpringboard** (0x6C) ×4: (0,8,8) (2,35,14) (2,47,18) (4,72,6)
    - [ ] **Sprite 0x78** (0x78) ×1: (0,3,2)
    - [ ] **MessageBox** (0xB9) ×1: (4,72,11)

### Nivel 0x12B — TUBULAR
- **Nombre (overworld)**: TUBULAR
- **Tipo**: nivel de MAPA (translevel 0x4F)
- **Direcciones**: L1ptr 0x2E381 → header 0x3B46E · SprPtr 0x2EE56 → stream 0x3E428 · L2ptr 0x2E981 · GFXslot 0x028FB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 0E` (spriteGfx=14) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=0 SPR=0 backArea=2
- **Colisión**: 176×27 casillas · LEDGE_TOP=21 SOLID=180
- **Entrada**: casilla (8,17) px (128,272) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x58 0x1 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (20)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×3: (5,84,24) (5,84,19) (5,84,14)
    - [ ] **BobOmb** (0xB) ×2: (6,102,23) (6,104,21)
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,46,24)
    - [s] **PSwitch** (0x3E) ×1: (2,46,17)
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (1,24,23) (6,108,20)
    - [ ] **GoalTape** (0x7B) ×1: (9,158,23)
    - [ ] **ClappinChuck** (0x95) ×2: (1,16,20) (2,33,21)
    - [ ] **Sprite 0x97** (0x97) ×2: (7,118,19) (7,126,13)
    - [ ] **Sprite 0x98** (0x98) ×2: (3,63,22) (4,69,19)
    - [ ] **VolcanoLotus** (0x99) ×4: (5,92,20) (5,92,14) (8,133,15) (9,146,19)

### Nivel 0x12C — WAY COOL HOUSE ISLAND SWITCH PALACE CASTLE PLAINS GHOST HOUSE SECRET DOME FORTRESS OF??????ON OF BOWSER ROAD WORLD AWESOME 12345PALACEAREAGROOVYMONDOOUTRAGEOUSFUNKYHOUSE
- **Nombre (overworld)**: WAY COOL HOUSE ISLAND SWITCH PALACE CASTLE PLAINS GHOST HOUSE SECRET DOME FORTRESS OF??????ON OF BOWSER ROAD WORLD AWESOME 12345PALACEAREAGROOVYMONDOOUTRAGEOUSFUNKYHOUSE
- **Tipo**: nivel de MAPA (translevel 0x50)
- **Direcciones**: L1ptr 0x2E384 → header 0x3B540 · SprPtr 0x2EE58 → stream 0x3E466 · L2ptr 0x2E984 · GFXslot 0x028CB · FGBGslot 0x02933
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 16` (tilesetFG=2)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=1 SPR=2 backArea=1
- **Colisión**: 305×27 casillas · SOLID=31
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x80]
- **Cabecera sprites**: 0x1 (memoria 0x1, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x1C9
- **Usa sprites grandes**: no
- **Enemigos (44)**:
    - [ ] **Sprite 0x62** (0x62) ×2: (12,205,19) (14,231,19)
    - [ ] **Sprite 0x63** (0x63) ×1: (1,18,22)
    - [ ] **Sprite 0x64** (0x64) ×4: (11,184,15) (12,192,17) (13,208,17) (15,246,15)
    - [ ] **Sprite 0x65** (0x65) ×2: (2,43,25) (6,111,16)
    - [ ] **Sprite 0x68** (0x68) ×32: (1,29,21) (2,33,15) (2,35,7) (2,39,16) (3,59,11) (3,59,19) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **GreyFallingPlatform** (0xC4) ×2: (10,168,22) (10,172,20)

### Nivel 0x12D — AWESOME 12345PALACEAREAGROOVYMONDOOUTRAGEOUSFUNKYHOUSE
- **Nombre (overworld)**: AWESOME 12345PALACEAREAGROOVYMONDOOUTRAGEOUSFUNKYHOUSE
- **Tipo**: nivel de MAPA (translevel 0x51)
- **Direcciones**: L1ptr 0x2E387 → header 0x3B908 · SprPtr 0x2EE5A → stream 0x3E4F1 · L2ptr 0x2E987 · GFXslot 0x028E3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=0 backArea=7
- **Colisión**: 320×27 casillas · LEDGE_TOP=152 SOLID=191 SLOPE=70 SLOPE_STEEP=77
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x28 0xA 0x0]
- **Cabecera sprites**: 0x84 (memoria 0x4, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — BanzaiBill (0x9F)
- **Enemigos (43)**:
    - [s] **BlueKoopa** (0x2) ×6: (2,45,17) (4,64,17) (6,104,17) (7,127,16) (8,142,20) (10,165,21)
    - [s] **YellowKoopa** (0x3) ×2: (0,15,20) (5,82,17)
    - [ ] **RedParakoopa** (0x9) ×1: (9,149,20)
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (16,270,23)
    - [ ] **BulletBillGenerator** (0xC) ×1: (9,146,20)
    - [ ] **Sprite 0x15** (0x15) ×1: (7,116,24)
    - [s] **PSwitch** (0x3E) ×1: (6,110,21)
    - [ ] **Sprite 0x59** (0x59) ×1: (1,22,18)
    - [ ] **Sprite 0x75** (0x75) ×1: (11,185,22)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [B] **BanzaiBill** (0x9F) ×4: (12,197,17) (14,224,17) (15,241,19) (16,263,19)
    - [ ] **Rex** (0xAB) ×13: (2,35,22) (3,52,22) (3,55,22) (5,89,21) (8,130,18) (8,132,19) …
    - [ ] **Sprite 0xD1** (0xD1) ×1: (11,184,0)
    - [ ] **Sprite 0xD9** (0xD9) ×1: (17,276,0)
    - [ ] **Sprite 0xDA** (0xDA) ×2: (6,103,17) (8,141,20)
    - [ ] **Sprite 0xDB** (0xDB) ×2: (2,44,17) (7,126,16)
    - [ ] **Sprite 0xDC** (0xDC) ×1: (3,63,17)
    - [ ] **Sprite 0xDD** (0xDD) ×3: (0,13,20) (5,81,17) (10,164,21)

### Nivel 0x12E — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x52)
- **Direcciones**: L1ptr 0x2E38A → header 0x30000 · SprPtr 0x2EE5C → stream 0x3E76D · L2ptr 0x2E98A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x12F — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x53)
- **Direcciones**: L1ptr 0x2E38D → header 0x30000 · SprPtr 0x2EE5E → stream 0x3E76D · L2ptr 0x2E98D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x130 — STAR WORLD 2
- **Nombre (overworld)**: STAR WORLD 2
- **Tipo**: nivel de MAPA (translevel 0x54)
- **Direcciones**: L1ptr 0x2E390 → header 0x3AF25 · SprPtr 0x2EE60 → stream 0x3E221 · L2ptr 0x2E990 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=6 SPR=3 backArea=2
- **Colisión**: 192×27 casillas · LEDGE_TOP=152 SOLID=97 SLOPE_STEEP=8
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 3 L1y 2 L2y 2 · secHdr [0x58 0xF8 0xA 0x80]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 9→0x1D5
- **Usa sprites grandes**: no
- **Enemigos (41)**:
    - [ ] **Keyhole** (0xE) ×1: (11,185,23)
    - [s] **YoshiEgg** (0x2C) ×1: (0,9,23)
    - [ ] **RipVanFish** (0x3D) ×17: (1,26,20) (1,26,16) (1,31,23) (1,31,18) (2,37,14) (2,37,19) …
    - [ ] **Star** (0x76) ×1: (0,4,10)
    - [ ] **Key** (0x80) ×1: (11,189,23)
    - [ ] **Blurp** (0xC2) ×20: (0,15,20) (1,18,23) (1,18,17) (3,49,20) (3,51,22) (3,51,18) …

### Nivel 0x131 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x55)
- **Direcciones**: L1ptr 0x2E393 → header 0x30000 · SprPtr 0x2EE62 → stream 0x3E76D · L2ptr 0x2E993 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x132 — STAR WORLD 3
- **Nombre (overworld)**: STAR WORLD 3
- **Tipo**: nivel de MAPA (translevel 0x56)
- **Direcciones**: L1ptr 0x2E396 → header 0x3AFE3 · SprPtr 0x2EE64 → stream 0x3E29E · L2ptr 0x2E996 · GFXslot 0x028C3 · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 2 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=5
- **Colisión**: 33×27 casillas · SOLID=109
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x80]
- **Cabecera sprites**: 0xC (memoria 0xC, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **Keyhole** (0xE) ×1: (1,23,5)
    - [ ] **Lakitu** (0x1E) ×1: (0,10,14)
    - [s] **YoshiEgg** (0x2C) ×1: (0,6,23)
    - [s] **PSwitch** (0x3E) ×1: (0,7,23)
    - [ ] **GoalTape** (0x7B) ×1: (1,30,23)

### Nivel 0x133 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x57)
- **Direcciones**: L1ptr 0x2E399 → header 0x30000 · SprPtr 0x2EE66 → stream 0x3E76D · L2ptr 0x2E999 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x134 — STAR WORLD 1
- **Nombre (overworld)**: STAR WORLD 1
- **Tipo**: nivel de MAPA (translevel 0x58)
- **Direcciones**: L1ptr 0x2E39C → header 0x3AD35 · SprPtr 0x2EE68 → stream 0x3E1C5 · L2ptr 0x2E99C · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0xA · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,3) px (128,48) · pantalla entrada 0 · **VERTICAL** · no-Yoshi · L2scroll 0 L3 0 L1y 0 L2y 3 · secHdr [0x1 0x1 0x3 0xE0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (30)**:
    - [ ] **GreenKoopaNoShell** (0x4) ×1: (5,93,24)
    - [s] **RedKoopaNoShell** (0x5) ×2: (5,93,20) (5,93,9)
    - [ ] **BlueKoopaNoShell** (0x6) ×3: (6,97,25) (6,97,17) (6,97,6)
    - [ ] **YellowKoopaNoShell** (0x7) ×3: (5,93,4) (9,153,20) (9,156,28)
    - [ ] **RedParakoopa** (0x9) ×6: (8,134,18) (8,140,27) (8,140,10) (9,145,23) (9,150,27) (9,150,10)
    - [ ] **GreenFlyingParakoopa** (0xA) ×5: (7,112,19) (7,113,19) (7,114,19) (7,115,19) (7,116,19)
    - [ ] **Keyhole** (0xE) ×1: (2,45,27)
    - [s] **YoshiEgg** (0x2C) ×1: (8,140,12)
    - [ ] **Sprite 0x74** (0x74) ×1: (0,4,13)
    - [ ] **Sprite 0x75** (0x75) ×1: (3,50,21)
    - [ ] **Star** (0x76) ×3: (5,89,23) (5,89,6) (8,134,5)
    - [ ] **Feather** (0x77) ×1: (2,47,3)
    - [ ] **Sprite 0x78** (0x78) ×1: (4,75,25)
    - [ ] **Key** (0x80) ×1: (2,45,29)

### Nivel 0x135 — STAR WORLD 4
- **Nombre (overworld)**: STAR WORLD 4
- **Tipo**: nivel de MAPA (translevel 0x59)
- **Direcciones**: L1ptr 0x2E39F → header 0x3B031 · SprPtr 0x2EE6A → stream 0x3E2AF · L2ptr 0x2E99F · GFXslot 0x028CB · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=1 SPR=2 backArea=5
- **Colisión**: 305×27 casillas · SOLID=74
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0x9A 0x80]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (44)**:
    - [s] **BlueKoopa** (0x2) ×1: (12,205,15)
    - [s] **RedKoopaNoShell** (0x5) ×6: (2,35,10) (2,41,13) (4,73,10) (6,99,10) (9,153,20) (14,235,12)
    - [ ] **BlueKoopaNoShell** (0x6) ×6: (3,57,11) (3,60,11) (4,73,18) (4,73,14) (7,125,17) (15,252,9)
    - [ ] **RedParakoopa** (0x9) ×2: (13,216,15) (13,218,15)
    - [ ] **GreenFlyingParakoopa** (0xA) ×13: (4,78,4) (4,78,6) (4,78,8) (4,78,10) (4,78,12) (4,78,14) …
    - [ ] **BobOmb** (0xB) ×2: (6,105,10) (15,241,9)
    - [ ] **Keyhole** (0xE) ×1: (13,216,22)
    - [s] **YoshiEgg** (0x2C) ×1: (1,16,19)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **GreyChainedPlatform** (0xA3) ×2: (16,270,15) (18,292,15)
    - [ ] **Sprite 0xDA** (0xDA) ×1: (12,204,15)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (12,199,15)
    - [ ] **Sprite 0xE0** (0xE0) ×7: (1,23,15) (3,48,12) (6,107,11) (8,132,20) (11,191,11) (15,243,10) …

### Nivel 0x136 — STAR WORLD 5
- **Nombre (overworld)**: STAR WORLD 5
- **Tipo**: nivel de MAPA (translevel 0x5A)
- **Direcciones**: L1ptr 0x2E3A2 → header 0x3B124 · SprPtr 0x2EE6C → stream 0x3E335 · L2ptr 0x2E9A2 · GFXslot 0x028C3 · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 17 pantallas (272 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=5
- **Colisión**: 257×27 casillas · SOLID=336
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · no-Yoshi · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x0 0xA 0x80]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — GreenParakoopa (0x8)
- **Enemigos (55)**:
    - [s] **RedKoopaNoShell** (0x5) ×2: (14,227,23) (14,236,17)
    - [B] **GreenParakoopa** (0x8) ×9: (1,16,20) (1,30,17) (2,44,17) (6,97,17) (6,106,23) (7,114,17) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×7: (2,45,21) (7,115,23) (7,117,23) (7,119,23) (9,145,21) (9,149,23) …
    - [ ] **BobOmb** (0xB) ×3: (3,55,22) (3,63,19) (4,70,21)
    - [ ] **Keyhole** (0xE) ×1: (14,227,4)
    - [ ] **KoopaKidBossFight** (0x13) ×8: (10,165,23) (10,172,23) (11,179,23) (14,230,23) (14,232,23) (15,240,19) …
    - [s] **YoshiEgg** (0x2C) ×1: (9,158,23)
    - [s] **PSwitch** (0x3E) ×1: (4,76,19)
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (10,167,21) (10,174,20)
    - [ ] **GoalTape** (0x7B) ×1: (15,254,23)
    - [ ] **Key** (0x80) ×1: (13,221,4)
    - [ ] **GreyFallingPlatform** (0xC4) ×19: (0,8,24) (0,13,24) (1,18,24) (1,23,21) (1,28,21) (2,33,21) …

### Nivel 0x137 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x5B)
- **Direcciones**: L1ptr 0x2E3A5 → header 0x30000 · SprPtr 0x2EE6E → stream 0x3E76D · L2ptr 0x2E9A5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x138 — STAR ROAD
- **Nombre (overworld)**: STAR ROAD
- **Tipo**: nivel de MAPA (translevel 0x5C)
- **Direcciones**: L1ptr 0x2E3A8 → header 0x30000 · SprPtr 0x2EE70 → stream 0x3E76D · L2ptr 0x2E9A8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x139 — !?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?B
- **Nombre (overworld)**: !?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?B
- **Tipo**: nivel de MAPA (translevel 0x5D)
- **Direcciones**: L1ptr 0x2E3AB → header 0x30000 · SprPtr 0x2EE72 → stream 0x3E76D · L2ptr 0x2E9AB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13A — !?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?B
- **Nombre (overworld)**: !?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?B
- **Tipo**: nivel de MAPA (translevel 0x5E)
- **Direcciones**: L1ptr 0x2E3AE → header 0x30000 · SprPtr 0x2EE74 → stream 0x3E76D · L2ptr 0x2E9AE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13B — !?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?B
- **Nombre (overworld)**: !?R??3 ?????P???C?B?C?ARB?CR??B?L??R??? 4?0??B?C?0?C?B?H?????R??B?L??R?43 ? ?0?BKC?RAC?B?H???12R??B?C??RB?G 43 ???BKCR??C?B?HR?????R?B?L???1? ? 2??0B?CR??C?B?RR????????P?12?12? B?H???P???RB?B?I?R?????P?B?E?B? BKH???????RB?B?B?J??????P???C?C?? BKG?O????RC?B?B?I???RP????D?C?? B?B?D????D?B6B?A?B6A?B?D12?'B5D'?12B?B??C6A?D?B6B?A?B6A?B'A8B?D8B?A8B'B??C6A?D?B6C?R?B6A?B'B8?E?C??8B'B??C6B?RC?B6J?R??6A????B?B?B?F?????AB6C??RC?B6C?BR?B?M6B?B?ARE?B6E?B?M6B?H?C6B?A?F?E?6?6?E6C?6?B6B?C6B?B?B6?F?C?6?B6AAD?C???B?B6A?B6B?B?C?ARD?GRA?6??RF?CR??B?C6B?B?D????B?H?????6??BKD?EO?R??C6B?C6ARC?B6C?E??6??BKD?B?DOR??B6B?A?BRA?B?B6B?B??B?A?H?EOKPRPB6B?F???R??B6D??RAB?B??D?ARC?B?B? B6B?F?????RB?BREA??R?E6D??R?BKB??B6B?E????RC?B??B?CR??D6E?6??RBKB?RD?E???RAD6C?C???E6D?6??B?F?B?CR??E6F?A?R??C6E?6?6?B?F?D????B?A?W?B?D?B?BRKV?B?BKB?B?B??V?B?BKB?D????O?AKBDBCBB?B?B?B?DR???O?A?B?B?RB?B?B?B?A?C?O?E?????B?B?C?BR?C?A?B?B??H?B?DAR??C?B?B?BR?B?G6??????I?E????RC?B?B?B?6B?F6?????B?B?B??B?C?R?B?B6?C?B?B?B?6B?D6???B?B??B?A?C?CR?6B?E6B?D??R?B?B6?BKD?B'B??B'C???B?E6B?D????B?B6?BKD?B5B??B5A?BRB??E6B?H??RA6???B?D????B'B?B'FRQ?Q??D6B?B?F????R?B?A?E?A?D?FRDR???C6B?B6B?A?C?BKD?B'B?BKB?BKB?AOC?B?H6?6??R??BKD?B5B?BKB?BKB?B?BO?B?H6?6?6?R?C?A?B?B'A?D?A?B?B?D?O??B?B6B?A?B?B?RB?C?D????B?B?B?A?B?C?B?I6?6??R?6?BRA?B?A?C?BRB?BRC?R?C?B?B6B?ER?R?RB?ARB?ARK?ARC?B?H6?6?R??RV?B???B6??B6??B6??B6??B6C?A?B?B?B?C?B?A?B?A?B?A?B?A?E?B?B?I?????????C?K???????????I?A?C?E?????C?A?C?G???????J?A?B?D????C?B??C?B??C?B??K?B??B?B?A?C?A?C?G???????I?E?????C?A?C?K???????????G?B?CA6?D6G??R?CBRD?D????BKF?C6B?C6G???????F?CR??BKF?B?C???C?HR?????ARF?B?RB?H?ER?R??B?B??B?C??RF?B??B?I?GR??????BKC??RF?CQ??B?J?FR?????BKB??E?EQDR?'B5A'G?A?B?B??B?A?B?C?RQD?A?B?B?'B5A'F?F????R?BRA?B?A?B?AQB?A?C?DR??PH?A?F?ARC?BRA?BDARB?B?B????B?AOH?AHBIAXBGBFBGCF?FBGAFBJAHBIBHFBGAFBJB?B?D???VBWBVBWCV?VBWAVBZAFBGBFVBWAVBZB?B?AFBGAXBIBHBICH?HBIAHBJAVBWBVHBIAXBGBF?B?AVBWBV?B?A?BJF?BZAHBIEH???VBWBV?B?AHBIBHFBGAFBZB?DJB'BJB?AFBGAXBIBX?B?AFBGBFVBWAVD?DZB'BZBJAVBWAVB?B?B?AVBWBVHBIAHB?DJC???C?BZAHBIBHFBGA?B?AHBIBHFBGAFB?DZAFBGAFDJAFBGBFVBWA?B?AFBGBFVBWAVB?DJAVBWAVDZAVBWBVHBIA?B?AVBWBVHBIAHB?DZAHBIBH?C?AHBIAHD?B?AHBIAXBGBF?BJB?DJA?BJA?B?FJB?B?A?B?AVBWBV?BZB?DZA?BZA?B?FZB?B5AFBGAXBIAHC?BJC?BJA?BJB?BJAFBGAFB?B?AVBWAVBJD?BZA?BJBZA?BZB?BZAVBWAVB?B?AHBIAHBZDJC???BZA?BJA?BJAFBGAXBIAHB?B?BJAFBGAFDZAFBGAFB?BZA?BZAVBWBV?BJB?B?BZAVBWAVDJAVBWAVDJAFBGAXBIBH?BZB?B?B?AHBIAHDZAHBIAHDZAVBWAVBJAFBGAFB?B?A?BJB??DJA?BJAFBGAFB?AHBIAHBZAVBWAVB?B?A?BZB??DZA?BZAVBWAVBJAFBGAFBJAHBIAHB?B?B?FBGDF??FBGCF?HBIAHBZAVBWAVBZF?B?D?D?6??B?D??6?P?B?C?A?B?A?C?C?B?A?B?F??????C?C?B???B6F?B?D?B?D?B?D?B?D?B?F?B?D?B?D?B?D?B???B'B8B'D?B?D?B?D?B?H?B'B8B'D?B?D?B?D?B???D?A?I?BQB?B?J?B?B?B??G?C??QBRBQ?B?C?A?F?B?D????C?B?C?BQRB'BRQL?B?D????C?B?C?I??'5?RQ??D?B?C?B?K?AQC?B9B??F?B?C?B?C?AQGDE?B9ERDEDECDAQC?B?B?B??B'D8B'E?C95'D?B'A?C?B?B?B??B'D8B'F?STUV?B'D?C5'?C?B?C?AAH?F??????E?B9BRAC?B?E?????F?H????????C?B9CR??C?B?C?C?6AB?B'Q??????????'59RA6?C?B?C565B?E6??5'B?F????#?B?B'C??6B?C5B?C?F?6???5E?B'G?D??6?C?B?B?E??6ARB9E?D'5??E?DRA6?C?B?F?????RB9F?BRA?B?A?D?CR??C?B?E???ARB9F?FRA??R?B?A?D?AAC?B?B?DQR'5E?JR??6?6?R??B?A?B'B?QB?B?B?B??B'B?ARB?AAC6A?B6BAYB?F??5'??B?B?C???C?CR??C6A?B6I?6?6????RC?AAB?B?D????B?C?6?C6C?6?C6A?D6AAB?B??B?B?B?C?6?B6A?D6A?B6C?6?E6E?6?6?B?B?D????B?A?W?B???D?Y????D?Y????EQ?Y??U?BQB?CQD?YU?Q?B?EQA?CQD?Q?QQ?A?BQL?J?CQB?B?A?GQA?CQA?B?J?DQA?B?A?BQEQA?CQC??YJ?A?BQB?B?BY?EQFQB?I?B??C?DYU?YB?C?A?CQE?I?B?B?CUB?YB?B?DY??YEQA?J?A?CUC?AYB?C?B?B??C?BYUG?BQH?B?YDYB?AYD?BYUG?BQB?FQA?FYBYA?C?BYUE?EQB?GQ?Q?Q?YD?BYBY?CYB?YE?B
- **Tipo**: nivel de MAPA (translevel 0x5F)
- **Direcciones**: L1ptr 0x2E3B1 → header 0x30000 · SprPtr 0x2EE76 → stream 0x3E76D · L2ptr 0x2E9B1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3B4 → header 0x30000 · SprPtr 0x2EE78 → stream 0x3E76D · L2ptr 0x2E9B4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3B7 → header 0x30000 · SprPtr 0x2EE7A → stream 0x3E76D · L2ptr 0x2E9B7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3BA → header 0x30000 · SprPtr 0x2EE7C → stream 0x3E76D · L2ptr 0x2E9BA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3BD → header 0x30000 · SprPtr 0x2EE7E → stream 0x3E76D · L2ptr 0x2E9BD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x140
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3C0 → header 0x30000 · SprPtr 0x2EE80 → stream 0x3E76D · L2ptr 0x2E9C0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x141
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3C3 → header 0x30000 · SprPtr 0x2EE82 → stream 0x3E76D · L2ptr 0x2E9C3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x142
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3C6 → header 0x30000 · SprPtr 0x2EE84 → stream 0x3E76D · L2ptr 0x2E9C6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x143
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3C9 → header 0x30000 · SprPtr 0x2EE86 → stream 0x3E76D · L2ptr 0x2E9C9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x144
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3CC → header 0x30000 · SprPtr 0x2EE88 → stream 0x3E76D · L2ptr 0x2E9CC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x145
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3CF → header 0x30000 · SprPtr 0x2EE8A → stream 0x3E76D · L2ptr 0x2E9CF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x146
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3D2 → header 0x30000 · SprPtr 0x2EE8C → stream 0x3E76D · L2ptr 0x2E9D2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x147
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3D5 → header 0x30000 · SprPtr 0x2EE8E → stream 0x3E76D · L2ptr 0x2E9D5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x148
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3D8 → header 0x30000 · SprPtr 0x2EE90 → stream 0x3E76D · L2ptr 0x2E9D8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x149
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3DB → header 0x30000 · SprPtr 0x2EE92 → stream 0x3E76D · L2ptr 0x2E9DB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3DE → header 0x30000 · SprPtr 0x2EE94 → stream 0x3E76D · L2ptr 0x2E9DE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3E1 → header 0x30000 · SprPtr 0x2EE96 → stream 0x3E76D · L2ptr 0x2E9E1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3E4 → header 0x30000 · SprPtr 0x2EE98 → stream 0x3E76D · L2ptr 0x2E9E4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3E7 → header 0x30000 · SprPtr 0x2EE9A → stream 0x3E76D · L2ptr 0x2E9E7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3EA → header 0x30000 · SprPtr 0x2EE9C → stream 0x3E76D · L2ptr 0x2E9EA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3ED → header 0x30000 · SprPtr 0x2EE9E → stream 0x3E76D · L2ptr 0x2E9ED · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x150
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3F0 → header 0x30000 · SprPtr 0x2EEA0 → stream 0x3E76D · L2ptr 0x2E9F0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x151
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3F3 → header 0x30000 · SprPtr 0x2EEA2 → stream 0x3E76D · L2ptr 0x2E9F3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x152
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3F6 → header 0x30000 · SprPtr 0x2EEA4 → stream 0x3E76D · L2ptr 0x2E9F6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x153
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3F9 → header 0x30000 · SprPtr 0x2EEA6 → stream 0x3E76D · L2ptr 0x2E9F9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x154
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3FC → header 0x30000 · SprPtr 0x2EEA8 → stream 0x3E76D · L2ptr 0x2E9FC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x155
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E3FF → header 0x30000 · SprPtr 0x2EEAA → stream 0x3E76D · L2ptr 0x2E9FF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x156
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E402 → header 0x30000 · SprPtr 0x2EEAC → stream 0x3E76D · L2ptr 0x2EA02 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x157
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E405 → header 0x30000 · SprPtr 0x2EEAE → stream 0x3E76D · L2ptr 0x2EA05 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x158
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E408 → header 0x30000 · SprPtr 0x2EEB0 → stream 0x3E76D · L2ptr 0x2EA08 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x159
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E40B → header 0x30000 · SprPtr 0x2EEB2 → stream 0x3E76D · L2ptr 0x2EA0B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E40E → header 0x30000 · SprPtr 0x2EEB4 → stream 0x3E76D · L2ptr 0x2EA0E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E411 → header 0x30000 · SprPtr 0x2EEB6 → stream 0x3E76D · L2ptr 0x2EA11 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E414 → header 0x30000 · SprPtr 0x2EEB8 → stream 0x3E76D · L2ptr 0x2EA14 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E417 → header 0x30000 · SprPtr 0x2EEBA → stream 0x3E76D · L2ptr 0x2EA17 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E41A → header 0x30000 · SprPtr 0x2EEBC → stream 0x3E76D · L2ptr 0x2EA1A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E41D → header 0x30000 · SprPtr 0x2EEBE → stream 0x3E76D · L2ptr 0x2EA1D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x160
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E420 → header 0x30000 · SprPtr 0x2EEC0 → stream 0x3E76D · L2ptr 0x2EA20 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x161
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E423 → header 0x30000 · SprPtr 0x2EEC2 → stream 0x3E76D · L2ptr 0x2EA23 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x162
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E426 → header 0x30000 · SprPtr 0x2EEC4 → stream 0x3E76D · L2ptr 0x2EA26 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x163
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E429 → header 0x30000 · SprPtr 0x2EEC6 → stream 0x3E76D · L2ptr 0x2EA29 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x164
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E42C → header 0x30000 · SprPtr 0x2EEC8 → stream 0x3E76D · L2ptr 0x2EA2C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x165
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E42F → header 0x30000 · SprPtr 0x2EECA → stream 0x3E76D · L2ptr 0x2EA2F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x166
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E432 → header 0x30000 · SprPtr 0x2EECC → stream 0x3E76D · L2ptr 0x2EA32 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x167
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E435 → header 0x30000 · SprPtr 0x2EECE → stream 0x3E76D · L2ptr 0x2EA35 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x168
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E438 → header 0x30000 · SprPtr 0x2EED0 → stream 0x3E76D · L2ptr 0x2EA38 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x169
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E43B → header 0x30000 · SprPtr 0x2EED2 → stream 0x3E76D · L2ptr 0x2EA3B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E43E → header 0x30000 · SprPtr 0x2EED4 → stream 0x3E76D · L2ptr 0x2EA3E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E441 → header 0x30000 · SprPtr 0x2EED6 → stream 0x3E76D · L2ptr 0x2EA41 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E444 → header 0x30000 · SprPtr 0x2EED8 → stream 0x3E76D · L2ptr 0x2EA44 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E447 → header 0x30000 · SprPtr 0x2EEDA → stream 0x3E76D · L2ptr 0x2EA47 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E44A → header 0x30000 · SprPtr 0x2EEDC → stream 0x3E76D · L2ptr 0x2EA4A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E44D → header 0x30000 · SprPtr 0x2EEDE → stream 0x3E76D · L2ptr 0x2EA4D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x170
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E450 → header 0x30000 · SprPtr 0x2EEE0 → stream 0x3E76D · L2ptr 0x2EA50 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x171
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E453 → header 0x30000 · SprPtr 0x2EEE2 → stream 0x3E76D · L2ptr 0x2EA53 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x172
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E456 → header 0x30000 · SprPtr 0x2EEE4 → stream 0x3E76D · L2ptr 0x2EA56 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x173
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E459 → header 0x30000 · SprPtr 0x2EEE6 → stream 0x3E76D · L2ptr 0x2EA59 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x174
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E45C → header 0x30000 · SprPtr 0x2EEE8 → stream 0x3E76D · L2ptr 0x2EA5C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x175
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E45F → header 0x30000 · SprPtr 0x2EEEA → stream 0x3E76D · L2ptr 0x2EA5F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x176
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E462 → header 0x30000 · SprPtr 0x2EEEC → stream 0x3E76D · L2ptr 0x2EA62 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x177
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E465 → header 0x30000 · SprPtr 0x2EEEE → stream 0x3E76D · L2ptr 0x2EA65 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x178
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E468 → header 0x30000 · SprPtr 0x2EEF0 → stream 0x3E76D · L2ptr 0x2EA68 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x179
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E46B → header 0x30000 · SprPtr 0x2EEF2 → stream 0x3E76D · L2ptr 0x2EA6B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E46E → header 0x30000 · SprPtr 0x2EEF4 → stream 0x3E76D · L2ptr 0x2EA6E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E471 → header 0x30000 · SprPtr 0x2EEF6 → stream 0x3E76D · L2ptr 0x2EA71 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E474 → header 0x30000 · SprPtr 0x2EEF8 → stream 0x3E76D · L2ptr 0x2EA74 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E477 → header 0x30000 · SprPtr 0x2EEFA → stream 0x3E76D · L2ptr 0x2EA77 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E47A → header 0x30000 · SprPtr 0x2EEFC → stream 0x3E76D · L2ptr 0x2EA7A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E47D → header 0x30000 · SprPtr 0x2EEFE → stream 0x3E76D · L2ptr 0x2EA7D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x180
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E480 → header 0x30000 · SprPtr 0x2EF00 → stream 0x3E76D · L2ptr 0x2EA80 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x181
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E483 → header 0x30000 · SprPtr 0x2EF02 → stream 0x3E76D · L2ptr 0x2EA83 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x182
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E486 → header 0x30000 · SprPtr 0x2EF04 → stream 0x3E76D · L2ptr 0x2EA86 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x183
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E489 → header 0x30000 · SprPtr 0x2EF06 → stream 0x3E76D · L2ptr 0x2EA89 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x184
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E48C → header 0x30000 · SprPtr 0x2EF08 → stream 0x3E76D · L2ptr 0x2EA8C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x185
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E48F → header 0x30000 · SprPtr 0x2EF0A → stream 0x3E76D · L2ptr 0x2EA8F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x186
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E492 → header 0x30000 · SprPtr 0x2EF0C → stream 0x3E76D · L2ptr 0x2EA92 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x187
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E495 → header 0x30000 · SprPtr 0x2EF0E → stream 0x3E76D · L2ptr 0x2EA95 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x188
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E498 → header 0x30000 · SprPtr 0x2EF10 → stream 0x3E76D · L2ptr 0x2EA98 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x189
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E49B → header 0x30000 · SprPtr 0x2EF12 → stream 0x3E76D · L2ptr 0x2EA9B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E49E → header 0x30000 · SprPtr 0x2EF14 → stream 0x3E76D · L2ptr 0x2EA9E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4A1 → header 0x30000 · SprPtr 0x2EF16 → stream 0x3E76D · L2ptr 0x2EAA1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4A4 → header 0x30000 · SprPtr 0x2EF18 → stream 0x3E76D · L2ptr 0x2EAA4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4A7 → header 0x30000 · SprPtr 0x2EF1A → stream 0x3E76D · L2ptr 0x2EAA7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4AA → header 0x30000 · SprPtr 0x2EF1C → stream 0x3E76D · L2ptr 0x2EAAA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4AD → header 0x30000 · SprPtr 0x2EF1E → stream 0x3E76D · L2ptr 0x2EAAD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x190
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4B0 → header 0x30000 · SprPtr 0x2EF20 → stream 0x3E76D · L2ptr 0x2EAB0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x191
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4B3 → header 0x30000 · SprPtr 0x2EF22 → stream 0x3E76D · L2ptr 0x2EAB3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x192
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4B6 → header 0x30000 · SprPtr 0x2EF24 → stream 0x3E76D · L2ptr 0x2EAB6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x193
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4B9 → header 0x3058B · SprPtr 0x2EF26 → stream 0x3C3E3 · L2ptr 0x2EAB9 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x1 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,6)
    - [ ] **Sprite 0xB6** (0xB6) ×2: (0,1,16) (0,14,16)

### Nivel 0x194
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4BC → header 0x30561 · SprPtr 0x2EF28 → stream 0x3C3DB · L2ptr 0x2EABC · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x1 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,5)
    - [ ] **Sprite 0xB6** (0xB6) ×1: (0,1,16)

### Nivel 0x195
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4BF → header 0x30258 · SprPtr 0x2EF2A → stream 0x3C367 · L2ptr 0x2EABF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x196
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4C2 → header 0x3025E · SprPtr 0x2EF2C → stream 0x3C359 · L2ptr 0x2EAC2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x92 (memoria 0x12, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,4)
    - [ ] **Podoboo** (0x33) ×3: (0,2,0) (0,7,0) (0,13,0)

### Nivel 0x197
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4C5 → header 0x3025E · SprPtr 0x2EF2E → stream 0x3C354 · L2ptr 0x2EAC5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x92 (memoria 0x12, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,3)

### Nivel 0x198
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4C8 → header 0x30258 · SprPtr 0x2EF30 → stream 0x3C34F · L2ptr 0x2EAC8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,2)

### Nivel 0x199
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4CB → header 0x30258 · SprPtr 0x2EF32 → stream 0x3C34A · L2ptr 0x2EACB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,1)

### Nivel 0x19A
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4CE → header 0x30258 · SprPtr 0x2EF34 → stream 0x3C345 · L2ptr 0x2EACE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,0)

### Nivel 0x19B
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4D1 → header 0x30252 · SprPtr 0x2EF36 → stream 0x3C340 · L2ptr 0x2EAD1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x10 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x10 (memoria 0x10, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **ActivateBowserBattle** (0xA0) ×1: (0,0,0)

### Nivel 0x19C
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4D4 → header 0x30000 · SprPtr 0x2EF38 → stream 0x3E76D · L2ptr 0x2EAD4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x19D
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4D7 → header 0x30000 · SprPtr 0x2EF3A → stream 0x3E76D · L2ptr 0x2EAD7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x19E
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4DA → header 0x30000 · SprPtr 0x2EF3C → stream 0x3E76D · L2ptr 0x2EADA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x19F
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4DD → header 0x30000 · SprPtr 0x2EF3E → stream 0x3E76D · L2ptr 0x2EADD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4E0 → header 0x30000 · SprPtr 0x2EF40 → stream 0x3E76D · L2ptr 0x2EAE0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4E3 → header 0x30000 · SprPtr 0x2EF42 → stream 0x3E76D · L2ptr 0x2EAE3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4E6 → header 0x30000 · SprPtr 0x2EF44 → stream 0x3E76D · L2ptr 0x2EAE6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4E9 → header 0x30000 · SprPtr 0x2EF46 → stream 0x3E76D · L2ptr 0x2EAE9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4EC → header 0x30000 · SprPtr 0x2EF48 → stream 0x3E76D · L2ptr 0x2EAEC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4EF → header 0x30000 · SprPtr 0x2EF4A → stream 0x3E76D · L2ptr 0x2EAEF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4F2 → header 0x30000 · SprPtr 0x2EF4C → stream 0x3E76D · L2ptr 0x2EAF2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4F5 → header 0x30000 · SprPtr 0x2EF4E → stream 0x3E76D · L2ptr 0x2EAF5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4F8 → header 0x30000 · SprPtr 0x2EF50 → stream 0x3E76D · L2ptr 0x2EAF8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4FB → header 0x30000 · SprPtr 0x2EF52 → stream 0x3E76D · L2ptr 0x2EAFB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E4FE → header 0x30000 · SprPtr 0x2EF54 → stream 0x3E76D · L2ptr 0x2EAFE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E501 → header 0x30000 · SprPtr 0x2EF56 → stream 0x3E76D · L2ptr 0x2EB01 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E504 → header 0x30000 · SprPtr 0x2EF58 → stream 0x3E76D · L2ptr 0x2EB04 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E507 → header 0x30000 · SprPtr 0x2EF5A → stream 0x3E76D · L2ptr 0x2EB07 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E50A → header 0x30000 · SprPtr 0x2EF5C → stream 0x3E76D · L2ptr 0x2EB0A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E50D → header 0x30000 · SprPtr 0x2EF5E → stream 0x3E76D · L2ptr 0x2EB0D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E510 → header 0x30000 · SprPtr 0x2EF60 → stream 0x3E76D · L2ptr 0x2EB10 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E513 → header 0x30000 · SprPtr 0x2EF62 → stream 0x3E76D · L2ptr 0x2EB13 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E516 → header 0x30000 · SprPtr 0x2EF64 → stream 0x3E76D · L2ptr 0x2EB16 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E519 → header 0x30000 · SprPtr 0x2EF66 → stream 0x3E76D · L2ptr 0x2EB19 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E51C → header 0x30000 · SprPtr 0x2EF68 → stream 0x3E76D · L2ptr 0x2EB1C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E51F → header 0x30000 · SprPtr 0x2EF6A → stream 0x3E76D · L2ptr 0x2EB1F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E522 → header 0x30000 · SprPtr 0x2EF6C → stream 0x3E76D · L2ptr 0x2EB22 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E525 → header 0x30000 · SprPtr 0x2EF6E → stream 0x3E76D · L2ptr 0x2EB25 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E528 → header 0x30000 · SprPtr 0x2EF70 → stream 0x3E76D · L2ptr 0x2EB28 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E52B → header 0x30000 · SprPtr 0x2EF72 → stream 0x3E76D · L2ptr 0x2EB2B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1BA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E52E → header 0x30000 · SprPtr 0x2EF74 → stream 0x3E76D · L2ptr 0x2EB2E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1BD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E537 → header 0x3AAC9 · SprPtr 0x2EF7A → stream 0x3E19D · L2ptr 0x2EB37 · GFXslot 0x028F3 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 24 0E` (spriteGfx=12) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x11 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=85 SOLID=67 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 7→0x1C7
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Ninji** (0x51) ×6: (2,43,17) (3,59,23) (4,70,23) (4,73,23) (5,87,23) (5,89,23)
    - [ ] **MechaKoopa** (0xA2) ×2: (4,64,21) (5,83,23)
    - [ ] **Spotlight** (0xC6) ×2: (1,24,0) (6,97,0)
    - [ ] **LightSwitch** (0xC8) ×1: (2,33,19)

### Nivel 0x1BE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E53A → header 0x30FB1 · SprPtr 0x2EF7C → stream 0x3C661 · L2ptr 0x2EB3A · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=6
- **Colisión**: 64×27 casillas · LEDGE_TOP=46 SOLID=26 SLOPE=10 SLOPE_STEEP=10
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1BE (2ª entrada)
- **Usa sprites grandes**: sí — Pokey (0x70)
- **Enemigos (4)**:
    - [B] **Pokey** (0x70) ×3: (1,19,18) (2,34,19) (3,50,16)
    - [ ] **MessageBox** (0xB9) ×1: (1,29,21)

### Nivel 0x1BF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E53D → header 0x39D84 · SprPtr 0x2EF7E → stream 0x3DF94 · L2ptr 0x2EB3D · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 57×27 casillas · LEDGE_TOP=18 SOLID=67 SLOPE_STEEP=4
- **Entrada**: casilla (1,12) px (16,192) · pantalla entrada 3 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x16 0x28 0xA 0x3]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 0→0x1BF (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (9)**:
    - [ ] **Sprite 0x15** (0x15) ×5: (1,18,25) (1,25,25) (1,31,25) (2,40,25) (2,46,25)
    - [ ] **Sprite 0x2E** (0x2E) ×4: (1,16,18) (1,22,20) (1,29,19) (2,39,18)

### Nivel 0x1C0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E540 → header 0x37511 · SprPtr 0x2EF80 → stream 0x3DA7F · L2ptr 0x2EB40 · GFXslot 0x028CF · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=0 SPR=4 backArea=6
- **Colisión**: 48×27 casillas · LEDGE_TOP=23 SOLID=28 SLOPE_STEEP=2
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x1C0 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **HammerBro** (0x9B) ×1: (1,23,15)
    - [ ] **HammerBroPlatform** (0x9C) ×1: (1,23,15)
    - [ ] **GreyFallingPlatform** (0xC4) ×4: (1,18,23) (1,23,23) (1,28,23) (2,33,23)

### Nivel 0x1C1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E543 → header 0x36128 · SprPtr 0x2EF82 → stream 0x3D5CF · L2ptr 0x2EB43 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=1 SPR=3 backArea=2
- **Colisión**: 59×27 casillas · LEDGE_TOP=12 SOLID=52 SLOPE_STEEP=5
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 3 · L2scroll 1 L3 3 L1y 2 L2y 2 · secHdr [0x19 0xF8 0xA 0x3]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 0→0x1C1 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Blurp** (0xC2) ×12: (0,6,22) (0,12,19) (1,18,21) (1,19,15) (1,23,23) (1,29,17) …

### Nivel 0x1C2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E546 → header 0x331B5 · SprPtr 0x2EF84 → stream 0x3CCBA · L2ptr 0x2EB46 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=20 SOLID=109 SLOPE=16 SLOPE_STEEP=20
- **Entrada**: casilla (1,12) px (16,192) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x16 0x28 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x1F7 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (3,51,22)
    - [s] **BuzzyBeetle** (0x11) ×5: (0,14,19) (1,17,19) (1,26,22) (2,35,18) (3,61,22)
    - [ ] **Sprite 0x15** (0x15) ×2: (2,41,25) (3,53,25)

### Nivel 0x1C3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E549 → header 0x32CA8 · SprPtr 0x2EF86 → stream 0x3CBC5 · L2ptr 0x2EB49 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=32 SOLID=82 SLOPE=6 SLOPE_STEEP=10
- **Entrada**: casilla (1,12) px (16,192) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x16 0x28 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x1C3 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (7)**:
    - [s] **BuzzyBeetle** (0x11) ×4: (0,15,21) (1,21,20) (2,45,22) (2,47,22)
    - [ ] **Sprite 0x15** (0x15) ×2: (1,28,25) (3,54,25)
    - [ ] **Sprite 0x2E** (0x2E) ×1: (3,52,20)

### Nivel 0x1C4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E54C → header 0x3B3C6 · SprPtr 0x2EF88 → stream 0x3E402 · L2ptr 0x2EB4C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=6
- **Colisión**: 112×27 casillas · LEDGE_TOP=90 SOLID=72 SLOPE_STEEP=3
- **Entrada**: casilla (14,17) px (224,272) · pantalla entrada 1 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x58 0x1B 0xA 0x1]
- **Cabecera sprites**: 0xC (memoria 0xC, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Lakitu** (0x1E) ×1: (4,70,14)
    - [s] **PSwitch** (0x3E) ×3: (1,24,18) (1,26,18) (1,29,18)
    - [ ] **Sprite 0x78** (0x78) ×3: (1,22,23) (1,24,23) (1,26,23)
    - [ ] **GoalTape** (0x7B) ×1: (5,94,23)
    - [ ] **HammerBro** (0x9B) ×1: (3,48,14)
    - [ ] **HammerBroPlatform** (0x9C) ×1: (3,48,14)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (4,72,21)
    - [ ] **Sprite 0xD2** (0xD2) ×1: (5,89,14)

### Nivel 0x1C5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E54F → header 0x3B3C6 · SprPtr 0x2EF8A → stream 0x3E402 · L2ptr 0x2EB4F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=6
- **Colisión**: 112×27 casillas · LEDGE_TOP=90 SOLID=72 SLOPE_STEEP=3
- **Entrada**: casilla (8,23) px (128,368) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5C 0x19 0xA 0x0]
- **Cabecera sprites**: 0xC (memoria 0xC, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Lakitu** (0x1E) ×1: (4,70,14)
    - [s] **PSwitch** (0x3E) ×3: (1,24,18) (1,26,18) (1,29,18)
    - [ ] **Sprite 0x78** (0x78) ×3: (1,22,23) (1,24,23) (1,26,23)
    - [ ] **GoalTape** (0x7B) ×1: (5,94,23)
    - [ ] **HammerBro** (0x9B) ×1: (3,48,14)
    - [ ] **HammerBroPlatform** (0x9C) ×1: (3,48,14)
    - [ ] **InvisibleMushroom** (0xC7) ×1: (4,72,21)
    - [ ] **Sprite 0xD2** (0xD2) ×1: (5,89,14)

### Nivel 0x1C6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E552 → header 0x3256D · SprPtr 0x2EF8C → stream 0x3CA6D · L2ptr 0x2EB52 · GFXslot 0x028C3 · FGBGslot 0x02947
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 15` (tilesetFG=7)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 64×27 casillas · LEDGE_TOP=21 SOLID=33
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1A 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1C6 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (1,24,24) (2,38,23)
    - [ ] **GreyFallingPlatform** (0xC4) ×6: (0,14,24) (1,19,24) (1,27,24) (2,33,24) (2,41,24) (3,48,24)

### Nivel 0x1C7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E555 → header 0x3AD2F · SprPtr 0x2EF8E → stream 0x3E1C0 · L2ptr 0x2EB55 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x10 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x10 (memoria 0x10, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **ActivateBowserBattle** (0xA0) ×1: (0,0,0)

### Nivel 0x1C8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E558 → header 0x3B896 · SprPtr 0x2EF90 → stream 0x3E4EC · L2ptr 0x2EB58 · GFXslot 0x028CB · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 7 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=5 SPR=2 backArea=0
- **Colisión**: 80×27 casillas · SOLID=15
- **Entrada**: casilla (1,25) px (16,400) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5D 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0 EE1)

### Nivel 0x1CA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E55E → header 0x30BB3 · SprPtr 0x2EF94 → stream 0x3C57F · L2ptr 0x2EB5E · GFXslot 0x028D7 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=30 SOLID=70 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x1CA (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Sprite 0x84** (0x84) ×6: (0,8,15) (0,12,14) (1,19,15) (1,20,13) (1,24,16) (1,28,14)

### Nivel 0x1CB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E561 → header 0x309F8 · SprPtr 0x2EF96 → stream 0x3C3EE · L2ptr 0x2EB61 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=30 SOLID=75 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x1CB (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1CC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E564 → header 0x3AA77 · SprPtr 0x2EF98 → stream 0x3E183 · L2ptr 0x2EB64 · GFXslot 0x028C3 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=0 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=58 SOLID=14 SLOPE_STEEP=6
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1BD
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **Sprite 0x93** (0x93) ×8: (0,14,23) (1,21,23) (1,30,23) (2,33,20) (2,40,23) (2,45,23) …

### Nivel 0x1CD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E567 → header 0x3AA16 · SprPtr 0x2EF9A → stream 0x3E160 · L2ptr 0x2EB67 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=7 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=51 SOLID=19 SLOPE_STEEP=7
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x8F (memoria 0xF, buoyancy 0x80)
- **Salidas de pantalla**: pant 3→0x1BD
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Podoboo** (0x33) ×1: (2,41,15)
    - [ ] **BowserStatue** (0xBC) ×9: (0,8,23) (0,13,21) (1,17,15) (1,26,23) (1,29,21) (2,38,23) …
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)

### Nivel 0x1CE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E56A → header 0x3A961 · SprPtr 0x2EF9C → stream 0x3E131 · L2ptr 0x2EB6A · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x8 · música 3 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=2 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,8) px (16,128) · pantalla entrada 4 · **VERTICAL** · L2scroll 7 L3 3 L1y 0 L2y 3 · secHdr [0x73 0xF8 0x3 0x64]
- **Cabecera sprites**: 0x8E (memoria 0xE, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (15)**:
    - [ ] **Sprite 0x32** (0x32) ×2: (2,39,6) (4,65,8)
    - [ ] **BallNChain** (0x9E) ×6: (1,17,10) (1,21,2) (1,24,7) (1,26,13) (1,30,6) (1,30,1)
    - [ ] **Fishbone** (0xAA) ×6: (3,52,22) (3,53,29) (3,53,15) (3,55,20) (3,56,14) (3,56,27)
    - [ ] **Sprite 0xEF** (0xEF) ×1: (4,72,0 EE1)

### Nivel 0x1CF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E56D → header 0x3A8D9 · SprPtr 0x2EF9E → stream 0x3E114 · L2ptr 0x2EB6D · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x2 · música 3 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 46×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x4B 0xC0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x1BD
- **Usa sprites grandes**: sí — Thwomp (0x26)
- **Enemigos (9)**:
    - [B] **Thwomp** (0x26) ×8: (0,8,14) (0,12,14) (1,16,14) (1,20,14) (1,24,14) (1,28,14) …
    - [ ] **Sprite 0xE9** (0xE9) ×1: (0,8,0 EE2)

### Nivel 0x1D0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E570 → header 0x3A83F · SprPtr 0x2EFA0 → stream 0x3C422 · L2ptr 0x2EB70 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=2 FG=3 SPR=1 backArea=7
- **Colisión**: 87×27 casillas · SOLID=72
- **Entrada**: casilla (1,21) px (16,336) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1A 0x0 0xA 0x0]
- **Cabecera sprites**: 0xF (memoria 0xF, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x1CF · pant 2→0x1CE · pant 3→0x1CD · pant 5→0x1CC
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)

### Nivel 0x1D1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E573 → header 0x3A802 · SprPtr 0x2EFA2 → stream 0x3E0E8 · L2ptr 0x2EB73 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · SOLID=3
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 1 · secHdr [0x59 0x0 0x9 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1D0
- **Usa sprites grandes**: no
- **Enemigos (14)**:
    - [ ] **Sprite 0x78** (0x78) ×1: (1,20,13)
    - [ ] **Sparky** (0xA5) ×8: (0,2,23) (0,7,23) (0,12,18) (1,30,21) (2,36,19) (2,44,20) …
    - [ ] **Sprite 0xA6** (0xA6) ×1: (2,40,20)
    - [ ] **MovingCastleStone** (0xBB) ×3: (0,15,23) (1,24,18) (1,25,23)
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,1)

### Nivel 0x1D2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E576 → header 0x3A765 · SprPtr 0x2EFA4 → stream 0x3E0C5 · L2ptr 0x2EB76 · GFXslot 0x028F3 · FGBGslot 0x02957
- **GFX sprites (SP1-4)**: `00 01 24 0E` (spriteGfx=12) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=11)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=7
- **Colisión**: 80×27 casillas · SOLID=459
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x59 0x0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 4→0x1D0
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Feather** (0x77) ×1: (3,61,12)
    - [ ] **Sprite 0x78** (0x78) ×1: (0,1,8)
    - [ ] **MechaKoopa** (0xA2) ×9: (1,18,20) (1,18,12) (1,31,8) (2,32,20) (2,42,12) (3,53,16) …

### Nivel 0x1D3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E579 → header 0x3A707 · SprPtr 0x2EFA6 → stream 0x3E08D · L2ptr 0x2EB79 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=14 SOLID=38
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x2 (memoria 0x2, buoyancy 0x0)
- **Salidas de pantalla**: pant 2→0x1D0
- **Usa sprites grandes**: no
- **Enemigos (18)**:
    - [ ] **Sprite 0x25** (0x25) ×8: (1,20,22) (1,20,18) (1,24,16) (1,30,19) (1,30,15) (2,36,21) …
    - [ ] **ClimbingNetDoor** (0x54) ×3: (0,11,17) (1,22,17) (2,32,17)
    - [ ] **Sprite 0xB6** (0xB6) ×7: (1,17,22) (1,17,16) (1,22,16) (1,28,18) (2,34,16) (2,34,22) …

### Nivel 0x1D4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E57C → header 0x3A68E · SprPtr 0x2EFA8 → stream 0x3E067 · L2ptr 0x2EB7C · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=15 SOLID=31
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 2 L1y 2 L2y 2 · secHdr [0x1B 0x80 0xA 0x0]
- **Cabecera sprites**: 0x8F (memoria 0xF, buoyancy 0x80)
- **Salidas de pantalla**: pant 2→0x1D0
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Podoboo** (0x33) ×9: (0,9,15) (0,14,15) (1,21,15) (1,26,19) (1,26,15) (2,34,15) …
    - [ ] **Layer3Smasher** (0x89) ×1: (0,7,0)
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0)

### Nivel 0x1D5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E57F → header 0x3AFCE · SprPtr 0x2EFAA → stream 0x3C3F0 · L2ptr 0x2EB7F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=21 SOLID=13
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1D6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E582 → header 0x3AF16 · SprPtr 0x2EFAC → stream 0x3C3F0 · L2ptr 0x2EB82 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1D7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E585 → header 0x30838 · SprPtr 0x2EFAE → stream 0x3C498 · L2ptr 0x2EB85 · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x1D8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E588 → header 0x307F3 · SprPtr 0x2EFB0 → stream 0x3C473 · L2ptr 0x2EB88 · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x1D9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E58B → header 0x39803 · SprPtr 0x2EFB2 → stream 0x3DE01 · L2ptr 0x2EB8B · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=4 SPR=5 backArea=7
- **Colisión**: 64×27 casillas · SOLID=1 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (14,19) px (224,304) · pantalla entrada 3 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0x9 0x3 0xA 0x3]
- **Cabecera sprites**: 0xD (memoria 0xD, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1DD
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **GreenGasBubble** (0x90) ×4: (1,21,18) (1,29,19) (2,40,19) (3,52,18)

### Nivel 0x1DA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E58E → header 0x30621 · SprPtr 0x2EFB4 → stream 0x3C3F5 · L2ptr 0x2EB8E · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1DB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E591 → header 0x39969 · SprPtr 0x2EFB6 → stream 0x3DE3B · L2ptr 0x2EB91 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=5 backArea=7
- **Colisión**: 51×27 casillas · SOLID=34
- **Entrada**: casilla (14,22) px (224,352) · pantalla entrada 3 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x3 0xA 0x3]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1DD
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Keyhole** (0xE) ×1: (3,63,5)
    - [ ] **Sprite 0x37** (0x37) ×3: (2,44,20) (3,54,21) (3,59,19)
    - [s] **PSwitch** (0x3E) ×1: (0,6,23)
    - [ ] **Key** (0x80) ×1: (3,58,5)

### Nivel 0x1DC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E594 → header 0x39969 · SprPtr 0x2EFB8 → stream 0x3DE3B · L2ptr 0x2EB94 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=5 backArea=7
- **Colisión**: 51×27 casillas · SOLID=34
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0xB 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1DD
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Keyhole** (0xE) ×1: (3,63,5)
    - [ ] **Sprite 0x37** (0x37) ×3: (2,44,20) (3,54,21) (3,59,19)
    - [s] **PSwitch** (0x3E) ×1: (0,6,23)
    - [ ] **Key** (0x80) ×1: (3,58,5)

### Nivel 0x1DD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E597 → header 0x39867 · SprPtr 0x2EFBA → stream 0x3DE0F · L2ptr 0x2EB97 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=5 backArea=7
- **Colisión**: 159×27 casillas · SOLID=96
- **Entrada**: casilla (8,19) px (128,304) · pantalla entrada 3 · L2scroll 0 L3 0 L1y 2 L2y 2 · secHdr [0x9 0x1 0xA 0x3]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x1DB · pant 3→0x1D9 · pant 6→0x114 · pant 7→0x1DB · pant 8→0x1DA · pant 9→0x1DC
- **Usa sprites grandes**: no
- **Enemigos (14)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (1,16,22) (2,34,23) (2,44,23) (4,76,21)
    - [ ] **Sprite 0x39** (0x39) ×3: (4,72,19) (5,95,24) (6,106,16)
    - [s] **PSwitch** (0x3E) ×1: (3,56,15)
    - [ ] **Star** (0x76) ×1: (3,56,4)
    - [ ] **Sprite 0x78** (0x78) ×1: (9,158,23)
    - [ ] **Sprite 0xB0** (0xB0) ×4: (1,24,20) (1,27,14) (2,39,19) (2,40,14)

### Nivel 0x1DE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E59A → header 0x30636 · SprPtr 0x2EFBC → stream 0x3C414 · L2ptr 0x2EB9A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x1DF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E59D → header 0x36104 · SprPtr 0x2EFBE → stream 0x3D5C7 · L2ptr 0x2EB9D · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: 16×27 casillas · LEDGE_TOP=12 SOLID=50 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x29 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Keyhole** (0xE) ×1: (0,11,22)
    - [ ] **Key** (0x80) ×1: (0,6,22)

### Nivel 0x1E0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5A0 → header 0x3BD8A · SprPtr 0x2EFC0 → stream 0x3C3EE · L2ptr 0x2EBA0 · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=5 SPR=0 backArea=1
- **Colisión**: 80×27 casillas · LEDGE_TOP=16 SOLID=54 SLOPE=60 SLOPE_STEEP=60
- **Entrada**: casilla (1,6) px (16,96) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 0 L2y 3 · secHdr [0x52 0x18 0x3 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 4→0x1E0 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1E1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5A3 → header 0x3BD75 · SprPtr 0x2EFC2 → stream 0x3C3F0 · L2ptr 0x2EBA3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1E2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5A6 → header 0x395F0 · SprPtr 0x2EFC4 → stream 0x3DDB8 · L2ptr 0x2EBA6 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 9 pantallas (144 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 144×27 casillas · LEDGE_TOP=130 SOLID=164 SLOPE_STEEP=38
- **Entrada**: casilla (8,17) px (128,272) · pantalla entrada 1 · L2scroll 4 L3 3 L1y 2 L2y 3 · secHdr [0x48 0xD1 0xB 0x1]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: sí — MegaMole (0xBF)
- **Enemigos (7)**:
    - [ ] **Keyhole** (0xE) ×1: (0,8,20)
    - [ ] **GoalTape** (0x7B) ×1: (7,126,23)
    - [ ] **Key** (0x80) ×1: (0,3,20)
    - [B] **MegaMole** (0xBF) ×3: (3,57,20) (4,71,17) (7,115,16)
    - [ ] **Sprite 0xF5** (0xF5) ×1: (1,24,0 EE2)

### Nivel 0x1E3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5A9 → header 0x393E2 · SprPtr 0x2EFC6 → stream 0x3DDB3 · L2ptr 0x2EBA9 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 160×27 casillas · LEDGE_TOP=136 SOLID=140 SLOPE=7 SLOPE_STEEP=67
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x4B 0xD1 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 9→0x1E2
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,0 EE1)

### Nivel 0x1E5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5AF → header 0x39221 · SprPtr 0x2EFCA → stream 0x3DD76 · L2ptr 0x2EBAF · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x18 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1E6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5B2 → header 0x35F46 · SprPtr 0x2EFCC → stream 0x3C3F5 · L2ptr 0x2EBB2 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1E7
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5B5 → header 0x30621 · SprPtr 0x2EFCE → stream 0x3C40C · L2ptr 0x2EBB5 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23 EE1)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1E8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5B8 → header 0x35ABE · SprPtr 0x2EFD0 → stream 0x3D522 · L2ptr 0x2EBB8 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x1 · música 4 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 170×27 casillas · SOLID=2
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 7 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x1 0xA 0x7]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x1E6 · pant 3→0x1E7 · pant 10→0x1FA
- **Usa sprites grandes**: no
- **Enemigos (24)**:
    - [ ] **BouncingFootball** (0x28) ×2: (1,17,14) (2,43,17)
    - [ ] **Sprite 0x37** (0x37) ×13: (1,25,17) (1,28,24) (3,51,16) (3,60,24) (4,66,18) (4,74,21) …
    - [ ] **Sprite 0x38** (0x38) ×1: (2,37,20)
    - [ ] **Sprite 0x39** (0x39) ×7: (8,128,20) (8,133,18) (8,133,19) (8,133,20) (9,155,16) (10,160,17) …
    - [ ] **Sprite 0xDE** (0xDE) ×1: (6,98,13)

### Nivel 0x1E9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5BB → header 0x35ABE · SprPtr 0x2EFD2 → stream 0x3D522 · L2ptr 0x2EBBB · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x1 · música 4 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 170×27 casillas · SOLID=2
- **Entrada**: casilla (8,19) px (128,304) · pantalla entrada 6 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x29 0x1 0xA 0x6]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x1E6 · pant 3→0x1E7 · pant 10→0x1FA
- **Usa sprites grandes**: no
- **Enemigos (24)**:
    - [ ] **BouncingFootball** (0x28) ×2: (1,17,14) (2,43,17)
    - [ ] **Sprite 0x37** (0x37) ×13: (1,25,17) (1,28,24) (3,51,16) (3,60,24) (4,66,18) (4,74,21) …
    - [ ] **Sprite 0x38** (0x38) ×1: (2,37,20)
    - [ ] **Sprite 0x39** (0x39) ×7: (8,128,20) (8,133,18) (8,133,19) (8,133,20) (9,155,16) (10,160,17) …
    - [ ] **Sprite 0xDE** (0xDE) ×1: (6,98,13)

### Nivel 0x1EA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5BE → header 0x32E18 · SprPtr 0x2EFD4 → stream 0x3CC11 · L2ptr 0x2EBBE · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0xC · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 127×27 casillas · SOLID=3
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x1 0xA 0x0]
- **Cabecera sprites**: 0xD (memoria 0xD, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x1FB · pant 7→0x1F9
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **GreenGasBubble** (0x90) ×6: (1,20,17) (2,34,16) (3,50,16) (4,70,16) (5,82,13) (6,98,16)

### Nivel 0x1EB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5C1 → header 0x30687 · SprPtr 0x2EFD6 → stream 0x3E024 · L2ptr 0x2EBC1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x92 (memoria 0x12, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,4)
    - [ ] **Podoboo** (0x33) ×3: (0,2,0) (0,7,0) (0,13,0)

### Nivel 0x1EC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5C4 → header 0x3735D · SprPtr 0x2EFD8 → stream 0x3DA44 · L2ptr 0x2EBC4 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=7 SPR=4 backArea=3
- **Colisión**: 256×27 casillas · LEDGE_TOP=93 SOLID=156 SLOPE=27 SLOPE_STEEP=41
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x4B 0xD1 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 15→0x1EE
- **Usa sprites grandes**: sí — CharginChuck (0x91)
- **Enemigos (19)**:
    - [ ] **Sprite 0x2E** (0x2E) ×12: (1,18,21) (1,21,19) (1,24,17) (1,27,15) (2,47,15) (3,54,19) …
    - [B] **CharginChuck** (0x91) ×6: (9,149,23) (9,158,19) (10,165,19) (10,174,15) (11,185,18) (12,193,15)
    - [ ] **Sprite 0xF5** (0xF5) ×1: (0,8,0)

### Nivel 0x1ED
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5C7 → header 0x37164 · SprPtr 0x2EFDA → stream 0x3DA12 · L2ptr 0x2EBC7 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0xA · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,3) px (16,48) · pantalla entrada 0 · **VERTICAL** · L2scroll 0 L3 0 L1y 0 L2y 3 · secHdr [0x1 0x10 0x3 0x60]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (16)**:
    - [s] **BuzzyBeetle** (0x11) ×16: (1,20,10) (1,21,8) (2,34,14) (2,36,18) (2,37,21) (3,52,16) …

### Nivel 0x1EE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5CA → header 0x374FC · SprPtr 0x2EFDC → stream 0x3C3F0 · L2ptr 0x2EBCA · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=20
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1EF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5CD → header 0x328E9 · SprPtr 0x2EFDE → stream 0x3CB01 · L2ptr 0x2EBCD · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=2 SPR=4 backArea=3
- **Colisión**: 96×27 casillas · LEDGE_TOP=17 SOLID=60 SLOPE=14 SLOPE_STEEP=16
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x49 0xD0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 5→0x1EF (2ª entrada)
- **Usa sprites grandes**: sí — Blargg (0xA8)
- **Enemigos (13)**:
    - [ ] **RedParakoopa** (0x9) ×1: (4,75,18)
    - [s] **BuzzyBeetle** (0x11) ×7: (1,30,14) (2,33,14) (2,41,14) (2,46,14) (3,56,16) (3,62,16) …
    - [B] **Blargg** (0xA8) ×4: (1,29,25) (2,40,25) (3,52,25) (3,62,25)
    - [ ] **Sprite 0xF5** (0xF5) ×1: (0,8,0 EE1)

### Nivel 0x1F0
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5D0 → header 0x33A33 · SprPtr 0x2EFE0 → stream 0x3CE14 · L2ptr 0x2EBD0 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=37 SOLID=41 SLOPE=5 SLOPE_STEEP=8
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x9 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (1,30,23 EE1)
    - [ ] **Sprite 0x97** (0x97) ×1: (1,23,15)

### Nivel 0x1F1
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5D3 → header 0x33A06 · SprPtr 0x2EFE2 → stream 0x3CE0C · L2ptr 0x2EBD3 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=49 SOLID=30 SLOPE=5 SLOPE_STEEP=6
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x11 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (1,30,23)
    - [ ] **Sprite 0x97** (0x97) ×1: (1,22,16)

### Nivel 0x1F2
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5D6 → header 0x337ED · SprPtr 0x2EFE4 → stream 0x3CDC0 · L2ptr 0x2EBD6 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) px (128,224) · pantalla entrada 0 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x57 0x1 0xA 0x0]
- **Cabecera sprites**: 0xE (memoria 0xE, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,5)
    - [ ] **Sprite 0xB6** (0xB6) ×1: (0,1,16)

### Nivel 0x1F3
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5D9 → header 0x33666 · SprPtr 0x2EFE6 → stream 0x3CD94 · L2ptr 0x2EBD9 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 4 L3 3 L1y 2 L2y 2 · secHdr [0x48 0xC0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 11→0x1F2
- **Usa sprites grandes**: no
- **Enemigos (14)**:
    - [ ] **Sprite 0x32** (0x32) ×8: (2,37,19) (3,56,21) (5,81,18) (8,142,21) (9,151,15) (9,154,15) …
    - [ ] **Podoboo** (0x33) ×4: (3,49,15) (4,74,15) (6,97,14) (8,137,14)
    - [ ] **Sprite 0x74** (0x74) ×1: (6,106,24)
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,0 EE3)

### Nivel 0x1F4
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5DC → header 0x33620 · SprPtr 0x2EFE8 → stream 0x3C3EE · L2ptr 0x2EBDC · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=1
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 1→0x1F3
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1F5
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5DF → header 0x33422 · SprPtr 0x2EFEA → stream 0x3CD63 · L2ptr 0x2EBDF · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 96×27 casillas · LEDGE_TOP=12 SOLID=107 SLOPE_STEEP=5
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 5→0x1F5 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **SkullRaft** (0x61) ×1: (0,10,24)

### Nivel 0x1F6
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5E2 → header 0x30687 · SprPtr 0x2EFEC → stream 0x3C6D0 · L2ptr 0x2EBE2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 0 L3 0 L1y 0 L2y 0 · secHdr [0xB 0x0 0x0 0x0]
- **Cabecera sprites**: 0x92 (memoria 0x12, buoyancy 0x80)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,3)

### Nivel 0x1F8
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5E8 → header 0x35914 · SprPtr 0x2EFF0 → stream 0x3D4C5 · L2ptr 0x2EBE8 · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 16×27 casillas · LEDGE_TOP=13 SOLID=42 SLOPE_STEEP=5
- **Entrada**: casilla (1,17) px (16,272) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x28 0x20 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: pant 0→0x1F8 (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Keyhole** (0xE) ×1: (0,10,23)
    - [ ] **Key** (0x80) ×1: (0,8,23)

### Nivel 0x1F9
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5EB → header 0x30621 · SprPtr 0x2EFF2 → stream 0x3C3F5 · L2ptr 0x2EBEB · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 2 L3 0 L1y 2 L2y 2 · secHdr [0x2B 0x0 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1FA
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5EE → header 0x35ED2 · SprPtr 0x2EFF4 → stream 0x3D56C · L2ptr 0x2EBEE · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 128×27 casillas · SOLID=2
- **Entrada**: casilla (8,22) px (128,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x1 0xA 0x0]
- **Cabecera sprites**: 0x7 (memoria 0x7, buoyancy 0x0)
- **Salidas de pantalla**: pant 6→0x1E8 · pant 7→0x1E9
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **PSwitch** (0x3E) ×1: (6,102,18)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (1,25,21)
    - [ ] **Sprite 0xE1** (0xE1) ×1: (0,8,0)

### Nivel 0x1FB
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5F1 → header 0x32D09 · SprPtr 0x2EFF6 → stream 0x3CBDC · L2ptr 0x2EBF1 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 176×27 casillas · SOLID=38 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (8,21) px (128,336) · pantalla entrada 5 · L2scroll 5 L3 0 L1y 2 L2y 2 · secHdr [0x5A 0x1 0xA 0x5]
- **Cabecera sprites**: 0xB (memoria 0xB, buoyancy 0x0)
- **Salidas de pantalla**: pant 10→0x1EA
- **Usa sprites grandes**: no
- **Enemigos (17)**:
    - [ ] **BouncingFootball** (0x28) ×2: (7,115,18) (8,142,20)
    - [ ] **Sprite 0x37** (0x37) ×9: (3,49,22) (3,61,23) (4,66,12) (4,72,22) (4,79,21) (5,90,16) …
    - [ ] **Sprite 0x38** (0x38) ×4: (1,18,22) (1,18,17) (9,156,4) (10,171,4)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (10,160,20)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,31,20)

### Nivel 0x1FC
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5F4 → header 0x3116F · SprPtr 0x2EFF8 → stream 0x3C6BF · L2ptr 0x2EBF4 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=51 SOLID=14 SLOPE_STEEP=2
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 2 L1y 2 L2y 2 · secHdr [0x1B 0x80 0xA 0x0]
- **Cabecera sprites**: 0xF (memoria 0xF, buoyancy 0x0)
- **Salidas de pantalla**: pant 3→0x1F6
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **Sprite 0x5A** (0x5A) ×1: (3,52,24)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (2,37,21)
    - [ ] **Layer3Smasher** (0x89) ×1: (1,18,0)
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0)

### Nivel 0x1FD
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5F7 → header 0x30E6D · SprPtr 0x2EFFA → stream 0x3C5EF · L2ptr 0x2EBF7 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=12 SOLID=44 SLOPE_STEEP=4
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x20 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 1→0x1FD (2ª entrada)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **PSwitch** (0x3E) ×1: (0,6,23)

### Nivel 0x1FE
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5FA → header 0x39F22 · SprPtr 0x2EFFC → stream 0x3DFE0 · L2ptr 0x2EBFA · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 160×27 casillas · LEDGE_TOP=105 SOLID=113 SLOPE_STEEP=14
- **Entrada**: casilla (1,19) px (16,304) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x19 0x0 0xA 0x0]
- **Cabecera sprites**: 0x80 (memoria 0x0, buoyancy 0x80)
- **Salidas de pantalla**: pant 9→0x1EB
- **Usa sprites grandes**: sí — MagiKoopa (0x1F)
- **Enemigos (22)**:
    - [B] **MagiKoopa** (0x1F) ×1: (2,39,0)
    - [ ] **Sprite 0x32** (0x32) ×6: (1,16,20) (1,29,23) (2,44,23) (4,66,23) (8,132,19) (9,149,23)
    - [ ] **Podoboo** (0x33) ×4: (4,68,16) (5,89,14) (6,110,16) (7,116,16)
    - [ ] **DownFirstWoodenSpike** (0xAC) ×7: (2,32,17) (2,35,17) (4,79,13) (5,83,13) (6,106,16) (7,113,16) …
    - [ ] **UpDownFirstWoodenSpike** (0xAD) ×4: (3,54,19) (3,58,19) (4,79,22) (5,83,22)

### Nivel 0x1FF
- **Nombre (overworld)**: — (sublevel, sin nombre de mapa)
- **Tipo**: sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)
- **Direcciones**: L1ptr 0x2E5FD → header 0x30F93 · SprPtr 0x2EFFE → stream 0x3C659 · L2ptr 0x2EBFD · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=1 SPR=2 backArea=6
- **Colisión**: 17×27 casillas · SOLID=4
- **Entrada**: casilla (1,22) px (16,352) · pantalla entrada 0 · L2scroll 1 L3 0 L1y 2 L2y 2 · secHdr [0x1B 0x10 0xA 0x0]
- **Cabecera sprites**: 0x0 (memoria 0x0, buoyancy 0x0)
- **Salidas de pantalla**: (ninguna)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **MessageBox** (0xB9) ×1: (0,6,21)

