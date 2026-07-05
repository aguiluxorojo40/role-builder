package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesGraphicsScanner
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SnesGraphicsScannerTest {

    /** ROM sintética: [0, W) en blanco, [W, 2W) gráficos estructurados, [2W, 3W) aleatorio. */
    private fun syntheticRom(windowBytes: Int): Triple<ByteArray, Int, Int> {
        val rom = ByteArray(windowBytes * 3)
        val graphicsStart = windowBytes
        val randomStart = windowBytes * 2

        // Región de gráficos: patrón entrelazado 4bpp con degradado diagonal
        // (pocos valores, estructura repetida -> entropía media).
        var p = graphicsStart
        while (p + 32 <= randomStart) {
            for (y in 0..7) {
                val planes = IntArray(4)
                for (x in 0..7) {
                    val value = (x + y + (p / 32)) % 16
                    for (bit in 0..3) if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
                rom[p + 2 * y] = planes[0].toByte()
                rom[p + 2 * y + 1] = planes[1].toByte()
                rom[p + 16 + 2 * y] = planes[2].toByte()
                rom[p + 16 + 2 * y + 1] = planes[3].toByte()
            }
            p += 32
        }

        // Región aleatoria (entropía casi máxima) con un PRNG determinista.
        var seed = 0x12345678
        for (i in randomStart until rom.size) {
            seed = seed * 1103515245 + 12345
            rom[i] = ((seed ushr 16) and 0xFF).toByte()
        }
        return Triple(rom, graphicsStart, randomStart)
    }

    @Test
    fun `encuentra la region de graficos y descarta blanco y aleatorio`() {
        val windowBytes = 32 * 64 // 64 tiles 4bpp
        val (rom, graphicsStart, randomStart) = syntheticRom(windowBytes)

        val candidates = SnesGraphicsScanner.findCandidates(rom, SnesGraphicFormat.SNES_4BPP)
        val offsets = candidates.map { it.offset }

        // La región de gráficos debe aparecer como candidata.
        assertTrue(
            offsets.any { it in graphicsStart until randomStart },
            "Se esperaba un candidato en la región de gráficos; obtenidos: $offsets",
        )
        // El bloque en blanco (offset 0) no debe puntuar.
        assertEquals(0.0, SnesGraphicsScanner.scoreWindow(rom, 0, windowBytes))
        // El bloque aleatorio no debe puntuar (entropía ~8).
        assertEquals(0.0, SnesGraphicsScanner.scoreWindow(rom, randomStart, windowBytes))
        // El bloque de gráficos sí puntúa.
        assertTrue(SnesGraphicsScanner.scoreWindow(rom, graphicsStart, windowBytes) > 0.0)
    }

    @Test
    fun `rom mas pequena que una ventana no da candidatos`() {
        val rom = ByteArray(100)
        assertTrue(SnesGraphicsScanner.findCandidates(rom, SnesGraphicFormat.SNES_4BPP).isEmpty())
    }
}
