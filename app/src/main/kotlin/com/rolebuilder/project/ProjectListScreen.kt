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
import com.rolebuilder.player.PlayerActivity
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Pantalla inicial: lista de proyectos con crear/editar/jugar/borrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(onOpenProject: (File) -> Unit) {
    val context = LocalContext.current
    var projects by remember { mutableStateOf(ProjectStore.list(context)) }
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
                    projects = ProjectStore.list(context)
                    Toast.makeText(context, "Importado: $name", Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(context, "Error al importar: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Role Builder — Mis proyectos") },
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
                Text("Aún no tienes proyectos.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pulsa + para crear tu primer RPG: incluye un mapa de ejemplo con NPC, cofre y enemigos.",
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
                                context.startActivity(PlayerActivity.intent(context, summary.dir))
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
        var mode by remember { mutableStateOf(GameMode.ARPG) }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Nuevo proyecto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                    )
                    Text("Modo del motor:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ModeChoice("🗡️ Role Builder", "acción RPG", mode == GameMode.ARPG, Modifier.weight(1f)) { mode = GameMode.ARPG }
                        ModeChoice("🍄 Platform Builder", "plataformas", mode == GameMode.PLATFORMER, Modifier.weight(1f)) { mode = GameMode.PLATFORMER }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val dir = ProjectStore.create(context, name, mode)
                    projects = ProjectStore.list(context)
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
                    projects = ProjectStore.list(context)
                    toDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

/** Botón-selector de modo del motor para el diálogo de creación de proyecto. */
@Composable
private fun ModeChoice(
    label: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }
    } else {
        TextButton(onClick = onClick, modifier = modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
