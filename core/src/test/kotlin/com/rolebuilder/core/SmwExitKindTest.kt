package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwGameSave
import com.rolebuilder.core.snes.SmwLevelGoal
import com.rolebuilder.core.snes.SmwLevelGoal.ExitKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Salida NORMAL vs SECRETA: en SMW un nivel puede tener dos y abren caminos distintos en el
 * overworld. Fija la clasificación por sprite (cinta/esfera = normal, cerradura = secreta) y
 * los banderines por translevel, que es como el juego recuerda cuál llevas hecha.
 */
class SmwExitKindTest {

    @Test
    fun `la cerradura es salida secreta y la cinta o esfera son normales`() {
        assertEquals(ExitKind.SECRET, SmwLevelGoal.exitKindOf(SmwLevelGoal.KEYHOLE))
        assertEquals(ExitKind.NORMAL, SmwLevelGoal.exitKindOf(SmwLevelGoal.GOAL_TAPE))
        assertEquals(ExitKind.NORMAL, SmwLevelGoal.exitKindOf(SmwLevelGoal.GOAL_SPHERE))
        assertEquals(null, SmwLevelGoal.exitKindOf(0x12), "un sprite cualquiera no es meta")
    }

    @Test
    fun `los bits de banderin son los del juego`() {
        assertEquals(0x01, SmwLevelGoal.exitFlagBit(ExitKind.NORMAL))
        assertEquals(0x02, SmwLevelGoal.exitFlagBit(ExitKind.SECRET))
    }

    @Test
    fun `la partida recuerda cada salida por separado`() {
        val s0 = SmwGameSave()
        assertFalse(s0.hasExit(5, ExitKind.NORMAL))

        // Superado por la salida normal: la secreta sigue pendiente.
        val s1 = s0.withLevelBeatenBy(translevel = 5, event = 3, exit = ExitKind.NORMAL)
        assertTrue(s1.hasExit(5, ExitKind.NORMAL))
        assertFalse(s1.hasExit(5, ExitKind.SECRET), "la secreta no se marca sola")
        assertTrue(3 in s1.firedEvents)
        assertTrue(5 in s1.completedTranslevels)

        // Y ahora la secreta: se ACUMULA, no reemplaza (nivel al 100%).
        val s2 = s1.withLevelBeatenBy(translevel = 5, event = 4, exit = ExitKind.SECRET)
        assertTrue(s2.hasExit(5, ExitKind.NORMAL))
        assertTrue(s2.hasExit(5, ExitKind.SECRET))
        assertEquals(0x03, s2.levelExitFlags[5])
    }
}
