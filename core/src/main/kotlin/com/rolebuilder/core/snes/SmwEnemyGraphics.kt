package com.rolebuilder.core.snes

import com.rolebuilder.core.snes.compression.LcLz2

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
        // Tanda 7: el P-SWITCH (0x3E). Sale en 38 niveles —de los ids sin cubrir es el que
        // mas niveles toca— y su entrada de la tabla generica SI es su aspecto real: el
        // bloque amarillo con la "P". Verificado cribando con genericImageUnchecked, que
        // tambien dejo claro que el Podoboo (0x33) y el trampolin (0x2F) NO valen por esa
        // via: dan teselas de fuente, porque llevan rutina de dibujo propia.
        0x3E to "P-Switch",
        // Tanda 8: el TRAMPOLIN (0x2F), 21 niveles. Se dibuja como CUADRADO de cuatro teselas
        // de 8x8 (GenericGFXRtDraw4Tiles8x8Square, $01:9CF5), no como bloque de 16x16, y por
        // eso con la via normal parecia basura: se estaba mirando mal. Con su rutina real
        // salen las barras verde/blancas y el muelle.
        0x2F to "Trampolin",
        // Tanda 9: los otros dos que el juego dibuja como cuadrado de cuatro teselas y que se
        // han visto correctos al renderizarlos: la bola de pinchos y el Thwimp.
        0x14 to "Huevo de Spiny", 0x27 to "Thwimp",
        // Tanda 10: el PODOBOO (0x33), el mayor hueco de la ROM entera —81 colocaciones en 15
        // niveles—. Estaba descartado por "graficos dinamicos", y era verdad a medias: sus
        // teselas NO estan en el tileset del nivel, pero SI se pueden reconstruir, porque el
        // buffer del que sale su DMA es GFX33 descomprimida. Ver [DYNAMIC_GFX]. Se dibuja como
        // cuadrado de cuatro teselas 8x8 igual que el trampolin ([SQUARE_SPRITES]).
        0x33 to "Podoboo",
        // Y el SPINY (0x13), 40 colocaciones. No hacia falta portar nada: entra por
        // SprXXX_Generic_SpinyEntry -> SprXXX_Generic_Spr0to13Gfx ($01:8BC3), que con
        // Spr0to13Prop[0x13] = 0x00 cae en GenericGFXRtDraw1Tile16x16 — o sea, la tabla
        // generica de siempre, la misma por la que ya salian el Goomba y el Buzzy. Solo
        // estaba sin catalogar. Verificado mirando el PNG en los cuatro niveles donde esta
        // puesto (001, 01C, 121, 136): el mismo bicho de pinchos en los cuatro.
        //
        // OJO con el nombre: [SmwSpriteNames] lo llama "KoopaKidBossFight", que no cuadra con
        // el juego — `Spr014_SpinyEgg` ($01:8C18) al tocar el suelo hace `spr_spriteid = 19`,
        // o sea que el huevo de Spiny se convierte EN ESTE id. Es el Spiny.
        0x13 to "Spiny",
        // Tanda 11: los dos CHEEP-CHEEP que faltaban. Los cuatro de la familia (0x15, 0x16,
        // 0x17 y 0x18) comparten la MISMA entrada de la tabla genérica —`TilesOffset` vale
        // 0x8A para los cuatro—, y el 0x47 también; por eso los cinco se dibujan igual y por
        // eso, estando ya el 0x15/0x16, estos dos eran gratis. Verificado mirándolos.
        //
        //  · 0x18 pasa por `SprXXX_FixedMovementCheepCheep_01B10A` ($01:B10A), el mismo
        //    ayudante que el 0x15/0x16. Ese ayudante hace algo que conviene tener anotado:
        //        spr_table15f6 = ((spr_table1602 >> 1) ^ 1) | (spr_table15f6 & 0xFE)
        //    o sea que FUERZA el bit de página en vez de heredarlo. Con el pez nadando
        //    (`spr_table1602` = 0 ó 1) el `>>1` da 0 y el `^1` lo deja en **1**. En la ROM
        //    vanilla el $166E de estos ids ya vale página 1, así que la vía genérica —que la
        //    saca de $166E— coincide; si alguna vez no coincidiera, manda la rutina.
        //    (Fuera del agua el pez colea: `spr_table1602 += 2` y entonces la página pasa a 0
        //    y el gráfico es otro. Ese estado no se guarda.)
        //  · 0x47 (`Spr047_SwimmingAndJumpingCheepCheep`, $02:E727) llama directamente a
        //    `GenericGFXRtDraw1Tile16x16` sin tocar nada, y su
        //    `SprXXX_SuperKoopas_02EB3D(k, 0)` ($02:EB3D) le deja `spr_table1602` en 0/1: el
        //    mismo par de fotogramas que los demás.
        0x18 to "Cheep-Cheep saltarin", 0x47 to "Cheep-Cheep nadador",
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
                  val palRow: Int? = null,
                  /**
                   * PÁGINA de tesela propia de ESTA tesela (0 o 1), o null = la del nivel ($166E).
                   *
                   * Hace falta porque no todos los enemigos heredan la propiedad OAM del nivel:
                   * los que ESCRIBEN su `flags` a mano (`oam[64].flags = sprites_tile_priority | 3`
                   * y compañía) están fijando ahí las dos cosas a la vez — el bit 0 es el noveno
                   * bit del nº de tesela y los bits 1-3 la paleta. Quedarse solo con la paleta y
                   * sacar la página de $166E pinta el gráfico de OTRA mitad de la VRAM de sprites,
                   * que es basura con forma de tesela.
                   */
                  val page: Int? = null)

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
            val base = (t.tile and 0xFF) + (t.page ?: art.page) * 0x100
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
        val variante = if (c.porAjuste.isEmpty()) null else {
            val ajuste = c.gfxSetting
                ?: SnesGameRecipes.smwLevelInfo(rom, header, level)?.spriteGfx?.and(0x0F)
            c.porAjuste[ajuste]
        }
        return customSprite(
            rom, header, level, spriteId,
            variante?.first ?: c.tiles, variante?.second ?: c.palRow, c.gfxSetting,
        )
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

    /**
     * DEPURACIÓN: pinta [spriteId] con las DOS teselas de su entrada apiladas (arriba
     * `TILE_BYTES[off]`, abajo `TILE_BYTES[off+1]`), se considere alto o no.
     *
     * Existe para poder responder MIRANDO a "¿quién lleva caparazón?": los ids que el juego
     * dibuja con una sola tesela se pintan normalmente a media altura, así que si su
     * caparazón viviera en la tesela de arriba no se vería, y se concluiría en falso que no
     * tienen. Forzando el par, la comparación entre ids es justa.
     */
    fun stackedPairImage(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int): ArgbImage? {
        val art = artFor(rom, header, level, spriteId) ?: return null
        val off = OAM_OFFSET.getOrElse(spriteId) { return null }
        if (off + 1 >= TILE_BYTES.size) return null
        val img = ArgbImage(16, 32)
        val arriba = art.paintBlock(TILE_BYTES[off] + art.page * 0x100, img, 0, 0)
        val abajo = art.paintBlock(TILE_BYTES[off + 1] + art.page * 0x100, img, 0, 16)
        return if (arriba || abajo) img else null
    }

    /**
     * Desplazamientos de las CUATRO teselas de 8×8 de un sprite "cuadrado"
     * (`kGenericSpriteOAMData_XDisp` / `_YDisp`, $01): forman un 16×16 con dos arriba y dos
     * abajo.
     */
    private val SQUARE_XDISP = intArrayOf(0, 8, 0, 8)
    private val SQUARE_YDISP = intArrayOf(0, 0, 8, 8)

    /**
     * `kGenericSpriteOAMData_Prop` (24 bytes): propiedades por tesela, en filas de 4. La rutina
     * indexa `Prop[tesela + 4 * fila]`, y la FILA la elige cada sprite (el trampolín usa la 2,
     * el Podoboo la 1). Los bits 0x40/0x80 son volteo horizontal/vertical, que es como el juego
     * compone simetrías reutilizando la misma tesela.
     */
    private val SQUARE_PROP = intArrayOf(
        0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x40,
        0x00, 0x40, 0x80, 0xC0, 0x40, 0x40, 0x00, 0x00,
        0x40, 0x00, 0xC0, 0x80, 0x40, 0x40, 0x40, 0x40,
    )

    /**
     * Sprites que el juego dibuja como CUADRADO DE CUATRO TESELAS 8×8
     * (`GenericGFXRtDraw4Tiles8x8Square`, $01:9CF5), con la fila de [SQUARE_PROP] que usa cada
     * uno. No caben en la vía normal —que pinta UN bloque de 16×16 por byte— y por eso salían
     * como basura al mirarlos con ella: sus cuatro teselas son cuatro bytes SEGUIDOS de la
     * misma tabla plana, no uno.
     */
    private val SQUARE_SPRITES = mapOf(
        0x14 to 2, // Spr014_SpinyEgg            -> Draw4Tiles8x8Square(k, 2)
        0x27 to 1, // Spr027_Thwimp              -> Draw4Tiles8x8Square(k, 1)
        0x2F to 2, // Spr02F_PortableSpringboard -> Entry1(k, 2, ...)
        0x33 to 1, // Spr033_Podoboo             -> Draw4Tiles8x8Square(k, 1)  ($01:E093)
        //
        // El Podoboo es de CUATRO teselas de 8x8 como los de arriba, pero ademas sus teselas
        // no salen del tileset del nivel sino del DMA por fotograma: ver [DYNAMIC_GFX]. Con
        // la fila 1 de [SQUARE_PROP] = {0x00, 0x40, 0x00, 0x40} el juego dibuja la MITAD
        // izquierda de la bola y la refleja: 06 / 06 en espejo / 16 / 16 en espejo.
        //
        // NO se meten aqui, aunque tambien llamen a esa rutina:
        //  · Goomba en PARACAIDAS (0x3F/0x40): su fila de propiedades sale de una tabla POR
        //    FOTOGRAMA (kSprXXX_ParachutingEnemy_DATA_01D5B0 = {1,5,0}), y con la del
        //    fotograma 0 salen fragmentos sueltos, no el bicho.
        //  · Topo de cornisa (0x4D/0x4E): su dibujo de cuadrado es solo el ESTADO "asomando"
        //    (SprXXX_SmallMontyMole_State01_AboutToEmerge), no su aspecto normal. Ademas ya
        //    estan curados y se dibujan bien por la via de siempre, asi que meterlos aqui
        //    seria ROMPER dos enemigos que funcionan.
    )

    /**
     * De los sprites de [SQUARE_SPRITES], los que además ANIMAN por esta vía: el fotograma
     * suma **4** al índice base (`TilesOffset + 4·spr_table1602`), no 1, porque cada
     * fotograma son cuatro teselas seguidas.
     *
     * Solo está el Podoboo, y verificado mirando los dos: el 0 es la llama con el cuerpo
     * AMARILLO y el 1 el mismo dibujo con el cuerpo ROJO — el parpadeo del fuego, que en el
     * juego alterna cada pocos ticks (`SetNormalSpriteAnimationFrame` pone `spr_table1602`
     * a 0/1 mientras SUBE; al caer le suma 2 y pasa a los fotogramas 2/3, que son las mismas
     * teselas al revés).
     *
     * Los otros tres (huevo de Spiny, Thwimp, trampolín) se quedan a 1 a propósito: su
     * `spr_table1602` no es un ciclo de andar (el del trampolín es el REBOTE y el del huevo
     * el giro, que no se puede leer con dos cuadros sueltos).
     */
    private val SQUARE_ANIMATED = setOf(0x33)

    /**
     * Imagen 16×16 de un sprite dibujado como cuadrado de cuatro teselas de 8×8. Port de
     * `GenericGFXRtDraw4Tiles8x8Square_Entry1` ($01:9CF5):
     *
     *     base    = TilesOffset[spriteId] + 4·fotograma
     *     tesela  = Tiles[i + base]        (i = 0..3)
     *     posición= XDisp[i], YDisp[i]
     *     props   = Prop[i + 4·fila]       (volteos incluidos)
     *
     * Null si el id no se dibuja así o faltan datos.
     */
    fun squareTileImage(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int,
                        frame: Int = 0): ArgbImage? {
        val fila = SQUARE_SPRITES[spriteId] ?: return null
        if (spriteId !in OAM_OFFSET.indices) return null
        val art = buildSpriteArt(rom, header, level, spriteId) ?: return null
        val base = OAM_OFFSET[spriteId] + 4 * frame
        val img = ArgbImage(16, 16)
        var pintado = false
        for (i in 0 until 4) {
            val idx = base + i
            if (idx >= TILE_BYTES.size) continue
            val prop = SQUARE_PROP.getOrElse(i + 4 * fila) { 0 }
            val ok = art.paintTile(
                TILE_BYTES[idx] + art.page * 0x100, img, SQUARE_XDISP[i], SQUARE_YDISP[i],
                size16 = false, xflip = (prop and 0x40) != 0, vflip = (prop and 0x80) != 0,
            )
            if (ok) pintado = true
        }
        return if (pintado) img else null
    }

    /**
     * INSPECCIÓN: dibuja [spriteId] con la tabla OAM genérica **saltándose la guarda del
     * catálogo curado**, que es la que hace que [spriteFrames] devuelva null para los ids no
     * curados.
     *
     * Sirve para CRIBAR: la auditoría dice que más de la mitad de los sprites colocados en el
     * juego se quedan sin gráfico, y no porque no exista, sino porque su id no está curado y
     * ni se intenta. Con esto se puede mirar cuáles saldrían bien y curarlos, y cuáles dan
     * basura porque tienen rutina de dibujo propia. NO usar para pintar en el juego: la
     * guarda existe justamente porque para muchos ids esta tabla no es su aspecto real.
     */
    fun genericImageUnchecked(rom: ByteArray, header: SnesHeader, level: Int, spriteId: Int,
                              tall: Boolean = isTall(spriteId)): ArgbImage? {
        if (spriteId !in OAM_OFFSET.indices) return null
        val art = buildSpriteArt(rom, header, level, spriteId) ?: return null
        val off = OAM_OFFSET[spriteId]
        if (off >= TILE_BYTES.size) return null
        val img = ArgbImage(16, 32)
        val pintado = if (tall && off + 1 < TILE_BYTES.size) {
            art.paintBlock(TILE_BYTES[off] + art.page * 0x100, img, 0, 0) or
                art.paintBlock(TILE_BYTES[off + 1] + art.page * 0x100, img, 0, 16)
        } else {
            art.paintBlock(TILE_BYTES[off] + art.page * 0x100, img, 0, 16)
        }
        return if (pintado) img else null
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

    /**
     * Ids con dibujo propio soportados (para el bake de `big_<id>.png`).
     *
     * ⚠ AÑADIR O QUITAR UNO OBLIGA A TOCAR TRES SITIOS MÁS, y esto está escrito aquí porque
     * los tres viven en `:app`, que **no se puede compilar sin el SDK de Android**: en un
     * entorno sin él el módulo ni siquiera entra en el proyecto de Gradle, así que el fallo no
     * aparece hasta CI. Ya pasó: tres commits seguidos en rojo por no actualizarlos.
     *
     *  - `SmwAssetStore.BAKE_VERSION`, o el almacén de quien ya horneó no se rehace y el
     *    sprite nuevo se queda sin fichero.
     *  - los DOS recuentos de `SmwAssetStoreTest` (`curatedIds.size` y `customEnemyIds.size`),
     *    que son la red que avisa de justo eso.
     */
    val customEnemyIds: List<Int> get() = CUSTOM_ENEMIES.keys.toList()

    /**
     * Teselas del dibujo propio de [spriteId] para un nivel con ese [ajuste] de GFX de
     * sprites, o null si no está en el catálogo. Es la MISMA elección que hace
     * [customEnemyImage] antes de pintar, expuesta sin ROM para poder fijarla en tests: qué
     * teselas se eligen es una decisión del port, y sin ROM no hay otra forma de comprobarla.
     */
    fun customEnemyTilesForTest(spriteId: Int, ajuste: Int): List<OamTile>? {
        val c = CUSTOM_ENEMIES[spriteId] ?: return null
        return c.porAjuste[c.gfxSetting ?: ajuste]?.first ?: c.tiles
    }

    /**
     * Plataforma ANCHA: 5 teselas 16×16, con el tramo central repetido.
     *
     * No hay hermana estrecha a propósito. `NormalSpritePlatformGFXRt_DrawFlatPlatform`
     * ($01:B2DF) tiene esa otra rama (3 teselas, 0x60/0x61/0x62, cuando `spr_table1602` es
     * 0), pero NINGUNO de los sprites del catálogo llega a ella: los dos que usan esta
     * rutina —0x55 y 0x57— comparten el mismo Init, que hace `++spr_table1602`. Las teselas
     * 0x60-0x62 son las que salían como barra gris cuando se horneaban por la rutina
     * equivocada; si algún día hace falta la rama estrecha, va con su sprite, no suelta.
     */
    private fun flatPlatformWide(): List<OamTile> =
        listOf(
            OamTile(0xEA, 0, 0), OamTile(0xEB, 16, 0), OamTile(0xEB, 32, 0),
            OamTile(0xEB, 48, 0), OamTile(0xEC, 64, 0),
        )

    private class CustomEnemy(val tiles: List<OamTile>, val palRow: Int? = null,
                              /** Ajuste de GFX de sprites FORZADO (salas de jefe Modo 7); null = el del nivel. */
                              val gfxSetting: Int? = null,
                              /**
                               * Variantes por AJUSTE DE GFX DE SPRITES del nivel. Unos pocos enemigos no
                               * se dibujan siempre igual: su rutina MIRA qué banco de sprites ha cargado
                               * el nivel y cambia de teselas, porque el dibujo que les toca no está en el
                               * mismo sitio en los dos bancos. Sin esto se pinta la tesela del otro banco,
                               * que no es "parecida": es OTRO gráfico entero.
                               */
                              val porAjuste: Map<Int, Pair<List<OamTile>, Int?>> = emptyMap())

    /**
     * Layouts de teselas REALES de enemigos con dibujo propio, transcritos de sus rutinas
     * `Spr..._Draw` del disassembly (snesrev/smw). Un fotograma representativo por enemigo.
     * Teselas en CRUDO (0..0xFF); la página y —salvo override— la paleta salen de $166E.
     */
    private val CUSTOM_ENEMIES: Map<Int, CustomEnemy> = mapOf(
        // Rex (0xAB): 2 teselas 16×16 apiladas (kSpr0AB_Rex_Tiles/XDisp/YDisp, $03), frame 0.
        // Cabeza 0x8a en (−4,−15) + cuerpo 0xaa en (0,0). Paleta 3 (ppp de Prop=0x07:
        // (0x07 >> 1) & 7 = 3) → fila 8+3 de la CGRAM; es la AZUL de Rex, no la rosa.
        // ---- PLATAFORMAS ----
        // ⚠ AQUÍ HABÍA TRES MAL. Se dio por hecho que 0x55, 0x57, 0x59, 0x5A y 0x5F
        // compartían `NormalSpritePlatformDraw` ($01:B2D1) y todas salían plataforma plana.
        // Solo es verdad de DOS. El despacho (`kSprStatus08SpriteNormalPtrs`, banco $01)
        // manda cada id a una rutina distinta, y hay que mirarlo id a id:
        //
        //   0x55, 0x57 -> Spr058_VerticalRockPlatform ($01:B26C) -> NormalSpritePlatformDraw
        //   0x59, 0x5A -> SprXXX_TurnBlockBridge_*Entry ($01:B6A5 / $01:B6DA)  ← OTRA rutina
        //   0x5F       -> Spr05F_BrownChainedPlatform ($01:C773)                ← OTRA rutina
        //
        // Y solo para los dos primeros vale la tabla $01:B2C3 (indexada por `id − 0x55`),
        // que elige entre plana y diagonal. Los otros tres salían como una barra gris que no
        // es de ningún sprite: son las teselas 0x60-0x62 del banco de ESE nivel, leídas por
        // la rutina equivocada.
        //
        // NormalSpritePlatformGFXRt_DrawFlatPlatform ($01:B2DF) tiene DOS formas según
        // spr_table1602, que se pone en el Init:
        //   - 0 (por defecto): plataforma ESTRECHA de 3 teselas 16×16 → 0x60, 0x61, 0x62.
        //   - 1: plataforma ANCHA de 5 teselas → 0xEA, 0xEB, 0xEB, 0xEB, 0xEC.
        // Y los DOS van anchos, aunque el nombre de la rutina solo mencione al 0x57: en la
        // tabla de Init del banco $01 (`kUnk_1817d`, indexada por id de sprite) las entradas
        // 0x55 y 0x57 apuntan a la MISMA, `Spr057_VerticalCheckerboardPlatform_Init`
        // ($01:B25E), que es un `++spr_table1602` y nada más. Comprobado con anclas de la
        // propia tabla (0x33 Podoboo, 0x35 Yoshi, 0x3E P-Switch, 0x54 ClimbingNetDoor caen
        // en su índice exacto). Los dos heredan página y paleta de $166E
        // (`flags = spr_table15f6 | prio`).
        0x55 to CustomEnemy(flatPlatformWide()),
        0x57 to CustomEnemy(flatPlatformWide()),
        // PUENTE DE BLOQUES GIRATORIOS (0x59 el que va en las dos direcciones, 0x5A solo
        // horizontal), `SprXXX_TurnBlockBridge_Draw` ($01:B710): CINCO teselas 16×16 y las
        // cinco son la MISMA, la 64 = 0x40, que es el bloque giratorio amarillo con sus dos
        // puntos. Nada de barra gris.
        //
        // El puente se ESTIRA desde el centro: `spr_table151c` (0..BlkBridgeLength = 0x20) da
        // la separación, y las cinco entradas van a ∓151c y ∓151c/2 alrededor del centro. Se
        // guarda extendido del todo, que es la pose con la que se ve y por la que se anda.
        //
        // Y ojo con la propiedad: `flags = sprites_tile_priority` A SECAS, sin `| 15f6`. Eso
        // deja el bit 0 (la página) a CERO y los bits de paleta también, así que este es de
        // los pocos que NO heredan de $166E y van a página 0 / paleta 0. (La entrada 0 lleva
        // además `| 0x60`, que es prioridad + volteo horizontal, invisible en una tesela
        // simétrica como el bloque giratorio.)
        0x59 to CustomEnemy((0..4).map { OamTile(0x40, it * 16, 0, page = 0) }, palRow = (8 + 0) * 16),
        0x5A to CustomEnemy((0..4).map { OamTile(0x40, it * 16, 0, page = 0) }, palRow = (8 + 0) * 16),
        // PLATAFORMA MARRÓN DE CADENA (0x5F), `Spr05F_BrownChainedPlatform` ($01:C773). Se
        // parecía a la plana lo justo para colarse, pero no lo es:
        //   - son CUATRO teselas, no tres: kSpr05F_BrownChainedPlatform_PlatformTiles =
        //     {0x60, 0x61, 0x61, 0x62} en XDisp {0xE0, 0xF0, 0x00, 0x10} = −32, −16, 0, +16.
        //   - y `flags = 49` = 0x31 escrito a mano: página 1 y paleta (0x31>>1)&7 = **0**,
        //     no la paleta 1 de $166E. Con la paleta del nivel salía gris en vez de MADERA.
        //
        // Lo que NO se guarda son los seis ESLABONES de la cadena (tesela 0xA2): sus
        // posiciones se calculan en vivo con seno/coseno sobre el ángulo del balanceo
        // (`CalculateCircleCoordinatesForTiltingPlaform`), y este catálogo guarda un
        // fotograma fijo. Se dibuja la plataforma, que es por lo que se anda.
        0x5F to CustomEnemy(
            listOf(0x60, 0x61, 0x61, 0x62).mapIndexed { i, t -> OamTile(t, i * 16, 0, page = 1) },
            palRow = (8 + 0) * 16,
        ),
        // PLATAFORMA FLOTANTE DIAGONAL (0x5D), la isla de hierba de YOSHI'S ISLAND 4 — 22
        // colocaciones en ese nivel, todas ellas huecos visibles hasta ahora.
        //
        // `SprXXX_BuoyantPlatformsAndMine_01B563` ($01:B563) acaba en NormalSpritePlatformDraw,
        // y ahí la tabla $01:B2C3 vale **1** para el 0x5D (índice 0x5D − 0x55 = 8), o sea que
        // NO es la plana: es la rama DIAGONAL ($01:B2D1). Esa monta cinco teselas 16×16 con la
        // x avanzando de 8 en 8 y la y ALTERNANDO entre 0 y +16 (`oam[64]/[66]/[68]` arriba,
        // `oam[65]/[67]` 16 px más abajo): quedan tres arriba (0, 16, 32) y dos abajo
        // encajadas en medio (8, 24).
        //
        // Teselas de kNormalSpritePlatformGFXRt_DiagPlatTiles[0..4] (el índice arranca en 0
        // porque el id es ≥ 0x5B; por debajo la rutina suma 9 y coge otras), y luego la propia
        // rutina PISA las dos últimas con −53 = 0xCB y −28 = 0xE4. El volteo horizontal entra
        // a partir de la tercera (`if (r1 < r2)`), que es lo que hace la isla simétrica.
        // Página y paleta SÍ se heredan de $166E (`flags = spr_table15f6 | prio`).
        0x5D to CustomEnemy(
            listOf(
                OamTile(0xCB, 0, 0), OamTile(0xE4, 8, 16), OamTile(0xCC, 16, 0, xflip = true),
                OamTile(0xE4, 24, 16, xflip = true), OamTile(0xCB, 32, 0, xflip = true),
            ),
        ),
        // BOLA DE PINCHOS / MINA (0xA4), `SprXXX_BuoyantPlatformsAndMine_SpikeBallDraw`
        // ($01:B666): UNA sola tesela espejada en los cuatro cuadrantes, que es como el juego
        // dibuja una bola simétrica con 32×32 px de aspecto y 8 bytes de gráficos.
        //
        //   charnum = ((counter_local_frames >> 2) & 4) >> 1) − 86  → 0xAA ó 0xAC (el brillo)
        //   XDisp = {0xF8, 0x08, 0xF8, 0x08}   YDisp = {0xF8, 0xF8, 0x08, 0x08}
        //   Prop  = {0x31, 0x71, 0xA1, 0xF1}   → 0x40 = volteo H, 0x80 = volteo V
        //
        // Las cuatro Prop acaban en 1: página 1 fija. Y los bits 1-3 son 0 en las cuatro
        // (0x31>>1&7 = 0x71>>1&7 = 0xA1>>1&7 = 0xF1>>1&7 = 0), o sea paleta 0 — otra que no
        // hereda nada de $166E, que para este id diría paleta 1.
        0xA4 to CustomEnemy(
            listOf(
                OamTile(0xAA, 0, 0, page = 1),
                OamTile(0xAA, 16, 0, page = 1, xflip = true),
                OamTile(0xAA, 0, 16, page = 1, vflip = true),
                OamTile(0xAA, 16, 16, page = 1, xflip = true, vflip = true),
            ),
            palRow = (8 + 0) * 16,
        ),
        0xAB to CustomEnemy(
            listOf(OamTile(0x8a, -4, -15, page = 1), OamTile(0xaa, 0, 0, page = 1)),
            palRow = (8 + 3) * 16,
        ),
        // Blurp (0xC2): 1 tesela 16×16 (Spr0C2_Blurp, $03: charnum 0xA2; paleta de $166E).
        0xC2 to CustomEnemy(listOf(OamTile(0xA2, 0, 0))),
        // LAS TRES SUPER KOOPA, cada una en el fotograma que de verdad usa (ver
        // [superKoopaFrame], donde está el porqué de la página y del fotograma).
        //  - 0x73 es la de SUELO: corre, y correr es el fotograma 0.
        //  - 0x71 y 0x72 cruzan la pantalla VOLANDO y nunca andan: su estado fija el
        //    fotograma 2/3, la pose con la capa extendida.
        // El valor de la capa lo decide una comparación explícita con el 0x72:
        // `if (spr_spriteid >= 0x72) v4 = 4`, y por debajo v4 = 8.
        0x73 to CustomEnemy(superKoopaFrame(frame = 0, capeV4 = 4)),
        0x72 to CustomEnemy(superKoopaFrame(frame = 2, capeV4 = 4)),
        0x71 to CustomEnemy(superKoopaFrame(frame = 2, capeV4 = 8)),
        // PorcuPuffer (0xC3): pez globo 32×32 = 4 teselas 16×16 en (∓8,∓8) (Spr0C3_PorcuPuffer_Draw,
        // $03: PocruPufferTiles/DispX/DispY). Frame 0. Paleta 6 (Prop 0x0D → (0xD>>1)&7).
        0xC3 to CustomEnemy(
            listOf(
                OamTile(0x86, -8, -8, page = 1), OamTile(0xc0, 8, -8, page = 1),
                OamTile(0xa6, -8, 8, page = 1), OamTile(0xc2, 8, 8, page = 1),
            ),
            palRow = (8 + 6) * 16,
        ),
        // Fishbone (0xAA): CABEZA 16×16 (0xA6, GenericGFXRtDraw1Tile16x16, paleta $166E) + 2
        // teselas de COLA 8×8 (0xa3) detrás (Spr0AA_Fishbone_Draw, $03: TailTiles/XDisp/YDisp).
        0xAA to CustomEnemy(
            listOf(
                OamTile(0xA6, 0, 0, size16 = true, page = 1),
                OamTile(0xa3, -8, 0, size16 = false, page = 1),
                OamTile(0xa3, -8, 8, size16 = false, page = 1),
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
                OamTile(0x42, 0, 0, xflip = true, page = 1), OamTile(0x40, 16, 0, xflip = true, page = 1),
                OamTile(0x62, 0, 16, xflip = true, page = 1), OamTile(0x60, 16, 16, xflip = true, page = 1),
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
                OamTile(0xc0, 0, 0, page = 1), OamTile(0xc2, 16, 0, page = 1),
                OamTile(0xe4, 0, 16, page = 1), OamTile(0xe6, 16, 16, page = 1),
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
                OamTile(0xaa, 0, 0, palRow = (8 + 7) * 16, page = 1),   // cabeza (frame r4=1)
                OamTile(0x86, -8, 0, palRow = (8 + 2) * 16, page = 1),  // llama (cerca de la boca)
                OamTile(0x84, -20, 0, palRow = (8 + 2) * 16, page = 1),
                OamTile(0x82, -32, 0, palRow = (8 + 2) * 16, page = 1),
                OamTile(0x80, -40, 0, palRow = (8 + 4) * 16, page = 1), // punta de la llama
            ),
        ),
        // Blargg (0xA8): cabeza de dragón de lava, 5 teselas 16×16 (Spr0A8_Blargg_Draw rama
        // v2==4, $03: kSpr0A8_Blargg_Tiles/XDisp/YDisp). Frame 0 (r3=0), dir sin flip (r2=1):
        // teselas 0xa2/0xa4 (arriba), 0xc2/0xc4/0xa6 (abajo). Prop 0x05 → paleta 2, página 1.
        0xA8 to CustomEnemy(
            listOf(
                OamTile(0xa2, -8, -8, page = 1), OamTile(0xa4, 8, -8, page = 1),
                OamTile(0xc2, -8, 8, page = 1), OamTile(0xc4, 8, 8, page = 1), OamTile(0xa6, 24, 8, page = 1),
            ),
            palRow = (8 + 2) * 16,
        ),

        // ---- los que faltaban para el 100% de YOSHI'S ISLAND 1/2/3 ----
        // Todos estos tienen id ≥ 0x54, o sea fuera de kGenericSpriteOAMData_TilesOffset
        // (que solo llega a 84 entradas), así que NUNCA podían salir por la vía genérica:
        // o tienen rutina de dibujo propia, o pisan la tesela que la genérica había puesto.

        // BANZAI BILL (0x9F), Spr09F_BanzaiBill_Draw ($02:D5E4): la bala gigante, un
        // CUADRADO de 4×4 teselas 16×16 = 64×64 px, de kSpr09F_BanzaiBill_Tiles/XDisp/
        // YDisp/Prop. Las dos últimas llevan Prop 0xB3 en vez de 0x33: mismo color pero
        // VOLTEADAS en vertical, que es como el juego reaprovecha la cola. Paleta 1
        // ((0x33 >> 1) & 7), o sea fila 8+1 de la CGRAM.
        0x9F to CustomEnemy(banzaiBillTiles(), palRow = (8 + 1) * 16),

        // CHARGIN' CHUCK (0x91) y CLAPPIN' CHUCK (0x95), Spr091_CharginChuck_Draw
        // ($02:C81A): comparten rutina, y el fotograma 0 son tres teselas — cabeza,
        // un 8×8 de detalle y el cuerpo. Las teselas EXTRA (la pelota de béisbol, el
        // polvo) solo aparecen a partir del fotograma 0x14, así que no van en la pose
        // base. La paleta sale del sprite ($166E): las Prop de este fotograma solo
        // llevan el bit de volteo, sin bits de color.
        0x91 to CustomEnemy(chuckFrame0()),
        0x95 to CustomEnemy(chuckFrame0()),

        // CAJA DE MENSAJE (0xB9), Spr0B9_MessageBox ($03:8D6F): llama a la rutina
        // genérica y acto seguido PISA la tesela con `charnum = -64`, o sea 0xC0. Por eso
        // daba "sin gráfico": su id está fuera de la tabla genérica, pero el dibujo real
        // no depende de ella. Una sola tesela 16×16.
        0xB9 to CustomEnemy(listOf(OamTile(0xC0, 0, 0))),

        // KOOPA AZUL DESNUDO DESLIZÁNDOSE (0xBD), Spr0BD_SlidingNakedBlueKoopa
        // ($03:8958): mismo patrón que la caja de mensaje — genérica y luego
        // `charnum = -122` (0x86). El otro valor (-32 = 0xE0) es el del temporizador de
        // vuelta, no la pose normal.
        0xBD to CustomEnemy(listOf(OamTile(0x86, 0, 0))),

        // ESTRELLA (0x76) y 1-UP (0x78), PowerUpAndItemDraw ($01:C61A). No estaban porque
        // la vía de powerups del proyecto solo cubría seta/flor/pluma, que son los tres
        // que salen en la hoja del HUD; estos dos se colocan en el nivel como cualquier
        // otro sprite y nadie los dibujaba.
        //
        // La rutina indexa su tabla con `spriteId - 116` (0x74), o sea: 0x74 seta → 0x24,
        // 0x75 flor → 0x26, 0x76 estrella → 0x48, 0x77 pluma → 0x0E, 0x78 1-Up → 0x24.
        // El 1-Up comparte tesela con la seta normal y se distingue solo por la PALETA
        // (la verde), que sale del $166E del propio sprite — por eso aquí no se fuerza
        // ninguna: `palRow` a null y que mande el nivel.
        //
        // La estrella además parpadea: el juego le cicla la paleta con
        // `kPowerUpAndItemGFXRt_StarPalValues` fotograma a fotograma. Aquí se deja su
        // color base, que es la pose que se ve al colocarla.
        0x76 to CustomEnemy(listOf(OamTile(0x48, 0, 0))),
        0x78 to CustomEnemy(listOf(OamTile(0x24, 0, 0))),

        // BLOQUE VOLADOR (0x83), Spr083_LeftFlyingBlock ($01:AD6E): otra vez el mismo
        // patrón, `charnum = spr_table00c2[k] ? 46 : 42` → 0x2A en reposo. Y encima
        // DrawWingTiles ($01:9E95) le pone las dos ALAS, que es lo que lo hace volar.
        //
        // La posición de las alas NO es libre y conviene hacerla bien, porque si se
        // quedan cortas se montan encima del bloque y lo tapan. Sale de dos sitios que
        // se suman: la propia DrawWingTiles desplaza el sprite (−2, +2) antes del ala
        // izquierda y le suma 4 a la X antes de la derecha, y luego
        // DrawWingTiles_019E37 aplica su tabla `kDrawWingTiles_XDisp` (16 bits con
        // signo: −1 la izquierda, +9 la derecha) y `_YDisp` (−4). Total:
        //
        //   ala izquierda  x = −2 − 1 = −3     ala derecha  x = −2 + 4 + 9 = +11
        //   las dos        y = +2 − 4 = −2
        //
        // Con eso flanquean el bloque de 16 px en vez de taparlo. Son de 8×8
        // (`kDrawWingTiles_TileSize` = 0 en el fotograma 0), tesela 0x5D, y la izquierda
        // va volteada (Prop 0x46; el 0x40 es el volteo). Paleta 3 en las dos.
        //
        // Los valores van escritos aquí y no tomados de [WING_TILE]/[WING_DISP_X]: esas
        // constantes se declaran MÁS ABAJO en el fichero y todavía no existen cuando se
        // construye este mapa.
        0x83 to CustomEnemy(
            listOf(
                OamTile(0x2A, 0, 0),
                OamTile(0x5D, -3, -2, size16 = false, xflip = true, palRow = (8 + 3) * 16),
                OamTile(0x5D, 11, -2, size16 = false, palRow = (8 + 3) * 16),
            ),
        ),
        // Plataforma gris que se cae (0xC4): 4 teselas 16×16 seguidas, 64×16 en total
        // (Spr0C4_GreyFallingPlatform_Draw, $03:8492; XDisp = {0,0x10,0x20,0x30},
        // Tiles = {0x60,0x61,0x61,0x62} — extremo, centro, centro, extremo).
        //
        // Esta escribe su propiedad a mano: `flags = sprites_tile_priority | 3`. Ese 3 lleva
        // las DOS cosas: bit 0 = página 1 y bits 1-3 = paleta (3>>1)&7 = 1. Por eso no hereda
        // nada de $166E, ni la página ni la paleta.
        0xC4 to CustomEnemy(
            (0..3).map { i ->
                OamTile(intArrayOf(0x60, 0x61, 0x61, 0x62)[i], i * 16, 0, page = 1)
            },
            palRow = (8 + 1) * 16,
        ),
        // Sparky (0xA5): la chispa que recorre las paredes. UNA tesela 16×16
        // (SprXXX_WallFollowers_SparkyDraw, $02:BE4E): parte de GenericGFXRtDraw1Tile16x16 —o
        // sea, paleta y página del nivel— y solo le cambia el nº de tesela a 10 = 0x0A. Su
        // `flags ^= 16*(counter & 0xC)` es la ROTACIÓN de la chispa fotograma a fotograma
        // (volteos H/V); el fotograma 0 va sin voltear, que es el que se guarda.
        //
        // Y hay que MIRAR el ajuste, no dar por hecho la rama común: con el ajuste de GFX de
        // sprites 2 la rutina usa la otra forma —tesela -56 = 0xC8 con la propiedad de Fuzzy
        // (`kSprXXX_WallFollowers_FuzzyProp` = {0x05,0x45}: página 1, paleta (5>>1)&7 = 2; el
        // 0x40 del segundo es el volteo del parpadeo)—. Y no es un matiz de color: la tesela
        // 0x0A en un nivel de ajuste 2 cae encima del gráfico de la BOTA de Yoshi, así que
        // Sparky salía dibujado como un zapato. Se vio renderizando el PNG y mirándolo.
        0xA5 to CustomEnemy(
            listOf(OamTile(0x0A, 0, 0)),
            porAjuste = mapOf(2 to (listOf(OamTile(0xC8, 0, 0, page = 1)) to (8 + 2) * 16)),
        ),
        // Pokey (0x70): el cactus de cinco segmentos (Spr070_Pokey, $02:B665 y su bucle de
        // dibujo). Se pinta de ABAJO hacia arriba, 16 px por segmento, y el nº de tesela sale
        // de si el segmento de ENCIMA sigue vivo: si lo está va cuerpo (-24 = 0xE8) y si no,
        // CABEZA (-118 = 0x8A). Con el Pokey entero (los cinco vivos) eso da cuatro cuerpos y
        // la cabeza arriba del todo, que es lo que se guarda.
        //
        // El bamboleo en X (`XDisp = {0,+1,0,-1}` indexado por `(j + frame>>3) & 3`) es la
        // animación de balanceo; aquí se congela en el fotograma 0, o sea j&3.
        //
        // `flags = sprites_tile_priority | 5`: página 1 (bit 0) y paleta (5>>1)&7 = 2.
        0x70 to CustomEnemy(
            (0..4).map { fila ->
                // fila 0 = arriba (cabeza). En el juego el bucle va de j=4 (abajo) a j=0.
                val j = 4 - fila
                OamTile(
                    if (fila == 0) 0x8A else 0xE8,
                    intArrayOf(0, 1, 0, -1)[j and 3],
                    fila * 16,
                    page = 1,
                )
            },
            palRow = (8 + 2) * 16,
        ),
        // Bola de la BOLA CON CADENA (0x9E): la bola que gira, tesela -24 = 0xE8 16×16
        // (Spr09E_BallNChain_Sub, $02:D62A: `r8 = -24` para el id 0x9E, frente al -94 del
        // 0xA3). La CADENA no se guarda: no es una tesela, son posiciones calculadas en vivo
        // sobre `kCircleCoordinates` según el ángulo del giro, y este catálogo guarda un
        // fotograma fijo. Lo que hace daño —y lo que se ve— es la bola.
        //
        // `flags = 51` = 0x33: página 1 (bit 0) y paleta (0x33>>1)&7 = 1.
        0x9E to CustomEnemy(listOf(OamTile(0xE8, 0, 0, page = 1)), palRow = (8 + 1) * 16),
        // LOS CHUCK QUE FALTABAN. Los ocho (0x91-0x98) entran por la MISMA rutina de dibujo
        // —la tabla de sprites apunta `Spr046_DigginChuck` para todo el rango, y esa llama a
        // `Spr091_CharginChuck_Draw` ($02:C81A)—, y la cabeza y el cuerpo salen de tablas
        // indexadas por el estado de la ANIMACIÓN, no por el id. Por eso el mismo fotograma
        // vale para todos, igual que ya valía para el 0x91 y el 0x95.
        //
        // Lo que NO se guarda es el trasto de cada uno: `DrawExtraTiles` y
        // `DrawDigginChuckExtraTiles` añaden la pala del que cava, las manos del que aplaude
        // o la pelota del que bota, y eso sí depende del id y del fotograma. Se dibuja el
        // Chuck, no su herramienta. Es mucho mejor que el rectángulo de antes, pero conviene
        // que quede dicho en vez de darlo por completo.
        0x92 to CustomEnemy(chuckFrame0()),
        0x93 to CustomEnemy(chuckFrame0()),
        0x94 to CustomEnemy(chuckFrame0()),
        // (el 0x96 se queda fuera: no está puesto en NINGÚN nivel de la ROM, así que no hay
        // dónde comprobarlo ni a quién servirle el dibujo)
        0x97 to CustomEnemy(chuckFrame0()),
        0x98 to CustomEnemy(chuckFrame0()),
        // Thwomp (0x26): el bloque de piedra con cara (Spr026_Thwomp_Draw, $01:AF54). Cuatro
        // teselas 16×16 en dos filas, y las de la DERECHA son las de la izquierda volteadas
        // (Prop = {0x03, 0x43, 0x03, 0x43}: el 0x40 es el volteo). Página 1 y paleta
        // (3>>1)&7 = 1, otra que fija su propiedad a mano.
        //
        // La QUINTA tesela de la tabla (0xC8, la CARA de enfado) no entra: el bucle arranca
        // en el índice 3 y solo llega al 4 cuando `spr_table1528` no es cero, o sea cuando el
        // Thwomp ya se ha lanzado. En reposo —que es el fotograma que guarda este catálogo—
        // el juego no la dibuja.
        0x26 to CustomEnemy(
            listOf(
                OamTile(0x8E, -4, 0, page = 1),
                OamTile(0x8E, 4, 0, page = 1, xflip = true),
                OamTile(0xAE, -4, 16, page = 1),
                OamTile(0xAE, 4, 16, page = 1, xflip = true),
            ),
            palRow = (8 + 1) * 16,
        ),
        // DRY BONES (0x30 el que tira huesos, 0x32 el que se rehace). NO son Bony Beetles:
        // los tres ids comparten la rutina de MOVIMIENTO (`Spr031_BonyBeetle`, $01:E42B) pero
        // NO la de dibujo. `Spr030_ThrowingDryBones_03C3DA` ($03:C3DA) bifurca en la primera
        // linea:
        //
        //     if (spr_spriteid[k] == 49)  GenericGFXRtDraw1Tile16x16(k);   // 0x31 Bony Beetle
        //     else                        ... dibujo propio de DOS teselas ...
        //
        // O sea que el 0x31 SI es de la tabla generica (por eso ya estaba curado y salia bien)
        // y el 0x30/0x32 NO: su entrada de la tabla generica es la del Bony Beetle y por eso
        // "salian raros de forma unanime". Aqui va el dibujo de verdad.
        //
        // El bucle (v2 = 2, 1, ... hasta v2 == DATA_03C3D7[1602]) pinta, en el fotograma 0:
        //   v2=2 -> Tiles[2] = 0x66 en YDisp[2] = 0x00   (cuerpo)
        //   v2=1 -> Tiles[1] = 0x64 en YDisp[1] = 0xF0   (cabeza, 16 px mas arriba)
        // y para en v2 == 0, asi que el Tiles[0] no entra: es del fotograma de TIRAR el hueso.
        // XDisp/Prop se indexan con 3*spr_table157c (la direccion); con direccion 1 salen
        // XDisp[5]=0x00 y XDisp[4]=0xF8=-8 y Prop[5]=Prop[4]=0x03, o sea SIN volteo. Ese 3
        // otra vez lleva las dos cosas: pagina 1 (bit 0) y paleta (3>>1)&7 = 1.
        0x30 to CustomEnemy(dryBonesFrame0(), palRow = (8 + 1) * 16),
        0x32 to CustomEnemy(dryBonesFrame0(), palRow = (8 + 1) * 16),
        // PÁJARO (0x8A), `Spr08A_Bird_Draw` ($02:F3EA). Es de los pocos sprites de UNA tesela
        // de 8×8 —`sprites_oamtile_size_buffer[v1 >> 2] = 0`, y el 0 es el tamaño pequeño—,
        // así que dibujarlo como bloque de 16×16 se comería tres teselas ajenas.
        //
        //   charnum = kSpr08A_Bird_Tiles[spr_table1602] = {0xD2, 0xD3, 0xD0, 0xD1, 0x9B}
        //
        // El fotograma 0 es el de VOLAR: la rama normal de `Spr08A_Bird` ($02:F317) pone
        // `spr_table1602 = 0` en cada tick mientras cruza. Los 2/3 son el pájaro POSADO y el 4
        // el de dar la vuelta (`spr_decrementing_table15ac` ≠ 0).
        //
        //   flags = kSpr08A_Bird_Direction[157c] | kSpr08A_Bird_Palette[k & 3]
        //           Direction = {0x71, 0x31}   Palette = {0x8, 0x4, 0x6, 0xA}
        //
        // Página 1 fija (bit 0 de 0x71 y de 0x31). Y ojo con la PALETA: no sale del nivel ni
        // del sprite, sale de `k & 3`, o sea del **slot de sprite** que le haya tocado — así
        // es como el juego llena el cielo de pájaros de colores distintos con un solo gráfico.
        // Aquí se guarda la del slot 0 (0x8 → (8>>1)&7 = 4), que es una elección obligada:
        // no hay "la" paleta del pájaro. Dirección 157c = 1 → 0x31, sin volteo.
        0x8A to CustomEnemy(
            listOf(OamTile(0xD2, 0, 0, size16 = false, page = 1)),
            palRow = (8 + 4) * 16,
        ),
        // CHIMENEA de la CASA DE YOSHI (0x8C), `Spr08C_SideExitAndFireplace` ($02:F4D5). Su
        // trabajo de verdad es poner `flag_side_exits = 1` —por eso se llama SideExit— y de
        // paso dibuja el fuego. DOS teselas de 8×8 apiladas (`oamtile_size_buffer` = 0 en las
        // dos), en ypos −80 y −72: 8 px de separación.
        //
        //   TopTile    = {0xD4, 0xAB}      BottomTile = {0xBB, 0x9A}
        //   v3 = spr_table00c2 & 1         (el parpadeo de la llama, que sube al azar)
        //   flags = 53 = 0x35              → página 1, paleta (0x35>>1)&7 = 2
        //
        // ⚠ Sus xpos/ypos son ABSOLUTOS de pantalla (−72, −80), no relativos al sprite: es
        // decoración clavada en un sitio fijo de la sala, no un enemigo que se mueva. Para el
        // catálogo eso da igual —lo que importa es el par de teselas—, pero conviene saberlo
        // antes de intentar colocarlo por la posición del sprite.
        0x8C to CustomEnemy(
            listOf(
                OamTile(0xD4, 0, 0, size16 = false, page = 1),
                OamTile(0xBB, 0, 8, size16 = false, page = 1),
            ),
            palRow = (8 + 2) * 16,
        ),
        // BURBUJA CON UN SPRITE DENTRO (0x9D), `Spr09D_BubbleWithSprite` ($02:D8BB) y su
        // dibujo ($02:D9D6). Era el hueco más grande que quedaba: 54 colocaciones.
        //
        // Son DOS dibujos superpuestos y hay que montar los dos o no se entiende:
        //
        //  · LA BURBUJA, cinco entradas (el bucle va de v3 = 4 a 0). Las cuatro primeras son
        //    la MISMA tesela 0xA0 de 16×16 espejada en los cuatro cuadrantes —
        //    kSpr09D_BubbleWithSprite_Prop = {0x07, 0x47, 0x87, 0xC7}, o sea 0x40 volteo H y
        //    0x80 volteo V—, que es como el juego hace un círculo con un cuarto de círculo.
        //    Página 1 y paleta (7>>1)&7 = 3. La quinta es un 8×8 (TileSize = 0) con la tesela
        //    0x99 y Prop 0x03 (paleta 1): el BRILLO del cristal.
        //    Posiciones de XDisp/YDisp con el desplazamiento del fotograma 0
        //    (`r2 = DATA_02D9D2[(counter_local_frames >> 3) & 3]` = 0). Los otros tres valores
        //    de esa tabla (5 y 10) son la burbuja RESPIRANDO, un par de píxeles arriba y
        //    abajo; no cambian el dibujo, así que se guarda el 0.
        //
        //  · LO QUE LLEVA DENTRO. La rutina llama primero a `GenericGFXRtDraw1Tile16x16` y
        //    acto seguido PISA charnum y flags con `BubbleSprTiles1[00c2]` /
        //    `BubbleSprGfxProp1[00c2]`. Esa llamada genérica no aporta nada —el id 0x9D está
        //    fuera de las 84 entradas de la tabla— y por eso la burbuja no salía por ahí.
        //
        // ⚠ Y QUÉ lleva dentro NO es fijo: `Spr09D_BubbleWithSprite_Init` ($01:8564) hace
        // `00c2 = Spr04C_ExplodingBlock_Init(k)` = `(spr_xpos_lo >> 4) & 3`, o sea que lo
        // decide el NIBBLE ALTO DE LA X de la colocación. Los cuatro son
        // `kSpr09D_BubbleWithSprite_BubbleSprites` = {0x0F Goomba, 0x0D Bob-omb,
        // 0x15 Cheep-Cheep, 0x74 seta}, con teselas {0xA8, 0xCA, 0x67, 0x24} y Prop
        // {0x84, 0x85, 0x05, 0x08}. Este catálogo guarda UN fotograma por id, así que va el
        // 0 (el Goomba, Prop 0x84 → página 0, paleta 2 y volteo vertical). Los otros tres se
        // renderizaron y salen: el Bob-omb negro, el Cheep-Cheep y la seta — de hecho el
        // Cheep-Cheep de aquí usa la tesela 0x67, la misma que la tabla genérica da al 0x15,
        // que es una confirmación por otra puerta de que esa tesela impar era la buena.
        0x9D to CustomEnemy(
            listOf(
                OamTile(0xA0, -8, -10, page = 1, palRow = (8 + 3) * 16),
                OamTile(0xA0, 8, -10, page = 1, xflip = true, palRow = (8 + 3) * 16),
                OamTile(0xA0, -8, 2, page = 1, vflip = true, palRow = (8 + 3) * 16),
                OamTile(0xA0, 8, 2, page = 1, xflip = true, vflip = true, palRow = (8 + 3) * 16),
                OamTile(0x99, -1, -4, size16 = false, page = 1, palRow = (8 + 1) * 16),
                // el contenido por defecto: Goomba (Prop 0x84 = página 0, paleta 2, volteo V)
                OamTile(0xA8, 0, 0, page = 0, vflip = true, palRow = (8 + 2) * 16),
            ),
        ),
        // FUZZY DE GUÍA (0x68), `SprXXX_LineGuided_LineFuzzyPlats` ($01:D74A). El id 0x68 =
        // 104 entra por `if (v1 == 104) SprXXX_LineGuided_01DBD4(k)` ($01:DBD4), que otra vez
        // llama a la genérica y PISA lo que hace falta:
        //
        //     oam[64].charnum = -56;                                   // 0xC8
        //     oam[64].flags = prio | kSprXXX_LineGuided_DATA_01DC09[v2];  // {0x05, 0x45}
        //
        // Una sola tesela 16×16 (`FinishOAMWrite(k, 2, 0)`), página 1 y paleta (5>>1)&7 = 2;
        // el 0x45 del segundo valor es el mismo dibujo VOLTEADO, que es el parpadeo del bicho.
        //
        // La tesela 0xC8 con paleta 2 es exactamente la que ya usaba el Sparky (0xA5) en su
        // variante `porAjuste[2]`, y no es casualidad: allí también es el Fuzzy.
        0x68 to CustomEnemy(listOf(OamTile(0xC8, 0, 0, page = 1)), palRow = (8 + 2) * 16),
        // PLATAFORMA CON CUENTA ATRÁS (0xBA), `Spr0BA_TimedPlatform_Draw` ($03:8E12). Tres
        // entradas, y la tercera es la gracia del sprite:
        //
        //   v2=0  (0, 0)   tesela 0xC4  16×16  Prop 0x0B
        //   v2=1  (16, 0)  tesela 0xC4  16×16  Prop 0x4B   ← la misma en espejo
        //   v2=2  (12, 4)  DÍGITO       8×8    Prop 0x0B   (TileSize = 0)
        //
        // Prop 0x0B = página 1 y paleta (0x0B>>1)&7 = 5 en las tres; el 0x40 del 0x4B es el
        // espejo, que es como el juego hace una plataforma simétrica con media.
        //
        // El dígito sale de `kSpr0BA_TimedPlatform_NumberTiles[spr_table1570 >> 6]` =
        // {0xB6, 0xB5, 0xB4, 0xB3}, o sea 1, 2, 3 y 4 — verificado renderizando los cuatro.
        // El Init ($01:8326) arranca el contador en 0xFF o en 63 según `spr_xpos_lo & 0x10`,
        // así que la plataforma nace mostrando el 4 (0xFF>>6 = 3) o el 1 (63>>6 = 0). Se
        // guarda el CUATRO, que es la cuenta entera y la pose con la que aparece la variante
        // normal. (Cuando `1570 < 8` el bucle arranca en v2 = 1 y el dígito ya no se dibuja.)
        0xBA to CustomEnemy(
            listOf(
                OamTile(0xC4, 0, 0, page = 1),
                OamTile(0xC4, 16, 0, page = 1, xflip = true),
                OamTile(0xB3, 12, 4, size16 = false, page = 1),
            ),
            palRow = (8 + 5) * 16,
        ),
    )

    /**
     * Fotograma 0 (de pie/andando) del DRY BONES, de `kSpr030_ThrowingDryBones_DryBonesTiles`
     * / `_DryBonesTileXDisp` / `_DryBonesTileYDisp` / `_DryBonesGfxProp` ($03). Dos teselas de
     * 16×16 (`FinishOAMWrite(k, 2, 2)` → tamaño 2 = 16×16): la cabeza medio bloque a la
     * izquierda y por encima del cuerpo. El fotograma 1 solo cambia el cuerpo (0x66→0x68) y
     * baja la cabeza 1 px, que es el pasito de andar.
     */
    private fun dryBonesFrame0(): List<OamTile> = listOf(
        OamTile(0x64, -8, 0, page = 1),  // cabeza (YDisp 0xF0 = −16 respecto al cuerpo)
        OamTile(0x66, 0, 16, page = 1),  // cuerpo (YDisp 0x00)
    )

    /**
     * Los 16 cuadros del BANZAI BILL, en el orden de sus tablas: cuatro filas de cuatro
     * teselas 16×16. Se saca a función porque la lista es larga y en el mapa estorbaría.
     */
    private fun banzaiBillTiles(): List<OamTile> {
        val tiles = intArrayOf(
            0x80, 0x82, 0x84, 0x86,
            0xA0, 0x88, 0xCE, 0xEE,
            0xC0, 0xC2, 0xCE, 0xEE,
            0x8E, 0xAE, 0x84, 0x86,
        )
        // kSpr09F_BanzaiBill_Prop: 0x33 en todas menos las dos últimas, que son 0xB3
        // (bit 0x80 = volteo vertical).
        return tiles.mapIndexed { i, t ->
            OamTile(t, (i % 4) * 16, (i / 4) * 16, vflip = i >= 14, page = 1)
        }
    }

    /**
     * Fotograma 0 de los CHUCK (`spr_table1602` = 0, `spr_table151c` = 0, mirando a la
     * derecha). Cabeza de kSpr091_CharginChuck_HeadTiles[0] con su volteo (Prop 0x40),
     * el 8×8 de kSpr091_CharginChuck_BodyTiles1[0] —tamaño 0 en BodyTileSize1— y el
     * cuerpo de BodyTiles2[0].
     */
    private fun chuckFrame0(): List<OamTile> = listOf(
        OamTile(0x06, -8, -8, xflip = true),        // cabeza (HeadXDisp/YDisp = 0xF8 = −8)
        OamTile(0x0D, -8, 6, size16 = false),       // detalle 8×8 (BodyXDisp1/YDisp1)
        OamTile(0x4E, 0, 0),                        // cuerpo (BodyXDisp2 = 0)
    )

    /**
     * Sprites que NO SE DIBUJAN, y no por falta de datos: su rutina no llega a poner una
     * sola tesela en el OAM. Distinguirlos importa, porque si no se cuentan para siempre
     * como "enemigos sin gráfico" y se acaba inventándoles un dibujo que el juego no tiene.
     *
     *  - `0x8E` AGUJERO DE TELETRANSPORTE (`Spr08E_WarpHole`, $02:EADA): su rutina entera
     *    es comprobar la colisión con Mario y moverlo. Ni una llamada de dibujo.
     *  - `0xC7` SETA INVISIBLE (`Spr0C7_InvisibleMushroom`, $03:C30F): tampoco dibuja; al
     *    tocarla se convierte en el sprite 116 (la seta de verdad) y es ESA la que se ve.
     *    El nombre ya lo decía.
     */
    val INVISIBLE_SPRITES: Set<Int> = setOf(0x8E, 0xC7)

    /** true si [spriteId] es invisible A PROPÓSITO (ver [INVISIBLE_SPRITES]). */
    fun isIntentionallyInvisible(spriteId: Int): Boolean = spriteId in INVISIBLE_SPRITES

    // ─────────────────────── GRÁFICOS DINÁMICOS (subidos por DMA) ───────────────────────
    //
    // Hay sprites cuyas teselas NO están en los cuatro ficheros GFX del nivel: el juego se
    // las mete en la VRAM de sprites por DMA en CADA fotograma, desde un buffer de RAM.
    // Dibujarlos desde el tileset estático del nivel no da "algo parecido": da lo que
    // hubiera en esa ranura del fichero, que en la ranura 0x06 es la FUENTE (letras).
    //
    // Durante un tiempo esto se dio por irrecuperable y los ids afectados se dejaron fuera
    // del catálogo. No lo es: el buffer de RAM del que sale el DMA se puede reconstruir
    // desde la ROM, porque es GFX33 descomprimida. Abajo está la cadena entera.

    /**
     * `UploadPlayerGFX` ($00:A300) copia, cada fotograma, dos bloques de 0x40 bytes por
     * ranura a la VRAM de sprites:
     *
     * ```c
     * uint16 t = *(uint16 *)&graphics_dynamic_sprite_pointers_top_lo[v0];
     * SmwCopyToVram(0x6000 + v0 * 0x10, g_ram + t, 0x40);   // fila de ARRIBA
     * ...
     * SmwCopyToVram(0x6100 + v1 * 0x10, g_ram + t, 0x40);   // fila de ABAJO
     * ```
     *
     * La VRAM de sprites empieza en la palabra 0x6000 y cada tesela ocupa 0x10 palabras, o
     * sea que `0x6000 + v0*0x10` es la TESELA `v0` y `0x6100 + v1*0x10` la `0x10 + v1`. Y
     * 0x40 bytes son DOS teselas de 4bpp, así que la ranura `v0` ocupa las teselas
     * `v0`,`v0+1` arriba y `0x10+v0`,`0x10+v0+1` abajo. Todas en la página 0 (SP1).
     */
    private const val DYNAMIC_TILES_PER_COPY = 2

    /** Primera tesela de VRAM de la fila de ABAJO de las ranuras dinámicas (`0x6100`). */
    private const val DYNAMIC_BOTTOM_TILE = 0x10

    /**
     * Base en g_ram del buffer del que sale ese DMA: **$7E:7D00**, y lo que hay ahí es
     * GFX33 descomprimida y expandida a 4bpp.
     *
     * Sale de `GraphicsDecompressionRoutines_DecompressGFX32And33` ($00:B888): copia GFX33
     * cruda (3bpp) a `g_ram + 0x2000` y la reexpande a 4bpp escribiendo HACIA ABAJO desde
     * `g_ram + 0xACFE`, 32 bytes de salida por cada 24 de entrada. Con las 384 teselas de
     * GFX33 eso son 0x3000 bytes, o sea desde 0xACFF hasta 0x7D00 justo. La versión con el
     * parche de Lunar Magic lo deja escrito sin rodeos: `memcpy(g_ram + 0x7d00, kGfx33, …)`.
     *
     * El mismo 0x7D00 lo usa ya [SnesGameRecipes] para las teselas ANIMADAS (monedas,
     * bloques `?`, agua), que salen del mismo buffer por la misma cuenta.
     */
    private const val GFX33_RAM_BASE = 0x7D00

    /** Bytes por tesela en ese buffer: está expandido a 4bpp, aunque la fuente sea 3bpp. */
    private const val GFX33_RAM_TILE = 32

    /** Una ranura de gráficos dinámicos: qué punteros de RAM instala la rutina del sprite. */
    private class DynamicGfx(
        /** Índice de ranura (`graphics_dynamic_sprite_pointers_*[slot]`) = nº de tesela VRAM. */
        val slot: Int,
        /** Puntero g_ram de la fila de ARRIBA. */
        val topRam: Int,
        /** Puntero g_ram de la fila de ABAJO. */
        val bottomRam: Int,
    )

    /**
     * Sprites que instalan sus propias teselas dinámicas, con los punteros EXACTOS que deja
     * escritos su rutina.
     *
     * **Podoboo (0x33)**, `Spr033_Podoboo` ($01:E093), rama normal (`spr_table00c2` == 0, la
     * bola de lava; con 00c2 ≠ 0 es la llamarada de Bowser, que dibuja otra cosa):
     *
     * ```c
     * GenericGFXRtDraw4Tiles8x8Square(k, 1);
     * *(uint16 *)&graphics_dynamic_sprite_pointers_top_lo[6]    = 0x8600;
     * *(uint16 *)&graphics_dynamic_sprite_pointers_bottom_lo[6] = 0x8800;
     * ```
     *
     * Ranura 6 → teselas de VRAM 0x06/0x07 (arriba) y 0x16/0x17 (abajo), que es EXACTAMENTE
     * su entrada de la tabla genérica (`06 06 16 16`). Y las fuentes:
     * (0x8600 − 0x7D00)/32 = tesela **72** de GFX33 y (0x8800 − 0x7D00)/32 = la **88**
     * (72 + 16, o sea la de justo debajo en una hoja de 16 de ancho: es un 16×16 partido).
     */
    private val DYNAMIC_GFX: Map<Int, DynamicGfx> = mapOf(
        0x33 to DynamicGfx(slot = 6, topRam = 0x8600, bottomRam = 0x8800),
    )

    /** Ids que reciben sus gráficos por DMA desde GFX33 en vez de desde el tileset del nivel. */
    val DYNAMIC_GFX_SPRITES: Set<Int> = DYNAMIC_GFX.keys

    /** true si [spriteId] recibe sus gráficos por DMA (ver [DYNAMIC_GFX]). */
    fun hasDynamicGraphics(spriteId: Int): Boolean = spriteId in DYNAMIC_GFX_SPRITES

    /**
     * Nº de tesela de VRAM de sprites → nº de tesela dentro de GFX33, para [d]. Es la
     * traducción literal de las dos copias de `UploadPlayerGFX`.
     */
    private fun dynamicTileMap(d: DynamicGfx): Map<Int, Int> {
        val top = (d.topRam - GFX33_RAM_BASE) / GFX33_RAM_TILE
        val bottom = (d.bottomRam - GFX33_RAM_BASE) / GFX33_RAM_TILE
        val m = HashMap<Int, Int>()
        for (i in 0 until DYNAMIC_TILES_PER_COPY) {
            m[d.slot + i] = top + i
            m[DYNAMIC_BOTTOM_TILE + d.slot + i] = bottom + i
        }
        return m
    }

    /**
     * La traducción de [dynamicTileMap] de un id, expuesta SIN ROM para poder fijarla en un
     * test: qué tesela de GFX33 acaba en qué tesela de VRAM es la decisión del port, y sin
     * ROM no hay otra forma de comprobarla. null si el id no lleva gráficos dinámicos.
     */
    fun dynamicTileMapForTest(spriteId: Int): Map<Int, Int>? =
        DYNAMIC_GFX[spriteId]?.let { dynamicTileMap(it) }

    /**
     * GFX33 descomprimida (3bpp, tal cual está en la ROM: la expansión a 4bpp del juego solo
     * añade un plano de ceros, así que para leer píxeles da igual). No está en la tabla de
     * punteros de GFX —que solo llega a GFX31—: va CONTIGUA detrás de GFX32 (Mario), cuyo
     * puntero sí es fijo ($00:38D8/38D9 lo/hi y $00:3890 el banco). null si no descomprime.
     */
    private fun gfx33(rom: ByteArray, delta: Int): ByteArray? {
        val lo = rom.getOrNull(SnesGameRecipes.SMW_GFX32_LO_PC + delta)?.toInt()?.and(0xFF) ?: return null
        val hi = rom.getOrNull(SnesGameRecipes.SMW_GFX32_HI_PC + delta)?.toInt()?.and(0xFF) ?: return null
        val bank = rom.getOrNull(SnesGameRecipes.SMW_GFX32_BANK_PC + delta)?.toInt()?.and(0xFF) ?: return null
        val addr = lo or (hi shl 8)
        if (addr < 0x8000) return null
        val pc = (bank and 0x7F) * 0x8000 + (addr - 0x8000)
        if (pc < 0x40000 || pc >= rom.size) return null
        val g32 = runCatching { LcLz2.decompress(rom, pc) }.getOrNull() ?: return null
        return runCatching { LcLz2.decompress(rom, pc + g32.consumedBytes).data }.getOrNull()
    }

    /** Frame 0 de andar de Super Koopa: cuerpo 16×16 (paleta del sprite) + 3 teselas de capa 8×8. */
    /**
     * Un fotograma de SUPER KOOPA (`SprXXX_SuperKoopas_Draw`, $02:ECDE), armado desde sus
     * tablas reales: nueve fotogramas de cuatro teselas. [capeV4] es el valor que el juego
     * mete en la propiedad de las teselas de CAPA — 8 para el 0x71 y 4 desde el 0x72, por una
     * comparación explícita: `if (spr_spriteid >= 0x72) v4 = 4`.
     *
     * Se copian las tablas enteras en vez de escribir a mano un fotograma porque cuál toca
     * depende del ESTADO del enemigo, y teniéndolas delante se puede sacar el que de verdad
     * usa cada variante en vez del primero que salga.
     *
     * ⚠ AQUÍ ESTABAN LOS DOS FALLOS que tenían a estas Koopas dibujadas como una mancha:
     *
     *  1. LA PÁGINA de tesela NO se hereda del nivel, y darla por heredada leía la mitad
     *     equivocada de la VRAM de sprites. La rutina tiene dos ramas y ninguna la coge de
     *     $166E: las teselas con el bit 1 en su Prop (la CAPA) van por `v5 = (Prop|v4) & ~2`,
     *     que deja el bit 0 a 1 → página 1 fija; el resto van por `v5 = r5 | Prop` con
     *     `r5 = spr_table15f6[k] & 0xE`, y ese `& 0xE` **borra el bit 0** del valor del nivel,
     *     así que la página sale del Prop de la tesela y de nada más.
     *  2. EL FOTOGRAMA. El catálogo usaba el 0 para las dos que había, pero el 0 es el de
     *     ANDAR y la voladora no anda nunca: cruza la pantalla. Su estado fija
     *     `spr_table1602 = 2` ó `3` (`SprXXX_SuperKoopas_02EBF8`, $02:EBF8), que es la pose
     *     con la capa extendida. El 0 es el de la de suelo mientras corre
     *     (`SprXXX_SuperKoopas_02EBB5` con r0≠0, $02:EBB5).
     *
     * Las tablas van DENTRO de la función a propósito: [CUSTOM_ENEMIES] se construye al
     * cargar la clase y llama aquí, así que unas constantes declaradas más abajo en el
     * fichero todavía no existirían — mismo tropiezo que ya está anotado en las alas.
     */
    private fun superKoopaFrame(frame: Int, capeV4: Int): List<OamTile> {
        val tiles = intArrayOf(
            0xc8, 0xd8, 0xd0, 0xe0, 0xc9, 0xd9, 0xc0, 0xe2, 0xe4, 0xe5, 0xf2, 0xe0,
            0xf4, 0xf5, 0xf2, 0xe0, 0xda, 0xca, 0xe0, 0xcf, 0xdb, 0xcb, 0xe0, 0xcf,
            0xe4, 0xe5, 0xe0, 0xcf, 0xf4, 0xf5, 0xe2, 0xcf, 0xe4, 0xe5, 0xe2, 0xcf,
        )
        val xDisp = intArrayOf(
            0x8, 0x8, 0x10, 0x0, 0x8, 0x8, 0x10, 0x0, 0x8, 0x10, 0x10, 0x0,
            0x8, 0x10, 0x10, 0x0, 0x9, 0x9, 0x0, 0x0, 0x9, 0x9, 0x0, 0x0,
            0x8, 0x10, 0x0, 0x0, 0x8, 0x10, 0x0, 0x0, 0x8, 0x10, 0x0, 0x0,
        )
        // Los negativos vienen como 0xFF/0xFD en la tabla: aquí van ya con signo.
        val yDisp = intArrayOf(
            0x0, 0x8, 0x8, 0x0, 0x0, 0x8, 0x8, 0x0, 0x3, 0x3, 0x8, 0x0,
            0x3, 0x3, 0x8, 0x0, -1, 0x7, 0x0, 0x0, -1, 0x7, 0x0, 0x0,
            -3, -3, 0x0, 0x0, -3, -3, 0x0, 0x0, -3, -3, 0x0, 0x0,
        )
        val prop = intArrayOf(
            0x3, 0x3, 0x3, 0x0, 0x3, 0x3, 0x3, 0x0, 0x3, 0x3, 0x1, 0x1,
            0x3, 0x3, 0x1, 0x1, 0x83, 0x83, 0x80, 0x0, 0x83, 0x83, 0x80, 0x0,
            0x3, 0x3, 0x0, 0x1, 0x3, 0x3, 0x0, 0x1, 0x3, 0x3, 0x0, 0x1,
        )
        val size = intArrayOf(
            0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2,
            0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 2, 0,
            0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 0,
        )
        return (0..3).map { i ->
            val v = frame * 4 + i
            val p = prop[v]
            val capa = (p and 2) != 0
            val flags = if (capa) (p or capeV4) and 2.inv() else p
            OamTile(
                tiles[v], xDisp[v], yDisp[v],
                size16 = size[v] == 2,
                vflip = (p and 0x80) != 0,
                // La capa lleva su paleta en la fórmula; el resto se queda con la del nivel,
                // porque su Prop no aporta bits de paleta y `r5 | Prop` deja los de $166E.
                palRow = if (capa) (8 + ((flags shr 1) and 7)) * 16 else null,
                page = flags and 1,
            )
        }
    }

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
        // Sprites de CUADRADO de 4 teselas (trampolin...): su dibujo no es un bloque de
        // 16x16 por byte, sino cuatro teselas de 8x8 seguidas. Sin esta rama saldrian como
        // basura, que es exactamente lo que parecian antes de encontrar la rutina.
        if (spriteId in SQUARE_SPRITES) {
            val n = if (spriteId in SQUARE_ANIMATED) ATLAS_FRAMES else 1
            val cuadros = (0 until n).mapNotNull { squareTileImage(rom, header, level, spriteId, it) }
            if (cuadros.isNotEmpty()) return cuadros
        }
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
        // Los OTROS TRES Koopa CON caparazón. Se quedaron fuera al entrar en el catálogo
        // (solo estaba el rojo 0x05) y el andar les salía CONGELADO mientras su hermano
        // rojo movía las patas. No hace falta mirarlo para saberlo: en
        // `kGenericSpriteOAMData_TilesOffset` ($01) las cuatro entradas 0x04..0x07 valen
        // LO MISMO, 0x00 —igual que las cuatro aladas 0x08..0x0B, que sí animaban—, y el
        // fotograma se suma con `+ 2 * spr_table1602[k]` sobre ese offset. O sea: los cuatro
        // Koopa leen exactamente las mismas teselas y solo se diferencian en la PALETA.
        0x04, 0x06, 0x07,
        // Verificados renderizando ambos fotogramas desde la ROM (2º fotograma REAL):
        // Cheep-Cheep (aleteo de aleta), Spike Top (giro), Bony Beetle (mandíbula), Boo
        // (se tapa/destapa la cara), Eerie (ondeo), Rip Van Fish (aletas), Topo (andar).
        0x15, 0x16, 0x2E, 0x31, 0x37, 0x38, 0x39, 0x3D, 0x4D, 0x4E,
        // Spiny (0x13): mismo caso que el Goomba —anda por la via generica—, y sus dos
        // fotogramas se diferencian en las PATAS. Visto en los dos PNG.
        0x13,
        // Los otros dos Cheep-Cheep (0x18, 0x47): comparten entrada con el 0x15/0x16, que ya
        // animaban, y su 2o byte OAM (0x69) es el pez con la aleta en la otra posicion.
        0x18, 0x47,
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

    /**
     * ¿Anima el id, por cualquiera de las cuatro vías? Cada una suma el fotograma a un sitio
     * distinto: el ala de las Parakoopa, la hélice de la Piraña saltarina, `+4` por cuadro en
     * los de cuadrado ([SQUARE_ANIMATED]) y `+1` en la tabla genérica ([ANIMATED_2FRAME]).
     */
    private fun anima(spriteId: Int): Boolean =
        isWinged(spriteId) || isJumpingPiranha(spriteId) ||
            spriteId in SQUARE_ANIMATED || genericAnimFrames(spriteId) > 1

    /** Nº de fotogramas de animación del id en el atlas: 2 si anima (ver [anima]), 1 si no. */
    fun animFrameCount(spriteId: Int): Int = if (anima(spriteId)) ATLAS_FRAMES else 1

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
        /** GFX33 descomprimida, fuente de las teselas DINÁMICAS del nivel (o null si no hay). */
        private val dynData: ByteArray? = null,
        /** Nº de tesela de VRAM → nº de tesela en [dynData] (ver [dynamicTileMap]). */
        private val dynMap: Map<Int, Int> = emptyMap(),
    ) {
        private fun tileIndices(tile9: Int): IntArray? {
            // Las teselas DINÁMICAS mandan sobre el tileset del nivel: el DMA de cada
            // fotograma las PISA en la VRAM, así que lo que hubiera en el fichero GFX no se
            // llega a ver nunca. Si el mapa no cubre esta tesela se sigue por la vía normal.
            dynMap[tile9]?.let { t ->
                val data = dynData ?: return null
                val off = t * FORMAT.bytesPerTile
                if (off + FORMAT.bytesPerTile > data.size) return null
                return SnesDecoder.decodeTile(data, off, FORMAT, t).pixelIndices
            }
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
        // Si el sprite instala teselas DINÁMICAS, se le añade GFX33 como fuente de ESAS
        // teselas concretas. Lo demás (paleta, página, los otros ficheros GFX) no cambia:
        // el DMA solo pisa un puñado de ranuras de la VRAM, no el banco entero.
        val dyn = DYNAMIC_GFX[spriteId]
        return if (dyn == null) {
            LevelSpriteArt(spData, cgram, cgRow, page)
        } else {
            LevelSpriteArt(spData, cgram, cgRow, page, gfx33(rom, delta), dynamicTileMap(dyn))
        }
    }
}
