package com.rolebuilder.core.snes

/**
 * Punto de INICIO del jugador en un nivel de Super Mario World, más los cuatro
 * bytes de su cabecera secundaria.
 *
 * Con la colisión y las físicas ya sabemos cómo es el nivel y cómo se mueve Mario;
 * esto dice DÓNDE aparece. SMW deriva la entrada de la cabecera secundaria (cuatro
 * tablas de 512 bytes en $05:F000/F200/F400/F600, un byte por nivel) y de unas
 * tablas de posiciones preset (`LoadLevel`, $05:D796, vía snesrev/smw):
 *  - Y de entrada = índice `secHdr0 & 0x0F` en las presets de Y ($05:D730/D740).
 *  - X de entrada = índice `secHdr1 & 0x07` en las presets de X ($05:D750/D758).
 *  - **PANTALLA de entrada = `secHdr3 & 0x1F`** ($05:F600).
 *
 * La pantalla no es un adorno ni "encuadre de cámara": es el byte ALTO de la posición.
 * `LoadLevel` pone primero el preset entero (`player_xpos = PAIR16(D758[xIdx], D750[xIdx])`)
 * y DESPUÉS, ya fuera del reparto principal/secundaria, hace `r1 &= 0x1F` con
 * `r1 = $05:F600[nivel]` y `HIBYTE(player_xpos) = r1`, **pisando** el byte alto del preset.
 * Si el nivel es VERTICAL (`misc_level_layout_flags & 1`, o sea `$05:F600 & 0x20`) ese mismo
 * valor va a `HIBYTE(player_ypos)` en vez de a la X. Por eso [startPixelX] ya viene ABSOLUTA:
 * un nivel cuya entrada está en la pantalla 5 empieza en x = 5*256 + preset, no en x = preset.
 *
 * (La entrada del MIDWAY es otra: `HIBYTE(player_xpos) = $05:F400[nivel] >> 4`, la rama
 * `else` de esa misma comparación. Aquí solo se resuelve la entrada NORMAL.)
 *
 * [startPixelX]/[startPixelY] son la posición de entrada en píxeles absolutos del nivel;
 * [startTileX]/[startTileY] la dan en casillas de 16 px, listas para el motor.
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

    /** PANTALLA de la entrada normal (`$05:F600[nivel] & 0x1F`), ya incluida en la posición. */
    val entranceScreen: Int get() = secHeader[3] and 0x1F

    /** ¿Nivel VERTICAL? `misc_level_layout_flags & 1`, o sea el bit 0x20 de $05:F600. */
    val isVertical: Boolean get() = (secHeader[3] and 0x20) != 0

    /**
     * ACCIÓN de entrada (`secHeader1 & 0x38 >> 3` → `misc_level_header_entrance_settings`,
     * consumida por `InitializeLevelRAM_00A6CC`, $00:A6CC): 0 andando, 1-4 saliendo de una
     * tubería, 5 andando + hielo, 6 disparado hacia arriba, 7 nadando.
     */
    val entranceAction: Int get() = (secHeader[1].toInt() and 0x38) shr 3

    @Deprecated(
        "Nombre erróneo: no es una posición FG/BG, es la ACCIÓN de entrada ($00:A6CC)",
        ReplaceWith("entranceAction"),
    )
    val fgBgPositionSetting: Int get() = entranceAction
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

    /** Bits 0-4 de $05:F600: PANTALLA de la entrada (`r1 &= 0x1F` de `LoadLevel`). */
    private const val SCREEN_MASK = 0x1F

    /** Bit 0x20 de $05:F600: nivel VERTICAL (`misc_level_layout_flags & 1`). */
    private const val VERTICAL_BIT = 0x20

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

        // La PANTALLA ($05:F600 & 0x1F) pisa el byte alto del preset: a la X si el nivel es
        // horizontal, a la Y si es vertical ($05:F600 & 0x20). Ver el KDoc de la clase.
        val screen = secHeader[3] and SCREEN_MASK
        val vertical = (secHeader[3] and VERTICAL_BIT) != 0
        val startY = if (vertical) (screen shl 8) or yLo else (yHi shl 8) or yLo
        val startX = if (vertical) (xHi shl 8) or xLo else (screen shl 8) or xLo
        return SmwLevelStart(level, secHeader, startX, startY)
    }
}
