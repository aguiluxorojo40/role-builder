package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests ROM-free del catálogo curado de gráficos de enemigos. El renderizado real
 * (de la ROM) se valida fuera de línea; aquí se comprueba el contrato del API.
 */
class SmwEnemyGraphicsTest {

    @Test
    fun `cubre los enemigos curados y no otros`() {
        for (id in intArrayOf(0x00, 0x01, 0x02, 0x03, 0x10)) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir 0x${id.toString(16)}")
            assertTrue(SmwEnemyGraphics.nameOf(id) != null)
        }
        assertFalse(SmwEnemyGraphics.handles(0x55))
        assertNull(SmwEnemyGraphics.nameOf(0x55))
    }

    @Test
    fun `nombres de los enemigos curados`() {
        assertEquals("Koopa verde", SmwEnemyGraphics.nameOf(0x00))
        assertEquals("Koopa rojo", SmwEnemyGraphics.nameOf(0x01))
        assertEquals("Koopa azul", SmwEnemyGraphics.nameOf(0x02))
        assertEquals("Koopa amarillo", SmwEnemyGraphics.nameOf(0x03))
        assertEquals("Goomba", SmwEnemyGraphics.nameOf(0x10))
    }
}
