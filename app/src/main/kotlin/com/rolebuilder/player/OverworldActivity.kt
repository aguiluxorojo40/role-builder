package com.rolebuilder.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.snes.SmwGameSave
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwOverworld
import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SmwOverworldWalk
import com.rolebuilder.core.snes.SmwSaveIo
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesHeader
import com.rolebuilder.editor.snes.SnesImport
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs

/**
 * MAPA DEL MUNDO de SMW, renderizado desde la ROM del usuario. Tiene **dos modos**, y la
 * diferencia entre ellos es justo la línea que separa los dos oficios de la app:
 *
 *  - [MODE_GAME] — **el juego**. Carga una ranura de guardado ([SmwGameSave]), dibuja el mapa
 *    con los caminos que TÚ has abierto, y al superar un nivel dispara su evento y **lo
 *    guarda en disco**. Los submapas se ven como en el juego: la ventana de cámara de 256×224
 *    de ese mundo, no los seis a la vez.
 *  - [MODE_TEST] — **probar desde el editor**. No toca ninguna partida ni escribe nada. Trae
 *    el conmutador "Todo abierto" para ver el mapa al 100% y el área de submapas entera, que
 *    es lo que hace falta cuando estás iterando sobre un nivel.
 *
 * En modo JUEGO Mario **anda de verdad** por los caminos (con su sprite real de la ROM,
 * mirando hacia donde va), y las tuberías lo llevan de un mundo a otro. En modo PRUEBA se
 * navega tocando las casillas, que es lo cómodo cuando iteras sobre un nivel.
 */
class OverworldActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val romPath = intent.getStringExtra(EXTRA_ROM_PATH)
        val romFile = romPath?.let { File(it) }
        val rom = runCatching { romFile?.takeIf { it.isFile }?.readBytes() }.getOrNull()
        if (rom == null || romFile == null) {
            Toast.makeText(this, "No se pudo leer la ROM", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val header = SnesDecoder.parseHeader(rom)
        val isGame = intent.getStringExtra(EXTRA_MODE) == MODE_GAME
        val slot = intent.getIntExtra(EXTRA_SLOT, 1)
        setContent { OverworldScreen(rom, header, romFile, isGame, slot) }
    }

    companion object {
        private const val EXTRA_ROM_PATH = "rom_path"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_SLOT = "slot"

        /** Modo JUEGO: partida guardada, progresión persistente. */
        const val MODE_GAME = "game"

        /** Modo PRUEBA: sin partida, sin escribir nada. Es el que abre el editor. */
        const val MODE_TEST = "test"

        /** Abre el mapa para JUGAR la ranura [slot]. */
        fun gameIntent(context: Context, romFile: File, slot: Int): Intent =
            intent(context, romFile).putExtra(EXTRA_MODE, MODE_GAME).putExtra(EXTRA_SLOT, slot)

        /** Abre el mapa para PROBAR (desde el editor): no guarda nada. */
        fun intent(context: Context, romFile: File): Intent =
            Intent(context, OverworldActivity::class.java)
                .putExtra(EXTRA_ROM_PATH, romFile.absolutePath)
                .putExtra(EXTRA_MODE, MODE_TEST)
    }
}

/** Lado en píxeles de los dos mapas de 512×512 del overworld (pantallas en 2×2). */
private const val MAP_SIDE = 512
/** Lado de una casilla del mapa (un bloque Map16). */
private const val TILE = 16
/** Milisegundos por casilla al andar. A ojo del original, que va a ~8 px por fotograma. */
private const val WALK_MS = 90L

/**
 * Lo que se está mirando: el mapa principal o uno de los seis submapas. En modo juego el
 * submapa se recorta a su ventana de cámara, así que hay que llevar el origen para poder
 * convertir coordenadas del área a coordenadas de la imagen.
 */
private data class MapView(
    val bitmap: Bitmap,
    val viewWidth: Int,
    val viewHeight: Int,
    val originX: Int,
    val originY: Int,
)

/** Dibuja el mapa de [world] con los eventos de [active] aplicados. */
private fun renderView(
    rom: ByteArray,
    header: SnesHeader,
    delta: Int,
    world: Int,
    cameraCrop: Boolean,
    active: (Int) -> Boolean,
): MapView? {
    val tilemap = SmwOverworld.overworldTilemapWithEvents(rom, delta, active)
    if (world == SmwGameSave.MAIN_MAP) {
        val img = SnesGameRecipes.renderOverworldMainMapFrom(rom, header, tilemap) ?: return null
        return MapView(SnesImport.toBitmap(img), MAP_SIDE, MAP_SIDE, 0, 0)
    }
    if (!cameraCrop) {
        val img = SnesGameRecipes.renderOverworldSubmapAreaFrom(rom, header, world, tilemap)
            ?: return null
        return MapView(SnesImport.toBitmap(img), MAP_SIDE, MAP_SIDE, 0, 0)
    }
    val img = SnesGameRecipes.renderOverworldSubmapFrom(rom, header, world, tilemap) ?: return null
    val camera = SmwOverworld.submapCamera(rom, delta, world)
    return MapView(
        SnesImport.toBitmap(img),
        SmwOverworld.OW_VIEW_WIDTH, SmwOverworld.OW_VIEW_HEIGHT,
        camera.first, camera.second,
    )
}

@Composable
private fun OverworldScreen(
    rom: ByteArray,
    header: SnesHeader,
    romFile: File,
    isGame: Boolean,
    slot: Int,
) {
    if (isGame) GameMapScreen(rom, header, romFile, slot)
    else TestMapScreen(rom, header, romFile)
}

// ---------------------------------------------------------------------------------------
// MODO JUEGO: Mario anda por los caminos
// ---------------------------------------------------------------------------------------

/**
 * El mapa del mundo **jugándose**: Mario en su casilla, la cruceta para andar por los caminos
 * reales ([SmwOverworldWalk]) y la partida guardándose en su ranura.
 *
 * Andar no es teletransportarse: se pulsa una dirección, se comprueba con las reglas del juego
 * si esa salida está abierta, y Mario recorre casilla a casilla hasta la siguiente parada,
 * doblando las esquinas que doble el camino. Al llegar se abre la dirección contraria —así se
 * puede volver— y se guarda.
 *
 * Y las **tuberías cambian de mundo**: si un recorrido acaba en una boca de tubería
 * ([SmwOverworldWalk.pathExits]), Mario salta al submapa de destino, como en el juego. Es lo
 * que conecta Yoshi's Island, Vanilla Dome, el Bosque y demás con el mapa principal — sin
 * ello habría mundos a los que no se llega jugando.
 */
@Composable
private fun GameMapScreen(rom: ByteArray, header: SnesHeader, romFile: File, slot: Int) {
    val context = LocalContext.current
    val delta = remember(rom) { header.headerOffset - 0x7FC0 }
    val saveDir = remember(context) { File(context.filesDir, SmwSaveIo.DIR) }
    val eventTable = remember(rom) { SmwOverworld.translevelEvents(rom, delta) }
    // La numeración de niveles va sobre la capa SIN eventos, como hace el juego.
    val levelNumbers = remember(rom) { SmwOverworldWalk.levelNumbers(rom, delta) }
    val start = remember(rom) { SmwOverworldWalk.newGameStart(rom, delta) }

    var save by remember {
        mutableStateOf(SmwSaveIo.load(saveDir, slot) ?: SmwGameSave(slot = slot))
    }
    // Los settings deciden por dónde se puede andar; una partida nueva los siembra de la ROM.
    var settings by remember {
        mutableStateOf(save.settingsOr(SmwOverworldWalk.newGameSettings(rom, delta)))
    }
    // Dónde está Mario. Sin posición guardada, la casilla de salida de la ROM.
    var world by remember { mutableStateOf(if (save.position == null) start.submap else save.submap) }
    var marioX by remember {
        mutableStateOf(save.position?.let { SmwOverworldWalk.gridOf(it).first } ?: start.x)
    }
    var marioY by remember {
        mutableStateOf(save.position?.let { SmwOverworldWalk.gridOf(it).second } ?: start.y)
    }

    val walkTiles = remember(save.firedEvents) {
        SmwOverworldWalk.layer1WithEvents(rom, delta, save.activeEvents())
    }
    // Las salidas de camino (tuberías) que cambian de mundo, y las casillas donde paran.
    val exits = remember(rom) { SmwOverworldWalk.pathExits(rom, delta) }
    // Mario del mapa, dibujado con su sprite real desde la ROM: uno por dirección.
    val marioSprites = remember(rom) {
        listOf(
            SmwOverworldWalk.DIR_UP, SmwOverworldWalk.DIR_DOWN,
            SmwOverworldWalk.DIR_LEFT, SmwOverworldWalk.DIR_RIGHT,
        ).associateWith { dir ->
            SnesGameRecipes.overworldMarioSprite(rom, header, dir)?.let { SnesImport.toBitmap(it) }
        }
    }
    // Hacia dónde mira: por defecto abajo (el idle del juego), y cambia al andar.
    var facing by remember { mutableStateOf(SmwOverworldWalk.DIR_DOWN) }
    val view = remember(world, save.firedEvents) {
        renderView(rom, header, delta, world, cameraCrop = true, active = save.activeEvents())
    }

    fun persist(next: SmwGameSave) {
        save = SmwSaveIo.save(saveDir, next.withSettings(settings))
    }

    // Nivel sobre el que está Mario ahora mismo (0 si es una casilla que no es de nivel).
    val hereLevel = levelNumbers.getOrElse(SmwOverworldWalk.position(world, marioX, marioY)) { 0 }
    val exitStops = remember(exits, world) { SmwOverworldWalk.exitStops(exits, world) }
    // Direcciones que Mario puede tomar desde aquí. En una casilla-de-nivel manda el candado
    // de caminos del juego (settings); en una boca de tubería, adonde haya camino físico.
    val open = remember(settings, hereLevel, world, marioX, marioY) {
        if (hereLevel > 0) {
            SmwOverworldWalk.openDirections(settings, hereLevel)
        } else {
            SmwOverworldWalk.SEARCH_ORDER.filter { dir ->
                SmwOverworldWalk.walk(walkTiles, levelNumbers, world, marioX, marioY, dir, exitStops) != null
            }
        }
    }

    // Recorrido en curso: la lista de casillas y por cuál vamos.
    var route by remember { mutableStateOf<SmwOverworldWalk.Walk?>(null) }
    LaunchedEffect(route) {
        val r = route ?: return@LaunchedEffect
        var px = marioX; var py = marioY
        for (step in r.steps) {
            delay(WALK_MS)
            // Mario mira hacia donde da el paso, casilla a casilla.
            facing = when {
                step.x < px -> SmwOverworldWalk.DIR_LEFT
                step.x > px -> SmwOverworldWalk.DIR_RIGHT
                step.y < py -> SmwOverworldWalk.DIR_UP
                else -> SmwOverworldWalk.DIR_DOWN
            }
            px = step.x; py = step.y
            marioX = step.x
            marioY = step.y
        }
        // Llegada: el juego abre la dirección CONTRARIA en la casilla de destino.
        if (r.endLevel > 0 && r.endLevel < settings.size) {
            val opened = settings.copyOf()
            opened[r.endLevel] = opened[r.endLevel] or
                SmwOverworldWalk.dirBit(SmwOverworldWalk.opposite(r.endDirection))
            settings = opened
        }
        route = null
        // ¿Ha parado en una tubería? Entonces salta al otro mundo, como en el juego.
        val exit = SmwOverworldWalk.exitAt(exits, world, r.endX, r.endY)
        if (exit != null) {
            world = exit.dstSubmap
            marioX = exit.dstX
            marioY = exit.dstY
            persist(save.movedTo(exit.dstSubmap, SmwOverworldWalk.position(exit.dstSubmap, exit.dstX, exit.dstY)))
        } else {
            persist(save.movedTo(world, SmwOverworldWalk.position(world, r.endX, r.endY)))
        }
    }

    val playLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val won = result.data?.getBooleanExtra(PlatformerActivity.RESULT_WON, false) == true
        if (won && hereLevel > 0) {
            // Superar el nivel abre su camino: la dirección sale de $04:D678 según la salida.
            val bit = SmwOverworldWalk.unlockedDirection(rom, delta, hereLevel, exitAction = 1)
            val opened = settings.copyOf()
            if (hereLevel < opened.size) opened[hereLevel] = opened[hereLevel] or bit or 0x80
            settings = opened
            val ev = eventTable.getOrNull(hereLevel) ?: SmwOverworld.EVENT_NONE
            persist(save.withLevelBeaten(hereLevel, ev))
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF121216)).safeDrawingPadding().padding(8.dp)) {
        Text(
            "Ranura $slot · ${save.completedTranslevels.size} niveles · " +
                "${save.firedEvents.size} caminos · ${save.lives} vidas",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFBBBBBB),
        )
        if (view == null) {
            Text("Esta ROM no parece ser Super Mario World.", color = Color.White)
            return@Column
        }
        val marioBitmap = marioSprites[facing]
        Box(Modifier.fillMaxWidth().aspectRatio(view.viewWidth.toFloat() / view.viewHeight)) {
            Image(
                bitmap = view.bitmap.asImageBitmap(),
                contentDescription = "Mapa del mundo",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            // Mario, con su sprite real de la ROM, en la casilla EXACTA en la que está. Si por
            // lo que sea no se pudo hornear el sprite, cae a un marcador para no quedarse ciego.
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / view.viewWidth
                val sy = size.height / view.viewHeight
                val left = (marioX * TILE - view.originX) * sx
                val top = (marioY * TILE - view.originY) * sy
                if (marioBitmap != null) {
                    val dst = IntSize((TILE * sx).toInt(), (TILE * sy).toInt())
                    drawImage(
                        image = marioBitmap.asImageBitmap(),
                        srcSize = IntSize(marioBitmap.width, marioBitmap.height),
                        dstOffset = IntOffset(left.toInt(), top.toInt()),
                        dstSize = dst,
                        filterQuality = FilterQuality.None,
                    )
                } else {
                    drawRect(
                        color = Color(0xFFFF4136),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(TILE * sx, TILE * sy),
                        style = Stroke(width = 4f),
                    )
                }
            }
        }

        val name = if (hereLevel > 0) SmwLevelNames.nameOfTranslevel(rom, delta, hereLevel) else null
        Text(
            name ?: if (hereLevel > 0) "Nivel $hereLevel" else "Camino",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp),
        )

        val walking = route != null
        // Cruceta. Solo se encienden las direcciones que el juego tiene abiertas desde aquí.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            for ((dir, label) in listOf(
                SmwOverworldWalk.DIR_LEFT to "◀",
                SmwOverworldWalk.DIR_UP to "▲",
                SmwOverworldWalk.DIR_DOWN to "▼",
                SmwOverworldWalk.DIR_RIGHT to "▶",
            )) {
                val can = !walking && dir in open
                Button(
                    onClick = {
                        route = SmwOverworldWalk.walk(
                            walkTiles, levelNumbers, world, marioX, marioY, dir, exitStops,
                        )
                    },
                    enabled = can,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                ) { Text(label) }
            }
        }

        val playable = if (hereLevel > 0) SmwLevelNames.levelOfTranslevel(hereLevel) else null
        if (playable != null && !walking) {
            Button(
                onClick = {
                    runCatching {
                        playLauncher.launch(PlatformerActivity.intent(context, romFile, playable))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) { Text("▶ Entrar al nivel") }
        }
        if (open.isEmpty() && !walking) {
            Text(
                "No hay camino abierto desde aquí. Supera este nivel para abrirlo.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// MODO PRUEBA: el mapa entero, tocando casillas, sin partida
// ---------------------------------------------------------------------------------------

/**
 * El mapa **para probar desde el editor**: lo enseña todo abierto, se toca una casilla y se
 * entra al nivel. No carga ni escribe ninguna partida, a propósito: cuando estás iterando
 * sobre un nivel, una progresión guardada solo estorba.
 */
@Composable
private fun TestMapScreen(rom: ByteArray, header: SnesHeader, romFile: File) {
    val context = LocalContext.current
    val delta = remember(rom) { header.headerOffset - 0x7FC0 }
    var world by remember { mutableStateOf(SmwGameSave.MAIN_MAP) }
    var showAll by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<SmwOverworldLevels.OwLevel?>(null) }

    val view = remember(world, showAll) {
        renderView(rom, header, delta, world, cameraCrop = false) { showAll }
    }
    val levels = remember(rom) { SmwOverworldLevels.levels(rom, delta) }
    val onMain = world == SmwGameSave.MAIN_MAP
    val shown = remember(world, levels) { levels.filter { it.onMainMap == onMain } }

    Column(Modifier.fillMaxSize().background(Color(0xFF121216)).safeDrawingPadding().padding(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            TextButton(onClick = { world = SmwGameSave.MAIN_MAP; selected = null }) {
                Text("Principal", color = if (onMain) Color.White else Color.Gray)
            }
            for (sm in 1..SmwOverworld.SUBMAP_COUNT) {
                TextButton(onClick = { world = sm; selected = null }) {
                    Text("Mundo $sm", color = if (world == sm) Color.White else Color.Gray)
                }
            }
        }
        Row {
            TextButton(onClick = { showAll = false }) {
                Text("Sin abrir", color = if (!showAll) Color.White else Color.Gray)
            }
            TextButton(onClick = { showAll = true }) {
                Text("Todo abierto", color = if (showAll) Color.White else Color.Gray)
            }
        }
        if (view == null) {
            Text("Esta ROM no parece ser Super Mario World.", color = Color.White)
            return@Column
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(view.viewWidth.toFloat() / view.viewHeight)
                .pointerInput(world, shown) {
                    detectTapGestures { off ->
                        val mx = view.originX + (off.x / size.width * view.viewWidth).toInt()
                        val my = view.originY + (off.y / size.height * view.viewHeight).toInt()
                        selected = shown.firstOrNull {
                            abs(it.x + TILE / 2 - mx) <= TILE && abs(it.y + TILE / 2 - my) <= TILE
                        }
                    }
                },
        ) {
            Image(
                bitmap = view.bitmap.asImageBitmap(),
                contentDescription = "Mapa del mundo",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / view.viewWidth
                val sy = size.height / view.viewHeight
                for (lv in shown) {
                    val isSel = lv === selected
                    drawRect(
                        color = if (isSel) Color(0xFFFFEB3B) else Color(0x66FFFFFF),
                        topLeft = Offset((lv.x - view.originX) * sx, (lv.y - view.originY) * sy),
                        size = androidx.compose.ui.geometry.Size(TILE * sx, TILE * sy),
                        style = Stroke(width = if (isSel) 3f else 1f),
                    )
                }
            }
        }
        val lv = selected
        if (lv == null) {
            Text(
                "Modo prueba: toca una casilla para entrar al nivel. " +
                    "${shown.size} niveles en esta vista.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Text(
                lv.name ?: "Nivel ${lv.levelNumber}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp),
            )
            val level = SmwLevelNames.levelOfTranslevel(lv.levelNumber)
            if (level == null) {
                Text("Este translevel no tiene nivel jugable.", color = Color(0xFFBBBBBB))
            } else {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                PlatformerActivity.intent(context, romFile, level),
                            )
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text("▶ Probar (nivel ${level.toString(16).uppercase()})") }
            }
        }
    }
}
