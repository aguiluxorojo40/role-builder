package com.rolebuilder.core.engine.platformer

import com.rolebuilder.core.snes.SmwPhysics
import com.rolebuilder.core.snes.SmwPowerup

/** Entorno de físicas de un nivel: cambia rozamiento y caída (hielo, agua). */
enum class PlatformerEnvironment { LAND, ICE, WATER }

/**
 * Parámetros de físicas del motor de plataformas, en **píxeles por fotograma** (el
 * motor avanza un fotograma por `tick`, a 60 fps como el juego original).
 *
 * Se puede rellenar a mano o derivar de las tablas REALES de SMW con [fromSmw], para
 * que el tacto (salto, gravedad, tope de caída, velocidad) sea el del juego.
 */
data class PlatformerTuning(
    /** Velocidad Y inicial del salto (negativa = hacia arriba). */
    val jumpSpeed: Float,
    /** Gravedad soltando el botón de salto (px/fotograma²). */
    val gravityFall: Float,
    /** Gravedad manteniendo el botón (menor → salto más alto: salto variable). */
    val gravityHold: Float,
    /** Velocidad de caída máxima (terminal). */
    val maxFallSpeed: Float,
    /** Tope horizontal andando. */
    val maxWalkSpeed: Float,
    /** Tope horizontal corriendo (botón de correr). */
    val maxRunSpeed: Float,
    /** Aceleración horizontal (px/fotograma²). */
    val runAccel: Float,
    /** Deceleración cuando no hay input (rozamiento). */
    val friction: Float,
    /** Caja de colisión del jugador (px). Mario pequeño ≈ 12×15 dentro de 16×16. */
    val playerWidth: Float = 12f,
    val playerHeight: Float = 15f,
) {
    companion object {
        /**
         * Deriva el ajuste de las físicas REALES de SMW ([SmwPhysics]). Las velocidades
         * de la ROM vienen en 1/16 px/fotograma → se dividen entre 16 para píxeles.
         *
         * Exactos de la ROM: salto, gravedad (soltando/manteniendo), caída terminal.
         * La velocidad máxima usa los valores canónicos de la tabla $00:D535 (andar
         * 0x16, correr 0x30). La aceleración/rozamiento son una aproximación razonable
         * (las tablas de aceleración de SMW van indexadas por estado); el tacto lo
         * marcan sobre todo salto, gravedad y topes, que sí son exactos.
         */
        fun fromSmw(
            p: SmwPhysics,
            powerup: SmwPowerup = SmwPowerup.SMALL,
            environment: PlatformerEnvironment = PlatformerEnvironment.LAND,
        ): PlatformerTuning {
            fun sp(v: Int) = v / 16f
            val landAccel = 0x180 / 4096f
            // Hielo: mucho menos rozamiento (Mario patina), usando el hecho de que SMW
            // cambia a la tabla de aceleración de hielo ($00:D43D), más suave.
            val friction = if (environment == PlatformerEnvironment.ICE) landAccel * 0.15f else landAccel
            // Agua: SMW limita la velocidad vertical a ±16 subpíxeles (±1.0 px/fotograma)
            // y la gravedad es suave (nada de salto seco); el resto se atenúa.
            val underwater = environment == PlatformerEnvironment.WATER
            val gravFall = if (underwater) sp(p.gravity.getOrElse(0) { 6 }) * 0.3f else sp(p.gravity.getOrElse(0) { 6 })
            val gravHold = if (underwater) gravFall * 0.5f else sp(p.gravity.getOrElse(1) { 3 })
            val maxFall = if (underwater) sp(0x10) else sp(p.defaultMaxFall) // agua: cap 1.0 px/f
            val jump = if (underwater) sp(0x20) * -1 else sp(p.baseJumpYSpeed) // agua: brazada suave
            // Powerup: Mario grande ocupa dos casillas; pequeño, una.
            val height = if (powerup.heightTiles >= 2) 26f else 14f
            return PlatformerTuning(
                jumpSpeed = jump,
                gravityFall = gravFall,
                gravityHold = gravHold,
                maxFallSpeed = maxFall,
                maxWalkSpeed = sp(0x16),                      //      ->  1.375
                maxRunSpeed = sp(0x30),                       //      ->  3.0
                runAccel = landAccel,                         //      ≈  0.094
                friction = friction,
                playerHeight = height,
            )
        }
    }
}
