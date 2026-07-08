package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test SINTÉTICO del ensamblador de CGRAM de SMW (assembleSmwCgram): reproduce la
 * rutina LoadPalette del juego (bank $00, disassembly SMWDisX). Sin ROM con
 * copyright: plantamos una cabecera de nivel y colores conocidos en las tablas
 * reales y comprobamos que cada color aterriza en la fila/columna correcta de la
 * CGRAM de 16×16. La validación contra la ROM real (48/48 contra un volcado de
 * emulador) se hace aparte; esto fija el ALGORITMO.
 */
class SmwCgramTest {

    private fun putColor(rom: ByteArray, pc: Int, bgr15: Int) {
        rom[pc] = (bgr15 and 0xFF).toByte(); rom[pc + 1] = ((bgr15 shr 8) and 0xFF).toByte()
    }

    private fun argb(bgr15: Int) = SnesDecoder.bgr15ToArgb(bgr15)

    @Test
    fun `assembleSmwCgram coloca cada tabla en su fila y columna reales`() {
        val rom = ByteArray(0x30000)

        // Cabecera del nivel 0 en PC 0x10000 (banco $02, addr $8000).
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC] = 0x00
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 1] = 0x80.toByte()
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 2] = 0x02
        val hdr = 0x10000
        val bgPal = 1; val backArea = 3; val fgPal = 2; val sprPal = 4
        rom[hdr] = (bgPal shl 5).toByte()                     // byte0: BG palette (bits 7-5)
        rom[hdr + 1] = (backArea shl 5).toByte()              // byte1: back area (bits 7-5)
        rom[hdr + 2] = 0                                       // byte2: (sprite tileset/música)
        rom[hdr + 3] = (fgPal or (sprPal shl 3)).toByte()     // byte3: FG (2-0) + sprite (5-3)
        rom[hdr + 4] = 5                                       // byte4: tileset FG (3-0)

        // Colores testigo en las tablas reales (offsets PC, delta=0).
        val fgBase = SnesGameRecipes.SMW_FG_PC + fgPal * 0x18
        val bgBase = SnesGameRecipes.SMW_BG_PC + bgPal * 0x18
        val sprBase = SnesGameRecipes.SMW_SPRITE_PC + sprPal * 0x18
        putColor(rom, fgBase, 0x1234)              // FG color 0 → fila 2, col 2
        putColor(rom, fgBase + 12, 0x1235)         // FG color 6 → fila 3, col 2
        putColor(rom, bgBase, 0x1236)              // BG color 0 → fila 0, col 2
        putColor(rom, sprBase, 0x1237)             // sprite color 0 → fila 14, col 2
        putColor(rom, SnesGameRecipes.SMW_BACK_AREA_PC + 2 * backArea, 0x1238) // fondo → índice 0
        putColor(rom, SnesGameRecipes.SMW_FIXED_PC, 0x1239)   // Standard color 0 → fila 4, col 2
        putColor(rom, SnesGameRecipes.SMW_PLAYER_PC, 0x123A)  // Player color 0 → fila 8, col 6
        putColor(rom, SnesGameRecipes.SMW_BERRY_PC, 0x123B)   // Berry color 0 → fila 2, col 9

        val cg = SnesGameRecipes.assembleSmwCgram(rom, delta = 0, level = 0)
        fun at(row: Int, col: Int) = cg[row * 16 + col]

        assertEquals(argb(0x1234), at(2, 2), "FG palette → fila 2 col 2")
        assertEquals(argb(0x1235), at(3, 2), "FG palette 2ª fila → fila 3 col 2")
        assertEquals(argb(0x1236), at(0, 2), "BG palette → fila 0 col 2")
        assertEquals(argb(0x1237), at(14, 2), "Sprite palette → fila 14 col 2")
        assertEquals(argb(0x1238), at(0, 0), "Back area color → índice 0 (fondo)")
        assertEquals(argb(0x1239), at(4, 2), "Standard colors → fila 4 col 2")
        assertEquals(argb(0x123A), at(8, 6), "Player (Mario) → fila 8 col 6")
        assertEquals(argb(0x123B), at(2, 9), "Berry → fila 2 col 9")
        // Col 1 = blanco fijo: $7FDD en objeto (filas 0-7), $7FFF en sprite (8-15).
        assertEquals(argb(0x7FDD), at(0, 1), "col 1 objeto = blanco $7FDD")
        assertEquals(argb(0x7FFF), at(8, 1), "col 1 sprite = blanco $7FFF")
    }

    @Test
    fun `readSmwLevelHeader decodifica los selectores de la cabecera primaria`() {
        val rom = ByteArray(0x30000)
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC] = 0x00
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 1] = 0x80.toByte()
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 2] = 0x02
        val hdr = 0x10000
        rom[hdr] = (5 shl 5).toByte()                 // bgPal = 5
        rom[hdr + 1] = (2 shl 5).toByte()             // backArea = 2
        rom[hdr + 3] = (7 or (3 shl 3)).toByte()      // fgPal = 7, sprPal = 3
        rom[hdr + 4] = 0x0C                           // objectTileset = 0xC

        val h = SnesGameRecipes.readSmwLevelHeader(rom, 0, 0)!!
        assertEquals(5, h.bgPal); assertEquals(2, h.backArea)
        assertEquals(7, h.fgPal); assertEquals(3, h.sprPal); assertEquals(0x0C, h.objectTileset)
    }
}
