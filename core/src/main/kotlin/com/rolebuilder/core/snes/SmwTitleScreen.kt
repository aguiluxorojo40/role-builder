package com.rolebuilder.core.snes

/**
 * PANTALLA DE TÍTULO de Super Mario World, reconstruida desde la ROM del usuario.
 *
 * El título no es una imagen guardada: el juego lo monta en `GameMode04_PrepareTitleScreen`
 * ($00:9A8B) con tres piezas:
 *  1. **Layer 3** — el logo "SUPER MARIO WORLD", el marco de lianas y el "© 1990-1991
 *     Nintendo". Se sube como *stripe image* nº 3 (`graphics_stripe_image_to_upload = 3`,
 *     `LoadStripeImage` $00:85D2) al tilemap de BG3 en VRAM $5000.
 *  2. **GFX de Layer 3** — ficheros 0x28..0x2B (`UploadGraphicsFiles_Layer3` $00:A993), en
 *     **2bpp**, 128 teselas cada uno → 512 teselas.
 *  3. **Paleta de título** (`BufferPalettesRoutines_TitleScreen` $00:ADA6): 8 colores de
 *     `$00:B63C` en los índices 8-15 y 8 de `kGlobalPalettes_TS_Layer3` ($00:B62C) en 24-31.
 *     Encajan con las sub-paletas 2,3 y 6,7 que usa el tilemap del logo (BG3 es 2bpp: la
 *     sub-paleta P ocupa los colores P*4..P*4+3).
 *
 * Detrás del logo el juego dibuja un NIVEL normal (con su demo jugándose); esa parte la
 * compone quien llame, usando el pipeline de niveles. Aquí se entrega la capa de título.
 *
 * Como en todo el proyecto: solo COORDENADAS y cómo decodificar; ni un byte con copyright.
 */
object SmwTitleScreen {

    /** Tabla de punteros (24 bits) de las *stripe images*: `$00:84D0 + i*3`. */
    const val STRIPE_TABLE_SNES = 0x0084D0
    /**
     * Índice de la stripe image del título. El juego pide la nº 3 y `LoadStripeImage`
     * indexa la tabla con `nº / 3` → entrada 1.
     */
    const val TITLE_STRIPE_INDEX = 1
    /** Primer fichero GFX de Layer 3 (0x28..0x2B), en 2bpp. */
    const val L3_GFX_FIRST = 0x28
    const val L3_GFX_COUNT = 4
    /** Teselas por fichero de Layer 3 (0x800 bytes / 16 por tesela 2bpp). */
    const val L3_TILES_PER_FILE = 128
    /** Dirección VRAM (en words) del tilemap de BG3: los destinos de la stripe salen de aquí. */
    const val L3_TILEMAP_VRAM = 0x5000
    /** El tilemap de BG3 es 32×32 casillas de 8×8 → 256×256 px. */
    const val L3_TILEMAP_SIZE = 0x400
    const val L3_TILEMAP_COLS = 32

    /** `kGlobalPalettes_DATA_00B63C` ($00:B63C): 8 colores → índices 8-15 (sub-paletas 2 y 3). */
    const val TITLE_PAL_MAIN_PC = 0x363C
    /** `kGlobalPalettes_TS_Layer3` ($00:B62C): 8 colores → índices 24-31 (sub-paletas 6 y 7). */
    const val TITLE_PAL_L3_PC = 0x362C

    private fun pc(snes: Int, delta: Int): Int =
        (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta

    private fun u8(rom: ByteArray, i: Int): Int = if (i in rom.indices) rom[i].toInt() and 0xFF else 0

    /**
     * Paleta del título: 32 colores ARGB (los que usa BG3). Port de
     * `BufferPalettesRoutines_TitleScreen` con el mismo `LoadColors` del resto del juego.
     */
    fun titlePalette(rom: ByteArray, delta: Int): IntArray {
        val pal = IntArray(256)
        fun color(pcAddr: Int, i: Int): Int =
            SnesDecoder.bgr15ToArgb(u8(rom, pcAddr + delta + 2 * i) or (u8(rom, pcAddr + delta + 2 * i + 1) shl 8))
        // LoadColors(_, dstByte=16, cnt=7, rows=0) → 8 colores desde el índice 8.
        for (i in 0..7) pal[8 + i] = color(TITLE_PAL_MAIN_PC, i)
        // LoadColors(_, dstByte=48, cnt=7, rows=0) → 8 colores desde el índice 24.
        for (i in 0..7) pal[24 + i] = color(TITLE_PAL_L3_PC, i)
        return pal
    }

    /**
     * Las 512 teselas de **Layer 3** (GFX 0x28..0x2B en 2bpp), o null si no se pueden leer.
     * Índice = el número de tesela que usa el tilemap del logo.
     */
    fun layer3Tiles(rom: ByteArray): Array<IntArray?>? {
        val out = arrayOfNulls<IntArray>(L3_GFX_COUNT * L3_TILES_PER_FILE)
        var any = false
        for (i in 0 until L3_GFX_COUNT) {
            val data = SnesGameRecipes.smwGfxFileDataPublic(rom, L3_GFX_FIRST + i) ?: continue
            val fmt = SnesGraphicFormat.SNES_2BPP
            val avail = data.size / fmt.bytesPerTile
            for (t in 0 until minOf(avail, L3_TILES_PER_FILE)) {
                out[i * L3_TILES_PER_FILE + t] =
                    SnesDecoder.decodeTile(data, t * fmt.bytesPerTile, fmt, t).pixelIndices
                any = true
            }
        }
        return if (any) out else null
    }

    /**
     * Descodifica la *stripe image* del título al tilemap de BG3 (0x400 casillas). Formato
     * de stripe (`LoadStripeImage` $00:85D2): bloques de cabecera de 4 bytes
     * `[dirAlta, dirBaja, flags+lenAlta, lenBaja]` — dirección VRAM en words, `flags & 0x40`
     * = run RLE (siguen 2 bytes que se repiten), `flags & 0x80` = escritura VERTICAL (avanza
     * de 32 en 32), longitud = `((flags<<8 | lenBaja) & 0x3FFF) + 1` BYTES. Un byte con el
     * bit 7 puesto termina la lista.
     */
    fun titleTilemap(rom: ByteArray, delta: Int): IntArray {
        val tm = IntArray(L3_TILEMAP_SIZE)
        val ptrPc = pc(STRIPE_TABLE_SNES + TITLE_STRIPE_INDEX * 3, delta)
        val snes = u8(rom, ptrPc) or (u8(rom, ptrPc + 1) shl 8) or (u8(rom, ptrPc + 2) shl 16)
        var ea = pc(snes, delta)
        var guard = 0
        while (ea + 3 < rom.size && guard++ < 256) {
            val b = u8(rom, ea)
            if (b and 0x80 != 0) break // fin de la stripe
            val p1 = u8(rom, ea + 1); val p2 = u8(rom, ea + 2); val p3 = u8(rom, ea + 3)
            val vram = (b shl 8) or p1
            val rle = p2 and 0x40 != 0
            val vertical = p2 and 0x80 != 0
            val len = (((p2 shl 8) or p3) and 0x3FFF) + 1
            ea += 4
            val dataAt = ea
            ea += if (rle) 2 else len
            val step = if (vertical) L3_TILEMAP_COLS else 1
            val pos = vram - L3_TILEMAP_VRAM
            // Cada casilla del tilemap son 2 bytes (tesela + propiedades).
            for (k in 0 until len / 2) {
                val lo: Int; val hi: Int
                if (rle) { lo = u8(rom, dataAt); hi = u8(rom, dataAt + 1) }
                else { lo = u8(rom, dataAt + k * 2); hi = u8(rom, dataAt + k * 2 + 1) }
                val idx = pos + k * step
                if (idx in 0 until L3_TILEMAP_SIZE) tm[idx] = lo or (hi shl 8)
            }
        }
        return tm
    }

    /**
     * Nivel que el juego dibuja DETRÁS del título (donde corre la demo). En la ROM vanilla
     * es el 0x0C7; `GameMode04_PrepareTitleScreen` lo carga con `GameMode12_PrepareLevel`.
     */
    const val TITLE_LEVEL = 0x0C7

    /**
     * La pantalla de título COMPLETA: el nivel de fondo con el logo compuesto encima, en
     * 256×256 (la ventana de la SNES). Si el nivel de fondo no se puede reconstruir, cae al
     * fondo liso de la paleta de título. Null si la ROM no es SMW.
     */
    fun renderComplete(rom: ByteArray, header: SnesHeader): ArgbImage? {
        val layer = render(rom, header) ?: return null
        val out = ArgbImage(layer.width, layer.height)
        val bg = runCatching {
            SnesGameRecipes.renderSmwLevelScene(rom, header, TITLE_LEVEL, maxCols = 32)?.image
        }.getOrNull()
        // Fondo: el nivel del título recortado a la ventana; si no, el color 0 de su paleta.
        val fallback = titlePalette(rom, header.headerOffset - 0x7FC0)[8]
        for (y in 0 until out.height) for (x in 0 until out.width) {
            out.set(x, y, if (bg != null && x < bg.width && y < bg.height) bg.get(x, y) else fallback)
        }
        // Logo encima: los píxeles transparentes de la capa dejan ver el fondo.
        for (i in layer.pixels.indices) {
            val p = layer.pixels[i]
            if (p ushr 24 != 0) out.pixels[i] = p
        }
        return out
    }

    /**
     * La **capa de título** (256×256): logo "SUPER MARIO WORLD", marco de lianas y el aviso
     * de copyright, con sus GFX y paleta reales. Las casillas vacías quedan TRANSPARENTES
     * (alfa 0) para poder componerlas sobre el nivel de fondo. Null si la ROM no es SMW.
     */
    fun render(rom: ByteArray, header: SnesHeader): ArgbImage? {
        val delta = header.headerOffset - 0x7FC0
        val tiles = layer3Tiles(rom) ?: return null
        val pal = titlePalette(rom, delta)
        val tm = titleTilemap(rom, delta)
        if (tm.all { it == 0 }) return null
        val img = ArgbImage(L3_TILEMAP_COLS * 8, (L3_TILEMAP_SIZE / L3_TILEMAP_COLS) * 8)
        for (i in tm.indices) {
            val w = tm[i]
            if (w == 0) continue
            val t = w and 0x3FF
            // BG3 es 2bpp: la sub-paleta P ocupa 4 colores (P*4 .. P*4+3).
            val base = ((w shr 10) and 7) * 4
            val hFlip = w and 0x4000 != 0
            val vFlip = w and 0x8000 != 0
            val px = tiles.getOrNull(t) ?: continue
            val ox = (i % L3_TILEMAP_COLS) * 8; val oy = (i / L3_TILEMAP_COLS) * 8
            for (yy in 0..7) for (xx in 0..7) {
                val sx = if (hFlip) 7 - xx else xx
                val sy = if (vFlip) 7 - yy else yy
                val ci = px[sy * 8 + sx]
                if (ci != 0) img.set(ox + xx, oy + yy, pal[base + ci])
            }
        }
        return img
    }
}
