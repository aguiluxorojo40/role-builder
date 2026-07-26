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
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.snes.SmwGameSave
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwOverworld
import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SmwSaveIo
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesHeader
import com.rolebuilder.editor.snes.SnesImport
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
 * Alcance honesto: aquí se navega **tocando** las casillas. Que Mario ande paso a paso por los
 * caminos necesita portar `OwProcess04_PlayerIsMoving` y el manejo de rutas; mientras tanto el
 * selector de mundo hace de sustituto para poder llegar a todos los mapas.
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

/**
 * Lo que se está mirando: el mapa principal o uno de los seis submapas. En modo juego el
 * submapa se recorta a su ventana de cámara, así que hay que llevar el origen para poder
 * convertir un toque en coordenadas del área.
 */
private data class MapView(
    val bitmap: Bitmap,
    val viewWidth: Int,
    val viewHeight: Int,
    val originX: Int,
    val originY: Int,
)

@Composable
private fun OverworldScreen(
    rom: ByteArray,
    header: SnesHeader,
    romFile: File,
    isGame: Boolean,
    slot: Int,
) {
    val context = LocalContext.current
    val delta = remember(rom) { header.headerOffset - 0x7FC0 }
    val saveDir = remember(context) { File(context.filesDir, SmwSaveIo.DIR) }

    // ESTADO DE LA PARTIDA. En modo juego se carga de disco (o se crea nueva) y todo lo que
    // pase aquí se persiste. En modo prueba es un objeto de usar y tirar que nadie guarda.
    var save by remember {
        mutableStateOf(
            if (isGame) SmwSaveIo.load(saveDir, slot) ?: SmwGameSave(slot = slot)
            else SmwGameSave(slot = slot),
        )
    }
    // Solo en modo prueba: ver el mapa al 100% sin haber jugado nada.
    var showAll by remember { mutableStateOf(!isGame) }
    /** [SmwGameSave.MAIN_MAP] o 1..6. En juego lo manda la partida; en prueba, los botones. */
    var world by remember { mutableStateOf(if (isGame) save.submap else SmwGameSave.MAIN_MAP) }
    var selected by remember { mutableStateOf<SmwOverworldLevels.OwLevel?>(null) }
    val eventTable = remember(rom) { SmwOverworld.translevelEvents(rom, delta) }

    // Render del mapa. Se rehace al cambiar de mundo o de progresión; lleva su rato.
    val view: MapView? = remember(world, save.firedEvents, showAll) {
        val active: (Int) -> Boolean = if (showAll) ({ true }) else save.activeEvents()
        val tilemap = SmwOverworld.overworldTilemapWithEvents(rom, delta, active)
        when {
            world == SmwGameSave.MAIN_MAP ->
                SnesGameRecipes.renderOverworldMainMapFrom(rom, header, tilemap)?.let {
                    MapView(SnesImport.toBitmap(it), MAP_SIDE, MAP_SIDE, 0, 0)
                }
            // En el juego, un submapa es SU ventana de cámara: ves ese mundo y solo ese.
            isGame ->
                SnesGameRecipes.renderOverworldSubmapFrom(rom, header, world, tilemap)?.let {
                    val camera = SmwOverworld.submapCamera(rom, delta, world)
                    MapView(
                        SnesImport.toBitmap(it),
                        SmwOverworld.OW_VIEW_WIDTH, SmwOverworld.OW_VIEW_HEIGHT,
                        camera.first, camera.second,
                    )
                }
            // Probando: el área entera, que es donde caben los seis mundos a la vez.
            else ->
                SnesGameRecipes.renderOverworldSubmapAreaFrom(rom, header, world, tilemap)?.let {
                    MapView(SnesImport.toBitmap(it), MAP_SIDE, MAP_SIDE, 0, 0)
                }
        }
    }

    // Lanzador del nivel: cuando vuelve diciendo que se SUPERÓ, se dispara su evento, el mapa
    // se redibuja con el camino nuevo abierto y —si estamos jugando— se guarda en la ranura.
    var lastPlayed by remember { mutableStateOf<Int?>(null) }
    val playLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val won = result.data?.getBooleanExtra(PlatformerActivity.RESULT_WON, false) == true
        val translevel = lastPlayed
        if (won && translevel != null) {
            val ev = eventTable.getOrNull(translevel) ?: SmwOverworld.EVENT_NONE
            val next = save.withLevelBeaten(translevel, ev)
            save = if (isGame) SmwSaveIo.save(saveDir, next) else next
            showAll = false
        }
    }

    val levels = remember(rom) { SmwOverworldLevels.levels(rom, delta) }
    val onMain = world == SmwGameSave.MAIN_MAP
    val shown = remember(world, levels) { levels.filter { it.onMainMap == onMain } }

    Column(Modifier.fillMaxSize().background(Color(0xFF121216)).safeDrawingPadding().padding(8.dp)) {
        // Selector de mundo. Hasta que Mario ande por los caminos, esto es lo que permite
        // llegar a los otros mapas; en el juego de verdad te llevan las salidas de los niveles.
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
        if (isGame) {
            Text(
                "Ranura $slot · ${save.completedTranslevels.size} niveles superados · " +
                    "${save.firedEvents.size} caminos abiertos · ${save.lives} vidas",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBBBBBB),
            )
        } else {
            // Solo probando: el atajo de verlo todo abierto sin haber jugado.
            Row {
                TextButton(onClick = { showAll = false }) {
                    Text(
                        "Sin abrir (${save.firedEvents.size})",
                        color = if (!showAll) Color.White else Color.Gray,
                    )
                }
                TextButton(onClick = { showAll = true }) {
                    Text("Todo abierto", color = if (showAll) Color.White else Color.Gray)
                }
            }
        }

        val v = view
        if (v == null) {
            Text("Esta ROM no parece ser Super Mario World.", color = Color.White)
            return@Column
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(v.viewWidth.toFloat() / v.viewHeight)
                .pointerInput(world, shown) {
                    detectTapGestures { off ->
                        // Pantalla → coordenadas del área del mapa (sumando el origen de la
                        // cámara cuando se está viendo la ventana de un submapa).
                        val mx = v.originX + (off.x / size.width * v.viewWidth).toInt()
                        val my = v.originY + (off.y / size.height * v.viewHeight).toInt()
                        selected = shown.firstOrNull {
                            abs(it.x + TILE / 2 - mx) <= TILE && abs(it.y + TILE / 2 - my) <= TILE
                        }
                    }
                },
        ) {
            Image(
                bitmap = v.bitmap.asImageBitmap(),
                contentDescription = "Mapa del mundo",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            // Marcadores de las casillas-de-nivel: la seleccionada resaltada, las ya superadas
            // en verde para que se vea de un vistazo por dónde vas.
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / v.viewWidth
                val sy = size.height / v.viewHeight
                for (lv in shown) {
                    val isSel = lv === selected
                    val beaten = lv.levelNumber in save.completedTranslevels
                    drawRect(
                        color = when {
                            isSel -> Color(0xFFFFEB3B)
                            beaten -> Color(0x8877E777)
                            else -> Color(0x66FFFFFF)
                        },
                        topLeft = Offset((lv.x - v.originX) * sx, (lv.y - v.originY) * sy),
                        size = androidx.compose.ui.geometry.Size(TILE * sx, TILE * sy),
                        style = Stroke(width = if (isSel) 3f else 1f),
                    )
                }
            }
        }

        val lv = selected
        if (lv == null) {
            Text(
                "Toca una casilla marcada para entrar al nivel. ${shown.size} niveles en esta vista.",
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
            if (lv.levelNumber in save.completedTranslevels) {
                Text(
                    "✓ Ya superado",
                    color = Color(0xFF77E777),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val level = SmwLevelNames.levelOfTranslevel(lv.levelNumber)
            if (level == null) {
                Text("Este translevel no tiene nivel jugable.", color = Color(0xFFBBBBBB))
            } else {
                Button(
                    onClick = {
                        lastPlayed = lv.levelNumber
                        // Al entrar se apunta dónde está Mario: si cierras la app y vuelves,
                        // el mapa se abre en el mundo correcto.
                        if (isGame) save = SmwSaveIo.save(saveDir, save.movedTo(world, lv.position))
                        runCatching {
                            playLauncher.launch(PlatformerActivity.intent(context, romFile, level))
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text("▶ Jugar (nivel ${level.toString(16).uppercase()})") }
            }
        }
    }
}
