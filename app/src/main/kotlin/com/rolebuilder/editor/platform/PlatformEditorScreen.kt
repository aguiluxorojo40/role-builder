package com.rolebuilder.editor.platform

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.loadAssetImageBitmap
import com.rolebuilder.editor.loadImageBitmap
import com.rolebuilder.editor.snes.SnesImportDialog
import com.rolebuilder.editor.widgets.IntField
import com.rolebuilder.player.PlatformerActivity
import com.rolebuilder.project.ProjectStore
import java.io.File
import kotlin.math.floor

// Paleta visual propia del Platform Builder (verdes/cielo estilo SMW), para que
// se distinga de un vistazo del editor top-down del Role Builder.
private val SkyBlue = Color(0xFF5C94FC)
private val LeafGreen = Color(0xFF3CB043)
private val Canvas0 = Color(0xFF0B1E12)
private val Panel = Color(0xFF10261A)

/** Herramientas del editor de plataformas. */
private enum class PTool(val label: String) {
    TERRAIN("Primer plano"),
    DECOR("Fondo"),
    ERASE("Borrar"),
    ENEMY("Enemigo"),
    START("Inicio"),
    COLLISION("Colisión"),
}

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
    val state = remember(projectDir) {
        runCatching { ProjectStore.ensureDefaultImages(context, projectDir) }
        EditorState(projectDir)
    }
    val map = state.currentMap
    val tileset = map?.let { state.database.tileset(it.tilesetId) } ?: state.database.tilesets.firstOrNull()
    val tilesetBitmap = remember(tileset?.image) {
        tileset?.let { loadImageBitmap(state.projectDir, it.image) }
    }
    // Atlas de enemigos horneado (mismo orden que SmwEnemyGraphics.curatedIds).
    val enemyAtlas = remember { loadAssetImageBitmap(context, "sprites/enemies.png") }

    var tool by remember { mutableStateOf(PTool.TERRAIN) }
    var selectedTile by remember { mutableIntStateOf(4) }
    var selectedEnemyId by remember {
        mutableIntStateOf(SmwEnemyGraphics.curatedIds.firstOrNull() ?: 0x0F)
    }
    var scale by remember { mutableFloatStateOf(28f) }
    var pan by remember(map?.id) { mutableStateOf(Offset(16f, 16f)) }
    var hover by remember(map?.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var showLevelSettings by remember { mutableStateOf(false) }
    var showRomImport by remember { mutableStateOf(false) }

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
            // ---------- barra de herramientas ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PTool.entries.forEach { t ->
                    FilterChip(
                        selected = tool == t,
                        onClick = { tool = t },
                        label = { Text(t.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LeafGreen,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }
                IconButton(onClick = { showLevelSettings = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes del nivel", tint = Color.White)
                }
                TextButton(onClick = { showRomImport = true }) {
                    Text("Importar de ROM", color = SkyBlue)
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

                                fun applyAt(position: Offset) {
                                    val tx = floor((position.x - pan.x) / scale).toInt()
                                    val ty = floor((position.y - pan.y) / scale).toInt()
                                    if (lastCell == tx to ty) return
                                    lastCell = tx to ty
                                    val cur = state.currentMap ?: return
                                    if (!cur.inBounds(tx, ty)) return
                                    hover = tx to ty
                                    when (tool) {
                                        PTool.TERRAIN -> state.updateMap(cur.withTile(0, tx, ty, selectedTile))
                                        PTool.DECOR -> state.updateMap(cur.withTile(1, tx, ty, selectedTile))
                                        PTool.ERASE -> {
                                            state.updateMap(cur.withTile(1, tx, ty, EMPTY_TILE)
                                                .withTile(0, tx, ty, EMPTY_TILE))
                                            state.updateMap(state.currentMap!!.copy(
                                                platformEnemies = state.currentMap!!.platformEnemies
                                                    .filterNot { it.x == tx && it.y == ty },
                                            ))
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

                                val dragTool = tool == PTool.TERRAIN || tool == PTool.DECOR || tool == PTool.ERASE
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
            }

            // ---------- paleta inferior ----------
            when (tool) {
                PTool.ENEMY -> EnemyPalette(enemyAtlas, selectedEnemyId) { selectedEnemyId = it }
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
                    }
                }
            }
        }
    }

    // ---------- diálogos ----------
    if (showRomImport) {
        SnesImportDialog(state = state, onDismiss = { showRomImport = false })
    }

    if (showLevelSettings) {
        LevelSettingsDialog(
            map = map,
            onDismiss = { showLevelSettings = false },
            onApply = { name, w, h ->
                var updated = (state.map(map.id) ?: map).copy(name = name)
                if (w != map.width || h != map.height) updated = updated.resized(w, h)
                state.updateMap(updated)
                showLevelSettings = false
            },
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
                        color = if (selected == tile) LeafGreen else Color(0x33FFFFFF),
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
                            color = if (selected == id) LeafGreen else Color(0x33FFFFFF),
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
                        selectedContainerColor = LeafGreen,
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
private fun LevelSettingsDialog(
    map: GameMap,
    onDismiss: () -> Unit,
    onApply: (String, Int, Int) -> Unit,
) {
    var name by remember(map.id) { mutableStateOf(map.name) }
    var width by remember(map.id) { mutableIntStateOf(map.width) }
    var height by remember(map.id) { mutableIntStateOf(map.height) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes del nivel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField("Ancho", width, { width = it.coerceIn(8, 400) }, Modifier.weight(1f))
                    IntField("Alto", height, { height = it.coerceIn(8, 60) }, Modifier.weight(1f))
                }
                Text(
                    "Los niveles de plataformas suelen ser anchos (scroll lateral) y de poca altura.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { Button(onClick = { onApply(name, width, height) }) { Text("Aplicar") } },
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

    // Inicio del jugador.
    start?.let { (sx, sy) ->
        if (sx in minX - 1..maxX + 1) {
            val topLeft = Offset(pan.x + sx * tilePx, pan.y + sy * tilePx)
            drawRect(Color(0xCC00E676), topLeft = topLeft, size = Size(tilePx, tilePx), style = Stroke(width = 3f))
            drawCircle(Color(0xFF00E676), radius = tilePx * 0.18f, center = topLeft + Offset(tilePx / 2f, tilePx / 2f))
        }
    }
}
