package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Los offsets de las tablas Map16 están marcados [PROBABLE]: son deducciones, no datos
 * verificados contra el desensamblado. Hasta ahora se usaban a ciegas y, si apuntaban mal, el
 * fondo salía incorrecto SIN aviso — una imagen plausible pero falsa. Esta comprobación no
 * demuestra que el offset sea el bueno, pero caza el fallo real: que apunte a zona vacía,
 * relleno o algo que no es una tabla.
 */
class Map16OffsetCheckTest {

    @Test
    fun `una zona vacia NO cuela como tabla Map16`() {
        val rom = ByteArray(0x80000) // todo ceros
        val c = SnesGameRecipes.checkMap16Table(rom, 0x68000, "FG")
        assertFalse(c.ok, "ceros no pueden pasar por tabla")
        assertTrue(c.reason.contains("mismo byte"), "debe decir POR QUÉ: ${c.reason}")
    }

    @Test
    fun `una zona de relleno 0xFF tampoco cuela`() {
        val rom = ByteArray(0x80000) { 0xFF.toByte() }
        assertFalse(SnesGameRecipes.checkMap16Table(rom, 0x68000, "FG").ok)
    }

    @Test
    fun `un offset fuera del ROM se detecta en vez de reventar`() {
        val rom = ByteArray(0x1000)
        val c = SnesGameRecipes.checkMap16Table(rom, 0x68000, "FG")
        assertFalse(c.ok)
        assertTrue(c.reason.contains("ROM"), "debe explicar que se sale: ${c.reason}")
    }

    @Test
    fun `datos con variedad de teselas SI parecen una tabla`() {
        val rom = ByteArray(0x80000)
        // 128 bloques × 4 palabras: nº de tesela variado + byte de propiedades.
        for (b in 0 until 128) for (w in 0 until 4) {
            val o = 0x68000 + b * 8 + w * 2
            rom[o] = ((b * 4 + w) and 0xFF).toByte()   // tesela
            rom[o + 1] = ((b % 8) shl 2).toByte()      // propiedades (sub-paleta)
        }
        val c = SnesGameRecipes.checkMap16Table(rom, 0x68000, "FG")
        assertTrue(c.ok, "una tabla con variedad debe pasar: ${c.reason}")
    }

    @Test
    fun `checkProbableOffsets informa de las dos tablas`() {
        val checks = SnesGameRecipes.checkProbableOffsets(ByteArray(0x80000))
        assertTrue(checks.size == 2)
        // Sobre un ROM de ceros, ninguna debe darse por buena.
        assertTrue(checks.none { it.ok }, "con ceros no puede validar nada")
        assertTrue(checks.all { it.reason.isNotBlank() }, "cada una debe explicar su veredicto")
    }
}
