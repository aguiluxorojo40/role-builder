package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwBakedAssets
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE: compara cada enemigo de dibujo propio HORNEADO (que sale siempre del
 * nivel de referencia [SmwBakedAssets.REF_LEVEL]) con el mismo enemigo dibujado EN UN NIVEL
 * DONDE ESTÁ PUESTO DE VERDAD.
 *
 * Es la comprobación que faltaba: el catálogo se verificó nivel a nivel, pero lo que la app
 * enseña en la paleta son los PNG horneados, y esos salen todos del mismo nivel. Si el banco
 * de ese nivel no contiene el dibujo del enemigo, lo horneado es basura por mucho que el
 * catálogo esté bien.
 */
class ZzHorneadoProbe {

    @Test
    fun `lo horneado contra lo que se ve en su nivel`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/horneado")
        out.mkdirs()

        val ids = SmwEnemyGraphics.customEnemyIds.sorted()
        val donde = HashMap<Int, Int>()
        for (lv in 0 until 0x200) {
            val puestos = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv) }.getOrNull() ?: continue
            for (id in puestos.map { it.first }) if (id in ids && id !in donde) donde[id] = lv
        }

        fun guarda(img: com.rolebuilder.core.snes.ArgbImage?, f: File): Boolean {
            if (img == null) return false
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until img.height) for (x in 0 until img.width) bi.setRGB(x, y, img.get(x, y))
            ImageIO.write(bi, "png", f)
            return true
        }

        var iguales = 0
        var distintos = 0
        println("\n=== HORNEADO (nivel %03X) CONTRA SU PROPIO NIVEL ===".format(SmwBakedAssets.REF_LEVEL))
        for (id in ids) {
            val suyo = donde[id]
            val ref = SmwEnemyGraphics.customEnemyImage(rom, header, SmwBakedAssets.REF_LEVEL, id)
            val pro = suyo?.let { SmwEnemyGraphics.customEnemyImage(rom, header, it, id) }
            guarda(ref, File(out, "%02X_ref.png".format(id)))
            guarda(pro, File(out, "%02X_suyo.png".format(id)))
            val mismo = ref != null && pro != null &&
                ref.width == pro.width && ref.height == pro.height &&
                (0 until ref.height).all { y -> (0 until ref.width).all { x -> ref.get(x, y) == pro.get(x, y) } }
            if (suyo == null) {
                println("  %02X %-26s NO ESTA PUESTO EN NINGUN NIVEL".format(id, SmwSpriteNames.nameOf(id)))
            } else if (mismo) {
                iguales++
            } else {
                distintos++
                println("  %02X %-26s DISTINTO  (su nivel %03X)".format(id, SmwSpriteNames.nameOf(id), suyo))
            }
        }
        println("\niguales=%d  DISTINTOS=%d  (de %d)".format(iguales, distintos, ids.size))
    }
}
