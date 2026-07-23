package com.rolebuilder.core

import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapStamp
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.PlatformItemMark
import com.rolebuilder.core.model.PlatformItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapStampTest {

    /** Mapa 4×3 de 2 capas; capa 0 = índice = celda, capa 1 = vacío, salvo lo indicado. */
    private fun grid(w: Int, h: Int, fg: (Int, Int) -> Int, bg: (Int, Int) -> Int = { _, _ -> EMPTY_TILE }): GameMap =
        GameMap(
            id = 1, name = "m", width = w, height = h, tilesetId = 7,
            layers = listOf(
                List(w * h) { fg(it % w, it / w) },
                List(w * h) { bg(it % w, it / w) },
            ),
        )

    @Test
    fun `fromRegion copia capas y normaliza coordenadas de enemigos e items`() {
        val map = grid(5, 4, fg = { x, y -> y * 5 + x }).copy(
            platformEnemies = listOf(PlatformEnemyMark(0xA0, 2, 1), PlatformEnemyMark(0x0F, 0, 0)),
            platformItems = listOf(PlatformItemMark(PlatformItemType.COIN, 3, 2)),
        )
        val s = MapStamp.fromRegion(map, x0 = 1, y0 = 1, w = 3, h = 2, name = "trozo")!!
        assertEquals(3, s.width); assertEquals(2, s.height); assertEquals(7, s.tilesetId)
        // La celda (1,1) del mapa (valor 1*5+1=6) queda en (0,0) del sello.
        assertEquals(6, s.layers[0][0])
        assertEquals(8, s.layers[0][2]) // (3,1) -> 8, en (2,0) del sello
        // El enemigo de (2,1) entra y queda relativo en (1,0); el de (0,0) queda fuera.
        assertEquals(1, s.enemies.size)
        assertEquals(1, s.enemies[0].x); assertEquals(0, s.enemies[0].y); assertEquals(0xA0, s.enemies[0].spriteId)
        assertEquals(1, s.items.size); assertEquals(2, s.items[0].x); assertEquals(1, s.items[0].y)
    }

    @Test
    fun `pasteInto escribe las celdas no vacias y respeta las vacias`() {
        // Sello 2×1: capa0 = [5, EMPTY]. Al pegarlo, la primera celda sobrescribe y la vacía NO.
        val stamp = MapStamp("s", 2, 1, 7, layers = listOf(listOf(5, EMPTY_TILE), listOf(EMPTY_TILE, EMPTY_TILE)))
        val map = grid(4, 1, fg = { _, _ -> 99 })
        val out = stamp.pasteInto(map, x0 = 1, y0 = 0)
        assertEquals(99, out.layers[0][0])  // fuera del sello, intacto
        assertEquals(5, out.layers[0][1])   // celda no vacía del sello, sobrescrita
        assertEquals(99, out.layers[0][2])  // celda VACÍA del sello: no borra
        assertEquals(99, out.layers[0][3])
    }

    @Test
    fun `pasteInto con overwriteEmpty borra debajo`() {
        val stamp = MapStamp("s", 2, 1, 7, layers = listOf(listOf(5, EMPTY_TILE), listOf(EMPTY_TILE, EMPTY_TILE)))
        val map = grid(2, 1, fg = { _, _ -> 99 })
        val out = stamp.pasteInto(map, 0, 0, overwriteEmpty = true)
        assertEquals(5, out.layers[0][0])
        assertEquals(EMPTY_TILE, out.layers[0][1]) // la celda vacía SÍ borra
    }

    @Test
    fun `pasteInto recorta a los limites y desplaza enemigos`() {
        val stamp = MapStamp(
            "s", 2, 2, 7,
            layers = listOf(listOf(1, 2, 3, 4), listOf(EMPTY_TILE, EMPTY_TILE, EMPTY_TILE, EMPTY_TILE)),
            enemies = listOf(PlatformEnemyMark(0xA0, 1, 1)),
        )
        val map = grid(3, 3, fg = { _, _ -> 0 })
        // Pegado en (2,2): solo la celda (0,0) del sello cae dentro (en 2,2); el resto se recorta.
        val out = stamp.pasteInto(map, 2, 2)
        assertEquals(1, out.layers[0][2 * 3 + 2])
        // El enemigo del sello en (1,1) iría a (3,3): fuera -> descartado.
        assertTrue(out.platformEnemies.isEmpty())
    }

    @Test
    fun `fromRegion recortada a vacio devuelve null`() {
        val map = grid(3, 3, fg = { _, _ -> 0 })
        assertNull(MapStamp.fromRegion(map, x0 = 5, y0 = 5, w = 2, h = 2, name = "x"))
    }

    @Test
    fun `round-trip extraer y volver a pegar reproduce la region`() {
        val map = grid(4, 4, fg = { x, y -> y * 4 + x })
        val s = MapStamp.fromRegion(map, 1, 1, 2, 2, "r")!!
        val blank = grid(2, 2, fg = { _, _ -> EMPTY_TILE })
        val pasted = s.pasteInto(blank, 0, 0)
        assertEquals(listOf(5, 6, 9, 10), pasted.layers[0])
    }
}
