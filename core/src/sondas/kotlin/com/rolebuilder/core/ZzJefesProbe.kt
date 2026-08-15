package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * SONDA DESECHABLE: por qué las SALAS DE JEFE salen vacías. Recorre los 512 niveles,
 * agrupa por MODO DE PANTALLA y dice, para cada modo, cuántos niveles se reconstruyen y
 * cuántos no — y para los que no, en qué paso se cae. Se salta sola sin `SMW_ROM`.
 */
class ZzJefesProbe {

    @Test
    fun `que modos de pantalla no se reconstruyen`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)

        class Cuenta {
            var total = 0
            var conMapa = 0
            var sinLayer1 = 0
            var sinObjetos = 0
            var demasiadosDesconocidos = 0
            val ejemplos = ArrayList<Int>()
        }

        val porModo = HashMap<Int, Cuenta>()
        val conCabecera = (0 until 0x200).mapNotNull { lv ->
            val lpc = SnesGameRecipes.smwLayer1DataPc(rom, delta, lv)
            if (lpc == null || lpc + 5 > rom.size) null else lv to lpc
        }
        for ((lv, lpc) in conCabecera) {
            val modo = rom[lpc + 1].toInt() and 0x1F
            val c = porModo.getOrPut(modo) { Cuenta() }
            c.total++
            val tm = SmwLayer1.parse(rom, delta, lv)
            when {
                tm == null -> c.sinLayer1++
                tm.totalObjects == 0 -> {
                    c.sinObjetos++
                    // Cero objetos ya no implica "no hay mapa": las salas de jefe se montan
                    // desde su fondo. Hay que preguntarselo al extractor, no suponerlo.
                    if (runCatching { SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) }.getOrNull() != null) {
                        c.conMapa++
                    }
                }
                tm.unknownObjects * 10 > tm.totalObjects -> c.demasiadosDesconocidos++
                else -> {
                    val m = runCatching { SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) }.getOrNull()
                    if (m != null) c.conMapa++ else c.ejemplos.add(lv)
                }
            }
            if (tm == null || tm.totalObjects == 0) if (c.ejemplos.size < 6) c.ejemplos.add(lv)
        }

        println("\n=== NIVELES POR MODO DE PANTALLA ===")
        println("%-5s %6s %6s %8s %8s %8s  %s".format(
            "modo", "total", "mapa", "sinL1", "0 objs", "descon.", "ejemplos"))
        for (modo in porModo.keys.sorted()) {
            val c = porModo[modo]!!
            println("%-5d %6d %6d %8d %8d %8d  %s".format(
                modo, c.total, c.conMapa, c.sinLayer1, c.sinObjetos, c.demasiadosDesconocidos,
                c.ejemplos.take(6).joinToString(",") { "%03X".format(it) }))
        }

        // Si la sala no vive en Layer 1, ¿vive en Layer 2? Es lo que hay que saber antes
        // de tocar nada: la capa 2 ya se parsea, y si trae el suelo la sala se puede montar.
        println("\n=== LOS VACIOS: QUE TRAE SU LAYER 2 ===")
        for (modo in intArrayOf(9, 11, 16)) {
            for (lv in porModo[modo]?.ejemplos.orEmpty().distinct().take(8)) {
                val l2 = runCatching { SmwLayer1.parseLayer2(rom, delta, lv) }.getOrNull()
                println("  modo %-3d nivel %03X  layer2=%s objetos=%s descon=%s %dx%d".format(
                    modo, lv, l2 != null, l2?.totalObjects, l2?.unknownObjects,
                    l2?.cols ?: 0, l2?.rows ?: 0))
            }
        }

        println("\n=== LOS VACIOS: FONDO DE IMAGEN Y COLISION ===")
        for (modo in intArrayOf(9, 11, 16)) {
            for (lv in porModo[modo]?.ejemplos.orEmpty().distinct()) {
                val bg = runCatching { SnesGameRecipes.layer2BgParse(rom, delta, lv) }.getOrNull()
                val col = runCatching { SnesGameRecipes.smwLevelCollision(rom, header, lv) }.getOrNull()
                println("  modo %-3d nivel %03X  fondo=%s  colision=%s".format(
                    modo, lv,
                    when (bg) {
                        is SnesGameRecipes.BgParse.Success -> "OK ${bg.entries.size} teselas pag=${bg.page}"
                        is SnesGameRecipes.BgParse.NoBackground -> "sin fondo (banco 0x%02X)".format(bg.bank)
                        is SnesGameRecipes.BgParse.Error -> "ERROR ${bg.reason}"
                        else -> "?"
                    },
                    col?.let { "si" } ?: "no"))
            }
        }

        println("\n=== QUE SPRITES TRAE CADA SALA DE JEFE ===")
        for (modo in intArrayOf(9, 11, 16)) {
            for (lv in porModo[modo]?.ejemplos.orEmpty().distinct().take(4)) {
                val e = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv) }.getOrNull()
                println("  modo %-3d nivel %03X  sprites=%s".format(
                    modo, lv, e?.map { "%02X".format(it.first) }?.distinct() ?: "null"))
            }
        }

        println("\n=== LOS QUE NO SALEN, CON NOMBRE ===")
        for (modo in porModo.keys.sorted()) {
            val c = porModo[modo]!!
            if (c.conMapa == c.total) continue
            for (lv in c.ejemplos.take(8)) {
                val n = runCatching { SmwLevelNames.nameOf(rom, delta, lv)?.trim() }.getOrNull()
                println("  modo %-3d nivel %03X  %s".format(modo, lv, n))
            }
        }
    }
}
