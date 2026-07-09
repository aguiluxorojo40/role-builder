package com.rolebuilder.core.snes

/**
 * Lee las tablas de físicas del jugador de una ROM de Super Mario World y las
 * devuelve como un [SmwPhysics]. No adivina: usa las direcciones REALES del banco
 * $00 (verificadas contra la ROM US) con el mapeo LoROM `PC = $00:dirección - 0x8000`,
 * más el delta de una posible cabecera de copiador SMC.
 *
 * Toda la aritmética es Kotlin puro (sin AWT ni Android): corre igual en el móvil,
 * en el escritorio y en los tests.
 */
object SmwPhysicsReader {

    // Direcciones SNES ($00:xxxx) de cada tabla y su tamaño (elementos).
    private const val JUMP_ADDR = 0xD2BD; private const val JUMP_LEN = 16
    private const val FRICTION1_ADDR = 0xD2CD; private const val FRICTION1_LEN = 30 // words
    private const val FRICTION2_ADDR = 0xD309; private const val FRICTION2_LEN = 30 // words
    private const val ACCEL_ADDR = 0xD345; private const val ACCEL_LEN = 124 // words
    private const val ICE_ACCEL_ADDR = 0xD43D; private const val ICE_ACCEL_LEN = 124 // words
    private const val MAX_SPEED_ADDR = 0xD535; private const val MAX_SPEED_LEN = 148
    private const val MAX_SPEED_EXTRA_ADDR = 0xD5C9; private const val MAX_SPEED_EXTRA_LEN = 17 // words
    private const val GRAVITY_ADDR = 0xD7A5; private const val GRAVITY_LEN = 10
    private const val MAX_FALL_ADDR = 0xD7AF; private const val MAX_FALL_LEN = 10

    /** Firma de cordura: el salto parado de la ROM vanilla US es 0xB0 (−80). */
    private const val VANILLA_JUMP0 = 0xB0

    /** LoROM: banco $00, dirección >= 0x8000 → PC = dirección − 0x8000 (+ delta SMC). */
    private fun pc(snesAddr: Int, delta: Int): Int = snesAddr - 0x8000 + delta

    /**
     * Extrae las físicas del jugador de [rom]. Devuelve null si la ROM no alcanza a
     * cubrir las tablas o si no parece SMW vanilla en esas direcciones (p. ej. un hack
     * que las haya reubicado): en ese caso las cifras no serían fiables.
     */
    fun read(rom: ByteArray, header: SnesHeader): SmwPhysics? {
        val delta = header.headerOffset - 0x7FC0

        fun bytesSigned(addr: Int, n: Int): IntArray? {
            val base = pc(addr, delta)
            if (base < 0 || base + n > rom.size) return null
            return IntArray(n) { rom[base + it].toByte().toInt() }
        }

        fun wordsSigned(addr: Int, n: Int): IntArray? {
            val base = pc(addr, delta)
            if (base < 0 || base + 2 * n > rom.size) return null
            return IntArray(n) {
                val lo = rom[base + 2 * it].toInt() and 0xFF
                val hi = rom[base + 2 * it + 1].toInt() and 0xFF
                ((hi shl 8) or lo).toShort().toInt()
            }
        }

        val jump = bytesSigned(JUMP_ADDR, JUMP_LEN) ?: return null
        // Cordura: si el salto parado no es el de SMW vanilla, no leemos basura.
        if ((jump[0] and 0xFF) != VANILLA_JUMP0) return null

        return SmwPhysics(
            jumpYSpeed = jump,
            marioXAccel = wordsSigned(ACCEL_ADDR, ACCEL_LEN) ?: return null,
            iceXAccel = wordsSigned(ICE_ACCEL_ADDR, ICE_ACCEL_LEN) ?: return null,
            maxXSpeed = bytesSigned(MAX_SPEED_ADDR, MAX_SPEED_LEN) ?: return null,
            maxXSpeedExtra = wordsSigned(MAX_SPEED_EXTRA_ADDR, MAX_SPEED_EXTRA_LEN) ?: return null,
            friction1 = wordsSigned(FRICTION1_ADDR, FRICTION1_LEN) ?: return null,
            friction2 = wordsSigned(FRICTION2_ADDR, FRICTION2_LEN) ?: return null,
            gravity = bytesSigned(GRAVITY_ADDR, GRAVITY_LEN) ?: return null,
            maxFallSpeed = bytesSigned(MAX_FALL_ADDR, MAX_FALL_LEN) ?: return null,
        )
    }
}
