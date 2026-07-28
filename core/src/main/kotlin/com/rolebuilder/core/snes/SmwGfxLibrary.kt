package com.rolebuilder.core.snes

/**
 * Biblioteca de GFX de Super Mario World: la capa que el EDITOR usa para SUMINISTRAR y ORDENAR
 * lo que el extractor saca del ROM. Enumera los bancos de gráficos (GFX00..GFX33) y las filas de
 * paleta (CGRAM) reales, y renderiza cualquier banco coloreado con la fila que elijas
 * (*palette-swap* en vivo). Es un catálogo navegable: bancos ordenados por id + 16 paletas por
 * nivel, todo derivado del ROM del usuario (nada se versiona).
 *
 * Reutiliza lo ya construido: [SnesGameRecipes.smwGfxFileDataPublic] (bytes de cada banco,
 * descomprimidos), [SnesGameRecipes.assembleSmwCgram] (las paletas reales del nivel) y
 * [SnesAssetExtractor.extractTileSheet] (bytes + paleta → imagen de teselas).
 */
object SmwGfxLibrary {

    /** Último id de fichero GFX de SMW (GFX00..GFX33). */
    const val LAST_GFX_FILE = 0x33

    /** Nº de filas de paleta de la CGRAM (16 colores por fila). */
    const val PALETTE_ROWS = 16

    /** Un banco de gráficos del ROM: su id, cuántas teselas 8×8 tiene y en qué formato. */
    class Bank(val id: Int, val tileCount: Int, val format: SnesGraphicFormat)

    /**
     * Bancos GFX presentes en el ROM (id 0x00..0x33 con datos decodificables), en orden de id.
     * El formato se detecta por banco; por defecto SMW usa 3bpp. Vacío si el ROM no es SMW.
     */
    fun banks(rom: ByteArray): List<Bank> {
        val out = ArrayList<Bank>()
        for (file in 0..LAST_GFX_FILE) {
            val data = SnesGameRecipes.smwGfxFileDataPublic(rom, file) ?: continue
            val fmt = formatOf(data)
            val tiles = data.size / fmt.bytesPerTile
            if (tiles > 0) out.add(Bank(file, tiles, fmt))
        }
        return out
    }

    /**
     * Las [PALETTE_ROWS] filas de paleta del nivel [level] (16 colores ARGB por fila), tal y como
     * quedan en la CGRAM ensamblada del juego. Son las que alimentan el *palette-swap*: cada fila
     * tiñe un banco de una forma. Lista vacía si el ROM no es SMW.
     */
    fun paletteRows(rom: ByteArray, header: SnesHeader, level: Int = 0x105): List<IntArray> {
        val cg = runCatching {
            SnesGameRecipes.assembleSmwCgram(rom, SnesGameRecipes.smwHeaderDelta(header), level)
        }.getOrNull() ?: return emptyList()
        return (0 until PALETTE_ROWS).map { r -> IntArray(16) { c -> cg.getOrElse(r * 16 + c) { 0 } } }
    }

    /**
     * Renderiza el banco [bankId] coloreado con [paletteRow] (16 colores ARGB; el índice 0 queda
     * transparente). [columns] teselas por fila. null si el banco no existe o no tiene teselas.
     * Cambiar [paletteRow] entre llamadas es el *palette-swap*.
     */
    fun bankSheet(rom: ByteArray, bankId: Int, paletteRow: IntArray, columns: Int = 16): ArgbImage? {
        val data = SnesGameRecipes.smwGfxFileDataPublic(rom, bankId) ?: return null
        val fmt = formatOf(data)
        val tiles = data.size / fmt.bytesPerTile
        if (tiles <= 0) return null
        val pal = if (paletteRow.size >= fmt.colorCount) paletteRow
        else IntArray(fmt.colorCount) { paletteRow.getOrElse(it) { 0xFF000000.toInt() } }
        return SnesAssetExtractor.extractTileSheet(data, 0, fmt, pal, tiles, columns).image
    }

    /** Formato de un banco: detectado, con 3bpp (el habitual en SMW) como respaldo. */
    private fun formatOf(data: ByteArray): SnesGraphicFormat =
        SnesGraphicsScanner.detectBestFormat(data, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
}
