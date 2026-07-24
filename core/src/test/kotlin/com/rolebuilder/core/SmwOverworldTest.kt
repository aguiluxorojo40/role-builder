package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworld
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests SINTÉTICOS de la capa estática del overworld ([SmwOverworld]). Se plantan a mano
 * unos bytes en las direcciones $05:D608 (eventos) y $04:8431… (Star Road) de un ROM vacío
 * y se comprueba el decodificado. Sin ROM con copyright.
 */
class SmwOverworldTest {

    /** SNES LoROM → PC headerless (delta 0), igual que usa el lector. */
    private fun pc(snes: Int): Int = (snes shr 16) * 0x8000 + (snes and 0x7FFF)

    private fun putWord(rom: ByteArray, snes: Int, v: Int) {
        rom[pc(snes)] = (v and 0xFF).toByte()
        rom[pc(snes + 1)] = ((v shr 8) and 0xFF).toByte()
    }

    @Test
    fun readsTranslevelEvents() {
        val rom = ByteArray(0x40000)
        rom[pc(SmwOverworld.EVENTS_SNES + 0x00)] = SmwOverworld.EVENT_NONE.toByte()
        rom[pc(SmwOverworld.EVENTS_SNES + 0x01)] = 0x1F
        rom[pc(SmwOverworld.EVENTS_SNES + 0x28)] = 0x00

        val ev = SmwOverworld.translevelEvents(rom, 0)
        assertEquals(256, ev.size)
        assertEquals(SmwOverworld.EVENT_NONE, ev[0x00])
        assertEquals(0x1F, ev[0x01])
        assertEquals(0x00, ev[0x28])
    }

    @Test
    fun readsStarRoadWarps() {
        val rom = ByteArray(0x40000)
        // warp 3: origen (0xB, 0xE) → destino (0x928, 0x18)
        putWord(rom, SmwOverworld.STAR_SRCX_SNES + 2 * 3, 0xB)
        putWord(rom, SmwOverworld.STAR_SRCY_SNES + 2 * 3, 0xE)
        putWord(rom, SmwOverworld.STAR_DSTX_SNES + 2 * 3, 0x928)
        putWord(rom, SmwOverworld.STAR_DSTY_SNES + 2 * 3, 0x18)

        val warps = SmwOverworld.starRoadWarps(rom, 0)
        assertEquals(27, warps.size)
        assertEquals(SmwOverworld.StarWarp(0xB, 0xE, 0x928, 0x18), warps[3])
    }

    @Test
    fun honorsHeaderDelta() {
        // Con cabecera de copiador (0x200), el mismo dato vive 0x200 más adelante.
        val delta = 0x200
        val rom = ByteArray(0x40200)
        rom[pc(SmwOverworld.EVENTS_SNES + 0x05) + delta] = 0x42
        val ev = SmwOverworld.translevelEvents(rom, delta)
        assertEquals(0x42, ev[0x05])
    }
}
