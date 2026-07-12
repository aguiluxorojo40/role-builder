package com.rolebuilder.core.snes

/**
 * COMPORTAMIENTO interactivo de un bloque Map16 de SMW, complementario a la SOLIDEZ
 * ([SmwBlockCollision]/[SmwSolidity]). Mientras la solidez dice si te paras/chocas, esto dice
 * si el bloque es un COLECCIONABLE o la META del nivel — la capa de "juego" que el motor de
 * plataformas necesita para que un nivel importado sea jugable de verdad, no solo transitable.
 *
 * Solo mira los bloques de "block code" de la PÁGINA 0 (0x001..0x0FF); el terreno (página 1)
 * nunca es moneda ni meta. Los números de bloque son los que dibuja el propio parser de objetos:
 *  - Moneda estándar (bloque 0x2B) y las dos mitades de la moneda de dragón (0x2D/0x2E).
 *  - Cartel/poste de META (bloques 0x66..0x69, los que coloca ExtObj86_GoalSign).
 *  - ? bloque / bloque de premio (0x21..0x24): al golpearlo por abajo suelta un power-up. Es el
 *    rango que el propio juego comprueba en el golpe de cabeza (RunPlayerBlockCode_EB77:
 *    `tile_lo in 0x21..0x24` con velocidad vertical hacia arriba).
 */
enum class SmwBlockBehavior { NONE, COIN, GOAL, PRIZE }

object SmwBlockBehaviorClassifier {

    /** Comportamiento del bloque Map16 [block] (alto*0x100 + bajo), o NONE si no es interactivo. */
    fun classify(block: Int): SmwBlockBehavior {
        if (block !in 1..0xFF) return SmwBlockBehavior.NONE // solo block-code de página 0
        return when (block) {
            0x2B, 0x2D, 0x2E -> SmwBlockBehavior.COIN
            in 0x21..0x24 -> SmwBlockBehavior.PRIZE
            in 0x66..0x69 -> SmwBlockBehavior.GOAL
            else -> SmwBlockBehavior.NONE
        }
    }
}
