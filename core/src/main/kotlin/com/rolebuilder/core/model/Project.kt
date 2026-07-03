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
) {
    companion object {
        const val DEFAULT_SWITCH_COUNT = 50
        const val DEFAULT_VARIABLE_COUNT = 50
        const val MAX_MAP_SIZE = 200
    }
}
