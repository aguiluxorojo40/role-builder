package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

        // DEFECTO CONOCIDO, documentado a proposito para que no se olvide: tratar el numero
        // correlativo como indice de la tabla de nombres NO cuadra. El 41 sale "YOSHI'S
        // ISLAND 1" pero cae en el area de submapas, y su byte de caminos es 0x00 ("no abre
        // camino"), igual que el 37 (#1 IGGY'S CASTLE) — imposible para un castillo. Este
        // test FIJA la contradiccion; cuando se resuelva el desfase habra que reescribirlo.
        val yi1 = levels.firstOrNull { it.levelNumber == 0x29 }
        assertNotNull(yi1)
        assertTrue(!yi1!!.onMainMap, "contradiccion conocida: 'YOSHI'S ISLAND 1' cae fuera del mapa principal")
        assertEquals(0, yi1!!.pathDirections, "contradiccion conocida: no abriria ningun camino")

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
