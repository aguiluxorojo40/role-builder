package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SmwLevelSet
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE de las BOCAS DE WARP: enfrenta lo que ve [SmwWarpTiles.pipeOrDoor]
 * sobre la rejilla de colisión con lo que hay DE VERDAD en el nivel, y mide la cobertura
 * de [SmwWarpTiles.levelWarps] en TODA la ROM (antes/después de tocar nada).
 *
 * Se salta sola sin `SMW_ROM`. Variables: `SMW_LEVEL` (hex) y `SMW_SCREEN` (decimal).
 */
class ZzWarpProbe {

    /** Bytes BAJOS que alguna vez son boca de warp, en cualquiera de los dos planos. */
    private val BAJOS_DE_WARP = intArrayOf(0x1F, 0x20, 0x37, 0x38, 0x3F, 0x9C)

    private val out = File(
        "/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/warps"
    )

    @Test
    fun `bocas de warp - diagnostico y cobertura`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        out.mkdirs()

        val lv = (System.getenv("SMW_LEVEL") ?: "105").toInt(16)
        val screen = (System.getenv("SMW_SCREEN") ?: "7").toInt()
        volcadoDePantalla(rom, header, lv, screen)
        val lista = (System.getenv("SMW_LEVELS") ?: "105,1CB,106,103,102,107,10E,115,117,101,11C")
            .split(",").map { it.trim().toInt(16) }
        for (n in lista) ficha(rom, header, n)
        cobertura(rom, header)
    }

    /** `misc_level_tileset_setting` del nivel (hace falta para la puerta de castillo 0x9C). */
    private fun tilesetDe(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int): Int =
        com.rolebuilder.core.snes.SmwLayer1
            .parse(rom, SnesGameRecipes.smwHeaderDeltaPublic(header), lv)?.tileset
            ?: SmwWarpTiles.TILESET_UNKNOWN

    /** Ficha corta de un nivel: sus salidas, sus warps y el nivel entero pintado con las bocas. */
    private fun ficha(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int) {
        println("\n--- NIVEL %03X ---".format(lv))
        val conn = runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull()
        println("  salidas: " + (conn?.exits?.joinToString { "p%d->%03X%s".format(
            it.fromScreen, it.destinationLevel, if (it.isSecondary) "(sec)" else ""
        ) } ?: "(sin conectividad)"))
        val w = runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrDefault(emptyList())
        println("  warps: " + (if (w.isEmpty()) "(ninguno)" else w.joinToString("\n    ", "\n    ")))
        pintaNivel(rom, header, lv)
    }

    /** Renderiza el nivel entero a PNG con un recuadro magenta en cada boca detectada. */
    private fun pintaNivel(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int) {
        val mapa = SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) ?: return
        val col = SnesGameRecipes.smwLevelCollision(rom, header, lv) ?: return
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
        fun recuadro(cx: Int, cy: Int, color: Int) {
            if (cx !in 0 until mapa.mapWidth || cy !in 0 until mapa.mapHeight) return
            for (t in 0 until 16) {
                bi.setRGB(cx * 16 + t, cy * 16, color)
                bi.setRGB(cx * 16 + t, cy * 16 + 15, color)
                bi.setRGB(cx * 16, cy * 16 + t, color)
                bi.setRGB(cx * 16 + 15, cy * 16 + t, color)
            }
        }
        for (r in 0 until minOf(col.rows, mapa.mapHeight)) for (c in 0 until minOf(col.cols, mapa.mapWidth)) {
            if (SmwWarpTiles.pipeOrDoor(col.blockAt(c, r), tilesetDe(rom, header, lv)) != null) recuadro(c, r, 0xFFFF00FF.toInt())
        }
        // Verde: la casilla DISPARADORA del warp. Amarillo: donde aparece el jugador al llegar.
        for (w in runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrDefault(emptyList())) {
            recuadro(w.xTile, w.yTile, 0xFF00FF00.toInt())
        }
        val start = com.rolebuilder.core.snes.SmwLevelStartReader.read(rom, header, lv)
        if (start != null) recuadro(start.startTileX, start.startTileY, 0xFFFFFF00.toInt())
        val f = File(out, "%03X.png".format(lv))
        ImageIO.write(bi, "png", f)
        println("  PNG: $f")
    }

    /** Vuelca la pantalla [screen] del nivel [lv]: bloque a bloque, y qué ve el detector. */
    private fun volcadoDePantalla(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int, screen: Int) {
        val col = SnesGameRecipes.smwLevelCollision(rom, header, lv) ?: run {
            println("nivel %03X sin colision".format(lv)); return
        }
        val c0 = screen * 16
        val c1 = minOf(c0 + 16, col.cols)
        println("\n=== NIVEL %03X, PANTALLA %d (columnas %d..%d de %d) ===".format(lv, screen, c0, c1 - 1, col.cols))
        println("rejilla de bloques Map16 (--- = aire 0x025; se marca [] lo que pipeOrDoor clasifica):")
        for (r in 0 until col.rows) {
            val sb = StringBuilder("fila %2d ".format(r))
            for (c in c0 until c1) {
                val b = col.blockAt(c, r)
                val k = SmwWarpTiles.pipeOrDoor(b)
                val txt = if (b == 0x25) "---" else "%03X".format(b)
                sb.append(if (k != null) "[$txt]" else " $txt ")
            }
            println(sb)
        }
        // Todas las celdas de la pantalla cuyo byte BAJO es de warp, con su plano alto.
        println("celdas con byte bajo 1F/20/37/38/3F en esta pantalla:")
        for (r in 0 until col.rows) {
            for (c in c0 until c1) {
                val b = col.blockAt(c, r)
                val lo = b and 0xFF
                if (lo !in BAJOS_DE_WARP) continue
                println(
                    "  (%3d,%2d) bloque=%03X alto=%d bajo=%02X  pipeOrDoor=%s  arriba=%03X abajo=%03X".format(
                        c, r, b, b ushr 8, lo, SmwWarpTiles.pipeOrDoor(b),
                        if (r > 0) col.blockAt(c, r - 1) else -1,
                        if (r + 1 < col.rows) col.blockAt(c, r + 1) else -1,
                    )
                )
            }
        }
        println("warps resueltos: " + SmwWarpTiles.levelWarps(rom, header, lv)
            .joinToString("\n  ", "\n  ") { it.toString() })
        pintaPantalla(rom, header, lv, screen)
    }

    /** Renderiza la pantalla [screen] del nivel [lv] a PNG, con las bocas detectadas marcadas. */
    private fun pintaPantalla(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int, screen: Int) {
        val mapa = SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) ?: return
        val col = SnesGameRecipes.smwLevelCollision(rom, header, lv) ?: return
        val c0 = screen * 16
        val ancho = minOf(16, mapa.mapWidth - c0)
        if (ancho <= 0) return
        val bi = BufferedImage(ancho * 16, mapa.mapHeight * 16, BufferedImage.TYPE_INT_ARGB)
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
        for (cy in 0 until mapa.mapHeight) for (cx in 0 until ancho) {
            val i = cy * mapa.mapWidth + (c0 + cx)
            pinta(mapa.bgTiles.getOrElse(i) { -1 }, cx, cy)
            pinta(mapa.tiles.getOrElse(i) { -1 }, cx, cy)
        }
        // Recuadro magenta en cada boca detectada.
        for (r in 0 until minOf(col.rows, mapa.mapHeight)) for (cx in 0 until ancho) {
            if (SmwWarpTiles.pipeOrDoor(col.blockAt(c0 + cx, r)) == null) continue
            for (t in 0 until 16) {
                bi.setRGB(cx * 16 + t, r * 16, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16 + t, r * 16 + 15, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16, r * 16 + t, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16 + 15, r * 16 + t, 0xFFFF00FF.toInt())
            }
        }
        val f = File(out, "%03X_p%d.png".format(lv, screen))
        ImageIO.write(bi, "png", f)
        println("PNG de la pantalla: $f")
    }

    /** Cobertura de warps en TODA la ROM: niveles con salida, con boca, y con warp resuelto. */
    private fun cobertura(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader) {
        println("\n=== COBERTURA EN TODA LA ROM ===")
        val detalle = StringBuilder()
        var conWarp = 0
        var totalWarps = 0
        val porGesto = HashMap<String, Int>()
        val conSalida = (0 until 0x200).mapNotNull { lv ->
            val conn = runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull()
            if (conn == null || conn.exits.isEmpty()) null else lv to conn
        }
        for ((lv, conn) in conSalida) {
            val w = runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrDefault(emptyList())
            if (w.isNotEmpty()) { conWarp++; totalWarps += w.size }
            for (x in w) porGesto[x.enter.name] = (porGesto[x.enter.name] ?: 0) + 1
            detalle.append(
                "%03X salidas=%d warps=%d %s destinos=%s\n".format(
                    lv, conn.exits.size, w.size,
                    w.groupingBy { it.enter.name }.eachCount(),
                    w.map { "%03X".format(it.destLevel) }.distinct(),
                )
            )
        }
        println("niveles con al menos una SALIDA de pantalla: ${conSalida.size}")
        println("de esos, con al menos un WARP resuelto: $conWarp")
        println("warps totales: $totalWarps  por gesto: ${porGesto.toSortedMap()}")
        // Los que tienen salida pero NINGÚN warp: son los rotos (sala inalcanzable).
        val rotos = detalle.lines().filter { it.contains("warps=0") }
        println("niveles con salida pero SIN warp (${rotos.size}): " + rotos.joinToString(" ") { it.take(3) })
        println("niveles alcanzables del set: ${SmwLevelSet.reachableLevels(rom, header).size}")
        // CONTROL: la salida se busca por la pantalla de la BOCA, pero el juego la busca por
        // la pantalla del JUGADOR ($05:D796, `misc_subscreen_exit_entrance_number_lo[HIBYTE
        // (player_xpos)]`). Si alguna casilla disparadora cae en otra pantalla que su boca,
        // ahi podriamos estar cogiendo la salida equivocada.
        var cruzan = 0
        for ((lv, _) in conSalida) {
            for (w in runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrDefault(emptyList())) {
                if (w.xTile / 16 != w.mouthXTile / 16) {
                    cruzan++
                    println("  ⚠ %03X: disparador (%d,%d) y boca (%d,%d) en pantallas distintas"
                        .format(lv, w.xTile, w.yTile, w.mouthXTile, w.mouthYTile))
                }
            }
        }
        println("warps con disparador en otra pantalla que su boca: $cruzan")
        File(out, "cobertura.txt").writeText(detalle.toString())
        println("detalle en ${File(out, "cobertura.txt")}")
    }
}
