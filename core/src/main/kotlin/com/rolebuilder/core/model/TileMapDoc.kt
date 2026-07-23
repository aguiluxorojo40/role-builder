package com.rolebuilder.core.model

/**
 * Documento de TILEMAP por CAPAS: la materia prima para AUTORAR mapas nuevos a partir de las
 * celdas (teselas) de un atlas. Cada capa es una rejilla `mapWidth × mapHeight` de índices al
 * atlas (o [EMPTY] = casilla vacía). Es un modelo PURO (sin Android/AWT), compartido por el
 * extractor de línea de comandos (`--scene` lo vuelca; `--compose-tilemap` lo re-renderiza) y,
 * más adelante, por el editor de mapas de la app.
 *
 * Formato de texto (estable, legible y diffeable), el mismo que emite `--scene`:
 * ```
 * # comentario libre (se ignora)
 * # ... mapa=<W>x<H>  atlas_cols=<C>
 * [layer1]
 * i,i,i,...   (W enteros por fila, H filas; -1 = vacío)
 * ...
 * [layer2]
 * ...
 * ```
 * El orden de aparición de las capas se conserva (Layer 2 se dibuja debajo de Layer 1).
 */
class TileMapDoc(
    val mapWidth: Int,
    val mapHeight: Int,
    /** Columnas del atlas al que apuntan los índices (para reconstruir x,y de cada tesela). */
    val atlasCols: Int,
    /** Capas en orden de dibujo (la primera es la más al fondo): nombre → rejilla de índices. */
    val layers: List<Layer>,
) {
    class Layer(val name: String, val tiles: IntArray)

    /** Índice de una capa por nombre, o null. */
    fun layer(name: String): Layer? = layers.firstOrNull { it.name == name }

    /** Serializa al formato de texto por capas (round-trip con [parse]). */
    fun serialize(): String {
        val sb = StringBuilder()
        sb.append("# mapa=${mapWidth}x${mapHeight}  atlas_cols=${atlasCols}\n")
        sb.append("# Rejilla de índices al atlas, ${EMPTY} = casilla vacía.\n")
        for (layer in layers) {
            sb.append("[").append(layer.name).append("]\n")
            for (y in 0 until mapHeight) {
                for (x in 0 until mapWidth) {
                    if (x > 0) sb.append(',')
                    sb.append(layer.tiles.getOrElse(y * mapWidth + x) { EMPTY })
                }
                sb.append('\n')
            }
        }
        return sb.toString()
    }

    companion object {
        /** Valor de casilla vacía (sin tesela). */
        const val EMPTY = -1

        private val HEADER = Regex("""mapa=(\d+)x(\d+).*atlas_cols=(\d+)""")

        /**
         * Parsea el formato de texto por capas. Robusto a espacios y a filas incompletas (se
         * rellenan con [EMPTY]). Lanza [IllegalArgumentException] si falta la cabecera con las
         * dimensiones o si un valor no es entero.
         */
        fun parse(text: String): TileMapDoc {
            var w = -1; var h = -1; var cols = 16
            val layers = ArrayList<Layer>()
            var curName: String? = null
            var curRows = ArrayList<IntArray>()

            fun flush() {
                val name = curName ?: return
                val tiles = IntArray(w * h) { EMPTY }
                for (y in 0 until minOf(h, curRows.size)) {
                    val row = curRows[y]
                    for (x in 0 until minOf(w, row.size)) tiles[y * w + x] = row[x]
                }
                layers.add(Layer(name, tiles))
                curRows = ArrayList()
            }

            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                if (line.startsWith("#")) {
                    HEADER.find(line)?.let {
                        w = it.groupValues[1].toInt(); h = it.groupValues[2].toInt()
                        cols = it.groupValues[3].toInt()
                    }
                    continue
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    flush()
                    curName = line.substring(1, line.length - 1)
                    continue
                }
                require(w > 0 && h > 0) { "Falta la cabecera 'mapa=WxH' antes de los datos" }
                curRows.add(IntArray(0).let {
                    line.split(',').map { tok ->
                        val t = tok.trim()
                        t.toIntOrNull() ?: throw IllegalArgumentException("Valor no entero en el tilemap: '$t'")
                    }.toIntArray()
                })
            }
            flush()
            require(w > 0 && h > 0) { "Tilemap sin cabecera de dimensiones (mapa=WxH)" }
            return TileMapDoc(w, h, cols, layers)
        }

        /** Construye un documento a partir de capas ya en forma de listas de índices. */
        fun of(mapWidth: Int, mapHeight: Int, atlasCols: Int, layers: List<Pair<String, List<Int>>>): TileMapDoc =
            TileMapDoc(mapWidth, mapHeight, atlasCols, layers.map { (n, t) -> Layer(n, t.toIntArray()) })
    }
}
