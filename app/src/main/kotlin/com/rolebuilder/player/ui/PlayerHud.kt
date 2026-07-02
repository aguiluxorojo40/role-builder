package com.rolebuilder.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Joystick virtual: comunica un vector (-1..1, -1..1); (0,0) al soltar. */
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onChange: (Float, Float) -> Unit,
) {
    var knob by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(150.dp)
            .pointerInput(Unit) {
                val radius = size.width / 2f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(radius, radius)

                    fun update(position: Offset) {
                        var v = (position - center) / radius
                        val len = sqrt(v.x * v.x + v.y * v.y)
                        if (len > 1f) v /= len
                        knob = v
                        onChange(v.x, v.y)
                    }

                    update(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        update(change.position)
                        change.consume()
                    }
                    knob = Offset.Zero
                    onChange(0f, 0f)
                }
            }
            .background(Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((knob.x * 45.dp.toPx()).roundToInt(), (knob.y * 45.dp.toPx()).roundToInt()) }
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.35f), CircleShape),
        )
    }
}

/** Botón de acción redondo (A/B). */
@Composable
fun ActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .background(color.copy(alpha = 0.35f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    onPress()
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

/** Barra de vida en corazones (cada corazón = 2 PV). */
@Composable
fun HeartsRow(hp: Int, maxHp: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val hearts = (maxHp + 1) / 2
        for (i in 0 until hearts) {
            val heartHp = hp - i * 2
            val tint = when {
                heartHp >= 2 -> Color(0xFFE53935)
                heartHp == 1 -> Color(0xFFFF8A65)
                else -> Color(0x66444444)
            }
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** Caja de mensaje inferior; un toque la cierra. */
@Composable
fun MessagePanel(text: String, speaker: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.9f)
                .background(Color(0xE6101426), RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            if (speaker.isNotBlank()) {
                Text(speaker, color = Color(0xFFFFC94D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(text, color = Color.White, fontSize = 17.sp, lineHeight = 24.sp)
            Text("▼", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}

/** Menú de elecciones centrado. */
@Composable
fun ChoicesPanel(choices: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 340.dp)
                .background(Color(0xE6101426), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            choices.forEachIndexed { index, choice ->
                Text(
                    choice,
                    color = Color.White,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF223055), RoundedCornerShape(8.dp))
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                )
            }
        }
    }
}
