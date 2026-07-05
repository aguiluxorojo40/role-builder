package com.rolebuilder.core.snes.compression

/**
 * Framework de descompresión de assets de SNES.
 *
 * No existe UN único formato de compresión en SNES: cada juego/editora usó el
 * suyo (LZSS/LZ77, RLE, LC_LZ2 de Lunar Compress, formatos de Square, Konami,
 * HAL…). Por eso el diseño es un **motor común + un códec por formato**: se
 * añaden códecs de uno en uno y un juego funciona cuando su códec está soportado.
 *
 * La validación de "¿ha funcionado la descompresión?" se hace con el detector de
 * coherencia ([com.rolebuilder.core.snes.SnesGraphicsScanner]): si la salida
 * descomprimida "parece un dibujo", el códec era el correcto. Ese lazo permite
 * incluso **autodetectar** el códec probando cada uno y quedándose con el que
 * produce gráficos reales.
 */
interface SnesDecompressor {
    /** Nombre legible del formato (para menús y logs). */
    val name: String

    /**
     * Descomprime desde [offset] en [rom]. Devuelve los bytes descomprimidos y
     * cuántos bytes comprimidos se consumieron. Lanza [DecompressException] si el
     * flujo es inválido para este formato (para que la autodetección lo descarte).
     *
     * @param maxOutput cota superior de bytes de salida (protección ante bucles).
     */
    fun decompress(rom: ByteArray, offset: Int, maxOutput: Int = DEFAULT_MAX_OUTPUT): DecompressResult

    companion object {
        const val DEFAULT_MAX_OUTPUT = 1 shl 20 // 1 MiB de salida como tope razonable
    }
}

/** Resultado de una descompresión correcta. */
class DecompressResult(
    /** Bytes descomprimidos, listos para pasar al decodificador de tiles. */
    val data: ByteArray,
    /** Bytes leídos de la fuente comprimida (por si se quiere seguir tras el bloque). */
    val consumedBytes: Int,
)

/** El flujo no es válido para el códec que lo intentó descomprimir. */
class DecompressException(message: String) : Exception(message)
