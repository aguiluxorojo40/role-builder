package com.rolebuilder.editor

import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapEdits
import com.rolebuilder.core.model.PlatformLayers
import com.rolebuilder.core.model.Project
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESHACER / REHACER del editor.
 *
 * Es la red de seguridad de todo lo demás —con el relleno por rectángulo, un toque mal dado
 * ya no ensucia tres casillas sino media pantalla— y su comportamiento tiene detalles que se
 * rompen en silencio: que un GESTO entero sea un solo paso, que un gesto que no cambió nada
 * no apile un paso vacío, y que hacer algo nuevo tire lo que había para rehacer.
 *
 * Corre en JVM pura sobre un proyecto de verdad en disco: sin Context, sin emulador.
 */
class EditorUndoTest {

    private val tmp: File = Files.createTempDirectory("editorundo").toFile()

    @After fun cleanup() { tmp.deleteRecursively() }

    /** Proyecto mínimo en disco: un mapa 4×3 de dos capas, vacío. */
    private fun nuevoEstado(): EditorState {
        ProjectIo.saveProject(tmp, Project(name = "test", startMapId = 1, mapIds = listOf(1)))
        ProjectIo.saveDatabase(tmp, Database())
        ProjectIo.saveMap(
            tmp,
            GameMap(
                id = 1, name = "nivel", width = 4, height = 3,
                layers = listOf(List(12) { EMPTY_TILE }, List(12) { EMPTY_TILE }),
            ),
        )
        return EditorState(tmp)
    }

    private fun fg(state: EditorState) = state.currentMap!!.layers[PlatformLayers.FOREGROUND]

    @Test
    fun `un gesto entero es un solo paso de deshacer`() {
        val state = nuevoEstado()
        assertFalse("recién abierto no hay nada que deshacer", state.canUndo)

        // Un trazo: tres celdas pintadas dentro del MISMO gesto.
        state.beginEdit()
        repeat(3) { i ->
            state.updateMap(state.currentMap!!.withTile(PlatformLayers.FOREGROUND, i, 0, 7))
        }
        state.commitEdit()

        assertTrue(state.canUndo)
        assertEquals(listOf(7, 7, 7), fg(state).take(3))

        // Un solo deshacer se lleva el trazo COMPLETO, no la última celda.
        assertTrue(state.undo())
        assertEquals(List(12) { EMPTY_TILE }, fg(state))
        assertFalse("y ya no queda nada más que deshacer", state.canUndo)
    }

    @Test
    fun `rehacer devuelve lo deshecho`() {
        val state = nuevoEstado()
        state.beginEdit()
        state.updateMap(MapEdits.fillRect(state.currentMap!!, PlatformLayers.FOREGROUND, 0, 0, 3, 2, 5))
        state.commitEdit()

        val relleno = fg(state)
        assertTrue(state.undo())
        assertTrue(state.canRedo)
        assertTrue(state.redo())
        assertEquals(relleno, fg(state))
        assertFalse(state.canRedo)
    }

    @Test
    fun `un gesto que no cambia nada no apila un paso`() {
        val state = nuevoEstado()
        state.beginEdit()
        // Rellenar con lo que ya hay: MapEdits devuelve el MISMO mapa.
        state.updateMap(MapEdits.fillRect(state.currentMap!!, PlatformLayers.FOREGROUND, 0, 0, 3, 2, EMPTY_TILE))
        state.commitEdit()
        assertFalse("tocar sin cambiar nada no es un paso", state.canUndo)
    }

    @Test
    fun `dibujar despues de deshacer tira lo que habia para rehacer`() {
        val state = nuevoEstado()
        state.beginEdit()
        state.updateMap(state.currentMap!!.withTile(PlatformLayers.FOREGROUND, 0, 0, 1))
        state.commitEdit()
        state.undo()
        assertTrue(state.canRedo)

        state.beginEdit()
        state.updateMap(state.currentMap!!.withTile(PlatformLayers.FOREGROUND, 1, 1, 2))
        state.commitEdit()

        assertFalse("la rama vieja de rehacer ya no vale", state.canRedo)
        assertEquals(2, fg(state)[1 * 4 + 1])
    }

    @Test
    fun `deshacer tambien recupera el punto de inicio`() {
        val state = nuevoEstado()
        val antes = state.project.startX
        state.beginEdit()
        state.updateProject(state.project.copy(startX = antes + 3))
        state.commitEdit()
        assertEquals(antes + 3, state.project.startX)

        assertTrue(state.undo())
        assertEquals(antes, state.project.startX)
    }

    @Test
    fun `deshacer sin nada que deshacer no revienta`() {
        val state = nuevoEstado()
        assertFalse(state.undo())
        assertFalse(state.redo())
    }
}
