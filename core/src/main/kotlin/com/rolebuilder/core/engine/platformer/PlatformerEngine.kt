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
) {
    val player = PlatformerBody(startPixelX.toFloat(), startPixelY.toFloat())

    /** Input horizontal (-1 izquierda, +1 derecha) y botón de correr. */
    var moveX = 0f
    var running = false

    private var jumpHeld = false
    private var jumpPressedEdge = false

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
        }
        jumpPressedEdge = false
        if (p.vy >= 0f) p.jumping = false

        // --- gravedad (salto variable: menor manteniendo el botón mientras sube) ---
        val gravity = if (jumpHeld && p.vy < 0f) t.gravityHold else t.gravityFall
        p.vy = min(p.vy + gravity, t.maxFallSpeed)

        // --- integración con colisión, eje a eje ---
        moveHorizontal(p.vx)
        moveVertical(p.vy)

        checkDeadly()
        if (p.y > (rows + 2) * tileSize) p.dead = true // caído al vacío
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
            if ((minCol..maxCol).any { solidity(it, row) == SmwSolidity.SOLID }) {
                ny = (row + 1) * tileSize + 0.01f
                p.vy = 0f
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
            if (solidity(c, r) == SmwSolidity.SPIKE) { p.dead = true; return }
        }
    }
}
