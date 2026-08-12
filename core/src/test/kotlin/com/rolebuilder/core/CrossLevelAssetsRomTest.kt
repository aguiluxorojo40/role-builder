package com.rolebuilder.core

import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.PlatformLayers
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.model.TilesetMerge
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Assume
import org.junit.Test

/**
 * Construir un nivel con material de VARIOS niveles de la ROM, contra la ROM de verdad.
 *
 * Estas dos cosas eran las que fallaban en la app:
 *  1. el terreno de un nivel importado se guardaba en la capa 0 o en la 1 según el nivel
 *     tuviera fondo o no, así que las herramientas del editor pintaban en la capa
 *     equivocada (los 8 niveles escaparate de la ROM traen fondo: siempre invertidas);
 *  2. no había forma de usar las teselas de un nivel en otro, porque cada nivel trae su
 *     tileset y un mapa apunta a uno solo.
 *
 * Sin ROM (el repositorio no la lleva: es de Nintendo) el test se salta solo.
 */
class CrossLevelAssetsRomTest {

    /** Igual que la app: tileset + mapa de un nivel de la ROM. */
    private class NivelImportado(val tileset: Tileset, val map: GameMap, val atlas: IntArray)

    private fun importar(rom: ByteArray, level: Int, tsId: Int): NivelImportado? {
        val hdr = SnesDecoder.parseHeader(rom)
        val m = SnesGameRecipes.extractSmwLevelAsMap(rom, hdr, level) ?: return null
        val tileset = Tileset(
            id = tsId, name = "Nivel ${level.toString(16)}", image = "n$tsId.png",
            tileSize = 16, columns = m.columns, rows = m.rows,
            passable = m.passable, platformSolidity = m.solidity,
            platformSlopeShape = m.slopeShapes, animations = m.animations,
            platformBlockActions = m.blockActions,
        )
        val map = GameMap(
            id = tsId, name = "SMW $level", width = m.mapWidth, height = m.mapHeight,
            tilesetId = tsId,
            layers = PlatformLayers.layersOf(m.tiles, m.bgTiles, m.mapWidth, m.mapHeight),
        )
        return NivelImportado(tileset, map, m.atlas.pixels)
    }

    private fun solidas(map: GameMap, tileset: Tileset, layer: Int): Int =
        map.layers[layer].count { t ->
            t >= 0 && (tileset.platformSolidity.getOrNull(t) ?: 0) != 0
        }

    @Test
    fun `el terreno de un nivel importado vive en la capa de primer plano`() {
        val rom = findRom() ?: return skip()
        val n = assertNotNull(importar(rom, 0x105, 1), "el nivel 105 tiene que reconstruirse")

        assertTrue(
            solidas(n.map, n.tileset, PlatformLayers.FOREGROUND) > 0,
            "el terreno jugable va en la Capa 1",
        )
        assertEquals(
            0, solidas(n.map, n.tileset, PlatformLayers.BACKGROUND),
            "el fondo (Capa 2) es decorado: no lleva colisión",
        )
        assertTrue(
            PlatformLayers.isCanonical(n.map, n.tileset),
            "un nivel recién importado ya está en el orden canónico",
        )
    }

    @Test
    fun `traerse teselas de otro nivel conserva su dibujo y su colision`() {
        val rom = findRom() ?: return skip()
        val destino = assertNotNull(importar(rom, 0x105, 1))
        val origen = assertNotNull(importar(rom, 0x101, 2)) // el nivel del castillo

        // Diez teselas del otro nivel, de las que de verdad se usan en su mapa.
        val usadas = origen.map.layers[PlatformLayers.FOREGROUND]
            .filter { it >= 0 }.distinct().take(10)
        assertTrue(usadas.size >= 5, "el nivel de origen tiene teselas que traerse")

        val antes = destino.tileset.tileCount
        val r = TilesetMerge.append(destino.tileset, destino.atlas, origen.tileset, origen.atlas, usadas)

        assertEquals(usadas.size, r.added.size, "llegan todas las pedidas")
        assertTrue(r.tileset.tileCount >= antes, "el atlas crece, no encoge")
        assertEquals(destino.tileset.columns, r.tileset.columns, "las columnas no cambian")

        usadas.forEachIndexed { i, srcTile ->
            val destTile = r.added[i]
            assertEquals(
                origen.tileset.platformSolidity[srcTile],
                r.tileset.platformSolidity[destTile],
                "la colisión viaja con la tesela",
            )
            assertEquals(
                origen.tileset.platformBlockActions[srcTile],
                r.tileset.platformBlockActions[destTile],
                "la acción de bloque viaja con la tesela",
            )
            // Y el dibujo: la primera fila de píxeles de la celda tiene que ser la misma.
            val so = (srcTile / origen.tileset.columns) * 16 * (origen.tileset.columns * 16) +
                (srcTile % origen.tileset.columns) * 16
            val dof = (destTile / r.tileset.columns) * 16 * r.width + (destTile % r.tileset.columns) * 16
            for (x in 0 until 16) {
                assertEquals(origen.atlas[so + x], r.pixels[dof + x], "píxel $x de la tesela traída")
            }
        }
    }

    @Test
    fun `las teselas que ya usaba el nivel no se mueven de sitio`() {
        val rom = findRom() ?: return skip()
        val destino = assertNotNull(importar(rom, 0x105, 1))
        val origen = assertNotNull(importar(rom, 0x101, 2))
        val usadas = origen.map.layers[PlatformLayers.FOREGROUND].filter { it >= 0 }.distinct().take(6)

        val r = TilesetMerge.append(destino.tileset, destino.atlas, origen.tileset, origen.atlas, usadas)

        // El mapa del nivel sigue apuntando a lo mismo: mismos índices, mismos píxeles.
        for (t in 0 until destino.tileset.tileCount) {
            assertEquals(
                destino.tileset.platformSolidity[t], r.tileset.platformSolidity[t],
                "la tesela $t del nivel conserva su colisión",
            )
        }
        val anchoViejo = destino.tileset.columns * 16
        for (i in 0 until anchoViejo) {
            assertEquals(destino.atlas[i], r.pixels[i], "la primera fila del atlas viejo, intacta")
        }
    }

    @Test
    fun `se puede traer de un nivel y despues de otro, y sobreviven los dos`() {
        val rom = findRom() ?: return skip()
        val destino = assertNotNull(importar(rom, 0x105, 1))
        val castillo = assertNotNull(importar(rom, 0x101, 2))
        val otro = assertNotNull(importar(rom, 0x106, 3))

        val delCastillo = castillo.map.layers[PlatformLayers.FOREGROUND].filter { it >= 0 }.distinct().take(5)
        val delOtro = otro.map.layers[PlatformLayers.FOREGROUND].filter { it >= 0 }.distinct().take(5)

        // Primer viaje: del castillo.
        val paso1 = TilesetMerge.append(
            destino.tileset, destino.atlas, castillo.tileset, castillo.atlas, delCastillo,
        )
        // Segundo viaje: de otro nivel distinto, SOBRE el resultado del primero.
        val paso2 = TilesetMerge.append(
            paso1.tileset, paso1.pixels, otro.tileset, otro.atlas, delOtro,
        )

        // Lo traído en el primer viaje sigue donde estaba y con su colisión.
        paso1.added.forEachIndexed { i, t ->
            assertEquals(
                castillo.tileset.platformSolidity[delCastillo[i]],
                paso2.tileset.platformSolidity[t],
                "la tesela del primer nivel traído no se toca en el segundo viaje",
            )
        }
        // Y lo del segundo viaje llega entero.
        assertEquals(delOtro.size, paso2.added.size)
        paso2.added.forEachIndexed { i, t ->
            assertEquals(
                otro.tileset.platformSolidity[delOtro[i]],
                paso2.tileset.platformSolidity[t],
                "la colisión del segundo nivel también viaja",
            )
        }
        assertTrue(
            paso2.tileset.tileCount >= paso1.tileset.tileCount,
            "el atlas acumula los dos viajes",
        )
    }

    private fun skip() = Assume.assumeTrue("sin ROM de SMW: se salta", false)

    private fun findRom(): ByteArray? {
        val candidates = listOf(System.getenv("SMW_ROM"), "/home/user/role-builder/smw.sfc")
        for (p in candidates) if (p != null && File(p).isFile) return File(p).readBytes()
        return null
    }
}
