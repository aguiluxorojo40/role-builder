package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwPhysics
import com.rolebuilder.core.snes.SmwPhysicsReader
import com.rolebuilder.core.snes.SnesDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests del lector de físicas del jugador de SMW ([SmwPhysicsReader]). Son
 * SINTÉTICOS: se planta una ROM con las tablas REALES (los mismos bytes que la ROM
 * US) en sus direcciones del banco $00 y se comprueba que el lector las devuelve y
 * las convierte a unidades correctas. Sin ROM con copyright.
 */
class SmwPhysicsTest {

    // Bytes reales de la ROM US (verificados contra la ROM y la recompilación snesrev/smw).
    private val jumpTable = intArrayOf(
        0xB0, 0xB6, 0xAE, 0xB4, 0xAB, 0xB2, 0xA9, 0xB0,
        0xA6, 0xAE, 0xA4, 0xAB, 0xA1, 0xA9, 0x9F, 0xA6,
    )
    private val gravityTable = intArrayOf(0x06, 0x03, 0x04, 0x10, 0xF4, 0x01, 0x03, 0x04, 0x05, 0x06)
    private val maxFallTable = intArrayOf(0x40, 0x40, 0x20, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40)

    /** Construye una ROM LoROM sin cabecera con las tablas en sus PC ($00:addr-0x8000). */
    private fun romWithPhysics(): ByteArray {
        val rom = ByteArray(0x10000)
        fun putBytes(snesAddr: Int, vals: IntArray) {
            val base = snesAddr - 0x8000
            vals.forEachIndexed { i, v -> rom[base + i] = v.toByte() }
        }
        putBytes(0xD2BD, jumpTable)
        putBytes(0xD7A5, gravityTable)
        putBytes(0xD7AF, maxFallTable)
        // Rellena de forma no nula el resto de tablas para que las lecturas no fallen por rango.
        // (Aceleración/velocidad máxima: valores arbitrarios pero presentes.)
        for (a in intArrayOf(0xD2CD, 0xD309, 0xD345, 0xD43D, 0xD535, 0xD5C9)) rom[a - 0x8000] = 0x11
        // Cabecera interna mínima para SnesDecoder (título en 0x7FC0).
        return rom
    }

    @Test
    fun `lee las tablas reales de salto, gravedad y caida`() {
        val rom = romWithPhysics()
        val phys = SmwPhysicsReader.read(rom, SnesDecoder.parseHeader(rom))
        assertNotNull(phys)
        // El salto parado es -80 (0xB0 con signo) = -5.0 px/fotograma hacia arriba.
        assertEquals(-80, phys.baseJumpYSpeed)
        assertEquals(-5.0, phys.toPixelsPerFrame(phys.baseJumpYSpeed))
        assertEquals(-300.0, phys.toPixelsPerSecond(phys.baseJumpYSpeed)) // -5 px/f * 60
        // Gravedad por defecto = 6 (0.375 px/fotograma²) y caída terminal = 0x40 = 4.0 px/f.
        assertEquals(6, phys.defaultGravity)
        assertEquals(0x40, phys.defaultMaxFall)
        assertEquals(4.0, phys.toPixelsPerFrame(phys.defaultMaxFall))
        // La tabla de salto completa se lee con signo.
        assertEquals(16, phys.jumpYSpeed.size)
        assertEquals(-80, phys.jumpYSpeed[0])
        assertEquals(-74, phys.jumpYSpeed[1]) // 0xB6 con signo
    }

    @Test
    fun `las constantes de muerte y pisar coinciden con el juego`() {
        assertEquals(-112, SmwPhysics.DEATH_POP_YSPEED)         // $00:F606
        assertEquals(-88, SmwPhysics.STOMP_BOUNCE_JUMP_HELD)    // $01:AA33 con salto
        assertEquals(-48, SmwPhysics.STOMP_BOUNCE)              // $01:AA33 sin salto
        assertTrue(SmwPhysics.STOMP_BOUNCE_JUMP_HELD < SmwPhysics.STOMP_BOUNCE,
            "mantener el salto rebota más alto")
    }

    @Test
    fun `una ROM que no es SMW vanilla devuelve null`() {
        // Salto parado != 0xB0 → el lector no se fía y devuelve null.
        val rom = romWithPhysics()
        rom[0xD2BD - 0x8000] = 0x00
        assertNull(SmwPhysicsReader.read(rom, SnesDecoder.parseHeader(rom)))
    }

    @Test
    fun `una ROM demasiado corta devuelve null`() {
        val tiny = ByteArray(0x100)
        assertNull(SmwPhysicsReader.read(tiny, SnesDecoder.parseHeader(ByteArray(0x10000))))
    }
}
