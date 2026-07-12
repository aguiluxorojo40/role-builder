package com.rolebuilder.core.engine.platformer

import com.rolebuilder.core.snes.SmwBlockBehavior
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
    /** Estado de power-up: false = pequeño, true = grande (caja más alta). */
    var big = false
    /** Fotogramas de invulnerabilidad tras encoger (para no morir el mismo frame). */
    var invuln = 0
}

/** Semilla de un enemigo: posición inicial en píxeles e id de sprite SMW. */
class EnemySeed(val xPixel: Int, val yPixel: Int, val id: Int)

/** Power-up (seta) que suelta un ? bloque: cae, patrulla y crece al jugador si lo toca. */
class Powerup(var x: Float, var y: Float) {
    val width = 14f
    val height = 14f
    var vx = 0.75f
    var vy = 0f
    var alive = true
}

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
    /** Comportamiento interactivo de la celda (moneda/meta), del tileset importado. */
    private val behaviorAt: (col: Int, row: Int) -> SmwBlockBehavior = { _, _ -> SmwBlockBehavior.NONE },
) {
    val player = PlatformerBody(startPixelX.toFloat(), startPixelY.toFloat())

    /** Enemigos vivos del nivel, instanciados de las semillas. */
    val enemies: List<PlatformerEnemy> =
        enemySeeds.map { PlatformerEnemy(it.xPixel.toFloat(), it.yPixel.toFloat(), it.id) }

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

    /** Monedas recogidas y evento monótono para el audio/HUD. */
    var coins = 0
        private set
    var coinEvents = 0
        private set

    /** true cuando el jugador alcanza la META; evento monótono para el remate del nivel. */
    var goalReached = false
        private set
    var goalEvents = 0
        private set

    /** Celdas cuyo coleccionable ya se recogió (clave col*rows+row), para no re-cogerlo. */
    private val consumed = HashSet<Int>()

    /** Power-ups sueltos en el nivel (los que soltaron los ? bloques). */
    val powerups = ArrayList<Powerup>()

    /** Eventos monótonos para el audio: ? bloque golpeado y jugador que crece. */
    var prizeEvents = 0
        private set
    var growEvents = 0
        private set

    /** Alto EFECTIVO de la caja del jugador: mayor cuando es grande (crece hacia arriba). */
    private val bigHeight = 26f
    private fun playerH(): Float = if (player.big) bigHeight else tuning.playerHeight

    /** Hace crecer al jugador conservando los pies (la caja se estira hacia arriba). */
    private fun growPlayer() {
        if (!player.big) {
            player.big = true
            player.y -= (bigHeight - tuning.playerHeight)
            growEvents++
        }
    }

    /** Golpe al jugador: si es grande, encoge con invulnerabilidad; si es pequeño, muere. */
    private fun hurtPlayer() {
        if (player.invuln > 0) return
        if (player.big) {
            player.big = false
            player.y += (bigHeight - tuning.playerHeight)
            player.invuln = 90
        } else {
            killPlayer()
        }
    }

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
        // Los ? bloques son sólidos (te paras encima y rebotas al golpearlos), aunque su
        // clase de colisión de terreno sea NONE (son block-code de página 0).
        if (behaviorAt(col, row) == SmwBlockBehavior.PRIZE) return SmwSolidity.SOLID
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

        if (p.invuln > 0) p.invuln--

        updateEnemies()
        handlePlayerEnemyContact()
        updatePowerups()

        checkDeadly()
        checkPickupsAndGoal()
        if (p.y > (rows + 2) * tileSize) killPlayer() // caído al vacío
    }

    /** Setas sueltas: gravedad, patrulla (rebote en paredes) y recogida por el jugador. */
    private fun updatePowerups() {
        val p = player
        val pw = tuning.playerWidth
        val ph = playerH()
        for (pu in powerups) {
            if (!pu.alive) continue
            pu.vy = min(pu.vy + tuning.gravityFall, tuning.maxFallSpeed)
            // vertical
            var ny = pu.y + pu.vy
            val minCol = (pu.x / tileSize).toInt()
            val maxCol = ((pu.x + pu.width - 0.01f) / tileSize).toInt()
            if (pu.vy > 0) {
                val row = ((ny + pu.height) / tileSize).toInt()
                if ((minCol..maxCol).any { blocksFloor(solidity(it, row)) }) {
                    ny = row * tileSize - pu.height - 0.01f; pu.vy = 0f
                }
            }
            pu.y = ny
            // horizontal (rebota en pared)
            val nx = pu.x + pu.vx
            val rowMin = (pu.y / tileSize).toInt()
            val rowMax = ((pu.y + pu.height - 0.01f) / tileSize).toInt()
            val frontCol = if (pu.vx > 0) ((nx + pu.width) / tileSize).toInt() else (nx / tileSize).toInt()
            if ((rowMin..rowMax).any { blocksSide(solidity(frontCol, it)) }) pu.vx = -pu.vx else pu.x = nx
            if (pu.y > (rows + 3) * tileSize) pu.alive = false
            // recogida
            val overlap = p.x < pu.x + pu.width && p.x + pw > pu.x && p.y < pu.y + pu.height && p.y + ph > pu.y
            if (overlap) { pu.alive = false; growPlayer() }
        }
    }

    /** Recoge monedas y detecta la META en las celdas que solapa la caja del jugador. */
    private fun checkPickupsAndGoal() {
        val p = player
        val w = tuning.playerWidth
        val h = playerH()
        val c0 = (p.x / tileSize).toInt()
        val c1 = ((p.x + w - 0.01f) / tileSize).toInt()
        val r0 = (p.y / tileSize).toInt()
        val r1 = ((p.y + h - 0.01f) / tileSize).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            if (c < 0 || c >= cols || r < 0 || r >= rows) continue
            when (behaviorAt(c, r)) {
                SmwBlockBehavior.COIN -> {
                    val key = c * rows + r
                    if (consumed.add(key)) { coins++; coinEvents++ }
                }
                SmwBlockBehavior.GOAL -> if (!goalReached) { goalReached = true; goalEvents++ }
                SmwBlockBehavior.PRIZE -> {} // se gestiona al golpearlo por abajo (bumpPrize)
                SmwBlockBehavior.NONE -> {}
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
        val ph = playerH()
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
                hurtPlayer() // grande → encoge; pequeño → muere
                if (p.dead) return
            }
        }
    }

    private fun moveHorizontal(dx: Float) {
        if (dx == 0f) return
        val p = player
        val w = tuning.playerWidth
        val h = playerH()
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
        val h = playerH()
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
            if ((minCol..maxCol).any { solidity(it, row) == SmwSolidity.SOLID }) {
                // ? bloque golpeado por abajo → suelta una seta (una vez por celda).
                for (c in minCol..maxCol) bumpPrize(c, row)
                ny = (row + 1) * tileSize + 0.01f
                p.vy = 0f
            }
        }
        p.y = ny
    }

    /** Si la celda (col,row) es un ? bloque sin usar, suelta una seta encima y lo marca usado. */
    private fun bumpPrize(col: Int, row: Int) {
        if (behaviorAt(col, row) != SmwBlockBehavior.PRIZE) return
        val key = -(col * rows + row) - 1 // clave distinta del espacio de monedas
        if (!consumed.add(key)) return
        powerups.add(Powerup(col * tileSize.toFloat(), (row - 1) * tileSize.toFloat()))
        prizeEvents++
    }

    private fun checkDeadly() {
        val p = player
        val w = tuning.playerWidth
        val h = playerH()
        val c0 = (p.x / tileSize).toInt()
        val c1 = ((p.x + w - 0.01f) / tileSize).toInt()
        val r0 = (p.y / tileSize).toInt()
        val r1 = ((p.y + h - 0.01f) / tileSize).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            if (solidity(c, r) == SmwSolidity.SPIKE) { killPlayer(); return }
        }
    }
}
