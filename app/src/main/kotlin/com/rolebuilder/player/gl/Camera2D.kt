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

    /**
     * Compresión vertical del diorama 2.5D: cos(inclinación). Con 1 no hay
     * efecto; con menos de 1 el semieje vertical del ortho se expande a
     * tilesVisibleY/squashY, de modo que se ven MÁS filas de mundo y cada
     * una ocupa squashY veces su alto normal en pantalla (el suelo se
     * "tumba" hacia atrás). El eje X no se toca.
     */
    var squashY = 1f

    /** Desplazamiento de temblor (en casillas), aplicado tras encuadrar. */
    var shakeX = 0f
    var shakeY = 0f

    var viewportWidth = 1
    var viewportHeight = 1

    val mvp = FloatArray(16)

    val viewTilesX: Float
        get() = tilesVisibleY * viewportWidth / viewportHeight.coerceAtLeast(1)

    /** Filas de mundo que caben en la vista con la compresión aplicada. */
    val viewTilesYWorld: Float
        get() = tilesVisibleY / squashY

    fun update(mapWidth: Int, mapHeight: Int) {
        val halfH = viewTilesYWorld / 2f
        val halfW = viewTilesX / 2f

        x = if (viewTilesX >= mapWidth) mapWidth / 2f else x.coerceIn(halfW, mapWidth - halfW)
        y = if (viewTilesYWorld >= mapHeight) mapHeight / 2f else y.coerceIn(halfH, mapHeight - halfH)

        // y del mundo crece hacia abajo: se invierte bottom/top.
        Matrix.orthoM(
            mvp, 0,
            x - halfW + shakeX, x + halfW + shakeX,
            y + halfH + shakeY, y - halfH + shakeY,
            -1f, 1f,
        )
    }
}
