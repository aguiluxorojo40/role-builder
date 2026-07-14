package com.rolebuilder.core.tools

import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SnesAssetExtractor
import com.rolebuilder.core.snes.SnesAutoExtractor
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGraphicFormat
import com.rolebuilder.core.snes.SnesGraphicsScanner
import com.rolebuilder.core.snes.SnesPaletteMatcher
import com.rolebuilder.core.snes.SnesTilemap
import com.rolebuilder.core.snes.compression.CompressionCodecs
import com.rolebuilder.core.snes.compression.LcLz2
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Herramienta de escritorio para extraer una hoja de tiles desde una ROM de
 * Super Nintendo usando el mismo decodificador que corre en el dispositivo
 * (`core/snes`). Escribe un PNG y el `Tileset` JSON correspondiente, y sirve
 * de referencia exacta de cómo la UI de `:app` debe llamar al decodificador.
 *
 * Uso (vía Gradle):
 *   ./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out out --offset 0x2000 --format 4bpp"
 *   ./gradlew :core:extractSnesTileset --args="--demo out"   # genera una ROM de prueba y la extrae
 *
 * Opciones:
 *   --rom <ruta>            ROM de entrada (.smc/.sfc/.bin). Con SMC de 512 bytes se detecta sola.
 *   --out <dir>            Carpeta de salida (por defecto: snes_out).
 *   --offset <n>           Offset de los gráficos (dec o 0x...). Por defecto 0.
 *   --format <fmt>         auto | 2bpp | 3bpp | 4bpp | 8bpp | gb2bpp | nes2bpp (por defecto 4bpp).
 *                          Con "auto" el programa adivina el bpp por sí solo.
 *   --tiles <n>            Nº de tiles a extraer (por defecto: los que quepan, máx. 256).
 *   --columns <n>          Columnas de la rejilla (por defecto 16).
 *   --palette-offset <n>  Offset de la paleta CGRAM en la ROM (si se omite, se usa una por defecto).
 *   --tilemap <n>         Offset de un TILEMAP de fondo: colorea cada tile con la sub-paleta REAL
 *                          que el mapa le asigna (bits 10-12), en vez de una paleta única. Requiere --cgram.
 *   --cgram <n>           Offset de la CGRAM real (256 colores) que usan las filas del tilemap.
 *   --tilemap-entries <n> Nº de entradas del tilemap a leer (por defecto 1024 ≈ una pantalla).
 *   --default-row <n>     Fila de CGRAM (0..15) para los tiles que el tilemap no cubre (por defecto 0).
 *   --grayscale           Colorea en escala de grises para ver la FORMA sin conocer la paleta real.
 *   --sprite <WxH>        Agrupa bloques de W×H tiles de 8×8 en un sprite entero (2x2 = 16×16, 4x4 = 32×32).
 *   --name <texto>        Nombre del tileset y base del archivo (por defecto "snes").
 *   --demo <dir>          Genera una ROM de prueba procedural en <dir>/demo.sfc y la extrae.
 */
fun main(args: Array<String>) {
    val opts = parseArgs(args)

    // Modo demo: fabrica una ROM procedural (sin material con copyright) y la extrae.
    val demoDir = opts["demo"]
    val demoCompressedDir = opts["demo-compressed"]
    val romFile: File
    val outDir: File
    if (demoCompressedDir != null) {
        // ROM de prueba con gráficos COMPRIMIDOS en LC_LZ2 a partir de 0x1000,
        // para demostrar la descompresión sin usar ninguna ROM con copyright.
        outDir = File(demoCompressedDir).also { it.mkdirs() }
        romFile = File(outDir, "demo_lz2.sfc")
        romFile.writeBytes(buildCompressedDemoRom())
        println("ROM comprimida de prueba: ${romFile.absolutePath}")
        println("Pruébala con:  --rom ${romFile.name} --offset 0x1000 --decompress auto --format 4bpp")
    } else if (demoDir != null) {
        outDir = File(demoDir).also { it.mkdirs() }
        romFile = File(outDir, "demo.sfc")
        romFile.writeBytes(buildDemoRom())
        println("ROM de prueba generada: ${romFile.absolutePath} (${romFile.length()} bytes)")
    } else {
        val romPath = opts["rom"] ?: run {
            System.err.println("Falta --rom <ruta> (o usa --demo <dir>). Ejecuta sin argumentos para ver la ayuda.")
            printUsage()
            return
        }
        romFile = File(romPath)
        if (!romFile.isFile) {
            System.err.println("No existe la ROM: ${romFile.absolutePath}")
            return
        }
        outDir = File(opts["out"] ?: "snes_out").also { it.mkdirs() }
    }

    val rom = romFile.readBytes()
    val header = SnesDecoder.parseHeader(rom)
    println("Cabecera: \"${header.title}\"  ${header.mapping}  ${header.romTypeDescription}")
    println("  ${header.country} · ${header.licensee} · checksum ${if (header.isChecksumValid) "válido" else "no válido"}")

    // Modo --recipe: "modo fácil FIABLE". Si la ROM es un juego conocido, usa su
    // mapa gráfico real y vuelca sus gráficos limpios, sin adivinar nada.
    if (opts.containsKey("recipe")) {
        val game = SnesGameRecipes.detect(header)
        if (game == null) {
            println("No hay receta para esta ROM (\"${header.title}\"). Usa --gallery o el modo manual.")
            return
        }
        println("Juego reconocido: $game. Extrayendo sus gráficos…")
        val imagesDir = File(outDir, "images").also { it.mkdirs() }
        val findings = SnesGameRecipes.extract(rom, header)
        println("Extraídos ${findings.size} ficheros gráficos:")
        findings.forEachIndexed { i, f ->
            val file = File(imagesDir, "recipe_${"%02d".format(i + 1)}.png")
            ImageIO.write(toBufferedImage(f.image), "png", file)
            println("  ${f.label}: ${f.image.width}x${f.image.height}px " +
                "(${f.format.name.removePrefix("SNES_").lowercase()} @0x${f.offset.toString(16)}) -> ${file.name}")
        }
        return
    }

    // Modo --collision: extrae el MAPA DE COLISIÓN (solidez de cada celda 16×16) de un
    // nivel de SMW, la pieza que faltaba para poder jugarlo. Imprime un mapa ASCII y
    // vuelca una máscara PNG coloreada por clase de solidez.
    if (opts.containsKey("collision")) {
        val level = opts["level"]?.let { parseInt(it) } ?: 0x106
        val map = SnesGameRecipes.smwLevelCollision(rom, header, level)
        if (map == null) {
            println("El nivel 0x${level.toString(16).uppercase()} no tiene datos de colisión " +
                "(¿vertical, sala de jefe o vacío?). Prueba otro --level.")
            return
        }
        println("Colisión del nivel 0x${level.toString(16).uppercase()}: ${map.cols}×${map.rows} celdas.")
        val counts = LinkedHashMap<String, Int>()
        for (s in map.solidity) counts[s.name] = (counts[s.name] ?: 0) + 1
        println("  Solidez: " + counts.entries.joinToString(", ") { "${it.key}=${it.value}" })
        // Vista ASCII: '.' aire · '-' borde de un sentido · '#' sólido · '/' cuesta ·
        // 'X' cuesta doble · '!' pinchos.
        val glyph = mapOf(
            "NONE" to '.', "LEDGE_TOP" to '-', "SOLID" to '#',
            "SLOPE" to '/', "SLOPE_STEEP" to 'X', "SPIKE" to '!',
        )
        val sb = StringBuilder()
        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) sb.append(glyph[map.solidityAt(c, r).name] ?: '?')
            sb.append('\n')
        }
        val imagesDir = File(outDir, "images").also { it.mkdirs() }
        File(outDir, "collision_${level.toString(16)}.txt").writeText(sb.toString())
        // Máscara PNG (4 px/celda) coloreada por clase.
        val color = mapOf(
            "NONE" to 0x00000000, "LEDGE_TOP" to 0xFF7FD07F.toInt(), "SOLID" to 0xFF8B5A2B.toInt(),
            "SLOPE" to 0xFF5AA0FF.toInt(), "SLOPE_STEEP" to 0xFF2060C0.toInt(), "SPIKE" to 0xFFE03030.toInt(),
        )
        val cell = 4
        val mask = ArgbImage(map.cols * cell, map.rows * cell)
        for (r in 0 until map.rows) for (c in 0 until map.cols) {
            val argb = color[map.solidityAt(c, r).name] ?: 0
            for (yy in 0 until cell) for (xx in 0 until cell) mask.set(c * cell + xx, r * cell + yy, argb)
        }
        val png = File(imagesDir, "collision_${level.toString(16)}.png")
        ImageIO.write(toBufferedImage(mask), "png", png)
        val start = com.rolebuilder.core.snes.SmwLevelStartReader.read(rom, header, level)
        if (start != null) {
            println("  Inicio del jugador: píxel (${start.startPixelX}, ${start.startPixelY}) " +
                "= casilla (${start.startTileX}, ${start.startTileY})")
        }
        println("  ASCII -> ${File(outDir, "collision_${level.toString(16)}.txt").name} · máscara -> images/${png.name}")
        return
    }

    // Modo --physics: lee las TABLAS DE FÍSICAS reales del jugador de SMW (acelerar,
    // correr, saltar, caer, gravedad, tope de caída) y las imprime en sus unidades.
    if (opts.containsKey("physics")) {
        val phys = com.rolebuilder.core.snes.SmwPhysicsReader.read(rom, header)
        if (phys == null) {
            println("No se pudieron leer las físicas: la ROM no parece SMW vanilla en las " +
                "direcciones del banco $00 (¿otro juego o un hack que las reubica?).")
            return
        }
        fun ppf(v: Int) = "%.2f".format(phys.toPixelsPerFrame(v))
        println("Físicas del jugador (unidades: 1/16 px por fotograma; 60 fps):")
        println("  Salto parado:        ${phys.baseJumpYSpeed}  (${ppf(phys.baseJumpYSpeed)} px/fotograma hacia arriba)")
        println("  Gravedad (por def.): ${phys.defaultGravity}  (${ppf(phys.defaultGravity)} px/fotograma²)")
        println("  Caída terminal:      ${phys.defaultMaxFall}  (${ppf(phys.defaultMaxFall)} px/fotograma)")
        println("  Muerte (impulso):    ${com.rolebuilder.core.snes.SmwPhysics.DEATH_POP_YSPEED}  (${ppf(com.rolebuilder.core.snes.SmwPhysics.DEATH_POP_YSPEED)} px/fotograma)")
        println("  Rebote pisar (salto mantenido / suelto): " +
            "${com.rolebuilder.core.snes.SmwPhysics.STOMP_BOUNCE_JUMP_HELD} / ${com.rolebuilder.core.snes.SmwPhysics.STOMP_BOUNCE}")
        println("  Tabla de salto ($00:D2BD):     " + phys.jumpYSpeed.joinToString(" "))
        println("  Gravedad ($00:D7A5):           " + phys.gravity.joinToString(" "))
        println("  Caída terminal ($00:D7AF):     " + phys.maxFallSpeed.joinToString(" "))
        println("  Vel. máx. horizontal ($00:D535, primeros 16): " +
            phys.maxXSpeed.take(16).joinToString(" "))
        println("  Aceleración ($00:D345, primeros 8 words):     " +
            phys.marioXAccel.take(8).joinToString(" "))
        return
    }

    // Modo --mario: vuelca la hoja de sprites de Mario (GFX32) coloreada de la ROM.
    if (opts.containsKey("mario")) {
        val img = com.rolebuilder.core.snes.SnesGameRecipes.smwMarioSheet(rom, header)
        if (img == null) { println("No se pudo leer la hoja de Mario (¿ROM no SMW?)."); return }
        val imagesDir = File(outDir, "images").also { it.mkdirs() }
        val png = File(imagesDir, "mario.png")
        ImageIO.write(toBufferedImage(img), "png", png)
        println("Hoja de Mario: ${img.width}x${img.height}px (${img.width / 8}x${img.height / 8} teselas) -> images/${png.name}")
        return
    }

    // Modo --enemies: hornea el ATLAS de enemigos del catálogo curado
    // (SmwEnemyGraphics.curatedIds, un fotograma 16×16 por id, en su orden) con el
    // sprite y color REALES de la ROM. Es el horneado oficial de
    // app/src/main/assets/sprites/enemies.png: si el catálogo cambia, regenerar con
    // este modo mantiene atlas y código sincronizados.
    if (opts.containsKey("enemies")) {
        val enemyGfx = com.rolebuilder.core.snes.SmwEnemyGraphics
        val ids = enemyGfx.curatedIds
        // Niveles de referencia por id: TODOS los que de verdad contienen ese enemigo
        // (su VRAM de sprites tiene los gráficos correctos cargados). Entre ellos se
        // VOTA: se renderiza en cada uno y gana el aspecto MAYORITARIO — algún
        // sub-nivel puede cargar otro sprite-set y daría un gráfico equivocado
        // (fuentes, tiles ajenos); en la mayoría quedan en minoría y se descartan.
        val levelsWithId = HashMap<Int, MutableList<Int>>()
        for (level in 0 until 0x200) {
            for ((id, _, _) in SnesGameRecipes.smwLevelEnemies(rom, header, level)) {
                levelsWithId.getOrPut(id) { ArrayList() }.add(level)
            }
        }
        val fallbackLevels = listOf(0x106, 0x024, 0x0C7, 0x022, 0x0C5, 0x101, 0x105, 0x001, 0x002)
        val atlas = ArgbImage(ids.size * 16, 16)
        var missing = 0
        println("Atlas de enemigos (${ids.size} fotogramas de 16×16, aspecto por mayoría):")
        ids.forEachIndexed { frame, id ->
            val candidates = (levelsWithId[id] ?: fallbackLevels).take(16)
            // hash de píxeles → (imagen, niveles que la producen)
            val variants = LinkedHashMap<Int, Pair<ArgbImage, MutableList<Int>>>()
            for (l in candidates) {
                val img = enemyGfx.spriteImage(rom, header, l, id) ?: continue
                val key = img.pixels.contentHashCode()
                variants.getOrPut(key) { img to ArrayList() }.second.add(l)
            }
            val winner = variants.values.maxByOrNull { it.second.size }
            if (winner != null) {
                val img = winner.first
                for (y in 0 until 16) for (x in 0 until 16) {
                    atlas.set(frame * 16 + x, y, img.pixels[y * 16 + x])
                }
            } else {
                missing++
            }
            println("  fotograma %2d · id %02X · %-24s %s".format(
                frame, id, enemyGfx.nameOf(id) ?: "?",
                if (winner != null) {
                    "niveles ${winner.second.size}/${candidates.size} " +
                        "(0x${winner.second.first().toString(16).uppercase()}…)" +
                        if (variants.size > 1) " · ${variants.size} variantes" else ""
                } else "SIN GRÁFICO"))
        }
        val imagesDir = File(outDir, "images").also { it.mkdirs() }
        val png = File(imagesDir, "enemies.png")
        ImageIO.write(toBufferedImage(atlas), "png", png)
        println("Atlas: ${atlas.width}x${atlas.height}px -> images/${png.name}" +
            if (missing > 0) "  (¡$missing sin gráfico!)" else "")
        return
    }

    // Modo --music: renderiza la MÚSICA del juego (N-SPC + S-DSP portados) a un
    // .wav de escritorio — la forma de PROBAR cómo suena sin pasar por la app, y
    // de cazar bugs de mezcla analizando la señal. --song elige la canción
    // (1 = nivel), --seconds la duración.
    if (opts.containsKey("music")) {
        val song = opts["song"]?.let { parseInt(it) } ?: 1
        val seconds = opts["seconds"]?.let { parseInt(it) } ?: 30
        val delta = header.headerOffset - 0x7FC0 // delta por cabecera de copiador SMC
        val bank = if (opts["bank"] == "overworld") {
            com.rolebuilder.core.snes.SmwMusic.OVERWORLD_MUSIC
        } else {
            com.rolebuilder.core.snes.SmwMusic.LEVEL_MUSIC
        }
        val aram = com.rolebuilder.core.snes.SmwMusic.assembleAram(rom, delta, bank)
        if (aram == null) { println("No se pudo ensamblar la ARAM (¿ROM no SMW?)."); return }
        val renderer = com.rolebuilder.core.snes.SmwMusicRenderer(aram)
        if (opts.containsKey("noecho")) { renderer.debugDisableEchoWrites = true; println("(eco DESACTIVADO para diagnóstico)") }
        renderer.selectSong(song)
        println("Renderizando canción $song, $seconds s…")
        val pcm = renderer.render(seconds)
        // Diagnóstico de solape eco↔muestras: rango de ARAM que el eco pisa vs el
        // directorio de muestras BRR. Si se solapan, el eco corrompe las muestras.
        val (elo, ehi) = renderer.debugEchoRange()
        if (ehi > elo) {
            val dir = com.rolebuilder.core.snes.SmwMusic.readSampleDirectory(aram)
            val sLo = dir.minOfOrNull { it.startAddr } ?: 0
            val sHi = dir.maxOfOrNull { it.loopAddr } ?: 0
            val overlap = elo < sHi && sLo < ehi
            println("Eco escribe en ARAM 0x${elo.toString(16)}..0x${ehi.toString(16)}; " +
                "muestras BRR 0x${sLo.toString(16)}..0x${sHi.toString(16)}+ " +
                "→ ${if (overlap) "¡SOLAPAN! (el eco corrompe las muestras)" else "sin solape"}")
        }
        // WAV PCM 16-bit estéreo 32000 Hz.
        val wav = File(outDir, "music_song$song.wav")
        java.io.DataOutputStream(wav.outputStream().buffered()).use { o ->
            val dataLen = pcm.size * 2
            fun le32(v: Int) { o.write(v); o.write(v shr 8); o.write(v shr 16); o.write(v shr 24) }
            fun le16(v: Int) { o.write(v); o.write(v shr 8) }
            o.writeBytes("RIFF"); le32(36 + dataLen); o.writeBytes("WAVE")
            o.writeBytes("fmt "); le32(16); le16(1); le16(2); le32(32000); le32(32000 * 4); le16(4); le16(16)
            o.writeBytes("data"); le32(dataLen)
            for (s in pcm) le16(s.toInt() and 0xFFFF)
        }
        // Diagnóstico de mezcla: saturación y envolvente por segundos.
        var clipped = 0
        for (s in pcm) if (s.toInt() == 32767 || s.toInt() == -32768) clipped++
        println("WAV: ${wav.absolutePath} (${pcm.size / 2} frames)")
        println("Muestras saturadas: $clipped (${"%.3f".format(100.0 * clipped / pcm.size)}%)")
        val win = 32000 * 2 // 1 s estéreo
        val env = StringBuilder("Envolvente RMS por segundo: ")
        var t = 0
        while (t + win <= pcm.size) {
            var acc = 0.0
            for (i in t until t + win) acc += pcm[i].toDouble() * pcm[i]
            env.append("%.0f ".format(Math.sqrt(acc / win)))
            t += win
        }
        println(env)
        return
    }

    // Modo --powerups: lista los estados de powerup de Mario y qué cambian.
    if (opts.containsKey("powerups")) {
        println("Powerups de Mario (id · alto · agacha/rompe/fuego/vuela):")
        for (pu in com.rolebuilder.core.snes.SmwPowerup.entries) {
            println("  ${pu.id} ${pu.name.padEnd(6)}: ${pu.heightTiles} casilla(s) · " +
                "agacha=${pu.canDuck} rompe=${pu.canBreakBlocks} fuego=${pu.canShootFire} capa=${pu.canFlyWithCape}")
        }
        return
    }

    // Modo --level-info: ficha del nivel (tamaño, modo, música, paletas, tiempo).
    if (opts.containsKey("level-info")) {
        val level = opts["level"]?.let { parseInt(it) } ?: 0x106
        val info = com.rolebuilder.core.snes.SnesGameRecipes.smwLevelInfo(rom, header, level)
        if (info == null) { println("Sin cabecera válida para el nivel 0x${level.toString(16)}."); return }
        println("Ficha del nivel 0x${level.toString(16).uppercase()}:")
        println("  Tamaño: ${info.screens} pantallas (${info.widthTiles} casillas de ancho)")
        println("  Modo: ${info.levelMode}  ·  Música: ${info.musicIndex}  ·  Tiempo: ${info.startTime}")
        println("  Paletas → fondo ${info.bgPalette}, color de fondo ${info.backgroundColor}, " +
            "FG ${info.fgPalette}, sprites ${info.spritePalette}")
        println("  Gráficos → sprite-set ${info.spriteGfx}, tileset FG ${info.fgTileset}")
        return
    }

    // Modo --sprite-behavior: lee las 6 tablas "tweaker" de COMPORTAMIENTO de sprites
    // (cómo se mueve/choca/se pisa cada enemigo) y las vuelca por id de sprite.
    if (opts.containsKey("sprite-behavior")) {
        val list = com.rolebuilder.core.snes.SmwSpriteBehaviorReader.read(rom, header)
        if (list == null) {
            println("No se pudieron leer los tweakers de sprites: ¿ROM no SMW vanilla?")
            return
        }
        println("Comportamiento de ${list.size} tipos de sprite (id: 1656 1662 166e 167a 1686 190f | hitbox):")
        val show = opts["id"]?.let { listOf(parseInt(it)) } ?: (0..0x20)
        for (id in show) {
            val b = list.getOrNull(id) ?: continue
            println("  %02X: %02x %02x %02x %02x %02x %02x | hitbox %02x".format(
                b.id, b.b1656, b.b1662, b.b166e, b.b167a, b.b1686, b.b190f, b.spriteClipping))
        }
        return
    }

    // Modo --play-sim: integra TODO lo extraído (colisión + físicas + inicio) en el
    // motor de plataformas y simula unos fotogramas, para probar que Mario aparece y
    // se posa sobre el suelo REAL del nivel.
    if (opts.containsKey("play-sim")) {
        val level = opts["level"]?.let { parseInt(it) } ?: 0x106
        val frames = opts["frames"]?.let { parseInt(it) } ?: 180
        val col = com.rolebuilder.core.snes.SnesGameRecipes.smwLevelCollision(rom, header, level)
        val phys = com.rolebuilder.core.snes.SmwPhysicsReader.read(rom, header)
        val start = com.rolebuilder.core.snes.SmwLevelStartReader.read(rom, header, level)
        if (col == null || phys == null || start == null) {
            println("Faltan datos para simular (colisión/físicas/inicio) del nivel 0x${level.toString(16)}.")
            return
        }
        val powerup = when (opts["powerup"]?.lowercase()) {
            "big", "grande" -> com.rolebuilder.core.snes.SmwPowerup.BIG
            "cape", "capa" -> com.rolebuilder.core.snes.SmwPowerup.CAPE
            "fire", "fuego" -> com.rolebuilder.core.snes.SmwPowerup.FIRE
            else -> com.rolebuilder.core.snes.SmwPowerup.SMALL
        }
        val env = when (opts["env"]?.lowercase()) {
            "ice", "hielo" -> com.rolebuilder.core.engine.platformer.PlatformerEnvironment.ICE
            "water", "agua" -> com.rolebuilder.core.engine.platformer.PlatformerEnvironment.WATER
            else -> com.rolebuilder.core.engine.platformer.PlatformerEnvironment.LAND
        }
        val engine = com.rolebuilder.core.engine.platformer.PlatformerEngine(
            cols = col.cols, rows = col.rows,
            solidityAt = { c, r -> col.solidityAt(c, r) },
            startPixelX = start.startPixelX, startPixelY = start.startPixelY,
            tuning = com.rolebuilder.core.engine.platformer.PlatformerTuning.fromSmw(phys, powerup, env),
        )
        println("Simulando ${frames} fotogramas del nivel 0x${level.toString(16)} " +
            "(inicio casilla ${start.startTileX},${start.startTileY})…")
        var landedAtFrame = -1
        for (f in 0 until frames) {
            engine.tick()
            if (engine.player.onGround && landedAtFrame < 0) landedAtFrame = f
        }
        val p = engine.player
        println("  Tras $frames fotogramas: píxel (%.1f, %.1f) = casilla (%d, %d), onGround=%b, dead=%b"
            .format(p.x, p.y, (p.x / 16).toInt(), (p.y / 16).toInt(), p.onGround, p.dead))
        if (landedAtFrame >= 0) println("  Mario se posó sobre el suelo en el fotograma $landedAtFrame.")
        return
    }

    // Modo --gallery: "modo fácil". Busca gráficos automáticamente y vuelca cada
    // hallazgo como una miniatura en color, sin pedir offsets ni formatos.
    if (opts.containsKey("gallery")) {
        val imagesDir = File(outDir, "images").also { it.mkdirs() }
        val findings = SnesAutoExtractor.findGraphics(rom, maxResults = 24)
        println("Encontrados ${findings.size} gráficos automáticamente:")
        findings.forEach { f ->
            val file = File(imagesDir, "auto_${"%02d".format(findings.indexOf(f) + 1)}.png")
            ImageIO.write(toBufferedImage(f.image), "png", file)
            println("  ${f.label}: ${f.image.width}x${f.image.height}px  " +
                "(${if (f.compressed) "comprimido" else "directo"} @0x${f.offset.toString(16)}, " +
                "${f.format.name.removePrefix("SNES_").lowercase()}, calidad ${"%.2f".format(f.score)}) -> ${file.name}")
        }
        return
    }

    // --format auto: se decide el bpp por sí solo (tras descomprimir, si procede).
    val autoFormat = opts["format"]?.lowercase() == "auto"
    var format = if (autoFormat) SnesGraphicFormat.SNES_4BPP else parseFormat(opts["format"] ?: "4bpp")

    // Modo --scan: autodetecta zonas con gráficos. Con --decompress prueba a
    // descomprimir en cada offset (localiza bloques comprimidos); sin él, busca
    // gráficos sin comprimir. Con --format auto, además adivina el bpp de cada uno.
    if (opts.containsKey("scan")) {
        val scanDecompress = opts["decompress"]
        val scanSprites = opts.containsKey("sprites")
        if (scanDecompress != null) {
            val hits = scanForCompressedGraphics(rom, format, sprites = scanSprites)
            if (hits.isEmpty()) {
                println("No se encontraron bloques descomprimibles con ${if (scanSprites) "hojas de sprites" else "gráficos"} (¿otro formato de compresión?).")
            } else {
                val kind = if (scanSprites) "hojas de SPRITES" else "gráficos"
                println("Bloques comprimidos con $kind, prueba: --offset <X> --decompress auto${if (autoFormat) " --format auto" else " ($format)"}")
                hits.forEach { (off, score, size) ->
                    val fmt = if (autoFormat) {
                        val data = CompressionCodecs.autoDecompress(rom, off, format)?.result?.data
                        val g = data?.let { SnesGraphicsScanner.detectBestFormat(it, 0) }
                        g?.let { " · ${formatShortName(it.format)}" } ?: ""
                    } else ""
                    println("  0x${off.toString(16).uppercase()}  (coherencia ${"%.2f".format(score)}, ${size}B)$fmt")
                }
            }
        } else {
            val candidates = if (scanSprites) {
                SnesGraphicsScanner.findSpriteCandidates(rom, format)
            } else {
                SnesGraphicsScanner.findCandidates(rom, format)
            }
            if (candidates.isEmpty()) {
                println("No se encontraron ${if (scanSprites) "hojas de sprites" else "zonas gráficas"} con $format (¿gráficos comprimidos?).")
            } else {
                val kind = if (scanSprites) "HOJAS DE SPRITES" else "gráficos SIN comprimir"
                println("Candidatos de $kind${if (autoFormat) " (bpp autodetectado)" else " ($format)"}, prueba estos offsets:")
                candidates.forEach {
                    val fmt = if (autoFormat) {
                        SnesGraphicsScanner.detectBestFormat(rom, it.offset)?.let { g -> " · ${formatShortName(g.format)}" } ?: ""
                    } else ""
                    println("  0x${it.offset.toString(16).uppercase()}  (score ${"%.2f".format(it.score)})$fmt")
                }
            }
        }
        return
    }

    // Si no se indica offset, se autodetecta el mejor candidato.
    val offset = opts["offset"]?.let { parseInt(it) }
        ?: SnesGraphicsScanner.findCandidates(rom, format).firstOrNull()?.offset?.also {
            println("Offset autodetectado: 0x${it.toString(16).uppercase()} (usa --offset para fijarlo)")
        } ?: 0
    // --decompress <auto|nombre>: descomprime el bloque en offset y extrae de su salida.
    // Los tiles salen de los bytes descomprimidos; la paleta sigue leyéndose de la ROM.
    val decompressMode = opts["decompress"]
    val tileRom: ByteArray
    val tileOffset: Int
    if (decompressMode != null) {
        val data = when {
            decompressMode.equals("auto", true) -> {
                val auto = CompressionCodecs.autoDecompress(rom, offset, format)
                if (auto == null) {
                    System.err.println("Ningún códec conocido produjo gráficos en 0x${offset.toString(16).uppercase()}.")
                    return
                }
                println("Códec detectado: ${auto.codec.name} (coherencia ${"%.2f".format(auto.score)})")
                auto.result.data
            }
            else -> {
                val codec = CompressionCodecs.all.firstOrNull { it.name.contains(decompressMode, true) }
                    ?: CompressionCodecs.all.first()
                runCatching { codec.decompress(rom, offset) }.getOrElse {
                    System.err.println("No se pudo descomprimir con ${codec.name}: ${it.message}")
                    return
                }.also { println("Descomprimidos ${it.data.size} bytes con ${codec.name}") }.data
            }
        }
        tileRom = data
        tileOffset = 0
    } else {
        tileRom = rom
        tileOffset = offset
    }

    // Con --format auto, ahora que tenemos los bytes finales (descomprimidos o no),
    // se decide el bpp por aptitud normalizada.
    if (autoFormat) {
        val guess = SnesGraphicsScanner.detectBestFormat(tileRom, tileOffset)
        if (guess != null) {
            format = guess.format
            println("Formato autodetectado: ${formatShortName(format)} (aptitud ${"%.2f".format(guess.fitness)})")
        } else {
            println("No se pudo autodetectar el formato; se usa ${formatShortName(format)}.")
        }
    }

    val columns = parseInt(opts["columns"] ?: "16")
    val available = SnesAssetExtractor.availableTiles(tileRom.size, tileOffset, format)
    val tileCount = (opts["tiles"]?.let { parseInt(it) } ?: available).coerceIn(1, minOf(available, 256))

    // Paleta: --grayscale (vista de formas) > offset explícito > emparejado
    // automático contra ESTOS tiles (la que mejor les encaja) > colores vivos.
    val palette: IntArray = when {
        opts.containsKey("grayscale") -> SnesDecoder.grayscalePalette(format.colorCount).also {
            println("Paleta en escala de grises (vista de formas: ignora el color real)")
        }
        opts["palette-offset"] != null ->
            SnesDecoder.parsePalette(rom, parseInt(opts["palette-offset"]!!), format.colorCount)
        else -> {
            val match = SnesPaletteMatcher
                .rankPalettes(tileRom, tileOffset, format, tileCount, SnesDecoder.scanRomForPalettes(rom))
                .firstOrNull()
            match?.also {
                val sub = if (it.window > 0) " (sub-paleta desde el color ${it.window})" else ""
                println("Paleta emparejada con el gráfico: ${it.source.name}$sub · afinidad ${"%.2f".format(it.score)}")
            }?.colors ?: defaultPalette(format.colorCount)
        }
    }

    val name = opts["name"] ?: "snes"
    // --tilemap + --cgram: color REAL de fondo. En vez de una paleta única para
    // toda la hoja, se lee la sub-paleta que el tilemap asigna a CADA tile (bits
    // 10-12 de cada entrada) contra la CGRAM real. Los tiles que el mapa no cubre
    // usan --default-row. Los números de tile del mapa se toman relativos al tile
    // extraído (tile 0 = primer tile desde --offset).
    val tilemapOffset = opts["tilemap"]?.let { parseInt(it) }
    val cgramOffset = opts["cgram"]?.let { parseInt(it) }
    // --sprite WxH (en tiles de 8×8): agrupa cada bloque en un sprite entero.
    val spriteDim = opts["sprite"]?.let { parseSpriteDim(it) }
    val sheet = if (spriteDim != null) {
        val (sw, sh) = spriteDim
        val availSprites = SnesAssetExtractor.availableSprites(tileRom.size, tileOffset, format, sw, sh)
        val spriteCount = (opts["tiles"]?.let { parseInt(it) / (sw * sh) } ?: availSprites)
            .coerceIn(1, minOf(availSprites, 256))
        println("Atlas de sprites: agrupando ${sw}×${sh} tiles → celdas de ${sw * 8}×${sh * 8}px ($spriteCount sprites)")
        SnesAssetExtractor.extractSpriteAtlas(
            tileRom, tileOffset, format, palette, spriteCount, sw, sh, columns,
        )
    } else if (tilemapOffset != null && cgramOffset != null) {
        val cgram = SnesTilemap.readCgram(rom, cgramOffset)
        val entryCount = opts["tilemap-entries"]?.let { parseInt(it) } ?: 1024
        val entries = SnesTilemap.parseTilemap(rom, tilemapOffset, entryCount)
        val defaultRow = opts["default-row"]?.let { parseInt(it) } ?: 0
        val selector = SnesTilemap.paletteSelector(entries, cgram, defaultRow, 0 until tileCount)
        val assigned = SnesTilemap.assignedPaletteByTile(entries, 0 until tileCount)
        println(
            "Color por TILEMAP (0x${tilemapOffset.toString(16)}) + CGRAM (0x${cgramOffset.toString(16)}): " +
                "${assigned.size} tiles con sub-paleta propia; el resto usa la fila $defaultRow"
        )
        SnesAssetExtractor.extractTileSheet(
            tileRom, tileOffset, format, tileCount, columns, paletteForTile = selector,
        )
    } else {
        SnesAssetExtractor.extractTileSheet(tileRom, tileOffset, format, palette, tileCount, columns)
    }
    val imageName = "$name.png"

    val imagesDir = File(outDir, "images").also { it.mkdirs() }
    ImageIO.write(toBufferedImage(sheet.image), "png", File(imagesDir, imageName))

    val tileset = SnesAssetExtractor.toTileset(sheet, id = 100, name = name, imageFileName = imageName)
    val json = Json { prettyPrint = true }
    File(outDir, "$name.tileset.json").writeText(json.encodeToString(Tileset.serializer(), tileset))

    println(
        "Extraídos $tileCount tiles ($format) desde 0x${offset.toString(16)} -> " +
            "${sheet.image.width}x${sheet.image.height}px, rejilla ${sheet.columns}x${sheet.rows}"
    )
    println("PNG:     ${File(imagesDir, imageName).absolutePath}")
    println("Tileset: ${File(outDir, "$name.tileset.json").absolutePath}")
}

// ------------------------------------------------------------------ helpers

private fun parseArgs(args: Array<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (a.startsWith("--")) {
            val key = a.substring(2)
            val next = args.getOrNull(i + 1)
            if (next != null && !next.startsWith("--")) {
                map[key] = next; i += 2
            } else {
                map[key] = "true"; i += 1
            }
        } else i += 1
    }
    return map
}

private fun parseInt(s: String): Int =
    if (s.startsWith("0x") || s.startsWith("0X")) s.substring(2).toInt(16) else s.toInt()

/**
 * Interpreta "2x2" (tiles) o "16x16" (píxeles, múltiplos de 8) como el tamaño de
 * sprite en TILES de 8×8. También acepta un solo número ("16" → 16×16 px).
 */
private fun parseSpriteDim(s: String): Pair<Int, Int> {
    val parts = s.lowercase().split("x", "×").map { it.trim().toIntOrNull() ?: 0 }
    val w = parts.getOrElse(0) { 0 }
    val h = parts.getOrElse(1) { w }
    fun toTiles(v: Int) = (if (v >= 8) v / 8 else v).coerceAtLeast(1)
    return toTiles(w) to toTiles(h)
}

private fun formatShortName(f: SnesGraphicFormat): String = when (f) {
    SnesGraphicFormat.SNES_2BPP -> "2bpp"
    SnesGraphicFormat.SNES_3BPP -> "3bpp"
    SnesGraphicFormat.SNES_4BPP -> "4bpp"
    SnesGraphicFormat.SNES_8BPP -> "8bpp"
    SnesGraphicFormat.GB_2BPP -> "gb2bpp"
    SnesGraphicFormat.NES_2BPP -> "nes2bpp"
}

private fun parseFormat(s: String): SnesGraphicFormat = when (s.lowercase()) {
    "2bpp", "snes2bpp" -> SnesGraphicFormat.SNES_2BPP
    "3bpp", "snes3bpp" -> SnesGraphicFormat.SNES_3BPP
    "4bpp", "snes4bpp" -> SnesGraphicFormat.SNES_4BPP
    "8bpp", "snes8bpp" -> SnesGraphicFormat.SNES_8BPP
    "gb2bpp", "gb" -> SnesGraphicFormat.GB_2BPP
    "nes2bpp", "nes" -> SnesGraphicFormat.NES_2BPP
    else -> error("Formato desconocido: $s (usa 2bpp|3bpp|4bpp|8bpp|gb2bpp|nes2bpp)")
}

/** Colores vivos de respaldo (índice 0 transparente): la salida siempre en color. */
private val VIVID_16 = intArrayOf(
    0x00000000, 0xFFE53935.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(),
    0xFFFDD835.toInt(), 0xFF8E24AA.toInt(), 0xFF00ACC1.toInt(), 0xFFF5F5F5.toInt(),
    0xFF6D4C41.toInt(), 0xFFFF7043.toInt(), 0xFF9CCC65.toInt(), 0xFF5C6BC0.toInt(),
    0xFFFFB300.toInt(), 0xFFEC407A.toInt(), 0xFF26A69A.toInt(), 0xFF212121.toInt(),
)

private fun defaultPalette(colorCount: Int): IntArray =
    IntArray(colorCount) { i -> if (i < VIVID_16.size) VIVID_16[i] else 0xFF000000.toInt() }

private fun toBufferedImage(img: ArgbImage): BufferedImage {
    val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    out.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
    return out
}

/**
 * Construye una ROM LoROM mínima de 64 KiB con cabecera válida, una paleta CGRAM
 * en 0x100 y un patrón de tiles 4bpp reconocible en 0x2000. Permite probar la
 * extracción de punta a punta sin usar ninguna ROM con derechos de autor.
 */
private fun buildDemoRom(): ByteArray {
    val rom = ByteArray(0x10000)

    // Cabecera LoROM en 0x7FC0.
    val o = 0x7FC0
    val title = "DEMO ASSET ROM".padEnd(21, ' ')
    for (i in title.indices) rom[o + i] = title[i].code.toByte()
    rom[o + 21] = 0x20            // SlowROM/LoROM
    rom[o + 22] = 0x00            // ROM Only
    rom[o + 23] = 0x08            // tamaño ROM (256 KiB nominal)
    rom[o + 24] = 0x00            // sin SRAM
    rom[o + 25] = 0x08            // España (PAL)
    rom[o + 26] = 0x01            // Nintendo
    rom[o + 27] = 0x00
    val checksum = 0xABCD
    val complement = checksum.inv() and 0xFFFF
    rom[o + 28] = (complement and 0xFF).toByte()
    rom[o + 29] = ((complement shr 8) and 0xFF).toByte()
    rom[o + 30] = (checksum and 0xFF).toByte()
    rom[o + 31] = ((checksum shr 8) and 0xFF).toByte()

    // Paleta CGRAM de 16 colores en 0x100 (arcoíris de 15 bits BGR).
    for (i in 0 until 16) {
        val bgr = SnesDecoder.argbToBgr15(
            (0xFF shl 24) or
                (((i * 17) and 0xFF) shl 16) or
                (((255 - i * 17) and 0xFF) shl 8) or
                ((i * 8) and 0xFF)
        )
        rom[0x100 + 2 * i] = (bgr and 0xFF).toByte()
        rom[0x100 + 2 * i + 1] = ((bgr shr 8) and 0xFF).toByte()
    }

    // 64 tiles 4bpp en 0x2000: cada tile con un degradado diagonal de índices.
    var p = 0x2000
    for (t in 0 until 64) {
        for (y in 0..7) {
            // Índice de color (0..15) por fila; se reparte entre los 4 planos.
            val planes = IntArray(4)
            for (x in 0..7) {
                val value = ((x + y + t) % 16)
                for (bit in 0..3) {
                    if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
                }
            }
            rom[p + 2 * y] = planes[0].toByte()
            rom[p + 2 * y + 1] = planes[1].toByte()
            rom[p + 16 + 2 * y] = planes[2].toByte()
            rom[p + 16 + 2 * y + 1] = planes[3].toByte()
        }
        p += 32
    }
    return rom
}

/**
 * Como [buildDemoRom] pero con gráficos COMPRIMIDOS en LC_LZ2 a partir de 0x1000
 * (tiles con zonas planas, coherentes). Sirve para demostrar y probar la
 * descompresión de punta a punta sin usar ninguna ROM con derechos de autor.
 */
private fun buildCompressedDemoRom(): ByteArray {
    val rom = buildDemoRom() // reutiliza cabecera y paleta CGRAM en 0x100

    // Genera 64 tiles 4bpp "gráficos" (fondo plano + una cruz de otro color).
    val gfx = ByteArray(32 * 64)
    var p = 0
    var tileNo = 0
    while (p + 32 <= gfx.size) {
        val base = tileNo % 16
        val mark = (base + 1) % 16
        for (y in 0..7) {
            val planes = IntArray(4)
            for (x in 0..7) {
                val value = if (x == 3 || y == 3) mark else base
                for (bit in 0..3) if ((value shr bit) and 1 == 1) planes[bit] = planes[bit] or (1 shl (7 - x))
            }
            gfx[p + 2 * y] = planes[0].toByte()
            gfx[p + 2 * y + 1] = planes[1].toByte()
            gfx[p + 16 + 2 * y] = planes[2].toByte()
            gfx[p + 16 + 2 * y + 1] = planes[3].toByte()
        }
        p += 32; tileNo++
    }

    val compressed = LcLz2.compress(gfx)
    compressed.copyInto(rom, destinationOffset = 0x1000)
    return rom
}

/**
 * Recorre la ROM probando a descomprimir en cada offset con los códecs conocidos
 * y devuelve los offsets cuya salida "parece un dibujo". Localiza bloques
 * comprimidos sin conocer las tablas de punteros internas del juego.
 */
private fun scanForCompressedGraphics(
    rom: ByteArray,
    format: SnesGraphicFormat,
    minScore: Double = 0.42,
    maxResults: Int = 24,
    sprites: Boolean = false,
): List<Triple<Int, Double, Int>> {
    // Los bloques comprimidos empiezan en cualquier byte (no están alineados), así
    // que se prueba byte a byte; tras un acierto se salta el bloque consumido para
    // no repetir casi-duplicados. Se acota la salida para que el barrido sea rápido.
    // Con sprites=true se puntúa por "hoja de personajes" (premia la transparencia
    // con figuras sólidas) en vez de por coherencia, para localizar sprites como
    // Mario que el barrido normal se salta.
    val hits = ArrayList<Triple<Int, Double, Int>>()
    val minBytes = format.bytesPerTile * 32
    val threshold = if (sprites) 0.55 else minScore
    var offset = 0
    while (offset < rom.size - 3) {
        var advanced = false
        for (codec in CompressionCodecs.all) {
            val res = runCatching { codec.decompress(rom, offset, 0x4000) }.getOrNull() ?: continue
            if (res.data.size >= minBytes) {
                val tiles = minOf(res.data.size / format.bytesPerTile, 32)
                val score = if (sprites) {
                    SnesGraphicsScanner.spriteFitness(res.data, 0, format, tiles)
                } else {
                    CompressionCodecs.graphicScore(res.data, format)
                }
                if (score >= threshold) {
                    hits.add(Triple(offset, score, res.data.size))
                    offset += maxOf(1, res.consumedBytes) // saltar el bloque encontrado
                    advanced = true
                    break
                }
            }
        }
        if (!advanced) offset++
    }
    return hits.sortedByDescending { it.second }.take(maxResults).sortedBy { it.first }
}

private fun printUsage() {
    println(
        """
        Extractor de assets de ROM de SNES
          --rom <ruta> --out <dir> [--offset 0x2000] --format 4bpp [--tiles N] [--columns 16]
          [--palette-offset 0x100] [--grayscale] [--sprite 2x2] [--name terreno]
          --grayscale                         (vista en escala de grises: ver la FORMA sin la paleta real)
          --sprite 2x2                        (agrupa bloques de tiles en sprites enteros: 2x2=16x16, 4x4=32x32)
          --rom <ruta> --format 4bpp --scan   (autodetecta offsets con gráficos)
          --scan --sprites                    (busca HOJAS DE SPRITES/personajes, no fondos)
          [--decompress auto|lc_lz2]          (descomprime el bloque antes de extraer)
          --demo-compressed <dir>             (ROM de prueba con gráficos LC_LZ2)
          --rom <ruta> --recipe               (modo fácil: vuelca los gráficos reales de un juego con receta)
          --rom smw.sfc --collision [--level 0x106]
                                              (MAPA DE COLISIÓN de un nivel de SMW: solidez por celda + máscara PNG)
          --rom smw.sfc --physics             (TABLAS DE FÍSICAS del jugador: acelerar, correr, saltar, caer, gravedad)
          --rom smw.sfc --sprite-behavior [--id 0x0C]
                                              (TWEAKERS de comportamiento de enemigos: hitbox y banderas por id)
          --rom smw.sfc --level-info [--level 0x106]
                                              (FICHA del nivel: tamaño, modo, música, paletas, tiempo)
          --rom smw.sfc --enemies             (ATLAS de enemigos del catálogo con sprite/color real → enemies.png)
          --rom smw.sfc --music [--song 1] [--seconds 30] [--bank overworld]
                                              (MÚSICA renderizada a .wav: pruébala sin la app + diagnóstico de mezcla)
          --rom smw.sfc --powerups            (estados de powerup de Mario: tamaño y habilidades)
          --rom smw.sfc --play-sim [--level 0x106] [--frames 180] [--powerup big|cape|fire] [--env ice|water]
                                              (SIMULA el nivel en el motor: colisión+físicas+inicio, con powerup/entorno)
        o bien:  --demo <dir>   (genera una ROM de prueba y la extrae)
        Si omites --offset, se autodetecta el mejor candidato de gráficos.
        """.trimIndent()
    )
}
