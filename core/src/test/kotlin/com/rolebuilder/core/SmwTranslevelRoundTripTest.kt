package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Ida y vuelta translevel ↔ leveldata ([SmwLevelNames.levelOfTranslevel]). Es el enlace entre
 * una casilla del overworld y el nivel que se juega, así que un desfase aquí mandaría al
 * jugador a otro nivel. Sin ROM: es aritmética pura.
 */
class SmwTranslevelRoundTripTest {

    @Test
    fun everyMapTranslevelRoundTrips() {
        for (t in 0x00..0x5F) {
            val level = SmwLevelNames.levelOfTranslevel(t)
            assertEquals(t, level?.let { SmwLevelNames.translevelOf(it) }, "translevel $t")
        }
    }

    @Test
    fun mapsTheTwoRangesLikeTheGame() {
        assertEquals(0x00, SmwLevelNames.levelOfTranslevel(0x00))
        assertEquals(0x24, SmwLevelNames.levelOfTranslevel(0x24))
        // A partir de 0x25 los niveles viven en el bloque 0x101-0x13B.
        assertEquals(0x101, SmwLevelNames.levelOfTranslevel(0x25))
        assertEquals(0x105, SmwLevelNames.levelOfTranslevel(0x29)) // YOSHI'S ISLAND 1
        assertEquals(0x13B, SmwLevelNames.levelOfTranslevel(0x5F))
        assertNull(SmwLevelNames.levelOfTranslevel(0x60), "fuera del mapa")
    }
}
