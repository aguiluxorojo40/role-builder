package com.rolebuilder.core.snes

/**
 * **LAYER 3 del overworld**: el rótulo con el NOMBRE DEL NIVEL que aparece sobre el mapa del
 * mundo al pararse en una casilla-de-nivel.
 *
 * Conviene aclarar algo que se presta a confusión: el overworld de SMW **no tiene un marco
 * decorativo** en Layer 3. Esa capa la usa solo para texto dinámico — el rótulo del nombre de
 * nivel y los mensajes de menú ("CONTINUE AND SAVE"…, *stripe image* nº 30) más un par de
 * flechitas de scroll (nº 21 y 24). Verificado descodificando esas stripe images.
 *
 * El rótulo lo construye `UpdateLevelName` ($04:9D07) en el búfer de subida a VRAM, con la
 * cabecera de *stripe* `50 8B 00 25`: destino **VRAM word $508B**, sin RLE, horizontal, y
 * `0x25 + 1 = 38` bytes = **19 casillas** de tilemap. Cada casilla lleva la tesela del glifo
 * (`byte & 0x7F`) y las propiedades **57 ($39)**, que en el formato de tilemap de la SNES
 * significan: bits 0-1 = 01 → la tesela vive en la **página 1** (`+0x100`), bits 2-4 = 6 →
 * **sub-paleta 6**, bit 5 = prioridad. Como BG3 es 2bpp, la sub-paleta 6 ocupa los colores
 * 24-27 de la CGRAM, que en el overworld los pone `kGlobalPalettes_B5EC` ($00:B5EC).
 *
 * El nombre se ENSAMBLA de tres trozos de un pool compartido, igual que en [SmwLevelNames]
 * (que devuelve el texto ya legible); aquí hacen falta las TESELAS crudas para dibujarlas con
 * la fuente real del juego, así que se recorren las mismas tablas.
 */
object SmwOverworldLayer3 {

    /** Destino en VRAM (words) del rótulo, de la cabecera de stripe de `UpdateLevelName`. */
    const val NAME_VRAM = 0x508B
    /** Casillas del rótulo (38 bytes / 2). */
    const val NAME_TILES = 19
    /** Propiedades de cada casilla del rótulo ($39): página 1, sub-paleta 6, prioridad. */
    const val NAME_PROPS = 0x39

    // Mismas tablas que SmwLevelNames (el rótulo sale del mismo sitio que el texto).
    private const val KLEVELNAMES = 0x04A0FC
    private const val POOL = 0x049AC5
    private const val T1 = 0x049C91
    private const val T2 = 0x049CCF
    private const val T3 = 0x049CED
    /** Offset centinela: trozo vacío y a la vez fin del pool (relleno del rótulo). */
    private const val BLANK = 0x01CB
    private const val MAX_TRANSLEVEL = 0x5F

    /** `kGlobalPalettes_B5EC` ($00:B5EC → PC 0x35EC): la paleta de BG3 en el overworld. */
    private const val PAL_B5EC_PC = 0x35EC

    private fun pc(snes: Int, delta: Int) = (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta

    private fun u8(rom: ByteArray, i: Int) = if (i in rom.indices) rom[i].toInt() and 0xFF else 0

    private fun u16(rom: ByteArray, i: Int) = u8(rom, i) or (u8(rom, i + 1) shl 8)

    /**
     * Emite las teselas de un trozo del pool desde [off]: copia bytes (enmascarados con 0x7F,
     * el bit 7 es paleta) hasta encontrar uno con el bit 7 puesto, que también se emite. Es
     * `UpdateLevelName_049D7F` ($04:9D7F).
     */
    private fun emitPart(rom: ByteArray, delta: Int, off: Int, out: MutableList<Int>, limit: Int) {
        var j = off
        var guard = 0
        while (guard++ < 64) {
            val t = u8(rom, pc(POOL, delta) + j)
            if (out.size < limit) out.add(t and 0x7F)
            j++
            if (t and 0x80 != 0) return
        }
    }

    /**
     * Las **19 teselas** del rótulo del nivel [translevel], listas para dibujar, o null si el
     * translevel no es válido. Port de `UpdateLevelName` ($04:9D07): tres trozos del pool y
     * relleno con el trozo vacío hasta completar el ancho.
     */
    fun levelNameTiles(rom: ByteArray, delta: Int, translevel: Int): IntArray? {
        if (translevel < 0 || translevel > MAX_TRANSLEVEL) return null
        val w = u16(rom, pc(KLEVELNAMES, delta) + 2 * translevel)
        val out = ArrayList<Int>(NAME_TILES)
        // Trozo 1: se salta si su primer byte ya trae el bit 7 (entrada vacía).
        val i1 = u16(rom, pc(T1, delta) + 2 * ((w shr 8) and 0x7F))
        if (u8(rom, pc(POOL, delta) + i1) and 0x80 == 0) emitPart(rom, delta, i1, out, NAME_TILES)
        // Trozo 2: se salta si vale 0x9F (centinela de "sin segunda parte").
        val i2 = u16(rom, pc(T2, delta) + 2 * ((w and 0xF0) shr 4))
        if (u8(rom, pc(POOL, delta) + i2) != 0x9F) emitPart(rom, delta, i2, out, NAME_TILES)
        // Trozo 3: siempre.
        val i3 = u16(rom, pc(T3, delta) + 2 * (w and 0x0F))
        emitPart(rom, delta, i3, out, NAME_TILES)
        // Relleno hasta las 19 casillas con el trozo vacío.
        var guard = 0
        while (out.size < NAME_TILES && guard++ < NAME_TILES) emitPart(rom, delta, BLANK, out, NAME_TILES)
        while (out.size < NAME_TILES) out.add(u8(rom, pc(POOL, delta) + BLANK) and 0x7F)
        return out.take(NAME_TILES).toIntArray()
    }

    /** Paleta de BG3 del overworld: 32 colores ARGB (la sub-paleta 6 son los índices 24-27). */
    fun layer3Palette(rom: ByteArray, delta: Int): IntArray {
        val pal = IntArray(256)
        fun color(i: Int) = SnesDecoder.bgr15ToArgb(
            u8(rom, PAL_B5EC_PC + delta + 2 * i) or (u8(rom, PAL_B5EC_PC + delta + 2 * i + 1) shl 8)
        )
        // LoadColors(kGlobalPalettes_B5EC, dstByte=16, cnt=7, rows=1): 8 colores en 8-15 y 8 en 24-31.
        var k = 0
        for (row in 0..1) {
            val base = 8 + row * 16
            for (c in 0..7) { pal[base + c] = color(k); k++ }
        }
        return pal
    }

    /**
     * El **rótulo del nombre de nivel** renderizado como capa de 256×256 con TRANSPARENCIA
     * alrededor, para componerlo sobre el mapa. Usa los GFX de Layer 3 (ficheros 0x28-0x2B en
     * 2bpp, los mismos que la pantalla de título) y la paleta de BG3 del overworld.
     * Null si la ROM no es SMW o el translevel no vale.
     */
    fun renderLevelName(rom: ByteArray, header: SnesHeader, translevel: Int): ArgbImage? {
        val delta = header.headerOffset - 0x7FC0
        val tiles = SmwTitleScreen.layer3Tiles(rom) ?: return null
        val glyphs = levelNameTiles(rom, delta, translevel) ?: return null
        val pal = layer3Palette(rom, delta)
        val cols = SmwTitleScreen.L3_TILEMAP_COLS
        val img = ArgbImage(cols * 8, (SmwTitleScreen.L3_TILEMAP_SIZE / cols) * 8)
        // Propiedades $39: página 1 (+0x100) y sub-paleta 6 → colores 24-27 (BG3 es 2bpp).
        val page = (NAME_PROPS and 3) shl 8
        val base = ((NAME_PROPS shr 2) and 7) * 4
        val start = NAME_VRAM - SmwTitleScreen.L3_TILEMAP_VRAM
        for (n in glyphs.indices) {
            val cell = start + n
            if (cell < 0 || cell >= SmwTitleScreen.L3_TILEMAP_SIZE) continue
            val px = tiles.getOrNull(page or glyphs[n]) ?: continue
            val ox = (cell % cols) * 8; val oy = (cell / cols) * 8
            for (yy in 0..7) for (xx in 0..7) {
                val ci = px[yy * 8 + xx]
                if (ci != 0) img.set(ox + xx, oy + yy, pal[base + ci])
            }
        }
        return img
    }
}
