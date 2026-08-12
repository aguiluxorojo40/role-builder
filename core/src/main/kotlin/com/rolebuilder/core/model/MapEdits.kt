package com.rolebuilder.core.model

/**
 * EDICIONES QUE CUBREN SUPERFICIE.
 *
 * Pintar casilla a casilla es lo que hace lento construir un nivel en un móvil: un nivel
 * de SMW son 27 filas por 128–320 columnas, y con el zoom al que se ve algo caben ~15
 * columnas en pantalla. Un suelo corrido son cientos de toques y varios viajes de dedo.
 *
 * Aquí viven las dos operaciones que lo convierten en un gesto, como funciones puras sobre
 * el [GameMap]: rellenar un rectángulo y rellenar la zona contigua (el "cubo"). El editor
 * solo decide QUÉ celdas y con qué tesela; la aritmética y los límites están aquí, que es
 * donde se pueden probar.
 *
 * Las dos devuelven **el mismo mapa** si no cambiaría nada (fuera del mapa, capa que no
 * existe, o la zona ya es de esa tesela). Eso importa: el editor usa esa igualdad para no
 * apilar un paso de deshacer que no deshace nada.
 */
object MapEdits {

    /**
     * Rellena con [tile] el rectángulo que va de ([x0],[y0]) a ([x1],[y1]) —en cualquier
     * orden: se normalizan— en la capa [layer], recortado al mapa.
     */
    fun fillRect(map: GameMap, layer: Int, x0: Int, y0: Int, x1: Int, y1: Int, tile: Int): GameMap {
        if (layer !in map.layers.indices) return map
        val left = minOf(x0, x1).coerceAtLeast(0)
        val right = maxOf(x0, x1).coerceAtMost(map.width - 1)
        val top = minOf(y0, y1).coerceAtLeast(0)
        val bottom = maxOf(y0, y1).coerceAtMost(map.height - 1)
        if (left > right || top > bottom) return map // el rectángulo cae fuera del mapa

        val cells = map.layers[layer].toMutableList()
        var changed = false
        for (y in top..bottom) for (x in left..right) {
            val at = y * map.width + x
            if (cells[at] != tile) {
                cells[at] = tile
                changed = true
            }
        }
        if (!changed) return map
        return map.copy(layers = map.layers.mapIndexed { i, l -> if (i == layer) cells else l })
    }

    /**
     * Rellena con [tile] la zona CONTIGUA a ([x],[y]) que comparte su tesela actual, en la
     * capa [layer]. Contigüidad de 4 vecinos (arriba/abajo/izquierda/derecha), como el
     * cubo de cualquier editor: en diagonal no se cuela.
     *
     * Va con pila propia, no con recursión: una zona puede ser el nivel entero (320×27 =
     * 8640 celdas) y eso reventaría la pila de llamadas en un móvil.
     */
    fun floodFill(map: GameMap, layer: Int, x: Int, y: Int, tile: Int): GameMap {
        if (layer !in map.layers.indices || !map.inBounds(x, y)) return map
        val cells = map.layers[layer].toMutableList()
        val from = cells[y * map.width + x]
        if (from == tile) return map // ya es de ese color: no hay nada que rellenar

        val pending = ArrayDeque<Int>()
        pending.addLast(y * map.width + x)
        while (pending.isNotEmpty()) {
            val at = pending.removeLast()
            if (cells[at] != from) continue
            cells[at] = tile
            val cx = at % map.width
            val cy = at / map.width
            if (cx > 0) pending.addLast(at - 1)
            if (cx < map.width - 1) pending.addLast(at + 1)
            if (cy > 0) pending.addLast(at - map.width)
            if (cy < map.height - 1) pending.addLast(at + map.width)
        }
        return map.copy(layers = map.layers.mapIndexed { i, l -> if (i == layer) cells else l })
    }
}
