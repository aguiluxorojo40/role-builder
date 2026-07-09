package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests SINTÉTICOS del parser de objetos de Layer 1 (port de snesrev/smw): plantamos
 * un nivel mínimo en una ROM vacía y comprobamos que los objetos rellenan el buffer
 * Map16 exactamente como el juego (suelo = bloque 0x100 + tierra 0x3F, tubería,
 * salto de pantalla). Sin ROM con copyright. La validación contra la ROM real se
 * hizo aparte: 831/832 celdas idénticas al buffer $7EC800 de un emulador.
 */
class SmwLayer1Test {

    /** ROM sintética con un nivel en 0x10000 y su puntero en la tabla de Layer 1. */
    private fun romWithLevel(vararg data: Int): ByteArray {
        val rom = ByteArray(0x70000)
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC] = 0x00
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 1] = 0x80.toByte()
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 2] = 0x02
        data.forEachIndexed { i, b -> rom[0x10000 + i] = b.toByte() }
        return rom
    }

    @Test
    fun `un ledge estandar escribe suelo 0x100 y tierra 0x3F`() {
        // Cabecera: modo 0 (horizontal), tileset 0. Objeto = (b1>>4) | ((b0&0x60)>>1).
        // Obj 0x14 (standard ledge): b0 bit5 (0x20) + b1 hi = 4. Posición: b0 low =
        // fila 1, b1 low = col 2. size 0x23: ancho k=3 (4 bloques), tierra r2=2 filas.
        val rom = romWithLevel(
            0x00, 0x00, 0x00, 0x00, 0x00,  // cabecera (5 bytes)
            0x21, 0x42, 0x23,               // objeto: ledge en fila1,col2, size 0x23
            0xFF,                           // fin
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(1, tm.totalObjects)
        assertEquals(0, tm.unknownObjects)
        // Fila 1: bloques de suelo (0x100) en cols 2..5 (k=3 → 4 bloques).
        for (c in 2..5) assertEquals(0x100, tm.block(c, 1), "suelo en col $c")
        // Filas 2-3: tierra 0x3F (r2=2 filas).
        for (c in 2..5) {
            assertEquals(0x3F, tm.block(c, 2), "tierra fila2 col $c")
            assertEquals(0x3F, tm.block(c, 3), "tierra fila3 col $c")
        }
        // Fuera del objeto: aire 0x25.
        assertEquals(0x25, tm.block(0, 0))
        assertEquals(0x25, tm.block(6, 1))
    }

    @Test
    fun `el salto de pantalla (ext 0x01) mueve el cursor de pantalla`() {
        // Ext obj: b0 con obj=0 y screen jump a pantalla 2, luego un ledge en esa pantalla.
        val rom = romWithLevel(
            0x00, 0x00, 0x00, 0x00, 0x00,
            0x02, 0x00, 0x01,               // ext 0x01: screen jump a pantalla 2 (b0&0x1F)
            0x20, 0x42, 0x11,               // ledge obj 0x14 en fila0,col2 de pantalla 2
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        // Pantalla 2 → columnas absolutas 32+. Suelo en col 34 (2+32), fila 0.
        assertEquals(0x100, tm.block(34, 0))
        assertEquals(0x25, tm.block(2, 0), "la pantalla 0 queda vacía")
    }

    @Test
    fun `cuesta invertida normal-izquierda (subtipo 6) se dibuja, ya no cuenta como desconocida`() {
        // Obj 0x12 (slopes) con size 0x16: subtipo 6 (UpsideDownNormalLeft), altura 1.
        // objNum 0x12 = (h1>>4=2) | ((h0&0x60)>>1=0x10). pos 0x44 (fila 4, col 4).
        // h0=0x24 (bit5 puesto, sin screen/página, low=4), h1=0x24 (hi=2, low=4).
        val rom = romWithLevel(
            0x00, 0x00, 0x00, 0x00, 0x00,  // cabecera modo 0, tileset 0
            0x24, 0x24, 0x16,               // obj 0x12 subtipo 6, altura 1, en fila4,col4
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(1, tm.totalObjects)
        assertEquals(0, tm.unknownObjects, "la cuesta invertida ya está portada")
        // Fila superior del techo (bloques de página 1): 0xEE, 0xF0 en cols 4,5.
        assertEquals(0x1EE, tm.block(4, 4))
        assertEquals(0x1F0, tm.block(5, 4))
        // Fila de debajo (relleno interior): 0xC6, 0xC7 en cols 4,5.
        assertEquals(0x1C6, tm.block(4, 5))
        assertEquals(0x1C7, tm.block(5, 5))
    }

    @Test
    fun `nivel vertical devuelve null (no soportado aun)`() {
        // Modo 10 (0x0A) es vertical según la VerticalTable.
        val rom = romWithLevel(0x00, 0x0A, 0x00, 0x00, 0x00, 0xFF)
        assertNull(SmwLayer1.parse(rom, 0, 0))
    }

    @Test
    fun `objetos desconocidos se cuentan sin romper el parse`() {
        val rom = romWithLevel(
            0x00, 0x00, 0x00, 0x00, 0x00,
            0x40, 0xE0, 0x00,               // obj 0x0E+? → obj=(0xE)|(2<<4 de b0&0x60>>1)=0x2E: no portado
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(1, tm.totalObjects)
        assertTrue(tm.unknownObjects >= 1)
    }
}
