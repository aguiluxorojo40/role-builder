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

        // Cada fichero lleva EXACTAMENTE una fuente: imagen (PNG), gif o bytes (WAV).
        for (e in entries) {
            val sources = listOfNotNull(e.image, e.gif, e.bytes).size
            assertEquals(1, sources, "una sola fuente por fichero: ${e.path}")
            when {
                e.path.endsWith(".png") -> assertTrue(e.image != null, "PNG lleva imagen: ${e.path}")
                e.path.endsWith(".gif") -> assertTrue(e.gif != null, "GIF lleva bytes: ${e.path}")
                e.path.endsWith(".wav") -> assertTrue(e.bytes != null, "WAV lleva bytes: ${e.path}")
            }
        }
        // El GIF de Mario es un GIF de verdad.
        val gif = marioDown.first { it.path.endsWith(".gif") }.gif!!
        assertTrue(String(gif.copyOfRange(0, 6), Charsets.US_ASCII).startsWith("GIF8"), "GIF real")

        // NINGUNA ruta se repite: si dos sprites comparten nombre no deben pisarse.
        val paths = entries.map { it.path }
        assertEquals(paths.size, paths.toSet().size, "no hay ficheros con la misma ruta")

        // Un enemigo de una sola animación va directo en su carpeta, sin subcarpeta 'anim'.
        //
        // Este test estuvo ROTO un tiempo sin que nadie se enterara: miraba
        // `enemigos/koopa_verde/`, y esa carpeta dejó de existir al corregir los nombres de
        // los Koopa (los 0x00-0x03 son los que van SIN caparazón, así que ahora se llama
        // `koopa_sin_caparazon_verde`). El código estaba bien; el test se quedó viejo. No
        // salta en CI porque sin ROM se salta entero — es el punto ciego de AUDITORIA.md.
        //
        // Para que no vuelva a pasar por un renombrado, la carpeta se busca por el nombre
        // que de verdad exporta el catálogo en vez de fijarlo a mano.
        val koopaDir = entries.map { it.path }
            .firstOrNull { it.startsWith("enemigos/koopa_sin_caparazon_verde/") }
            ?.substringBeforeLast('/')
        assertTrue(koopaDir != null, "el Koopa verde sin caparazón debería estar en el catálogo")
        val koopa = entries.filter { it.path.startsWith("$koopaDir/") }
        val nombre = koopaDir!!.substringAfterLast('/')
        assertTrue(koopa.any { it.path == "$koopaDir/$nombre.gif" }, "GIF con el nombre del sprite")
        assertTrue(koopa.none { it.path.contains("/anim/") }, "sin subcarpeta redundante")

        // Los nombres repetidos se distinguen: hay 'cheep_cheep' y 'cheep_cheep_2'.
        val cheeps = paths.filter { it.startsWith("enemigos/cheep_cheep") }.map { it.substringAfter("enemigos/").substringBefore('/') }.toSet()
        assertTrue(cheeps.contains("cheep_cheep") && cheeps.contains("cheep_cheep_2"), "los dos Cheep-Cheep, separados")
    }

    @Test
    fun theCatalogAlsoExtractsSound() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val groups = SmwAssetCatalog.build(rom, header)
        val audio = groups.firstOrNull { it.name == "Audio" } ?: return // gated si no hay SFX
        assertTrue(audio.items.all { it.audio != null }, "los items de audio traen su WAV")
        // El WAV de un efecto es un RIFF/WAVE de verdad.
        val wav = audio.items.first().audio!!.wav
        assertTrue(String(wav.copyOfRange(0, 4), Charsets.US_ASCII) == "RIFF", "cabecera RIFF")
        assertTrue(String(wav.copyOfRange(8, 12), Charsets.US_ASCII) == "WAVE", "formato WAVE")
        // Y sale como .wav en la estructura de export.
        val entries = SmwAssetCatalog.exportEntries(listOf(audio))
        assertTrue(entries.all { it.path.endsWith(".wav") && it.bytes != null }, "todo WAV")
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
