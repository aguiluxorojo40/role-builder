package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * META de los niveles ([SmwLevelGoal]). Es el eslabón que hace que un nivel se pueda
 * "superar", y con eso el mapa del mundo dispare su evento. Gated a la ROM local.
 */
class SmwLevelGoalTest {

    @Test
    fun realLevelsHaveTheirGoalTape() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0

        // YOSHI'S ISLAND 1 y 2: cinta de meta en la casilla (302, 23); YI3 en la (318, 23).
        for ((level, x) in listOf(0x105 to 302, 0x106 to 302, 0x103 to 318)) {
            val goals = SmwLevelGoal.goalsOf(rom, delta, level)
            assertEquals(1, goals.size, "el nivel ${level.toString(16)} tiene una meta")
            assertEquals(SmwLevelGoal.GOAL_TAPE, goals[0].spriteId, "es la cinta de meta")
            assertEquals(x, goals[0].xTile, "columna de la meta en ${level.toString(16)}")
            assertEquals(23, goals[0].yTile, "fila de la meta en ${level.toString(16)}")
        }

        // Un castillo se acaba derrotando al jefe y una casa no se acaba: NO llevan meta.
        // Que salga vacío es lo correcto — no hay que inventarles una.
        assertTrue(SmwLevelGoal.goalsOf(rom, delta, 0x101).isEmpty(), "#1 IGGY'S CASTLE sin cinta")
        assertTrue(SmwLevelGoal.goalsOf(rom, delta, 0x104).isEmpty(), "YOSHI'S HOUSE sin cinta")
    }

    @Test
    fun goalCoversAColumnSoItCannotBeJumpedOver() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val cells = SmwLevelGoal.goalCells(rom, delta, 0x105)
        assertEquals(SmwLevelGoal.GOAL_HEIGHT_TILES, cells.size, "la meta cubre una columna")
        assertTrue(cells.all { it.first == 302 }, "toda la columna en la misma x")
        // De la base (23) hacia arriba, sin salirse del mapa.
        assertEquals((19..23).toList(), cells.map { it.second }.sorted())
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
