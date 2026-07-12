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
}

/** Semilla de un enemigo: posición inicial en píxeles e id de sprite SMW. */
class EnemySeed(val xPixel: Int, val yPixel: Int, val id: Int)

/**
 * Acción interactiva de una celda del mapa, a nivel de motor (independiente de la
 * clasificación de bloques Map16 de SMW, que se mapea a esto al montar el nivel):
 *  - [NONE]: sin interacción.
 *  - [COIN]: se recoge al tocarla (suma moneda y desaparece).
 *  - [PRIZE]: bloque `?`: sólido; al golpearlo desde abajo suelta una moneda y queda
 *    "usado" (sigue sólido pero ya no premia).
 */
enum class BlockAction { NONE, COIN, PRIZE }

private val BLOCK_ACTIONS = BlockAction.values()

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
 *  - [SmwSolidity.SPIKE] mata al jugador al tocarla.
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
) {
    val player = PlatformerBody(startPixelX.toFloat(), startPixelY.toFloat())

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

    /** Marca al jugador como muerto una sola vez y cuenta el evento. */
    private fun killPlayer() {
        if (!player.dead) {
            player.dead = true
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
        if (p.dead) return
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

        // --- integración con colisión, eje a eje ---
        moveHorizontal(p.vx)
        moveVertical(p.vy)

        updateEnemies()
        handlePlayerEnemyContact()
        collectCoins()

        checkDeadly()
        if (p.y > (rows + 2) * tileSize) killPlayer() // caído al vacío
    }

    /** Recoge las monedas sueltas que solape la caja del jugador. */
    private fun collectCoins() {
        if (actions == null) return
        val p = player
        val w = tuning.playerWidth
        val h = tuning.playerHeight
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
        val ph = tuning.playerHeight
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
                killPlayer()
                return
            }
        }
    }

    private fun moveHorizontal(dx: Float) {
        if (dx == 0f) return
        val p = player
        val w = tuning.playerWidth
        val h = tuning.playerHeight
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
        val h = tuning.playerHeight
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
                // Bloque '?': el golpe desde abajo suelta una moneda y lo deja "usado"
                // (sigue sólido; su solidez no cambia). Un cabezazo = un bloque.
                for (c in hitCols) {
                    if (blockActionAt(c, row) == BlockAction.PRIZE) {
                        setAction(c, row, BlockAction.NONE)
                        coins++
                        coinEvents++
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
        val h = tuning.playerHeight
        val c0 = (p.x / tileSize).toInt()
        val c1 = ((p.x + w - 0.01f) / tileSize).toInt()
        val r0 = (p.y / tileSize).toInt()
        val r1 = ((p.y + h - 0.01f) / tileSize).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            if (solidity(c, r) == SmwSolidity.SPIKE) { killPlayer(); return }
        }
    }
}
