package com.rolebuilder.core.snes

/**
 * TESELAS DE WARP de Super Mario World: qué bloques Map16 son ENTRADAS DE TUBERÍA y
 * PUERTAS (los sitios donde, al pulsar la dirección adecuada, se dispara la salida de
 * pantalla del nivel — el otro extremo de [SmwLevelExits]) y cómo un índice de ENTRADA
 * se convierte en una casilla de aparición concreta.
 *
 * Es la pieza gemela de [SmwLevelExits]: aquél dice "esta pantalla lleva a tal nivel";
 * esto dice "esta tesela es la boca de la tubería/puerta que lo activa" y "el jugador
 * reaparece en tal casilla". Sigue el patrón de [SmwBlockBehavior]: clasifica por el
 * byte BAJO del número de bloque Map16 (el plano de "block code", byte alto 0), que es
 * el que la rutina de bloques del jugador (`RunPlayerBlockCode`, banco $00) interpreta.
 *
 * ── TUBERÍAS (transcrito de `RunPlayerBlockCode`, banco $00) ──
 * La rutina recorre los puntos de colisión del jugador y, según qué byte bajo toca cada
 * punto y qué botón se mantiene ([PIPE_BUTTONS]), llama a `sub_F40A` ($00:F40A) para
 * arrancar el estado de "entrar en tubería":
 *  - VERTICAL: bytes bajos 0x37 y 0x38 → `RunPlayerBlockCode_00F3E9` ($00:F3E9) trata
 *    `tile_lo - 0x37 < 2` como boca vertical. Se entra HACIA ARRIBA si la toca el punto
 *    de la CABEZA (llamada con dir=2 → `PIPE_BUTTONS[2]=0x08`=Arriba, $00:00F3E9 vía la
 *    rama de "golpe desde abajo") o HACIA ABAJO si la tocan los PIES (dir=3 →
 *    `PIPE_BUTTONS[3]=0x04`=Abajo). 0x37 vs 0x38 solo cambia la mitad (izq/der) de la
 *    tesela donde la boca acepta al jugador (`DATA_00F3E3 = {0x0A, 0xFF}`), no la dirección.
 *  - HORIZONTAL: byte bajo 0x3F → `RunPlayerBlockCode_CheckIfEnteringHorizontalPipe`
 *    ($00:F3C4, `if tile_lo == 0x3F`). Se entra a IZQUIERDA o DERECHA según `player_facing_direction`
 *    (dir=facing → `PIPE_BUTTONS[0]=0x02`=Izq / `PIPE_BUTTONS[1]=0x01`=Der).
 * Por eso la dirección UP/DOWN (o LEFT/RIGHT) NO está en la tesela: la fija la geometría
 * (qué lado toca) más el botón. La tesela solo distingue VERTICAL vs HORIZONTAL.
 *
 * ── PUERTAS (transcrito de `RunPlayerBlockCode_EB77`, $00:EB77) ──
 * El punto de colisión del cuerpo, sobre una tesela ATRAVESABLE, llega a la rama de
 * "entrar por puerta" ($00:~EBC0, etiqueta LABEL_26): si se pulsa Arriba
 * (`io_controller_press1 & 0x08`) suena la puerta (`io_sound_ch3=15`),
 * `IncrementSublevelsEnteredAndPrepareToLoadSublevel` ($00:D273) prepara el sub-nivel y
 * `player_current_state = 0x0D` (animación de puerta). Los bytes bajos que llegan ahí:
 *  - 0x20 : puerta estándar (incondicional).
 *  - 0x1F : puerta que solo se entra con Mario PEQUEÑO (`if player_current_power_up goto skip`).
 *  - 0x9C : solo con `misc_level_tileset_setting == 1` (variante por tileset).
 *  - 0x27, 0x28 : solo con el interruptor-P AZUL activo (`timer_blue_pswitch`).
 * [pipeOrDoor] devuelve [WarpTile.DOOR] para las puertas SIEMPRE-puerta 0x1F y 0x20; las
 * condicionales (0x9C/0x27/0x28) se documentan pero no se clasifican para no dar falsos
 * positivos fuera de su tileset/estado.
 *
 * Igual que [SmwBlockBehavior], solo se clasifica el plano "block code" (byte alto 0): un
 * bloque de terreno gráfico (0x100..0x1FF) que "actúa como" una de estas teselas hay que
 * traducirlo antes por su tabla acts-like; aquí se recibe ya el byte de comportamiento.
 */
enum class WarpTile {
    /** Boca de tubería VERTICAL (bytes bajos 0x37/0x38): se entra arriba o abajo. */
    PIPE_VERTICAL,
    /** Boca de tubería HORIZONTAL (byte bajo 0x3F): se entra a izquierda o derecha. */
    PIPE_HORIZONTAL,
    /** Puerta (bytes bajos 0x1F/0x20): se entra pulsando Arriba. */
    DOOR,
}

object SmwWarpTiles {

    /** Bytes bajos de boca de tubería VERTICAL (`RunPlayerBlockCode_00F3E9`, $00:F3E9). */
    private val PIPE_VERTICAL_LO = 0x37..0x38

    /** Byte bajo de boca de tubería HORIZONTAL (`...CheckIfEnteringHorizontalPipe`, $00:F3C4). */
    private const val PIPE_HORIZONTAL_LO = 0x3F

    /** Bytes bajos de PUERTA siempre-puerta (LABEL_26 de `RunPlayerBlockCode_EB77`, $00:EB77). */
    private val DOOR_LO = intArrayOf(0x1F, 0x20)

    /**
     * Máscara de BOTÓN (bits del joypad alto de SMW) por índice de dirección de tubería,
     * `kRunPlayerBlockCode_PIPE_BUTTONS` ($00:F3xx): 0=Izq(0x02) 1=Der(0x01) 2=Arriba(0x08)
     * 3=Abajo(0x04). Expuesto para que el llamador sepa qué pulsación activa cada boca.
     */
    val PIPE_BUTTONS = intArrayOf(0x02, 0x01, 0x08, 0x04)

    /**
     * Clasifica el bloque Map16 [block] (`alto*0x100 + bajo`, 0x000..0x1FF) como tesela de
     * warp, o null si no lo es. Solo actúa el plano "block code" (byte alto 0), igual que
     * [SmwBlockBehavior.classify].
     */
    fun pipeOrDoor(block: Int): WarpTile? {
        if (block < 0 || block ushr 8 != 0) return null
        val lo = block and 0xFF
        return when {
            lo in PIPE_VERTICAL_LO -> WarpTile.PIPE_VERTICAL
            lo == PIPE_HORIZONTAL_LO -> WarpTile.PIPE_HORIZONTAL
            lo in DOOR_LO -> WarpTile.DOOR
            else -> null
        }
    }

    // ───────────────────────── posición de entrada ─────────────────────────
    //
    // `LoadLevel` ($05:D796) coloca al jugador leyendo cuatro tablas de PRESETS de posición
    // (las MISMAS para la entrada principal y para la secundaria, y las MISMAS que lee
    // [SmwLevelStartReader]). Con la entrada secundaria (`flag_use_secondary_entrance`),
    // $05:FA00[n] da el índice Y y $05:FC00[n] el índice X; con la principal salen de la
    // cabecera secundaria. En ambos casos el píxel es:
    //   yPixel = D740[yIdx]<<8 | D730[yIdx]     (yIdx 0..15)
    //   xPixel = D758[xIdx]<<8 | D750[xIdx]     (xIdx 0..7)
    // Transcrito de `LoadLevel` líneas $05:D7xx (`kLoadLevel_DATA_05D730/40/50/58`).
    //
    // Valores VANILLA de las tablas (idénticos a los que [SmwLevelStartReader] lee de la
    // ROM; se validan ahí con la firma D731==0x30). Se incrustan como constantes para que
    // este helper sea SIN-ROM, que es justo la transcripción del índice→posición del juego.

    /** $05:D730 — byte bajo de las 16 posiciones Y preset. */
    val ENTRANCE_Y_LO = intArrayOf(
        0x00, 0x30, 0x60, 0x80, 0xA0, 0xB0, 0xC0, 0xE0,
        0x10, 0x30, 0x50, 0x60, 0x70, 0x90, 0x00, 0x00,
    )

    /** $05:D740 — byte alto de las 16 posiciones Y preset. */
    val ENTRANCE_Y_HI = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1,
    )

    /** $05:D750 — byte bajo de las 8 posiciones X preset. */
    val ENTRANCE_X_LO = intArrayOf(0x10, 0x80, 0x00, 0xE0, 0x10, 0x70, 0x00, 0xE0)

    /** $05:D758 — byte alto de las 8 posiciones X preset. */
    val ENTRANCE_X_HI = intArrayOf(0, 0, 0, 0, 1, 1, 1, 1)

    private const val TILE = 16 // píxeles por casilla (una pantalla = 16 casillas)

    /**
     * Convierte un par de índices preset ([xIndex] 0..7, [yIndex] 0..15) en la casilla de
     * aparición `(xTile, yTile)` de 16 px, con la misma cuenta que [SmwLevelStartReader]
     * (`startPixelX/16`, `startPixelY/16`). Es la posición del PRESET dentro de la vista de
     * la entrada; el número de PANTALLA base de la entrada (byte alto real de `player_xpos`)
     * lo aporta el encuadre por separado, igual que [SmwLevelStart.fgBgPositionSetting].
     */
    fun presetPosition(xIndex: Int, yIndex: Int): Pair<Int, Int> {
        val xi = xIndex and 0x7
        val yi = yIndex and 0xF
        val xPixel = (ENTRANCE_X_HI[xi] shl 8) or ENTRANCE_X_LO[xi]
        val yPixel = (ENTRANCE_Y_HI[yi] shl 8) or ENTRANCE_Y_LO[yi]
        return (xPixel / TILE) to (yPixel / TILE)
    }

    /**
     * Casilla `(xTile, yTile)` donde reaparece el jugador al llegar por la ENTRADA
     * SECUNDARIA [secondary] (resuelta por [SmwLevelExits]). Usa sus índices preset
     * `entranceXIndex`/`entranceYIndex` con [presetPosition].
     */
    fun entrancePosition(secondary: SmwLevelExits.SecondaryEntrance): Pair<Int, Int> =
        presetPosition(secondary.entranceXIndex, secondary.entranceYIndex)

    /**
     * Casilla `(xTile, yTile)` de la ENTRADA PRINCIPAL de [start], consistente con
     * [SmwLevelStart.startTileX]/[SmwLevelStart.startTileY] (misma cuenta y tablas). Los
     * índices salen de la cabecera secundaria: `secHeader[1] & 7` (X), `secHeader[0] & 0xF` (Y).
     */
    fun mainEntrancePosition(start: SmwLevelStart): Pair<Int, Int> =
        presetPosition(start.secHeader[1] and 0x7, start.secHeader[0] and 0xF)

    // ───────────────────────── warps de un nivel, listos para el motor ─────────────────────────

    /**
     * Un warp RESUELTO de un nivel: la casilla de la boca (tubería vertical o puerta),
     * cómo se entra y a qué nivel/casilla lleva. Es la unión de las tres piezas: la
     * tesela de warp (esta clase), la salida de pantalla ([SmwLevelExits]) y la
     * posición de entrada del destino.
     */
    class LevelWarp(
        val xTile: Int,
        val yTile: Int,
        val kind: WarpTile,
        /** true = se entra pulsando ABAJO de pie sobre la boca; false = pulsando ARRIBA. */
        val enterDown: Boolean,
        val destLevel: Int,
        val destXTile: Int,
        val destYTile: Int,
    )

    /**
     * Los warps JUGABLES del [level]: cruza las teselas de warp de su rejilla de bloques
     * con la salida de pantalla de SU pantalla ([SmwLevelExits]) y con la entrada del
     * nivel destino. Decisiones (documentadas, no adivinadas):
     *  - Solo PUERTAS (0x1F/0x20) y TUBERÍAS VERTICALES (0x37/0x38). El 0x3F crudo
     *    (tubería horizontal) NO se usa: sin la tabla acts-like ese byte inunda la
     *    rejilla (65k+ apariciones en la ROM US) y daría falsos warps.
     *  - La dirección de una tubería vertical la fija su geometría, como en el juego:
     *    con AIRE encima se entra HACIA ABAJO (de pie sobre la boca); si no, hacia arriba.
     *  - El destino usa la ENTRADA PRINCIPAL del nivel destino (como [SmwLevelBundle]);
     *    la pantalla absoluta de las entradas secundarias queda para un refinamiento.
     */
    fun levelWarps(rom: ByteArray, header: SnesHeader, level: Int): List<LevelWarp> {
        val col = SnesGameRecipes.smwLevelCollision(rom, header, level) ?: return emptyList()
        val conn = runCatching { SmwLevelExits.read(rom, header, level) }.getOrNull() ?: return emptyList()
        if (conn.exits.isEmpty()) return emptyList()
        val exitByScreen = conn.exits.associateBy { it.fromScreen }
        val out = ArrayList<LevelWarp>()
        for (r in 0 until col.rows) for (c in 0 until col.cols) {
            val kind = pipeOrDoor(col.blockAt(c, r)) ?: continue
            if (kind == WarpTile.PIPE_HORIZONTAL) continue
            val exit = exitByScreen[c / 16] ?: continue
            val dest = exit.destinationLevel
            if (dest < 0 || dest >= 0x200) continue
            val start = SmwLevelStartReader.read(rom, header, dest) ?: continue
            val airAbove = r == 0 || col.solidityAt(c, r - 1) == SmwSolidity.NONE
            val enterDown = kind == WarpTile.PIPE_VERTICAL && airAbove
            out.add(LevelWarp(c, r, kind, enterDown, dest, start.startTileX, start.startTileY))
        }
        return out
    }
}
