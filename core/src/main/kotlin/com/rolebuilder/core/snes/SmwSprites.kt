package com.rolebuilder.core.snes

/**
 * Parser de la LISTA DE SPRITES (enemigos/entidades) de un nivel de Super Mario
 * World: la TERCERA capa de datos de un nivel, independiente de Layer 1 (objetos de
 * primer plano) y Layer 2 (fondo). Dice dónde aparece cada Koopa, Goomba, planta
 * Piraña, plataforma, bloque saltarín, etc. Hasta ahora no se extraía nada de ella,
 * así que las escenas reconstruían el escenario pero no "quién lo habita".
 *
 * Formato (estable y documentado 1:1 en SnesLab / SMW Central / Lunar Magic):
 *  - Tabla de punteros de 2 bytes por nivel en $05:EC00 (PC [PTR_TABLE_PC]); el
 *    puntero es una dirección de 16 bits dentro del banco $07 (PC base
 *    [DATA_BANK_PC]) donde vive el flujo de sprites del nivel (en ROMs vanilla el
 *    banco es fijo; Lunar Magic puede reubicarlo).
 *  - El flujo es: 1 byte de CABECERA (ajuste del buffer de sprites; no posiciona) +
 *    entradas de 3 bytes, terminadas por 0xFF.
 *
 * Cada entrada (b1 b2 b3):
 *   b1 = yyyy EE s y     b2 = XXXX SSSS     b3 = NNNNNNNN
 *     Y (0..31, teselas 16px) = (y<<4) | yyyy      → ((b1 and 1) shl 4) or (b1 shr 4)
 *     bits extra              = EE                 → (b1 shr 2) and 3
 *     pantalla (0..31)        = (s<<4) | SSSS      → (((b1 shr 1) and 1) shl 4) or (b2 and 0xF)
 *     X (teselas 16px)        = pantalla*16 + XXXX → pantalla*16 + (b2 shr 4)
 *     id del sprite           = b3
 *
 * Todo es Kotlin puro y sin ROM con copyright en los tests: se planta un flujo
 * mínimo y se comprueba el decodificado bit a bit.
 */
internal object SmwSprites {

    /** $05:EC00 → PC. 512 niveles (0x000..0x1FF) × 2 bytes (low/high). */
    internal const val PTR_TABLE_PC = 0x2EC00

    /** Banco $07 (base PC): donde vive el flujo de datos de sprites en ROMs vanilla. */
    private const val DATA_BANK_PC = 0x38000

    private const val TERMINATOR = 0xFF

    /** El motor real tiene ~0x80 slots; cota de seguridad ante punteros corruptos. */
    private const val MAX_SPRITES = 128

    /** Un sprite colocado: posición en teselas de 16px, id y metadatos. */
    internal class SmwSprite(
        val xTile: Int,
        val yTile: Int,
        val id: Int,
        val extraBits: Int,
        val screen: Int,
    )

    /** La lista de sprites del nivel más su byte de cabecera. */
    internal class SmwSpriteList(val header: Int, val sprites: List<SmwSprite>)

    /**
     * Parsea la lista de sprites del [level]. Devuelve null si no hay puntero válido
     * o el flujo está corrupto (sin terminador dentro de la cota).
     */
    fun parse(rom: ByteArray, delta: Int, level: Int): SmwSpriteList? {
        val ptr = PTR_TABLE_PC + delta + 2 * level
        if (ptr < 0 || ptr + 1 >= rom.size) return null
        val addr = (rom[ptr].toInt() and 0xFF) or ((rom[ptr + 1].toInt() and 0xFF) shl 8)
        if (addr < 0x8000) return null
        var p = DATA_BANK_PC + delta + (addr - 0x8000)
        if (p !in rom.indices) return null

        val header = rom[p].toInt() and 0xFF
        p++
        val list = ArrayList<SmwSprite>()
        while (list.size < MAX_SPRITES) {
            if (p >= rom.size) return null // flujo sin terminador: datos corruptos
            val b1 = rom[p].toInt() and 0xFF
            if (b1 == TERMINATOR) return SmwSpriteList(header, list)
            if (p + 2 >= rom.size) return null
            val b2 = rom[p + 1].toInt() and 0xFF
            val b3 = rom[p + 2].toInt() and 0xFF
            p += 3
            val y = ((b1 and 0x01) shl 4) or (b1 shr 4)
            val extra = (b1 shr 2) and 0x03
            val screen = (((b1 shr 1) and 0x01) shl 4) or (b2 and 0x0F)
            val x = screen * 16 + (b2 shr 4)
            list.add(SmwSprite(x, y, b3, extra, screen))
        }
        return SmwSpriteList(header, list)
    }
}
