package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwBlockBehavior
import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE de AUDITORÍA: qué BLOQUES tiene de verdad un nivel, celda a celda.
 *
 * [ZzNivelProbe] cuenta las acciones por TESELA DEL ATLAS (bloques distintos), no por
 * casilla, así que "DRAGON_COIN=2" no quiere decir dos monedas dragón sino dos teselas
 * distintas. Aquí se cuenta por CASILLA y se listan las coordenadas, que es lo que hace
 * falta para comparar con cómo es el nivel de verdad.
 *
 * Además vuelca:
 *  - la imagen del nivel SIN el recorte a 256 columnas (`SMW_MAXCOLS`), para ver si se
 *    está perdiendo la parte final del nivel;
 *  - una HOJA DE BLOQUES con cada bloque Map16 distinto dibujado y etiquetado, que es la
 *    única forma de saber qué es cada id sin fiarse de tablas de memoria.
 *
 * Se salta sola sin `SMW_ROM`.
 */
class ZzBloquesProbe {

    private companion object {
        const val AIRE = 0x25
        const val SALIDA = "/tmp/claude-0/-home-user-role-builder/" +
            "a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/niveles"
    }

    @Test
    fun `bloques celda a celda de los niveles`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val maxCols = (System.getenv("SMW_MAXCOLS") ?: "512").toInt()
        val out = File(SALIDA).apply { mkdirs() }
        val niveles = (System.getenv("SMW_LEVELS") ?: "105,106,103,102,104")
            .split(",").map { it.trim().toInt(16) }

        for (lv in niveles) {
            println("\n===== NIVEL %03X — %s =====".format(lv, SmwLevelNames.nameOf(rom, delta, lv)?.trim()))
            unNivel(rom, header, delta, lv, maxCols, out)
        }
    }

    private fun unNivel(
        rom: ByteArray,
        header: com.rolebuilder.core.snes.SnesHeader,
        delta: Int,
        lv: Int,
        maxCols: Int,
        out: File,
    ) {
        val tm = SmwLayer1.parse(rom, delta, lv)
        if (tm == null) {
            println("  sin Layer 1")
            return
        }
        println("  tilemap crudo: %d pantallas → %dx%d casillas (vertical=%s), tileset=%d modo=%d"
            .format(tm.screens, tm.cols, tm.rows, tm.vertical, tm.tileset, tm.mode))
        println("  objetos: %d (%d sin portar %s)".format(tm.totalObjects, tm.unknownObjects, tm.unknownIds))
        println("  recorte real=%s  recorte con el tope 256 de extractSmwLevelAsMap=%s"
            .format(tm.trimmedSize(), tm.trimmedSize(256)))

        cuentaBloques(tm)
        val mapa = SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv, maxCols)
        if (mapa == null) {
            println("  extractSmwLevelAsMap: null")
            return
        }
        println("  mapa exportado: %dx%d casillas, atlas %dx%d".format(
            mapa.mapWidth, mapa.mapHeight, mapa.columns, mapa.rows))
        pintaNivel(mapa, File(out, "%03X_full.png".format(lv)))
        hojaDeBloques(tm, mapa, File(out, "%03X_bloques.png".format(lv)))
    }

    /** Cuenta por CASILLA (no por tesela) los bloques interactivos y los lista con coordenadas. */
    private fun cuentaBloques(tm: SmwLayer1.SmwLevelTilemap) {
        val porAccion = HashMap<SmwBlockAction, MutableList<Pair<Int, Int>>>()
        val hist = HashMap<Int, Int>()
        val primera = HashMap<Int, Pair<Int, Int>>()
        for (y in 0 until tm.rows) {
            for (x in 0 until tm.cols) {
                val b = tm.block(x, y)
                if (b <= 0 || b == AIRE) continue
                hist[b] = (hist[b] ?: 0) + 1
                primera.putIfAbsent(b, x to y)
                val a = SmwBlockBehavior.classify(b)
                if (a != SmwBlockAction.NONE) porAccion.getOrPut(a) { ArrayList() }.add(x to y)
            }
        }
        println("  --- INTERACTIVOS POR CASILLA ---")
        for (a in SmwBlockAction.entries) {
            if (a == SmwBlockAction.NONE) continue
            val l = porAccion[a].orEmpty()
            println("    %-12s %3d  %s".format(a.name, l.size,
                l.take(24).joinToString(" ") { "(${it.first},${it.second})" }))
        }
        println("  --- BLOQUES DISTINTOS (%d) id=veces ---".format(hist.size))
        println("    " + hist.entries.sortedBy { it.key }
            .joinToString(" ") { "%03X=%d".format(it.key, it.value) })
    }

    private fun pintaNivel(mapa: SnesGameRecipes.SmwLevelMap, destino: File) {
        val bi = BufferedImage(mapa.mapWidth * 16, mapa.mapHeight * 16, BufferedImage.TYPE_INT_ARGB)
        for (cy in 0 until mapa.mapHeight) {
            for (cx in 0 until mapa.mapWidth) {
                val i = cy * mapa.mapWidth + cx
                pintaTesela(mapa, mapa.bgTiles.getOrElse(i) { -1 }, bi, cx * 16, cy * 16)
                pintaTesela(mapa, mapa.tiles.getOrElse(i) { -1 }, bi, cx * 16, cy * 16)
            }
        }
        ImageIO.write(bi, "png", destino)
        println("  imagen: ${destino.name} ${bi.width}x${bi.height}")
    }

    private fun pintaTesela(mapa: SnesGameRecipes.SmwLevelMap, idx: Int, bi: BufferedImage, px: Int, py: Int) {
        if (idx < 0) return
        val tx = (idx % mapa.columns) * 16
        val ty = (idx / mapa.columns) * 16
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val dentro = tx + x < mapa.atlas.width && ty + y < mapa.atlas.height
                val c = if (dentro) mapa.atlas.get(tx + x, ty + y) else 0
                if ((c ushr 24) != 0 && px + x < bi.width && py + y < bi.height) bi.setRGB(px + x, py + y, c)
            }
        }
    }

    /** Cada bloque Map16 distinto, dibujado a 3× y etiquetado con su id: la leyenda visual. */
    private fun hojaDeBloques(
        tm: SmwLayer1.SmwLevelTilemap,
        mapa: SnesGameRecipes.SmwLevelMap,
        destino: File,
    ) {
        val aTesela = LinkedHashMap<Int, Int>()
        for (y in 0 until mapa.mapHeight) {
            for (x in 0 until mapa.mapWidth) {
                val t = mapa.tiles.getOrElse(y * mapa.mapWidth + x) { -1 }
                if (t >= 0) aTesela.putIfAbsent(tm.block(x, y), t)
            }
        }
        val cols = 10
        val filas = (aTesela.size + cols - 1) / cols
        val cw = 56
        val ch = 72
        val bi = BufferedImage(cols * cw, maxOf(1, filas) * ch, BufferedImage.TYPE_INT_RGB)
        val g = bi.createGraphics()
        g.color = Color(0x20, 0x20, 0x30)
        g.fillRect(0, 0, bi.width, bi.height)
        g.font = Font(Font.MONOSPACED, Font.BOLD, 14)
        aTesela.entries.forEachIndexed { i, (block, tesela) ->
            val cx = (i % cols) * cw + 4
            val cy = (i / cols) * ch + 4
            for (y in 0 until 16) {
                for (x in 0 until 16) {
                    val ax = (tesela % mapa.columns) * 16 + x
                    val ay = (tesela / mapa.columns) * 16 + y
                    val c = if (ax < mapa.atlas.width && ay < mapa.atlas.height) mapa.atlas.get(ax, ay) else 0
                    g.color = if ((c ushr 24) == 0) Color(0x40, 0x40, 0x50) else Color(c, true)
                    g.fillRect(cx + x * 3, cy + y * 3, 3, 3)
                }
            }
            g.color = Color.WHITE
            g.drawString("%03X".format(block), cx, cy + 62)
        }
        g.dispose()
        ImageIO.write(bi, "png", destino)
        println("  hoja de bloques: ${destino.name} (${aTesela.size} bloques distintos)")
    }
}
