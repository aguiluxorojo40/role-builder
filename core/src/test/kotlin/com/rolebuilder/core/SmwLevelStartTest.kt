package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SnesDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests del lector del punto de inicio del jugador ([SmwLevelStartReader]).
 * SINTÉTICOS: se plantan las tablas de presets y la cabecera secundaria del nivel
 * 0x106 con sus bytes REALES y se comprueba que la entrada decodifica a la posición
 * verificada contra la ROM: casilla (1, 22). Sin ROM con copyright.
 */
class SmwLevelStartTest {

    // Presets de Y (16) y X (8) reales de la ROM US.
    private val yLo = intArrayOf(0x0, 0x30, 0x60, 0x80, 0xa0, 0xb0, 0xc0, 0xe0, 0x10, 0x30, 0x50, 0x60, 0x70, 0x90, 0x0, 0x0)
    private val yHi = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1)
    private val xLo = intArrayOf(0x10, 0x80, 0x0, 0xe0, 0x10, 0x70, 0x0, 0xe0)
    private val xHi = intArrayOf(0, 0, 0, 0, 1, 1, 1, 1)

    private fun romWithStart(secHdr0: Int = 0x5B, secHdr1: Int = 0x00, mangle: Boolean = false): ByteArray {
        val rom = ByteArray(0x50000)
        fun put(addr: Int, vals: IntArray) {
            val base = 0x05 * 0x8000 + (addr and 0x7FFF)
            vals.forEachIndexed { i, v -> rom[base + i] = v.toByte() }
        }
        put(0xD730, if (mangle) intArrayOf(0x00, 0x00) else yLo)
        put(0xD740, yHi); put(0xD750, xLo); put(0xD758, xHi)
        // Cabecera secundaria del nivel 0x106.
        val base = 0x05 * 0x8000
        rom[base + (0xF000 and 0x7FFF) + 0x106] = secHdr0.toByte()
        rom[base + (0xF200 and 0x7FFF) + 0x106] = secHdr1.toByte()
        rom[base + (0xF400 and 0x7FFF) + 0x106] = 0x9A.toByte()
        rom[base + (0xF600 and 0x7FFF) + 0x106] = 0x00
        return rom
    }

    @Test
    fun `el inicio del nivel 0x106 decodifica a la casilla verificada`() {
        val rom = romWithStart()
        val start = SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106)
        assertNotNull(start)
        // secHdr0=0x5B → Yidx=0xB → Y = 0x160 = 352 px = fila 22.
        // secHdr1=0x00 → Xidx=0   → X = 0x010 =  16 px = col 1.
        assertEquals(16, start.startPixelX)
        assertEquals(352, start.startPixelY)
        assertEquals(1, start.startTileX)
        assertEquals(22, start.startTileY)
        assertEquals(4, start.secHeader.size)
    }

    @Test
    fun `una ROM que no es SMW vanilla devuelve null`() {
        val rom = romWithStart(mangle = true)
        assertNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106))
    }

    @Test
    fun `un nivel fuera de rango devuelve null`() {
        val rom = romWithStart()
        assertNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x200))
    }
}
