package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Hornea los SPRITES GRANDES de SMW ([SmwEnemyGraphics.bigSprites]) a
 * `app/src/main/assets/sprites/big/big_<id>.png`, cada uno a su tamaño nativo compuesto
 * desde su receta OAM real (`renderOam`). El renderer del motor los carga por id de
 * sprite y los dibuja anclados por los pies. Reproducible: reejecuta al cambiar recetas.
 *
 * Uso:  gradle :core:bakeSmwBigSprites --args="--rom smw.sfc [--out app/src/main/assets/sprites/big]"
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
    val outDir = File(opts["out"] ?: "app/src/main/assets/sprites/big").also { it.mkdirs() }

    var ok = 0
    for (bs in SmwEnemyGraphics.bigSprites) {
        val img = SmwEnemyGraphics.bigSpriteImage(rom, header, bs.id)
        if (img == null) {
            System.err.println("  AVISO: ${bs.name} (0x${bs.id.toString(16)}) no compuso (¿nivel 0x${bs.level.toString(16)} sin GFX?)")
            continue
        }
        val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        bi.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
        val f = File(outDir, "big_%02x.png".format(bs.id))
        ImageIO.write(bi, "png", f)
        ok++
        println("  + big_%02x.png  %-16s %dx%d  (nivel 0x%X)".format(bs.id, bs.name, img.width, img.height, bs.level))
    }
    println("Horneados $ok/${SmwEnemyGraphics.bigSprites.size} sprites grandes en ${outDir.absolutePath}")
}
