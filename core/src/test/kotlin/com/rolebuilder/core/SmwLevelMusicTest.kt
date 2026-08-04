package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwMusic
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La `LevelMusicTable` real de SMW: el ajuste de música de la cabecera (0..7) indexa la tabla
 * para dar el id de canción ($1DFB) — NO es "ajuste + 1". Fija el mapeo verificado contra el
 * desensamblado (lv_read.s: .DB $02,$06,$01,$08,$07,$03,$05,$12).
 */
class SmwLevelMusicTest {

    @Test
    fun `la tabla de musica de nivel es la del juego`() {
        assertEquals(
            listOf(0x02, 0x06, 0x01, 0x08, 0x07, 0x03, 0x05, 0x12),
            SmwMusic.LEVEL_MUSIC_TABLE.toList(),
        )
    }

    @Test
    fun `levelSongId mapea el ajuste de cabecera a la cancion real`() {
        // Ajuste 2 = tema de nivel principal ($01), no la cancion 3.
        assertEquals(0x01, SmwMusic.levelSongId(2))
        assertEquals(0x08, SmwMusic.levelSongId(3)) // castillo
        assertEquals(0x03, SmwMusic.levelSongId(5)) // agua
        assertEquals(0x05, SmwMusic.levelSongId(6)) // jefe
    }

    @Test
    fun `levelSongId acota el ajuste al rango 0 a 7 sin reventar`() {
        assertEquals(SmwMusic.levelSongId(0), SmwMusic.levelSongId(8))   // 8 & 7 = 0
        assertEquals(SmwMusic.levelSongId(1), SmwMusic.levelSongId(0x11)) // 0x11 & 7 = 1
    }
}
