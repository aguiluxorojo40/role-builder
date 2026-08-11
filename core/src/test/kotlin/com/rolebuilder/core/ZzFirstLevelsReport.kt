package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwBlockBehavior
import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwLayer1
import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import com.rolebuilder.core.snes.WarpTile
import java.io.File
import kotlin.test.Test

/**
 * SEGUIMIENTO del objetivo "100% de los primeros niveles": qué está y qué falta,
 * pieza por pieza. Se salta sola sin `SMW_ROM`, así que no afecta a CI.
 *
 * Antes marcaba cuatro casillas (Layer 1, colisión, mapa importable, gráficos de
 * sprites) y por eso daba "100%" en niveles a los que les faltaban cosas. Ahora mide
 * también lo que un nivel TIENE que tener para poder jugarse entero:
 *
 *  - que el mapa exportado no se quede CORTO (el tope de 256 columnas de
 *    [SnesGameRecipes.extractSmwLevelAsMap] recorta los niveles de 20-21 pantallas);
 *  - que la META caiga DENTRO del mapa exportado;
 *  - cuántos bloques INTERACTIVOS del nivel sabemos clasificar, contados por CASILLA
 *    (no por tesela del atlas, que es lo que engañaba);
 *  - que las bocas de warp del nivel se resuelvan a un destino jugable.
 *
 * Ojo con dos falsos huecos que ya están resueltos y NO hay que contar:
 * - Los sprites de META (0x7B cinta, 0x4A esfera, 0x0E cerradura) no son enemigos:
 *   el motor los siembra como ítem de meta ([SmwLevelGoal.GOAL_SPRITES]).
 * - Los ids con rutina de dibujo propia sí tienen gráfico, aunque no estén en el
 *   catálogo curado.
 */
class ZzFirstLevelsReport {

    /**
     * Los CINCO PRIMEROS de verdad, leídos de la tabla de nombres de la ROM: la casa de
     * Yoshi es 0x104 (0x107 es VANILLA GHOST HOUSE), y el orden es el de partida.
     */
    private val primeros = listOf(
        0x104 to "YOSHI'S HOUSE",
        0x105 to "YOSHI'S ISLAND 1",
        0x106 to "YOSHI'S ISLAND 2",
        0x103 to "YOSHI'S ISLAND 3",
        0x102 to "YOSHI'S ISLAND 4",
    )

    private companion object {
        /** Bytes bajos que el juego trata como MONEDA (`$00:F309`: 0x2A con P azul, 0x2B, 0x2C). */
        val MONEDA = 0x2A..0x2C

        /** Mitades de la MONEDA DRAGÓN (`$00:F309`, `CPY #$2D`). */
        val DRAGON = 0x2D..0x2E

        /** Bloques `?` golpeables desde abajo en el plano de block code (`$00:EB77`). */
        val PREMIO = 0x21..0x24

        /** Bloques GOLPEABLES del plano de terreno (`CheckIfBlockWasHit`, $00:F160: lo-0x11 < 0x1D). */
        val GOLPEABLE = 0x11..0x2D

        /** Cinta del PUNTO INTERMEDIO (`$00:F2C9`, `j == 56`). */
        const val MIDWAY = 0x38

        /** LUNA 3-UP (`$00:F309`, `j != 110`). */
        const val LUNA = 0x6E

        /** Los cuatro puntos de control del 1-UP escondido (`$00:F28C`, `j - 111 < 4`). */
        val CHECKPOINT_1UP = 0x6F..0x72

        /** Bloques de interruptor (verde 0x6A / amarillo 0x6B) en su silueta punteada. */
        val INTERRUPTOR = 0x6A..0x6B

        /** Boca de tubería VERTICAL en el plano de TERRENO (`$00:F3E9`). */
        val TUBERIA_V = 0x37..0x38

        /** Boca de tubería HORIZONTAL en el plano de TERRENO (`$00:F3C4`). */
        const val TUBERIA_H = 0x3F

        const val AIRE = 0x25
    }

    @Test
    fun `cuanto falta para el 100 por cien de los primeros niveles`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val faltan = sortedMapOf<Int, MutableList<Int>>()

        for ((lv, _) in primeros) {
            println("\n=== %03X — %s ===".format(lv, SmwLevelNames.nameOf(rom, delta, lv)?.trim()))
            val tm = SmwLayer1.parse(rom, delta, lv)
            println("  Layer 1        : %s (%d objetos)".format(
                if (tm != null && tm.unknownObjects == 0) "✓ 100%" else "✗ ${tm?.unknownIds}", tm?.totalObjects ?: -1))
            println("  Colisión       : %s".format(
                if (runCatching { SnesGameRecipes.smwLevelCollision(rom, header, lv) }.getOrNull() != null) "✓" else "✗"))
            val mapa = runCatching { SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) }.getOrNull()
            println("  Mapa importable: %s".format(if (mapa != null) "✓" else "✗"))
            anchoDelMapa(tm, mapa)
            meta(rom, delta, lv, mapa)
            if (tm != null) interactivos(tm)
            warps(rom, header, lv, tm)
            sprites(rom, header, lv, faltan)
        }

        println("\n=== SPRITES SIN GRÁFICO EN LOS CINCO PRIMEROS: ${faltan.size} ids ===")
        faltan.forEach { (id, lvs) ->
            println("  %02X %-28s en %s".format(id, SmwSpriteNames.nameOf(id),
                lvs.joinToString(",") { "%03X".format(it) }))
        }
    }

    /** El nivel entero tiene que caber en el mapa exportado: el tope de 256 columnas recorta. */
    private fun anchoDelMapa(tm: SmwLayer1.SmwLevelTilemap?, mapa: SnesGameRecipes.SmwLevelMap?) {
        val real = tm?.trimmedSize()?.first
        val exportado = mapa?.mapWidth
        val ok = real != null && exportado != null && exportado >= real
        println("  Ancho completo : %s (nivel %s casillas, mapa exportado %s)".format(
            if (ok) "✓" else "✗ SE PIERDEN ${(real ?: 0) - (exportado ?: 0)} COLUMNAS", real, exportado))
    }

    /** La meta tiene que existir y caer DENTRO del mapa exportado, o el nivel no se acaba. */
    private fun meta(rom: ByteArray, delta: Int, lv: Int, mapa: SnesGameRecipes.SmwLevelMap?) {
        val celdas = runCatching { SmwLevelGoal.goalCellsWithKind(rom, delta, lv) }.getOrNull().orEmpty()
        val x = celdas.firstOrNull()?.xTile
        val dentro = x != null && mapa != null && x < mapa.mapWidth
        val estado = when {
            celdas.isEmpty() -> "✗ SIN META en este nivel (la meta está en un sub-nivel)"
            dentro -> "✓ en x=$x"
            else -> "✗ en x=$x, FUERA del mapa exportado"
        }
        println("  Meta           : %s %s".format(estado, celdas.map { it.kind }.toSet()))
    }

    /** Bloques interactivos POR CASILLA y cuántos de ellos sabe clasificar el motor. */
    private fun interactivos(tm: SmwLayer1.SmwLevelTilemap) {
        val cuenta = HashMap<String, Int>()
        var clasificados = 0
        var total = 0
        for (y in 0 until tm.rows) {
            for (x in 0 until tm.cols) {
                val b = tm.block(x, y)
                val tipo = if (b <= 0 || b == AIRE) null else tipoInteractivo(b)
                if (tipo != null) {
                    cuenta[tipo] = (cuenta[tipo] ?: 0) + 1
                    total++
                    if (SmwBlockBehavior.classify(b) != SmwBlockAction.NONE) clasificados++
                }
            }
        }
        println("  Interactivos   : %d casillas, el motor entiende %d (%d%%)".format(
            total, clasificados, if (total == 0) 100 else clasificados * 100 / total))
        println("                   " + cuenta.toSortedMap())
    }

    /** Qué es un bloque, con el criterio del juego: el byte BAJO manda, el ALTO dice si es terreno. */
    private fun tipoInteractivo(block: Int): String? {
        val lo = block and 0xFF
        val terreno = (block ushr 8) != 0
        val deBlockCode = tipoBlockCode(lo)
        return when {
            terreno && lo in GOLPEABLE -> "GOLPEABLE(terreno)"
            terreno && lo in TUBERIA_V -> "BOCA_TUBERIA_V"
            terreno && lo == TUBERIA_H -> "BOCA_TUBERIA_H"
            terreno -> null
            else -> deBlockCode
        }
    }

    private fun tipoBlockCode(lo: Int): String? = when {
        lo in MONEDA -> "MONEDA"
        lo in DRAGON -> "MONEDA_DRAGON"
        lo in PREMIO -> "PREMIO(?)"
        lo == MIDWAY -> "CINTA_MIDWAY"
        lo == LUNA -> "LUNA_3UP"
        lo in CHECKPOINT_1UP -> "CHECKPOINT_1UP"
        lo in INTERRUPTOR -> "BLOQUE_INTERRUPTOR"
        else -> null
    }

    /** Las bocas del nivel tienen que llevar a algún sitio: si no, el nivel se queda a medias. */
    private fun warps(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int, tm: SmwLayer1.SmwLevelTilemap?) {
        val resueltos = runCatching { SmwWarpTiles.levelWarps(rom, header, lv) }.getOrNull().orEmpty()
        val salidas = runCatching { SmwLevelExits.read(rom, header, lv) }.getOrNull()?.exits.orEmpty()
        var horizontales = 0
        if (tm != null) {
            for (y in 0 until tm.rows) {
                for (x in 0 until tm.cols) {
                    val b = tm.block(x, y)
                    if (b > 0 && SmwWarpTiles.pipeOrDoor(b) == WarpTile.PIPE_HORIZONTAL) horizontales++
                }
            }
        }
        println("  Warps          : %d salidas en la tabla, %d bocas resueltas, %d bocas HORIZONTALES sin soporte"
            .format(salidas.size, resueltos.size, horizontales))
        resueltos.take(4).forEach {
            println("                   (%d,%d) %s → %03X (%d,%d)".format(
                it.xTile, it.yTile, it.kind, it.destLevel, it.destXTile, it.destYTile))
        }
    }

    /** Sprites del nivel sin gráfico y sin nombre. */
    private fun sprites(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int, faltan: MutableMap<Int, MutableList<Int>>) {
        val ids = SnesGameRecipes.smwLevelEnemies(rom, header, lv).map { it.first }
        val porId = ids.groupingBy { it }.eachCount().toSortedMap()
        val sinGrafico = ArrayList<String>()
        val sinNombre = ArrayList<String>()
        for ((id, n) in porId) {
            if (SmwSpriteNames.nameOf(id).startsWith("Sprite 0x")) sinNombre.add("%02X×%d".format(id, n))
            if (!tieneGrafico(rom, header, lv, id)) {
                sinGrafico.add("%02X×%d %s".format(id, n, SmwSpriteNames.nameOf(id)))
                faltan.getOrPut(id) { mutableListOf() }.add(lv)
            }
        }
        println("  Sprites SIN gráfico (%d): %s".format(sinGrafico.size, sinGrafico.joinToString(", ")))
        println("  Sprites SIN nombre  (%d): %s".format(sinNombre.size, sinNombre.joinToString(", ")))
    }

    private fun tieneGrafico(rom: ByteArray, header: com.rolebuilder.core.snes.SnesHeader, lv: Int, id: Int): Boolean {
        if (id in SmwLevelGoal.GOAL_SPRITES) return true              // es meta, no enemigo
        if (SmwEnemyGraphics.isIntentionallyInvisible(id)) return true // no dibuja NADA en el juego
        if (SmwEnemyGraphics.handles(id)) return true                 // catálogo curado
        return runCatching {
            SmwEnemyGraphics.customEnemyImage(rom, header, lv, id) != null
        }.getOrDefault(false)
    }
}
