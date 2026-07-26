package com.rolebuilder.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rolebuilder.core.engine.platformer.BlockAction
import com.rolebuilder.core.engine.platformer.EnemySeed
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.engine.platformer.ProjectPlatformer
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwBlockBehavior
import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SmwPhysicsReader
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.player.ui.VirtualJoystick
import java.io.File

/**
 * Juega un nivel de SMW en el motor de plataformas: extrae de la ROM la colisión,
 * las físicas y el punto de inicio y lo conecta al renderer y a los controles
 * táctiles. Es el puente entre "la ROM nos entrega todo" y verlo jugándose.
 */
class PlatformerActivity : ComponentActivity() {

    private var music: PlatformerMusic? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        val projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH)
        val renderer = if (projectPath != null) {
            buildProjectRenderer(File(projectPath)) ?: run { finish(); return }
        } else {
            buildRomRenderer() ?: run { finish(); return }
        }
        // Música de fondo: motor N-SPC+S-DSP real de SMW, sintetizado en streaming. En
        // la ruta ROM (▶) suena la música REAL del nivel (su musicIndex del header);
        // si falla o es ruta proyecto, cae a la pista pre-horneada.
        music = (buildRomMusic() ?: PlatformerMusic.fromAssets(this))?.also { it.start() }
        setContent {
            PlatformerScreen(
                renderer,
                onRestart = { recreate() },
                // Al salir se devuelve si el nivel se SUPERÓ: es lo que deja al mapa del
                // mundo aplicar el evento de ese nivel y abrir el camino siguiente.
                onExit = {
                    setResult(RESULT_OK, Intent().putExtra(RESULT_WON, renderer.levelWon))
                    finish()
                },
                // Al morir, la música PARA (como SMW) y queda solo el jingle de muerte;
                // reiniciar (recreate) vuelve a arrancarla.
                onDeath = { music?.stop() },
                // Warp consumido (tubería/puerta): en modo ROM el destino es un número
                // de NIVEL de SMW; en modo proyecto, un id de MAPA del proyecto (con su
                // casilla de entrada). En ambos casos se relanza la actividad allí.
                onWarp = { dest ->
                    val romPath = intent.getStringExtra(EXTRA_ROM_PATH)
                    val projPath = intent.getStringExtra(EXTRA_PROJECT_PATH)
                    when {
                        romPath != null -> {
                            startActivity(intent(this, File(romPath), dest.destMapId))
                            finish()
                        }
                        projPath != null -> {
                            startActivity(
                                intentForProject(this, File(projPath), dest.destMapId)
                                    .putExtra(EXTRA_START_X, dest.destX)
                                    .putExtra(EXTRA_START_Y, dest.destY)
                            )
                            finish()
                        }
                    }
                },
            )
        }
    }

    override fun onDestroy() {
        music?.stop()
        music = null
        super.onDestroy()
    }

    /** Modo proyecto: juega el mapa del Platform Builder con sus tiles reales. */
    private fun buildProjectRenderer(projectDir: File): PlatformerRenderer? {
        return try {
            val project = ProjectIo.loadProject(projectDir)
            val database = ProjectIo.loadDatabase(projectDir)
            val mapId = intent.getIntExtra(EXTRA_MAP_ID, project.startMapId)
            val map = ProjectIo.loadMap(projectDir, mapId)
            val tileset = database.tileset(map.tilesetId) ?: database.tilesets.firstOrNull()
                ?: run {
                    Toast.makeText(this, "El proyecto no tiene tileset.", Toast.LENGTH_LONG).show()
                    return null
                }
            // Inicio: el del proyecto, o el punto de entrada del warp que nos trajo aquí.
            val startX = intent.getIntExtra(EXTRA_START_X, project.startX)
            val startY = intent.getIntExtra(EXTRA_START_Y, project.startY)
            // Perfiles de rampa AUTOMÁTICOS desde el DIBUJO de los tiles de cuesta:
            // los niveles ya importados y los tilesets propios funcionan sin más.
            val autoProfiles = autoSlopeProfiles(projectDir, tileset)
            val engine = ProjectPlatformer.engine(map, tileset, startX, startY, autoSlopeProfiles = autoProfiles)
            PlatformerRenderer(
                engine, PlatformerWorld(projectDir, map, tileset), loadMario(), loadEnemies(),
                PlatformerAudio.fromAssets(this),
                // Mario grande/fuego/capa con sus gráficos REALES horneados (no el pequeño
                // estirado): assets/sprites/mario_big|fire|cape.png.
                marioBigBitmap = loadMarioBig(), marioFireBitmap = loadMarioFire(),
                marioCapeBitmap = loadMarioCape(),
                bigSpriteBitmaps = loadBigSprites(), coinBitmap = loadCoin(),
                powerupBitmap = loadPowerups(),
            )
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar el proyecto: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /** Modo ROM: extrae el nivel de SMW (colisión de colores) y el sprite de Mario. */
    private fun buildRomRenderer(): PlatformerRenderer? {
        val romPath = intent.getStringExtra(EXTRA_ROM_PATH) ?: return null
        val level = intent.getIntExtra(EXTRA_LEVEL, 0x106)
        val rom = try {
            File(romPath).readBytes()
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo leer la ROM: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
        val engine = try {
            buildEngine(rom, level)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar el nivel: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
        if (engine == null) {
            Toast.makeText(this, "Faltan datos (colisión/físicas/inicio) para el nivel.", Toast.LENGTH_LONG).show()
            return null
        }
        // Sprite REAL de Mario (GFX32) COMPUESTO como el juego: cada pose apila cabeza
        // + cuerpo, así sale con cara (16×32 por fotograma). Coloreado en vivo desde la
        // ROM; si falla, drawMario cae al rectángulo. Enemigos desde el atlas horneado y
        // audio con las muestras reales de SMW (o assets si la ROM no lo permite).
        val header = SnesDecoder.parseHeader(rom)
        fun marioSheet(powerup: Int) = runCatching {
            val img = SnesGameRecipes.smwMarioSheet(rom, header, powerup) ?: return@runCatching null
            Bitmap.createBitmap(img.pixels, img.width, img.height, Bitmap.Config.ARGB_8888)
        }.getOrNull()
        val marioBmp = marioSheet(0)        // pequeño
        val marioBigBmp = marioSheet(1)     // grande (gráficos propios, no escalado)
        val marioFireBmp = marioSheet(3)    // fuego (gráficos de grande, paleta blanca)
        val marioCapeBmp = marioSheet(2)    // capa (gráficos con capa amarilla)
        // Gráficos VIVOS de enemigos del nivel: fotogramas de andar reales (las Koopas
        // CON su caparazón, 16×32 apilado). El atlas horneado queda de reserva para
        // los ids que no salgan.
        val enemyFrames: Map<Int, List<Bitmap>> = com.rolebuilder.core.snes.SnesGameRecipes
            .smwLevelEnemies(rom, header, level)
            .map { (id, _, _) -> id }.distinct()
            .mapNotNull { id ->
                runCatching {
                    val gfx = com.rolebuilder.core.snes.SmwEnemyGraphics
                    // Las Koopas aladas llevan su cuerpo+ala y las Plantas Piraña saltarinas
                    // su boca+hojas (celda ancha); el resto, sus fotogramas de andar. Todos
                    // se animan igual en el renderer.
                    val frames = when {
                        gfx.isWinged(id) -> gfx.wingedKoopaFrames(rom, header, level, id)
                        gfx.isJumpingPiranha(id) -> gfx.jumpingPiranhaFrames(rom, header, level, id)
                        else -> gfx.spriteFrames(rom, header, level, id)
                    }
                    frames?.map { Bitmap.createBitmap(it.pixels, it.width, it.height, Bitmap.Config.ARGB_8888) }
                }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { id to it }
            }.toMap()
        val audio = PlatformerAudio.fromRom(this, rom) ?: PlatformerAudio.fromAssets(this)
        return PlatformerRenderer(
            engine, null, marioBmp, loadEnemies(), audio,
            marioBigBmp, marioFireBmp, marioCapeBmp,
            romEnemyFrames = enemyFrames.ifEmpty { null },
            bigSpriteBitmaps = loadBigSprites(),
            coinBitmap = loadCoin(),
            powerupBitmap = loadPowerups(),
        )
    }

    /**
     * Música DERIVADA de la ROM para la ruta ▶ (solo modo ROM): lee el musicIndex del
     * header del nivel y reproduce SU canción real del banco de música de nivel. El
     * mapeo es songId = musicIndex + 1 (la canción 0 del banco es silencio; los niveles
     * estándar tienen musicIndex 0 → canción 1, el tema de nivel clásico). Null si no es
     * ruta ROM o la ROM no permite ensamblar la música → cae al asset pre-horneado.
     */
    private fun buildRomMusic(): PlatformerMusic? {
        val romPath = intent.getStringExtra(EXTRA_ROM_PATH) ?: return null
        val level = intent.getIntExtra(EXTRA_LEVEL, 0x106)
        val rom = runCatching { File(romPath).readBytes() }.getOrNull() ?: return null
        val header = SnesDecoder.parseHeader(rom)
        val musicIndex = SnesGameRecipes.smwLevelInfo(rom, header, level)?.musicIndex ?: return null
        return PlatformerMusic.fromRom(rom, musicIndex + 1)
    }

    /** Carga el sprite de Mario empaquetado (assets/sprites/mario.png), o null si falta. */
    /**
     * Perfiles de rampa AUTOMÁTICOS por tile: para cada tile de solidez "cuesta" SIN
     * forma explícita, deduce la altura del suelo del propio DIBUJO del tile
     * ([com.rolebuilder.core.snes.SmwSlopes.profileFromTilePixels]: primer píxel opaco
     * por columna). Es lo que hace las cuestas GENERALES: los niveles ya importados
     * funcionan sin reimportar y los tilesets dibujados a mano, sin configurar nada.
     * Si la silueta no es útil (tile macizo), la cuesta se queda como bloque (seguro).
     */
    private fun autoSlopeProfiles(projectDir: File, tileset: com.rolebuilder.core.model.Tileset): Map<Int, IntArray> {
        if (tileset.platformSolidity.isEmpty() || tileset.tileSize != 16) return emptyMap()
        val file = ProjectIo.imageFile(projectDir, tileset.image)
        val bmp = runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            ?: return emptyMap()
        val out = HashMap<Int, IntArray>()
        val px = IntArray(256)
        val slopeOrd = com.rolebuilder.core.snes.SmwSolidity.SLOPE.ordinal
        val steepOrd = com.rolebuilder.core.snes.SmwSolidity.SLOPE_STEEP.ordinal
        for (tile in 0 until tileset.tileCount) {
            val ord = tileset.platformSolidity.getOrNull(tile) ?: continue
            if (ord != slopeOrd && ord != steepOrd) continue
            val explicit = tileset.platformSlopeShape.getOrNull(tile)
                ?: com.rolebuilder.core.snes.SmwSlopes.NO_SLOPE
            if (com.rolebuilder.core.snes.SmwSlopes.floorOffsets(explicit) != null) continue
            val x = (tile % tileset.columns) * 16
            val y = (tile / tileset.columns) * 16
            if (x + 16 > bmp.width || y + 16 > bmp.height) continue
            bmp.getPixels(px, 0, 16, x, y, 16, 16)
            com.rolebuilder.core.snes.SmwSlopes.profileFromTilePixels(px)?.let { out[tile] = it.copyOf() }
        }
        return out
    }

    private fun loadMario(): android.graphics.Bitmap? = loadSprite("sprites/mario.png")

    /** Hojas de Mario GRANDE/FUEGO/CAPA horneadas de la ROM (gráficos propios, 16×32 por pose). */
    private fun loadMarioBig(): android.graphics.Bitmap? = loadSprite("sprites/mario_big.png")
    private fun loadMarioFire(): android.graphics.Bitmap? = loadSprite("sprites/mario_fire.png")
    private fun loadMarioCape(): android.graphics.Bitmap? = loadSprite("sprites/mario_cape.png")

    /** Carga el atlas de enemigos empaquetado (assets/sprites/enemies.png), o null si falta. */
    private fun loadEnemies(): android.graphics.Bitmap? = loadSprite("sprites/enemies.png")

    /** Hoja de POWERUPS real horneada (assets/sprites/powerups.png: seta|flor|pluma), o null. */
    private fun loadPowerups(): android.graphics.Bitmap? = loadSprite("sprites/powerups.png")

    /** Carga la hoja de la moneda animada real (assets/sprites/coin.png), o null si falta. */
    private fun loadCoin(): android.graphics.Bitmap? = loadSprite("sprites/coin.png")

    /**
     * Carga los sprites GRANDES empaquetados (assets/sprites/big/big_<id>.png): id de
     * sprite → bitmap. El id se lee del nombre del fichero (hex). Vacío si no hay carpeta.
     */
    private fun loadBigSprites(): Map<Int, android.graphics.Bitmap> = runCatching {
        (assets.list("sprites/big") ?: emptyArray()).mapNotNull { name ->
            val id = Regex("big_([0-9a-fA-F]+)\\.png").matchEntire(name)?.groupValues?.get(1)
                ?.toInt(16) ?: return@mapNotNull null
            loadSprite("sprites/big/$name")?.let { id to it }
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun loadSprite(path: String): android.graphics.Bitmap? = runCatching {
        com.rolebuilder.editor.snes.SmwAssetStore.open(this, path)
            ?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
    }.getOrNull()

    /** Extrae de la ROM lo necesario y monta el motor, o null si no es SMW jugable. */
    private fun buildEngine(rom: ByteArray, level: Int): PlatformerEngine? {
        val header = SnesDecoder.parseHeader(rom)
        val col = SnesGameRecipes.smwLevelCollision(rom, header, level) ?: return null
        val phys = SmwPhysicsReader.read(rom, header) ?: return null
        val start = SmwLevelStartReader.read(rom, header, level) ?: return null
        // Bloques interactivos REALES del nivel (monedas y bloques `?`): clasifica cada
        // bloque Map16 del mapa de colisión con la misma rutina que el juego, igualando
        // esta ruta con la de mapas importados.
        var anyAction = false
        val actions = IntArray(col.cols * col.rows) { i ->
            when (SmwBlockBehavior.classify(col.blocks[i])) {
                SmwBlockAction.COIN -> BlockAction.COIN.ordinal.also { anyAction = true }
                SmwBlockAction.QUESTION -> BlockAction.PRIZE.ordinal.also { anyAction = true }
                else -> BlockAction.NONE.ordinal
            }
        }
        // Enemigos reales del nivel (lista de sprites de la ROM), recortados al mapa. Se
        // EXCLUYE la meta: la cinta/esfera/cerradura viven en la misma lista de sprites, pero
        // no son bichos — van como ítem de META más abajo.
        val goalIds = com.rolebuilder.core.snes.SmwLevelGoal.GOAL_SPRITES
        val enemySeeds = SnesGameRecipes.smwLevelEnemies(rom, header, level)
            .filter { (id, x, y) -> id !in goalIds && x in 0 until col.cols && y in 0 until col.rows }
            .map { (id, x, y) -> EnemySeed(x * 16, y * 16, id) }
        // META del nivel: con esto tocar la cinta marca el nivel como SUPERADO y el mapa del
        // mundo puede disparar su evento. Los niveles sin meta (castillos, casas) no siembran.
        val goalSeeds = com.rolebuilder.core.snes.SmwLevelGoal
            .goalCells(rom, SnesGameRecipes.smwHeaderDeltaPublic(header), level)
            .filter { (x, y) -> x in 0 until col.cols && y in 0 until col.rows }
            .map { (x, y) ->
                com.rolebuilder.core.engine.platformer.ItemSeed(
                    x * 16, y * 16, com.rolebuilder.core.engine.platformer.ItemKind.GOAL,
                )
            }
        // Los bloques `?` son "block code" (el terreno los da como NONE) pero en el
        // juego son SÓLIDOS: se fuerza su solidez, como hace ProjectPlatformer. Es
        // inmutable a propósito: un `?` ya usado sigue siendo sólido.
        val prizeCells = HashSet<Int>()
        for (i in actions.indices) if (actions[i] == BlockAction.PRIZE.ordinal) prizeCells.add(i)
        // Warps REALES del nivel (puertas y tuberías verticales cruzadas con sus salidas
        // de pantalla): al entrar, la UI recarga la actividad en el nivel destino.
        val warps = com.rolebuilder.core.snes.SmwWarpTiles.levelWarps(rom, header, level).map {
            com.rolebuilder.core.engine.platformer.EngineWarp(
                col = it.xTile, row = it.yTile,
                input = if (it.enterDown) com.rolebuilder.core.engine.platformer.WarpInput.DOWN
                else com.rolebuilder.core.engine.platformer.WarpInput.UP,
                destMapId = it.destLevel, destX = it.destXTile, destY = it.destYTile,
            )
        }
        return PlatformerEngine(
            cols = col.cols, rows = col.rows,
            solidityAt = { c, r ->
                if (r * col.cols + c in prizeCells) com.rolebuilder.core.snes.SmwSolidity.SOLID
                else col.solidityAt(c, r)
            },
            startPixelX = start.startPixelX, startPixelY = start.startPixelY,
            tuning = PlatformerTuning.fromSmw(phys),
            // Modo 1:1: el manejo horizontal de Mario usa las tablas REALES de la ROM.
            smwPhysics = phys,
            enemySeeds = enemySeeds,
            itemSeeds = goalSeeds,
            blockActions = if (anyAction) actions else null,
            warps = warps,
            // Formas de RAMPA reales de la ROM (tabla $00:E55E): las cuestas se juegan
            // como rampas, con deslizamiento.
            slopeOffsetsAt = { c, r ->
                com.rolebuilder.core.snes.SmwSlopes.floorOffsets(col.slopeShapeAt(c, r))
            },
        )
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val EXTRA_ROM_PATH = "romPath"
        private const val EXTRA_LEVEL = "level"
        private const val EXTRA_PROJECT_PATH = "projectPath"
        private const val EXTRA_MAP_ID = "mapId"
        private const val EXTRA_START_X = "startX"
        private const val EXTRA_START_Y = "startY"

        /** Extra del resultado: true si el jugador SUPERO el nivel (toco la meta). */
        const val RESULT_WON = "won"

        fun intent(context: Context, romFile: File, level: Int): Intent =
            Intent(context, PlatformerActivity::class.java)
                .putExtra(EXTRA_ROM_PATH, romFile.absolutePath)
                .putExtra(EXTRA_LEVEL, level)

        /** Juega un proyecto de Platform Builder por su carpeta (mapa de inicio o [mapId]). */
        fun intentForProject(context: Context, projectDir: File, mapId: Int? = null): Intent =
            Intent(context, PlatformerActivity::class.java)
                .putExtra(EXTRA_PROJECT_PATH, projectDir.absolutePath)
                .apply { if (mapId != null) putExtra(EXTRA_MAP_ID, mapId) }
    }
}

@Composable
private fun PlatformerScreen(
    renderer: PlatformerRenderer,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onWarp: (com.rolebuilder.core.engine.platformer.WarpTarget) -> Unit = {},
    /** Primera vez que Mario muere: la actividad para la música (suena el jingle). */
    onDeath: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var glView by remember { mutableStateOf<GLSurfaceView?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        DisposableEffect(lifecycleOwner, glView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> glView?.onPause()
                    Lifecycle.Event.ON_RESUME -> glView?.onResume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                renderer.releaseAudio()
            }
        }

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            // Marcador de monedas (HUD) y warp pendiente: sondea el renderer.
            var coinCount by remember { mutableStateOf(0) }
            var warped by remember { mutableStateOf(false) }
            var died by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    coinCount = renderer.coins
                    val warp = renderer.pendingWarp
                    if (warp != null && !warped) { warped = true; onWarp(warp) }
                    if (renderer.dead && !died) { died = true; onDeath() }
                    kotlinx.coroutines.delay(120)
                }
            }
            Text(
                "🪙 $coinCount",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            // Joystick: eje X mueve; eje Y entra por tuberías (abajo) y puertas (arriba).
            VirtualJoystick(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                onChange = { x, y -> renderer.inMoveX = x; renderer.inMoveY = y },
            )

            // Salto (mantener = salto más alto) y correr.
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                HoldButton("Correr", Color(0xFF4D79FF)) { renderer.inRunning = it }
                HoldButton("Salto", Color(0xFFFF5D5D)) { renderer.inJumpHeld = it }
            }

            TextButton(onClick = onRestart, modifier = Modifier.align(Alignment.TopEnd)) {
                Text("↺", color = Color.White, fontSize = 22.sp)
            }
            TextButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart)) {
                Text("Salir", color = Color.White.copy(alpha = 0.8f))
            }

            // Panel de FÍSICAS en vivo: ver los valores reales del motor y ajustarlos
            // con sliders MIENTRAS se juega (joystick a la izquierda, panel a la
            // derecha); "Original" restaura los valores de la ROM/proyecto.
            var showPhysics by remember { mutableStateOf(false) }
            val originalTuning = remember { renderer.tuning }
            TextButton(
                onClick = { showPhysics = !showPhysics },
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 52.dp),
            ) {
                Text("⚙", color = Color.White, fontSize = 20.sp)
            }
            if (showPhysics) {
                PhysicsPanel(
                    renderer = renderer,
                    original = originalTuning,
                    onClose = { showPhysics = false },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, top = 48.dp, bottom = 96.dp),
                )
            }
        }
    }
}

/** Botón que informa true mientras se mantiene pulsado y false al soltar. */
@Composable
private fun HoldButton(label: String, color: Color, onHold: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .background(color.copy(alpha = 0.35f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().also { it.consume() }
                    onHold(true)
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                    }
                    onHold(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Panel de FÍSICAS en vivo: muestra los valores REALES que mueven el motor (los
 * extraídos de la ROM de SMW o los del proyecto) y los ajusta con sliders mientras
 * se juega — cada cambio se aplica al fotograma siguiente, así se SIENTE al
 * instante. "Original" restaura los valores con los que arrancó el nivel. Las
 * dimensiones de la caja no se tocan (cambiarlas en caliente atraviesa paredes).
 */
@Composable
private fun PhysicsPanel(
    renderer: PlatformerRenderer,
    original: PlatformerTuning,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var t by remember { mutableStateOf(renderer.tuning) }
    fun apply(n: PlatformerTuning) { t = n; renderer.tuning = n }

    // Cajas de colisión (hitboxes): estado de los sliders y valores de arranque.
    var showBoxes by remember { mutableStateOf(renderer.showHitboxes) }
    val originalSmallH = remember { renderer.playerSmallHeight }
    val originalEnemyBox = remember { renderer.enemyBoxSize() }
    var smallH by remember { mutableStateOf(renderer.playerSmallHeight) }
    var enemyBox by remember { mutableStateOf(renderer.enemyBoxSize()) }

    Column(
        modifier = modifier
            .width(290.dp)
            .fillMaxHeight()
            .background(Color(0xE0101024), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Físicas", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            TextButton(onClick = {
                apply(original)
                smallH = originalSmallH; renderer.playerSmallHeight = originalSmallH
                enemyBox = originalEnemyBox; renderer.setEnemyBoxSize(originalEnemyBox)
            }) {
                Text("Original", color = Color(0xFFFFD54D), fontSize = 13.sp)
            }
            TextButton(onClick = onClose) { Text("✕", color = Color.White) }
        }
        // El salto se edita como IMPULSO positivo (más = salta más alto); el motor
        // lo guarda negativo (hacia arriba).
        PhysSlider("Impulso de salto", -t.jumpSpeed, 2f..8f) { apply(t.copy(jumpSpeed = -it)) }
        PhysSlider("Gravedad (caída)", t.gravityFall, 0.05f..1f) { apply(t.copy(gravityFall = it)) }
        PhysSlider("Gravedad (salto mantenido)", t.gravityHold, 0.02f..1f) { apply(t.copy(gravityHold = it)) }
        PhysSlider("Caída máxima", t.maxFallSpeed, 1f..8f) { apply(t.copy(maxFallSpeed = it)) }
        PhysSlider("Velocidad andando", t.maxWalkSpeed, 0.5f..3f) { apply(t.copy(maxWalkSpeed = it)) }
        PhysSlider("Velocidad corriendo", t.maxRunSpeed, 1f..5f) { apply(t.copy(maxRunSpeed = it)) }
        PhysSlider("Aceleración", t.runAccel, 0.02f..0.4f) { apply(t.copy(runAccel = it)) }
        PhysSlider("Rozamiento", t.friction, 0.02f..0.8f) { apply(t.copy(friction = it)) }

        // ------------------------------------------------ cajas de colisión (hitboxes)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cajas de colisión", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            Switch(checked = showBoxes, onCheckedChange = { showBoxes = it; renderer.showHitboxes = it })
        }
        Text(
            "Ver las cajas REALES del motor sobre el juego: Mario (verde), enemigos " +
                "(rojo), powerups (amarillo), bolas (naranja), warps (cian) y la " +
                "rejilla de solidez con el color de cada tipo.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
        )
        PhysSlider("Ancho de Mario (px)", t.playerWidth, 6f..16f) { apply(t.copy(playerWidth = it)) }
        PhysSlider("Alto de Mario pequeño (px)", smallH, 6f..24f) {
            smallH = it; renderer.playerSmallHeight = it
        }
        PhysSlider("Caja de enemigos (px)", enemyBox, 6f..20f) {
            enemyBox = it; renderer.setEnemyBoxSize(it)
        }

        Text(
            "Valores en píxeles/fotograma a 60 fps, como el juego original.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
        )
    }
}

/** Slider etiquetado del panel de físicas, con el valor actual a dos decimales. */
@Composable
private fun PhysSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Text("$label · ${"%.2f".format(value)}", color = Color.White, fontSize = 12.sp)
    Slider(value = value, onValueChange = onChange, valueRange = range)
}
