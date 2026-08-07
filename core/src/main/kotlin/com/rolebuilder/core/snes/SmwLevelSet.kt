package com.rolebuilder.core.snes

/**
 * QUÉ es un nivel de verdad, y qué no.
 *
 * La ROM tiene **512 huecos** de nivel (0x000..0x1FF), pero la inmensa mayoría no son
 * niveles: son huecos vacíos o basura que nadie referencia. Medir o listar sobre los 512
 * infla cualquier número y llena la UI de entradas que no llevan a ninguna parte. Este
 * objeto es la ÚNICA fuente de verdad de qué huecos cuentan, y la saca de la ROM en vez de
 * llevar una lista a mano.
 *
 * ## De casilla del mapa a número de nivel
 *
 * El overworld no guarda el número de nivel: guarda un **translevel** (el número correlativo
 * de la casilla, ver [SmwOverworldLevels]). La conversión es de `LoadLevelInfo` ($05:D908):
 *
 * ```c
 * v2 = ow_level_number_of_each_tiletbl[pos];        // translevel
 * if (v2 >= 0x25) v2 -= 0x24;
 * level = (ow_players_map[player] != 0) << 8 | v2;  // byte alto = estás en un SUBMAPA
 * ```
 *
 * O sea: los translevels 1..0x24 son el **mapa principal** y dan los niveles 0x001..0x024;
 * los 0x25 en adelante son **submapas** y dan 0x101 en adelante. Que "translevel >= 0x25" y
 * "está en un submapa" son la misma cosa NO es una suposición: se comprueba contra la ROM en
 * `SmwLevelSetTest` (en la US se cumple en las 92 casillas, 36 + 56).
 *
 * ## Los números, medidos en la ROM US
 *
 * - **92 niveles de mapa**: 0x001..0x024 y 0x101..0x138. Uno por casilla del overworld.
 * - **215 niveles alcanzables**: esos 92 más los **123 sub-niveles** (cuartos, tuberías,
 *   salas de jefe) a los que llevan sus salidas. No tienen casilla propia, pero son niveles
 *   reales y se importan con su nivel padre.
 * - Los 297 huecos restantes no los referencia nadie.
 *
 * Ojo con el **96**: es el número de SALIDAS del juego ("96 EXITS"), no de niveles. Hay 92
 * casillas y 96 salidas porque bastantes niveles tienen dos (normal + secreta) y otros no
 * tienen ninguna (Yoshi's House, Top Secret Area) o acaban de otra forma (castillos y
 * fortalezas, por jefe; palacios, por interruptor). Son dos cuentas distintas: no se deben
 * mezclar.
 */
object SmwLevelSet {

    /** Último translevel del mapa principal; a partir de 0x25 son submapas ($05:D908). */
    const val LAST_MAIN_MAP_TRANSLEVEL = 0x24

    /**
     * Nivel al que lleva una casilla del overworld, según `LoadLevelInfo` ($05:D908).
     * [onMainMap] es lo que en el juego es `ow_players_map[player] == 0`.
     */
    fun levelOfTranslevel(translevel: Int, onMainMap: Boolean): Int =
        (if (onMainMap) 0 else 0x100) or
            (if (translevel > LAST_MAIN_MAP_TRANSLEVEL) translevel - LAST_MAIN_MAP_TRANSLEVEL else translevel)

    /** Un nivel que tiene casilla propia en el mapa del mundo. */
    data class MapLevel(
        /** Nº de nivel (0x001..0x024 o 0x101..0x138 en la ROM US). */
        val level: Int,
        /** Translevel = número correlativo de su casilla, e índice de la tabla de nombres. */
        val translevel: Int,
        /** Nombre real leído de la ROM, o null si esa entrada no tiene. */
        val name: String?,
        /** true si la casilla está en el mapa principal; false si en un submapa. */
        val onMainMap: Boolean,
    )

    /**
     * Los niveles que tienen CASILLA en el overworld, en el orden en que el juego los numera.
     * En la ROM US vanilla son 92.
     */
    fun mapLevels(rom: ByteArray, delta: Int): List<MapLevel> =
        SmwOverworldLevels.levels(rom, delta).map {
            MapLevel(
                level = levelOfTranslevel(it.levelNumber, it.onMainMap),
                translevel = it.levelNumber,
                name = it.name,
                onMainMap = it.onMainMap,
            )
        }

    /**
     * Todos los niveles ALCANZABLES: los de mapa más el cierre transitivo de sus salidas
     * (los sub-niveles). Es el conjunto honesto sobre el que medir cobertura, en vez de los
     * 512 huecos. En la ROM US vanilla son 215.
     */
    fun reachableLevels(rom: ByteArray, header: SnesHeader): Set<Int> {
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val seen = sortedSetOf<Int>()
        val queue = ArrayDeque<Int>()
        for (m in mapLevels(rom, delta)) if (seen.add(m.level)) queue.addLast(m.level)
        while (queue.isNotEmpty()) {
            val lv = queue.removeFirst()
            val conn = SmwLevelExits.read(rom, header, lv) ?: continue
            for (e in conn.exits) {
                val d = e.destinationLevel
                if (d in 0 until SnesGameRecipes.SMW_LEVEL_COUNT && seen.add(d)) queue.addLast(d)
            }
        }
        return seen
    }
}
