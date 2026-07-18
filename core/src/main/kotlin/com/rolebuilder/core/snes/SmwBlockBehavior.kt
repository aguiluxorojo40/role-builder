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
 * de monedas (`player_current_coin_count`) sube al recogerla.
 *
 * El bloque `?` / PREMIO ([QUESTION]) es el byte bajo 0x21..0x24: la rutina de golpe
 * desde abajo ($00, `RunPlayerBlockCode`) solo trata el bloque como golpeable si el
 * jugador sube y `0x21 <= tile_lo < 0x25`, y entonces llama a `CheckIfBlockWasHit`
 * (que suelta el contenido). Son bloques SÓLIDOS (te apoyas y los cabeceas).
 */
enum class SmwBlockAction { NONE, COIN, QUESTION, DRAGON_COIN }

object SmwBlockBehavior {

    /** Byte bajo del bloque Map16 de una moneda suelta (confirmado en $00:F545). */
    private const val COIN_LO = 0x2B

    /** Rango de bytes bajos de los bloques golpeables `?`/premio (confirmado en $00:EBxx). */
    private val QUESTION_LO = 0x21..0x24

    /**
     * Bytes bajos de la DRAGON COIN (llamada "Yoshi Coin" en el ROM): es un objeto de
     * 16×32 formado por DOS bloques Map16 apilados — `$2D` (mitad de abajo) y `$2E`
     * (mitad de arriba). Confirmado en el "Yoshi Coin Handler" ($00:F332): `CPY #$2D`
     * recoge la de abajo, y la de arriba resta $10 a la Y del bloque tocado para recoger
     * en la posición de abajo; al llegar a 5 marca [AllDragonCoinsCollected] y suena el
     * SFX propio. Se recoge como una moneda grande (5 = vida extra).
     */
    private val DRAGON_COIN_LO = 0x2D..0x2E

    /**
     * Acción interactiva del bloque Map16 [block] (tal cual lo entrega
     * [SmwLayer1.SmwLevelTilemap.block]: `alto*0x100 + bajo`, 0x000..0x1FF). Solo el
     * plano "block code" (byte alto 0) interactúa; el terreno normal es [SmwBlockAction.NONE].
     */
    fun classify(block: Int): SmwBlockAction {
        if (block < 0 || block ushr 8 != 0) return SmwBlockAction.NONE
        val lo = block and 0xFF
        return when {
            lo == COIN_LO -> SmwBlockAction.COIN
            lo in DRAGON_COIN_LO -> SmwBlockAction.DRAGON_COIN
            lo in QUESTION_LO -> SmwBlockAction.QUESTION
            else -> SmwBlockAction.NONE
        }
    }
}
