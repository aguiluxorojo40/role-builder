package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwSpriteBehaviorReader
import com.rolebuilder.core.snes.SnesDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests del lector de tweakers de comportamiento de sprites de SMW
 * ([SmwSpriteBehaviorReader]). SINTÉTICOS: se plantan las 6 tablas con sus primeros
 * bytes REALES en las direcciones del banco $07 y se comprueba la lectura y el
 * decodificado del hitbox. Sin ROM con copyright.
 */
class SmwSpriteBehaviorTest {

    // Primeros bytes reales de cada tabla en la ROM US.
    private val t1656 = intArrayOf(0x70, 0x70, 0x70, 0x70, 0x10, 0x10)
    private val t1662 = intArrayOf(0x00, 0x00, 0x00, 0x00, 0x40, 0x40)
    private val t166e = intArrayOf(0x0A, 0x08, 0x06, 0x04, 0x0A, 0x08)
    private val t167a = intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    private val t1686 = intArrayOf(0x00, 0x00, 0x00, 0x00, 0x02, 0x02)
    private val t190f = intArrayOf(0x00, 0x00, 0x00, 0x00, 0xA0, 0xA0)

    private fun romWithTweakers(mangle1656: Boolean = false): ByteArray {
        val rom = ByteArray(0x50000)
        fun put(bank: Int, addr: Int, vals: IntArray) {
            val base = bank * 0x8000 + (addr and 0x7FFF)
            vals.forEachIndexed { i, v -> rom[base + i] = v.toByte() }
        }
        put(0x07, 0xF26C, if (mangle1656) intArrayOf(0x00) else t1656)
        put(0x07, 0xF335, t1662)
        put(0x07, 0xF3FE, t166e)
        put(0x07, 0xF4C7, t167a)
        put(0x07, 0xF590, t1686)
        put(0x07, 0xF659, t190f)
        return rom
    }

    @Test
    fun `lee los seis bytes de comportamiento y el hitbox`() {
        val rom = romWithTweakers()
        val list = SmwSpriteBehaviorReader.read(rom, SnesDecoder.parseHeader(rom))
        assertNotNull(list)
        assertEquals(201, list.size)
        // Sprite id 4: 1656=0x10, 1662=0x40 (hitbox 0x00), 166e=0x0A, 1686=0x02, 190f=0xA0.
        val s4 = list[4]
        assertEquals(0x10, s4.b1656)
        assertEquals(0x40, s4.b1662)
        assertEquals(0x00, s4.spriteClipping) // 0x40 & 0x3F
        assertEquals(0x0A, s4.b166e)
        assertEquals(0xA0, s4.b190f)
        assertEquals(4, s4.id)
    }

    @Test
    fun `una ROM que no es SMW vanilla devuelve null`() {
        val rom = romWithTweakers(mangle1656 = true)
        assertNull(SmwSpriteBehaviorReader.read(rom, SnesDecoder.parseHeader(rom)))
    }

    @Test
    fun `una ROM demasiado corta devuelve null`() {
        assertNull(SmwSpriteBehaviorReader.read(ByteArray(0x100), SnesDecoder.parseHeader(ByteArray(0x10000))))
    }
}
