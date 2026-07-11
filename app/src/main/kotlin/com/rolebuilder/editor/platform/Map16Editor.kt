package com.rolebuilder.editor.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.loadImageBitmap

/**
 * Compone teselas 16×16 (bloques "Map16") a partir de cuatro teselas de 8×8 de
 * una hoja de gráficos del proyecto y las hornea en una imagen PNG de bloques,
 * como el editor de Map16 de Lunar Magic. Cada bloque lleva su comportamiento
 * ("acts like", [SmwSolidity]).
 */
object Map16Baker {

    const val BLOCKS_COLUMNS = 8
    private const val BLOCK_PX = 16
    private const val SRC_PX = 8

    /**
     * Añade un bloque al tileset de bloques [blocks] (o crea uno nuevo si es null),
     * componiendo las cuatro esquinas desde [src] (una hoja de 8×8), y guarda el PNG.
     * Devuelve el tileset de bloques actualizado.
     */
    fun addBlock(
        state: EditorState,
        blocks: Tileset?,
        src: Tileset,
        corners: IntArray, // [tl, tr, bl, br] índices de tesela en src
        solidity: SmwSolidity,
        newTilesetId: Int,
    ): Tileset {
        val projectDir = state.projectDir
        val srcBmp = BitmapFactory.decodeFile(ProjectIo.imageFile(projectDir, src.image).absolutePath)
            ?: error("No se pudo abrir la hoja de origen")

        val usedBefore = blocks?.platformSolidity?.size ?: 0
        val index = usedBefore
        val newCount = index + 1
        val rows = (newCount + BLOCKS_COLUMNS - 1) / BLOCKS_COLUMNS
        val imageName = blocks?.image ?: "blocks_$newTilesetId.png"

        val out = Bitmap.createBitmap(BLOCKS_COLUMNS * BLOCK_PX, rows * BLOCK_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        // Copia los bloques ya horneados.
        if (blocks != null) {
            BitmapFactory.decodeFile(ProjectIo.imageFile(projectDir, blocks.image).absolutePath)?.let { old ->
                canvas.drawBitmap(old, 0f, 0f, null)
            }
        }
        // Dibuja el bloque nuevo en su celda (índice = orden).
        val bx = (index % BLOCKS_COLUMNS) * BLOCK_PX
        val by = (index / BLOCKS_COLUMNS) * BLOCK_PX
        val offsets = arrayOf(intArrayOf(0, 0), intArrayOf(SRC_PX, 0), intArrayOf(0, SRC_PX), intArrayOf(SRC_PX, SRC_PX))
        for (i in 0..3) {
            val tile = corners[i]
            val sc = (tile % src.columns) * SRC_PX
            val sr = (tile / src.columns) * SRC_PX
            val srcRect = Rect(sc, sr, sc + SRC_PX, sr + SRC_PX)
            val dx = bx + offsets[i][0]
            val dy = by + offsets[i][1]
            val dstRect = Rect(dx, dy, dx + SRC_PX, dy + SRC_PX)
            canvas.drawBitmap(srcBmp, srcRect, dstRect, null)
        }

        // Guarda el PNG.
        val dest = ProjectIo.imageFile(projectDir, imageName)
        dest.parentFile?.mkdirs()
        dest.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val newSolidity = (blocks?.platformSolidity ?: emptyList()) + solidity.ordinal
        return blocks?.copy(rows = rows, platformSolidity = newSolidity)
            ?: Tileset(
                id = newTilesetId,
                name = "Bloques",
                image = imageName,
                tileSize = BLOCK_PX,
                columns = BLOCKS_COLUMNS,
                rows = rows,
                platformSolidity = newSolidity,
            )
    }
}

private val CORNER_LABELS = listOf("↖", "↗", "↙", "↘")

private fun map16SolidityLabel(s: SmwSolidity): String = when (s) {
    SmwSolidity.NONE -> "Aire"
    SmwSolidity.LEDGE_TOP -> "Plataforma"
    SmwSolidity.SOLID -> "Sólido"
    SmwSolidity.SLOPE -> "Cuesta"
    SmwSolidity.SLOPE_STEEP -> "Cuesta 2"
    SmwSolidity.SPIKE -> "Pinchos"
}

/**
 * Diálogo del editor de bloques Map16: elige una hoja de 8×8, compón las cuatro
 * esquinas de un bloque 16×16, asígnale su "acts like" y hornéalo al tileset de
 * bloques del proyecto (que puedes elegir luego en Ajustes del nivel).
 */
@Composable
fun Map16EditorDialog(state: EditorState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sources = state.database.tilesets.filter { it.tileSize == 8 }

    if (sources.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editor de bloques (Map16)") },
            text = {
                Text(
                    "Necesitas una hoja de gráficos de 8×8 en el proyecto. Usa \"Importar de ROM\" " +
                        "y extrae los gráficos (GFX) de tu ROM de SMW; se guardan como teselas de 8×8.",
                )
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } },
        )
        return
    }

    var srcId by remember { mutableIntStateOf(sources.first().id) }
    val src = sources.firstOrNull { it.id == srcId } ?: sources.first()
    val srcBitmap = remember(src.image) { loadImageBitmap(state.projectDir, src.image) }
    val corners = remember { mutableStateOf(intArrayOf(0, 1, 8, 9)) }
    var activeCorner by remember { mutableIntStateOf(0) }
    var solidity by remember { mutableStateOf(SmwSolidity.SOLID) }

    val blocks = remember(state.database.tilesets) { state.database.tilesets.firstOrNull { it.name == "Bloques" } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editor de bloques (Map16)") },
        text = {
            Column(
                modifier = Modifier.height(420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (sources.size > 1) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        sources.forEach { s ->
                            FilterChip(selected = s.id == srcId, onClick = { srcId = s.id }, label = { Text(s.name) })
                        }
                    }
                }

                // Vista previa del bloque + selector de esquina activa.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BlockPreview(srcBitmap, src, corners.value)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Esquina a colocar:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CORNER_LABELS.forEachIndexed { i, lbl ->
                                FilterChip(selected = activeCorner == i, onClick = { activeCorner = i }, label = { Text(lbl) })
                            }
                        }
                    }
                }

                Text("Toca una tesela de 8×8 para la esquina ${CORNER_LABELS[activeCorner]}:", style = MaterialTheme.typography.bodySmall)
                if (srcBitmap != null) {
                    SrcTileGrid(src, srcBitmap, corners.value[activeCorner]) { tile ->
                        corners.value = corners.value.copyOf().also { it[activeCorner] = tile }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Colisión:", style = MaterialTheme.typography.labelMedium)
                    SmwSolidity.values().forEach { s ->
                        FilterChip(selected = solidity == s, onClick = { solidity = s }, label = { Text(map16SolidityLabel(s)) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                runCatching {
                    val newId = blocks?.id ?: state.nextTilesetId()
                    val updated = Map16Baker.addBlock(state, blocks, src, corners.value, solidity, newId)
                    if (blocks == null) state.addTileset(updated) else state.updateTileset(updated)
                    state.save()
                }.onFailure {
                    android.widget.Toast.makeText(context, "Error: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
                }.onSuccess {
                    android.widget.Toast.makeText(context, "Bloque añadido al tileset \"Bloques\"", android.widget.Toast.LENGTH_SHORT).show()
                }
            }) { Text("Añadir bloque") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun BlockPreview(bitmap: ImageBitmap?, src: Tileset, corners: IntArray) {
    ComposeCanvas(modifier = Modifier.size(64.dp).border(2.dp, Color(0xFF3CB043), RoundedCornerShape(4.dp))) {
        if (bitmap == null) return@ComposeCanvas
        val half = size.width / 2f
        val pos = arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1))
        for (i in 0..3) {
            val tile = corners[i]
            val sc = (tile % src.columns) * 8
            val sr = (tile / src.columns) * 8
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(sc, sr),
                srcSize = IntSize(8, 8),
                dstOffset = IntOffset((pos[i][0] * half).toInt(), (pos[i][1] * half).toInt()),
                dstSize = IntSize(half.toInt(), half.toInt()),
            )
        }
    }
}

@Composable
private fun SrcTileGrid(src: Tileset, bitmap: ImageBitmap, selected: Int, onSelect: (Int) -> Unit) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF10261A)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items((0 until src.tileCount).toList()) { tile ->
            val col = tile % src.columns
            val row = tile / src.columns
            ComposeCanvas(
                modifier = Modifier.size(32.dp)
                    .border(if (selected == tile) 2.dp else 1.dp, if (selected == tile) Color(0xFF3CB043) else Color(0x33FFFFFF))
                    .clickable { onSelect(tile) },
            ) {
                drawImage(
                    image = bitmap,
                    srcOffset = IntOffset(col * 8, row * 8),
                    srcSize = IntSize(8, 8),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                )
            }
        }
    }
}
