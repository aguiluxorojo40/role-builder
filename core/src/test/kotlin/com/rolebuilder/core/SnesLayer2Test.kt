package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test sintético del pipeline de fondos de Layer 2: puntero $05E600 → banco $0C →
 * RLE1 → índices de bloque Map16 → 4 palabras [tile#][YXPCCCTT] por bloque. Sin ROM
 * real: plantamos una tabla de punteros, un flujo RLE1 y una tabla Map16 conocidos.
 */
class SnesLayer2Test {

    private fun w16(rom: ByteArray, at: Int, v: Int) {
        rom[at] = (v and 0xFF).toByte(); rom[at + 1] = ((v shr 8) and 0xFF).toByte()
    }

    @Test
    fun `layer2BgEntries resuelve puntero, RLE1 y Map16 a entradas de tilemap`() {
        val rom = ByteArray(0x70000)

        // Puntero de Layer 2 del nivel 0: banco 0xFF (=fondo), addr $8100 en banco $0C.
        // dataPc = 0x60000 + (0x8100 - 0x8000) = 0x60100.
        val ptr = SnesGameRecipes.SMW_LAYER2_PTR_PC
        rom[ptr] = 0x00; rom[ptr + 1] = 0x81.toByte(); rom[ptr + 2] = SnesGameRecipes.SMW_BG_IS_BACKGROUND.toByte()

        // Datos de fondo (RLE1) en 0x60100: copia directa de 4 bloques [5,6,5,6] + fin.
        val dataPc = SnesGameRecipes.SMW_BG_BANK_PC + (0x8100 - 0x8000)
        rom[dataPc] = 0x03           // copia directa, L+1 = 4
        rom[dataPc + 1] = 5; rom[dataPc + 2] = 6; rom[dataPc + 3] = 5; rom[dataPc + 4] = 6
        rom[dataPc + 5] = 0xFF.toByte() // fin

        // Tabla Map16 (Layer 2): bloque 5 y 6, 8 bytes = 4 palabras cada uno.
        val m16 = SnesGameRecipes.SMW_MAP16_L2_PC
        // Bloque 5: 4 teselas con tile#=0x100 y sub-paleta 2 → word = 0x100 | (2<<10) = 0x900.
        for (k in 0..3) w16(rom, m16 + 8 * 5 + 2 * k, 0x100 or (2 shl 10))
        // Bloque 6: tile#=0x123, sub-paleta 5, hFlip → word = 0x123 | (5<<10) | 0x4000.
        for (k in 0..3) w16(rom, m16 + 8 * 6 + 2 * k, 0x123 or (5 shl 10) or 0x4000)

        val entries = SnesGameRecipes.layer2BgEntries(rom, delta = 0, level = 0)
        assertEquals(16, entries.size, "4 bloques × 4 teselas")
        // Bloque 5 (posiciones 0 y 8).
        assertEquals(0x100, entries[0].tileIndex)
        assertEquals(2, entries[0].palette)
        // Bloque 6 (posiciones 4 y 12).
        assertEquals(0x123, entries[4].tileIndex)
        assertEquals(5, entries[4].palette)
        assertTrue(entries[4].hFlip)
    }

    @Test
    fun `layer2BgEntries vacio cuando el nivel no es un fondo`() {
        val rom = ByteArray(0x70000)
        // Banco != 0xFF → es Layer 2 de objetos, no un fondo tilemap.
        val ptr = SnesGameRecipes.SMW_LAYER2_PTR_PC
        rom[ptr] = 0x00; rom[ptr + 1] = 0x81.toByte(); rom[ptr + 2] = 0x0C
        assertTrue(SnesGameRecipes.layer2BgEntries(rom, 0, 0).isEmpty())
    }
}
