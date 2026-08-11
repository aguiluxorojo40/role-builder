package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.BlockAction
import com.rolebuilder.core.engine.platformer.EnemyBehavior
import com.rolebuilder.core.engine.platformer.EnemySeed
import com.rolebuilder.core.engine.platformer.EngineWarp
import com.rolebuilder.core.engine.platformer.PlatformerEnemy
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PowerupKind
import com.rolebuilder.core.engine.platformer.WarpInput
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.snes.SmwPhysics
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
            seeds = listOf(EnemySeed(9 * 16, 6 * 16, 0x0F)))
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
    fun `jefe - Bowser suelta un Mechakoopa cuando Mario está cerca`() {
        val e = engineGrab(18, 10, startCol = 2, startRow = 6, grabCol = 16, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(6 * 16, 6 * 16, 0xA0)))  // Bowser a 4 casillas de Mario
        assertEquals(1, e.enemies.size, "solo Bowser al arrancar")
        e.run(PlatformerEngine.BOSS_ATTACK_INTERVAL + 5)
        val mechas = e.enemies.count { it.behavior == EnemyBehavior.MECHAKOOPA }
        assertTrue(mechas >= 1, "Bowser genera un Mechakoopa (0xA2)")
    }

    @Test
    fun `jefe - Bowser alterna la tanda de bolas de bolos tras la de Mechakoopas`() {
        // Techo (fila 5) entre Bowser (flota arriba) y Mario (suelo fila 9): los sub-sprites que
        // caen aterrizan en el techo y NO alcanzan a Mario, que así sobrevive toda la secuencia
        // (si Mario muere, tick() congela el mundo y Bowser dejaría de atacar).
        val e = engineEnemies(22, 12, startCol = 2, startRow = 8,
            seeds = listOf(EnemySeed(12 * 16, 3 * 16, 0xA0))) { g ->
            for (c in 0 until 22) g[9][c] = SmwSolidity.SOLID   // suelo de Mario
            for (c in 0 until 22) g[5][c] = SmwSolidity.SOLID   // techo que para a los sub-sprites
        }
        // Primero una tanda de Mechakoopas (BOWSER_ATTACKS_PER_PHASE) y luego la de bolas: hay
        // que dejar pasar toda la primera fase + el primer ataque de la segunda.
        val n = PlatformerEngine.BOWSER_ATTACKS_PER_PHASE
        e.run((n + 1) * PlatformerEngine.BOSS_ATTACK_INTERVAL + 10)
        assertFalse(e.player.dead, "Mario sobrevive bajo el techo (el mundo no se congela)")
        val balls = e.enemies.count { it.behavior == EnemyBehavior.BOWLING_BALL }
        assertTrue(balls >= 1, "tras la tanda de Mechakoopas, Bowser deja caer bolas de bolos (0xA1)")
    }

    @Test
    fun `jefe - Bowser FLOTA arriba y no cae al suelo`() {
        val e = engineGrab(20, 14, startCol = 2, startRow = 11, grabCol = 18, grabRow = 11, floorRow = 12,
            seeds = listOf(EnemySeed(9 * 16, 10 * 16, 0xA0)))  // Bowser arranca abajo
        val boss = e.enemies.first()
        val y0 = boss.y
        e.run(200)
        // Sube hacia la franja de vuelo (fila BOWSER_HOVER_ROW): NO cae al suelo (fila 12), gana
        // altura respecto a donde arrancó.
        assertTrue(boss.y < y0, "Bowser gana altura en vez de caer (y=${boss.y}, y0=$y0)")
        assertTrue(boss.y < 6 * 16f, "flota en la franja superior (y=${boss.y})")
        assertTrue(boss.alive)
    }

    @Test
    fun `jefe - Bowser flotante DERIVA hacia Mario en X`() {
        // Mario a la izquierda (col 2); Bowser arranca a la derecha (col 15) y debe acercarse.
        val e = engineGrab(20, 14, startCol = 2, startRow = 11, grabCol = 18, grabRow = 11, floorRow = 12,
            seeds = listOf(EnemySeed(15 * 16, 3 * 16, 0xA0)))
        val boss = e.enemies.first()
        val x0 = boss.x
        e.run(60)
        assertTrue(boss.x < x0, "Bowser deriva hacia Mario (a la izquierda) (x=${boss.x})")
    }

    @Test
    fun `bola de bolos rueda hacia Mario tras rebotar`() {
        val e = engineGrab(20, 12, startCol = 2, startRow = 8, grabCol = 18, grabRow = 8, floorRow = 9,
            seeds = listOf(EnemySeed(12 * 16, 4 * 16, 0xA1)))   // bola arriba a la derecha de Mario
        val ball = e.enemies.first()
        assertEquals(EnemyBehavior.BOWLING_BALL, ball.behavior, "0xA1 es bola de bolos")
        val x0 = ball.x
        e.run(120)               // cae, rebota y empieza a rodar
        assertTrue(ball.x < x0, "la bola rueda hacia Mario (a la izquierda)")
    }

    @Test
    fun `Mechakoopa anda hacia Mario`() {
        val e = engineGrab(20, 10, startCol = 2, startRow = 6, grabCol = 18, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(12 * 16, 6 * 16, 0xA2)))  // Mechakoopa a la derecha de Mario
        val m = e.enemies.first()
        assertEquals(EnemyBehavior.MECHAKOOPA, m.behavior, "0xA2 es Mechakoopa")
        e.run(20)               // cae al suelo
        val x0 = m.x
        e.run(60)
        assertTrue(m.x < x0, "el Mechakoopa camina hacia Mario (a la izquierda)")
    }

    @Test
    fun `Mechakoopa - pisarlo lo VOLTEA (aturde) en vez de matarlo`() {
        val e = engineEnemies(12, 10, startCol = 5, startRow = 5,
            seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0xA2))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        val m = e.enemies.single()
        e.run(60)
        assertTrue(m.alive, "el Mechakoopa NO muere al pisarlo (se voltea, como en SMW)")
        assertTrue(m.stunned, "queda VOLTEADO (aturdido)")
        assertFalse(e.player.dead, "pisarlo no hiere a Mario")
    }

    @Test
    fun `Mechakoopa - volteado se coge y lanzado arrolla a otro enemigo`() {
        val e = engineEnemies(20, 10, startCol = 2, startRow = 6,
            seeds = listOf(
                EnemySeed(44, 8 * 16 - 14, 0xA2),          // Mechakoopa pegado al frente de Mario
                EnemySeed(12 * 16, 8 * 16 - 14, 0x00))) { g ->  // objetivo lejos a la derecha
            for (c in 0 until 20) g[8][c] = SmwSolidity.SOLID
        }
        val mech = e.enemies.first { it.behavior == EnemyBehavior.MECHAKOOPA }
        val target = e.enemies.first { it.behavior == EnemyBehavior.WALKER }
        mech.stunned = true; mech.stunTimer = PlatformerEngine.MECHA_STUN  // ya volteado
        e.run(20)                          // ambos se posan; el Mechakoopa cae quieto
        e.moveX = 1f; e.tick()             // Mario mira a la derecha
        e.running = true; e.tick()         // flanco de correr -> lo coge
        assertTrue(e.carriedEnemy != null, "Mario coge el Mechakoopa volteado")
        assertTrue(mech.carried, "el Mechakoopa queda cargado")
        e.moveX = 0f
        e.running = false; e.tick()        // suelta el botón
        e.running = true; e.tick()         // vuelve a pulsar -> lo lanza
        assertTrue(mech.thrown, "el Mechakoopa sale lanzado")
        assertTrue(mech.vx > 0f, "vuela hacia la derecha (adonde mira Mario)")
        e.run(120)                         // vuela hasta el enemigo
        assertFalse(target.alive, "el Mechakoopa lanzado arrolla al enemigo")
    }

    @Test
    fun `Mechakoopa - volteado se endereza al agotar el temporizador`() {
        val e = engineEnemies(12, 10, startCol = 2, startRow = 6,
            seeds = listOf(EnemySeed(8 * 16, 8 * 16 - 14, 0xA2))) { g ->  // lejos de Mario
            for (c in 0 until 12) g[8][c] = SmwSolidity.SOLID
        }
        val m = e.enemies.single()
        m.stunned = true; m.stunTimer = 30
        e.run(50)
        assertTrue(m.alive, "sigue vivo")
        assertFalse(m.stunned, "se endereza (despierta) al agotar el temporizador de volteo")
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
    fun `jefe - Koopaling escupe la bola de Ludwig HORIZONTAL hacia Mario`() {
        val e = engineGrab(18, 10, startCol = 2, startRow = 6, grabCol = 16, grabRow = 6, floorRow = 7,
            seeds = listOf(EnemySeed(6 * 16, 6 * 16, 0x29)))   // Koopaling a la derecha de Mario
        e.run(PlatformerEngine.BOSS_ATTACK_INTERVAL + 5)
        val fire = e.enemyProjectiles.firstOrNull { it.alive }
        assertTrue(fire != null, "el Koopaling escupe una bola")
        assertFalse(fire!!.arc, "vuela recta (sin gravedad)")
        assertEquals(0f, fire.vy, "horizontal, como la bola de Ludwig (0x34, DATA_01D0BE)")
        assertTrue(fire.vx < 0f, "hacia Mario (a la izquierda)")
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
        // Suelo corto (columnas 3..6) en la fila 8; el enemigo empieza sobre él. Se usa el
        // Koopa ROJO con caparazón (0x05), que es de los que giran en el borde (bit 0x02).
        val e = engineEnemies(12, 10, startCol = 0, startRow = 0, seeds = listOf(EnemySeed(5 * 16, 6 * 16, 0x05))) { g ->
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
    fun `pisar un Koopa deja el caparazon y saca al Koopa desnudo`() {
        // Los Koopa CON caparazón son 0x04-0x07 (no 0x00-0x03: ver canShell). Al pisarlos, el
        // bicho SALE del caparazón y huye; lo que queda en el suelo es el caparazón.
        // El mapa es ANCHO y el pisotón ocurre lejos de las paredes a propósito: el desnudo
        // huye a 4 px/f y, en una caja estrecha, rebotaría en la pared y volvería a por Mario
        // dentro de los 60 fotogramas (que es lo que hace en el juego, pero aquí estorba).
        val e = engineEnemies(40, 10, startCol = 20, startRow = 5, seeds = listOf(EnemySeed(20 * 16, 8 * 16 - 14, 0x04))) { g ->
            for (c in 0 until 40) g[9][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        assertTrue(k.canShell, "0x04 es un Koopa CON caparazón")
        // Avanza hasta el fotograma EXACTO en que aparece el desnudo (Mario tiene que caer).
        var esperados = 0
        while (esperados < 60 && e.enemies.none { it.isNakedKoopa }) { e.run(1); esperados++ }
        val desnudo = e.enemies.firstOrNull { it.isNakedKoopa }
        assertNotNull(desnudo, "pisar un Koopa deja el caparazón Y saca al Koopa desnudo")
        assertEquals(0x00, desnudo!!.id, "el verde con caparazón (0x04) deja al desnudo verde (0x00)")
        assertTrue(desnudo.sliding, "sale DESLIZÁNDOSE (spr_table1528), no andando")
        assertEquals(PlatformerEngine.NAKED_KOOPA_SPEED, kotlin.math.abs(desnudo.vx), 0.001f,
            "y lo hace a los 4 px/f de kSprStatus09_Stunned_DATA_0197AD")
        val x0 = desnudo.x
        e.run(40)
        assertTrue(k.alive, "el Koopa no muere al pisarlo")
        assertTrue(k.shell, "queda el caparazón")
        assertFalse(k.shellMoving, "el caparazón queda quieto")
        assertTrue(kotlin.math.abs(desnudo.x - x0) > 32f, "el Koopa desnudo se aleja de verdad")
        assertFalse(e.player.dead)
    }

    @Test
    fun `el Koopa desnudo colocado anda normal y muere de un pisoton`() {
        // El 0x00-0x03 es el "beach koopa": un pisotón lo mata, porque ya no tiene caparazón
        // que lo proteja. Y OJO: el COLOCADO en el nivel arranca ANDANDO a la velocidad
        // genérica (kUnk_1817d[0x00] = SprXXX_Generic_Init_StandardSpritesInit, $01:8575, que
        // solo lo hace mirar a Mario). Los 4 px/f son del que sale de un caparazón pisado.
        val e = engineEnemies(12, 10, startCol = 5, startRow = 5, seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0x00))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        assertTrue(k.isNakedKoopa, "0x00 es un Koopa desnudo")
        assertFalse(k.sliding, "el colocado NO nace deslizándose")
        assertEquals(PlatformerEngine.WALK_SPEED_SLOW, kotlin.math.abs(k.vx), 0.001f,
            "anda a 0x08 = 0.5 px/f, como el Koopa verde con caparazón")
        k.vx = 0f // lo fijamos bajo Mario para probar el pisotón de forma determinista
        e.run(60)
        assertFalse(k.alive, "el pisotón lo mata: no tiene caparazón")
        assertFalse(e.player.dead)
    }

    @Test
    fun `el Koopa rojo y el azul giran en el borde, el verde se tira`() {
        // Bit 0x02 de kSprXXX_Generic_Spr0to13Prop: giran 0x01/0x02 (desnudos rojo y azul) y
        // 0x05/0x06 (con caparazón rojo y azul). El verde y el amarillo, no.
        for (id in intArrayOf(0x01, 0x02, 0x05, 0x06)) {
            assertTrue(PlatformerEnemy(0f, 0f, id).turnsAtLedge, "0x%02X gira en el borde".format(id))
        }
        for (id in intArrayOf(0x00, 0x03, 0x04, 0x07, 0x0A, 0x0B)) {
            assertFalse(PlatformerEnemy(0f, 0f, id).turnsAtLedge, "0x%02X NO gira en el borde".format(id))
        }
    }

    @Test
    fun `tocar de lado un caparazon quieto lo patea`() {
        val e = engineEnemies(20, 10, startCol = 2, startRow = 7, seeds = listOf(EnemySeed(6 * 16, 8 * 16 - 14, 0x04))) { g ->
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
            seeds = listOf(EnemySeed(3 * 16, 8 * 16 - 14, 0x04), EnemySeed(9 * 16, 8 * 16 - 14, 0x0F))) { g ->
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
        val e = engineEnemies(12, 10, startCol = 5, startRow = 3, seeds = listOf(EnemySeed(5 * 16, 8 * 16 - 14, 0x04))) { g ->
            for (c in 0 until 12) g[9][c] = SmwSolidity.SOLID
        }
        val k = e.enemies.single()
        e.run(10)
        k.shell = true; k.shellMoving = true; k.vx = 0f
        e.run(40) // Mario cae encima
        assertFalse(k.shellMoving, "pisar el caparazón que se desliza lo detiene")
        assertTrue(k.shell)
    }

    // ------------------------------------------------- tubería HORIZONTAL (entrar de lado)

    /**
     * Escenario de tubería horizontal: suelo en la fila 8 y una BOCA sólida en (10,7), que
     * hace de pared. El warp va en la casilla ABIERTA de al lado, la (9,7), que es la que
     * pisa el jugador cuando llega empujando (la boca es sólida: su caja nunca la solapa).
     */
    private fun engineTuberiaLateral(
        warpCol: Int,
        input: WarpInput,
        bocaCol: Int = 10,
        salidaCol: Int = 4,
    ): PlatformerEngine {
        val cols = 14
        val rows = 10
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[8][c] = SmwSolidity.SOLID
        grid[7][bocaCol] = SmwSolidity.SOLID
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> grid[r][c] },
            startPixelX = salidaCol * 16, startPixelY = 6 * 16,
            tuning = tuning,
            warps = listOf(EngineWarp(warpCol, 7, input, destMapId = 9, destX = 5, destY = 6)),
        )
    }

    @Test
    fun `andar contra la boca de una tuberia horizontal activa el warp`() {
        val e = engineTuberiaLateral(warpCol = 9, input = WarpInput.SIDE_RIGHT)
        e.moveX = 1f
        e.run(120)
        val warp = e.pendingWarp
        assertNotNull(warp, "empujando a la derecha contra la boca se entra")
        assertEquals(9, warp.destMapId)
        assertEquals(5, warp.destX)
        assertEquals(6, warp.destY)
    }

    @Test
    fun `pasar por delante andando al reves NO entra en la tuberia horizontal`() {
        // En el juego hay que tener PULSADA la dirección hacia la que se mira
        // (`sub_F40A`, $00:F40A: `PIPE_BUTTONS[facing] & io_controller_hold1`). Un "de lado"
        // que valiese para cualquier movimiento te tragaría al pasar de largo.
        val e = engineTuberiaLateral(warpCol = 9, input = WarpInput.SIDE_RIGHT, salidaCol = 9)
        e.moveX = -1f
        e.run(30)
        assertNull(e.pendingWarp, "andando al revés sobre la misma celda no se entra")
    }

    @Test
    fun `la tuberia horizontal del otro lado se entra andando a la izquierda`() {
        val e = engineTuberiaLateral(
            warpCol = 4, input = WarpInput.SIDE_LEFT, bocaCol = 3, salidaCol = 10,
        )
        e.moveX = -1f
        e.run(120)
        assertNotNull(e.pendingWarp, "empujando a la izquierda contra la boca se entra")
    }

    @Test
    fun `el warp de lado NO va en la boca - la caja del jugador nunca la solapa`() {
        // La boca es SÓLIDA: el jugador se para pegado a ella y su caja se queda en la
        // casilla de al lado. Poner el warp en la boca es ponerlo donde no se puede llegar.
        val e = engineTuberiaLateral(warpCol = 10, input = WarpInput.SIDE_RIGHT)
        e.moveX = 1f
        e.run(120)
        assertNull(e.pendingWarp, "en la propia boca el warp no se dispara nunca")
    }
}
