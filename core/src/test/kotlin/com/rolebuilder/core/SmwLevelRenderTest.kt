package com.rolebuilder.core

import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contrato de la diferenciación Layer 1 / Layer 2 ([SnesGameRecipes.renderLevelLayers]),
 * ROM-free: sobre un [SmwLevelMap] sintético con atlas de 2 teselas (roja = primer plano,
 * azul = fondo) comprobamos que cada capa sale POR SEPARADO y que la escena combinada pone
 * el fondo debajo del primer plano. Los caminos con ROM real se verifican renderizando
 * niveles con el extractor, fuera de estos tests.
 */
class SmwLevelRenderTest {

    private val RED = 0xFFFF0000.toInt()
    private val BLUE = 0xFF0000FF.toInt()

    /** Atlas de 2 teselas en una fila: tesela 0 = roja opaca, tesela 1 = azul opaca. */
    private fun atlas2(): ArgbImage {
        val img = ArgbImage(32, 16) // 2 columnas × 1 fila de 16×16
        for (y in 0..15) for (x in 0..15) {
            img.set(x, y, RED)        // tesela 0
            img.set(16 + x, y, BLUE)  // tesela 1
        }
        return img
    }

    /** Mapa 2×1: casilla (0,0) primer plano rojo; casilla (1,0) fondo azul, sin primer plano. */
    private fun sampleMap() = SnesGameRecipes.SmwLevelMap(
        atlas = atlas2(),
        columns = 2,
        rows = 1,
        passable = listOf(false, true),
        mapWidth = 2,
        mapHeight = 1,
        tiles = listOf(0, -1),      // Layer 1: rojo en (0,0), aire en (1,0)
        bgTiles = listOf(-1, 1),    // Layer 2: aire en (0,0), azul en (1,0)
    )

    @Test
    fun `separa Layer 1 y Layer 2 con huecos transparentes`() {
        val r = SnesGameRecipes.renderLevelLayers(sampleMap())
        assertEquals(32, r.layer1.width); assertEquals(16, r.layer1.height)

        // Layer 1: solo la casilla (0,0) tiene rojo; la (1,0) es transparente (deja ver el fondo).
        assertEquals(RED, r.layer1.get(4, 8), "primer plano en (0,0)")
        assertEquals(0, r.layer1.get(20, 8), "hueco transparente en (1,0)")

        // Layer 2: existe y solo la casilla (1,0) tiene azul; la (0,0) es transparente.
        assertNotNull(r.layer2)
        assertEquals(BLUE, r.layer2!!.get(20, 8), "fondo en (1,0)")
        assertEquals(0, r.layer2.get(4, 8), "sin fondo en (0,0)")
    }

    @Test
    fun `la escena combinada pone el fondo debajo del primer plano`() {
        val r = SnesGameRecipes.renderLevelLayers(sampleMap())
        assertEquals(RED, r.combined.get(4, 8), "primer plano visible en (0,0)")
        assertEquals(BLUE, r.combined.get(20, 8), "fondo visible por el hueco en (1,0)")
    }

    @Test
    fun `sin fondo, Layer 2 es null y no revienta`() {
        val noBg = SnesGameRecipes.SmwLevelMap(
            atlas = atlas2(), columns = 2, rows = 1, passable = listOf(false, true),
            mapWidth = 2, mapHeight = 1, tiles = listOf(0, 1), bgTiles = emptyList(),
        )
        val r = SnesGameRecipes.renderLevelLayers(noBg)
        assertNull(r.layer2)
        assertEquals(RED, r.combined.get(4, 8))
        assertEquals(BLUE, r.combined.get(20, 8))
    }

    @Test
    fun `cols recorta el ancho de la escena`() {
        val r = SnesGameRecipes.renderLevelLayers(sampleMap(), cols = 1)
        assertEquals(16, r.combined.width, "recortado a 1 columna")
        assertTrue(r.layer2 == null || r.layer2!!.width == 16)
    }
}
