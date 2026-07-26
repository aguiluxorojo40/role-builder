package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworld
import com.rolebuilder.core.snes.SmwOverworldSprites
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Catálogo de sprites del overworld ([SmwOverworldSprites]). Gated a la ROM local; en CI pasa
 * sin hacer nada.
 */
class SmwOverworldSpritesTest {

    /** Las sub-paletas del catálogo salen de los bits 3-1 del byte de atributos OAM. */
    @Test
    fun paletteMatchesOamAttributeBits() {
        // Valores de atributos leídos de las rutinas: Boo $34, cartel $30, piraña $32,
        // Koopa Kid $22, pájaro $36, humo $30, Bowser $00.
        fun palOf(attr: Int) = (attr shr 1) and 7
        assertEquals(2, palOf(0x34), "Boo → sub-paleta 2")
        assertEquals(0, palOf(0x30), "cartel de BOWSER → sub-paleta 0")
        assertEquals(1, palOf(0x32), "planta piraña → sub-paleta 1")
        assertEquals(1, palOf(0x22), "Koopa Kid → sub-paleta 1")
        assertEquals(3, palOf(0x36), "pájaro azul → sub-paleta 3")
        assertEquals(0, palOf(0x00), "Bowser → sub-paleta 0")
        // El catálogo tiene que coincidir con eso.
        fun def(id: Int) = SmwOverworldSprites.CATALOG.first { it.id == id }
        assertEquals(2, def(SmwOverworld.SPRITE_BOO).palette)
        assertEquals(0, def(SmwOverworld.SPRITE_BOWSER_SIGN).palette)
        assertEquals(1, def(SmwOverworld.SPRITE_PIRANHA).palette)
        assertEquals(3, def(SmwOverworld.SPRITE_BLUE_BIRD).palette)
    }

    /** Tamaño: `cr = 0` → 8×8 (Draw8x8) y `cr = 1` → 16×16 (GenericOverworldSpriteDraw). */
    @Test
    fun sizesMatchDrawRoutine() {
        fun def(id: Int) = SmwOverworldSprites.CATALOG.first { it.id == id }
        // Cartel y humo van por Draw8x8; el resto por el dibujo genérico de 16×16.
        assertTrue(!def(SmwOverworld.SPRITE_BOWSER_SIGN).size16, "el cartel se dibuja en 8×8")
        assertTrue(!def(SmwOverworld.SPRITE_SMOKE).size16, "el humo se dibuja en 8×8")
        assertTrue(def(SmwOverworld.SPRITE_BOO).size16, "el Boo se dibuja en 16×16")
        assertTrue(def(SmwOverworld.SPRITE_CLOUD).size16, "la nube se dibuja en 16×16")
    }

    /** Solo el cartel y los Boos tienen sitio fijo; el resto los coloca el juego al vuelo. */
    @Test
    fun onlySlotSpritesHaveFixedPositions() {
        val fixed = SmwOverworldSprites.CATALOG.filter { !it.spawned }.map { it.id }.toSet()
        assertEquals(setOf(SmwOverworld.SPRITE_BOWSER_SIGN, SmwOverworld.SPRITE_BOO), fixed)
    }

    @Test
    fun rendersSpriteSheet() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val img = SmwOverworldSprites.renderSheet(rom, header)
        assertNotNull(img, "la hoja de sprites debe renderizar")
        // Cada sprite tiene que pintar algo: si una tesela estuviera mal, saldría en blanco.
        val cell = img.width / SmwOverworldSprites.CATALOG.size
        SmwOverworldSprites.CATALOG.forEachIndexed { i, def ->
            var painted = 0
            for (y in 0 until img.height) for (x in i * cell until (i + 1) * cell) {
                if (img.get(x, y) ushr 24 != 0) painted++
            }
            assertTrue(painted >= 8, "${def.name} debe dibujar píxeles: $painted")
        }

        val dumpDir = File(
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/scratchpad"
        )
        if (dumpDir.isDirectory) {
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            bi.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
            ImageIO.write(bi, "png", File(dumpDir, "ow_sprite_catalog.png"))
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
