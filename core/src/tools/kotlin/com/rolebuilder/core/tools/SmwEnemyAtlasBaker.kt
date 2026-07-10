package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * AÑADE al ATLAS de enemigos empaquetado (`app/src/main/assets/sprites/enemies.png`) los
 * fotogramas de los ids de [SmwEnemyGraphics.curatedIds] que aún no estén en él, tomando
 * sus gráficos de una ROM de SMW. El atlas es una tira horizontal de fotogramas de 16×16,
 * uno por id y EN ESE ORDEN (el que espera el renderer del motor de plataformas).
 *
 * Importante — por qué FUSIONA en vez de rehornear todo: varios enemigos curados (Buzzy,
 * caparazón verde, Rex, topos…) usan gráficos que solo están cargados en CIERTOS niveles;
 * cada fotograma original se horneó en un nivel donde ese sprite se ve bien. Un horneado
 * de nivel ÚNICO los estropearía. Por eso esta tarea CONSERVA intactos los fotogramas ya
 * presentes y solo renderiza (al [--level] dado) los NUEVOS ids añadidos al final del
 * roster. Al ampliar [SmwEnemyGraphics.curatedIds], añade el id al final y elige un
 * [--level] donde ese sprite se dibuje correctamente.
 *
 * Uso:  gradle :core:bakeSmwEnemies --args="--rom RUTA.sfc [--level 0x105] [--out RUTA.png]"
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
    val romPath = opts["rom"] ?: run { System.err.println("Falta --rom <ruta>"); return }
    val rom = File(romPath).readBytes()
    val header = SnesDecoder.parseHeader(rom)
    val level = opts["level"]?.let {
        if (it.startsWith("0x") || it.startsWith("0X")) it.substring(2).toInt(16) else it.toInt()
    } ?: 0x105
    val outPath = opts["out"] ?: "app/src/main/assets/sprites/enemies.png"

    val ids = SmwEnemyGraphics.curatedIds
    val cell = 16
    val outFile = File(outPath).also { it.parentFile?.mkdirs() }

    // Fotogramas ya presentes en el atlas: se conservan tal cual (ver KDoc).
    val existing = if (outFile.isFile) ImageIO.read(outFile) else null
    val keep = (existing?.let { it.width / cell } ?: 0).coerceAtMost(ids.size)

    val atlas = BufferedImage(ids.size * cell, cell, BufferedImage.TYPE_INT_ARGB)
    if (existing != null && keep > 0) {
        atlas.graphics.drawImage(existing.getSubimage(0, 0, keep * cell, cell), 0, 0, null)
    }
    var baked = 0
    for (frame in keep until ids.size) {
        val id = ids[frame]
        val img = SmwEnemyGraphics.spriteImage(rom, header, level, id)
        if (img == null) {
            System.err.println("  AVISO: id 0x${id.toString(16).uppercase()} " +
                "(${SmwEnemyGraphics.nameOf(id)}) no renderizó en el nivel 0x${level.toString(16)}")
            continue
        }
        for (y in 0 until cell) for (x in 0 until cell) atlas.setRGB(frame * cell + x, y, img.get(x, y))
        baked++
        println("  + fotograma $frame: 0x${id.toString(16).uppercase()} ${SmwEnemyGraphics.nameOf(id)}")
    }
    ImageIO.write(atlas, "png", outFile)
    println("Atlas: ${outFile.absolutePath} (${atlas.width}x${atlas.height}, " +
        "$keep conservados + $baked nuevos = ${ids.size} fotogramas) — nuevos desde " +
        "\"${header.title}\" nivel 0x${level.toString(16).uppercase()}")
}
