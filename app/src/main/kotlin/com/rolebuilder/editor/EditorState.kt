package com.rolebuilder.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.Tiles
import java.io.File

/**
 * Estado observable del proyecto abierto en el editor. Todas las mutaciones
 * pasan por aquí; [save] persiste todo con ProjectIo.
 */
class EditorState(val projectDir: File) {

    var project by mutableStateOf(ProjectIo.loadProject(projectDir))
        private set
    var database by mutableStateOf(ProjectIo.loadDatabase(projectDir))
        private set

    private val maps = mutableStateMapOf<Int, GameMap>()

    var currentMapId by mutableStateOf(0)
        private set

    var dirty by mutableStateOf(false)
        private set

    init {
        project.mapIds.forEach { id ->
            runCatching { maps[id] = ProjectIo.loadMap(projectDir, id) }
        }
        currentMapId = project.startMapId.takeIf { it in maps } ?: maps.keys.firstOrNull() ?: 0
    }

    val currentMap: GameMap? get() = maps[currentMapId]

    fun map(id: Int): GameMap? = maps[id]

    val mapList: List<GameMap> get() = project.mapIds.mapNotNull { maps[it] }

    /**
     * Imágenes PNG disponibles en el proyecto (para sprites y tilesets), incluidas
     * las de subcarpetas (p. ej. images/graphics/smw/…). Devuelve rutas relativas a
     * images/ con separador "/".
     */
    fun imageNames(): List<String> {
        val imagesDir = File(projectDir, ProjectIo.IMAGES_DIR)
        return imagesDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() == "png" }
            .map { it.relativeTo(imagesDir).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    fun updateProject(newProject: Project) {
        project = newProject
        dirty = true
    }

    fun updateDatabase(newDatabase: Database) {
        database = newDatabase
        dirty = true
    }

    fun updateMap(map: GameMap) {
        maps[map.id] = map
        dirty = true
    }

    fun selectMap(id: Int) {
        if (id in maps) currentMapId = id
    }

    /**
     * Conecta (o desconecta) el borde [edge] del mapa [mapId] con [neighborId].
     * Mantiene el enlace recíproco: el vecino apunta de vuelta por el lado
     * opuesto, y se limpia cualquier conexión anterior que quedara colgando.
     */
    fun setEdge(mapId: Int, edge: Edge, neighborId: Int?) {
        val map = maps[mapId] ?: return
        val previous = edge.get(map)

        // Limpia el recíproco anterior si apuntaba de vuelta a este mapa.
        if (previous != null && previous != neighborId) {
            maps[previous]?.let { old ->
                if (edge.opposite.get(old) == mapId) {
                    maps[previous] = edge.opposite.set(old, null)
                }
            }
        }

        maps[mapId] = edge.set(map, neighborId)
        if (neighborId != null && neighborId != mapId) {
            maps[neighborId]?.let { nb ->
                maps[neighborId] = edge.opposite.set(nb, mapId)
            }
        }
        dirty = true
    }

    fun addMap(name: String, width: Int, height: Int): GameMap {
        val id = (project.mapIds.maxOrNull() ?: 0) + 1
        val map = GameMap.empty(id, name.ifBlank { "Mapa $id" }, width, height, fillTile = Tiles.GRASS)
        maps[id] = map
        project = project.copy(mapIds = project.mapIds + id)
        currentMapId = id
        dirty = true
        return map
    }

    /**
     * Crea un nivel de plataformas nuevo: rejilla vacía con dos filas de suelo
     * ([groundTile]) abajo y el inicio a la izquierda, sobre el suelo. Devuelve el
     * nivel y lo abre.
     */
    fun addPlatformLevel(name: String, width: Int, height: Int, tilesetId: Int, groundTile: Int): GameMap {
        val id = (project.mapIds.maxOrNull() ?: 0) + 1
        val floorTop = height - 2
        val ground = MutableList(width * height) { com.rolebuilder.core.model.EMPTY_TILE }
        for (r in floorTop until height) for (c in 0 until width) ground[r * width + c] = groundTile
        val map = GameMap(
            id = id,
            name = name.ifBlank { "Nivel $id" },
            width = width,
            height = height,
            tilesetId = tilesetId,
            layers = listOf(ground, List(width * height) { com.rolebuilder.core.model.EMPTY_TILE }),
        )
        maps[id] = map
        project = project.copy(mapIds = project.mapIds + id, startMapId = id, startX = 2, startY = floorTop - 1)
        currentMapId = id
        dirty = true
        return map
    }

    /** Añade un mapa ya construido (p. ej. importado de una ROM) y lo abre. */
    fun addImportedMap(map: GameMap): GameMap {
        val id = (project.mapIds.maxOrNull() ?: 0) + 1
        val stored = map.copy(id = id)
        maps[id] = stored
        project = project.copy(mapIds = project.mapIds + id)
        currentMapId = id
        dirty = true
        return stored
    }

    fun deleteMap(id: Int) {
        if (project.mapIds.size <= 1) return
        maps.remove(id)
        project = project.copy(mapIds = project.mapIds - id)
        ProjectIo.deleteMap(projectDir, id)
        if (currentMapId == id) currentMapId = project.mapIds.first()
        dirty = true
    }

    /** Id libre para un nuevo tileset (mayor existente + 1). */
    fun nextTilesetId(): Int = (database.tilesets.maxOfOrNull { it.id } ?: 0) + 1

    /** Registra un tileset nuevo en la base de datos (p. ej. extraído de una ROM). */
    fun addTileset(tileset: com.rolebuilder.core.model.Tileset) {
        updateDatabase(database.copy(tilesets = database.tilesets + tileset))
    }

    /** Reemplaza un tileset existente por su id (p. ej. al editar la colisión). */
    fun updateTileset(tileset: com.rolebuilder.core.model.Tileset) {
        updateDatabase(
            database.copy(tilesets = database.tilesets.map { if (it.id == tileset.id) tileset else it }),
        )
    }

    fun save() {
        ProjectIo.saveProject(projectDir, project)
        ProjectIo.saveDatabase(projectDir, database)
        maps.values.forEach { ProjectIo.saveMap(projectDir, it) }
        projectDir.setLastModified(System.currentTimeMillis())
        dirty = false
    }
}

/** Los cuatro bordes de un mapa, con acceso al campo correspondiente de [GameMap]. */
enum class Edge(val label: String) {
    NORTH("Norte ↑"),
    SOUTH("Sur ↓"),
    WEST("Oeste ←"),
    EAST("Este →");

    val opposite: Edge
        get() = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            WEST -> EAST
            EAST -> WEST
        }

    fun get(map: GameMap): Int? = when (this) {
        NORTH -> map.edgeNorth
        SOUTH -> map.edgeSouth
        WEST -> map.edgeWest
        EAST -> map.edgeEast
    }

    fun set(map: GameMap, neighborId: Int?): GameMap = when (this) {
        NORTH -> map.copy(edgeNorth = neighborId)
        SOUTH -> map.copy(edgeSouth = neighborId)
        WEST -> map.copy(edgeWest = neighborId)
        EAST -> map.copy(edgeEast = neighborId)
    }
}
