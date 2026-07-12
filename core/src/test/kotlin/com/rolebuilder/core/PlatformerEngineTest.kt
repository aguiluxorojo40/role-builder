package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.BlockAction
import com.rolebuilder.core.engine.platformer.EnemySeed
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.snes.SmwPhysics
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests del motor de plataformas: gravedad, salto, colisión por caja, plataformas
 * de un sentido, pinchos y caída al vacío. Usa físicas fijas y deterministas y
 * niveles sintéticos (rejilla de [SmwSolidity]).
 */
class PlatformerEngineTest {

    private val tuning = PlatformerTuning(
        jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
        maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
    )

    /** Construye un motor sobre una rejilla; [fill] pinta celdas sólidas. */
    private fun engine(
        cols: Int, rows: Int, startCol: Int, startRow: Int,
        fill: (grid: Array<Array<SmwSolidity>>) -> Unit,
    ): PlatformerEngine {
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        fill(grid)
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = startCol * 16, startPixelY = startRow * 16,
            tuning = tuning,
        )
    }

    private fun PlatformerEngine.run(frames: Int) { repeat(frames) { tick() } }

    @Test
    fun `cae por gravedad y se posa sobre el suelo`() {
        val e = engine(10, 10, startCol = 2, startRow = 1) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.SOLID
        }
        e.run(120)
        assertTrue(e.player.onGround, "acaba en el suelo")
        // Reposa justo sobre la fila 8: y = 8*16 - alto.
        assertEquals(128f - tuning.playerHeight, e.player.y, 1f)
        assertEquals(0f, e.player.vy, 0.001f)
    }

    @Test
    fun `salta desde el suelo, sube y vuelve a caer`() {
        val e = engine(10, 10, startCol = 2, startRow = 7) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.SOLID
        }
        e.run(30) // que se pose
        val restY = e.player.y
        assertTrue(e.player.onGround)
        e.setJumpHeld(true)
        e.pressJump()
        e.tick()
        assertTrue(e.player.vy < 0f, "arranca subiendo")
        var minY = e.player.y
        repeat(60) { e.tick(); if (e.player.y < minY) minY = e.player.y }
        assertTrue(minY < restY - 16f, "llegó a subir al menos una casilla")
        e.setJumpHeld(false)
        e.run(120)
        assertTrue(e.player.onGround, "vuelve al suelo")
        assertEquals(restY, e.player.y, 1f)
    }

    @Test
    fun `un muro solido frena el avance horizontal`() {
        val e = engine(12, 10, startCol = 2, startRow = 7) { g ->
            for (c in 0 until 12) g[8][c] = SmwSolidity.SOLID
            for (r in 0 until 8) g[r][6] = SmwSolidity.SOLID // muro en la col 6
        }
        e.run(20)
        e.moveX = 1f; e.running = true
        e.run(120)
        // El muro empieza en x=96; con ancho 12 el jugador se para antes de 96-12.
        assertTrue(e.player.x <= 96f - tuning.playerWidth + 0.5f, "no atraviesa el muro (x=${e.player.x})")
    }

    @Test
    fun `una plataforma de un sentido frena la caida pero se cruza de lado`() {
        val e = engine(10, 12, startCol = 2, startRow = 1) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.LEDGE_TOP
        }
        e.run(120)
        assertTrue(e.player.onGround, "se posa sobre el borde de un sentido")
        assertEquals(128f - tuning.playerHeight, e.player.y, 1f)
    }

    @Test
    fun `los pinchos matan al jugador`() {
        val e = engine(10, 10, startCol = 2, startRow = 6) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.SOLID
            g[7][2] = SmwSolidity.SPIKE
        }
        e.run(60)
        assertTrue(e.player.dead, "muere al tocar los pinchos")
    }

    @Test
    fun `caer al vacio mata al jugador`() {
        val e = engine(10, 8, startCol = 2, startRow = 1) { /* sin suelo */ }
        e.run(200)
        assertTrue(e.player.dead, "cae fuera del nivel y muere")
    }

    @Test
    fun `las fisicas reales de SMW dan un salto de -5 px por fotograma`() {
        val phys = SmwPhysics(
            jumpYSpeed = intArrayOf(-80), marioXAccel = IntArray(0), iceXAccel = IntArray(0),
            maxXSpeed = IntArray(0), maxXSpeedExtra = IntArray(0),
            friction1 = IntArray(0), friction2 = IntArray(0),
            gravity = intArrayOf(6, 3), maxFallSpeed = intArrayOf(0x40),
        )
        val t = PlatformerTuning.fromSmw(phys)
        assertEquals(-5f, t.jumpSpeed, 0.001f)
        assertEquals(0.375f, t.gravityFall, 0.001f)
        assertEquals(0.1875f, t.gravityHold, 0.001f)
        assertEquals(4f, t.maxFallSpeed, 0.001f)
        assertFalse(t.maxRunSpeed <= t.maxWalkSpeed)
    }

    /** Motor con enemigos: rejilla + suelo + semillas de enemigo. */
    private fun engineEnemies(
        cols: Int, rows: Int, startCol: Int, startRow: Int,
        seeds: List<EnemySeed>,
        fill: (grid: Array<Array<SmwSolidity>>) -> Unit,
    ): PlatformerEngine {
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        fill(grid)
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = startCol * 16, startPixelY = startRow * 16,
            tuning = tuning, enemySeeds = seeds,
        )
    }

    @Test
    fun `el enemigo cae al suelo y patrulla dandose la vuelta en el borde`() {
        // Suelo corto (columnas 3..6) en la fila 8; el enemigo empieza sobre él.
        val e = engineEnemies(12, 10, startCol = 0, startRow = 0, seeds = listOf(EnemySeed(5 * 16, 6 * 16, 0))) { g ->
            for (c in 3..6) g[8][c] = SmwSolidity.SOLID
        }
        val enemy = e.enemies.single()
        e.run(30)
        assertTrue(enemy.onGround, "el enemigo se posa en el suelo")
        // Patrulla dentro de la plataforma: no se sale por el borde al vacío.
        e.run(300)
        assertTrue(enemy.alive, "no se cae de la plataforma")
        assertTrue(enemy.x >= 3 * 16f - 1f && enemy.x <= 6 * 16f + 1f, "sigue sobre la plataforma (x=${enemy.x})")
    }

    @Test
    fun `pisar al enemigo lo mata y el jugador rebota`() {
        // Jugador justo encima del enemigo, ambos sobre el suelo.
        val e = engineEnemies(12, 10, startCol = 5, startRow = 5, seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        val enemy = e.enemies.single()
        e.run(60)
        assertFalse(enemy.alive, "el jugador cae encima y lo pisa")
        assertFalse(e.player.dead, "pisar no mata al jugador")
    }

    @Test
    fun `chocar de lado con el enemigo mata al jugador`() {
        // Jugador y enemigo a la misma altura sobre el suelo, pegados: contacto lateral.
        val e = engineEnemies(12, 10, startCol = 5, startRow = 8, seeds = listOf(EnemySeed(6 * 16, 8 * 16, 0))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        e.moveX = 1f // el jugador avanza hacia el enemigo
        e.run(30)
        assertTrue(e.player.dead, "el contacto lateral mata al jugador")
    }

    // ----------------------------------------------- contadores de eventos (audio)

    @Test
    fun `saltar incrementa jumpEvents una vez por salto`() {
        val e = engine(10, 10, startCol = 2, startRow = 7) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.SOLID
        }
        e.run(30)
        assertEquals(0, e.jumpEvents)
        e.setJumpHeld(true)
        e.pressJump()
        e.tick()
        assertEquals(1, e.jumpEvents, "un flanco de salto = un evento")
        e.run(10) // mantener pulsado en el aire no re-dispara
        assertEquals(1, e.jumpEvents)
    }

    @Test
    fun `pisar a un enemigo incrementa stompEvents`() {
        val e = engineEnemies(12, 10, startCol = 5, startRow = 5, seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        e.run(60)
        assertEquals(1, e.stompEvents, "un pisotón = un evento")
    }

    @Test
    fun `morir incrementa deathEvents una sola vez`() {
        val e = engine(10, 8, startCol = 2, startRow = 1) { /* sin suelo: cae al vacío */ }
        e.run(200)
        assertTrue(e.player.dead)
        assertEquals(1, e.deathEvents, "la muerte se cuenta una vez, no en cada tick posterior")
    }

    // -------------------------------------------------------- bloques interactivos

    @Test
    fun `recoge una moneda al tocarla`() {
        val cols = 6; val rows = 6
        val actions = IntArray(cols * rows) { BlockAction.NONE.ordinal }
        actions[2 * cols + 2] = BlockAction.COIN.ordinal // moneda en (2,2)
        val e = PlatformerEngine(
            cols, rows, solidityAt = { _, _ -> SmwSolidity.NONE },
            startPixelX = 2 * 16, startPixelY = 2 * 16, tuning = tuning, blockActions = actions,
        )
        e.tick() // el jugador arranca solapando la celda de la moneda
        assertEquals(1, e.coins, "recoge la moneda")
        assertEquals(1, e.coinEvents, "suena una vez")
        assertEquals(BlockAction.NONE, e.blockActionAt(2, 2), "la moneda desaparece")
    }

    @Test
    fun `golpear un bloque interrogante desde abajo suelta moneda y lo deja usado`() {
        val cols = 6; val rows = 8
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[6][c] = SmwSolidity.SOLID // suelo
        grid[3][2] = SmwSolidity.SOLID                          // bloque ? sólido
        val actions = IntArray(cols * rows) { BlockAction.NONE.ordinal }
        actions[3 * cols + 2] = BlockAction.PRIZE.ordinal
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 2 * 16, startPixelY = 5 * 16, tuning = tuning, blockActions = actions,
        )
        e.run(20) // se posa en el suelo
        e.setJumpHeld(true); e.pressJump()
        e.run(60)  // salta y cabecea el bloque
        assertEquals(1, e.coins, "el bloque ? soltó una moneda")
        assertEquals(BlockAction.NONE, e.blockActionAt(2, 3), "el bloque queda usado")
    }
}
