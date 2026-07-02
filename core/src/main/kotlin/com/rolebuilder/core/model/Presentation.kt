package com.rolebuilder.core.model

/** Clima ambiental de un mapa (el renderer dibuja las partículas). */
enum class Weather { NONE, RAIN, SNOW }

/**
 * Pistas de música sintetizada disponibles. Es la lista canónica que
 * comparten el editor (selector de pista) y el runtime (sintetizador).
 * La cadena vacía "" significa silencio.
 */
object BgmTracks {
    val ALL = listOf("pueblo", "campo", "mazmorra", "tension")
}
