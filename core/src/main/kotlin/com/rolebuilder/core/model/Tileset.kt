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
    /**
     * Índices de tile que el renderer 2.5D dibuja DE PIE (billboard vertical,
     * como los personajes) cuando aparecen en la capa 2 del mapa (índice de
     * capa 1): árboles, arbustos, puertas, rocas... Solo afecta al dibujado
     * con el diorama activo ([Project.dioramaTilt] > 0); no cambia colisiones
     * ni lógica de juego.
     */
    val standingTiles: List<Int> = emptyList(),
    /**
     * Animaciones de tiles: teselas que ciclan entre varios fotogramas (monedas,
     * bloques ?, agua de SMW). El renderer sustituye la tesela base por el fotograma
     * actual. Vacío = sin animación.
     */
    val animations: List<TileAnimation> = emptyList(),
    /**
     * Solidez de plataformas por índice de tile, para el motor de Platform Builder.
     * Vacío = usar [passable] (no atravesable → suelo sólido). Cuando viene relleno
     * (p. ej. de un nivel SMW extraído), conserva la colisión REAL del juego por
     * casilla codificada como el ordinal de `SmwSolidity`:
     * 0=aire, 1=borde de un sentido, 2=sólido, 3=cuesta, 4=cuesta doble, 5=pinchos.
     */
    val platformSolidity: List<Int> = emptyList(),
    /**
     * Acción interactiva por índice de tile (ordinal de `SmwBlockAction`) para el
     * motor de Platform Builder: 0 = nada, 1 = moneda (se recoge). Vacío = sin bloques
     * interactivos. Lo rellena la importación de niveles SMW.
     */
    val platformBlockActions: List<Int> = emptyList(),
) {
    val tileCount: Int get() = columns * rows

    fun isPassable(tile: Int): Boolean =
        tile == EMPTY_TILE || passable.getOrElse(tile) { true }
}

/**
 * Una tesela animada: [baseTile] es la tesela que aparece en el mapa; [frames] son
 * los índices de tesela (en el mismo tileset) por los que cicla, y [periodFrames]
 * cuántos fotogramas de juego (60 fps) dura cada uno. El primer frame suele ser la
 * propia [baseTile].
 */
@Serializable
data class TileAnimation(
    val baseTile: Int,
    val frames: List<Int>,
    val periodFrames: Int = 8,
)
