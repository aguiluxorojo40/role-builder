package com.rolebuilder.core.snes

import kotlin.math.abs
import kotlin.math.ln

/**
 * Autodetector de zonas con gráficos SIN COMPRIMIR dentro de una ROM.
 *
 * Muchos juegos de SNES comprimen sus gráficos (LZ77/LZSS y variantes propias);
 * un visor de datos crudos como este solo puede mostrar los que están sin
 * comprimir. Este scanner no descomprime nada: recorre la ROM en ventanas y
 * puntúa cuáles "parecen" tiles sin comprimir, para que el usuario salte entre
 * candidatos en vez de teclear offsets en hexadecimal a ciegas.
 *
 * Heurística (deliberadamente sencilla y permisiva, pensada para navegar):
 *  - Se descarta una ventana casi uniforme (relleno de 0x00/0xFF): no hay dibujo.
 *  - Se descarta una ventana de entropía casi máxima (~8 bits/byte): suele ser
 *    código o datos comprimidos, que se ven como "nieve".
 *  - Se favorece la entropía media, típica de gráficos indexados sin comprimir.
 */
object SnesGraphicsScanner {

    /** Un offset candidato con su puntuación (0..1); mayor = más "gráfico". */
    data class Candidate(val offset: Int, val score: Double)

    /**
     * Devuelve hasta [maxResults] offsets candidatos, ordenados por posición en
     * la ROM (para poder recorrerlos de principio a fin). Cada ventana abarca
     * [windowTiles] tiles del [format] indicado y se alinea a su tamaño de tile.
     */
    fun findCandidates(
        rom: ByteArray,
        format: SnesGraphicFormat,
        maxResults: Int = 24,
        windowTiles: Int = 64,
    ): List<Candidate> {
        val windowBytes = format.bytesPerTile * windowTiles
        if (windowBytes <= 0 || rom.size < windowBytes) return emptyList()

        val scored = ArrayList<Candidate>()
        var offset = 0
        while (offset + windowBytes <= rom.size) {
            val s = scoreWindow(rom, offset, windowBytes)
            if (s > 0.0) scored.add(Candidate(offset, s))
            offset += windowBytes // ventanas sin solapamiento: barrido rápido
        }
        // Nos quedamos con las mejores y las devolvemos ordenadas por offset.
        return scored.sortedByDescending { it.score }
            .take(maxResults)
            .sortedBy { it.offset }
    }

    /**
     * Puntúa una ventana [offset, offset+length) como "gráfico sin comprimir".
     * Devuelve 0 si parece relleno o datos comprimidos/código.
     */
    fun scoreWindow(rom: ByteArray, offset: Int, length: Int): Double {
        if (offset < 0 || offset + length > rom.size || length <= 0) return 0.0

        val hist = IntArray(256)
        var maxCount = 0
        for (i in offset until offset + length) {
            val b = rom[i].toInt() and 0xFF
            val c = ++hist[b]
            if (c > maxCount) maxCount = c
        }

        val n = length.toDouble()
        val topFraction = maxCount / n
        if (topFraction > 0.92) return 0.0 // casi todo el mismo byte: relleno vacío

        var entropy = 0.0
        for (c in hist) {
            if (c > 0) {
                val p = c / n
                entropy -= p * (ln(p) / LN2)
            }
        }
        if (entropy > 7.3) return 0.0 // casi aleatorio: código o datos comprimidos

        // La entropía de gráficos indexados sin comprimir suele rondar 2..6.5.
        // Puntuamos con un pico suave alrededor de un valor ideal y penalizamos
        // las ventanas dominadas por un único byte.
        val ideal = 4.5
        val entropyScore = (1.0 - abs(entropy - ideal) / 4.5).coerceIn(0.0, 1.0)
        return entropyScore * (1.0 - topFraction)
    }

    private val LN2 = ln(2.0)
}
