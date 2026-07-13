package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwMusicRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests ROM-free del renderer de música (S-DSP + secuenciador N-SPC portados). Con una
 * ARAM sintética no hay canción, pero el motor debe CONSTRUIRSE y RENDERIZAR sin
 * reventar y con el formato correcto (estéreo intercalado). El audio real de una
 * canción de SMW se valida fuera de línea contra la ROM.
 */
class SmwMusicRendererTest {

    @Test
    fun `exige una ARAM de 64 KiB`() {
        assertFailsWith<IllegalArgumentException> { SmwMusicRenderer(ByteArray(1024)) }
    }

    @Test
    fun `construye y renderiza silencio sin reventar`() {
        val r = SmwMusicRenderer(ByteArray(0x10000))
        r.selectSong(1)
        val pcm = r.render(1) // 1 segundo
        // Estéreo intercalado: 2 shorts por muestra a SAMPLE_RATE.
        assertTrue(pcm.isNotEmpty())
        assertEquals(0, pcm.size % 2, "debe ser estéreo intercalado")
    }

    @Test
    fun `renderInto respeta el numero de muestras pedido`() {
        val r = SmwMusicRenderer(ByteArray(0x10000))
        val out = ShortArray(256 * 2)
        val written = r.renderInto(out, 0, 256)
        assertEquals(256 * 2, written, "escribe 2 shorts por muestra")
    }
}
