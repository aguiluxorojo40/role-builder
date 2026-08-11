package com.rolebuilder.core

import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.PlatformLayers
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/**
 * El convenio de capas de los niveles de plataformas: fondo en la 0, primer plano en la 1.
 * Lo que se comprueba aquí es lo que en la app se veía como "las herramientas Primer
 * plano y Fondo pintan en la capa contraria": los niveles importados de la ROM traían el
 * terreno en la capa 1 y los creados a mano en la 0.
 */
class PlatformLayersTest {

    /** Tileset de juguete: tesela 0 = aire, 1 = sólida, 2 = decorado. */
    private fun tileset() = Tileset(
        id = 1,
        name = "test",
        image = "t.png",
        tileSize = 16,
        columns = 4,
        rows = 1,
        passable = listOf(true, false, true, true),
        platformSolidity = listOf(
            SmwSolidity.NONE.ordinal,
            SmwSolidity.SOLID.ordinal,
            SmwSolidity.NONE.ordinal,
            SmwSolidity.NONE.ordinal,
        ),
    )

    private fun map(layer0: List<Int>, layer1: List<Int>) = GameMap(
        id = 1, name = "n", width = 2, height = 2, tilesetId = 1, layers = listOf(layer0, layer1),
    )

    private val vacia = List(4) { EMPTY_TILE }

    @Test
    fun `un nivel importado con fondo ya esta en el orden bueno`() {
        // Fondo (decorado) en la 0, terreno sólido en la 1: lo que hace la ROM.
        val m = map(listOf(2, 2, 2, 2), listOf(EMPTY_TILE, EMPTY_TILE, 1, 1))
        assertTrue(PlatformLayers.isCanonical(m, tileset()))
        assertEquals(m, PlatformLayers.normalized(m, tileset()))
    }

    @Test
    fun `un nivel creado a mano con el suelo en la capa 0 se da la vuelta`() {
        val m = map(listOf(EMPTY_TILE, EMPTY_TILE, 1, 1), vacia)
        assertFalse(PlatformLayers.isCanonical(m, tileset()))
        val fix = PlatformLayers.normalized(m, tileset())
        assertEquals(listOf(EMPTY_TILE, EMPTY_TILE, 1, 1), fix.layers[PlatformLayers.FOREGROUND])
        assertEquals(vacia, fix.layers[PlatformLayers.BACKGROUND])
    }

    @Test
    fun `normalizar dos veces no vuelve a darle la vuelta`() {
        val m = map(listOf(EMPTY_TILE, EMPTY_TILE, 1, 1), vacia)
        val once = PlatformLayers.normalized(m, tileset())
        assertEquals(once, PlatformLayers.normalized(once, tileset()))
    }

    @Test
    fun `sin tileset se usa el patron capa 1 vacia`() {
        val m = map(listOf(3, 3, 3, 3), vacia)
        val fix = PlatformLayers.normalized(m, null)
        assertEquals(listOf(3, 3, 3, 3), fix.layers[PlatformLayers.FOREGROUND])
        assertEquals(vacia, fix.layers[PlatformLayers.BACKGROUND])
    }

    @Test
    fun `un mapa de una sola capa se completa sin perder el terreno`() {
        val m = GameMap(id = 1, name = "n", width = 2, height = 2, layers = listOf(listOf(1, 1, 1, 1)))
        val fix = PlatformLayers.normalized(m, tileset())
        assertEquals(PlatformLayers.COUNT, fix.layers.size)
        assertEquals(listOf(1, 1, 1, 1), fix.layers[PlatformLayers.FOREGROUND])
        assertTrue(fix.layers[PlatformLayers.BACKGROUND].all { it == EMPTY_TILE })
    }

    @Test
    fun `un nivel vacio no se toca`() {
        val m = map(vacia, vacia)
        assertEquals(m, PlatformLayers.normalized(m, tileset()))
    }

    @Test
    fun `layersOf deja el fondo detras tenga o no fondo el nivel`() {
        val fg = listOf(1, 1, 1, 1)
        val bg = listOf(2, 2, 2, 2)
        val conFondo = PlatformLayers.layersOf(fg, bg, 2, 2)
        assertEquals(bg, conFondo[PlatformLayers.BACKGROUND])
        assertEquals(fg, conFondo[PlatformLayers.FOREGROUND])

        // Sin fondo, el terreno SIGUE en la capa 1 (antes se colaba en la 0).
        val sinFondo = PlatformLayers.layersOf(fg, emptyList(), 2, 2)
        assertEquals(fg, sinFondo[PlatformLayers.FOREGROUND])
        assertTrue(sinFondo[PlatformLayers.BACKGROUND].all { it == EMPTY_TILE })
    }
}
