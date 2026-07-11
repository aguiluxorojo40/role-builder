package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Renderiza un sprite de SMW ENSAMBLANDO sus entradas OAM reales, tal y como lo dibuja el
 * juego (delega en [SmwEnemyGraphics.renderOam]). Sirve para reconstruir sprites de varias
 * teselas (Koopa alado, para-goomba…) transcribiendo sus tablas de dibujo de snesrev/smw
 * (`kSprXXX_..._Tiles`, `_XDisp`, `_YDisp`, `_TileSize`, `_Prop`) a una lista de entradas.
 *
 * Cada entrada OAM: `charnum,dx,dy,size,prop`
 *   charnum : nº de tesela de la tabla `_Tiles`            (hex 0xC6 o C6)
 *   dx,dy   : desplazamiento CON SIGNO respecto al origen   (dec -9  o hex 0xF7)
 *   size    : 8 (tesela 8×8) | 16 (16×16)                   (de `_TileSize`: 0→8, 2→16)
 *   prop    : byte OAM: bit7=Vflip bit6=Hflip bits3-1=paleta(0-7) bit0=página (hex 0x46)
 *
 * Uso:
 *   gradle :core:composeSmwSprite --args="--rom smw.sfc --level 0x106 --out spr.png \
 *     --spec '0xAA,0,0,16,0x04 ; 0xC6,-11,-9,16,0x46 ; 0xC6,11,-9,16,0x06'"
 *   (o --spec-file receta.txt; admite '#' para comentarios y saltos de línea entre entradas)
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
    val level = parseIntFlex(opts["level"] ?: "0x106")
    val outPath = opts["out"] ?: "sprite.png"
    val scale = opts["scale"]?.toInt() ?: 8
    val specText = opts["spec"] ?: opts["spec-file"]?.let { File(it).readText() }
    ?: run { System.err.println("Falta --spec o --spec-file"); return }

    val parts = specText.split(";", "\n").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line ->
            val p = line.split(",").map { it.trim() }
            require(p.size == 5) { "Entrada OAM inválida (esperado charnum,dx,dy,size,prop): '$line'" }
            SmwEnemyGraphics.OamPart(parseIntFlex(p[0]), signed8(parseIntFlex(p[1])),
                signed8(parseIntFlex(p[2])), p[3].toInt(), parseIntFlex(p[4]))
        }

    val img = SmwEnemyGraphics.renderOam(rom, header, level, parts) ?: run {
        System.err.println("No se pudo componer (¿ROM no SMW vanilla o nivel sin GFX?)"); return
    }
    val out = File(outPath).also { it.parentFile?.mkdirs() }
    ImageIO.write(scaleImg(toBI(img), scale), "png", out)
    println("Sprite compuesto: ${out.absolutePath} (${img.width}x${img.height}px, ${parts.size} entradas OAM)")
}

private fun toBI(img: ArgbImage): BufferedImage {
    val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    bi.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
    return bi
}

private fun scaleImg(bi: BufferedImage, f: Int): BufferedImage {
    val o = BufferedImage(bi.width * f, bi.height * f, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until o.height) for (x in 0 until o.width) o.setRGB(x, y, bi.getRGB(x / f, y / f))
    return o
}

private fun signed8(v: Int): Int = if (v in 0..255 && v > 127) v - 256 else v
private fun parseIntFlex(s: String): Int {
    val t = s.trim()
    return if (t.startsWith("0x") || t.startsWith("0X")) t.substring(2).toInt(16) else t.toInt()
}
