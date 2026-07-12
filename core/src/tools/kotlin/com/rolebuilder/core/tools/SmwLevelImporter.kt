package com.rolebuilder.core.tools

import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.GameMode
import com.rolebuilder.core.model.ParallaxLayer
import com.rolebuilder.core.model.PlatformEnemyMark
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.snes.ArgbImage
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * IMPORTA un nivel real de Super Mario World como PROYECTO editable de Role Builder
 * (Platform Builder). Toma la data ya extraída de la ROM —el tilemap Map16 reconstruido,
 * la colisión REAL por casilla, los enemigos con su posición y las teselas animadas— y la
 * escribe como un proyecto jugable: un tileset (atlas de los bloques distintos del nivel con
 * su solidez de plataformas), un mapa con esos tiles + los enemigos SMW colocados, y un
 * project.json en modo PLATFORMER cuyo inicio es la entrada real del nivel.
 *
 * No es una imagen troceada: el mapa resultante se juega con el tacto de plataformas del
 * motor (bordes de un sentido, cuestas, pinchos) porque conserva `Tileset.platformSolidity`.
 *
 * Uso:  gradle :core:importSmwLevel --args="--rom smw.sfc --level 0x105 [--out proyecto]"
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
    val level = (opts["level"] ?: "0x105").removePrefix("0x").toInt(16)
    val header = SnesDecoder.parseHeader(rom)
    val delta = header.headerOffset - 0x7FC0

    // maxCols alto: importa el nivel COMPLETO (hasta 32 pantallas), incluida la meta del final.
    val map = SnesGameRecipes.extractSmwLevelAsMap(rom, header, level, maxCols = 512) ?: run {
        System.err.println("El nivel ${level.toString(16)} no es reconstruible como mapa"); return
    }
    val levelName = SmwLevelNames.nameOf(rom, delta, level)
    val hx = level.toString(16).uppercase()
    val title = levelName ?: "Nivel $hx"

    val outDir = File(opts["out"] ?: "proyecto_smw_$hx").also { it.mkdirs() }
    val imagesDir = File(outDir, ProjectIo.IMAGES_DIR).also { it.mkdirs() }

    // Atlas del nivel (bloques Map16 distintos, 16×16) → PNG del tileset.
    val atlasName = "level_$hx.png"
    ImageIO.write(toBufferedImage(map.atlas), "png", File(imagesDir, atlasName))

    // NADA de assets del ARPG (hero/slime/…): un proyecto de Platform Builder solo lleva lo
    // que viene del import (tileset + fondo del ROM). El jugador (Mario) y los enemigos los
    // dibuja el renderer del platformer desde sus propios sprites SMW empaquetados en la app.

    // Fondo (Layer 2) REAL como capa de parallax detrás de la escena (si el nivel tiene y
    // decodifica). Se tila en mosaico y acompaña a la cámara a media velocidad (profundidad).
    val bgLayers = ArrayList<ParallaxLayer>()
    val bg = SnesGameRecipes.smwBackgroundImage(rom, header, level)
    if (bg != null) {
        val bgName = "bg_$hx.png"
        ImageIO.write(toBufferedImage(bg), "png", File(imagesDir, bgName))
        bgLayers.add(ParallaxLayer(image = bgName, factor = 0.5f, above = false, alpha = 1f))
    }

    // Tileset con la COLISIÓN REAL por casilla (platformSolidity) y las animaciones del nivel.
    val tileset = Tileset(
        id = 1,
        name = "SMW $title",
        image = atlasName,
        tileSize = 16,
        columns = map.columns,
        rows = map.rows,
        passable = map.passable,
        animations = map.animations,
        platformSolidity = map.solidity,
        platformBehavior = map.behavior,
    )

    // Mapa: capa 0 = tiles del nivel, capa 1 vacía; enemigos SMW como marcas de plataformas.
    val gameMap = GameMap(
        id = 1,
        name = title,
        width = map.mapWidth,
        height = map.mapHeight,
        tilesetId = 1,
        layers = listOf(map.tiles, List(map.mapWidth * map.mapHeight) { EMPTY_TILE }),
        platformEnemies = map.enemies.map { PlatformEnemyMark(it.first, it.second, it.third) },
        parallaxLayers = bgLayers,
    )

    // Inicio = entrada real del nivel (en casillas), recortada al mapa.
    val start = runCatching { SmwLevelStartReader.read(rom, header, level) }.getOrNull()
    val startX = (start?.startTileX ?: 2).coerceIn(0, map.mapWidth - 1)
    val startY = (start?.startTileY ?: 0).coerceIn(0, map.mapHeight - 1)

    val project = Project(
        name = "SMW · $title",
        mode = GameMode.PLATFORMER,
        startMapId = 1,
        startX = startX,
        startY = startY,
        mapIds = listOf(1),
    )
    // Base de datos MÍNIMA: solo el tileset del nivel. Sin actores/enemigos/items del ARPG —
    // el platformer no los usa. (Database.tileset(map.tilesetId) es lo único que lee la app.)
    val database = Database(tilesets = listOf(tileset))

    ProjectIo.saveProject(outDir, project)
    ProjectIo.saveDatabase(outDir, database)
    ProjectIo.saveMap(outDir, gameMap)

    // Cuenta celdas coleccionables/meta/premio (tile del mapa cuyo comportamiento no es NONE).
    var coinCells = 0; var goalCells = 0; var prizeCells = 0
    for (t in map.tiles) if (t >= 0) when (map.behavior.getOrNull(t) ?: 0) {
        1 -> coinCells++; 2 -> goalCells++; 3 -> prizeCells++
    }

    println("Proyecto: ${outDir.absolutePath}")
    println("Nivel $hx \"$title\": ${map.mapWidth}×${map.mapHeight} casillas · " +
        "${map.enemies.size} enemigos · $coinCells monedas · $prizeCells ? bloques · " +
        "meta ${if (goalCells > 0) "sí" else "no"} · " +
        "fondo ${if (bgLayers.isEmpty()) "no" else "sí (Layer 2)"} · " +
        "inicio ($startX,$startY) · modo PLATFORMER")
}

private fun toBufferedImage(img: ArgbImage): BufferedImage {
    val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    out.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
    return out
}
