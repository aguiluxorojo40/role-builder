package com.rolebuilder.player.gl

import android.opengl.Matrix
import kotlin.math.tan

/**
 * Cámara ortográfica en coordenadas de casilla (y hacia abajo).
 * Sigue al jugador y se limita a los bordes del mapa.
 *
 * Con [tiltDegrees] > 0 aplica una proyección keystone (trapecio): las filas
 * superiores (lejanas) se encogen, dando la inclinación de maqueta del
 * estilo HD-2D sin necesidad de un mundo 3D real.
 */
class Camera2D {

    /** Centro de la cámara, en casillas. */
    var x = 0f
    var y = 0f

    /** Casillas visibles en vertical. */
    var tilesVisibleY = 9f

    /** Inclinación del diorama, en grados (0 = vista cenital plana). */
    var tiltDegrees = 0f

    var viewportWidth = 1
    var viewportHeight = 1

    val mvp = FloatArray(16)

    private val ortho = FloatArray(16)
    private val keystone = FloatArray(16)

    /** Factor keystone aplicado (0 sin inclinación); útil para ampliar el culling. */
    var keystoneK = 0f
        private set

    val viewTilesX: Float
        get() = tilesVisibleY * viewportWidth / viewportHeight.coerceAtLeast(1)

    fun update(mapWidth: Int, mapHeight: Int) {
        val halfH = tilesVisibleY / 2f
        val halfW = viewTilesX / 2f

        x = if (viewTilesX >= mapWidth) mapWidth / 2f else x.coerceIn(halfW, mapWidth - halfW)
        y = if (tilesVisibleY >= mapHeight) mapHeight / 2f else y.coerceIn(halfH, mapHeight - halfH)

        // y del mundo crece hacia abajo: se invierte bottom/top.
        Matrix.orthoM(ortho, 0, x - halfW, x + halfW, y + halfH, y - halfH, -1f, 1f)

        keystoneK = if (tiltDegrees > 0.5f) {
            tan(Math.toRadians(tiltDegrees.toDouble())).toFloat().coerceIn(0f, 0.45f)
        } else {
            0f
        }
        if (keystoneK > 0f) {
            // w = 1 + k*y_ndc: la parte alta de la pantalla (y_ndc = +1, lejos)
            // se divide por más y se encoge; la baja (cerca) se acerca.
            Matrix.setIdentityM(keystone, 0)
            keystone[7] = keystoneK
            Matrix.multiplyMM(mvp, 0, keystone, 0, ortho, 0)
        } else {
            System.arraycopy(ortho, 0, mvp, 0, 16)
        }
    }
}
