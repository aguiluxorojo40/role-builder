package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelSet
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * SONDA de cobertura del parser de Layer 1 (no es una aserción: mide e imprime).
 * Se salta sola si no hay ROM (`SMW_ROM`), así que no afecta a CI. Sirve para saber
 * ANTES/DESPUÉS cuántos niveles se reconstruyen al 100% y qué ids quedan sin portar.
 */
class ZzLayer1CoverageProbe {

    @Test
    fun `cobertura de Layer 1 por nivel`() {
        val rom = findRom() ?: return
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(SnesDecoder.parseHeader(rom))
        var parsed = 0
        var full = 0
        val hist = HashMap<String, Int>()
        val levelsOf = HashMap<String, MutableSet<Int>>()
        val brokenLevels = sortedSetOf<Int>()
        val tilesetOf = HashMap<Int, Int>()
        // El denominador honesto NO son los 512 huecos: son los niveles que el juego
        // referencia de verdad (casillas del mapa + sus sub-niveles). Ver SmwLevelSet.
        val universe = SmwLevelSet.reachableLevels(rom, SnesDecoder.parseHeader(rom))
        val mapOnly = SmwLevelSet.mapLevels(rom, delta).map { it.level }.toSet()
        println("=== universo: ${universe.size} alcanzables (${mapOnly.size} con casilla en el mapa) ===")
        for (lvl in universe) {
            val tm = SmwLayer1.parse(rom, delta, lvl) ?: continue
            parsed++
            tilesetOf[lvl] = tm.tileset
            if (tm.unknownObjects == 0) {
                full++
            } else {
                brokenLevels += lvl
            }
            for ((k, v) in tm.unknownIds) {
                hist[k] = (hist[k] ?: 0) + v
                levelsOf.getOrPut(k) { mutableSetOf() } += lvl
            }
        }
        println("=== LAYER 1: $full/$parsed niveles REALES al 100% (${parsed - full} con ids sin portar) ===")
        hist.entries.sortedByDescending { it.value }.forEach { (k, v) ->
            val lv = levelsOf[k]!!.sorted().joinToString(",") { "%03X".format(it) }
            println("  %-8s %3d ocurrencias en %d niveles: %s".format(k, v, levelsOf[k]!!.size, lv))
        }
        println("=== niveles incompletos (nivel:tileset): ${
            brokenLevels.joinToString(",") { "%03X:%d".format(it, tilesetOf[it] ?: -1) }
        }")

        // Lo que de verdad hace falta portar: id + TILESET (los >=0x2E son específicos del
        // tileset, así que "std:35" no dice nada sin saber en qué familia sale).
        val byIdTileset = HashMap<String, Int>()
        val lvlsOf = HashMap<String, MutableSet<Int>>()
        for (lvl in universe) {
            val tm = SmwLayer1.parse(rom, delta, lvl) ?: continue
            for ((k, v) in tm.unknownIds) {
                val fam = when (tm.tileset) {
                    0, 7, 0xC -> "pradera"
                    1 -> "castillo"
                    2, 6, 8 -> "cuerda"
                    3, 9, 0xA, 0xB, 0xE -> "cueva"
                    4, 5, 0xD -> "fantasma"
                    else -> "ts${tm.tileset}"
                }
                val key = "%s@%s".format(k, fam)
                byIdTileset[key] = (byIdTileset[key] ?: 0) + v
                lvlsOf.getOrPut(key) { mutableSetOf() } += lvl
            }
        }
        println("\n=== QUÉ PORTAR (id @ familia de tileset), por nº de niveles que arregla ===")
        byIdTileset.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { lvlsOf[it.key]!!.size }.thenByDescending { it.value })
            .forEach { (k, v) ->
                println("  %-18s %3d ocurr. en %2d niveles: %s".format(
                    k, v, lvlsOf[k]!!.size, lvlsOf[k]!!.sorted().joinToString(",") { "%03X".format(it) },
                ))
            }
    }

    private fun findRom(): ByteArray? {
        val candidates = listOf(
            System.getenv("SMW_ROM"),
            "/home/user/role-builder/smw.sfc",
        )
        for (p in candidates) if (p != null && File(p).isFile) return File(p).readBytes()
        return null
    }
}
