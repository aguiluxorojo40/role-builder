package com.rolebuilder.project

import android.content.Context
import com.rolebuilder.core.io.ProjectIo
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    /** Comprime el proyecto en [out], excluyendo las partidas guardadas. */
    fun exportZip(projectDir: File, out: OutputStream) {
        val base = projectDir.canonicalFile
        ZipOutputStream(out.buffered()).use { zip ->
            base.walkTopDown()
                .filter { it.isFile }
                .filterNot { it.relativeTo(base).invariantSeparatorsPath.startsWith("saves/") }
                .forEach { file ->
                    zip.putNextEntry(ZipEntry(file.relativeTo(base).invariantSeparatorsPath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    /**
     * Importa un proyecto desde un zip creado con [exportZip] y devuelve su
     * carpeta. Valida cada entrada (rutas fuera del destino, extensiones,
     * número de archivos y tamaño descomprimido) y que el resultado sea un
     * proyecto cargable; si algo falla, borra lo extraído y lanza
     * [IllegalArgumentException] con un mensaje legible.
     */
    fun importZip(context: Context, input: InputStream): File {
        val dir = File(root(context), "p${System.currentTimeMillis()}")
        dir.mkdirs()
        try {
            var totalBytes = 0L
            var entries = 0
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    entries++
                    require(entries <= MAX_ZIP_ENTRIES) { "demasiados archivos" }
                    val name = entry.name
                    require(!name.startsWith("/") && !name.contains("..")) { "ruta no permitida ($name)" }
                    require(name.endsWith(".json") || name.endsWith(".png")) { "tipo de archivo no permitido ($name)" }
                    val dest = File(dir, name)
                    require(dest.canonicalPath.startsWith(dir.canonicalPath + File.separator)) {
                        "ruta no permitida ($name)"
                    }
                    dest.parentFile?.mkdirs()
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            totalBytes += read
                            require(totalBytes <= MAX_ZIP_BYTES) { "supera los ${MAX_ZIP_BYTES / (1024 * 1024)} MB" }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
            // Tiene que ser un proyecto completo y cargable.
            ProjectIo.loadFull(dir)
            dir.setLastModified(System.currentTimeMillis())
            return dir
        } catch (e: Exception) {
            dir.deleteRecursively()
            throw IllegalArgumentException("Zip de proyecto no válido: ${e.message}", e)
        }
    }

    private const val MAX_ZIP_ENTRIES = 500
    private const val MAX_ZIP_BYTES = 50L * 1024 * 1024

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
