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
 * Cubre niveles HORIZONTALES y VERTICALES: la base de cada pantalla sale de la
 * tabla de layout del modo (0x1B0/pantalla horizontal, 0x200/pantalla vertical)
 * y en verticales el juego intercambia los nibbles de posición de cada objeto.
 * Los slopes específicos de nivel vertical (ext 0x91/0x93/0x95) aún no se portan
 * y se cuentan como no reconocidos.
 */
object SmwLayer1 {

    /**
     * Resultado: buffer Map16 del nivel. La base de cada pantalla sale de la TABLA DE LAYOUT
     * ([layout], offsets dentro del buffer, réplica de `kLevelDataLayoutTables_Layer1LoPtrs`):
     * horizontal = 0x1B0 por pantalla; VERTICAL = 0x200 por pantalla (las pantallas se apilan
     * hacia abajo). En [vertical] la rejilla es 16 columnas × (pantallas×32) filas.
     */
    internal class SmwLevelTilemap(
        val lo: ByteArray,
        val hi: ByteArray,
        val screens: Int,
        val tileset: Int,
        val mode: Int,
        val totalObjects: Int,
        val unknownObjects: Int,
        val vertical: Boolean,
        val layout: IntArray,
    ) {
        /** Nº de columnas de la rejilla (horizontal: pantallas×16; vertical: 16). */
        val cols: Int get() = if (vertical) 16 else screens * 16
        /** Nº de filas de la rejilla (horizontal: 27; vertical: pantallas×32). */
        val rows: Int get() = if (vertical) screens * 32 else 27

        /**
         * Bloque Map16 en la rejilla. En horizontal (a=colAbs 0..cols-1, b=fila 0..26); en
         * vertical (a=columna 0..15, b=fila 0..rows-1). Devuelve -1 fuera de rango.
         */
        fun block(a: Int, b: Int): Int {
            val screen: Int; val within: Int; val cc: Int
            if (vertical) { screen = b / 32; within = b % 32; cc = a }
            else { screen = a / 16; within = b; cc = a % 16 }
            if (within !in 0..31 || cc !in 0..15 || screen !in layout.indices) return -1
            val idx = layout[screen] + (if (within >= 16) 256 + (within - 16) * 16 else within * 16) + cc
            if (idx !in lo.indices) return -1
            return (lo[idx].toInt() and 0xFF) or ((hi[idx].toInt() and 0xFF) shl 8)
        }
    }

    /** Tamaño del buffer: cubre 30 pantallas verticales × 0x200 (> 32 × 0x1B0 horizontal). */
    private const val BUF = 0x3E00

    /** Modos de nivel sin objetos de Layer 1 (salas de jefe / especiales). */
    private val MODES_NO_LAYER1 = intArrayOf(9, 11, 16)

    // Tablas de layout de pantallas (offsets dentro del buffer = valor g_ram − 0xC800), copiadas
    // de kLevelDataLayout_* (smw_05.c). Horizontal: 0x1B0/pantalla. Vertical: 0x200/pantalla.
    private val LAYOUT_STD_HORIZ = IntArray(32) { 0x1B0 * it }
    private val LAYOUT_STD_VERT = IntArray(30) { 0x200 * it }
    private val LAYOUT_VERT_L1_HORIZ_L2 =
        IntArray(30) { if (it < 14) 0x200 * it else 0x1B00 + 0x1B0 * (it - 14) }

    /**
     * Tabla de layout de Layer 1 según el modo (de `kLevelDataLayoutTables_Layer1LoPtrs`): los
     * modos 3,4 son L1 vertical con cola horizontal; 7,8,10,13 son L1 totalmente vertical; el
     * resto (0,1,2,5,6,12,14,15,17,30,31) es horizontal. Null = modo sin Layer 1 direccionable.
     */
    private fun l1Layout(mode: Int): IntArray? = when (mode) {
        3, 4 -> LAYOUT_VERT_L1_HORIZ_L2
        7, 8, 10, 13 -> LAYOUT_STD_VERT
        in 18..29 -> null
        else -> LAYOUT_STD_HORIZ
    }

    /** VerticalTable de LoadLevelHeader: bit0 = nivel vertical. */
    private val VERTICAL_TABLE = intArrayOf(
        0x00, 0x00, 0x80, 0x01, 0x81, 0x02, 0x82, 0x03, 0x83, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80,
    )

    /** Parsea el nivel [level]; null si no hay datos, es vertical o modo sin Layer 1. */
    internal fun parse(rom: ByteArray, delta: Int, level: Int): SmwLevelTilemap? {
        val p = SnesGameRecipes.smwLayer1DataPc(rom, delta, level) ?: return null
        return P(rom, p).run()
    }

    /**
     * Una SALIDA DE PANTALLA (ExtObj00) del nivel: cuando el jugador cruza la [screen], el
     * juego lo lleva a [destination] (byte bajo). Si [secondaryEntrance] es true, [destination]
     * es un nº de ENTRADA SECUNDARIA (→ un sublevel, vía la tabla de entradas secundarias);
     * si no, es el byte bajo de un nº de NIVEL destino (el bit alto lo pone el submapa/agua).
     */
    data class ScreenExit(val screen: Int, val destination: Int, val properties: Int) {
        val secondaryEntrance: Boolean get() = (properties shr 1) and 1 != 0
    }

    /** Salidas de pantalla del [level] (grafo nivel→sublevel), leídas del stream de Layer 1. */
    fun screenExits(rom: ByteArray, delta: Int, level: Int): List<ScreenExit> {
        val p = SnesGameRecipes.smwLayer1DataPc(rom, delta, level) ?: return emptyList()
        val machine = P(rom, p)
        machine.run()
        return machine.screenExits
    }

    // =============================== máquina de estado ===============================

    private class P(val rom: ByteArray, dataStart: Int) {
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
        var vertical = false
        var layout = LAYOUT_STD_HORIZ
        val screenExits = ArrayList<ScreenExit>()

        fun rd(i: Int): Int = if (i in rom.indices) rom[i].toInt() and 0xFF else 0xFF

        fun run(): SmwLevelTilemap? {
            // ---- LoadLevelHeader (solo los campos que afectan al Layer 1) ----
            val b0 = rd(data); val b1 = rd(data + 1); val b4 = rd(data + 4)
            screensInLevel = (b0 and 0x1F) + 1
            mode = b1 and 0x1F
            tileset = b4 and 0x0F
            vertical = (VERTICAL_TABLE[mode] and 1) != 0
            if (mode in MODES_NO_LAYER1) return done()
            layout = l1Layout(mode) ?: return null // modo sin Layer 1 direccionable
            data += 5
            // ---- LoadLevelDataObject ----
            if (rd(data) == 0xFF) return done()
            var guard = 0
            do {
                if (++guard > 2000 || data + 3 > rom.size) return null // flujo corrupto
                val h0 = rd(data); val h1 = rd(data + 1); size = rd(data + 2); data += 3
                objNum = (h1 shr 4) or ((h0 and 0x60) shr 1)
                // Niveles VERTICALES: el juego intercambia los nibbles bajos de b0/b1 (la
                // posición) cuando PAIR16(obj,size) >= 2 (todo salvo screen-exit/jump), para
                // que Y/X mapeen al apilado vertical. LoadLevelDataObject ($05:85FF).
                var e0 = h0; var e1 = h1
                if (vertical && ((objNum shl 8) or size) >= 2) {
                    e0 = (h1 and 0x0F) or (h0 and 0xF0)
                    e1 = (h0 and 0x0F) or (h1 and 0xF0)
                }
                pos = ((e0 and 0x0F) shl 4) or (e1 and 0x0F)
                if (h0 and 0x80 != 0) screen++
                nextScreen = screen
                ptr = layout.getOrElse(screen) { BUF } // fuera de tabla: escrituras se descartan
                if (h0 and 0x10 != 0) ptr += 256
                r10 = e0; r11 = e1
                total++
                if (objNum != 0) std(objNum) else ext(size)
            } while (rd(data) != 0xFF)
            return done()
        }

        fun done() = SmwLevelTilemap(lo, hi, screensInLevel, tileset, mode, total, unknown, vertical, layout)

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
            // Los objetos ≥0x30 son ESPECÍFICOS del tileset; solo están portadas las
            // rutinas de pradera (tileset 0). En otros tilesets significan otra cosa:
            // se cuentan como no portados en vez de dibujar basura.
            if (obj >= 0x30 && tileset != 0) { unknown++; return }
            when (obj) {
                in 0x01..0x0E -> stdCoins(obj - 1)
                0x0F -> if ((size and 0xF) < 5) stdVerticalPipes() else unknown++
                0x10 -> if (((size and 0xF0) shr 3) < 7) stdHorizontalPipes() else unknown++
                0x11 -> stdBulletShooter()
                0x12 -> stdSlopes()
                0x13 -> if ((size and 0xF) < 15) stdGroundEdges() else unknown++
                0x14 -> stdLedge(size and 0xF, size shr 4)      // entrada "standard ledge"
                0x15 -> stdMidwayGoal()
                0x16 -> stdPurpleCoins()
                0x17 -> if ((size shr 4) < 2) stdRopeCloudLine() else unknown++
                0x1C -> stdDonutBridge()
                0x21 -> stdLedge(size, 2)                        // wide-scale ground ledge
                0x3A -> grassDiagonalLedgeLeft()
                0x3B -> grassDiagonalLedgeRight()
                0x3C -> grassArchLedge()
                0x3D -> if ((size shr 4) < 2) grassTopCloudFringe() else unknown++
                0x3E -> if ((size and 0xF) < 8) grassSideCloudFringes() else unknown++
                0x3F -> if ((size shr 4) < 5) grassSmallBushes() else unknown++
                else -> unknown++
            }
        }

        fun ext(k: Int) {
            when (k) {
                0x00 -> { // screen exit (ExtObj00): pantalla r10&0x1F → destino = byte extra
                    screenExits.add(ScreenExit(r10 and 0x1F, rd(data), r11))
                    data++
                }
                0x01 -> { screen = r10 and 0x1F; nextScreen = screen } // screen jump
                in 0x10..0x16, in 0x18..0x40 -> ext1Tile(k - 16)
                0x17 -> ext1Tile(0x32)
                0x41 -> extYoshiCoin()
                0x42, 0x43 -> extTopLeftSlope(k - 66)
                0x44, 0x45 -> extPurpleTriangle(k - 68)
                0x46 -> extMidwayBar()
                0x47, 0x48 -> extDoor(k - 71)
                0x82 -> extLargeBush(EXT_BIG_BUSH, 8, 4)
                0x83 -> extLargeBush(EXT_SMALL_BUSH, 5, 3)
                else -> unknown++
            }
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
