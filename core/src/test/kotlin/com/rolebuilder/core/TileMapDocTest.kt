package com.rolebuilder.core

import com.rolebuilder.core.model.TileMapDoc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileMapDocTest {

    private fun sample() = TileMapDoc.of(
        mapWidth = 3, mapHeight = 2, atlasCols = 8,
        layers = listOf(
            "layer2" to listOf(10, 11, 12, 13, 14, 15),
            "layer1" to listOf(-1, 5, -1, 5, -1, 5),
        ),
    )

    @Test
    fun `round-trip serializar y parsear preserva capas y dimensiones`() {
        val doc = sample()
        val back = TileMapDoc.parse(doc.serialize())
        assertEquals(doc.mapWidth, back.mapWidth)
        assertEquals(doc.mapHeight, back.mapHeight)
        assertEquals(doc.atlasCols, back.atlasCols)
        assertEquals(doc.layers.map { it.name }, back.layers.map { it.name }) // orden de dibujo
        assertTrue(doc.layer("layer1")!!.tiles.contentEquals(back.layer("layer1")!!.tiles))
        assertTrue(doc.layer("layer2")!!.tiles.contentEquals(back.layer("layer2")!!.tiles))
    }

    @Test
    fun `parsea el formato que emite el extractor (comentarios y cabecera)`() {
        val text = """
            # Nivel 0x105  mapa=3x2  atlas_cols=8
            # Rejilla de índices al atlas, -1 = casilla vacía.
            [layer1]
            -1,5,-1
            5,-1,5
        """.trimIndent()
        val doc = TileMapDoc.parse(text)
        assertEquals(3, doc.mapWidth); assertEquals(2, doc.mapHeight); assertEquals(8, doc.atlasCols)
        assertEquals(1, doc.layers.size)
        assertEquals(5, doc.layer("layer1")!!.tiles[1])
        assertNull(doc.layer("layer2"))
    }

    @Test
    fun `filas incompletas se rellenan con vacio`() {
        val text = "# mapa=4x1  atlas_cols=4\n[layer1]\n7,8\n"
        val t = TileMapDoc.parse(text).layer("layer1")!!.tiles
        assertEquals(listOf(7, 8, -1, -1), t.toList())
    }

    @Test
    fun `sin cabecera de dimensiones falla`() {
        assertFailsWith<IllegalArgumentException> { TileMapDoc.parse("[layer1]\n1,2,3\n") }
    }

    @Test
    fun `valor no entero falla`() {
        assertFailsWith<IllegalArgumentException> { TileMapDoc.parse("# mapa=2x1 atlas_cols=2\n[l]\n1,x\n") }
    }
}
