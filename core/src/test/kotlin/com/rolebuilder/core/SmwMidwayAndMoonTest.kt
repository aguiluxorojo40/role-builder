package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.BlockAction
import com.rolebuilder.core.engine.platformer.MOON_3UP_LIVES
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.engine.platformer.ProjectPlatformer
import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Los bloques que el motor CLASIFICABA pero no jugaba: la CINTA del punto intermedio, la
 * LUNA 3-UP y la SALIDA DE LADO de la casa de Yoshi.
 *
 * No es un detalle de contabilidad: la cinta está en YOSHI'S ISLAND 1, 2 y 3 (una casilla
 * en cada uno) y la luna en el 1; los tres se reconocían desde el clasificador y se caían
 * por el camino, unos porque el motor no tenía ningún valor para ellos y la cinta porque
 * ni siquiera se clasificaba.
 */
class SmwMidwayAndMoonTest {

    private val tuning = PlatformerTuning(
        jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
        maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
    )

    private val cols = 12
    private val rows = 10
    private val floorRow = 7

    /** Motor con suelo y UNA celda de acción [action] en (col,row). */
    private fun engine(
        action: BlockAction,
        col: Int,
        row: Int,
        startCol: Int = 2,
        sideExits: Boolean = false,
    ): PlatformerEngine {
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[floorRow][c] = SmwSolidity.SOLID
        val actions = IntArray(cols * rows)
        actions[row * cols + col] = action.ordinal
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> if (r in 0 until rows && c in 0 until cols) grid[r][c] else SmwSolidity.NONE },
            startPixelX = startCol * 16, startPixelY = (floorRow - 1) * 16,
            tuning = tuning,
            blockActions = actions,
            sideExits = sideExits,
        )
    }

    private fun PlatformerEngine.run(frames: Int) { repeat(frames) { tick() } }

    /** Camina a la derecha hasta [frames] o hasta que se cumpla [hasta]. */
    private fun PlatformerEngine.camina(frames: Int, hasta: () -> Boolean = { false }) {
        moveX = 1f
        repeat(frames) {
            if (hasta()) return
            tick()
        }
    }

    @Test
    fun `cruzar la cinta marca el punto intermedio y borra la cinta`() {
        // `RunPlayerBlockCode_00F2C9` ($00:F2C9, `j == 56`): quita la tesela, marca el punto
        // intermedio (`PlayerState00_SetMidpointFlag`, $00:CA2B) y suena.
        val e = engine(BlockAction.MIDWAY, col = 6, row = floorRow - 1)
        assertFalse(e.midwayReached, "aún no ha pasado por la cinta")
        e.camina(240) { e.midwayReached }

        assertTrue(e.midwayReached, "cruzar la cinta marca el punto de control")
        assertEquals(6, e.midwayCol, "recuerda la columna de la cinta")
        assertEquals(floorRow - 1, e.midwayRow, "recuerda la fila de la cinta")
        assertEquals(1, e.midwayEvents, "un evento para el SFX")
        assertEquals(
            BlockAction.NONE, e.blockActionAt(6, floorRow - 1),
            "la cinta desaparece al cruzarla, como en el juego",
        )
    }

    @Test
    fun `la cinta hace GRANDE al que va pequeño, como el juego`() {
        // `if (!player_current_power_up) player_current_power_up = 1` ($00:F2C9).
        val e = engine(BlockAction.MIDWAY, col = 6, row = floorRow - 1)
        assertFalse(e.player.big, "empieza pequeño")
        val powerupsAntes = e.powerupEvents
        e.camina(240) { e.midwayReached }

        assertTrue(e.player.big, "la cinta da la seta si vas pequeño")
        assertTrue(e.powerupEvents > powerupsAntes, "y suena a power-up")
    }

    @Test
    fun `la cinta se marca UNA vez aunque se cruce dos casillas`() {
        val e = engine(BlockAction.MIDWAY, col = 6, row = floorRow - 1)
        e.camina(240) { e.midwayReached }
        val col = e.midwayCol
        e.run(60) // sigue andando por encima
        assertEquals(col, e.midwayCol, "el punto de control no se mueve después")
    }

    @Test
    fun `la luna 3-UP se coge al tocarla y vale tres vidas`() {
        val e = engine(BlockAction.MOON_3UP, col = 6, row = floorRow - 1)
        assertEquals(0, e.moonEvents)
        e.camina(240) { e.moonEvents > 0 }

        assertEquals(1, e.moonEvents, "la luna se recoge")
        assertEquals(
            BlockAction.NONE, e.blockActionAt(6, floorRow - 1),
            "y desaparece del mapa",
        )
        assertEquals(3, MOON_3UP_LIVES, "cada luna son TRES vidas (tabla StuffToGive, \$02)")
    }

    @Test
    fun `la luna no se cuenta dos veces`() {
        val e = engine(BlockAction.MOON_3UP, col = 6, row = floorRow - 1)
        e.camina(240) { e.moonEvents > 0 }
        e.run(60)
        assertEquals(1, e.moonEvents, "ya no está: no vuelve a contar")
    }

    @Test
    fun `sin salida de lado el nivel tiene paredes y no se sale`() {
        val e = engine(BlockAction.NONE, col = 0, row = 0, startCol = cols - 2)
        e.camina(240)
        assertFalse(e.leftBySide, "un nivel normal no se abandona andando")
        assertTrue(e.player.x < cols * 16f, "la pared lateral lo frena")
    }

    @Test
    fun `con salida de lado, llegar al borde derecho abandona el nivel`() {
        // `HandlePlayerLevelColl_00E98C` ($00:E98C): con `flag_side_exits`, ni frena en el
        // borde ni cuenta como superado — `DisplayMessage_ExitToOverworldNoEvent` ($05:B160)
        // vuelve al mapa con `misc_exit_level_action = 0`.
        val e = engine(BlockAction.NONE, col = 0, row = 0, startCol = cols - 3, sideExits = true)
        e.camina(240) { e.leftBySide }

        assertTrue(e.leftBySide, "salirse por la derecha abandona el nivel")
        assertFalse(e.won, "y NO es ganar: no dispara el evento del nivel")
        assertFalse(e.player.dead, "ni morir")
    }

    @Test
    fun `con salida de lado tambien se sale por la izquierda`() {
        // La comparación del juego es de 16 bits (`player_on_screen_pos_x >= 0xFA` con
        // `uint16`), así que una posición NEGATIVA —salirse por la izquierda— también pasa.
        val e = engine(BlockAction.NONE, col = 0, row = 0, startCol = 1, sideExits = true)
        e.moveX = -1f
        repeat(240) {
            if (e.leftBySide) return@repeat
            e.tick()
        }
        assertTrue(e.leftBySide, "salirse por la izquierda también abandona el nivel")
    }

    @Test
    fun `el mapeo a acciones del motor no pierde ningun valor por el camino`() {
        // Es el sitio donde se caían: SmwBlockBehavior clasificaba la luna y nadie la
        // traducía, así que llegaba al motor como NONE. Todo lo que clasifique tiene que
        // sobrevivir a esta traducción.
        assertEquals(BlockAction.COIN.ordinal, ProjectPlatformer.engineAction(SmwBlockAction.COIN.ordinal))
        assertEquals(BlockAction.PRIZE.ordinal, ProjectPlatformer.engineAction(SmwBlockAction.QUESTION.ordinal))
        assertEquals(
            BlockAction.DRAGON_COIN.ordinal,
            ProjectPlatformer.engineAction(SmwBlockAction.DRAGON_COIN.ordinal),
        )
        assertEquals(BlockAction.MOON_3UP.ordinal, ProjectPlatformer.engineAction(SmwBlockAction.MOON_3UP.ordinal))
        assertEquals(BlockAction.MIDWAY.ordinal, ProjectPlatformer.engineAction(SmwBlockAction.MIDWAY_TAPE.ordinal))
        assertEquals(BlockAction.NONE.ordinal, ProjectPlatformer.engineAction(SmwBlockAction.NONE.ordinal))

        // Y NINGUNA acción de SMW puede quedarse sin traducir: si mañana se añade una,
        // este test la caza antes de que se pierda en silencio.
        val sinTraducir = SmwBlockAction.entries
            .filter { it != SmwBlockAction.NONE }
            .filter { ProjectPlatformer.engineAction(it.ordinal) == BlockAction.NONE.ordinal }
        assertEquals(emptyList(), sinTraducir, "acciones de SMW que el motor tiraría a la basura")
    }

    @Test
    fun `los ordinales de BlockAction NO se pueden reordenar`() {
        // Se guardan en la rejilla de acciones de los mapas importados.
        assertEquals(0, BlockAction.NONE.ordinal)
        assertEquals(1, BlockAction.COIN.ordinal)
        assertEquals(2, BlockAction.PRIZE.ordinal)
        assertEquals(3, BlockAction.DRAGON_COIN.ordinal)
        assertEquals(4, BlockAction.GRAB.ordinal)
        assertEquals(5, BlockAction.MOON_3UP.ordinal)
        assertEquals(6, BlockAction.MIDWAY.ordinal)
    }
}
