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
/**
 * Los valores NUEVOS van SIEMPRE al final: el ordinal se guarda en el tileset del proyecto
 * (`platformBlockActions`), así que reordenarlos cambiaría en silencio lo que hace cada
 * bloque de los mapas ya importados.
 */
enum class SmwBlockAction { NONE, COIN, QUESTION, DRAGON_COIN, MOON_3UP, MIDWAY_TAPE }

object SmwBlockBehavior {

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
     * Bytes bajos de MONEDA SUELTA, los tres. `RunPlayerBlockCode_00F309` ($00:F309) los
     * despacha juntos: entra con `0x2A <= j < 0x2F` y todo lo que sea `j < 0x2D` llama a
     * `GiveCoins_OneCoin()`. O sea 0x2A, 0x2B y 0x2C.
     *
     * El 0x2A tiene una condición: `if (j != 42 || timer_blue_pswitch)` — con el
     * interruptor-P AZUL activo NO es moneda (es cuando las monedas se vuelven bloques).
     * Se clasifica igual, porque el estado por defecto de un nivel es sin interruptor.
     */
    private val COIN_LO_ALL = 0x2A..0x2C

    /**
     * Byte bajo de la LUNA 3-UP. `RunPlayerBlockCode_00F309` ($00:F309): `if (j != 110)
     * return` — o sea 0x6E, y lo que hace es subir `unusedram_3up_moons_counter` y marcar
     * `flag_collected_moons` del nivel. Da tres vidas.
     */
    private const val MOON_3UP_LO = 0x6E

    /**
     * Byte bajo de la CINTA DEL PUNTO INTERMEDIO (la "puerta intermedia"). Vive en el
     * plano de BLOCK CODE, y lo dicen los dos extremos de la ROM:
     *
     *  - QUIÉN LA DIBUJA: `ExtObj46_MidwayBar` ($0D:A68E) escribe la cinta con
     *    `SetMap16HighByteForCurrentObject_Page00(...)` —plano 00, explícito— y
     *    `SetMap16LowByte(v2, 0x38)`. Y solo la dibuja si aún no tienes el punto
     *    intermedio (`(ow_level_tile_settings[nivel] & 0x40) == 0 && !flag_got_midpoint`).
     *  - QUIÉN LA TOCA: `RunPlayerBlockCode_00F2C9` ($00:F2C9), `if (j == 56)`. Y ahí solo
     *    se llega desde las ramas con el byte ALTO a 0 (`if (!v9) ... F2C2(...)` →
     *    `RunPlayerBlockCode_00F2C2`, $00:F2C2), o sea el plano de block code.
     *
     * Lo que hace al tocarla ($00:F2C9): sustituye la tesela
     * (`blocks_map16_to_generate = 2; GenerateTile()`), suelta el brillo
     * (`SpawnGlitterEffectForCoin`), marca el punto intermedio
     * (`PlayerState00_SetMidpointFlag` → `flag_got_midpoint = 1`, $00:CA2B), **da un
     * champiñón si vas pequeño** (`if (!player_current_power_up) player_current_power_up = 1`)
     * y suena (`io_sound_ch1 = 5`).
     *
     * OJO con el plano: con el byte alto ≠ 0 el 0x38 NO es esto, es la mitad derecha de una
     * boca de tubería VERTICAL ([SmwWarpTiles]).
     */
    private const val MIDWAY_TAPE_LO = 0x38

    /**
     * Rango de bytes bajos de los bloques GOLPEABLES del plano de TERRENO: el `?` de toda
     * la vida y sus parientes (bloque de premio, de bonus, el que suelta el huevo de Yoshi).
     *
     * ⚠ ESTÁN EN EL PLANO DE TERRENO, NO EN EL DE BLOCK CODE, y por eso no se veía ni uno:
     * la comprobación vive en `RunPlayerBlockCode` ($00:F160 y su llamador), que primero
     * exige el byte ALTO distinto de cero (`if (v2)`) y el bajo en `0x11..0x6D`, y solo
     * entonces llama a `CheckIfBlockWasHit(v3, 0)`; ahí dentro, `a = a - 17; if (a >= 0x1D)`
     * descarta todo lo que no esté en **0x11..0x2D**.
     *
     * Lo que estaba clasificado antes (0x21..0x24 en el plano de block code) no es esto:
     * ese rango sale de otra rama y dejaba fuera TODOS los `?` de los primeros niveles —
     * 7 en YOSHI'S ISLAND 1, 17 en el 2, 9 en el 3—, incluido el que suelta el HUEVO DE
     * YOSHI, que es el motivo de existir de ese nivel.
     */
    private val HITTABLE_TERRAIN_LO = 0x11..0x2D

    /**
     * Acción interactiva del bloque Map16 [block] (tal cual lo entrega
     * [SmwLayer1.SmwLevelTilemap.block]: `alto*0x100 + bajo`, 0x000..0x1FF).
     *
     * **El plano importa**, y confundirlos era el fallo: `RunPlayerBlockCode` bifurca con el
     * byte ALTO antes de mirar el bajo. Lo que se RECOGE (monedas, moneda dragón, luna) vive
     * en el plano de block code (alto == 0); lo que se GOLPEA desde abajo vive en el de
     * terreno (alto != 0), porque son bloques sólidos sobre los que además te apoyas.
     */
    fun classify(block: Int): SmwBlockAction {
        if (block < 0 || block >= 0x200) return SmwBlockAction.NONE
        val lo = block and 0xFF
        val terreno = block ushr 8 != 0
        return when {
            terreno -> if (lo in HITTABLE_TERRAIN_LO) SmwBlockAction.QUESTION else SmwBlockAction.NONE
            lo in COIN_LO_ALL -> SmwBlockAction.COIN
            lo in DRAGON_COIN_LO -> SmwBlockAction.DRAGON_COIN
            lo == MOON_3UP_LO -> SmwBlockAction.MOON_3UP
            lo == MIDWAY_TAPE_LO -> SmwBlockAction.MIDWAY_TAPE
            lo in QUESTION_LO -> SmwBlockAction.QUESTION
            else -> SmwBlockAction.NONE
        }
    }
}
