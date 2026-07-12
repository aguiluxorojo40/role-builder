package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSprites
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File

/**
 * Documentación EXHAUSTIVA de los niveles de SMW extraída de la ROM: por cada nivel, sus
 * DIRECCIONES ROM (punteros de Layer 1/sprites, cabecera, ranura de GFX de sprites), sus
 * propiedades (ancho, modo, música, tiempo, paletas, tileset), qué ficheros GFX de sprites
 * sube (SP1-4) y la lista de ENEMIGOS con su posición, nombre canónico y si es un sprite
 * GRANDE reconstruido. Pensado para construir/contrastar niveles con todo documentado.
 *
 * Uso:  gradle :core:dumpLevelDocs --args="--rom smw.sfc [--out docs/niveles_smw.md]"
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
    val out = File(opts["out"] ?: "docs/niveles_smw.md").also { it.parentFile?.mkdirs() }

    val bigIds = SmwEnemyGraphics.bigSprites.associateBy { it.id }
    val smallIds = SmwEnemyGraphics.curatedIds.toSet()
    fun tag(id: Int) = when { id in bigIds -> "[B]"; id in smallIds -> "[s]"; else -> "[ ]" }
    fun hx(v: Int?, w: Int = 0) = if (v == null) "—" else "0x" + v.toString(16).uppercase().padStart(w, '0')

    val sb = StringBuilder()
    sb.appendLine("# Documentación de niveles de SMW (extraída de la ROM)")
    sb.appendLine()
    sb.appendLine("Todo sale de `Super Mario World (USA)` leído directamente. Direcciones en PC")
    sb.appendLine("(offset de fichero). `[B]` = enemigo con sprite grande reconstruido · `[s]` = roster")
    sb.appendLine("pequeño · `[ ]` = aún sin reconstruir. \"Usa grandes\" = el nivel coloca algún sprite")
    sb.appendLine("multi-tesela (los reconstruidos van marcados).")
    sb.appendLine()
    sb.appendLine("## Tablas de referencia (direcciones PC)")
    sb.appendLine()
    for ((name, pc) in SnesGameRecipes.smwReferenceTables()) sb.appendLine("- **$name**: ${hx(pc, 5)}")
    sb.appendLine()
    sb.appendLine("Punteros por nivel: Layer1 = tablaL1 + 3·nivel (3 bytes) · Sprites = tablaSpr + 2·nivel")
    sb.appendLine("(2 bytes → dirección en banco \$07) · GFX sprites = tablaSP + 4·(spriteGfx del nivel).")
    sb.appendLine()

    var count = 0
    val perLevel = StringBuilder()
    for (level in 0x000..0x1FF) {
        val info = SnesGameRecipes.smwLevelInfo(rom, header, level) ?: continue
        val placements = SmwSprites.placements(rom, delta, level)
        // Solo niveles con contenido real: cabecera válida y (sprites o ancho > 1 pantalla).
        if (placements.isEmpty() && info.screens <= 1) continue
        count++
        val addr = SnesGameRecipes.smwLevelAddresses(rom, header, level)
        val gfx = addr.spriteGfxFiles?.joinToString(" ") {
            if (it == 0x7F) "--" else it.toString(16).uppercase().padStart(2, '0')
        } ?: "—"

        perLevel.appendLine("### Nivel ${hx(level, 3)}")
        perLevel.appendLine("- **Direcciones**: L1ptr ${hx(addr.layer1PtrTablePc, 5)} → header ${hx(addr.headerPc, 5)}" +
            " · SprPtr ${hx(addr.spritePtrTablePc, 5)} → stream ${hx(addr.spriteStreamPc, 5)}" +
            " · GFXslot ${hx(addr.spriteGfxSlotPc, 5)}")
        perLevel.appendLine("- **GFX sprites (SP1-4)**: `$gfx`  ·  spriteGfx=${info.spriteGfx}")
        perLevel.appendLine("- **Propiedades**: ancho ${info.screens} pantallas (${info.widthTiles} casillas)" +
            " · modo ${hx(info.levelMode)} · música ${info.musicIndex} · tiempo ${info.startTime}" +
            " · paletas BG=${info.bgPalette} FG=${info.fgPalette} SPR=${info.spritePalette}" +
            " backArea=${info.backgroundColor} · tilesetFG=${info.fgTileset}")
        // Enemigos agregados por id, con posiciones.
        val byId = placements.groupBy { it.id }
        val big = byId.keys.filter { it in bigIds }
        perLevel.appendLine("- **Usa sprites grandes**: ${if (big.isEmpty()) "no" else "sí — " +
            big.joinToString(", ") { "${SmwSpriteNames.nameOf(it)} (${hx(it)})" }}")
        if (placements.isEmpty()) {
            perLevel.appendLine("- **Enemigos**: (ninguno colocado)")
        } else {
            perLevel.appendLine("- **Enemigos (${placements.size})**:")
            for ((id, ps) in byId.entries.sortedBy { it.key }) {
                val pos = ps.take(6).joinToString(" ") { "(${it.screen},${it.xTile},${it.yTile})" } +
                    if (ps.size > 6) " …" else ""
                perLevel.appendLine("    - ${tag(id)} **${SmwSpriteNames.nameOf(id)}** (${hx(id)}) ×${ps.size}: $pos")
            }
        }
        perLevel.appendLine()
    }

    sb.appendLine("## Niveles con datos: $count")
    sb.appendLine()
    sb.append(perLevel)
    out.writeText(sb.toString())
    println("Informe: ${out.absolutePath}")
    println("Niveles documentados: $count")
}
