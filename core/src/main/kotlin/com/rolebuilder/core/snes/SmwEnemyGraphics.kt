package com.rolebuilder.core.snes

/**
 * Gráficos REALES de enemigos de Super Mario World: dado un nivel, devuelve la
 * imagen ARGB de un enemigo tal y como lo dibuja el juego, para que un motor de
 * plataformas pinte el sprite auténtico en vez de un bloque de color.
 *
 * Une lo que ya sabíamos leer por separado:
 *  - QUIÉN aparece y DÓNDE lo da [SmwSprites] (id de sprite por posición).
 *  - CÓMO se colorea, [SmwSpriteBehavior] (byte "tweaker" $166E).
 *  - El COLOR real del nivel lo ensambla [SnesGameRecipes.assembleSmwCgram].
 *
 * ─── De id de enemigo a teselas y paleta (verificado en snesrev/smw) ───
 * Cada nivel sube 4 ficheros GFX a la zona de sprites de la VRAM (`UploadGraphicsFiles`,
 * $00:A9DA); el "sprite GFX setting" del nivel indexa las ranuras SP1..SP4
 * ([SnesGameRecipes.SMW_SPRITE_GFX_TABLE_PC]). El nº de tesela de 9 bits del OAM mapea
 * 0x000..0x07F→SP1, 0x080..0x0FF→SP2, 0x100..0x17F→SP3, 0x180..0x1FF→SP4. SP1/SP2 son
 * SIEMPRE GFX00/GFX01, así que los enemigos "globales" tienen un mapeo INEQUÍVOCO.
 *
 * El número de tesela de cada enemigo sale de la TABLA REAL del juego, el
 * `kGenericSpriteOAMData_Tiles` del banco $01 (rutina `GenericGFXRtDraw1Tile16x16`),
 * indexada por `kGenericSpriteOAMData_TilesOffset[id]`: un enemigo terrestre común se
 * dibuja como UN sprite de 16×16 (2×2 teselas: N, N+1, N+0x10, N+0x11). La sub-paleta y
 * el bit de página de tesela salen del nibble bajo de $166E ([SmwSpriteBehavior.b166e]),
 * igual que `spr_table15f6 = Sprite166EVals[id] & 0xF` ($07:F78B): bit0 = página, bits1-3
 * = paleta de objeto 0..7 → fila 8+paleta de la CGRAM. Color REAL, sin adivinar.
 *
 * Cubrimos un catálogo curado y verificado renderizándolo desde la ROM real (Koopas,
 * Goomba, Buzzy, Bob-omb, Cheep-Cheep, caparazones, huevo de Yoshi, Rex, topo…);
 * [spriteImage] devuelve null para los ids fuera del catálogo.
 */
object SmwEnemyGraphics {

    private val FORMAT = SnesGraphicFormat.SNES_3BPP
    private const val TILES_PER_FILE = 0x80

    /** `kGenericSpriteOAMData_Tiles` (126 uint16) del banco $01 de SMW. */
    private val OAM_TILES = intArrayOf(
        0xa082, 0xa282, 0xa484, 0x8a8c, 0xc88e, 0xcaca, 0xccce, 0x4e86, 0xe2e0, 0xcee2, 0xe0e4, 0xa3e0, 0xb3a3, 0xe9b3,
        0xf9e8, 0xe8f8, 0xf8e9, 0xe2f9, 0xaae6, 0xa8a8, 0xa2aa, 0xb2a2, 0xc3b2, 0xd3c2, 0xc2d2, 0xd2c3, 0xe2d3, 0xcae6,
        0xcacc, 0xceac, 0xceae, 0x8383, 0xc4c4, 0x8383, 0xc5c5, 0xa68a, 0xa6a4, 0x80a8, 0x8082, 0x8484, 0x8484, 0x9494,
        0x9494, 0xb0a0, 0xd0a0, 0x8082, 0x0082, 0x0000, 0x8486, 0xec88, 0xa88c, 0x8eaa, 0xaeac, 0xec8e, 0xceee, 0xa8ee,
        0x40ee, 0xa040, 0xa0c0, 0xa4c0, 0xa4c4, 0xa0c4, 0xa0c0, 0x40c0, 0x2707, 0x294c, 0x2b4e, 0xa082, 0xa484, 0x6967,
        0xce88, 0xae8e, 0xa2a2, 0xb2b2, 0x4000, 0x4244, 0x422c, 0x2828, 0x2828, 0x4c4c, 0x4c4c, 0x8383, 0x6f6f, 0xbcac,
        0xa6ac, 0xaa8c, 0x8486, 0xecdc, 0xeede, 0x0606, 0x1616, 0x0707, 0x1717, 0x1616, 0x0606, 0x1717, 0x0707, 0x8684,
        0x0000, 0x0e00, 0x242a, 0x0602, 0x200a, 0x2822, 0x2e26, 0x4240, 0x040c, 0x6a2b, 0x88ed, 0xa88c, 0xaa8e, 0x8cae,
        0xa888, 0xacae, 0x8e8c, 0xeece, 0xc6c4, 0x8482, 0x8c86, 0xcece, 0x8988, 0xcece, 0x8889, 0xcef3, 0xcef3, 0xa9a7,
    )

    /** `kGenericSpriteOAMData_TilesOffset` (84 uint8): id de sprite → byte en [OAM_TILES]. */
    private val OAM_OFFSET = intArrayOf(
        0x9, 0x9, 0x10, 0x9, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x37, 0x0, 0x25, 0x25, 0x5a, 0x0, 0x4b, 0x4e,
        0x8a, 0x8a, 0x8a, 0x8a, 0x56, 0x3a, 0x46, 0x47, 0x69, 0x6b, 0x73, 0x0, 0x0, 0x80, 0x80, 0x80, 0x80, 0x8e, 0x90,
        0x0, 0x0, 0x3a, 0xf6, 0x94, 0x95, 0x63, 0x9a, 0xa6, 0xaa, 0xae, 0xb2, 0xc2, 0xc4, 0xd5, 0xd9, 0xd7, 0xd7, 0xe6,
        0xe6, 0xe6, 0xe2, 0x99, 0x17, 0x29, 0xe6, 0xe6, 0xe6, 0x0, 0xe8, 0x0, 0x8a, 0xe8, 0x0, 0xed, 0xea, 0x7f, 0xea,
        0xea, 0x3a, 0x3a, 0xfa, 0x71, 0x7f,
    )

    /** Bytes little-endian de [OAM_TILES] (el offset del juego es en bytes). */
    private val TILE_BYTES: IntArray = IntArray(OAM_TILES.size * 2) {
        if (it % 2 == 0) OAM_TILES[it / 2] and 0xFF else (OAM_TILES[it / 2] shr 8) and 0xFF
    }

    /**
     * Catálogo curado de ids con gráfico real fiable (verificado renderizando desde la
     * ROM), con nombre legible. El orden define el atlas horneado.
     */
    private val NAMES: Map<Int, String> = linkedMapOf(
        0x00 to "Koopa verde", 0x01 to "Koopa rojo", 0x02 to "Koopa azul", 0x03 to "Koopa amarillo",
        0x05 to "Koopa", 0x0F to "Goomba", 0x10 to "Goomba", 0x11 to "Buzzy Beetle",
        0x1C to "Bob-omb", 0x29 to "Caparazon verde", 0x2A to "Caparazon rojo",
        0x2C to "Huevo de Yoshi", 0x4B to "Rex", 0x4D to "Topo", 0x4E to "Topo",
        // Añadidos verificados contra la ROM (gráfico GLOBAL SP1/SP2 e imagen coherente):
        0x1A to "Planta Piraña", 0x4F to "Planta Piraña saltarina", 0x3E to "Interruptor P",
    )

    /** Ids cubiertos, en orden estable (el mismo que el atlas horneado). */
    val curatedIds: List<Int> = NAMES.keys.toList()

    /** ¿Tenemos gráfico curado para este id de sprite? */
    fun handles(spriteId: Int): Boolean = NAMES.containsKey(spriteId)

    /**
     * ¿Los gráficos de este sprite viven en SP1/SP2 (GFX00/GFX01), que TODO nivel sube a
     * la VRAM? Si es así, el sprite se dibuja igual en cualquier nivel y es un candidato
     * fiable para el roster; si usa SP3/SP4 (bit de página = 1) depende del nivel y solo
     * se ve bien en los que suben esa hoja concreta. `null` si el id no está en la tabla.
     */
    fun isGlobalGraphic(rom: ByteArray, header: SnesHeader, spriteId: Int): Boolean? {
        if (spriteId !in OAM_OFFSET.indices) return null
        val behaviors = SmwSpriteBehaviorReader.read(rom, header) ?: return null
        val page = behaviors[spriteId].b166e and 0x01
        return page == 0
    }

    /** Nombre legible del enemigo curado, o null. */
    fun nameOf(spriteId: Int): String? = NAMES[spriteId]

    /**
     * Imagen ARGB (16×16) del enemigo [spriteId] tal y como lo dibuja el juego en el
     * [level] dado, con la CGRAM y sub-paleta REALES del nivel; null si el id no está en
     * el catálogo, la ROM no parece SMW vanilla, o faltan datos del nivel. El índice de
     * color 0 queda transparente.
     */
    fun spriteImage(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): ArgbImage? {
        if (!NAMES.containsKey(spriteId)) return null
        return paint(rom, header, level, spriteId)
    }

    /**
     * Renderiza CUALQUIER id que exista en la tabla OAM real del juego, esté o no en el
     * catálogo curado [NAMES]. Pensado para herramientas de validación/descubrimiento:
     * permite ver qué ids producen un sprite coherente (candidatos a añadir) y cuáles
     * son ruido o usan una rutina de dibujo no genérica. En el juego, usa [spriteImage].
     */
    fun spriteImageAnyId(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): ArgbImage? {
        if (spriteId !in OAM_OFFSET.indices) return null
        return paint(rom, header, level, spriteId)
    }

    private fun paint(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): ArgbImage? {
        if (spriteId !in OAM_OFFSET.indices) return null
        val delta = header.headerOffset - 0x7FC0

        val behaviors = SmwSpriteBehaviorReader.read(rom, header) ?: return null
        val prop = behaviors[spriteId].b166e and 0x0F
        val page = prop and 0x01                        // bit 9 del nº de tesela
        val objPalette = (prop shr 1) and 0x07
        val cgRow = (8 + objPalette) * 16               // filas 8-15 = sprites

        val info = SnesGameRecipes.smwLevelInfo(rom, header, level) ?: return null
        val setting = info.spriteGfx and 0x0F
        val slotBase = SnesGameRecipes.SMW_SPRITE_GFX_TABLE_PC + delta + 4 * setting
        if (slotBase < 0 || slotBase + 4 > rom.size) return null
        val spData = arrayOfNulls<ByteArray>(4)
        for (s in 0..3) {
            val file = rom[slotBase + s].toInt() and 0xFF
            if (file == SnesGameRecipes.SMW_SLOT_EMPTY) continue
            spData[s] = SnesGameRecipes.smwGfxFileData(rom, file)
        }

        val cgram = SnesGameRecipes.assembleSmwCgram(rom, delta, level)

        fun tileIndices(tile9: Int): IntArray? {
            val slot = tile9 / TILES_PER_FILE
            if (slot !in 0..3) return null
            val data = spData[slot] ?: return null
            val local = tile9 % TILES_PER_FILE
            val off = local * FORMAT.bytesPerTile
            if (off + FORMAT.bytesPerTile > data.size) return null
            return SnesDecoder.decodeTile(data, off, FORMAT, local).pixelIndices
        }

        val base = TILE_BYTES[OAM_OFFSET[spriteId]] + page * 0x100
        val img = ArgbImage(16, 16) // pixels a 0 = transparente
        var painted = false
        // 16×16 = 4 teselas de 8×8: N, N+1 (arriba); N+0x10, N+0x11 (abajo).
        val sub = arrayOf(intArrayOf(0, 0, 0), intArrayOf(1, 8, 0), intArrayOf(0x10, 0, 8), intArrayOf(0x11, 8, 8))
        for (so in sub) {
            val px = tileIndices(base + so[0]) ?: continue
            for (y in 0..7) for (x in 0..7) {
                val ci = px[y * 8 + x]
                if (ci == 0) continue
                img.set(so[1] + x, so[2] + y, cgram[cgRow + (ci and 0x0F)])
                painted = true
            }
        }
        return if (painted) img else null
    }

    /**
     * Una entrada OAM tal y como la escribe el juego para dibujar (parte de) un sprite:
     * [charnum] nº de tesela de la tabla `_Tiles`; [dx]/[dy] desplazamiento CON SIGNO
     * respecto al origen del sprite; [size] 8 (tesela 8×8) o 16 (16×16, de `_TileSize`
     * 0→8 / 2→16); [prop] byte de propiedades OAM (bit7=Vflip, bit6=Hflip, bits3-1=paleta
     * 0-7 → filas CGRAM 8-15, bit0=página SP1/2 vs SP3/4).
     */
    data class OamPart(val charnum: Int, val dx: Int, val dy: Int, val size: Int, val prop: Int)

    /**
     * Compone un sprite ENSAMBLANDO sus entradas OAM reales tal y como lo hace el PPU:
     * cada [OamPart] toma su tesela del GFX de sprites del [level] y la CGRAM real, con
     * su tamaño, volteo, paleta y página. Es la vía FIEL para reconstruir sprites de
     * varias teselas (Koopa alado, para-goomba…): se transcriben sus tablas de dibujo de
     * snesrev/smw a una lista de [parts]. `null` si la ROM no es SMW vanilla, faltan los
     * GFX del nivel o la lista está vacía.
     */
    fun renderOam(rom: ByteArray, header: SnesHeader, level: Int, parts: List<OamPart>): ArgbImage? {
        if (parts.isEmpty()) return null
        val delta = header.headerOffset - 0x7FC0
        val info = SnesGameRecipes.smwLevelInfo(rom, header, level) ?: return null
        val slotBase = SnesGameRecipes.SMW_SPRITE_GFX_TABLE_PC + delta + 4 * (info.spriteGfx and 0x0F)
        if (slotBase < 0 || slotBase + 4 > rom.size) return null
        val spData = arrayOfNulls<ByteArray>(4)
        for (s in 0..3) {
            val file = rom[slotBase + s].toInt() and 0xFF
            if (file != SnesGameRecipes.SMW_SLOT_EMPTY) spData[s] = SnesGameRecipes.smwGfxFileData(rom, file)
        }
        val cgram = SnesGameRecipes.assembleSmwCgram(rom, delta, level)

        fun tileIndices(tile9: Int): IntArray? {
            val slot = tile9 / TILES_PER_FILE
            if (slot !in 0..3) return null
            val data = spData[slot] ?: return null
            val local = tile9 % TILES_PER_FILE
            val off = local * FORMAT.bytesPerTile
            if (off + FORMAT.bytesPerTile > data.size) return null
            return SnesDecoder.decodeTile(data, off, FORMAT, local).pixelIndices
        }

        var minX = 0; var minY = 0; var maxX = 0; var maxY = 0
        for (p in parts) {
            minX = minOf(minX, p.dx); minY = minOf(minY, p.dy)
            maxX = maxOf(maxX, p.dx + p.size); maxY = maxOf(maxY, p.dy + p.size)
        }
        val ox = -minX; val oy = -minY
        val img = ArgbImage(maxX - minX, maxY - minY)
        var painted = false
        for (p in parts) {
            val page = p.prop and 0x01
            val palRow = (8 + ((p.prop shr 1) and 0x07)) * 16
            val flipX = p.prop and 0x40 != 0
            val flipY = p.prop and 0x80 != 0
            val baseTile = p.charnum + page * 0x100
            // OAM 16×16 = 4 teselas (N, N+1, N+0x10, N+0x11); 8×8 = solo N.
            val quads = if (p.size == 16)
                arrayOf(intArrayOf(0, 0, 0), intArrayOf(1, 1, 0), intArrayOf(0x10, 0, 1), intArrayOf(0x11, 1, 1))
            else arrayOf(intArrayOf(0, 0, 0))
            val span = if (p.size == 16) 1 else 0
            for (q in quads) {
                val px = tileIndices(baseTile + q[0]) ?: continue
                val qcx = if (flipX) span - q[1] else q[1]
                val qcy = if (flipY) span - q[2] else q[2]
                for (y in 0..7) for (x in 0..7) {
                    val ci = px[y * 8 + x]
                    if (ci == 0) continue
                    val dstX = ox + p.dx + qcx * 8 + (if (flipX) 7 - x else x)
                    val dstY = oy + p.dy + qcy * 8 + (if (flipY) 7 - y else y)
                    if (dstX in 0 until img.width && dstY in 0 until img.height) {
                        img.set(dstX, dstY, cgram[palRow + (ci and 0x0F)])
                        painted = true
                    }
                }
            }
        }
        return if (painted) img else null
    }
}
