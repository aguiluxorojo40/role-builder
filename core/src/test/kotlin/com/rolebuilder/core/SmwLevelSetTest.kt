package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelSet
import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Qué huecos de la ROM son NIVELES de verdad. La regla translevel→nivel se testea sin ROM
 * (es aritmética del juego); los recuentos concretos de la ROM US solo si hay `SMW_ROM`.
 */
class SmwLevelSetTest {

    @Test
    fun `la regla translevel a nivel es la de LoadLevelInfo`() {
        // Mapa principal: el translevel ES el nivel (0x001..0x024).
        assertEquals(0x001, SmwLevelSet.levelOfTranslevel(1, onMainMap = true))
        assertEquals(0x024, SmwLevelSet.levelOfTranslevel(0x24, onMainMap = true))
        // Submapas: se le quita 0x24 y se enciende el byte alto → 0x101 en adelante.
        assertEquals(0x101, SmwLevelSet.levelOfTranslevel(0x25, onMainMap = false))
        assertEquals(0x138, SmwLevelSet.levelOfTranslevel(0x5C, onMainMap = false))
        // El corte está EXACTAMENTE en 0x25 (0x24 es el último del mapa principal).
        assertEquals(0x24, SmwLevelSet.LAST_MAIN_MAP_TRANSLEVEL)
        assertEquals(0x001, SmwLevelSet.levelOfTranslevel(0x25, onMainMap = true))
    }

    @Test
    fun `en la ROM US hay 92 niveles con casilla y 215 alcanzables`() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)

        val map = SmwLevelSet.mapLevels(rom, delta)
        assertEquals(92, map.size, "casillas-de-nivel del overworld")
        assertEquals(92, map.map { it.level }.distinct().size, "una casilla, un nivel")
        assertEquals(36, map.count { it.onMainMap }, "mapa principal")

        // Los niveles caen justo en los dos tramos que usa el juego.
        assertTrue(map.all { it.level in 0x001..0x024 || it.level in 0x101..0x138 }, "tramos")
        // Y los slots que nadie referencia quedan FUERA (antes se listaban a mano).
        val levels = map.map { it.level }.toSet()
        for (huerfano in listOf(0x139, 0x13A, 0x13B)) {
            assertTrue(huerfano !in levels, "$huerfano no tiene casilla en el mapa")
        }

        // "translevel >= 0x25" y "está en un submapa" son la MISMA cosa: no es suposición.
        val ow = SmwOverworldLevels.levels(rom, delta)
        assertTrue(
            ow.all { (it.levelNumber > SmwLevelSet.LAST_MAIN_MAP_TRANSLEVEL) == !it.onMainMap },
            "el corte 0x25 coincide con el salto a submapas",
        )

        val reachable = SmwLevelSet.reachableLevels(rom, header)
        assertEquals(215, reachable.size, "mapa + sub-niveles por salidas")
        assertTrue(levels.all { it in reachable }, "todo nivel de mapa es alcanzable")
        // Es MUCHO menos que los 512 huecos: ese era el problema de medir sobre ellos.
        assertTrue(reachable.size < SnesGameRecipes.SMW_LEVEL_COUNT / 2, "215 << 512")
    }

    private fun findRom(): ByteArray? {
        val candidates = listOf(System.getenv("SMW_ROM"), "/home/user/role-builder/smw.sfc")
        for (p in candidates) if (p != null && File(p).isFile) return File(p).readBytes()
        return null
    }
}
