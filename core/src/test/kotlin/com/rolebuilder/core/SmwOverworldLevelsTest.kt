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

        // VERIFICACION FUERTE de la correspondencia numero->nivel: los niveles 37 a 42
        // tienen que ser la zona de Yoshi's Island ENTERA y en bloque. Si el indice de la
        // tabla de nombres se desfasara, esto se rompe al instante.
        val yoshiBlock = (37..42).map { n -> levels.first { it.levelNumber == n }.name }
        assertEquals(
            listOf(
                "#1 IGGY'S CASTLE", "YOSHI'S ISLAND 4", "YOSHI'S ISLAND 3",
                "YOSHI'S HOUSE", "YOSHI'S ISLAND 1", "YOSHI'S ISLAND 2",
            ),
            yoshiBlock,
            "los niveles 37-42 son la zona de Yoshi's Island",
        )
        // OJO: Yoshi's Island es el SUBMAPA 1, no el mapa principal — por eso caen en las
        // pantallas 4-7. El mapa principal es Donut Plains / Vanilla Secret / Chocolate.
        assertTrue((37..42).none { n -> levels.first { it.levelNumber == n }.onMainMap },
            "Yoshi's Island vive en el area de submapas")
        assertTrue(levels.first { it.levelNumber == 1 }.onMainMap,
            "los numeros bajos si son del mapa principal")

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
