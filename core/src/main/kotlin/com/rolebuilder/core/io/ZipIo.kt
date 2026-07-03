package com.rolebuilder.core.io

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exporta e importa proyectos como archivos .zip para compartirlos.
 * El zip contiene la carpeta del proyecto tal cual (JSON + imágenes),
 * sin las partidas guardadas.
 */
object ZipIo {

    private const val SAVES_DIR = "saves"

    /** Comprime el proyecto en [output]. No incluye la carpeta de partidas. */
    fun exportProject(projectDir: File, output: OutputStream) {
        require(File(projectDir, ProjectIo.PROJECT_FILE).exists()) {
            "La carpeta no contiene un proyecto (${ProjectIo.PROJECT_FILE})"
        }
        ZipOutputStream(output.buffered()).use { zip ->
            projectDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val relative = file.relativeTo(projectDir).invariantSeparatorsPath
                    if (relative.startsWith("$SAVES_DIR/")) return@forEach
                    zip.putNextEntry(ZipEntry(relative))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    /**
     * Descomprime un proyecto en [destDir] (que no debe existir o estar vacío).
     * Valida que el zip contenga un proyecto y rechaza rutas maliciosas
     * (zip-slip). Devuelve el nombre del proyecto importado.
     */
    fun importProject(input: InputStream, destDir: File): String {
        destDir.mkdirs()
        val canonicalDest = destDir.canonicalFile
        try {
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val target = File(destDir, entry.name)
                        if (!target.canonicalFile.toPath().startsWith(canonicalDest.toPath())) {
                            throw IllegalArgumentException("Entrada de zip no permitida: ${entry.name}")
                        }
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            // Validación: debe poder cargarse como proyecto completo.
            val loaded = ProjectIo.loadFull(destDir)
            return loaded.project.name
        } catch (e: Exception) {
            destDir.deleteRecursively()
            throw e
        }
    }
}
