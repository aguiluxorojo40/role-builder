package com.rolebuilder.core.snes

import java.io.ByteArrayOutputStream

/**
 * Codificador WAV mínimo: PCM de 16 bits con signo, mono. Es lo justo para poder **descargar**
 * los sonidos que la app extrae de la ROM ([SmwSfxCatalog] entrega PCM), sin depender de
 * Android — así el catálogo de extracción produce el fichero entero desde `core`.
 */
object Wav {

    /** Envuelve [pcm] (mono, 16-bit con signo) a [sampleRate] Hz en un fichero WAV completo. */
    fun encode(pcm: ShortArray, sampleRate: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val dataBytes = pcm.size * 2
        val byteRate = sampleRate * 2 // mono, 2 bytes por muestra

        fun str(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun le16(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
        fun le32(v: Int) {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
        }

        str("RIFF"); le32(36 + dataBytes); str("WAVE")
        str("fmt "); le32(16); le16(1); le16(1)       // PCM, 1 canal
        le32(sampleRate); le32(byteRate); le16(2); le16(16) // block align 2, 16 bits
        str("data"); le32(dataBytes)
        for (s in pcm) le16(s.toInt() and 0xFFFF)
        return out.toByteArray()
    }
}
