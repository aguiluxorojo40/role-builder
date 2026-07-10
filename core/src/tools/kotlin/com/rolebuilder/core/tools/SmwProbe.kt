package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwMusic
import com.rolebuilder.core.snes.SmwSfxCatalog
import com.rolebuilder.core.snes.SmwSoundFx
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Herramienta de VALIDACIÓN (no se empaqueta en la app) para afinar sonido y sprites
 * de SMW contra la ROM real:
 *
 *  - `--sfx`: resuelve una lista de eventos candidatos (banco+id de puerto de la APU)
 *    a su voz real (instrumento, nota, muestra) y vuelca un WAV de cada uno, para
 *    comprobar que suenan antes de meterlos en el catálogo del juego.
 *  - `--sprites`: renderiza TODOS los ids de la tabla OAM real del juego y marca
 *    cuáles producen un sprite con píxeles (candidatos a añadir al roster).
 *
 * Uso:  gradle :core:probeSmw --args="--rom RUTA.sfc --out DIR [--level 0x105]"
 */
fun main(args: Array<String>) {
    val opts = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (a.startsWith("--")) {
            val v = if (i + 1 < args.size && !args[i + 1].startsWith("--")) args[++i] else "true"
            opts[a.substring(2)] = v
        }
        i++
    }
    val romPath = opts["rom"] ?: run { System.err.println("Falta --rom"); return }
    val rom = File(romPath).readBytes()
    val out = File(opts["out"] ?: "smw_probe_out").also { it.mkdirs() }
    val header = SnesDecoder.parseHeader(rom)
    println("ROM: \"${header.title}\"  ${header.mapping}")
    val level = opts["level"]?.let { parseIntFlexible(it) } ?: 0x105

    val doSfx = opts.containsKey("sfx") || !opts.containsKey("sprites")
    val doSprites = opts.containsKey("sprites") || !opts.containsKey("sfx")

    if (doSfx) probeSfx(rom, header, out)
    if (doSprites) {
        val levels = opts["levels"]?.split(",")?.map { parseIntFlexible(it.trim()) } ?: listOf(level)
        for (lv in levels) probeSprites(rom, header, lv, out)
    }
}

/** Candidatos de SFX: nombre → (banco de puerto APU, id). Verificados en snesrev/smw. */
private val SFX_CANDIDATES = listOf(
    // Ya en el catálogo (control de que siguen resolviendo):
    Triple("jump", 1, 1), Triple("spin_jump", 3, 4), Triple("coin", 3, 1),
    Triple("stomp", 0, 0x13), Triple("kick", 0, 0x03), Triple("powerup", 0, 0x0A),
    // Nuevos candidatos:
    Triple("fireball", 3, 0x06),      // SpawnPlayerFireball
    Triple("springboard", 3, 0x08),   // Spr02F_PortableSpringboard / WallSpringboard
    Triple("hurt", 0, 0x0F),          // DamagePlayer_Hurt
    Triple("cape", 0, 0x0D),          // GiveMarioCape
    Triple("block_bump", 0, 0x01),    // RunPlayerBlockCode / SolidSpriteBlock
    Triple("yoshi_egg", 3, 0x0A),     // PrepareToHatchNormalSpriteYoshiEgg
    Triple("message", 3, 0x22),       // Spr0B9_MessageBox
    Triple("key", 0, 0x1E),           // Spr080_Key
)

private fun probeSfx(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, out: File) {
    println("\n=== SFX (voz real resuelta desde la ARAM de la ROM) ===")
    val bank = SmwSoundFx.extract(rom, header) ?: run { println("  ROM sin banco BRR de SMW"); return }
    val delta = SmwMusic.deltaOf(header)
    val engine = SmwMusic.readNspc(rom, SmwMusic.ENGINE, delta) ?: run { println("  ROM sin motor N-SPC"); return }
    val aram = ByteArray(SmwMusic.ARAM_SIZE)
    SmwMusic.applyBlocks(aram, engine)
    val dir = File(out, "sfx").also { it.mkdirs() }
    println("  %-12s %-5s %-4s %-5s %-6s %-7s %s".format("evento", "banco", "id", "instr", "nota", "srcn", "muestras@22050"))
    for ((name, b, id) in SFX_CANDIDATES) {
        val src = SmwSfxCatalog.SfxSource(b, id)
        val voice = SmwSfxCatalog.resolveVoice(aram, src)
        if (voice == null || voice.instrument < 0) {
            println("  %-12s %-5d 0x%02X  (no resuelve voz)".format(name, b, id))
            continue
        }
        val srcn = SmwSfxCatalog.instrumentSample(aram, voice.instrument)
        val sample = bank.samples.firstOrNull { it.index == srcn }
        val pitchBase = SmwSfxCatalog.instrumentPitchBase(aram, voice.instrument)
        val pitch = SmwSfxCatalog.dspPitch(voice.note, pitchBase)
        val srcRate = if (sample == null || pitch <= 0) SmwSoundFx.NATIVE_SAMPLE_RATE.toFloat()
        else sample.sampleRate * (pitch.toFloat() / SmwSfxCatalog.NATIVE_PITCH)
        val pcm = if (sample == null) ShortArray(0)
        else SmwSfxCatalog.resampleLinear(sample.pcm, srcRate, SmwSfxCatalog.DEFAULT_OUTPUT_RATE.toFloat())
        val peak = pcm.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
        println("  %-12s %-5d 0x%02X  %-5d 0x%02X  %-7d %d (pico %d)".format(
            name, b, id, voice.instrument, voice.note, srcn, pcm.size, peak))
        if (pcm.isNotEmpty()) File(dir, "$name.wav").writeBytes(wav(pcm, SmwSfxCatalog.DEFAULT_OUTPUT_RATE))
    }
    println("  WAVs en ${File(out, "sfx").absolutePath}")
}

private fun probeSprites(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, level: Int, out: File) {
    val lvHex = level.toString(16).uppercase()
    println("\n=== SPRITES (tabla OAM real, nivel 0x$lvHex) ===")
    val painted = ArrayList<Pair<Int, java.awt.image.BufferedImage>>()
    for (id in 0x00..0x53) {
        val img = SmwEnemyGraphics.spriteImageAnyId(rom, header, level, id) ?: continue
        val curated = SmwEnemyGraphics.handles(id)
        val global = SmwEnemyGraphics.isGlobalGraphic(rom, header, id) == true
        if (!curated) println("  0x%02X %s".format(id, if (global) "GLOBAL" else "nivel"))
        painted.add(id to toBI(img))
    }
    if (painted.isEmpty()) { println("  Ningún id pintó (¿nivel sin datos?)"); return }
    // Lámina de contacto etiquetada: id (y * si ya está en el roster) para inspección.
    val cols = 12
    val s = 4
    val cell = 16 * s
    val pad = 14
    val rows = (painted.size + cols - 1) / cols
    val sheet = BufferedImage(cols * cell, rows * (cell + pad), BufferedImage.TYPE_INT_ARGB)
    val g = sheet.createGraphics()
    g.color = java.awt.Color(40, 40, 40); g.fillRect(0, 0, sheet.width, sheet.height)
    painted.forEachIndexed { idx, (id, bi) ->
        val x = (idx % cols) * cell
        val y = (idx / cols) * (cell + pad)
        g.drawImage(scale(bi, s), x, y, null)
        g.color = if (SmwEnemyGraphics.handles(id)) java.awt.Color(120, 220, 120) else java.awt.Color.WHITE
        g.drawString("%02X%s".format(id, if (SmwEnemyGraphics.handles(id)) "*" else ""), x + 2, y + cell + 11)
    }
    g.dispose()
    val f = File(out, "sheet_%03X.png".format(level))
    ImageIO.write(sheet, "png", f)
    println("  ${painted.size} ids pintados. Lámina: ${f.absolutePath}")
}

private fun toBI(img: ArgbImage): BufferedImage {
    val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    bi.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
    return bi
}

private fun scale(bi: BufferedImage, f: Int): BufferedImage {
    val out = BufferedImage(bi.width * f, bi.height * f, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until out.height) for (x in 0 until out.width) out.setRGB(x, y, bi.getRGB(x / f, y / f))
    return out
}

private fun wav(pcm: ShortArray, sampleRate: Int): ByteArray {
    val dataSize = pcm.size * 2
    val b = java.nio.ByteBuffer.allocate(44 + dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    b.put("RIFF".toByteArray()); b.putInt(36 + dataSize); b.put("WAVE".toByteArray())
    b.put("fmt ".toByteArray()); b.putInt(16); b.putShort(1); b.putShort(1)
    b.putInt(sampleRate); b.putInt(sampleRate * 2); b.putShort(2); b.putShort(16)
    b.put("data".toByteArray()); b.putInt(dataSize)
    for (s in pcm) b.putShort(s)
    return b.array()
}

private fun parseIntFlexible(s: String): Int =
    if (s.startsWith("0x") || s.startsWith("0X")) s.substring(2).toInt(16) else s.toInt()
