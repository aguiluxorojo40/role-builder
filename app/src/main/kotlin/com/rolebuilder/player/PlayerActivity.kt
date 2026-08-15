package com.rolebuilder.player

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.rolebuilder.core.io.LoadedProject
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.SaveIo
import com.rolebuilder.core.engine.ShopSession
import com.rolebuilder.core.model.EquipSlot
import com.rolebuilder.core.model.MusicTracks
import com.rolebuilder.player.ui.ActionButton
import com.rolebuilder.player.ui.ChoicesPanel
import com.rolebuilder.player.ui.HeartsRow
import com.rolebuilder.player.ui.MessagePanel
import com.rolebuilder.player.ui.VirtualJoystick
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Información de una ranura de guardado. */
data class SaveSlot(val index: Int, val file: File, val savedAt: Long?)

/** Ejecuta un proyecto RPG: pantalla de título, partida y guardado en ranuras. */
class PlayerActivity : ComponentActivity() {

    private lateinit var projectDir: File
    private lateinit var soundFx: SoundFx
    private val musicPlayer = MusicPlayer()

    /** Última música solicitada, para reanudarla al volver de segundo plano. */
    private var requestedBgm: String? = null

    private fun requestBgm(track: String?) {
        requestedBgm = track
        if (track == null) musicPlayer.stop() else musicPlayer.play(track)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        val path = intent.getStringExtra(EXTRA_PROJECT_DIR)
        if (path == null) {
            finish()
            return
        }
        projectDir = File(path)

        // Proyectos creados con versiones anteriores: completa las imágenes
        // de plantilla que falten (espada, haz de corte...).
        runCatching { com.rolebuilder.project.ProjectStore.ensureDefaultImages(this, projectDir) }

        val cargado = try {
            ProjectIo.loadFull(projectDir)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar el proyecto: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        soundFx = SoundFx(this)

        setContent {
            // Estado OBSERVABLE, no un campo de la actividad: antes "Guardar estilo
            // visual" hacía `data = data.copy(...)` sobre un campo normal, que Compose
            // no observa, así que la interfaz no se recomponía nunca. No se notaba
            // porque el renderer lee sus valores en vivo y disimulaba el fallo, pero
            // todo lo que se dibujara desde `data` seguía enseñando lo viejo.
            var data by remember { mutableStateOf(cargado) }

            PlayerRoot(
                data = data,
                slots = { saveSlots() },
                onBgm = ::requestBgm,
                soundFx = soundFx,
                onSaveToSlot = { engine, slot ->
                    SaveIo.save(slot.file, engine.state)
                    Toast.makeText(this, "Guardado en ranura ${slot.index}", Toast.LENGTH_SHORT).show()
                },
                onSaveVisual = { strength, tilt, spriteStand ->
                    val updated = data.project.copy(
                        hd2dStrength = strength,
                        dioramaTilt = tilt,
                        spriteStand = spriteStand,
                    )
                    ProjectIo.saveProject(projectDir, updated)
                    data = data.copy(project = updated)
                    Toast.makeText(this, "Estilo visual guardado en el proyecto", Toast.LENGTH_SHORT).show()
                },
                onExit = { finish() },
            )
        }
    }

    private fun saveSlots(): List<SaveSlot> = (1..SLOT_COUNT).map { index ->
        val file = File(projectDir, "saves/slot$index.json")
        SaveSlot(index, file, if (file.exists()) file.lastModified() else null)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestedBgm != null) musicPlayer.play(requestedBgm!!)
    }

    override fun onPause() {
        super.onPause()
        musicPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundFx.isInitialized) soundFx.release()
        musicPlayer.release()
    }

    companion object {
        private const val EXTRA_PROJECT_DIR = "projectDir"
        const val SLOT_COUNT = 3

        fun intent(context: Context, projectDir: File): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_PROJECT_DIR, projectDir.absolutePath)
    }
}

// =============================================================================
// Raíz: título <-> partida
// =============================================================================

@Composable
private fun PlayerRoot(
    data: LoadedProject,
    slots: () -> List<SaveSlot>,
    onBgm: (String?) -> Unit,
    soundFx: SoundFx,
    onSaveToSlot: (RpgEngine, SaveSlot) -> Unit,
    onSaveVisual: (Float, Float, Float) -> Unit,
    onExit: () -> Unit,
) {
    var engine by remember { mutableStateOf<RpgEngine?>(null) }

    val current = engine
    if (current == null) {
        LaunchedEffect(Unit) { onBgm(MusicTracks.TITLE) }
        TitleScreen(
            gameName = data.project.name,
            slots = slots(),
            onNewGame = {
                engine = RpgEngine(data, GameState.newGame(data.project, data.database))
            },
            onContinue = { slot ->
                runCatching { SaveIo.load(slot.file) }
                    .onSuccess { engine = RpgEngine(data, it) }
            },
            onExit = onExit,
        )
    } else {
        GameScreen(
            engine = current,
            projectDir = data.dir,
            soundFx = soundFx,
            onBgm = onBgm,
            slots = slots,
            onSaveToSlot = { slot -> onSaveToSlot(current, slot) },
            onSaveVisual = onSaveVisual,
            onNewGame = {
                engine = RpgEngine(data, GameState.newGame(data.project, data.database))
            },
            onBackToTitle = { engine = null },
            onExit = onExit,
        )
    }
}

// =============================================================================
// Pantalla de título
// =============================================================================

@Composable
private fun TitleScreen(
    gameName: String,
    slots: List<SaveSlot>,
    onNewGame: () -> Unit,
    onContinue: (SaveSlot) -> Unit,
    onExit: () -> Unit,
) {
    var showSlots by remember { mutableStateOf(false) }
    val hasSaves = slots.any { it.savedAt != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B1230), Color(0xFF1A1040), Color(0xFF060810))),
            )
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            gameName,
            color = Color(0xFFFFC94D),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            "⚔",
            fontSize = 34.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(vertical = 10.dp),
        )
        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onNewGame, modifier = Modifier.width(230.dp)) { Text("Nueva partida") }
            Button(
                onClick = { showSlots = true },
                enabled = hasSaves,
                modifier = Modifier.width(230.dp),
            ) { Text("Continuar") }
            TextButton(onClick = onExit) { Text("Salir", color = Color.White.copy(alpha = 0.7f)) }
        }
        Text(
            "hecho con Role Builder",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 30.dp),
        )
    }

    if (showSlots) {
        SlotPickerDialog(
            title = "Continuar partida",
            slots = slots,
            emptySelectable = false,
            onSelect = { slot ->
                showSlots = false
                onContinue(slot)
            },
            onDismiss = { showSlots = false },
        )
    }
}

// =============================================================================
// Selector de ranuras (guardar / continuar)
// =============================================================================

@Composable
private fun SlotPickerDialog(
    title: String,
    slots: List<SaveSlot>,
    emptySelectable: Boolean,
    onSelect: (SaveSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xF0121A30), RoundedCornerShape(16.dp))
                .padding(18.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            slots.forEach { slot ->
                val enabled = emptySelectable || slot.savedAt != null
                val label = slot.savedAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "— Vacía —"
                Button(
                    onClick = { onSelect(slot) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ranura ${slot.index} · $label") }
            }
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    }
}

// =============================================================================
// Pantalla de juego
// =============================================================================

@Composable
private fun GameScreen(
    engine: RpgEngine,
    projectDir: File,
    soundFx: SoundFx,
    onBgm: (String?) -> Unit,
    slots: () -> List<SaveSlot>,
    onSaveToSlot: (SaveSlot) -> Unit,
    onSaveVisual: (Float, Float, Float) -> Unit,
    onNewGame: () -> Unit,
    onBackToTitle: () -> Unit,
    onExit: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var menuOpen by remember { mutableStateOf(false) }
    var showSaveSlots by remember { mutableStateOf(false) }
    val renderer = remember(engine) {
        GameRenderer(engine, projectDir, onSound = { soundFx.play(it) }, onBgmChange = onBgm)
    }

    // Re-lee el estado del motor una vez por frame de UI.
    var frame by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(engine) {
        while (isActive) {
            androidx.compose.runtime.withFrameMillis { }
            frame++
            engine.notice?.let {
                notice = it
                engine.notice = null
            }
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2500)
            notice = null
        }
    }

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
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        @Suppress("UNUSED_EXPRESSION")
        frame // fuerza la recomposición del HUD cada frame

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                HeartsRow(hp = engine.state.hp, maxHp = engine.state.maxHp)
                Text(
                    "Nv ${engine.state.level} · \uD83E\uDE99 ${engine.state.gold}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                )
            }

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

        notice?.let { text ->
            Text(
                text,
                color = Color(0xFFFFE082),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Color(0xB0101426), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }

        engine.shop?.let { session ->
            ShopDialog(engine = engine, session = session)
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
                renderer = renderer,
                onSave = { showSaveSlots = true },
                onSaveVisual = onSaveVisual,
                onBackToTitle = {
                    menuOpen = false
                    onBackToTitle()
                },
                onExit = onExit,
                onDismiss = {
                    menuOpen = false
                    engine.paused = false
                },
            )
        }

        if (showSaveSlots) {
            SlotPickerDialog(
                title = "Guardar partida",
                slots = slots(),
                emptySelectable = true,
                onSelect = { slot ->
                    onSaveToSlot(slot)
                    showSaveSlots = false
                },
                onDismiss = { showSaveSlots = false },
            )
        }

        if (engine.gameOver) {
            GameOverOverlay(onRetry = onNewGame, onBackToTitle = onBackToTitle)
        }
    }
}

@Composable
private fun PauseMenu(
    engine: RpgEngine,
    renderer: GameRenderer,
    onSave: () -> Unit,
    onSaveVisual: (Float, Float, Float) -> Unit,
    onBackToTitle: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var strength by remember { mutableFloatStateOf(renderer.liveStrength) }
    var tilt by remember { mutableFloatStateOf(renderer.liveTilt) }
    var spriteStand by remember { mutableFloatStateOf(renderer.liveSpriteStand) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xF0121A30), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Menú", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // Potenciómetros del estilo visual: se aplican en vivo tras el diálogo.
            if (engine.data.project.hd2d) {
                Text(
                    "Efecto HD-2D · ${(strength * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
                Slider(
                    value = strength,
                    onValueChange = {
                        strength = it
                        renderer.liveStrength = it
                    },
                    valueRange = 0f..2f,
                )
                Text(
                    "Inclinación 2.5D · ${tilt.toInt()}°",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
                Slider(
                    value = tilt,
                    onValueChange = {
                        tilt = it
                        renderer.liveTilt = it
                    },
                    valueRange = 0f..60f,
                )
                Text(
                    "Altura de sprites · ${(spriteStand * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
                Slider(
                    value = spriteStand,
                    onValueChange = {
                        spriteStand = it
                        renderer.liveSpriteStand = it
                    },
                    valueRange = 0.5f..2.5f,
                )
                TextButton(onClick = { onSaveVisual(strength, tilt, spriteStand) }) {
                    Text("Guardar estilo en el proyecto")
                }
            }

            Text(
                "Nv ${engine.state.level} · ATQ ${engine.effectiveAttack} · DEF ${engine.effectiveDefense} · \uD83E\uDE99 ${engine.state.gold}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
            )

            // La foto que publica el motor, NO `state.items`: ese mapa lo muta el hilo
            // del motor al recoger objetos o aplicar una orden, y recorrerlo desde el
            // hilo de UI es la ConcurrentModificationException que la cola de comandos
            // viene a evitar (ver RpgEngine.inventory).
            val items = engine.inventory
            val equippedIds = listOfNotNull(engine.state.weaponItemId, engine.state.armorItemId)
            if (items.isEmpty() && equippedIds.isEmpty()) {
                Text("No llevas objetos.", color = Color.White.copy(alpha = 0.7f))
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Equipado actualmente (se puede quitar).
                    equippedIds.forEach { itemId ->
                        val item = engine.data.database.item(itemId) ?: return@forEach
                        val slot = item.equipSlot ?: return@forEach
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⚔ ${item.name} (equipado)", color = Color(0xFFB9D0FF))
                            TextButton(onClick = { engine.equip(slot, null) }) { Text("Quitar") }
                        }
                    }
                    items.forEach { (itemId, count) ->
                        val item = engine.data.database.item(itemId)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${item?.name ?: "Objeto $itemId"} ×$count", color = Color.White)
                            when {
                                item?.equipSlot != null -> TextButton(onClick = {
                                    engine.equip(item.equipSlot!!, itemId)
                                }) { Text("Equipar") }
                                else -> TextButton(onClick = { engine.useItem(itemId) }) { Text("Usar") }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSave) { Text("Guardar") }
                Button(onClick = onDismiss) { Text("Seguir") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onBackToTitle) { Text("Título") }
                TextButton(onClick = onExit) { Text("Salir") }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(onRetry: () -> Unit, onBackToTitle: () -> Unit) {
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
            TextButton(onClick = onBackToTitle) { Text("Volver al título", color = Color.White) }
        }
    }
}

// =============================================================================
// Tienda
// =============================================================================

@Composable
private fun ShopDialog(engine: RpgEngine, session: ShopSession) {
    Dialog(onDismissRequest = { engine.closeShop() }) {
        Column(
            modifier = Modifier
                .background(Color(0xF0121A30), RoundedCornerShape(16.dp))
                .padding(18.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Tienda", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Tu oro: \uD83E\uDE99 ${engine.state.gold}",
                color = Color(0xFFFFE082),
                fontSize = 14.sp,
            )

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Comprar:", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                session.itemIds.forEach { itemId ->
                    val item = engine.data.database.item(itemId) ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${item.name} · ${item.price} oro", color = Color.White)
                        TextButton(
                            onClick = { engine.buyItem(itemId) },
                            enabled = engine.state.gold >= item.price,
                        ) { Text("Comprar") }
                    }
                }

                // Igual que en el inventario: se lee la foto publicada, no `state.items`.
                val sellable = engine.inventory.mapNotNull { engine.data.database.item(it.itemId) }
                    .filter { it.price > 0 }
                if (sellable.isNotEmpty()) {
                    Text("Vender (a mitad de precio):", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    sellable.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${item.name} ×${engine.state.itemCount(item.id)} · ${item.price / 2} oro",
                                color = Color.White,
                            )
                            TextButton(onClick = { engine.sellItem(item.id) }) { Text("Vender") }
                        }
                    }
                }
            }

            Button(onClick = { engine.closeShop() }) { Text("Cerrar") }
        }
    }
}
