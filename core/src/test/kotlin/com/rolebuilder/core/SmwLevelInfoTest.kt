package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test del lector de la ficha de nivel ([SnesGameRecipes.smwLevelInfo]): decodifica
 * la cabecera primaria (5 bytes) igual que LoadLevelHeader. SINTÉTICO: se planta un
 * nivel con una cabecera conocida. Sin ROM con copyright.
 */
class SmwLevelInfoTest {

    @Test
    fun `decodifica tamano, musica, modo y tiempo de la cabecera primaria`() {
        val rom = ByteArray(0x70000)
        // Puntero de Layer 1 del nivel 0 → datos en 0x10000.
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC] = 0x00
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 1] = 0x80.toByte()
        rom[SnesGameRecipes.SMW_LAYER1_PTR_PC + 2] = 0x02
        // Cabecera primaria: h0=0x0F (16 pantallas), h1=0x02 (modo 2), h2=0x30 (música 3),
        // h3=0x40 (timer idx 1 → 200; fgPal 0; sprPal 0), h4=0x05 (tileset FG 5).
        val hdr = intArrayOf(0x0F, 0x02, 0x30, 0x40, 0x05)
        hdr.forEachIndexed { i, b -> rom[0x10000 + i] = b.toByte() }

        val info = SnesGameRecipes.smwLevelInfo(rom, SnesDecoder.parseHeader(rom), 0)
        assertNotNull(info)
        assertEquals(16, info.screens)
        assertEquals(256, info.widthTiles) // 16 pantallas × 16 casillas
        assertEquals(2, info.levelMode)
        assertEquals(3, info.musicIndex)
        assertEquals(200, info.startTime)
        assertEquals(5, info.fgTileset)
    }
}
