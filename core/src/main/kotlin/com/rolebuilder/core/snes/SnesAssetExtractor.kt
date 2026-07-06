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
    ): TileSheet = extractTileSheet(rom, offset, format, tileCount, columns, transparentIndex0) { palette }

    /**
     * Igual que la sobrecarga con una [IntArray] única, pero elige la paleta POR
     * TILE mediante [paletteForTile] `(índice de tile en la hoja) -> colores ARGB`.
     *
     * Es lo que permite pintar un fondo con su color REAL: en el SNES cada tile de
     * un fondo puede llevar una sub-paleta distinta que ordena el tilemap (ver
     * [SnesTilemap]). Con una sola paleta global todos los tiles salían teñidos
     * igual; aquí cada uno recibe su fila asignada. La sobrecarga de una paleta
     * delega en esta pasando la misma para todos, así que el camino sin tilemap no
     * cambia.
     */
    fun extractTileSheet(
        rom: ByteArray,
        offset: Int,
        format: SnesGraphicFormat,
        tileCount: Int,
        columns: Int = 16,
        transparentIndex0: Boolean = true,
        paletteForTile: (Int) -> IntArray,
    ): TileSheet {
        require(tileCount > 0) { "tileCount debe ser positivo" }
        require(columns > 0) { "columns debe ser positivo" }
        val cols = minOf(columns, tileCount)
        val rows = (tileCount + cols - 1) / cols
        val tile = 8
        val image = ArgbImage(cols * tile, rows * tile)

        for (i in 0 until tileCount) {
            val decoded = SnesDecoder.decodeTile(rom, offset + i * format.bytesPerTile, format, i)
            val palette = paletteForTile(i)
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
     * Compone un **atlas de sprites** agrupando bloques de [spriteTilesW]×[spriteTilesH]
     * tiles de 8×8 en una sola celda (p. ej. 2×2 → un sprite de 16×16). Dentro de
     * cada sprite los tiles se leen en orden de lectura (arriba-izq, arriba-der,
     * …, abajo-der), que es como la mayoría de juegos de SNES guardan un sprite
     * grande: varios 8×8 consecutivos. Así los personajes y objetos salen ENTEROS
     * en el atlas en vez de partidos en trozos sueltos.
     *
     * Los sprites se disponen en una rejilla de [columns] sprites por fila. Con
     * [spriteTilesW] = [spriteTilesH] = 1 equivale a [extractTileSheet].
     */
    fun extractSpriteAtlas(
        rom: ByteArray,
        offset: Int,
        format: SnesGraphicFormat,
        palette: IntArray,
        spriteCount: Int,
        spriteTilesW: Int = 2,
        spriteTilesH: Int = 2,
        columns: Int = 8,
        transparentIndex0: Boolean = true,
    ): TileSheet {
        require(spriteCount > 0) { "spriteCount debe ser positivo" }
        require(spriteTilesW > 0 && spriteTilesH > 0) { "el tamaño del sprite debe ser positivo" }
        require(columns > 0) { "columns debe ser positivo" }
        val tile = 8
        val tilesPerSprite = spriteTilesW * spriteTilesH
        val spriteCols = minOf(columns, spriteCount)
        val spriteRows = (spriteCount + spriteCols - 1) / spriteCols
        val spriteW = spriteTilesW * tile
        val spriteH = spriteTilesH * tile
        val image = ArgbImage(spriteCols * spriteW, spriteRows * spriteH)

        for (s in 0 until spriteCount) {
            val originX = (s % spriteCols) * spriteW
            val originY = (s / spriteCols) * spriteH
            for (ty in 0 until spriteTilesH) {
                for (tx in 0 until spriteTilesW) {
                    val tileIndex = s * tilesPerSprite + ty * spriteTilesW + tx
                    val decoded = SnesDecoder.decodeTile(
                        rom, offset + tileIndex * format.bytesPerTile, format, tileIndex,
                    )
                    val baseX = originX + tx * tile
                    val baseY = originY + ty * tile
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
            }
        }
        // tileSize describe la celda del atlas: el lado del sprite (cuadrado habitual).
        return TileSheet(image, spriteCols, spriteRows, spriteW)
    }

    /**
     * Cuántos sprites completos de [spriteTilesW]×[spriteTilesH] tiles caben desde
     * [offset] con [format] hasta el final de la ROM.
     */
    fun availableSprites(
        romSize: Int,
        offset: Int,
        format: SnesGraphicFormat,
        spriteTilesW: Int,
        spriteTilesH: Int,
    ): Int = availableTiles(romSize, offset, format) / (spriteTilesW * spriteTilesH).coerceAtLeast(1)

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
