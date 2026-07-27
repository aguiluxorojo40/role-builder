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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapWarp
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.PlatformItemMark
import com.rolebuilder.core.model.PlatformItemType
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.model.MapStamp
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.loadAssetImageBitmap
import com.rolebuilder.editor.loadBigSprites
import com.rolebuilder.editor.loadImageBitmap
import com.rolebuilder.editor.snes.AUTO_MAX_LEVELS
import com.rolebuilder.editor.snes.SnesImport
import com.rolebuilder.editor.snes.SnesImportDialog
import com.rolebuilder.editor.snes.importSmwLevelMap
import com.rolebuilder.editor.widgets.DropdownField
import com.rolebuilder.editor.widgets.IntField
import com.rolebuilder.player.PlatformerActivity
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
private enum class PTool(val label: String, val short: String = label) {
    SELECT("Seleccionar", "Sel"),
    TERRAIN("Primer plano", "1er\nplano"),
    DECOR("Fondo", "Fondo"),
    ERASE("Borrar", "Borrar"),
    ENEMY("Enemigo", "Enem."),
    COIN("Moneda", "Mon."),
    GOAL("Meta", "Meta"),
    START("Inicio", "Inicio"),
    COLLISION("Colisión", "Colis."),
    WARP("Warp", "Warp"),
    STAMP("Sello", "Sello"),
}

/** Acciones REALES del editor que puede hospedar el raíl configurable (abajo-izq). */
private enum class RailAction(val label: String) {
    NUEVO("Nuevo"),
    AJUSTES("Ajustes"),
    ROM("ROM"),
    BLOQUES("Bloques"),
    AVANZADO("Avanz."),
    GUARDAR("Guard."),
    PROBAR("Probar"),
    ZOOM_IN("Zoom +"),
    ZOOM_OUT("Zoom −"),
    BORRAR("Borrar"),
}

/** Un favorito de la hotbar: una instantánea del pincel actual (herramienta + tile
 *  o enemigo seleccionado). Al tocarlo se restaura esa selección. */
private data class Fav(val tool: PTool, val tile: Int, val enemyId: Int)

/**
 * Categoría de una tesela del tileset, por PROPÓSITO (no por aspecto), para agrupar la
 * paleta como en Super Mario Maker. Se deriva de los metadatos que ya trae el tileset
 * importado: [Tileset.platformBlockActions] (moneda / bloque ?) y [Tileset.platformSolidity]
 * (terreno / pinchos). Lo que no encaja es decoración/fondo.
 */
private enum class TileCat(val label: String) {
    TERRENO("Terreno"),
    ITEMS("Ítems"),
}

/**
 * Tabs de teselas al estilo Super Mario Maker 2: **Terreno** (suelo, cuestas,
 * decoración, pinchos — todo lo estructural/escénico) e **Ítems** (monedas, bloques
 * ?/premio — lo interactivo/coleccionable). Enemigos y Gizmos son otras herramientas
 * (radial), no teselas. La categoría sale del comportamiento REAL del bloque
 * ([Tileset.platformBlockActions]), igual que lo decide la ROM.
 */
private fun tileCategory(tileset: Tileset, tile: Int): TileCat {
    return when (tileset.platformBlockActions.getOrNull(tile)) {
        SmwBlockAction.COIN.ordinal, SmwBlockAction.DRAGON_COIN.ordinal,
        SmwBlockAction.QUESTION.ordinal -> TileCat.ITEMS
        else -> TileCat.TERRENO
    }
}

/** Mapa tesela-base → fotogramas, para animar en el editor las teselas animadas. */
private fun animMap(tileset: Tileset?): Map<Int, List<Int>> =
    tileset?.animations?.associate { it.baseTile to it.frames } ?: emptyMap()

/** La tesela a dibujar AHORA para [base]: si está animada, el fotograma actual. */
private fun animatedTile(base: Int, anim: Map<Int, List<Int>>, frame: Int): Int {
    val f = anim[base] ?: return base
    return if (f.isEmpty()) base else f[frame % f.size]
}

/** Tablero de transparencia (dos grises), para VER el alpha de sprites/tiles. */
private fun DrawScope.drawChecker(cell: Float = 6f) {
    val a = Color(0xFF3A3F4C)
    val b = Color(0xFF2A2E38)
    drawRect(b)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else cell
        while (x < size.width) {
            drawRect(a, topLeft = Offset(x, y), size = Size(cell, cell))
            x += cell * 2
        }
        y += cell
        row++
    }
}

/**
 * Destello (glint) para la Dragon Coin: un brillo diagonal que BARRE la celda con la
 * cadencia del reloj de animación. OJO: en el ROM la tesela de la Dragon Coin es
 * ESTÁTICA (a diferencia de la moneda giratoria $2B, que sí usa teselas animadas de
 * GFX33), así que esto es una ayuda VISUAL solo del editor —"por estética", para que la
 * moneda grande se lea como coleccionable— y NO se hornea en la extracción de la ROM.
 */
private fun DrawScope.drawCoinShine(ox: Float, oy: Float, w: Float, h: Float, phase: Float) {
    clipRect(ox, oy, ox + w, oy + h) {
        val cx = ox - h + phase * (w + 2f * h) // de fuera-izquierda a fuera-derecha
        val thick = maxOf(1f, w * 0.12f)
        drawLine(Color(0x73FFFFFF), Offset(cx, oy - h * 0.3f), Offset(cx - h, oy + h * 1.3f), thick)
        drawLine(Color(0x40FFFFFF), Offset(cx + w * 0.24f, oy - h * 0.3f), Offset(cx + w * 0.24f - h, oy + h * 1.3f), thick * 0.7f)
    }
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
    // Sprites GRANDES (jefes y enemigos mayores) por id: los que no caben en el atlas
    // cuadrado y se dibujan desde big_<id>.png, para poder COLOCARLOS también en el editor.
    val bigSprites = remember { loadBigSprites(context) }
    // Hoja de la moneda real de SMW (para el icono del sector Moneda en la rueda).
    val coinAtlas = remember { loadAssetImageBitmap(context, "sprites/coin.png") }
    // Reloj de animación del editor: teselas animadas (monedas, ? bloques…) y las
    // MONEDAS COLOCADAS cobran vida. Solo tictea si hay algo que animar (si no, no
    // redibuja de balde) — cuidando el rendimiento.
    var animFrame by remember { mutableIntStateOf(0) }
    val animByBase = remember(tileset) { animMap(tileset) }
    val hasAnim = animByBase.isNotEmpty() ||
        (map?.platformItems?.any { it.type == PlatformItemType.COIN } == true) ||
        (tileset?.platformBlockActions?.any { it == SmwBlockAction.DRAGON_COIN.ordinal } == true) ||
        (map?.platformEnemies?.any { SmwEnemyGraphics.animFrameCount(it.spriteId) > 1 } == true)
    LaunchedEffect(hasAnim) {
        if (hasAnim) while (true) { delay(140); animFrame = (animFrame + 1) and 0x3FFFFFFF }
    }

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
    // Herramienta SELLO: nombre del sello "en mano" (null = modo DEFINIR región) y la primera
    // esquina marcada al definir (dos toques: esquina 1 → esquina 2 = guardar).
    var selectedStampName by remember { mutableStateOf<String?>(null) }
    var stampAnchor by remember(map?.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var showLevelSettings by remember { mutableStateOf(false) }
    var showRomImport by remember { mutableStateOf(false) }
    var showNewLevel by remember { mutableStateOf(false) }
    var showMap16 by remember { mutableStateOf(false) }
    // Menú radial (lienzo-primero): la rueda se invoca con el botón flotante; el
    // "modo limpio" oculta el chrome para ver el nivel entero. lastPaint recuerda la
    // última herramienta de pintar para alternar con Seleccionar desde el centro.
    var wheelOpen by remember { mutableStateOf(false) }
    var cleanMode by remember { mutableStateOf(false) }
    // En HORIZONTAL el alto es oro: la fila del selector de nivel se convierte en un chip que
    // FLOTA sobre el lienzo (esquina) en vez de robar una fila entera. En vertical, fila normal.
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var lastPaint by remember { mutableStateOf(PTool.TERRAIN) }
    // Config de UI (raíl + favoritos) PERSISTENTE por proyecto (SharedPreferences).
    val uiPrefs = remember(projectDir) {
        context.getSharedPreferences("pb_ui_" + projectDir.name, android.content.Context.MODE_PRIVATE)
    }
    // Raíl configurable (abajo-izq): controles reales del editor, se añaden/quitan.
    val railActions = remember {
        val init = uiPrefs.getString("rail", null)
            ?.split(",")?.mapNotNull { runCatching { RailAction.valueOf(it) }.getOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(
                RailAction.NUEVO, RailAction.AJUSTES, RailAction.ROM, RailAction.BLOQUES,
                RailAction.AVANZADO, RailAction.ZOOM_IN, RailAction.ZOOM_OUT, RailAction.BORRAR,
            )
        mutableStateListOf(*init.toTypedArray())
    }
    var showRailConfig by remember { mutableStateOf(false) }
    // Hotbar de favoritos (abajo-centro): instantáneas del pincel actual, editable.
    val favs = remember {
        val init = uiPrefs.getString("favs", null)?.split(";")?.mapNotNull { s ->
            val p = s.split(":")
            val t = p.getOrNull(0)?.let { n -> runCatching { PTool.valueOf(n) }.getOrNull() }
            val tile = p.getOrNull(1)?.toIntOrNull()
            val eid = p.getOrNull(2)?.toIntOrNull()
            if (t != null && tile != null && eid != null) Fav(t, tile, eid) else null
        } ?: emptyList()
        mutableStateListOf(*init.toTypedArray())
    }
    var favEdit by remember { mutableStateOf(false) }
    // Celda donde se está colocando un warp (abre el diálogo de destino). null = ninguno.
    var pendingWarp by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Guarda ambas listas (se llama tras cada cambio).
    val persistUi = {
        uiPrefs.edit()
            .putString("rail", railActions.joinToString(",") { it.name })
            .putString("favs", favs.joinToString(";") { it.tool.name + ":" + it.tile + ":" + it.enemyId })
            .apply()
    }
    // Despacha una acción del raíl al mismo manejador que ya usa la app.
    val onRail: (RailAction) -> Unit = { a ->
        when (a) {
            RailAction.NUEVO -> showNewLevel = true
            RailAction.AJUSTES -> showLevelSettings = true
            RailAction.ROM -> romPicker.launch("*/*")
            RailAction.BLOQUES -> showMap16 = true
            RailAction.AVANZADO -> showRomImport = true
            RailAction.GUARDAR -> { state.save(); Toast.makeText(context, "Nivel guardado", Toast.LENGTH_SHORT).show() }
            RailAction.PROBAR -> {
                state.save()
                state.currentMap?.let { context.startActivity(PlatformerActivity.intentForProject(context, projectDir, it.id)) }
            }
            RailAction.ZOOM_IN -> scale = (scale * 1.25f).coerceIn(8f, 96f)
            RailAction.ZOOM_OUT -> scale = (scale / 1.25f).coerceIn(8f, 96f)
            RailAction.BORRAR -> tool = PTool.ERASE
        }
    }

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
            // ---------- fila 1: nivel (se oculta en modo limpio; en HORIZONTAL flota sobre
            // el lienzo para no robar alto — ver el chip dentro del Box del lienzo) ----------
            if (!cleanMode && !isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
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
                    // Las acciones (Nuevo, Ajustes, ROM, Bloques, Avanzado) viven ahora
                    // en el RAÍL configurable abajo-izquierda (lienzo-primero).
                }
            }
            // La fila de herramientas se sustituye por el MENÚ RADIAL (botón flotante
            // sobre el lienzo): lienzo-primero, sin barra de chips permanente.

            // ---------- lienzo con scroll lateral ----------
            Box(Modifier.weight(1f).fillMaxWidth().background(SkyBlue.copy(alpha = 0.25f))) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(map.id, tool, selectedTile, selectedEnemyId, selectedStampName) {
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
                                        PTool.WARP -> {
                                            // Sobre un warp existente lo quita; si no, abre el
                                            // diálogo para configurar destino y tipo de entrada.
                                            val existing = cur.platformWarps.firstOrNull { it.x == tx && it.y == ty }
                                            if (existing != null) {
                                                state.updateMap(cur.copy(platformWarps = cur.platformWarps - existing))
                                            } else {
                                                pendingWarp = tx to ty
                                            }
                                        }
                                        PTool.STAMP -> {
                                            val held = state.project.stamps.firstOrNull { it.name == selectedStampName }
                                            if (held != null) {
                                                // Sello EN MANO: un toque lo PEGA con su esquina aquí.
                                                state.updateMap(held.pasteInto(cur, tx, ty))
                                            } else {
                                                // Modo DEFINIR: 1er toque marca esquina, 2º guarda la región.
                                                val a = stampAnchor
                                                if (a == null) {
                                                    stampAnchor = tx to ty
                                                } else {
                                                    val x0 = minOf(a.first, tx); val y0 = minOf(a.second, ty)
                                                    val w = kotlin.math.abs(tx - a.first) + 1
                                                    val h = kotlin.math.abs(ty - a.second) + 1
                                                    val s = com.rolebuilder.core.model.MapStamp.fromRegion(
                                                        cur, x0, y0, w, h, "Sello ${state.project.stamps.size + 1}",
                                                    )
                                                    stampAnchor = null
                                                    if (s != null) {
                                                        state.addStamp(s)
                                                        selectedStampName = s.name // queda en mano para pegar
                                                        Toast.makeText(context, "Sello guardado: ${s.name} (${s.width}×${s.height})", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
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
                        bigSprites = bigSprites,
                        pan = pan,
                        scale = scale,
                        showSolidity = tool == PTool.COLLISION,
                        start = if (state.project.startMapId == map.id) state.project.startX to state.project.startY else null,
                        selected = selected.takeIf { tool == PTool.SELECT },
                        animByBase = animByBase,
                        animFrame = animFrame,
                        coinAtlas = coinAtlas,
                        stampRect = if (tool == PTool.STAMP && stampAnchor != null) {
                            val a = stampAnchor!!; val hv = hover
                            if (hv != null) intArrayOf(minOf(a.first, hv.first), minOf(a.second, hv.second), maxOf(a.first, hv.first), maxOf(a.second, hv.second))
                            else intArrayOf(a.first, a.second, a.first, a.second)
                        } else null,
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

                // ---------- MENÚ RADIAL (lienzo-primero) ----------
                if (wheelOpen) {
                    // scrim: tocar fuera cierra la rueda
                    Box(Modifier.fillMaxSize().clickable { wheelOpen = false })
                    RadialWheel(
                        tool = tool,
                        enemyAtlas = enemyAtlas,
                        coinAtlas = coinAtlas,
                        onPickTool = { picked ->
                            if (picked != PTool.SELECT) lastPaint = picked
                            tool = picked
                            wheelOpen = false
                        },
                        onToggleSelect = {
                            if (tool == PTool.SELECT) {
                                tool = lastPaint
                            } else {
                                lastPaint = tool
                                tool = PTool.SELECT
                            }
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // Raíl configurable (abajo-izquierda), oculto en modo limpio.
                if (!cleanMode) {
                    EditableRail(
                        active = railActions,
                        onAct = onRail,
                        onConfig = { showRailConfig = true },
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    )
                }
                // Hotbar de favoritos (abajo-centro), oculta en modo limpio.
                if (!cleanMode) {
                    Row(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(14.dp)).background(Glass)
                            .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                            .horizontalScroll(rememberScrollState()).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        favs.forEach { f ->
                            FavSlot(f, tileset, tilesetBitmap, enemyAtlas, bigSprites, favEdit) {
                                if (favEdit) {
                                    favs.remove(f)
                                    persistUi()
                                } else {
                                    tool = f.tool
                                    if (f.tool != PTool.SELECT) lastPaint = f.tool
                                    selectedTile = f.tile
                                    selectedEnemyId = f.enemyId
                                }
                            }
                        }
                        // Añadir el pincel actual como favorito.
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, LuigiGreen, RoundedCornerShape(10.dp))
                                .clickable { favs.add(Fav(tool, selectedTile, selectedEnemyId)); persistUi() },
                            contentAlignment = Alignment.Center,
                        ) { Text("+", color = LuigiGreen, style = MaterialTheme.typography.titleMedium) }
                        // Editar (quitar) favoritos.
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (favEdit) CoinYellow else GlassStroke, RoundedCornerShape(10.dp))
                                .clickable { favEdit = !favEdit },
                            contentAlignment = Alignment.Center,
                        ) { Text("✎", color = if (favEdit) CoinYellow else Color.White) }
                    }
                }
                // Botón flotante que invoca la rueda.
                RadialFab(
                    open = wheelOpen,
                    onClick = { wheelOpen = !wheelOpen },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
                // Modo limpio: justo encima del botón flotante.
                CleanToggle(
                    clean = cleanMode,
                    onClick = { cleanMode = !cleanMode },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 86.dp),
                )
                // HORIZONTAL: el selector de nivel flota en la esquina superior izquierda del
                // lienzo, sin robarle alto a la vista de edición (que en horizontal es lo justo).
                if (!cleanMode && isLandscape) {
                    Box(
                        Modifier.align(Alignment.TopStart).padding(8.dp)
                            .clip(RoundedCornerShape(12.dp)).background(Glass)
                            .border(1.dp, GlassStroke, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        DropdownField(
                            label = "Nivel",
                            options = state.mapList,
                            selected = map,
                            optionLabel = { "${it.id}: ${it.name}" },
                            onSelect = { state.selectMap(it.id) },
                        )
                    }
                }
            }

            // ---------- paleta inferior (se oculta en modo limpio; en horizontal se limita el
            // alto para no comerse el lienzo) ----------
            val paletteMod = if (isLandscape) Modifier.fillMaxWidth().heightIn(max = 150.dp)
            else Modifier.fillMaxWidth()
            Box(paletteMod) {
            if (!cleanMode) when (tool) {
                PTool.SELECT -> SelectionPanel(
                    selected = selected,
                    enemyAtlas = enemyAtlas,
                    bigSprites = bigSprites,
                    onDelete = { selected?.let { deleteSelected(state, state.currentMap ?: map, it); selected = null } },
                    onChangeEnemy = { id -> selected?.let { setSelectedEnemyType(state, state.currentMap ?: map, it, id) } },
                    onToggleItem = { selected?.let { toggleSelectedItem(state, state.currentMap ?: map, it) } },
                )
                PTool.ENEMY -> EnemyPalette(enemyAtlas, bigSprites, selectedEnemyId) { selectedEnemyId = it }
                PTool.COIN -> Hint("Toca para poner o quitar monedas. Se recogen al jugar y suman al contador.")
                PTool.GOAL -> Hint("Toca para poner la meta (bandera). Al tocarla, el nivel se completa.")
                PTool.START -> Hint("Toca el nivel para fijar dónde aparece el jugador.")
                PTool.WARP -> Hint("Toca una celda para poner un warp (tubería/puerta) y elige a qué nivel y celda lleva. Tócalo otra vez para quitarlo.")
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
                    onSetSlopeShape = { shape ->
                        val ts = tileset ?: return@CollisionPanel
                        val list = MutableList(ts.tileCount) {
                            ts.platformSlopeShape.getOrNull(it) ?: com.rolebuilder.core.snes.SmwSlopes.NO_SLOPE
                        }
                        if (selectedTile in list.indices) list[selectedTile] = shape
                        state.updateTileset(ts.copy(platformSlopeShape = list))
                    },
                    currentShape = tileset?.platformSlopeShape?.getOrNull(selectedTile)
                        ?: com.rolebuilder.core.snes.SmwSlopes.NO_SLOPE,
                )
                PTool.STAMP -> StampPanel(
                    stamps = state.project.stamps,
                    selected = selectedStampName,
                    defining = stampAnchor != null,
                    onDefine = { selectedStampName = null; stampAnchor = null },
                    onSelect = { selectedStampName = it; stampAnchor = null },
                    onDelete = { name ->
                        state.deleteStamp(name)
                        if (selectedStampName == name) selectedStampName = null
                    },
                )
                else -> {
                    if (tileset != null && tilesetBitmap != null) {
                        TilePalette(tileset, tilesetBitmap, selectedTile) { selectedTile = it }
                    } else {
                        Hint("Este nivel no tiene gráficos todavía. Pulsa \"⚡ Cargar ROM\" para importar los niveles de tu ROM de SMW con sus tiles.")
                    }
                }
            }
            } // cierra el Box de la paleta (limitado en alto en horizontal)
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

    if (showRailConfig) {
        RailConfigDialog(
            active = railActions,
            onToggle = { a -> if (a in railActions) railActions.remove(a) else railActions.add(a); persistUi() },
            onDismiss = { showRailConfig = false },
        )
    }

    pendingWarp?.let { (wx, wy) ->
        WarpDialog(
            maps = state.mapList,
            currentMapId = map.id,
            onDismiss = { pendingWarp = null },
            onConfirm = { input, destMapId, dx, dy ->
                state.currentMap?.let { cur ->
                    state.updateMap(
                        cur.copy(
                            platformWarps = cur.platformWarps.filterNot { it.x == wx && it.y == wy } +
                                MapWarp(wx, wy, input, destMapId, dx, dy),
                        ),
                    )
                }
                pendingWarp = null
            },
        )
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
    bigSprites: Map<Int, ImageBitmap>,
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
            EnemyPalette(enemyAtlas, bigSprites, -1) { onChangeEnemy(it) }
        }
    }
}

@Composable
private fun TilePalette(tileset: Tileset, bitmap: ImageBitmap, selected: Int, onSelect: (Int) -> Unit) {
    // Clasifica las teselas por categoría una sola vez por tileset (eficiente).
    val byCat = remember(tileset) {
        val m = linkedMapOf<TileCat, MutableList<Int>>()
        for (t in 0 until tileset.tileCount) m.getOrPut(tileCategory(tileset, t)) { mutableListOf() }.add(t)
        m
    }
    val cats = remember(byCat) { TileCat.entries.filter { !byCat[it].isNullOrEmpty() } }
    var cat by remember(tileset) { mutableStateOf(cats.firstOrNull() ?: TileCat.TERRENO) }
    // Reloj + mapa de animación para que las teselas animadas se muevan en la paleta.
    val anim = remember(tileset) { animMap(tileset) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(anim.isEmpty()) {
        if (anim.isNotEmpty()) while (true) { delay(140); frame = (frame + 1) and 0x3FFFFFFF }
    }

    Column(Modifier.fillMaxWidth().background(Panel)) {
        // Pestañas de categoría (estilo Super Mario Maker: terreno con terreno…).
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            cats.forEach { c ->
                FilterChip(
                    selected = cat == c,
                    onClick = { cat = c },
                    label = { Text("${c.label} ${byCat[c]?.size ?: 0}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarioRed,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }
        }
        val tiles = byCat[cat] ?: emptyList()
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(104.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(tiles) { tile ->
                Canvas(
                    modifier = Modifier.size(44.dp)
                        .border(
                            width = if (selected == tile) 3.dp else 1.dp,
                            color = if (selected == tile) MarioRed else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable { onSelect(tile) },
                ) {
                    drawChecker() // fondo de tablero: la transparencia se VE
                    val shown = animatedTile(tile, anim, frame)
                    val col = shown % tileset.columns
                    val row = shown / tileset.columns
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
}

/** Nombre legible de un enemigo para el selector: el del catálogo de gráficos curado y, si no
 *  está (jefes/sprites grandes), el nombre de sprite genérico de SMW. */
/** Nombres en español de los enemigos GRANDES (jefes y mayores), para no mezclar el
 *  catálogo en español con los nombres técnicos en inglés en el selector. */
private val BIG_ENEMY_NAMES_ES: Map<Int, String> = mapOf(
    0xA0 to "Bowser", 0xA1 to "Bola de bolos", 0xA2 to "Mechakoopa", 0xA9 to "Reznor",
    0xC5 to "Big Boo", 0xA8 to "Blargg", 0x86 to "Wiggler", 0xBE to "Swooper",
    0x6E to "Dino-Rhino", 0x6F to "Dino-Torch", 0xAA to "Fishbone", 0xAB to "Rex",
    0xC2 to "Blurp", 0xC3 to "Pez globo", 0x71 to "Super Koopa (capa)", 0x73 to "Super Koopa",
    0x1F to "Magikoopa", 0x26 to "Thwomp", 0x70 to "Pokey", 0x91 to "Chargin' Chuck",
    0x9F to "Banzai Bill", 0xBF to "Topo gigante",
)

private fun enemyDisplayName(id: Int): String =
    SmwEnemyGraphics.nameOf(id) ?: BIG_ENEMY_NAMES_ES[id] ?: SmwSpriteNames.nameOf(id)

/** Ids COLOCABLES: el atlas curado (animado) + los que solo existen como sprite GRANDE
 *  (jefes y enemigos mayores: Bowser, Reznor, Big Boo, Dino-Torch…), sin duplicar. */
private fun placeableEnemyIds(bigSprites: Map<Int, ImageBitmap>): List<Int> {
    val curated = SmwEnemyGraphics.curatedIds
    return curated + bigSprites.keys.filter { it !in curated }.sorted()
}

/** Dibuja un sprite GRANDE encajado en la celda actual, conservando proporción y anclado
 *  abajo-centro (como en la vista de juego). */
private fun DrawScope.drawBigSpriteFit(img: ImageBitmap) {
    val scale = minOf(size.width / img.width, size.height / img.height)
    val w = (img.width * scale).coerceAtLeast(1f)
    val h = (img.height * scale).coerceAtLeast(1f)
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset(((size.width - w) / 2f).toInt(), (size.height - h).toInt()),
        dstSize = IntSize(w.toInt(), h.toInt()),
    )
}

@Composable
private fun EnemyPalette(
    atlas: ImageBitmap?, bigSprites: Map<Int, ImageBitmap>, selected: Int, onSelect: (Int) -> Unit,
) {
    val curated = SmwEnemyGraphics.curatedIds
    val ids = remember(bigSprites) { placeableEnemyIds(bigSprites) }
    val frameW = atlas?.let { it.width / curated.size.coerceAtLeast(1) } ?: 0
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(104.dp).background(Panel),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(ids) { id ->
            val index = curated.indexOf(id)
            val big = bigSprites[id]
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
                    if (index >= 0 && atlas != null && frameW > 0) {
                        // Del atlas: celda cuadrada (ATLAS_CELL), fotograma 0 (reposo), 1:1.
                        val cell = SmwEnemyGraphics.ATLAS_CELL
                        val dstH = size.height
                        val dstW = dstH * frameW / cell
                        drawImage(
                            image = atlas,
                            srcOffset = IntOffset(index * frameW, 0),
                            srcSize = IntSize(frameW, cell),
                            dstOffset = IntOffset(((size.width - dstW) / 2f).toInt(), 0),
                            dstSize = IntSize(dstW.toInt(), dstH.toInt()),
                        )
                    } else if (big != null) {
                        drawBigSpriteFit(big)   // jefe / enemigo grande (big_<id>.png)
                    } else {
                        drawRect(Color(0xFFB0303C), size = size)
                    }
                }
                Text(
                    enemyDisplayName(id),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Panel de la herramienta SELLO (prefabs). Un chip "Definir" (modo marcar región con dos
 * toques) y un chip por sello guardado (nombre · tamaño); al elegir uno queda "en mano" para
 * pegarlo tocando el nivel. La línea de ayuda dice siempre qué toca hacer ahora.
 */
@Composable
private fun StampPanel(
    stamps: List<MapStamp>,
    selected: String?,
    defining: Boolean,
    onDefine: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Panel).padding(8.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = selected == null,
                onClick = onDefine,
                label = { Text(if (defining && selected == null) "Marca la 2ª esquina…" else "➕ Definir") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MarioRed, selectedLabelColor = Color.Black,
                ),
            )
            stamps.forEach { s ->
                FilterChip(
                    selected = selected == s.name,
                    onClick = { onSelect(s.name) },
                    label = { Text("${s.name} · ${s.width}×${s.height}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarioRed, selectedLabelColor = Color.Black,
                    ),
                )
            }
        }
        val hint = when {
            selected != null -> "Sello \"$selected\" en mano: toca el nivel para PEGARLO."
            defining -> "Toca la 2ª esquina para GUARDAR esa región como sello."
            else -> "Toca 2 esquinas del nivel para crear un sello, o elige uno de arriba para pegarlo."
        }
        Text(
            hint,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (selected != null) {
            TextButton(onClick = { onDelete(selected) }) { Text("Borrar sello", color = MarioRed) }
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
    onSetSlopeShape: (Int) -> Unit = {},
    currentShape: Int = com.rolebuilder.core.snes.SmwSlopes.NO_SLOPE,
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
        // FORMA de la rampa (estilo Lunar Magic): con solidez "Cuesta", elige cómo es
        // la pendiente del tile; el motor la juega como rampa real (con tobogán).
        if (currentSolidity == SmwSolidity.SLOPE || currentSolidity == SmwSolidity.SLOPE_STEEP) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Forma de la rampa:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                SLOPE_SHAPE_OPTIONS.forEach { (label, shape) ->
                    FilterChip(
                        selected = currentShape == shape,
                        onClick = { onSetSlopeShape(shape) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MarioRed,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }
            }
        }
        if (tileset != null && bitmap != null) {
            TilePalette(tileset, bitmap, selectedTile, onSelectTile)
        }
    }
}

/**
 * Formas de rampa que ofrece la herramienta (de [com.rolebuilder.core.snes.SmwSlopes]):
 * las de 45° y las suaves 1:2 (que ocupan DOS tiles: mitad A y B, de izquierda a
 * derecha). Sin forma, la cuesta se comporta como bloque macizo (lo de antes).
 */
private val SLOPE_SHAPE_OPTIONS: List<Pair<String, Int>> = listOf(
    "╱ 45°" to com.rolebuilder.core.snes.SmwSlopes.SHAPE_45_UP_RIGHT,
    "╲ 45°" to com.rolebuilder.core.snes.SmwSlopes.SHAPE_45_DOWN_RIGHT,
    "╱ suave A" to com.rolebuilder.core.snes.SmwSlopes.SHAPE_GENTLE_UP_RIGHT_A,
    "╱ suave B" to com.rolebuilder.core.snes.SmwSlopes.SHAPE_GENTLE_UP_RIGHT_B,
    "╲ suave A" to com.rolebuilder.core.snes.SmwSlopes.SHAPE_GENTLE_DOWN_RIGHT_A,
    "╲ suave B" to com.rolebuilder.core.snes.SmwSlopes.SHAPE_GENTLE_DOWN_RIGHT_B,
    "Auto (según el dibujo)" to com.rolebuilder.core.snes.SmwSlopes.NO_SLOPE,
)

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
    bigSprites: Map<Int, ImageBitmap>,
    pan: Offset,
    scale: Float,
    showSolidity: Boolean,
    start: Pair<Int, Int>?,
    selected: Selected? = null,
    animByBase: Map<Int, List<Int>> = emptyMap(),
    animFrame: Int = 0,
    coinAtlas: ImageBitmap? = null,
    /** Región del sello en curso [x0,y0,x1,y1] en casillas (herramienta SELLO), o null. */
    stampRect: IntArray? = null,
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
                val base = map.tileAt(layer, tx, ty)
                if (base < 0) continue
                val tile = animatedTile(base, animByBase, animFrame) // teselas animadas vivas
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

    // Destello de las Dragon Coins: brillo del editor "por estética" (su tesela es
    // estática en el ROM). Se pinta encima del terreno y ambas mitades (16×32) brillan
    // en fase, así la moneda grande se lee como un coleccionable vivo.
    val dc = SmwBlockAction.DRAGON_COIN.ordinal
    if (tileset != null && tileset.platformBlockActions.any { it == dc }) {
        val phase = (animFrame % 16) / 16f
        for (ty in minY..maxY) for (tx in minX..maxX) {
            var isDragon = false
            for (layer in map.layers.indices) {
                val b = map.tileAt(layer, tx, ty)
                if (b >= 0 && tileset.platformBlockActions.getOrNull(b) == dc) { isDragon = true; break }
            }
            if (isDragon) drawCoinShine(pan.x + tx * tilePx, pan.y + ty * tilePx, tilePx, tilePx, phase)
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

    // Enemigos: el atlas guarda cada uno ENTERO en una celda cuadrada (ATLAS_CELL)
    // anclada por los pies y centrada, con ATLAS_FRAMES fotogramas apilados en vertical
    // (andar / aleteo). Se dibuja a 2×2 casillas (crece hacia arriba y a los lados, así
    // caben las ALAS de las Koopas), y el fotograma vivo lo da el reloj de animación.
    val ids = SmwEnemyGraphics.curatedIds
    val cell = SmwEnemyGraphics.ATLAS_CELL
    val frameW = enemyAtlas?.let { it.width / ids.size.coerceAtLeast(1) } ?: 0
    val frameCount = enemyAtlas?.let { (it.height / cell).coerceAtLeast(1) } ?: 1
    for (e in map.platformEnemies) {
        if (e.x < minX - 2 || e.x > maxX + 2) continue
        val idx = ids.indexOf(e.spriteId)
        if (enemyAtlas != null && frameW > 0 && idx >= 0) {
            // La celda cuadrada = 2×2 casillas, centrada en la del enemigo (los pies al
            // fondo de e.y). El fotograma vivo alterna con el reloj (estáticos = repetido).
            val fr = if (SmwEnemyGraphics.animFrameCount(e.spriteId) > 1) animFrame % frameCount else 0
            drawImage(
                image = enemyAtlas,
                srcOffset = IntOffset(idx * frameW, fr * cell),
                srcSize = IntSize(frameW, cell),
                dstOffset = IntOffset(
                    (pan.x + (e.x - 0.5f) * tilePx).toInt(),
                    (pan.y + (e.y - 1) * tilePx).toInt(),
                ),
                dstSize = IntSize((tilePx * 2f).toInt(), (tilePx * 2f).toInt()),
            )
        } else {
            val big = bigSprites[e.spriteId]
            if (big != null) {
                // Jefe / enemigo grande (big_<id>.png): a ESCALA NATIVA (bmp.w/16 casillas),
                // centrado en X y anclado por los pies en e.y, igual que al jugar.
                val wCells = big.width / 16f
                val hCells = big.height / 16f
                drawImage(
                    image = big,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(big.width, big.height),
                    dstOffset = IntOffset(
                        (pan.x + (e.x + 0.5f - wCells / 2f) * tilePx).toInt(),
                        (pan.y + (e.y + 1f - hCells) * tilePx).toInt(),
                    ),
                    dstSize = IntSize((wCells * tilePx).toInt(), (hCells * tilePx).toInt()),
                )
            } else {
                drawRect(
                    Color(0xCCB0303C),
                    topLeft = Offset(pan.x + e.x * tilePx, pan.y + e.y * tilePx),
                    size = Size(tilePx, tilePx),
                )
            }
        }
    }

    // Ítems: monedas (sprite REAL animado, como al jugar) y meta (poste + banderín).
    for (item in map.platformItems) {
        if (item.x < minX - 1 || item.x > maxX + 1) continue
        val ox = pan.x + item.x * tilePx
        val oy = pan.y + item.y * tilePx
        when (item.type) {
            PlatformItemType.COIN -> {
                val ca = coinAtlas
                if (ca != null) {
                    // coin.png = 4 fotogramas de 16×16 en fila: gira a la cadencia del reloj.
                    val frames = (ca.width / 16).coerceAtLeast(1)
                    val fr = animFrame % frames
                    drawImage(
                        image = ca,
                        srcOffset = IntOffset(fr * 16, 0),
                        srcSize = IntSize(16, minOf(16, ca.height)),
                        dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                        dstSize = IntSize(tilePx.toInt() + 1, tilePx.toInt() + 1),
                    )
                } else {
                    drawRect(
                        Color(0xFFFFD54F),
                        topLeft = Offset(ox + tilePx * 0.28f, oy + tilePx * 0.14f),
                        size = Size(tilePx * 0.44f, tilePx * 0.72f),
                    )
                }
            }
            PlatformItemType.GOAL -> {
                drawRect(Color(0xFFE0E0E0), topLeft = Offset(ox + tilePx * 0.44f, oy - tilePx), size = Size(tilePx * 0.12f, tilePx * 2f))
                drawRect(Color(0xFF35C759), topLeft = Offset(ox + tilePx * 0.56f, oy - tilePx), size = Size(tilePx * 0.5f, tilePx * 0.4f))
            }
        }
    }

    // Warps (tuberías/puertas): recuadro teal con borde, para verlos y quitarlos.
    for (w in map.platformWarps) {
        if (w.x < minX - 1 || w.x > maxX + 1) continue
        val ox = pan.x + w.x * tilePx
        val oy = pan.y + w.y * tilePx
        drawRect(Color(0x5517C0B8), topLeft = Offset(ox, oy), size = Size(tilePx, tilePx))
        drawRect(Color(0xFF17C0B8), topLeft = Offset(ox, oy), size = Size(tilePx, tilePx), style = Stroke(width = 3f))
        // flecha hacia dentro según el tipo de entrada (0=abajo,1=arriba,2=lado)
        val cx = ox + tilePx * 0.5f
        val cy = oy + tilePx * 0.5f
        drawRect(Color(0xFF0B3A38), topLeft = Offset(cx - tilePx * 0.08f, cy - tilePx * 0.24f), size = Size(tilePx * 0.16f, tilePx * 0.48f))
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

    // Marco de la región del SELLO en curso (violeta, semitransparente relleno + borde).
    stampRect?.let { r ->
        val x0 = r[0]; val y0 = r[1]; val x1 = r[2]; val y1 = r[3]
        val topLeft = Offset(pan.x + x0 * tilePx, pan.y + y0 * tilePx)
        val sz = Size((x1 - x0 + 1) * tilePx, (y1 - y0 + 1) * tilePx)
        drawRect(Color(0x33B388FF), topLeft = topLeft, size = sz)
        drawRect(Color(0xFFB388FF), topLeft = topLeft, size = sz, style = Stroke(width = 3f))
    }
}

// ============================================================================
//  MENÚ RADIAL  (lienzo-primero)
//  La rueda sustituye a la barra de chips de herramientas: se invoca con el
//  botón flotante y se cierra al elegir, dejando todo el lienzo para el nivel.
// ============================================================================

/** Herramientas que forman los sectores de la rueda (Seleccionar es el centro). */
private val WHEEL_TOOLS = listOf(
    PTool.TERRAIN, PTool.DECOR, PTool.ENEMY, PTool.COIN, PTool.GOAL,
    PTool.START, PTool.WARP, PTool.STAMP, PTool.COLLISION, PTool.ERASE,
)

/** Color de marca por herramienta (para leer la rueda de un vistazo). */
private fun toolColor(t: PTool): Color = when (t) {
    PTool.TERRAIN -> LuigiGreen
    PTool.DECOR -> MarioBlue
    PTool.ENEMY -> Color(0xFFB5651D)
    PTool.COIN -> CoinYellow
    PTool.GOAL -> Color(0xFF35C759)
    PTool.START -> MarioRed
    PTool.COLLISION -> Color(0xFF40C4FF)
    PTool.ERASE -> Color(0xFF9AA6BE)
    PTool.WARP -> Color(0xFF17C0B8)
    PTool.STAMP -> Color(0xFFB388FF)
    PTool.SELECT -> MarioBlue
}

/** La rueda radial: disco de fondo, centro = Seleccionar, y los sectores en círculo. */
@Composable
private fun RadialWheel(
    tool: PTool,
    enemyAtlas: ImageBitmap?,
    coinAtlas: ImageBitmap?,
    onPickTool: (PTool) -> Unit,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val radius = 108f // dp del centro a cada sector
    Box(modifier.size(300.dp), contentAlignment = Alignment.Center) {
        // disco de fondo (hace que se lea como RUEDA, no sectores sueltos)
        Box(
            Modifier.size(248.dp).clip(CircleShape).background(Panel)
                .border(1.dp, GlassStroke, CircleShape),
        )
        WHEEL_TOOLS.forEachIndexed { i, t ->
            val ang = (-90.0 + i * (360.0 / WHEEL_TOOLS.size)) * PI / 180.0
            SectorButton(
                tool = t,
                selected = tool == t,
                enemyAtlas = enemyAtlas,
                coinAtlas = coinAtlas,
                modifier = Modifier.offset(x = (cos(ang) * radius).dp, y = (sin(ang) * radius).dp),
                onClick = { onPickTool(t) },
            )
        }
        // centro: alterna Dibujar (última de pintar) / Seleccionar
        val selecting = tool == PTool.SELECT
        Box(
            Modifier.size(76.dp).clip(CircleShape)
                .background(if (selecting) MarioBlue else Panel)
                .border(2.dp, if (selecting) MarioBlue else GlassStroke, CircleShape)
                .clickable { onToggleSelect() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (selecting) "Selec." else "Dibujar",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Un sector de la rueda: token circular con el color de la herramienta. Para Enemigo
 *  y Moneda dibuja el SPRITE real (Goomba del atlas, moneda de SMW); el resto, etiqueta. */
@Composable
private fun SectorButton(
    tool: PTool,
    selected: Boolean,
    enemyAtlas: ImageBitmap?,
    coinAtlas: ImageBitmap?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = toolColor(tool)
    Box(
        modifier.size(64.dp).clip(CircleShape)
            .background(if (selected) c else Panel)
            .border(2.dp, if (selected) CoinYellow else c.copy(alpha = 0.7f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        when {
            tool == PTool.ENEMY && enemyAtlas != null -> {
                val ids = SmwEnemyGraphics.curatedIds
                val idx = ids.indexOf(0x0F).coerceAtLeast(0) // Goomba
                val fw = enemyAtlas.width / ids.size.coerceAtLeast(1)
                Canvas(Modifier.size(42.dp)) {
                    // Celda cuadrada (ATLAS_CELL), fotograma 0: el sprite va centrado y
                    // anclado abajo, así el icono sale con su proporción real.
                    drawImage(
                        image = enemyAtlas,
                        srcOffset = IntOffset(idx * fw, 0),
                        srcSize = IntSize(fw, SmwEnemyGraphics.ATLAS_CELL),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    )
                }
            }
            tool == PTool.COIN && coinAtlas != null -> {
                Canvas(Modifier.size(30.dp)) {
                    val s = minOf(coinAtlas.width, coinAtlas.height)
                    drawImage(
                        image = coinAtlas,
                        srcOffset = IntOffset(0, 0), // primer fotograma de la moneda
                        srcSize = IntSize(s, s),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    )
                }
            }
            else -> Text(
                tool.short,
                color = if (selected) Color.Black else Color.White,
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Botón flotante que abre/cierra la rueda (dorado, esquina). */
@Composable
private fun RadialFab(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(60.dp).clip(CircleShape)
            .background(CoinYellow)
            .border(2.dp, if (open) MarioRed else Color(0x55000000), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (open) "✕" else "☰",
            color = Color(0xFF06210D),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** Interruptor de "modo limpio" (oculta el chrome para ver el nivel entero). */
@Composable
private fun CleanToggle(clean: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(if (clean) MarioBlue.copy(alpha = 0.9f) else Panel)
            .border(1.dp, GlassStroke, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(if (clean) "▣" else "⛶", color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

// ============================================================================
//  RAÍL CONFIGURABLE  (abajo-izquierda)
//  Cluster pequeño con las acciones REALES del editor; se configura (añadir/
//  quitar) desde un diálogo. Sustituye a la fila de acciones de arriba.
// ============================================================================

@Composable
private fun EditableRail(
    active: List<RailAction>,
    onAct: (RailAction) -> Unit,
    onConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .heightIn(max = 340.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Glass)
            .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        active.forEach { a -> RailButton(a.label) { onAct(a) } }
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .border(1.dp, GlassStroke, CircleShape)
                .clickable { onConfig() },
            contentAlignment = Alignment.Center,
        ) { Text("✎", color = Color.White, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun RailButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 58.dp, height = 34.dp).clip(RoundedCornerShape(9.dp))
            .background(Panel).border(1.dp, GlassStroke, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Diálogo para elegir qué controles reales aparecen en el raíl. */
@Composable
private fun RailConfigDialog(
    active: List<RailAction>,
    onToggle: (RailAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar barra de acciones") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RailAction.entries.forEach { a ->
                    val on = a in active
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggle(a) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (on) "☑" else "☐", color = if (on) LuigiGreen else Color.White)
                        Text("  ${a.label}", color = Color.White)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}

// ============================================================================
//  HOTBAR DE FAVORITOS  (abajo-centro)
//  Cada casilla es una instantánea del pincel (tile o enemigo real) que se
//  restaura al tocarla. "+" fija el pincel actual; ✎ permite quitarlos.
// ============================================================================

@Composable
private fun FavSlot(
    fav: Fav,
    tileset: Tileset?,
    tilesetBitmap: ImageBitmap?,
    enemyAtlas: ImageBitmap?,
    bigSprites: Map<Int, ImageBitmap>,
    editing: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Panel)
            .border(1.dp, if (editing) MarioRed else GlassStroke, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        when (fav.tool) {
            PTool.TERRAIN, PTool.DECOR, PTool.COLLISION -> {
                if (tileset != null && tilesetBitmap != null && fav.tile in 0 until tileset.tileCount) {
                    Canvas(Modifier.size(38.dp)) {
                        drawChecker()
                        val col = fav.tile % tileset.columns
                        val row = fav.tile / tileset.columns
                        drawImage(
                            image = tilesetBitmap,
                            srcOffset = IntOffset(col * tileset.tileSize, row * tileset.tileSize),
                            srcSize = IntSize(tileset.tileSize, tileset.tileSize),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        )
                    }
                } else {
                    FavLabel(fav.tool.short)
                }
            }
            PTool.ENEMY -> {
                val ids = SmwEnemyGraphics.curatedIds
                val idx = ids.indexOf(fav.enemyId)
                val big = bigSprites[fav.enemyId]
                if (enemyAtlas != null && idx >= 0) {
                    val fw = enemyAtlas.width / ids.size.coerceAtLeast(1)
                    Canvas(Modifier.size(44.dp)) {
                        // Celda cuadrada (ATLAS_CELL), fotograma 0.
                        drawImage(
                            image = enemyAtlas,
                            srcOffset = IntOffset(idx * fw, 0),
                            srcSize = IntSize(fw, SmwEnemyGraphics.ATLAS_CELL),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        )
                    }
                } else if (big != null) {
                    Canvas(Modifier.size(44.dp)) { drawBigSpriteFit(big) }
                } else {
                    FavLabel("Enem.")
                }
            }
            else -> FavLabel(fav.tool.short)
        }
        if (editing) {
            Box(
                Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(MarioRed),
                contentAlignment = Alignment.Center,
            ) { Text("–", color = Color.White, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun FavLabel(text: String) {
    Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
}

// ============================================================================
//  DIÁLOGO DE WARP  (tubería / puerta)
//  Configura a qué nivel y celda lleva el warp, y el tipo de entrada.
// ============================================================================

@Composable
private fun WarpDialog(
    maps: List<GameMap>,
    currentMapId: Int,
    onDismiss: () -> Unit,
    onConfirm: (input: Int, destMapId: Int, destX: Int, destY: Int) -> Unit,
) {
    val inputs = listOf("Tubería (entrar abajo)", "Puerta (entrar arriba)", "Tubería (entrar de lado)")
    var input by remember { mutableIntStateOf(0) }
    var destMap by remember { mutableStateOf(maps.firstOrNull { it.id == currentMapId } ?: maps.firstOrNull()) }
    var dx by remember { mutableIntStateOf(0) }
    var dy by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo warp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownField(
                    label = "Cómo se entra",
                    options = inputs,
                    selected = inputs[input],
                    optionLabel = { it },
                    onSelect = { input = inputs.indexOf(it).coerceAtLeast(0) },
                )
                DropdownField(
                    label = "Nivel de destino",
                    options = maps,
                    selected = destMap,
                    optionLabel = { "${it.id}: ${it.name}" },
                    onSelect = { destMap = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField(label = "Celda X", value = dx, onChange = { dx = it }, modifier = Modifier.weight(1f))
                    IntField(label = "Celda Y", value = dy, onChange = { dy = it }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = { destMap?.let { onConfirm(input, it.id, dx, dy) } }) { Text("Poner warp") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
