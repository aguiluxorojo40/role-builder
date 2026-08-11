package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelExits
import com.rolebuilder.core.snes.SmwLevelStart
import com.rolebuilder.core.snes.SmwWarpTiles
import com.rolebuilder.core.snes.WarpTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests SINTÉTICOS de [SmwWarpTiles]: clasificación de teselas de warp (tuberías/puertas)
 * por byte bajo Map16 e índice→posición de entrada con las tablas preset vanilla. Sin ROM.
 */
class SmwWarpTilesTest {

    @Test
    fun `las bocas de tuberia vertical son 0x37 y 0x38 DEL PLANO DE TERRENO`() {
        // $00:EB77 solo llega a $00:F3E9 con el byte ALTO != 0 (rama de terreno).
        assertEquals(WarpTile.PIPE_VERTICAL, SmwWarpTiles.pipeOrDoor(0x137))
        assertEquals(WarpTile.PIPE_VERTICAL, SmwWarpTiles.pipeOrDoor(0x138))
        assertNull(SmwWarpTiles.pipeOrDoor(0x136))
        assertNull(SmwWarpTiles.pipeOrDoor(0x139))
    }

    @Test
    fun `la boca de tuberia horizontal es 0x3F DEL PLANO DE TERRENO`() {
        assertEquals(WarpTile.PIPE_HORIZONTAL, SmwWarpTiles.pipeOrDoor(0x13F))
        assertNull(SmwWarpTiles.pipeOrDoor(0x13E))
        assertNull(SmwWarpTiles.pipeOrDoor(0x140))
    }

    @Test
    fun `en el plano de BLOCK CODE los mismos bytes bajos NO son tuberia`() {
        // Con el alto a 0, 0x38 es la CINTA DE MIDWAY ($00:F2C9, `if (j == 56)`) y 0x3F es
        // la TIERRA bajo el borde de hierba (`stdLedge` de SmwLayer1 la pinta con alto 0).
        // Tomarlas por tuberías inundaba de warps falsos cualquier nivel con suelo.
        assertNull(SmwWarpTiles.pipeOrDoor(0x37))
        assertNull(SmwWarpTiles.pipeOrDoor(0x38))
        assertNull(SmwWarpTiles.pipeOrDoor(0x3F))
    }

    @Test
    fun `las puertas siempre-puerta son 0x1F y 0x20 DEL PLANO DE BLOCK CODE`() {
        assertEquals(WarpTile.DOOR, SmwWarpTiles.pipeOrDoor(0x1F))
        assertEquals(WarpTile.DOOR, SmwWarpTiles.pipeOrDoor(0x20))
        assertNull(SmwWarpTiles.pipeOrDoor(0x1E))
        assertNull(SmwWarpTiles.pipeOrDoor(0x21)) // bloque '?' , no puerta
        assertNull(SmwWarpTiles.pipeOrDoor(0x11F)) // 0x1F pero en el plano de terreno
        assertNull(SmwWarpTiles.pipeOrDoor(0x120)) // 0x20 pero en el plano de terreno
    }

    @Test
    fun `las teselas no-warp y lo que se sale del rango devuelven null`() {
        assertNull(SmwWarpTiles.pipeOrDoor(0x2B))  // moneda
        assertNull(SmwWarpTiles.pipeOrDoor(0x00))  // aire
        assertNull(SmwWarpTiles.pipeOrDoor(0x200)) // fuera de los dos planos de Map16
        assertNull(SmwWarpTiles.pipeOrDoor(-1))
    }

    @Test
    fun `PIPE_BUTTONS mapea indice de direccion a boton del joypad`() {
        // 0=Izq 1=Der 2=Arriba 3=Abajo, bits del joypad alto de SMW.
        assertEquals(listOf(0x02, 0x01, 0x08, 0x04), SmwWarpTiles.PIPE_BUTTONS.toList())
    }

    @Test
    fun `presetPosition convierte indices a casilla con las tablas vanilla`() {
        // xIdx: D758<<8|D750; yIdx: D740<<8|D730; /16.
        assertEquals(1 to 0, SmwWarpTiles.presetPosition(0, 0))    // x=0x010=16→1 ; y=0
        assertEquals(14 to 11, SmwWarpTiles.presetPosition(3, 5))  // x=0x0E0=224→14 ; y=0xB0=176→11
        assertEquals(17 to 17, SmwWarpTiles.presetPosition(4, 8))  // x=0x110=272→17 ; y=0x110=272→17
        assertEquals(30 to 25, SmwWarpTiles.presetPosition(7, 13)) // x=0x1E0=480→30 ; y=0x190=400→25
    }

    @Test
    fun `presetPosition enmascara los indices al rango de las tablas`() {
        // xIndex se toma & 0x7, yIndex & 0xF (defensa, no debe desbordar).
        assertEquals(SmwWarpTiles.presetPosition(3, 5), SmwWarpTiles.presetPosition(3 or 0x8, 5 or 0x10))
    }

    @Test
    fun `absolutePosition pone la PANTALLA en el byte alto, pisando el preset`() {
        // `LoadLevel` ($05:D796) hace `r1 &= 0x1F` y `HIBYTE(player_xpos) = r1` DESPUÉS de
        // cargar el preset entero: la pantalla gana. Pantalla 11 + xIdx=3 (D750=0xE0) →
        // x = 0x0BE0 = 3040 px → casilla 190; el 0 de D758[3] no pinta nada.
        assertEquals(190 to 11, SmwWarpTiles.absolutePosition(11, 3, 5, vertical = false))
        // Y aunque el preset traiga byte alto 1 (xIdx=7 → D758=1), la pantalla lo pisa:
        // pantalla 2 + D750[7]=0xE0 → x = 0x02E0 = 736 → casilla 46 (no 0x1E0/16 = 30).
        assertEquals(46 to 25, SmwWarpTiles.absolutePosition(2, 7, 13, vertical = false))
    }

    @Test
    fun `en un nivel VERTICAL la pantalla va a la Y, no a la X`() {
        // `if (misc_level_layout_flags & 1) HIBYTE(player_ypos) = r1;` — ahí la X conserva
        // su byte alto de preset. xIdx=7 → x=0x1E0=480→30 ; pantalla 2 + D730[13]=0x90 →
        // y = 0x0290 = 656 → casilla 41.
        assertEquals(30 to 41, SmwWarpTiles.absolutePosition(2, 7, 13, vertical = true))
    }

    @Test
    fun `entrancePosition usa la pantalla y los indices de la entrada secundaria`() {
        // Mismo fixture que SmwLevelExitsTest: xIdx=3, yIdx=5, PANTALLA=11.
        val se = SmwLevelExits.SecondaryEntrance(
            number = 0x07,
            destinationLevel = 0x1A,
            entranceXIndex = 3,
            entranceYIndex = 5,
            entranceScreen = 11,
            entranceAction = 4,
            layer1YPos = 3,
            layer2YPos = 2,
        )
        // Sin la pantalla salía (14,11) — el principio del nivel. Con ella, (190,11).
        assertEquals(190 to 11, SmwWarpTiles.entrancePosition(se))
    }

    @Test
    fun `mainEntrancePosition es consistente con startTile de SmwLevelStart`() {
        // secHeader[1] & 7 = xIdx, secHeader[0] & 0xF = yIdx. Preset xIdx=2 → x=0x000=0;
        // yIdx=7 → y=0xE0=224→14. Y el propio SmwLevelStart con esos píxeles da lo mismo.
        val start = SmwLevelStart(
            level = 0x105,
            secHeader = intArrayOf(0x07, 0x02, 0x00, 0x00), // yIdx=7, xIdx=2, pantalla 0
            presetPixelX = 0x000,
            presetPixelY = 0x0E0,
        )
        assertEquals(0 to 14, SmwWarpTiles.mainEntrancePosition(start))
        assertEquals(start.startTileX to start.startTileY, SmwWarpTiles.mainEntrancePosition(start))
    }
}
