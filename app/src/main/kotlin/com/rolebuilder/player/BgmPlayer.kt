package com.rolebuilder.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * Música de fondo 100% sintetizada: cada pista de [com.rolebuilder.core.model.BgmTracks]
 * se genera como un loop PCM en memoria y se reproduce con AudioTrack en modo
 * estático con puntos de bucle, sin cortes y sin assets de audio externos.
 *
 * Todas las operaciones de audio se serializan en un executor propio para no
 * bloquear el hilo de UI con la síntesis (~0.1 s por pista, cacheada).
 */
class BgmPlayer {

    private val executor = Executors.newSingleThreadExecutor()
    private val cache = mutableMapOf<String, ShortArray>()
    private var track: AudioTrack? = null
    private var playingName: String = ""
    private var released = false

    @Volatile
    private var requestedName: String = ""

    /** Reproduce [name] en bucle; "" o pista desconocida detiene. Idempotente. */
    fun play(name: String) {
        if (requestedName == name) return
        requestedName = name
        executor.execute {
            if (released || playingName == name) return@execute
            stopTrack()
            playingName = name
            if (name !in TRACKS) return@execute
            val pcm = cache.getOrPut(name) { compose(name) }
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            t.write(pcm, 0, pcm.size)
            t.setLoopPoints(0, pcm.size, -1)
            t.setVolume(VOLUME)
            t.play()
            track = t
        }
    }

    fun stop() = play("")

    /** Silencia al pasar la Activity a segundo plano. */
    fun pause() {
        executor.execute { track?.takeIf { it.playState == AudioTrack.PLAYSTATE_PLAYING }?.pause() }
    }

    fun resume() {
        executor.execute { track?.takeIf { it.playState == AudioTrack.PLAYSTATE_PAUSED }?.play() }
    }

    fun release() {
        executor.execute {
            released = true
            stopTrack()
        }
        executor.shutdown()
    }

    private fun stopTrack() {
        track?.let {
            it.stop()
            it.release()
        }
        track = null
        playingName = ""
    }

    // ------------------------------------------------------------- síntesis

    /**
     * Un compás del loop: acorde de fondo, nota del bajo y melodía en
     * corcheas (8 posiciones; NONE = silencio, HOLD = liga la nota anterior).
     */
    private class Bar(val bass: Int, val chord: IntArray, val melody: IntArray)

    private fun compose(name: String): ShortArray {
        val spec = TRACKS.getValue(name)
        val samplesPerBeat = SAMPLE_RATE * 60 / spec.bpm
        val barSamples = samplesPerBeat * 4
        val eighth = barSamples / 8
        val mix = FloatArray(barSamples * spec.bars.size)

        spec.bars.forEachIndexed { barIndex, bar ->
            val base = barIndex * barSamples

            // Acorde: senos suaves sostenidos todo el compás.
            for (midi in bar.chord) {
                val freq = midiFreq(midi)
                for (i in 0 until barSamples) {
                    val env = fadeInOut(i, barSamples, edge = samplesPerBeat / 4)
                    mix[base + i] += sin(2.0 * PI * freq * i / SAMPLE_RATE).toFloat() * 0.05f * env
                }
            }

            // Bajo: una nota por pulso, onda triangular.
            val bassFreq = midiFreq(bar.bass)
            for (beat in 0 until 4) {
                val start = base + beat * samplesPerBeat
                for (i in 0 until samplesPerBeat) {
                    val env = (1f - i.toFloat() / samplesPerBeat) * minOf(1f, i / 120f)
                    mix[start + i] += triangle(bassFreq, i) * 0.16f * env
                }
            }

            // Melodía: corcheas con la onda principal de la pista.
            var slot = 0
            while (slot < 8) {
                val midi = bar.melody[slot]
                if (midi <= 0) {
                    slot++
                    continue
                }
                var length = 1
                while (slot + length < 8 && bar.melody[slot + length] == HOLD) length++
                val start = base + slot * eighth
                val total = eighth * length
                val freq = midiFreq(midi)
                for (i in 0 until total) {
                    val env = (1f - i.toFloat() / total).pow(0.6f) * minOf(1f, i / 100f)
                    val sample = when (spec.lead) {
                        Lead.SINE -> sin(2.0 * PI * freq * i / SAMPLE_RATE).toFloat()
                        Lead.SQUARE -> if (sin(2.0 * PI * freq * i / SAMPLE_RATE) >= 0) 0.6f else -0.6f
                        Lead.TRIANGLE -> triangle(freq, i)
                    }
                    mix[start + i] += sample * 0.15f * env
                }
                slot += length
            }
        }

        return ShortArray(mix.size) { i ->
            (mix[i].coerceIn(-1f, 1f) * 0.9f * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun triangle(freq: Float, i: Int): Float {
        val phase = (freq * i / SAMPLE_RATE) % 1f
        return 4f * abs(phase - 0.5f) - 1f
    }

    /** Entrada y salida suaves para evitar chasquidos en los bordes. */
    private fun fadeInOut(i: Int, total: Int, edge: Int): Float =
        minOf(1f, i.toFloat() / edge, (total - 1 - i).toFloat() / edge).coerceAtLeast(0f)

    private fun midiFreq(midi: Int): Float = 440f * 2f.pow((midi - 69) / 12f)

    private enum class Lead { SINE, SQUARE, TRIANGLE }

    private class TrackSpec(val bpm: Int, val lead: Lead, val bars: List<Bar>)

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val VOLUME = 0.25f

        /** Liga la corchea anterior. */
        private const val HOLD = -2
        private const val REST = -1

        // Notas MIDI: C4 = 60. Cada pista son 4 compases en bucle.
        private val TRACKS: Map<String, TrackSpec> = mapOf(
            // Do mayor, tranquila: C - F - G - C
            "pueblo" to TrackSpec(
                bpm = 96,
                lead = Lead.SINE,
                bars = listOf(
                    Bar(36, intArrayOf(60, 64, 67), intArrayOf(64, REST, 67, REST, 72, HOLD, 67, REST)),
                    Bar(41, intArrayOf(60, 65, 69), intArrayOf(69, REST, 72, REST, 69, HOLD, 65, REST)),
                    Bar(43, intArrayOf(62, 67, 71), intArrayOf(67, REST, 71, REST, 74, HOLD, 71, REST)),
                    Bar(36, intArrayOf(60, 64, 67), intArrayOf(72, HOLD, 67, REST, 64, HOLD, REST, REST)),
                ),
            ),
            // Sol mayor, aventurera: G - C - D - G
            "campo" to TrackSpec(
                bpm = 132,
                lead = Lead.SQUARE,
                bars = listOf(
                    Bar(43, intArrayOf(62, 67, 71), intArrayOf(67, 71, 74, REST, 71, 74, 79, REST)),
                    Bar(36, intArrayOf(60, 64, 72), intArrayOf(76, REST, 72, 76, 79, HOLD, 76, REST)),
                    Bar(38, intArrayOf(62, 66, 69), intArrayOf(74, 78, 81, REST, 78, 74, 69, REST)),
                    Bar(43, intArrayOf(62, 67, 71), intArrayOf(79, HOLD, 74, REST, 71, HOLD, 67, REST)),
                ),
            ),
            // La menor, lenta y grave: Am - F - E - Am
            "mazmorra" to TrackSpec(
                bpm = 66,
                lead = Lead.TRIANGLE,
                bars = listOf(
                    Bar(33, intArrayOf(57, 60, 64), intArrayOf(69, HOLD, HOLD, REST, 72, HOLD, REST, REST)),
                    Bar(29, intArrayOf(53, 57, 60), intArrayOf(REST, REST, 69, HOLD, 65, HOLD, HOLD, REST)),
                    Bar(28, intArrayOf(52, 56, 59), intArrayOf(68, HOLD, HOLD, REST, 64, HOLD, REST, REST)),
                    Bar(33, intArrayOf(57, 60, 64), intArrayOf(69, HOLD, HOLD, HOLD, REST, REST, REST, REST)),
                ),
            ),
            // Re menor, urgente, ostinato: Dm - Bb - C - A
            "tension" to TrackSpec(
                bpm = 152,
                lead = Lead.SQUARE,
                bars = listOf(
                    Bar(38, intArrayOf(62, 65, 69), intArrayOf(62, 62, 65, 62, 62, 67, 65, 62)),
                    Bar(34, intArrayOf(58, 62, 65), intArrayOf(62, 62, 65, 62, 70, HOLD, 65, 62)),
                    Bar(36, intArrayOf(60, 64, 67), intArrayOf(64, 64, 67, 64, 72, HOLD, 67, 64)),
                    Bar(33, intArrayOf(57, 61, 64), intArrayOf(61, 61, 64, 61, 69, HOLD, 64, 61)),
                ),
            ),
        )
    }
}
