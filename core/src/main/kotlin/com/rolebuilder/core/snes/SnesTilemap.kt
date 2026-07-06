package com.rolebuilder.core.snes

/**
 * Lectura del **tilemap** de fondo (BG) del SNES: la tabla que de verdad decide
 * qué SUB-PALETA usa cada tile de un fondo.
 *
 * Por qué existe este motor: el color en SNES NO vive en el gráfico. El tile solo
 * guarda índices de color (0..15 en 4bpp); qué FILA de la CGRAM (una de las 16
 * filas de 16 colores) le toca a ese tile lo dice el tilemap, no el gráfico. Hoy
 * [SnesPaletteMatcher] ADIVINA una paleta buena para toda una hoja mirando el
 * propio dibujo; funciona sorprendentemente bien pero pinta TODOS los tiles con
 * la misma fila. En un fondo real, dos tiles vecinos de la misma hoja pueden
 * llevar sub-paletas distintas (p. ej. el mismo ladrillo en versión "día" y
 * "sombra"). Este motor lee la asignación EXACTA del tilemap y colorea cada tile
 * con SU fila real: es la corrección de fondo del problema, no un heurístico.
 *
 * Formato de una entrada de tilemap (2 bytes little-endian, layout estándar SNES):
 *  - bits 0-9  (0x03FF): número de tile
 *  - bits 10-12 (0x1C00): índice de sub-paleta (0..7)  ← lo que buscamos
 *  - bit 13 (0x2000): prioridad
 *  - bit 14 (0x4000): flip horizontal
 *  - bit 15 (0x8000): flip vertical
 *
 * Como el resto de `core/snes`, es Kotlin JVM puro (sin AWT ni Android) y reutiliza
 * [SnesDecoder.parsePalette] para leer la CGRAM. La CGRAM se maneja como un
 * `IntArray` de ARGB con 16 filas de 16 colores (256 en total).
 */
object SnesTilemap {

    /** Nº de sub-paletas direccionables por los bits 10-12 (3 bits → 0..7). */
    const val PALETTE_COUNT = 8

    /** Colores por fila de CGRAM (una sub-paleta de 4bpp). */
    const val COLORS_PER_ROW = 16

    /** Filas de CGRAM completas (256 colores / 16). */
    const val CGRAM_ROWS = 16

    /** Una entrada de tilemap ya decodificada a sus campos. */
    data class TilemapEntry(
        /** Número de tile referenciado (bits 0-9, 0..1023). */
        val tileIndex: Int,
        /** Sub-paleta / fila de CGRAM asignada (bits 10-12, 0..7). */
        val palette: Int,
        /** Prioridad de dibujado (bit 13). */
        val priority: Boolean,
        /** Espejo horizontal (bit 14). */
        val hFlip: Boolean,
        /** Espejo vertical (bit 15). */
        val vFlip: Boolean,
    ) {
        /** Reconstruye la palabra de 16 bits original (útil para tests/round-trip). */
        fun toWord(): Int =
            (tileIndex and 0x03FF) or
                ((palette and 0x07) shl 10) or
                (if (priority) 0x2000 else 0) or
                (if (hFlip) 0x4000 else 0) or
                (if (vFlip) 0x8000 else 0)
    }

    private fun byte(rom: ByteArray, i: Int): Int =
        if (i < 0 || i >= rom.size) 0 else rom[i].toInt() and 0xFF

    // ----------------------------------------------------------------- parser

    /** Decodifica una única palabra de 16 bits a sus campos. Función pura. */
    fun decodeEntry(word: Int): TilemapEntry = TilemapEntry(
        tileIndex = word and 0x03FF,
        palette = (word shr 10) and 0x07,
        priority = word and 0x2000 != 0,
        hFlip = word and 0x4000 != 0,
        vFlip = word and 0x8000 != 0,
    )

    /**
     * Lee [entryCount] entradas de tilemap (2 bytes little-endian cada una) desde
     * [offset] y las decodifica. Función pura: los bytes que caigan fuera de la ROM
     * se leen como 0 (entrada vacía: tile 0, paleta 0), igual que
     * [SnesDecoder.parsePalette] rellena con negro, para no reventar en los bordes.
     */
    fun parseTilemap(rom: ByteArray, offset: Int, entryCount: Int): List<TilemapEntry> {
        if (entryCount <= 0) return emptyList()
        val out = ArrayList<TilemapEntry>(entryCount)
        for (i in 0 until entryCount) {
            val idx = offset + 2 * i
            val word = byte(rom, idx) or (byte(rom, idx + 1) shl 8)
            out.add(decodeEntry(word))
        }
        return out
    }

    // --------------------------------------------------------- asignación real

    /**
     * Extrae los 16 colores de la fila [paletteIndex] (0..15) de una [cgram] de 256
     * colores ARGB. Los índices que se salgan se rellenan con negro opaco.
     */
    fun paletteRow(cgram: IntArray, paletteIndex: Int): IntArray {
        val base = paletteIndex * COLORS_PER_ROW
        return IntArray(COLORS_PER_ROW) { i ->
            val idx = base + i
            if (idx in cgram.indices) cgram[idx] else 0xFF000000.toInt()
        }
    }

    /**
     * Lee una CGRAM completa (16 filas × 16 colores = 256) desde [offset] de la ROM
     * reutilizando [SnesDecoder.parsePalette]. Atajo cómodo para el motor.
     */
    fun readCgram(rom: ByteArray, offset: Int): IntArray =
        SnesDecoder.parsePalette(rom, offset, CGRAM_ROWS * COLORS_PER_ROW)

    /**
     * Núcleo de la corrección: para cada tile REFERENCIADO en [entries], decide con
     * qué sub-paleta lo pinta el fondo. Devuelve `tileIndex -> paleta (0..7)`.
     *
     * Un mismo tile puede aparecer muchas veces en un fondo y, en teoría, con filas
     * distintas; nos quedamos con la MAYORITARIA (la fila con la que ese tile se
     * dibuja más veces). En un fondo normal el reparto es casi siempre unánime, así
     * que la mayoría es la respuesta correcta y estable; el desempate por la fila
     * más baja lo hace determinista. Con [tileRange] se limita al rango de tiles de
     * la hoja extraída (se ignoran referencias a otras zonas de VRAM).
     */
    fun assignedPaletteByTile(
        entries: List<TilemapEntry>,
        tileRange: IntRange? = null,
    ): Map<Int, Int> {
        // tileIndex -> (paleta -> nº de veces que ese tile usa esa paleta)
        val counts = HashMap<Int, IntArray>()
        for (e in entries) {
            if (tileRange != null && e.tileIndex !in tileRange) continue
            val row = counts.getOrPut(e.tileIndex) { IntArray(PALETTE_COUNT) }
            row[e.palette]++
        }
        // Orden estable por tileIndex; mayoría con desempate por paleta más baja.
        val result = LinkedHashMap<Int, Int>()
        for (tile in counts.keys.sorted()) {
            val row = counts.getValue(tile)
            var best = 0
            for (p in 1 until PALETTE_COUNT) if (row[p] > row[best]) best = p
            result[tile] = best
        }
        return result
    }

    /**
     * Igual que [assignedPaletteByTile] pero ya resuelto a COLORES: para cada tile
     * referenciado, los 16 colores de su fila real de la [cgram]. Es lo que consume
     * el render para pintar cada tile con SU sub-paleta en vez de con una global.
     */
    fun assignPalettesByTile(
        entries: List<TilemapEntry>,
        cgram: IntArray,
        tileRange: IntRange? = null,
    ): Map<Int, IntArray> {
        val byPalette = assignedPaletteByTile(entries, tileRange)
        val result = LinkedHashMap<Int, IntArray>(byPalette.size)
        for ((tile, palette) in byPalette) {
            result[tile] = paletteRow(cgram, palette)
        }
        return result
    }

    // ----------------------------------------------------- detector heurístico

    /**
     * Una región candidata a ser un tilemap sin comprimir que referencia el rango
     * de tiles buscado.
     *
     * @param offset offset (en bytes, alineado a 2) donde arranca el run.
     * @param entryCount número de entradas de 2 bytes del run.
     * @param score puntuación 0..1 (mayor = más "parece un fondo real").
     * @param distinctPalettes cuántas sub-paletas distintas usa (un fondo real usa pocas).
     * @param distinctTiles cuántos tiles distintos referencia.
     * @param inRangeFraction fracción de entradas cuyo tile cae dentro del rango buscado.
     */
    data class TilemapRegion(
        val offset: Int,
        val entryCount: Int,
        val score: Double,
        val distinctPalettes: Int,
        val distinctTiles: Int,
        val inRangeFraction: Double,
    )

    /**
     * Escanea la ROM buscando runs de entradas de 2 bytes que "parezcan" un tilemap
     * que referencia [tileRange] (el rango de tiles de la hoja que se extrajo).
     *
     * Cómo funciona (dos señales complementarias que juntas descartan el ruido):
     *
     *  1. **Continuidad dentro del rango.** Recorre la ROM en offsets pares y va
     *     extendiendo un run mientras cada entrada sea "aceptable": su tile cae en
     *     [tileRange] o es el tile 0 (el hueco/blanco típico de los fondos). En
     *     datos aleatorios o de código, el tile (10 bits) rara vez cae en un rango
     *     acotado muchas veces SEGUIDAS, así que los runs se cortan enseguida y no
     *     alcanzan [minEntries]. Esta señal por sí sola ya filtra casi todo el ruido
     *     cuando el rango no es la ROM entera.
     *
     *  2. **Pocas sub-paletas y concentradas.** Un fondo real usa un puñado de filas
     *     de CGRAM, no las 8 al azar: se puntúa alto tener `distinctPalettes` bajo y
     *     que la mayoría de entradas compartan una misma fila. Esta señal cubre el
     *     caso en que [tileRange] es muy amplio (todo "cae dentro" y la señal 1 no
     *     discrimina): ahí el ruido delata sus bits 10-12 aleatorios (las 8 paletas
     *     repartidas) y puntúa bajo.
     *
     * También exige variedad mínima de tiles (un bloque de un solo tile o todo
     * blanco no es un mapa útil que colorear) y penaliza los runs cortos.
     *
     * Devuelve las regiones ordenadas por puntuación (mejor primero), sin
     * solapamientos, hasta [maxResults].
     *
     * LÍMITES HONESTOS: esto SOLO encuentra tilemaps almacenados SIN COMPRIMIR, igual
     * que [SnesGraphicsScanner] con los gráficos. Muchísimos juegos comprimen el
     * tilemap (LZ/RLE propios) o lo montan por hardware/DMA en tiempo de ejecución;
     * de esos no hay bytes contiguos que reconocer y este detector no devolverá nada
     * (la respuesta honesta). Tampoco conoce el ancho del fondo ni su offset base de
     * VRAM: entrega bytes candidatos, no una garantía. Cuando no aparece ningún
     * tilemap, el respaldo sigue siendo [SnesPaletteMatcher] (adivinar por el dibujo).
     */
    fun detectTilemapRegions(
        rom: ByteArray,
        tileRange: IntRange,
        minEntries: Int = 32,
        maxDistinctPalettes: Int = 4,
        maxResults: Int = 16,
    ): List<TilemapRegion> {
        if (rom.size < 4 || tileRange.isEmpty() || minEntries <= 0) return emptyList()

        val candidates = ArrayList<TilemapRegion>()
        var offset = 0
        val lastStart = rom.size - 2
        while (offset <= lastStart) {
            // Extiende un run de entradas aceptables (en rango o tile 0 = hueco).
            var end = offset
            while (end + 1 < rom.size) {
                val word = byte(rom, end) or (byte(rom, end + 1) shl 8)
                val tile = word and 0x03FF
                if (tile != 0 && tile !in tileRange) break
                end += 2
            }
            val entryCount = (end - offset) / 2
            if (entryCount >= minEntries) {
                val region = scoreRun(rom, offset, entryCount, tileRange, maxDistinctPalettes)
                if (region != null) candidates.add(region)
                // Salta el run entero: los runs no se solapan.
                offset = end
            } else {
                offset += 2
            }
        }

        candidates.sortWith(compareByDescending<TilemapRegion> { it.score }.thenBy { it.offset })
        return if (candidates.size <= maxResults) candidates else candidates.subList(0, maxResults).toList()
    }

    /**
     * Puntúa un run ya delimitado. Devuelve null si no cumple lo mínimo para ser un
     * fondo útil (demasiadas paletas, un solo tile, o todo hueco).
     */
    private fun scoreRun(
        rom: ByteArray,
        offset: Int,
        entryCount: Int,
        tileRange: IntRange,
        maxDistinctPalettes: Int,
    ): TilemapRegion? {
        val palUse = IntArray(PALETTE_COUNT)
        val tiles = HashSet<Int>()
        var inRange = 0
        var blanks = 0
        for (i in 0 until entryCount) {
            val idx = offset + 2 * i
            val word = byte(rom, idx) or (byte(rom, idx + 1) shl 8)
            val tile = word and 0x03FF
            palUse[(word shr 10) and 0x07]++
            if (tile == 0) blanks++
            if (tile in tileRange) { inRange++; if (tile != 0) tiles.add(tile) }
        }

        val distinctPalettes = palUse.count { it > 0 }
        val distinctTiles = tiles.size
        // Un mapa que colorear necesita variedad real de tiles (no todo hueco ni un
        // único tile repetido) y no puede usar más filas que el tope plausible.
        if (distinctTiles < 2) return null
        if (distinctPalettes > maxDistinctPalettes) return null

        val total = entryCount.toDouble()
        val inRangeFraction = inRange / total
        // Concentración: fracción de entradas en la sub-paleta dominante (un fondo
        // real la tiene alta; el ruido reparte las 8 y la baja).
        val topPalette = palUse.max()
        val concentration = topPalette / total
        // Longitud: satura a 256 entradas (un fondo de pantalla 32×28 ≈ 900, pero con
        // 256 ya hay señal de sobra; no premiamos runs gigantes por serlo).
        val lengthScore = (entryCount / 256.0).coerceAtMost(1.0)
        // Menos paletas = mejor (1 fila → 1.0; maxDistinct → cae linealmente).
        val paletteScore = (1.0 - (distinctPalettes - 1) / PALETTE_COUNT.toDouble()).coerceIn(0.0, 1.0)

        val score = paletteScore * (0.35 * lengthScore + 0.30 * inRangeFraction + 0.35 * concentration)
        return TilemapRegion(
            offset = offset,
            entryCount = entryCount,
            score = score,
            distinctPalettes = distinctPalettes,
            distinctTiles = distinctTiles,
            inRangeFraction = inRangeFraction,
        )
    }
}
