package com.rolebuilder.core

import com.rolebuilder.core.snes.ArgbImage
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
 * SONDA DESECHABLE: enfrenta lo que de verdad SALE DEL HORNEADO con lo que se ve en el nivel,
 * por las DOS puertas por las que un enemigo llega al dispositivo:
 *
 *  1. `SmwBakedAssets.bigSprites` (los de dibujo propio, `big_<id>.png`).
 *  2. `SmwEnemyGraphics.bakeAtlas` (los curados, una columna por id de `curatedIds`).
 *
 * Existe porque [ZzHorneadoProbe] mide otra cosa: compara `customEnemyImage` en el nivel de
 * referencia contra el nivel propio, o sea el camino de ANTES del arreglo, y por eso nunca da
 * cero. Lo que hay que vigilar es la SALIDA de `bigSprites`, que desde el arreglo ya usa el
 * nivel propio de cada sprite. Se salta sola sin `SMW_ROM`.
 */
class ZzHorneadoRealProbe {

    private val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/horneado-real")

    @Test
    fun `lo que produce el horneado contra su nivel`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        out.mkdirs()

        val donde = HashMap<Int, Int>()
        for (lv in 0 until 0x200) {
            val puestos = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv) }.getOrNull() ?: continue
            for (id in puestos.map { it.first }) if (id !in donde) donde[id] = lv
        }

        // ---- 1. los de DIBUJO PROPIO, tal y como los deja bigSprites ----
        val horneados = SmwBakedAssets.bigSprites(rom, header)
        var iguales = 0
        var distintos = 0
        println("\n=== bigSprites CONTRA EL NIVEL PROPIO DE CADA UNO ===")
        for (id in SmwEnemyGraphics.customEnemyIds.sorted()) {
            val suyo = donde[id]
            val horneado = horneados[id]
            val nombre = SmwSpriteNames.nameOf(id)
            horneado?.let { guarda(it, File(out, "%02X_horneado.png".format(id))) }
            val enSuNivel = suyo?.let { SmwEnemyGraphics.customEnemyImage(rom, header, it, id) }
            when {
                horneado == null -> println("  %02X %-26s NO SE HORNEA".format(id, nombre))
                suyo == null ->
                    println("  %02X %-26s no esta puesto en ningun nivel (queda el de referencia)"
                        .format(id, nombre))
                mismo(horneado, enSuNivel) -> iguales++
                else -> {
                    distintos++
                    guarda(enSuNivel, File(out, "%02X_ensunivel.png".format(id)))
                    println("  %02X %-26s DISTINTO (su nivel %03X)".format(id, nombre, suyo))
                }
            }
        }
        println("dibujo propio: iguales=%d  DISTINTOS=%d".format(iguales, distintos))

        // ---- 2. el ATLAS de curados: se vuelca entero y celda a celda ----
        val (atlas, faltan) = SmwEnemyGraphics.bakeAtlas(rom, header)
        guarda(atlas, File(out, "atlas.png"))
        println("\n=== ATLAS: %d columnas, %d sin grafico ===".format(SmwEnemyGraphics.curatedIds.size, faltan))
        val celda = SmwEnemyGraphics.ATLAS_CELL
        SmwEnemyGraphics.curatedIds.forEachIndexed { idx, id ->
            val cell = ArgbImage(celda, SmwEnemyGraphics.ATLAS_FRAMES * celda)
            var vacia = true
            for (y in 0 until cell.height) for (x in 0 until celda) {
                val px = atlas.get(idx * celda + x, y)
                cell.set(x, y, px)
                if (px ushr 24 != 0) vacia = false
            }
            guarda(cell, File(out, "atlas_%02d_%02X.png".format(idx, id)))
            // Los ids NUEVOS, ampliados: en el tamaño del atlas no se distingue si el sprite
            // está anclado por los pies y centrado, que es lo que descuadra una celda.
            if (id in intArrayOf(0x33, 0x13)) guarda(zoom(cell, 6), File(out, "atlas_zoom_%02X.png".format(id)))
            if (vacia) println("  columna %d (id %02X %s) VACIA".format(idx, id, SmwEnemyGraphics.nameOf(id)))
        }
    }

    private fun mismo(a: ArgbImage, b: ArgbImage?): Boolean =
        b != null && a.width == b.width && a.height == b.height &&
            (0 until a.height).all { y -> (0 until a.width).all { x -> a.get(x, y) == b.get(x, y) } }

    private fun zoom(src: ArgbImage, n: Int): ArgbImage {
        val big = ArgbImage(src.width * n, src.height * n)
        for (y in 0 until big.height) for (x in 0 until big.width) big.set(x, y, src.get(x / n, y / n))
        return big
    }

    private fun guarda(img: ArgbImage?, f: File) {
        if (img == null) return
        val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until img.height) for (x in 0 until img.width) bi.setRGB(x, y, img.get(x, y))
        ImageIO.write(bi, "png", f)
    }
}
