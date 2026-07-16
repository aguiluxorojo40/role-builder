package com.rolebuilder.editor.platform

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.PlatformItemMark
import com.rolebuilder.core.model.PlatformItemType
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.loadAssetImageBitmap
import com.rolebuilder.editor.loadImageBitmap
import com.rolebuilder.editor.snes.AUTO_MAX_LEVELS
import com.rolebuilder.editor.snes.SnesImport
import com.rolebuilder.editor.snes.SnesImportDialog
import com.rolebuilder.editor.snes.importSmwLevelMap
import com.rolebuilder.editor.widgets.DropdownField
import com.rolebuilder.editor.widgets.IntField
import com.rolebuilder.player.PlatformerActivity
import java.io.File
import kotlin.math.floor

// Paleta visual propia del Platform Builder (verdes/cielo estilo SMW), para que
// se distinga de un vistazo del editor top-down del Role Builder.
// Paleta Nintendo (colores de marca de Mario): rojo Nintendo, azul, amarillo
// moneda y verde Luigi, sobre paneles translúcidos oscuros (efecto glass).
private val SkyBlue = Color(0xFF5C94FC)
private val MarioRed = Color(0xFFE60012)    // rojo Nintendo — acento principal
private val MarioBlue = Color(0xFF049CD8)   // azul Mario
private val CoinYellow = Color(0xFFFBD000)  // amarillo moneda/estrella
private val LuigiGreen = Color(0xFF43B047)  // verde Luigi/tubería
private val Canvas0 = Color(0xFF0B1220)
// Paneles translúcidos (glass) que dejan ver el fondo por detrás.
private val Panel = Color(0xCC10131C)
private val Glass = Color(0x9910131C)
private val GlassStroke = Color(0x33FFFFFF)

/** Herramientas del editor de plataformas. */
private enum class PTool(val label: String) {
    SELECT("Seleccionar"),
    TERRAIN("Primer plano"),
    DECOR("Fondo"),
    ERASE("Borrar"),
    ENEMY("Enemigo"),
    COIN("Moneda"),
    GOAL("Meta"),
    START("Inicio"),
    COLLISION("Colisión"),
}

/** Qué clase de objeto está seleccionado en el modo Seleccionar. */
private enum class SelKind { ENEMY, ITEM, START }

/** Un objeto seleccionado, identificado por su tipo y su celda actual. */
private data class Selected(val kind: SelKind, val x: Int, val y: Int)

/** Colores del recubrimiento de colisión (estilo "mostrar solidez" de Lunar Magic). */
private fun solidityColor(s: SmwSolidity): Color = when (s) {
    SmwSolidity.NONE -> Color.Transparent
    SmwSolidity.LEDGE_TOP -> Color(0x6640C4FF)
    SmwSolidity.SOLID -> Color(0x66FF4D4D)
    SmwSolidity.SLOPE -> Color(0x66FFA83C)
    SmwSolidity.SLOPE_STEEP -> Color(0x66FF7A00)
    SmwSolidity.SPIKE -> Color(0x66FF35C8)
}

private val SOLIDITY_VALUES = SmwSolidity.values()

private fun solidityOfTile(tileset: Tileset?, tile: Int): SmwSolidity {
    if (tileset == null || tile < 0) return SmwSolidity.NONE
    val ord = tileset.platformSolidity.getOrNull(tile)
    if (ord != null) return SOLIDITY_VALUES.getOrElse(ord) { SmwSolidity.NONE }
    return if (tileset.isPassable(tile)) SmwSolidity.NONE else SmwSolidity.SOLID
}

/**
 * Auto-importa una ROM de SMW: extrae los niveles como mapas jugables (tiles con
 * gráficos reales, colisión y enemigos ya colocados), selecciona el primero y borra
 * el nivel de arranque vacío si no se había tocado. Devuelve el nº de niveles, 0 si
 * no hay niveles reconocibles, o lanza si la ROM no es válida.
 */
internal fun autoImportSmwRom(state: EditorState, romBytes: ByteArray): Int {
    val header = SnesDecoder.parseHeader(romBytes)
    val maps = SnesGameRecipes.extractSmwLevelMaps(romBytes, header).take(AUTO_MAX_LEVELS)
    if (maps.isEmpty()) return 0
    val starterId = state.currentMapId
    val starter = state.currentMap
    var firstId: Int? = null
    maps.forEach { (_, nm, m) ->
        importSmwLevelMap(state, nm, m)
        if (firstId == null) firstId = state.currentMapId
    }
    firstId?.let { state.selectMap(it) }
    // Limpia el nivel de arranque vacío (sin tiles, enemigos ni ítems).
    if (starter != null && starter.id == starterId &&
        starter.platformEnemies.isEmpty() && starter.platformItems.isEmpty() &&
        starter.layers.all { layer -> layer.all { it == EMPTY_TILE } }
    ) {
        state.deleteMap(starterId)
    }
    return maps.size
}

// ---- Modo Seleccionar: encontrar, mover, borrar y editar objetos ----

/** Devuelve el objeto en la celda (tx,ty), priorizando enemigo > ítem > inicio. */
private fun hitTest(map: GameMap, project: Project, tx: Int, ty: Int): Selected? {
    map.platformEnemies.firstOrNull { it.x == tx && it.y == ty }?.let { return Selected(SelKind.ENEMY, tx, ty) }
    map.platformItems.firstOrNull { it.x == tx && it.y == ty }?.let { return Selected(SelKind.ITEM, tx, ty) }
    if (project.startMapId == map.id && project.startX == tx && project.startY == ty) {
        return Selected(SelKind.START, tx, ty)
    }
    return null
}

/** Mueve el objeto seleccionado a (nx,ny). Devuelve la nueva selección o null. */
private fun moveSelected(state: EditorState, cur: GameMap, sel: Selected, nx: Int, ny: Int): Selected? {
    if (!cur.inBounds(nx, ny)) return null
    when (sel.kind) {
        SelKind.ENEMY -> {
            val e = cur.platformEnemies.firstOrNull { it.x == sel.x && it.y == sel.y } ?: return null
            state.updateMap(cur.copy(platformEnemies = cur.platformEnemies - e + e.copy(x = nx, y = ny)))
        }
        SelKind.ITEM -> {
            val i = cur.platformItems.firstOrNull { it.x == sel.x && it.y == sel.y } ?: return null
            state.updateMap(cur.copy(platformItems = cur.platformItems - i + i.copy(x = nx, y = ny)))
        }
        SelKind.START -> state.updateProject(state.project.copy(startMapId = cur.id, startX = nx, startY = ny))
    }
    return Selected(sel.kind, nx, ny)
}

/** Borra el objeto seleccionado (el inicio del jugador no se puede borrar). */
private fun deleteSelected(state: EditorState, cur: GameMap, sel: Selected) {
    when (sel.kind) {
        SelKind.ENEMY -> state.updateMap(cur.copy(platformEnemies = cur.platformEnemies.filterNot { it.x == sel.x && it.y == sel.y }))
        SelKind.ITEM -> state.updateMap(cur.copy(platformItems = cur.platformItems.filterNot { it.x == sel.x && it.y == sel.y }))
        SelKind.START -> Unit
    }
}

/** Cambia el tipo de sprite del enemigo seleccionado. */
private fun setSelectedEnemyType(state: EditorState, cur: GameMap, sel: Selected, spriteId: Int) {
    val e = cur.platformEnemies.firstOrNull { it.x == sel.x && it.y == sel.y } ?: return
    state.updateMap(cur.copy(platformEnemies = cur.platformEnemies - e + e.copy(spriteId = spriteId)))
}

/** Alterna moneda/meta del ítem seleccionado. */
private fun toggleSelectedItem(state: EditorState, cur: GameMap, sel: Selected) {
    val i = cur.platformItems.firstOrNull { it.x == sel.x && it.y == sel.y } ?: return
    val newType = if (i.type == PlatformItemType.COIN) PlatformItemType.GOAL else PlatformItemType.COIN
    state.updateMap(cur.copy(platformItems = cur.platformItems - i + i.copy(type = newType)))
}

/** Solidez efectiva de una celda del mapa (la capa más bloqueante). */
private fun cellSolidity(map: GameMap, tileset: Tileset?, x: Int, y: Int): SmwSolidity {
    var result = SmwSolidity.NONE
    for (layer in map.layers.indices) {
        val tile = map.tileAt(layer, x, y)
        if (tile == EMPTY_TILE) continue
        val s = solidityOfTile(tileset, tile)
        if (s == SmwSolidity.SPIKE) return SmwSolidity.SPIKE
        if (s != SmwSolidity.NONE && result == SmwSolidity.NONE) result = s
    }
    return result
}

/**
 * Editor de niveles de plataformas al estilo Lunar Magic: lienzo con scroll
 * lateral, paleta de tiles (Map16), paleta de enemigos de SMW, capas de primer
 * plano/fondo, edición de la colisión por bloque y colocación del inicio.
 *
 * Edita el mismo [GameMap] que consume el motor de plataformas
 * ([com.rolebuilder.core.engine.platformer.ProjectPlatformer]); solo cambia la
 * interfaz respecto al editor top-down del Role Builder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformEditorScreen(projectDir: File, onBack: () -> Unit) {
    val context = LocalContext.current
    // El Platform Builder NO copia los assets del ARPG: arranca limpio y se llena
    // con lo que importes de la ROM de SMW.
    val state = remember(projectDir) { EditorState(projectDir) }
    val map = state.currentMap
    val tileset = map?.let { state.database.tileset(it.tilesetId) } ?: state.database.tilesets.firstOrNull()
    val tilesetBitmap = remember(tileset?.image) {
        tileset?.let { loadImageBitmap(state.projectDir, it.image) }
    }
    // Atlas de enemigos horneado (mismo orden que SmwEnemyGraphics.curatedIds).
    val enemyAtlas = remember { loadAssetImageBitmap(context, "sprites/enemies.png") }

    // Selector de ROM: al elegir un .sfc, auto-importa los niveles con gráficos reales.
    val romPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = SnesImport.readRomBytes(context, uri)
            if (bytes == null) {
                Toast.makeText(context, "No se pudo leer la ROM", Toast.LENGTH_LONG).show()
            } else {
                val n = runCatching { autoImportSmwRom(state, bytes) }.getOrElse { -1 }
                val msg = when {
                    n > 0 -> "Cargados $n niveles de la ROM (tiles, colisión y enemigos)"
                    n == 0 -> "No se encontraron niveles. ¿Es una ROM de Super Mario World?"
                    else -> "No se pudo cargar la ROM"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    var tool by remember { mutableStateOf(PTool.SELECT) }
    var selected by remember(map?.id) { mutableStateOf<Selected?>(null) }
    var selectedTile by remember { mutableIntStateOf(0) }
    var selectedEnemyId by remember {
        mutableIntStateOf(SmwEnemyGraphics.curatedIds.firstOrNull() ?: 0x0F)
    }
    var scale by remember { mutableFloatStateOf(28f) }
    var pan by remember(map?.id) { mutableStateOf(Offset(16f, 16f)) }
    var hover by remember(map?.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var showLevelSettings by remember { mutableStateOf(false) }
    var showRomImport by remember { mutableStateOf(false) }
    var showNewLevel by remember { mutableStateOf(false) }
    var showMap16 by remember { mutableStateOf(false) }

    DisposableEffect(state) { onDispose { if (state.dirty) state.save() } }

    if (map == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Este proyecto no tiene niveles.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Panel,
                    titleContentColor = Color.White,
                ),
                title = { Text("🍄 " + state.project.name + if (state.dirty) " •" else "") },
                navigationIcon = {
                    IconButton(onClick = { if (state.dirty) state.save(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        state.save()
                        Toast.makeText(context, "Nivel guardado", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Filled.Save, contentDescription = "Guardar") }
                    IconButton(onClick = {
                        state.save()
                        context.startActivity(PlatformerActivity.intentForProject(context, projectDir, map.id))
                    }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Probar nivel") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ---------- fila 1: nivel y acciones ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Glass)
                    .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DropdownField(
                    label = "Nivel",
                    options = state.mapList,
                    selected = map,
                    optionLabel = { "${it.id}: ${it.name}" },
                    onSelect = { state.selectMap(it.id) },
                )
                IconButton(onClick = { showNewLevel = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo nivel", tint = Color.White)
                }
                IconButton(onClick = { showLevelSettings = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes del nivel", tint = Color.White)
                }
                Button(
                    onClick = { romPicker.launch("*/*") },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MarioRed, contentColor = Color.Black),
                ) { Text("⚡ Cargar ROM") }
                TextButton(onClick = { showMap16 = true }) {
                    Text("Bloques Map16", color = LuigiGreen)
                }
                TextButton(onClick = { showRomImport = true }) {
                    Text("Avanzado", color = MarioBlue)
                }
            }
            // ---------- fila 2: herramientas de edición ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Glass)
                    .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PTool.entries.forEach { t ->
                    FilterChip(
                        selected = tool == t,
                        onClick = { tool = t },
                        label = { Text(t.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MarioRed,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }
            }

            // ---------- lienzo con scroll lateral ----------
            Box(Modifier.weight(1f).fillMaxWidth().background(SkyBlue.copy(alpha = 0.25f))) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(map.id, tool, selectedTile, selectedEnemyId) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var transform = false
                                var lastCell: Pair<Int, Int>? = null
                                // Estado del modo Seleccionar durante este gesto.
                                var didHitTest = false
                                var selDrag: Selected? = null

                                fun applyAt(position: Offset) {
                                    val tx = floor((position.x - pan.x) / scale).toInt()
                                    val ty = floor((position.y - pan.y) / scale).toInt()
                                    if (lastCell == tx to ty) return
                                    lastCell = tx to ty
                                    val cur = state.currentMap ?: return
                                    if (!cur.inBounds(tx, ty)) return
                                    hover = tx to ty
                                    when (tool) {
                                        PTool.SELECT -> {
                                            if (!didHitTest) {
                                                // Primer toque: selecciona el objeto bajo el dedo (o deselecciona).
                                                didHitTest = true
                                                selDrag = hitTest(cur, state.project, tx, ty)
                                                selected = selDrag
                                            } else if (selDrag != null) {
                                                // Arrastre: mueve el objeto seleccionado a la celda nueva.
                                                moveSelected(state, cur, selDrag!!, tx, ty)?.let {
                                                    selDrag = it
                                                    selected = it
                                                }
                                            }
                                        }
                                        PTool.TERRAIN -> state.updateMap(cur.withTile(0, tx, ty, selectedTile))
                                        PTool.DECOR -> state.updateMap(cur.withTile(1, tx, ty, selectedTile))
                                        PTool.ERASE -> {
                                            state.updateMap(
                                                cur.withTile(1, tx, ty, EMPTY_TILE)
                                                    .withTile(0, tx, ty, EMPTY_TILE)
                                                    .copy(
                                                        platformEnemies = cur.platformEnemies.filterNot { it.x == tx && it.y == ty },
                                                        platformItems = cur.platformItems.filterNot { it.x == tx && it.y == ty },
                                                    ),
                                            )
                                        }
                                        PTool.ENEMY -> {
                                            val existing = cur.platformEnemies.firstOrNull { it.x == tx && it.y == ty }
                                            state.updateMap(
                                                if (existing != null) {
                                                    cur.copy(platformEnemies = cur.platformEnemies - existing)
                                                } else {
                                                    cur.copy(platformEnemies = cur.platformEnemies +
                                                        PlatformEnemyMark(selectedEnemyId, tx, ty))
                                                },
                                            )
                                        }
                                        PTool.COIN, PTool.GOAL -> {
                                            val kind = if (tool == PTool.COIN) PlatformItemType.COIN else PlatformItemType.GOAL
                                            val existing = cur.platformItems.firstOrNull { it.x == tx && it.y == ty }
                                            state.updateMap(
                                                if (existing != null && existing.type == kind) {
                                                    cur.copy(platformItems = cur.platformItems - existing)
                                                } else {
                                                    cur.copy(platformItems = cur.platformItems
                                                        .filterNot { it.x == tx && it.y == ty } +
                                                        PlatformItemMark(kind, tx, ty))
                                                },
                                            )
                                        }
                                        PTool.START -> state.updateProject(
                                            state.project.copy(startMapId = cur.id, startX = tx, startY = ty),
                                        )
                                        PTool.COLLISION -> {
                                            // Toma el tile de la celda (primer plano primero) para editar su solidez.
                                            val t0 = cur.tileAt(0, tx, ty)
                                            val t1 = cur.tileAt(1, tx, ty)
                                            val picked = if (t0 != EMPTY_TILE) t0 else t1
                                            if (picked != EMPTY_TILE) selectedTile = picked
                                        }
                                    }
                                }

                                val dragTool = tool == PTool.TERRAIN || tool == PTool.DECOR ||
                                    tool == PTool.ERASE || tool == PTool.SELECT
                                if (dragTool) applyAt(down.position)
                                var tapPos: Offset? = down.position
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val pressed = ev.changes.filter { it.pressed }
                                    if (pressed.size >= 2) {
                                        transform = true; tapPos = null
                                        val zoom = ev.calculateZoom()
                                        val panDelta = ev.calculatePan()
                                        val centroid = ev.calculateCentroid()
                                        val newScale = (scale * zoom).coerceIn(8f, 96f)
                                        pan = (pan - centroid) * (newScale / scale) + centroid + panDelta
                                        scale = newScale
                                        ev.changes.forEach { it.consume() }
                                    } else if (pressed.size == 1 && !transform && dragTool) {
                                        applyAt(pressed[0].position); pressed[0].consume()
                                    }
                                    if (ev.changes.none { it.pressed }) {
                                        if (!transform && !dragTool) tapPos?.let { applyAt(it) }
                                        break
                                    }
                                }
                            }
                        },
                ) {
                    drawLevel(
                        map = map,
                        tileset = tileset,
                        tilesetBitmap = tilesetBitmap,
                        enemyAtlas = enemyAtlas,
                        pan = pan,
                        scale = scale,
                        showSolidity = tool == PTool.COLLISION,
                        start = if (state.project.startMapId == map.id) state.project.startX to state.project.startY else null,
                        selected = selected.takeIf { tool == PTool.SELECT },
                    )
                }

                hover?.let { (hx, hy) ->
                    Text(
                        "x $hx  y $hy",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .background(Color(0xCC0B1E12), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Text(
                    "${map.width} × ${map.height}",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                )

                // Primer arranque: proyecto sin gráficos → invita a importar de la ROM.
                if (tileset == null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "🍄 Proyecto vacío",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Carga tu ROM de Super Mario World y sus niveles se importan solos:\n" +
                                "gráficos reales, colisión y enemigos ya colocados.",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Button(
                            onClick = { romPicker.launch("*/*") },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MarioRed, contentColor = Color.Black,
                            ),
                            modifier = Modifier.padding(top = 12.dp),
                        ) { Text("⚡ Cargar ROM de SMW") }
                    }
                }
            }

            // ---------- paleta inferior ----------
            when (tool) {
                PTool.SELECT -> SelectionPanel(
                    selected = selected,
                    enemyAtlas = enemyAtlas,
                    onDelete = { selected?.let { deleteSelected(state, state.currentMap ?: map, it); selected = null } },
                    onChangeEnemy = { id -> selected?.let { setSelectedEnemyType(state, state.currentMap ?: map, it, id) } },
                    onToggleItem = { selected?.let { toggleSelectedItem(state, state.currentMap ?: map, it) } },
                )
                PTool.ENEMY -> EnemyPalette(enemyAtlas, selectedEnemyId) { selectedEnemyId = it }
                PTool.COIN -> Hint("Toca para poner o quitar monedas. Se recogen al jugar y suman al contador.")
                PTool.GOAL -> Hint("Toca para poner la meta (bandera). Al tocarla, el nivel se completa.")
                PTool.START -> Hint("Toca el nivel para fijar dónde aparece el jugador.")
                PTool.COLLISION -> CollisionPanel(
                    tileset = tileset,
                    bitmap = tilesetBitmap,
                    selectedTile = selectedTile,
                    onSelectTile = { selectedTile = it },
                    onSetSolidity = { s ->
                        val ts = tileset ?: return@CollisionPanel
                        val list = MutableList(ts.tileCount) {
                            ts.platformSolidity.getOrNull(it)
                                ?: if (ts.isPassable(it)) SmwSolidity.NONE.ordinal else SmwSolidity.SOLID.ordinal
                        }
                        if (selectedTile in list.indices) list[selectedTile] = s.ordinal
                        state.updateTileset(ts.copy(platformSolidity = list))
                    },
                    currentSolidity = solidityOfTile(tileset, selectedTile),
                )
                else -> {
                    if (tileset != null && tilesetBitmap != null) {
                        TilePalette(tileset, tilesetBitmap, selectedTile) { selectedTile = it }
                    } else {
                        Hint("Este nivel no tiene gráficos todavía. Pulsa \"⚡ Cargar ROM\" para importar los niveles de tu ROM de SMW con sus tiles.")
                    }
                }
            }
        }
    }

    // ---------- diálogos ----------
    if (showRomImport) {
        SnesImportDialog(state = state, onDismiss = {
            showRomImport = false
            // Si el nivel actual no tenía tileset válido, adopta el recién importado
            // para que sus gráficos aparezcan en los selectores del editor.
            val cur = state.currentMap
            if (cur != null && state.database.tileset(cur.tilesetId) == null) {
                state.database.tilesets.lastOrNull()?.let { state.updateMap(cur.copy(tilesetId = it.id)) }
            }
        })
    }

    if (showMap16) {
        Map16EditorDialog(state = state, onDismiss = { showMap16 = false })
    }

    if (showNewLevel) {
        NewLevelDialog(
            onDismiss = { showNewLevel = false },
            onCreate = { name, w, h ->
                state.addPlatformLevel(name, w, h, tilesetId = map.tilesetId, groundTile = selectedTile)
                showNewLevel = false
            },
        )
    }

    if (showLevelSettings) {
        LevelSettingsDialog(
            state = state,
            map = map,
            onDismiss = { showLevelSettings = false },
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().background(Panel).padding(10.dp),
    )
}

/** Panel del modo Seleccionar: info del objeto elegido, mover (en el lienzo), editar y borrar. */
@Composable
private fun SelectionPanel(
    selected: Selected?,
    enemyAtlas: ImageBitmap?,
    onDelete: () -> Unit,
    onChangeEnemy: (Int) -> Unit,
    onToggleItem: () -> Unit,
) {
    if (selected == null) {
        Hint("Toca un objeto (enemigo, moneda, meta o inicio) para seleccionarlo; arrástralo para moverlo.")
        return
    }
    Column(Modifier.fillMaxWidth().background(Panel)) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                when (selected.kind) {
                    SelKind.ENEMY -> "Enemigo — arrástralo para moverlo"
                    SelKind.ITEM -> "Ítem — arrástralo para moverlo"
                    SelKind.START -> "Inicio del jugador — arrástralo para moverlo"
                },
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (selected.kind == SelKind.ITEM) {
                TextButton(onClick = onToggleItem) { Text("Moneda/Meta", color = SkyBlue) }
            }
            if (selected.kind != SelKind.START) {
                Button(onClick = onDelete) { Text("Borrar") }
            }
        }
        if (selected.kind == SelKind.ENEMY) {
            Text(
                "Cambiar tipo:",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
            EnemyPalette(enemyAtlas, -1) { onChangeEnemy(it) }
        }
    }
}

@Composable
private fun TilePalette(tileset: Tileset, bitmap: ImageBitmap, selected: Int, onSelect: (Int) -> Unit) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(104.dp).background(Panel),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items((0 until tileset.tileCount).toList()) { tile ->
            val col = tile % tileset.columns
            val row = tile / tileset.columns
            Canvas(
                modifier = Modifier.size(44.dp)
                    .border(
                        width = if (selected == tile) 3.dp else 1.dp,
                        color = if (selected == tile) MarioRed else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .clickable { onSelect(tile) },
            ) {
                drawImage(
                    image = bitmap,
                    srcOffset = IntOffset(col * tileset.tileSize, row * tileset.tileSize),
                    srcSize = IntSize(tileset.tileSize, tileset.tileSize),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                )
            }
        }
    }
}

@Composable
private fun EnemyPalette(atlas: ImageBitmap?, selected: Int, onSelect: (Int) -> Unit) {
    val ids = SmwEnemyGraphics.curatedIds
    val frameW = atlas?.let { it.width / ids.size.coerceAtLeast(1) } ?: 0
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(104.dp).background(Panel),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(ids) { id ->
            val index = ids.indexOf(id)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(
                    modifier = Modifier.size(52.dp)
                        .border(
                            width = if (selected == id) 3.dp else 1.dp,
                            color = if (selected == id) MarioRed else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(6.dp),
                        )
                        .clickable { onSelect(id) },
                ) {
                    if (atlas != null && frameW > 0) {
                        drawImage(
                            image = atlas,
                            srcOffset = IntOffset(index * frameW, 0),
                            srcSize = IntSize(frameW, atlas.height),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        )
                    } else {
                        drawRect(Color(0xFFB0303C), size = size)
                    }
                }
                Text(
                    SmwEnemyGraphics.nameOf(id) ?: "#$id",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CollisionPanel(
    tileset: Tileset?,
    bitmap: ImageBitmap?,
    selectedTile: Int,
    onSelectTile: (Int) -> Unit,
    onSetSolidity: (SmwSolidity) -> Unit,
    currentSolidity: SmwSolidity,
) {
    Column(Modifier.fillMaxWidth().background(Panel)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Solidez del bloque:", color = Color.White, style = MaterialTheme.typography.labelMedium)
            SmwSolidity.values().forEach { s ->
                FilterChip(
                    selected = currentSolidity == s,
                    onClick = { onSetSolidity(s) },
                    label = { Text(solidityLabel(s)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarioRed,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }
        }
        if (tileset != null && bitmap != null) {
            TilePalette(tileset, bitmap, selectedTile, onSelectTile)
        }
    }
}

private fun solidityLabel(s: SmwSolidity): String = when (s) {
    SmwSolidity.NONE -> "Aire"
    SmwSolidity.LEDGE_TOP -> "Plataforma"
    SmwSolidity.SOLID -> "Sólido"
    SmwSolidity.SLOPE -> "Cuesta"
    SmwSolidity.SLOPE_STEEP -> "Cuesta 2"
    SmwSolidity.SPIKE -> "Pinchos"
}

@Composable
private fun NewLevelDialog(onDismiss: () -> Unit, onCreate: (String, Int, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableIntStateOf(48) }
    var height by remember { mutableIntStateOf(15) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo nivel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField("Ancho", width, { width = it.coerceIn(8, 400) }, Modifier.weight(1f))
                    IntField("Alto", height, { height = it.coerceIn(8, 60) }, Modifier.weight(1f))
                }
                Text(
                    "Se crea con dos filas de suelo (el tile seleccionado) y el inicio a la izquierda.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, width, height) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// Colores de cielo predefinidos (etiqueta a ARGB).
private val SKY_PRESETS: List<Pair<String, Long>> = listOf(
    "Cielo (por defecto)" to 0xFF5C94FCL,
    "Atardecer" to 0xFFF08030L,
    "Noche" to 0xFF101830L,
    "Cueva" to 0xFF000000L,
    "Nieve" to 0xFFB0D0F0L,
    "Lava" to 0xFF801010L,
)

@Composable
private fun LevelSettingsDialog(
    state: EditorState,
    map: GameMap,
    onDismiss: () -> Unit,
) {
    var name by remember(map.id) { mutableStateOf(map.name) }
    var width by remember(map.id) { mutableIntStateOf(map.width) }
    var height by remember(map.id) { mutableIntStateOf(map.height) }
    var tilesetId by remember(map.id) { mutableIntStateOf(map.tilesetId) }
    var sky by remember(map.id) { mutableStateOf(map.skyColor ?: SKY_PRESETS.first().second) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes del nivel") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField("Ancho", width, { width = it.coerceIn(8, 400) }, Modifier.weight(1f))
                    IntField("Alto", height, { height = it.coerceIn(8, 60) }, Modifier.weight(1f))
                }
                DropdownField(
                    label = "Tileset",
                    options = state.database.tilesets.map { it.id },
                    selected = tilesetId,
                    optionLabel = { id -> state.database.tileset(id)?.name ?: "Tileset $id" },
                    onSelect = { tilesetId = it },
                )
                DropdownField(
                    label = "Color de cielo",
                    options = SKY_PRESETS.map { it.second },
                    selected = sky,
                    optionLabel = { c -> SKY_PRESETS.firstOrNull { it.second == c }?.first ?: "Personalizado" },
                    onSelect = { sky = it },
                )
                if (state.mapList.size > 1) {
                    TextButton(onClick = {
                        state.deleteMap(map.id)
                        onDismiss()
                    }) { Text("Borrar este nivel", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                var updated = (state.map(map.id) ?: map).copy(name = name, tilesetId = tilesetId, skyColor = sky)
                if (width != map.width || height != map.height) updated = updated.resized(width, height)
                state.updateMap(updated)
                onDismiss()
            }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun DrawScope.drawLevel(
    map: GameMap,
    tileset: Tileset?,
    tilesetBitmap: ImageBitmap?,
    enemyAtlas: ImageBitmap?,
    pan: Offset,
    scale: Float,
    showSolidity: Boolean,
    start: Pair<Int, Int>?,
    selected: Selected? = null,
) {
    val tilePx = scale
    val minX = floor(-pan.x / tilePx).toInt().coerceAtLeast(0)
    val maxX = floor((size.width - pan.x) / tilePx).toInt().coerceAtMost(map.width - 1)
    val minY = floor(-pan.y / tilePx).toInt().coerceAtLeast(0)
    val maxY = floor((size.height - pan.y) / tilePx).toInt().coerceAtMost(map.height - 1)
    if (maxX < minX || maxY < minY) return

    drawRect(SkyBlue, topLeft = pan, size = Size(map.width * tilePx, map.height * tilePx))

    if (tileset != null && tilesetBitmap != null) {
        for (layer in map.layers.indices) {
            for (ty in minY..maxY) for (tx in minX..maxX) {
                val tile = map.tileAt(layer, tx, ty)
                if (tile < 0) continue
                val col = tile % tileset.columns
                val row = tile / tileset.columns
                drawImage(
                    image = tilesetBitmap,
                    srcOffset = IntOffset(col * tileset.tileSize, row * tileset.tileSize),
                    srcSize = IntSize(tileset.tileSize, tileset.tileSize),
                    dstOffset = IntOffset((pan.x + tx * tilePx).toInt(), (pan.y + ty * tilePx).toInt()),
                    dstSize = IntSize(tilePx.toInt() + 1, tilePx.toInt() + 1),
                )
            }
        }
    }

    // Recubrimiento de colisión.
    if (showSolidity) {
        for (ty in minY..maxY) for (tx in minX..maxX) {
            val s = cellSolidity(map, tileset, tx, ty)
            if (s == SmwSolidity.NONE) continue
            drawRect(
                solidityColor(s),
                topLeft = Offset(pan.x + tx * tilePx, pan.y + ty * tilePx),
                size = Size(tilePx, tilePx),
            )
        }
    }

    // Rejilla.
    if (tilePx >= 16f) {
        val grid = Color(0x22000000)
        for (tx in minX..maxX + 1) {
            val x = pan.x + tx * tilePx
            drawLine(grid, Offset(x, pan.y + minY * tilePx), Offset(x, pan.y + (maxY + 1) * tilePx))
        }
        for (ty in minY..maxY + 1) {
            val y = pan.y + ty * tilePx
            drawLine(grid, Offset(pan.x + minX * tilePx, y), Offset(pan.x + (maxX + 1) * tilePx, y))
        }
    }

    // Enemigos.
    val ids = SmwEnemyGraphics.curatedIds
    val frameW = enemyAtlas?.let { it.width / ids.size.coerceAtLeast(1) } ?: 0
    for (e in map.platformEnemies) {
        if (e.x < minX - 1 || e.x > maxX + 1) continue
        val dst = IntOffset((pan.x + e.x * tilePx).toInt(), (pan.y + e.y * tilePx).toInt())
        val idx = ids.indexOf(e.spriteId)
        if (enemyAtlas != null && frameW > 0 && idx >= 0) {
            drawImage(
                image = enemyAtlas,
                srcOffset = IntOffset(idx * frameW, 0),
                srcSize = IntSize(frameW, enemyAtlas.height),
                dstOffset = dst,
                dstSize = IntSize(tilePx.toInt(), tilePx.toInt()),
            )
        } else {
            drawRect(
                Color(0xCCB0303C),
                topLeft = Offset(dst.x.toFloat(), dst.y.toFloat()),
                size = Size(tilePx, tilePx),
            )
        }
    }

    // Ítems: monedas (dorado) y meta (poste + banderín verde).
    for (item in map.platformItems) {
        if (item.x < minX - 1 || item.x > maxX + 1) continue
        val ox = pan.x + item.x * tilePx
        val oy = pan.y + item.y * tilePx
        when (item.type) {
            PlatformItemType.COIN -> {
                drawRect(
                    Color(0xFFFFD54F),
                    topLeft = Offset(ox + tilePx * 0.28f, oy + tilePx * 0.14f),
                    size = Size(tilePx * 0.44f, tilePx * 0.72f),
                )
            }
            PlatformItemType.GOAL -> {
                drawRect(Color(0xFFE0E0E0), topLeft = Offset(ox + tilePx * 0.44f, oy - tilePx), size = Size(tilePx * 0.12f, tilePx * 2f))
                drawRect(Color(0xFF35C759), topLeft = Offset(ox + tilePx * 0.56f, oy - tilePx), size = Size(tilePx * 0.5f, tilePx * 0.4f))
            }
        }
    }

    // Inicio del jugador.
    start?.let { (sx, sy) ->
        if (sx in minX - 1..maxX + 1) {
            val topLeft = Offset(pan.x + sx * tilePx, pan.y + sy * tilePx)
            drawRect(Color(0xCC00E676), topLeft = topLeft, size = Size(tilePx, tilePx), style = Stroke(width = 3f))
            drawCircle(Color(0xFF00E676), radius = tilePx * 0.18f, center = topLeft + Offset(tilePx / 2f, tilePx / 2f))
        }
    }

    // Resaltado del objeto seleccionado (modo Seleccionar) — amarillo moneda.
    selected?.let { sel ->
        val topLeft = Offset(pan.x + sel.x * tilePx, pan.y + sel.y * tilePx)
        drawRect(CoinYellow, topLeft = topLeft, size = Size(tilePx, tilePx), style = Stroke(width = 3f))
    }
}
