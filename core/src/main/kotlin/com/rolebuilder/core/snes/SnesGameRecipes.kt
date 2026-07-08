package com.rolebuilder.core.snes

import com.rolebuilder.core.snes.compression.LcLz2
import com.rolebuilder.core.snes.compression.Rle1

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
        // Nota: las ESCENAS de fondo (Layer 2) están implementadas y validadas
        // (RLE1 + Map16 + renderSmwBackground: algunos fondos salen perfectos, como las
        // colinas de Yoshi's Island), pero el mapeo VRAM de CADA fondo varía y el render
        // fiable de todos necesita más ingeniería inversa. Para no ensuciar la galería
        // con escenas a medias, de momento la receta entrega solo las HOJAS de tiles con
        // color por-tesela; el motor de fondos queda listo para reactivarlo (ver
        // renderSmwBackground / layer2BgEntries) cuando se resuelva el layout de VRAM.
        // La galería entrega las HOJAS por categoría coloreadas con la CGRAM REAL del
        // juego (ensamblada por assembleSmwCgram, validada 48/48 contra emulador): FG y
        // BG POR-TESELA con su sub-paleta Map16, sprites y Mario con su fila real. El
        // render Map16 de "todos los bloques" (extractSmwMap16Tileset) queda como base
        // para las ESCENAS de nivel (objetivo B): en frío pinta también los ~cientos de
        // bloques SIN USAR (magenta placeholder), así que no va en la galería hasta
        // acotarlo al tilemap real de un nivel.
        "Super Mario World" -> extractSmw(rom, header)
        else -> emptyList()
    }

    /**
     * Renderiza el TILESET Map16 de SMW: monta cada bloque 16×16 del juego (suelo,
     * tuberías, bloques…) desde la VRAM del nivel (sus 4 slots de GFX) y lo colorea
     * con la CGRAM real por su CCC. Es la hoja de tiles LIMPIA y usable para pintar
     * mapas — los ladrillos de verdad de SMW, no un atlas de VRAM en crudo.
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
            val fmt = SnesGraphicsScanner.detectBestFormat(data, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
            val avail = SnesAssetExtractor.availableTiles(data.size, 0, fmt)
            for (t in 0 until minOf(avail, 128)) {
                vram[s * 128 + t] = SnesDecoder.decodeTile(data, t * fmt.bytesPerTile, fmt, t).pixelIndices
            }
        }
        if (vram.all { it == null }) return null
        // CGRAM REAL del nivel, ensamblada como el juego (no horneada): color correcto.
        val cgram = assembleSmwCgram(rom, delta, 0x106)
        // Renderiza los 512 bloques Map16 (8 bytes = 4 teselas + su CCC/flip).
        val map16 = SMW_MAP16_FG_PC + delta
        val cols = 16; val blocks = 512
        val img = ArgbImage(cols * 16, (blocks / cols) * 16)
        val subPos = arrayOf(intArrayOf(0, 0), intArrayOf(0, 8), intArrayOf(8, 0), intArrayOf(8, 8))
        for (blk in 0 until blocks) {
            val o = map16 + 8 * blk
            if (o + 8 > rom.size) break
            val bx = (blk % cols) * 16; val by = (blk / cols) * 16
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
        return pal
    }

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
    internal fun layer2BgEntries(rom: ByteArray, delta: Int, level: Int): List<SnesTilemap.TilemapEntry> {
        val p = SMW_LAYER2_PTR_PC + delta + 3 * level
        if (p < 0 || p + 2 >= rom.size) return emptyList()
        if (byte(rom, p + 2) != SMW_BG_IS_BACKGROUND) return emptyList() // no es un fondo
        val addr = byte(rom, p) or (byte(rom, p + 1) shl 8)
        if (addr < 0x8000) return emptyList()
        val dataPc = SMW_BG_BANK_PC + delta + (addr - 0x8000)
        if (dataPc < 0 || dataPc >= rom.size) return emptyList()
        val blocks = runCatching { Rle1.decompress(rom, dataPc, 0x4000).data }.getOrNull() ?: return emptyList()
        if (blocks.size < 4) return emptyList()
        val page = if (dataPc - delta < SMW_BG_PAGE_THRESHOLD_PC) 0 else 1
        val map16Base = SMW_MAP16_L2_PC + delta
        val entries = ArrayList<SnesTilemap.TilemapEntry>(blocks.size * 4)
        for (bb in blocks) {
            val block = page * 0x100 + (bb.toInt() and 0xFF)
            val o = map16Base + 8 * block
            if (o < 0 || o + 8 > rom.size) continue
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
     * 32×27 bloques 16×16; el flujo llena primero la mitad izquierda y luego la
     * derecha, en column-major dentro de cada mitad.
     */
    internal fun renderSmwBackground(rom: ByteArray, header: SnesHeader, level: Int): ArgbImage? {
        val delta = smwHeaderDelta(header)
        val entries = layer2BgEntries(rom, delta, level)
        val blockCount = entries.size / 4
        if (blockCount < 64) return null

        // Gate de calidad: solo renderizamos fondos cuyo contenido cae en el slot BG1
        // (tile# 0x100..0x17F), que es el que sabemos mapear a su fichero GFX con
        // certeza. Algunos fondos dibujan con teselas de otras regiones de VRAM cuyo
        // mapeo exacto aún no está resuelto; esos se omiten para no ensuciar la galería
        // con escenas a medias. Mejor pocos fondos correctos que muchos rotos.
        // Gate de calidad: solo mostramos fondos cuyo contenido cae mayoritariamente en
        // el slot BG1 (tile# 0x100..0x17F), que sabemos mapear limpio a su fichero GFX.
        // Los fondos que dibujan con teselas de otras regiones de VRAM (cuyo mapeo exacto
        // aún no está resuelto) se omiten: mejor pocos fondos correctos que muchos a
        // medias. Contamos por ENTRADA para que el cielo (fuera de BG1) no infle nada.
        val distinct = entries.mapTo(HashSet()) { it.tileIndex }
        val inBg1 = entries.count { it.tileIndex in 0x100..0x17F }
        if (distinct.size < 4 || inBg1.toDouble() / entries.size < 0.45) return null

        // Cabecera del nivel: índices de paleta e info de GFX.
        val l1 = SMW_LAYER1_PTR_PC + delta + 3 * level
        val lpc = lorom(byte(rom, l1), byte(rom, l1 + 1), byte(rom, l1 + 2))
        if (lpc < 0 || lpc + 5 > rom.size) return null
        val backIdx = byte(rom, lpc + 1) shr 5          // CCC de back area
        val fgbgSetting = byte(rom, lpc + 4) and 0x0F    // ZZZZ
        val bgFile = byte(rom, SMW_FGBG_GFX_TABLE_PC + delta + 4 * fgbgSetting + 2) // BG1

        val bases = findSmwGfxTable(rom) ?: return null
        val (bLo, bHi, bBank) = bases
        fun gfxData(file: Int): ByteArray? {
            if (file == SMW_SLOT_EMPTY) return null
            val pc = lorom(byte(rom, bLo + file), byte(rom, bHi + file), byte(rom, bBank + file))
            if (pc < 0x40000 || pc >= rom.size) return null
            return runCatching { LcLz2.decompress(rom, pc).data }.getOrNull()
        }
        val bgData = gfxData(bgFile) ?: return null
        val fmt = SnesGraphicsScanner.detectBestFormat(bgData, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
        val avail = SnesAssetExtractor.availableTiles(bgData.size, 0, fmt)
        val tables = SmwPaletteTables(rom, delta)
        // Filas por CCC (una por sub-paleta 0..7). El índice 0 es el color de back
        // area (cielo), NO transparente: los fondos son opacos.
        val rowsByCcc = (0..7).map { row3bppBG(tables, it, backIdx) }

        val w = 32; val h = 27
        val cols = if (blockCount % h == 0) blockCount / h else w
        val img = ArgbImage(cols * 16, h * 16)
        val half = blockCount / 2
        // Posiciones de las 4 sub-teselas del bloque: TL, BL, TR, BR.
        val subPos = arrayOf(intArrayOf(0, 0), intArrayOf(0, 8), intArrayOf(8, 0), intArrayOf(8, 8))
        for (b in 0 until blockCount) {
            val leftHalf = b < half
            val idx = if (leftHalf) b else b - half
            val colInHalf = idx / h
            val row = idx % h
            val col = (if (leftHalf) 0 else cols / 2) + colInHalf
            if (col >= cols) continue
            val ox = col * 16; val oy = row * 16
            for (k in 0..3) {
                val e = entries[b * 4 + k]
                val t = e.tileIndex - 0x100 // slot BG1; fuera de rango = cielo (back area)
                val palette = rowsByCcc[e.palette and 7]
                val bx = ox + subPos[k][0]; val by = oy + subPos[k][1]
                if (t in 0 until avail) {
                    val dec = SnesDecoder.decodeTile(bgData, t * fmt.bytesPerTile, fmt, t)
                    for (py in 0..7) for (px in 0..7) {
                        val sx = if (e.hFlip) 7 - px else px
                        val sy = if (e.vFlip) 7 - py else py
                        val ci = dec.pixelIndices[sy * 8 + sx]
                        val argb = if (ci < palette.size) palette[ci] else 0xFF000000.toInt()
                        img.set(bx + px, by + py, argb)
                    }
                } else {
                    // Tesela fuera del slot BG1 (p. ej. FG): rellena con back area.
                    for (py in 0..7) for (px in 0..7) img.set(bx + px, by + py, palette[0])
                }
            }
        }
        return img
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
        val fmt = SnesGraphicsScanner.detectBestFormat(data, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
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
            val fmt = SnesGraphicsScanner.detectBestFormat(data, 0)?.format ?: SnesGraphicFormat.SNES_3BPP
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
