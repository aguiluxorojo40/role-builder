package com.rolebuilder.core.model

/**
 * QUÉ LES PASA A LAS TUBERÍAS CUANDO MUEVES UN TROZO DE NIVEL.
 *
 * Un warp tiene dos extremos y solo uno de ellos vive donde lo pusiste:
 *
 *  - la **boca** (`x`,`y`): la casilla de ESTE mapa por la que se entra. Va con el terreno,
 *    así que si mueves el trozo donde está, tiene que irse con él —hoy se quedaba atrás,
 *    separada del dibujo de su tubería—.
 *  - el **destino** (`destMapId`,`destX`,`destY`): unas coordenadas fijas de otro mapa (o de
 *    este). Mover el trozo al que apunta NO las cambia, así que la tubería sigue soltando al
 *    jugador donde antes había algo y ahora hay otra cosa.
 *
 * Aquí se calcula lo uno y lo otro, en funciones puras: cuántos warps toca un movimiento y
 * cómo quedarían. Quién decide si se aplica es el editor —preguntando—, porque reapuntar
 * tuberías por su cuenta es de esas ayudas que, cuando se equivocan, cuesta encontrar.
 */
object MapWarps {

    /** Cuántos warps toca un movimiento, separados por lo que hay que hacerles. */
    class Afectados(
        /** Bocas dentro del trozo movido: se van con él. */
        val bocas: Int,
        /** Warps (de cualquier mapa) que apuntan DENTRO del trozo: hay que reapuntarlos. */
        val entrantes: Int,
    ) {
        val hay: Boolean get() = bocas > 0 || entrantes > 0
        val total: Int get() = bocas + entrantes
    }

    /** ¿Cae la celda dentro del área? */
    private fun dentro(area: MapRegion.Area, x: Int, y: Int): Boolean =
        x in area.x until (area.x + area.w) && y in area.y until (area.y + area.h)

    /**
     * Cuenta lo que tocaría mover el trozo [area] del mapa [mapId]. [maps] son todos los
     * mapas del proyecto, porque un warp que apunta aquí puede vivir en cualquier otro.
     */
    fun afectados(maps: List<GameMap>, mapId: Int, area: MapRegion.Area): Afectados {
        val bocas = maps.firstOrNull { it.id == mapId }
            ?.platformWarps?.count { dentro(area, it.x, it.y) } ?: 0
        val entrantes = maps.sumOf { m ->
            m.platformWarps.count { it.destMapId == mapId && dentro(area, it.destX, it.destY) }
        }
        return Afectados(bocas, entrantes)
    }

    /**
     * Los mapas que CAMBIAN al mover el trozo [area] del mapa [mapId] en ([dx],[dy]):
     *
     *  - las bocas dentro del trozo se corren con él; la que caiga fuera del mapa se
     *    descarta, porque una boca fuera de la rejilla es una tubería a la que no se puede
     *    entrar (y hasta ahora se quedaban ahí, invisibles);
     *  - los destinos que apuntaban dentro del trozo se corren igual, recortados a los
     *    límites del mapa: es mejor una tubería que suelta un poco desviada que una que
     *    desaparece y deja la sala inalcanzable.
     *
     * Devuelve solo los mapas que de verdad cambian, listos para guardarlos.
     */
    fun ajustados(
        maps: List<GameMap>,
        mapId: Int,
        area: MapRegion.Area,
        dx: Int,
        dy: Int,
    ): List<GameMap> {
        if (dx == 0 && dy == 0) return emptyList()
        val destino = maps.firstOrNull { it.id == mapId } ?: return emptyList()
        return maps.mapNotNull { m ->
            val nuevos = m.platformWarps.mapNotNull { w ->
                var salida = w
                // La boca solo se mueve si es de ESTE mapa y está en el trozo.
                if (m.id == mapId && dentro(area, w.x, w.y)) {
                    val nx = w.x + dx
                    val ny = w.y + dy
                    if (!m.inBounds(nx, ny)) return@mapNotNull null // boca fuera: se descarta
                    salida = salida.copy(x = nx, y = ny)
                }
                if (w.destMapId == mapId && dentro(area, w.destX, w.destY)) {
                    salida = salida.copy(
                        destX = (w.destX + dx).coerceIn(0, destino.width - 1),
                        destY = (w.destY + dy).coerceIn(0, destino.height - 1),
                    )
                }
                salida
            }
            if (nuevos == m.platformWarps) null else m.copy(platformWarps = nuevos)
        }
    }
}
