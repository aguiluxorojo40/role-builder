package com.rolebuilder.core.snes

/**
 * CUESTAS de Super Mario World, portadas de las tablas REALES del banco $00
 * (`kSlopeDataTables_ShapeOfSlope` y `kSlopeSteepness`, $00:E55E, verificadas en
 * snesrev/smw): cada bloque Map16 de cuesta (byte bajo 0x6E..0xD7) mapea a una de 32
 * FORMAS, y cada forma da el OFFSET del suelo desde el borde superior del bloque en
 * cada una de sus 16 columnas de píxel (0 = suelo arriba del todo, 15 = abajo).
 *
 * Formas útiles: 0-7 pendiente 1:4 (cuatro bloques), 8-11 pendiente 1:2 (dos
 * bloques), 12/13 (y 14/15, 24-27) las rampas de 45°, 16/17 crestas redondeadas,
 * 28/30 medios bordes. Los valores >= 0xF0 son alturas NEGATIVAS: cuestas de TECHO
 * (boca abajo); el motor las trata como bloque macizo.
 */
object SmwSlopes {

    /** Sin cuesta (celda normal). */
    const val NO_SLOPE = -1

    /** Forma canónica: rampa de 45° que SUBE hacia la derecha («/»). */
    const val SHAPE_45_UP_RIGHT = 12

    /** Forma canónica: rampa de 45° que BAJA hacia la derecha («\»). */
    const val SHAPE_45_DOWN_RIGHT = 13

    /** Pendiente suave 1:2 que SUBE a la derecha: bloque izquierdo, luego derecho. */
    const val SHAPE_GENTLE_UP_RIGHT_A = 8
    const val SHAPE_GENTLE_UP_RIGHT_B = 9

    /** Pendiente suave 1:2 que BAJA a la derecha: bloque izquierdo, luego derecho. */
    const val SHAPE_GENTLE_DOWN_RIGHT_A = 10
    const val SHAPE_GENTLE_DOWN_RIGHT_B = 11

    /** `kSlopeDataTables_ShapeOfSlope`: 32 formas x 16 columnas. */
    private val SHAPES = intArrayOf(
        15, 15, 15, 15, 14, 14, 14, 14, 13, 13, 13, 13, 12, 12, 12, 12,
        11, 11, 11, 11, 10, 10, 10, 10, 9, 9, 9, 9, 8, 8, 8, 8,
        7, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 5, 4, 4, 4, 4,
        3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0, 0, 0, 0,
        0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3,
        4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7,
        8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11,
        12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15,
        15, 15, 14, 14, 13, 13, 12, 12, 11, 11, 10, 10, 9, 9, 8, 8,
        7, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0, 0,
        0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7,
        8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15,
        15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        8, 6, 4, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 4, 6, 8,
        255, 254, 253, 252, 251, 250, 249, 248, 247, 246, 245, 244, 243, 242, 241, 240,
        240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255,
        255, 255, 254, 254, 253, 253, 252, 252, 251, 251, 250, 250, 249, 249, 248, 248,
        247, 247, 246, 246, 245, 245, 244, 244, 243, 243, 242, 242, 241, 241, 240, 240,
        240, 240, 241, 241, 242, 242, 243, 243, 244, 244, 245, 245, 246, 246, 247, 247,
        248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253, 253, 254, 254, 255, 255,
        15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
        16, 16, 16, 16, 16, 16, 16, 16, 14, 12, 10, 8, 6, 4, 2, 0,
        14, 12, 10, 8, 6, 4, 2, 0, 254, 252, 250, 248, 246, 244, 242, 240,
        0, 2, 4, 6, 8, 10, 12, 14, 16, 16, 16, 16, 16, 16, 16, 16,
        240, 242, 244, 246, 248, 250, 252, 254, 0, 2, 4, 6, 8, 10, 12, 14,
    )

    /** `kSlopeSteepness` ($00:E55E): bloque Map16 (byte bajo 0x6E..0xD7) -> forma. */
    private val BLOCK_TO_SHAPE = intArrayOf(
        0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7,
        8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11,
        12, 12, 12, 12, 12, 13, 13, 13, 13, 13, 14, 15, 16, 17, 3, 3, 4, 4, 9, 9,
        10, 10, 12, 12, 13, 13, 18, 19, 20, 21, 22, 23, 28, 29, 30, 31, 24, 25, 26, 27,
        8, 9, 10, 11, 12, 13,
    )

    /** Forma de cuesta del bloque Map16 con byte bajo [blockLo], o [NO_SLOPE]. */
    fun shapeForBlockLo(blockLo: Int): Int =
        if (blockLo in 0x6E..0xD7) BLOCK_TO_SHAPE[blockLo - 0x6E] else NO_SLOPE

    /** true si la forma es de TECHO (alturas negativas): se trata como bloque macizo. */
    fun isCeiling(shape: Int): Boolean {
        if (shape !in 0..31) return false
        for (x in 0 until 16) if (SHAPES[shape * 16 + x] >= 0xF0) return true
        return false
    }

    /** true si [shape] es una cuesta de SUELO utilizable por el motor. */
    fun isFloorShape(shape: Int): Boolean = shape in 0..31 && !isCeiling(shape)

    /**
     * Offset del SUELO desde el borde superior del bloque en la columna de píxel
     * [x] (0..15) de la forma [shape]: 0 = arriba del todo, 16 = sin suelo en esa
     * columna (el valor 0x10 de las formas de medio borde).
     */
    fun floorOffset(shape: Int, x: Int): Int =
        SHAPES[shape * 16 + x.coerceIn(0, 15)].coerceAtMost(16)

    /**
     * Pendiente media de la forma (positiva = el suelo BAJA hacia la derecha, o sea,
     * cuesta abajo es +X), en pixeles de caida por pixel horizontal.
     */
    fun gradient(shape: Int): Float =
        (floorOffset(shape, 15) - floorOffset(shape, 0)) / 15f

    /**
     * PERFIL de la forma: las 16 alturas de suelo (offset desde arriba, 0..16) de la
     * forma [shape], o null si no es una forma de suelo utilizable. Es el formato
     * GENERAL que consume el motor ([slopeOffsetsAt]): la ROM y el editor dan formas
     * de esta tabla, y cualquier otra fuente (p. ej. el perfil deducido del DIBUJO de
     * un tile) puede dar el suyo propio.
     */
    fun floorOffsets(shape: Int): IntArray? {
        if (!isFloorShape(shape)) return null
        return IntArray(16) { floorOffset(shape, it) }
    }

    /**
     * Perfil de rampa deducido del DIBUJO de un tile de 16×16 ([pixels] ARGB por
     * filas): el primer píxel OPACO (alfa > 0x7F) de cada columna marca la altura del
     * suelo. Es lo que hace GENERAL el sistema: cualquier tile marcado "cuesta" sin
     * forma explícita se juega como rampa según su propia silueta — niveles ya
     * importados incluidos, sin reimportar. Devuelve null si la silueta no es una
     * rampa útil (columna llena arriba del todo en TODAS las columnas = bloque
     * macizo, o vacía del todo).
     */
    fun profileFromTilePixels(pixels: IntArray): IntArray? {
        if (pixels.size < 256) return null
        val off = IntArray(16) { 16 }
        for (x in 0 until 16) {
            for (y in 0 until 16) {
                if ((pixels[y * 16 + x] ushr 24) > 0x7F) { off[x] = y; break }
            }
        }
        val any = off.any { it < 16 }
        val allTop = off.all { it == 0 }
        val flat = off.distinct().size == 1
        // Sin silueta útil: vacío, bloque macizo (todo arriba) o franja plana.
        if (!any || allTop || flat) return null
        return off
    }
}
