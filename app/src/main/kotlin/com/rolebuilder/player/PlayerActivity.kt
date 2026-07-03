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
import androidx.compose.foundation.clickable
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
import com.rolebuilder.core.engine.ShopSession
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.SaveIo
import com.rolebuilder.core.model.EquipSlot
import com.rolebuilder.player.ui.ActionButton
import com.rolebuilder.player.ui.ChoicesPanel
import com.rolebuilder.player.ui.GoldCounter
import com.rolebuilder.player.ui.HeartsRow
import com.rolebuilder.player.ui.LevelExpIndicator
import com.rolebuilder.player.ui.MessagePanel
import com.rolebuilder.player.ui.NoticeBanner
import com.rolebuilder.player.ui.VirtualJoystick
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.isActive

/** Ejecuta un proyecto RPG a pantalla completa, empezando por el título. */
class PlayerActivity : ComponentActivity() {

    private lateinit var soundFx: SoundFx
    private lateinit var bgm: BgmPlayer
    private lateinit var projectDir: File
    private lateinit var savesDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        val path = intent.getStringExtra(EXTRA_PROJECT_DIR)
        if (path == null) {
            finish()
            return
        }
        projectDir = File(path)
        savesDir = File(projectDir, "saves")

        val data = try {
            ProjectIo.loadFull(projectDir)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar el proyecto: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        soundFx = SoundFx(this)
        bgm = BgmPlayer()

        setContent {
            var engine by remember { mutableStateOf<RpgEngine?>(null) }
            val current = engine

            if (current == null) {
                LaunchedEffect(Unit) { bgm.stop() }
                TitleScreen(
                    gameName = data.project.name,
                    savesDir = savesDir,
                    onNewGame = {
                        engine = RpgEngine(data, GameState.newGame(data.project, data.database))
                    },
                    onLoadSlot = { slot ->
                        SaveIo.load(savesDir, slot)?.let { engine = RpgEngine(data, it) }
                    },
                    onExit = { finish() },
                )
            } else {
                PlayerScreen(
                    engine = current,
                    projectDir = projectDir,
                    soundFx = soundFx,
                    bgm = bgm,
                    savesDir = savesDir,
                    onSave = { slot ->
                        SaveIo.save(savesDir, slot, current.state)
                        Toast.makeText(this, "Partida guardada en la ranura $slot", Toast.LENGTH_SHORT).show()
                    },
                    onExit = { finish() },
                    onRetry = { engine = null }, // vuelta al título
                )
            }
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
        if (::bgm.isInitialized) bgm.release()
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
    bgm: BgmPlayer,
    savesDir: File,
    onSave: (Int) -> Unit,
    onExit: () -> Unit,
    onRetry: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var menuOpen by remember { mutableStateOf(false) }

    // Re-lee el estado del motor una vez por frame de UI. En el mismo bucle
    // se consume engine.notice (ocultándolo pasados ~2 s) y se sigue la
    // pista de música que pida el motor (play es idempotente).
    var frame by remember { mutableLongStateOf(0L) }
    var noticeText by remember { mutableStateOf<String?>(null) }
    var noticeShownAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            androidx.compose.runtime.withFrameMillis { now ->
                engine.notice?.let {
                    engine.notice = null
                    noticeText = it
                    noticeShownAt = now
                }
                if (noticeText != null && now - noticeShownAt > 2000L) noticeText = null
                if (engine.gameOver) bgm.stop() else bgm.play(engine.state.currentBgm)
            }
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
                    Lifecycle.Event.ON_PAUSE -> {
                        glView?.onPause()
                        bgm.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        glView?.onResume()
                        bgm.resume()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        @Suppress("UNUSED_EXPRESSION")
        frame // fuerza la recomposición del HUD cada frame

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HeartsRow(hp = engine.state.hp, maxHp = engine.state.maxHp)
                LevelExpIndicator(
                    level = engine.state.level,
                    exp = engine.state.exp,
                    expToNext = if (engine.state.level >= engine.actor.maxLevel) {
                        0
                    } else {
                        engine.actor.expBase * engine.state.level
                    },
                )
                GoldCounter(gold = engine.state.gold)
            }

            noticeText?.let {
                NoticeBanner(text = it, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
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

        engine.choices?.let { choices ->
            ChoicesPanel(choices = choices, onSelect = { engine.selectChoice(it) })
        }

        engine.message?.let { message ->
            MessagePanel(text = message.text, speaker = message.speaker, onDismiss = { engine.dismissMessage() })
        }

        engine.shop?.let { shop ->
            ShopOverlay(engine = engine, shop = shop)
        }

        if (menuOpen) {
            PauseMenu(
                engine = engine,
                savesDir = savesDir,
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
    savesDir: File,
    onSave: (Int) -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var savingOpen by remember { mutableStateOf(false) }
    if (savingOpen) {
        SaveSlotDialog(
            savesDir = savesDir,
            onSave = onSave,
            onDismiss = { savingOpen = false },
        )
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xF0121A30), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Menú", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val state = engine.state
                val database = engine.data.database

                // ---- Estado ----
                MenuSectionTitle("Estado")
                val atMaxLevel = state.level >= engine.actor.maxLevel
                StatLine("Nivel", if (atMaxLevel) "${state.level} (máx)" else "${state.level}")
                StatLine("Experiencia", if (atMaxLevel) "—" else "${state.exp} / ${engine.actor.expBase * state.level}")
                StatLine("PV", "${state.hp} / ${state.maxHp}")
                StatLine("Ataque", "${engine.effectiveAttack}")
                StatLine("Defensa", "${engine.effectiveDefense}")
                StatLine("Oro", "${state.gold}")

                // ---- Equipo ----
                MenuSectionTitle("Equipo")
                EquipSlotRow(
                    slotName = "Arma",
                    itemName = state.weaponItemId?.let { database.item(it)?.name ?: "Objeto $it" },
                    onUnequip = { engine.equip(EquipSlot.WEAPON, null) },
                )
                EquipSlotRow(
                    slotName = "Armadura",
                    itemName = state.armorItemId?.let { database.item(it)?.name ?: "Objeto $it" },
                    onUnequip = { engine.equip(EquipSlot.ARMOR, null) },
                )
                val inventory = state.items.toList()
                inventory.forEach { (itemId, count) ->
                    val item = database.item(itemId) ?: return@forEach
                    val slot = item.equipSlot ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${item.name} ×$count", color = Color.White)
                        TextButton(onClick = { engine.equip(slot, itemId) }) {
                            Text("Equipar")
                        }
                    }
                }

                // ---- Objetos (solo los no equipables; el equipo va arriba) ----
                MenuSectionTitle("Objetos")
                val usables = inventory.filter { (itemId, _) -> database.item(itemId)?.equipSlot == null }
                if (usables.isEmpty()) {
                    Text("No llevas objetos.", color = Color.White.copy(alpha = 0.7f))
                }
                usables.forEach { (itemId, count) ->
                    val item = database.item(itemId)
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { savingOpen = true }) { Text("Guardar") }
                Button(onClick = onDismiss) { Text("Seguir") }
                TextButton(onClick = onExit) { Text("Salir") }
            }
        }
    }
}

/** Encabezado de sección del menú de pausa. */
@Composable
private fun MenuSectionTitle(text: String) {
    Text(
        text,
        color = Color(0xFFFFC94D),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Línea etiqueta/valor de la sección Estado. */
@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f))
        Text(value, color = Color.White)
    }
}

/** Ranura de equipo (arma/armadura) con opción de quitar lo equipado. */
@Composable
private fun EquipSlotRow(slotName: String, itemName: String?, onUnequip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$slotName: ${itemName ?: "—"}",
            color = if (itemName != null) Color.White else Color.White.copy(alpha = 0.6f),
        )
        if (itemName != null) {
            TextButton(onClick = onUnequip) { Text("Quitar") }
        }
    }
}

/** Tienda: comprar de shop.itemIds y vender del inventario a mitad de precio. */
@Composable
private fun ShopOverlay(engine: RpgEngine, shop: ShopSession) {
    Dialog(onDismissRequest = { engine.closeShop() }) {
        var selling by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(Color(0xF0121A30), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tienda", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                GoldCounter(gold = engine.state.gold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShopTab("Comprar", selected = !selling) { selling = false }
                ShopTab("Vender", selected = selling) { selling = true }
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!selling) {
                    val items = shop.itemIds.mapNotNull { engine.data.database.item(it) }
                    if (items.isEmpty()) {
                        Text("No hay nada a la venta.", color = Color.White.copy(alpha = 0.7f))
                    }
                    items.forEach { item ->
                        ShopRow(
                            name = item.name,
                            description = item.description,
                            price = item.price,
                            actionLabel = "Comprar",
                            enabled = engine.state.gold >= item.price,
                            onAction = { engine.buyItem(item.id) },
                        )
                    }
                } else {
                    val sellable = engine.state.items.toList().mapNotNull { (itemId, count) ->
                        val item = engine.data.database.item(itemId) ?: return@mapNotNull null
                        if (item.price <= 0) null else Pair(item, count)
                    }
                    if (sellable.isEmpty()) {
                        Text("No tienes nada que vender.", color = Color.White.copy(alpha = 0.7f))
                    }
                    sellable.forEach { (item, count) ->
                        ShopRow(
                            name = "${item.name} ×$count",
                            description = item.description,
                            price = item.price / 2,
                            actionLabel = "Vender",
                            enabled = true,
                            onAction = { engine.sellItem(item.id) },
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { engine.closeShop() }) { Text("Salir") }
            }
        }
    }
}

/** Pestaña Comprar/Vender de la tienda. */
@Composable
private fun ShopTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(if (selected) Color(0xFF223055) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 14.dp),
    )
}

/** Fila de la tienda: nombre, descripción, precio y acción. */
@Composable
private fun ShopRow(
    name: String,
    description: String,
    price: Int,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White)
            if (description.isNotBlank()) {
                Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        GoldCounter(gold = price)
        TextButton(onClick = onAction, enabled = enabled) { Text(actionLabel) }
    }
}

/** Pantalla de título: nombre del juego, Nueva partida y Continuar. */
@Composable
private fun TitleScreen(
    gameName: String,
    savesDir: File,
    onNewGame: () -> Unit,
    onLoadSlot: (Int) -> Unit,
    onExit: () -> Unit,
) {
    val slots = remember { SaveIo.listSlots(savesDir) }
    var choosingSlot by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B1020)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            gameName,
            color = Color(0xFFFFC94D),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Column(
            modifier = Modifier.padding(top = 32.dp).fillMaxWidth(0.7f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!choosingSlot) {
                Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
                    Text("Nueva partida")
                }
                Button(
                    onClick = { choosingSlot = true },
                    enabled = slots.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continuar")
                }
                TextButton(onClick = onExit) { Text("Salir", color = Color.White.copy(alpha = 0.7f)) }
            } else {
                Text("Elige una partida", color = Color.White, fontWeight = FontWeight.Bold)
                slots.forEach { info ->
                    SlotRow(info = info, emptyLabel = null) { onLoadSlot(info.slot) }
                }
                TextButton(onClick = { choosingSlot = false }) {
                    Text("Volver", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

/** Diálogo de guardado: una fila por ranura, guardar sobrescribe. */
@Composable
private fun SaveSlotDialog(
    savesDir: File,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var refresh by remember { mutableStateOf(0) }
    val slots = remember(refresh) { SaveIo.listSlots(savesDir).associateBy { it.slot } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xF0121A30), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Guardar partida", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Guardar en una ranura ocupada la sobrescribe.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
            for (slot in 1..SaveIo.SLOT_COUNT) {
                SlotRow(info = slots[slot], emptyLabel = "Ranura $slot — vacía") {
                    onSave(slot)
                    refresh++
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    }
}

/**
 * Fila de ranura de guardado. Con [info] muestra sus metadatos; si es null
 * usa [emptyLabel] (y si este también es null, no se dibuja nada).
 */
@Composable
private fun SlotRow(info: SaveIo.SlotInfo?, emptyLabel: String?, onClick: () -> Unit) {
    if (info == null && emptyLabel == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B2745), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (info == null) {
            Text(emptyLabel ?: "", color = Color.White.copy(alpha = 0.6f))
        } else {
            Text("Ranura ${info.slot} · Nv ${info.level} · ${info.gold} oro", color = Color.White)
            Text(
                "${formatPlayTime(info.playTimeSeconds)} jugado · ${formatDate(info.modifiedAtMillis)}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }
    }
}

private fun formatPlayTime(seconds: Float): String {
    val total = seconds.toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

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
