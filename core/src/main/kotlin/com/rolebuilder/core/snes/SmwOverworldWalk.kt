package com.rolebuilder.core.snes

/**
 * CAMINOS del overworld: Mario andando casilla a casilla, como en el juego.
 *
 * Port de `OwProcess03_StandingStill` ($04:9120) y `OwProcess04_PlayerIsMoving` ($04:945D),
 * más la inicialización de partida nueva de `InitializeSaveData` ($00:9F06). Se porta la
 * **lógica** del recorrido (qué casillas se pisan y dónde se para); la animación por
 * subpíxeles y las poses del sprite se dejan a quien dibuje.
 *
 * ## Cómo decide el juego si puedes moverte
 * Son dos comprobaciones encadenadas, y ninguna es la que uno esperaría:
 *
 * 1. **Desde dónde sales.** `ow_level_tile_settings[nivel]` (RAM `$7E:1EA2`, y es dato de
 *    PARTIDA GUARDADA, no de la ROM) lleva en su nibble bajo qué direcciones tienes abiertas
 *    desde esa casilla-de-nivel. El juego hace `settings[nivel] & botones & 0x0F`. Los bits
 *    son los mismos del mando: bit0 derecha, bit1 izquierda, bit2 abajo, bit3 arriba.
 * 2. **Adónde llegas.** La casilla de destino se mira en la capa 1 (`blocks_map16_table_lo`,
 *    cargada de `$0C:F7DF`): si vale 0 o es ≥ 0x87 **no se puede pisar**.
 *
 * Y a partir de ahí Mario avanza solo: en cada casilla, si la tesela es < 0x56 es CAMINO y
 * sigue; si está en 0x56..0x86 es casilla-de-nivel (o especial) y **para ahí**. Si la
 * dirección en la que iba se corta, prueba las otras —nunca la contraria, para no volverse—
 * en el orden derecha, izquierda, abajo, arriba. Así es como los caminos giran sin necesidad
 * de que nadie los describa: la forma la da el propio mapa.
 *
 * ## Al llegar
 * La casilla de destino abre la dirección CONTRARIA en su `settings` ($04:945D:
 * `settings[nivelDestino] |= kBitTable[(dir ^ 2) >> 1]`), que es lo que te deja volver por
 * donde viniste.
 *
 * ## Al superar un nivel
 * `UnlockOverworldPathBasedOnExit` ($04:9903) lee `kOwDirectionAfterBeatingLevel` ($04:D678)
 * — que **no** es una dirección suelta sino **cuatro códigos de 2 bits empaquetados**, uno
 * por tipo de salida (normal, secreta…), del bit 7-6 al 1-0 — y abre esa dirección. Ver
 * [unlockedDirection].
 *
 * ## Verificado de punta a punta contra la ROM US
 * Partida nueva: Mario en el submapa 1, casilla (6,7) = **YOSHI'S HOUSE**, con izquierda y
 * derecha abiertas (`settings[40] = 0x03`). Andando a la izquierda se llega en 4 casillas a
 * **YOSHI'S ISLAND 1**; a la derecha, en 4 casillas a **YOSHI'S ISLAND 2**. Que es
 * exactamente el principio del juego.
 */
object SmwOverworldWalk {

    // --- Direcciones -------------------------------------------------------------------
    // Los valores son los que usa el juego (`ow_player_direction`), no un enum nuestro:
    // se indexan tal cual en las tablas de la ROM con `dir >> 1`.
    const val DIR_UP = 0
    const val DIR_DOWN = 2
    const val DIR_LEFT = 4
    const val DIR_RIGHT = 6

    /** Las cuatro, en el orden en que las prueba el juego al buscar por dónde seguir. */
    val SEARCH_ORDER = intArrayOf(DIR_RIGHT, DIR_LEFT, DIR_DOWN, DIR_UP)

    /** `kBitTable_Bank04` ($04:9118): código de 2 bits → bit de dirección. */
    private val BIT_TABLE = intArrayOf(8, 4, 2, 1)

    /** `kSharedOverworldPathTables_DATA_049058` ($04:9058): desplazamiento por dirección. */
    private val STEP = intArrayOf(-1, 1, -1, 1)

    /** Bit de [dir] dentro del nibble bajo de `ow_level_tile_settings`. */
    fun dirBit(dir: Int): Int = BIT_TABLE[(dir shr 1) and 3]

    /** La dirección contraria (`dir ^ 2`): arriba↔abajo, izquierda↔derecha. */
    fun opposite(dir: Int): Int = dir xor 2

    // --- Direcciones de la ROM ---------------------------------------------------------
    /** `kChangingLayer1OverworldTiles_Layer1TileLocation` ($04:D85D): 112 words, una por evento. */
    const val CHANGING_LOC_SNES = 0x04D85D

    /** `kChangingLayer1OverworldTiles_TilesThatChange` ($04:DA1D): 22 teselas de origen. */
    const val CHANGING_FROM_SNES = 0x04DA1D

    /** `kChangingLayer1OverworldTiles_TilesToBecome` ($04:DA33): 22 teselas de destino. */
    const val CHANGING_TO_SNES = 0x04DA33

    /** Cuántas entradas tiene la tabla de teselas que cambian. */
    const val CHANGING_COUNT = 22

    /** Eventos "silenciosos" ($04:E8E4): nº de evento, capa, sitio y tesela. 44 entradas. */
    const val SILENT_EVENT_SNES = 0x04E8E4
    const val SILENT_LAYER_SNES = 0x04E910
    const val SILENT_LOC_SNES = 0x04E93C
    const val SILENT_TILE_SNES = 0x04E994
    const val SILENT_COUNT = 44

    /** `kInitializeSaveData_InitialLevelFlags` ($00:9EE0): 8 words `(nivel | valor << 8)`. */
    const val INITIAL_FLAGS_SNES = 0x009EE0
    const val INITIAL_FLAGS_COUNT = 8

    /** `kInitializeSaveData_InitialOWPlayerPos` ($00:9EF0): 22 bytes de `ow_save_buffer[111..]`. */
    const val INITIAL_POS_SNES = 0x009EF0

    /** `kSharedOverworldPathTables_DATA_049060` ($04:9060): desplazamiento por tipo de salida. */
    const val EXIT_SHIFT_SNES = 0x049060

    /** Tamaño de `ow_level_tile_settings` (lo que copia `UpdateSaveBuffer`, más margen). */
    const val SETTINGS_SIZE = 141

    /** Última tesela que es CAMINO: por encima ya es casilla-de-nivel o especial. */
    const val PATH_TILE_MAX = 0x55

    /** Última tesela PISABLE. De 0x87 en adelante no se puede entrar. */
    const val STOP_TILE_MAX = 0x86

    /** Lado de la rejilla de casillas de un mapa (2 pantallas de 16 casillas). */
    const val GRID_SIDE = 32

    /** Tope de pasos de un recorrido: más que casillas hay en la rejilla entera. */
    private const val MAX_WALK_STEPS = GRID_SIDE * GRID_SIDE

    private fun pc(snes: Int, delta: Int): Int =
        (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta

    private fun u8(rom: ByteArray, snes: Int, delta: Int): Int {
        val i = pc(snes, delta)
        return if (i in rom.indices) rom[i].toInt() and 0xFF else 0
    }

    private fun u16(rom: ByteArray, snes: Int, delta: Int): Int =
        u8(rom, snes, delta) or (u8(rom, snes + 1, delta) shl 8)

    // --- La capa 1 (caminos y casillas-de-nivel) ---------------------------------------

    /** La capa 1 tal cual viene en la ROM (`$0C:F7DF`, 0x800 índices Map16), sin eventos. */
    fun layer1(rom: ByteArray, delta: Int): IntArray =
        IntArray(SmwOverworld.LEVEL_TILES_SIZE) {
            u8(rom, SmwOverworld.LEVEL_TILES_SNES + it, delta)
        }

    /**
     * La capa 1 con los eventos de [isActive] aplicados: **es la que hay que usar para andar**,
     * porque los caminos que abre un evento solo existen aquí.
     *
     * Port de `LoadOverworldLayer1AndEvents_04DA49` ($04:DA49): por cada evento disparado se
     * mira la tesela que hay en su sitio, se busca en la tabla de "teselas que cambian" —de
     * atrás hacia delante, como el juego— y se sustituye por su pareja. La entrada 21 es
     * especial y escribe también la casilla de al lado. Después van los eventos
     * "silenciosos" ($04:E9F1), que colocan teselas sueltas de capa 1.
     */
    fun layer1WithEvents(rom: ByteArray, delta: Int, isActive: (Int) -> Boolean): IntArray {
        val tiles = layer1(rom, delta)
        val from = IntArray(CHANGING_COUNT) { u8(rom, CHANGING_FROM_SNES + it, delta) }
        val to = IntArray(CHANGING_COUNT) { u8(rom, CHANGING_TO_SNES + it, delta) }
        for (ev in 0 until SmwOverworld.EVENT_COUNT) {
            if (!isActive(ev)) continue
            val loc = u16(rom, CHANGING_LOC_SNES + 2 * ev, delta)
            if (loc < tiles.size) {
                val cur = tiles[loc]
                // El juego recorre la tabla de 21 a 0 y se queda con la PRIMERA que encaja.
                for (i in CHANGING_COUNT - 1 downTo 0) {
                    if (cur != from[i]) continue
                    tiles[loc] = to[i]
                    if (i == CHANGING_COUNT - 1 && loc + 1 < tiles.size) tiles[loc + 1] = to[i]
                    break
                }
            }
            // Eventos silenciosos de este mismo número: solo los de capa 1 (bit 0 a cero).
            for (i in SILENT_COUNT - 1 downTo 0) {
                if (u8(rom, SILENT_EVENT_SNES + i, delta) != ev) continue
                if (u8(rom, SILENT_LAYER_SNES + i, delta) and 1 != 0) continue
                val loc2 = u16(rom, SILENT_LOC_SNES + 2 * i, delta)
                if (loc2 < tiles.size) tiles[loc2] = u16(rom, SILENT_TILE_SNES + 2 * i, delta) and 0xFF
            }
        }
        return tiles
    }

    /**
     * `ow_level_number_of_each_tiletbl` de la ROM: por cada casilla, su número de nivel.
     *
     * **Se numera SIEMPRE sobre la capa 1 sin eventos**, y no es un detalle menor:
     * `LoadOverworldLayer1AndEvents_04D7F2` numera primero y aplica los eventos después. Si
     * se numerara sobre la capa con eventos saldrían 91 niveles en vez de 92 —el evento que
     * convierte la casilla 0x275 de 0x5D en 0x81 se come una— y todos los nombres y enlaces
     * a nivel se correrían un puesto en silencio.
     */
    fun levelNumbers(rom: ByteArray, delta: Int): IntArray = levelNumbers(layer1(rom, delta))

    /**
     * Numeración correlativa de las casillas-de-nivel de [tiles]. Pásale **la capa 1 sin
     * eventos**: ver [levelNumbers]. Es pública para no obligar a releer la ROM cuando ya se
     * tiene la capa base a mano.
     */
    fun levelNumbers(tiles: IntArray): IntArray {
        val out = IntArray(tiles.size)
        var n = 1
        for (j in tiles.indices) {
            val v = tiles[j]
            if (v >= SmwOverworld.LEVEL_TILE_MIN && v <= SmwOverworld.LEVEL_TILE_MAX) {
                out[j] = n; n++
            }
        }
        return out
    }

    // --- Geometría ---------------------------------------------------------------------

    /**
     * `CalculateOverworldPlayerPosition` ($04:9885): casilla (x, y) de la rejilla 32×32 de un
     * mapa → índice 0..0x7FF en la capa 1. Las cuatro pantallas van en 2×2 y el área de
     * submapas ([submap] distinto de 0) suma 0x400.
     */
    fun position(submap: Int, x: Int, y: Int): Int =
        (if (submap != 0) 0x400 else 0) + ((y / 16) * 2 + (x / 16)) * 0x100 + (y % 16) * 16 + (x % 16)

    /** ¿Se puede pisar esta tesela? Ni vacía ni de las que no dejan pasar. */
    fun isWalkable(tile: Int): Boolean = tile != 0 && tile <= STOP_TILE_MAX

    /** ¿Es una casilla donde Mario SE PARA (nivel o especial), en vez de camino de paso? */
    fun isStop(tile: Int): Boolean = tile > PATH_TILE_MAX && tile <= STOP_TILE_MAX

    // --- Partida nueva -----------------------------------------------------------------

    /** Dónde empieza una partida nueva, leído de la ROM. */
    data class Start(val submap: Int, val x: Int, val y: Int)

    /**
     * `ow_level_tile_settings` de una partida nueva, según `InitializeSaveData` ($00:9F06):
     * todo a cero salvo las 8 entradas de `kInitialLevelFlags`. En la ROM US eso deja abierto
     * el nivel 40 (YOSHI'S HOUSE) hacia izquierda y derecha, y poco más.
     */
    fun newGameSettings(rom: ByteArray, delta: Int): IntArray {
        val s = IntArray(SETTINGS_SIZE)
        for (i in 0 until INITIAL_FLAGS_COUNT) {
            val w = u16(rom, INITIAL_FLAGS_SNES + 2 * i, delta)
            val idx = w and 0xFF
            if (idx < s.size) s[idx] = (w shr 8) and 0xFF
        }
        return s
    }

    /**
     * Casilla de salida de una partida nueva. Sale de los 22 bytes que
     * `InitializeSaveData` copia a `ow_save_buffer[111..132]`: el primero es el submapa y los
     * bytes 14 y 16 son la casilla ya alineada a la rejilla. En la ROM US: submapa 1,
     * casilla (6, 7) — **la casa de Yoshi**.
     */
    fun newGameStart(rom: ByteArray, delta: Int): Start {
        val p = IntArray(22) { u8(rom, INITIAL_POS_SNES + it, delta) }
        return Start(submap = p[0], x = p[14], y = p[16])
    }

    // --- Abrir caminos al superar un nivel ----------------------------------------------

    /**
     * Qué dirección se abre en `ow_level_tile_settings[nivel]` al terminar [level] por la
     * salida [exitAction] (1 = normal, 2 = secreta, …). Devuelve el BIT, no la dirección.
     *
     * Port de `UnlockOverworldPathBasedOnExit` ($04:9903). El byte de `$04:D678` empaqueta
     * **cuatro códigos de 2 bits**; el tipo de salida elige cuál mirando `$04:9060`, que da
     * {5, 3, 1, 0}. Ojo con esos valores: el bucle del juego es un `do…while` que desplaza
     * una vez de más, así que el desplazamiento real es `n + 1` — salvo el 0, que no entra
     * al bucle y no desplaza nada. De ahí salen los desplazamientos 6, 4, 2 y 0.
     */
    fun unlockedDirection(rom: ByteArray, delta: Int, level: Int, exitAction: Int): Int {
        if (exitAction !in 1..4) return 0
        val raw = u8(rom, EXIT_SHIFT_SNES + (exitAction - 1), delta)
        val shift = if (raw == 0) 0 else raw + 1
        val packed = u8(rom, SmwOverworldLevels.DIRECTIONS_SNES + level, delta)
        return BIT_TABLE[(packed shr shift) and 3]
    }

    // --- El recorrido -------------------------------------------------------------------

    /** Una casilla pisada durante el recorrido. */
    data class Step(val x: Int, val y: Int, val tile: Int)

    /**
     * El resultado de andar: por dónde se pasó y dónde se acabó. [endLevel] es 0 si la
     * casilla final no es de nivel (una tubería, por ejemplo).
     */
    data class Walk(
        val steps: List<Step>,
        val endX: Int,
        val endY: Int,
        val endTile: Int,
        val endLevel: Int,
        val endDirection: Int,
    )

    /**
     * ¿Puede Mario salir de la casilla-de-nivel [level] hacia [dir], con esta partida?
     * Es el `settings[nivel] & botones & 0x0F` del juego.
     */
    fun canLeave(settings: IntArray, level: Int, dir: Int): Boolean =
        level in settings.indices && (settings[level] and dirBit(dir)) != 0

    /** Las direcciones abiertas desde [level], en el orden en que las prueba el juego. */
    fun openDirections(settings: IntArray, level: Int): List<Int> =
        SEARCH_ORDER.filter { canLeave(settings, level, it) }

    /**
     * Anda desde la casilla ([x], [y]) del mapa [submap] en la dirección [dir] hasta la
     * siguiente casilla donde Mario se para. Null si no se puede dar el primer paso.
     *
     * **El primer paso no gira.** Es fiel al juego y marca la diferencia: `OwProcess03`
     * comprueba la casilla de destino ANTES de ponerse a andar y, si no es pisable, no se
     * mueve nada. Sólo una vez en marcha (`OwProcess04`) el camino puede doblar esquinas.
     * Sin esto, pulsar hacia un sitio sin camino haría que Mario se fuera por otro lado.
     *
     * [tiles] tiene que ser la capa 1 **con los eventos de la partida aplicados**
     * ([layer1WithEvents]) — si no, los caminos recién abiertos no estarán. [levels], en
     * cambio, va SIN eventos: ver [levelNumbers].
     */
    fun walk(
        tiles: IntArray,
        levels: IntArray,
        submap: Int,
        x: Int,
        y: Int,
        dir: Int,
    ): Walk? {
        // Puerta de entrada: la casilla en la dirección pedida tiene que ser pisable.
        val firstX = if (dir >= 4) x + STEP[dir shr 1] else x
        val firstY = if (dir < 4) y + STEP[dir shr 1] else y
        if (!isWalkable(tileAt(tiles, submap, firstX, firstY))) return null

        var cx = x
        var cy = y
        var d = dir
        val steps = ArrayList<Step>()
        // Tope de seguridad: la rejilla es de 32×32 casillas, así que ningún camino real da
        // tantos pasos. Está para que una ROM rara no cuelgue la interfaz, no por lógica.
        repeat(MAX_WALK_STEPS) {
            val nx = if (d >= 4) cx + STEP[d shr 1] else cx
            val ny = if (d < 4) cy + STEP[d shr 1] else cy
            val tile = tileAt(tiles, submap, nx, ny)
            if (!isWalkable(tile)) {
                // Cortado: se prueban las demás, nunca la contraria (para no dar media vuelta).
                val opp = opposite(d)
                val next = SEARCH_ORDER.firstOrNull { cand ->
                    cand != opp && isWalkable(
                        tileAt(
                            tiles, submap,
                            if (cand >= 4) cx + STEP[cand shr 1] else cx,
                            if (cand < 4) cy + STEP[cand shr 1] else cy,
                        ),
                    )
                } ?: return if (steps.isEmpty()) null else Walk(
                    steps, cx, cy, tileAt(tiles, submap, cx, cy),
                    levels.getOrElse(position(submap, cx, cy)) { 0 }, d,
                )
                d = next
                return@repeat
            }
            cx = nx; cy = ny
            steps.add(Step(cx, cy, tile))
            if (isStop(tile)) {
                return Walk(
                    steps, cx, cy, tile,
                    levels.getOrElse(position(submap, cx, cy)) { 0 }, d,
                )
            }
        }
        return if (steps.isEmpty()) null else Walk(
            steps, cx, cy, tileAt(tiles, submap, cx, cy),
            levels.getOrElse(position(submap, cx, cy)) { 0 }, d,
        )
    }

    /** Tesela de la capa 1 en la casilla ([x], [y]) del mapa [submap]; 0 si se sale. */
    fun tileAt(tiles: IntArray, submap: Int, x: Int, y: Int): Int {
        if (x !in 0 until GRID_SIDE || y !in 0 until GRID_SIDE) return 0
        return tiles.getOrElse(position(submap, x, y)) { 0 }
    }
}
