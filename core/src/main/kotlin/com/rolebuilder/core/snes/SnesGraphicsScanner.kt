package com.rolebuilder.core.snes

/**
 * Autodetector de zonas con gráficos SIN COMPRIMIR dentro de una ROM.
 *
 * Muchos juegos de SNES comprimen sus gráficos (LZ77/LZSS y variantes propias);
 * un visor de datos crudos como este solo puede mostrar los que están sin
 * comprimir. Este scanner no descomprime nada: recorre la ROM en ventanas y
 * puntúa cuáles "parecen" un dibujo real, para que el usuario salte entre
 * candidatos en vez de teclear offsets en hexadecimal a ciegas.
 *
 * Clave del método: en vez de fiarse solo de la entropía de los bytes (que no
 * distingue bien los datos comprimidos de los gráficos), **decodifica** cada
 * ventana como tiles y mide la **coherencia espacial**: qué fracción de píxeles
 * contiguos comparten color. Una imagen real tiene grandes zonas planas (alta
 * coherencia); el ruido de datos comprimidos o de código apenas la tiene. Así,
 * en un juego que comprime todo su arte, el scanner devuelve pocos o ningún
 * candidato —la respuesta honesta— en lugar de mandar al usuario a ruido.
 */
object SnesGraphicsScanner {

    /** Un offset candidato con su puntuación (0..1); mayor = más "gráfico". */
    data class Candidate(val offset: Int, val score: Double)

    /** Coherencia mínima para considerar que una ventana "parece" un dibujo. */
    private const val MIN_SCORE = 0.30

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
            val s = scoreWindow(rom, offset, format, windowTiles)
            if (s >= MIN_SCORE) scored.add(Candidate(offset, s))
            offset += windowBytes // ventanas sin solapamiento: barrido rápido
        }
        return scored.sortedByDescending { it.score }
            .take(maxResults)
            .sortedBy { it.offset }
    }

    /**
     * Puntúa la ventana de [windowTiles] tiles que empieza en [offset] como
     * "gráfico sin comprimir". Devuelve 0 si parece relleno vacío o ruido
     * (datos comprimidos / código). La puntuación combina:
     *  - coherencia: fracción de píxeles contiguos (horizontal y vertical) con
     *    el mismo índice de color; alta en dibujos, baja en ruido;
     *  - diversidad: penaliza las ventanas de un solo color (relleno).
     */
    fun scoreWindow(rom: ByteArray, offset: Int, format: SnesGraphicFormat, windowTiles: Int): Double {
        val windowBytes = format.bytesPerTile * windowTiles
        if (offset < 0 || offset + windowBytes > rom.size) return 0.0

        var equalAdjacent = 0L
        var totalAdjacent = 0L
        val globalHist = IntArray(256)
        var globalPixels = 0L
        var maxGlobal = 0

        for (t in 0 until windowTiles) {
            val tile = SnesDecoder.decodeTile(rom, offset + t * format.bytesPerTile, format, t)
            val px = tile.pixelIndices
            // Coherencia horizontal y vertical dentro del tile 8x8.
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val v = px[y * 8 + x]
                    if (x < 7) { totalAdjacent++; if (px[y * 8 + x + 1] == v) equalAdjacent++ }
                    if (y < 7) { totalAdjacent++; if (px[(y + 1) * 8 + x] == v) equalAdjacent++ }
                    val c = ++globalHist[v]
                    if (c > maxGlobal) maxGlobal = c
                    globalPixels++
                }
            }
        }

        if (totalAdjacent == 0L || globalPixels == 0L) return 0.0

        val dominant = maxGlobal / globalPixels.toDouble()
        if (dominant > 0.97) return 0.0 // ventana de un solo color: relleno vacío

        val coherence = equalAdjacent / totalAdjacent.toDouble()
        // Penaliza el relleno casi uniforme conservando el premio a las zonas planas.
        return coherence * (1.0 - dominant)
    }
}
