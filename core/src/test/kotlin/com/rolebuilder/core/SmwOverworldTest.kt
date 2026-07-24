package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworld
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests SINTÉTICOS de la capa estática del overworld ([SmwOverworld]). Se plantan a mano
 * unos bytes en las direcciones $05:D608 (eventos) y $04:8431… (Star Road) de un ROM vacío
 * y se comprueba el decodificado. Sin ROM con copyright.
 */
class SmwOverworldTest {

    /** SNES LoROM → PC headerless (delta 0), igual que usa el lector. */
    private fun pc(snes: Int): Int = (snes shr 16) * 0x8000 + (snes and 0x7FFF)

    private fun putWord(rom: ByteArray, snes: Int, v: Int) {
        rom[pc(snes)] = (v and 0xFF).toByte()
        rom[pc(snes + 1)] = ((v shr 8) and 0xFF).toByte()
    }

    @Test
    fun readsTranslevelEvents() {
        val rom = ByteArray(0x40000)
        rom[pc(SmwOverworld.EVENTS_SNES + 0x00)] = SmwOverworld.EVENT_NONE.toByte()
        rom[pc(SmwOverworld.EVENTS_SNES + 0x01)] = 0x1F
        rom[pc(SmwOverworld.EVENTS_SNES + 0x28)] = 0x00

        val ev = SmwOverworld.translevelEvents(rom, 0)
        assertEquals(256, ev.size)
        assertEquals(SmwOverworld.EVENT_NONE, ev[0x00])
        assertEquals(0x1F, ev[0x01])
        assertEquals(0x00, ev[0x28])
    }

    @Test
    fun readsStarRoadWarps() {
        val rom = ByteArray(0x40000)
        // warp 3: origen (0xB, 0xE) → destino (0x928, 0x18)
        putWord(rom, SmwOverworld.STAR_SRCX_SNES + 2 * 3, 0xB)
        putWord(rom, SmwOverworld.STAR_SRCY_SNES + 2 * 3, 0xE)
        putWord(rom, SmwOverworld.STAR_DSTX_SNES + 2 * 3, 0x928)
        putWord(rom, SmwOverworld.STAR_DSTY_SNES + 2 * 3, 0x18)

        val warps = SmwOverworld.starRoadWarps(rom, 0)
        assertEquals(27, warps.size)
        assertEquals(SmwOverworld.StarWarp(0xB, 0xE, 0x928, 0x18), warps[3])
    }

    @Test
    fun detectsLevelTiles() {
        val rom = ByteArray(0x80000)
        // dos casillas-de-nivel (0x56..0x80) y una que NO lo es (0x40) en $0C:F7DF
        rom[pc(SmwOverworld.LEVEL_TILES_SNES + 0x10)] = 0x56          // límite inferior
        rom[pc(SmwOverworld.LEVEL_TILES_SNES + 0x11)] = 0x40          // fuera de rango
        rom[pc(SmwOverworld.LEVEL_TILES_SNES + 0x20)] = 0x80.toByte() // límite superior

        val tiles = SmwOverworld.levelTiles(rom, 0)
        assertEquals(2, tiles.size)
        assertEquals(SmwOverworld.LevelTile(0x10, 0x56), tiles[0])
        assertEquals(SmwOverworld.LevelTile(0x20, 0x80), tiles[1])
    }

    /**
     * Verificación con la ROM REAL si está disponible en el scratchpad (no versionada, con
     * copyright). En CI (sin ROM) el test pasa sin hacer nada. Confirma los ~92 niveles.
     */
    @Test
    fun realRomHasNinetyTwoLevelTiles() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val tiles = SmwOverworld.levelTiles(rom, delta)
        assertEquals(92, tiles.size, "SMW US vanilla tiene 92 casillas-de-nivel")
        val named = SmwOverworld.namedLevels(rom, delta)
        assertTrue(named.size >= 90, "debería nombrar la gran mayoría de niveles: ${named.size}")
        assertTrue(named.any { it.translevel == 0x29 && it.name.contains("YOSHI") },
            "translevel 0x29 = YOSHI'S ISLAND 1")

        // El tilemap L2 (mapa visible) descomprime a 0x2000 índices Map16 con variedad real.
        val l2 = SmwOverworld.layer2Tilemap(rom, delta)
        assertEquals(0x2000, l2.size)
        assertTrue(l2.toSet().size in 50..255, "el mapa usa muchos Map16 distintos: ${l2.toSet().size}")
    }

    /**
     * Render del MAPA PRINCIPAL del overworld con la ROM real (si está en el scratchpad).
     * El tilemap del overworld son casillas SNES de 8×8 DIRECTAS (no Map16): byte bajo de
     * la tabla de teselas, byte alto de la de propiedades (paleta/flip). El mapa principal
     * son las pantallas 0-3 en 2×2 (512×512). Se comprueba que sale una imagen con COLOR y
     * VARIEDAD reales (mapa reconocible), no un lienzo plano. En CI (sin ROM) no hace nada.
     */
    @Test
    fun rendersOverworldMainMap() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)

        // Tilemap combinado (bajo|prop<<8): 0x2000 casillas de 8×8 con propiedades reales.
        val delta = header.headerOffset - 0x7FC0
        val tm = SmwOverworld.overworldTilemap(rom, delta)
        assertEquals(0x2000, tm.size)
        assertTrue(tm.map { it shr 10 and 7 }.toSet().size >= 4, "el mapa usa varias sub-paletas")

        val img = SnesGameRecipes.renderOverworldMainMap(rom, header)
        assertNotNull(img, "el mapa principal del overworld debe renderizar")
        assertEquals(512, img.width)
        assertEquals(512, img.height)

        // Volcado local para inspección visual (solo si existe el scratchpad; nunca en CI ni
        // versionado: la imagen es contenido derivado de la ROM con copyright).
        val dumpDir = File(
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/scratchpad"
        )
        if (dumpDir.isDirectory) {
            fun dump(name: String, im: com.rolebuilder.core.snes.ArgbImage) {
                val bi = BufferedImage(im.width, im.height, BufferedImage.TYPE_INT_ARGB)
                bi.setRGB(0, 0, im.width, im.height, im.pixels, 0, im.width)
                ImageIO.write(bi, "png", File(dumpDir, name))
            }
            dump("ow_kotlin_main.png", img)
            for (s in 4..7) SnesGameRecipes.renderOverworldScreen(rom, header, s, s - 3)?.let {
                dump("ow_kotlin_screen$s.png", it)
            }
        }

        // Variedad de color: la paleta de área del mapa principal es 3bpp (pocas filas), así
        // que un mapa REAL sale con ~12 colores (verdes, marrones, arena, agua, blanco,
        // negro, gris); si el decodificado de teselas o paleta fallara saldría 1-2. El umbral
        // distingue "mapa real" de "lienzo plano" sin exigir más color del que hay.
        assertTrue(img.pixels.toSet().size >= 8, "el mapa debe tener color y variedad reales: ${img.pixels.toSet().size}")
    }

    private fun findRom(): ByteArray? {
        val candidates = listOf(
            System.getenv("SMW_ROM"),
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/" +
                "scratchpad/smw_work/rom/Super Mario World (USA).sfc",
        )
        for (p in candidates) {
            if (p != null && File(p).isFile) return File(p).readBytes()
        }
        return null
    }

    @Test
    fun honorsHeaderDelta() {
        // Con cabecera de copiador (0x200), el mismo dato vive 0x200 más adelante.
        val delta = 0x200
        val rom = ByteArray(0x40200)
        rom[pc(SmwOverworld.EVENTS_SNES + 0x05) + delta] = 0x42
        val ev = SmwOverworld.translevelEvents(rom, delta)
        assertEquals(0x42, ev[0x05])
    }
}
