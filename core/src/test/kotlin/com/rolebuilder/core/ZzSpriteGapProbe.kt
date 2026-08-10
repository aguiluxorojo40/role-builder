package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * SONDA DESECHABLE: qué sprites siguen SIN gráfico en TODA la ROM, ordenados por lo que
 * de verdad importa — cuántas veces están puestos en niveles jugables y en cuántos niveles
 * distintos salen. Se salta sola sin `SMW_ROM`, así que no afecta a CI.
 *
 * No cuenta como hueco lo que ya está resuelto por otra vía: los sprites de META
 * ([SmwLevelGoal.GOAL_SPRITES]), los que el juego NO dibuja a propósito
 * ([SmwEnemyGraphics.isIntentionallyInvisible]) y los que tienen rutina de dibujo propia.
 */
class ZzSpriteGapProbe {

    @Test
    fun `que sprites faltan en toda la ROM`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)

        val puestos = HashMap<Int, Int>()          // id -> colocaciones
        val niveles = HashMap<Int, MutableSet<Int>>()
        var totalPuestos = 0
        var totalConGrafico = 0

        for (lv in 0 until 0x200) {
            val ids = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv) }
                .getOrNull()?.map { it.first } ?: continue
            val cuentan = ids.filterNot { it in SmwLevelGoal.GOAL_SPRITES || SmwEnemyGraphics.isIntentionallyInvisible(it) }
            for (id in cuentan) {
                totalPuestos++
                if (tieneGrafico(rom, header, lv, id)) {
                    totalConGrafico++
                } else {
                    puestos[id] = (puestos[id] ?: 0) + 1
                    niveles.getOrPut(id) { HashSet() }.add(lv)
                }
            }
        }

        val pct = if (totalPuestos == 0) 0.0 else 100.0 * totalConGrafico / totalPuestos
        println("\n=== COBERTURA REAL DE SPRITES EN TODA LA ROM ===")
        println("colocaciones con gráfico: %d / %d (%.1f%%)".format(totalConGrafico, totalPuestos, pct))
        println("ids distintos sin gráfico: ${puestos.size}")
        println("\n%-4s %-30s %8s %8s".format("id", "nombre", "puestos", "niveles"))
        puestos.entries.sortedByDescending { it.value }.forEach { (id, n) ->
            println("%02X   %-30s %8d %8d".format(id, SmwSpriteNames.nameOf(id), n, niveles[id]!!.size))
        }
    }

    private fun tieneGrafico(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int, id: Int): Boolean {
        if (SmwEnemyGraphics.handles(id)) return true
        return runCatching { SmwEnemyGraphics.customEnemyImage(rom, header, lv, id) != null }.getOrDefault(false)
    }
}
