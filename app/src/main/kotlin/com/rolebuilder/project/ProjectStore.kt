package com.rolebuilder.project

import android.content.Context
import android.net.Uri
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.ZipIo
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMode
import java.io.File

/** Resumen de un proyecto guardado en el dispositivo. */
data class ProjectSummary(
    val dir: File,
    val name: String,
    val lastModified: Long,
    val mode: GameMode = GameMode.ARPG,
)

/**
 * Los proyectos viven en filesDir/projects/<carpeta>/ con el formato de
 * ProjectIo. Los proyectos nuevos se copian de assets/default_project.
 */
object ProjectStore {

    private const val TEMPLATE_ASSET = "default_project"
    private const val STANDALONE_ASSET = "standalone_game"

    fun root(context: Context): File =
        File(context.filesDir, "projects").apply { mkdirs() }

    fun list(context: Context): List<ProjectSummary> =
        root(context).listFiles { file -> file.isDirectory }
            .orEmpty()
            .mapNotNull { dir ->
                runCatching {
                    val project = ProjectIo.loadProject(dir)
                    ProjectSummary(dir, project.name, dir.lastModified(), project.mode)
                }.getOrNull()
            }
            .sortedByDescending { it.lastModified }

    /** Crea un proyecto nuevo con nombre y modo de motor. */
    fun create(context: Context, name: String, mode: GameMode = GameMode.ARPG): File {
        val dir = File(root(context), "p${System.currentTimeMillis()}")
        if (mode == GameMode.PLATFORMER) {
            // El Platform Builder arranca LIMPIO, sin los assets del ARPG: un nivel
            // vacío y una base de datos sin tilesets. Los gráficos y tilesets de SMW
            // se traen luego con "Importar de ROM" y rellenan los selectores.
            createEmptyPlatformer(dir, name)
        } else {
            copyAssetDir(context, TEMPLATE_ASSET, dir)
            val project = ProjectIo.loadProject(dir).copy(name = name.ifBlank { "Mi aventura" }, mode = mode)
            ProjectIo.saveProject(dir, project)
        }
        dir.setLastModified(System.currentTimeMillis())
        return dir
    }

    /**
     * Crea un proyecto de plataformas vacío: nivel sin tiles (rejilla ancha de
     * scroll lateral), base de datos sin tilesets ni enemigos, y carpeta de
     * imágenes vacía. El editor lo rellena al importar gráficos de la ROM de SMW.
     */
    private fun createEmptyPlatformer(dir: File, name: String) {
        val w = 48
        val h = 15
        val map = com.rolebuilder.core.model.GameMap.empty(
            id = 1, name = "Nivel 1", width = w, height = h, tilesetId = 1, fillTile = EMPTY_TILE,
        )
        val project = com.rolebuilder.core.model.Project(
            name = name.ifBlank { "Mi plataformas" },
            mode = GameMode.PLATFORMER,
            startMapId = 1,
            startX = 2,
            startY = h - 3,
            mapIds = listOf(1),
        )
        ProjectIo.saveProject(dir, project)
        ProjectIo.saveDatabase(dir, com.rolebuilder.core.model.Database())
        ProjectIo.saveMap(dir, map)
        File(dir, ProjectIo.IMAGES_DIR).mkdirs()
    }

    fun delete(dir: File) {
        dir.deleteRecursively()
    }

    fun touch(dir: File) {
        dir.setLastModified(System.currentTimeMillis())
    }

    /**
     * Copia al proyecto las imágenes de la plantilla que le falten (p. ej.
     * sprites añadidos en versiones nuevas, como la espada). No sobreescribe.
     */
    fun ensureDefaultImages(context: Context, projectDir: File) {
        val imagesAsset = "$TEMPLATE_ASSET/${ProjectIo.IMAGES_DIR}"
        val imagesDir = File(projectDir, ProjectIo.IMAGES_DIR).apply { mkdirs() }
        context.assets.list(imagesAsset).orEmpty().forEach { name ->
            val dest = File(imagesDir, name)
            if (!dest.exists()) {
                context.assets.open("$imagesAsset/$name").use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    /**
     * Modo "juego independiente": si el APK incluye un proyecto en
     * assets/standalone_game, lo instala en filesDir y devuelve su carpeta;
     * la app arranca entonces directamente en el juego, sin editor.
     */
    fun installStandaloneIfPresent(context: Context): File? {
        val present = runCatching {
            context.assets.list(STANDALONE_ASSET)?.contains(ProjectIo.PROJECT_FILE) == true
        }.getOrDefault(false)
        if (!present) return null
        val dir = File(context.filesDir, STANDALONE_ASSET)
        if (!File(dir, ProjectIo.PROJECT_FILE).exists()) {
            copyAssetDir(context, STANDALONE_ASSET, dir)
        }
        return dir
    }

    /** Exporta un proyecto como .zip al destino elegido por el usuario (SAF). */
    fun export(context: Context, projectDir: File, target: Uri) {
        context.contentResolver.openOutputStream(target)?.use { output ->
            ZipIo.exportProject(projectDir, output)
        } ?: error("No se pudo abrir el destino")
    }

    /** Importa un .zip como proyecto nuevo y devuelve su nombre. */
    fun import(context: Context, source: Uri): String {
        val dest = File(root(context), "p${System.currentTimeMillis()}")
        val input = context.contentResolver.openInputStream(source) ?: error("No se pudo leer el archivo")
        return input.use { ZipIo.importProject(it, dest) }
    }

    private fun copyAssetDir(context: Context, assetPath: String, dest: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            // Es un archivo.
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            dest.mkdirs()
            for (child in children) {
                copyAssetDir(context, "$assetPath/$child", File(dest, child))
            }
        }
    }
}
