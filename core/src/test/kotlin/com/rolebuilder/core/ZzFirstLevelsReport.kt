package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * SEGUIMIENTO del objetivo "100% de los primeros niveles": qué está y qué falta,
 * pieza por pieza. Se salta sola sin `SMW_ROM`, así que no afecta a CI.
 *
 * Ojo con dos falsos huecos que ya están resueltos y NO hay que contar:
 * - Los sprites de META (0x7B cinta, 0x4A esfera, 0x0E cerradura) no son enemigos:
 *   el motor los siembra como ítem de meta ([SmwLevelGoal.GOAL_SPRITES]).
 * - Los ids con rutina de dibujo propia sí tienen gráfico, aunque no estén en el
 *   catálogo curado.
 */
class ZzFirstLevelsReport {

    private val primeros = listOf(0x105 to "YOSHI'S ISLAND 1", 0x106 to "YOSHI'S ISLAND 2", 0x103 to "YOSHI'S ISLAND 3")

    @Test
    fun `cuanto falta para el 100 por cien de los primeros niveles`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val faltan = sortedMapOf<Int, MutableList<Int>>()

        for ((lv, _) in primeros) {
            println("\n=== %03X — %s ===".format(lv, SmwLevelNames.nameOf(rom, delta, lv)?.trim()))
            val tm = SmwLayer1.parse(rom, delta, lv)
            println("  Layer 1        : %s (%d objetos)".format(
                if (tm != null && tm.unknownObjects == 0) "✓ 100%" else "✗ ${tm?.unknownIds}", tm?.totalObjects ?: -1))
            println("  Colisión       : %s".format(
                if (runCatching { SnesGameRecipes.smwLevelCollision(rom, header, lv) }.getOrNull() != null) "✓" else "✗"))
            println("  Mapa importable: %s".format(
                if (runCatching { SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) }.getOrNull() != null) "✓" else "✗"))
            println("  Meta           : %s".format(
                runCatching { SmwLevelGoal.goalCellsWithKind(rom, delta, lv).map { it.kind }.toSet() }.getOrNull()))

            val ids = SnesGameRecipes.smwLevelEnemies(rom, header, lv).map { it.first }
            val porId = ids.groupingBy { it }.eachCount().toSortedMap()
            val sinGrafico = ArrayList<String>()
            for ((id, n) in porId) {
                if (id in SmwLevelGoal.GOAL_SPRITES) continue          // es meta, no enemigo
                if (SmwEnemyGraphics.isIntentionallyInvisible(id)) continue // no dibuja NADA en el juego
                if (SmwEnemyGraphics.handles(id)) continue             // catálogo curado
                val propio = runCatching {
                    SmwEnemyGraphics.customEnemyImage(rom, header, lv, id) != null
                }.getOrDefault(false)
                if (propio) continue                                   // rutina de dibujo propia
                sinGrafico.add("%02X×%d %s".format(id, n, SmwSpriteNames.nameOf(id)))
                faltan.getOrPut(id) { mutableListOf() }.add(lv)
            }
            println("  Sprites SIN gráfico (%d): %s".format(sinGrafico.size, sinGrafico.joinToString(", ")))
        }

        println("\n=== LO QUE FALTA PARA EL 100%: ${faltan.size} ids de sprite ===")
        faltan.forEach { (id, lvs) ->
            println("  %02X %-28s en %s".format(id, SmwSpriteNames.nameOf(id),
                lvs.joinToString(",") { "%03X".format(it) }))
        }
    }
}
