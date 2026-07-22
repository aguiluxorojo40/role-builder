package com.rolebuilder.core.snes

import kotlin.math.abs

/**
 * Puntería de sprite hacia el jugador, portada 1:1 de `AimTowardsPlayer_Bank23` ($02:D2FB) del
 * disassembly (snesrev/smw). La usa, entre otros, la bola de fuego de Reznor
 * (`Spr0A9_Reznor_ReznorFireRt`, con velocidad 0x10).
 *
 * Devuelve el vector de velocidad en UNIDADES de SMW (1/16 px por fotograma): el eje DOMINANTE
 * recibe la magnitud [speed] y el otro una fracción proporcional (reparto tipo Bresenham entero),
 * con el signo hacia el jugador. Igual que el juego, las magnitudes se toman del byte bajo del
 * delta (válido a corta distancia, que es cuando el jefe dispara).
 */
object SmwSpriteAim {
    /**
     * @param dxPx  playerX − spriteX en píxeles (con signo).
     * @param dyPx  playerY − spriteY en píxeles (con signo).
     * @param speed magnitud del eje dominante en unidades SMW (p.ej. 0x10 = 1 px/f para Reznor).
     * @return (xSpeed, ySpeed) en unidades SMW (1/16 px/f).
     */
    fun aimTowardsPlayer(dxPx: Int, dyPx: Int, speed: Int): Pair<Int, Int> {
        val adx = abs(dxPx) and 0xFF          // r13 = |Δx| (byte, como el juego)
        val ady = abs(dyPx) and 0xFF          // r12 = |Δy|
        // r13 debe ser el eje MAYOR; si Δy manda, se intercambian (v4=1).
        val swapped = adx < ady
        val major = if (swapped) ady else adx
        val minor = if (swapped) adx else ady
        // Reparto entero (Bresenham) de [speed] pasos: r0 ≈ speed·minor/major.
        var acc = 0
        var r0 = 0
        repeat(speed) {
            acc += minor
            if (acc >= major) { acc -= major; r0++ }
        }
        // Sin intercambio: X = eje mayor (speed), Y = fracción. Con intercambio, al revés.
        var xs = if (swapped) r0 else speed
        var ys = if (swapped) speed else r0
        if (dxPx < 0) xs = -xs               // jugador a la izquierda
        if (dyPx < 0) ys = -ys               // jugador arriba
        return Pair(xs, ys)
    }
}
