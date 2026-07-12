package com.rolebuilder.core.engine.platformer

import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwSolidity

/**
 * Puente entre un proyecto del editor (Platform Builder) y el motor de plataformas:
 * convierte un [GameMap] + su [Tileset] en la rejilla de colisión que consume
 * [PlatformerEngine], sin depender de ninguna ROM.
 *
 * La solidez sale de la MISMA marca que ya usa el editor y el RPG:
 * [Tileset.isPassable]. Una celda es [SmwSolidity.SOLID] si en CUALQUIERA de sus
 * capas hay un tile no atravesable; si todas las capas son atravesables (o vacías),
 * es [SmwSolidity.NONE] (aire). Así, un mapa dibujado para el ARPG se puede jugar
 * como plataformas marcando el suelo como "no caminable".
 */
object ProjectPlatformer {

    /** El motor de plataformas asume una rejilla de 16 px por celda. */
    const val TILE = 16

    private val SOLIDITY_VALUES = SmwSolidity.values()

    /**
     * Solidez de la celda (col,row) del mapa. Si el tileset trae la colisión REAL por
     * casilla ([Tileset.platformSolidity], p. ej. de un nivel SMW extraído), la usa tal
     * cual —bordes de un sentido, cuestas y pinchos incluidos—; si no, cae en la
     * pasabilidad ([Tileset.isPassable]: no atravesable → sólido). Gana la capa más
     * bloqueante de la celda.
     */
    fun solidityAt(map: GameMap, tileset: Tileset, col: Int, row: Int): SmwSolidity {
        if (!map.inBounds(col, row)) return SmwSolidity.NONE
        var result = SmwSolidity.NONE
        for (layer in map.layers.indices) {
            val tile = map.tileAt(layer, col, row)
            if (tile == EMPTY_TILE) continue
            val s = tileSolidity(tileset, tile)
            if (s != SmwSolidity.NONE && result == SmwSolidity.NONE) result = s
            if (s == SmwSolidity.SPIKE) return SmwSolidity.SPIKE // los pinchos mandan
        }
        return result
    }

    private fun tileSolidity(tileset: Tileset, tile: Int): SmwSolidity {
        val ord = tileset.platformSolidity.getOrNull(tile)
        if (ord != null) return SOLIDITY_VALUES.getOrElse(ord) { SmwSolidity.NONE }
        return if (tileset.isPassable(tile)) SmwSolidity.NONE else SmwSolidity.SOLID
    }

    /**
     * Monta un [PlatformerEngine] jugable sobre [map]. El inicio (en celdas) se
     * convierte a píxeles con la rejilla de 16; se coloca al jugador un poco por
     * encima del suelo para que caiga a su sitio al empezar.
     */
    fun engine(
        map: GameMap,
        tileset: Tileset,
        startCol: Int,
        startRow: Int,
        tuning: PlatformerTuning = PlatformerTuning.default(),
    ): PlatformerEngine = PlatformerEngine(
        cols = map.width,
        rows = map.height,
        solidityAt = { c, r -> solidityAt(map, tileset, c, r) },
        startPixelX = startCol * TILE,
        startPixelY = startRow * TILE,
        tuning = tuning,
        enemySeeds = map.platformEnemies.map { enemySeed(it) },
        blockActions = blockActionGrid(map, tileset),
    )

    /**
     * Rejilla de acciones interactivas (moneda…) por celda para el motor, a partir de
     * la acción por tesela del tileset ([Tileset.platformBlockActions], que rellena la
     * importación de niveles SMW). Gana la primera capa con acción. null si no hay.
     */
    private fun blockActionGrid(map: GameMap, tileset: Tileset): IntArray? {
        if (tileset.platformBlockActions.isEmpty()) return null
        val grid = IntArray(map.width * map.height) { BlockAction.NONE.ordinal }
        var any = false
        for (r in 0 until map.height) for (c in 0 until map.width) {
            for (layer in map.layers.indices) {
                val tile = map.tileAt(layer, c, r)
                if (tile == EMPTY_TILE) continue
                val sa = tileset.platformBlockActions.getOrNull(tile) ?: continue
                val ea = when (sa) {
                    SmwBlockAction.COIN.ordinal -> BlockAction.COIN.ordinal
                    else -> BlockAction.NONE.ordinal
                }
                if (ea != BlockAction.NONE.ordinal) {
                    grid[r * map.width + c] = ea
                    any = true
                    break
                }
            }
        }
        return if (any) grid else null
    }

    /** Convierte un enemigo del mapa (celda) en semilla del motor (píxeles). */
    private fun enemySeed(mark: PlatformEnemyMark): EnemySeed =
        EnemySeed(mark.x * TILE, mark.y * TILE, mark.spriteId)
}
