package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwOverworldLayer3
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Layer 3 del overworld: el rótulo con el nombre del nivel ([SmwOverworldLayer3]).
 * Gated a la ROM local (no versionada); en CI pasa sin hacer nada.
 */
class SmwOverworldLayer3Test {

    @Test
    fun rendersLevelNameBox() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val delta = header.headerOffset - 0x7FC0

        // El rótulo son siempre 19 casillas (38 bytes de la cabecera de stripe).
        val tiles = SmwOverworldLayer3.levelNameTiles(rom, delta, 0x29)
        assertNotNull(tiles, "el rótulo de YOSHI'S ISLAND 1 debe montarse")
        assertEquals(SmwOverworldLayer3.NAME_TILES, tiles.size)
        // No puede salir todo relleno: el nombre ocupa buena parte del rótulo.
        assertTrue(tiles.toSet().size >= 5, "el rótulo lleva glifos de verdad: ${tiles.toList()}")

        // Coherencia con el decodificador de texto: mismo translevel, mismo nombre.
        val name = SmwLevelNames.nameOfTranslevel(rom, delta, 0x29)
        assertTrue(name?.contains("YOSHI") == true, "translevel 0x29 = YOSHI'S ISLAND 1")

        val img = SmwOverworldLayer3.renderLevelName(rom, header, 0x29)
        assertNotNull(img, "el rótulo debe renderizar")
        assertEquals(256, img.width)
        // Hay píxeles pintados (el resto es transparente para componer sobre el mapa).
        assertTrue(img.pixels.count { it ushr 24 != 0 } > 100, "el rótulo pinta texto")

        val dumpDir = File(
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/scratchpad"
        )
        if (dumpDir.isDirectory) {
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            bi.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
            ImageIO.write(bi, "png", File(dumpDir, "ow_layer3_name.png"))
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
