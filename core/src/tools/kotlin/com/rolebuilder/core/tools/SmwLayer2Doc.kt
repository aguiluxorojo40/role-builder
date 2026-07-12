package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File

/**
 * Documenta el LAYER 2 de cada nivel de SMW: si es una IMAGEN de fondo (tilemap comprimido en
 * banco $0C, `is_bg` = banco 0xFF → remapeado a $0C) o una 2ª capa de OBJETOS (puntero de 24
 * bits en su banco, se parsea igual que Layer 1). Agrupa los niveles por fuente de fondo para
 * ver qué niveles COMPARTEN el mismo fondo. Regla de clasificación tomada 1:1 del extractor de
 * assets de snesrev/smw (`add_packed_level_bg`): banco 0xFF ⇒ fondo; cualquier otro ⇒ objetos.
 *
 * Uso:  gradle :core:dumpLayer2Doc --args="--rom smw.sfc [--out docs/fondos_layer2_smw.md]"
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
    val delta = header.headerOffset - 0x7FC0
    val out = File(opts["out"] ?: "docs/fondos_layer2_smw.md").also { it.parentFile?.mkdirs() }
    fun hx(v: Int?, w: Int = 0) = if (v == null) "—" else "0x" + v.toString(16).uppercase().padStart(w, '0')

    // Solo niveles de MAPA (0x000-0x024 y 0x101-0x13B); ahí es donde el fondo es "el del nivel".
    val mapLevels = (0x000..0x024) + (0x101..0x13B)
    val byBg = LinkedHashMap<Int, MutableList<Int>>()      // fuente $0C → niveles
    val objects = ArrayList<Triple<Int, Int, Int>>()        // nivel, banco, addr

    for (level in mapLevels) {
        val l2 = SnesGameRecipes.smwLayer2Info(rom, header, level)
        if (l2.isBackground) byGroup(byBg, l2.backgroundSourceSnes ?: -1, level)
        else objects.add(Triple(level, l2.objectBank, l2.addrSnes))
    }

    val sb = StringBuilder()
    sb.appendLine("# Layer 2 de SMW — fondos y 2ª capa de objetos (extraído de la ROM)")
    sb.appendLine()
    sb.appendLine("Por cada nivel de MAPA, su Layer 2. **Fondo** = tilemap comprimido en banco \$0C")
    sb.appendLine("(864 bloques Map16, tabla Map16 de fondo \$0D:9100); **objetos** = 2ª capa de")
    sb.appendLine("objetos con puntero de 24 bits (misma estructura que Layer 1). Clasificación:")
    sb.appendLine("banco 0xFF ⇒ fondo (regla de `add_packed_level_bg` de snesrev/smw).")
    sb.appendLine()
    sb.appendLine("## Fondos por fuente (niveles que COMPARTEN fondo)")
    sb.appendLine()
    sb.appendLine("| Fuente (\$0C) | Nº niveles | Niveles (nombre) |")
    sb.appendLine("|--------------|-----------|------------------|")
    for ((src, levels) in byBg.entries.sortedBy { it.key }) {
        val names = levels.joinToString(", ") {
            val n = SmwLevelNames.nameOf(rom, delta, it)
            "${hx(it, 3)}${if (n != null) " ($n)" else ""}"
        }
        sb.appendLine("| ${hx(src, 6)} | ${levels.size} | $names |")
    }
    sb.appendLine()
    sb.appendLine("Fondos distintos: ${byBg.size} · niveles con fondo: ${byBg.values.sumOf { it.size }}")
    sb.appendLine()
    sb.appendLine("## Niveles con Layer 2 de OBJETOS (no fondo)")
    sb.appendLine()
    if (objects.isEmpty()) sb.appendLine("(ninguno)")
    else {
        sb.appendLine("| Nivel | Nombre | Puntero (24 bits) |")
        sb.appendLine("|-------|--------|-------------------|")
        for ((level, bank, addr) in objects) {
            val n = SmwLevelNames.nameOf(rom, delta, level) ?: "—"
            sb.appendLine("| ${hx(level, 3)} | $n | ${hx(bank)}:${hx(addr, 4)} |")
        }
    }
    out.writeText(sb.toString())
    println("Informe: ${out.absolutePath}")
    println("Fondos: ${byBg.size} · niveles con fondo: ${byBg.values.sumOf { it.size }} · objetos: ${objects.size}")
}

private fun byGroup(m: LinkedHashMap<Int, MutableList<Int>>, key: Int, level: Int) {
    m.getOrPut(key) { ArrayList() }.add(level)
}
