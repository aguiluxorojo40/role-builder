package com.rolebuilder.core

import com.rolebuilder.core.engine.platformer.BlockAction
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.engine.platformer.PowerupKind
import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwBlockBehavior
import com.rolebuilder.core.snes.SmwSolidity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Los CUATRO PUNTOS DE CONTROL del 1-UP escondido (bytes bajos 0x6F..0x72 del plano de
 * block code): invisibles, hay que pisarlos EN ORDEN y el cuarto suelta la seta verde.
 *
 * Port de `RunPlayerBlockCode_00F28C` ($00:F28C) + `TriggerHidden1up` ($03:C2D9). Los
 * cuatro están en YOSHI'S ISLAND 4 y hasta ahora eran cuatro celdas inertes.
 */
class Smw1UpCheckpointsTest {

    private val tuning = PlatformerTuning(
        jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
        maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
    )

    private val cols = 16
    private val rows = 10
    private val floorRow = 7

    /** Columnas donde se siembran los puntos, separadas para no pisar dos a la vez. */
    private val columnas = intArrayOf(3, 6, 9, 12)

    /** Motor con los cuatro puntos de control repartidos según [orden] (índice por columna). */
    private fun engine(orden: IntArray): PlatformerEngine {
        val grid = Array(rows) { Array(cols) { SmwSolidity.NONE } }
        for (c in 0 until cols) grid[floorRow][c] = SmwSolidity.SOLID
        val actions = IntArray(cols * rows)
        val puntos = arrayOf(
            BlockAction.CHECKPOINT_1UP_1, BlockAction.CHECKPOINT_1UP_2,
            BlockAction.CHECKPOINT_1UP_3, BlockAction.CHECKPOINT_1UP_4,
        )
        for (i in columnas.indices) {
            actions[(floorRow - 1) * cols + columnas[i]] = puntos[orden[i]].ordinal
        }
        return PlatformerEngine(
            cols, rows,
            solidityAt = { c, r -> if (r in 0 until rows && c in 0 until cols) grid[r][c] else SmwSolidity.NONE },
            startPixelX = 16, startPixelY = (floorRow - 1) * 16,
            tuning = tuning,
            blockActions = actions,
        )
    }

    private fun PlatformerEngine.caminaDerecha(frames: Int) {
        moveX = 1f
        repeat(frames) { tick() }
    }

    @Test
    fun `pisar los cuatro EN ORDEN suelta la seta verde de una vida`() {
        val e = engine(intArrayOf(0, 1, 2, 3))
        assertEquals(0, e.checkpoints1up, "empieza sin ninguno")
        e.caminaDerecha(300)

        assertEquals(4, e.checkpoints1up, "los cuatro, en orden")
        assertEquals(1, e.hidden1upEvents, "el cuarto dispara el 1-UP escondido")
        // `TriggerHidden1up` ($03:C2D9) suelta el sprite 0x78, la seta VERDE.
        assertTrue(
            e.items.any { it.kind == PowerupKind.ONE_UP } || e.oneUpEvents > 0,
            "sale una seta verde (o ya se ha recogido por el camino)",
        )
    }

    @Test
    fun `recoger la seta verde da una vida y NO hace grande a Mario`() {
        val e = engine(intArrayOf(0, 1, 2, 3))
        e.caminaDerecha(300)
        // Se queda quieto encima hasta cogerla.
        e.moveX = 0f
        repeat(240) { e.tick() }

        assertTrue(e.oneUpEvents > 0, "la seta verde da una vida")
        assertTrue(!e.player.big, "y no cambia el tamaño: GiveMario1Up no toca el power-up")
    }

    @Test
    fun `pisarlos DESORDENADOS no da nada`() {
        // Recorrido 4 → 3 → 2 → 1. Cada uno que no es el que tocaba reinicia la cuenta
        // (`v1 != counter` y `v1 + 1 != counter` → `counter = 0`, $00:F28C), así que no
        // hay premio. Y termina en UNO, no en cero, porque el último que pisa es el
        // PRIMERO de la secuencia: eso sí es un comienzo válido. La cuenta no se "castiga",
        // simplemente vuelve a empezar donde toque.
        val e = engine(intArrayOf(3, 2, 1, 0))
        e.caminaDerecha(300)

        assertEquals(0, e.hidden1upEvents, "desordenados no hay premio")
        assertEquals(1, e.checkpoints1up, "acaba con el primero de la secuencia recién pisado")
    }

    @Test
    fun `saltarse uno en medio corta la secuencia`() {
        // 1 → 2 → 4 → 3: al llegar al CUARTO la cuenta va por 2, y `v1 = 3` no es el que
        // tocaba ni el recién cogido, así que reinicia. Sin la regla de orden, esto daría
        // el 1-UP igual y sería un premio regalado.
        val e = engine(intArrayOf(0, 1, 3, 2))
        e.caminaDerecha(300)

        assertEquals(0, e.hidden1upEvents, "saltarse el tercero no da premio")
    }

    @Test
    fun `quedarse encima del mismo punto no rompe la secuencia`() {
        // Es la rama `v1 + 1 == counter` de $00:F28C: repisar el que acabas de coger no es
        // ni avanzar ni reiniciar. Sin ella, estar dos fotogramas sobre el bloque —que es
        // lo normal andando— dejaría la cuenta a cero para siempre.
        val e = engine(intArrayOf(0, 1, 2, 3))
        e.moveX = 1f
        repeat(400) {
            e.tick()
            if (e.checkpoints1up == 1) e.moveX = 0f   // se para justo encima del primero
        }
        assertEquals(1, e.checkpoints1up, "sigue contando UNO tras quedarse quieto encima")
    }

    @Test
    fun `los puntos de control NO se consumen`() {
        // La rutina del juego nunca llama a `GenerateTile`: los bloques siguen ahí (son
        // invisibles), y hace falta que sigan para que la regla de reinicio funcione.
        val e = engine(intArrayOf(0, 1, 2, 3))
        e.caminaDerecha(300)
        assertEquals(
            BlockAction.CHECKPOINT_1UP_1, e.blockActionAt(columnas[0], floorRow - 1),
            "el primer punto sigue en el mapa",
        )
    }

    @Test
    fun `los cuatro bytes bajos se clasifican en su orden de secuencia`() {
        // `v1 = j - 111` y `(uint8)(j - 111) < 4`: 0x6F..0x72, y el índice es el sitio en
        // la secuencia. Fuera de ese rango no son puntos de control.
        assertEquals(SmwBlockAction.CHECKPOINT_1UP_1, SmwBlockBehavior.classify(0x6F))
        assertEquals(SmwBlockAction.CHECKPOINT_1UP_2, SmwBlockBehavior.classify(0x70))
        assertEquals(SmwBlockAction.CHECKPOINT_1UP_3, SmwBlockBehavior.classify(0x71))
        assertEquals(SmwBlockAction.CHECKPOINT_1UP_4, SmwBlockBehavior.classify(0x72))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x73))
        // Y en el plano de TERRENO esos bytes no son esto: 0x6F..0x72 caen fuera del rango
        // de golpeables (0x11..0x2D), así que son terreno y punto.
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x16F))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x172))
    }

    @Test
    fun `los ordinales nuevos van al final y no se pueden reordenar`() {
        assertEquals(6, SmwBlockAction.CHECKPOINT_1UP_1.ordinal)
        assertEquals(9, SmwBlockAction.CHECKPOINT_1UP_4.ordinal)
        assertEquals(7, BlockAction.CHECKPOINT_1UP_1.ordinal)
        assertEquals(10, BlockAction.CHECKPOINT_1UP_4.ordinal)
        // Y el orden del enum ES la secuencia: el motor usa la diferencia de ordinales.
        assertEquals(
            listOf(1, 2, 3),
            listOf(
                BlockAction.CHECKPOINT_1UP_2.ordinal - BlockAction.CHECKPOINT_1UP_1.ordinal,
                BlockAction.CHECKPOINT_1UP_3.ordinal - BlockAction.CHECKPOINT_1UP_1.ordinal,
                BlockAction.CHECKPOINT_1UP_4.ordinal - BlockAction.CHECKPOINT_1UP_1.ordinal,
            ),
        )
    }
}
