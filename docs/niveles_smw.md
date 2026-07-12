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
- **Direcciones**: L1ptr 0x2E000 → header 0x30654 · SprPtr 0x2EC00 → stream 0x3C407 · L2ptr 0x2E600 · GFXslot 0x028DB · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=6) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 7 · tiempo 0 · Layer2 fondo · paletas BG=5 FG=0 SPR=4 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,14) = px (16,224)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **BonusGame** (0x82) ×1: (0,5,7)

### Nivel 0x001
- **Direcciones**: L1ptr 0x2E003 → header 0x33A69 · SprPtr 0x2EC02 → stream 0x3CE1C · L2ptr 0x2E603 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=6 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=238 SOLID=123 SLOPE=72 SLOPE_STEEP=78
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x002
- **Direcciones**: L1ptr 0x2E006 → header 0x33C33 · SprPtr 0x2EC04 → stream 0x3CEBF · L2ptr 0x2E606 · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=3 backArea=2
- **Colisión**: 256×27 casillas · SOLID=14
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (23)**:
    - [ ] **Sprite 0x41** (0x41) ×3: (6,103,24) (7,115,24) (9,159,24)
    - [ ] **Sprite 0x42** (0x42) ×9: (1,18,24) (1,24,24) (1,30,24) (2,36,24) (6,99,24) (7,120,24) …
    - [ ] **Sprite 0x43** (0x43) ×5: (6,106,24) (8,142,24) (10,162,24) (10,170,24) (10,174,24)
    - [ ] **PorcuPuffer** (0xC3) ×1: (6,110,24)
    - [ ] **Sprite 0xCF** (0xCF) ×1: (3,52,0)
    - [ ] **Sprite 0xD0** (0xD0) ×1: (11,185,0)
    - [ ] **Sprite 0xD9** (0xD9) ×3: (1,21,0) (5,86,0) (9,158,0)

### Nivel 0x003
- **Direcciones**: L1ptr 0x2E009 → header 0x308BF · SprPtr 0x2EC06 → stream 0x3C4C5 · L2ptr 0x2E609 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 16×27 casillas · LEDGE_TOP=16 SOLID=5
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **SideExitAndFireplace** (0x8C) ×1: (0,7,22)

### Nivel 0x004
- **Direcciones**: L1ptr 0x2E00C → header 0x31807 · SprPtr 0x2EC08 → stream 0x3C7B5 · L2ptr 0x2E60C · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=4 SPR=5 backArea=7
- **Colisión**: 110×27 casillas · SOLID=4
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **MessageBox** (0xB9) ×1: (0,9,21)
    - [ ] **Sprite 0xE1** (0xE1) ×1: (0,0,16)

### Nivel 0x005
- **Direcciones**: L1ptr 0x2E00F → header 0x31961 · SprPtr 0x2EC0A → stream 0x3C7D9 · L2ptr 0x2E60F · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=1 SPR=2 backArea=1
- **Colisión**: 305×27 casillas · LEDGE_TOP=41 SOLID=40
- **Entrada**: casilla (8,22) = px (128,352)
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

### Nivel 0x006
- **Direcciones**: L1ptr 0x2E012 → header 0x31BB5 · SprPtr 0x2EC0C → stream 0x3C844 · L2ptr 0x2E612 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 320×27 casillas · LEDGE_TOP=292 SOLID=155 SLOPE=32 SLOPE_STEEP=41
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x007
- **Direcciones**: L1ptr 0x2E015 → header 0x31DC0 · SprPtr 0x2EC0E → stream 0x3C904 · L2ptr 0x2E615 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=47 SOLID=9
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **Thwimp** (0x27) ×2: (0,9,11) (1,16,11)
    - [ ] **BallNChain** (0x9E) ×3: (1,22,18) (1,28,12) (2,34,6)

### Nivel 0x008
- **Direcciones**: L1ptr 0x2E018 → header 0x3076E · SprPtr 0x2EC10 → stream 0x3C49D · L2ptr 0x2E618 · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=58
- **Entrada**: casilla (1,14) = px (16,224)
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [s] **BlueKoopa** (0x2) ×1: (0,8,19)
    - [s] **RedKoopaNoShell** (0x5) ×8: (0,14,23) (1,18,23) (1,22,23) (1,26,23) (1,30,23) (2,34,23) …
    - [s] **PSwitch** (0x3E) ×1: (0,10,15)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (0,6,15)

### Nivel 0x009
- **Direcciones**: L1ptr 0x2E01B → header 0x3162D · SprPtr 0x2EC12 → stream 0x3C751 · L2ptr 0x2E61B · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=2 SPR=4 backArea=3
- **Colisión**: 192×27 casillas · LEDGE_TOP=100 SOLID=189 SLOPE=42 SLOPE_STEEP=50
- **Entrada**: casilla (1,14) = px (16,224)
- **Usa sprites grandes**: no
- **Enemigos (28)**:
    - [s] **BuzzyBeetle** (0x11) ×10: (1,19,20) (1,27,18) (2,39,18) (4,70,13) (7,113,16) (7,119,16) …
    - [ ] **Sprite 0x2E** (0x2E) ×2: (11,184,17) (11,186,17)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (8,130,14)
    - [ ] **Swooper** (0xBE) ×14: (1,22,16) (1,30,16) (2,47,11) (4,73,6) (4,76,6) (5,82,6) …
    - [ ] **Sprite 0xE8** (0xE8) ×1: (0,8,0)

### Nivel 0x00A
- **Direcciones**: L1ptr 0x2E01E → header 0x32134 · SprPtr 0x2EC14 → stream 0x3C948 · L2ptr 0x2E61E · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 13 pantallas (208 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=2 SPR=3 backArea=2
- **Colisión**: 194×27 casillas · LEDGE_TOP=26 SOLID=135 SLOPE=3 SLOPE_STEEP=19
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (32)**:
    - [ ] **Keyhole** (0xE) ×1: (9,152,20)
    - [ ] **Sprite 0x15** (0x15) ×6: (3,53,23) (7,112,17) (7,114,20) (10,163,19) (10,166,22) (11,176,22)
    - [ ] **VerticalCheepCheep** (0x16) ×6: (1,25,19) (4,76,15) (5,81,16) (5,92,22) (5,94,21) (10,172,25)
    - [ ] **RipVanFish** (0x3D) ×9: (0,12,23) (2,39,22) (2,47,18) (3,61,20) (5,86,20) (6,99,19) …
    - [s] **PSwitch** (0x3E) ×1: (7,116,21)
    - [ ] **GoalTape** (0x7B) ×1: (11,190,23)
    - [ ] **Blurp** (0xC2) ×8: (1,29,23) (3,50,23) (4,68,18) (4,71,21) (7,126,20) (8,129,17) …

### Nivel 0x00B
- **Direcciones**: L1ptr 0x2E021 → header 0x33D0F · SprPtr 0x2EC16 → stream 0x3CF06 · L2ptr 0x2E621 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=2 SPR=1 backArea=2
- **Colisión**: 127×27 casillas · SOLID=22
- **Entrada**: casilla (8,19) = px (128,304)
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (23)**:
    - [ ] **ThrowingDryBones** (0x30) ×2: (6,103,23) (7,113,23)
    - [B] **BonyBeetle** (0x31) ×2: (6,96,23) (6,110,23)
    - [ ] **BallNChain** (0x9E) ×3: (1,20,19) (2,33,17) (3,48,14)
    - [ ] **Fishbone** (0xAA) ×10: (1,25,21) (1,29,17) (2,44,15) (3,55,23) (4,69,15) (5,84,21) …
    - [ ] **FallingSpike** (0xB2) ×6: (5,93,16) (6,100,16) (6,101,16) (6,107,16) (6,110,16) (7,115,16)

### Nivel 0x00C
- **Direcciones**: L1ptr 0x2E024 → header 0x35000 · SprPtr 0x2EC18 → stream 0x3D1F5 · L2ptr 0x2E624 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=6
- **Colisión**: 191×27 casillas · SOLID=47
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: sí — GreenParakoopa (0x8)
- **Enemigos (33)**:
    - [B] **GreenParakoopa** (0x8) ×11: (4,78,10) (5,80,10) (5,82,10) (5,84,10) (5,86,10) (5,88,10) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×3: (2,35,20) (3,57,16) (9,152,9)
    - [ ] **BobOmb** (0xB) ×6: (10,172,14) (10,174,15) (11,176,16) (11,178,17) (11,180,18) (11,182,19)
    - [ ] **ScalePlatform** (0x8F) ×9: (1,23,22) (2,33,20) (2,46,18) (4,67,13) (5,95,13) (6,103,9) …
    - [ ] **GreyFallingPlatform** (0xC4) ×3: (2,41,18) (3,49,17) (10,161,8)
    - [ ] **Sprite 0xE8** (0xE8) ×1: (0,0,0)

### Nivel 0x00D
- **Direcciones**: L1ptr 0x2E027 → header 0x350F4 · SprPtr 0x2EC1A → stream 0x3D25A · L2ptr 0x2E627 · GFXslot 0x028D7 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=0 backArea=7
- **Colisión**: 305×27 casillas · LEDGE_TOP=11 SOLID=24
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x00E
- **Direcciones**: L1ptr 0x2E02A → header 0x343A3 · SprPtr 0x2EC1C → stream 0x3D0D7 · L2ptr 0x2E62A · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x1 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=0 FG=3 SPR=1 backArea=3
- **Colisión**: 95×27 casillas · SOLID=11
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (9)**:
    - [B] **BonyBeetle** (0x31) ×2: (2,33,24) (2,47,20)
    - [ ] **Sprite 0x74** (0x74) ×1: (5,80,20)
    - [ ] **BallNChain** (0x9E) ×6: (2,37,24) (3,50,16) (3,54,23) (3,58,16) (4,65,22) (4,66,17)

### Nivel 0x00F
- **Direcciones**: L1ptr 0x2E02D → header 0x33EAD · SprPtr 0x2EC1E → stream 0x3CFAF · L2ptr 0x2E62D · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 22 pantallas (352 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=0
- **Colisión**: 337×27 casillas · SOLID=20
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (42)**:
    - [ ] **Sprite 0x63** (0x63) ×3: (1,19,24) (1,27,21) (2,39,18)
    - [ ] **Sprite 0x64** (0x64) ×6: (10,164,16) (11,186,17) (12,205,18) (13,210,16) (13,216,16) (15,241,16)
    - [ ] **Sprite 0x65** (0x65) ×30: (1,29,24) (2,41,21) (3,59,25) (4,68,22) (4,69,17) (4,77,25) …
    - [ ] **Sprite 0x66** (0x66) ×1: (15,249,13)
    - [ ] **GoalTape** (0x7B) ×2: (18,302,21) (20,334,23)

### Nivel 0x010
- **Direcciones**: L1ptr 0x2E030 → header 0x341C4 · SprPtr 0x2EC20 → stream 0x3D043 · L2ptr 0x2E630 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 21 pantallas (336 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=2
- **Colisión**: 336×27 casillas · LEDGE_TOP=412 SOLID=216 SLOPE=20 SLOPE_STEEP=27
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x011
- **Direcciones**: L1ptr 0x2E033 → header 0x34783 · SprPtr 0x2EC22 → stream 0x3D157 · L2ptr 0x2E633 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=4 SPR=3 backArea=2
- **Colisión**: 192×27 casillas · LEDGE_TOP=4 SOLID=77 SLOPE_STEEP=29
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (52)**:
    - [ ] **Blurp** (0xC2) ×33: (1,19,17) (1,22,15) (2,35,23) (2,35,20) (2,42,23) (2,44,23) …
    - [ ] **Sprite 0xCA** (0xCA) ×19: (1,17,19) (2,38,16) (2,40,17) (3,55,22) (3,57,17) (4,77,15) …

### Nivel 0x012
- **Direcciones**: L1ptr 0x2E036 → header 0x30000 · SprPtr 0x2EC24 → stream 0x3E76D · L2ptr 0x2E636 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,11) = px (16,176)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x013
- **Direcciones**: L1ptr 0x2E039 → header 0x322F2 · SprPtr 0x2EC26 → stream 0x3C9CA · L2ptr 0x2E639 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 79×27 casillas · SOLID=2 SLOPE=16 SLOPE_STEEP=16
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **BouncingFootball** (0x28) ×1: (3,51,20)
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,41,15)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,42,22)
    - [s] **PSwitch** (0x3E) ×1: (2,46,23)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,26,20)

### Nivel 0x014
- **Direcciones**: L1ptr 0x2E03C → header 0x3068D · SprPtr 0x2EC28 → stream 0x3C446 · L2ptr 0x2E63C · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=36
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **PSwitch** (0x3E) ×1: (0,8,23)

### Nivel 0x015
- **Direcciones**: L1ptr 0x2E03F → header 0x311E5 · SprPtr 0x2EC2A → stream 0x3C6D5 · L2ptr 0x2E63F · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=418 SOLID=151 SLOPE=1 SLOPE_STEEP=8
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x016
- **Direcciones**: L1ptr 0x2E042 → header 0x311E5 · SprPtr 0x2EC2C → stream 0x3C6D5 · L2ptr 0x2E642 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=418 SOLID=151 SLOPE=1 SLOPE_STEEP=8
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x017
- **Direcciones**: L1ptr 0x2E045 → header 0x311E5 · SprPtr 0x2EC2E → stream 0x3C6D5 · L2ptr 0x2E645 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=418 SOLID=151 SLOPE=1 SLOPE_STEEP=8
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x018
- **Direcciones**: L1ptr 0x2E048 → header 0x38C14 · SprPtr 0x2EC30 → stream 0x3DC2D · L2ptr 0x2E648 · GFXslot 0x028D3 · FGBGslot 0x0295F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 07` (tilesetFG=13)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0xE · música 4 · tiempo 400 · Layer2 fondo · paletas BG=4 FG=4 SPR=3 backArea=3
- **Colisión**: 46×27 casillas · SOLID=26
- **Entrada**: casilla (1,3) = px (16,48)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0xC9** (0xC9) ×4: (1,17,10) (1,22,12) (1,24,5) (2,35,14)

### Nivel 0x019
- **Direcciones**: L1ptr 0x2E04B → header 0x30000 · SprPtr 0x2EC32 → stream 0x3E76D · L2ptr 0x2E64B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x01A
- **Direcciones**: L1ptr 0x2E04E → header 0x389CC · SprPtr 0x2EC34 → stream 0x3DBBB · L2ptr 0x2E64E · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 192×27 casillas · LEDGE_TOP=163 SOLID=59 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (20)**:
    - [ ] **PortableSpringboard** (0x2F) ×2: (1,20,24) (1,21,24)
    - [ ] **ThrowingDryBones** (0x30) ×1: (1,17,23)
    - [ ] **Podoboo** (0x33) ×2: (4,66,16) (4,73,15)
    - [ ] **Sprite 0x67** (0x67) ×13: (2,45,22) (3,52,22) (3,57,22) (4,64,22) (4,76,22) (5,85,19) …
    - [ ] **Sprite 0xE9** (0xE9) ×2: (0,8,0) (11,184,0)

### Nivel 0x01B
- **Direcciones**: L1ptr 0x2E051 → header 0x36E36 · SprPtr 0x2EC36 → stream 0x3D95E · L2ptr 0x2E651 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 9 pantallas (144 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=1 backArea=5
- **Colisión**: 144×27 casillas · LEDGE_TOP=95 SOLID=73 SLOPE_STEEP=4
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (27)**:
    - [ ] **Sprite 0x32** (0x32) ×3: (1,26,23) (3,58,23) (6,96,23)
    - [ ] **DownFirstWoodenSpike** (0xAC) ×10: (1,19,16) (1,28,15) (2,39,17) (3,50,14) (3,61,19) (5,80,16) …
    - [ ] **UpDownFirstWoodenSpike** (0xAD) ×9: (1,19,23) (1,28,20) (4,73,22) (6,99,23) (6,100,20) (6,108,20) …
    - [ ] **BowserStatueFire** (0xB3) ×3: (3,62,22) (4,74,23) (5,94,22)
    - [ ] **Sprite 0xD8** (0xD8) ×1: (4,65,0)
    - [ ] **Sprite 0xD9** (0xD9) ×1: (7,126,0)

### Nivel 0x01C
- **Direcciones**: L1ptr 0x2E054 → header 0x386E3 · SprPtr 0x2EC38 → stream 0x3DB0F · L2ptr 0x2E654 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 17 pantallas (272 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=7 SPR=0 backArea=0
- **Colisión**: 272×27 casillas · LEDGE_TOP=154 SOLID=307 SLOPE=2 SLOPE_STEEP=12
- **Entrada**: casilla (1,14) = px (16,224)
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

### Nivel 0x01D
- **Direcciones**: L1ptr 0x2E057 → header 0x38100 · SprPtr 0x2EC3A → stream 0x3DA93 · L2ptr 0x2E657 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=4 backArea=3
- **Colisión**: 240×27 casillas · LEDGE_TOP=34 SOLID=830 SLOPE=427 SLOPE_STEEP=490
- **Entrada**: casilla (8,6) = px (128,96)
- **Usa sprites grandes**: sí — MegaMole (0xBF)
- **Enemigos (24)**:
    - [ ] **Sprite 0x78** (0x78) ×3: (6,109,19) (6,110,19) (7,112,19)
    - [ ] **GoalTape** (0x7B) ×1: (13,222,23)
    - [ ] **ClappinChuck** (0x95) ×1: (13,218,21)
    - [ ] **CarrotTopLiftUpperRight** (0xB7) ×12: (1,31,14) (3,49,10) (3,53,16) (4,75,15) (4,79,9) (5,85,3) …
    - [ ] **CarrotTopLiftUpperLeft** (0xB8) ×3: (2,47,20) (5,84,24) (5,89,23)
    - [B] **MegaMole** (0xBF) ×3: (10,170,21) (11,178,5) (11,188,21)
    - [ ] **GreyFallingPlatform** (0xC4) ×1: (6,96,23)

### Nivel 0x01E
- **Direcciones**: L1ptr 0x2E05A → header 0x30000 · SprPtr 0x2EC3C → stream 0x3E76D · L2ptr 0x2E65A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x01F
- **Direcciones**: L1ptr 0x2E05D → header 0x3620A · SprPtr 0x2EC3E → stream 0x3D648 · L2ptr 0x2E65D · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=7 SPR=1 backArea=4
- **Colisión**: 96×27 casillas · LEDGE_TOP=67 SOLID=34 SLOPE_STEEP=4
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (10)**:
    - [ ] **Sprite 0x67** (0x67) ×7: (1,25,21) (2,33,20) (2,43,21) (3,51,20) (4,67,20) (4,77,19) …
    - [ ] **Layer3Smasher** (0x89) ×1: (1,18,0)
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0)

### Nivel 0x020
- **Direcciones**: L1ptr 0x2E060 → header 0x359D9 · SprPtr 0x2EC40 → stream 0x3D4CD · L2ptr 0x2E660 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=3
- **Colisión**: 217×27 casillas · SOLID=53
- **Entrada**: casilla (8,14) = px (128,224)
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

### Nivel 0x021
- **Direcciones**: L1ptr 0x2E063 → header 0x367A2 · SprPtr 0x2EC42 → stream 0x3D74C · L2ptr 0x2E663 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 14 pantallas (224 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 218×27 casillas · SOLID=1 SLOPE=12 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (25)**:
    - [ ] **Sprite 0x38** (0x38) ×7: (1,24,23) (1,25,22) (5,80,18) (5,80,23) (8,140,20) (11,188,22) …
    - [ ] **Sprite 0x39** (0x39) ×7: (4,69,21) (4,70,21) (4,71,21) (7,118,22) (11,179,17) (11,179,22) …
    - [ ] **MovingLedgeHole** (0x52) ×8: (1,17,24) (3,58,24) (4,73,24) (5,90,24) (10,174,24) (11,189,24) …
    - [ ] **FishinBoo** (0xAE) ×1: (5,95,12)
    - [ ] **Sprite 0xDE** (0xDE) ×2: (3,59,21) (9,154,17)

### Nivel 0x022
- **Direcciones**: L1ptr 0x2E066 → header 0x36444 · SprPtr 0x2EC44 → stream 0x3D6D9 · L2ptr 0x2E666 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=190 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x023
- **Direcciones**: L1ptr 0x2E069 → header 0x36CC9 · SprPtr 0x2EC46 → stream 0x3D8BE · L2ptr 0x2E669 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 24 pantallas (384 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=1 SPR=2 backArea=1
- **Colisión**: 369×27 casillas · SOLID=99
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (50)**:
    - [s] **RedKoopaNoShell** (0x5) ×6: (2,35,16) (3,63,19) (5,87,23) (7,112,20) (8,142,17) (11,179,19)
    - [ ] **BlueKoopaNoShell** (0x6) ×11: (3,55,22) (4,74,21) (6,100,21) (7,121,20) (8,132,20) (9,152,15) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×3: (13,208,20) (13,208,18) (16,268,20)
    - [ ] **GoalTape** (0x7B) ×2: (18,302,12) (22,366,23)
    - [ ] **GreyChainedPlatform** (0xA3) ×22: (1,18,20) (2,35,17) (3,55,23) (3,63,20) (4,74,22) (6,100,22) …
    - [ ] **Sparky** (0xA5) ×6: (12,203,20) (14,230,20) (14,238,19) (15,247,22) (15,255,17) (17,273,20)

### Nivel 0x024
- **Direcciones**: L1ptr 0x2E06C → header 0x36897 · SprPtr 0x2EC48 → stream 0x3D7BF · L2ptr 0x2E66C · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=0 backArea=2
- **Colisión**: 64×27 casillas · LEDGE_TOP=98 SOLID=76 SLOPE=62 SLOPE_STEEP=68
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **PortableSpringboard** (0x2F) ×1: (0,14,19)
    - [ ] **DinoRhino** (0x6E) ×4: (1,17,23) (1,28,8) (1,29,15) (3,54,10)
    - [ ] **DinoTorch** (0x6F) ×6: (0,12,19) (1,27,23) (1,31,9) (2,47,8) (3,49,20) (3,51,14)
    - [ ] **MessageBox** (0xB9) ×1: (0,7,20)

### Nivel 0x025
- **Direcciones**: L1ptr 0x2E06F → header 0x30000 · SprPtr 0x2EC4A → stream 0x3E76D · L2ptr 0x2E66F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x026
- **Direcciones**: L1ptr 0x2E072 → header 0x30000 · SprPtr 0x2EC4C → stream 0x3E76D · L2ptr 0x2E672 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x027
- **Direcciones**: L1ptr 0x2E075 → header 0x30000 · SprPtr 0x2EC4E → stream 0x3E76D · L2ptr 0x2E675 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x028
- **Direcciones**: L1ptr 0x2E078 → header 0x30000 · SprPtr 0x2EC50 → stream 0x3E76D · L2ptr 0x2E678 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x029
- **Direcciones**: L1ptr 0x2E07B → header 0x30000 · SprPtr 0x2EC52 → stream 0x3E76D · L2ptr 0x2E67B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02A
- **Direcciones**: L1ptr 0x2E07E → header 0x30000 · SprPtr 0x2EC54 → stream 0x3E76D · L2ptr 0x2E67E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02B
- **Direcciones**: L1ptr 0x2E081 → header 0x30000 · SprPtr 0x2EC56 → stream 0x3E76D · L2ptr 0x2E681 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02C
- **Direcciones**: L1ptr 0x2E084 → header 0x30000 · SprPtr 0x2EC58 → stream 0x3E76D · L2ptr 0x2E684 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02D
- **Direcciones**: L1ptr 0x2E087 → header 0x30000 · SprPtr 0x2EC5A → stream 0x3E76D · L2ptr 0x2E687 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02E
- **Direcciones**: L1ptr 0x2E08A → header 0x30000 · SprPtr 0x2EC5C → stream 0x3E76D · L2ptr 0x2E68A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x02F
- **Direcciones**: L1ptr 0x2E08D → header 0x30000 · SprPtr 0x2EC5E → stream 0x3E76D · L2ptr 0x2E68D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x030
- **Direcciones**: L1ptr 0x2E090 → header 0x30000 · SprPtr 0x2EC60 → stream 0x3E76D · L2ptr 0x2E690 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x031
- **Direcciones**: L1ptr 0x2E093 → header 0x30000 · SprPtr 0x2EC62 → stream 0x3E76D · L2ptr 0x2E693 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x032
- **Direcciones**: L1ptr 0x2E096 → header 0x30000 · SprPtr 0x2EC64 → stream 0x3E76D · L2ptr 0x2E696 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x033
- **Direcciones**: L1ptr 0x2E099 → header 0x30000 · SprPtr 0x2EC66 → stream 0x3E76D · L2ptr 0x2E699 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x034
- **Direcciones**: L1ptr 0x2E09C → header 0x30000 · SprPtr 0x2EC68 → stream 0x3E76D · L2ptr 0x2E69C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x035
- **Direcciones**: L1ptr 0x2E09F → header 0x30000 · SprPtr 0x2EC6A → stream 0x3E76D · L2ptr 0x2E69F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x036
- **Direcciones**: L1ptr 0x2E0A2 → header 0x30000 · SprPtr 0x2EC6C → stream 0x3E76D · L2ptr 0x2E6A2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x037
- **Direcciones**: L1ptr 0x2E0A5 → header 0x30000 · SprPtr 0x2EC6E → stream 0x3E76D · L2ptr 0x2E6A5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x038
- **Direcciones**: L1ptr 0x2E0A8 → header 0x30000 · SprPtr 0x2EC70 → stream 0x3E76D · L2ptr 0x2E6A8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x039
- **Direcciones**: L1ptr 0x2E0AB → header 0x30000 · SprPtr 0x2EC72 → stream 0x3E76D · L2ptr 0x2E6AB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03A
- **Direcciones**: L1ptr 0x2E0AE → header 0x30000 · SprPtr 0x2EC74 → stream 0x3E76D · L2ptr 0x2E6AE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03B
- **Direcciones**: L1ptr 0x2E0B1 → header 0x30000 · SprPtr 0x2EC76 → stream 0x3E76D · L2ptr 0x2E6B1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03C
- **Direcciones**: L1ptr 0x2E0B4 → header 0x30000 · SprPtr 0x2EC78 → stream 0x3E76D · L2ptr 0x2E6B4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03D
- **Direcciones**: L1ptr 0x2E0B7 → header 0x30000 · SprPtr 0x2EC7A → stream 0x3E76D · L2ptr 0x2E6B7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03E
- **Direcciones**: L1ptr 0x2E0BA → header 0x30000 · SprPtr 0x2EC7C → stream 0x3E76D · L2ptr 0x2E6BA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x03F
- **Direcciones**: L1ptr 0x2E0BD → header 0x30000 · SprPtr 0x2EC7E → stream 0x3E76D · L2ptr 0x2E6BD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x040
- **Direcciones**: L1ptr 0x2E0C0 → header 0x30000 · SprPtr 0x2EC80 → stream 0x3E76D · L2ptr 0x2E6C0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x041
- **Direcciones**: L1ptr 0x2E0C3 → header 0x30000 · SprPtr 0x2EC82 → stream 0x3E76D · L2ptr 0x2E6C3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x042
- **Direcciones**: L1ptr 0x2E0C6 → header 0x30000 · SprPtr 0x2EC84 → stream 0x3E76D · L2ptr 0x2E6C6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x043
- **Direcciones**: L1ptr 0x2E0C9 → header 0x30000 · SprPtr 0x2EC86 → stream 0x3E76D · L2ptr 0x2E6C9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x044
- **Direcciones**: L1ptr 0x2E0CC → header 0x30000 · SprPtr 0x2EC88 → stream 0x3E76D · L2ptr 0x2E6CC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x045
- **Direcciones**: L1ptr 0x2E0CF → header 0x30000 · SprPtr 0x2EC8A → stream 0x3E76D · L2ptr 0x2E6CF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x046
- **Direcciones**: L1ptr 0x2E0D2 → header 0x30000 · SprPtr 0x2EC8C → stream 0x3E76D · L2ptr 0x2E6D2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x047
- **Direcciones**: L1ptr 0x2E0D5 → header 0x30000 · SprPtr 0x2EC8E → stream 0x3E76D · L2ptr 0x2E6D5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x048
- **Direcciones**: L1ptr 0x2E0D8 → header 0x30000 · SprPtr 0x2EC90 → stream 0x3E76D · L2ptr 0x2E6D8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x049
- **Direcciones**: L1ptr 0x2E0DB → header 0x30000 · SprPtr 0x2EC92 → stream 0x3E76D · L2ptr 0x2E6DB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04A
- **Direcciones**: L1ptr 0x2E0DE → header 0x30000 · SprPtr 0x2EC94 → stream 0x3E76D · L2ptr 0x2E6DE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04B
- **Direcciones**: L1ptr 0x2E0E1 → header 0x30000 · SprPtr 0x2EC96 → stream 0x3E76D · L2ptr 0x2E6E1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04C
- **Direcciones**: L1ptr 0x2E0E4 → header 0x30000 · SprPtr 0x2EC98 → stream 0x3E76D · L2ptr 0x2E6E4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04D
- **Direcciones**: L1ptr 0x2E0E7 → header 0x30000 · SprPtr 0x2EC9A → stream 0x3E76D · L2ptr 0x2E6E7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04E
- **Direcciones**: L1ptr 0x2E0EA → header 0x30000 · SprPtr 0x2EC9C → stream 0x3E76D · L2ptr 0x2E6EA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x04F
- **Direcciones**: L1ptr 0x2E0ED → header 0x30000 · SprPtr 0x2EC9E → stream 0x3E76D · L2ptr 0x2E6ED · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x050
- **Direcciones**: L1ptr 0x2E0F0 → header 0x30000 · SprPtr 0x2ECA0 → stream 0x3E76D · L2ptr 0x2E6F0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x051
- **Direcciones**: L1ptr 0x2E0F3 → header 0x30000 · SprPtr 0x2ECA2 → stream 0x3E76D · L2ptr 0x2E6F3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x052
- **Direcciones**: L1ptr 0x2E0F6 → header 0x30000 · SprPtr 0x2ECA4 → stream 0x3E76D · L2ptr 0x2E6F6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x053
- **Direcciones**: L1ptr 0x2E0F9 → header 0x30000 · SprPtr 0x2ECA6 → stream 0x3E76D · L2ptr 0x2E6F9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x054
- **Direcciones**: L1ptr 0x2E0FC → header 0x30000 · SprPtr 0x2ECA8 → stream 0x3E76D · L2ptr 0x2E6FC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x055
- **Direcciones**: L1ptr 0x2E0FF → header 0x30000 · SprPtr 0x2ECAA → stream 0x3E76D · L2ptr 0x2E6FF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x056
- **Direcciones**: L1ptr 0x2E102 → header 0x30000 · SprPtr 0x2ECAC → stream 0x3E76D · L2ptr 0x2E702 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x057
- **Direcciones**: L1ptr 0x2E105 → header 0x30000 · SprPtr 0x2ECAE → stream 0x3E76D · L2ptr 0x2E705 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x058
- **Direcciones**: L1ptr 0x2E108 → header 0x30000 · SprPtr 0x2ECB0 → stream 0x3E76D · L2ptr 0x2E708 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x059
- **Direcciones**: L1ptr 0x2E10B → header 0x30000 · SprPtr 0x2ECB2 → stream 0x3E76D · L2ptr 0x2E70B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05A
- **Direcciones**: L1ptr 0x2E10E → header 0x30000 · SprPtr 0x2ECB4 → stream 0x3E76D · L2ptr 0x2E70E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05B
- **Direcciones**: L1ptr 0x2E111 → header 0x30000 · SprPtr 0x2ECB6 → stream 0x3E76D · L2ptr 0x2E711 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05C
- **Direcciones**: L1ptr 0x2E114 → header 0x30000 · SprPtr 0x2ECB8 → stream 0x3E76D · L2ptr 0x2E714 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05D
- **Direcciones**: L1ptr 0x2E117 → header 0x30000 · SprPtr 0x2ECBA → stream 0x3E76D · L2ptr 0x2E717 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05E
- **Direcciones**: L1ptr 0x2E11A → header 0x30000 · SprPtr 0x2ECBC → stream 0x3E76D · L2ptr 0x2E71A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x05F
- **Direcciones**: L1ptr 0x2E11D → header 0x30000 · SprPtr 0x2ECBE → stream 0x3E76D · L2ptr 0x2E71D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x060
- **Direcciones**: L1ptr 0x2E120 → header 0x30000 · SprPtr 0x2ECC0 → stream 0x3E76D · L2ptr 0x2E720 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x061
- **Direcciones**: L1ptr 0x2E123 → header 0x30000 · SprPtr 0x2ECC2 → stream 0x3E76D · L2ptr 0x2E723 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x062
- **Direcciones**: L1ptr 0x2E126 → header 0x30000 · SprPtr 0x2ECC4 → stream 0x3E76D · L2ptr 0x2E726 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x063
- **Direcciones**: L1ptr 0x2E129 → header 0x30000 · SprPtr 0x2ECC6 → stream 0x3E76D · L2ptr 0x2E729 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x064
- **Direcciones**: L1ptr 0x2E12C → header 0x30000 · SprPtr 0x2ECC8 → stream 0x3E76D · L2ptr 0x2E72C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x065
- **Direcciones**: L1ptr 0x2E12F → header 0x30000 · SprPtr 0x2ECCA → stream 0x3E76D · L2ptr 0x2E72F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x066
- **Direcciones**: L1ptr 0x2E132 → header 0x30000 · SprPtr 0x2ECCC → stream 0x3E76D · L2ptr 0x2E732 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x067
- **Direcciones**: L1ptr 0x2E135 → header 0x30000 · SprPtr 0x2ECCE → stream 0x3E76D · L2ptr 0x2E735 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x068
- **Direcciones**: L1ptr 0x2E138 → header 0x30000 · SprPtr 0x2ECD0 → stream 0x3E76D · L2ptr 0x2E738 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x069
- **Direcciones**: L1ptr 0x2E13B → header 0x30000 · SprPtr 0x2ECD2 → stream 0x3E76D · L2ptr 0x2E73B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06A
- **Direcciones**: L1ptr 0x2E13E → header 0x30000 · SprPtr 0x2ECD4 → stream 0x3E76D · L2ptr 0x2E73E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06B
- **Direcciones**: L1ptr 0x2E141 → header 0x30000 · SprPtr 0x2ECD6 → stream 0x3E76D · L2ptr 0x2E741 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06C
- **Direcciones**: L1ptr 0x2E144 → header 0x30000 · SprPtr 0x2ECD8 → stream 0x3E76D · L2ptr 0x2E744 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06D
- **Direcciones**: L1ptr 0x2E147 → header 0x30000 · SprPtr 0x2ECDA → stream 0x3E76D · L2ptr 0x2E747 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06E
- **Direcciones**: L1ptr 0x2E14A → header 0x30000 · SprPtr 0x2ECDC → stream 0x3E76D · L2ptr 0x2E74A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x06F
- **Direcciones**: L1ptr 0x2E14D → header 0x30000 · SprPtr 0x2ECDE → stream 0x3E76D · L2ptr 0x2E74D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x070
- **Direcciones**: L1ptr 0x2E150 → header 0x30000 · SprPtr 0x2ECE0 → stream 0x3E76D · L2ptr 0x2E750 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x071
- **Direcciones**: L1ptr 0x2E153 → header 0x30000 · SprPtr 0x2ECE2 → stream 0x3E76D · L2ptr 0x2E753 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x072
- **Direcciones**: L1ptr 0x2E156 → header 0x30000 · SprPtr 0x2ECE4 → stream 0x3E76D · L2ptr 0x2E756 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x073
- **Direcciones**: L1ptr 0x2E159 → header 0x30000 · SprPtr 0x2ECE6 → stream 0x3E76D · L2ptr 0x2E759 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x074
- **Direcciones**: L1ptr 0x2E15C → header 0x30000 · SprPtr 0x2ECE8 → stream 0x3E76D · L2ptr 0x2E75C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x075
- **Direcciones**: L1ptr 0x2E15F → header 0x30000 · SprPtr 0x2ECEA → stream 0x3E76D · L2ptr 0x2E75F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x076
- **Direcciones**: L1ptr 0x2E162 → header 0x30000 · SprPtr 0x2ECEC → stream 0x3E76D · L2ptr 0x2E762 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x077
- **Direcciones**: L1ptr 0x2E165 → header 0x30000 · SprPtr 0x2ECEE → stream 0x3E76D · L2ptr 0x2E765 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x078
- **Direcciones**: L1ptr 0x2E168 → header 0x30000 · SprPtr 0x2ECF0 → stream 0x3E76D · L2ptr 0x2E768 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x079
- **Direcciones**: L1ptr 0x2E16B → header 0x30000 · SprPtr 0x2ECF2 → stream 0x3E76D · L2ptr 0x2E76B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07A
- **Direcciones**: L1ptr 0x2E16E → header 0x30000 · SprPtr 0x2ECF4 → stream 0x3E76D · L2ptr 0x2E76E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07B
- **Direcciones**: L1ptr 0x2E171 → header 0x30000 · SprPtr 0x2ECF6 → stream 0x3E76D · L2ptr 0x2E771 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07C
- **Direcciones**: L1ptr 0x2E174 → header 0x30000 · SprPtr 0x2ECF8 → stream 0x3E76D · L2ptr 0x2E774 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07D
- **Direcciones**: L1ptr 0x2E177 → header 0x30000 · SprPtr 0x2ECFA → stream 0x3E76D · L2ptr 0x2E777 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07E
- **Direcciones**: L1ptr 0x2E17A → header 0x30000 · SprPtr 0x2ECFC → stream 0x3E76D · L2ptr 0x2E77A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x07F
- **Direcciones**: L1ptr 0x2E17D → header 0x30000 · SprPtr 0x2ECFE → stream 0x3E76D · L2ptr 0x2E77D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x080
- **Direcciones**: L1ptr 0x2E180 → header 0x30000 · SprPtr 0x2ED00 → stream 0x3E76D · L2ptr 0x2E780 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x081
- **Direcciones**: L1ptr 0x2E183 → header 0x30000 · SprPtr 0x2ED02 → stream 0x3E76D · L2ptr 0x2E783 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x082
- **Direcciones**: L1ptr 0x2E186 → header 0x30000 · SprPtr 0x2ED04 → stream 0x3E76D · L2ptr 0x2E786 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x083
- **Direcciones**: L1ptr 0x2E189 → header 0x30000 · SprPtr 0x2ED06 → stream 0x3E76D · L2ptr 0x2E789 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x084
- **Direcciones**: L1ptr 0x2E18C → header 0x30000 · SprPtr 0x2ED08 → stream 0x3E76D · L2ptr 0x2E78C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x085
- **Direcciones**: L1ptr 0x2E18F → header 0x30000 · SprPtr 0x2ED0A → stream 0x3E76D · L2ptr 0x2E78F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x086
- **Direcciones**: L1ptr 0x2E192 → header 0x30000 · SprPtr 0x2ED0C → stream 0x3E76D · L2ptr 0x2E792 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x087
- **Direcciones**: L1ptr 0x2E195 → header 0x30000 · SprPtr 0x2ED0E → stream 0x3E76D · L2ptr 0x2E795 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x088
- **Direcciones**: L1ptr 0x2E198 → header 0x30000 · SprPtr 0x2ED10 → stream 0x3E76D · L2ptr 0x2E798 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x089
- **Direcciones**: L1ptr 0x2E19B → header 0x30000 · SprPtr 0x2ED12 → stream 0x3E76D · L2ptr 0x2E79B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08A
- **Direcciones**: L1ptr 0x2E19E → header 0x30000 · SprPtr 0x2ED14 → stream 0x3E76D · L2ptr 0x2E79E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08B
- **Direcciones**: L1ptr 0x2E1A1 → header 0x30000 · SprPtr 0x2ED16 → stream 0x3E76D · L2ptr 0x2E7A1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08C
- **Direcciones**: L1ptr 0x2E1A4 → header 0x30000 · SprPtr 0x2ED18 → stream 0x3E76D · L2ptr 0x2E7A4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08D
- **Direcciones**: L1ptr 0x2E1A7 → header 0x30000 · SprPtr 0x2ED1A → stream 0x3E76D · L2ptr 0x2E7A7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08E
- **Direcciones**: L1ptr 0x2E1AA → header 0x30000 · SprPtr 0x2ED1C → stream 0x3E76D · L2ptr 0x2E7AA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x08F
- **Direcciones**: L1ptr 0x2E1AD → header 0x30000 · SprPtr 0x2ED1E → stream 0x3E76D · L2ptr 0x2E7AD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x090
- **Direcciones**: L1ptr 0x2E1B0 → header 0x30000 · SprPtr 0x2ED20 → stream 0x3E76D · L2ptr 0x2E7B0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x091
- **Direcciones**: L1ptr 0x2E1B3 → header 0x30000 · SprPtr 0x2ED22 → stream 0x3E76D · L2ptr 0x2E7B3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x092
- **Direcciones**: L1ptr 0x2E1B6 → header 0x30000 · SprPtr 0x2ED24 → stream 0x3E76D · L2ptr 0x2E7B6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x093
- **Direcciones**: L1ptr 0x2E1B9 → header 0x30561 · SprPtr 0x2ED26 → stream 0x3C3DB · L2ptr 0x2E7B9 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) = px (128,224)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,5)
    - [ ] **Sprite 0xB6** (0xB6) ×1: (0,1,16)

### Nivel 0x094
- **Direcciones**: L1ptr 0x2E1BC → header 0x3058B · SprPtr 0x2ED28 → stream 0x3C3E3 · L2ptr 0x2E7BC · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) = px (128,224)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,6)
    - [ ] **Sprite 0xB6** (0xB6) ×2: (0,1,16) (0,14,16)

### Nivel 0x095
- **Direcciones**: L1ptr 0x2E1BF → header 0x30258 · SprPtr 0x2ED2A → stream 0x3C367 · L2ptr 0x2E7BF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x096
- **Direcciones**: L1ptr 0x2E1C2 → header 0x3025E · SprPtr 0x2ED2C → stream 0x3C359 · L2ptr 0x2E7C2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,4)
    - [ ] **Podoboo** (0x33) ×3: (0,2,0) (0,7,0) (0,13,0)

### Nivel 0x097
- **Direcciones**: L1ptr 0x2E1C5 → header 0x3025E · SprPtr 0x2ED2E → stream 0x3C354 · L2ptr 0x2E7C5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,3)

### Nivel 0x098
- **Direcciones**: L1ptr 0x2E1C8 → header 0x30258 · SprPtr 0x2ED30 → stream 0x3C34F · L2ptr 0x2E7C8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,2)

### Nivel 0x099
- **Direcciones**: L1ptr 0x2E1CB → header 0x30258 · SprPtr 0x2ED32 → stream 0x3C34A · L2ptr 0x2E7CB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,1)

### Nivel 0x09A
- **Direcciones**: L1ptr 0x2E1CE → header 0x30258 · SprPtr 0x2ED34 → stream 0x3C345 · L2ptr 0x2E7CE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,0)

### Nivel 0x09B
- **Direcciones**: L1ptr 0x2E1D1 → header 0x30252 · SprPtr 0x2ED36 → stream 0x3C340 · L2ptr 0x2E7D1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x10 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **ActivateBowserBattle** (0xA0) ×1: (0,0,0)

### Nivel 0x09C
- **Direcciones**: L1ptr 0x2E1D4 → header 0x30000 · SprPtr 0x2ED38 → stream 0x3E76D · L2ptr 0x2E7D4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x09D
- **Direcciones**: L1ptr 0x2E1D7 → header 0x30000 · SprPtr 0x2ED3A → stream 0x3E76D · L2ptr 0x2E7D7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x09E
- **Direcciones**: L1ptr 0x2E1DA → header 0x30000 · SprPtr 0x2ED3C → stream 0x3E76D · L2ptr 0x2E7DA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x09F
- **Direcciones**: L1ptr 0x2E1DD → header 0x30000 · SprPtr 0x2ED3E → stream 0x3E76D · L2ptr 0x2E7DD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A0
- **Direcciones**: L1ptr 0x2E1E0 → header 0x30000 · SprPtr 0x2ED40 → stream 0x3E76D · L2ptr 0x2E7E0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A1
- **Direcciones**: L1ptr 0x2E1E3 → header 0x30000 · SprPtr 0x2ED42 → stream 0x3E76D · L2ptr 0x2E7E3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A2
- **Direcciones**: L1ptr 0x2E1E6 → header 0x30000 · SprPtr 0x2ED44 → stream 0x3E76D · L2ptr 0x2E7E6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A3
- **Direcciones**: L1ptr 0x2E1E9 → header 0x30000 · SprPtr 0x2ED46 → stream 0x3E76D · L2ptr 0x2E7E9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A4
- **Direcciones**: L1ptr 0x2E1EC → header 0x30000 · SprPtr 0x2ED48 → stream 0x3E76D · L2ptr 0x2E7EC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A5
- **Direcciones**: L1ptr 0x2E1EF → header 0x30000 · SprPtr 0x2ED4A → stream 0x3E76D · L2ptr 0x2E7EF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A6
- **Direcciones**: L1ptr 0x2E1F2 → header 0x30000 · SprPtr 0x2ED4C → stream 0x3E76D · L2ptr 0x2E7F2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A7
- **Direcciones**: L1ptr 0x2E1F5 → header 0x30000 · SprPtr 0x2ED4E → stream 0x3E76D · L2ptr 0x2E7F5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A8
- **Direcciones**: L1ptr 0x2E1F8 → header 0x30000 · SprPtr 0x2ED50 → stream 0x3E76D · L2ptr 0x2E7F8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0A9
- **Direcciones**: L1ptr 0x2E1FB → header 0x30000 · SprPtr 0x2ED52 → stream 0x3E76D · L2ptr 0x2E7FB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AA
- **Direcciones**: L1ptr 0x2E1FE → header 0x30000 · SprPtr 0x2ED54 → stream 0x3E76D · L2ptr 0x2E7FE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AB
- **Direcciones**: L1ptr 0x2E201 → header 0x30000 · SprPtr 0x2ED56 → stream 0x3E76D · L2ptr 0x2E801 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AC
- **Direcciones**: L1ptr 0x2E204 → header 0x30000 · SprPtr 0x2ED58 → stream 0x3E76D · L2ptr 0x2E804 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AD
- **Direcciones**: L1ptr 0x2E207 → header 0x30000 · SprPtr 0x2ED5A → stream 0x3E76D · L2ptr 0x2E807 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AE
- **Direcciones**: L1ptr 0x2E20A → header 0x30000 · SprPtr 0x2ED5C → stream 0x3E76D · L2ptr 0x2E80A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0AF
- **Direcciones**: L1ptr 0x2E20D → header 0x30000 · SprPtr 0x2ED5E → stream 0x3E76D · L2ptr 0x2E80D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B0
- **Direcciones**: L1ptr 0x2E210 → header 0x30000 · SprPtr 0x2ED60 → stream 0x3E76D · L2ptr 0x2E810 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B1
- **Direcciones**: L1ptr 0x2E213 → header 0x30000 · SprPtr 0x2ED62 → stream 0x3E76D · L2ptr 0x2E813 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B2
- **Direcciones**: L1ptr 0x2E216 → header 0x30000 · SprPtr 0x2ED64 → stream 0x3E76D · L2ptr 0x2E816 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B3
- **Direcciones**: L1ptr 0x2E219 → header 0x30000 · SprPtr 0x2ED66 → stream 0x3E76D · L2ptr 0x2E819 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B4
- **Direcciones**: L1ptr 0x2E21C → header 0x30000 · SprPtr 0x2ED68 → stream 0x3E76D · L2ptr 0x2E81C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B5
- **Direcciones**: L1ptr 0x2E21F → header 0x30000 · SprPtr 0x2ED6A → stream 0x3E76D · L2ptr 0x2E81F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B6
- **Direcciones**: L1ptr 0x2E222 → header 0x30000 · SprPtr 0x2ED6C → stream 0x3E76D · L2ptr 0x2E822 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B7
- **Direcciones**: L1ptr 0x2E225 → header 0x30000 · SprPtr 0x2ED6E → stream 0x3E76D · L2ptr 0x2E825 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B8
- **Direcciones**: L1ptr 0x2E228 → header 0x30000 · SprPtr 0x2ED70 → stream 0x3E76D · L2ptr 0x2E828 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0B9
- **Direcciones**: L1ptr 0x2E22B → header 0x30000 · SprPtr 0x2ED72 → stream 0x3E76D · L2ptr 0x2E82B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BA
- **Direcciones**: L1ptr 0x2E22E → header 0x30000 · SprPtr 0x2ED74 → stream 0x3E76D · L2ptr 0x2E82E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BB
- **Direcciones**: L1ptr 0x2E231 → header 0x30000 · SprPtr 0x2ED76 → stream 0x3E76D · L2ptr 0x2E831 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BC
- **Direcciones**: L1ptr 0x2E234 → header 0x30000 · SprPtr 0x2ED78 → stream 0x3E76D · L2ptr 0x2E834 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x0BE
- **Direcciones**: L1ptr 0x2E23A → header 0x3676E · SprPtr 0x2ED7C → stream 0x3D741 · L2ptr 0x2E83A · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=1 SPR=3 backArea=6
- **Colisión**: 63×27 casillas · SOLID=77
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [ ] **PorcuPuffer** (0xC3) ×2: (0,14,24) (2,33,24)
    - [ ] **Sprite 0xCF** (0xCF) ×1: (0,4,0)

### Nivel 0x0BF
- **Direcciones**: L1ptr 0x2E23D → header 0x34199 · SprPtr 0x2ED7E → stream 0x3D02F · L2ptr 0x2E83D · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=0
- **Colisión**: 63×27 casillas · SOLID=80
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Sprite 0x55** (0x55) ×1: (3,53,23)
    - [ ] **VerticalCheckerboardPlatform** (0x57) ×4: (1,21,23) (1,28,20) (2,35,24) (2,42,21)
    - [ ] **Sprite 0xD7** (0xD7) ×1: (0,8,0)

### Nivel 0x0C0
- **Direcciones**: L1ptr 0x2E240 → header 0x388CB · SprPtr 0x2ED80 → stream 0x3DB95 · L2ptr 0x2E840 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=7 SPR=0 backArea=0
- **Colisión**: 64×27 casillas · LEDGE_TOP=32 SOLID=70 SLOPE_STEEP=1
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **BubbleWithSprite** (0x9D) ×12: (1,18,18) (1,22,19) (1,27,18) (1,30,16) (2,34,20) (2,38,18) …

### Nivel 0x0C1
- **Direcciones**: L1ptr 0x2E243 → header 0x34375 · SprPtr 0x2ED82 → stream 0x3D0CF · L2ptr 0x2E843 · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=1 SPR=3 backArea=2
- **Colisión**: 52×27 casillas · SOLID=48
- **Entrada**: casilla (1,3) = px (16,48)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **PorcuPuffer** (0xC3) ×2: (1,17,24) (2,34,24)

### Nivel 0x0C2
- **Direcciones**: L1ptr 0x2E246 → header 0x32270 · SprPtr 0x2ED84 → stream 0x3C9AA · L2ptr 0x2E846 · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xA · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,25) = px (16,400)
- **Usa sprites grandes**: no
- **Enemigos (10)**:
    - [ ] **BobOmb** (0xB) ×10: (0,15,23) (0,15,14) (1,26,29) (1,26,19) (1,26,11) (2,41,23) …

### Nivel 0x0C3
- **Direcciones**: L1ptr 0x2E249 → header 0x31D83 · SprPtr 0x2ED86 → stream 0x3C8EA · L2ptr 0x2E849 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 64×27 casillas · LEDGE_TOP=26 SOLID=18
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×2: (1,27,21) (2,35,19)
    - [ ] **ShiftingPipe** (0x49) ×6: (0,13,22) (1,17,21) (1,24,22) (1,29,21) (2,37,19) (2,42,23)

### Nivel 0x0C4
- **Direcciones**: L1ptr 0x2E24C → header 0x3194F · SprPtr 0x2ED88 → stream 0x3C3F5 · L2ptr 0x2E84C · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=4 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0C5
- **Direcciones**: L1ptr 0x2E24F → header 0x30603 · SprPtr 0x2ED8A → stream 0x3C441 · L2ptr 0x2E84F · GFXslot 0x028E7 · FGBGslot 0x02947
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 15` (tilesetFG=7)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=0
- **Colisión**: 16×27 casillas · LEDGE_TOP=16
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **DisplayMessage** (0x19) ×1: (0,0,0)

### Nivel 0x0C6
- **Direcciones**: L1ptr 0x2E252 → header 0x34949 · SprPtr 0x2ED8C → stream 0x3C3F0 · L2ptr 0x2E852 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=26 SOLID=17 SLOPE_STEEP=1
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x0C7
- **Direcciones**: L1ptr 0x2E255 → header 0x305B5 · SprPtr 0x2ED8E → stream 0x3C427 · L2ptr 0x2E855 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=2 FG=0 SPR=0 backArea=6
- **Colisión**: 128×27 casillas · LEDGE_TOP=124 SOLID=1 SLOPE=11 SLOPE_STEEP=10
- **Entrada**: casilla (1,21) = px (16,336)
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
- **Direcciones**: L1ptr 0x2E258 → header 0x3977C · SprPtr 0x2ED90 → stream 0x3DDCF · L2ptr 0x2E858 · GFXslot 0x028CB · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=2 backArea=5
- **Colisión**: 95×27 casillas · SOLID=65
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (16)**:
    - [ ] **Sparky** (0xA5) ×15: (1,23,6) (1,25,11) (1,31,7) (2,36,10) (2,43,16) (2,47,16) …
    - [ ] **Sprite 0xE8** (0xE8) ×1: (0,8,1)

### Nivel 0x0C9
- **Direcciones**: L1ptr 0x2E25B → header 0x3087D · SprPtr 0x2ED92 → stream 0x3C4C0 · L2ptr 0x2E85B · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x0CA
- **Direcciones**: L1ptr 0x2E25E → header 0x307AE · SprPtr 0x2ED94 → stream 0x3C44B · L2ptr 0x2E85E · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x0CB
- **Direcciones**: L1ptr 0x2E261 → header 0x33CEE · SprPtr 0x2ED96 → stream 0x3C3F0 · L2ptr 0x2E861 · GFXslot 0x028D3 · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=3 backArea=2
- **Colisión**: 17×27 casillas · SOLID=4
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x0CC
- **Direcciones**: L1ptr 0x2E264 → header 0x30636 · SprPtr 0x2ED98 → stream 0x3D51D · L2ptr 0x2E864 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,1)

### Nivel 0x0CD
- **Direcciones**: L1ptr 0x2E267 → header 0x36C24 · SprPtr 0x2ED9A → stream 0x3D899 · L2ptr 0x2E867 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=0 SPR=0 backArea=6
- **Colisión**: 64×27 casillas · LEDGE_TOP=33 SOLID=34
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **PSwitch** (0x3E) ×1: (0,8,23)
    - [ ] **GoalTape** (0x7B) ×1: (2,46,23)

### Nivel 0x0CE
- **Direcciones**: L1ptr 0x2E26A → header 0x36B0B · SprPtr 0x2ED9C → stream 0x3D84B · L2ptr 0x2E86A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=0 backArea=2
- **Colisión**: 64×27 casillas · LEDGE_TOP=23 SOLID=122 SLOPE_STEEP=3
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **BubbleWithSprite** (0x9D) ×11: (1,19,17) (1,23,19) (1,27,16) (1,27,20) (2,35,18) (2,39,17) …

### Nivel 0x0CF
- **Direcciones**: L1ptr 0x2E26D → header 0x36985 · SprPtr 0x2ED9E → stream 0x3D7E5 · L2ptr 0x2E86D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=4 SPR=0 backArea=6
- **Colisión**: 112×27 casillas · LEDGE_TOP=21 SOLID=136 SLOPE=1 SLOPE_STEEP=3
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Feather** (0x77) ×1: (0,4,23)

### Nivel 0x0D0
- **Direcciones**: L1ptr 0x2E270 → header 0x36444 · SprPtr 0x2EDA0 → stream 0x3D6D9 · L2ptr 0x2E870 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=190 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (8,19) = px (128,304)
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
- **Direcciones**: L1ptr 0x2E273 → header 0x36444 · SprPtr 0x2EDA2 → stream 0x3D6D9 · L2ptr 0x2E873 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=190 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (1,17) = px (16,272)
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
- **Direcciones**: L1ptr 0x2E276 → header 0x31D4C · SprPtr 0x2EDA4 → stream 0x3C8CD · L2ptr 0x2E876 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=21 SOLID=53 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (9)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×9: (0,10,23) (0,10,21) (1,16,23) (1,16,21) (1,21,22) (1,22,22) …

### Nivel 0x0D3
- **Direcciones**: L1ptr 0x2E279 → header 0x38BEA · SprPtr 0x2EDA6 → stream 0x3DC22 · L2ptr 0x2E879 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) = px (128,224)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,6)
    - [ ] **Sprite 0xB6** (0xB6) ×2: (0,1,16) (0,14,16)

### Nivel 0x0D4
- **Direcciones**: L1ptr 0x2E27C → header 0x38B4A · SprPtr 0x2EDA8 → stream 0x3DBF9 · L2ptr 0x2E87C · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (13)**:
    - [ ] **ThrowingDryBones** (0x30) ×1: (6,104,12)
    - [ ] **Sparky** (0xA5) ×5: (1,22,20) (3,62,19) (5,84,17) (6,108,14) (6,109,23)
    - [ ] **Sprite 0xA6** (0xA6) ×4: (2,38,19) (4,68,19) (5,93,19) (6,109,19)
    - [ ] **MovingCastleStone** (0xBB) ×2: (5,82,19) (5,89,23)
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,0)

### Nivel 0x0D5
- **Direcciones**: L1ptr 0x2E27F → header 0x30636 · SprPtr 0x2EDAA → stream 0x3C414 · L2ptr 0x2E87F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x0D6
- **Direcciones**: L1ptr 0x2E282 → header 0x36307 · SprPtr 0x2EDAC → stream 0x3D668 · L2ptr 0x2E882 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=4
- **Colisión**: 256×27 casillas · LEDGE_TOP=115 SOLID=147 SLOPE=2 SLOPE_STEEP=11
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (37)**:
    - [ ] **Podoboo** (0x33) ×26: (1,21,19) (1,21,15) (3,51,11) (3,54,11) (4,68,14) (4,73,13) …
    - [ ] **NonLineGuideGrinder** (0xB4) ×11: (1,16,14) (2,33,23) (2,36,23) (4,72,22) (4,79,22) (5,86,22) …

### Nivel 0x0D7
- **Direcciones**: L1ptr 0x2E285 → header 0x36DE7 · SprPtr 0x2EDAE → stream 0x3D956 · L2ptr 0x2E885 · GFXslot 0x028CB · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 7 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=5 SPR=2 backArea=0
- **Colisión**: 32×27 casillas · LEDGE_TOP=17 SOLID=55 SLOPE=10 SLOPE_STEEP=10
- **Entrada**: casilla (1,6) = px (16,96)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Sprite 0x55** (0x55) ×1: (0,7,5)
    - [ ] **BrownChainedPlatform** (0x5F) ×1: (0,13,5)

### Nivel 0x0D8
- **Direcciones**: L1ptr 0x2E288 → header 0x33BC9 · SprPtr 0x2EDB0 → stream 0x3CEBA · L2ptr 0x2E888 · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 71×27 casillas · LEDGE_TOP=14 SOLID=146 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **PSwitch** (0x3E) ×1: (3,60,23)

### Nivel 0x0D9
- **Direcciones**: L1ptr 0x2E28B → header 0x30636 · SprPtr 0x2EDB2 → stream 0x3D152 · L2ptr 0x2E88B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,2)

### Nivel 0x0DB
- **Direcciones**: L1ptr 0x2E291 → header 0x34559 · SprPtr 0x2EDB6 → stream 0x3D111 · L2ptr 0x2E891 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xA · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,23) = px (128,368)
- **Usa sprites grandes**: no
- **Enemigos (21)**:
    - [ ] **Sprite 0x22** (0x22) ×6: (1,29,4) (2,33,20) (2,38,26) (2,38,10) (2,43,20) (3,52,6)
    - [ ] **Sprite 0x23** (0x23) ×3: (1,29,7) (2,39,7) (3,52,9)
    - [ ] **Grinder** (0x24) ×11: (0,13,3) (1,16,9) (1,19,9) (1,29,12) (2,33,26) (2,33,12) …
    - [ ] **Sprite 0x25** (0x25) ×1: (1,24,3)

### Nivel 0x0DC
- **Direcciones**: L1ptr 0x2E294 → header 0x34495 · SprPtr 0x2EDB8 → stream 0x3D0F4 · L2ptr 0x2E894 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 176×27 casillas · LEDGE_TOP=146 SOLID=39
- **Entrada**: casilla (8,17) = px (128,272)
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (9)**:
    - [B] **BonyBeetle** (0x31) ×2: (6,99,23) (6,108,23)
    - [ ] **Podoboo** (0x33) ×6: (4,76,17) (5,82,17) (5,88,17) (5,94,17) (7,123,17) (8,132,17)
    - [ ] **Sprite 0xF2** (0xF2) ×1: (9,152,0)

### Nivel 0x0DD
- **Direcciones**: L1ptr 0x2E297 → header 0x351D6 · SprPtr 0x2EDBA → stream 0x3D304 · L2ptr 0x2E897 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 7 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=7
- **Colisión**: 64×27 casillas · SOLID=40
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Sprite 0x63** (0x63) ×1: (0,2,24)
    - [ ] **Sprite 0x64** (0x64) ×1: (0,6,16)

### Nivel 0x0DE
- **Direcciones**: L1ptr 0x2E29A → header 0x3189D · SprPtr 0x2EDBC → stream 0x3C7BD · L2ptr 0x2E89A · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=4 SPR=5 backArea=6
- **Colisión**: 30×27 casillas · SOLID=1 SLOPE=6 SLOPE_STEEP=6
- **Entrada**: casilla (8,21) = px (128,336)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,6,15) (0,11,24) (1,23,14) (1,28,21)

### Nivel 0x0DF
- **Direcciones**: L1ptr 0x2E29D → header 0x30636 · SprPtr 0x2EDBE → stream 0x3C414 · L2ptr 0x2E89D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x0E0
- **Direcciones**: L1ptr 0x2E2A0 → header 0x33DB6 · SprPtr 0x2EDC0 → stream 0x3CF4D · L2ptr 0x2E8A0 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=2 SPR=1 backArea=2
- **Colisión**: 128×27 casillas · LEDGE_TOP=110 SOLID=78 SLOPE_STEEP=17
- **Entrada**: casilla (8,19) = px (128,304)
- **Usa sprites grandes**: sí — Thwomp (0x26), BonyBeetle (0x31)
- **Enemigos (32)**:
    - [B] **Thwomp** (0x26) ×4: (1,23,14) (2,40,14) (3,62,14) (5,84,14)
    - [ ] **ThrowingDryBones** (0x30) ×3: (3,49,18) (5,82,23) (6,107,24)
    - [B] **BonyBeetle** (0x31) ×4: (2,34,23) (2,36,23) (7,117,19) (7,122,19)
    - [ ] **BallNChain** (0x9E) ×7: (2,47,5) (3,54,19) (3,58,5) (4,69,5) (5,80,5) (6,100,16) …
    - [ ] **Fishbone** (0xAA) ×14: (1,20,23) (1,23,21) (1,31,21) (2,39,18) (2,42,7) (3,61,7) …

### Nivel 0x0E1
- **Direcciones**: L1ptr 0x2E2A3 → header 0x33DB6 · SprPtr 0x2EDC2 → stream 0x3CF4D · L2ptr 0x2E8A3 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=2 SPR=1 backArea=2
- **Colisión**: 128×27 casillas · LEDGE_TOP=110 SOLID=78 SLOPE_STEEP=17
- **Entrada**: casilla (8,19) = px (128,304)
- **Usa sprites grandes**: sí — Thwomp (0x26), BonyBeetle (0x31)
- **Enemigos (32)**:
    - [B] **Thwomp** (0x26) ×4: (1,23,14) (2,40,14) (3,62,14) (5,84,14)
    - [ ] **ThrowingDryBones** (0x30) ×3: (3,49,18) (5,82,23) (6,107,24)
    - [B] **BonyBeetle** (0x31) ×4: (2,34,23) (2,36,23) (7,117,19) (7,122,19)
    - [ ] **BallNChain** (0x9E) ×7: (2,47,5) (3,54,19) (3,58,5) (4,69,5) (5,80,5) (6,100,16) …
    - [ ] **Fishbone** (0xAA) ×14: (1,20,23) (1,23,21) (1,31,21) (2,39,18) (2,42,7) (3,61,7) …

### Nivel 0x0E2
- **Direcciones**: L1ptr 0x2E2A6 → header 0x30636 · SprPtr 0x2EDC4 → stream 0x3C414 · L2ptr 0x2E8A6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x0E3
- **Direcciones**: L1ptr 0x2E2A9 → header 0x31473 · SprPtr 0x2EDC6 → stream 0x3C749 · L2ptr 0x2E8A9 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 14 pantallas (224 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=2
- **Colisión**: 224×27 casillas · LEDGE_TOP=220 SOLID=422 SLOPE=4 SLOPE_STEEP=4
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Feather** (0x77) ×1: (0,5,23)
    - [ ] **MessageBox** (0xB9) ×1: (0,7,20)

### Nivel 0x0E4
- **Direcciones**: L1ptr 0x2E2AC → header 0x3244F · SprPtr 0x2EDC8 → stream 0x3CA0C · L2ptr 0x2E8AC · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 16×27 casillas · SOLID=42
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [ ] **Sprite 0x37** (0x37) ×2: (0,2,15) (0,10,15)
    - [ ] **BigBooBoss** (0xC5) ×1: (0,11,13)

### Nivel 0x0E5
- **Direcciones**: L1ptr 0x2E2AF → header 0x30636 · SprPtr 0x2EDCA → stream 0x3C943 · L2ptr 0x2E8AF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,0)

### Nivel 0x0E7
- **Direcciones**: L1ptr 0x2E2B5 → header 0x31F64 · SprPtr 0x2EDCE → stream 0x3C926 · L2ptr 0x2E8B5 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x8 · música 3 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,25) = px (16,400)
- **Usa sprites grandes**: no
- **Enemigos (9)**:
    - [ ] **PortableSpringboard** (0x2F) ×1: (5,85,4)
    - [ ] **Sprite 0x32** (0x32) ×7: (0,9,11) (2,40,13) (2,40,9) (2,45,13) (3,53,3) (4,68,10) …
    - [ ] **Sprite 0xEF** (0xEF) ×1: (7,119,0)

### Nivel 0x0E8
- **Direcciones**: L1ptr 0x2E2B8 → header 0x31E2E · SprPtr 0x2EDD0 → stream 0x3C915 · L2ptr 0x2E8B8 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=52 SOLID=43 SLOPE_STEEP=8
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: sí — Thwomp (0x26)
- **Enemigos (5)**:
    - [B] **Thwomp** (0x26) ×3: (1,18,3) (1,28,11) (2,35,14)
    - [ ] **Sprite 0x32** (0x32) ×1: (0,2,5)
    - [ ] **Podoboo** (0x33) ×1: (1,22,8)

### Nivel 0x0E9
- **Direcciones**: L1ptr 0x2E2BB → header 0x3178E · SprPtr 0x2EDD2 → stream 0x3C7A7 · L2ptr 0x2E8BB · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=4
- **Colisión**: 64×27 casillas · LEDGE_TOP=54 SOLID=76 SLOPE_STEEP=11
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Keyhole** (0xE) ×1: (2,37,6)
    - [ ] **Key** (0x80) ×1: (2,40,6)
    - [ ] **Sprite 0x97** (0x97) ×1: (1,21,23)
    - [ ] **Sprite 0xDC** (0xDC) ×1: (3,52,23)

### Nivel 0x0EA
- **Direcciones**: L1ptr 0x2E2BE → header 0x385B4 · SprPtr 0x2EDD4 → stream 0x3DADD · L2ptr 0x2E8BE · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0xA · música 7 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=4 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,0) = px (128,0)
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
- **Direcciones**: L1ptr 0x2E2C1 → header 0x30621 · SprPtr 0x2EDD6 → stream 0x3C40C · L2ptr 0x2E8C1 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0EC
- **Direcciones**: L1ptr 0x2E2C4 → header 0x322F2 · SprPtr 0x2EDD8 → stream 0x3C9CA · L2ptr 0x2E8C4 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 79×27 casillas · SOLID=2 SLOPE=16 SLOPE_STEEP=16
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **BouncingFootball** (0x28) ×1: (3,51,20)
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,41,15)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,42,22)
    - [s] **PSwitch** (0x3E) ×1: (2,46,23)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,26,20)

### Nivel 0x0ED
- **Direcciones**: L1ptr 0x2E2C7 → header 0x32374 · SprPtr 0x2EDDA → stream 0x3C9DB · L2ptr 0x2E8C7 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 74×27 casillas · SOLID=11
- **Entrada**: casilla (14,22) = px (224,352)
- **Usa sprites grandes**: no
- **Enemigos (7)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,12,22) (1,18,21) (2,37,20) (3,62,18)
    - [s] **PSwitch** (0x3E) ×1: (0,6,18)
    - [ ] **MessageBox** (0xB9) ×1: (1,20,21)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (3,49,20)

### Nivel 0x0EE
- **Direcciones**: L1ptr 0x2E2CA → header 0x322F2 · SprPtr 0x2EDDC → stream 0x3C9CA · L2ptr 0x2E8CA · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 79×27 casillas · SOLID=2 SLOPE=16 SLOPE_STEEP=16
- **Entrada**: casilla (14,22) = px (224,352)
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **BouncingFootball** (0x28) ×1: (3,51,20)
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,41,15)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,42,22)
    - [s] **PSwitch** (0x3E) ×1: (2,46,23)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,26,20)

### Nivel 0x0EF
- **Direcciones**: L1ptr 0x2E2CD → header 0x36EFD · SprPtr 0x2EDDE → stream 0x3D9B1 · L2ptr 0x2E8CD · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=1 backArea=5
- **Colisión**: 192×27 casillas · LEDGE_TOP=160 SOLID=68 SLOPE_STEEP=28
- **Entrada**: casilla (1,14) = px (16,224)
- **Usa sprites grandes**: sí — Thwomp (0x26)
- **Enemigos (20)**:
    - [B] **Thwomp** (0x26) ×12: (0,12,12) (1,25,14) (2,35,14) (3,52,15) (3,57,15) (4,71,14) …
    - [ ] **Thwimp** (0x27) ×8: (2,37,23) (4,69,23) (5,83,23) (6,101,22) (6,103,22) (6,108,22) …

### Nivel 0x0F0
- **Direcciones**: L1ptr 0x2E2D0 → header 0x30621 · SprPtr 0x2EDE0 → stream 0x3C3F5 · L2ptr 0x2E8D0 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0F1
- **Direcciones**: L1ptr 0x2E2D3 → header 0x32420 · SprPtr 0x2EDE2 → stream 0x3C9F2 · L2ptr 0x2E8D3 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 47×27 casillas · SOLID=1
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **Sprite 0x37** (0x37) ×5: (0,4,17) (0,8,21) (0,14,20) (1,18,17) (1,30,17)
    - [ ] **Sprite 0x38** (0x38) ×1: (2,35,20)
    - [ ] **Sprite 0x39** (0x39) ×2: (2,41,16) (2,44,22)

### Nivel 0x0F2
- **Direcciones**: L1ptr 0x2E2D6 → header 0x32374 · SprPtr 0x2EDE4 → stream 0x3C9DB · L2ptr 0x2E8D6 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 74×27 casillas · SOLID=11
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (7)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,12,22) (1,18,21) (2,37,20) (3,62,18)
    - [s] **PSwitch** (0x3E) ×1: (0,6,18)
    - [ ] **MessageBox** (0xB9) ×1: (1,20,21)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (3,49,20)

### Nivel 0x0F3
- **Direcciones**: L1ptr 0x2E2D9 → header 0x350DC · SprPtr 0x2EDE6 → stream 0x3C3F0 · L2ptr 0x2E8D9 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=6
- **Colisión**: 17×27 casillas · SOLID=4
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x0F5
- **Direcciones**: L1ptr 0x2E2DF → header 0x365D0 · SprPtr 0x2EDEA → stream 0x3D6D9 · L2ptr 0x2E8DF · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=200 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (8,19) = px (128,304)
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
- **Direcciones**: L1ptr 0x2E2E2 → header 0x365D0 · SprPtr 0x2EDEC → stream 0x3D6D9 · L2ptr 0x2E8E2 · GFXslot 0x028EB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 23` (spriteGfx=10) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=200 SOLID=163 SLOPE=59 SLOPE_STEEP=77
- **Entrada**: casilla (1,17) = px (16,272)
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
- **Direcciones**: L1ptr 0x2E2E5 → header 0x38DAB · SprPtr 0x2EDEE → stream 0x3DC61 · L2ptr 0x2E8E5 · GFXslot 0x028CB · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 26 pantallas (416 casillas) · modo 0xA · música 4 · tiempo 400 · Layer2 fondo · paletas BG=5 FG=4 SPR=2 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,3) = px (16,48)
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
- **Direcciones**: L1ptr 0x2E2E8 → header 0x38CC6 · SprPtr 0x2EDF0 → stream 0x3DC3B · L2ptr 0x2E8E8 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0xC · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 127×27 casillas · SOLID=18
- **Entrada**: casilla (1,14) = px (16,224)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Sprite 0x37** (0x37) ×2: (6,101,20) (6,107,14)
    - [ ] **Sprite 0x38** (0x38) ×3: (0,15,23) (1,18,19) (1,25,21)
    - [ ] **Sprite 0x39** (0x39) ×3: (5,87,23) (5,93,17) (6,96,23)
    - [ ] **Sprite 0xD2** (0xD2) ×1: (5,95,0)
    - [ ] **Sprite 0xE2** (0xE2) ×2: (6,110,22) (7,124,14)
    - [ ] **Sprite 0xE5** (0xE5) ×1: (2,33,0)

### Nivel 0x0F9
- **Direcciones**: L1ptr 0x2E2EB → header 0x3189D · SprPtr 0x2EDF2 → stream 0x3C7BD · L2ptr 0x2E8EB · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=4 SPR=5 backArea=6
- **Colisión**: 30×27 casillas · SOLID=1 SLOPE=6 SLOPE_STEEP=6
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,6,15) (0,11,24) (1,23,14) (1,28,21)

### Nivel 0x0FB
- **Direcciones**: L1ptr 0x2E2F1 → header 0x30621 · SprPtr 0x2EDF6 → stream 0x3C3F5 · L2ptr 0x2E8F1 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x0FC
- **Direcciones**: L1ptr 0x2E2F4 → header 0x36815 · SprPtr 0x2EDF8 → stream 0x3D799 · L2ptr 0x2E8F4 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 95×27 casillas · SOLID=2
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Sprite 0xAF** (0xAF) ×6: (1,16,24) (1,17,24) (1,18,24) (5,81,22) (5,89,14) (5,91,19)
    - [ ] **Sprite 0xB0** (0xB0) ×6: (2,32,20) (3,49,18) (3,55,17) (4,68,20) (4,73,13) (5,88,22)

### Nivel 0x0FE
- **Direcciones**: L1ptr 0x2E2FA → header 0x318F0 · SprPtr 0x2EDFC → stream 0x3C7CB · L2ptr 0x2E8FA · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=4 SPR=5 backArea=6
- **Colisión**: 30×27 casillas · SOLID=1 SLOPE=6 SLOPE_STEEP=6
- **Entrada**: casilla (8,21) = px (128,336)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (0,6,15) (0,11,24) (1,23,14) (1,28,21)

### Nivel 0x0FF
- **Direcciones**: L1ptr 0x2E2FD → header 0x3063C · SprPtr 0x2EDFE → stream 0x3C3F0 · L2ptr 0x2E8FD · GFXslot 0x028CF · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=6
- **Colisión**: 32×27 casillas · LEDGE_TOP=31 SOLID=11 SLOPE_STEEP=1
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x100
- **Direcciones**: L1ptr 0x2E300 → header 0x30654 · SprPtr 0x2EE00 → stream 0x3C407 · L2ptr 0x2E900 · GFXslot 0x028DB · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=6) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 7 · tiempo 0 · Layer2 fondo · paletas BG=5 FG=0 SPR=4 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,14) = px (16,224)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **BonusGame** (0x82) ×1: (0,5,7)

### Nivel 0x101
- **Direcciones**: L1ptr 0x2E303 → header 0x30FFD · SprPtr 0x2EE02 → stream 0x3C66F · L2ptr 0x2E903 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=86 SOLID=29
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x102
- **Direcciones**: L1ptr 0x2E306 → header 0x30EAD · SprPtr 0x2EE04 → stream 0x3C5F4 · L2ptr 0x2E906 · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=1 SPR=2 backArea=6
- **Colisión**: 176×27 casillas · SOLID=47
- **Entrada**: casilla (1,17) = px (16,272)
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

### Nivel 0x103
- **Direcciones**: L1ptr 0x2E309 → header 0x30BDE · SprPtr 0x2EE06 → stream 0x3C593 · L2ptr 0x2E909 · GFXslot 0x028CB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 21 pantallas (336 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=1 SPR=2 backArea=2
- **Colisión**: 336×27 casillas · LEDGE_TOP=170 SOLID=172 SLOPE_STEEP=7
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x104
- **Direcciones**: L1ptr 0x2E30C → header 0x3802D · SprPtr 0x2EE08 → stream 0x3E759 · L2ptr 0x2E90C · GFXslot 0x028E7 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 0 · tiempo 0 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=0
- **Colisión**: 15×27 casillas · 
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Bird** (0x8A) ×4: (0,4,14) (0,5,14) (0,6,14) (0,7,14)
    - [ ] **SideExitAndFireplace** (0x8C) ×1: (0,8,7)
    - [ ] **MessageBox** (0xB9) ×1: (0,8,21)

### Nivel 0x105
- **Direcciones**: L1ptr 0x2E30F → header 0x308DD · SprPtr 0x2EE0A → stream 0x3C4CA · L2ptr 0x2E90F · GFXslot 0x028E3 · FGBGslot 0x02947
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 15` (tilesetFG=7)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=2
- **Colisión**: 320×27 casillas · LEDGE_TOP=349 SOLID=72 SLOPE=6 SLOPE_STEEP=14
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x106
- **Direcciones**: L1ptr 0x2E312 → header 0x30A2F · SprPtr 0x2EE0C → stream 0x3C532 · L2ptr 0x2E912 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=0
- **Colisión**: 320×27 casillas · LEDGE_TOP=373 SOLID=71 SLOPE_STEEP=6
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x107
- **Direcciones**: L1ptr 0x2E315 → header 0x32D09 · SprPtr 0x2EE0E → stream 0x3CBDC · L2ptr 0x2E915 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 176×27 casillas · SOLID=38 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (17)**:
    - [ ] **BouncingFootball** (0x28) ×2: (7,115,18) (8,142,20)
    - [ ] **Sprite 0x37** (0x37) ×9: (3,49,22) (3,61,23) (4,66,12) (4,72,22) (4,79,21) (5,90,16) …
    - [ ] **Sprite 0x38** (0x38) ×4: (1,18,22) (1,18,17) (9,156,4) (10,171,4)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (10,160,20)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,31,20)

### Nivel 0x108
- **Direcciones**: L1ptr 0x2E318 → header 0x380C3 · SprPtr 0x2EE10 → stream 0x3E76D · L2ptr 0x2E918 · GFXslot 0x028C7 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0xA · música 3 · tiempo 200 · Layer2 fondo · paletas BG=3 FG=2 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x109
- **Direcciones**: L1ptr 0x2E31B → header 0x33817 · SprPtr 0x2EE12 → stream 0x3CDC8 · L2ptr 0x2E91B · GFXslot 0x028CB · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0xA · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=2 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,25) = px (128,400)
- **Usa sprites grandes**: no
- **Enemigos (22)**:
    - [s] **RedKoopaNoShell** (0x5) ×2: (5,85,30) (6,102,23)
    - [ ] **RedParakoopa** (0x9) ×1: (6,104,18)
    - [ ] **BobOmb** (0xB) ×8: (2,47,24) (2,47,23) (2,47,22) (2,47,19) (2,47,18) (2,47,17) …
    - [ ] **PortableSpringboard** (0x2F) ×2: (2,44,26) (3,54,18)
    - [ ] **Sprite 0x6B** (0x6B) ×5: (1,20,1) (1,28,1) (4,65,1) (4,70,1) (4,75,1)
    - [ ] **RightWallSpringboard** (0x6C) ×4: (1,16,7) (1,24,7) (5,89,15) (5,94,15)

### Nivel 0x10A
- **Direcciones**: L1ptr 0x2E31E → header 0x32E7D · SprPtr 0x2EE14 → stream 0x3CC25 · L2ptr 0x2E91E · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 32 pantallas (512 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: 512×27 casillas · LEDGE_TOP=211 SOLID=569 SLOPE=36 SLOPE_STEEP=61
- **Entrada**: casilla (1,17) = px (16,272)
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

### Nivel 0x10B
- **Direcciones**: L1ptr 0x2E321 → header 0x32461 · SprPtr 0x2EE16 → stream 0x3CA17 · L2ptr 0x2E921 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 240×27 casillas · LEDGE_TOP=228 SOLID=196 SLOPE=13 SLOPE_STEEP=20
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (28)**:
    - [ ] **GreenKoopaNoShell** (0x4) ×2: (1,19,17) (11,183,23)
    - [ ] **GreenFlyingParakoopa** (0xA) ×9: (10,165,20) (13,208,16) (13,208,17) (13,208,18) (13,208,19) (13,208,20) …
    - [ ] **BobOmb** (0xB) ×1: (5,88,16)
    - [ ] **Sprite 0x2E** (0x2E) ×12: (3,53,23) (3,58,23) (4,66,16) (6,100,23) (6,110,23) (9,154,14) …
    - [ ] **PortableSpringboard** (0x2F) ×1: (2,34,23)
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (8,128,20) (8,133,21)
    - [ ] **GoalTape** (0x7B) ×1: (13,222,23)

### Nivel 0x10C
- **Direcciones**: L1ptr 0x2E324 → header 0x30000 · SprPtr 0x2EE18 → stream 0x3E76D · L2ptr 0x2E924 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x10D
- **Direcciones**: L1ptr 0x2E327 → header 0x3A600 · SprPtr 0x2EE1A → stream 0x3C422 · L2ptr 0x2E927 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=2 FG=3 SPR=1 backArea=7
- **Colisión**: 87×27 casillas · SOLID=64
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)

### Nivel 0x10E
- **Direcciones**: L1ptr 0x2E32A → header 0x3ABF9 · SprPtr 0x2EE1C → stream 0x3E19D · L2ptr 0x2E92A · GFXslot 0x028F3 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 24 0E` (spriteGfx=12) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x11 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=85 SOLID=67 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Ninji** (0x51) ×6: (2,43,17) (3,59,23) (4,70,23) (4,73,23) (5,87,23) (5,89,23)
    - [ ] **MechaKoopa** (0xA2) ×2: (4,64,21) (5,83,23)
    - [ ] **Spotlight** (0xC6) ×2: (1,24,0) (6,97,0)
    - [ ] **LightSwitch** (0xC8) ×1: (2,33,19)

### Nivel 0x10F
- **Direcciones**: L1ptr 0x2E32D → header 0x39B58 · SprPtr 0x2EE1E → stream 0x3DF08 · L2ptr 0x2E92D · GFXslot 0x028CB · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 21 pantallas (336 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=2 backArea=3
- **Colisión**: 336×27 casillas · LEDGE_TOP=159 SOLID=255 SLOPE=58 SLOPE_STEEP=74
- **Entrada**: casilla (1,17) = px (16,272)
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

### Nivel 0x110
- **Direcciones**: L1ptr 0x2E330 → header 0x39DE2 · SprPtr 0x2EE20 → stream 0x3DFB1 · L2ptr 0x2E930 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 126×27 casillas · LEDGE_TOP=29 SOLID=68 SLOPE_STEEP=12
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (15)**:
    - [ ] **Sprite 0x74** (0x74) ×1: (7,118,23)
    - [ ] **BallNChain** (0x9E) ×12: (1,31,18) (2,37,11) (2,43,18) (3,49,11) (3,55,18) (3,61,11) …
    - [ ] **CreateEatBlock** (0xB1) ×2: (0,11,24) (1,20,24)

### Nivel 0x111
- **Direcciones**: L1ptr 0x2E333 → header 0x3A028 · SprPtr 0x2EE22 → stream 0x3E032 · L2ptr 0x2E933 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 15 pantallas (240 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=7 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: sí — BonyBeetle (0x31)
- **Enemigos (17)**:
    - [ ] **ThrowingDryBones** (0x30) ×2: (4,69,23) (5,82,23)
    - [B] **BonyBeetle** (0x31) ×2: (4,78,23) (10,169,21)
    - [ ] **Sprite 0x32** (0x32) ×2: (9,150,19) (9,159,17)
    - [ ] **Podoboo** (0x33) ×7: (9,144,16) (9,153,14) (10,162,14) (10,164,14) (10,172,15) (12,192,15) …
    - [ ] **FallingSpike** (0xB2) ×3: (4,64,17) (4,78,17) (5,82,17)
    - [ ] **Sprite 0xE9** (0xE9) ×1: (0,8,0)

### Nivel 0x112
- **Direcciones**: L1ptr 0x2E336 → header 0x30000 · SprPtr 0x2EE24 → stream 0x3E76D · L2ptr 0x2E936 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x113
- **Direcciones**: L1ptr 0x2E339 → header 0x399D6 · SprPtr 0x2EE26 → stream 0x3DE4F · L2ptr 0x2E939 · GFXslot 0x028E3 · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 305×27 casillas · LEDGE_TOP=16 SOLID=89 SLOPE=36 SLOPE_STEEP=36
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x114
- **Direcciones**: L1ptr 0x2E33C → header 0x39803 · SprPtr 0x2EE28 → stream 0x3DE01 · L2ptr 0x2E93C · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=4 SPR=5 backArea=7
- **Colisión**: 64×27 casillas · SOLID=1 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **GreenGasBubble** (0x90) ×4: (1,21,18) (1,29,19) (2,40,19) (3,52,18)

### Nivel 0x115
- **Direcciones**: L1ptr 0x2E33F → header 0x392CA · SprPtr 0x2EE2A → stream 0x3DD7B · L2ptr 0x2E93F · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=50 SOLID=212 SLOPE_STEEP=3
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (18)**:
    - [s] **YellowKoopa** (0x3) ×1: (3,60,15)
    - [ ] **GreenKoopaNoShell** (0x4) ×5: (2,34,15) (3,51,22) (5,80,15) (6,98,16) (6,106,23)
    - [ ] **Swooper** (0xBE) ×9: (1,19,7) (1,27,7) (2,36,7) (3,52,7) (4,70,7) (5,83,7) …
    - [ ] **Sprite 0xDD** (0xDD) ×1: (3,58,15)
    - [ ] **Sprite 0xEA** (0xEA) ×2: (0,8,0) (7,120,0)

### Nivel 0x116
- **Direcciones**: L1ptr 0x2E342 → header 0x38EA4 · SprPtr 0x2EE2C → stream 0x3DD14 · L2ptr 0x2E942 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 18 pantallas (288 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 288×27 casillas · LEDGE_TOP=343 SOLID=519 SLOPE=52 SLOPE_STEEP=124
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: sí — MegaMole (0xBF), CharginChuck (0x91)
- **Enemigos (32)**:
    - [B] **CharginChuck** (0x91) ×12: (1,26,16) (3,58,23) (6,106,19) (6,110,21) (7,115,23) (10,165,23) …
    - [B] **MegaMole** (0xBF) ×20: (1,17,22) (3,53,14) (4,65,18) (4,65,7) (5,83,6) (5,87,11) …

### Nivel 0x117
- **Direcciones**: L1ptr 0x2E345 → header 0x3705D · SprPtr 0x2EE2E → stream 0x3D9EF · L2ptr 0x2E945 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=76 SOLID=130 SLOPE=32 SLOPE_STEEP=42
- **Entrada**: casilla (1,14) = px (16,224)
- **Usa sprites grandes**: sí — Blargg (0xA8)
- **Enemigos (11)**:
    - [s] **BuzzyBeetle** (0x11) ×4: (1,28,20) (2,37,18) (3,53,23) (3,56,23)
    - [ ] **PortableSpringboard** (0x2F) ×1: (0,8,23)
    - [ ] **Sprite 0x97** (0x97) ×5: (3,51,17) (3,60,15) (5,83,16) (6,101,21) (7,113,20)
    - [B] **Blargg** (0xA8) ×1: (0,8,25)

### Nivel 0x118
- **Direcciones**: L1ptr 0x2E348 → header 0x3295F · SprPtr 0x2EE30 → stream 0x3CB2A · L2ptr 0x2E948 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 320×27 casillas · LEDGE_TOP=282 SOLID=342 SLOPE=113 SLOPE_STEEP=153
- **Entrada**: casilla (8,6) = px (128,96)
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

### Nivel 0x119
- **Direcciones**: L1ptr 0x2E34B → header 0x332D1 · SprPtr 0x2EE32 → stream 0x3CCD4 · L2ptr 0x2E94B · GFXslot 0x028CB · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=1 SPR=2 backArea=4
- **Colisión**: 305×27 casillas · SOLID=32
- **Entrada**: casilla (1,14) = px (16,224)
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

### Nivel 0x11A
- **Direcciones**: L1ptr 0x2E34E → header 0x32600 · SprPtr 0x2EE34 → stream 0x3CA87 · L2ptr 0x2E94E · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 27 pantallas (432 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 432×27 casillas · LEDGE_TOP=316 SOLID=345 SLOPE=4 SLOPE_STEEP=45
- **Entrada**: casilla (1,14) = px (16,224)
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

### Nivel 0x11B
- **Direcciones**: L1ptr 0x2E351 → header 0x306D0 · SprPtr 0x2EE36 → stream 0x3C450 · L2ptr 0x2E951 · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=14
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [s] **YellowKoopa** (0x3) ×1: (0,9,23)
    - [ ] **GreenKoopaNoShell** (0x4) ×3: (0,13,23) (1,25,23) (2,37,23)
    - [s] **RedKoopaNoShell** (0x5) ×3: (1,17,23) (1,29,23) (2,41,23)
    - [ ] **BlueKoopaNoShell** (0x6) ×2: (1,21,23) (2,33,23)
    - [s] **PSwitch** (0x3E) ×1: (0,4,23)
    - [ ] **Sprite 0xDD** (0xDD) ×1: (0,8,23)

### Nivel 0x11C
- **Direcciones**: L1ptr 0x2E354 → header 0x334E0 · SprPtr 0x2EE38 → stream 0x3CD68 · L2ptr 0x2E954 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 160×27 casillas · LEDGE_TOP=25 SOLID=187 SLOPE_STEEP=4
- **Entrada**: casilla (8,14) = px (128,224)
- **Usa sprites grandes**: sí — MagiKoopa (0x1F)
- **Enemigos (14)**:
    - [B] **MagiKoopa** (0x1F) ×1: (1,22,21)
    - [ ] **Podoboo** (0x33) ×7: (1,20,19) (2,32,19) (2,43,19) (7,118,17) (8,132,14) (8,138,18) …
    - [s] **PSwitch** (0x3E) ×1: (5,84,14)
    - [ ] **Sprite 0x5B** (0x5B) ×5: (4,72,24) (4,77,24) (5,83,24) (5,89,24) (6,96,24)

### Nivel 0x11D
- **Direcciones**: L1ptr 0x2E357 → header 0x35ABE · SprPtr 0x2EE3A → stream 0x3D522 · L2ptr 0x2E957 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x1 · música 4 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 170×27 casillas · SOLID=2
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (24)**:
    - [ ] **BouncingFootball** (0x28) ×2: (1,17,14) (2,43,17)
    - [ ] **Sprite 0x37** (0x37) ×13: (1,25,17) (1,28,24) (3,51,16) (3,60,24) (4,66,18) (4,74,21) …
    - [ ] **Sprite 0x38** (0x38) ×1: (2,37,20)
    - [ ] **Sprite 0x39** (0x39) ×7: (8,128,20) (8,133,18) (8,133,19) (8,133,20) (9,155,16) (10,160,17) …
    - [ ] **Sprite 0xDE** (0xDE) ×1: (6,98,13)

### Nivel 0x11E
- **Direcciones**: L1ptr 0x2E35A → header 0x3523A · SprPtr 0x2EE3C → stream 0x3D30C · L2ptr 0x2E95A · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=1
- **Colisión**: 305×27 casillas · SOLID=28
- **Entrada**: casilla (1,23) = px (16,368)
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

### Nivel 0x11F
- **Direcciones**: L1ptr 0x2E35D → header 0x35F5B · SprPtr 0x2EE3E → stream 0x3D577 · L2ptr 0x2E95D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=0 backArea=1
- **Colisión**: 320×27 casillas · LEDGE_TOP=261 SOLID=253 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x120
- **Direcciones**: L1ptr 0x2E360 → header 0x3540B · SprPtr 0x2EE40 → stream 0x3D380 · L2ptr 0x2E960 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 14 pantallas (224 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=7 SPR=3 backArea=5
- **Colisión**: 224×27 casillas · LEDGE_TOP=219 SOLID=225 SLOPE_STEEP=90
- **Entrada**: casilla (1,19) = px (16,304)
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

### Nivel 0x121
- **Direcciones**: L1ptr 0x2E363 → header 0x3072B · SprPtr 0x2EE42 → stream 0x3C478 · L2ptr 0x2E963 · GFXslot 0x028C3 · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · SOLID=50
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (10)**:
    - [ ] **KoopaKidBossFight** (0x13) ×8: (0,7,14) (0,12,14) (1,17,14) (1,22,14) (1,27,14) (2,32,14) …
    - [s] **PSwitch** (0x3E) ×2: (0,6,23) (0,11,23)

### Nivel 0x122
- **Direcciones**: L1ptr 0x2E366 → header 0x36183 · SprPtr 0x2EE44 → stream 0x3D5F5 · L2ptr 0x2E966 · GFXslot 0x028CB · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=0 SPR=2 backArea=1
- **Colisión**: 145×27 casillas · SOLID=3
- **Entrada**: casilla (1,12) = px (16,192)
- **Usa sprites grandes**: sí — GreenParakoopa (0x8)
- **Enemigos (27)**:
    - [B] **GreenParakoopa** (0x8) ×14: (2,38,13) (2,39,13) (2,40,13) (2,47,17) (3,63,21) (4,78,19) …
    - [ ] **GreenFlyingParakoopa** (0xA) ×5: (1,24,19) (1,27,11) (3,55,15) (5,80,9) (6,105,21)
    - [ ] **BobOmb** (0xB) ×1: (4,69,12)
    - [ ] **Sprite 0x78** (0x78) ×3: (9,147,17) (9,148,17) (9,149,17)
    - [ ] **GoalTape** (0x7B) ×1: (8,142,17)
    - [ ] **WingedPlatform** (0xC1) ×2: (0,12,14) (1,17,14)
    - [ ] **Sprite 0xF4** (0xF4) ×1: (0,8,0)

### Nivel 0x123
- **Direcciones**: L1ptr 0x2E369 → header 0x356F3 · SprPtr 0x2EE46 → stream 0x3D445 · L2ptr 0x2E969 · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=0 SPR=0 backArea=0
- **Colisión**: 305×27 casillas · SOLID=176
- **Entrada**: casilla (1,23) = px (16,368)
- **Usa sprites grandes**: no
- **Enemigos (42)**:
    - [ ] **PortableSpringboard** (0x2F) ×2: (3,48,24) (5,95,15)
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **Sprite 0x92** (0x92) ×1: (17,278,24)
    - [ ] **BubbleWithSprite** (0x9D) ×31: (1,17,22) (1,28,21) (2,41,23) (2,44,21) (3,57,19) (3,61,23) …
    - [ ] **Sprite 0xD4** (0xD4) ×4: (12,206,0) (13,217,0) (17,272,0) (17,283,0)
    - [ ] **Sprite 0xD9** (0xD9) ×2: (11,189,0) (18,300,0)
    - [ ] **Sprite 0xDB** (0xDB) ×1: (5,95,19)

### Nivel 0x124
- **Direcciones**: L1ptr 0x2E36C → header 0x30000 · SprPtr 0x2EE48 → stream 0x3E76D · L2ptr 0x2E96C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x125
- **Direcciones**: L1ptr 0x2E36F → header 0x3BF65 · SprPtr 0x2EE4A → stream 0x3E6F4 · L2ptr 0x2E96F · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 32 pantallas (512 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=5
- **Colisión**: 512×27 casillas · LEDGE_TOP=400 SOLID=325 SLOPE_STEEP=4
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x126
- **Direcciones**: L1ptr 0x2E372 → header 0x3BDE5 · SprPtr 0x2EE4C → stream 0x3E650 · L2ptr 0x2E972 · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=1 SPR=0 backArea=1
- **Colisión**: 305×27 casillas · SOLID=158
- **Entrada**: casilla (1,23) = px (16,368)
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

### Nivel 0x127
- **Direcciones**: L1ptr 0x2E375 → header 0x3BC11 · SprPtr 0x2EE4E → stream 0x3E5DF · L2ptr 0x2E975 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 256×27 casillas · LEDGE_TOP=254 SOLID=286 SLOPE_STEEP=28
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (37)**:
    - [s] **RedKoopaNoShell** (0x5) ×4: (7,113,19) (7,117,23) (11,189,21) (11,191,21)
    - [ ] **Sprite 0x15** (0x15) ×16: (2,40,18) (3,52,19) (3,59,18) (4,75,21) (4,78,17) (5,95,21) …
    - [ ] **VerticalCheepCheep** (0x16) ×9: (1,19,23) (1,25,23) (4,68,22) (5,81,23) (6,101,22) (9,155,21) …
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (2,44,20) (8,130,22)
    - [ ] **HammerBro** (0x9B) ×3: (1,24,16) (4,71,16) (14,239,15)
    - [ ] **HammerBroPlatform** (0x9C) ×3: (1,24,16) (4,71,16) (14,239,15)

### Nivel 0x128
- **Direcciones**: L1ptr 0x2E378 → header 0x3BABE · SprPtr 0x2EE50 → stream 0x3E574 · L2ptr 0x2E978 · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=0 SPR=0 backArea=6
- **Colisión**: 320×27 casillas · LEDGE_TOP=296 SOLID=220 SLOPE=14 SLOPE_STEEP=24
- **Entrada**: casilla (1,21) = px (16,336)
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

### Nivel 0x129
- **Direcciones**: L1ptr 0x2E37B → header 0x30000 · SprPtr 0x2EE52 → stream 0x3E76D · L2ptr 0x2E97B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x12A
- **Direcciones**: L1ptr 0x2E37E → header 0x3B26B · SprPtr 0x2EE54 → stream 0x3E3DC · L2ptr 0x2E97E · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xA · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=1 SPR=2 backArea=6
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,11) = px (128,176)
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

### Nivel 0x12B
- **Direcciones**: L1ptr 0x2E381 → header 0x3B46E · SprPtr 0x2EE56 → stream 0x3E428 · L2ptr 0x2E981 · GFXslot 0x028FB · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 0E` (spriteGfx=14) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=0 SPR=0 backArea=2
- **Colisión**: 176×27 casillas · LEDGE_TOP=21 SOLID=180
- **Entrada**: casilla (8,17) = px (128,272)
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

### Nivel 0x12C
- **Direcciones**: L1ptr 0x2E384 → header 0x3B540 · SprPtr 0x2EE58 → stream 0x3E466 · L2ptr 0x2E984 · GFXslot 0x028CB · FGBGslot 0x02933
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 16` (tilesetFG=2)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=1 SPR=2 backArea=1
- **Colisión**: 305×27 casillas · SOLID=31
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (44)**:
    - [ ] **Sprite 0x62** (0x62) ×2: (12,205,19) (14,231,19)
    - [ ] **Sprite 0x63** (0x63) ×1: (1,18,22)
    - [ ] **Sprite 0x64** (0x64) ×4: (11,184,15) (12,192,17) (13,208,17) (15,246,15)
    - [ ] **Sprite 0x65** (0x65) ×2: (2,43,25) (6,111,16)
    - [ ] **Sprite 0x68** (0x68) ×32: (1,29,21) (2,33,15) (2,35,7) (2,39,16) (3,59,11) (3,59,19) …
    - [ ] **GoalTape** (0x7B) ×1: (18,302,23)
    - [ ] **GreyFallingPlatform** (0xC4) ×2: (10,168,22) (10,172,20)

### Nivel 0x12D
- **Direcciones**: L1ptr 0x2E387 → header 0x3B908 · SprPtr 0x2EE5A → stream 0x3E4F1 · L2ptr 0x2E987 · GFXslot 0x028E3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=0 backArea=7
- **Colisión**: 320×27 casillas · LEDGE_TOP=152 SOLID=191 SLOPE=70 SLOPE_STEEP=77
- **Entrada**: casilla (1,19) = px (16,304)
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

### Nivel 0x12E
- **Direcciones**: L1ptr 0x2E38A → header 0x30000 · SprPtr 0x2EE5C → stream 0x3E76D · L2ptr 0x2E98A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x12F
- **Direcciones**: L1ptr 0x2E38D → header 0x30000 · SprPtr 0x2EE5E → stream 0x3E76D · L2ptr 0x2E98D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x130
- **Direcciones**: L1ptr 0x2E390 → header 0x3AF25 · SprPtr 0x2EE60 → stream 0x3E221 · L2ptr 0x2E990 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=6 SPR=3 backArea=2
- **Colisión**: 192×27 casillas · LEDGE_TOP=152 SOLID=97 SLOPE_STEEP=8
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (41)**:
    - [ ] **Keyhole** (0xE) ×1: (11,185,23)
    - [s] **YoshiEgg** (0x2C) ×1: (0,9,23)
    - [ ] **RipVanFish** (0x3D) ×17: (1,26,20) (1,26,16) (1,31,23) (1,31,18) (2,37,14) (2,37,19) …
    - [ ] **Star** (0x76) ×1: (0,4,10)
    - [ ] **Key** (0x80) ×1: (11,189,23)
    - [ ] **Blurp** (0xC2) ×20: (0,15,20) (1,18,23) (1,18,17) (3,49,20) (3,51,22) (3,51,18) …

### Nivel 0x131
- **Direcciones**: L1ptr 0x2E393 → header 0x30000 · SprPtr 0x2EE62 → stream 0x3E76D · L2ptr 0x2E993 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x132
- **Direcciones**: L1ptr 0x2E396 → header 0x3AFE3 · SprPtr 0x2EE64 → stream 0x3E29E · L2ptr 0x2E996 · GFXslot 0x028C3 · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 2 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=5
- **Colisión**: 33×27 casillas · SOLID=109
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **Keyhole** (0xE) ×1: (1,23,5)
    - [ ] **Lakitu** (0x1E) ×1: (0,10,14)
    - [s] **YoshiEgg** (0x2C) ×1: (0,6,23)
    - [s] **PSwitch** (0x3E) ×1: (0,7,23)
    - [ ] **GoalTape** (0x7B) ×1: (1,30,23)

### Nivel 0x133
- **Direcciones**: L1ptr 0x2E399 → header 0x30000 · SprPtr 0x2EE66 → stream 0x3E76D · L2ptr 0x2E999 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x134
- **Direcciones**: L1ptr 0x2E39C → header 0x3AD35 · SprPtr 0x2EE68 → stream 0x3E1C5 · L2ptr 0x2E99C · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0xA · música 0 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (8,3) = px (128,48)
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

### Nivel 0x135
- **Direcciones**: L1ptr 0x2E39F → header 0x3B031 · SprPtr 0x2EE6A → stream 0x3E2AF · L2ptr 0x2E99F · GFXslot 0x028CB · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 20 pantallas (320 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=1 SPR=2 backArea=5
- **Colisión**: 305×27 casillas · SOLID=74
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x136
- **Direcciones**: L1ptr 0x2E3A2 → header 0x3B124 · SprPtr 0x2EE6C → stream 0x3E335 · L2ptr 0x2E9A2 · GFXslot 0x028C3 · FGBGslot 0x02943
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 16` (tilesetFG=6)
- **Propiedades**: ancho 17 pantallas (272 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=5
- **Colisión**: 257×27 casillas · SOLID=336
- **Entrada**: casilla (1,22) = px (16,352)
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

### Nivel 0x137
- **Direcciones**: L1ptr 0x2E3A5 → header 0x30000 · SprPtr 0x2EE6E → stream 0x3E76D · L2ptr 0x2E9A5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x138
- **Direcciones**: L1ptr 0x2E3A8 → header 0x30000 · SprPtr 0x2EE70 → stream 0x3E76D · L2ptr 0x2E9A8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x139
- **Direcciones**: L1ptr 0x2E3AB → header 0x30000 · SprPtr 0x2EE72 → stream 0x3E76D · L2ptr 0x2E9AB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13A
- **Direcciones**: L1ptr 0x2E3AE → header 0x30000 · SprPtr 0x2EE74 → stream 0x3E76D · L2ptr 0x2E9AE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13B
- **Direcciones**: L1ptr 0x2E3B1 → header 0x30000 · SprPtr 0x2EE76 → stream 0x3E76D · L2ptr 0x2E9B1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13C
- **Direcciones**: L1ptr 0x2E3B4 → header 0x30000 · SprPtr 0x2EE78 → stream 0x3E76D · L2ptr 0x2E9B4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13D
- **Direcciones**: L1ptr 0x2E3B7 → header 0x30000 · SprPtr 0x2EE7A → stream 0x3E76D · L2ptr 0x2E9B7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13E
- **Direcciones**: L1ptr 0x2E3BA → header 0x30000 · SprPtr 0x2EE7C → stream 0x3E76D · L2ptr 0x2E9BA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x13F
- **Direcciones**: L1ptr 0x2E3BD → header 0x30000 · SprPtr 0x2EE7E → stream 0x3E76D · L2ptr 0x2E9BD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x140
- **Direcciones**: L1ptr 0x2E3C0 → header 0x30000 · SprPtr 0x2EE80 → stream 0x3E76D · L2ptr 0x2E9C0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x141
- **Direcciones**: L1ptr 0x2E3C3 → header 0x30000 · SprPtr 0x2EE82 → stream 0x3E76D · L2ptr 0x2E9C3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x142
- **Direcciones**: L1ptr 0x2E3C6 → header 0x30000 · SprPtr 0x2EE84 → stream 0x3E76D · L2ptr 0x2E9C6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x143
- **Direcciones**: L1ptr 0x2E3C9 → header 0x30000 · SprPtr 0x2EE86 → stream 0x3E76D · L2ptr 0x2E9C9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x144
- **Direcciones**: L1ptr 0x2E3CC → header 0x30000 · SprPtr 0x2EE88 → stream 0x3E76D · L2ptr 0x2E9CC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x145
- **Direcciones**: L1ptr 0x2E3CF → header 0x30000 · SprPtr 0x2EE8A → stream 0x3E76D · L2ptr 0x2E9CF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x146
- **Direcciones**: L1ptr 0x2E3D2 → header 0x30000 · SprPtr 0x2EE8C → stream 0x3E76D · L2ptr 0x2E9D2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x147
- **Direcciones**: L1ptr 0x2E3D5 → header 0x30000 · SprPtr 0x2EE8E → stream 0x3E76D · L2ptr 0x2E9D5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x148
- **Direcciones**: L1ptr 0x2E3D8 → header 0x30000 · SprPtr 0x2EE90 → stream 0x3E76D · L2ptr 0x2E9D8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x149
- **Direcciones**: L1ptr 0x2E3DB → header 0x30000 · SprPtr 0x2EE92 → stream 0x3E76D · L2ptr 0x2E9DB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14A
- **Direcciones**: L1ptr 0x2E3DE → header 0x30000 · SprPtr 0x2EE94 → stream 0x3E76D · L2ptr 0x2E9DE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14B
- **Direcciones**: L1ptr 0x2E3E1 → header 0x30000 · SprPtr 0x2EE96 → stream 0x3E76D · L2ptr 0x2E9E1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14C
- **Direcciones**: L1ptr 0x2E3E4 → header 0x30000 · SprPtr 0x2EE98 → stream 0x3E76D · L2ptr 0x2E9E4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14D
- **Direcciones**: L1ptr 0x2E3E7 → header 0x30000 · SprPtr 0x2EE9A → stream 0x3E76D · L2ptr 0x2E9E7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14E
- **Direcciones**: L1ptr 0x2E3EA → header 0x30000 · SprPtr 0x2EE9C → stream 0x3E76D · L2ptr 0x2E9EA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x14F
- **Direcciones**: L1ptr 0x2E3ED → header 0x30000 · SprPtr 0x2EE9E → stream 0x3E76D · L2ptr 0x2E9ED · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x150
- **Direcciones**: L1ptr 0x2E3F0 → header 0x30000 · SprPtr 0x2EEA0 → stream 0x3E76D · L2ptr 0x2E9F0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x151
- **Direcciones**: L1ptr 0x2E3F3 → header 0x30000 · SprPtr 0x2EEA2 → stream 0x3E76D · L2ptr 0x2E9F3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x152
- **Direcciones**: L1ptr 0x2E3F6 → header 0x30000 · SprPtr 0x2EEA4 → stream 0x3E76D · L2ptr 0x2E9F6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x153
- **Direcciones**: L1ptr 0x2E3F9 → header 0x30000 · SprPtr 0x2EEA6 → stream 0x3E76D · L2ptr 0x2E9F9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x154
- **Direcciones**: L1ptr 0x2E3FC → header 0x30000 · SprPtr 0x2EEA8 → stream 0x3E76D · L2ptr 0x2E9FC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x155
- **Direcciones**: L1ptr 0x2E3FF → header 0x30000 · SprPtr 0x2EEAA → stream 0x3E76D · L2ptr 0x2E9FF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x156
- **Direcciones**: L1ptr 0x2E402 → header 0x30000 · SprPtr 0x2EEAC → stream 0x3E76D · L2ptr 0x2EA02 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x157
- **Direcciones**: L1ptr 0x2E405 → header 0x30000 · SprPtr 0x2EEAE → stream 0x3E76D · L2ptr 0x2EA05 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x158
- **Direcciones**: L1ptr 0x2E408 → header 0x30000 · SprPtr 0x2EEB0 → stream 0x3E76D · L2ptr 0x2EA08 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x159
- **Direcciones**: L1ptr 0x2E40B → header 0x30000 · SprPtr 0x2EEB2 → stream 0x3E76D · L2ptr 0x2EA0B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15A
- **Direcciones**: L1ptr 0x2E40E → header 0x30000 · SprPtr 0x2EEB4 → stream 0x3E76D · L2ptr 0x2EA0E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15B
- **Direcciones**: L1ptr 0x2E411 → header 0x30000 · SprPtr 0x2EEB6 → stream 0x3E76D · L2ptr 0x2EA11 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15C
- **Direcciones**: L1ptr 0x2E414 → header 0x30000 · SprPtr 0x2EEB8 → stream 0x3E76D · L2ptr 0x2EA14 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15D
- **Direcciones**: L1ptr 0x2E417 → header 0x30000 · SprPtr 0x2EEBA → stream 0x3E76D · L2ptr 0x2EA17 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15E
- **Direcciones**: L1ptr 0x2E41A → header 0x30000 · SprPtr 0x2EEBC → stream 0x3E76D · L2ptr 0x2EA1A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x15F
- **Direcciones**: L1ptr 0x2E41D → header 0x30000 · SprPtr 0x2EEBE → stream 0x3E76D · L2ptr 0x2EA1D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x160
- **Direcciones**: L1ptr 0x2E420 → header 0x30000 · SprPtr 0x2EEC0 → stream 0x3E76D · L2ptr 0x2EA20 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x161
- **Direcciones**: L1ptr 0x2E423 → header 0x30000 · SprPtr 0x2EEC2 → stream 0x3E76D · L2ptr 0x2EA23 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x162
- **Direcciones**: L1ptr 0x2E426 → header 0x30000 · SprPtr 0x2EEC4 → stream 0x3E76D · L2ptr 0x2EA26 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x163
- **Direcciones**: L1ptr 0x2E429 → header 0x30000 · SprPtr 0x2EEC6 → stream 0x3E76D · L2ptr 0x2EA29 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x164
- **Direcciones**: L1ptr 0x2E42C → header 0x30000 · SprPtr 0x2EEC8 → stream 0x3E76D · L2ptr 0x2EA2C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x165
- **Direcciones**: L1ptr 0x2E42F → header 0x30000 · SprPtr 0x2EECA → stream 0x3E76D · L2ptr 0x2EA2F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x166
- **Direcciones**: L1ptr 0x2E432 → header 0x30000 · SprPtr 0x2EECC → stream 0x3E76D · L2ptr 0x2EA32 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x167
- **Direcciones**: L1ptr 0x2E435 → header 0x30000 · SprPtr 0x2EECE → stream 0x3E76D · L2ptr 0x2EA35 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x168
- **Direcciones**: L1ptr 0x2E438 → header 0x30000 · SprPtr 0x2EED0 → stream 0x3E76D · L2ptr 0x2EA38 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x169
- **Direcciones**: L1ptr 0x2E43B → header 0x30000 · SprPtr 0x2EED2 → stream 0x3E76D · L2ptr 0x2EA3B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16A
- **Direcciones**: L1ptr 0x2E43E → header 0x30000 · SprPtr 0x2EED4 → stream 0x3E76D · L2ptr 0x2EA3E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16B
- **Direcciones**: L1ptr 0x2E441 → header 0x30000 · SprPtr 0x2EED6 → stream 0x3E76D · L2ptr 0x2EA41 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16C
- **Direcciones**: L1ptr 0x2E444 → header 0x30000 · SprPtr 0x2EED8 → stream 0x3E76D · L2ptr 0x2EA44 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16D
- **Direcciones**: L1ptr 0x2E447 → header 0x30000 · SprPtr 0x2EEDA → stream 0x3E76D · L2ptr 0x2EA47 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16E
- **Direcciones**: L1ptr 0x2E44A → header 0x30000 · SprPtr 0x2EEDC → stream 0x3E76D · L2ptr 0x2EA4A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x16F
- **Direcciones**: L1ptr 0x2E44D → header 0x30000 · SprPtr 0x2EEDE → stream 0x3E76D · L2ptr 0x2EA4D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x170
- **Direcciones**: L1ptr 0x2E450 → header 0x30000 · SprPtr 0x2EEE0 → stream 0x3E76D · L2ptr 0x2EA50 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x171
- **Direcciones**: L1ptr 0x2E453 → header 0x30000 · SprPtr 0x2EEE2 → stream 0x3E76D · L2ptr 0x2EA53 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x172
- **Direcciones**: L1ptr 0x2E456 → header 0x30000 · SprPtr 0x2EEE4 → stream 0x3E76D · L2ptr 0x2EA56 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x173
- **Direcciones**: L1ptr 0x2E459 → header 0x30000 · SprPtr 0x2EEE6 → stream 0x3E76D · L2ptr 0x2EA59 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x174
- **Direcciones**: L1ptr 0x2E45C → header 0x30000 · SprPtr 0x2EEE8 → stream 0x3E76D · L2ptr 0x2EA5C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x175
- **Direcciones**: L1ptr 0x2E45F → header 0x30000 · SprPtr 0x2EEEA → stream 0x3E76D · L2ptr 0x2EA5F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x176
- **Direcciones**: L1ptr 0x2E462 → header 0x30000 · SprPtr 0x2EEEC → stream 0x3E76D · L2ptr 0x2EA62 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x177
- **Direcciones**: L1ptr 0x2E465 → header 0x30000 · SprPtr 0x2EEEE → stream 0x3E76D · L2ptr 0x2EA65 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x178
- **Direcciones**: L1ptr 0x2E468 → header 0x30000 · SprPtr 0x2EEF0 → stream 0x3E76D · L2ptr 0x2EA68 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x179
- **Direcciones**: L1ptr 0x2E46B → header 0x30000 · SprPtr 0x2EEF2 → stream 0x3E76D · L2ptr 0x2EA6B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17A
- **Direcciones**: L1ptr 0x2E46E → header 0x30000 · SprPtr 0x2EEF4 → stream 0x3E76D · L2ptr 0x2EA6E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17B
- **Direcciones**: L1ptr 0x2E471 → header 0x30000 · SprPtr 0x2EEF6 → stream 0x3E76D · L2ptr 0x2EA71 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17C
- **Direcciones**: L1ptr 0x2E474 → header 0x30000 · SprPtr 0x2EEF8 → stream 0x3E76D · L2ptr 0x2EA74 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17D
- **Direcciones**: L1ptr 0x2E477 → header 0x30000 · SprPtr 0x2EEFA → stream 0x3E76D · L2ptr 0x2EA77 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17E
- **Direcciones**: L1ptr 0x2E47A → header 0x30000 · SprPtr 0x2EEFC → stream 0x3E76D · L2ptr 0x2EA7A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x17F
- **Direcciones**: L1ptr 0x2E47D → header 0x30000 · SprPtr 0x2EEFE → stream 0x3E76D · L2ptr 0x2EA7D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x180
- **Direcciones**: L1ptr 0x2E480 → header 0x30000 · SprPtr 0x2EF00 → stream 0x3E76D · L2ptr 0x2EA80 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x181
- **Direcciones**: L1ptr 0x2E483 → header 0x30000 · SprPtr 0x2EF02 → stream 0x3E76D · L2ptr 0x2EA83 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x182
- **Direcciones**: L1ptr 0x2E486 → header 0x30000 · SprPtr 0x2EF04 → stream 0x3E76D · L2ptr 0x2EA86 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x183
- **Direcciones**: L1ptr 0x2E489 → header 0x30000 · SprPtr 0x2EF06 → stream 0x3E76D · L2ptr 0x2EA89 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x184
- **Direcciones**: L1ptr 0x2E48C → header 0x30000 · SprPtr 0x2EF08 → stream 0x3E76D · L2ptr 0x2EA8C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x185
- **Direcciones**: L1ptr 0x2E48F → header 0x30000 · SprPtr 0x2EF0A → stream 0x3E76D · L2ptr 0x2EA8F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x186
- **Direcciones**: L1ptr 0x2E492 → header 0x30000 · SprPtr 0x2EF0C → stream 0x3E76D · L2ptr 0x2EA92 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x187
- **Direcciones**: L1ptr 0x2E495 → header 0x30000 · SprPtr 0x2EF0E → stream 0x3E76D · L2ptr 0x2EA95 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x188
- **Direcciones**: L1ptr 0x2E498 → header 0x30000 · SprPtr 0x2EF10 → stream 0x3E76D · L2ptr 0x2EA98 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x189
- **Direcciones**: L1ptr 0x2E49B → header 0x30000 · SprPtr 0x2EF12 → stream 0x3E76D · L2ptr 0x2EA9B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18A
- **Direcciones**: L1ptr 0x2E49E → header 0x30000 · SprPtr 0x2EF14 → stream 0x3E76D · L2ptr 0x2EA9E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18B
- **Direcciones**: L1ptr 0x2E4A1 → header 0x30000 · SprPtr 0x2EF16 → stream 0x3E76D · L2ptr 0x2EAA1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18C
- **Direcciones**: L1ptr 0x2E4A4 → header 0x30000 · SprPtr 0x2EF18 → stream 0x3E76D · L2ptr 0x2EAA4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18D
- **Direcciones**: L1ptr 0x2E4A7 → header 0x30000 · SprPtr 0x2EF1A → stream 0x3E76D · L2ptr 0x2EAA7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18E
- **Direcciones**: L1ptr 0x2E4AA → header 0x30000 · SprPtr 0x2EF1C → stream 0x3E76D · L2ptr 0x2EAAA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x18F
- **Direcciones**: L1ptr 0x2E4AD → header 0x30000 · SprPtr 0x2EF1E → stream 0x3E76D · L2ptr 0x2EAAD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x190
- **Direcciones**: L1ptr 0x2E4B0 → header 0x30000 · SprPtr 0x2EF20 → stream 0x3E76D · L2ptr 0x2EAB0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x191
- **Direcciones**: L1ptr 0x2E4B3 → header 0x30000 · SprPtr 0x2EF22 → stream 0x3E76D · L2ptr 0x2EAB3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x192
- **Direcciones**: L1ptr 0x2E4B6 → header 0x30000 · SprPtr 0x2EF24 → stream 0x3E76D · L2ptr 0x2EAB6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x193
- **Direcciones**: L1ptr 0x2E4B9 → header 0x3058B · SprPtr 0x2EF26 → stream 0x3C3E3 · L2ptr 0x2EAB9 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) = px (128,224)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,6)
    - [ ] **Sprite 0xB6** (0xB6) ×2: (0,1,16) (0,14,16)

### Nivel 0x194
- **Direcciones**: L1ptr 0x2E4BC → header 0x30561 · SprPtr 0x2EF28 → stream 0x3C3DB · L2ptr 0x2EABC · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) = px (128,224)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,5)
    - [ ] **Sprite 0xB6** (0xB6) ×1: (0,1,16)

### Nivel 0x195
- **Direcciones**: L1ptr 0x2E4BF → header 0x30258 · SprPtr 0x2EF2A → stream 0x3C367 · L2ptr 0x2EABF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x196
- **Direcciones**: L1ptr 0x2E4C2 → header 0x3025E · SprPtr 0x2EF2C → stream 0x3C359 · L2ptr 0x2EAC2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,4)
    - [ ] **Podoboo** (0x33) ×3: (0,2,0) (0,7,0) (0,13,0)

### Nivel 0x197
- **Direcciones**: L1ptr 0x2E4C5 → header 0x3025E · SprPtr 0x2EF2E → stream 0x3C354 · L2ptr 0x2EAC5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,3)

### Nivel 0x198
- **Direcciones**: L1ptr 0x2E4C8 → header 0x30258 · SprPtr 0x2EF30 → stream 0x3C34F · L2ptr 0x2EAC8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,2)

### Nivel 0x199
- **Direcciones**: L1ptr 0x2E4CB → header 0x30258 · SprPtr 0x2EF32 → stream 0x3C34A · L2ptr 0x2EACB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,1)

### Nivel 0x19A
- **Direcciones**: L1ptr 0x2E4CE → header 0x30258 · SprPtr 0x2EF34 → stream 0x3C345 · L2ptr 0x2EACE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,0)

### Nivel 0x19B
- **Direcciones**: L1ptr 0x2E4D1 → header 0x30252 · SprPtr 0x2EF36 → stream 0x3C340 · L2ptr 0x2EAD1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x10 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **ActivateBowserBattle** (0xA0) ×1: (0,0,0)

### Nivel 0x19C
- **Direcciones**: L1ptr 0x2E4D4 → header 0x30000 · SprPtr 0x2EF38 → stream 0x3E76D · L2ptr 0x2EAD4 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x19D
- **Direcciones**: L1ptr 0x2E4D7 → header 0x30000 · SprPtr 0x2EF3A → stream 0x3E76D · L2ptr 0x2EAD7 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x19E
- **Direcciones**: L1ptr 0x2E4DA → header 0x30000 · SprPtr 0x2EF3C → stream 0x3E76D · L2ptr 0x2EADA · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x19F
- **Direcciones**: L1ptr 0x2E4DD → header 0x30000 · SprPtr 0x2EF3E → stream 0x3E76D · L2ptr 0x2EADD · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A0
- **Direcciones**: L1ptr 0x2E4E0 → header 0x30000 · SprPtr 0x2EF40 → stream 0x3E76D · L2ptr 0x2EAE0 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A1
- **Direcciones**: L1ptr 0x2E4E3 → header 0x30000 · SprPtr 0x2EF42 → stream 0x3E76D · L2ptr 0x2EAE3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A2
- **Direcciones**: L1ptr 0x2E4E6 → header 0x30000 · SprPtr 0x2EF44 → stream 0x3E76D · L2ptr 0x2EAE6 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A3
- **Direcciones**: L1ptr 0x2E4E9 → header 0x30000 · SprPtr 0x2EF46 → stream 0x3E76D · L2ptr 0x2EAE9 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A4
- **Direcciones**: L1ptr 0x2E4EC → header 0x30000 · SprPtr 0x2EF48 → stream 0x3E76D · L2ptr 0x2EAEC · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A5
- **Direcciones**: L1ptr 0x2E4EF → header 0x30000 · SprPtr 0x2EF4A → stream 0x3E76D · L2ptr 0x2EAEF · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A6
- **Direcciones**: L1ptr 0x2E4F2 → header 0x30000 · SprPtr 0x2EF4C → stream 0x3E76D · L2ptr 0x2EAF2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A7
- **Direcciones**: L1ptr 0x2E4F5 → header 0x30000 · SprPtr 0x2EF4E → stream 0x3E76D · L2ptr 0x2EAF5 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A8
- **Direcciones**: L1ptr 0x2E4F8 → header 0x30000 · SprPtr 0x2EF50 → stream 0x3E76D · L2ptr 0x2EAF8 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1A9
- **Direcciones**: L1ptr 0x2E4FB → header 0x30000 · SprPtr 0x2EF52 → stream 0x3E76D · L2ptr 0x2EAFB · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AA
- **Direcciones**: L1ptr 0x2E4FE → header 0x30000 · SprPtr 0x2EF54 → stream 0x3E76D · L2ptr 0x2EAFE · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AB
- **Direcciones**: L1ptr 0x2E501 → header 0x30000 · SprPtr 0x2EF56 → stream 0x3E76D · L2ptr 0x2EB01 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AC
- **Direcciones**: L1ptr 0x2E504 → header 0x30000 · SprPtr 0x2EF58 → stream 0x3E76D · L2ptr 0x2EB04 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AD
- **Direcciones**: L1ptr 0x2E507 → header 0x30000 · SprPtr 0x2EF5A → stream 0x3E76D · L2ptr 0x2EB07 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AE
- **Direcciones**: L1ptr 0x2E50A → header 0x30000 · SprPtr 0x2EF5C → stream 0x3E76D · L2ptr 0x2EB0A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1AF
- **Direcciones**: L1ptr 0x2E50D → header 0x30000 · SprPtr 0x2EF5E → stream 0x3E76D · L2ptr 0x2EB0D · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B0
- **Direcciones**: L1ptr 0x2E510 → header 0x30000 · SprPtr 0x2EF60 → stream 0x3E76D · L2ptr 0x2EB10 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B1
- **Direcciones**: L1ptr 0x2E513 → header 0x30000 · SprPtr 0x2EF62 → stream 0x3E76D · L2ptr 0x2EB13 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B2
- **Direcciones**: L1ptr 0x2E516 → header 0x30000 · SprPtr 0x2EF64 → stream 0x3E76D · L2ptr 0x2EB16 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B3
- **Direcciones**: L1ptr 0x2E519 → header 0x30000 · SprPtr 0x2EF66 → stream 0x3E76D · L2ptr 0x2EB19 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B4
- **Direcciones**: L1ptr 0x2E51C → header 0x30000 · SprPtr 0x2EF68 → stream 0x3E76D · L2ptr 0x2EB1C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B5
- **Direcciones**: L1ptr 0x2E51F → header 0x30000 · SprPtr 0x2EF6A → stream 0x3E76D · L2ptr 0x2EB1F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B6
- **Direcciones**: L1ptr 0x2E522 → header 0x30000 · SprPtr 0x2EF6C → stream 0x3E76D · L2ptr 0x2EB22 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B7
- **Direcciones**: L1ptr 0x2E525 → header 0x30000 · SprPtr 0x2EF6E → stream 0x3E76D · L2ptr 0x2EB25 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B8
- **Direcciones**: L1ptr 0x2E528 → header 0x30000 · SprPtr 0x2EF70 → stream 0x3E76D · L2ptr 0x2EB28 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1B9
- **Direcciones**: L1ptr 0x2E52B → header 0x30000 · SprPtr 0x2EF72 → stream 0x3E76D · L2ptr 0x2EB2B · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1BA
- **Direcciones**: L1ptr 0x2E52E → header 0x30000 · SprPtr 0x2EF74 → stream 0x3E76D · L2ptr 0x2EB2E · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 200 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: 48×27 casillas · LEDGE_TOP=44 SOLID=85
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1BD
- **Direcciones**: L1ptr 0x2E537 → header 0x3AAC9 · SprPtr 0x2EF7A → stream 0x3E19D · L2ptr 0x2EB37 · GFXslot 0x028F3 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 24 0E` (spriteGfx=12) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0x11 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 128×27 casillas · LEDGE_TOP=85 SOLID=67 SLOPE_STEEP=12
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Ninji** (0x51) ×6: (2,43,17) (3,59,23) (4,70,23) (4,73,23) (5,87,23) (5,89,23)
    - [ ] **MechaKoopa** (0xA2) ×2: (4,64,21) (5,83,23)
    - [ ] **Spotlight** (0xC6) ×2: (1,24,0) (6,97,0)
    - [ ] **LightSwitch** (0xC8) ×1: (2,33,19)

### Nivel 0x1BE
- **Direcciones**: L1ptr 0x2E53A → header 0x30FB1 · SprPtr 0x2EF7C → stream 0x3C661 · L2ptr 0x2EB3A · GFXslot 0x028D7 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=0 SPR=0 backArea=6
- **Colisión**: 64×27 casillas · LEDGE_TOP=46 SOLID=26 SLOPE=10 SLOPE_STEEP=10
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: sí — Pokey (0x70)
- **Enemigos (4)**:
    - [B] **Pokey** (0x70) ×3: (1,19,18) (2,34,19) (3,50,16)
    - [ ] **MessageBox** (0xB9) ×1: (1,29,21)

### Nivel 0x1BF
- **Direcciones**: L1ptr 0x2E53D → header 0x39D84 · SprPtr 0x2EF7E → stream 0x3DF94 · L2ptr 0x2EB3D · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 57×27 casillas · LEDGE_TOP=18 SOLID=67 SLOPE_STEEP=4
- **Entrada**: casilla (1,12) = px (16,192)
- **Usa sprites grandes**: no
- **Enemigos (9)**:
    - [ ] **Sprite 0x15** (0x15) ×5: (1,18,25) (1,25,25) (1,31,25) (2,40,25) (2,46,25)
    - [ ] **Sprite 0x2E** (0x2E) ×4: (1,16,18) (1,22,20) (1,29,19) (2,39,18)

### Nivel 0x1C0
- **Direcciones**: L1ptr 0x2E540 → header 0x37511 · SprPtr 0x2EF80 → stream 0x3DA7F · L2ptr 0x2EB40 · GFXslot 0x028CF · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=0 SPR=4 backArea=6
- **Colisión**: 48×27 casillas · LEDGE_TOP=23 SOLID=28 SLOPE_STEEP=2
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **HammerBro** (0x9B) ×1: (1,23,15)
    - [ ] **HammerBroPlatform** (0x9C) ×1: (1,23,15)
    - [ ] **GreyFallingPlatform** (0xC4) ×4: (1,18,23) (1,23,23) (1,28,23) (2,33,23)

### Nivel 0x1C1
- **Direcciones**: L1ptr 0x2E543 → header 0x36128 · SprPtr 0x2EF82 → stream 0x3D5CF · L2ptr 0x2EB43 · GFXslot 0x028D3 · FGBGslot 0x0294F
- **GFX sprites (SP1-4)**: `00 01 13 06` (spriteGfx=4) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0D 1A` (tilesetFG=9)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 5 · tiempo 300 · Layer2 fondo · paletas BG=4 FG=1 SPR=3 backArea=2
- **Colisión**: 59×27 casillas · LEDGE_TOP=12 SOLID=52 SLOPE_STEEP=5
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Blurp** (0xC2) ×12: (0,6,22) (0,12,19) (1,18,21) (1,19,15) (1,23,23) (1,29,17) …

### Nivel 0x1C2
- **Direcciones**: L1ptr 0x2E546 → header 0x331B5 · SprPtr 0x2EF84 → stream 0x3CCBA · L2ptr 0x2EB46 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=20 SOLID=109 SLOPE=16 SLOPE_STEEP=20
- **Entrada**: casilla (1,12) = px (16,192)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **GreenFlyingParakoopa** (0xA) ×1: (3,51,22)
    - [s] **BuzzyBeetle** (0x11) ×5: (0,14,19) (1,17,19) (1,26,22) (2,35,18) (3,61,22)
    - [ ] **Sprite 0x15** (0x15) ×2: (2,41,25) (3,53,25)

### Nivel 0x1C3
- **Direcciones**: L1ptr 0x2E549 → header 0x32CA8 · SprPtr 0x2EF86 → stream 0x3CBC5 · L2ptr 0x2EB49 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=5 SPR=4 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=32 SOLID=82 SLOPE=6 SLOPE_STEEP=10
- **Entrada**: casilla (1,12) = px (16,192)
- **Usa sprites grandes**: no
- **Enemigos (7)**:
    - [s] **BuzzyBeetle** (0x11) ×4: (0,15,21) (1,21,20) (2,45,22) (2,47,22)
    - [ ] **Sprite 0x15** (0x15) ×2: (1,28,25) (3,54,25)
    - [ ] **Sprite 0x2E** (0x2E) ×1: (3,52,20)

### Nivel 0x1C4
- **Direcciones**: L1ptr 0x2E54C → header 0x3B3C6 · SprPtr 0x2EF88 → stream 0x3E402 · L2ptr 0x2EB4C · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=6
- **Colisión**: 112×27 casillas · LEDGE_TOP=90 SOLID=72 SLOPE_STEEP=3
- **Entrada**: casilla (14,17) = px (224,272)
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
- **Direcciones**: L1ptr 0x2E54F → header 0x3B3C6 · SprPtr 0x2EF8A → stream 0x3E402 · L2ptr 0x2EB4F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 7 pantallas (112 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=6
- **Colisión**: 112×27 casillas · LEDGE_TOP=90 SOLID=72 SLOPE_STEEP=3
- **Entrada**: casilla (8,23) = px (128,368)
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
- **Direcciones**: L1ptr 0x2E552 → header 0x3256D · SprPtr 0x2EF8C → stream 0x3CA6D · L2ptr 0x2EB52 · GFXslot 0x028C3 · FGBGslot 0x02947
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 15` (tilesetFG=7)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=1 FG=0 SPR=0 backArea=6
- **Colisión**: 64×27 casillas · LEDGE_TOP=21 SOLID=33
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [s] **JumpingPiranhaPlant** (0x4F) ×2: (1,24,24) (2,38,23)
    - [ ] **GreyFallingPlatform** (0xC4) ×6: (0,14,24) (1,19,24) (1,27,24) (2,33,24) (2,41,24) (3,48,24)

### Nivel 0x1C7
- **Direcciones**: L1ptr 0x2E555 → header 0x3AD2F · SprPtr 0x2EF8E → stream 0x3E1C0 · L2ptr 0x2EB55 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x10 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **ActivateBowserBattle** (0xA0) ×1: (0,0,0)

### Nivel 0x1C8
- **Direcciones**: L1ptr 0x2E558 → header 0x3B896 · SprPtr 0x2EF90 → stream 0x3E4EC · L2ptr 0x2EB58 · GFXslot 0x028CB · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 7 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=5 SPR=2 backArea=0
- **Colisión**: 80×27 casillas · SOLID=15
- **Entrada**: casilla (1,25) = px (16,400)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0)

### Nivel 0x1CA
- **Direcciones**: L1ptr 0x2E55E → header 0x30BB3 · SprPtr 0x2EF94 → stream 0x3C57F · L2ptr 0x2EB5E · GFXslot 0x028D7 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 09` (spriteGfx=5) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=30 SOLID=70 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Sprite 0x84** (0x84) ×6: (0,8,15) (0,12,14) (1,19,15) (1,20,13) (1,24,16) (1,28,14)

### Nivel 0x1CB
- **Direcciones**: L1ptr 0x2E561 → header 0x309F8 · SprPtr 0x2EF96 → stream 0x3C3EE · L2ptr 0x2EB61 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=30 SOLID=75 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1CC
- **Direcciones**: L1ptr 0x2E564 → header 0x3AA77 · SprPtr 0x2EF98 → stream 0x3E183 · L2ptr 0x2EB64 · GFXslot 0x028C3 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=0 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=58 SOLID=14 SLOPE_STEEP=6
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (8)**:
    - [ ] **Sprite 0x93** (0x93) ×8: (0,14,23) (1,21,23) (1,30,23) (2,33,20) (2,40,23) (2,45,23) …

### Nivel 0x1CD
- **Direcciones**: L1ptr 0x2E567 → header 0x3AA16 · SprPtr 0x2EF9A → stream 0x3E160 · L2ptr 0x2EB67 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=7 FG=7 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=51 SOLID=19 SLOPE_STEEP=7
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Podoboo** (0x33) ×1: (2,41,15)
    - [ ] **BowserStatue** (0xBC) ×9: (0,8,23) (0,13,21) (1,17,15) (1,26,23) (1,29,21) (2,38,23) …
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)

### Nivel 0x1CE
- **Direcciones**: L1ptr 0x2E56A → header 0x3A961 · SprPtr 0x2EF9C → stream 0x3E131 · L2ptr 0x2EB6A · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x8 · música 3 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=2 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,8) = px (16,128)
- **Usa sprites grandes**: no
- **Enemigos (15)**:
    - [ ] **Sprite 0x32** (0x32) ×2: (2,39,6) (4,65,8)
    - [ ] **BallNChain** (0x9E) ×6: (1,17,10) (1,21,2) (1,24,7) (1,26,13) (1,30,6) (1,30,1)
    - [ ] **Fishbone** (0xAA) ×6: (3,52,22) (3,53,29) (3,53,15) (3,55,20) (3,56,14) (3,56,27)
    - [ ] **Sprite 0xEF** (0xEF) ×1: (4,72,0)

### Nivel 0x1CF
- **Direcciones**: L1ptr 0x2E56D → header 0x3A8D9 · SprPtr 0x2EF9E → stream 0x3E114 · L2ptr 0x2EB6D · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x2 · música 3 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 46×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: sí — Thwomp (0x26)
- **Enemigos (9)**:
    - [B] **Thwomp** (0x26) ×8: (0,8,14) (0,12,14) (1,16,14) (1,20,14) (1,24,14) (1,28,14) …
    - [ ] **Sprite 0xE9** (0xE9) ×1: (0,8,0)

### Nivel 0x1D0
- **Direcciones**: L1ptr 0x2E570 → header 0x3A83F · SprPtr 0x2EFA0 → stream 0x3C422 · L2ptr 0x2EB70 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=2 FG=3 SPR=1 backArea=7
- **Colisión**: 87×27 casillas · SOLID=72
- **Entrada**: casilla (1,21) = px (16,336)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)

### Nivel 0x1D1
- **Direcciones**: L1ptr 0x2E573 → header 0x3A802 · SprPtr 0x2EFA2 → stream 0x3E0E8 · L2ptr 0x2EB73 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · SOLID=3
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (14)**:
    - [ ] **Sprite 0x78** (0x78) ×1: (1,20,13)
    - [ ] **Sparky** (0xA5) ×8: (0,2,23) (0,7,23) (0,12,18) (1,30,21) (2,36,19) (2,44,20) …
    - [ ] **Sprite 0xA6** (0xA6) ×1: (2,40,20)
    - [ ] **MovingCastleStone** (0xBB) ×3: (0,15,23) (1,24,18) (1,25,23)
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,1)

### Nivel 0x1D2
- **Direcciones**: L1ptr 0x2E576 → header 0x3A765 · SprPtr 0x2EFA4 → stream 0x3E0C5 · L2ptr 0x2EB76 · GFXslot 0x028F3 · FGBGslot 0x02957
- **GFX sprites (SP1-4)**: `00 01 24 0E` (spriteGfx=12) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=11)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=7
- **Colisión**: 80×27 casillas · SOLID=459
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (11)**:
    - [ ] **Feather** (0x77) ×1: (3,61,12)
    - [ ] **Sprite 0x78** (0x78) ×1: (0,1,8)
    - [ ] **MechaKoopa** (0xA2) ×9: (1,18,20) (1,18,12) (1,31,8) (2,32,20) (2,42,12) (3,53,16) …

### Nivel 0x1D3
- **Direcciones**: L1ptr 0x2E579 → header 0x3A707 · SprPtr 0x2EFA6 → stream 0x3E08D · L2ptr 0x2EB79 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=14 SOLID=38
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (18)**:
    - [ ] **Sprite 0x25** (0x25) ×8: (1,20,22) (1,20,18) (1,24,16) (1,30,19) (1,30,15) (2,36,21) …
    - [ ] **ClimbingNetDoor** (0x54) ×3: (0,11,17) (1,22,17) (2,32,17)
    - [ ] **Sprite 0xB6** (0xB6) ×7: (1,17,22) (1,17,16) (1,22,16) (1,28,18) (2,34,16) (2,34,22) …

### Nivel 0x1D4
- **Direcciones**: L1ptr 0x2E57C → header 0x3A68E · SprPtr 0x2EFA8 → stream 0x3E067 · L2ptr 0x2EB7C · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 3 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=3 SPR=1 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=15 SOLID=31
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (12)**:
    - [ ] **Podoboo** (0x33) ×9: (0,9,15) (0,14,15) (1,21,15) (1,26,19) (1,26,15) (2,34,15) …
    - [ ] **Layer3Smasher** (0x89) ×1: (0,7,0)
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0)

### Nivel 0x1D5
- **Direcciones**: L1ptr 0x2E57F → header 0x3AFCE · SprPtr 0x2EFAA → stream 0x3C3F0 · L2ptr 0x2EB7F · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=21 SOLID=13
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1D6
- **Direcciones**: L1ptr 0x2E582 → header 0x3AF16 · SprPtr 0x2EFAC → stream 0x3C3F0 · L2ptr 0x2EB82 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=4
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1D7
- **Direcciones**: L1ptr 0x2E585 → header 0x30838 · SprPtr 0x2EFAE → stream 0x3C498 · L2ptr 0x2EB85 · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x1D8
- **Direcciones**: L1ptr 0x2E588 → header 0x307F3 · SprPtr 0x2EFB0 → stream 0x3C473 · L2ptr 0x2EB88 · GFXslot 0x028EF · FGBGslot 0x0293B
- **GFX sprites (SP1-4)**: `00 01 0D 14` (spriteGfx=11) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 08` (tilesetFG=4)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 7 · tiempo 200 · Layer2 fondo · paletas BG=5 FG=1 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0x6D** (0x6D) ×1: (2,32,20)

### Nivel 0x1D9
- **Direcciones**: L1ptr 0x2E58B → header 0x39803 · SprPtr 0x2EFB2 → stream 0x3DE01 · L2ptr 0x2EB8B · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=4 SPR=5 backArea=7
- **Colisión**: 64×27 casillas · SOLID=1 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (14,19) = px (224,304)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **GreenGasBubble** (0x90) ×4: (1,21,18) (1,29,19) (2,40,19) (3,52,18)

### Nivel 0x1DA
- **Direcciones**: L1ptr 0x2E58E → header 0x30621 · SprPtr 0x2EFB4 → stream 0x3C3F5 · L2ptr 0x2EB8E · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1DB
- **Direcciones**: L1ptr 0x2E591 → header 0x39969 · SprPtr 0x2EFB6 → stream 0x3DE3B · L2ptr 0x2EB91 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=5 backArea=7
- **Colisión**: 51×27 casillas · SOLID=34
- **Entrada**: casilla (14,22) = px (224,352)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Keyhole** (0xE) ×1: (3,63,5)
    - [ ] **Sprite 0x37** (0x37) ×3: (2,44,20) (3,54,21) (3,59,19)
    - [s] **PSwitch** (0x3E) ×1: (0,6,23)
    - [ ] **Key** (0x80) ×1: (3,58,5)

### Nivel 0x1DC
- **Direcciones**: L1ptr 0x2E594 → header 0x39969 · SprPtr 0x2EFB8 → stream 0x3DE3B · L2ptr 0x2EB94 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=5 backArea=7
- **Colisión**: 51×27 casillas · SOLID=34
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **Keyhole** (0xE) ×1: (3,63,5)
    - [ ] **Sprite 0x37** (0x37) ×3: (2,44,20) (3,54,21) (3,59,19)
    - [s] **PSwitch** (0x3E) ×1: (0,6,23)
    - [ ] **Key** (0x80) ×1: (3,58,5)

### Nivel 0x1DD
- **Direcciones**: L1ptr 0x2E597 → header 0x39867 · SprPtr 0x2EFBA → stream 0x3DE0F · L2ptr 0x2EB97 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 4 · tiempo 300 · Layer2 fondo · paletas BG=2 FG=4 SPR=5 backArea=7
- **Colisión**: 159×27 casillas · SOLID=96
- **Entrada**: casilla (8,19) = px (128,304)
- **Usa sprites grandes**: no
- **Enemigos (14)**:
    - [ ] **Sprite 0x37** (0x37) ×4: (1,16,22) (2,34,23) (2,44,23) (4,76,21)
    - [ ] **Sprite 0x39** (0x39) ×3: (4,72,19) (5,95,24) (6,106,16)
    - [s] **PSwitch** (0x3E) ×1: (3,56,15)
    - [ ] **Star** (0x76) ×1: (3,56,4)
    - [ ] **Sprite 0x78** (0x78) ×1: (9,158,23)
    - [ ] **Sprite 0xB0** (0xB0) ×4: (1,24,20) (1,27,14) (2,39,19) (2,40,14)

### Nivel 0x1DE
- **Direcciones**: L1ptr 0x2E59A → header 0x30636 · SprPtr 0x2EFBC → stream 0x3C414 · L2ptr 0x2EB9A · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x9 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [ ] **Reznor** (0xA9) ×4: (0,1,0) (0,2,0) (0,3,0) (0,4,0)

### Nivel 0x1DF
- **Direcciones**: L1ptr 0x2E59D → header 0x36104 · SprPtr 0x2EFBE → stream 0x3D5C7 · L2ptr 0x2EB9D · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=0 backArea=3
- **Colisión**: 16×27 casillas · LEDGE_TOP=12 SOLID=50 SLOPE_STEEP=3
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Keyhole** (0xE) ×1: (0,11,22)
    - [ ] **Key** (0x80) ×1: (0,6,22)

### Nivel 0x1E0
- **Direcciones**: L1ptr 0x2E5A0 → header 0x3BD8A · SprPtr 0x2EFC0 → stream 0x3C3EE · L2ptr 0x2EBA0 · GFXslot 0x028C3 · FGBGslot 0x0295B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 1F` (tilesetFG=12)
- **Propiedades**: ancho 5 pantallas (80 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=5 SPR=0 backArea=1
- **Colisión**: 80×27 casillas · LEDGE_TOP=16 SOLID=54 SLOPE=60 SLOPE_STEEP=60
- **Entrada**: casilla (1,6) = px (16,96)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1E1
- **Direcciones**: L1ptr 0x2E5A3 → header 0x3BD75 · SprPtr 0x2EFC2 → stream 0x3C3F0 · L2ptr 0x2EBA3 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 0 · tiempo 300 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=1
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=4
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1E2
- **Direcciones**: L1ptr 0x2E5A6 → header 0x395F0 · SprPtr 0x2EFC4 → stream 0x3DDB8 · L2ptr 0x2EBA6 · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 9 pantallas (144 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 144×27 casillas · LEDGE_TOP=130 SOLID=164 SLOPE_STEEP=38
- **Entrada**: casilla (8,17) = px (128,272)
- **Usa sprites grandes**: sí — MegaMole (0xBF)
- **Enemigos (7)**:
    - [ ] **Keyhole** (0xE) ×1: (0,8,20)
    - [ ] **GoalTape** (0x7B) ×1: (7,126,23)
    - [ ] **Key** (0x80) ×1: (0,3,20)
    - [B] **MegaMole** (0xBF) ×3: (3,57,20) (4,71,17) (7,115,16)
    - [ ] **Sprite 0xF5** (0xF5) ×1: (1,24,0)

### Nivel 0x1E3
- **Direcciones**: L1ptr 0x2E5A9 → header 0x393E2 · SprPtr 0x2EFC6 → stream 0x3DDB3 · L2ptr 0x2EBA9 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=1 SPR=4 backArea=3
- **Colisión**: 160×27 casillas · LEDGE_TOP=136 SOLID=140 SLOPE=7 SLOPE_STEEP=67
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,0)

### Nivel 0x1E5
- **Direcciones**: L1ptr 0x2E5AF → header 0x39221 · SprPtr 0x2EFCA → stream 0x3DD76 · L2ptr 0x2EBAF · GFXslot 0x028E3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 20` (spriteGfx=8) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=4
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1E6
- **Direcciones**: L1ptr 0x2E5B2 → header 0x35F46 · SprPtr 0x2EFCC → stream 0x3C3F5 · L2ptr 0x2EBB2 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1E7
- **Direcciones**: L1ptr 0x2E5B5 → header 0x30621 · SprPtr 0x2EFCE → stream 0x3C40C · L2ptr 0x2EBB5 · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1E8
- **Direcciones**: L1ptr 0x2E5B8 → header 0x35ABE · SprPtr 0x2EFD0 → stream 0x3D522 · L2ptr 0x2EBB8 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x1 · música 4 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 170×27 casillas · SOLID=2
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (24)**:
    - [ ] **BouncingFootball** (0x28) ×2: (1,17,14) (2,43,17)
    - [ ] **Sprite 0x37** (0x37) ×13: (1,25,17) (1,28,24) (3,51,16) (3,60,24) (4,66,18) (4,74,21) …
    - [ ] **Sprite 0x38** (0x38) ×1: (2,37,20)
    - [ ] **Sprite 0x39** (0x39) ×7: (8,128,20) (8,133,18) (8,133,19) (8,133,20) (9,155,16) (10,160,17) …
    - [ ] **Sprite 0xDE** (0xDE) ×1: (6,98,13)

### Nivel 0x1E9
- **Direcciones**: L1ptr 0x2E5BB → header 0x35ABE · SprPtr 0x2EFD2 → stream 0x3D522 · L2ptr 0x2EBBB · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x1 · música 4 · tiempo 400 · Layer2 nivel · paletas BG=6 FG=4 SPR=5 backArea=3
- **Colisión**: 170×27 casillas · SOLID=2
- **Entrada**: casilla (8,19) = px (128,304)
- **Usa sprites grandes**: no
- **Enemigos (24)**:
    - [ ] **BouncingFootball** (0x28) ×2: (1,17,14) (2,43,17)
    - [ ] **Sprite 0x37** (0x37) ×13: (1,25,17) (1,28,24) (3,51,16) (3,60,24) (4,66,18) (4,74,21) …
    - [ ] **Sprite 0x38** (0x38) ×1: (2,37,20)
    - [ ] **Sprite 0x39** (0x39) ×7: (8,128,20) (8,133,18) (8,133,19) (8,133,20) (9,155,16) (10,160,17) …
    - [ ] **Sprite 0xDE** (0xDE) ×1: (6,98,13)

### Nivel 0x1EA
- **Direcciones**: L1ptr 0x2E5BE → header 0x32E18 · SprPtr 0x2EFD4 → stream 0x3CC11 · L2ptr 0x2EBBE · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0xC · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 127×27 casillas · SOLID=3
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (6)**:
    - [ ] **GreenGasBubble** (0x90) ×6: (1,20,17) (2,34,16) (3,50,16) (4,70,16) (5,82,13) (6,98,16)

### Nivel 0x1EB
- **Direcciones**: L1ptr 0x2E5C1 → header 0x30687 · SprPtr 0x2EFD6 → stream 0x3E024 · L2ptr 0x2EBC1 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (4)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,4)
    - [ ] **Podoboo** (0x33) ×3: (0,2,0) (0,7,0) (0,13,0)

### Nivel 0x1EC
- **Direcciones**: L1ptr 0x2E5C4 → header 0x3735D · SprPtr 0x2EFD8 → stream 0x3DA44 · L2ptr 0x2EBC4 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 16 pantallas (256 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=7 SPR=4 backArea=3
- **Colisión**: 256×27 casillas · LEDGE_TOP=93 SOLID=156 SLOPE=27 SLOPE_STEEP=41
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: sí — CharginChuck (0x91)
- **Enemigos (19)**:
    - [ ] **Sprite 0x2E** (0x2E) ×12: (1,18,21) (1,21,19) (1,24,17) (1,27,15) (2,47,15) (3,54,19) …
    - [B] **CharginChuck** (0x91) ×6: (9,149,23) (9,158,19) (10,165,19) (10,174,15) (11,185,18) (12,193,15)
    - [ ] **Sprite 0xF5** (0xF5) ×1: (0,8,0)

### Nivel 0x1ED
- **Direcciones**: L1ptr 0x2E5C7 → header 0x37164 · SprPtr 0x2EFDA → stream 0x3DA12 · L2ptr 0x2EBC7 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0xA · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,3) = px (16,48)
- **Usa sprites grandes**: no
- **Enemigos (16)**:
    - [s] **BuzzyBeetle** (0x11) ×16: (1,20,10) (1,21,8) (2,34,14) (2,36,18) (2,37,21) (3,52,16) …

### Nivel 0x1EE
- **Direcciones**: L1ptr 0x2E5CA → header 0x374FC · SprPtr 0x2EFDC → stream 0x3C3F0 · L2ptr 0x2EBCA · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=7 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=20
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)

### Nivel 0x1EF
- **Direcciones**: L1ptr 0x2E5CD → header 0x328E9 · SprPtr 0x2EFDE → stream 0x3CB01 · L2ptr 0x2EBCD · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x2 · música 1 · tiempo 400 · Layer2 nivel · paletas BG=3 FG=2 SPR=4 backArea=3
- **Colisión**: 96×27 casillas · LEDGE_TOP=17 SOLID=60 SLOPE=14 SLOPE_STEEP=16
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: sí — Blargg (0xA8)
- **Enemigos (13)**:
    - [ ] **RedParakoopa** (0x9) ×1: (4,75,18)
    - [s] **BuzzyBeetle** (0x11) ×7: (1,30,14) (2,33,14) (2,41,14) (2,46,14) (3,56,16) (3,62,16) …
    - [B] **Blargg** (0xA8) ×4: (1,29,25) (2,40,25) (3,52,25) (3,62,25)
    - [ ] **Sprite 0xF5** (0xF5) ×1: (0,8,0)

### Nivel 0x1F0
- **Direcciones**: L1ptr 0x2E5D0 → header 0x33A33 · SprPtr 0x2EFE0 → stream 0x3CE14 · L2ptr 0x2EBD0 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=37 SOLID=41 SLOPE=5 SLOPE_STEEP=8
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (1,30,23)
    - [ ] **Sprite 0x97** (0x97) ×1: (1,23,15)

### Nivel 0x1F1
- **Direcciones**: L1ptr 0x2E5D3 → header 0x33A06 · SprPtr 0x2EFE2 → stream 0x3CE0C · L2ptr 0x2EBD3 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 3 pantallas (48 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 48×27 casillas · LEDGE_TOP=49 SOLID=30 SLOPE=5 SLOPE_STEEP=6
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (1,30,23)
    - [ ] **Sprite 0x97** (0x97) ×1: (1,22,16)

### Nivel 0x1F2
- **Direcciones**: L1ptr 0x2E5D6 → header 0x337ED · SprPtr 0x2EFE4 → stream 0x3CDC0 · L2ptr 0x2EBD6 · GFXslot 0x028F7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 0A 22` (spriteGfx=13) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 6 · tiempo 400 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 16×27 casillas · SOLID=28
- **Entrada**: casilla (8,14) = px (128,224)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,5)
    - [ ] **Sprite 0xB6** (0xB6) ×1: (0,1,16)

### Nivel 0x1F3
- **Direcciones**: L1ptr 0x2E5D9 → header 0x33666 · SprPtr 0x2EFE6 → stream 0x3CD94 · L2ptr 0x2EBD9 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 12 pantallas (192 casillas) · modo 0x2 · música 3 · tiempo 300 · Layer2 nivel · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (14)**:
    - [ ] **Sprite 0x32** (0x32) ×8: (2,37,19) (3,56,21) (5,81,18) (8,142,21) (9,151,15) (9,154,15) …
    - [ ] **Podoboo** (0x33) ×4: (3,49,15) (4,74,15) (6,97,14) (8,137,14)
    - [ ] **Sprite 0x74** (0x74) ×1: (6,106,24)
    - [ ] **Sprite 0xEA** (0xEA) ×1: (0,8,0)

### Nivel 0x1F4
- **Direcciones**: L1ptr 0x2E5DC → header 0x33620 · SprPtr 0x2EFE8 → stream 0x3C3EE · L2ptr 0x2EBDC · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=32 SOLID=1
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos**: (ninguno colocado)

### Nivel 0x1F5
- **Direcciones**: L1ptr 0x2E5DF → header 0x33422 · SprPtr 0x2EFEA → stream 0x3CD63 · L2ptr 0x2EBDF · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 6 pantallas (96 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 96×27 casillas · LEDGE_TOP=12 SOLID=107 SLOPE_STEEP=5
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [ ] **SkullRaft** (0x61) ×1: (0,10,24)

### Nivel 0x1F6
- **Direcciones**: L1ptr 0x2E5E2 → header 0x30687 · SprPtr 0x2EFEC → stream 0x3C6D0 · L2ptr 0x2EBE2 · GFXslot 0x028C3 · FGBGslot 0x0292B
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 15` (tilesetFG=0)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0xB · música 6 · tiempo 400 · Layer2 fondo · paletas BG=0 FG=0 SPR=0 backArea=0
- **Colisión**: — (sin colisión reconstruible)
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **KoopaKid** (0x29) ×1: (0,12,3)

### Nivel 0x1F8
- **Direcciones**: L1ptr 0x2E5E8 → header 0x35914 · SprPtr 0x2EFF0 → stream 0x3D4C5 · L2ptr 0x2EBE8 · GFXslot 0x028C3 · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 02` (spriteGfx=0) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 1 pantallas (16 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 16×27 casillas · LEDGE_TOP=13 SOLID=42 SLOPE_STEEP=5
- **Entrada**: casilla (1,17) = px (16,272)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **Keyhole** (0xE) ×1: (0,10,23)
    - [ ] **Key** (0x80) ×1: (0,8,23)

### Nivel 0x1F9
- **Direcciones**: L1ptr 0x2E5EB → header 0x30621 · SprPtr 0x2EFF2 → stream 0x3C3F5 · L2ptr 0x2EBEB · GFXslot 0x028E7 · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 13 0F` (spriteGfx=9) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x1 · música 4 · tiempo 300 · Layer2 nivel · paletas BG=6 FG=4 SPR=0 backArea=5
- **Colisión**: 17×27 casillas · 
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **GhostHouseDoor** (0x8D) ×1: (0,0,0)

### Nivel 0x1FA
- **Direcciones**: L1ptr 0x2E5EE → header 0x35ED2 · SprPtr 0x2EFF4 → stream 0x3D56C · L2ptr 0x2EBEE · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 8 pantallas (128 casillas) · modo 0xC · música 4 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 128×27 casillas · SOLID=2
- **Entrada**: casilla (8,22) = px (128,352)
- **Usa sprites grandes**: no
- **Enemigos (3)**:
    - [s] **PSwitch** (0x3E) ×1: (6,102,18)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (1,25,21)
    - [ ] **Sprite 0xE1** (0xE1) ×1: (0,8,0)

### Nivel 0x1FB
- **Direcciones**: L1ptr 0x2E5F1 → header 0x32D09 · SprPtr 0x2EFF6 → stream 0x3CBDC · L2ptr 0x2EBF1 · GFXslot 0x028DF · FGBGslot 0x0293F
- **GFX sprites (SP1-4)**: `00 01 06 11` (spriteGfx=7) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 07` (tilesetFG=5)
- **Propiedades**: ancho 11 pantallas (176 casillas) · modo 0x0 · música 4 · tiempo 400 · Layer2 fondo · paletas BG=6 FG=4 SPR=5 backArea=5
- **Colisión**: 176×27 casillas · SOLID=38 SLOPE=7 SLOPE_STEEP=7
- **Entrada**: casilla (8,21) = px (128,336)
- **Usa sprites grandes**: no
- **Enemigos (17)**:
    - [ ] **BouncingFootball** (0x28) ×2: (7,115,18) (8,142,20)
    - [ ] **Sprite 0x37** (0x37) ×9: (3,49,22) (3,61,23) (4,66,12) (4,72,22) (4,79,21) (5,90,16) …
    - [ ] **Sprite 0x38** (0x38) ×4: (1,18,22) (1,18,17) (9,156,4) (10,171,4)
    - [ ] **Sprite 0xE2** (0xE2) ×1: (10,160,20)
    - [ ] **Sprite 0xE3** (0xE3) ×1: (1,31,20)

### Nivel 0x1FC
- **Direcciones**: L1ptr 0x2E5F4 → header 0x3116F · SprPtr 0x2EFF8 → stream 0x3C6BF · L2ptr 0x2EBF4 · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 4 pantallas (64 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 64×27 casillas · LEDGE_TOP=51 SOLID=14 SLOPE_STEEP=2
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (5)**:
    - [ ] **Sprite 0x5A** (0x5A) ×1: (3,52,24)
    - [ ] **LeftFlyingBlock** (0x83) ×1: (2,37,21)
    - [ ] **Layer3Smasher** (0x89) ×1: (1,18,0)
    - [ ] **Sprite 0xE6** (0xE6) ×1: (0,0,0)
    - [ ] **Sprite 0xF3** (0xF3) ×1: (0,8,0)

### Nivel 0x1FD
- **Direcciones**: L1ptr 0x2E5F7 → header 0x30E6D · SprPtr 0x2EFFA → stream 0x3C5EF · L2ptr 0x2EBF7 · GFXslot 0x028CF · FGBGslot 0x02937
- **GFX sprites (SP1-4)**: `00 01 13 04` (spriteGfx=3) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 0C 1A` (tilesetFG=3)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 1 · tiempo 300 · Layer2 fondo · paletas BG=6 FG=2 SPR=4 backArea=3
- **Colisión**: 32×27 casillas · LEDGE_TOP=12 SOLID=44 SLOPE_STEEP=4
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: no
- **Enemigos (1)**:
    - [s] **PSwitch** (0x3E) ×1: (0,6,23)

### Nivel 0x1FE
- **Direcciones**: L1ptr 0x2E5FA → header 0x39F22 · SprPtr 0x2EFFC → stream 0x3DFE0 · L2ptr 0x2EBFA · GFXslot 0x028C7 · FGBGslot 0x0292F
- **GFX sprites (SP1-4)**: `00 01 12 03` (spriteGfx=1) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 1B 18` (tilesetFG=1)
- **Propiedades**: ancho 10 pantallas (160 casillas) · modo 0x0 · música 3 · tiempo 300 · Layer2 fondo · paletas BG=3 FG=3 SPR=1 backArea=3
- **Colisión**: 160×27 casillas · LEDGE_TOP=105 SOLID=113 SLOPE_STEEP=14
- **Entrada**: casilla (1,19) = px (16,304)
- **Usa sprites grandes**: sí — MagiKoopa (0x1F)
- **Enemigos (22)**:
    - [B] **MagiKoopa** (0x1F) ×1: (2,39,0)
    - [ ] **Sprite 0x32** (0x32) ×6: (1,16,20) (1,29,23) (2,44,23) (4,66,23) (8,132,19) (9,149,23)
    - [ ] **Podoboo** (0x33) ×4: (4,68,16) (5,89,14) (6,110,16) (7,116,16)
    - [ ] **DownFirstWoodenSpike** (0xAC) ×7: (2,32,17) (2,35,17) (4,79,13) (5,83,13) (6,106,16) (7,113,16) …
    - [ ] **UpDownFirstWoodenSpike** (0xAD) ×4: (3,54,19) (3,58,19) (4,79,22) (5,83,22)

### Nivel 0x1FF
- **Direcciones**: L1ptr 0x2E5FD → header 0x30F93 · SprPtr 0x2EFFE → stream 0x3C659 · L2ptr 0x2EBFD · GFXslot 0x028CB · FGBGslot 0x0294B
- **GFX sprites (SP1-4)**: `00 01 13 05` (spriteGfx=2) · **GFX FG/BG [FG1 FG2 BG1 FG3]**: `14 17 19 16` (tilesetFG=8)
- **Propiedades**: ancho 2 pantallas (32 casillas) · modo 0x0 · música 2 · tiempo 300 · Layer2 fondo · paletas BG=7 FG=1 SPR=2 backArea=6
- **Colisión**: 17×27 casillas · SOLID=4
- **Entrada**: casilla (1,22) = px (16,352)
- **Usa sprites grandes**: no
- **Enemigos (2)**:
    - [ ] **GoalTape** (0x7B) ×1: (0,14,23)
    - [ ] **MessageBox** (0xB9) ×1: (0,6,21)

