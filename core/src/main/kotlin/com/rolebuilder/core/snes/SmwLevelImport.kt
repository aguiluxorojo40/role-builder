package com.rolebuilder.core.snes

import com.rolebuilder.core.engine.platformer.WarpInput
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapWarp
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.PlatformItemMark
import com.rolebuilder.core.model.PlatformItemType
import com.rolebuilder.core.model.PlatformLayers
import com.rolebuilder.core.model.Tileset

/**
 * Convierte un nivel de SMW ya reconstruido ([SnesGameRecipes.SmwLevelMap]) en las piezas
 * del PROYECTO: su [Tileset], su [GameMap] con enemigos/metas/música y sus [MapWarp].
 *
 * Es la parte de "importar un nivel" que no sabe nada de Android ni del editor: no escribe
 * ficheros ni toca el estado de la interfaz, solo traduce datos de la ROM a nuestro modelo.
 * La app se queda con lo que sí es suyo —guardar el PNG del atlas y dar de alta lo devuelto
 * en el editor—, y a cambio estas reglas (qué capa es el fondo, qué casillas de meta caben
 * en el mapa, con qué gesto se entra a cada tubería) quedan cubiertas por tests.
 */
object SmwLevelImport {

    /** Sufijo del nombre del tileset importado, para distinguirlo de los del usuario. */
    private const val TILESET_SUFFIX = " (SMW)"

    /** Prefijo del nombre del mapa importado. */
    private const val MAP_PREFIX = "SMW "

    /**
     * Tileset del proyecto para el atlas [imageFile] del nivel [map]: conserva la colisión
     * REAL de la ROM (solidez, forma de las cuestas), las teselas animadas y las acciones de
     * bloque. Sin esto el nivel se vería igual pero no se JUGARÍA igual.
     */
    fun tilesetOf(id: Int, name: String, imageFile: String, map: SnesGameRecipes.SmwLevelMap): Tileset =
        Tileset(
            id = id,
            name = name + TILESET_SUFFIX,
            image = imageFile,
            tileSize = 16,
            columns = map.columns,
            rows = map.rows,
            passable = map.passable,
            platformSolidity = map.solidity,
            platformSlopeShape = map.slopeShapes,
            animations = map.animations,
            platformBlockActions = map.blockActions,
        )

    /**
     * Mapa del proyecto para el nivel [map]. Las capas van en el orden CANÓNICO de
     * [PlatformLayers]: el fondo (Layer 2) DEBAJO y el primer plano (Layer 1) encima, tenga
     * fondo el nivel o no. Antes esto se decidía en cada sitio de importación por su cuenta
     * y, cuando el nivel no traía fondo, el terreno acababa en la capa del fondo: dos niveles
     * del mismo proyecto guardaban el terreno en capas distintas y las herramientas del
     * editor pintaban en la capa contraria según cuál tuvieras abierto.
     */
    fun gameMapOf(
        name: String,
        map: SnesGameRecipes.SmwLevelMap,
        tilesetId: Int,
        items: List<PlatformItemMark> = emptyList(),
        musicIndex: Int = -1,
    ): GameMap = GameMap(
        id = 0,
        name = MAP_PREFIX + name,
        width = map.mapWidth,
        height = map.mapHeight,
        tilesetId = tilesetId,
        layers = PlatformLayers.layersOf(map.tiles, map.bgTiles, map.mapWidth, map.mapHeight),
        platformEnemies = map.enemies.map { PlatformEnemyMark(spriteId = it.first, x = it.second, y = it.third) },
        platformItems = items,
        platformMusicIndex = musicIndex,
    )

    /**
     * Nombre del sub-nivel [level] dentro del paquete cuyo nivel principal es [mainLevel]:
     * el principal conserva el nombre real del juego y los demás lo llevan con su número de
     * nivel en hexadecimal. Así, al importar un nivel completo, en la lista del editor se ve
     * de un vistazo cuál es la entrada y cuáles las salitas a las que llevan sus tuberías.
     */
    fun subLevelName(mainName: String, mainLevel: Int, level: Int): String =
        if (level == mainLevel) mainName else "$mainName·${level.toString(16).uppercase()}"

    // ───────────────────────────────── meta del nivel ─────────────────────────────────

    /**
     * Convierte las casillas de meta de la ROM ([SmwLevelGoal.goalCells]) en ítems del mapa,
     * descartando las que caen FUERA de un mapa de [width]×[height]. El recorte no es un
     * detalle: [GameMap] admite ítems fuera de rango sin quejarse y el motor no los
     * encontraría nunca, así que el nivel importado sería imposible de superar sin que
     * nada lo delatara.
     */
    fun goalMarks(cells: List<Pair<Int, Int>>, width: Int, height: Int): List<PlatformItemMark> =
        cells.filter { (x, y) -> x in 0 until width && y in 0 until height }
            .map { (x, y) -> PlatformItemMark(PlatformItemType.GOAL, x, y) }

    /**
     * Marcas de META del nivel [level] (cinta/esfera/cerradura reales) recortadas a [width]×[height].
     * Vacío si el nivel no tiene meta —castillos con jefe, casas de Yoshi— o si falla la lectura;
     * en ese caso se avisa por [onError] en vez de tragárselo: un nivel sin meta es un nivel que
     * no se puede superar, y antes eso pasaba en silencio.
     */
    fun readGoalMarks(
        rom: ByteArray,
        header: SnesHeader,
        level: Int,
        width: Int,
        height: Int,
        onError: (Throwable) -> Unit = {},
    ): List<PlatformItemMark> = runCatching {
        goalMarks(SmwLevelGoal.goalCells(rom, SnesGameRecipes.smwHeaderDeltaPublic(header), level), width, height)
    }.onFailure(onError).getOrDefault(emptyList())

    /**
     * Índice de MÚSICA del nivel [level] (de su cabecera), para que el mapa importado toque SU
     * canción real al jugarlo. -1 si no se puede leer (el reproductor cae a la de por defecto).
     */
    fun musicIndex(rom: ByteArray, header: SnesHeader, level: Int): Int =
        runCatching { SnesGameRecipes.smwLevelInfo(rom, header, level)?.musicIndex ?: -1 }.getOrDefault(-1)

    // ──────────────────────────────────── warps ────────────────────────────────────

    /**
     * Gesto con el que se entra a un warp, traducido al que entiende el motor. Son dos enums
     * distintos a propósito ([WarpEnter] describe la ROM, [WarpInput] el motor), pero el mapeo
     * es uno a uno y aquí queda escrito: [MapWarp.input] guarda el ORDINAL de [WarpInput].
     */
    fun warpInputOf(enter: WarpEnter): WarpInput = when (enter) {
        WarpEnter.DOWN -> WarpInput.DOWN
        WarpEnter.UP -> WarpInput.UP
        WarpEnter.SIDE_LEFT -> WarpInput.SIDE_LEFT
        WarpEnter.SIDE_RIGHT -> WarpInput.SIDE_RIGHT
    }

    /**
     * Traduce las bocas de warp REALES de un sub-nivel ([SmwWarpTiles.levelWarps]) a los warps
     * del proyecto, cambiando "nivel de SMW" por "id de mapa" con [mapIdByLevel]. Los destinos
     * que no se han importado se DESCARTAN: un warp a un mapa inexistente se jugaría como una
     * tubería que te tira a la nada.
     */
    fun warpsOf(warps: List<SmwWarpTiles.LevelWarp>, mapIdByLevel: Map<Int, Int>): List<MapWarp> =
        warps.mapNotNull { w ->
            mapIdByLevel[w.destLevel]?.let { destMapId ->
                MapWarp(
                    x = w.xTile,
                    y = w.yTile,
                    input = warpInputOf(w.enter).ordinal,
                    destMapId = destMapId,
                    destX = w.destXTile,
                    destY = w.destYTile,
                )
            }
        }
}
