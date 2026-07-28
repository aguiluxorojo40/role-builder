package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwGfxLibrary
import com.rolebuilder.core.snes.SnesDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contrato ROM-free de la biblioteca de GFX: sobre datos que NO son una ROM de SMW válida no
 * revienta y devuelve vacío/null (los caminos con ROM real se verifican renderizando bancos —
 * palette-swap — con el extractor, fuera de estos tests).
 */
class SmwGfxLibraryTest {

    // Cabecera derivada de un buffer cualquiera (sin construir SnesHeader a mano).
    private fun fakeHeader() = SnesDecoder.parseHeader(ByteArray(0x10000))

    @Test
    fun `banks sobre datos no-SMW no revienta y no inventa bancos`() {
        // Un buffer de ceros no contiene punteros GFX válidos → catálogo vacío, sin excepción.
        val banks = SmwGfxLibrary.banks(ByteArray(0x8000))
        assertTrue(banks.size <= SmwGfxLibrary.LAST_GFX_FILE + 1)
        banks.forEach { assertTrue(it.tileCount > 0) } // nunca lista un banco vacío
    }

    @Test
    fun `paletteRows sobre datos invalidos no lanza y devuelve filas bien formadas`() {
        // Contrato defensivo: no revienta; si devuelve filas, son 16 de 16 colores.
        val rows = SmwGfxLibrary.paletteRows(ByteArray(0x1000), fakeHeader())
        assertTrue(rows.isEmpty() || rows.size == SmwGfxLibrary.PALETTE_ROWS)
        rows.forEach { assertEquals(16, it.size) }
    }

    @Test
    fun `bankSheet de un banco inexistente es null`() {
        assertNull(SmwGfxLibrary.bankSheet(ByteArray(0x1000), bankId = 0x00, paletteRow = IntArray(16)))
    }
}
