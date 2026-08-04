package com.rolebuilder.core.engine.platformer

import com.rolebuilder.core.snes.SmwPhysics
import com.rolebuilder.core.snes.SmwPlayerXMovement
import com.rolebuilder.core.snes.SmwSpriteAim
import com.rolebuilder.core.snes.SmwSlopes
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.math.abs
import kotlin.math.hypot
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
    /** Acumulador de subpíxeles en X del movimiento 1:1 de SMW (paralelo al `pSubX` del enemigo). */
    var xSub = 0
    /** ¿Derrapando (cambio de sentido a velocidad)? Lo marca el manejo horizontal 1:1. */
    var skidding = false
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
enum class EnemyBehavior {
    WALKER, PIPE_PIRANHA, JUMPING_PIRANHA, BOSS, MECHAKOOPA, BOWLING_BALL, BULLET_BILL,

    /** Boo: TÍMIDO — persigue a Mario solo cuando NO lo está mirando, y se para si lo mira. */
    BOO,

    /** Eerie: vuela recto atravesándolo todo, ondulando en vertical. Sin gravedad. */
    EERIE,

    /** Cheep-Cheep: NADA — avanza ondulando, sin gravedad ni suelo. */
    SWIMMER,

    /** Rip Van Fish: DUERME quieto hasta que Mario pasa cerca; entonces lo persigue. */
    RIP_VAN_FISH,
}

/**
 * Ids de los JEFES soportados como enemigos jugables (dibujados con su `big_<id>.png`):
 * Bowser (0xA0), Koopaling (0x29), Reznor (0xA9) y Big Boo Boss (0xC5). Su conducta aquí es
 * SIMPLIFICADA (enemigo grande con varios puntos de vida, patrulla lenta), no la IA de Modo 7
 * del juego original.
 */
val BOSS_IDS = intArrayOf(0xA0, 0x29, 0xA9, 0xC5)

/** Conducta del enemigo [id] (jefes y Plantas Piraña aparte; el resto andan). */
fun enemyBehaviorOf(id: Int): EnemyBehavior = when {
    id in BOSS_IDS -> EnemyBehavior.BOSS
    id == 0xA2 -> EnemyBehavior.MECHAKOOPA                        // Mechakoopa de Bowser
    id == 0xA1 -> EnemyBehavior.BOWLING_BALL                      // bola de bolos de Bowser
    id == 0x1A || id == 0x2A -> EnemyBehavior.PIPE_PIRANHA        // recta / cabeza-abajo
    id == 0x4F || id == 0x50 -> EnemyBehavior.JUMPING_PIRANHA     // saltarina / saltarina de fuego
    id == 0x1C -> EnemyBehavior.BULLET_BILL                       // Bala Bill: vuela recto, sin gravedad
    id == 0x37 -> EnemyBehavior.BOO                               // Boo: tímido, solo avanza si no lo miras
    id == 0x38 || id == 0x39 -> EnemyBehavior.EERIE               // Eerie: vuela ondulando
    id == 0x15 || id == 0x16 -> EnemyBehavior.SWIMMER             // Cheep-Cheep: nada
    id == 0x3D -> EnemyBehavior.RIP_VAN_FISH                      // Rip Van Fish: duerme hasta que pasas
    else -> EnemyBehavior.WALKER
}

/**
 * Proyectil de enemigo (bola de fuego de la Planta Piraña de fuego): sale en diagonal
 * hacia arriba y ARQUEA con gravedad (como la rutina `Hammer` de SMW), hiere al jugador
 * al tocarlo y se apaga al salir del nivel o agotar su vida.
 */
class EnemyProjectile(var x: Float, var y: Float, var vx: Float, var vy: Float,
                      /** true = arquea con gravedad (bola de Hammer / lanzamiento de Bowser);
                       *  false = vuela recto (aliento de fuego de Reznor/Koopaling). */
                      val arc: Boolean = true) {
    val width = 8f
    val height = 8f
    var alive = true
    var life = 150
}

/** Tipo de ítem COLOCADO en el editor de plataformas. */
/**
 * Tipo de ítem colocado. [GOAL] es el final NORMAL (cinta/esfera) y [GOAL_SECRET] la salida
 * SECRETA (cerradura): en SMW no son intercambiables — abren caminos DISTINTOS en el mapa del
 * mundo, y por eso el motor tiene que decir por CUÁL se acabó el nivel.
 */
enum class ItemKind { COIN, GOAL, GOAL_SECRET }

/** Semilla de un ítem colocado: posición en píxeles y tipo (moneda/meta). */
class ItemSeed(val xPixel: Int, val yPixel: Int, val kind: ItemKind)

/** Ítem colocado en ejecución (16×16): moneda que se recoge o meta que gana el nivel. */
class PlacedItem(val x: Float, val y: Float, val kind: ItemKind) {
    val size = 16f
    var collected = false
}

/**
 * Bloque de AGARRAR estilo SMW (píxeles). Empieza quieto en su celda; Mario lo coge con
 * el botón de correr estando al lado ([carried] = true, sigue a Mario), y al volver a
 * pulsar lo LANZA en la dirección a la que mira ([thrown] = true): vuela recto arrollando
 * enemigos y se rompe al chocar con una pared o salir del nivel. Con abajo pulsado, en vez
 * de lanzar lo deja en el suelo.
 */
class GrabBlock(var x: Float, var y: Float) {
    val width = 16f
    val height = 16f
    var carried = false
    var thrown = false
    var vx = 0f
    var alive = true
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
 *  - [GRAB]: bloque de AGARRAR (estilo SMW): al montar el nivel se convierte en una
 *    entidad [GrabBlock] que Mario puede coger (botón de correr), llevar y lanzar.
 */
enum class BlockAction { NONE, COIN, PRIZE, DRAGON_COIN, GRAB }

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

    /** Conducta (andador / Planta Piraña de tubo / saltarina / jefe). */
    val behavior = enemyBehaviorOf(id)
    /** ¿Es un JEFE? (Bowser/Koopaling/Reznor/Big Boo): grande, con varios puntos de vida. */
    val isBoss = behavior == EnemyBehavior.BOSS
    /** Puntos de vida: 1 para el enemigo normal, [PlatformerEngine.BOSS_HP] para los jefes. */
    var hp = if (isBoss) PlatformerEngine.BOSS_HP else 1
    /** Fotogramas de invulnerabilidad tras recibir un golpe (para no gastar todo el HP de una). */
    var hurtCooldown = 0
    /** Cuenta atrás hasta el próximo ATAQUE del jefe (bola de fuego / lanzamiento). */
    var attackTimer = PlatformerEngine.BOSS_ATTACK_INTERVAL
    /** Contador de fotogramas del Mechakoopa: cada 0x40 re-encara a Mario (`spr_table00c2`). */
    var faceTimer = 0
    /** Mechakoopa VOLTEADO tras un pisotón: quieto e inofensivo, se puede COGER; despierta al
     *  agotar [stunTimer] (como en SMW, que parpadea antes de levantarse). */
    var stunned = false
    var stunTimer = 0
    /** Mechakoopa que Mario LLEVA en brazos ahora mismo. */
    var carried = false
    /** Mechakoopa LANZADO: vuela recto y ARROLLA a lo que toca (incl. jefes). */
    var thrown = false
    /** Nº de ataques lanzados por el jefe en la FASE actual (Bowser: cuántos van de la tanda). */
    var attackCount = 0
    /** Fase de Bowser flotante: 0 = tanda de Mechakoopas, 1 = tanda de bolas de bolos. */
    var bossPhase = 0
    /**
     * ¿Se da la vuelta al llegar al BORDE de una plataforma, en vez de tirarse? Es la
     * diferencia clásica del Koopa ROJO (0x01, y sus variantes con alas verticales) frente al
     * VERDE y los demás andadores, que caminan hasta caerse. Dos enemigos que se dibujan casi
     * igual pero NO se juegan igual.
     */
    val turnsAtLedge = id == 0x01 || id == 0x0A || id == 0x0B

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
        if (behavior != EnemyBehavior.WALKER && behavior != EnemyBehavior.BOSS) vx = 0f
        // Los jefes son grandes: caja de colisión mayor (editable en caliente). Patrullan
        // despacio como un andador robusto.
        if (isBoss) {
            width = PlatformerEngine.BOSS_WIDTH
            height = PlatformerEngine.BOSS_HEIGHT
            vx = -PlatformerEngine.BOSS_SPEED
        }
        // La Piraña descansa en [spawnY] (metida) y asoma/salta desde ahí.
        when (behavior) {
            EnemyBehavior.PIPE_PIRANHA -> pTimer = PlatformerEngine.PIRANHA_TIME[0]
            EnemyBehavior.JUMPING_PIRANHA -> pTimer = PlatformerEngine.PIRANHA_JUMP_WAIT
            EnemyBehavior.BOSS -> {}
            EnemyBehavior.MECHAKOOPA -> {}
            EnemyBehavior.BOWLING_BALL -> {}
            // Bala Bill: vuela recto y sin gravedad; la dirección la fija el motor al sembrarla
            // (hacia el lado de Mario), aquí solo se anula la velocidad de andar por defecto.
            EnemyBehavior.BULLET_BILL -> vx = 0f
            // Flotantes: arrancan quietos; su update les da velocidad y los mueve sin gravedad.
            EnemyBehavior.BOO -> vx = 0f
            EnemyBehavior.EERIE -> vx = 0f
            EnemyBehavior.SWIMMER -> vx = 0f
            // Rip Van Fish empieza DORMIDO (pState 0) hasta que Mario se acerca.
            EnemyBehavior.RIP_VAN_FISH -> { vx = 0f; pState = 0 }
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
    /**
     * Físicas REALES de SMW leídas de la ROM ([SmwPhysics]). Si viene (modo ROM), el
     * movimiento HORIZONTAL de Mario usa el port 1:1 de `HandlePlayerPhysics`
     * ([SmwPlayerXMovement]) en vez del modelo simple de [tuning]; los proyectos
     * genéricos (sin ROM) la dejan a null y conservan el tacto de siempre.
     */
    private val smwPhysics: SmwPhysics? = null,
    /** Entorno del nivel: en el modo 1:1 elige rozamiento de agua y aceleración de hielo. */
    private val environment: PlatformerEnvironment = PlatformerEnvironment.LAND,
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

    /** Manejo horizontal 1:1 de Mario, solo en modo ROM (si hay [smwPhysics]). */
    private val smwMotion: SmwPlayerXMovement? = smwPhysics?.let { SmwPlayerXMovement(it) }
    private val onIce = environment == PlatformerEnvironment.ICE
    private val inWater = environment == PlatformerEnvironment.WATER
    /** Lo pone [moveHorizontal] cuando un muro frena a Mario (para cortar la velocidad SMW). */
    private var hBlocked = false

    /**
     * Lados en que Mario está bloqueado este fotograma (equivalente a `player_blocked_flags`
     * `$77` de SMW), recalculado cada `tick` por la colisión 1:1. Bits en [BLOCKED_LEFT] etc.
     */
    var blockedFlags = 0
        private set

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

    /**
     * Bloques de AGARRAR del nivel (estilo SMW), sembrados de las celdas [BlockAction.GRAB].
     * Uno puede estar cogido ([carriedBlock]); el resto quietos o lanzados.
     */
    val grabBlocks = ArrayList<GrabBlock>()

    /** Bloque que Mario lleva ahora mismo, o null. */
    var carriedBlock: GrabBlock? = null
        private set

    /** Mechakoopa VOLTEADO que Mario lleva ahora mismo, o null (se coge/lanza como un bloque). */
    var carriedEnemy: PlatformerEnemy? = null
        private set

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

    /** Enemigos vivos del nivel, instanciados de las semillas (+ los que generan los jefes). */
    val enemies: MutableList<PlatformerEnemy> =
        enemySeeds.mapTo(ArrayList()) { PlatformerEnemy(it.xPixel.toFloat(), it.yPixel.toFloat(), it.id) }

    /** Sub-sprites que un jefe genera este frame (Mechakoopas de Bowser…): se añaden a
     *  [enemies] al FINAL del bucle de enemigos, para no modificar la lista mientras se itera. */
    private val pendingSpawns = ArrayList<PlatformerEnemy>()

    /** Ítems COLOCADOS en el editor (monedas/meta), instanciados de las semillas. */
    val placedItems: List<PlacedItem> =
        itemSeeds.map { PlacedItem(it.xPixel.toFloat(), it.yPixel.toFloat(), it.kind) }

    /** true cuando el jugador toca una META colocada (nivel completado, por cualquier salida). */
    var won = false
        private set

    /**
     * true si el nivel se completó por la salida SECRETA (la cerradura), no por la meta normal.
     * Solo tiene sentido con [won] a true. El mapa del mundo lo necesita para abrir el camino
     * correcto: en SMW cada salida de un nivel lleva a un sitio distinto.
     */
    var wonSecret = false
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
    /** Evento "Mario coge un bloque" (para SFX). */
    var grabEvents = 0
        private set
    /** Evento "Mario lanza un bloque" (para SFX). */
    var grabThrowEvents = 0
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
     * Agarrar/llevar/lanzar bloques (estilo SMW), en el flanco del botón de correr:
     *  - Llevando uno: lo LANZA en la dirección de Mario (o lo deja si se pulsa abajo).
     *  - Al lado de un bloque quieto: lo COGE.
     * Devuelve true si consumió la pulsación (para no lanzar bola de fuego a la vez).
     */
    private fun handleGrabThrow(): Boolean {
        if (player.dead) return false
        val carried = carriedBlock
        if (carried != null) {
            carried.carried = false
            if (inputDown) {
                // Dejar en el suelo delante de Mario.
                carried.vx = 0f
            } else {
                carried.thrown = true
                carried.vx = if (player.facingRight) GRAB_THROW_SPEED else -GRAB_THROW_SPEED
                grabThrowEvents++
            }
            carriedBlock = null
            return true
        }
        // Llevando un Mechakoopa volteado: lo LANZA (o lo deja, volteado, si se pulsa abajo).
        val mech = carriedEnemy
        if (mech != null) {
            mech.carried = false
            if (inputDown) {
                mech.vx = 0f   // lo deja en el suelo, aún volteado
            } else {
                mech.thrown = true
                mech.vx = if (player.facingRight) GRAB_THROW_SPEED else -GRAB_THROW_SPEED
                grabThrowEvents++
            }
            carriedEnemy = null
            return true
        }
        // Coger un bloque quieto al alcance (solapando el frente de Mario).
        val reach = if (player.facingRight) player.x + tuning.playerWidth + GRAB_REACH
        else player.x - GRAB_REACH
        for (b in grabBlocks) {
            if (!b.alive || b.carried || b.thrown) continue
            val overlapY = player.y < b.y + b.height && player.y + playerHeight > b.y
            val nearX = reach >= b.x && reach <= b.x + b.width
            if (overlapY && nearX) {
                b.carried = true
                carriedBlock = b
                grabEvents++
                return true
            }
        }
        // Coger un Mechakoopa VOLTEADO al alcance (como un bloque).
        for (e in enemies) {
            if (e.behavior != EnemyBehavior.MECHAKOOPA || !e.alive || !e.stunned || e.carried || e.thrown) continue
            val overlapY = player.y < e.y + e.height && player.y + playerHeight > e.y
            val nearX = reach >= e.x && reach <= e.x + e.width
            if (overlapY && nearX) {
                e.carried = true
                carriedEnemy = e
                grabEvents++
                return true
            }
        }
        return false
    }

    /** Sigue al bloque llevado sobre la cabeza de Mario y avanza/colisiona los lanzados. */
    private fun updateGrabBlocks() {
        carriedBlock?.let { b ->
            // Se lleva DELANTE, con la base a la altura de los pies (menos 1px para no morder
            // la fila del suelo): así al lanzarlo alcanza a los enemigos de suelo sin que el
            // chequeo de pared lo confunda con el propio suelo.
            b.x = if (player.facingRight) player.x + tuning.playerWidth - 2f else player.x - b.width + 2f
            b.y = player.y + playerHeight - b.height - 1f
        }
        val it = grabBlocks.iterator()
        while (it.hasNext()) {
            val b = it.next()
            if (!b.alive) { it.remove(); continue }
            if (!b.thrown) continue
            // Vuela recto; se rompe contra pared o al salir del nivel.
            val nx = b.x + b.vx
            val minRow = (b.y / tileSize).toInt()
            val maxRow = ((b.y + b.height - 0.01f) / tileSize).toInt()
            val frontCol = if (b.vx > 0) ((nx + b.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((minRow..maxRow).any { r -> wallsAt(frontCol, r) }) { b.alive = false; it.remove(); continue }
            b.x = nx
            if (b.x < -16f || b.x > (cols + 1) * tileSize) { b.alive = false; it.remove(); continue }
            // Arrolla a cualquier enemigo vivo que toque.
            for (e in enemies) {
                if (!e.alive) continue
                if (b.x < e.x + e.width && b.x + b.width > e.x && b.y < e.y + e.height && b.y + b.height > e.y) {
                    damageEnemy(e); stompEvents++
                    b.alive = false; it.remove(); break
                }
            }
        }
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

    init {
        // Siembra las celdas de AGARRAR como entidades [GrabBlock] y libera su acción (ya
        // no es una celda-acción, es un objeto que Mario puede coger/lanzar).
        val a = actions
        if (a != null) for (r in 0 until rows) for (c in 0 until cols) {
            if (a[r * cols + c] == BlockAction.GRAB.ordinal) {
                grabBlocks.add(GrabBlock((c * tileSize).toFloat(), (r * tileSize).toFloat()))
                a[r * cols + c] = BlockAction.NONE.ordinal
            }
        }
    }

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

        // --- horizontal ---
        if (p.sliding) {
            // deslizándose no se acelera ni frena a mano (manda el tobogán, ya calculado)
        } else if (smwMotion != null) {
            // MODO 1:1: manejo horizontal EXACTO de SMW (HandlePlayerPhysics $00:D5F2).
            val dir = if (abs(moveX) < 0.1f) 0 else if (moveX > 0) 1 else -1
            smwMotion.step(dir, running, p.onGround, onIce, inWater)
            if (dir != 0) p.facingRight = smwMotion.direction == 1
            p.skidding = smwMotion.skidding
            // La posición avanza con el byte alto (1/16 px) y el acumulador de subpíxeles.
            p.vx = bodyStepX(smwMotion.speedHi)
        } else {
            // MODO GENÉRICO (proyectos sin ROM): modelo simple de tuning.
            val maxSpeed = if (running) t.maxRunSpeed else t.maxWalkSpeed
            if (abs(moveX) > 0.1f) {
                p.facingRight = moveX > 0
                p.vx += (if (moveX > 0) t.runAccel else -t.runAccel)
                p.vx = p.vx.coerceIn(-maxSpeed, maxSpeed)
            } else {
                // rozamiento hacia 0
                p.vx = if (p.vx > 0) max(0f, p.vx - t.friction) else min(0f, p.vx + t.friction)
            }
        }

        // --- lanzar bola de fuego: al PULSAR correr (misma tecla que en SMW) ---
        if (running && !prevRunning) { if (!handleGrabThrow()) throwFireball() }
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
        hBlocked = false
        blockedFlags = 0
        if (smwMotion != null) {
            // MODO 1:1: colisión por PUNTOS-SONDA de SMW (fija blockedFlags y aplastamiento).
            smwResolveHorizontal(p.vx)
            if (hBlocked) smwMotion.stop()
            smwResolveVertical(p.vy)
        } else {
            // MODO GENÉRICO (sin ROM): AABB por span de caja de siempre.
            moveHorizontal(p.vx)
            moveVertical(p.vy)
        }
        snapToSlope()

        if (p.invulnFrames > 0) p.invulnFrames--

        updateEnemies()
        updateItems()
        updateFireballs()
        updateGrabBlocks()
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
                // Cerradura: también gana el nivel, pero POR LA SALIDA SECRETA.
                ItemKind.GOAL_SECRET -> { won = true; wonSecret = true }
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
                    damageEnemy(e)
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
                // Jefe: patrulla + ataque propio de cada uno (ver [updateBoss]).
                EnemyBehavior.BOSS -> updateBoss(e)
                // Mechakoopa (0xA2): anda hacia Mario, re-encarándolo cada 0x40 frames.
                EnemyBehavior.MECHAKOOPA -> updateMechakoopa(e)
                // Bola de bolos de Bowser (0xA1): cae, rebota y rueda hacia Mario.
                EnemyBehavior.BOWLING_BALL -> updateBowlingBall(e)
                // Bala Bill (0x1C): vuela recto y constante, sin gravedad ni terreno.
                EnemyBehavior.BULLET_BILL -> updateBulletBill(e)
                EnemyBehavior.BOO -> updateBoo(e)
                EnemyBehavior.EERIE -> updateEerie(e)
                EnemyBehavior.SWIMMER -> updateSwimmer(e)
                EnemyBehavior.RIP_VAN_FISH -> updateRipVanFish(e)
                EnemyBehavior.WALKER -> {
                    if (e.winged) updateWingedKoopa(e)
                    else if (e.shell) updateShell(e)
                    else {
                        e.vy = min(e.vy + SPRITE_GRAVITY, SPRITE_MAX_FALL)
                        moveEnemyVertical(e)
                        moveEnemyHorizontal(e)
                        if (e.y > (rows + 3) * tileSize) e.alive = false // cayó al vacío
                    }
                }
            }
        }
        // Sub-sprites generados por jefes este frame (fuera del bucle, para no modificar al iterar).
        if (pendingSpawns.isNotEmpty()) { enemies.addAll(pendingSpawns); pendingSpawns.clear() }
    }

    /**
     * Mechakoopa (0xA2), puerto de `Spr0A2_MechaKoopa` ($03): cae con gravedad y, en el suelo,
     * ANDA a ±0x8 (0.5 px/f) hacia Mario, RE-ENCARÁNDOLO cada 0x40 fotogramas; REBOTA en las
     * PAREDES pero se tira por los bordes (no gira en los precipicios, a diferencia del andador).
     * Se puede pisar (muere). Lo genera el ataque de Bowser ([bossShoot]).
     */
    private fun updateMechakoopa(e: PlatformerEnemy) {
        // LLEVADO: pegado delante de Mario a la altura de sus manos (como [carriedBlock]).
        if (e.carried) {
            e.x = if (player.facingRight) player.x + tuning.playerWidth - 2f else player.x - e.width + 2f
            e.y = player.y + playerHeight - e.height - 1f
            return
        }
        // LANZADO: vuela recto y ARROLLA a lo que toque; se rompe contra pared o al salir.
        if (e.thrown) {
            val nx = e.x + e.vx
            val minRow = (e.y / tileSize).toInt()
            val maxRow = ((e.y + e.height - 0.01f) / tileSize).toInt()
            val frontCol = if (e.vx > 0) ((nx + e.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((minRow..maxRow).any { r -> wallsAt(frontCol, r) }) { e.alive = false; return }
            e.x = nx
            if (e.x < -16f || e.x > (cols + 1) * tileSize) { e.alive = false; return }
            for (o in enemies) {
                if (o === e || !o.alive || o.behavior == EnemyBehavior.MECHAKOOPA && (o.carried || o.thrown)) continue
                if (e.x < o.x + o.width && e.x + e.width > o.x && e.y < o.y + o.height && e.y + e.height > o.y) {
                    damageEnemy(o); stompEvents++; e.alive = false; break
                }
            }
            return
        }
        e.vy = min(e.vy + SPRITE_GRAVITY, SPRITE_MAX_FALL)
        moveEnemyVertical(e)
        // VOLTEADO (aturdido): quieto; despierta al agotar el temporizador.
        if (e.stunned) {
            e.vx = 0f
            if (--e.stunTimer <= 0) e.stunned = false
            if (e.y > (rows + 3) * tileSize) e.alive = false
            return
        }
        if (e.onGround) {
            if (e.faceTimer % 0x40 == 0) {  // re-encara a Mario (spr_table157c = pos. relativa X)
                val toLeft = (player.x + tuning.playerWidth / 2f) < (e.x + e.width / 2f)
                e.vx = if (toLeft) -MECHA_WALK else MECHA_WALK
            }
            e.faceTimer++
        }
        // Movimiento horizontal: rebota SOLO en paredes (se cae por los bordes).
        val nx = e.x + e.vx
        val minRow = (e.y / tileSize).toInt()
        val maxRow = ((e.y + e.height - 0.01f) / tileSize).toInt()
        val frontCol = if (e.vx > 0) ((nx + e.width) / tileSize).toInt() else (nx / tileSize).toInt()
        if ((minRow..maxRow).any { wallsAt(frontCol, it) }) e.vx = -e.vx else e.x = nx
        if (e.y > (rows + 3) * tileSize) e.alive = false
    }

    /**
     * Bola de bolos de Bowser (0xA1), puerto de `Spr0A1_BowserBowlingBall` ($03): cae con
     * gravedad (+3 unidades/f, tope 0x40=4 px/f), REBOTA en el suelo con altura según la
     * velocidad de caída (`kSpr0A1_BowserBowlingBall_BounceYSpeed[(caída>>2)+19]`) y RUEDA hacia
     * Mario a ±0x10 (1 px/f). Hiere por cualquier lado (no se puede pisar); se apaga al caer.
     */
    private fun updateBowlingBall(e: PlatformerEnemy) {
        e.vy = min(e.vy + BALL_GRAVITY, BALL_MAX_FALL)
        val landUnits = (e.vy * 16f).toInt()                 // velocidad de caída en unidades SMW
        moveEnemyVertical(e)                                 // pone onGround y vy=0 al aterrizar
        if (e.onGround) {
            val idx = ((landUnits shr 2) + 19).coerceIn(0, BALL_BOUNCE_YSPEED.size - 1)
            e.vy = BALL_BOUNCE_YSPEED[idx].toByte().toInt() / 16f   // rebote (negativo = arriba)
            if (e.vx == 0f) {
                e.vx = if (player.x + tuning.playerWidth / 2f < e.x + e.width / 2f) -BALL_ROLL else BALL_ROLL
            }
        }
        // Rueda; rebota en paredes.
        val nx = e.x + e.vx
        val minRow = (e.y / tileSize).toInt()
        val maxRow = ((e.y + e.height - 0.01f) / tileSize).toInt()
        val frontCol = if (e.vx > 0) ((nx + e.width) / tileSize).toInt() else (nx / tileSize).toInt()
        if (e.vx != 0f && (minRow..maxRow).any { wallsAt(frontCol, it) }) e.vx = -e.vx else e.x = nx
        if (e.y > (rows + 3) * tileSize) e.alive = false
    }

    /**
     * Conducta de un JEFE (SIMPLIFICADA, no la IA de Modo 7): Big Boo flota persiguiendo a
     * Mario como un fantasma (sin gravedad ni terreno); los demás patrullan por el suelo y
     * lanzan su ATAQUE propio cuando Mario está cerca ([bossShoot]). Cuenta la invulnerabilidad.
     */
    private fun updateBoss(e: PlatformerEnemy) {
        if (e.hurtCooldown > 0) e.hurtCooldown--
        // Bowser (0xA0): FLOTA arriba (coche-payaso) y va por FASES; conducta propia.
        if (e.id == 0xA0) { updateBowser(e); return }
        // Big Boo (0xC5): fantasma que persigue flotando, atraviesa el terreno, no dispara.
        if (e.id == 0xC5) {
            val cx = e.x + e.width / 2f; val cy = e.y + e.height / 2f
            val dx = (player.x + tuning.playerWidth / 2f) - cx
            val dy = (player.y + playerHeight / 2f) - cy
            val d = max(1f, hypot(dx, dy))
            e.x += dx / d * BIG_BOO_SPEED
            e.y += dy / d * BIG_BOO_SPEED
            return
        }
        // Resto: patrulla con gravedad + ataque propio si Mario está en rango.
        e.vy = min(e.vy + SPRITE_GRAVITY, SPRITE_MAX_FALL)
        moveEnemyVertical(e)
        moveEnemyHorizontal(e)
        if (e.y > (rows + 3) * tileSize) { e.alive = false; return }
        if (e.attackTimer > 0) { e.attackTimer--; return }
        val dx = (player.x + tuning.playerWidth / 2f) - (e.x + e.width / 2f)
        if (abs(dx) <= BOSS_ATTACK_RANGE) {
            bossShoot(e, if (dx >= 0f) 1f else -1f)
            e.attackTimer = BOSS_ATTACK_INTERVAL
        }
    }

    /**
     * Bowser (0xA0), conducta MÁS FIEL (aproximación, no la IA de Modo 7): FLOTA en la franja
     * superior del nivel (el coche-payaso), DERIVA en X para seguir a Mario y NO cae con
     * gravedad. Va por FASES alternas ([bossPhase]): una tanda de Mechakoopas (0xA2, cogibles
     * y lanzables) y una tanda de bolas de bolos (0xA1, rebotan y ruedan), [BOWSER_ATTACKS_PER_PHASE]
     * ataques por tanda con [BOSS_ATTACK_INTERVAL] de separación. El pisotón le resta HP como al
     * resto de jefes. (El descenso/embestida del original se documenta como fuera de alcance.)
     */
    private fun updateBowser(e: PlatformerEnemy) {
        // Sube/mantiene la altura de vuelo (franja superior) sin gravedad, con un bob suave.
        val targetY = BOWSER_HOVER_ROW * tileSize.toFloat()
        e.y += (targetY - e.y).coerceIn(-BOWSER_VSPEED, BOWSER_VSPEED)
        // Deriva hacia Mario en X, sin salirse del nivel.
        val marioCx = player.x + tuning.playerWidth / 2f
        val cx = e.x + e.width / 2f
        val dir = if (marioCx >= cx) 1f else -1f
        e.x = (e.x + dir * BOWSER_HOVER_SPEED).coerceIn(0f, cols * tileSize - e.width)
        // Máquina de fases: cada [BOSS_ATTACK_INTERVAL] suelta un ataque de la tanda actual;
        // al completar [BOWSER_ATTACKS_PER_PHASE] cambia de tanda (Mechakoopa <-> bola).
        if (e.attackTimer > 0) { e.attackTimer--; return }
        if (e.bossPhase == 0) spawnMechakoopa(e, dir) else dropBowlingBall(e)
        e.attackCount++
        if (e.attackCount >= BOWSER_ATTACKS_PER_PHASE) { e.attackCount = 0; e.bossPhase = 1 - e.bossPhase }
        e.attackTimer = BOSS_ATTACK_INTERVAL
    }

    /** Bowser suelta un Mechakoopa (0xA2) en arco hacia [dir]; respeta el tope de vivos. */
    private fun spawnMechakoopa(e: PlatformerEnemy, dir: Float) {
        if (enemies.count { it.alive && it.behavior == EnemyBehavior.MECHAKOOPA } >= BOSS_MAX_MECHAKOOPAS) return
        val cx = e.x + e.width / 2f
        val m = PlatformerEnemy(cx - 7f, e.y - 8f, 0xA2)
        m.vx = dir * MECHA_TOSS_VX
        m.vy = MECHA_TOSS_VY
        pendingSpawns.add(m)
        piranhaFireEvents++
    }

    /** Bowser deja caer una bola de bolos (0xA1) desde su posición; rebota y rueda al aterrizar. */
    private fun dropBowlingBall(e: PlatformerEnemy) {
        val cx = e.x + e.width / 2f
        val b = PlatformerEnemy(cx - 8f, e.y, 0xA1)
        b.vy = BALL_DROP_VY
        pendingSpawns.add(b)
        piranhaFireEvents++
    }

    /**
     * Ataque de proyectil del jefe hacia Mario ([dir] = ±1, sentido a Mario). Reznor (0xA9) y el
     * Koopaling (0x29): bola de fuego APUNTADA al jugador y RECTA, con la misma matemática y
     * velocidad que el juego (`Spr0A9_Reznor_ReznorFireRt` → `AimTowardsPlayer` con 0x10). El
     * vector [SmwSpriteAim] va en unidades SMW (÷16 = px/f). (Bowser tiene su propia [updateBowser].)
     */
    private fun bossShoot(e: PlatformerEnemy, dir: Float) {
        val cx = e.x + e.width / 2f
        val cy = e.y + e.height / 2f
        if (e.id == 0xA9) {
            // Reznor: bola de fuego APUNTADA a Mario (AimTowardsPlayer, 0x10), recta.
            if (enemyProjectiles.count { it.alive } >= 6) return
            val dx = (player.x + tuning.playerWidth / 2f - cx).toInt()
            val dy = (player.y + playerHeight / 2f - cy).toInt()
            val (xs, ys) = SmwSpriteAim.aimTowardsPlayer(dx, dy, REZNOR_FIRE_SPEED)
            enemyProjectiles.add(EnemyProjectile(cx, cy, xs / 16f, ys / 16f, arc = false))
            piranhaFireEvents++
        } else {
            // Koopaling (0x29): bola de Ludwig (0x34) HORIZONTAL hacia Mario a 0x20 (2 px/f),
            // como `Spr029..._01D059` → `Spr034_LudwigFireball` (kSpr029..._DATA_01D0BE = ±0x20).
            if (enemyProjectiles.count { it.alive } >= 6) return
            enemyProjectiles.add(EnemyProjectile(cx, cy, dir * LUDWIG_FIRE_SPEED, 0f, arc = false))
            piranhaFireEvents++
        }
    }

    /**
     * Bala Bill (`Spr01C_BulletBill`, $01:8FE7): vuela en LÍNEA RECTA a velocidad constante,
     * **sin gravedad y sin colisión con el terreno** (usa `UpdateNormalSpritePosition` directo,
     * no `HandleNormalSpriteGravity`). La dirección se fija la primera vez hacia el lado de
     * Mario, que es como el generador la lanza. Se apaga al salir de la pantalla; se pisa como
     * un andador.
     */
    private fun updateBulletBill(e: PlatformerEnemy) {
        if (e.vx == 0f) {
            e.vx = if (player.x + tuning.playerWidth / 2f < e.x) -BULLET_BILL_SPEED else BULLET_BILL_SPEED
        }
        e.x += e.vx // recto: ni gravedad ni paredes lo paran
        if (e.x < -32f || e.x > (cols + 2) * tileSize) e.alive = false
    }

    /**
     * BOO (0x37): el fantasma TÍMIDO de SMW. Persigue a Mario **solo cuando no lo está
     * mirando**; en cuanto Mario le da la cara, se queda quieto (y en el juego se tapa). No
     * le afecta la gravedad, atraviesa el terreno y NO se puede pisar: es invencible.
     *
     * "Mirar" = Mario está encarado HACIA el Boo. Se compara la posición del Boo con la de
     * Mario y su [PlatformerPlayer.facingRight], igual que la condición del juego.
     */
    private fun updateBoo(e: PlatformerEnemy) {
        val pcx = player.x + tuning.playerWidth / 2f
        val ecx = e.x + e.width / 2f
        val booIsToTheRight = ecx > pcx
        // Mario lo mira si está encarado hacia el lado donde está el Boo.
        val looking = if (booIsToTheRight) player.facingRight else !player.facingRight
        if (looking) return // tímido: se queda clavado mientras lo miras
        val dx = pcx - ecx
        val dy = (player.y + playerHeight / 2f) - (e.y + e.height / 2f)
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        e.x += BOO_SPEED * dx / len
        e.y += BOO_SPEED * dy / len
    }

    /**
     * EERIE (0x38/0x39): vuela RECTO en horizontal atravesando el terreno, ondulando en
     * vertical alrededor de la altura donde apareció. Sin gravedad. Se apaga al salir del mapa.
     */
    private fun updateEerie(e: PlatformerEnemy) {
        if (e.vx == 0f) {
            e.vx = if (player.x + tuning.playerWidth / 2f < e.x) -EERIE_SPEED else EERIE_SPEED
        }
        e.x += e.vx
        e.pTimer++
        // Onda vertical suave alrededor de su altura de origen.
        e.y = e.spawnY + EERIE_WAVE_AMPLITUDE *
            kotlin.math.sin(e.pTimer * EERIE_WAVE_SPEED.toDouble()).toFloat()
        if (e.x < -32f || e.x > (cols + 2) * tileSize) e.alive = false
    }

    /**
     * CHEEP-CHEEP (0x15/0x16): NADA. Avanza en horizontal ondulando, sin gravedad ni suelo, y
     * da la vuelta al topar con una pared (el agua lo contiene). Se puede pisar como un andador.
     */
    private fun updateSwimmer(e: PlatformerEnemy) {
        if (e.vx == 0f) e.vx = -SWIM_SPEED
        val nx = e.x + e.vx
        // Rebota contra el terreno sólido (bordes de la zona de agua), como el andador.
        val frontCol = if (e.vx > 0) ((nx + e.width) / tileSize).toInt() else (nx / tileSize).toInt()
        val midRow = ((e.y + e.height / 2f) / tileSize).toInt()
        if (wallsAt(frontCol, midRow)) e.vx = -e.vx else e.x = nx
        e.pTimer++
        e.y = e.spawnY + SWIM_WAVE_AMPLITUDE *
            kotlin.math.sin(e.pTimer * SWIM_WAVE_SPEED.toDouble()).toFloat()
    }

    /**
     * RIP VAN FISH (0x3D): DUERME quieto hasta que Mario pasa cerca ([RIPVAN_WAKE_DIST]);
     * entonces despierta y lo PERSIGUE nadando, sin gravedad. Una vez despierto no se vuelve a
     * dormir (como en el juego, que te persigue hasta que lo pierdes de vista).
     */
    private fun updateRipVanFish(e: PlatformerEnemy) {
        val pcx = player.x + tuning.playerWidth / 2f
        val pcy = player.y + playerHeight / 2f
        val ecx = e.x + e.width / 2f
        val ecy = e.y + e.height / 2f
        val dx = pcx - ecx
        val dy = pcy - ecy
        if (e.pState == 0) {
            // Dormido: solo despierta si Mario se acerca lo bastante.
            if (kotlin.math.abs(dx) > RIPVAN_WAKE_DIST || kotlin.math.abs(dy) > RIPVAN_WAKE_DIST) return
            e.pState = 1
        }
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        e.x += RIPVAN_SPEED * dx / len
        e.y += RIPVAN_SPEED * dy / len
    }

    /**
     * Caparazón de Koopa: cae con gravedad; si está PATEADO ([shellMoving]) se desliza
     * rápido, REBOTA en las paredes y se cae por los bordes (no gira como el andador), y
     * ARROLLA a los demás enemigos que toca (la cadena de caparazón de SMW). Si está
     * quieto, solo se posa. Se apaga al caer al vacío.
     */
    private fun updateShell(e: PlatformerEnemy) {
        e.vy = min(e.vy + SPRITE_GRAVITY, SPRITE_MAX_FALL)
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

    /**
     * Desplazamiento en X de MARIO por una velocidad SMW (byte alto de la palabra `$7A/$7B`,
     * en 1/16 px con signo), con el mismo acumulador de subpíxeles que [smwStepX]. Devuelve
     * los píxeles a mover este fotograma; el motor los pasa por [moveHorizontal] (colisión).
     */
    private fun bodyStepX(speed: Int): Float {
        val s = speed and 0xFF
        val spx = player.xSub + ((s shl 4) and 0xFF)
        player.xSub = spx and 0xFF
        val carry = spx shr 8
        var intPart = (s shr 4) and 0x0F
        if (intPart >= 8) intPart -= 16
        return (intPart + carry).toFloat()
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
                e.vy = min(e.vy + SPRITE_GRAVITY, SPRITE_MAX_FALL)
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
            if (f.arc) f.vy = min(f.vy + PIRANHA_FIRE_G, 4f)  // gravedad de Hammer (los rectos no)
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
        // BORDES: aquí los andadores NO son todos iguales. En SMW el Koopa ROJO se da la vuelta
        // al llegar a un precipicio y el VERDE (y el resto de andadores) se tira sin más. Antes
        // giraban todos, así que el verde se comportaba como el rojo.
        val footRow = ((e.y + e.height + 1f) / tileSize).toInt()
        val ledge = e.turnsAtLedge && e.onGround && !blocksFloor(solidity(frontCol, footRow))
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
    /**
     * Aplica un golpe (pisotón, caparazón, bloque lanzado o bola de fuego) a un enemigo.
     * Los jefes tienen [PlatformerEnemy.hp] y solo mueren al agotarlo (con invulnerabilidad
     * entre golpes); el resto muere de un golpe. Devuelve true si el enemigo ha MUERTO.
     */
    private fun damageEnemy(e: PlatformerEnemy): Boolean {
        if (e.isBoss) {
            if (e.hurtCooldown > 0) return false
            e.hurtCooldown = BOSS_HURT_COOLDOWN
            if (--e.hp > 0) return false
            e.alive = false; e.squashTimer = 20
            return true
        }
        e.alive = false; e.squashTimer = 12
        return true
    }

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
                // Bola de bolos de Bowser: no se puede pisar, hiere por cualquier lado.
                e.behavior == EnemyBehavior.BOWLING_BALL -> { hurtPlayer(); return }
                // Mechakoopa: pisarlo lo VOLTEA (aturdido, no muere); volteado es inofensivo y
                // se puede COGER corriendo por encima (ver [handleGrabThrow]). Pisar uno ya
                // volteado solo rebota. De lado, uno de pie hiere a Mario. (Como en SMW.)
                e.behavior == EnemyBehavior.MECHAKOOPA -> {
                    if (e.stunned) {
                        if (stompFromAbove) bounceMario()
                        // volteado y tocado de lado: inofensivo (se recoge con el botón de correr).
                    } else if (stompFromAbove) {
                        e.stunned = true; e.stunTimer = MECHA_STUN; e.vx = 0f
                        bounceMario(); stompEvents++
                    } else { hurtPlayer(); return }
                }
                // Jefe: pisarlo le quita un punto de vida (rebota a Mario, con invulnerabilidad
                // entre golpes); tocarlo de lado hiere a Mario. Muere al agotar el HP.
                e.isBoss -> {
                    if (stompFromAbove) {
                        damageEnemy(e); bounceMario(); stompEvents++
                    } else { hurtPlayer(); return }
                }
                // Resto: los andadores y la Bala Bill se pisan; las Plantas Piraña muerden por
                // cualquier lado.
                else -> {
                    // Los peces SÍ se pisan; el Boo y el Eerie NO (en SMW son invencibles: no
                    // hay pisotón que valga, solo esquivarlos), así que hieren por cualquier lado.
                    val stompable = e.behavior == EnemyBehavior.WALKER ||
                        e.behavior == EnemyBehavior.BULLET_BILL ||
                        e.behavior == EnemyBehavior.SWIMMER ||
                        e.behavior == EnemyBehavior.RIP_VAN_FISH
                    val stomp = stompable && stompFromAbove
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

    /**
     * Golpe de bloque `?` desde abajo (celda col,row): si es PRIZE, suelta el premio y lo
     * deja "usado". Devuelve true si era un premio. Premio progresivo (diseño, no dato ROM):
     * SETA si pequeño; FLOR si grande; PLUMA si ya tiene fuego; MONEDA si ya tiene capa.
     */
    private fun tryBonk(col: Int, row: Int): Boolean {
        if (blockActionAt(col, row) != BlockAction.PRIZE) return false
        setAction(col, row, BlockAction.NONE)
        val px = col * tileSize + 1f
        val py = row * tileSize - 15f
        val p = player
        when {
            !p.big -> items.add(PowerupItem(px, py, PowerupKind.MUSHROOM))
            !p.fire && !p.cape -> items.add(PowerupItem(px, py, PowerupKind.FIRE_FLOWER))
            !p.cape -> items.add(PowerupItem(px, py, PowerupKind.CAPE_FEATHER))
            else -> { coins++; coinEvents++ }
        }
        return true
    }

    // --- Colisión 1:1 por PUNTOS-SONDA de SMW (RunPlayerBlockCode, $00:EADB) ---------------
    // El juego no barre el borde entero de la caja: muestrea el mapa en puntos de offset fijo
    // (izq./der. del cuerpo para paredes, dos puntos de pies para el suelo, uno de cabeza
    // para el techo), fija player_blocked_flags ($77) y mata por aplastamiento (& 0x1C). Aquí
    // se porta ese modelo para las clases de bloque que el motor tiene (sólido/un-sentido/
    // cuesta/pincho + block-code); pipes, throw-blocks, wall-run, agua y Yoshi quedan fuera
    // (subsistemas ausentes en el motor), igual que otras rutinas específicas del proyecto.

    /** Fracción del alto del cuerpo donde van las dos sondas de pared (como los puntos de SMW). */
    private fun wallProbeRows(top: Float, h: Float): IntArray {
        val upper = ((top + h * 0.25f) / tileSize).toInt()
        val lower = ((top + h * 0.80f) / tileSize).toInt()
        return if (upper == lower) intArrayOf(upper) else intArrayOf(upper, lower)
    }

    /** Resolvedor horizontal por sondas laterales; fija BLOCKED_LEFT/RIGHT y corta la velocidad. */
    private fun smwResolveHorizontal(dx: Float) {
        if (dx == 0f) return
        val p = player
        val w = tuning.playerWidth
        val h = playerHeight
        var nx = p.x + dx
        val onSlope = supportSlopeOffsets() != null
        val rows = wallProbeRows(p.y, h)
        fun tryMove(col: Int, clampX: Float, flag: Int) {
            val blockers = rows.filter { wallsAt(col, it) }
            if (blockers.isEmpty()) return
            val stepTop = blockers.min() * tileSize.toFloat()
            if (onSlope && (p.y + h) - stepTop <= 8f) {
                p.y = stepTop - h - 0.01f // corona el escalón de la cuesta (sensor central)
            } else {
                nx = clampX
                p.vx = 0f
                hBlocked = true
                blockedFlags = blockedFlags or flag
            }
        }
        if (dx > 0) {
            val col = ((nx + w) / tileSize).toInt()
            tryMove(col, col * tileSize - w - 0.01f, BLOCKED_RIGHT)
        } else {
            val col = (nx / tileSize).toInt()
            tryMove(col, (col + 1) * tileSize + 0.01f, BLOCKED_LEFT)
        }
        p.x = nx
    }

    /** Resolvedor vertical por sondas de pies (dos, izq./der. del centro) y cabeza (centro). */
    private fun smwResolveVertical(dy: Float) {
        val p = player
        val w = tuning.playerWidth
        val h = playerHeight
        var ny = p.y + dy
        p.onGround = false
        // Sondas de pies: izquierda y derecha del centro (como los dos puntos de suelo de SMW).
        val footL = p.x + w * 0.25f
        val footR = p.x + w * 0.75f
        val centerX = p.x + w / 2f
        if (dy > 0) {
            val prevBottom = p.y + h
            // Cuesta bajo el centro (perfil sub-píxel, ya 1:1).
            var landed = false
            run {
                val ccol = (centerX / tileSize).toInt()
                val rowTo = ((ny + h) / tileSize).toInt()
                for (r in (prevBottom / tileSize).toInt()..rowTo) {
                    val surf = slopeSurfaceY(ccol, r, centerX) ?: continue
                    if (ny + h >= surf) { ny = surf - h - 0.01f; p.vy = 0f; p.onGround = true; landed = true; break }
                }
            }
            if (!landed) {
                val row = ((ny + h) / tileSize).toInt()
                val tileTop = row * tileSize
                val hit = intArrayOf((footL / tileSize).toInt(), (footR / tileSize).toInt()).any { c ->
                    val s = solidity(c, row)
                    floorsAt(c, row) && (s != SmwSolidity.LEDGE_TOP || prevBottom <= tileTop + 0.01f)
                }
                if (hit) { ny = tileTop - h - 0.01f; p.vy = 0f; p.onGround = true; blockedFlags = blockedFlags or BLOCKED_DOWN }
            }
        } else if (dy < 0) {
            // Techo: sonda de cabeza (dos puntos, esquinas superiores), solo sólidos.
            val row = (ny / tileSize).toInt()
            val hitCols = intArrayOf((footL / tileSize).toInt(), (footR / tileSize).toInt())
                .filter { solidity(it, row) == SmwSolidity.SOLID }
            if (hitCols.isNotEmpty()) {
                ny = (row + 1) * tileSize + 0.01f
                p.vy = 0f
                blockedFlags = blockedFlags or BLOCKED_UP
                for (c in hitCols) if (tryBonk(c, row)) break
            }
        }
        p.y = ny
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
                hBlocked = true
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
                // Bloque '?': el golpe desde abajo suelta el premio. Un cabezazo = un bloque.
                for (c in hitCols) if (tryBonk(c, row)) break
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
        /** Bits de [blockedFlags] (modelo de `player_blocked_flags` $77 de SMW). */
        const val BLOCKED_RIGHT = 0x1
        const val BLOCKED_LEFT = 0x2
        const val BLOCKED_UP = 0x4
        const val BLOCKED_DOWN = 0x8

        /** Velocidad del bloque de agarrar al LANZARLO (px/f), estilo SMW. */
        const val GRAB_THROW_SPEED = 4f
        /** Alcance (px) desde el frente de Mario para coger un bloque quieto. */
        const val GRAB_REACH = 4f

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

        // ---- Jefes (conducta SIMPLIFICADA: enemigo grande y resistente, no la IA de Modo 7). ----
        /** Puntos de vida de un jefe: aguanta varios golpes (pisotón, caparazón, bloque o fuego). */
        const val BOSS_HP = 5
        /** Fotogramas de invulnerabilidad del jefe entre golpes (para no gastar todo el HP de una). */
        const val BOSS_HURT_COOLDOWN = 40
        /** Caja de colisión del jefe (px). Grande respecto al andador; editable en caliente. */
        const val BOSS_WIDTH = 28f
        const val BOSS_HEIGHT = 30f
        /** Velocidad de patrulla del jefe (px/f): lenta y pesada. */
        const val BOSS_SPEED = 0.35f
        /** Fotogramas entre ataques del jefe (bola de fuego / lanzamiento). */
        const val BOSS_ATTACK_INTERVAL = 100
        /** Solo ataca si Mario está a menos de este alcance horizontal (px). */
        const val BOSS_ATTACK_RANGE = 220f
        /** Velocidad de la bola de fuego de Reznor en unidades SMW (0x10 = 1 px/f en el eje
         *  dominante), como `Spr0A9_Reznor_ReznorFireRt` → `AimTowardsPlayer(k, 0x10)`. */
        const val REZNOR_FIRE_SPEED = 0x10
        /** Bola de Ludwig del Koopaling: horizontal a 0x20 = 2 px/f (`..._DATA_01D0BE`). */
        const val LUDWIG_FIRE_SPEED = 2f
        /** Lanzamiento en ARCO de Bowser: ±2 px/f horizontal, −3.5 px/f hacia arriba (arquea). */
        const val BOSS_LOB_VX = 2f
        const val BOSS_LOB_VY = -3.5f
        /** Velocidad de flotación de Big Boo persiguiendo a Mario (px/f). */
        const val BIG_BOO_SPEED = 0.8f
        /** Mechakoopa: anda a 0x8 = 0.5 px/f (`kSpr0A2_MechaKoopa_XSpeed` = ±8 unidades SMW). */
        const val MECHA_WALK = 0.5f
        /** Fotogramas que el Mechakoopa queda VOLTEADO (aturdido) tras pisarlo, tiempo para
         *  cogerlo antes de que se enderece (`sprite_misc_1594` cuenta atrás en `Spr0A2`). */
        const val MECHA_STUN = 0x100
        /** Impulso con que Bowser SUELTA el Mechakoopa hacia Mario (px/f): sale en arco. */
        const val MECHA_TOSS_VX = 1.2f
        const val MECHA_TOSS_VY = -3f
        /** Máximo de Mechakoopas vivos que Bowser mantiene a la vez. */
        const val BOSS_MAX_MECHAKOOPAS = 3
        /** Bowser flotante: fila objetivo de vuelo (franja superior), deriva en X hacia Mario y
         *  velocidad con que gana altura. Fases de [BOWSER_ATTACKS_PER_PHASE] ataques cada una. */
        const val BOWSER_HOVER_ROW = 2
        const val BOWSER_HOVER_SPEED = 0.4f
        const val BOWSER_VSPEED = 0.5f
        const val BOWSER_ATTACKS_PER_PHASE = 3
        /** Bola de bolos: gravedad +3 unidades/f y tope 0x40 (=4 px/f); rueda a 0x10 (=1 px/f). */
        const val BALL_GRAVITY = 3f / 16f
        const val BALL_MAX_FALL = 4f
        const val BALL_ROLL = 1f
        /** Impulso vertical con que Bowser deja caer la bola (px/f, hacia arriba antes de caer). */
        const val BALL_DROP_VY = -1.5f
        /** `kSpr0A1_BowserBowlingBall_BounceYSpeed` ($03): rebote (unidades SMW) por vel. de caída;
         *  la bola usa índice `(caída>>2)+19`. A más caída, más alto rebota; 0 = deja de botar. */
        val BALL_BOUNCE_YSPEED = intArrayOf(
            0x0, 0x0, 0x0, 0xf8, 0xf8, 0xf8, 0xf8, 0xf8, 0xf8, 0xf7, 0xf6, 0xf5, 0xf4, 0xf3, 0xf2,
            0xe8, 0xe8, 0xe8, 0xe8, 0x0, 0x0, 0x0, 0x0, 0xfe, 0xfc, 0xf8, 0xec, 0xec, 0xec, 0xe8,
            0xe4, 0xe0, 0xdc, 0xd8, 0xd4, 0xd0, 0xcc, 0xc8,
        )
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

        // ---- Gravedad de SPRITES (SubUpdateSprPos $01): valores EXACTOS de SMW ----
        /** `DATA_019030` = 0x03/frame = 3/16 = **0.1875 px/f²** (los sprites caen más suave que Mario). */
        const val SPRITE_GRAVITY = 3f / 16f
        /** `DATA_01902E` = 0x40 = 64/16 = **4 px/f** de caída terminal de un sprite. */
        const val SPRITE_MAX_FALL = 0x40 / 16f
        /**
         * Bala Bill (`kSpr01C_BulletBill_XSpeed`, $01:8FE7): 0x20 en unidades de SMW (1/16 px)
         * = **2 px/f**. Vuela recto y constante, sin gravedad ni colisión con el terreno.
         */
        const val BULLET_BILL_SPEED = 0x20 / 16f

        // --- Lote de enemigos FLOTANTES (sin gravedad). Velocidades en px/frame; son
        // aproximaciones calibradas "a ojo de juego", no tablas de la ROM: se marcan como tales.
        /** Boo: persecución lenta mientras no lo miras. */
        const val BOO_SPEED = 0.55f

        /** Eerie: avance horizontal constante. */
        const val EERIE_SPEED = 0.9f

        /** Eerie: amplitud (px) y velocidad de su onda vertical. */
        const val EERIE_WAVE_AMPLITUDE = 10f
        const val EERIE_WAVE_SPEED = 0.06f

        /** Cheep-Cheep: avance y onda al nadar. */
        const val SWIM_SPEED = 0.6f
        const val SWIM_WAVE_AMPLITUDE = 6f
        const val SWIM_WAVE_SPEED = 0.08f

        /** Rip Van Fish: distancia (px) a la que despierta y velocidad con la que persigue. */
        const val RIPVAN_WAKE_DIST = 64f
        const val RIPVAN_SPEED = 0.7f
    }
}
