package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tiles
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.MapEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovementTest {

    private fun openMap(): GameMap = GameMap.empty(1, "m", 12, 12, fillTile = Tiles.GRASS)

    @Test
    fun `player moves with joystick input`() {
        val engine = engine(listOf(openMap()))
        val startX = engine.player.x
        engine.inputX = 1f
        engine.run(1f)
        assertTrue(engine.player.x > startX + 2f, "el jugador avanzó hacia la derecha")
        assertEquals(Direction.RIGHT, engine.player.dir)
    }

    @Test
    fun `player cannot walk through water`() {
        var map = openMap()
        for (y in 0 until 12) map = map.withTile(0, 7, y, Tiles.WATER)
        val engine = engine(listOf(map), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.run(2f)
        assertTrue(engine.player.x < 7f, "el agua bloquea el paso (x=${engine.player.x})")
    }

    @Test
    fun `player cannot leave the map`() {
        val engine = engine(listOf(openMap()), startX = 1, startY = 1)
        engine.inputX = -1f
        engine.inputY = -1f
        engine.run(2f)
        assertTrue(engine.player.x > 0f && engine.player.y > 0f)
    }

    @Test
    fun `player slides along walls`() {
        var map = openMap()
        for (x in 0 until 12) map = map.withTile(0, x, 3, Tiles.WALL)
        val engine = engine(listOf(map), startX = 5, startY = 5)
        engine.inputX = 0.7f
        engine.inputY = -0.7f // diagonal contra el muro
        engine.run(1f)
        assertTrue(engine.player.y > 3.5f, "no atraviesa el muro")
        assertTrue(engine.player.x > 6f, "se desliza en horizontal")
    }

    @Test
    fun `impassable event blocks the player`() {
        val npc = MapEvent(id = 1, x = 7, y = 5, pages = listOf(EventPage(passable = false)))
        val engine = engine(listOf(openMap().copy(events = listOf(npc))), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.run(2f)
        assertTrue(engine.player.x < 7f, "el evento bloquea (x=${engine.player.x})")
    }
}
