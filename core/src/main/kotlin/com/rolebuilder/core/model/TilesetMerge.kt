package com.rolebuilder.core.model

/**
 * TRAERSE TESELAS DE OTRO NIVEL.
 *
 * Cada nivel importado de una ROM trae su propio tileset (su atlas de gráficos con la
 * colisión y las acciones de bloque reales de ese nivel), y un [GameMap] apunta a UNO
 * solo. Por eso, hasta ahora, hacer un nivel propio con los gráficos del castillo del
 * nivel 101 y las tuberías del 105 era imposible: la paleta solo enseñaba el tileset del
 * nivel abierto, y no había forma de pedir el de otro.
 *
 * La solución que no rompe nada es COPIAR: se añaden al tileset del nivel actual las
 * teselas elegidas de otro —píxeles y metadatos, colisión incluida—, y el nivel sigue
 * teniendo un único tileset. El motor, el renderer y el guardado no se enteran de nada.
 *
 * Lo que se copia de cada tesela:
 *  - sus píxeles (celda de `tileSize`×`tileSize` del atlas de origen),
 *  - su solidez real ([Tileset.platformSolidity]) y su forma de cuesta
 *    ([Tileset.platformSlopeShape]), para que una cuesta siga siendo una cuesta,
 *  - su acción de bloque ([Tileset.platformBlockActions]): una moneda sigue dando moneda,
 *  - su pasabilidad ([Tileset.passable]),
 *  - y su ANIMACIÓN entera si la tiene ([Tileset.animations]): copiar la moneda giratoria
 *    sin sus fotogramas la dejaría congelada.
 *
 * Teselas repetidas no se duplican: si la misma tesela (mismos píxeles y mismos
 * metadatos) ya está en el destino, se reutiliza. Así, coger diez veces el mismo suelo
 * no hace crecer el atlas diez veces.
 */
object TilesetMerge {

    /** El resultado de una copia: el tileset ampliado, su atlas nuevo y dónde cayó cada tesela. */
    class Result(
        /** Tileset del destino ya ampliado (mismas columnas, más filas si hizo falta). */
        val tileset: Tileset,
        /** Atlas nuevo, ARGB fila a fila, de [width]×[height] píxeles. */
        val pixels: IntArray,
        val width: Int,
        val height: Int,
        /**
         * Índice en el DESTINO de cada tesela pedida, en el mismo orden. Sirve para
         * seleccionar en el editor la que se acaba de traer.
         */
        val added: List<Int>,
    )

    /**
     * Copia [tiles] del tileset [src] al final de [dest] y devuelve el destino ampliado.
     *
     * Los atlas se pasan como buffers ARGB (fila a fila) porque `core` no depende de
     * Android: la app convierte sus `Bitmap` a `IntArray` y al revés.
     *
     * Exige que ambos tilesets usen el mismo tamaño de tesela: mezclar rejillas de 8 y de
     * 16 px daría teselas cortadas, y es mejor decirlo que dibujar basura.
     */
    fun append(
        dest: Tileset,
        destPixels: IntArray,
        src: Tileset,
        srcPixels: IntArray,
        tiles: List<Int>,
    ): Result {
        require(dest.tileSize == src.tileSize) {
            "No se pueden mezclar teselas de ${src.tileSize}px con un tileset de ${dest.tileSize}px"
        }
        val size = dest.tileSize
        val destW = dest.columns * size
        require(destPixels.size >= destW * dest.rows * size) {
            "El atlas de destino no cuadra con ${dest.columns}×${dest.rows} teselas de ${size}px"
        }

        // Cada tesela pedida arrastra, si está animada, TODOS sus fotogramas: se copian
        // también (aunque no se puedan pintar sueltos) o la animación quedaría coja.
        val animBySrcBase = src.animations.associateBy { it.baseTile }
        val wanted = LinkedHashSet<Int>()
        tiles.forEach { t ->
            if (t in 0 until src.tileCount) {
                wanted.add(t)
                animBySrcBase[t]?.frames?.forEach { f -> if (f in 0 until src.tileCount) wanted.add(f) }
            }
        }

        // Metadatos del destino, materializados a listas mutables del tamaño actual: si el
        // destino no traía colisión (tileset dibujado a mano) se deduce de su pasabilidad,
        // porque la tesela que llega SÍ trae colisión y las dos tienen que convivir.
        val passable = MutableList(dest.tileCount) { dest.passable.getOrElse(it) { true } }
        val solidity = MutableList(dest.tileCount) { solidityOf(dest, it) }
        val slopes = MutableList(dest.tileCount) { dest.platformSlopeShape.getOrElse(it) { NO_SLOPE } }
        val actions = MutableList(dest.tileCount) { dest.platformBlockActions.getOrElse(it) { 0 } }

        // Índice de lo que ya hay, para no duplicar: huella de píxeles + metadatos.
        val destAtlas = Atlas(destPixels, dest.columns, size)
        val existing = HashMap<Long, Int>()
        for (t in 0 until dest.tileCount) {
            existing.putIfAbsent(destAtlas.fingerprint(t, intArrayOf(solidity[t], actions[t], slopes[t])), t)
        }

        val srcAtlas = Atlas(srcPixels, src.columns, size)
        val mapping = HashMap<Int, Int>()          // tesela de origen → tesela de destino
        val newTiles = ArrayList<IntArray>()       // píxeles de las teselas realmente nuevas
        for (t in wanted) {
            val sSol = solidityOf(src, t)
            val sAct = src.platformBlockActions.getOrElse(t) { 0 }
            val sSlope = src.platformSlopeShape.getOrElse(t) { NO_SLOPE }
            val key = srcAtlas.fingerprint(t, intArrayOf(sSol, sAct, sSlope))
            val reused = existing[key]
            if (reused != null) {
                mapping[t] = reused
                continue
            }
            val index = dest.tileCount + newTiles.size
            newTiles.add(srcAtlas.cell(t))
            passable.add(src.passable.getOrElse(t) { sSol == SOLIDITY_AIR })
            solidity.add(sSol)
            slopes.add(sSlope)
            actions.add(sAct)
            existing[key] = index
            mapping[t] = index
        }

        // Rejilla nueva: se conservan las columnas y se añaden las filas que hagan falta.
        val total = dest.tileCount + newTiles.size
        val rows = maxOf(dest.rows, (total + dest.columns - 1) / dest.columns)
        val width = dest.columns * size
        val height = rows * size
        val pixels = IntArray(width * height)
        // El atlas viejo se copia tal cual (misma anchura: las filas caen en su sitio).
        System.arraycopy(destPixels, 0, pixels, 0, minOf(destPixels.size, pixels.size))
        val grown = Atlas(pixels, dest.columns, size)
        newTiles.forEachIndexed { i, cell -> grown.put(dest.tileCount + i, cell) }

        // Relleno hasta completar la última fila: teselas vacías, pasables y sin acción.
        val slots = dest.columns * rows
        while (passable.size < slots) {
            passable.add(true); solidity.add(SOLIDITY_AIR); slopes.add(NO_SLOPE); actions.add(0)
        }

        // Animaciones traídas, con los índices ya traducidos al destino.
        val newAnimations = src.animations.mapNotNull { anim ->
            val base = mapping[anim.baseTile] ?: return@mapNotNull null
            if (dest.animations.any { it.baseTile == base }) return@mapNotNull null
            val frames = anim.frames.mapNotNull { mapping[it] }
            if (frames.size < 2) null else TileAnimation(base, frames, anim.periodFrames)
        }

        return Result(
            tileset = dest.copy(
                rows = rows,
                passable = passable.toList(),
                platformSolidity = solidity.toList(),
                platformSlopeShape = slopes.toList(),
                platformBlockActions = actions.toList(),
                animations = dest.animations + newAnimations,
            ),
            pixels = pixels,
            width = width,
            height = height,
            added = tiles.mapNotNull { mapping[it] },
        )
    }

    /** Solidez efectiva de una tesela: la real si la trae, y si no la deducida de la pasabilidad. */
    private fun solidityOf(tileset: Tileset, tile: Int): Int =
        tileset.platformSolidity.getOrNull(tile)
            ?: if (tileset.passable.getOrElse(tile) { true }) SOLIDITY_AIR else SOLIDITY_SOLID

    /**
     * Un atlas de teselas visto como rejilla: buffer ARGB fila a fila, [columns] columnas y
     * celdas de [size]×[size]. Tener la rejilla en un sitio evita repetir la aritmética de
     * "¿dónde empieza la tesela n?" en cada operación (leer, escribir y firmar).
     */
    private class Atlas(val pixels: IntArray, val columns: Int, val size: Int) {
        val width: Int get() = columns * size

        /** Índice del primer píxel de la fila [y] de la tesela [tile]. */
        private fun rowStart(tile: Int, y: Int): Int =
            ((tile / columns) * size + y) * width + (tile % columns) * size

        /** Píxeles de la celda [tile]. */
        fun cell(tile: Int): IntArray {
            val out = IntArray(size * size)
            for (y in 0 until size) {
                val row = rowStart(tile, y)
                for (x in 0 until size) out[y * size + x] = pixels.getOrElse(row + x) { 0 }
            }
            return out
        }

        /** Vuelca [cell] en la posición [tile]. */
        fun put(tile: Int, cell: IntArray) {
            for (y in 0 until size) {
                val row = rowStart(tile, y)
                for (x in 0 until size) {
                    val at = row + x
                    if (at in pixels.indices) pixels[at] = cell[y * size + x]
                }
            }
        }

        /**
         * Huella de una tesela: mezcla sus píxeles con sus metadatos ([meta]: solidez, acción
         * y forma de cuesta). Dos teselas con la misma huella son intercambiables, así que se
         * reutiliza la que ya está en vez de duplicarla. No hace falta que sea criptográfica:
         * una colisión solo reutilizaría una tesela idéntica a ojos del juego.
         */
        fun fingerprint(tile: Int, meta: IntArray): Long {
            var h = 1125899906842597L // primo grande (semilla clásica de FNV-ish)
            for (y in 0 until size) {
                val row = rowStart(tile, y)
                for (x in 0 until size) h = h * 31 + pixels.getOrElse(row + x) { 0 }
            }
            meta.forEach { h = h * 31 + it }
            return h
        }
    }

    /** Ordinales que `model` no puede importar de `snes` sin atarse a él. */
    private const val SOLIDITY_AIR = 0
    private const val SOLIDITY_SOLID = 2
    private const val NO_SLOPE = -1
}
