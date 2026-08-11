package com.rolebuilder.core.snes

/**
 * TESELAS DE WARP de Super Mario World: qué bloques Map16 son ENTRADAS DE TUBERÍA y
 * PUERTAS (los sitios donde, al pulsar la dirección adecuada, se dispara la salida de
 * pantalla del nivel — el otro extremo de [SmwLevelExits]) y cómo un índice de ENTRADA
 * se convierte en una casilla de aparición concreta.
 *
 * Es la pieza gemela de [SmwLevelExits]: aquél dice "esta pantalla lleva a tal nivel";
 * esto dice "esta tesela es la boca de la tubería/puerta que lo activa" y "el jugador
 * reaparece en tal casilla".
 *
 * ── EL DATO CLAVE: SON DOS PLANOS, Y NO SIGNIFICAN LO MISMO ──
 * El layout del nivel vive en dos planos por celda, el byte BAJO ($7EC800) y el ALTO
 * ($7FC800) del número Map16; [SmwLayer1.SmwLevelTilemap.block] los entrega juntos como
 * `alto*0x100 + bajo`. `GetPlayerLevelCollisionMap16ID_Entry2` ($00:F465) devuelve el
 * par (`MakePairU16_AY(alto, bajo)`) y `RunPlayerBlockCode_EB77` ($00:EB77) BIFURCA con
 * el ALTO antes de mirar el bajo:
 *  - alto == 0 → plano de "block code" (monedas, `?`, PUERTAS, cinta de midway…).
 *  - alto != 0 → plano de TERRENO, y el bajo es el índice de colisión/comportamiento.
 * Es exactamente el mismo reparto que documenta [SmwBlockCollision].
 *
 * Las TUBERÍAS están en el plano de TERRENO (alto != 0) y las PUERTAS en el de BLOCK
 * CODE (alto == 0). Confundirlos no es un matiz: el bajo 0x38 del plano de block code
 * es la CINTA DE MIDWAY (`RunPlayerBlockCode_00F2C9`, $00:F2C9: `if (j == 56)` da la
 * seta y marca el punto intermedio), y el bajo 0x3F del plano de block code es la
 * TIERRA bajo el borde de hierba (`stdLedge` rellena con `setHi00`/0x3F, ver
 * [SmwLayer1]) — nada de eso es una tubería.
 *
 * ── TUBERÍAS (transcrito de `RunPlayerBlockCode`, banco $00) ──
 * La rutina recorre los puntos de colisión del jugador y, según qué byte bajo toca cada
 * punto y qué botón se mantiene ([PIPE_BUTTONS]), llama a `sub_F40A` ($00:F40A) para
 * arrancar el estado de "entrar en tubería". Los tres puntos que pueden hacerlo están
 * TODOS en la rama de terreno (alto != 0):
 *  - VERTICAL: bytes bajos 0x37 y 0x38 → `RunPlayerBlockCode_00F3E9` ($00:F3E9) trata
 *    `tile_lo - 0x37 < 2` como boca vertical. Se entra HACIA ARRIBA si la toca el punto
 *    de la CABEZA ($00:EB77 llama `RunPlayerBlockCode_00F3E9(2, …)` tras comprobar
 *    `alto != 0 && 0x11 <= bajo < 0x6E` → `PIPE_BUTTONS[2]=0x08`=Arriba) o HACIA ABAJO
 *    si la tocan los PIES (`RunPlayerBlockCode_00F3E9(3, …)` tras `alto != 0 && bajo <
 *    0x6E` → `PIPE_BUTTONS[3]=0x04`=Abajo). 0x37 vs 0x38 solo cambia la mitad (izq/der)
 *    de la tesela donde la boca acepta al jugador (`DATA_00F3E3 = {0x0A, 0xFF}`), no la
 *    dirección.
 *  - HORIZONTAL: byte bajo 0x3F → `RunPlayerBlockCode_CheckIfEnteringHorizontalPipe`
 *    ($00:F3C4, `if (j == 63)`), a la que solo se llega desde la sonda de PARED
 *    (`alto != 0 && 0x11 <= bajo < 0x6E`). Se entra a IZQUIERDA o DERECHA según
 *    `player_facing_direction` (`PIPE_BUTTONS[0]=0x02`=Izq / `PIPE_BUTTONS[1]=0x01`=Der).
 * Por eso la dirección UP/DOWN (o LEFT/RIGHT) NO está en la tesela: la fija la geometría
 * (qué lado toca) más el botón. La tesela solo distingue VERTICAL vs HORIZONTAL.
 *
 * ── PUERTAS (transcrito de `RunPlayerBlockCode_EB77`, $00:EB77) ──
 * El punto de colisión del cuerpo, con el plano ALTO a 0 (`if (!v2)`), llega a la rama de
 * "entrar por puerta" ($00:~EBC0, etiqueta LABEL_26): si se pulsa Arriba
 * (`io_controller_press1 & 0x08`) suena la puerta (`io_sound_ch3=15`),
 * `IncrementSublevelsEnteredAndPrepareToLoadSublevel` ($00:D273) prepara el sub-nivel y
 * `player_current_state = 0x0D` (animación de puerta). Los bytes bajos que llegan ahí:
 *  - 0x20 : puerta estándar (incondicional).
 *  - 0x1F : puerta que solo se entra con Mario PEQUEÑO (`if player_current_power_up goto skip`).
 *  - 0x9C : puerta de CASTILLO — solo con `misc_level_tileset_setting == 1`. Ese ajuste es
 *    el nibble bajo del byte 4 de la cabecera del nivel (`LoadLevelHeader`, $05:84E3:
 *    `misc_level_tileset_setting = hdr[4] & 0xF`), o sea [SmwLayer1.SmwLevelTilemap.tileset];
 *    por eso [pipeOrDoor] la acepta cuando se le pasa ese tileset y la rechaza si no.
 *  - 0x27, 0x28 : solo con el interruptor-P AZUL activo (`timer_blue_pswitch`). NO se
 *    clasifican: dependen de un estado temporal de la partida, no del nivel.
 *
 * ── A QUÉ NIVEL LLEVA ── Al entrar, `LoadLevel` ($05:D796) resuelve el destino con
 * `misc_subscreen_exit_entrance_number_lo[HIBYTE(player_xpos)]`: la salida de la PANTALLA
 * en la que está el jugador. Por eso [levelWarps] cruza cada boca con la salida de SU
 * pantalla ([SmwLevelExits]) y no con "la salida del nivel".
 */
enum class WarpTile {
    /** Boca de tubería VERTICAL (terreno, bytes bajos 0x37/0x38): se entra arriba o abajo. */
    PIPE_VERTICAL,
    /** Boca de tubería HORIZONTAL (terreno, byte bajo 0x3F): se entra a izquierda o derecha. */
    PIPE_HORIZONTAL,
    /** Puerta (block code, bytes bajos 0x1F/0x20): se entra pulsando Arriba. */
    DOOR,
}

/**
 * Cómo se dispara un warp: pulsando ABAJO, pulsando ARRIBA, o ANDANDO contra la boca de
 * una tubería horizontal hacia la IZQUIERDA ([SIDE_LEFT], la boca queda a la izquierda del
 * jugador) o hacia la DERECHA ([SIDE_RIGHT]).
 *
 * El lado va en el propio valor porque en el juego también va: `sub_F40A` ($00:F40A) exige
 * tener PULSADA la dirección hacia la que mira el jugador
 * (`kRunPlayerBlockCode_PIPE_BUTTONS[facing] & io_controller_hold1`, con
 * `PIPE_BUTTONS[0]=0x02` Izquierda y `[1]=0x01` Derecha), y a
 * `RunPlayerBlockCode_CheckIfEnteringHorizontalPipe` ($00:F3C4) solo se llega con
 * `player_facing_direction != player_hdir_block_touched`, o sea mirando al bloque que se
 * toca. Pasar por delante de la tubería andando al revés NO te mete en ella.
 */
enum class WarpEnter { DOWN, UP, SIDE_LEFT, SIDE_RIGHT }

object SmwWarpTiles {

    /** Bytes bajos de boca de tubería VERTICAL (`RunPlayerBlockCode_00F3E9`, $00:F3E9). */
    private val PIPE_VERTICAL_LO = 0x37..0x38

    /** Byte bajo de boca de tubería HORIZONTAL (`...CheckIfEnteringHorizontalPipe`, $00:F3C4). */
    private const val PIPE_HORIZONTAL_LO = 0x3F

    /** Bytes bajos de PUERTA siempre-puerta (LABEL_26 de `RunPlayerBlockCode_EB77`, $00:EB77). */
    private val DOOR_LO = intArrayOf(0x1F, 0x20)

    /** Byte bajo de la puerta de CASTILLO: solo es puerta con [CASTLE_TILESET] ($00:EB77). */
    private const val CASTLE_DOOR_LO = 0x9C

    /** Valor de `misc_level_tileset_setting` con el que 0x9C es puerta (cabecera, $05:84E3). */
    const val CASTLE_TILESET = 1

    /** "No sé el tileset del nivel": las puertas condicionales no se clasifican. */
    const val TILESET_UNKNOWN = -1

    /** Nº de bloques Map16 de Layer 1 (dos planos de 8 bits: 0x000..0x1FF). */
    private const val BLOCK_COUNT = 0x200

    /**
     * Máscara de BOTÓN (bits del joypad alto de SMW) por índice de dirección de tubería,
     * `kRunPlayerBlockCode_PIPE_BUTTONS` ($00:F3xx): 0=Izq(0x02) 1=Der(0x01) 2=Arriba(0x08)
     * 3=Abajo(0x04). Expuesto para que el llamador sepa qué pulsación activa cada boca.
     */
    val PIPE_BUTTONS = intArrayOf(0x02, 0x01, 0x08, 0x04)

    /**
     * Clasifica el bloque Map16 [block] (`alto*0x100 + bajo`, 0x000..0x1FF) como tesela de
     * warp, o null si no lo es. **El plano importa**: las tuberías solo cuentan en el plano
     * de TERRENO (alto != 0) y las puertas solo en el de BLOCK CODE (alto == 0), tal como
     * bifurca `RunPlayerBlockCode_EB77` ($00:EB77) con el byte alto antes de mirar el bajo.
     *
     * [tileset] es `misc_level_tileset_setting` del nivel (nibble bajo del byte 4 de su
     * cabecera, [SmwLayer1.SmwLevelTilemap.tileset]); solo hace falta para la puerta de
     * CASTILLO 0x9C. Sin él ([TILESET_UNKNOWN]) esa puerta no se clasifica.
     */
    fun pipeOrDoor(block: Int, tileset: Int = TILESET_UNKNOWN): WarpTile? {
        if (block < 0 || block >= BLOCK_COUNT) return null
        val lo = block and 0xFF
        val terrain = block ushr 8 != 0
        return when {
            terrain && lo in PIPE_VERTICAL_LO -> WarpTile.PIPE_VERTICAL
            terrain && lo == PIPE_HORIZONTAL_LO -> WarpTile.PIPE_HORIZONTAL
            !terrain && lo in DOOR_LO -> WarpTile.DOOR
            !terrain && lo == CASTLE_DOOR_LO && tileset == CASTLE_TILESET -> WarpTile.DOOR
            else -> null
        }
    }

    // ───────────────────────── posición de entrada ─────────────────────────
    //
    // `LoadLevel` ($05:D796) coloca al jugador leyendo cuatro tablas de PRESETS de posición
    // (las MISMAS para la entrada principal y para la secundaria, y las MISMAS que lee
    // [SmwLevelStartReader]). Con la entrada secundaria (`flag_use_secondary_entrance`),
    // $05:FA00[n] da el índice Y y $05:FC00[n] el índice X; con la principal salen de la
    // cabecera secundaria. En ambos casos el píxel del PRESET es:
    //   yPixel = D740[yIdx]<<8 | D730[yIdx]     (yIdx 0..15)
    //   xPixel = D758[xIdx]<<8 | D750[xIdx]     (xIdx 0..7)
    // Transcrito de `LoadLevel` líneas $05:D7xx (`kLoadLevel_DATA_05D730/40/50/58`).
    //
    // PERO EL PRESET NO ES LA POSICIÓN FINAL. Unas líneas más abajo, ya fuera del reparto
    // principal/secundaria, `LoadLevel` hace `r1 &= 0x1F` y `HIBYTE(player_xpos) = r1`
    // (o `HIBYTE(player_ypos) = r1` si el nivel es VERTICAL, `misc_level_layout_flags & 1`),
    // con `r1` = $05:F600[nivel] en la principal y $05:FC00[nº] en la secundaria. O sea que
    // el byte alto del preset SE PISA con la PANTALLA de la entrada, y D758 solo sobrevive
    // en los niveles verticales. Eso es [absolutePosition]; [presetPosition] es solo el
    // trozo de dentro de la pantalla.
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
     * Convierte un par de índices preset ([xIndex] 0..7, [yIndex] 0..15) en la casilla
     * `(xTile, yTile)` de 16 px del PRESET SOLO, sin la pantalla. Para la posición real de
     * llegada usa [absolutePosition]: el juego pisa el byte alto con la pantalla.
     */
    fun presetPosition(xIndex: Int, yIndex: Int): Pair<Int, Int> {
        val xi = xIndex and 0x7
        val yi = yIndex and 0xF
        val xPixel = (ENTRANCE_X_HI[xi] shl 8) or ENTRANCE_X_LO[xi]
        val yPixel = (ENTRANCE_Y_HI[yi] shl 8) or ENTRANCE_Y_LO[yi]
        return (xPixel / TILE) to (yPixel / TILE)
    }

    /**
     * Casilla `(xTile, yTile)` ABSOLUTA de una entrada: el preset ([xIndex] 0..7,
     * [yIndex] 0..15) con la [screen] (0..31) pisando el byte alto, como hace `LoadLevel`
     * ($05:D796) con `r1 &= 0x1F`. En un nivel [vertical] la pantalla va a la Y y el preset
     * de X conserva su byte alto; en uno horizontal, al revés. Misma cuenta que
     * [SmwLevelStartReader] (`startPixel/16`).
     */
    fun absolutePosition(screen: Int, xIndex: Int, yIndex: Int, vertical: Boolean): Pair<Int, Int> {
        val xi = xIndex and 0x7
        val yi = yIndex and 0xF
        val s = screen and 0x1F
        val xPixel = if (vertical) (ENTRANCE_X_HI[xi] shl 8) or ENTRANCE_X_LO[xi] else (s shl 8) or ENTRANCE_X_LO[xi]
        val yPixel = if (vertical) (s shl 8) or ENTRANCE_Y_LO[yi] else (ENTRANCE_Y_HI[yi] shl 8) or ENTRANCE_Y_LO[yi]
        return (xPixel / TILE) to (yPixel / TILE)
    }

    /**
     * Casilla `(xTile, yTile)` donde reaparece el jugador al llegar por la ENTRADA
     * SECUNDARIA [secondary] (resuelta por [SmwLevelExits]), ya ABSOLUTA: incluye su
     * `entranceScreen` ($05:FC00[n] & 0x1F). [vertical] es el destino, no el origen.
     */
    fun entrancePosition(
        secondary: SmwLevelExits.SecondaryEntrance,
        vertical: Boolean = false,
    ): Pair<Int, Int> = absolutePosition(
        secondary.entranceScreen, secondary.entranceXIndex, secondary.entranceYIndex, vertical,
    )

    /**
     * Casilla `(xTile, yTile)` de la ENTRADA PRINCIPAL de [start], consistente con
     * [SmwLevelStart.startTileX]/[SmwLevelStart.startTileY] (misma cuenta y tablas). Los
     * índices salen de la cabecera secundaria: `secHeader[1] & 7` (X), `secHeader[0] & 0xF` (Y).
     */
    fun mainEntrancePosition(start: SmwLevelStart): Pair<Int, Int> =
        presetPosition(start.secHeader[1] and 0x7, start.secHeader[0] and 0xF)

    // ───────────────────────── warps de un nivel, listos para el motor ─────────────────────────

    /**
     * Un warp RESUELTO de un nivel: la casilla DISPARADORA, cómo se entra y a qué
     * nivel/casilla lleva. Es la unión de las tres piezas: la tesela de warp (esta clase),
     * la salida de pantalla ([SmwLevelExits]) y la posición de entrada del destino.
     *
     * OJO con [xTile]/[yTile]: es la casilla donde tiene que ESTAR EL JUGADOR, que no
     * siempre es la de la boca ([mouthXTile]/[mouthYTile]). En una tubería que se baja, la
     * boca es el suelo que pisa: el jugador está en la casilla de ENCIMA. El juego lo
     * resuelve con la sonda de los PIES (`RunPlayerBlockCode_00F3E9(3, …)`, $00:F3E9), que
     * mira la tesela de debajo; los motores que activan por SOLAPE de caja necesitan la
     * casilla que el jugador ocupa, o la tubería no se dispara nunca.
     */
    class LevelWarp(
        val xTile: Int,
        val yTile: Int,
        val kind: WarpTile,
        /** Botón/gesto que lo dispara ([WarpEnter]). */
        val enter: WarpEnter,
        /** Casilla de la BOCA real (la tesela 0x?37/0x?38/0x?3F o la puerta). */
        val mouthXTile: Int,
        val mouthYTile: Int,
        val destLevel: Int,
        val destXTile: Int,
        val destYTile: Int,
        /**
         * CÓMO aparece el jugador al llegar ($00:A6CC). El motor todavía no lo consume: hasta
         * que lo haga, un [SmwEntranceAction.SHOT_UP_OUT_OF_PIPE] deja al jugador quieto en la
         * casilla en vez de salir despedido hacia arriba de la tubería.
         */
        val destAction: SmwEntranceAction = SmwEntranceAction.WALK_IN,
    ) {
        /** true = se entra pulsando ABAJO de pie sobre la boca; false = pulsando ARRIBA. */
        val enterDown: Boolean get() = enter == WarpEnter.DOWN

        override fun toString(): String =
            "LevelWarp(en=($xTile,$yTile) boca=($mouthXTile,$mouthYTile) $kind $enter " +
                "-> ${"%03X".format(destLevel)} ($destXTile,$destYTile) $destAction)"
    }

    /**
     * Los warps JUGABLES del [level]: cruza las teselas de warp de su rejilla de bloques
     * con la salida de pantalla de SU pantalla ([SmwLevelExits]) y con la entrada del
     * nivel destino. Decisiones (documentadas, no adivinadas):
     *  - PUERTAS (block code 0x1F/0x20, más la de CASTILLO 0x9C cuando el tileset del nivel
     *    es 1), TUBERÍAS VERTICALES (terreno 0x?37/0x?38) y HORIZONTALES (terreno 0x?3F).
     *  - La dirección de una tubería vertical la fija su geometría, como en el juego:
     *    con AIRE encima se entra HACIA ABAJO (de pie sobre la boca, sonda de los PIES,
     *    `RunPlayerBlockCode_00F3E9(3, …)`); si no, HACIA ARRIBA (sonda de la CABEZA,
     *    `RunPlayerBlockCode_00F3E9(2, …)`).
     *  - Una tubería HORIZONTAL se entra ANDANDO contra su boca: el juego solo llega a
     *    `RunPlayerBlockCode_CheckIfEnteringHorizontalPipe` ($00:F3C4) desde la sonda de
     *    PARED y con `player_facing_direction != player_hdir_block_touched` (el jugador
     *    empuja contra el bloque). La boca es SÓLIDA, así que la casilla disparadora es la
     *    de al lado por el lado ABIERTO: el único vecino horizontal que no es pared
     *    ([SmwBlockCollision.blocksSide]). Si los dos lados son pared, o ninguno, no se
     *    emite: sin un lado claro sería inventar por dónde se entra. El gesto lleva el
     *    sentido ([WarpEnter.SIDE_LEFT]/[WarpEnter.SIDE_RIGHT]) porque el juego exige tener
     *    pulsada la dirección que apunta a la boca; pasar por delante al revés no entra.
     *  - El destino usa la ENTRADA PRINCIPAL del nivel destino (como [SmwLevelBundle]);
     *    la pantalla absoluta de las entradas secundarias queda para un refinamiento.
     */
    fun levelWarps(rom: ByteArray, header: SnesHeader, level: Int): List<LevelWarp> {
        val col = SnesGameRecipes.smwLevelCollision(rom, header, level) ?: return emptyList()
        val conn = runCatching { SmwLevelExits.read(rom, header, level) }.getOrNull() ?: return emptyList()
        if (conn.exits.isEmpty()) return emptyList()
        val exitByScreen = conn.exits.associateBy { it.fromScreen }
        // El tileset del nivel hace falta para la puerta de CASTILLO (0x9C), que solo es
        // puerta con `misc_level_tileset_setting == 1`. Sin pasarlo, esas puertas no se
        // clasifican y los castillos se quedan sin sus warps.
        val tileset = runCatching {
            SmwLayer1.parse(rom, SnesGameRecipes.smwHeaderDeltaPublic(header), level)?.tileset
        }.getOrNull() ?: TILESET_UNKNOWN
        val out = ArrayList<LevelWarp>()
        for (r in 0 until col.rows) {
            for (c in 0 until col.cols) {
                warpAt(rom, header, col, exitByScreen, c, r, tileset)?.let { out.add(it) }
            }
        }
        return out
    }

    /** El warp de la celda ([c],[r]) del mapa de colisión [col], o null si no hay boca ahí. */
    private fun warpAt(
        rom: ByteArray,
        header: SnesHeader,
        col: SnesGameRecipes.SmwCollisionMap,
        exitByScreen: Map<Int, SmwLevelExits.LevelExit>,
        c: Int,
        r: Int,
        tileset: Int,
    ): LevelWarp? {
        val kind = pipeOrDoor(col.blockAt(c, r), tileset) ?: return null
        val exit = exitByScreen[c / 16] ?: return null
        val dest = exit.destinationLevel
        if (dest < 0 || dest >= LEVEL_COUNT) return null
        val start = SmwLevelStartReader.read(rom, header, dest) ?: return null
        val (trigger, enter) = triggerCell(col, kind, c, r) ?: return null
        val (dx, dy) = landingOf(exit, start)
        return LevelWarp(
            xTile = trigger.first,
            yTile = trigger.second,
            kind = kind,
            enter = enter,
            mouthXTile = c,
            mouthYTile = r,
            destLevel = dest,
            destXTile = dx,
            destYTile = dy,
            destAction = landingActionOf(exit, start),
        )
    }

    /**
     * CÓMO aparece el jugador al tomar [exit]: con entrada secundaria, `$05:FE00[n] & 7`; con
     * la principal, `($05:F200[destino] & 0x38) >> 3`. Los dos acaban en la misma variable
     * (`misc_level_header_entrance_settings`) y en la misma rutina ($00:A6CC).
     */
    private fun landingActionOf(
        exit: SmwLevelExits.LevelExit,
        start: SmwLevelStart,
    ): SmwEntranceAction {
        val sec = exit.secondaryEntrance
        return if (exit.isSecondary && sec != null) sec.entranceActionKind else start.entranceActionKind
    }

    /**
     * Casilla `(xTile, yTile)` donde aterriza el jugador al tomar [exit], siendo [start] la
     * cabecera del nivel destino. Es el reparto de `LoadLevel` ($05:D796): con
     * `flag_use_secondary_entrance` la posición sale de las tablas de la ENTRADA SECUNDARIA
     * ($05:FA00/FC00, con su propia PANTALLA); si no, de la cabecera del nivel destino
     * ($05:F000/F200/F600). Sin lo primero, salir de una sala te devolvía al PRINCIPIO del
     * nivel en vez de a la tubería por la que entraste.
     */
    private fun landingOf(exit: SmwLevelExits.LevelExit, start: SmwLevelStart): Pair<Int, Int> {
        val sec = exit.secondaryEntrance
        return if (exit.isSecondary && sec != null) {
            entrancePosition(sec, start.isVertical)
        } else {
            start.startTileX to start.startTileY
        }
    }

    /**
     * Casilla donde tiene que estar el jugador para disparar la boca [kind] de ([c],[r]),
     * y con qué gesto. Null si la boca no es alcanzable (se sale de la rejilla, o una
     * horizontal sin lado abierto claro).
     */
    private fun triggerCell(
        col: SnesGameRecipes.SmwCollisionMap,
        kind: WarpTile,
        c: Int,
        r: Int,
    ): Pair<Pair<Int, Int>, WarpEnter>? = when (kind) {
        WarpTile.DOOR -> (c to r) to WarpEnter.UP
        WarpTile.PIPE_VERTICAL -> {
            // Con AIRE encima la boca mira hacia arriba y se baja pisándola: la casilla
            // disparadora es la de ENCIMA (ver el KDoc de [LevelWarp]). Si no, es una boca
            // de techo y se sube metiéndose en ella.
            val airAbove = r == 0 || col.solidityAt(c, r - 1) == SmwSolidity.NONE
            when {
                !airAbove -> (c to r) to WarpEnter.UP
                r == 0 -> null // boca en la fila 0: no hay "encima" donde ponerse
                else -> (c to (r - 1)) to WarpEnter.DOWN
            }
        }
        WarpTile.PIPE_HORIZONTAL -> openSideOf(col, c, r)?.let { open ->
            // El jugador se pone en el lado abierto y ANDA hacia la boca: si la boca le
            // queda a la derecha (la columna abierta es la de la izquierda), empuja a la
            // DERECHA, y al revés.
            (open to r) to (if (open < c) WarpEnter.SIDE_RIGHT else WarpEnter.SIDE_LEFT)
        }
    }

    /** Columna del ÚNICO vecino horizontal de ([c],[r]) que no es pared, o null si no lo hay. */
    private fun openSideOf(col: SnesGameRecipes.SmwCollisionMap, c: Int, r: Int): Int? {
        val left = c - 1
        val right = c + 1
        val leftOpen = left >= 0 && !SmwBlockCollision.blocksSide(col.blockAt(left, r))
        val rightOpen = right < col.cols && !SmwBlockCollision.blocksSide(col.blockAt(right, r))
        return when {
            leftOpen && !rightOpen -> left
            rightOpen && !leftOpen -> right
            else -> null
        }
    }

    /** Nº de huecos de nivel de la ROM (0x000..0x1FF). */
    private const val LEVEL_COUNT = 0x200
}
