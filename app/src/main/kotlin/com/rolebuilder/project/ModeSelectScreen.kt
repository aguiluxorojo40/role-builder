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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolebuilder.core.model.GameMode

/**
 * Pantalla de arranque. Separa los dos oficios de la app, que conviene no mezclar:
 *
 *  - **CREAR** — Role Builder (ARPG cenital) y Platform Builder (plataformas estilo SMW).
 *    Cada uno abre su lista de proyectos filtrada por [GameMode]. Ahí se edita y se PRUEBA.
 *  - **JUGAR** — Super Mario World de principio a fin desde tu ROM: título, mapa del mundo,
 *    niveles y partida guardada. No pasa por el editor ni comparte estado con él.
 *
 * [onPlaySmw] recibe el control de la puerta de jugar; [romChosen] solo cambia el texto de la
 * tarjeta (elegir ROM la primera vez vs. entrar directo), y [onChangeRom] permite cambiarla.
 */
@Composable
fun ModeSelectScreen(
    onSelect: (GameMode) -> Unit,
    onPlaySmw: () -> Unit = {},
    onChangeRom: () -> Unit = {},
    romChosen: Boolean = false,
) {
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
            "Crea tu juego — o juega a Super Mario World desde tu ROM",
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
        ModeCard(
            emoji = "🎮",
            title = "Jugar Super Mario World",
            subtitle = if (romChosen) {
                "Título, mapa del mundo y niveles desde tu ROM, con partida guardada."
            } else {
                "Elige tu ROM de SMW y juega: título, mapa del mundo y niveles, con guardado."
            },
            accent = Color(0xFFFFC94D),
            onClick = onPlaySmw,
        )
        if (romChosen) {
            TextButton(onClick = onChangeRom) {
                Text("Cambiar la ROM del juego", style = MaterialTheme.typography.bodySmall)
            }
        }
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
