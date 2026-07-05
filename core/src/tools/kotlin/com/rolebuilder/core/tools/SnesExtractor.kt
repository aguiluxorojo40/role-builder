package com.rolebuilder.core.tools

import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SnesAssetExtractor
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesGraphicsScanner
import com.rolebuilder.core.snes.compression.CompressionCodecs
import com.rolebuilder.core.snes.compression.LcLz2
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Herramienta de escritorio para extraer una hoja de tiles desde una ROM de
 * Super Nintendo usando el mismo decodificador que corre en el dispositivo
 * (`core/snes`). Escribe un PNG y el `Tileset` JSON correspondiente, y sirve
 * de referencia exacta de cómo la UI de `:app` debe llamar al decodificador.
 *
 * Uso (vía Gradle):
 *   ./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out out --offset 0x2000 --format 4bpp"
 *   ./gradlew :core:extractSnesTileset --args="--demo out"   # genera una ROM de prueba y la extrae
 *
 * Opciones:
 *   --rom <ruta>            ROM de entrada (.smc/.sfc/.bin). Con SMC de 512 bytes se detecta sola.
 *   --out <dir>            Carpeta de salida (por defecto: snes_out).
 *   --offset <n>           Offset de los gráficos (dec o 0x...). Por defecto 0.
 *   --format <fmt>         2bpp | 4bpp | 8bpp | gb2bpp | nes2bpp (por defecto 4bpp).
 *   --tiles <n>            Nº de tiles a extraer (por defecto: los que quepan, máx. 256).
 *   --columns <n>          Columnas de la rejilla (por defecto 16).
 *   --palette-offset <n>  Offset de la paleta CGRAM en la ROM (si se omite, se usa una por defecto).
 *   --name <texto>        Nombre del tileset y base del archivo (por defecto "snes").
 *   --demo <dir>          Genera una ROM de prueba procedural en <dir>/demo.sfc y la extrae.
 */
fun main(args: Array<String>) {
    val opts = parseArgs(args)

    // Modo demo: fabrica una ROM procedural (sin material con copyright) y la extrae.
    val demoDir = opts["demo"]
    val demoCompressedDir = opts["demo-compressed"]
    val romFile: File
    val outDir: File
    if (demoCompressedDir != null) {
        // ROM de prueba con gráficos COMPRIMIDOS en LC_LZ2 a partir de 0x1000,
        // para demostrar la descompresión sin usar ninguna ROM con copyright.
        outDir = File(demoCompressedDir).also { it.mkdirs() }
        romFile = File(outDir, "demo_lz2.sfc")
        romFile.writeBytes(buildCompressedDemoRom())
        println("ROM comprimida de prueba: ${romFile.absolutePath}")
        println("Pruébala con:  --rom ${romFile.name} --offset 0x1000 --decompress auto --format 4bpp")
    } else if (demoDir != null) {
        outDir = File(demoDir).also { it.mkdirs() }
        romFile = File(outDir, "demo.sfc")
        romFile.writeBytes(buildDemoRom())
        println("ROM de prueba generada: ${romFile.absolutePath} (${romFile.length()} bytes)")
    } else {
        val romPath = opts["rom"] ?: run {
            System.err.println("Falta --rom <ruta> (o usa --demo <dir>). Ejecuta sin argumentos para ver la ayuda.")
            printUsage()
            return
        }
        romFile = File(romPath)
        if (!romFile.isFile) {
            System.err.println("No existe la ROM: ${romFile.absolutePath}")
            return
        }
        outDir = File(opts["out"] ?: "snes_out").also { it.mkdirs() }
    }

    val rom = romFile.readBytes()
    val header = SnesDecoder.parseHeader(rom)
    println("Cabecera: \"${header.title}\"  ${header.mapping}  ${header.romTypeDescription}")
    println("  ${header.country} · ${header.licensee} · checksum ${if (header.isChecksumValid) "válido" else "no válido"}")

    val format = parseFormat(opts["format"] ?: "4bpp")

    // Modo --scan: autodetecta zonas con gráficos. Con --decompress prueba a
    // descomprimir en cada offset (localiza bloques comprimidos); sin él, busca
    // gráficos sin comprimir.
    if (opts.containsKey("scan")) {
        val scanDecompress = opts["decompress"]
        if (scanDecompress != null) {
            val hits = scanForCompressedGraphics(rom, format)
            if (hits.isEmpty()) {
                println("No se encontraron bloques descomprimibles con gráficos (¿otro formato de compresión?).")
            } else {
                println("Bloques comprimidos con gráficos ($format), prueba: --offset <X> --decompress auto")
                hits.forEach { (off, score, size) ->
                    println("  0x${off.toString(16).uppercase()}  (coherencia ${"%.2f".format(score)}, ${size}B)")
                }
            }
        } else {
            val candidates = SnesGraphicsScanner.findCandidates(rom, format)
            if (candidates.isEmpty()) {
                println("No se encontraron zonas gráficas evidentes con $format (¿gráficos comprimidos?).")
            } else {
                println("Candidatos de gráficos SIN comprimir ($format), prueba estos offsets:")
                candidates.forEach { println("  0x${it.offset.toString(16).uppercase()}  (score ${"%.2f".format(it.score)})") }
            }
        }
        return
    }

    // Si no se indica offset, se autodetecta el mejor candidato.
    val offset = opts["offset"]?.let { parseInt(it) }
        ?: SnesGraphicsScanner.findCandidates(rom, format).firstOrNull()?.offset?.also {
            println("Offset autodetectado: 0x${it.toString(16).uppercase()} (usa --offset para fijarlo)")
        } ?: 0
    // --decompress <auto|nombre>: descomprime el bloque en offset y extrae de su salida.
    // Los tiles salen de los bytes descomprimidos; la paleta sigue leyéndose de la ROM.
    val decompressMode = opts["decompress"]
    val tileRom: ByteArray
    val tileOffset: Int
    if (decompressMode != null) {
        val data = when {
            decompressMode.equals("auto", true) -> {
                val auto = CompressionCodecs.autoDecompress(rom, offset, format)
                if (auto == null) {
                    System.err.println("Ningún códec conocido produjo gráficos en 0x${offset.toString(16).uppercase()}.")
                    return
                }
                println("Códec detectado: ${auto.codec.name} (coherencia ${"%.2f".format(auto.score)})")
                auto.result.data
            }
            else -> {
                val codec = CompressionCodecs.all.firstOrNull { it.name.contains(decompressMode, true) }
                    ?: CompressionCodecs.all.first()
                runCatching { codec.decompress(rom, offset) }.getOrElse {
                    System.err.println("No se pudo descomprimir con ${codec.name}: ${it.message}")
                    return
                }.also { println("Descomprimidos ${it.data.size} bytes con ${codec.name}") }.data
            }
        }
        tileRom = data
        tileOffset = 0
    } else {
        tileRom = rom
        tileOffset = offset
    }

    val columns = parseInt(opts["columns"] ?: "16")
    val available = SnesAssetExtractor.availableTiles(tileRom.size, tileOffset, format)
    val tileCount = (opts["tiles"]?.let { parseInt(it) } ?: available).coerceIn(1, minOf(available, 256))

    val palette: IntArray = opts["palette-offset"]?.let {
        SnesDecoder.parsePalette(rom, parseInt(it), format.colorCount)
    } ?: defaultPalette(format.colorCount)

    val name = opts["name"] ?: "snes"
    val sheet = SnesAssetExtractor.extractTileSheet(tileRom, tileOffset, format, palette, tileCount, columns)
    val imageName = "$name.png"

    val imagesDir = File(outDir, "images").also { it.mkdirs() }
    ImageIO.write(toBufferedImage(sheet.image), "png", File(imagesDir, imageName))

    val tileset = SnesAssetExtractor.toTileset(sheet, id = 100, name = name, imageFileName = imageName)
    val json = Json { prettyPrint = true }
    File(outDir, "$name.tileset.json").writeText(json.encodeToString(Tileset.serializer(), tileset))

    println(
        "Extraídos $tileCount tiles ($format) desde 0x${offset.toString(16)} -> " +
            "${sheet.image.width}x${sheet.image.height}px, rejilla ${sheet.columns}x${sheet.rows}"
    )
    println("PNG:     ${File(imagesDir, imageName).absolutePath}")
    println("Tileset: ${File(outDir, "$name.tileset.json").absolutePath}")
}

// ------------------------------------------------------------------ helpers

private fun parseArgs(args: Array<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (a.startsWith("--")) {
            val key = a.substring(2)
            val next = args.getOrNull(i + 1)
            if (next != null && !next.startsWith("--")) {
                map[key] = next; i += 2
            } else {
                map[key] = "true"; i += 1
            }
        } else i += 1
    }
    return map
}

private fun parseInt(s: String): Int =
    if (s.startsWith("0x") || s.startsWith("0X")) s.substring(2).toInt(16) else s.toInt()

private fun parseFormat(s: String): SnesGraphicFormat = when (s.lowercase()) {
    "2bpp", "snes2bpp" -> SnesGraphicFormat.SNES_2BPP
    "4bpp", "snes4bpp" -> SnesGraphicFormat.SNES_4BPP
    "8bpp", "snes8bpp" -> SnesGraphicFormat.SNES_8BPP
    "gb2bpp", "gb" -> SnesGraphicFormat.GB_2BPP
    "nes2bpp", "nes" -> SnesGraphicFormat.NES_2BPP
    else -> error("Formato desconocido: $s (usa 2bpp|4bpp|8bpp|gb2bpp|nes2bpp)")
}

/** Paleta por defecto (degradado + acentos) cuando no se indica un offset CGRAM. */
private fun defaultPalette(colorCount: Int): IntArray = IntArray(colorCount) { i ->
    when (i) {
        0 -> 0x00000000 // transparente
        else -> {
            val t = i * 255 / maxOf(1, colorCount - 1)
            (0xFF shl 24) or (t shl 16) or (t shl 8) or t
        }
    }
}

private fun toBufferedImage(img: ArgbImage): BufferedImage {
    val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    out.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
    return out
}

/**
 * Construye una ROM LoROM mínima de 64 KiB con cabecera válida, una paleta CGRAM
 * en 0x100 y un patrón de tiles 4bpp reconocible en 0x2000. Permite probar la
 * extracción de punta a punta sin usar ninguna ROM con derechos de autor.
 */
private fun buildDemoRom(): ByteArray {
    val rom = ByteArray(0x10000)

    // Cabecera LoROM en 0x7FC0.
    val o = 0x7FC0
    val title = "DEMO ASSET ROM".padEnd(21, ' ')
    for (i in title.indices) rom[o + i] = title[i].code.toByte()
    rom[o + 21] = 0x20            // SlowROM/LoROM
    rom[o + 22] = 0x00            // ROM Only
    rom[o + 23] = 0x08            // tamaño ROM (256 KiB nominal)
    rom[o + 24] = 0x00            // sin SRAM
    rom[o + 25] = 0x08            // España (PAL)
    rom[o + 26] = 0x01            // Nintendo
    rom[o + 27] = 0x00
    val checksum = 0xABCD
    val complement = checksum.inv() and 0xFFFF
    rom[o + 28] = (complement and 0xFF).toByte()
    rom[o + 29] = ((complement shr 8) and 0xFF).toByte()
    rom[o + 30] = (checksum and 0xFF).toByte()
    rom[o + 31] = ((checksum shr 8) and 0xFF).toByte()

    // Paleta CGRAM de 16 colores en 0x100 (arcoíris de 15 bits BGR).
    for (i in 0 until 16) {
        val bgr = SnesDecoder.argbToBgr15(
            (0xFF shl 24) or
                (((i * 17) and 0xFF) shl 16) or
                (((255 - i * 17) and 0xFF) shl 8) or
                ((i * 8) and 0xFF)
        )
        rom[0x100 + 2 * i] = (bgr and 0xFF).toByte()
        rom[0x100 + 2 * i + 1] = ((bgr shr 8) and 0xFF).toByte()
    }

    // 64 tiles 4bpp en 0x2000: cada tile con un degradado diagonal de índices.
    var p = 0x2000
    for (t in 0 until 64) {
        for (y in 0..7) {
            // Índice de color (0..15) por fila; se reparte entre los 4 planos.
            val planes = IntArray(4)
            for (x in 0..7) {
                val value = ((x + y + t) % 16)
                for (bit in 0..3) {
                    if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
            }
            rom[p + 2 * y] = planes[0].toByte()
            rom[p + 2 * y + 1] = planes[1].toByte()
            rom[p + 16 + 2 * y] = planes[2].toByte()
            rom[p + 16 + 2 * y + 1] = planes[3].toByte()
        }
        p += 32
    }
    return rom
}

/**
 * Como [buildDemoRom] pero con gráficos COMPRIMIDOS en LC_LZ2 a partir de 0x1000
 * (tiles con zonas planas, coherentes). Sirve para demostrar y probar la
 * descompresión de punta a punta sin usar ninguna ROM con derechos de autor.
 */
private fun buildCompressedDemoRom(): ByteArray {
    val rom = buildDemoRom() // reutiliza cabecera y paleta CGRAM en 0x100

    // Genera 64 tiles 4bpp "gráficos" (fondo plano + una cruz de otro color).
    val gfx = ByteArray(32 * 64)
    var p = 0
    var tileNo = 0
    while (p + 32 <= gfx.size) {
        val base = tileNo % 16
        val mark = (base + 1) % 16
        for (y in 0..7) {
            val planes = IntArray(4)
            for (x in 0..7) {
                val value = if (x == 3 || y == 3) mark else base
                for (bit in 0..3) if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
            }
            gfx[p + 2 * y] = planes[0].toByte()
            gfx[p + 2 * y + 1] = planes[1].toByte()
            gfx[p + 16 + 2 * y] = planes[2].toByte()
            gfx[p + 16 + 2 * y + 1] = planes[3].toByte()
        }
        p += 32; tileNo++
    }

    val compressed = LcLz2.compress(gfx)
    compressed.copyInto(rom, destinationOffset = 0x1000)
    return rom
}

/**
 * Recorre la ROM probando a descomprimir en cada offset con los códecs conocidos
 * y devuelve los offsets cuya salida "parece un dibujo". Localiza bloques
 * comprimidos sin conocer las tablas de punteros internas del juego.
 */
private fun scanForCompressedGraphics(
    rom: ByteArray,
    format: SnesGraphicFormat,
    minScore: Double = 0.42,
    maxResults: Int = 24,
): List<Triple<Int, Double, Int>> {
    // Los bloques comprimidos empiezan en cualquier byte (no están alineados), así
    // que se prueba byte a byte; tras un acierto se salta el bloque consumido para
    // no repetir casi-duplicados. Se acota la salida para que el barrido sea rápido.
    val hits = ArrayList<Triple<Int, Double, Int>>()
    val minBytes = format.bytesPerTile * 32
    var offset = 0
    while (offset < rom.size - 3) {
        var advanced = false
        for (codec in CompressionCodecs.all) {
            val res = runCatching { codec.decompress(rom, offset, 0x4000) }.getOrNull() ?: continue
            if (res.data.size >= minBytes) {
                val score = CompressionCodecs.graphicScore(res.data, format)
                if (score >= minScore) {
                    hits.add(Triple(offset, score, res.data.size))
                    offset += maxOf(1, res.consumedBytes) // saltar el bloque encontrado
                    advanced = true
                    break
                }
            }
        }
        if (!advanced) offset++
    }
    return hits.sortedByDescending { it.second }.take(maxResults).sortedBy { it.first }
}

private fun printUsage() {
    println(
        """
        Extractor de assets de ROM de SNES
          --rom <ruta> --out <dir> [--offset 0x2000] --format 4bpp [--tiles N] [--columns 16]
          [--palette-offset 0x100] [--name terreno]
          --rom <ruta> --format 4bpp --scan   (autodetecta offsets con gráficos)
          [--decompress auto|lc_lz2]          (descomprime el bloque antes de extraer)
          --demo-compressed <dir>             (ROM de prueba con gráficos LC_LZ2)
        o bien:  --demo <dir>   (genera una ROM de prueba y la extrae)
        Si omites --offset, se autodetecta el mejor candidato de gráficos.
        """.trimIndent()
    )
}
