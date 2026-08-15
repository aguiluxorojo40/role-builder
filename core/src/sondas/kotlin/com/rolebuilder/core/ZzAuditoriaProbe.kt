package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwLevelStartReader
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * SONDA DESECHABLE de AUDITORÍA: la FICHA de varios niveles de una tacada (entrada,
 * midway, salidas, warps, sprites, meta, cabecera), para poder compararla con cómo son
 * los niveles de verdad sin lanzar una ejecución de Gradle por nivel.
 *
 * Lo que aporta sobre [ZzNivelProbe]:
 *  - recorre una LISTA de niveles (`SMW_LEVELS`);
 *  - busca las bocas de tubería con el criterio del JUEGO —byte bajo 0x37/0x38 en un
 *    bloque SÓLIDO (página ≥ 1), que es la rama por la que pasa `RunPlayerBlockCode`—,
 *    además de con [SmwWarpTiles.pipeOrDoor], para poder enseñar la diferencia;
 *  - imprime la posición de entrada principal y la del midway.
 *
 * Se salta sola sin `SMW_ROM`.
 */
class ZzAuditoriaProbe {

    private companion object {
        /** Bytes bajos de boca de tubería VERTICAL (`$00:F3E9`). */
        val PIPE_V = 0x37..0x38

        /** Byte bajo de boca de tubería HORIZONTAL (`$00:F3C4`). */
        const val PIPE_H = 0x3F

        /** Bytes bajos de PUERTA siempre-puerta (`$00:EB77`). */
        val PUERTA = intArrayOf(0x1F, 0x20)
    }

    @Test
    fun `ficha de auditoria de varios niveles`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val niveles = (System.getenv("SMW_LEVELS") ?: "104,105,106,103,102")
            .split(",").map { it.trim().toInt(16) }

        for (lv in niveles) {
            println("\n##### NIVEL %03X — %s #####".format(lv, SmwLevelNames.nameOf(rom, delta, lv)?.trim()))
            val info = SnesGameRecipes.smwLevelInfo(rom, header, lv)
            println("  cabecera: pantallas=%s modo=%s musica=%s tiempo=%s gfxSprites=%s tilesetFG=%s palFG=%s palBG=%s palSpr=%s"
                .format(info?.screens, info?.levelMode, info?.musicIndex, info?.startTime,
                    info?.spriteGfx, info?.fgTileset, info?.fgPalette, info?.bgPalette, info?.spritePalette))
            val start = SmwLevelStartReader.read(rom, header, lv)
            println("  entrada principal: casilla=(%s,%s) pantallaFgBg=%s"
                .format(start?.startTileX, start?.startTileY, start?.fgBgPositionSetting))

            val conn = runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull()
            println("  midway: %s".format(conn?.midway))
            println("  salidas de pantalla:")
            conn?.exits?.forEach { println("    $it") } ?: println("    (sin conectividad)")

            bocas(rom, header, lv)

            println("  warps que resuelve SmwWarpTiles.levelWarps:")
            val w = runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrNull().orEmpty()
            if (w.isEmpty()) println("    (ninguno)")
            w.forEach {
                println("    (%d,%d) %s abajo=%s → %03X (%d,%d)".format(
                    it.xTile, it.yTile, it.kind, it.enterDown, it.destLevel, it.destXTile, it.destYTile))
            }

            println("  sprites colocados:")
            SnesGameRecipes.smwLevelEnemySeeds(rom, header, lv).groupBy { it.id }.toSortedMap()
                .forEach { (id, l) ->
                    println("    %02X %-26s x%-3d %s".format(id, SmwSpriteNames.nameOf(id), l.size,
                        l.joinToString(" ") { "(${it.xTile},${it.yTile})" }))
                }
            println("  meta: %s".format(
                runCatching { SmwLevelGoal.goalCellsWithKind(rom, delta, lv) }.getOrNull()))
        }
    }

    /** Bocas de warp con el criterio del juego (sólido para tuberías, atravesable para puertas). */
    private fun bocas(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int) {
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val tm = SmwLayer1.parse(rom, delta, lv) ?: return
        val reales = ArrayList<String>()
        val falsas = HashMap<String, Int>()
        for (y in 0 until tm.rows) {
            for (x in 0 until tm.cols) {
                val b = tm.block(x, y)
                if (b <= 0) continue
                val lo = b and 0xFF
                val solido = (b ushr 8) != 0
                val tipo = when {
                    solido && lo in PIPE_V -> "TUBERIA_VERTICAL"
                    solido && lo == PIPE_H -> "TUBERIA_HORIZONTAL"
                    !solido && lo in PUERTA -> "PUERTA"
                    else -> null
                }
                if (tipo != null) reales.add("(%d,%d)%03X %s".format(x, y, b, tipo))
                val vieja = SmwWarpTiles.pipeOrDoor(b)
                if (vieja != null && tipo == null) falsas[vieja.name] = (falsas[vieja.name] ?: 0) + 1
            }
        }
        println("  bocas REALES (criterio del juego): " + (reales.takeIf { it.isNotEmpty() } ?: "(ninguna)"))
        println("  falsos positivos de pipeOrDoor(): " + (falsas.takeIf { it.isNotEmpty() } ?: "(ninguno)"))
    }
}
