package com.rolebuilder.editor.map

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.EnemySpawn
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.ParallaxLayer
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.model.MusicTracks
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.MapEvent
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.event.EventEditorDialog
import com.rolebuilder.editor.event.musicName
import com.rolebuilder.editor.loadImageBitmap
import com.rolebuilder.editor.widgets.DropdownField
import com.rolebuilder.editor.widgets.FloatField
import com.rolebuilder.editor.widgets.IntField
import kotlin.math.floor

private enum class Tool(val label: String) {
    PAINT("Pincel"),
    ERASE("Borrar"),
    EVENT("Evento"),
    SPAWN("Enemigo"),
    START("Inicio"),
}

/** Editor de mapas táctil: 1 dedo aplica la herramienta, 2 dedos pan/zoom. */
@Composable
fun MapEditorTab(state: EditorState) {
    val map = state.currentMap
    if (map == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Este proyecto no tiene mapas.")
        }
        return
    }
    val tileset = state.database.tileset(map.tilesetId) ?: state.database.tilesets.firstOrNull()
    val tilesetBitmap = remember(tileset?.image) {
        tileset?.let { loadImageBitmap(state.projectDir, it.image) }
    }

    var tool by remember { mutableStateOf(Tool.PAINT) }
    var layer by remember { mutableIntStateOf(0) }
    var selectedTile by remember { mutableIntStateOf(0) }
    var selectedEnemyId by remember(state.database.enemies) {
        mutableIntStateOf(state.database.enemies.firstOrNull()?.id ?: 0)
    }
    var scale by remember { mutableFloatStateOf(64f) } // px por casilla
    var pan by remember(map.id) { mutableStateOf(Offset.Zero) }
    var editingEventId by remember { mutableStateOf<Int?>(null) }
    var showMapSettings by remember { mutableStateOf(false) }
    var showNewMap by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // ---------- barra superior: mapa + herramientas ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DropdownField(
                label = "Mapa",
                options = state.mapList,
                selected = map,
                optionLabel = { "${it.id}: ${it.name}" },
                onSelect = { state.selectMap(it.id) },
            )
            IconButton(onClick = { showNewMap = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo mapa")
            }
            IconButton(onClick = { showMapSettings = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes del mapa")
            }
            Tool.entries.forEach { t ->
                FilterChip(
                    selected = tool == t,
                    onClick = { tool = t },
                    label = { Text(t.label) },
                )
            }
            if (tool == Tool.PAINT || tool == Tool.ERASE) {
                FilterChip(
                    selected = layer == 0,
                    onClick = { layer = 0 },
                    label = { Text("Suelo") },
                )
                FilterChip(
                    selected = layer == 1,
                    onClick = { layer = 1 },
                    label = { Text("Capa 2") },
                )
            }
        }

        // ---------- lienzo ----------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0A0E1A)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // scale/pan se leen del estado capturado: no van en las claves para
                    // no reiniciar el gesto en mitad de un pan/zoom.
                    .pointerInput(map.id, tool, layer, selectedTile, selectedEnemyId) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var transform = false
                            var lastTile: Pair<Int, Int>? = null

                            fun applyAt(position: Offset) {
                                val tx = floor((position.x - pan.x) / scale).toInt()
                                val ty = floor((position.y - pan.y) / scale).toInt()
                                if (lastTile == tx to ty) return
                                lastTile = tx to ty
                                val current = state.currentMap ?: return
                                if (!current.inBounds(tx, ty)) return
                                when (tool) {
                                    Tool.PAINT -> state.updateMap(current.withTile(layer, tx, ty, selectedTile))
                                    Tool.ERASE -> state.updateMap(
                                        current.withTile(layer, tx, ty, if (layer == 0) 0 else EMPTY_TILE),
                                    )
                                    Tool.EVENT -> {
                                        val existing = current.eventAt(tx, ty)
                                        if (existing != null) {
                                            editingEventId = existing.id
                                        } else {
                                            val event = MapEvent(
                                                id = current.nextEventId(),
                                                name = "Evento ${current.nextEventId()}",
                                                x = tx,
                                                y = ty,
                                                pages = listOf(EventPage()),
                                            )
                                            state.updateMap(current.copy(events = current.events + event))
                                            editingEventId = event.id
                                        }
                                    }
                                    Tool.SPAWN -> {
                                        val existing = current.spawns.firstOrNull { it.x == tx && it.y == ty }
                                        state.updateMap(
                                            if (existing != null) {
                                                current.copy(spawns = current.spawns - existing)
                                            } else {
                                                current.copy(spawns = current.spawns + EnemySpawn(selectedEnemyId, tx, ty))
                                            },
                                        )
                                    }
                                    Tool.START -> state.updateProject(
                                        state.project.copy(startMapId = current.id, startX = tx, startY = ty),
                                    )
                                }
                            }

                            val dragTool = tool == Tool.PAINT || tool == Tool.ERASE
                            if (dragTool) applyAt(down.position)

                            var tapPosition: Offset? = down.position
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    transform = true
                                    tapPosition = null
                                    val zoom = event.calculateZoom()
                                    val panDelta = event.calculatePan()
                                    val centroid = event.calculateCentroid()
                                    val newScale = (scale * zoom).coerceIn(12f, 200f)
                                    // Mantener el punto bajo los dedos estable al hacer zoom.
                                    pan = (pan - centroid) * (newScale / scale) + centroid + panDelta
                                    scale = newScale
                                    event.changes.forEach { it.consume() }
                                } else if (pressed.size == 1 && !transform) {
                                    if (dragTool) {
                                        applyAt(pressed[0].position)
                                        pressed[0].consume()
                                    }
                                }
                                if (event.changes.none { it.pressed }) {
                                    if (!transform && !dragTool) {
                                        tapPosition?.let { applyAt(it) }
                                    }
                                    break
                                }
                            }
                        }
                    },
            ) {
                drawMap(
                    map = map,
                    tileset = tileset,
                    tilesetBitmap = tilesetBitmap,
                    pan = pan,
                    scale = scale,
                    startMarker = if (state.project.startMapId == map.id) {
                        state.project.startX to state.project.startY
                    } else {
                        null
                    },
                )
            }
        }

        // ---------- paleta ----------
        when (tool) {
            Tool.PAINT -> {
                if (tileset != null && tilesetBitmap != null) {
                    TilePalette(
                        tileset = tileset,
                        bitmap = tilesetBitmap,
                        selected = selectedTile,
                        onSelect = { selectedTile = it },
                    )
                }
            }
            Tool.SPAWN -> {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        label = "Enemigo (toca el mapa para poner/quitar)",
                        options = state.database.enemies,
                        selected = state.database.enemies.firstOrNull { it.id == selectedEnemyId },
                        optionLabel = { "${it.id}: ${it.name}" },
                        onSelect = { selectedEnemyId = it.id },
                    )
                }
            }
            Tool.EVENT -> HintBar("Toca una casilla para crear o editar un evento.")
            Tool.START -> HintBar("Toca una casilla para fijar el punto de inicio del jugador.")
            Tool.ERASE -> HintBar("Arrastra para borrar tiles de la capa activa.")
        }
    }

    // ---------- diálogos ----------
    editingEventId?.let { eventId ->
        val current = state.currentMap
        val event = current?.events?.firstOrNull { it.id == eventId }
        if (current != null && event != null) {
            EventEditorDialog(
                state = state,
                event = event,
                onChange = { updated ->
                    state.updateMap(
                        current.copy(
                            events = if (updated == null) {
                                current.events.filter { it.id != eventId }
                            } else {
                                current.events.map { if (it.id == eventId) updated else it }
                            },
                        ),
                    )
                    if (updated == null) editingEventId = null
                },
                onDismiss = { editingEventId = null },
            )
        } else {
            editingEventId = null
        }
    }

    if (showNewMap) {
        var name by remember { mutableStateOf("") }
        var width by remember { mutableIntStateOf(20) }
        var height by remember { mutableIntStateOf(15) }
        AlertDialog(
            onDismissRequest = { showNewMap = false },
            title = { Text("Nuevo mapa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IntField("Ancho", width, { width = it.coerceIn(5, 200) }, Modifier.weight(1f))
                        IntField("Alto", height, { height = it.coerceIn(5, 200) }, Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    state.addMap(name, width, height)
                    showNewMap = false
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { showNewMap = false }) { Text("Cancelar") } },
        )
    }

    if (showMapSettings) {
        var name by remember(map.id) { mutableStateOf(map.name) }
        var width by remember(map.id) { mutableIntStateOf(map.width) }
        var height by remember(map.id) { mutableIntStateOf(map.height) }
        var bgm by remember(map.id) { mutableStateOf(map.bgm) }
        var parallax by remember(map.id) { mutableStateOf(map.parallaxLayers) }
        AlertDialog(
            onDismissRequest = { showMapSettings = false },
            title = { Text("Ajustes del mapa") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IntField("Ancho", width, { width = it.coerceIn(5, 200) }, Modifier.weight(1f))
                        IntField("Alto", height, { height = it.coerceIn(5, 200) }, Modifier.weight(1f))
                    }
                    DropdownField(
                        label = "Música de fondo",
                        options = listOf<String?>(null) + MusicTracks.ALL,
                        selected = bgm,
                        optionLabel = { it?.musicName() ?: "(silencio)" },
                        onSelect = { bgm = it },
                    )

                    Text("Parallax (profundidad de diorama)", style = MaterialTheme.typography.titleSmall)
                    parallax.forEachIndexed { index, layer ->
                        fun change(updated: ParallaxLayer) {
                            parallax = parallax.mapIndexed { i, l -> if (i == index) updated else l }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DropdownField(
                                "Imagen",
                                state.imageNames(),
                                layer.image,
                                { it },
                                { change(layer.copy(image = it)) },
                                Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                parallax = parallax.filterIndexed { i, _ -> i != index }
                            }) { Text("Quitar") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FloatField(
                                "Anclaje (0 cielo, 1 mapa)",
                                layer.factor,
                                { change(layer.copy(factor = it.coerceIn(0f, 1f))) },
                                Modifier.weight(1f),
                            )
                            FloatField(
                                "Opacidad (0-1)",
                                layer.alpha,
                                { change(layer.copy(alpha = it.coerceIn(0f, 1f))) },
                                Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FloatField(
                                "Deriva X (cas/s)",
                                layer.autoX,
                                { change(layer.copy(autoX = it)) },
                                Modifier.weight(1f),
                            )
                            FloatField(
                                "Deriva Y (cas/s)",
                                layer.autoY,
                                { change(layer.copy(autoY = it)) },
                                Modifier.weight(1f),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(layer.above, { change(layer.copy(above = it)) })
                            Text("  Por encima de la escena (niebla)")
                        }
                    }
                    TextButton(onClick = {
                        parallax = parallax + ParallaxLayer(
                            image = state.imageNames().firstOrNull { it.startsWith("clouds") }
                                ?: state.imageNames().firstOrNull().orEmpty(),
                            factor = 0.35f,
                            autoX = 0.25f,
                            above = true,
                            alpha = 0.5f,
                        )
                    }) { Text("Añadir capa de parallax") }

                    if (state.mapList.size > 1) {
                        TextButton(onClick = {
                            state.deleteMap(map.id)
                            showMapSettings = false
                        }) { Text("Borrar este mapa", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    var updated = map.copy(
                        name = name,
                        bgm = bgm,
                        parallaxLayers = parallax.filter { it.image.isNotBlank() },
                    )
                    if (width != map.width || height != map.height) {
                        updated = updated.resized(width, height)
                    }
                    state.updateMap(updated)
                    showMapSettings = false
                }) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { showMapSettings = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HintBar(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141B30))
            .padding(10.dp),
    )
}

@Composable
private fun TilePalette(
    tileset: Tileset,
    bitmap: ImageBitmap,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFF141B30)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items((0 until tileset.tileCount).toList()) { tile ->
            val col = tile % tileset.columns
            val row = tile / tileset.columns
            Canvas(
                modifier = Modifier
                    .size(42.dp)
                    .border(
                        width = if (selected == tile) 2.dp else 1.dp,
                        color = if (selected == tile) Color(0xFFFFC94D) else Color(0x33FFFFFF),
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

private fun DrawScope.drawMap(
    map: GameMap,
    tileset: Tileset?,
    tilesetBitmap: ImageBitmap?,
    pan: Offset,
    scale: Float,
    startMarker: Pair<Int, Int>?,
) {
    val tilePx = scale
    val minX = floor(-pan.x / tilePx).toInt().coerceAtLeast(0)
    val maxX = floor((size.width - pan.x) / tilePx).toInt().coerceAtMost(map.width - 1)
    val minY = floor(-pan.y / tilePx).toInt().coerceAtLeast(0)
    val maxY = floor((size.height - pan.y) / tilePx).toInt().coerceAtMost(map.height - 1)

    // Fondo del área del mapa.
    drawRect(
        color = Color(0xFF1A2138),
        topLeft = pan,
        size = Size(map.width * tilePx, map.height * tilePx),
    )

    if (tileset != null && tilesetBitmap != null) {
        for (layer in map.layers.indices) {
            for (ty in minY..maxY) {
                for (tx in minX..maxX) {
                    val tile = map.tileAt(layer, tx, ty)
                    if (tile < 0) continue
                    val col = tile % tileset.columns
                    val row = tile / tileset.columns
                    drawImage(
                        image = tilesetBitmap,
                        srcOffset = IntOffset(col * tileset.tileSize, row * tileset.tileSize),
                        srcSize = IntSize(tileset.tileSize, tileset.tileSize),
                        dstOffset = IntOffset(
                            (pan.x + tx * tilePx).toInt(),
                            (pan.y + ty * tilePx).toInt(),
                        ),
                        dstSize = IntSize(tilePx.toInt() + 1, tilePx.toInt() + 1),
                    )
                }
            }
        }
    }

    // Rejilla.
    if (tilePx >= 24f) {
        val gridColor = Color(0x22FFFFFF)
        for (tx in minX..maxX + 1) {
            val x = pan.x + tx * tilePx
            drawLine(gridColor, Offset(x, pan.y + minY * tilePx), Offset(x, pan.y + (maxY + 1) * tilePx))
        }
        for (ty in minY..maxY + 1) {
            val y = pan.y + ty * tilePx
            drawLine(gridColor, Offset(pan.x + minX * tilePx, y), Offset(pan.x + (maxX + 1) * tilePx, y))
        }
    }

    // Marcadores de eventos.
    for (event in map.events) {
        val topLeft = Offset(pan.x + event.x * tilePx, pan.y + event.y * tilePx)
        drawRect(Color(0x584D79FF), topLeft = topLeft, size = Size(tilePx, tilePx))
        drawRect(
            Color(0xFF6C8EFF),
            topLeft = topLeft + Offset(2f, 2f),
            size = Size(tilePx - 4f, tilePx - 4f),
            style = Stroke(width = 3f),
        )
    }

    // Puntos de aparición de enemigos.
    for (spawn in map.spawns) {
        val center = Offset(pan.x + (spawn.x + 0.5f) * tilePx, pan.y + (spawn.y + 0.5f) * tilePx)
        drawCircle(Color(0xAAE53935), radius = tilePx * 0.3f, center = center)
        drawCircle(Color(0xFFFFFFFF), radius = tilePx * 0.3f, center = center, style = Stroke(width = 2f))
    }

    // Punto de inicio del jugador.
    startMarker?.let { (sx, sy) ->
        val center = Offset(pan.x + (sx + 0.5f) * tilePx, pan.y + (sy + 0.5f) * tilePx)
        drawCircle(Color(0xCCFFC94D), radius = tilePx * 0.35f, center = center)
        drawCircle(Color(0xFF3A2E00), radius = tilePx * 0.15f, center = center)
    }
}
