package com.rolebuilder.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.PlatformLayers
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.Tiles
import java.io.File

/**
 * Estado observable del proyecto abierto en el editor. Todas las mutaciones
 * pasan por aquí; [save] persiste todo con ProjectIo.
 */
class EditorState(val projectDir: File) {

    /** Cuántos pasos de deshacer se guardan (por gesto, no por celda). */
    private val MAX_UNDO = 40

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

    /** Imágenes PNG disponibles en el proyecto (para sprites y tilesets). */
    fun imageNames(): List<String> =
        File(projectDir, ProjectIo.IMAGES_DIR).listFiles { f -> f.extension.lowercase() == "png" }
            .orEmpty()
            .map { it.name }
            .sorted()

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

    /** Guarda un sello/prefab nuevo en el proyecto (para pegarlo luego al construir mapas). */
    fun addStamp(stamp: com.rolebuilder.core.model.MapStamp) {
        updateProject(project.copy(stamps = project.stamps + stamp))
    }

    /** Borra el sello con ese nombre. */
    fun deleteStamp(name: String) {
        updateProject(project.copy(stamps = project.stamps.filterNot { it.name == name }))
    }

    /** Id libre para un nuevo tileset (mayor existente + 1). */
    fun nextTilesetId(): Int = (database.tilesets.maxOfOrNull { it.id } ?: 0) + 1

    /** Registra un tileset nuevo en la base de datos (p. ej. extraído de una ROM). */
    fun addTileset(tileset: com.rolebuilder.core.model.Tileset) {
        updateDatabase(database.copy(tilesets = database.tilesets + tileset))
    }

    /** Reemplaza un tileset existente en la base de datos (mismo id). */
    fun updateTileset(tileset: com.rolebuilder.core.model.Tileset) {
        updateDatabase(
            database.copy(tilesets = database.tilesets.map { if (it.id == tileset.id) tileset else it }),
        )
    }

    /**
     * Crea un nivel de plataformas VACÍO con un suelo de [groundTile] en las dos filas
     * inferiores y lo abre como mapa de inicio. Lo usa el Platform Builder al crear un
     * nivel desde cero.
     *
     * El suelo va en la capa de PRIMER PLANO ([PlatformLayers.FOREGROUND]), que es donde
     * lo ponen los niveles importados de la ROM. Antes se sembraba en la capa 0 —el
     * fondo—, así que un nivel hecho a mano y uno importado guardaban el terreno en capas
     * distintas y las herramientas del editor pintaban en una u otra según el nivel.
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
            layers = PlatformLayers.layersOf(ground, emptyList(), width, height),
        )
        maps[id] = map
        project = project.copy(mapIds = project.mapIds + id, startMapId = id, startX = 2, startY = floorTop - 1)
        currentMapId = id
        dirty = true
        return map
    }

    // ================================================================================
    //  DESHACER / REHACER
    //
    //  Un editor táctil sin deshacer da miedo de usar: un dedo que resbala pinta un
    //  reguero de teselas y la única salida era repintarlo a mano. Con el relleno por
    //  rectángulo, un toque mal dado ya no ensucia tres casillas, sino media pantalla.
    //
    //  Lo barato aquí es que [GameMap], [Project] y [Database] son inmutables: guardar un
    //  estado anterior es guardar TRES REFERENCIAS, no copiar nada. Y como `withTile`
    //  reemplaza solo la lista de la capa tocada, dos instantáneas seguidas comparten todo
    //  lo demás.
    //
    //  El paso se agrupa por GESTO, no por celda: el editor llama a [beginEdit] cuando el
    //  dedo toca y a [commitEdit] cuando lo levanta, así un trazo entero (o un relleno) se
    //  deshace de una vez, que es lo que uno espera.
    // ================================================================================

    private class Snapshot(
        val mapId: Int,
        val map: GameMap?,
        val project: Project,
        val database: Database,
    )

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var pending: Snapshot? = null

    /** ¿Hay algo que deshacer/rehacer? (observable: pinta los botones del editor). */
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private fun snapshot() = Snapshot(currentMapId, maps[currentMapId], project, database)

    /** Empieza un paso deshacible (el editor lo llama al empezar un gesto). */
    fun beginEdit() {
        if (pending == null) pending = snapshot()
    }

    /**
     * Cierra el paso empezado en [beginEdit]. Si el gesto no cambió nada —un toque en el
     * aire, repintar la misma tesela— no apila un paso que no deshace nada.
     */
    fun commitEdit() {
        val before = pending ?: return
        pending = null
        val now = snapshot()
        if (before.map === now.map && before.project === now.project && before.database === now.database) return
        undoStack.addLast(before)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        refreshUndoFlags()
    }

    fun undo(): Boolean = step(undoStack, redoStack)

    fun redo(): Boolean = step(redoStack, undoStack)

    /** Saca el último estado de [from], guarda el actual en [to] y lo restaura. */
    private fun step(from: ArrayDeque<Snapshot>, to: ArrayDeque<Snapshot>): Boolean {
        val target = from.removeLastOrNull() ?: return false
        pending = null // un gesto a medias no sobrevive a un deshacer
        to.addLast(snapshot())
        currentMapId = target.mapId
        if (target.map != null) maps[target.mapId] = target.map else maps.remove(target.mapId)
        project = target.project
        database = target.database
        dirty = true
        refreshUndoFlags()
        return true
    }

    private fun refreshUndoFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    /**
     * Pone TODOS los mapas del proyecto en el orden de capas canónico de plataformas
     * ([PlatformLayers]): fondo en la 0, primer plano en la 1. Es la reparación de los
     * proyectos que ya existen —los niveles creados a mano guardaban el terreno en la capa
     * del fondo— y se aplica al abrir el Platform Builder. Devuelve cuántos mapas cambiaron
     * (0 = no había nada que arreglar, y entonces tampoco se marca el proyecto como sucio).
     */
    fun normalizePlatformLayers(): Int {
        var changed = 0
        maps.keys.toList().forEach { id ->
            val map = maps[id] ?: return@forEach
            val fixed = PlatformLayers.normalized(map, database.tileset(map.tilesetId))
            if (fixed != map) {
                maps[id] = fixed
                changed++
            }
        }
        if (changed > 0) dirty = true
        return changed
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
