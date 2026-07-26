package com.rolebuilder.core.snes

/**
 * La META de un nivel de Super Mario World.
 *
 * En SMW el final del nivel no es un bloque del terreno: es un **sprite**. Los tres que
 * terminan un nivel son la **cinta de meta** (`$7B`), la **esfera de meta** (`$4A`, la de las
 * salas de jefe y algunas salidas) y la **cerradura** (`$0E`, la salida secreta con llave).
 * Salen en la misma lista de sprites del nivel que los enemigos ([SmwSprites]), así que hasta
 * ahora el motor los sembraba como si fueran bichos y el nivel no se podía "superar".
 *
 * Verificado contra la ROM US: `YOSHI'S ISLAND 1` (0x105) y `YOSHI'S ISLAND 2` (0x106) llevan
 * la cinta en la casilla (302, 23) e `YOSHI'S ISLAND 3` (0x103) en la (318, 23) — el extremo
 * derecho a ras de suelo, que es donde está. En cambio `#1 IGGY'S CASTLE` (0x101) y
 * `YOSHI'S HOUSE` (0x104) **no llevan ninguno**, y es lo correcto: un castillo se acaba
 * derrotando al jefe y la casa no se acaba. Los niveles sin meta simplemente no siembran
 * ninguna, en vez de inventarse uno.
 */
object SmwLevelGoal {

    /** Cinta de meta: el final normal de un nivel. */
    const val GOAL_TAPE = 0x7B
    /** Esfera de meta: salas de jefe y algunas salidas. */
    const val GOAL_SPHERE = 0x4A
    /** Cerradura: la salida secreta, la que se abre con la llave. */
    const val KEYHOLE = 0x0E

    /** Los sprites que dan por SUPERADO el nivel. */
    val GOAL_SPRITES = setOf(GOAL_TAPE, GOAL_SPHERE, KEYHOLE)

    /**
     * Alto en casillas que se le da a la meta. La cinta de SMW es un poste ALTO: se cruza
     * tanto por abajo como saltando arriba, y el sprite guarda solo su base. Sembrar una sola
     * casilla dejaría pasar al jugador por encima sin acabar el nivel, así que se cubre la
     * columna. Es una aproximación deliberada del alcance del poste, no un dato de la ROM.
     */
    const val GOAL_HEIGHT_TILES = 5

    /** Una meta del nivel: su tipo de sprite y la casilla donde está su base. */
    data class Goal(val spriteId: Int, val xTile: Int, val yTile: Int)

    /**
     * Las metas del nivel [level], leídas de su lista de sprites. Lista vacía si el nivel no
     * tiene ninguna (castillos, casas, salas de paso).
     */
    fun goalsOf(rom: ByteArray, delta: Int, level: Int): List<Goal> =
        SmwSprites.parse(rom, delta, level)?.sprites
            ?.filter { it.id in GOAL_SPRITES }
            ?.map { Goal(it.id, it.xTile, it.yTile) }
            .orEmpty()

    /**
     * Las casillas que hay que marcar como meta para el motor: la columna de
     * [GOAL_HEIGHT_TILES] casillas que arranca en la base de cada meta y sube.
     */
    fun goalCells(rom: ByteArray, delta: Int, level: Int): List<Pair<Int, Int>> =
        goalsOf(rom, delta, level).flatMap { g ->
            (0 until GOAL_HEIGHT_TILES).map { g.xTile to (g.yTile - it) }
        }.filter { it.second >= 0 }
}
