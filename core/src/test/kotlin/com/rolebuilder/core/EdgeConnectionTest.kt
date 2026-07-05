package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Conexión de zonas por los bordes del mapa. */
class EdgeConnectionTest {

    private fun field(id: Int, east: Int? = null, west: Int? = null): GameMap =
        GameMap.empty(id, "zona$id", 12, 10, fillTile = Tiles.GRASS)
            .copy(edgeEast = east, edgeWest = west)

    @Test
    fun `walking off the east edge enters the neighbour from the west`() {
        val a = field(1, east = 2)
        val b = field(2, west = 1)
        val engine = engine(listOf(a, b), startX = 10, startY = 4)

        // Empuja al este hasta que el motor detecte el cruce de zona.
        engine.inputX = 1f
        var guard = 0
        while (engine.state.mapId == 1 && guard++ < 600) engine.tick(1f / 60f)

        assertEquals(2, engine.state.mapId, "cruzó a la zona vecina")
        assertTrue(engine.player.tileX <= 1, "aparece pegado al borde oeste (x=${engine.player.tileX})")
        assertEquals(4, engine.player.tileY, "conserva su fila")
    }

    @Test
    fun `a closed edge blocks the player`() {
        val a = field(1, east = null)
        val engine = engine(listOf(a), startX = 10, startY = 4)
        engine.inputX = 1f
        engine.run(2f)
        assertEquals(1, engine.state.mapId, "sin vecino, no cruza")
        assertTrue(engine.player.x < a.width.toFloat(), "queda dentro del mapa")
    }

    @Test
    fun `crossing back and forth does not loop instantly`() {
        val a = field(1, east = 2)
        val b = field(2, west = 1)
        val engine = engine(listOf(a, b), startX = 10, startY = 4)

        engine.inputX = 1f
        engine.run(3f)
        assertEquals(2, engine.state.mapId)
        // Sigue empujando al oeste: vuelve a la zona 1, sin quedar rebotando.
        engine.inputX = -1f
        engine.run(3f)
        assertEquals(1, engine.state.mapId, "vuelve a la zona de origen")
    }
}
