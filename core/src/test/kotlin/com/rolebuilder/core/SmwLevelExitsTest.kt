package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SnesDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests SINTÉTICOS del extractor de conectividad de niveles ([SmwLevelExits]).
 * Se planta a mano: la firma vanilla, el puntero de Layer 1 del nivel 0, un flujo de
 * objetos con un midway + una salida DIRECTA + una salida SECUNDARIA, y las cuatro
 * tablas de entradas secundarias. Se comprueba el decodificado bit a bit, incluida la
 * resolución de la entrada secundaria a nivel + posición. Sin ROM con copyright.
 */
class SmwLevelExitsTest {

    // Direcciones PC (mapeo LoROM banco $05, headerless → delta 0).
    private val ptrTable = 0x2E000                       // $05:E000 tabla de punteros de Layer 1
    private val sigAddr = 0x05 * 0x8000 + 0x5731         // $05:D731 firma vanilla (== 0x30)
    private val levelDataPc = 0x30000                    // $06:8000 → datos del nivel 0
    private fun secBase(table: Int) = 0x05 * 0x8000 + (table and 0x7FFF)

    /**
     * ROM con el nivel 0 apuntando a [levelDataPc] y un flujo con:
     *  - midway (obj estándar 0x15) en pantalla 0, columna 4, fila 3
     *  - salida DIRECTA en pantalla 5 → nivel 0x42
     *  - salida SECUNDARIA en pantalla 9 → entrada secundaria 0x07
     * más la tabla de entradas secundarias (entrada 7 → nivel 0x1A, presets conocidos).
     */
    private fun buildRom(mode: Int = 0x00, vanilla: Boolean = true): ByteArray {
        val rom = ByteArray(0x40000)
        if (vanilla) rom[sigAddr] = 0x30

        // Puntero de Layer 1 del nivel 0: lorom(0x00,0x80,0x06) = PC 0x30000.
        rom[ptrTable] = 0x00; rom[ptrTable + 1] = 0x80.toByte(); rom[ptrTable + 2] = 0x06

        // Cabecera de 5 bytes (byte 1 = modo) + flujo de objetos + 0xFF.
        val stream = intArrayOf(
            0x00, mode, 0x00, 0x00, 0x00,   // cabecera (solo el modo importa aquí)
            0x23, 0x54, 0x00,               // midway (obj 0x15): pantalla 0, fila 3, col 4
            0x05, 0x00, 0x00, 0x42,         // screen exit DIRECTO: pantalla 5 → nivel 0x42
            0x09, 0x02, 0x00, 0x07,         // screen exit SECUNDARIO: pantalla 9 → entrada 0x07
            0xFF,                           // terminador
        )
        stream.forEachIndexed { i, b -> rom[levelDataPc + i] = b.toByte() }

        // Tabla de entradas secundarias, entrada 0x07.
        rom[secBase(0xF800) + 7] = 0x1A            // destino (byte bajo) → nivel 0x1A
        rom[secBase(0xFA00) + 7] = 0xB5.toByte()   // Y: yIdx=5, L1yPos=3, L2yPos=2
        rom[secBase(0xFC00) + 7] = 0x60            // X: xIdx=3 (bits 5-7)
        rom[secBase(0xFE00) + 7] = 0x04            // FG/BG: fgBgPosition=4 (bits 0-2)
        return rom
    }

    @Test
    fun `parsea salida directa, salida secundaria resuelta y midway`() {
        val rom = buildRom()
        val c = SmwLevelExits.read(rom, SnesDecoder.parseHeader(rom), 0)
        assertNotNull(c)
        assertEquals(0, c.level)
        assertEquals(2, c.exits.size)

        // Salida DIRECTA.
        val direct = c.exits.first { it.fromScreen == 5 }
        assertFalse(direct.isSecondary)
        assertEquals(0x42, direct.destinationLevel)
        assertEquals(0x42, direct.rawDestinationByte)
        assertNull(direct.secondaryEntrance)

        // Salida SECUNDARIA resuelta por la tabla.
        val sec = c.exits.first { it.fromScreen == 9 }
        assertTrue(sec.isSecondary)
        assertEquals(0x07, sec.rawDestinationByte)
        assertEquals(0x1A, sec.destinationLevel)
        val se = assertNotNull(sec.secondaryEntrance)
        assertEquals(0x07, se.number)
        assertEquals(0x1A, se.destinationLevel)
        assertEquals(5, se.entranceYIndex)
        assertEquals(3, se.entranceXIndex)
        assertEquals(4, se.fgBgPosition)
        assertEquals(3, se.layer1YPos)
        assertEquals(2, se.layer2YPos)

        // Midway (no es poste de meta porque tamaño & 0xF == 0).
        val mid = assertNotNull(c.midway)
        assertEquals(0, mid.screen)
        assertEquals(4, mid.xTile)
        assertEquals(3, mid.row)
        assertFalse(mid.isGoalPost)
    }

    @Test
    fun `el bit alto del nivel de origen se aplica a los destinos`() {
        // Mismo flujo pero para el nivel 0x100 (mitad alta): el puntero del nivel 0x100
        // reutiliza el mismo dato, así que los destinos deben salir en 0x1xx.
        val rom = buildRom()
        val base = ptrTable + 3 * 0x100
        rom[base] = 0x00; rom[base + 1] = 0x80.toByte(); rom[base + 2] = 0x06
        // La entrada secundaria del nivel 0x100 es la número 0x107; la plantamos.
        rom[secBase(0xF800) + 0x107] = 0x1A
        rom[secBase(0xFA00) + 0x107] = 0xB5.toByte()
        rom[secBase(0xFC00) + 0x107] = 0x60
        rom[secBase(0xFE00) + 0x107] = 0x04

        val c = SmwLevelExits.read(rom, SnesDecoder.parseHeader(rom), 0x100)
        assertNotNull(c)
        val direct = c.exits.first { it.fromScreen == 5 }
        assertEquals(0x142, direct.destinationLevel)      // 0x100 | 0x42
        val sec = c.exits.first { it.fromScreen == 9 }
        assertEquals(0x107, sec.secondaryEntrance!!.number) // 0x100 | 0x07
        assertEquals(0x11A, sec.destinationLevel)           // 0x100 | 0x1A
    }

    @Test
    fun `una ROM que no es SMW vanilla devuelve null`() {
        assertNull(SmwLevelExits.read(buildRom(vanilla = false), SnesDecoder.parseHeader(buildRom(vanilla = false)), 0))
    }

    @Test
    fun `un nivel vertical devuelve null`() {
        // Modo 3: VERTICAL_TABLE[3] & 1 == 1 → fuera de alcance.
        val rom = buildRom(mode = 0x03)
        assertNull(SmwLevelExits.read(rom, SnesDecoder.parseHeader(rom), 0))
    }

    @Test
    fun `un nivel fuera de rango devuelve null`() {
        val rom = buildRom()
        assertNull(SmwLevelExits.read(rom, SnesDecoder.parseHeader(rom), 0x200))
    }
}
