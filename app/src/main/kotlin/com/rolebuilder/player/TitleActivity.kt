package com.rolebuilder.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.snes.SmwGameSave
import com.rolebuilder.core.snes.SmwSaveIo
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.editor.snes.SmwAssetStore
import com.rolebuilder.editor.snes.SnesImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * PANTALLA DE TÍTULO: la puerta del **juego**, separada del editor.
 *
 * El fondo no es un dibujo nuestro: es la pantalla de título de SMW reconstruida desde la ROM
 * del usuario ([com.rolebuilder.core.snes.SmwTitleScreen] — nivel de fondo 0x0C7 + el logo de
 * Layer 3 con su paleta real). Encima van las tres ranuras de guardado, como en el original.
 *
 * De aquí solo se sale a jugar: título → mapa del mundo en modo juego → nivel → vuelta al mapa
 * con el camino abierto y la partida guardada. El editor tiene su propia puerta y nunca pasa
 * por aquí.
 *
 * Al entrar se **hornean** los assets de la ROM en el dispositivo si aún no lo estaban
 * ([SmwAssetStore]); en el repositorio no va ni un byte de Nintendo.
 */
class TitleActivity : ComponentActivity() {

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
        setContent { TitleScreen(rom, romFile) }
    }

    companion object {
        private const val EXTRA_ROM_PATH = "rom_path"

        /** Abre el título del juego con la ROM del usuario. */
        fun intent(context: Context, romFile: File): Intent =
            Intent(context, TitleActivity::class.java)
                .putExtra(EXTRA_ROM_PATH, romFile.absolutePath)
    }
}

@Composable
private fun TitleScreen(rom: ByteArray, romFile: File) {
    val context = LocalContext.current
    val saveDir = remember(context) { File(context.filesDir, SmwSaveIo.DIR) }
    // Se relee tras borrar una ranura; por eso el contador, y no una lista fija.
    var reload by remember { mutableStateOf(0) }
    val slots = remember(reload) { SmwSaveIo.slots(saveDir) }
    var title by remember { mutableStateOf<Bitmap?>(null) }
    var preparing by remember { mutableStateOf(true) }

    // Dibujar el título descomprime un nivel entero, y hornear los assets recorre todos los
    // sprites y sonidos de la ROM. Las dos cosas fuera del hilo de interfaz: en el principal
    // bloquearían la pantalla y Android mataría la app por no responder.
    LaunchedEffect(rom) {
        title = withContext(Dispatchers.IO) {
            val header = SnesDecoder.parseHeader(rom)
            if (!SmwAssetStore.isBaked(context)) runCatching { SmwAssetStore.bake(context, rom) }
            runCatching { SnesImport.titleScreen(rom, header) }.getOrNull()
        }
        preparing = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        title?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Super Mario World",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            if (preparing) {
                Text(
                    "Preparando el juego desde tu ROM…",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            } else if (title == null) {
                Text(
                    "Esta ROM no parece ser Super Mario World.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
            slots.forEachIndexed { index, save ->
                SlotCard(
                    slot = index + 1,
                    save = save,
                    onPlay = {
                        context.startActivity(
                            OverworldActivity.gameIntent(context, romFile, index + 1),
                        )
                    },
                    onErase = {
                        SmwSaveIo.delete(saveDir, index + 1)
                        reload++
                    },
                )
            }
        }
    }
}

/**
 * Una ranura. Vacía ofrece "Nueva partida"; con datos, "Continuar" más su progreso y la fecha
 * del último guardado, para no tener que adivinar cuál es cuál.
 */
@Composable
private fun SlotCard(slot: Int, save: SmwGameSave?, onPlay: () -> Unit, onErase: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC141B30)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Ranura $slot",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    if (save == null || save.isNew) {
                        "Vacía"
                    } else {
                        val fecha = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(save.savedAtEpochMs))
                        "${save.completedTranslevels.size} niveles · " +
                            "${save.firedEvents.size} caminos · $fecha"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBBBBBB),
                )
            }
            if (save != null && !save.isNew) {
                TextButton(onClick = onErase) { Text("Borrar", color = Color(0xFFFF8A80)) }
            }
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7ED957)),
            ) {
                Text(
                    if (save == null || save.isNew) "Nueva partida" else "Continuar",
                    color = Color(0xFF102010),
                )
            }
        }
    }
}
