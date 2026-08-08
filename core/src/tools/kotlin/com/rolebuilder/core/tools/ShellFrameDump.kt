package com.rolebuilder.core.tools

import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SnesDecoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * DEPURACIÓN: vuelca los fotogramas de la tabla OAM genérica de los cuatro Koopas para
 * MIRAR cuál es el caparazón, en vez de deducirlo.
 *
 * Contexto (por qué existe esta herramienta). El caparazón se dibuja con
 * `StunnedShellDraw` ($01:9806) → `StunnedShellGFXRt_01980F` ($01:980F), que hace
 * `spr_table1602[k] = a` (a = 6 quieto / 8, y {6,7,8,7} girando en `kKickedShellGFXRt_
 * ShellAniTiles`) y llama a `GenericGFXRtDraw1Tile16x16` ($01:9F0D). Esa rutina resuelve
 * el nº de tesela así:
 *
 *     charnum = kGenericSpriteOAMData_Tiles[ TilesOffset[spriteid] + spr_table1602[k] ]
 *
 * O sea: los 6/7/8 son FOTOGRAMAS que se suman al offset del sprite dentro de una tabla
 * PLANA y COMPARTIDA por todos los ids 0x00-0x53.
 *
 * ⚠ LO QUE ESTA HERRAMIENTA DESTAPÓ, y por lo que merece la pena conservarla: volcando los
 * ids 0x00-0x03 NO sale ningún caparazón, sale un bicho NARANJA sin caparazón con los pies
 * de color (el "beach koopa"). Los ids están INVERTIDOS respecto a lo que asumía el motor.
 * Tres fuentes independientes coinciden:
 *
 *  1. `kSprXXX_Generic_Spr0to13Prop` ($01): el bit 0x40 (dibujo de DOS teselas apiladas, o
 *     sea el Koopa ALTO que lleva caparazón) está en los ids 0x04-0x0B, NO en 0x00-0x03,
 *     que se dibujan con UNA sola tesela (bajos, sin caparazón).
 *  2. SMW Central etiqueta el bloque `C8,CA,CA,CE,CC,86,4E` ($019B8C) como "Shell-less
 *     Koopa": son exactamente los bytes del offset 0x9, que es el que usa el id 0x00.
 *  3. Este volcado, mirado a ojo.
 *
 * Y encaja con `kSprStatus09_Stunned_SpriteKoopasSpawn` = {0,0,0,0,0,1,2,3}, que mapea
 * 0x04→0x00, 0x05→0x01, 0x06→0x02, 0x07→0x03: de CON caparazón a SIN caparazón, mismo color.
 *
 * Conclusión: **0x00-0x03 son los Koopas SIN caparazón y 0x04-0x07 los que lo llevan**. El
 * caparazón hay que buscarlo en los fotogramas de 0x04-0x07, no en los de 0x00-0x03.
 *
 * Esta herramienta pinta esos fotogramas de verdad, desde la ROM del usuario, para poder
 * confirmarlo a ojo antes de tocar el motor. La ROM no se versiona: se pasa por --rom.
 *
 * Uso:
 *   ./gradlew :core:dumpShellFrames --args="--rom smw.sfc --level 0x105 --out out/"
 *
 * Opciones:
 *   --rom     ROM de SMW (obligatoria; nunca se sube al repositorio).
 *   --level   Nivel del que tomar CGRAM/paleta y página de GFX (por defecto 0x105, YI-1).
 *   --frames  Cuántos fotogramas volcar por Koopa (por defecto 9: 0..8).
 *   --scale   Escala de ampliación de cada fotograma (por defecto 6).
 *   --out     Carpeta de salida.
 */
object ShellFrameDump {

    /** Fotogramas que el juego usa para el caparazón (quieto 6/8; girando 6,7,8,7). */
    private val SHELL_FRAMES = setOf(6, 7, 8)

    /**
     * Los ocho Koopas: 0x00-0x03 SIN caparazón (una tesela, bajos) y 0x04-0x07 CON caparazón
     * (dos teselas apiladas, altos). Se vuelcan los dos grupos justamente porque la pregunta
     * abierta es dónde está el gráfico del caparazón, y estaba en el grupo equivocado.
     */
    private val KOOPAS = listOf(
        0x00 to "sin-caparazon verde", 0x01 to "sin-caparazon rojo",
        0x02 to "sin-caparazon azul", 0x03 to "sin-caparazon amarillo",
        0x04 to "CON-caparazon verde", 0x05 to "CON-caparazon rojo",
        0x06 to "CON-caparazon azul", 0x07 to "CON-caparazon amarillo",
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val o = parseArgs(args)
        val romPath = o["rom"] ?: error("Falta --rom (la ROM no se versiona; pásala por ruta)")
        val level = o["level"].parseIntOrNull() ?: 0x105
        val frames = o["frames"]?.toIntOrNull() ?: 9
        val scale = o["scale"]?.toIntOrNull() ?: 6
        val outDir = File(o["out"] ?: "shell_frames").apply { mkdirs() }

        val rom = File(romPath).readBytes()
        val header = SnesDecoder.parseHeader(rom)

        println("Nivel 0x${level.toString(16)} · fotogramas 0..${frames - 1} · los 6/7/8 son el CAPARAZÓN")
        val rows = ArrayList<Pair<String, List<ArgbImage>>>()
        for ((id, name) in KOOPAS) {
            val imgs = SmwEnemyGraphics.dumpOamFrames(rom, header, level, id, frames)
            if (imgs == null) {
                println("  Koopa $name (0x%02X): sin datos (¿ROM no vanilla o nivel sin sprites?)".format(id))
                continue
            }
            rows.add("$name (0x%02X)".format(id) to imgs)
            // Cada fotograma también suelto, por si hay que mirarlo de cerca.
            imgs.forEachIndexed { f, img ->
                val marca = if (f in SHELL_FRAMES) "_CAPARAZON" else ""
                write(img, scale, File(outDir, "koopa_%02X_frame%d%s.png".format(id, f, marca)))
            }
            println("  Koopa $name (0x%02X): %d fotogramas".format(id, imgs.size))
        }
        if (rows.isEmpty()) error("No se pudo volcar ningún Koopa.")

        // Volcado de TODA la VRAM de sprites (512 teselas 8×8, SP1..SP4) con rejilla e
        // índices: sirve para LOCALIZAR el caparazón a ojo y deducir su nº de tesela, en
        // vez de fiarse de una fórmula. Filas 0-15 = SP1, 16-31 = SP2, 32-47 = SP3, 48-63 = SP4.
        val palRow = o["pal"]?.toIntOrNull() ?: 8
        SmwEnemyGraphics.spriteVramSheet(rom, header, level, 0x00, palRow)?.let { vram ->
            val z = 3
            val big = ArgbImage(vram.width * z + 40, vram.height * z + 20)
            for (x in 0 until big.width) for (y in 0 until big.height) big.set(x, y, FONDO)
            blit(vram, z, big, 40, 20)
            // Rejilla cada 8 px de tesela para poder contar filas y columnas.
            for (c in 0..16) for (y in 0 until vram.height * z) pset(big, 40 + c * 8 * z, 20 + y, REJILLA)
            for (r in 0..(512 / 16)) for (x in 0 until vram.width * z) pset(big, 40 + x, 20 + r * 8 * z, REJILLA)
            val f = File(outDir, "vram_sprites_pal$palRow.png")
            write(big, 1, f)
            println("VRAM de sprites (512 teselas, paleta $palRow): ${f.absolutePath}")
            println("  Cada celda es una tesela 8×8. Nº de tesela = fila*16 + columna.")
        }

        // EL CAPARAZÓN, por [SmwEnemyGraphics.shellImage]: los cuatro colores × el ciclo de
        // giro {6,7,8,7}. Es la prueba de que el gráfico real ya se puede pedir por API.
        val shellRows = (0x04..0x07).mapNotNull { id ->
            val fr = SmwEnemyGraphics.SHELL_SPIN_FRAMES.toList().mapNotNull { f ->
                SmwEnemyGraphics.shellImage(rom, header, level, id, f)
            }
            if (fr.isEmpty()) null else "caparazon 0x%02X".format(id) to fr
        }
        if (shellRows.isNotEmpty()) {
            val c = 16 * scale
            val p = 4
            val sh = ArgbImage(p + shellRows[0].second.size * (c + p), p + shellRows.size * (c + p))
            for (x in 0 until sh.width) for (y in 0 until sh.height) sh.set(x, y, FONDO)
            shellRows.forEachIndexed { r, (_, imgs) ->
                imgs.forEachIndexed { f, img -> blit(img, scale, sh, p + f * (c + p), p + r * (c + p)) }
            }
            val sf = File(outDir, "caparazones.png")
            write(sh, 1, sf)
            println("CAPARAZONES (0x04-0x07 × ciclo de giro): ${sf.absolutePath}")
        } else {
            println("No se pudo pintar ningún caparazón (¿ROM no vanilla?).")
        }

        // Hoja de contacto: una fila por Koopa, una columna por fotograma. Los fotogramas
        // del caparazón van marcados con un borde para localizarlos de un vistazo.
        val cell = 16 * scale
        val pad = 4
        val sheet = ArgbImage(pad + rows[0].second.size * (cell + pad), pad + rows.size * (cell + pad))
        for (x in 0 until sheet.width) for (y in 0 until sheet.height) sheet.set(x, y, FONDO)
        rows.forEachIndexed { r, (_, imgs) ->
            imgs.forEachIndexed { f, img ->
                val ox = pad + f * (cell + pad)
                val oy = pad + r * (cell + pad)
                blit(img, scale, sheet, ox, oy)
                if (f in SHELL_FRAMES) borde(sheet, ox - 1, oy - 1, cell + 2, cell + 2, MARCA)
            }
        }
        val sheetFile = File(outDir, "koopa_frames.png")
        write(sheet, 1, sheetFile)
        println()
        println("Hoja de contacto: ${sheetFile.absolutePath}")
        println("Filas: " + rows.joinToString(", ") { it.first })
        println("Columnas: fotogramas 0..${rows[0].second.size - 1}; los 6/7/8 llevan borde = CAPARAZÓN")
    }

    private const val FONDO = 0xFF202024.toInt()
    private const val MARCA = 0xFFFFCC00.toInt()
    private const val REJILLA = 0xFF505060.toInt()

    /** Pega [img] ampliada [scale] veces sobre [dst] en ([ox],[oy]); respeta la transparencia. */
    private fun blit(img: ArgbImage, scale: Int, dst: ArgbImage, ox: Int, oy: Int) {
        for (y in 0 until img.height) for (x in 0 until img.width) {
            val c = img.get(x, y)
            if (c ushr 24 == 0) continue // transparente: deja ver el fondo
            for (dy in 0 until scale) for (dx in 0 until scale) {
                val px = ox + x * scale + dx
                val py = oy + y * scale + dy
                if (px in 0 until dst.width && py in 0 until dst.height) dst.set(px, py, c)
            }
        }
    }

    private fun borde(dst: ArgbImage, x: Int, y: Int, w: Int, h: Int, color: Int) {
        for (i in 0 until w) {
            pset(dst, x + i, y, color); pset(dst, x + i, y + h - 1, color)
        }
        for (i in 0 until h) {
            pset(dst, x, y + i, color); pset(dst, x + w - 1, y + i, color)
        }
    }

    private fun pset(dst: ArgbImage, x: Int, y: Int, color: Int) {
        if (x in 0 until dst.width && y in 0 until dst.height) dst.set(x, y, color)
    }

    private fun write(img: ArgbImage, scale: Int, file: File) {
        val w = img.width * scale
        val h = img.height * scale
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until h) for (x in 0 until w) out.setRGB(x, y, img.get(x / scale, y / scale))
        ImageIO.write(out, "png", file)
    }

    private fun String?.parseIntOrNull(): Int? = this?.let {
        if (it.startsWith("0x", true)) it.drop(2).toIntOrNull(16) else it.toIntOrNull()
    }

    private fun parseArgs(args: Array<String>): Map<String, String> {
        val m = HashMap<String, String>()
        var i = 0
        while (i < args.size) {
            val a = args[i]
            if (a.startsWith("--")) {
                val key = a.drop(2)
                val v = args.getOrNull(i + 1)
                if (v != null && !v.startsWith("--")) { m[key] = v; i++ } else m[key] = "true"
            }
            i++
        }
        return m
    }
}
