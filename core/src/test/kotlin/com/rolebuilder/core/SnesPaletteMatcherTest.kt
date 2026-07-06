package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesPalette
import com.rolebuilder.core.snes.SnesPaletteMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnesPaletteMatcherTest {

    /**
     * Píxeles de un tile "con sombreado": anillos concéntricos con índice 4 en
     * el centro y 1 en el borde. Los píxeles vecinos siempre son índices
     * consecutivos de la rampa (1-2, 2-3, 3-4), como en el pixel art real.
     */
    private fun ringTilePixels(): IntArray {
        val px = IntArray(64)
        for (y in 0..7) for (x in 0..7) {
            val d = maxOf(kotlin.math.abs(2 * x - 7), kotlin.math.abs(2 * y - 7)) // 1,3,5,7
            px[y * 8 + x] = 4 - (d - 1) / 2 // centro=4 ... borde=1
        }
        return px
    }

    private fun encode4bpp(pixels: IntArray, tiles: Int): ByteArray {
        val rom = ByteArray(32 * tiles)
        for (t in 0 until tiles) {
            val p = t * 32
            for (y in 0..7) {
                val planes = IntArray(4)
                for (x in 0..7) {
                    val v = pixels[y * 8 + x]
                    for (bit in 0..3) if ((v shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
                rom[p + 2 * y] = planes[0].toByte()
                rom[p + 2 * y + 1] = planes[1].toByte()
                rom[p + 16 + 2 * y] = planes[2].toByte()
                rom[p + 16 + 2 * y + 1] = planes[3].toByte()
            }
        }
        return rom
    }

    private fun encode3bpp(pixels: IntArray, tiles: Int): ByteArray {
        val rom = ByteArray(24 * tiles)
        for (t in 0 until tiles) {
            val p = t * 24
            for (y in 0..7) {
                val planes = IntArray(3)
                for (x in 0..7) {
                    val v = pixels[y * 8 + x]
                    for (bit in 0..2) if ((v shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
                rom[p + 2 * y] = planes[0].toByte()
                rom[p + 2 * y + 1] = planes[1].toByte()
                rom[p + 16 + y] = planes[2].toByte()
            }
        }
        return rom
    }

    /** Rampa de verdes coherente en los índices 1..4 (la paleta "correcta"). */
    private fun greenRamp16(): IntArray {
        val colors = IntArray(16) { 0xFF000000.toInt() }
        colors[1] = 0xFF0A3D0A.toInt()
        colors[2] = 0xFF1E6B1E.toInt()
        colors[3] = 0xFF37A337.toInt()
        colors[4] = 0xFF66E066.toInt()
        return colors
    }

    /** Tonos vivos SIN relación entre sí en 1..4 (paleta "ajena": ruido de colores). */
    private fun clashing16(): IntArray {
        val colors = IntArray(16) { 0xFF000000.toInt() }
        colors[1] = 0xFFE53935.toInt() // rojo
        colors[2] = 0xFF1E88E5.toInt() // azul
        colors[3] = 0xFFFDD835.toInt() // amarillo
        colors[4] = 0xFF8E24AA.toInt() // magenta
        return colors
    }

    /** Grises casi idénticos: fundiría los índices y aplanaría el dibujo. */
    private fun flat16(): IntArray = IntArray(16) { i ->
        val level = 0x40 + i * 3
        (0xFF shl 24) or (level shl 16) or (level shl 8) or level
    }

    private fun pal(id: String, colors: IntArray) = SnesPalette(id, id, colors)

    @Test
    fun `la rampa coherente gana a la paleta ajena y a la plana`() {
        val rom = encode4bpp(ringTilePixels(), tiles = 16)
        val candidates = listOf(
            pal("ajena", clashing16()),
            pal("plana", flat16()),
            pal("rampa", greenRamp16()),
        )
        val ranked = SnesPaletteMatcher.rankPalettes(rom, 0, SnesGraphicFormat.SNES_4BPP, 16, candidates)
        assertEquals(3, ranked.size)
        assertEquals("rampa", ranked[0].source.id, "la rampa de sombreado debería ganar: $ranked")
        assertTrue(
            ranked[0].score > ranked[1].score + 0.1,
            "la correcta debería destacar con margen: ${ranked.map { it.source.id to it.score }}",
        )
        // bestColors devuelve los colores de esa misma ganadora.
        val best = SnesPaletteMatcher.bestColors(rom, 0, SnesGraphicFormat.SNES_4BPP, 16, candidates)
        assertTrue(best != null && best.contentEquals(ranked[0].colors))
    }

    @Test
    fun `con graficos de 8 colores encuentra la SUB-paleta correcta`() {
        // Paleta de 16 colores: mitad baja ajena, mitad alta con la rampa buena.
        val colors = IntArray(16)
        clashing16().copyInto(colors, 0, 0, 8)
        greenRamp16().copyInto(colors, 8, 0, 8)
        val rom = encode3bpp(ringTilePixels(), tiles = 16)

        val ranked = SnesPaletteMatcher.rankPalettes(
            rom, 0, SnesGraphicFormat.SNES_3BPP, 16, listOf(pal("cgram", colors)),
        )
        assertEquals(1, ranked.size)
        assertEquals(8, ranked[0].window, "debería elegir la ventana alta (colores 8..15)")
        assertEquals(0xFF0A3D0A.toInt(), ranked[0].colors[1])
    }

    @Test
    fun `muestrea toda la hoja, no solo los primeros tiles`() {
        // Hoja grande (256 tiles) cuyo dibujo real vive en la SEGUNDA mitad; la
        // primera son tiles vacíos (padding transparente). Leer solo los primeros
        // 128 dejaría la radiografía vacía y no habría con qué elegir paleta. Al
        // repartir la muestra por toda la hoja, la rampa correcta debe destacar.
        val ring = encode4bpp(ringTilePixels(), tiles = 128)
        val rom = ByteArray(32 * 128) + ring // 128 tiles vacíos + 128 con anillos

        val candidates = listOf(
            pal("ajena", clashing16()),
            pal("rampa", greenRamp16()),
        )
        val ranked = SnesPaletteMatcher.rankPalettes(rom, 0, SnesGraphicFormat.SNES_4BPP, 256, candidates)
        assertEquals("rampa", ranked[0].source.id, "debería mirar la segunda mitad: $ranked")

        // Cordura: si el dibujo estuviera SOLO en la cabecera, el muestreo antiguo
        // (contiguo) y el nuevo coinciden, así que el orden se mantiene.
        val soloCabecera = ring + ByteArray(32 * 128)
        val head = SnesPaletteMatcher.rankPalettes(soloCabecera, 0, SnesGraphicFormat.SNES_4BPP, 256, candidates)
        assertEquals("rampa", head[0].source.id)
    }

    @Test
    fun `sin candidatas o sin indices usados no revienta`() {
        val rom = encode4bpp(ringTilePixels(), tiles = 4)
        assertTrue(SnesPaletteMatcher.rankPalettes(rom, 0, SnesGraphicFormat.SNES_4BPP, 4, emptyList()).isEmpty())
        // Tiles vacíos (todo índice 0 = transparente): puntuación nula, sin excepción.
        val empty = ByteArray(32 * 4)
        val stats = SnesPaletteMatcher.indexStats(empty, 0, SnesGraphicFormat.SNES_4BPP, 4)
        assertEquals(0.0, SnesPaletteMatcher.scorePalette(greenRamp16(), stats))
    }

    @Test
    fun `indexStats ignora el transparente y cuenta pares vecinos`() {
        val rom = encode4bpp(ringTilePixels(), tiles = 1)
        val stats = SnesPaletteMatcher.indexStats(rom, 0, SnesGraphicFormat.SNES_4BPP, 1)
        assertEquals(0, stats.usage[0]) // el tile de anillos no usa el índice 0
        assertTrue(stats.usedIndices.containsAll(listOf(1, 2, 3, 4)))
        // Solo conviven índices consecutivos de la rampa: 1-2, 2-3 y 3-4.
        val pairs = stats.edges.keys.map { (it shr 16) to (it and 0xFFFF) }.toSet()
        assertEquals(setOf(1 to 2, 2 to 3, 3 to 4), pairs)
    }
}
