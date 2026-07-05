package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesAssetExtractor
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesMapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SnesDecoderTest {

    // -------------------------------------------------------------- color

    @Test
    fun `bgr15 blanco y negro`() {
        assertEquals(0xFF000000.toInt(), SnesDecoder.bgr15ToArgb(0x0000))
        // 0x7FFF = todos los canales de 5 bits a máximo -> blanco puro
        assertEquals(0xFFFFFFFF.toInt(), SnesDecoder.bgr15ToArgb(0x7FFF))
    }

    @Test
    fun `bgr15 canales primarios`() {
        // r5 = 0x1F -> rojo puro (b y g a 0)
        assertEquals(0xFFFF0000.toInt(), SnesDecoder.bgr15ToArgb(0x001F))
        // g5 = 0x1F desplazado 5 -> verde puro
        assertEquals(0xFF00FF00.toInt(), SnesDecoder.bgr15ToArgb(0x03E0))
        // b5 = 0x1F desplazado 10 -> azul puro
        assertEquals(0xFF0000FF.toInt(), SnesDecoder.bgr15ToArgb(0x7C00))
    }

    @Test
    fun `argb a bgr15 ida y vuelta preserva la profundidad de 5 bits`() {
        // Cualquier BGR15 debe sobrevivir a bgr15 -> argb -> bgr15.
        for (bgr in intArrayOf(0x0000, 0x7FFF, 0x001F, 0x03E0, 0x7C00, 0x1234, 0x5AD6)) {
            val back = SnesDecoder.argbToBgr15(SnesDecoder.bgr15ToArgb(bgr))
            assertEquals(bgr, back, "round trip de 0x${bgr.toString(16)}")
        }
    }

    // --------------------------------------------------------------- tile

    @Test
    fun `decode 2bpp lee ambos planos`() {
        // Fila 0: plano0 = 0b10000001, plano1 = 0b11000000
        // -> pixel0 = bit0(1)|bit1(1)<<1 = 3 ; pixel1 = 0|1<<1 = 2 ; pixel7 = 1|0 = 1
        val rom = ByteArray(16)
        rom[0] = 0x81.toByte()
        rom[1] = 0xC0.toByte()
        val tile = SnesDecoder.decodeTile(rom, 0, SnesGraphicFormat.SNES_2BPP)
        assertEquals(3, tile.pixelIndices[0])
        assertEquals(2, tile.pixelIndices[1])
        assertEquals(0, tile.pixelIndices[2])
        assertEquals(1, tile.pixelIndices[7])
    }

    @Test
    fun `decode 4bpp usa los cuatro planos`() {
        // Coloca un unico pixel (0,0) al valor 15 = 0b1111 activando el bit 7
        // (mascara 0x80) de los cuatro planos.
        val rom = ByteArray(32)
        rom[0] = 0x80.toByte()  // plano bit0, fila 0
        rom[1] = 0x80.toByte()  // plano bit1, fila 0
        rom[16] = 0x80.toByte() // plano bit2, fila 0
        rom[17] = 0x80.toByte() // plano bit3, fila 0
        val tile = SnesDecoder.decodeTile(rom, 0, SnesGraphicFormat.SNES_4BPP)
        assertEquals(15, tile.pixelIndices[0])
        assertEquals(0, tile.pixelIndices[1])
    }

    @Test
    fun `decode 8bpp alcanza 255`() {
        val rom = ByteArray(64)
        // Ocho planos: bytes 0,1,16,17,32,33,48,49 -> bit 7 (0x80) para el pixel (0,0)
        for (idx in intArrayOf(0, 1, 16, 17, 32, 33, 48, 49)) rom[idx] = 0x80.toByte()
        val tile = SnesDecoder.decodeTile(rom, 0, SnesGraphicFormat.SNES_8BPP)
        assertEquals(255, tile.pixelIndices[0])
    }

    @Test
    fun `decode fuera de rango devuelve tile vacio`() {
        val rom = ByteArray(8) // menor que un tile de 16 bytes
        val tile = SnesDecoder.decodeTile(rom, 0, SnesGraphicFormat.SNES_2BPP)
        assertTrue(tile.pixelIndices.all { it == 0 })
    }

    // ------------------------------------------------------------- header

    @Test
    fun `parseHeader detecta LoROM con checksum valido`() {
        val rom = ByteArray(0x10000)
        val o = 0x7FC0
        val title = "MI JUEGO DE PRUEBA   " // 21 chars
        for (i in title.indices) rom[o + i] = title[i].code.toByte()
        rom[o + 21] = 0x30            // makeup: FastROM (bit 4)
        rom[o + 22] = 0x02            // ROM + RAM + Battery
        rom[o + 23] = 0x0A            // tamaño ROM
        rom[o + 24] = 0x03            // tamaño SRAM
        rom[o + 25] = 0x01            // EE.UU.
        rom[o + 26] = 0x01            // Nintendo
        rom[o + 27] = 0x00            // versión
        // checksum ^ complement = 0xFFFF
        val checksum = 0x1234
        val complement = checksum.inv() and 0xFFFF
        rom[o + 28] = (complement and 0xFF).toByte()
        rom[o + 29] = ((complement shr 8) and 0xFF).toByte()
        rom[o + 30] = (checksum and 0xFF).toByte()
        rom[o + 31] = ((checksum shr 8) and 0xFF).toByte()

        val header = SnesDecoder.parseHeader(rom)
        assertEquals("MI JUEGO DE PRUEBA", header.title)
        assertEquals(SnesMapping.LOROM, header.mapping)
        assertTrue(header.isFastRom)
        assertTrue(header.isChecksumValid)
        assertEquals("EE.UU./Canadá (NTSC)", header.country)
        assertEquals("Nintendo", header.licensee)
        assertEquals((1L shl 0x0A) * 1024L, header.romSizeBytes)
    }

    // ------------------------------------------------------------ palette

    @Test
    fun `parsePalette lee colores little endian`() {
        val rom = ByteArray(8)
        // color 0 = rojo (0x001F), color 1 = verde (0x03E0)
        rom[0] = 0x1F; rom[1] = 0x00
        rom[2] = 0xE0.toByte(); rom[3] = 0x03
        val pal = SnesDecoder.parsePalette(rom, 0, 2)
        assertEquals(0xFFFF0000.toInt(), pal[0])
        assertEquals(0xFF00FF00.toInt(), pal[1])
    }

    @Test
    fun `scanRomForPalettes encuentra un bloque CGRAM de 16 colores`() {
        val rom = ByteArray(0x400)
        // Escribe 16 colores unicos con bit 15 = 0 en el offset 0.
        for (i in 0 until 16) {
            val bgr = (i * 0x111) and 0x7FFF // variados, bit15 a 0
            rom[2 * i] = (bgr and 0xFF).toByte()
            rom[2 * i + 1] = ((bgr shr 8) and 0xFF).toByte()
        }
        val palettes = SnesDecoder.scanRomForPalettes(rom, minUniqueColors = 8)
        assertTrue(palettes.isNotEmpty())
        assertEquals(0, palettes.first().offset)
        assertEquals(16, palettes.first().size)
    }

    // ------------------------------------------------------ extractor

    @Test
    fun `extractTileSheet compone la rejilla con transparencia en el indice 0`() {
        val rom = ByteArray(64)
        // Tile 0: pixel (0,0) = 3, resto 0 (con 2bpp)
        rom[0] = 0x80.toByte()
        rom[1] = 0x80.toByte()
        val palette = intArrayOf(0xFF111111.toInt(), 0xFF222222.toInt(), 0xFF333333.toInt(), 0xFF444444.toInt())
        val sheet = SnesAssetExtractor.extractTileSheet(
            rom, offset = 0, format = SnesGraphicFormat.SNES_2BPP,
            palette = palette, tileCount = 4, columns = 2,
        )
        assertEquals(2, sheet.columns)
        assertEquals(2, sheet.rows)
        assertEquals(16, sheet.image.width)  // 2 cols * 8 px
        assertEquals(16, sheet.image.height)
        // pixel (0,0) del tile 0 -> color 3
        assertEquals(0xFF444444.toInt(), sheet.image.get(0, 0))
        // pixel (1,0) es indice 0 -> transparente
        assertEquals(0x00000000, sheet.image.get(1, 0))
    }

    @Test
    fun `toTileset describe la hoja generada`() {
        val rom = ByteArray(16 * 8)
        val palette = SnesDecoder.parsePalette(rom, 0, 4)
        val sheet = SnesAssetExtractor.extractTileSheet(
            rom, 0, SnesGraphicFormat.SNES_2BPP, palette, tileCount = 8, columns = 4,
        )
        val tileset = SnesAssetExtractor.toTileset(sheet, id = 7, name = "SNES rip", imageFileName = "snes_rip.png")
        assertEquals(7, tileset.id)
        assertEquals("snes_rip.png", tileset.image)
        assertEquals(8, tileset.tileSize)
        assertEquals(4, tileset.columns)
        assertEquals(2, tileset.rows)
        assertEquals(8, tileset.passable.size)
    }

    @Test
    fun `availableTiles cuenta tiles completos`() {
        assertEquals(4, SnesAssetExtractor.availableTiles(64, 0, SnesGraphicFormat.SNES_2BPP))
        assertEquals(2, SnesAssetExtractor.availableTiles(64, 32, SnesGraphicFormat.SNES_2BPP))
        assertEquals(0, SnesAssetExtractor.availableTiles(64, 64, SnesGraphicFormat.SNES_2BPP))
        assertNotEquals(0, SnesAssetExtractor.availableTiles(64, 0, SnesGraphicFormat.SNES_8BPP))
    }
}
