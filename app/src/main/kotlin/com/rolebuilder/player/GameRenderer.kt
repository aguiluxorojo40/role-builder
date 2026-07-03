package com.rolebuilder.player

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.rolebuilder.core.engine.EnemyEntity
import com.rolebuilder.core.engine.EventEntity
import com.rolebuilder.core.engine.HitEffect
import com.rolebuilder.core.engine.RpgEngine
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.player.gl.Camera2D
import com.rolebuilder.player.gl.SpriteBatch
import com.rolebuilder.player.gl.Texture
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Renderer del juego: ejecuta el tick del motor y dibuja el estado con
 * el SpriteBatch. Todo ocurre en el hilo GL.
 */
class GameRenderer(
    private val engine: RpgEngine,
    private val projectDir: File,
    private val onSound: (String) -> Unit,
    private val onBgmChange: (String?) -> Unit = {},
) : GLSurfaceView.Renderer {

    private lateinit var batch: SpriteBatch
    private lateinit var white: Texture
    private val textures = mutableMapOf<String, Texture>()
    private val camera = Camera2D()
    private var lastFrameNanos = 0L
    private var lastBgm: String? = BGM_UNSET

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        batch = SpriteBatch()
        white = Texture.white()
        textures.clear()
        lastFrameNanos = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        camera.viewportWidth = width
        camera.viewportHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 1f / 60f else ((now - lastFrameNanos) / 1e9f).coerceAtMost(0.05f)
        lastFrameNanos = now

        engine.tick(dt)
        engine.mapChanged = false
        drainSounds()
        if (engine.currentBgm != lastBgm) {
            lastBgm = engine.currentBgm
            onBgmChange(lastBgm)
        }

        GLES30.glClearColor(0.05f, 0.05f, 0.08f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        val map = engine.currentMap
        camera.x = engine.player.x
        camera.y = engine.player.y
        camera.update(map.width, map.height)

        batch.begin(camera.mvp)
        drawTiles()
        drawDrops()
        drawCharacters()
        drawProjectiles()
        drawSwordSwing()
        drawEffects()
        batch.end()
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

    /** Como [texture] pero devuelve null si la imagen no existe (efectos opcionales). */
    private fun textureOrNull(name: String): Texture? {
        if (name in missingTextures) return null
        textures[name]?.let { return it }
        if (!ProjectIo.imageFile(projectDir, name).exists()) {
            missingTextures.add(name)
            return null
        }
        return texture(name)
    }

    private val missingTextures = mutableSetOf<String>()

    // ------------------------------------------------------------------ tiles

    private fun drawTiles() {
        val map = engine.currentMap
        val tileset = engine.tileset
        val tex = texture(tileset.image)

        val minX = (camera.x - camera.viewTilesX / 2f - 1f).toInt().coerceAtLeast(0)
        val maxX = (camera.x + camera.viewTilesX / 2f + 1f).toInt().coerceAtMost(map.width - 1)
        val minY = (camera.y - camera.tilesVisibleY / 2f - 1f).toInt().coerceAtLeast(0)
        val maxY = (camera.y + camera.tilesVisibleY / 2f + 1f).toInt().coerceAtMost(map.height - 1)

        for (layer in map.layers.indices) {
            for (ty in minY..maxY) {
                for (tx in minX..maxX) {
                    val tile = map.tileAt(layer, tx, ty)
                    if (tile < 0) continue
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

        for (event in engine.events) {
            val sprite = event.currentSprite ?: continue
            if (event.erased || event.page == null) continue
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

    /** Dibuja un frame de una hoja 3x4 centrado en (cx, cy), tamaño 1 casilla. */
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
        batch.draw(
            tex,
            cx - 0.5f, cy - 0.6f, 1f, 1f,
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

    /**
     * Barrido de espada: la hoja gira 140° delante del jugador siguiendo el
     * golpe y un haz de corte (3 frames) "rompe el aire" en la dirección del
     * ataque. Si el proyecto no tiene los sprites, cae a un tajo blanco simple.
     */
    private fun drawSwordSwing() {
        val p = engine.player
        if (p.attackFlash <= 0f) return
        val progress = (1f - p.attackFlash / ATTACK_SWING_SECONDS).coerceIn(0f, 1f)

        // Ángulo base según dirección (mundo con Y hacia abajo).
        val baseAngle = when (p.attackDir) {
            Direction.RIGHT -> 0f
            Direction.DOWN -> (Math.PI / 2).toFloat()
            Direction.LEFT -> Math.PI.toFloat()
            Direction.UP -> (-Math.PI / 2).toFloat()
        }
        // La hoja barre de -70° a +70° respecto a la dirección del golpe.
        val sweep = Math.toRadians(-70.0 + 140.0 * progress).toFloat()
        val angle = baseAngle + sweep

        val swordTex = textureOrNull(SWORD_IMAGE)
        if (swordTex != null) {
            // La espada apunta "arriba" en el sprite: girarla para que la hoja
            // salga del puño del jugador a lo largo del ángulo del barrido.
            val cx = p.x + kotlin.math.cos(angle) * 0.55f
            val cy = p.y + kotlin.math.sin(angle) * 0.55f
            batch.draw(
                swordTex,
                cx - 0.45f, cy - 0.45f, 0.9f, 0.9f,
                rotation = angle + (Math.PI / 2).toFloat(),
            )
        } else {
            // Sin sprite: tajo blanco alargado girando con el barrido.
            val cx = p.x + kotlin.math.cos(angle) * 0.6f
            val cy = p.y + kotlin.math.sin(angle) * 0.6f
            batch.draw(
                white,
                cx - 0.45f, cy - 0.07f, 0.9f, 0.14f,
                r = 1f, g = 1f, b = 0.9f, a = 0.8f * (1f - progress * 0.5f),
                rotation = angle,
            )
        }

        // Haz de corte: avanza y se desvanece delante del jugador.
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
        }
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

    companion object {
        private val WALK_CYCLE = intArrayOf(0, 1, 2, 1)

        /** Centinela para forzar el primer aviso de BGM aunque sea null. */
        private const val BGM_UNSET = "__sin_bgm__"

        private const val SWORD_IMAGE = "sword.png"
        private const val SLASH_IMAGE = "slash.png"
    }
}
