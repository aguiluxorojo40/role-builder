package com.rolebuilder.editor.snes

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.snes.ArgbImage
import java.io.File

/**
 * Puente Android para la extracción de assets de SNES. La lógica de decodificación
 * vive en `core/snes` (Kotlin puro); aquí solo se leen los bytes de la ROM desde un
 * `Uri`, se vuelca el buffer ARGB a un `Bitmap` y se guarda como PNG en el proyecto.
 */
object SnesImport {

    /** Lee los bytes de la ROM elegida por el usuario, o null si no se pudo abrir. */
    fun readRomBytes(context: Context, uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

    /** Convierte una imagen ARGB de `core` en un `Bitmap` de Android. */
    fun toBitmap(image: ArgbImage): Bitmap {
        val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        // Los píxeles ya vienen en formato 0xAARRGGBB, idéntico al Color int de Android.
        bmp.setPixels(image.pixels, 0, image.width, 0, 0, image.width, image.height)
        return bmp
    }

    /**
     * Guarda [bitmap] como PNG en images/[fileName] del proyecto y devuelve el
     * nombre de archivo (para referenciarlo desde el Tileset).
     */
    fun saveTilesetPng(projectDir: File, fileName: String, bitmap: Bitmap): String {
        val dest = ProjectIo.imageFile(projectDir, fileName)
        dest.parentFile?.mkdirs()
        dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return fileName
    }

    /** Normaliza un nombre a un archivo PNG seguro (minúsculas, sin espacios). */
    fun sanitizeFileName(name: String): String {
        val base = name.trim().lowercase()
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "snes" }
        return "$base.png"
    }
}
