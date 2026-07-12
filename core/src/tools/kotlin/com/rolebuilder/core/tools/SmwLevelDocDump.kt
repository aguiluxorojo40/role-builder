package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SmwSolidity
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
        fun files(a: IntArray?) = a?.joinToString(" ") {
            if (it == 0x7F) "--" else it.toString(16).uppercase().padStart(2, '0')
        } ?: "—"

        // Colisión (decodifica Layer 1): dimensiones + histograma de solidez.
        val col = runCatching { SnesGameRecipes.smwLevelCollision(rom, header, level) }.getOrNull()
        val colTxt = if (col == null) "— (sin colisión reconstruible)" else {
            val h = col.solidity.groupingBy { it }.eachCount()
            val nonEmpty = SmwSolidity.entries.filter { it != SmwSolidity.NONE && (h[it] ?: 0) > 0 }
                .joinToString(" ") { "${it.name}=${h[it]}" }
            "${col.cols}×${col.rows} casillas · $nonEmpty"
        }
        // Entrada + cabecera secundaria decodificada.
        val start = runCatching { SmwLevelStartReader.read(rom, header, level) }.getOrNull()
        val startTxt = if (start == null) "—" else
            "casilla (${start.startTileX},${start.startTileY}) px (${start.startPixelX},${start.startPixelY})" +
                " · pantalla entrada ${start.entranceScreen}" +
                (if (start.vertical) " · **VERTICAL**" else "") +
                (if (start.disableNoYoshiIntro) " · no-Yoshi" else "") +
                " · L2scroll ${start.layer2ScrollSetting} L3 ${start.layer3Setting}" +
                " L1y ${start.layer1YSetting} L2y ${start.layer2YSetting}" +
                " · secHdr [${start.secHeader.joinToString(" ") { hx(it) }}]"
        // Byte de cabecera de la lista de sprites (memoria + buoyancy).
        val sprHdr = SmwSprites.spriteHeaderByte(rom, delta, level)
        val sprHdrTxt = if (sprHdr == null) "—" else
            "${hx(sprHdr)} (memoria ${hx(sprHdr and 0x1F)}, buoyancy ${hx(sprHdr and 0xC0)})"

        // Tipo: nivel de mapa (overworld) vs sublevel/secundario. Los niveles del mapa
        // son translevel 0x00-0x5F → leveldata 0x000-0x024 y 0x101-0x13B (96 = las 96 salidas).
        val translevel = when {
            level <= 0x024 -> level
            level in 0x101..0x13B -> level - 0xDC
            else -> null
        }
        val tipo = if (translevel != null)
            "nivel de MAPA (translevel ${hx(translevel)})"
        else "sublevel / sala secundaria (no aparece en el mapa; se entra por salida de pantalla)"

        val levelName = SmwLevelNames.nameOf(rom, delta, level)
        perLevel.appendLine("### Nivel ${hx(level, 3)}${if (levelName != null) " — $levelName" else ""}")
        perLevel.appendLine("- **Nombre (overworld)**: ${levelName ?: "— (sublevel, sin nombre de mapa)"}")
        perLevel.appendLine("- **Tipo**: $tipo")
        perLevel.appendLine("- **Direcciones**: L1ptr ${hx(addr.layer1PtrTablePc, 5)} → header ${hx(addr.headerPc, 5)}" +
            " · SprPtr ${hx(addr.spritePtrTablePc, 5)} → stream ${hx(addr.spriteStreamPc, 5)}" +
            " · L2ptr ${hx(addr.layer2PtrTablePc, 5)} · GFXslot ${hx(addr.spriteGfxSlotPc, 5)}" +
            " · FGBGslot ${hx(addr.fgbgGfxSlotPc, 5)}")
        perLevel.appendLine("- **GFX sprites (SP1-4)**: `${files(addr.spriteGfxFiles)}` (spriteGfx=${info.spriteGfx}) · " +
            "**GFX FG/BG [FG1 FG2 BG1 FG3]**: `${files(addr.fgbgGfxFiles)}` (tilesetFG=${info.fgTileset})")
        perLevel.appendLine("- **Propiedades**: ancho ${info.screens} pantallas (${info.widthTiles} casillas)" +
            " · modo ${hx(info.levelMode)} · música ${info.musicIndex} · tiempo ${info.startTime}" +
            " · Layer2 ${if (addr.layer2IsBackground) "fondo" else "nivel"}" +
            " · paletas BG=${info.bgPalette} FG=${info.fgPalette} SPR=${info.spritePalette}" +
            " backArea=${info.backgroundColor}")
        perLevel.appendLine("- **Colisión**: $colTxt")
        perLevel.appendLine("- **Entrada**: $startTxt")
        perLevel.appendLine("- **Cabecera sprites**: $sprHdrTxt")
        // Salidas de pantalla (grafo nivel → sublevel/nivel destino).
        val exits = runCatching { SmwLayer1.screenExits(rom, delta, level) }.getOrNull() ?: emptyList()
        val exitsTxt = if (exits.isEmpty()) "(ninguna)" else exits.joinToString(" · ") {
            val full = (level and 0x100) or it.destination
            "pant ${it.screen}→${hx(full, 3)}${if (it.secondaryEntrance) " (2ª entrada)" else ""}"
        }
        perLevel.appendLine("- **Salidas de pantalla**: $exitsTxt")
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
                val pos = ps.take(6).joinToString(" ") {
                    "(${it.screen},${it.xTile},${it.yTile}${if (it.extraBits != 0) " EE${it.extraBits}" else ""})"
                } + if (ps.size > 6) " …" else ""
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
