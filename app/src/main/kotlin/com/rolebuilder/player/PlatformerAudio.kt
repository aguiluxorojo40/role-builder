package com.rolebuilder.player

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.rolebuilder.core.snes.SmwSfxCatalog
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File

/**
 * Efectos de sonido del motor de plataformas con las MUESTRAS REALES de Super Mario
 * World. [SmwSfxCatalog] resuelve, a partir de la ROM, qué muestra BRR y a qué tono
 * usa cada evento (salto, moneda, pisotón…) y entrega un clip PCM ya re-muestreado.
 * Aquí los empaquetamos como WAV en caché y los reproducimos con [SoundPool] —el
 * mismo camino probado que usa el RPG en [SoundFx]—, que da baja latencia y permite
 * solapar disparos.
 *
 * La MUERTE de Mario en SMW es un jingle de música, no una muestra suelta; como
 * aproximación reproducimos el "pisotón" a un tono más grave (parámetro `rate` de
 * SoundPool), lo que suena como un golpe descendente.
 */
class PlatformerAudio private constructor(
    context: Context,
    wavByEvent: Map<SmwSfxCatalog.Event, ByteArray>,
) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = mutableMapOf<SmwSfxCatalog.Event, Int>()
    private val loaded = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        for ((event, wavBytes) in wavByEvent) {
            val file = File(context.cacheDir, "smwsfx_${event.name.lowercase()}.wav")
            file.writeBytes(wavBytes)
            ids[event] = soundPool.load(file.absolutePath, 1)
        }
    }

    /** Reproduce el efecto del [event] a [rate] (1.0 = tono tal cual; <1 más grave). */
    fun play(event: SmwSfxCatalog.Event, volume: Float = 0.7f, rate: Float = 1f) {
        val id = ids[event] ?: return
        if (id in loaded) soundPool.play(id, volume, volume, 1, 0, rate)
    }

    /** Muerte de Mario: "pisotón" grave como sustituto del jingle de muerte. */
    fun playDeath() = play(SmwSfxCatalog.Event.STOMP, volume = 0.8f, rate = 0.55f)

    fun release() = soundPool.release()

    companion object {
        /**
         * Construye el audio a partir de los bytes de la ROM de SMW. Devuelve `null`
         * si la ROM no es SMW vanilla o no se puede resolver ningún efecto (en ese
         * caso el juego simplemente corre sin sonido de plataformas).
         */
        fun fromRom(context: Context, rom: ByteArray): PlatformerAudio? {
            val clips = runCatching {
                val header = SnesDecoder.parseHeader(rom)
                SmwSfxCatalog.build(rom, header)
            }.getOrNull() ?: return null
            if (clips.isEmpty()) return null
            val wavs = clips.mapValues { (_, clip) -> wav(clip.pcm, clip.sampleRate) }
            return PlatformerAudio(context, wavs)
        }

        /**
         * Construye el audio desde los efectos EMPAQUETADOS (assets/sfx/<event>.wav,
         * horneados de la ROM). Es la vía para los proyectos de plataformas, que se
         * juegan sin ROM. Devuelve `null` si no hay ningún efecto empaquetado.
         */
        fun fromAssets(context: Context): PlatformerAudio? {
            val wavs = mutableMapOf<SmwSfxCatalog.Event, ByteArray>()
            for (event in SmwSfxCatalog.Event.values()) {
                runCatching {
                    context.assets.open("sfx/${event.name.lowercase()}.wav").use { it.readBytes() }
                }.onSuccess { wavs[event] = it }
            }
            if (wavs.isEmpty()) return null
            return PlatformerAudio(context, wavs)
        }

        /** Empaqueta PCM16 mono a [sampleRate] Hz en un contenedor WAV. */
        private fun wav(pcm: ShortArray, sampleRate: Int): ByteArray {
            val dataSize = pcm.size * 2
            val buffer = java.nio.ByteBuffer.allocate(44 + dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray())
            buffer.putInt(36 + dataSize)
            buffer.put("WAVE".toByteArray())
            buffer.put("fmt ".toByteArray())
            buffer.putInt(16)
            buffer.putShort(1) // PCM
            buffer.putShort(1) // mono
            buffer.putInt(sampleRate)
            buffer.putInt(sampleRate * 2)
            buffer.putShort(2)
            buffer.putShort(16)
            buffer.put("data".toByteArray())
            buffer.putInt(dataSize)
            for (sample in pcm) buffer.putShort(sample)
            return buffer.array()
        }
    }
}
