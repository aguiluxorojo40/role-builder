package com.rolebuilder.core.snes

/**
 * Catálogo de los SPRITES del overworld con su dibujo REAL, extraído de las rutinas
 * `OWSpr00..OWSpr0A` del banco $04.
 *
 * ## Cómo dibuja el juego un sprite del mapa
 * Todas las rutinas acaban en `OWSpr04_PiranhaPlant_GenericOverworldSpriteGFXRt_Entry2`
 * ($04:FB0A), que recibe un word `a` con **la tesela en el byte bajo y los atributos OAM en el
 * alto**, y un parámetro `cr` que va al buffer de tamaños como `cr << 1`:
 *  - `cr = 0` → sprite de **8×8** (es lo que hace `..._Draw8x8` $04:FAED).
 *  - `cr = 1` → sprite de **16×16** (lo que hace `...GenericOverworldSpriteDraw` $04:FB06);
 *    en la SNES eso son las teselas `N`, `N+1`, `N+16` y `N+17`.
 *
 * Los atributos OAM son `vhoopppN`: bit 7 volteo vertical, bit 6 horizontal, bits 5-4
 * prioridad, **bits 3-1 la sub-paleta** y bit 0 la página de teselas. De ahí sale la columna
 * "paleta" de la tabla; la sub-paleta P son los colores `128 + P*16` de la CGRAM, que en el
 * overworld pone `kGlobalPalettes_OW_Sprites`.
 *
 * La posición es `GetOverworldSpriteOnScreenPosition` ($04:FE62) = **posición del sprite menos
 * la cámara**, así que sobre un render del área completa (sin cámara) valen las coordenadas
 * crudas del mapa.
 *
 * ## Cuáles tienen sitio fijo y cuáles no
 * Solo los sprites cuyas coordenadas salen de las 13 ranuras estáticas
 * ([SmwOverworld.mapSprites], $04:F625) están en un punto concreto del mapa: el **cartel de
 * BOWSER** y los **Boos**. Los demás **NO tienen posición en los datos**:
 *  - Lakitu, pájaro azul, Cheep-Cheep, planta piraña y nubes los **genera el juego al vuelo**
 *    (`OverworldLightningAndRandomCloudSpawning` $04:F708), en sitios que dependen de la
 *    partida.
 *  - Koopa Kid, humo y Bowser se colocan en tiempo de ejecución desde tablas propias según
 *    dónde esté el jugador (p. ej. `kOWSpr06_KoopaKid_InitialX/Y`).
 *
 * Por eso este fichero expone su **arte** (para extraerlo y exportarlo, que es a lo que va la
 * app) en vez de pintarlos en posiciones inventadas sobre el mapa. Dibujar una nube en un
 * sitio elegido a dedo no sería el mapa del juego.
 */
object SmwOverworldSprites {

    /**
     * Un sprite del overworld tal y como lo dibuja su rutina. [tile] es la tesela OAM,
     * [palette] la sub-paleta (bits 3-1 del byte de atributos) y [size16] el tamaño.
     * [spawned] marca los que el juego coloca en tiempo de ejecución (sin sitio en los datos).
     */
    data class OwSpriteDef(
        val id: Int,
        val name: String,
        val tile: Int,
        val palette: Int,
        val size16: Boolean,
        val spawned: Boolean,
        /**
         * Nº de teselas de 8×8 en HORIZONTAL que forma el sprite, hacia la IZQUIERDA desde
         * [tile] (como hace el cartel: 111, 110, 109, 108). 1 para el resto.
         */
        val tilesLeftward: Int = 1,
        /** Nota sobre lo que NO cubre esta definición (fotogramas extra, piezas sueltas…). */
        val note: String = "",
    )

    /**
     * Los sprites del overworld con los valores LEÍDOS de sus rutinas (tesela, atributos y
     * tamaño). No incluye el Cheep-Cheep (id 3): su dibujo va por `_04FA7D`/`_04FA83` y no lo
     * he portado todavía — prefiero dejarlo fuera a meterlo a ojo.
     */
    val CATALOG: List<OwSpriteDef> = listOf(
        // OWSpr01_Lakitu ($04:F8CC): cuerpo con `_04FB7A(0x3226)` → tesela $26, atributos $32.
        OwSpriteDef(SmwOverworld.SPRITE_LAKITU, "Lakitu", 0x26, 1, true, true,
            note = "solo el cuerpo; la nube lleva teselas $28 aparte"),
        // OWSpr02_BlueBird ($04:F9B8): `_04FB7A(v5)` con tesela 76 y atributos 54/118.
        OwSpriteDef(SmwOverworld.SPRITE_BLUE_BIRD, "Pájaro azul", 0x4C, 3, true, true,
            note = "el ala va en teselas aparte según el fotograma"),
        // OWSpr04_PiranhaPlant ($04:FAF1): teselas 42/44 según fotograma, atributos 50.
        OwSpriteDef(SmwOverworld.SPRITE_PIRANHA, "Planta piraña", 0x2A, 1, true, true,
            note = "alterna con la tesela $2C cada pocos fotogramas"),
        // OWSpr05_Cloud ($04:FB37): dos mitades de la tesela 68, la derecha volteada.
        OwSpriteDef(SmwOverworld.SPRITE_CLOUD, "Nube", 0x44, 1, true, true,
            note = "son dos mitades: la segunda a +16 px y volteada en horizontal"),
        // OWSpr06_KoopaKid ($04:FB98): tesela 106, atributos 34/98.
        OwSpriteDef(SmwOverworld.SPRITE_KOOPA_KID, "Koopa Kid", 0x6A, 1, true, true),
        // OWSpr07_Smoke ($04:FC46): 4 teselas de 8×8, alterna 40/95, atributos 48.
        OwSpriteDef(SmwOverworld.SPRITE_SMOKE, "Humo", 0x28, 0, false, true,
            note = "son 4 volutas de 8×8 que alternan con la tesela $5F"),
        // OWSpr08_BowserSign ($04:FCE1): 4 teselas de 8×8, 111→108. Sub-paleta PARPADEANTE.
        OwSpriteDef(SmwOverworld.SPRITE_BOWSER_SIGN, "Cartel de BOWSER", 0x6F, 0, false, false,
            tilesLeftward = 4, note = "teselas 111→108 hacia la izquierda; la sub-paleta parpadea 0-3"),
        // OWSpr09_Bowser ($04:FD24): tesela 104, atributos 0/64.
        OwSpriteDef(SmwOverworld.SPRITE_BOWSER, "Bowser", 0x68, 0, true, true),
        // OWSpr0A_Boo ($04:FD70): tesela 96, atributos 52/68.
        OwSpriteDef(SmwOverworld.SPRITE_BOO, "Boo", 0x60, 2, true, false),
    )

    /** Ancho de celda de la hoja: cabe el sprite más ancho (el cartel, 4 teselas). */
    private const val CELL_W = 40
    private const val CELL_H = 24

    /**
     * Hoja con TODOS los sprites del catálogo dibujados con sus GFX y su paleta reales, para
     * verlos y exportarlos. Fondo transparente. Null si la ROM no es SMW.
     */
    fun renderSheet(rom: ByteArray, header: SnesHeader): ArgbImage? {
        val delta = header.headerOffset - 0x7FC0
        val vram = SnesGameRecipes.overworldSpriteVram(rom, delta) ?: return null
        val cgram = SnesGameRecipes.overworldCgram(rom, delta, 0)
        val img = ArgbImage(CELL_W * CATALOG.size, CELL_H)
        CATALOG.forEachIndexed { i, def ->
            val ox = i * CELL_W + 4
            val oy = 8
            if (def.tilesLeftward > 1) {
                // Tira horizontal de 8×8 hacia la izquierda desde `tile`, como el cartel.
                for (n in 0 until def.tilesLeftward) {
                    drawTile(def.tile - n, ox + (def.tilesLeftward - 1 - n) * 8, oy, def.palette, vram, cgram, img)
                }
            } else if (def.size16) {
                // 16×16 de la SNES: teselas N, N+1, N+16, N+17.
                drawTile(def.tile, ox, oy, def.palette, vram, cgram, img)
                drawTile(def.tile + 1, ox + 8, oy, def.palette, vram, cgram, img)
                drawTile(def.tile + 16, ox, oy + 8, def.palette, vram, cgram, img)
                drawTile(def.tile + 17, ox + 8, oy + 8, def.palette, vram, cgram, img)
            } else {
                drawTile(def.tile, ox, oy, def.palette, vram, cgram, img)
            }
        }
        return img
    }

    /**
     * Cada sprite del mapa por separado (nombre → imagen [CELL_W]×[CELL_H] transparente), para
     * el catálogo de extracción. Es la [renderSheet] partida en su celda por sprite: así cada
     * uno (Boo, nube, piraña, cartel de Bowser…) sale a su propia carpeta. Null si no es SMW.
     */
    fun images(rom: ByteArray, header: SnesHeader): List<Pair<String, ArgbImage>>? {
        val sheet = renderSheet(rom, header) ?: return null
        return CATALOG.mapIndexed { i, def ->
            val cell = ArgbImage(CELL_W, CELL_H)
            for (y in 0 until CELL_H) for (x in 0 until CELL_W) {
                cell.set(x, y, sheet.get(i * CELL_W + x, y))
            }
            def.name to cell
        }
    }

    /** Dibuja una tesela OAM de 8×8 con su sub-paleta de sprite (CGRAM `128 + P*16`). */
    private fun drawTile(
        tile: Int, x: Int, y: Int, palette: Int,
        vram: Array<IntArray?>, cgram: IntArray, img: ArgbImage,
    ) {
        val px = vram.getOrNull(tile) ?: return
        val base = 128 + (palette and 7) * 16
        for (yy in 0..7) for (xx in 0..7) {
            val ci = px[yy * 8 + xx]
            if (ci == 0) continue
            val dx = x + xx; val dy = y + yy
            if (dx in 0 until img.width && dy in 0 until img.height) img.set(dx, dy, cgram[base + ci])
        }
    }
}
