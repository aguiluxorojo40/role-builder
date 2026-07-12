package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SnesDecoder
import java.io.File

/**
 * Documenta la capa ESTÁTICA del overworld (mapa del mundo) de SMW leída de la ROM: la
 * red de tuberías del Star Road (origen→destino) y el evento de mapa que dispara cada
 * translevel al superarse. El tilemap del mapa (submapas, caminos, posición de cada nivel)
 * va comprimido y queda para una fase aparte; esto es lo directamente legible.
 *
 * Uso:  gradle :core:dumpOverworldDoc --args="--rom smw.sfc [--out docs/overworld_smw.md]"
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
    val out = File(opts["out"] ?: "docs/overworld_smw.md").also { it.parentFile?.mkdirs() }

    // Conversión SNES(LoROM)→PC: las direcciones del inventario son SNES (banco en la parte alta).
    fun pc(snes: Int) = (snes shr 16) * 0x8000 + (snes and 0x7FFF) + delta
    fun u8(snes: Int) = rom[pc(snes)].toInt() and 0xFF
    fun u16(snes: Int) = u8(snes) or (u8(snes + 1) shl 8)
    fun hx(v: Int, w: Int = 0) = "0x" + v.toString(16).uppercase().padStart(w, '0')

    // Direcciones (PC) del inventario de snesrev/smw.
    val EVENTS = 0x5D608          // kLoadLevel_DATA_05D608[256]: evento por translevel
    val STAR_SRCX = 0x48431       // kOwStarPipeWarp_SrcX[27]
    val STAR_SRCY = 0x48467
    val STAR_DSTX = 0x4849D
    val STAR_DSTY = 0x484D3
    val OW_SPRITE_SLOTS = 0x4F625 // kLoadOverworldSprites_SpriteSlotData[65]

    val sb = StringBuilder()
    sb.appendLine("# Overworld de SMW — capa estática (extraída de la ROM)")
    sb.appendLine()
    sb.appendLine("El mapa del mundo es un dominio aparte de los datos de nivel. Aquí van las tablas")
    sb.appendLine("estáticas legibles; el tilemap de submapas/caminos (comprimido) es una fase futura.")
    sb.appendLine()
    sb.appendLine("## Direcciones (SNES, del inventario)")
    sb.appendLine("- Evento por translevel (`kLoadLevel_DATA_05D608`): ${hx(EVENTS, 5)} (256 bytes)")
    sb.appendLine("- Star Road warps (`kOwStarPipeWarp_*`): SrcX ${hx(STAR_SRCX, 5)} · SrcY ${hx(STAR_SRCY, 5)}" +
        " · DstX ${hx(STAR_DSTX, 5)} · DstY ${hx(STAR_DSTY, 5)} (27×2 bytes cada una)")
    sb.appendLine("- Sprites de mapa (`kLoadOverworldSprites_SpriteSlotData`): ${hx(OW_SPRITE_SLOTS, 5)} (65 bytes)")
    sb.appendLine("- Map16 overworld L1: 0x5D000 · tilemap OW L2/eventos: banco \$0C (comprimidos)")
    sb.appendLine()

    // --- Star Road: red de tuberías origen → destino ---
    sb.appendLine("## Star Road — red de tuberías (27 warps)")
    sb.appendLine()
    sb.appendLine("Posiciones en coordenadas de casilla del overworld (los altos indican submapa).")
    sb.appendLine()
    sb.appendLine("| # | Origen (X,Y) | Destino (X,Y) |")
    sb.appendLine("|---|--------------|----------------|")
    for (n in 0 until 27) {
        val sx = u16(STAR_SRCX + 2 * n); val sy = u16(STAR_SRCY + 2 * n)
        val dx = u16(STAR_DSTX + 2 * n); val dy = u16(STAR_DSTY + 2 * n)
        sb.appendLine("| $n | (${hx(sx)}, ${hx(sy)}) | (${hx(dx)}, ${hx(dy)}) |")
    }
    sb.appendLine()

    // --- Evento por translevel ---
    sb.appendLine("## Evento de mapa por translevel (al superar el nivel)")
    sb.appendLine()
    sb.appendLine("`evento[translevel]` = nº de evento del overworld que se dispara al pasar ese nivel")
    sb.appendLine("(revela caminos/niveles nuevos). Translevel 0x00-0x5F son los niveles del mapa.")
    sb.appendLine()
    val line = StringBuilder("    ")
    for (t in 0x00..0x5F) {
        val e = u8(EVENTS + t)
        line.append("%02X→%02X ".format(t, e))
        if ((t and 0x0F) == 0x0F) { sb.appendLine(line.toString()); line.setLength(0); line.append("    ") }
    }
    if (line.isNotBlank()) sb.appendLine(line.toString())
    sb.appendLine()
    sb.appendLine("(Traducción a texto de nombres y posición en el mapa: requiere el tilemap del")
    sb.appendLine("overworld; ver `docs/auditoria_cobertura_smw.md`.)")

    out.writeText(sb.toString())
    println("Informe: ${out.absolutePath}")
}
