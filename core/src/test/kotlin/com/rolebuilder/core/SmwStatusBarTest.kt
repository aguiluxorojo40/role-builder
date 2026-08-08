package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwStatusBar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests del HUD real de SMW. Son ROM-free: comprueban los DATOS portados
 * (`InitializeStatusBarTilemap` / `UpdateStatusBarCounters`), que es lo que evita volver a
 * inventarse la barra de estado.
 */
class SmwStatusBarTest {

    @Test
    fun `la plantilla tiene las 59 celdas del juego`() {
        // kStatusBarTilemap_SecondRow son 118 bytes = 59 pares (tesela, atributos).
        assertEquals(118, SmwStatusBar.SECOND_ROW.size)
        assertEquals(59, SmwStatusBar.CELL_COUNT)
        assertEquals(8, SmwStatusBar.TOP_ROW.size)
        assertEquals(8, SmwStatusBar.BOTTOM_ROW.size)
    }

    @Test
    fun `la primera celda es la M de MARIO y el marco son las 3A-3B`() {
        // Las 5 primeras celdas (0x30..0x34) son el texto fijo; el marco usa 0x3A/0x3B.
        assertEquals(0x30, SmwStatusBar.tileAt(0))
        assertEquals(0x34, SmwStatusBar.tileAt(4))
        assertEquals(0x3A, SmwStatusBar.TOP_ROW[0])
        assertEquals(0x3B, SmwStatusBar.TOP_ROW[2])
    }

    @Test
    fun `el reloj borra los ceros de la izquierda, como el juego`() {
        val m = SmwStatusBar.buildTilemap(time = 39, coins = 0, lives = 0, score = 0)
        val t = SmwStatusBar.Field.TIME
        assertEquals(SmwStatusBar.BLANK, m[t[0]], "las centenas de 39 van en blanco, no '0'")
        assertEquals(3, m[t[1]])
        assertEquals(9, m[t[2]])
    }

    @Test
    fun `el reloj de tres digitos se escribe entero`() {
        val m = SmwStatusBar.buildTilemap(time = 394, coins = 0, lives = 0, score = 0)
        val t = SmwStatusBar.Field.TIME
        assertEquals(3, m[t[0]]); assertEquals(9, m[t[1]]); assertEquals(4, m[t[2]])
    }

    @Test
    fun `las vidas se muestran sumando una, como en SMW`() {
        // Con 0 vidas restantes el juego pone "1" (el contador es vidas+1).
        val m = SmwStatusBar.buildTilemap(time = 0, coins = 0, lives = 0, score = 0)
        val l = SmwStatusBar.Field.LIVES
        assertEquals(SmwStatusBar.BLANK, m[l[0]], "la decena en blanco")
        assertEquals(1, m[l[1]])
    }

    @Test
    fun `las monedas ocupan dos digitos con la decena en blanco si es cero`() {
        val m = SmwStatusBar.buildTilemap(time = 0, coins = 7, lives = 0, score = 0)
        val c = SmwStatusBar.Field.COINS
        assertEquals(SmwStatusBar.BLANK, m[c[0]])
        assertEquals(7, m[c[1]])
    }

    @Test
    fun `la puntuacion usa seis digitos`() {
        val m = SmwStatusBar.buildTilemap(time = 0, coins = 0, lives = 0, score = 12345)
        val s = SmwStatusBar.Field.SCORE
        assertEquals(SmwStatusBar.BLANK, m[s[0]], "el cero de la izquierda se borra")
        assertEquals(intArrayOf(1, 2, 3, 4, 5).toList(), s.drop(1).map { m[it] })
    }

    @Test
    fun `el reloj corre a 40 fotogramas por unidad`() {
        // misc_status_bar_tilemap[55] = 40: a 60 fps una unidad dura 2/3 s, que es por lo que
        // un nivel de 400 dura unos 4 minutos y medio y no casi 7.
        assertEquals(40, SmwStatusBar.TIMER_FRAMES_PER_UNIT)
        val m = SmwStatusBar.buildTilemap(time = 400, coins = 0, lives = 0, score = 0)
        assertEquals(40, m[SmwStatusBar.Field.TIMER_TICK])
    }

    @Test
    fun `los contadores no se pisan entre si`() {
        // Si dos campos compartieran posición, un contador borraría al otro en pantalla.
        val f = SmwStatusBar.Field
        val todas = f.COINS + f.LIVES + f.TIME + f.SCORE + f.BONUS_STARS_TOP + f.BONUS_STARS_BOTTOM
        assertEquals(todas.size, todas.toSet().size, "hay posiciones repetidas entre contadores")
        assertTrue(f.TIMER_TICK !in todas, "el contador de frames no debe pisar un dígito")
    }
}
