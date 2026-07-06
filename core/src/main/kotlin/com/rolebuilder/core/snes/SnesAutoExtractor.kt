package com.rolebuilder.core.snes

import com.rolebuilder.core.snes.compression.CompressionCodecs

/**
 * Extracción AUTOMÁTICA de gráficos para el "modo fácil": una sola llamada que
 * hace por dentro todo lo técnico (buscar zonas, descomprimir, adivinar el
 * formato de bits, elegir paleta y componer una vista en color) y devuelve una
 * GALERÍA de resultados listos para enseñar como miniaturas. El usuario no ve ni
 * un offset ni un "bpp": solo toca las imágenes que le gusten.
 *
 * La galería visual es, además, el mejor filtro de ruido: en ROMs muy
 * comprimidas la autodetección puede colar algún bloque que no es un dibujo,
 * pero el ojo humano lo descarta de un vistazo entre las miniaturas.
 */
object SnesAutoExtractor {

    /** Un hallazgo de la búsqueda automática, con todo resuelto y su vista en color. */
    data class Finding(
        val image: ArgbImage,
        val label: String,
        val offset: Int,
        val compressed: Boolean,
        val format: SnesGraphicFormat,
        val palette: IntArray,
        val tileCount: Int,
        val columns: Int,
        val score: Double,
    )

    /**
     * Busca gráficos en [rom] y devuelve hasta [maxResults] hallazgos ordenados
     * de mejor a peor, cada uno ya descomprimido/decodificado/coloreado.
     *
     * @param previewTiles cuántos tiles compone cada miniatura.
     * @param maxScanBytes tope del barrido de bloques comprimidos (para que en el
     *   dispositivo tarde poco); 0 = toda la ROM.
     */
    fun findGraphics(
        rom: ByteArray,
        maxResults: Int = 24,
        previewTiles: Int = 96,
        columns: Int = 16,
        maxScanBytes: Int = 0,
    ): List<Finding> {
        val palettes = SnesDecoder.scanRomForPalettes(rom)
        val bestPalette: (SnesGraphicFormat) -> IntArray = { fmt ->
            palettes.firstOrNull()?.colors?.let { adaptPalette(it, fmt.colorCount) }
                ?: SnesDecoder.grayscalePalette(fmt.colorCount)
        }

        data class Cand(val offset: Int, val compressed: Boolean)
        val cands = LinkedHashSet<Cand>()

        // 1) Zonas SIN comprimir (rápido): tilesets y hojas de sprites, varios bpp.
        for (fmt in SnesGraphicsScanner.DETECTABLE_FORMATS) {
            SnesGraphicsScanner.findCandidates(rom, fmt, maxResults = 12).forEach { cands.add(Cand(it.offset, false)) }
            SnesGraphicsScanner.findSpriteCandidates(rom, fmt, maxResults = 12).forEach { cands.add(Cand(it.offset, false)) }
        }
        // 2) Bloques COMPRIMIDOS (acotado para no tardar): p. ej. Super Mario World.
        CompressionCodecs.findCompressedBlocks(rom, maxResults = maxResults, maxScanBytes = maxScanBytes)
            .forEach { cands.add(Cand(it, true)) }

        val findings = ArrayList<Finding>()
        for (c in cands) {
            // Datos finales (descomprimidos o crudos) sobre los que trabajar.
            val (data, dataOffset) = if (c.compressed) {
                val auto = CompressionCodecs.autoDecompress(rom, c.offset, SnesGraphicFormat.SNES_4BPP)
                    ?: continue
                auto.result.data to 0
            } else {
                rom to c.offset
            }
            val guess = SnesGraphicsScanner.detectBestFormat(data, dataOffset) ?: continue
            // Sólo lo que de verdad parece un dibujo: mejor 8 miniaturas limpias
            // que 24 con ruido (la galería es lo que ve el usuario, no un log).
            if (guess.fitness < 0.36) continue
            val fmt = guess.format
            val available = SnesAssetExtractor.availableTiles(data.size, dataOffset, fmt)
            if (available < 16) continue
            val count = minOf(available, previewTiles)
            val palette = bestPalette(fmt)
            val sheet = SnesAssetExtractor.extractTileSheet(data, dataOffset, fmt, palette, count, columns)
            findings.add(
                Finding(
                    image = sheet.image,
                    label = "Gráfico",
                    offset = c.offset,
                    compressed = c.compressed,
                    format = fmt,
                    palette = palette,
                    tileCount = count,
                    columns = columns,
                    score = guess.fitness,
                )
            )
        }

        // Ordena por calidad, deduplica por cercanía de offset y numera.
        val ranked = findings.sortedByDescending { it.score }
        val kept = ArrayList<Finding>()
        for (f in ranked) {
            if (kept.any { it.compressed == f.compressed && kotlin.math.abs(it.offset - f.offset) < 32 }) continue
            kept.add(f)
            if (kept.size >= maxResults) break
        }
        return kept.mapIndexed { i, f -> f.copy(label = "Gráfico ${i + 1}") }
    }

    /** Ajusta una paleta detectada al nº de colores del formato (recorta o rellena). */
    private fun adaptPalette(src: IntArray, colorCount: Int): IntArray =
        IntArray(colorCount) { if (it < src.size) src[it] else 0xFF000000.toInt() }
}
