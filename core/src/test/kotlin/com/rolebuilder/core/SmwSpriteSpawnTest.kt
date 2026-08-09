package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwSpriteSpawn
import com.rolebuilder.core.snes.SmwSpriteSpawn.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El id que hay ESCRITO en la lista de sprites no es el enemigo que acaba habiendo en el
 * nivel. Aquí se comprueba la clasificación de `LoadOneSprite` ($02:AA4D), que es la que
 * lo traduce. No hace falta ROM: es aritmética de la rutina.
 */
class SmwSpriteSpawnTest {

    @Test
    fun `el tramo normal pasa tal cual y con estado 1`() {
        for (raw in listOf(0x00, 0x01, 0x05, 0x7B, 0xAB, 0xC8)) {
            val s = SmwSpriteSpawn.of(raw)
            assertEquals(Kind.NORMAL, s.kind, "id %02X".format(raw))
            assertEquals(raw, s.id, "id %02X no debe traducirse".format(raw))
            assertEquals(SmwSpriteSpawn.STATUS_NORMAL, s.status)
            assertFalse(s.stunned)
        }
    }

    @Test
    fun `los 0xDA-0xDD son Koopas YA dentro de su caparazon`() {
        // Es el hallazgo que explicaba los "ids sin gráfico" 0xDA y 0xDB de YOSHI'S
        // ISLAND 1 y 2: no son sprites raros, son caparazones sueltos por el suelo.
        val esperado = mapOf(0xDA to 0x04, 0xDB to 0x05, 0xDC to 0x06, 0xDD to 0x07)
        for ((raw, id) in esperado) {
            val s = SmwSpriteSpawn.of(raw)
            assertEquals(Kind.NORMAL, s.kind, "%02X sí es un enemigo".format(raw))
            assertEquals(id, s.id, "%02X → Koopa %02X".format(raw, id))
            assertEquals(SmwSpriteSpawn.STATUS_STUNNED, s.status)
            assertTrue(s.stunned)
        }
    }

    @Test
    fun `lanzadores, generadores y scroll de capas NO son enemigos`() {
        assertEquals(Kind.SHOOTER, SmwSpriteSpawn.of(0xC9).kind)
        assertEquals(Kind.SHOOTER, SmwSpriteSpawn.of(0xCA).kind)
        assertEquals(Kind.GENERATOR, SmwSpriteSpawn.of(0xCB).kind)
        assertEquals(Kind.GENERATOR, SmwSpriteSpawn.of(0xD9).kind)
        assertEquals(Kind.LAYER_SCROLL, SmwSpriteSpawn.of(0xE7).kind)
        assertEquals(Kind.LAYER_SCROLL, SmwSpriteSpawn.of(0xFF).kind)
        for (raw in listOf(0xC9, 0xCA, 0xCB, 0xD9, 0xE7, 0xFF)) {
            assertFalse(SmwSpriteSpawn.of(raw).isEnemy, "%02X no ocupa ranura de enemigo".format(raw))
        }
    }

    @Test
    fun `el 0xDE y el 0xE0 se atrapan ANTES que el tramo de aturdidos`() {
        // Los dos caen dentro de 0xDA-0xE0, pero el original los comprueba antes, así que
        // NO se traducen: son cargas en grupo (cinco Eeries y tres plataformas). Invertir
        // el orden los convertiría en dos Koopas que no existen.
        assertEquals(Kind.GROUP, SmwSpriteSpawn.of(0xDE).kind)
        assertEquals(Kind.GROUP, SmwSpriteSpawn.of(0xE0).kind)
        // El 0xDF, que va entre medias, sí es del tramo aturdido.
        val df = SmwSpriteSpawn.of(0xDF)
        assertEquals(Kind.NORMAL, df.kind)
        assertEquals(0x09, df.id)
        // Y los Boos de techo son grupo, no aturdidos.
        assertEquals(Kind.GROUP, SmwSpriteSpawn.of(0xE1).kind)
        assertEquals(Kind.GROUP, SmwSpriteSpawn.of(0xE6).kind)
    }

    @Test
    fun `la frontera exacta entre normal, lanzador y generador`() {
        assertEquals(Kind.NORMAL, SmwSpriteSpawn.of(0xC8).kind, "0xC8 es el último normal")
        assertEquals(Kind.SHOOTER, SmwSpriteSpawn.of(0xC9).kind, "0xC9 ya es lanzador")
        assertEquals(Kind.GENERATOR, SmwSpriteSpawn.of(0xCB).kind, "0xCB ya es generador")
        assertEquals(Kind.NORMAL, SmwSpriteSpawn.of(0xDA).kind, "0xDA vuelve a ser enemigo")
    }
}
