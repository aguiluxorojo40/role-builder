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

    /** Físicas US mínimas (andar/correr a la derecha) para el modo 1:1 en el motor. */
    private fun physUS() = SmwPhysics(
        jumpYSpeed = intArrayOf(-80),
        marioXAccel = shortsAsInts(0xFE80, 0xFE80, 0x0180, 0x0180, 0xFE80, 0xFE80, 0x0180, 0x0180),
        iceXAccel = shortsAsInts(0xFF80, 0xFE80, 0x0080, 0x0180),
        maxXSpeed = bytesAsInts(0xEC, 0x14, 0xDC, 0x24, 0xDC, 0x24, 0xD0, 0x30),
        maxXSpeedExtra = IntArray(0),
        friction1 = shortsAsInts(0xFF00, 0x0100, 0xFF00, 0x0100),
        friction2 = shortsAsInts(0xFFE0, 0x0020, 0xFFE0, 0x0020),
        gravity = intArrayOf(6, 3), maxFallSpeed = intArrayOf(0x40),
    )
    private fun shortsAsInts(vararg v: Int) = IntArray(v.size) { v[it].toShort().toInt() }
    private fun bytesAsInts(vararg v: Int) = IntArray(v.size) { v[it].toByte().toInt() }

    /** Motor en modo ROM (con [SmwPhysics]) sobre una rejilla; [fill] pinta celdas sólidas. */
    private fun engineRom(
        cols: Int, rows: Int, startCol: Int, startRow: Int,
        fill: (grid: Array<Array<SmwSolidity>>) -> Unit,
    ): PlatformerEngine {
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        fill(grid)
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = startCol * 16, startPixelY = startRow * 16,
            tuning = PlatformerTuning.fromSmw(physUS()),
            smwPhysics = physUS(),
        )
    }

    @Test
    fun `modo 1_1 - Mario avanza a la derecha y mira a la derecha`() {
        val e = engineRom(30, 10, startCol = 2, startRow = 7) { g ->
            for (c in 0 until 30) g[8][c] = SmwSolidity.SOLID
        }
        e.run(20) // que se pose
        val x0 = e.player.x
        e.moveX = 1f
        e.run(60)
        assertTrue(e.player.x > x0 + 16f, "avanza claramente a la derecha")
        assertTrue(e.player.facingRight, "mira a la derecha")
    }

    @Test
    fun `modo 1_1 - un muro frena a Mario sin atravesarlo`() {
        val wallCol = 8
        val e = engineRom(20, 10, startCol = 2, startRow = 7) { g ->
            for (c in 0 until 20) g[8][c] = SmwSolidity.SOLID
            for (r in 0..7) g[r][wallCol] = SmwSolidity.SOLID // muro vertical
        }
        e.run(20)
        e.moveX = 1f
        e.running = true
        e.run(200)
        // No atraviesa el muro: el borde derecho queda a la izquierda de la columna del muro.
        assertTrue(e.player.x + tuning.playerWidth <= wallCol * 16f + 0.5f,
            "se queda pegado al muro (x=${e.player.x})")
        // Colisión por sondas: empujando contra el muro marca el lado derecho (player_blocked_flags)
        // en algún fotograma (al re-acelerar tras pararse contra la pared).
        var seen = 0
        repeat(30) { e.tick(); seen = seen or e.blockedFlags }
        assertTrue(seen and PlatformerEngine.BLOCKED_RIGHT != 0, "marca pared derecha al empujar")
    }

    @Test
    fun `modo 1_1 - posado en el suelo marca BLOCKED_DOWN`() {
        val e = engineRom(10, 10, startCol = 2, startRow = 3) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.SOLID
        }
        e.run(60)
        assertTrue(e.player.onGround, "acaba en el suelo")
        assertTrue(e.blockedFlags and PlatformerEngine.BLOCKED_DOWN != 0, "suelo = BLOCKED_DOWN")
    }

    @Test
    fun `modo 1_1 - cabezazo a un bloque ? suelta premio y marca BLOCKED_UP`() {
        val cols = 10; val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[8][c] = SmwSolidity.SOLID   // suelo
        grid[5][2] = SmwSolidity.SOLID                            // bloque ? encima de Mario
        val actions = IntArray(cols * rows)
        actions[5 * cols + 2] = BlockAction.PRIZE.ordinal
        val e = PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = 2 * 16, startPixelY = 6 * 16,
            tuning = PlatformerTuning.fromSmw(physUS()),
            smwPhysics = physUS(),
            blockActions = actions,
        )
        e.run(20) // que se pose bajo el bloque
        val itemsBefore = e.items.size
        e.pressJump(); e.setJumpHeld(true)
        e.run(40)   // salta y golpea el bloque desde abajo
        assertTrue(e.items.size > itemsBefore, "el cabezazo soltó un premio")
    }

    /** Motor con un bloque de AGARRAR en (grabCol,grabRow) y suelo en la fila [floorRow]. */
    private fun engineGrab(
        cols: Int, rows: Int, startCol: Int, startRow: Int, grabCol: Int, grabRow: Int,
        floorRow: Int, seeds: List<EnemySeed> = emptyList(),
    ): PlatformerEngine {
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[floorRow][c] = SmwSolidity.SOLID
        val actions = IntArray(cols * rows)
        actions[grabRow * cols + grabCol] = BlockAction.GRAB.ordinal
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = startCol * 16, startPixelY = startRow * 16,
            tuning = tuning, enemySeeds = seeds, blockActions = actions,
        )
    }

    @Test
    fun `throw-block - Mario coge un bloque al lado pulsando correr`() {
        val e = engineGrab(10, 10, startCol = 2, startRow = 6, grabCol = 3, grabRow = 6, floorRow = 7)
        assertEquals(1, e.grabBlocks.size, "el nivel sembró un bloque de agarrar")
        e.run(20) // que se pose
        e.running = true
        e.tick()   // flanco de correr -> coge
        assertTrue(e.carriedBlock != null, "Mario coge el bloque")
        assertTrue(e.grabBlocks.first().carried, "el bloque queda cargado")
    }

    @Test
    fun `throw-block - lanzar el bloque arrolla a un enemigo`() {
        val e = engineGrab(16, 10, startCol = 2, startRow = 6, grabCol = 3, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(9 * 16, 6 * 16, 0x00)))
        e.run(20)
        e.running = true; e.tick()          // coge
        assertTrue(e.carriedBlock != null)
        e.running = false; e.tick()          // suelta el botón
        e.running = true; e.tick()           // vuelve a pulsar -> lanza
        assertTrue(e.grabBlocks.first().thrown, "el bloque sale lanzado")
        val enemy = e.enemies.first()
        e.run(60)                            // el bloque vuela hasta el enemigo
        assertFalse(enemy.alive, "el bloque lanzado arrolla al enemigo")
    }

    @Test
    fun `jefe - identidad, HP y caja grande`() {
        val e = engineGrab(16, 10, startCol = 2, startRow = 6, grabCol = 3, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(9 * 16, 6 * 16, 0xA0)))  // Bowser
        val boss = e.enemies.first()
        assertTrue(boss.isBoss, "0xA0 (Bowser) es un jefe")
        assertEquals(PlatformerEngine.BOSS_HP, boss.hp, "arranca con el HP de jefe")
        assertEquals(PlatformerEngine.BOSS_WIDTH, boss.width, "caja ancha de jefe")
        assertEquals(PlatformerEngine.BOSS_HEIGHT, boss.height, "caja alta de jefe")
    }

    @Test
    fun `jefe - un bloque lanzado NO lo mata (a diferencia del enemigo normal)`() {
        val e = engineGrab(16, 10, startCol = 2, startRow = 6, grabCol = 3, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(9 * 16, 6 * 16, 0xA0)))  // Bowser
        val boss = e.enemies.first()
        e.run(20)
        e.running = true; e.tick()          // coge
        e.running = false; e.tick()          // suelta
        e.running = true; e.tick()           // lanza
        e.run(60)                            // el bloque vuela hasta el jefe
        assertTrue(boss.alive, "el jefe aguanta un bloque (tiene varios HP)")
        assertEquals(PlatformerEngine.BOSS_HP - 1, boss.hp, "el bloque le quita un punto de vida")
    }

    @Test
    fun `jefe - Bowser lanza un proyectil cuando Mario está cerca`() {
        val e = engineGrab(18, 10, startCol = 2, startRow = 6, grabCol = 16, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(6 * 16, 6 * 16, 0xA0)))  // Bowser a 4 casillas de Mario
        assertTrue(e.enemyProjectiles.isEmpty(), "arranca sin proyectiles")
        e.run(PlatformerEngine.BOSS_ATTACK_INTERVAL + 5)
        assertTrue(e.enemyProjectiles.isNotEmpty(), "Bowser lanza su proyectil hacia Mario")
    }

    @Test
    fun `jefe - Reznor escupe fuego RECTO apuntando a Mario`() {
        // Reznor (0xA9) a la derecha de Mario -> la bola de fuego sale hacia la IZQUIERDA y recta.
        val e = engineGrab(18, 10, startCol = 2, startRow = 6, grabCol = 16, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(6 * 16, 6 * 16, 0xA9)))
        e.run(PlatformerEngine.BOSS_ATTACK_INTERVAL + 5)
        val fire = e.enemyProjectiles.firstOrNull { it.alive }
        assertTrue(fire != null, "Reznor escupe una bola de fuego")
        assertFalse(fire!!.arc, "vuela RECTA (sin gravedad), como el sprite extendido real")
        assertTrue(fire.vx < 0f, "apunta hacia Mario (a la izquierda)")
    }

    @Test
    fun `jefe - Big Boo flota hacia Mario`() {
        val e = engineGrab(20, 12, startCol = 2, startRow = 8, grabCol = 18, grabRow = 8, floorRow = 9,
            seeds = listOf(EnemySeed(14 * 16, 3 * 16, 0xC5)))  // Big Boo arriba a la derecha
        val boo = e.enemies.first()
        val x0 = boo.x; val y0 = boo.y
        e.run(30)
        assertTrue(boo.x < x0, "Big Boo se acerca a Mario en X")
        assertTrue(boo.y > y0, "Big Boo baja hacia Mario en Y")
    }

    @Test
    fun `throw-block - con abajo pulsado lo deja en vez de lanzarlo`() {
        val e = engineGrab(10, 10, startCol = 2, startRow = 6, grabCol = 3, grabRow = 6, floorRow = 7)
        e.run(20)
        e.running = true; e.tick()           // coge
        e.running = false; e.tick()
        e.inputDown = true
        e.running = true; e.tick()           // con abajo: deja, no lanza
        assertTrue(e.carriedBlock == null, "suelta el bloque")
        assertFalse(e.grabBlocks.first().thrown, "no lo lanza (lo deja)")
    }

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
        // Goomba (0x0F): se pisa y muere (los Koopas dejan caparazón, ver otros tests).
        val e = engineEnemies(12, 10, startCol = 5, startRow = 5, seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0x0F))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        val enemy = e.enemies.single()
        e.run(60)
        assertFalse(enemy.alive, "el jugador cae encima y lo pisa")
        assertFalse(e.player.dead, "pisar no mata al jugador")
    }

    @Test
    fun `pisar un Koopa lo mete en su caparazon sin matarlo`() {
        val e = engineEnemies(12, 10, startCol = 5, startRow = 5, seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0x00))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        e.run(60)
        assertTrue(k.alive, "el Koopa no muere al pisarlo")
        assertTrue(k.shell, "se mete en su caparazón")
        assertFalse(k.shellMoving, "el caparazón queda quieto")
        assertFalse(e.player.dead)
    }

    @Test
    fun `tocar de lado un caparazon quieto lo patea`() {
        val e = engineEnemies(20, 10, startCol = 2, startRow = 7, seeds = listOf(EnemySeed(6 * 16, 8 * 16 - 14, 0x00))) { g ->
            for (c in 0 until 20) g[9][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        e.run(15)
        k.shell = true; k.shellMoving = false // caparazón quieto en el suelo
        e.moveX = 1f // Mario camina hacia él
        e.run(120)
        assertTrue(k.shellMoving, "tocar el caparazón de lado lo lanza")
    }

    @Test
    fun `el caparazon deslizandose arrolla a otro enemigo`() {
        val e = engineEnemies(20, 10, startCol = 0, startRow = 0,
            seeds = listOf(EnemySeed(3 * 16, 8 * 16 - 14, 0x00), EnemySeed(9 * 16, 8 * 16 - 14, 0x0F))) { g ->
            for (c in 0 until 20) g[9][c] = SmwSolidity.SOLID
        }
        val shell = e.enemies[0]; val victim = e.enemies[1]
        e.run(15)
        shell.shell = true; shell.shellMoving = true; shell.vx = PlatformerEngine.SHELL_SPEED
        e.run(160)
        assertFalse(victim.alive, "el caparazón deslizándose arrolla al enemigo")
    }

    @Test
    fun `la Koopa alada vuela y no se cae al suelo`() {
        val e = engineEnemies(20, 14, startCol = 0, startRow = 12, seeds = listOf(EnemySeed(10 * 16, 5 * 16, 0x08))) { g ->
            for (c in 0 until 20) g[13][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        assertTrue(k.winged, "empieza con alas")
        val y0 = k.y
        e.run(60)
        assertTrue(k.alive)
        assertTrue(k.y in (y0 - 20f)..(y0 + 20f), "vuela a su altura, no cae al suelo (y=${k.y})")
        assertTrue(k.x < 10 * 16f, "la 0x08 vuela hacia la izquierda (x=${k.x})")
    }

    @Test
    fun `la Koopa alada roja vertical patrulla arriba y abajo`() {
        val e = engineEnemies(14, 20, startCol = 0, startRow = 18, seeds = listOf(EnemySeed(7 * 16, 10 * 16, 0x0A))) { g ->
            for (c in 0 until 14) g[19][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        var maxY = k.y; var minY = k.y
        for (i in 0 until 200) { e.tick(); maxY = maxOf(maxY, k.y); minY = minOf(minY, k.y) }
        assertTrue(maxY - minY > 40f, "patrulla un rango vertical (${maxY - minY})")
        assertTrue(k.alive)
    }

    @Test
    fun `pisar una Koopa alada le quita las alas`() {
        val e = engineEnemies(12, 14, startCol = 5, startRow = 5, seeds = listOf(EnemySeed(5 * 16, 7 * 16, 0x0A))) { g ->
            for (c in 0 until 12) g[13][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        assertTrue(k.winged)
        e.run(60) // Mario cae encima
        assertFalse(k.winged, "pisarla le quita las alas")
        assertTrue(k.alive, "no muere: queda de andador")
    }

    @Test
    fun `pisar un caparazon que se desliza lo para`() {
        val e = engineEnemies(12, 10, startCol = 5, startRow = 3, seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0x00))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        e.run(10)
        k.shell = true; k.shellMoving = true; k.vx = 0f
        e.run(40) // Mario cae encima
        assertFalse(k.shellMoving, "pisar el caparazón que se desliza lo detiene")
        assertTrue(k.shell)
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

    @Test
    fun `la Planta Piraña de tubo empieza metida y asoma con Mario lejos`() {
        // Mario lejos (col 0); la piraña en el tubo de la col 10.
        val e = engineEnemies(20, 12, startCol = 0, startRow = 10, seeds = listOf(EnemySeed(10 * 16, 6 * 16, 0x1A))) { g ->
            for (c in 0 until 20) g[11][c] = SmwSolidity.SOLID
        }
        val pir = e.enemies.single()
        val spawnY = 6f * 16
        assertTrue(pir.hidden, "empieza metida en el tubo")
        assertEquals(spawnY, pir.y, 0.1f, "en reposo está en su posición de tubo")
        e.run(84) // metida (32) + saliendo (48) + margen
        assertTrue(pir.y <= spawnY - 40f, "asomó ~48 px hacia arriba (y=${pir.y})")
        assertFalse(pir.hidden, "fuera del tubo ya no está 'metida'")
    }

    @Test
    fun `la Planta Piraña de tubo no sale si Mario está encima`() {
        // Mario justo sobre el tubo (misma columna): la piraña no debe emerger.
        val e = engineEnemies(20, 12, startCol = 10, startRow = 6, seeds = listOf(EnemySeed(10 * 16, 6 * 16, 0x1A))) { g ->
            for (c in 0 until 20) g[7][c] = SmwSolidity.SOLID
        }
        val pir = e.enemies.single()
        val hidY = pir.y
        e.run(200)
        assertTrue(pir.hidden, "sigue metida mientras Mario está encima")
        assertEquals(hidY, pir.y, 1f, "no asomó (y=${pir.y})")
        assertFalse(e.player.dead, "metida no hiere a Mario")
    }

    @Test
    fun `la Planta Piraña saltarina salta en arco por encima del tubo`() {
        val e = engineEnemies(20, 14, startCol = 0, startRow = 12, seeds = listOf(EnemySeed(10 * 16, 8 * 16, 0x4F))) { g ->
            for (c in 0 until 20) g[13][c] = SmwSolidity.SOLID
        }
        val pir = e.enemies.single()
        val spawnY = 8f * 16
        assertEquals(spawnY, pir.y, 0.1f, "espera en el tubo a su altura")
        var highest = spawnY
        for (i in 0 until 200) { e.tick(); highest = minOf(highest, pir.y) }
        assertTrue(highest < spawnY - 16f, "saltó al menos una casilla por encima (subió a $highest)")
    }

    @Test
    fun `la Planta Piraña de fuego escupe bolas al saltar`() {
        val e = engineEnemies(20, 14, startCol = 0, startRow = 12, seeds = listOf(EnemySeed(10 * 16, 8 * 16, 0x50))) { g ->
            for (c in 0 until 20) g[13][c] = SmwSolidity.SOLID
        }
        var sawFire = false
        for (i in 0 until 200) { e.tick(); if (e.enemyProjectiles.isNotEmpty()) sawFire = true }
        assertTrue(sawFire, "la piraña de fuego escupió al menos una bola")
        assertTrue(e.piranhaFireEvents > 0, "hubo evento de fuego")
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

    // ------------------------------------------------------------------- rampas

    /**
     * Motor con suelo llano y una RAMPA de 45° en (6,9): forma 12 («/», sube a la
     * derecha) o 13 («\», baja a la derecha), con meseta sólida en (7..,9) para la 12.
     */
    private fun slopeEngine(shape: Int, startCol: Int): com.rolebuilder.core.engine.platformer.PlatformerEngine {
        val cols = 14; val rows = 12
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[10][c] = SmwSolidity.SOLID          // suelo llano
        grid[9][6] = SmwSolidity.SLOPE                                    // la rampa
        if (shape == com.rolebuilder.core.snes.SmwSlopes.SHAPE_45_UP_RIGHT) {
            for (c in 7 until cols) grid[9][c] = SmwSolidity.SOLID        // meseta arriba
        }
        return PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = startCol * 16, startPixelY = 10 * 16 - 15,
            tuning = tuning,
            slopeOffsetsAt = { c, r ->
                if (c == 6 && r == 9) com.rolebuilder.core.snes.SmwSlopes.floorOffsets(shape) else null
            },
        )
    }

    @Test
    fun `la rampa no bloquea de lado y Mario SUBE andando por ella`() {
        val e = slopeEngine(com.rolebuilder.core.snes.SmwSlopes.SHAPE_45_UP_RIGHT, startCol = 3)
        e.run(30) // se posa en el suelo llano (pies en 160)
        e.moveX = 1f
        e.run(180) // anda hacia la rampa y la sube
        assertTrue(e.player.x > 7 * 16f, "no se queda parado en la pared de la rampa (x=${e.player.x})")
        assertTrue(e.player.onGround, "acaba de pie")
        val feet = e.player.y + e.playerHeight
        assertTrue(feet <= 9 * 16f + 1f, "los pies acabaron a la altura de la meseta (feet=$feet)")
    }

    @Test
    fun `bajar la rampa mantiene los pies pegados (sin escalones voladores)`() {
        // Rampa «\» que baja a la derecha; Mario arranca sobre su borde alto.
        val e = slopeEngine(com.rolebuilder.core.snes.SmwSlopes.SHAPE_45_DOWN_RIGHT, startCol = 5)
        e.run(40) // se posa
        e.moveX = 1f
        var airborne = 0
        var maxAirborne = 0
        repeat(90) {
            e.tick()
            if (!e.player.onGround) { airborne++; if (airborne > maxAirborne) maxAirborne = airborne }
            else airborne = 0
        }
        assertTrue(maxAirborne <= 2, "bajando la cuesta no se queda flotando ($maxAirborne ticks en el aire)")
    }

    @Test
    fun `agachado sobre la rampa Mario se desliza y arrolla al enemigo`() {
        val cols = 14; val rows = 12
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[10][c] = SmwSolidity.SOLID
        grid[9][5] = SmwSolidity.SLOPE // rampa «\» baja hacia la derecha
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 5 * 16 - 4, startPixelY = 9 * 16 - 15,
            tuning = tuning,
            enemySeeds = listOf(EnemySeed(8 * 16, 10 * 16 - 14, 0)), // enemigo cuesta abajo
            slopeOffsetsAt = { c, r ->
                if (c == 5 && r == 9) {
                    com.rolebuilder.core.snes.SmwSlopes.floorOffsets(com.rolebuilder.core.snes.SmwSlopes.SHAPE_45_DOWN_RIGHT)
                } else null
            },
        )
        e.run(20) // se posa sobre la rampa
        e.inputDown = true // agachado: TOBOGÁN
        e.run(6)
        assertTrue(e.player.sliding, "se está deslizando")
        assertTrue(e.player.vx > 0.4f, "acelera cuesta abajo (vx=${e.player.vx})")
        e.run(90) // baja la rampa y sigue arrollando por el llano con la inercia
        val enemy = e.enemies.single()
        assertFalse(enemy.alive, "el tobogán arrolla al enemigo")
        assertFalse(e.player.dead, "sin daño para Mario")
    }

    @Test
    fun `el perfil se deduce del DIBUJO del tile (rampas generales sin configurar)`() {
        // Tile 16×16 con diagonal «/»: transparente arriba, opaco debajo de y = 15-x.
        val px = IntArray(256)
        for (y in 0 until 16) for (x in 0 until 16) {
            if (y >= 15 - x) px[y * 16 + x] = 0xFF000000.toInt()
        }
        val profile = com.rolebuilder.core.snes.SmwSlopes.profileFromTilePixels(px)
        assertTrue(profile != null, "la silueta diagonal da perfil")
        assertEquals(15, profile!![0], "columna 0: suelo abajo")
        assertEquals(0, profile[15], "columna 15: suelo arriba")
        // Un tile macizo o vacío NO da perfil (queda como bloque, seguro).
        assertTrue(com.rolebuilder.core.snes.SmwSlopes.profileFromTilePixels(IntArray(256) { 0xFF000000.toInt() }) == null)
        assertTrue(com.rolebuilder.core.snes.SmwSlopes.profileFromTilePixels(IntArray(256)) == null)
        // Y el motor lo juega tal cual: rampa «/» desde el perfil del dibujo.
        val cols = 14; val rows = 12
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[10][c] = SmwSolidity.SOLID
        grid[9][6] = SmwSolidity.SLOPE
        for (c in 7 until cols) grid[9][c] = SmwSolidity.SOLID
        val e = PlatformerEngine(
            cols, rows, solidityAt = { c, r -> grid[r][c] },
            startPixelX = 3 * 16, startPixelY = 10 * 16 - 15,
            tuning = tuning,
            slopeOffsetsAt = { c, r -> if (c == 6 && r == 9) profile else null },
        )
        e.run(30)
        e.moveX = 1f
        e.run(180)
        assertTrue(e.player.x > 7 * 16f, "sube la rampa deducida del dibujo (x=${e.player.x})")
        assertTrue(e.player.onGround)
    }

    @Test
    fun `una cuesta SIN forma sigue siendo bloque macizo (compatibilidad)`() {
        val e = engine(10, 10, startCol = 2, startRow = 7) { g ->
            for (c in 0 until 10) g[8][c] = SmwSolidity.SOLID
            for (r in 0 until 8) g[r][6] = SmwSolidity.SLOPE // muro de "cuesta" sin forma
        }
        e.run(20)
        e.moveX = 1f; e.run(120)
        assertTrue(e.player.x <= 96f - tuning.playerWidth + 0.5f, "sin forma, la cuesta bloquea como pared")
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
