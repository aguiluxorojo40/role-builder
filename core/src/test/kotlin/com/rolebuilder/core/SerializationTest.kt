package com.rolebuilder.core

import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.DefaultProjectFactory
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.Tiles
import com.rolebuilder.core.model.event.EventCommand
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SerializationTest {

    private val json = ProjectIo.json

    @Test
    fun `project round trip`() {
        val project = DefaultProjectFactory.defaultProject("Test")
        val restored = json.decodeFromString(Project.serializer(), json.encodeToString(Project.serializer(), project))
        assertEquals(project, restored)
    }

    @Test
    fun `database round trip`() {
        val db = DefaultProjectFactory.defaultDatabase()
        val restored = json.decodeFromString(Database.serializer(), json.encodeToString(Database.serializer(), db))
        assertEquals(db, restored)
    }

    @Test
    fun `map with events round trip`() {
        val map = DefaultProjectFactory.starterMap()
        val restored = json.decodeFromString(GameMap.serializer(), json.encodeToString(GameMap.serializer(), map))
        assertEquals(map, restored)
    }

    @Test
    fun `event commands polymorphic round trip`() {
        val commands: List<EventCommand> = listOf(
            EventCommand.ShowText("hola", "NPC"),
            EventCommand.ShowChoices(
                listOf(
                    EventCommand.ShowChoices.Choice("Sí", listOf(EventCommand.SetSwitch(1, true))),
                    EventCommand.ShowChoices.Choice("No"),
                ),
            ),
            EventCommand.TransferPlayer(2, 3, 4),
            EventCommand.Wait(1.5f),
            EventCommand.EraseEvent,
        )
        val encoded = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(EventCommand.serializer()),
            commands,
        )
        val restored = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(EventCommand.serializer()),
            encoded,
        )
        assertEquals(commands, restored)
        assertTrue(encoded.contains("\"type\""), "usa discriminador legible")
    }

    @Test
    fun `save and load full project from disk`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "rb-test-${System.nanoTime()}")
        try {
            ProjectIo.saveProject(dir, DefaultProjectFactory.defaultProject("Disco"))
            ProjectIo.saveDatabase(dir, DefaultProjectFactory.defaultDatabase())
            ProjectIo.saveMap(dir, DefaultProjectFactory.starterMap())

            val loaded = ProjectIo.loadFull(dir)
            assertEquals("Disco", loaded.project.name)
            assertEquals(1, loaded.maps.size)
            assertEquals(2, loaded.maps.getValue(1).events.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `map editing helpers`() {
        var map = GameMap.empty(1, "m", 10, 8)
        map = map.withTile(0, 3, 2, Tiles.WATER)
        assertEquals(Tiles.WATER, map.tileAt(0, 3, 2))
        assertEquals(Tiles.GRASS, map.tileAt(0, 0, 0))

        val bigger = map.resized(12, 10)
        assertEquals(Tiles.WATER, bigger.tileAt(0, 3, 2))
        assertEquals(12, bigger.width)

        val smaller = map.resized(4, 4)
        assertFalse(smaller.inBounds(5, 5))
    }

    @Test
    fun `tileset passability defaults`() {
        val ts = DefaultProjectFactory.defaultTileset()
        assertTrue(ts.isPassable(Tiles.GRASS))
        assertFalse(ts.isPassable(Tiles.WATER))
        assertFalse(ts.isPassable(Tiles.WALL))
        assertTrue(ts.isPassable(com.rolebuilder.core.model.EMPTY_TILE))
    }
}
