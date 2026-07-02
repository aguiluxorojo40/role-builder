package com.rolebuilder.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rolebuilder.core.engine.GameState
import com.rolebuilder.core.engine.RpgEngine
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.SaveIo
import com.rolebuilder.player.ui.ActionButton
import com.rolebuilder.player.ui.ChoicesPanel
import com.rolebuilder.player.ui.HeartsRow
import com.rolebuilder.player.ui.MessagePanel
import com.rolebuilder.player.ui.VirtualJoystick
import java.io.File
import kotlinx.coroutines.isActive

/** Ejecuta un proyecto RPG a pantalla completa. */
class PlayerActivity : ComponentActivity() {

    private lateinit var engine: RpgEngine
    private lateinit var soundFx: SoundFx
    private lateinit var projectDir: File
    private lateinit var saveFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        val path = intent.getStringExtra(EXTRA_PROJECT_DIR)
        if (path == null) {
            finish()
            return
        }
        projectDir = File(path)
        saveFile = File(projectDir, "saves/slot1.json")

        val data = try {
            ProjectIo.loadFull(projectDir)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar el proyecto: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val state = if (intent.getBooleanExtra(EXTRA_CONTINUE, false) && saveFile.exists()) {
            SaveIo.load(saveFile)
        } else {
            GameState.newGame(data.project, data.database)
        }

        engine = RpgEngine(data, state)
        soundFx = SoundFx(this)

        setContent {
            PlayerScreen(
                engine = engine,
                projectDir = projectDir,
                soundFx = soundFx,
                onSave = {
                    SaveIo.save(saveFile, engine.state)
                    Toast.makeText(this, "Partida guardada", Toast.LENGTH_SHORT).show()
                },
                onExit = { finish() },
                onRetry = { recreate() },
            )
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundFx.isInitialized) soundFx.release()
    }

    companion object {
        private const val EXTRA_PROJECT_DIR = "projectDir"
        private const val EXTRA_CONTINUE = "continueSave"

        fun intent(context: Context, projectDir: File, continueSave: Boolean = false): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_PROJECT_DIR, projectDir.absolutePath)
                .putExtra(EXTRA_CONTINUE, continueSave)
    }
}

@Composable
private fun PlayerScreen(
    engine: RpgEngine,
    projectDir: File,
    soundFx: SoundFx,
    onSave: () -> Unit,
    onExit: () -> Unit,
    onRetry: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var menuOpen by remember { mutableStateOf(false) }

    // Re-lee el estado del motor una vez por frame de UI.
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            androidx.compose.runtime.withFrameMillis { }
            frame++
        }
    }

    var glView by remember { mutableStateOf<GLSurfaceView?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    setRenderer(GameRenderer(engine, projectDir) { soundFx.play(it) })
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
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        @Suppress("UNUSED_EXPRESSION")
        frame // fuerza la recomposición del HUD cada frame

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            HeartsRow(
                hp = engine.state.hp,
                maxHp = engine.state.maxHp,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )

            IconButton(
                onClick = {
                    menuOpen = true
                    engine.paused = true
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = Color.White.copy(alpha = 0.8f))
            }

            VirtualJoystick(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                onChange = { x, y ->
                    engine.inputX = x
                    engine.inputY = y
                },
            )

            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ActionButton("B", Color(0xFF4D79FF)) { engine.pressSecondary() }
                ActionButton("A", Color(0xFFFF5D5D)) { engine.pressAction() }
            }
        }

        engine.choices?.let { choices ->
            ChoicesPanel(choices = choices, onSelect = { engine.selectChoice(it) })
        }

        engine.message?.let { message ->
            MessagePanel(text = message.text, speaker = message.speaker, onDismiss = { engine.dismissMessage() })
        }

        if (menuOpen) {
            PauseMenu(
                engine = engine,
                onSave = onSave,
                onExit = onExit,
                onDismiss = {
                    menuOpen = false
                    engine.paused = false
                },
            )
        }

        if (engine.gameOver) {
            GameOverOverlay(onRetry = onRetry, onExit = onExit)
        }
    }
}

@Composable
private fun PauseMenu(
    engine: RpgEngine,
    onSave: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xF0121A30), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Menú", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            val items = engine.state.items.toList()
            if (items.isEmpty()) {
                Text("No llevas objetos.", color = Color.White.copy(alpha = 0.7f))
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items.forEach { (itemId, count) ->
                        val item = engine.data.database.item(itemId)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${item?.name ?: "Objeto $itemId"} ×$count", color = Color.White)
                            TextButton(onClick = { engine.useItem(itemId) }) {
                                Text("Usar")
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSave) { Text("Guardar") }
                Button(onClick = onDismiss) { Text("Seguir") }
                TextButton(onClick = onExit) { Text("Salir") }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(onRetry: () -> Unit, onExit: () -> Unit) {
    val activity = LocalContext.current as? Activity
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Fin de la aventura", color = Color(0xFFFF6B6B), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(onClick = onRetry) { Text("Reintentar") }
            TextButton(onClick = { onExit(); activity?.finish() }) { Text("Salir", color = Color.White) }
        }
    }
}
