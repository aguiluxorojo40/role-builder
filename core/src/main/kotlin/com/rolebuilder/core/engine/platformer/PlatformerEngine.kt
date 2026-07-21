package com.rolebuilder.core.engine.platformer

import com.rolebuilder.core.snes.SmwSlopes
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Estado en tiempo de ejecución del jugador del motor de plataformas (píxeles). */
class PlatformerBody(var x: Float, var y: Float) {
    var vx = 0f
    var vy = 0f
    var onGround = false
    var facingRight = true
    var dead = false
    /** Tiempo (en fotogramas) que ha estado subiendo el salto actual. */
    var jumping = false
    /** ¿Mario grande? (seta recogida). Cambia la altura de la caja y aguanta un golpe. */
    var big = false
    /** ¿Mario de FUEGO? (flor recogida). Implica grande y permite lanzar bolas de fuego. */
    var fire = false
    /** ¿Mario CAPA? (pluma recogida). Implica grande y permite PLANEAR al caer. */
    var cape = false
    /** ¿Deslizándose por una cuesta (agachado)? Mata a los enemigos que arrolla. */
    var sliding = false
    /** Fotogramas de invulnerabilidad restantes tras encoger por un golpe. */
    var invulnFrames = 0
}

/** Qué otorga un [PowerupItem]: SETA (crece), FLOR (fuego) o PLUMA (capa/planeo). */
enum class PowerupKind { MUSHROOM, FIRE_FLOWER, CAPE_FEATHER }

/**
 * Powerup en marcha (píxeles): sale de un bloque `?`, cae con gravedad, avanza en
 * línea recta rebotando en las paredes (y cayéndose por los bordes, como en SMW) y
 * al tocar al jugador le da su efecto según [kind] (SETA crece; FLOR da fuego). La
 * flor de SMW no camina, pero mantener el mismo movimiento simplifica y se recoge
 * igual de rápido.
 */
class PowerupItem(var x: Float, var y: Float, val kind: PowerupKind = PowerupKind.MUSHROOM) {
    val width = 14f
    val height = 14f
    var vx = 0.8f
    var vy = 0f
    var onGround = false
    var alive = true
}

/**
 * Bola de fuego que lanza Mario de fuego (píxeles): avanza en su dirección, cae con
 * gravedad y REBOTA en el suelo (como en SMW), mata al enemigo que toca y se apaga al
 * chocar con una pared, salir del nivel o agotar su vida.
 */
class Fireball(var x: Float, var y: Float, var vx: Float) {
    val width = 8f
    val height = 8f
    var vy = 1.5f
    var alive = true
    var life = 200 // fotogramas máximos en pantalla
}

/** Semilla de un enemigo: posición inicial en píxeles e id de sprite SMW. */
class EnemySeed(val xPixel: Int, val yPixel: Int, val id: Int)

/**
 * Conducta de un enemigo según su id de sprite SMW. La mayoría son [WALKER] (patrullan
 * y caen); las Plantas Piraña tienen su propia máquina de estados, portada del juego:
 * [PIPE_PIRANHA] (asoma del tubo por ciclos, no sale si Mario está encima) y
 * [JUMPING_PIRANHA] (salta en arco; la de fuego escupe bolas).
 */
enum class EnemyBehavior { WALKER, PIPE_PIRANHA, JUMPING_PIRANHA }

/** Conducta del enemigo [id] (Plantas Piraña aparte; el resto andan). */
fun enemyBehaviorOf(id: Int): EnemyBehavior = when (id) {
    0x1A, 0x2A -> EnemyBehavior.PIPE_PIRANHA        // recta / cabeza-abajo
    0x4F, 0x50 -> EnemyBehavior.JUMPING_PIRANHA     // saltarina / saltarina de fuego
    else -> EnemyBehavior.WALKER
}

/**
 * Proyectil de enemigo (bola de fuego de la Planta Piraña de fuego): sale en diagonal
 * hacia arriba y ARQUEA con gravedad (como la rutina `Hammer` de SMW), hiere al jugador
 * al tocarlo y se apaga al salir del nivel o agotar su vida.
 */
class EnemyProjectile(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    val width = 8f
    val height = 8f
    var alive = true
    var life = 150
}

/** Tipo de ítem COLOCADO en el editor de plataformas. */
enum class ItemKind { COIN, GOAL }

/** Semilla de un ítem colocado: posición en píxeles y tipo (moneda/meta). */
class ItemSeed(val xPixel: Int, val yPixel: Int, val kind: ItemKind)

/** Ítem colocado en ejecución (16×16): moneda que se recoge o meta que gana el nivel. */
class PlacedItem(val x: Float, val y: Float, val kind: ItemKind) {
    val size = 16f
    var collected = false
}

/**
 * Acción interactiva de una celda del mapa, a nivel de motor (independiente de la
 * clasificación de bloques Map16 de SMW, que se mapea a esto al montar el nivel):
 *  - [NONE]: sin interacción.
 *  - [COIN]: se recoge al tocarla (suma moneda y desaparece).
 *  - [PRIZE]: bloque `?`: sólido; al golpearlo desde abajo suelta el premio (SETA si
 *    el jugador es pequeño, moneda si ya es grande) y queda "usado" (sigue sólido
 *    pero ya no premia).
 *  - [DRAGON_COIN]: la moneda grande "Yoshi/Dragon Coin" (objeto de 16×32 = dos celdas
 *    apiladas); se recoge al tocarla como una moneda enorme y cada 5 dan una vida extra.
 */
enum class BlockAction { NONE, COIN, PRIZE, DRAGON_COIN }

private val BLOCK_ACTIONS = BlockAction.values()

/** Cómo se entra a un warp: hacia abajo (tubería), arriba (puerta/tubería) o de lado. */
enum class WarpInput { DOWN, UP, SIDE }

/** Un warp en una celda: al entrar lleva al mapa [destMapId] en la celda ([destX],[destY]). */
class EngineWarp(
    val col: Int,
    val row: Int,
    val input: WarpInput,
    val destMapId: Int,
    val destX: Int,
    val destY: Int,
)

/** Destino de un warp que el jugador acaba de activar (lo consume la app). */
class WarpTarget(val destMapId: Int, val destX: Int, val destY: Int)

private fun cellKey(col: Int, row: Int): Long = (col.toLong() shl 32) or (row.toLong() and 0xffffffffL)

/** Enemigo en ejecución (píxeles): camina, cae con gravedad y se puede pisar. */
class PlatformerEnemy(var x: Float, var y: Float, val id: Int) {
    /** Caja de colisión (px). Editable en caliente desde el panel de hitboxes. */
    var width = 14f
    var height = 14f
    var vx = -0.5f    // patrulla: arranca hacia la izquierda, como los Goomba de SMW
    var vy = 0f
    var onGround = false
    var alive = true
    /** Fotogramas que sigue visible "aplastado" tras el pisotón antes de desaparecer. */
    var squashTimer = 0

    /** Conducta (andador / Planta Piraña de tubo / saltarina). */
    val behavior = enemyBehaviorOf(id)
    /** La Planta Piraña de fuego (0x50) escupe bolas; la cabeza-abajo (0x2A) asoma al revés. */
    val firePiranha = id == 0x50
    val upsideDown = id == 0x2A
    /** Ancla de la Planta Piraña (su tubo): la posición donde se colocó. */
    val spawnX = x
    val spawnY = y
    /** Estado y temporizador de la máquina de la Planta Piraña. */
    var pState = 0
    var pTimer = 0
    /**
     * Velocidad Y en UNIDADES de SMW (byte con signo `IIIISSSS`: nibble alto = px enteros,
     * bajo = 1/16 px) y su acumulador de SUBPÍXELES, para mover exactamente como el juego
     * (`SubSprYPosNoGrvty`). Solo la saltarina varía su velocidad; la de tubo usa fijas.
     */
    var pSpeed = 0
    var pSubY = 0
    /** Temporizador del estado 2 de la saltarina (dispara el fuego al valer 0x40). */
    var p2Timer = 0
    /** Contador de frames para la gravedad lenta del descenso (cada 4 frames). */
    var pFrame = 0
    /** true mientras la Piraña de tubo está METIDA (no hiere ni se dibuja como amenaza). */
    var hidden = behavior == EnemyBehavior.PIPE_PIRANHA || behavior == EnemyBehavior.JUMPING_PIRANHA
    /** Ya escupió fuego en el salto actual (una sola vez por salto). */
    var spat = false

    /**
     * ¿Este Koopa deja CAPARAZÓN al pisarlo? Los de caparazón (0x00-0x03, 0x05) y también
     * las Koopas ALADAS (0x08-0x0B), que al pisarlas pierden las alas y quedan de andador
     * con caparazón.
     */
    val canShell = id in intArrayOf(0x00, 0x01, 0x02, 0x03, 0x05, 0x08, 0x09, 0x0A, 0x0B)
    /** El Koopa está en su CAPARAZÓN (estado 9 quieto / A pateado de SMW). */
    var shell = false
    /** El caparazón se DESLIZA (pateado). Si false y [shell], está quieto en el suelo. */
    var shellMoving = false
    /** Gracia tras patear: fotogramas en que el caparazón que sale disparado no hiere a Mario. */
    var shellKickGrace = 0

    /** Koopa ALADA (Parakoopa 0x08-0x0B): vuela hasta que la pisan y pierde las alas. */
    var winged = id in 0x08..0x0B
    /** Id del Koopa de suelo equivalente (para color de caparazón / dibujo sin alas). */
    val koopaColorId = when (id) { 0x08, 0x09 -> 0x00; 0x0A, 0x0B -> 0x01; else -> id }
    /** Estado del vuelo (port EXACTO de las rutinas ParaKoopa $01), con sus nombres de RAM. */
    var flyPhase = 0    // SpriteMisc1570: temporizador del bob del aleteo (bit 0x20)
    var pSubX = 0       // acumulador de subpíxeles en X (como pSubY en Y)
    var oscSpeed = 0    // velocidad oscilante en unidades SMW (0x0A vertical / 0x0B horizontal)
    var oscTimer = 0    // SpriteMisc1540: pausa entre rampas
    var oscC2 = 0       // SpriteTableC2: cuenta para rampar cada 4 frames
    var oscPhase = 0    // SpriteMisc151C: sentido de la rampa (sube/baja)

    init {
        if (behavior != EnemyBehavior.WALKER) vx = 0f
        // La Piraña descansa en [spawnY] (metida) y asoma/salta desde ahí.
        when (behavior) {
            EnemyBehavior.PIPE_PIRANHA -> pTimer = PlatformerEngine.PIRANHA_TIME[0]
            EnemyBehavior.JUMPING_PIRANHA -> pTimer = PlatformerEngine.PIRANHA_JUMP_WAIT
            EnemyBehavior.WALKER -> {}
        }
    }
}

/**
 * Motor de plataformas de scroll lateral (estilo SMW), independiente del dibujado.
 * Consume lo que la ROM nos entregó: la **solidez por celda** ([SmwSolidity], del
 * extractor de colisión), las **físicas** ([PlatformerTuning], derivadas de las
 * tablas de SMW) y el **punto de inicio**. La app llama a [tick] una vez por
 * fotograma (60 fps) y dibuja el estado.
 *
 * Colisión por caja (AABB) contra la rejilla de 16 px, resuelta eje a eje:
 *  - [SmwSolidity.SOLID] frena por los cuatro lados.
 *  - [SmwSolidity.LEDGE_TOP] es plataforma de UN SENTIDO: solo frena la caída (te
 *    paras encima), se atraviesa de lado y saltando desde abajo — igual que SMW.
 *  - [SmwSolidity.SLOPE] con perfil ([slopeOffsetsAt]) es una RAMPA real (el suelo
 *    sigue la altura por columna, sin bloquear de lado); sin perfil, bloque macizo.
 *  - [SmwSolidity.SLOPE_STEEP] es la familia RELLENO de cuesta (lo 0xD8+ de SMW):
 *    con perfil, rampa; sin él, NO colisiona (difiere al bloque de debajo), fiel al
 *    dispatch real $00:EB77 — antes hacía de muro invisible en las diagonales.
 *  - [SmwSolidity.SPIKE] daña al jugador al tocarla (grande encoge; pequeño muere).
 *  - [SmwSolidity.NONE] no colisiona.
 */
class PlatformerEngine(
    val cols: Int,
    val rows: Int,
    private val solidityAt: (col: Int, row: Int) -> SmwSolidity,
    startPixelX: Int,
    startPixelY: Int,
    tuning: PlatformerTuning,
    enemySeeds: List<EnemySeed> = emptyList(),
    blockActions: IntArray? = null,
    warps: List<EngineWarp> = emptyList(),
    itemSeeds: List<ItemSeed> = emptyList(),
    /**
     * PERFIL de rampa de cada celda: las 16 alturas del suelo (offset desde arriba,
     * 0..16; 16 = sin suelo en esa columna) o null. Solo se consulta en celdas cuya
     * solidez es cuesta: con perfil, la celda es una RAMPA REAL (el suelo sigue la
     * altura por columna de píxel, no bloquea de lado y se puede uno deslizar); sin
     * él, bloque macizo (el comportamiento de siempre). Las fuentes son GENERALES:
     * formas de la ROM/editor ([SmwSlopes.floorOffsets]) o el perfil deducido del
     * dibujo del tile ([SmwSlopes.profileFromTilePixels]).
     */
    private val slopeOffsetsAt: (col: Int, row: Int) -> IntArray? = { _, _ -> null },
) {
    /**
     * Físicas ACTUALES del motor. Son MUTABLES en caliente: el panel de físicas del
     * jugador las sustituye en vivo (el tick lee esta referencia cada fotograma, así
     * el cambio se siente al instante). Las dimensiones de la caja (playerWidth/
     * playerHeight) se fijan al construir y no deben cambiarse en caliente.
     */
    @Volatile var tuning: PlatformerTuning = tuning

    val player = PlatformerBody(startPixelX.toFloat(), startPixelY.toFloat()).also {
        // Si el tuning ya viene de Mario grande (26 px), el estado arranca grande.
        it.big = tuning.playerHeight >= BIG_HEIGHT
    }

    /**
     * Altura ACTUAL de la caja del jugador: la del tuning de pequeño, o la de Mario
     * grande tras coger una seta. Todo el motor (y el dibujado) debe usar esta, no
     * `tuning.playerHeight`, para que crecer/encoger sea real.
     */
    val playerHeight: Float
        get() = if (player.big) BIG_HEIGHT else smallHeight

    /** Altura de pequeño: la del tuning, o la canónica si el tuning ya era de grande. */
    @Volatile var smallHeight =
        if (tuning.playerHeight >= BIG_HEIGHT) SMALL_HEIGHT else tuning.playerHeight
        private set

    /**
     * Cambia en CALIENTE la altura de la caja de Mario pequeño (panel de hitboxes),
     * conservando los PIES en su sitio (como crecer/encoger) para no hundirse en el
     * suelo ni quedar flotando.
     */
    fun setSmallHeight(h: Float) {
        val clamped = h.coerceIn(4f, BIG_HEIGHT)
        if (!player.big) player.y += smallHeight - clamped
        smallHeight = clamped
    }

    /** Powerups en marcha (seta/flor; vivas o no; la capa de dibujo filtra por alive). */
    val items = ArrayList<PowerupItem>()

    /** Bolas de fuego en vuelo lanzadas por Mario de fuego. */
    val fireballs = ArrayList<Fireball>()

    /** Bolas de fuego de ENEMIGOS en vuelo (Planta Piraña de fuego). */
    val enemyProjectiles = ArrayList<EnemyProjectile>()

    /** Celdas de warp del nivel (expuestas para el overlay de hitboxes). */
    val warpCells: List<EngineWarp> = warps

    /** Warps por celda (col,row) del nivel; se activan al entrar con el input correcto. */
    private val warpAt: Map<Long, EngineWarp> = warps.associateBy { cellKey(it.col, it.row) }

    /** Entrada de dirección para tuberías/puertas (además de [moveX] para el eje X). */
    var inputDown = false
    var inputUp = false

    /** Warp recién activado (destino de sala) que la app debe consumir, o null. */
    var pendingWarp: WarpTarget? = null
        private set

    /** La app llama a esto tras cambiar de sala para limpiar el warp pendiente. */
    fun consumeWarp() { pendingWarp = null }

    /** Enemigos vivos del nivel, instanciados de las semillas. */
    val enemies: List<PlatformerEnemy> =
        enemySeeds.map { PlatformerEnemy(it.xPixel.toFloat(), it.yPixel.toFloat(), it.id) }

    /** Ítems COLOCADOS en el editor (monedas/meta), instanciados de las semillas. */
    val placedItems: List<PlacedItem> =
        itemSeeds.map { PlacedItem(it.xPixel.toFloat(), it.yPixel.toFloat(), it.kind) }

    /** true cuando el jugador toca una META colocada (nivel completado). */
    var won = false
        private set

    /**
     * Rejilla MUTABLE de acciones de celda (ordinal de [BlockAction], cols*rows) o null
     * si el nivel no tiene bloques interactivos. El motor la modifica al recoger monedas
     * o golpear bloques `?`; la capa de dibujo la lee para no pintar lo ya consumido.
     */
    private val actions: IntArray? = blockActions?.copyOf()

    /** Acción interactiva de la celda (col,row), o [BlockAction.NONE] fuera de rango. */
    fun blockActionAt(col: Int, row: Int): BlockAction {
        val a = actions ?: return BlockAction.NONE
        if (col < 0 || col >= cols || row < 0 || row >= rows) return BlockAction.NONE
        return BLOCK_ACTIONS[a[row * cols + col]]
    }

    private fun setAction(col: Int, row: Int, value: BlockAction) {
        val a = actions ?: return
        if (col in 0 until cols && row in 0 until rows) a[row * cols + col] = value.ordinal
    }

    /** Monedas recogidas (monedas sueltas + premios de bloques `?`). */
    var coins = 0
        private set

    /** Contador monótono de "moneda conseguida" para el audio (SFX de moneda). */
    var coinEvents = 0
        private set

    /** Dragon Coins ("Yoshi Coins") recogidas en el nivel (0..4; a la 5ª da vida y vuelve a 0). */
    var dragonCoins = 0
        private set

    /** Contador monótono de "Dragon Coin conseguida" (SFX/animación propios). */
    var dragonCoinEvents = 0
        private set

    /** Contador monótono de vidas extra por juntar 5 Dragon Coins (evento 1-up). */
    var oneUpEvents = 0
        private set

    /** Input horizontal (-1 izquierda, +1 derecha) y botón de correr. */
    var moveX = 0f
    var running = false

    private var jumpHeld = false
    private var jumpPressedEdge = false

    /**
     * Contadores de eventos monótonos para el audio: cada vez que ocurre el evento
     * se incrementa. La capa de presentación recuerda el último valor visto y suena
     * el efecto por cada incremento (así no necesita callbacks ni acoplarse al motor).
     */
    var jumpEvents = 0
        private set
    var stompEvents = 0
        private set
    var deathEvents = 0
        private set
    /** Evento "powerup recogido" (Mario crece). */
    var powerupEvents = 0
        private set
    /** Evento "golpe recibido" (Mario encoge en vez de morir). */
    var damageEvents = 0
        private set
    /** Evento "bola de fuego lanzada". */
    var fireballEvents = 0
        private set
    /** Evento "Planta Piraña escupe fuego" (para SFX). */
    var piranhaFireEvents = 0
        private set

    /** Rising-edge del botón de correr (para lanzar bolas con la misma tecla, como SMW). */
    private var prevRunning = false

    /** Crece a Mario grande (los pies se quedan donde están; sube la cabeza). */
    private fun growPlayer() {
        if (player.big) return
        player.y -= BIG_HEIGHT - smallHeight
        player.big = true
        powerupEvents++
    }

    /** Da a Mario el poder de FUEGO (implica grande; excluye la capa). */
    private fun makeFire() {
        if (!player.big) growPlayer() else powerupEvents++
        player.fire = true
        player.cape = false
    }

    /** Da a Mario la CAPA (implica grande; excluye el fuego). */
    private fun makeCape() {
        if (!player.big) growPlayer() else powerupEvents++
        player.cape = true
        player.fire = false
    }

    /**
     * Golpe de enemigo: con poder (grande/fuego/capa) → vuelve a PEQUEÑO (pierde el
     * poder), con invulnerabilidad para escapar como en SMW; pequeño → muere.
     */
    private fun hurtPlayer() {
        if (player.invulnFrames > 0) return
        if (player.big) {
            player.big = false
            player.fire = false
            player.cape = false
            player.y += BIG_HEIGHT - smallHeight // los pies no se mueven
            player.invulnFrames = INVULN_FRAMES
            damageEvents++
        } else {
            killPlayer()
        }
    }

    /**
     * Lanza una bola de fuego si Mario tiene el poder, está en el suelo o el aire y no
     * hay ya dos en pantalla (límite de SMW). Sale desde el pecho en la dirección a la
     * que mira.
     */
    private fun throwFireball() {
        if (!player.fire || player.dead) return
        if (fireballs.count { it.alive } >= 2) return
        val dir = if (player.facingRight) 1f else -1f
        val speed = 4f
        val fx = player.x + if (player.facingRight) tuning.playerWidth else -8f
        val fy = player.y + playerHeight * 0.35f
        fireballs.add(Fireball(fx, fy, dir * speed))
        fireballEvents++
    }

    /**
     * Marca al jugador como muerto una sola vez y cuenta el evento. [pop] = el saltito
     * de muerte de SMW (DEATH_POP_SPEED); caer al vacío no lo da (ya está fuera).
     */
    private fun killPlayer(pop: Boolean = true) {
        if (!player.dead) {
            player.dead = true
            player.vx = 0f
            if (pop) player.vy = DEATH_POP_SPEED
            deathEvents++
        }
    }

    /** Marca la pulsación de salto (flanco). */
    fun pressJump() { jumpPressedEdge = true; jumpHeld = true }
    fun releaseJump() { jumpHeld = false }
    fun setJumpHeld(held: Boolean) {
        if (held && !jumpHeld) jumpPressedEdge = true
        jumpHeld = held
    }

    private val tileSize = 16

    /** Solidez de la celda (col,row), con los bordes del nivel como pared y el fondo abierto. */
    fun solidity(col: Int, row: Int): SmwSolidity {
        if (col < 0 || col >= cols) return SmwSolidity.SOLID // paredes laterales del nivel
        if (row < 0) return SmwSolidity.NONE                 // cielo abierto por arriba
        if (row >= rows) return SmwSolidity.NONE             // por debajo: hueco (se cae)
        return solidityAt(col, row)
    }

    private fun blocksSide(s: SmwSolidity) = s == SmwSolidity.SOLID ||
        s == SmwSolidity.SLOPE || s == SmwSolidity.SLOPE_STEEP

    private fun blocksFloor(s: SmwSolidity) = s == SmwSolidity.SOLID ||
        s == SmwSolidity.LEDGE_TOP || s == SmwSolidity.SLOPE || s == SmwSolidity.SLOPE_STEEP

    // ------------------------------------------------------------------- cuestas

    /**
     * PERFIL de rampa UTILIZABLE de la celda (16 alturas de suelo), o null. Solo
     * celdas con solidez de cuesta; sin perfil caen al comportamiento macizo.
     */
    private fun cellSlopeOffsets(col: Int, row: Int): IntArray? {
        val s = solidity(col, row)
        if (s != SmwSolidity.SLOPE && s != SmwSolidity.SLOPE_STEEP) return null
        val off = slopeOffsetsAt(col, row) ?: return null
        return if (off.size >= 16) off else null
    }

    /** ¿La celda es una RAMPA real (no bloquea de lado ni al subir)? */
    private fun isSlopeCell(col: Int, row: Int): Boolean = cellSlopeOffsets(col, row) != null

    /**
     * ¿La celda hace de PARED lateral? Fiel al dispatch real ($00:EB77): los bloques
     * de la familia cuesta (lo ≥ 0x6E) NUNCA empujan de lado — ni las rampas ni los
     * RELLENOS sobre/bajo cuesta (lo 0xD8+, nuestra SLOPE_STEEP sin perfil, que
     * difieren al bloque de debajo). Antes esos rellenos hacían de muro invisible a
     * mitad de diagonal. La cuesta simple SIN perfil se mantiene maciza (compatibilidad
     * con proyectos que la usan como bloque).
     */
    private fun wallsAt(col: Int, row: Int): Boolean = when (solidity(col, row)) {
        SmwSolidity.SOLID -> true
        SmwSolidity.SLOPE -> !isSlopeCell(col, row)
        else -> false
    }

    /**
     * ¿La celda hace de SUELO macizo (aparte de las rampas por perfil)? Los rellenos
     * de cuesta (STEEP sin perfil) NO: en el juego difieren al bloque de debajo, y
     * nuestro barrido de rampas/suelo ya encuentra ese soporte al pasar de largo.
     */
    private fun floorsAt(col: Int, row: Int): Boolean = when (solidity(col, row)) {
        SmwSolidity.SOLID, SmwSolidity.LEDGE_TOP -> true
        SmwSolidity.SLOPE -> !isSlopeCell(col, row)
        else -> false
    }

    /** Perfil de RAMPA visible de la celda (para el dibujado), o null. */
    fun slopeOffsets(col: Int, row: Int): IntArray? = cellSlopeOffsets(col, row)

    /**
     * Y del SUELO de la rampa en la celda (col,row) bajo la columna de píxel de
     * [xPixel] (el juego usa el CENTRO del jugador), o null si la celda no es rampa
     * o su columna no tiene suelo (offset 16 de los medios bordes).
     */
    private fun slopeSurfaceY(col: Int, row: Int, xPixel: Float): Float? {
        val off = cellSlopeOffsets(col, row) ?: return null
        val xLocal = (xPixel - col * tileSize).toInt().coerceIn(0, 15)
        val o = off[xLocal]
        return if (o >= 16) null else row * tileSize + o.toFloat()
    }

    /** Perfil de la rampa que SOSTIENE al jugador (celda de los pies o la de debajo). */
    private fun supportSlopeOffsets(): IntArray? {
        val centerX = player.x + tuning.playerWidth / 2f
        val ccol = (centerX / tileSize).toInt()
        val feet = player.y + playerHeight
        return cellSlopeOffsets(ccol, ((feet + 1f) / tileSize).toInt())
            ?: cellSlopeOffsets(ccol, ((feet - 1f) / tileSize).toInt())
    }

    /**
     * Aterrizaje sobre RAMPA de una ENTIDAD cayendo (centro en [cx], pies que pasan
     * de [feetFrom] a [feetTo]): la Y del suelo de la rampa alcanzada, o null. Lo
     * comparten jugador, enemigos, powerups y bolas para que TODO el mundo siga las
     * rampas, no solo Mario.
     */
    private fun slopeLandingY(cx: Float, feetFrom: Float, feetTo: Float): Float? {
        val ccol = (cx / tileSize).toInt()
        val rowFrom = (feetFrom / tileSize).toInt()
        val rowTo = (feetTo / tileSize).toInt()
        for (r in rowFrom..rowTo) {
            val surf = slopeSurfaceY(ccol, r, cx) ?: continue
            if (feetTo >= surf) return surf
        }
        return null
    }

    /**
     * Snap de RAMPA para una entidad apoyada (centro [cx], pies en [feet]): la Y de
     * superficie a la que pegarse si está a ≤6 px, o null. Evita que enemigos y setas
     * "floten" al bajar una rampa.
     */
    private fun slopeSnapY(cx: Float, feet: Float): Float? {
        val ccol = (cx / tileSize).toInt()
        val feetRow = (feet / tileSize).toInt()
        for (r in feetRow..feetRow + 1) {
            val surf = slopeSurfaceY(ccol, r, cx) ?: continue
            if (feet >= surf - 6f && feet <= surf + 5f) return surf
        }
        return null
    }

    /**
     * Pega los pies a la RAMPA (el snap-to-slope de SMW): al andar cuesta abajo evita
     * el "escalón volador" (quedarse en el aire un tramo) y al subir aúpa sobre la
     * superficie. Solo actúa sin velocidad de subida y cerca de la superficie.
     */
    private fun snapToSlope() {
        val p = player
        if (p.vy < 0f || p.jumping) return
        val h = playerHeight
        val centerX = p.x + tuning.playerWidth / 2f
        val ccol = (centerX / tileSize).toInt()
        val feet = p.y + h
        val feetRow = (feet / tileSize).toInt()
        for (r in feetRow..feetRow + 1) {
            val surf = slopeSurfaceY(ccol, r, centerX) ?: continue
            // Hasta 9 px POR ENCIMA de la superficie (bajando la cuesta: pega los pies
            // hacia abajo) o 5 px METIDO en la rampa (subiendo: aúpa hacia arriba).
            if (feet >= surf - 9f && feet <= surf + 5f) {
                p.y = surf - h - 0.01f
                if (p.vy > 0f) p.vy = 0f
                p.onGround = true
                return
            }
        }
    }

    /** Avanza un fotograma. */
    fun tick() {
        val p = player
        if (p.dead) {
            // Animación de MUERTE de SMW: Mario da el saltito hacia arriba (el "pop" de
            // -112/16 px/f de $00:F606, puesto por killPlayer) y cae SIN colisión hasta
            // salir del nivel; el mundo queda quieto alrededor. Antes se congelaba todo.
            p.vy = min(p.vy + tuning.gravityFall, tuning.maxFallSpeed)
            p.y += p.vy
            return
        }
        val t = tuning

        // --- TOBOGÁN: agachado (abajo) sobre una rampa, se desliza cuesta abajo; al
        // llegar al llano SIGUE arrollando con la inercia hasta frenarse, como SMW.
        // Mientras se desliza NO hay control ni rozamiento: manda la pendiente. ---
        p.sliding = false
        if (inputDown && p.onGround) {
            val off = supportSlopeOffsets()
            if (off != null) {
                p.sliding = true
                // pendiente > 0 = el suelo baja hacia la derecha → acelera a +X.
                val g = (off[15].coerceAtMost(16) - off[0].coerceAtMost(16)) / 15f
                p.vx = (p.vx + g * SLIDE_ACCEL).coerceIn(-SLIDE_MAX_SPEED, SLIDE_MAX_SPEED)
                if (g != 0f) p.facingRight = g > 0f
            } else if (abs(p.vx) > 0.5f) {
                p.sliding = true // en llano, sigue deslizando mientras conserve impulso
                // frenado suave (mucho menor que el rozamiento normal)
                p.vx = if (p.vx > 0) max(0f, p.vx - t.friction * 0.15f) else min(0f, p.vx + t.friction * 0.15f)
            }
        }

        // --- horizontal: aceleración hacia el tope, o rozamiento ---
        val maxSpeed = if (running) t.maxRunSpeed else t.maxWalkSpeed
        if (p.sliding) {
            // deslizándose no se acelera ni frena a mano
        } else if (abs(moveX) > 0.1f) {
            p.facingRight = moveX > 0
            p.vx += (if (moveX > 0) t.runAccel else -t.runAccel)
            p.vx = p.vx.coerceIn(-maxSpeed, maxSpeed)
        } else {
            // rozamiento hacia 0
            p.vx = if (p.vx > 0) max(0f, p.vx - t.friction) else min(0f, p.vx + t.friction)
        }

        // --- lanzar bola de fuego: al PULSAR correr (misma tecla que en SMW) ---
        if (running && !prevRunning) throwFireball()
        prevRunning = running

        // --- salto (con buffer de una pulsación) ---
        if (jumpPressedEdge && p.onGround) {
            p.vy = t.jumpSpeed
            p.onGround = false
            p.jumping = true
            jumpEvents++
        }
        jumpPressedEdge = false
        if (p.vy >= 0f) p.jumping = false

        // --- gravedad (salto variable: menor manteniendo el botón mientras sube) ---
        val gravity = if (jumpHeld && p.vy < 0f) t.gravityHold else t.gravityFall
        p.vy = min(p.vy + gravity, t.maxFallSpeed)

        // --- capa: PLANEO. Al caer manteniendo el salto, desciende despacio (como en
        // SMW al aletear); no anula el salto ni la subida, solo suaviza la caída. ---
        if (p.cape && !p.onGround && jumpHeld && p.vy > CAPE_GLIDE_SPEED) p.vy = CAPE_GLIDE_SPEED

        // --- integración con colisión, eje a eje ---
        moveHorizontal(p.vx)
        moveVertical(p.vy)
        snapToSlope()

        if (p.invulnFrames > 0) p.invulnFrames--

        updateEnemies()
        updateItems()
        updateFireballs()
        updateEnemyProjectiles()
        handlePlayerEnemyContact()
        collectCoins()
        collectPlacedItems()
        checkWarps()

        checkDeadly()
        if (p.y > (rows + 2) * tileSize) killPlayer(pop = false) // caído al vacío
    }

    /** Activa un warp si el jugador está sobre su celda y pulsa la dirección correcta. */
    private fun checkWarps() {
        if (pendingWarp != null || warpAt.isEmpty()) return
        val p = player
        val w = tuning.playerWidth
        val h = playerHeight
        val c0 = (p.x / tileSize).toInt()
        val c1 = ((p.x + w - 0.01f) / tileSize).toInt()
        val r0 = (p.y / tileSize).toInt()
        val r1 = ((p.y + h - 0.01f) / tileSize).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            val warp = warpAt[cellKey(c, r)] ?: continue
            val enter = when (warp.input) {
                WarpInput.DOWN -> inputDown && p.onGround // baja por la tubería estando de pie
                WarpInput.UP -> inputUp                   // entra por la puerta / sube por la tubería
                WarpInput.SIDE -> abs(moveX) > 0.3f       // entra de lado por la tubería horizontal
            }
            if (enter) {
                pendingWarp = WarpTarget(warp.destMapId, warp.destX, warp.destY)
                return
            }
        }
    }

    /** Recoge las monedas sueltas que solape la caja del jugador. */
    private fun collectCoins() {
        if (actions == null) return
        val p = player
        val w = tuning.playerWidth
        val h = playerHeight
        val c0 = (p.x / tileSize).toInt()
        val c1 = ((p.x + w - 0.01f) / tileSize).toInt()
        val r0 = (p.y / tileSize).toInt()
        val r1 = ((p.y + h - 0.01f) / tileSize).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            when (blockActionAt(c, r)) {
                BlockAction.COIN -> {
                    setAction(c, r, BlockAction.NONE)
                    coins++
                    coinEvents++
                }
                BlockAction.DRAGON_COIN -> collectDragonCoinAt(c, r)
                else -> {}
            }
        }
    }

    /**
     * Recoge la Dragon Coin que toca la celda (c,r): como es un objeto de 16×32 (dos
     * celdas apiladas), limpia toda la columna contigua de celdas Dragon Coin y cuenta
     * UNA sola. Cada 5 → vida extra ([oneUpEvents]) y vuelve a 0, como en SMW.
     */
    private fun collectDragonCoinAt(c: Int, r: Int) {
        setAction(c, r, BlockAction.NONE)
        var rr = r - 1
        while (blockActionAt(c, rr) == BlockAction.DRAGON_COIN) { setAction(c, rr, BlockAction.NONE); rr-- }
        rr = r + 1
        while (blockActionAt(c, rr) == BlockAction.DRAGON_COIN) { setAction(c, rr, BlockAction.NONE); rr++ }
        dragonCoins++
        dragonCoinEvents++
        if (dragonCoins >= 5) { dragonCoins = 0; oneUpEvents++ }
    }

    /**
     * Recoge los ítems COLOCADOS en el editor que solape la caja del jugador: las
     * monedas suman al contador (con SFX vía [coinEvents]); tocar la META marca [won].
     */
    private fun collectPlacedItems() {
        if (placedItems.isEmpty()) return
        val p = player
        val pw = tuning.playerWidth
        val ph = playerHeight
        for (item in placedItems) {
            if (item.collected) continue
            val overlap = p.x < item.x + item.size && p.x + pw > item.x &&
                p.y < item.y + item.size && p.y + ph > item.y
            if (!overlap) continue
            when (item.kind) {
                ItemKind.COIN -> { item.collected = true; coins++; coinEvents++ }
                ItemKind.GOAL -> won = true
            }
        }
    }

    /**
     * Mueve cada seta (gravedad + línea recta rebotando en paredes, cayéndose por los
     * bordes como en SMW) y la recoge si el jugador la toca: crece a grande, o da una
     * moneda si ya lo es.
     */
    private fun updateItems() {
        if (items.isEmpty()) return
        val p = player
        val pw = tuning.playerWidth
        val ph = playerHeight
        for (m in items) {
            if (!m.alive) continue
            // Vertical: gravedad y aterrizaje (mismos criterios que los enemigos, con
            // las RAMPAS primero: la seta rueda cuesta abajo en vez de flotar).
            m.vy = min(m.vy + tuning.gravityFall, tuning.maxFallSpeed)
            var ny = m.y + m.vy
            val minCol = (m.x / tileSize).toInt()
            val maxCol = ((m.x + m.width - 0.01f) / tileSize).toInt()
            m.onGround = false
            if (m.vy > 0) {
                val surf = slopeLandingY(m.x + m.width / 2f, m.y + m.height, ny + m.height)
                if (surf != null) {
                    ny = surf - m.height - 0.01f
                    m.vy = 0f
                    m.onGround = true
                } else {
                    val row = ((ny + m.height) / tileSize).toInt()
                    if ((minCol..maxCol).any { c -> floorsAt(c, row) }) {
                        ny = row * tileSize - m.height - 0.01f
                        m.vy = 0f
                        m.onGround = true
                    }
                }
            }
            m.y = ny
            // Horizontal: rebota SOLO en paredes (las rampas no lo son; por los
            // bordes se cae, no se gira), pegándose a la rampa al recorrerla.
            val nx = m.x + m.vx
            val minRow = (m.y / tileSize).toInt()
            val maxRow = ((m.y + m.height - 0.01f) / tileSize).toInt()
            val frontCol = if (m.vx > 0) ((nx + m.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((minRow..maxRow).any { r -> wallsAt(frontCol, r) }) {
                m.vx = -m.vx
            } else {
                m.x = nx
                if (m.onGround) {
                    slopeSnapY(m.x + m.width / 2f, m.y + m.height)?.let { m.y = it - m.height - 0.01f }
                }
            }
            if (m.y > (rows + 3) * tileSize) { m.alive = false; continue }
            // Recogida por solape con la caja del jugador.
            val overlap = p.x < m.x + m.width && p.x + pw > m.x &&
                p.y < m.y + m.height && p.y + ph > m.y
            if (overlap && !p.dead) {
                m.alive = false
                when (m.kind) {
                    PowerupKind.MUSHROOM -> if (!p.big) growPlayer() else { coins++; coinEvents++ }
                    PowerupKind.FIRE_FLOWER -> if (!p.fire) makeFire() else { coins++; coinEvents++ }
                    PowerupKind.CAPE_FEATHER -> if (!p.cape) makeCape() else { coins++; coinEvents++ }
                }
            }
        }
    }

    /**
     * Mueve las bolas de fuego: avanzan en su dirección, caen con gravedad y REBOTAN al
     * tocar el suelo (como en SMW); matan al enemigo que tocan y se apagan al chocar con
     * una pared, salir del nivel o agotar su vida. Máximo dos vivas (lo controla el
     * lanzamiento).
     */
    private fun updateFireballs() {
        if (fireballs.isEmpty()) return
        val it = fireballs.iterator()
        while (it.hasNext()) {
            val fb = it.next()
            if (!fb.alive) { it.remove(); continue }
            if (--fb.life <= 0) { fb.alive = false; it.remove(); continue }
            // Vertical: gravedad y rebote contra el suelo (las RAMPAS también botan,
            // sobre su superficie real).
            fb.vy = min(fb.vy + tuning.gravityFall, tuning.maxFallSpeed)
            var ny = fb.y + fb.vy
            val minCol = (fb.x / tileSize).toInt()
            val maxCol = ((fb.x + fb.width - 0.01f) / tileSize).toInt()
            if (fb.vy > 0) {
                val surf = slopeLandingY(fb.x + fb.width / 2f, fb.y + fb.height, ny + fb.height)
                if (surf != null) {
                    ny = surf - fb.height - 0.01f
                    fb.vy = -2.5f // rebota hacia arriba
                } else {
                    val row = ((ny + fb.height) / tileSize).toInt()
                    if ((minCol..maxCol).any { c -> floorsAt(c, row) }) {
                        ny = row * tileSize - fb.height - 0.01f
                        fb.vy = -2.5f // rebota hacia arriba
                    }
                }
            }
            fb.y = ny
            // Horizontal: al chocar con una pared se apaga.
            val nx = fb.x + fb.vx
            val minRow = (fb.y / tileSize).toInt()
            val maxRow = ((fb.y + fb.height - 0.01f) / tileSize).toInt()
            val frontCol = if (fb.vx > 0) ((nx + fb.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((minRow..maxRow).any { r -> wallsAt(frontCol, r) }) {
                fb.alive = false; it.remove(); continue
            }
            fb.x = nx
            // Fuera del nivel (lados o fondo): se apaga.
            if (fb.x < -16f || fb.x > (cols + 1) * tileSize || fb.y > (rows + 3) * tileSize) {
                fb.alive = false; it.remove(); continue
            }
            // Impacto con enemigo vivo: lo mata (como un pisotón, para el sonido).
            for (e in enemies) {
                if (!e.alive) continue
                if (fb.x < e.x + e.width && fb.x + fb.width > e.x &&
                    fb.y < e.y + e.height && fb.y + fb.height > e.y
                ) {
                    e.alive = false
                    e.squashTimer = 12
                    stompEvents++
                    fb.alive = false
                    it.remove()
                    break
                }
            }
        }
    }

    /** Gravedad, patrulla y colisión de cada enemigo con el terreno. */
    private fun updateEnemies() {
        for (e in enemies) {
            if (!e.alive) {
                if (e.squashTimer > 0) e.squashTimer--
                continue
            }
            when (e.behavior) {
                EnemyBehavior.PIPE_PIRANHA -> updatePipePiranha(e)
                EnemyBehavior.JUMPING_PIRANHA -> updateJumpingPiranha(e)
                EnemyBehavior.WALKER -> {
                    if (e.winged) updateWingedKoopa(e)
                    else if (e.shell) updateShell(e)
                    else {
                        e.vy = min(e.vy + tuning.gravityFall, tuning.maxFallSpeed)
                        moveEnemyVertical(e)
                        moveEnemyHorizontal(e)
                        if (e.y > (rows + 3) * tileSize) e.alive = false // cayó al vacío
                    }
                }
            }
        }
    }

    /**
     * Caparazón de Koopa: cae con gravedad; si está PATEADO ([shellMoving]) se desliza
     * rápido, REBOTA en las paredes y se cae por los bordes (no gira como el andador), y
     * ARROLLA a los demás enemigos que toca (la cadena de caparazón de SMW). Si está
     * quieto, solo se posa. Se apaga al caer al vacío.
     */
    private fun updateShell(e: PlatformerEnemy) {
        e.vy = min(e.vy + tuning.gravityFall, tuning.maxFallSpeed)
        moveEnemyVertical(e)
        if (e.shellKickGrace > 0) e.shellKickGrace--
        if (e.shellMoving) {
            val nx = e.x + e.vx
            val minRow = (e.y / tileSize).toInt()
            val maxRow = ((e.y + e.height - 0.01f) / tileSize).toInt()
            val frontCol = if (e.vx > 0) ((nx + e.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((minRow..maxRow).any { wallsAt(frontCol, it) }) e.vx = -e.vx  // rebota, no gira
            else e.x = nx
            // Arrolla a los demás enemigos que solape (no a otro caparazón parado ni a sí mismo).
            for (o in enemies) {
                if (o === e || !o.alive || o.hidden) continue
                if (e.x < o.x + o.width && e.x + e.width > o.x && e.y < o.y + o.height && e.y + e.height > o.y) {
                    o.alive = false; o.squashTimer = 12; stompEvents++
                }
            }
        }
        if (e.y > (rows + 3) * tileSize) e.alive = false // cayó al vacío
    }

    /** Mueve al enemigo en X por una velocidad en unidades de SMW (como [smwStepY] en Y). */
    private fun smwStepX(e: PlatformerEnemy, speed: Int) {
        val s = speed and 0xFF
        val spx = e.pSubX + ((s shl 4) and 0xFF)
        e.pSubX = spx and 0xFF
        val carry = spx shr 8
        var intPart = (s shr 4) and 0x0F
        if (intPart >= 8) intPart -= 16
        e.x += (intPart + carry).toFloat()
    }

    /**
     * Oscilación de velocidad de las Koopas aladas roja vertical/horizontal (`CODE_018CFD`):
     * mientras `1540` (=[oscTimer]) corre, la velocidad es constante; al agotarse, cada 4
     * cuentas de [oscC2] rampa ±1 (DATA_018CBA) hacia el tope ±0x10 (DATA_018CBC); al tocar
     * el tope invierte el sentido ([oscPhase]) y pausa 0x30. Devuelve la velocidad SMW.
     */
    private fun oscillate(e: PlatformerEnemy): Int {
        if (e.oscTimer > 0) { e.oscTimer--; return e.oscSpeed }
        e.oscC2 = (e.oscC2 + 1) and 0xFF
        if (e.oscC2 and 0x03 == 0) {
            val down = e.oscPhase and 0x01 == 1
            e.oscSpeed = (e.oscSpeed + (if (down) 1 else -1)) and 0xFF
            if (e.oscSpeed == (if (down) WINGED_OSC_LIMIT_DOWN else WINGED_OSC_LIMIT_UP)) {
                e.oscPhase++; e.oscTimer = WINGED_OSC_PAUSE
            }
        }
        return e.oscSpeed
    }

    /**
     * Koopa ALADA (Parakoopa 0x08-0x0B), port EXACTO de las rutinas ParaKoopa ($01) con las
     * unidades reales de SMW ([smwStepX]/[smwStepY]):
     *  - 0x08 verde: vuela a la izquierda a −0.5 px/f (F8; 0 si choca) con bob ∓0.25 (FC/04).
     *  - 0x09 verde saltarina: gravedad + salto −3 px/f (D0) al tocar el suelo, patrullando.
     *  - 0x0A rojo vertical: sube/baja con la velocidad OSCILANTE ([oscillate]).
     *  - 0x0B rojo horizontal: velocidad OSCILANTE en X + bob (FC/04) en Y.
     */
    private fun updateWingedKoopa(e: PlatformerEnemy) {
        e.flyPhase = (e.flyPhase + 1) and 0xFF                 // SpriteMisc1570
        val bob = if (e.flyPhase and 0x20 != 0) WINGED_BOB_DOWN else WINGED_BOB_UP
        when (e.id) {
            0x08 -> { // vuela a la izquierda; para en X si hay pared, sigue con el bob
                val minRow = (e.y / tileSize).toInt()
                val maxRow = ((e.y + e.height - 0.01f) / tileSize).toInt()
                val frontCol = (e.x / tileSize).toInt()
                if ((minRow..maxRow).none { wallsAt(frontCol, it) }) smwStepX(e, WINGED_FLY_X)
                smwStepY(e, bob)
            }
            0x09 -> { // saltarina: gravedad + salto al tocar suelo, patrullando en X
                e.vy = min(e.vy + tuning.gravityFall, tuning.maxFallSpeed)
                moveEnemyVertical(e)
                if (e.onGround) e.vy = signed(WINGED_BOUNCE).toFloat() / 16f   // 0xD0 = −3 px/f
                if (e.vx == 0f) e.vx = -0.5f
                moveEnemyHorizontal(e)
            }
            0x0A -> smwStepY(e, oscillate(e))                  // vertical: velocidad oscilante
            else -> { smwStepY(e, bob); smwStepX(e, oscillate(e)) } // 0x0B: bob en Y + oscila en X
        }
    }

    /** ¿Mario está horizontalmente pegado al tubo de la Piraña (dentro de [PIRANHA_NEAR_PX])? */
    private fun marioNearPipe(e: PlatformerEnemy): Boolean {
        val marioCx = player.x + tuning.playerWidth / 2f
        val pipeCx = e.spawnX + e.width / 2f
        return abs(marioCx - pipeCx) < PIRANHA_NEAR_PX
    }

    /**
     * Mueve al enemigo en Y por una velocidad en UNIDADES de SMW (`speed`, byte con signo
     * `IIIISSSS`), replicando `SubSprYPosNoGrvty` ($01): el nibble bajo (1/16 px) va a un
     * acumulador de subpíxeles y el alto (con signo) son los px enteros. Así el movimiento
     * es exacto al del juego (p. ej. 0xF0 = −1 px/f, 0xC2 = −3.875 px/f).
     */
    private fun smwStepY(e: PlatformerEnemy, speed: Int) {
        val s = speed and 0xFF
        val spx = e.pSubY + ((s shl 4) and 0xFF)
        e.pSubY = spx and 0xFF
        val carry = spx shr 8
        var intPart = (s shr 4) and 0x0F
        if (intPart >= 8) intPart -= 16                      // extensión de signo del nibble alto
        e.y += (intPart + carry).toFloat()
    }

    /** signed(byte): interpreta el byte SMW como velocidad con signo. */
    private fun signed(b: Int): Int = if ((b and 0xFF) >= 0x80) (b and 0xFF) - 256 else (b and 0xFF)

    /**
     * Planta Piraña de TUBO (0x1A/0x2A), port EXACTO de `ClassicPiranhas` ($01): ciclo de
     * 4 estados con velocidad Y por estado ([PIRANHA_SPEED] en unidades de SMW: 0, −1, 0,
     * +1 px/f) durante sus tiempos ([PIRANHA_TIME]): metida → saliendo (−1 px/f · 48 f =
     * asoma 48 px) → fuera → entrando (+1 px/f · 48 f = vuelve). NO sale del tubo (metida e
     * inerte) mientras Mario esté horizontalmente pegado. La 0x2A cuelga del techo (sentido
     * invertido, dos-complemento como el juego).
     */
    private fun updatePipePiranha(e: PlatformerEnemy) {
        if (e.pTimer > 0) {
            e.pTimer--
            var sp = PIRANHA_SPEED[e.pState]
            if (e.upsideDown) sp = (-sp) and 0xFF            // 0x2A: EOR #$FF : INC A
            smwStepY(e, sp)
        } else {
            // En el estado 0 (metida) solo sale si Mario NO está pegado al tubo.
            if (e.pState == 0 && marioNearPipe(e)) { e.pTimer = PIRANHA_TIME[0]; e.hidden = true; return }
            e.pState = (e.pState + 1) and 0x03
            e.pTimer = PIRANHA_TIME[e.pState]
            e.pSubY = 0
        }
        e.hidden = e.pState == 0                              // metida solo en reposo (dentro del tubo)
    }

    /**
     * Planta Piraña SALTARINA (0x4F/0x50), port EXACTO de `JumpingPiranhaMain` ($02).
     * Espera en el tubo; salta con velocidad inicial −4 px/f (0xC0) y sube frenando a
     * +0.125 px/f² (`+2` unidades/frame) hasta −1 px/f → pasa a la fase de descenso, que
     * usa una gravedad MUY lenta (`+1` unidad cada 4 frames) — el "cae poco a poco" — hasta
     * volver al tubo. No salta si Mario está pegado. La de fuego (0x50) escupe al valer el
     * temporizador 0x40.
     */
    private fun updateJumpingPiranha(e: PlatformerEnemy) {
        when (e.pState) {
            0 -> { // esperando en el tubo
                e.y = e.spawnY; e.hidden = true
                if (e.pTimer > 0) { e.pTimer--; return }
                if (marioNearPipe(e)) { e.pTimer = PIRANHA_JUMP_WAIT; return }
                e.pSpeed = PIRANHA_JUMP_SPEED                 // 0xC0 = −4 px/f
                e.pSubY = 0; e.spat = false; e.pFrame = 0
                e.hidden = false; e.pState = 1
            }
            1 -> { // subida frenando (gravedad normal)
                smwStepY(e, e.pSpeed)
                if (signed(e.pSpeed) < 0 || e.pSpeed < 0x40) e.pSpeed = (e.pSpeed + 2) and 0xFF
                if (signed(e.pSpeed) >= -16) { e.pState = 2; e.p2Timer = 0x50 }
            }
            2 -> { // descenso lento hasta volver al tubo
                if (e.firePiranha && e.p2Timer == 0x40 && !e.spat) { spitFireballs(e); e.spat = true }
                if (e.p2Timer > 0) e.p2Timer--
                e.pFrame++
                if ((e.pFrame and 0x03) == 0 && signed(e.pSpeed) < 8) e.pSpeed = (e.pSpeed + 1) and 0xFF
                smwStepY(e, e.pSpeed)
                if (e.y >= e.spawnY) {                        // volvió al tubo
                    e.y = e.spawnY; e.pState = 0; e.pTimer = PIRANHA_JUMP_WAIT
                    e.pSpeed = 0; e.hidden = true
                }
            }
        }
    }

    /**
     * La Piraña de fuego escupe DOS bolas en abanico, con las velocidades EXACTAS del juego
     * (X ±0x10 = ±1 px/f, Y 0xD0 = −3 px/f arriba); luego arquean con la gravedad de
     * `Hammer` ([EnemyProjectile]). No apunta a Mario: salen simétricas a los dos lados.
     */
    private fun spitFireballs(e: PlatformerEnemy) {
        if (enemyProjectiles.count { it.alive } >= 6) return
        val cx = e.x + e.width / 2f
        val cy = e.y + e.height / 2f
        enemyProjectiles.add(EnemyProjectile(cx, cy, PIRANHA_FIRE_VX, PIRANHA_FIRE_VY))
        enemyProjectiles.add(EnemyProjectile(cx, cy, -PIRANHA_FIRE_VX, PIRANHA_FIRE_VY))
        piranhaFireEvents++
    }

    /**
     * Avanza las bolas de fuego de enemigos: como en `Hammer` ($02), arquean (gravedad
     * `+2` unidades/frame = +0.125 px/f², tope +4 px/f) y hieren a Mario al tocarlo; se
     * apagan al salir del nivel o agotar su vida.
     */
    private fun updateEnemyProjectiles() {
        if (enemyProjectiles.isEmpty()) return
        val p = player
        val pw = tuning.playerWidth
        val ph = playerHeight
        val it = enemyProjectiles.iterator()
        while (it.hasNext()) {
            val f = it.next()
            if (!f.alive) { it.remove(); continue }
            f.vy = min(f.vy + PIRANHA_FIRE_G, 4f)            // gravedad de Hammer
            f.x += f.vx
            f.y += f.vy
            if (--f.life <= 0 || f.x < -16f || f.x > (cols + 1) * tileSize || f.y < -16f || f.y > (rows + 1) * tileSize) {
                f.alive = false; it.remove(); continue
            }
            if (!p.dead && p.invulnFrames == 0 &&
                p.x < f.x + f.width && p.x + pw > f.x && p.y < f.y + f.height && p.y + ph > f.y
            ) {
                f.alive = false; it.remove()
                hurtPlayer()
            }
        }
    }

    private fun moveEnemyHorizontal(e: PlatformerEnemy) {
        val nx = e.x + e.vx
        val top = e.y
        val bottom = e.y + e.height - 0.01f
        val minRow = (top / tileSize).toInt()
        val maxRow = (bottom / tileSize).toInt()
        val frontCol = if (e.vx > 0) ((nx + e.width) / tileSize).toInt() else (nx / tileSize).toInt()
        // Las RAMPAS no son pared para el enemigo: entra y el snap lo pega al suelo.
        val wall = (minRow..maxRow).any { wallsAt(frontCol, it) }
        // Se da la vuelta en el borde de una plataforma (no se tira al vacío), como en SMW.
        val footRow = ((e.y + e.height + 1f) / tileSize).toInt()
        val ledge = e.onGround && !blocksFloor(solidity(frontCol, footRow))
        if (wall || ledge) {
            e.vx = -e.vx
        } else {
            e.x = nx
            // Pegado a la rampa al recorrerla (baja sin flotar, sube sin hundirse).
            if (e.onGround) {
                slopeSnapY(e.x + e.width / 2f, e.y + e.height)?.let { e.y = it - e.height - 0.01f }
            }
        }
    }

    private fun moveEnemyVertical(e: PlatformerEnemy) {
        var ny = e.y + e.vy
        val left = e.x
        val right = e.x + e.width - 0.01f
        val minCol = (left / tileSize).toInt()
        val maxCol = (right / tileSize).toInt()
        e.onGround = false
        if (e.vy > 0) {
            // Primero las RAMPAS (superficie bajo el centro); luego el suelo normal.
            val surf = slopeLandingY(e.x + e.width / 2f, e.y + e.height, ny + e.height)
            if (surf != null) {
                ny = surf - e.height - 0.01f
                e.vy = 0f
                e.onGround = true
            } else {
                val row = ((ny + e.height) / tileSize).toInt()
                if ((minCol..maxCol).any { floorsAt(it, row) }) {
                    ny = row * tileSize - e.height - 0.01f
                    e.vy = 0f
                    e.onGround = true
                }
            }
        } else if (e.vy < 0) {
            val row = (ny / tileSize).toInt()
            if ((minCol..maxCol).any { solidity(it, row) == SmwSolidity.SOLID }) {
                ny = (row + 1) * tileSize + 0.01f
                e.vy = 0f
            }
        }
        e.y = ny
    }

    /** Pisotón (mata al enemigo y rebota) o contacto lateral (mata al jugador). */
    private fun handlePlayerEnemyContact() {
        val p = player
        if (p.dead) return
        val pw = tuning.playerWidth
        val ph = playerHeight
        for (e in enemies) {
            if (!e.alive) continue
            // La Piraña METIDA en el tubo no hiere ni colisiona (está a resguardo).
            if (e.hidden) continue
            val overlap = p.x < e.x + e.width && p.x + pw > e.x &&
                p.y < e.y + e.height && p.y + ph > e.y
            if (!overlap) continue
            // Deslizándose por la cuesta ARROLLA al enemigo (el tobogán de SMW).
            if (p.sliding) {
                e.alive = false
                e.squashTimer = 12
                stompEvents++
                continue
            }
            // Viene cayendo y sus pies están cerca de la cabeza del enemigo → pisotón.
            val stompFromAbove = p.vy > 0f && (p.y + ph) - e.y < e.height * 0.6f
            when {
                // Caparazón que SE DESLIZA: se para al pisarlo; de lado muerde (salvo gracia).
                e.canShell && e.shell && e.shellMoving -> {
                    if (stompFromAbove) { e.shellMoving = false; e.vx = 0f; bounceMario() }
                    else if (e.shellKickGrace <= 0) { hurtPlayer(); return }
                }
                // Caparazón QUIETO: pisarlo rebota (sigue quieto); tocarlo de lado lo PATEA.
                e.canShell && e.shell -> {
                    if (stompFromAbove) bounceMario()
                    else {
                        val marioCx = p.x + pw / 2f
                        e.shellMoving = true
                        e.vx = if (e.x + e.width / 2f >= marioCx) SHELL_SPEED else -SHELL_SPEED
                        e.shellKickGrace = SHELL_KICK_GRACE
                        stompEvents++ // reutiliza el SFX de "patada/pisotón"
                    }
                }
                // Koopa ALADA: pisarla le quita las ALAS y queda de andador (aún NO caparazón).
                e.canShell && e.winged -> {
                    if (stompFromAbove) {
                        e.winged = false; e.vy = 0f; e.vx = 0f
                        bounceMario(); stompEvents++
                    } else { hurtPlayer(); return }
                }
                // Koopa con caparazón: pisarlo lo mete en el CAPARAZÓN (no muere).
                e.canShell -> {
                    if (stompFromAbove) {
                        e.shell = true; e.shellMoving = false; e.vx = 0f
                        bounceMario(); stompEvents++
                    } else { hurtPlayer(); return }
                }
                // Resto: andadores se pisan; Plantas Piraña muerden por cualquier lado.
                else -> {
                    val stomp = e.behavior == EnemyBehavior.WALKER && stompFromAbove
                    if (stomp) {
                        e.alive = false; e.squashTimer = 12; bounceMario(); stompEvents++
                    } else { hurtPlayer(); return }
                }
            }
        }
    }

    /** Rebote de Mario tras pisar (enemigo o caparazón): pequeño impulso hacia arriba. */
    private fun bounceMario() {
        val p = player
        p.vy = tuning.jumpSpeed * 0.6f
        p.onGround = false
        p.jumping = false
    }

    private fun moveHorizontal(dx: Float) {
        if (dx == 0f) return
        val p = player
        val w = tuning.playerWidth
        val h = playerHeight
        var nx = p.x + dx
        val top = p.y
        val bottom = p.y + h - 0.01f
        val minRow = (top / tileSize).toInt()
        val maxRow = (bottom / tileSize).toInt()
        // Las RAMPAS con forma conocida no bloquean de lado: se entra en ellas y el
        // snap vertical aúpa/pega los pies a la superficie. Además, YENDO por una
        // rampa, el remate contra la meseta (un escalón de pocos px a la altura de
        // los pies) se SUBE en vez de chocar — es el equivalente al sensor central
        // de SMW, que corona la cuesta sin encallarse en la esquina del bloque.
        val onSlope = supportSlopeOffsets() != null
        fun tryMove(col: Int, clampX: Float) {
            val blockers = (minRow..maxRow).filter { wallsAt(col, it) }
            if (blockers.isEmpty()) return
            val stepTop = blockers.min() * tileSize.toFloat()
            val feet = p.y + h
            if (onSlope && feet - stepTop <= 8f) {
                p.y = stepTop - h - 0.01f // corona el escalón
            } else {
                nx = clampX
                p.vx = 0f
            }
        }
        if (dx > 0) {
            val col = ((nx + w) / tileSize).toInt()
            tryMove(col, col * tileSize - w - 0.01f)
        } else {
            val col = (nx / tileSize).toInt()
            tryMove(col, (col + 1) * tileSize + 0.01f)
        }
        p.x = nx
    }

    private fun moveVertical(dy: Float) {
        val p = player
        val w = tuning.playerWidth
        val h = playerHeight
        var ny = p.y + dy
        val left = p.x
        val right = p.x + w - 0.01f
        val minCol = (left / tileSize).toInt()
        val maxCol = (right / tileSize).toInt()
        p.onGround = false
        if (dy > 0) {
            // Cayendo. Primero las RAMPAS: el soporte se mide bajo el CENTRO de Mario
            // y aterriza sobre la superficie de la forma (altura por columna de píxel).
            val prevBottom = p.y + h
            val centerX = p.x + w / 2f
            val ccol = (centerX / tileSize).toInt()
            var landedOnSlope = false
            val rowFrom = (prevBottom / tileSize).toInt()
            val rowTo = ((ny + h) / tileSize).toInt()
            for (r in rowFrom..rowTo) {
                val surf = slopeSurfaceY(ccol, r, centerX) ?: continue
                if (ny + h >= surf) {
                    ny = surf - h - 0.01f
                    p.vy = 0f
                    p.onGround = true
                    landedOnSlope = true
                    break
                }
            }
            // Suelo normal (excluyendo rampas: su celda no bloquea como bloque). Los
            // bordes de un sentido solo cuentan si los pies cruzan el borde superior
            // de la celda desde arriba este fotograma.
            if (!landedOnSlope) {
                val row = ((ny + h) / tileSize).toInt()
                val tileTop = row * tileSize
                val hit = (minCol..maxCol).any {
                    val s = solidity(it, row)
                    floorsAt(it, row) && (s != SmwSolidity.LEDGE_TOP || prevBottom <= tileTop + 0.01f)
                }
                if (hit) {
                    ny = row * tileSize - h - 0.01f
                    p.vy = 0f
                    p.onGround = true
                }
            }
        } else if (dy < 0) {
            // Subiendo: golpe de cabeza solo contra sólidos (no los de un sentido).
            val row = (ny / tileSize).toInt()
            val hitCols = (minCol..maxCol).filter { solidity(it, row) == SmwSolidity.SOLID }
            if (hitCols.isNotEmpty()) {
                ny = (row + 1) * tileSize + 0.01f
                p.vy = 0f
                // Bloque '?': el golpe desde abajo suelta el premio y lo deja "usado"
                // (sigue sólido; su solidez no cambia). Un cabezazo = un bloque.
                // Premio progresivo (simplificación de diseño, no dato de la ROM):
                // SETA si es pequeño; FLOR (fuego) si es grande normal; PLUMA (capa) si
                // ya tiene fuego; MONEDA si ya tiene capa.
                for (c in hitCols) {
                    if (blockActionAt(c, row) == BlockAction.PRIZE) {
                        setAction(c, row, BlockAction.NONE)
                        val px = c * tileSize + 1f
                        val py = row * tileSize - 15f
                        when {
                            !p.big -> items.add(PowerupItem(px, py, PowerupKind.MUSHROOM))
                            !p.fire && !p.cape -> items.add(PowerupItem(px, py, PowerupKind.FIRE_FLOWER))
                            !p.cape -> items.add(PowerupItem(px, py, PowerupKind.CAPE_FEATHER))
                            else -> { coins++; coinEvents++ }
                        }
                        break
                    }
                }
            }
        }
        p.y = ny
    }

    private fun checkDeadly() {
        val p = player
        val w = tuning.playerWidth
        val h = playerHeight
        val c0 = (p.x / tileSize).toInt()
        val c1 = ((p.x + w - 0.01f) / tileSize).toInt()
        val r0 = (p.y / tileSize).toInt()
        val r1 = ((p.y + h - 0.01f) / tileSize).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            if (solidity(c, r) == SmwSolidity.SPIKE) { hurtPlayer(); return }
        }
    }

    companion object {
        /** Altura de la caja de Mario grande (dos casillas, como fromSmw con BIG). */
        const val BIG_HEIGHT = 26f
        /** Altura canónica de pequeño si el tuning ya venía de grande. */
        const val SMALL_HEIGHT = 14f
        /** Invulnerabilidad tras encoger (~1.5 s a 60 fps), para poder escapar. */
        const val INVULN_FRAMES = 90
        /** Velocidad de caída al PLANEAR con la capa (px/fotograma): descenso suave. */
        const val CAPE_GLIDE_SPEED = 1.0f
        /** "Pop" de muerte de SMW: -112 dieciseisavos de px/f ($00:F606) = -7 px/f. */
        const val DEATH_POP_SPEED = -7f
        /** Aceleración del TOBOGÁN por unidad de pendiente (px/f²): supera al rozamiento. */
        const val SLIDE_ACCEL = 0.25f
        /** Tope de velocidad deslizándose (px/f): el 0x40 dieciseisavos de SMW = 4.0. */
        const val SLIDE_MAX_SPEED = 4f

        // ---- Plantas Piraña: valores EXACTOS del ROM (ClassicPiranhas $01 y
        // JumpingPiranhaMain $02). Las velocidades Y de la de tubo van en unidades de SMW
        // (byte con signo, /16 = px/f) porque las mueve [smwStepY]; el resto son px/f. ----
        /** `PiranhaSpeed`: velocidad Y por estado (0, −1, 0, +1 px/f en unidades de SMW). */
        val PIRANHA_SPEED = intArrayOf(0x00, 0xF0, 0x00, 0x10)
        /** `PiranTimeInState`: duración de cada estado (metida, saliendo, fuera, entrando), frames. */
        val PIRANHA_TIME = intArrayOf(0x20, 0x30, 0x20, 0x30)
        /** Radio horizontal (px) dentro del cual Mario impide que la Piraña salga (`_F+0x1B<0x37` ≈ ±27). */
        const val PIRANHA_NEAR_PX = 27f
        /** Impulso de salto de la saltarina en unidades de SMW: 0xC0 = −4 px/f. */
        const val PIRANHA_JUMP_SPEED = 0xC0
        /** Espera en el tubo entre saltos (frames; `1540 = 0x40` al aterrizar). */
        const val PIRANHA_JUMP_WAIT = 0x40
        /** Bola de fuego de la Piraña: X ±0x10 = ±1 px/f, Y 0xD0 = −3 px/f (arriba), gravedad +0.125 px/f². */
        const val PIRANHA_FIRE_VX = 1f
        const val PIRANHA_FIRE_VY = -3f
        const val PIRANHA_FIRE_G = 0.125f

        // ---- Caparazón de Koopa (HandleSprKicked/Stunned $01, tabla ShellSpeedX) ----
        /** Velocidad del caparazón PATEADO (px/f): ShellSpeedX = 0x37 = 55 unidades = 3.4375 px/f. */
        const val SHELL_SPEED = 55f / 16f
        /** Gracia tras patear: el `KickingTimer = 0x0C` de SMW = 12 frames (valor exacto). */
        const val SHELL_KICK_GRACE = 12

        // ---- Koopas ALADAS (Parakoopa 0x08-0x0B): valores EXACTOS de las rutinas
        // GreenParaKoopa/RedVertParaKoopa/RedHorzParaKoopa ($01), en unidades de SMW.
        /** Velocidad de vuelo horizontal (0x08): Spr0to13SpeedX = 0xF8 = −0.5 px/f. */
        const val WINGED_FLY_X = 0xF8
        /** Bob del aleteo (Y): FC = −0.25 px/f o 04 = +0.25 px/f, según `1570 & 0x20`. */
        const val WINGED_BOB_UP = 0xFC
        const val WINGED_BOB_DOWN = 0x04
        /** Salto de la verde saltarina (0x09): 0xD0 = −3 px/f al tocar suelo. */
        const val WINGED_BOUNCE = 0xD0
        /** Oscilación de velocidad (0x0A/0x0B): rampa ±1 hacia ±0x10, pausa 0x30 (DATA_018CBA/CBC). */
        const val WINGED_OSC_LIMIT_DOWN = 0x10   // +1 px/f
        const val WINGED_OSC_LIMIT_UP = 0xF0     // −1 px/f
        const val WINGED_OSC_PAUSE = 0x30
    }
}
