package com.rolebuilder.core.snes

/**
 * Capa ESTÁTICA del overworld (mapa del mundo) de Super Mario World, leída directamente
 * de la ROM del usuario. No se versiona ni un byte con copyright: solo las COORDENADAS de
 * dónde está cada cosa en el ROM y cómo decodificarla; el usuario pone SU ROM y la app
 * extrae en el dispositivo (mismo principio que snesrev/smw).
 *
 * Primera fase: las tablas estáticas legibles SIN descomprimir el tilemap del mapa.
 *  - **Eventos por translevel** (`kLoadLevel_DATA_05D608`, $05:D608, 256 bytes): al superar
 *    el translevel N se dispara `eventos[N]` en el overworld, que revela caminos/niveles
 *    nuevos. `0xFF` = ese translevel no dispara evento. Los translevels 0x00-0x5F son los
 *    niveles del mapa. Es el motor de la PROGRESIÓN (abrir el siguiente nivel al ganar).
 *  - **Star Road** (`kOwStarPipeWarp_*`, banco $04): 27 warps de tubería origen→destino,
 *    en coordenadas de casilla del overworld (el bit alto de la coordenada indica submapa).
 *
 * Pendiente (siguiente fase, necesita el tilemap COMPRIMIDO del banco $0C): la tabla
 * casilla→nivel `ow_level_number_of_each_tiletbl` (la rellena `InitializeLevelData` $05:82C8
 * al cargar el mapa) y el Map16 del overworld (L1 en $05:D000). De ahí sale la LISTA DE
 * NIVELES JUGABLES reales (los que el mapa coloca) y el render navegable del mundo.
 *
 * Direcciones del inventario/desensamblado (snesrev/smw, `docs/overworld_smw.md`).
 * Todo Kotlin puro; los tests son SINTÉTICOS (sin ROM con copyright).
 */
object SmwOverworld {

    /** `kLoadLevel_DATA_05D608`: evento de overworld por translevel. */
    const val EVENTS_SNES = 0x05D608
    /** Nº de translevels que son niveles del mapa (0x00-0x5F). */
    const val MAP_TRANSLEVELS = 0x60
    /** Valor de "sin evento" en la tabla de eventos. */
    const val EVENT_NONE = 0xFF

    /**
     * `kLoadOverworldLayer1AndEvents_DATA_0CF7DF` ($0C:F7DF, 0x800 B, SIN comprimir):
     * la capa de casillas-de-nivel/eventos del overworld. El juego la copia tal cual
     * (`MemCpy` en `LoadOverworldLayer1AndEvents` $04:DC09) y detecta las casillas-de-nivel
     * en `04D7F2`: un byte es CASILLA-DE-NIVEL si su valor Map16 está en [0x56, 0x80].
     */
    const val LEVEL_TILES_SNES = 0x0CF7DF
    const val LEVEL_TILES_SIZE = 0x800
    const val LEVEL_TILE_MIN = 0x56
    const val LEVEL_TILE_MAX = 0x80

    /** `kOwStarPipeWarp_*` (banco $04): 27 warps de Star Road. */
    const val STAR_SRCX_SNES = 0x048431
    const val STAR_SRCY_SNES = 0x048467
    const val STAR_DSTX_SNES = 0x04849D
    const val STAR_DSTY_SNES = 0x0484D3
    const val STAR_WARP_COUNT = 27

    /**
     * `kMap16Data_OverworldLayer1` = **$05:D000**: definiciones Map16 del overworld (bloque
     * B → 8 bytes = 4 tile-words `vhopppcc cccccccc`; sub-teselas word0=TL, 1=BL, 2=TR, 3=BR).
     * Las usa la CAPA 1 interactiva (casillas-de-nivel con número, castillos, fortalezas,
     * caminos, casa de Yoshi, tuberías/Star Road) que se dibuja SOBRE la tierra (capa 2).
     */
    const val OW_MAP16_DEFS_SNES = 0x05D000

    /** SNES LoROM → offset PC, con [delta] = ajuste por cabecera de copiador (0 si headerless). */
    private fun pc(snes: Int, delta: Int): Int =
        (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta

    private fun u8(rom: ByteArray, snes: Int, delta: Int): Int =
        rom[pc(snes, delta)].toInt() and 0xFF

    private fun u16(rom: ByteArray, snes: Int, delta: Int): Int =
        u8(rom, snes, delta) or (u8(rom, snes + 1, delta) shl 8)

    /**
     * Evento de mapa por translevel (256 entradas). `eventos[N] == EVENT_NONE` significa
     * que superar ese translevel no revela nada; los translevels de nivel son 0x00..0x5F.
     */
    fun translevelEvents(rom: ByteArray, delta: Int): IntArray =
        IntArray(256) { u8(rom, EVENTS_SNES + it, delta) }

    /** Un warp de Star Road: casilla de ORIGEN y casilla de DESTINO del overworld. */
    data class StarWarp(val srcX: Int, val srcY: Int, val dstX: Int, val dstY: Int)

    /** Los 27 warps de la red de tuberías de Star Road (origen→destino en casillas). */
    fun starRoadWarps(rom: ByteArray, delta: Int): List<StarWarp> =
        (0 until STAR_WARP_COUNT).map { n ->
            StarWarp(
                srcX = u16(rom, STAR_SRCX_SNES + 2 * n, delta),
                srcY = u16(rom, STAR_SRCY_SNES + 2 * n, delta),
                dstX = u16(rom, STAR_DSTX_SNES + 2 * n, delta),
                dstY = u16(rom, STAR_DSTY_SNES + 2 * n, delta),
            )
        }

    /** Una casilla-de-nivel del overworld: su posición en la capa y su valor Map16. */
    data class LevelTile(val position: Int, val map16: Int)

    /**
     * Las CASILLAS-DE-NIVEL del overworld (los sitios donde entras a un nivel), leídas de
     * `$0C:F7DF` tal como hace el juego (`04D7F2`): bytes con Map16 en [0x56, 0x80]. En la
     * ROM US vanilla salen los ~92 niveles reales; excluye por construcción los slots de
     * test (que no son casillas en el mapa). Es la base del render del worldmap y del filtro
     * de "solo niveles jugables".
     */
    fun levelTiles(rom: ByteArray, delta: Int): List<LevelTile> {
        val out = ArrayList<LevelTile>()
        for (j in 0 until LEVEL_TILES_SIZE) {
            val v = u8(rom, LEVEL_TILES_SNES + j, delta)
            if (v in LEVEL_TILE_MIN..LEVEL_TILE_MAX) out.add(LevelTile(j, v))
        }
        return out
    }

    /**
     * Tilemap de LAYER 2 del overworld (el mapa VISIBLE: tierra, agua, decorados), RLE.
     * El puntero de 24 bits vive en `word($04:DC72) | byte($04:DC79) << 16`; el juego lo
     * expande en `BufferOverworldLayer2Tilemap` ($04:DABA). Formato RLE: un byte de control
     * `c`; si `c & 0x80` → run de `(c & 0x7F)+1` copias del siguiente byte; si no → `c+1`
     * literales. Salen 0x2000 índices Map16 (todos los submapas).
     */
    const val L2_PTR_LO_SNES = 0x04DC72   // word: los 16 bits bajos del puntero
    const val L2_PTR_BANK_SNES = 0x04DC79 // byte: el banco del puntero
    const val L2_TILE_COUNT = 0x2000

    /** Descomprime el tilemap L2 del overworld → 0x2000 índices Map16 del mapa visible. */
    fun layer2Tilemap(rom: ByteArray, delta: Int): IntArray {
        val ptr = u16(rom, L2_PTR_LO_SNES, delta) or (u8(rom, L2_PTR_BANK_SNES, delta) shl 16)
        return rleDecompress(rom, delta, ptr, L2_TILE_COUNT)
    }

    /**
     * Operando (word) del puntero de la tabla de PROPIEDADES del tilemap L2. Descubierto
     * en el código de `LoadOverworldLayer2AndEventsTilemaps_Sub` ($04:DC8D `LDA #$C02B`):
     * la segunda pasada de `BufferOverworldLayer2Tilemap` rellena los bytes IMPARES de
     * `ow_layer2_tiles` con esta tabla (banco reutilizado del puntero bajo, $04:DC79).
     * Cada entrada del tilemap del overworld NO es un índice Map16: es una casilla SNES
     * de 8×8 directa `vhopppcc cccccccc`, donde el byte alto (props) trae paleta y flips.
     */
    const val L2_PROP_PTR_LO_SNES = 0x04DC8D

    /**
     * El tilemap VISIBLE del overworld como **casillas SNES de 8×8 directas** (0x2000):
     * byte bajo = tabla de teselas ($04:A533), byte alto = tabla de propiedades ($04:C02B),
     * ambas RLE. `entrada = bajo | (alto << 8)`, formato `vhopppcc cccccccc`: tesela 10 bits,
     * paleta bits 10-12, flipX bit14, flipY bit15. A diferencia de los NIVELES, la capa de
     * tierra del overworld no pasa por Map16: se dibuja tesela a tesela. Es la base del
     * render del mapa del mundo (verificado contra la ROM US: sale el mapa principal, Vanilla
     * Dome, Star World y "SPECIAL" reconocibles con color real).
     */
    fun overworldTilemap(rom: ByteArray, delta: Int): IntArray {
        val loPtr = u16(rom, L2_PTR_LO_SNES, delta) or (u8(rom, L2_PTR_BANK_SNES, delta) shl 16)
        val hiPtr = u16(rom, L2_PROP_PTR_LO_SNES, delta) or (u8(rom, L2_PTR_BANK_SNES, delta) shl 16)
        val lo = rleDecompress(rom, delta, loPtr, L2_TILE_COUNT)
        val hi = rleDecompress(rom, delta, hiPtr, L2_TILE_COUNT)
        return IntArray(L2_TILE_COUNT) { lo[it] or (hi[it] shl 8) }
    }

    /**
     * RLE del overworld (`BufferOverworldLayer2Tilemap` $04:DABA): byte de control `c`;
     * si `c & 0x80` → run de `(c & 0x7F)+1` copias del byte siguiente; si no → `c+1`
     * literales. Devuelve [count] bytes.
     */
    private fun rleDecompress(rom: ByteArray, delta: Int, snesPtr: Int, count: Int): IntArray {
        val out = IntArray(count)
        var j = pc(snesPtr, delta)
        var n = 0
        while (n < out.size && j < rom.size - 1) {
            val c = rom[j].toInt() and 0xFF; j++
            if (c and 0x80 != 0) {
                val v = rom[j].toInt() and 0xFF; j++
                repeat((c and 0x7F) + 1) { if (n < out.size) out[n++] = v }
            } else {
                repeat(c + 1) { if (n < out.size && j < rom.size) out[n++] = rom[j++].toInt() and 0xFF }
            }
        }
        return out
    }

    // --- EVENTOS del overworld (los caminos que se REVELAN) ---------------------------
    // Al superar un nivel, su evento (ver [translevelEvents]) "parchea" el mapa visible:
    // aparecen caminos, se abren tuberías y emergen zonas nuevas (p. ej. la entrada al
    // Valle de Bowser saliendo del agua). El juego lo hace en `LoadOverworldLayer2AndEvents
    // Tilemaps_04E453` ($04:E453): por cada evento ACTIVO recorre sus "casillas de evento" y
    // pega en el tilemap un parche de 6×6 (o 2×2) tomado de una tabla de teselas + props.
    // Aplicando TODOS los eventos sale el mapa tal y como se ve al 100% del juego.

    /** `kLayer2EventData_Ptrs_04E359` ($04:E359, 121 words): rango de casillas por evento. */
    const val EVENT_PTRS_SNES = 0x04E359
    /** Nº de eventos del overworld en la ROM vanilla (0..110). */
    const val EVENT_COUNT = 111
    /** Puntero largo (24 bits) a `kLayer2EventData_TileEntries` (vanilla $04:DD8D). */
    const val EVENT_ENTRIES_PTR_SNES = 0x04E49F
    /** Puntero largo a `kOverworldLayer2EventTilemap_Tiles` (vanilla $0C:8000, 0xD00 B). */
    const val EVENT_TILES_PTR_SNES = 0x04EAF5
    /** Puntero (word+banco) a `kOverworldLayer2EventTilemap_Prop`, RLE (vanilla $0C:8D00). */
    const val EVENT_PROP_PTR_LO_SNES = 0x04DD45
    const val EVENT_PROP_PTR_BANK_SNES = 0x04DD4A
    /** Umbral de `j` que separa parches de 6×6 (abajo) de los de 2×2 (arriba). */
    private const val EVENT_6X6_LIMIT = 0x900
    /** Tamaño del buffer de casillas del overworld en BYTES (0x2000 casillas × 2). */
    private const val OW_TILE_BYTES = 0x4000

    private fun u24(rom: ByteArray, snes: Int, delta: Int): Int =
        u16(rom, snes, delta) or (u8(rom, snes + 2, delta) shl 16)

    /**
     * Buffer de PROPIEDADES de las casillas de evento (`ow_layer2_event_tiles`), RLE desde
     * `$0C:8D00` (`LoadOverworldLayer2AndEventsTilemaps_04DD57` $04:DD57). Mismo RLE que el
     * tilemap, pero escribiendo consecutivo y terminando en dos `0xFF` seguidos. En la ROM US
     * expande a 0xD00 bytes: exactamente el tamaño de la tabla de teselas (van emparejadas).
     */
    private fun eventPropBuffer(rom: ByteArray, delta: Int): IntArray {
        val ptr = u16(rom, EVENT_PROP_PTR_LO_SNES, delta) or
            (u8(rom, EVENT_PROP_PTR_BANK_SNES, delta) shl 16)
        val out = ArrayList<Int>(0x1000)
        var j = pc(ptr, delta)
        while (j < rom.size - 1 && out.size < 0x4000) {
            if ((rom[j].toInt() and 0xFF) == 0xFF && (rom[j + 1].toInt() and 0xFF) == 0xFF) break
            val c = rom[j].toInt() and 0xFF; j++
            if (c and 0x80 != 0) {
                val v = rom[j].toInt() and 0xFF; j++
                repeat((c and 0x7F) + 1) { out.add(v) }
            } else {
                repeat(c + 1) { if (j < rom.size) out.add(rom[j++].toInt() and 0xFF) }
            }
        }
        return out.toIntArray()
    }

    /**
     * El tilemap del overworld **con los eventos aplicados**: el mapa tal y como se ve tras
     * superar los primeros [eventCount] eventos (por defecto TODOS → mapa al 100%, con todos
     * los caminos abiertos y la entrada al Valle de Bowser emergida). Con `eventCount = 0`
     * devuelve el mapa de partida nueva. Port de `LoadOverworldLayer2AndEventsTilemaps_04E453`
     * + `BufferEventTileToLayer2Tilemap` ($04:E496).
     */
    fun overworldTilemapWithEvents(rom: ByteArray, delta: Int, eventCount: Int = EVENT_COUNT): IntArray {
        val n = eventCount.coerceIn(0, EVENT_COUNT)
        return overworldTilemapWithEvents(rom, delta) { it < n }
    }

    /**
     * Igual que el anterior pero con los eventos ACTIVOS a la carta: [isActive] decide, evento
     * a evento, si está disparado. Es como funciona el juego de verdad —lleva un banderín por
     * evento (`ow_event_flags`), no un contador—, así que esta es la forma fiel de dibujar la
     * progresión real de una partida: el jugador supera niveles sueltos y se activan SUS
     * eventos, no un prefijo.
     */
    fun overworldTilemapWithEvents(rom: ByteArray, delta: Int, isActive: (Int) -> Boolean): IntArray {
        val base = overworldTilemap(rom, delta)
        if ((0 until EVENT_COUNT).none(isActive)) return base
        // Buffer intercalado como en la RAM del juego: [2i] = tesela, [2i+1] = props.
        val buf = IntArray(OW_TILE_BYTES)
        for (i in base.indices) { buf[2 * i] = base[i] and 0xFF; buf[2 * i + 1] = (base[i] shr 8) and 0xFF }

        val entriesSnes = u24(rom, EVENT_ENTRIES_PTR_SNES, delta)
        val tilesPc = pc(u24(rom, EVENT_TILES_PTR_SNES, delta), delta)
        val prop = eventPropBuffer(rom, delta)

        for (ev in 0 until EVENT_COUNT) {
            if (!isActive(ev)) continue
            val start = u16(rom, EVENT_PTRS_SNES + 2 * ev, delta)
            val end = u16(rom, EVENT_PTRS_SNES + 2 * (ev + 1), delta)
            for (i in start until end) {
                // TileEntries[2i] = índice de origen; TileEntries[2i+1] = destino (en bytes).
                val src = u16(rom, entriesSnes + 4 * i, delta)
                val dst = u16(rom, entriesSnes + 4 * i + 2, delta)
                val side = if (src < EVENT_6X6_LIMIT) 6 else 2
                var j = src
                var r4 = dst
                repeat(side) {
                    var v2 = r4
                    repeat(side) {
                        // Cada casilla = tesela (tabla en ROM) + props (buffer RLE).
                        if (v2 + 1 < buf.size && tilesPc + j < rom.size) {
                            buf[v2] = rom[tilesPc + j].toInt() and 0xFF
                            buf[v2 + 1] = if (j < prop.size) prop[j] else 0
                        }
                        v2 += 2; j++
                        // Borde derecho de la pantalla → misma fila de la pantalla siguiente.
                        if (v2 and 0x3F == 0) v2 = ((v2 - 1) and 0xFFC0) + 0x800
                    }
                    val v4 = r4
                    r4 = (r4 + 64) and 0xFFFF
                    // Borde inferior → fila 0 de la pantalla de abajo (el mapa es 2 pantallas
                    // de ancho, por eso +0x1000 bytes = 2 pantallas).
                    if (r4 and 0x7C0 == 0) r4 = (v4 and 0xF83F) + 0x1000
                }
            }
        }
        return IntArray(base.size) { buf[2 * it] or (buf[2 * it + 1] shl 8) }
    }

    // --- Geometría del overworld (verificada por render contra la ROM US) --------------
    /** Nº de "pantallas" (regiones de 32×32 teselas = 256×256 px) en el tilemap. */
    const val OW_SCREENS = 8
    /** Lado de una pantalla en teselas de 8×8. */
    const val OW_SCREEN_SIDE = 32
    /** Teselas por pantalla (32×32). */
    const val OW_SCREEN_TILES = OW_SCREEN_SIDE * OW_SCREEN_SIDE
    /**
     * El **mapa principal** ocupa las pantallas 0-3 dispuestas en 2×2 (512×512 px). Las
     * pantallas 4-7 son los submapas (Vanilla Dome, Valley, Bosque/SPECIAL, Star World…),
     * cada uno con su propia paleta de área. Orden de sub-pantallas del mapa 2×2:
     * 0=arriba-izq, 1=arriba-der, 2=abajo-izq, 3=abajo-der.
     */
    val OW_MAIN_MAP_SCREENS = intArrayOf(0, 1, 2, 3)
    const val OW_MAIN_MAP_COLS = 2

    /**
     * El **área de submapas**: las pantallas 4-7, también en 2×2 (512×512). Ojo: NO son
     * cuatro submapas sueltos. Los **6 submapas** del juego son seis VENTANAS de cámara
     * (256×224, el tamaño de pantalla de la SNES) recortadas sobre esa área, colocadas en
     * 2 columnas × 3 filas y solapándose entre sí. Las esquinas de cámara están en
     * `kOwExitLayerPosition_049A0C` ($04:9A0C, 6 pares x,y con signo), que es justo lo que
     * usa `HandleOverworldPathExits_SetLayerPositions` ($04:9A93) al cambiar de submapa.
     */
    val OW_SUBMAP_AREA_SCREENS = intArrayOf(4, 5, 6, 7)
    const val OW_SUBMAP_AREA_COLS = 2
    /** Nº de submapas reales del overworld. */
    const val SUBMAP_COUNT = 6
    /** `kOwExitLayerPosition_049A0C` ($04:9A0C): 6 pares (x, y) de esquina de cámara. */
    const val SUBMAP_CAMERA_SNES = 0x049A0C
    /** Ventana visible de la SNES en píxeles (lo que se ve de un submapa de golpe). */
    const val OW_VIEW_WIDTH = 256
    const val OW_VIEW_HEIGHT = 224

    /**
     * Esquina (x, y) de la cámara del [submap] (1..6) dentro del área de submapas, leída de
     * la ROM. Los valores son de 16 bits CON SIGNO (p. ej. `0xFFEF` = -17), así que parte de
     * la ventana puede caer fuera del área — igual que en el juego.
     */
    fun submapCamera(rom: ByteArray, delta: Int, submap: Int): Pair<Int, Int> {
        val i = (submap.coerceIn(1, SUBMAP_COUNT) - 1) * 2
        fun signed(v: Int) = if (v >= 0x8000) v - 0x10000 else v
        return signed(u16(rom, SUBMAP_CAMERA_SNES + 2 * i, delta)) to
            signed(u16(rom, SUBMAP_CAMERA_SNES + 2 * (i + 1), delta))
    }

    /**
     * Área de paleta del overworld por número de submapa (`kBufferPalettesRoutines_DATA_00AD1E`,
     * $00:AD1E). El submapa 0 = mapa principal → área 1. Se usa como `tt` para leer
     * `kGlobalPalettes_OW_Areas` ($00:B3D8) al montar la CGRAM del overworld.
     */
    val OW_AREA_BY_SUBMAP = intArrayOf(1, 0, 3, 4, 3, 5, 2)

    // --- SPRITES del mapa -------------------------------------------------------------
    /**
     * `kLoadOverworldSprites_SpriteSlotData` ($04:F625, 65 bytes): 13 ranuras de 5 bytes
     * `(id, x:16, y:16)` con los sprites ESTÁTICOS del overworld que carga
     * `LoadOverworldSprites` ($04:F675). Sus coordenadas son del **área de submapas**
     * (Bowser y su cartel en el Valle, los Boos…). Los decorativos (nubes, pájaros, Lakitu,
     * Cheep-Cheeps) no salen de aquí: los genera el juego al vuelo.
     */
    const val SPRITE_SLOT_DATA_SNES = 0x04F625
    const val SPRITE_SLOT_COUNT = 13

    /** Tipos de sprite del overworld (índice de `kProcessOverworldSprites_OverworldSpritePtrs`). */
    const val SPRITE_NONE = 0x00
    const val SPRITE_LAKITU = 0x01
    const val SPRITE_BLUE_BIRD = 0x02
    const val SPRITE_CHEEP_CHEEP = 0x03
    const val SPRITE_PIRANHA = 0x04
    const val SPRITE_CLOUD = 0x05
    const val SPRITE_KOOPA_KID = 0x06
    const val SPRITE_SMOKE = 0x07
    const val SPRITE_BOWSER_SIGN = 0x08
    const val SPRITE_BOWSER = 0x09
    const val SPRITE_BOO = 0x0A

    /** Un sprite estático del mapa: tipo y posición en el área de submapas. */
    data class MapSprite(val id: Int, val x: Int, val y: Int)

    /** Los 13 sprites estáticos del overworld, con su tipo y posición. */
    fun mapSprites(rom: ByteArray, delta: Int): List<MapSprite> =
        (0 until SPRITE_SLOT_COUNT).map { i ->
            val b = SPRITE_SLOT_DATA_SNES + i * 5
            fun signed(v: Int) = if (v >= 0x8000) v - 0x10000 else v
            MapSprite(
                id = u8(rom, b, delta),
                x = signed(u16(rom, b + 1, delta)),
                y = signed(u16(rom, b + 3, delta)),
            )
        }

    /** Un nivel JUGABLE con su nombre real de overworld. */
    data class NamedLevel(val translevel: Int, val name: String)

    /**
     * Lista de NIVELES JUGABLES reales: los translevels 0x00..0x5F que tienen NOMBRE de
     * overworld en la ROM (`SmwLevelNames`). Es la respuesta directa a "solo los niveles que
     * son jugables en el juego": excluye los slots de test/utilidad (sin nombre) y da el
     * nombre real de cada uno (p. ej. 0x29 → "YOSHI'S ISLAND 1"). ROM-derivado, sin listas a
     * mano. (Enlazar cada nivel con SU casilla en el mapa y su nº de datos de nivel es la
     * siguiente fase.)
     */
    fun namedLevels(rom: ByteArray, delta: Int): List<NamedLevel> =
        (0..MAP_TRANSLEVELS - 1).mapNotNull { tl ->
            SmwLevelNames.nameOfTranslevel(rom, delta, tl)?.let { NamedLevel(tl, it) }
        }
}
