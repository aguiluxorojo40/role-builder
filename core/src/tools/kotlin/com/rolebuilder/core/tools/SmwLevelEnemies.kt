package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File

/**
 * Vuelca QUÉ ENEMIGOS/SPRITES hay en CADA NIVEL de Super Mario World, leyendo la lista de
 * sprites real de la ROM (la 3ª capa de datos de cada nivel). Genera un informe markdown:
 * por nivel, los sprites que aparecen con su nombre canónico y cuántos; y un índice
 * inverso (cada enemigo → en qué niveles sale). Marca [B]/[s] los ya reconstruidos.
 *
 * Uso:  gradle :core:dumpLevelEnemies --args="--rom smw.sfc [--out docs/enemigos_por_nivel.md]"
 */
fun main(args: Array<String>) {
    val opts = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (a.startsWith("--")) opts[a.substring(2)] =
            if (i + 1 < args.size && !args[i + 1].startsWith("--")) args[++i] else "true"
        i++
    }
    val rom = File(opts["rom"] ?: run { System.err.println("Falta --rom"); return }).readBytes()
    val header = SnesDecoder.parseHeader(rom)
    val out = File(opts["out"] ?: "docs/enemigos_por_nivel.md")
    out.parentFile?.mkdirs()

    val bigIds = SmwEnemyGraphics.bigSprites.map { it.id }.toSet()
    val smallIds = SmwEnemyGraphics.curatedIds.toSet()
    fun tag(id: Int) = when (id) { in bigIds -> "[B]"; in smallIds -> "[s]"; else -> "[ ]" }

    val perLevel = LinkedHashMap<Int, List<Int>>()
    val inverse = sortedMapOf<Int, MutableList<Int>>()
    for (lv in 0x000..0x1FF) {
        val ids = SmwEnemyGraphics.spritesInLevel(rom, header, lv)
        if (ids.isEmpty()) continue
        perLevel[lv] = ids
        for (id in ids.toSet()) inverse.getOrPut(id) { ArrayList() }.add(lv)
    }

    val sb = StringBuilder()
    sb.appendLine("# Enemigos por nivel de SMW (lista de sprites real de la ROM)")
    sb.appendLine()
    sb.appendLine("Generado con `:core:dumpLevelEnemies` leyendo la lista de sprites de cada nivel.")
    sb.appendLine("`[B]` = sprite grande reconstruido · `[s]` = en el roster pequeño · `[ ]` = falta.")
    sb.appendLine("Niveles con datos: ${perLevel.size}.")
    sb.appendLine()
    sb.appendLine("## Por nivel")
    sb.appendLine()
    for ((lv, ids) in perLevel) {
        val counts = ids.groupingBy { it }.eachCount()
        val parts = counts.entries.sortedBy { it.key }.joinToString(", ") { (id, n) ->
            "${tag(id)} ${SmwSpriteNames.nameOf(id)}${if (n > 1) " ×$n" else ""}"
        }
        sb.appendLine("- **0x${lv.toString(16).uppercase().padStart(3, '0')}**: $parts")
    }
    sb.appendLine()
    sb.appendLine("## Índice inverso (enemigo → niveles)")
    sb.appendLine()
    for ((id, levels) in inverse) {
        val lvs = levels.joinToString(" ") { "0x" + it.toString(16).uppercase() }
        sb.appendLine("- ${tag(id)} **${SmwSpriteNames.nameOf(id)}** (0x${id.toString(16).uppercase()}): ${levels.size} niveles — $lvs")
    }
    out.writeText(sb.toString())
    println("Informe: ${out.absolutePath}")
    println("Niveles con enemigos: ${perLevel.size} · enemigos distintos: ${inverse.size}")
    // Resumen a consola: los enemigos más frecuentes.
    println("\nEnemigos más comunes:")
    inverse.entries.sortedByDescending { it.value.size }.take(15).forEach { (id, lv) ->
        println("  %-3s %-24s en %3d niveles".format(tag(id), SmwSpriteNames.nameOf(id), lv.size))
    }
}
