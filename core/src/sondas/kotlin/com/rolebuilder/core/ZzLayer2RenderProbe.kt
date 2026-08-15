package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/** SONDA: ¿llega de verdad el Layer 2 de objetos hasta la imagen y hasta el mapa? */
class ZzLayer2RenderProbe {

    @Test
    fun `el layer 2 de objetos se dibuja y llega al mapa`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        for (lv in intArrayOf(0x009, 0x0DC, 0x1EC, 0x0E7, 0x115)) {
            val img = runCatching { SnesGameRecipes.renderSmwLayer2Objects(rom, header, lv) }
            val i = img.getOrNull()
            val opacos = i?.pixels?.count { it ushr 24 != 0 } ?: -1
            val mapa = runCatching { SnesGameRecipes.extractSmwLevelAsMap(rom, header, lv) }.getOrNull()
            val conFondo = mapa?.bgTiles?.count { it >= 0 } ?: -1
            println(
                "%03X  imagenL2=%s (%s)  pixelesOpacos=%d  celdasFondoEnMapa=%d".format(
                    lv,
                    if (i != null) "${i.width}x${i.height}" else "NULL",
                    img.exceptionOrNull()?.let { "EXC ${it::class.simpleName}: ${it.message}" } ?: "ok",
                    opacos, conFondo,
                ),
            )
        }
    }
}
