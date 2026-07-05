package com.rolebuilder.core.snes

import com.rolebuilder.core.model.Tileset

/**
 * Puente entre la decodificación cruda de una ROM de SNES y el formato de assets
 * de role-builder. Toma un tramo de tiles a partir de un offset, los decodifica
 * con una paleta y los compone en una hoja de tiles ([ArgbImage]) lista para
 * guardarse como PNG en `images/`, además de sugerir el [Tileset] correspondiente.
 */
object SnesAssetExtractor {

    /** Resultado de una extracción: hoja de píxeles + rejilla usada. */
    data class TileSheet(
        val image: ArgbImage,
        val columns: Int,
        val rows: Int,
        val tileSize: Int,
    )

    /**
     * Extrae [tileCount] tiles de 8×8 desde [offset] con [format], coloreándolos
     * con [palette] (ARGB), y los compone en una rejilla de [columns] columnas.
     *
     * El índice de color 0 se trata como transparente (convención de sprites del
     * SNES) salvo que [transparentIndex0] sea false.
     */
    fun extractTileSheet(
        rom: ByteArray,
        offset: Int,
        format: SnesGraphicFormat,
        palette: IntArray,
        tileCount: Int,
        columns: Int = 16,
        transparentIndex0: Boolean = true,
    ): TileSheet {
        require(tileCount > 0) { "tileCount debe ser positivo" }
        require(columns > 0) { "columns debe ser positivo" }
        val cols = minOf(columns, tileCount)
        val rows = (tileCount + cols - 1) / cols
        val tile = 8
        val image = ArgbImage(cols * tile, rows * tile)

        for (i in 0 until tileCount) {
            val decoded = SnesDecoder.decodeTile(rom, offset + i * format.bytesPerTile, format, i)
            val tileCol = i % cols
            val tileRow = i / cols
            val baseX = tileCol * tile
            val baseY = tileRow * tile
            for (py in 0 until tile) {
                for (px in 0 until tile) {
                    val colorIndex = decoded.pixelIndices[py * tile + px]
                    val argb = when {
                        transparentIndex0 && colorIndex == 0 -> 0x00000000
                        colorIndex < palette.size -> palette[colorIndex]
                        else -> 0xFF000000.toInt()
                    }
                    image.set(baseX + px, baseY + py, argb)
                }
            }
        }
        return TileSheet(image, cols, rows, tile)
    }

    /**
     * Construye un [Tileset] de role-builder que describe la hoja generada por
     * [extractTileSheet]. La imagen se guarda aparte; aquí solo se registran la
     * rejilla y el nombre de archivo. Todos los tiles se marcan transitables por
     * defecto (el usuario los ajusta luego en la Base de datos).
     */
    fun toTileset(sheet: TileSheet, id: Int, name: String, imageFileName: String): Tileset =
        Tileset(
            id = id,
            name = name,
            image = imageFileName,
            tileSize = sheet.tileSize,
            columns = sheet.columns,
            rows = sheet.rows,
            passable = List(sheet.columns * sheet.rows) { true },
        )

    /**
     * Cuántos tiles completos de [format] caben desde [offset] hasta el final de
     * la ROM. Útil para acotar la vista o una extracción "hasta el final".
     */
    fun availableTiles(romSize: Int, offset: Int, format: SnesGraphicFormat): Int {
        if (offset >= romSize) return 0
        return (romSize - offset) / format.bytesPerTile
    }
}
