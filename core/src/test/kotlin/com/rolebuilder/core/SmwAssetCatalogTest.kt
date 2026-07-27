package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwAssetCatalog
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CATÁLOGO de extracción ([SmwAssetCatalog]). Gated a la ROM local.
 *
 * Lo que importa aquí es la promesa que hizo el usuario explícita: la herramienta identifica y
 * separa los sprites **por animación**, cada uno en **su propia subcarpeta**. Estos tests
 * comprueban justo eso: la estructura de carpetas y que cada animación lleva sus fotogramas y
 * su GIF.
 */
class SmwAssetCatalogTest {

    @Test
    fun theCatalogClassifiesMarioAndEnemies() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val groups = SmwAssetCatalog.build(rom, header)

        val mario = groups.firstOrNull { it.name == "Mario (mapa)" }
        assertTrue(mario != null, "hay grupo de Mario")
        val marioItem = mario!!.items.single()
        // Cuatro animaciones de andar, una por dirección, cada una con sus fotogramas.
        assertEquals(4, marioItem.clips.size, "4 direcciones")
        assertTrue(marioItem.clips.all { it.frames.size == 4 }, "cada una, su ciclo de 4")
        assertTrue(marioItem.clips.all { it.animated }, "las 4 son animaciones")

        val enemies = groups.firstOrNull { it.name == "Enemigos" }
        assertTrue(enemies != null && enemies.items.size > 10, "hay un buen puñado de enemigos")
    }

    @Test
    fun everyAnimationLandsInItsOwnSubfolder() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val entries = SmwAssetCatalog.exportEntries(SmwAssetCatalog.build(rom, header))
        assertTrue(entries.isNotEmpty())

        // La estructura es grupo/item/animación/fichero — separada por animación.
        val marioDown = entries.filter { it.path.startsWith("mario_mapa/mario/andar_abajo/") }
        assertTrue(marioDown.isNotEmpty(), "la animación de andar hacia abajo tiene su carpeta")
        // Sus 4 fotogramas PNG y su GIF.
        assertEquals(4, marioDown.count { it.path.endsWith(".png") }, "4 PNG de fotograma")
        assertEquals(1, marioDown.count { it.path.endsWith(".gif") }, "y un GIF de la animación")

        // Los PNG llevan imagen y los GIF, bytes; nunca los dos ni ninguno.
        for (e in entries) {
            val isPng = e.path.endsWith(".png")
            assertEquals(isPng, e.image != null, "el PNG lleva imagen: ${e.path}")
            assertEquals(!isPng, e.gif != null, "el GIF lleva bytes: ${e.path}")
        }
        // El GIF de Mario es un GIF de verdad.
        val gif = marioDown.first { it.path.endsWith(".gif") }.gif!!
        assertTrue(String(gif.copyOfRange(0, 6), Charsets.US_ASCII).startsWith("GIF8"), "GIF real")
    }

    @Test
    fun folderNamesAreCleanNoAccentsNoSpaces() {
        // Los nombres de carpeta son seguros: sin acentos, sin espacios.
        assertEquals("koopa_verde", SmwAssetCatalog.slug("Koopa verde"))
        assertEquals("planta_pirana", SmwAssetCatalog.slug("Planta Piraña"))
        assertEquals("mario_mapa", SmwAssetCatalog.slug("Mario (mapa)"))
        assertTrue(SmwAssetCatalog.slug("Cheep-Cheep").none { it == '-' || it == ' ' })
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
