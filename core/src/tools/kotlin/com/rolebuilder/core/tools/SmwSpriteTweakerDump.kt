package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSpriteBehaviorReader
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File

/**
 * Vuelca el COMPORTAMIENTO (tweaker) de cada sprite de SMW: los seis bytes de propiedades
 * ($1656/$1662/$166E/$167A/$1686/$190F) que el motor copia a la RAM del sprite al aparecer
 * (`InitializeNormalSpriteRAMTables_PropertyTables`, $07:F7A0). Son los mismos seis bytes que
 * edita el "tweaker" de Lunar Magic. Se leen 1:1 de la ROM (via [SmwSpriteBehaviorReader]).
 *
 * Se documenta el dato FIEL (los seis bytes crudos) más los dos índices de hitbox que el propio
 * código del juego usa sin ambigüedad: object-clip ($1656 & 0x0F) y sprite-clip ($1662 & 0x3F).
 * Los roles a nivel de byte están descritos en `SmwSpriteBehavior`; la interpretación bit a bit
 * de las banderas restantes se deja a quien implemente un motor de comportamiento completo.
 *
 * Uso:  gradle :core:dumpSpriteTweaker --args="--rom smw.sfc [--out docs/comportamiento_sprites.md]"
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
    val out = File(opts["out"] ?: "docs/comportamiento_sprites.md").also { it.parentFile?.mkdirs() }

    val behaviors = SmwSpriteBehaviorReader.read(rom, header) ?: run {
        System.err.println("La ROM no cubre las tablas tweaker o no parece SMW vanilla"); return
    }
    val bigIds = SmwEnemyGraphics.bigSprites.map { it.id }.toSet()
    val smallIds = SmwEnemyGraphics.curatedIds.toSet()
    fun tag(id: Int) = when { id in bigIds -> "[B]"; id in smallIds -> "[s]"; else -> "[ ]" }
    fun h2(v: Int) = v.toString(16).uppercase().padStart(2, '0')

    val sb = StringBuilder()
    sb.appendLine("# Comportamiento (tweaker) de los sprites de SMW")
    sb.appendLine()
    sb.appendLine("Los seis bytes de propiedades por id de sprite (0x00–0xC8), leídos de la ROM real")
    sb.appendLine("(`InitializeNormalSpriteRAMTables_PropertyTables`, \$07:F7A0). Son el dato fiel; los")
    sb.appendLine("nombres de sprite salen de `SmwSpriteNames`. `[B]` = sprite grande reconstruido ·")
    sb.appendLine("`[s]` = roster pequeño · `[ ]` = solo comportamiento (sin gráfico reconstruido).")
    sb.appendLine()
    sb.appendLine("## Papel de cada byte (de `SmwSpriteBehavior`)")
    sb.appendLine()
    sb.appendLine("| Byte | RAM | Papel |")
    sb.appendLine("|------|-----|-------|")
    sb.appendLine("| 1656 | \$7E:1656 | banderas varias (interacción con jugador/objetos/estrella); bits 0-3 = **object-clip** (hitbox con objetos) |")
    sb.appendLine("| 1662 | \$7E:1662 | bits 0-5 = **sprite-clip** (hitbox con jugador/sprites); resto banderas |")
    sb.appendLine("| 166E | \$7E:166E | banderas de proceso (fuera de pantalla, dirección inicial…); nibble bajo → página/paleta de tesela (`spr_table15f6`) |")
    sb.appendLine("| 167A | \$7E:167A | banderas de interacción (con jugador, capa, otros sprites) |")
    sb.appendLine("| 1686 | \$7E:1686 | más banderas (gravedad, no interacción con objetos…) |")
    sb.appendLine("| 190F | \$7E:190F | banderas extra (inmunidades, comportamiento especial) |")
    sb.appendLine()
    sb.appendLine("## Tabla por sprite")
    sb.appendLine()
    sb.appendLine("| id | Sprite | 1656 | 1662 | 166E | 167A | 1686 | 190F | obj-clip | spr-clip |")
    sb.appendLine("|----|--------|------|------|------|------|------|------|----------|----------|")
    for (b in behaviors) {
        val name = SmwSpriteNames.nameOf(b.id)
        sb.appendLine(
            "| ${tag(b.id)} 0x${h2(b.id)} | $name | ${h2(b.b1656)} | ${h2(b.b1662)} | ${h2(b.b166e)} " +
                "| ${h2(b.b167a)} | ${h2(b.b1686)} | ${h2(b.b190f)} | ${b.b1656 and 0x0F} | ${b.spriteClipping} |"
        )
    }
    out.writeText(sb.toString())
    println("Informe: ${out.absolutePath}")
    println("Sprites documentados: ${behaviors.size}")
}
