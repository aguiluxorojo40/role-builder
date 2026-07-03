package com.rolebuilder.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rolebuilder.core.model.MapReachability
import com.rolebuilder.editor.database.DatabaseTab
import com.rolebuilder.editor.map.MapEditorTab
import com.rolebuilder.player.PlayerActivity
import java.io.File

/** Editor de un proyecto: pestañas de mapa, base de datos y ajustes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(projectDir: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val state = remember(projectDir) {
        runCatching { com.rolebuilder.project.ProjectStore.ensureDefaultImages(context, projectDir) }
        EditorState(projectDir)
    }
    var tab by remember { mutableIntStateOf(0) }
    var orphanWarningNames by remember { mutableStateOf<List<String>?>(null) }

    // Guardado automático al salir del editor.
    DisposableEffect(state) {
        onDispose { if (state.dirty) state.save() }
    }

    // Diálogo de advertencia de mapas huérfanos.
    orphanWarningNames?.let { names ->
        AlertDialog(
            onDismissRequest = { orphanWarningNames = null },
            title = { Text("Mapas inaccesibles") },
            text = {
                Text(
                    "Los siguientes mapas no son alcanzables desde el mapa inicial " +
                        "porque no hay ningún evento TransferPlayer que lleve a ellos:\n\n" +
                        names.joinToString("\n") { "• $it" } +
                        "\n\nCrea un evento con el comando \"Transferir jugador\" en un mapa " +
                        "accesible para conectarlos.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    orphanWarningNames = null
                    state.save()
                    context.startActivity(PlayerActivity.intent(context, projectDir))
                }) { Text("Jugar de todas formas") }
            },
            dismissButton = {
                TextButton(onClick = { orphanWarningNames = null }) { Text("Volver al editor") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.project.name + if (state.dirty) " •" else "") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.dirty) state.save()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        state.save()
                        Toast.makeText(context, "Proyecto guardado", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Guardar")
                    }
                    IconButton(onClick = {
                        state.save()
                        val maps = state.mapList.associateBy { it.id }
                        val orphans = MapReachability.findOrphanMaps(state.project.startMapId, maps)
                        if (orphans.isNotEmpty()) {
                            orphanWarningNames = orphans.mapNotNull { maps[it]?.name }
                        } else {
                            context.startActivity(PlayerActivity.intent(context, projectDir))
                        }
                    }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Probar juego")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    label = { Text("Mapa") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Storage, contentDescription = null) },
                    label = { Text("Base de datos") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> MapEditorTab(state)
                1 -> DatabaseTab(state)
                else -> SettingsTab(state)
            }
        }
    }
}
