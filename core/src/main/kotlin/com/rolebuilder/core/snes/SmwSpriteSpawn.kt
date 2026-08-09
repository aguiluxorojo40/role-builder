package com.rolebuilder.core.snes

/**
 * QUÉ ENTRA DE VERDAD EN EL NIVEL cuando la lista de sprites dice un id.
 *
 * Port de `LoadOneSprite` ($02:AA4D en `smw_02.c`). Es una pieza que faltaba y que se
 * notaba: hasta ahora se tomaba el tercer byte de cada entrada de la lista como si fuera
 * ya el id del enemigo, y **no lo es**. El juego lo clasifica antes, y solo el tramo
 * 0x00-0xC8 pasa tal cual. Todo lo de arriba es otra cosa:
 *
 * | id crudo    | qué es en realidad                                              |
 * |-------------|-----------------------------------------------------------------|
 * | 0x00-0xC8   | sprite normal, tal cual, con estado 1                           |
 * | 0xC9-0xCA   | LANZADOR (`SprXXX_LoadShooter`): no es un enemigo, es una boca   |
 * |             | que escupe balas                                                |
 * | 0xCB-0xD9   | GENERADOR (`gen_spr_spriteid = id + 54`): tampoco es un enemigo, |
 * |             | es el que va soltando enemigos por su cuenta                     |
 * | 0xDA-0xDD,  | sprite normal **con estado 9 (aturdido)** e id `crudo + 0x2A`:   |
 * | 0xDF        | o sea, el bicho ya colocado DENTRO de su caparazón               |
 * | 0xDE        | cinco Eeries de golpe (`Spr0DE_Load5Eeries`)                     |
 * | 0xE0        | tres plataformas (`Spr0E0_Load3Platforms`)                       |
 * | 0xE1-0xE6   | Boos de techo (`Spr0E1_LoadBooCeiling`)                          |
 * | 0xE7-0xFF   | sprite de SCROLL de capas: mueve el Layer 1/2, no se dibuja      |
 *
 * El caso que más se nota es el del estado 9. En YOSHI'S ISLAND 1 y 2 hay caparazones
 * sueltos por el suelo, y en la lista aparecen como 0xDA y 0xDB. Con la traducción:
 *
 * ```
 * 0xDA + 0x2A = 0x04  → Koopa VERDE con caparazón, aturdido = caparazón verde suelto
 * 0xDB + 0x2A = 0x05  → Koopa ROJO  con caparazón, aturdido = caparazón rojo  suelto
 * ```
 *
 * Encaja con que 0x04-0x07 sean los Koopa **con** caparazón (ver `SmwSpriteNames`): un
 * 0x04 aturdido es justo un caparazón verde. Antes esos dos ids se contaban como
 * "enemigos sin gráfico", cuando el gráfico ya existía; lo que faltaba era la traducción.
 *
 * Lo que NO se porta aquí, por ser estado de partida y no dato del nivel: el cambio
 * 4→7 / 5→6 que hace el juego cuando `ow_level_tile_settings[73]` tiene el bit 0x80, y
 * el de la P-switch plateada, que convierte casi todo en moneda (id 33).
 */
object SmwSpriteSpawn {

    /** Estado inicial de un sprite normal recién colocado (`spr_current_status`). */
    const val STATUS_NORMAL = 1

    /** Estado 9: ATURDIDO. Para un Koopa es "metido en el caparazón, quieto en el suelo". */
    const val STATUS_STUNNED = 9

    /** Lo que el juego suma al id crudo cuando lo carga aturdido (`v15 += 42`). */
    const val STUNNED_ID_DELTA = 0x2A

    /** Qué clase de cosa es una entrada de la lista de sprites. */
    enum class Kind {
        /** Un enemigo/objeto normal, con su id y su estado. */
        NORMAL,

        /** Lanzador de balas ($02: `SprXXX_LoadShooter`): no ocupa ranura de enemigo. */
        SHOOTER,

        /** Generador que va soltando enemigos solo (`gen_spr_spriteid`). */
        GENERATOR,

        /** Carga en grupo: cinco Eeries, tres plataformas o los Boos de techo. */
        GROUP,

        /** Sprite de scroll de capas: mueve el fondo, no se dibuja. */
        LAYER_SCROLL,
    }

    /**
     * Resultado de clasificar un id crudo. [id] y [status] solo tienen sentido cuando
     * [kind] es [Kind.NORMAL]; en el resto valen -1 porque no hay enemigo que colocar.
     */
    class Spawn(val kind: Kind, val id: Int = -1, val status: Int = -1) {
        /** true si esto se convierte en un enemigo de verdad dentro del nivel. */
        val isEnemy: Boolean get() = kind == Kind.NORMAL

        /** true si nace ya dentro de su caparazón (estado 9). */
        val stunned: Boolean get() = status == STATUS_STUNNED
    }

    /**
     * Clasifica el id CRUDO leído de la lista de sprites. El orden de las comprobaciones
     * es el del original y **importa**: 0xDE y 0xE0 se atrapan antes que el tramo de
     * aturdidos, así que no se traducen aunque caigan dentro de 0xDA-0xE0.
     */
    fun of(rawId: Int): Spawn {
        val v = rawId and 0xFF
        if (v >= 0xE7) return Spawn(Kind.LAYER_SCROLL)
        if (v == 0xDE || v == 0xE0) return Spawn(Kind.GROUP)
        if (v < 0xCB) {
            if (v >= 0xC9) return Spawn(Kind.SHOOTER)
            return Spawn(Kind.NORMAL, v, STATUS_NORMAL)
        }
        if (v < 0xDA) return Spawn(Kind.GENERATOR)
        if (v >= 0xE1) return Spawn(Kind.GROUP)
        return Spawn(Kind.NORMAL, (v + STUNNED_ID_DELTA) and 0xFF, STATUS_STUNNED)
    }
}
