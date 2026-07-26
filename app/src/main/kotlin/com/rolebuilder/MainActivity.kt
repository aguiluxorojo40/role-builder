package com.rolebuilder

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.GameMode
import com.rolebuilder.editor.EditorScreen
import com.rolebuilder.editor.platform.PlatformEditorScreen
import com.rolebuilder.player.GameRom
import com.rolebuilder.player.TitleActivity
import com.rolebuilder.project.ModeSelectScreen
import com.rolebuilder.project.ProjectListScreen
import com.rolebuilder.project.ProjectStore
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/** Punto de entrada: gestor de proyectos y editor (o juego independiente). */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si el APK lleva un juego embebido (assets/standalone_game), esta
        // build ES ese juego: arranca directo a su pantalla de título.
        val standalone = ProjectStore.installStandaloneIfPresent(this)
        if (standalone != null) {
            startActivity(com.rolebuilder.player.PlayerActivity.intent(this, standalone))
            finish()
            return
        }

        setContent {
            RoleBuilderTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun RoleBuilderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6C8EFF),
            secondary = Color(0xFFFFC94D),
            background = Color(0xFF0D1220),
            surface = Color(0xFF141B30),
            surfaceVariant = Color(0xFF1D2742),
        ),
    ) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "mode") {
        composable("mode") {
            val context = LocalContext.current
            // La ROM del JUEGO se elige una vez y se queda ([GameRom]); el editor sigue
            // pidiendo la suya cada vez, que es lo suyo cuando estás trasteando con varias.
            var romChosen by remember { mutableStateOf(GameRom.isPresent(context)) }
            val pickRom = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                val file = uri?.let { GameRom.store(context, it) }
                if (file == null) {
                    if (uri != null) {
                        Toast.makeText(context, "No se pudo leer la ROM", Toast.LENGTH_LONG).show()
                    }
                } else {
                    romChosen = true
                    context.startActivity(TitleActivity.intent(context, file))
                }
            }
            ModeSelectScreen(
                onSelect = { mode -> navController.navigate("projects/${mode.name}") },
                onPlaySmw = {
                    if (romChosen) {
                        context.startActivity(TitleActivity.intent(context, GameRom.file(context)))
                    } else {
                        pickRom.launch(arrayOf("*/*"))
                    }
                },
                onChangeRom = { pickRom.launch(arrayOf("*/*")) },
                romChosen = romChosen,
            )
        }
        composable("projects/{mode}") { backStackEntry ->
            val mode = runCatching {
                GameMode.valueOf(backStackEntry.arguments?.getString("mode").orEmpty())
            }.getOrDefault(GameMode.ARPG)
            ProjectListScreen(
                mode = mode,
                onOpenProject = { dir ->
                    navController.navigate("editor/${URLEncoder.encode(dir.absolutePath, "UTF-8")}")
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable("editor/{dir}") { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("dir").orEmpty()
            val dir = File(URLDecoder.decode(encoded, "UTF-8"))
            // La interfaz depende del tipo de proyecto: Platform Builder (estilo Lunar
            // Magic) para PLATFORMER, Role Builder (estilo RPG Maker) para ARPG.
            val mode = remember(dir) {
                runCatching { ProjectIo.loadProject(dir).mode }.getOrDefault(GameMode.ARPG)
            }
            if (mode == GameMode.PLATFORMER) {
                PlatformEditorScreen(projectDir = dir, onBack = { navController.popBackStack() })
            } else {
                EditorScreen(projectDir = dir, onBack = { navController.popBackStack() })
            }
        }
    }
}
