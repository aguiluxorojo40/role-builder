package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests ROM-free del catálogo curado de gráficos de enemigos. El renderizado real
 * (de la ROM) se valida fuera de línea; aquí se comprueba el contrato del API.
 */
class SmwEnemyGraphicsTest {

    @Test
    fun `cubre los enemigos curados y no otros`() {
        for (id in intArrayOf(0x00, 0x01, 0x02, 0x03, 0x10)) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir 0x${id.toString(16)}")
            assertTrue(SmwEnemyGraphics.nameOf(id) != null)
        }
        assertFalse(SmwEnemyGraphics.handles(0x55))
        assertNull(SmwEnemyGraphics.nameOf(0x55))
    }

    @Test
    fun `nombres de los enemigos curados`() {
        // 0x00-0x03 son los Koopa SIN caparazón; los que lo llevan son 0x04-0x07.
        assertEquals("Koopa sin caparazon verde", SmwEnemyGraphics.nameOf(0x00))
        assertEquals("Koopa sin caparazon rojo", SmwEnemyGraphics.nameOf(0x01))
        assertEquals("Koopa sin caparazon azul", SmwEnemyGraphics.nameOf(0x02))
        assertEquals("Koopa sin caparazon amarillo", SmwEnemyGraphics.nameOf(0x03))
        assertEquals("Koopa verde", SmwEnemyGraphics.nameOf(0x04))
        assertEquals("Koopa rojo", SmwEnemyGraphics.nameOf(0x05))
        assertEquals("Goomba volador", SmwEnemyGraphics.nameOf(0x10))
        assertEquals("Bullet Bill", SmwEnemyGraphics.nameOf(0x1C))
        assertEquals("Boo", SmwEnemyGraphics.nameOf(0x37))
    }

    @Test
    fun `los cuatro Koopa CON caparazon estan cubiertos`() {
        // Faltaban 0x04, 0x06 y 0x07: solo estaba el 0x05, que por eso era el único cuyo
        // caparazón se podía volcar de la ROM.
        for (id in 0x04..0x07) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir 0x${id.toString(16)}")
        }
    }

    @Test
    fun `shellImage solo acepta los Koopa que llevan caparazon`() {
        // Sin ROM no se puede pintar, pero el contrato de ids sí se comprueba: los Koopa
        // SIN caparazón (0x00-0x03) y cualquier otro id no tienen caparazón que dibujar.
        val romFalsa = ByteArray(0x8000)
        val hdr = com.rolebuilder.core.snes.SnesDecoder.parseHeader(romFalsa)
        for (id in intArrayOf(0x00, 0x01, 0x02, 0x03, 0x0F, 0x37)) {
            assertNull(SmwEnemyGraphics.shellImage(romFalsa, hdr, 0x105, id),
                "0x${id.toString(16)} no lleva caparazón")
        }
    }

    @Test
    fun `los fotogramas del caparazon son los del juego`() {
        // StunnedShellDraw pinta el 6 (quieto); kKickedShellGFXRt_ShellAniTiles = {6,7,8,7}.
        assertEquals(6, SmwEnemyGraphics.SHELL_FRAME_STILL)
        assertEquals(listOf(6, 7, 8, 7), SmwEnemyGraphics.SHELL_SPIN_FRAMES.toList())
    }

    @Test
    fun `el orden del atlas es estable, la tanda 0 conserva sus 15 fotogramas`() {
        // enemies.png se indexa por posición en curatedIds: si estos 15 se mueven,
        // el atlas horneado queda desincronizado en silencio. Los nuevos ids van
        // SIEMPRE al final (y el atlas se regenera con --enemies).
        val tanda0 = listOf(0x00, 0x01, 0x02, 0x03, 0x05, 0x0F, 0x10, 0x11, 0x1C, 0x29, 0x2A, 0x2C, 0x4B, 0x4D, 0x4E)
        assertEquals(tanda0, SmwEnemyGraphics.curatedIds.take(15))
    }

    @Test
    fun `la tanda 3 anade las Koopas aladas y las marca como aladas y animadas`() {
        for (id in 0x08..0x0B) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir la Parakoopa 0x${id.toString(16)}")
            assertTrue(SmwEnemyGraphics.isWinged(id), "0x${id.toString(16)} es alada")
            assertEquals(SmwEnemyGraphics.ATLAS_FRAMES, SmwEnemyGraphics.animFrameCount(id))
        }
        // Una Koopa CON caparazón normal no es alada, pero sí anima el andar.
        assertFalse(SmwEnemyGraphics.isWinged(0x05))
        assertEquals(SmwEnemyGraphics.ATLAS_FRAMES, SmwEnemyGraphics.animFrameCount(0x05))
        // Un id fuera de la familia andadora no anima (1 fotograma).
        assertFalse(SmwEnemyGraphics.isWinged(0x2C))
        assertEquals(1, SmwEnemyGraphics.animFrameCount(0x2C))
    }

    @Test
    fun `los enemigos con 2o fotograma real animan y los de rutina propia no`() {
        // Verificados renderizando ambos fotogramas desde la ROM (2º fotograma REAL).
        for (id in intArrayOf(0x15, 0x16, 0x2E, 0x31, 0x37, 0x38, 0x39, 0x3D, 0x4D, 0x4E)) {
            assertEquals(SmwEnemyGraphics.ATLAS_FRAMES, SmwEnemyGraphics.animFrameCount(id),
                "0x${id.toString(16)} debería animar")
        }
        // Su 2º byte OAM daría basura (animación de rutina propia): quedan estáticos.
        for (id in intArrayOf(0x1C, 0x29, 0x2C, 0x4B)) {
            assertEquals(1, SmwEnemyGraphics.animFrameCount(id), "0x${id.toString(16)} es estático")
        }
    }

    @Test
    fun `las tres Plantas Pirana estan catalogadas y animan`() {
        // Los tres tipos: de tubo (0x1A/0x2A), saltarina (0x4F) y saltarina de fuego (0x50).
        for (id in intArrayOf(0x1A, 0x2A, 0x4F, 0x50)) {
            assertTrue(SmwEnemyGraphics.handles(id), "0x${id.toString(16)} está catalogada")
            assertEquals(SmwEnemyGraphics.ATLAS_FRAMES, SmwEnemyGraphics.animFrameCount(id),
                "0x${id.toString(16)} anima")
        }
        // Solo las saltarinas llevan tallo/hojas (dibujo de 2 partes).
        assertTrue(SmwEnemyGraphics.isJumpingPiranha(0x4F))
        assertTrue(SmwEnemyGraphics.isJumpingPiranha(0x50))
        assertFalse(SmwEnemyGraphics.isJumpingPiranha(0x1A))
        assertFalse(SmwEnemyGraphics.isJumpingPiranha(0x2A))
    }

    @Test
    fun `la tanda 1 esta cubierta y los descartados no`() {
        for (id in intArrayOf(0x4F, 0x37, 0x3D, 0x15, 0x16, 0x2E, 0x38, 0x39, 0x31)) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir 0x${id.toString(16)}")
        }
        // Descartados tras verificación visual: su entrada de la tabla genérica no es
        // su aspecto real (rutina de dibujo propia; salían fuentes o basura).
        for (id in intArrayOf(0x33, 0x30, 0x32)) {
            assertFalse(SmwEnemyGraphics.handles(id), "0x${id.toString(16)} está descartado")
        }
    }

    @Test
    fun `los que faltaban para los primeros niveles tienen dibujo propio`() {
        // Todos estos tienen id >= 0x54, o sea fuera de kGenericSpriteOAMData_TilesOffset
        // (84 entradas): por la via generica NUNCA podian salir. Si alguno se cae de aqui,
        // vuelve a aparecer como rectangulo en YOSHI'S ISLAND 1/2/3.
        val esperados = mapOf(
            0x9F to "Banzai Bill",
            0x91 to "Chargin' Chuck",
            0x95 to "Clappin' Chuck",
            0xB9 to "caja de mensaje",
            0xBD to "Koopa desnudo deslizandose",
            0x83 to "bloque volador",
        )
        for ((id, que) in esperados) {
            assertTrue(id in SmwEnemyGraphics.customEnemyIds, "falta el dibujo propio de $que (0x%02X)".format(id))
        }
    }

    @Test
    fun `el agujero de warp y la seta invisible NO se dibujan, y eso es correcto`() {
        // Sus rutinas ($02:EADA y $03:C30F) no ponen ni una tesela en el OAM. Marcarlos
        // evita contarlos para siempre como huecos y acabar inventandoles un dibujo.
        assertTrue(SmwEnemyGraphics.isIntentionallyInvisible(0x8E), "el agujero de warp no dibuja")
        assertTrue(SmwEnemyGraphics.isIntentionallyInvisible(0xC7), "la seta invisible no dibuja")
        // Y no vale marcar de invisible a cualquiera para que cuadren las cuentas.
        for (id in intArrayOf(0x9F, 0x91, 0xB9, 0x83, 0x00, 0xAB)) {
            assertFalse(SmwEnemyGraphics.isIntentionallyInvisible(id), "0x%02X si se ve".format(id))
        }
    }

    @Test
    fun `la estrella y el 1-Up son sprites colocables, no solo del HUD`() {
        // La via de powerups del proyecto solo cubria seta/flor/pluma (los tres de la hoja
        // del HUD). La estrella y el 1-Up se colocan en el nivel como cualquier otro
        // sprite y nadie los dibujaba.
        assertTrue(0x76 in SmwEnemyGraphics.customEnemyIds, "falta la estrella (0x76)")
        assertTrue(0x78 in SmwEnemyGraphics.customEnemyIds, "falta el 1-Up (0x78)")
    }

    @Test
    fun `el Podoboo recibe sus graficos por DMA y no se puede sacar del nivel`() {
        // Sus teselas (06/06/16/16) caen en la zona que UploadPlayerGFX reescribe cada
        // fotograma, asi que dibujarlo desde el tileset estatico da letras. Marcarlo evita
        // que alguien "arregle" la tabla a base de prueba y error.
        assertTrue(SmwEnemyGraphics.hasDynamicGraphics(0x33), "el Podoboo es de graficos dinamicos")
        // Y NO vale meter ahi a los que se descartaron por otra razon: el 0x30 y el 0x32
        // fallan porque comparten rutina con el Bony Beetle, no por el DMA.
        for (id in intArrayOf(0x30, 0x32, 0x31, 0x00, 0x76)) {
            assertFalse(SmwEnemyGraphics.hasDynamicGraphics(id), "0x%02X no es dinamico".format(id))
        }
    }
}
