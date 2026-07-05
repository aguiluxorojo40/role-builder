package com.rolebuilder.core.snes

/**
 * Modelos de datos para la extracción de assets de ROMs de Super Nintendo.
 *
 * Toda la lógica de decodificación vive en `core` (Kotlin JVM puro, sin AWT ni
 * dependencias de Android) para poder importar gráficos de ROMs directamente en
 * el dispositivo y probarla con tests rápidos de JVM. La app solo convierte los
 * píxeles ARGB resultantes en un `Bitmap`/PNG dentro del proyecto.
 */

/** Esquema de mapeo del cartucho: dónde vive la cabecera interna de la ROM. */
enum class SnesMapping { LOROM, HIROM, UNKNOWN }

/**
 * Formato planar de los gráficos. El SNES almacena los tiles en planos de bits
 * entrelazados; cada formato usa un número distinto de bits por píxel (bpp) y,
 * por tanto, de bytes por tile de 8×8.
 */
enum class SnesGraphicFormat(val bytesPerTile: Int, val colorCount: Int) {
    /** SNES 2bpp: 16 bytes/tile, 4 colores (fondos sencillos, fuentes). */
    SNES_2BPP(16, 4),
    /** SNES 4bpp: 32 bytes/tile, 16 colores (formato más común de sprites y BG). */
    SNES_4BPP(32, 16),
    /** SNES 8bpp: 64 bytes/tile, 256 colores (modo 7, gráficos de alto color). */
    SNES_8BPP(64, 256),
    /** Game Boy 2bpp: mismo entrelazado que SNES 2bpp. */
    GB_2BPP(16, 4),
    /** NES 2bpp: los dos planos van consecutivos (8 + 8 bytes), no entrelazados. */
    NES_2BPP(16, 4),
}

/**
 * Cabecera interna de registro de la ROM (0x7FC0 en LoROM, 0xFFC0 en HiROM),
 * decodificada con alta fidelidad para mostrar metadatos del cartucho.
 */
data class SnesHeader(
    val title: String,
    val isFastRom: Boolean,
    val romTypeDescription: String,
    val romSizeBytes: Long,
    val sramSizeBytes: Long,
    val country: String,
    val licensee: String,
    val version: Int,
    val checksum: Int,
    val checksumComplement: Int,
    val isChecksumValid: Boolean,
    val mapping: SnesMapping,
    val headerOffset: Int,
)

/**
 * Un tile de 8×8 decodificado a índices de color.
 *
 * @param index índice cronológico del tile respecto a su offset base.
 * @param romOffset offset de origen (en bytes) de los datos del tile en la ROM.
 * @param pixelIndices 64 valores (fila a fila) con el índice de color de cada
 *   píxel; el rango depende del formato (0..3, 0..15 o 0..255).
 */
class DecodedTile(
    val index: Int,
    val romOffset: Int,
    val pixelIndices: IntArray,
) {
    init {
        require(pixelIndices.size == 64) { "Un tile 8x8 tiene 64 píxeles, no ${pixelIndices.size}" }
    }
}

/**
 * Una paleta de colores. Los colores se guardan como ARGB (0xFFRRGGBB) para no
 * depender de AWT; [rawBgr15] conserva los valores originales de 15 bits BGR por
 * si se quieren reexportar sin pérdida.
 */
data class SnesPalette(
    val id: String,
    val name: String,
    val colors: IntArray,
    val rawBgr15: IntArray? = null,
    val offset: Int? = null,
) {
    val size: Int get() = colors.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnesPalette) return false
        return id == other.id && name == other.name &&
            colors.contentEquals(other.colors) && offset == other.offset
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + colors.contentHashCode()
        result = 31 * result + (offset ?: 0)
        return result
    }
}

/**
 * Imagen en memoria: buffer ARGB (0xAARRGGBB) de [width]×[height] píxeles,
 * fila a fila. La app la vuelca a un `Bitmap` de Android; en JVM/tests se puede
 * codificar a PNG con `ImageIO`.
 */
class ArgbImage(val width: Int, val height: Int) {
    val pixels: IntArray = IntArray(width * height)

    fun set(x: Int, y: Int, argb: Int) {
        pixels[y * width + x] = argb
    }

    fun get(x: Int, y: Int): Int = pixels[y * width + x]
}
