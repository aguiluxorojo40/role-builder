package com.rolebuilder.project

import android.content.Context
import com.rolebuilder.core.io.ProjectIo
import java.io.File

/** Resumen de un proyecto guardado en el dispositivo. */
data class ProjectSummary(
    val dir: File,
    val name: String,
    val lastModified: Long,
)

/**
 * Los proyectos viven en filesDir/projects/<carpeta>/ con el formato de
 * ProjectIo. Los proyectos nuevos se copian de assets/default_project.
 */
object ProjectStore {

    private const val TEMPLATE_ASSET = "default_project"

    fun root(context: Context): File =
        File(context.filesDir, "projects").apply { mkdirs() }

    fun list(context: Context): List<ProjectSummary> =
        root(context).listFiles { file -> file.isDirectory }
            .orEmpty()
            .mapNotNull { dir ->
                runCatching {
                    val project = ProjectIo.loadProject(dir)
                    ProjectSummary(dir, project.name, dir.lastModified())
                }.getOrNull()
            }
            .sortedByDescending { it.lastModified }

    /** Crea un proyecto nuevo desde la plantilla y le pone nombre. */
    fun create(context: Context, name: String): File {
        val dir = File(root(context), "p${System.currentTimeMillis()}")
        copyAssetDir(context, TEMPLATE_ASSET, dir)
        val project = ProjectIo.loadProject(dir).copy(name = name.ifBlank { "Mi aventura" })
        ProjectIo.saveProject(dir, project)
        dir.setLastModified(System.currentTimeMillis())
        return dir
    }

    fun delete(dir: File) {
        dir.deleteRecursively()
    }

    fun touch(dir: File) {
        dir.setLastModified(System.currentTimeMillis())
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
