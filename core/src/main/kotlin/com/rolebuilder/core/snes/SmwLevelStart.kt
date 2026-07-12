package com.rolebuilder.core.snes

/**
 * Punto de INICIO del jugador en un nivel de Super Mario World, más los cuatro
 * bytes de su cabecera secundaria.
 *
 * Con la colisión y las físicas ya sabemos cómo es el nivel y cómo se mueve Mario;
 * esto dice DÓNDE aparece. SMW deriva la entrada de la cabecera secundaria (cuatro
 * tablas de 512 bytes en $05:F000/F200/F400/F600, un byte por nivel) y de unas
 * tablas de posiciones preset (`LoadLevel`, $05:xxxx, vía snesrev/smw):
 *  - Y de entrada = índice `secHdr0 & 0x0F` en las presets de Y ($05:D730/D740).
 *  - X de entrada = índice `secHdr1 & 0x07` en las presets de X ($05:D750/D758).
 *
 * [startPixelX]/[startPixelY] son la posición preset de entrada en píxeles (para las
 * entradas estándar, relativa a la primera pantalla; una entrada en pantalla
 * avanzada añade el desplazamiento de cámara, que va aparte). [startTileX]/[startTileY]
 * la dan en casillas de 16 px, listas para el motor.
 */
class SmwLevelStart(
    val level: Int,
    /** Los 4 bytes de la cabecera secundaria ($05:F000, F200, F400, F600). */
    val secHeader: IntArray,
    val startPixelX: Int,
    val startPixelY: Int,
) {
    val startTileX: Int get() = startPixelX / 16
    val startTileY: Int get() = startPixelY / 16

    /** Preset de posición FG/BG al entrar (`secHeader1 & 0x38 >> 3`): en qué pantalla
     *  y encuadre arranca la cámara. Las entradas estándar valen 0 (pantalla inicial). */
    val fgBgPositionSetting: Int get() = (secHeader[1].toInt() and 0x38) shr 3

    // --- Campos decodificados de la cabecera secundaria (ver smw_05.c:2412-2434) ---
    /** ¿Nivel VERTICAL? (misc_level_layout_flags bit0 = F600 bit5). Los verticales no
     *  tienen Layer 1 horizontal, por eso su colisión/salidas salen vacías. */
    val vertical: Boolean get() = (secHeader[3] and 0x20) != 0
    /** Banderas de layout (F600 bits 6-5): bit0=vertical, bit1=modo de layout. */
    val layoutFlags: Int get() = (secHeader[3] shr 5) and 0x03
    /** Pantalla/página de entrada (F600 bits 4-0): alto de player X o Y según vertical. */
    val entranceScreen: Int get() = secHeader[3] and 0x1F
    /** ¿Desactiva la intro "no Yoshi" (F600 bit7)? Relacionado con midway. */
    val disableNoYoshiIntro: Boolean get() = (secHeader[3] and 0x80) != 0
    /** Ajuste de Layer 3 (F200 bits 7-6). */
    val layer3Setting: Int get() = (secHeader[1] shr 6) and 0x03
    /** Índice de scroll de Layer 2 (F000 bits 7-4). */
    val layer2ScrollSetting: Int get() = (secHeader[0] shr 4) and 0x0F
    /** Preset de Y de Layer 1 (F400 bits 3-2). */
    val layer1YSetting: Int get() = (secHeader[2] shr 2) and 0x03
    /** Preset de Y de Layer 2 (F400 bits 1-0). */
    val layer2YSetting: Int get() = secHeader[2] and 0x03
}

/**
 * Lee el punto de inicio de un nivel de SMW de la ROM (cabecera secundaria +
 * tablas de posición preset) con el mapeo LoROM. Devuelve null si la ROM no cubre
 * las tablas o no parece SMW vanilla en esas direcciones.
 */
object SmwLevelStartReader {

    private const val SEC_HDR_0 = 0xF000 // $05: Y de entrada + scroll de Layer 2
    private const val SEC_HDR_1 = 0xF200 // $05: X de entrada + posición FG/BG
    private const val SEC_HDR_2 = 0xF400 // $05: posiciones Y de Layer 1/2
    private const val SEC_HDR_3 = 0xF600 // $05: banderas de layout (vertical, Layer 2…)
    private const val ENTRANCE_Y_LO = 0xD730 // $05: 16 presets Y (byte bajo)
    private const val ENTRANCE_Y_HI = 0xD740 // $05: 16 presets Y (byte alto)
    private const val ENTRANCE_X_LO = 0xD750 // $05: 8 presets X (byte bajo)
    private const val ENTRANCE_X_HI = 0xD758 // $05: 8 presets X (byte alto)
    private const val BANK = 0x05
    private const val LEVELS = 0x200 // 512 niveles por tabla de cabecera secundaria

    /** Firma de cordura: el segundo preset de Y de la ROM vanilla es 0x30. */
    private const val VANILLA_Y1 = 0x30

    private fun pc(snesAddr: Int, delta: Int): Int = BANK * 0x8000 + (snesAddr and 0x7FFF) + delta

    fun read(rom: ByteArray, header: SnesHeader, level: Int): SmwLevelStart? {
        if (level < 0 || level >= LEVELS) return null
        val delta = header.headerOffset - 0x7FC0

        fun byteAt(addr: Int, index: Int): Int? {
            val at = pc(addr, delta) + index
            return if (at in rom.indices) rom[at].toInt() and 0xFF else null
        }

        // Presets de entrada.
        val y1 = byteAt(ENTRANCE_Y_LO, 1) ?: return null
        if (y1 != VANILLA_Y1) return null // no parece SMW vanilla

        val secHeader = IntArray(4)
        secHeader[0] = byteAt(SEC_HDR_0, level) ?: return null
        secHeader[1] = byteAt(SEC_HDR_1, level) ?: return null
        secHeader[2] = byteAt(SEC_HDR_2, level) ?: return null
        secHeader[3] = byteAt(SEC_HDR_3, level) ?: return null

        val yIdx = secHeader[0] and 0x0F
        val xIdx = secHeader[1] and 0x07
        val yLo = byteAt(ENTRANCE_Y_LO, yIdx) ?: return null
        val yHi = byteAt(ENTRANCE_Y_HI, yIdx) ?: return null
        val xLo = byteAt(ENTRANCE_X_LO, xIdx) ?: return null
        val xHi = byteAt(ENTRANCE_X_HI, xIdx) ?: return null

        val startY = (yHi shl 8) or yLo
        val startX = (xHi shl 8) or xLo
        return SmwLevelStart(level, secHeader, startX, startY)
    }
}
