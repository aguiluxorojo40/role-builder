package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * BUSCADOR de paleta/nivel para los sprites grandes: para cada [SmwEnemyGraphics.bigSprites],
 * localiza los niveles donde el sprite REALMENTE aparece (su lista de sprites lo contiene) y
 * renderiza su receta OAM en cada uno, montando una lámina etiquetada por nivel. Así se elige
 * a ojo el nivel de horneado con los COLORES canónicos (p.ej. dientes/patas blancos, no rosas).
 *
 * Uso:  gradle :core:scoutBigSpritePalettes --args="--rom smw.sfc --out DIR [--only 0x9F]"
 */
fun main(args: Array<String>) {
    val opts = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (a.startsWith("--")) opts[a.substring(2)] =
            if (i + 1 < args.size && !args[i + 1].startsWith("--")) args[++i] else "true"
        i++
    }
    val rom = File(opts["rom"] ?: run { System.err.println("Falta --rom"); return }).readBytes()
    val header = SnesDecoder.parseHeader(rom)
    val out = File(opts["out"] ?: "palette_scout").also { it.mkdirs() }
    val only = opts["only"]?.let { if (it.startsWith("0x")) it.substring(2).toInt(16) else it.toInt() }

    for (bs in SmwEnemyGraphics.bigSprites) {
        if (only != null && bs.id != only) continue
        val levels = SmwEnemyGraphics.levelsWithSprite(rom, header, bs.id, max = 12)
        println("0x%02X %-16s aparece en %d niveles: %s".format(
            bs.id, bs.name, levels.size, levels.joinToString(" ") { "0x" + it.toString(16).uppercase() }))
        if (levels.isEmpty()) {
            println("   (no aparece en ninguna lista de sprites; se queda con el nivel actual 0x${bs.level.toString(16)})")
            continue
        }
        // Renderiza el sprite en cada nivel candidato y monta una lámina.
        val tiles = ArrayList<Pair<Int, BufferedImage>>()
        for (lv in levels) {
            val img = SmwEnemyGraphics.renderOam(rom, header, lv, bs.parts) ?: continue
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            bi.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
            tiles.add(lv to bi)
        }
        if (tiles.isEmpty()) continue
        val s = 4
        val cw = (tiles.maxOf { it.second.width } * s) + 24
        val ch = (tiles.maxOf { it.second.height } * s) + 22
        val cols = minOf(6, tiles.size)
        val rows = (tiles.size + cols - 1) / cols
        val sheet = BufferedImage(cols * cw, rows * ch, BufferedImage.TYPE_INT_ARGB)
        val g = sheet.createGraphics()
        g.color = Color(45, 45, 45); g.fillRect(0, 0, sheet.width, sheet.height)
        tiles.forEachIndexed { idx, (lv, bi) ->
            val x = (idx % cols) * cw + 4
            val y = (idx / cols) * ch + 16
            val scaled = BufferedImage(bi.width * s, bi.height * s, BufferedImage.TYPE_INT_ARGB)
            for (yy in 0 until scaled.height) for (xx in 0 until scaled.width)
                scaled.setRGB(xx, yy, bi.getRGB(xx / s, yy / s))
            g.drawImage(scaled, x, y, null)
            g.color = Color.YELLOW
            g.drawString("0x" + lv.toString(16).uppercase(), x, y - 3)
        }
        g.dispose()
        val f = File(out, "scout_%02x_%s.png".format(bs.id, bs.name.replace("[^A-Za-z]".toRegex(), "")))
        ImageIO.write(sheet, "png", f)
        println("   lámina: ${f.absolutePath}")
    }
}
