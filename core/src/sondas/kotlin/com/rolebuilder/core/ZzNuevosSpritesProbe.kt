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
 * SONDA DESECHABLE: vuelca a PNG los sprites recién portados, EN UN NIVEL DONDE SALEN DE
 * VERDAD (la paleta y el banco de GFX son del nivel, así que sacarlos del nivel equivocado
 * los pinta con colores que no son suyos). Es para MIRARLOS: contar tests en verde no dice
 * si el dibujo es el que debe ser. Se salta sola sin `SMW_ROM`.
 */
class ZzNuevosSpritesProbe {

    @Test
    fun `volcar los sprites nuevos a PNG`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/sprites")
        out.mkdirs()

        val objetivo = listOf(0xB9)
        // Un id puede verse DISTINTO segun el nivel (banco de GFX y paleta son del nivel), asi
        // que para los dudosos se vuelcan varios niveles y se comparan.
        val varios = setOf(0xB9)
        // Para cada id, el primer nivel de la ROM donde está puesto de verdad.
        val donde = HashMap<Int, Int>()
        val todos = HashMap<Int, LinkedHashSet<Int>>()
        for (lv in 0 until 0x200) {
            val ids = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv) }.getOrNull() ?: continue
            for (id in ids.map { it.first }) {
                if (id in objetivo && id !in donde) donde[id] = lv
                if (id in varios) todos.getOrPut(id) { LinkedHashSet() }.add(lv)
            }
        }

        objetivo.filter { it !in donde }.forEach { println("%02X: no aparece en ningún nivel".format(it)) }
        for ((id, lv) in objetivo.mapNotNull { id -> donde[id]?.let { id to it } }) {
            val ajuste = SnesGameRecipes.smwLevelInfo(rom, header, lv)?.spriteGfx?.and(0x0F)
            println("  (nivel %03X: ajuste de GFX de sprites = %s)".format(lv, ajuste))
            val img = SmwEnemyGraphics.customEnemyImage(rom, header, lv, id)
            if (img == null) { println("%02X: SIN IMAGEN (nivel %03X)".format(id, lv)); continue }
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            var opacos = 0
            for (y in 0 until img.height) for (x in 0 until img.width) {
                val c = img.get(x, y)
                bi.setRGB(x, y, c)
                if ((c ushr 24) != 0) opacos++
            }
            val f = File(out, "%02X_%s.png".format(id, SmwSpriteNames.nameOf(id).replace(' ', '_')))
            ImageIO.write(bi, "png", f)
            println("%02X %-24s nivel %03X  %dx%d  %d px opacos  -> %s"
                .format(id, SmwSpriteNames.nameOf(id), lv, img.width, img.height, opacos, f.name))
        }

        // Hoja de VRAM del nivel donde vive la bola: para MIRAR si la tesela que se ha
        // elegido es de verdad la bola, y no un trozo de otro dibujo del mismo banco.
        SmwEnemyGraphics.spriteVramSheet(rom, header, 0x00D, 0x73, (8 + 2) * 16)?.let { v ->
            val bi = BufferedImage(v.width, v.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until v.height) for (x in 0 until v.width) bi.setRGB(x, y, v.get(x, y))
            ImageIO.write(bi, "png", File(out, "vram_00D.png"))
            println("  hoja VRAM del nivel 00D -> vram_00D.png (%dx%d)".format(v.width, v.height))
        }

        for ((id, lvs) in todos) {
            for (lv in lvs.take(6)) {
                val img = SmwEnemyGraphics.customEnemyImage(rom, header, lv, id) ?: continue
                val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
                for (y in 0 until img.height) for (x in 0 until img.width) bi.setRGB(x, y, img.get(x, y))
                ImageIO.write(bi, "png", File(out, "var_%02X_%03X.png".format(id, lv)))
                println("  variante %02X en nivel %03X (ajuste %s)".format(
                    id, lv, SnesGameRecipes.smwLevelInfo(rom, header, lv)?.spriteGfx?.and(0x0F)))
            }
        }
    }
}
