package com.rolebuilder.core.engine.platformer

import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tileset
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

    /** Solidez de la celda (col,row) del mapa a partir de la pasabilidad del tileset. */
    fun solidityAt(map: GameMap, tileset: Tileset, col: Int, row: Int): SmwSolidity {
        if (!map.inBounds(col, row)) return SmwSolidity.NONE
        for (layer in map.layers.indices) {
            val tile = map.tileAt(layer, col, row)
            if (tile != EMPTY_TILE && !tileset.isPassable(tile)) return SmwSolidity.SOLID
        }
        return SmwSolidity.NONE
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
    )
}
