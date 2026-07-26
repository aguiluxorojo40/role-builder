package com.rolebuilder.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwOverworld
import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesHeader
import com.rolebuilder.editor.snes.SnesImport
import java.io.File
import kotlin.math.abs

/**
 * MAPA DEL MUNDO navegable. Muestra el overworld de SMW renderizado desde la ROM del usuario
 * (con los eventos aplicados, o sea con los caminos abiertos) y deja **tocar una casilla-de-
 * nivel para jugarla**: se resuelve su translevel → leveldata con
 * [SmwLevelNames.levelOfTranslevel] y se lanza [PlatformerActivity].
 *
 * Se puede alternar entre el **mapa principal** (pantallas 0-3) y el **área de submapas**
 * (pantallas 4-7, donde viven Yoshi's Island, Vanilla Dome, el Valle de Bowser, el Bosque,
 * Special y Star World). Los dos son de 512×512, así que el toque se convierte a coordenadas
 * del mapa con una simple regla de tres.
 *
 * Nota honesta sobre el alcance: aquí se navega **tocando** las casillas, no moviendo a Mario
 * paso a paso por los caminos. El andar casilla-a-casilla necesita portar el manejo de
 * caminos del juego (`OwProcess04_PlayerIsMoving` y compañía) y la progresión guardada; eso
 * es la siguiente fase y no se finge aquí.
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
        setContent {
            OverworldScreen(rom, header) { level ->
                runCatching { startActivity(PlatformerActivity.intent(this, romFile, level)) }
                    .onFailure { Toast.makeText(this, "No se pudo entrar: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    companion object {
        private const val EXTRA_ROM_PATH = "rom_path"

        /** Abre el mapa del mundo con la ROM del usuario. */
        fun intent(context: Context, romFile: File): Intent =
            Intent(context, OverworldActivity::class.java)
                .putExtra(EXTRA_ROM_PATH, romFile.absolutePath)
    }
}

/** Lado en píxeles de los dos mapas del overworld (pantallas en 2×2). */
private const val MAP_SIDE = 512
/** Lado de una casilla del mapa (un bloque Map16). */
private const val TILE = 16

@Composable
private fun OverworldScreen(rom: ByteArray, header: SnesHeader, onPlay: (Int) -> Unit) {
    val context = LocalContext.current
    val delta = remember(rom) { header.headerOffset - 0x7FC0 }
    var showSubmaps by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SmwOverworldLevels.OwLevel?>(null) }
    // PROGRESIÓN: cuántos eventos del overworld están aplicados. 0 = partida nueva (casi sin
    // caminos), el total = juego al 100%. Es el mismo mecanismo con el que el juego abre los
    // caminos al superar niveles ([SmwOverworld.overworldTilemapWithEvents]).
    var events by remember { mutableStateOf(SmwOverworld.EVENT_COUNT) }

    // Render del mapa (se rehace al cambiar de vista o de progresión; lleva su rato).
    val bitmap: Bitmap? = remember(showSubmaps, events) {
        val img = if (showSubmaps) SnesGameRecipes.renderOverworldSubmapArea(rom, header, 1, events)
        else SnesGameRecipes.renderOverworldMainMap(rom, header, events)
        img?.let { SnesImport.toBitmap(it) }
    }
    val levels = remember(rom) { SmwOverworldLevels.levels(rom, delta) }
    val shown = remember(showSubmaps, levels) { levels.filter { it.onMainMap != showSubmaps } }

    Column(Modifier.fillMaxSize().background(Color(0xFF121216)).safeDrawingPadding().padding(8.dp)) {
        Row {
            TextButton(onClick = { showSubmaps = false; selected = null }) {
                Text("Mapa principal", color = if (!showSubmaps) Color.White else Color.Gray)
            }
            TextButton(onClick = { showSubmaps = true; selected = null }) {
                Text("Submapas", color = if (showSubmaps) Color.White else Color.Gray)
            }
        }
        // Progresión: se ve cómo los eventos van abriendo los caminos del mapa.
        Row {
            TextButton(onClick = { events = 0 }) {
                Text("Partida nueva", color = if (events == 0) Color.White else Color.Gray)
            }
            TextButton(onClick = { events = 7 }) {
                Text("Yoshi's Island", color = if (events == 7) Color.White else Color.Gray)
            }
            TextButton(onClick = { events = SmwOverworld.EVENT_COUNT }) {
                Text("Todo abierto", color = if (events == SmwOverworld.EVENT_COUNT) Color.White else Color.Gray)
            }
        }
        if (bitmap == null) {
            Text("Esta ROM no parece ser Super Mario World.", color = Color.White)
            return@Column
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(showSubmaps, shown) {
                    detectTapGestures { off ->
                        // Pantalla → coordenadas del mapa de 512×512.
                        val mx = (off.x / size.width * MAP_SIDE).toInt()
                        val my = (off.y / size.height * MAP_SIDE).toInt()
                        selected = shown.firstOrNull {
                            abs(it.x + TILE / 2 - mx) <= TILE && abs(it.y + TILE / 2 - my) <= TILE
                        }
                    }
                },
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Mapa del mundo",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            // Marcadores de las casillas-de-nivel, y la seleccionada resaltada.
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / MAP_SIDE
                val sy = size.height / MAP_SIDE
                for (lv in shown) {
                    val isSel = lv === selected
                    drawRect(
                        color = if (isSel) Color(0xFFFFEB3B) else Color(0x66FFFFFF),
                        topLeft = Offset(lv.x * sx, lv.y * sy),
                        size = androidx.compose.ui.geometry.Size(TILE * sx, TILE * sy),
                        style = Stroke(width = if (isSel) 3f else 1f),
                    )
                }
            }
        }
        val lv = selected
        if (lv == null) {
            Text(
                "Toca una casilla marcada para entrar al nivel. " +
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
                    onClick = { onPlay(level) },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text("▶ Jugar (nivel ${level.toString(16).uppercase()})") }
            }
        }
    }
}
