package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
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
    fun `la copa del arbol de bosque es un mural de 16x6`() {
        // objNum 0x33 = (h1>>4=3) | ((h0&0x60)>>1=0x30). size 0 → una sola copa.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x60, 0x30, 0x00,               // obj 0x33 en fila 0, col 0
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Filas 0-1: relleno macizo de copa.
        for (c in 0..15) assertEquals(0xB4, tm.block(c, 0), "relleno en col $c")
        // Fila 2: el relleno acaba en la col 12 y empiezan los bordes.
        assertEquals(0xB4, tm.block(11, 2))
        assertEquals(0xB5, tm.block(12, 2)); assertEquals(0xB3, tm.block(13, 2))
        // Fila 5: el borde inferior, con huecos de aire entre las hojas.
        assertEquals(0x25, tm.block(0, 5))
        assertEquals(0xB1, tm.block(1, 5)); assertEquals(0xB6, tm.block(2, 5))
        assertEquals(0x25, tm.block(0, 6), "6 filas, ni una más")
    }

    @Test
    fun `el tronco grande atraviesa la copa en vez de taparla`() {
        // Primero suelo de bosque (0x35) de 2 de ancho, que deja teselas 0x0E de
        // página 1; encima, el tronco grande (0x36) en la misma casilla.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x00,
            0x61, 0x53, 0x01,               // obj 0x35 en fila 1, col 3, 2 de ancho
            0x61, 0x63, 0x10,               // obj 0x36 en la MISMA casilla
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Sobre la copa (0x0E) el tronco usa la pareja de PÁGINA 1, no la normal.
        assertEquals(0x10B, tm.block(3, 1)); assertEquals(0x10C, tm.block(4, 1))
        // La fila siguiente ya es la pareja corriente del tronco.
        assertEquals(0xBB, tm.block(3, 2)); assertEquals(0xBC, tm.block(4, 2))
        // Y sin copa debajo, el tronco sale con su pareja normal.
        val solo = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x00, 0x61, 0x63, 0x10, 0xFF), 0, 0,
        )
        assertNotNull(solo)
        assertEquals(0xB9, solo.block(3, 1)); assertEquals(0xBA, solo.block(4, 1))
    }

    @Test
    fun `las ramas del arbol son una tesela por lado`() {
        val d = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x00, 0x02, 0x05, 0x88, 0xFF), 0, 0,
        )
        assertNotNull(d)
        assertEquals(0, d.unknownObjects)
        assertEquals(0xC1, d.block(5, 2))
        val i = SmwLayer1.parse(
            romWithLevel(0x03, 0x00, 0x00, 0x00, 0x00, 0x02, 0x05, 0x89, 0xFF), 0, 0,
        )
        assertNotNull(i)
        assertEquals(0xC2, i.block(5, 2))
    }

    @Test
    fun `borde de lava de cueva remata arriba y rellena debajo`() {
        // objNum 0x38 = (h1>>4=8) | ((h0&0x60)>>1=0x30). size 0x20: variante 0, alto 2.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x03,   // tileset 3 (cueva)
            0x61, 0x83, 0x20,
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x15A, tm.block(3, 1), "remate")
        assertEquals(0x15B, tm.block(3, 2)); assertEquals(0x15B, tm.block(3, 3))
        assertEquals(0x25, tm.block(3, 4))
    }

    @Test
    fun `la columna de cuerda lleva planta, capitel y fuste ciclico`() {
        // objNum 0x35 = (h1>>4=5) | ((h0&0x60)>>1=0x30). size 0x30: variante 0, alto 3.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x02,   // tileset 2 (cuerda)
            0x61, 0x53, 0x30,
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Planta arriba (página 0), capitel y luego el fuste, que cicla de pareja.
        assertEquals(0x9A, tm.block(3, 1)); assertEquals(0x9B, tm.block(4, 1))
        assertEquals(0x15F, tm.block(3, 2)); assertEquals(0x160, tm.block(4, 2))
        assertEquals(0x161, tm.block(3, 3)); assertEquals(0x162, tm.block(4, 3))
        assertEquals(0x163, tm.block(3, 4)); assertEquals(0x164, tm.block(4, 4))
    }

    @Test
    fun `la escalera de castillo baja en diagonal con su tierra detras`() {
        // objNum 0x3D = (h1>>4=0xD) | ((h0&0x60)>>1=0x30). size 0x12: lado DERECHO
        // (bit 2), cinta 2, alto 1 → dos peldaños.
        val rom = romWithLevel(
            0x03, 0x00, 0x00, 0x00, 0x01,   // tileset 1 (castillo)
            0x61, 0xD3, 0x12,
            0xFF,
        )
        val tm = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0x1CF, tm.block(3, 1), "cinta del primer peldaño")
        assertEquals(0x1F4, tm.block(3, 2), "esquina")
        assertEquals(0x1CF, tm.block(4, 2), "cinta del segundo, una columna a la derecha")
        assertEquals(0x3F, tm.block(3, 3), "tierra detrás")
        assertEquals(0x1F4, tm.block(4, 3))
    }

    // ------------------------------ NIVELES VERTICALES ------------------------------
    // Modo 0x0A (y el 8) son verticales. Cambian TRES cosas a la vez: la tabla de bases de
    // pantalla (0x200 en vez de 0x1B0), el orden de los nibbles de posición, y hacia dónde
    // crece el nivel. Si solo se cambia una, el nivel sale movido pero "parece" bien.

    @Test
    fun `un nivel vertical ya se parsea, y mide 16 columnas`() {
        // Cabecera: b0 = 1 → 2 pantallas; b1 = 0x0A → modo vertical.
        val tm = SmwLayer1.parse(romWithLevel(0x01, 0x0A, 0x00, 0x00, 0x00, 0xFF), 0, 0)
        assertNotNull(tm)
        assertTrue(tm.vertical, "el modo 0x0A es vertical")
        assertEquals(16, tm.cols, "un nivel vertical mide SIEMPRE 16 columnas")
        assertEquals(2 * 32, tm.rows, "y crece hacia abajo: 32 filas por pantalla")
        assertEquals(32, tm.rowsPerScreen)
    }

    @Test
    fun `en vertical los nibbles de posicion van CRUZADOS`() {
        // Es lo que hace LoadLevelDataObject antes de llamar a la rutina del objeto. En
        // horizontal el nibble bajo del byte 0 es la FILA; en vertical es la COLUMNA.
        // Aquí: byte0 bajo = 3 (columna), byte1 bajo = 5 (fila).
        val tm = SmwLayer1.parse(
            romWithLevel(0x00, 0x0A, 0x00, 0x00, 0x00, 0x23, 0x45, 0x21, 0xFF), 0, 0,
        )
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Suelo en la fila 5, columnas 3 y 4 (tamaño 0x21 → 2 bloques de ancho).
        assertEquals(0x100, tm.block(3, 5)); assertEquals(0x100, tm.block(4, 5))
        // Y tierra debajo, en las filas 6 y 7.
        for (f in 6..7) for (c in 3..4) assertEquals(0x3F, tm.block(c, f), "tierra ($c,$f)")
        // Si los nibbles NO se cruzaran, esto habría caído en la fila 3 / columna 5.
        assertEquals(0x25, tm.block(5, 3), "aire donde caería sin cruzar los nibbles")
    }

    @Test
    fun `la fila 16 de una pantalla vertical salta a la segunda mitad del bloque`() {
        // Cada pantalla vertical son 0x200 bytes: filas 0-15 en la página 0 y 16-31 a
        // partir del byte 256. Colocar en la fila 20 comprueba ese salto.
        val tm = SmwLayer1.parse(
            romWithLevel(0x00, 0x0A, 0x00, 0x00, 0x00, 0x22, 0x4E, 0x01, 0xFF), 0, 0,
        )
        assertNotNull(tm)
        // columna 2, fila 14 (0x0E). El bloque cae dentro de la primera mitad.
        assertEquals(0x100, tm.block(2, 14))
        assertEquals(0x25, tm.block(2, 15), "no se desborda a la fila siguiente")
    }

    @Test
    fun `el bit de pantalla nueva baja una pantalla, no la mueve a la derecha`() {
        // En vertical las pantallas se APILAN. El mismo bit 0x80 que en horizontal lleva
        // el objeto a la pantalla de la derecha, aquí lo lleva 32 filas más abajo.
        val tm = SmwLayer1.parse(
            romWithLevel(0x00, 0x0A, 0x00, 0x00, 0x00, 0xA3, 0x45, 0x21, 0xFF), 0, 0,
        )
        assertNotNull(tm)
        assertEquals(0x100, tm.block(3, 32 + 5), "pantalla 1 = 32 filas más abajo")
        assertEquals(0x25, tm.block(3, 5), "la pantalla 0 queda vacía")
    }

    @Test
    fun `las cuestas de nivel vertical (ext 0x91-0x96) ya se dibujan`() {
        // ExtObj91/93/95 son los únicos objetos que usan el cruce vertical (+0x200).
        // Ext: objNum 0 y el id en el byte de tamaño. Columna 4, fila 6.
        val tm = SmwLayer1.parse(
            romWithLevel(0x00, 0x0A, 0x00, 0x00, 0x00, 0x04, 0x06, 0x91, 0xFF), 0, 0,
        )
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects, "el 0x91 ya está portado")
        // Dos teselas apiladas, las dos de página 1 (son sólidas).
        assertEquals(0x1AA, tm.block(4, 6))
        assertEquals(0x1E2, tm.block(4, 7))
    }

    // --------------------------- LAYER 2 DE OBJETOS ---------------------------
    // 26 niveles de la ROM no llevan fondo de imagen sino geometría hecha con los mismos
    // objetos que el primer plano. Se parsea con la misma máquina, cambiando solo la tabla
    // de disposición y de qué bit sale "es vertical".

    /** ROM con nivel en 0x10000 y flujo de objetos de Layer 2 en 0x11000 (banco 0x02). */
    private fun romConLayer2(mode: Int, vararg objetosL2: Int): ByteArray {
        val rom = romWithLevel(0x00, mode, 0x00, 0x00, 0x00, 0xFF)
        // Puntero de Layer 2: banco 0x02 (≠ 0xFF, o sea OBJETOS) → PC 0x11000.
        rom[SnesGameRecipes.SMW_LAYER2_PTR_PC] = 0x00
        rom[SnesGameRecipes.SMW_LAYER2_PTR_PC + 1] = 0x90.toByte()
        rom[SnesGameRecipes.SMW_LAYER2_PTR_PC + 2] = 0x02
        // Cabecera de 5 bytes del flujo de Layer 2: se SALTA, no se lee.
        val datos = intArrayOf(0, 0, 0, 0, 0) + objetosL2.toTypedArray().toIntArray() + intArrayOf(0xFF)
        datos.forEachIndexed { i, b -> rom[0x11000 + i] = b.toByte() }
        return rom
    }

    @Test
    fun `el Layer 2 de objetos se parsea aparte del Layer 1`() {
        // Modo 1: Layer 1 en las pantallas 0-15 y Layer 2 de la 16 en adelante, del mismo
        // buffer de $7EC800. Un ledge en (col 2, fila 1) del Layer 2.
        val rom = romConLayer2(0x01, 0x21, 0x42, 0x23)
        val l2 = SmwLayer1.parseLayer2(rom, 0, 0)
        assertNotNull(l2)
        assertEquals(1, l2.totalObjects)
        assertEquals(0, l2.unknownObjects)
        for (c in 2..5) assertEquals(0x100, l2.block(c, 1), "suelo de Layer 2 en col $c")
        // Y el Layer 1 del MISMO nivel no ve nada: su flujo está vacío.
        val l1 = SmwLayer1.parse(rom, 0, 0)
        assertNotNull(l1)
        assertEquals(0, l1.totalObjects)
        assertEquals(0x25, l1.block(2, 1), "el Layer 1 sigue vacío")
    }

    @Test
    fun `hay modos que ni siquiera procesan la capa 2`() {
        // BeginLoadingLevelData corta el bucle tras la capa 1 en estos modos. El 0x0A está
        // entre ellos, y por eso los verticales de ese modo llevan fondo de IMAGEN.
        for (mode in intArrayOf(0, 10, 12, 13, 14, 17, 30)) {
            assertNull(SmwLayer1.parseLayer2(romConLayer2(mode, 0x21, 0x42, 0x23), 0, 0),
                "el modo $mode no procesa Layer 2")
        }
        // El 1 y el 2 sí.
        for (mode in intArrayOf(1, 2)) {
            assertNotNull(SmwLayer1.parseLayer2(romConLayer2(mode, 0x21, 0x42, 0x23), 0, 0))
        }
    }

    @Test
    fun `el bit de vertical del Layer 2 es el 1, no el 0`() {
        // Modo 5: flags 0x02 → Layer 1 HORIZONTAL y Layer 2 VERTICAL. Las dos capas del
        // mismo nivel no tienen por qué coincidir, y confundir los bits saldría movido.
        val rom = romConLayer2(0x05, 0x21, 0x42, 0x23)
        assertEquals(false, SmwLayer1.parse(rom, 0, 0)?.vertical, "el Layer 1 del modo 5 es horizontal")
        assertEquals(true, SmwLayer1.parseLayer2(rom, 0, 0)?.vertical, "pero su Layer 2 es vertical")
    }

    @Test
    fun `un fondo de IMAGEN no es Layer 2 de objetos`() {
        // Banco 0xFF en el puntero = tilemap comprimido, no geometría. Hay que decir null
        // para que quien dibuja tire de la vía de imagen.
        val rom = romConLayer2(0x01, 0x21, 0x42, 0x23)
        rom[SnesGameRecipes.SMW_LAYER2_PTR_PC + 2] = 0xFF.toByte()
        assertNull(SmwLayer1.parseLayer2(rom, 0, 0))
    }

    @Test
    fun `el pilar con pinchos del castillo remata arriba o abajo segun el nibble bajo`() {
        // CastleObj36: cuatro columnas, las dos del medio en PÁGINA 1 (los pinchos hacen
        // daño) y los bordes en página 0. Con nibble bajo ≠ 0 el remate va ARRIBA.
        val tm = SmwLayer1.parse(romTileset(1, 0x62, 0x64, 0x11), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects, "el 0x36 de castillo ya está portado")
        // Remate superior en la fila del objeto.
        assertEquals(0x87, tm.block(5, 2)); assertEquals(0x88, tm.block(6, 2))
        // Cuerpo: bordes de página 0 y pinchos de página 1 en medio.
        assertEquals(0x89, tm.block(4, 3))
        assertEquals(0x166, tm.block(5, 3)); assertEquals(0x167, tm.block(6, 3))
        assertEquals(0x8A, tm.block(7, 3))
    }

    @Test
    fun `el muro de roca del castillo es un patron 2x2 repetido`() {
        // CastleObj35: 0x94/0x95 arriba y 0x96/0x97 abajo, todo de página 0 (decorado).
        val tm = SmwLayer1.parse(romTileset(1, 0x62, 0x54, 0x11), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(listOf(0x94, 0x95, 0x94, 0x95), (4..7).map { tm.block(it, 2) })
        assertEquals(listOf(0x96, 0x97, 0x96, 0x97), (4..7).map { tm.block(it, 3) })
    }

    @Test
    fun `la ventana de casa fantasma reusa la rutina del 2x2 entrando por el byte 8`() {
        // ExtObj8F no tiene cuerpo propio: llama a la del 0x64/0x65 con k = 8.
        val tm = SmwLayer1.parse(romTileset(5, 0x02, 0x04, 0x8F), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        assertEquals(0xFC, tm.block(4, 2)); assertEquals(0xFD, tm.block(5, 2))
        assertEquals(0xFE, tm.block(4, 3)); assertEquals(0xFF, tm.block(5, 3))
    }

    @Test
    fun `un modo SIN Layer 1 no trae geometria`() {
        // El modo 9 no tiene entrada en kLevelDataLayoutTables_Layer1LoPtrs (vale 0).
        val tm = SmwLayer1.parse(romWithLevel(0x00, 0x09, 0x00, 0x00, 0x00, 0xFF), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.totalObjects)
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

    // ------------- guías de línea en cuesta y lava en cuesta (RopeObj3A / UndergroundObj39) -------------
    // Estos objetos eran los que más niveles dejaban incompletos (68 y 10 apariciones).
    // Lo delicado no es la tesela, es el PASO DIAGONAL: cada variante baja una fila y se
    // desplaza un número distinto de columnas, así que se comprueban posiciones exactas.

    /** ROM sintética con [tileset] en el byte 4 de la cabecera. */
    private fun romTileset(tileset: Int, vararg obj: Int): ByteArray {
        val cab = intArrayOf(0x00, 0x00, 0x00, 0x00, tileset)
        return romWithLevel(*(cab + obj.toTypedArray().toIntArray() + intArrayOf(0xFF)))
    }

    @Test
    fun `guia de linea en cuesta izquierda baja una fila y DOS columnas por paso`() {
        // Tileset 2 (cuerda), obj 0x3A: h0 = 0x60 | fila, h1 = 0xA0 | col.
        // size 0x20 → nibble bajo 0 (cuesta izquierda normal), nibble alto 2 → 3 pasos.
        val tm = SmwLayer1.parse(romTileset(2, 0x62, 0xA8, 0x20), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects, "el 0x3A de cuerda ya está portado")
        // Arranca en (col 8, fila 2) y va bajando: 8→6→4, filas 2→3→4. Dos teselas por
        // paso (0x8C izquierda, 0x8D derecha), ambas de página 0.
        val pasos = listOf(Triple(8, 2, 0), Triple(6, 3, 1), Triple(4, 4, 2))
        for ((col, fila, i) in pasos) {
            assertEquals(0x8C, tm.block(col, fila), "paso $i: guía izquierda")
            assertEquals(0x8D, tm.block(col + 1, fila), "paso $i: guía derecha")
        }
        assertEquals(0x25, tm.block(8, 3), "debajo del arranque queda aire")
    }

    @Test
    fun `la cuesta de ON-OFF baja UNA columna por paso y usa su propia tesela`() {
        // size 0x21 → nibble bajo 1 (ON/OFF izquierda, tesela 0x86), 3 pasos.
        val tm = SmwLayer1.parse(romTileset(2, 0x61, 0xA8, 0x21), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Una sola tesela por paso, y el desplazamiento es de UNA columna (+15), no de dos.
        assertEquals(0x86, tm.block(8, 1))
        assertEquals(0x86, tm.block(7, 2))
        assertEquals(0x86, tm.block(6, 3))
        assertEquals(0x25, tm.block(9, 1), "la de ON/OFF no dibuja segunda tesela")

        // El nibble bajo 4 es la MISMA cuesta con la otra tesela (0x94).
        val otra = SmwLayer1.parse(romTileset(2, 0x61, 0xA8, 0x24), 0, 0)
        assertNotNull(otra)
        assertEquals(0x94, otra.block(8, 1))
    }

    @Test
    fun `un nibble fuera de la tabla de seis punteros se cuenta como no portado`() {
        // La tabla del juego solo tiene 6 entradas: con 6 saltaría a basura. Se prefiere
        // contarlo que inventar un dibujo.
        val tm = SmwLayer1.parse(romTileset(2, 0x62, 0xA8, 0x26), 0, 0)
        assertNotNull(tm)
        assertEquals(1, tm.unknownObjects)
        assertEquals(0x25, tm.block(8, 2), "no se dibuja nada")
    }

    @Test
    fun `el 0x33 de cuerda son los bloques de interrogacion azules`() {
        // RopeObj33_BlueSwitchBlocks no es una rutina propia: llama a la de pradera.
        // Comparar las dos salidas es la forma honesta de comprobarlo.
        val cuerda = SmwLayer1.parse(romTileset(2, 0x62, 0x38, 0x03), 0, 0)
        val pradera = SmwLayer1.parse(romTileset(0, 0x62, 0x28, 0x03), 0, 0)
        assertNotNull(cuerda); assertNotNull(pradera)
        assertEquals(0, cuerda.unknownObjects, "el 0x33 de cuerda ya está portado")
        for (c in 0..20) for (r in 0..5) {
            assertEquals(pradera.block(c, r), cuerda.block(c, r), "bloque ($c,$r)")
        }
    }

    // ------------------- el LIENZO y el lanzatorpedos (ExtObj70..7F / UndergroundObj37) -------------------
    // Objetos extendidos: se codifican con objNum 0 y el id en el byte de tamaño.

    @Test
    fun `el lanzatorpedos es un cuadrado 2x2 solido`() {
        // ext 0x7F en fila 2, col 4: h0 = fila, h1 = col, tamaño = id extendido.
        val tm = SmwLayer1.parse(romTileset(3, 0x02, 0x04, 0x7F), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Las cuatro teselas van en página 1 (0x1xx) porque el lanzatorpedos es sólido.
        assertEquals(0x166, tm.block(4, 2)); assertEquals(0x167, tm.block(5, 2))
        assertEquals(0x168, tm.block(4, 3)); assertEquals(0x169, tm.block(5, 3))
        assertEquals(0x25, tm.block(6, 2), "no se sale de sus 2×2")
    }

    @Test
    fun `el tapiz mide 4 de ancho por 6 de alto y solo el travesano es solido`() {
        val tm = SmwLayer1.parse(romTileset(3, 0x02, 0x04, 0x71), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Fila 0: el travesaño, cuatro teselas de PÁGINA 1.
        assertEquals(listOf(0x15C, 0x15D, 0x15E, 0x160), (4..7).map { tm.block(it, 2) })
        // Filas 1-4: la tela, tres teselas de página 0 y siempre las mismas.
        for (f in 3..6) assertEquals(listOf(0x73, 0x74, 0x75), (4..6).map { tm.block(it, f) }, "fila $f")
        // El remate 0x5F de la fila 4 es de página 1 y va a la DERECHA de la tela.
        assertEquals(0x15F, tm.block(7, 6))
        // Fila 5: el fleco.
        assertEquals(listOf(0x76, 0x76, 0x76), (4..6).map { tm.block(it, 7) })
    }

    @Test
    fun `las piezas sueltas del lienzo son de adorno, en pagina 0`() {
        // ext 0x77 → tercera tesela de la tabla de piezas sueltas.
        val suelta = SmwLayer1.parse(romTileset(3, 0x02, 0x04, 0x77), 0, 0)
        assertNotNull(suelta)
        assertEquals(0, suelta.unknownObjects)
        assertEquals(0x7F, suelta.block(4, 2))
        // ext 0x7D → pareja VERTICAL (una tesela y la de debajo).
        val pareja = SmwLayer1.parse(romTileset(3, 0x02, 0x04, 0x7D), 0, 0)
        assertNotNull(pareja)
        assertEquals(0x82, pareja.block(4, 2)); assertEquals(0x85, pareja.block(4, 3))
        // ext 0x70 → pareja HORIZONTAL.
        val dupla = SmwLayer1.parse(romTileset(3, 0x02, 0x04, 0x70), 0, 0)
        assertNotNull(dupla)
        assertEquals(0x84, dupla.block(4, 2)); assertEquals(0x85, dupla.block(5, 2))
    }

    @Test
    fun `el lienzo grande pone cinco travesanos y replica la pantalla entera`() {
        // std 0x37 de cueva: h0 = 0x60 | fila, h1 = 0x70 | col. El nibble bajo del
        // tamaño (1) dice a cuántas pantallas se replica.
        val tm = SmwLayer1.parse(romTileset(3, 0x60, 0x70, 0x01), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects)
        // Los cinco travesaños van en filas FIJAS (5, 9, 13, 17 y 21), no donde caiga el
        // objeto. Pero los ocho tapices se pintan DESPUÉS y encima, y como miden 6 de
        // alto acaban tapando casi todo: de los cinco travesaños solo asoman trozos del
        // primero y del último. Es lo que se ve en el pasillo del castillo.
        for (c in listOf(4, 7, 12, 15)) assertEquals(0x161, tm.block(c, 5), "travesaño fila 5 col $c")
        for (c in listOf(0, 3, 8, 11)) assertEquals(0x161, tm.block(c, 21), "travesaño fila 21 col $c")
        // Donde sí hay tapiz, manda el tapiz.
        assertEquals(0x15C, tm.block(0, 5), "tapiz encima del travesaño")
        assertEquals(0x15C, tm.block(8, 5), "segundo tapiz de la fila 5")
        // Las filas 9, 13 y 17 quedan enteramente cubiertas por la tela y los travesaños
        // de los tapices, así que ahí no queda ni un bloque 0x161 a la vista.
        for (f in listOf(9, 13, 17)) for (c in 0..15) {
            assertTrue(tm.block(c, f) != 0x161, "fila $f col $c: el travesaño debería estar tapado")
        }
        // Y todo ello se replica tal cual en la pantalla siguiente (columnas 16+).
        for (c in 0..15) for (f in listOf(5, 9, 13)) {
            assertEquals(tm.block(c, f), tm.block(c + 16, f), "réplica en ($c,$f)")
        }
    }

    @Test
    fun `la lava en cuesta muy inclinada de la derecha remata con su tesela de borde`() {
        // Tileset 3 (cueva), obj 0x39: h0 = 0x60 | fila, h1 = 0x90 | col.
        // size 0x03 → los dos bits bajos = 3 (derecha muy inclinada), un solo paso.
        val tm = SmwLayer1.parse(romTileset(3, 0x62, 0x94, 0x03), 0, 0)
        assertNotNull(tm)
        assertEquals(0, tm.unknownObjects, "el 0x39 de cueva ya está portado")
        // Borde diagonal arriba y remate justo debajo, los dos en página 1.
        assertEquals(0x1D7, tm.block(4, 2), "borde de la cuesta")
        assertEquals(0x1FE, tm.block(4, 3), "remate de la fila de lava")
    }

    @Test
    fun `cero objetos solo esta BIEN en los modos de sala de jefe`() {
        // `BeginLoadingLevelData` ($05:83AC) corta el bucle ANTES de la primera llamada a
        // `LoadLevelDataObject()` cuando el modo es 9, 11 o 16:
        //
        //     if (misc_level_mode_setting == 9 || == 11 || == 16) break;
        //     if (*ptr_layer1_data != 0xFF) LoadLevelDataObject();
        //
        // O sea que en esos tres un nivel sin objetos es EL DATO CORRECTO, no un nivel roto:
        // son las 24 salas de jefe de la ROM, y su sala se monta desde el fondo. Tratarlas
        // como ilegibles las descartaba enteras, que es lo que pasaba antes.
        for (modo in intArrayOf(9, 11, 16)) {
            assertTrue(SmwLayer1.esSalaDeJefe(modo), "el modo $modo no carga objetos")
        }
        // Y no vale ensanchar la excepcion para que cuadren mas niveles: en cualquier otro
        // modo el juego SI procesa objetos, asi que cero objetos sigue siendo "no hay nivel".
        for (modo in intArrayOf(0, 1, 2, 8, 10, 12, 13, 14, 17, 30)) {
            assertFalse(SmwLayer1.esSalaDeJefe(modo), "el modo $modo si carga objetos")
        }
    }

}
