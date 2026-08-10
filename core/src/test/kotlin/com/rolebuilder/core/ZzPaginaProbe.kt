package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwEnemyGraphics
import com.rolebuilder.core.snes.SmwSpriteBehaviorReader
import com.rolebuilder.core.snes.SmwSpriteNames
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test

/**
 * SONDA DESECHABLE: ¿coincide la PÁGINA de tesela que hereda cada enemigo de dibujo propio
 * ($166E, bit 0) con la que su rutina fija a mano en su tabla de propiedades?
 *
 * Es la pregunta que abrió el fallo de las Super Koopa. Casi todas estas rutinas escriben su
 * `flags` con una constante, y ahí el bit 0 es el noveno bit del nº de tesela: heredar la
 * página del nivel solo funciona mientras las dos coincidan por casualidad. Esto lista los
 * casos en que NO coinciden, que son los que están dibujando la mitad equivocada de la VRAM.
 */
class ZzPaginaProbe {

    /** Página que fija a mano la tabla de propiedades de cada rutina (bit 0 del Prop). */
    private val paginaDeLaRutina = mapOf(
        0xAB to 1,  // kSpr0AB_Rex_Prop = {0x47, 0x07}
        0xC3 to 1,  // kSpr0C3_PorcuPuffer_PocruPufferGfxProp = {0x0d, ...}
        0xAA to 1,  // kSpr0AA_Fishbone_Prop = {0x4d, 0xcd, 0x0d, 0x8d}
        0x9F to 1,  // kSpr09F_BanzaiBill_Prop = {0x33, ...}
        0xA8 to 1,  // kSpr0A8_Blargg_Prop = {0x45, 0x05}
        0x6F to 1,  // kSpr06F_DinoTorch_DinoTorchProp = {0x09, 0x05, ...}
        0x6E to 1,  // kSpr06F_DinoTorch_DinoRhinoProp = {0x2f, ...}
        0xA9 to 1,  // kSpr0A9_Reznor_Prop = {0x3f, ...}
    )

    @Test
    fun `la pagina heredada contra la que fija cada rutina`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val beh = SmwSpriteBehaviorReader.read(rom, header) ?: return

        println("\n=== PÁGINA: heredada de \$166E contra la que fija la rutina ===")
        var discrepan = 0
        for (id in SmwEnemyGraphics.customEnemyIds.sorted()) {
            val heredada = (beh[id].b166e and 0x0F) and 0x01
            val propia = paginaDeLaRutina[id]
            val marca = when {
                propia == null -> "  (su rutina no fija propiedad: hereda de verdad)"
                propia == heredada -> "  ok (coinciden)"
                else -> "  ¡DISCREPAN! la rutina manda ${propia}".also { discrepan++ }
            }
            println("%02X %-26s heredada=%d %s".format(id, SmwSpriteNames.nameOf(id), heredada, marca))
        }
        println("\ncasos que discrepan: $discrepan")
    }
}
