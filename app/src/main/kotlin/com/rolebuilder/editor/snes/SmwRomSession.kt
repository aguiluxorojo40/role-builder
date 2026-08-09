package com.rolebuilder.editor.snes

import android.content.Context
import java.io.File

/**
 * LA ROM DE LA SESIÓN, en un solo sitio.
 *
 * Antes cada pantalla se la cargaba por su cuenta y luego la tiraba:
 *
 *  - el raíl del editor tenía su propio selector, leía los bytes, auto-importaba niveles
 *    y **se deshacía de ellos**;
 *  - el diálogo AVANZADO guardaba los suyos en un `remember` **suyo**, que muere al
 *    cerrar el diálogo;
 *  - y para jugar o abrir el navegador de assets se escribía una copia en `cacheDir`
 *    cada vez, sin reaprovecharla nunca.
 *
 * Resultado: entrabas en AVANZADO, salías, y volver a entrar te obligaba a buscar la ROM
 * en el selector de archivos otra vez. Con tres sitios cargándola por separado, el
 * trabajo se hacía a trompicones sin que hubiera un fallo concreto al que señalar.
 *
 * Aquí la ROM se carga UNA vez y queda disponible para todos mientras la app viva. La
 * copia en disco va al almacenamiento privado de la app —no a `cacheDir`, que el sistema
 * borra cuando le hace falta sitio—, así que también sobrevive a cerrar y abrir la app.
 *
 * Sigue sin haber ni un byte de Nintendo en el repositorio: esto es la ROM **del
 * usuario**, en **su** dispositivo, en la carpeta privada de la app — el mismo sitio del
 * que ya salían los assets horneados. [forget] la borra si quiere quitarla.
 */
object SmwRomSession {

    private const val FILE = "rom/current.sfc"

    /** Bytes en memoria: se rellena al cargarla o al leerla del disco la primera vez. */
    @Volatile
    private var cached: ByteArray? = null

    /** Nombre original del fichero, solo para enseñarlo en la interfaz. */
    @Volatile
    var displayName: String = ""
        private set

    // Las variantes `*At(root)` no tocan Android: existen para poder probar en JVM toda
    // esta lógica (que es la que decide si hay que volver a pedirle la ROM al usuario),
    // igual que hace [SmwAssetStore]. Las de Context son envoltorios de una línea.

    private fun fileAt(root: File): File = File(root, FILE)
    private fun nameFileAt(root: File): File = File(root, "$FILE.name")

    /** ¿Hay una ROM disponible sin volver a pedírsela al usuario? */
    fun isAvailable(context: Context): Boolean = isAvailableAt(context.filesDir)

    /** [isAvailable] sobre una carpeta suelta (testeable en JVM). */
    fun isAvailableAt(root: File): Boolean = cached != null || fileAt(root).isFile

    /**
     * La ROM de la sesión, o null si el usuario no ha cargado ninguna todavía. Lee de
     * memoria; si no la tiene, la recupera del disco (y entonces sí la deja cacheada).
     */
    fun get(context: Context): ByteArray? = getAt(context.filesDir)

    /** [get] sobre una carpeta suelta (testeable en JVM). */
    fun getAt(root: File): ByteArray? {
        cached?.let { return it }
        val f = fileAt(root)
        if (!f.isFile) return null
        val bytes = runCatching { f.readBytes() }.getOrNull() ?: return null
        cached = bytes
        if (displayName.isBlank()) {
            displayName = runCatching { nameFileAt(root).readText().trim() }.getOrNull().orEmpty()
        }
        return bytes
    }

    /**
     * Guarda la ROM recién elegida para el resto de la sesión (y para las siguientes).
     * [name] es solo el nombre que se le enseña al usuario.
     */
    fun remember(context: Context, bytes: ByteArray, name: String) =
        rememberAt(context.filesDir, bytes, name)

    /** [remember] sobre una carpeta suelta (testeable en JVM). */
    fun rememberAt(root: File, bytes: ByteArray, name: String) {
        cached = bytes
        displayName = name
        runCatching {
            val f = fileAt(root)
            f.parentFile?.mkdirs()
            // Escritura ATÓMICA: un corte a media escritura dejaría una ROM truncada que
            // luego pasaría por válida y daría basura por todas partes. Es el mismo fallo
            // que ya costó partidas corruptas antes de arreglar SaveIo.
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(f)) { f.writeBytes(bytes); tmp.delete() }
            nameFileAt(root).writeText(name)
        }
    }

    /** Olvida la ROM: la borra del disco y de memoria. */
    fun forget(context: Context) = forgetAt(context.filesDir)

    /** [forget] sobre una carpeta suelta (testeable en JVM). */
    fun forgetAt(root: File) {
        cached = null
        displayName = ""
        runCatching { fileAt(root).delete(); nameFileAt(root).delete() }
    }

    /**
     * Un FICHERO con la ROM, para las pantallas que reciben una ruta por `Intent`
     * (el reproductor y el navegador de assets). Devuelve el del almacén, así que ya no
     * hace falta ir escribiendo copias sueltas en `cacheDir` cada vez que se juega.
     */
    fun asFile(context: Context): File? = asFileAt(context.filesDir)

    /**
     * Tira SOLO la copia en memoria, sin tocar el disco. Sirve para probar el caso que
     * motiva todo esto —volver a la app y que la ROM siga ahí—, que es justo lo que no se
     * puede comprobar si la caché sigue caliente.
     */
    internal fun dropMemoryCacheForTests() {
        cached = null
        displayName = ""
    }

    /** [asFile] sobre una carpeta suelta (testeable en JVM). */
    fun asFileAt(root: File): File? {
        val f = fileAt(root)
        if (f.isFile) return f
        val bytes = cached ?: return null
        return runCatching {
            f.parentFile?.mkdirs()
            f.writeBytes(bytes)
            f
        }.getOrNull()
    }
}
