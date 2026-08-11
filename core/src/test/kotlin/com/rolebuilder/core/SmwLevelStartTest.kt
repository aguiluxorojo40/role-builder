package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEntranceAction
import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SnesDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests del lector del punto de inicio del jugador ([SmwLevelStartReader]).
 * SINTÉTICOS: se plantan las tablas de presets y la cabecera secundaria del nivel
 * 0x106 con sus bytes REALES y se comprueba que la entrada decodifica a la posición
 * verificada contra la ROM: casilla (1, 22). Sin ROM con copyright.
 */
class SmwLevelStartTest {

    // Presets de Y (16) y X (8) reales de la ROM US.
    private val yLo = intArrayOf(0x0, 0x30, 0x60, 0x80, 0xa0, 0xb0, 0xc0, 0xe0, 0x10, 0x30, 0x50, 0x60, 0x70, 0x90, 0x0, 0x0)
    private val yHi = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1)
    private val xLo = intArrayOf(0x10, 0x80, 0x0, 0xe0, 0x10, 0x70, 0x0, 0xe0)
    private val xHi = intArrayOf(0, 0, 0, 0, 1, 1, 1, 1)

    private fun romWithStart(
        secHdr0: Int = 0x5B,
        secHdr1: Int = 0x00,
        mangle: Boolean = false,
        secHdr3: Int = 0x00,
        secHdr2: Int = 0x9A,
    ): ByteArray {
        val rom = ByteArray(0x50000)
        fun put(addr: Int, vals: IntArray) {
            val base = 0x05 * 0x8000 + (addr and 0x7FFF)
            vals.forEachIndexed { i, v -> rom[base + i] = v.toByte() }
        }
        put(0xD730, if (mangle) intArrayOf(0x00, 0x00) else yLo)
        put(0xD740, yHi); put(0xD750, xLo); put(0xD758, xHi)
        // Cabecera secundaria del nivel 0x106.
        val base = 0x05 * 0x8000
        rom[base + (0xF000 and 0x7FFF) + 0x106] = secHdr0.toByte()
        rom[base + (0xF200 and 0x7FFF) + 0x106] = secHdr1.toByte()
        rom[base + (0xF400 and 0x7FFF) + 0x106] = secHdr2.toByte()
        rom[base + (0xF600 and 0x7FFF) + 0x106] = secHdr3.toByte()
        return rom
    }

    @Test
    fun `el inicio del nivel 0x106 decodifica a la casilla verificada`() {
        val rom = romWithStart()
        val start = SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106)
        assertNotNull(start)
        // secHdr0=0x5B → Yidx=0xB → Y = 0x160 = 352 px = fila 22.
        // secHdr1=0x00 → Xidx=0   → X = 0x010 =  16 px = col 1.
        assertEquals(16, start.startPixelX)
        assertEquals(352, start.startPixelY)
        assertEquals(1, start.startTileX)
        assertEquals(22, start.startTileY)
        assertEquals(4, start.secHeader.size)
    }

    @Test
    fun `la PANTALLA de la cabecera pisa el byte alto de la X`() {
        // `LoadLevel` ($05:D796) hace `r1 = $05:F600[nivel]; r1 &= 0x1F;
        // HIBYTE(player_xpos) = r1` DESPUÉS de cargar el preset entero. Pantalla 5 +
        // Xidx=0 (D750=0x10) → X = 0x0510 = 1296 px = columna 81, no la 1.
        val rom = romWithStart(secHdr3 = 0x05)
        val start = assertNotNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106))
        assertEquals(5, start.entranceScreen)
        assertEquals(1296, start.startPixelX)
        assertEquals(81, start.startTileX)
        assertEquals(22, start.startTileY) // la Y no la toca en un nivel horizontal
    }

    @Test
    fun `en un nivel VERTICAL la pantalla va a la Y`() {
        // Bit 0x20 de $05:F600 = `misc_level_layout_flags & 1` = nivel vertical; ahí el
        // mismo `r1 & 0x1F` va a `HIBYTE(player_ypos)` y la X conserva su preset.
        val rom = romWithStart(secHdr1 = 0x07, secHdr3 = 0x25) // vertical + pantalla 5
        val start = assertNotNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106))
        assertEquals(true, start.isVertical)
        assertEquals(5, start.entranceScreen)
        assertEquals(0x1E0, start.startPixelX)  // Xidx=7 → D758=1, D750=0xE0: conserva el alto
        assertEquals(0x560, start.startPixelY)  // pantalla 5 + D730[0xB]=0x60
    }

    @Test
    fun `la entrada del MIDWAY solo cambia la PANTALLA, no el preset`() {
        // Rama `else` de la misma comparación de `LoadLevel` ($05:D796):
        // `HIBYTE(player_xpos) = r2 >> 4` con `r2` = $05:F400[nivel]. Son 4 bits (0..15).
        // F400=0x9A → pantalla 9; el preset X (Xidx=0 → D750=0x10) y TODA la Y no se tocan.
        val rom = romWithStart(secHdr3 = 0x05, secHdr2 = 0x9A) // entrada normal en la pantalla 5
        val start = assertNotNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106))
        assertEquals(9, start.midwayScreen)
        assertEquals(0x910, start.midwayPixelX)  // pantalla 9 + D750[0]=0x10
        assertEquals(145, start.midwayTileX)
        assertEquals(352, start.midwayPixelY)    // la Y es la misma que la de la entrada normal
        assertEquals(22, start.midwayTileY)
        // Y la entrada NORMAL sigue en su pantalla 5, sin contaminarse.
        assertEquals(81, start.startTileX)
    }

    @Test
    fun `en un nivel VERTICAL el midway no le pone pantalla a la Y`() {
        // La rama `else` no distingue verticales: mete `F400 >> 4` en la X igual, y deja la Y
        // en el PRESET crudo — mientras que la entrada normal sí le pone su pantalla a la Y.
        val rom = romWithStart(secHdr3 = 0x25, secHdr2 = 0x30) // vertical, entrada en pantalla 5
        val start = assertNotNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106))
        assertEquals(true, start.isVertical)
        assertEquals(3, start.midwayScreen)
        assertEquals(0x310, start.midwayPixelX)
        assertEquals(0x160, start.midwayPixelY)  // preset crudo (D740[0xB]=1, D730[0xB]=0x60)
        assertEquals(0x560, start.startPixelY)   // la normal sí lleva su pantalla 5
    }

    @Test
    fun `la accion de entrada se clasifica como en 00A6CC`() {
        // (F200 & 0x38) >> 3 = 6 → sales DISPARADO HACIA ARRIBA (`player_current_state = 7`).
        val rom = romWithStart(secHdr1 = 0x30) // 0x30 & 0x38 = 0x30 → >>3 = 6
        val start = assertNotNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106))
        assertEquals(6, start.entranceAction)
        assertEquals(SmwEntranceAction.SHOT_UP_OUT_OF_PIPE, start.entranceActionKind)
        // Y las cuatro direcciones de tubería (1..4) caen todas en OUT_OF_PIPE.
        assertEquals(SmwEntranceAction.WALK_IN, SmwEntranceAction.of(0))
        assertEquals(SmwEntranceAction.OUT_OF_PIPE, SmwEntranceAction.of(1))
        assertEquals(SmwEntranceAction.OUT_OF_PIPE, SmwEntranceAction.of(4))
        assertEquals(SmwEntranceAction.WALK_IN_ICE, SmwEntranceAction.of(5))
        assertEquals(SmwEntranceAction.OUT_OF_PIPE_UNDERWATER, SmwEntranceAction.of(7))
    }

    @Test
    fun `una ROM que no es SMW vanilla devuelve null`() {
        val rom = romWithStart(mangle = true)
        assertNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x106))
    }

    @Test
    fun `un nivel fuera de rango devuelve null`() {
        val rom = romWithStart()
        assertNull(SmwLevelStartReader.read(rom, SnesDecoder.parseHeader(rom), 0x200))
    }
}
