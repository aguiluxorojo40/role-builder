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
     * `kSprXXX_Generic_Spr0to13Prop` (banco $01): propiedades de DIBUJO de los ids
     * 0x00-0x13. El bit 0x40 = el sprite se dibuja como DOS bloques 16×16 APILADOS
     * (`GenericGFXRtDraw2Tiles16x16sStacked`, ~16×32 de alto): son las Koopas CON
     * caparazón (0x04-0x07) y las aladas (0x08-0x0B). Su entrada de [OAM_TILES] lleva
     * los DOS bloques (byte 0 = arriba, byte 1 = abajo); pintando solo el primero la
     * Koopa sale como una cabeza suelta "sin caparazón".
     */
    private val SPR0TO13_PROP = intArrayOf(
        0x00, 0x02, 0x03, 0x0d, 0x40, 0x42, 0x43, 0x45, 0x50, 0x50,
        0x50, 0x5c, 0xdd, 0x05, 0x00, 0x20, 0x20, 0x00, 0x00, 0x00,
    )

    /** Último id de la familia "genérica andadora" (0x00-0x13): la que ANIMA el andar. */
    private const val LAST_GENERIC_WALKER = 0x13

    /**
     * Plantas Piraña de TUBO (Classic recta 0x1A, cabeza-abajo 0x2A). Su entrada OAM está
     * en formato APILADO (top,bottom,top,bottom, `off + 2·frame`), pero SOLO la tesela de
     * ARRIBA es la BOCA que anima (0xAC cerrada ↔ 0xAE abierta); la de abajo (0xCE) es un
     * tile ajeno (un pez) que en el juego queda OCULTO dentro del tubo (OBJ_Priority1). Por
     * eso la dibujamos como una BOCA 16×16 que abre/cierra, con paso 2 (la tesela de arriba
     * de cada par). Las SALTARINAS (0x4F/0x50) NO van aquí: llevan tallo/hojas y su propio
     * dibujo de 2 partes ([jumpingPiranhaFrames]).
     */
    private val PIRANHAS = setOf(0x1A, 0x2A)

    /** ¿El id se dibuja APILADO (16×32, p. ej. Koopa con caparazón)? */
    fun isTall(spriteId: Int): Boolean =
        spriteId in SPR0TO13_PROP.indices && (SPR0TO13_PROP[spriteId] and 0x40) != 0

    /**
     * Catálogo curado de ids con gráfico real fiable (verificado renderizando desde la
     * ROM), con nombre legible. El orden define el atlas horneado: los ids NUEVOS se
     * añaden SIEMPRE al final para no mover los fotogramas ya horneados. El atlas se
     * regenera con `--enemies` en el extractor (no editar enemies.png a mano).
     *
     * Los nombres siguen el despacho real del juego (`kSprStatus08SpriteNormalPtrs`,
     * banco $01 de snesrev/smw); algunos de la primera tanda estaban mal etiquetados
     * (0x1C es Bullet Bill, no Bob-omb; 0x29/0x2A/0x4B ídem) y se corrigieron.
     */
    private val NAMES: Map<Int, String> = linkedMapOf(
        // Tanda 0 (el orden fija los fotogramas 0..14 del atlas horneado).
        // OJO: 0x00-0x03 son los Koopa SIN caparazón (el "beach koopa"), no los normales.
        // Los que llevan caparazón son 0x04-0x07 (ver la tanda 6 al final).
        0x00 to "Koopa sin caparazon verde", 0x01 to "Koopa sin caparazon rojo",
        0x02 to "Koopa sin caparazon azul", 0x03 to "Koopa sin caparazon amarillo",
        0x05 to "Koopa rojo", 0x0F to "Goomba", 0x10 to "Goomba volador", 0x11 to "Buzzy Beetle",
        0x1C to "Bullet Bill", 0x29 to "Koopa Kid", 0x2A to "Planta Pirana techo",
        0x2C to "Huevo de Yoshi", 0x4B to "Lakitu de tuberia", 0x4D to "Topo", 0x4E to "Topo",
        // Tanda 1: los ids mas frecuentes de la ROM US aun sin cubrir, verificados
        // visualmente con `--enemies` (voto por mayoria entre los niveles que los
        // contienen). Se DESCARTARON 0x33 Podoboo, 0x30 y 0x32: su entrada de la
        // tabla generica no es su aspecto real (rutina de dibujo propia; salian
        // tiles de fuente o basura de forma unanime en todos sus niveles).
        0x4F to "Planta Pirana saltarina", 0x37 to "Boo",
        0x3D to "Rip Van Fish", 0x15 to "Cheep-Cheep", 0x16 to "Cheep-Cheep",
        0x2E to "Spike Top", 0x38 to "Eerie", 0x39 to "Eerie",
        0x31 to "Bony Beetle",
        // Tanda 3: las Koopas ALADAS (Parakoopa, 0x08..0x0B) — los enemigos MÁS
        // colocados de la ROM. Ahora sí, portando su dibujo real (`Spr0to13Gfx` +
        // `KoopaWingGfxRt`, banco $01): cuerpo CON caparazón (misma entrada apilada que
        // las Koopas con caparazón) + ala con sus dos fotogramas de aleteo. Se hornean
        // con [wingedKoopaFrames] en celda ancha, no por la vía genérica.
        0x08 to "Koopa verde volador", 0x09 to "Koopa verde saltarin",
        0x0A to "Koopa rojo vertical", 0x0B to "Koopa rojo horizontal",
        // Tanda 4: la Planta Piraña RECTA (0x1A), la más común del juego y que faltaba.
        // Se dibuja apilada (16×32) con `SubSprGfx1` y ABRE/CIERRA la boca (2 fotogramas),
        // igual que la de cabeza-abajo (0x2A) y la saltarina (0x4F), ahora también apiladas.
        0x1A to "Planta Pirana",
        // Tanda 5: las Plantas Piraña SALTARINAS, con tallo/hojas-hélice ([jumpingPiranhaFrames]).
        // 0x4F ya estaba (se recataloga como saltarina); 0x50 es la que escupe fuego (mismo
        // sprite, el disparo es un extended sprite aparte que el motor no simula).
        0x50 to "Planta Pirana de fuego",
        // Tanda 6: los Koopa CON caparazón que FALTABAN. El 0x05 ya estaba (por eso era el
        // único que se podía volcar); sus hermanos de color no. Se añaden AL FINAL a
        // propósito: el orden de [curatedIds] fija los fotogramas del atlas horneado, así que
        // meterlos en medio desincronizaría el atlas de quien ya lo tenga generado.
        0x04 to "Koopa verde", 0x06 to "Koopa azul", 0x07 to "Koopa amarillo",
    )

    /** Ids cubiertos, en orden estable (el mismo que el atlas horneado). */
    val curatedIds: List<Int> = NAMES.keys.toList()

    /** ¿Tenemos gráfico curado para este id de sprite? */
    fun handles(spriteId: Int): Boolean = NAMES.containsKey(spriteId)

    /** Nombre legible del enemigo curado, o null. */
    fun nameOf(spriteId: Int): String? = NAMES[spriteId]

    /**
     * Imagen ARGB (16×16) del enemigo [spriteId] tal y como lo dibuja el juego en el
     * [level] dado, con la CGRAM y sub-paleta REALES del nivel; null si el id no está en
     * el catálogo, la ROM no parece SMW vanilla, o faltan datos del nivel. El índice de
     * color 0 queda transparente.
     */
    fun spriteImage(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): ArgbImage? {
        val art = artFor(rom, header, level, spriteId) ?: return null
        val base = TILE_BYTES[OAM_OFFSET[spriteId]] + art.page * 0x100
        val img = ArgbImage(16, 16) // pixels a 0 = transparente
        return if (art.paintBlock(base, img, 0, 0)) img else null
    }

    /**
     * Una tesela OAM de un enemigo con DIBUJO PROPIO: [tile] es el nº de tesela de sprite
     * (0..0x1FF, con el bit de página ya incluido), ([dx],[dy]) su desplazamiento en px
     * respecto al ancla, [size16] si es 16×16 (o 8×8) y [xflip] espejo horizontal.
     */
    class OamTile(val tile: Int, val dx: Int, val dy: Int, val size16: Boolean = true, val xflip: Boolean = false,
                  /** Volteo VERTICAL de la tesela (bit 0x80 de la propiedad OAM). */
                  val vflip: Boolean = false,
                  /** Fila de paleta propia de ESTA tesela (o null = la del sprite/override global). */
                  val palRow: Int? = null)

    /**
     * Compone un sprite de enemigo con DIBUJO PROPIO (fuera de la tabla OAM genérica) a
     * partir de sus teselas OAM REALES (nº de tesela CRUDO 0..0xFF de su rutina
     * `Spr..._Draw`, banco $01/$03), añadiéndoles la PÁGINA del sprite ($166E). La paleta es
     * [palRow] (8+paleta de objeto) o, si null, la del sprite ($166E). Recorta al
     * bounding-box. Reutiliza el GFX de sprites y la CGRAM del nivel. null si faltan datos.
     */
    fun customSprite(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int,
                     tiles: List<OamTile>, palRow: Int? = null, settingOverride: Int? = null): ArgbImage? {
        if (tiles.isEmpty()) return null
        val art = buildSpriteArt(rom, header, level, spriteId, settingOverride) ?: return null
        val row = palRow ?: art.cgRow
        val minX = tiles.minOf { it.dx }
        val minY = tiles.minOf { it.dy }
        val maxX = tiles.maxOf { it.dx + if (it.size16) 16 else 8 }
        val maxY = tiles.maxOf { it.dy + if (it.size16) 16 else 8 }
        val img = ArgbImage(maxX - minX, maxY - minY)
        var any = false
        for (t in tiles) {
            val base = (t.tile and 0xFF) + art.page * 0x100
            if (art.paintTile(base, img, t.dx - minX, t.dy - minY, t.size16, t.xflip, t.palRow ?: row, t.vflip)) any = true
        }
        return if (any) img else null
    }

    /**
     * DEPURACIÓN: vuelca las 512 teselas de la VRAM de sprites (SP1..SP4, ids 0x000..0x1FF)
     * de un nivel en una rejilla de 16 teselas 8×8 por fila (128×256 px), con la paleta
     * [palRow]. Filas 0–15 = SP1, 16–31 = SP2, 32–47 = SP3, 48–63 = SP4. Sirve para localizar
     * a ojo qué gráficos carga de verdad un nivel (p.ej. si un jefe está o no en las ranuras
     * estáticas). null si faltan datos.
     */
    fun spriteVramSheet(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int, palRow: Int,
                        settingOverride: Int? = null): ArgbImage? {
        val art = buildSpriteArt(rom, header, level, spriteId, settingOverride) ?: return null
        val cols = 16
        val img = ArgbImage(cols * 8, (512 / cols) * 8)
        for (t in 0 until 512) {
            art.paintTile(t, img, (t % cols) * 8, (t / cols) * 8, size16 = false, xflip = false, rowOverride = palRow)
        }
        return img
    }

    /**
     * Imagen del enemigo con DIBUJO PROPIO [spriteId] (Rex, Blurp, Super Koopa…), a su
     * tamaño real, o null si no está en el catálogo de dibujos propios [CUSTOM_ENEMIES].
     * Es la vía para los enemigos que la tabla OAM genérica no sabe dibujar (ids ≥ 0x54 o
     * multi-tesela): cada uno lleva su layout de teselas real de su rutina del banco $01/$03.
     */
    fun customEnemyImage(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): ArgbImage? {
        val c = CUSTOM_ENEMIES[spriteId] ?: return null
        return customSprite(rom, header, level, spriteId, c.tiles, c.palRow, c.gfxSetting)
    }

    /**
     * Fotogramas del CAPARAZÓN dentro de la tabla OAM genérica, tal y como los elige el
     * juego: `StunnedShellDraw` ($01:9806) pinta el **6** (quieto) y
     * `kKickedShellGFXRt_ShellAniTiles` = `{6,7,8,7}` es el ciclo del caparazón GIRANDO.
     */
    const val SHELL_FRAME_STILL = 6
    val SHELL_SPIN_FRAMES = intArrayOf(6, 7, 8, 7)

    /**
     * Imagen ARGB (16×16) del CAPARAZÓN de un Koopa, con la paleta REAL del [level].
     * [spriteId] debe ser un Koopa CON caparazón (**0x04-0x07**); [frame] es uno de
     * [SHELL_SPIN_FRAMES] (por defecto el quieto).
     *
     * Port de `StunnedShellGFXRt_01980F` ($01:980F) + `GenericGFXRtDraw1Tile16x16` ($01:9F0D):
     * el caparazón es UNA sola tesela 16×16 cuyo número sale de
     * `Tiles[TilesOffset[spriteId] + frame]`, o sea el mismo mecanismo que cualquier otro
     * fotograma del sprite. La razón de que costara tanto encontrarlo es que se buscaba en
     * los ids 0x00-0x03, que son los Koopa SIN caparazón y por tanto no lo tienen.
     *
     * Devuelve null si el id no lleva caparazón o faltan datos del nivel.
     */
    fun shellImage(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int,
                   frame: Int = SHELL_FRAME_STILL): ArgbImage? {
        if (spriteId !in 0x04..0x07) return null
        val art = artFor(rom, header, level, spriteId) ?: return null
        val idx = OAM_OFFSET.getOrElse(spriteId) { return null } + frame
        if (idx !in TILE_BYTES.indices) return null
        val img = ArgbImage(16, 16)
        return if (art.paintTile(TILE_BYTES[idx] + art.page * 0x100, img, 0, 0,
                size16 = true, xflip = false)) img else null
    }

    /**
     * Los fotogramas del CAPARAZÓN de [spriteId] (un Koopa CON caparazón, 0x04-0x07) en el
     * orden del ciclo de giro [SHELL_SPIN_FRAMES]: el primero es el caparazón QUIETO, y los
     * cuatro juntos son la animación del caparazón deslizándose. null si el id no lleva
     * caparazón o no se pudo pintar ninguno.
     */
    fun shellFrames(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): List<ArgbImage>? {
        if (spriteId !in 0x04..0x07) return null
        val out = SHELL_SPIN_FRAMES.toList().mapNotNull { shellImage(rom, header, level, spriteId, it) }
        return out.ifEmpty { null }
    }

    /** DEPURACIÓN: vuelca los primeros [count] fotogramas de la tabla OAM de [spriteId],
     *  para ver a ojo cuál es cuál (andar, caparazón, aplastado…). */
    fun dumpOamFrames(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int, count: Int): List<ArgbImage>? {
        val art = artFor(rom, header, level, spriteId) ?: return null
        val off = OAM_OFFSET.getOrElse(spriteId) { return null }
        val out = ArrayList<ArgbImage>()
        for (f in 0 until count) {
            val idx = off + f
            if (idx >= TILE_BYTES.size) break
            val img = ArgbImage(16, 16)
            art.paintTile(TILE_BYTES[idx] + art.page * 0x100, img, 0, 0, size16 = true, xflip = false)
            out.add(img)
        }
        return out
    }

    /** Ids con dibujo propio soportados (para el bake de `big_<id>.png`). */
    val customEnemyIds: List<Int> get() = CUSTOM_ENEMIES.keys.toList()

    /** Plataforma ESTRECHA: 3 teselas 16×16 seguidas ($01:B2DF, rama sin 1602). */
    private fun flatPlatformNarrow(): List<OamTile> =
        listOf(OamTile(0x60, 0, 0), OamTile(0x61, 16, 0), OamTile(0x62, 32, 0))

    /** Plataforma ANCHA: 5 teselas 16×16, con el tramo central repetido. */
    private fun flatPlatformWide(): List<OamTile> =
        listOf(
            OamTile(0xEA, 0, 0), OamTile(0xEB, 16, 0), OamTile(0xEB, 32, 0),
            OamTile(0xEB, 48, 0), OamTile(0xEC, 64, 0),
        )

    private class CustomEnemy(val tiles: List<OamTile>, val palRow: Int? = null,
                              /** Ajuste de GFX de sprites FORZADO (salas de jefe Modo 7); null = el del nivel. */
                              val gfxSetting: Int? = null)

    /**
     * Layouts de teselas REALES de enemigos con dibujo propio, transcritos de sus rutinas
     * `Spr..._Draw` del disassembly (snesrev/smw). Un fotograma representativo por enemigo.
     * Teselas en CRUDO (0..0xFF); la página y —salvo override— la paleta salen de $166E.
     */
    private val CUSTOM_ENEMIES: Map<Int, CustomEnemy> = mapOf(
        // Rex (0xAB): 2 teselas 16×16 apiladas (kSpr0AB_Rex_Tiles/XDisp/YDisp, $03), frame 0.
        // Cabeza 0x8a en (−4,−15) + cuerpo 0xaa en (0,0). Paleta 3 (ppp de Prop=0x07:
        // (0x07 >> 1) & 7 = 3) → fila 8+3 de la CGRAM; es la AZUL de Rex, no la rosa.
        // ---- PLATAFORMAS de guía (las que sostienen YOSHI'S ISLAND 3) ----
        // Las cinco (0x55, 0x57, 0x59, 0x5A, 0x5F) comparten rutina de dibujo:
        // NormalSpritePlatformDraw ($01:B2D1) mira la tabla $01:B2C3 —que para todas
        // ellas vale 0— y cae en NormalSpritePlatformGFXRt_DrawFlatPlatform ($01:B2DF).
        //
        // Esa rutina tiene DOS formas según spr_table1602, que se pone en el Init:
        //   - 0 (por defecto): plataforma ESTRECHA de 3 teselas 16×16 → 0x60, 0x61, 0x62.
        //   - 1: plataforma ANCHA de 5 teselas → 0xEA, 0xEB, 0xEB, 0xEB, 0xEC.
        // De 0x57 SÍ consta que su Init hace ++spr_table1602 (Spr057_Vertical
        // CheckerboardPlatform_Init, $01:B25E), así que va ANCHA; las demás se quedan
        // con el valor por defecto y van estrechas.
        0x55 to CustomEnemy(flatPlatformNarrow()),
        0x59 to CustomEnemy(flatPlatformNarrow()),
        0x5A to CustomEnemy(flatPlatformNarrow()),
        0x5F to CustomEnemy(flatPlatformNarrow()),
        0x57 to CustomEnemy(flatPlatformWide()),
        0xAB to CustomEnemy(
            listOf(OamTile(0x8a, -4, -15), OamTile(0xaa, 0, 0)),
            palRow = (8 + 3) * 16,
        ),
        // Blurp (0xC2): 1 tesela 16×16 (Spr0C2_Blurp, $03: charnum 0xA2; paleta de $166E).
        0xC2 to CustomEnemy(listOf(OamTile(0xA2, 0, 0))),
        // Super Koopa suelo (0x73) / capa roja (0x71): frame 0 de ANDAR (SprXXX_SuperKoopas,
        // $02; SprXXX_SuperKoopas_02EBB5 usa frame 0/1 al andar, sin vflip). 4 teselas:
        // cuerpo 0xe0 (16×16, paleta del sprite) + CAPA 0xc8/0xd8/0xd0 (8×8). La capa usa la
        // paleta especial de la fórmula (Prop|v4)&~2: 0x73 (≥0x72, v4=4)→2, 0x71 (v4=8)→4.
        0x73 to CustomEnemy(superKoopaFrame0(capePal = (8 + 2) * 16)),
        0x71 to CustomEnemy(superKoopaFrame0(capePal = (8 + 4) * 16)),
        // PorcuPuffer (0xC3): pez globo 32×32 = 4 teselas 16×16 en (∓8,∓8) (Spr0C3_PorcuPuffer_Draw,
        // $03: PocruPufferTiles/DispX/DispY). Frame 0. Paleta 6 (Prop 0x0D → (0xD>>1)&7).
        0xC3 to CustomEnemy(
            listOf(
                OamTile(0x86, -8, -8), OamTile(0xc0, 8, -8),
                OamTile(0xa6, -8, 8), OamTile(0xc2, 8, 8),
            ),
            palRow = (8 + 6) * 16,
        ),
        // Fishbone (0xAA): CABEZA 16×16 (0xA6, GenericGFXRtDraw1Tile16x16, paleta $166E) + 2
        // teselas de COLA 8×8 (0xa3) detrás (Spr0AA_Fishbone_Draw, $03: TailTiles/XDisp/YDisp).
        0xAA to CustomEnemy(
            listOf(
                OamTile(0xA6, 0, 0, size16 = true),
                OamTile(0xa3, -8, 0, size16 = false),
                OamTile(0xa3, -8, 8, size16 = false),
            ),
        ),
        // Wiggler (0x86): oruga de 5 segmentos 16×16 + flor 8×8 (Spr086_Wiggler, $02:02F035).
        // Cabeza = tesela 0x8c (v7==0 → charnum −116); cuerpo = kSpr086_Wiggler_WigglerTiles
        // [r6] con r6=v7&3 → 0xc6/0xc8/0xc6/0xc4 (segs 1..4). Arco de la oruga por
        // WigglerYDisp={0,1,2,1} (y = 8 − disp). Flor 8×8 (tesela 0x98 = −104) sobre la
        // cabeza (ypos−8), paleta 5 (flags &0xF1|0x0A). Cuerpo/cabeza usan la paleta de $166E.
        0x86 to CustomEnemy(
            listOf(
                OamTile(0x8c, 0, 8, size16 = true),   // cabeza
                OamTile(0xc6, 16, 7, size16 = true),  // seg 1
                OamTile(0xc8, 32, 6, size16 = true),  // seg 2 (joroba)
                OamTile(0xc6, 48, 7, size16 = true),  // seg 3
                OamTile(0xc4, 64, 8, size16 = true),  // seg 4 (cola)
                OamTile(0x98, 4, 0, size16 = false, palRow = (8 + 5) * 16), // flor
            ),
        ),
        // Swooper (0xBE): murciélago, 1 tesela 16×16 (Spr0BE_Swooper, $03: GenericGFXRtDraw1-
        // Tile16x16 + charnum kSpr0BE_Swooper_Tiles[1602]; frame 0 = 0xae; paleta de $166E).
        0xBE to CustomEnemy(listOf(OamTile(0xae, 0, 0))),
        // Reznor (0xA9): dino-jefe de castillo 32×32 = 4 teselas 16×16 (Spr0A9_Reznor_Draw,
        // $03). Frame 0 (r3=r2=0): teselas 0x40/0x42/0x60/0x62; con r2=0 cada tesela lleva
        // h-flip (Prop ^= 0x40). Prop 0x3f → paleta 7. CLAVE: la sala de jefe en Modo 7 IGNORA
        // el GFX del nivel y FUERZA el ajuste 19 ({0,1,0x25,0x22}) en PrepareMode7Level; el GFX
        // real del dino está en el fichero 0x25 (SP3), no en el 0x13 estático del nivel.
        0xA9 to CustomEnemy(
            listOf(
                OamTile(0x42, 0, 0, xflip = true), OamTile(0x40, 16, 0, xflip = true),
                OamTile(0x62, 0, 16, xflip = true), OamTile(0x60, 16, 16, xflip = true),
            ),
            palRow = (8 + 7) * 16,
            gfxSetting = 19,
        ),
        // Big Boo Boss (0xC5): boo gigante ~64×64 = 4×4 teselas 16×16 + boca/colmillos
        // (NormalSpriteBooDraw, $03; kNormalSpriteBooGFXRt_BigBooTiles/XDisp/YDisp/Prop,
        // frame 0). El cuerpo usa v-flip en la mitad inferior (Prop 0x80). Boca (0xc0/0xe0) y
        // colmillos (0xe8) al final para que queden POR ENCIMA. Paleta 7 ($166E).
        0xC5 to CustomEnemy(
            listOf(
                // cuerpo: 4 columnas × 4 filas de 16×16
                OamTile(0x80, 0, 0), OamTile(0xa0, 0, 16), OamTile(0xa0, 0, 32, vflip = true), OamTile(0x80, 0, 48, vflip = true),
                OamTile(0x82, 16, 0), OamTile(0xa2, 16, 16), OamTile(0xa2, 16, 32, vflip = true), OamTile(0x82, 16, 48, vflip = true),
                OamTile(0x84, 32, 0), OamTile(0xa4, 32, 16), OamTile(0xc4, 32, 32), OamTile(0xe4, 32, 48),
                OamTile(0x86, 48, 0), OamTile(0xa6, 48, 16), OamTile(0xc6, 48, 32), OamTile(0xe6, 48, 48),
                // boca y colmillos (encima)
                OamTile(0xc0, 8, 18), OamTile(0xe0, 8, 34),
                OamTile(0xe8, 32, 24, xflip = true), OamTile(0xe8, -3, 24),
            ),
        ),
        // Dino-Rhino (0x6E): dino 32×32 = 4 teselas 16×16 (Spr06F_DinoTorch_Draw rama "no
        // fuego", $03: kSpr06F_DinoTorch_DinoRhinoTiles/XDisp/YDisp/Prop). Frame 0, dir sin
        // flip (r2=1): teselas 0xc0/0xc2/0xe4/0xe6 en (−8,−16)/(8,−16)/(−8,0)/(8,0). Prop
        // 0x2f → paleta 7, página 1.
        0x6E to CustomEnemy(
            listOf(
                OamTile(0xc0, 0, 0), OamTile(0xc2, 16, 0),
                OamTile(0xe4, 0, 16), OamTile(0xe6, 16, 16),
            ),
            palRow = (8 + 7) * 16,
        ),
        // Dino-Torch (0x6F): el dino-fuego que ESCUPE llama (Spr06F_DinoTorch_Draw rama
        // spr_spriteid==111, $03). Cabeza 16×16 = kSpr06F_DinoTorch_DinoTorchTiles[r4] (frame
        // r4=1 → 0xaa) en (0,0), Prop[4]=0xf → paleta 7. La LLAMA horizontal son 4 teselas
        // kSpr06F_DinoTorch_DinoFlameTiles[0..3]={0x80,0x82,0x84,0x86} a XDisp
        // {0xd8,0xe0,0xec,0xf8}={−40,−32,−20,−8} (YDisp 0), con Prop 0x9/0x5/0x5/0x5 →
        // paletas 4/2/2/2 (fila 8+p). r2!=0 (sin flip): la llama sale a la IZQUIERDA.
        0x6F to CustomEnemy(
            listOf(
                OamTile(0xaa, 0, 0, palRow = (8 + 7) * 16),      // cabeza (frame r4=1)
                OamTile(0x86, -8, 0, palRow = (8 + 2) * 16),     // llama (cerca de la boca)
                OamTile(0x84, -20, 0, palRow = (8 + 2) * 16),
                OamTile(0x82, -32, 0, palRow = (8 + 2) * 16),
                OamTile(0x80, -40, 0, palRow = (8 + 4) * 16),    // punta de la llama
            ),
        ),
        // Blargg (0xA8): cabeza de dragón de lava, 5 teselas 16×16 (Spr0A8_Blargg_Draw rama
        // v2==4, $03: kSpr0A8_Blargg_Tiles/XDisp/YDisp). Frame 0 (r3=0), dir sin flip (r2=1):
        // teselas 0xa2/0xa4 (arriba), 0xc2/0xc4/0xa6 (abajo). Prop 0x05 → paleta 2, página 1.
        0xA8 to CustomEnemy(
            listOf(
                OamTile(0xa2, -8, -8), OamTile(0xa4, 8, -8),
                OamTile(0xc2, -8, 8), OamTile(0xc4, 8, 8), OamTile(0xa6, 24, 8),
            ),
            palRow = (8 + 2) * 16,
        ),
    )

    /** Frame 0 de andar de Super Koopa: cuerpo 16×16 (paleta del sprite) + 3 teselas de capa 8×8. */
    private fun superKoopaFrame0(capePal: Int): List<OamTile> = listOf(
        OamTile(0xe0, 0, 0, size16 = true),                       // cuerpo (paleta $166E)
        OamTile(0xc8, 8, 0, size16 = false, palRow = capePal),    // capa
        OamTile(0xd8, 8, 8, size16 = false, palRow = capePal),    // capa
        OamTile(0xd0, 16, 8, size16 = false, palRow = capePal),   // capa
    )

    /** Ids de sprite de los POWERUPS y su tesela (`kPowerUpAndItemGFXRt_PowerUpTiles`, $01). */
    private val POWERUP_SPRITES = intArrayOf(0x74, 0x75, 0x77) // seta, flor de fuego, pluma
    private val POWERUP_TILES = intArrayOf(0x24, 0x26, 0x0E)

    /**
     * Hoja 48×16 de los POWERUPS REALES de SMW —SETA | FLOR | PLUMA, en ese orden— cada uno
     * con su tesela de sprite (`0x24/0x26/0x0E`) y su paleta real (`$166E`). El juego los
     * dibuja fuera de la tabla OAM genérica de enemigos (`PowerUpAndItemDraw`, `$01:C61A`),
     * por eso no salen de [spriteImage]. Índice 0 transparente. null si faltan datos.
     */
    fun powerupSheet(rom: ByteArray, header: SnesHeader, level: Int): ArgbImage? {
        val img = ArgbImage(16 * POWERUP_SPRITES.size, 16)
        var any = false
        for (i in POWERUP_SPRITES.indices) {
            val art = buildSpriteArt(rom, header, level, POWERUP_SPRITES[i]) ?: continue
            val base = POWERUP_TILES[i] + art.page * 0x100
            if (art.paintTile(base, img, i * 16, 0, size16 = true, xflip = false)) any = true
        }
        return if (any) img else null
    }

    /** Nombres de los powerups, en el mismo orden que [POWERUP_SPRITES]. */
    val POWERUP_NAMES = listOf("Seta", "Flor de fuego", "Pluma")

    /**
     * Los POWERUPS por separado (nombre → imagen 16×16 transparente), para el catálogo de
     * extracción: la [powerupSheet] partida en su celda por powerup. Null si no es SMW.
     */
    fun powerupImages(rom: ByteArray, header: SnesHeader, level: Int): List<Pair<String, ArgbImage>>? {
        val sheet = powerupSheet(rom, header, level) ?: return null
        return POWERUP_NAMES.mapIndexed { i, name ->
            val cell = ArgbImage(16, 16)
            for (y in 0 until 16) for (x in 0 until 16) cell.set(x, y, sheet.get(i * 16 + x, y))
            name to cell
        }
    }

    /**
     * FOTOGRAMAS de ANDAR del enemigo [spriteId] como los dibuja el juego, cada uno en
     * una celda UNIFORME de 16×32 anclada por los pies:
     *  - Ids APILADOS ([isTall], Koopas con caparazón): los dos bloques 16×16 (arriba +
     *    abajo) de su entrada, con los 2 fotogramas del ciclo (`offset + 2·fotograma`).
     *  - Resto de andadores genéricos (id ≤ 0x13, p. ej. Goomba, Koopas sin caparazón):
     *    su 16×16 en la MITAD BAJA de la celda, con sus 2 fotogramas (`offset + f`),
     *    exactamente el `+ spr_table1602` de `GenericGFXRtDraw1Tile16x16`.
     *  - Ids fuera de la familia andadora: UN solo fotograma (su rutina de animación es
     *    propia y meter el "siguiente byte" de la tabla pintaría basura).
     * null si el id no está curado o faltan datos. [spriteImage] queda intacta (es la
     * que consume el atlas horneado de 16×16).
     */
    fun spriteFrames(
        rom: ByteArray,
        header: SnesHeader,
        level: Int,
        spriteId: Int,
        frames: Int = genericAnimFrames(spriteId),
    ): List<ArgbImage>? {
        val art = artFor(rom, header, level, spriteId) ?: return null
        val off = OAM_OFFSET[spriteId]
        val tall = isTall(spriteId)
        val animFrames = frames.coerceAtLeast(1)
        val out = ArrayList<ArgbImage>(animFrames)
        for (f in 0 until animFrames) {
            val img = ArgbImage(16, 32)
            val painted = if (tall) {
                val i = off + 2 * f
                if (i + 1 >= TILE_BYTES.size) break
                val top = TILE_BYTES[i] + art.page * 0x100
                val bottom = TILE_BYTES[i + 1] + art.page * 0x100
                // El bob de 1 px del juego (fotograma impar) se omite: el ciclo lo dan
                // los pies del bloque de abajo.
                art.paintBlock(top, img, 0, 0) or art.paintBlock(bottom, img, 0, 16)
            } else {
                // Paso normal 1 (byte siguiente = fotograma siguiente); las Plantas Piraña
                // van de 2 en 2 (su entrada es apilada, pero solo pintamos la BOCA de arriba).
                val stride = if (spriteId in PIRANHAS) 2 else 1
                val i = off + stride * f
                if (i >= TILE_BYTES.size) break
                art.paintBlock(TILE_BYTES[i] + art.page * 0x100, img, 0, 16)
            }
            if (!painted) break
            out.add(img)
        }
        // La Piraña de techo (0x2A) cuelga boca ABAJO: se voltea en vertical, para que se
        // dibuje bien igual en el editor (atlas) que en el juego (fotogramas en vivo).
        val res = if (spriteId == 0x2A) out.map { vflip(it) } else out
        return res.ifEmpty { null }
    }

    /**
     * Plantas Piraña SALTARINAS (0x4F normal, 0x50 escupefuego). A diferencia de la de
     * tubo (solo boca), la saltarina sale con TALLO/HOJAS y su dibujo es de DOS partes
     * (`JumpingPiranhaMain`, banco $02): la BOCA arriba (tesela 16×16, `SubSprGfx2`) y las
     * HOJAS-hélice 8px más abajo (cuatro teselas 8×8, `SubSprGfx0`), con las columnas
     * derechas en espejo. Las aspas giran alternando 0xC4↔0xC5 abajo; la boca alterna
     * 0xAC↔0xAE. Su entrada OAM está en el offset 0x3A (igual que la de tubo).
     */
    private val JUMPING_PIRANHAS = setOf(0x4F, 0x50)

    /** ¿Es una Planta Piraña saltarina (con tallo/hojas)? */
    fun isJumpingPiranha(spriteId: Int): Boolean = spriteId in JUMPING_PIRANHAS

    /**
     * FOTOGRAMAS de la Planta Piraña saltarina: boca (16×16) + hojas-hélice (16×16 de
     * cuatro 8×8, columnas derechas en espejo) 8px por debajo, ancladas por los pies y
     * centradas en la celda [cellW]. Dos fotogramas: boca cerrada/aspas 0xC4 y boca
     * abierta/aspas 0xC5 (el giro de la hélice). null si el id no es saltarina o faltan datos.
     */
    fun jumpingPiranhaFrames(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int, cellW: Int = ATLAS_CELL): List<ArgbImage>? {
        if (!isJumpingPiranha(spriteId)) return null
        val art = artFor(rom, header, level, spriteId) ?: return null
        val off = OAM_OFFSET[spriteId] // 0x3A
        val page = art.page * 0x100
        val bodyX = (cellW - 16) / 2
        val mouthY = 8
        val leafY = mouthY + 8
        val mouthTile = intArrayOf(TILE_BYTES[off + 0], TILE_BYTES[off + 2]) // 0xAC, 0xAE
        val leafTop = TILE_BYTES[off + 4]                                    // 0x83 (hoja superior)
        val leafBot = intArrayOf(TILE_BYTES[off + 6], TILE_BYTES[off + 10])  // 0xC4, 0xC5 (aspas)
        // Las hojas usan la sub-paleta 5 (la parte 2 dibuja con SpriteOBJAttribute=$0A →
        // bits de paleta = 5); las filas de sprite son 8..15 de la CGRAM.
        val leafRow = (8 + 5) * 16
        val out = ArrayList<ArgbImage>(2)
        for (f in 0..1) {
            val img = ArgbImage(cellW, ATLAS_CELL)
            // Boca (16×16) arriba.
            val mouth = art.paintBlock(mouthTile[f] + page, img, bodyX, mouthY)
            // Hojas: fila de arriba 8×8 (izq normal, der en espejo) y aspas debajo igual.
            art.paintTile(leafTop + page, img, bodyX + 0, leafY, false, false, leafRow)
            art.paintTile(leafTop + page, img, bodyX + 8, leafY, false, true, leafRow)
            art.paintTile(leafBot[f] + page, img, bodyX + 0, leafY + 8, false, false, leafRow)
            art.paintTile(leafBot[f] + page, img, bodyX + 8, leafY + 8, false, true, leafRow)
            if (!mouth) break
            out.add(img)
        }
        return out.ifEmpty { null }
    }

    /** Ids de las Koopas ALADAS (Parakoopa): cuerpo CON caparazón + ala (2 fotogramas). */
    private val WINGED_KOOPAS = 0x08..0x0B

    /** ¿Es una Koopa alada (Parakoopa)? */
    fun isWinged(spriteId: Int): Boolean = spriteId in WINGED_KOOPAS

    /**
     * Geometría de la CELDA del atlas de enemigos horneado: cuadrada de [ATLAS_CELL]px
     * (ancha para que quepan las ALAS que sobresalen, alta para el sprite apilado 16×32),
     * con [ATLAS_FRAMES] fotogramas de animación apilados en vertical. El sprite se ancla
     * por los pies (abajo) y CENTRADO en horizontal (margen (ATLAS_CELL-16)/2 a cada lado).
     * El atlas resultante mide `curatedIds.size·ATLAS_CELL × ATLAS_FRAMES·ATLAS_CELL`.
     */
    const val ATLAS_CELL = 32
    const val ATLAS_FRAMES = 2

    /**
     * Ids del catálogo que ANIMAN 2 fotogramas por la vía GENÉRICA: usan `SetAnimationFrame`
     * (alterna `spr_table1602` 0↔1 cada 8 ticks) y un dibujo genérico que suma ese frame al
     * nº de tesela, así el fotograma 1 = el byte SIGUIENTE de su entrada OAM (`off+f`, o
     * `off+2·f` si es apilado). Verificado renderizando ambos fotogramas desde la ROM: solo
     * están los que dan un 2º fotograma REAL (los demás pintarían basura y van a 1). Las
     * Koopas aladas ([WINGED_KOOPAS]) animan aparte con su ala.
     */
    private val ANIMATED_2FRAME = setOf(
        // Andadores genéricos (≤0x13): Koopas con/sin caparazón, Goombas, Buzzy.
        0x00, 0x01, 0x02, 0x03, 0x05, 0x0F, 0x10, 0x11,
        // Verificados renderizando ambos fotogramas desde la ROM (2º fotograma REAL):
        // Cheep-Cheep (aleteo de aleta), Spike Top (giro), Bony Beetle (mandíbula), Boo
        // (se tapa/destapa la cara), Eerie (ondeo), Rip Van Fish (aletas), Topo (andar).
        0x15, 0x16, 0x2E, 0x31, 0x37, 0x38, 0x39, 0x3D, 0x4D, 0x4E,
        // Plantas Piraña de TUBO ([PIRANHAS]): abren/cierran la boca con paso 2 (solo la
        // tesela de boca de cada par apilado). Las SALTARINAS animan aparte
        // ([isJumpingPiranha]), no por esta vía.
        0x1A, 0x2A,
        // NO se animan por esta vía (su 2º byte OAM daría basura; su animación real es de
        // rutina propia, no genérica): Bullet Bill 0x1C, Koopa Kid 0x29, Huevo de Yoshi
        // 0x2C, Lakitu de tubería 0x4B → quedan a 1 fotograma.
    )

    /** Nº de fotogramas que la vía genérica ([spriteFrames]) saca para el id. */
    private fun genericAnimFrames(spriteId: Int): Int = if (spriteId in ANIMATED_2FRAME) 2 else 1

    /** Nº de fotogramas de animación del id en el atlas: 2 si anima (aladas, saltarinas o genéricos), 1 si no. */
    fun animFrameCount(spriteId: Int): Int =
        if (isWinged(spriteId) || isJumpingPiranha(spriteId) || genericAnimFrames(spriteId) > 1) ATLAS_FRAMES else 1

    // Tablas REALES del ala (banco $01, `KoopaWing*`), indexadas por dir*2 + fotograma.
    // Usamos SIEMPRE la dirección 1 (ala a la DERECHA, sin espejo) para hornear el atlas.
    private val WING_DISP_X = intArrayOf(-1, -9, 9, 9)   // KoopaWingDispXLo (con signo)
    private val WING_DISP_Y = intArrayOf(-4, -12, -4, -12) // KoopaWingDispY (con signo)
    private val WING_TILE = intArrayOf(0x5D, 0xC6, 0x5D, 0xC6)
    private val WING_SIZE16 = booleanArrayOf(false, true, false, true) // 0=8×8, 2=16×16
    private val WING_XFLIP = booleanArrayOf(true, true, false, false)   // prop 0x40

    /**
     * FOTOGRAMAS de la Koopa ALADA (Parakoopa, [WINGED_KOOPAS]) como los dibuja el juego:
     * el cuerpo CON caparazón (misma entrada apilada 16×32 que las Koopas con caparazón)
     * MÁS el ala de `KoopaWingGfxRt`, con sus dos fotogramas de aleteo (0x5D plegada 8×8,
     * 0xC6 abierta 16×16) en sus desplazamientos reales. Se pinta en una celda de
     * [cellW]×32 con el cuerpo anclado a la izquierda (x=0) y los pies abajo; el ala
     * sobresale a la derecha (cabe con cellW≈26). Dirección fija = 1 (ala a la derecha).
     * null si el id no es una Koopa alada o faltan datos.
     */
    fun wingedKoopaFrames(
        rom: ByteArray,
        header: SnesHeader,
        level: Int,
        spriteId: Int,
        cellW: Int = ATLAS_CELL,
        bodyX: Int = (ATLAS_CELL - 16) / 2,
    ): List<ArgbImage>? {
        if (!isWinged(spriteId)) return null
        val art = artFor(rom, header, level, spriteId) ?: return null
        // Cuerpo con caparazón = misma entrada OAM que las Koopas con caparazón (offset 0).
        val bodyOff = OAM_OFFSET[spriteId]
        val out = ArrayList<ArgbImage>(2)
        for (f in 0..1) {
            val img = ArgbImage(cellW, ATLAS_CELL)
            val bi = bodyOff + 2 * f
            if (bi + 1 >= TILE_BYTES.size) break
            val top = TILE_BYTES[bi] + art.page * 0x100
            val bottom = TILE_BYTES[bi + 1] + art.page * 0x100
            val body = art.paintBlock(top, img, bodyX, 0) or art.paintBlock(bottom, img, bodyX, 16)
            if (!body) break
            // Ala: dirección 1 (índice 2 + fotograma). Ancla: SpriteX = borde izq. del
            // cuerpo (x=bodyX); SpriteY ≈ techo del bloque inferior (y=16, los pies abajo).
            val wi = 2 + f
            val wx = bodyX + WING_DISP_X[wi]
            val wy = 16 + WING_DISP_Y[wi]
            art.paintTile(WING_TILE[wi], img, wx, wy, WING_SIZE16[wi], WING_XFLIP[wi])
            out.add(img)
        }
        return out.ifEmpty { null }
    }

    /**
     * Hornea el ATLAS de enemigos completo: para cada id de [curatedIds], en el orden
     * estable, una celda [ATLAS_CELL]×[ATLAS_CELL] por fotograma ([ATLAS_FRAMES] apilados
     * en vertical), con el sprite REAL y su color, anclado por los pies y centrado. El
     * aspecto se decide por VOTO entre los niveles que de verdad contienen ese enemigo
     * (algún sub-nivel carga otro sprite-set y daría basura; queda en minoría). Las Koopas
     * aladas usan [wingedKoopaFrames]; el resto [spriteFrames]. Los ids de un solo
     * fotograma repiten su imagen en el segundo (se ven estáticos). El atlas mide
     * `curatedIds.size·ATLAS_CELL × ATLAS_FRAMES·ATLAS_CELL`; devuelve también cuántos
     * ids quedaron sin gráfico.
     */
    fun bakeAtlas(rom: ByteArray, header: SnesHeader): Pair<ArgbImage, Int> {
        val ids = curatedIds
        val levelsWithId = HashMap<Int, MutableList<Int>>()
        for (level in 0 until 0x200) {
            for ((id, _, _) in SnesGameRecipes.smwLevelEnemies(rom, header, level)) {
                levelsWithId.getOrPut(id) { ArrayList() }.add(level)
            }
        }
        val fallbackLevels = listOf(0x106, 0x024, 0x0C7, 0x022, 0x0C5, 0x101, 0x105, 0x001, 0x002)
        val bodyX = (ATLAS_CELL - 16) / 2
        val atlas = ArgbImage(ids.size * ATLAS_CELL, ATLAS_FRAMES * ATLAS_CELL)
        var missing = 0
        ids.forEachIndexed { idx, id ->
            val candidates = (levelsWithId[id] ?: fallbackLevels).take(16)
            // Vota el nivel por el aspecto del fotograma 0; ambos fotogramas salen del mismo.
            val variants = LinkedHashMap<Int, Pair<List<ArgbImage>, MutableList<Int>>>()
            for (l in candidates) {
                val frames = when {
                    isWinged(id) -> wingedKoopaFrames(rom, header, l, id)
                    isJumpingPiranha(id) -> jumpingPiranhaFrames(rom, header, l, id)
                    // spriteFrames ya voltea la Piraña de techo (0x2A) boca abajo.
                    else -> spriteFrames(rom, header, l, id)?.map { padded(it, bodyX) }
                }
                val f0 = frames?.firstOrNull() ?: continue
                variants.getOrPut(f0.pixels.contentHashCode()) { frames to ArrayList() }.second.add(l)
            }
            val winner = variants.values.maxByOrNull { it.second.size }
            if (winner == null) { missing++; return@forEachIndexed }
            val frames = winner.first
            for (f in 0 until ATLAS_FRAMES) {
                val src = frames[f % frames.size] // 1 fotograma → se repite (estático)
                val cx = idx * ATLAS_CELL
                val cy = f * ATLAS_CELL
                for (y in 0 until ATLAS_CELL) for (x in 0 until ATLAS_CELL) {
                    if (x < src.width && y < src.height) atlas.set(cx + x, cy + y, src.pixels[y * src.width + x])
                }
            }
        }
        return atlas to missing
    }

    /** Voltea una imagen en VERTICAL (para la Piraña de techo, que cuelga boca abajo). */
    private fun vflip(src: ArgbImage): ArgbImage {
        val out = ArgbImage(src.width, src.height)
        for (y in 0 until src.height) for (x in 0 until src.width) {
            out.set(x, src.height - 1 - y, src.pixels[y * src.width + x])
        }
        return out
    }

    /** Coloca una imagen 16×[ATLAS_CELL] en una celda cuadrada, centrada en [bodyX]. */
    private fun padded(src: ArgbImage, bodyX: Int): ArgbImage {
        if (src.width == ATLAS_CELL && src.height == ATLAS_CELL) return src
        val out = ArgbImage(ATLAS_CELL, ATLAS_CELL)
        // Ancla por los pies: si la fuente es más baja que la celda, va a la parte de abajo.
        val oy = ATLAS_CELL - src.height
        for (y in 0 until src.height) for (x in 0 until src.width) {
            val dx = bodyX + x; val dy = oy + y
            if (dx in 0 until ATLAS_CELL && dy in 0 until ATLAS_CELL) out.set(dx, dy, src.pixels[y * src.width + x])
        }
        return out
    }

    /**
     * "Pintor" de bloques 16×16 de un nivel: los 4 ficheros GFX de sprites del nivel,
     * su CGRAM ensamblada y la sub-paleta/página del sprite (nibble bajo de $166E).
     * Es el mismo montaje verificado de [spriteImage], factorizado para reutilizarlo.
     */
    private class LevelSpriteArt(
        private val spData: Array<ByteArray?>,
        private val cgram: IntArray,
        val cgRow: Int,
        val page: Int,
    ) {
        private fun tileIndices(tile9: Int): IntArray? {
            val slot = tile9 / TILES_PER_FILE
            if (slot !in 0..3) return null
            val data = spData[slot] ?: return null
            val local = tile9 % TILES_PER_FILE
            val off = local * FORMAT.bytesPerTile
            if (off + FORMAT.bytesPerTile > data.size) return null
            return SnesDecoder.decodeTile(data, off, FORMAT, local).pixelIndices
        }

        /** Pinta el bloque 16×16 con esquina en el nº de tesela [base] en (ox,oy) de [img]. */
        fun paintBlock(base: Int, img: ArgbImage, ox: Int, oy: Int): Boolean {
            var painted = false
            // 16×16 = 4 teselas de 8×8: N, N+1 (arriba); N+0x10, N+0x11 (abajo).
            val sub = arrayOf(intArrayOf(0, 0, 0), intArrayOf(1, 8, 0), intArrayOf(0x10, 0, 8), intArrayOf(0x11, 8, 8))
            for (so in sub) {
                val px = tileIndices(base + so[0]) ?: continue
                for (y in 0..7) for (x in 0..7) {
                    val ci = px[y * 8 + x]
                    if (ci == 0) continue
                    val dx = ox + so[1] + x
                    val dy = oy + so[2] + y
                    if (dx >= img.width || dy >= img.height) continue
                    img.set(dx, dy, cgram[cgRow + (ci and 0x0F)])
                    painted = true
                }
            }
            return painted
        }

        /**
         * Pinta una tesela OAM en (ox,oy) con la paleta [rowOverride] (o la del sprite si
         * null), con espejo horizontal [xflip] y vertical [vflip] opcionales. [size16] elige
         * 16×16 (2×2 teselas, como [paintBlock]) o 8×8 (una tesela). Recorta a los límites de
         * [img] y omite el índice de color 0 (transparente).
         */
        fun paintTile(base: Int, img: ArgbImage, ox: Int, oy: Int, size16: Boolean, xflip: Boolean,
                      rowOverride: Int? = null, vflip: Boolean = false): Boolean {
            val row = rowOverride ?: cgRow
            val sub = if (size16)
                arrayOf(intArrayOf(0, 0, 0), intArrayOf(1, 8, 0), intArrayOf(0x10, 0, 8), intArrayOf(0x11, 8, 8))
            else arrayOf(intArrayOf(0, 0, 0))
            val w = if (size16) 16 else 8
            val h = w
            var painted = false
            for (so in sub) {
                val px = tileIndices(base + so[0]) ?: continue
                for (y in 0..7) for (x in 0..7) {
                    val ci = px[y * 8 + x]
                    if (ci == 0) continue
                    val lx = so[1] + x
                    val ly = so[2] + y
                    val dx = ox + if (xflip) (w - 1 - lx) else lx
                    val dy = oy + if (vflip) (h - 1 - ly) else ly
                    if (dx < 0 || dy < 0 || dx >= img.width || dy >= img.height) continue
                    img.set(dx, dy, cgram[row + (ci and 0x0F)])
                    painted = true
                }
            }
            return painted
        }
    }

    /** Prepara el [LevelSpriteArt] del nivel para [spriteId], o null si faltan datos. */
    private fun artFor(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): LevelSpriteArt? {
        if (!NAMES.containsKey(spriteId)) return null
        if (spriteId !in OAM_OFFSET.indices) return null
        return buildSpriteArt(rom, header, level, spriteId)
    }

    /**
     * Como [artFor] pero SIN la guarda del catálogo de enemigos: prepara la paleta/página
     * ($166E) y el GFX de sprites del nivel para CUALQUIER id de sprite. Lo usan los
     * powerups ([powerupSheet]), que se dibujan fuera de la tabla OAM genérica de enemigos.
     */
    private fun buildSpriteArt(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int,
                               settingOverride: Int? = null): LevelSpriteArt? {
        val delta = header.headerOffset - 0x7FC0

        val behaviors = SmwSpriteBehaviorReader.read(rom, header) ?: return null
        if (spriteId !in behaviors.indices) return null
        val prop = behaviors[spriteId].b166e and 0x0F
        val page = prop and 0x01                        // bit 9 del nº de tesela
        val objPalette = (prop shr 1) and 0x07
        val cgRow = (8 + objPalette) * 16               // filas 8-15 = sprites

        val info = SnesGameRecipes.smwLevelInfo(rom, header, level) ?: return null
        // Las salas de jefe en Modo 7 (Reznor, etc.) IGNORAN el ajuste de GFX de la cabecera
        // y lo FUERZAN en GameMode12_PrepareLevel_PrepareMode7Level ($00:97BC). Por eso los
        // jefes aceptan un [settingOverride] con el ajuste real (p.ej. 19 = ficheros
        // {0,1,0x25,0x22} para Reznor), en vez del ajuste estático del nivel.
        val setting = settingOverride ?: (info.spriteGfx and 0x0F)
        val slotBase = SnesGameRecipes.SMW_SPRITE_GFX_TABLE_PC + delta + 4 * setting
        if (slotBase < 0 || slotBase + 4 > rom.size) return null
        val spData = arrayOfNulls<ByteArray>(4)
        for (s in 0..3) {
            val file = rom[slotBase + s].toInt() and 0xFF
            if (file == SnesGameRecipes.SMW_SLOT_EMPTY) continue
            spData[s] = SnesGameRecipes.smwGfxFileData(rom, file)
        }

        val cgram = SnesGameRecipes.assembleSmwCgram(rom, delta, level)
        return LevelSpriteArt(spData, cgram, cgRow, page)
    }
}
