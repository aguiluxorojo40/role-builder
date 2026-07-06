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
        "Super Mario World" -> extractSmw(rom, header)
        else -> emptyList()
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
    /** FG palette: $00B190 + 0x18*x → PC 0x3190 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_FG_PC = 0x3190
    /** Fijas filas 4-D: $00B250 + 0x0C*(row-4) → PC 0x3250 + 0x0C*(row-4) (6 colores). */
    internal const val SMW_FIXED_PC = 0x3250
    /** Player palette: $00B2C8 + 0x14*p → PC 0x32C8 + 0x14*p (10 colores). */
    internal const val SMW_PLAYER_PC = 0x32C8
    /** Sprite palette: $00B318 + 0x18*x → PC 0x3318 + 0x18*x (x=0..7, 12 colores). */
    internal const val SMW_SPRITE_PC = 0x3318

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
     * Localiza las tres tablas de punteros de GFX de SMW (byte bajo/alto/banco).
     * Prueba primero las posiciones de la ROM estándar y, si no validan, hace una
     * búsqueda acotada anclada en la tabla de bancos (tira de bytes de banco
     * válidos y no decrecientes). Elige la alineación que MÁS ficheros descomprime.
     */
    private fun findSmwGfxTable(rom: ByteArray): Triple<Int, Int, Int>? {
        fun successes(bLo: Int, bHi: Int, bBank: Int): Int {
            if (bLo < 0 || bBank + 52 > rom.size) return 0
            var ok = 0
            for (i in 0 until 52) {
                val pc = lorom(byte(rom, bLo + i), byte(rom, bHi + i), byte(rom, bBank + i))
                if (pc < 0x40000 || pc >= rom.size) continue
                val data = runCatching { LcLz2.decompress(rom, pc).data }.getOrNull() ?: continue
                if (data.size >= 0x400) ok++
            }
            return ok
        }
        // 1) Posiciones de la ROM estándar (USA/EUR): validar antes de fiarse.
        val standard = Triple(0x398D, 0x39BF, 0x39F1)
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
        // Paletas REALES del juego (no adivinadas). Para cada CLASE construimos sus
        // 8 sub-paletas reales (índices 0..7 de la tabla). La clase la decide el slot;
        // el índice, la coherencia con el dibujo: la cabecera de nivel elige un índice
        // 0..7 que la galería (sin nivel) no conoce, así que en vez de fijar el 0
        // (que es el terreno verde → todo salía verde) probamos los 8 y nos quedamos
        // con el que mejor encaja. Da a cada hoja su paleta real más plausible.
        val delta = smwHeaderDelta(header)
        val tables = SmwPaletteTables(rom, delta)
        val fgRows = (0..7).map { row3bppFG(tables, it, SMW_DEFAULT_BACK_INDEX) }
        val bgRows = (0..7).map { row3bppBG(tables, it, SMW_DEFAULT_BACK_INDEX) }
        val spriteRows = (0..7).map { row3bppSprite(tables, it) }
        val marioRow = rowMario(tables)
        // Tablas de slot: si validan, deciden la CLASE por la VERDAD del juego (qué
        // ranura carga cada fichero). Si no (ROM modificada u offset dudoso), null y
        // se elige entre todas las clases por coherencia de dibujo.
        val slots = SmwSlotTables.readIfValid(rom, delta)
        val out = ArrayList<SnesAutoExtractor.Finding>()
        var n = 0
        for (i in 0 until 52) {
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
            fun bestOf(rows: List<IntArray>): IntArray =
                rows.maxByOrNull { SnesPaletteMatcher.scorePalette(it, stats) }!!
            // La CLASE la manda el slot (la verdad del juego); dentro de ella se elige
            // el sub-índice 0..7 más coherente con el dibujo. La fila de Mario solo
            // aplica a GFX32/4bpp; imponerla a todo 4bpp teñía de arcoíris fuentes/HUD.
            val pal = if (slots != null) {
                when (slots.roleOf(i, is4bpp)) {
                    SmwGfxRole.PLAYER -> if (is4bpp) marioRow else bestOf(fgRows)
                    SmwGfxRole.SPRITE -> bestOf(spriteRows)
                    SmwGfxRole.BG -> bestOf(bgRows)
                    SmwGfxRole.FG -> bestOf(fgRows)
                }
            } else {
                val all = if (is4bpp) fgRows + spriteRows + marioRow else fgRows + spriteRows
                bestOf(all)
            }
            val sheet = SnesAssetExtractor.extractTileSheet(data, 0, fmt, pal, count, 16)
            n++
            out.add(
                SnesAutoExtractor.Finding(
                    image = sheet.image,
                    label = "SMW $n",
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
        return out
    }
}
