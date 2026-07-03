package com.rolebuilder.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * Música de fondo 100% procedural: pistas chiptune definidas como patrones
 * de notas y sintetizadas en streaming con AudioTrack (melodía de onda
 * cuadrada + bajo triangular). Sin assets de audio.
 */
class MusicPlayer {

    @Volatile
    private var current: String? = null
    private var worker: Thread? = null

    /** Reproduce una pista en bucle; si ya suena, no hace nada. */
    @Synchronized
    fun play(track: String?) {
        if (track == current) return
        stopInternal()
        if (track == null || track !in TRACKS) return
        current = track
        worker = thread(name = "rb-bgm", isDaemon = true) { runLoop(track) }
    }

    @Synchronized
    fun stop() = stopInternal()

    fun release() = stop()

    private fun stopInternal() {
        current = null
        worker?.join(300)
        worker = null
    }

    private fun runLoop(track: String) {
        val def = TRACKS.getValue(track)
        val audio = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(
                maxOf(
                    AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
                    SAMPLE_RATE / 2,
                ) * 2,
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val stepSamples = (SAMPLE_RATE * 30.0 / def.bpm).toInt() // corchea
        val buffer = ShortArray(stepSamples)
        var melodyPhase = 0.0
        var bassPhase = 0.0
        var melodyFreq = 0.0
        var bassFreq = 0.0
        var step = 0

        try {
            audio.play()
            while (current == track) {
                val melodyNote = def.melody[step % def.melody.size]
                val bassNote = def.bass[(step / 2) % def.bass.size]
                val retrigger = melodyNote != HOLD
                if (melodyNote != REST && melodyNote != HOLD) melodyFreq = freq(melodyNote)
                if (melodyNote == REST) melodyFreq = 0.0
                if (bassNote != REST && bassNote != HOLD) bassFreq = freq(bassNote)
                if (bassNote == REST) bassFreq = 0.0

                for (i in 0 until stepSamples) {
                    val t = i.toFloat() / stepSamples
                    var sample = 0f
                    if (melodyFreq > 0) {
                        melodyPhase += melodyFreq / SAMPLE_RATE
                        // Onda cuadrada con envolvente por nota (retrigger) o sostenida.
                        val envelope = if (retrigger) (1f - 0.55f * t) else 0.55f
                        sample += (if ((melodyPhase % 1.0) < 0.25) 0.9f else -0.9f) * envelope * 0.32f
                    }
                    if (bassFreq > 0) {
                        bassPhase += bassFreq / SAMPLE_RATE
                        // Onda triangular suave para el bajo.
                        val tri = (4.0 * abs((bassPhase % 1.0) - 0.5) - 1.0).toFloat()
                        sample += tri * 0.26f
                    }
                    // Vibrato muy sutil global para dar vida.
                    val vibrato = 1f + 0.004f * sin(2.0 * PI * 5.0 * (step * stepSamples + i) / SAMPLE_RATE).toFloat()
                    buffer[i] = (sample * vibrato * Short.MAX_VALUE * 0.55f).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                audio.write(buffer, 0, buffer.size)
                step = (step + 1) % def.length
            }
        } finally {
            runCatching {
                audio.stop()
                audio.release()
            }
        }
    }

    private fun freq(semitonesFromA4: Int): Double = 440.0 * 2.0.pow(semitonesFromA4 / 12.0)

    private class TrackDef(val bpm: Int, val melody: IntArray, val bass: IntArray) {
        val length: Int = melody.size
    }

    companion object {
        private const val SAMPLE_RATE = 22050

        /** Silencio. */
        private const val REST = Int.MIN_VALUE

        /** Sostiene la nota anterior sin re-atacarla. */
        private const val HOLD = Int.MAX_VALUE

        private val R = REST
        private val H = HOLD

        // Notas en semitonos desde A4 (0 = La 440). -12 = La3, 3 = Do5...
        private val TRACKS: Map<String, TrackDef> = mapOf(
            // Título: arpegio tranquilo Am - F - C - G.
            "title" to TrackDef(
                bpm = 92,
                melody = intArrayOf(
                    0, 3, 7, 12, 7, 3, 0, H,
                    -4, 0, 5, 8, 5, 0, -4, H,
                    -9, -5, 0, 3, 0, -5, -9, H,
                    -2, 2, 5, 10, 5, 2, -2, H,
                ),
                bass = intArrayOf(-24, -24, -28, -28, -33, -33, -26, -26),
            ),
            // Campo: alegre en Do mayor.
            "field" to TrackDef(
                bpm = 144,
                melody = intArrayOf(
                    3, H, 7, 3, 10, H, 7, 3,
                    5, H, 8, 5, 12, H, 10, 8,
                    3, H, 7, 3, 10, H, 12, 15,
                    14, 12, 10, 8, 7, 5, 3, H,
                ),
                bass = intArrayOf(-21, -21, -17, -14, -19, -19, -16, -12),
            ),
            // Aldea: melodía dulce y pausada.
            "village" to TrackDef(
                bpm = 104,
                melody = intArrayOf(
                    7, H, H, 5, 3, H, 5, 7,
                    8, H, 7, 5, 3, H, R, R,
                    5, H, H, 3, 2, H, 3, 5,
                    7, H, 5, 3, 0, H, R, R,
                ),
                bass = intArrayOf(-21, -26, -24, -28, -21, -26, -19, -24),
            ),
            // Mazmorra: oscura, en menor y lenta.
            "dungeon" to TrackDef(
                bpm = 84,
                melody = intArrayOf(
                    0, R, 1, R, 0, R, -4, R,
                    -2, R, 0, R, -2, R, -7, R,
                    0, R, 1, R, 3, R, 1, R,
                    0, R, -2, R, -4, H, R, R,
                ),
                bass = intArrayOf(-33, -32, -33, -36, -33, -32, -35, -36),
            ),
            // Batalla: rápida y tensa.
            "battle" to TrackDef(
                bpm = 172,
                melody = intArrayOf(
                    0, 0, 3, 0, 5, 3, 7, 5,
                    0, 0, 3, 0, 8, 7, 5, 3,
                    1, 1, 5, 1, 8, 5, 10, 8,
                    12, 10, 8, 7, 5, 3, 1, 0,
                ),
                bass = intArrayOf(-24, -24, -23, -23, -21, -21, -23, -16),
            ),
        )
    }
}
