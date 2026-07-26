package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Enlace casilla-del-mapa → nivel ([SmwOverworldLevels]). La prueba de fuego es que los
 * nombres que salen sean los REALES del juego: si la numeración correlativa no fuese el
 * índice de la tabla de nombres, saldrían nombres descolocados o vacíos.
 * Gated a la ROM local (no versionada); en CI pasa sin hacer nada.
 */
class SmwOverworldLevelsTest {

    @Test
    fun mapsMapTilesToRealLevels() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val levels = SmwOverworldLevels.levels(rom, delta)

        assertEquals(92, levels.size, "SMW US vanilla tiene 92 casillas-de-nivel")
        // Numeración correlativa empezando en 1, como hace $04:D7F2.
        assertEquals(1, levels.first().levelNumber)
        assertEquals(92, levels.last().levelNumber)
        assertTrue(levels.map { it.position }.let { it == it.sorted() }, "en orden de barrido")

        // La gran mayoría tiene nombre real; y el nº de nivel es el índice de la tabla de
        // nombres, así que el 0x29 tiene que ser YOSHI'S ISLAND 1 (verificado 1:1 aparte).
        assertTrue(levels.count { it.name != null } >= 85, "casi todas con nombre")
        val yi1 = levels.firstOrNull { it.levelNumber == 0x29 }
        assertTrue(yi1?.name?.contains("YOSHI") == true, "nivel 0x29 = YOSHI'S ISLAND 1: ${yi1?.name}")

        // Reparto entre mapa principal y área de submapas: los dos tienen niveles de verdad.
        val main = SmwOverworldLevels.mainMapLevels(rom, delta)
        val sub = SmwOverworldLevels.submapLevels(rom, delta)
        assertEquals(levels.size, main.size + sub.size)
        assertTrue(main.size >= 10, "el mapa principal tiene niveles: ${main.size}")
        assertTrue(sub.size >= 10, "los submapas tienen niveles: ${sub.size}")

        // Posiciones dentro del mapa de 512×512 al que pertenecen.
        assertTrue(levels.all { it.x in 0..511 && it.y in 0..511 }, "posiciones dentro del mapa")

        println("OWLEVELS " + levels.take(6).joinToString { "${it.levelNumber}:${it.name}" })
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
