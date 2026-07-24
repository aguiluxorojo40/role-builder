package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwTitleScreen
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pantalla de TÍTULO reconstruida desde la ROM real (si está en el scratchpad; en CI el test
 * pasa sin hacer nada). Comprueba las tres piezas: GFX de Layer 3 (2bpp), paleta de título y
 * la *stripe image* del logo. Nunca versiona la imagen (derivada de ROM con copyright).
 */
class SmwTitleScreenTest {

    @Test
    fun rendersTitleScreen() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val delta = header.headerOffset - 0x7FC0

        // GFX de Layer 3: los 4 ficheros dan 512 teselas 2bpp.
        val tiles = SmwTitleScreen.layer3Tiles(rom)
        assertNotNull(tiles, "Layer 3 debe decodificar")
        assertEquals(512, tiles.size)
        assertTrue(tiles.count { it != null } >= 500, "casi todas las teselas de Layer 3")

        // Paleta: los 16 colores del título (8-15 y 24-31) no pueden salir todos a cero.
        val pal = SmwTitleScreen.titlePalette(rom, delta)
        assertTrue((8..15).map { pal[it] }.toSet().size >= 4, "paleta principal del título")
        assertTrue((24..31).map { pal[it] }.toSet().size >= 4, "paleta de Layer 3 del título")

        // Tilemap del logo: la stripe image llena una buena parte de las 0x400 casillas.
        val tm = SmwTitleScreen.titleTilemap(rom, delta)
        assertEquals(0x400, tm.size)
        assertTrue(tm.count { it != 0 } > 200, "el logo/marco ocupa el tilemap: ${tm.count { it != 0 }}")

        val img = SmwTitleScreen.render(rom, header)
        assertNotNull(img, "la pantalla de título debe renderizar")
        assertEquals(256, img.width); assertEquals(256, img.height)
        assertTrue(img.pixels.toSet().size >= 6, "el logo tiene varios colores reales")

        val dumpDir = File(
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/scratchpad"
        )
        if (dumpDir.isDirectory) {
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            bi.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
            ImageIO.write(bi, "png", File(dumpDir, "title_kotlin.png"))
            SmwTitleScreen.renderComplete(rom, header)?.let { full ->
                val bf = BufferedImage(full.width, full.height, BufferedImage.TYPE_INT_ARGB)
                bf.setRGB(0, 0, full.width, full.height, full.pixels, 0, full.width)
                ImageIO.write(bf, "png", File(dumpDir, "title_kotlin_full.png"))
            }
        }
    }

    private fun findRom(): ByteArray? {
        val candidates = listOf(
            System.getenv("SMW_ROM"),
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/" +
                "scratchpad/smw_work/rom/Super Mario World (USA).sfc",
        )
        for (p in candidates) if (p != null && File(p).isFile) return File(p).readBytes()
        return null
    }
}
