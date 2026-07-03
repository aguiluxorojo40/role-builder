package com.rolebuilder.player.gl

import android.opengl.Matrix

/**
 * Cámara ortográfica en coordenadas de casilla (y hacia abajo).
 * Sigue al jugador y se limita a los bordes del mapa.
 */
class Camera2D {

    /** Centro de la cámara, en casillas. */
    var x = 0f
    var y = 0f

    /** Casillas visibles en vertical. */
    var tilesVisibleY = 9f

    /** Desplazamiento de temblor (en casillas), aplicado tras encuadrar. */
    var shakeX = 0f
    var shakeY = 0f

    var viewportWidth = 1
    var viewportHeight = 1

    val mvp = FloatArray(16)

    val viewTilesX: Float
        get() = tilesVisibleY * viewportWidth / viewportHeight.coerceAtLeast(1)

    fun update(mapWidth: Int, mapHeight: Int) {
        val halfH = tilesVisibleY / 2f
        val halfW = viewTilesX / 2f

        x = if (viewTilesX >= mapWidth) mapWidth / 2f else x.coerceIn(halfW, mapWidth - halfW)
        y = if (tilesVisibleY >= mapHeight) mapHeight / 2f else y.coerceIn(halfH, mapHeight - halfH)

        // y del mundo crece hacia abajo: se invierte bottom/top.
        Matrix.orthoM(
            mvp, 0,
            x - halfW + shakeX, x + halfW + shakeX,
            y + halfH + shakeY, y - halfH + shakeY,
            -1f, 1f,
        )
    }
}
