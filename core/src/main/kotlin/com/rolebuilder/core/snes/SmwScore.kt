package com.rolebuilder.core.snes

/**
 * PUNTUACIÓN y BONUS de Super Mario World: los valores reales del juego, no inventados.
 *
 * ## Los puntos
 *
 * El juego no suma puntos directamente: suelta un "sprite de puntuación" con un id, y al
 * resolverse ese id se traduce a puntos con las tablas `kProcessScoreSprites_PointMultiplier`
 * Lo/Hi ($02:AC..). El multiplicador de 16 bits se muestra ×10, que es de donde salen los
 * valores clásicos: 10, 20, 40, 80, 100, 200, 400, 800, 1000, 2000, 4000 y 8000.
 *
 * ## La cinta de meta
 *
 * La cinta NO está quieta: `Spr07B_GoalTape` ($01:C098) le pone velocidad vertical
 * ±[TAPE_SPEED_SUBPX] (tabla `kSpr07B_GoalTape_YSpeed` = `{0x10, 0xF0}`, o sea ±1 px por
 * fotograma) y le da la vuelta cada [TAPE_HALF_PERIOD] fotogramas. Cuanto MÁS ALTA la
 * pilles, más bonus:
 *
 * ```c
 * spr_table1594[k] = spr_table1528[k] - spr_ypos_lo[k];        // cuánto ha subido
 * counter_bonus_stars_earned = kSpr07B_GoalTape_BonusStarsEarned[spr_table1594[k] >> 2];
 * ```
 *
 * Esa tabla ([BONUS_STARS]) va en BCD: `0x25` significa 25 estrellas, no 37. Con el máximo
 * (`0x50` = 50 estrellas) el juego además llama a `GivePoints`.
 */
object SmwScore {

    /**
     * Puntos por id de sprite de puntuación (1..12). Es el multiplicador de las tablas
     * `kProcessScoreSprites_PointMultiplierLo/Hi` ya multiplicado por 10.
     */
    val POINT_VALUES: Map<Int, Int> = mapOf(
        1 to 10, 2 to 20, 3 to 40, 4 to 80,
        5 to 100, 6 to 200, 7 to 400, 8 to 800,
        9 to 1_000, 10 to 2_000, 11 to 4_000, 12 to 8_000,
    )

    /** Puntos de un id de sprite de puntuación, o 0 si ese id no da puntos (da vidas). */
    fun pointsOf(scoreSpriteId: Int): Int = POINT_VALUES[scoreSpriteId] ?: 0

    /**
     * `kSpr07B_GoalTape_BonusStarsEarned` ($07:F2..), 32 entradas **en BCD**: 1..9, luego
     * 10..19, 20..29 y por último 30, 40 y 50.
     */
    val BONUS_STARS_BCD: IntArray = intArrayOf(
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,
        0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29,
        0x30, 0x40, 0x50,
    )

    /** Pasa un byte BCD a su valor decimal (0x25 → 25). */
    fun fromBcd(v: Int): Int = (v shr 4) * 10 + (v and 0xF)

    /** Velocidad vertical de la cinta, en subpíxeles/16 (0x10 = 1 px por fotograma). */
    const val TAPE_SPEED_SUBPX = 0x10

    /** Fotogramas que tarda la cinta en dar la vuelta (`spr_decrementing_table1540 = 124`). */
    const val TAPE_HALF_PERIOD = 124

    /** Subida máxima que la tabla de bonus sabe puntuar (32 entradas × 4 px). */
    const val TAPE_MAX_RISE_PX = 127

    /**
     * ESTRELLAS DE BONUS por tocar la cinta habiendo subido [risePx] píxeles sobre su punto
     * más bajo. Devuelve ya el número decimal (la tabla del juego está en BCD).
     */
    fun bonusStars(risePx: Int): Int {
        val idx = (risePx.coerceIn(0, TAPE_MAX_RISE_PX)) shr 2
        return fromBcd(BONUS_STARS_BCD[idx.coerceIn(0, BONUS_STARS_BCD.lastIndex)])
    }

    /** El máximo que se puede sacar (50): con él el juego da premio extra. */
    val MAX_BONUS_STARS: Int = fromBcd(BONUS_STARS_BCD.last())
}
