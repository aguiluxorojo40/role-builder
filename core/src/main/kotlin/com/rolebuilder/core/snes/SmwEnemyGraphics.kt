package com.rolebuilder.core.snes

/**
 * Gráficos REALES de enemigos de Super Mario World: dado un nivel, devuelve la
 * imagen ARGB de un enemigo tal y como lo dibuja el juego, para que un motor de
 * plataformas pinte el sprite auténtico en vez de un bloque de color.
 *
 * Es la pieza que une lo que ya sabíamos leer por separado:
 *  - QUIÉN aparece y DÓNDE lo da [SmwSprites] (id de sprite por posición).
 *  - CÓMO se comporta y con qué paleta, [SmwSpriteBehavior] (bytes "tweaker").
 *  - El COLOR real del nivel lo ensambla [SnesGameRecipes.assembleSmwCgram] (CGRAM
 *    completa de 256 colores, igual que la rutina LoadPalette del juego).
 * Aquí añadimos el eslabón que faltaba: de un id de enemigo a sus TESELAS reales en
 * la VRAM de sprites y a la sub-paleta con la que el juego las colorea.
 *
 * ─── Cómo carga SMW los gráficos de sprites (verificado en snesrev/smw) ───
 * Cada nivel sube 4 ficheros GFX a la zona de sprites de la VRAM (rutina
 * `UploadGraphicsFiles`, $00:A9DA). El "sprite GFX setting" del nivel (nibble bajo
 * del byte 2 de la cabecera) indexa una tabla de 4 ficheros por entrada
 * ([SnesGameRecipes.SMW_SPRITE_GFX_TABLE_PC], $00:A8C3): son las ranuras SP1..SP4.
 * Se colocan en VRAM (palabras 0x6000/0x6800/0x7000/0x7800) de modo que el número
 * de tesela de 9 bits del OAM mapea así:
 *   0x000..0x07F → SP1   0x080..0x0FF → SP2   0x100..0x17F → SP3   0x180..0x1FF → SP4
 * SP1 y SP2 son SIEMPRE GFX00 y GFX01 en TODAS las entradas de la tabla: por eso los
 * enemigos "globales" (Koopas, Goomba) tienen un mapeo tesela→GFX INEQUÍVOCO,
 * independiente del nivel. Por eso empezamos por ellos.
 *
 * ─── De id de enemigo a teselas y paleta ───
 * El número de tesela y la animación salen de las tablas OAM del banco $01
 * (`kGenericSpriteOAMData_Tiles`/`_TilesOffset`, rutina `GenericGFXRtDraw...`): un
 * enemigo terrestre común se dibuja como UN sprite de 16×16 (2×2 teselas de 8×8:
 * N, N+1, N+0x10, N+0x11). La sub-paleta y el bit de "página" de tesela salen del
 * nibble bajo del byte tweaker $166E ([SmwSpriteBehavior.b166e]), que es
 * EXACTAMENTE lo que el juego copia a la propiedad OAM del sprite
 * (`spr_table15f6 = Sprite166EVals[id] & 0xF`, $07:F78B): bit0 = página de tesela,
 * bits1-3 = paleta de objeto 0..7 → fila 8+paleta de la CGRAM. Así el color es el
 * REAL del nivel, sin adivinar.
 *
 * Fidelidad por delante de amplitud: cubrimos un puñado curado y verificado contra
 * la ROM real (Koopas verde/rojo/azul/amarillo y el Goomba); [spriteImage] devuelve
 * null para los ids que aún no tratamos.
 */
object SmwEnemyGraphics {

    /** SMW guarda los gráficos de sprites en 3bpp (8 colores; índice 0 transparente). */
    private val FORMAT = SnesGraphicFormat.SNES_3BPP

    /** Nº de teselas de 8×8 por fichero GFX de sprite en VRAM (0x80 = 16×8). */
    private const val TILES_PER_FILE = 0x80

    /**
     * Una parte de 16×16 del dibujo de un enemigo: la tesela superior-izquierda
     * [tile] (número OAM de 8 bits; el bit 9 de página lo aporta el tweaker $166E) y
     * su desplazamiento en píxeles dentro de la imagen final.
     */
    private class Part(val tile: Int, val dx: Int, val dy: Int)

    /** Definición curada de un enemigo: nombre legible y sus partes de 16×16. */
    private class Enemy(val name: String, val widthPx: Int, val heightPx: Int, val parts: List<Part>)

    /**
     * Tabla curada id de sprite → dibujo. Números de tesela tomados de las tablas OAM
     * vanilla del banco $01 (frame de animación 0, mirando a su dirección por defecto)
     * y verificados renderizándolos desde la ROM real:
     *  - 0x00..0x03 Koopa Troopa (verde/rojo/azul/amarillo): un 16×16. Verde/rojo/
     *    amarillo comparten la tesela 0xC8; el azul usa 0xE0. El COLOR distinto sale
     *    solo de la sub-paleta ($166E), no de teselas distintas.
     *  - 0x10 Goomba (cuerpo del Para-Goomba, su forma terrestre): un 16×16, tesela 0xAA.
     */
    private val ENEMIES: Map<Int, Enemy> = mapOf(
        0x00 to Enemy("Koopa verde", 16, 16, listOf(Part(0xC8, 0, 0))),
        0x01 to Enemy("Koopa rojo", 16, 16, listOf(Part(0xC8, 0, 0))),
        0x02 to Enemy("Koopa azul", 16, 16, listOf(Part(0xE0, 0, 0))),
        0x03 to Enemy("Koopa amarillo", 16, 16, listOf(Part(0xC8, 0, 0))),
        0x10 to Enemy("Goomba", 16, 16, listOf(Part(0xAA, 0, 0))),
    )

    /** ¿Tenemos gráfico curado para este id de sprite? */
    fun handles(spriteId: Int): Boolean = ENEMIES.containsKey(spriteId)

    /** Nombre legible del enemigo curado, o null. Útil para depurar/galería. */
    fun nameOf(spriteId: Int): String? = ENEMIES[spriteId]?.name

    /**
     * Devuelve la imagen ARGB (16×16) del enemigo [spriteId] tal y como lo dibuja el
     * juego en el [level] dado, coloreada con la CGRAM y la sub-paleta REALES del
     * nivel. Devuelve null si el id no está en la lista curada, si la ROM no parece
     * SMW vanilla en las tablas que necesitamos, o si faltan los datos del nivel.
     *
     * El índice de color 0 queda transparente (alfa 0), como en el OAM real.
     */
    fun spriteImage(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): ArgbImage? {
        val enemy = ENEMIES[spriteId] ?: return null
        if (spriteId !in 0..200) return null
        val delta = header.headerOffset - 0x7FC0

        // Propiedad OAM del sprite = nibble bajo del tweaker $166E (bit0 = página de
        // tesela, bits1-3 = paleta de objeto). Igual que spr_table15f6 en el juego.
        val behaviors = SmwSpriteBehaviorReader.read(rom, header) ?: return null
        val prop = behaviors[spriteId].b166e and 0x0F
        val nameHighBit = prop and 0x01                 // bit 9 del nº de tesela
        val objPalette = (prop shr 1) and 0x07          // 0..7
        val cgRow = (8 + objPalette) * 16               // filas 8-15 = sprites

        // Fichero GFX de cada ranura SP1..SP4 según el "sprite GFX setting" del nivel.
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

        // CGRAM real del nivel (256 colores ARGB), como la monta LoadPalette.
        val cgram = SnesGameRecipes.assembleSmwCgram(rom, delta, level)

        // Devuelve los 64 índices (0..7) de la tesela OAM de 9 bits [tile9], o null si
        // su ranura SP no está cargada.
        fun tileIndices(tile9: Int): IntArray? {
            val slot = tile9 / TILES_PER_FILE           // 0..3 → SP1..SP4
            if (slot !in 0..3) return null
            val data = spData[slot] ?: return null
            val local = tile9 % TILES_PER_FILE
            val off = local * FORMAT.bytesPerTile
            if (off + FORMAT.bytesPerTile > data.size) return null
            return SnesDecoder.decodeTile(data, off, FORMAT, local).pixelIndices
        }

        val img = ArgbImage(enemy.widthPx, enemy.heightPx) // pixels ya a 0 = transparente
        var painted = false
        // Cada parte de 16×16 = 4 teselas de 8×8: N, N+1 (arriba); N+0x10, N+0x11 (abajo).
        val subOffsets = arrayOf(intArrayOf(0, 0, 0), intArrayOf(1, 8, 0), intArrayOf(0x10, 0, 8), intArrayOf(0x11, 8, 8))
        for (part in enemy.parts) {
            val base = part.tile + nameHighBit * 0x100
            for (so in subOffsets) {
                val px = tileIndices(base + so[0]) ?: continue
                val ox = part.dx + so[1]
                val oy = part.dy + so[2]
                for (y in 0..7) for (x in 0..7) {
                    val ci = px[y * 8 + x]
                    if (ci == 0) continue                // índice 0 = transparente
                    img.set(ox + x, oy + y, cgram[cgRow + (ci and 0x0F)])
                    painted = true
                }
            }
        }
        return if (painted) img else null
    }
}
