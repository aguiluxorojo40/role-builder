package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesHeader
import java.io.File
import kotlin.test.Test

/**
 * SONDA DESECHABLE de las ENTRADAS: enfrenta lo que dicen las tablas CRUDAS de la ROM
 * (cabecera secundaria $05:F000/F200/F400/F600 y entradas secundarias
 * $05:F800/FA00/FC00/FE00) con la posición de llegada que resuelve [SmwWarpTiles].
 *
 * Lo que se busca es la PANTALLA de la entrada, que es el campo que faltaba: `LoadLevel`
 * ($05:D796) termina haciendo `HIBYTE(player_xpos) = r1 & 0x1F`, con `r1` = $05:F600[nivel]
 * en la entrada principal y `r1` = $05:FC00[nº] en la secundaria. Sin ella, toda llegada
 * cae en la pantalla 0.
 *
 * Se salta sola sin `SMW_ROM`.
 */
class ZzEntradasProbe {

    private val out = File(
        "/tmp/claude-0/-home-user-role-builder/a10077d4-0a3f-5105-8ad8-f3622d1f4549/scratchpad/entradas"
    )

    private fun rd(rom: ByteArray, delta: Int, snes: Int, index: Int): Int {
        val at = 0x05 * 0x8000 + (snes and 0x7FFF) + delta + index
        return if (at in rom.indices) rom[at].toInt() and 0xFF else -1
    }

    @Test
    fun `entradas principales y secundarias, crudas y resueltas`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        out.mkdirs()

        crudoDeLosCitados(rom, header, delta)
        cuantasPantallasNoCero(rom, delta)
        dondeAterrizanLosWarps(rom, header)
        for (lv in (System.getenv("SMW_LEVELS") ?: "1BE,1CB,1CA,1FD").split(",")) {
            pintaLlegadas(rom, header, lv.trim().toInt(16))
        }
        // Y la ENTRADA PRINCIPAL de niveles cuya pantalla de entrada NO es la 0: es el otro
        // sitio donde la pantalla se caía, y cambia dónde aparece el jugador al jugarlos.
        for (lv in (System.getenv("SMW_PRINCIPALES") ?: "0EC,0E8,0F1,0FE").split(",")) {
            val n = lv.trim().toInt(16)
            val start = SmwLevelStartReader.read(rom, header, n) ?: continue
            println("  entrada principal %03X: pantalla=%d vertical=%s -> (%d,%d)"
                .format(n, start.entranceScreen, start.isVertical, start.startTileX, start.startTileY))
            pintaCasilla(rom, header, n, start.startTileX, start.startTileY, "entrada")
        }
    }

    /** Renderiza la pantalla de ([xTile],[yTile]) del nivel [lv] con esa casilla recuadrada. */
    private fun pintaCasilla(
        rom: ByteArray,
        header: SnesHeader,
        lv: Int,
        xTile: Int,
        yTile: Int,
        etiqueta: String,
    ) {
        val mapa = SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) ?: return
        val pantalla = xTile / 16
        val c0 = pantalla * 16
        val ancho = minOf(16, mapa.mapWidth - c0)
        if (ancho <= 0) return
        val bi = java.awt.image.BufferedImage(
            ancho * 16, mapa.mapHeight * 16, java.awt.image.BufferedImage.TYPE_INT_ARGB
        )
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
        val cx = xTile - c0
        if (cx in 0 until ancho && yTile in 0 until mapa.mapHeight) {
            for (t in 0 until 16) {
                bi.setRGB(cx * 16 + t, yTile * 16, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16 + t, yTile * 16 + 15, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16, yTile * 16 + t, 0xFFFF00FF.toInt())
                bi.setRGB(cx * 16 + 15, yTile * 16 + t, 0xFFFF00FF.toInt())
            }
        }
        val f = File(out, "%s_%03X_p%d.png".format(etiqueta, lv, pantalla))
        javax.imageio.ImageIO.write(bi, "png", f)
        println("    PNG: $f")
    }

    /**
     * Pinta la PANTALLA DE LLEGADA de cada warp que sale del nivel [lv], con la casilla de
     * aterrizaje recuadrada. Es la comprobación a ojo: si el aterrizaje cae en una tubería
     * de salida, la pantalla está bien; si cae en mitad de un llano cualquiera, no.
     */
    private fun pintaLlegadas(rom: ByteArray, header: SnesHeader, lv: Int) {
        for (w in runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrDefault(emptyList())) {
            println("  llegada %03X -> %03X en (%d,%d), pantalla %d"
                .format(lv, w.destLevel, w.destXTile, w.destYTile, w.destXTile / 16))
            pintaCasilla(rom, header, w.destLevel, w.destXTile, w.destYTile, "llegada_de_%03X".format(lv))
        }
    }

    /** Vuelca los bytes crudos de los casos que cita el encargo, para poder comprobarlos. */
    private fun crudoDeLosCitados(rom: ByteArray, header: SnesHeader, delta: Int) {
        println("\n=== CASOS CITADOS: bytes crudos de sus entradas ===")
        for (lv in intArrayOf(0x1BE, 0x1CB, 0x1CA, 0x1FD, 0x102, 0x105, 0x106, 0x103)) {
            val f000 = rd(rom, delta, 0xF000, lv)
            val f200 = rd(rom, delta, 0xF200, lv)
            val f400 = rd(rom, delta, 0xF400, lv)
            val f600 = rd(rom, delta, 0xF600, lv)
            println(
                ("  %03X principal: F000=%02X F200=%02X F400=%02X F600=%02X" +
                    "  -> yIdx=%X xIdx=%X accion=%d PANTALLA=%d (midway=%d)").format(
                    lv, f000, f200, f400, f600,
                    f000 and 0xF, f200 and 0x7, (f200 and 0x38) shr 3, f600 and 0x1F, f400 shr 4,
                )
            )
        }
        println("  --- las salidas SECUNDARIAS que salen de esos niveles ---")
        for (lv in intArrayOf(0x1BE, 0x1CB, 0x1CA, 0x1FD)) {
            val conn = runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull() ?: continue
            for (ex in conn.exits) {
                val se = ex.secondaryEntrance ?: continue
                val n = se.number
                val f800 = rd(rom, delta, 0xF800, n)
                val fa00 = rd(rom, delta, 0xFA00, n)
                val fc00 = rd(rom, delta, 0xFC00, n)
                val fe00 = rd(rom, delta, 0xFE00, n)
                println(
                    ("  %03X sale por la secundaria %d: F800=%02X FA00=%02X FC00=%02X FE00=%02X" +
                        "  -> destino=%03X yIdx=%X xIdx=%X PANTALLA=%d accion=%d").format(
                        lv, n, f800, fa00, fc00, fe00,
                        (n and 0x100) or f800, fa00 and 0xF, fc00 shr 5, fc00 and 0x1F, fe00 and 0x7,
                    )
                )
            }
        }
    }

    /** ¿Cuántas entradas tienen la pantalla a 0? Si casi ninguna, ignorarla es un error gordo. */
    private fun cuantasPantallasNoCero(rom: ByteArray, delta: Int) {
        println("\n=== CUÁNTAS ENTRADAS NO EMPIEZAN EN LA PANTALLA 0 ===")
        var principalNoCero = 0
        for (lv in 0 until 0x200) if ((rd(rom, delta, 0xF600, lv) and 0x1F) != 0) principalNoCero++
        var secNoCero = 0
        for (n in 0 until 0x200) if ((rd(rom, delta, 0xFC00, n) and 0x1F) != 0) secNoCero++
        println("  entradas PRINCIPALES con pantalla != 0: $principalNoCero de 512")
        println("  entradas SECUNDARIAS con pantalla != 0: $secNoCero de 512")
        println("  niveles cuya entrada PRINCIPAL no está en la pantalla 0: " +
            (0 until 0x200).filter { (rd(rom, delta, 0xF600, it) and 0x1F) != 0 }
                .joinToString(" ") { "%03X:p%d".format(it, rd(rom, delta, 0xF600, it) and 0x1F) })
    }

    /** Dónde aterrizan hoy los warps de toda la ROM, y por qué clase de entrada. */
    private fun dondeAterrizanLosWarps(rom: ByteArray, header: SnesHeader) {
        println("\n=== DÓNDE ATERRIZAN LOS WARPS (toda la ROM) ===")
        var total = 0
        var porSecundaria = 0
        var enPantalla0 = 0
        val detalle = StringBuilder()
        val conSalida = (0 until 0x200).mapNotNull { lv ->
            val conn = runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull()
            if (conn == null || conn.exits.isEmpty()) null else lv to conn
        }
        for ((lv, conn) in conSalida) {
            val secPorPantalla = conn.exits.associate { it.fromScreen to (it.secondaryEntrance != null) }
            for (w in runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrDefault(emptyList())) {
                total++
                if (secPorPantalla[w.mouthXTile / 16] == true) porSecundaria++
                if (w.destXTile / 16 == 0) enPantalla0++
                detalle.append(
                    "%03X (%d,%d) -> %03X (%d,%d) pantallaDestino=%d sec=%s\n".format(
                        lv, w.xTile, w.yTile, w.destLevel, w.destXTile, w.destYTile,
                        w.destXTile / 16, secPorPantalla[w.mouthXTile / 16],
                    )
                )
            }
        }
        println("  warps totales: $total")
        println("  de ellos, por salida SECUNDARIA: $porSecundaria")
        println("  que aterrizan en la PANTALLA 0 del destino: $enPantalla0")
        File(out, "aterrizajes.txt").writeText(detalle.toString())
        println("  detalle en ${File(out, "aterrizajes.txt")}")
        // Control de cordura del reader de entrada principal.
        val start = SmwLevelStartReader.read(rom, header, 0x102)
        println("  entrada principal de 102 segun el reader: (${start?.startTileX},${start?.startTileY})")
    }
}
