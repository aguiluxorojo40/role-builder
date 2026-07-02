package com.rolebuilder.core.model

import kotlinx.serialization.Serializable

/**
 * Un tileset: imagen en images/<image> dividida en tiles cuadrados de [tileSize] px.
 * El índice de tile recorre la imagen por filas (fila 0: 0..columns-1, etc.).
 */
@Serializable
data class Tileset(
    val id: Int,
    val name: String,
    val image: String,
    val tileSize: Int = 16,
    val columns: Int = 8,
    val rows: Int = 8,
    /** true = se puede caminar sobre el tile. Indexado por índice de tile. */
    val passable: List<Boolean> = List(columns * rows) { true },
) {
    val tileCount: Int get() = columns * rows

    fun isPassable(tile: Int): Boolean =
        tile == EMPTY_TILE || passable.getOrElse(tile) { true }
}
