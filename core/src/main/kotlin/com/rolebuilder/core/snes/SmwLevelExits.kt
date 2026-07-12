package com.rolebuilder.core.snes

/**
 * CONECTIVIDAD de un nivel de Super Mario World: sus SALIDAS DE PANTALLA (screen
 * exits), la resolución de cada salida a un nivel destino concreto (por entrada
 * principal o por ENTRADA SECUNDARIA) y la posición del PUNTO INTERMEDIO (midway).
 *
 * Es la pieza que permite ENLAZAR los sub-niveles de un nivel importado: una
 * tubería/puerta/borde de pantalla que "lleva a algún sitio" se corresponde con un
 * screen exit, y este apunta a otro nivel (o a una entrada secundaria que a su vez
 * concreta nivel + pantalla/posición de entrada).
 *
 * Todo es Kotlin puro (sin emulador) y port 1:1 del código real del juego
 * (snesrev/smw): `ExtObj00_ScreenExit` ($0D:A512, smw_0d.c) y la resolución de
 * `LoadLevel` ($05:D796, smw_05.c). Los tests van con flujos SINTÉTICOS, sin ROM.
 *
 * ── Formato del SCREEN EXIT (objeto extendido 0x00 del flujo de Layer 1) ──
 * Un objeto extendido son 3 bytes (b0 b1 tipo) + 1 byte EXTRA cuando el tipo es 0x00:
 *   b0 = loadlvl_R10   b1 = loadlvl_R11   tipo = 0x00 (screen exit)   extra = destino
 *   - PANTALLA de la salida        = b0 & 0x1F        (0..31; el bit 4 es pantalla 16-31)
 *   - USA ENTRADA SECUNDARIA       = (b1 >> 1) & 1    (bit 1 de R11)
 *   - bit "propiedades" (LM)       = b1 & 1           (bit alto del destino en Lunar Magic)
 *   - byte EXTRA                   = nº de nivel destino (byte bajo) o nº de entrada
 *                                    secundaria, según el flag anterior
 * (`objeto_extendido == 0` porque b0&0x60==0 y b1>>4==0, así que b1 vale 0..0xF.)
 *
 * El bit ALTO del destino (0x100) en la ROM VANILLA NO viene del dato del nivel: lo
 * pone el submapa del overworld en tiempo de ejecución. Como un nivel y sus
 * sub-niveles viven en la misma mitad, aquí se usa `level and 0x100` como esa mitad
 * (fiel a vanilla para importar un nivel completo). El bit de "propiedades" (que
 * Lunar Magic sí usa como bit alto) se expone aparte por si el llamador lo necesita.
 *
 * ── Tablas de ENTRADAS SECUNDARIAS (banco $05, 512 entradas de 1 byte cada una) ──
 *   $05:F800 → nivel destino (byte bajo)                         [SEC_ENTR_DEST_SNES]
 *   $05:FA00 → Y de entrada: bits0-3 preset Y, 4-5 pos Y L1, 6-7 pos Y L2  [SEC_ENTR_YPOS_SNES]
 *   $05:FC00 → X de entrada: bits5-7 preset X                     [SEC_ENTR_XPOS_SNES]
 *   $05:FE00 → ajuste FG/BG (pantalla/encuadre de entrada): bits0-2  [SEC_ENTR_FGBG_SNES]
 * El nº de entrada secundaria indexa las cuatro tablas; su destino real es
 * `(número & 0x100) | $05F800[número]`.
 *
 * ── MIDWAY ── El punto intermedio/meta es un objeto de Layer 1 (estándar 0x15
 * `StdObj15_MidwayAndGoalPoint`, o extendido 0x46 `ExtObj46_MidwayBar`); su pantalla
 * y columna salen del MISMO flujo que se recorre para los screen exits. En el 0x15,
 * `tamaño & 0xF != 0` marca el poste de META (fin de nivel) frente a la cinta de midway.
 *
 * Solo niveles HORIZONTALES (igual que [SmwLayer1]); para verticales/sin Layer 1 se
 * devuelve null o sin salidas, según el caso.
 */
object SmwLevelExits {

    // -------- direcciones SNES (mapeo LoROM banco $05, idéntico a SmwLevelStartReader) --------
    private const val BANK = 0x05
    private const val SEC_ENTR_DEST_SNES = 0xF800 // → PC 0x2F800: destino (byte bajo)
    private const val SEC_ENTR_YPOS_SNES = 0xFA00 // → PC 0x2FA00: Y de entrada
    private const val SEC_ENTR_XPOS_SNES = 0xFC00 // → PC 0x2FC00: X de entrada
    private const val SEC_ENTR_FGBG_SNES = 0xFE00 // → PC 0x2FE00: FG/BG (pantalla de entrada)
    private const val ENTRANCE_Y_LO_SNES = 0xD730 // presets Y (firma vanilla en +1)

    /** Firma de cordura: el segundo preset de Y de la ROM vanilla es 0x30. */
    private const val VANILLA_Y1 = 0x30
    private const val LEVEL_COUNT = 0x200

    /** Modos de nivel sin objetos de Layer 1 (salas de jefe/especiales): sin salidas. */
    private val MODES_NO_LAYER1 = intArrayOf(9, 11, 16)

    /** VerticalTable de LoadLevelHeader: bit0 = nivel vertical (fuera de alcance). */
    private val VERTICAL_TABLE = intArrayOf(
        0x00, 0x00, 0x80, 0x01, 0x81, 0x02, 0x82, 0x03, 0x83, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80,
    )

    /** Una entrada secundaria ya resuelta a nivel + posición de entrada. */
    data class SecondaryEntrance(
        /** Nº de entrada secundaria (0..0x1FF), índice de las cuatro tablas. */
        val number: Int,
        /** Nivel destino completo: `(number and 0x100) or $05F800[number]`. */
        val destinationLevel: Int,
        /** Preset de X de entrada (0..7), índice en $05:D750/D758. */
        val entranceXIndex: Int,
        /** Preset de Y de entrada (0..15), índice en $05:D730/D740. */
        val entranceYIndex: Int,
        /** Ajuste FG/BG (pantalla/encuadre de entrada, 0..7). */
        val fgBgPosition: Int,
        /** Posición Y de Layer 1 al entrar (0..3). */
        val layer1YPos: Int,
        /** Posición Y de Layer 2 al entrar (0..3). */
        val layer2YPos: Int,
    )

    /** Una salida de pantalla: qué pantalla la dispara y a qué nivel/entrada lleva. */
    data class LevelExit(
        /** Pantalla (0..31) del nivel de origen que activa esta salida. */
        val fromScreen: Int,
        /**
         * Nivel destino final (con la mitad 0x100 del nivel de origen aplicada). Si es
         * una salida secundaria, es el destino ya resuelto por la tabla; -1 si la ROM
         * no cubre la tabla de entradas secundarias.
         */
        val destinationLevel: Int,
        /** true = usa ENTRADA SECUNDARIA; false = entrada principal del nivel destino. */
        val isSecondary: Boolean,
        /** Byte EXTRA crudo: nivel destino (byte bajo) o nº de entrada secundaria. */
        val rawDestinationByte: Int,
        /** Bit "propiedades" (b1 & 1); en Lunar Magic es el bit alto del destino. */
        val propertiesBit: Int,
        /** Entrada secundaria resuelta, o null si es directa (o no resoluble). */
        val secondaryEntrance: SecondaryEntrance?,
    )

    /** Punto intermedio (midway) o poste de meta del nivel, tomado del flujo de Layer 1. */
    data class MidwayPoint(
        /** Pantalla (0..31) donde está el objeto. */
        val screen: Int,
        /** Columna absoluta en teselas de 16px (`screen*16 + columna`). */
        val xTile: Int,
        /** Fila (0..27) en teselas de 16px. */
        val row: Int,
        /** true = poste de META (fin de nivel); false = cinta/barra de midway. */
        val isGoalPost: Boolean,
    )

    /** Conectividad completa de un nivel: sus salidas y su midway. */
    data class LevelConnectivity(
        val level: Int,
        val exits: List<LevelExit>,
        val midway: MidwayPoint?,
    )

    private fun pc(snes: Int, delta: Int): Int = BANK * 0x8000 + (snes and 0x7FFF) + delta

    private fun rd(rom: ByteArray, i: Int): Int = if (i in rom.indices) rom[i].toInt() and 0xFF else 0xFF

    private fun secByte(rom: ByteArray, delta: Int, table: Int, index: Int): Int? {
        val at = pc(table, delta) + index
        return if (at in rom.indices) rom[at].toInt() and 0xFF else null
    }

    /** Resuelve la entrada secundaria [number] (0..0x1FF); null si la ROM no la cubre. */
    private fun resolveSecondary(rom: ByteArray, delta: Int, number: Int): SecondaryEntrance? {
        val dest = secByte(rom, delta, SEC_ENTR_DEST_SNES, number) ?: return null
        val yb = secByte(rom, delta, SEC_ENTR_YPOS_SNES, number) ?: return null
        val xb = secByte(rom, delta, SEC_ENTR_XPOS_SNES, number) ?: return null
        val fb = secByte(rom, delta, SEC_ENTR_FGBG_SNES, number) ?: return null
        return SecondaryEntrance(
            number = number,
            destinationLevel = (number and 0x100) or dest,
            entranceXIndex = (xb shr 5) and 0x7,
            entranceYIndex = yb and 0xF,
            fgBgPosition = fb and 0x7,
            layer1YPos = (yb and 0x30) shr 4,
            layer2YPos = (yb shr 6) and 0x3,
        )
    }

    /**
     * Lee la conectividad del nivel [level] de una ROM de SMW. Devuelve null si el
     * nivel no tiene datos de Layer 1, es vertical, o la ROM no parece SMW vanilla.
     */
    fun read(rom: ByteArray, header: SnesHeader, level: Int): LevelConnectivity? {
        if (level < 0 || level >= LEVEL_COUNT) return null
        val delta = SnesGameRecipes.smwHeaderDelta(header)

        // Firma vanilla: segundo preset de Y == 0x30 (mismo criterio que SmwLevelStartReader).
        val sig = pc(ENTRANCE_Y_LO_SNES, delta) + 1
        if (sig !in rom.indices || (rom[sig].toInt() and 0xFF) != VANILLA_Y1) return null

        val dataStart = SnesGameRecipes.smwLayer1DataPc(rom, delta, level) ?: return null
        val half = level and 0x100

        // ---- LoadLevelHeader: solo el modo (para verticales / sin Layer 1) ----
        val mode = rd(rom, dataStart + 1) and 0x1F
        if (VERTICAL_TABLE[mode] and 1 != 0) return null // verticales: fuera de alcance
        if (mode in MODES_NO_LAYER1) return LevelConnectivity(level, emptyList(), null)

        // ---- Recorrido del flujo de objetos (cabecera de 5 bytes + objetos, fin 0xFF) ----
        var p = dataStart + 5
        var screen = 0
        val exitsByScreen = LinkedHashMap<Int, LevelExit>()
        var midway: MidwayPoint? = null

        if (rd(rom, p) == 0xFF) return LevelConnectivity(level, emptyList(), null)
        var guard = 0
        while (true) {
            if (++guard > 4000 || p + 3 > rom.size) break // flujo corrupto: no inventamos datos
            val h0 = rd(rom, p); val h1 = rd(rom, p + 1); val size = rd(rom, p + 2); p += 3
            val objNum = (h1 shr 4) or ((h0 and 0x60) shr 1)
            if (h0 and 0x80 != 0) screen++ // nueva pantalla (igual que el juego, para TODO objeto)

            if (objNum != 0) {
                // Objeto estándar: solo nos interesa el midway/meta (0x15).
                if (objNum == 0x15 && midway == null) {
                    midway = midwayAt(screen, h0, h1, isGoalPost = (size and 0xF) != 0)
                }
            } else {
                // Objeto extendido: 'size' es el número de objeto extendido.
                when (size) {
                    0x00 -> { // screen exit: consume 1 byte extra (el destino)
                        val destByte = rd(rom, p); p++
                        val fromScreen = h0 and 0x1F
                        val isSecondary = (h1 shr 1) and 1 != 0
                        val propertiesBit = h1 and 1
                        val sec = if (isSecondary) resolveSecondary(rom, delta, half or destByte) else null
                        val dest = if (isSecondary) (sec?.destinationLevel ?: -1) else (half or destByte)
                        exitsByScreen[fromScreen] =
                            LevelExit(fromScreen, dest, isSecondary, destByte, propertiesBit, sec)
                    }
                    0x01 -> { screen = h0 and 0x1F } // screen jump: fija la pantalla actual
                    0x46 -> if (midway == null) { // midway bar extendido
                        midway = midwayAt(screen, h0, h1, isGoalPost = false)
                    }
                }
            }
            if (rd(rom, p) == 0xFF) break
        }
        return LevelConnectivity(level, exitsByScreen.values.toList(), midway)
    }

    private fun midwayAt(screen: Int, h0: Int, h1: Int, isGoalPost: Boolean): MidwayPoint {
        val colInScreen = h1 and 0xF
        val row = (h0 and 0xF) + if (h0 and 0x10 != 0) 16 else 0
        return MidwayPoint(screen, screen * 16 + colInScreen, row, isGoalPost)
    }
}
