package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworldAnim
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Oleaje del mar del overworld ([SmwOverworldAnim]). Gated a la ROM local (no versionada);
 * en CI pasa sin hacer nada.
 */
class SmwOverworldAnimTest {

    @Test
    fun animatesWaterTiles() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0

        // Las teselas de origen salen de la tabla de la ROM, no cableadas: 0x50, 0x51, 0x52.
        val src = SmwOverworldAnim.waterSourceTiles(rom, delta)
        assertEquals(3, src.size)
        assertTrue(src.contentEquals(intArrayOf(0x50, 0x51, 0x52)), "origen: ${src.toList()}")

        val f0 = SmwOverworldAnim.waterTiles(rom, delta, 0)
        assertNotNull(f0, "el agua debe decodificar")
        assertEquals(setOf(0x75, 0x76, 0x77), f0.keys, "anima las teselas del mar")
        assertTrue(f0.values.all { it.size == 64 })
        // Es agua de verdad: varios colores, no una tesela plana.
        assertTrue(f0[0x75]!!.toSet().size >= 2, "la tesela del mar tiene patrón")

        // El ciclo es de 8 pasos: todos distintos entre sí y el 8º vuelve al inicio.
        val frames = (0 until SmwOverworldAnim.FRAMES).map { SmwOverworldAnim.waterTiles(rom, delta, it)!! }
        for (i in 1 until frames.size) {
            assertTrue(
                !frames[i][0x75]!!.contentEquals(frames[0][0x75]!!),
                "el fotograma $i del oleaje debe diferir del 0",
            )
        }
        val wrapped = SmwOverworldAnim.waterTiles(rom, delta, SmwOverworldAnim.FRAMES)!!
        assertTrue(wrapped[0x75]!!.contentEquals(frames[0][0x75]!!), "el ciclo cierra en 8 pasos")
    }

    /** Sin ROM: la rotación de bits y el ciclo de words son puras y comprobables a mano. */
    @Test
    fun cycleLengthIsEightSteps() {
        // Rotar un byte 8 veces a la izquierda lo deja igual — es la base del ciclo de 8.
        var b = 0b1011_0010
        repeat(8) { b = ((b shl 1) or (b ushr 7)) and 0xFF }
        assertEquals(0b1011_0010, b)
        assertEquals(8, SmwOverworldAnim.FRAMES)
        assertEquals(0x75, SmwOverworldAnim.WATER_VRAM_FIRST_TILE)
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
