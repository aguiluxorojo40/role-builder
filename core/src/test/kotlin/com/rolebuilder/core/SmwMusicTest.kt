package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwMusic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests del extractor/ensamblador de música N-SPC ([SmwMusic]).
 *
 * SINTÉTICOS y sin ROM: se plantan bloques de subida con el formato N-SPC real
 * (`[tamaño u16][destino u16][datos]…[0x0000][entry u16]`) en una ROM ficticia
 * en las MISMAS direcciones SNES que usa SMW, y se comprueba el
 * direccionamiento LoROM, el parseo de bloques, el ensamblado de ARAM, la
 * lectura del directorio de muestras, la tabla de canciones y el contenedor
 * `.spc`. No se incluye ningún byte con copyright.
 */
class SmwMusicTest {

    // ---- utilidades para construir datos sintéticos -------------------------

    /** Escribe una región de bloques N-SPC en [rom] en la dirección SNES [snes]. */
    private fun writeBlocks(rom: ByteArray, snes: Int, blocks: List<Pair<Int, ByteArray>>, entry: Int? = null) {
        var pc = SmwMusic.regionPc(snes, 0)
        for ((dest, data) in blocks) {
            rom[pc++] = (data.size and 0xFF).toByte()
            rom[pc++] = ((data.size ushr 8) and 0xFF).toByte()
            rom[pc++] = (dest and 0xFF).toByte()
            rom[pc++] = ((dest ushr 8) and 0xFF).toByte()
            for (b in data) rom[pc++] = b
        }
        if (entry != null) {
            rom[pc++] = 0; rom[pc++] = 0
            rom[pc++] = (entry and 0xFF).toByte()
            rom[pc++] = ((entry ushr 8) and 0xFF).toByte()
        }
    }

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    // ---- direccionamiento ---------------------------------------------------

    @Test
    fun regionPcMapsLoromBanks() {
        // $0E8000 → banco 0x0E * 0x8000 + 0 = 0x70000 (motor).
        assertEquals(0x70000, SmwMusic.regionPc(0x0E8000, 0))
        // $0F8000 → 0x78000 (muestras).
        assertEquals(0x78000, SmwMusic.regionPc(0x0F8000, 0))
        // $03E400 → 3*0x8000 + 0x6400 = 0x1E400 (créditos).
        assertEquals(0x1E400, SmwMusic.regionPc(0x03E400, 0))
        // El delta de cabecera SMC se suma.
        assertEquals(0x70000 + 0x200, SmwMusic.regionPc(0x0E8000, 0x200))
    }

    // ---- parseo de bloques --------------------------------------------------

    @Test
    fun parseBlocksHandlesMultipleBlocksAndEntry() {
        val raw = ByteArray(64)
        // bloque 1: 3 bytes a 0x0500 ; bloque 2: 2 bytes a 0x5570 ; term + entry 0x0500
        var p = 0
        fun blk(dest: Int, data: ByteArray) {
            raw[p++] = (data.size).toByte(); raw[p++] = 0
            raw[p++] = (dest and 0xFF).toByte(); raw[p++] = ((dest ushr 8) and 0xFF).toByte()
            for (b in data) raw[p++] = b
        }
        blk(0x0500, bytes(1, 2, 3))
        blk(0x5570, bytes(0xAA, 0xBB))
        raw[p++] = 0; raw[p++] = 0; raw[p++] = 0x00; raw[p++] = 0x05

        val data = SmwMusic.parseBlocks("t", raw)
        assertEquals(2, data.blocks.size)
        assertEquals(0x0500, data.blocks[0].destAram)
        assertEquals(3, data.blocks[0].data.size)
        assertEquals(0x5570, data.blocks[1].destAram)
        assertEquals(0x0500, data.entryPoint)
    }

    @Test
    fun parseBlocksStopsAtEndWhenNoTerminator() {
        // El motor de SMW no lleva terminador: los bloques ocupan toda la región.
        val raw = ByteArray(4 + 3)
        raw[0] = 3; raw[1] = 0; raw[2] = 0x00; raw[3] = 0x05
        raw[4] = 9; raw[5] = 8; raw[6] = 7
        val data = SmwMusic.parseBlocks("engine", raw)
        assertEquals(1, data.blocks.size)
        assertNull(data.entryPoint)
        assertEquals(0x0500, data.blocks[0].destAram)
        assertEquals(listOf(9, 8, 7), data.blocks[0].data.map { it.toInt() and 0xFF })
    }

    // ---- ensamblado de ARAM + directorio + tabla + spc ----------------------

    /** Construye una ROM sintética con motor, muestras y banco de niveles. */
    private fun syntheticRom(): ByteArray {
        val rom = ByteArray(0x80000)

        // Motor: dos bloques (0x0500, 0x5570), SIN terminador (como en SMW).
        writeBlocks(rom, SmwMusic.ENGINE.snesAddr, listOf(
            0x0500 to bytes(0x11, 0x22, 0x33),
            0x5570 to bytes(0x44, 0x55),
        ), entry = null)

        // Muestras: directorio en 0x8000 (2 entradas) + BRR en 0x8100, con terminador.
        val dir = bytes(
            0x00, 0x81, 0x24, 0x81,   // muestra 0: start=0x8100 loop=0x8124
            0x3F, 0x81, 0x63, 0x81,   // muestra 1: start=0x813F loop=0x8163
        )
        writeBlocks(rom, SmwMusic.SAMPLES.snesAddr, listOf(
            0x8000 to dir,
            0x8100 to ByteArray(0x80) { (it and 0xFF).toByte() },
        ), entry = 0xFFFF)

        // Banco de niveles en 0x1360: tabla de 3 canciones seguida de datos.
        // tabla: id1→0x1366, id2→0x1380, id3→0x13A0 (menor=0x1366 ⇒ 3 entradas).
        val bank = ByteArray(0x100)
        fun w16(off: Int, v: Int) { bank[off] = (v and 0xFF).toByte(); bank[off + 1] = ((v ushr 8) and 0xFF).toByte() }
        w16(0, 0x1366); w16(2, 0x1380); w16(4, 0x13A0)
        writeBlocks(rom, SmwMusic.LEVEL_MUSIC.snesAddr, listOf(0x1360 to bank), entry = null)

        return rom
    }

    @Test
    fun assembleAramPlacesEngineSamplesAndMusic() {
        val rom = syntheticRom()
        val aram = SmwMusic.assembleAram(rom, 0, SmwMusic.LEVEL_MUSIC)
        assertNotNull(aram)
        assertEquals(SmwMusic.ARAM_SIZE, aram.size)
        // Motor.
        assertEquals(0x11, aram[0x0500].toInt() and 0xFF)
        assertEquals(0x44, aram[0x5570].toInt() and 0xFF)
        // Directorio de muestras.
        assertEquals(0x00, aram[0x8000].toInt() and 0xFF)
        assertEquals(0x81, aram[0x8001].toInt() and 0xFF)
        // Tabla de canciones.
        assertEquals(0x66, aram[0x1360].toInt() and 0xFF)
        assertEquals(0x13, aram[0x1361].toInt() and 0xFF)
    }

    @Test
    fun readSampleDirectoryParsesEntries() {
        val rom = syntheticRom()
        val aram = SmwMusic.assembleAram(rom, 0, SmwMusic.LEVEL_MUSIC)!!
        val samples = SmwMusic.readSampleDirectory(aram)
        assertEquals(2, samples.size)
        assertEquals(0x8100, samples[0].startAddr)
        assertEquals(0x8124, samples[0].loopAddr)
        assertEquals(0x24, samples[0].loopOffset)
        assertEquals(0x813F, samples[1].startAddr)
    }

    @Test
    fun songPointersDeducesTableLength() {
        val rom = syntheticRom()
        val aram = SmwMusic.assembleAram(rom, 0, SmwMusic.LEVEL_MUSIC)!!
        val songs = SmwMusic.songPointers(aram)
        assertEquals(3, songs.size)
        assertEquals(listOf(0x1366, 0x1380, 0x13A0), songs)
        assertEquals(3, SmwMusic.songCount(aram))
    }

    @Test
    fun buildSpcProducesWellFormedContainer() {
        val rom = syntheticRom()
        val spc = SmwMusic.buildSpc(rom, 0, SmwMusic.LEVEL_MUSIC, song = 2, title = "Test")
        assertNotNull(spc)
        assertEquals(SmwMusic.SPC_FILE_SIZE, spc.size)
        // Firma.
        val sig = String(spc.copyOfRange(0, 33), Charsets.US_ASCII)
        assertEquals("SNES-SPC700 Sound File Data v0.30", sig)
        // PC = punto de entrada del motor 0x0500.
        assertEquals(0x00, spc[0x25].toInt() and 0xFF)
        assertEquals(0x05, spc[0x26].toInt() and 0xFF)
        // La RAM del .spc contiene la ARAM ensamblada (motor en 0x0500 → 0x100+0x0500).
        assertEquals(0x11, spc[0x100 + 0x0500].toInt() and 0xFF)
        // Puerto de selección de canción precargado (0xF6 → 0x100+0xF6) = song 2.
        assertEquals(2, spc[0x100 + SmwMusic.SONG_SELECT_PORT_ARAM].toInt() and 0xFF)
        // Registro DSP DIR (índice 0x5D) = 0x80 en el bloque DSP (0x10100+0x5D).
        assertEquals(0x80, spc[0x10100 + 0x5D].toInt() and 0xFF)
        // MVOLL/MVOLR = 0x7F.
        assertEquals(0x7F, spc[0x10100 + 0x0C].toInt() and 0xFF)
        assertEquals(0x7F, spc[0x10100 + 0x1C].toInt() and 0xFF)
    }

    @Test
    fun catalogEnumeratesBanks() {
        val rom = syntheticRom()
        val cat = SmwMusic.catalog(rom, 0)
        assertNotNull(cat)
        // El banco de niveles debe aparecer con sus 3 canciones y 2 muestras.
        val level = cat.first { it.bank == SmwMusic.LEVEL_MUSIC }
        assertEquals(3, level.songCount)
        assertEquals(2, level.sampleCount)
    }

    @Test
    fun returnsNullWhenRomTooSmall() {
        val small = ByteArray(0x20000) // no llega a los bancos de música
        assertNull(SmwMusic.assembleAram(small, 0, SmwMusic.LEVEL_MUSIC))
        assertNull(SmwMusic.buildSpc(small, 0, SmwMusic.LEVEL_MUSIC, song = 1))
        assertNull(SmwMusic.readRegion(small, SmwMusic.SAMPLES, 0))
    }
}
