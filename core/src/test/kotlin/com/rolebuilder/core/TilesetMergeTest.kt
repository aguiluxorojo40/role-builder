package com.rolebuilder.core

import com.rolebuilder.core.model.TileAnimation
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.model.TilesetMerge
import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Traerse teselas de OTRO nivel al nivel que se está construyendo. Es lo que hacía falta
 * para poder montar un nivel propio con los gráficos de varios niveles de la ROM: cada
 * nivel importado trae su tileset y un mapa apunta a uno solo, así que la única forma
 * coherente es copiar la tesela —píxeles Y colisión— al tileset del nivel actual.
 */
class TilesetMergeTest {

    /** Tesela de color plano [c] en la celda [tile] de un atlas de [columns] columnas. */
    private fun paint(pixels: IntArray, columns: Int, size: Int, tile: Int, c: Int) {
        val atlasW = columns * size
        val ox = (tile % columns) * size
        val oy = (tile / columns) * size
        for (y in 0 until size) for (x in 0 until size) pixels[(oy + y) * atlasW + ox + x] = c
    }

    private fun atlas(columns: Int, rows: Int, size: Int) = IntArray(columns * size * rows * size)

    /** Destino: 2×1 teselas de 4px, la 0 aire y la 1 sólida. */
    private fun dest(): Pair<Tileset, IntArray> {
        val ts = Tileset(
            id = 1, name = "destino", image = "d.png", tileSize = 4, columns = 2, rows = 1,
            passable = listOf(true, false),
            platformSolidity = listOf(SmwSolidity.NONE.ordinal, SmwSolidity.SOLID.ordinal),
            platformSlopeShape = listOf(-1, -1),
            platformBlockActions = listOf(0, 0),
        )
        val px = atlas(2, 1, 4)
        paint(px, 2, 4, 0, 0x11111111)
        paint(px, 2, 4, 1, 0x22222222)
        return ts to px
    }

    /** Origen: 2×1 teselas de 4px, la 0 una cuesta y la 1 una moneda animada con la 2. */
    private fun src(): Pair<Tileset, IntArray> {
        val ts = Tileset(
            id = 2, name = "otro nivel", image = "s.png", tileSize = 4, columns = 2, rows = 2,
            passable = listOf(false, true, true, true),
            platformSolidity = listOf(
                SmwSolidity.SLOPE.ordinal, SmwSolidity.NONE.ordinal,
                SmwSolidity.NONE.ordinal, SmwSolidity.NONE.ordinal,
            ),
            platformSlopeShape = listOf(7, -1, -1, -1),
            platformBlockActions = listOf(0, SmwBlockAction.COIN.ordinal, SmwBlockAction.COIN.ordinal, 0),
            animations = listOf(TileAnimation(baseTile = 1, frames = listOf(1, 2), periodFrames = 8)),
        )
        val px = atlas(2, 2, 4)
        paint(px, 2, 4, 0, 0x33333333)
        paint(px, 2, 4, 1, 0x44444444)
        paint(px, 2, 4, 2, 0x55555555)
        return ts to px
    }

    @Test
    fun `una tesela traida conserva sus pixeles y su colision`() {
        val (d, dp) = dest()
        val (s, sp) = src()
        val r = TilesetMerge.append(d, dp, s, sp, listOf(0)) // la cuesta

        val nueva = r.added.single()
        assertEquals(2, nueva) // se añade detrás de las dos que ya había
        assertEquals(SmwSolidity.SLOPE.ordinal, r.tileset.platformSolidity[nueva])
        assertEquals(7, r.tileset.platformSlopeShape[nueva])
        assertEquals(false, r.tileset.passable[nueva])

        // Los píxeles de la celda nueva son los del origen.
        val ox = (nueva % r.tileset.columns) * 4
        val oy = (nueva / r.tileset.columns) * 4
        assertEquals(0x33333333, r.pixels[oy * r.width + ox])
    }

    @Test
    fun `el atlas crece por filas y lo que ya estaba no se mueve`() {
        val (d, dp) = dest()
        val (s, sp) = src()
        val r = TilesetMerge.append(d, dp, s, sp, listOf(0))

        assertEquals(2, r.tileset.columns) // las columnas no cambian
        assertEquals(2, r.tileset.rows)    // una fila más
        assertEquals(2 * 4, r.width)
        assertEquals(2 * 4, r.height)
        assertEquals(0x11111111, r.pixels[0])            // tesela 0 del destino, intacta
        assertEquals(0x22222222, r.pixels[4])            // tesela 1 del destino, intacta
        assertEquals(r.tileset.tileCount, r.tileset.passable.size)
        assertEquals(r.tileset.tileCount, r.tileset.platformSolidity.size)
    }

    @Test
    fun `traerse la misma tesela dos veces no duplica nada`() {
        val (d, dp) = dest()
        val (s, sp) = src()
        val una = TilesetMerge.append(d, dp, s, sp, listOf(0))
        val otra = TilesetMerge.append(una.tileset, una.pixels, s, sp, listOf(0))

        assertEquals(una.tileset.rows, otra.tileset.rows)
        assertEquals(una.added, otra.added) // reutiliza la que ya estaba
    }

    @Test
    fun `una moneda animada se trae con sus fotogramas`() {
        val (d, dp) = dest()
        val (s, sp) = src()
        val r = TilesetMerge.append(d, dp, s, sp, listOf(1)) // la moneda

        val base = r.added.single()
        val anim = r.tileset.animations.single { it.baseTile == base }
        assertEquals(2, anim.frames.size)
        assertTrue(anim.frames.all { it in 0 until r.tileset.tileCount })
        // El segundo fotograma también se copió de verdad (no apunta al vacío).
        val f = anim.frames[1]
        val ox = (f % r.tileset.columns) * 4
        val oy = (f / r.tileset.columns) * 4
        assertEquals(0x55555555, r.pixels[oy * r.width + ox])
        assertEquals(SmwBlockAction.COIN.ordinal, r.tileset.platformBlockActions[base])
    }

    @Test
    fun `varias teselas de golpe salen en el orden pedido`() {
        val (d, dp) = dest()
        val (s, sp) = src()
        val r = TilesetMerge.append(d, dp, s, sp, listOf(1, 0))
        assertEquals(2, r.added.size)
        assertEquals(SmwBlockAction.COIN.ordinal, r.tileset.platformBlockActions[r.added[0]])
        assertEquals(SmwSolidity.SLOPE.ordinal, r.tileset.platformSolidity[r.added[1]])
    }

    @Test
    fun `un tileset sin datos de colision se completa al recibir una tesela que si los trae`() {
        val plano = Tileset(
            id = 3, name = "a mano", image = "m.png", tileSize = 4, columns = 2, rows = 1,
            passable = listOf(true, false),
        )
        val (s, sp) = src()
        val r = TilesetMerge.append(plano, atlas(2, 1, 4), s, sp, listOf(0))

        assertEquals(r.tileset.tileCount, r.tileset.platformSolidity.size)
        // La que era intransitable pasa a sólida; la traída conserva su cuesta.
        assertContentEquals(
            listOf(SmwSolidity.NONE.ordinal, SmwSolidity.SOLID.ordinal, SmwSolidity.SLOPE.ordinal, SmwSolidity.NONE.ordinal),
            r.tileset.platformSolidity,
        )
    }

    @Test
    fun `mezclar rejillas de distinto tamano se niega en vez de cortar teselas`() {
        val (d, dp) = dest()
        val ocho = Tileset(id = 4, name = "8px", image = "o.png", tileSize = 8, columns = 1, rows = 1)
        assertFailsWith<IllegalArgumentException> {
            TilesetMerge.append(d, dp, ocho, atlas(1, 1, 8), listOf(0))
        }
    }
}
