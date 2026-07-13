package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwSoundFx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests SIN ROM del decodificador BRR y del extractor de muestras de SMW. Todos
 * los vectores BRR son sintéticos y calculados a mano con el mismo algoritmo
 * documentado en [SmwSoundFx] (el de snesbrr, referencia de los conversores
 * BRR↔WAV), para que no dependan de ningún byte con copyright de la ROM.
 */
class SmwSoundFxTest {

    private fun block(header: Int, vararg data: Int): ByteArray {
        require(data.size == 8) { "un bloque BRR son 1 cabecera + 8 bytes de datos" }
        val b = ByteArray(9)
        b[0] = header.toByte()
        for (i in 0 until 8) b[i + 1] = data[i].toByte()
        return b
    }

    // ------------------------------------------------------------- filtro 0

    @Test
    fun `filtro 0 range 0 aplica desplazamiento a la derecha`() {
        // cabecera: range=0, filtro=0, fin=1 -> 0x01
        // nibble con signo s -> (s<<0)>>1 = s>>1, sin realimentación.
        // 0x72 -> 7,2 -> 3,1 ; 0x8F -> -8,-1 -> -4,-1 ; resto 0.
        val brr = block(0x01, 0x72, 0x8F, 0, 0, 0, 0, 0, 0)
        val pcm = SmwSoundFx.decodeBrr(brr, 0)
        assertEquals(16, pcm.size)
        val exp = intArrayOf(3, 1, -4, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        for (i in exp.indices) assertEquals(exp[i].toShort(), pcm[i], "muestra $i")
    }

    @Test
    fun `filtro 0 con range 4 escala hacia arriba`() {
        // range=4, filtro=0, fin=1 -> 0x41 ; s -> (s<<4)>>1 = s<<3.
        // 0x13 -> 1,3 -> 8,24
        val brr = block(0x41, 0x13, 0, 0, 0, 0, 0, 0, 0)
        val pcm = SmwSoundFx.decodeBrr(brr, 0)
        assertEquals(8.toShort(), pcm[0])
        assertEquals(24.toShort(), pcm[1])
    }

    // ------------------------------------------------------------- filtro 1

    @Test
    fun `filtro 1 realimenta la muestra anterior`() {
        // range=4, filtro=1, fin=1 -> 0x45. Base s = (nib<<4)>>1 = nib<<3.
        // filtro1: s += p1 + ((-p1) shr 4). Primer nibble 4 -> 32, luego 0s:
        // decaimiento 32,30,28,...,9 calculado a mano (shr aritmético).
        val brr = block(0x45, 0x40, 0, 0, 0, 0, 0, 0, 0)
        val pcm = SmwSoundFx.decodeBrr(brr, 0)
        val exp = intArrayOf(32, 30, 28, 26, 24, 22, 20, 18, 16, 15, 14, 13, 12, 11, 10, 9)
        assertEquals(16, pcm.size)
        for (i in exp.indices) assertEquals(exp[i].toShort(), pcm[i], "muestra $i")
    }

    // ------------------------------------------------------- range de saturación

    @Test
    fun `range mayor que 12 satura a menos 2048 o 0`() {
        // range=13, filtro=0, fin=1 -> 0xD1. positivo->0, negativo->-2048.
        // 0x7F -> 7,-1 -> 0,-2048 ; 0x80 -> -8,0 -> -2048,0.
        val brr = block(0xD1, 0x7F, 0x80, 0, 0, 0, 0, 0, 0)
        val pcm = SmwSoundFx.decodeBrr(brr, 0)
        assertEquals(0.toShort(), pcm[0])
        assertEquals((-2048).toShort(), pcm[1])
        assertEquals((-2048).toShort(), pcm[2])
        assertEquals(0.toShort(), pcm[3])
    }

    // ------------------------------------------------------- multi-bloque + fin

    @Test
    fun `decodeBrr se detiene en el flag de fin`() {
        // Dos bloques: el primero SIN fin (0x00), el segundo con fin (0x01).
        val b0 = block(0x00, 0, 0, 0, 0, 0, 0, 0, 0)
        val b1 = block(0x01, 0, 0, 0, 0, 0, 0, 0, 0)
        val brr = b0 + b1
        val pcm = SmwSoundFx.decodeBrr(brr, 0)
        assertEquals(32, pcm.size) // 2 bloques * 16 muestras
    }

    @Test
    fun `decodeBrr ignora bloques tras el flag de fin`() {
        // El primer bloque ya tiene fin: solo debe decodificar 16 muestras
        // aunque haya un segundo bloque detrás.
        val brr = block(0x01, 0, 0, 0, 0, 0, 0, 0, 0) + block(0x01, 0, 0, 0, 0, 0, 0, 0, 0)
        val pcm = SmwSoundFx.decodeBrr(brr, 0)
        assertEquals(16, pcm.size)
    }

    // ----------------------------------------------- extractor / directorio

    /** Construye un bloque de subida N-SPC sintético: sub-bloques [len][dest][datos] + terminador. */
    private fun uploadBlock(vararg parts: Pair<Int, ByteArray>): ByteArray {
        val out = ArrayList<Byte>()
        fun w16(v: Int) { out.add((v and 0xFF).toByte()); out.add(((v ushr 8) and 0xFF).toByte()) }
        for ((dest, data) in parts) {
            w16(data.size); w16(dest); for (x in data) out.add(x)
        }
        w16(0) // terminador
        return out.toByteArray()
    }

    @Test
    fun `extractAt parsea el directorio y decodifica las muestras`() {
        // Directorio en ARAM 0x8000 con 2 entradas {inicio, bucle}:
        //   #0 inicio=0x8100 bucle=0x8100  (1 bloque, sin bucle)
        //   #1 inicio=0x8109 bucle=0x8112  (2 bloques, con bucle en el 2º)
        fun dirEntry(start: Int, loop: Int) = byteArrayOf(
            (start and 0xFF).toByte(), ((start ushr 8) and 0xFF).toByte(),
            (loop and 0xFF).toByte(), ((loop ushr 8) and 0xFF).toByte(),
        )
        val dir = dirEntry(0x8100, 0x8100) + dirEntry(0x8109, 0x8112)

        val sample0 = block(0x01, 0x72, 0x8F, 0, 0, 0, 0, 0, 0)      // fin, sin bucle
        val sample1a = block(0x00, 0, 0, 0, 0, 0, 0, 0, 0)           // bloque 1 de #1
        val sample1b = block(0x03, 0, 0, 0, 0, 0, 0, 0, 0)           // fin + bucle
        val brr = sample0 + sample1a + sample1b

        val rom = uploadBlock(0x8000 to dir, 0x8100 to brr)
        val bank = SmwSoundFx.extractAt(rom, 0)!!

        assertEquals(2, bank.samples.size)

        val s0 = bank.samples[0]
        assertEquals(0x8100, s0.aramStart)
        assertEquals(9, s0.brrByteLength)
        assertFalse(s0.hasLoop)
        assertEquals(-1, s0.loopStartSample)
        assertEquals(16, s0.pcm.size)
        assertEquals(SmwSoundFx.NATIVE_SAMPLE_RATE, s0.sampleRate)
        // Mismos valores que el test de filtro 0.
        assertEquals(3.toShort(), s0.pcm[0])
        assertEquals((-4).toShort(), s0.pcm[2])

        val s1 = bank.samples[1]
        assertEquals(0x8109, s1.aramStart)
        assertEquals(18, s1.brrByteLength) // 2 bloques
        assertTrue(s1.hasLoop)
        assertEquals(16, s1.loopStartSample) // bucle empieza en el 2º bloque
        assertEquals(32, s1.pcm.size)
    }

    @Test
    fun `extractAt devuelve null si no hay directorio en 0x8000`() {
        // Un bloque que sube a otra dirección: sin directorio -> null.
        val rom = uploadBlock(0x9000 to ByteArray(16))
        assertNull(SmwSoundFx.extractAt(rom, 0))
    }
}
