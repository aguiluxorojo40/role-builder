package com.rolebuilder.core

import com.rolebuilder.core.snes.compression.Rle1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests del códec LC_RLE1 (fondos de Layer 2 de SMW): decodificación explícita de
 * un flujo conocido, fin en 0xFF, y round-trip compresor↔decodificador, todo
 * sintético (sin ROM con copyright).
 */
class SnesRle1Test {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun `decodifica copia directa y RLE y para en 0xFF`() {
        // 0x02 = copia directa L+1=3 (a,b,c); 0x81 = RLE L+1=2 de d; 0xFF = fin.
        val stream = bytes(0x02, 0xAA, 0xBB, 0xCC, 0x81, 0xDD, 0xFF)
        val r = Rle1.decompress(stream, 0)
        assertEquals(listOf(0xAA, 0xBB, 0xCC, 0xDD, 0xDD), r.data.map { it.toInt() and 0xFF })
        assertEquals(stream.size, r.consumedBytes) // consumió todo el flujo, incluido el 0xFF
    }

    @Test
    fun `el segundo 0xFF del terminador FF FF es irrelevante`() {
        val stream = bytes(0x00, 0x42, 0xFF, 0xFF) // copia 1 byte, luego FF FF
        val r = Rle1.decompress(stream, 0)
        assertEquals(listOf(0x42), r.data.map { it.toInt() and 0xFF })
        assertEquals(3, r.consumedBytes) // para en el PRIMER 0xFF
    }

    @Test
    fun `respeta el offset de inicio`() {
        val stream = bytes(0x99, 0x99, 0x00, 0x7A, 0xFF)
        val r = Rle1.decompress(stream, 2) // arranca en la copia directa
        assertEquals(listOf(0x7A), r.data.map { it.toInt() and 0xFF })
    }

    @Test
    fun `round-trip de datos con rachas y literales`() {
        val original = bytes(
            1, 2, 3, 4,                       // literales
            7, 7, 7, 7, 7, 7,                 // racha
            9, 8, 9, 8,                       // literales alternos
            0, 0,                             // racha corta
            5,                                // literal suelto
        )
        val packed = Rle1.compress(original)
        val r = Rle1.decompress(packed, 0)
        assertTrue(r.data.contentEquals(original), "round-trip debe reconstruir el original")
    }

    @Test
    fun `round-trip de una racha larga (mas de 127) se parte en varios comandos`() {
        val original = ByteArray(300) { 0x5A } // 300 iguales → varios comandos RLE de <=127
        val packed = Rle1.compress(original)
        val r = Rle1.decompress(packed, 0)
        assertTrue(r.data.contentEquals(original))
        // Ningún byte de cabecera del flujo comprimido puede ser 0xFF salvo el fin.
        assertEquals(0xFF, packed.last().toInt() and 0xFF)
    }

    @Test
    fun `un flujo sin terminador lanza excepcion`() {
        val bad = bytes(0x00, 0x11) // copia 1 byte y se acaba sin 0xFF
        try {
            Rle1.decompress(bad, 0)
            assertTrue(false, "debería lanzar por falta de terminador")
        } catch (e: Exception) {
            assertTrue(true)
        }
    }
}
