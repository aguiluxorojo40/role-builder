package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.engine.RpgEngine
import com.rolebuilder.core.io.LoadedProject
import com.rolebuilder.core.model.DefaultProjectFactory
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Pruebas de integración sobre el proyecto plantilla que incluye la app. */
class DemoProjectTest {

    private fun demoEngine(startX: Int, startY: Int, mapId: Int = 1): RpgEngine {
        val project = DefaultProjectFactory.defaultProject("Demo")
            .copy(startMapId = mapId, startX = startX, startY = startY)
        val data = LoadedProject(
            dir = File("."),
            project = project,
            database = DefaultProjectFactory.defaultDatabase(),
            maps = DefaultProjectFactory.maps().associateBy { it.id },
        )
        return RpgEngine(data, random = Random(7))
    }

    @Test
    fun `walking into the cabin door transfers to map 2`() {
        val engine = demoEngine(startX = 15, startY = 12) // frente a la puerta
        engine.inputY = -1f
        engine.run(1.5f)
        assertEquals(2, engine.state.mapId, "el jugador entró en la cabaña")
    }

    @Test
    fun `stepping on the cabin exit returns to map 1`() {
        val engine = demoEngine(startX = 4, startY = 6, mapId = 2) // sobre la salida
        engine.inputY = 1f
        engine.run(1.5f)
        assertEquals(1, engine.state.mapId, "el jugador volvió a la pradera")
    }

    @Test
    fun `opening the meadow chest gives a potion once`() {
        val engine = demoEngine(startX = 4, startY = 12) // junto al cofre (3, 12)
        engine.inputX = -1f
        engine.tick(1 / 60f) // mirar a la izquierda
        engine.inputX = 0f

        engine.pressAction()
        engine.run(0.2f)
        // Cerrar el mensaje "¡Has encontrado una Poción!".
        assertNotNull(engine.message)
        engine.dismissMessage()
        engine.run(0.2f)

        assertEquals(1, engine.state.itemCount(1))
        assertTrue(engine.state.selfSwitch(1, 2, "A"), "el cofre quedó abierto")

        // Segunda interacción: página del cofre vacío, sin más objetos.
        engine.pressAction()
        engine.run(0.2f)
        engine.dismissMessage()
        engine.run(0.2f)
        assertEquals(1, engine.state.itemCount(1), "no vuelve a dar poción")
    }

    @Test
    fun `npc conversation shows both lines`() {
        val engine = demoEngine(startX = 8, startY = 7) // debajo del aldeano (8, 6)
        engine.inputY = -1f
        engine.tick(1 / 60f)
        engine.inputY = 0f

        engine.pressAction()
        engine.run(0.1f)
        val first = engine.message
        assertNotNull(first)
        assertTrue(first.text.contains("Bienvenido"))

        engine.dismissMessage()
        engine.run(0.1f)
        val second = engine.message
        assertNotNull(second)
        assertTrue(second.text.contains("editor"))
        engine.dismissMessage()
        engine.run(0.1f)
        assertEquals(null, engine.message)
    }

    @Test
    fun `template data is internally consistent`() {
        val database = DefaultProjectFactory.defaultDatabase()
        val project = DefaultProjectFactory.defaultProject("Demo")
        val maps = DefaultProjectFactory.maps()

        assertEquals(maps.map { it.id }, project.mapIds)
        assertNotNull(database.actor(project.playerActorId))
        for (map in maps) {
            assertNotNull(database.tileset(map.tilesetId))
            for (spawn in map.spawns) {
                assertNotNull(database.enemy(spawn.enemyId), "enemigo ${spawn.enemyId} existe")
                assertTrue(map.inBounds(spawn.x, spawn.y))
            }
            for (event in map.events) {
                assertTrue(map.inBounds(event.x, event.y))
                assertTrue(event.pages.isNotEmpty())
            }
        }
    }
}
