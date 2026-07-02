package com.rolebuilder.core.io

import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Project
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Lee y escribe proyectos en disco. Estructura de carpeta:
 *
 *   <dir>/project.json
 *   <dir>/database.json
 *   <dir>/maps/map_<id>.json
 *   <dir>/images/  (imágenes PNG)
 */
object ProjectIo {

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    const val PROJECT_FILE = "project.json"
    const val DATABASE_FILE = "database.json"
    const val MAPS_DIR = "maps"
    const val IMAGES_DIR = "images"

    fun mapFileName(mapId: Int): String = "map_$mapId.json"

    fun saveProject(dir: File, project: Project) {
        dir.mkdirs()
        File(dir, PROJECT_FILE).writeText(json.encodeToString(Project.serializer(), project))
    }

    fun loadProject(dir: File): Project =
        json.decodeFromString(Project.serializer(), File(dir, PROJECT_FILE).readText())

    fun saveDatabase(dir: File, database: Database) {
        dir.mkdirs()
        File(dir, DATABASE_FILE).writeText(json.encodeToString(Database.serializer(), database))
    }

    fun loadDatabase(dir: File): Database =
        json.decodeFromString(Database.serializer(), File(dir, DATABASE_FILE).readText())

    fun saveMap(dir: File, map: GameMap) {
        val mapsDir = File(dir, MAPS_DIR).also { it.mkdirs() }
        File(mapsDir, mapFileName(map.id)).writeText(json.encodeToString(GameMap.serializer(), map))
    }

    fun loadMap(dir: File, mapId: Int): GameMap =
        json.decodeFromString(GameMap.serializer(), File(File(dir, MAPS_DIR), mapFileName(mapId)).readText())

    fun deleteMap(dir: File, mapId: Int) {
        File(File(dir, MAPS_DIR), mapFileName(mapId)).delete()
    }

    fun imageFile(dir: File, name: String): File = File(File(dir, IMAGES_DIR), name)

    /** Carga el proyecto completo en memoria (para ejecutar el juego). */
    fun loadFull(dir: File): LoadedProject {
        val project = loadProject(dir)
        val database = loadDatabase(dir)
        val maps = project.mapIds.associateWith { loadMap(dir, it) }
        return LoadedProject(dir, project, database, maps)
    }
}

/** Proyecto completo cargado en memoria. */
data class LoadedProject(
    val dir: File,
    val project: Project,
    val database: Database,
    val maps: Map<Int, GameMap>,
)
