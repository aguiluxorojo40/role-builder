package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE: ¿en qué banco de GFX de sprites vive de verdad la SUPER KOOPA?
 * Su fotograma sale igual de mal en los cuatro niveles donde está puesta, así que en vez
 * de seguir mirando esos niveles se prueban TODOS los ajustes de GFX y se vuelca cada uno.
 * Si en alguno aparece la Koopa con su capa, el problema es de qué banco elegimos; si no
 * aparece en ninguno, es del layout de teselas. Se salta sola sin `SMW_ROM`.
 */
class ZzSuperKoopaProbe {

    @Test
    fun `probar todos los bancos de GFX`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/sprites/sk")
        out.mkdirs()

        val capePal = (8 + 2) * 16
        // Tablas REALES de $02:ECDE, tal cual: 9 fotogramas x 4 teselas.
        val tl = intArrayOf(
            0xc8, 0xd8, 0xd0, 0xe0, 0xc9, 0xd9, 0xc0, 0xe2, 0xe4, 0xe5, 0xf2, 0xe0,
            0xf4, 0xf5, 0xf2, 0xe0, 0xda, 0xca, 0xe0, 0xcf, 0xdb, 0xcb, 0xe0, 0xcf,
            0xe4, 0xe5, 0xe0, 0xcf, 0xf4, 0xf5, 0xe2, 0xcf, 0xe4, 0xe5, 0xe2, 0xcf,
        )
        val xd = intArrayOf(
            0x8, 0x8, 0x10, 0x0, 0x8, 0x8, 0x10, 0x0, 0x8, 0x10, 0x10, 0x0,
            0x8, 0x10, 0x10, 0x0, 0x9, 0x9, 0x0, 0x0, 0x9, 0x9, 0x0, 0x0,
            0x8, 0x10, 0x0, 0x0, 0x8, 0x10, 0x0, 0x0, 0x8, 0x10, 0x0, 0x0,
        )
        val yd = intArrayOf(
            0x0, 0x8, 0x8, 0x0, 0x0, 0x8, 0x8, 0x0, 0x3, 0x3, 0x8, 0x0,
            0x3, 0x3, 0x8, 0x0, 0xff, 0x7, 0x0, 0x0, 0xff, 0x7, 0x0, 0x0,
            0xfd, 0xfd, 0x0, 0x0, 0xfd, 0xfd, 0x0, 0x0, 0xfd, 0xfd, 0x0, 0x0,
        )
        val pr = intArrayOf(
            0x3, 0x3, 0x3, 0x0, 0x3, 0x3, 0x3, 0x0, 0x3, 0x3, 0x1, 0x1,
            0x3, 0x3, 0x1, 0x1, 0x83, 0x83, 0x80, 0x0, 0x83, 0x83, 0x80, 0x0,
            0x3, 0x3, 0x0, 0x1, 0x3, 0x3, 0x0, 0x1, 0x3, 0x3, 0x0, 0x1,
        )
        val sz = intArrayOf(
            0x0, 0x0, 0x0, 0x2, 0x0, 0x0, 0x0, 0x2, 0x0, 0x0, 0x0, 0x2,
            0x0, 0x0, 0x0, 0x2, 0x0, 0x0, 0x2, 0x0, 0x0, 0x0, 0x2, 0x0,
            0x0, 0x0, 0x2, 0x0, 0x0, 0x0, 0x2, 0x0, 0x0, 0x0, 0x2, 0x0,
        )
        for (frame in 0..8) {
            val tiles = (0..3).map { r4 ->
                val v2 = frame * 4 + r4
                val prop = pr[v2]
                // La rama del juego: con el bit 1 puesto va (Prop|v4)&~2 (capa, pagina 1);
                // si no, r5|Prop con r5 = $166E & 0xE, que BORRA el bit 0 (pagina 0).
                val capa = (prop and 2) != 0
                val flags = if (capa) (prop or 4) and 2.inv() else prop
                SmwEnemyGraphics.OamTile(
                    tl[v2],
                    xd[v2].toByte().toInt(),
                    yd[v2].toByte().toInt(),
                    size16 = sz[v2] == 2,
                    vflip = (prop and 0x80) != 0,
                    palRow = if (capa) capePal else null,
                    page = flags and 1,
                )
            }
            val img = runCatching {
                SmwEnemyGraphics.customSprite(rom, header, 0x00D, 0x73, tiles, null, null)
            }.getOrNull() ?: continue
            val bi = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until img.height) for (x in 0 until img.width) bi.setRGB(x, y, img.get(x, y))
            ImageIO.write(bi, "png", File(out, "fr_%d.png".format(frame)))
            println("  fotograma %d -> %dx%d".format(frame, img.width, img.height))
        }
    }
}
