package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesGraphicsScanner
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SnesGraphicsScannerTest {

    private val format = SnesGraphicFormat.SNES_4BPP
    private val windowTiles = 64
    private val windowBytes = format.bytesPerTile * windowTiles // 32 * 64

    /**
     * ROM sintética con tres regiones de una ventana cada una:
     *  - [0, W)      en blanco (relleno),
     *  - [W, 2W)     "gráficos": tiles con zonas planas (alta coherencia),
     *  - [2W, 3W)    ruido aleatorio (baja coherencia, como datos comprimidos).
     */
    private fun syntheticRom(): Triple<ByteArray, Int, Int> {
        val rom = ByteArray(windowBytes * 3)
        val graphicsStart = windowBytes
        val randomStart = windowBytes * 2

        // Gráficos: cada tile relleno con un índice base (plano) y una cruz de
        // otro color. Coherencia alta y algo de diversidad -> parece un dibujo.
        var p = graphicsStart
        var tileNo = 0
        while (p + 32 <= randomStart) {
            val base = tileNo % 16
            val mark = (base + 1) % 16
            for (y in 0..7) {
                val planes = IntArray(4)
                for (x in 0..7) {
                    val value = if (x == 3 || y == 3) mark else base
                    for (bit in 0..3) if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
                rom[p + 2 * y] = planes[0].toByte()
                rom[p + 2 * y + 1] = planes[1].toByte()
                rom[p + 16 + 2 * y] = planes[2].toByte()
                rom[p + 16 + 2 * y + 1] = planes[3].toByte()
            }
            p += 32
            tileNo++
        }

        // Ruido casi aleatorio (baja coherencia entre píxeles contiguos).
        var seed = 0x12345678
        for (i in randomStart until rom.size) {
            seed = seed * 1103515245 + 12345
            rom[i] = ((seed ushr 16) and 0xFF).toByte()
        }
        return Triple(rom, graphicsStart, randomStart)
    }

    @Test
    fun `encuentra la region de graficos y descarta blanco y ruido`() {
        val (rom, graphicsStart, randomStart) = syntheticRom()

        val candidates = SnesGraphicsScanner.findCandidates(rom, format)
        val offsets = candidates.map { it.offset }

        assertTrue(
            offsets.any { it in graphicsStart until randomStart },
            "Se esperaba un candidato en la región de gráficos; obtenidos: $offsets",
        )
        // El bloque en blanco (offset 0) no debe puntuar.
        assertEquals(0.0, SnesGraphicsScanner.scoreWindow(rom, 0, format, windowTiles))
        // El bloque de gráficos coherentes sí debe puntuar por encima del ruido.
        val gScore = SnesGraphicsScanner.scoreWindow(rom, graphicsStart, format, windowTiles)
        val nScore = SnesGraphicsScanner.scoreWindow(rom, randomStart, format, windowTiles)
        assertTrue(gScore > nScore, "gráficos ($gScore) deberían superar al ruido ($nScore)")
        assertTrue(gScore >= 0.30, "la región de gráficos debería superar el umbral: $gScore")
    }

    @Test
    fun `detectBestFormat acierta el 3bpp de unos graficos 3bpp`() {
        // 64 tiles 3bpp (24B) con zonas planas + una cruz: coherentes en 3bpp,
        // pero desalineados y revueltos si se leen como 2bpp o 4bpp.
        val tiles = 64
        val rom = ByteArray(24 * tiles)
        var p = 0
        var tileNo = 0
        while (p + 24 <= rom.size) {
            val base = tileNo % 8
            val mark = (base + 1) % 8
            for (y in 0..7) {
                val planes = IntArray(3)
                for (x in 0..7) {
                    val value = if (x == 3 || y == 3) mark else base
                    for (bit in 0..2) if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
                rom[p + 2 * y] = planes[0].toByte()
                rom[p + 2 * y + 1] = planes[1].toByte()
                rom[p + 16 + y] = planes[2].toByte()
            }
            p += 24; tileNo++
        }
        val guess = SnesGraphicsScanner.detectBestFormat(rom, 0)
        assertTrue(guess != null, "debería detectar un formato")
        assertEquals(SnesGraphicFormat.SNES_3BPP, guess!!.format, "debería reconocer 3bpp")
        // La aptitud del 3bpp debe superar a la del 4bpp sobre estos mismos bytes.
        val fit3 = SnesGraphicsScanner.formatFitness(rom, 0, SnesGraphicFormat.SNES_3BPP)
        val fit4 = SnesGraphicsScanner.formatFitness(rom, 0, SnesGraphicFormat.SNES_4BPP)
        assertTrue(fit3 > fit4, "3bpp ($fit3) debería encajar mejor que 4bpp ($fit4)")
    }

    @Test
    fun `rom mas pequena que una ventana no da candidatos`() {
        val rom = ByteArray(100)
        assertTrue(SnesGraphicsScanner.findCandidates(rom, format).isEmpty())
    }
}
