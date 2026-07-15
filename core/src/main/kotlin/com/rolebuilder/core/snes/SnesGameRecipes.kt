package com.rolebuilder.core.snes

import com.rolebuilder.core.snes.compression.LcLz2

/**
 * "Recetas por juego": para ROMs concretas y conocidas sabemos EXACTAMENTE dónde
 * están sus gráficos (p. ej. la tabla de punteros de Super Mario World), así que
 * podemos ofrecer una extracción limpia de un solo toque, sin que el usuario
 * toque offsets ni formatos. Es la vía FIABLE para el "modo fácil": a diferencia
 * de la búsqueda automática a ciegas, aquí no hay ruido porque seguimos el mapa
 * real del juego.
 */
object SnesGameRecipes {

    /** Nombre legible del juego reconocido, o null si no hay receta para esta ROM. */
    fun detect(header: SnesHeader): String? {
        val title = header.title.trim().uppercase()
        return when {
            title.startsWith("SUPER MARIOWORLD") || title.startsWith("SUPER MARIO WORLD") -> "Super Mario World"
            else -> null
        }
    }

    /**
     * Extrae los gráficos del juego reconocido como una galería limpia y en color.
     * Devuelve vacío si no hay receta o no se pudo localizar su mapa gráfico.
     */
    fun extract(rom: ByteArray, header: SnesHeader): List<SnesAutoExtractor.Finding> = when (detect(header)) {
        // La galería entrega: (1) ESCENAS de nivel reales — tilemap reconstruido con el
        // parser de objetos de Layer 1 (port de las rutinas del juego, validado 831/832
        // celdas contra el buffer $7EC800 de un emulador) pintado con los gráficos del
        // nivel y su CGRAM ensamblada; (2) los FONDOS (Layer 2) con color real por tesela
        // —ya con el layout VRAM resuelto: los 4 ficheros GFX del tileset ocupan los
        // tiles 0x000/0x080/0x100/0x180, así que el fondo se decodifica contra la VRAM
        // completa, no solo el slot BG1—; (3) el tileset Map16 acotado a los bloques que
        // el nivel usa de verdad; (4) las HOJAS por categoría con la paleta real por
        // fichero; (5) Mario con su fila real.
        "Super Mario World" ->
            extractSmwScenes(rom, header) +
                extractSmwBackgrounds(rom, header) +
                listOfNotNull(extractSmwMap16Tileset(rom, header)) +
                extractSmw(rom, header)
        else -> emptyList()
    }

    /**
     * Slot GFX (0..3) al que pertenece un número de tesela de VRAM en un nivel de SMW,
     * o -1 si cae fuera de los 4 slots FG/BG. Derivado 1:1 de UploadGraphicsFiles
     * ($00:A9DA): los 4 ficheros del tileset se suben a las direcciones VRAM
     * 0x0000/0x0800/0x1000/0x1800; a 4bpp (0x10 palabras/tesela) eso son las teselas
     * 0x000/0x080/0x100/0x180, 0x80 por slot. FONDO y primer plano comparten esta VRAM.
     */
    internal fun smwFgbgVramSlot(tileIndex: Int): Int =
        if (tileIndex in 0 until 512) tileIndex / 128 else -1

    /**
     * ¿Es [tileIndex] una tesela ANIMADA? SMW reescribe cada pocos frames unas regiones
     * de VRAM (monedas, bloques ?, agua…) desde GFX32 (ver HandleLevelTileAnimations,
     * $05:BB39). Sus destinos son las direcciones word 0x400..0x800 (+0xDA0/0xEA0), es
     * decir tiles 0x40..0x83, 0xDA..0xDD y 0xEA..0xED. En un render estático su GFX base
     * no es su aspecto de juego; las tratamos aparte.
     */
    internal fun smwAnimatedVramTile(tileIndex: Int): Boolean {
        val t = tileIndex and 0x1FF
        return t in 0x40..0x83 || t in 0xDA..0xDD || t in 0xEA..0xED
    }

    // ------------------------ teselas ANIMADAS (monedas, bloques ?, agua) ------------------------
    // Port de HandleLevelTileAnimations ($05:BB39): cada frame el juego copia teselas de
    // GFX33 (descomprimida y expandida a 4bpp en g_ram, hacia abajo desde ~0xACFE) a los
    // 19 destinos VRAM de abajo. FrameData (SNES $05:B999 → PC) da el offset g_ram fuente
    // por tesela y frame. Para el render estático usamos el frame 0 (R0_W=0).

    /** Destinos VRAM (word addr) de las 19 teselas animadas; /16 = nº de tesela. */
    private val SMW_ANIM_DEST = intArrayOf(
        0x600, 0x640, 0x680, 0x740, 0xEA0, 0x800, 0x500, 0x540, 0x580, 0x5C0,
        0x780, 0x7C0, 0xDA0, 0x6C0, 0x700, 0x4C0, 0x440, 0x480, 0x400,
    )
    /** Tipo por destino (DATA_05B96B): 2 = índice de fuente dependiente del tileset. */
    private val SMW_ANIM_TYPE = intArrayOf(0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2)
    /** Desplazamiento de fuente por tileset para los de tipo 2 (DATA_05B98B). */
    private val SMW_ANIM_TS = intArrayOf(0, 5, 0xA, 0xF, 0x14, 0x14, 0x19, 0x14, 0xA, 0x14, 0, 5, 0, 0x14)

    /** FrameData: $05:B999 → PC 0x2B999 (208 words, offsets g_ram por tesela×frame). */
    internal const val SMW_TILEANIM_FRAMEDATA_PC = 0x2B999
    /** Base en g_ram del buffer GFX33 expandido a 4bpp (escrito hacia abajo desde 0xACFE). */
    private const val SMW_ANIM_SRC_BASE = 0x7D00

    /** Descomprime GFX33 (fuente de las animadas): va contigua tras GFX32/Mario. */
    private fun decompressSmwGfx33(rom: ByteArray, delta: Int): ByteArray? {
        val mpc = lorom(
            byte(rom, SMW_GFX32_LO_PC + delta), byte(rom, SMW_GFX32_HI_PC + delta),
            byte(rom, SMW_GFX32_BANK_PC + delta),
        )
        if (mpc < 0x40000 || mpc >= rom.size) return null
        val g32 = runCatching { LcLz2.decompress(rom, mpc) }.getOrNull() ?: return null
        return runCatching { LcLz2.decompress(rom, mpc + g32.consumedBytes).data }.getOrNull()
    }

    /** Nº de fotogramas de la animación de teselas de SMW (monedas, bloques ?, agua). */
    internal const val SMW_TILEANIM_FRAMES = 4
    /** Fotogramas de juego (60 fps) que dura cada fotograma de la animación de teselas. */
    internal const val SMW_TILEANIM_PERIOD = 8

    /** Rellena en [vram] las 19 teselas animadas (4 teselas cada una) con el [frame] dado (0..3). */
    internal fun fillSmwAnimatedTiles(rom: ByteArray, delta: Int, tileset: Int, vram: Array<IntArray?>, frame: Int = 0) {
        val g33 = decompressSmwGfx33(rom, delta) ?: return
        val fmt = SnesGraphicFormat.SNES_3BPP
        val avail = SnesAssetExtractor.availableTiles(g33.size, 0, fmt)
        val fdPc = SMW_TILEANIM_FRAMEDATA_PC + delta
        fun fd(i: Int) = byte(rom, fdPc + 2 * i) or (byte(rom, fdPc + 2 * i + 1) shl 8)
        val fr = frame.coerceIn(0, SMW_TILEANIM_FRAMES - 1)
        for (s in SMW_ANIM_DEST.indices) {
            val v3 = if (SMW_ANIM_TYPE[s] == 2) SMW_ANIM_TS[tileset.coerceIn(0, SMW_ANIM_TS.size - 1)] + s else s
            val source = fd(4 * v3 + fr)
            val tile0 = (source - SMW_ANIM_SRC_BASE) / 32
            val destTile = SMW_ANIM_DEST[s] / 16
            for (i in 0..3) {
                val t = tile0 + i
                if (destTile + i >= vram.size) continue
                // Si la fuente cae fuera de GFX33, deja la tesela de cielo (null) en vez
                // del GFX estático (que sería basura), igual que el blanqueo anterior.
                vram[destTile + i] =
                    if (t in 0 until avail) SnesDecoder.decodeTile(g33, t * fmt.bytesPerTile, fmt, t).pixelIndices else null
            }
        }
    }

    /** PC del inicio de los datos de Layer 1 del nivel (cabecera de 5 bytes + objetos). */
    internal fun smwLayer1DataPc(rom: ByteArray, delta: Int, level: Int): Int? {
        val l1 = SMW_LAYER1_PTR_PC + delta + 3 * level
        if (l1 + 2 >= rom.size) return null
        val pc = lorom(byte(rom, l1), byte(rom, l1 + 1), byte(rom, l1 + 2))
        return if (pc >= 0 && pc + 5 < rom.size) pc else null
    }

    /**
     * Tabla de definiciones Map16 por TILESET: PC de la definición (8 bytes) de cada
     * bloque 0..511. Port de InitializeMap16Pointers ($05:81FB, vía snesrev/smw): las
     * definiciones NO son lineales; una máscara de bits decide bloque a bloque si la
     * definición viene de la zona COMÚN ($0D:8000+) o de la zona ESPECÍFICA del
     * tileset (p.ej. pradera $0D:8B70+). Con la tabla equivocada salen bloques
     * "sucios" — este era el fallo del render Map16 lineal.
     */
    internal fun smwMap16DefTable(rom: ByteArray, delta: Int, tileset: Int): IntArray {
        val ptrs = IntArray(512)
        var common = 0x8000
        var specific = SMW_TILESET_MAP16_PTRS[tileset and 0xF]
        var idx = 0
        for (v0 in 0 until 64) {
            var bits = SMW_MAP16_COMMON_MASK[v0]
            repeat(8) {
                val fromCommon = bits and 0x80 != 0
                bits = (bits shl 1) and 0xFF
                if (fromCommon) { ptrs[idx] = common; common += 8 } else { ptrs[idx] = specific; specific += 8 }
                idx++
            }
        }
        // Pradera/etc. (tileset 0 y 7): 8 bloques de animación con definiciones propias.
        if (tileset == 0 || tileset == 7) {
            var addr = 0x8A70
            for (b in 452..455) { ptrs[b] = addr; addr += 8 }
            for (b in 492..495) { ptrs[b] = addr; addr += 8 }
        }
        // SNES $0D:8000+x → PC 0x68000 + x.
        return IntArray(512) { 0x68000 + delta + (ptrs[it] - 0x8000) }
    }

    /** Máscara (64 bytes, MSB primero): bit=1 → definición común, 0 → del tileset. */
    private val SMW_MAP16_COMMON_MASK = intArrayOf(
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xe0, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0xfe, 0x00, 0x7f, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xe0, 0x00, 0x00, 0x03, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
    )

    /** Base SNES (banco $0D) de las definiciones Map16 específicas de cada tileset. */
    private val SMW_TILESET_MAP16_PTRS = intArrayOf(
        0x8B70, 0xBC00, 0xC800, 0xD400, 0xE300, 0xE300, 0xC800, 0x8B70,
        0xC800, 0xD400, 0xD400, 0xD400, 0x8B70, 0xE300, 0xD400, 0xD400,
    )

    /**
     * ESCENA de un nivel: reconstruye el tilemap Map16 con el parser de objetos de
     * Layer 1 ([SmwLayer1], port del juego) y lo pinta con los gráficos del nivel y
     * su CGRAM ensamblada. Devuelve null si el nivel no es renderizable con fidelidad
     * (vertical, tileset sin rutinas, o demasiados objetos sin portar).
     */
    /**
     * Niveles "escaparate" para las ESCENAS de la galería: lista CURADA de niveles
     * REALES y reconocibles del juego (praderas, colinas de Yoshi's Island, cielo,
     * cueva, castillo, casa fantasma). Se excluyen a propósito los niveles de PRUEBA/
     * utilidad de la ROM vanilla —sobre todo 0x104 ("Side Exit Enabled / TEST") y el
     * bloque 0x107-0x13B, lleno de slots de test y escenas a medias— que ensuciaban la
     * galería con mapeados de test. Cada uno verificado sobre la ROM US: los cubre el
     * parser al 100% y salen limpios (sin bloques placeholder magenta). Praderas,
     * cueva y cielo — escaparate variado y reconocible.
     */
    private val SMW_SCENE_LEVELS = intArrayOf(
        0x105, 0x106, 0x101, 0x0DE, 0x024, 0x0C7, 0x022, 0x0C5,
    )

    /**
     * ESCENAS para la galería: renderiza los niveles escaparate ([SMW_SCENE_LEVELS])
     * que el parser cubre al 100% (0 objetos sin portar), recortadas a [maxCols]
     * columnas para no disparar la memoria del dispositivo. Máximo [maxScenes] escenas.
     */
    internal fun extractSmwScenes(rom: ByteArray, header: SnesHeader, maxScenes: Int = 4, maxCols: Int = 96): List<SnesAutoExtractor.Finding> {
        val out = ArrayList<SnesAutoExtractor.Finding>()
        for (level in SMW_SCENE_LEVELS) {
            if (out.size >= maxScenes) break
            val f = renderSmwLevelScene(rom, header, level, maxCols) ?: continue
            out.add(f)
        }
        return out
    }

    internal fun renderSmwLevelScene(rom: ByteArray, header: SnesHeader, level: Int, maxCols: Int = Int.MAX_VALUE): SnesAutoExtractor.Finding? {
        val delta = smwHeaderDelta(header)
        val tm = SmwLayer1.parse(rom, delta, level) ?: return null
        // Gate de honestidad: si el parser se saltó objetos, la escena tendría huecos.
        if (tm.totalObjects == 0 || tm.unknownObjects * 10 > tm.totalObjects) return null

        // Capa de sprites (enemigos): tercer flujo de datos del nivel, independiente
        // del tilemap. Solo la CUENTA se refleja en la etiqueta; el escenario no cambia.
        val spriteCount = SmwSprites.parse(rom, delta, level)?.sprites?.size ?: 0

        val (bLo, bHi, bBank) = findSmwGfxTable(rom) ?: return null
        val lpc = smwLayer1DataPc(rom, delta, level) ?: return null
        val fgbgSetting = byte(rom, lpc + 4) and 0x0F
        val se = SMW_FGBG_GFX_TABLE_PC + delta + 4 * fgbgSetting
        val slotFiles = intArrayOf(SMW_FG1_GFX, SMW_FG2_GFX, byte(rom, se + 2), byte(rom, se + 3))
        val vram = arrayOfNulls<IntArray>(512)
        for (s in 0..3) {
            val file = slotFiles[s]; if (file == SMW_SLOT_EMPTY) continue
            val pc = lorom(byte(rom, bLo + file), byte(rom, bHi + file), byte(rom, bBank + file))
            if (pc < 0x40000 || pc >= rom.size) continue
            val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
            val fmt = SnesGraphicsScanner.detectBestFormat(data, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
            val avail = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
            for (t in 0 until minOf(avail, 128)) {
                vram[s * 128 + t] = SnesDecoder.decodeTile(data, t * fmt.bytesPerTile, fmt, t).pixelIndices
            }
        }
        if (vram.all { it == null }) return null
        // Teselas animadas (monedas, bloques ?, agua…): se cargan de GFX33 en su frame 0,
        // como hace el motor, en vez de dejarlas de cielo.
        fillSmwAnimatedTiles(rom, delta, tm.tileset, vram)
        val cgram = assembleSmwCgram(rom, delta, level)
        val defs = smwMap16DefTable(rom, delta, tm.tileset)

        // Recorta a las columnas con contenido real (los niveles no llenan 32 pantallas).
        val totalCols = tm.screens * 16
        var lastCol = -1
        for (c in 0 until totalCols) for (r in 0..26) {
            val b = tm.block(c, r); if (b > 0 && b != 0x25) { lastCol = maxOf(lastCol, c); break }
        }
        if (lastCol < 3) return null
        val cols = minOf(lastCol + 2, totalCols, maxCols)
        val rows = 27
        val img = ArgbImage(cols * 16, rows * 16)
        // Fondo (Layer 2) COMPUESTO detrás del primer plano: si el nivel tiene fondo de
        // imagen, se tila a lo ancho como telón; si no, cielo plano (back area). El primer
        // plano se dibuja encima y sus píxeles transparentes (índice 0) dejan ver el fondo.
        val bg = renderSmwBackground(rom, header, level)
        if (bg != null) {
            val bw = bg.width; val bh = bg.height
            for (y in 0 until img.height) for (x in 0 until img.width) {
                img.pixels[y * img.width + x] = bg.pixels[(y % bh) * bw + (x % bw)]
            }
        } else {
            for (i in img.pixels.indices) img.pixels[i] = cgram[0] // cielo = back area color
        }
        for (y in 0 until rows) for (x in 0 until cols) {
            drawSmwBlock(rom, defs, tm.block(x, y), vram, cgram, img, x * 16, y * 16)
        }
        return SnesAutoExtractor.Finding(
            image = img,
            label = "Escena nivel ${level.toString(16).uppercase()}" +
                if (spriteCount > 0) " · $spriteCount sprites" else "",
            offset = lpc,
            compressed = false, format = SnesGraphicFormat.SNES_3BPP, palette = IntArray(0),
            tileCount = cols * rows, columns = cols, score = 1.0,
        )
    }

    private val SMW_BLOCK_SUBPOS = arrayOf(intArrayOf(0, 0), intArrayOf(0, 8), intArrayOf(8, 0), intArrayOf(8, 8))

    /** Dibuja un bloque Map16 (16×16) en [img] en (ox,oy) con la VRAM y CGRAM del nivel. */
    private fun drawSmwBlock(
        rom: ByteArray, defs: IntArray, block: Int, vram: Array<IntArray?>, cgram: IntArray,
        img: ArgbImage, ox: Int, oy: Int,
    ) {
        if (block <= 0 || block >= 0x200) return
        val o = defs[block]
        if (o + 8 > rom.size) return
        for (k in 0..3) {
            val word = byte(rom, o + 2 * k) or (byte(rom, o + 2 * k + 1) shl 8)
            val e = SnesTilemap.decodeEntry(word)
            // Las teselas sin contenido en VRAM (ranura vacía o animada sin fuente)
            // quedan null y se saltan solas: no hace falta (ni es correcto) descartar
            // el rango 0xF8-0xFF a mano — son las últimas teselas ESTÁTICAS de FG2 y
            // la tubería diagonal de YI1 las usa (saltarlas abría huecos al cielo).
            val px = vram.getOrNull(e.tileIndex) ?: continue
            val rowP = (e.palette and 7) * 16
            val bx = ox + SMW_BLOCK_SUBPOS[k][0]; val by = oy + SMW_BLOCK_SUBPOS[k][1]
            for (yy in 0..7) for (xx in 0..7) {
                val sx = if (e.hFlip) 7 - xx else xx
                val sy = if (e.vFlip) 7 - yy else yy
                val ci = px[sy * 8 + sx]
                if (ci != 0) img.set(bx + xx, by + yy, cgram[rowP + ci])
            }
        }
    }

    /**
     * Un nivel SMW convertido en MAPA de Role Builder: un tileset (atlas de los bloques
     * Map16 DISTINTOS que usa el nivel, 16×16 cada uno) + el tilemap (índice de tesela por
     * casilla, -1 = aire) + la colisión REAL de la ROM por tesela ([SmwBlockCollision]:
     * bordes de un sentido, sólidos, cuestas y pinchos). Es la vía para que un nivel
     * reconstruido sea contenido jugable —con su tacto de plataformas—, no una imagen
     * troceada.
     */
    class SmwLevelMap(
        val atlas: ArgbImage,
        val columns: Int,
        val rows: Int,
        val passable: List<Boolean>,
        val mapWidth: Int,
        val mapHeight: Int,
        val tiles: List<Int>, // mapWidth*mapHeight, índice en el atlas o -1 (aire)
        /** Solidez REAL por tesela del atlas (ordinal de [SmwSolidity]); para el motor de plataformas. */
        val solidity: List<Int> = emptyList(),
        /** Enemigos del nivel: (id de sprite, x, y) en celdas de 16px, ya recortados al mapa. */
        val enemies: List<Triple<Int, Int, Int>> = emptyList(),
        /** Teselas animadas del atlas (monedas, bloques ?, agua). */
        val animations: List<com.rolebuilder.core.model.TileAnimation> = emptyList(),
        /** Acción interactiva por tesela del atlas (ordinal de [SmwBlockAction]): moneda… */
        val blockActions: List<Int> = emptyList(),
        /**
         * FONDO (Layer 2) por casilla del mapa (mapWidth*mapHeight): índice en el atlas
         * o -1 (sin fondo). Vacío si el nivel no tiene fondo renderizable. Es la capa que
         * se dibuja DEBAJO del primer plano al importar.
         */
        val bgTiles: List<Int> = emptyList(),
    )

    /** ¿La definición del bloque Map16 usa alguna tesela de VRAM animada? */
    private fun blockIsAnimated(rom: ByteArray, defs: IntArray, block: Int): Boolean {
        if (block <= 0 || block >= 0x200) return false
        val o = defs[block]
        if (o + 8 > rom.size) return false
        for (k in 0..3) {
            val word = byte(rom, o + 2 * k) or (byte(rom, o + 2 * k + 1) shl 8)
            val e = SnesTilemap.decodeEntry(word)
            if ((e.tileIndex and 0xFF) in 0xF8..0xFF) continue // slots que drawSmwBlock ignora
            if (smwAnimatedVramTile(e.tileIndex)) return true
        }
        return false
    }

    /** Convierte el [level] SMW en un [SmwLevelMap], o null si no es reconstruible. */
    fun extractSmwLevelAsMap(rom: ByteArray, header: SnesHeader, level: Int, maxCols: Int = 256): SmwLevelMap? {
        val delta = smwHeaderDelta(header)
        val tm = SmwLayer1.parse(rom, delta, level) ?: return null
        if (tm.totalObjects == 0 || tm.unknownObjects * 10 > tm.totalObjects) return null
        val (bLo, bHi, bBank) = findSmwGfxTable(rom) ?: return null
        val lpc = smwLayer1DataPc(rom, delta, level) ?: return null
        val fgbgSetting = byte(rom, lpc + 4) and 0x0F
        val se = SMW_FGBG_GFX_TABLE_PC + delta + 4 * fgbgSetting
        val slotFiles = intArrayOf(SMW_FG1_GFX, SMW_FG2_GFX, byte(rom, se + 2), byte(rom, se + 3))
        val vram = arrayOfNulls<IntArray>(512)
        for (s in 0..3) {
            val file = slotFiles[s]; if (file == SMW_SLOT_EMPTY) continue
            val pc = lorom(byte(rom, bLo + file), byte(rom, bHi + file), byte(rom, bBank + file))
            if (pc < 0x40000 || pc >= rom.size) continue
            val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
            val fmt = SnesGraphicsScanner.detectBestFormat(data, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
            val avail = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
            for (t in 0 until minOf(avail, 128)) {
                vram[s * 128 + t] = SnesDecoder.decodeTile(data, t * fmt.bytesPerTile, fmt, t).pixelIndices
            }
        }
        if (vram.all { it == null }) return null
        // VRAM por fotograma de animación: copia estática (superficial: las teselas
        // animadas se REASIGNAN, no se mutan) con las teselas animadas de ese fotograma.
        val vramFrames = Array(SMW_TILEANIM_FRAMES) { f ->
            vram.copyOf().also { fillSmwAnimatedTiles(rom, delta, tm.tileset, it, f) }
        }
        val cgram = assembleSmwCgram(rom, delta, level)
        val defs = smwMap16DefTable(rom, delta, tm.tileset)

        val totalCols = tm.screens * 16
        var lastCol = -1
        for (c in 0 until totalCols) for (r in 0..26) {
            val b = tm.block(c, r); if (b > 0 && b != 0x25) { lastCol = maxOf(lastCol, c); break }
        }
        if (lastCol < 3) return null
        val w = minOf(lastCol + 2, totalCols, maxCols)
        val h = 27

        // Bloques distintos usados → tesela del atlas. El aire (0x25) queda como -1.
        val blockToTile = HashMap<Int, Int>()
        val orderedBlocks = ArrayList<Int>()
        val tiles = IntArray(w * h) { -1 }
        for (y in 0 until h) for (x in 0 until w) {
            val block = tm.block(x, y)
            if (block <= 0 || block == 0x25 || block >= 0x200) continue
            tiles[y * w + x] = blockToTile.getOrPut(block) { orderedBlocks.add(block); orderedBlocks.size - 1 }
        }
        if (orderedBlocks.isEmpty()) return null

        val columns = 16
        // Bloques que animan: sus fotogramas extra van APÉNDICE al final del atlas, así
        // el frame 0 (la tesela base) mantiene su índice y el mapa no cambia si no se anima.
        val animBlockIdx = orderedBlocks.indices.filter { blockIsAnimated(rom, defs, orderedBlocks[it]) }
        val extraTiles = animBlockIdx.size * (SMW_TILEANIM_FRAMES - 1)
        val fgTileCount = orderedBlocks.size + extraTiles

        // FONDO (Layer 2): se rasteriza el fondo REAL del nivel y se trocea en teselas
        // 16×16 DISTINTAS que se APENDAN al atlas tras el primer plano (así los índices
        // del primer plano no cambian). Cada casilla del mapa de fondo apunta a su tesela
        // del atlas (o -1 = sin fondo). El fondo repite en horizontal como en el juego
        // (columna módulo ancho-del-fondo). Nota: aquí el fondo scrollea 1:1 con el primer
        // plano (SIN paralaje), pero al menos es VISIBLE y editable como Layer 2 — el
        // paralaje real es harina de otro costal.
        val bgMap = IntArray(w * h) { -1 }
        val bgCells = ArrayList<IntArray>() // píxeles ARGB 16×16 de cada tesela de fondo distinta
        val bgImg = renderSmwBackground(rom, header, level)
        if (bgImg != null) {
            val bgCols = bgImg.width / 16; val bgRows = bgImg.height / 16
            if (bgCols > 0 && bgRows > 0) {
                val byKey = HashMap<List<Int>, Int>() // contenido de la tesela → índice en bgCells
                for (y in 0 until minOf(h, bgRows)) for (x in 0 until w) {
                    val sc = x % bgCols
                    val px = IntArray(256)
                    for (py in 0..15) for (pxx in 0..15) px[py * 16 + pxx] = bgImg.get(sc * 16 + pxx, y * 16 + py)
                    val idx = byKey.getOrPut(px.toList()) { bgCells.add(px); bgCells.size - 1 }
                    bgMap[y * w + x] = fgTileCount + idx
                }
            }
        }

        val totalTiles = fgTileCount + bgCells.size
        val rows = (totalTiles + columns - 1) / columns
        val atlas = ArgbImage(columns * 16, rows * 16)
        fun cell(i: Int) = (i % columns) * 16 to (i / columns) * 16
        // Base (frame 0) de cada bloque.
        orderedBlocks.forEachIndexed { i, block ->
            val (cx, cy) = cell(i)
            drawSmwBlock(rom, defs, block, vramFrames[0], cgram, atlas, cx, cy)
        }
        // Fotogramas extra de los bloques animados + metadatos de animación.
        val animations = ArrayList<com.rolebuilder.core.model.TileAnimation>()
        var nextIdx = orderedBlocks.size
        for (bi in animBlockIdx) {
            val block = orderedBlocks[bi]
            val frameIdx = ArrayList<Int>(SMW_TILEANIM_FRAMES).apply { add(bi) } // frame 0 = tesela base
            for (f in 1 until SMW_TILEANIM_FRAMES) {
                val idx = nextIdx++
                val (cx, cy) = cell(idx)
                drawSmwBlock(rom, defs, block, vramFrames[f], cgram, atlas, cx, cy)
                frameIdx.add(idx)
            }
            animations.add(com.rolebuilder.core.model.TileAnimation(bi, frameIdx, SMW_TILEANIM_PERIOD))
        }
        // Teselas de fondo (Layer 2), apendadas tras primer plano + fotogramas de animación.
        // Su solidez/acción quedan en el valor por defecto (NONE/pasable): el fondo es
        // decorado, no colisiona ni interactúa; la colisión la pone el primer plano.
        for ((i, px) in bgCells.withIndex()) {
            val (cx, cy) = cell(fgTileCount + i)
            for (py in 0..15) for (pxx in 0..15) atlas.set(cx + pxx, cy + py, px[py * 16 + pxx])
        }
        // Colisión REAL de la ROM por tesela: clasifica cada bloque Map16 con la misma
        // rutina que el juego ([SmwBlockCollision]), así el mapa importado se juega con
        // bordes de un sentido, cuestas y pinchos fieles —no todo sólido—. El atlas puede
        // tener teselas de relleno al final (aire) que quedan como NONE/pasable. Los
        // fotogramas extra heredan la solidez de su bloque base.
        val solidity = IntArray(columns * rows) { SmwSolidity.NONE.ordinal }
        val passable = BooleanArray(columns * rows) { true }
        fun setCollision(idx: Int, s: SmwSolidity) {
            if (idx < solidity.size) { solidity[idx] = s.ordinal; passable[idx] = s == SmwSolidity.NONE }
        }
        orderedBlocks.forEachIndexed { i, block -> setCollision(i, SmwBlockCollision.classify(block)) }
        for ((ai, bi) in animBlockIdx.withIndex()) {
            val s = SmwBlockCollision.classify(orderedBlocks[bi])
            for (f in 0 until SMW_TILEANIM_FRAMES - 1) setCollision(orderedBlocks.size + ai * (SMW_TILEANIM_FRAMES - 1) + f, s)
        }

        // Acción interactiva por tesela ([SmwBlockBehavior]): las monedas se recogen. Los
        // fotogramas extra de un bloque animado heredan la acción de su bloque base (una
        // moneda anima, y todos sus fotogramas siguen siendo "moneda").
        val blockActions = IntArray(columns * rows) { SmwBlockAction.NONE.ordinal }
        fun setAction(idx: Int, block: Int) {
            if (idx < blockActions.size) blockActions[idx] = SmwBlockBehavior.classify(block).ordinal
        }
        orderedBlocks.forEachIndexed { i, block -> setAction(i, block) }
        for ((ai, bi) in animBlockIdx.withIndex()) {
            val block = orderedBlocks[bi]
            for (f in 0 until SMW_TILEANIM_FRAMES - 1) setAction(orderedBlocks.size + ai * (SMW_TILEANIM_FRAMES - 1) + f, block)
        }

        // Enemigos/entidades del nivel: la 3ª capa de datos. Se recortan al mapa visible.
        val enemies = SmwSprites.parse(rom, delta, level)?.sprites
            ?.filter { it.xTile in 0 until w && it.yTile in 0 until h }
            ?.map { Triple(it.id, it.xTile, it.yTile) }
            .orEmpty()

        return SmwLevelMap(
            atlas, columns, rows, passable.toList(), w, h, tiles.toList(), solidity.toList(),
            enemies, animations, blockActions.toList(),
            bgTiles = if (bgCells.isNotEmpty()) bgMap.toList() else emptyList(),
        )
    }

    /** Convierte los niveles escaparate en mapas (nivel#, nombre, mapa) para la app. */
    fun extractSmwLevelMaps(rom: ByteArray, header: SnesHeader): List<Triple<Int, String, SmwLevelMap>> =
        SMW_SCENE_LEVELS.toList().mapNotNull { lv ->
            val m = extractSmwLevelAsMap(rom, header, lv) ?: return@mapNotNull null
            Triple<Int, String, SmwLevelMap>(lv, "Nivel ${lv.toString(16).uppercase()}", m)
        }

    /** Ficha ligera de un nivel importable, SIN construir su mapa (eso es caro). */
    class SmwLevelListing(
        val level: Int,
        val name: String,
        /** Nº de pantallas de ancho (de la cabecera). */
        val screens: Int,
        /** % de objetos del nivel con rutina portada (100 = reconstrucción fiel). */
        val coveragePct: Int,
        /** Nombre corto del tipo de tileset (pradera, castillo, cueva…). */
        val tilesetName: String,
    )

    /** Nombres cortos por tileset FG (índice 0..F), según la tabla de despacho del juego. */
    private val SMW_TILESET_NAMES = arrayOf(
        "pradera", "castillo", "cuerda", "cueva", "casa fantasma", "casa fantasma",
        "cuerda", "pradera", "cuerda", "cueva", "cueva", "cueva", "pradera",
        "casa fantasma", "cueva", "?",
    )

    /**
     * TODOS los niveles del JUEGO importables: recorre los huecos de nivel que el
     * mundo del juego referencia (0x001..0x024 y 0x101..0x13B — los ~96 niveles de
     * SMW) y devuelve la ficha de los que el parser reconstruye con fidelidad
     * (cobertura 100% y contenido real). Los sub-niveles a los que llevan sus
     * tuberías/puertas entran solos al importar (bundle); no se listan sueltos.
     * Es barato (solo parsea; no monta atlas): apto para la UI.
     */
    fun listImportableSmwLevels(rom: ByteArray, header: SnesHeader): List<SmwLevelListing> {
        val delta = smwHeaderDelta(header)
        val out = ArrayList<SmwLevelListing>()
        val slots = (0x001..0x024) + (0x101..0x13B)
        for (lv in slots) {
            val tm = SmwLayer1.parse(rom, delta, lv) ?: continue
            if (tm.totalObjects == 0) continue
            if (tm.unknownObjects * 10 > tm.totalObjects) continue // mismo gate que el mapa
            val pct = 100 * (tm.totalObjects - tm.unknownObjects) / tm.totalObjects
            val ts = SMW_TILESET_NAMES.getOrElse(tm.tileset) { "?" }
            // Nombre REAL del nivel decodificado de la ROM (rutina UpdateLevelName del
            // overworld); si es un sublevel sin nombre propio, cae al id hexadecimal.
            val label = SmwLevelNames.nameOf(rom, delta, lv) ?: "Nivel ${lv.toString(16).uppercase()}"
            out.add(
                SmwLevelListing(
                    level = lv,
                    name = "$label · $ts" +
                        if (pct < 100) " · $pct%" else "",
                    screens = tm.screens,
                    coveragePct = pct,
                    tilesetName = ts,
                )
            )
        }
        return out
    }

    /** Nombre REAL del nivel (leveldata) decodificado de la ROM, o null si es sublevel. */
    fun smwLevelName(rom: ByteArray, header: SnesHeader, level: Int): String? =
        SmwLevelNames.nameOf(rom, smwHeaderDelta(header), level)

    /**
     * Bloques Map16 FG (0..0x1FF) que el nivel 0x106 usa DE VERDAD, como máscara de
     * bits little-endian por byte (bit t%8 del byte t/8). Derivada del informe "Map16
     * Tile Usage" de Lunar Magic 3.63 sobre la ROM US vanilla: 75 bloques (suelo,
     * taludes, tuberías, arbustos…). Con ella el render Map16 entrega SOLO los
     * ladrillos reales del nivel —cuyo VRAM/CGRAM ya montamos— en vez de pintar
     * también los ~cientos de bloques sin usar como placeholders magenta, que era lo
     * que mantenía este render fuera de la galería.
     */
    private val SMW_MAP16_USED_106: ByteArray = (
        "0000000020e82db763fe9fe4db031802e00000000000000000000000000000" +
            "005f0000445000fc016019000000080000000000000000000000000000140000" +
            "00"
        ).chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun map16Used106(block: Int): Boolean =
        block < 0x200 && (SMW_MAP16_USED_106[block / 8].toInt() shr (block % 8)) and 1 == 1

    /**
     * Renderiza el TILESET Map16 de SMW: monta cada bloque 16×16 del juego (suelo,
     * tuberías, bloques…) desde la VRAM del nivel (sus 4 slots de GFX) y lo colorea
     * con la CGRAM real por su CCC. Es la hoja de tiles LIMPIA y usable para pintar
     * mapas — los ladrillos de verdad de SMW, no un atlas de VRAM en crudo. Se
     * limita a los bloques que el nivel usa de verdad ([SMW_MAP16_USED_106]).
     */
    internal fun extractSmwMap16Tileset(rom: ByteArray, header: SnesHeader): SnesAutoExtractor.Finding? {
        val delta = smwHeaderDelta(header)
        val (bLo, bHi, bBank) = findSmwGfxTable(rom) ?: return null
        // VRAM de un nivel de pradera (0x106): FG1=GFX14, FG2=GFX17, BG1/FG3 del setting.
        val l1 = SMW_LAYER1_PTR_PC + delta + 3 * 0x106
        val lpc = lorom(byte(rom, l1), byte(rom, l1 + 1), byte(rom, l1 + 2))
        val fgbgSetting = if (lpc in 0 until rom.size) byte(rom, lpc + 4) and 0x0F else 0
        val se = SMW_FGBG_GFX_TABLE_PC + delta + 4 * fgbgSetting
        val slotFiles = intArrayOf(SMW_FG1_GFX, SMW_FG2_GFX, byte(rom, se + 2), byte(rom, se + 3))
        // Ensambla 512 teselas de VRAM (4 slots × 128) decodificadas a índices.
        val vram = arrayOfNulls<IntArray>(512)
        for (s in 0..3) {
            val file = slotFiles[s]; if (file == SMW_SLOT_EMPTY) continue
            val pc = lorom(byte(rom, bLo + file), byte(rom, bHi + file), byte(rom, bBank + file))
            if (pc < 0x40000 || pc >= rom.size) continue
            val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
            val fmt = smwFormat(data)
            val avail = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
            for (t in 0 until minOf(avail, 128)) {
                vram[s * 128 + t] = SnesDecoder.decodeTile(data, t * fmt.bytesPerTile, fmt, t).pixelIndices
            }
        }
        if (vram.all { it == null }) return null
        // CGRAM REAL del nivel, ensamblada como el juego (no horneada): color correcto.
        val cgram = assembleSmwCgram(rom, delta, 0x106)
        // Renderiza SOLO los bloques Map16 que el nivel usa (8 bytes = 4 teselas +
        // su CCC/flip cada uno), compactados en una rejilla: la hoja de "ladrillos
        // reales" lista para pintar, sin placeholders de bloques sin usar.
        val map16 = SMW_MAP16_FG_PC + delta
        val cols = 16
        val usedBlocks = (0 until 512).filter { map16Used106(it) && map16 + 8 * it + 8 <= rom.size }
        if (usedBlocks.isEmpty()) return null
        val rows = (usedBlocks.size + cols - 1) / cols
        val img = ArgbImage(cols * 16, rows * 16)
        val subPos = arrayOf(intArrayOf(0, 0), intArrayOf(0, 8), intArrayOf(8, 0), intArrayOf(8, 8))
        for ((pos, blk) in usedBlocks.withIndex()) {
            val o = map16 + 8 * blk
            val bx = (pos % cols) * 16; val by = (pos / cols) * 16
            for (k in 0..3) {
                val word = byte(rom, o + 2 * k) or (byte(rom, o + 2 * k + 1) shl 8)
                val e = SnesTilemap.decodeEntry(word)
                val px = vram.getOrNull(e.tileIndex) ?: continue
                val row = (e.palette and 7) * 16
                val ox = bx + subPos[k][0]; val oy = by + subPos[k][1]
                for (yy in 0..7) for (xx in 0..7) {
                    val sx = if (e.hFlip) 7 - xx else xx
                    val sy = if (e.vFlip) 7 - yy else yy
                    val ci = px[sy * 8 + sx]
                    val argb = if (ci == 0) 0x00000000 else cgram[row + ci]
                    img.set(ox + xx, oy + yy, argb)
                }
            }
        }
        return SnesAutoExtractor.Finding(
            image = img, label = "Tileset SMW", offset = map16, compressed = false,
            format = SnesGraphicFormat.SNES_4BPP, palette = IntArray(0),
            tileCount = (img.width / 8) * (img.height / 8), columns = img.width / 8, score = 1.0,
        )
    }

    // ------------------------------------------------------------ Super Mario World

    // -------- Paletas REALES de SMW (tablas fijas, sin comprimir, banco $00) --------
    //
    // SMW ensambla la CGRAM de cada nivel desde tablas fijas en el banco $00, en
    // BGR15 little-endian (lo mismo que lee SnesDecoder.parsePalette). Mapeo LoROM
    // banco $00: PC = SNES - 0x8000. Si la ROM trae cabecera de copiador SMC de 512
    // bytes, se suma un delta de 0x200 a cada offset PC (ver smwHeaderDelta).
    //
    // Todas las direcciones son offsets PC "unheadered". Las tablas teselan sin
    // huecos: B0A0+0x10=B0B0, B0B0+0xC0=B170, B190+0xC0=B250, B250+0x78=B2C8,
    // B2C8+0x50=B318, B318+0xC0=B3D8.

    /**
     * Índice de sub-paleta FG por defecto. Verificado contra la ROM real US 1.0:
     * el índice 0 es la paleta de terreno clásica (marrones, verdes y grises de
     * suelo/hierba), mientras que otros índices tiran a azules/rojos de fortaleza.
     * Es un parámetro estético; el 0 da el aspecto de pradera reconocible.
     */
    const val SMW_DEFAULT_FG_INDEX = 0
    /** Índice de color de "back area" por defecto (parámetro estético). */
    const val SMW_DEFAULT_BACK_INDEX = 0
    /** Índice de sub-paleta de sprites por defecto (parámetro estético). */
    const val SMW_DEFAULT_SPRITE_INDEX = 0

    /** Back area color: $00B0A0 + 2*x → PC 0x30A0 + 2*x (x=0..7, 1 color c/u). */
    internal const val SMW_BACK_AREA_PC = 0x30A0
    /** BG palette: $00B0B0 + 0x18*x → PC 0x30B0 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_BG_PC = 0x30B0
    /** Barra de estado (Layer 3): $00B170 → PC 0x3170 (colores 8-F de paletas 0 y 1). */
    internal const val SMW_STATUSBAR_PC = 0x3170
    /** FG palette: $00B190 + 0x18*x → PC 0x3190 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_FG_PC = 0x3190
    /** Fijas filas 4-D: $00B250 + 0x0C*(row-4) → PC 0x3250 + 0x0C*(row-4) (6 colores). */
    internal const val SMW_FIXED_PC = 0x3250
    /** Player palette: $00B2C8 + 0x14*p → PC 0x32C8 + 0x14*p (10 colores). */
    internal const val SMW_PLAYER_PC = 0x32C8
    /** Sprite palette: $00B318 + 0x18*x → PC 0x3318 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_SPRITE_PC = 0x3318
    /** Colores de bayas (berry): $00B674 → PC 0x3674 (colores 9-F de varias paletas). */
    internal const val SMW_BERRY_PC = 0x3674

    // -------- Tablas de SLOT: qué fichero GFX se carga en cada ranura de VRAM -------
    //
    // La paleta de un GFX no es fija: depende de la ranura donde el nivel lo carga.
    // Dos tablas en banco $00, 16 entradas de 4 bytes (un nº de fichero GFX por byte):
    //   - Sprite ($00A8C3 → PC 0x28C3): SP1, SP2, SP3, SP4  → paleta de sprite.
    //   - FG/BG  ($00A92B → PC 0x292B): FG1, FG2, BG1, FG3   → paleta FG (o BG en BG1).
    // Un GFX que solo aparece en ranuras FG/BG va con la fila FG/BG; el que solo
    // aparece en SP va con la de sprite. Así clasificamos por la VERDAD del juego en
    // vez de adivinar la paleta mirando el dibujo.
    internal const val SMW_SPRITE_GFX_TABLE_PC = 0x28C3
    internal const val SMW_FGBG_GFX_TABLE_PC = 0x292B
    /**
     * Tabla de punteros de datos de Layer 1 por nivel: SNES $05E000 → LoROM PC
     * 0x2E000. 512 niveles (0x000..0x1FF) × 3 bytes (low/high/bank). El puntero
     * lleva al inicio de los datos del nivel, cuyos 5 primeros bytes son la CABECERA
     * (de ahí salen los índices REALES de paleta FG/BG/sprite/back y los GFX settings).
     */
    internal const val SMW_LAYER1_PTR_PC = 0x2E000
    /** Nº de niveles direccionables (0x000..0x1FF). */
    internal const val SMW_LEVEL_COUNT = 0x200

    // -------------------- Fondos de Layer 2 (color REAL por tesela) --------------------
    //
    // Los fondos de Layer 2 sí llevan un tilemap ESTÁTICO: por eso son la vía asequible
    // al color por tesela. Cadena: puntero $05E600 → (si banco 0xFF) banco $0C → RLE1 →
    // índices de bloque Map16 → tabla Map16 de Layer 2 → 4 palabras [tile#][YXPCCCTT]
    // que SnesTilemap ya sabe leer. OFFSETS [PROBABLE] pendientes de validar en ROM real
    // (por eso el render va con gate de cordura y es aditivo).

    /** Tabla de punteros de Layer 2: SNES $05E600 → PC 0x2E600 (0x200 × 3 bytes). */
    internal const val SMW_LAYER2_PTR_PC = 0x2E600
    /** Base PC del banco $0C, donde viven los datos de fondo (SNES $0C8000 → 0x60000). */
    internal const val SMW_BG_BANK_PC = 0x60000
    /** Byte de banco que marca "este Layer 2 es un FONDO (tilemap), no objetos". */
    internal const val SMW_BG_IS_BACKGROUND = 0xFF
    /** Tabla de definiciones Map16 de Layer 2: SNES $0D9100 → PC 0x69100 [PROBABLE]. */
    internal const val SMW_MAP16_L2_PC = 0x69100
    /** Tabla de definiciones Map16 de FG (Layer 1): SNES $0D8000 → PC 0x68000 [PROBABLE]. */
    internal const val SMW_MAP16_FG_PC = 0x68000

    // -------------------- Mario (GFX32): puntero especial + paleta REAL --------------------
    //
    // GFX32 (los gráficos de Mario) NO está en la tabla de punteros estándar: usa un
    // puntero propio ($00B8D8 low/high + banco en $00B890). Su color tampoco se lee
    // "en frío": el juego lo ENSAMBLA en la fila 8 de la CGRAM (sprite 0), así que su
    // paleta sale de assembleSmwCgram(fila 8) — rojo, no cian, y sin verde compartido.
    /** Puntero de GFX32 (Mario): byte bajo/alto en 0x38D8/0x38D9, banco en 0x3890. */
    internal const val SMW_GFX32_LO_PC = 0x38D8
    internal const val SMW_GFX32_HI_PC = 0x38D9
    internal const val SMW_GFX32_BANK_PC = 0x3890

    /**
     * Tablas del ensamblador de OAM del jugador (rutina PlayerGFXRt, banco $00). Para
     * CADA pose el juego dibuja a Mario como DOS teselas 16×16 apiladas: la CABEZA
     * (arriba) y el CUERPO (abajo), cuyos gráficos NO están juntos en GFX32 sino en
     * celdas separadas que la ROM combina por DMA. Estas dos tablas dan, por pose, el
     * "puntero de tesela" de la cabeza y del cuerpo (índice v13 = pose para Mario
     * pequeño). Sin componerlas Mario sale "sin cabeza" (el hueco de la cara queda en
     * blanco). Direcciones: $00:E00C y $00:E0CC (192 bytes cada una).
     */
    internal const val SMW_PLAYER_HEAD_TILE_PC = 0x600C
    internal const val SMW_PLAYER_BODY_TILE_PC = 0x60CC

    /** Umbral PC: los datos de fondo por debajo son página 0; por encima, página 1 [PROBABLE]. */
    internal const val SMW_BG_PAGE_THRESHOLD_PC = 0x668FE
    /** Nº de entradas (settings 0..F) de cada tabla de slots. */
    internal const val SMW_SLOT_ENTRIES = 16
    /** Valor "ranura vacía" en las tablas de slots (no carga ningún fichero). */
    internal const val SMW_SLOT_EMPTY = 0x7F
    /** Invariante vanilla verificable: FG1 siempre es GFX 0x14 y FG2 siempre GFX 0x17. */
    internal const val SMW_FG1_GFX = 0x14
    internal const val SMW_FG2_GFX = 0x17
    /** Nº de fichero GFX de Mario/player (GFX32). Siempre va con la player palette. */
    internal const val SMW_MARIO_GFX = 0x32

    /** Negro opaco (índice de contorno/sombra en las filas 3bpp/4bpp). */
    private val SMW_BLACK = 0xFF000000.toInt()
    /** Transparente: la hoja de tiles ya trata el índice 0 como transparente. */
    private const val SMW_TRANSPARENT = 0

    /**
     * Delta de offset por cabecera de copiador SMC. Se deriva de [SnesHeader.headerOffset]:
     * 0x7FC0 (sin cabecera) → 0; 0x7FC0+512 = 0x81C0 (con cabecera) → 0x200.
     */
    internal fun smwHeaderDelta(header: SnesHeader): Int = header.headerOffset - 0x7FC0

    /**
     * Mapa de COLISIÓN de un nivel de SMW: la solidez de cada celda 16×16, lista para
     * que un motor de plataformas la use directamente. Es la pieza que faltaba para
     * jugar un nivel extraído (los gráficos y la geometría ya se sacaban; esto dice
     * dónde te paras, chocas, resbalas o mueres).
     *
     * Rejilla fila a fila (row-major) de [cols]×[rows] celdas. [block] guarda el
     * número de bloque Map16 crudo de cada celda por si se quiere reclasificar o
     * dibujar; [solidity] su clase de colisión ya resuelta ([SmwBlockCollision]).
     */
    class SmwCollisionMap(
        val level: Int,
        val cols: Int,
        val rows: Int,
        val blocks: IntArray,
        val solidity: List<SmwSolidity>,
    ) {
        fun blockAt(col: Int, row: Int): Int = blocks[row * cols + col]

        fun solidityAt(col: Int, row: Int): SmwSolidity = solidity[row * cols + col]
    }

    /**
     * Extrae el mapa de colisión del nivel [level] de una ROM de SMW: parsea la
     * geometría de Layer 1 ([SmwLayer1]) y clasifica cada bloque con la solidez real
     * del juego ([SmwBlockCollision]). Recorta a las columnas con contenido (los
     * niveles no llenan las 32 pantallas). Devuelve null si el nivel no tiene datos
     * de Layer 1 (vertical, sala de jefe) o queda vacío.
     */
    fun smwLevelCollision(rom: ByteArray, header: SnesHeader, level: Int): SmwCollisionMap? {
        val delta = smwHeaderDelta(header)
        val tm = SmwLayer1.parse(rom, delta, level) ?: return null

        // Última columna con algún bloque distinto de aire (0x25), como en las escenas.
        val totalCols = tm.screens * 16
        var lastCol = -1
        for (c in 0 until totalCols) {
            for (r in 0..26) {
                val b = tm.block(c, r)
                if (b > 0 && b != 0x25) { lastCol = c; break }
            }
        }
        if (lastCol < 3) return null

        val cols = minOf(lastCol + 2, totalCols)
        val rows = 27 // 0x1B0 / 16: alto fijo de un nivel horizontal de SMW
        val blocks = IntArray(cols * rows)
        val solidity = ArrayList<SmwSolidity>(cols * rows)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val b = tm.block(c, r)
                blocks[r * cols + c] = b
                solidity.add(SmwBlockCollision.classify(b))
            }
        }
        return SmwCollisionMap(level, cols, rows, blocks, solidity)
    }

    /**
     * ENEMIGOS del nivel [level] como (id de sprite, casilla X, casilla Y), leídos de
     * la lista de sprites real de la ROM ([SmwSprites]). Es el API público para que
     * la app siembre enemigos también al jugar un nivel DIRECTO desde la ROM (la ruta
     * de mapa importado ya los lleva dentro de [SmwLevelMap.enemies]). Vacío si el
     * nivel no tiene lista válida.
     */
    fun smwLevelEnemies(rom: ByteArray, header: SnesHeader, level: Int): List<Triple<Int, Int, Int>> =
        SmwSprites.parse(rom, smwHeaderDelta(header), level)?.sprites
            ?.map { Triple(it.id, it.xTile, it.yTile) }
            .orEmpty()

    /**
     * Índice de las poses de Mario que exporta [smwMarioSheet], en el ORDEN de los
     * fotogramas de la hoja. Se eligen las poses "de a pie" del juego real:
     * `0` parado, `4`/`6` los dos pasos del ciclo de andar, `12` la pose inclinada
     * (correr/saltar). El renderer mapea: fotograma 0 = quieto, 2/3 = andar, 4 = aire.
     */
    val SMW_MARIO_SHEET_POSES = intArrayOf(0, 1, 4, 6, 12)

    /**
     * Desplazamiento de la tesela por PODER del jugador (kPlayerGFXRt_PowerupTilesetIndex,
     * banco $00): la MISMA pose usa teselas distintas según Mario sea pequeño (0),
     * grande (1), de fuego (2) o con capa (3). El índice a las tablas de cabeza/cuerpo
     * es `PowerupTilesetIndex[poder] + pose`. Grande no es solo "más grande": son otros
     * gráficos con torso y peto completos, no el chibi de pequeño escalado.
     */
    val SMW_MARIO_POWERUP_TILESET = intArrayOf(0x00, 0x46, 0x83, 0x46)

    /**
     * Fila de paleta de jugador por PODER (de kPlayerGFXRt_PalettePointers para Mario:
     * v4 = 2·poder). Pequeño/grande/capa usan la paleta normal de Mario (player 0);
     * FUEGO usa la suya (player 2): peto y gorra BLANCOS, camisa roja. Sin esto, Mario
     * de fuego saldría con los colores normales.
     */
    val SMW_MARIO_POWERUP_PALETTE = intArrayOf(0, 0, 0, 2)

    /**
     * Hoja de sprites de MARIO ya COMPUESTA como la ensambla el juego: por cada pose
     * apila la tesela de la CABEZA sobre la del CUERPO (ver [SMW_PLAYER_HEAD_TILE_PC]/
     * [SMW_PLAYER_BODY_TILE_PC]), así la cara sale con su color de piel en vez de un
     * hueco en blanco. Coloreada con la paleta de jugador REAL de la ROM (fila 8 de la
     * CGRAM: col 1 blanco, cols 2-5 colores estándar, cols 6-F la paleta de Mario).
     *
     * [powerup] elige el juego de gráficos Y la paleta (0 pequeño, 1 grande, 2 capa,
     * 3 fuego) vía [SMW_MARIO_POWERUP_TILESET]/[SMW_MARIO_POWERUP_PALETTE]; por defecto
     * 0 (pequeño), la salida histórica.
     *
     * Salida: un fotograma de 16×32 px por pose de [SMW_MARIO_SHEET_POSES], en fila
     * (ancho = 16·nPoses, alto = 32). Devuelve null si no se pudo leer/descomprimir
     * GFX32 o no llega hasta las teselas de la cabeza.
     */
    fun smwMarioSheet(rom: ByteArray, header: SnesHeader, powerup: Int = 0): ArgbImage? {
        val delta = smwHeaderDelta(header)
        val mpc = lorom(
            byte(rom, SMW_GFX32_LO_PC + delta),
            byte(rom, SMW_GFX32_HI_PC + delta),
            byte(rom, SMW_GFX32_BANK_PC + delta),
        )
        if (mpc < 0x40000 || mpc >= rom.size) return null
        val mdata = runCatching { LcLz2.decompress(rom, mpc).data }.getOrNull() ?: return null
        val mfmt = SnesGraphicFormat.SNES_4BPP
        val avail = SnesAssetExtractor.availableTiles(mdata.size, 0, mfmt)
        // Las teselas de la cabeza (puntero 0x50) viven en la mitad BAJA de GFX32; hace
        // falta la hoja entera (≥ 256 teselas), no solo las 128 de arriba.
        if (avail < 256) return null
        val tables = SmwPaletteTables(rom, delta)
        // Blanco de SPRITE (fila 8) = $7FFF, no el $7FDD de objeto.
        val white = SnesDecoder.bgr15ToArgb(0x7FFF)
        val fix8 = tables.fixedRow(8)
        val pl = tables.player(SMW_MARIO_POWERUP_PALETTE[powerup.coerceIn(0, 3)])
        val marioPal = intArrayOf(
            0, white, fix8[0], fix8[1], fix8[2], fix8[3],
            pl[0], pl[1], pl[2], pl[3], pl[4], pl[5], pl[6], pl[7], pl[8], pl[9],
        )
        return runCatching {
            val poses = SMW_MARIO_SHEET_POSES
            val sheet = ArgbImage(16 * poses.size, 32)
            // Pinta el bloque 16×16 (4 teselas 8×8) que apunta [ptr] en (ox,oy).
            fun blitBlock(ptr: Int, ox: Int, oy: Int) {
                // Puntero de tesela → índice de tesela 8×8 en el buffer de 16 de ancho:
                // t = (ptr & 0xF7) << 6 bytes; /32 = (ptr & 0xF7) << 1; bit 3 = +512.
                val base = ((ptr and 0xF7) shl 1) + if (ptr and 0x08 != 0) 512 else 0
                // Orden 16×16: sup-izq, sup-der, inf-izq (16 después), inf-der.
                val cells = intArrayOf(base, base + 1, base + 16, base + 17)
                for (c in 0 until 4) {
                    val t = cells[c]
                    if (t < 0 || t >= avail) continue
                    val decoded = SnesDecoder.decodeTile(mdata, t * mfmt.bytesPerTile, mfmt, t)
                    val dx = ox + (c and 1) * 8
                    val dy = oy + (c ushr 1) * 8
                    for (py in 0 until 8) for (px in 0 until 8) {
                        val ci = decoded.pixelIndices[py * 8 + px]
                        if (ci == 0) continue
                        sheet.set(dx + px, dy + py, marioPal[ci])
                    }
                }
            }
            val tilesetBase = SMW_MARIO_POWERUP_TILESET[powerup.coerceIn(0, 3)]
            for (f in poses.indices) {
                // Índice a las tablas de cabeza/cuerpo = base del poder + pose.
                val v13 = tilesetBase + poses[f]
                val headPtr = byte(rom, SMW_PLAYER_HEAD_TILE_PC + delta + v13)
                val bodyPtr = byte(rom, SMW_PLAYER_BODY_TILE_PC + delta + v13)
                blitBlock(bodyPtr, f * 16, 16) // cuerpo abajo
                blitBlock(headPtr, f * 16, 0)  // cabeza arriba (sobre el cuerpo)
            }
            sheet
        }.getOrNull()
    }

    /**
     * Datos de un nivel de SMW leídos de su cabecera primaria (5 bytes en el puntero
     * de Layer 1). Complementa a colisión/físicas/inicio con el "resto de la ficha"
     * del nivel: tamaño, modo, música, paletas y tiempo.
     */
    class SmwLevelInfo(
        val level: Int,
        /** Nº de pantallas de 16 casillas (ancho del nivel: ×16 casillas, ×256 px). */
        val screens: Int,
        /** Modo de nivel (horizontal/vertical/especial). */
        val levelMode: Int,
        /** Índice de MÚSICA del nivel (0-7): elige una de las pistas de nivel del juego. */
        val musicIndex: Int,
        val bgPalette: Int,
        val backgroundColor: Int,
        val fgPalette: Int,
        val spritePalette: Int,
        val spriteGfx: Int,
        val fgTileset: Int,
        /** Tiempo inicial (×100): 0 = sin tiempo, si no 200/300/400. */
        val startTime: Int,
    ) {
        /** Ancho del nivel en casillas de 16 px. */
        val widthTiles: Int get() = screens * 16
    }

    private val SMW_TIMER_TABLE = intArrayOf(0, 2, 3, 4)

    /**
     * Lee la ficha de un nivel de SMW (cabecera primaria de 5 bytes) EXACTAMENTE como
     * `LoadLevelHeader` ($05:84E3, vía snesrev/smw). Devuelve null si el puntero de
     * Layer 1 no es válido.
     */
    fun smwLevelInfo(rom: ByteArray, header: SnesHeader, level: Int): SmwLevelInfo? {
        val delta = smwHeaderDelta(header)
        val lpc = smwLayer1DataPc(rom, delta, level) ?: return null
        val h0 = byte(rom, lpc); val h1 = byte(rom, lpc + 1); val h2 = byte(rom, lpc + 2)
        val h3 = byte(rom, lpc + 3); val h4 = byte(rom, lpc + 4)
        return SmwLevelInfo(
            level = level,
            screens = (h0 and 0x1F) + 1,
            levelMode = h1 and 0x1F,
            musicIndex = (h2 shr 4) and 0x07,
            bgPalette = h0 shr 5,
            backgroundColor = h1 shr 5,
            fgPalette = h3 and 0x07,
            spritePalette = (h3 shr 3) and 0x07,
            spriteGfx = h2 and 0x0F,
            fgTileset = h4 and 0x0F,
            startTime = SMW_TIMER_TABLE[(h3 shr 6) and 0x03] * 100,
        )
    }

    /**
     * Selectores de paleta REALES de la cabecera primaria (5 bytes) de un nivel.
     * Decodificados EXACTAMENTE como la rutina de carga del juego (bank $00
     * CODE_0584E3 del disassembly SMWDisX):
     *   byte0 bits 7-5 = BG palette; byte1 bits 7-5 = back area color;
     *   byte3 bits 2-0 = FG palette; byte3 bits 5-3 = sprite palette;
     *   byte4 bits 3-0 = tileset FG (ObjectTileset).
     */
    internal data class SmwLevelHeader(
        val bgPal: Int, val backArea: Int, val fgPal: Int, val sprPal: Int, val objectTileset: Int,
    )

    /** Lee y decodifica la cabecera primaria del nivel [level] (0..0x1FF), o null. */
    internal fun readSmwLevelHeader(rom: ByteArray, delta: Int, level: Int): SmwLevelHeader? {
        val l1 = SMW_LAYER1_PTR_PC + delta + 3 * level
        if (l1 + 2 >= rom.size) return null
        val lpc = lorom(byte(rom, l1), byte(rom, l1 + 1), byte(rom, l1 + 2))
        if (lpc < 0 || lpc + 4 >= rom.size) return null
        val b0 = byte(rom, lpc); val b1 = byte(rom, lpc + 1)
        val b3 = byte(rom, lpc + 3); val b4 = byte(rom, lpc + 4)
        return SmwLevelHeader(
            bgPal = (b0 shr 5) and 0x07, backArea = (b1 shr 5) and 0x07,
            fgPal = b3 and 0x07, sprPal = (b3 shr 3) and 0x07, objectTileset = b4 and 0x0F,
        )
    }

    /**
     * Ensambla la CGRAM COMPLETA (256 colores ARGB) de un nivel EXACTAMENTE como el
     * juego (rutina LoadPalette, bank $00 del disassembly SMWDisX). Esto sustituye a
     * cualquier paleta "horneada": lee las tablas reales de la ROM y las coloca en la
     * misma disposición de 16 filas × 16 colores que construye SMW en tiempo real, así
     * que el color sale CORRECTO para CUALQUIER nivel, no solo pradera.
     *
     * Disposición (filas 0-7 = capas de fondo/objeto; 8-15 = sprites):
     *  - col 1 de filas 0-7 = blanco $7FDD; col 1 de filas 8-15 = blanco $7FFF.
     *  - StatusBar → filas 0-1 cols 8-F.        - StandardColors → filas 4-D cols 2-7.
     *  - BackArea → color 0 (fondo).            - FG palette → filas 2-3 cols 2-7.
     *  - Sprite palette → filas E-F cols 2-7.   - BG palette → filas 0-1 cols 2-7.
     *  - Berry → filas 2-4 y 9-B cols 9-F.      - Player (Mario) → fila 8 cols 6-F.
     */
    internal fun assembleSmwCgram(rom: ByteArray, delta: Int, level: Int): IntArray {
        val hdr = readSmwLevelHeader(rom, delta, level)
            ?: SmwLevelHeader(0, 0, SMW_DEFAULT_FG_INDEX, SMW_DEFAULT_SPRITE_INDEX, 0)
        val pal = IntArray(256)
        fun color(pc: Int): Int = SnesDecoder.bgr15ToArgb(byte(rom, pc + delta) or (byte(rom, pc + delta + 1) shl 8))
        // idx(fila,col) → índice de color absoluto 0..255.
        fun idx(row: Int, col: Int) = row * 16 + col
        // Copia n colores consecutivos de ROM[srcPc..] a (row,col0), (row,col0+1)...
        fun run(srcPc: Int, row: Int, col0: Int, n: Int) {
            for (i in 0 until n) pal[idx(row, col0 + i)] = color(srcPc + 2 * i)
        }
        // A/B) col 1 = blanco en las 16 filas (objeto $7FDD, sprite $7FFF): estético.
        for (r in 0..7) pal[idx(r, 1)] = SnesDecoder.bgr15ToArgb(0x7FDD)
        for (r in 8..15) pal[idx(r, 1)] = SnesDecoder.bgr15ToArgb(0x7FFF)
        // C) StatusBar: filas 0-1, cols 8-F (8 colores/fila, secuencial).
        for (r in 0..1) run(SMW_STATUSBAR_PC + r * 16, r, 8, 8)
        // D) StandardColors: filas 4-D, cols 2-7 (6 colores/fila, secuencial).
        for (r in 4..13) run(SMW_FIXED_PC + (r - 4) * 12, r, 2, 6)
        // E) Back area color → color de fondo (índice 0).
        pal[0] = color(SMW_BACK_AREA_PC + 2 * (hdr.backArea and 0x0F))
        // F) FG palette (según cabecera): filas 2-3, cols 2-7.
        val fgBase = SMW_FG_PC + hdr.fgPal * 0x18
        run(fgBase, 2, 2, 6); run(fgBase + 12, 3, 2, 6)
        // G) Sprite palette: filas E-F, cols 2-7.
        val sprBase = SMW_SPRITE_PC + hdr.sprPal * 0x18
        run(sprBase, 14, 2, 6); run(sprBase + 12, 15, 2, 6)
        // H) BG palette: filas 0-1, cols 2-7.
        val bgBase = SMW_BG_PC + hdr.bgPal * 0x18
        run(bgBase, 0, 2, 6); run(bgBase + 12, 1, 2, 6)
        // I/J) Berry: filas 2-4 y 9-B, cols 9-F (7 colores/fila, secuencial).
        for (r in 0..2) run(SMW_BERRY_PC + r * 14, 2 + r, 9, 7)
        for (r in 0..2) run(SMW_BERRY_PC + r * 14, 9 + r, 9, 7)
        // Player (Mario normal): fila 8 (sprite 0), cols 6-F (10 colores).
        run(SMW_PLAYER_PC, 8, 6, 10)
        // K) Colores ANIMADOS (UploadLevelAnimations, $00:A414): el juego cicla cada
        // pocos frames los índices 0x64/0x6D (DORADO del brillo de monedas/bloques ?)
        // y 0x7D (rojo, bloques ON/OFF) desde kGlobalPalettes_Flashing ($00:B60C).
        // En las tablas fijas esos huecos guardan un MAGENTA placeholder (#f808f8):
        // sin esta pasada, las monedas de dragón y los '?' salen rosas. Frame 0.
        pal[0x64] = color(SMW_FLASHING_PC)
        pal[0x6D] = color(SMW_FLASHING_PC)
        pal[0x7D] = color(SMW_FLASHING_PC + 16)
        return pal
    }

    /** Tabla de colores del DESTELLO (kGlobalPalettes_Flashing): $00B60C → PC 0x360C.
     *  8 words de dorado (frames del brillo) + 8 words de rojo (ON/OFF). */
    internal const val SMW_FLASHING_PC = 0x360C

    /**
     * Lecturas de las tablas de paleta reales de SMW sobre [rom], ya aplicado el
     * [delta] de cabecera. Todas devuelven ARGB (vía [SnesDecoder.parsePalette]).
     *
     * Las entradas de BG/FG/Sprite ocupan 12 colores (cols 2-7 de DOS filas). Aquí
     * leemos solo los 6 primeros (bytes 0-11), que son la fila "de arriba"; la fila
     * hermana serían los 6 siguientes (bytes 12-23) de la misma entrada.
     */
    internal class SmwPaletteTables(private val rom: ByteArray, private val delta: Int) {
        /** Back area: 1 color en x=0..7. */
        fun backColor(x: Int): Int =
            SnesDecoder.parsePalette(rom, SMW_BACK_AREA_PC + delta + 2 * x, 1)[0]

        /** BG: 6 colores (fila de arriba de la entrada de 12) en x=0..7. */
        fun bgEntry(x: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_BG_PC + delta + 0x18 * x, 6)

        /** FG: 6 colores (fila de arriba de la entrada de 12) en x=0..7. */
        fun fgEntry(x: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_FG_PC + delta + 0x18 * x, 6)

        /** Sprite: 6 colores (fila de arriba de la entrada de 12) en x=0..7. */
        fun sprEntry(x: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_SPRITE_PC + delta + 0x18 * x, 6)

        /** Fijas: 6 colores de la fila [row] (4..D). */
        fun fixedRow(row: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_FIXED_PC + delta + 0x0C * (row - 4), 6)

        /** Player: 10 colores (cols 6-F) para p = 0..3 (Mario/Luigi/FireMario/FireLuigi). */
        fun player(p: Int): IntArray =
            SnesDecoder.parsePalette(rom, SMW_PLAYER_PC + delta + 0x14 * p, 10)
    }

    /**
     * Fila canónica FG (3bpp, 8 colores):
     * `[backColor(backIndex), NEGRO, fg0..fg5]`.
     */
    internal fun row3bppFG(t: SmwPaletteTables, fgIndex: Int, backIndex: Int): IntArray {
        val fg = t.fgEntry(fgIndex)
        return intArrayOf(t.backColor(backIndex), SMW_BLACK, fg[0], fg[1], fg[2], fg[3], fg[4], fg[5])
    }

    /**
     * Fila canónica BG (3bpp, 8 colores):
     * `[backColor(backIndex), NEGRO, bg0..bg5]`. Igual estructura que la FG pero con
     * la entrada de BG; es la que corresponde a los ficheros que se cargan en la
     * ranura BG1.
     */
    internal fun row3bppBG(t: SmwPaletteTables, bgIndex: Int, backIndex: Int): IntArray {
        val bg = t.bgEntry(bgIndex)
        return intArrayOf(t.backColor(backIndex), SMW_BLACK, bg[0], bg[1], bg[2], bg[3], bg[4], bg[5])
    }

    /**
     * Fila canónica de sprite (3bpp, 8 colores):
     * `[TRANSPARENTE, NEGRO, spr0..spr5]`.
     */
    internal fun row3bppSprite(t: SmwPaletteTables, sprIndex: Int): IntArray {
        val s = t.sprEntry(sprIndex)
        return intArrayOf(SMW_TRANSPARENT, SMW_BLACK, s[0], s[1], s[2], s[3], s[4], s[5])
    }

    /** Rol de paleta de un fichero GFX según la ranura de VRAM donde se carga. */
    internal enum class SmwGfxRole { FG, BG, SPRITE, PLAYER }

    /**
     * Tablas de slots LEÍDAS y VALIDADAS de la ROM: qué números de fichero GFX se
     * cargan en ranuras FG/BG y en ranuras de sprite. Con esto clasificamos cada
     * fichero por la verdad del juego en vez de puntuar su dibujo.
     *
     * [readIfValid] solo devuelve tablas si se cumple el invariante vanilla (FG1 =
     * GFX 0x14 y FG2 = GFX 0x17 en la mayoría de entradas). Ese chequeo confirma de
     * paso que el offset, el layout y el delta de cabecera son correctos; si no
     * cuadra (ROM modificada, offset equivocado…), devuelve null y la receta cae al
     * heurístico de siempre. Es la misma disciplina que [findSmwGfxTable]: no fiarse
     * de una dirección sin verificarla contra la propia ROM.
     */
    internal class SmwSlotTables private constructor(
        private val fgGfx: Set<Int>,
        private val bgGfx: Set<Int>,
        private val spriteGfx: Set<Int>,
    ) {
        /** Rol de la paleta para el fichero [gfx] (su número 0..51). [is4bpp] desempata. */
        fun roleOf(gfx: Int, is4bpp: Boolean): SmwGfxRole {
            if (gfx == SMW_MARIO_GFX) return SmwGfxRole.PLAYER
            val inFg = gfx in fgGfx
            val inBg = gfx in bgGfx
            val inSp = gfx in spriteGfx
            return when {
                inSp && !inFg && !inBg -> SmwGfxRole.SPRITE
                (inFg || inBg) && !inSp -> if (inFg) SmwGfxRole.FG else SmwGfxRole.BG
                // Aparece en ambos tipos de ranura, o en ninguno (fuente/HUD por
                // rutinas fijas): desempata el formato. 4bpp → sprite; 3bpp → FG.
                else -> if (is4bpp) SmwGfxRole.SPRITE else SmwGfxRole.FG
            }
        }

        companion object {
            fun readIfValid(rom: ByteArray, delta: Int): SmwSlotTables? {
                val fgBase = SMW_FGBG_GFX_TABLE_PC + delta
                val spBase = SMW_SPRITE_GFX_TABLE_PC + delta
                if (fgBase < 0 || spBase < 0 ||
                    fgBase + 4 * SMW_SLOT_ENTRIES > rom.size ||
                    spBase + 4 * SMW_SLOT_ENTRIES > rom.size
                ) return null

                // Gate: FG1==0x14 y FG2==0x17 en la gran mayoría de las 16 entradas.
                var invariant = 0
                for (e in 0 until SMW_SLOT_ENTRIES) {
                    if (byte(rom, fgBase + 4 * e) == SMW_FG1_GFX &&
                        byte(rom, fgBase + 4 * e + 1) == SMW_FG2_GFX
                    ) invariant++
                }
                if (invariant < SMW_SLOT_ENTRIES - 2) return null

                val fg = HashSet<Int>()
                val bg = HashSet<Int>()
                val sp = HashSet<Int>()
                for (e in 0 until SMW_SLOT_ENTRIES) {
                    // FG/BG: [FG1, FG2, BG1, FG3]
                    val fgb = fgBase + 4 * e
                    for (v in intArrayOf(byte(rom, fgb), byte(rom, fgb + 1), byte(rom, fgb + 3)))
                        if (v != SMW_SLOT_EMPTY) fg.add(v)
                    byte(rom, fgb + 2).let { if (it != SMW_SLOT_EMPTY) bg.add(it) }
                    // Sprite: [SP1, SP2, SP3, SP4]
                    val spb = spBase + 4 * e
                    for (k in 0..3) byte(rom, spb + k).let { if (it != SMW_SLOT_EMPTY) sp.add(it) }
                }
                return SmwSlotTables(fg, bg, sp)
            }
        }
    }

    /**
     * Paletas REALES por fichero GFX, derivadas de los NIVELES que lo cargan.
     *
     * La idea que da color de verdad (no adivinado): cada nivel tiene una cabecera
     * (5 bytes al inicio de sus datos de Layer 1, tabla de punteros en $05E000) que
     * dice qué índice de sub-paleta 0..7 usa para FG, BG, sprites y back area, y qué
     * "GFX setting" carga en sus ranuras (que se expanden con las tablas de slot). Así,
     * recorriendo los 512 niveles, sabemos para CADA fichero GFX con qué índice de
     * paleta lo pinta el juego de verdad. Elegimos el índice MAYORITARIO entre los
     * niveles que lo usan: color real y variado, sin heurístico.
     *
     * [read] valida el invariante de las tablas de slot (FG1=0x14/FG2=0x17) y exige un
     * mínimo de cabeceras de nivel plausibles; si no cuadra (ROM modificada u offsets
     * dudosos) devuelve null y la receta cae al heurístico. Los mapas van indexados por
     * número de fichero GFX (0..51) → índice de sub-paleta 0..7.
     */
    internal class SmwLevelPalettes private constructor(
        val fgIndexByFile: Map<Int, Int>,
        val bgIndexByFile: Map<Int, Int>,
        val spriteIndexByFile: Map<Int, Int>,
        val levelsRead: Int,
    ) {
        companion object {
            /** Mínimo de niveles válidos para fiarnos (una ROM vanilla tiene cientos). */
            private const val MIN_VALID_LEVELS = 64

            fun read(rom: ByteArray, delta: Int): SmwLevelPalettes? {
                val fgBase = SMW_FGBG_GFX_TABLE_PC + delta
                val spBase = SMW_SPRITE_GFX_TABLE_PC + delta
                val ptrBase = SMW_LAYER1_PTR_PC + delta
                if (fgBase < 0 || spBase < 0 || ptrBase < 0 ||
                    fgBase + 4 * SMW_SLOT_ENTRIES > rom.size ||
                    spBase + 4 * SMW_SLOT_ENTRIES > rom.size ||
                    ptrBase + 3 * SMW_LEVEL_COUNT > rom.size
                ) return null

                // Mismo gate que SmwSlotTables: confirma offset/layout/delta de slots.
                var invariant = 0
                for (e in 0 until SMW_SLOT_ENTRIES) {
                    if (byte(rom, fgBase + 4 * e) == SMW_FG1_GFX &&
                        byte(rom, fgBase + 4 * e + 1) == SMW_FG2_GFX
                    ) invariant++
                }
                if (invariant < SMW_SLOT_ENTRIES - 2) return null

                // file -> conteo de veces que cada índice 0..7 se le asigna.
                val fgAcc = HashMap<Int, IntArray>()
                val bgAcc = HashMap<Int, IntArray>()
                val sprAcc = HashMap<Int, IntArray>()
                fun bump(acc: HashMap<Int, IntArray>, file: Int, idx: Int) {
                    if (file != SMW_SLOT_EMPTY) acc.getOrPut(file) { IntArray(8) }[idx and 7]++
                }

                var valid = 0
                for (level in 0 until SMW_LEVEL_COUNT) {
                    val p = ptrBase + 3 * level
                    val pc = lorom(byte(rom, p), byte(rom, p + 1), byte(rom, p + 2))
                    if (pc < 0 || pc + 5 > rom.size) continue
                    val h0 = byte(rom, pc); val h2 = byte(rom, pc + 2); val h3 = byte(rom, pc + 3)
                    val h4 = byte(rom, pc + 4)
                    val bgPal = h0 shr 5            // BBB
                    val sprPal = (h3 shr 3) and 7  // PPP
                    val fgPal = h3 and 7           // FFF
                    val sprSetting = h2 and 0x0F   // SSSS
                    val fgbgSetting = h4 and 0x0F  // ZZZZ
                    valid++
                    // FG/BG: entrada [FG1, FG2, BG1, FG3] del setting de este nivel.
                    val fe = fgBase + 4 * fgbgSetting
                    bump(fgAcc, byte(rom, fe), fgPal)
                    bump(fgAcc, byte(rom, fe + 1), fgPal)
                    bump(bgAcc, byte(rom, fe + 2), bgPal)
                    bump(fgAcc, byte(rom, fe + 3), fgPal)
                    // Sprite: entrada [SP1..SP4] del setting de sprite.
                    val se = spBase + 4 * sprSetting
                    for (k in 0..3) bump(sprAcc, byte(rom, se + k), sprPal)
                }
                if (valid < MIN_VALID_LEVELS) return null

                fun modal(acc: HashMap<Int, IntArray>): Map<Int, Int> =
                    acc.mapValues { (_, counts) ->
                        var best = 0
                        for (idx in 1 until 8) if (counts[idx] > counts[best]) best = idx
                        best
                    }
                return SmwLevelPalettes(modal(fgAcc), modal(bgAcc), modal(sprAcc), valid)
            }
        }
    }

    /**
     * Lee las entradas de tilemap (`[tile#][YXPCCCTT]`) del FONDO de Layer 2 de un
     * [level], o vacío si ese nivel no tiene fondo (Layer 2 de objetos) o los datos
     * no son válidos. Cadena: puntero $05E600 → banco $0C → RLE1 → índices de bloque
     * Map16 → 4 palabras por bloque en la tabla Map16 de Layer 2.
     *
     * Es la vía al color REAL por tesela de los fondos: cada entrada trae el `CCC`
     * (sub-paleta) de esa tesela. Función pura para poder testearla con datos
     * sintéticos. Los offsets Map16/umbral son [PROBABLE]; el consumidor aplica un
     * gate de cordura sobre la concentración de sub-paletas.
     */
    /**
     * Descompresor EXACTO del fondo de Layer 2 (port 1:1 de BufferBGTilemap, $05:8126):
     * flujo de comandos de 1 byte. `L = cmd & 0x7F` emite L+1 bytes: bit7=0 copia L+1
     * literales; bit7=1 repite el byte siguiente L+1 veces. A diferencia de un RLE
     * genérico, **0xFF es un comando válido** (repite 128); el flujo termina SOLO con
     * DOS 0xFF seguidos en posición de comando. (Mi Rle1 genérico paraba en el primero,
     * lo que truncaba fondos con rachas de 128.)
     */
    private fun decompressSmwBackground(rom: ByteArray, dataPc: Int, maxOut: Int = 0x4000): ByteArray {
        val out = ArrayList<Byte>(1024)
        var p = dataPc
        fun rd(): Int { val v = if (p in rom.indices) rom[p].toInt() and 0xFF else 0xFF; p++; return v }
        while (out.size < maxOut) {
            if (p + 1 >= rom.size) break
            if ((rom[p].toInt() and 0xFF) == 0xFF && (rom[p + 1].toInt() and 0xFF) == 0xFF) break
            val cmd = rd()
            val len = (cmd and 0x7F) + 1
            if (cmd and 0x80 == 0) repeat(len) { out.add(rd().toByte()) }
            else { val v = rd().toByte(); repeat(len) { out.add(v) } }
        }
        return ByteArray(out.size) { out[it] }
    }

    internal fun layer2BgEntries(rom: ByteArray, delta: Int, level: Int): List<SnesTilemap.TilemapEntry> {
        val p = SMW_LAYER2_PTR_PC + delta + 3 * level
        if (p < 0 || p + 2 >= rom.size) return emptyList()
        val isBg = byte(rom, p + 2)
        if (isBg and 2 == 0) return emptyList() // bit1: Layer 2 de imagen; si no, son objetos
        val addr = byte(rom, p) or (byte(rom, p + 1) shl 8)
        if (addr < 0x8000) return emptyList()
        val dataPc = SMW_BG_BANK_PC + delta + (addr - 0x8000)
        if (dataPc < 0 || dataPc >= rom.size) return emptyList()
        val blocks = decompressSmwBackground(rom, dataPc)
        if (blocks.size < 4) return emptyList()
        // block = (hi<<8)|lo. LoadSublevel: si bit2 está set (fondos vanilla, is_bg=0xFF)
        // el juego NO rellena hi → hi=0 (página 0); si no, hi = is_bg>>4.
        // Página del Map16 de fondo (hi del índice de bloque). Con is_bg vanilla el juego
        // deja el hi en el estado previo de WRAM, que resulta ser 0 o 1 según el grupo de
        // datos; el umbral de PC (reverse-engineered) lo distingue: los datos por encima
        // de él usan la página 1 del Map16 de fondo.
        val hi = if (dataPc - delta < SMW_BG_PAGE_THRESHOLD_PC) 0 else 1
        val map16Base = SMW_MAP16_L2_PC + delta
        val entries = ArrayList<SnesTilemap.TilemapEntry>(blocks.size * 4)
        for (bb in blocks) {
            val block = (hi shl 8) or (bb.toInt() and 0xFF)
            val o = map16Base + 8 * block
            if (o < 0 || o + 8 > rom.size) { repeat(4) { entries.add(SnesTilemap.decodeEntry(0)) }; continue }
            for (k in 0..3) {
                val w = byte(rom, o + 2 * k) or (byte(rom, o + 2 * k + 1) shl 8)
                entries.add(SnesTilemap.decodeEntry(w))
            }
        }
        return entries
    }

    /**
     * Renderiza la ESCENA de fondo (Layer 2) de un nivel con COLOR REAL POR TESELA:
     * cada tesela 8×8 pintada con la sub-paleta (`CCC`) que le asigna su bloque Map16.
     * Devuelve null si el nivel no tiene fondo o faltan datos. La rejilla de SMW es
     * El buffer es screen-major (16×27 por screen, fila*16+col), como monta el juego
     * en BufferBGTilemap; se decodifica contra la VRAM de 4 slots y se colorea con la
     * CGRAM real del nivel.
     */
    internal fun renderSmwBackground(rom: ByteArray, header: SnesHeader, level: Int): ArgbImage? {
        val delta = smwHeaderDelta(header)
        val entries = layer2BgEntries(rom, delta, level)
        val blockCount = entries.size / 4
        if (blockCount < 64) return null

        // Cabecera del nivel: índices de paleta e info de GFX.
        val l1 = SMW_LAYER1_PTR_PC + delta + 3 * level
        val lpc = lorom(byte(rom, l1), byte(rom, l1 + 1), byte(rom, l1 + 2))
        if (lpc < 0 || lpc + 5 > rom.size) return null
        val tileset = byte(rom, lpc + 4) and 0x0F
        val se = SMW_FGBG_GFX_TABLE_PC + delta + 4 * tileset

        // VRAM de 4 slots (tiles 0x000/0x080/0x100/0x180), montada como UploadGraphicsFiles:
        // los 4 ficheros GFX del tileset en esos rangos, 0x80 teselas cada uno. El fondo
        // se decodifica contra la VRAM COMPLETA, no solo BG1 — ese era el bloqueo.
        val bases = findSmwGfxTable(rom) ?: return null
        val (bLo, bHi, bBank) = bases
        val vram = arrayOfNulls<IntArray>(512)
        for (s in 0..3) {
            val file = byte(rom, se + s); if (file == SMW_SLOT_EMPTY) continue
            val pc = lorom(byte(rom, bLo + file), byte(rom, bHi + file), byte(rom, bBank + file))
            if (pc < 0x40000 || pc >= rom.size) continue
            val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
            val fmt = SnesGraphicsScanner.detectBestFormat(data, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
            val avail = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
            for (t in 0 until minOf(avail, 128)) {
                vram[s * 128 + t] = SnesDecoder.decodeTile(data, t * fmt.bytesPerTile, fmt, t).pixelIndices
            }
        }
        if (vram.all { it == null }) return null

        // Gate de honestidad: la mayoría de las teselas del fondo deben caer en VRAM que
        // sabemos decodificar. Si un fondo usa sobre todo teselas no mapeables (animadas,
        // FG3…), lo omitimos en vez de pintarlo a medias.
        val distinct = entries.mapTo(HashSet()) { it.tileIndex }
        val mapped = entries.count { smwFgbgVramSlot(it.tileIndex) >= 0 && vram[it.tileIndex] != null }
        if (distinct.size < 4 || mapped.toDouble() / entries.size < 0.5) return null

        // CGRAM ensamblada del nivel (la MISMA que la escena FG), no la fila 3bpp
        // simplificada: esta trae el col 1 = blanco real, así las nubes salen blancas
        // en vez de negras. Índice de color 0 = back area (cielo), compartido por todas
        // las filas de fondo.
        val cgram = assembleSmwCgram(rom, delta, level)

        // Arrangement REAL (BufferBGTilemap + BufferScrollingTiles_Layer2_Background):
        // el buffer es screen-major; cada screen es 16 cols × 27 filas = 432 bloques,
        // ordenados fila*16+col. El bloque lineal b va a screen b/432, dentro fila
        // (b%432)/16 y columna b%432%16; las screens se colocan una tras otra a lo ancho.
        val h = 27
        val screens = (blockCount + 431) / 432
        val cols = maxOf(1, screens) * 16
        val img = ArgbImage(cols * 16, h * 16)
        for (i in img.pixels.indices) img.pixels[i] = cgram[0] // back area (cielo)
        // Posiciones de las 4 sub-teselas del bloque: TL, BL, TR, BR.
        val subPos = arrayOf(intArrayOf(0, 0), intArrayOf(0, 8), intArrayOf(8, 0), intArrayOf(8, 8))
        for (b in 0 until blockCount) {
            val screen = b / 432
            val rem = b % 432
            val row = rem / 16
            val col = screen * 16 + rem % 16
            if (col >= cols || row >= h) continue
            val ox = col * 16; val oy = row * 16
            for (k in 0..3) {
                val e = entries[b * 4 + k]
                val rowP = (e.palette and 7) * 16
                val bx = ox + subPos[k][0]; val by = oy + subPos[k][1]
                val px = if (smwFgbgVramSlot(e.tileIndex) >= 0) vram[e.tileIndex] else null
                if (px != null) {
                    for (py in 0..7) for (pxx in 0..7) {
                        val sx = if (e.hFlip) 7 - pxx else pxx
                        val sy = if (e.vFlip) 7 - py else py
                        val ci = px[sy * 8 + sx]
                        // ci 0 = transparente → back area (cielo), común a todo el fondo.
                        val argb = if (ci == 0) cgram[0] else cgram[rowP + ci]
                        img.set(bx + pxx, by + py, argb)
                    }
                } else {
                    // Tesela no mapeable: rellena con back area (cielo).
                    for (py in 0..7) for (pxx in 0..7) img.set(bx + pxx, by + py, cgram[0])
                }
            }
        }
        return img
    }

    /**
     * FONDOS (Layer 2) para la galería: renderiza los fondos de los niveles escaparate
     * ([SMW_SCENE_LEVELS]) que tienen Layer 2 de imagen y pasan el gate de honestidad.
     * Los niveles cuyo Layer 2 son objetos (no fondo) o cuyo fondo no es mapeable se
     * saltan solos. Máximo [maxScenes] fondos para no disparar la memoria.
     */
    internal fun extractSmwBackgrounds(rom: ByteArray, header: SnesHeader, maxScenes: Int = 3): List<SnesAutoExtractor.Finding> {
        val out = ArrayList<SnesAutoExtractor.Finding>()
        for (level in SMW_SCENE_LEVELS) {
            if (out.size >= maxScenes) break
            val img = renderSmwBackground(rom, header, level) ?: continue
            val cols = img.width / 16
            out.add(
                SnesAutoExtractor.Finding(
                    image = img, label = "Fondo nivel ${level.toString(16).uppercase()}", offset = 0,
                    compressed = false, format = SnesGraphicFormat.SNES_3BPP, palette = IntArray(0),
                    tileCount = cols * 27, columns = cols, score = 1.0,
                )
            )
        }
        return out
    }

    /**
     * Agrega, de la tabla de definiciones Map16 de FG ($0D8000), la sub-paleta (`CCC`)
     * mayoritaria de CADA número de tesela de VRAM. A diferencia del LAYOUT de un nivel
     * (que se monta en runtime), estas DEFINICIONES de bloque sí son estáticas: dicen
     * "el bloque B son estas 4 teselas con estas sub-paletas". Recorriendo las páginas
     * FG (0..0x1FF) obtenemos, por tesela, con qué fila la pinta el juego. Es la base
     * del color por-tesela del primer plano.
     */
    internal fun map16FgPaletteByTile(rom: ByteArray, delta: Int): Map<Int, Int> {
        val base = SMW_MAP16_FG_PC + delta
        val acc = HashMap<Int, IntArray>()
        for (b in 0 until 0x200) {
            val o = base + 8 * b
            if (o + 8 > rom.size) break
            for (k in 0..3) {
                val w = byte(rom, o + 2 * k) or (byte(rom, o + 2 * k + 1) shl 8)
                val e = SnesTilemap.decodeEntry(w)
                acc.getOrPut(e.tileIndex) { IntArray(8) }[e.palette]++
            }
        }
        return acc.mapValues { (_, c) -> var best = 0; for (i in 1 until 8) if (c[i] > c[best]) best = i; best }
    }

    /**
     * Colorea una hoja de tiles de FG con COLOR REAL POR TESELA: cada tesela recibe la
     * sub-paleta que le asigna el Map16 de FG. [gfxFile] es el fichero (p. ej. 0x14 para
     * FG1, 0x17 para FG2) y [slotBase] el número de tesela de VRAM donde arranca ese
     * slot (0x000 FG1, 0x080 FG2, 0x180 FG3). Índice 0 = transparente (tiles de mapa).
     */
    internal fun renderSmwForeground(
        rom: ByteArray, header: SnesHeader, gfxFile: Int, slotBase: Int, backIndex: Int = SMW_DEFAULT_BACK_INDEX,
    ): SnesAutoExtractor.Finding? {
        val delta = smwHeaderDelta(header)
        val cccByTile = map16FgPaletteByTile(rom, delta)
        val bases = findSmwGfxTable(rom) ?: return null
        val (bLo, bHi, bBank) = bases
        val pc = lorom(byte(rom, bLo + gfxFile), byte(rom, bHi + gfxFile), byte(rom, bBank + gfxFile))
        if (pc < 0x40000 || pc >= rom.size) return null
        val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: return null
        val fmt = smwFormat(data)
        val avail = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
        if (avail < 16) return null
        val count = minOf(avail, 128)
        val tables = SmwPaletteTables(rom, delta)
        val rowsByCcc = (0..7).map { row3bppFG(tables, it, backIndex) }
        val selector: (Int) -> IntArray = { t -> rowsByCcc[cccByTile[slotBase + t] ?: SMW_DEFAULT_FG_INDEX] }
        val sheet = SnesAssetExtractor.extractTileSheet(data, 0, fmt, count, 16, paletteForTile = selector)
        return SnesAutoExtractor.Finding(
            image = sheet.image, label = "SMW FG 0x${gfxFile.toString(16)}", offset = pc,
            compressed = true, format = fmt, palette = rowsByCcc[0], tileCount = count, columns = 16, score = 1.0,
        )
    }

    /**
     * Fila canónica de Mario (4bpp, 16 colores):
     * `[TRANSPARENTE, NEGRO, fix0..fix3, pl0..pl9]`, con `fix = fixedRow(8)` (los 4
     * primeros de los 6) y `pl = player(0)` (10 colores).
     */
    internal fun rowMario(t: SmwPaletteTables): IntArray {
        val fix = t.fixedRow(8)
        val pl = t.player(0)
        return intArrayOf(
            SMW_TRANSPARENT, SMW_BLACK, fix[0], fix[1], fix[2], fix[3],
            pl[0], pl[1], pl[2], pl[3], pl[4], pl[5], pl[6], pl[7], pl[8], pl[9],
        )
    }

    private fun lorom(lo: Int, hi: Int, bank: Int): Int {
        val addr = lo or (hi shl 8)
        return if (addr >= 0x8000) (bank and 0x7F) * 0x8000 + (addr - 0x8000) else -1
    }

    /**
     * Nº de ficheros de la tabla de descompresión de GFX de SMW: GFX00..GFX31
     * (0x32 entradas). GFX32/GFX33 existen pero se cargan por otra vía, no por esta
     * tabla (leer más allá se solaparía con la tabla de bytes altos).
     */
    private const val SMW_GFX_COUNT = 0x32

    /**
     * Tablas de punteros de GFX en el banco $00, VERIFICADAS contra el desensamblado
     * SMWDisX: GFXFilesLow=$00B992, GFXFilesHigh=$00B9C4, GFXFilesBank=$00B9F6. En
     * offset PC (unheadered, banco $00: PC = SNES − 0x8000): 0x3992/0x39C4/0x39F6.
     * (Antes estaban 5 bytes desplazadas —0x398D…—: el bucle empezaba en basura y se
     *  cortaba antes, dejándose GFX2F..GFX31 y desalineando el índice de fichero, con
     *  el que SmwLevelPalettes/SmwSlotTables asignan la sub-paleta REAL de cada hoja.)
     */
    internal val SMW_GFX_TABLE = Triple(0x3992, 0x39C4, 0x39F6)

    /**
     * Margen de aptitud por el que otro formato tiene que GANAR a 3bpp para creérselo.
     * Los gráficos de SMW son 3bpp casi sin excepción; cuando el detector genérico elige
     * 4bpp/2bpp por una diferencia mínima suele ser un error que sale como "ruido
     * arcoíris" (índices altos coloreados con basura). Exigiendo un margen claro, esos
     * empates técnicos vuelven a 3bpp —su formato real— y la hoja se ve.
     */
    private const val SMW_FORMAT_MARGIN = 0.06

    /**
     * Formato de un fichero de SMW con SESGO a 3bpp: solo se acepta otro formato si su
     * aptitud supera a la de 3bpp por [SMW_FORMAT_MARGIN]; si no, 3bpp (el real). Los
     * 4bpp de verdad (Mario, algún objeto) ganan por margen claro y se conservan.
     */
    internal fun smwFormat(data: ByteArray): SnesGraphicFormat {
        val guess = SnesGraphicsScanner.detectBestFormat(data, 0) ?: return SnesGraphicFormat.SNES_3BPP
        if (guess.format == SnesGraphicFormat.SNES_3BPP) return SnesGraphicFormat.SNES_3BPP
        val fit3 = SnesGraphicsScanner.formatFitness(data, 0, SnesGraphicFormat.SNES_3BPP)
        return if (guess.fitness - fit3 >= SMW_FORMAT_MARGIN) guess.format else SnesGraphicFormat.SNES_3BPP
    }

    /**
     * Localiza las tres tablas de punteros de GFX de SMW (byte bajo/alto/banco).
     * Prueba primero las posiciones de la ROM estándar y, si no validan, hace una
     * búsqueda acotada anclada en la tabla de bancos (tira de bytes de banco
     * válidos y no decrecientes). Elige la alineación que MÁS ficheros descomprime.
     */
    private fun findSmwGfxTable(rom: ByteArray): Triple<Int, Int, Int>? {
        fun successes(bLo: Int, bHi: Int, bBank: Int): Int {
            if (bLo < 0 || bBank + SMW_GFX_COUNT > rom.size) return 0
            var ok = 0
            for (i in 0 until SMW_GFX_COUNT) {
                val pc = lorom(byte(rom, bLo + i), byte(rom, bHi + i), byte(rom, bBank + i))
                if (pc < 0x40000 || pc >= rom.size) continue
                val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
                if (data.size >= 0x400) ok++
            }
            return ok
        }
        // 1) Posiciones de la ROM estándar (USA/EUR), del desensamblado: validar igual.
        // (GFXFilesLow/High/Bank = $00B992/$00B9C4/$00B9F6; una versión anterior usaba
        // 0x398D, corrido 5 bytes: decomprimía bien pero entregaba el fichero N-5.)
        val standard = SMW_GFX_TABLE
        if (successes(standard.first, standard.second, standard.third) >= 30) return standard

        // 2) Búsqueda acotada: tira de bancos válidos no decreciente en la zona baja.
        var best: Triple<Int, Int, Int>? = null
        var bestOk = 29
        var i = 0x3000
        while (i < minOf(rom.size - 52, 0x5000)) {
            val b = byte(rom, i) and 0x7F
            if (b in 0x06..0x10) {
                // ¿empieza aquí una tira de bancos plausible?
                var j = i
                while (j < i + 60 && j < rom.size && (byte(rom, j) and 0x7F) in 0x06..0x10 &&
                    (byte(rom, j) and 0x7F) >= (byte(rom, j - 1) and 0x7F)) j++
                if (j - i >= 40) {
                    for (bBank in (i - 6)..(i + 6)) {
                        for (s in intArrayOf(0x32, 0x33, 0x34)) {
                            val ok = successes(bBank - 2 * s, bBank - s, bBank)
                            if (ok > bestOk) { bestOk = ok; best = Triple(bBank - 2 * s, bBank - s, bBank) }
                        }
                    }
                    i = j
                    continue
                }
            }
            i++
        }
        return best
    }

    private fun byte(rom: ByteArray, i: Int): Int = if (i in rom.indices) rom[i].toInt() and 0xFF else 0

    /**
     * Descomprime el fichero GFX [file] (0x00..0x33) de SMW y devuelve sus bytes, o
     * null si el fichero no existe o la descompresión falla. Reutiliza la localización
     * robusta de la tabla de punteros de GFX ([findSmwGfxTable], que ya absorbe el delta
     * de cabecera SMC) y el mapeo LoROM del juego. Lo usan las recetas que necesitan un
     * fichero de gráficos concreto (p. ej. los sprites de enemigos, en los ficheros SP).
     */
    internal fun smwGfxFileData(rom: ByteArray, file: Int): ByteArray? {
        val (bLo, bHi, bBank) = findSmwGfxTable(rom) ?: return null
        val pc = lorom(byte(rom, bLo + file), byte(rom, bHi + file), byte(rom, bBank + file))
        if (pc < 0x40000 || pc >= rom.size) return null
        return runCatching { LcLz2.decompress(rom, pc).data }.getOrNull()
    }

    private fun extractSmw(rom: ByteArray, header: SnesHeader): List<SnesAutoExtractor.Finding> {
        val bases = findSmwGfxTable(rom) ?: return emptyList()
        val (bLo, bHi, bBank) = bases
        // Paletas REALES del juego. Para cada CLASE construimos sus 8 sub-paletas
        // reales (índices 0..7). La CLASE (FG/BG/sprite/player) la decide el slot; el
        // ÍNDICE 0..7 lo decide, cuando podemos, el color REAL que los niveles que
        // cargan ese fichero le asignan (SmwLevelPalettes, leyendo las cabeceras de
        // nivel en $05E000). Solo si no hay dato real cae al índice más coherente con
        // el dibujo (heurístico), para no volver al "todo verde" del índice 0 fijo.
        val delta = smwHeaderDelta(header)
        val tables = SmwPaletteTables(rom, delta)
        // Sub-paletas REALES por ÍNDICE 0..7, montadas como en la CGRAM del juego pero
        // con el índice 0 TRANSPARENTE (hoja de tileset, no escena): col 1 = blanco
        // $7FDD, cols 2-7 = la entrada FG/BG del índice, cols 8-F = berry (para los
        // pocos FG/BG 4bpp). El ÍNDICE lo elige el color REAL del nivel (SmwLevelPalettes),
        // así cada tileset (pradera, cueva, castillo, casa fantasma…) sale con SU color y
        // no todo teñido de verde de pradera.
        val white = SnesDecoder.bgr15ToArgb(0x7FDD)
        val berryCols = SnesDecoder.parsePalette(rom, SMW_BERRY_PC + delta, 7)
        fun sub(entry: IntArray): IntArray = intArrayOf(
            0, white, entry[0], entry[1], entry[2], entry[3], entry[4], entry[5],
            0, berryCols[0], berryCols[1], berryCols[2], berryCols[3], berryCols[4], berryCols[5], berryCols[6],
        )
        fun fgSub(k: Int) = sub(tables.fgEntry(k and 7))
        fun bgSub(k: Int) = sub(tables.bgEntry(k and 7))
        fun sprSub(k: Int) = sub(tables.sprEntry(k and 7))
        val marioRow = rowMario(tables)
        // Tablas de slot: si validan, deciden la CLASE por la VERDAD del juego (qué
        // ranura carga cada fichero). Si no (ROM modificada u offset dudoso), null y
        // se elige entre todas las clases por coherencia de dibujo.
        val slots = SmwSlotTables.readIfValid(rom, delta)
        // Índices de paleta REALES por fichero, derivados de los niveles que lo cargan.
        val levelPals = SmwLevelPalettes.read(rom, delta)
        val out = ArrayList<SnesAutoExtractor.Finding>()
        var n = 0
        for (i in 0 until SMW_GFX_COUNT) {
            val pc = lorom(byte(rom, bLo + i), byte(rom, bHi + i), byte(rom, bBank + i))
            if (pc < 0x40000 || pc >= rom.size) continue
            val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
            if (data.size < 0x300) continue
            // El bpp lo decide el detector (casi todo SMW es 3bpp; algunos 4bpp).
            val fmt = smwFormat(data)
            val available = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
            if (available < 16) continue
            val count = minOf(available, 128)
            val is4bpp = fmt == SnesGraphicFormat.SNES_4BPP
            val stats = SnesPaletteMatcher.indexStats(data, 0, fmt, count)
            // Índice 0..7 más coherente con el DIBUJO (solo como respaldo si no hay dato
            // real del nivel para este fichero).
            fun bestIndex(subFor: (Int) -> IntArray): Int =
                (0..7).maxByOrNull { SnesPaletteMatcher.scorePalette(subFor(it), stats) }!!
            // La CLASE la manda el slot; el ÍNDICE de sub-paleta lo manda el color REAL
            // con el que los niveles cargan este fichero (SmwLevelPalettes) — así cada
            // tileset sale con SU paleta (pradera verde, cueva azul, castillo gris…) y no
            // todo teñido de pradera. Si no hay dato, cae al índice más coherente con el
            // dibujo. Mario (GFX32/4bpp) va con su fila de jugador.
            val role = if (slots != null) slots.roleOf(i, is4bpp) else null
            val pal = when (role) {
                SmwGfxRole.PLAYER -> if (is4bpp) marioRow else fgSub(levelPals?.fgIndexByFile?.get(i) ?: bestIndex(::fgSub))
                SmwGfxRole.SPRITE -> sprSub(levelPals?.spriteIndexByFile?.get(i) ?: bestIndex(::sprSub))
                SmwGfxRole.BG -> bgSub(levelPals?.bgIndexByFile?.get(i) ?: bestIndex(::bgSub))
                SmwGfxRole.FG -> fgSub(levelPals?.fgIndexByFile?.get(i) ?: bestIndex(::fgSub))
                null -> {
                    val fi = bestIndex(::fgSub); val si = bestIndex(::sprSub)
                    if (SnesPaletteMatcher.scorePalette(sprSub(si), stats) >
                        SnesPaletteMatcher.scorePalette(fgSub(fi), stats)) sprSub(si) else fgSub(fi)
                }
            }
            val sheet = SnesAssetExtractor.extractTileSheet(data, 0, fmt, pal, count, 16)
            n++
            // Etiqueta por CATEGORÍA (el rol de slot que ya detectamos), para una
            // galería ORDENADA: fondos, tilesets de primer plano, sprites, personajes.
            val category = when (role) {
                SmwGfxRole.FG -> "Tileset FG"
                SmwGfxRole.BG -> "Tileset BG"
                SmwGfxRole.SPRITE -> "Sprites"
                SmwGfxRole.PLAYER -> "Personaje"
                null -> "Gráficos"
            }
            out.add(
                SnesAutoExtractor.Finding(
                    image = sheet.image,
                    label = "$category $n",
                    offset = pc,
                    compressed = true,
                    format = fmt,
                    palette = pal,
                    tileCount = count,
                    columns = 16,
                    score = 1.0,
                )
            )
        }
        // Mario (GFX32): puntero ESPECIAL + paleta de jugador REAL. Va aparte porque
        // no está en la tabla estándar y su color no se puede leer en frío de la ROM.
        runCatching {
            val mpc = lorom(
                byte(rom, SMW_GFX32_LO_PC + delta),
                byte(rom, SMW_GFX32_HI_PC + delta),
                byte(rom, SMW_GFX32_BANK_PC + delta),
            )
            if (mpc in 0x40000 until rom.size) {
                val mdata = LcLz2.decompress(rom, mpc).data
                val mfmt = SnesGraphicFormat.SNES_4BPP
                val mcount = minOf(SnesAssetExtractor.availableTiles(mdata.size, 0, mfmt), 128)
                if (mcount >= 16) {
                    // Paleta de Mario = fila 8 REAL de la CGRAM (sprite 0), montada como
                    // el juego: col 1 = blanco, cols 2-5 = StandardColors(fila 8), cols
                    // 6-F = mario_normal.pal (player 0). Sin horneado ni verde compartido.
                    val fix8 = tables.fixedRow(8); val pl = tables.player(0)
                    val marioPal = intArrayOf(
                        0, white, fix8[0], fix8[1], fix8[2], fix8[3],
                        pl[0], pl[1], pl[2], pl[3], pl[4], pl[5], pl[6], pl[7], pl[8], pl[9],
                    )
                    val sheet = SnesAssetExtractor.extractTileSheet(
                        mdata, 0, mfmt, marioPal, mcount, 16,
                    )
                    out.add(
                        SnesAutoExtractor.Finding(
                            image = sheet.image, label = "Personaje Mario", offset = mpc,
                            compressed = true, format = mfmt, palette = marioPal,
                            tileCount = mcount, columns = 16, score = 1.0,
                        )
                    )
                }
            }
        }
        // Orden final por categoría, para una galería limpia y navegable.
        val order = listOf("Personaje", "Tileset FG", "Tileset BG", "Sprites", "Gráficos")
        return out.sortedBy { f -> order.indexOfFirst { f.label.startsWith(it) }.let { if (it < 0) order.size else it } }
    }
}
