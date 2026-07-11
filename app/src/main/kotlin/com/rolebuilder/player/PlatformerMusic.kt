package com.rolebuilder.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.rolebuilder.core.snes.SmwMusicRenderer

/**
 * Música de fondo del motor de plataformas con el MOTOR REAL de SMW: reproduce la
 * canción renderizando en tiempo real el N-SPC (secuenciador) + S-DSP portados a
 * Kotlin ([SmwMusicRenderer]) y volcando el PCM a un [AudioTrack] en streaming.
 *
 * En vez de empaquetar un WAV enorme, la app lleva solo la IMAGEN ARAM (64 KiB) del
 * banco de música y la sintetiza en el móvil: ocupa nada, hace bucle sin costuras
 * (el secuenciador nunca para) y funciona en cualquier dispositivo, sin librerías
 * nativas.
 */
class PlatformerMusic private constructor(private val renderer: SmwMusicRenderer) {

    @Volatile private var running = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    fun start() {
        if (running) return
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)
        // Colchón grande (~0,75 s): la síntesis del N-SPC+S-DSP va muestra a muestra y
        // es pesada; con un buffer de decenas de ms cualquier pausa del planificador
        // corta el sonido (underrun). Como write() bloquea, el hilo lo mantiene lleno.
        val bytesPerSecond = SAMPLE_RATE * 2 /* canales */ * 2 /* bytes */
        val bufBytes = (bytesPerSecond * 3 / 4).coerceAtLeast(minBuf * 2)
        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(bufBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = at
        running = true
        at.play()
        thread = Thread {
            // Prioridad de audio urgente: evita cortes cuando el hilo del juego compite.
            runCatching {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            }
            val frames = 2048
            val buf = ShortArray(frames * 2)
            while (running) {
                renderer.renderInto(buf, 0, frames)
                val t = track ?: break
                // write() en MODE_STREAM puede escribir de forma parcial: reintenta.
                var written = 0
                while (written < buf.size && running) {
                    val n = t.write(buf, written, buf.size - written)
                    if (n <= 0) break
                    written += n
                }
            }
        }.apply { isDaemon = true; start() }
    }

    fun stop() {
        running = false
        thread?.join(300)
        thread = null
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
    }

    companion object {
        private const val SAMPLE_RATE = 32000

        /**
         * Carga la música empaquetada (assets/music/level.aram, la imagen ARAM del
         * banco de música de SMW horneada de la ROM) y prepara el reproductor con la
         * canción [songId]. Devuelve null si el asset falta o no mide 64 KiB.
         */
        fun fromAssets(context: Context, songId: Int = 1): PlatformerMusic? {
            val aram = runCatching {
                context.assets.open("music/level.aram").use { it.readBytes() }
            }.getOrNull() ?: return null
            if (aram.size != 0x10000) return null
            val renderer = SmwMusicRenderer(aram)
            renderer.selectSong(songId)
            return PlatformerMusic(renderer)
        }
    }
}
