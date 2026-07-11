# Recetas OAM de sprites GRANDES de SMW (reconstrucción fiel)

Sprites de Super Mario World de VARIAS teselas, reconstruidos **exactamente como los
dibuja el juego** (transcribiendo sus tablas OAM reales de la reimplementación
`snesrev/smw`, sin inventar píxeles). Cada receta es una lista de entradas OAM para
[`SmwEnemyGraphics.renderOam`](../core/src/main/kotlin/com/rolebuilder/core/snes/SmwEnemyGraphics.kt)
/ la tarea `:core:composeSmwSprite`.

## Formato de entrada OAM: `charnum,dx,dy,size,prop`
- `charnum`: nº de tesela (tabla `_Tiles`).
- `dx,dy`: desplazamiento con signo respecto al origen del sprite.
- `size`: `8` (8×8) o `16` (16×16), de `_TileSize` (0→8, 2→16).
- `prop`: byte OAM (bit7=Vflip, bit6=Hflip, bits3-1=paleta 0-7 → filas CGRAM 8-15, bit0=página).

Renderizar/verificar:
```
./gradlew :core:composeSmwSprite --args="--rom smw.sfc --level 0xNNN --out spr.png --spec '...'"
```

## Recetas verificadas

### ParaGoomba — goomba volador (id 0x10) · nivel 0x106 · ✅ verificado
De `Spr010_ParaGoomba_GoombaWingDraw` + `GenericGFXRtDraw1Tile16x16`. Cuerpo goomba +
2 alas blancas/azules.
```
0xAA,0,0,16,0x04 ; 0xC6,-11,-9,16,0x46 ; 0xC6,11,-9,16,0x06
```

### Chargin' Chuck (id 0x91) · nivel 0x106 · ✅ verificado
De `Spr091_CharginChuck_Draw` (frame de reposo `table1602=3`). Cabeza + cuerpo simétrico.
Página 1 (SP3/SP4): solo en niveles que suben su GFX (0x106, 0x00A, 0x024, 0x1CB, 0x002).
```
0x06,0,-8,16,0x4B ; 0x26,-4,0,16,0x0B ; 0x26,4,0,16,0x4B
```
Se ve: jugador con casco verde y visera, hombreras marrón-rojizas, torso blanco con
cordones naranjas, guantes blancos.

### Magikoopa / Kamek (id 0x1F) · nivel 0x101 · ✅ verificado
Cuerpo por `GenericGFXRtDraw2Tiles16x16sStacked` (`TilesOffset[0x1F]=0x73`, par de reposo
0xA0/0xC0) + varita de `Spr01F_MagiKoopa_BE86`. `Sprite166EVals[0x1F]=0x4F`. Página 1
(solo en niveles con su GFX; 0x101 confirmado).
```
0xa0,0,0,16,0x4F ; 0xc0,0,16,16,0x4F ; 0x99,16,16,8,0x4F
```
Se ve: mago con sombrero puntiagudo, pico naranja, túnica magenta y estrella de la varita.
(Color magenta = paleta estática de la ROM; el juego la recolorea en el fade-in.)

### Koopa alado / Paratroopa verde (id 0x08) · cualquier nivel (0x106) · ✅ verificado
De `SprXXX_Generic_Spr0to13Gfx`: cuerpo apilado (`GenericGFXRtDraw2Tiles16x16sStacked`)
+ UNA sola ala (de perfil) por `DrawWingTiles_ParaKoopaEntry`. Frame con ala abierta:
top `0x82` / bottom `0xa2`, paleta 5 → prop `0x0a`; ala `0xc6` 16×16 en (+9,+3), prop `0x06`.
Tiles globales (SP1/SP2). Clave del ajuste: el cuerpo se dibuja subido 15px pero el ala no,
así que el ala va relativa al top del cuerpo en `dy=+3` (no −12) → queda pegada al caparazón.
```
0x82,0,0,16,0x0a ; 0xa2,0,16,16,0x0a ; 0xc6,9,3,16,0x06
```
Se ve: Koopa naranja de perfil (izquierda), caparazón y patas verdes, un ala blanca con
contorno azul barriendo hacia arriba, pegada al caparazón. Para mirar a la derecha, el
juego espeja cuerpo+ala con Hflip (prop cuerpo `0x4a`, ala `0x46`, X del ala `−9`).

### Thwomp (id 0x26) · nivel 0x101 · ✅ verificado
De `Spr026_Thwomp_Draw` (`src/smw_01.c`). En reposo dibuja 4 esquinas 16×16 (la tesela
central de boca `0xc8` solo aparece al caer). `Sprite166EVals[0x26]=0x33` → prop base `0x03`
(página 1 SP3/SP4, paleta 1); esquinas derechas con Hflip (`0x43`). Solo en niveles de
castillo con su GFX (0x101; en 0x106 sale paleta rosa).
```
0x8e,-4,0,16,0x03 ; 0x8e,4,0,16,0x43 ; 0xae,-4,16,16,0x03 ; 0xae,4,16,16,0x43
```
Se ve: bloque de piedra gris con cara enfadada (cejas/ojos + boca de dientes apretados).
Variante "cayendo": añadir la boca abierta `0xc8,0,8,16,0x03`.

### Banzai Bill (id 0x9F) · nivel 0x105 · ✅ verificado
De `Spr09F_BanzaiBill_Draw` (`src/smw_02.c`) con sus tablas `_Tiles/_XDisp/_YDisp/_Prop`
(banco $02): rejilla 4×4 de teselas 16×16 (64×64). `Prop=0x33` (página 1 SP4=GFX20,
paleta 1); las 2 teselas de la cola llevan Vflip (`0xb3`). Se verifica en 0x105 (Yoshi's
Island 2), donde de verdad aparece.
```
0x80,0,0,16,0x33 ; 0x82,16,0,16,0x33 ; 0x84,32,0,16,0x33 ; 0x86,48,0,16,0x33
0xa0,0,16,16,0x33 ; 0x88,16,16,16,0x33 ; 0xce,32,16,16,0x33 ; 0xee,48,16,16,0x33
0xc0,0,32,16,0x33 ; 0xc2,16,32,16,0x33 ; 0xce,32,32,16,0x33 ; 0xee,48,32,16,0x33
0x8e,0,48,16,0x33 ; 0xae,16,48,16,0x33 ; 0x84,32,48,16,0xb3 ; 0x86,48,48,16,0xb3
```
Se ve: bala negra gigante, ojo blanco enfadado, boca roja con dientes y cola metálica.

### Mega Mole (id 0xBF) · nivel 0x1D · ✅ verificado
De `Spr0BF_MegaMole_Draw` (`src/smw_03.c`, `_Tiles/_TileDispX/_TileDispY`): 2×2 (32×32).
Fotograma de andar (frame 0) mirando a la derecha (prop base 1, sin Hflip). Página 1
(SP4=GFX20). El fotograma 1 usa 0xCA/0xCC/0xEA/0xEC (patas alternas).
```
0xc6,0,-16,16,0x01 ; 0xc8,16,-16,16,0x01 ; 0xe6,0,0,16,0x01 ; 0xe8,16,0,16,0x01
```
Se ve: topo marrón peludo, panza clara y garras.

### Blargg (id 0xA8) · nivel 0x10A · ✅ verificado
De `Spr0A8_Blargg_Draw` (`src/smw_03.c`, forma emergida `table00c2==4`,
`_Tiles/_XDisp/_YDisp/_Prop`). Mirando a la derecha (r2=1 → `prop 0x05`, sin Hflip): 2
teselas arriba + 3 abajo (la mandíbula se alarga a la derecha). Página 1 (SP4=GFX04),
paleta 2.
```
0xa2,-8,-8,16,0x05 ; 0xa4,8,-8,16,0x05 ; 0xc2,-8,8,16,0x05 ; 0xc4,8,8,16,0x05 ; 0xa6,24,8,16,0x05
```
Se ve: cabeza de dinosaurio de lava rojo/naranja con boca abierta de dientes y ojo.

### Pokey (id 0x70) · nivel 0xC7 · ✅ verificado
De `Spr070_Pokey_Draw` (`src/smw_02.c`), que apila segmentos verticalmente (cabeza tesela
0x8A con cara, cuerpo 0xE8). Reconstrucción estática de 5 segmentos (cabeza + 4 cuerpos)
cada 16px; `prop 0x05` (página 1 SP4=GFX09, paleta 2).
```
0x8a,0,-16,16,0x05 ; 0xe8,0,0,16,0x05 ; 0xe8,0,16,16,0x05 ; 0xe8,0,32,16,0x05 ; 0xe8,0,48,16,0x05
```
Se ve: cactus del desierto amarillo, segmentos redondos apilados y cabeza con ojos/boca.

### Thwimp (id 0x27) · ⏳ pendiente
Rutina en `src/smw_01.c:3915`. No reconstruido (baja prioridad).

## Integración en el roster jugable
Ya integradas: cada `BigSprite` de `SmwEnemyGraphics.bigSprites` se hornea con
`:core:bakeSmwBigSprites` a `app/src/main/assets/sprites/big/big_<id>.png` a su tamaño
nativo, y el motor de plataformas los carga por id (`PlatformerActivity`) y los dibuja
anclados abajo-centro (`PlatformerRenderer`). Para añadir uno nuevo: mete su receta en
`bigSprites`, rehornea, y aparece solo.
