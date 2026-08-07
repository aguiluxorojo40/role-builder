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
    /** Hoja de Mario PEQUEÑO (GFX32 compuesto, 16×32 por pose) de la ROM; null = rectángulo. */
    private val marioBitmap: Bitmap? = null,
    /** Atlas de enemigos (celda ATLAS_CELL² por id, ATLAS_FRAMES apilados; orden de curatedIds); null = rectángulo. */
    private val enemyBitmap: Bitmap? = null,
    /** Audio de SMW (SFX reales resueltos por SmwSfxCatalog); null = silencio. */
    private val audio: PlatformerAudio? = null,
    /** Hoja de Mario GRANDE (poder 1, gráficos propios); null = escalar la de pequeño. */
    private val marioBigBitmap: Bitmap? = null,
    /** Hoja de Mario de FUEGO (poder 3: gráficos de grande, paleta blanca); null = usar grande. */
    private val marioFireBitmap: Bitmap? = null,
    /** Hoja de Mario CAPA (poder 2: gráficos con capa amarilla); null = usar grande. */
    private val marioCapeBitmap: Bitmap? = null,
    /**
     * Fotogramas VIVOS de enemigos por id (celdas 16×32 ancladas por los pies, 1-2
     * fotogramas de andar); tienen prioridad sobre el atlas horneado [enemyBitmap].
     * Es la vía del modo ROM: Koopas CON caparazón y andar animado.
     */
    private val romEnemyFrames: Map<Int, List<Bitmap>>? = null,
    /**
     * Sprites GRANDES por id (assets/sprites/big/big_<id>.png): enemigos mayores de
     * 16×16 (Thwomp de piedra, fuego grande…). Se dibujan a su tamaño real anclados
     * por los pies y tienen PRIORIDAD sobre el resto; si falta el id, cae al render
     * normal (aditivo, no rompe nada).
     */
    private val bigSpriteBitmaps: Map<Int, Bitmap> = emptyMap(),
    /**
     * Hoja de la moneda animada real de SMW (bloque Map16 0x2B, 4 fotogramas de
     * 16×16 en fila = 64×16) horneada de la ROM (assets/sprites/coin.png). Es la
     * moneda que se dibuja para los ítems COIN colocados en el editor; si falta,
     * cae al cuadrado dorado que parpadea.
     */
    private val coinBitmap: Bitmap? = null,
    /**
     * Hoja de POWERUPS real de SMW (assets/sprites/powerups.png, 48×16 = seta|flor|pluma,
     * 16×16 cada uno) horneada de la ROM. Si falta, los powerups caen a los rectángulos.
     */
    private val powerupBitmap: Bitmap? = null,
) : GLSurfaceView.Renderer {

    @Volatile var inMoveX = 0f
    /** Eje vertical del joystick (-1 arriba .. +1 abajo): entra por tuberías/puertas. */
    @Volatile var inMoveY = 0f
    @Volatile var inRunning = false
    @Volatile var inJumpHeld = false

    /** Warp activado por el jugador (tubería/puerta); la UI lo consume y cambia de nivel. */
    /** true si el jugador ha SUPERADO el nivel (tocó la meta). Lo consulta la Activity al
     *  salir para devolver el resultado, y con eso el mapa del mundo aplica su evento. */
    val levelWon: Boolean get() = engine.won

    /** ¿Se ganó por la salida SECRETA (cerradura)? Abre un camino distinto en el mapa. */
    val levelWonSecret: Boolean get() = engine.wonSecret

    @Volatile var pendingWarp: com.rolebuilder.core.engine.platformer.WarpTarget? = null
        private set

    // Últimos contadores de eventos vistos, para sonar cada evento una sola vez.
    private var lastJumpEvents = 0
    private var lastStompEvents = 0
    private var lastDeathEvents = 0
    private var lastCoinEvents = 0
    private var lastPowerupEvents = 0
    private var lastDamageEvents = 0
    private var lastFireballEvents = 0

    /** Lo lee la UI para el aviso de "has muerto". */
    @Volatile var dead = false
        private set

    /** Monedas recogidas; lo lee la UI para el marcador (HUD). */
    @Volatile var coins = 0
        private set

    /** Físicas ACTUALES del motor: el panel de físicas las lee y las ajusta EN VIVO. */
    var tuning: com.rolebuilder.core.engine.platformer.PlatformerTuning
        get() = engine.tuning
        set(value) { engine.tuning = value }

    /** Muestra el overlay de COLISIÓN (hitboxes + rejilla de solidez + warps). */
    @Volatile var showHitboxes = false

    /** Alto de la caja de Mario PEQUEÑO (px); el panel lo ajusta conservando los pies. */
    var playerSmallHeight: Float
        get() = engine.smallHeight
        set(value) { engine.setSmallHeight(value) }

    /** Caja (cuadrada) de TODOS los enemigos en caliente (px). */
    fun setEnemyBoxSize(px: Float) {
        for (e in engine.enemies) { e.width = px; e.height = px }
    }

    /** Tamaño de caja actual de los enemigos (para inicializar el slider). */
    fun enemyBoxSize(): Float = engine.enemies.firstOrNull()?.width ?: 14f

    private lateinit var batch: SpriteBatch
    private lateinit var white: Texture
    private var marioTex: Texture? = null
    private var marioBigTex: Texture? = null
    private var marioFireTex: Texture? = null
    private var marioCapeTex: Texture? = null
    private var marioAnim = 0f
    private var enemyTex: Texture? = null
    private var romEnemyTex: Map<Int, List<Texture>> = emptyMap()
    private var bigTex: Map<Int, Pair<Texture, Bitmap>> = emptyMap()
    private var coinFrames: List<Texture> = emptyList()
    /** Texturas de powerup por tipo (0=seta, 1=flor, 2=pluma), de [powerupBitmap]. */
    private var powerupFrames: List<Texture> = emptyList()
    private var tilesetTex: Texture? = null
    private var animByTile: Map<Int, com.rolebuilder.core.model.TileAnimation> = emptyMap()
    private val camera = Camera2D()
    private var lastNanos = 0L
    private var acc = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        batch = SpriteBatch()
        white = Texture.white()
        marioTex = marioBitmap?.let { Texture(it) }
        marioBigTex = marioBigBitmap?.let { Texture(it) }
        marioFireTex = marioFireBitmap?.let { Texture(it) }
        marioCapeTex = marioCapeBitmap?.let { Texture(it) }
        enemyTex = enemyBitmap?.let { Texture(it) }
        romEnemyTex = romEnemyFrames?.mapValues { (_, frames) -> frames.map { Texture(it) } } ?: emptyMap()
        bigTex = bigSpriteBitmaps.mapValues { (_, bmp) -> Texture(bmp) to bmp }
        coinFrames = coinBitmap?.let { bmp ->
            val n = (bmp.width / 16).coerceAtLeast(1)
            (0 until n).map { fi ->
                Texture(Bitmap.createBitmap(bmp, fi * 16, 0, 16, minOf(16, bmp.height)))
            }
        } ?: emptyList()
        powerupFrames = powerupBitmap?.let { bmp ->
            val n = (bmp.width / 16).coerceAtLeast(1)
            (0 until n).map { fi ->
                Texture(Bitmap.createBitmap(bmp, fi * 16, 0, 16, minOf(16, bmp.height)))
            }
        } ?: emptyList()
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
            // Lanzar bola: sin SFX propio en el catálogo; un blip agudo y corto ("fwip").
            if (engine.fireballEvents > lastFireballEvents) a.play(com.rolebuilder.core.snes.SmwSfxCatalog.Event.STOMP, volume = 0.35f, rate = 1.9f)
            lastJumpEvents = engine.jumpEvents
            lastStompEvents = engine.stompEvents
            lastDeathEvents = engine.deathEvents
            lastCoinEvents = engine.coinEvents
            lastPowerupEvents = engine.powerupEvents
            lastDamageEvents = engine.damageEvents
            lastFireballEvents = engine.fireballEvents
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
            // Sin proyecto: pinta la colisión por colores (modo ROM cruda). Las RAMPAS
            // con forma se pintan como su TRIÁNGULO real (16 franjas de columna
            // siguiendo la altura del suelo), no como bloque entero.
            for (r in minR..maxR) {
                for (c in minC..maxC) {
                    val s = engine.solidity(c, r)
                    if (s == SmwSolidity.NONE) continue
                    val col = colorOf(s)
                    val slopeOff = engine.slopeOffsets(c, r)
                    if (slopeOff != null) {
                        for (x in 0 until 16) {
                            val off = slopeOff[x]
                            if (off >= 16) continue
                            batch.draw(
                                white,
                                c + x / 16f, r + off / 16f, 1f / 16f, (16 - off) / 16f,
                                r = col[0], g = col[1], b = col[2], a = 1f,
                            )
                        }
                        continue
                    }
                    batch.draw(white, c.toFloat(), r.toFloat(), 1f, 1f, r = col[0], g = col[1], b = col[2], a = 1f)
                }
            }
        }

        // Ítems COLOCADOS en el editor: monedas (sprite REAL de SMW girando, bloque
        // 0x2B de la ROM; si falta el asset, cuadrado dorado que parpadea) y meta
        // (poste con banderín verde).
        for (item in engine.placedItems) {
            if (item.collected) continue
            val ix = item.x / 16f
            val iy = item.y / 16f
            when (item.kind) {
                com.rolebuilder.core.engine.platformer.ItemKind.COIN -> {
                    if (coinFrames.isNotEmpty()) {
                        // Gira a la cadencia clásica: ~8 fotogramas de juego por cuadro (≈133 ms).
                        val fi = ((now / 133_000_000L) % coinFrames.size).toInt()
                        batch.draw(coinFrames[fi], ix, iy, 1f, 1f)
                    } else {
                        val blink = 0.85f + 0.15f * kotlin.math.sin(now / 120_000_000.0).toFloat()
                        batch.draw(white, ix + 0.28f, iy + 0.15f, 0.44f, 0.7f, r = 0.98f * blink, g = 0.82f * blink, b = 0.16f, a = 1f)
                    }
                }
                com.rolebuilder.core.engine.platformer.ItemKind.GOAL -> {
                    // El POSTE no se mueve: va en la casilla donde está sembrada la meta.
                    batch.draw(white, ix + 0.44f, iy - 1f, 0.12f, 2f, r = 0.85f, g = 0.85f, b = 0.9f, a = 1f)
                    // La CINTA sí: sube y baja como en SMW, y de su altura sale el bonus
                    // (el motor la mueve en tickGoalTapes; aquí solo se dibuja donde esté).
                    // Por eso se pinta en `drawY` y no en la Y sembrada.
                    val ty = item.drawY / 16f
                    batch.draw(white, ix - 0.1f, ty + 0.42f, 1.3f, 0.16f, r = 0.95f, g = 0.95f, b = 0.35f, a = 1f)
                    batch.draw(white, ix + 0.56f, ty + 0.1f, 0.5f, 0.4f, r = 0.15f, g = 0.8f, b = 0.3f, a = 1f)
                }
                // Salida SECRETA (cerradura): ojo de cerradura dorado, para distinguirla a
                // simple vista de la meta normal — llevan a sitios distintos del mapa.
                com.rolebuilder.core.engine.platformer.ItemKind.GOAL_SECRET -> {
                    batch.draw(white, ix + 0.2f, iy + 0.1f, 0.6f, 0.8f, r = 0.95f, g = 0.78f, b = 0.2f, a = 1f)
                    batch.draw(white, ix + 0.42f, iy + 0.3f, 0.16f, 0.42f, r = 0.15f, g = 0.12f, b = 0.1f, a = 1f)
                }
            }
        }

        // Enemigos: primero los fotogramas VIVOS de la ROM (Koopa CON caparazón, andar
        // animado a la cadencia real de SMW: cambia cada 8 fotogramas); si no, el atlas
        // horneado; si no, un rectángulo. Aplastados = franja fina al pisarlos.
        val etex = enemyTex
        for (e in engine.enemies) {
            if (!e.alive && e.squashTimer <= 0) continue
            if (e.hidden) continue // Planta Piraña metida en el tubo: no se dibuja
            // Koopa en su CAPARAZÓN (quieto o deslizándose): domo del color del Koopa.
            if (e.alive && e.shell) {
                val sx = e.x / 16f; val sw = e.width / 16f
                val sy = (e.y + e.height * 0.35f) / 16f; val sh = e.height * 0.65f / 16f
                val (r, g, b) = shellColor(e.koopaColorId)
                batch.draw(white, sx, sy, sw, sh, r = r, g = g, b = b, a = 1f)                       // domo
                batch.draw(white, sx + sw * 0.15f, sy + sh * 0.2f, sw * 0.7f, sh * 0.3f, r = r * 0.6f + 0.4f, g = g * 0.6f + 0.4f, b = b * 0.6f + 0.4f, a = 1f) // brillo
                batch.draw(white, sx, sy + sh * 0.7f, sw, sh * 0.3f, r = 0.98f, g = 0.9f, b = 0.6f, a = 1f) // reborde claro
                continue
            }
            val ex = e.x / 16f
            val ew = e.width / 16f
            // Una Koopa alada PISADA (sin alas) se dibuja como el Koopa de suelo (sin alas).
            val drawId = if (e.winged) e.id else e.koopaColorId
            val frame = ENEMY_FRAME[drawId]
            val live = romEnemyTex[drawId]
            val big = bigTex[drawId]
            if (e.alive && big != null) {
                // Sprite GRANDE (Thwomp, fuego grande…): a su tamaño real (celdas = px/16),
                // centrado en horizontal y anclado por los pies.
                val (tex, bmp) = big
                val wCells = bmp.width / 16f
                val hCells = bmp.height / 16f
                val cx = (e.x + e.width / 2f) / 16f - wCells / 2f
                val feet = (e.y + e.height) / 16f - hCells
                batch.draw(tex, cx, feet, wCells, hCells, flipX = e.vx > 0f)
            } else if (e.alive && live != null && live.isNotEmpty()) {
                // Fotogramas VIVOS anclados por los pies y centrados. El ancho sale del
                // bitmap: 16 px (1 casilla, andadores) o 32 px (2 casillas, Koopas aladas
                // con su ala). El andar/aleteo alterna a la cadencia real de SMW.
                val walkFrame = live[((now / ENEMY_STEP_NS) % live.size).toInt()]
                val wCells = walkFrame.width / 16f
                val hCells = walkFrame.height / 16f
                val cx = (e.x + e.width / 2f) / 16f - wCells / 2f
                val feet = (e.y + e.height) / 16f
                batch.draw(walkFrame, cx, feet - hCells, wCells, hCells, flipX = e.vx > 0f)
            } else if (e.alive && etex != null && frame != null) {
                // Atlas horneado: celda cuadrada (ATLAS_CELL = 2×2 casillas) anclada por los
                // pies y centrada, con ATLAS_FRAMES fotogramas apilados; el sprite entero
                // (aladas incluidas) sin partir. Fotograma vivo a la cadencia de SMW.
                val frames = ENEMY_FRAMES
                val fr = if (com.rolebuilder.core.snes.SmwEnemyGraphics.animFrameCount(e.id) > 1)
                    ((now / ENEMY_STEP_NS) % frames).toInt() else 0
                val cx = (e.x + e.width / 2f) / 16f - 1f
                val feet = (e.y + e.height) / 16f
                val u0 = frame * ENEMY_UV
                val v0 = fr * ENEMY_VH
                batch.draw(
                    etex, cx, feet - 2f, 2f, 2f,
                    u0 = u0, v0 = v0, u1 = u0 + ENEMY_UV, v1 = v0 + ENEMY_VH,
                    flipX = e.vx > 0f,
                )
            } else if (e.alive) {
                batch.draw(white, ex, e.y / 16f, ew, e.height / 16f, r = 0.65f, g = 0.20f, b = 0.55f, a = 1f)
            } else {
                val fh = e.height / 16f * 0.3f
                batch.draw(white, ex, (e.y + e.height * 0.7f) / 16f, ew, fh, r = 0.5f, g = 0.5f, b = 0.5f, a = 1f)
            }
        }

        // Powerups en marcha: si está la hoja REAL de la ROM (assets/sprites/powerups.png,
        // seta|flor|pluma), se dibuja el sprite correcto por tipo; si no, cae a los
        // rectángulos de motor.
        for (m in engine.items) {
            if (!m.alive) continue
            val mx = m.x / 16f
            val my = m.y / 16f
            val mw = m.width / 16f
            val mh = m.height / 16f
            val kind = m.kind
            val frameIdx = kind.ordinal // 0=seta, 1=flor, 2=pluma (orden de powerups.png)
            if (frameIdx < powerupFrames.size) {
                batch.draw(powerupFrames[frameIdx], mx, my, mw, mh)
            } else if (kind == com.rolebuilder.core.engine.platformer.PowerupKind.FIRE_FLOWER) {
                // Flor de fuego: pétalos naranjas y centro claro (dibujo de motor).
                batch.draw(white, mx, my, mw, mh * 0.6f, r = 1f, g = 0.45f, b = 0.10f, a = 1f)
                batch.draw(white, mx + mw * 0.3f, my + mh * 0.2f, mw * 0.4f, mh * 0.35f, r = 1f, g = 0.95f, b = 0.7f, a = 1f)
                batch.draw(white, mx + mw * 0.2f, my + mh * 0.6f, mw * 0.6f, mh * 0.4f, r = 0.25f, g = 0.7f, b = 0.25f, a = 1f)
            } else if (kind == com.rolebuilder.core.engine.platformer.PowerupKind.CAPE_FEATHER) {
                // Pluma de capa: cuerpo amarillo alargado con nervio claro (dibujo de motor).
                batch.draw(white, mx + mw * 0.25f, my, mw * 0.5f, mh, r = 1f, g = 0.85f, b = 0.10f, a = 1f)
                batch.draw(white, mx + mw * 0.45f, my + mh * 0.1f, mw * 0.1f, mh * 0.8f, r = 1f, g = 1f, b = 0.85f, a = 1f)
            } else {
                batch.draw(white, mx, my, mw, mh * 0.55f, r = 0.90f, g = 0.16f, b = 0.14f, a = 1f)
                batch.draw(white, mx + mw * 0.2f, my + mh * 0.55f, mw * 0.6f, mh * 0.45f, r = 1f, g = 0.9f, b = 0.75f, a = 1f)
            }
        }

        // Bolas de fuego en vuelo: núcleo claro con halo naranja (dibujo de motor).
        for (fb in engine.fireballs) {
            if (!fb.alive) continue
            val bx = fb.x / 16f
            val by = fb.y / 16f
            val bw = fb.width / 16f
            val bh = fb.height / 16f
            batch.draw(white, bx, by, bw, bh, r = 1f, g = 0.45f, b = 0.10f, a = 1f)
            batch.draw(white, bx + bw * 0.25f, by + bh * 0.25f, bw * 0.5f, bh * 0.5f, r = 1f, g = 0.95f, b = 0.6f, a = 1f)
        }

        // Bolas de fuego de la Planta Piraña de fuego: rojo-naranja con núcleo claro.
        for (fb in engine.enemyProjectiles) {
            if (!fb.alive) continue
            val bx = fb.x / 16f; val by = fb.y / 16f
            val bw = fb.width / 16f; val bh = fb.height / 16f
            batch.draw(white, bx, by, bw, bh, r = 0.95f, g = 0.25f, b = 0.10f, a = 1f)
            batch.draw(white, bx + bw * 0.25f, by + bh * 0.25f, bw * 0.5f, bh * 0.5f, r = 1f, g = 0.9f, b = 0.5f, a = 1f)
        }

        // Bloques de AGARRAR (quietos, llevados o lanzados): cubo gris con borde claro.
        for (b in engine.grabBlocks) {
            if (!b.alive) continue
            val bx = b.x / 16f; val by = b.y / 16f
            val bw = b.width / 16f; val bh = b.height / 16f
            batch.draw(white, bx, by, bw, bh, r = 0.55f, g = 0.5f, b = 0.42f, a = 1f)
            batch.draw(white, bx + bw * 0.15f, by + bh * 0.15f, bw * 0.7f, bh * 0.7f, r = 0.72f, g = 0.66f, b = 0.55f, a = 1f)
        }

        drawMario(dt)

        // Overlay de COLISIÓN por encima de todo (activado desde el panel ⚙).
        if (showHitboxes) drawHitboxes(minC, maxC, minR, maxR)

        batch.end()
    }

    /**
     * Overlay de HITBOXES: dibuja las cajas AABB REALES del motor — las mismas con las
     * que se resuelven los choques, no las de los sprites — y la rejilla de solidez:
     *  - celdas sólidas/un sentido/cuesta/pinchos con el contorno de su color,
     *  - celdas de warp en cian,
     *  - Mario en verde, enemigos en rojo, powerups en amarillo, bolas en naranja,
     *    monedas/meta colocadas en blanco.
     */
    private fun drawHitboxes(minC: Int, maxC: Int, minR: Int, maxR: Int) {
        val t = 0.07f // grosor del contorno, en casillas
        fun outline(x: Float, y: Float, w: Float, h: Float, r: Float, g: Float, b: Float, a: Float = 0.95f) {
            batch.draw(white, x, y, w, t, r = r, g = g, b = b, a = a)
            batch.draw(white, x, y + h - t, w, t, r = r, g = g, b = b, a = a)
            batch.draw(white, x, y + t, t, h - 2 * t, r = r, g = g, b = b, a = a)
            batch.draw(white, x + w - t, y + t, t, h - 2 * t, r = r, g = g, b = b, a = a)
        }
        // Rejilla de solidez visible, con el color de cada tipo. Las RAMPAS enseñan
        // su LÍNEA de superficie real (la que pisan los pies), no la caja del bloque.
        for (r in minR..maxR) for (c in minC..maxC) {
            val s = engine.solidity(c, r)
            if (s == SmwSolidity.NONE) continue
            val col = colorOf(s)
            val slopeOff = engine.slopeOffsets(c, r)
            if (slopeOff != null) {
                for (x in 0 until 16) {
                    val off = slopeOff[x]
                    if (off >= 16) continue
                    batch.draw(white, c + x / 16f, r + off / 16f, 1f / 16f, t, r = col[0], g = col[1], b = col[2], a = 0.95f)
                }
                continue
            }
            outline(c.toFloat(), r.toFloat(), 1f, 1f, col[0], col[1], col[2], 0.7f)
        }
        for (w in engine.warpCells) {
            if (w.col in minC..maxC && w.row in minR..maxR) {
                outline(w.col.toFloat(), w.row.toFloat(), 1f, 1f, 0.15f, 0.95f, 0.95f)
            }
        }
        val p = engine.player
        outline(p.x / 16f, p.y / 16f, engine.tuning.playerWidth / 16f, engine.playerHeight / 16f, 0.2f, 1f, 0.3f)
        for (e in engine.enemies) {
            if (e.alive) outline(e.x / 16f, e.y / 16f, e.width / 16f, e.height / 16f, 1f, 0.25f, 0.25f)
        }
        for (m in engine.items) {
            if (m.alive) outline(m.x / 16f, m.y / 16f, m.width / 16f, m.height / 16f, 1f, 0.9f, 0.2f)
        }
        for (fb in engine.fireballs) {
            if (fb.alive) outline(fb.x / 16f, fb.y / 16f, fb.width / 16f, fb.height / 16f, 1f, 0.6f, 0.1f)
        }
        for (pi in engine.placedItems) {
            if (!pi.collected) outline(pi.x / 16f, pi.y / 16f, pi.size / 16f, pi.size / 16f, 1f, 1f, 1f)
        }
    }

    /**
     * Dibuja a Mario. Con la hoja de la ROM ([marioTex]) pinta el SPRITE REAL: elige
     * el fotograma por el estado (parado/andando/corriendo/saltando), lo voltea según
     * hacia dónde mira y lo ancla por los pies. Sin hoja, cae al rectángulo.
     */
    private fun drawMario(dt: Float) {
        val p = engine.player
        // Invulnerable tras encoger: parpadea (se salta el dibujo en pulsos), como SMW.
        // Muerto NO parpadea: la caída de muerte se ve entera.
        if (!p.dead && p.invulnFrames > 0 && (p.invulnFrames / 4) % 2 == 1) return
        // Cada poder usa su PROPIA hoja: fuego (blanco) / capa (amarilla) → grande →
        // pequeño como reserva.
        val tex = when {
            p.fire -> marioFireTex ?: marioBigTex ?: marioTex
            p.cape -> marioCapeTex ?: marioBigTex ?: marioTex
            p.big -> marioBigTex ?: marioTex
            else -> marioTex
        }
        if (tex == null) {
            val w = engine.tuning.playerWidth / 16f
            val h = engine.playerHeight / 16f
            val red = if (p.dead) 0.4f else 1f
            batch.draw(white, p.x / 16f, p.y / 16f, w, h, r = red, g = 0.25f, b = 0.2f, a = 1f)
            return
        }
        // Cada fotograma es Mario ENTERO 16×32 (cabeza + cuerpo ya compuestos por
        // smwMarioSheet, con su cara). Poses de la hoja: 0 parado, 1 paso, 2/3 carrera
        // a tope (brazos extendidos), 4 en el aire. El ANDAR de SMW alterna parado↔paso
        // (poses 0/1); los brazos extendidos son SOLO la carrera a máxima velocidad —
        // usarlos para andar era lo que se veía raro.
        val running = abs(p.vx) > 2.2f
        if (p.onGround && abs(p.vx) > 0.2f) marioAnim += dt else if (p.onGround) marioAnim = 0f
        val phase = (marioAnim * (if (running) 14f else 9f)).toInt() % 2
        val fc = when {
            p.dead -> 4                                        // muerte: cae aspaventado
            !p.onGround -> 4                                   // saltando (pose inclinada)
            abs(p.vx) <= 0.2f -> 0                             // parado
            running -> 2 + phase                               // carrera a tope: brazos fuera
            else -> phase                                      // andar: parado↔paso, como SMW
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

    /** Color del caparazón según el Koopa (verde/rojo/azul/amarillo). */
    private fun shellColor(id: Int): Triple<Float, Float, Float> = when (id) {
        0x01 -> Triple(0.86f, 0.18f, 0.16f) // rojo
        0x02 -> Triple(0.20f, 0.42f, 0.90f) // azul
        0x03 -> Triple(0.95f, 0.82f, 0.12f) // amarillo
        else -> Triple(0.22f, 0.72f, 0.24f) // verde (0x00 / 0x05)
    }

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
        /** Ancho UV de una CELDA (un id) en el atlas de enemigos. */
        private val ENEMY_UV = 1f / com.rolebuilder.core.snes.SmwEnemyGraphics.curatedIds.size.toFloat()
        /** Nº de fotogramas apilados en vertical en el atlas, y su alto UV. */
        private val ENEMY_FRAMES = com.rolebuilder.core.snes.SmwEnemyGraphics.ATLAS_FRAMES
        private val ENEMY_VH = 1f / ENEMY_FRAMES.toFloat()

        /**
         * Cadencia del andar de los enemigos: SMW cambia de fotograma cada 8 ticks de
         * 60 fps (`spr_table1602 = (++contador & 8) != 0`) → 8/60 s por fotograma.
         */
        private const val ENEMY_STEP_NS = 8L * 1_000_000_000L / 60L
    }
}
