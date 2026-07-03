package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tiles
import com.rolebuilder.core.model.Weather
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MapEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PresentationTest {

    private fun map(vararg events: MapEvent): GameMap =
        GameMap.empty(1, "m", 12, 12, fillTile = Tiles.GRASS).copy(events = events.toList())

    private fun actionEvent(x: Int, y: Int, vararg commands: EventCommand, id: Int = 1): MapEvent =
        MapEvent(id = id, x = x, y = y, pages = listOf(EventPage(trigger = EventTrigger.ACTION_BUTTON, commands = commands.toList())))

    /** Lanza el evento de acción situado a la derecha del jugador. */
    private fun com.rolebuilder.core.engine.RpgEngine.triggerEvent() {
        inputX = 1f
        tick(1 / 60f)
        inputX = 0f
        pressAction()
        tick(1 / 60f)
    }

    @Test
    fun `play and stop music assign currentBgm without blocking`() {
        val event = actionEvent(
            6, 5,
            EventCommand.PlayMusic("tension"),
            EventCommand.SetSwitch(1, true),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.triggerEvent()
        assertEquals("tension", engine.state.currentBgm)
        assertTrue(engine.state.switchOn(1), "PlayMusic no bloquea: el siguiente comando corre en el mismo tick")

        val stop = actionEvent(6, 5, EventCommand.StopMusic, EventCommand.SetSwitch(2, true), id = 1)
        val engine2 = engine(listOf(map(stop).copy(bgm = "pueblo")), startX = 5, startY = 5)
        assertEquals("pueblo", engine2.state.currentBgm)
        engine2.triggerEvent()
        assertEquals("", engine2.state.currentBgm)
        assertTrue(engine2.state.switchOn(2), "StopMusic no bloquea")
    }

    @Test
    fun `set weather assigns state without blocking`() {
        val event = actionEvent(
            6, 5,
            EventCommand.SetWeather(Weather.SNOW),
            EventCommand.SetSwitch(1, true),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.triggerEvent()
        assertEquals(Weather.SNOW, engine.state.weather)
        assertTrue(engine.state.switchOn(1), "SetWeather no bloquea")
    }

    @Test
    fun `tint screen transitions linearly and persists the target`() {
        val event = actionEvent(
            6, 5,
            EventCommand.TintScreen(r = 1f, g = 0f, b = 0.5f, a = 0.8f, seconds = 1f),
            EventCommand.SetSwitch(1, true),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.triggerEvent()
        assertTrue(engine.state.switchOn(1), "TintScreen no bloquea")

        // El destino se persiste de inmediato; el valor visible aún no.
        assertEquals(1f, engine.state.tintR)
        assertEquals(0.8f, engine.state.tintA)
        assertTrue(engine.tintCurrent[0] < 0.1f, "la transición acaba de empezar")

        engine.tick(0.5f)
        assertEquals(0.5f, engine.tintCurrent[0], 0.02f, "valor intermedio a mitad de camino")
        assertEquals(0.25f, engine.tintCurrent[2], 0.02f)
        assertEquals(0.4f, engine.tintCurrent[3], 0.02f)

        engine.tick(0.6f) // sobrepasa el final: se clava exactamente en el destino
        assertEquals(1f, engine.tintCurrent[0])
        assertEquals(0f, engine.tintCurrent[1])
        assertEquals(0.5f, engine.tintCurrent[2])
        assertEquals(0.8f, engine.tintCurrent[3])
    }

    @Test
    fun `tint with zero seconds applies immediately`() {
        val event = actionEvent(6, 5, EventCommand.TintScreen(0.2f, 0.4f, 0.6f, 1f, seconds = 0f))
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.triggerEvent()
        assertEquals(0.2f, engine.tintCurrent[0])
        assertEquals(1f, engine.tintCurrent[3])
    }

    @Test
    fun `flash screen decays linearly to zero`() {
        val event = actionEvent(6, 5, EventCommand.FlashScreen(r = 1f, g = 0.5f, b = 0f, seconds = 0.5f))
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.triggerEvent()
        assertEquals(1f, engine.flashIntensity)
        assertEquals(0.5f, engine.flashColor[1])

        engine.tick(0.25f)
        assertEquals(0.5f, engine.flashIntensity, 0.02f)
        engine.tick(1f)
        assertEquals(0f, engine.flashIntensity)
    }

    @Test
    fun `shake screen counts down to zero`() {
        val event = actionEvent(6, 5, EventCommand.ShakeScreen(seconds = 0.5f))
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.triggerEvent()
        assertEquals(0.5f, engine.shakeTimeLeft, 0.001f)
        engine.tick(0.2f)
        assertEquals(0.3f, engine.shakeTimeLeft, 0.001f)
        engine.tick(1f)
        assertEquals(0f, engine.shakeTimeLeft)
    }

    @Test
    fun `effects advance while a message blocks the ui`() {
        val event = actionEvent(
            6, 5,
            EventCommand.FlashScreen(seconds = 0.5f),
            EventCommand.TintScreen(1f, 1f, 1f, 1f, seconds = 1f),
            EventCommand.ShakeScreen(seconds = 0.5f),
            EventCommand.ShowText("¡Boom!"),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        engine.triggerEvent()
        assertNotNull(engine.message, "el mensaje está abierto")
        assertTrue(engine.uiBlocked)
        assertEquals(1f, engine.flashIntensity)

        engine.tick(0.25f) // con el mensaje aún abierto
        assertNotNull(engine.message)
        assertEquals(0.5f, engine.flashIntensity, 0.02f, "el destello decae con la UI bloqueada")
        assertEquals(0.25f, engine.tintCurrent[0], 0.02f, "el tinte avanza con la UI bloqueada")
        assertEquals(0.25f, engine.shakeTimeLeft, 0.02f, "el temblor decae con la UI bloqueada")
    }

    @Test
    fun `map imposes bgm and weather on load and transfer`() {
        val map1 = map(
            actionEvent(
                6, 5,
                EventCommand.PlayMusic("tension"),
                EventCommand.SetWeather(Weather.SNOW),
            ),
        ).copy(bgm = "campo", weather = Weather.RAIN)
        val map2 = GameMap.empty(2, "cueva", 10, 10, fillTile = Tiles.GRASS)
            .copy(bgm = "mazmorra", weather = Weather.NONE)
        val engine = engine(listOf(map1, map2), startX = 5, startY = 5)

        // Carga inicial: el mapa impone su presentación.
        assertEquals("campo", engine.state.currentBgm)
        assertEquals(Weather.RAIN, engine.state.weather)

        // Los comandos la sobreescriben...
        engine.triggerEvent()
        assertEquals("tension", engine.state.currentBgm)
        assertEquals(Weather.SNOW, engine.state.weather)

        // ...hasta el siguiente cambio de mapa.
        engine.transferPlayer(2, 3, 3, null)
        assertEquals("mazmorra", engine.state.currentBgm)
        assertEquals(Weather.NONE, engine.state.weather)
    }

    @Test
    fun `engine starts with tint already at the saved target`() {
        val plainMap = map()
        val data = EngineTestSupport.project(listOf(plainMap))
        val state = com.rolebuilder.core.engine.GameState.newGame(data.project, data.database).apply {
            tintR = 0.1f
            tintG = 0.2f
            tintB = 0.3f
            tintA = 0.4f
        }
        val engine = com.rolebuilder.core.engine.RpgEngine(data, state)
        assertEquals(0.1f, engine.tintCurrent[0])
        assertEquals(0.4f, engine.tintCurrent[3])
        engine.run(0.2f)
        assertEquals(0.1f, engine.tintCurrent[0], 0.001f, "sin transición pendiente al cargar")
    }

    @Test
    fun `set diorama tilt transitions and persists in state`() {
        val event = actionEvent(
            6, 5,
            EventCommand.SetDioramaTilt(degrees = 22f, seconds = 1f),
            EventCommand.SetSwitch(1, true),
        )
        val engine = engine(listOf(map(event)), startX = 5, startY = 5)
        assertEquals(12f, engine.tiltCurrent, "arranca con la inclinación del proyecto")

        engine.triggerEvent()
        assertTrue(engine.state.switchOn(1), "SetDioramaTilt no bloquea")
        assertEquals(22f, engine.state.dioramaTilt)
        assertEquals(22f, engine.tiltTarget)

        engine.tick(0.5f)
        assertEquals(17f, engine.tiltCurrent, 0.5f, "valor intermedio a mitad de camino")

        engine.tick(0.6f) // sobrepasa el final: se clava en el destino
        assertEquals(22f, engine.tiltCurrent)
    }

    @Test
    fun `map tilt override applies on transfer and clears the command tilt`() {
        val exterior = map(
            actionEvent(
                6, 5,
                EventCommand.SetDioramaTilt(degrees = 20f, seconds = 0f),
                EventCommand.TransferPlayer(mapId = 2, x = 3, y = 3),
            ),
        )
        val interior = GameMap.empty(2, "interior", 12, 12, fillTile = Tiles.GRASS)
            .copy(dioramaTilt = 0f)
        val engine = engine(listOf(exterior, interior), startX = 5, startY = 5)

        engine.triggerEvent()
        engine.run(0.2f)
        assertEquals(2, engine.state.mapId, "la transferencia se completó")
        assertEquals(null, engine.state.dioramaTilt, "el cambio de mapa limpia el comando")
        assertEquals(0f, engine.tiltTarget, "rige la inclinación propia del mapa")
        assertEquals(0f, engine.tiltCurrent, "sin transición al cargar mapa")
    }
}
