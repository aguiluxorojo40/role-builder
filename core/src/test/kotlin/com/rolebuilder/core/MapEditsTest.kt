package com.rolebuilder.core

import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapEdits
import com.rolebuilder.core.model.PlatformLayers
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test

/**
 * Relleno por rectángulo y cubo: las dos operaciones que convierten "un suelo de 128
 * columnas" en un gesto en vez de en cientos de toques.
 */
class MapEditsTest {

    /** Mapa 4×3 de dos capas, todo vacío. */
    private fun map() = GameMap(
        id = 1, name = "n", width = 4, height = 3,
        layers = listOf(List(12) { EMPTY_TILE }, List(12) { EMPTY_TILE }),
    )

    private fun fg(m: GameMap) = m.layers[PlatformLayers.FOREGROUND]
    private fun bg(m: GameMap) = m.layers[PlatformLayers.BACKGROUND]

    @Test
    fun `el rectangulo rellena solo su zona y solo su capa`() {
        val r = MapEdits.fillRect(map(), PlatformLayers.FOREGROUND, 1, 0, 2, 1, 7)
        assertEquals(
            listOf(
                EMPTY_TILE, 7, 7, EMPTY_TILE,
                EMPTY_TILE, 7, 7, EMPTY_TILE,
                EMPTY_TILE, EMPTY_TILE, EMPTY_TILE, EMPTY_TILE,
            ),
            fg(r),
        )
        assertEquals(List(12) { EMPTY_TILE }, bg(r), "la otra capa no se toca")
    }

    @Test
    fun `las esquinas valen en cualquier orden`() {
        val a = MapEdits.fillRect(map(), PlatformLayers.FOREGROUND, 0, 0, 2, 1, 3)
        val b = MapEdits.fillRect(map(), PlatformLayers.FOREGROUND, 2, 1, 0, 0, 3)
        assertEquals(fg(a), fg(b))
    }

    @Test
    fun `un rectangulo que se sale se recorta al mapa`() {
        val r = MapEdits.fillRect(map(), PlatformLayers.FOREGROUND, -5, -5, 99, 99, 2)
        assertEquals(List(12) { 2 }, fg(r), "queda el mapa entero, sin reventar")
    }

    @Test
    fun `un rectangulo entero fuera del mapa no hace nada`() {
        val m = map()
        assertSame(m, MapEdits.fillRect(m, PlatformLayers.FOREGROUND, 10, 10, 20, 20, 5))
    }

    @Test
    fun `rellenar con lo que ya hay devuelve el mismo mapa`() {
        val m = MapEdits.fillRect(map(), PlatformLayers.FOREGROUND, 0, 0, 3, 2, 4)
        // Idéntica operación otra vez: nada que cambiar, así que ni un paso de deshacer.
        assertSame(m, MapEdits.fillRect(m, PlatformLayers.FOREGROUND, 0, 0, 3, 2, 4))
    }

    @Test
    fun `una capa que no existe no rompe nada`() {
        val m = map()
        assertSame(m, MapEdits.fillRect(m, 5, 0, 0, 1, 1, 3))
        assertSame(m, MapEdits.floodFill(m, 5, 0, 0, 3))
    }

    @Test
    fun `el cubo rellena la zona contigua y respeta la pared`() {
        // Columna 1 hecha pared: el cubo desde la izquierda no debe pasarla.
        val conPared = MapEdits.fillRect(map(), PlatformLayers.FOREGROUND, 1, 0, 1, 2, 9)
        val r = MapEdits.floodFill(conPared, PlatformLayers.FOREGROUND, 0, 0, 5)
        assertEquals(
            listOf(
                5, 9, EMPTY_TILE, EMPTY_TILE,
                5, 9, EMPTY_TILE, EMPTY_TILE,
                5, 9, EMPTY_TILE, EMPTY_TILE,
            ),
            fg(r),
        )
    }

    @Test
    fun `el cubo no se cuela en diagonal`() {
        // Escalón: (1,0) y (0,1) son pared; desde (0,0) no se llega a (1,1).
        var m = MapEdits.fillRect(map(), PlatformLayers.FOREGROUND, 1, 0, 1, 0, 9)
        m = MapEdits.fillRect(m, PlatformLayers.FOREGROUND, 0, 1, 0, 1, 9)
        val r = MapEdits.floodFill(m, PlatformLayers.FOREGROUND, 0, 0, 5)
        assertEquals(5, fg(r)[0], "la celda de origen sí")
        assertEquals(EMPTY_TILE, fg(r)[1 * 4 + 1], "la diagonal no")
    }

    @Test
    fun `el cubo sobre su propia tesela no hace nada`() {
        val m = map()
        assertSame(m, MapEdits.floodFill(m, PlatformLayers.FOREGROUND, 0, 0, EMPTY_TILE))
    }

    @Test
    fun `el cubo llena un mapa grande sin desbordar la pila`() {
        // 320x27 es el tamaño de los niveles largos de SMW: con recursión, esto revienta.
        val grande = GameMap(
            id = 2, name = "largo", width = 320, height = 27,
            layers = listOf(List(320 * 27) { EMPTY_TILE }, List(320 * 27) { EMPTY_TILE }),
        )
        val r = MapEdits.floodFill(grande, PlatformLayers.FOREGROUND, 0, 0, 1)
        assertEquals(320 * 27, r.layers[PlatformLayers.FOREGROUND].count { it == 1 })
    }
}
