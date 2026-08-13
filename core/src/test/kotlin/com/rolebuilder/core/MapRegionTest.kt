package com.rolebuilder.core

import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapRegion
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.PlatformItemMark
import com.rolebuilder.core.model.PlatformItemType
import com.rolebuilder.core.model.PlatformLayers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test

/**
 * El marco de selección: copiar, cortar, pegar, duplicar y voltear un trozo de nivel.
 *
 * Lo que de verdad hace falta comprobar aquí es el ALCANCE POR CAPAS: recortar un fondo sin
 * llevarse por delante el suelo que tiene delante es justo lo que se pide para construir
 * fondos, y es lo que se rompería sin darse cuenta.
 */
class MapRegionTest {

    private val bg = PlatformLayers.BACKGROUND
    private val fg = PlatformLayers.FOREGROUND

    /** Mapa 4×3: fondo todo a 8, primer plano con una fila de suelo abajo (tesela 1). */
    private fun map(): GameMap {
        val fondo = List(12) { 8 }
        val suelo = MutableList(12) { EMPTY_TILE }
        for (x in 0 until 4) suelo[2 * 4 + x] = 1
        return GameMap(
            id = 1, name = "n", width = 4, height = 3, tilesetId = 3,
            layers = listOf(fondo, suelo),
            platformEnemies = listOf(PlatformEnemyMark(0x0F, 1, 1)),
            platformItems = listOf(PlatformItemMark(PlatformItemType.COIN, 2, 1)),
        )
    }

    @Test
    fun `copiar solo el fondo deja el primer plano vacio en el portapapeles`() {
        val clip = assertNotNull(MapRegion.copy(map(), MapRegion.Area(0, 0, 2, 2), listOf(bg), withObjects = false))
        assertEquals(2, clip.width)
        assertEquals(2, clip.height)
        assertEquals(3, clip.tilesetId)
        assertEquals(List(4) { 8 }, clip.layers[bg])
        assertTrue(clip.layers[fg].all { it == EMPTY_TILE }, "el primer plano no viaja")
        assertTrue(clip.enemies.isEmpty())
    }

    @Test
    fun `pegar un trozo de fondo no toca el terreno`() {
        val m = map()
        val clip = assertNotNull(MapRegion.copy(m, MapRegion.Area(0, 0, 4, 1), listOf(bg), withObjects = false))
        // Se pega justo encima de la fila de suelo: el suelo tiene que seguir ahí.
        val r = MapRegion.paste(m, clip, 0, 2)
        assertEquals(List(4) { 1 }, r.layers[fg].subList(8, 12), "el suelo sigue intacto")
        assertEquals(List(4) { 8 }, r.layers[bg].subList(8, 12), "y el fondo se pegó")
    }

    @Test
    fun `pegar respeta los huecos del trozo`() {
        // Primer plano: fila 0 con teselas sueltas y fila 2 con el suelo; en medio, aire.
        val conTecho = map().let { base ->
            val capa = base.layers[fg].toMutableList()
            for (x in 0 until 4) capa[x] = 5
            base.copy(layers = base.layers.mapIndexed { i, l -> if (i == fg) capa else l })
        }
        // Se copian las filas 1 y 2 (aire + suelo) y se pegan arriba del todo: la fila de
        // teselas sueltas tiene que DESAPARECER, porque el trozo trae aire ahí.
        val clip = assertNotNull(
            MapRegion.copy(conTecho, MapRegion.Area(0, 1, 4, 2), listOf(fg), withObjects = false),
        )
        val r = MapRegion.paste(conTecho, clip, 0, 0)
        assertTrue(r.layers[fg].subList(0, 4).all { it == EMPTY_TILE }, "el hueco se pega también")
        assertEquals(List(4) { 1 }, r.layers[fg].subList(4, 8), "y el suelo cae una fila más arriba")
    }

    @Test
    fun `un trozo con la capa entera vacia no pisa esa capa`() {
        // Es la regla que hace útil el alcance por capas: copiar un rectángulo de cielo
        // (fondo vacío) y pegarlo no puede borrar el terreno que haya delante. Para vaciar
        // está "Borrar".
        val m = map()
        val cielo = assertNotNull(
            MapRegion.copy(m, MapRegion.Area(0, 0, 4, 2), listOf(fg), withObjects = false),
        )
        assertTrue(cielo.layers[fg].all { it == EMPTY_TILE }, "el trozo copiado es todo aire")
        assertSame(m, MapRegion.paste(m, cielo, 0, 1), "pegarlo no cambia nada")
    }

    @Test
    fun `un trozo sabe que teselas necesita`() {
        val clip = assertNotNull(
            MapRegion.copy(map(), MapRegion.Area(0, 0, 4, 3), listOf(bg, fg), withObjects = false),
        )
        // Fondo todo a 8 y una fila de suelo a 1: dos teselas distintas, sin contar el vacío.
        assertEquals(listOf(1, 8), MapRegion.usedTiles(clip))
    }

    @Test
    fun `traducir un trozo a otro tileset reescribe sus indices`() {
        val clip = assertNotNull(
            MapRegion.copy(map(), MapRegion.Area(0, 0, 4, 3), listOf(bg, fg), withObjects = false),
        )
        // Las teselas 8 y 1 del origen han caído en la 40 y la 41 del nivel de destino.
        val r = MapRegion.remapped(clip, mapOf(8 to 40, 1 to 41), tilesetId = 9)

        assertEquals(9, r.tilesetId)
        assertTrue(r.layers[bg].all { it == 40 }, "el fondo apunta a su tesela nueva")
        assertEquals(List(4) { 41 }, r.layers[fg].subList(8, 12), "y el suelo a la suya")
        assertTrue(r.layers[fg].subList(0, 8).all { it == EMPTY_TILE }, "el vacío sigue vacío")
    }

    @Test
    fun `una tesela sin traduccion se queda vacia en vez de mentir`() {
        val clip = assertNotNull(
            MapRegion.copy(map(), MapRegion.Area(0, 0, 4, 3), listOf(bg, fg), withObjects = false),
        )
        // Solo se pudo traer el fondo: el suelo no tiene equivalente y NO puede quedarse
        // apuntando al índice viejo, que en el destino es otra cosa.
        val r = MapRegion.remapped(clip, mapOf(8 to 40), tilesetId = 9)
        assertTrue(r.layers[fg].all { it == EMPTY_TILE })
        assertTrue(r.layers[bg].all { it == 40 })
    }

    // ---- Selección por trozos (pantallas de SMW) ----

    @Test
    fun `tocar una pantalla selecciona la pantalla entera`() {
        // Nivel de 4 pantallas: 64 columnas, 27 filas. Un toque suelto dentro de la 2ª.
        val r = MapRegion.snapped(
            MapRegion.Area(20, 5, 1, 1), MapRegion.SCREEN_COLS, 27, 64, 27,
        )
        assertEquals(16, r.x)
        assertEquals(0, r.y)
        assertEquals(16, r.w)
        assertEquals(27, r.h, "la pantalla es la columna entera")
    }

    @Test
    fun `un arrastre a caballo de dos pantallas coge las dos`() {
        val r = MapRegion.snapped(
            MapRegion.Area(14, 3, 6, 4), MapRegion.SCREEN_COLS, 27, 64, 27,
        )
        assertEquals(0, r.x)
        assertEquals(32, r.w, "de la pantalla 0 a la 1, enteras")
    }

    @Test
    fun `el ajuste se recorta al mapa aunque el nivel no sea multiplo`() {
        // 40 columnas = dos pantallas y media: la última se queda corta, no se sale.
        val r = MapRegion.snapped(
            MapRegion.Area(38, 0, 1, 1), MapRegion.SCREEN_COLS, 27, 40, 27,
        )
        assertEquals(32, r.x)
        assertEquals(8, r.w)
    }

    @Test
    fun `trozos cuadrados para los niveles verticales`() {
        val r = MapRegion.snapped(MapRegion.Area(3, 20, 2, 2), 16, 16, 16, 64)
        assertEquals(0, r.x)
        assertEquals(16, r.y)
        assertEquals(16, r.w)
        assertEquals(16, r.h)
    }

    @Test
    fun `un trozo de 1 deja la seleccion libre tal cual`() {
        val libre = MapRegion.Area(3, 4, 5, 6)
        val r = MapRegion.snapped(libre, 1, 1, 64, 27)
        assertEquals(libre.x, r.x)
        assertEquals(libre.y, r.y)
        assertEquals(libre.w, r.w)
        assertEquals(libre.h, r.h)
    }

    @Test
    fun `una region fuera del mapa no da portapapeles`() {
        assertNull(MapRegion.copy(map(), MapRegion.Area(9, 9, 2, 2), listOf(fg), withObjects = false))
    }

    @Test
    fun `cortar el fondo vacia solo el fondo`() {
        val m = map()
        val r = MapRegion.clear(m, MapRegion.Area(0, 0, 4, 1), listOf(bg), withObjects = false)
        assertTrue(r.layers[bg].subList(0, 4).all { it == EMPTY_TILE })
        assertEquals(m.layers[fg], r.layers[fg], "el primer plano no se toca")
        assertEquals(m.platformEnemies, r.platformEnemies, "ni los enemigos")
    }

    @Test
    fun `los objetos viajan solo cuando se piden`() {
        val m = map()
        val conObjetos = assertNotNull(MapRegion.copy(m, MapRegion.Area(0, 0, 4, 3), listOf(fg), withObjects = true))
        assertEquals(1, conObjetos.enemies.size)
        assertEquals(1, conObjetos.items.size)

        val cortado = MapRegion.clear(m, MapRegion.Area(0, 0, 4, 3), listOf(fg), withObjects = true)
        assertTrue(cortado.platformEnemies.isEmpty())
        assertTrue(cortado.platformItems.isEmpty())
    }

    @Test
    fun `pegar encima de un enemigo no deja dos en la misma casilla`() {
        val m = map()
        val clip = assertNotNull(MapRegion.copy(m, MapRegion.Area(1, 1, 1, 1), listOf(fg), withObjects = true))
        assertEquals(1, clip.enemies.size)
        val r = MapRegion.paste(m, clip, 1, 1) // pegado sobre sí mismo
        assertEquals(1, r.platformEnemies.count { it.x == 1 && it.y == 1 })
    }

    @Test
    fun `duplicar al lado no pisa el original`() {
        val m = map()
        val clip = assertNotNull(MapRegion.copy(m, MapRegion.Area(0, 0, 2, 2), listOf(bg), withObjects = false))
        val r = MapRegion.paste(m, clip, 2, 0) // a la derecha, ancho del trozo
        assertEquals(List(4) { 8 }, listOf(r.layers[bg][0], r.layers[bg][1], r.layers[bg][2], r.layers[bg][3]))
    }

    @Test
    fun `voltear en horizontal espeja celdas y objetos`() {
        val m = map()
        val clip = assertNotNull(MapRegion.copy(m, MapRegion.Area(0, 1, 4, 1), listOf(fg), withObjects = true))
        // Fila del medio: primer plano vacío, con el enemigo en x=1 y la moneda en x=2.
        val v = MapRegion.flippedH(clip)
        assertEquals(4 - 1 - 1, v.enemies.single().x)
        assertEquals(4 - 1 - 2, v.items.single().x)
    }

    @Test
    fun `voltear en vertical espeja las filas`() {
        val m = map()
        val clip = assertNotNull(MapRegion.copy(m, MapRegion.Area(0, 0, 4, 3), listOf(fg), withObjects = false))
        val v = MapRegion.flippedV(clip)
        // El suelo estaba en la fila de abajo; volteado, arriba.
        assertEquals(List(4) { 1 }, v.layers[fg].subList(0, 4))
    }

    @Test
    fun `borrar algo que ya esta vacio devuelve el mismo mapa`() {
        val m = map()
        val vacio = MapRegion.clear(m, MapRegion.Area(0, 0, 4, 2), listOf(fg), withObjects = false)
        assertSame(vacio, MapRegion.clear(vacio, MapRegion.Area(0, 0, 4, 2), listOf(fg), withObjects = false))
    }

    @Test
    fun `pegar recorta en el borde del mapa`() {
        val m = map()
        val clip = assertNotNull(MapRegion.copy(m, MapRegion.Area(0, 0, 4, 3), listOf(bg), withObjects = false))
        val r = MapRegion.paste(m, clip, 3, 2) // casi fuera: solo entra una celda
        assertEquals(m.width * m.height, r.layers[bg].size, "no se sale de la rejilla")
        assertEquals(8, r.layers[bg][2 * 4 + 3])
    }
}
