package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Lógica del diálogo de importación de ROMs que **antes vivía dentro del propio
 * diálogo**, entre Composables, donde ninguna suite podía mirarla: `SnesImportDialog.kt`
 * tenía 1.788 líneas y solo 2 `@Composable`. Al bajarla a `:core` deja de necesitar un
 * dispositivo para probarse, y esto es lo que se gana.
 *
 * Nada de esto necesita la ROM del usuario: son cuentas puras, así que corre en CI.
 */
class SnesPreviewTest {

    // ---- parseOffset --------------------------------------------------------
    // El usuario TECLEA aquí, así que el contrato importante no es el caso feliz
    // sino que ningún estado intermedio de la escritura reviente el diálogo.

    @Test
    fun `lee un offset decimal`() {
        assertEquals(1024, SnesPreview.parseOffset("1024"))
    }

    @Test
    fun `lee un offset hexadecimal con prefijo 0x`() {
        assertEquals(0x2000, SnesPreview.parseOffset("0x2000"))
    }

    @Test
    fun `el prefijo hexadecimal vale en mayusculas`() {
        assertEquals(0xAB, SnesPreview.parseOffset("0XAB"))
    }

    @Test
    fun `ignora los espacios de alrededor`() {
        assertEquals(0x2000, SnesPreview.parseOffset("  0x2000  "))
    }

    @Test
    fun `un 0x a medias vale 0 y no revienta`() {
        // Estado real al teclear "0x2000": pasa por "0x" antes de ser válido.
        assertEquals(0, SnesPreview.parseOffset("0x"))
    }

    @Test
    fun `el texto vacio o sin sentido vale 0`() {
        assertEquals(0, SnesPreview.parseOffset(""))
        assertEquals(0, SnesPreview.parseOffset("   "))
        assertEquals(0, SnesPreview.parseOffset("no soy un numero"))
        assertEquals(0, SnesPreview.parseOffset("0xZZ"))
    }

    @Test
    fun `un offset negativo se recorta a 0`() {
        // Un offset negativo indexaría fuera de la ROM: mejor 0 que una excepción
        // a mitad de dibujar la previsualización.
        assertEquals(0, SnesPreview.parseOffset("-1"))
        assertEquals(0, SnesPreview.parseOffset("-99999"))
    }

    @Test
    fun `un numero enorme no desborda a negativo`() {
        // toIntOrNull devuelve null al desbordar, y el coerce lo deja en 0: lo que
        // no puede pasar es que salga un offset negativo por dar la vuelta.
        assertTrue(SnesPreview.parseOffset("999999999999") >= 0)
        assertTrue(SnesPreview.parseOffset("0xFFFFFFFFFF") >= 0)
    }

    // ---- defaultColorPalette ------------------------------------------------

    @Test
    fun `la paleta de respaldo tiene exactamente los colores pedidos`() {
        // El formato manda: 2bpp son 4 colores y 4bpp son 16. Si la paleta no trae
        // tantos, el decodificador indexa fuera al pintar.
        assertEquals(4, SnesPreview.defaultColorPalette(4).size)
        assertEquals(16, SnesPreview.defaultColorPalette(16).size)
        assertEquals(256, SnesPreview.defaultColorPalette(256).size)
    }

    @Test
    fun `los primeros colores son los vivos y el resto negro opaco`() {
        val paleta = SnesPreview.defaultColorPalette(20)
        val vivos = SnesPreview.VIVID_16

        repeat(vivos.size) { i ->
            assertEquals(vivos[i], paleta[i], "el color $i debería ser el vivo de la tabla")
        }
        for (i in vivos.size until paleta.size) {
            assertEquals(0xFF000000.toInt(), paleta[i], "el relleno debe ser negro OPACO")
        }
    }

    @Test
    fun `solo el indice 0 es transparente y el resto opaco`() {
        // Convención de SNES: el color 0 de una paleta es el transparente, y de ahí
        // sale el fondo de las teselas. Los demás tienen que ser OPACOS: un alfa a 0
        // por descuido pinta teselas invisibles y parece que la extracción falló,
        // cuando lo que está mal es la paleta.
        val paleta = SnesPreview.defaultColorPalette(16)

        assertEquals(0, (paleta[0] ushr 24) and 0xFF, "el índice 0 debe ser transparente")
        val opacosEsperados = paleta.drop(1)
        val noOpacos = opacosEsperados.filter { (it ushr 24) and 0xFF != 0xFF }
        assertTrue(noOpacos.isEmpty(), "hay colores no opacos fuera del índice 0")
    }

    @Test
    fun `pedir cero colores da una paleta vacia sin reventar`() {
        assertEquals(0, SnesPreview.defaultColorPalette(0).size)
    }
}
