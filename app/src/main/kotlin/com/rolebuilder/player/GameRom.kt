package com.rolebuilder.player

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * La ROM que usa **el juego** (no el editor), guardada en el almacenamiento privado de la app.
 *
 * El editor pide la ROM cada vez porque ahí se está trasteando con varias. El juego, en
 * cambio, quiere una y la misma siempre: la eliges una vez y a partir de ahí el título arranca
 * solo. Se copia dentro de `filesDir`, que es privado de la app — no se comparte con nadie ni
 * sale del dispositivo, igual que los assets horneados.
 */
object GameRom {

    private const val NAME = "smw_game.sfc"

    /** Fichero donde vive la ROM del juego (exista o no todavía). */
    fun file(context: Context): File = File(context.filesDir, NAME)

    /** ¿Ya hay una ROM elegida? */
    fun isPresent(context: Context): Boolean = file(context).let { it.isFile && it.length() > 0 }

    /**
     * Copia la ROM apuntada por [uri] al almacén del juego. Devuelve el fichero, o null si no
     * se pudo leer (permiso revocado, fichero borrado, etc.).
     */
    fun store(context: Context, uri: Uri): File? = runCatching {
        val dest = file(context)
        context.contentResolver.openInputStream(uri)!!.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        dest.takeIf { it.length() > 0 }
    }.getOrNull()
}
