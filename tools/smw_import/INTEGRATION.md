# Integración con el motor Role Builder

Cómo los assets ingeridos de SMW encajan en el formato de proyecto existente, para que
un futuro `SmwImporter` (en Kotlin) los meta en un proyecto jugable.

## El puente: `Tileset`

El modelo actual `core/src/main/kotlin/com/rolebuilder/core/model/Tileset.kt` ya describe
justo lo que produce la ingesta:

```kotlin
data class Tileset(
    val id: Int, val name: String, val image: String,
    val tileSize: Int = 16, val columns: Int = 8, val rows: Int = 8,
    val passable: List<Boolean> = ...,
    val standingTiles: List<Int> = ...,
)
```

Un **bloque Map16 de SMW es 16×16 px** → mapea 1:1 sobre un tile de `Tileset` con
`tileSize = 16`. Es decir: la hoja de bloques (`map16_fg.png`, siguiente paso) se importa
como un `Tileset`, y el `passable`/comportamiento de cada bloque sale de las propiedades
del Map16 (act-as / tipo de bloque).

## Flujo previsto (`SmwImporter`, hermano de `AssetGenerator`)

`core/src/tools/kotlin/com/rolebuilder/core/tools/AssetGenerator.kt` ya genera PNGs con
`ImageIO` y se ejecuta con la tarea Gradle `generateDefaultAssets`. El importador de SMW
seguiría el mismo patrón:

```
tarea gradle :core:importSmw  ->  SmwImporter.main(romPath, outProjectDir)
  1. Reutiliza la lógica de extract_smw.py (portada a Kotlin, o llamada al script)
     para decodificar GFX + paletas + Map16.
  2. Escribe images/tileset_smw.png  (hoja de bloques 16×16)
  3. Escribe database.json con un Tileset { image, tileSize=16, columns, rows, passable }
  4. (fase siguiente) niveles reales -> maps/map_XX.json
```

## Estado por fase

| Fase | Qué | Estado |
|------|-----|--------|
| A | Decodificar GFX 8×8 (3/4/2 bpp) + paletas | ✅ hecho (`extract_smw.py`) |
| B | Ensamblar bloques Map16 16×16 (`0xD8000`) con paleta | ⏳ siguiente |
| C | `SmwImporter` Kotlin → `Tileset` + PNG en un proyecto | ⏳ |
| D | Niveles reales (`kLevelData_Layer1` en `0x5E000`, 512 niveles) | 🔭 requiere portar el decodificador de objetos de `smw/src/smw_*.c` |
| E | Sprites/enemigos por nivel (`kLoadLevel_SpriteDataPtrs` `0x5EC00`) | 🔭 |

## Referencias en `smw` (snesrev/smw)

- `assets/util.py` — `decomp` (LC_LZ2) y traducción de direcciones LoROM.
- `assets/compile_resources.py` — todos los offsets (GFX, Map16, paletas, niveles).
- `assets/smw_assets.h` — catálogo de los 178 assets con puntero + tamaño.
- `src/smw_*.c` — la lógica del juego (renderizador de objetos de nivel, sprites, física)
  que sirve de referencia para portar a `core` en las fases D/E.
