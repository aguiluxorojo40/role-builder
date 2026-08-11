package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwSpriteNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests ROM-free de los nombres de sprite. Un nombre no cambia ningún píxel, así que ningún
 * test de dibujo lo protege — y sin embargo es lo que se lee en el editor y en cada informe.
 * El 0x13 estuvo mucho tiempo llamado "KoopaKidBossFight" y nadie se dio cuenta.
 */
class SmwSpriteNamesTest {

    @Test
    fun `el 0x13 es el Spiny y no una pelea de jefe`() {
        // Dos sitios de la recompilación, y ninguno deja margen:
        //  · `kSprStatus08SpriteNormalPtrs` (banco $01) manda el 0x13 a
        //    `SprXXX_Generic_SpinyEntry`, la familia andadora genérica.
        //  · `Spr014_SpinyEgg` ($01:8C18), el huevo de Spiny, al tocar el suelo hace
        //    `spr_spriteid[k] = 19` — el huevo se convierte EN este id.
        // Eran 40 colocaciones en 4 niveles etiquetadas como pelea de jefe.
        assertEquals("Spiny", SmwSpriteNames.nameOf(0x13))
        // Y el huevo del que sale sigue siendo el huevo.
        assertEquals("SpinyEgg", SmwSpriteNames.nameOf(0x14))
    }

    @Test
    fun `los ids que ya tienen dibujo tienen tambien nombre`() {
        // Salían en el editor como "Sprite 0xXX" justo despues de darles gráfico, que es la
        // forma más tonta de estrenar un sprite nuevo. `nameOf` cae a ese texto cuando el id
        // no está catalogado, así que basta con comprobar que no es el fallback.
        val esperados = mapOf(
            0x15 to "FixedMovementCheepCheep",
            0x4D to "SmallMontyMole",
            0x55 to "HorizontalCheckerboardPlatform",
            0x59 to "HorizontalAndVerticalTurnBlockBridge",
            0x5A to "HorizontalTurnBlockBridge",
            0x5D to "BuoyantFloatingPlatformDiagonal",
            0xA4 to "SpikeBall",
        )
        for ((id, nombre) in esperados) {
            assertEquals(nombre, SmwSpriteNames.nameOf(id), "0x%02X".format(id))
            assertTrue(!SmwSpriteNames.nameOf(id).startsWith("Sprite 0x"), "0x%02X sin nombre".format(id))
        }
    }
}
