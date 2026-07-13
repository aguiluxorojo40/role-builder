package com.rolebuilder.core.snes

/**
 * "Bundle" (paquete) de un nivel de Super Mario World con TODOS sus sub-niveles
 * enlazados: sigue las SALIDAS DE PANTALLA ([SmwLevelExits]) desde el nivel de inicio
 * y reúne cada sub-nivel alcanzable como un mapa jugable ([SnesGameRecipes.SmwLevelMap]),
 * más el GRAFO DE WARPS (qué pantalla de qué sub-mapa lleva a qué sub-mapa y a qué
 * posición de entrada). Es lo que permite importar un nivel de SMW ENTERO —con sus
 * tuberías/puertas a otras salas— en vez de una sola pantalla suelta.
 *
 * El disparo del warp en el juego lo hace una tubería/puerta (clasificadas aparte) en
 * la pantalla de la salida; aquí solo se resuelve el DESTINO y la posición de entrada.
 * La posición de entrada usa, de momento, la ENTRADA PRINCIPAL del nivel destino
 * ([SmwLevelStartReader]); las entradas secundarias se afinarán con su fórmula propia.
 */
object SmwLevelBundle {

    /** Un warp: pantalla [fromScreen] del sub-mapa [fromMapIndex] → sub-mapa [toMapIndex]. */
    class Warp(
        val fromMapIndex: Int,
        val fromScreen: Int,
        val toMapIndex: Int,
        val entryXTile: Int,
        val entryYTile: Int,
        val isSecondary: Boolean,
    )

    /**
     * Un nivel SMW completo: [maps] son los sub-niveles (índice 0 = el de inicio),
     * [levels] su número de nivel SMW por índice, y [warps] cómo se enlazan.
     */
    class Bundle(
        val startLevel: Int,
        val maps: List<SnesGameRecipes.SmwLevelMap>,
        val levels: List<Int>,
        val warps: List<Warp>,
    )

    /** Cota de sub-niveles para no arrastrar niveles enteros por punteros raros. */
    private const val DEFAULT_MAX_MAPS = 8

    /**
     * Extrae el bundle a partir de [startLevel]. Recorre las salidas en anchura,
     * extrayendo cada sub-nivel alcanzable (hasta [maxMaps]). Devuelve null si el nivel
     * de inicio no es reconstruible.
     */
    fun extract(
        rom: ByteArray,
        header: SnesHeader,
        startLevel: Int,
        maxMaps: Int = DEFAULT_MAX_MAPS,
    ): Bundle? {
        val index = LinkedHashMap<Int, Int>()          // nivel SMW → índice de sub-mapa
        val levels = ArrayList<Int>()
        val maps = ArrayList<SnesGameRecipes.SmwLevelMap>()
        val conns = ArrayList<SmwLevelExits.LevelConnectivity?>()

        fun addLevel(lv: Int): Int? {
            index[lv]?.let { return it }
            if (maps.size >= maxMaps) return null
            val m = SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) ?: return null
            val i = maps.size
            index[lv] = i
            levels.add(lv)
            maps.add(m)
            conns.add(runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull())
            return i
        }

        if (addLevel(startLevel) == null) return null
        // BFS: añade los destinos alcanzables.
        var head = 0
        while (head < levels.size) {
            val lv = levels[head++]
            val conn = conns[index[lv]!!] ?: continue
            for (exit in conn.exits) {
                val dest = exit.destinationLevel
                if (dest in 0 until SMW_LEVEL_MAX && dest !in index) addLevel(dest)
            }
        }

        // Grafo de warps (solo a destinos que hemos podido extraer).
        val warps = ArrayList<Warp>()
        for (lv in levels) {
            val fi = index[lv]!!
            val conn = conns[fi] ?: continue
            for (exit in conn.exits) {
                val ti = index[exit.destinationLevel] ?: continue
                val (ex, ey) = entryPosition(rom, header, exit.destinationLevel)
                warps.add(Warp(fi, exit.fromScreen, ti, ex, ey, exit.isSecondary))
            }
        }
        return Bundle(startLevel, maps, levels, warps)
    }

    /** Posición de entrada (en teselas de 16px) del nivel destino: su entrada principal. */
    private fun entryPosition(rom: ByteArray, header: SnesHeader, level: Int): Pair<Int, Int> {
        val start = SmwLevelStartReader.read(rom, header, level) ?: return 2 to 2
        return (start.startPixelX / 16) to (start.startPixelY / 16)
    }

    private const val SMW_LEVEL_MAX = 0x200
}
