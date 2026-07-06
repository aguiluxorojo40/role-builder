package com.rolebuilder.core.snes.compression

import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesGraphicsScanner

/**
 * Registro de códecs de descompresión y autodetección del formato.
 *
 * La gracia: como cada juego puede usar un códec distinto, probamos todos los
 * conocidos en un offset y nos quedamos con el que produce una salida que "parece
 * un dibujo" según el detector de coherencia. Es el mismo juez matemático que usa
 * el explorador de gráficos, reutilizado para validar una descompresión.
 */
object CompressionCodecs {

    /** Todos los códecs soportados. Añadir uno nuevo aquí lo activa en toda la app. */
    val all: List<SnesDecompressor> = listOf(LcLz2)

    /** Resultado de una autodetección: el códec que ganó y su salida descomprimida. */
    class AutoResult(
        val codec: SnesDecompressor,
        val result: DecompressResult,
        val score: Double,
    )

    /**
     * Prueba cada códec en [offset] y devuelve el que produce la salida más
     * "gráfica" para [format], o null si ninguno supera [minScore] (probablemente
     * el bloque no está comprimido con un formato conocido).
     */
    fun autoDecompress(
        rom: ByteArray,
        offset: Int,
        format: SnesGraphicFormat,
        minScore: Double = 0.30,
    ): AutoResult? {
        var best: AutoResult? = null
        for (codec in all) {
            val result = runCatching { codec.decompress(rom, offset) }.getOrNull() ?: continue
            val score = graphicScore(result.data, format)
            if (score >= minScore && (best == null || score > best!!.score)) {
                best = AutoResult(codec, result, score)
            }
        }
        return best
    }

    /** Puntúa cómo de "gráfica" es una salida descomprimida (0..1). */
    fun graphicScore(data: ByteArray, format: SnesGraphicFormat): Double {
        val tiles = data.size / format.bytesPerTile
        if (tiles <= 0) return 0.0
        return SnesGraphicsScanner.scoreWindow(data, 0, format, minOf(tiles, 64))
    }

    /**
     * Recorre la ROM probando a descomprimir en cada byte y devuelve los offsets
     * cuya salida "parece un dibujo" (por coherencia O por firma de sprite), sin
     * conocer las tablas de punteros del juego. Tras un acierto salta el bloque
     * consumido para no repetir. [maxScanBytes] acota el barrido (0 = toda la ROM)
     * para que en el dispositivo tarde poco.
     */
    fun findCompressedBlocks(
        rom: ByteArray,
        format: SnesGraphicFormat = SnesGraphicFormat.SNES_4BPP,
        minScore: Double = 0.42,
        maxResults: Int = 24,
        maxScanBytes: Int = 0,
    ): List<Int> {
        val limit = if (maxScanBytes in 1 until rom.size) maxScanBytes else rom.size
        val minBytes = format.bytesPerTile * 32
        val hits = ArrayList<Pair<Int, Double>>()
        var offset = 0
        while (offset < limit - 3) {
            var advanced = false
            for (codec in all) {
                val res = runCatching { codec.decompress(rom, offset, 0x4000) }.getOrNull() ?: continue
                if (res.data.size >= minBytes) {
                    val tiles = minOf(res.data.size / format.bytesPerTile, 32)
                    val coh = SnesGraphicsScanner.scoreWindow(res.data, 0, format, tiles)
                    val spr = SnesGraphicsScanner.spriteFitness(res.data, 0, format, tiles)
                    val score = maxOf(coh, spr)
                    if (score >= minScore) {
                        hits.add(offset to score)
                        offset += maxOf(1, res.consumedBytes)
                        advanced = true
                        break
                    }
                }
            }
            if (!advanced) offset++
        }
        return hits.sortedByDescending { it.second }.take(maxResults).map { it.first }.sorted()
    }
}
