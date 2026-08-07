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

    // ---- Cola de objetos sin portar (medida con la ROM: 443 → 451 niveles al 100%) ----
    // Encoding: objNum = (h1>>4) | ((h0&0x60)>>1); pos = (h0&0xF)<<4 | (h1&0xF).
    // Un objeto EXTENDIDO es objNum==0, y entonces el 3er byte es su id.

    @Test
    fun `ext 0x97 pinta la tesela de borde del palacio en pagina 1`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x03, 0x05, 0x97,               // ext 0x97 en fila 3, col 5
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x110, tm.block(5, 3), "tesela 0x10 de PÁGINA 1")
        assertEquals(0x25, tm.block(6, 3), "no desborda a la derecha")
    }

    @Test
    fun `ext 0x8A pinta el interruptor verde 2x2`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x02, 0x04, 0x8A,               // ext 0x8A en fila 2, col 4
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0xEC, tm.block(4, 2)); assertEquals(0xED, tm.block(5, 2))
        assertEquals(0xEE, tm.block(4, 3)); assertEquals(0xEF, tm.block(5, 3))
        // Los otros tres interruptores salen del mismo grupo de 4 teselas.
        val rojo = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x00, 0x02, 0x04, 0x8D, 0xFF), 0, 0,
        )
        assertNotNull(rojo)
        assertEquals(0xF8, rojo.block(4, 2)); assertEquals(0xFB, rojo.block(5, 3))
    }

    @Test
    fun `ext 0x62 pinta una telarana 3x3 del grupo del reloj`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x04,   // tileset 4 (casa fantasma)
            0x01, 0x02, 0x62,               // ext 0x62 en fila 1, col 2
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x86, tm.block(2, 1)); assertEquals(0x87, tm.block(3, 1))
        assertEquals(0x86, tm.block(3, 2)); assertEquals(0x87, tm.block(4, 2))
        assertEquals(0x86, tm.block(4, 3))
        assertEquals(0x25, tm.block(4, 1), "el 0x25 de la tabla es aire")
    }

    @Test
    fun `ext 0x85 pinta la casa de Yoshi de 16x10`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x85,               // ext 0x85 en fila 0, col 0
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Fila 0: el tejadillo empieza en la col 11.
        assertEquals(0x25, tm.block(0, 0)); assertEquals(0xCB, tm.block(11, 0))
        assertEquals(0xCC, tm.block(12, 0))
        // Fila 1: alero 0xCD/0xCE, cuerpo 0xCF y remates 0xD0/0xD1.
        assertEquals(0xCD, tm.block(1, 1)); assertEquals(0xCF, tm.block(5, 1))
        assertEquals(0xD0, tm.block(13, 1)); assertEquals(0xD1, tm.block(14, 1))
        // Fila 9 (la última): la 16ª tesela se escribe SIN avanzar, así que existe.
        assertEquals(0xE6, tm.block(2, 9)); assertEquals(0xE9, tm.block(13, 9))
        assertEquals(0xE5, tm.block(15, 9), "la columna 15 de la última fila")
        assertEquals(0x25, tm.block(0, 10), "10 filas, ni una más")
    }

    @Test
    fun `casa fantasma 0x2E pinta una linea de pinchos de pagina 1`() {
        // objNum 0x2E = (h1>>4=0xE) | ((h0&0x60)>>1=0x20) → h0 bit6, h1 hi=0xE.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x05,   // tileset 5 (casa fantasma)
            0x42, 0xE3, 0x03,               // obj 0x2E en fila 2, col 3, ancho 3+1
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        for (c in 3..6) assertEquals(0x159, tm.block(c, 2), "pincho en col $c")
        assertEquals(0x25, tm.block(7, 2))
    }

    @Test
    fun `casa fantasma 0x30 pinta repisa de hierba con relleno debajo`() {
        // objNum 0x30 = (h1>>4=0) | ((h0&0x60)>>1=0x30) → h0 bits 5+6.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x05,
            0x61, 0x02, 0x21,               // obj 0x30 en fila 1, col 2; ancho 2, alto 2
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x10F, tm.block(2, 1)); assertEquals(0x10F, tm.block(3, 1))
        for (r in 2..3) {
            assertEquals(0xEA, tm.block(2, r), "relleno fila $r")
            assertEquals(0xEA, tm.block(3, r), "relleno fila $r")
        }
        assertEquals(0x25, tm.block(2, 4), "no baja más de lo pedido")
    }

    @Test
    fun `pradera 0x30 pinta la tuberia helada de dos columnas`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,   // tileset 0 (pradera)
            0x61, 0x03, 0x20,               // obj 0x30 en fila 1, col 3; alto 2
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x161, tm.block(3, 1)); assertEquals(0x162, tm.block(4, 1))
        for (r in 2..3) {
            assertEquals(0x163, tm.block(3, r)); assertEquals(0x164, tm.block(4, r))
        }
        assertEquals(0x25, tm.block(3, 4))
    }

    @Test
    fun `pradera 0x31 pinta bloques giratorios helados`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x61, 0x12, 0x01,               // obj 0x31 en fila 1, col 2; ancho 2, alto 1
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x165, tm.block(2, 1), "tesela 0x65 de PÁGINA 1")
        assertEquals(0x165, tm.block(3, 1))
        assertEquals(0x25, tm.block(4, 1))
    }

    // ---- Segunda tanda: lo que más niveles bloqueaba (154 → 183 al 100%) ----

    @Test
    fun `bloques de interruptor sin pulsar salen en pagina 0`() {
        // objNum 0x32 = (h1>>4=2) | ((h0&0x60)>>1=0x30). size 0x11 → 2x2.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,   // pradera
            0x61, 0x22, 0x11,
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        for (c in 2..3) for (r in 1..2) assertEquals(0x6C, tm.block(c, r), "azul en ($c,$r)")
        // Los ROJOS son la misma rutina con la otra tesela (castillo 0x3A).
        val rojo = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x01, 0x61, 0xA2, 0x00, 0xFF), 0, 0,
        )
        assertNotNull(rojo)
        assertEquals(0, rojo.unknownObjects)
        assertEquals(0x6D, rojo.block(2, 1))
    }

    @Test
    fun `pinchos de castillo horizontales y verticales`() {
        // 0x3E: nibble alto elige tesela (0 → 0x5A), nibble bajo el ancho.
        val h = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x01, 0x62, 0xE3, 0x03, 0xFF), 0, 0,
        )
        assertNotNull(h)
        assertEquals(0, h.unknownObjects)
        for (c in 3..6) assertEquals(0x15A, h.block(c, 2), "pincho H en col $c")
        assertEquals(0x25, h.block(7, 2))
        // Variante 1 → la otra tesela.
        val h1 = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x01, 0x62, 0xE3, 0x13, 0xFF), 0, 0,
        )
        assertNotNull(h1)
        assertEquals(0x159, h1.block(3, 2))
        // 0x3F: aquí es al revés, el nibble BAJO elige tesela y el alto el largo.
        val v = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x01, 0x61, 0xF3, 0x20, 0xFF), 0, 0,
        )
        assertNotNull(v)
        assertEquals(0, v.unknownObjects)
        for (r in 1..3) assertEquals(0x15B, v.block(3, r), "pincho V en fila $r")
        assertEquals(0x25, v.block(3, 4))
    }

    @Test
    fun `suelo de bosque copa en pagina 1 y relleno debajo`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x61, 0x52, 0x11,               // obj 0x35, ancho 2, alto 1
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        for (c in 2..3) {
            assertEquals(0x10E, tm.block(c, 1), "copa en col $c")
            assertEquals(0xB8, tm.block(c, 2), "relleno en col $c")
        }
        assertEquals(0x25, tm.block(2, 3))
    }

    @Test
    fun `bordes del suelo de bosque bajan una columna`() {
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x61, 0x43, 0x20,               // obj 0x34, variante 0, alto 2
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x15F, tm.block(3, 1), "remate")
        assertEquals(0x160, tm.block(3, 2)); assertEquals(0x160, tm.block(3, 3))
        assertEquals(0x25, tm.block(3, 4))
    }

    @Test
    fun `la tuberia vertical INVISIBLE (tipo 5) ya no cuenta como desconocida`() {
        // objNum 0x0F = (h1>>4=0xF) | ((h0&0x60)>>1=0). size 0x15: alto 1, tipo 5.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x01, 0xF2, 0x15,
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects, "el tipo 5 SÍ se sabe dibujar")
        for (r in 1..2) {
            assertEquals(0x168, tm.block(2, r), "izquierda fila $r")
            assertEquals(0x169, tm.block(3, r), "derecha fila $r")
        }
        // Del 6 en adelante no hay dato: sigue siendo desconocido, no basura.
        val seis = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x00, 0x01, 0xF2, 0x16, 0xFF), 0, 0,
        )
        assertNotNull(seis)
        assertEquals(1, seis.unknownObjects)
    }

    @Test
    fun `la cuesta muy inclinada de cueva baja dos filas por columna`() {
        // objNum 0x3C = (h1>>4=0xC) | ((h0&0x60)>>1=0x30). size 0x01: lado IZQUIERDO
        // (bit 0x10 a 0), ancho 1 → dos columnas.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x03,   // tileset 3 (cueva)
            0x60, 0xC5, 0x01,               // obj 0x3C en fila 0, col 5
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Columna 1: las tres teselas de cuesta (página 1) y 2 filas de tierra.
        assertEquals(0x1CA, tm.block(5, 0)); assertEquals(0x1CB, tm.block(5, 1))
        assertEquals(0x1F1, tm.block(5, 2))
        assertEquals(0x3F, tm.block(5, 3)); assertEquals(0x3F, tm.block(5, 4))
        // Columna 2: DOS filas más abajo y UNA a la izquierda; ya sin tierra.
        assertEquals(0x1CA, tm.block(4, 2)); assertEquals(0x1CB, tm.block(4, 3))
        assertEquals(0x1F1, tm.block(4, 4))
        assertEquals(0x25, tm.block(4, 5), "la última columna ya no lleva tierra")
    }

    @Test
    fun `remates y circulos de guia de linea`() {
        // 0x55: el remate de la guía HORIZONTAL apila sus dos teselas en VERTICAL.
        val h = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x02, 0x02, 0x04, 0x55, 0xFF), 0, 0,
        )
        assertNotNull(h)
        assertEquals(0, h.unknownObjects)
        assertEquals(0x96, h.block(4, 2)); assertEquals(0x97, h.block(4, 3))
        // 0x56: y el de la VERTICAL las pone en horizontal.
        val v = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x02, 0x02, 0x04, 0x56, 0xFF), 0, 0,
        )
        assertNotNull(v)
        assertEquals(0x98, v.block(4, 2)); assertEquals(0x99, v.block(5, 2))
        // 0x4D: cuarto de círculo grande, 2×2.
        val c = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x02, 0x01, 0x03, 0x4D, 0xFF), 0, 0,
        )
        assertNotNull(c)
        assertEquals(0, c.unknownObjects)
        assertEquals(0x7A, c.block(3, 1)); assertEquals(0x7B, c.block(4, 1))
        assertEquals(0x7C, c.block(3, 2)); assertEquals(0x25, c.block(4, 2), "el 0x25 es aire")
        // 0x51: cuarto de círculo pequeño, una sola tesela.
        val p = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x02, 0x01, 0x03, 0x51, 0xFF), 0, 0,
        )
        assertNotNull(p)
        assertEquals(0x76, p.block(3, 1))
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
