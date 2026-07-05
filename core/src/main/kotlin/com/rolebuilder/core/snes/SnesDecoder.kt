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
     * Recorre el inicio de la ROM buscando bloques densos de 16 colores CGRAM
     * válidos (bit 15 a 0) para autodetectar paletas. Devuelve como mucho 12.
     */
    fun scanRomForPalettes(rom: ByteArray, minUniqueColors: Int = 10): List<SnesPalette> {
        val detected = mutableListOf<SnesPalette>()
        val limit = minOf(rom.size - 32, 0x10000)
        var offset = 0
        while (offset < limit) {
            val colors = IntArray(16)
            val raw = IntArray(16)
            val unique = HashSet<Int>()
            var valid = true
            for (i in 0 until 16) {
                val idx = offset + 2 * i
                val bgr = byte(rom, idx) or (byte(rom, idx + 1) shl 8)
                if (bgr and 0x8000 != 0) { valid = false; break } // bit 15 debe ser 0 en CGRAM
                val argb = bgr15ToArgb(bgr)
                colors[i] = argb
                raw[i] = bgr
                unique.add(argb)
            }
            if (valid && unique.size >= minUniqueColors) {
                detected.add(
                    SnesPalette(
                        id = "detected_$offset",
                        name = "CGRAM @ 0x${offset.toString(16).uppercase()}",
                        colors = colors,
                        rawBgr15 = raw,
                        offset = offset,
                    )
                )
                if (detected.size >= 12) break
            }
            offset += 256
        }
        return detected
    }
}
