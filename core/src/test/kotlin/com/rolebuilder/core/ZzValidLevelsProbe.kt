package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwOverworldLevels
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * INVESTIGACIÓN: ¿cuáles de los 512 slots de nivel son de verdad NIVELES?
 * Mide contra la ROM; se salta sola sin `SMW_ROM`.
 */
class ZzValidLevelsProbe {

    @Test
    fun `que slots son niveles de verdad`() {
        val rom = findRom() ?: return
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)

        // 1) Casillas-de-nivel del overworld = translevels.
        val ow = SmwOverworldLevels.levels(rom, delta)
        println("=== CASILLAS-DE-NIVEL DEL OVERWORLD: ${ow.size} ===")
        println("  translevel min=${ow.minOf { it.levelNumber }} max=${ow.maxOf { it.levelNumber }}")
        val mainCount = ow.count { it.onMainMap }
        println("  mapa principal=$mainCount  submapas=${ow.size - mainCount}")

        // 2) ¿Se cumple  (translevel >= 0x25) <=> está en submapa?
        val mismatches = ow.filter { (it.levelNumber >= 0x25) != !it.onMainMap }
        println("  regla (translevel>=0x25 <=> submapa): ${if (mismatches.isEmpty()) "SE CUMPLE" else "FALLA en ${mismatches.size}"}")
        mismatches.take(6).forEach {
            println("    translevel=${it.levelNumber} onMainMap=${it.onMainMap} name=${it.name}")
        }

        // 3) Regla VANILLA de $05:D908: si translevel >= 0x25 -> nivel = translevel - 0x24
        //    y el byte alto lo pone el estar en un submapa.
        fun levelOf(t: Int, onMain: Boolean): Int =
            (if (onMain) 0 else 0x100) or (if (t >= 0x25) t - 0x24 else t)

        val mapLevels = ow.map { levelOf(it.levelNumber, it.onMainMap) }.toSortedSet()
        println("\n=== NIVELES DE MAPA (destino de una casilla): ${mapLevels.size} ===")
        println("  " + mapLevels.joinToString(",") { "%03X".format(it) })

        // 4) Cierre transitivo por SALIDAS: los sub-niveles (cuartos) también son niveles
        //    reales, aunque no tengan casilla en el mapa.
        val reachable = sortedSetOf<Int>()
        val queue = ArrayDeque(mapLevels)
        reachable += mapLevels
        while (queue.isNotEmpty()) {
            val lv = queue.removeFirst()
            val conn = SmwLevelExits.read(rom, header, lv) ?: continue
            for (e in conn.exits) {
                val d = e.destinationLevel
                if (d in 0 until 0x200 && reachable.add(d)) queue.addLast(d)
            }
        }
        println("\n=== ALCANZABLES (mapa + sub-niveles por salidas): ${reachable.size} ===")
        val subLevels = reachable - mapLevels
        println("  sub-niveles añadidos: ${subLevels.size}")
        println("  " + reachable.joinToString(",") { "%03X".format(it) })

        // 5) Nombres: cuántas casillas tienen nombre real.
        val named = ow.count { !it.name.isNullOrBlank() }
        println("\n=== casillas con nombre real: $named de ${ow.size}")

        // 6) SALIDAS: por translevel, juntando el nivel de mapa y TODOS sus sub-niveles.
        //    El juego marca bit0 = salida normal y bit1 = secreta (OwLvFlags), así que el
        //    total de "exits" es la suma de banderines distintos.
        var totalExits = 0
        val sinMeta = ArrayList<String>()
        var conDos = 0
        for (t in ow) {
            val root = levelOf(t.levelNumber, t.onMainMap)
            val seen = sortedSetOf(root)
            val q = ArrayDeque(listOf(root))
            while (q.isNotEmpty()) {
                val lv = q.removeFirst()
                val c = SmwLevelExits.read(rom, header, lv) ?: continue
                for (e in c.exits) {
                    val d = e.destinationLevel
                    if (d in 0 until 0x200 && seen.add(d)) q.addLast(d)
                }
            }
            val kinds = seen.flatMap { SmwLevelGoal.goalCellsWithKind(rom, delta, it) }
                .map { it.kind }.toSet()
            totalExits += kinds.size
            if (kinds.isEmpty()) sinMeta.add("%02X %s".format(t.levelNumber, t.name?.trim()))
            if (kinds.size == 2) conDos++
        }
        // 6b) ¿POR QUÉ salen 215? ¿Están conectados entre sí, o el cierre se escapa?
        run {
            val parents = HashMap<Int, MutableSet<Int>>()   // nivel -> quién lleva a él
            val depth = HashMap<Int, Int>()
            for (l in mapLevels) depth[l] = 0
            val q2 = ArrayDeque(mapLevels)
            val vis = sortedSetOf<Int>().also { it += mapLevels }
            while (q2.isNotEmpty()) {
                val lv = q2.removeFirst()
                val c = SmwLevelExits.read(rom, header, lv) ?: continue
                for (e in c.exits) {
                    val d = e.destinationLevel
                    if (d !in 0 until 0x200) continue
                    parents.getOrPut(d) { mutableSetOf() } += lv
                    if (vis.add(d)) { depth[d] = (depth[lv] ?: 0) + 1; q2.addLast(d) }
                }
            }
            val subs = vis - mapLevels
            println("\n=== ¿ESTÁN CONECTADOS ENTRE SÍ? ===")
            println("  sub-niveles: ${subs.size}")
            println("  compartidos por 2+ niveles padre: ${subs.count { (parents[it]?.size ?: 0) > 1 }}")
            val byDepth = vis.groupBy { depth[it] ?: -1 }.toSortedMap()
            println("  por profundidad desde una casilla del mapa: " +
                byDepth.entries.joinToString(", ") { "d${it.key}=${it.value.size}" })
            // Niveles de MAPA a los que además se llega por una tubería/puerta de otro.
            val mapAsSub = mapLevels.filter { (parents[it]?.size ?: 0) > 0 }
            println("  niveles de MAPA a los que también se llega desde otro nivel: ${mapAsSub.size}")
            // ¿Se cuela basura? Un hueco sin objetos no es un nivel.
            var vacios = 0; var nulos = 0; var conObjetos = 0
            for (lv in vis) {
                val tm = SmwLayer1.parse(rom, delta, lv)
                if (tm == null) nulos++ else if (tm.totalObjects == 0) vacios++ else conObjetos++
            }
            println("  con objetos=$conObjetos  VACÍOS(sospechosos)=$vacios  no parseables(verticales)=$nulos")
            val huerfanosVacios = vis.filter { lv ->
                val tm = SmwLayer1.parse(rom, delta, lv); tm != null && tm.totalObjects == 0
            }
            if (huerfanosVacios.isNotEmpty()) {
                println("  vacíos: " + huerfanosVacios.joinToString(",") { "%03X".format(it) })
                println("  --- ficha de cada vacío ---")
                for (lv in huerfanosVacios) {
                    val tm = SmwLayer1.parse(rom, delta, lv)!!
                    val spr = runCatching { SnesGameRecipes.smwLevelEnemies(rom, header, lv).size }.getOrElse { -1 }
                    val padres = parents[lv]?.sorted()?.joinToString(",") { "%03X".format(it) } ?: "-"
                    val sec = parents[lv]?.any { p ->
                        SmwLevelExits.read(rom, header, p)?.exits
                            ?.any { it.destinationLevel == lv && it.isSecondary } == true
                    } ?: false
                    println("    %03X modo=%d pantallas=%d sprites=%d porSalidaSecundaria=%s padres=%s"
                        .format(lv, tm.mode, tm.screens, spr, sec, padres))
                }
            }
        }

        println("\n=== SALIDAS contadas por sprite de meta: $totalExits")
        println("  translevels con DOS salidas (normal+secreta): $conDos")
        println("  translevels SIN sprite de meta: ${sinMeta.size}")
        sinMeta.forEach { println("    $it") }
    }

    private fun findRom(): ByteArray? {
        val candidates = listOf(System.getenv("SMW_ROM"), "/home/user/role-builder/smw.sfc")
        for (p in candidates) if (p != null && File(p).isFile) return File(p).readBytes()
        return null
    }
}
