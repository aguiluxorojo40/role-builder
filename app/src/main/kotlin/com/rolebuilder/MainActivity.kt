package com.rolebuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rolebuilder.editor.EditorScreen
import com.rolebuilder.project.ProjectListScreen
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/** Punto de entrada: gestor de proyectos y editor. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    NavHost(navController = navController, startDestination = "projects") {
        composable("projects") {
            ProjectListScreen(
                onOpenProject = { dir ->
                    navController.navigate("editor/${URLEncoder.encode(dir.absolutePath, "UTF-8")}")
                },
            )
        }
        composable("editor/{dir}") { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("dir").orEmpty()
            val dir = File(URLDecoder.decode(encoded, "UTF-8"))
            EditorScreen(
                projectDir = dir,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
