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
}
