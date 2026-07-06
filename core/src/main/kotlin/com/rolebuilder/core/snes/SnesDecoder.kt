package com.rolebuilder.core.snes

/**
 * Decodificador de los formatos planares del SNES (y variantes GB/NES),
 * conversión de color BGR15 ↔ ARGB, análisis de la cabecera y detección de
 * paletas. Portado a Kotlin JVM puro para ejecutarse en el dispositivo.
 */
object SnesDecoder {

    // ------------------------------------------------------------------ color

    /** Convierte un color de 15 bits BGR del SNES (0bbbbbgg gggrrrrr) a ARGB opaco. */
    fun bgr15ToArgb(bgr: Int): Int {
        val r5 = bgr and 0x1F
        val g5 = (bgr shr 5) and 0x1F
        val b5 = (bgr shr 10) and 0x1F
        // Escala de 5 a 8 bits repartiendo el rango completo (x*255/31).
        val r8 = (r5 * 255 + 15) / 31
        val g8 = (g5 * 255 + 15) / 31
        val b8 = (b5 * 255 + 15) / 31
        return (0xFF shl 24) or (r8 shl 16) or (g8 shl 8) or b8
    }

    /** Convierte un ARGB estándar al color de 15 bits BGR del SNES. */
    fun argbToBgr15(argb: Int): Int {
        val r8 = (argb shr 16) and 0xFF
        val g8 = (argb shr 8) and 0xFF
        val b8 = argb and 0xFF
        val r5 = (r8 * 31 + 127) / 255
        val g5 = (g8 * 31 + 127) / 255
        val b5 = (b8 * 31 + 127) / 255
        return (b5 shl 10) or (g5 shl 5) or r5
    }

    /** Formatea un ARGB como cadena hexadecimal CSS (#RRGGBB), útil para depurar/exportar. */
    fun argbToHex(argb: Int): String {
        val rgb = argb and 0xFFFFFF
        return "#" + rgb.toString(16).padStart(6, '0').uppercase()
    }

    // -------------------------------------------------------------------- tile

    private fun byte(rom: ByteArray, i: Int): Int = rom[i].toInt() and 0xFF

    /**
     * Decodifica un tile 8×8 en su [offset] al formato indicado, devolviendo 64
     * índices de color. Si el tile se sale de la ROM, devuelve un tile vacío.
     */
    fun decodeTile(rom: ByteArray, offset: Int, format: SnesGraphicFormat, index: Int = offset / format.bytesPerTile): DecodedTile {
        val pixels = IntArray(64)
        if (offset < 0 || offset + format.bytesPerTile > rom.size) {
            return DecodedTile(index, offset, pixels)
        }

        when (format) {
            SnesGraphicFormat.SNES_2BPP, SnesGraphicFormat.GB_2BPP -> {
                // 2 planos entrelazados byte a byte: b0, b1, b0, b1, ...
                for (y in 0..7) {
                    val b1 = byte(rom, offset + 2 * y)
                    val b2 = byte(rom, offset + 2 * y + 1)
                    for (x in 0..7) {
                        val bit0 = (b1 shr (7 - x)) and 1
                        val bit1 = (b2 shr (7 - x)) and 1
                        pixels[y * 8 + x] = bit0 or (bit1 shl 1)
                    }
                }
            }
            SnesGraphicFormat.NES_2BPP -> {
                // Los dos planos van consecutivos: 8 bytes del bit 0, luego 8 del bit 1.
                for (y in 0..7) {
                    val b1 = byte(rom, offset + y)
                    val b2 = byte(rom, offset + y + 8)
                    for (x in 0..7) {
                        val bit0 = (b1 shr (7 - x)) and 1
                        val bit1 = (b2 shr (7 - x)) and 1
                        pixels[y * 8 + x] = bit0 or (bit1 shl 1)
                    }
                }
            }
            SnesGraphicFormat.SNES_3BPP -> {
                // Bytes 0-15: planos 0 y 1 entrelazados. Bytes 16-23: plano 2 suelto.
                for (y in 0..7) {
                    val b1 = byte(rom, offset + 2 * y)
                    val b2 = byte(rom, offset + 2 * y + 1)
                    val b3 = byte(rom, offset + 16 + y)
                    for (x in 0..7) {
                        val s = 7 - x
                        pixels[y * 8 + x] = ((b1 shr s) and 1) or
                            (((b2 shr s) and 1) shl 1) or
                            (((b3 shr s) and 1) shl 2)
                    }
                }
            }
            SnesGraphicFormat.SNES_4BPP -> {
                // Bytes 0-15: bits 0,1. Bytes 16-31: bits 2,3.
                for (y in 0..7) {
                    val b1 = byte(rom, offset + 2 * y)
                    val b2 = byte(rom, offset + 2 * y + 1)
                    val b3 = byte(rom, offset + 16 + 2 * y)
                    val b4 = byte(rom, offset + 16 + 2 * y + 1)
                    for (x in 0..7) {
                        val s = 7 - x
                        pixels[y * 8 + x] = ((b1 shr s) and 1) or
                            (((b2 shr s) and 1) shl 1) or
                            (((b3 shr s) and 1) shl 2) or
                            (((b4 shr s) and 1) shl 3)
                    }
                }
            }
            SnesGraphicFormat.SNES_8BPP -> {
                // 4 pares de planos (16 bytes cada par): bits 0-1, 2-3, 4-5, 6-7.
                for (y in 0..7) {
                    val b1 = byte(rom, offset + 2 * y)
                    val b2 = byte(rom, offset + 2 * y + 1)
                    val b3 = byte(rom, offset + 16 + 2 * y)
                    val b4 = byte(rom, offset + 16 + 2 * y + 1)
                    val b5 = byte(rom, offset + 32 + 2 * y)
                    val b6 = byte(rom, offset + 32 + 2 * y + 1)
                    val b7 = byte(rom, offset + 48 + 2 * y)
                    val b8 = byte(rom, offset + 48 + 2 * y + 1)
                    for (x in 0..7) {
                        val s = 7 - x
                        pixels[y * 8 + x] = ((b1 shr s) and 1) or
                            (((b2 shr s) and 1) shl 1) or
                            (((b3 shr s) and 1) shl 2) or
                            (((b4 shr s) and 1) shl 3) or
                            (((b5 shr s) and 1) shl 4) or
                            (((b6 shr s) and 1) shl 5) or
                            (((b7 shr s) and 1) shl 6) or
                            (((b8 shr s) and 1) shl 7)
                    }
                }
            }
        }
        return DecodedTile(index, offset, pixels)
    }

    // ------------------------------------------------------------------ header

    private data class HeaderCandidate(val offset: Int, val mapping: SnesMapping)

    /**
     * Analiza la cabecera interna de la ROM probando las ubicaciones estándar
     * (LoROM/HiROM, con y sin cabecera de copiador SMC de 512 bytes) y elige la
     * candidata con mejor puntuación de integridad.
     */
    fun parseHeader(rom: ByteArray): SnesHeader {
        val candidates = listOf(
            HeaderCandidate(0x7FC0, SnesMapping.LOROM),
            HeaderCandidate(0x7FC0 + 512, SnesMapping.LOROM),
            HeaderCandidate(0xFFC0, SnesMapping.HIROM),
            HeaderCandidate(0xFFC0 + 512, SnesMapping.HIROM),
        )

        var best: SnesHeader? = null
        var bestScore = -1

        for (cand in candidates) {
            val o = cand.offset
            if (o + 32 > rom.size) continue

            val sb = StringBuilder()
            var printable = 0
            for (i in 0..20) {
                val c = byte(rom, o + i)
                when {
                    c in 32..126 -> { sb.append(c.toChar()); printable++ }
                    c == 0 -> sb.append(' ')
                    else -> sb.append('?')
                }
            }

            val makeup = byte(rom, o + 21)
            val romType = byte(rom, o + 22)
            val sizeExp = byte(rom, o + 23)
            val sramExp = byte(rom, o + 24)
            val country = byte(rom, o + 25)
            val licensee = byte(rom, o + 26)
            val version = byte(rom, o + 27)
            val complement = byte(rom, o + 28) or (byte(rom, o + 29) shl 8)
            val checksum = byte(rom, o + 30) or (byte(rom, o + 31) shl 8)

            var score = 0
            if (printable >= 10) score += 5
            if ((checksum xor complement) == 0xFFFF) score += 15
            if (sizeExp in 0x05..0x0E) score += 3
            if (sramExp <= 0x08) score += 2
            if (country <= 0x14) score += 2

            val romSize = if (sizeExp > 0) (1L shl sizeExp) * 1024L else rom.size.toLong()
            val sramSize = if (sramExp in 1..0x1F) (1L shl sramExp) * 1024L else 0L

            val header = SnesHeader(
                title = sb.toString().trim(),
                isFastRom = (makeup and 0x10) != 0,
                romTypeDescription = romTypeName(romType),
                romSizeBytes = romSize,
                sramSizeBytes = sramSize,
                country = countryName(country),
                licensee = licenseeName(licensee),
                version = version,
                checksum = checksum,
                checksumComplement = complement,
                isChecksumValid = (checksum xor complement) == 0xFFFF,
                mapping = cand.mapping,
                headerOffset = o,
            )
            if (score > bestScore) {
                bestScore = score
                best = header
            }
        }

        return best ?: SnesHeader(
            title = "ROM desconocida",
            isFastRom = false,
            romTypeDescription = "ROM Only",
            romSizeBytes = rom.size.toLong(),
            sramSizeBytes = 0,
            country = "N/A",
            licensee = "N/A",
            version = 0,
            checksum = 0,
            checksumComplement = 0xFFFF,
            isChecksumValid = false,
            mapping = SnesMapping.UNKNOWN,
            headerOffset = 0,
        )
    }

    private fun romTypeName(type: Int): String = when (type) {
        0x00 -> "ROM Only"
        0x01 -> "ROM + RAM"
        0x02 -> "ROM + RAM + Battery"
        0x03 -> "ROM + DSP-1"
        0x04 -> "ROM + DSP-1 + RAM + Battery"
        0x13 -> "ROM + Super FX"
        0x15 -> "ROM + Super FX + RAM + Battery"
        0x25 -> "ROM + OBC-1 + RAM + Battery"
        0x34 -> "ROM + S-DD1"
        0x35 -> "ROM + S-DD1 + RAM + Battery"
        0xE3 -> "ROM + Super Game Boy"
        else -> "Desconocido (0x${type.toString(16).uppercase()})"
    }

    private fun countryName(code: Int): String = when (code) {
        0x00 -> "Japón (NTSC)"
        0x01 -> "EE.UU./Canadá (NTSC)"
        0x02 -> "Europa/Oceanía (PAL)"
        0x03 -> "Suecia (PAL)"
        0x06 -> "Francia (PAL)"
        0x08 -> "España (PAL)"
        0x09 -> "Alemania (PAL)"
        0x0A -> "Italia (PAL)"
        0x0D -> "Corea (NTSC)"
        else -> "Global (0x${code.toString(16).uppercase()})"
    }

    private fun licenseeName(code: Int): String = when (code) {
        0x01 -> "Nintendo"
        0x03 -> "Capcom"
        0x08 -> "Jaleco"
        0x09 -> "Electronic Arts"
        0x0B -> "Hudson Soft"
        0x0C -> "Square"
        0x0D -> "Namco"
        0x0E -> "Acclaim"
        0x10 -> "Bandai"
        0x12 -> "Enix"
        0x1A -> "Konami"
        0x24 -> "Taito"
        0x28 -> "Kemco"
        else -> "Código 0x${code.toString(16).uppercase()}"
    }

    // ----------------------------------------------------------------- palette

    /**
     * Lee [colorCount] colores CGRAM (2 bytes cada uno, little-endian) desde
     * [offset] y los convierte a ARGB. Los huecos fuera de la ROM se rellenan
     * con negro opaco.
     */
    fun parsePalette(rom: ByteArray, offset: Int, colorCount: Int): IntArray {
        val colors = IntArray(colorCount)
        for (i in 0 until colorCount) {
            val idx = offset + 2 * i
            colors[i] = if (idx + 1 >= rom.size) {
                0xFF000000.toInt()
            } else {
                bgr15ToArgb(byte(rom, idx) or (byte(rom, idx + 1) shl 8))
            }
        }
        return colors
    }

    /**
     * Paleta en escala de grises: una rampa del índice 0 (negro) al máximo
     * (blanco). Sirve para VER LA FORMA de unos gráficos cuando todavía no se
     * conoce su paleta real. Con una paleta de color equivocada, unos tiles de
     * 4/8bpp parecen ruido de colores aunque el dibujo esté perfectamente
     * decodificado; en gris, cada índice se traduce a un nivel de brillo
     * ordenado, así que las formas se distinguen con claridad. El índice 0 se
     * sigue tratando como transparente al componer la hoja de tiles.
     */
    fun grayscalePalette(colorCount: Int): IntArray {
        val steps = (colorCount - 1).coerceAtLeast(1)
        return IntArray(colorCount) { i ->
            val level = i * 255 / steps
            (0xFF shl 24) or (level shl 16) or (level shl 8) or level
        }
    }

    /**
     * Puntúa una ventana de 16 colores en [offset] según cuánto "parece una
     * paleta real" de SNES, o devuelve null si no es válida (algún bit 15 a 1, o
     * menos de [minUniqueColors] colores distintos). Las paletas reales tienden a
     * tener **rampas de brillo suaves** (para el sombreado), un color oscuro de
     * fondo, buen contraste y algo de saturación; los datos aleatorios o de
     * código casi no cumplen nada de esto. Se prueba en offsets pares porque cada
     * color CGRAM ocupa 2 bytes (los offsets impares serían lecturas desalineadas).
     */
    private fun scorePaletteWindow(rom: ByteArray, offset: Int, minUniqueColors: Int): Double? {
        val lum = IntArray(16)               // luminancia 0..255
        val unique = HashSet<Int>()
        var satSum = 0                        // saturación acumulada 0..255 por color
        for (i in 0 until 16) {
            val idx = offset + 2 * i
            val bgr = byte(rom, idx) or (byte(rom, idx + 1) shl 8)
            if (bgr and 0x8000 != 0) return null // bit 15 debe ser 0 en CGRAM
            // Escala 5->8 bits idéntica a bgr15ToArgb para puntuar en el mismo espacio.
            val r = ((bgr and 0x1F) * 255 + 15) / 31
            val g = (((bgr shr 5) and 0x1F) * 255 + 15) / 31
            val b = (((bgr shr 10) and 0x1F) * 255 + 15) / 31
            unique.add(bgr)
            lum[i] = (r * 30 + g * 59 + b * 11) / 100
            satSum += maxOf(r, g, b) - minOf(r, g, b)
        }
        if (unique.size < minUniqueColors) return null

        var jumpSum = 0
        var ramps = 0
        var minL = lum[0]; var maxL = lum[0]
        for (i in 0 until 15) {
            val d = kotlin.math.abs(lum[i + 1] - lum[i])
            jumpSum += d
            if (d <= 24) ramps++                                // paso suave -> rampa de sombreado
            if (lum[i + 1] < minL) minL = lum[i + 1]
            if (lum[i + 1] > maxL) maxL = lum[i + 1]
        }
        val avgJump = jumpSum / 15.0
        val span = (maxL - minL).toDouble()
        val sat = satSum / 16.0
        val darkBonus = if (minL < 40) 20.0 else 0.0            // un color oscuro/fondo
        return ramps * 6.0 + span * 0.25 + sat * 0.6 + darkBonus - avgJump * 0.8
    }

    /**
     * Recorre TODA la ROM (en offsets pares) buscando bloques de 16 colores que
     * parezcan paletas CGRAM reales, los puntúa por calidad (ver
     * [scorePaletteWindow]), descarta casi-duplicados (ventanas a menos de 32
     * bytes) y devuelve los mejores primero, hasta [maxResults]. Así la primera
     * opción ya es una paleta con pinta de real, no el primer bloque cualquiera.
     *
     * Nota honesta: qué paleta corresponde EXACTAMENTE a unos gráficos concretos
     * lo decide el juego con sus tablas internas de punteros, así que ningún
     * escaneo puede GARANTIZAR el color exacto. Pero elegir a mano tampoco es la
     * única salida: [SnesPaletteMatcher] puntúa estas candidatas contra un
     * gráfico concreto (coherencia de tono de sus pares de índices vecinos) y
     * automatiza el emparejado con acierto habitual; esta lista queda como
     * respaldo manual para corregirlo cuando falle.
     */
    fun scanRomForPalettes(
        rom: ByteArray,
        minUniqueColors: Int = 10,
        maxResults: Int = 16,
    ): List<SnesPalette> {
        val limit = rom.size - 32
        if (limit < 0) return emptyList()

        // Pass 1: puntúa cada ventana válida (barato: solo offset + score).
        val scored = ArrayList<LongArray>() // [score*1000 como Long empaquetado, offset]
        var offset = 0
        while (offset <= limit) {
            val s = scorePaletteWindow(rom, offset, minUniqueColors)
            if (s != null) scored.add(longArrayOf((s * 1000).toLong(), offset.toLong()))
            offset += 2
        }
        // Mejor score primero; a igualdad, el offset más bajo (alineación más limpia).
        scored.sortWith(compareByDescending<LongArray> { it[0] }.thenBy { it[1] })

        // Pass 2: dedup por cercanía (>= 32 bytes) y materializa las mejores.
        val keptOffsets = ArrayList<Int>()
        val result = ArrayList<SnesPalette>()
        for (entry in scored) {
            val off = entry[1].toInt()
            if (keptOffsets.any { kotlin.math.abs(it - off) < 32 }) continue
            keptOffsets.add(off)
            val raw = IntArray(16)
            val colors = IntArray(16)
            for (i in 0 until 16) {
                val bgr = byte(rom, off + 2 * i) or (byte(rom, off + 2 * i + 1) shl 8)
                raw[i] = bgr
                colors[i] = bgr15ToArgb(bgr)
            }
            result.add(
                SnesPalette(
                    id = "detected_$off",
                    name = "CGRAM @ 0x${off.toString(16).uppercase()}",
                    colors = colors,
                    rawBgr15 = raw,
                    offset = off,
                )
            )
            if (result.size >= maxResults) break
        }
        return result
    }
}
