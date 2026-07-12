package com.rolebuilder.core.snes

/**
 * Decodifica el NOMBRE de cada nivel de Super Mario World tal y como lo hace el juego en el
 * overworld (rutina `UpdateLevelName`, banco $04). El nombre no se guarda como una cadena, sino
 * ENSAMBLADO de tres trozos que se concatenan, tomados de un "pool" de fragmentos de texto
 * compartido y tres tablas de offsets (parte 1 / parte 2 / parte 3).
 *
 * Estructura en la ROM (SNES LoROM, direcciones verificadas 1:1 contra la ROM real):
 *  - `kLevelNames`  $04:A0FC — 1 word (2 bytes) por TRANSLEVEL (0x00..0x5F). El word empaqueta
 *    los tres índices de trozo:  i1 = byte alto,  i2 = nibble alto del byte bajo,  i3 = nibble bajo.
 *  - Tabla de offsets parte 1  $04:9C91 (word por índice)
 *  - Tabla de offsets parte 2  $04:9CCF
 *  - Tabla de offsets parte 3  $04:9CED
 *  - Pool de fragmentos        $04:9AC5 (0x1CB bytes; cada byte es una tesela de la fuente del nombre)
 *
 * El trozo `k` de la tabla T es `pool[T[k] .. T[k+1])`. El offset 0x01CB (= longitud del pool)
 * marca la entrada VACÍA (índice 0) y a la vez el final del pool (fin del último trozo real).
 *
 * Fuente de teselas→carácter (fuente del nombre de nivel, no es ASCII): 0x00-0x19 = A-Z,
 * 0x1F = espacio, 0x5A = '#', 0x5D = apóstrofo, 0x63-0x6C = 0-9, 0x76 = '.', 0x77 = '!'. El bit 7
 * de cada tesela es paleta (se ignora para el texto).
 *
 * Verificado: translevel 0x29 → "YOSHI'S ISLAND 1", 0x25 → "#1 IGGY'S CASTLE", 0x02 → "VANILLA
 * SECRET 3", 0x24 → "CHOCOLATE ISLAND 2", 0x11 → "SODA LAKE".
 */
object SmwLevelNames {

    private const val KLEVELNAMES = 0x04A0FC // word por translevel
    private const val POOL = 0x049AC5        // pool de fragmentos
    private const val T1 = 0x049C91          // offsets parte 1
    private const val T2 = 0x049CCF          // offsets parte 2
    private const val T3 = 0x049CED          // offsets parte 3
    private const val BLANK = 0x01CB         // offset centinela = trozo vacío / fin del pool

    // Nº de entradas de cada tabla de offsets (índice 0 = trozo vacío). Fijos en la ROM vanilla:
    // T1 abarca [9C91,9CCF)=31 words, T2 [9CCF,9CED)=15, T3 13 (hasta que empieza el código).
    private const val T1_COUNT = 31
    private const val T2_COUNT = 15
    private const val T3_COUNT = 13

    private const val MAX_TRANSLEVEL = 0x5F

    /** SNES(LoROM)→PC con el delta de cabecera de la ROM. */
    private fun pc(snes: Int, delta: Int) = (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta

    private fun u16(rom: ByteArray, pc: Int) =
        (rom[pc].toInt() and 0xFF) or ((rom[pc + 1].toInt() and 0xFF) shl 8)

    /** Tesela de la fuente del nombre → carácter (o '?' si no está en el mapa conocido). */
    private fun glyph(tile: Int): Char {
        val b = tile and 0x7F
        return when {
            b in 0x00..0x19 -> 'A' + b
            b == 0x1C -> '-'                                     // guión ("CHOCO-GHOST HOUSE")
            b == 0x1F -> ' '
            // Teselas del LOGO estilizado de "ILLUSION" (I L L U S I) — spelling fijo del gráfico.
            b in 0x32..0x37 -> "ILLUSI"[b - 0x32]
            // Teselas del logo estilizado "GREEN" (G R E E N) del switch palace duplicado.
            b in 0x38..0x3C -> "GREEN"[b - 0x38]
            b == 0x5A -> '#'
            b == 0x5D -> '\''
            b in 0x63..0x6C -> '0' + (b - 0x63)
            b == 0x76 -> '.'
            b == 0x77 -> '!'
            else -> '?'
        }
    }

    /**
     * Conjunto ORDENADO de todos los offsets del pool (las 3 tablas ∪ {fin del pool}). Los tres
     * grupos de fragmentos comparten el mismo pool, así que la longitud de un fragmento llega
     * hasta el SIGUIENTE límite entre TODOS los offsets, no solo el de su propia tabla (usar el
     * de la propia tabla desbordaba el último fragmento de cada tabla hasta el fin del pool).
     */
    private fun boundaries(rom: ByteArray, delta: Int): IntArray {
        val set = sortedSetOf(BLANK)
        for ((t, n) in listOf(T1 to T1_COUNT, T2 to T2_COUNT, T3 to T3_COUNT)) {
            val base = pc(t, delta)
            for (i in 0 until n) { val o = u16(rom, base + 2 * i); if (o != BLANK) set.add(o) }
        }
        return set.toIntArray()
    }

    /** Fin de un fragmento: el menor límite estrictamente mayor que [off] (o fin del pool). */
    private fun endOf(bounds: IntArray, off: Int): Int {
        for (b in bounds) if (b > off) return b
        return BLANK
    }

    /** Trozo `idx` de la tabla [tableSnes] (o null si el índice se sale del nº de entradas). */
    private fun part(rom: ByteArray, delta: Int, tableSnes: Int, count: Int, idx: Int, bounds: IntArray): String? {
        if (idx < 0 || idx >= count) return null // índice fuera de rango: translevel sin nombre real
        val off = u16(rom, pc(tableSnes, delta) + 2 * idx)
        if (off == BLANK) return "" // índice 0: trozo vacío
        val end = endOf(bounds, off)
        val poolPc = pc(POOL, delta)
        val sb = StringBuilder()
        for (o in off until end) sb.append(glyph(rom[poolPc + o].toInt() and 0xFF))
        return sb.toString()
    }

    /**
     * Nombre de un TRANSLEVEL (0x00..0x5F), ensamblado de sus tres trozos. Devuelve la cadena con
     * los espacios de relleno recortados, o null si el translevel no tiene nombre.
     */
    fun nameOfTranslevel(rom: ByteArray, delta: Int, translevel: Int): String? {
        if (translevel < 0 || translevel > MAX_TRANSLEVEL) return null
        val w = u16(rom, pc(KLEVELNAMES, delta) + 2 * translevel)
        val i1 = w shr 8
        val i2 = (w shr 4) and 0xF
        val i3 = w and 0xF
        val bounds = boundaries(rom, delta)
        val p1 = part(rom, delta, T1, T1_COUNT, i1, bounds) ?: return null
        val p2 = part(rom, delta, T2, T2_COUNT, i2, bounds) ?: return null
        val p3 = part(rom, delta, T3, T3_COUNT, i3, bounds) ?: return null
        return (p1 + p2 + p3).trim().ifEmpty { null }
    }

    /** Convierte un leveldata (0x000..0x1FF) al translevel del mapa, o null si es sublevel. */
    fun translevelOf(level: Int): Int? = when {
        level <= 0x024 -> level
        level in 0x101..0x13B -> level - 0xDC
        else -> null
    }

    /** Nombre del NIVEL (leveldata) si es un nivel de mapa; null para sublevels/salas. */
    fun nameOf(rom: ByteArray, delta: Int, level: Int): String? =
        translevelOf(level)?.let { nameOfTranslevel(rom, delta, it) }
}
