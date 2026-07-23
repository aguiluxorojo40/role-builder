package com.rolebuilder.core.model

import kotlinx.serialization.Serializable

/**
 * Un SELLO (stamp / prefab): un trozo reutilizable de mapa — sus dos capas de teselas más los
 * enemigos e ítems que contenga — que se puede EXTRAER de un mapa y PEGAR en otro (o en el mismo)
 * al construir niveles nuevos. Las teselas son índices al mismo atlas ([tilesetId]) que el mapa
 * de origen, así que un sello solo encaja con proyectos que usen ese tileset.
 *
 * Es un modelo PURO y serializable (se guarda en el proyecto); la UI del editor solo tiene que
 * (1) marcar una región y llamar a [fromRegion], y (2) tocar una celda y llamar a [pasteInto].
 */
@Serializable
data class MapStamp(
    val name: String,
    val width: Int,
    val height: Int,
    val tilesetId: Int,
    /** Capas alineadas con [GameMap.layers] (0 = primer plano, 1 = fondo): width*height índices. */
    val layers: List<List<Int>>,
    val enemies: List<PlatformEnemyMark> = emptyList(),
    val items: List<PlatformItemMark> = emptyList(),
) {
    init {
        require(width > 0 && height > 0) { "Un sello no puede estar vacío ($width×$height)" }
        require(layers.isNotEmpty()) { "Un sello necesita al menos una capa" }
        require(layers.all { it.size == width * height }) { "Capa con tamaño distinto de $width×$height" }
    }

    /**
     * Devuelve una copia de [map] con este sello ESTAMPADO con su esquina superior-izquierda en
     * ([x0],[y0]). Las celdas del sello que no sean [EMPTY_TILE] sobrescriben; las vacías se
     * saltan (dejan ver lo que había) salvo que [overwriteEmpty] sea true (borra debajo). Se
     * recorta a los límites del mapa. Enemigos e ítems se desplazan a ([x0],[y0]) y se descartan
     * los que caigan fuera. No exige que los tamaños coincidan.
     */
    fun pasteInto(map: GameMap, x0: Int, y0: Int, overwriteEmpty: Boolean = false): GameMap {
        val layerCount = minOf(map.layers.size, layers.size)
        val newLayers = map.layers.mapIndexed { li, dest ->
            if (li >= layerCount) return@mapIndexed dest
            val out = dest.toMutableList()
            val src = layers[li]
            for (sy in 0 until height) for (sx in 0 until width) {
                val tx = x0 + sx; val ty = y0 + sy
                if (!map.inBounds(tx, ty)) continue
                val v = src[sy * width + sx]
                if (v == EMPTY_TILE && !overwriteEmpty) continue
                out[ty * map.width + tx] = v
            }
            out
        }
        val movedEnemies = enemies.mapNotNull { e ->
            val nx = e.x + x0; val ny = e.y + y0
            if (map.inBounds(nx, ny)) e.copy(x = nx, y = ny) else null
        }
        val movedItems = items.mapNotNull { it2 ->
            val nx = it2.x + x0; val ny = it2.y + y0
            if (map.inBounds(nx, ny)) it2.copy(x = nx, y = ny) else null
        }
        return map.copy(
            layers = newLayers,
            platformEnemies = map.platformEnemies + movedEnemies,
            platformItems = map.platformItems + movedItems,
        )
    }

    companion object {
        /**
         * Extrae un sello de la región rectangular de [map] que empieza en ([x0],[y0]) con tamaño
         * [w]×[h] (recortado a los límites del mapa). Copia las dos capas y captura los enemigos e
         * ítems de la región, con sus coordenadas ya RELATIVAS a la esquina del sello. Devuelve
         * null si la región recortada queda vacía.
         */
        fun fromRegion(map: GameMap, x0: Int, y0: Int, w: Int, h: Int, name: String): MapStamp? {
            val sx = x0.coerceIn(0, map.width)
            val sy = y0.coerceIn(0, map.height)
            val ex = (x0 + w).coerceIn(0, map.width)
            val ey = (y0 + h).coerceIn(0, map.height)
            val rw = ex - sx; val rh = ey - sy
            if (rw <= 0 || rh <= 0) return null
            val layers = map.layers.map { layer ->
                ArrayList<Int>(rw * rh).apply {
                    for (yy in sy until ey) for (xx in sx until ex) add(layer[yy * map.width + xx])
                }
            }
            val enemies = map.platformEnemies
                .filter { it.x in sx until ex && it.y in sy until ey }
                .map { it.copy(x = it.x - sx, y = it.y - sy) }
            val items = map.platformItems
                .filter { it.x in sx until ex && it.y in sy until ey }
                .map { it.copy(x = it.x - sx, y = it.y - sy) }
            return MapStamp(name, rw, rh, map.tilesetId, layers, enemies, items)
        }
    }
}
