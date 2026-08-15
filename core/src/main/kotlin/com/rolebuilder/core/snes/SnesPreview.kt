package com.rolebuilder.core.snes

import com.rolebuilder.core.snes.compression.CompressionCodecs
import com.rolebuilder.core.snes.compression.LcLz2

/**
 * VISTA PREVIA de gráficos de una ROM: dado un offset, un formato, una paleta y una
 * rejilla, decide QUÉ píxeles hay que enseñar. Es el cerebro del diálogo de importación
 * (`SnesImportDialog`) sacado de la interfaz.
 *
 * Vive aquí, en `:core`, por dos motivos:
 *  - No tiene ni una línea de Android: descomprime, elige paleta y compone una
 *    [SnesAssetExtractor.TileSheet] de píxeles ARGB. Convertir esa hoja en un `Bitmap`
 *    —lo único específico de la plataforma— se queda en la app, que es una llamada.
 *  - Estando en un fichero de interfaz nadie podía probarlo: cada regla ("con este
 *    offset no hay tiles", "si no hay paleta detectada usa los colores vivos",
 *    "agrupa de 2×2 si se piden sprites") solo se validaba MIRANDO la pantalla. Aquí
 *    se comprueba con ROMs sintéticas en el CI.
 */
object SnesPreview {

    /** Índice de paleta especial: colores vivos de respaldo (no hay paleta de la ROM). */
    const val PALETTE_DEFAULT = -1

    /** Índice de paleta especial: escala de grises (para VER la forma de los tiles). */
    const val PALETTE_GRAYSCALE = -2

    /** Índice de paleta especial: la que mejor encaje según [SnesPaletteMatcher]. */
    const val PALETTE_AUTO = -3

    /** Modo de descompresión: los tiles se leen tal cual de la ROM. */
    const val DECOMPRESS_NONE = 0

    /** Modo de descompresión: se prueba cada códec conocido y gana el que dé datos coherentes. */
    const val DECOMPRESS_AUTO = 1

    /** Modo de descompresión: LC_LZ2 (el de Super Mario World) a la fuerza. */
    const val DECOMPRESS_LZ2 = 2

    /**
     * Tope de teselas (o sprites) que se dibujan de una vez. No es una limitación del
     * formato: es que la vista previa se rehace con CADA tecla del offset y componer
     * media ROM en cada pulsación no lo aguanta ningún móvil.
     */
    const val MAX_PREVIEW_TILES = 1024

    /**
     * Colores vivos de respaldo (índice 0 transparente) para cuando no hay una paleta
     * de la ROM: así los gráficos SIEMPRE salen en color, no en gris —que es lo que
     * hace pensar que unos gráficos correctos son ruido—.
     */
    val VIVID_16 = intArrayOf(
        0x00000000, 0xFFE53935.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(),
        0xFFFDD835.toInt(), 0xFF8E24AA.toInt(), 0xFF00ACC1.toInt(), 0xFFF5F5F5.toInt(),
        0xFF6D4C41.toInt(), 0xFFFF7043.toInt(), 0xFF9CCC65.toInt(), 0xFF5C6BC0.toInt(),
        0xFFFFB300.toInt(), 0xFFEC407A.toInt(), 0xFF26A69A.toInt(), 0xFF212121.toInt(),
    )

    /**
     * Paleta de respaldo de [colorCount] colores: los [VIVID_16] y, si el formato pide
     * más (8bpp son 256), negro opaco para el resto. Nunca devuelve menos colores de los
     * pedidos, porque el extractor indexa por color y un índice fuera de rango pintaría
     * de negro sin avisar.
     */
    fun defaultColorPalette(colorCount: Int): IntArray =
        IntArray(colorCount) { i -> if (i < VIVID_16.size) VIVID_16[i] else 0xFF000000.toInt() }

    /**
     * Interpreta lo que hay ESCRITO en la casilla del offset: decimal, o hexadecimal si
     * lleva el prefijo `0x` (como se citan los offsets en la documentación de ROMs).
     * Lo que no sea un número vale 0 en vez de reventar: el usuario está TECLEANDO y
     * "0x" a medias no es un error, es un offset todavía sin escribir.
     */
    fun parseOffset(text: String): Int {
        val s = text.trim()
        val value = if (s.startsWith("0x", ignoreCase = true)) s.substring(2).toIntOrNull(16) else s.toIntOrNull()
        return (value ?: 0).coerceAtLeast(0)
    }

    /** De dónde salen los tiles: los bytes (ROM o bloque ya descomprimido) y el offset dentro de ellos. */
    class TileSource(val data: ByteArray, val offset: Int)

    /**
     * Resuelve la fuente de tiles según el modo de descompresión. Ojo: al descomprimir,
     * los tiles salen del bloque resultante (offset 0), pero la PALETA se sigue leyendo
     * de la ROM original — por eso esto solo decide de dónde salen los píxeles.
     * Devuelve null si se pidió descomprimir y en ese offset no hay un bloque válido.
     */
    fun tileSource(rom: ByteArray, offset: Int, format: SnesGraphicFormat, mode: Int): TileSource? = when (mode) {
        DECOMPRESS_AUTO ->
            CompressionCodecs.autoDecompress(rom, offset, format)?.let { TileSource(it.result.data, 0) }
        DECOMPRESS_LZ2 ->
            runCatching { LcLz2.decompress(rom, offset) }.getOrNull()?.let { TileSource(it.data, 0) }
        else -> TileSource(rom, offset)
    }

    /**
     * La paleta elegida y, cuando la eligió el emparejador, CÓMO se llama la elegida
     * (nombre de la paleta CGRAM y sub-paleta) para poder decírselo a quien mira: si el
     * color no convence, hay que saber qué se está usando para elegir otra.
     */
    class PaletteChoice(val colors: IntArray, val autoLabel: String? = null)

    /**
     * Colores con los que pintar [tileCount] teselas de [source]: escala de grises,
     * automática (la mejor de [detected] para ESTOS tiles), una concreta de la ROM por
     * índice, o los colores vivos de respaldo.
     */
    fun paletteFor(
        source: TileSource,
        format: SnesGraphicFormat,
        tileCount: Int,
        paletteIndex: Int,
        detected: List<SnesPalette>,
    ): PaletteChoice = when {
        paletteIndex == PALETTE_GRAYSCALE -> PaletteChoice(SnesDecoder.grayscalePalette(format.colorCount))
        paletteIndex == PALETTE_AUTO -> autoPalette(source, format, tileCount, detected)
        paletteIndex >= 0 -> PaletteChoice(
            detected.getOrNull(paletteIndex)?.colors ?: defaultColorPalette(format.colorCount),
        )
        else -> PaletteChoice(defaultColorPalette(format.colorCount))
    }

    /**
     * El emparejador puntúa cada paleta detectada CONTRA estos tiles y se queda con la
     * que mejor les sienta (y con su sub-paleta). Sin candidatas, los colores vivos.
     */
    private fun autoPalette(
        source: TileSource,
        format: SnesGraphicFormat,
        tileCount: Int,
        detected: List<SnesPalette>,
    ): PaletteChoice {
        val match = SnesPaletteMatcher
            .rankPalettes(source.data, source.offset, format, tileCount, detected)
            .firstOrNull()
            ?: return PaletteChoice(defaultColorPalette(format.colorCount))
        val window = if (match.window > 0) {
            " · colores ${match.window}-${match.window + format.colorCount - 1}"
        } else {
            ""
        }
        return PaletteChoice(match.colors, match.source.name + window)
    }

    /** Los parámetros que el usuario ajusta en el diálogo, en un solo bulto. */
    class Request(
        val format: SnesGraphicFormat,
        val offsetText: String,
        val columns: Int,
        val tiles: Int,
        val paletteIndex: Int = PALETTE_DEFAULT,
        val decompressMode: Int = DECOMPRESS_NONE,
        /** > 1 agrupa bloques de N×N teselas en un sprite entero (atlas). */
        val spriteTiles: Int = 1,
        /** Paletas CGRAM halladas en la ROM, candidatas de la elección automática. */
        val detected: List<SnesPalette> = emptyList(),
    )

    /** Resultado de la vista previa: la hoja de píxeles y, en modo automático, qué paleta se usó. */
    class Sheet(val sheet: SnesAssetExtractor.TileSheet, val autoPalette: String?)

    /**
     * Compone la vista previa con los parámetros actuales. Devuelve null cuando en ese
     * offset/formato no hay datos que enseñar (o al descomprimir no sale un bloque
     * válido): la interfaz lo traduce a "aquí no hay gráficos", que es información útil
     * mientras se busca a mano por la ROM.
     */
    fun compute(rom: ByteArray, req: Request): Sheet? = runCatching { computeSheet(rom, req) }.getOrNull()

    private fun computeSheet(rom: ByteArray, req: Request): Sheet? {
        val source = tileSource(rom, parseOffset(req.offsetText), req.format, req.decompressMode) ?: return null
        val available = SnesAssetExtractor.availableTiles(source.data.size, source.offset, req.format)
        if (available <= 0) return null
        val count = req.tiles.coerceIn(1, minOf(available, MAX_PREVIEW_TILES))
        val palette = paletteFor(source, req.format, count, req.paletteIndex, req.detected)
        val sheet = extractSheet(source, req, palette.colors, count) ?: return null
        return Sheet(sheet, palette.autoLabel)
    }

    /** Hoja de teselas sueltas, o atlas de sprites si se pidió agrupar. */
    private fun extractSheet(
        source: TileSource,
        req: Request,
        colors: IntArray,
        count: Int,
    ): SnesAssetExtractor.TileSheet? {
        val columns = req.columns.coerceAtLeast(1)
        if (req.spriteTiles <= 1) {
            return SnesAssetExtractor.extractTileSheet(
                source.data, source.offset, req.format, colors, count, columns,
            )
        }
        val availSprites = SnesAssetExtractor.availableSprites(
            source.data.size, source.offset, req.format, req.spriteTiles, req.spriteTiles,
        )
        if (availSprites <= 0) return null
        val spriteCount = (count / (req.spriteTiles * req.spriteTiles))
            .coerceIn(1, minOf(availSprites, MAX_PREVIEW_TILES))
        return SnesAssetExtractor.extractSpriteAtlas(
            source.data, source.offset, req.format, colors, spriteCount,
            req.spriteTiles, req.spriteTiles, columns,
        )
    }
}
