package com.rolebuilder.editor.snes

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesHeader
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

    /**
     * Mapa del mundo (overworld) de SMW renderizado desde la ROM del usuario como [Bitmap],
     * o null si la ROM no es SMW. La decodificación (tierra + capa 1 + GFX/paleta reales)
     * vive en `core` ([SnesGameRecipes.renderOverworldMainMap]); aquí solo se vuelca a Bitmap.
     */
    fun overworldMap(rom: ByteArray, header: SnesHeader): Bitmap? =
        SnesGameRecipes.renderOverworldMainMap(rom, header)?.let { toBitmap(it) }

    /** Overworld como GIF animado (destello real del juego), listo para exportar, o null. */
    fun overworldGif(rom: ByteArray, header: SnesHeader): ByteArray? =
        SnesGameRecipes.overworldMainMapGif(rom, header)

    /** Todo el mundo (mapa principal + submapas) como (nombre, Bitmap), para exportar completo. */
    fun overworldWorld(rom: ByteArray, header: SnesHeader): List<Pair<String, Bitmap>> =
        SnesGameRecipes.renderOverworldWorld(rom, header).map { (name, img) -> name to toBitmap(img) }

    /**
     * EXPORTA [bytes] al almacenamiento del usuario (carpeta Descargas) con [displayName] y
     * [mimeType], para que el asset SALGA de la app (PNG estático o GIF animado). Devuelve la
     * ruta/URI mostrable o null si falla. Usa MediaStore en Android 10+ (sin permisos) y la
     * carpeta pública de Descargas en versiones anteriores.
     */
    fun exportToDownloads(context: Context, displayName: String, mimeType: String, bytes: ByteArray): String? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Descargas/$displayName"
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val dest = File(dir, displayName)
                dest.outputStream().use { it.write(bytes) }
                dest.absolutePath
            }
        }.getOrNull()

    /** Vuelca un [Bitmap] a PNG en memoria (para exportar). */
    fun bitmapToPng(bitmap: Bitmap): ByteArray =
        java.io.ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    /** Normaliza un nombre a un archivo PNG seguro (minúsculas, sin espacios). */
    fun sanitizeFileName(name: String): String {
        val base = name.trim().lowercase()
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "snes" }
        return "$base.png"
    }
}
