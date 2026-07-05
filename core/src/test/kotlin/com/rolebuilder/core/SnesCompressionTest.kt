package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.compression.CompressionCodecs
import com.rolebuilder.core.snes.compression.DecompressException
import com.rolebuilder.core.snes.compression.LcLz2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnesCompressionTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }
    private fun decode(vararg stream: Int) = LcLz2.decompress(bytes(*stream), 0).data

    // -------------------------------------------------------- round trip

    @Test
    fun `round trip de datos variados`() {
        val cases = listOf(
            ByteArray(0) { 0 }, // vacío
            bytes(1, 2, 3, 4, 5),
            ByteArray(500) { 0x55 },                       // racha larga -> byte fill
            ByteArray(300) { (it % 7).toByte() },          // patrón -> repeticiones
            ByteArray(1000) { ((it * 31 + 7) % 251).toByte() }, // pseudoaleatorio
        )
        for ((idx, original) in cases.withIndex()) {
            val restored = LcLz2.decompress(LcLz2.compress(original), 0).data
            assertTrue(original.contentEquals(restored), "caso $idx no coincide tras round trip")
        }
    }

    @Test
    fun `el compresor de una racha larga ocupa menos que el original`() {
        val original = ByteArray(2000) { 0x00 }
        val compressed = LcLz2.compress(original)
        assertTrue(compressed.size < original.size / 10, "esperaba buena compresión, fue ${compressed.size}")
    }

    // -------------------------------------------------- comandos sueltos

    @Test
    fun `comando copia directa`() {
        // header (cmd0, len3) = 0x02, 3 bytes, fin 0xFF
        assertTrue(decode(0x02, 0x0A, 0x0B, 0x0C, 0xFF).contentEquals(bytes(0x0A, 0x0B, 0x0C)))
    }

    @Test
    fun `comando relleno de byte`() {
        // header (cmd1, len4) = 0x23, byte 0x77
        assertTrue(decode(0x23, 0x77, 0xFF).contentEquals(bytes(0x77, 0x77, 0x77, 0x77)))
    }

    @Test
    fun `comando relleno de word`() {
        // header (cmd2, len4) = 0x43, bytes AA BB -> AA BB AA BB
        assertTrue(decode(0x43, 0xAA, 0xBB, 0xFF).contentEquals(bytes(0xAA, 0xBB, 0xAA, 0xBB)))
    }

    @Test
    fun `comando relleno creciente`() {
        // header (cmd3, len5) = 0x64, byte 0x10 -> 10 11 12 13 14
        assertTrue(decode(0x64, 0x10, 0xFF).contentEquals(bytes(0x10, 0x11, 0x12, 0x13, 0x14)))
    }

    @Test
    fun `comando repeticion con solapamiento`() {
        // copia directa "AB" (0x01,0x41,0x42), luego repeat off=0 len4 (0x83,0x00,0x00)
        // -> ABABAB (el solapamiento genera un patrón repetido)
        val out = decode(0x01, 0x41, 0x42, 0x83, 0x00, 0x00, 0xFF)
        assertTrue(out.contentEquals(bytes(0x41, 0x42, 0x41, 0x42, 0x41, 0x42)))
    }

    @Test
    fun `longitud larga con comando 7`() {
        // long: cmd1 (byte fill), len 100. header = 0xE0 | (1<<2) | ((99>>8)&3) = 0xE4, low=99
        val out = decode(0xE4, 0x63, 0x55, 0xFF)
        assertEquals(100, out.size)
        assertTrue(out.all { it == 0x55.toByte() })
    }

    @Test
    fun `flujo sin marcador de fin lanza excepcion`() {
        assertFailsWith<DecompressException> { LcLz2.decompress(bytes(0x02, 0x0A, 0x0B, 0x0C), 0) }
    }

    // -------------------------------------------------- autodetección

    @Test
    fun `autoDecompress elige LC_LZ2 con salida coherente`() {
        // Gráficos coherentes (tiles con zonas planas), comprimidos con LC_LZ2.
        val graphics = ByteArray(32 * 64)
        var p = 0
        var tileNo = 0
        while (p + 32 <= graphics.size) {
            val base = tileNo % 16
            val mark = (base + 1) % 16
            for (y in 0..7) {
                val planes = IntArray(4)
                for (x in 0..7) {
                    val value = if (x == 3 || y == 3) mark else base
                    for (bit in 0..3) if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
                graphics[p + 2 * y] = planes[0].toByte()
                graphics[p + 2 * y + 1] = planes[1].toByte()
                graphics[p + 16 + 2 * y] = planes[2].toByte()
                graphics[p + 16 + 2 * y + 1] = planes[3].toByte()
            }
            p += 32; tileNo++
        }
        val compressed = LcLz2.compress(graphics)

        val auto = CompressionCodecs.autoDecompress(compressed, 0, SnesGraphicFormat.SNES_4BPP)
        assertNotNull(auto, "debería detectar un códec que produce gráficos")
        assertEquals(LcLz2.name, auto.codec.name)
        assertTrue(graphics.contentEquals(auto.result.data), "la salida debe reconstruir los gráficos")
        assertTrue(auto.score >= 0.30, "score de coherencia insuficiente: ${auto.score}")
    }

    @Test
    fun `autoDecompress devuelve null con datos que no descomprimen`() {
        // Bytes sin marcador de fin válido y sin estructura: ningún códec acierta.
        val garbage = ByteArray(2000) { ((it * 37 + 11) % 254).toByte() } // evita 0xFF frecuente
        assertNull(CompressionCodecs.autoDecompress(garbage, 0, SnesGraphicFormat.SNES_4BPP))
    }
}
