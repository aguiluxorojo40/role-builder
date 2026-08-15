package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelBundle
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE: FICHA COMPLETA de un nivel, para poder compararla con cómo es el nivel
 * de verdad. Vuelca la imagen del nivel entero (fondo + primer plano) y todo lo que sabemos
 * de él: sprites con nombre y posición, meta, warps, música, tiempo y sub-niveles.
 *
 * El nivel se elige con `SMW_LEVEL` (hex, p.ej. `105`). Se salta sola sin `SMW_ROM`.
 */
class ZzNivelProbe {

    @Test
    fun `ficha completa de un nivel`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val lv = (System.getenv("SMW_LEVEL") ?: "105").toInt(16)
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val out = File("/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/niveles")
        out.mkdirs()

        println("\n=== NIVEL %03X — %s ===".format(lv, SmwLevelNames.nameOf(rom, delta, lv)?.trim()))
        val info = SnesGameRecipes.smwLevelInfo(rom, header, lv)
        println("cabecera: musica=%s tiempo=%s gfxSprites=%s".format(
            info?.musicIndex, info?.startTime, info?.spriteGfx?.and(0x0F)))

        val mapa = SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv)
        if (mapa == null) { println("SIN MAPA"); return }
        println("mapa: %dx%d casillas, primerPlano=%d fondo=%d, atlas %dx%d".format(
            mapa.mapWidth, mapa.mapHeight, mapa.tiles.count { it >= 0 }, mapa.bgTiles.count { it >= 0 },
            mapa.columns, mapa.rows))

        val bi = BufferedImage(mapa.mapWidth * 16, mapa.mapHeight * 16, BufferedImage.TYPE_INT_ARGB)
        val at = mapa.atlas
        fun pinta(idx: Int, cx: Int, cy: Int) {
            if (idx < 0) return
            val tx = (idx % mapa.columns) * 16
            val ty = (idx / mapa.columns) * 16
            for (y in 0 until 16) for (x in 0 until 16) {
                val dentro = tx + x < at.width && ty + y < at.height
                val px = if (dentro) at.get(tx + x, ty + y) else 0
                if ((px ushr 24) != 0) bi.setRGB(cx * 16 + x, cy * 16 + y, px)
            }
        }
        for (cy in 0 until mapa.mapHeight) for (cx in 0 until mapa.mapWidth) {
            val i = cy * mapa.mapWidth + cx
            pinta(mapa.bgTiles.getOrElse(i) { -1 }, cx, cy)
            pinta(mapa.tiles.getOrElse(i) { -1 }, cx, cy)
        }
        ImageIO.write(bi, "png", File(out, "%03X.png".format(lv)))

        println("\n--- BLOQUES INTERACTIVOS (lo que se recoge o se golpea) ---")
        val acciones = mapa.blockActions
        val cuenta = HashMap<String, Int>()
        for (a in acciones) {
            val n = com.rolebuilder.core.snes.SmwBlockAction.entries[a]
            if (n != com.rolebuilder.core.snes.SmwBlockAction.NONE) cuenta[n.name] = (cuenta[n.name] ?: 0) + 1
        }
        println("  " + (if (cuenta.isEmpty()) "(ninguno)" else cuenta.toSortedMap().toString()))
        // La PUERTA INTERMEDIA son bloques Map16 concretos (0x2F-0x34 / 0x39-0x3E segun si
        // es la version de meta). Se buscan en el mapa reconstruido.
        val tmp = com.rolebuilder.core.snes.SmwLayer1.parse(rom, delta, lv)
        var midway = 0
        if (tmp != null) {
            for (y in 0 until tmp.rows) for (x in 0 until tmp.cols) {
                val b = tmp.block(x, y)
                if (b in 0x2F..0x34 || b in 0x39..0x3E) midway++
            }
        }
        println("  puerta intermedia: %d bloques".format(midway))

        // Histograma de bloques del PLANO INTERACTIVO (byte alto 0): ahi viven monedas,
        // bloques ?, la moneda dragon y la puerta. Si algo que se ve no sale aqui, es que
        // esta en otro plano y no se esta clasificando.
        if (tmp != null) {
            val hist = HashMap<Int, Int>()
            for (y in 0 until tmp.rows) for (x in 0 until tmp.cols) {
                val b = tmp.block(x, y)
                if (b in 1..0xFF && b != 0x25) hist[b] = (hist[b] ?: 0) + 1
            }
            println("  bloques del plano interactivo (id x veces):")
            println("    " + hist.entries.sortedByDescending { it.value }.take(24)
                .joinToString(" ") { "%02X=%d".format(it.key, it.value) })
        }

        println("\n--- SPRITES COLOCADOS ---")
        val seeds = SnesGameRecipes.smwLevelEnemySeeds(rom, header, lv)
        seeds.groupBy { it.id }.toSortedMap().forEach { (id, l) ->
            println("  %02X %-28s x%-3d  en %s".format(
                id, SmwSpriteNames.nameOf(id), l.size,
                l.take(8).joinToString(" ") { "(${it.xTile},${it.yTile})" }))
        }

        println("\n--- META ---")
        println("  " + runCatching { SmwLevelGoal.goalCellsWithKind(rom, delta, lv) }.getOrNull())

        println("\n--- WARPS (bocas de tuberia/puerta) ---")
        runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrNull()?.forEach {
            println("  $it")
        } ?: println("  (ninguno)")

        println("\n--- QUE VE EL DETECTOR DE BOCAS EN EL MAPA DE COLISION ---")
        val col = SnesGameRecipes.smwLevelCollision(rom, header, lv)
        if (col == null) println("  sin mapa de colision") else {
            val porTipo = HashMap<String, Int>()
            val enPantalla = HashMap<Int, MutableSet<String>>()
            for (r in 0 until col.rows) for (c in 0 until col.cols) {
                val k = SmwWarpTiles.pipeOrDoor(col.blockAt(c, r)) ?: continue
                porTipo[k.name] = (porTipo[k.name] ?: 0) + 1
                enPantalla.getOrPut(c / 16) { HashSet() }.add(k.name)
            }
            println("  bocas por tipo: " + (porTipo.toSortedMap().takeIf { it.isNotEmpty() } ?: "(ninguna)"))
            println("  por pantalla: " + enPantalla.toSortedMap())
            // Y que bloques hay de verdad en la pantalla 7, que es la que tiene la salida.
            val h = HashMap<Int, Int>()
            for (r in 0 until col.rows) for (c in 112 until minOf(128, col.cols)) {
                val b = col.blockAt(c, r); if (b > 0 && b != 0x25) h[b] = (h[b] ?: 0) + 1
            }
            println("  bloques de la pantalla 7: " + h.entries.sortedByDescending { it.value }
                .take(16).joinToString(" ") { "%03X=%d".format(it.key, it.value) })
        }

        println("\n--- SALIDAS DE PANTALLA (la tabla de la ROM) ---")
        val ex = runCatching { com.rolebuilder.core.snes.SmwLevelExits.read(rom, header, lv) }
        val conn = ex.getOrNull()
        if (conn == null) println("  fallo: ${ex.exceptionOrNull()?.message}")
        else if (conn.exits.isEmpty()) println("  (ninguna)")
        else conn.exits.forEach { println("  $it") }

        println("\n--- PAQUETE (sub-niveles enlazados) ---")
        val b = runCatching { SmwLevelBundle.extract(rom, header, lv) }
        println("  " + (b.getOrNull()?.levels?.joinToString(",") { "%03X".format(it) }
            ?: "fallo: ${b.exceptionOrNull()?.message}"))
    }
}
