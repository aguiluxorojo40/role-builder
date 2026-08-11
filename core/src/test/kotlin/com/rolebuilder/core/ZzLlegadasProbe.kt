package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEntranceAction
import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SmwSolidity
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesHeader
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * SONDA DESECHABLE: ¿el jugador CAE BIEN donde le mandamos? Audita las 366 llegadas de warp
 * de toda la ROM contra la rejilla de colisión del nivel destino y las clasifica; luego
 * pinta una muestra para mirarla a ojo, que es lo único que convence.
 *
 * El criterio NO es "la casilla está libre", porque depende de CÓMO se llega
 * ([SmwEntranceAction], `InitializeLevelRAM_00A6CC` $00:A6CC):
 *  - Andando (0/5): la casilla tiene que estar LIBRE y con suelo debajo a mano.
 *  - Saliendo de una tubería (1-4/6/7): la casilla ES la tubería, o sea SÓLIDA — el jugador
 *    sale de ella. Ahí lo raro sería aterrizar en el aire.
 *
 * Se salta sola sin `SMW_ROM`.
 */
class ZzLlegadasProbe {

    private val out = File(
        "/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/llegadas"
    )

    /** Filas que se miran hacia abajo buscando suelo antes de dar la llegada por "en el aire". */
    private val ALCANCE_SUELO = 12

    private class Llegada(
        val fromLevel: Int,
        val destLevel: Int,
        val x: Int,
        val y: Int,
        val secundaria: Boolean,
        val accion: SmwEntranceAction,
        val veredicto: String,
        val detalle: String,
    )

    @Test
    fun `auditoria de las llegadas de warp`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        out.mkdirs()

        val todas = recoge(rom, header)
        informe(todas)
        // ¿Los casos raros están en niveles a los que se llega de verdad, o en huecos muertos?
        val vivos = com.rolebuilder.core.snes.SmwLevelSet.reachableLevels(rom, header)
        println("\n--- ¿los casos raros son de niveles ALCANZABLES? ---")
        for (l in todas.filter { it.veredicto != "OK" }.distinctBy { it.fromLevel to it.destLevel }) {
            println("  %-20s %03X(%s) -> %03X(%s)".format(
                l.veredicto, l.fromLevel, if (l.fromLevel in vivos) "vivo" else "MUERTO",
                l.destLevel, if (l.destLevel in vivos) "vivo" else "MUERTO",
            ))
        }
        // Rejilla alrededor de las llegadas dudosas, para ver si el fallo es de la posición
        // o de cómo leemos el nivel destino.
        for (l in todas.filter { it.veredicto == "SIN_SUELO_DEBAJO" || it.veredicto == "TUBERIA_EN_EL_AIRE" }
            .distinctBy { it.destLevel }) {
            rejillaAlrededor(rom, header, l)
        }
        // Muestra a ojo: todas las MALAS (que son pocas) + un surtido de las buenas cubriendo
        // pantalla 0 y pantalla != 0, andando y de tubería, principal y secundaria.
        val muestra = todas.filter { it.veredicto != "OK" } + muestraDeBuenas(todas)
        for (l in muestra) pinta(rom, header, l)
        println("\nPNG de la muestra en $out (${muestra.size} imágenes)")
    }

    /** Recorre toda la ROM y clasifica cada llegada contra la colisión del nivel destino. */
    private fun recoge(rom: ByteArray, header: SnesHeader): List<Llegada> {
        val res = ArrayList<Llegada>()
        val conSalida = (0 until 0x200).mapNotNull { lv ->
            val conn = runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull()
            if (conn == null || conn.exits.isEmpty()) null else lv to conn
        }
        for ((lv, conn) in conSalida) {
            val secPorPantalla = conn.exits.associate { it.fromScreen to it.isSecondary }
            for (w in runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrDefault(emptyList())) {
                res.add(juzga(rom, header, lv, w, secPorPantalla[w.mouthXTile / 16] == true))
            }
        }
        return res
    }

    private fun juzga(
        rom: ByteArray,
        header: SnesHeader,
        lv: Int,
        w: SmwWarpTiles.LevelWarp,
        secundaria: Boolean,
    ): Llegada {
        val col = SnesGameRecipes.smwLevelCollision(rom, header, w.destLevel)
        val deTuberia = w.destAction != SmwEntranceAction.WALK_IN &&
            w.destAction != SmwEntranceAction.WALK_IN_ICE
        fun ll(veredicto: String, detalle: String) =
            Llegada(lv, w.destLevel, w.destXTile, w.destYTile, secundaria, w.destAction, veredicto, detalle)

        if (col == null) return ll("SIN_COLISION", "el destino no tiene Layer 1 (sala de jefe o similar)")
        if (w.destXTile !in 0 until col.cols || w.destYTile !in 0 until col.rows) {
            return ll("FUERA_DEL_MAPA", "la casilla se sale de la rejilla ${col.cols}x${col.rows}")
        }
        val celda = col.solidityAt(w.destXTile, w.destYTile)
        val suelo = (1..ALCANCE_SUELO)
            .takeWhile { w.destYTile + it < col.rows }
            .firstOrNull { col.solidityAt(w.destXTile, w.destYTile + it) != SmwSolidity.NONE }
            ?: -1
        val bloque = col.blockAt(w.destXTile, w.destYTile)
        val det = "bloque=%03X solidez=%s sueloA=%s".format(bloque, celda, if (suelo < 0) "nada" else "$suelo")
        return when {
            // Sale de una tubería: la casilla ES la tubería, tiene que ser sólida.
            deTuberia && celda == SmwSolidity.NONE -> ll("TUBERIA_EN_EL_AIRE", det)
            deTuberia -> ll("OK", det)
            // Andando: la casilla libre y con suelo a mano.
            celda != SmwSolidity.NONE -> ll("DENTRO_DE_MURO", det)
            suelo < 0 -> ll("SIN_SUELO_DEBAJO", det)
            else -> ll("OK", det)
        }
    }

    /** Surtido de llegadas OK que cubra los casos distintos, para no mirar solo las malas. */
    private fun muestraDeBuenas(todas: List<Llegada>): List<Llegada> {
        val buenas = todas.filter { it.veredicto == "OK" }
        val grupos = buenas.groupBy {
            "p%s-%s-%s".format(
                if (it.x / 16 == 0) "0" else "N",
                if (it.secundaria) "sec" else "pri",
                if (it.accion == SmwEntranceAction.WALK_IN || it.accion == SmwEntranceAction.WALK_IN_ICE) "and" else "tub",
            )
        }
        println("\n--- grupos de llegadas OK (para elegir la muestra) ---")
        grupos.toSortedMap().forEach { (k, v) -> println("  %-16s %d".format(k, v.size)) }
        // Hasta 4 de cada grupo, distintas entre sí por nivel destino.
        return grupos.values.flatMap { g -> g.distinctBy { it.destLevel to it.x }.take(4) }
    }

    private fun informe(todas: List<Llegada>) {
        println("\n=== AUDITORÍA DE LAS ${todas.size} LLEGADAS DE WARP ===")
        val enP0 = todas.filter { it.x / 16 == 0 }
        println("llegadas en la PANTALLA 0 del destino: ${enP0.size}; en otra pantalla: ${todas.size - enP0.size}")
        for ((titulo, grupo) in listOf("TODAS" to todas, "SOLO LAS DE LA PANTALLA 0" to enP0)) {
            println("--- $titulo ---")
            grupo.groupingBy { it.veredicto }.eachCount().toSortedMap()
                .forEach { (k, v) -> println("  %-20s %d".format(k, v)) }
        }
        println("--- las que NO son OK, una a una ---")
        for (l in todas.filter { it.veredicto != "OK" }.sortedBy { it.veredicto }) {
            println("  %-20s %03X -> %03X en (%d,%d) pantalla=%d %s %s  [%s]".format(
                l.veredicto, l.fromLevel, l.destLevel, l.x, l.y, l.x / 16,
                if (l.secundaria) "sec" else "pri", l.accion, l.detalle,
            ))
        }
        val txt = StringBuilder()
        for (l in todas.sortedWith(compareBy({ it.veredicto }, { it.destLevel }))) {
            txt.append("%-20s %03X -> %03X (%d,%d) p%d %s %s [%s]\n".format(
                l.veredicto, l.fromLevel, l.destLevel, l.x, l.y, l.x / 16,
                if (l.secundaria) "sec" else "pri", l.accion, l.detalle,
            ))
        }
        File(out, "auditoria.txt").writeText(txt.toString())
    }

    /** Vuelca la rejilla de bloques alrededor de una llegada dudosa. */
    private fun rejillaAlrededor(rom: ByteArray, header: SnesHeader, l: Llegada) {
        val col = SnesGameRecipes.smwLevelCollision(rom, header, l.destLevel) ?: return
        println("\n--- rejilla de %03X (%dx%d) alrededor de la llegada (%d,%d) ---"
            .format(l.destLevel, col.cols, col.rows, l.x, l.y))
        val r0 = maxOf(0, l.y - 4)
        val r1 = minOf(col.rows - 1, l.y + 14)
        for (r in r0..r1) {
            val sb = StringBuilder("fila %3d ".format(r))
            for (c in 0 until minOf(col.cols, 20)) {
                val b = col.blockAt(c, r)
                val marca = if (c == l.x && r == l.y) "*" else " "
                sb.append(marca).append(if (b == 0x25) "---" else "%03X".format(b))
            }
            println(sb)
        }
    }

    /** Pinta la pantalla de llegada con la casilla recuadrada, para mirarla. */
    private fun pinta(rom: ByteArray, header: SnesHeader, l: Llegada) {
        val mapa = SnesGameRecipes.extractSmwLevelAsMap(rom, header, l.destLevel) ?: return
        val pantalla = l.x / 16
        val c0 = pantalla * 16
        val ancho = minOf(16, mapa.mapWidth - c0)
        if (ancho <= 0) return
        val bi = BufferedImage(ancho * 16, mapa.mapHeight * 16, BufferedImage.TYPE_INT_ARGB)
        val at = mapa.atlas
        fun pintaTesela(idx: Int, cx: Int, cy: Int) {
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
            pintaTesela(mapa.bgTiles.getOrElse(i) { -1 }, cx, cy)
            pintaTesela(mapa.tiles.getOrElse(i) { -1 }, cx, cy)
        }
        val cx = l.x - c0
        if (cx in 0 until ancho && l.y in 0 until mapa.mapHeight) {
            for (t in 0 until 16) {
                bi.setRGB(cx * 16 + t, l.y * 16, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16 + t, l.y * 16 + 15, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16, l.y * 16 + t, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16 + 15, l.y * 16 + t, 0xFFFF00FF.toInt())
            }
        }
        val f = File(out, "%s_%03X_a_%03X_p%d.png".format(l.veredicto, l.fromLevel, l.destLevel, pantalla))
        ImageIO.write(bi, "png", f)
    }
}
