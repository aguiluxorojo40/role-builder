package com.rolebuilder.player.gl

import android.opengl.Matrix
import kotlin.math.cos
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

    /** Vibración de pantalla (ShakeScreen): desplazamiento del centro, en casillas. */
    var shakeX = 0f
    var shakeY = 0f

    var viewportWidth = 1
    var viewportHeight = 1

    val mvp = FloatArray(16)

    private val ortho = FloatArray(16)
    private val keystone = FloatArray(16)

    /** Factor keystone aplicado (0 sin inclinación); útil para ampliar el culling. */
    var keystoneK = 0f
        private set

    /**
     * Compresión vertical del plano del suelo por la inclinación (1 = sin
     * inclinar). El terreno se dibuja "tumbado" (cada fila más chata y se ven
     * más filas), y el renderer compensa los sprites de pie dibujándolos más
     * altos (1/groundSquash) anclados por la base, para que se levanten del
     * suelo en su propio eje en lugar de quedar pegados a él (efecto modo 7).
     */
    var groundSquash = 1f
        private set

    val viewTilesX: Float
        get() = tilesVisibleY * viewportWidth / viewportHeight.coerceAtLeast(1)

    /** Media altura visible del mundo en casillas (crece al inclinar). */
    val halfViewY: Float
        get() = (tilesVisibleY / groundSquash) / 2f

    fun update(mapWidth: Int, mapHeight: Int) {
        groundSquash = if (tiltDegrees > 0.5f) {
            // [tiltDegrees] es la inclinación REAL del plano del suelo: a 45°
            // el terreno proyecta cos(45°) ≈ 0.71 de su alto, a 60° la mitad.
            // Octopath ronda 40-50°. Se acota para no degenerar más allá de ~70°.
            cos(Math.toRadians(tiltDegrees.toDouble())).toFloat().coerceIn(0.34f, 1f)
        } else {
            1f
        }

        val halfH = halfViewY
        val halfW = viewTilesX / 2f

        x = if (viewTilesX >= mapWidth) mapWidth / 2f else x.coerceIn(halfW, mapWidth - halfW)
        y = if (halfH * 2f >= mapHeight) mapHeight / 2f else y.coerceIn(halfH, mapHeight - halfH)

        // y del mundo crece hacia abajo: se invierte bottom/top.
        val cx = x + shakeX
        val cy = y + shakeY
        Matrix.orthoM(ortho, 0, cx - halfW, cx + halfW, cy + halfH, cy - halfH, -1f, 1f)

        keystoneK = if (tiltDegrees > 0.5f) {
            // Perspectiva del trapecio: crece con el ángulo pero de forma
            // suave (media pendiente) para no saturar antes de tiempo.
            tan(Math.toRadians((tiltDegrees * 0.5f).toDouble())).toFloat().coerceIn(0f, 0.6f)
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
