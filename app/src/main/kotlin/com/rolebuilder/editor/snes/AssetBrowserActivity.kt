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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
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
import com.rolebuilder.core.snes.SmwGfxLibrary
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
    // 0 = catálogo de sprites (por animación); 1 = bancos GFX + paletas (palette-swap).
    var mode by remember { mutableIntStateOf(0) }

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
        Text("Biblioteca de assets de tu ROM", style = MaterialTheme.typography.titleLarge, color = Color.White)

        // Conmutador: catálogo de sprites (por animación) vs bancos GFX crudos con palette-swap.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeTab("Sprites", mode == 0) { mode = 0 }
            ModeTab("GFX y Paletas", mode == 1) { mode = 1 }
        }

        if (mode == 1) {
            GfxPaletteView(rom, header)
            return@Column
        }

        Text(
            "Clasificado y separado por animación. Cada animación se descarga en su carpeta.",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBBBBB),
            modifier = Modifier.padding(top = 6.dp),
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
                                val bytes = e.gif ?: e.bytes
                                    ?: e.image?.let { SnesImport.bitmapToPng(SnesImport.toBitmap(it)) }
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
            val detail = if (row.item.audio != null) "sonido (WAV)"
            else "${row.item.clips.size} animación(es)"
            Text(
                "${row.group.name} · $detail",
                color = Color(0xFF9AA0A6), style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onExport) { Text("⬇") }
    }
}

/** Pestaña/píldora del conmutador de modo. */
@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) Color(0xFF6C8EFF) else Color(0xFF23232B), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Navegador de BANCOS GFX crudos con **palette-swap** en vivo (estilo Lunar Magic / YY-CHR):
 * eliges una de las 16 filas de paleta de la CGRAM y una tesela-banco, y ves el banco recoloreado
 * al instante. Es la vía de "GFX + paletas" que complementa el catálogo de sprites. Reutiliza
 * [SmwGfxLibrary] (bancos + paletas + render).
 */
@Composable
private fun GfxPaletteView(rom: ByteArray, header: SnesHeader) {
    val context = LocalContext.current
    val banks = remember(rom) { SmwGfxLibrary.banks(rom) }
    val rows = remember(rom) { SmwGfxLibrary.paletteRows(rom, header) }
    var bankIdx by remember { mutableIntStateOf(0) }
    var palRow by remember { mutableIntStateOf(if (rows.size > 8) 8 else 0) }

    if (banks.isEmpty() || rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se pudieron leer bancos/paletas (¿ROM no SMW?).", color = Color.White)
        }
        return
    }

    val sheet: Bitmap? = remember(bankIdx, palRow) {
        val bank = banks.getOrNull(bankIdx) ?: return@remember null
        val row = rows.getOrNull(palRow) ?: return@remember null
        SmwGfxLibrary.bankSheet(rom, bank.id, row)?.let { SnesImport.toBitmap(it) }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            "Bancos de gráficos crudos, con la paleta que elijas (como Lunar Magic).",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBBBBB),
            modifier = Modifier.padding(vertical = 6.dp),
        )

        Text("Paleta · 16 filas de la CGRAM", color = Color(0xFF9AA0A6), style = MaterialTheme.typography.labelMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEachIndexed { i, colors -> PaletteSwatch(colors, selected = i == palRow) { palRow = i } }
        }

        sheet?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Banco GFX",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            // Exporta el banco tal y como se ve (con la paleta elegida) a Descargas.
            Button(
                onClick = {
                    val bankId = banks.getOrNull(bankIdx)?.id ?: 0
                    exportInBackground(context, onDone = {}) {
                        SnesImport.exportToDownloads(
                            context, "smw_gfx_%02x_p%d.png".format(bankId, palRow),
                            "image/png", SnesImport.bitmapToPng(bmp),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text("⬇ Exportar este banco (PNG, con esta paleta)") }
        }

        Text("Banco · ${banks.size} en la ROM", color = Color(0xFF9AA0A6), style = MaterialTheme.typography.labelMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            banks.forEachIndexed { i, b ->
                Box(
                    Modifier
                        .background(if (i == bankIdx) Color(0xFF6C8EFF) else Color(0xFF23232B), RoundedCornerShape(8.dp))
                        .clickable { bankIdx = i }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Text(
                        "0x%02X".format(b.id),
                        color = if (i == bankIdx) Color.Black else Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/** Muestra una fila de paleta como una tira de 16 colores; resalta la seleccionada. */
@Composable
private fun PaletteSwatch(colors: IntArray, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Color(0xFF6C8EFF) else Color(0x33FFFFFF),
                RoundedCornerShape(4.dp),
            )
            .clickable { onClick() }
            .padding(2.dp),
    ) {
        for (i in 0 until 16) {
            Box(Modifier.width(6.dp).height(20.dp).background(Color(colors.getOrElse(i) { 0xFF000000.toInt() })))
        }
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
