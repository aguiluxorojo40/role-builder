package com.rolebuilder.editor.snes

import android.content.Context
import android.graphics.Bitmap
import com.rolebuilder.core.snes.SmwBakedAssets
import com.rolebuilder.core.snes.SmwMusic
import com.rolebuilder.core.snes.SmwSfxCatalog
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File

/**
 * Almacén EN EL DISPOSITIVO de los assets de SMW.
 *
 * En el repositorio no va ni un byte de Nintendo: solo las coordenadas y el cómo decodificar.
 * Los gráficos de Mario, el atlas de enemigos, los powerups, la moneda, los sprites grandes de
 * jefes y los efectos de sonido se **hornean aquí** a partir de la ROM del usuario, una sola
 * vez, y se guardan en el almacenamiento privado de la app (`filesDir/smw_assets/`).
 *
 * Así no se pierde nada: el modo ROM ya generaba todo en vivo, y el modo PROYECTO (que se
 * juega sin ROM) lee de este almacén. Quien cargue un asset debe usar [open], que mira primero
 * aquí y solo después en los `assets/` empaquetados — que para el material de SMW están vacíos
 * a propósito.
 */
object SmwAssetStore {

    private const val DIR = "smw_assets"

    /**
     * Versión del horneado. SÚBELA al añadir o rehacer assets horneados (nuevos sprites, la
     * imagen ARAM de música…). [isBaked] compara contra ella: si un almacén viejo tiene una
     * versión anterior, se considera OBSOLETO y se re-hornea, para que quien horneó antes de un
     * cambio reciba los assets nuevos sin borrar datos a mano.
     *   v1: sprites + SFX.   v2: + música de nivel (music/level.aram).
     */
    private const val BAKE_VERSION = 2

    /** Carpeta del almacén (se crea al hornear). */
    fun dir(context: Context): File = File(context.filesDir, DIR)

    private fun versionFile(context: Context): File = File(dir(context), ".bake_version")

    /**
     * ¿Ya se horneó con la versión ACTUAL? No basta con que existan los ficheros: un almacén de
     * una versión anterior (p. ej. sin la música) cuenta como NO horneado, para forzar el
     * re-horneado y que aparezcan los assets nuevos.
     */
    fun isBaked(context: Context): Boolean {
        if (!File(dir(context), "sprites/enemies.png").isFile) return false
        val v = runCatching { versionFile(context).readText().trim().toIntOrNull() }.getOrNull()
        return v == BAKE_VERSION
    }

    /** ¿Se horneó la MÚSICA (imagen ARAM del banco de nivel) desde la ROM? Para diagnóstico. */
    fun isMusicBaked(context: Context): Boolean = File(dir(context), "music/level.aram").isFile

    /**
     * Abre el asset [relPath] (p. ej. `sprites/mario.png`): primero del almacén horneado y, si
     * no está, de los `assets/` empaquetados. Null si no aparece en ninguno.
     */
    fun open(context: Context, relPath: String): ByteArray? {
        val baked = File(dir(context), relPath)
        if (baked.isFile) return runCatching { baked.readBytes() }.getOrNull()
        return runCatching { context.assets.open(relPath).use { it.readBytes() } }.getOrNull()
    }

    /** Los sprites grandes horneados (id → PNG), o vacío si no se ha horneado. */
    fun bigSpriteFiles(context: Context): Map<Int, File> {
        val d = File(dir(context), "sprites/big")
        if (!d.isDirectory) return emptyMap()
        return d.listFiles().orEmpty().mapNotNull { f ->
            Regex("big_([0-9a-fA-F]+)\\.png").matchEntire(f.name)?.groupValues?.get(1)
                ?.toIntOrNull(16)?.let { it to f }
        }.toMap()
    }

    /**
     * HORNEA desde [rom] todos los assets de SMW al almacén. Devuelve cuántos ficheros
     * escribió, o 0 si la ROM no es SMW. Es idempotente: sobrescribe.
     */
    fun bake(context: Context, rom: ByteArray): Int = runCatching {
        val header = SnesDecoder.parseHeader(rom)
        val root = dir(context)
        var count = 0

        fun writePng(rel: String, image: com.rolebuilder.core.snes.ArgbImage) {
            val dest = File(root, rel)
            dest.parentFile?.mkdirs()
            val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            bmp.setPixels(image.pixels, 0, image.width, 0, 0, image.width, image.height)
            dest.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            count++
        }

        // Mario (4 estados), moneda, powerups y el atlas de enemigos.
        for ((rel, img) in SmwBakedAssets.images(rom, header)) writePng(rel, img)
        // Sprites grandes (jefes y enemigos altos) uno por fichero, como antes.
        for ((id, img) in SmwBakedAssets.bigSprites(rom, header)) {
            writePng("sprites/big/big_%02x.png".format(id), img)
        }
        // Efectos de sonido, desde las muestras BRR de la propia ROM.
        runCatching {
            val clips = SmwSfxCatalog.build(rom, header).orEmpty()
            for ((event, clip) in clips) {
                val dest = File(root, "sfx/${event.name.lowercase()}.wav")
                dest.parentFile?.mkdirs()
                dest.writeBytes(com.rolebuilder.player.PlatformerAudio.wavBytes(clip.pcm, clip.sampleRate))
                count++
            }
        }
        // MÚSICA del banco de NIVEL (imagen ARAM de 64 KiB, ensamblada de la ROM igual que
        // PlatformerMusic.fromRom). Sin esto, el modo PROYECTO (jugar un mapa importado, sin
        // ROM) se quedaba MUDO: fromAssets busca este fichero horneado y no existía.
        runCatching {
            val delta = header.headerOffset - 0x7FC0
            val aram = SmwMusic.assembleAram(rom, delta, SmwMusic.LEVEL_MUSIC)
            if (aram != null && aram.size == 0x10000) {
                val dest = File(root, "music/level.aram")
                dest.parentFile?.mkdirs()
                dest.writeBytes(aram)
                count++
            }
        }
        // Sella la versión del horneado SOLO si algo se escribió: así isBaked reconoce el
        // almacén como al día y un cambio futuro (BAKE_VERSION mayor) fuerza el re-horneado.
        if (count > 0) runCatching { versionFile(context).writeText(BAKE_VERSION.toString()) }
        count
    }.getOrDefault(0)
}
