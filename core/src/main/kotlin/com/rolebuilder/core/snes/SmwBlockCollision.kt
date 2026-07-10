package com.rolebuilder.core.snes

/**
 * Solidez (clase de colisión) de un bloque Map16 de Super Mario World.
 *
 * Este es el dato que faltaba para poder JUGAR un nivel extraído: la rejilla de
 * bloques ([SmwLayer1]) dice *qué* bloque hay en cada celda y cómo se dibuja, pero
 * no si el jugador se para encima, choca, lo atraviesa, resbala o muere. Aquí se
 * clasifica cada bloque exactamente como lo hace el juego.
 *
 * - [NONE]: no colisiona con el terreno. Aire y bloques de "block code" de la
 *   página 0 (monedas, bloques ?, mensajes, decoración): su efecto lo lleva el
 *   intérprete de bloques, no la colisión de movimiento.
 * - [LEDGE_TOP]: sólido SOLO por arriba (plataforma de un sentido). Te paras
 *   encima, pero lo atraviesas de lado y saltando desde abajo. Es el borde de
 *   suelo clásico de SMW (bloque 0x100).
 * - [SOLID]: sólido por los cuatro lados (muro, suelo macizo, tubería, bloque ?).
 * - [SLOPE]: cuesta de un bloque de alto; la superficie sigue la tabla de forma.
 * - [SLOPE_STEEP]: cuesta que ocupa dos bloques en vertical (tramo de 45°+).
 * - [SPIKE]: el contacto mata al jugador (pinchos, lava, muerte instantánea).
 */
enum class SmwSolidity { NONE, LEDGE_TOP, SOLID, SLOPE, SLOPE_STEEP, SPIKE }

/**
 * Clasificador de solidez de bloques Map16 de SMW, portado FIEL de la rutina de
 * colisión del jugador `RunPlayerBlockCode` ($00:EB77, vía la recompilación
 * snesrev/smw). No adivina por dibujo ni por rangos inventados: reproduce las
 * comparaciones EXACTAS que el juego aplica al número de bloque para decidir la
 * solidez, así que el resultado coincide con cómo se comporta la ROM.
 *
 * Cómo funciona en el juego: el layout del nivel se guarda en dos planos por celda,
 * el byte bajo ($7EC800) y el alto ($7FC800) del número de bloque Map16. Eso es
 * justo lo que [SmwLayer1.SmwLevelTilemap.block] devuelve: `alto*0x100 + bajo`.
 * La rutina de colisión reparte según el plano alto y clasifica el bajo por rangos:
 *
 *  - alto == 0 (bloques 0x000..0x0FF): son tiles de "block code" (la página con
 *    monedas, bloques ?, cajas de mensaje, agua interactiva…). No cuentan como
 *    terreno sólido; su lógica la lleva el intérprete de bloques → [SmwSolidity.NONE].
 *  - alto != 0 (bloques 0x100..0x1FF, el terreno) se clasifica por el byte bajo:
 *      0x00..0x10 → sólido solo por ARRIBA ([LEDGE_TOP]); las sondas de pared y
 *                   techo de la rutina saltan estos valores (`< 0x11`), la del suelo
 *                   los acepta (`< 0x6E`): de ahí la plataforma de un sentido.
 *      0x11..0x6D → sólido por los cuatro lados ([SOLID]).
 *      0x6E..0xD7 → cuesta ([SLOPE], usa la tabla de forma del juego).
 *      0xD8..0xFA → cuesta de dos bloques ([SLOPE_STEEP]).
 *      0xFB..0xFF → muerte por contacto ([SPIKE]).
 */
object SmwBlockCollision {

    /** Umbrales EXACTOS de la rutina $00:EB77 sobre el byte bajo del bloque de terreno. */
    private const val WALL_MIN = 0x11        // < 0x11 → no es pared ni techo (borde de un sentido)
    private const val SLOPE_MIN = 0x6E       // 0x11..0x6D sólido; >= 0x6E cuesta
    private const val SLOPE_STEEP_MIN = 0xD8 // 0x6E..0xD7 cuesta simple; 0xD8..0xFA cuesta doble
    private const val SPIKE_MIN = 0xFB       // >= 0xFB muerte instantánea

    /**
     * Clase de colisión del bloque Map16 [block] (tal cual lo entrega
     * [SmwLayer1.SmwLevelTilemap.block]: `alto*0x100 + bajo`, 0x000..0x1FF).
     * Un valor negativo (fuera del nivel) se trata como [SmwSolidity.NONE].
     */
    fun classify(block: Int): SmwSolidity {
        if (block < 0) return SmwSolidity.NONE
        // Plano alto 0 = página de "block code": no es terreno sólido.
        if (block ushr 8 == 0) return SmwSolidity.NONE
        return when (block and 0xFF) {
            in 0x00 until WALL_MIN -> SmwSolidity.LEDGE_TOP
            in WALL_MIN until SLOPE_MIN -> SmwSolidity.SOLID
            in SLOPE_MIN until SLOPE_STEEP_MIN -> SmwSolidity.SLOPE
            in SLOPE_STEEP_MIN until SPIKE_MIN -> SmwSolidity.SLOPE_STEEP
            else -> SmwSolidity.SPIKE
        }
    }

    /** ¿El jugador se para ENCIMA de este bloque? (sonda de suelo: [LEDGE_TOP] y [SOLID]).
     *  Las cuestas tienen su propia resolución de altura y no se cuentan aquí. */
    fun standsOn(block: Int): Boolean = when (classify(block)) {
        SmwSolidity.LEDGE_TOP, SmwSolidity.SOLID -> true
        else -> false
    }

    /** ¿Este bloque frena el avance HORIZONTAL? Solo el [SOLID] es pared; el borde de
     *  un sentido ([LEDGE_TOP]) se atraviesa de lado (sonda de pared: bajo 0x11..0x6D). */
    fun blocksSide(block: Int): Boolean = classify(block) == SmwSolidity.SOLID

    /** ¿Este bloque frena al jugador por ARRIBA (golpe de cabeza)? Igual que la pared:
     *  solo el [SOLID] hace de techo; los bordes de un sentido se saltan. */
    fun blocksCeiling(block: Int): Boolean = classify(block) == SmwSolidity.SOLID

    /** ¿Es una cuesta (simple o doble)? */
    fun isSlope(block: Int): Boolean =
        classify(block) == SmwSolidity.SLOPE || classify(block) == SmwSolidity.SLOPE_STEEP

    /** ¿El contacto con este bloque mata al jugador? */
    fun isDeadly(block: Int): Boolean = classify(block) == SmwSolidity.SPIKE
}
