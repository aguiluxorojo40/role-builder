package com.rolebuilder.editor.snes

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapWarp
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.PlatformItemMark
import com.rolebuilder.core.model.PlatformItemType
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.SmwGfxLibrary
import com.rolebuilder.core.snes.SmwLevelBundle
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.SnesHeader
import com.rolebuilder.core.snes.SnesAssetExtractor
import com.rolebuilder.core.snes.SnesAutoExtractor
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesGraphicsScanner
import com.rolebuilder.core.snes.SnesPalette
import com.rolebuilder.core.snes.SnesPaletteMatcher
import com.rolebuilder.core.snes.compression.CompressionCodecs
import com.rolebuilder.core.snes.compression.LcLz2
import com.rolebuilder.editor.EditorState
import com.rolebuilder.player.PlatformerMusic
import com.rolebuilder.editor.widgets.DropdownField
import com.rolebuilder.editor.widgets.IntField
import kotlin.math.floor

/**
 * Opción de paleta en el desplegable. Índices especiales negativos:
 *   -1 = colores vivos por defecto, -2 = escala de grises (ver formas),
 *   -3 = automática (el emparejador elige la que mejor encaja al gráfico actual).
 * Índices >= 0 apuntan a una paleta CGRAM detectada en la ROM.
 */
private data class PaletteOption(val index: Int, val label: String)

private const val PALETTE_DEFAULT = -1
private const val PALETTE_GRAYSCALE = -2
private const val PALETTE_AUTO = -3

private val FORMAT_LABELS = mapOf(
    SnesGraphicFormat.SNES_2BPP to "SNES 2bpp (4 colores)",
    SnesGraphicFormat.SNES_3BPP to "SNES 3bpp (8 colores · fondos SMW)",
    SnesGraphicFormat.SNES_4BPP to "SNES 4bpp (16 colores)",
    SnesGraphicFormat.SNES_8BPP to "SNES 8bpp (256 colores)",
    SnesGraphicFormat.GB_2BPP to "Game Boy 2bpp",
    SnesGraphicFormat.NES_2BPP to "NES 2bpp",
)

// Colores vivos de respaldo (índice 0 transparente) para cuando no hay una paleta
// de la ROM: así los gráficos SIEMPRE salen en color, no en gris.
private val VIVID_16 = intArrayOf(
    0x00000000, 0xFFE53935.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(),
    0xFFFDD835.toInt(), 0xFF8E24AA.toInt(), 0xFF00ACC1.toInt(), 0xFFF5F5F5.toInt(),
    0xFF6D4C41.toInt(), 0xFFFF7043.toInt(), 0xFF9CCC65.toInt(), 0xFF5C6BC0.toInt(),
    0xFFFFB300.toInt(), 0xFFEC407A.toInt(), 0xFF26A69A.toInt(), 0xFF212121.toInt(),
)

private fun defaultColorPalette(colorCount: Int): IntArray =
    IntArray(colorCount) { i -> if (i < VIVID_16.size) VIVID_16[i] else 0xFF000000.toInt() }

/** Resultado de la vista previa: imagen, rejilla y, en modo automático, la paleta elegida. */
private data class PreviewData(
    val bitmap: ImageBitmap,
    val sheet: SnesAssetExtractor.TileSheet,
    val autoPalette: String?,
)

private fun parseOffset(text: String): Int {
    val s = text.trim()
    val value = if (s.startsWith("0x", true)) s.substring(2).toIntOrNull(16) else s.toIntOrNull()
    return (value ?: 0).coerceAtLeast(0)
}

/**
 * Diálogo para importar una hoja de tiles desde una ROM de Super Nintendo. Deja
 * elegir el archivo, ajustar offset/formato/paleta/rejilla con vista previa en
 * vivo y guardar el resultado como un tileset PNG + entrada en la base de datos.
 */
@Composable
fun SnesImportDialog(state: EditorState, onDismiss: () -> Unit) {
    val context = LocalContext.current

    var romBytes by remember { mutableStateOf<ByteArray?>(null) }
    var romName by remember { mutableStateOf("") }
    // Reproductor de la PRUEBA de música (directo de la ROM). Se para al salir del diálogo.
    var musicTest by remember { mutableStateOf<PlatformerMusic?>(null) }
    var format by remember { mutableStateOf(SnesGraphicFormat.SNES_4BPP) }
    var offsetText by remember { mutableStateOf("0x0") }
    var columns by remember { mutableStateOf(16) }
    var tiles by remember { mutableStateOf(64) }
    var paletteIndex by remember { mutableStateOf(-1) }
    var name by remember { mutableStateOf("snes_rip") }
    // 0 = sin descompresión (datos crudos), 1 = autodetectar códec, 2 = LC_LZ2.
    var decompressMode by remember { mutableStateOf(0) }
    // Lado del sprite en tiles de 8×8: 1 = sin agrupar, 2 = 16×16, 3 = 24×24, 4 = 32×32.
    var spriteTiles by remember { mutableStateOf(1) }
    // Modo fácil: si la ROM es un juego con receta, sus gráficos ya extraídos.
    var recipeFindings by remember(romBytes) { mutableStateOf<List<SnesAutoExtractor.Finding>>(emptyList()) }
    // Modo fácil (mapas): TODOS los niveles del juego importables (ficha ligera;
    // el mapa se construye al pulsar "Mapa", no antes).
    var levelListings by remember(romBytes) { mutableStateOf<List<SnesGameRecipes.SmwLevelListing>>(emptyList()) }
    // Overworld (mapa del mundo): previsualización renderizada desde la ROM.
    var overworldPreview by remember(romBytes) { mutableStateOf<ImageBitmap?>(null) }
    // Pantalla de título reconstruida desde la ROM.
    var titlePreview by remember(romBytes) { mutableStateOf<ImageBitmap?>(null) }

    val header = remember(romBytes) { romBytes?.let { SnesDecoder.parseHeader(it) } }
    // Juego reconocido con receta (modo fácil), o null.
    val gameRecipe = remember(header) { header?.let { SnesGameRecipes.detect(it) } }
    val detected: List<SnesPalette> = remember(romBytes) {
        romBytes?.let { SnesDecoder.scanRomForPalettes(it) } ?: emptyList()
    }
    val paletteOptions = remember(detected) {
        listOf(
            PaletteOption(PALETTE_AUTO, "Automática (la que mejor encaja)"),
            PaletteOption(PALETTE_DEFAULT, "Colores por defecto"),
            PaletteOption(PALETTE_GRAYSCALE, "Escala de grises (ver formas)"),
        ) + detected.mapIndexed { i, p -> PaletteOption(i, p.name) }
    }
    // Con paletas detectadas, arranca en modo automático: el emparejador elige
    // para CADA gráfico la paleta detectada que mejor le encaja (sin tocar nada).
    LaunchedEffect(detected) {
        paletteIndex = if (detected.isNotEmpty()) PALETTE_AUTO else PALETTE_DEFAULT
    }

    // Buscar hojas de sprites/personajes en vez de fondos (no penaliza la transparencia).
    var spriteSearch by remember { mutableStateOf(false) }
    // Autodetección de zonas con gráficos sin comprimir para el offset elegido.
    val candidates: List<Int> = remember(romBytes, format, spriteSearch) {
        romBytes?.let { rom ->
            if (spriteSearch) SnesGraphicsScanner.findSpriteCandidates(rom, format).map { it.offset }
            else SnesGraphicsScanner.findCandidates(rom, format).map { it.offset }
        } ?: emptyList()
    }
    var candidateIndex by remember { mutableStateOf(0) }
    fun jumpToCandidate(i: Int) {
        if (candidates.isEmpty()) return
        val idx = ((i % candidates.size) + candidates.size) % candidates.size
        candidateIndex = idx
        offsetText = "0x" + candidates[idx].toString(16).uppercase()
    }

    val romLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val bytes = SnesImport.readRomBytes(context, uri)
                ?: error("No se pudo leer el archivo")
            // HORNEA los assets de SMW (Mario, enemigos, powerups, moneda, jefes y efectos de
            // sonido) desde la ROM del usuario al almacén del dispositivo. En el repo no va
            // ninguno: son de Nintendo. Con esto el modo PROYECTO (sin ROM) sigue teniéndolos.
            runCatching { SmwAssetStore.bake(context, bytes) }.onSuccess { n ->
                if (n > 0) {
                    val music = if (SmwAssetStore.isMusicBaked(context)) "música ✓" else "música ✗"
                    Toast.makeText(
                        context, "Assets de SMW generados desde tu ROM ($n) · $music", Toast.LENGTH_LONG,
                    ).show()
                }
            }
            romBytes = bytes
            romName = (context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            } ?: "rom.sfc")
            paletteIndex = -1
        }.onFailure {
            Toast.makeText(context, "No se pudo abrir la ROM: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Vista previa reactiva: recalcula la hoja de tiles al cambiar cualquier parámetro.
    val preview: PreviewData? =
        remember(romBytes, format, offsetText, columns, tiles, paletteIndex, decompressMode, spriteTiles) {
            val rom = romBytes ?: return@remember null
            runCatching {
                val offset = parseOffset(offsetText)
                // Descompresión opcional: los tiles salen del bloque descomprimido,
                // pero la paleta se sigue leyendo de la ROM original.
                val tileRom: ByteArray
                val tileOffset: Int
                when (decompressMode) {
                    1 -> {
                        val auto = CompressionCodecs.autoDecompress(rom, offset, format)
                            ?: return@runCatching null
                        tileRom = auto.result.data; tileOffset = 0
                    }
                    2 -> {
                        val res = runCatching { LcLz2.decompress(rom, offset) }.getOrNull()
                            ?: return@runCatching null
                        tileRom = res.data; tileOffset = 0
                    }
                    else -> { tileRom = rom; tileOffset = offset }
                }
                val available = SnesAssetExtractor.availableTiles(tileRom.size, tileOffset, format)
                if (available <= 0) return@runCatching null
                val count = tiles.coerceIn(1, minOf(available, 1024))
                var autoPalette: String? = null
                val palette = when {
                    paletteIndex == PALETTE_GRAYSCALE -> SnesDecoder.grayscalePalette(format.colorCount)
                    paletteIndex == PALETTE_AUTO -> {
                        // El emparejador puntúa cada paleta detectada CONTRA estos
                        // tiles y se queda con la que mejor les sienta (y su sub-paleta).
                        val match = SnesPaletteMatcher
                            .rankPalettes(tileRom, tileOffset, format, count, detected)
                            .firstOrNull()
                        if (match != null) {
                            autoPalette = match.source.name + if (match.window > 0) {
                                " · colores ${match.window}-${match.window + format.colorCount - 1}"
                            } else ""
                            match.colors
                        } else defaultColorPalette(format.colorCount)
                    }
                    paletteIndex >= 0 -> detected.getOrNull(paletteIndex)?.colors
                        ?: defaultColorPalette(format.colorCount)
                    else -> defaultColorPalette(format.colorCount)
                }
                val sheet = if (spriteTiles > 1) {
                    // Agrupa bloques de spriteTiles×spriteTiles tiles en sprites enteros.
                    val availSprites = SnesAssetExtractor.availableSprites(
                        tileRom.size, tileOffset, format, spriteTiles, spriteTiles,
                    )
                    if (availSprites <= 0) return@runCatching null
                    val spriteCount = (count / (spriteTiles * spriteTiles))
                        .coerceIn(1, minOf(availSprites, 1024))
                    SnesAssetExtractor.extractSpriteAtlas(
                        tileRom, tileOffset, format, palette, spriteCount,
                        spriteTiles, spriteTiles, columns.coerceAtLeast(1),
                    )
                } else {
                    SnesAssetExtractor.extractTileSheet(
                        tileRom, tileOffset, format, palette, count, columns.coerceAtLeast(1),
                    )
                }
                PreviewData(SnesImport.toBitmap(sheet.image).asImageBitmap(), sheet, autoPalette)
            }.getOrNull()
        }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Importar assets desde ROM de SNES", style = MaterialTheme.typography.titleMedium)

            Button(onClick = { romLauncher.launch("*/*") }) { Text("Elegir ROM (.smc/.sfc)") }

            if (header != null) {
                Text(romName, style = MaterialTheme.typography.labelLarge)
                Text(
                    "\"${header.title}\" · ${header.mapping} · ${header.romTypeDescription}\n" +
                        "${header.country} · ${header.licensee} · " +
                        "${header.romSizeBytes / 1024} KiB · checksum " +
                        if (header.isChecksumValid) "válido" else "no válido",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "Carga una ROM para empezar. Todo se decodifica en el dispositivo; " +
                        "la ROM no se copia al proyecto, solo los tiles que extraigas.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (romBytes != null) {
                if (gameRecipe != null) {
                    HorizontalDivider()
                    Text("✨ Modo fácil", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Reconocido: $gameRecipe. Extrae sus gráficos de un toque " +
                            "(sin tocar nada técnico) y luego toca las imágenes que quieras guardar.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = {
                        val rom = romBytes ?: return@Button
                        val hdr = header ?: return@Button
                        recipeFindings = runCatching { SnesGameRecipes.extract(rom, hdr) }.getOrDefault(emptyList())
                        if (recipeFindings.isEmpty()) {
                            Toast.makeText(context, "No se pudo leer el mapa gráfico de este juego.", Toast.LENGTH_LONG).show()
                        }
                    }) { Text("Extraer gráficos de $gameRecipe") }

                    if (gameRecipe.contains("Mario World", ignoreCase = true)) {
                        Button(onClick = {
                            val rom = romBytes ?: return@Button
                            runCatching {
                                val tmp = java.io.File(context.cacheDir, "smw_play.sfc").apply { writeBytes(rom) }
                                context.startActivity(
                                    com.rolebuilder.player.PlatformerActivity.intent(context, tmp, 0x106),
                                )
                            }.onFailure {
                                Toast.makeText(context, "No se pudo iniciar el nivel: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }) { Text("▶ Jugar un nivel (plataformas)") }

                        // PRUEBA DIRECTA DE MÚSICA: toca la banda sonora del nivel ENSAMBLADA en
                        // vivo de la ROM (PlatformerMusic.fromRom), sin pasar por el almacén ni el
                        // modo proyecto. Aísla el fallo: si aquí se OYE, el motor de audio va bien
                        // y el problema era el horneado; si no ensambla, avisa (la ROM no trae ese
                        // banco); si ensambla pero no suena, es el audio del móvil.
                        DisposableEffect(Unit) { onDispose { musicTest?.stop() } }
                        Button(onClick = {
                            val cur = musicTest
                            if (cur != null) { cur.stop(); musicTest = null; return@Button }
                            val rom = romBytes ?: return@Button
                            val hdr = header
                            val setting = (hdr?.let {
                                runCatching { SnesGameRecipes.smwLevelInfo(rom, it, 0x105)?.musicIndex }.getOrNull()
                            } ?: 2)
                            val song = com.rolebuilder.core.snes.SmwMusic.levelSongId(setting)
                            val m = runCatching { com.rolebuilder.player.PlatformerMusic.fromRom(rom, song) }.getOrNull()
                            if (m == null) {
                                Toast.makeText(
                                    context,
                                    "🔇 Esta ROM no ensambla la música (✗): no trae el banco esperado o no es SMW estándar.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                m.start(); musicTest = m
                                Toast.makeText(
                                    context, "🔊 Sonando la canción $song de la ROM. ¿La oyes? (sube el volumen MULTIMEDIA). Toca otra vez para parar.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }) { Text(if (musicTest != null) "⏹ Parar prueba de música" else "🔊 Probar música (directo de la ROM)") }

                        // ESTADO de la música del MODO PROYECTO: el editor la lee del fichero
                        // horneado (music/level.aram); las rutas ▶/título la ensamblan de la ROM y
                        // no lo necesitan (por eso "suena en lo demás pero no en el editor" = falta
                        // este horneado). El botón la hornea al momento desde la ROM ya cargada.
                        // ESTADO del almacén horneado, que es de donde el EDITOR saca los sprites
                        // y la música al jugar un mapa (modo proyecto, sin ROM). Si algo sale ✗,
                        // eso es justo lo que se ve como CUADRO ROJO en vez del enemigo. Se puede
                        // rehornear a la fuerza desde aquí, con la ROM ya cargada.
                        var bakeStatus by remember(romBytes) {
                            mutableStateOf(SmwAssetStore.status(context))
                        }
                        // Motivo de cada fallo del último horneado (antes se perdía en silencio).
                        var bakeDetail by remember(romBytes) { mutableStateOf(emptyList<String>()) }
                        Text(
                            "Almacén del editor: " +
                                bakeStatus.joinToString(" · ") { (n, ok) -> "$n ${if (ok) "✓" else "✗"}" },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (bakeStatus.any { !it.second }) {
                            Text(
                                "Lo que salga ✗ es lo que el editor NO puede dibujar (de ahí los " +
                                    "cuadros rojos en vez de enemigos). Pulsa para rehornearlo de tu ROM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0A030),
                            )
                        }
                        // Offsets marcados [PROBABLE] en el core (tablas Map16 de terreno y de
                        // fondo): son deducciones, no datos verificados. Si apuntan mal, el fondo
                        // sale incorrecto SIN avisar. Aquí se comprueban contra TU ROM y se dice.
                        val map16 = remember(romBytes) {
                            romBytes?.let { SnesGameRecipes.checkProbableOffsets(it) }.orEmpty()
                        }
                        if (map16.isNotEmpty()) {
                            Text(
                                "Tablas Map16 (offsets no verificados): " +
                                    map16.joinToString(" · ") { "${it.name} ${if (it.ok) "✓" else "✗"}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (map16.all { it.ok }) Color(0xFF9AA0A6) else Color(0xFFE0A030),
                            )
                            map16.filter { !it.ok }.forEach {
                                Text(
                                    "⚠ ${it.name}: ${it.reason} → el fondo/terreno de esta ROM puede salir mal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE0A030),
                                )
                            }
                        }

                        Button(onClick = {
                            val rom = romBytes ?: return@Button
                            // Parte DETALLADO: dice qué paso falló y por qué, en vez del 0 mudo.
                            val report = SmwAssetStore.bakeDetailed(context, rom)
                            bakeStatus = SmwAssetStore.status(context)
                            bakeDetail = report.failures.map { "${it.name}: ${it.detail}" }
                            Toast.makeText(context, report.summary(), Toast.LENGTH_LONG).show()
                        }) { Text("🔄 Rehornear assets del editor") }
                        // El MOTIVO de cada fallo, en pantalla: antes esto se lo tragaba un
                        // runCatching y el usuario solo veía cuadros rojos sin explicación.
                        bakeDetail.forEach {
                            Text(
                                "⚠ $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0A030),
                            )
                        }
                    }

                    if (recipeFindings.isNotEmpty()) {
                        Text(
                            "${recipeFindings.size} conjuntos de gráficos. Toca uno para importarlo como tileset:",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        recipeFindings.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                rowItems.forEach { f ->
                                    val idx = recipeFindings.indexOf(f)
                                    Image(
                                        bitmap = SnesImport.toBitmap(f.image).asImageBitmap(),
                                        contentDescription = f.label,
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .background(Color(0xFF202024), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFF555555), RoundedCornerShape(4.dp))
                                            .clickable {
                                                runCatching {
                                                    val fileName = SnesImport.sanitizeFileName("${gameRecipe}_${idx + 1}")
                                                    SnesImport.saveTilesetPng(state.projectDir, fileName, SnesImport.toBitmap(f.image))
                                                    val sheet = SnesAssetExtractor.TileSheet(
                                                        f.image, f.columns, f.image.height / 8, 8,
                                                    )
                                                    state.addTileset(
                                                        SnesAssetExtractor.toTileset(
                                                            sheet, state.nextTilesetId(),
                                                            "$gameRecipe ${idx + 1}", fileName,
                                                        )
                                                    )
                                                    Toast.makeText(context, "Importado: $fileName", Toast.LENGTH_SHORT).show()
                                                }.onFailure {
                                                    Toast.makeText(context, "No se pudo guardar: ${it.message}", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                    )
                                }
                                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("🗺️ Niveles como MAPAS jugables", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Importa un nivel completo como MAPA (tileset de bloques Map16 + tilemap + " +
                            "colisión), no una imagen. Aparece en tu lista de mapas para editarlo y jugarlo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = {
                        val rom = romBytes; val hdr = header
                        if (rom != null && hdr != null) {
                            // Antes un FALLO del código y una ROM sin niveles daban el mismo
                            // mensaje ("no hay niveles"), o sea que se culpaba a la ROM de
                            // nuestros errores. Ahora se distingue y se dice el motivo real.
                            runCatching { SnesGameRecipes.listImportableSmwLevels(rom, hdr) }
                                .onSuccess {
                                    levelListings = it
                                    if (it.isEmpty()) Toast.makeText(
                                        context, "Esta ROM no tiene niveles reconstruibles.", Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                .onFailure { e ->
                                    levelListings = emptyList()
                                    Toast.makeText(
                                        context,
                                        "Fallo al listar niveles (no es culpa de la ROM): ${e::class.simpleName}: ${e.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                        }
                    }) { Text("Buscar niveles importables") }
                    if (levelListings.isNotEmpty()) {
                        Text(
                            "${levelListings.size} niveles del juego reconstruibles. \"Mapa\" lo importa " +
                                "COMPLETO (con sus sub-niveles y warps); \"Capas\" exporta Layer 1 y Layer 2 " +
                                "por SEPARADO (PNG); ▶ lo juega directo de la ROM.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    levelListings.forEach { listing ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${listing.name} · ${listing.screens} pant.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                val rom = romBytes; val hdr = header
                                runCatching {
                                    if (rom == null || hdr == null) error("carga primero la ROM")
                                    val realName = SnesGameRecipes.smwLevelName(rom, hdr, listing.level)
                                        ?: "Nivel ${listing.level.toString(16).uppercase()}"
                                    val msg = importSmwLevelBundle(state, rom, hdr, listing.level, realName)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }) { Text("Mapa") }
                            // Diferencia Layer 1 / Layer 2 del nivel y los SACA como PNG separados
                            // (más la escena combinada) a Descargas: el extractor a la altura de la app.
                            TextButton(onClick = {
                                val rom = romBytes; val hdr = header
                                runCatching {
                                    if (rom == null || hdr == null) error("carga primero la ROM")
                                    val layers = SnesImport.levelLayers(rom, hdr, listing.level)
                                        ?: error("nivel no reconstruible")
                                    val hx = listing.level.toString(16).uppercase()
                                    var saved = 0
                                    fun put(suffix: String, bmp: Bitmap) {
                                        if (SnesImport.exportToDownloads(
                                                context, "smw_${hx}_$suffix.png", "image/png",
                                                SnesImport.bitmapToPng(bmp),
                                            ) != null
                                        ) saved++
                                    }
                                    put("layer1", layers.layer1)
                                    layers.layer2?.let { put("layer2", it) }
                                    put("scene", layers.scene)
                                    val bg = if (layers.layer2 != null) "" else " (este nivel no tiene fondo)"
                                    Toast.makeText(
                                        context, "Capas exportadas: $saved PNG en Descargas$bg",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }) { Text("Capas") }
                            TextButton(onClick = {
                                val rom = romBytes ?: return@TextButton
                                runCatching {
                                    val tmp = java.io.File(context.cacheDir, "smw_play.sfc").apply { writeBytes(rom) }
                                    context.startActivity(
                                        com.rolebuilder.player.PlatformerActivity.intent(context, tmp, listing.level),
                                    )
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo iniciar: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }) { Text("▶") }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("🎞️ Extraer sprites (catálogo por animación)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Abre el catálogo navegable de TODO lo que se puede sacar de tu ROM: busca " +
                            "\"Mario\", \"Koopa\", \"Piraña\"…, ve la animación en vivo y descárgala. Se " +
                            "guarda clasificado y separado por animación (cada una en su subcarpeta).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = {
                        val rom = romBytes ?: return@Button
                        runCatching {
                            val tmp = java.io.File(context.cacheDir, "smw_extract.sfc").apply { writeBytes(rom) }
                            context.startActivity(AssetBrowserActivity.intent(context, tmp))
                        }.onFailure {
                            Toast.makeText(context, "No se pudo abrir el catálogo: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }) { Text("🎞️ Abrir catálogo de extracción") }

                    if (gameRecipe.contains("Mario World", ignoreCase = true) && header != null) {
                        Spacer(Modifier.height(8.dp))
                        GfxBankToTilesetSection(state, romBytes!!, header)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("🌍 Overworld (mapa del mundo)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Renderiza el MAPA DEL MUNDO de SMW desde tu ROM (tierra + niveles, castillos, " +
                            "casa de Yoshi, con GFX y paleta reales) y expórtalo: PNG estático o GIF animado " +
                            "(destello real del juego). Se guarda en tu carpeta Descargas.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = {
                        val rom = romBytes; val hdr = header
                        if (rom != null && hdr != null) {
                            runCatching { SnesImport.overworldMap(rom, hdr)?.asImageBitmap() }
                                .onSuccess {
                                    overworldPreview = it
                                    if (it == null) Toast.makeText(
                                        context, "Esta ROM no parece ser Super Mario World.", Toast.LENGTH_LONG,
                                    ).show()
                                }
                                .onFailure { e ->
                                    overworldPreview = null
                                    Toast.makeText(
                                        context, "Fallo al renderizar el overworld: ${e::class.simpleName}: ${e.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                        }
                    }) { Text("Renderizar overworld") }
                    Button(onClick = {
                        val rom = romBytes ?: return@Button
                        runCatching {
                            val tmp = java.io.File(context.cacheDir, "smw_play.sfc").apply { writeBytes(rom) }
                            context.startActivity(com.rolebuilder.player.OverworldActivity.intent(context, tmp))
                        }.onFailure {
                            Toast.makeText(context, "No se pudo abrir el mapa: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }) { Text("🗺️ Probar el mapa (modo prueba: no guarda partida)") }
                    Text(
                        "Esto es el modo PRUEBA del editor: lo abre todo y no toca ninguna " +
                            "partida guardada. Para jugar de verdad —título, progresión y " +
                            "guardado— usa “Jugar Super Mario World” en la pantalla de inicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9AA0A6),
                    )
                    overworldPreview?.let { ow ->
                        Image(
                            bitmap = ow,
                            contentDescription = "Overworld",
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF202024), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF555555), RoundedCornerShape(4.dp)),
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = {
                                val rom = romBytes; val hdr = header
                                runCatching {
                                    if (rom == null || hdr == null) error("carga la ROM")
                                    val bmp = SnesImport.overworldMap(rom, hdr) ?: error("no es SMW")
                                    val where = SnesImport.exportToDownloads(
                                        context, "smw_overworld.png", "image/png", SnesImport.bitmapToPng(bmp),
                                    ) ?: error("no se pudo guardar")
                                    Toast.makeText(context, "PNG guardado en $where", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo exportar PNG: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }, modifier = Modifier.weight(1f)) { Text("⬇ PNG") }
                            TextButton(onClick = {
                                val rom = romBytes; val hdr = header
                                runCatching {
                                    if (rom == null || hdr == null) error("carga la ROM")
                                    val gif = SnesImport.overworldGif(rom, hdr) ?: error("no es SMW")
                                    val where = SnesImport.exportToDownloads(
                                        context, "smw_overworld.gif", "image/gif", gif,
                                    ) ?: error("no se pudo guardar")
                                    Toast.makeText(context, "GIF animado guardado en $where", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo exportar GIF: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }, modifier = Modifier.weight(1f)) { Text("⬇ GIF animado") }
                        }
                        TextButton(onClick = {
                            val rom = romBytes; val hdr = header
                            runCatching {
                                if (rom == null || hdr == null) error("carga la ROM")
                                val world = SnesImport.overworldWorld(rom, hdr)
                                if (world.isEmpty()) error("no es SMW")
                                var saved = 0
                                world.forEach { (name, bmp) ->
                                    if (SnesImport.exportToDownloads(
                                            context, "smw_ow_$name.png", "image/png", SnesImport.bitmapToPng(bmp),
                                        ) != null
                                    ) saved++
                                }
                                Toast.makeText(context, "Mundo completo: $saved PNG en Descargas", Toast.LENGTH_LONG).show()
                            }.onFailure {
                                Toast.makeText(context, "No se pudo exportar el mundo: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }) { Text("⬇ Mundo completo (todos los submapas, PNG)") }
                        TextButton(onClick = {
                            val rom = romBytes; val hdr = header
                            runCatching {
                                if (rom == null || hdr == null) error("carga la ROM")
                                val bmp = SnesImport.overworldSpriteSheet(rom, hdr) ?: error("no es SMW")
                                val where = SnesImport.exportToDownloads(
                                    context, "smw_ow_sprites.png", "image/png", SnesImport.bitmapToPng(bmp),
                                ) ?: error("no se pudo guardar")
                                Toast.makeText(context, "Sprites del mapa guardados en $where", Toast.LENGTH_LONG).show()
                            }.onFailure {
                                Toast.makeText(context, "No se pudo exportar los sprites: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }) { Text("⬇ Sprites del mapa (Boo, nube, piraña…) PNG") }
                        // MARIO del mapa: la hoja de las 4 direcciones (PNG) y el GIF andando.
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = {
                                val rom = romBytes; val hdr = header
                                runCatching {
                                    if (rom == null || hdr == null) error("carga la ROM")
                                    val bmp = SnesImport.overworldMarioSheet(rom, hdr) ?: error("no es SMW")
                                    val where = SnesImport.exportToDownloads(
                                        context, "smw_ow_mario.png", "image/png", SnesImport.bitmapToPng(bmp),
                                    ) ?: error("no se pudo guardar")
                                    Toast.makeText(context, "Mario del mapa guardado en $where", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo exportar Mario: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }, modifier = Modifier.weight(1f)) { Text("⬇ Mario mapa PNG") }
                            TextButton(onClick = {
                                val rom = romBytes; val hdr = header
                                runCatching {
                                    if (rom == null || hdr == null) error("carga la ROM")
                                    // Dirección 2 = mirando a cámara (el idle del juego).
                                    val gif = SnesImport.overworldMarioGif(rom, hdr, 2) ?: error("no es SMW")
                                    val where = SnesImport.exportToDownloads(
                                        context, "smw_ow_mario_walk.gif", "image/gif", gif,
                                    ) ?: error("no se pudo guardar")
                                    Toast.makeText(context, "Mario andando (GIF) guardado en $where", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo exportar el GIF: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }, modifier = Modifier.weight(1f)) { Text("⬇ Mario andando GIF") }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("🎬 Pantalla de título", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Reconstruye la PANTALLA DE TÍTULO de SMW desde tu ROM: el nivel de fondo con " +
                            "el logo \"SUPER MARIO WORLD\" (Layer 3) y su paleta reales. Exportable a PNG.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = {
                        val rom = romBytes; val hdr = header
                        if (rom != null && hdr != null) {
                            runCatching { SnesImport.titleScreen(rom, hdr)?.asImageBitmap() }
                                .onSuccess {
                                    titlePreview = it
                                    if (it == null) Toast.makeText(
                                        context, "Esta ROM no parece ser Super Mario World.", Toast.LENGTH_LONG,
                                    ).show()
                                }
                                .onFailure { e ->
                                    titlePreview = null
                                    Toast.makeText(
                                        context, "Fallo al reconstruir el título: ${e::class.simpleName}: ${e.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                        }
                    }) { Text("Renderizar pantalla de título") }
                    titlePreview?.let { ts ->
                        Image(
                            bitmap = ts,
                            contentDescription = "Pantalla de título",
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color(0xFF202024), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF555555), RoundedCornerShape(4.dp)),
                        )
                        TextButton(onClick = {
                            val rom = romBytes; val hdr = header
                            runCatching {
                                if (rom == null || hdr == null) error("carga la ROM")
                                val bmp = SnesImport.titleScreen(rom, hdr) ?: error("no es SMW")
                                val where = SnesImport.exportToDownloads(
                                    context, "smw_titulo.png", "image/png", SnesImport.bitmapToPng(bmp),
                                ) ?: error("no se pudo guardar")
                                Toast.makeText(context, "PNG guardado en $where", Toast.LENGTH_LONG).show()
                            }.onFailure {
                                Toast.makeText(context, "No se pudo exportar: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }) { Text("⬇ PNG de la pantalla de título") }
                    }

                    HorizontalDivider()
                    Text("— o ajústalo a mano (avanzado) —", style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider()

                DropdownField(
                    label = "Formato gráfico",
                    options = SnesGraphicFormat.entries.toList(),
                    selected = format,
                    optionLabel = { FORMAT_LABELS[it] ?: it.name },
                    onSelect = { format = it },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        // Detecta el bpp por sí solo sobre los bytes actuales (descomprimidos
                        // si toca): elige el formato con mejor aptitud normalizada.
                        val rom = romBytes ?: return@Button
                        val offset = parseOffset(offsetText)
                        val data = when (decompressMode) {
                            1 -> CompressionCodecs.autoDecompress(rom, offset, format)?.result?.data?.let { it to 0 }
                            2 -> runCatching { LcLz2.decompress(rom, offset) }.getOrNull()?.data?.let { it to 0 }
                            else -> rom to offset
                        }
                        val guess = data?.let { (bytes, off) -> SnesGraphicsScanner.detectBestFormat(bytes, off) }
                        if (guess != null) format = guess.format
                    }) { Text("Detectar bpp") }
                    Text(
                        "  Prueba 2/3/4/8bpp y elige el que da gráficos coherentes.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                val decompressOptions = remember {
                    listOf(
                        0 to "Ninguna (datos crudos)",
                        1 to "Auto-detectar códec",
                        2 to LcLz2.name,
                    )
                }
                DropdownField(
                    label = "Descompresión (experimental)",
                    options = decompressOptions,
                    selected = decompressOptions.first { it.first == decompressMode },
                    optionLabel = { it.second },
                    onSelect = { decompressMode = it.first },
                )
                if (decompressMode != 0) {
                    Text(
                        "Si el juego comprime sus gráficos, indica el offset donde EMPIEZA el " +
                            "bloque comprimido y elige el códec (o Auto). Si la vista previa sigue " +
                            "en ruido, ese juego usa un formato aún no soportado.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = spriteSearch, onCheckedChange = { spriteSearch = it })
                    Text(
                        "  Buscar hojas de SPRITES/personajes (no fondos): premia figuras " +
                            "sólidas sobre fondo transparente, para dar con hojas tipo Mario.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                OutlinedTextField(
                    value = offsetText,
                    onValueChange = { offsetText = it },
                    label = { Text("Offset de los gráficos (dec o 0x…)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (candidates.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = { jumpToCandidate(0) }) { Text("Auto-buscar gráficos") }
                        TextButton(onClick = { jumpToCandidate(candidateIndex - 1) }) { Text("◀") }
                        Text("${candidateIndex + 1}/${candidates.size}", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { jumpToCandidate(candidateIndex + 1) }) { Text("▶") }
                    }
                    Text(
                        "Se detectaron ${candidates.size} zonas que parecen gráficos sin comprimir. " +
                            "Salta entre ellas con ◀ ▶ y mira la vista previa.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (romBytes != null) {
                    Text(
                        "No se detectaron zonas de gráficos claras: puede que los gráficos de " +
                            "este juego estén comprimidos (frecuente en SNES). Prueba otro formato " +
                            "o desplaza el offset manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField("Columnas", columns, { columns = it.coerceIn(1, 64) }, Modifier.weight(1f))
                    IntField("Nº de tiles", tiles, { tiles = it.coerceIn(1, 1024) }, Modifier.weight(1f))
                }

                val spriteOptions = remember {
                    listOf(
                        1 to "8×8 · sin agrupar (tiles sueltos)",
                        2 to "16×16 · agrupar 2×2",
                        3 to "24×24 · agrupar 3×3",
                        4 to "32×32 · agrupar 4×4",
                    )
                }
                DropdownField(
                    label = "Tamaño de sprite (atlas)",
                    options = spriteOptions,
                    selected = spriteOptions.first { it.first == spriteTiles },
                    optionLabel = { it.second },
                    onSelect = { spriteTiles = it.first },
                )
                if (spriteTiles > 1) {
                    Text(
                        "Muchos juegos guardan un sprite grande como varios tiles de 8×8 " +
                            "consecutivos; sin agrupar salen partidos en trozos. Al agrupar " +
                            "${spriteTiles}×${spriteTiles}, cada bloque se recompone como un " +
                            "sprite entero de ${spriteTiles * 8}×${spriteTiles * 8} px en el atlas. " +
                            "\"Columnas\" pasa a ser sprites por fila.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                DropdownField(
                    label = "Paleta",
                    options = paletteOptions,
                    selected = paletteOptions.firstOrNull { it.index == paletteIndex } ?: paletteOptions.first(),
                    optionLabel = { it.label },
                    onSelect = { paletteIndex = it.index },
                )
                Text(
                    "Se detectaron ${detected.size} paletas CGRAM en la ROM. El índice de " +
                        "color 0 se guarda transparente (fondo de los sprites).",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (paletteIndex == PALETTE_AUTO && preview?.autoPalette != null) {
                    Text(
                        "Elegida para este gráfico: ${preview.autoPalette}. Si el color no " +
                            "convence, elige otra del desplegable.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (paletteIndex == PALETTE_GRAYSCALE) {
                    Text(
                        "En escala de grises ves la FORMA de los tiles aunque no sepas su paleta " +
                            "real: si aquí distingues dibujos, has dado con gráficos de verdad " +
                            "(vuelve a \"Automática\" y el emparejador buscará su color). Con la " +
                            "paleta equivocada, unos gráficos correctos parecen ruido de colores.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                HorizontalDivider()
                Text("Vista previa", style = MaterialTheme.typography.titleSmall)
                if (preview != null) {
                    val img = preview.bitmap
                    val sheet = preview.sheet
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFF202024), RoundedCornerShape(8.dp)),
                    ) {
                        // Escala entera para mantener el pixel-art nítido y encajar el ancho/alto.
                        val scale = floor(
                            minOf(size.width / img.width, size.height / img.height),
                        ).coerceAtLeast(1f)
                        drawImage(
                            image = img,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(img.width, img.height),
                            dstOffset = IntOffset(
                                ((size.width - img.width * scale) / 2f).toInt().coerceAtLeast(0),
                                ((size.height - img.height * scale) / 2f).toInt().coerceAtLeast(0),
                            ),
                            dstSize = IntSize((img.width * scale).toInt(), (img.height * scale).toInt()),
                            filterQuality = FilterQuality.None,
                        )
                    }
                    val unit = if (spriteTiles > 1) "sprites" else "tiles"
                    Text(
                        "${sheet.columns}×${sheet.rows} $unit · ${img.width}×${img.height} px · celda ${sheet.tileSize}px",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        "Sin datos válidos en este offset/formato. Prueba otro offset " +
                            "(muchos gráficos SNES empiezan en múltiplos de 0x1000).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                HorizontalDivider()
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del tileset") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(
                    enabled = preview != null,
                    onClick = {
                        val sheet = preview?.sheet ?: return@Button
                        runCatching {
                            val fileName = SnesImport.sanitizeFileName(name)
                            val bmp = SnesImport.toBitmap(sheet.image)
                            SnesImport.saveTilesetPng(state.projectDir, fileName, bmp)
                            val tileset = SnesAssetExtractor.toTileset(
                                sheet, id = state.nextTilesetId(),
                                name = name.ifBlank { "SNES" }, imageFileName = fileName,
                            )
                            state.addTileset(tileset)
                            Toast.makeText(context, "Tileset importado: $fileName", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }.onFailure {
                            Toast.makeText(context, "No se pudo guardar: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                ) { Text("Guardar tileset") }
            }
        }
    }
}

/**
 * Puente GFX → EDITOR: coge un BANCO de gráficos de SMW (GFX00..GFX33) con la FILA DE PALETA
 * que elijas (palette-swap en vivo) y lo hornea como TILESET 8×8 del proyecto. Ese tileset de
 * 8×8 es justo lo que come el editor Map16 para COMPONER bloques de 16×16 y pintar mapas nuevos
 * — el eslabón que faltaba entre "el extractor saca y ordena" y "el editor suministra y ordena".
 *
 * Nada derivado de la ROM se versiona: el PNG se hornea en el proyecto del usuario, en su
 * dispositivo, igual que el resto de importaciones.
 */
@Composable
private fun GfxBankToTilesetSection(state: EditorState, rom: ByteArray, header: SnesHeader) {
    val context = LocalContext.current
    val banks = remember(rom) { SmwGfxLibrary.banks(rom) }
    if (banks.isEmpty()) return
    val paletteRows = remember(rom, header) { SmwGfxLibrary.paletteRows(rom, header) }
    val rowCount = if (paletteRows.isNotEmpty()) paletteRows.size else SmwGfxLibrary.PALETTE_ROWS

    var bankIdx by remember { mutableStateOf(0) }
    // Fila 2 (sprites) es un arranque razonable; cualquiera vale con el palette-swap.
    var palRow by remember { mutableStateOf(2.coerceIn(0, rowCount - 1)) }

    val bank = banks[bankIdx.coerceIn(0, banks.size - 1)]
    fun paletteRow(): IntArray = paletteRows.getOrNull(palRow) ?: VIVID_16

    val previewSheet = remember(rom, bank.id, palRow, paletteRows) {
        runCatching { SmwGfxLibrary.bankTileSheet(rom, bank.id, paletteRow()) }.getOrNull()
    }

    Text("🧩 Banco GFX → hoja del proyecto", style = MaterialTheme.typography.titleSmall)
    Text(
        "Elige un BANCO de gráficos de SMW y su FILA DE PALETA (mismo banco, colores distintos). " +
            "\"Usar este banco\" lo guarda como TILESET de 8×8 en tu proyecto; luego el editor " +
            "Map16 arma bloques de 16×16 con él para pintar mapas nuevos.",
        style = MaterialTheme.typography.bodySmall,
    )

    // Selector de banco.
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { bankIdx = (bankIdx - 1 + banks.size) % banks.size }) { Text("◀") }
        Text(
            "Banco GFX%02X · %d teselas".format(bank.id, bank.tileCount),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { bankIdx = (bankIdx + 1) % banks.size }) { Text("▶") }
    }

    // Selector de fila de paleta (palette-swap).
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { palRow = (palRow - 1 + rowCount) % rowCount }) { Text("◀") }
        Text("Paleta $palRow", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { palRow = (palRow + 1) % rowCount }) { Text("▶") }
        // Muestras de la fila elegida.
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            paletteRow().forEach { argb ->
                Spacer(
                    Modifier
                        .width(12.dp)
                        .height(12.dp)
                        .background(Color(argb), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0xFF555555), RoundedCornerShape(2.dp)),
                )
            }
        }
    }

    // Vista previa del banco recoloreado.
    if (previewSheet != null) {
        val img = SnesImport.toBitmap(previewSheet.image).asImageBitmap()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF202024), RoundedCornerShape(8.dp)),
        ) {
            val scale = floor(minOf(size.width / img.width, size.height / img.height)).coerceAtLeast(1f)
            drawImage(
                image = img,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset(
                    ((size.width - img.width * scale) / 2f).toInt().coerceAtLeast(0),
                    ((size.height - img.height * scale) / 2f).toInt().coerceAtLeast(0),
                ),
                dstSize = IntSize((img.width * scale).toInt(), (img.height * scale).toInt()),
                filterQuality = FilterQuality.None,
            )
        }
        Text(
            "${previewSheet.columns}×${previewSheet.rows} teselas · " +
                "${img.width}×${img.height} px · celda ${previewSheet.tileSize}px",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Button(
        enabled = previewSheet != null,
        onClick = {
            val sheet = previewSheet ?: return@Button
            runCatching {
                val fileName = SnesImport.sanitizeFileName("smw_gfx_%02x_p%d".format(bank.id, palRow))
                SnesImport.saveTilesetPng(state.projectDir, fileName, SnesImport.toBitmap(sheet.image))
                state.addTileset(
                    SnesAssetExtractor.toTileset(
                        sheet, state.nextTilesetId(),
                        "GFX%02X · pal %d".format(bank.id, palRow), fileName,
                    ),
                )
                Toast.makeText(
                    context, "Tileset 8×8 añadido: $fileName (úsalo en el editor Map16)",
                    Toast.LENGTH_LONG,
                ).show()
            }.onFailure {
                Toast.makeText(context, "No se pudo guardar: ${it.message}", Toast.LENGTH_LONG).show()
            }
        },
    ) { Text("Usar este banco → hoja del proyecto") }
}

/** Máximo de niveles que auto-carga el Platform Builder de una ROM de un toque. */
internal const val AUTO_MAX_LEVELS = 8

/**
 * Importa UN nivel de SMW como mapa jugable del proyecto (tileset con colisión real +
 * acciones de bloque + Layer 2 de fondo editable + enemigos). Devuelve el id del
 * tileset creado. Lo usa el Platform Builder para auto-cargar los niveles de una ROM.
 */
internal fun importSmwLevelMap(
    state: EditorState,
    name: String,
    m: SnesGameRecipes.SmwLevelMap,
    // ROM/cabecera/nivel: opcionales, solo para sembrar la META real del nivel. Sin ellos el
    // mapa se importa igual pero sin meta (compatibilidad con llamadas antiguas).
    rom: ByteArray? = null,
    hdr: SnesHeader? = null,
    level: Int? = null,
): Int {
    val fileName = SnesImport.sanitizeFileName("smw_${name}_tiles")
    SnesImport.saveTilesetPng(state.projectDir, fileName, SnesImport.toBitmap(m.atlas))
    val tsId = state.nextTilesetId()
    state.addTileset(
        Tileset(
            id = tsId, name = "$name (SMW)", image = fileName,
            tileSize = 16, columns = m.columns, rows = m.rows,
            passable = m.passable, platformSolidity = m.solidity,
            platformSlopeShape = m.slopeShapes,
            animations = m.animations, platformBlockActions = m.blockActions,
        ),
    )
    val layers = if (m.bgTiles.isNotEmpty()) {
        listOf(m.bgTiles, m.tiles)
    } else {
        listOf(m.tiles, List(m.mapWidth * m.mapHeight) { EMPTY_TILE })
    }
    state.addImportedMap(
        GameMap(
            id = 0, name = "SMW $name", width = m.mapWidth, height = m.mapHeight,
            tilesetId = tsId, layers = layers,
            platformEnemies = m.enemies.map {
                PlatformEnemyMark(spriteId = it.first, x = it.second, y = it.third)
            },
            platformItems = if (rom != null && hdr != null && level != null)
                smwGoalMarks(rom, hdr, level, m.mapWidth, m.mapHeight) else emptyList(),
            platformMusicIndex = if (rom != null && hdr != null && level != null)
                smwMusicIndex(rom, hdr, level) else -1,
        ),
    )
    return tsId
}

/**
 * Marcas de META del nivel [level] de SMW (cinta/esfera/cerradura reales, [SmwLevelGoal.goalCells])
 * como ítems del mapa, recortadas a [w]×[h]. Vacío si el nivel no tiene meta (castillos con jefe,
 * casas de Yoshi) o si falla la lectura. Con esto el mapa importado se puede SUPERAR al jugarlo:
 * tocar la meta lo marca como completado, igual que la ruta ▶ directa de la ROM.
 */
private fun smwGoalMarks(rom: ByteArray, hdr: SnesHeader, level: Int, w: Int, h: Int): List<PlatformItemMark> =
    runCatching {
        SmwLevelGoal.goalCells(rom, SnesGameRecipes.smwHeaderDeltaPublic(hdr), level)
            .filter { (x, y) -> x in 0 until w && y in 0 until h }
            .map { (x, y) -> PlatformItemMark(PlatformItemType.GOAL, x, y) }
    }.onFailure {
        // Antes esto se tragaba y el nivel quedaba SIN META sin que nadie lo supiera: un
        // nivel imposible de superar y ninguna pista de por qué. Ahora al menos queda traza.
        android.util.Log.w("SnesImport", "sin meta en el nivel ${level.toString(16)}: ${it.message}")
    }.getOrDefault(emptyList())

/**
 * Índice de MÚSICA del nivel [level] de SMW (de su cabecera), para que el mapa importado toque
 * SU canción real al jugarlo. -1 si no se puede leer (el reproductor cae a la canción por defecto).
 */
private fun smwMusicIndex(rom: ByteArray, hdr: SnesHeader, level: Int): Int =
    runCatching { SnesGameRecipes.smwLevelInfo(rom, hdr, level)?.musicIndex ?: -1 }.getOrDefault(-1)

/**
 * Importa el nivel [level] de SMW como MAPAS del proyecto: el nivel elegido MÁS sus
 * sub-niveles enlazados ([SmwLevelBundle]: a donde llevan sus tuberías y puertas),
 * y rellena [GameMap.platformWarps] cruzando las bocas de warp reales de cada
 * sub-nivel ([SmwWarpTiles.levelWarps]) con los ids de mapa recién creados — así las
 * tuberías/puertas FUNCIONAN al jugar el mapa. Si el bundle no es extraíble, cae al
 * comportamiento clásico: un solo mapa ([fallback]) sin warps.
 * Devuelve el mensaje para el usuario.
 */
private fun importSmwLevelBundle(
    state: EditorState,
    rom: ByteArray,
    hdr: SnesHeader,
    level: Int,
    name: String,
): String {
    // Si el paquete (nivel + sub-niveles + warps) no se puede extraer, se cae a UN solo nivel.
    // Eso es una degradación real —te llevas menos de lo que crees— así que se deja traza y
    // el mensaje final la refleja, en vez de fingir que se importó todo.
    var bundleError: String? = null
    val bundle = runCatching { SmwLevelBundle.extract(rom, hdr, level) }
        .onFailure { bundleError = "${it::class.simpleName}: ${it.message}" }
        .getOrNull()
    val entries: List<Pair<Int, SnesGameRecipes.SmwLevelMap>> =
        bundle?.levels?.zip(bundle.maps)
            ?: listOf(level to (SnesGameRecipes.extractSmwLevelAsMap(rom, hdr, level) ?: error("nivel no reconstruible")))

    // 1ª pasada: crea tileset + mapa por sub-nivel y apunta nivel SMW → id de mapa.
    val mapIdByLevel = HashMap<Int, Int>()
    val created = ArrayList<Pair<Int, GameMap>>()
    for ((lv, m) in entries) {
        val subName = if (lv == level) name else "$name·${lv.toString(16).uppercase()}"
        val fileName = SnesImport.sanitizeFileName("smw_${subName}_tiles")
        SnesImport.saveTilesetPng(state.projectDir, fileName, SnesImport.toBitmap(m.atlas))
        val tsId = state.nextTilesetId()
        state.addTileset(
            Tileset(
                id = tsId, name = "$subName (SMW)", image = fileName,
                tileSize = 16, columns = m.columns, rows = m.rows,
                passable = m.passable, platformSolidity = m.solidity,
                platformSlopeShape = m.slopeShapes,
                animations = m.animations, platformBlockActions = m.blockActions,
            )
        )
        // Capas del mapa. Si el nivel trae fondo (Layer 2), va DEBAJO (capa 0) y el primer
        // plano (Layer 1) encima; si no, primer plano en capa 0 + una capa vacía por
        // compatibilidad. El editor y el motor dibujan las capas en orden (0 = fondo).
        val layers = if (m.bgTiles.isNotEmpty()) {
            listOf(m.bgTiles, m.tiles)
        } else {
            listOf(m.tiles, List(m.mapWidth * m.mapHeight) { EMPTY_TILE })
        }
        val stored = state.addImportedMap(
            GameMap(
                id = 0, name = "SMW $subName", width = m.mapWidth, height = m.mapHeight,
                tilesetId = tsId,
                layers = layers,
                platformEnemies = m.enemies.map {
                    PlatformEnemyMark(spriteId = it.first, x = it.second, y = it.third)
                },
                // META del nivel: la cinta/esfera/cerradura reales del sub-nivel, para que al
                // JUGAR el mapa importado tocar la meta cuente como SUPERADO (antes no se sembraba
                // ninguna → el nivel no tenía final).
                platformItems = smwGoalMarks(rom, hdr, lv, m.mapWidth, m.mapHeight),
                // Música real del sub-nivel: al jugar el mapa suena SU canción (no la genérica).
                platformMusicIndex = smwMusicIndex(rom, hdr, lv),
            )
        )
        mapIdByLevel[lv] = stored.id
        created.add(lv to stored)
    }

    // 2ª pasada: warps por sub-nivel, con el destino traducido a id de mapa del
    // proyecto. Solo puertas y tuberías verticales (mismos criterios que levelWarps).
    var warpCount = 0
    for ((lv, stored) in created) {
        val warps = SmwWarpTiles.levelWarps(rom, hdr, lv).mapNotNull { w ->
            mapIdByLevel[w.destLevel]?.let { destMapId ->
                MapWarp(
                    x = w.xTile, y = w.yTile,
                    input = if (w.enterDown) 0 else 1, // 0=abajo (tubería), 1=arriba (puerta)
                    destMapId = destMapId, destX = w.destXTile, destY = w.destYTile,
                )
            }
        }
        if (warps.isNotEmpty()) {
            state.updateMap(stored.copy(platformWarps = warps))
            warpCount += warps.size
        }
    }
    // Deja abierto el nivel PRINCIPAL (el primero), no el último sub-nivel del
    // bundle: si no, tras importar el editor mostraba una salita casi vacía.
    created.firstOrNull()?.let { state.selectMap(it.second.id) }
    val aviso = bundleError?.let { " — ⚠ solo el nivel principal: falló el paquete ($it)" } ?: ""
    return if (created.size > 1) {
        "Nivel completo: ${created.size} mapas y $warpCount warps (SMW $name)$aviso"
    } else {
        "Mapa creado: SMW $name" + (if (warpCount > 0) " ($warpCount warps)" else "") + aviso
    }
}
