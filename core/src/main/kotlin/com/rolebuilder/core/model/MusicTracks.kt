package com.rolebuilder.core.model

/**
 * Identificadores de las pistas de música de fondo integradas en el motor.
 * Los usan GameMap.bgm y el comando PlayMusic; la app los traduce a audio real.
 */
object MusicTracks {
    const val TITLE = "title"
    const val FIELD = "field"
    const val VILLAGE = "village"
    const val DUNGEON = "dungeon"
    const val BATTLE = "battle"

    /** Todas las pistas disponibles, para los selectores del editor. */
    val ALL = listOf(TITLE, FIELD, VILLAGE, DUNGEON, BATTLE)
}
