# Auditoría de cobertura de datos de SMW (¿queda algún resquicio?)

Contraste de **toda** la data por nivel/sprite de Super Mario World (inventario tomado de
`assets/compile_resources.py` de snesrev/smw, que lista cada tabla de la ROM) frente a lo
que extraemos y documentamos. Estado: ✅ completo · ⚠️ parcial · ❌ falta · N/A no aplica.

## Datos POR NIVEL (0x000–0x1FF)

| Dato | Dirección ROM (SNES) | Estado | Dónde |
|------|----------------------|--------|-------|
| Punteros Layer 1 (`kLevelData_Layer1`) | $05:E000 (3B×512) | ✅ | dirección + **parseado**: colisión, salidas de pantalla |
| Objetos de Layer 1 (tilemap) | (banco $05/$06…) | ✅¹ | `SmwLayer1` reconstruye el Map16 (objetos comunes) |
| Punteros Layer 2 (`kLevelData_Layer2`) | $05:E600 (3B×512) | ✅ | dirección + tipo (fondo/nivel) |
| Contenido de Layer 2 (fondo/objetos) | (banco $0C…) | ⚠️ | solo puntero/tipo; el tilemap de fondo no se vuelca |
| Punteros de sprites (`kLoadLevel_SpriteDataPtrs`) | $05:EC00 (2B×512) | ✅ | dirección + **lista parseada** |
| Lista de sprites (enemigos) | banco $07 | ✅ | id, nombre, posición (pantalla,x,y), **extra bits** |
| Byte de cabecera de sprites | (1er byte de la lista) | ✅ | memoria + buoyancy |
| Cabecera primaria (5 bytes) | (inicio de Layer 1 data) | ✅ | ancho, modo, música, tiempo, paletas, GFX settings, tileset |
| Cabecera secundaria F000 | $05:F000 (1B×512) | ✅ | Y de entrada + scroll Layer 2 |
| Cabecera secundaria F200 | $05:F200 (1B×512) | ✅ | X de entrada + pos FG/BG + Layer 3 |
| Cabecera secundaria F400 | $05:F400 (1B×512) | ✅ | Y de Layer 1 y Layer 2 |
| Cabecera secundaria F600 | $05:F600 (1B×512) | ✅ | **vertical**, layout, pantalla entrada, no-Yoshi |
| Salidas de pantalla (ExtObj00) | (en objetos de Layer 1) | ✅ | pantalla → nivel/sublevel destino (+ 2ª entrada) |
| GFX de sprites SP1-4 | $00:A8C3 + 4·setting | ✅ | los 4 ficheros GFX |
| GFX FG/BG [FG1 FG2 BG1 FG3] | $00:A92B + 4·tileset | ✅ | los 4 ficheros GFX |
| Nombre de nivel (`kLevelNames`) | $04:A0FC (2B×256) | ✅ | **decodificado**: ensamblado de 3 trozos (`SmwLevelNames`), texto por nivel en `niveles_smw.md` |
| Música de nivel | (índice en cabecera) | ✅ | índice 0-7 + reproductor N-SPC/S-DSP real |

¹ Limitación: **niveles VERTICALES** — `SmwLayer1` solo parsea horizontales; en verticales
la colisión/salidas salen vacías, pero ahora quedan **identificados** por la bandera de la
cabecera secundaria (marcados `VERTICAL` en `niveles_smw.md`). Salas de jefe / modos sin
Layer 1 (modos 9/11/16) tampoco tienen objetos, es correcto.

## Datos GLOBALES (no por nivel) — usados o documentados

| Dato | Dirección | Estado |
|------|-----------|--------|
| Paletas (Sky/BG/FG/Objetos/Player/Sprites/…) | $00:B0A0–B7xx | ✅ direcciones + ensamblado real de CGRAM por nivel |
| Map16 (FG por tileset, Castle/Rope/Underground/GhostHouse) | $0D:8000… | ✅ direcciones + render de tileset |
| Tabla OAM de sprites genéricos | $01 (banco) | ✅ (render de sprites 16×16) |
| Recetas OAM de sprites grandes | — | ✅ 11 reconstruidos (`docs/sprites_grandes_smw.md`) |
| Física / powerups / colisión de bloques | varias | ✅ (motor de plataformas) |
| Música (bancos N-SPC + muestras BRR) | $0E:… / $3E400… | ✅ (S-DSP + secuenciador en Kotlin) |
| SFX (muestras BRR + secuencias) | $05:56xx… | ✅ (catálogo real) |

## Resquicios HONESTOS que quedan (nada crítico para construir)

1. **Contenido de Layer 2 de fondo**: documentamos puntero y tipo (fondo/nivel), no el tilemap
   del fondo en sí.
2. **Niveles verticales**: identificados, pero su Layer 1 (colisión/salidas) aún no se parsea
   (otra disposición de pantallas).
3. **Comportamiento por sprite** (tweaker `$166E`): se usa para la paleta; no se vuelca entero
   por enemigo en el informe.

**Resuelto** (antes pendiente): el **nombre de nivel en texto** ya se decodifica. No hacía falta
la fuente gráfica: el nombre se ensambla de 3 trozos de un pool ($04:9AC5) vía tres tablas de
offsets ($04:9C91/9CCF/9CED), y el word por translevel ($04:A0FC) empaqueta los tres índices
(i1=byte alto, i2/i3=nibbles del byte bajo). El mapa tesela→carácter es directo (0x00-0x19=A-Z,
0x1F=espacio, 0x5A='#', 0x5D=apóstrofo, 0x63-0x6C=0-9). Implementado en `SmwLevelNames` y volcado
por nivel en `niveles_smw.md` (94 nombres). El bloqueo anterior ("hace falta la fuente") era falso:
venía del bug SNES→PC que daba teselas basura.

Todo lo demás relevante para **construir y contrastar niveles** (direcciones, cabeceras,
GFX/paletas, colisión, entrada, salidas→sublevels, enemigos con posición y extra bits, sprites
grandes) está cubierto y en `docs/niveles_smw.md` + `docs/enemigos_por_nivel.md`.

## Fuera del alcance actual (otro dominio, no "datos de nivel")

Repasado el inventario COMPLETO de la ROM, lo único no documentado son cosas que NO son
datos por nivel:

- **OVERWORLD / mapa del mundo**: tilemaps de submapa (`kMap16Data_OverworldLayer1`,
  `OverworldLayer2Tilemap`), caminos y **eventos** (`kLmEventStuff1-4`,
  `kLoadOverworldLayer1AndEvents`, `kLoadLevel_DATA_05D608` = evento por nivel), sprites de
  mapa (`kLoadOverworldSprites`), warps del Star Road (`kOwStarPipeWarp_*`), paletas OW. Es un
  dominio propio (qué casilla del mapa es cada nivel, cómo se conectan, switch palaces…).
- **Niveles especiales / edge**: 6 salas de entrada (`kEntranceData`), Chocolate Island 2
  especial (`kChoclateIsland2`), pantalla de créditos y roll-call (`kRollCallData`,
  `kGameMode25_ShowEnemyRollcallScreen`).
- **Animación de teselas de nivel** (`kLevelTileAnimations_FrameData`, monedas/bloques/agua):
  ya soportada en el motor (teselas animadas), no volcada en el informe por nivel.

Conclusión: para **datos de nivel + enemigos** no queda nada; los 4 puntos de arriba están
acotados y lo pendiente de verdad es el **overworld**, que es harina de otro costal.
