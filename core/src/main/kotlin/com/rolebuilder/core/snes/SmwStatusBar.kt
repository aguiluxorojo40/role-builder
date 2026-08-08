package com.rolebuilder.core.snes

/**
 * El HUD (barra de estado) REAL de Super Mario World, portado de `InitializeStatusBarTilemap`
 * ($00:8CFF) y `UpdateStatusBarCounters` ($00:8E1A).
 *
 * Por qué existe: el HUD de la app estaba INVENTADO —emoji y texto de Compose (`★ 0`, `⏱ 394`)—
 * y no se parece al del juego, que es una franja de teselas de Layer 3 con su propia
 * tipografía. Esto es la parte de datos: la plantilla de teselas tal cual la sube el juego a
 * VRAM y en qué posición va cada contador. Con eso, dibujarlo es pegar teselas.
 *
 * El juego mantiene la barra en `misc_status_bar_tilemap[]`, un array de teselas donde cada
 * contador escribe SUS dígitos en posiciones fijas (ver [Field]). Los dígitos son directos: el
 * valor 0-9 ES el número de tesela, porque la fuente de Layer 3 tiene los dígitos en las
 * teselas 0x00-0x09. El "hueco" es [BLANK] (0xFC), que es la tesela en blanco de esa fuente.
 */
object SmwStatusBar {

    /**
     * Tesela EN BLANCO de la fuente de Layer 3 (`-4` en el desensamblado, o sea 0xFC). El juego
     * la usa para borrar los ceros a la izquierda: si las centenas del reloj son 0, escribe
     * blanco en vez de un '0'.
     */
    const val BLANK = 0xFC

    /** Fotogramas que dura una unidad del reloj: `misc_status_bar_tilemap[55] = 40`. */
    const val TIMER_FRAMES_PER_UNIT = 40

    /** Tope de puntuación del juego (0xF423F = 999990 en decimal SMW). */
    const val MAX_SCORE = 0xF423F

    /** Tope de vidas que el juego deja mostrar (`>= 98` se recorta a 98). */
    const val MAX_LIVES = 98

    /**
     * Dónde escribe cada contador dentro de `misc_status_bar_tilemap[]`. Son las posiciones
     * EXACTAS del juego; cambiarlas mueve el dato de sitio en la barra.
     */
    object Field {
        /** Monedas: 2 dígitos. [26] es la DECENA (en blanco si es 0) y [27] la unidad. */
        val COINS = intArrayOf(26, 27)

        /** Vidas: 2 dígitos de `player_current_life_count + 1` ([29] en blanco si es 0). */
        val LIVES = intArrayOf(29, 30)

        /** Reloj: centenas, decenas y unidades. */
        val TIME = intArrayOf(44, 45, 46)

        /** Puntuación: 6 dígitos (los ceros a la izquierda se borran con [BLANK]). */
        val SCORE = intArrayOf(48, 49, 50, 51, 52, 53)

        /**
         * Estrellas de bonus: 2 dígitos que NO son la fuente normal. Cada dígito se parte en
         * dos teselas (arriba en [10]/[11], abajo en [37]/[38]) vía
         * `kUpdateStatusBarCounters_BonusStarCounterNumberTiles`, porque se dibujan con el
         * marco de la estrella.
         */
        val BONUS_STARS_TOP = intArrayOf(10, 11)
        val BONUS_STARS_BOTTOM = intArrayOf(37, 38)

        /** Posición del contador de fotogramas del reloj (no se dibuja: es estado). */
        const val TIMER_TICK = 55
    }

    /**
     * Plantilla de la barra tal y como la inicializa el juego: pares (tesela, propiedades) de
     * `kStatusBarTilemap_SecondRow`, 118 bytes = 59 celdas. El juego sube los bytes PARES
     * (las teselas) y usa los IMPARES como atributos YXPPCCCT de cada celda.
     */
    val SECOND_ROW = intArrayOf(
        0x30, 0x28, 0x31, 0x28, 0x32, 0x28, 0x33, 0x28, 0x34, 0x28, 0xfc, 0x38, 0xfc, 0x3c,
        0xfc, 0x3c, 0xfc, 0x3c, 0xfc, 0x3c, 0xfc, 0x38, 0xfc, 0x38, 0x4a, 0x38, 0xfc, 0x38,
        0xfc, 0x38, 0x4a, 0x78, 0xfc, 0x38, 0x3d, 0x3c, 0x3e, 0x3c, 0x3f, 0x3c, 0xfc, 0x38,
        0xfc, 0x38, 0xfc, 0x38, 0x2e, 0x3c, 0x26, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0x00, 0x38,
        0x26, 0x38, 0xfc, 0x38, 0x00, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0x64, 0x28,
        0x26, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0x4a, 0x38, 0xfc, 0x38, 0xfc, 0x38,
        0x4a, 0x78, 0xfc, 0x38, 0xfe, 0x3c, 0xfe, 0x3c, 0x00, 0x3c, 0xfc, 0x38, 0xfc, 0x38,
        0xfc, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0xfc, 0x38, 0x00, 0x38, 0x3a, 0xb8,
        0x3b, 0xb8, 0x3b, 0xb8, 0x3a, 0xf8,
    )

    /** Fila SUPERIOR del marco (`kStatusBarTilemap_TopRow`), pares (tesela, atributos). */
    val TOP_ROW = intArrayOf(0x3A, 0x38, 0x3B, 0x38, 0x3B, 0x38, 0x3A, 0x78)

    /** Fila INFERIOR del marco (`kStatusBarTilemap_SomeRow`), pares (tesela, atributos). */
    val BOTTOM_ROW = intArrayOf(0x3A, 0xB8, 0x3B, 0xB8, 0x3B, 0xB8, 0x3A, 0xF8)

    /** Nº de celdas de [SECOND_ROW] (118 bytes / 2). */
    val CELL_COUNT: Int get() = SECOND_ROW.size / 2

    /** Nº de tesela de la celda [cell] de [SECOND_ROW]. */
    fun tileAt(cell: Int): Int = SECOND_ROW[cell * 2]

    /** Atributos YXPPCCCT de la celda [cell] de [SECOND_ROW]. */
    fun attrAt(cell: Int): Int = SECOND_ROW[cell * 2 + 1]

    /**
     * Escribe [value] en [positions] como dígitos decimales, de derecha a izquierda, borrando
     * con [BLANK] los ceros a la IZQUIERDA (que es lo que hace el juego: un reloj de 39 se ve
     * " 39", no "039"). Devuelve el mapa modificado.
     *
     * [keepLeadingZeros] deja los ceros (el reloj los conserva en las unidades y decenas: el
     * juego solo borra los dos primeros dígitos, y solo mientras valgan 0).
     */
    fun writeNumber(tilemap: IntArray, positions: IntArray, value: Int,
                    keepLeadingZeros: Boolean = false): IntArray {
        var v = value.coerceAtLeast(0)
        for (i in positions.indices.reversed()) {
            tilemap[positions[i]] = v % 10
            v /= 10
        }
        if (!keepLeadingZeros) {
            for (i in 0 until positions.size - 1) {
                if (tilemap[positions[i]] != 0) break
                tilemap[positions[i]] = BLANK
            }
        }
        return tilemap
    }

    /**
     * Ficheros GFX de la FUENTE de Layer 3, o sea la tipografía del HUD:
     * `UploadGraphicsFiles_Layer3` ($00:A993) descomprime los ficheros **40..43 (0x28-0x2B)**
     * y los sube a VRAM 0x4000 + i·0x400. Layer 3 es 2bpp (16 bytes por tesela = 128 teselas
     * por fichero), así que las teselas 0x00-0x7F salen del 0x28 y las 0x80-0xFF del 0x29.
     */
    val LAYER3_GFX_FILES = intArrayOf(0x28, 0x29, 0x2A, 0x2B)

    /** Teselas de 8×8 que entran en un fichero GFX de Layer 3 (0x800 bytes / 16). */
    const val TILES_PER_GFX_FILE = 128

    /**
     * Colores por paleta en Layer 3: es 2bpp, así que cada paleta son **4** colores. El byte
     * de atributos de cada celda lleva la paleta en los bits 2-4 (`vhoppp cc`).
     */
    const val COLORS_PER_PALETTE = 4

    /** Paleta (0-7) que pide el byte de atributos [attr] de una celda. */
    fun paletteOf(attr: Int): Int = (attr shr 2) and 0x07

    /** ¿La celda [attr] va volteada en horizontal / vertical? */
    fun flipXOf(attr: Int): Boolean = (attr and 0x40) != 0
    fun flipYOf(attr: Int): Boolean = (attr and 0x80) != 0

    /**
     * Construye el estado de la barra para los contadores dados, igual que
     * `UpdateStatusBarCounters`: reloj de 3 dígitos (con los ceros de la izquierda en blanco),
     * monedas de 2, vidas de 2 sobre `vidas + 1`, y puntuación de 6.
     *
     * Devuelve el array de teselas indexable por [Field]; las posiciones que no toca ningún
     * contador quedan a [BLANK].
     */
    fun buildTilemap(time: Int, coins: Int, lives: Int, score: Int): IntArray {
        val map = IntArray(64) { BLANK }
        writeNumber(map, Field.TIME, time.coerceIn(0, 999))
        writeNumber(map, Field.COINS, coins.coerceIn(0, 99))
        // El juego muestra vidas+1 (con 0 vidas restantes pone "1"), y recorta a 98.
        writeNumber(map, Field.LIVES, (lives.coerceIn(0, MAX_LIVES) + 1).coerceAtMost(99))
        writeNumber(map, Field.SCORE, score.coerceIn(0, MAX_SCORE))
        map[Field.TIMER_TICK] = TIMER_FRAMES_PER_UNIT
        return map
    }

    /**
     * Dibuja la barra de estado REAL desde la ROM: teselas de la fuente de Layer 3 con la
     * paleta del nivel. Devuelve una imagen de `CELL_COUNT × 8` px (la fila de contadores),
     * o null si faltan datos.
     *
     * [tilemap] es el estado de los contadores ([buildTilemap]); las celdas cuyo índice NO
     * pisa ningún contador se pintan con la plantilla fija [SECOND_ROW].
     */
    fun render(rom: ByteArray, header: SnesHeader, level: Int, tilemap: IntArray): ArgbImage? {
        val font = fontTiles(rom) ?: return null
        val delta = header.headerOffset - 0x7FC0
        val cgram = SnesGameRecipes.assembleSmwCgram(rom, delta, level)
        val img = ArgbImage(SCREEN_COLS * 8, ROWS * 8)
        var pintadoAlgo = false
        // Las dos filas del juego, cada una con su celda de arranque en la pantalla.
        for ((fila, tramo) in ROW_SPANS.withIndex()) {
            val (primeraCelda, celdas, colInicial) = tramo
            // Se recorta de antemano lo que no cabe (celdas fuera de la plantilla o de la
            // pantalla), para que el bucle solo pinte.
            val ultima = minOf(celdas, CELL_COUNT - primeraCelda, SCREEN_COLS - colInicial)
            for (i in 0 until ultima) {
                val cell = primeraCelda + i
                // El contador manda sobre la plantilla: si algún campo escribe en esta celda,
                // se pinta su dígito; si no, la tesela fija de la barra.
                val tile = tilemap.getOrNull(cell)?.takeIf { it != BLANK || cell in contadorCells }
                    ?: tileAt(cell)
                val ox = (colInicial + i) * 8
                if (paintTile(font, cgram, tile, attrAt(cell), img, ox, fila * 8)) pintadoAlgo = true
            }
        }
        return if (pintadoAlgo) img else null
    }

    /** Ancho de la pantalla de SNES en teselas de 8 px (256 px / 8). */
    const val SCREEN_COLS = 32

    /** Filas que ocupa la barra de contadores. */
    const val ROWS = 2

    /**
     * Cómo reparte el juego las 59 celdas en la PANTALLA (`InitializeStatusBarTilemap`):
     * `SmwCopyToVram(0x5042, …, 0x38)` son 28 celdas y `SmwCopyToVram(0x5063, …+0x38, 0x36)`
     * otras 27, en dos filas consecutivas del tilemap (paso de 32) que arrancan en las
     * columnas 2 y 3. Cada tramo es (primera celda, nº de celdas, columna de pantalla).
     */
    val ROW_SPANS = listOf(
        Triple(0, 28, 2),
        Triple(28, 27, 3),
    )

    /** Celdas que algún contador escribe (para que [render] les haga caso aunque valgan BLANK). */
    private val contadorCells: Set<Int> by lazy {
        (Field.COINS + Field.LIVES + Field.TIME + Field.SCORE +
            Field.BONUS_STARS_TOP + Field.BONUS_STARS_BOTTOM).toSet()
    }

    private fun IntArray.getOrNull(i: Int): Int? = if (i in indices) this[i] else null

    /**
     * La fuente del HUD como bytes crudos: los ficheros [LAYER3_GFX_FILES] descomprimidos y
     * concatenados, que es como quedan en VRAM (0x28 → teselas 0x00-0x7F, 0x29 → 0x80-0xFF…).
     */
    fun fontTiles(rom: ByteArray): ByteArray? {
        val partes = LAYER3_GFX_FILES.map { SnesGameRecipes.smwGfxFileDataPublic(rom, it) ?: return null }
        val total = partes.sumOf { it.size }
        val out = ByteArray(total)
        var p = 0
        for (parte in partes) { parte.copyInto(out, p); p += parte.size }
        return out
    }

    /**
     * La barra PREPARADA: la fuente descomprimida y la CGRAM del nivel, ya listas. Existe
     * porque [render] descomprime cuatro ficheros GFX cada vez, y el HUD se redibuja cada vez
     * que cambia un contador: preparándolo una vez, repintar es solo pegar teselas.
     */
    class Prepared internal constructor(private val font: ByteArray, private val cgram: IntArray) {
        /** Dibuja la barra para el estado [tilemap] (ver [buildTilemap]). */
        fun render(tilemap: IntArray): ArgbImage {
            val img = ArgbImage(SCREEN_COLS * 8, ROWS * 8)
            for ((fila, tramo) in ROW_SPANS.withIndex()) {
                val (primeraCelda, celdas, colInicial) = tramo
                val ultima = minOf(celdas, CELL_COUNT - primeraCelda, SCREEN_COLS - colInicial)
                for (i in 0 until ultima) {
                    val cell = primeraCelda + i
                    val tile = tilemap.getOrNull(cell)?.takeIf { it != BLANK || cell in contadorCells }
                        ?: tileAt(cell)
                    paintTile(font, cgram, tile, attrAt(cell), img, (colInicial + i) * 8, fila * 8)
                }
            }
            return img
        }

        /** Atajo: construye el estado y lo dibuja de una vez. */
        fun render(time: Int, coins: Int, lives: Int, score: Int): ArgbImage =
            render(buildTilemap(time, coins, lives, score))
    }

    /**
     * Prepara la barra para [level]: descomprime la fuente y arma la paleta UNA vez. Devuelve
     * null si la ROM no trae los GFX de Layer 3 (no parece SMW).
     */
    fun prepare(rom: ByteArray, header: SnesHeader, level: Int): Prepared? {
        val font = fontTiles(rom) ?: return null
        val cgram = SnesGameRecipes.assembleSmwCgram(rom, header.headerOffset - 0x7FC0, level)
        return Prepared(font, cgram)
    }

    /**
     * Hoja con TODA la fuente del HUD (16×16 teselas de 8×8 = 256), pintada con la [palette]
     * indicada y la CGRAM del [level]. El nº de tesela de cada celda es `fila*16 + columna`,
     * así que sirve para localizar a ojo qué tesela es cada dígito, letra o pieza del marco.
     */
    fun fontSheet(rom: ByteArray, header: SnesHeader, level: Int, palette: Int = 6): ArgbImage? {
        val font = fontTiles(rom) ?: return null
        val delta = header.headerOffset - 0x7FC0
        val cgram = SnesGameRecipes.assembleSmwCgram(rom, delta, level)
        val cols = 16
        val img = ArgbImage(cols * 8, cols * 8)
        val palBase = palette * COLORS_PER_PALETTE
        val bpt = SnesGraphicFormat.SNES_2BPP.bytesPerTile
        val teselas = minOf(cols * cols, font.size / bpt)
        for (t in 0 until teselas) {
            val dec = SnesDecoder.decodeTile(font, t * bpt, SnesGraphicFormat.SNES_2BPP)
            for (y in 0 until 8) for (x in 0 until 8) {
                val ci = dec.pixelIndices[y * 8 + x]
                if (ci != 0) {
                    img.set((t % cols) * 8 + x, (t / cols) * 8 + y, cgram.getOrElse(palBase + ci) { 0 })
                }
            }
        }
        return img
    }

    /** Pinta la tesela 2bpp [tile] con la paleta de [attr] en [img] a partir de la columna [ox]. */
    private fun paintTile(font: ByteArray, cgram: IntArray, tile: Int, attr: Int,
                          img: ArgbImage, ox: Int, oy: Int = 0): Boolean {
        val fmt = SnesGraphicFormat.SNES_2BPP
        val off = tile * fmt.bytesPerTile
        if (off + fmt.bytesPerTile > font.size) return false
        val dec = SnesDecoder.decodeTile(font, off, fmt)
        val palBase = paletteOf(attr) * COLORS_PER_PALETTE
        val fx = flipXOf(attr)
        val fy = flipYOf(attr)
        var algo = false
        for (y in 0 until 8) for (x in 0 until 8) {
            val sx = if (fx) 7 - x else x
            val sy = if (fy) 7 - y else y
            val ci = dec.pixelIndices[sy * 8 + sx]
            // Índice 0 = transparente, como en el juego; y un color sin alfa tampoco se pinta.
            val argb = if (ci == 0) 0 else cgram.getOrElse(palBase + ci) { 0 }
            if (argb ushr 24 != 0) {
                img.set(ox + x, oy + y, argb)
                algo = true
            }
        }
        return algo
    }
}
