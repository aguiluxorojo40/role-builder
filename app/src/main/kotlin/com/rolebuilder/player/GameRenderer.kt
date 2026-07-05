package com.rolebuilder.player

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.rolebuilder.core.engine.ATTACK_SWING_SECONDS
import com.rolebuilder.core.engine.HitEffect
import com.rolebuilder.core.engine.RpgEngine
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.player.gl.Camera3D
import com.rolebuilder.player.gl.PostProcessor
import com.rolebuilder.player.gl.SpriteBatch
import com.rolebuilder.player.gl.Texture
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renderer del juego en 3D real (HD-2D): el suelo es un plano de casillas
 * visto en perspectiva y los personajes, árboles y casas son BILLBOARDS que
 * se levantan del suelo mirando a la cámara — no se estiran, la perspectiva
 * hace el trabajo. El z-buffer resuelve la oclusión. Todo ocurre en el hilo GL.
 */
class GameRenderer(
    private val engine: RpgEngine,
    private val projectDir: File,
    private val onSound: (String) -> Unit,
    private val onBgmChange: (String?) -> Unit = {},
) : GLSurfaceView.Renderer {

    private lateinit var batch: SpriteBatch
    private lateinit var white: Texture
    private lateinit var radial: Texture
    private lateinit var post: PostProcessor
    private val textures = mutableMapOf<String, Texture>()
    private val camera = Camera3D()
    private var lastFrameNanos = 0L
    private var lastBgm: String? = BGM_UNSET
    private var elapsed = 0f

    private val hd2d = engine.data.project.hd2d

    @Volatile
    var liveStrength = engine.data.project.hd2dStrength

    @Volatile
    var liveTilt = engine.data.project.dioramaTilt

    /** Altura de los sprites de pie (multiplicador del alto de dibujo). */
    @Volatile
    var liveSpriteStand = engine.data.project.spriteStand

    private val missingTextures = mutableSetOf<String>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        batch = SpriteBatch()
        white = Texture.white()
        radial = Texture.radial()
        post = PostProcessor()
        textures.clear()
        missingTextures.clear()
        lastFrameNanos = 0L
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
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
        elapsed += dt
        drainSounds()
        if (engine.currentBgm != lastBgm) {
            lastBgm = engine.currentBgm
            onBgmChange(lastBgm)
        }

        val usePost = hd2d && post.enabled
        if (usePost) post.beginScene()

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)
        GLES30.glClearColor(0.04f, 0.05f, 0.09f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val map = engine.currentMap
        camera.x = engine.player.x
        camera.y = engine.player.y
        camera.pitchDegrees = if (hd2d) liveTilt.coerceIn(0f, 75f) else 8f
        post.strength = liveStrength
        updateShake()
        camera.update(map.width, map.height)

        batch.begin(camera.vp, camera.right, camera.up)

        // --- Suelo (escribe profundidad) ---
        drawGround()
        batch.flush()

        // --- Decoración de suelo sin profundidad: sombras, drops, tajo ---
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        if (hd2d) drawShadows()
        drawDrops()
        drawProjectiles()
        drawSwordSwing()
        batch.flush()

        // --- Billboards de pie (con profundidad) ---
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        drawBillboards()
        batch.flush()

        // --- Overlays sin profundidad: números de daño, luces, tinte, destello ---
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        drawEffects()
        drawLights()
        drawTint()
        drawFlash()
        batch.end()

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        if (usePost) post.compose()
    }

    private fun updateShake() {
        if (engine.shakeTimeLeft > 0f) {
            val amp = 0.14f * minOf(1f, engine.shakeTimeLeft * 2f)
            camera.shakeX = sin(elapsed * 55f) * amp
            camera.shakeY = kotlin.math.cos(elapsed * 41f) * amp * 0.6f
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

    private fun textureOrNull(name: String): Texture? {
        if (name in missingTextures) return null
        textures[name]?.let { return it }
        if (!ProjectIo.imageFile(projectDir, name).exists()) {
            missingTextures.add(name)
            return null
        }
        return texture(name)
    }

    // ---- ventana de casillas visibles (generosa, la perspectiva recorta) ----

    private val viewRadius get() = (camera.tilesVisibleY + 4f).toInt()
    private val minX get() = (camera.x - viewRadius).toInt().coerceAtLeast(0)
    private val maxX get() = (camera.x + viewRadius).toInt().coerceAtMost(engine.currentMap.width - 1)
    private val minY get() = (camera.y - viewRadius).toInt().coerceAtLeast(0)
    private val maxY get() = (camera.y + viewRadius).toInt().coerceAtMost(engine.currentMap.height - 1)

    private val spriteStand: Float get() = liveSpriteStand.coerceIn(0.5f, 2.5f)

    /** Alto en casillas del billboard: ancho 1, alto = proporción del arte. */
    private fun billboardHeight(tex: Texture, cols: Int = 3, rows: Int = 4): Float {
        val cellAspect = (tex.height.toFloat() / rows) / (tex.width.toFloat() / cols)
        return cellAspect * spriteStand
    }

    // --------------------------------------------------------------- suelo

    private fun drawGround() {
        val map = engine.currentMap
        val tileset = engine.tileset
        val tex = texture(tileset.image)
        val standing = if (hd2d) tileset.standingTiles else emptyList()
        for (layer in map.layers.indices) {
            for (ty in minY..maxY) {
                for (tx in minX..maxX) {
                    val tile = map.tileAt(layer, tx, ty)
                    if (tile < 0) continue
                    if (layer == 1 && tile in standing) continue // se dibuja como billboard
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

    // ---------------------------------------------------------- billboards

    private class Standee(val z: Float, val draw: () -> Unit)

    private fun drawBillboards() {
        val list = ArrayList<Standee>(64)
        collectStandingTiles(list)

        for (event in engine.events) {
            val sprite = event.currentSprite ?: continue
            if (event.erased || event.page == null) continue
            list.add(Standee(event.y) { drawSheet(sprite.image, event.x, event.y, event.dir, event.animTime, event.moving) })
        }
        for (enemy in engine.enemies) {
            list.add(
                Standee(enemy.y) {
                    val flash = !enemy.alive || enemy.invulnTime > 0.15f
                    drawSheet(enemy.def.sprite, enemy.x, enemy.y, enemy.dir, enemy.animTime, enemy.moving, flash)
                },
            )
        }
        val p = engine.player
        val blink = p.invulnTime > 0f && ((p.invulnTime * 10f).toInt() % 2 == 0)
        if (!blink && !engine.gameOver) {
            list.add(Standee(p.y) { drawSheet(engine.actor.sprite, p.x, p.y, p.dir, p.animTime, p.moving) })
        }

        // Lejos (z pequeño) primero para la mezcla alfa correcta.
        list.sortBy { it.z }
        list.forEach { it.draw() }
    }

    /**
     * Tiles "de pie" agrupados en columnas verticales: una cadena contigua
     * (copa+tronco, tejado+muro) se apila en el eje Y como una sola torre en
     * la casilla base (la más al sur), en vez de tumbarse. Cada tramo se
     * ordena por profundidad con los personajes.
     */
    private fun collectStandingTiles(out: MutableList<Standee>) {
        if (!hd2d) return
        val map = engine.currentMap
        val tileset = engine.tileset
        val standing = tileset.standingTiles
        if (standing.isEmpty() || map.layers.size < 2) return
        val tex = texture(tileset.image)
        val tileH = spriteStand

        for (tx in minX..maxX) {
            for (ty in minY..maxY) {
                val tile = map.tileAt(1, tx, ty)
                if (tile < 0 || tile !in standing) continue
                // Solo la BASE de la columna (la de más al sur) genera la torre.
                val south = map.tileAt(1, tx, ty + 1)
                if (south >= 0 && south in standing) continue
                // Reúne la cadena hacia el norte.
                val stack = ArrayList<Int>(4)
                var yy = ty
                while (yy >= 0) {
                    val t = map.tileAt(1, tx, yy)
                    if (t < 0 || t !in standing) break
                    stack.add(t)
                    yy--
                }
                val baseX = tx + 0.5f
                val baseZ = ty + 0.5f
                out.add(
                    Standee(baseZ) {
                        stack.forEachIndexed { level, t ->
                            val col = t % tileset.columns
                            val row = t / tileset.columns
                            batch.drawBillboard(
                                tex, baseX, baseZ, 1f, tileH, baseY = level * tileH,
                                u0 = col / tileset.columns.toFloat(),
                                v0 = row / tileset.rows.toFloat(),
                                u1 = (col + 1) / tileset.columns.toFloat(),
                                v1 = (row + 1) / tileset.rows.toFloat(),
                            )
                        }
                    },
                )
            }
        }
    }

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
        val h = billboardHeight(tex)
        batch.drawBillboard(
            tex, cx, cy, 1f, h, baseY = 0f,
            u0 = frame / 3f, v0 = row / 4f, u1 = (frame + 1) / 3f, v1 = (row + 1) / 4f,
            r = tint, g = tint, b = tint,
        )
    }

    /** Altura del héroe (para anclar la espada y los números de daño). */
    private val heroHeight: Float get() = billboardHeight(texture(engine.actor.sprite))

    // ------------------------------------------------------------- extras

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

    private fun drawSwordSwing() {
        val p = engine.player
        if (p.attackFlash <= 0f) return
        val progress = (1f - p.attackFlash / ATTACK_SWING_SECONDS).coerceIn(0f, 1f)
        val baseAngle = when (p.attackDir) {
            Direction.RIGHT -> 0f
            Direction.DOWN -> (Math.PI / 2).toFloat()
            Direction.LEFT -> Math.PI.toFloat()
            Direction.UP -> (-Math.PI / 2).toFloat()
        }
        val sweep = Math.toRadians(-70.0 + 140.0 * progress).toFloat()
        val angle = baseAngle + sweep
        val slashTex = textureOrNull(SLASH_IMAGE)
        if (slashTex != null) {
            val frame = (progress * 3f).toInt().coerceIn(0, 2)
            val reach = 0.75f + 0.35f * progress
            val cx = p.x + p.attackDir.dx * reach
            val cy = p.y + p.attackDir.dy * reach
            batch.draw(
                slashTex,
                cx - 0.7f, cy - 0.7f, 1.4f, 1.4f,
                u0 = frame / 3f, u1 = (frame + 1) / 3f,
                a = 0.95f - 0.45f * progress,
                rotation = baseAngle,
            )
        } else {
            val cx = p.x + kotlin.math.cos(angle) * 0.6f
            val cy = p.y + kotlin.math.sin(angle) * 0.6f
            batch.draw(
                white,
                cx - 0.45f, cy - 0.07f, 0.9f, 0.14f,
                r = 1f, g = 1f, b = 0.9f, a = 0.8f * (1f - progress * 0.5f),
                rotation = angle,
            )
        }
    }

    /** Números de daño: billboards que flotan sobre la cabeza. */
    private fun drawEffects() {
        for (effect in engine.effects) {
            val progress = 1f - (effect.timer / 0.8f)
            val rise = progress * 0.8f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val (r, g, b) = when (effect.kind) {
                HitEffect.Kind.DAMAGE_ENEMY -> Triple(1f, 1f, 1f)
                HitEffect.Kind.DAMAGE_PLAYER -> Triple(1f, 0.3f, 0.3f)
                HitEffect.Kind.HEAL -> Triple(0.4f, 1f, 0.5f)
                HitEffect.Kind.ITEM -> Triple(1f, 0.95f, 0.4f)
            }
            val count = effect.amount.coerceIn(1, 6)
            for (i in 0 until count) {
                val dx = (i - (count - 1) / 2f) * 0.22f
                batch.drawBillboard(
                    white,
                    effect.x + dx, effect.y, 0.12f, 0.12f,
                    baseY = heroHeight + 0.2f + rise,
                    r = r, g = g, b = b, a = alpha,
                )
            }
        }
    }

    /** Overlay de tinte gradual a pantalla completa. */
    private fun drawTint() {
        val tint = engine.tintCurrent
        if (tint[3] <= 0.004f) return
        batch.screenOverlay(white, tint[0], tint[1], tint[2], tint[3])
    }

    /** Destello de pantalla. */
    private fun drawFlash() {
        if (engine.flashIntensity <= 0.004f) return
        val flash = engine.flashColor
        batch.screenOverlay(white, flash[0], flash[1], flash[2], engine.flashIntensity)
    }

    // -------------------------------------------------------- luces y sombras

    /** Sombras elípticas suaves en el suelo bajo cada billboard. */
    private fun drawShadows() {
        val alpha = 0.35f * liveStrength.coerceIn(0f, 1.4f)
        if (alpha <= 0.01f) return
        fun shadow(cx: Float, cy: Float) {
            batch.draw(radial, cx - 0.34f, cy - 0.14f, 0.68f, 0.28f, r = 0f, g = 0f, b = 0f, a = alpha)
        }
        for (event in engine.events) {
            if (event.erased || event.currentSprite == null) continue
            shadow(event.x, event.y)
        }
        for (enemy in engine.enemies) {
            if (enemy.alive) shadow(enemy.x, enemy.y)
        }
        if (!engine.gameOver) shadow(engine.player.x, engine.player.y)
    }

    /** Charcos de luz cálida (aditivos) de los eventos con lightRadius > 0. */
    private fun drawLights() {
        var any = false
        for (event in engine.events) {
            val radius = event.page?.lightRadius ?: 0f
            if (radius <= 0f || event.erased) continue
            if (!any) { batch.setAdditive(true); any = true }
            val flicker = 0.85f + 0.15f * sin(elapsed * 9f + event.event.id * 1.7f)
            batch.draw(
                radial,
                event.x - radius, event.y - radius, radius * 2f, radius * 2f,
                r = 1f, g = 0.72f, b = 0.42f, a = 0.5f * flicker,
            )
        }
        if (any) {
            batch.flush()
            batch.setAdditive(false)
        }
    }

    companion object {
        private val WALK_CYCLE = intArrayOf(0, 1, 2, 1)
        private const val BGM_UNSET = "__sin_bgm__"
        private const val SLASH_IMAGE = "slash.png"
    }
}
