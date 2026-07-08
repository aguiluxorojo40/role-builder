package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesHeader
import com.rolebuilder.core.snes.SnesMapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests del ensamblado de paletas REALES de Super Mario World: comprueban que las
 * lecturas de las tablas fijas del banco $00 y la reconstrucción de las filas
 * canónicas (FG/sprite/Mario) coinciden con la especificación, de forma aislada,
 * sin necesitar una ROM real ni descompresión LC_LZ2.
 */
class SmwPaletteRecipeTest {

    private val BLACK = 0xFF000000.toInt()

    /** Cabecera mínima que solo fija el [headerOffset] (lo único que usa el delta). */
    private fun headerWith(headerOffset: Int) = SnesHeader(
        title = "SUPER MARIOWORLD", isFastRom = false, romTypeDescription = "",
        romSizeBytes = 0, sramSizeBytes = 0, country = "", licensee = "", version = 0,
        checksum = 0, checksumComplement = 0, isChecksumValid = true,
        mapping = SnesMapping.LOROM, headerOffset = headerOffset,
    )

    /** Escribe un color BGR15 little-endian (bit15=0) en el offset PC [at]. */
    private fun writeColor(rom: ByteArray, at: Int, bgr15: Int) {
        val v = bgr15 and 0x7FFF
        rom[at] = (v and 0xFF).toByte()
        rom[at + 1] = ((v shr 8) and 0xFF).toByte()
    }

    /** Escribe [colors] BGR15 consecutivos (2 bytes c/u) desde [at]. */
    private fun writeColors(rom: ByteArray, at: Int, colors: IntArray) {
        for (i in colors.indices) writeColor(rom, at + 2 * i, colors[i])
    }

    /** Rampa BGR15 de verdes crecientes (g5 = 4,8,12,...). */
    private fun greenRamp(n: Int) = IntArray(n) { i -> ((i + 1) * 4).coerceAtMost(31) shl 5 }
    /** Rampa BGR15 de rojos crecientes (r5 = 4,8,12,...). */
    private fun redRamp(n: Int) = IntArray(n) { i -> ((i + 1) * 4).coerceAtMost(31) }
    /** Rampa BGR15 de azules crecientes (b5 = 3,6,9,...). */
    private fun blueRamp(n: Int) = IntArray(n) { i -> ((i + 1) * 3).coerceAtMost(31) shl 10 }

    // ------------------------------------------------------------------ delta

    @Test
    fun `smwHeaderDelta distingue ROM con y sin cabecera SMC`() {
        assertEquals(0, SnesGameRecipes.smwHeaderDelta(headerWith(0x7FC0)))
        assertEquals(0x200, SnesGameRecipes.smwHeaderDelta(headerWith(0x7FC0 + 512)))
    }

    // -------------------------------------------------------------- lecturas

    @Test
    fun `las lecturas devuelven los ARGB esperados desde las tablas`() {
        val rom = ByteArray(0x8000)
        val backBgr = 0x1234
        val greens = greenRamp(6)
        val reds = redRamp(6)
        val player0 = IntArray(10) { i -> (i * 2 + 1) }         // valores BGR15 distintos
        writeColor(rom, SnesGameRecipes.SMW_BACK_AREA_PC, backBgr)              // back x=0
        writeColors(rom, SnesGameRecipes.SMW_FG_PC + 0x18 * 2, greens)          // FG idx 2
        writeColors(rom, SnesGameRecipes.SMW_SPRITE_PC, reds)                   // sprite idx 0
        writeColors(rom, SnesGameRecipes.SMW_PLAYER_PC, player0)                // player 0

        val t = SnesGameRecipes.SmwPaletteTables(rom, 0)
        assertEquals(SnesDecoder.bgr15ToArgb(backBgr), t.backColor(0))
        assertEquals(
            greens.map { SnesDecoder.bgr15ToArgb(it) },
            t.fgEntry(2).toList(),
        )
        assertEquals(
            reds.map { SnesDecoder.bgr15ToArgb(it) },
            t.sprEntry(0).toList(),
        )
        assertEquals(
            player0.map { SnesDecoder.bgr15ToArgb(it) },
            t.player(0).toList(),
        )
        // fgEntry/sprEntry leen SOLO 6 colores (la fila de arriba de la entrada de 12).
        assertEquals(6, t.fgEntry(2).size)
        assertEquals(6, t.sprEntry(0).size)
        assertEquals(10, t.player(0).size)
    }

    @Test
    fun `el delta de cabecera desplaza las lecturas 0x200 bytes`() {
        val rom = ByteArray(0x8000)
        val greens = greenRamp(6)
        // La misma entrada FG idx 2, pero escrita en la posición "con cabecera".
        writeColors(rom, SnesGameRecipes.SMW_FG_PC + 0x200 + 0x18 * 2, greens)
        val t = SnesGameRecipes.SmwPaletteTables(rom, 0x200)
        assertEquals(greens.map { SnesDecoder.bgr15ToArgb(it) }, t.fgEntry(2).toList())
    }

    // ------------------------------------------------------------ filas 3bpp

    @Test
    fun `row3bppFG ordena back, negro y los 6 colores FG`() {
        val rom = ByteArray(0x8000)
        val backBgr = 0x0421
        val greens = greenRamp(6)
        writeColor(rom, SnesGameRecipes.SMW_BACK_AREA_PC, backBgr)
        writeColors(rom, SnesGameRecipes.SMW_FG_PC + 0x18 * SnesGameRecipes.SMW_DEFAULT_FG_INDEX, greens)

        val t = SnesGameRecipes.SmwPaletteTables(rom, 0)
        val row = SnesGameRecipes.row3bppFG(
            t, SnesGameRecipes.SMW_DEFAULT_FG_INDEX, SnesGameRecipes.SMW_DEFAULT_BACK_INDEX,
        )
        assertEquals(8, row.size)
        assertEquals(SnesDecoder.bgr15ToArgb(backBgr), row[0])
        assertEquals(BLACK, row[1])
        for (i in 0 until 6) {
            assertEquals(SnesDecoder.bgr15ToArgb(greens[i]), row[2 + i], "FG color $i")
        }
    }

    @Test
    fun `row3bppSprite empieza por transparente y negro`() {
        val rom = ByteArray(0x8000)
        val reds = redRamp(6)
        writeColors(rom, SnesGameRecipes.SMW_SPRITE_PC, reds)

        val t = SnesGameRecipes.SmwPaletteTables(rom, 0)
        val row = SnesGameRecipes.row3bppSprite(t, SnesGameRecipes.SMW_DEFAULT_SPRITE_INDEX)
        assertEquals(8, row.size)
        assertEquals(0, row[0]) // transparente
        assertEquals(BLACK, row[1])
        for (i in 0 until 6) {
            assertEquals(SnesDecoder.bgr15ToArgb(reds[i]), row[2 + i], "sprite color $i")
        }
    }

    // ------------------------------------------------------------ fila Mario

    @Test
    fun `rowMario tiene 16 colores con transparente, negro, fix y player`() {
        val rom = ByteArray(0x8000)
        val fixed = blueRamp(6)                       // fixedRow(8): usamos los 4 primeros
        val player0 = IntArray(10) { i -> (i * 3 + 2) }
        writeColors(rom, SnesGameRecipes.SMW_FIXED_PC + 0x0C * (8 - 4), fixed)
        writeColors(rom, SnesGameRecipes.SMW_PLAYER_PC, player0)

        val t = SnesGameRecipes.SmwPaletteTables(rom, 0)
        val row = SnesGameRecipes.rowMario(t)
        assertEquals(16, row.size)
        assertEquals(0, row[0])       // transparente
        assertEquals(BLACK, row[1])   // negro
        // fix0..fix3 = 4 primeros de fixedRow(8)
        for (i in 0 until 4) {
            assertEquals(SnesDecoder.bgr15ToArgb(fixed[i]), row[2 + i], "fix $i")
        }
        // pl0..pl9 = player(0)
        for (i in 0 until 10) {
            assertEquals(SnesDecoder.bgr15ToArgb(player0[i]), row[6 + i], "player $i")
        }
    }

    // ------------------------------------------------------ sanidad teselado

    @Test
    fun `las tablas de paleta teselan sin huecos`() {
        assertEquals(SnesGameRecipes.SMW_BG_PC, SnesGameRecipes.SMW_BACK_AREA_PC + 0x10)
        assertEquals(0x3170, SnesGameRecipes.SMW_BG_PC + 0xC0)      // B0B0+0xC0 = B170
        assertEquals(SnesGameRecipes.SMW_FIXED_PC, SnesGameRecipes.SMW_FG_PC + 0xC0)
        assertEquals(SnesGameRecipes.SMW_PLAYER_PC, SnesGameRecipes.SMW_FIXED_PC + 0x78)
        assertEquals(SnesGameRecipes.SMW_SPRITE_PC, SnesGameRecipes.SMW_PLAYER_PC + 0x50)
        assertEquals(0x33D8, SnesGameRecipes.SMW_SPRITE_PC + 0xC0)  // B318+0xC0 = B3D8
    }

    // -------------------------------------------------- sesgo de formato a 3bpp

    /** Anillos concéntricos: píxeles vecinos casi siempre iguales (arte real). */
    private fun ringTilePixels(): IntArray {
        val px = IntArray(64)
        for (y in 0..7) for (x in 0..7) {
            val d = maxOf(kotlin.math.abs(2 * x - 7), kotlin.math.abs(2 * y - 7))
            px[y * 8 + x] = 4 - (d - 1) / 2
        }
        return px
    }

    private fun encode3bpp(pixels: IntArray, tiles: Int): ByteArray {
        val rom = ByteArray(24 * tiles)
        for (t in 0 until tiles) {
            val p = t * 24
            for (y in 0..7) {
                val planes = IntArray(3)
                for (x in 0..7) {
                    val v = pixels[y * 8 + x]
                    for (bit in 0..2) if ((v shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
                rom[p + 2 * y] = planes[0].toByte()
                rom[p + 2 * y + 1] = planes[1].toByte()
                rom[p + 16 + y] = planes[2].toByte()
            }
        }
        return rom
    }

    @Test
    fun `smwFormat elige 3bpp cuando el dibujo es 3bpp`() {
        val data = encode3bpp(ringTilePixels(), tiles = 64)
        assertEquals(SnesGraphicFormat.SNES_3BPP, SnesGameRecipes.smwFormat(data))
    }

    // ------------------------------------------------- extract no revienta

    @Test
    fun `extract sin tabla GFX valida devuelve vacio sin lanzar`() {
        // ROM en blanco: no hay tabla de punteros valida, pero el ensamblado de
        // paletas con delta no debe lanzar aunque la ROM sea pequena.
        val rom = ByteArray(0x8000)
        assertTrue(SnesGameRecipes.extract(rom, headerWith(0x7FC0)).isEmpty())
    }
}
