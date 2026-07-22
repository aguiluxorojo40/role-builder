package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwSpriteAim
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El vector de puntería debe coincidir 1:1 con `AimTowardsPlayer` ($02:D2FB): el eje dominante
 * lleva la magnitud pedida y el otro una fracción entera (Bresenham), con el signo hacia el
 * jugador. Valores calculados a mano a partir del algoritmo del disassembly.
 */
class SmwSpriteAimTest {
    @Test
    fun `recto a la derecha`() {
        // Δx=100, Δy=0, speed=0x10 -> eje X domina, sin componente Y.
        assertEquals(Pair(0x10, 0), SmwSpriteAim.aimTowardsPlayer(100, 0, 0x10))
    }

    @Test
    fun `recto a la izquierda`() {
        assertEquals(Pair(-0x10, 0), SmwSpriteAim.aimTowardsPlayer(-50, 0, 0x10))
    }

    @Test
    fun `diagonal 2 a 1 hacia abajo-derecha`() {
        // Δx=100, Δy=50 -> X domina (16), Y = 16·50/100 = 8.
        assertEquals(Pair(0x10, 8), SmwSpriteAim.aimTowardsPlayer(100, 50, 0x10))
    }

    @Test
    fun `Y domina hacia arriba-izquierda`() {
        // Δx=-30, Δy=-60 -> Y domina (magnitud 16, negativa=arriba); X = 16·30/60 = 8, negativa.
        assertEquals(Pair(-8, -0x10), SmwSpriteAim.aimTowardsPlayer(-30, -60, 0x10))
    }

    @Test
    fun `45 grados reparte por igual`() {
        // Δx=Δy -> ambos ejes a la magnitud pedida.
        assertEquals(Pair(0x10, 0x10), SmwSpriteAim.aimTowardsPlayer(40, 40, 0x10))
    }
}
