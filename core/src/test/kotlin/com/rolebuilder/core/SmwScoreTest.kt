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
    fun `el bonus de tiempo es el reloj por 50`() {
        assertEquals(50, SmwScore.TIME_BONUS_PER_UNIT)
        assertEquals(0, SmwScore.timeBonus(0))
        assertEquals(5_000, SmwScore.timeBonus(100))
        assertEquals(19_950, SmwScore.timeBonus(399), "el reloj típico de SMW")
        assertEquals(0, SmwScore.timeBonus(-5), "un reloj negativo no regala puntos")
    }

    @Test
    fun `el recuento pasa de 100 en 100 y afina al final`() {
        assertEquals(100, SmwScore.tallyStep(5_000))
        assertEquals(100, SmwScore.tallyStep(200))
        // Justo en 0x63 el original restaría 100 de 99 y se le daría la vuelta al
        // contador. No pasa nunca (el bonus es tiempo × 50, múltiplo de 10), pero aquí
        // se pasa lo que queda en vez de desbordar.
        assertEquals(99, SmwScore.tallyStep(0x63), "no se pasa de lo que queda")
        assertEquals(10, SmwScore.tallyStep(0x62), "por debajo de 0x63 va de 10 en 10")
        assertEquals(10, SmwScore.tallyStep(10))
        // Nunca pasa más de lo que queda ni se va por debajo de cero.
        assertEquals(5, SmwScore.tallyStep(5))
        assertEquals(0, SmwScore.tallyStep(0))
        // Y el recuento entero acaba dando EXACTAMENTE el bonus, sin perder ni un punto.
        var pend = SmwScore.timeBonus(399)
        var dado = 0
        var guard = 0
        while (pend > 0 && guard++ < 10_000) {
            val paso = SmwScore.tallyStep(pend); pend -= paso; dado += paso
        }
        assertEquals(SmwScore.timeBonus(399), dado, "el recuento debe cuadrar")
        assertEquals(0, pend)
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
