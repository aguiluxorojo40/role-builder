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

    fun addMap(name: String, width: Int, height: Int): GameMap {
        val id = (project.mapIds.maxOrNull() ?: 0) + 1
        val map = GameMap.empty(id, name.ifBlank { "Mapa $id" }, width, height, fillTile = Tiles.GRASS)
        maps[id] = map
        project = project.copy(mapIds = project.mapIds + id)
        currentMapId = id
        dirty = true
        return map
    }

    fun deleteMap(id: Int) {
        if (project.mapIds.size <= 1) return
        maps.remove(id)
        project = project.copy(mapIds = project.mapIds - id)
        ProjectIo.deleteMap(projectDir, id)
        if (currentMapId == id) currentMapId = project.mapIds.first()
        dirty = true
    }

    fun save() {
        ProjectIo.saveProject(projectDir, project)
        ProjectIo.saveDatabase(projectDir, database)
        maps.values.forEach { ProjectIo.saveMap(projectDir, it) }
        projectDir.setLastModified(System.currentTimeMillis())
        dirty = false
    }
}
