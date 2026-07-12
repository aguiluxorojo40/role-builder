package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwBlockBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests del clasificador de comportamiento de bloques (moneda). */
class SmwBlockBehaviorTest {

    @Test
    fun `la moneda es el byte bajo 0x2B del plano de block-code`() {
        assertEquals(SmwBlockAction.COIN, SmwBlockBehavior.classify(0x2B))
        assertEquals(SmwBlockAction.COIN, SmwBlockBehavior.classify(0x02B)) // alto 0
    }

    @Test
    fun `los bloques golpeables 0x21-0x24 son bloques interrogante`() {
        for (lo in 0x21..0x24) {
            assertEquals(SmwBlockAction.QUESTION, SmwBlockBehavior.classify(lo), "0x${lo.toString(16)}")
        }
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x20))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x25))
    }

    @Test
    fun `el terreno y los bloques del plano alto no son interactivos`() {
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x25)) // aire
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x100)) // terreno sólido
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x12B)) // 0x2B pero plano alto 1
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(-1))
    }
}
