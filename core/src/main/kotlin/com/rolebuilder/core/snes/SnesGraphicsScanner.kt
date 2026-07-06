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

    /** Un formato candidato con su aptitud normalizada (0..1); mayor = mejor encaje. */
    data class FormatGuess(val format: SnesGraphicFormat, val fitness: Double)

    /** Coherencia mínima para considerar que una ventana "parece" un dibujo. */
    private const val MIN_SCORE = 0.30

    /**
     * Formatos que se prueban al autodetectar el número de bits por píxel. Se
     * omiten GB/NES 2bpp (se decodifican igual que SNES 2bpp en la práctica) para
     * no duplicar; el ganador se decide por aptitud normalizada, no por bpp.
     */
    val DETECTABLE_FORMATS = listOf(
        SnesGraphicFormat.SNES_2BPP,
        SnesGraphicFormat.SNES_3BPP,
        SnesGraphicFormat.SNES_4BPP,
        SnesGraphicFormat.SNES_8BPP,
    )

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
        val m = measure(rom, offset, format, windowTiles) ?: return 0.0
        if (m.dominant > 0.97) return 0.0 // ventana de un solo color: relleno vacío
        // Penaliza el relleno casi uniforme conservando el premio a las zonas planas.
        return m.coherence * (1.0 - m.dominant)
    }

    /** Coherencia y color dominante de una ventana, o null si se sale de la ROM. */
    private data class Measure(val coherence: Double, val dominant: Double)

    private fun measure(rom: ByteArray, offset: Int, format: SnesGraphicFormat, windowTiles: Int): Measure? {
        val windowBytes = format.bytesPerTile * windowTiles
        if (offset < 0 || offset + windowBytes > rom.size) return null

        var equalAdjacent = 0L
        var totalAdjacent = 0L
        val globalHist = IntArray(256)
        var globalPixels = 0L
        var maxGlobal = 0

        for (t in 0 until windowTiles) {
            val tile = SnesDecoder.decodeTile(rom, offset + t * format.bytesPerTile, format, t)
            val px = tile.pixelIndices
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
        if (totalAdjacent == 0L || globalPixels == 0L) return null
        return Measure(equalAdjacent / totalAdjacent.toDouble(), maxGlobal / globalPixels.toDouble())
    }

    /**
     * Aptitud de un [format] en [offset], NORMALIZADA para poder comparar entre
     * distintas profundidades de bits. La coherencia bruta favorece a los
     * formatos con menos colores (más coincidencias por puro azar), así que se
     * resta la línea base aleatoria (≈ 1/colores) y se reescala: unos datos al
     * azar puntúan ~0 en cualquier bpp, y el formato CORRECTO —que produce zonas
     * planas de verdad— destaca. Devuelve 0 si es relleno de un solo color.
     */
    fun formatFitness(rom: ByteArray, offset: Int, format: SnesGraphicFormat, windowTiles: Int = 32): Double {
        val m = measure(rom, offset, format, windowTiles) ?: return 0.0
        if (m.dominant > 0.97) return 0.0
        val baseline = 1.0 / format.colorCount
        val normalized = ((m.coherence - baseline) / (1.0 - baseline)).coerceIn(0.0, 1.0)
        // Sesgo de Occam: a igualdad, gana el bpp más BAJO. Sin esto, el 8bpp (cuya
        // base aleatoria 1/256 es minúscula) se lleva casi todo por inflación, y
        // unos datos leídos con demasiados planos parecen "coherentes" por azar.
        val simplicity = when (format) {
            SnesGraphicFormat.SNES_2BPP, SnesGraphicFormat.GB_2BPP, SnesGraphicFormat.NES_2BPP -> 1.0
            SnesGraphicFormat.SNES_3BPP -> 0.97
            SnesGraphicFormat.SNES_4BPP -> 0.93
            SnesGraphicFormat.SNES_8BPP -> 0.55
        }
        return normalized * simplicity * (1.0 - m.dominant)
    }

    /**
     * Aptitud de "hoja de sprites/personajes" en [offset]. A diferencia de
     * [scoreWindow] —que penaliza la transparencia y por eso se salta a los
     * personajes—, aquí se PREMIA la firma de una hoja de sprites: un fondo
     * transparente (índice 0) generoso pero no total, con figuras **compactas y
     * sólidas** encima (regiones de color contiguas) y algo de sombreado (varios
     * colores). Así afloran las hojas tipo "Mario" que el barrido normal ignora.
     *
     * Devuelve 0 si el bloque está casi vacío, casi lleno (es un fondo, no
     * sprites) o si las zonas de color no forman cuerpos sólidos (ruido).
     */
    fun spriteFitness(rom: ByteArray, offset: Int, format: SnesGraphicFormat, windowTiles: Int = 32): Double {
        val windowBytes = format.bytesPerTile * windowTiles
        if (offset < 0 || offset + windowBytes > rom.size) return 0.0

        var zero = 0L
        var total = 0L
        // Coherencia de cuerpo separada por dirección: una FIGURA es sólida en
        // horizontal Y vertical; una barra/raya vertical solo lo es en vertical,
        // así que se puede descartar exigiendo cuerpo en ambas direcciones.
        var hEqual = 0L; var hAdj = 0L
        var vEqual = 0L; var vAdj = 0L
        val nonZeroColors = HashSet<Int>()

        for (t in 0 until windowTiles) {
            val px = SnesDecoder.decodeTile(rom, offset + t * format.bytesPerTile, format, t).pixelIndices
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val v = px[y * 8 + x]
                    total++
                    if (v == 0) zero++ else nonZeroColors.add(v)
                    if (x < 7) {
                        val w = px[y * 8 + x + 1]
                        if (v != 0 && w != 0) { hAdj++; if (v == w) hEqual++ }
                    }
                    if (y < 7) {
                        val w = px[(y + 1) * 8 + x]
                        if (v != 0 && w != 0) { vAdj++; if (v == w) vEqual++ }
                    }
                }
            }
        }
        if (total == 0L) return 0.0
        val transparentFrac = zero / total.toDouble()
        // Ni casi vacío ni casi lleno: una hoja de sprites tiene fondo Y figuras.
        if (transparentFrac < 0.15 || transparentFrac > 0.82) return 0.0
        if (nonZeroColors.size < 3) return 0.0        // sin sombreado: no es un personaje
        // Se exigen cuerpos con extensión en AMBAS direcciones (descarta rayas).
        if (hAdj < 24 || vAdj < 24) return 0.0
        val hCoh = hEqual / hAdj.toDouble()
        val vCoh = vEqual / vAdj.toDouble()
        val bodyCoherence = minOf(hCoh, vCoh)         // el eje más débil manda: figura 2D real
        if (bodyCoherence < 0.5) return 0.0           // cuerpos poco sólidos: ruido, no figuras
        // Puntúa por solidez de los cuerpos, con un empujón por tener contenido.
        val contentBonus = ((1.0 - transparentFrac) / 0.85).coerceIn(0.0, 1.0)
        return bodyCoherence * (0.7 + 0.3 * contentBonus)
    }

    /**
     * Barre la ROM buscando **hojas de sprites** (ver [spriteFitness]) y devuelve
     * hasta [maxResults] offsets, ordenados por posición. Complementa a
     * [findCandidates]: uno encuentra fondos/tilesets, el otro personajes.
     */
    fun findSpriteCandidates(
        rom: ByteArray,
        format: SnesGraphicFormat,
        maxResults: Int = 24,
        windowTiles: Int = 32,
        minFitness: Double = 0.55,
    ): List<Candidate> {
        val windowBytes = format.bytesPerTile * windowTiles
        if (windowBytes <= 0 || rom.size < windowBytes) return emptyList()
        val scored = ArrayList<Candidate>()
        var offset = 0
        while (offset + windowBytes <= rom.size) {
            val s = spriteFitness(rom, offset, format, windowTiles)
            if (s >= minFitness) scored.add(Candidate(offset, s))
            offset += windowBytes
        }
        return scored.sortedByDescending { it.score }.take(maxResults).sortedBy { it.offset }
    }

    /**
     * Prueba varios [formats] en [offset] y devuelve el que mejor encaja por
     * [formatFitness], o null si ninguno supera el mínimo. Sirve para que el
     * programa acierte solo entre 2/3/4/8bpp en vez de pedirlo a mano (clave para
     * juegos como Super Mario World, cuyos gráficos son casi todos 3bpp).
     */
    fun detectBestFormat(
        rom: ByteArray,
        offset: Int,
        windowTiles: Int = 32,
        formats: List<SnesGraphicFormat> = DETECTABLE_FORMATS,
    ): FormatGuess? {
        var best: FormatGuess? = null
        for (f in formats) {
            val fit = formatFitness(rom, offset, f, windowTiles)
            if (best == null || fit > best.fitness) best = FormatGuess(f, fit)
        }
        return best?.takeIf { it.fitness >= MIN_SCORE }
    }
}
