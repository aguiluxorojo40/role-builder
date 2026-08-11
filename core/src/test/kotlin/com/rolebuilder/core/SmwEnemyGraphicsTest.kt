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
    fun `la tanda 1 esta cubierta, y los tres que se habian descartado ya no lo estan`() {
        for (id in intArrayOf(0x4F, 0x37, 0x3D, 0x15, 0x16, 0x2E, 0x38, 0x39, 0x31)) {
            assertTrue(SmwEnemyGraphics.handles(id), "debería cubrir 0x${id.toString(16)}")
        }
        // 0x33, 0x30 y 0x32 se descartaron a la vez tras mirarlos ("salían fuentes o basura"),
        // pero por razones DISTINTAS, y las dos tienen arreglo:
        //  · el PODOBOO (0x33) sí es de la tabla genérica; lo que no tenía era la FUENTE de sus
        //    teselas, que llega por DMA desde GFX33 ([SmwEnemyGraphics.DYNAMIC_GFX_SPRITES]).
        //    Ahora está en el catálogo curado como cualquier otro.
        assertTrue(SmwEnemyGraphics.handles(0x33), "el Podoboo ya se dibuja")
        //  · el DRY BONES (0x30/0x32) tiene rutina de dibujo PROPIA —solo el 0x31 Bony Beetle
        //    cae en la genérica—, así que sigue FUERA del catálogo genérico a propósito y
        //    entra por el de dibujos propios. Si algún día apareciera en `handles` sería que
        //    alguien lo ha metido en la tabla genérica, que es justo lo que da basura.
        for (id in intArrayOf(0x30, 0x32)) {
            assertFalse(SmwEnemyGraphics.handles(id), "0x${id.toString(16)} va por dibujo propio")
            assertTrue(id in SmwEnemyGraphics.customEnemyIds, "0x${id.toString(16)} tiene dibujo propio")
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
    fun `el que fija su propiedad OAM a mano fija tambien la pagina en el catalogo`() {
        // REGLA que sale del fallo de las Super Koopa: si la rutina de un enemigo escribe su
        // `flags` con una constante, ahi el bit 0 es el noveno bit del nº de tesela — o sea la
        // PAGINA — y no se puede heredar del nivel. En la ROM de SMW las dos coinciden para
        // todos estos (se comprobo id a id contra $166E, cero discrepancias, y los 37 dibujos
        // salen byte a byte iguales antes y despues de fijarla), pero coincidir no es lo mismo
        // que ser correcto: a la Super Koopa se le acabo la suerte porque su rutina BORRA ese
        // bit, y estuvo dibujandose como una mancha hasta que se miro.
        //
        // Cada id de aqui lleva al lado la tabla de la que sale su propiedad.
        val fijanSuPropiedad = mapOf(
            0xAB to "kSpr0AB_Rex_Prop = {0x47, 0x07}",
            0xC3 to "kSpr0C3_PorcuPuffer_PocruPufferGfxProp = {0x0d, ...}",
            0xAA to "kSpr0AA_Fishbone_Prop = {0x4d, 0xcd, 0x0d, 0x8d}",
            0x9F to "kSpr09F_BanzaiBill_Prop = {0x33, ...}",
            0xA8 to "kSpr0A8_Blargg_Prop = {0x45, 0x05}",
            0x6F to "kSpr06F_DinoTorch_DinoTorchProp = {0x09, 0x05, ...}",
            0x6E to "kSpr06F_DinoTorch_DinoRhinoProp = {0x2f, ...}",
            0xA9 to "kSpr0A9_Reznor_Prop = {0x3f, ...}",
        )
        for ((id, tabla) in fijanSuPropiedad) {
            val t = SmwEnemyGraphics.customEnemyTilesForTest(id, ajuste = 1)
            assertNotNull(t, "falta 0x%02X".format(id))
            assertTrue(t.all { it.page == 1 },
                "0x%02X saca la pagina de su propia propiedad (%s), no del nivel".format(id, tabla))
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
    fun `el Podoboo recibe sus graficos por DMA, y por eso hay que ir a buscarlos a GFX33`() {
        // Sus teselas (06/06/16/16) caen en la zona que UploadPlayerGFX reescribe cada
        // fotograma, asi que dibujarlo desde el tileset ESTATICO del nivel da letras (ahi es
        // donde vive la fuente). Eso lo tuvo fuera del catalogo mucho tiempo, y era medio
        // cierto: la fuente no esta en el fichero GFX del nivel, pero SI se puede reconstruir.
        assertTrue(SmwEnemyGraphics.hasDynamicGraphics(0x33), "el Podoboo es de graficos dinamicos")
        // Y NO vale meter ahi a cualquiera que se descartara por otra razon: el 0x30 y el 0x32
        // fallaban por compartir la tabla generica con el Bony Beetle, no por el DMA.
        for (id in intArrayOf(0x30, 0x32, 0x31, 0x00, 0x76)) {
            assertFalse(SmwEnemyGraphics.hasDynamicGraphics(id), "0x%02X no es dinamico".format(id))
        }
    }

    @Test
    fun `la ranura dinamica 6 del Podoboo apunta a las teselas 72 y 88 de GFX33`() {
        // ESTA es la cuenta que hace posible dibujarlo, y la que hay que poder revisar sin
        // ROM. Sale de dos sitios que se juntan:
        //
        //  · `Spr033_Podoboo` ($01:E093) instala la ranura 6:
        //        graphics_dynamic_sprite_pointers_top_lo[6]    = 0x8600
        //        graphics_dynamic_sprite_pointers_bottom_lo[6] = 0x8800
        //  · `UploadPlayerGFX` ($00:A300) copia 0x40 bytes (= DOS teselas) de cada puntero a
        //        VRAM 0x6000 + 6*0x10  -> teselas 0x06 y 0x07
        //        VRAM 0x6100 + 6*0x10  -> teselas 0x16 y 0x17
        //  · y el buffer fuente es GFX33 expandida a 4bpp en g_ram desde 0x7D00
        //    (`GraphicsDecompressionRoutines_DecompressGFX32And33`, $00:B888), 32 bytes por
        //    tesela: (0x8600 − 0x7D00)/32 = 72 y (0x8800 − 0x7D00)/32 = 88.
        //
        // Equivocarse en la BASE (0x7D00) o en el paso (32) no rompe nada visible en el
        // codigo: simplemente pinta OTRA tesela de GFX33, que es un dibujo entero distinto.
        val mapa = SmwEnemyGraphics.dynamicTileMapForTest(0x33)
        assertNotNull(mapa, "el Podoboo tiene ranura dinamica")
        assertEquals(mapOf(0x06 to 72, 0x07 to 73, 0x16 to 88, 0x17 to 89), mapa)
        // Y las 4 teselas que instala son EXACTAMENTE las 4 de su entrada de la tabla
        // generica (06 06 16 16): si no coincidieran, la ranura elegida no seria la suya.
        assertNull(SmwEnemyGraphics.dynamicTileMapForTest(0x31), "el Bony Beetle no es dinamico")
    }

    @Test
    fun `el Dry Bones no hereda del nivel ni la pagina ni la paleta`() {
        // `Spr030_ThrowingDryBones_03C3DA` ($03:C3DA) escribe su propiedad a mano:
        // `flags = sprites_tile_priority | DryBonesGfxProp[...]`, y esa Prop vale 0x03/0x43.
        // Ese 3 lleva las DOS cosas a la vez: bit 0 = pagina 1 y bits 1-3 = paleta 1. Sacar
        // la pagina de $166E pinta la otra mitad de la VRAM de sprites.
        for (id in intArrayOf(0x30, 0x32)) {
            val t = SmwEnemyGraphics.customEnemyTilesForTest(id, ajuste = 1)
            assertNotNull(t, "falta el Dry Bones 0x%02X".format(id))
            assertEquals(2, t.size, "cabeza + cuerpo, y nada mas (el Tiles[0] es el de tirar el hueso)")
            assertTrue(t.all { it.page == 1 }, "el Dry Bones fija la pagina 1 en su propiedad OAM")
            // La CABEZA (0x64) va ARRIBA y medio bloque a la izquierda del cuerpo (0x66):
            // XDisp[4] = 0xF8 = −8 y YDisp[1] = 0xF0 = −16. Invertirlo deja el craneo en el
            // suelo, que es el error facil al portar un bucle que va de abajo a arriba.
            assertEquals(0x64, t[0].tile, "arriba va el craneo")
            assertEquals(0x66, t[1].tile, "abajo el cuerpo del fotograma 0")
            assertEquals(t[1].dx - 8, t[0].dx, "la cabeza sobresale 8 px a la izquierda")
            assertEquals(t[1].dy - 16, t[0].dy, "y 16 px por encima")
        }
    }

    @Test
    fun `la plataforma 0x55 va ANCHA, igual que la 0x57, porque comparten el Init`() {
        // El nombre de la rutina solo nombra al 0x57 y eso despista: en la tabla de Init del
        // banco $01 (`kUnk_1817d`, indexada por id de sprite) las entradas 0x55 y 0x57 apuntan
        // a la MISMA, `Spr057_VerticalCheckerboardPlatform_Init` ($01:B25E), cuyo cuerpo
        // entero es `++spr_table1602[k]`. Y `NormalSpritePlatformGFXRt_DrawFlatPlatform`
        // ($01:B2DF) mira justo ese valor: con 0 dibuja 3 teselas (0x60, 0x61, 0x62) y llama
        // a FinishOAMWrite con 2; con 1, CINCO (0xEA, 0xEB, 0xEB, 0xEB, 0xEC) y con 4.
        //
        // O sea que el 0x55 con teselas 0x60-0x62 no es "la version corta del mismo sprite":
        // son teselas de OTRO sitio del banco del nivel, la barra gris de siempre.
        val p55 = SmwEnemyGraphics.customEnemyTilesForTest(0x55, ajuste = 1)
        val p57 = SmwEnemyGraphics.customEnemyTilesForTest(0x57, ajuste = 1)
        assertNotNull(p55, "falta la plataforma 0x55")
        assertNotNull(p57, "falta la plataforma 0x57")
        assertEquals(listOf(0xEA, 0xEB, 0xEB, 0xEB, 0xEC), p55.map { it.tile }, "la 0x55 es la ANCHA de 5 teselas")
        assertEquals(p57.map { it.tile }, p55.map { it.tile }, "las dos salen del mismo Init")
        assertEquals(64, p55.last().dx, "y ocupa los 5 bloques de 16 px, no 3")
    }

    @Test
    fun `la burbuja del 0x9D lleva DENTRO un sprite, y sin el no es nada`() {
        // `Spr09D_BubbleWithSprite` ($02:D8BB) monta DOS dibujos: la burbuja de
        // $02:D9D6 —cuatro cuartos de circulo espejados con la MISMA tesela 0xA0, mas un 8x8
        // de brillo— y el bicho que va dentro, que la rutina mete PISANDO el charnum que
        // acababa de poner la generica. Quedarse solo con la burbuja deja un aro vacio, que
        // es lo que sale si uno para de leer en el bucle de dibujo.
        val t = SmwEnemyGraphics.customEnemyTilesForTest(0x9D, ajuste = 1)
        assertNotNull(t, "falta la burbuja")
        assertEquals(6, t.size, "cuatro cuartos de burbuja + brillo + contenido")
        assertEquals(listOf(0xA0, 0xA0, 0xA0, 0xA0), t.take(4).map { it.tile }, "la burbuja es UNA tesela espejada")
        // Los cuatro cuartos: sin volteo, H, V y los dos. Es lo que hace el circulo.
        assertEquals(listOf(false, true, false, true), t.take(4).map { it.xflip })
        assertEquals(listOf(false, false, true, true), t.take(4).map { it.vflip })
        assertTrue(t.take(5).all { it.page == 1 }, "burbuja y brillo van a pagina 1 (Prop 0x07/0x03)")
        // El contenido por defecto es el Goomba, y su Prop 0x84 tiene el bit 0 a CERO: es de
        // los pocos que van a pagina 0 estando el resto del sprite en la 1. Heredarla del
        // vecino pinta otra mitad de la VRAM.
        val dentro = t.last()
        assertEquals(0xA8, dentro.tile, "dentro va el Goomba (BubbleSprTiles1[0])")
        assertEquals(0, dentro.page, "y su Prop 0x84 lo manda a la pagina 0, no a la 1")
        assertTrue(dentro.vflip, "el 0x80 de esa Prop es el volteo vertical")
    }

    @Test
    fun `la plataforma con cuenta atras lleva el DIGITO, y es un 8x8`() {
        // `Spr0BA_TimedPlatform_Draw` ($03:8E12): dos teselas 16x16 (la misma, 0xC4, la
        // segunda en espejo) y una TERCERA de 8x8 con el numero. El tamaño sale de
        // `kSpr0BA_TimedPlatform_TileSize` = {2, 2, 0}, y confundirlo dibuja el digito como
        // bloque de 16x16, o sea el numero mas tres teselas ajenas pegadas.
        val t = SmwEnemyGraphics.customEnemyTilesForTest(0xBA, ajuste = 1)
        assertNotNull(t, "falta la plataforma con cuenta atras")
        assertEquals(3, t.size, "dos mitades + digito")
        assertEquals(listOf(0xC4, 0xC4), t.take(2).map { it.tile }, "la plataforma es UNA tesela espejada")
        assertTrue(t[1].xflip, "la mitad derecha va en espejo (Prop 0x4B)")
        assertTrue(t.take(2).all { it.size16 }, "las dos mitades son de 16x16")
        assertFalse(t[2].size16, "el digito es de 8x8 (TileSize = 0)")
        // NumberTiles = {0xB6, 0xB5, 0xB4, 0xB3} indexado por 1570>>6, y el Init ($01:8326)
        // arranca el contador en 0xFF -> indice 3 -> 0xB3, que renderizado es un CUATRO.
        assertEquals(0xB3, t[2].tile, "nace mostrando el 4")
        assertTrue(t.all { it.page == 1 }, "Prop 0x0B/0x4B: pagina 1 en las tres")
    }

    @Test
    fun `el Fuzzy de guia y el Sparky con ajuste 2 son el mismo bicho`() {
        // `SprXXX_LineGuided_01DBD4` ($01:DBD4) le pone al 0x68 `charnum = -56` = 0xC8 y
        // `flags = prio | DATA_01DC09[v2]` con {0x05, 0x45}: pagina 1 y paleta 2.
        // Y el Sparky (0xA5) en un nivel con ajuste de GFX 2 usa EXACTAMENTE la misma tesela
        // con la misma propiedad (`kSprXXX_WallFollowers_FuzzyProp`), porque alli tambien es
        // un Fuzzy. Si alguna vez dejan de coincidir es que alguien ha tocado uno de los dos
        // sin mirar el otro.
        val fuzzy = SmwEnemyGraphics.customEnemyTilesForTest(0x68, ajuste = 1)
        val sparky2 = SmwEnemyGraphics.customEnemyTilesForTest(0xA5, ajuste = 2)
        assertNotNull(fuzzy, "falta el Fuzzy de guia")
        assertNotNull(sparky2, "falta la variante Fuzzy del Sparky")
        assertEquals(1, fuzzy.size, "una sola tesela 16x16")
        assertEquals(0xC8, fuzzy[0].tile)
        assertEquals(1, fuzzy[0].page, "el 0x05 de su Prop es la pagina 1")
        assertEquals(sparky2.map { it.tile }, fuzzy.map { it.tile }, "misma tesela que el Sparky-Fuzzy")
        assertEquals(sparky2.map { it.page }, fuzzy.map { it.page }, "y misma pagina")
    }
}
