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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapRegion
import com.rolebuilder.core.model.MapStamp
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.model.TilesetMerge
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.widgets.DropdownField
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
internal class TilesPreparadas(
    /** Tileset del nivel ya ampliado, listo para darlo de alta. null = no se pudo. */
    val tileset: Tileset?,
    /** Dónde ha quedado cada tesela pedida, en el mismo orden. */
    val added: List<Int>,
    /** Por qué no se pudo, para DECIRLO en vez de no hacer nada. */
    val error: String? = null,
)

/**
 * MITAD CARA (para `Dispatchers.IO`): lee los dos atlas, copia las teselas y reescribe el
 * PNG del nivel de destino. No toca el [EditorState], así que puede correr fuera del hilo
 * principal — y tiene que hacerlo: traerse un nivel entero son cientos de teselas y volver
 * a codificar un PNG grande, justo el tipo de trabajo que antes congelaba la pantalla.
 *
 * Que el atlas de destino se reescriba en su MISMO archivo es a propósito: otros niveles
 * pueden compartir ese tileset y siguen viendo lo mismo que antes, con teselas nuevas al
 * final.
 */
internal fun prepararTeselas(
    projectDir: File,
    dest: Tileset,
    src: Tileset,
    tiles: List<Int>,
): TilesPreparadas {
    if (tiles.isEmpty()) return TilesPreparadas(null, emptyList(), "No has elegido ninguna tesela")
    val srcBmp = loadTilesetBitmap(projectDir, src)
        ?: return TilesPreparadas(null, emptyList(), "No se encuentra el PNG de «${src.name}»")
    val destBmp = loadTilesetBitmap(projectDir, dest)
    // Un tileset sin PNG (o con el PNG perdido) se trata como un atlas vacío del tamaño
    // que dice su rejilla: así traer teselas ARREGLA el nivel en vez de fallar.
    val destPixels = destBmp?.argb()
        ?: IntArray(dest.columns * dest.tileSize * dest.rows * dest.tileSize)

    val merged = runCatching {
        TilesetMerge.append(dest, destPixels, src, srcBmp.argb(), tiles)
    }.getOrElse { return TilesPreparadas(null, emptyList(), "No se pudieron copiar: ${it.message}") }

    val out = Bitmap.createBitmap(merged.width, merged.height, Bitmap.Config.ARGB_8888)
    out.setPixels(merged.pixels, 0, merged.width, 0, 0, merged.width, merged.height)
    val file = ProjectIo.imageFile(projectDir, dest.image)
    file.parentFile?.mkdirs()
    runCatching { file.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        .getOrElse { return TilesPreparadas(null, emptyList(), "No se pudo guardar el atlas: ${it.message}") }

    return TilesPreparadas(merged.tileset, merged.added)
}

/** Un asset traído al nivel: el trozo ya traducido a SUS teselas, o el motivo del fallo. */
internal class AssetTraido(
    val clip: MapStamp?,
    val tileset: Tileset?,
    val error: String? = null,
)

/**
 * TRAERSE UN ASSET ENTERO (un sello, un trozo copiado) A ESTE NIVEL, con sus gráficos.
 *
 * Esta es la pieza que faltaba para que el material del proyecto sirva en cualquier mapa.
 * Un sello no guarda dibujos: guarda ÍNDICES al atlas del nivel donde se hizo. Pegarlo en
 * otro nivel pintaba teselas al azar —sin avisar— porque el índice 37 allí es otra cosa.
 *
 * Aquí se hace lo único que lo arregla de verdad: se copian al tileset del nivel de destino
 * las teselas que el asset usa ([TilesetMerge]) y se reescribe el asset con los índices
 * nuevos ([MapRegion.remapped]). A partir de ahí el trozo significa lo mismo aquí que allí.
 *
 * Es la mitad cara (lee dos atlas y reescribe un PNG): va en `Dispatchers.IO`. Quien llame
 * se queda con dar de alta el tileset en el editor, que es estado de Compose.
 */
internal fun traerAssetAlNivel(
    projectDir: File,
    clip: MapStamp,
    src: Tileset,
    dest: Tileset,
): AssetTraido {
    if (src.id == dest.id) return AssetTraido(clip, null) // mismo atlas: no hay nada que traer
    if (src.tileSize != dest.tileSize) {
        return AssetTraido(
            null, null,
            "Ese asset usa teselas de ${src.tileSize}px y este nivel las tiene de ${dest.tileSize}px",
        )
    }
    val usadas = MapRegion.usedTiles(clip)
    if (usadas.isEmpty()) return AssetTraido(clip.copy(tilesetId = dest.id), null)

    val r = prepararTeselas(projectDir, dest, src, usadas)
    val nuevo = r.tileset ?: return AssetTraido(null, null, r.error)
    // prepararTeselas devuelve dónde cayó cada tesela pedida, en el mismo orden.
    val mapping = usadas.zip(r.added).toMap()
    return AssetTraido(MapRegion.remapped(clip, mapping, nuevo.id), nuevo)
}

/**
 * Diálogo del banco de assets: elige nivel de origen, categoría y teselas —o el nivel
 * entero de un botón— y las trae al nivel actual.
 *
 * El diálogo **NO se cierra al traer**: se queda abierto para que puedas cambiar de nivel
 * de origen y traerte más, que es como se construye de verdad (el suelo de uno, las
 * tuberías de otro, el decorado de un tercero). Antes se cerraba en cada viaje y parecía
 * que solo se podía hacer una vez. [onBrought] avisa de cada tanda —el editor recarga el
 * atlas y deja la primera tesela como pincel— y [onClose] cierra.
 */
@Composable
internal fun AssetBankDialog(
    state: EditorState,
    map: GameMap,
    onBrought: (List<Int>) -> Unit,
    /** Un sello elegido de la biblioteca: el editor lo pone EN MANO para pegarlo. */
    onPickStamp: (MapStamp) -> Unit,
    onClose: () -> Unit,
) {
    // Dos familias de material: teselas sueltas y trozos enteros (sellos). Los sellos son la
    // parte que hasta ahora no cruzaba de nivel: se pegaban con los índices del nivel donde
    // se hicieron, pintando cualquier cosa.
    var tab by remember { mutableStateOf(0) }
    val dest = state.database.tileset(map.tilesetId)
    val sources = remember(state.database.tilesets, map.tilesetId) {
        assetSources(state, map.tilesetId, dest?.tileSize ?: 16)
    }
    // El origen se recuerda por ID, no por objeto: traer teselas cambia la base de datos y
    // con ella la lista de orígenes, así que guardar el objeto haría que el desplegable
    // saltara al primer nivel después de cada viaje.
    var sourceId by remember(map.tilesetId) { mutableStateOf<Int?>(null) }
    val source = sources.firstOrNull { it.tileset.id == sourceId } ?: sources.firstOrNull()
    val picked = remember(source?.tileset?.id) { mutableStateListOf<Int>() }
    val srcBitmap = remember(source?.tileset?.image, source?.tileset?.rows) {
        source?.let { loadTilesetBitmap(state.projectDir, it.tileset)?.asImageBitmap() }
    }
    // Trabajo en curso, cuántas se llevan traídas en esta sesión del diálogo y el último
    // fallo. Antes un fallo devolvía "cero teselas" en silencio: idéntico a que no pasara nada.
    var working by remember { mutableStateOf(false) }
    var brought by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    /** Trae [tiles] del origen actual: la parte cara en IO y el alta del tileset aquí. */
    fun traer(tiles: List<Int>) {
        val ts = source?.tileset ?: return
        val destino = state.database.tileset(map.tilesetId) ?: return
        if (tiles.isEmpty() || working) return
        working = true
        error = null
        scope.launch {
            val r = withContext(Dispatchers.IO) { prepararTeselas(state.projectDir, destino, ts, tiles) }
            working = false
            if (r.tileset != null) {
                state.updateTileset(r.tileset)   // estado de Compose: solo en el hilo principal
                brought += r.added.size
                picked.clear()
                onBrought(r.added)
            } else {
                error = r.error ?: "No se pudieron traer las teselas"
            }
        }
    }

    /** Trae un SELLO entero al nivel, con sus gráficos, y lo deja en mano para pegarlo. */
    fun traerSello(sello: MapStamp) {
        val destino = state.database.tileset(map.tilesetId) ?: return
        val origen = state.database.tileset(sello.tilesetId)
        if (working) return
        if (origen == null && sello.tilesetId != destino.id) {
            error = "El nivel de donde salió ese sello ya no está en el proyecto"
            return
        }
        working = true
        error = null
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                if (origen == null) AssetTraido(sello, null)
                else traerAssetAlNivel(state.projectDir, sello, origen, destino)
            }
            working = false
            r.tileset?.let {
                state.updateTileset(it)
                brought += MapRegion.usedTiles(sello).size
                onBrought(emptyList()) // el atlas cambió: que el editor lo relea
            }
            val listo = r.clip
            if (listo == null) {
                error = r.error ?: "No se pudo traer ese sello"
            } else {
                onPickStamp(listo)
                onClose()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onClose() },
        title = { Text("Biblioteca de assets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("Teselas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarioRed, selectedLabelColor = Color.Black,
                    ),
                )
                FilterChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("Sellos ${state.project.stamps.size}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarioRed, selectedLabelColor = Color.Black,
                    ),
                )
            }
            if (tab == 1) {
                StampLibrary(
                    stamps = state.project.stamps,
                    database = state.database,
                    projectDir = state.projectDir,
                    currentTilesetId = map.tilesetId,
                    enabled = !working && dest != null,
                    onPick = { traerSello(it) },
                )
                if (working) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Trayendo el sello y sus gráficos…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MarioRed) }
                return@Column
            }
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
                        onSelect = { sourceId = it.tileset.id },
                    )
                    val ts = source?.tileset
                    if (ts != null && srcBitmap != null) {
                        AssetTileGrid(
                            tileset = ts,
                            bitmap = srcBitmap,
                            picked = picked,
                            enabled = !working,
                            onToggle = { t -> if (t in picked) picked.remove(t) else picked.add(t) },
                            onPickMany = { list -> list.forEach { if (it !in picked) picked.add(it) } },
                            onClear = { picked.clear() },
                            onBringAll = { traer((0 until ts.tileCount).toList()) },
                        )
                        Text(
                            "Se copian al tileset de «${map.name}» con su colisión, su acción de " +
                                "bloque y su animación. Las teselas que ya tuviera no se mueven, y " +
                                "puedes cambiar de nivel de origen y seguir trayendo sin cerrar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    } else {
                        Text("No se encuentra el PNG de ese nivel.")
                    }
                    if (working) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Copiando teselas…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (brought > 0) {
                        Text(
                            "Traídas $brought tesela(s) a este nivel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LuigiGreen,
                        )
                    }
                    error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MarioRed)
                    }
                }
            }
            } // cierra la columna de las dos pestañas
        },
        confirmButton = {
            if (tab == 0) {
                Button(
                    enabled = dest != null && source != null && picked.isNotEmpty() && !working,
                    onClick = { traer(picked.toList()) },
                ) { Text(if (picked.isEmpty()) "Traer" else "Traer ${picked.size}") }
            }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onClose) {
                Text(if (brought > 0) "Listo" else "Cerrar")
            }
        },
    )
}

/**
 * BIBLIOTECA DE SELLOS: los trozos guardados del proyecto, vengan del nivel que vengan.
 *
 * Cada sello dice de qué nivel salió y si sus gráficos ya están aquí. Al elegirlo, si viene
 * de otro tileset, sus teselas se copian a este nivel y el sello se reescribe con los
 * índices nuevos: por eso se puede usar en un mapa nuevo, que es justo lo que antes no se
 * podía. Se dibuja una vista previa de verdad —con el atlas de SU nivel—, porque un sello
 * llamado "Sello 7" no le dice nada a nadie.
 */
@Composable
private fun StampLibrary(
    stamps: List<MapStamp>,
    database: com.rolebuilder.core.model.Database,
    projectDir: File,
    currentTilesetId: Int,
    enabled: Boolean,
    onPick: (MapStamp) -> Unit,
) {
    if (stamps.isEmpty()) {
        Text(
            "Todavía no hay sellos. Marca un trozo con la herramienta Área y pulsa «Guardar " +
                "sello», o usa la herramienta Sello: desde aquí podrás pegarlo en cualquier " +
                "otro nivel, aunque no comparta gráficos.",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    Column(
        Modifier.fillMaxWidth().height(260.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        stamps.forEach { s ->
            val origen = database.tileset(s.tilesetId)
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) { onPick(s) }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StampThumb(s, origen, projectDir)
                Column(Modifier.weight(1f)) {
                    Text(s.name, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${s.width}×${s.height} · de ${origen?.name ?: "un nivel que ya no está"}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    if (s.tilesetId == currentTilesetId) "Usar" else "Traer gráficos",
                    color = if (s.tilesetId == currentTilesetId) LuigiGreen else CoinYellow,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Vista previa de un sello, dibujada con el atlas del nivel del que salió. */
@Composable
private fun StampThumb(stamp: MapStamp, source: Tileset?, projectDir: File) {
    val bmp = remember(stamp.name, source?.image) {
        source?.let { loadTilesetBitmap(projectDir, it)?.asImageBitmap() }
    }
    Box(
        Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp == null || source == null) {
            Text("?", color = Color.White.copy(alpha = 0.5f))
            return@Box
        }
        Canvas(Modifier.size(64.dp)) {
            drawChecker()
            // El sello se encaja entero en el cuadro, conservando proporción.
            val cell = minOf(size.width / stamp.width, size.height / stamp.height)
            val ox = (size.width - cell * stamp.width) / 2f
            val oy = (size.height - cell * stamp.height) / 2f
            for (li in stamp.layers.indices) {
                val layer = stamp.layers[li]
                for (y in 0 until stamp.height) for (x in 0 until stamp.width) {
                    val t = layer[y * stamp.width + x]
                    if (t < 0) continue
                    drawImage(
                        image = bmp,
                        srcOffset = IntOffset(
                            (t % source.columns) * source.tileSize,
                            (t / source.columns) * source.tileSize,
                        ),
                        srcSize = IntSize(source.tileSize, source.tileSize),
                        dstOffset = IntOffset((ox + x * cell).toInt(), (oy + y * cell).toInt()),
                        dstSize = IntSize(cell.toInt() + 1, cell.toInt() + 1),
                    )
                }
            }
        }
    }
}

/**
 * Rejilla de teselas del nivel de origen, agrupada por categoría y con selección múltiple.
 * Además de tocarlas una a una, se puede marcar la categoría entera o **absorber el nivel
 * completo** de un botón: ir picando 150 teselas de decorado no es una forma de trabajar.
 */
@Composable
private fun AssetTileGrid(
    tileset: Tileset,
    bitmap: ImageBitmap,
    picked: List<Int>,
    enabled: Boolean,
    onToggle: (Int) -> Unit,
    onPickMany: (List<Int>) -> Unit,
    onClear: () -> Unit,
    onBringAll: () -> Unit,
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
        // Atajos de selección: la categoría entera, nada, o el nivel completo de un viaje.
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(enabled = enabled && tiles.isNotEmpty(), onClick = { onPickMany(tiles) }) {
                Text("Toda la categoría (${tiles.size})")
            }
            TextButton(enabled = enabled && picked.isNotEmpty(), onClick = onClear) { Text("Ninguna") }
            TextButton(enabled = enabled, onClick = onBringAll) {
                Text("Traer el nivel entero (${tileset.tileCount})", color = CoinYellow)
            }
        }
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
                            .clickable(enabled = enabled) { onToggle(tile) },
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
