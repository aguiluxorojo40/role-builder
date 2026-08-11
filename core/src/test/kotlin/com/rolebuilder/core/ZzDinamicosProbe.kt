package com.rolebuilder.core

import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSpriteBehaviorReader
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE: MIRAR los tres candidatos de esta tanda —Podoboo (0x33) por la vía de
 * gráficos DINÁMICOS, Dry Bones (0x30/0x32) y Spiny (0x13)— cada uno en niveles donde está
 * puesto de verdad, más la hoja de VRAM del nivel para comprobar en qué ranura cae el dibujo.
 * Se salta sola sin `SMW_ROM`.
 */
class ZzDinamicosProbe {

    private val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/dyn")

    @Test
    fun `mirar podoboo drybones y spiny`() {
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
        for (id in intArrayOf(0x33, 0x30, 0x32, 0x13, 0x31)) {
            val prop = beh[id].b166e and 0x0F
            println("id %02X  \$166E&0F=%X  pagina=%d  paleta=%d  niveles=%s".format(
                id, prop, prop and 1, (prop shr 1) and 7,
                donde[id]?.take(8)?.joinToString { "%03X".format(it) } ?: "NINGUNO"))
        }
        println("mapa dinamico 0x33: " + SmwEnemyGraphics.dynamicTileMapForTest(0x33))

        // Podoboo: su cuadrado de 4 teselas (fila 1 de Prop), en cada nivel donde sale.
        for (lv in donde[0x33].orEmpty().take(6)) {
            save(SmwEnemyGraphics.squareTileImage(rom, header, lv, 0x33), "33_sq_%03X".format(lv), 8)
            save(SmwEnemyGraphics.genericImageUnchecked(rom, header, lv, 0x33), "33_gen_%03X".format(lv), 8)
        }
        // La hoja de VRAM de un nivel con Podoboo, para ver la ranura 0x06/0x16 ocupada.
        donde[0x33]?.firstOrNull()?.let { lv ->
            save(SmwEnemyGraphics.spriteVramSheet(rom, header, lv, 0x33, palRow = (8 + 4) * 16), "33_vram_%03X".format(lv), 2)
        }

        // Spiny 0x13 y Bony Beetle 0x31 (que YA está curado) por la vía genérica.
        for (id in intArrayOf(0x13, 0x31)) {
            for (lv in donde[id].orEmpty().take(5)) {
                save(SmwEnemyGraphics.genericImageUnchecked(rom, header, lv, id), "%02X_gen_%03X".format(id, lv), 8)
            }
        }

        // Dry Bones 0x30/0x32 por su dibujo propio y por la genérica (para comparar).
        for (id in intArrayOf(0x30, 0x32)) {
            for (lv in donde[id].orEmpty().take(5)) {
                save(SmwEnemyGraphics.customEnemyImage(rom, header, lv, id), "%02X_cus_%03X".format(id, lv), 8)
                save(SmwEnemyGraphics.genericImageUnchecked(rom, header, lv, id), "%02X_gen_%03X".format(id, lv), 8)
            }
        }

        // Dry Bones a mano: las DOS direcciones y los DOS fotogramas de andar, para elegir
        // mirando en vez de a ojo de tabla. Fotograma 1 = cuerpo 0x68 y cabeza 1 px mas abajo.
        val lvDb = donde[0x32]?.first() ?: 0x01B
        val poses = mapOf(
            "dir1_f0" to listOf(t(0x64, -8, 0), t(0x66, 0, 16)),
            "dir1_f1" to listOf(t(0x64, -8, 1), t(0x68, 0, 16)),
            "dir0_f0" to listOf(t(0x64, 8, 0, flip = true), t(0x66, 0, 16, flip = true)),
        )
        for ((nombre, tiles) in poses) {
            save(SmwEnemyGraphics.customSprite(rom, header, lvDb, 0x32, tiles, palRow = (8 + 1) * 16),
                "32_$nombre", 16)
        }

        // Hoja de VRAM del nivel del Dry Bones con la paleta 1: para localizar A OJO en que
        // ranura vive el esqueleto y comprobar que 0x164/0x166 son sus teselas.
        save(SmwEnemyGraphics.spriteVramSheet(rom, header, lvDb, 0x32, palRow = (8 + 1) * 16),
            "32_vram_%03X".format(lvDb), 3)

        // Los bloques 16x16 de la zona donde deberia vivir el esqueleto (pagina 1, 0x60..0x6F),
        // uno al lado de otro y etiquetados por su nº de tesela, para MIRAR cual es cual.
        val tira = ArrayList<SmwEnemyGraphics.OamTile>()
        for (i in 0 until 8) tira.add(SmwEnemyGraphics.OamTile(0x60 + 2 * i, i * 18, 0, page = 1))
        for (i in 0 until 8) tira.add(SmwEnemyGraphics.OamTile(0x80 + 2 * i, i * 18, 20, page = 1))
        for (i in 0 until 8) tira.add(SmwEnemyGraphics.OamTile(0xE0 + 2 * i, i * 18, 40, page = 1))
        save(SmwEnemyGraphics.customSprite(rom, header, lvDb, 0x32, tira, palRow = (8 + 1) * 16),
            "32_tira_bloques", 8)

        // LA PRUEBA del DMA: el MISMO bloque 16x16 (teselas 06/07/16/17, pagina 0) del MISMO
        // nivel, pedido por un id dinamico (0x33 -> sale de GFX33) y por uno que no lo es
        // (0x31 -> sale del fichero GFX estatico del nivel). Si la teoria es buena, el
        // segundo es basura/fuente y el primero el Podoboo.
        donde[0x33]?.firstOrNull()?.let { lv ->
            val bloque = listOf(SmwEnemyGraphics.OamTile(0x06, 0, 0, page = 0))
            save(SmwEnemyGraphics.customSprite(rom, header, lv, 0x33, bloque, palRow = (8 + 2) * 16),
                "ranura06_dinamica_%03X".format(lv), 16)
            save(SmwEnemyGraphics.customSprite(rom, header, lv, 0x31, bloque, palRow = (8 + 2) * 16),
                "ranura06_estatica_%03X".format(lv), 16)
        }

        // Podoboo: los dos fotogramas del cuadrado (0 subiendo, 1 el parpadeo de la llama).
        for (f in 0..2) {
            save(SmwEnemyGraphics.squareTileImage(rom, header, 0x01A, 0x33, frame = f), "33_frame$f", 16)
        }

        // Spiny: los dos fotogramas de andar de la via generica.
        SmwEnemyGraphics.spriteFrames(rom, header, 0x001, 0x13, frames = 2)?.forEachIndexed { i, im ->
            save(im, "13_frame$i", 16)
        }

        // La MISMA hoja de VRAM del nivel del Podoboo pero pedida para un id NO dinamico:
        // ahi la ranura 0x06/0x16 sale como esta en el fichero GFX estatico (la fuente).
        donde[0x33]?.firstOrNull()?.let { lv ->
            save(SmwEnemyGraphics.spriteVramSheet(rom, header, lv, 0x31, palRow = (8 + 2) * 16),
                "vram_estatica_%03X".format(lv), 2)
        }
    }

    private fun t(tile: Int, dx: Int, dy: Int, flip: Boolean = false) =
        SmwEnemyGraphics.OamTile(tile, dx, dy, page = 1, xflip = flip)

    private fun save(img: ArgbImage?, name: String, zoom: Int) {
        if (img == null) { println("$name -> SIN IMAGEN"); return }
        val bi = BufferedImage(img.width * zoom, img.height * zoom, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until img.height * zoom) for (x in 0 until img.width * zoom) {
            bi.setRGB(x, y, img.get(x / zoom, y / zoom))
        }
        ImageIO.write(bi, "png", File(out, "$name.png"))
        println("$name -> ${img.width}x${img.height}")
    }
}
