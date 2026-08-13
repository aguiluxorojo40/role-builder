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

    /** Dónde se queda el contenido al cambiar el tamaño del nivel. */
    enum class Anchor { START, CENTER, END }

    /** Un nivel redimensionado, con el desplazamiento que sufrió su contenido. */
    class Resized(val map: GameMap, val dx: Int, val dy: Int)

    /**
     * Cambia el tamaño del nivel a [newWidth]×[newHeight] colocando el contenido según el
     * ANCLAJE. Es lo que faltaba: `GameMap.resized` siempre ancla arriba-izquierda, así que
     * agrandar un nivel de plataformas por abajo era imposible —el suelo se quedaba pegado
     * arriba y aparecía aire debajo— y añadir espacio por la izquierda, tampoco.
     *
     * Se mueve TODO lo que va por celdas: teselas, eventos, enemigos, ítems y también los
     * warps, que `GameMap.resized` se dejaba (quedaban bocas de tubería fuera del mapa,
     * invisibles y activas). Lo que cae fuera del nuevo tamaño se descarta.
     *
     * Devuelve además el desplazamiento aplicado, para que quien llame pueda mover con él
     * lo que vive fuera del mapa —el punto de inicio del jugador, sin ir más lejos—.
     */
    fun resized(
        map: GameMap,
        newWidth: Int,
        newHeight: Int,
        anchorX: Anchor = Anchor.START,
        anchorY: Anchor = Anchor.START,
    ): Resized {
        val dx = offset(newWidth - map.width, anchorX)
        val dy = offset(newHeight - map.height, anchorY)
        if (newWidth == map.width && newHeight == map.height) return Resized(map, 0, 0)

        val layers = map.layers.map { layer ->
            List(newWidth * newHeight) { i ->
                val sx = i % newWidth - dx
                val sy = i / newWidth - dy
                if (map.inBounds(sx, sy)) layer[sy * map.width + sx] else EMPTY_TILE
            }
        }
        fun dentro(x: Int, y: Int) = x in 0 until newWidth && y in 0 until newHeight
        return Resized(
            map.copy(
                width = newWidth,
                height = newHeight,
                layers = layers,
                events = map.events.mapNotNull { e ->
                    e.copy(x = e.x + dx, y = e.y + dy).takeIf { dentro(it.x, it.y) }
                },
                spawns = map.spawns.mapNotNull { s ->
                    s.copy(x = s.x + dx, y = s.y + dy).takeIf { dentro(it.x, it.y) }
                },
                platformEnemies = map.platformEnemies.mapNotNull { e ->
                    e.copy(x = e.x + dx, y = e.y + dy).takeIf { dentro(it.x, it.y) }
                },
                platformItems = map.platformItems.mapNotNull { i ->
                    i.copy(x = i.x + dx, y = i.y + dy).takeIf { dentro(it.x, it.y) }
                },
                // Los warps también viven en celdas de ESTE mapa (su destino no se toca).
                platformWarps = map.platformWarps.mapNotNull { w ->
                    w.copy(x = w.x + dx, y = w.y + dy).takeIf { dentro(it.x, it.y) }
                },
            ),
            dx,
            dy,
        )
    }

    /** Cuánto se corre el contenido cuando el nivel crece (o mengua) [delta] casillas. */
    private fun offset(delta: Int, anchor: Anchor): Int = when (anchor) {
        Anchor.START -> 0
        Anchor.CENTER -> delta / 2
        Anchor.END -> delta
    }

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
