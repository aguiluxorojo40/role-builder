package com.rolebuilder.core.snes

/**
 * COMPORTAMIENTO de un sprite (enemigo/entidad) de Super Mario World: los seis
 * bytes "tweaker" que definen cómo se mueve, choca, se le pisa y reacciona.
 *
 * La lista de sprites de un nivel ([SmwSprites]) dice QUIÉN aparece y DÓNDE; esto
 * dice CÓMO se comporta cada tipo. SMW guarda, por cada id de sprite (0x00..0xC8),
 * seis bytes de propiedades que el motor copia a la RAM del sprite al aparecer
 * (`InitializeNormalSpriteRAMTables`, $07:F7A0, vía snesrev/smw). Son las mismas
 * seis "tweaker bytes" que edita Lunar Magic.
 *
 * Cada byte guarda su papel y su dirección RAM histórica ($7E:16xx):
 *  - [b1656]: banderas varias (interacción con el jugador, objetos, estrella…).
 *  - [b1662]: en los bits 0-5, el ÍNDICE DE HITBOX del sprite ([spriteClipping]);
 *    el resto, banderas.
 *  - [b166e]: banderas de proceso (fuera de pantalla, dirección inicial…).
 *  - [b167a]: banderas de interacción (con el jugador, capa, otros sprites…).
 *  - [b1686]: más banderas (gravedad, no interacción con objetos…).
 *  - [b190f]: banderas extra (inmune a algo, comportamiento especial…).
 *
 * Se exponen los seis bytes CRUDOS —el dato fiel de la ROM— y se decodifica solo el
 * campo que el propio código del juego usa sin ambigüedad como hitbox ([spriteClipping]).
 * El resto de bits los interpreta quien quiera un motor de comportamiento completo.
 */
class SmwSpriteBehavior(
    val id: Int,
    val b1656: Int,
    val b1662: Int,
    val b166e: Int,
    val b167a: Int,
    val b1686: Int,
    val b190f: Int,
) {
    /** Índice de hitbox del sprite (bits 0-5 de [b1662]); el juego lo usa para el
     *  tamaño/forma de colisión con el jugador y otros sprites. */
    val spriteClipping: Int get() = b1662 and 0x3F
}

/**
 * Lee las seis tablas "tweaker" de comportamiento de sprites de una ROM de SMW
 * ($07:F26C..$07:F659, 201 bytes cada una, indexadas por id de sprite) con el mapeo
 * LoROM. Devuelve una lista de 201 [SmwSpriteBehavior] (índice = id de sprite) o
 * null si la ROM no cubre las tablas o no parece SMW vanilla en esas direcciones.
 */
object SmwSpriteBehaviorReader {

    private const val COUNT = 201 // ids de sprite 0x00..0xC8

    // Direcciones SNES ($07:xxxx) de cada tabla de 201 bytes.
    private const val ADDR_1656 = 0xF26C
    private const val ADDR_1662 = 0xF335
    private const val ADDR_166E = 0xF3FE
    private const val ADDR_167A = 0xF4C7
    private const val ADDR_1686 = 0xF590
    private const val ADDR_190F = 0xF659
    private const val BANK = 0x07

    /** Firma de cordura: la tabla $1656 vanilla empieza con cuatro 0x70. */
    private const val VANILLA_1656_0 = 0x70

    /** LoROM: PC = banco*0x8000 + (dirección & 0x7FFF) + delta SMC. */
    private fun pc(bank: Int, snesAddr: Int, delta: Int): Int = bank * 0x8000 + (snesAddr and 0x7FFF) + delta

    fun read(rom: ByteArray, header: SnesHeader): List<SmwSpriteBehavior>? {
        val delta = header.headerOffset - 0x7FC0

        fun table(addr: Int): IntArray? {
            val base = pc(BANK, addr, delta)
            if (base < 0 || base + COUNT > rom.size) return null
            return IntArray(COUNT) { rom[base + it].toInt() and 0xFF }
        }

        val t1656 = table(ADDR_1656) ?: return null
        if (t1656[0] != VANILLA_1656_0) return null
        val t1662 = table(ADDR_1662) ?: return null
        val t166e = table(ADDR_166E) ?: return null
        val t167a = table(ADDR_167A) ?: return null
        val t1686 = table(ADDR_1686) ?: return null
        val t190f = table(ADDR_190F) ?: return null

        return List(COUNT) { i ->
            SmwSpriteBehavior(i, t1656[i], t1662[i], t166e[i], t167a[i], t1686[i], t190f[i])
        }
    }
}
