package com.rolebuilder.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * Música de fondo 100% procedural: pistas chiptune definidas como patrones
 * de notas ([MusicTrackDefs]) y sintetizadas en streaming con AudioTrack
 * (melodía de onda cuadrada + bajo triangular). Sin assets de audio.
 *
 * ## Modelo de hilos (arregla la deuda de AUDITORIA §4)
 *
 * El `AudioTrack` es un recurso NATIVO: escribir en uno ya liberado no lanza una
 * excepción de Kotlin que se pueda ver en el logcat, cuelga o mete ruido. Antes,
 * `stopInternal()` hacía `worker?.join(300)` y **seguía adelante aunque el join
 * expirara**. Y expiraba de verdad, no en teoría: una escritura bloqueante mete
 * hasta un paso de corchea (~360 ms a 84 bpm) en un buffer de medio segundo, así
 * que 300 ms se quedan cortos con facilidad. Consecuencias reales:
 *
 * - `play(otra)` arrancaba un SEGUNDO hilo con su propio AudioTrack mientras el
 *   primero seguía sonando: dos pistas solapadas.
 * - `play("title")` → `stop()` → `play("title")` seguido RESUCITABA al hilo viejo:
 *   su condición de bucle era `current == track`, y `current` volvía a valer
 *   "title". Dos hilos tocando la misma pista, desfasados.
 * - `release()` volvía enseguida y la actividad moría con un hilo zombi escribiendo.
 *
 * La solución NO es subir el timeout, son tres reglas:
 *
 * 1. **Cada hilo posee su AudioTrack** y es él quien lo suelta en su `finally`.
 *    Nadie de fuera lo toca, así que un hilo tardón nunca puede escribir en un
 *    recurso que otro haya liberado.
 * 2. **Una [Session] por arranque**: el bucle mira la bandera de SU sesión (por
 *    identidad), no un nombre compartido. Una sesión parada no se puede resucitar.
 * 3. **El hilo se entera de la parada en ~50 ms**: se escribe en trozos cortos y
 *    se comprueba la bandera entre trozos, en vez de bloquearse un paso entero.
 *
 * Con eso, la espera de `stop()` pasa a ser una CORTESÍA (evitar que se solapen
 * dos pistas medio segundo), no un requisito de corrección: si expirase, el peor
 * caso es audio solapado un instante, nunca un uso-después-de-liberar.
 */
class MusicPlayer {

    /**
     * Un arranque de reproducción. Es la unidad de propiedad: el hilo compara
     * identidad con la suya, así que dos arranques del MISMO nombre de pista son
     * dos sesiones distintas y la vieja no revive.
     */
    private class Session(val track: String) {
        /** Mientras sea true el hilo sigue sintetizando. Solo se pone a false. */
        @Volatile
        var running = true

        /** Se abre cuando el hilo YA ha soltado su AudioTrack. */
        val finished = CountDownLatch(1)
    }

    /** Sesión en curso; null = silencio. La lee el hilo que para y la UI. */
    @Volatile
    private var session: Session? = null

    /** Reproduce una pista en bucle; si ya suena esa misma, no hace nada. */
    @Synchronized
    fun play(track: String?) {
        if (track == session?.track) return
        stopInternal()
        if (track == null || track !in MusicTrackDefs.TRACKS) return
        val nueva = Session(track)
        session = nueva
        thread(name = "rb-bgm", isDaemon = true) { runLoop(nueva) }
    }

    @Synchronized
    fun stop() = stopInternal()

    fun release() = stop()

    /**
     * Idempotente y reentrante-seguro: si no hay sesión sale sin hacer nada, así
     * que `stop()` dos veces seguidas (o `stop()` + `release()`, que es lo que
     * hace la actividad al cerrarse) no espera dos veces ni toca nada liberado.
     */
    private fun stopInternal() {
        val actual = session ?: return
        session = null
        actual.running = false
        if (!esperarFin(actual)) {
            // No es fatal (el hilo suelta SU propio track cuando salga), pero si
            // pasa hay audio solapado un momento y conviene poder verlo.
            Log.w(TAG, "el hilo de música tardó más de $STOP_TIMEOUT_MS ms en soltar su AudioTrack")
        }
    }

    private fun esperarFin(sesion: Session): Boolean = try {
        sesion.finished.await(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    } catch (interrumpido: InterruptedException) {
        // Tragarse la interrupción dejaría al hilo llamante sin enterarse de que
        // le han pedido parar: se restaura la marca y se sale sin esperar más.
        Thread.currentThread().interrupt()
        false
    }

    private fun runLoop(sesion: Session) {
        try {
            // Si el dispositivo no puede abrir el AudioTrack, antes la excepción
            // salía sin capturar en un hilo suelto y eso MATA el proceso. Un móvil
            // sin salida de audio no debe tumbar el juego: se queda sin música.
            val audio = runCatching { crearTrack() }
                .onFailure { Log.w(TAG, "no se pudo abrir el AudioTrack de la música", it) }
                .getOrNull() ?: return
            try {
                sintetizar(audio, MusicTrackDefs.TRACKS.getValue(sesion.track), sesion)
            } finally {
                cerrar(audio)
            }
        } finally {
            // Pase lo que pase (fallo al crear, excepción a medias), quien espera
            // en stop() tiene que despertarse: si no, se come el timeout entero.
            sesion.finished.countDown()
        }
    }

    private fun crearTrack(): AudioTrack = AudioTrack.Builder()
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

    private fun sintetizar(audio: AudioTrack, def: MusicTrackDefs.TrackDef, sesion: Session) {
        val stepSamples = (SAMPLE_RATE * 30.0 / def.bpm).toInt() // corchea
        val buffer = ShortArray(stepSamples)
        var melodyPhase = 0.0
        var bassPhase = 0.0
        var melodyFreq = 0.0
        var bassFreq = 0.0
        var step = 0

        audio.play()
        while (sesion.running) {
            val melodyNote = def.melody[step % def.melody.size]
            val bassNote = def.bass[(step / 2) % def.bass.size]
            val retrigger = melodyNote != MusicTrackDefs.HOLD
            if (melodyNote != MusicTrackDefs.REST && melodyNote != MusicTrackDefs.HOLD) melodyFreq = freq(melodyNote)
            if (melodyNote == MusicTrackDefs.REST) melodyFreq = 0.0
            if (bassNote != MusicTrackDefs.REST && bassNote != MusicTrackDefs.HOLD) bassFreq = freq(bassNote)
            if (bassNote == MusicTrackDefs.REST) bassFreq = 0.0

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
            if (!escribir(audio, buffer, sesion)) return
            step = (step + 1) % def.length
        }
    }

    /**
     * Vuelca [buffer] en trozos de [WRITE_CHUNK] muestras comprobando la parada
     * ENTRE trozos. Con una sola escritura del paso entero, el hilo se quedaba
     * bloqueado dentro de `write()` hasta ~360 ms sin poder mirar la bandera: ese
     * era el retardo que hacía expirar el `join(300)` de antes. En trozos de 1024
     * muestras (~46 ms a 22 kHz) el hilo reacciona enseguida y la música suena
     * exactamente igual: cambia el tamaño de la llamada, no los datos.
     *
     * Devuelve false si hay que salir del bucle (parada pedida o error del track).
     */
    private fun escribir(audio: AudioTrack, buffer: ShortArray, sesion: Session): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            if (!sesion.running) return false
            val escrito = audio.write(buffer, offset, minOf(WRITE_CHUNK, buffer.size - offset))
            // Negativo = código de error; 0 = el track ya no acepta datos (parado
            // desde fuera). Reintentar en bucle sería girar en vacío para siempre.
            if (escrito <= 0) return false
            offset += escrito
        }
        return true
    }

    /**
     * Cierre ordenado y SIEMPRE en el hilo dueño. `pause()` + `flush()` antes de
     * `stop()` porque un `stop()` a secas deja el track "vaciando" lo que quede en
     * el buffer (hasta medio segundo de música) justo cuando el usuario ha pedido
     * silencio. Cada paso va en su propio runCatching para que un fallo en
     * cualquiera de ellos no se salte el `release()` y filtre el recurso nativo.
     */
    private fun cerrar(audio: AudioTrack) {
        runCatching { audio.pause() }
        runCatching { audio.flush() }
        runCatching { audio.stop() }
        runCatching { audio.release() }
    }

    private fun freq(semitonesFromA4: Int): Double = 440.0 * 2.0.pow(semitonesFromA4 / 12.0)

    companion object {
        private const val TAG = "MusicPlayer"
        private const val SAMPLE_RATE = 22050

        /** Muestras por escritura: ~46 ms a 22 kHz. Ver [escribir]. */
        private const val WRITE_CHUNK = 1024

        /**
         * Red de seguridad de [stopInternal], no un requisito: con los trozos
         * cortos el hilo sale en decenas de ms. Es holgado a propósito para no
         * avisar por un GC largo, y aun así acotado para no bloquear la UI.
         */
        private const val STOP_TIMEOUT_MS = 2_000L
    }
}

/**
 * Las pistas chiptune como DATOS puros: ni una referencia a Android, para que el
 * contrato con `:core` (que `MusicTracks.ALL` y estas pistas cuadren) se pueda
 * probar en JVM pura, sin dispositivo ni Robolectric. Si el editor ofrece una
 * pista que aquí no existe, `MusicPlayer.play()` la descarta y el mapa se queda
 * MUDO en silencio: es el mismo fallo que tuvieron los efectos "coin" y
 * "levelup" (AUDITORIA §2), y por eso lleva test propio.
 */
object MusicTrackDefs {

    /** Silencio. */
    const val REST = Int.MIN_VALUE

    /** Sostiene la nota anterior sin re-atacarla. */
    const val HOLD = Int.MAX_VALUE

    /**
     * Patrón de una pista. Valida en el constructor —como `GameMap` (AUDITORIA
     * §2.6)— porque los tres invariantes son divisiones del bucle de audio: con
     * una melodía o un bajo vacíos, `step % melody.size` revienta con
     * ArithmeticException DENTRO del hilo de audio (y ahí mata el proceso), y con
     * bpm 0 el paso sale de 0 muestras y el hilo gira en vacío al 100% de CPU.
     */
    class TrackDef(val bpm: Int, val melody: IntArray, val bass: IntArray) {
        val length: Int = melody.size

        init {
            require(bpm > 0) { "bpm debe ser > 0, era $bpm" }
            require(melody.isNotEmpty()) { "la melodía no puede estar vacía" }
            require(bass.isNotEmpty()) { "el bajo no puede estar vacío" }
        }
    }

    private val R = REST
    private val H = HOLD

    // Notas en semitonos desde A4 (0 = La 440). -12 = La3, 3 = Do5...
    // Las claves DEBEN ser exactamente las de MusicTracks.ALL en :core.
    val TRACKS: Map<String, TrackDef> = mapOf(
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
