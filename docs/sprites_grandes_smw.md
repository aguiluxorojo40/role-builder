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

## Recetas parciales / pendientes

### Koopa alado / Paratroopa verde · ⚠️ fiel-parcial
Cuerpo apilado (top `0x82` / bottom `0xa0`, paleta 5 → prop `0x0a`) + 2 alas de
`kDrawWingTiles` (tile `0xc6` 16×16; YDisp del juego = −12). Los tiles del Koopa y las
alas son globales (SP1/SP2), así que vale cualquier nivel (0x106).
```
0xc6,-9,-12,16,0x46 ; 0xc6,9,-12,16,0x06 ; 0x82,0,0,16,0x0a ; 0xa0,0,16,16,0x0a
```
Estado: sale un Koopa verde con las alas reales, pero queda un hueco entre alas y
cuerpo. Falta fijar la tesela/frame de reposo exacto del cuerpo del Koopa volador (el
agente se cortó por el límite de sesión). Revisar `Spr0..._FlyingKoopa`/`DrawWingTiles`.

### Thwomp (id 0x26) / Thwimp (0x27) · ⏳ pendiente
Tablas `kSpr026_Thwomp_XDisp[5]={0xfc,0x4,0xfc,0x4,0x0}`, `_YDisp[5]={0x0,0x0,0x10,0x10,0x8}`
(sprite ~32×32: 4 esquinas + centro). Falta transcribir sus `_Tiles/_TileSize/_Prop` y
el nivel donde cargan sus teselas (SP3/SP4). No reconstruido aún.

## Integración pendiente (siguiente fase)
Estas recetas aún NO están en el roster jugable. Para meterlas hace falta: un mapa
`id de sprite → List<OamPart>` en `SmwEnemyGraphics`, atlas de fotogramas de altura
variable, y ajustar el renderer del motor de plataformas para dibujar sprites más
grandes anclados por los pies.
