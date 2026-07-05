package com.rolebuilder.player.gl

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Cámara en perspectiva 3D para el HD-2D real. El mundo del juego es 2D por
 * casillas (x, y) con y hacia el sur; aquí se interpreta como un plano de
 * suelo en 3D: la X del juego es la X del mundo y la Y del juego (sur) es la
 * Z (profundidad). El eje Y del mundo es la altura, por el que se levantan
 * los billboards (personajes, árboles, casas).
 *
 * La cámara se sitúa por encima y al sur del jugador mirándolo con un ángulo
 * de picado; la perspectiva REAL hace que lo cercano se vea grande y lo
 * lejano pequeño, sin estirar nada.
 */
class Camera3D {

    /** Punto que la cámara sigue, en casillas (x, y-sur). */
    var x = 0f
    var y = 0f

    /** Casillas que se quieren ver en vertical (controla el zoom). */
    var tilesVisibleY = 10f

    /**
     * Picado de la cámara en grados: 0 = cenital puro (mirando recto hacia
     * abajo), 90 = a ras de suelo. ~45-55 da el diorama de Octopath.
     */
    var pitchDegrees = 50f

    /** Vibración (ShakeScreen): desplazamiento del objetivo, en casillas. */
    var shakeX = 0f
    var shakeY = 0f

    var viewportWidth = 1
    var viewportHeight = 1

    /** View-projection para el plano del suelo y los billboards. */
    val vp = FloatArray(16)

    /** Ejes de la cámara en el mundo (para orientar los billboards a cámara). */
    val right = FloatArray(3)
    val up = FloatArray(3)
    val eye = FloatArray(3)

    private val view = FloatArray(16)
    private val proj = FloatArray(16)

    private val fovYDeg = 40f

    fun update(mapWidth: Int, mapHeight: Int) {
        val aspect = viewportWidth.toFloat() / viewportHeight.coerceAtLeast(1)
        val fovY = Math.toRadians(fovYDeg.toDouble())

        // Distancia para que entren ~tilesVisibleY casillas en vertical.
        val dist = (tilesVisibleY / 2f) / tan(fovY / 2.0).toFloat()

        val half = tilesVisibleY / 2f
        val cx = (x + shakeX).coerceIn(half, (mapWidth - half).coerceAtLeast(half))
        val cz = (y + shakeY).coerceIn(half, (mapHeight - half).coerceAtLeast(half))

        // Elevación sobre la horizontal (pitch 50 -> 40° sobre el suelo).
        val elev = Math.toRadians((90f - pitchDegrees).coerceIn(20f, 89f).toDouble())
        val h = dist * sin(elev).toFloat()
        val back = dist * cos(elev).toFloat()

        val ex = cx
        val ey = h
        val ez = cz + back
        eye[0] = ex; eye[1] = ey; eye[2] = ez

        Matrix.setLookAtM(view, 0, ex, ey, ez, cx, 0f, cz, 0f, 1f, 0f)
        Matrix.perspectiveM(proj, 0, fovYDeg, aspect, 0.1f, 400f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        // Ejes de cámara en el mundo = filas de la rotación de la view.
        right[0] = view[0]; right[1] = view[4]; right[2] = view[8]
        up[0] = view[1]; up[1] = view[5]; up[2] = view[9]
    }
}
