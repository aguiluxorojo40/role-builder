package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.engine.GameState
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.SaveIo
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MusicTracks
import com.rolebuilder.core.model.Tiles
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MapEvent
import com.rolebuilder.core.model.event.PageConditions
import com.rolebuilder.core.model.event.SpriteRef
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas de música de fondo, cambio de sprite, variable aleatoria y marca de guardado. */
class NewCommandsTest {

    private fun map(vararg events: MapEvent, bgm: String? = null): GameMap =
        GameMap.empty(1, "m", 12, 12, fillTile = Tiles.GRASS).copy(events = events.toList(), bgm = bgm)

    private fun actionEvent(x: Int, y: Int, vararg commands: EventCommand, id: Int = 1): MapEvent =
        MapEvent(id = id, x = x, y = y, pages = listOf(EventPage(trigger = EventTrigger.ACTION_BUTTON, commands = commands.toList())))

    /** Mira a la derecha y activa el evento adyacente. */
    private fun com.rolebuilder.core.engine.RpgEngine.activateEventToTheRight() {
        inputX = 1f
        tick(1 / 60f)
        inputX = 0f
        pressAction()
        run(0.1f)
    }

    // ---- música --------------------------------------------------------------

    @Test
    fun `play music changes current bgm`() {
        val event = actionEvent(6, 5, EventCommand.PlayMusic(MusicTracks.BATTLE))
        val engine = engine(listOf(map(event, bgm = MusicTracks.FIELD)), startX = 5, startY = 5)
        assertEquals(MusicTracks.FIELD, engine.currentBgm, "al cargar el mapa suena su bgm")

        engine.activateEventToTheRight()
        assertEquals(MusicTracks.BATTLE, engine.currentBgm)
    }

    @Test
    fun `stop music silences current bgm`() {
        val event = actionEvent(6, 5, EventCommand.StopMusic)
        val engine = engine(listOf(map(event, bgm = MusicTracks.FIELD)), startX = 5, startY = 5)
        engine.activateEventToTheRight()
        assertNull(engine.currentBgm)
    }

    @Test
    fun `transfer takes the bgm of the destination map`() {
        val event = actionEvent(6, 5, EventCommand.TransferPlayer(2, 3, 4))
        val map2 = GameMap.empty(2, "otro", 10, 10, fillTile = Tiles.GRASS).copy(bgm = MusicTracks.DUNGEON)
        val engine = engine(listOf(map(event, bgm = MusicTracks.FIELD), map2), startX = 5, startY = 5)
        engine.activateEventToTheRight()
        assertEquals(2, engine.state.mapId)
        assertEquals(MusicTracks.DUNGEON, engine.currentBgm)
    }

    // ---- cambio de sprite ----------------------------------------------------

    @Test
    fun `change event sprite overrides the page sprite`() {
        val npc = MapEvent(
            id = 1,
            x = 6,
            y = 5,
            pages = listOf(
                EventPage(
                    sprite = SpriteRef("npc.png", Direction.DOWN),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(EventCommand.ChangeEventSprite(image = "hero.png", direction = Direction.LEFT)),
                ),
            ),
        )
        val engine = engine(listOf(map(npc)), startX = 5, startY = 5)
        engine.activateEventToTheRight()
        assertEquals(SpriteRef("hero.png", Direction.LEFT), engine.events.first().currentSprite)
    }

    @Test
    fun `change event sprite with null image makes the event invisible`() {
        val npc = MapEvent(
            id = 1,
            x = 6,
            y = 5,
            pages = listOf(
                EventPage(
                    sprite = SpriteRef("npc.png", Direction.DOWN),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(EventCommand.ChangeEventSprite(image = null)),
                ),
            ),
        )
        val engine = engine(listOf(map(npc)), startX = 5, startY = 5)
        engine.activateEventToTheRight()
        val entity = engine.events.first()
        assertNotNull(entity.page?.sprite, "la página sigue teniendo sprite propio")
        assertNull(entity.currentSprite, "el override lo hace invisible")
    }

    @Test
    fun `sprite override is cleared when active page changes`() {
        val npc = MapEvent(
            id = 1,
            x = 6,
            y = 5,
            pages = listOf(
                EventPage(
                    sprite = SpriteRef("npc.png", Direction.DOWN),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.ChangeEventSprite(image = null),
                        EventCommand.SetSelfSwitch("A", true),
                    ),
                ),
                EventPage(
                    conditions = PageConditions(selfSwitch = "A"),
                    sprite = SpriteRef("chest.png", Direction.UP),
                    trigger = EventTrigger.ACTION_BUTTON,
                ),
            ),
        )
        val engine = engine(listOf(map(npc)), startX = 5, startY = 5)
        engine.activateEventToTheRight()
        engine.run(0.2f) // la nueva página se activa y limpia el override
        assertEquals(SpriteRef("chest.png", Direction.UP), engine.events.first().currentSprite)
    }

    // ---- variable aleatoria ----------------------------------------------------

    @Test
    fun `set variable random stays within range`() {
        val event = actionEvent(6, 5, EventCommand.SetVariableRandom(variableId = 1, min = 5, max = 9))
        val engine = engine(listOf(map(event)), startX = 5, startY = 5, seed = 123)
        engine.activateEventToTheRight()
        val value = engine.state.variable(1)
        assertTrue(value in 5..9, "valor fuera de rango: $value")
    }

    @Test
    fun `set variable random with inverted range uses min`() {
        val event = actionEvent(6, 5, EventCommand.SetVariableRandom(variableId = 2, min = 10, max = 3))
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.activateEventToTheRight()
        assertEquals(10, engine.state.variable(2))
    }

    // ---- marca de guardado -----------------------------------------------------

    @Test
    fun `save stamps savedAtEpochMs and it survives the round trip`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "rb-save-${System.nanoTime()}")
        try {
            val file = File(dir, "save1.json")
            val state = GameState(hp = 10, maxHp = 10)
            assertEquals(0, state.savedAtEpochMs)

            SaveIo.save(file, state)
            assertTrue(state.savedAtEpochMs > 0, "save() actualiza la marca de tiempo")

            val loaded = SaveIo.load(file)
            assertEquals(state.savedAtEpochMs, loaded.savedAtEpochMs)
        } finally {
            dir.deleteRecursively()
        }
    }

    // ---- serialización -----------------------------------------------------------

    @Test
    fun `new commands polymorphic round trip`() {
        val commands: List<EventCommand> = listOf(
            EventCommand.PlayMusic(MusicTracks.VILLAGE),
            EventCommand.StopMusic,
            EventCommand.ChangeEventSprite(image = "hero.png", direction = Direction.LEFT),
            EventCommand.ChangeEventSprite(image = null),
            EventCommand.SetVariableRandom(variableId = 3, min = 1, max = 6),
        )
        val serializer = kotlinx.serialization.builtins.ListSerializer(EventCommand.serializer())
        val encoded = ProjectIo.json.encodeToString(serializer, commands)
        val restored = ProjectIo.json.decodeFromString(serializer, encoded)
        assertEquals(commands, restored)
        assertTrue(encoded.contains("\"playMusic\""), "usa los nombres cortos del discriminador")
    }
}
