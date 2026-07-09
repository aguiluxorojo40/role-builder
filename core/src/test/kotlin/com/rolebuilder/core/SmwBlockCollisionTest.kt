package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwBlockCollision
import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests del clasificador de solidez de bloques Map16 ([SmwBlockCollision]), portado
 * de la rutina de colisión del jugador de SMW ($00:EB77). Son SINTÉTICOS: comprueban
 * los umbrales exactos de la rutina y los cruzan con la geometría que el parser de
 * Layer 1 ([SmwLayer1]) escribe para un objeto conocido (suelo 0x100 + tierra 0x3F).
 * Sin ROM con copyright.
 */
class SmwBlockCollisionTest {

    @Test
    fun `pagina 0 (block code) no es terreno solido`() {
        // Bloques 0x000..0x0FF: monedas, bloques ?, mensajes, aire… → NONE.
        assertEquals(SmwSolidity.NONE, SmwBlockCollision.classify(0x025)) // aire
        assertEquals(SmwSolidity.NONE, SmwBlockCollision.classify(0x02F)) // moneda (page 0)
        assertEquals(SmwSolidity.NONE, SmwBlockCollision.classify(0x0FF))
        assertEquals(SmwSolidity.NONE, SmwBlockCollision.classify(0x3F))  // relleno de tierra
        // Fuera del nivel.
        assertEquals(SmwSolidity.NONE, SmwBlockCollision.classify(-1))
    }

    @Test
    fun `el borde de suelo 0x100 es solido solo por arriba`() {
        // Bloque 0x100 = alto 1, bajo 0x00 → LEDGE_TOP (plataforma de un sentido).
        assertEquals(SmwSolidity.LEDGE_TOP, SmwBlockCollision.classify(0x100))
        assertEquals(SmwSolidity.LEDGE_TOP, SmwBlockCollision.classify(0x110)) // bajo 0x10, aún de un sentido
        assertTrue(SmwBlockCollision.standsOn(0x100), "te paras encima")
        assertFalse(SmwBlockCollision.blocksSide(0x100), "lo atraviesas de lado")
        assertFalse(SmwBlockCollision.blocksCeiling(0x100), "saltas a través desde abajo")
    }

    @Test
    fun `los umbrales de solido, cuesta y pinchos coinciden con la rutina`() {
        // 0x11..0x6D → sólido por los 4 lados.
        assertEquals(SmwSolidity.SOLID, SmwBlockCollision.classify(0x111))
        assertEquals(SmwSolidity.SOLID, SmwBlockCollision.classify(0x16D))
        assertTrue(SmwBlockCollision.blocksSide(0x111) && SmwBlockCollision.blocksCeiling(0x111))
        // 0x6E..0xD7 → cuesta simple.
        assertEquals(SmwSolidity.SLOPE, SmwBlockCollision.classify(0x16E))
        assertEquals(SmwSolidity.SLOPE, SmwBlockCollision.classify(0x1D7))
        // 0xD8..0xFA → cuesta doble.
        assertEquals(SmwSolidity.SLOPE_STEEP, SmwBlockCollision.classify(0x1D8))
        assertEquals(SmwSolidity.SLOPE_STEEP, SmwBlockCollision.classify(0x1FA))
        assertTrue(SmwBlockCollision.isSlope(0x16E) && SmwBlockCollision.isSlope(0x1D8))
        // 0xFB..0xFF → muerte.
        assertEquals(SmwSolidity.SPIKE, SmwBlockCollision.classify(0x1FB))
        assertEquals(SmwSolidity.SPIKE, SmwBlockCollision.classify(0x1FF))
        assertTrue(SmwBlockCollision.isDeadly(0x1FF))
        assertFalse(SmwBlockCollision.standsOn(0x1FF), "los pinchos no son suelo")
    }

    /** ROM sintética con un nivel en 0x10000 y su puntero en la tabla de Layer 1. */
    private fun romWithLevel(vararg data: Int): ByteArray {
        val rom = ByteArray(0x70000)
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC] = 0x00
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 1] = 0x80.toByte()
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 2] = 0x02
        data.forEachIndexed { i, b -> rom[0x10000 + i] = b.toByte() }
        return rom
    }

    @Test
    fun `un ledge estandar da suelo de un sentido con tierra hueca debajo`() {
        // Mismo objeto que SmwLayer1Test: ledge estándar en fila 1, cols 2..5, con
        // 2 filas de tierra 0x3F debajo. Comprobamos la solidez resultante.
        val tm = SmwLayer1.parse(
            romWithLevel(
                0x00, 0x00, 0x00, 0x00, 0x00, // cabecera modo 0, tileset 0
                0x21, 0x42, 0x23,             // ledge en fila1,col2, ancho 4, 2 filas de tierra
                0xFF,
            ),
            0, 0,
        )!!
        // La superficie (0x100) es suelo de un sentido: te paras encima.
        for (c in 2..5) {
            assertEquals(SmwSolidity.LEDGE_TOP, SmwBlockCollision.classify(tm.block(c, 1)), "suelo col $c")
            assertTrue(SmwBlockCollision.standsOn(tm.block(c, 1)))
        }
        // La tierra de debajo (0x3F, página 0) NO es sólida: el interior de las lomas
        // de SMW es hueco para la colisión, solo la superficie para el jugador.
        for (c in 2..5) {
            assertEquals(SmwSolidity.NONE, SmwBlockCollision.classify(tm.block(c, 2)), "tierra col $c")
        }
        // El aire de alrededor tampoco colisiona.
        assertEquals(SmwSolidity.NONE, SmwBlockCollision.classify(tm.block(0, 0)))
    }
}
