package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.PlatformerEnvironment
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.snes.SmwPhysics
import com.rolebuilder.core.snes.SmwPowerup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de los powerups de Mario y de las variantes de físicas por entorno
 * (hielo resbaladizo, agua submarina), derivadas de las tablas reales de SMW.
 */
class SmwPowerupTest {

    private fun physics() = SmwPhysics(
        jumpYSpeed = intArrayOf(-80), marioXAccel = IntArray(0), iceXAccel = IntArray(0),
        maxXSpeed = IntArray(0), maxXSpeedExtra = IntArray(0),
        friction1 = IntArray(0), friction2 = IntArray(0),
        gravity = intArrayOf(6, 3), maxFallSpeed = intArrayOf(0x40),
    )

    @Test
    fun `los powerups tienen tamano y habilidades correctos`() {
        assertEquals(1, SmwPowerup.SMALL.heightTiles)
        assertEquals(2, SmwPowerup.BIG.heightTiles)
        assertTrue(SmwPowerup.CAPE.canFlyWithCape)
        assertTrue(SmwPowerup.FIRE.canShootFire)
        assertTrue(SmwPowerup.BIG.canBreakBlocks && !SmwPowerup.SMALL.canBreakBlocks)
        assertTrue(!SmwPowerup.SMALL.canDuck && SmwPowerup.BIG.canDuck)
        // Al recibir un golpe, todos menos el pequeño bajan a pequeño; el pequeño muere.
        assertEquals(SmwPowerup.SMALL, SmwPowerup.FIRE.downgrade)
        assertNull(SmwPowerup.SMALL.downgrade)
        assertEquals(SmwPowerup.CAPE, SmwPowerup.of(2))
    }

    @Test
    fun `Mario grande ocupa dos casillas de alto`() {
        val small = PlatformerTuning.fromSmw(physics(), SmwPowerup.SMALL)
        val big = PlatformerTuning.fromSmw(physics(), SmwPowerup.BIG)
        assertTrue(small.playerHeight <= 16f, "pequeño cabe en una casilla")
        assertTrue(big.playerHeight > 16f && big.playerHeight <= 32f, "grande ~2 casillas")
    }

    @Test
    fun `el hielo reduce el rozamiento (Mario patina)`() {
        val land = PlatformerTuning.fromSmw(physics(), environment = PlatformerEnvironment.LAND)
        val ice = PlatformerTuning.fromSmw(physics(), environment = PlatformerEnvironment.ICE)
        assertTrue(ice.friction < land.friction, "en hielo hay menos rozamiento")
    }

    @Test
    fun `el agua limita la caida y suaviza la gravedad`() {
        val land = PlatformerTuning.fromSmw(physics(), environment = PlatformerEnvironment.LAND)
        val water = PlatformerTuning.fromSmw(physics(), environment = PlatformerEnvironment.WATER)
        assertEquals(1f, water.maxFallSpeed, 0.001f, "en agua la caída va capada a ~1 px/fotograma")
        assertTrue(water.maxFallSpeed < land.maxFallSpeed)
        assertTrue(water.gravityFall < land.gravityFall, "gravedad submarina más suave")
    }
}
