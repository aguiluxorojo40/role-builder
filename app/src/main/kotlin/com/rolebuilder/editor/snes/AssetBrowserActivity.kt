package com.rolebuilder.editor.snes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SmwAssetCatalog
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * HERRAMIENTA DE EXTRACCIÓN unificada: un catálogo navegable de todo lo que la app sabe sacar
 * de tu ROM ([SmwAssetCatalog]), **clasificado y separado por animación**. Buscas "Mario",
 * "Koopa", "Piraña"…, ves la animación en vivo, y descargas — todo en subcarpetas por
 * animación (`grupo/item/animación/` con sus PNG y un GIF), sin que nada de Nintendo viva en
 * el repositorio: las coordenadas están en el código, los datos salen de TU ROM.
 */
class AssetBrowserActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val romPath = intent.getStringExtra(EXTRA_ROM_PATH)
        val rom = runCatching { romPath?.let { File(it) }?.takeIf { it.isFile }?.readBytes() }.getOrNull()
        if (rom == null) {
            Toast.makeText(this, "No se pudo leer la ROM", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent { AssetBrowserScreen(rom, SnesDecoder.parseHeader(rom)) }
    }

    companion object {
        private const val EXTRA_ROM_PATH = "rom_path"

        fun intent(context: Context, romFile: File): Intent =
            Intent(context, AssetBrowserActivity::class.java)
                .putExtra(EXTRA_ROM_PATH, romFile.absolutePath)
    }
}

/** Una fila del catálogo: el sprite, con su grupo (para exportar solo esto) y su vista previa. */
private class Row(
    val group: SmwAssetCatalog.AssetGroup,
    val item: SmwAssetCatalog.AssetItem,
) {
    /** Todos los fotogramas de todas sus animaciones, en fila, para animar la miniatura. */
    val previewFrames: List<ArgbImage> = item.clips.flatMap { it.frames }
}

@Composable
private fun AssetBrowserScreen(rom: ByteArray, header: SnesHeader) {
    val context = LocalContext.current
    var groups by remember { mutableStateOf<List<SmwAssetCatalog.AssetGroup>?>(null) }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    // Construir el catálogo recorre la ROM: fuera del hilo de interfaz.
    LaunchedEffect(rom) {
        groups = withContext(Dispatchers.IO) { SmwAssetCatalog.build(rom, header) }
    }

    // Tic de animación compartido para todas las miniaturas.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(150); tick++ } }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF121216)).safeDrawingPadding().padding(12.dp),
    ) {
        Text("Extraer sprites de tu ROM", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(
            "Clasificado y separado por animación. Cada animación se descarga en su carpeta.",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBBBBB),
        )
        val all = groups
        if (all == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Leyendo la ROM…", color = Color.White)
            }
            return@Column
        }
        if (all.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Esta ROM no parece ser Super Mario World.", color = Color.White)
            }
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar (Mario, Koopa, Piraña…)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )

        // Exportar TODO el catálogo, clasificado, a un ZIP en Descargas.
        Button(
            onClick = {
                busy = true
                exportInBackground(context, onDone = { busy = false }) {
                    SnesImport.exportAssetCatalogZip(context, rom, header)
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Exportando…" else "⬇ Exportar TODO (ZIP clasificado por animación)") }

        val rows = remember(all, query) {
            all.flatMap { g -> g.items.map { Row(g, it) } }
                .filter { query.isBlank() || it.item.name.contains(query, ignoreCase = true) }
        }
        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            items(rows) { row ->
                AssetRow(row, tick) {
                    // Exporta SOLO este sprite (sus animaciones, en subcarpetas) a un ZIP.
                    val name = SmwAssetCatalog.slug(row.item.name)
                    val entries = SmwAssetCatalog.exportEntries(
                        listOf(SmwAssetCatalog.AssetGroup(row.group.name, listOf(row.item))),
                    )
                    exportInBackground(context, onDone = {}) {
                        val buffer = java.io.ByteArrayOutputStream()
                        java.util.zip.ZipOutputStream(buffer).use { zip ->
                            for (e in entries) {
                                zip.putNextEntry(java.util.zip.ZipEntry(e.path))
                                val bytes = e.gif ?: e.image?.let { SnesImport.bitmapToPng(SnesImport.toBitmap(it)) }
                                    ?: ByteArray(0)
                                zip.write(bytes); zip.closeEntry()
                            }
                        }
                        SnesImport.exportToDownloads(context, "smw_$name.zip", "application/zip", buffer.toByteArray())
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(row: Row, tick: Int, onExport: () -> Unit) {
    val frames = row.previewFrames
    val bmp: Bitmap? = remember(frames) {
        frames.map { SnesImport.toBitmap(it) }.takeIf { it.isNotEmpty() }?.let { it }
    }?.getOrNull(if (frames.isEmpty()) 0 else tick % frames.size)

    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = row.item.name,
                    filterQuality = FilterQuality.None,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(row.item.name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${row.group.name} · ${row.item.clips.size} animación(es)",
                color = Color(0xFF9AA0A6), style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onExport) { Text("⬇") }
    }
}

/** Lanza [block] (que devuelve dónde se guardó) fuera del hilo de UI, avisa con un Toast y
 *  llama a [onDone] en el hilo principal al terminar (para reactivar botones). */
private fun exportInBackground(context: Context, onDone: () -> Unit, block: () -> String?) {
    // Se ejecuta en un hilo aparte para no bloquear la interfaz al codificar/comprimir.
    Thread {
        val where = runCatching { block() }.getOrNull()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                if (where != null) "Guardado en $where" else "No se pudo exportar",
                Toast.LENGTH_LONG,
            ).show()
            onDone()
        }
    }.start()
}
