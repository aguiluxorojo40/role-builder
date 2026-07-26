package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwOverworld
import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CADENA COMPLETA de los primeros niveles del juego: casilla del mapa → nº de nivel → nombre
 * real → leveldata jugable → nivel que el parser reconstruye → evento que dispara al
 * superarlo. Es el recorrido exacto que hace la pantalla navegable cuando tocas una casilla,
 * así que si algo se desfasa aquí el jugador acaba en otro nivel.
 *
 * La zona de Yoshi's Island (nº 37-42) es el mejor banco de pruebas porque en la ROM sale
 * ordenada y encadenada: sus leveldata son **0x101 a 0x106 consecutivos** y sus eventos van
 * **en el mismo orden en que se juega el juego**. Que las dos cosas encajen a la vez es una
 * confirmación independiente de que el mapeo es correcto — no cuadraría por casualidad.
 *
 * Gated a la ROM local (no versionada); en CI pasa sin hacer nada.
 */
class SmwFirstLevelsChainTest {

    private data class Chain(val number: Int, val name: String, val leveldata: Int, val event: Int)

    /** (nº de nivel, nombre, leveldata, evento que dispara) verificados contra la ROM US. */
    private val expected = listOf(
        Chain(37, "#1 IGGY'S CASTLE", 0x101, 0x06),
        Chain(38, "YOSHI'S ISLAND 4", 0x102, 0x05),
        Chain(39, "YOSHI'S ISLAND 3", 0x103, 0x04),
        Chain(40, "YOSHI'S HOUSE", 0x104, 0x00),
        Chain(41, "YOSHI'S ISLAND 1", 0x105, 0x01),
        Chain(42, "YOSHI'S ISLAND 2", 0x106, 0x03),
    )

    @Test
    fun firstLevelsAreWiredEndToEnd() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val levels = SmwOverworldLevels.levels(rom, delta)
        val events = SmwOverworld.translevelEvents(rom, delta)

        for (e in expected) {
            val lv = levels.firstOrNull { it.levelNumber == e.number }
            assertNotNull(lv, "falta la casilla del nivel ${e.number}")
            assertEquals(e.name, lv.name, "nombre del nivel ${e.number}")

            // Casilla → leveldata jugable.
            assertEquals(e.leveldata, SmwLevelNames.levelOfTranslevel(e.number), "leveldata de ${e.name}")

            // El leveldata tiene que reconstruirse de verdad: si apuntara a otro sitio saldría
            // vacío o con un montón de objetos sin portar.
            val tm = SmwLayer1.parse(rom, delta, e.leveldata)
            assertNotNull(tm, "${e.name} debe parsear")
            assertTrue(tm.totalObjects > 0, "${e.name} tiene objetos: ${tm.totalObjects}")
            assertTrue(
                tm.unknownObjects * 4 <= tm.totalObjects,
                "${e.name}: demasiados objetos sin portar (${tm.unknownObjects}/${tm.totalObjects})",
            )

            // Evento que dispara al superarlo (motor de la progresión del mapa).
            assertEquals(e.event, events[e.number], "evento de ${e.name}")
        }
    }

    /**
     * Los leveldata de Yoshi's Island son CONSECUTIVOS (0x101…0x106) y sus eventos van en el
     * orden real de juego: casa → 0, YI1 → 1, YI2 → 3, YI3 → 4, YI4 → 5, castillo → 6. Que las
     * dos secuencias encajen es la confirmación de que el mapeo casilla↔nivel no está corrido.
     */
    @Test
    fun yoshiIslandLevelDataAndEventsAreSequential() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val events = SmwOverworld.translevelEvents(rom, delta)

        assertEquals(
            (0x101..0x106).toList(),
            expected.sortedBy { it.number }.map { it.leveldata },
            "leveldata consecutivos",
        )
        // En orden de PARTIDA (no de numeración) los eventos suben: 0, 1, 3, 4, 5, 6.
        val playOrder = listOf(
            "YOSHI'S HOUSE", "YOSHI'S ISLAND 1", "YOSHI'S ISLAND 2",
            "YOSHI'S ISLAND 3", "YOSHI'S ISLAND 4", "#1 IGGY'S CASTLE",
        )
        val evs = playOrder.map { n -> events[expected.first { it.name == n }.number] }
        assertEquals(listOf(0x00, 0x01, 0x03, 0x04, 0x05, 0x06), evs, "eventos en orden de partida")
    }

    /**
     * Los eventos de esos niveles tienen que ABRIR CAMINO de verdad en Yoshi's Island. Se
     * compara el submapa con 0 eventos (partida nueva) contra el mismo con los 7 primeros
     * aplicados: si el sistema de eventos estuviera mal, la imagen no cambiaría.
     */
    @Test
    fun firstEventsOpenPathsOnYoshiIsland() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val fresh = com.rolebuilder.core.snes.SnesGameRecipes
            .renderOverworldSubmap(rom, header, submap = 1, eventCount = 0)
        val after = com.rolebuilder.core.snes.SnesGameRecipes
            .renderOverworldSubmap(rom, header, submap = 1, eventCount = 7)
        assertNotNull(fresh); assertNotNull(after)
        val changed = fresh.pixels.indices.count { fresh.pixels[it] != after.pixels[it] }
        assertTrue(changed > 400, "los 7 primeros eventos deben abrir camino: $changed px")

        val dumpDir = File(
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/scratchpad"
        )
        if (dumpDir.isDirectory) {
            fun dump(name: String, im: com.rolebuilder.core.snes.ArgbImage) {
                val bi = java.awt.image.BufferedImage(im.width, im.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                bi.setRGB(0, 0, im.width, im.height, im.pixels, 0, im.width)
                javax.imageio.ImageIO.write(bi, "png", File(dumpDir, name))
            }
            dump("yoshi_ev0.png", fresh)
            dump("yoshi_ev7.png", after)
            println("YOSHIEV cambian $changed px entre 0 y 7 eventos")
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
