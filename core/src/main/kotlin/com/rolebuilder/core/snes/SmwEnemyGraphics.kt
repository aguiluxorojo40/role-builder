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
        0x00 to "Koopa verde", 0x01 to "Koopa rojo", 0x02 to "Koopa azul", 0x03 to "Koopa amarillo",
        0x05 to "Koopa", 0x0F to "Goomba", 0x10 to "Goomba volador", 0x11 to "Buzzy Beetle",
        0x1C to "Bullet Bill", 0x29 to "Koopa Kid", 0x2A to "Planta Pirana",
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
        return out.ifEmpty { null }
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
        private val cgRow: Int,
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
         * null), con espejo horizontal opcional [xflip]. [size16] elige 16×16 (2×2 teselas,
         * como [paintBlock]) o 8×8 (una tesela). Recorta a los límites de [img] y omite el
         * índice de color 0 (transparente). Sirve para las ALAS de las Koopas.
         */
        fun paintTile(base: Int, img: ArgbImage, ox: Int, oy: Int, size16: Boolean, xflip: Boolean, rowOverride: Int? = null): Boolean {
            val row = rowOverride ?: cgRow
            val sub = if (size16)
                arrayOf(intArrayOf(0, 0, 0), intArrayOf(1, 8, 0), intArrayOf(0x10, 0, 8), intArrayOf(0x11, 8, 8))
            else arrayOf(intArrayOf(0, 0, 0))
            val w = if (size16) 16 else 8
            var painted = false
            for (so in sub) {
                val px = tileIndices(base + so[0]) ?: continue
                for (y in 0..7) for (x in 0..7) {
                    val ci = px[y * 8 + x]
                    if (ci == 0) continue
                    val lx = so[1] + x
                    val dx = ox + if (xflip) (w - 1 - lx) else lx
                    val dy = oy + so[2] + y
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
        return LevelSpriteArt(spData, cgram, cgRow, page)
    }
}
