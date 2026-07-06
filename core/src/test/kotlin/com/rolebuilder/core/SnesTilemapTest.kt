package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesTilemap
import com.rolebuilder.core.snes.SnesTilemap.TilemapEntry
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnesTilemapTest {

    // ---------------------------------------------------------------- utilidades

    /** Empaqueta entradas de tilemap a bytes little-endian en [dest] desde [offset]. */
    private fun writeEntries(dest: ByteArray, offset: Int, entries: List<TilemapEntry>) {
        for ((i, e) in entries.withIndex()) {
            val word = e.toWord()
            dest[offset + 2 * i] = (word and 0xFF).toByte()
            dest[offset + 2 * i + 1] = ((word shr 8) and 0xFF).toByte()
        }
    }

    private fun entry(tile: Int, pal: Int, prio: Boolean = false, h: Boolean = false, v: Boolean = false) =
        TilemapEntry(tile, pal, prio, h, v)

    /** CGRAM sintética: la fila p tiene 16 colores marcados con p en el canal rojo. */
    private fun syntheticCgram(): IntArray = IntArray(SnesTilemap.CGRAM_ROWS * SnesTilemap.COLORS_PER_ROW) { i ->
        val row = i / SnesTilemap.COLORS_PER_ROW
        val col = i % SnesTilemap.COLORS_PER_ROW
        (0xFF shl 24) or (row shl 16) or (col shl 8) or 0
    }

    // -------------------------------------------------------------------- parser

    @Test
    fun `decodeEntry desempaqueta todos los campos del layout SNES`() {
        // tile=0x123, paleta=5 (101), prioridad=1, hFlip=0, vFlip=1
        val word = 0x123 or (5 shl 10) or 0x2000 or 0x8000
        val e = SnesTilemap.decodeEntry(word)
        assertEquals(0x123, e.tileIndex)
        assertEquals(5, e.palette)
        assertTrue(e.priority)
        assertFalse(e.hFlip)
        assertTrue(e.vFlip)
        // Round-trip: reempaquetar reproduce la palabra original.
        assertEquals(word, e.toWord())
    }

    @Test
    fun `parseTilemap lee little-endian y respeta el numero de entradas`() {
        val src = listOf(
            entry(tile = 0x001, pal = 0),
            entry(tile = 0x2AA, pal = 7, h = true),
            entry(tile = 0x000, pal = 3, prio = true),
        )
        val rom = ByteArray(2 * src.size + 4)
        writeEntries(rom, 2, src) // con offset para probar que no lee desde 0
        val parsed = SnesTilemap.parseTilemap(rom, 2, src.size)
        assertEquals(src, parsed)
    }

    @Test
    fun `parseTilemap rellena con cero fuera de la ROM sin reventar`() {
        val rom = ByteArray(4) // solo 2 entradas caben
        writeEntries(rom, 0, listOf(entry(0x0FF, 2), entry(0x055, 1)))
        val parsed = SnesTilemap.parseTilemap(rom, 0, 4) // pedimos 4
        assertEquals(4, parsed.size)
        assertEquals(0x0FF, parsed[0].tileIndex)
        // Las dos que se salen: palabra 0 → tile 0, paleta 0.
        assertEquals(entry(0, 0), parsed[2])
        assertEquals(entry(0, 0), parsed[3])
    }

    // ------------------------------------------------------------- asignación real

    @Test
    fun `asigna a cada tile su fila real y por eso dos tiles iguales de la hoja difieren`() {
        // El fondo usa el tile 10 con la paleta 2 y el tile 20 con la paleta 5.
        // Es justo la corrección del problema: misma hoja, sub-paletas distintas.
        val entries = listOf(
            entry(10, 2), entry(20, 5), entry(10, 2), entry(20, 5),
        )
        val cgram = syntheticCgram()
        val byPal = SnesTilemap.assignedPaletteByTile(entries)
        assertEquals(mapOf(10 to 2, 20 to 5), byPal)

        val colors = SnesTilemap.assignPalettesByTile(entries, cgram)
        // La fila p está marcada con p en el canal rojo de la CGRAM sintética.
        assertEquals(2, (colors.getValue(10)[0] shr 16) and 0xFF)
        assertEquals(5, (colors.getValue(20)[0] shr 16) and 0xFF)
        // Los 16 colores devueltos son exactamente la fila 2 de la CGRAM.
        assertTrue(colors.getValue(10).contentEquals(SnesTilemap.paletteRow(cgram, 2)))
    }

    @Test
    fun `ante filas contradictorias para un tile gana la mayoritaria y es determinista`() {
        // El tile 7 aparece 3 veces con paleta 4 y 1 vez con paleta 1 → gana la 4.
        val entries = listOf(
            entry(7, 4), entry(7, 1), entry(7, 4), entry(7, 4),
        )
        assertEquals(mapOf(7 to 4), SnesTilemap.assignedPaletteByTile(entries))
    }

    @Test
    fun `tileRange filtra referencias fuera de la hoja extraida`() {
        val entries = listOf(entry(5, 1), entry(300, 6), entry(8, 1))
        val byPal = SnesTilemap.assignedPaletteByTile(entries, tileRange = 0..15)
        assertEquals(mapOf(5 to 1, 8 to 1), byPal) // el tile 300 se ignora
    }

    @Test
    fun `paletteRow extrae la fila correcta y rellena fuera de rango`() {
        val cgram = syntheticCgram()
        val row3 = SnesTilemap.paletteRow(cgram, 3)
        assertEquals(16, row3.size)
        assertEquals(3, (row3[0] shr 16) and 0xFF)
        // Una fila que se sale de una CGRAM corta se rellena con negro opaco.
        val short = IntArray(16)
        val filled = SnesTilemap.paletteRow(short, 5)
        assertTrue(filled.all { it == 0xFF000000.toInt() })
    }

    // ----------------------------------------------------------- detector heurístico

    /**
     * Construye un tilemap "de fondo real" determinista: [entryCount] entradas que
     * referencian tiles del [tileRange] usando solo unas pocas paletas, con hueco
     * (tile 0) intercalado, tal como se vería un fondo sin comprimir.
     */
    private fun backgroundBytes(tileRange: IntRange, entryCount: Int, palettes: IntArray): ByteArray {
        val bytes = ByteArray(2 * entryCount)
        val rangeSize = tileRange.last - tileRange.first + 1
        val entries = ArrayList<TilemapEntry>(entryCount)
        for (i in 0 until entryCount) {
            // Patrón repetitivo/continuo típico de un fondo (no aleatorio).
            val tile = if (i % 7 == 0) 0 else tileRange.first + (i % rangeSize)
            val pal = palettes[(i / 5) % palettes.size]
            entries.add(entry(tile, pal))
        }
        writeEntries(bytes, 0, entries)
        return bytes
    }

    @Test
    fun `detecta un tilemap real embebido entre relleno`() {
        val range = 0..63
        val map = backgroundBytes(range, entryCount = 200, palettes = intArrayOf(1, 2))
        // Relleno "no-tilemap" a ambos lados: tiles fuera de rango (bit alto puesto)
        // para que no formen runs aceptables.
        val padWord = 0x0200 or (7 shl 10) // tile 512 (fuera de 0..63), paleta 7
        val pad = ByteArray(2 * 40)
        for (i in 0 until 40) {
            pad[2 * i] = (padWord and 0xFF).toByte()
            pad[2 * i + 1] = ((padWord shr 8) and 0xFF).toByte()
        }
        val rom = pad + map + pad

        val regions = SnesTilemap.detectTilemapRegions(rom, range, minEntries = 32)
        assertTrue(regions.isNotEmpty(), "debería encontrar el tilemap embebido")
        val best = regions.first()
        assertEquals(pad.size, best.offset, "el run debería empezar justo tras el relleno")
        assertEquals(200, best.entryCount)
        assertTrue(best.distinctPalettes <= 2, "usa pocas sub-paletas: ${best.distinctPalettes}")
        assertTrue(best.score > 0.3, "un fondo real debería puntuar alto: ${best.score}")
    }

    @Test
    fun `descarta ruido aleatorio (no confunde datos con un tilemap)`() {
        // Bytes pseudoaleatorios deterministas: los tiles (10 bits) rara vez caen en
        // un rango acotado muchas veces seguidas, así que no se forman runs largos.
        val rnd = Random(seed = 0xC0FFEE)
        val noise = ByteArray(4096) { rnd.nextInt(256).toByte() }
        val regions = SnesTilemap.detectTilemapRegions(noise, tileRange = 0..63, minEntries = 32)
        assertTrue(
            regions.isEmpty(),
            "el ruido no debería dar tilemaps de 32+ entradas en un rango de 64 tiles: $regions",
        )
    }

    @Test
    fun `un run que usa las 8 sub-paletas al azar se descarta y el de pocas gana`() {
        // Segunda señal: aunque un bloque referencie tiles del rango de forma
        // continua (forma run), si reparte las 8 filas de CGRAM al azar NO es un
        // fondo real y debe caer por el tope de paletas. En paralelo, un fondo de
        // pocas filas sí se acepta. Los separamos con relleno fuera de rango.
        val range = 0..63
        // Bloque de "paletas de ruido": tiles en rango pero las 8 filas cicladas.
        val noisyPal = ByteArray(2 * 100)
        writeEntries(noisyPal, 0, List(100) { entry(1 + it % 63, it % 8) })
        // Bloque de fondo real: una sola fila.
        val real = backgroundBytes(range, entryCount = 100, palettes = intArrayOf(3))
        // Separador fuera de rango para cortar los runs.
        val padWord = 0x0200 or (7 shl 10)
        val pad = ByteArray(2 * 40)
        for (i in 0 until 40) {
            pad[2 * i] = (padWord and 0xFF).toByte()
            pad[2 * i + 1] = ((padWord shr 8) and 0xFF).toByte()
        }
        val rom = pad + noisyPal + pad + real + pad

        val regions = SnesTilemap.detectTilemapRegions(rom, range, minEntries = 32, maxDistinctPalettes = 4)
        // El bloque de 8 paletas queda descartado; el de una sola fila, aceptado.
        assertTrue(regions.all { it.distinctPalettes <= 4 }, "no debe colar el run de 8 paletas: $regions")
        assertEquals(1, regions.size, "solo el fondo real debería sobrevivir: $regions")
        assertEquals(pad.size + noisyPal.size + pad.size, regions.first().offset)
        assertEquals(1, regions.first().distinctPalettes)
    }

    @Test
    fun `un bloque de un solo tile o todo hueco no cuenta como tilemap`() {
        // 100 entradas todas al tile 5, paleta 1: sin variedad, no es un mapa útil.
        val entries = List(100) { entry(5, 1) }
        val bytes = ByteArray(2 * entries.size)
        writeEntries(bytes, 0, entries)
        val regions = SnesTilemap.detectTilemapRegions(bytes, tileRange = 0..63, minEntries = 32)
        assertTrue(regions.isEmpty(), "un único tile repetido no es un fondo que colorear")

        // Todo hueco (tile 0) tampoco.
        val blanks = ByteArray(200)
        assertTrue(SnesTilemap.detectTilemapRegions(blanks, 0..63, minEntries = 32).isEmpty())
    }
}
