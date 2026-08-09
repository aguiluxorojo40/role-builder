package com.rolebuilder.core.snes

/**
 * Parser de OBJETOS de Layer 1 de Super Mario World: reconstruye el tilemap Map16
 * de un nivel (el mismo buffer que el juego monta en $7EC800/$7FC800) leyendo la
 * lista de objetos del nivel desde la ROM, sin emulador.
 *
 * Es un port fiel de la reimplementación en C del juego (snesrev/smw, smw_05.c y
 * smw_0d.c, rutinas 0x05:83AC/85FF y banco $0D), que a su vez es ingeniería
 * inversa 1:1 del código original. Cada rutina de objeto reproduce la aritmética
 * de punteros del juego (pantallas de 0x1B0 bytes, posición fila*16+columna con
 * página alta en el bit 4 del primer byte del objeto).
 *
 * Cobertura: los objetos estándar y extendidos más comunes (suelo, cuestas,
 * tuberías, monedas, bloques, arbustos, setos diagonales…). Los no portados se
 * saltan y se CUENTAN ([SmwLevelTilemap.unknownObjects]), para poder medir
 * cobertura y decidir honestamente si una escena es fiable.
 *
 * Limitación actual: solo niveles HORIZONTALES (los verticales usan otra
 * disposición de pantallas); para esos parse() devuelve null.
 */
internal object SmwLayer1 {

    /**
     * Resultado: buffer Map16 del nivel. La disposición NO es única, y por eso hace falta
     * [vertical]:
     *
     *  - HORIZONTAL: las pantallas se ponen una a la DERECHA de otra, cada una de
     *    0x1B0 bytes = 27 filas × 16 columnas (las 16 primeras filas en la página 0 y las
     *    11 restantes a partir del byte 256). El nivel crece a lo ancho.
     *  - VERTICAL: las pantallas se apilan hacia ABAJO, cada una de 0x200 bytes = 32 filas
     *    × 16 columnas (16 filas en la página 0 y otras 16 a partir del byte 256). El
     *    nivel mide siempre 16 columnas y crece a lo alto.
     *
     * [block] entiende las dos y siempre se le pasa (x, y) en casillas.
     */
    internal class SmwLevelTilemap(
        val lo: ByteArray,
        val hi: ByteArray,
        val screens: Int,
        val tileset: Int,
        val mode: Int,
        val totalObjects: Int,
        val unknownObjects: Int,
        /** id de objeto sin rutina portada → nº de apariciones ("std:NN"/"ext:NN"). */
        val unknownIds: Map<String, Int> = emptyMap(),
        /** true si el nivel se recorre hacia ABAJO (pantallas apiladas, 16 columnas). */
        val vertical: Boolean = false,
        /** Base de cada pantalla dentro de [lo]/[hi] (`kLevelDataLayout_*`). */
        val screenBase: IntArray = IntArray(32) { it * 0x1B0 },
    ) {
        /** Ancho del nivel en casillas: 16 fijas si es vertical, 16 por pantalla si no. */
        val cols: Int get() = if (vertical) 16 else screens * 16

        /** Alto del nivel en casillas: 32 por pantalla si es vertical, 27 fijas si no. */
        val rows: Int get() = if (vertical) screens * 32 else 27

        /** Filas por pantalla: 32 en vertical (0x200 bytes), 27 en horizontal (0x1B0). */
        val rowsPerScreen: Int get() = if (vertical) 32 else 27

        /**
         * RECORTE al contenido real: los niveles no llenan sus 32 pantallas. Devuelve
         * (ancho, alto) en casillas, ya recortado por el eje por el que CRECE el nivel —a
         * lo ancho si es horizontal, a lo alto si es vertical— y con un margen de una
         * casilla. Devuelve null si no hay contenido suficiente.
         *
         * Que el recorte siga el eje bueno no es un detalle: recortar un nivel vertical
         * por columnas lo dejaría siempre en 16 y con las 32 pantallas de alto vacías
         * arrastradas detrás.
         */
        fun trimmedSize(maxCols: Int = Int.MAX_VALUE, maxRows: Int = Int.MAX_VALUE): Pair<Int, Int>? {
            var ultimo = -1
            if (vertical) {
                for (y in 0 until rows) for (x in 0 until 16) {
                    val b = block(x, y)
                    if (b > 0 && b != 0x25) { ultimo = maxOf(ultimo, y); break }
                }
                if (ultimo < 3) return null
                return 16 to minOf(ultimo + 2, rows, maxRows)
            }
            for (x in 0 until cols) for (y in 0 until 27) {
                val b = block(x, y)
                if (b > 0 && b != 0x25) { ultimo = maxOf(ultimo, x); break }
            }
            if (ultimo < 3) return null
            return minOf(ultimo + 2, cols, maxCols) to 27
        }

        /** Bloque Map16 en la casilla (x, y), o -1 fuera del nivel. */
        fun block(x: Int, y: Int): Int {
            if (x < 0 || y < 0) return -1
            val screen: Int
            val col: Int
            val row: Int
            if (vertical) {
                if (x >= 16) return -1
                screen = y / 32; col = x; row = y % 32
            } else {
                if (y >= 27) return -1
                screen = x / 16; col = x % 16; row = y
            }
            if (screen >= screenBase.size) return -1
            // La página 1 de la pantalla empieza en el byte 256, tanto en horizontal
            // (filas 16..26) como en vertical (filas 16..31).
            val idx = screenBase[screen] + (if (row >= 16) 256 + (row - 16) * 16 else row * 16) + col
            if (idx !in lo.indices) return -1
            return (lo[idx].toInt() and 0xFF) or ((hi[idx].toInt() and 0xFF) shl 8)
        }
    }

    /**
     * Tamaño del buffer: $7EC800..$7EFFFF, o sea 0x3800 bytes. No son "32 × 0x1B0"
     * (eso da 0x3600 y se queda corto): las disposiciones VERTICALES llegan hasta
     * $7EFE00 + 0x200, que es justo el final de la RAM.
     */
    private const val BUF = 0x3800

    /** Modos de nivel sin objetos de Layer 1 (salas de jefe / especiales). */
    private val MODES_NO_LAYER1 = intArrayOf(9, 11, 16)

    // ------------------------- DISPOSICIÓN de las pantallas -------------------------
    // `kLevelDataLayout_*` ($05), como offsets desde $7EC800. Es la tabla que dice dónde
    // empieza cada pantalla dentro del buffer Map16, y es LO QUE CAMBIA entre un nivel
    // horizontal y uno vertical: no es una fórmula distinta, es otra tabla.

    /** Horizontal: 32 pantallas de 0x1B0 (27 filas × 16 columnas), una a la derecha de otra. */
    private val LAYOUT_STD_HORIZ = IntArray(32) { it * 0x1B0 }

    /** Vertical: 28 pantallas de 0x200 (32 filas × 16 columnas), apiladas hacia abajo. */
    private val LAYOUT_STD_VERT = IntArray(28) { it * 0x200 }

    /** Layer 1 vertical y Layer 2 horizontal: 14 pantallas de 0x200 y luego de 0x1B0. */
    private val LAYOUT_VERT_L1 = IntArray(30) {
        if (it < 14) it * 0x200 else 0x1B00 + (it - 14) * 0x1B0
    }

    /** Layer 1 horizontal y Layer 2 vertical: 16 de 0x1B0 y luego de 0x200. */
    private val LAYOUT_HORIZ_L1 = IntArray(30) {
        if (it < 16) it * 0x1B0 else 0x1C00 + (it - 16) * 0x200
    }

    /**
     * `kLevelDataLayoutTables_Layer1LoPtrs` ($05): modo de nivel → disposición de Layer 1.
     * null es "este modo no tiene Layer 1" (el 0 de la tabla del juego), que es de donde
     * salen de verdad los modos sin geometría, sin necesidad de listarlos a mano.
     */
    /**
     * `kLevelDataLayoutTables_Layer2LoPtrs` ($05): modo → disposición de **Layer 2**.
     *
     * Layer 1 y Layer 2 comparten el MISMO buffer de $7EC800: cada modo reparte las
     * pantallas entre los dos. Por eso las entradas de Layer 2 son las mismas tablas pero
     * empezadas más adelante (`+16` en las horizontales, `+14` en las verticales): ahí es
     * donde acaba lo de Layer 1 y empieza lo de Layer 2.
     */
    private val LAYER2_LAYOUT: Array<IntArray?> = arrayOf(
        LAYOUT_STD_HORIZ.drop16(), LAYOUT_STD_HORIZ.drop16(), LAYOUT_STD_HORIZ.drop16(),
        LAYOUT_VERT_L1.dropN(14), LAYOUT_VERT_L1.dropN(14),                     // 03-04
        LAYOUT_HORIZ_L1.drop16(), LAYOUT_HORIZ_L1.drop16(),                     // 05-06
        LAYOUT_STD_VERT.dropN(14), LAYOUT_STD_VERT.dropN(14),                   // 07-08
        null, LAYOUT_STD_VERT.dropN(14), null,                                  // 09-0B
        LAYOUT_STD_HORIZ.drop16(), LAYOUT_STD_VERT.dropN(14),                   // 0C-0D
        LAYOUT_STD_HORIZ.drop16(), LAYOUT_STD_HORIZ.drop16(),                   // 0E-0F
        null, LAYOUT_STD_HORIZ.drop16(), null, null, null, null, null, null,    // 10-17
        null, null, null, null, null, null,                                     // 18-1D
        LAYOUT_STD_HORIZ.drop16(), LAYOUT_STD_HORIZ.drop16(),                   // 1E-1F
    )

    private fun IntArray.drop16(): IntArray = dropN(16)
    private fun IntArray.dropN(n: Int): IntArray =
        if (size <= n) IntArray(1) else IntArray(size - n) { this[n + it] }

    /**
     * Modos que NO procesan objetos de Layer 2: `BeginLoadingLevelData` ($05:83AC) corta
     * el bucle después de la capa 1 para estos. El 10 (0x0A) está aquí, y por eso los
     * niveles verticales de ese modo llevan fondo de IMAGEN y no de objetos.
     */
    private val MODES_NO_LAYER2 = intArrayOf(0, 10, 12, 13, 14, 17, 30)

    private val LAYER1_LAYOUT: Array<IntArray?> = arrayOf(
        LAYOUT_STD_HORIZ, LAYOUT_STD_HORIZ, LAYOUT_STD_HORIZ, LAYOUT_VERT_L1,   // 00-03
        LAYOUT_VERT_L1, LAYOUT_HORIZ_L1, LAYOUT_HORIZ_L1, LAYOUT_STD_VERT,      // 04-07
        LAYOUT_STD_VERT, null, LAYOUT_STD_VERT, null,                           // 08-0B
        LAYOUT_STD_HORIZ, LAYOUT_STD_VERT, LAYOUT_STD_HORIZ, LAYOUT_STD_HORIZ,  // 0C-0F
        null, LAYOUT_STD_HORIZ, null, null, null, null, null, null,             // 10-17
        null, null, null, null, null, null, LAYOUT_STD_HORIZ, LAYOUT_STD_HORIZ, // 18-1F
    )

    /**
     * `misc_level_layout_flags` (VerticalTable de LoadLevelHeader), por modo:
     * **bit 0 = Layer 1 vertical**, **bit 1 = Layer 2 vertical**. Las dos capas se
     * recorren por separado, y hay modos donde no coinciden: el 5 tiene el Layer 1
     * horizontal y el Layer 2 vertical, y el 0x0A justo al revés.
     */
    private val VERTICAL_TABLE = intArrayOf(
        0x00, 0x00, 0x80, 0x01, 0x81, 0x02, 0x82, 0x03, 0x83, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80,
    )

    /** Parsea el LAYER 1 del nivel [level]; null si no hay datos o el flujo está corrupto. */
    fun parse(rom: ByteArray, delta: Int, level: Int): SmwLevelTilemap? {
        val p = SnesGameRecipes.smwLayer1DataPc(rom, delta, level) ?: return null
        return P(rom, p).run()
    }

    /**
     * Parsea el LAYER 2 DE OBJETOS del nivel [level]: los 26 niveles cuyo fondo no es una
     * imagen comprimida sino geometría de verdad, hecha con los mismos objetos que el
     * primer plano.
     *
     * Se apoya en tres cosas de `BeginLoadingLevelData` ($05:83AC):
     *  - el flujo de Layer 2 empieza en `ptr_layer2_data + 5`, o sea que también lleva
     *    cabecera de 5 bytes, pero **no se lee**: el modo y el tileset son los de Layer 1;
     *  - se usa la tabla de disposición de Layer 2, que reparte con Layer 1 el mismo
     *    buffer de $7EC800;
     *  - hay modos que ni siquiera llegan a procesar la capa 2 ([MODES_NO_LAYER2]).
     *
     * Devuelve null si el nivel no tiene Layer 2 de objetos (porque lleva imagen, porque
     * su modo no procesa capa 2, o porque no hay datos).
     */
    fun parseLayer2(rom: ByteArray, delta: Int, level: Int): SmwLevelTilemap? {
        val p = SnesGameRecipes.smwLayer2ObjectDataPc(rom, delta, level) ?: return null
        // El modo y el tileset salen de la cabecera de LAYER 1: la de Layer 2 se salta.
        val l1 = SnesGameRecipes.smwLayer1DataPc(rom, delta, level) ?: return null
        val b1 = if (l1 + 1 < rom.size) rom[l1 + 1].toInt() and 0xFF else return null
        val mode = b1 and 0x1F
        if (mode in MODES_NO_LAYER2) return null
        return P(rom, p, layer = 1, headerPc = l1).run()
    }

    // =============================== máquina de estado ===============================

    /**
     * [layer] 0 = primer plano, 1 = fondo de objetos. Cambia la tabla de disposición y de
     * qué bit sale "es vertical", nada más: las rutinas de objeto son las mismas.
     * [headerPc] es de dónde leer la cabecera de 5 bytes (para Layer 2, la de Layer 1).
     */
    private class P(val rom: ByteArray, dataStart: Int, val layer: Int = 0, headerPc: Int = dataStart) {
        val lo = ByteArray(BUF) { 0x25 } // aire
        val hi = ByteArray(BUF)          // página 0

        var data = dataStart // índice PC del flujo de datos del nivel (cabecera incluida)
        var ptr = 0          // ptr_lo_map16_data - $C800
        var ptrBak = 0
        var pos = 0          // blocks_sub_scr_pos (uint8)
        var screen = 0       // blocks_screen_to_place_current_object
        var nextScreen = 0   // blocks_screen_to_place_next_object
        var objNum = 0; var size = 0; var r10 = 0; var r11 = 0
        var screensInLevel = 1; var mode = 0; var tileset = 0
        var total = 0; var unknown = 0

        /** Nivel VERTICAL: cambia la tabla de pantallas y el orden de los nibbles. */
        var vertical = false

        /** Base de cada pantalla (`kLevelDataLayout_*`), ya elegida según el modo. */
        var layout: IntArray = LAYOUT_STD_HORIZ

        /** PC de la cabecera de 5 bytes (para Layer 2 es la de Layer 1, ver [parseLayer2]). */
        private val cabecera = headerPc

        fun rd(i: Int): Int = if (i in rom.indices) rom[i].toInt() and 0xFF else 0xFF

        fun run(): SmwLevelTilemap? {
            // ---- LoadLevelHeader (solo los campos que afectan a la geometría) ----
            val b0 = rd(cabecera); val b1 = rd(cabecera + 1); val b4 = rd(cabecera + 4)
            screensInLevel = (b0 and 0x1F) + 1
            mode = b1 and 0x1F
            tileset = b4 and 0x0F
            // bit 0 para Layer 1, bit 1 para Layer 2: el juego hace `flags >> 1` cuando
            // está procesando la capa 2. Hay modos donde las dos capas no coinciden.
            vertical = (VERTICAL_TABLE[mode] shr layer) and 1 != 0
            // La disposición sale de kLevelDataLayoutTables_LayerNLoPtrs. Un modo sin
            // entrada (el 0 de la tabla) es un modo SIN esa capa: eso los detecta solos,
            // sin listarlos a mano.
            val tabla = if (layer == 0) LAYER1_LAYOUT else LAYER2_LAYOUT
            layout = tabla.getOrNull(mode) ?: return done()
            if (layer == 0 && mode in MODES_NO_LAYER1) return done()
            data += 5
            // ---- LoadLevelDataObject ----
            if (rd(data) == 0xFF) return done()
            var guard = 0
            do {
                if (++guard > 2000 || data + 3 > rom.size) return null // flujo corrupto
                var h0 = rd(data); var h1 = rd(data + 1); size = rd(data + 2); data += 3
                objNum = (h1 shr 4) or ((h0 and 0x60) shr 1)
                // En los niveles VERTICALES los nibbles bajos de los dos bytes van
                // INTERCAMBIADOS respecto al horizontal: el juego los cruza aquí para
                // poder reutilizar tal cual todas las rutinas de objeto. Se salta el
                // cruce cuando (objNum:size) como pareja de 16 bits vale menos de 2, que
                // son la salida y el salto de pantalla —esos llevan el dato en crudo—.
                if (vertical && ((objNum shl 8) or size) >= 2) {
                    val bajo0 = h0 and 0x0F
                    h0 = (h1 and 0x0F) or (h0 and 0xF0)
                    h1 = bajo0 or (h1 and 0xF0)
                }
                pos = ((h0 and 0x0F) shl 4) or (h1 and 0x0F)
                if (h0 and 0x80 != 0) screen++
                nextScreen = screen
                ptr = layout.getOrElse(screen) { layout.last() }
                if (h0 and 0x10 != 0) ptr += 256
                r10 = h0; r11 = h1
                total++
                if (objNum != 0) std(objNum) else ext(size)
            } while (rd(data) != 0xFF)
            return done()
        }

        val unknownIds = HashMap<String, Int>()

        fun unkStd(id: Int) { unknown++; val k = "std:%02X".format(id); unknownIds[k] = (unknownIds[k] ?: 0) + 1 }

        fun unkExt(id: Int) { unknown++; val k = "ext:%02X".format(id); unknownIds[k] = (unknownIds[k] ?: 0) + 1 }

        fun done() = SmwLevelTilemap(
            lo, hi, screensInLevel, tileset, mode, total, unknown, unknownIds,
            vertical = vertical, screenBase = layout,
        )

        // ------------------------------ primitivas ------------------------------
        // Réplicas exactas de las rutinas de $0D:A6B1..A9EF (ver smw_0d.c).

        fun setHi01(j: Int) { val i = ptr + j; if (i in hi.indices) hi[i] = 1 }
        fun setHi00(j: Int) { val i = ptr + j; if (i in hi.indices) hi[i] = 0 }
        fun setLo(j: Int, a: Int) { val i = ptr + j; if (i in lo.indices) lo[i] = a.toByte() }
        fun rdLo(j: Int): Int { val i = ptr + j; return if (i in lo.indices) lo[i].toInt() and 0xFF else 0x25 }

        fun preserve() { ptrBak = ptr }
        fun restore() { ptr = ptrBak; nextScreen = screen }
        fun backOneScreen() { ptr -= 0x1B0; ptrBak = ptr; nextScreen-- }
        fun forwardOneScreen() { ptr += 0x1B0; ptrBak = ptr; nextScreen++ }

        /** Escribe [a] en [j] y avanza una columna (cruzando de pantalla si toca). */
        fun horiz(j: Int, a: Int): Int { setLo(j, a); return horizNext(j) }
        fun horizNext(j: Int): Int {
            var r = (j + 1) and 0xFF
            if (r and 0xF == 0) { ptr += 0x1B0; nextScreen++; r = pos and 0xF0 }
            return r
        }

        /** Baja una fila (cruzando de página si toca); actualiza y devuelve pos. */
        fun vert(): Int {
            if (pos + 16 >= 256) pageCross(1)
            pos = (pos + 16) and 0xFF
            return pos
        }

        fun pageCross(cr: Int) { ptr += cr shl 8; ptrBak = ptr }

        /**
         * Bajar una fila en un nivel VERTICAL
         * (`HandleVerticalSubScreenCrossingForCurrentObject_VerticalLevel`, $0D:DA82A).
         * Se diferencia de [vert] en dos cosas: al pasarse de 255 suma 0x200 —una pantalla
         * vertical entera, no media— y NO toca el backup del puntero.
         *
         * Ojo: esto lo usan SOLO los tres objetos de cuesta de nivel vertical. El resto de
         * objetos siguen llamando a la variante horizontal aunque el nivel sea vertical,
         * porque el juego no los cambia. Aquí se respeta.
         */
        fun vertVertical(): Int {
            val t = pos + 16
            pos = t and 0xFF
            if (t >= 256) ptr += 0x200
            return pos
        }

        /**
         * Cruce de página CRUDO: el `ptr_lo_map16_data += 256` que hacen a pelo las
         * cuestas muy inclinadas mientras bajan por una columna. A diferencia de
         * [pageCross] —que es `HandleVerticalSubScreenCrossing…_Entry2` y SÍ actualiza el
         * backup— este no lo toca, porque la columna se recorre entre preserve/restore y
         * el restore tiene que devolver el puntero al arranque de la columna. Confundir
         * los dos corrompe el tilemap de forma silenciosa.
         */
        fun pageCrossRaw() { ptr += 0x100 }

        fun goDownLeft(): Int {
            var r = pos + 15
            if (r >= 256) pageCross(1)
            r = r and 0xFF
            if (r and 0xF == 15) {
                if (r + 16 >= 256) pageCross(1)
                r = (r + 16) and 0xFF
                backOneScreen()
            }
            pos = r
            return r
        }

        fun goDownRight(): Int {
            var v0 = pos + 17
            if (v0 >= 256) pageCross(1)
            v0 = v0 and 0xFF
            if ((v0 and 0xF) - 1 < 0) {
                if (v0 < 0x10) pageCross(0)
                v0 = (v0 - 16) and 0xFF
                forwardOneScreen()
            }
            pos = v0
            return v0
        }

        // int8: (int8)(x - n) >= 0
        fun ge8(x: Int, n: Int) = ((x - n) and 0x80) == 0

        // Reinterpreta un uint8 como int8 con signo (contadores de cuesta invertida).
        fun s8(x: Int) = x.toByte().toInt()

        // ------------------------------ dispatch ------------------------------

        fun std(obj: Int) {
            // Los objetos ≥0x30 son ESPECÍFICOS del tileset
            // (kProcessStandardAndTilesetSpecificObjects_TilesetPtrs, $0D). Portados:
            // PRADERA completa (tilesets 0/7/0xC), y de CASTILLO (1) y CASA FANTASMA
            // (4/5/0xD) los objetos que usan los niveles reales; cuerda y subterráneo
            // siguen sin portar.
            // El umbral es 0x2E porque el 0x2E ya difiere por tileset (en casa fantasma
            // son PINCHOS; en el resto, una rutina sin usar que no portamos).
            if (obj >= 0x2E && tileset != 0 && tileset != 7 && tileset != 0xC) {
                when (tileset) {
                    1 -> when (obj) {
                        0x34 -> castleVerticalDoubleEndedPipe()
                        0x35 -> castleRockWallBackground()
                        0x36 -> castleLargeSpikedPillar()
                        0x37 -> ropeHorizontalLineGuide()
                        0x38 -> ropeVerticalLineGuide()
                        0x39 -> switchBlocks(0)          // azules
                        0x3A -> switchBlocks(1)          // rojos
                        0x3C -> castleStoneBlock()
                        0x3D -> castleEscalator()
                        0x3E -> castleSpikeLineH()
                        0x3F -> castleSpikeLineV()
                        else -> unkStd(obj)
                    }
                    2, 6, 8 -> when (obj) {
                        0x32 -> ropeLogBridge()
                        // El 0x33 de CUERDA es `RopeObj33_BlueSwitchBlocks`, que no es
                        // una rutina propia: llama a `GrassObj32_BlueSwitchBlocks_0DB920(0)`,
                        // o sea exactamente los bloques ! azules de siempre.
                        0x33 -> switchBlocks(0)          // azules
                        0x34 -> switchBlocks(1)          // rojos
                        0x35 -> ropeColumnWithPlant()
                        0x37 -> castleEscalator()
                        0x38 -> ropeHorizontalLineGuide()
                        0x39 -> ropeVerticalLineGuide()
                        0x3A -> ropeSlopedLineGuide()
                        0x3C -> ropeMushroomTop()
                        0x3D -> ropeMushroomColumn()
                        else -> unkStd(obj)
                    }
                    3, 9, 0xA, 0xB, 0xE -> when (obj) {
                        0x34 -> switchBlocks(0)          // azules
                        0x35 -> switchBlocks(1)          // rojos
                        0x36 -> underground4SidedGround()
                        0x37 -> undergroundLargeCanvas()
                        0x38 -> undergroundRightLavaEdge()
                        0x39 -> undergroundSlopedCaveLava()
                        0x3A -> undergroundCaveLava(withSurface = true)
                        0x3B -> undergroundCaveLava(withSurface = false)
                        0x3C -> undergroundVerySteepSlope()
                        0x3D -> undergroundCeilingLedge()
                        0x3E -> undergroundCeilingEdges()
                        0x3F -> undergroundSolidDirt()
                        else -> unkStd(obj)
                    }
                    4, 5, 0xD -> when (obj) {
                        0x2E -> ghostSpikeLine()
                        0x30 -> ghostGrassLedge()
                        0x34 -> ghostWoodLedgeOnColumn()
                        0x35, 0x36 -> ghostBrickOrWood(obj - 0x35)
                        0x37 -> ghostHorizontalLog()
                        0x38 -> ghostWoodenLedgeRun(size and 0xF, pos)
                        0x39 -> ghostVerticalLog()
                        0x31 -> ghostFramedPanel()
                        0x32 -> ghostRectTopPlank()
                        0x3A -> ghostBrickWallOrSpikes()
                        0x3B -> ghostWoodBeam()
                        0x3C -> ghostWoodRectWithBase()
                        0x3D -> ghostWoodRectWithTop()
                        0x3E -> ghostWoodRectRightEdge()
                        0x3F -> ghostWoodRectLeftEdge()
                        else -> unkStd(obj)
                    }
                    else -> unkStd(obj)
                }
                return
            }
            when (obj) {
                in 0x01..0x0E -> stdCoins(obj - 1)
                // El tipo 5 es la tubería INVISIBLE y [stdVerticalPipes] ya la dibuja
                // (teselas 0x68/0x69); el gate lo excluía por error. Del 6 en adelante no
                // hay dato: las tablas de tapas solo llegan al 4.
                0x0F -> if ((size and 0xF) <= 5) stdVerticalPipes() else unkStd(obj)
                0x10 -> if (((size and 0xF0) shr 3) < 7) stdHorizontalPipes() else unkStd(obj)
                0x11 -> stdBulletShooter()
                0x12 -> stdSlopes()
                0x13 -> if ((size and 0xF) < 15) stdGroundEdges() else unkStd(obj)
                0x14 -> stdLedge(size and 0xF, size shr 4)      // entrada "standard ledge"
                0x15 -> stdMidwayGoal()
                0x16 -> stdPurpleCoins()
                0x17 -> if ((size shr 4) < 2) stdRopeCloudLine() else unkStd(obj)
                in 0x18..0x1B -> stdWaterOrLava(obj - 0x18)
                0x1C -> stdDonutBridge()
                0x1D -> stdClimbingNetBottom()
                0x1E -> stdClimbingNetSide()
                0x1F -> stdSkinnyVerticalPipe()
                0x20 -> stdHorizontalPlank()
                0x21 -> stdLedge(size, 2)                        // wide-scale ground ledge
                // Específicos de PRADERA (tilesets 0/7/0xC), que no pasan por el bloque
                // por tileset: tubería helada y bloque giratorio helado (= objeto de 1
                // tesela repetida con la entrada 0xE de la tabla, tesela 0x65).
                0x30 -> grassIcyVerticalPipe()
                0x31 -> stdCoins(0xE)
                0x32 -> switchBlocks(0)                  // azules
                0x33 -> grassForestTreeTop()
                0x34 -> grassForestGroundEdges()
                0x35 -> grassForestGround()
                0x36 -> grassLargeTreeTrunk()
                0x37 -> grassSmallTreeTrunk()
                0x38 -> switchBlocks(1)                  // rojos
                0x39 -> grassDiagonalPipeRight()
                0x3A -> grassDiagonalLedgeLeft()
                0x3B -> grassDiagonalLedgeRight()
                0x3C -> grassArchLedge()
                0x3D -> if ((size shr 4) < 2) grassTopCloudFringe() else unkStd(obj)
                0x3E -> if ((size and 0xF) < 8) grassSideCloudFringes() else unkStd(obj)
                0x3F -> if ((size shr 4) < 5) grassSmallBushes() else unkStd(obj)
                else -> unkStd(obj)
            }
        }

        fun ext(k: Int) {
            when (k) {
                0x00 -> { data++ } // screen exit: consume el byte extra; no dibuja
                0x01 -> { screen = r10 and 0x1F; nextScreen = screen } // screen jump
                in 0x10..0x16, in 0x18..0x40 -> ext1Tile(k - 16)
                0x17 -> ext1Tile(0x32)
                0x41 -> extYoshiCoin()
                0x42, 0x43 -> extTopLeftSlope(k - 66)
                0x44, 0x45 -> extPurpleTriangle(k - 68)
                0x46 -> extMidwayBar()
                0x47, 0x48 -> extDoor(k - 71)
                0x4A -> extClimbingNetDoor()
                0x90 -> extLargeBossDoor()
                0x82 -> extLargeBush(EXT_BIG_BUSH, 8, 4)
                0x83 -> extLargeBush(EXT_SMALL_BUSH, 5, 3)
                0x86 -> extGoalSign()
                0x87 -> extSwitchBlockGreen()
                0x8E -> extSwitchBlockYellow()
                in 0x57..0x5E -> extGhostSingleTile(k)
                in 0x61..0x63 -> extGhostClock(k)
                0x64, 0x65, 0x8F -> extGhost2x2(k)
                0x49 -> extGhostMural()
                0x85 -> extYoshisHouse()
                in 0x8A..0x8D -> extSwitchPalaceSwitch(k)
                0x97 -> extSwitchPalaceEdge()
                0x4B, 0x4C -> extConveyorEnd(k)
                in 0x4D..0x50 -> extLineGuideLargeCircle(k)
                in 0x51..0x54 -> extLineGuideSmallCircle(k)
                0x55 -> extLineGuideEnd(horizontalGuide = true)
                0x56 -> extLineGuideEnd(horizontalGuide = false)
                0x60 -> extCaveLavaInnerCorner()
                0x88, 0x89 -> extTreeBranch(k)
                0x70 -> extBitOfCanvasPair()
                in 0x71..0x74 -> extCanvas(k)
                in 0x75..0x7B -> extCanvasTile(k)
                in 0x7C..0x7E -> extCanvasBit(k)
                0x7F -> extTorpedoLauncher()
                in 0x91..0x92 -> extVertSteepLeftSlope(k)
                in 0x93..0x94 -> extVertNormalLeftSlope(k)
                in 0x95..0x96 -> extVertVerySteepLeftSlope(k)
                else -> unkExt(k)
            }
        }

        // -------------------- cuestas de NIVEL VERTICAL (ExtObj91/93/95) --------------------
        // Los únicos objetos del juego que usan [vertVertical]. Van en página 1 (son
        // sólidos) y cada id de la pareja elige una de las dos teselas de su tabla.

        /** 0x91/0x92: cuesta INCLINADA de una columna ($0D:A80D). */
        fun extVertSteepLeftSlope(k: Int) {
            val top = intArrayOf(0xAA, 0xAF)
            val bottom = intArrayOf(0xE2, 0xE4)
            val i = k - 0x91
            val v1 = pos
            setHi01(v1); setLo(v1, top[i])
            val v3 = vertVertical()
            setHi01(v3); setLo(v3, bottom[i])
        }

        /** 0x93/0x94: cuesta NORMAL, dos columnas de ancho ($0D:A846). */
        fun extVertNormalLeftSlope(k: Int) {
            val topLeft = intArrayOf(0x96, 0xA0)
            val topRight = intArrayOf(0x9B, 0xA5)
            val bottomLeft = intArrayOf(0xDE, 0xE6)
            val bottomRight = intArrayOf(0xE6, 0xE0)
            val i = k - 0x93
            val v1 = pos
            setHi01(v1)
            val v3 = horiz(v1, topLeft[i])
            setHi01(v3); setLo(v3, topRight[i])
            val v4 = vertVertical()
            setHi01(v4)
            val v5 = horiz(v4, bottomLeft[i])
            setHi01(v5); setLo(v5, bottomRight[i])
        }

        /** 0x95/0x96: cuesta MUY inclinada, tres filas de una columna ($0D:A87D). */
        fun extVertVerySteepLeftSlope(k: Int) {
            val top = intArrayOf(0xCA, 0xCC)
            val middle = intArrayOf(0xCB, 0xCD)
            val bottom = intArrayOf(0xF1, 0xF2)
            val i = k - 0x95
            val v1 = pos
            setHi01(v1); setLo(v1, top[i])
            val v3 = vertVertical()
            setHi01(v3); setLo(v3, middle[i])
            val v4 = vertVertical()
            setHi01(v4); setLo(v4, bottom[i])
        }

        // ------------------------------ el LIENZO (canvas) ------------------------------
        // Familia ExtObj70..ExtObj7E ($0D:CEA6, $0D:E0AE, $0D:DA68, $0D:DA80): el decorado
        // de "lienzo" con marco que usa el nivel 0x1D2. Son objetos de adorno, sin
        // colisión: piezas sueltas de una tesela, parejas y el bastidor completo de 4×5.

        /**
         * TABLA DE TESELAS DEL LIENZO ($0D:E05E, PC 0x06E05E): 4 variantes × 19 teselas.
         *
         * Va escrita a mano aquí y no copiada de `smw_0d.c` porque **la decompilación la
         * trunca**: declara `kExtObj71_Canvas1_Tiles[19]` pero el código indexa hasta la
         * 75 usando `kExtObj71_Canvas1_TileIndex = {0, 19, 38, 57}`. Las 57 teselas que
         * faltan se recuperaron leyendo la ROM en ese offset, y la propia estructura las
         * valida: los cuatro bloques empiezan por 5C 5D 5E 60 (el borde superior) y
         * acaban en 76 76 76 (el inferior), que es justo el marco del lienzo.
         */
        private val CANVAS_TILES = intArrayOf(
            // 0x71: lienzo liso
            0x5C, 0x5D, 0x5E, 0x60, 0x73, 0x74, 0x75, 0x73, 0x74, 0x75,
            0x73, 0x74, 0x75, 0x73, 0x74, 0x75, 0x76, 0x76, 0x76,
            // 0x72
            0x5C, 0x5D, 0x5E, 0x60, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C,
            0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x76, 0x76, 0x76,
            // 0x73
            0x5C, 0x5D, 0x5E, 0x60, 0x73, 0x7D, 0x75, 0x73, 0x7E, 0x75,
            0x73, 0x74, 0x75, 0x7F, 0x74, 0x75, 0x76, 0x76, 0x76,
            // 0x74
            0x5C, 0x5D, 0x5E, 0x60, 0x77, 0x82, 0x83, 0x7A, 0x85, 0x86,
            0x81, 0x78, 0x79, 0x84, 0x7B, 0x7C, 0x76, 0x76, 0x76,
        )

        /** Base de cada variante en [CANVAS_TILES] (`kExtObj71_Canvas1_TileIndex`). */
        private val CANVAS_BASE = intArrayOf(0, 19, 38, 57)

        /**
         * Objetos 0x71-0x74: LIENZO COMPLETO (ExtObj71_Canvas1, $0D:E0AE). En el nivel
         * 0x1D2 son los TAPICES colgados del castillo de Bowser. Miden 4 de ancho por 6
         * de alto y se dibujan así, gastando las 19 teselas de la variante:
         *
         *  - fila 0: cuatro teselas de página 1 (el travesaño del que cuelga, lo único
         *    sólido);
         *  - filas 1 a 4: tres teselas de página 0 cada una (la tela);
         *  - la fila 4 lleva además un remate 0x5F de página 1 a la derecha, que es fijo
         *    y no sale de la tabla;
         *  - fila 5: tres teselas de página 0 (el fleco de abajo).
         */
        fun extCanvas(k: Int) {
            var v1 = CANVAS_BASE[k - 0x71]
            var v0 = pos
            preserve()
            var r1 = 2
            var r2 = 3
            do { setHi01(v0); v0 = horiz(v0, CANVAS_TILES[v1++]); r2-- } while (r2 and 0x80 == 0)
            restore()
            var v2 = vert()
            do {
                r2 = 2
                do { setHi00(v2); v2 = horiz(v2, CANVAS_TILES[v1++]); r2-- } while (r2 and 0x80 == 0)
                restore()
                v2 = vert()
                r1--
            } while (r1 and 0x80 == 0)
            r2 = 2
            do { setHi00(v2); v2 = horiz(v2, CANVAS_TILES[v1++]); r2-- } while (r2 and 0x80 == 0)
            setHi01(v2); setLo(v2, 0x5F)
            restore()
            r2 = 2
            var v3 = vert()
            do { setHi00(v3); v3 = horiz(v3, CANVAS_TILES[v1++]); r2-- } while (r2 and 0x80 == 0)
        }

        /**
         * Objetos 0x75-0x7B: UNA tesela suelta del lienzo (ExtObj75_CanvasTile1,
         * $0D:DA68), de la tabla $0D:DA60 según (ext - 0x75).
         */
        fun extCanvasTile(k: Int) {
            val tiles = intArrayOf(0x7D, 0x7E, 0x7F, 0x80, 0x81, 0x82, 0x83)
            val v0 = pos
            setHi00(v0); setLo(v0, tiles[k - 0x75])
        }

        /**
         * Objetos 0x7C-0x7E: PAREJA VERTICAL del lienzo (ExtObj7C_BitOfCanvas1,
         * $0D:DA80): una tesela arriba y otra justo debajo, las dos de página 0.
         */
        fun extCanvasBit(k: Int) {
            val top = intArrayOf(0x81, 0x82, 0x83)
            val bottom = intArrayOf(0x84, 0x85, 0x86)
            val i = k - 0x7C
            val v0 = pos
            setHi00(v0); setLo(v0, top[i])
            val v2 = vert()
            setHi00(v2); setLo(v2, bottom[i])
        }

        /**
         * Objeto 0x70: PAREJA HORIZONTAL del lienzo (ExtObj70_BitOfCanvas1, $0D:CEA6):
         * teselas 0x84 y 0x85 una al lado de la otra, en página 0.
         */
        fun extBitOfCanvasPair() {
            val v0 = pos
            setHi00(v0)
            val v1 = horiz(v0, 0x84)
            setHi00(v1); setLo(v1, 0x85)
        }

        /**
         * Objeto 0x7F: LANZATORPEDOS (ExtObj7F_TorpedoLauncher, $0D:DAA2): cuadrado de
         * 2×2 con las teselas 0x66/0x67 arriba y 0x68/0x69 abajo, todas de página 1
         * (son sólidas). El original recorre la tabla de dos en dos usando el bit 0 del
         * índice como fin de fila, que es lo que aquí hace la condición `v1 and 1`.
         */
        fun extTorpedoLauncher() {
            val tiles = intArrayOf(0x66, 0x67, 0x68, 0x69)
            var v0 = pos
            var v1 = 0
            preserve()
            do {
                do { setHi01(v0); v0 = horiz(v0, tiles[v1++]) } while (v1 and 1 != 0)
                restore()
                v0 = vert()
            } while (v1 != 4)
        }

        /**
         * Objetos extendidos 0x57-0x5E de casa fantasma ($0D:E95F): una sola tesela de
         * página 0 tomada de la tabla $0D:E957 según (ext - 0x57). Puertas/ventanas y
         * detalles del decorado de casa fantasma.
         */
        fun extGhostSingleTile(k: Int) {
            val tiles = intArrayOf(0x73, 0x74, 0x75, 0x76, 0x93, 0x94, 0x95, 0x96)
            val v = pos
            setHi00(v); setLo(v, tiles[k - 0x57])
        }

        /**
         * Objetos extendidos 0x64/0x65 de casa fantasma ($0D:E9ED): bloque 2×2 de
         * página 0 tomado de la tabla $0D:E9E1 en grupos de 4 según (ext - 0x64). El
         * 0x25 de la tabla es aire (se deja pasar), como en el original.
         */
        fun extGhost2x2(k: Int) {
            // Tabla $0D:E9E1, leída BYTE a byte (la decompilación la declara como uint16,
            // pero el código la indexa con un puntero a uint8). La VENTANA de casa
            // fantasma (ext 0x8F) es la misma rutina entrando por el byte 8:
            // `ExtObj8F_GhostHouseWindow` no hace otra cosa que llamarla con k = 8.
            val data = intArrayOf(
                0x8C, 0x8D, 0x25, 0x8E, 0x90, 0x91, 0x8F, 0x25, // 0x64 y 0x65
                0xFC, 0xFD, 0xFE, 0xFF,                          // 0x8F: la ventana
            )
            var x = if (k == 0x8F) 8 else (k - 0x64) * 4
            preserve()
            var rows = 1
            do {
                var v1 = pos; var cnt = 1
                do { setHi00(v1); v1 = horiz(v1, data[x]); x++ } while (cnt-- != 0)
                restore(); vert()
                rows = (rows - 1) and 0xFF
            } while (rows != 0xFF)
        }

        /**
         * Objeto extendido 0x49 de casa fantasma ($0D:EABF): MURAL fijo de 6×13 teselas
         * (página 0) tomado de la tabla $0D:EA71 (la gran ilustración de la pared). Sin
         * preserve/restore: cada fila dibuja 6 teselas y baja una (como el original).
         */
        fun extGhostMural() {
            val data = intArrayOf(
                0xA5, 0xA5, 0xA4, 0xA5, 0xA5, 0xA4, 0xA7, 0xA8, 0xA4, 0xA7, 0xA8, 0xA4, 0xAC, 0xAD, 0xA4, 0xAC,
                0xAD, 0xA4, 0xAE, 0xAF, 0xA4, 0xAE, 0xAF, 0xA4, 0xB0, 0xB1, 0xA4, 0xB0, 0xB1, 0xA4, 0xA7, 0xA8,
                0xA4, 0xA7, 0xA8, 0xA4, 0xA5, 0xA5, 0xA5, 0xA5, 0xA5, 0xA4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xA4,
                0xAC, 0xB2, 0xAD, 0xB4, 0xB4, 0xA4, 0xB0, 0xB3, 0xB1, 0xB4, 0xB4, 0xA4, 0xC1, 0xC2, 0xC6, 0xB4,
                0xB4, 0xA4, 0xC1, 0xC2, 0xC6, 0xA5, 0xA5, 0xA4, 0xC1, 0xC2, 0xC6, 0xA7, 0xA8, 0xA4,
            )
            var idx = 0
            while (idx < 0x4E) {
                var v1 = pos; var c = 5
                do { setHi00(v1); v1 = horiz(v1, data[idx]); idx++ } while (c-- != 0)
                vert()
            }
        }

        /**
         * Objetos extendidos 0x61-0x63 de casa fantasma (ExtObj61_GhostHouseClock,
         * $0D:E9AA): bloque 3×3 de página 0 tomado de la tabla $0D:E99B en grupos de 9
         * según (ext - 0x61) — el RELOJ de pared (0x61) y las dos telarañas diagonales
         * (0x62/0x63). El 0x25 de la tabla es aire, como en el original.
         */
        fun extGhostClock(k: Int) {
            val data = intArrayOf(
                0x97, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F,
                0x86, 0x87, 0x25, 0x25, 0x86, 0x87, 0x25, 0x25, 0x86,
                0x25, 0x84, 0x85, 0x84, 0x85, 0x25, 0x85, 0x25, 0x25,
            )
            var x = (k - 0x61) * 9
            preserve()
            var rows = 2
            do {
                var v1 = pos; var cnt = 2
                do { setHi00(v1); v1 = horiz(v1, data[x]); x++ } while (cnt-- != 0)
                restore(); vert()
                rows = (rows - 1) and 0xFF
            } while (rows != 0xFF)
        }

        /**
         * Objeto extendido 0x85: CASA DE YOSHI (ExtObj85_YoshisHouse, $0D:EC33). Mural
         * fijo de 16×10 teselas de página 0 tomado de la tabla $0D:EBF3. Como el
         * original, cada fila escribe 15 teselas avanzando y la 16ª SIN avanzar (solo
         * `SetMap16LowByte`) antes de bajar de fila. El 0x25 es aire.
         */
        fun extYoshisHouse() {
            val data = intArrayOf(
                0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0xCB, 0xCC, 0x25, 0x25, 0x25,
                0x25, 0xCD, 0xCE, 0xCF, 0xCF, 0xCF, 0xCF, 0xCF, 0xCF, 0xCF, 0xCF, 0xCF, 0xCF, 0xD0, 0xD1, 0x25,
                0x25, 0xD2, 0xEB, 0xD3, 0xD3, 0xD3, 0xEB, 0xD3, 0xD3, 0xD3, 0xD3, 0xEB, 0xD3, 0xD3, 0xD4, 0x25,
                0x25, 0xD5, 0xD3, 0xEB, 0xD3, 0xD3, 0xD3, 0xD3, 0xD3, 0xEB, 0xD3, 0xD3, 0xD3, 0xEB, 0xD6, 0x25,
                0x25, 0xD5, 0xD3, 0xD3, 0xD3, 0xD3, 0xD3, 0xEB, 0xD3, 0xD3, 0xD3, 0xD3, 0xD3, 0xD3, 0xD6, 0x25,
                0x25, 0xD7, 0xD8, 0xD9, 0xD8, 0xD8, 0xD9, 0xD8, 0xD8, 0xD9, 0xD8, 0xDA, 0xDB, 0xD8, 0xDC, 0x25,
                0x25, 0x25, 0x25, 0xDD, 0x25, 0x25, 0xDD, 0x25, 0x25, 0xDD, 0x25, 0xCB, 0xCC, 0x25, 0x25, 0x25,
                0x25, 0x25, 0xDE, 0xDD, 0x25, 0x25, 0xDD, 0x25, 0x25, 0xDD, 0x25, 0xCB, 0xCC, 0x25, 0x25, 0x25,
                0x25, 0xDF, 0xE0, 0xE1, 0x25, 0x25, 0xDD, 0x25, 0x25, 0xDD, 0x25, 0xE2, 0xE3, 0xE4, 0x25, 0x25,
                0xE5, 0xE5, 0xE6, 0xDD, 0xE5, 0xE5, 0xDD, 0xE5, 0xE5, 0xDD, 0xE5, 0xE7, 0xE8, 0xE9, 0xE5, 0xE5,
            )
            var i = 0
            while (i != 0xA0) {
                var v1 = pos; var c = 15
                do { setHi00(v1); v1 = horiz(v1, data[i]); i++ } while (--c != 0)
                setHi00(v1); setLo(v1, data[i]); i++
                vert()
            }
        }

        /**
         * Objetos extendidos 0x8A-0x8D: los cuatro INTERRUPTORES de palacio
         * (ExtObj8D_SwitchPalaceSwitch, $0D:EC8E). Bloque 2×2 de página 0 tomado de la
         * tabla $0D:EC7E en grupos de 4 según (ext + 118) mod 256 → 0..3 (verde,
         * amarillo, azul, rojo).
         *
         * El original omite el dibujo si el interruptor YA está pulsado
         * (`flag_activated_switches`); en un parse EN FRÍO nada está pulsado, así que
         * siempre se dibuja (misma convención que el objeto de 1 tesela). A diferencia
         * del resto de murales, la rutina NO hace preserve/restore: se replica igual.
         */
        fun extSwitchPalaceSwitch(k: Int) {
            val data = intArrayOf(
                0xEC, 0xED, 0xEE, 0xEF, 0xF0, 0xF1, 0xF2, 0xF3,
                0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9, 0xFA, 0xFB,
            )
            var x = ((k + 118) and 0xFF) * 4
            var rows = 1
            do {
                var v1 = pos; var cnt = 1
                do { setHi00(v1); v1 = horiz(v1, data[x]); x++ } while (cnt-- != 0)
                vert()
                rows = (rows - 1) and 0xFF
            } while (rows != 0xFF)
        }

        /**
         * Objeto extendido 0x97: tesela de BORDE derecho/inferior del palacio del
         * interruptor (ExtObj97_SwitchPalaceRightAndBottomEdgeTile, $0D:EC5C). Una sola
         * tesela 0x10, y es de PÁGINA 1 (no 0, como casi todos los extendidos).
         */
        fun extSwitchPalaceEdge() { val v1 = pos; setHi01(v1); setLo(v1, 0x10) }

        // ---- GUÍAS DE LÍNEA (las vías por las que corren plataformas y sierras) ----

        /**
         * Extendidos 0x4B/0x4C: REMATE DE CINTA TRANSPORTADORA ($0D:C259). Una tesela de
         * página 1 según (ext - 0x4B).
         */
        fun extConveyorEnd(k: Int) {
            val tiles = intArrayOf(0x07, 0x08)
            val v1 = pos; setHi01(v1); setLo(v1, tiles[k - 0x4B])
        }

        /**
         * Extendidos 0x4D-0x50: CUARTO DE CÍRCULO GRANDE de guía ($0D:CE67). Bloque 2×2
         * de página 0 tomado de la tabla $0D:CE57 en grupos de 4 según (ext - 0x4D); el
         * original recorre la tabla con los bits 0 y 1 del índice, que es justo un 2×2.
         * El 0x25 de la tabla es aire.
         */
        fun extLineGuideLargeCircle(k: Int) {
            val tiles = intArrayOf(
                0x7A, 0x7B, 0x7C, 0x25, 0x7E, 0x7F, 0x25, 0x7D,
                0x82, 0x25, 0x80, 0x81, 0x25, 0x83, 0x84, 0x85,
            )
            var v1 = 4 * (k - 0x4D)
            var v0 = pos
            preserve()
            do {
                do { setHi00(v0); v0 = horiz(v0, tiles[v1]); v1++ } while (v1 and 1 != 0)
                restore(); v0 = vert()
            } while (v1 and 3 != 0)
        }

        /**
         * Extendidos 0x51-0x54: CUARTO DE CÍRCULO PEQUEÑO ($0D:CE94). Una sola tesela de
         * página 0 según (ext - 0x51).
         */
        fun extLineGuideSmallCircle(k: Int) {
            val tiles = intArrayOf(0x76, 0x77, 0x78, 0x79)
            val v1 = pos; setHi00(v1); setLo(v1, tiles[k - 0x51])
        }

        /**
         * Extendido 0x55 ($0D:CEC0) y 0x56 ($0D:CEDA): los REMATES de una guía. El nombre
         * del original despista: el remate de la guía HORIZONTAL (0x55) apila sus dos
         * teselas en VERTICAL, y el de la VERTICAL (0x56) las pone en horizontal.
         */
        fun extLineGuideEnd(horizontalGuide: Boolean) {
            if (horizontalGuide) {
                val tiles = intArrayOf(0x96, 0x97)
                var v0 = pos
                for (t in tiles) { setHi00(v0); setLo(v0, t); v0 = vert() }
            } else {
                val tiles = intArrayOf(0x98, 0x99)
                var v0 = pos
                for (t in tiles) { setHi00(v0); v0 = horiz(v0, t) }
            }
        }

        /** Extendido 0x60: ESQUINA INTERIOR de la lava de cueva ($0D:DA57), tesela 0xFE
         *  de página 1. */
        fun extCaveLavaInnerCorner() { val v1 = pos; setHi01(v1); setLo(v1, 0xFE) }

        /**
         * Extendidos 0x88/0x89: RAMAS del árbol de bosque, derecha e izquierda
         * ($0D:B6E3). Una tesela de página 0; el original indexa la tabla con
         * `ext + 120` aprovechando que da la vuelta el byte (0x88 → 0, 0x89 → 1).
         */
        fun extTreeBranch(k: Int) {
            val tiles = intArrayOf(0xC1, 0xC2)
            val v1 = pos; setHi00(v1); setLo(v1, tiles[(k + 120) and 0xFF])
        }

        // --------------------------- objetos extendidos ---------------------------

        /** Objeto de 1 tesela (bloque ?, seta, moneda, luna…): $0D:A57F simplificado
         *  con memoria de objetos a 0 (parse en frío: nada está "cogido"). */
        fun ext1Tile(a: Int) {
            val v4 = pos
            setHi00(v4)
            if (ge8(a, 19)) setHi01(v4)
            setLo(v4, EXT_1TILE_TILES[a])
        }

        fun extYoshiCoin() {
            val v2 = pos
            setHi00(v2); setLo(v2, 0x2D)
            val v3 = vert()
            setHi00(v3); setLo(v3, 0x2E)
        }

        /**
         * Objetos estándar 0x18-0x1B: AGUA/LAVA con superficie (RopeObj2E/StdObj18,
         * $0D:B3E3): rectángulo página 0 con la fila superior de TopTiles[k] y el
         * relleno de BottomTiles[k] (k = obj-0x18: agua 0x00/0x02, agua sin animar
         * 0x01/0x03, lava 0x04/0x05, y 0x08/0x0B).
         */
        fun stdWaterOrLava(k: Int) {
            val top = intArrayOf(0x00, 0x01, 0x04, 0x08)
            val bottom = intArrayOf(0x02, 0x03, 0x05, 0x0B)
            var v1 = pos
            val r0 = size and 0xF
            var r2 = size and 0xF
            var r1 = size shr 4
            preserve()
            do {
                setHi00(v1)
                v1 = horiz(v1, top[k])
                r2 = (r2 - 1) and 0xFF
            } while (r2 and 0x80 == 0)
            while (true) {
                restore()
                var v3 = vert()
                r2 = r0
                r1 = (r1 - 1) and 0xFF
                if (r1 and 0x80 != 0) break
                do {
                    setHi00(v3)
                    v3 = horiz(v3, bottom[k])
                    r2 = (r2 - 1) and 0xFF
                } while (r2 and 0x80 == 0)
            }
        }

        /**
         * Objeto estándar 0x1D: RED TREPABLE con borde inferior (StdObj1D, $0D:B461):
         * filas de red (0x0B) y una última fila de borde (0x0E), página 0.
         */
        fun stdClimbingNetBottom() {
            var v1 = pos
            var r0 = size shr 4
            val r1 = size and 0xF
            var v2 = size and 0xF
            preserve()
            while (r0 != 0) {
                do {
                    setHi00(v1)
                    v1 = horiz(v1, 0x0B)
                    v2--
                } while (v2 >= 0)
                restore()
                v1 = vert()
                v2 = r1
                r0--
            }
            do {
                setHi00(v1)
                v1 = horiz(v1, 0x0E)
                v2--
            } while (v2 >= 0)
        }

        /**
         * Objeto estándar 0x1E: BORDE LATERAL de red trepable (StdObj1E, $0D:B49E):
         * columna de SideTiles[k] (k = nibble bajo: 0 izq, 1 der) cuyas puntas se
         * convierten en ESQUINA leyendo lo ya dibujado en el buffer (0x08 → esquina
         * superior, 0x0E → esquina interior), como hace el juego.
         */
        fun stdClimbingNetSide() {
            val kk = size and 0x1
            val side = intArrayOf(0x0A, 0x0C)[kk]
            val topCorner = intArrayOf(0x07, 0x09)[kk]
            val topInner = intArrayOf(0x1A, 0x19)[kk]
            val botCorner = intArrayOf(0x0D, 0x0F)[kk]
            val botInner = intArrayOf(0x1C, 0x1B)[kk]
            var v1 = pos
            var r0 = size shr 4
            run {
                val a = when (rdLo(v1)) {
                    0x08 -> topCorner
                    0x0E -> topInner
                    else -> side
                }
                setHi00(v1); setLo(v1, a)
            }
            while (true) {
                if (v1 + 16 >= 256) pageCross(1)
                v1 = (v1 + 16) and 0xFF
                r0 = (r0 - 1) and 0xFF
                if (r0 == 0) break
                setHi00(v1); setLo(v1, side)
            }
            val a = when (rdLo(v1)) {
                0x0E -> botCorner
                0x08 -> botInner
                else -> side
            }
            setHi00(v1); setLo(v1, a)
        }

        /**
         * Objeto de CASTILLO 0x34: TUBERÍA VERTICAL de doble boca (CastleObj34,
         * $0D:C5D8): boca 0x33/0x34 (página 1) arriba y abajo, cuerpo 0x9D/0x9E
         * (página 0) en medio. Alto = nibble alto (uint8: 0 = 256).
         */
        fun castleVerticalDoubleEndedPipe() {
            var r0 = size shr 4
            if (r0 == 0) r0 = 256
            preserve()
            val v1 = pos
            setHi01(v1)
            val v2 = horiz(v1, 0x33)
            setHi01(v2); setLo(v2, 0x34)
            var v4: Int
            while (true) {
                restore()
                v4 = vert()
                if (--r0 == 0) break
                setHi00(v4)
                val v3 = horiz(v4, 0x9D)
                setHi00(v3); setLo(v3, 0x9E)
            }
            setHi01(v4)
            val v5 = horiz(v4, 0x33)
            setHi01(v5); setLo(v5, 0x34)
        }

        /**
         * Objeto extendido 0x90: PUERTA GRANDE DEL JEFE 2×3 (ExtObj90, $0D:C31E):
         * teselas 98/99, 9A/9B, 9C/9C, página 0.
         */
        fun extLargeBossDoor() {
            val tiles = intArrayOf(0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9C)
            var v0 = pos
            var v1 = 0
            do {
                var r1 = 1
                do {
                    setHi00(v0)
                    v0 = horiz(v0, tiles[v1++])
                    r1--
                } while (r1 >= 0)
                v0 = vert()
            } while (v1 != 6)
        }

        // ------------------------- objetos de CASA FANTASMA -------------------------
        // Ports 1:1 de las rutinas GhostHouseObjXX del banco $0D (tilesets 4/5/0xD).

        /**
         * Objeto 0x2E de casa fantasma: LÍNEA HORIZONTAL DE PINCHOS
         * (GhostHouseObj2E_HorizontalLineOfSpikes, $0D:F06C). Repite una misma tesela
         * de página 1 a lo ancho (nibble bajo + 1); el nibble ALTO elige la tesela en
         * la tabla $0D:F065, de la que el juego solo usa la entrada 0 (0x59). Para
         * cualquier otro índice no tenemos el dato: se declara desconocido en vez de
         * pintar basura.
         */
        fun ghostSpikeLine() {
            val variant = size shr 4
            if (variant != 0) { unkStd(0x2E); return }
            var v1 = pos
            var r0 = size and 0xF
            do { setHi01(v1); v1 = horiz(v1, 0x59); r0 = (r0 - 1) and 0xFF } while (r0 and 0x80 == 0)
        }

        /**
         * Objeto 0x30 de casa fantasma: REPISA DE HIERBA
         * (GhostHouseObj30_GrassLedge1, $0D:F02B). Fila superior de hierba (tesela 0x0F,
         * página 1) de ancho nibble-bajo + 1, y debajo nibble-alto filas de relleno
         * (tesela 0xEA, página 0).
         */
        fun ghostGrassLedge() {
            val w = size and 0xF
            var r1 = size shr 4
            var v1 = pos
            preserve()
            var c = w
            do { setHi01(v1); v1 = horiz(v1, 0x0F); c-- } while (c >= 0)
            while (true) {
                restore()
                var v4 = vert()
                r1 = (r1 - 1) and 0xFF
                if (r1 and 0x80 != 0) break
                var c2 = w
                do { setHi00(v4); v4 = horiz(v4, 0xEA); c2-- } while (c2 >= 0)
            }
        }

        /**
         * Tramo de SALIENTE DE MADERA (GhostHouseObj38 y la repisa de Obj34,
         * $0D:ED4A): teselas 0x0A + 0x0B… y remate 0x0C, página 1.
         */
        fun ghostWoodenLedgeRun(kIn: Int, jIn: Int) {
            var k = if (kIn == 0) 256 else kIn
            var j = jIn
            setHi01(j)
            var i = 0x0A
            while (true) {
                j = horiz(j, i)
                if (--k == 0) break
                setHi01(j)
                i = 0x0B
            }
            setHi01(j)
            setLo(j, 0x0C)
        }

        /**
         * Objeto 0x34 de casa fantasma: SALIENTE DE MADERA SOBRE COLUMNAS
         * ($0D:EEC0): repisa de ancho 4n+2 y una columna (0x78 + 0x79…) cada 4
         * celdas bajo ella. El cruce de página del interior de la columna NO
         * sincroniza el puntero de respaldo, como el original.
         */
        fun ghostWoodLedgeOnColumn() {
            var r0 = size and 0xF
            val r1 = size shr 4
            preserve()
            ghostWoodenLedgeRun(4 * (size and 0xF) + 2, pos)
            restore()
            var v3 = (pos + 1) and 0xFF
            if (v3 and 0xF == 0) { v3 = pos and 0xF0; forwardOneScreen() }
            if (v3 + 16 >= 256) pageCross(1)
            pos = (v3 + 16) and 0xFF
            var v5 = pos
            do {
                restore()
                var v6 = if (r1 == 0) 256 else r1
                setHi00(v5)
                var i = 0x78
                while (true) {
                    setLo(v5, i)
                    if (v5 + 16 >= 256) ptr += 0x100 // cruce crudo, sin tocar ptrBak
                    v5 = (v5 + 16) and 0xFF
                    if (--v6 == 0) break
                    setHi00(v5)
                    i = 0x79
                }
                restore()
                v5 = (pos + 4) and 0xFF
                if (((v5 and 0xF) - 4) and 0x80 != 0) {
                    v5 = (pos - 12) and 0xFF
                    forwardOneScreen()
                }
                pos = v5
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        /**
         * Objetos 0x35/0x36 de casa fantasma: FONDO DE LADRILLO (k=0, tesela 0x92,
         * página 0) o BLOQUES DE MADERA sólidos (k=1, tesela 0x5E, página 1)
         * ($0D:ECCE): rectángulo ancho×alto.
         */
        fun ghostBrickOrWood(k: Int) {
            val tiles = intArrayOf(0x92, 0x5E, 0x82)
            var v1 = pos
            val r0 = size and 0xF
            var r2 = size and 0xF
            var r1 = size shr 4
            preserve()
            while (true) {
                setHi00(v1)
                if (k == 1) setHi01(v1)
                v1 = horiz(v1, tiles[k])
                r2 = (r2 - 1) and 0xFF
                if (r2 and 0x80 != 0) {
                    restore()
                    v1 = vert()
                    r2 = r0
                    r1 = (r1 - 1) and 0xFF
                    if (r1 and 0x80 != 0) break
                }
            }
        }

        /**
         * Objeto 0x37 de casa fantasma: TRONCO/BARANDILLA horizontal de fondo
         * ($0D:ED12): fila izquierda/medio/derecha según el tipo (nibble alto),
         * página 0.
         */
        fun ghostHorizontalLog() {
            val left = intArrayOf(0x82, 0x89, 0x88)
            val mid = intArrayOf(0x82, 0x8A, 0x88)
            val right = intArrayOf(0x82, 0x8B, 0x88)
            val v2 = (size shr 4).coerceAtMost(2) // vanilla solo usa 0..2
            var v1 = pos
            var r0 = size and 0xF
            if (r0 == 0) r0 = 256
            setHi00(v1)
            var i = left[v2]
            while (true) {
                v1 = horiz(v1, i)
                if (--r0 == 0) break
                setHi00(v1)
                i = mid[v2]
            }
            setHi00(v1)
            setLo(v1, right[v2])
        }

        /**
         * Objeto 0x39 de casa fantasma: TRONCO VERTICAL de fondo ($0D:ED6B):
         * columna con tesela superior y cuerpo según el tipo (nibble bajo), página 0.
         */
        fun ghostVerticalLog() {
            val top = intArrayOf(0x83, 0x78, 0x79)
            val bottom = intArrayOf(0x83, 0x79, 0x79)
            val v2 = (size and 0xF).coerceAtMost(2) // vanilla solo usa 0..2
            var v1 = pos
            var r0 = size shr 4
            setHi00(v1)
            var i = top[v2]
            while (true) {
                setLo(v1, i)
                v1 = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                setHi00(v1)
                i = bottom[v2]
            }
        }

        /**
         * Objeto 0x3A de casa fantasma: MURO DE LADRILLO sólido o LÍNEA VERTICAL DE
         * PINCHOS ($0D:ED99): columna de la tesela elegida por el nibble bajo
         * (0x5F/0x60 muro, 0x5A/0x5B pinchos), página 1.
         */
        fun ghostBrickWallOrSpikes() {
            val tiles = intArrayOf(0x5F, 0x60, 0x5A, 0x5B)
            val v1 = (size and 0xF).coerceAtMost(3) // vanilla solo usa 0..3
            var v2 = pos
            var r0 = size shr 4
            do {
                setHi01(v2)
                setLo(v2, tiles[v1])
                v2 = vert()
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        /**
         * Objeto 0x3B de casa fantasma: VIGA HORIZONTAL de madera ($0D:EDB9): tesela
         * inicial 0x07, (n-1) teselas centrales 0x08 y tesela final 0x09, página 1.
         */
        fun ghostWoodBeam() {
            var x = size and 0xF
            var v1 = pos
            setHi01(v1); v1 = horiz(v1, 0x07)
            x = (x - 1) and 0xFF
            while (x != 0) { setHi01(v1); v1 = horiz(v1, 0x08); x = (x - 1) and 0xFF }
            setHi01(v1); setLo(v1, 0x09)
        }

        /**
         * Objeto 0x3C de casa fantasma: LOSA de madera ($0D:EDDB): rectángulo de
         * (nibble bajo +1) de ancho, con (nibble alto) filas de 0x53 y una fila
         * inferior de remate 0x54, página 1.
         */
        fun ghostWoodRectWithBase() {
            val w = size and 0xF
            var rows = size shr 4
            preserve()
            while (rows != 0) {
                var v1 = pos; var x = w
                do { setHi01(v1); v1 = horiz(v1, 0x53) } while (x-- != 0)
                restore(); vert()
                rows = (rows - 1) and 0xFF
            }
            var v1 = pos; var x = w
            do { setHi01(v1); v1 = horiz(v1, 0x54) } while (x-- != 0)
        }

        /**
         * Objeto 0x3D de casa fantasma: LOSA con REMATE SUPERIOR ($0D:EE17): una fila
         * superior de 0x5D y (nibble alto) filas de 0x53 debajo, ancho nibble bajo +1,
         * página 1.
         */
        fun ghostWoodRectWithTop() {
            val w = size and 0xF
            var rows = size shr 4
            preserve()
            run { var v1 = pos; var x = w; do { setHi01(v1); v1 = horiz(v1, 0x5D) } while (x-- != 0) }
            restore(); vert()
            while (rows != 0) {
                var v1 = pos; var x = w
                do { setHi01(v1); v1 = horiz(v1, 0x53) } while (x-- != 0)
                restore(); vert()
                rows = (rows - 1) and 0xFF
            }
        }

        /**
         * Objeto 0x3E de casa fantasma: LOSA con BORDE DERECHO ($0D:EE52): (nibble
         * alto +1) filas, cada una con (nibble bajo) teselas 0x53 y una tesela de
         * borde 0x55 a la derecha, página 1.
         */
        fun ghostWoodRectRightEdge() {
            val w = size and 0xF
            var rows = size shr 4
            preserve()
            do {
                var v1 = pos; var x = w
                while (x != 0) { setHi01(v1); v1 = horiz(v1, 0x53); x = (x - 1) and 0xFF }
                setHi01(v1); v1 = horiz(v1, 0x55)
                restore(); vert()
                rows = (rows - 1) and 0xFF
            } while (rows != 0xFF)
        }

        /**
         * Objeto 0x3F de casa fantasma: LOSA con BORDE IZQUIERDO ($0D:EE89): (nibble
         * alto +1) filas, cada una con una tesela de borde 0x5C a la izquierda y
         * (nibble bajo +1) teselas 0x53 si el ancho no es 0, página 1.
         */
        fun ghostWoodRectLeftEdge() {
            val w = size and 0xF
            var rows = size shr 4
            preserve()
            do {
                var v1 = pos
                setHi01(v1); v1 = horiz(v1, 0x5C)
                if (w != 0) {
                    var x = w
                    do { setHi01(v1); v1 = horiz(v1, 0x53) } while (x-- != 0)
                }
                restore(); vert()
                rows = (rows - 1) and 0xFF
            } while (rows != 0xFF)
        }

        /**
         * Objeto estándar 0x20 ($0D:B547): BARRA/TABLÓN horizontal de página 1 (tesela
         * inicial 0x56, centrales 0x57, final 0x58). Objeto común (no específico de
         * tileset); en casa fantasma es una pasarela de madera.
         */
        fun stdHorizontalPlank() {
            var x = size and 0xF
            var v1 = pos
            setHi01(v1); v1 = horiz(v1, 0x56)
            x = (x - 1) and 0xFF
            while (x != 0) { setHi01(v1); v1 = horiz(v1, 0x57); x = (x - 1) and 0xFF }
            setHi01(v1); setLo(v1, 0x58)
        }

        /**
         * Objeto 0x32 de casa fantasma ($0D:EF67): PLATAFORMA con fila superior de 0x0E
         * (página 1) y (nibble alto) filas de relleno 0xA3 (página 0), ancho nibble
         * bajo +1.
         */
        fun ghostRectTopPlank() {
            val w = size and 0xF
            var rows = size shr 4
            preserve()
            run { var v1 = pos; var x = w; do { setHi01(v1); v1 = horiz(v1, 0x0E) } while (x-- != 0) }
            while (rows != 0) {
                restore(); vert()
                var v1 = pos; var x = w
                do { setHi00(v1); v1 = horiz(v1, 0xA3) } while (x-- != 0)
                rows = (rows - 1) and 0xFF
            }
        }

        /**
         * Objeto 0x31 de casa fantasma ($0D:EFA8): PANEL ENMARCADO (estantería/ventana):
         * fila superior 0x61/0x0D/0x62, (alto-1) filas centrales que ALTERNAN entre dos
         * patrones (izq/relleno/der de las tablas $0D:EFA2/EFA4/EFA6, con el relleno en
         * página 0), y fila inferior 0x6B/0x6C/0x6D. Marco en página 1.
         */
        fun ghostFramedPanel() {
            val leftMid = intArrayOf(0x63, 0x65)
            val fillMid = intArrayOf(0xC7, 0xC8)
            val rightMid = intArrayOf(0x64, 0x6A)
            val w = size and 0xF
            var h = size shr 4
            preserve()
            var v1 = pos; var c = w
            setHi01(v1); v1 = horiz(v1, 0x61); c = (c - 1) and 0xFF
            while (c != 0) { setHi01(v1); v1 = horiz(v1, 0x0D); c = (c - 1) and 0xFF }
            setHi01(v1); setLo(v1, 0x62)
            var x = 1
            restore(); x = x xor 1; vert(); h = (h - 1) and 0xFF
            while (h != 0) {
                v1 = pos; c = w
                setHi01(v1); v1 = horiz(v1, leftMid[x]); c = (c - 1) and 0xFF
                while (c != 0) { setHi00(v1); v1 = horiz(v1, fillMid[x]); c = (c - 1) and 0xFF }
                setHi01(v1); setLo(v1, rightMid[x])
                restore(); x = x xor 1; vert(); h = (h - 1) and 0xFF
            }
            v1 = pos; c = w
            setHi01(v1); v1 = horiz(v1, 0x6B); c = (c - 1) and 0xFF
            while (c != 0) { setHi01(v1); v1 = horiz(v1, 0x6C); c = (c - 1) and 0xFF }
            setHi01(v1); setLo(v1, 0x6D)
        }

        /**
         * Rectángulo 3×3 genérico (izquierda/medio/derecha × fila superior/media/
         * inferior), página 1: el patrón que comparten el BLOQUE DE PIEDRA de
         * castillo (CastleObj3C, $0D:C478) y el SUELO DE 4 LADOS de subterráneo
         * (UndergroundObj36, $0D:E135) — misma rutina, distintas tablas.
         */
        fun rect3x3(left: IntArray, mid: IntArray, right: IntArray) {
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size shr 4
            preserve()
            var v2 = 0
            while (true) {
                var r2 = r0
                setHi01(v1)
                var i = horiz(v1, left[v2])
                while (true) {
                    r2 = (r2 - 1) and 0xFF
                    if (r2 == 0) break
                    setHi01(i)
                    i = horiz(i, mid[v2])
                }
                setHi01(i)
                setLo(i, right[v2])
                restore()
                v1 = vert()
                v2 = 1
                val lastNext = r1 == 1
                r1 = (r1 - 1) and 0xFF
                if (r1 and 0x80 != 0) break
                if (lastNext) v2 = 2
            }
        }

        /**
         * Objeto de CASTILLO 0x3C: BLOQUE DE PIEDRA (CastleObj3C, $0D:C478):
         * superior 5D/5E/5F, medias 60/61/62, inferior 63/64/65. El muro básico.
         */
        /**
         * Objeto 0x3D de CASTILLO: ESCALERA MECÁNICA (CastleObj3D_Escalator, $0D:C341).
         * El bit 2 del tamaño elige lado: 0 = izquierda ($0D:C358), 1 = derecha
         * ($0D:C3D8); los bits 0-1 eligen cuál de las cuatro cintas.
         *
         * Es una escalera diagonal: cada peldaño pone la tesela de CINTA (página 1),
         * luego la ESQUINA y detrás un tramo de tierra 0x3F (página 0) que crece un
         * bloque por peldaño, y salta en diagonal al siguiente.
         */
        fun castleEscalator() {
            val conv = intArrayOf(0xCE, 0xD1, 0xCF, 0xD0)
            val corner = intArrayOf(0xF3, 0xF6, 0xF4, 0xF5)
            val v1 = size and 3
            var r0 = (size shr 4) + 1
            var r2 = 0
            preserve()
            if (size and 2 != 0) {
                // --- lado DERECHO ---
                var v0 = pos
                while (r0 != 0) {
                    setHi01(v0)
                    horiz(v0, conv[v1])
                    restore()
                    var v2 = vert()
                    r2++
                    var r3 = r2
                    r0 = (r0 - 1) and 0xFF
                    if (r0 and 0x80 != 0) break
                    while (r3 != 1) {
                        setHi00(v2); v2 = horiz(v2, 0x3F); r3--
                    }
                    setHi01(v2)
                    v0 = horiz(v2, corner[v1])
                }
                return
            }
            // --- lado IZQUIERDO ---
            var v0 = pos
            var v4: Boolean
            while (true) {
                var r3 = r2
                setHi01(v0)
                var i = horiz(v0, conv[v1])
                while (true) {
                    r3 = (r3 - 1) and 0xFF
                    if (r3 and 0x80 == 0) {
                        setHi01(i)
                        var j = horiz(i, corner[v1])
                        while (true) {
                            r3 = (r3 - 1) and 0xFF
                            if (r3 and 0x80 != 0) break
                            setHi00(j); j = horiz(j, 0x3F)
                        }
                    }
                    restore()
                    r2++
                    r0 = (r0 - 1) and 0xFF
                    v4 = r0 and 0x80 != 0
                    if (r0 != 0) break
                    r3 = r2
                    i = vert()
                }
                if (v4) break
                // Paso diagonal abajo-izquierda (una fila y una columna).
                val sum = pos + 15
                v0 = sum and 0xFF
                if (sum >= 256) pageCross(1)
                if (v0 and 0xF == 15) {
                    if (v0 + 16 >= 256) pageCross(1)
                    v0 = (v0 + 16) and 0xFF
                    backOneScreen()
                }
                pos = v0
            }
        }

        fun castleStoneBlock() =
            rect3x3(intArrayOf(0x5D, 0x60, 0x63), intArrayOf(0x5E, 0x61, 0x64), intArrayOf(0x5F, 0x62, 0x65))

        /**
         * Objeto 0x35 de castillo: MURO DE ROCA de fondo (CastleObj35_RockWallBackground,
         * $0D:C58A). Rectángulo hecho con un patrón de 2×2 teselas —0x94/0x95 arriba y
         * 0x96/0x97 abajo— repetido nibble-bajo+1 veces a lo ancho y nibble-alto+1 a lo
         * alto. Todo de página 0: es decorado, no bloquea.
         */
        fun castleRockWallBackground() {
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size shr 4
            preserve()
            do {
                var v2 = r0
                do {
                    setHi00(v1)
                    val v3 = horiz(v1, 0x94)
                    setHi00(v3)
                    v1 = horiz(v3, 0x95)
                    v2--
                } while (v2 >= 0)
                restore()
                var v4 = vert()
                var v5 = r0
                do {
                    setHi00(v4)
                    val v6 = horiz(v4, 0x96)
                    setHi00(v6)
                    v4 = horiz(v6, 0x97)
                    v5--
                } while (v5 >= 0)
                restore()
                v1 = vert()
                r1--
            } while (r1 and 0x80 == 0)
        }

        /**
         * Objeto 0x36 de castillo: PILAR GRANDE CON PINCHOS (CastleObj36_LargeSpikedPillar,
         * $0D:C4EF). Cuatro columnas de ancho: los dos bordes van en página 0 (decorado) y
         * las dos del medio en página 1 (los pinchos, que sí hacen daño).
         *
         * El nibble BAJO no es un tamaño sino el lado por el que remata: si vale algo, el
         * remate (0x87/0x88) va ARRIBA; si vale 0, va ABAJO (0x8D/0x8E). Y el cuerpo
         * alterna dos filas distintas, que es lo que da el dibujo de los eslabones.
         */
        fun castleLargeSpikedPillar() {
            var v1 = pos
            val v2 = size and 0xF
            var r0 = size shr 4
            preserve()
            if (v2 != 0) {
                val v3 = horizNext(v1)
                setHi00(v3)
                val v4 = horiz(v3, 0x87)
                setHi00(v4); horiz(v4, 0x88)
                restore()
                v1 = vert()
            }
            do {
                setHi00(v1)
                val v5 = horiz(v1, 0x89)
                setHi01(v5)
                val v6 = horiz(v5, 0x66)
                setHi01(v6)
                val v7 = horiz(v6, 0x67)
                setHi00(v7); horiz(v7, 0x8A)
                restore()
                v1 = vert()
                r0--
                if (r0 and 0x80 != 0) break
                setHi00(v1)
                val v8 = horiz(v1, 0x8B)
                setHi01(v8)
                val v9 = horiz(v8, 0x68)
                setHi01(v9)
                val v10 = horiz(v9, 0x69)
                setHi00(v10); horiz(v10, 0x8C)
                restore()
                v1 = vert()
                r0--
            } while (r0 and 0x80 == 0)
            if (v2 == 0) {
                val v11 = horizNext(v1)
                setHi00(v11)
                val v12 = horiz(v11, 0x8D)
                setHi00(v12); horiz(v12, 0x8E)
            }
        }

        /**
         * BLOQUES DE INTERRUPTOR (GrassObj32_BlueSwitchBlocks, $0D:B920): rectángulo de
         * ancho nibble-bajo + 1 por alto nibble-alto + 1. [kind] 0 = AZUL, 1 = ROJO.
         *
         * Los usan cuatro casillas de las tablas por tileset (castillo 0x39/0x3A y
         * pradera 0x32/0x38), por eso está aquí una sola vez. El juego pinta la tesela
         * de PÁGINA 1 (bloque sólido) si su interruptor ya está pulsado y la de página 0
         * (translúcido) si no; en un parse EN FRÍO no hay ninguno pulsado, que es el
         * estado con el que empieza una partida.
         */
        fun switchBlocks(kind: Int) {
            val tiles = intArrayOf(0x6C, 0x6D)
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size shr 4
            do {
                var r2 = r0
                preserve()
                do {
                    setHi00(v1); v1 = horiz(v1, tiles[kind])
                    r2 = (r2 - 1) and 0xFF
                } while (r2 and 0x80 == 0)
                restore(); v1 = vert()
                r1 = (r1 - 1) and 0xFF
            } while (r1 and 0x80 == 0)
        }

        /**
         * Objeto de CASTILLO 0x3E: LÍNEA HORIZONTAL DE PINCHOS
         * (CastleObj3E_HorizontalLineOfSpikes, $0D:C42E). Repite a lo ancho (nibble bajo
         * + 1) una tesela de página 1 que elige el nibble ALTO en la tabla $0D:C427, de
         * la que el juego solo define dos entradas. Fuera de ellas no hay dato: se
         * declara desconocido en vez de pintar basura.
         */
        fun castleSpikeLineH() {
            val tiles = intArrayOf(0x5A, 0x59)
            val variant = size shr 4
            if (variant >= tiles.size) { unkStd(0x3E); return }
            var v1 = pos
            var r0 = size and 0xF
            do {
                setHi01(v1); v1 = horiz(v1, tiles[variant])
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        /**
         * Objeto de CASTILLO 0x3F: LÍNEA VERTICAL DE PINCHOS
         * (CastleObj3F_VerticalLineOfSpikes, $0D:C44F). Baja (nibble alto + 1) teselas de
         * página 1; aquí es el nibble BAJO el que elige entre las tres de $0D:C446.
         */
        fun castleSpikeLineV() {
            val tiles = intArrayOf(0x5B, 0x5C, 0x53)
            val variant = size and 0xF
            if (variant >= tiles.size) { unkStd(0x3F); return }
            var v1 = pos
            var r0 = size shr 4
            do {
                setHi01(v1); setLo(v1, tiles[variant])
                v1 = vert()
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        // ---------------------------- objetos de CUERDA ----------------------------
        // Ports 1:1 de RopeObjXX del banco $0D (tilesets 2/6/8; algunos compartidos
        // con la tabla de castillo).

        /**
         * Objeto 0x3C: SOMBRERO DE SETA (RopeObj3C, $0D:D103): fila página 1 de
         * borde 7 + medio 8 + borde 9; si una punta cae sobre un TALLO ya dibujado
         * (teselas 0x73..0x75) usa la unión 10/11, leyendo el buffer como el juego.
         */
        fun ropeMushroomTop() {
            var r0 = size and 0xF
            val j = pos
            setHi01(j)
            val prev = rdLo(j)
            val first = if (prev in 0x73..0x75) 10 else 7
            var i = horiz(j, first)
            while (true) {
                r0 = (r0 - 1) and 0xFF
                if (r0 == 0) break
                setHi01(i)
                i = horiz(i, 8)
            }
            setHi01(i)
            val prev2 = rdLo(i)
            val last = if (prev2 in 0x73..0x75) 11 else 9
            horiz(i, last)
        }

        /**
         * Objeto 0x3D: TALLO DE SETA (RopeObj3D, $0D:D145): filas página 0 de
         * 0x73 + 0x74… más una tesela 0x75 al final de cada fila.
         */
        fun ropeMushroomColumn() {
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size shr 4
            preserve()
            do {
                var v2 = if (r0 == 0) 256 else r0
                setHi00(v1)
                var i = 0x73
                while (true) {
                    v1 = horiz(v1, i)
                    if (--v2 == 0) break
                    setHi00(v1)
                    i = 0x74
                }
                setHi00(v1)
                horiz(v1, 0x75)
                restore()
                v1 = vert()
                r1 = (r1 - 1) and 0xFF
            } while (r1 and 0x80 == 0)
        }

        /**
         * Objeto 0x32: PUENTE DE TRONCOS (RopeObj32, $0D:D24E): fila superior 0xA3
         * (página 0, la pasarela) y fila inferior 0x0E (página 1, el tronco).
         */
        /**
         * Objeto 0x35 de cuerda: COLUMNA CON PLANTA ENCIMA
         * (RopeObj35_ColumnWithPlantOnTop, $0D:D1D9). De arriba abajo: la PLANTA (dos
         * teselas de página 0, cuatro variantes según el nibble bajo), luego el capitel
         * 0x5F/0x60 y después el fuste, que cicla tres parejas de teselas
         * (0x61..0x66) hasta agotar el alto del nibble alto. Todo de página 1 salvo la
         * planta.
         */
        fun ropeColumnWithPlant() {
            val left = intArrayOf(0x9A, 0x9C, 0x9E, 0xA0)
            val right = intArrayOf(0x9B, 0x9D, 0x9F, 0xA1)
            val col = intArrayOf(0x61, 0x62, 0x63, 0x64, 0x65, 0x66)
            val v2 = size and 0xF
            if (v2 >= left.size) { unkStd(0x35); return }
            var r0 = size shr 4
            val v1 = pos
            preserve()
            setHi00(v1)
            val v3 = horiz(v1, left[v2])
            setHi00(v3); setLo(v3, right[v2])
            r0 = (r0 - 1) and 0xFF
            if (r0 and 0x80 != 0) return
            restore()
            val v4 = vert()
            setHi01(v4)
            val v5 = horiz(v4, 0x5F)
            setHi01(v5); setLo(v5, 0x60)
            r0 = (r0 - 1) and 0xFF
            if (r0 and 0x80 != 0) return
            restore()
            var v6 = vert()
            var v7 = 0
            do {
                setHi01(v6)
                val v8 = horiz(v6, col[v7])
                val v9 = v7 + 1
                setHi01(v8); setLo(v8, col[v9])
                v7 = v9 + 1
                restore()
                v6 = vert()
                if (v7 == 6) v7 = 0
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        fun ropeLogBridge() {
            val tiles = intArrayOf(0xA3, 0x0E)
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size and 0xF
            preserve()
            var v2 = 0
            while (true) {
                setHi00(v1)
                if (v2 == 1) setHi01(v1)
                v1 = horiz(v1, tiles[v2])
                r1 = (r1 - 1) and 0xFF
                if (r1 and 0x80 != 0) {
                    restore()
                    v1 = vert()
                    r1 = r0
                    if (++v2 == 2) break
                }
            }
        }

        /**
         * GUÍA DE LÍNEA horizontal (RopeObj38/CastleObj37, $0D:CF12): fila página 0
         * de la tesela elegida por el nibble alto (0x92/0x93).
         */
        fun ropeHorizontalLineGuide() {
            val tiles = intArrayOf(0x92, 0x93)
            val t = (size shr 4).coerceAtMost(1) // vanilla solo usa 0..1
            var v1 = pos
            var r0 = size and 0xF
            do {
                setHi00(v1)
                v1 = horiz(v1, tiles[t])
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        /**
         * GUÍA DE LÍNEA vertical (CastleObj38/RopeObj39, $0D:CF33): columna página 0
         * de la tesela elegida por el nibble bajo (0x90/0x91/0xA2).
         */
        fun ropeVerticalLineGuide() {
            val tiles = intArrayOf(0x90, 0x91, 0xA2)
            val t = (size and 0xF).coerceAtMost(2) // vanilla solo usa 0..2
            var v1 = pos
            var r0 = size shr 4
            do {
                setHi00(v1)
                setLo(v1, tiles[t])
                v1 = vert()
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        /**
         * Objeto 0x3A: GUÍA DE LÍNEA EN CUESTA (RopeObj3A_SlopedLineGuide, $0D:CF53).
         * El nibble BAJO elige variante en una tabla de SEIS entradas
         * (`kRopeObj3A_SlopedLineGuide_SlopedLineGuidePtrs`), y el ALTO es el número de
         * pasos menos uno. Es el objeto que más niveles bloqueaba: 68 apariciones.
         *
         * Dos formas distintas, no una con parámetro:
         *  - las cuestas NORMALES (0 izquierda, 2 derecha) dibujan DOS teselas por paso
         *    —la guía tiene grosor de dos bloques— y por eso van entre preserve/restore;
         *  - las de ON/OFF (1, 3, 4, 5) dibujan UNA sola tesela con [setLo], sin avanzar
         *    en horizontal, así que no necesitan salvar el puntero.
         *
         * El paso diagonal tampoco es el mismo: la izquierda normal baja +14 (dos
         * columnas a la izquierda), la de ON/OFF +15 (una), y las de la derecha suman
         * +18 y +17. De ahí que cada variante lleve su propio avance en vez de
         * reutilizar [goDownLeft]/[goDownRight] para todas.
         */
        fun ropeSlopedLineGuide() {
            when (val k = size and 0xF) {
                0 -> ropeSlopedLineGuideLeft()
                2 -> ropeSlopedLineGuideRight()
                1, 4 -> ropeSlopedLineGuideOnOffLeft(if (k == 4) 0x94 else 0x86)
                3, 5 -> ropeSlopedLineGuideOnOffRight(if (k == 5) 0x95 else 0x87)
                // La tabla del juego solo tiene 6 punteros: un nibble ≥6 saltaría a
                // basura. Se marca como no portado en vez de inventar un dibujo.
                else -> unkStd(0x3A)
            }
        }

        /** Cuesta IZQUIERDA normal ($0D:CF6E): teselas 0x8C/0x8D, paso diagonal +14. */
        private fun ropeSlopedLineGuideLeft() {
            var v1 = pos
            var v2 = size shr 4
            preserve()
            do {
                setHi00(v1)
                val v3 = horiz(v1, 0x8C)
                setHi00(v3); setLo(v3, 0x8D)
                restore()
                v1 = stepDownLeftBy14()
                v2 = (v2 - 1) and 0xFF
            } while (v2 and 0x80 == 0)
        }

        /** Cuesta DERECHA normal ($0D:CFF0): teselas 0x8E/0x8F, paso diagonal +18. */
        private fun ropeSlopedLineGuideRight() {
            var v1 = pos
            var v2 = size shr 4
            preserve()
            do {
                setHi00(v1)
                val v3 = horiz(v1, 0x8E)
                setHi00(v3); setLo(v3, 0x8F)
                restore()
                // +16 (una fila) y luego +2 (dos columnas). El enmascarado final es
                // `& 0xF1`, no `& 0xF0`: conserva el bit 0 para que la guía siga
                // cayendo en columnas impares al cambiar de pantalla.
                if (pos + 16 >= 256) pageCross(1)
                val v4 = (pos + 16) and 0xFF
                v1 = (v4 + 2) and 0xFF
                if ((v1 and 0xF) < 2) {
                    v1 = (v1 - 16) and 0xF1
                    forwardOneScreen()
                }
                pos = v1
                v2 = (v2 - 1) and 0xFF
            } while (v2 and 0x80 == 0)
        }

        /** Cuesta IZQUIERDA de ON/OFF ($0D:CFB1): una tesela, paso [goDownLeft] (+15). */
        private fun ropeSlopedLineGuideOnOffLeft(tile: Int) {
            var v2 = pos
            var v3 = size shr 4
            do {
                setHi00(v2); setLo(v2, tile)
                v2 = goDownLeft()
                v3 = (v3 - 1) and 0xFF
            } while (v3 and 0x80 == 0)
        }

        /** Cuesta DERECHA de ON/OFF ($0D:D034): una tesela, paso +17. */
        private fun ropeSlopedLineGuideOnOffRight(tile: Int) {
            var v2 = pos
            var v3 = size shr 4
            do {
                setHi00(v2); setLo(v2, tile)
                if (pos + 16 >= 256) pageCross(1)
                val v4 = (pos + 16) and 0xFF
                v2 = (v4 + 1) and 0xFF
                if (v2 and 0xF == 0) {
                    v2 = (v2 - 1) and 0xF0
                    forwardOneScreen()
                }
                pos = v2
                v3 = (v3 - 1) and 0xFF
            } while (v3 and 0x80 == 0)
        }

        /**
         * Paso diagonal ABAJO-IZQUIERDA de DOS columnas (+14). Es el hermano de
         * [goDownLeft] (+15, una columna) para los objetos de guía y lava que avanzan
         * de dos en dos; el umbral del reajuste de pantalla también es 14, no 15.
         */
        fun stepDownLeftBy14(): Int {
            var r = pos + 14
            if (r >= 256) pageCross(1)
            r = r and 0xFF
            if (r and 0xF >= 14) {
                if (r + 16 >= 256) pageCross(1)
                r = (r + 16) and 0xFF
                backOneScreen()
            }
            pos = r
            return r
        }

        // -------------------------- objetos de SUBTERRÁNEO --------------------------
        // Ports 1:1 de UndergroundObjXX del banco $0D (tilesets 3/9/0xA/0xB/0xE).

        /**
         * Objeto 0x3C: CUESTA MUY INCLINADA (UndergroundObj3C_VerySteepSlope,
         * $0D:DD87). El bit 0x10 del tamaño elige lado: 0 = izquierda ($0D:DD99),
         * 1 = derecha ($0D:DE3C).
         *
         * Cada paso dibuja una COLUMNA: tres teselas de cuesta (página 1) y debajo el
         * relleno de tierra 0x3F (página 0), que mengua de dos en dos columnas — por eso
         * la pendiente es de 2 filas por columna. La columna se recorre con [pageCrossRaw]
         * (el `+= 256` a pelo del original, que NO toca el backup) entre preserve y
         * restore; al acabar, el salto a la columna siguiente sí usa las primitivas
         * normales, e incluye el retroceso/avance de pantalla cuando el paso diagonal se
         * sale por el borde.
         */
        fun undergroundVerySteepSlope() {
            val right = size and 0x10 != 0
            val topTiles = if (right) intArrayOf(0xCC, 0xCD, 0xF2) else intArrayOf(0xCA, 0xCB, 0xF1)
            var v0 = pos
            var r0 = size and 0xF
            var r1 = 2 * (size and 0xF) + 2

            fun down(at: Int): Int {
                if (at + 16 >= 256) pageCrossRaw()
                return (at + 16) and 0xFF
            }

            do {
                preserve()
                val v1 = r1
                for (t in topTiles) { setHi01(v0); setLo(v0, t); v0 = down(v0) }
                var v8 = v1 - 2
                while (--v8 >= 0) { setHi00(v0); setLo(v0, 0x3F); v0 = down(v0) }
                restore()
                if (right) {
                    // Paso diagonal a la derecha: dos filas abajo y una columna a la
                    // derecha; si se sale de la pantalla, avanza de pantalla.
                    val s = pos + 32
                    if (s >= 256) pageCross(1)
                    v0 = (s + 1) and 0xFF
                    if (v0 and 0xF == 0) { forwardOneScreen(); v0 = (v0 - 1) and 0xF0 }
                } else {
                    // A la izquierda: dos filas abajo y una columna a la izquierda.
                    val s = pos + 31
                    v0 = s and 0xFF
                    if (s >= 256) pageCross(1)
                    if (v0 and 0xF == 15) {
                        if (v0 + 16 >= 256) pageCross(1)
                        v0 = (v0 + 16) and 0xFF
                        backOneScreen()
                    }
                }
                pos = v0
                r1 -= 2
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        /**
         * Objeto 0x39 de cueva: LAVA EN CUESTA (UndergroundObj39_SlopedCaveLava,
         * $0D:DAF2). Los DOS bits bajos del tamaño eligen entre las cuatro variantes de
         * `kUndergroundObj39_SlopedCaveLava_SlopedCaveLavaPtrs`: 0 izquierda, 1
         * izquierda muy inclinada, 2 derecha, 3 derecha muy inclinada.
         *
         * Las cuatro comparten una idea: por cada paso se dibuja el BORDE diagonal de la
         * lava y, a su lado, el relleno (0xFF) que va creciendo — de ahí el contador
         * `r2`, que en las suaves sube de dos en dos y en las inclinadas de uno en uno.
         * Lo que cambia es cómo se llega a la fila siguiente: las de la IZQUIERDA dan un
         * paso diagonal explícito ([stepDownLeftBy14] o [goDownLeft]), mientras que las
         * de la DERECHA no lo necesitan — acaban la fila de relleno con la tesela de
         * borde y ahí mismo se quedan colocadas para el paso siguiente.
         *
         * Ojo con el final del bucle: el original decrementa `r0` y sale cuando se ha
         * desbordado a 0xFF, no cuando llega a 0. Es lo que hace que se pinte UNA fila de
         * lava de más por debajo de la cuesta, que es justo lo que se ve en el juego.
         */
        /**
         * Objeto 0x37 de cueva: LIENZO GRANDE (UndergroundObj37_LargeCanvas, $0D:DF3A).
         * Es el decorado completo de una sala —el del nivel 0x1D2— y funciona distinto a
         * todo lo demás, en tres fases:
         *
         *  1. Cinco listones horizontales de 16 teselas (0x61, página 1) separados por
         *     cuatro filas, empezando en la fila 5. Es lo único que usa el puntero del
         *     objeto tal cual.
         *  2. OCHO bastidores de lienzo (4 de ancho × 5 de alto) en posiciones FIJAS de
         *     las tablas $0D:DF2A/$0D:DF32. Aquí el original se salta el puntero del
         *     objeto y escribe directamente en $7EC800/$7EC900, o sea siempre en la
         *     pantalla 0, pase donde pase el objeto.
         *  3. El nibble bajo del tamaño dice a cuántas pantallas se REPLICA lo anterior,
         *     copiando la pantalla 0 entera con `MemCpy`.
         *
         * Se conservan dos rarezas del original en vez de "arreglarlas": la copia es de
         * 0x1B1 bytes, uno más que una pantalla (se cuela en la siguiente, que de todas
         * formas se sobrescribe en la vuelta siguiente), y las tres filas centrales del
         * bastidor repiten SIEMPRE las mismas tres teselas porque el índice no avanza.
         */
        fun undergroundLargeCanvas() {
            val canvasTiles = intArrayOf(
                0x5C, 0x5D, 0x5E, 0x60, 0x73, 0x74, 0x75, 0x62, 0x63, 0x64, 0x5F, 0x76, 0x76, 0x76,
            )
            val posLo = intArrayOf(0x50, 0x58, 0x94, 0x9C, 0xD0, 0xD8, 0x14, 0x1C)
            val posHi = intArrayOf(0xC8, 0xC8, 0xC8, 0xC8, 0xC8, 0xC8, 0xC9, 0xC9)
            val veces = size and 0xF

            // --- 1. los cinco listones ---
            preserve()
            pos = 80
            var v1 = pos
            var r1 = 4
            do {
                var v2 = 15
                do { setHi01(v1); v1 = horiz(v1, 0x61); v2-- } while (v2 >= 0)
                restore()
                if (pos + 64 >= 256) pageCross(1)
                pos = (pos + 64) and 0xFF
                v1 = pos
                r1--
            } while (r1 and 0x80 == 0)

            // --- 2. los ocho bastidores, en coordenadas absolutas de la pantalla 0 ---
            for (r0 in 0..7) {
                r1 = 2
                pos = posLo[r0]
                var v4 = pos
                // $C800 → índice 0 del buffer; $C900 → 0x100 (la "página 1" de la pantalla).
                ptr = (posHi[r0] - 0xC8) shl 8
                ptrBak = ptr
                var r2 = 3
                var idx = 0
                do { setHi01(v4); v4 = horiz(v4, canvasTiles[idx++]); r2-- } while (r2 and 0x80 == 0)
                restore()
                var v6 = vert()
                do {
                    // Las teselas 4/5/6 NO avanzan el índice: las tres filas centrales
                    // del bastidor son idénticas, y así es en el original.
                    setHi00(v6)
                    val v7 = horiz(v6, canvasTiles[4])
                    setHi00(v7)
                    val v8 = horiz(v7, canvasTiles[5])
                    setHi00(v8); setLo(v8, canvasTiles[6])
                    v6 = vert()
                    r1--
                } while (r1 and 0x80 == 0)
                var v9 = 7
                r2 = 3
                do { setHi01(v6); v6 = horiz(v6, canvasTiles[v9++]); r2-- } while (r2 and 0x80 == 0)
                restore()
                r2 = 2
                var v10 = vert()
                do { setHi00(v10); v10 = horiz(v10, canvasTiles[v9++]); r2-- } while (r2 and 0x80 == 0)
            }

            // --- 3. réplica de la pantalla 0 en las siguientes ---
            // El original no protege el caso `veces == 0` (se desbordaría y leería la
            // tabla de pantallas fuera de rango); aquí se corta en la última entrada real.
            var quedan = veces
            var pantalla = 1
            while (quedan > 0 && pantalla <= 15) {
                val dst = pantalla * 0x1B0
                for (n in 0 until 0x1B1) {
                    if (dst + n < lo.size && n < lo.size) { lo[dst + n] = lo[n]; hi[dst + n] = hi[n] }
                }
                quedan--
                pantalla++
            }
        }

        fun undergroundSlopedCaveLava() {
            when (size and 3) {
                0 -> undergroundSlopedCaveLavaLeft()
                1 -> undergroundSlopedCaveLavaSteepLeft()
                2 -> undergroundSlopedCaveLavaRight()
                else -> undergroundSlopedCaveLavaSteepRight()
            }
        }

        /** Izquierda suave ($0D:DB06): borde 0xD2/0xD3, relleno 0xFB+0xFF, paso +14. */
        private fun undergroundSlopedCaveLavaLeft() {
            var v0 = pos
            var r2 = 1
            preserve()
            var r0 = (size shr 4) + 1
            var v8: Boolean
            outer@ while (true) {
                val v1 = r2
                setHi01(v0)
                val v2 = horiz(v0, 0xD2)
                setHi01(v2)
                var v3 = horiz(v2, 0xD3)
                var v4 = s8(v1 - 2)
                var saltaRelleno = v4 < 0
                while (true) {
                    if (!saltaRelleno) {
                        setHi01(v3)
                        val v5 = horiz(v3, 0xFB)
                        setHi01(v5)
                        var v6 = horiz(v5, 0xFF)
                        var v7 = s8(v4 - 1)
                        while (--v7 >= 0) { setHi01(v6); v6 = horiz(v6, 0xFF) }
                    }
                    saltaRelleno = false
                    restore()
                    r2 += 2
                    r0 = (r0 - 1) and 0xFF
                    v8 = r0 and 0x80 != 0
                    if (r0 != 0) break
                    v4 = s8(r2 - 2)
                    v3 = vert()
                }
                if (v8) break@outer
                v0 = stepDownLeftBy14()
            }
        }

        /** Izquierda muy inclinada ($0D:DBA4): borde 0xD6, relleno 0xFD+0xFF, paso +15. */
        private fun undergroundSlopedCaveLavaSteepLeft() {
            var j = pos
            var r2 = 0
            preserve()
            var r0 = (size shr 4) + 1
            var v5: Boolean
            outer@ while (true) {
                var v1 = r2
                setHi01(j)
                var i = horiz(j, 0xD6)
                while (true) {
                    var v3 = s8(v1 - 1)
                    if (v3 >= 0) {
                        setHi01(i)
                        var k = horiz(i, 0xFD)
                        while (--v3 >= 0) { setHi01(k); k = horiz(k, 0xFF) }
                    }
                    restore()
                    r2++
                    r0 = (r0 - 1) and 0xFF
                    v5 = r0 and 0x80 != 0
                    if (r0 != 0) break
                    v1 = r2
                    i = vert()
                }
                if (v5) break@outer
                j = goDownLeft()
            }
        }

        /** Derecha suave ($0D:DC02): borde 0xD4/0xD5, relleno 0xFF y remate 0xFC. */
        private fun undergroundSlopedCaveLavaRight() {
            var v0 = pos
            var r2 = 1
            preserve()
            var r0 = (size shr 4) + 1
            do {
                setHi01(v0)
                val v4 = horiz(v0, 0xD4)
                setHi01(v4); horiz(v4, 0xD5)
                restore()
                var v1 = vert()
                r2 += 2
                var v2 = r2
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                while (v2 != 3) { setHi01(v1); v1 = horiz(v1, 0xFF); v2-- }
                setHi01(v1)
                val v3 = horiz(v1, 0xFF)
                setHi01(v3)
                v0 = horiz(v3, 0xFC)
            } while (r0 != 0)
        }

        /** Derecha muy inclinada ($0D:DC61): borde 0xD7, relleno 0xFF y remate 0xFE. */
        private fun undergroundSlopedCaveLavaSteepRight() {
            var v0 = pos
            var r2 = 0
            preserve()
            var r0 = (size shr 4) + 1
            while (r0 != 0) {
                setHi01(v0); horiz(v0, 0xD7)
                restore()
                var v1 = vert()
                r2++
                var v2 = r2
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                while (v2 != 1) { setHi01(v1); v1 = horiz(v1, 0xFF); v2-- }
                setHi01(v1)
                v0 = horiz(v1, 0xFE)
            }
        }

        /**
         * Objeto 0x38 de cueva: BORDE DERECHO DE LAVA (UndergroundObj38_RightLavaEdge,
         * $0D:DAC8). Una tesela de remate arriba y debajo nibble-alto de relleno, todas
         * de página 1; el nibble BAJO elige variante (solo hay dos en las tablas
         * $0D:DAC0/$0D:DAC2, y fuera de ellas no hay dato).
         */
        fun undergroundRightLavaEdge() {
            val top = intArrayOf(0x5A, 0x5B)
            val mid = intArrayOf(0x5B, 0x5B)
            val v2 = size and 0xF
            if (v2 >= top.size) { unkStd(0x38); return }
            var j = pos
            var r0 = size shr 4
            var a = top[v2]
            setHi01(j)
            while (true) {
                setLo(j, a)
                j = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                setHi01(j)
                a = mid[v2]
            }
        }

        /** Objeto 0x36: SUELO DE 4 LADOS (UndergroundObj36, $0D:E135): la plataforma
         *  de cueva, mismo patrón 3×3 que el bloque de piedra con sus tablas. */
        fun underground4SidedGround() =
            rect3x3(intArrayOf(0x45, 0x50, 0x4D), intArrayOf(0x00, 0xF0, 0x4E), intArrayOf(0x48, 0x51, 0x4F))

        /**
         * Objeto 0x3D: SALIENTE DE TECHO ($0D:DCEA): filas de tierra 0x65 (alto =
         * nibble alto) y una última fila de borde 0x4E, página 1. Era el objeto MÁS
         * usado de toda la ROM sin portar (307 apariciones en 24 niveles).
         */
        fun undergroundCeilingLedge() {
            var v1 = pos
            var r0 = size shr 4
            val r1 = size and 0xF
            preserve()
            while (r0 != 0) {
                var v2 = r1
                do {
                    setHi01(v1)
                    v1 = horiz(v1, 0x65)
                    v2--
                } while (v2 >= 0)
                restore()
                v1 = vert()
                r0--
            }
            var v3 = r1
            do {
                setHi01(v1)
                v1 = horiz(v1, 0x4E)
                v3--
            } while (v3 >= 0)
        }

        /**
         * Objeto 0x3E: BORDES DE TECHO ($0D:DD2E): columna de TopTiles[t] (alto =
         * nibble alto) y una tesela final de BottomTiles[t], página 1.
         */
        fun undergroundCeilingEdges() {
            val top = intArrayOf(0x50, 0x50, 0x51, 0x51)
            val bottom = intArrayOf(0x4D, 0x50, 0x4F, 0x51)
            val t = (size and 0xF).coerceAtMost(3) // vanilla solo usa 0..3
            var v1 = pos
            var r0 = size shr 4
            if (r0 != 0) {
                do {
                    setHi01(v1)
                    setLo(v1, top[t])
                    v1 = vert()
                    r0--
                } while (r0 != 0)
            }
            setHi01(v1)
            setLo(v1, bottom[t])
        }

        /** Objeto 0x3F: TIERRA MACIZA ($0D:DD5C): rectángulo de 0x65, página 1. */
        fun undergroundSolidDirt() {
            var v1 = pos
            var r0 = size shr 4
            val r1 = size and 0xF
            preserve()
            do {
                var v2 = r1
                do {
                    setHi01(v1)
                    v1 = horiz(v1, 0x65)
                    v2--
                } while (v2 >= 0)
                restore()
                v1 = vert()
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        /**
         * Objetos 0x3A/0x3B: LAVA DE CUEVA ($0D:DCA9): 0x3A con fila de superficie
         * 0x59 y relleno 0xFF; 0x3B solo el relleno. Todo página 1.
         */
        fun undergroundCaveLava(withSurface: Boolean) {
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size shr 4
            preserve()
            var v3 = r0
            do {
                setHi01(v1)
                v1 = horiz(v1, if (withSurface) 0x59 else 0xFF)
                v3--
            } while (v3 >= 0)
            while (true) {
                restore()
                v1 = vert()
                v3 = r0
                r1 = (r1 - 1) and 0xFF
                if (r1 and 0x80 != 0) break
                do {
                    setHi01(v1)
                    v1 = horiz(v1, 0xFF)
                    v3--
                } while (v3 >= 0)
            }
        }

        /**
         * Objeto extendido 0x4A: PUERTA DE RED GIRATORIA 4×4 (ExtObj4A, $0D:A7C1),
         * con las teselas de la tabla del juego. Como el original, NO toca el byte
         * alto (la puerta se dibuja sobre fondo vacío, página 0).
         */
        fun extClimbingNetDoor() {
            val tiles = intArrayOf(
                0x10, 0x11, 0x11, 0x12, 0x13, 0x0B, 0x0B, 0x15,
                0x13, 0x0B, 0x0B, 0x15, 0x16, 0x17, 0x17, 0x18,
            )
            var v0 = pos
            var v1 = 0
            preserve()
            do {
                var r2 = 3
                do {
                    v0 = horiz(v0, tiles[v1++])
                    r2--
                } while (r2 >= 0)
                restore()
                v0 = vert()
            } while (v1 != 16)
        }

        /**
         * Objeto estándar 0x1F: TUBERÍA VERTICAL FINA / tronco (StdObj1F, $0D:B51F):
         * columna de boca 0x53 + cuerpo 0x54 y remate 0x55, todo página 1. El alto es
         * el nibble alto del byte de tamaño; el juego decrementa un uint8 (0 = 256).
         */
        fun stdSkinnyVerticalPipe() {
            var v1 = pos
            var v2 = (size and 0xF0) shr 4
            if (v2 == 0) v2 = 256
            setHi01(v1)
            var i = 0x53
            while (true) {
                setLo(v1, i)
                v1 = vert()
                if (--v2 == 0) break
                setHi01(v1)
                i = 0x54
            }
            setHi01(v1)
            setLo(v1, 0x55)
        }

        /**
         * Objeto extendido 0x86: CARTEL DE META 2×2 (ExtObj86_GoalSign, $0D:A7E7):
         * teselas 0x66/0x67 arriba y 0x68/0x69 abajo, página 0.
         */
        fun extGoalSign() {
            val tiles = intArrayOf(0x66, 0x67, 0x68, 0x69)
            var v0 = pos
            var v1 = 0
            preserve()
            do {
                do {
                    setHi00(v0)
                    v0 = horiz(v0, tiles[v1++])
                } while (v1 and 1 != 0)
                restore()
                v0 = vert()
            } while (v1 != 4)
        }

        /**
         * Objeto extendido 0x8E: BLOQUE DE INTERRUPTOR AMARILLO (ExtObj8E, $0D:B58D).
         * En frío (interruptor sin pisar; misma convención de "memoria a 0" que
         * [ext1Tile]) es la silueta punteada: tesela 0x6B de página 0
         * (kExtObj8E_YellowSwitchBlock_InactiveTiles[1]).
         */
        fun extSwitchBlockYellow() {
            val v1 = pos
            setHi00(v1)
            setLo(v1, 0x6B)
        }

        /** Objeto extendido 0x87: BLOQUE DE INTERRUPTOR VERDE en frío (tesela 0x6A
         *  de página 0, kExtObj8E_YellowSwitchBlock_InactiveTiles[0]). */
        fun extSwitchBlockGreen() {
            val v1 = pos
            setHi00(v1)
            setLo(v1, 0x6A)
        }

        fun extTopLeftSlope(v2: Int) {
            val v1 = pos
            setHi01(v1)
            val v3 = horiz(v1, EXT_TLSLOPE_LEFT[v2])
            setLo(v3, EXT_TLSLOPE_RIGHT[v2])
            setHi01(v3)
        }

        fun extPurpleTriangle(i: Int) {
            val v1 = pos
            setLo(v1, EXT_TRIANGLE[i]); setHi01(v1)
            val v2 = vert()
            setLo(v2, 0xEB); setHi01(v2)
        }

        fun extMidwayBar() {
            val v1 = (pos - 1) and 0xFF
            setHi00(v1)
            val v2 = horiz(v1, 0x35)
            setHi00(v2); setLo(v2, 0x38)
        }

        fun extDoor(i: Int) {
            val v1 = pos
            setHi00(v1); setLo(v1, EXT_DOOR_TOP[i])
            val v3 = vert()
            setHi00(v3); setLo(v3, EXT_DOOR_BOTTOM[i])
        }

        fun extLargeBush(tiles: IntArray, width: Int, height: Int) {
            var v1 = pos; var r1 = height; var v2 = 0
            preserve()
            do {
                var r2 = width
                do {
                    setHi00(v1)
                    v1 = bushTile(v1, tiles[v2++])
                    r2--
                } while (r2 and 0x80 == 0 && r2 >= 0)
                restore()
                v1 = vert()
                r1--
            } while (r1 >= 0)
        }

        fun bushTile(j: Int, aIn: Int): Int {
            var a = aIn
            if (a == 37) return horizNext(j)
            if (a >= 0x54) {
                val v2 = rdLo(j)
                if (v2 != 37) { if (v2 != 73) a++; a++ }
            }
            return horiz(j, a)
        }

        // --------------------------- objetos estándar ---------------------------

        /** Objetos 01-0E: rectángulo de una tesela repetida (agua, bloques, monedas…). */
        fun stdCoins(k: Int) {
            var v1 = pos
            val r0 = size and 0xF
            var r2 = size and 0xF
            var r1 = size shr 4
            preserve()
            while (true) {
                // k==4 = moneda con memoria de objetos; en frío no hay nada cogido.
                val r12 = STD_1TILE_TILES[k]
                setHi00(v1)
                if (ge8(k, 7)) setHi01(v1)
                v1 = horiz(v1, r12)
                r2 = (r2 - 1) and 0xFF
                if (r2 and 0x80 != 0) {
                    restore()
                    v1 = vert()
                    r2 = r0
                    r1 = (r1 - 1) and 0xFF
                    if (r1 and 0x80 != 0) break
                }
            }
        }

        fun stdVerticalPipes() {
            var v1 = pos
            var r0 = size shr 4
            val v2 = size and 0xF
            preserve()
            fun drawSeg(at: Int): Int {
                // LABEL_3/5: v2==5 tubería invisible; resto, tramo normal.
                setHi01(at)
                val l = if (v2 == 5) 0x68 else 0x35
                val r = if (v2 == 5) 0x69 else 0x36
                val n = horiz(at, l); setHi01(n); setLo(n, r)
                return n
            }
            if (!ge8(v2, 3)) {
                // Tapa superior (tipos 0-2).
                setHi01(v1)
                val v3 = horiz(v1, PIPE_V_TOP_L[v2]); setHi01(v3)
                setLo(v3, PIPE_V_TOP_R[v2])
            } else {
                drawSeg(v1) // entra directo en LABEL_3 sin bajar fila
            }
            while (true) {
                restore(); v1 = vert()
                if (v2 != 5 && ge8(v2, 2)) {
                    // Tipos 2-4: tramo hasta agotar r0 y tapa inferior al final.
                    r0 = (r0 - 1) and 0xFF
                    if (r0 == 0) break
                    setHi01(v1)
                    val v5 = horiz(v1, 0x35); setHi01(v5); setLo(v5, 0x36)
                } else {
                    // Tipos 0,1,5: tramos hacia abajo hasta agotar r0 (sin tapa final).
                    r0 = (r0 - 1) and 0xFF
                    if (r0 and 0x80 != 0) return
                    drawSeg(v1)
                }
            }
            setHi01(v1)
            val v6 = horiz(v1, PIPE_V_BOT_L[v2]); setHi01(v6)
            setLo(v6, PIPE_V_BOT_R[v2])
        }

        fun stdHorizontalPipes() {
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size and 0xF
            var v2 = (size and 0xF0) shr 3
            preserve()
            var guard = 0
            outer@ while (guard++ < 512) {
                var gotoL10 = false
                if (!ge8(v2, 4)) {
                    // Extremo primero y tramos hasta agotar r1 (goto LABEL_10 al acabar).
                    setHi01(v1)
                    v1 = horiz(v1, PIPE_H_END[v2])
                    r1 = (r1 - 1) and 0xFF
                    if (r1 and 0x80 != 0) gotoL10 = true
                    while (!gotoL10) {
                        setHi01(v1)
                        v1 = horiz(v1, PIPE_H_SHAFT[v2])
                        r1 = (r1 - 1) and 0xFF
                        if (r1 and 0x80 != 0) gotoL10 = true
                    }
                } else {
                    // Un tramo por vuelta; el extremo se pinta cuando r1 llega a 0.
                    setHi01(v1)
                    v1 = horiz(v1, PIPE_H_SHAFT[v2])
                }
                if (!gotoL10) {
                    r1 = (r1 - 1) and 0xFF
                    if (r1 != 0) continue@outer
                    setHi01(v1); setLo(v1, PIPE_H_END[v2])
                }
                // LABEL_10: segunda fila del tubo (los tubos son de 2 de alto).
                r1 = r0
                restore(); v1 = vert()
                v2 = (v2 + 1) and 0xFF
                if (v2 and 1 == 0) break
            }
        }

        fun stdBulletShooter() {
            var v1 = pos
            val v2 = size shr 4
            setHi01(v1); setLo(v1, 0x41)
            var v3 = vert()
            var i = v2 - 1
            if (i >= 0) {
                setHi01(v3); setLo(v3, 0x42)
                var v5 = vert()
                i--
                while (i >= 0) {
                    setHi01(v5); setLo(v5, 0x43)
                    v5 = vert()
                    i--
                }
            }
        }

        fun stdSlopes() {
            var i = size and 0xF
            while (ge8(i, 10)) i -= 10
            when (i) {
                0 -> slopeLeft()
                1 -> slopeSteepLeft()
                2 -> slopeGradualLeft()
                3 -> slopeRight()
                4 -> slopeSteepRight()
                5 -> slopeGradualRight()
                6 -> slopeUpsideDownNormalLeft()
                7 -> slopeUpsideDownNormalRight()
                8 -> slopeUpsideDownSteepLeft()
                9 -> slopeUpsideDownSteepRight()
                else -> unknown++ // (i queda en 0..9 tras el módulo; else nunca ocurre)
            }
        }

        fun fillSlopeAir(j: Int, aIn: Int): Int {
            var a = aIn
            var v2 = 2
            val v3 = rdLo(j)
            while (v3 != SLOPE_AIR_MATCH[v2]) {
                v2--
                if (v2 and 0x80 != 0) return horiz(j, a)
            }
            a += SLOPE_AIR_ADD[v2]
            return horiz(j, a)
        }

        fun slopeLeft() {
            var v0 = pos
            var r2 = 1
            preserve()
            var r0 = (size shr 4) + 1
            while (true) {
                var v1 = r2
                setHi01(v0)
                val v2 = fillSlopeAir(v0, 0x96)
                setHi01(v2)
                var v3 = fillSlopeAir(v2, 0x9B)
                var v4 = v1 - 2
                var exited: Boolean
                while (true) {
                    if (v4 >= 0) {
                        setHi01(v3)
                        val v5 = horiz(v3, 0xDE)
                        setHi01(v5)
                        var v6 = horiz(v5, 0xE6)
                        var v7 = v4 - 1
                        while (--v7 >= 0) { setHi00(v6); v6 = horiz(v6, 0x3F) }
                    }
                    restore()
                    r2 += 2
                    r0 = (r0 - 1) and 0xFF
                    exited = r0 and 0x80 != 0
                    if (r0 != 0) break
                    v4 = r2 - 2
                    v3 = vert()
                }
                if (exited) break
                var v0n = pos + 14
                if (v0n >= 256) pageCross(1)
                v0n = v0n and 0xFF
                if (ge8(v0n and 0xF, 14)) {
                    if (v0n + 16 >= 256) pageCross(1)
                    v0n = (v0n + 16) and 0xFF
                    backOneScreen()
                }
                pos = v0n; v0 = v0n
            }
        }

        fun slopeSteepLeft() {
            var v0 = pos
            var r0 = (size shr 4) + 1
            var r2 = 0
            preserve()
            var exited: Boolean
            while (true) {
                var v1 = r2
                setHi01(v0)
                var i = fillSlopeAir(v0, 0xAA)
                while (true) {
                    var v3 = v1 - 1
                    if (v3 >= 0) {
                        setHi01(i)
                        var j = horiz(i, 0xE2)
                        while (--v3 >= 0) { setHi00(j); j = horiz(j, 0x3F) }
                    }
                    restore()
                    r2++
                    r0 = (r0 - 1) and 0xFF
                    exited = r0 and 0x80 != 0
                    if (r0 != 0) break
                    v1 = r2
                    i = vert()
                }
                if (exited) break
                var v0n = pos + 15
                if (v0n >= 256) pageCross(1)
                v0n = v0n and 0xFF
                if (v0n and 0xF == 15) {
                    if (v0n + 16 >= 256) pageCross(1)
                    v0n = (v0n + 16) and 0xFF
                    backOneScreen()
                }
                pos = v0n; v0 = v0n
            }
        }

        fun slopeGradualLeft() {
            var v0 = pos
            var r2 = 3
            preserve()
            var r0 = (size shr 4) + 1
            var exited: Boolean
            while (true) {
                var v1 = r2
                setHi01(v0)
                val a = fillSlopeAir(v0, 0x6E); setHi01(a)
                val b = fillSlopeAir(a, 0x73); setHi01(b)
                val c = fillSlopeAir(b, 0x78); setHi01(c)
                var v5 = fillSlopeAir(c, 0x7D)
                var v6 = v1 - 4
                while (true) {
                    if (v6 >= 0) {
                        setHi01(v5)
                        val d = horiz(v5, 0xD8); setHi01(d)
                        val e = horiz(d, 0xDA); setHi01(e)
                        val f = horiz(e, 0xE6); setHi01(f)
                        var g = horiz(f, 0xE6)
                        var v11 = v6 - 3
                        while (--v11 >= 0) { setHi00(g); g = horiz(g, 0x3F) }
                    }
                    restore()
                    r2 += 4
                    r0 = (r0 - 1) and 0xFF
                    exited = r0 and 0x80 != 0
                    if (r0 != 0) break
                    v6 = r2 - 4
                    v5 = vert()
                }
                if (exited) break
                var v0n = pos + 12
                if (v0n >= 256) pageCross(1)
                v0n = v0n and 0xFF
                if (ge8(v0n and 0xF, 12)) {
                    if (v0n + 16 >= 256) pageCross(1)
                    v0n = (v0n + 16) and 0xFF
                    backOneScreen()
                }
                pos = v0n; v0 = v0n
            }
        }

        fun slopeRight() {
            var v0 = pos
            var r2 = 1
            preserve()
            var r0 = (size shr 4) + 1
            do {
                setHi01(v0)
                val v4 = fillSlopeAir(v0, 0xA0)
                setHi01(v4)
                fillSlopeAir(v4, 0xA5)
                restore()
                var v1 = vert()
                r2 += 2
                var v2 = r2
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                while (v2 != 3) { setHi00(v1); v1 = horiz(v1, 0x3F); v2-- }
                setHi01(v1)
                val v3 = horiz(v1, 0xE6)
                setHi01(v3)
                v0 = horiz(v3, 0xE0)
            } while (r0 != 0)
        }

        fun slopeSteepRight() {
            var v0 = pos
            var r2 = 0
            preserve()
            var r0 = (size shr 4) + 1
            while (r0 != 0) {
                setHi01(v0)
                fillSlopeAir(v0, 0xAF)
                restore()
                var v1 = vert()
                var v2 = ++r2
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                while (v2 != 1) { setHi00(v1); v1 = horiz(v1, 0x3F); v2-- }
                setHi01(v1)
                v0 = horiz(v1, 0xE4)
            }
        }

        fun slopeGradualRight() {
            var v0 = pos
            var r2 = 3
            preserve()
            var r0 = (size shr 4) + 1
            do {
                setHi01(v0)
                val a = fillSlopeAir(v0, 0x82); setHi01(a)
                val b = fillSlopeAir(a, 0x87); setHi01(b)
                val c = fillSlopeAir(b, 0x8C); setHi01(c)
                fillSlopeAir(c, 0x91)
                restore()
                var v1 = vert()
                r2 += 4
                var v2 = r2
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                while (v2 != 7) { setHi00(v1); v1 = horiz(v1, 0x3F); v2-- }
                setHi01(v1)
                val v3 = horiz(v1, 0xE6); setHi01(v3)
                val v4 = horiz(v3, 0xE6); setHi01(v4)
                val v5 = horiz(v4, 0xDB); setHi01(v5)
                v0 = horiz(v5, 0xDC)
            } while (r0 != 0)
        }

        // --------------------- cuestas invertidas (techos en pendiente) ---------------------
        // Port 1:1 de StdObj12_Slopes_UpsideDown* ($0D:AE6D..AFEA, smw_0d.c). Ojo: los
        // dos "izquierda" avanzan de pantalla a mano (no con vert()) y difieren entre sí:
        // el normal usa Entry2(0) (no toca ptr), el steep usa 0DAFDF(0) (ptr -= 0x100).

        fun slopeUpsideDownNormalLeft() {
            var v0 = pos
            var r0 = size shr 4
            var r2 = (2 * (size shr 4) - 1) and 0xFF
            var r1 = 0
            var v1 = s8(r2)
            preserve()
            label3@ while (true) {
                setHi01(v0)
                val v4 = horiz(v0, 0xEE)
                setHi01(v4)
                var v5 = horiz(v4, 0xF0)
                var v6 = v1 - 1
                while (--v6 >= 0) { setHi01(v5); v5 = horiz(v5, 0x65) }
                while (true) {
                    restore()
                    var v7: Int
                    if (r1 != 0) {
                        r2 = (r2 - 2) and 0xFF
                        val raw = pos + 18
                        if (raw >= 256) pageCross(1)
                        v7 = raw and 0xFF
                        if (!ge8(v7 and 0xF, 2)) {
                            val v8 = v7 >= 0x10
                            v7 = (v7 - 16) and 0xFF
                            if (!v8) pageCross(0) // Entry2(0): sincroniza ptrBak, no mueve ptr
                            forwardOneScreen()
                        }
                    } else {
                        r1++
                        v7 = vert()
                    }
                    pos = v7
                    r0 = (r0 - 1) and 0xFF
                    if (r0 and 0x80 != 0) return
                    val v2 = r2
                    setHi01(v7)
                    val v3 = horiz(v7, 0xC6)
                    setHi01(v3)
                    v0 = horiz(v3, 0xC7)
                    v1 = s8((v2 - 2) and 0xFF)
                    if (v1 >= 0) continue@label3
                }
            }
        }

        fun slopeUpsideDownNormalRight() {
            var v0 = pos
            var r0 = size shr 4
            var r2 = (2 * (size shr 4) + 1) and 0xFF
            var r1 = 0
            preserve()
            var v1 = r2
            do {
                while (s8(v1 - 4) >= 0) {
                    setHi01(v0); v0 = horiz(v0, 0x65); v1 = (v1 - 1) and 0xFF
                }
                // if ((int8)(v1-2) < 0 || (pinta F0/EF, valor = r1)) → pinta C8/C9.
                val cond: Boolean
                if (s8(v1 - 2) < 0) {
                    cond = true
                } else {
                    setHi01(v0)
                    val v2 = horiz(v0, 0xF0)
                    setHi01(v2)
                    v0 = horiz(v2, 0xEF)
                    cond = r1 != 0
                }
                if (cond) {
                    setHi01(v0)
                    val v3 = horiz(v0, 0xC8)
                    setHi01(v3)
                    horiz(v3, 0xC9)
                }
                restore()
                r2 = (r2 - 2) and 0xFF
                v1 = r2
                r1++
                v0 = vert()
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        fun slopeUpsideDownSteepLeft() {
            var v0 = pos
            var r0 = size shr 4
            var r2 = ((size shr 4) - 1) and 0xFF
            var r1 = 0
            var v1 = s8(((size shr 4) - 1) and 0xFF)
            preserve()
            label3@ while (true) {
                setHi01(v0)
                var i = horiz(v0, 0xEC)
                while (--v1 >= 0) { setHi01(i); i = horiz(i, 0x65) }
                while (true) {
                    restore()
                    var v4: Int
                    if (r1 != 0) {
                        r2 = (r2 - 1) and 0xFF
                        val raw = pos + 17
                        if (raw >= 256) pageCross(1)
                        v4 = raw and 0xFF
                        if (!ge8(v4 and 0xF, 1)) {
                            val v5 = v4 >= 0x10
                            v4 = (v4 - 16) and 0xFF
                            if (!v5) { ptr -= 256; ptrBak = ptr } // 0DAFDF(0): ptr -= una página
                            forwardOneScreen()
                        }
                    } else {
                        r1++
                        v4 = vert()
                    }
                    pos = v4
                    r0 = (r0 - 1) and 0xFF
                    if (r0 and 0x80 != 0) return
                    val v2 = r2
                    setHi01(v4)
                    v0 = horiz(v4, 0xC4)
                    v1 = s8((v2 - 1) and 0xFF)
                    if (v1 >= 0) continue@label3
                }
            }
        }

        fun slopeUpsideDownSteepRight() {
            var v0 = pos
            var r0 = size shr 4
            var r2 = size shr 4
            var r1 = 0
            preserve()
            var v1 = r2
            do {
                while (s8(v1 - 2) >= 0) {
                    setHi01(v0); v0 = horiz(v0, 0x65); v1 = (v1 - 1) and 0xFF
                }
                val cond: Boolean
                if (s8(v1 - 1) < 0) {
                    cond = true
                } else {
                    setHi01(v0)
                    v0 = horiz(v0, 0xED)
                    cond = r1 != 0
                }
                if (cond) {
                    setHi01(v0)
                    horiz(v0, 0xC5)
                }
                restore()
                r2 = (r2 - 1) and 0xFF
                v1 = r2
                r1++
                v0 = vert()
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        fun stdGroundEdges() {
            var v1 = pos
            var r0 = size shr 4
            val v2 = size and 0xF
            setHi00(v1)
            if (ge8(v2, 3)) setHi01(v1)
            val v3 = edgeTop(v2, v1, EDGE_TOP[v2])
            setLo(v1, v3)
            var v4 = vert()
            r0 = (r0 - 1) and 0xFF
            if (r0 and 0x80 == 0) {
                setHi00(v4)
                if (ge8(v2, 9) || (!ge8(v2, 7) && ge8(v2, 3))) setHi01(v4)
                val v5 = edgeMiddle(v2, v4, EDGE_MID1[v2])
                setLo(v4, v5)
                v4 = vert()
                r0 = (r0 - 1) and 0xFF
                while (r0 and 0x80 == 0) {
                    setHi00(v4)
                    if (ge8(v2, 9) || (!ge8(v2, 7) && ge8(v2, 3))) setHi01(v4)
                    val v6 = edgeMiddle(v2, v4, EDGE_MID2[v2])
                    setLo(v4, v6)
                    v4 = vert()
                    r0 = (r0 - 1) and 0xFF
                }
            }
            if (ge8(v2, 11)) {
                setHi01(v4)
                setLo(v4, EDGE_BOTTOM[v2])
            }
        }

        fun edgeTop(k: Int, j: Int, aIn: Int): Int {
            var a = aIn
            if ((!ge8(k, 3) || !ge8(k, 9) || ge8(k, 11)) && k != 2) {
                var v3 = 17
                val v4 = rdLo(j)
                do {
                    if (v4 == EDGE_D0F0[v3]) { setHi01(j); return EDGE_D102[v3] }
                    v3--
                } while (v3 and 0x80 == 0)
                if (v4 != 37 && (a == 1 || a == 3 || a == 69 || a == 72)) a++
            }
            return a
        }

        fun edgeMiddle(k: Int, j: Int, a: Int): Int {
            if ((!ge8(k, 3) || (ge8(k, 7) && !ge8(k, 9))) && k != 2) {
                var v3 = 29
                val v4 = rdLo(j)
                while (v4 != EDGE_D15C[v3]) {
                    v3--
                    if (v3 and 0x80 != 0) return a
                }
                setHi01(j)
                return EDGE_D17A[v3]
            }
            return a
        }

        /** Suelo con hierba: fila superior = bloque 0x100, debajo tierra 0x3F. */
        fun stdLedge(kIn: Int, r2In: Int) {
            val r0 = kIn
            var r2 = r2In
            preserve()
            var v1 = pos
            var k = kIn
            do {
                setHi01(v1)
                v1 = horiz(v1, 0)
                k = (k - 1) and 0xFF
            } while (k != 0xFF)
            while (true) {
                restore()
                var v2 = vert()
                var v3 = r0
                r2 = (r2 - 1) and 0xFF
                if (r2 and 0x80 != 0) break
                do {
                    setHi00(v2)
                    v2 = horiz(v2, 0x3F)
                    v3 = (v3 - 1) and 0xFF
                } while (v3 != 0xFF)
            }
        }

        fun stdMidwayGoal() {
            var v1 = pos
            val r2 = size and 0xF
            val r0 = size shr 4
            var r1 = size shr 4
            preserve()
            for (i in 0 until 3) {
                var r3 = if (r2 != 0) MIDWAY_TOP_GOAL[i] else MIDWAY_TOP[i]
                setHi00(v1); setLo(v1, r3)
                if (v1 + 16 >= 256) ptr += 256
                var v4 = (v1 + 16) and 0xFF
                r1 = (r1 - 1) and 0xFF
                if (r1 != 0) {
                    do {
                        r3 = if (r2 != 0) MIDWAY_MID_GOAL[i] else MIDWAY_MID[i]
                        setHi00(v4); setLo(v4, r3)
                        if (v4 + 16 >= 256) ptr += 256
                        v4 = (v4 + 16) and 0xFF
                        r1 = (r1 - 1) and 0xFF
                    } while (r1 != 0)
                }
                r3 = if (r2 != 0) MIDWAY_BOT_GOAL[i] else MIDWAY_BOT[i]
                setHi00(v4); setLo(v4, r3)
                restore()
                var p = (pos + 1) and 0xFF
                if (p and 0xF == 0) { forwardOneScreen(); p = pos and 0xF0 }
                pos = p; v1 = p
                r1 = r0
            }
        }

        fun stdPurpleCoins() {
            var v1 = pos
            val r0 = size and 0xF
            var v2 = size and 0xF
            var r1 = size shr 4
            preserve()
            while (true) {
                setHi00(v1)
                v1 = horiz(v1, 44)
                if (--v2 < 0) {
                    restore()
                    v1 = vert()
                    v2 = r0
                    r1 = (r1 - 1) and 0xFF
                    if (r1 and 0x80 != 0) break
                }
            }
        }

        fun stdRopeCloudLine() {
            var v1 = pos
            var r0 = size and 0xF
            val v2 = size shr 4
            do {
                setHi01(v1)
                v1 = horiz(v1, ROPE_CLOUD[v2])
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        fun stdDonutBridge() {
            var v1 = pos
            val r0 = size and 0xF
            var r1 = size and 0xF
            preserve()
            var v2 = 0
            while (true) {
                setHi00(v1)
                if (v2 != 0) setHi01(v1)
                v1 = horiz(v1, DONUT[v2])
                r1 = (r1 - 1) and 0xFF
                if (r1 and 0x80 != 0) {
                    restore()
                    v1 = vert()
                    r1 = r0
                    if (++v2 == 2) break
                }
            }
        }

        // --------------------------- objetos de pradera ---------------------------

        fun grassSmallBushes() {
            var v1 = pos
            var r0 = size and 0xF
            val v2 = size shr 4
            setHi00(v1)
            var tile = BUSH_LEFT[v2]
            while (true) {
                v1 = horiz(v1, tile)
                if (--r0 == 0) break
                setHi00(v1)
                tile = BUSH_MID[v2]
            }
            setHi00(v1)
            setLo(v1, BUSH_RIGHT[v2])
        }

        fun grassTopCloudFringe() {
            var v1 = pos
            var r0 = size and 0xF
            val v2 = size shr 4
            do {
                setHi00(v1)
                v1 = horiz(v1, CLOUD_TOP[v2])
                r0 = (r0 - 1) and 0xFF
            } while (r0 and 0x80 == 0)
        }

        fun grassSideCloudFringes() {
            var v1 = pos
            var r0 = size shr 4
            val v2 = size and 0xF
            setHi00(v1)
            var tile = CLOUD_SIDE_TOP[v2]
            while (true) {
                setLo(v1, tile)
                v1 = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                setHi00(v1)
                tile = CLOUD_SIDE_BOT[v2]
            }
        }

        fun diagDirt(j: Int, aIn: Int): Int {
            var a = aIn
            val v2 = rdLo(j)
            if (v2 != 37) { if (v2 != 63) a++; a++ }
            return horiz(j, a)
        }

        /**
         * Objeto 0x30 de pradera: TUBERÍA VERTICAL HELADA (GrassObj30_IcyVerticalPipe,
         * $0D:BB2C). Tubería de 2 columnas y página 1: boca 0x61/0x62 arriba y, debajo,
         * nibble-alto filas de cuerpo 0x63/0x64. La 2ª columna de cada fila se escribe
         * SIN avanzar (solo `SetMap16LowByte`), como en el original.
         */
        fun grassIcyVerticalPipe() {
            var v2 = size shr 4
            val v1 = pos
            preserve()
            setHi01(v1)
            val v3 = horiz(v1, 0x61)
            setHi01(v3); setLo(v3, 0x62)
            while (true) {
                restore()
                val v5 = vert()
                v2--
                if (v2 < 0) break
                setHi01(v5)
                val v4 = horiz(v5, 0x63)
                setHi01(v4); setLo(v4, 0x64)
            }
        }

        // ----------------------------- BOSQUE (pradera) -----------------------------

        /**
         * Objeto 0x35 de pradera: SUELO DE BOSQUE (GrassObj35_ForestGround, $0D:BA0A).
         * Fila superior de copa (tesela 0x0E, página 1) de ancho nibble-bajo + 1, y
         * debajo nibble-alto filas de relleno (tesela 0xB8, página 0).
         */
        fun grassForestGround() {
            var v1 = pos
            val r0 = size and 0xF
            var v2 = size and 0xF
            var r1 = size shr 4
            preserve()
            do { setHi01(v1); v1 = horiz(v1, 0x0E); v2-- } while (v2 >= 0)
            while (true) {
                restore()
                var v3 = vert()
                var v4 = r0
                r1 = (r1 - 1) and 0xFF
                if (r1 and 0x80 != 0) break
                do { setHi00(v3); v3 = horiz(v3, 0xB8); v4-- } while (v4 >= 0)
            }
        }

        /**
         * Objeto 0x34 de pradera: BORDES DEL SUELO DE BOSQUE
         * (GrassObj34_ForestGroundEdges, $0D:BA4C). Columna de alto nibble-alto: una
         * tesela de remate arriba y el resto de relleno, ambas elegidas por el nibble
         * BAJO (4 variantes: los dos lados del suelo y sus dos esquinas).
         *
         * Sutileza del original que se replica tal cual: el relleno solo fuerza PÁGINA 1
         * en las variantes 0 y 1; en las otras deja el byte alto como esté (página 0).
         */
        fun grassForestGroundEdges() {
            val top = intArrayOf(0x5F, 0x5E, 0x10, 0x0F)
            val bot = intArrayOf(0x60, 0x5D, 0xC5, 0xC4)
            val v2 = size and 0xF
            if (v2 >= top.size) { unkStd(0x34); return }
            val v1 = pos
            var r0 = size shr 4
            setHi01(v1); setLo(v1, top[v2])
            while (true) {
                val v3 = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                if (v2 < 2) setHi01(v3)
                setLo(v3, bot[v2])
            }
        }

        /**
         * La tesela de un tramo de TRONCO PEQUEÑO ($0D:B997), que depende de lo que YA
         * haya debajo: la variante 1 se encadena con el tronco de al lado (0xB1/0xB6 →
         * la siguiente) y la 0, si cae sobre la copa del bosque (0x0E), se cambia por la
         * de página 1 (0x0D) para que el tronco atraviese la copa en vez de taparla.
         */
        fun smallTrunkTile(k: Int, j: Int, aIn: Int) {
            var a = aIn
            val cur = rdLo(j)
            if (k == 1) {
                if (cur == 0xB1 || cur == 0xB6) a = cur + 1
            } else if (cur == 0x0E) {
                setHi01(j); a = 0x0D
            }
            setLo(j, a)
        }

        /**
         * Objeto 0x37 de pradera: TRONCO PEQUEÑO (GrassObj37_SmallTreeTrunk, $0D:B966).
         * Baja alternando tesela superior e inferior (dos variantes según el nibble
         * bajo) hasta agotar el alto del nibble alto.
         */
        fun grassSmallTreeTrunk() {
            val top = intArrayOf(0xBD, 0xBF)
            val bot = intArrayOf(0xBE, 0xC0)
            val v2 = size and 0xF
            if (v2 >= top.size) { unkStd(0x37); return }
            var v1 = pos
            var r0 = size shr 4
            while (true) {
                setHi00(v1)
                smallTrunkTile(v2, v1, top[v2])
                val v3 = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                setHi00(v3); setLo(v3, bot[v2])
                v1 = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
            }
        }

        /**
         * Objeto 0x33 de pradera: COPA DEL ÁRBOL DE BOSQUE (GrassObj33_ForestTreeTop,
         * $0D:BADC). Mural fijo de 16×6 de la tabla $0D:BA7C, repetido (tamaño + 1)
         * veces.
         *
         * Dos rarezas del original que se replican tal cual:
         * - NO toca el byte alto en ningún momento: la copa se dibuja en la página que
         *   ya hubiera, no fuerza la 0 ni la 1.
         * - Entre repetición y repetición avanza el puntero `0xB0` a pelo. Sumado al
         *   `0x100` que mete el cruce de la fila 16, da los `0x1B0` de UNA PANTALLA:
         *   por eso la copa se repite a lo ancho del bosque.
         */
        fun grassForestTreeTop() {
            val data = intArrayOf(
                0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4,
                0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4,
                0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB4, 0xB5, 0xB3, 0xB5, 0xB3,
                0xB3, 0xB4, 0xB4, 0xB5, 0xB3, 0xB4, 0xB4, 0xB4, 0xB4, 0xB5, 0xB3, 0xB5, 0xB6, 0xB1, 0xB6, 0xB1,
                0xB1, 0xB3, 0xB5, 0xB6, 0xB1, 0xB3, 0xB5, 0xB3, 0xB5, 0xB6, 0xB1, 0xB6, 0x25, 0x25, 0x25, 0x25,
                0x25, 0xB1, 0xB6, 0x25, 0x25, 0xB1, 0xB6, 0xB1, 0xB6, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25,
            )
            var r15 = size
            do {
                var r14 = pos
                var v1 = pos
                var v2 = 0
                var r1 = 5
                preserve()
                do {
                    var r2 = 15
                    do { v1 = horiz(v1, data[v2]); v2++; r2 = (r2 - 1) and 0xFF } while (r2 and 0x80 == 0)
                    restore()
                    if (r14 + 16 >= 256) pageCross(1)
                    r14 = (r14 + 16) and 0xFF
                    v1 = r14
                    r1 = (r1 - 1) and 0xFF
                } while (r1 and 0x80 == 0)
                ptr += 0xB0
                r15 = (r15 - 1) and 0xFF
            } while (r15 and 0x80 == 0)
        }

        /**
         * Una fila del TRONCO GRANDE ($0D:B9F6): escribe la tesela [kIn] y su pareja
         * [kIn]+1. Si debajo está la copa del bosque (0x0E), cambia a la pareja de
         * página 1 (0x0B/0x0C) para que el tronco atraviese la copa en vez de taparla —
         * el mismo truco que el tronco pequeño.
         */
        fun largeTrunkPair(kIn: Int, j: Int) {
            var k = kIn
            if (rdLo(j) == 0x0E) { setHi01(j); k = 0x0B }
            val v2 = horiz(j, k)
            setLo(v2, k + 1)
        }

        /**
         * Objeto 0x36 de pradera: TRONCO GRANDE (GrassObj36_LargeTreeTrunk, $0D:B9C0).
         * Baja de dos en dos columnas alternando la pareja 0xB9/0xBA y la 0xBB/0xBC
         * hasta agotar el alto del nibble alto.
         */
        fun grassLargeTreeTrunk() {
            var v1 = pos
            var r0 = size shr 4
            while (true) {
                preserve()
                largeTrunkPair(0xB9, v1)
                restore()
                val v2 = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                setHi00(v2)
                val v3 = horiz(v2, 0xBB)
                setHi00(v3); setLo(v3, 0xBC)
                restore()
                v1 = vert()
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
            }
        }

        fun grassArchLedge() {
            val r0 = size and 0xF
            var r3 = 3
            var v1 = 0
            preserve()
            var v2 = pos
            var r2 = r0
            var r1 = 2
            do {
                setHi01(v2)
                v2 = horiz(v2, ARCH[v1++])
                r1--
            } while (r1 >= 0)
            r2 = (r2 - 1) and 0xFF
            if (r2 != 0) {
                do {
                    setHi01(v2)
                    val v3 = horiz(v2, ARCH[v1]); setHi01(v3)
                    val v4 = horiz(v3, ARCH[v1 + 1]); setHi01(v4)
                    v2 = horiz(v4, ARCH[v1 + 2])
                    r2 = (r2 - 1) and 0xFF
                } while (r2 != 0)
            }
            var v5 = v1 + 3
            setHi01(v2)
            horiz(v2, ARCH[v5])
            while (true) {
                var v7 = v5 + 1
                restore()
                vert()
                r3--
                if (r3 < 0) break
                var v6 = pos
                r2 = r0
                r1 = 2
                do {
                    setHi00(v6)
                    v6 = horiz(v6, ARCH[v7++])
                    r1--
                } while (r1 >= 0)
                r2 = (r2 - 1) and 0xFF
                if (r2 != 0) {
                    do {
                        setHi00(v6)
                        val v8 = horiz(v6, ARCH[v7]); setHi00(v8)
                        val v9 = horiz(v8, ARCH[v7 + 1]); setHi00(v9)
                        v6 = horiz(v9, ARCH[v7 + 2])
                        r2 = (r2 - 1) and 0xFF
                    } while (r2 != 0)
                }
                v5 = v7 + 3
                setHi00(v6)
                horiz(v6, ARCH[v5])
            }
        }

        /**
         * Objeto de pradera 0x39: TUBERÍA DIAGONAL hacia la derecha
         * (GrassObj39_RightFacingDiagonalPipe, $0D:B73F): crece en diagonal bajando a
         * la izquierda (filas cada vez más anchas), con remate 0xEB. Todo página 1.
         * Port 1:1 incluida la aritmética uint8 de los contadores.
         */
        fun grassDiagonalPipeRight() {
            val tiles = intArrayOf(
                0xC4, 0xC5, 0xC7, 0xEC, 0xED, 0xC6, 0xC7, 0xEE,
                0x59, 0x5A, 0xEF, 0xC7, 0xEE, 0x59, 0x5B, 0x5C,
            )
            var updated = pos
            var r0 = size shr 4
            var r1 = 1
            var v2 = 0
            preserve()
            while (true) {
                var r2 = r1
                do {
                    setHi01(updated)
                    updated = horiz(updated, tiles[v2++])
                    r2 = (r2 - 1) and 0xFF
                } while (r2 and 0x80 == 0)
                restore()
                updated = goDownLeft()
                r1 += 2
                r0 = (r0 - 1) and 0xFF
                if (r0 and 0x80 != 0) break
                if (v2 == 6) {
                    r1--
                    do {
                        r2 = r1
                        do {
                            setHi01(updated)
                            updated = horiz(updated, tiles[v2++])
                            r2 = (r2 - 1) and 0xFF
                        } while (r2 and 0x80 == 0)
                        restore()
                        updated = goDownLeft()
                        if (v2 == 16) v2 = 11
                        r0 = (r0 - 1) and 0xFF
                    } while (r0 and 0x80 == 0)
                    break
                }
            }
            val v3 = horizNext(updated)
            setHi01(v3)
            setLo(v3, 0xEB)
        }

        fun grassDiagonalLedgeLeft() {
            var j = pos
            var r2 = size and 0xF
            var r3 = size shr 4
            preserve()
            setHi01(j)
            j = fillSlopeAir(j, 0xAA)
            diagDirt(j, 0xA1)
            var v7 = 1
            while (true) {
                restore()
                j = goDownLeft()
                v7 += 2 // C: ++r1; v7 = ++r1 → 3, 5, 7…
                r2 = (r2 - 1) and 0xFF
                if (r2 and 0x80 != 0) break
                setHi01(j)
                j = fillSlopeAir(j, 0xAA)
                var v4 = (v7 - 1 - 1) and 0xFF // primer --v4 del while(--v4)
                setHi01(j)
                j = horiz(j, 0xE2)
                while (v4 != 0) { setHi00(j); j = horiz(j, 0x3F); v4 = (v4 - 1) and 0xFF }
                setHi00(j)
                diagDirt(j, 0xA6)
            }
            j = horizNext(j)
            pos = j
            preserve()
            val r1 = v7 - 1
            var v9 = r1
            setHi01(j)
            j = fillSlopeAir(j, 0xF7)
            while (true) {
                v9 = (v9 - 1) and 0xFF
                while (v9 != 0) { setHi00(j); j = horiz(j, 0x3F); v9 = (v9 - 1) and 0xFF }
                setHi00(j)
                j = diagDirt(j, 0xA6)
                restore()
                j = goDownRight()
                v9 = r1
                r3 = (r3 - 1) and 0xFF
                if (r3 and 0x80 != 0) break
                setHi00(j)
                j = diagDirt(j, 0xA3)
            }
        }

        fun grassDiagonalLedgeRight() {
            var r2 = size and 0xF
            var r3 = size shr 4
            var r1 = 1
            preserve()
            val v2 = diagDirt(pos, 0xAF)
            setHi01(v2)
            fillSlopeAir(v2, 0xAF)
            var updated = pos
            while (true) {
                restore()
                updated = goDownLeft()
                r1 += 2 // C: ++r1; v4 = ++r1 → 3, 5, 7…
                r2 = (r2 - 1) and 0xFF
                if (r2 and 0x80 != 0) break
                setHi00(updated)
                var i = diagDirt(updated, 0xA9)
                var vv = (r1 - 1) and 0xFF // while (--v4 != 1)
                while (vv != 1) { setHi00(i); i = horiz(i, 0x3F); vv = (vv - 1) and 0xFF }
                setHi01(i)
                val v5 = horiz(i, 0xE4)
                setHi01(v5)
                fillSlopeAir(v5, 0xAF)
            }
            r1--
            var v7 = (r1 - 1) and 0xFF // while (--v7)
            setHi00(updated)
            var j = diagDirt(updated, 0xA9)
            while (v7 != 0) { setHi00(j); j = horiz(j, 0x3F); v7 = (v7 - 1) and 0xFF }
            setHi01(j)
            diagDirt(j, 0xF9)
            while (true) {
                restore()
                val v11 = goDownLeft()
                r3 = (r3 - 1) and 0xFF
                if (r3 and 0x80 != 0) break
                setHi00(v11)
                var m = diagDirt(v11, 0xA9)
                var v10 = (r1 - 1) and 0xFF
                while (v10 != 0) { setHi00(m); m = horiz(m, 0x3F); v10 = (v10 - 1) and 0xFF }
                setHi00(m)
                diagDirt(m, 0xAC)
            }
        }
    }

    // ------------------------------ tablas de tiles ------------------------------
    // Copiadas 1:1 de smw_0d.c (snesrev/smw).

    private val EXT_1TILE_TILES = intArrayOf(
        0x1f, 0x22, 0x24, 0x42, 0x43, 0x27, 0x29, 0x25, 0x6e, 0x6f, 0x70, 0x71, 0x72, 0x45, 0x46, 0x47,
        0x48, 0x36, 0x37, 0x11, 0x12, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x29, 0x1d,
        0x1f, 0x20, 0x21, 0x22, 0x23, 0x25, 0x26, 0x27, 0x28, 0x2a, 0xde, 0xe0, 0xe2, 0xe4, 0xec, 0xed,
        0x2c, 0x25, 0x2d,
    )
    private val EXT_TLSLOPE_LEFT = intArrayOf(0xd8, 0xdb)
    private val EXT_TLSLOPE_RIGHT = intArrayOf(0xda, 0xdc)
    private val EXT_TRIANGLE = intArrayOf(0xb4, 0xb5)
    private val EXT_DOOR_TOP = intArrayOf(0x1f, 0x27)
    private val EXT_DOOR_BOTTOM = intArrayOf(0x20, 0x28)
    private val EXT_BIG_BUSH = intArrayOf(
        0x25, 0x25, 0x25, 0x4b, 0x4d, 0x4e, 0x25, 0x25, 0x25, 0x25, 0x25, 0x54, 0x49, 0x49, 0x5f, 0x63,
        0x25, 0x25, 0x25, 0x25, 0x57, 0x49, 0x49, 0x52, 0x4a, 0x5d, 0x25, 0x25, 0x5a, 0x49, 0x49, 0x50,
        0x51, 0x4a, 0x60, 0x25, 0x5a, 0x49, 0x49, 0x49, 0x53, 0x4a, 0x4a, 0x4a, 0x63,
    )
    private val EXT_SMALL_BUSH = intArrayOf(
        0x25, 0x25, 0x4b, 0x4c, 0x25, 0x25, 0x25, 0x54, 0x49, 0x5f, 0x63, 0x25,
        0x25, 0x57, 0x49, 0x52, 0x4a, 0x5d, 0x5a, 0x49, 0x49, 0x49, 0x4f, 0x60,
    )
    private val STD_1TILE_TILES = intArrayOf(0x2, 0x21, 0x23, 0x2a, 0x2b, 0x3f, 0x3, 0x13, 0x1e, 0x24, 0x2e, 0x2f, 0x30, 0x32, 0x65)
    private val PIPE_V_TOP_L = intArrayOf(0x33, 0x37, 0x39, 0x0, 0x0)
    private val PIPE_V_TOP_R = intArrayOf(0x34, 0x38, 0x3a, 0x0, 0x0)
    private val PIPE_V_BOT_L = intArrayOf(0x0, 0x0, 0x39, 0x33, 0x37)
    private val PIPE_V_BOT_R = intArrayOf(0x0, 0x0, 0x3a, 0x34, 0x38)
    private val PIPE_H_END = intArrayOf(0x3b, 0x3c, 0x3b, 0x3f, 0x3b, 0x3c, 0x3b, 0x3f)
    private val PIPE_H_SHAFT = intArrayOf(0x3d, 0x3e, 0x3d, 0x3e, 0x3d, 0x3e, 0x3d, 0x3e)
    private val SLOPE_AIR_MATCH = intArrayOf(0x3f, 0x1, 0x3)
    private val SLOPE_AIR_ADD = intArrayOf(0x1, 0x3, 0x4)
    private val EDGE_TOP = intArrayOf(0x40, 0x41, 0x6, 0x45, 0x4b, 0x48, 0x4c, 0x1, 0x3, 0xb6, 0xb7, 0x45, 0x4b, 0x48, 0x4c)
    private val EDGE_MID1 = intArrayOf(0x40, 0x41, 0x6, 0x4b, 0x4b, 0x4c, 0x4c, 0x40, 0x41, 0x4b, 0x4c, 0x4b, 0x4b, 0x4c, 0x4c)
    private val EDGE_MID2 = EDGE_MID1
    private val EDGE_BOTTOM = intArrayOf(0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xe2, 0xe2, 0xe4, 0xe4)
    private val EDGE_D0F0 = intArrayOf(0x7d, 0x7e, 0x82, 0x83, 0x9b, 0x9c, 0xa0, 0xa1, 0xaa, 0xab, 0xaf, 0xb0, 0xd8, 0xdc, 0xde, 0xe0, 0xe2, 0xe4)
    private val EDGE_D102 = intArrayOf(0xb8, 0xb9, 0xba, 0xbb, 0xbc, 0xbd, 0xbe, 0xbf, 0xc0, 0xc1, 0xc2, 0xc3, 0xd9, 0xdd, 0xdf, 0xe1, 0xe3, 0xe5)
    private val EDGE_D15C = intArrayOf(
        0x6e, 0x6f, 0x73, 0x74, 0x78, 0x79, 0x7d, 0x7e, 0x82, 0x83, 0x87, 0x88, 0x8c, 0x8d, 0x91,
        0x92, 0x96, 0x97, 0x9b, 0x9c, 0xa0, 0xa1, 0xa5, 0xa6, 0xaa, 0xab, 0xaf, 0xb0, 0xe2, 0xe4,
    )
    private val EDGE_D17A = intArrayOf(
        0x70, 0x70, 0x75, 0x75, 0x7a, 0x7a, 0x7f, 0x7f, 0x84, 0x84, 0x89, 0x89, 0x8e, 0x8e, 0x93,
        0x93, 0x98, 0x98, 0x9d, 0x9d, 0xa2, 0xa2, 0xa7, 0xa7, 0xac, 0xac, 0xb1, 0xb1, 0xe9, 0xea,
    )
    private val MIDWAY_TOP = intArrayOf(0x2f, 0x25, 0x32)
    private val MIDWAY_MID = intArrayOf(0x30, 0x25, 0x33)
    private val MIDWAY_BOT = intArrayOf(0x31, 0x25, 0x34)
    private val MIDWAY_TOP_GOAL = intArrayOf(0x39, 0x25, 0x3c)
    private val MIDWAY_MID_GOAL = intArrayOf(0x3a, 0x25, 0x3d)
    private val MIDWAY_BOT_GOAL = intArrayOf(0x3b, 0x25, 0x3e)
    private val ROPE_CLOUD = intArrayOf(0x5, 0x6)
    private val DONUT = intArrayOf(0x26, 0x44)
    private val BUSH_LEFT = intArrayOf(0x73, 0x7a, 0x85, 0x88, 0xc3)
    private val BUSH_MID = intArrayOf(0x74, 0x7b, 0x86, 0x89, 0xc3)
    private val BUSH_RIGHT = intArrayOf(0x79, 0x80, 0x87, 0x8e, 0xc3)
    private val CLOUD_TOP = intArrayOf(0x93, 0x9c)
    private val CLOUD_SIDE_TOP = intArrayOf(0x94, 0x8f, 0x9d, 0x98, 0x95, 0x90, 0x9e, 0x99)
    private val CLOUD_SIDE_BOT = intArrayOf(0x8f, 0x8f, 0x98, 0x98, 0x90, 0x90, 0x99, 0x99)
    private val ARCH = intArrayOf(
        0x7, 0xa, 0xa, 0x8, 0xa, 0xa, 0x9, 0x81, 0x82, 0x83, 0x81, 0x82, 0x83, 0x81,
        0x81, 0x25, 0x84, 0x81, 0x25, 0x84, 0x81, 0x81, 0x25, 0x84, 0x81, 0x25, 0x84, 0x81,
    )
}
