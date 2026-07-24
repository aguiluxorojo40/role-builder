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
        val out = IntArray(L2_TILE_COUNT)
        var j = pc(ptr, delta)
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
