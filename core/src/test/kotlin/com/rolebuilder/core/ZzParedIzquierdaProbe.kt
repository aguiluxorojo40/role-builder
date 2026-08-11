package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.Test

/** SONDA DESECHABLE: ¿frena de verdad la pared IZQUIERDA del nivel, o se cuela? */
class ZzParedIzquierdaProbe {

    @Test
    fun `hasta donde llega el jugador andando a la izquierda`() {
        val cols = 12
        val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[7][c] = SmwSolidity.SOLID
        val e = PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> if (r in 0 until rows && c in 0 until cols) grid[r][c] else SmwSolidity.NONE },
            startPixelX = 16, startPixelY = 6 * 16,
            tuning = PlatformerTuning(
                jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
                maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
            ),
        )
        e.moveX = -1f
        repeat(240) { e.tick() }
        println("SONDA pared izquierda: x final = ${e.player.x}")
    }
}
