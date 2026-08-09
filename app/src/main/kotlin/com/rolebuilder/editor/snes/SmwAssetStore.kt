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
     *   v3: los Koopa CON caparazón 0x04/0x06/0x07 entran en el catálogo curado, y se hornea
     *       la hoja de CAPARAZONES (sprites/shells.png).
     *   v4: el P-Switch (0x3E) entra en el catálogo curado (una columna más en el atlas).
     *   v5: el trampolín (0x2F), que se dibuja como cuadrado de cuatro teselas de 8×8.
     *   v6: la bola de pinchos (0x14) y el Thwimp (0x27), del mismo tipo de dibujo.
     *   v7: los seis de DIBUJO PROPIO que faltaban para el 100% de los tres primeros
     *       niveles (Banzai Bill, los dos Chuck, caja de mensaje, Koopa deslizándose y
     *       bloque volador). Estos NO tocan el atlas: se hornean como sprites grandes
     *       sueltos, y por eso hizo falta atarlo con un segundo test — el que vigilaba
     *       `curatedIds` no veía este caso.
     *   v8: la ESTRELLA (0x76) y el 1-UP (0x78), también de dibujo propio.
     *   v9: los ocho Koopa CON caparazón (0x04-0x0B) se horneaban a MEDIA ALTURA —16×16
     *       en vez de 16×32, y las aladas sin su ala—, así que en modo PROYECTO salía
     *       medio Koopa flotando mientras el modo ROM los enseñaba enteros. Quien horneó
     *       con la v8 o anterior tiene esos ficheros mal y necesita re-hornear.
     *
     * ⚠ El v3 NO es opcional, y conviene entender por qué para no repetirlo: el atlas de
     * enemigos tiene UNA COLUMNA POR ID de `curatedIds`, y el renderer calcula el ancho de
     * columna como `1 / curatedIds.size`. Si se añaden ids sin subir esta versión, el almacén
     * viejo NO se re-hornea (su versión seguiría valiendo) y el atlas cacheado tendría MENOS
     * columnas de las que el renderer da por hechas: se desplazan TODAS y todos los enemigos
     * salen cortados, no solo los nuevos.
     */
    private const val BAKE_VERSION = 9

    /** Carpeta del almacén (se crea al hornear). */
    fun dir(context: Context): File = File(context.filesDir, DIR)

    private fun versionFile(context: Context): File = File(dir(context), ".bake_version")

    /**
     * ¿Ya se horneó con la versión ACTUAL? No basta con que existan los ficheros: un almacén de
     * una versión anterior (p. ej. sin la música) cuenta como NO horneado, para forzar el
     * re-horneado y que aparezcan los assets nuevos.
     */
    fun isBaked(context: Context): Boolean = isBakedAt(dir(context))

    /**
     * Igual que [isBaked] pero sobre una carpeta suelta, SIN depender de Android. Existe para
     * poder testear en JVM la regla que nos costó la musica muda: un almacen de version
     * anterior cuenta como NO horneado.
     */
    fun isBakedAt(root: File): Boolean {
        if (!File(root, "sprites/enemies.png").isFile) return false
        val v = runCatching { File(root, ".bake_version").readText().trim().toIntOrNull() }.getOrNull()
        return v == BAKE_VERSION
    }

    /** Version de horneado que exige [isBakedAt] (para los tests y para sellar). */
    fun bakeVersion(): Int = BAKE_VERSION

    /** ¿Se horneó la MÚSICA (imagen ARAM del banco de nivel) desde la ROM? Para diagnóstico. */
    fun isMusicBaked(context: Context): Boolean = isMusicBakedAt(dir(context))

    /** [isMusicBaked] sobre una carpeta suelta (testeable en JVM). */
    fun isMusicBakedAt(root: File): Boolean = File(root, "music/level.aram").isFile

    /**
     * ESTADO real del almacén, para poder decir al usuario qué falta en vez de fallar en
     * silencio (los cuadros rojos en vez de enemigos son exactamente esto: el atlas no está).
     * Devuelve pares (etiqueta, ¿está?) en el orden en que importan.
     */
    fun status(context: Context): List<Pair<String, Boolean>> = statusAt(dir(context))

    /** [status] sobre una carpeta suelta (testeable en JVM, sin Context ni Bitmap). */
    fun statusAt(root: File): List<Pair<String, Boolean>> {
        val bigCount = bigSpriteFilesAt(root).size
        return listOf(
            "enemigos" to File(root, "sprites/enemies.png").isFile,
            "Mario" to File(root, "sprites/mario.png").isFile,
            "moneda" to File(root, "sprites/coin.png").isFile,
            "jefes ($bigCount)" to (bigCount > 0),
            "música" to isMusicBakedAt(root),
        )
    }

    /**
     * Hornea SOLO la música (imagen ARAM del banco de nivel) desde [rom] al almacén, sin re-hacer
     * el resto de assets. Para el botón "hornear música" del editor: deja el modo proyecto con
     * banda sonora al instante. Devuelve true si la escribió (false si la ROM no la ensambla).
     */
    fun bakeMusic(context: Context, rom: ByteArray): Boolean = runCatching {
        val header = SnesDecoder.parseHeader(rom)
        val delta = header.headerOffset - 0x7FC0
        val aram = SmwMusic.assembleAram(rom, delta, SmwMusic.LEVEL_MUSIC) ?: return false
        if (aram.size != 0x10000) return false
        val dest = File(dir(context), "music/level.aram")
        dest.parentFile?.mkdirs()
        dest.writeBytes(aram)
        true
    }.getOrDefault(false)

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
    fun bigSpriteFiles(context: Context): Map<Int, File> = bigSpriteFilesAt(dir(context))

    /** [bigSpriteFiles] sobre una carpeta suelta (testeable en JVM). */
    fun bigSpriteFilesAt(root: File): Map<Int, File> {
        val d = File(root, "sprites/big")
        if (!d.isDirectory) return emptyMap()
        return d.listFiles().orEmpty().mapNotNull { f ->
            Regex("big_([0-9a-fA-F]+)\\.png").matchEntire(f.name)?.groupValues?.get(1)
                ?.toIntOrNull(16)?.let { it to f }
        }.toMap()
    }

    /** Resultado de UN paso del horneado: qué es, si salió, y por qué no si falló. */
    class BakeStep(val name: String, val written: Int, val ok: Boolean, val detail: String)

    /**
     * Parte de un horneado, paso a paso. Antes [bake] devolvía un `0` mudo cuando algo fallaba
     * y cada bloque interno se tragaba su excepción, así que un horneado roto era
     * indistinguible de una ROM sin datos — y el usuario solo veía cuadros rojos.
     */
    class BakeReport(val steps: List<BakeStep>) {
        val count: Int get() = steps.sumOf { it.written }
        val failures: List<BakeStep> get() = steps.filter { !it.ok }
        val ok: Boolean get() = failures.isEmpty()

        /** Resumen legible para enseñárselo al usuario tal cual. */
        fun summary(): String =
            if (ok) "✅ Horneado completo: $count ficheros"
            else "⚠️ Horneado con fallos ($count ficheros). " +
                failures.joinToString("; ") { "${it.name}: ${it.detail}" }
    }

    /**
     * HORNEA desde [rom] todos los assets de SMW al almacén. Devuelve cuántos ficheros
     * escribió, o 0 si la ROM no es SMW. Es idempotente: sobrescribe.
     * Para saber QUÉ falló y por qué, usa [bakeDetailed].
     */
    fun bake(context: Context, rom: ByteArray): Int = bakeDetailed(context, rom).count

    /**
     * Igual que [bake] pero devolviendo el parte completo: qué pasos salieron, cuántos ficheros
     * escribió cada uno y el MOTIVO del fallo cuando lo hay. Es lo que permite decirle al
     * usuario "el atlas de enemigos no se pudo generar porque X" en vez de callar.
     */
    fun bakeDetailed(context: Context, rom: ByteArray): BakeReport {
        val steps = ArrayList<BakeStep>()
        val header = runCatching { SnesDecoder.parseHeader(rom) }.getOrElse {
            steps.add(BakeStep("cabecera", 0, false, "no se pudo leer la ROM: ${it.message}"))
            return BakeReport(steps)
        }
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

        /** Ejecuta un paso capturando su error, en vez de tragárselo en silencio. */
        fun step(name: String, block: () -> Int) {
            val before = count
            runCatching { block() }
                .onSuccess {
                    val n = count - before
                    steps.add(
                        if (n > 0) BakeStep(name, n, true, "$n ficheros")
                        // Sin excepción pero sin resultado: la ROM no da ese asset. Se dice.
                        else BakeStep(name, 0, false, "esta ROM no produjo ningún dato"),
                    )
                }
                .onFailure {
                    steps.add(BakeStep(name, count - before, false, "${it::class.simpleName}: ${it.message}"))
                }
        }

        // Mario (4 estados), moneda, powerups y el atlas de enemigos.
        step("sprites (Mario, moneda, powerups, enemigos)") {
            for ((rel, img) in SmwBakedAssets.images(rom, header)) writePng(rel, img); count
        }
        // Sprites grandes (jefes y enemigos altos) uno por fichero, como antes.
        step("jefes y enemigos grandes") {
            for ((id, img) in SmwBakedAssets.bigSprites(rom, header)) {
                writePng("sprites/big/big_%02x.png".format(id), img)
            }
            count
        }
        // Efectos de sonido, desde las muestras BRR de la propia ROM.
        step("efectos de sonido") {
            val clips = SmwSfxCatalog.build(rom, header).orEmpty()
            for ((event, clip) in clips) {
                val dest = File(root, "sfx/${event.name.lowercase()}.wav")
                dest.parentFile?.mkdirs()
                dest.writeBytes(com.rolebuilder.player.PlatformerAudio.wavBytes(clip.pcm, clip.sampleRate))
                count++
            }
            count
        }
        step("música (banco de nivel)") {
            val delta = header.headerOffset - 0x7FC0
            val aram = SmwMusic.assembleAram(rom, delta, SmwMusic.LEVEL_MUSIC)
                ?: error("la ROM no ensambla el banco de música de nivel")
            if (aram.size != 0x10000) error("el ARAM salió de ${aram.size} bytes, no 64 KiB")
            val dest = File(root, "music/level.aram")
            dest.parentFile?.mkdirs()
            dest.writeBytes(aram)
            count++
            count
        }
        // Sella la versión solo si algo se escribió (ver isBaked).
        if (count > 0) runCatching { versionFile(context).writeText(BAKE_VERSION.toString()) }
        return BakeReport(steps)
    }
}
