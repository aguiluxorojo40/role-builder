package com.rolebuilder.core.snes

import kotlin.math.abs

/**
 * Movimiento HORIZONTAL de Mario, port 1:1 de `HandlePlayerPhysics` (`$00:D5F2`,
 * disassembly IsoFrieze/SMWDisX, versión US/NTSC). Reproduce la rama horizontal tal
 * cual: aceleración por estado, derrape (skid) al cambiar de sentido a velocidad,
 * rozamiento al soltar, andar/correr/despegue, hielo y el medidor-P.
 *
 * ## Unidades
 * SMW guarda la velocidad X en la PALABRA `$7A/$7B`:
 *  - byte ALTO (`$7B`) = velocidad en **1/16 px por fotograma** (con signo);
 *  - byte BAJO (`$7A`) = subvelocidad (acumulador de la aceleración, 1/256 de ese paso).
 * Las tablas de aceleración/rozamiento son WORDS que se SUMAN a esa palabra de 16 bits
 * (p. ej. andar = `$0180` = +1.5 unidades de 1/16 px por fotograma). La posición avanza
 * después con el byte alto usando el mismo acumulador de subpíxeles que los enemigos
 * (`smwStepX`), así las distancias salen bit-exactas.
 *
 * ## Índices (clave)
 * En el ROM los índices `X`/`Y` son offsets en **BYTES** sobre tablas de words. Aquí las
 * tablas de [SmwPhysics] ya vienen como words (un `Int` por entrada), así que un offset
 * de byte `b` corresponde al word `b ushr 1`.
 *
 * ## Alcance
 * Se porta el caso de suelo llano y aire (`SlopeType = 0`): la superficie de las cuestas
 * la resuelve aparte el motor (`snapToSlope`). No se modela aquí el vuelo con capa, llevar
 * objetos ni el salto giratorio (los gestiona el motor). El agua/hielo entran por
 * [Environment], fieles a los flags de nivel `LevelIsWater`/`LevelIsSlippery`.
 */
class SmwPlayerXMovement(private val phys: SmwPhysics) {

    /** Palabra de velocidad X de 16 bits con signo (byte alto = 1/16 px, bajo = subvelocidad). */
    var speedWord: Int = 0
        private set

    /** Sentido actual: 1 = derecha, 0 = izquierda (como `$76 PlayerDirection`). */
    var direction: Int = 1
        private set

    /** Medidor-P (`$13E4`), 0..[PMETER_MAX]. Sube a velocidad de despegue; habilita el vuelo con capa. */
    var pMeter: Int = 0
        private set

    /** ¿Derrapando (girando a velocidad)? Para la pose de giro (`$0D PlayerTurningPose`). */
    var skidding: Boolean = false
        private set

    /** Velocidad en 1/16 px con signo (byte alto de [speedWord]). */
    val speedHi: Int get() = speedWord shr 8

    /** Para a Mario en seco (choque de pared: el motor pone `PlayerXSpeed = 0`). */
    fun stop() { speedWord = 0 }

    /**
     * Un fotograma del manejo horizontal (`CODE_00D5F2`, rama horizontal).
     *
     * @param dir  -1 izquierda, 0 sin dirección, +1 derecha (`byetudlr` left/right).
     * @param running botón de correr (Y) mantenido (`byetudlrHold & $40`).
     * @param onGround en el suelo (`PlayerInAir == 0`).
     * @param onIce nivel resbaladizo (`LevelIsSlippery`): usa la tabla de aceleración de hielo.
     * @param water nivel de agua (`LevelIsWater`): usa la rama de rozamiento acuático.
     */
    fun step(dir: Int, running: Boolean, onGround: Boolean, onIce: Boolean, water: Boolean) {
        skidding = false

        // --- Sin dirección: rozamiento (CODE_00D764). En el aire no hay rozamiento. ---
        if (dir == 0) {
            updatePMeter(0)                 // JSR CODE_00D968: Y=0 -> el medidor decae
            if (!onGround) return           // LDA PlayerInAir; BNE Return00D7A4
            applyFriction(water)
            return
        }

        // --- Dirección mantenida (CODE_00D6B1). ---
        val d = if (dir > 0) 1 else 0       // AND #$01: 1 = derecha, 0 = izquierda
        direction = d                       // STA PlayerDirection (bookkeeping de giro simplificado)

        // Índice de aceleración X (en BYTES, SlopeType = 0): X = dir*4.
        var x = d * 4
        val spHi = speedHi

        // Derrape: velocidad != 0 y de signo OPUESTO a la aceleración del sentido pulsado
        // (EOR MarioAccel_+1,X; BPL). Añade $90 al índice -> tramo de deceleración de giro.
        if (spHi != 0) {
            val accelSign = accelWord(x)    // MarioAccel_[dir] antes de correr/derrape
            if ((spHi xor accelSign) < 0) { // signos opuestos
                if (!onIce) skidding = true // pose de giro (en hielo no hay derrape)
                x += 0x90
            }
        }

        // Escalón de velocidad (CODE_00D713): andar / correr / despegue.
        var tier = 0
        if (running) {                      // BIT byetudlrHold; BVC (bit6 = botón Y)
            x += 2
            tier = 1
            if (abs(spHi) >= RUN_THRESHOLD && onGround) tier = 2  // CMP #$23 -> tramo despegue
        }
        updatePMeter(tier)                  // JSR CODE_00D96A con Y = tier

        // Tope de velocidad (CODE_00D742): Y = tier*2 | dir (BYTES, SlopeType = 0).
        val yIdx = (tier shl 1) or d
        val maxCap = maxSpeed(yIdx)         // DATA_00D535,Y (byte con signo)

        val diff = spHi - maxCap
        // BEQ (en el tope) o mismo signo que el tope (pasado): rozar hacia el tope.
        if (diff == 0 || (diff xor maxCap) >= 0) {
            applyFriction(water)
        } else {
            // Por debajo del tope: acelerar (en hielo y suelo, tabla de hielo D43D).
            val a = if (onIce && onGround) iceAccelWord(x) else accelWord(x)
            speedWord += a
        }
    }

    /** Rozamiento hacia 0 (rama común de CODE_00D76B/D772): resta y clampa al cruzar cero. */
    private fun applyFriction(water: Boolean) {
        // Y' (BYTES): 0 si la velocidad es >= 0, 2 si es negativa (SBC D5C9+1,X; BPL; INY INY).
        val yByte = if (speedHi >= 0) 0 else 2
        // Suelo seco -> D2CD (friction1); agua -> D309 (friction2). (El comentario histórico
        // de SmwPhysics tiene los papeles cambiados; esto sigue al ROM: dry=D2CD, water=D309.)
        val fw = if (water) friction2(yByte) else friction1(yByte)
        val newW = speedWord + fw
        // Clampa a 0 si el rozamiento cruzó el cero (SBC D5C9,X=0; EOR frict; BMI keep; else 0).
        speedWord = if ((newW xor fw) < 0) newW else 0
    }

    /** Medidor-P: += DATA_00D5EB[y] ({-1,-1,+2}), acotado a [0, PMETER_MAX]. */
    private fun updatePMeter(y: Int) {
        pMeter = (pMeter + PMETER_DELTA[y]).coerceIn(0, PMETER_MAX)
    }

    // --- Acceso a tablas: el índice viene en BYTES; las tablas de words usan `b ushr 1`. ---
    private fun accelWord(byteX: Int): Int = wordAt(phys.marioXAccel, byteX)
    private fun iceAccelWord(byteX: Int): Int = wordAt(phys.iceXAccel, byteX)
    private fun friction1(byteY: Int): Int = wordAt(phys.friction1, byteY)
    private fun friction2(byteY: Int): Int = wordAt(phys.friction2, byteY)
    private fun maxSpeed(byteY: Int): Int = phys.maxXSpeed.getOrElse(byteY) { 0 }

    private fun wordAt(table: IntArray, byteOffset: Int): Int = table.getOrElse(byteOffset ushr 1) { 0 }

    companion object {
        /** Umbral de |velocidad| para saltar al tramo de despegue con correr (CMP #$23, US). */
        const val RUN_THRESHOLD = 0x23

        /** Tope del medidor-P en la versión US ($70 = 112). */
        const val PMETER_MAX = 0x70

        /** DATA_00D5EB (US, lores): incremento del medidor-P por tramo {andar,correr,despegue}. */
        val PMETER_DELTA = intArrayOf(-1, -1, 2)
    }
}
