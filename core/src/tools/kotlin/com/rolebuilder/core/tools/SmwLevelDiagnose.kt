package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SmwSprites
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File

/**
 * DIAGNÓSTICO / CHECKLIST de un nivel de SMW: dice si el nivel se puede IMPORTAR como proyecto
 * jugable y, si no, exactamente QUÉ falta (qué objetos de Layer 1 no están portados, con su
 * nombre del decomp; qué enemigos no tienen gráfico). Es el instrumento del flujo de trabajo:
 * diagnosticar → portar lo que falte → volver a diagnosticar hasta verde → importar → probar.
 *
 * Uso:  gradle :core:diagnoseSmwLevel --args="--rom smw.sfc --level 0x105"
 */
private val TILESET_NAME = mapOf(
    0 to "grassland", 1 to "castle", 2 to "rope", 3 to "underground", 4 to "ghost house",
    5 to "ghost house", 6 to "rope", 7 to "grassland", 8 to "rope", 9 to "underground",
    10 to "underground", 11 to "underground", 12 to "grassland", 13 to "ghost house", 14 to "underground",
)

// Nombres de objeto (del decomp snesrev/smw) para los códigos que suelen faltar.
private val STD_OBJ_NAME = mapOf(
    0x1D to "ClimbingNetWithBottomEdge", 0x1E to "ClimbingNetWithSideEdge",
    0x1F to "SkinnyVerticalPipeBoneLog", 0x20 to "SkinnyHorizontalPipeBoneLog",
    0x30 to "obj tileset 0x30", 0x31 to "obj tileset 0x31", 0x32 to "SwitchBlocks",
    0x38 to "obj tileset 0x38", 0x39 to "Grass RightFacingDiagonalPipe",
)
private val EXT_OBJ_NAME = mapOf(
    0x49 to "MidwayPoint", 0x4A to "ClimbingNetDoor", 0x4B to "BigBush?",
    0x80 to "GhostHouseEntrance", 0x81 to "Seaweed", 0x84 to "CastleEntrance",
    0x85 to "YoshisHouse", 0x86 to "GoalSign", 0x8D to "SwitchPalaceSwitch",
    0x8E to "YellowSwitchBlock", 0x8F to "GhostHouseWindow", 0x90 to "LargeBossDoor",
    0x97 to "SwitchPalaceEdgeTile",
)

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
    val level = (opts["level"] ?: "0x105").removePrefix("0x").toInt(16)
    val header = SnesDecoder.parseHeader(rom)
    val delta = header.headerOffset - 0x7FC0
    fun hx(v: Int?, w: Int = 0) = if (v == null) "—" else "0x" + v.toString(16).uppercase().padStart(w, '0')

    val name = SmwLevelNames.nameOf(rom, delta, level) ?: "(sublevel sin nombre)"
    val info = SnesGameRecipes.smwLevelInfo(rom, header, level)
    val cov = SmwLayer1.objectCoverage(rom, delta, level)

    val sb = StringBuilder()
    sb.appendLine("# Diagnóstico de nivel ${hx(level, 3)} — $name")
    sb.appendLine()

    if (cov == null || info == null) {
        sb.appendLine("## Veredicto: ❌ sin Layer 1 parseable (modo sin objetos o datos ausentes)")
        print(sb); return
    }

    // Gate REAL de extractSmwLevelAsMap: importable si total>0 y unknown*10 <= total.
    val importable = cov.totalObjects > 0 && cov.unknownObjects * 10 <= cov.totalObjects
    val pct = if (cov.totalObjects > 0) cov.unknownObjects * 100 / cov.totalObjects else 0
    sb.appendLine("## Veredicto: ${if (importable) "✅ IMPORTABLE" else "⚠️ NO importable aún"}")
    sb.appendLine("Objetos no portados: ${cov.unknownObjects}/${cov.totalObjects} ($pct%). " +
        "Umbral: ≤10%. ${if (importable) "" else "Faltan bajar a ≤ ${cov.totalObjects / 10}."}")
    sb.appendLine()

    // --- Cabecera ---
    sb.appendLine("## Cabecera")
    sb.appendLine("- Modo ${cov.mode}${if (cov.vertical) " (VERTICAL)" else " (horizontal)"} · " +
        "tileset ${cov.tileset} (${TILESET_NAME[cov.tileset] ?: "?"}) · " +
        "ancho ${info.screens} pantallas · música ${info.musicIndex}")
    sb.appendLine("- Paletas BG=${info.bgPalette} FG=${info.fgPalette} SPR=${info.spritePalette}")
    sb.appendLine()

    // --- Layer 1 ---
    sb.appendLine("## Layer 1 (objetos): ${cov.recognized}/${cov.totalObjects} reconocidos")
    if (cov.unknownStd.isEmpty() && cov.unknownExt.isEmpty()) {
        sb.appendLine("- ✅ Todos los objetos portados.")
    } else {
        sb.appendLine("- Objetos que FALTAN por portar:")
        for ((code, n) in cov.unknownStd.entries.sortedByDescending { it.value }) {
            sb.appendLine("    - [std] ${hx(code)} ${STD_OBJ_NAME[code] ?: "?"} ×$n")
        }
        for ((code, n) in cov.unknownExt.entries.sortedByDescending { it.value }) {
            sb.appendLine("    - [ext] ${hx(code)} ${EXT_OBJ_NAME[code] ?: "?"} ×$n")
        }
    }
    sb.appendLine()

    // --- Colisión ---
    val col = runCatching { SnesGameRecipes.smwLevelCollision(rom, header, level) }.getOrNull()
    sb.appendLine("## Colisión: " + if (col == null) "❌ no reconstruible (¿nivel casi vacío?)"
    else "✅ ${col.cols}×${col.rows} casillas")

    // --- Enemigos ---
    val placements = SmwSprites.placements(rom, delta, level)
    val bigIds = SmwEnemyGraphics.bigSprites.map { it.id }.toSet()
    val smallIds = SmwEnemyGraphics.curatedIds.toSet()
    val withGfx = placements.count { it.id in bigIds || it.id in smallIds }
    val noGfx = placements.filter { it.id !in bigIds && it.id !in smallIds }.map { it.id }.toSortedSet()
    sb.appendLine("## Enemigos: ${placements.size} colocados · con gráfico $withGfx · sin gráfico ${noGfx.size}")
    if (noGfx.isNotEmpty()) {
        sb.appendLine("- Sin gráfico reconstruido (se instancian pero se dibujan con placeholder):")
        for (id in noGfx) sb.appendLine("    - ${hx(id)} ${SmwSpriteNames.nameOf(id)}")
    }

    // --- Otros dominios ---
    val l2 = SnesGameRecipes.smwLayer2Info(rom, header, level)
    sb.appendLine("## Layer 2: " + if (l2.isBackground) "fondo ${hx(l2.backgroundSourceSnes, 6)}" else "objetos ${hx(l2.objectBank)}:${hx(l2.addrSnes, 4)}")
    sb.appendLine("## Nombre: ${if (SmwLevelNames.nameOf(rom, delta, level) != null) "✅ \"$name\"" else "— (sublevel)"}")
    sb.appendLine()

    // --- Resumen accionable ---
    val toPort = cov.unknownStd.keys.map { "std ${hx(it)}" } + cov.unknownExt.keys.map { "ext ${hx(it)}" }
    sb.appendLine("## Para importar este nivel falta:")
    if (importable) sb.appendLine("- Nada — ya pasa el gate. Ejecuta `importSmwLevel --level ${hx(level)}`.")
    else sb.appendLine("- Portar ${toPort.size} rutina(s) de objeto: ${toPort.joinToString(", ")}")

    val out = opts["out"]
    if (out != null) { File(out).also { it.parentFile?.mkdirs() }.writeText(sb.toString()); println("Informe: $out") }
    print(sb)
}
