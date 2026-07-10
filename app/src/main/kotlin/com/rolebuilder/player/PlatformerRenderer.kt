package com.rolebuilder.player

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.player.gl.Camera2D
import com.rolebuilder.player.gl.SpriteBatch
import com.rolebuilder.player.gl.Texture
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Renderer del motor de plataformas: avanza el motor a 60 fps fijos (como SMW) y
 * dibuja el nivel y a Mario con el mismo [SpriteBatch]/[Camera2D] que el RPG.
 *
 * De momento pinta la COLISIÓN por colores (suelo de un sentido, sólido, cuesta,
 * pinchos) sobre un cielo azul, y a Mario como un rectángulo — lo justo para VER y
 * JUGAR lo extraído. Encima se pueden montar luego los gráficos Map16 reales.
 *
 * La app escribe el input en [inMoveX]/[inRunning]/[inJumpHeld] desde otro hilo; el
 * hilo GL los aplica antes de cada tick.
 */
class PlatformerRenderer(private val engine: PlatformerEngine) : GLSurfaceView.Renderer {

    @Volatile var inMoveX = 0f
    @Volatile var inRunning = false
    @Volatile var inJumpHeld = false

    /** Lo lee la UI para el aviso de "has muerto". */
    @Volatile var dead = false
        private set

    private lateinit var batch: SpriteBatch
    private lateinit var white: Texture
    private val camera = Camera2D()
    private var lastNanos = 0L
    private var acc = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        batch = SpriteBatch()
        white = Texture.white()
        camera.tilesVisibleY = 15f
        lastNanos = 0L
        acc = 0f
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        camera.viewportWidth = width
        camera.viewportHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastNanos == 0L) 1f / 60f else ((now - lastNanos) / 1e9f).coerceAtMost(0.1f)
        lastNanos = now

        // Avanza en pasos fijos de 1/60 s para respetar el tacto de SMW.
        val step = 1f / 60f
        acc += dt
        var guard = 0
        while (acc >= step && guard < 8) {
            engine.moveX = inMoveX
            engine.running = inRunning
            engine.setJumpHeld(inJumpHeld)
            engine.tick()
            acc -= step
            guard++
        }
        dead = engine.player.dead

        // Cámara en casillas (16 px = 1 casilla), centrada en Mario.
        camera.x = engine.player.x / 16f
        camera.y = engine.player.y / 16f
        camera.update(engine.cols, engine.rows)

        GLES30.glClearColor(0.42f, 0.62f, 1.0f, 1f) // cielo
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        batch.begin(camera.mvp)

        // Solo las celdas visibles.
        val halfX = camera.viewTilesX / 2f + 1f
        val halfY = camera.halfViewY + 1f
        val minC = (camera.x - halfX).toInt().coerceAtLeast(0)
        val maxC = (camera.x + halfX).toInt().coerceAtMost(engine.cols - 1)
        val minR = (camera.y - halfY).toInt().coerceAtLeast(0)
        val maxR = (camera.y + halfY).toInt().coerceAtMost(engine.rows - 1)
        for (r in minR..maxR) {
            for (c in minC..maxC) {
                val s = engine.solidity(c, r)
                if (s == SmwSolidity.NONE) continue
                val col = colorOf(s)
                batch.draw(white, c.toFloat(), r.toFloat(), 1f, 1f, r = col[0], g = col[1], b = col[2], a = 1f)
            }
        }

        // Mario (rectángulo por ahora), en casillas.
        val w = engine.tuning.playerWidth / 16f
        val h = engine.tuning.playerHeight / 16f
        val mx = engine.player.x / 16f
        val my = engine.player.y / 16f
        val red = if (engine.player.dead) 0.4f else 1f
        batch.draw(white, mx, my, w, h, r = red, g = 0.25f, b = 0.2f, a = 1f)

        batch.end()
    }

    private fun colorOf(s: SmwSolidity): FloatArray = when (s) {
        SmwSolidity.LEDGE_TOP -> floatArrayOf(0.50f, 0.82f, 0.50f)   // verde: un sentido
        SmwSolidity.SOLID -> floatArrayOf(0.55f, 0.36f, 0.18f)       // marrón: sólido
        SmwSolidity.SLOPE, SmwSolidity.SLOPE_STEEP -> floatArrayOf(0.35f, 0.62f, 1.0f) // azul: cuesta
        SmwSolidity.SPIKE -> floatArrayOf(0.88f, 0.20f, 0.20f)       // rojo: pinchos
        SmwSolidity.NONE -> floatArrayOf(0f, 0f, 0f)
    }
}
