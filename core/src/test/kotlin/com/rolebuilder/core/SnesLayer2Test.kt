package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesHeader
import com.rolebuilder.core.snes.SnesMapping
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

        // Puntero de Layer 2 del nivel 0: banco 0xFF (marca de FONDO real), addr $8100.
        // Cuando el banco es 0xFF los datos viven en el banco $0C, así que
        // dataPc = 0x60000 + (0x8100 - 0x8000) = 0x60100.
        val ptr = SnesGameRecipes.SMW_LAYER2_PTR_PC
        // REGLA REAL portada de galaxyhaxz/smw (add_packed_level_bg) y verificada contra la
        // ROM del usuario: banco==0xFF => hay fondo (en banco $0C). El código antiguo lo tenía
        // AL REVÉS (tomaba 0xFF por "vacío" y 0x06/0x07 por fondos), y este test blindaba el
        // bug poniendo 0x06 aquí.
        rom[ptr] = 0x00; rom[ptr + 1] = 0x81.toByte(); rom[ptr + 2] = 0xFF.toByte()

        // Datos de fondo en 0x60100: copia directa de 4 bloques [5,6,5,6] + fin (FF FF).
        val dataPc = SnesGameRecipes.SMW_BG_BANK_PC + (0x8100 - 0x8000)
        rom[dataPc] = 0x03           // copia directa, L+1 = 4
        rom[dataPc + 1] = 5; rom[dataPc + 2] = 6; rom[dataPc + 3] = 5; rom[dataPc + 4] = 6
        rom[dataPc + 5] = 0xFF.toByte(); rom[dataPc + 6] = 0xFF.toByte() // fin: doble 0xFF

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

        // Y como diagnóstico explícito: es Success, banco 0xFF y página 0 (addr $8100 < $E8FE).
        val r = SnesGameRecipes.layer2BgParse(rom, 0, 0)
        assertTrue(
            r is SnesGameRecipes.BgParse.Success && r.bank == 0xFF && r.page == 0,
            "debe ser Success con banco 0xFF y página 0: $r",
        )
    }

    @Test
    fun `un banco distinto de 0xFF es Layer 2 de OBJETOS, no un fondo`() {
        // Banco 0x06/0x07 = el Layer 2 son objetos (datos de nivel en ese banco), no una imagen
        // de fondo comprimida. VERIFICADO en ROM: los 26 slots 0x06/0x07 desbordan si se leen
        // como RLE desde $0C, justo el bug anterior. Deben clasificarse como "sin fondo".
        val rom = ByteArray(0x70000)
        val ptr = SnesGameRecipes.SMW_LAYER2_PTR_PC
        for (bank in intArrayOf(0x06, 0x07, 0x0C)) {
            rom[ptr] = 0x00; rom[ptr + 1] = 0x81.toByte(); rom[ptr + 2] = bank.toByte()
            val r = SnesGameRecipes.layer2BgParse(rom, 0, 0)
            assertTrue(r is SnesGameRecipes.BgParse.NoBackground, "banco 0x%02X debe ser sin fondo".format(bank))
            assertTrue(SnesGameRecipes.layer2BgEntries(rom, 0, 0).isEmpty())
        }
    }

    @Test
    fun `la pagina del Map16 de fondo la decide el umbral de direccion 0xE8FE`() {
        // Portado del juego: page = (addr >= $E8FE). Dos fondos con addr a cada lado del umbral.
        fun parseAt(addr: Int): SnesGameRecipes.BgParse {
            val rom = ByteArray(0x70000)
            val ptr = SnesGameRecipes.SMW_LAYER2_PTR_PC
            rom[ptr] = (addr and 0xFF).toByte(); rom[ptr + 1] = ((addr shr 8) and 0xFF).toByte()
            rom[ptr + 2] = 0xFF.toByte()
            val dataPc = SnesGameRecipes.SMW_BG_BANK_PC + (addr - 0x8000)
            rom[dataPc] = 0x03                                   // copia directa de 4 bloques
            rom[dataPc + 1] = 0; rom[dataPc + 2] = 0; rom[dataPc + 3] = 0; rom[dataPc + 4] = 0
            rom[dataPc + 5] = 0xFF.toByte(); rom[dataPc + 6] = 0xFF.toByte()
            return SnesGameRecipes.layer2BgParse(rom, 0, 0)
        }
        val below = parseAt(0xE8FD)   // justo por debajo → página 0
        val atOr = parseAt(0xE8FE)    // en el umbral → página 1
        assertTrue(below is SnesGameRecipes.BgParse.Success && below.page == 0, "addr \$E8FD → página 0")
        assertTrue(atOr is SnesGameRecipes.BgParse.Success && atOr.page == 1, "addr \$E8FE → página 1")
    }

    @Test
    fun `un fondo sin terminador util no se da por bueno en silencio`() {
        // No-fallo-silencioso: si el banco dice "fondo" (0xFF) pero los datos no descomprimen a
        // algo con sentido, debe ser BgParse.Error, no una lista vacía indistinguible de
        // "sin fondo". Aquí los datos son FF FF inmediato → 0 bloques → Error.
        val rom = ByteArray(0x70000)
        val ptr = SnesGameRecipes.SMW_LAYER2_PTR_PC
        rom[ptr] = 0x00; rom[ptr + 1] = 0x81.toByte(); rom[ptr + 2] = 0xFF.toByte()
        val dataPc = SnesGameRecipes.SMW_BG_BANK_PC + (0x8100 - 0x8000)
        rom[dataPc] = 0xFF.toByte(); rom[dataPc + 1] = 0xFF.toByte()
        val r = SnesGameRecipes.layer2BgParse(rom, 0, 0)
        assertTrue(r is SnesGameRecipes.BgParse.Error, "datos vacíos deben ser Error, no vacío mudo")
    }

    @Test
    fun `smwFgbgVramSlot mapea cada rango de tile al slot GFX correcto`() {
        // Layout VRAM de UploadGraphicsFiles: 4 ficheros en tiles 0x000/0x080/0x100/0x180,
        // 0x80 teselas por slot. Fuera de 0x000..0x1FF no hay slot FG/BG (-1).
        assertEquals(0, SnesGameRecipes.smwFgbgVramSlot(0x000))
        assertEquals(0, SnesGameRecipes.smwFgbgVramSlot(0x07F))
        assertEquals(1, SnesGameRecipes.smwFgbgVramSlot(0x080))
        assertEquals(1, SnesGameRecipes.smwFgbgVramSlot(0x0FF))
        assertEquals(2, SnesGameRecipes.smwFgbgVramSlot(0x100)) // BG1: el único que se mapeaba antes
        assertEquals(2, SnesGameRecipes.smwFgbgVramSlot(0x17F))
        assertEquals(3, SnesGameRecipes.smwFgbgVramSlot(0x180))
        assertEquals(3, SnesGameRecipes.smwFgbgVramSlot(0x1FF))
        assertEquals(-1, SnesGameRecipes.smwFgbgVramSlot(0x200)) // animadas/FG3/sprites: no mapeable
        assertEquals(-1, SnesGameRecipes.smwFgbgVramSlot(-1))
    }

    @Test
    fun `fillSmwAnimatedTiles no revienta sin datos y FrameData mapea a PC 0x2B999`() {
        // La tabla FrameData vive en SNES $05:B999; el mapeo LoROM da PC 0x2B999.
        assertEquals(0x2B999, SnesGameRecipes.SMW_TILEANIM_FRAMEDATA_PC)
        // Con una ROM vacía (sin GFX33 válida) no debe petar; deja el vram intacto.
        val vram = arrayOfNulls<IntArray>(512)
        SnesGameRecipes.fillSmwAnimatedTiles(ByteArray(0x80000), 0, 0, vram)
        assertTrue(vram.all { it == null })
    }

    @Test
    fun `smwAnimatedVramTile marca las regiones de teselas animadas`() {
        // Destinos de HandleLevelTileAnimations: tiles 0x40-0x83, 0xDA-0xDD, 0xEA-0xED.
        assertTrue(SnesGameRecipes.smwAnimatedVramTile(0x40))
        assertTrue(SnesGameRecipes.smwAnimatedVramTile(0x83))
        assertTrue(SnesGameRecipes.smwAnimatedVramTile(0xDA))
        assertTrue(SnesGameRecipes.smwAnimatedVramTile(0xEC))
        // Terreno normal: NO animado.
        assertTrue(!SnesGameRecipes.smwAnimatedVramTile(0x3F))
        assertTrue(!SnesGameRecipes.smwAnimatedVramTile(0x84))
        assertTrue(!SnesGameRecipes.smwAnimatedVramTile(0x100))
        assertTrue(!SnesGameRecipes.smwAnimatedVramTile(0x192))
    }

    @Test
    fun `extractSmwLevelAsMap no revienta sin datos`() {
        val header = SnesHeader(
            title = "SUPER MARIOWORLD", isFastRom = false, romTypeDescription = "",
            romSizeBytes = 0, sramSizeBytes = 0, country = "", licensee = "", version = 0,
            checksum = 0, checksumComplement = 0, isChecksumValid = true,
            mapping = SnesMapping.LOROM, headerOffset = 0x7FC0,
        )
        assertTrue(SnesGameRecipes.extractSmwLevelAsMap(ByteArray(0x80000), header, 0x106) == null)
    }

    @Test
    fun `renderSmwBackground devuelve null cuando el nivel no tiene fondo`() {
        // Motor de escenas de fondo (base guardada): sin datos de fondo válidos, no
        // revienta, devuelve null.
        val header = SnesHeader(
            title = "SUPER MARIOWORLD", isFastRom = false, romTypeDescription = "",
            romSizeBytes = 0, sramSizeBytes = 0, country = "", licensee = "", version = 0,
            checksum = 0, checksumComplement = 0, isChecksumValid = true,
            mapping = SnesMapping.LOROM, headerOffset = 0x7FC0,
        )
        assertTrue(SnesGameRecipes.renderSmwBackground(ByteArray(0x70000), header, 0) == null)
    }

}
