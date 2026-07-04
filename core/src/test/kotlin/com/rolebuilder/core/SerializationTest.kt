package com.rolebuilder.core

import com.rolebuilder.core.engine.GameState
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.DefaultProjectFactory
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.ParallaxLayer
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.Tiles
import com.rolebuilder.core.model.Weather
import com.rolebuilder.core.model.event.EventCommand
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
            EventCommand.ChangeGold(25),
            EventCommand.ChangeExp(8),
            EventCommand.OpenShop(itemIds = listOf(3, 4, 1)),
            EventCommand.PlayMusic("mazmorra"),
            EventCommand.StopMusic,
            EventCommand.TintScreen(1f, 0f, 0f, 0.5f, seconds = 2f),
            EventCommand.FlashScreen(seconds = 0.4f),
            EventCommand.SetWeather(Weather.RAIN),
            EventCommand.ShakeScreen(0.7f),
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
            DefaultProjectFactory.maps().forEach { ProjectIo.saveMap(dir, it) }

            val loaded = ProjectIo.loadFull(dir)
            assertEquals("Disco", loaded.project.name)
            assertEquals(2, loaded.maps.size)
            assertEquals(4, loaded.maps.getValue(1).events.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `map round trip with parallax layers`() {
        val map = GameMap.empty(1, "m", 4, 4).copy(
            parallaxLayers = listOf(
                ParallaxLayer("clouds.png", factor = 0.35f, autoX = 0.25f, above = true, alpha = 0.5f),
                ParallaxLayer("sky.png", factor = 0f),
            ),
        )
        val restored = json.decodeFromString(GameMap.serializer(), json.encodeToString(GameMap.serializer(), map))
        assertEquals(map, restored)
    }

    @Test
    fun `game state round trip with progression fields`() {
        val state = GameState(
            hp = 12,
            maxHp = 24,
            gold = 55,
            level = 3,
            exp = 6,
            weaponItemId = 3,
            armorItemId = 4,
        )
        val restored = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), state))
        assertEquals(state, restored)
    }

    @Test
    fun `game state round trip with presentation fields`() {
        val state = GameState(
            weather = Weather.SNOW,
            tintR = 0.1f,
            tintG = 0.2f,
            tintB = 0.3f,
            tintA = 0.4f,
        )
        val restored = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), state))
        assertEquals(state, restored)
    }

    @Test
    fun `map round trip with bgm and weather`() {
        val map = GameMap.empty(1, "m", 4, 4).copy(bgm = "campo", weather = Weather.RAIN)
        val restored = json.decodeFromString(GameMap.serializer(), json.encodeToString(GameMap.serializer(), map))
        assertEquals(map, restored)
    }

    @Test
    fun `legacy map without presentation fields loads with defaults`() {
        val legacy = """{"id":1,"name":"m","width":1,"height":1,"layers":[[0],[-1]]}"""
        val map = json.decodeFromString(GameMap.serializer(), legacy)
        assertNull(map.bgm)
        assertEquals(Weather.NONE, map.weather)
        assertEquals(emptyList(), map.parallaxLayers)
    }

    @Test
    fun `legacy save without progression fields loads with defaults`() {
        // Partida guardada antes de la Fase 5: sin gold/level/exp/equipo.
        val legacy = """
            {"switches":{},"variables":{},"selfSwitches":{},"items":{"1":2},
             "hp":12,"maxHp":20,"mapId":1,"x":5.5,"y":8.5,"dir":"DOWN","playTimeSeconds":3.0}
        """.trimIndent()
        val state = json.decodeFromString(GameState.serializer(), legacy)
        assertEquals(0, state.gold)
        assertEquals(1, state.level)
        assertEquals(0, state.exp)
        assertNull(state.weaponItemId)
        assertNull(state.armorItemId)
        assertEquals(12, state.hp)
        assertEquals(2, state.itemCount(1))
        // Campos de la Fase 6 tampoco están: cargan con sus defaults.
        assertEquals(Weather.NONE, state.weather)
        assertEquals(0f, state.tintA)
    }

    @Test
    fun `legacy database without progression fields loads with defaults`() {
        // Base de datos guardada antes de la Fase 5: sin recompensas ni equipo.
        val legacy = """{"actors":[{"id":1}],"enemies":[{"id":1}],"items":[{"id":1}]}"""
        val db = json.decodeFromString(Database.serializer(), legacy)
        val actor = db.actor(1)!!
        assertEquals(4, actor.hpPerLevel)
        assertEquals(1, actor.attackPerLevel)
        assertEquals(1, actor.defensePerLevel)
        assertEquals(8, actor.expBase)
        assertEquals(20, actor.maxLevel)
        val enemy = db.enemy(1)!!
        assertEquals(3, enemy.expReward)
        assertEquals(2, enemy.goldReward)
        val item = db.item(1)!!
        assertEquals(0, item.price)
        assertNull(item.equipSlot)
        assertEquals(0, item.attackBonus)
        assertEquals(0, item.defenseBonus)
        assertEquals(0, item.maxHpBonus)
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
    fun `project round trip with diorama tilt`() {
        val project = DefaultProjectFactory.defaultProject("Test").copy(dioramaTilt = 18.5f)
        val restored = json.decodeFromString(Project.serializer(), json.encodeToString(Project.serializer(), project))
        assertEquals(project, restored)
        assertEquals(18.5f, restored.dioramaTilt)
    }

    @Test
    fun `legacy project without diorama tilt loads with default`() {
        val legacy = """{"name":"Vieja aventura"}"""
        val project = json.decodeFromString(Project.serializer(), legacy)
        assertEquals(45f, project.dioramaTilt)
        assertEquals(1f, project.spriteStand)
    }

    @Test
    fun `project round trip with sprite stand`() {
        val project = DefaultProjectFactory.defaultProject("Test").copy(spriteStand = 1.8f)
        val restored = json.decodeFromString(Project.serializer(), json.encodeToString(Project.serializer(), project))
        assertEquals(project, restored)
        assertEquals(1.8f, restored.spriteStand)
    }

    @Test
    fun `tileset round trip with standing tiles`() {
        val ts = DefaultProjectFactory.defaultTileset().copy(standingTiles = listOf(5, 6, 7))
        val restored = json.decodeFromString(
            com.rolebuilder.core.model.Tileset.serializer(),
            json.encodeToString(com.rolebuilder.core.model.Tileset.serializer(), ts),
        )
        assertEquals(ts, restored)
        assertEquals(listOf(5, 6, 7), restored.standingTiles)
    }

    @Test
    fun `legacy tileset without standing tiles loads with default`() {
        val legacy = """{"id":1,"name":"Campo","image":"tileset.png"}"""
        val ts = json.decodeFromString(com.rolebuilder.core.model.Tileset.serializer(), legacy)
        assertEquals(emptyList(), ts.standingTiles)
    }

    @Test
    fun `default tileset marks expected standing tiles`() {
        val ts = DefaultProjectFactory.defaultTileset()
        assertEquals(
            listOf(
                Tiles.TREE, Tiles.TREE_TOP, Tiles.BUSH, Tiles.ROCK,
                Tiles.WALL, Tiles.ROOF, Tiles.DOOR_CLOSED, Tiles.DOOR_OPEN,
            ),
            ts.standingTiles,
        )
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
