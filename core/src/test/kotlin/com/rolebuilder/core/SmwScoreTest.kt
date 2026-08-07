package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Puntuación y bonus de meta con los valores REALES de SMW (tablas del banco $02/$07). */
class SmwScoreTest {

    @Test
    fun `los puntos son los clasicos de SMW`() {
        // El multiplicador de kProcessScoreSprites_PointMultiplier ×10.
        assertEquals(listOf(10, 20, 40, 80, 100, 200, 400, 800, 1000, 2000, 4000, 8000),
            (1..12).map { SmwScore.pointsOf(it) })
        // Los ids que NO dan puntos (dan vidas) valen 0, no basura.
        assertEquals(0, SmwScore.pointsOf(15))
        assertEquals(0, SmwScore.pointsOf(0))
    }

    @Test
    fun `la tabla de bonus esta en BCD y hay que leerla como tal`() {
        // 0x25 son VEINTICINCO estrellas, no 37: es BCD, no hexadecimal.
        assertEquals(25, SmwScore.fromBcd(0x25))
        assertEquals(50, SmwScore.fromBcd(0x50))
        assertEquals(32, SmwScore.BONUS_STARS_BCD.size, "la tabla del juego tiene 32 entradas")
    }

    @Test
    fun `cuanto mas alta la cinta, mas bonus`() {
        // Tocarla abajo del todo da lo mínimo; arriba del todo, el máximo.
        assertEquals(1, SmwScore.bonusStars(0))
        assertEquals(50, SmwScore.bonusStars(SmwScore.TAPE_MAX_RISE_PX))
        assertEquals(50, SmwScore.MAX_BONUS_STARS)
        // El índice avanza cada 4 px (>> 2), así que 0..3 px dan lo mismo.
        assertEquals(SmwScore.bonusStars(0), SmwScore.bonusStars(3))
        assertTrue(SmwScore.bonusStars(4) > SmwScore.bonusStars(3))
        // Y nunca se sale de la tabla por mucho que se pase.
        assertEquals(50, SmwScore.bonusStars(9999))
        assertEquals(1, SmwScore.bonusStars(-50))
        // Monótona: subir nunca da menos.
        val serie = (0..SmwScore.TAPE_MAX_RISE_PX).map { SmwScore.bonusStars(it) }
        assertTrue(serie.zipWithNext().all { (a, b) -> b >= a }, "el bonus no debe bajar al subir")
    }
}
