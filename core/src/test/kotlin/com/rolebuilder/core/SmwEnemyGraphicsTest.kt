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

    @Test
    fun `roster incluye los sprites nuevos verificados`() {
        // Añadidos al final (no deben alterar el orden de los fotogramas ya horneados).
        assertEquals("Planta Piraña", SmwEnemyGraphics.nameOf(0x1A))
        assertEquals("Planta Piraña saltarina", SmwEnemyGraphics.nameOf(0x4F))
        assertEquals("Interruptor P", SmwEnemyGraphics.nameOf(0x3E))
        val ids = SmwEnemyGraphics.curatedIds
        assertEquals(listOf(0x1A, 0x4F, 0x3E), ids.takeLast(3))
    }

    @Test
    fun `spriteImageAnyId ignora ids fuera de la tabla OAM`() {
        // Sin ROM válida no hay imagen, pero el contrato de rango es puro: un id fuera
        // de la tabla real siempre es null (0x54 queda justo pasado el último id, 0x53).
        val header = com.rolebuilder.core.snes.SnesDecoder.parseHeader(ByteArray(0x10000))
        assertNull(SmwEnemyGraphics.spriteImageAnyId(ByteArray(0x10000), header, 0x105, 0x54))
    }

    @Test
    fun `catalogo de sprites grandes verificados`() {
        val ids = SmwEnemyGraphics.bigSprites.map { it.id }
        // Los 5 sprites grandes reconstruidos y verificados.
        assertEquals(listOf(0x10, 0x08, 0x1F, 0x26, 0x91), ids)
        // Cada uno tiene receta OAM (al menos una parte) y un nivel de composición.
        for (bs in SmwEnemyGraphics.bigSprites) {
            assertTrue(bs.parts.isNotEmpty(), "${bs.name} debería tener partes OAM")
            assertTrue(bs.level > 0)
        }
        assertEquals("Thwomp", SmwEnemyGraphics.bigSpriteFor(0x26)?.name)
        assertNull(SmwEnemyGraphics.bigSpriteFor(0x00)) // Koopa normal no es sprite grande
    }

    @Test
    fun `renderOam exige entradas y ROM de SMW`() {
        val header = com.rolebuilder.core.snes.SnesDecoder.parseHeader(ByteArray(0x10000))
        // Lista vacía → null (contrato puro, sin depender de la ROM).
        assertNull(SmwEnemyGraphics.renderOam(ByteArray(0x10000), header, 0x106, emptyList()))
        // Con entradas pero ROM en blanco (sin datos de nivel SMW) → null.
        val part = SmwEnemyGraphics.OamPart(charnum = 0xAA, dx = 0, dy = 0, size = 16, prop = 0x04)
        assertNull(SmwEnemyGraphics.renderOam(ByteArray(0x10000), header, 0x106, listOf(part)))
    }
}
