package com.rolebuilder.editor.snes

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
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.Tileset
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
    // Modo fácil (mapas): niveles reconstruidos como mapas jugables (tileset + tilemap).
    var recipeMaps by remember(romBytes) { mutableStateOf<List<Triple<Int, String, SnesGameRecipes.SmwLevelMap>>>(emptyList()) }

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
                            recipeMaps = runCatching { SnesGameRecipes.extractSmwLevelMaps(rom, hdr) }.getOrDefault(emptyList())
                            if (recipeMaps.isEmpty()) {
                                Toast.makeText(context, "No hay niveles reconstruibles en esta ROM", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("Buscar niveles importables") }
                    recipeMaps.forEach { entry ->
                        val name = entry.second
                        val m = entry.third
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Image(
                                bitmap = SnesImport.toBitmap(m.atlas).asImageBitmap(),
                                contentDescription = name,
                                filterQuality = FilterQuality.None,
                                contentScale = ContentScale.FillHeight,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .background(Color(0xFF202024), RoundedCornerShape(4.dp)),
                            )
                            TextButton(onClick = {
                                runCatching {
                                    val fileName = SnesImport.sanitizeFileName("smw_${name}_tiles")
                                    SnesImport.saveTilesetPng(state.projectDir, fileName, SnesImport.toBitmap(m.atlas))
                                    val tsId = state.nextTilesetId()
                                    state.addTileset(
                                        Tileset(
                                            id = tsId, name = "$name (SMW)", image = fileName,
                                            tileSize = 16, columns = m.columns, rows = m.rows,
                                            passable = m.passable, platformSolidity = m.solidity,
                                            animations = m.animations, platformBlockActions = m.blockActions,
                                        )
                                    )
                                    state.addImportedMap(
                                        GameMap(
                                            id = 0, name = "SMW $name", width = m.mapWidth, height = m.mapHeight,
                                            tilesetId = tsId,
                                            layers = listOf(m.tiles, List(m.mapWidth * m.mapHeight) { EMPTY_TILE }),
                                            platformEnemies = m.enemies.map {
                                                PlatformEnemyMark(spriteId = it.first, x = it.second, y = it.third)
                                            },
                                        )
                                    )
                                    Toast.makeText(context, "Mapa creado: SMW $name", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "No se pudo: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }) { Text("Crear mapa") }
                        }
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
