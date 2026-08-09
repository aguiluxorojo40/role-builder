package com.rolebuilder.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwBlockBehavior
import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SmwPhysicsReader
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesHeader
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

    /**
     * Dibuja el HUD REAL de SMW (tiempo, monedas, vidas, puntos) → bitmap de 256×16, o null
     * si no hay ROM (modo proyecto) o no trae los GFX de Layer 3. Lo prepara
     * [buildRomRenderer]; el HUD de texto queda como respaldo.
     */
    private var hudArt: ((Int, Int, Int, Int) -> Bitmap?)? = null

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
        // la ruta ROM (▶) suena la música REAL del nivel (su musicIndex del header); en modo
        // PROYECTO, la del mapa importado (su platformMusicIndex); si nada de eso, la canción
        // de nivel por defecto del banco horneado.
        music = (buildRomMusic() ?: buildProjectMusic() ?: PlatformerMusic.fromAssets(this))
            ?.also { it.start() }
        setContent {
            PlatformerScreen(
                renderer,
                onRestart = { recreate() },
                // Al salir se devuelve si el nivel se SUPERÓ: es lo que deja al mapa del
                // mundo aplicar el evento de ese nivel y abrir el camino siguiente.
                onExit = {
                    setResult(
                        RESULT_OK,
                        Intent()
                            .putExtra(RESULT_WON, renderer.levelWon)
                            // Por QUÉ salida: el mapa abre un camino u otro (normal vs secreta).
                            .putExtra(RESULT_WON_SECRET, renderer.levelWonSecret)
                            // Murió si está muerto y NO llegó a la meta: al mapa le cuesta una vida.
                            .putExtra(RESULT_DIED, renderer.dead && !renderer.levelWon),
                    )
                    finish()
                },
                // Al morir, la música PARA (como SMW) y queda solo el jingle de muerte;
                // reiniciar (recreate) vuelve a arrancarla.
                onDeath = { music?.stop() },
                hudArt = hudArt,
                lives = intent.getIntExtra(EXTRA_LIVES, -1).coerceAtLeast(0),
                // Diagnóstico del HUD: 0 = no cargó (recarga la ROM), 1 = cargada muda, 2 = sonando.
                musicState = {
                    val m = music
                    when {
                        m == null -> 0
                        m.isPlaying() -> 2
                        else -> 1
                    }
                },
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
                // Caparazones HORNEADOS: sin ROM, esta es la unica via para que el modo
                // proyecto enseñe el caparazon real en vez del domo de color.
                romShellFrames = loadShellFrames(),
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
                        // Enemigos con DIBUJO PROPIO (Rex, Blurp, Super Koopa…): su aspecto NO
                        // sale de la tabla OAM genérica. Sin esta rama caían al `else` y se
                        // pintaban con la entrada genérica de su id, que para ellos es basura
                        // (Rex salía como un manchón naranja en vez del dinosaurio). Y como esta
                        // vía tiene prioridad sobre el atlas horneado en el renderer, el gráfico
                        // bueno no llegaba a usarse nunca.
                        id in gfx.customEnemyIds -> gfx.customEnemyImage(rom, header, level, id)?.let { listOf(it) }
                        else -> gfx.spriteFrames(rom, header, level, id)
                    }
                    frames?.map { Bitmap.createBitmap(it.pixels, it.width, it.height, Bitmap.Config.ARGB_8888) }
                }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { id to it }
            }.toMap()
        // Caparazones REALES de la ROM, por color. Se piden para los cuatro Koopa CON
        // caparazón (0x04-0x07) y no solo para los que hay en el nivel: un caparazón puede
        // llegar rodando desde otra pantalla, y son cuatro imágenes de 16×16.
        // HUD REAL de SMW: se PREPARA aquí (descomprimir la fuente de Layer 3 y armar la
        // paleta es caro y se hace una sola vez); repintarlo al cambiar un contador es solo
        // pegar teselas. Si la ROM no trae los GFX, se queda null y el HUD cae al de texto.
        val barra = runCatching {
            com.rolebuilder.core.snes.SmwStatusBar.prepare(rom, header, level)
        }.getOrNull()
        if (barra != null) {
            hudArt = { time, coins, lives, score ->
                runCatching {
                    val img = barra.render(time, coins, lives, score)
                    Bitmap.createBitmap(img.pixels, img.width, img.height, Bitmap.Config.ARGB_8888)
                }.getOrNull()
            }
        }

        val shellFrames: Map<Int, List<Bitmap>> = (0x04..0x07).mapNotNull { id ->
            runCatching {
                com.rolebuilder.core.snes.SmwEnemyGraphics.shellFrames(rom, header, level, id)
                    ?.map { Bitmap.createBitmap(it.pixels, it.width, it.height, Bitmap.Config.ARGB_8888) }
            }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { id to it }
        }.toMap()
        // RELOJ del nivel: el tiempo sale de su cabecera (200/300/400; 0 = sin tiempo,
        // como Yoshi's House). Sin esto el HUD no tendría qué contar y el bonus de
        // tiempo al acabar sería siempre 0.
        runCatching { SnesGameRecipes.smwLevelInfo(rom, header, level)?.startTime }
            .getOrNull()?.let { if (it > 0) engine.startTimer(it) }

        // Sprites GRANDES con la paleta de ESTE nivel. Los horneados salen todos del nivel
        // de referencia (0x106), así que al jugar otro nivel las plataformas de guía o Rex
        // saldrían con colores de Yoshi's Island 2. Aquí ya estamos leyendo la ROM en vivo,
        // así que no hay motivo para conformarse: se recalculan los ids que USA el nivel y
        // se superponen a los horneados, que quedan de reserva para el resto.
        val liveBig: Map<Int, Bitmap> = com.rolebuilder.core.snes.SnesGameRecipes
            .smwLevelEnemies(rom, header, level)
            .map { (id, _, _) -> id }.distinct()
            .mapNotNull { id ->
                runCatching {
                    com.rolebuilder.core.snes.SmwEnemyGraphics
                        .customEnemyImage(rom, header, level, id)
                        ?.let { img -> id to Bitmap.createBitmap(img.pixels, img.width, img.height, Bitmap.Config.ARGB_8888) }
                }.getOrNull()
            }.toMap()
        val audio = PlatformerAudio.fromRom(this, rom) ?: PlatformerAudio.fromAssets(this)
        // MUNDO de dibujo: el mismo atlas de tiles REAL que la importación al editor, para que
        // "▶ Jugar" se vea con sus gráficos (y su fondo de Layer 2) en vez de la colisión por
        // colores. Si el nivel no se reconstruye, queda null y el renderer cae a la vista de
        // colisión (comportamiento anterior). El MOTOR sigue saliendo de [buildEngine].
        val world = buildRomWorld(rom, header, level)
        return PlatformerRenderer(
            engine, world, marioBmp, loadEnemies(), audio,
            marioBigBmp, marioFireBmp, marioCapeBmp,
            romEnemyFrames = enemyFrames.ifEmpty { null },
            romShellFrames = shellFrames.ifEmpty { null },
            bigSpriteBitmaps = loadBigSprites() + liveBig,
            coinBitmap = loadCoin(),
            powerupBitmap = loadPowerups(),
        )
    }

    /**
     * MUNDO de dibujo para el modo ROM: rasteriza el nivel [level] a su atlas de tiles REAL
     * ([SnesGameRecipes.extractSmwLevelAsMap], el mismo que usa la importación al editor) y lo
     * deja como [PlatformerWorld] sobre la caché, para que "▶ Jugar" dibuje sus gráficos (y el
     * fondo de Layer 2) en vez de la colisión por colores.
     *
     * El MOTOR sigue saliendo de la colisión de la ROM ([buildEngine]); esto SOLO añade el
     * dibujo. Es seguro superponerlos porque [extractSmwLevelAsMap] y [smwLevelCollision]
     * calculan la rejilla IGUAL (cols = min(lastCol+2, totalCols); 27 filas), así el tile de
     * cada casilla cae sobre su colisión. `maxCols = 512` (máximo de un nivel = 32 pantallas)
     * garantiza que el ancho del mundo iguala al del motor.
     *
     * Devuelve null si el nivel no se reconstruye → el renderer cae a la vista de colisión
     * (comportamiento anterior), sin romper nada.
     */
    private fun buildRomWorld(rom: ByteArray, header: SnesHeader, level: Int): PlatformerWorld? {
        val m = runCatching { SnesGameRecipes.extractSmwLevelAsMap(rom, header, level, maxCols = 512) }
            .getOrNull() ?: return null
        // Atlas → PNG en la caché (images/), que es donde el renderer busca la imagen del tileset.
        val dir = File(cacheDir, "smw_play_world")
        val fileName = "smw_%03X_tiles.png".format(level)
        val bmp = Bitmap.createBitmap(m.atlas.width, m.atlas.height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(m.atlas.pixels, 0, m.atlas.width, 0, 0, m.atlas.width, m.atlas.height)
        val dest = ProjectIo.imageFile(dir, fileName)
        dest.parentFile?.mkdirs()
        dest.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val tileset = Tileset(
            id = 1, name = "SMW", image = fileName, tileSize = 16,
            columns = m.columns, rows = m.rows,
            passable = m.passable, platformSolidity = m.solidity,
            platformSlopeShape = m.slopeShapes, animations = m.animations,
            platformBlockActions = m.blockActions,
        )
        // Capa 0 = fondo (Layer 2) si lo hay; capa 1 = primer plano. Igual que importSmwLevelMap.
        val layers = if (m.bgTiles.isNotEmpty()) listOf(m.bgTiles, m.tiles)
            else listOf(m.tiles, List(m.mapWidth * m.mapHeight) { EMPTY_TILE })
        val map = GameMap(
            id = 0, name = "SMW", width = m.mapWidth, height = m.mapHeight,
            tilesetId = 1, layers = layers,
        )
        return PlatformerWorld(dir, map, tileset)
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
        // El ajuste 0..7 de la cabecera indexa LevelMusicTable → id de canción real ($1DFB).
        return PlatformerMusic.fromRom(rom, com.rolebuilder.core.snes.SmwMusic.levelSongId(musicIndex))
    }

    /**
     * Música para el modo PROYECTO (mapa importado, sin ROM). Si el mapa guarda su índice de
     * música de SMW ([GameMap.platformMusicIndex], que rellena la importación de niveles), toca
     * ESA canción del banco de música horneado (songId = índice + 1) — así cada nivel importado
     * suena SU tema (atlético, castillo, subterráneo…), no el genérico. Null si no es modo
     * proyecto o el mapa no trae índice → cae a la canción de nivel por defecto.
     */
    private fun buildProjectMusic(): PlatformerMusic? {
        val projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH) ?: return null
        val dir = File(projectPath)
        return runCatching {
            val startId = ProjectIo.loadProject(dir).startMapId
            val mapId = intent.getIntExtra(EXTRA_MAP_ID, startId)
            val idx = ProjectIo.loadMap(dir, mapId).platformMusicIndex
            if (idx < 0) null
            else PlatformerMusic.fromAssets(this, songId = com.rolebuilder.core.snes.SmwMusic.levelSongId(idx))
        }.getOrNull()
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

    /**
     * Caparazones horneados (sprites/shells.png): una FILA por Koopa con caparazón (0x04-0x07)
     * y una COLUMNA por fotograma del ciclo de giro, en celdas de 16×16. Se trocea a la misma
     * forma que devuelve `SmwEnemyGraphics.shellFrames`, para que el renderer no distinga de
     * dónde vienen. Es lo que hace que el modo PROYECTO —que no tiene ROM— enseñe el
     * caparazón de verdad y no el domo de color.
     */
    private fun loadShellFrames(): Map<Int, List<Bitmap>>? {
        val hoja = loadSprite("sprites/shells.png") ?: return null
        val cell = com.rolebuilder.core.snes.SmwBakedAssets.SHELL_CELL
        val columnas = hoja.width / cell
        val filas = hoja.height / cell
        if (columnas <= 0 || filas <= 0) return null
        val out = LinkedHashMap<Int, List<Bitmap>>()
        for (fila in 0 until minOf(filas, 4)) {
            val frames = (0 until columnas).mapNotNull { c ->
                runCatching { Bitmap.createBitmap(hoja, c * cell, fila * cell, cell, cell) }.getOrNull()
            }
            if (frames.isNotEmpty()) out[0x04 + fila] = frames
        }
        return out.ifEmpty { null }
    }

    /** Hoja de POWERUPS real horneada (assets/sprites/powerups.png: seta|flor|pluma), o null. */
    private fun loadPowerups(): android.graphics.Bitmap? = loadSprite("sprites/powerups.png")

    /** Carga la hoja de la moneda animada real (assets/sprites/coin.png), o null si falta. */
    private fun loadCoin(): android.graphics.Bitmap? = loadSprite("sprites/coin.png")

    /**
     * Carga los sprites GRANDES (id de sprite → bitmap): jefes, Rex, Wiggler, las
     * plataformas de guía… todo lo que no cabe en el atlas cuadrado `enemies.png`.
     *
     * Salen del ALMACÉN horneado en el dispositivo desde la ROM del usuario, no de los
     * assets del APK: en el repositorio no va ni un PNG de estos porque son de Nintendo.
     * Antes esta función ENUMERABA con `assets.list("sprites/big")` —una carpeta que no
     * existe en el APK, justamente por eso— aunque luego leyera del almacén, así que
     * devolvía SIEMPRE un mapa vacío y al jugar no salía ni un sprite grande. El editor
     * ya lo hacía bien ([com.rolebuilder.editor.loadBigSprites]); ahora los dos usan la
     * misma vía. Vacío si aún no se ha horneado.
     */
    private fun loadBigSprites(): Map<Int, android.graphics.Bitmap> = runCatching {
        com.rolebuilder.editor.snes.SmwAssetStore.bigSpriteFiles(this).mapNotNull { (id, file) ->
            android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.let { id to it }
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
        // Se usa la vista CON estado: los que el juego coloca aturdidos (0xDA/0xDB de la
        // lista) nacen ya dentro de su caparazón, no andando.
        val enemySeeds = SnesGameRecipes.smwLevelEnemySeeds(rom, header, level)
            .filter { it.id !in goalIds && it.xTile in 0 until col.cols && it.yTile in 0 until col.rows }
            .map { EnemySeed(it.xTile * 16, it.yTile * 16, it.id, it.stunned) }
        // META del nivel: con esto tocar la cinta marca el nivel como SUPERADO y el mapa del
        // mundo puede disparar su evento. Los niveles sin meta (castillos, casas) no siembran.
        // Cada meta va con SU tipo de salida: la cerradura siembra GOAL_SECRET (salida secreta),
        // la cinta/esfera GOAL. Así el motor sabe por cuál se acabó y el mapa abre el camino real.
        val goalSeeds = com.rolebuilder.core.snes.SmwLevelGoal
            .goalCellsWithKind(rom, SnesGameRecipes.smwHeaderDeltaPublic(header), level)
            .filter { it.xTile in 0 until col.cols && it.yTile in 0 until col.rows }
            .map { g ->
                com.rolebuilder.core.engine.platformer.ItemSeed(
                    g.xTile * 16, g.yTile * 16,
                    if (g.kind == com.rolebuilder.core.snes.SmwLevelGoal.ExitKind.SECRET)
                        com.rolebuilder.core.engine.platformer.ItemKind.GOAL_SECRET
                    else com.rolebuilder.core.engine.platformer.ItemKind.GOAL,
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

        /**
         * Vidas que le quedan al jugador, para el HUD. Las lleva el mapa del mundo
         * (`SmwGameSave.lives`), no el motor de plataformas, asi que viajan por el intent.
         * Si no viene, el HUD las omite en vez de inventarse un numero.
         */
        private const val EXTRA_LIVES = "lives"


        /** Extra del resultado: true si el jugador SUPERO el nivel (toco la meta). */
        const val RESULT_WON = "won"

        /** Extra del resultado: true si se ganó por la salida SECRETA (cerradura). */
        const val RESULT_WON_SECRET = "won_secret"

        /** Extra del resultado: true si el jugador MURIO en el nivel (cuesta una vida). */
        const val RESULT_DIED = "died"

        fun intent(context: Context, romFile: File, level: Int, lives: Int = -1): Intent =
            Intent(context, PlatformerActivity::class.java)
                .putExtra(EXTRA_LIVES, lives)
                .putExtra(EXTRA_ROM_PATH, romFile.absolutePath)
                .putExtra(EXTRA_LEVEL, level)

        /** Juega un proyecto de Platform Builder por su carpeta (mapa de inicio o [mapId]). */
        fun intentForProject(context: Context, projectDir: File, mapId: Int? = null): Intent =
            Intent(context, PlatformerActivity::class.java)
                .putExtra(EXTRA_PROJECT_PATH, projectDir.absolutePath)
                .apply { if (mapId != null) putExtra(EXTRA_MAP_ID, mapId) }
    }
}

/**
 * Fraccion del ancho que ocupa el HUD real de SMW. A pantalla completa (1f) la barra sale
 * enorme en apaisado y tapa los botones de "Salir" y recargar, que viven en las esquinas de
 * arriba. Con 0.62 queda holgada a los lados y conserva una altura parecida a la del juego.
 *
 * Vive a nivel de FICHERO, no en el companion de la actividad: [PlatformerScreen] es una
 * funcion top-level y no ve los miembros privados de la clase.
 */
private const val HUD_ANCHO_MAXIMO = 0.62f

@Composable
private fun PlatformerScreen(
    renderer: PlatformerRenderer,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onWarp: (com.rolebuilder.core.engine.platformer.WarpTarget) -> Unit = {},
    /** Primera vez que Mario muere: la actividad para la música (suena el jingle). */
    onDeath: () -> Unit = {},
    /**
     * Estado de la música para el indicador de diagnóstico del HUD: 0 = no cargó (ARAM sin
     * hornear → recarga la ROM), 1 = cargada pero en silencio (problema de audio del móvil),
     * 2 = sonando. Ayuda a saber POR QUÉ no se oye sin tener el dispositivo delante.
     */
    musicState: () -> Int = { 2 },
    /**
     * Dibuja el HUD REAL de SMW (tiempo, monedas, vidas, puntos) a un bitmap de 256×16 px.
     * Si es null —modo proyecto, o ROM sin los GFX de Layer 3— se usa el HUD de texto.
     */
    hudArt: ((Int, Int, Int, Int) -> Bitmap?)? = null,
    /** Vidas restantes para el HUD (las sabe el mapa del mundo, no el motor). */
    lives: Int = 0,
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
            var scoreNow by remember { mutableStateOf(0) }
            var timeNow by remember { mutableStateOf(0) }
            // Las vidas las lleva el mapa del mundo, no el motor: llegan por el intent.
            val livesNow = remember { lives }
            var starsNow by remember { mutableStateOf(0) }
            // Diagnóstico de dibujo de enemigos (vacío si el interruptor está apagado).
            var enemyDebugLines by remember { mutableStateOf(emptyList<String>()) }
            var wonNow by remember { mutableStateOf(false) }
            var warped by remember { mutableStateOf(false) }
            var died by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    coinCount = renderer.coins
                    scoreNow = renderer.score
                    timeNow = renderer.timeLeft
                    starsNow = renderer.bonusStars
                    enemyDebugLines = renderer.enemyDrawInfo
                    wonNow = renderer.won
                    val warp = renderer.pendingWarp
                    if (warp != null && !warped) { warped = true; onWarp(warp) }
                    if (renderer.dead && !died) { died = true; onDeath() }
                    kotlinx.coroutines.delay(120)
                }
            }
            // Diagnóstico de música: 🎵 suena · 🔈 cargada pero muda (audio del móvil) ·
            // 🔇 no cargó (recarga la ROM en la app para hornear la banda sonora).
            var musicIcon by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                // Un pequeño margen para que el secuenciador arranque antes de juzgar.
                kotlinx.coroutines.delay(600)
                musicIcon = when (musicState()) {
                    0 -> "🔇"
                    1 -> "🔈"
                    else -> "🎵"
                }
            }
            // HUD REAL de SMW: la barra de estado del juego (fuente de Layer 3, dos filas),
            // ESCALADA al ancho de la pantalla. Se redibuja solo cuando cambia un contador,
            // no cada frame. Sin ROM se cae al HUD de texto de más abajo.
            val hudBmp = hudArt?.let { pintar ->
                remember(coinCount, scoreNow, timeNow, livesNow) {
                    pintar(timeNow, coinCount, livesNow, scoreNow)?.asImageBitmap()
                }
            }
            if (hudBmp != null) {
                // Escalado al ancho, pero con TOPE: a pantalla completa la barra sale
                // gigante y se come el botón "Salir" y el de recargar. El HUD original
                // ocupa ~1/12 del alto de la pantalla de SNES, así que se le deja como
                // mucho ese sitio y se centra.
                Image(
                    bitmap = hudBmp,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(HUD_ANCHO_MAXIMO)
                        .padding(top = 2.dp),
                    // Pixel art: sin interpolación, o los bordes salen borrosos al ampliar.
                    filterQuality = FilterQuality.None,
                    contentScale = ContentScale.FillWidth,
                )
                // El diagnóstico de música vive en el HUD de texto; con el HUD real hay que
                // sacarlo aparte o se perdería (y es lo que dice POR QUÉ no suena).
                if (musicIcon.isNotEmpty()) {
                    Text(
                        musicIcon,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 8.dp),
                    )
                }
            } else Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "🪙 $coinCount",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                // MARCADOR. En SMW el número se muestra con un 0 de más (el marcador va
                // en decenas), así que aquí se enseña tal cual lo cuenta el motor.
                Text(
                    "★ $scoreNow",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                // RELOJ. Solo si el nivel lleva tiempo (Yoshi's House no). Se pone ROJO
                // al cruzar el aviso de SMW, que es a 100.
                if (timeNow > 0) {
                    Text(
                        "⏱ $timeNow",
                        color = if (timeNow <= com.rolebuilder.core.snes.SmwScore.HURRY_UP_AT)
                            Color(0xFFFF5D5D) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (musicIcon.isNotEmpty()) {
                    Text(musicIcon, fontSize = 18.sp)
                }
            }

            // Diagnóstico de enemigos: qué id hay y por qué vía se dibuja cada uno. Abajo a
            // la izquierda para no tapar el HUD ni los botones.
            if (enemyDebugLines.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                ) {
                    Text("Enemigos (id · cuántos · vía)", color = Color(0xFFFFD54F), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    for (linea in enemyDebugLines.take(12)) {
                        Text(linea, color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // ---------------------------- COURSE CLEAR ----------------------------
            // Al tocar la meta, el juego no corta: hace el RECUENTO. Pasa el bonus de
            // tiempo al marcador de 100 en 100 (de 10 en 10 al final) y las estrellas de
            // una en una, con la cadencia real de GiveTimeBonusAndBonusStars.
            if (wonNow) {
                val sc = com.rolebuilder.core.snes.SmwScore
                var pendiente by remember { mutableStateOf(-1) }
                var marcador by remember { mutableStateOf(0) }
                var estrellas by remember { mutableStateOf(0) }
                LaunchedEffect(wonNow) {
                    // El bonus se congela al ganar: es el reloj que quedaba en ese momento.
                    pendiente = sc.timeBonus(timeNow)
                    marcador = scoreNow
                    estrellas = 0
                    while (pendiente > 0) {
                        val paso = sc.tallyStep(pendiente)
                        pendiente -= paso
                        marcador += paso
                        kotlinx.coroutines.delay(16)
                    }
                    // Las estrellas van después, una cada 4 fotogramas (~66 ms).
                    while (estrellas < starsNow) {
                        estrellas++
                        kotlinx.coroutines.delay(
                            (sc.BONUS_STAR_TRANSFER_PERIOD * 1000L / 60L).coerceAtLeast(1L),
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xCC101018)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "COURSE CLEAR!",
                            color = Color(0xFFFFE066),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(18.dp))
                        Text("⏱ BONUS  ${pendiente.coerceAtLeast(0)}", color = Color.White, fontSize = 20.sp)
                        Text("★ $marcador", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "ESTRELLAS  $estrellas / $starsNow",
                            color = Color(0xFF9AE6B4),
                            fontSize = 18.sp,
                        )
                    }
                }
            }

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

            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Visualizador de FÍSICAS: superpone las hitboxes (Mario verde, enemigos rojos,
                // ítems amarillos), la rejilla de solidez coloreada por tipo, la línea de
                // superficie de las rampas y las bocas de warp. Para depurar el tacto del motor.
                var showHb by remember { mutableStateOf(renderer.showHitboxes) }
                TextButton(onClick = { showHb = !showHb; renderer.showHitboxes = showHb }) {
                    Text(
                        "🔧",
                        color = if (showHb) Color(0xFF7CFF7C) else Color.White.copy(alpha = 0.55f),
                        fontSize = 20.sp,
                    )
                }
                TextButton(onClick = onRestart) {
                    Text("↺", color = Color.White, fontSize = 22.sp)
                }
            }
            TextButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart)) {
                Text("Salir", color = Color.White.copy(alpha = 0.8f))
            }

            // Al morir, un panel claro: reintentar el nivel o volver al mapa (que descuenta
            // la vida). Sin esto, morir dejaba a Mario tirado esperando que adivinaras qué
            // botón diminuto pulsar.
            if (died) {
                Column(
                    modifier = Modifier.align(Alignment.Center)
                        .background(Color(0xE6101018), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Has perdido una vida", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onRestart) { Text("Reintentar") }
                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555B70)),
                        ) { Text("Volver al mapa") }
                    }
                }
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
    var showEnemyDebug by remember { mutableStateOf(renderer.debugEnemyDraw) }
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
        // ------------------------------------- diagnostico de dibujo de enemigos
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Diagnóstico de enemigos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            Switch(
                checked = showEnemyDebug,
                onCheckedChange = { showEnemyDebug = it; renderer.debugEnemyDraw = it },
            )
        }
        Text(
            "Dice POR QUÉ VÍA se dibuja cada enemigo del nivel (gráfico propio, fotogramas " +
                "vivos de la ROM, atlas horneado o rectángulo por falta de gráfico), con su id " +
                "y cuántos hay. Sirve para saber si un enemigo sale raro porque le falta el " +
                "gráfico o porque se está usando el equivocado, en vez de deducirlo mirando.",
            color = Color(0xFFBBBBBB), fontSize = 12.sp,
        )
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
