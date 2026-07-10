package com.rolebuilder.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolebuilder.core.model.GameMode

/**
 * Pantalla de arranque: selector de MODO/motor. Divide la app en dos mundos —
 * Role Builder (ARPG top-down) y Platform Builder (plataformas estilo SMW)—;
 * cada uno abre su propia lista de proyectos filtrada por [GameMode].
 */
@Composable
fun ModeSelectScreen(onSelect: (GameMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Role Builder",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Elige el motor con el que quieres crear",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        ModeCard(
            emoji = "🗡️",
            title = "Role Builder",
            subtitle = "Aventura ARPG con vista cenital, mapas por capas, NPCs, cofres y combate.",
            accent = Color(0xFF6C8EFF),
            onClick = { onSelect(GameMode.ARPG) },
        )
        ModeCard(
            emoji = "🍄",
            title = "Platform Builder",
            subtitle = "Plataformas de scroll lateral estilo SMW: gravedad, saltos, pendientes y pinchos.",
            accent = Color(0xFF7ED957),
            onClick = { onSelect(GameMode.PLATFORMER) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(150.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(emoji, fontSize = 40.sp)
                Text(title, style = MaterialTheme.typography.titleLarge, color = accent)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
