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
}
