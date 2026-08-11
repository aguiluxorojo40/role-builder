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
 * SONDA DESECHABLE de la tanda 2: mira los candidatos ANTES de catalogarlos, cada uno en un
 * nivel donde está puesto de verdad (el banco de GFX y la CGRAM son del nivel), y con las
 * teselas escritas a mano tal y como salen de su rutina. Se salta sola sin `SMW_ROM`.
 *
 * Los layouts van AQUÍ y no en el catálogo a propósito: primero se mira, y solo lo que se
 * reconoce pasa a [SmwEnemyGraphics]. Lo que salga basura se dice y se deja fuera.
 */
class ZzTanda2Probe {

    private val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/tanda2")

    @Test
    fun `mirar los candidatos de la tanda 2`() {
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
        for (id in intArrayOf(0x5D, 0xA4, 0x15, 0x18, 0x47, 0x8A, 0x8C, 0x55, 0x59, 0x5A, 0x4D)) {
            val prop = beh[id].b166e and 0x0F
            println("id %02X  \$166E&0F=%X pagina=%d paleta=%d  niveles=%s".format(
                id, prop, prop and 1, (prop shr 1) and 7,
                donde[id]?.take(8)?.joinToString { "%03X".format(it) } ?: "NINGUNO"))
        }

        // ---- 0x5D: plataforma DIAGONAL (NormalSpritePlatformDraw, $01:B2D1, rama DATA_01B2C3=1)
        // Cinco teselas 16x16: x = 0,8,16,24,32 y las PARES arriba / IMPARES 16 px mas abajo.
        // Teselas DiagPlatTiles[0..4] y luego la rutina PISA las dos ultimas (0xCB y 0xE4).
        // Pagina y paleta SI se heredan de $166E (`flags = spr_table15f6 | prio`).
        val diag = listOf(
            OamTile(0xcb, 0, 0), OamTile(0xe4, 8, 16), OamTile(0xcc, 16, 0, xflip = true),
            OamTile(0xe4, 24, 16, xflip = true), OamTile(0xcb, 32, 0, xflip = true),
        )
        mira(rom, header, donde, 0x5D, diag, null, "diag")

        // ---- 0xA4: BOLA DE PINCHOS (SprXXX_BuoyantPlatformsAndMine_SpikeBallDraw, $01:B666).
        // Una tesela (0xAA) espejada en los cuatro cuadrantes. Prop {0x31,0x71,0xa1,0xf1}:
        // pagina 1 y paleta 0 en las cuatro; el 0x40/0x80 son los volteos.
        val bola = listOf(
            OamTile(0xAA, 0, 0, page = 1),
            OamTile(0xAA, 16, 0, page = 1, xflip = true),
            OamTile(0xAA, 0, 16, page = 1, vflip = true),
            OamTile(0xAA, 16, 16, page = 1, xflip = true, vflip = true),
        )
        mira(rom, header, donde, 0xA4, bola, (8 + 0) * 16, "bola")

        // ---- 0x8A: PAJARO (Spr08A_Bird_Draw, $02:F3EA). UNA tesela de 8x8
        // (`sprites_oamtile_size_buffer[...] = 0`), Tiles[1602] con 1602 = 0 volando.
        // flags = Direction[157c] | Palette[k & 3]: pagina 1 fija, y la PALETA depende del
        // SLOT del sprite, no del nivel. Aqui se prueban las cuatro.
        for ((i, pal) in intArrayOf(0x8, 0x4, 0x6, 0xa).withIndex()) {
            mira(rom, header, donde, 0x8A, listOf(OamTile(0xd2, 0, 0, size16 = false, page = 1)),
                (8 + ((pal shr 1) and 7)) * 16, "pajaro_slot$i")
        }
        // Y sus cinco fotogramas con la paleta del slot 0.
        for ((i, t) in intArrayOf(0xd2, 0xd3, 0xd0, 0xd1, 0x9b).withIndex()) {
            mira(rom, header, donde, 0x8A, listOf(OamTile(t, 0, 0, size16 = false, page = 1)),
                (8 + 4) * 16, "pajaro_f$i")
        }

        // ---- 0x8C: CHIMENEA (Spr08C_SideExitAndFireplace_Draw, $02:F4EB). DOS teselas 8x8
        // apiladas (ypos −80 y −72). flags = 53 = 0x35: pagina 1 y paleta (0x35>>1)&7 = 2.
        for ((i, par) in listOf(0xd4 to 0xbb, 0xab to 0x9a).withIndex()) {
            mira(rom, header, donde, 0x8C, listOf(
                OamTile(par.first, 0, 0, size16 = false, page = 1),
                OamTile(par.second, 0, 8, size16 = false, page = 1),
            ), (8 + 2) * 16, "chimenea_f$i")
        }

        // ---- Cheep-Cheeps por la via GENERICA. El 0x18 pasa por
        // SprXXX_FixedMovementCheepCheep_01B10A ($01:B10A), que FUERZA el bit de pagina:
        // `spr_table15f6 = ((1602 >> 1) ^ 1) | (15f6 & 0xFE)` -> con 1602 = 0/1 la pagina es 1.
        for (id in intArrayOf(0x15, 0x16, 0x18, 0x47)) {
            for (lv in donde[id].orEmpty().take(3)) {
                guarda(SmwEnemyGraphics.genericImageUnchecked(rom, header, lv, id), "%02X_gen_%03X".format(id, lv), 8)
            }
        }
        // El 0x18 con la pagina FORZADA a 1, que es lo que hace su rutina, y con la del nivel.
        for (pag in 0..1) {
            for (lv in donde[0x18].orEmpty().take(2)) {
                guarda(SmwEnemyGraphics.customSprite(rom, header, lv, 0x18,
                    listOf(OamTile(0x84, 0, 0, page = pag))), "18_pag%d_%03X".format(pag, lv), 8)
            }
        }

        // ---- la tesela del Cheep-Cheep es 0x67, un numero IMPAR, que en un bloque 16x16 es
        // raro: para verlo se vuelca la tira de bloques de esa zona (pagina 1) y se compara.
        for (lv in intArrayOf(0x102, 0x00A)) {
            val tira = (0 until 12).map { OamTile(0x60 + it, it * 18, 0, page = 1) }
            guarda(SmwEnemyGraphics.customSprite(rom, header, lv, 0x15, tira, (8 + 2) * 16),
                "cheep_tira_%03X".format(lv), 6)
        }

        // Y los tres bloques candidatos, uno a uno y grandes, para no decidirlo entrecerrando
        // los ojos sobre una tira.
        for (t in intArrayOf(0x66, 0x67, 0x68, 0x69)) {
            guarda(SmwEnemyGraphics.customSprite(rom, header, 0x00A, 0x15,
                listOf(OamTile(t, 0, 0, page = 1)), (8 + 2) * 16), "cheep_bloque_%02X".format(t), 16)
        }

        // El BLANCO de un PNG con alfa y el BLANCO de la paleta se ven igual en un visor. Para
        // saber si el cuerpo del pez es blanco de verdad o es hueco, se pinta ENCIMA de un
        // bloque solido (0x60, una de las barras naranjas de al lado): lo que sea transparente
        // dejara ver el naranja.
        for (t in intArrayOf(0x67, 0x69)) {
            guarda(SmwEnemyGraphics.customSprite(rom, header, 0x00A, 0x15,
                listOf(OamTile(0x60, 0, 0, page = 1), OamTile(t, 0, 0, page = 1)), (8 + 2) * 16),
                "cheep_sobre_fondo_%02X".format(t), 16)
        }

        // ---- las plataformas YA catalogadas cuya rutina hay que revisar (0x55, 0x59, 0x5A):
        // el despacho las manda a Spr058_VerticalRockPlatform y a TurnBlockBridge, no a la
        // plataforma plana. Se vuelca lo que hay catalogado para compararlo con el nivel.
        for (id in intArrayOf(0x55, 0x59, 0x5A, 0x5F, 0x57)) {
            val lv = donde[id]?.firstOrNull() ?: continue
            guarda(SmwEnemyGraphics.customEnemyImage(rom, header, lv, id), "%02X_catalogo_%03X".format(id, lv), 4)
        }

        // PUENTE DE BLOQUES GIRATORIOS (0x59/0x5A), SprXXX_TurnBlockBridge_Draw ($01:B710):
        // CINCO teselas 16x16, TODAS la 64 = 0x40, y `flags = sprites_tile_priority` a secas
        // -> pagina 0 y paleta 0, sin heredar nada. Totalmente extendido (spr_table151c =
        // BlkBridgeLength = 0x20) quedan en -32,-16,0,+16,+32.
        val puente = (0..4).map { OamTile(0x40, it * 16, 0, page = 0) }
        for (id in intArrayOf(0x59, 0x5A)) mira(rom, header, donde, id, puente, (8 + 0) * 16, "puente_%02X".format(id))

        // PLATAFORMA DE CADENA (0x5F), Spr05F_BrownChainedPlatform ($01:C773): la plataforma
        // son CUATRO teselas {0x60,0x61,0x61,0x62} en XDisp {-32,-16,0,+16} con flags 49 =
        // 0x31 -> pagina 1, paleta 0. Los eslabones (0xA2) van aparte y en circulo.
        val cadena = listOf(0x60, 0x61, 0x61, 0x62).mapIndexed { i, t -> OamTile(t, i * 16, 0, page = 1) }
        mira(rom, header, donde, 0x5F, cadena, (8 + 0) * 16, "cadena")
        mira(rom, header, donde, 0x5F, cadena + OamTile(0xA2, 16, -20, page = 1), null, "cadena_conancla")

        // 0x55: su Init es el MISMO que el del 0x57 (Spr057_VerticalCheckerboardPlatform_Init,
        // $01:B25E, que hace ++spr_table1602), asi que deberia ser la plataforma ANCHA de 5
        // teselas y no la estrecha de 3. Se pintan las dos para elegir mirando.
        val ancha = listOf(0xEA, 0xEB, 0xEB, 0xEB, 0xEC).mapIndexed { i, t -> OamTile(t, i * 16, 0) }
        val estrecha = listOf(0x60, 0x61, 0x62).mapIndexed { i, t -> OamTile(t, i * 16, 0) }
        mira(rom, header, donde, 0x55, ancha, null, "55_ancha")
        mira(rom, header, donde, 0x55, estrecha, null, "55_estrecha")
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
