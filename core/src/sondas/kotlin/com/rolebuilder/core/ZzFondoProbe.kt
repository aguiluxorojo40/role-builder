package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwLevelNames
import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import java.io.File
import kotlin.test.Test

/**
 * SONDA DESECHABLE de AUDITORÍA: de dónde sale el FONDO de cada nivel y de qué tamaño es.
 *
 * Importa porque [SnesGameRecipes.extractSmwLevelAsMap] REPITE el fondo comprimido en
 * las dos direcciones (`x % ancho`, `y % alto`). Si el telón del nivel es más bajo que
 * las 27 filas del nivel, la repetición VERTICAL vuelve a pintar el cielo por debajo y
 * se come lo que hubiera en la parte de abajo (p.ej. la banda de agua de un lago).
 *
 * Se salta sola sin `SMW_ROM`.
 */
class ZzFondoProbe {

    @Test
    fun `tamano del fondo de los primeros niveles`() {
        val p = System.getenv("SMW_ROM") ?: return
        if (!File(p).isFile) return
        val rom = File(p).readBytes()
        val header = SnesDecoder.parseHeader(rom)
        val delta = SnesGameRecipes.smwHeaderDeltaPublic(header)
        val niveles = (System.getenv("SMW_LEVELS") ?: "104,105,106,103,102")
            .split(",").map { it.trim().toInt(16) }

        for (lv in niveles) {
            val img = runCatching { SnesGameRecipes.renderSmwBackground(rom, header, lv) }.getOrNull()
            val l2 = runCatching { SnesGameRecipes.renderSmwLayer2Objects(rom, header, lv) }.getOrNull()
            println("%03X %-18s fondoImagen=%s  layer2Objetos=%s".format(
                lv, SmwLevelNames.nameOf(rom, delta, lv)?.trim() ?: "-",
                img?.let { "${it.width / 16}x${it.height / 16} casillas" } ?: "(ninguno)",
                l2?.let { "${it.width / 16}x${it.height / 16} casillas" } ?: "(ninguno)"))
        }
    }
}
