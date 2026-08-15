package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * SONDA del LAYER 2 DE OBJETOS: cuántos niveles lo tienen, cuántos objetos trae cada uno
 * y si queda algún id sin portar. Mide e imprime, no afirma. Se salta sola sin `SMW_ROM`.
 */
class ZzLayer2ObjectProbe {

    @Test
    fun `layer 2 de objetos por nivel`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(SnesDecoder.parseHeader(rom))
        var con = 0
        var completos = 0
        val hist = HashMap<String, Int>()
        println("nivel modo  objetos  desconocidos  vert  tamaño")
        val conObjetos = (0x000..0x1FF)
            .mapNotNull { lv -> SmwLayer1.parseLayer2(rom, delta, lv)?.let { lv to it } }
            .filter { (_, tm) -> tm.totalObjects > 0 }
        for ((lv, tm) in conObjetos) {
            con++
            if (tm.unknownObjects == 0) completos++
            for ((k, v) in tm.unknownIds) hist[k] = (hist[k] ?: 0) + v
            val t = tm.trimmedSize()
            println(
                "  %03X   %2d   %5d   %5d        %-5s %s".format(
                    lv, tm.mode, tm.totalObjects, tm.unknownObjects, tm.vertical,
                    t?.let { "${it.first}x${it.second}" } ?: "(vacío)",
                ),
            )
        }
        println("=== LAYER 2 DE OBJETOS: $completos/$con niveles al 100% ===")
        hist.entries.sortedByDescending { it.value }.forEach { (k, v) -> println("  %-8s %d".format(k, v)) }
    }
}
