package com.rolebuilder.core.snes

import com.rolebuilder.core.snes.compression.LcLz2

/**
 * "Recetas por juego": para ROMs concretas y conocidas sabemos EXACTAMENTE dónde
 * están sus gráficos (p. ej. la tabla de punteros de Super Mario World), así que
 * podemos ofrecer una extracción limpia de un solo toque, sin que el usuario
 * toque offsets ni formatos. Es la vía FIABLE para el "modo fácil": a diferencia
 * de la búsqueda automática a ciegas, aquí no hay ruido porque seguimos el mapa
 * real del juego.
 */
object SnesGameRecipes {

    /** Nombre legible del juego reconocido, o null si no hay receta para esta ROM. */
    fun detect(header: SnesHeader): String? {
        val title = header.title.trim().uppercase()
        return when {
            title.startsWith("SUPER MARIOWORLD") || title.startsWith("SUPER MARIO WORLD") -> "Super Mario World"
            else -> null
        }
    }

    /**
     * Extrae los gráficos del juego reconocido como una galería limpia y en color.
     * Devuelve vacío si no hay receta o no se pudo localizar su mapa gráfico.
     */
    fun extract(rom: ByteArray, header: SnesHeader): List<SnesAutoExtractor.Finding> = when (detect(header)) {
        "Super Mario World" -> extractSmw(rom, header)
        else -> emptyList()
    }

    // ------------------------------------------------------------ Super Mario World

    // -------- Paletas REALES de SMW (tablas fijas, sin comprimir, banco $00) --------
    //
    // SMW ensambla la CGRAM de cada nivel desde tablas fijas en el banco $00, en
    // BGR15 little-endian (lo mismo que lee SnesDecoder.parsePalette). Mapeo LoROM
    // banco $00: PC = SNES - 0x8000. Si la ROM trae cabecera de copiador SMC de 512
    // bytes, se suma un delta de 0x200 a cada offset PC (ver smwHeaderDelta).
    //
    // Todas las direcciones son offsets PC "unheadered". Las tablas teselan sin
    // huecos: B0A0+0x10=B0B0, B0B0+0xC0=B170, B190+0xC0=B250, B250+0x78=B2C8,
    // B2C8+0x50=B318, B318+0xC0=B3D8.

    /**
     * Índice de sub-paleta FG por defecto. Verificado contra la ROM real US 1.0:
     * el índice 0 es la paleta de terreno clásica (marrones, verdes y grises de
     * suelo/hierba), mientras que otros índices tiran a azules/rojos de fortaleza.
     * Es un parámetro estético; el 0 da el aspecto de pradera reconocible.
     */
    const val SMW_DEFAULT_FG_INDEX = 0
    /** Índice de color de "back area" por defecto (parámetro estético). */
    const val SMW_DEFAULT_BACK_INDEX = 0
    /** Índice de sub-paleta de sprites por defecto (parámetro estético). */
    const val SMW_DEFAULT_SPRITE_INDEX = 0

    /** Back area color: $00B0A0 + 2*x → PC 0x30A0 + 2*x (x=0..7, 1 color c/u). */
    internal const val SMW_BACK_AREA_PC = 0x30A0
    /** BG palette: $00B0B0 + 0x18*x → PC 0x30B0 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_BG_PC = 0x30B0
    /** FG palette: $00B190 + 0x18*x → PC 0x3190 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_FG_PC = 0x3190
    /** Fijas filas 4-D: $00B250 + 0x0C*(row-4) → PC 0x3250 + 0x0C*(row-4) (6 colores). */
    internal const val SMW_FIXED_PC = 0x3250
    /** Player palette: $00B2C8 + 0x14*p → PC 0x32C8 + 0x14*p (10 colores). */
    internal const val SMW_PLAYER_PC = 0x32C8
    /** Sprite palette: $00B318 + 0x18*x → PC 0x3318 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_SPRITE_PC = 0x3318

    /** Negro opaco (índice de contorno/sombra en las filas 3bpp/4bpp). */
    private val SMW_BLACK = 0xFF000000.toInt()
    /** Transparente: la hoja de tiles ya trata el índice 0 como transparente. */
    private const val SMW_TRANSPARENT = 0

    /**
     * Delta de offset por cabecera de copiador SMC. Se deriva de [SnesHeader.headerOffset]:
     * 0x7FC0 (sin cabecera) → 0; 0x7FC0+512 = 0x81C0 (con cabecera) → 0x200.
     */
    internal fun smwHeaderDelta(header: SnesHeader): Int = header.headerOffset - 0x7FC0

    /**
     * Lecturas de las tablas de paleta reales de SMW sobre [rom], ya aplicado el
     * [delta] de cabecera. Todas devuelven ARGB (vía [SnesDecoder.parsePalette]).
     *
     * Las entradas de BG/FG/Sprite ocupan 12 colores (cols 2-7 de DOS filas). Aquí
     * leemos solo los 6 primeros (bytes 0-11), que son la fila "de arriba"; la fila
     * hermana serían los 6 siguientes (bytes 12-23) de la misma entrada.
     */
    internal class SmwPaletteTables(private val rom: ByteArray, private val delta: Int) {
        /** Back area: 1 color en x=0..7. */
        fun backColor(x: Int): Int =
            SnesDecoder.parsePalette(rom, SMW_BACK_AREA_PC + delta + 2 * x, 1)[0]

        /** BG: 6 colores (fila de arriba de la entrada de 12) en x=0..7. */
        fun bgEntry(x: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_BG_PC + delta + 0x18 * x, 6)

        /** FG: 6 colores (fila de arriba de la entrada de 12) en x=0..7. */
        fun fgEntry(x: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_FG_PC + delta + 0x18 * x, 6)

        /** Sprite: 6 colores (fila de arriba de la entrada de 12) en x=0..7. */
        fun sprEntry(x: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_SPRITE_PC + delta + 0x18 * x, 6)

        /** Fijas: 6 colores de la fila [row] (4..D). */
        fun fixedRow(row: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_FIXED_PC + delta + 0x0C * (row - 4), 6)

        /** Player: 10 colores (cols 6-F) para p = 0..3 (Mario/Luigi/FireMario/FireLuigi). */
        fun player(p: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_PLAYER_PC + delta + 0x14 * p, 10)
    }

    /**
     * Fila canónica FG (3bpp, 8 colores):
     * `[backColor(backIndex), NEGRO, fg0..fg5]`.
     */
    internal fun row3bppFG(t: SmwPaletteTables, fgIndex: Int, backIndex: Int): IntArray {
        val fg = t.fgEntry(fgIndex)
        return intArrayOf(t.backColor(backIndex), SMW_BLACK, fg[0], fg[1], fg[2], fg[3], fg[4], fg[5])
    }

    /**
     * Fila canónica de sprite (3bpp, 8 colores):
     * `[TRANSPARENTE, NEGRO, spr0..spr5]`.
     */
    internal fun row3bppSprite(t: SmwPaletteTables, sprIndex: Int): IntArray {
        val s = t.sprEntry(sprIndex)
        return intArrayOf(SMW_TRANSPARENT, SMW_BLACK, s[0], s[1], s[2], s[3], s[4], s[5])
    }

    /**
     * Fila canónica de jugador (4bpp, 16 colores) para p = 0..3
     * (Mario/Luigi/FireMario/FireLuigi):
     * `[TRANSPARENTE, NEGRO, fix0..fix3, pl0..pl9]`, con `fix = fixedRow(8)` (los 4
     * primeros de los 6) y `pl = player(p)` (10 colores).
     */
    internal fun rowPlayer(t: SmwPaletteTables, p: Int): IntArray {
        val fix = t.fixedRow(8)
        val pl = t.player(p)
        return intArrayOf(
            SMW_TRANSPARENT, SMW_BLACK, fix[0], fix[1], fix[2], fix[3],
            pl[0], pl[1], pl[2], pl[3], pl[4], pl[5], pl[6], pl[7], pl[8], pl[9],
        )
    }

    /** Fila canónica de Mario (4bpp, 16 colores): jugador 0. */
    internal fun rowMario(t: SmwPaletteTables): IntArray = rowPlayer(t, 0)

    private fun lorom(lo: Int, hi: Int, bank: Int): Int {
        val addr = lo or (hi shl 8)
        return if (addr >= 0x8000) (bank and 0x7F) * 0x8000 + (addr - 0x8000) else -1
    }

    /**
     * Localiza las tres tablas de punteros de GFX de SMW (byte bajo/alto/banco).
     * Prueba primero las posiciones de la ROM estándar y, si no validan, hace una
     * búsqueda acotada anclada en la tabla de bancos (tira de bytes de banco
     * válidos y no decrecientes). Elige la alineación que MÁS ficheros descomprime.
     */
    private fun findSmwGfxTable(rom: ByteArray): Triple<Int, Int, Int>? {
        fun successes(bLo: Int, bHi: Int, bBank: Int): Int {
            if (bLo < 0 || bBank + 52 > rom.size) return 0
            var ok = 0
            for (i in 0 until 52) {
                val pc = lorom(byte(rom, bLo + i), byte(rom, bHi + i), byte(rom, bBank + i))
                if (pc < 0x40000 || pc >= rom.size) continue
                val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
                if (data.size >= 0x400) ok++
            }
            return ok
        }
        // 1) Posiciones de la ROM estándar (USA/EUR): validar antes de fiarse.
        val standard = Triple(0x398D, 0x39BF, 0x39F1)
        if (successes(standard.first, standard.second, standard.third) >= 30) return standard

        // 2) Búsqueda acotada: tira de bancos válidos no decreciente en la zona baja.
        var best: Triple<Int, Int, Int>? = null
        var bestOk = 29
        var i = 0x3000
        while (i < minOf(rom.size - 52, 0x5000)) {
            val b = byte(rom, i) and 0x7F
            if (b in 0x06..0x10) {
                // ¿empieza aquí una tira de bancos plausible?
                var j = i
                while (j < i + 60 && j < rom.size && (byte(rom, j) and 0x7F) in 0x06..0x10 &&
                    (byte(rom, j) and 0x7F) >= (byte(rom, j - 1) and 0x7F)) j++
                if (j - i >= 40) {
                    for (bBank in (i - 6)..(i + 6)) {
                        for (s in intArrayOf(0x32, 0x33, 0x34)) {
                            val ok = successes(bBank - 2 * s, bBank - s, bBank)
                            if (ok > bestOk) { bestOk = ok; best = Triple(bBank - 2 * s, bBank - s, bBank) }
                        }
                    }
                    i = j
                    continue
                }
            }
            i++
        }
        return best
    }

    private fun byte(rom: ByteArray, i: Int): Int = if (i in rom.indices) rom[i].toInt() and 0xFF else 0

    /**
     * Margen de aptitud por el que otro formato tiene que GANAR a 3bpp para que se
     * le crea. Los gráficos de SMW son 3bpp casi sin excepción; cuando el detector
     * elige 4bpp/2bpp por una diferencia mínima suele ser un error que sale como
     * "ruido arcoíris" (índices altos coloreados con basura). Exigiendo un margen
     * claro, esos empates técnicos vuelven a 3bpp —su formato real— y la hoja se ve.
     */
    private const val SMW_FORMAT_MARGIN = 0.06

    /**
     * Formato de un fichero de SMW con SESGO a 3bpp. Solo se acepta otro formato si
     * su aptitud supera a la de 3bpp por [SMW_FORMAT_MARGIN]; si no, 3bpp (el real).
     */
    internal fun smwFormat(data: ByteArray): SnesGraphicFormat {
        val guess = SnesGraphicsScanner.detectBestFormat(data, 0) ?: return SnesGraphicFormat.SNES_3BPP
        if (guess.format == SnesGraphicFormat.SNES_3BPP) return SnesGraphicFormat.SNES_3BPP
        val fit3 = SnesGraphicsScanner.formatFitness(data, 0, SnesGraphicFormat.SNES_3BPP)
        return if (guess.fitness - fit3 >= SMW_FORMAT_MARGIN) guess.format else SnesGraphicFormat.SNES_3BPP
    }

    /**
     * Índices de fichero GFX que son HOJAS DE SPRITE/PERSONAJE (fondo transparente,
     * figuras con sombreado) y no tiles de fondo. Curados mirando el propio juego:
     * al contrario que los tilesets FG/BG —que quedan bien con la paleta de terreno—
     * estas hojas piden una paleta de sprite viva. Para ellas elegimos, entre las 8
     * paletas de sprite reales, la de mejor coherencia con el dibujo (así Bowser sale
     * naranja, los enemigos con su tono, etc.), sin arriesgar los fondos.
     */
    private val SMW_SPRITE_GFX = setOf(18, 37, 38, 40)

    /**
     * Paleta curada para el fichero GFX número [gfxIndex]. La inmensa mayoría de los
     * ficheros de esta tabla son tiles de FG/BG y se ven con la paleta de terreno
     * clásica (FG0), que es el aspecto canónico de SMW; las pocas hojas de personaje
     * ([SMW_SPRITE_GFX]) reciben la mejor paleta de sprite para su dibujo; y los
     * ficheros realmente 4bpp toman la paleta de jugador que mejor encaje.
     */
    private fun smwPalette(
        gfxIndex: Int,
        data: ByteArray,
        fmt: SnesGraphicFormat,
        count: Int,
        fgRow: IntArray,
        spriteRows: List<IntArray>,
        playerRows: List<IntArray>,
    ): IntArray {
        if (fmt == SnesGraphicFormat.SNES_4BPP) {
            val stats = SnesPaletteMatcher.indexStats(data, 0, fmt, count)
            return playerRows.maxByOrNull { SnesPaletteMatcher.scorePalette(it, stats) }!!
        }
        if (gfxIndex in SMW_SPRITE_GFX) {
            val stats = SnesPaletteMatcher.indexStats(data, 0, fmt, count)
            return spriteRows.maxByOrNull { SnesPaletteMatcher.scorePalette(it, stats) }!!
        }
        return fgRow
    }

    private fun extractSmw(rom: ByteArray, header: SnesHeader): List<SnesAutoExtractor.Finding> {
        val bases = findSmwGfxTable(rom) ?: return emptyList()
        val (bLo, bHi, bBank) = bases
        // Paletas REALES del juego (no adivinadas): banco de filas canónicas.
        val tables = SmwPaletteTables(rom, smwHeaderDelta(header))
        val fgRow = row3bppFG(tables, SMW_DEFAULT_FG_INDEX, SMW_DEFAULT_BACK_INDEX)
        val spriteRows = (0 until 8).map { row3bppSprite(tables, it) }
        val playerRows = (0 until 4).map { rowPlayer(tables, it) }
        val out = ArrayList<SnesAutoExtractor.Finding>()
        var n = 0
        for (i in 0 until 52) {
            val pc = lorom(byte(rom, bLo + i), byte(rom, bHi + i), byte(rom, bBank + i))
            if (pc < 0x40000 || pc >= rom.size) continue
            val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
            if (data.size < 0x300) continue
            // Formato con sesgo a 3bpp (el real de SMW): evita el "ruido arcoíris" que
            // producían los ficheros 3bpp detectados como 4bpp/2bpp por un pelo.
            val fmt = smwFormat(data)
            val available = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
            if (available < 16) continue
            val count = minOf(available, 128)
            // Paleta REAL del juego según una tabla curada por fichero (ver smwPalette).
            val pal = smwPalette(i, data, fmt, count, fgRow, spriteRows, playerRows)
            val sheet = SnesAssetExtractor.extractTileSheet(data, 0, fmt, pal, count, 16)
            n++
            out.add(
                SnesAutoExtractor.Finding(
                    image = sheet.image,
                    label = "SMW $n",
                    offset = pc,
                    compressed = true,
                    format = fmt,
                    palette = pal,
                    tileCount = count,
                    columns = 16,
                    score = 1.0,
                )
            )
        }
        return out
    }
}
