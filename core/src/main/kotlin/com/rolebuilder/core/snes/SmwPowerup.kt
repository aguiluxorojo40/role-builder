package com.rolebuilder.core.snes

/**
 * Estados de powerup de Mario en Super Mario World y lo que cambian de su juego.
 *
 * SMW guarda el powerup en `player_current_power_up` (0-3) y ramifica el
 * comportamiento por su valor (tamaño de la caja, ataques, romper bloques…). Aquí
 * se recoge de forma fiel a cómo lo trata el motor (`HandlePlayerPhysics`,
 * `CheckPowerUpSpecificPlayerAttacks` $00:D062, vía snesrev/smw):
 *
 *  - [SMALL] (0): un tile de alto, no se agacha, no rompe bloques.
 *  - [BIG] (1): dos tiles de alto (seta), se agacha y rompe bloques de giro.
 *  - [CAPE] (2): como grande + planea/vuela y da capirotazo con la capa.
 *  - [FIRE] (3): como grande + lanza bolas de fuego.
 *
 * El salto NO cambia con el powerup (la tabla de salto se indexa por velocidad y
 * giro, no por powerup); lo que cambia es el TAMAÑO de la caja y las habilidades.
 */
enum class SmwPowerup(
    val id: Int,
    /** Alto de la caja de colisión en casillas de 16 px (1 = pequeño, 2 = grande). */
    val heightTiles: Int,
    val canDuck: Boolean,
    val canBreakBlocks: Boolean,
    val canShootFire: Boolean,
    val canFlyWithCape: Boolean,
) {
    SMALL(0, 1, canDuck = false, canBreakBlocks = false, canShootFire = false, canFlyWithCape = false),
    BIG(1, 2, canDuck = true, canBreakBlocks = true, canShootFire = false, canFlyWithCape = false),
    CAPE(2, 2, canDuck = true, canBreakBlocks = true, canShootFire = false, canFlyWithCape = true),
    FIRE(3, 2, canDuck = true, canBreakBlocks = true, canShootFire = true, canFlyWithCape = false);

    /** Al recibir un golpe, a qué powerup pasa (grande/capa/fuego → pequeño; pequeño muere). */
    val downgrade: SmwPowerup? get() = if (this == SMALL) null else SMALL

    companion object {
        fun of(id: Int): SmwPowerup = entries.firstOrNull { it.id == id } ?: SMALL
    }
}
