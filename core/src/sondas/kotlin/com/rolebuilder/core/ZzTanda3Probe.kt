package com.rolebuilder.core

import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwEnemyGraphics.OamTile
import com.rolebuilder.core.snes.SmwSpriteBehaviorReader
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE de la tanda 3: los tres huecos MÁS GRANDES que quedaban por colocaciones
 * —0x9D burbuja (54), 0x68 Fuzzy de guía (37) y 0xBA plataforma con cuenta atrás (34)—,
 * dibujados con las teselas de su rutina y en un nivel donde están puestos de verdad.
 * Primero se MIRA, y solo lo que se reconoce pasa al catálogo. Se salta sola sin `SMW_ROM`.
 */
class ZzTanda3Probe {

    private val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/tanda3")

    @Test
    fun `mirar los tres huecos grandes`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        out.mkdirs()

        val donde = HashMap<Int, MutableList<Int>>()
        for (lv in 0 until 0x200) {
            val puestos = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv) }.getOrNull() ?: continue
            for (id in puestos.map { it.first }.distinct()) donde.getOrPut(id) { ArrayList() }.add(lv)
        }
        val beh = SmwSpriteBehaviorReader.read(rom, header)!!
        for (id in intArrayOf(0x9D, 0x68, 0xBA, 0x35)) {
            val prop = beh[id].b166e and 0x0F
            println("id %02X  \$166E&0F=%X pagina=%d paleta=%d  niveles=%s".format(
                id, prop, prop and 1, (prop shr 1) and 7,
                donde[id]?.take(8)?.joinToString { "%03X".format(it) } ?: "NINGUNO"))
        }

        // ---- 0x9D BURBUJA CON SPRITE DENTRO (Spr09D_BubbleWithSprite, $02:D8BB;
        // dibujo de la burbuja en $02:D9D6). Son DOS cosas superpuestas:
        //  · la burbuja: 4 teselas 16x16, todas la 0xA0, espejadas en los cuatro cuadrantes
        //    (Prop {0x07,0x47,0x87,0xC7} -> pagina 1, paleta 3), mas un 8x8 (0x99, Prop 0x03)
        //    que es el brillo. XDisp/YDisp del fotograma 0 (r2 = DATA_02D9D2[0] = 0).
        //  · lo que lleva DENTRO: la rutina llama a GenericGFXRtDraw1Tile16x16 y acto seguido
        //    PISA charnum y flags con BubbleSprTiles1[00c2] / BubbleSprGfxProp1[00c2].
        val burbuja = listOf(
            OamTile(0xA0, -8, -10, page = 1),
            OamTile(0xA0, 8, -10, page = 1, xflip = true),
            OamTile(0xA0, -8, 2, page = 1, vflip = true),
            OamTile(0xA0, 8, 2, page = 1, xflip = true, vflip = true),
        )
        val brillo = OamTile(0x99, -1, -4, size16 = false, page = 1, palRow = (8 + 1) * 16)
        mira(rom, header, donde, 0x9D, burbuja, (8 + 3) * 16, "burbuja_sola")
        mira(rom, header, donde, 0x9D, burbuja + brillo, (8 + 3) * 16, "burbuja_conbrillo")
        // Los cuatro contenidos posibles (00c2 = (xpos >> 4) & 3), cada uno con su Prop.
        val dentroTiles = intArrayOf(0xA8, 0xCA, 0x67, 0x24)
        val dentroProp = intArrayOf(0x84, 0x85, 0x05, 0x08)
        for (i in 0..3) {
            val pr = dentroProp[i]
            val dentro = OamTile(dentroTiles[i], 0, 0, page = pr and 1,
                vflip = (pr and 0x80) != 0, xflip = (pr and 0x40) != 0,
                palRow = (8 + ((pr shr 1) and 7)) * 16)
            mira(rom, header, donde, 0x9D, listOf(dentro), null, "burbuja_dentro$i")
            mira(rom, header, donde, 0x9D, burbuja + brillo + dentro, (8 + 3) * 16, "burbuja_completa$i")
        }

        // ---- 0x68 FUZZY de guía (SprXXX_LineGuided_LineFuzzyPlats, $01:D74A, rama
        // `v1 == 104` -> SprXXX_LineGuided_01DBD4, $01:DBD4): UNA tesela 16x16, charnum
        // −56 = 0xC8 y flags = prio | DATA_01DC09[frame] con DATA = {0x05, 0x45} -> pagina 1,
        // paleta 2, y el 0x40 del segundo es el volteo del parpadeo.
        mira(rom, header, donde, 0x68, listOf(OamTile(0xC8, 0, 0, page = 1)), (8 + 2) * 16, "fuzzy")

        // ---- 0xBA PLATAFORMA CON CUENTA ATRÁS (Spr0BA_TimedPlatform_Draw, $03:8E12):
        // dos teselas 16x16 (la misma, 0xC4, la segunda en espejo) mas el DÍGITO en 8x8.
        // El digito sale de NumberTiles[spr_table1570 >> 6] = {0xB6,0xB5,0xB4,0xB3}; el Init
        // ($01:8326) pone 1570 = 0xFF o 63 segun `xpos & 0x10`, o sea indice 3 o 0.
        // Prop {0x0B, 0x4B, 0x0B} -> pagina 1, paleta 5, y el 0x40 es el espejo.
        for ((i, dig) in intArrayOf(0xB3, 0xB6).withIndex()) {
            mira(rom, header, donde, 0xBA, listOf(
                OamTile(0xC4, 0, 0, page = 1),
                OamTile(0xC4, 16, 0, page = 1, xflip = true),
                OamTile(dig, 12, 4, size16 = false, page = 1),
            ), (8 + 5) * 16, "plataforma_digito$i")
        }
        // Y los cuatro digitos sueltos, para saber cual es cual.
        for (t in intArrayOf(0xB3, 0xB4, 0xB5, 0xB6)) {
            mira(rom, header, donde, 0xBA, listOf(OamTile(t, 0, 0, size16 = false, page = 1)),
                (8 + 5) * 16, "digito_%02X".format(t))
        }
    }

    private fun mira(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader,
                     donde: Map<Int, List<Int>>, id: Int, tiles: List<OamTile>, palRow: Int?, nombre: String) {
        val niveles = donde[id].orEmpty().take(3)
        if (niveles.isEmpty()) { println("$nombre: 0x%02X no esta puesto en ningun nivel".format(id)); return }
        for (lv in niveles) {
            guarda(SmwEnemyGraphics.customSprite(rom, header, lv, id, tiles, palRow), "%s_%03X".format(nombre, lv), 8)
        }
    }

    private fun guarda(img: ArgbImage?, name: String, zoom: Int) {
        if (img == null) { println("$name -> SIN IMAGEN"); return }
        val bi = BufferedImage(img.width * zoom, img.height * zoom, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until img.height * zoom) for (x in 0 until img.width * zoom) {
            bi.setRGB(x, y, img.get(x / zoom, y / zoom))
        }
        ImageIO.write(bi, "png", File(out, "$name.png"))
        println("$name -> ${img.width}x${img.height}")
    }
}
