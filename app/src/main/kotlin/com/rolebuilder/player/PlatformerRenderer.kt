package com.rolebuilder.player

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import kotlin.math.abs
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.player.gl.Camera2D
import com.rolebuilder.player.gl.SpriteBatch
import com.rolebuilder.player.gl.Texture
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Mundo de un proyecto para pintar tiles REALES en el motor de plataformas: el mapa
 * dibujado en el editor y su tileset. Si es null, el renderer pinta la colisión por
 * colores (modo ROM cruda).
 */
class PlatformerWorld(val projectDir: File, val map: GameMap, val tileset: Tileset)

/**
 * Renderer del motor de plataformas: avanza el motor a 60 fps fijos (como SMW) y
 * dibuja el nivel, a Mario y a los enemigos con el mismo [SpriteBatch]/[Camera2D]
 * que el RPG.
 *
 * Mario se dibuja con su hoja GFX32 real de la ROM ([marioBitmap]); los enemigos con
 * su atlas real ([enemyBitmap], un fotograma 16×16 por id curado); si falta un asset
 * se cae al rectángulo de reserva. El audio ([audio]) suena cada evento del motor
 * (salto, pisotón, moneda, muerte) una sola vez con las muestras reales de SMW.
 *
 * La app escribe el input en [inMoveX]/[inRunning]/[inJumpHeld] desde otro hilo; el
 * hilo GL los aplica antes de cada tick.
 */
class PlatformerRenderer(
    private val engine: PlatformerEngine,
    private val world: PlatformerWorld? = null,
    /** Hoja de sprites de Mario (GFX32, 128×64) de la ROM; null = dibujar rectángulo. */
    private val marioBitmap: Bitmap? = null,
    /** Atlas de enemigos (16×16 por id curado, en el orden de curatedIds); null = rectángulo. */
    private val enemyBitmap: Bitmap? = null,
    /** Audio de SMW (SFX reales resueltos por SmwSfxCatalog); null = silencio. */
    private val audio: PlatformerAudio? = null,
) : GLSurfaceView.Renderer {

    @Volatile var inMoveX = 0f
    /** Eje vertical del joystick (-1 arriba .. +1 abajo): entra por tuberías/puertas. */
    @Volatile var inMoveY = 0f
    @Volatile var inRunning = false
    @Volatile var inJumpHeld = false

    /** Warp activado por el jugador (tubería/puerta); la UI lo consume y cambia de nivel. */
    @Volatile var pendingWarp: com.rolebuilder.core.engine.platformer.WarpTarget? = null
        private set

    // Últimos contadores de eventos vistos, para sonar cada evento una sola vez.
    private var lastJumpEvents = 0
    private var lastStompEvents = 0
    private var lastDeathEvents = 0
    private var lastCoinEvents = 0
    private var lastPowerupEvents = 0
    private var lastDamageEvents = 0

    /** Lo lee la UI para el aviso de "has muerto". */
    @Volatile var dead = false
        private set

    /** Monedas recogidas; lo lee la UI para el marcador (HUD). */
    @Volatile var coins = 0
        private set

    private lateinit var batch: SpriteBatch
    private lateinit var white: Texture
    private var marioTex: Texture? = null
    private var marioAnim = 0f
    private var enemyTex: Texture? = null
    private var tilesetTex: Texture? = null
    private var animByTile: Map<Int, com.rolebuilder.core.model.TileAnimation> = emptyMap()
    private val camera = Camera2D()
    private var lastNanos = 0L
    private var acc = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        batch = SpriteBatch()
        white = Texture.white()
        marioTex = marioBitmap?.let { Texture(it) }
        enemyTex = enemyBitmap?.let { Texture(it) }
        tilesetTex = world?.let {
            runCatching { Texture.fromFile(ProjectIo.imageFile(it.projectDir, it.tileset.image)) }.getOrNull()
        }
        animByTile = world?.tileset?.animations?.associateBy { it.baseTile } ?: emptyMap()
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
            engine.inputDown = inMoveY > 0.5f
            engine.inputUp = inMoveY < -0.5f
            engine.running = inRunning
            engine.setJumpHeld(inJumpHeld)
            engine.tick()
            acc -= step
            guard++
        }
        dead = engine.player.dead
        coins = engine.coins
        engine.pendingWarp?.let { pendingWarp = it }

        // Audio: suena cada evento del motor (salto, pisotón, moneda, muerte) una vez
        // por aparición, con las muestras reales de SMW resueltas por SmwSfxCatalog.
        audio?.let { a ->
            if (engine.jumpEvents > lastJumpEvents) a.play(com.rolebuilder.core.snes.SmwSfxCatalog.Event.JUMP)
            if (engine.stompEvents > lastStompEvents) a.play(com.rolebuilder.core.snes.SmwSfxCatalog.Event.STOMP)
            if (engine.deathEvents > lastDeathEvents) a.playDeath()
            if (engine.coinEvents > lastCoinEvents) a.play(com.rolebuilder.core.snes.SmwSfxCatalog.Event.COIN)
            if (engine.powerupEvents > lastPowerupEvents) a.play(com.rolebuilder.core.snes.SmwSfxCatalog.Event.POWERUP)
            // Encoger por golpe: no hay SFX propio en el catálogo; un pisotón grave sirve.
            if (engine.damageEvents > lastDamageEvents) a.play(com.rolebuilder.core.snes.SmwSfxCatalog.Event.STOMP, volume = 0.6f, rate = 0.6f)
            lastJumpEvents = engine.jumpEvents
            lastStompEvents = engine.stompEvents
            lastDeathEvents = engine.deathEvents
            lastCoinEvents = engine.coinEvents
            lastPowerupEvents = engine.powerupEvents
            lastDamageEvents = engine.damageEvents
        }

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
        val tex = tilesetTex
        val wld = world
        if (tex != null && wld != null) {
            // Tiles reales del proyecto (capa 0 y luego 1) en las celdas visibles.
            val ts = wld.tileset
            val map = wld.map
            for (layer in map.layers.indices) {
                for (r in minR..maxR) {
                    for (c in minC..maxC) {
                        val mapTile = map.tileAt(layer, c, r)
                        if (mapTile == EMPTY_TILE || mapTile < 0 || mapTile >= ts.tileCount) continue
                        // Moneda ya recogida: el motor la marcó consumida → no la pintes.
                        if (ts.platformBlockActions.getOrNull(mapTile) == com.rolebuilder.core.snes.SmwBlockAction.COIN.ordinal &&
                            engine.blockActionAt(c, r) == com.rolebuilder.core.engine.platformer.BlockAction.NONE
                        ) continue
                        // Si la tesela anima (monedas, bloques ?), sustituye por su fotograma.
                        val anim = animByTile[mapTile]
                        val tile = if (anim != null && anim.frames.isNotEmpty()) {
                            val stepNs = (anim.periodFrames * 1_000_000_000L) / 60L
                            anim.frames[((now / stepNs) % anim.frames.size).toInt()]
                        } else {
                            mapTile
                        }
                        if (tile < 0 || tile >= ts.tileCount) continue
                        val tc = tile % ts.columns
                        val tr = tile / ts.columns
                        batch.draw(
                            tex, c.toFloat(), r.toFloat(), 1f, 1f,
                            u0 = tc / ts.columns.toFloat(),
                            v0 = tr / ts.rows.toFloat(),
                            u1 = (tc + 1) / ts.columns.toFloat(),
                            v1 = (tr + 1) / ts.rows.toFloat(),
                        )
                    }
                }
            }
        } else {
            // Sin proyecto: pinta la colisión por colores (modo ROM cruda).
            for (r in minR..maxR) {
                for (c in minC..maxC) {
                    val s = engine.solidity(c, r)
                    if (s == SmwSolidity.NONE) continue
                    val col = colorOf(s)
                    batch.draw(white, c.toFloat(), r.toFloat(), 1f, 1f, r = col[0], g = col[1], b = col[2], a = 1f)
                }
            }
        }

        // Enemigos: sprite REAL de SMW para los ids con gráfico curado; si no, un
        // rectángulo. Aplastados = franja fina un instante al pisarlos.
        val etex = enemyTex
        for (e in engine.enemies) {
            if (!e.alive && e.squashTimer <= 0) continue
            val ex = e.x / 16f
            val ew = e.width / 16f
            val frame = ENEMY_FRAME[e.id]
            if (e.alive && etex != null && frame != null) {
                // Sprite 16×16 centrado sobre la caja del enemigo, con los pies abajo.
                val cx = (e.x + e.width / 2f) / 16f - 0.5f
                val feet = (e.y + e.height) / 16f - 1f
                val u0 = frame * ENEMY_UV
                batch.draw(
                    etex, cx, feet, 1f, 1f,
                    u0 = u0, v0 = 0f, u1 = u0 + ENEMY_UV, v1 = 1f,
                    flipX = e.vx > 0f,
                )
            } else if (e.alive) {
                batch.draw(white, ex, e.y / 16f, ew, e.height / 16f, r = 0.65f, g = 0.20f, b = 0.55f, a = 1f)
            } else {
                val fh = e.height / 16f * 0.3f
                batch.draw(white, ex, (e.y + e.height * 0.7f) / 16f, ew, fh, r = 0.5f, g = 0.5f, b = 0.5f, a = 1f)
            }
        }

        // Setas de powerup en marcha: seta clásica (sombrero rojo, base clara). Es un
        // dibujo de motor (dos rectángulos), no un sprite de la ROM: el gráfico de la
        // seta vive fuera de la tabla OAM genérica que ya portamos.
        for (m in engine.items) {
            if (!m.alive) continue
            val mx = m.x / 16f
            val my = m.y / 16f
            val mw = m.width / 16f
            val mh = m.height / 16f
            batch.draw(white, mx, my, mw, mh * 0.55f, r = 0.90f, g = 0.16f, b = 0.14f, a = 1f)
            batch.draw(white, mx + mw * 0.2f, my + mh * 0.55f, mw * 0.6f, mh * 0.45f, r = 1f, g = 0.9f, b = 0.75f, a = 1f)
        }

        drawMario(dt)

        batch.end()
    }

    /**
     * Dibuja a Mario. Con la hoja de la ROM ([marioTex]) pinta el SPRITE REAL: elige
     * el fotograma por el estado (parado/andando/corriendo/saltando), lo voltea según
     * hacia dónde mira y lo ancla por los pies. Sin hoja, cae al rectángulo.
     */
    private fun drawMario(dt: Float) {
        val p = engine.player
        // Invulnerable tras encoger: parpadea (se salta el dibujo en pulsos), como SMW.
        if (p.invulnFrames > 0 && (p.invulnFrames / 4) % 2 == 1) return
        val tex = marioTex
        if (tex == null) {
            val w = engine.tuning.playerWidth / 16f
            val h = engine.playerHeight / 16f
            val red = if (p.dead) 0.4f else 1f
            batch.draw(white, p.x / 16f, p.y / 16f, w, h, r = red, g = 0.25f, b = 0.2f, a = 1f)
            return
        }
        // Cada fotograma es Mario ENTERO 16×32 (cabeza + cuerpo ya compuestos por
        // smwMarioSheet, con su cara). Índices de pose de la hoja: 0 parado, 2/3 el
        // ciclo de andar, 4 saltando/corriendo (pose inclinada).
        val running = abs(p.vx) > 2.2f
        if (p.onGround && abs(p.vx) > 0.2f) marioAnim += dt else if (p.onGround) marioAnim = 0f
        val fc = when {
            !p.onGround -> 4                                   // saltando (pose inclinada)
            abs(p.vx) <= 0.2f -> 0                             // parado
            else -> if ((marioAnim * (if (running) 14f else 9f)).toInt() % 2 == 0) 2 else 3
        }
        val sw = tex.width.toFloat()
        val sh = tex.height.toFloat()
        val u0 = fc * 16f / sw
        val u1 = (fc * 16f + 16f) / sw
        val v0 = 0f
        val v1 = 1f // cada fotograma ocupa toda la altura (32 px) de la hoja
        // Sprite centrado en horizontal y anclado por los pies. La hoja ya es Mario
        // grande (32 px): a tamaño natural son 2 casillas; pequeño se dibuja algo más
        // bajo para acercarse a su caja de colisión.
        val dw = 1f
        val dh = if (p.big) 2f else 1.5f
        val cx = (p.x + engine.tuning.playerWidth / 2f) / 16f
        val feetY = (p.y + engine.playerHeight) / 16f
        // La hoja de GFX32 mira a la IZQUIERDA; se voltea al mirar a la derecha.
        batch.draw(
            tex, cx - dw / 2f, feetY - dh, dw, dh,
            u0 = u0, v0 = v0, u1 = u1, v1 = v1,
            flipX = p.facingRight,
        )
    }

    /** Libera los recursos de audio (SoundPool). Llamar al salir del nivel. */
    fun releaseAudio() = audio?.release()

    private fun colorOf(s: SmwSolidity): FloatArray = when (s) {
        SmwSolidity.LEDGE_TOP -> floatArrayOf(0.50f, 0.82f, 0.50f)   // verde: un sentido
        SmwSolidity.SOLID -> floatArrayOf(0.55f, 0.36f, 0.18f)       // marrón: sólido
        SmwSolidity.SLOPE, SmwSolidity.SLOPE_STEEP -> floatArrayOf(0.35f, 0.62f, 1.0f) // azul: cuesta
        SmwSolidity.SPIKE -> floatArrayOf(0.88f, 0.20f, 0.20f)       // rojo: pinchos
        SmwSolidity.NONE -> floatArrayOf(0f, 0f, 0f)
    }

    companion object {
        /** id de sprite → su fotograma en enemies.png; en el MISMO orden que hornea el atlas. */
        private val ENEMY_FRAME: Map<Int, Int> =
            com.rolebuilder.core.snes.SmwEnemyGraphics.curatedIds.withIndex()
                .associate { (frame, id) -> id to frame }
        /** Ancho UV de un fotograma en el atlas de enemigos. */
        private val ENEMY_UV = 1f / com.rolebuilder.core.snes.SmwEnemyGraphics.curatedIds.size.toFloat()
    }
}
