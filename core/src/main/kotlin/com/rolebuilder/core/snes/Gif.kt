package com.rolebuilder.core.snes

import java.io.ByteArrayOutputStream

/**
 * Codificador de GIF89a animado (bucle infinito), autocontenido y sin dependencias. Los
 * assets extraídos de la ROM se exportan como PNG (estático, vía [ArgbImage]) y GIF
 * (animado): las ANIMACIONES del juego —agua del overworld, brillo de monedas/marcadores,
 * enemigos— se entregan como GIF, tal como especificamos.
 *
 * Usa la técnica de "LZW no comprimido": tamaño de código fijo = bits de paleta + 1, con un
 * código CLEAR periódico para que el diccionario nunca crezca. Es válido GIF89a estándar
 * (lo leen todos los visores) y evita el codificador LZW completo, que es la parte frágil.
 *
 * Todos los fotogramas comparten UNA paleta global (≤256 colores). Como los assets de SNES
 * salen de una CGRAM de ≤256 colores, encaja de sobra; si un fotograma trajera más colores,
 * [encode] lanza una excepción clara en vez de corromper el GIF.
 */
object Gif {

    /** Un fotograma: píxeles ARGB (fila a fila, `width*height`) y su duración en centésimas de s. */
    class Frame(val pixels: IntArray, val delayCs: Int = 8)

    /**
     * Codifica [frames] (todos [width]×[height]) como un GIF89a animado en bucle. El color
     * ARGB con alfa 0 se trata como TRANSPARENTE (índice de transparencia del GIF).
     */
    fun encode(width: Int, height: Int, frames: List<Frame>): ByteArray {
        require(frames.isNotEmpty()) { "GIF sin fotogramas" }
        // Paleta global: colores distintos de TODOS los fotogramas. El transparente (alfa 0)
        // ocupa el índice 0 reservado.
        val colorToIndex = LinkedHashMap<Int, Int>()
        colorToIndex[TRANSPARENT] = 0 // índice 0 = transparente
        for (f in frames) for (argb in f.pixels) {
            val c = if (argb ushr 24 == 0) TRANSPARENT else (argb or (0xFF shl 24))
            if (c !in colorToIndex) {
                require(colorToIndex.size < 256) { "el GIF admite ≤256 colores, hay más" }
                colorToIndex[c] = colorToIndex.size
            }
        }
        // Tamaño de la GCT: potencia de 2 ≥ nº colores, mínimo 2 (1 bit).
        var gctBits = 1
        while ((1 shl gctBits) < colorToIndex.size) gctBits++
        val gctSize = 1 shl gctBits

        val out = ByteArrayOutputStream()
        fun b(v: Int) = out.write(v and 0xFF)
        fun w(v: Int) { b(v); b(v shr 8) }

        // Header + Logical Screen Descriptor.
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        w(width); w(height)
        b(0xF0 or (gctBits - 1)) // GCT presente, 8 bits/color, tamaño gctBits
        b(0)                     // índice de color de fondo
        b(0)                     // aspect ratio
        // Global Color Table.
        val colors = colorToIndex.keys.toList()
        for (i in 0 until gctSize) {
            val c = colors.getOrElse(i) { TRANSPARENT }
            b(c shr 16); b(c shr 8); b(c)
        }
        // Application Extension: bucle infinito (NETSCAPE2.0).
        out.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00))

        val minCode = maxOf(2, gctBits)     // ≥2 según la especificación
        for (f in frames) {
            require(f.pixels.size == width * height) { "fotograma de tamaño incorrecto" }
            // Graphic Control Extension: retardo + índice transparente 0.
            out.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, 0x01))
            w(f.delayCs); b(0); b(0)
            // Image Descriptor.
            b(0x2C); w(0); w(0); w(width); w(height); b(0)
            // Datos LZW "no comprimido".
            b(minCode)
            val indices = IntArray(f.pixels.size) { i ->
                val argb = f.pixels[i]
                colorToIndex[if (argb ushr 24 == 0) TRANSPARENT else (argb or (0xFF shl 24))]!!
            }
            writeUncompressedLzw(out, indices, minCode)
        }
        b(0x3B) // trailer
        return out.toByteArray()
    }

    /** Empaqueta [indices] como flujo LZW de ancho fijo (minCode+1 bits) con CLEAR periódico. */
    private fun writeUncompressedLzw(out: ByteArrayOutputStream, indices: IntArray, minCode: Int) {
        val clear = 1 shl minCode
        val eoi = clear + 1
        val codeSize = minCode + 1 // nunca crece: reiniciamos con CLEAR antes de agotar
        val buffer = ByteArrayOutputStream()
        var bitBuf = 0
        var bitCnt = 0
        fun emit(code: Int) {
            bitBuf = bitBuf or (code shl bitCnt)
            bitCnt += codeSize
            while (bitCnt >= 8) { buffer.write(bitBuf and 0xFF); bitBuf = bitBuf ushr 8; bitCnt -= 8 }
        }
        // Reiniciamos el diccionario cada vez que el siguiente código superaría (1<<codeSize)-1.
        // Con codeSize = minCode+1, el diccionario arranca en eoi+1 y crece 1 por símbolo hasta
        // el límite; emitimos CLEAR antes de llegar y así el ancho de código no cambia nunca.
        val maxEntries = (1 shl codeSize)
        var next = eoi + 1
        emit(clear)
        for (idx in indices) {
            if (next >= maxEntries - 1) { emit(clear); next = eoi + 1 }
            emit(idx)
            next++
        }
        emit(eoi)
        if (bitCnt > 0) buffer.write(bitBuf and 0xFF)
        // Sub-bloques de ≤255 bytes.
        val data = buffer.toByteArray()
        var p = 0
        while (p < data.size) {
            val n = minOf(255, data.size - p)
            out.write(n)
            out.write(data, p, n)
            p += n
        }
        out.write(0) // terminador de bloque
    }

    private const val TRANSPARENT = 0
}
