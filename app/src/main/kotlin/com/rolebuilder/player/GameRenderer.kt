package com.rolebuilder.player

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.rolebuilder.core.engine.EnemyEntity
import com.rolebuilder.core.engine.EventEntity
import com.rolebuilder.core.engine.HitEffect
import com.rolebuilder.core.engine.RpgEngine
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Weather
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.player.gl.Camera2D
import com.rolebuilder.player.gl.PostProcessor
import com.rolebuilder.player.gl.SpriteBatch
import com.rolebuilder.player.gl.Texture
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renderer del juego: ejecuta el tick del motor y dibuja el estado con
 * el SpriteBatch. Todo ocurre en el hilo GL.
 */
class GameRenderer(
    private val engine: RpgEngine,
    private val projectDir: File,
    private val onSound: (String) -> Unit,
) : GLSurfaceView.Renderer {

    private lateinit var batch: SpriteBatch
    private lateinit var white: Texture
    private lateinit var radial: Texture
    private lateinit var post: PostProcessor
    private val textures = mutableMapOf<String, Texture>()
    private val camera = Camera2D()
    private var lastFrameNanos = 0L
    private var elapsed = 0f

    /** Estilo HD-2D del proyecto: post-procesado, sombras y motas. */
    private val hd2d = engine.data.project.hd2d

    /**
     * Factor de compresión del diorama 2.5D: s = cos(dioramaTilt). Geometría:
     * al tumbar el plano del suelo un ángulo t hacia atrás, cada fila de
     * casillas proyecta en pantalla cos(t) veces su alto, así que el ortho
     * vertical se expande a tilesVisibleY/s filas de mundo (más filas, cada
     * una más chata). Lo ERGUIDO no debe comprimirse: un billboard que en
     * pantalla debe seguir midiendo 1 casilla de alto necesita 1/s unidades
     * de mundo, y se ancla por los PIES (el punto que sí está apoyado en el
     * suelo comprimido) creciendo hacia arriba. Con s = 1 todo es idéntico
     * al render plano clásico.
     */
    private var squash = 1f

    /** Partículas de clima: x, y (relativas a la vista, en casillas), velocidad y fase. */
    private val particles = Array(WEATHER_PARTICLES) { FloatArray(4) }
    private var particlesSeeded = false

    /** Motas de luz ambientales (solo HD-2D): x, y, velocidad y fase. */
    private val motes = Array(MOTES) { FloatArray(4) }
    private var motesSeeded = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        batch = SpriteBatch()
        white = Texture.white()
        radial = Texture.radial()
        post = PostProcessor()
        textures.clear()
        lastFrameNanos = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        camera.viewportWidth = width
        camera.viewportHeight = height
        if (hd2d) post.resize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 1f / 60f else ((now - lastFrameNanos) / 1e9f).coerceAtMost(0.05f)
        lastFrameNanos = now

        engine.tick(dt)
        engine.mapChanged = false
        drainSounds()
        elapsed += dt

        val usePost = hd2d && post.enabled
        if (usePost) post.beginScene()
        GLES30.glClearColor(0.05f, 0.05f, 0.08f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        val map = engine.currentMap
        // El motor resuelve la inclinación (comando > mapa > proyecto) y su transición.
        squash = cos(engine.tiltCurrent * (PI.toFloat() / 180f))
        camera.squashY = squash
        camera.x = engine.player.x
        camera.y = engine.player.y
        updateShake()
        camera.update(map.width, map.height)

        batch.begin(camera.mvp)
        drawParallax(above = false)
        drawTiles()
        if (hd2d) drawShadows()
        drawDrops()
        drawCharacters()
        drawProjectiles()
        drawAttackFlash()
        drawEffects()
        drawWeather(dt)
        drawParallax(above = true)
        drawTint()
        drawLights(dt)
        drawFlash()
        batch.end()
        if (usePost) post.compose()
    }

    /**
     * Capas de parallax del mapa: la imagen se repite en mosaico (16 px =
     * 1 casilla) y acompaña a la cámara según su factor (0 = pegada a la
     * pantalla, 1 = pegada al mapa), con deriva automática opcional.
     * [above] elige entre las capas de fondo y las de niebla.
     */
    private fun drawParallax(above: Boolean) {
        val map = engine.currentMap
        if (map.parallaxLayers.isEmpty()) return
        val left = camera.x - camera.viewTilesX / 2f - 1f + camera.shakeX
        val top = camera.y - camera.viewTilesYWorld / 2f - 1f + camera.shakeY
        val viewW = camera.viewTilesX + 2f
        val viewH = camera.viewTilesYWorld + 2f

        for (layer in map.parallaxLayers) {
            if (layer.above != above || layer.alpha <= 0f) continue
            val tex = texture(layer.image)
            val imgW = tex.width / 16f
            val imgH = tex.height / 16f
            if (imgW <= 0f || imgH <= 0f) continue
            val phaseX = (left * layer.factor + layer.autoX * elapsed).mod(imgW)
            val phaseY = (top * layer.factor + layer.autoY * elapsed).mod(imgH)
            var y = top - phaseY
            while (y < top + viewH) {
                var x = left - phaseX
                while (x < left + viewW) {
                    batch.draw(tex, x, y, imgW, imgH, a = layer.alpha)
                    x += imgW
                }
                y += imgH
            }
        }
    }

    private fun updateShake() {
        if (engine.shakeTimeLeft > 0f) {
            val amp = 0.14f * minOf(1f, engine.shakeTimeLeft * 2f)
            camera.shakeX = sin(elapsed * 55f) * amp
            camera.shakeY = cos(elapsed * 41f) * amp * 0.6f
        } else {
            camera.shakeX = 0f
            camera.shakeY = 0f
        }
    }

    private fun drainSounds() {
        while (true) {
            val sound = engine.soundQueue.poll() ?: break
            onSound(sound)
        }
    }

    private fun texture(name: String): Texture = textures.getOrPut(name) {
        Texture.fromFile(ProjectIo.imageFile(projectDir, name))
    }

    // ------------------------------------------------------------------ tiles

    private fun drawTiles() {
        val map = engine.currentMap
        val tileset = engine.tileset
        val tex = texture(tileset.image)

        val minX = (camera.x - camera.viewTilesX / 2f - 1f).toInt().coerceAtLeast(0)
        val maxX = (camera.x + camera.viewTilesX / 2f + 1f).toInt().coerceAtMost(map.width - 1)
        val minY = (camera.y - camera.viewTilesYWorld / 2f - 1f).toInt().coerceAtLeast(0)
        val maxY = (camera.y + camera.viewTilesYWorld / 2f + 1f).toInt().coerceAtMost(map.height - 1)

        for (layer in map.layers.indices) {
            for (ty in minY..maxY) {
                for (tx in minX..maxX) {
                    val tile = map.tileAt(layer, tx, ty)
                    if (tile < 0) continue
                    // Los tiles "de pie" de la segunda capa se dibujan como
                    // billboards ordenados por Y en drawCharacters, no aquí.
                    if (layer == 1 && tile in tileset.standingTiles) continue
                    val col = tile % tileset.columns
                    val row = tile / tileset.columns
                    batch.draw(
                        tex,
                        tx.toFloat(), ty.toFloat(), 1f, 1f,
                        u0 = col / tileset.columns.toFloat(),
                        v0 = row / tileset.rows.toFloat(),
                        u1 = (col + 1) / tileset.columns.toFloat(),
                        v1 = (row + 1) / tileset.rows.toFloat(),
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------- characters

    private fun drawCharacters() {
        data class Sortable(val y: Float, val draw: () -> Unit)

        val list = mutableListOf<Sortable>()

        // Tiles "de pie": billboards verticales con la base en la parte
        // inferior de su casilla (ty + 1) y alto 1/s; compiten por Y con los
        // personajes (clave ty + 0.5, comparable con los centros de entidad)
        // para que un árbol tape al héroe que pasa por detrás y viceversa.
        // Con s = 1 el quad es exactamente el 1x1 de su casilla.
        val map = engine.currentMap
        val tileset = engine.tileset
        if (tileset.standingTiles.isNotEmpty() && map.layers.size > 1) {
            val tex = texture(tileset.image)
            val minX = (camera.x - camera.viewTilesX / 2f - 1f).toInt().coerceAtLeast(0)
            val maxX = (camera.x + camera.viewTilesX / 2f + 1f).toInt().coerceAtMost(map.width - 1)
            val minY = (camera.y - camera.viewTilesYWorld / 2f - 1f).toInt().coerceAtLeast(0)
            val maxY = (camera.y + camera.viewTilesYWorld / 2f + 1f).toInt().coerceAtMost(map.height - 1)
            for (ty in minY..maxY) {
                for (tx in minX..maxX) {
                    val tile = map.tileAt(1, tx, ty)
                    if (tile < 0 || tile !in tileset.standingTiles) continue
                    val col = tile % tileset.columns
                    val row = tile / tileset.columns
                    list.add(
                        Sortable(ty + 0.5f) {
                            val h = 1f / squash
                            batch.draw(
                                tex,
                                tx.toFloat(), ty + 1f - h, 1f, h,
                                u0 = col / tileset.columns.toFloat(),
                                v0 = row / tileset.rows.toFloat(),
                                u1 = (col + 1) / tileset.columns.toFloat(),
                                v1 = (row + 1) / tileset.rows.toFloat(),
                            )
                        },
                    )
                }
            }
        }

        for (event in engine.events) {
            val sprite = event.page?.sprite ?: continue
            if (event.erased) continue
            list.add(Sortable(event.y) { drawSheet(sprite.image, event.x, event.y, event.dir, event.animTime, event.moving) })
        }
        for (enemy in engine.enemies) {
            list.add(
                Sortable(enemy.y) {
                    val flash = !enemy.alive || enemy.invulnTime > 0.15f
                    drawSheet(enemy.def.sprite, enemy.x, enemy.y, enemy.dir, enemy.animTime, enemy.moving, flash)
                },
            )
        }
        val p = engine.player
        val blink = p.invulnTime > 0f && ((p.invulnTime * 10f).toInt() % 2 == 0)
        if (!blink && !engine.gameOver) {
            list.add(Sortable(p.y) { drawSheet(engine.actor.sprite, p.x, p.y, p.dir, p.animTime, p.moving) })
        }

        list.sortBy { it.y }
        list.forEach { it.draw() }
    }

    /**
     * Dibuja un frame de una hoja 3x4 en (cx, cy), 1 casilla de ancho. Los
     * pies quedan anclados en cy + 0.4 y el alto es 1/s hacia arriba para
     * que el billboard no se comprima con el suelo del diorama (con s = 1
     * es el quad clásico de 1x1 en cy - 0.6 .. cy + 0.4).
     */
    private fun drawSheet(
        image: String,
        cx: Float,
        cy: Float,
        dir: Direction,
        animTime: Float,
        moving: Boolean,
        flashWhite: Boolean = false,
    ) {
        val tex = texture(image)
        val frame = if (moving) WALK_CYCLE[(animTime * 8f).toInt() % 4] else 1
        val row = when (dir) {
            Direction.DOWN -> 0
            Direction.LEFT -> 1
            Direction.RIGHT -> 2
            Direction.UP -> 3
        }
        val tint = if (flashWhite) 4f else 1f
        val h = 1f / squash
        batch.draw(
            tex,
            cx - 0.5f, cy + 0.4f - h, 1f, h,
            u0 = frame / 3f, v0 = row / 4f, u1 = (frame + 1) / 3f, v1 = (row + 1) / 4f,
            r = tint, g = tint, b = tint,
        )
    }

    // ----------------------------------------------------------------- extras

    private fun drawDrops() {
        for (drop in engine.drops) {
            batch.draw(white, drop.x - 0.15f, drop.y - 0.15f, 0.3f, 0.3f, r = 0.4f, g = 0.9f, b = 0.5f)
            batch.draw(white, drop.x - 0.08f, drop.y - 0.08f, 0.16f, 0.16f, r = 0.9f, g = 1f, b = 0.9f)
        }
    }

    private fun drawProjectiles() {
        for (proj in engine.projectiles) {
            batch.draw(white, proj.x - 0.15f, proj.y - 0.15f, 0.3f, 0.3f, r = 1f, g = 0.85f, b = 0.3f)
        }
    }

    private fun drawAttackFlash() {
        val p = engine.player
        if (p.attackFlash <= 0f) return
        val alpha = (p.attackFlash / 0.18f).coerceIn(0f, 1f)
        val cx = p.x + p.dir.dx * 0.9f
        val cy = p.y + p.dir.dy * 0.9f
        val w = if (p.dir.dx != 0) 0.9f else 1.3f
        val h = if (p.dir.dy != 0) 0.9f else 1.3f
        batch.draw(white, cx - w / 2f, cy - h / 2f, w, h, r = 1f, g = 1f, b = 0.85f, a = 0.55f * alpha)
    }

    private fun drawEffects() {
        for (effect in engine.effects) {
            val progress = 1f - (effect.timer / 0.8f)
            val yOffset = progress * 0.8f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val (r, g, b) = when (effect.kind) {
                HitEffect.Kind.DAMAGE_ENEMY -> Triple(1f, 1f, 1f)
                HitEffect.Kind.DAMAGE_PLAYER -> Triple(1f, 0.3f, 0.3f)
                HitEffect.Kind.HEAL -> Triple(0.4f, 1f, 0.5f)
                HitEffect.Kind.ITEM -> Triple(1f, 0.95f, 0.4f)
            }
            // Una marca por punto de daño, en abanico.
            val count = effect.amount.coerceIn(1, 6)
            for (i in 0 until count) {
                val dx = (i - (count - 1) / 2f) * 0.22f
                batch.draw(
                    white,
                    effect.x + dx - 0.06f, effect.y - 0.5f - yOffset - 0.06f,
                    0.12f, 0.12f, r = r, g = g, b = b, a = alpha,
                )
            }
        }
    }

    // ------------------------------------------------------ clima y pantalla

    /** Lluvia o nieve como partículas en coordenadas relativas a la vista. */
    private fun drawWeather(dt: Float) {
        val weather = engine.state.weather
        if (weather == Weather.NONE) {
            particlesSeeded = false
            return
        }
        val viewW = camera.viewTilesX + 2f
        val viewH = camera.viewTilesYWorld + 2f
        if (!particlesSeeded) {
            for (p in particles) {
                p[0] = Random.nextFloat() * viewW
                p[1] = Random.nextFloat() * viewH
                p[2] = 0.7f + Random.nextFloat() * 0.6f // multiplicador de velocidad
                p[3] = Random.nextFloat() * 6.28f // fase de oscilación
            }
            particlesSeeded = true
        }

        val left = camera.x - camera.viewTilesX / 2f - 1f + camera.shakeX
        val top = camera.y - camera.viewTilesYWorld / 2f - 1f + camera.shakeY

        for (p in particles) {
            when (weather) {
                Weather.RAIN -> {
                    p[1] += dt * 13f * p[2]
                    p[0] += dt * 3.5f * p[2]
                }
                Weather.SNOW -> {
                    p[1] += dt * 1.4f * p[2]
                    p[0] += sin(elapsed * 1.7f + p[3]) * dt * 0.6f
                }
                Weather.NONE -> Unit
            }
            if (p[1] > viewH) {
                p[1] -= viewH
                p[0] = Random.nextFloat() * viewW
            }
            if (p[0] > viewW) p[0] -= viewW
            if (p[0] < 0f) p[0] += viewW

            val x = left + p[0]
            val y = top + p[1]
            when (weather) {
                Weather.RAIN -> batch.draw(
                    white, x, y, 0.04f, 0.35f * p[2],
                    r = 0.6f, g = 0.7f, b = 1f, a = 0.45f,
                )
                Weather.SNOW -> batch.draw(
                    white, x, y, 0.09f, 0.09f,
                    r = 1f, g = 1f, b = 1f, a = 0.8f,
                )
                Weather.NONE -> Unit
            }
        }
    }

    /** Overlay de tinte gradual a pantalla completa. */
    private fun drawTint() {
        val tint = engine.tintCurrent
        if (tint[3] <= 0.004f) return
        val left = camera.x - camera.viewTilesX / 2f - 1f + camera.shakeX
        val top = camera.y - camera.viewTilesYWorld / 2f - 1f + camera.shakeY
        batch.draw(
            white, left, top, camera.viewTilesX + 2f, camera.viewTilesYWorld + 2f,
            r = tint[0], g = tint[1], b = tint[2], a = tint[3],
        )
    }

    /** Destello de pantalla, por encima de tinte y luces. */
    private fun drawFlash() {
        if (engine.flashIntensity <= 0.004f) return
        val left = camera.x - camera.viewTilesX / 2f - 1f + camera.shakeX
        val top = camera.y - camera.viewTilesYWorld / 2f - 1f + camera.shakeY
        val flash = engine.flashColor
        batch.draw(
            white, left, top, camera.viewTilesX + 2f, camera.viewTilesYWorld + 2f,
            r = flash[0], g = flash[1], b = flash[2], a = engine.flashIntensity,
        )
    }

    // ------------------------------------------------------- luces y sombras

    /** Sombras elípticas suaves bajo los personajes (solo HD-2D). */
    private fun drawShadows() {
        fun shadow(cx: Float, cy: Float) {
            batch.draw(radial, cx - 0.32f, cy + 0.16f, 0.64f, 0.26f, r = 0f, g = 0f, b = 0f, a = 0.35f)
        }
        for (event in engine.events) {
            if (event.erased || event.page?.sprite == null) continue
            shadow(event.x, event.y)
        }
        for (enemy in engine.enemies) {
            if (enemy.alive) shadow(enemy.x, enemy.y)
        }
        if (!engine.gameOver) shadow(engine.player.x, engine.player.y)

        // Con el diorama activo, los tiles "de pie" también proyectan la
        // misma elipse, centrada en la base de su casilla, para asentarlos
        // en el suelo. Con s = 1 se dibujan planos y no necesitan sombra.
        if (squash < 1f) {
            val map = engine.currentMap
            val tileset = engine.tileset
            if (tileset.standingTiles.isEmpty() || map.layers.size < 2) return
            val minX = (camera.x - camera.viewTilesX / 2f - 1f).toInt().coerceAtLeast(0)
            val maxX = (camera.x + camera.viewTilesX / 2f + 1f).toInt().coerceAtMost(map.width - 1)
            val minY = (camera.y - camera.viewTilesYWorld / 2f - 1f).toInt().coerceAtLeast(0)
            val maxY = (camera.y + camera.viewTilesYWorld / 2f + 1f).toInt().coerceAtMost(map.height - 1)
            for (ty in minY..maxY) {
                for (tx in minX..maxX) {
                    val tile = map.tileAt(1, tx, ty)
                    if (tile < 0 || tile !in tileset.standingTiles) continue
                    batch.draw(
                        radial,
                        tx + 0.5f - 0.32f, ty + 1f - 0.13f, 0.64f, 0.26f,
                        r = 0f, g = 0f, b = 0f, a = 0.35f,
                    )
                }
            }
        }
    }

    /**
     * Luces cálidas aditivas de los eventos con lightRadius > 0 (atraviesan
     * el tinte de pantalla) y, en HD-2D, motas de luz ambientales.
     */
    private fun drawLights(dt: Float) {
        batch.setAdditive(true)

        for (event in engine.events) {
            val radius = event.page?.lightRadius ?: 0f
            if (radius <= 0f || event.erased) continue
            val flicker = 0.85f + 0.15f * sin(elapsed * 9f + event.event.id * 1.7f)
            batch.draw(
                radial,
                event.x - radius, event.y - radius, radius * 2f, radius * 2f,
                r = 1f, g = 0.72f, b = 0.42f, a = 0.55f * flicker,
            )
        }

        if (hd2d) drawMotes(dt)
        batch.setAdditive(false)
    }

    /** Motas de polvo/luciérnagas flotando; se dibujan en modo aditivo. */
    private fun drawMotes(dt: Float) {
        val viewW = camera.viewTilesX + 2f
        val viewH = camera.viewTilesYWorld + 2f
        if (!motesSeeded) {
            for (m in motes) {
                m[0] = Random.nextFloat() * viewW
                m[1] = Random.nextFloat() * viewH
                m[2] = 0.5f + Random.nextFloat()
                m[3] = Random.nextFloat() * 6.28f
            }
            motesSeeded = true
        }
        val left = camera.x - camera.viewTilesX / 2f - 1f + camera.shakeX
        val top = camera.y - camera.viewTilesYWorld / 2f - 1f + camera.shakeY
        for (m in motes) {
            m[1] -= dt * 0.12f * m[2]
            m[0] += sin(elapsed * 0.8f + m[3]) * dt * 0.25f
            if (m[1] < 0f) m[1] += viewH
            if (m[0] > viewW) m[0] -= viewW
            if (m[0] < 0f) m[0] += viewW
            val pulse = 0.5f + 0.5f * sin(elapsed * 1.3f + m[3] * 2f)
            batch.draw(
                radial,
                left + m[0], top + m[1], 0.12f, 0.12f,
                r = 1f, g = 0.9f, b = 0.6f, a = 0.05f + 0.09f * pulse,
            )
        }
    }

    companion object {
        private val WALK_CYCLE = intArrayOf(0, 1, 2, 1)
        private const val WEATHER_PARTICLES = 90
        private const val MOTES = 40
    }
}
