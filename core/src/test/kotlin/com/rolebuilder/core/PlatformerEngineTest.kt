package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.BlockAction
import com.rolebuilder.core.engine.platformer.EnemySeed
import com.rolebuilder.core.engine.platformer.EngineWarp
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PowerupKind
import com.rolebuilder.core.engine.platformer.WarpInput
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
    fun `al morir hay animacion de pop hacia arriba y caida fuera del nivel`() {
        // Muere por pinchos: el mundo no debe congelarse; Mario da el saltito de SMW
        // y cae sin colisión hasta salir del nivel.
        val e = engine(10, 10, startCol = 2, startRow = 6) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.SOLID
            g[7][2] = SmwSolidity.SPIKE
        }
        var guard = 0
        while (!e.player.dead && guard < 120) { e.tick(); guard++ }
        assertTrue(e.player.dead)
        assertTrue(e.player.vy < 0f, "arranca el pop de muerte hacia arriba (vy=${e.player.vy})")
        val y0 = e.player.y
        e.tick()
        assertTrue(e.player.y < y0, "sube en el primer fotograma del pop")
        e.run(300)
        assertTrue(e.player.y > 10 * 16f, "cae atravesando el suelo y sale del nivel (y=${e.player.y})")
        assertEquals(1, e.deathEvents, "la muerte sigue contándose una sola vez")
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

    /** Motor con suelo, un bloque `?` sobre la cabeza y el jugador listo para cabecearlo. */
    private fun prizeEngine(): PlatformerEngine {
        val cols = 6; val rows = 8
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[6][c] = SmwSolidity.SOLID // suelo
        grid[3][2] = SmwSolidity.SOLID                          // bloque ? sólido
        val actions = IntArray(cols * rows) { BlockAction.NONE.ordinal }
        actions[3 * cols + 2] = BlockAction.PRIZE.ordinal
        return PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 2 * 16, startPixelY = 5 * 16, tuning = tuning, blockActions = actions,
        )
    }

    @Test
    fun `golpear un bloque interrogante siendo pequeno suelta una SETA y lo deja usado`() {
        val e = prizeEngine()
        e.run(20) // se posa en el suelo
        e.setJumpHeld(true); e.pressJump()
        e.run(60)  // salta y cabecea el bloque
        assertEquals(0, e.coins, "el premio de pequeño no es moneda")
        assertEquals(1, e.items.size, "el bloque ? soltó una seta")
        assertEquals(BlockAction.NONE, e.blockActionAt(2, 3), "el bloque queda usado")
    }

    // ------------------------------------------------------------------ powerups

    @Test
    fun `recoger la seta hace crecer a Mario y suena el powerup`() {
        val e = prizeEngine()
        e.run(20)
        e.setJumpHeld(true); e.pressJump()
        e.run(60) // cabecea: sale la seta y cae al suelo
        val smallH = e.playerHeight
        assertFalse(e.player.big)
        // La seta patrulla el mismo suelo que el jugador: acaba tocándolo.
        e.run(600)
        assertTrue(e.player.big, "creció al tocar la seta")
        assertEquals(1, e.powerupEvents, "el powerup suena una vez")
        assertEquals(PlatformerEngine.BIG_HEIGHT, e.playerHeight, "la caja crece")
        assertTrue(e.playerHeight > smallH)
        assertTrue(e.items.none { it.alive }, "la seta se consumió")
    }

    @Test
    fun `un golpe siendo grande encoge con invulnerabilidad en vez de matar`() {
        val cols = 12; val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[8][c] = SmwSolidity.SOLID
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 5 * 16, startPixelY = 8 * 16 - 26, // de pie sobre el suelo
            tuning = tuning.copy(playerHeight = PlatformerEngine.BIG_HEIGHT), // arranca grande
            enemySeeds = listOf(EnemySeed(6 * 16, 8 * 16 - 14, 0)),
        )
        assertTrue(e.player.big, "el tuning de 26 px arranca grande")
        e.run(30) // el enemigo patrulla hacia el jugador y le golpea de lado
        assertFalse(e.player.dead, "no muere: encoge")
        assertFalse(e.player.big, "queda pequeño")
        assertEquals(1, e.damageEvents)
        assertTrue(e.player.invulnFrames > 0, "queda invulnerable un rato")
        // Durante la invulnerabilidad, más contacto no mata ni re-encoge.
        e.run(10)
        assertFalse(e.player.dead, "el contacto durante la invulnerabilidad no mata")
    }

    @Test
    fun `un golpe siendo pequeno sigue matando`() {
        val cols = 12; val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[8][c] = SmwSolidity.SOLID
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 5 * 16, startPixelY = 8 * 16 - 14,
            tuning = tuning,
            enemySeeds = listOf(EnemySeed(6 * 16, 8 * 16 - 14, 0)),
        )
        e.run(60)
        assertTrue(e.player.dead, "pequeño muere al contacto")
        assertEquals(1, e.deathEvents)
    }

    // --------------------------------------------------------------- flor de fuego

    /** Motor que ARRANCA grande, con suelo lleno, un `?` sobre la cabeza y enemigos. */
    private fun bigPrizeEngine(seeds: List<EnemySeed> = emptyList()): PlatformerEngine {
        val cols = 10; val rows = 8
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[6][c] = SmwSolidity.SOLID // suelo
        grid[3][2] = SmwSolidity.SOLID                          // bloque ? sólido
        val actions = IntArray(cols * rows) { BlockAction.NONE.ordinal }
        actions[3 * cols + 2] = BlockAction.PRIZE.ordinal
        return PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 2 * 16, startPixelY = 6 * 16 - PlatformerEngine.BIG_HEIGHT.toInt(),
            tuning = tuning.copy(playerHeight = PlatformerEngine.BIG_HEIGHT),
            blockActions = actions, enemySeeds = seeds,
        )
    }

    @Test
    fun `Mario grande cabecea un ? y suelta una FLOR DE FUEGO`() {
        val e = bigPrizeEngine()
        assertTrue(e.player.big, "arranca grande")
        e.run(20)
        e.setJumpHeld(true); e.pressJump()
        e.run(60) // cabecea el bloque
        assertEquals(0, e.coins, "grande sin fuego no da moneda")
        assertEquals(1, e.items.size, "el bloque ? soltó un premio")
        assertEquals(PowerupKind.FIRE_FLOWER, e.items.first().kind, "y es una flor de fuego")
    }

    @Test
    fun `recoger la flor de fuego da el poder de fuego`() {
        val e = bigPrizeEngine()
        e.run(20)
        e.setJumpHeld(true); e.pressJump()
        e.run(60) // cabecea: sale la flor
        e.setJumpHeld(false)
        assertFalse(e.player.fire)
        // La flor cae sobre el jugador (misma columna) y se recoge.
        var guard = 0
        while (!e.player.fire && guard < 400) { e.tick(); guard++ }
        assertTrue(e.player.fire, "recogió la flor y tiene fuego")
        assertTrue(e.player.big, "el fuego implica seguir grande")
        assertTrue(e.items.none { it.alive }, "la flor se consumió")
    }

    @Test
    fun `Mario de fuego lanza una bola que mata a un enemigo`() {
        // Suelo lleno, jugador de fuego a la izquierda, enemigo unas casillas a la derecha.
        val cols = 12; val rows = 8
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[6][c] = SmwSolidity.SOLID
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 2 * 16, startPixelY = 6 * 16 - PlatformerEngine.BIG_HEIGHT.toInt(),
            tuning = tuning.copy(playerHeight = PlatformerEngine.BIG_HEIGHT),
            enemySeeds = listOf(EnemySeed(6 * 16, 6 * 16 - 14, 0)),
        )
        e.player.fire = true
        e.run(10) // se posa
        val enemy = e.enemies.single()
        assertTrue(enemy.alive)
        e.moveX = 1f; e.tick()          // mira a la derecha (hacia el enemigo)
        e.running = true; e.tick()      // flanco de correr → lanza una bola
        assertEquals(1, e.fireballEvents, "un flanco de correr = una bola")
        assertTrue(e.fireballs.any { it.alive }, "hay una bola en vuelo")
        e.moveX = 0f                     // no avanzar tras el enemigo; que lo mate la bola
        e.run(40)
        assertFalse(enemy.alive, "la bola de fuego mata al enemigo")
    }

    @Test
    fun `dos bolas de fuego son el maximo en pantalla`() {
        val cols = 20; val rows = 8
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[6][c] = SmwSolidity.SOLID
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 2 * 16, startPixelY = 6 * 16 - PlatformerEngine.BIG_HEIGHT.toInt(),
            tuning = tuning.copy(playerHeight = PlatformerEngine.BIG_HEIGHT),
        )
        e.player.fire = true
        e.run(10)
        // Tres pulsaciones de correr seguidas: solo dos bolas caben en pantalla.
        repeat(3) { e.running = false; e.tick(); e.running = true; e.tick() }
        assertTrue(e.fireballs.count { it.alive } <= 2, "nunca más de dos bolas vivas")
    }

    @Test
    fun `Mario de fuego cabecea un ? y suelta una PLUMA de capa`() {
        val e = bigPrizeEngine()
        e.player.fire = true
        e.run(20)
        e.setJumpHeld(true); e.pressJump()
        e.run(60) // cabecea
        assertEquals(1, e.items.size, "el bloque ? soltó un premio")
        assertEquals(PowerupKind.CAPE_FEATHER, e.items.first().kind, "y es una pluma de capa")
    }

    @Test
    fun `recoger la pluma da capa y quita el fuego`() {
        val e = bigPrizeEngine()
        e.player.fire = true
        e.run(20)
        e.setJumpHeld(true); e.pressJump()
        e.run(60); e.setJumpHeld(false)
        var guard = 0
        while (!e.player.cape && guard < 400) { e.tick(); guard++ }
        assertTrue(e.player.cape, "recogió la pluma y tiene capa")
        assertFalse(e.player.fire, "la capa reemplaza al fuego")
        assertTrue(e.player.big, "sigue grande")
    }

    @Test
    fun `la capa hace planear al caer con el salto mantenido`() {
        // Nivel alto sin suelo cerca: Mario cae en caída libre.
        val e = PlatformerEngine(
            6, 20, solidityAt = { _, _ -> SmwSolidity.NONE },
            startPixelX = 2 * 16, startPixelY = 1 * 16,
            tuning = tuning.copy(playerHeight = PlatformerEngine.BIG_HEIGHT),
        )
        e.player.cape = true
        e.setJumpHeld(true) // mantiene el salto → planea al caer
        e.run(60)
        assertTrue(e.player.vy <= PlatformerEngine.CAPE_GLIDE_SPEED + 0.01f, "planea: cae despacio (vy=${e.player.vy})")
        assertTrue(e.player.vy < tuning.maxFallSpeed, "más lento que la caída normal")
        assertFalse(e.player.dead, "planeando no ha caído fuera aún")
    }

    @Test
    fun `sin capa la caida no planea`() {
        val e = PlatformerEngine(
            6, 20, solidityAt = { _, _ -> SmwSolidity.NONE },
            startPixelX = 2 * 16, startPixelY = 1 * 16,
            tuning = tuning.copy(playerHeight = PlatformerEngine.BIG_HEIGHT),
        )
        e.setJumpHeld(true)
        e.run(60)
        assertEquals(tuning.maxFallSpeed, e.player.vy, 0.001f, "sin capa cae a velocidad terminal")
    }

    @Test
    fun `un golpe siendo de fuego lo deja pequeno`() {
        val cols = 12; val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[8][c] = SmwSolidity.SOLID
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 5 * 16, startPixelY = 8 * 16 - PlatformerEngine.BIG_HEIGHT.toInt(),
            tuning = tuning.copy(playerHeight = PlatformerEngine.BIG_HEIGHT),
            enemySeeds = listOf(EnemySeed(6 * 16, 8 * 16 - 14, 0)),
        )
        e.player.fire = true // ya tenía fuego
        e.run(30) // el enemigo le golpea de lado
        assertFalse(e.player.dead, "no muere: pierde el poder")
        assertFalse(e.player.fire, "deja de tener fuego")
        assertFalse(e.player.big, "y queda pequeño")
    }

    // ---------------------------------------------------------- físicas en caliente

    @Test
    fun `las fisicas se pueden cambiar en caliente y el motor las usa al instante`() {
        val e = engine(10, 12, startCol = 2, startRow = 9) { g ->
            for (c in 0 until 10) g[10][c] = SmwSolidity.SOLID
        }
        e.run(30) // se posa
        // Salto con el tuning original: registra la altura máxima.
        e.setJumpHeld(true); e.pressJump()
        var min1 = e.player.y
        repeat(90) { e.tick(); if (e.player.y < min1) min1 = e.player.y }
        e.setJumpHeld(false); e.run(120) // vuelve al suelo
        // Cambia el IMPULSO en caliente (lo que hace el panel de físicas) y re-salta.
        e.tuning = e.tuning.copy(jumpSpeed = e.tuning.jumpSpeed * 1.5f)
        e.setJumpHeld(true); e.pressJump()
        var min2 = e.player.y
        repeat(120) { e.tick(); if (e.player.y < min2) min2 = e.player.y }
        assertTrue(min2 < min1 - 8f, "con más impulso sube claramente más alto ($min2 vs $min1)")
    }

    @Test
    fun `cambiar la altura de la caja en caliente conserva los pies`() {
        val e = engine(10, 12, startCol = 2, startRow = 9) { g ->
            for (c in 0 until 10) g[10][c] = SmwSolidity.SOLID
        }
        e.run(60) // se posa: los pies en y + alto = 10*16
        val feet = e.player.y + e.playerHeight
        e.setSmallHeight(8f)
        assertEquals(8f, e.playerHeight, 0.001f, "la caja usa la altura nueva")
        assertEquals(feet, e.player.y + e.playerHeight, 0.001f, "los pies no se mueven al encoger")
        e.setSmallHeight(20f)
        assertEquals(feet, e.player.y + e.playerHeight, 0.001f, "ni al agrandar")
        e.run(30)
        assertTrue(e.player.onGround, "sigue de pie sobre el suelo")
    }

    // -------------------------------------------------------------------- warps

    @Test
    fun `bajar por una tuberia activa el warp al mapa destino`() {
        val cols = 6; val rows = 8
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[6][c] = SmwSolidity.SOLID // suelo
        val warps = listOf(EngineWarp(3, 5, WarpInput.DOWN, destMapId = 7, destX = 2, destY = 3))
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 3 * 16, startPixelY = 4 * 16, tuning = tuning, warps = warps,
        )
        e.run(20) // se posa sobre la celda (3,5)
        assertTrue(e.pendingWarp == null, "sin pulsar abajo no hay warp")
        e.inputDown = true
        e.run(2)
        val warp = e.pendingWarp
        assertTrue(warp != null, "bajar sobre la tubería activa el warp")
        assertEquals(7, warp!!.destMapId)
        assertEquals(2, warp.destX)
        assertEquals(3, warp.destY)
    }
}
