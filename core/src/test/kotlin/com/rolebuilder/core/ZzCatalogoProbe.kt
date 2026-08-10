package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE: vuelca TODOS los enemigos de dibujo propio del catálogo, cada uno en un
 * nivel donde está puesto de verdad, para poder MIRARLOS de una vez. Nació de las Super
 * Koopa: llevaban tiempo horneándose como una mancha y ningún test lo veía, porque el fallo
 * —heredar del nivel una página de tesela que la rutina no hereda— no cambia ninguna
 * comprobación de código, solo el dibujo. Se salta sola sin `SMW_ROM`.
 */
class ZzCatalogoProbe {

    @Test
    fun `volcar el catalogo entero`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/sprites/cat")
        out.mkdirs()

        val ids = SmwEnemyGraphics.customEnemyIds.sorted()
        val donde = HashMap<Int, Int>()
        for (lv in 0 until 0x200) {
            val puestos = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv) }.getOrNull() ?: continue
            for (id in puestos.map { it.first }) if (id in ids && id !in donde) donde[id] = lv
        }

        for (id in ids) {
            val lv = donde[id] ?: 0x106
            val img = runCatching { SmwEnemyGraphics.customEnemyImage(rom, header, lv, id) }.getOrNull()
            if (img == null) {
                println("%02X %-26s SIN IMAGEN".format(id, SmwSpriteNames.nameOf(id)))
                continue
            }
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until img.height) for (x in 0 until img.width) bi.setRGB(x, y, img.get(x, y))
            ImageIO.write(bi, "png", File(out, "%02X.png".format(id)))
            println("%02X %-26s nivel %03X %s %dx%d".format(
                id, SmwSpriteNames.nameOf(id), lv,
                if (donde.containsKey(id)) "puesto" else "NO-PUESTO", img.width, img.height))
        }
    }
}
