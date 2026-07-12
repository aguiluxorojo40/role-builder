package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.ProjectPlatformer
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprueba que un mapa del editor se convierte en colisión de plataformas: los
 * tiles no atravesables del tileset se vuelven suelo sólido y el jugador se posa.
 */
class ProjectPlatformerTest {

    // Tileset con 2 tiles: 0 atravesable (aire), 1 no atravesable (suelo).
    private val tileset = Tileset(
        id = 1, name = "t", image = "t.png", tileSize = 16, columns = 2, rows = 1,
        passable = listOf(true, false),
    )

    /** Mapa 4x4 con una fila de suelo (tile 1) abajo del todo en la capa 0. */
    private fun mapWithFloorRow(floorRow: Int): GameMap {
        val w = 4; val h = 4
        val layer0 = MutableList(w * h) { 0 }
        for (c in 0 until w) layer0[floorRow * w + c] = 1
        return GameMap(
            id = 1, name = "m", width = w, height = h, tilesetId = 1,
            layers = listOf(layer0, List(w * h) { EMPTY_TILE }),
        )
    }

    @Test
    fun `tile no atravesable es SOLID y el atravesable es NONE`() {
        val map = mapWithFloorRow(floorRow = 3)
        assertEquals(SmwSolidity.SOLID, ProjectPlatformer.solidityAt(map, tileset, 1, 3))
        assertEquals(SmwSolidity.NONE, ProjectPlatformer.solidityAt(map, tileset, 1, 0))
    }

    @Test
    fun `fuera del mapa no colisiona`() {
        val map = mapWithFloorRow(floorRow = 3)
        assertEquals(SmwSolidity.NONE, ProjectPlatformer.solidityAt(map, tileset, -1, 3))
        assertEquals(SmwSolidity.NONE, ProjectPlatformer.solidityAt(map, tileset, 99, 3))
    }

    @Test
    fun `un solido en la capa superior tambien cuenta`() {
        val w = 4; val h = 4
        val layer0 = List(w * h) { 0 }
        val layer1 = MutableList(w * h) { EMPTY_TILE }
        layer1[1 * w + 2] = 1 // tile sólido en la capa 1
        val map = GameMap(id = 1, name = "m", width = w, height = h, layers = listOf(layer0, layer1))
        assertEquals(SmwSolidity.SOLID, ProjectPlatformer.solidityAt(map, tileset, 2, 1))
    }

    @Test
    fun `platformSolidity del tileset conserva la colision real de la ROM`() {
        // Tileset con solidez REAL por tesela: tile 0 aire, 1 borde de un sentido,
        // 2 sólido, 3 pinchos (ordinales de SmwSolidity).
        val ts = Tileset(
            id = 2, name = "smw", image = "s.png", columns = 2, rows = 2,
            passable = listOf(true, true, true, true), // pasable no debe mandar aquí
            platformSolidity = listOf(
                SmwSolidity.NONE.ordinal, SmwSolidity.LEDGE_TOP.ordinal,
                SmwSolidity.SOLID.ordinal, SmwSolidity.SPIKE.ordinal,
            ),
        )
        val w = 4; val h = 2
        fun mapOf(tile: Int): GameMap {
            val l0 = MutableList(w * h) { EMPTY_TILE }
            l0[0] = tile
            return GameMap(id = 3, name = "m", width = w, height = h, tilesetId = 2, layers = listOf(l0, List(w * h) { EMPTY_TILE }))
        }
        assertEquals(SmwSolidity.LEDGE_TOP, ProjectPlatformer.solidityAt(mapOf(1), ts, 0, 0))
        assertEquals(SmwSolidity.SOLID, ProjectPlatformer.solidityAt(mapOf(2), ts, 0, 0))
        assertEquals(SmwSolidity.SPIKE, ProjectPlatformer.solidityAt(mapOf(3), ts, 0, 0))
        assertEquals(SmwSolidity.NONE, ProjectPlatformer.solidityAt(mapOf(0), ts, 0, 0))
    }

    @Test
    fun `el jugador cae y se posa sobre el suelo del mapa`() {
        val map = mapWithFloorRow(floorRow = 3)
        val e = ProjectPlatformer.engine(map, tileset, startCol = 1, startRow = 0)
        repeat(120) { e.tick() }
        // Se posa encima de la fila 3 (suelo), sin atravesarla ni morir.
        assertTrue(e.player.onGround, "debería estar en el suelo")
        assertTrue(e.player.y < 3 * 16f, "no debe hundirse en el suelo (y=${e.player.y})")
        assertTrue(!e.player.dead, "no debe morir sobre suelo firme")
    }

    // Tileset con comportamiento: tile 2 = moneda (ordinal 1), tile 3 = meta (ordinal 2).
    private val behaviorTileset = Tileset(
        id = 1, name = "t", image = "t.png", tileSize = 16, columns = 4, rows = 1,
        passable = listOf(true, false, true, true),
        platformBehavior = listOf(0, 0, 1, 2), // NONE, NONE, COIN, GOAL
    )

    private fun mapWith(cellTile: Map<Pair<Int, Int>, Int>): GameMap {
        val w = 4; val h = 4
        val l0 = MutableList(w * h) { 0 }
        for ((cr, t) in cellTile) l0[cr.second * w + cr.first] = t
        return GameMap(id = 1, name = "m", width = w, height = h, tilesetId = 1,
            layers = listOf(l0, List(w * h) { EMPTY_TILE }))
    }

    @Test
    fun `moneda se recoge una vez y la meta se alcanza`() {
        val map = mapWith(mapOf((1 to 1) to 2, (2 to 1) to 3)) // moneda en (1,1), meta en (2,1)
        val e = ProjectPlatformer.engine(map, behaviorTileset, startCol = 0, startRow = 0)
        // Coloca al jugador sobre la moneda y avanza: la recoge (contador +1, evento +1).
        e.player.x = 1 * 16f; e.player.y = 1 * 16f
        e.tick()
        assertEquals(1, e.coins)
        assertEquals(1, e.coinEvents)
        // Quieto sobre la moneda otro tick: NO se re-cuenta (celda consumida).
        e.tick()
        assertEquals(1, e.coins)
        // Sobre la meta: se marca alcanzada.
        e.player.x = 2 * 16f; e.player.y = 1 * 16f
        e.tick()
        assertTrue(e.goalReached)
        assertEquals(1, e.goalEvents)
    }
}
