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
            // Buffer holgado (4× el mínimo): con el justo, cualquier pausa del GC o
            // un frame lento del sintetizador provoca UNDERRUNS periódicos, que se
            // oyen como clicks/huecos RÍTMICOS sobre la música.
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = at
        running = true
        at.play()
        thread = Thread {
            // Prioridad de AUDIO: sin ella, el hilo compite con el render GL y el
            // motor a 60 fps y se salta plazos (misma sintomatología rítmica).
            runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO) }
            val frames = 1024
            val buf = ShortArray(frames * 2)
            while (running) {
                renderer.renderInto(buf, 0, frames)
                val t = track ?: break
                t.write(buf, 0, buf.size)
            }
        }.apply { isDaemon = true; start() }
    }

    /** ¿Está el hilo de audio activo y el secuenciador con una canción sonando? */
    fun isPlaying(): Boolean = running && runCatching { renderer.isPlaying() }.getOrDefault(false)

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
                com.rolebuilder.editor.snes.SmwAssetStore.open(context, "music/level.aram")
                    ?: error("sin música horneada")
            }.getOrNull() ?: return null
            if (aram.size != 0x10000) return null
            val renderer = SmwMusicRenderer(aram)
            renderer.selectSong(songId)
            return PlatformerMusic(renderer)
        }

        /**
         * Deriva la música del banco de NIVEL directamente de la ROM del usuario (con
         * [SmwMusic.assembleAram], sin el asset pre-horneado) y prepara el reproductor
         * con la canción [songId] — así cada nivel suena SU música real. Devuelve null
         * si no se puede ensamblar el ARAM (ROM no compatible), para caer al asset.
         */
        fun fromRom(rom: ByteArray, songId: Int): PlatformerMusic? =
            fromRomBank(rom, com.rolebuilder.core.snes.SmwMusic.LEVEL_MUSIC, songId)

        /**
         * Igual que [fromRom] pero eligiendo el BANCO de música: el de NIVEL, el del OVERWORLD
         * (mapa del mundo) o el de CRÉDITOS ([SmwMusic.MUSIC_BANKS]). Así el overworld y otras
         * pantallas suenan con SU banco real, no solo los niveles. [songId] es 1-based dentro
         * del banco. null si la ROM no ensambla ese banco.
         */
        fun fromRomBank(
            rom: ByteArray,
            bank: com.rolebuilder.core.snes.SmwMusic.NspcRegion,
            songId: Int,
        ): PlatformerMusic? {
            val header = com.rolebuilder.core.snes.SnesDecoder.parseHeader(rom)
            val delta = header.headerOffset - 0x7FC0
            val aram = com.rolebuilder.core.snes.SmwMusic.assembleAram(rom, delta, bank) ?: return null
            if (aram.size != 0x10000) return null
            val renderer = SmwMusicRenderer(aram)
            renderer.selectSong(songId)
            return PlatformerMusic(renderer)
        }
    }
}
