package com.rolebuilder.core.engine.platformer

import com.rolebuilder.core.snes.SmwPhysics

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
        fun fromSmw(p: SmwPhysics): PlatformerTuning {
            fun sp(v: Int) = v / 16f
            return PlatformerTuning(
                jumpSpeed = sp(p.baseJumpYSpeed),              // -80  -> -5.0
                gravityFall = sp(p.gravity.getOrElse(0) { 6 }),   // 6   ->  0.375
                gravityHold = sp(p.gravity.getOrElse(1) { 3 }),   // 3   ->  0.1875
                maxFallSpeed = sp(p.defaultMaxFall),          // 0x40 ->  4.0
                maxWalkSpeed = sp(0x16),                      //      ->  1.375
                maxRunSpeed = sp(0x30),                       //      ->  3.0
                runAccel = 0x180 / 4096f,                     //      ≈  0.094
                friction = 0x180 / 4096f,
            )
        }
    }
}
