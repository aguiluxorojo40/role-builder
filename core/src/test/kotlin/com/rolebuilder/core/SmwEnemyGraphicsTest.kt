package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun `la tanda de los mas puestos en toda la ROM tiene dibujo propio`() {
        // Elegidos por lo que de verdad pesa: colocaciones en niveles jugables de la ROM
        // entera, no por lo llamativos que sean. Entre los cuatro suman 132 colocaciones que
        // hasta ahora salian como rectangulo.
        val esperados = mapOf(
            0xC4 to "plataforma gris que se cae (35 colocaciones)",
            0xA5 to "Sparky/Fuzzy (34)",
            0x9E to "bola con cadena (44)",
            0x70 to "Pokey (19)",
        )
        for ((id, que) in esperados) {
            assertTrue(id in SmwEnemyGraphics.customEnemyIds, "falta el dibujo propio de $que (0x%02X)".format(id))
        }
    }

    @Test
    fun `la Super Koopa que vuela no usa el fotograma de andar`() {
        // El catalogo usaba el fotograma 0 para todas, y el 0 es el de ANDAR: la voladora no
        // anda nunca, cruza la pantalla. Su estado fija `spr_table1602 = 2` o `3`
        // (SprXXX_SuperKoopas_02EBF8, $02:EBF8), que es la pose con la capa extendida; el 0 es
        // el de la de suelo mientras corre ($02:EBB5 con r0 distinto de cero). Se nota al
        // mirarlo: con el 0 la voladora sale con la capa recogida bajo el cuerpo.
        val suelo = SmwEnemyGraphics.customEnemyTilesForTest(0x73, ajuste = 5)
        val vuela = SmwEnemyGraphics.customEnemyTilesForTest(0x72, ajuste = 5)
        assertNotNull(suelo, "falta la Super Koopa de suelo")
        assertNotNull(vuela, "falta la Super Koopa voladora")
        assertEquals(listOf(0xc8, 0xd8, 0xd0, 0xe0), suelo.map { it.tile }, "la de suelo es el fotograma 0")
        assertEquals(listOf(0xe4, 0xe5, 0xf2, 0xe0), vuela.map { it.tile }, "la voladora es el fotograma 2")
    }

    @Test
    fun `las Super Koopa no heredan del nivel la pagina de tesela`() {
        // Su rutina ($02:ECDE) tiene dos ramas y NINGUNA la coge de $166E: las teselas de CAPA
        // van por `(Prop|v4) & ~2`, que deja el bit 0 a 1 (pagina 1 fija), y el resto por
        // `r5 | Prop` con `r5 = spr_table15f6[k] & 0xE` — y ese `& 0xE` BORRA el bit 0 del
        // valor del nivel. Heredarla leia la mitad equivocada de la VRAM de sprites y las
        // dibujaba como una mancha naranja.
        for (id in intArrayOf(0x71, 0x72, 0x73)) {
            val t = SmwEnemyGraphics.customEnemyTilesForTest(id, ajuste = 5)
            assertNotNull(t, "falta la Super Koopa 0x%02X".format(id))
            assertTrue(t.all { it.page != null }, "0x%02X fija la pagina en TODAS sus teselas".format(id))
        }
        // Y la capa de la 0x71 no lleva la misma paleta que las otras dos: su `v4` es 8 en vez
        // de 4, por la comparacion `if (spr_spriteid >= 0x72) v4 = 4`.
        val capa71 = SmwEnemyGraphics.customEnemyTilesForTest(0x71, ajuste = 5)!!.first().palRow
        val capa72 = SmwEnemyGraphics.customEnemyTilesForTest(0x72, ajuste = 5)!!.first().palRow
        assertEquals((8 + 4) * 16, capa71, "la 0x71 va con la paleta 4")
        assertEquals((8 + 2) * 16, capa72, "de la 0x72 en adelante, con la 2")
    }

    @Test
    fun `todos los Chuck que salen en la ROM comparten el mismo fotograma`() {
        // La tabla de sprites manda TODO el rango 0x91-0x98 a la misma rutina de dibujo, y la
        // cabeza y el cuerpo salen de tablas indexadas por el estado de la ANIMACION, no por
        // el id: por eso el fotograma del 0x91 vale para sus hermanos. El 0x96 se queda fuera
        // aposta, que no esta puesto en ningun nivel y no habria donde comprobarlo.
        val chucks = intArrayOf(0x91, 0x92, 0x93, 0x94, 0x95, 0x97, 0x98)
        val base = SmwEnemyGraphics.customEnemyTilesForTest(0x91, ajuste = 1)
        assertNotNull(base, "falta el Chuck de referencia")
        for (id in chucks) {
            val t = SmwEnemyGraphics.customEnemyTilesForTest(id, ajuste = 1)
            assertNotNull(t, "falta el Chuck 0x%02X".format(id))
            assertEquals(base.map { it.tile }, t.map { it.tile }, "0x%02X usa el mismo dibujo".format(id))
        }
        assertNull(SmwEnemyGraphics.customEnemyTilesForTest(0x96, ajuste = 1),
            "el 0x96 no sale en ningun nivel: no se cataloga lo que no se puede comprobar")
    }

    @Test
    fun `el Thwomp es simetrico y en reposo NO lleva la cara de enfado`() {
        // Su tabla ($01, Prop = {0x03,0x43,0x03,0x43}) dice que las teselas de la DERECHA son
        // las de la izquierda volteadas: el 0x40 es el volteo. Y la QUINTA tesela de la tabla
        // (0xC8) no entra, porque el bucle arranca en el indice 3 y solo llega al 4 cuando
        // `spr_table1528` no es cero, o sea cuando el Thwomp ya se ha lanzado.
        val t = SmwEnemyGraphics.customEnemyTilesForTest(0x26, ajuste = 1)
        assertNotNull(t, "falta el Thwomp")
        assertEquals(4, t.size, "en reposo son cuatro teselas, sin la cara de enfado")
        assertFalse(t.any { it.tile == 0xC8 }, "la cara de enfado es de cuando cae, no de reposo")
        assertEquals(listOf(false, true, false, true), t.map { it.xflip }, "la mitad derecha va volteada")
        assertEquals(listOf(0x8E, 0x8E, 0xAE, 0xAE), t.map { it.tile }, "arriba 0x8E y abajo 0xAE")
    }

    @Test
    fun `Sparky cambia de teselas segun el banco de GFX del nivel`() {
        // No es un matiz: su rutina ($02:BE4E) MIRA el ajuste de GFX de sprites del nivel y
        // con el 2 usa la otra forma. Dando por hecha la rama comun, la tesela 0x0A caia
        // encima del grafico de la BOTA y Sparky se dibujaba como un zapato. Aqui se fija que
        // las dos ramas existen y son DISTINTAS; el aspecto de cada una se comprobo mirando
        // los PNG (negro con boca roja en los niveles de ajuste 2, chispa naranja en el resto).
        val a = SmwEnemyGraphics.customEnemyTilesForTest(0xA5, ajuste = 1)
        val b = SmwEnemyGraphics.customEnemyTilesForTest(0xA5, ajuste = 2)
        assertNotNull(a, "falta la forma comun de Sparky")
        assertNotNull(b, "falta la forma del ajuste 2")
        assertEquals(listOf(0x0A), a.map { it.tile }, "la forma comun es la tesela 0x0A")
        assertEquals(listOf(0xC8), b.map { it.tile }, "con ajuste 2 la rutina usa la 0xC8")
    }

    @Test
    fun `los que fijan su propia propiedad OAM no heredan la pagina del nivel`() {
        // `oam[64].flags = sprites_tile_priority | 3` lleva las DOS cosas: el bit 0 es el
        // noveno bit del nº de tesela y los bits 1-3 la paleta. Si se toma solo la paleta y la
        // pagina se saca de $166E, se pinta la otra mitad de la VRAM de sprites: basura con
        // forma de tesela. La plataforma gris (flags 3), el Pokey (5) y la bola (0x33) fijan
        // pagina 1 en todas sus teselas.
        for ((id, que) in mapOf(0xC4 to "plataforma gris", 0x70 to "Pokey", 0x9E to "bola con cadena")) {
            val tiles = SmwEnemyGraphics.customEnemyTilesForTest(id, ajuste = 1)
            assertNotNull(tiles, "falta $que")
            assertTrue(tiles.all { it.page == 1 }, "$que fija la pagina 1 en su propiedad OAM")
        }
    }

    @Test
    fun `el Pokey lleva la cabeza arriba y cuatro cuerpos debajo`() {
        // El nº de tesela sale de si el segmento de ENCIMA sigue vivo ($02, bucle de dibujo):
        // si lo esta va cuerpo (0xE8) y si no, CABEZA (0x8A). Con los cinco segmentos vivos
        // eso deja la cabeza arriba del todo. Invertirlo dibuja un Pokey decapitado con la
        // cara en el suelo, que es exactamente el fallo facil de cometer al portar el bucle,
        // porque en el juego va de abajo a arriba.
        val t = SmwEnemyGraphics.customEnemyTilesForTest(0x70, ajuste = 1)
        assertNotNull(t, "falta el Pokey")
        assertEquals(5, t.size, "cinco segmentos")
        assertEquals(0x8A, t.first().tile, "la cabeza va arriba")
        assertTrue(t.drop(1).all { it.tile == 0xE8 }, "los otros cuatro son cuerpo")
        assertEquals(listOf(0, 16, 32, 48, 64), t.map { it.dy }, "16 px por segmento, de arriba abajo")
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
