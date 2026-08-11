package com.rolebuilder.editor.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.model.TilesetMerge
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.widgets.DropdownField
import java.io.File

/**
 * EL BANCO DE ASSETS: coger teselas de CUALQUIER nivel del proyecto para el nivel que
 * estás construyendo.
 *
 * El problema que resuelve: cada nivel importado de la ROM trae su propio tileset y un
 * nivel apunta a uno solo, así que la paleta de abajo solo enseñaba los gráficos del
 * nivel abierto. Hacer un nivel propio con el castillo de uno y las tuberías de otro no
 * era "difícil": no existía. Aquí se eligen teselas de otro nivel y se COPIAN al tileset
 * del actual (píxeles + colisión + acción de bloque + animación, ver [TilesetMerge]), que
 * es la única forma de mezclarlas sin cambiar el modelo de datos ni el motor.
 */

/** Un tileset del proyecto con el nombre de los niveles que lo usan (para el desplegable). */
internal class AssetSource(val tileset: Tileset, val levels: List<String>) {
    val label: String
        get() = if (levels.isEmpty()) tileset.name else "${tileset.name} · ${levels.joinToString(", ")}"
}

/** Tilesets del proyecto que se pueden usar como origen, con los niveles que los usan. */
internal fun assetSources(state: EditorState, exceptTilesetId: Int?, tileSize: Int): List<AssetSource> {
    val usedBy = HashMap<Int, MutableList<String>>()
    state.mapList.forEach { m -> usedBy.getOrPut(m.tilesetId) { mutableListOf() }.add(m.name) }
    return state.database.tilesets
        .filter { it.id != exceptTilesetId && it.tileSize == tileSize }
        .map { AssetSource(it, usedBy[it.id].orEmpty()) }
}

/** Carga el atlas de un tileset del proyecto como Bitmap, o null si falta el PNG. */
internal fun loadTilesetBitmap(projectDir: File, tileset: Tileset): Bitmap? {
    val file = ProjectIo.imageFile(projectDir, tileset.image)
    if (!file.exists()) return null
    return BitmapFactory.decodeFile(file.absolutePath)
}

/** Píxeles ARGB de un bitmap, fila a fila (lo que come [TilesetMerge]). */
private fun Bitmap.argb(): IntArray =
    IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

/**
 * Copia [tiles] del tileset [src] al tileset [dest] del proyecto: reescribe su PNG (crece
 * por filas, así que los índices que ya usaban los niveles NO se mueven) y actualiza la
 * base de datos. Devuelve los índices donde han quedado, o lista vacía si no se pudo.
 *
 * Que el atlas de destino se reescriba en su MISMO archivo es a propósito: otros niveles
 * pueden compartir ese tileset y siguen viendo lo mismo que antes, con teselas nuevas al
 * final.
 */
internal fun copyTilesIntoLevel(
    state: EditorState,
    dest: Tileset,
    src: Tileset,
    tiles: List<Int>,
): List<Int> {
    if (tiles.isEmpty()) return emptyList()
    val srcBmp = loadTilesetBitmap(state.projectDir, src) ?: return emptyList()
    val destBmp = loadTilesetBitmap(state.projectDir, dest)
    // Un tileset sin PNG (o con el PNG perdido) se trata como un atlas vacío del tamaño
    // que dice su rejilla: así traer teselas ARREGLA el nivel en vez de fallar.
    val destPixels = destBmp?.argb()
        ?: IntArray(dest.columns * dest.tileSize * dest.rows * dest.tileSize)

    val merged = runCatching {
        TilesetMerge.append(dest, destPixels, src, srcBmp.argb(), tiles)
    }.getOrNull() ?: return emptyList()

    val out = Bitmap.createBitmap(merged.width, merged.height, Bitmap.Config.ARGB_8888)
    out.setPixels(merged.pixels, 0, merged.width, 0, 0, merged.width, merged.height)
    val file = ProjectIo.imageFile(state.projectDir, dest.image)
    file.parentFile?.mkdirs()
    runCatching { file.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        .getOrElse { return emptyList() }

    state.updateTileset(merged.tileset)
    return merged.added
}

/**
 * Diálogo del banco de assets: elige nivel de origen, categoría y teselas (varias de una
 * vez), y las trae al nivel actual. [onDone] recibe los índices ya traídos —el editor
 * selecciona la primera como pincel— o lista vacía si se canceló.
 */
@Composable
internal fun AssetBankDialog(
    state: EditorState,
    map: GameMap,
    onDone: (List<Int>) -> Unit,
) {
    val dest = state.database.tileset(map.tilesetId)
    val sources = remember(state.database.tilesets, map.tilesetId) {
        assetSources(state, map.tilesetId, dest?.tileSize ?: 16)
    }
    var source by remember(sources) { mutableStateOf(sources.firstOrNull()) }
    val picked = remember(source) { mutableStateListOf<Int>() }
    val srcBitmap = remember(source?.tileset?.image) {
        source?.let { loadTilesetBitmap(state.projectDir, it.tileset)?.asImageBitmap() }
    }
    var working by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!working) onDone(emptyList()) },
        title = { Text("Assets de otros niveles") },
        text = {
            when {
                dest == null -> Text(
                    "Este nivel todavía no tiene tileset propio. Carga una ROM o asígnale uno " +
                        "en Ajustes del nivel y luego trae aquí las teselas que quieras.",
                )
                sources.isEmpty() -> Text(
                    "No hay otros niveles con gráficos compatibles (teselas de ${dest.tileSize}px) " +
                        "en este proyecto. Importa más niveles de la ROM y podrás mezclarlos.",
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField(
                        label = "Nivel de origen",
                        options = sources,
                        selected = source,
                        optionLabel = { it.label },
                        onSelect = { source = it },
                    )
                    val ts = source?.tileset
                    if (ts != null && srcBitmap != null) {
                        AssetTileGrid(
                            tileset = ts,
                            bitmap = srcBitmap,
                            picked = picked,
                            onToggle = { t -> if (t in picked) picked.remove(t) else picked.add(t) },
                        )
                        Text(
                            "Se copian al tileset de «${map.name}» con su colisión, su acción de " +
                                "bloque y su animación. Las teselas que ya tuviera no se mueven.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    } else {
                        Text("No se encuentra el PNG de ese nivel.")
                    }
                }
            }
        },
        confirmButton = {
            val ts = source?.tileset
            Button(
                enabled = dest != null && ts != null && picked.isNotEmpty() && !working,
                onClick = {
                    if (dest == null || ts == null) return@Button
                    working = true
                    onDone(copyTilesIntoLevel(state, dest, ts, picked.toList()))
                },
            ) { Text(if (picked.isEmpty()) "Traer" else "Traer ${picked.size}") }
        },
        dismissButton = { TextButton(onClick = { onDone(emptyList()) }) { Text("Cerrar") } },
    )
}

/** Rejilla de teselas del nivel de origen, agrupada por categoría y con selección múltiple. */
@Composable
private fun AssetTileGrid(
    tileset: Tileset,
    bitmap: ImageBitmap,
    picked: List<Int>,
    onToggle: (Int) -> Unit,
) {
    val byCat = remember(tileset) {
        val m = linkedMapOf<TileCat, MutableList<Int>>()
        for (t in 0 until tileset.tileCount) m.getOrPut(tileCategory(tileset, t)) { mutableListOf() }.add(t)
        m
    }
    val cats = remember(byCat) { TileCat.entries.filter { !byCat[it].isNullOrEmpty() } }
    var cat by remember(tileset) { mutableStateOf(cats.firstOrNull() ?: TileCat.SUELO) }

    Column {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            cats.forEach { c ->
                FilterChip(
                    selected = cat == c,
                    onClick = { cat = c },
                    label = { Text("${c.label} ${byCat[c]?.size ?: 0}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarioRed,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }
        }
        val tiles = byCat[cat] ?: emptyList()
        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp),
            modifier = Modifier.fillMaxWidth().height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(tiles) { tile ->
                val on = tile in picked
                Box(contentAlignment = Alignment.TopEnd) {
                    Canvas(
                        modifier = Modifier.size(48.dp)
                            .border(
                                width = if (on) 3.dp else 1.dp,
                                color = if (on) LuigiGreen else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .clickable { onToggle(tile) },
                    ) {
                        drawChecker()
                        val col = tile % tileset.columns
                        val row = tile / tileset.columns
                        drawImage(
                            image = bitmap,
                            srcOffset = IntOffset(col * tileset.tileSize, row * tileset.tileSize),
                            srcSize = IntSize(tileset.tileSize, tileset.tileSize),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        )
                    }
                    if (on) {
                        Text(
                            "✓",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(2.dp)
                                .background(LuigiGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
