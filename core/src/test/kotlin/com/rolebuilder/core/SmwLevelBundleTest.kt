package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelBundle
import com.rolebuilder.core.snes.SnesDecoder
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Test ROM-free del bundle: sin datos de SMW válidos no revienta y devuelve null.
 * El enlazado real de sub-niveles se valida fuera de línea contra la ROM.
 */
class SmwLevelBundleTest {
    @Test
    fun `sin datos SMW devuelve null sin reventar`() {
        val rom = ByteArray(0x80000)
        val header = SnesDecoder.parseHeader(rom)
        assertNull(SmwLevelBundle.extract(rom, header, 0x106))
    }
}
