package com.rolebuilder.player

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.engine.platformer.ProjectPlatformer
import com.rolebuilder.core.io.ProjectIo
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        val projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH)
        val renderer = if (projectPath != null) {
            buildProjectRenderer(File(projectPath)) ?: run { finish(); return }
        } else {
            buildRomRenderer() ?: run { finish(); return }
        }
        setContent { PlatformerScreen(renderer, onRestart = { recreate() }, onExit = { finish() }) }
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
            val engine = ProjectPlatformer.engine(map, tileset, project.startX, project.startY)
            PlatformerRenderer(engine, PlatformerWorld(projectDir, map, tileset), loadMario(), loadEnemies(), PlatformerAudio.fromAssets(this))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar el proyecto: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /** Modo ROM: extrae el nivel de SMW y lo pinta por colisión de colores. */
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
        // Audio con las muestras reales de SMW (o null si la ROM no lo permite).
        val audio = PlatformerAudio.fromRom(this, rom) ?: PlatformerAudio.fromAssets(this)
        return PlatformerRenderer(engine, null, loadMario(), loadEnemies(), audio)
    }

    /** Carga el sprite de Mario empaquetado (assets/sprites/mario.png), o null si falta. */
    private fun loadMario(): android.graphics.Bitmap? = loadSprite("sprites/mario.png")

    /** Carga el atlas de enemigos empaquetado (assets/sprites/enemies.png), o null si falta. */
    private fun loadEnemies(): android.graphics.Bitmap? = loadSprite("sprites/enemies.png")

    private fun loadSprite(path: String): android.graphics.Bitmap? = runCatching {
        assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it) }
    }.getOrNull()

    /** Extrae de la ROM lo necesario y monta el motor, o null si no es SMW jugable. */
    private fun buildEngine(rom: ByteArray, level: Int): PlatformerEngine? {
        val header = SnesDecoder.parseHeader(rom)
        val col = SnesGameRecipes.smwLevelCollision(rom, header, level) ?: return null
        val phys = SmwPhysicsReader.read(rom, header) ?: return null
        val start = SmwLevelStartReader.read(rom, header, level) ?: return null
        return PlatformerEngine(
            cols = col.cols, rows = col.rows,
            solidityAt = { c, r -> col.solidityAt(c, r) },
            startPixelX = start.startPixelX, startPixelY = start.startPixelY,
            tuning = PlatformerTuning.fromSmw(phys),
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
private fun PlatformerScreen(renderer: PlatformerRenderer, onRestart: () -> Unit, onExit: () -> Unit) {
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
            // Movimiento horizontal con el joystick (solo el eje X).
            VirtualJoystick(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                onChange = { x, _ -> renderer.inMoveX = x },
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
