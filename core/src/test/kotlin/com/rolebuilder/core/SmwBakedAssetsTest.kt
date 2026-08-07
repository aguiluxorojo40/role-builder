package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwBakedAssets
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Los assets que ANTES iban empaquetados (material de Nintendo) se regeneran desde la ROM del
 * usuario. Este test comprueba que salen de verdad: si no, quitarlos del repo habría costado
 * funcionalidad. Gated a la ROM local.
 */
class SmwBakedAssetsTest {

    @Test
    fun regeneratesEverythingThatUsedToBeShipped() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)

        val images = SmwBakedAssets.images(rom, header)
        // Los siete ficheros que estaban en assets/sprites/ tienen que volver a salir.
        val expected = listOf(
            "sprites/mario.png", "sprites/mario_big.png", "sprites/mario_cape.png",
            "sprites/mario_fire.png", "sprites/coin.png", "sprites/powerups.png",
            "sprites/enemies.png",
        )
        for (path in expected) {
            val img = images[path]
            assertNotNull(img, "falta regenerar $path")
            assertTrue(img.width > 0 && img.height > 0, "$path vacío")
            // Tiene que tener DIBUJO, no ser un lienzo transparente.
            assertTrue(img.pixels.count { it ushr 24 != 0 } > 32, "$path sin píxeles: $path")
        }

        // El atlas de enemigos debe tener la geometría EXACTA que indexa el renderer.
        val atlas = images["sprites/enemies.png"]!!
        assertEquals(SmwEnemyGraphics.curatedIds.size * SmwEnemyGraphics.ATLAS_CELL, atlas.width)
        assertEquals(SmwEnemyGraphics.ATLAS_FRAMES * SmwEnemyGraphics.ATLAS_CELL, atlas.height)

        val big = SmwBakedAssets.bigSprites(rom, header)
        assertTrue(big.isNotEmpty(), "los sprites grandes deben regenerarse: ${big.size}")

        val dumpDir = File(
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/scratchpad"
        )
        if (dumpDir.isDirectory) {
            val a = images["sprites/enemies.png"]!!
            val bi = BufferedImage(a.width, a.height, BufferedImage.TYPE_INT_ARGB)
            bi.setRGB(0, 0, a.width, a.height, a.pixels, 0, a.width)
            ImageIO.write(bi, "png", File(dumpDir, "baked_enemies.png"))
            val m = images["sprites/mario.png"]!!
            val bm = BufferedImage(m.width, m.height, BufferedImage.TYPE_INT_ARGB)
            bm.setRGB(0, 0, m.width, m.height, m.pixels, 0, m.width)
            ImageIO.write(bm, "png", File(dumpDir, "baked_mario.png"))
            println("BAKED atlas=${a.width}x${a.height} grandes=${big.size} imagenes=${images.size}")
        }
    }

    @Test
    fun `los enemigos de DIBUJO PROPIO tambien se hornean`() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val big = SmwBakedAssets.bigSprites(rom, header)
        // Estos no están en curatedIds, así que bigSprites es su ÚNICA vía al
        // dispositivo. Si alguien los deja fuera, en el móvil vuelven a salir como
        // rectángulo y nadie se entera: por eso el test.
        val faltan = SmwEnemyGraphics.customEnemyIds.filter { it !in big.keys }
        assertTrue(faltan.isEmpty(), "sin hornear: " + faltan.joinToString { "%02X".format(it) })
        assertTrue(big.size >= SmwEnemyGraphics.customEnemyIds.size, "faltan grandes")
    }

    private fun findRom(): ByteArray? {
        val candidates = listOf(
            System.getenv("SMW_ROM"),
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/" +
                "scratchpad/smw_work/rom/Super Mario World (USA).sfc",
        )
        for (p in candidates) if (p != null && File(p).isFile) return File(p).readBytes()
        return null
    }
}
