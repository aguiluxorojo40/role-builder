package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tiles
import com.rolebuilder.core.model.event.Condition
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MapEvent
import com.rolebuilder.core.model.event.MoveStep
import com.rolebuilder.core.model.event.PageConditions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InterpreterTest {

    private fun map(vararg events: MapEvent): GameMap =
        GameMap.empty(1, "m", 12, 12, fillTile = Tiles.GRASS).copy(events = events.toList())

    private fun actionEvent(x: Int, y: Int, vararg commands: EventCommand, id: Int = 1): MapEvent =
        MapEvent(id = id, x = x, y = y, pages = listOf(EventPage(trigger = EventTrigger.ACTION_BUTTON, commands = commands.toList())))

    @Test
    fun `action button runs event and shows message`() {
        val event = actionEvent(6, 5, EventCommand.ShowText("Hola", "NPC"), EventCommand.SetSwitch(1, true))
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f) // fija la dirección hacia la derecha
        engine.inputX = 0f

        engine.pressAction()
        engine.tick(1 / 60f)

        val message = engine.message
        assertNotNull(message)
        assertEquals("Hola", message.text)
        assertEquals("NPC", message.speaker)
        assertFalse(engine.state.switchOn(1), "el switch aún no se ha puesto: mensaje abierto")

        engine.dismissMessage()
        engine.tick(1 / 60f)
        assertNull(engine.message)
        assertTrue(engine.state.switchOn(1))
    }

    @Test
    fun `choices run selected branch`() {
        val event = actionEvent(
            6, 5,
            EventCommand.ShowChoices(
                listOf(
                    EventCommand.ShowChoices.Choice("Sí", listOf(EventCommand.SetVariable(1, EventCommand.SetVariable.Op.SET, 10))),
                    EventCommand.ShowChoices.Choice("No", listOf(EventCommand.SetVariable(1, EventCommand.SetVariable.Op.SET, 20))),
                ),
            ),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.pressAction()
        engine.tick(1 / 60f)

        assertEquals(listOf("Sí", "No"), engine.choices)
        engine.selectChoice(1)
        engine.tick(1 / 60f)
        assertEquals(20, engine.state.variable(1))
    }

    @Test
    fun `conditional branch takes else when condition fails`() {
        val event = actionEvent(
            6, 5,
            EventCommand.ConditionalBranch(
                condition = Condition(Condition.Kind.SWITCH, id = 3),
                then = listOf(EventCommand.SetVariable(1, value = 1)),
                otherwise = listOf(EventCommand.SetVariable(1, value = 2)),
            ),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.pressAction()
        engine.tick(1 / 60f)
        assertEquals(2, engine.state.variable(1))
    }

    @Test
    fun `transfer moves player to another map`() {
        val event = actionEvent(6, 5, EventCommand.TransferPlayer(2, 3, 4))
        val map2 = GameMap.empty(2, "otro", 10, 10, fillTile = Tiles.GRASS)
        val engine = engine(listOf(map(event), map2), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.pressAction()
        engine.tick(1 / 60f)

        assertEquals(2, engine.state.mapId)
        assertEquals(3, engine.player.tileX)
        assertEquals(4, engine.player.tileY)
        assertTrue(engine.mapChanged)
    }

    @Test
    fun `self switch changes active page`() {
        val chest = MapEvent(
            id = 7,
            x = 6,
            y = 5,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.ChangeItems(1, 1),
                        EventCommand.SetSelfSwitch("A", true),
                    ),
                ),
                EventPage(
                    conditions = PageConditions(selfSwitch = "A"),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(EventCommand.SetVariable(9, value = 99)),
                ),
            ),
        )
        val engine = engine(listOf(map(chest)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)

        engine.pressAction() // primera página: da el objeto y activa el self switch
        engine.run(0.2f)
        assertEquals(1, engine.state.itemCount(1))
        assertTrue(engine.state.selfSwitch(1, 7, "A"))

        engine.pressAction() // segunda página
        engine.run(0.2f)
        assertEquals(99, engine.state.variable(9))
        assertEquals(1, engine.state.itemCount(1), "la primera página ya no se ejecuta")
    }

    @Test
    fun `player touch trigger fires when stepping on event`() {
        val trap = MapEvent(
            id = 2,
            x = 7,
            y = 5,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.PLAYER_TOUCH,
                    passable = true,
                    commands = listOf(EventCommand.ChangeHp(-2)),
                ),
            ),
        )
        val engine = engine(listOf(map(trap)), startX = 5, startY = 5)
        val hp0 = engine.state.hp
        engine.inputX = 1f
        engine.run(1.2f)
        assertEquals(hp0 - 2, engine.state.hp)
    }

    @Test
    fun `wait command delays execution`() {
        val event = actionEvent(
            6, 5,
            EventCommand.Wait(0.5f),
            EventCommand.SetSwitch(2, true),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.pressAction()
        engine.run(0.2f)
        assertFalse(engine.state.switchOn(2), "aún esperando")
        engine.run(0.5f)
        assertTrue(engine.state.switchOn(2))
    }

    @Test
    fun `move route moves the event and blocks until done`() {
        val event = actionEvent(
            6, 5,
            EventCommand.MoveRoute(steps = listOf(MoveStep.DOWN, MoveStep.DOWN), waitForCompletion = true),
            EventCommand.SetSwitch(5, true),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.pressAction()
        engine.run(0.1f)
        assertFalse(engine.state.switchOn(5), "la ruta sigue en curso")
        engine.run(2f)
        assertTrue(engine.state.switchOn(5))
        val entity = engine.events.first()
        assertEquals(7, entity.tileY)
    }

    @Test
    fun `erase event removes it until map reload`() {
        val event = actionEvent(6, 5, EventCommand.EraseEvent)
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.pressAction()
        engine.run(0.1f)
        assertNull(engine.eventAtTile(6, 5))
    }

    @Test
    fun `parallel event runs without blocking player`() {
        val counter = MapEvent(
            id = 3,
            x = 1,
            y = 1,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.PARALLEL,
                    commands = listOf(
                        EventCommand.SetVariable(1, EventCommand.SetVariable.Op.ADD, 1),
                        EventCommand.Wait(0.1f),
                    ),
                ),
            ),
        )
        val engine = engine(listOf(map(counter)), startX = 5, startY = 5)
        val startX = engine.player.x
        engine.inputX = 1f
        engine.run(0.55f)
        assertTrue(engine.state.variable(1) >= 4, "el paralelo itera (v=${engine.state.variable(1)})")
        assertTrue(engine.player.x > startX, "el jugador se mueve libremente")
    }
}
