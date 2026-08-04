package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.EnemyBehavior
import com.rolebuilder.core.engine.platformer.EnemySeed
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Lote de enemigos FLOTANTES (sin gravedad) con conducta propia: Boo (tímido), Eerie (vuela
 * ondulando), Cheep-Cheep (nada) y Rip Van Fish (duerme y persigue). Antes todos caían en el
 * andador genérico: andaban por el suelo como Goombas, que no es lo que hacen en SMW.
 */
class FloatingEnemiesTest {

    private val tuning = PlatformerTuning(
        jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
        maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
    )

    /** Mundo vacío (sin suelo): si un enemigo tuviera gravedad, se hundiría. */
    private fun engineWith(id: Int, ex: Int, ey: Int, px: Int = 5, py: Int = 6): PlatformerEngine =
        PlatformerEngine(
            cols = 40, rows = 20,
            solidityAt = { _, _ -> SmwSolidity.NONE },
            startPixelX = px * 16, startPixelY = py * 16,
            tuning = tuning,
            enemySeeds = listOf(EnemySeed(ex * 16, ey * 16, id)),
        )

    @Test
    fun `los flotantes tienen su propia conducta y no la del andador`() {
        assertEquals(EnemyBehavior.BOO, engineWith(0x37, 10, 6).enemies[0].behavior)
        assertEquals(EnemyBehavior.EERIE, engineWith(0x38, 10, 6).enemies[0].behavior)
        assertEquals(EnemyBehavior.SWIMMER, engineWith(0x15, 10, 6).enemies[0].behavior)
        assertEquals(EnemyBehavior.RIP_VAN_FISH, engineWith(0x3D, 10, 6).enemies[0].behavior)
    }

    @Test
    fun `no caen y flotan aunque no haya suelo`() {
        for (id in listOf(0x37, 0x38, 0x15)) {
            val g = engineWith(id, 10, 6)
            val e = g.enemies[0]
            val y0 = e.y
            repeat(60) { g.tick() }
            // Con gravedad habrían caído decenas de píxeles; aquí solo ondulan un poco.
            assertTrue(abs(e.y - y0) < 24f, "el enemigo 0x${id.toString(16)} no debe caerse")
        }
    }

    @Test
    fun `el Boo se para si Mario lo MIRA y avanza si no`() {
        // Boo a la DERECHA de Mario.
        val g = engineWith(0x37, ex = 14, ey = 6, px = 5, py = 6)
        val boo = g.enemies[0]

        // Mario mirando a la derecha = lo está mirando → el Boo no se mueve.
        g.player.facingRight = true
        val xLooking = boo.x
        repeat(30) { g.tick() }
        assertEquals(xLooking, boo.x, "mirado, el Boo se queda quieto")

        // Mario mirando a la izquierda = le da la espalda → el Boo se acerca.
        g.player.facingRight = false
        repeat(30) { g.tick() }
        assertTrue(boo.x < xLooking, "sin mirarlo, el Boo persigue a Mario")
    }

    @Test
    fun `el Rip Van Fish duerme lejos y persigue cerca`() {
        // Lejos (más de RIPVAN_WAKE_DIST): sigue dormido.
        val lejos = engineWith(0x3D, ex = 30, ey = 6, px = 2, py = 6)
        val dormido = lejos.enemies[0]
        val x0 = dormido.x
        repeat(40) { lejos.tick() }
        assertEquals(x0, dormido.x, "lejos de Mario sigue dormido")

        // Cerca: despierta y se acerca.
        val cerca = engineWith(0x3D, ex = 7, ey = 6, px = 5, py = 6)
        val despierto = cerca.enemies[0]
        val x1 = despierto.x
        repeat(40) { cerca.tick() }
        assertTrue(despierto.x < x1, "cerca de Mario despierta y lo persigue")
    }
}
