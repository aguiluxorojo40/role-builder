package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwGameSave
import com.rolebuilder.core.snes.SmwOverworld
import com.rolebuilder.core.snes.SmwSaveIo
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PARTIDA guardada del juego ([SmwGameSave]). Es lo que separa "jugar" de "probar en el
 * editor": el modo juego persiste, el modo prueba no. Casi todo se prueba sin ROM porque el
 * modelo es puro; el último test sí la usa para comprobar que los eventos que guardamos son
 * los eventos REALES del mapa.
 */
class SmwGameSaveTest {

    @Test
    fun aNewSaveStartsEmptyAndOnYoshisIsland() {
        val s = SmwGameSave()
        assertTrue(s.isNew, "una partida nueva no tiene nada superado")
        assertEquals(SmwGameSave.START_SUBMAP, s.submap, "SMW empieza en Yoshi's Island")
        assertEquals(SmwGameSave.START_LIVES, s.lives)
        assertNull(s.position, "la casilla de salida la resuelve el mapa, no se inventa aquí")
        // Sin eventos disparados, el mapa no debe abrir ningún camino.
        assertFalse(s.activeEvents()(0), "sin progreso ningún evento está activo")
    }

    @Test
    fun beatingALevelFiresItsEventAndMarksTheLevel() {
        val after = SmwGameSave().withLevelBeaten(translevel = 0x29, event = 4)
        assertTrue(0x29 in after.completedTranslevels, "el nivel queda apuntado")
        assertTrue(after.activeEvents()(4), "su evento abre camino")
        assertFalse(after.isNew)
        assertFalse(after.activeEvents()(5), "y solo el suyo")
    }

    @Test
    fun aLevelWithoutEventStillCountsAsBeaten() {
        // Hay casillas sin evento de mapa: superarlas no abre camino, pero pasarlas sí cuenta.
        val after = SmwGameSave().withLevelBeaten(0x04, SmwOverworld.EVENT_NONE)
        assertTrue(0x04 in after.completedTranslevels, "cuenta como superado")
        assertTrue(after.firedEvents.isEmpty(), "pero no dispara ningún evento")
    }

    @Test
    fun savesSurviveARoundTripToDisk() {
        val dir = Files.createTempDirectory("smw-save-test").toFile()
        try {
            assertNull(SmwSaveIo.load(dir, 1), "ranura vacía antes de guardar")

            val original = SmwGameSave(slot = 2, lives = 3, coins = 47)
                .withLevelBeaten(0x29, 4)
                .withLevelBeaten(0x2A, 5)
                .movedTo(submap = SmwGameSave.MAIN_MAP, position = 0x123)
            val written = SmwSaveIo.save(dir, original)
            assertTrue(written.savedAtEpochMs > 0, "al guardar se sella la fecha")

            val read = SmwSaveIo.load(dir, 2)
            assertNotNull(read)
            assertEquals(written, read, "lo leído es exactamente lo escrito")
            assertEquals(setOf(4, 5), read.firedEvents)
            assertEquals(setOf(0x29, 0x2A), read.completedTranslevels)
            assertEquals(SmwGameSave.MAIN_MAP, read.submap)
            assertEquals(0x123, read.position)
            assertEquals(47, read.coins)

            // Las ranuras se leen todas de golpe, con hueco donde no hay nada.
            val slots = SmwSaveIo.slots(dir)
            assertEquals(SmwGameSave.MAX_SLOTS, slots.size)
            assertNull(slots[0], "ranura 1 vacía")
            assertNotNull(slots[1], "ranura 2 ocupada")
            assertNull(slots[2], "ranura 3 vacía")

            assertTrue(SmwSaveIo.delete(dir, 2))
            assertNull(SmwSaveIo.load(dir, 2), "borrada de verdad")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aCorruptFileReadsAsEmptySlotInsteadOfCrashing() {
        val dir = Files.createTempDirectory("smw-save-bad").toFile()
        try {
            SmwSaveIo.file(dir, 1).apply { parentFile.mkdirs() }.writeText("{ esto no es json")
            assertNull(SmwSaveIo.load(dir, 1), "una ranura corrupta es una ranura vacía")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun theEventsWeStoreAreTheRealMapEvents() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val events = SmwOverworld.translevelEvents(rom, delta)

        // Se juega el primer nivel de verdad (YOSHI'S ISLAND 1 = translevel 0x29) y se guarda
        // su evento tal cual lo da la ROM. Es el eslabón nivel → camino abierto.
        val ev = events[0x29]
        assertTrue(ev != SmwOverworld.EVENT_NONE, "YOSHI'S ISLAND 1 abre camino")
        val save = SmwGameSave().withLevelBeaten(0x29, ev)
        assertTrue(save.activeEvents()(ev), "y la partida lo recuerda")

        // El mapa dibujado con esa partida debe diferir del mapa de partida nueva: si no,
        // el guardado no estaría sirviendo para nada.
        val nuevo = SmwOverworld.overworldTilemapWithEvents(rom, delta) { false }
        val tras = SmwOverworld.overworldTilemapWithEvents(rom, delta, save.activeEvents())
        assertFalse(nuevo.contentEquals(tras), "superar el nivel cambia el mapa")
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
