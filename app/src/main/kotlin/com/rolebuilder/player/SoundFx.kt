package com.rolebuilder.player

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Efectos de sonido 100% sintetizados: se generan como WAV en la caché la
 * primera vez y se reproducen con SoundPool. Sin assets de audio externos.
 */
class SoundFx(private val context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = mutableMapOf<String, Int>()
    private val loaded = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        NAMES.forEach { register(it) }
    }

    fun play(name: String) {
        val id = ids[name] ?: return
        if (id in loaded) soundPool.play(id, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun release() = soundPool.release()

    private fun register(name: String) {
        val file = File(context.cacheDir, "sfx_$name.wav")
        if (!file.exists()) file.writeBytes(wav(synthesize(name)))
        ids[name] = soundPool.load(file.absolutePath, 1)
    }

    private fun synthesize(name: String): ShortArray = when (name) {
        "attack" -> noise(90, 0.4f)
        "hit" -> tone(220f, 160f, 90, square = true)
        "hurt" -> tone(140f, 90f, 200, square = true)
        "defeat" -> tone(360f, 60f, 280)
        "pickup" -> tone(880f, 1320f, 120)
        "select" -> tone(660f, 660f, 60)
        "chest" -> tone(440f, 880f, 200)
        "heal" -> tone(520f, 780f, 250)
        "shoot" -> tone(500f, 240f, 110, square = true)
        else -> tone(440f, 440f, 80)
    }

    private fun tone(f0: Float, f1: Float, ms: Int, square: Boolean = false): ShortArray {
        val n = SAMPLE_RATE * ms / 1000
        return ShortArray(n) { i ->
            val t = i.toFloat() / n
            val freq = f0 + (f1 - f0) * t
            val phase = 2f * PI.toFloat() * freq * i / SAMPLE_RATE
            var sample = sin(phase.toDouble()).toFloat()
            if (square) sample = if (sample >= 0f) 0.7f else -0.7f
            val envelope = (1f - t) * minOf(1f, i / 80f)
            (sample * envelope * 0.5f * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun noise(ms: Int, fade: Float): ShortArray {
        val n = SAMPLE_RATE * ms / 1000
        val rnd = Random(1)
        return ShortArray(n) { i ->
            val t = i.toFloat() / n
            val envelope = (1f - t * fade) * (1f - t)
            ((rnd.nextFloat() * 2f - 1f) * envelope * 0.4f * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Empaqueta PCM16 mono en un WAV. */
    private fun wav(pcm: ShortArray): ByteArray {
        val dataSize = pcm.size * 2
        val buffer = java.nio.ByteBuffer.allocate(44 + dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(1) // mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        for (sample in pcm) buffer.putShort(sample)
        return buffer.array()
    }

    companion object {
        private const val SAMPLE_RATE = 22050
        private val NAMES = listOf("attack", "hit", "hurt", "defeat", "pickup", "select", "chest", "heal", "shoot")
    }
}
