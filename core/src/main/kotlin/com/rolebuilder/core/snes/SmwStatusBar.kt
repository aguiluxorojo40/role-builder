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
}
