package com.rolebuilder.core.tools

import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.model.DefaultProjectFactory
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random

/**
 * Genera los assets del proyecto plantilla: tileset y hojas de sprites en
 * pixel-art programático, más los JSON (project/database/map) usando el
 * mismo modelo de datos del motor. Uso: AssetGeneratorKt <dirSalida>
 */

const val T = 16 // tamaño de tile en px

fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "default_project")
    val imagesDir = File(outDir, ProjectIo.IMAGES_DIR).also { it.mkdirs() }

    ImageIO.write(generateTileset(), "png", File(imagesDir, "tileset.png"))
    ImageIO.write(generateHero(), "png", File(imagesDir, "hero.png"))
    ImageIO.write(generateNpc(), "png", File(imagesDir, "npc.png"))
    ImageIO.write(generateSlime(), "png", File(imagesDir, "slime.png"))
    ImageIO.write(generateBat(), "png", File(imagesDir, "bat.png"))
    ImageIO.write(generateChest(), "png", File(imagesDir, "chest.png"))

    ProjectIo.saveProject(outDir, DefaultProjectFactory.defaultProject("Mi aventura"))
    ProjectIo.saveDatabase(outDir, DefaultProjectFactory.defaultDatabase())
    ProjectIo.saveMap(outDir, DefaultProjectFactory.starterMap())

    println("Assets generados en ${outDir.absolutePath}")
}

private fun image(w: Int, h: Int): BufferedImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

private fun BufferedImage.draw(block: Graphics2D.() -> Unit) {
    val g = createGraphics()
    g.block()
    g.dispose()
}

/** Rellena un tile con color base y motas aleatorias (semilla fija: salida determinista). */
private fun Graphics2D.speckledTile(x0: Int, y0: Int, base: Color, speck: Color, seed: Int, density: Int = 10) {
    color = base
    fillRect(x0, y0, T, T)
    val rnd = Random(seed)
    color = speck
    repeat(density) {
        fillRect(x0 + rnd.nextInt(T - 1), y0 + rnd.nextInt(T - 1), 1 + rnd.nextInt(2), 1)
    }
}

// ---------------------------------------------------------------- tileset ---

fun generateTileset(): BufferedImage {
    val img = image(8 * T, 8 * T)
    img.draw {
        fun cell(index: Int): Pair<Int, Int> = (index % 8) * T to (index / 8) * T

        val grass = Color(64, 132, 60)
        val grassDark = Color(48, 108, 48)
        val dirt = Color(150, 112, 70)
        val dirtDark = Color(126, 92, 56)
        val sand = Color(214, 190, 132)
        val water = Color(52, 96, 176)
        val waterLight = Color(96, 140, 210)
        val stone = Color(130, 130, 138)
        val stoneDark = Color(100, 100, 108)
        val wood = Color(160, 116, 66)
        val woodDark = Color(132, 94, 52)

        // 0 GRASS
        cell(0).let { (x, y) -> speckledTile(x, y, grass, grassDark, 1) }
        // 1 FLOWERS
        cell(1).let { (x, y) ->
            speckledTile(x, y, grass, grassDark, 2)
            val rnd = Random(7)
            repeat(4) {
                val fx = x + 2 + rnd.nextInt(T - 4); val fy = y + 2 + rnd.nextInt(T - 4)
                color = if (it % 2 == 0) Color(230, 90, 110) else Color(240, 220, 90)
                fillRect(fx, fy, 2, 2)
                color = Color.WHITE
                fillRect(fx, fy, 1, 1)
            }
        }
        // 2 DIRT
        cell(2).let { (x, y) -> speckledTile(x, y, dirt, dirtDark, 3) }
        // 3 SAND
        cell(3).let { (x, y) -> speckledTile(x, y, sand, Color(196, 170, 110), 4) }
        // 4 WATER
        cell(4).let { (x, y) ->
            color = water; fillRect(x, y, T, T)
            color = waterLight
            fillRect(x + 2, y + 3, 5, 1); fillRect(x + 9, y + 7, 5, 1); fillRect(x + 4, y + 12, 5, 1)
        }
        // 5 TREE (sobre hierba)
        cell(5).let { (x, y) ->
            speckledTile(x, y, grass, grassDark, 5)
            color = Color(96, 64, 32); fillRect(x + 6, y + 10, 4, 5) // tronco
            color = Color(34, 88, 38); fillOval(x + 1, y + 0, 14, 12) // copa
            color = Color(52, 116, 52); fillOval(x + 3, y + 1, 8, 7)
        }
        // 6 BUSH
        cell(6).let { (x, y) ->
            speckledTile(x, y, grass, grassDark, 6)
            color = Color(40, 100, 44); fillOval(x + 2, y + 5, 12, 10)
            color = Color(60, 128, 60); fillOval(x + 4, y + 6, 6, 5)
        }
        // 7 ROCK
        cell(7).let { (x, y) ->
            speckledTile(x, y, grass, grassDark, 8)
            color = stoneDark; fillOval(x + 2, y + 5, 12, 10)
            color = stone; fillOval(x + 3, y + 5, 9, 7)
        }
        // 8 WALL (ladrillo)
        cell(8).let { (x, y) ->
            color = stone; fillRect(x, y, T, T)
            color = stoneDark
            for (row in 0 until 4) {
                fillRect(x, y + row * 4 + 3, T, 1)
                val off = if (row % 2 == 0) 0 else 4
                fillRect(x + off, y + row * 4, 1, 3); fillRect(x + off + 8, y + row * 4, 1, 3)
            }
        }
        // 9 WOOD_FLOOR
        cell(9).let { (x, y) ->
            color = wood; fillRect(x, y, T, T)
            color = woodDark
            for (i in 0 until 4) fillRect(x, y + i * 4, T, 1)
            fillRect(x + 5, y + 1, 1, 3); fillRect(x + 11, y + 5, 1, 3); fillRect(x + 3, y + 9, 1, 3)
        }
        // 10 STONE_FLOOR
        cell(10).let { (x, y) ->
            speckledTile(x, y, Color(168, 168, 174), Color(140, 140, 148), 9, 6)
            color = Color(140, 140, 148)
            fillRect(x, y + 7, T, 1); fillRect(x + 7, y, 1, 7); fillRect(x + 3, y + 8, 1, 8)
        }
        // 11 BRIDGE
        cell(11).let { (x, y) ->
            color = water; fillRect(x, y, T, T)
            color = wood; fillRect(x, y + 1, T, 14)
            color = woodDark
            for (i in 0 until 5) fillRect(x + i * 4 + 1, y + 1, 1, 14)
            fillRect(x, y + 1, T, 1); fillRect(x, y + 14, T, 1)
        }
        // 12 DOOR_CLOSED
        cell(12).let { (x, y) ->
            color = stone; fillRect(x, y, T, T)
            color = woodDark; fillRect(x + 3, y + 2, 10, 14)
            color = wood; fillRect(x + 4, y + 3, 8, 12)
            color = Color(240, 210, 90); fillRect(x + 10, y + 9, 2, 2) // pomo
        }
        // 13 DOOR_OPEN
        cell(13).let { (x, y) ->
            color = stone; fillRect(x, y, T, T)
            color = Color(30, 26, 34); fillRect(x + 3, y + 2, 10, 14)
        }
        // 14 STAIRS
        cell(14).let { (x, y) ->
            color = stoneDark; fillRect(x, y, T, T)
            color = stone
            for (i in 0 until 4) fillRect(x + 1, y + i * 4 + 1, 14, 3)
        }
        // 15 ROOF
        cell(15).let { (x, y) ->
            color = Color(150, 62, 54); fillRect(x, y, T, T)
            color = Color(120, 48, 42)
            for (i in 0 until 4) fillRect(x, y + i * 4 + 3, T, 1)
            for (i in 0 until 2) fillRect(x + i * 8 + 3, y, 1, T)
        }
        // Resto: cuadros grises numerables para que el usuario vea huecos libres.
        for (i in 16 until 64) {
            cell(i).let { (x, y) ->
                color = if ((i / 8 + i) % 2 == 0) Color(58, 58, 66) else Color(66, 66, 74)
                fillRect(x, y, T, T)
            }
        }
    }
    return img
}

// ------------------------------------------------------------- characters ---

/**
 * Hoja de personaje estilo RPG Maker: 3 columnas (animación) x 4 filas
 * (mirando abajo, izquierda, derecha, arriba). Cada frame 16x16.
 */
private fun characterSheet(drawFrame: Graphics2D.(x0: Int, y0: Int, dir: Int, frame: Int) -> Unit): BufferedImage {
    val img = image(3 * T, 4 * T)
    img.draw {
        for (dir in 0 until 4) for (frame in 0 until 3) {
            drawFrame(frame * T, dir * T, dir, frame)
        }
    }
    return img
}

private const val DIR_DOWN = 0
private const val DIR_LEFT = 1
private const val DIR_RIGHT = 2
private const val DIR_UP = 3

/** Humanoide sencillo: pelo/casco, cara, túnica y pies que alternan al andar. */
private fun humanoid(hair: Color, skin: Color, tunic: Color, tunicDark: Color): BufferedImage =
    characterSheet { x0, y0, dir, frame ->
        val step = frame - 1 // -1, 0, 1: desplazamiento de pies

        // pies
        color = Color(60, 44, 30)
        when (dir) {
            DIR_LEFT, DIR_RIGHT -> {
                fillRect(x0 + 5 + step, y0 + 14, 3, 2)
                fillRect(x0 + 9 - step, y0 + 14, 3, 2)
            }
            else -> {
                fillRect(x0 + 4, y0 + 14 + (if (step == -1) -1 else 0), 3, 2)
                fillRect(x0 + 9, y0 + 14 + (if (step == 1) -1 else 0), 3, 2)
            }
        }
        // cuerpo
        color = tunic
        fillRect(x0 + 4, y0 + 8, 8, 6)
        color = tunicDark
        fillRect(x0 + 4, y0 + 12, 8, 2)
        // brazos
        color = if (dir == DIR_UP) tunicDark else skin
        fillRect(x0 + 3, y0 + 9, 1, 3)
        fillRect(x0 + 12, y0 + 9, 1, 3)
        // cabeza
        color = skin
        fillRect(x0 + 4, y0 + 2, 8, 7)
        // pelo según dirección
        color = hair
        fillRect(x0 + 3, y0 + 1, 10, 3)
        when (dir) {
            DIR_DOWN -> { fillRect(x0 + 3, y0 + 3, 2, 3); fillRect(x0 + 11, y0 + 3, 2, 3) }
            DIR_UP -> fillRect(x0 + 3, y0 + 3, 10, 5)
            DIR_LEFT -> fillRect(x0 + 9, y0 + 3, 4, 5)
            DIR_RIGHT -> fillRect(x0 + 3, y0 + 3, 4, 5)
        }
        // ojos
        if (dir != DIR_UP) {
            color = Color(24, 24, 40)
            when (dir) {
                DIR_DOWN -> { fillRect(x0 + 6, y0 + 5, 1, 2); fillRect(x0 + 9, y0 + 5, 1, 2) }
                DIR_LEFT -> fillRect(x0 + 5, y0 + 5, 1, 2)
                DIR_RIGHT -> fillRect(x0 + 10, y0 + 5, 1, 2)
            }
        }
    }

fun generateHero(): BufferedImage =
    humanoid(hair = Color(88, 56, 28), skin = Color(240, 200, 160), tunic = Color(52, 100, 180), tunicDark = Color(40, 76, 140))

fun generateNpc(): BufferedImage =
    humanoid(hair = Color(120, 120, 126), skin = Color(236, 192, 152), tunic = Color(120, 150, 60), tunicDark = Color(94, 118, 46))

fun generateSlime(): BufferedImage =
    characterSheet { x0, y0, _, frame ->
        val squash = if (frame == 1) 1 else 0 // frame central más aplastado
        color = Color(70, 170, 90)
        fillOval(x0 + 2 - squash, y0 + 5 + squash, 12 + 2 * squash, 10 - squash)
        color = Color(110, 210, 130)
        fillOval(x0 + 4, y0 + 7 + squash, 5, 3)
        color = Color(20, 40, 26)
        fillRect(x0 + 5, y0 + 9 + squash, 2, 2)
        fillRect(x0 + 9, y0 + 9 + squash, 2, 2)
    }

fun generateBat(): BufferedImage =
    characterSheet { x0, y0, _, frame ->
        val up = frame == 1 // alas arriba en el frame central
        color = Color(84, 60, 130)
        if (up) {
            fillRect(x0 + 1, y0 + 4, 5, 4); fillRect(x0 + 10, y0 + 4, 5, 4)
        } else {
            fillRect(x0 + 1, y0 + 8, 5, 4); fillRect(x0 + 10, y0 + 8, 5, 4)
        }
        color = Color(60, 42, 96)
        fillOval(x0 + 5, y0 + 5, 6, 7)
        color = Color(230, 70, 70)
        fillRect(x0 + 6, y0 + 7, 1, 1); fillRect(x0 + 9, y0 + 7, 1, 1)
    }

/** Cofre: fila "abajo" = cerrado, fila "arriba" = abierto (se elige con SpriteRef.direction). */
fun generateChest(): BufferedImage =
    characterSheet { x0, y0, dir, _ ->
        val open = dir == DIR_UP
        val wood = Color(146, 96, 46)
        val woodDark = Color(112, 72, 34)
        val metal = Color(220, 190, 90)
        color = woodDark
        fillRect(x0 + 2, y0 + 6, 12, 8)
        color = wood
        fillRect(x0 + 3, y0 + 7, 10, 6)
        if (open) {
            color = Color(30, 24, 20)
            fillRect(x0 + 3, y0 + 7, 10, 3)
            color = woodDark
            fillRect(x0 + 2, y0 + 3, 12, 3) // tapa levantada
        } else {
            color = woodDark
            fillRect(x0 + 2, y0 + 4, 12, 3) // tapa
            color = metal
            fillRect(x0 + 7, y0 + 6, 2, 4) // cierre
        }
    }
