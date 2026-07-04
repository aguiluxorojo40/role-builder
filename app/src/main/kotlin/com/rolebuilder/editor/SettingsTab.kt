package com.rolebuilder.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.editor.widgets.FloatField
import java.io.File

/** Ajustes del proyecto: nombre, inicio, switches/variables e importación de imágenes. */
@Composable
fun SettingsTab(state: EditorState) {
    val context = LocalContext.current
    var showSwitches by remember { mutableStateOf(false) }
    var showVariables by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val name = (context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            } ?: "imagen_${System.currentTimeMillis()}.png").let {
                if (it.endsWith(".png", ignoreCase = true)) it else "$it.png"
            }
            val dest = ProjectIo.imageFile(state.projectDir, name)
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            Toast.makeText(context, "Importada: $name", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "No se pudo importar: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = state.project.name,
            onValueChange = { state.updateProject(state.project.copy(name = it)) },
            label = { Text("Nombre del juego") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            "Inicio: mapa ${state.project.startMapId} en (${state.project.startX}, ${state.project.startY}). " +
                "Usa la herramienta \"Inicio\" del editor de mapas para cambiarlo.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("Estilo visual", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = state.project.hd2d,
                onCheckedChange = { state.updateProject(state.project.copy(hd2d = it)) },
            )
            Text("  Estilo HD-2D (tilt-shift, bloom, viñeta, sombras y motas de luz)")
        }
        Text(
            "Efecto maqueta al estilo Octopath Traveler: la franja central se ve nítida " +
                "y los bordes superior e inferior se desenfocan. Las luces de los eventos " +
                "brillan con bloom.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Intensidad del efecto · ${(state.project.hd2dStrength * 100).toInt()}%")
        Slider(
            value = state.project.hd2dStrength,
            onValueChange = { state.updateProject(state.project.copy(hd2dStrength = it)) },
            valueRange = 0f..2f,
            enabled = state.project.hd2d,
        )
        Text("Inclinación del diorama 2.5D · ${state.project.dioramaTilt.toInt()}°")
        Slider(
            value = state.project.dioramaTilt,
            onValueChange = { state.updateProject(state.project.copy(dioramaTilt = it)) },
            valueRange = 0f..60f,
            enabled = state.project.hd2d,
        )
        Text("Altura de los sprites 2.5D · ${(state.project.spriteStand * 100).toInt()}%")
        Slider(
            value = state.project.spriteStand,
            onValueChange = { state.updateProject(state.project.copy(spriteStand = it)) },
            valueRange = 0.5f..2.5f,
            enabled = state.project.hd2d,
        )
        Text(
            "La inclinación tumba el plano del suelo como una maqueta 2.5D; la altura " +
                "estira a los personajes y a los tiles marcados \"De pie\" para exagerar " +
                "cuánto se levantan del terreno. Puedes ajustar los tres potenciómetros " +
                "EN VIVO desde el menú de pausa mientras pruebas el juego.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("Switches y variables", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { showSwitches = true }) { Text("Nombrar switches") }
            Button(onClick = { showVariables = true }) { Text("Nombrar variables") }
        }

        HorizontalDivider()

        Text("Imágenes del proyecto", style = MaterialTheme.typography.titleSmall)
        Text(
            "Hojas de personaje: PNG de 3 columnas × 4 filas (abajo, izquierda, derecha, arriba). " +
                "Tilesets: cuadrícula de tiles de 16 px.",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = { importLauncher.launch("image/png") }) { Text("Importar imagen PNG") }
        Text(
            "Disponibles: " + state.imageNames().joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (showSwitches) {
        NameListDialog(
            title = "Switches",
            names = state.project.switchNames,
            onChange = { state.updateProject(state.project.copy(switchNames = it)) },
            onDismiss = { showSwitches = false },
        )
    }
    if (showVariables) {
        NameListDialog(
            title = "Variables",
            names = state.project.variableNames,
            onChange = { state.updateProject(state.project.copy(variableNames = it)) },
            onDismiss = { showVariables = false },
        )
    }
}

@Composable
private fun NameListDialog(
    title: String,
    names: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(names) { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            onChange(names.mapIndexed { i, n -> if (i == index) newName else n })
                        },
                        label = { Text("%04d".format(index + 1)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    }
}
