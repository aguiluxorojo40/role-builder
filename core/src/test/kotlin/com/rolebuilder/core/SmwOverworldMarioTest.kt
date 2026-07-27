package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworldWalk
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MARIO en el mapa del mundo ([SnesGameRecipes.overworldMarioSprite]). Gated a la ROM local.
 *
 * No se comprueba "que se parezca a Mario" (eso es de ojo), sino que las cuatro direcciones
 * producen un 16×16 con píxeles de verdad y que izquierda y derecha salen espejadas — que es
 * como el juego dibuja los lados, con la misma tesela y volteo horizontal.
 */
class SmwOverworldMarioTest {

    @Test
    fun everyFacingDrawsARealSprite() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        for (dir in listOf(
            SmwOverworldWalk.DIR_UP, SmwOverworldWalk.DIR_DOWN,
            SmwOverworldWalk.DIR_LEFT, SmwOverworldWalk.DIR_RIGHT,
        )) {
            val img = SnesGameRecipes.overworldMarioSprite(rom, header, dir)
            assertNotNull(img, "Mario mirando $dir se dibuja")
            assertTrue(img.width == 16 && img.height == 16, "es 16×16")
            val painted = img.pixels.count { (it ushr 24) != 0 }
            assertTrue(painted > 20, "tiene cuerpo (píxeles opacos): $painted")
        }
    }

    @Test
    fun leftAndRightAreMirrorImages() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val left = SnesGameRecipes.overworldMarioSprite(rom, header, SmwOverworldWalk.DIR_LEFT)
        val right = SnesGameRecipes.overworldMarioSprite(rom, header, SmwOverworldWalk.DIR_RIGHT)
        assertNotNull(left); assertNotNull(right)
        // El juego dibuja los dos lados con las mismas teselas volteadas: espejar uno da el otro.
        var matches = 0
        var opaque = 0
        for (y in 0 until 16) for (x in 0 until 16) {
            val a = left.get(x, y)
            val b = right.get(15 - x, y)
            if ((a ushr 24) != 0) {
                opaque++
                if (a == b) matches++
            }
        }
        assertTrue(opaque > 0)
        assertTrue(matches.toDouble() / opaque > 0.9, "izquierda espejada ≈ derecha ($matches/$opaque)")
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
