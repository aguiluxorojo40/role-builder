package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SmwLayer1
import java.io.File
import kotlin.test.Test

/** SONDA DESECHABLE: ids sin portar agregados por (tileset, id) en toda la ROM. */
class ZzGlobalProbe {
    @Test
    fun probe() {
        val f = File("/home/user/role-builder/smw.sfc")
        if (!f.exists()) { println("sin ROM"); return }
        val rom = f.readBytes()
        val delta = SnesGameRecipes.smwHeaderDelta(SnesDecoder.parseHeader(rom))
        val agg = HashMap<String, Int>()      // "ts=N id" -> apariciones
        val lvls = HashMap<String, Int>()     // "ts=N id" -> nº de niveles
        for (lv in 0 until 0x200) {
            val tm = SmwLayer1.parse(rom, delta, lv) ?: continue
            for ((id, n) in tm.unknownIds) {
                val k = "ts=${tm.tileset.toString(16)} $id"
                agg[k] = (agg[k] ?: 0) + n
                lvls[k] = (lvls[k] ?: 0) + 1
            }
        }
        println("TOP ids sin portar (apariciones × niveles):")
        agg.entries.sortedByDescending { it.value }.take(20).forEach {
            println("  ${it.key}: ${it.value}× en ${lvls[it.key]} niveles")
        }
    }
}
