package com.rolebuilder.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.model.GameMode
import com.rolebuilder.player.PlatformerActivity
import com.rolebuilder.player.PlayerActivity
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Lista de proyectos de un MODO (mundo) concreto: crear/editar/jugar/borrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(mode: GameMode, onOpenProject: (File) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    fun load() = ProjectStore.list(context).filter { it.mode == mode }
    var projects by remember(mode) { mutableStateOf(load()) }
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ProjectSummary?>(null) }
    var exportingDir by remember { mutableStateOf<File?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val dir = exportingDir
        exportingDir = null
        if (uri != null && dir != null) {
            runCatching { ProjectStore.export(context, dir, uri) }
                .onSuccess { Toast.makeText(context, "Proyecto exportado", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Error al exportar: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { ProjectStore.import(context, uri) }
                .onSuccess { name ->
                    projects = load()
                    Toast.makeText(context, "Importado: $name", Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(context, "Error al importar: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    val worldTitle = if (mode == GameMode.PLATFORMER) "🍄 Platform Builder" else "🗡️ Role Builder"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$worldTitle — Mis proyectos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cambiar de modo")
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch("application/zip") }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Importar proyecto (.zip)")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo proyecto")
            }
        },
    ) { padding ->
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Aún no tienes proyectos aquí.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (mode == GameMode.PLATFORMER) {
                        "Pulsa + para crear un proyecto de plataformas: importa niveles SMW como mapas jugables."
                    } else {
                        "Pulsa + para crear tu primer RPG: incluye un mapa de ejemplo con NPC, cofre y enemigos."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(projects, key = { it.dir.absolutePath }) { summary ->
                    Card {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    DateFormat.getDateTimeInstance().format(Date(summary.lastModified)),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(onClick = {
                                val intent = if (mode == GameMode.PLATFORMER) {
                                    PlatformerActivity.intentForProject(context, summary.dir)
                                } else {
                                    PlayerActivity.intent(context, summary.dir)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Jugar")
                            }
                            IconButton(onClick = { onOpenProject(summary.dir) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = {
                                exportingDir = summary.dir
                                exportLauncher.launch("${summary.name}.zip")
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Exportar (.zip)")
                            }
                            IconButton(onClick = { toDelete = summary }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Nuevo proyecto — $worldTitle") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    val dir = ProjectStore.create(context, name, mode)
                    projects = load()
                    showCreate = false
                    onOpenProject(dir)
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancelar") }
            },
        )
    }

    toDelete?.let { summary ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Borrar \"${summary.name}\"") },
            text = { Text("Se borrará el proyecto y sus partidas guardadas. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = {
                    ProjectStore.delete(summary.dir)
                    projects = load()
                    toDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancelar") }
            },
        )
    }
}
