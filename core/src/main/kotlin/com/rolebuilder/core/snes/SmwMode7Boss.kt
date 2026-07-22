package com.rolebuilder.core.snes

/**
 * Decodificador de los JEFES de MODO 7 de SMW (Bowser y los Koopalings Morton/Roy/Ludwig),
 * que NO se dibujan por OAM sino como bloque del tilemap de Modo 7 (teselas 8×8 de 3bpp).
 *
 * Portado 1:1 de `InitializeMode7TilemapsAndPalettes` / `UpdateMode7SpriteAnimations` del
 * banco $03 (snesrev/smw):
 *  - El GFX del jefe (`kInitializeMode7TilemapsAndPalettes_GFXFile[jefe]`) se descomprime
 *    (LZ2) y se DESEMPAQUETA con `_BufferTilemap`: 24 bytes → una tesela 8×8 de 3bpp
 *    (planos 1&2 intercalados en los 16 primeros bytes, plano 3 en los 8 últimos; el píxel
 *    es `(buf>>5)&7`). Cada bloque de 24 bytes emite la tesela y su espejo horizontal, igual
 *    que el juego al volcarlas a VRAM.
 *  - Un fotograma del jefe es un bloque 4×4 de teselas: 16 números de tesela de
 *    `kInitializeMode7TilemapsAndPalettes_TilemapData` (PC 0x3D9DE, 912 bytes = 57 fotogramas).
 *  - La paleta son 6 colores BGR15 de `kInitializeMode7TilemapsAndPalettes_Pals[jefe]` en los
 *    índices 1–6; el índice 0 es transparente.
 */
object SmwMode7Boss {
    /** Fichero GFX de Modo 7 por grupo de jefe (0 = sin GFX de Modo 7, p.ej. Reznor). */
    private val GFX_FILE = intArrayOf(0xb, 0xb, 0xb, 0x21, 0x0)

    /** 6 colores BGR15 (índices CGRAM 1–6) por grupo de jefe. Índice 0 = transparente. */
    private val PALS = arrayOf(
        intArrayOf(0x0, 0x1e0, 0x2e0, 0x3e0, 0xb7, 0x23f),
        intArrayOf(0x0, 0x6d08, 0x6dad, 0x7e31, 0xb7, 0x23f),
        intArrayOf(0x0, 0x1ff, 0x31f, 0x3ff, 0xb7, 0x23f),
        intArrayOf(0x0, 0x2940, 0x3de0, 0x5280, 0xb7, 0x23f),
        intArrayOf(0x2e32, 0x4a0d, 0x1088, 0x214a, 0x296d, 0x3dcf),
    )

    private const val TILEMAP_DATA_SNES_PC = 0x3D9DE  // $07:D9DE
    private const val TILEMAP_DATA_LEN = 912
    private const val TILES_PER_FRAME = 16            // bloque 4×4
    const val FRAMES = TILEMAP_DATA_LEN / TILES_PER_FRAME  // 57

    /** Nº de grupos de jefe con GFX de Modo 7 (los que tienen fichero != 0). */
    val bossGroups: List<Int> get() = GFX_FILE.indices.filter { GFX_FILE[it] != 0 }

    /**
     * DESEMPAQUETA todo el fichero GFX de Modo 7 [comp] (ya descomprimido) en teselas 8×8 de
     * 3bpp (cada [IntArray] de 64 con valores 0–7), replicando `_BufferTilemap`. Cada 24 bytes
     * producen la tesela directa y su espejo horizontal, en el mismo orden en que el juego las
     * vuelca a VRAM (así los números de [TilemapData] indexan igual).
     */
    fun decodeCharacter(comp: ByteArray): List<IntArray> {
        val tiles = ArrayList<IntArray>()
        val buf = IntArray(64)
        var p = 0
        // _03DE3C: reparte los 8 bits de comp[p+j] (MSB primero) en buf[k..k+7], insertando
        // cada bit como nuevo bit 7 (buf[k] = (buf[k]>>1) | (bit<<7)). Devuelve j+1.
        fun de3c(k0: Int, j: Int): Int {
            var v2 = comp[p + j].toInt() and 0xFF
            var k = k0
            repeat(8) {
                val carry = (v2 and 0x80) != 0
                v2 = (v2 shl 1) and 0xFF
                buf[k] = (buf[k] ushr 1) or (if (carry) 0x80 else 0)
                k++
            }
            return j + 1
        }
        fun de39(k: Int, j: Int): Int = de3c(k, de3c(k, j))
        while (p + 24 <= comp.size) {
            var v2row = 0
            for (row in 0 until 8) {
                val start = row * 8
                val v12 = de39(start, v2row)            // planos 1&2 (bytes v2row, v2row+1)
                de3c(start, (v12 ushr 1) + 15)          // plano 3 (byte v2row/2 + 16)
                for (px in 0 until 8) buf[start + px] = (buf[start + px] ushr 5) and 7
                v2row = v12
            }
            tiles.add(buf.copyOf(64))                    // tesela directa (row-major)
            val flip = IntArray(64); var o = 0; var i = 7
            while (i < 0x40) { for (d in 0 until 8) flip[o++] = buf[i - d]; i += 8 }
            tiles.add(flip)                              // espejo horizontal
            p += 24
        }
        return tiles
    }

    /** Los 16 números de tesela (bloque 4×4) del fotograma [frame] de la tabla del ROM. */
    private fun frameTiles(rom: ByteArray, delta: Int, frame: Int): IntArray? {
        val base = TILEMAP_DATA_SNES_PC + delta + frame * TILES_PER_FRAME
        if (base < 0 || base + TILES_PER_FRAME > rom.size) return null
        return IntArray(TILES_PER_FRAME) { rom[base + it].toInt() and 0xFF }
    }

    /** Paleta ARGB (8 colores) del grupo de jefe [boss]: 0 transparente, 1–6 de [PALS], 7 fallback. */
    private fun palette(boss: Int): IntArray {
        val p = PALS[boss]
        val out = IntArray(8)
        out[0] = 0                                       // transparente
        for (i in 0 until 6) out[i + 1] = SnesDecoder.bgr15ToArgb(p[i])
        out[7] = SnesDecoder.bgr15ToArgb(0x7FFF)         // blanco (índice sin uso habitual)
        return out
    }

    /**
     * Compone el fotograma [frame] del jefe de Modo 7 [boss] (0–4) como imagen 32×32
     * (bloque 4×4 de teselas 8×8), o null si el jefe no usa GFX de Modo 7 o faltan datos.
     */
    fun bossFrame(rom: ByteArray, header: SnesHeader, boss: Int, frame: Int): ArgbImage? {
        if (boss !in GFX_FILE.indices || GFX_FILE[boss] == 0) return null
        val comp = SnesGameRecipes.smwGfxFileDataPublic(rom, GFX_FILE[boss]) ?: return null
        val tiles = decodeCharacter(comp)
        val delta = header.headerOffset - 0x7FC0
        val map = frameTiles(rom, delta, frame) ?: return null
        val pal = palette(boss)
        val img = ArgbImage(32, 32)
        for (r in 0 until 4) for (c in 0 until 4) {
            val t = map[r * 4 + c]
            val px = tiles.getOrNull(t) ?: continue
            for (y in 0 until 8) for (x in 0 until 8) {
                val ci = px[y * 8 + x]
                if (ci == 0) continue
                img.set(c * 8 + x, r * 8 + y, pal[ci])
            }
        }
        return img
    }

    /** DEPURACIÓN: hoja con TODAS las teselas de carácter decodificadas del GFX del jefe. */
    fun characterSheet(rom: ByteArray, boss: Int, cols: Int = 16): ArgbImage? {
        if (boss !in GFX_FILE.indices || GFX_FILE[boss] == 0) return null
        val comp = SnesGameRecipes.smwGfxFileDataPublic(rom, GFX_FILE[boss]) ?: return null
        val tiles = decodeCharacter(comp)
        val pal = palette(boss)
        val rows = (tiles.size + cols - 1) / cols
        val img = ArgbImage(cols * 8, rows * 8)
        for ((t, px) in tiles.withIndex()) {
            val ox = (t % cols) * 8; val oy = (t / cols) * 8
            for (y in 0 until 8) for (x in 0 until 8) {
                val ci = px[y * 8 + x]
                img.set(ox + x, oy + y, if (ci == 0) 0 else pal[ci])
            }
        }
        return img
    }
}
