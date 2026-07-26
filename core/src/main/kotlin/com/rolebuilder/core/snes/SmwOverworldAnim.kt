package com.rolebuilder.core.snes

/**
 * ANIMACIÓN DE TESELAS del overworld: el **oleaje del mar** que recorre todo el mapa del
 * mundo. Port 1:1 de `OwTileAnimations` ($04:80E0), `OwTileAnimations_ShiftWaterTiles`
 * ($04:8086), `OwTileAnimations_0480B9` ($04:80B9) y `OwTileAnimations_048172` ($04:8172).
 *
 * ## Cómo funciona en el juego
 * `ShiftWaterTiles` coge **3 teselas de agua** y las expande de 3bpp a 4bpp en un búfer de
 * 96 bytes (`sprites_cutscene_sprite_table_0AF6`, `g_ram+0xAF6`). Cada fotograma,
 * `UploadOverworldExAnimationData` ($00:A4E3) sube ese búfer con
 * `SmwCopyToVram(0x750, g_ram+0xAF6, 0x160)`: VRAM word 0x750 en direccionamiento 4bpp
 * (16 words por tesela) = **tesela 0x75**, así que las 3 teselas animadas son la
 * **0x75, 0x76 y 0x77** — las del mar.
 *
 * Cada 8 fotogramas de juego, `OwTileAnimations` deforma el búfer:
 *  - **Tesela 0 (0x75)**: rotación de BITS byte a byte. Los bytes con el bit 3 del índice
 *    puesto rotan a la DERECHA y el resto a la IZQUIERDA; como en 4bpp los bytes 0-15 son
 *    los planos 0/1 de las filas 0-7, sale que las filas de arriba se desplazan hacia un
 *    lado y las de abajo hacia el otro: el rielado clásico del agua.
 *  - **Tesela 1 (0x76)**: `_048172(0x20)` rota UN word cada grupo de 16 bytes → la tesela
 *    hace scroll VERTICAL de una fila por paso.
 *  - **Tesela 2 (0x77)**: primero rota todos sus bytes a la izquierda y luego `_048172(0x40)`
 *    → combina desplazamiento horizontal y vertical.
 *
 * El ciclo se cierra en **8 pasos** (8 bits de rotación / 8 words por grupo).
 *
 * ## De dónde salen las teselas de origen
 * `kOwTileAnimations_WaterTileNumbers` ($04:8000, 3 words) da las direcciones de RAM de las
 * teselas fuente: `0xB480, 0xB498, 0xB4B0` — espaciadas 24 bytes, o sea teselas 3bpp
 * consecutivas. `GraphicsDecompress` ($00:BA28) descomprime SIEMPRE a `g_ram+0xAD00`, así que
 * esas direcciones son las teselas `(0xB480-0xAD00)/24 = 0x50`, `0x51` y `0x52` del fichero
 * GFX que hubiera en el búfer.
 *
 * Cuál es ese fichero depende de estado de ejecución (el último descomprimido), pero solo hay
 * una respuesta coherente: **GFX 0x1C**. Es el fichero del slot 0 del overworld —el dueño de
 * las teselas destino 0x75-0x77— y sus teselas 0x50-0x52 son justamente el patrón de oleaje
 * (verificado por render: los demás candidatos dan letras o decorados). Ver [WATER_GFX_FILE].
 */
object SmwOverworldAnim {

    /** `kOwTileAnimations_WaterTileNumbers` ($04:8000): 3 words con las direcciones de RAM. */
    const val WATER_SRC_TABLE_SNES = 0x048000
    /** `GraphicsDecompress` ($00:BA28) descomprime siempre a `g_ram + 0xAD00`. */
    const val GFX_DECOMP_RAM_BASE = 0xAD00
    /** Bytes de una tesela 3bpp (para pasar de dirección de RAM a nº de tesela). */
    private const val BYTES_PER_3BPP_TILE = 24
    /** Fichero GFX del que salen las teselas de oleaje (slot 0 del overworld). */
    const val WATER_GFX_FILE = 0x1C
    /** Primera tesela de VRAM que se anima (VRAM word 0x750 en 4bpp = tesela 0x75). */
    const val WATER_VRAM_FIRST_TILE = 0x75
    /** Nº de teselas de agua animadas (0x75, 0x76, 0x77). */
    const val WATER_TILE_COUNT = 3
    /** Pasos del ciclo de animación (8 bits de rotación / 8 words por grupo). */
    const val FRAMES = 8
    /** Fotogramas de juego (60 fps) entre paso y paso: `counter_global_frames & 7`. */
    const val GAME_FRAMES_PER_STEP = 8

    private fun pc(snes: Int, delta: Int): Int =
        (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta

    private fun u8(rom: ByteArray, i: Int): Int = if (i in rom.indices) rom[i].toInt() and 0xFF else 0

    /**
     * Los índices de las 3 teselas 3bpp de ORIGEN, leídos de la tabla de la ROM (no
     * cableados): `(dirección de RAM − 0xAD00) / 24`. En la ROM US salen 0x50, 0x51 y 0x52.
     */
    fun waterSourceTiles(rom: ByteArray, delta: Int): IntArray = IntArray(WATER_TILE_COUNT) { i ->
        val addr = pc(WATER_SRC_TABLE_SNES + 2 * i, delta)
        val ram = u8(rom, addr) or (u8(rom, addr + 1) shl 8)
        (ram - GFX_DECOMP_RAM_BASE) / BYTES_PER_3BPP_TILE
    }

    /** Rota un byte a la izquierda (el bit 7 vuelve al bit 0). */
    private fun rotl(b: Int): Int = ((b shl 1) or (b ushr 7)) and 0xFF

    /** Rota un byte a la derecha (el bit 0 vuelve al bit 7). */
    private fun rotr(b: Int): Int = ((b ushr 1) or ((b and 1) shl 7)) and 0xFF

    /**
     * `OwTileAnimations_048172`: rota UN word (2 bytes) hacia delante en cada grupo de 16
     * bytes, empezando en [k]. Son dos grupos (planos 0/1 y planos 2/3) → scroll vertical.
     */
    private fun cycleWords(buf: IntArray, k: Int) {
        for (group in intArrayOf(k, k + 16)) {
            val last0 = buf[group + 14]; val last1 = buf[group + 15]
            for (i in 7 downTo 1) {
                buf[group + 2 * i] = buf[group + 2 * i - 2]
                buf[group + 2 * i + 1] = buf[group + 2 * i - 1]
            }
            buf[group] = last0; buf[group + 1] = last1
        }
    }

    /** Un paso de [OwTileAnimations] sobre el búfer de 96 bytes (3 teselas de 4bpp). */
    private fun step(buf: IntArray) {
        // Tesela 0: bytes con bit 3 del índice → derecha; el resto → izquierda.
        for (i in 31 downTo 0) buf[i] = if (i and 8 != 0) rotr(buf[i]) else rotl(buf[i])
        // Tesela 1: solo scroll vertical.
        cycleWords(buf, 0x20)
        // Tesela 2: rotación a la izquierda de todos sus bytes + scroll vertical.
        for (j in 31 downTo 0) buf[0x40 + j] = rotl(buf[0x40 + j])
        cycleWords(buf, 0x40)
    }

    /**
     * `OwTileAnimations_0480B9`: expande una tesela 3bpp (24 B) a 4bpp (32 B) en [buf] desde
     * [k]. Los 16 primeros bytes (planos 0 y 1) se copian tal cual; los 8 siguientes (plano 2)
     * se separan a words dejando el plano 3 a cero.
     */
    private fun expand3bppTo4bpp(src: ByteArray, srcOff: Int, buf: IntArray, k: Int) {
        for (i in 0 until 16) buf[k + i] = src[srcOff + i].toInt() and 0xFF
        for (i in 0 until 8) {
            buf[k + 16 + 2 * i] = src[srcOff + 16 + i].toInt() and 0xFF
            buf[k + 16 + 2 * i + 1] = 0
        }
    }

    /** Decodifica una tesela 4bpp del búfer a sus 64 índices de color. */
    private fun decode4bpp(buf: IntArray, tile: Int): IntArray {
        val b = tile * 32
        val px = IntArray(64)
        for (y in 0..7) {
            val p0 = buf[b + 2 * y]; val p1 = buf[b + 2 * y + 1]
            val p2 = buf[b + 16 + 2 * y]; val p3 = buf[b + 16 + 2 * y + 1]
            for (x in 0..7) {
                val s = 7 - x
                px[y * 8 + x] = ((p0 ushr s) and 1) or (((p1 ushr s) and 1) shl 1) or
                    (((p2 ushr s) and 1) shl 2) or (((p3 ushr s) and 1) shl 3)
            }
        }
        return px
    }

    /**
     * Las teselas de agua ANIMADAS en el paso [frame] (0..[FRAMES]-1): número de tesela de
     * VRAM (0x75..0x77) → sus 64 índices de color. Null si no se puede leer el GFX del agua.
     */
    fun waterTiles(rom: ByteArray, delta: Int, frame: Int): Map<Int, IntArray>? {
        val gfx = SnesGameRecipes.smwGfxFileDataPublic(rom, WATER_GFX_FILE) ?: return null
        val srcTiles = waterSourceTiles(rom, delta)
        val buf = IntArray(WATER_TILE_COUNT * 32)
        for (n in 0 until WATER_TILE_COUNT) {
            val off = srcTiles[n] * BYTES_PER_3BPP_TILE
            if (off < 0 || off + BYTES_PER_3BPP_TILE > gfx.size) return null
            expand3bppTo4bpp(gfx, off, buf, n * 32)
        }
        repeat(((frame % FRAMES) + FRAMES) % FRAMES) { step(buf) }
        return (0 until WATER_TILE_COUNT).associate {
            (WATER_VRAM_FIRST_TILE + it) to decode4bpp(buf, it)
        }
    }

    /**
     * Aplica el oleaje del paso [frame] sobre una VRAM del overworld ya montada (la de
     * [SnesGameRecipes.overworldTileVram]), sustituyendo las teselas 0x75-0x77. Devuelve la
     * misma VRAM para encadenar.
     */
    fun applyWater(rom: ByteArray, delta: Int, vram: Array<IntArray?>, frame: Int): Array<IntArray?> {
        waterTiles(rom, delta, frame)?.forEach { (tile, px) ->
            if (tile in vram.indices) vram[tile] = px
        }
        return vram
    }
}
