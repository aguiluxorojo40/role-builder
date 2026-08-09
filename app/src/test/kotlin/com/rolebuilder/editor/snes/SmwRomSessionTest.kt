package com.rolebuilder.editor.snes

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * La ROM de la sesión. El fallo que arregla esto no era un error en pantalla sino una
 * fricción: cargabas la ROM, entrabas en AVANZADO, salías, y volver a entrar te obligaba
 * a buscar el fichero otra vez en el selector. Tres pantallas la cargaban por su cuenta
 * y ninguna la compartía.
 */
class SmwRomSessionTest {

    private val tmp: File = Files.createTempDirectory("smwrom").toFile()

    @Before fun limpiaAntes() = SmwRomSession.forgetAt(tmp)
    @After fun limpia() { SmwRomSession.forgetAt(tmp); tmp.deleteRecursively() }

    private val rom = ByteArray(2048) { (it and 0xFF).toByte() }

    @Test
    fun `sin cargar nada no hay ROM`() {
        assertFalse(SmwRomSession.isAvailableAt(tmp))
        assertNull(SmwRomSession.getAt(tmp))
    }

    @Test
    fun `una vez cargada sigue disponible sin volver a pedirla`() {
        SmwRomSession.rememberAt(tmp, rom, "Super Mario World.sfc")
        assertTrue(SmwRomSession.isAvailableAt(tmp))
        assertArrayEquals(rom, SmwRomSession.getAt(tmp))
        assertEquals("Super Mario World.sfc", SmwRomSession.displayName)
    }

    /**
     * EL CASO QUE MOTIVA TODO ESTO: aunque se pierda lo que hubiera en memoria, la ROM
     * tiene que seguir ahí sin volver a pedírsela al usuario.
     */
    @Test
    fun `sobrevive a perder la memoria porque queda en disco`() {
        SmwRomSession.rememberAt(tmp, rom, "mi_rom.sfc")
        // Simula un arranque nuevo: misma carpeta, sin nada cacheado.
        vaciarCache()
        assertTrue("debería encontrarla en disco", SmwRomSession.isAvailableAt(tmp))
        assertArrayEquals(rom, SmwRomSession.getAt(tmp))
        assertEquals("el nombre también se recupera", "mi_rom.sfc", SmwRomSession.displayName)
    }

    @Test
    fun `quitarla la borra de verdad`() {
        SmwRomSession.rememberAt(tmp, rom, "x.sfc")
        SmwRomSession.forgetAt(tmp)
        assertFalse(SmwRomSession.isAvailableAt(tmp))
        assertNull(SmwRomSession.getAt(tmp))
        assertEquals("", SmwRomSession.displayName)
    }

    @Test
    fun `da un fichero para las pantallas que reciben una ruta`() {
        SmwRomSession.rememberAt(tmp, rom, "x.sfc")
        val f = SmwRomSession.asFileAt(tmp)
        assertTrue("debería existir el fichero", f != null && f.isFile)
        assertArrayEquals(rom, f!!.readBytes())
    }

    @Test
    fun `cambiar de ROM reemplaza la anterior`() {
        SmwRomSession.rememberAt(tmp, rom, "vieja.sfc")
        val otra = ByteArray(1024) { 0x5A }
        SmwRomSession.rememberAt(tmp, otra, "nueva.sfc")
        vaciarCache()
        assertArrayEquals("debe quedar la nueva, no la vieja", otra, SmwRomSession.getAt(tmp))
        assertEquals("nueva.sfc", SmwRomSession.displayName)
    }

    /** Tira la copia en memoria SIN tocar el disco, para simular un arranque en frío. */
    private fun vaciarCache() = SmwRomSession.dropMemoryCacheForTests()
}
