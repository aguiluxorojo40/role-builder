package com.rolebuilder.core.snes

/**
 * FÍSICAS del jugador de Super Mario World, leídas de las tablas REALES de la ROM.
 *
 * SMW guarda cómo Mario acelera, corre, salta, cae, muere y rebota en tablas de
 * bytes dentro del banco $00 (más un par de constantes inmediatas en el código).
 * Aquí se leen tal cual de la ROM, en sus **unidades originales**, para que un
 * motor de plataformas reproduzca el tacto exacto del juego.
 *
 * Unidades (importantes para interpretar los números):
 *  - Velocidad: byte/word con signo en **1/16 de píxel por fotograma**. O sea,
 *    0x10 = 1 px/fotograma; 0x40 = 4 px/fotograma. El juego corre a 60 fps.
 *    → px/fotograma = valor / 16 ;  px/segundo = valor / 16 * 60.
 *  - Aceleración: igual, en 1/16 px por fotograma² (se suma a la subvelocidad).
 *  - Salto/gravedad negativos = hacia ARRIBA (Y crece hacia abajo).
 *
 * Cada tabla anota su dirección SNES ($00:xxxx) y su papel en `HandlePlayerPhysics`
 * ($00:D5F2, vía la recompilación snesrev/smw). Direcciones verificadas contra la
 * ROM US: leídas por [SmwPhysicsReader] con el mapeo LoROM `PC = $00:xxxx - 0x8000`.
 */
class SmwPhysics(
    /** $00:D2BD (16): velocidad Y del salto, por velocidad horizontal y giro. La [0]
     *  es el salto parado: 0xB0 = -80 = -5.0 px/fotograma hacia arriba. */
    val jumpYSpeed: IntArray,
    /** $00:D345 (124 words): aceleración horizontal de Mario por estado (andar/correr,
     *  aire/suelo, sentido). Se suma a la subvelocidad cada fotograma. */
    val marioXAccel: IntArray,
    /** $00:D43D (124 words): la misma aceleración pero en niveles de HIELO (resbala). */
    val iceXAccel: IntArray,
    /** $00:D535 (148): velocidad horizontal MÁXIMA por estado. Valores típicos:
     *  andar 0x16≈1.4, correr 0x24≈2.25, con Meta-P 0x30=3.0 px/fotograma. */
    val maxXSpeed: IntArray,
    /** $00:D5C9 (17 words): límites/ajustes finos de velocidad que acompañan a [maxXSpeed]. */
    val maxXSpeedExtra: IntArray,
    /** $00:D2CD (30 words): tabla de deceleración/rozamiento (rama de aire/agua). */
    val friction1: IntArray,
    /** $00:D309 (30 words): tabla de deceleración/rozamiento (rama de suelo, más suave). */
    val friction2: IntArray,
    /** $00:D7A5 (10): GRAVEDAD por estado de aire; se suma a la velocidad Y cada
     *  fotograma. Con el botón de salto MANTENIDO es menor (salto variable). */
    val gravity: IntArray,
    /** $00:D7AF (10): velocidad de CAÍDA máxima (terminal) por estado. 0x40 = 4.0 px/fotograma. */
    val maxFallSpeed: IntArray,
) {
    /** Velocidad Y del salto parado, en 1/16 px/fotograma (−80 en la ROM US). */
    val baseJumpYSpeed: Int get() = jumpYSpeed.firstOrNull() ?: 0

    /** Gravedad por defecto (sin mantener salto), en 1/16 px/fotograma². */
    val defaultGravity: Int get() = gravity.firstOrNull() ?: 0

    /** Velocidad de caída terminal por defecto, en 1/16 px/fotograma. */
    val defaultMaxFall: Int get() = maxFallSpeed.firstOrNull() ?: 0

    /** Convierte una velocidad en 1/16 px/fotograma a píxeles por fotograma. */
    fun toPixelsPerFrame(subpixelPerFrame: Int): Double = subpixelPerFrame / SUBPIXELS_PER_PIXEL.toDouble()

    /** Convierte una velocidad en 1/16 px/fotograma a píxeles por segundo (60 fps). */
    fun toPixelsPerSecond(subpixelPerFrame: Int): Double =
        subpixelPerFrame / SUBPIXELS_PER_PIXEL.toDouble() * FRAMES_PER_SECOND

    companion object {
        /** Subpíxeles por píxel: las velocidades vienen en 1/16 de píxel. */
        const val SUBPIXELS_PER_PIXEL = 16

        /** Fotogramas por segundo del juego. */
        const val FRAMES_PER_SECOND = 60

        /**
         * Impulso hacia arriba al MORIR ($00:F606 `DamagePlayer_Kill`, LDA #$90):
         * Mario da un salto de -112 (−7.0 px/fotograma) antes de caer fuera de pantalla.
         * Es un inmediato del código, no una tabla; se documenta por completitud.
         */
        const val DEATH_POP_YSPEED = -112

        /** Rebote al PISAR un enemigo con el salto MANTENIDO ($01:AA33 `BoostMarioSpeed`). */
        const val STOMP_BOUNCE_JUMP_HELD = -88

        /** Rebote al PISAR un enemigo SIN mantener el salto ($01:AA33). */
        const val STOMP_BOUNCE = -48
    }
}
