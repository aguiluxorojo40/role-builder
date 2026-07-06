package com.rolebuilder.core.snes

/**
 * Emparejador AUTOMÁTICO de paletas: dado un gráfico concreto (tiles ya
 * decodificados a índices) y las paletas CGRAM detectadas en la ROM, puntúa
 * cuál le sienta mejor A ESE GRÁFICO en vez de aplicar la misma paleta a todo.
 *
 * La idea que lo hace posible: aunque el emparejado exacto lo deciden las
 * tablas internas de cada juego, el propio DIBUJO delata qué paleta es la suya.
 * En pixel art real, dos índices que aparecen pegados en los píxeles casi
 * siempre son pasos de una rampa de sombreado (mismo tono, distinto brillo) o
 * un contorno oscuro. Con la paleta correcta esos pares vecinos se traducen a
 * colores afines; con una paleta ajena se traducen a saltos de tono aleatorios
 * ("ruido de colores"). Midiendo esa coherencia de tono sobre los pares de
 * índices que de verdad conviven en el dibujo, la paleta correcta destaca sin
 * intervención manual. No es una garantía absoluta —eso solo lo da el juego—,
 * pero convierte el "ve probando paletas a mano" en un valor por defecto que
 * suele acertar y siempre se puede corregir.
 */
object SnesPaletteMatcher {

    /**
     * Radiografía de índices de un gráfico: cuánto se usa cada índice y qué
     * pares de índices DISTINTOS aparecen pegados (horizontal o verticalmente)
     * en los píxeles. El índice 0 (transparente) se ignora: no aporta color.
     */
    class IndexStats(
        val colorCount: Int,
        val usage: IntArray,
        val edges: Map<Int, Int>,
    ) {
        val totalEdges: Long = edges.values.sumOf { it.toLong() }
        val usedIndices: List<Int> = (1 until colorCount).filter { usage[it] > 0 }
    }

    /** Una paleta candidata ya evaluada contra el gráfico. */
    data class Match(
        val source: SnesPalette,
        /** Colores adaptados al formato (ventana elegida + relleno si falta). */
        val colors: IntArray,
        /** Índice inicial de la ventana dentro de la paleta origen (sub-paleta). */
        val window: Int,
        val score: Double,
    ) {
        override fun equals(other: Any?): Boolean =
            other is Match && source == other.source && window == other.window &&
                score == other.score && colors.contentEquals(other.colors)

        override fun hashCode(): Int =
            31 * (31 * source.hashCode() + window) + colors.contentHashCode()
    }

    /** Tope de tiles a radiografiar: más no mejora la señal y sí el coste. */
    private const val MAX_STAT_TILES = 128

    /**
     * Decodifica hasta [tileCount] tiles desde [offset] y acumula el uso de
     * índices y los pares vecinos (clave `(a shl 16) or b` con a < b, ambos > 0).
     */
    fun indexStats(
        data: ByteArray,
        offset: Int,
        format: SnesGraphicFormat,
        tileCount: Int,
    ): IndexStats {
        val usage = IntArray(format.colorCount)
        val edges = HashMap<Int, Int>()
        val tiles = minOf(tileCount, MAX_STAT_TILES)

        fun addEdge(a: Int, b: Int) {
            if (a == 0 || b == 0 || a == b) return
            val key = if (a < b) (a shl 16) or b else (b shl 16) or a
            edges[key] = (edges[key] ?: 0) + 1
        }

        for (t in 0 until tiles) {
            val px = SnesDecoder.decodeTile(data, offset + t * format.bytesPerTile, format, t).pixelIndices
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val v = px[y * 8 + x]
                    if (v < usage.size) usage[v]++
                    if (x < 7) addEdge(v, px[y * 8 + x + 1])
                    if (y < 7) addEdge(v, px[(y + 1) * 8 + x])
                }
            }
        }
        return IndexStats(format.colorCount, usage, edges)
    }

    /**
     * Puntúa (0..1) cuánto encajan estos [colors] con el gráfico radiografiado
     * en [stats]. Combina tres señales:
     *  - coherencia de tono: los pares de índices vecinos en el dibujo deben
     *    traducirse a colores afines (rampa de sombreado) y no a saltos de tono;
     *    los pares con un color muy oscuro se toleran (contornos y sombras), y
     *    los pares que caen en colores casi IDÉNTICOS penalizan (aplanan el arte);
     *  - profundidad de sombreado: los índices usados deben abarcar un rango de
     *    brillo real (una zona plana de la ROM daría un render monocromo);
     *  - viveza: algo de saturación media en los índices usados.
     */
    fun scorePalette(colors: IntArray, stats: IndexStats): Double {
        if (stats.usedIndices.isEmpty()) return 0.0

        fun colorAt(i: Int): Int = if (i < colors.size) colors[i] else 0xFF000000.toInt()
        fun lum(argb: Int): Int {
            val r = (argb shr 16) and 0xFF; val g = (argb shr 8) and 0xFF; val b = argb and 0xFF
            return (r * 30 + g * 59 + b * 11) / 100
        }
        fun sat(argb: Int): Int {
            val r = (argb shr 16) and 0xFF; val g = (argb shr 8) and 0xFF; val b = argb and 0xFF
            return maxOf(r, g, b) - minOf(r, g, b)
        }

        // Coherencia de tono sobre los pares vecinos reales del dibujo.
        var simSum = 0.0
        var weight = 0L
        for ((key, w) in stats.edges) {
            val ca = colorAt(key shr 16)
            val cb = colorAt(key and 0xFFFF)
            simSum += edgeSimilarity(ca, cb) * w
            weight += w
        }
        val hueCoherence = if (weight > 0) simSum / weight else 0.5

        // Profundidad de sombreado: rango de brillo entre los índices usados de
        // verdad (>= 1% de los píxeles no transparentes, para ignorar motas).
        val nonZero = stats.usedIndices.sumOf { stats.usage[it].toLong() }
        val minUse = (nonZero / 100).coerceAtLeast(1)
        val lums = stats.usedIndices.filter { stats.usage[it] >= minUse }.map { lum(colorAt(it)) }
        val depth = if (lums.size >= 2) (lums.max() - lums.min()) / 255.0 else 0.0

        // Viveza media ponderada por uso: un render con algo de color gana a uno
        // mortecino, pero sin llegar a imponerse a la coherencia de tono.
        var satSum = 0.0
        for (i in stats.usedIndices) satSum += sat(colorAt(i)).toDouble() * stats.usage[i]
        val colorfulness = ((satSum / nonZero) / 96.0).coerceAtMost(1.0)

        return hueCoherence * (0.5 + 0.3 * depth + 0.2 * colorfulness)
    }

    /**
     * Afinidad (0..1) entre dos colores que conviven pegados en el dibujo.
     * Casi idénticos = malo (funde índices distintos y aplana el arte); con un
     * extremo muy oscuro = aceptable (contorno/sombra, el tono no informa);
     * resto = 1 menos la distancia de CROMATICIDAD (tono/saturación sin brillo),
     * que es lo que una rampa de sombreado mantiene estable.
     */
    private fun edgeSimilarity(ca: Int, cb: Int): Double {
        val ra = (ca shr 16) and 0xFF; val ga = (ca shr 8) and 0xFF; val ba = ca and 0xFF
        val rb = (cb shr 16) and 0xFF; val gb = (cb shr 8) and 0xFF; val bb = cb and 0xFF

        val rgbDist = kotlin.math.abs(ra - rb) + kotlin.math.abs(ga - gb) + kotlin.math.abs(ba - bb)
        if (rgbDist < 24) return 0.35

        val lumA = (ra * 30 + ga * 59 + ba * 11) / 100
        val lumB = (rb * 30 + gb * 59 + bb * 11) / 100
        if (minOf(lumA, lumB) < 40) return 0.8

        val sumA = (ra + ga + ba + 24).toDouble()
        val sumB = (rb + gb + bb + 24).toDouble()
        val chromaDist = kotlin.math.abs(ra / sumA - rb / sumB) +
            kotlin.math.abs(ga / sumA - gb / sumB) +
            kotlin.math.abs(ba / sumA - bb / sumB)
        return (1.0 - chromaDist / 0.6).coerceIn(0.0, 1.0)
    }

    /**
     * Evalúa cada paleta candidata contra el gráfico y devuelve los emparejados
     * de mejor a peor. Si el formato usa menos colores que la paleta (p. ej.
     * gráficos de 8 colores con paletas CGRAM de 16), se prueban también sus
     * SUB-PALETAS (ventanas alineadas al tamaño del formato): en el SNES real
     * las filas de CGRAM se trocean exactamente así.
     */
    fun rankPalettes(
        data: ByteArray,
        offset: Int,
        format: SnesGraphicFormat,
        tileCount: Int,
        candidates: List<SnesPalette>,
    ): List<Match> {
        if (candidates.isEmpty()) return emptyList()
        val stats = indexStats(data, offset, format, tileCount)
        val n = format.colorCount

        val matches = ArrayList<Match>(candidates.size)
        for (pal in candidates) {
            var best: Match? = null
            val windows = if (pal.colors.size > n) (0..pal.colors.size - n step n).toList() else listOf(0)
            for (w in windows) {
                val colors = IntArray(n) { i ->
                    val idx = w + i
                    if (idx < pal.colors.size) pal.colors[idx] else 0xFF000000.toInt()
                }
                val s = scorePalette(colors, stats)
                if (best == null || s > best.score) best = Match(pal, colors, w, s)
            }
            best?.let { matches.add(it) }
        }
        return matches.sortedByDescending { it.score }
    }

    /**
     * Atajo: los colores de la mejor paleta para este gráfico, ya adaptados al
     * formato, o null si no hay candidatas. Es el "modo automático" que usan la
     * galería, las recetas y la vista previa cuando nadie elige paleta a mano.
     */
    fun bestColors(
        data: ByteArray,
        offset: Int,
        format: SnesGraphicFormat,
        tileCount: Int,
        candidates: List<SnesPalette>,
    ): IntArray? = rankPalettes(data, offset, format, tileCount, candidates).firstOrNull()?.colors
}
