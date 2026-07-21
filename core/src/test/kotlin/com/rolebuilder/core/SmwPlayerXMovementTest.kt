package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwPhysics
import com.rolebuilder.core.snes.SmwPlayerXMovement
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifica el port 1:1 del manejo horizontal de Mario ([SmwPlayerXMovement]) contra los
 * valores REALES de las tablas de la ROM US (`bank_00.asm`, IsoFrieze/SMWDisX):
 * `MarioAccel_`, `$D2CD`/`$D309` (rozamiento), `$D535` (tope), `$D43D` (hielo).
 */
class SmwPlayerXMovementTest {

    /** Físicas con los primeros tramos REALES de la ROM US (bastan para suelo llano). */
    private fun physUS(): SmwPhysics = SmwPhysics(
        jumpYSpeed = intArrayOf(0xB0.toByte().toInt()),
        // MarioAccel_ (words 0..75): incluye el tramo de derrape (+$90 bytes = word 72+).
        marioXAccel = words(
            0xFE80, 0xFE80, 0x0180, 0x0180,  // 0  andar/correr izq/der
            0xFE80, 0xFE80, 0x0180, 0x0180,  // 1
            0xFE80, 0xFE80, 0x0180, 0x0180,  // 2
            0xFE80, 0xFE80, 0x0140, 0x0140,  // 3
            0xFEC0, 0xFEC0, 0x0180, 0x0180,  // 4
            0xFE80, 0xFE80, 0x0100, 0x0100,  // 5
            0xFF00, 0xFF00, 0x0180, 0x0180,  // 6
            0xFE80, 0xFE80, 0x0100, 0x0100,  // 7
            0xFE80, 0xFE80, 0x0100, 0x0100,  // 8
            0xFF00, 0xFF00, 0x0180, 0x0180,  // 9
            0xFF00, 0xFF00, 0x0180, 0x0180,  // 10
            0xFC00, 0xFC00, 0xFD00, 0xFD00,  // 11
            0x0300, 0x0300, 0x0400, 0x0400,  // 12
            0xFC00, 0xFC00, 0x0600, 0x0600,  // 13
            0xFA00, 0xFA00, 0x0400, 0x0400,  // 14
            0xFF80, 0x0080, 0xFF00, 0x0100,  // 15
            0xFE80, 0x0180, 0xFE80, 0xFE80,  // 16
            0x0180, 0x0180, 0xFE80, 0x0280,  // 17
            0xFD80, 0xFB00, 0x0280, 0x0500,  // 18  derrape: word72=−640 der..., word74=+640
        ),
        // $D43D hielo (words 0..3): aceleración más suave (andar der = +$0080 vs +$0180).
        iceXAccel = words(0xFF80, 0xFE80, 0x0080, 0x0180),
        // $D535 tope (bytes 0..7): andar ∓20, correr ∓36, ∓36, despegue ∓48.
        maxXSpeed = bytes(0xEC, 0x14, 0xDC, 0x24, 0xDC, 0x24, 0xD0, 0x30),
        maxXSpeedExtra = IntArray(0),
        // $D2CD rozamiento suelo seco (words 0..3): ∓256.
        friction1 = words(0xFF00, 0x0100, 0xFF00, 0x0100),
        // $D309 rozamiento de agua (words 0..3): ∓32 (más suave).
        friction2 = words(0xFFE0, 0x0020, 0xFFE0, 0x0020),
        gravity = intArrayOf(6, 3),
        maxFallSpeed = intArrayOf(0x40),
    )

    private fun words(vararg v: Int) = IntArray(v.size) { v[it].toShort().toInt() }
    private fun bytes(vararg v: Int) = IntArray(v.size) { v[it].toByte().toInt() }

    @Test
    fun `andar a la derecha se estabiliza en el tope de andar (0x14 = 1_25 px por f)`() {
        val m = SmwPlayerXMovement(physUS())
        repeat(80) { m.step(dir = 1, running = false, onGround = true, onIce = false, water = false) }
        // Tope de andar = 0x14 (20/16 = 1.25 px/f); oscila alrededor por el sobrepaso del ROM.
        assertTrue(m.speedHi in 19..21, "velocidad de andar ~0x14, fue ${m.speedHi}")
        assertEquals(1, m.direction)
    }

    @Test
    fun `correr alcanza un tope mayor que andar`() {
        val walk = SmwPlayerXMovement(physUS())
        repeat(120) { walk.step(1, running = false, onGround = true, onIce = false, water = false) }
        val run = SmwPlayerXMovement(physUS())
        repeat(120) { run.step(1, running = true, onGround = true, onIce = false, water = false) }
        // Correr (tope 0x24 = 36) claramente por encima de andar (0x14 = 20).
        assertTrue(run.speedHi > walk.speedHi + 8, "correr ${run.speedHi} debe superar andar ${walk.speedHi}")
        assertTrue(run.speedHi in 34..38, "tope de correr ~0x24, fue ${run.speedHi}")
    }

    @Test
    fun `soltar la direccion en el suelo decelera hasta 0`() {
        val m = SmwPlayerXMovement(physUS())
        repeat(120) { m.step(1, running = true, onGround = true, onIce = false, water = false) }
        assertTrue(m.speedHi > 0, "arranca en marcha")
        repeat(120) { m.step(0, running = false, onGround = true, onIce = false, water = false) }
        assertEquals(0, m.speedHi, "el rozamiento lo para del todo")
    }

    @Test
    fun `en el aire sin direccion NO hay rozamiento (conserva la velocidad)`() {
        val m = SmwPlayerXMovement(physUS())
        repeat(120) { m.step(1, running = false, onGround = true, onIce = false, water = false) }
        val before = m.speedHi
        repeat(30) { m.step(0, running = false, onGround = false, onIce = false, water = false) }
        assertEquals(before, m.speedHi, "sin suelo la velocidad horizontal se conserva")
    }

    @Test
    fun `girar a velocidad activa el derrape y voltea la cara`() {
        val m = SmwPlayerXMovement(physUS())
        // corre a la izquierda hasta velocidad de sobra
        repeat(60) { m.step(-1, running = true, onGround = true, onIce = false, water = false) }
        assertTrue(m.speedHi < -10 && m.direction == 0, "va rápido a la izquierda")
        // ahora pulsa derecha: debe derrapar (pose de giro) y mirar a la derecha
        m.step(1, running = true, onGround = true, onIce = false, water = false)
        assertTrue(m.skidding, "cambiar de sentido a velocidad = derrape")
        assertEquals(1, m.direction, "la cara pasa a la derecha")
    }

    @Test
    fun `en hielo cuesta mas arrancar que en tierra`() {
        val land = SmwPlayerXMovement(physUS())
        val ice = SmwPlayerXMovement(physUS())
        repeat(6) {
            land.step(1, running = false, onGround = true, onIce = false, water = false)
            ice.step(1, running = false, onGround = true, onIce = true, water = false)
        }
        // La aceleración de hielo ($0080) es un tercio de la de tierra ($0180): patina.
        assertTrue(ice.speedHi < land.speedHi, "en hielo arranca más despacio (${ice.speedHi} < ${land.speedHi})")
    }

    @Test
    fun `correr a tope de despegue carga el medidor-P`() {
        val m = SmwPlayerXMovement(physUS())
        assertEquals(0, m.pMeter)
        repeat(200) { m.step(1, running = true, onGround = true, onIce = false, water = false) }
        // A velocidad de despegue (|v| >= 0x23) el medidor sube +2/f hasta el tope 0x70.
        assertEquals(SmwPlayerXMovement.PMETER_MAX, m.pMeter, "el medidor-P llega al tope corriendo")
    }

    @Test
    fun `el medidor-P decae al dejar de correr`() {
        val m = SmwPlayerXMovement(physUS())
        repeat(200) { m.step(1, running = true, onGround = true, onIce = false, water = false) }
        assertTrue(m.pMeter > 0)
        repeat(200) { m.step(0, running = false, onGround = true, onIce = false, water = false) }
        assertEquals(0, m.pMeter, "quieto, el medidor-P se vacía")
    }
}
