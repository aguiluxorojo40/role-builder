package com.rolebuilder.core.model

/**
 * SELECCIONAR UN TROZO DE NIVEL Y MANEJARLO: copiar, cortar, pegar, duplicar, voltear.
 *
 * Construir un fondo no es pintar tesela a tesela: es hacer un trozo bueno —una arcada, una
 * fila de ventanas, una montaña— y repetirlo, moverlo y espejarlo. Para eso hace falta un
 * marco de selección con portapapeles, que es lo que hay aquí.
 *
 * El trozo copiado se guarda en un [MapStamp] (el mismo modelo que los sellos con nombre del
 * proyecto, así que una selección buena se puede guardar como sello sin convertir nada). La
 * diferencia es el ALCANCE: estas operaciones trabajan sobre las capas que le digas, no
 * sobre todas. Es lo que permite recortar un fondo sin llevarte el suelo que tiene delante.
 *
 * Regla del pegado: **una capa del portapapeles que esté entera vacía no se pega**. Así, un
 * trozo copiado solo del fondo lleva el primer plano vacío y al pegarlo no toca el terreno,
 * sin necesidad de arrastrar banderas por todos lados.
 */
object MapRegion {

    /**
     * Una región rectangular en casillas: esquina ([x],[y]) y tamaño [w]×[h]. Existe para que
     * el editor no ande pasando cuatro enteros sueltos por todas partes —y para tener en un
     * sitio la normalización de esquinas, que el dedo lo mismo arrastra hacia arriba y a la
     * izquierda que al revés.
     */
    class Area(val x: Int, val y: Int, val w: Int, val h: Int) {
        companion object {
            /** La región entre dos esquinas, en el orden que sea (ambas incluidas). */
            fun between(x0: Int, y0: Int, x1: Int, y1: Int): Area = Area(
                x = minOf(x0, x1),
                y = minOf(y0, y1),
                w = kotlin.math.abs(x1 - x0) + 1,
                h = kotlin.math.abs(y1 - y0) + 1,
            )
        }
    }

    /**
     * Copia la región [area] (recortada al mapa) en las capas [layers]. Las capas que no estén
     * en [layers] salen vacías. Con [withObjects], los enemigos e ítems de la región viajan
     * también, con coordenadas relativas a la esquina.
     *
     * Devuelve null si la región recortada no deja nada.
     */
    fun copy(
        map: GameMap,
        area: Area,
        layers: List<Int>,
        withObjects: Boolean,
        name: String = "",
    ): MapStamp? {
        val sx = area.x.coerceIn(0, map.width)
        val sy = area.y.coerceIn(0, map.height)
        val ex = (area.x + area.w).coerceIn(0, map.width)
        val ey = (area.y + area.h).coerceIn(0, map.height)
        val rw = ex - sx
        val rh = ey - sy
        if (rw <= 0 || rh <= 0) return null

        val cells = map.layers.mapIndexed { li, layer ->
            if (li !in layers) {
                List(rw * rh) { EMPTY_TILE }
            } else {
                ArrayList<Int>(rw * rh).apply {
                    for (yy in sy until ey) for (xx in sx until ex) add(layer[yy * map.width + xx])
                }
            }
        }
        val enemies = if (!withObjects) emptyList() else map.platformEnemies
            .filter { it.x in sx until ex && it.y in sy until ey }
            .map { it.copy(x = it.x - sx, y = it.y - sy) }
        val items = if (!withObjects) emptyList() else map.platformItems
            .filter { it.x in sx until ex && it.y in sy until ey }
            .map { it.copy(x = it.x - sx, y = it.y - sy) }
        return MapStamp(name, rw, rh, map.tilesetId, cells, enemies, items)
    }

    /**
     * Vacía la región en las capas [layers] (y se lleva los enemigos e ítems si
     * [withObjects]). Es el "borrar" del marco de selección y la segunda mitad del "cortar".
     */
    fun clear(
        map: GameMap,
        area: Area,
        layers: List<Int>,
        withObjects: Boolean,
    ): GameMap {
        val sx = area.x.coerceIn(0, map.width)
        val sy = area.y.coerceIn(0, map.height)
        val ex = (area.x + area.w).coerceIn(0, map.width)
        val ey = (area.y + area.h).coerceIn(0, map.height)
        if (ex <= sx || ey <= sy) return map

        var changed = false
        val newLayers = map.layers.mapIndexed { li, layer ->
            if (li !in layers) return@mapIndexed layer
            val out = layer.toMutableList()
            for (yy in sy until ey) for (xx in sx until ex) {
                val at = yy * map.width + xx
                if (out[at] != EMPTY_TILE) {
                    out[at] = EMPTY_TILE
                    changed = true
                }
            }
            out
        }
        val enemies = if (!withObjects) map.platformEnemies else
            map.platformEnemies.filterNot { it.x in sx until ex && it.y in sy until ey }
        val items = if (!withObjects) map.platformItems else
            map.platformItems.filterNot { it.x in sx until ex && it.y in sy until ey }
        if (!changed && enemies.size == map.platformEnemies.size && items.size == map.platformItems.size) {
            return map
        }
        return map.copy(layers = newLayers, platformEnemies = enemies, platformItems = items)
    }

    /**
     * Pega [clip] con su esquina superior-izquierda en ([x0],[y0]), recortando a los límites.
     * Solo toca las capas que el portapapeles trae con contenido (una capa entera vacía se
     * salta: es lo que hace que un trozo de fondo no pise el terreno).
     *
     * Dentro de una capa que sí se pega, las celdas vacías del trozo TAMBIÉN se escriben:
     * pegar es dejar el bloque tal cual estaba, con sus huecos.
     */
    fun paste(map: GameMap, clip: MapStamp, x0: Int, y0: Int): GameMap {
        val live = clip.layers.indices.filter { li -> clip.layers[li].any { it != EMPTY_TILE } }
        if (live.isEmpty() && clip.enemies.isEmpty() && clip.items.isEmpty()) return map

        val newLayers = map.layers.mapIndexed { li, dest ->
            if (li !in live || li >= clip.layers.size) return@mapIndexed dest
            val out = dest.toMutableList()
            val src = clip.layers[li]
            for (sy in 0 until clip.height) for (sx in 0 until clip.width) {
                val tx = x0 + sx
                val ty = y0 + sy
                if (!map.inBounds(tx, ty)) continue
                out[ty * map.width + tx] = src[sy * clip.width + sx]
            }
            out
        }
        val enemies = clip.enemies.mapNotNull { e ->
            val nx = e.x + x0
            val ny = e.y + y0
            if (map.inBounds(nx, ny)) e.copy(x = nx, y = ny) else null
        }
        val items = clip.items.mapNotNull { i ->
            val nx = i.x + x0
            val ny = i.y + y0
            if (map.inBounds(nx, ny)) i.copy(x = nx, y = ny) else null
        }
        return map.copy(
            layers = newLayers,
            // Lo que hubiera EXACTAMENTE en las celdas pegadas se sustituye: pegar encima de
            // un enemigo no deja dos apilados en la misma casilla.
            platformEnemies = map.platformEnemies.filterNot { old ->
                enemies.any { it.x == old.x && it.y == old.y }
            } + enemies,
            platformItems = map.platformItems.filterNot { old ->
                items.any { it.x == old.x && it.y == old.y }
            } + items,
        )
    }

    /**
     * Las teselas DISTINTAS que usa [clip] (sin el vacío). Es lo que hay que traerse al
     * tileset del nivel de destino para que el trozo signifique lo mismo allí.
     */
    fun usedTiles(clip: MapStamp): List<Int> =
        clip.layers.flatten().filter { it != EMPTY_TILE }.distinct().sorted()

    /**
     * El mismo trozo pero con sus teselas TRADUCIDAS por [mapping] (tesela del tileset de
     * origen → tesela del de destino) y apuntando a [tilesetId].
     *
     * Esto es lo que hace que un asset sea portable. Un sello, o un trozo copiado, no
     * guarda gráficos: guarda ÍNDICES a un atlas. Llevarlo a un nivel con otro tileset sin
     * traducir sus índices pinta teselas al azar —que es lo que pasaba—, así que el camino
     * bueno es copiar sus teselas al tileset del destino y reescribir el trozo con los
     * índices nuevos. Lo que no esté en [mapping] queda vacío, que es preferible a dejar un
     * índice mintiendo.
     */
    fun remapped(clip: MapStamp, mapping: Map<Int, Int>, tilesetId: Int): MapStamp = clip.copy(
        tilesetId = tilesetId,
        layers = clip.layers.map { layer ->
            layer.map { if (it == EMPTY_TILE) EMPTY_TILE else mapping[it] ?: EMPTY_TILE }
        },
    )

    /** El trozo espejado en horizontal (para arcadas, laderas y simetrías del fondo). */
    fun flippedH(clip: MapStamp): MapStamp = clip.copy(
        layers = clip.layers.map { layer ->
            ArrayList<Int>(layer.size).apply {
                for (y in 0 until clip.height) for (x in 0 until clip.width) {
                    add(layer[y * clip.width + (clip.width - 1 - x)])
                }
            }
        },
        enemies = clip.enemies.map { it.copy(x = clip.width - 1 - it.x) },
        items = clip.items.map { it.copy(x = clip.width - 1 - it.x) },
    )

    /** El trozo espejado en vertical (techos a partir de suelos, reflejos). */
    fun flippedV(clip: MapStamp): MapStamp = clip.copy(
        layers = clip.layers.map { layer ->
            ArrayList<Int>(layer.size).apply {
                for (y in 0 until clip.height) for (x in 0 until clip.width) {
                    add(layer[(clip.height - 1 - y) * clip.width + x])
                }
            }
        },
        enemies = clip.enemies.map { it.copy(y = clip.height - 1 - it.y) },
        items = clip.items.map { it.copy(y = clip.height - 1 - it.y) },
    )
}
