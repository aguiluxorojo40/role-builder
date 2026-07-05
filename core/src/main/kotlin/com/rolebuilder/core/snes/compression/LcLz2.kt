package com.rolebuilder.core.snes.compression

/**
 * Códec **LC_LZ2** (el formato de Lunar Compress usado por Super Mario World y
 * muchos otros juegos de SNES para comprimir gráficos).
 *
 * Formato: un flujo de "chunks". Cada chunk empieza por una cabecera:
 *  - bits 7-5 = comando; bits 4-0 = longitud L (se emiten L+1 bytes).
 *  - si el comando es 111, es la forma "larga": el comando real está en los
 *    bits 4-2 y la longitud en 10 bits = (bits 1-0 << 8 | siguiente byte) + 1.
 *  - una cabecera 0xFF marca el fin del flujo.
 *
 * Comandos:
 *  0 Copia directa   — copia L+1 bytes literales.
 *  1 Relleno de byte — 1 byte repetido L+1 veces.
 *  2 Relleno de word — 2 bytes alternados, L+1 en total.
 *  3 Relleno creciente — 1 byte b: b, b+1, b+2, … (L+1 valores).
 *  4 Repetición (LZ) — 2 bytes de offset ABSOLUTO en la salida ya escrita;
 *                      copia L+1 bytes desde ahí (permite solapamiento tipo RLE).
 *
 * El decodificador sigue esta especificación; el compresor incluido produce
 * flujos que el propio decodificador reconstruye (usado en los tests y disponible
 * para empaquetar assets propios). La validación contra una ROM real concreta se
 * hace visualmente con la vista previa / el detector de coherencia.
 */
object LcLz2 : SnesDecompressor {

    override val name: String get() = "LC_LZ2 (Lunar Compress)"

    /** Si el offset de la orden de repetición se lee big-endian (por defecto) o little. */
    private const val REPEAT_OFFSET_BIG_ENDIAN = true

    private const val CMD_DIRECT_COPY = 0
    private const val CMD_BYTE_FILL = 1
    private const val CMD_WORD_FILL = 2
    private const val CMD_INCREASING_FILL = 3
    private const val CMD_REPEAT = 4
    private const val CMD_LONG = 7
    private const val END_MARKER = 0xFF

    override fun decompress(rom: ByteArray, offset: Int, maxOutput: Int): DecompressResult {
        if (offset < 0 || offset >= rom.size) throw DecompressException("Offset fuera de la ROM")

        val out = GrowableBytes()
        var i = offset

        fun readByte(): Int {
            if (i >= rom.size) throw DecompressException("Fin inesperado del flujo")
            return rom[i++].toInt() and 0xFF
        }

        while (true) {
            if (i >= rom.size) throw DecompressException("No se encontró el marcador de fin (0xFF)")
            val header = rom[i].toInt() and 0xFF
            if (header == END_MARKER) { i++; break }
            i++

            var command = header ushr 5
            val length: Int
            if (command == CMD_LONG) {
                command = (header ushr 2) and 0x07
                val low = readByte()
                length = (((header and 0x03) shl 8) or low) + 1
            } else {
                length = (header and 0x1F) + 1
            }

            when (command) {
                CMD_DIRECT_COPY -> repeat(length) { out.add(readByte()) }
                CMD_BYTE_FILL -> {
                    val b = readByte()
                    repeat(length) { out.add(b) }
                }
                CMD_WORD_FILL -> {
                    val b0 = readByte(); val b1 = readByte()
                    for (k in 0 until length) out.add(if (k and 1 == 0) b0 else b1)
                }
                CMD_INCREASING_FILL -> {
                    var b = readByte()
                    repeat(length) { out.add(b); b = (b + 1) and 0xFF }
                }
                CMD_REPEAT -> {
                    val a = readByte(); val b = readByte()
                    val src = if (REPEAT_OFFSET_BIG_ENDIAN) (a shl 8) or b else (b shl 8) or a
                    if (src >= out.size) throw DecompressException("Offset de repetición inválido")
                    for (k in 0 until length) {
                        val from = src + k
                        if (from >= out.size) throw DecompressException("Repetición fuera de rango")
                        out.add(out[from])
                    }
                }
                else -> throw DecompressException("Comando LC_LZ2 no soportado: $command")
            }

            if (out.size >= maxOutput) break
        }
        return DecompressResult(out.toByteArray(), i - offset)
    }

    // ---------------------------------------------------------------- compresor

    /**
     * Compresor LC_LZ2 correcto (no óptimo): usa copia directa, relleno de byte y
     * repeticiones. Sirve para probar el decodificador con round-trip y para
     * empaquetar assets propios en este formato.
     */
    fun compress(data: ByteArray): ByteArray {
        val out = GrowableBytes()
        val literals = ArrayList<Int>()

        fun flushLiterals() {
            var start = 0
            while (start < literals.size) {
                val take = minOf(1024, literals.size - start)
                writeHeader(out, CMD_DIRECT_COPY, take)
                for (k in 0 until take) out.add(literals[start + k])
                start += take
            }
            literals.clear()
        }

        var pos = 0
        while (pos < data.size) {
            // 1) ¿Hay una repetición útil hacia atrás?
            val match = longestMatch(data, pos)
            if (match != null && match.second >= 4) {
                flushLiterals()
                var remaining = match.second
                var src = match.first
                while (remaining > 0) {
                    val take = minOf(1024, remaining)
                    writeHeader(out, CMD_REPEAT, take)
                    if (REPEAT_OFFSET_BIG_ENDIAN) { out.add((src ushr 8) and 0xFF); out.add(src and 0xFF) }
                    else { out.add(src and 0xFF); out.add((src ushr 8) and 0xFF) }
                    src += take; remaining -= take
                }
                pos += match.second
                continue
            }
            // 2) ¿Una racha del mismo byte?
            var runLen = 1
            while (pos + runLen < data.size && data[pos + runLen] == data[pos]) runLen++
            if (runLen >= 3) {
                flushLiterals()
                var remaining = runLen
                while (remaining > 0) {
                    val take = minOf(1024, remaining)
                    writeHeader(out, CMD_BYTE_FILL, take)
                    out.add(data[pos].toInt() and 0xFF)
                    remaining -= take
                }
                pos += runLen
                continue
            }
            // 3) Literal suelto.
            literals.add(data[pos].toInt() and 0xFF)
            pos++
        }
        flushLiterals()
        out.add(END_MARKER)
        return out.toByteArray()
    }

    /** Cabecera para [command] con [length] bytes de salida (1..1024). */
    private fun writeHeader(out: GrowableBytes, command: Int, length: Int) {
        val l = length - 1
        if (l < 0x20) {
            out.add((command shl 5) or l)
        } else {
            out.add((CMD_LONG shl 5) or (command shl 2) or ((l ushr 8) and 0x03))
            out.add(l and 0xFF)
        }
    }

    /** Busca el mayor emparejamiento hacia atrás (src, len) para la posición [pos]. */
    private fun longestMatch(data: ByteArray, pos: Int): Pair<Int, Int>? {
        var bestSrc = -1
        var bestLen = 0
        val maxLen = minOf(1024, data.size - pos)
        var src = 0
        while (src < pos) {
            var len = 0
            while (len < maxLen && data[src + len] == data[pos + len]) len++
            if (len > bestLen) { bestLen = len; bestSrc = src }
            src++
        }
        return if (bestLen > 0) bestSrc to bestLen else null
    }
}

/** Buffer de bytes con crecimiento y acceso aleatorio (para las repeticiones LZ). */
internal class GrowableBytes(initial: Int = 1024) {
    private var buf = ByteArray(initial)
    var size = 0
        private set

    fun add(value: Int) {
        if (size == buf.size) buf = buf.copyOf(buf.size * 2)
        buf[size++] = value.toByte()
    }

    operator fun get(index: Int): Int = buf[index].toInt() and 0xFF

    fun toByteArray(): ByteArray = buf.copyOf(size)
}
