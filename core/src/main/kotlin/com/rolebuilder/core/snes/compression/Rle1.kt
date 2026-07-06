package com.rolebuilder.core.snes.compression

/**
 * Códec **LC_RLE1** (el RLE de Lunar Compress que usa Super Mario World para los
 * **fondos de Layer 2**). Es el formato de los datos apuntados por la tabla de
 * Layer 2 ($05E600) cuando el byte de banco es 0xFF (fondo, banco $0C).
 *
 * Formato: un flujo de comandos de 1 byte. `L = C & 0x7F` (se emiten L+1 bytes):
 *  - bit7 == 0 (Copia directa): copia los siguientes L+1 bytes literales.
 *  - bit7 == 1 (RLE): el siguiente 1 byte se repite L+1 veces.
 *  - Comando 0xFF: fin del flujo (los fondos vanilla terminan en `FF FF`; basta
 *    con parar en el primer 0xFF, el segundo es relleno).
 *
 * Ojo: la salida descomprimida de un fondo NO son teselas 8×8 ni palabras de
 * tilemap, sino **índices de bloque Map16 de 8 bits**; hace falta un paso más de
 * indirección contra la tabla Map16 para obtener las palabras `[tile#][YXPCCCTT]`.
 * Este códec solo se ocupa de la descompresión.
 *
 * El compresor incluido no es óptimo pero produce flujos que el decodificador
 * reconstruye (round-trip de tests). La validación contra una ROM real se hace
 * visualmente con la coherencia de las sub-paletas del fondo resultante.
 */
object Rle1 : SnesDecompressor {

    override val name: String get() = "LC_RLE1 (fondos de Layer 2)"

    /** Comando reservado como fin de flujo. */
    private const val END_MARKER = 0xFF

    override fun decompress(rom: ByteArray, offset: Int, maxOutput: Int): DecompressResult {
        if (offset < 0 || offset >= rom.size) throw DecompressException("Offset fuera de la ROM")

        val out = GrowableBytes()
        var i = offset

        fun readByte(): Int {
            if (i >= rom.size) throw DecompressException("Fin inesperado del flujo RLE1")
            return rom[i++].toInt() and 0xFF
        }

        while (true) {
            if (i >= rom.size) throw DecompressException("No se encontró el marcador de fin (0xFF)")
            val command = rom[i].toInt() and 0xFF
            if (command == END_MARKER) { i++; break }
            i++

            val length = (command and 0x7F) + 1
            if (command and 0x80 == 0) {
                // Copia directa: L+1 bytes literales.
                repeat(length) { out.add(readByte()) }
            } else {
                // RLE: un byte repetido L+1 veces.
                val b = readByte()
                repeat(length) { out.add(b) }
            }

            if (out.size >= maxOutput) break
        }
        return DecompressResult(out.toByteArray(), i - offset)
    }

    // ---------------------------------------------------------------- compresor

    /**
     * Compresor RLE1 (no óptimo): rachas del mismo byte como RLE, el resto como
     * copia directa. Sirve para el round-trip de tests. La longitud por comando se
     * limita a 127 (no 128) para que ninguna cabecera coincida con el fin 0xFF.
     */
    fun compress(data: ByteArray): ByteArray {
        val out = GrowableBytes()
        var pos = 0
        while (pos < data.size) {
            // Racha del mismo byte (máx. 127 para no chocar con 0xFF).
            var run = 1
            while (pos + run < data.size && data[pos + run] == data[pos] && run < 127) run++
            if (run >= 2) {
                out.add(0x80 or (run - 1))
                out.add(data[pos].toInt() and 0xFF)
                pos += run
                continue
            }
            // Literales sueltos hasta que empiece una racha o se llene el bloque.
            val lits = ArrayList<Int>()
            while (pos < data.size && lits.size < 128) {
                if (pos + 1 < data.size && data[pos] == data[pos + 1]) break
                lits.add(data[pos].toInt() and 0xFF); pos++
            }
            out.add((lits.size - 1) and 0x7F)
            for (b in lits) out.add(b)
        }
        out.add(END_MARKER)
        return out.toByteArray()
    }
}
