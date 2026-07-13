package com.rolebuilder.core.engine.platformer

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
 * Acción interactiva de una celda del mapa, a nivel de motor (independiente de la
 * clasificación de bloques Map16 de SMW, que se mapea a esto al montar el nivel):
 *  - [NONE]: sin interacción.
 *  - [COIN]: se recoge al tocarla (suma moneda y desaparece).
 *  - [PRIZE]: bloque `?`: sólido; al golpearlo desde abajo suelta el premio (SETA si
 *    el jugador es pequeño, moneda si ya es grande) y queda "usado" (sigue sólido
 *    pero ya no premia).
 */
enum class BlockAction { NONE, COIN, PRIZE }

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
    val width = 14f
    val height = 14f
    var vx = -0.5f    // patrulla: arranca hacia la izquierda, como los Goomba de SMW
    var vy = 0f
    var onGround = false
    var alive = true
    /** Fotogramas que sigue visible "aplastado" tras el pisotón antes de desaparecer. */
    var squashTimer = 0
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
 *  - [SmwSolidity.SLOPE]/[SmwSolidity.SLOPE_STEEP] se tratan de momento como sólidas
 *    (bloque completo); la forma sub-píxel de la cuesta queda para un refinamiento.
 *  - [SmwSolidity.SPIKE] daña al jugador al tocarla (grande encoge; pequeño muere).
 *  - [SmwSolidity.NONE] no colisiona.
 */
class PlatformerEngine(
    val cols: Int,
    val rows: Int,
    private val solidityAt: (col: Int, row: Int) -> SmwSolidity,
    startPixelX: Int,
    startPixelY: Int,
    val tuning: PlatformerTuning,
    enemySeeds: List<EnemySeed> = emptyList(),
    blockActions: IntArray? = null,
    warps: List<EngineWarp> = emptyList(),
) {
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
    private val smallHeight =
        if (tuning.playerHeight >= BIG_HEIGHT) SMALL_HEIGHT else tuning.playerHeight

    /** Powerups en marcha (seta/flor; vivas o no; la capa de dibujo filtra por alive). */
    val items = ArrayList<PowerupItem>()

    /** Bolas de fuego en vuelo lanzadas por Mario de fuego. */
    val fireballs = ArrayList<Fireball>()

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

        // --- horizontal: aceleración hacia el tope, o rozamiento ---
        val maxSpeed = if (running) t.maxRunSpeed else t.maxWalkSpeed
        if (abs(moveX) > 0.1f) {
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

        if (p.invulnFrames > 0) p.invulnFrames--

        updateEnemies()
        updateItems()
        updateFireballs()
        handlePlayerEnemyContact()
        collectCoins()
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
            if (blockActionAt(c, r) == BlockAction.COIN) {
                setAction(c, r, BlockAction.NONE)
                coins++
                coinEvents++
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
            // Vertical: gravedad y aterrizaje (mismos criterios que los enemigos).
            m.vy = min(m.vy + tuning.gravityFall, tuning.maxFallSpeed)
            var ny = m.y + m.vy
            val minCol = (m.x / tileSize).toInt()
            val maxCol = ((m.x + m.width - 0.01f) / tileSize).toInt()
            m.onGround = false
            if (m.vy > 0) {
                val row = ((ny + m.height) / tileSize).toInt()
                if ((minCol..maxCol).any { c -> blocksFloor(solidity(c, row)) }) {
                    ny = row * tileSize - m.height - 0.01f
                    m.vy = 0f
                    m.onGround = true
                }
            }
            m.y = ny
            // Horizontal: rebota SOLO en paredes (por los bordes se cae, no se gira).
            val nx = m.x + m.vx
            val minRow = (m.y / tileSize).toInt()
            val maxRow = ((m.y + m.height - 0.01f) / tileSize).toInt()
            val frontCol = if (m.vx > 0) ((nx + m.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((minRow..maxRow).any { r -> blocksSide(solidity(frontCol, r)) }) m.vx = -m.vx
            else m.x = nx
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
            // Vertical: gravedad y rebote contra el suelo.
            fb.vy = min(fb.vy + tuning.gravityFall, tuning.maxFallSpeed)
            var ny = fb.y + fb.vy
            val minCol = (fb.x / tileSize).toInt()
            val maxCol = ((fb.x + fb.width - 0.01f) / tileSize).toInt()
            if (fb.vy > 0) {
                val row = ((ny + fb.height) / tileSize).toInt()
                if ((minCol..maxCol).any { c -> blocksFloor(solidity(c, row)) }) {
                    ny = row * tileSize - fb.height - 0.01f
                    fb.vy = -2.5f // rebota hacia arriba
                }
            }
            fb.y = ny
            // Horizontal: al chocar con una pared se apaga.
            val nx = fb.x + fb.vx
            val minRow = (fb.y / tileSize).toInt()
            val maxRow = ((fb.y + fb.height - 0.01f) / tileSize).toInt()
            val frontCol = if (fb.vx > 0) ((nx + fb.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((minRow..maxRow).any { r -> blocksSide(solidity(frontCol, r)) }) {
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
            e.vy = min(e.vy + tuning.gravityFall, tuning.maxFallSpeed)
            moveEnemyVertical(e)
            moveEnemyHorizontal(e)
            if (e.y > (rows + 3) * tileSize) e.alive = false // cayó al vacío
        }
    }

    private fun moveEnemyHorizontal(e: PlatformerEnemy) {
        val nx = e.x + e.vx
        val top = e.y
        val bottom = e.y + e.height - 0.01f
        val minRow = (top / tileSize).toInt()
        val maxRow = (bottom / tileSize).toInt()
        val frontCol = if (e.vx > 0) ((nx + e.width) / tileSize).toInt() else (nx / tileSize).toInt()
        val wall = (minRow..maxRow).any { blocksSide(solidity(frontCol, it)) }
        // Se da la vuelta en el borde de una plataforma (no se tira al vacío), como en SMW.
        val footRow = ((e.y + e.height + 1f) / tileSize).toInt()
        val ledge = e.onGround && !blocksFloor(solidity(frontCol, footRow))
        if (wall || ledge) e.vx = -e.vx else e.x = nx
    }

    private fun moveEnemyVertical(e: PlatformerEnemy) {
        var ny = e.y + e.vy
        val left = e.x
        val right = e.x + e.width - 0.01f
        val minCol = (left / tileSize).toInt()
        val maxCol = (right / tileSize).toInt()
        e.onGround = false
        if (e.vy > 0) {
            val row = ((ny + e.height) / tileSize).toInt()
            if ((minCol..maxCol).any { blocksFloor(solidity(it, row)) }) {
                ny = row * tileSize - e.height - 0.01f
                e.vy = 0f
                e.onGround = true
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
            val overlap = p.x < e.x + e.width && p.x + pw > e.x &&
                p.y < e.y + e.height && p.y + ph > e.y
            if (!overlap) continue
            // Viene cayendo y sus pies están cerca de la cabeza del enemigo → pisotón.
            val stomp = p.vy > 0f && (p.y + ph) - e.y < e.height * 0.6f
            if (stomp) {
                e.alive = false
                e.squashTimer = 12
                p.vy = tuning.jumpSpeed * 0.6f // rebote
                p.onGround = false
                p.jumping = false
                stompEvents++
            } else {
                hurtPlayer() // grande: encoge con invulnerabilidad; pequeño: muere
                return
            }
        }
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
        if (dx > 0) {
            val col = ((nx + w) / tileSize).toInt()
            if ((minRow..maxRow).any { blocksSide(solidity(col, it)) }) {
                nx = col * tileSize - w - 0.01f
                p.vx = 0f
            }
        } else {
            val col = (nx / tileSize).toInt()
            if ((minRow..maxRow).any { blocksSide(solidity(col, it)) }) {
                nx = (col + 1) * tileSize + 0.01f
                p.vx = 0f
            }
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
            // Cayendo: pisa suelo. Los bordes de un sentido solo cuentan si los
            // pies cruzan el borde superior de la celda desde arriba este fotograma.
            val prevBottom = p.y + h
            val row = ((ny + h) / tileSize).toInt()
            val tileTop = row * tileSize
            val hit = (minCol..maxCol).any {
                val s = solidity(it, row)
                blocksFloor(s) && (s != SmwSolidity.LEDGE_TOP || prevBottom <= tileTop + 0.01f)
            }
            if (hit) {
                ny = row * tileSize - h - 0.01f
                p.vy = 0f
                p.onGround = true
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
    }
}
