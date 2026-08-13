package com.rolebuilder.core

import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapRegion
import com.rolebuilder.core.model.MapWarp
import com.rolebuilder.core.model.MapWarps
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Las tuberías al mover un trozo de nivel: la boca se va con el terreno y el destino, que
 * son unas coordenadas fijas, hay que reapuntarlo. Sin esto, mover la pantalla de destino
 * deja al jugador saliendo donde antes había algo y ahora hay otra cosa.
 */
class MapWarpsTest {

    private fun mapa(id: Int, warps: List<MapWarp> = emptyList()) = GameMap(
        id = id, name = "m$id", width = 32, height = 27,
        layers = listOf(List(32 * 27) { EMPTY_TILE }, List(32 * 27) { EMPTY_TILE }),
        platformWarps = warps,
    )

    /** Pantalla 1 de un nivel horizontal: columnas 16..31, alto entero. */
    private val pantalla1 = MapRegion.Area(16, 0, 16, 27)

    @Test
    fun `cuenta las bocas del trozo y los que apuntan dentro`() {
        val nivel = mapa(1, listOf(MapWarp(20, 25, 0, 2, 3, 3)))      // boca dentro de la pantalla 1
        val sala = mapa(2, listOf(MapWarp(3, 5, 1, 1, 18, 20)))       // vuelve a la pantalla 1
        val a = MapWarps.afectados(listOf(nivel, sala), 1, pantalla1)
        assertEquals(1, a.bocas)
        assertEquals(1, a.entrantes)
        assertTrue(a.hay)
    }

    @Test
    fun `una pantalla sin tuberias no molesta con preguntas`() {
        val nivel = mapa(1, listOf(MapWarp(2, 25, 0, 2, 3, 3)))       // boca en la pantalla 0
        val a = MapWarps.afectados(listOf(nivel, mapa(2)), 1, pantalla1)
        assertEquals(0, a.total)
        assertTrue(!a.hay)
    }

    @Test
    fun `la boca se va con el trozo`() {
        val nivel = mapa(1, listOf(MapWarp(20, 25, 0, 2, 3, 3)))
        val r = MapWarps.ajustados(listOf(nivel, mapa(2)), 1, pantalla1, dx = -16, dy = 0)
        val w = r.single { it.id == 1 }.platformWarps.single()
        assertEquals(4, w.x, "la boca se corre una pantalla a la izquierda")
        assertEquals(25, w.y)
        assertEquals(3, w.destX, "y su destino, que está en OTRO mapa, no se toca")
    }

    @Test
    fun `el que apuntaba dentro se reapunta`() {
        val nivel = mapa(1)
        val sala = mapa(2, listOf(MapWarp(3, 5, 1, 1, 18, 20)))
        val r = MapWarps.ajustados(listOf(nivel, sala), 1, pantalla1, dx = -16, dy = 0)
        val w = r.single { it.id == 2 }.platformWarps.single()
        assertEquals(2, w.destX, "sale donde ha quedado la pantalla, no donde estaba")
        assertEquals(20, w.destY)
        assertEquals(3, w.x, "su propia boca, en la sala, no se mueve")
    }

    @Test
    fun `un warp del mismo mapa puede mover boca y destino a la vez`() {
        // Boca en la pantalla 1 y destino también: al moverla, se corren las dos cosas.
        val nivel = mapa(1, listOf(MapWarp(20, 25, 0, 1, 22, 4)))
        val r = MapWarps.ajustados(listOf(nivel), 1, pantalla1, dx = -16, dy = 0)
        val w = r.single().platformWarps.single()
        assertEquals(4, w.x)
        assertEquals(6, w.destX)
    }

    @Test
    fun `una boca que se saldria del mapa se descarta`() {
        val nivel = mapa(1, listOf(MapWarp(17, 25, 0, 2, 3, 3)))
        val r = MapWarps.ajustados(listOf(nivel), 1, pantalla1, dx = -20, dy = 0)
        assertTrue(r.single().platformWarps.isEmpty())
    }

    @Test
    fun `un destino que se saldria se recorta en vez de perderse`() {
        // Mejor que suelte un poco desviado a que la sala se quede inalcanzable.
        val sala = mapa(2, listOf(MapWarp(3, 5, 1, 1, 17, 20)))
        val r = MapWarps.ajustados(listOf(mapa(1), sala), 1, pantalla1, dx = 0, dy = 10)
        val w = r.single { it.id == 2 }.platformWarps.single()
        assertEquals(26, w.destY, "20+10 se recorta al alto del mapa, pero el warp sigue vivo")
    }

    @Test
    fun `sin movimiento no cambia nada`() {
        val nivel = mapa(1, listOf(MapWarp(20, 25, 0, 2, 3, 3)))
        assertTrue(MapWarps.ajustados(listOf(nivel), 1, pantalla1, 0, 0).isEmpty())
    }

    @Test
    fun `solo devuelve los mapas que cambian`() {
        val nivel = mapa(1, listOf(MapWarp(20, 25, 0, 2, 3, 3)))
        val sala = mapa(2, listOf(MapWarp(3, 5, 1, 3, 1, 1)))  // apunta a otro mapa: ni se toca
        val r = MapWarps.ajustados(listOf(nivel, sala), 1, pantalla1, dx = -16, dy = 0)
        assertEquals(listOf(1), r.map { it.id })
    }
}
