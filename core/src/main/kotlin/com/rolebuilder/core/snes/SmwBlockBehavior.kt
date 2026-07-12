package com.rolebuilder.core.snes

/**
 * Comportamiento INTERACTIVO de un bloque Map16 de Super Mario World: lo que hace
 * cuando el jugador lo toca o lo golpea, más allá de su solidez ([SmwBlockCollision]).
 *
 * SMW guarda el terreno en dos planos por celda; el byte BAJO del número de bloque
 * Map16 es el que la rutina de bloques (`RunPlayerBlockCode`, banco $00) usa para
 * decidir el efecto. Los bloques del plano alto 0 (0x000..0x0FF) son "block code"
 * (monedas, bloques `?`, cajas de mensaje, note blocks…); su lógica la lleva ese
 * intérprete, no la colisión de movimiento.
 *
 * De momento clasificamos lo que hemos podido fijar 1:1 en la recompilación
 * snesrev/smw. La MONEDA es el byte bajo 0x2B: se confirma en
 * `ModifyMap16IDForSpecialBlocks` ($00:F545), que con el interruptor-P azul
 * intercambia 0x2B ↔ 0x32 —el clásico "monedas ⇄ bloques" de SMW—, y `counter`
 * de monedas (`player_current_coin_count`) sube al recogerla. El resto de bloques
 * interactivos (`?`, note, message, turn) se irán añadiendo a esta tabla.
 */
enum class SmwBlockAction { NONE, COIN }

object SmwBlockBehavior {

    /** Byte bajo del bloque Map16 de una moneda suelta (confirmado en $00:F545). */
    private const val COIN_LO = 0x2B

    /**
     * Acción interactiva del bloque Map16 [block] (tal cual lo entrega
     * [SmwLayer1.SmwLevelTilemap.block]: `alto*0x100 + bajo`, 0x000..0x1FF). Solo el
     * plano "block code" (byte alto 0) interactúa; el terreno normal es [SmwBlockAction.NONE].
     */
    fun classify(block: Int): SmwBlockAction {
        if (block < 0 || block ushr 8 != 0) return SmwBlockAction.NONE
        return when (block and 0xFF) {
            COIN_LO -> SmwBlockAction.COIN
            else -> SmwBlockAction.NONE
        }
    }
}
