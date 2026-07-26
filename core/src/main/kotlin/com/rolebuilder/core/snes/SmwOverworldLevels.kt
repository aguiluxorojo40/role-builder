package com.rolebuilder.core.snes

/**
 * Enlaza cada CASILLA-DE-NIVEL del overworld con SU nivel: número, nombre real y posición en
 * el mapa. Es la pieza que faltaba para que el mapa del mundo sea NAVEGABLE (saber a qué
 * nivel entras al pulsar una casilla) y para etiquetarlo.
 *
 * Port 1:1 de `LoadOverworldLayer1AndEvents_04D7F2` ($04:D7F2). En la ROM vanilla el juego
 * NO guarda una tabla casilla→nivel: la **construye al vuelo** recorriendo la capa 1
 * (`$0C:F7DF`, 0x800 bytes) de principio a fin y numerando las casillas-de-nivel (Map16 en
 * `[0x56, 0x80]`) de forma correlativa **empezando en 1**:
 *
 * ```c
 * uint8 r0 = 1;
 * for (uint16 j = 0; j != 0x800; ++j) {
 *   uint8 v3 = r4[j];                       // capa 1
 *   if (v3 >= 0x56 && v3 < 0x81) {
 *     r13[j] = r0;                          // ow_level_number_of_each_tiletbl
 *     r10[j] = kOwDirectionAfterBeatingLevel_04D678[r0];
 *     ++r0;
 *   }
 * }
 * ```
 *
 * Ese número ES el índice de la tabla de nombres: el juego rotula la casilla con
 * `UpdateLevelName(kLevelNames[ow_level_number_of_each_tiletbl[pos]])` ($04:96xx), o sea el
 * mismo índice que usa [SmwLevelNames]. Así que **número de nivel = "translevel"** a efectos
 * de nombre, y de ahí sale el nombre real de cada casilla del mapa.
 *
 * (En ROMs modificadas con Lunar Magic la tabla viene precalculada y comprimida en vez de
 * generarse; aquí se implementa el camino vanilla. Que la ROM es vanilla se comprueba en el
 * juego mirando si `$04:D813` es `0x5C` —un JML de Lunar Magic—; en la ROM US original vale
 * `0x85`, así que se usa la numeración correlativa de arriba.)
 *
 * ## PENDIENTE DE VERIFICAR (no dar por bueno sin comprobarlo)
 * El port del ALGORITMO es 1:1 y los nombres que salen son reales y coherentes entre sí
 * ("VANILLA SECRET 2", "DONUT GHOST HOUSE", "DONUT PLAINS 3"…). Lo que **falta confirmar
 * visualmente** es la correspondencia POSICIÓN↔NOMBRE: al mirar dónde cae cada casilla, el
 * nivel nº 0x29 ("YOSHI'S ISLAND 1") aparece en las pantallas 4-7 (área de submapas) y no en
 * el mapa principal, que es donde uno esperaría Yoshi's Island. Puede ser que la intuición
 * sobre la geografía del mapa esté equivocada, o que las pantallas de la CAPA 1 no vayan en
 * el mismo orden que las del tilemap visible. Hasta comprobarlo dibujando marcadores sobre el
 * render y mirándolo, **no usar [OwLevel.x]/[OwLevel.y] como verdad** para colocar cosas en el
 * mapa; el número y el nombre sí salen del mismo sitio que los usa el juego.
 */
object SmwOverworldLevels {

    /**
     * `kOwDirectionAfterBeatingLevel_04D678` ($04:D678, 113 bytes): por número de nivel, las
     * DIRECCIONES de camino que se abren al superarlo. Es lo que el juego copia a
     * `ow_level_direction_flags` y consulta al mover a Mario por el mapa.
     */
    const val DIRECTIONS_SNES = 0x04D678

    /** Casillas de la capa 1 (16×16 bloques por pantalla, 8 pantallas). */
    private const val TILES_PER_SCREEN = 0x100
    private const val SCREEN_COLS = 16

    /** Una casilla-de-nivel del overworld, ya resuelta a nivel concreto. */
    data class OwLevel(
        /** Índice en la capa 1 (0..0x7FF). */
        val position: Int,
        /** Nº de nivel correlativo (1..92 en la ROM US) = índice de la tabla de nombres. */
        val levelNumber: Int,
        /** Nombre real del nivel, o null si esa entrada no tiene nombre. */
        val name: String?,
        /** Pantalla del tilemap (0-3 = mapa principal, 4-7 = área de submapas). */
        val screen: Int,
        /** Columna y fila del bloque dentro de su pantalla (0..15). */
        val col: Int,
        val row: Int,
        /**
         * Posición en PÍXELES dentro del mapa de 512×512 al que pertenece la casilla
         * (el principal o el área de submapas), lista para dibujar encima del render.
         */
        val x: Int,
        val y: Int,
        /** true si está en el mapa principal (pantallas 0-3); false si en el área de submapas. */
        val onMainMap: Boolean,
        /** Byte de direcciones de camino que se abren al superarlo (tabla $04:D678). */
        val pathDirections: Int,
    )

    private fun pc(snes: Int, delta: Int): Int =
        (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta

    private fun u8(rom: ByteArray, snes: Int, delta: Int): Int {
        val i = pc(snes, delta)
        return if (i in rom.indices) rom[i].toInt() and 0xFF else 0
    }

    /**
     * Todas las casillas-de-nivel del overworld con su nivel, nombre y posición. En la ROM US
     * vanilla salen **92**, en el mismo orden en que las numera el juego.
     */
    fun levels(rom: ByteArray, delta: Int): List<OwLevel> {
        val out = ArrayList<OwLevel>()
        var levelNumber = 1
        for (j in 0 until SmwOverworld.LEVEL_TILES_SIZE) {
            val v = u8(rom, SmwOverworld.LEVEL_TILES_SNES + j, delta)
            if (v < SmwOverworld.LEVEL_TILE_MIN || v > SmwOverworld.LEVEL_TILE_MAX) continue
            val screen = j / TILES_PER_SCREEN
            val inScreen = j % TILES_PER_SCREEN
            val col = inScreen % SCREEN_COLS
            val row = inScreen / SCREEN_COLS
            val onMain = screen < SmwOverworld.OW_MAIN_MAP_SCREENS.size
            // Las pantallas van en 2×2 dentro de su mapa (principal o área de submapas).
            val local = if (onMain) screen else screen - SmwOverworld.OW_MAIN_MAP_SCREENS.size
            val cols = SmwOverworld.OW_MAIN_MAP_COLS
            out.add(
                OwLevel(
                    position = j,
                    levelNumber = levelNumber,
                    name = SmwLevelNames.nameOfTranslevel(rom, delta, levelNumber),
                    screen = screen,
                    col = col,
                    row = row,
                    x = (local % cols) * 256 + col * 16,
                    y = (local / cols) * 256 + row * 16,
                    onMainMap = onMain,
                    pathDirections = u8(rom, DIRECTIONS_SNES + levelNumber, delta),
                )
            )
            levelNumber++
        }
        return out
    }

    /** Las casillas-de-nivel del **mapa principal** (para dibujarlas sobre su render 512×512). */
    fun mainMapLevels(rom: ByteArray, delta: Int): List<OwLevel> =
        levels(rom, delta).filter { it.onMainMap }

    /** Las casillas-de-nivel del **área de submapas**. */
    fun submapLevels(rom: ByteArray, delta: Int): List<OwLevel> =
        levels(rom, delta).filter { !it.onMainMap }
}
