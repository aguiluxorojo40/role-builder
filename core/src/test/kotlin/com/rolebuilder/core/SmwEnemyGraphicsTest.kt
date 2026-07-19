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
        assertEquals("Goomba volador", SmwEnemyGraphics.nameOf(0x10))
        assertEquals("Bullet Bill", SmwEnemyGraphics.nameOf(0x1C))
        assertEquals("Boo", SmwEnemyGraphics.nameOf(0x37))
    }

    @Test
    fun `el orden del atlas es estable, la tanda 0 conserva sus 15 fotogramas`() {
        // enemies.png se indexa por posición en curatedIds: si estos 15 se mueven,
        // el atlas horneado queda desincronizado en silencio. Los nuevos ids van
        // SIEMPRE al final (y el atlas se regenera con --enemies).
        val tanda0 = listOf(0x00, 0x01, 0x02, 0x03, 0x05, 0x0F, 0x10, 0x11, 0x1C, 0x29, 0x2A, 0x2C, 0x4B, 0x4D, 0x4E)
        assertEquals(tanda0, SmwEnemyGraphics.curatedIds.take(15))
    }

    @Test
    fun `la tanda 3 anade las Koopas aladas y las marca como aladas y animadas`() {
        for (id in 0x08..0x0B) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir la Parakoopa 0x${id.toString(16)}")
            assertTrue(SmwEnemyGraphics.isWinged(id), "0x${id.toString(16)} es alada")
            assertEquals(SmwEnemyGraphics.ATLAS_FRAMES, SmwEnemyGraphics.animFrameCount(id))
        }
        // Una Koopa CON caparazón normal no es alada, pero sí anima el andar.
        assertFalse(SmwEnemyGraphics.isWinged(0x05))
        assertEquals(SmwEnemyGraphics.ATLAS_FRAMES, SmwEnemyGraphics.animFrameCount(0x05))
        // Un id fuera de la familia andadora no anima (1 fotograma).
        assertFalse(SmwEnemyGraphics.isWinged(0x2C))
        assertEquals(1, SmwEnemyGraphics.animFrameCount(0x2C))
    }

    @Test
    fun `los enemigos con 2o fotograma real animan y los de rutina propia no`() {
        // Verificados renderizando ambos fotogramas desde la ROM (2º fotograma REAL).
        for (id in intArrayOf(0x15, 0x16, 0x2E, 0x31, 0x37, 0x38, 0x39, 0x3D, 0x4D, 0x4E)) {
            assertEquals(SmwEnemyGraphics.ATLAS_FRAMES, SmwEnemyGraphics.animFrameCount(id),
                "0x${id.toString(16)} debería animar")
        }
        // Su 2º byte OAM daría basura (animación de rutina propia): quedan estáticos.
        for (id in intArrayOf(0x1C, 0x29, 0x2A, 0x2C, 0x4B, 0x4F)) {
            assertEquals(1, SmwEnemyGraphics.animFrameCount(id), "0x${id.toString(16)} es estático")
        }
    }

    @Test
    fun `la tanda 1 esta cubierta y los descartados no`() {
        for (id in intArrayOf(0x4F, 0x37, 0x3D, 0x15, 0x16, 0x2E, 0x38, 0x39, 0x31)) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir 0x${id.toString(16)}")
        }
        // Descartados tras verificación visual: su entrada de la tabla genérica no es
        // su aspecto real (rutina de dibujo propia; salían fuentes o basura).
        for (id in intArrayOf(0x33, 0x30, 0x32)) {
            assertFalse(SmwEnemyGraphics.handles(id), "0x${id.toString(16)} está descartado")
        }
    }
}
