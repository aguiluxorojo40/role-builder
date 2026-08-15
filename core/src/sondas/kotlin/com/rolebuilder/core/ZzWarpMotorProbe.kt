package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.EngineWarp
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.engine.platformer.WarpInput
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.Test

/**
 * SONDA DESECHABLE del MOTOR: ¿en qué CELDA hay que poner un warp de "pulsar ABAJO"
 * para que el jugador que está DE PIE sobre la boca de la tubería lo dispare?
 *
 * El motor ([PlatformerEngine.checkWarps]) activa un warp si la CAJA del jugador solapa
 * su celda. De pie sobre una tesela, la caja NO solapa esa tesela (los pies quedan justo
 * en su borde superior), así que un warp puesto en la boca no se dispararía nunca. Esto
 * lo mide, en vez de suponerlo. No necesita ROM.
 */
class ZzWarpMotorProbe {

    private fun motor(warpRow: Int): PlatformerEngine {
        val cols = 12
        val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[8][c] = SmwSolidity.SOLID // suelo (fila 8) = la "boca"
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = 5 * 16, startPixelY = 5 * 16,
            tuning = PlatformerTuning(
                jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
                maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
            ),
            warps = listOf(EngineWarp(5, warpRow, WarpInput.DOWN, destMapId = 42, destX = 3, destY = 4)),
        )
    }

    @Test
    fun `donde hay que poner el warp de bajar por la tuberia`() {
        for (fila in listOf(8, 7)) {
            val e = motor(fila)
            repeat(40) { e.tick() }          // que caiga y se pose en la fila 8
            e.inputDown = true
            repeat(5) { e.tick() }
            println(
                "warp en fila %d -> disparado=%s (jugador y=%.1f alto=%.1f)".format(
                    fila, e.pendingWarp != null, e.player.y, e.playerHeight
                )
            )
        }
    }

    /**
     * Escenario de TUBERÍA HORIZONTAL: suelo en la fila 8, y una BOCA sólida en la columna
     * [bocaCol] de la fila 7 (la fila que pisa el jugador). Se mide en qué celda hay que
     * poner el warp de lado y con qué empuje se dispara.
     */
    private fun motorHorizontal(
        warpCol: Int,
        input: WarpInput,
        bocaCol: Int = 10,
        salidaCol: Int = 4,
    ): PlatformerEngine {
        val cols = 14
        val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[8][c] = SmwSolidity.SOLID // suelo
        grid[7][bocaCol] = SmwSolidity.SOLID                   // la boca: sólida, hace de pared
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = salidaCol * 16, startPixelY = 6 * 16,
            tuning = PlatformerTuning(
                jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
                maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
            ),
            warps = listOf(EngineWarp(warpCol, 7, input, destMapId = 42, destX = 3, destY = 4)),
        )
    }

    @Test
    fun `donde hay que poner el warp de entrar de lado, y hacia donde`() {
        // La boca está en la columna 10. Andando a la DERECHA hacia ella, ¿qué celda pisa
        // el jugador cuando la toca: la 9 (el lado abierto) o la 10 (la boca misma)?
        for (col in listOf(10, 9)) {
            val e = motorHorizontal(col, WarpInput.SIDE_RIGHT)
            e.moveX = 1f
            repeat(120) { e.tick() }
            println(
                "warp SIDE_RIGHT en columna %d -> disparado=%s (jugador x=%.1f ancho=%.1f)".format(
                    col, e.pendingWarp != null, e.player.x, e.tuning.playerWidth
                )
            )
        }
        // Andando en SENTIDO CONTRARIO por encima de la misma celda NO se debe disparar: en
        // el juego hay que empujar contra la boca ($00:F40A pide la dirección pulsada).
        val alReves = motorHorizontal(9, WarpInput.SIDE_RIGHT, salidaCol = 9)
        alReves.moveX = -1f
        repeat(30) { alReves.tick() }
        println("warp SIDE_RIGHT pisandolo pero andando a la IZQUIERDA -> disparado=${alReves.pendingWarp != null}")
        // Espejo: boca a la IZQUIERDA (columna 3), casilla disparadora la 4, y el jugador
        // llega andando hacia la izquierda desde la 10.
        val haciaIzq = motorHorizontal(4, WarpInput.SIDE_LEFT, bocaCol = 3, salidaCol = 10)
        haciaIzq.moveX = -1f
        repeat(120) { haciaIzq.tick() }
        println("warp SIDE_LEFT andando a la IZQUIERDA -> disparado=${haciaIzq.pendingWarp != null}")
    }
}
