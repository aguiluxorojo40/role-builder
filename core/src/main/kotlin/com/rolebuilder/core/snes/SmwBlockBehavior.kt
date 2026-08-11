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
enum class SmwBlockAction {
    NONE, COIN, QUESTION, DRAGON_COIN, MOON_3UP, MIDWAY_TAPE,

    /**
     * Los CUATRO puntos de control del 1-UP escondido, en su ORDEN de la secuencia
     * (0x6F, 0x70, 0x71 y 0x72). Van como cuatro acciones distintas y no como una sola
     * porque el juego exige tocarlos EN ORDEN: si solo se supiera "esto es un punto de
     * control" se perdería justo el dato que decide si suena el 1-UP o se reinicia la
     * cuenta. Ver [SmwBlockBehavior.CHECKPOINT_1UP_LO] y `RunPlayerBlockCode_00F28C`.
     */
    CHECKPOINT_1UP_1, CHECKPOINT_1UP_2, CHECKPOINT_1UP_3, CHECKPOINT_1UP_4,
}

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
     * Ver [INTERRUPTOR_P_PENDIENTE].
     */
    private val COIN_LO_ALL = 0x2A..0x2C

    /**
     * HUECO CONOCIDO: el INTERRUPTOR-P (azul y plateado) NO está implementado, y esta
     * clasificación describe siempre el nivel "en reposo". Queda escrito aquí, con todo lo
     * averiguado, para que quien lo haga no tenga que volver a buscarlo — y para que no se
     * confunda con un olvido.
     *
     * QUÉ ES. `ModifyMap16IDForSpecialBlocks` ($00:F545) reinterpreta cada casilla EN EL
     * MOMENTO DE LEERLA, no reescribe el mapa. Con `timer_blue_pswitch` activo:
     *  - plano de BLOCK CODE, `lo == 0x2B` (moneda) → devuelve `lo = 0x32` y, como la
     *    función devuelve ese mismo valor (≠ 0) en el sitio del byte ALTO, la moneda pasa
     *    a ser **SÓLIDA**: es el clásico "monedas → bloques";
     *  - plano de TERRENO, `lo == 0x32` (bloque) → `lo = 0x2B` y devuelve 0, o sea deja de
     *    ser sólido: "bloques → monedas";
     *  - plano de BLOCK CODE, `lo == 0x29` → `lo = 0x24` (un `?`);
     *  - y con el PLATEADO, terreno `lo == 0x2F` → moneda 0x2B.
     * Lo enciende PISAR el sprite 0x3E: `spr_spriteid[k] == 62` → `*(&timer_blue_pswitch +
     * spr_table151c[k]) = -80`, o sea **176 fotogramas** ($01:~3588; `spr_table151c` vale 0
     * para el azul y 1 para el plateado).
     *
     * POR QUÉ NO ESTÁ. No es una acción de celda más: exige que la SOLIDEZ de una casilla
     * cambie en caliente, y en este motor la solidez entra por un `solidityAt` fijo que se
     * captura al construir. Media implementación —cambiar solo la acción y no la solidez—
     * dejaría las monedas convertidas en paredes invisibles y los bloques en suelo falso:
     * peor que no tenerlo. Además el dibujado de :app decide qué pinta con la acción del
     * TILESET, no con la rejilla del motor, así que tampoco seguiría al intercambio.
     *
     * DÓNDE VIVIRÍA EL ESTADO. El temporizador es estado de PARTIDA EN CURSO del nivel, o
     * sea [com.rolebuilder.core.engine.platformer.PlatformerEngine], al lado del reloj
     * (`timeLeft`): se enciende al pisar el sprite y baja solo. Lo que hace falta antes:
     * (1) una acción nueva para el bloque de terreno `0x132`, que hoy no se clasifica;
     * (2) que el motor pueda invertir la solidez de una celda; (3) comportamiento para el
     * sprite 0x3E, que hoy solo tiene gráfico.
     *
     * CUÁNTO AFECTA (medido sobre la ROM US, casillas por nivel):
     * YOSHI'S ISLAND 2 → 27 monedas + 1 bloque 0x132, y su interruptor en (282,17);
     * YOSHI'S ISLAND 4 → 35 monedas + 16 bloques, interruptor en (72,13);
     * YOSHI'S ISLAND 3 → 31 monedas, y 18 más en su sala 0x1FD, que es donde la guía dice
     * que hay que usarlo para cruzar. YOSHI'S ISLAND 1 no tiene ninguno.
     */
    const val INTERRUPTOR_P_PENDIENTE = "no implementado: ver KDoc"

    /**
     * HUECO CONOCIDO nº 2: los BLOQUES `!` DE INTERRUPTOR (los de colores) tampoco están, y
     * por una razón distinta a la del interruptor-P: **no es estado de nivel, es estado de
     * PARTIDA**, y no se resuelve al tocarlos sino al CONSTRUIR el nivel.
     *
     * `ExtObj8E` ($0D:B58D) escribe la silueta punteada (`lo = 0x6B`, plano de block code) o
     * el bloque macizo según los banderines de palacio de la partida guardada; el
     * equivalente en este proyecto es `SmwLayer1.extSwitchBlockYellow`, que hoy escribe
     * SIEMPRE la versión punteada — que es la correcta para una partida nueva. La otra
     * mitad, `ModifyMap16IDForSpecialBlocks` ($00:F545), convierte en bloque sólido todo
     * `lo` de 0xEC..0xFB en el plano de block code (`(uint8)(lo + 20) < 0x10`).
     *
     * No se toca desde aquí a propósito: la sustitución hay que hacerla donde se construye
     * el nivel ([SmwLayer1]) y con los banderines de [SmwGameSave], no en el clasificador
     * de bloques. Clasificarlos aquí sin eso solo serviría para que el motor los tratara
     * como algo que en una partida nueva NO son.
     *
     * CUÁNTO AFECTA (medido): 97 casillas en YOSHI'S ISLAND 3 —donde son los PUENTES sobre
     * los huecos, así que el nivel cambia de forma—, 5 en el 1, 4 en el 2 y 1 en el 4.
     */
    const val BLOQUES_INTERRUPTOR_PENDIENTE = "no implementado: ver KDoc"

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
     * Bytes bajos de los CUATRO PUNTOS DE CONTROL del 1-UP escondido, del plano de block
     * code. `RunPlayerBlockCode_00F28C` ($00:F28C) empieza con `v1 = j - 111` y descarta
     * todo lo que no cumpla `(uint8)(j - 111) < 4`: son 0x6F, 0x70, 0x71 y 0x72, y el
     * índice `v1` (0..3) es su SITIO en la secuencia.
     *
     * La regla completa de esa rutina, que es lo que hace falta portar:
     *  - si `v1 == counter_1up_check_points_collected` (es el que tocaba), avanza:
     *    `counter = v1 + 1`, y con `v1 == 3` (el cuarto) llama a `TriggerHidden1up()`
     *    ($03:C2D9) y marca `flag_collected1up_checkpoints` del nivel;
     *  - si no, y `v1 + 1 != counter` y `counter < 4`, REINICIA la cuenta a 0
     *    (`v1 = -1; counter = v1 + 1`);
     *  - `v1 + 1 == counter` es volver a pisar el que acabas de coger: no hace nada (por
     *    eso quedarse encima del bloque no rompe la secuencia).
     * `TriggerHidden1up` suelta el sprite 0x78, que es la SETA VERDE de una vida
     * (`SprXXX_PowerUps_01C538`, $01:C538: `v2 = powerup | 4*(id - 0x74)`, y el grupo del
     * 0x78 son las cuatro entradas `5` de `kSprXXX_PowerUps_GivePowerPtrIndex` →
     * `SprXXX_PowerUps_GiveMario1Up`).
     *
     * Los bloques NO desaparecen al tocarlos: la rutina nunca llama a `GenerateTile`.
     * En SMW son invisibles, y de hecho hay que poder repisarlos para que la regla de
     * reinicio tenga sentido.
     */
    val CHECKPOINT_1UP_LO = 0x6F..0x72

    /** Los cuatro puntos de control EN ORDEN, indexados por `lo - 0x6F`. */
    private val CHECKPOINTS = arrayOf(
        SmwBlockAction.CHECKPOINT_1UP_1, SmwBlockAction.CHECKPOINT_1UP_2,
        SmwBlockAction.CHECKPOINT_1UP_3, SmwBlockAction.CHECKPOINT_1UP_4,
    )

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
            lo in CHECKPOINT_1UP_LO -> CHECKPOINTS[lo - CHECKPOINT_1UP_LO.first]
            lo in QUESTION_LO -> SmwBlockAction.QUESTION
            else -> SmwBlockAction.NONE
        }
    }
}
