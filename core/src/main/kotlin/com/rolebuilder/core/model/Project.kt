package com.rolebuilder.core.model

import kotlinx.serialization.Serializable

/**
 * Metadatos de un proyecto RPG. Un proyecto es una carpeta con:
 *   project.json, database.json, maps/map_<id>.json e imágenes PNG en images/
 */
@Serializable
data class Project(
    val name: String,
    val startMapId: Int = 1,
    val startX: Int = 5,
    val startY: Int = 5,
    val playerActorId: Int = 1,
    /** Nombres visibles en el editor; el índice+1 es el id del switch/variable. */
    val switchNames: List<String> = List(DEFAULT_SWITCH_COUNT) { "" },
    val variableNames: List<String> = List(DEFAULT_VARIABLE_COUNT) { "" },
    /** Mapas del proyecto en el orden que muestra el editor. */
    val mapIds: List<Int> = listOf(1),
    /**
     * Estilo visual "HD-2D": post-procesado con desenfoque tilt-shift
     * (efecto maqueta), bloom, viñeta y etalonaje cálido, más sombras
     * suaves y motas de luz ambientales en el runtime.
     */
    val hd2d: Boolean = true,
    /**
     * Intensidad del estilo HD-2D, de 0 (imperceptible) a 2 (exagerado);
     * 1 = normal. Escala a la vez el tilt-shift, el bloom, la viñeta, el
     * etalonaje, las sombras y las motas de luz.
     */
    val hd2dStrength: Float = 1f,
    /**
     * Inclinación del diorama 2.5D en grados: el runtime inclina el plano
     * del suelo hacia atrás (efecto maqueta HD-2D estilo Octopath Traveler)
     * y dibuja de pie a los personajes y a los tiles marcados en
     * [Tileset.standingTiles]. 0 = desactivado (plano cenital clásico);
     * es la inclinación REAL del plano, rango 0..60°. Octopath ronda
     * 40-50°; 45 es el valor por defecto del "teatrillo".
     */
    val dioramaTilt: Float = 45f,
    /**
     * Altura extra de los sprites de pie (personajes, enemigos y tiles
     * "de pie") en el diorama 2.5D: multiplica su alto de dibujo sobre la
     * proporción natural del arte. 1 = tal cual el arte; >1 los estira
     * hacia arriba para exagerar cuánto se levantan del suelo. Rango
     * razonable 0.5..2.5.
     */
    val spriteStand: Float = 1f,
) {
    companion object {
        const val DEFAULT_SWITCH_COUNT = 50
        const val DEFAULT_VARIABLE_COUNT = 50
        const val MAX_MAP_SIZE = 200
    }
}
