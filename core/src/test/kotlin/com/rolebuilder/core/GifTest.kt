package com.rolebuilder.core

import com.rolebuilder.core.snes.Gif
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trip del codificador [Gif]: codifica fotogramas conocidos y los vuelve a leer con
 * el lector de GIF de la plataforma (ImageIO), comprobando píxel a píxel. Así verificamos
 * que el flujo "LZW no comprimido" es GIF89a válido de verdad (no solo "parece").
 */
class GifTest {

    private fun frame(w: Int, h: Int, f: (Int, Int) -> Int): IntArray =
        IntArray(w * h) { val x = it % w; val y = it / w; f(x, y) or (0xFF shl 24) }

    @Test
    fun roundTripsMultiFrameGif() {
        val w = 37; val h = 23
        // Tres fotogramas con patrones distintos y muchos colores (banda diagonal).
        // ≤255 colores opacos (el índice 0 del GIF queda para transparente): 64 tonos.
        val frames = (0 until 3).map { fi ->
            Gif.Frame(frame(w, h) { x, y -> ((x * 7 + y * 13 + fi * 50) and 0x3F).let { it or (it shl 8) or (it shl 16) } }, delayCs = 5)
        }
        val bytes = Gif.encode(w, h, frames)
        assertTrue(bytes.size > 100, "GIF no vacío")
        assertEquals('G'.code.toByte(), bytes[0])
        assertEquals('8'.code.toByte(), bytes[3])

        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        assertEquals(3, reader.getNumImages(true), "3 fotogramas")
        for (fi in 0 until 3) {
            val img = reader.read(fi)
            assertEquals(w, img.width); assertEquals(h, img.height)
            for (y in 0 until h) for (x in 0 until w) {
                val expected = frames[fi].pixels[y * w + x] and 0xFFFFFF
                val got = img.getRGB(x, y) and 0xFFFFFF
                assertEquals(expected, got, "píxel ($x,$y) fotograma $fi")
            }
        }
    }

    @Test
    fun encodesManyColors() {
        // 200 colores distintos (bajo el tope de 255 opacos): no revienta y round-trip de muestra.
        val w = 20; val h = 10
        val px = IntArray(w * h) { (it % 200).let { v -> (0xFF shl 24) or (v shl 16) or (v shl 8) or v } }
        val bytes = Gif.encode(w, h, listOf(Gif.Frame(px)))
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        val img = reader.read(0)
        assertEquals(px[0] and 0xFFFFFF, img.getRGB(0, 0) and 0xFFFFFF)
        assertEquals(px[150] and 0xFFFFFF, img.getRGB(10, 7) and 0xFFFFFF)
    }
}
