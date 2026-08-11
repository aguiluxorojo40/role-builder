package com.rolebuilder.core.snes

/**
 * CÓMO aparece el jugador al entrar a un nivel: `misc_level_header_entrance_settings`, que
 * `InitializeLevelRAM_00A6CC` ($00:A6CC) traduce a un estado del jugador. Sale del nibble
 * `(secHdr1 & 0x38) >> 3` en la entrada normal ($05:F200) y de `$05:FE00[n] & 7` en la
 * secundaria; ambos acaban en la misma variable y en la misma rutina.
 *
 * Transcrito de `InitializeLevelRAM_00A6CC` ($00:A6CC), que reparte así:
 *  - 0 → cae al `LABEL_8`: `player_in_air_flag = 36`, sin estado especial. Apareces ANDANDO.
 *  - 1..4 → `player_pipe_action = v1 + 3`, `player_current_state = 5` (o 6 si `v1+3 >= 6`),
 *    `player_facing_direction = kInitializeLevelRAM_DATA_00A60D[v1 - 1]`. Sales de una
 *    TUBERÍA, y el índice dice hacia dónde.
 *  - 5 → `LABEL_8` también, pero rotando `flag_ice_level`. Andando, en nivel de hielo.
 *  - 6 → `InitializeLevelRAM_00A6C7(7, 0x20)`: `player_current_state = 7`
 *    (`PlayerState07_ShootOutOfPipe`, $00:D287) con `player_timer_pipe_warping = 0x20`, y
 *    `LOBYTE(player_xpos) |= 8`, `LOBYTE(player_ypos) |= 2`. Sales DISPARADO HACIA ARRIBA.
 *  - 7 → `flag_underwater_level = j` y, salvo cerradura/midway, `v1 = 4`: sales de una
 *    tubería en nivel de AGUA.
 *
 * Aquí sólo se NOMBRA el dato. El motor de plataformas no lo consume todavía: mientras no lo
 * haga, una llegada con [SHOT_UP_OUT_OF_PIPE] deja al jugador quieto en la casilla en vez de
 * salir despedido hacia arriba.
 */
enum class SmwEntranceAction {
    /** 0: andando, sin animación de entrada. */
    WALK_IN,
    /** 1..4: sale de una TUBERÍA; el valor crudo dice la dirección (`DATA_00A60D`). */
    OUT_OF_PIPE,
    /** 5: andando, y además rota `flag_ice_level` (nivel de hielo). */
    WALK_IN_ICE,
    /** 6: DISPARADO HACIA ARRIBA por una tubería (`player_current_state = 7`, $00:D287). */
    SHOT_UP_OUT_OF_PIPE,
    /** 7: sale de una tubería en un nivel de AGUA (`flag_underwater_level`). */
    OUT_OF_PIPE_UNDERWATER,
    ;

    companion object {
        /** Traduce el valor crudo 0..7 de `misc_level_header_entrance_settings`. */
        fun of(raw: Int): SmwEntranceAction = when (raw and 0x7) {
            0 -> WALK_IN
            5 -> WALK_IN_ICE
            6 -> SHOT_UP_OUT_OF_PIPE
            7 -> OUT_OF_PIPE_UNDERWATER
            else -> OUT_OF_PIPE
        }
    }
}

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
 * (La entrada del MIDWAY es la rama `else` de esa misma comparación y va aparte:
 * [midwayPixelX]/[midwayPixelY].)
 *
 * [startPixelX]/[startPixelY] son la posición de entrada en píxeles absolutos del nivel;
 * [startTileX]/[startTileY] la dan en casillas de 16 px, listas para el motor.
 */
class SmwLevelStart(
    val level: Int,
    /** Los 4 bytes de la cabecera secundaria ($05:F000, F200, F400, F600). */
    val secHeader: IntArray,
    /** X del PRESET crudo, `PAIR16(D758[xIdx], D750[xIdx])`, ANTES de que la pantalla lo pise. */
    val presetPixelX: Int,
    /** Y del PRESET crudo, `PAIR16(D740[yIdx], D730[yIdx])`, ANTES de que la pantalla lo pise. */
    val presetPixelY: Int,
) {
    /**
     * X de entrada ABSOLUTA: en un nivel horizontal la [entranceScreen] pisa el byte alto del
     * preset; en uno vertical el preset se queda como está (allí la pantalla va a la Y).
     */
    val startPixelX: Int
        get() = if (isVertical) presetPixelX else (entranceScreen shl 8) or (presetPixelX and 0xFF)

    /** Y de entrada ABSOLUTA: sólo en los niveles VERTICALES lleva la pantalla. */
    val startPixelY: Int
        get() = if (isVertical) (entranceScreen shl 8) or (presetPixelY and 0xFF) else presetPixelY

    val startTileX: Int get() = startPixelX / 16
    val startTileY: Int get() = startPixelY / 16

    /** PANTALLA de la entrada normal (`$05:F600[nivel] & 0x1F`), ya incluida en la posición. */
    val entranceScreen: Int get() = secHeader[3] and SCREEN_MASK

    /** ¿Nivel VERTICAL? `misc_level_layout_flags & 1`, o sea el bit 0x20 de $05:F600. */
    val isVertical: Boolean get() = (secHeader[3] and VERTICAL_BIT) != 0

    // ───────────────────────── entrada del PUNTO INTERMEDIO (midway) ─────────────────────────
    //
    // Cuando ya has cruzado la cinta del midway y vuelves a entrar al nivel, NO apareces en la
    // entrada normal. `LoadLevel` ($05:D796) lo decide en la MISMA comparación que la pantalla
    // de la entrada normal, en su rama `else`:
    //
    //   if (counter_sublevels_entered || (…, (ow_level_tile_settings[nivel] & 0x40) == 0)) {
    //       r1 &= 0x1F;  HIBYTE(player_xpos) = r1;            // entrada NORMAL (o vertical: a la Y)
    //   } else {
    //       flag_override_no_yoshi_intro_for_midway_entrance = ow_level_tile_settings[nivel] & 0x40;
    //       HIBYTE(player_xpos) = r2 >> 4;                     // entrada del MIDWAY
    //   }
    //
    // con `r2` = $05:F400[nivel] (asignado unas líneas antes, dentro del bloque de la entrada
    // principal). O sea: el midway SOLO cambia la PANTALLA — los presets de X y de Y dentro de
    // la pantalla son los MISMOS que los de la entrada normal. Por eso son 4 bits (0..15) y no
    // 5: `$05:F400[nivel] >> 4`.
    //
    // La condición para que se use es `ow_level_tile_settings[nivel] & 0x40`, el bit que el
    // overworld marca al salir del nivel habiendo tocado el midway ($04:~9800:
    // `ow_level_tile_settings[…] |= 0x40` cuando `flag_got_midpoint && misc_exit_level_action`).
    // Eso es ESTADO DE PARTIDA, no dato del nivel: aquí se expone la posición y el consumidor
    // decide cuándo usarla. Es el mismo bit que hace que `ExtObj46_MidwayBar` ($0D:A68E) deje
    // de dibujar la cinta (`SetMap16LowByte(v2, 0x38)` en el plano de block code) una vez
    // cogida.

    /** PANTALLA de la entrada del MIDWAY (`$05:F400[nivel] >> 4`, 0..15). */
    val midwayScreen: Int get() = secHeader[2] shr 4

    /**
     * X en píxeles donde reapareces con el midway cogido: la pantalla del midway pisando el
     * byte alto del MISMO preset de X de la entrada normal. Ojo a la rareza vanilla: esa rama
     * `else` NO distingue niveles verticales, así que el valor va a la X también ahí.
     */
    val midwayPixelX: Int get() = (midwayScreen shl 8) or (presetPixelX and 0xFF)

    /**
     * Y en píxeles con el midway cogido. La rama del midway no toca `player_ypos`: es el
     * PRESET crudo. Coincide con [startPixelY] en un nivel horizontal, y en uno vertical NO
     * (allí [startPixelY] lleva la pantalla de la entrada normal y esto no).
     */
    val midwayPixelY: Int get() = presetPixelY

    val midwayTileX: Int get() = midwayPixelX / 16
    val midwayTileY: Int get() = midwayPixelY / 16

    /**
     * ACCIÓN de entrada (`secHeader1 & 0x38 >> 3` → `misc_level_header_entrance_settings`,
     * consumida por `InitializeLevelRAM_00A6CC`, $00:A6CC). Ver [SmwEntranceAction].
     */
    val entranceAction: Int get() = (secHeader[1].toInt() and 0x38) shr 3

    /** [entranceAction] ya clasificada; lo que el motor necesita para animar la llegada. */
    val entranceActionKind: SmwEntranceAction get() = SmwEntranceAction.of(entranceAction)

    @Deprecated(
        "Nombre erróneo: no es una posición FG/BG, es la ACCIÓN de entrada ($00:A6CC)",
        ReplaceWith("entranceAction"),
    )
    val fgBgPositionSetting: Int get() = entranceAction

    private companion object {
        /** Bits 0-4 de $05:F600: PANTALLA de la entrada (`r1 &= 0x1F` de `LoadLevel`). */
        const val SCREEN_MASK = 0x1F

        /** Bit 0x20 de $05:F600: nivel VERTICAL (`misc_level_layout_flags & 1`). */
        const val VERTICAL_BIT = 0x20
    }
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

        // Se guardan los PRESETS crudos; quién pisa qué byte alto (la pantalla de la entrada
        // normal, la del midway, o nadie) lo resuelve [SmwLevelStart] con la cabecera.
        return SmwLevelStart(level, secHeader, (xHi shl 8) or xLo, (yHi shl 8) or yLo)
    }
}
