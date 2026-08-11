package com.rolebuilder.core.model

/**
 * QUÉ CAPA ES CADA CAPA en un nivel de plataformas.
 *
 * Un [GameMap] guarda una lista de capas y hasta ahora nadie decía cuál era cuál: el
 * importador de SMW ponía el fondo en la 0 y el terreno en la 1 *solo si el nivel tenía
 * fondo* —si no, el terreno se iba a la 0—, y los niveles creados a mano desde el editor
 * ponían el suelo en la 0 siempre. Resultado: dos niveles del mismo proyecto con la misma
 * capa 0 significando cosas distintas, y las herramientas "Primer plano"/"Fondo" del
 * editor pintando en la capa contraria en todos los niveles importados (los 8 niveles
 * escaparate de la ROM traen fondo, así que **siempre** estaban invertidas).
 *
 * Aquí se fija el convenio y se arregla lo que ya está guardado:
 *
 *  - capa [BACKGROUND] (0) = **Capa 2**, el fondo. Se dibuja primero (detrás).
 *  - capa [FOREGROUND] (1) = **Capa 1**, el primer plano jugable: es la que lleva el
 *    suelo, las cuestas y los bloques.
 *
 * El orden importa porque el editor y el renderer dibujan las capas por índice: lo que
 * esté en la 0 queda detrás de lo que esté en la 1.
 */
object PlatformLayers {

    /** Índice de la capa de FONDO (Capa 2 de SMW): decorado, no colisiona. */
    const val BACKGROUND = 0

    /** Índice de la capa de PRIMER PLANO (Capa 1 de SMW): el terreno jugable. */
    const val FOREGROUND = 1

    /** Cuántas capas tiene un nivel de plataformas. */
    const val COUNT = 2

    /** Nombre visible de la capa (el que ve quien edita). */
    fun label(layer: Int): String = if (layer == BACKGROUND) "Capa 2 · Fondo" else "Capa 1 · Primer plano"

    /**
     * Construye las dos capas de un nivel importado **ya en el orden canónico**: fondo
     * abajo, primer plano arriba. Si el nivel no trae fondo, la capa 2 queda vacía (y el
     * terreno sigue estando donde toca, en la 1). Antes esto se escribía a mano en dos
     * sitios distintos —y en el caso "sin fondo" ponía el terreno en la capa 0—, que es
     * justo de donde venía la incoherencia.
     */
    fun layersOf(foreground: List<Int>, background: List<Int>, width: Int, height: Int): List<List<Int>> {
        val empty = List(width * height) { EMPTY_TILE }
        return listOf(background.ifEmpty { empty }, foreground)
    }

    /**
     * ¿Está el mapa en el orden canónico? Se mira por el CONTENIDO, no por la fe: el
     * terreno es la capa con teselas sólidas (suelo, bloques, cuestas). Si las sólidas
     * están en la capa 0 y la 1 no tiene ninguna, el mapa viene del convenio viejo.
     *
     * Sin tileset (o sin datos de colisión) se cae a una regla más tosca pero segura:
     * una capa 1 completamente vacía con una capa 0 con contenido es el patrón exacto
     * que dejaba el editor antiguo al crear un nivel desde cero.
     */
    fun isCanonical(map: GameMap, tileset: Tileset?): Boolean {
        if (map.layers.size < COUNT) return false
        val bg = map.layers[BACKGROUND]
        val fg = map.layers[FOREGROUND]
        if (tileset != null) {
            val solidBg = countSolid(bg, tileset)
            val solidFg = countSolid(fg, tileset)
            // El terreno manda: si hay sólidas y TODAS están en la capa de fondo, está al revés.
            if (solidBg > 0 && solidFg == 0) return false
            if (solidFg > 0) return true
        }
        // Sin colisión que mirar: el patrón "todo en la 0 y la 1 vacía" es el convenio viejo.
        val fgEmpty = fg.all { it == EMPTY_TILE }
        val bgEmpty = bg.all { it == EMPTY_TILE }
        return !(fgEmpty && !bgEmpty)
    }

    /**
     * Devuelve el mapa en el orden canónico: rellena la capa que falte y, si el nivel
     * viene del convenio viejo, INTERCAMBIA las dos capas. Es idempotente (aplicarlo dos
     * veces da lo mismo) y no toca nada más del mapa: enemigos, ítems y warps van por
     * celda, no por capa.
     *
     * Solo tiene sentido en proyectos de plataformas ([GameMode.PLATFORMER]); el editor
     * top-down del RPG usa la capa 0 como suelo y la 1 como decoración, y ahí no se toca.
     */
    fun normalized(map: GameMap, tileset: Tileset?): GameMap {
        val padded = padded(map)
        if (isCanonical(padded, tileset)) return padded
        val swapped = padded.layers.toMutableList()
        val bg = swapped[BACKGROUND]
        swapped[BACKGROUND] = swapped[FOREGROUND]
        swapped[FOREGROUND] = bg
        return padded.copy(layers = swapped)
    }

    /** El mapa con exactamente [COUNT] capas (las que falten, vacías). */
    fun padded(map: GameMap): GameMap {
        if (map.layers.size == COUNT) return map
        val empty = List(map.width * map.height) { EMPTY_TILE }
        val layers = (0 until COUNT).map { map.layers.getOrElse(it) { empty } }
        return map.copy(layers = layers)
    }

    private fun countSolid(layer: List<Int>, tileset: Tileset): Int =
        layer.count { it != EMPTY_TILE && it >= 0 && isSolid(tileset, it) }

    /**
     * ¿Esta tesela es terreno? Con la colisión REAL importada de la ROM
     * ([Tileset.platformSolidity]) cualquier valor distinto de "aire" cuenta; sin ella,
     * la pasabilidad clásica del tileset.
     */
    private fun isSolid(tileset: Tileset, tile: Int): Boolean {
        val ord = tileset.platformSolidity.getOrNull(tile)
        if (ord != null) return ord != SOLIDITY_AIR
        return !tileset.isPassable(tile)
    }

    /** Ordinal de `SmwSolidity.NONE` (aire). Se repite aquí para no atar `model` a `snes`. */
    private const val SOLIDITY_AIR = 0
}
