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
            // El mando 0..25° se amplifica a un cabeceo de cámara efectivo
            // 0..~55° para que el tumbado del suelo sea bien visible.
            cos(Math.toRadians((tiltDegrees * 2.2f).toDouble())).toFloat().coerceIn(0.45f, 1f)
        } else {
            1f
        }

        val halfH = halfViewY
        val halfW = viewTilesX / 2f

        x = if (viewTilesX >= mapWidth) mapWidth / 2f else x.coerceIn(halfW, mapWidth - halfW)
        y = if (halfH * 2f >= mapHeight) mapHeight / 2f else y.coerceIn(halfH, mapHeight - halfH)

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
