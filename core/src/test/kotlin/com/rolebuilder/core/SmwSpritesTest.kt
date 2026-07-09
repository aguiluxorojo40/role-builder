package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwSprites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests SINTÉTICOS del parser de la lista de sprites (enemigos) de un nivel de SMW.
 * Plantamos un puntero en la tabla $05:EC00 y un flujo de sprites en el banco $07,
 * y comprobamos que cada entrada de 3 bytes se decodifica bit a bit como el juego.
 * Sin ROM con copyright.
 */
class SmwSpritesTest {

    /** ROM con el puntero de sprites del nivel 0 apuntando a $07:8000 (PC 0x38000). */
    private fun romWithSprites(vararg stream: Int): ByteArray {
        val rom = ByteArray(0x40000)
        // Puntero de 2 bytes ($07:8000 = 0x00,0x80) en la tabla, nivel 0.
        rom[SmwSprites.PTR_TABLE_PC] = 0x00
        rom[SmwSprites.PTR_TABLE_PC + 1] = 0x80.toByte()
        stream.forEachIndexed { i, b -> rom[0x38000 + i] = b.toByte() }
        return rom
    }

    @Test
    fun `decodifica posicion, id y bits extra de cada sprite`() {
        // Cabecera 0x00, luego dos sprites y terminador 0xFF.
        // Sprite A: b1=0x54 b2=0x32 b3=0x1E → Y=5, extra=1, pantalla=2, X=2*16+3=35.
        // Sprite B: b1=0x1B b2=0xA2 b3=0x0B → Y=17, extra=2, pantalla=18, X=18*16+10=298.
        val list = SmwSprites.parse(
            romWithSprites(0x00, 0x54, 0x32, 0x1E, 0x1B, 0xA2, 0x0B, 0xFF), 0, 0,
        )
        assertNotNull(list)
        assertEquals(0x00, list.header)
        assertEquals(2, list.sprites.size)

        val a = list.sprites[0]
        assertEquals(35, a.xTile); assertEquals(5, a.yTile)
        assertEquals(0x1E, a.id); assertEquals(1, a.extraBits); assertEquals(2, a.screen)

        val b = list.sprites[1]
        assertEquals(298, b.xTile); assertEquals(17, b.yTile)
        assertEquals(0x0B, b.id); assertEquals(2, b.extraBits); assertEquals(18, b.screen)
    }

    @Test
    fun `una lista vacia (solo terminador) devuelve cero sprites`() {
        val list = SmwSprites.parse(romWithSprites(0x00, 0xFF), 0, 0)
        assertNotNull(list)
        assertEquals(0, list.sprites.size)
    }

    @Test
    fun `un puntero fuera de rango en LoROM devuelve null`() {
        val rom = ByteArray(0x40000) // puntero a 0x0000: dirección inválida en LoROM
        assertNull(SmwSprites.parse(rom, 0, 0))
    }

    @Test
    fun `un flujo sin terminador se considera corrupto (null)`() {
        // Rellenamos hasta el final sin 0xFF: no debe colgarse ni inventar datos.
        val rom = ByteArray(0x38010)
        rom[SmwSprites.PTR_TABLE_PC] = 0x00
        rom[SmwSprites.PTR_TABLE_PC + 1] = 0x80.toByte()
        for (i in 0x38000 until 0x38010) rom[i] = 0x54.toByte() // nunca 0xFF
        assertNull(SmwSprites.parse(rom, 0, 0))
    }
}
