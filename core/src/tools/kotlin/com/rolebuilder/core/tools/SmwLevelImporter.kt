package com.rolebuilder.core.tools

import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.DefaultProjectFactory
import com.rolebuilder.core.model.EMPTY_TILE
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.GameMode
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

    val map = SnesGameRecipes.extractSmwLevelAsMap(rom, header, level) ?: run {
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

    // Sprites base de personaje/enemigo RPG para que el proyecto cargue completo en la app.
    copyDefaultSprites(imagesDir)

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
    // Base de datos por defecto pero con NUESTRO tileset (id 1) para que el mapa resuelva.
    val database = DefaultProjectFactory.defaultDatabase().copy(tilesets = listOf(tileset))

    ProjectIo.saveProject(outDir, project)
    ProjectIo.saveDatabase(outDir, database)
    ProjectIo.saveMap(outDir, gameMap)

    println("Proyecto: ${outDir.absolutePath}")
    println("Nivel $hx \"$title\": ${map.mapWidth}×${map.mapHeight} casillas · " +
        "${map.enemies.size} enemigos · ${map.animations.size} teselas animadas · " +
        "inicio ($startX,$startY) · modo PLATFORMER")
}

/** Copia los sprites sintéticos del proyecto por defecto (si están) para que el proyecto cargue. */
private fun copyDefaultSprites(imagesDir: File) {
    val src = File("app/src/main/assets/default_project/images")
    if (!src.isDirectory) return
    for (f in src.listFiles().orEmpty()) {
        if (f.extension == "png" && f.name != "tileset.png") {
            f.copyTo(File(imagesDir, f.name), overwrite = true)
        }
    }
}

private fun toBufferedImage(img: ArgbImage): BufferedImage {
    val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    out.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width)
    return out
}
