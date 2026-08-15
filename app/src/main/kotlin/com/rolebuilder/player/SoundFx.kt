package com.rolebuilder.player

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.rolebuilder.core.model.SoundEffects
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Efectos de sonido 100% sintetizados: se generan como WAV en la caché la
 * primera vez y se reproducen con SoundPool. Sin assets de audio externos.
 *
 * Aquí solo vive la parte que necesita Android (SoundPool, caché, Context); la
 * síntesis y la tabla de nombres están en [SoundSynth], que es Kotlin puro y por
 * eso SÍ se puede probar en CI sin dispositivo. Esa separación es el punto: los
 * dos bugs de sonido mudo de AUDITORIA §2 vivían en la tabla de nombres, la
 * parte más fácil de probar, y no había forma de tocarla desde un test.
 */
class SoundFx(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /**
     * Muestras ya cargadas. La carga de SoundPool es ASÍNCRONA y su callback
     * llega en un hilo propio mientras el hilo de render lee este conjunto: con
     * un HashSet normal eso es una carrera de datos (puede perder elementos o
     * romperse por dentro), así que va un conjunto concurrente.
     */
    private val loaded = ConcurrentHashMap.newKeySet<Int>()

    /** Nombres desconocidos ya avisados, para no inundar el logcat a 60 fps. */
    private val avisados = ConcurrentHashMap.newKeySet<String>()

    /**
     * id de SoundPool por nombre. Mapa INMUTABLE creado en el constructor: al ser
     * un campo final, el hilo de render lo ve completo y sin sincronizar
     * (publicación segura del JMM). Antes era un MutableMap que se escribía en
     * `init` y se leía desde el hilo GL.
     */
    private val ids: Map<String, Int>

    @Volatile
    private var released = false

    init {
        // El listener SE REGISTRA ANTES de cargar nada: si una carga terminase
        // antes de tenerlo puesto, esa muestra no entraría nunca en `loaded` y el
        // efecto se quedaría mudo para siempre.
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        ids = SoundSynth.NAMES.associateWith { register(context, it) }
    }

    fun play(name: String) {
        // Tras release() el SoundPool nativo ya no existe. La comprobación no
        // cierra del todo la ventana (haría falta un lock en el camino caliente
        // del hilo GL), pero la reduce a lo despreciable: release() solo se llama
        // al destruir la actividad, cuando el render ya no tira sonidos.
        if (released) return
        val id = ids[name]
        if (id == null) {
            // FALLO SILENCIOSO que ya costó dos bugs (AUDITORIA §2): el motor
            // encola un nombre que la app no sintetiza y la acción se queda muda
            // sin rastro. Ahora al menos deja huella; el test de contrato
            // (AudioContractTest) es lo que impide que llegue a pasar.
            if (avisados.add(name)) {
                Log.w(TAG, "sonido '$name' no registrado en SoundSynth.NAMES: se oirá SILENCIO")
            }
            return
        }
        if (id in loaded) soundPool.play(id, VOLUME, VOLUME, 1, 0, 1f)
    }

    /** Idempotente: liberar dos veces el SoundPool nativo no es inofensivo. */
    @Synchronized
    fun release() {
        if (released) return
        released = true
        soundPool.release()
    }

    private fun register(context: Context, name: String): Int {
        val file = File(context.cacheDir, "sfx_$name.wav")
        val bytes = SoundSynth.wav(SoundSynth.synthesize(name))
        // Se regenera también si el tamaño no cuadra: un WAV a medias (la app
        // murió mientras lo escribía) dejaba el efecto mudo PARA SIEMPRE, porque
        // exists() decía true y SoundPool no podía cargarlo. Sintetizar de nuevo
        // cuesta microsegundos; quedarse mudo, una sesión entera de juego.
        if (!file.exists() || file.length() != bytes.size.toLong()) {
            escribirAtomico(file, bytes)
        }
        return soundPool.load(file.absolutePath, 1)
    }

    /**
     * Temporal + rename, como el guardado de partidas (AUDITORIA §2.3): así un
     * corte a media escritura no deja el WAV truncado en su sitio definitivo.
     */
    private fun escribirAtomico(file: File, bytes: ByteArray) {
        val tmp = File(file.absolutePath + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            // Algunos sistemas de ficheros no renombran sobre uno existente.
            file.writeBytes(bytes)
            tmp.delete()
        }
    }

    companion object {
        private const val TAG = "SoundFx"
        private const val VOLUME = 0.6f
    }
}

/**
 * Síntesis de los efectos de sonido del RPG y **tabla de nombres registrados**,
 * sin una sola dependencia de Android para poder probarla en JVM pura.
 *
 * [NAMES] es la mitad de `:app` de un contrato con `:core`: el motor encola
 * nombres en `RpgEngine.soundQueue` y la app los busca aquí. Si el motor encola
 * uno que no está, `SoundFx.play()` no encuentra id y la acción se queda MUDA —
 * pasó con "coin" y "levelup" (AUDITORIA §2.1). Lo vigila `AudioContractTest`.
 */
object SoundSynth {

    const val SAMPLE_RATE = 22050

    /**
     * Nombres que la app sabe sintetizar. **No es una lista propia: es la de
     * `:core`.** Antes había dos listas —la del motor y la de la app— y nada
     * impedía que se separasen; separarse era exactamente el bug de "coin" y
     * "levelup" (AUDITORIA §2.1). Ahora hay una sola, así que la app no puede
     * quedarse corta: si `:core` añade un efecto, aparece aquí solo.
     *
     * Lo que esto NO garantiza es que [synthesize] sepa darle forma de onda a
     * cada uno —un nombre nuevo sin su rama caería en el pitido genérico— y de
     * eso se ocupa `AudioContractTest`.
     */
    val NAMES: List<String> = SoundEffects.ALL

    /**
     * PCM del efecto [name]. Un nombre desconocido cae en un pitido genérico
     * (audible a propósito: mejor un sonido raro que un silencio que nadie sabe
     * diagnosticar), porque `EventCommand.PlaySound` acepta texto libre y un
     * proyecto editado a mano puede traer cualquier cosa.
     */
    fun synthesize(name: String): ShortArray = when (name) {
        SoundEffects.ATTACK -> noise(90, 0.4f)
        SoundEffects.HIT -> tone(220f, 160f, 90, square = true)
        SoundEffects.HURT -> tone(140f, 90f, 200, square = true)
        SoundEffects.DEFEAT -> tone(360f, 60f, 280)
        SoundEffects.PICKUP -> tone(880f, 1320f, 120)
        SoundEffects.SELECT -> tone(660f, 660f, 60)
        SoundEffects.CHEST -> tone(440f, 880f, 200)
        SoundEffects.HEAL -> tone(520f, 780f, 250)
        SoundEffects.SHOOT -> tone(500f, 240f, 110, square = true)
        SoundEffects.COIN -> tone(988f, 1319f, 130)
        SoundEffects.LEVELUP -> tone(523f, 1568f, 400)
        else -> GENERICO
    }

    /**
     * Pitido de reserva para un nombre desconocido. Audible a propósito: mejor
     * un sonido raro que un silencio imposible de diagnosticar. Es un valor con
     * nombre para que `AudioContractTest` pueda preguntar "¿esto cayó aquí?".
     */
    internal val GENERICO: ShortArray by lazy { tone(440f, 440f, 80) }

    /** Empaqueta PCM16 mono en un WAV. */
    fun wav(pcm: ShortArray): ByteArray {
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
}
