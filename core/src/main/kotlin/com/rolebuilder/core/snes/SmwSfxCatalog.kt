package com.rolebuilder.core.snes

/**
 * # SMW SFX CATALOG — mapea EVENTOS de juego a un clip PCM real de Super Mario World
 *
 * [SmwSoundFx] ya extrae y decodifica el BANCO de 20 muestras BRR de la ROM. Lo que
 * faltaba —y resuelve esta clase— es la otra mitad del problema: **qué muestra y a
 * qué tono** usa cada efecto de sonido, para poder reproducir "moneda", "salto",
 * "pisotón"… con el sonido auténtico del juego.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CÓMO DISPARA SMW SUS SFX (investigado en la reimplementación C de snesrev/smw,
 * `smw_spc_player.c`, y en el juego `smw_00.c`..`smw_0c.c`)
 * ─────────────────────────────────────────────────────────────────────────────
 * La CPU escribe un id de efecto en uno de tres puertos de la APU:
 *
 *   `io_sound_ch1` ($1DF9) → puerto 0 → `Sfx0_Process`  (tabla de secuencias `$5681`)
 *   `io_sound_ch2` ($1DFA) → puerto 1 → `Sfx1_Process`  (dos efectos CABLEADOS)
 *   `io_sound_ch3` ($1DFC) → puerto 3 → `Sfx3_Process`  (tabla de secuencias `$5619`)
 *
 * El motor N-SPC vive subido en la ARAM (bloque `kSpcEngine`, `$0E:8000`). Dentro
 * de él, en direcciones ARAM fijas, están las tablas que traducen id→sonido:
 *
 *   `$5570` TABLA DE INSTRUMENTOS: 9 bytes por instrumento. Los 8 primeros son los
 *           registros de voz del DSP (VOLL, VOLR, PITCHL, PITCHH, **SRCN**, ADSR1,
 *           ADSR2, GAIN) y el 9º es el *pitch base* del instrumento. El byte 4
 *           (**SRCN**) es el ÍNDICE de muestra en el directorio BRR (`$8000`) — es
 *           decir, apunta a una de las muestras que [SmwSoundFx] decodifica.
 *   `$5681` Tabla de punteros de Sfx0: `secuencia = WORD(ram[$5681 + id*2])`.
 *   `$5619` Tabla de punteros de Sfx3: `secuencia = WORD(ram[$5619 + id*2])`.
 *
 * Una SECUENCIA de SFX es un flujo de bytes con el mismo formato que la música:
 *   - `00`            fin del efecto.
 *   - `len [volL [volR]]`  bytes < 0x80: duración de nota y volúmenes opcionales.
 *   - `DA ii`         fija el instrumento `ii` (→ muestra `instr[ii].SRCN`).
 *   - `DD nn ...`     toca la nota `nn` (con envolvente de tono a continuación).
 *   - `EB a b c`      envolvente de tono (sin nota).
 *   - `FF`            reinicia la secuencia (bucle).
 *   - `80..C5`        una nota (`PlayNote`), toca la muestra del instrumento actual.
 *
 * `PlayNote`/`WritePitch` convierten la nota + el *pitch base* del instrumento en el
 * paso de tono de 14 bits del DSP (0x1000 = tono nativo, 32000 Hz). Reproducimos la
 * muestra decodificada re-muestreada a ese tono.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * EVENTOS QUE MAPEAMOS (id verificado en la desensamblada del juego)
 * ─────────────────────────────────────────────────────────────────────────────
 *   JUMP       `io_sound_ch2 = 1`  Sfx1 cableado: instrumento 8, nota 0xB2   (00daa9/00d…)
 *   SPIN_JUMP  `io_sound_ch3 = 4`  Sfx3 id 0x04                              (HandlePlayerPhysics)
 *   COIN       `io_sound_ch3 = 1`  Sfx3 id 0x01                              (GiveCoins_OneCoin, 05b34a)
 *   STOMP      `io_sound_ch1 = 0x13` Sfx0 id 0x13  (kStompSoundTable_Bank01) (01…)
 *   KICK       `io_sound_ch1 = 3`  Sfx0 id 0x03                              (…PlayKickSfx, 01a728)
 *   POWERUP    `io_sound_ch1 = 0x0A` Sfx0 id 0x0A                            (GiveMarioMushroom, 01c561)
 *
 * (La MUERTE de Mario en SMW es un *jingle de música*, no una muestra suelta, así que
 * no aparece aquí; la app la aproxima re-pitcheando otro clip.)
 *
 * ## Límite honesto
 *
 * El cálculo del tono ([dspPitch]) replica `ComputePeriod`/`WritePitch` del motor,
 * incluyendo el *pitch base* del instrumento y la interpolación fraccionaria. No
 * modela la envolvente ADSR, el vibrato, el barrido de tono ni el eco del DSP: un
 * SFX de SMW se reproduce aquí como su muestra al tono de su PRIMERA nota, que basta
 * para que "moneda", "salto" o "pisotón" suenen reconocibles. La selección de
 * muestra y el re-muestreo son lógica pura y están cubiertos por tests sin ROM.
 */
object SmwSfxCatalog {

    /** Eventos de juego a los que damos un sonido auténtico de SMW. */
    enum class Event { JUMP, SPIN_JUMP, COIN, STOMP, KICK, POWERUP }

    /** Dirección ARAM de la tabla de instrumentos (9 bytes/entrada). */
    const val INSTRUMENT_TABLE = 0x5570

    /** Dirección ARAM de la tabla de punteros de secuencias de Sfx0. */
    const val SFX0_PTR_TABLE = 0x5681

    /** Dirección ARAM de la tabla de punteros de secuencias de Sfx3. */
    const val SFX3_PTR_TABLE = 0x5619

    /** Paso de tono del DSP que equivale al tono nativo de la muestra (32000 Hz). */
    const val NATIVE_PITCH = 0x1000

    /** Frecuencia de salida por defecto de los clips (coincide con la del RPG). */
    const val DEFAULT_OUTPUT_RATE = 22050

    /**
     * Origen de un SFX: banco (0=Sfx0, 1=Sfx1, 3=Sfx3) e id que la CPU escribe en
     * el puerto. Los bancos 0 y 3 se resuelven por tabla de secuencias; el banco 1
     * tiene dos efectos cableados en el motor.
     */
    data class SfxSource(val bank: Int, val id: Int)

    /** Instrumento + nota inicial que un SFX toca (lo que fija su muestra y su tono). */
    data class Voice(val instrument: Int, val note: Int)

    /** Un clip PCM listo para reproducir: mono, 16-bit con signo, a [sampleRate] Hz. */
    class PcmClip(val event: Event, val pcm: ShortArray, val sampleRate: Int)

    /** Evento → puerto/id de SMW (ver KDoc para la procedencia de cada id). */
    val EVENT_SOURCE: Map<Event, SfxSource> = mapOf(
        Event.JUMP to SfxSource(1, 1),
        Event.SPIN_JUMP to SfxSource(3, 4),
        Event.COIN to SfxSource(3, 1),
        Event.STOMP to SfxSource(0, 0x13),
        Event.KICK to SfxSource(0, 0x03),
        Event.POWERUP to SfxSource(0, 0x0A),
    )

    /**
     * Efectos CABLEADOS de Sfx1 (no viven en tabla): el motor los toca a mano en
     * `Sfx1_Process`. Mapea id → primera voz (instrumento, nota).
     *   id 1 = salto  → instrumento 8, nota 0xB2
     *   id 4 = doble  → instrumento 7, nota 0xA4
     */
    private val SFX1_HARDCODED: Map<Int, Voice> = mapOf(
        1 to Voice(8, 0xB2),
        4 to Voice(7, 0xA4),
    )

    // -------------------------------------------------------------- direccionamiento

    private fun u16(a: ByteArray, i: Int): Int =
        (a[i].toInt() and 0xFF) or ((a[i + 1].toInt() and 0xFF) shl 8)

    private fun byteAt(a: ByteArray, i: Int): Int = a[i and 0xFFFF].toInt() and 0xFF

    /** Índice de muestra (SRCN, byte 4) del instrumento [instr] en la ARAM. */
    fun instrumentSample(aram: ByteArray, instr: Int): Int =
        byteAt(aram, INSTRUMENT_TABLE + instr * 9 + 4)

    /** Pitch base (byte 8) del instrumento [instr] en la ARAM. */
    fun instrumentPitchBase(aram: ByteArray, instr: Int): Int =
        byteAt(aram, INSTRUMENT_TABLE + instr * 9 + 8)

    // ------------------------------------------------------------- resolución de voz

    /**
     * Recorre una secuencia de SFX desde [seqStart] en la [aram] y devuelve la
     * PRIMERA voz que toca (instrumento fijado por `DA` + primera nota `DD`/`80..C5`).
     * Replica el descodificador de comandos de `Sfx0_Process`/`Sfx3_Process`.
     * Devuelve `null` si la secuencia termina o hace bucle sin tocar ninguna nota.
     */
    fun resolveSequenceVoice(aram: ByteArray, seqStart: Int): Voice? {
        var p = seqStart and 0xFFFF
        var instrument = -1
        var guard = 0
        while (guard++ < 512) {
            var cmd = byteAt(aram, p)
            if (cmd == 0x00) return null // fin sin nota
            // Prefijo de duración + volúmenes (todos bytes < 0x80).
            if (cmd < 0x80) {
                p = (p + 1) and 0xFFFF; cmd = byteAt(aram, p) // len -> siguiente
                if (cmd < 0x80) {
                    p = (p + 1) and 0xFFFF; cmd = byteAt(aram, p) // volL -> siguiente
                    if (cmd < 0x80) {
                        p = (p + 1) and 0xFFFF; cmd = byteAt(aram, p) // volR -> siguiente
                    }
                }
            }
            when {
                cmd == 0xDA -> { // fija instrumento, sigue leyendo
                    p = (p + 1) and 0xFFFF
                    instrument = byteAt(aram, p)
                    p = (p + 1) and 0xFFFF
                }
                cmd == 0xDD -> { // toca nota explícita
                    p = (p + 1) and 0xFFFF
                    return Voice(instrument, byteAt(aram, p))
                }
                cmd == 0xEB -> p = (p + 4) and 0xFFFF // envolvente de tono, sin nota
                cmd == 0xFF -> return null // bucle sin nota previa: evita recursión infinita
                cmd in 0x80..0xC5 -> return Voice(instrument, cmd) // nota directa
                else -> p = (p + 1) and 0xFFFF // comando desconocido: salta un byte
            }
        }
        return null
    }

    /** Resuelve la voz (instrumento, nota) de un [source] usando la [aram]. */
    fun resolveVoice(aram: ByteArray, source: SfxSource): Voice? = when (source.bank) {
        1 -> SFX1_HARDCODED[source.id]
        0 -> resolveSequenceVoice(aram, u16(aram, SFX0_PTR_TABLE + source.id * 2))
        3 -> resolveSequenceVoice(aram, u16(aram, SFX3_PTR_TABLE + source.id * 2))
        else -> null
    }

    // ------------------------------------------------------------------- tono (pitch)

    /** Frecuencias base de las 12 notas de una octava (motor N-SPC de SMW). */
    private val BASE_NOTE_FREQS =
        intArrayOf(4286, 4541, 4811, 5097, 5400, 5721, 6061, 6422, 6804, 7208, 7637, 8091)

    /** `ComputePeriod` del motor: periodo/frecuencia base de la nota [v] (byte). */
    fun computePeriod(v: Int): Int {
        val pp = v and 0x7F
        var q = pp / 12
        val r = pp % 12
        var t = BASE_NOTE_FREQS[r]
        while (q != 6) { t = t shr 1; q++ }
        return t and 0xFFFF
    }

    /**
     * `WritePitch` del motor: paso de tono de 14 bits del DSP para la [note] con el
     * [pitchBase] del instrumento (afinación fina = 0, transposición global = 0).
     * 0x1000 ≈ tono nativo (32000 Hz). Devuelve el valor recortado a 14 bits.
     */
    fun dspPitch(note: Int, pitchBase: Int): Int {
        var pitch = (note and 0x7F) shl 8
        val hi = pitch shr 8
        if (hi >= 0x34) {
            pitch += hi - 0x34
        } else if (hi < 0x13) {
            pitch += (((hi - 0x13) * 2) and 0xFF) - 256
        }
        val ph = pitch shr 8
        val pl = pitch and 0xFF
        val t0 = computePeriod(ph)
        val t1 = computePeriod(ph + 1)
        val td = (t1 - t0) and 0xFFFF
        var t = t0
        t += (td shr 8) * pl
        t += ((td and 0xFF) * pl) shr 8
        t = (t * (pitchBase and 0xFF)) and 0xFFFF // el motor multiplica en uint16
        return t and 0x3FFF // el DSP usa solo 14 bits de tono
    }

    // ----------------------------------------------------------------- re-muestreo

    /**
     * Re-muestreo lineal de [src] (a [srcRate] Hz) a [dstRate] Hz. Lógica pura,
     * núcleo comprobado por tests: preserva el primer y último valor y el número de
     * muestras escala con la razón de frecuencias. Si [dstRate] o [srcRate] no son
     * válidos, o [src] está vacío, devuelve un arreglo vacío.
     */
    fun resampleLinear(src: ShortArray, srcRate: Float, dstRate: Float): ShortArray {
        if (src.isEmpty() || srcRate <= 0f || dstRate <= 0f) return ShortArray(0)
        if (srcRate == dstRate) return src.copyOf()
        val outLen = maxOf(1, ((src.size * dstRate) / srcRate).toInt())
        val out = ShortArray(outLen)
        val step = (src.size - 1).toFloat() / maxOf(1, outLen - 1)
        for (i in 0 until outLen) {
            val pos = i * step
            val i0 = pos.toInt().coerceIn(0, src.size - 1)
            val i1 = (i0 + 1).coerceAtMost(src.size - 1)
            val frac = pos - i0
            val v = src[i0] + (src[i1] - src[i0]) * frac
            out[i] = v.toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    // --------------------------------------------------------------------- catálogo

    /**
     * Construye el clip PCM de un [event] a [outRate] Hz, resolviendo su muestra y su
     * tono en la [aram] del motor y tomando el PCM decodificado del [bank]. Devuelve
     * `null` si el evento no se puede resolver (secuencia vacía o muestra fuera de
     * rango).
     */
    fun clipFor(
        aram: ByteArray,
        bank: SmwSoundFx.SoundBank,
        event: Event,
        outRate: Int = DEFAULT_OUTPUT_RATE,
    ): PcmClip? {
        val source = EVENT_SOURCE[event] ?: return null
        val voice = resolveVoice(aram, source) ?: return null
        if (voice.instrument < 0) return null
        val srcn = instrumentSample(aram, voice.instrument)
        val sample = bank.samples.firstOrNull { it.index == srcn } ?: return null
        if (sample.pcm.isEmpty()) return null
        val pitchBase = instrumentPitchBase(aram, voice.instrument)
        val pitch = dspPitch(voice.note, pitchBase)
        // Tono efectivo de la muestra: nativo * (pitch/0x1000). Si el tono degenera
        // a 0 (instrumento sin datos válidos), cae al tono nativo.
        val effectiveSrcRate =
            if (pitch <= 0) sample.sampleRate.toFloat()
            else sample.sampleRate * (pitch.toFloat() / NATIVE_PITCH)
        // La muestra BRR cruda dura pocos ms; SMW la reproduce EN BUCLE (punto de loop
        // del BRR) mientras suena el efecto. Sin eso el clip es un "clic" inaudible.
        // La extendemos a una duración audible siguiendo el loop, con envolvente
        // (ataque corto + caída) para que no chasquee al cortar.
        val pcm = renderSfxWaveform(sample, effectiveSrcRate, outRate.toFloat(), durationSecFor(event))
        if (pcm.isEmpty()) return null
        return PcmClip(event, pcm, outRate)
    }

    /** Duración audible del efecto (s). Los sonidos "largos" de SMW se sostienen más. */
    private fun durationSecFor(event: Event): Float = when (event) {
        Event.POWERUP -> 0.45f
        Event.COIN -> 0.25f
        else -> 0.18f
    }

    /**
     * Reproduce la [sample] durante [durationSec] a [outRate] Hz siguiendo su punto de
     * bucle BRR (si lo tiene), con interpolación lineal al [srcRate] efectivo (tono) y
     * una envolvente de ataque/caída para evitar chasquidos. Es lo que convierte una
     * muestra de pocos ms en un efecto audible.
     */
    private fun renderSfxWaveform(
        sample: SmwSoundFx.BrrSample,
        srcRate: Float,
        outRate: Float,
        durationSec: Float,
    ): ShortArray {
        val src = sample.pcm
        if (src.isEmpty()) return ShortArray(0)
        val outLen = (outRate * durationSec).toInt().coerceAtLeast(1)
        val step = srcRate / outRate                 // muestras fuente por muestra de salida
        val loopStart = sample.loopStartSample.coerceIn(0, src.size - 1)
        val loopLen = src.size - loopStart
        val canLoop = sample.hasLoop && loopLen > 1
        val attack = (outRate * 0.004f).coerceAtLeast(1f)   // 4 ms de ataque
        val release = outLen * 0.35f                        // 35 % final en caída
        val out = ShortArray(outLen)
        var pos = 0f
        for (i in 0 until outLen) {
            // Posición fuente con bucle.
            var p = pos
            if (p >= loopStart) {
                p = if (canLoop) loopStart + ((p - loopStart) % loopLen)
                else if (p < src.size) p else Float.NaN
            }
            var s = 0f
            if (!p.isNaN()) {
                val i0 = p.toInt()
                val frac = p - i0
                val a = src[i0.coerceIn(0, src.size - 1)].toFloat()
                val b = src[(i0 + 1).coerceIn(0, src.size - 1)].toFloat()
                s = a + (b - a) * frac
            }
            // Envolvente (ataque + caída lineal).
            var gain = 1f
            if (i < attack) gain = i / attack
            val fromEnd = outLen - i
            if (fromEnd < release) gain *= fromEnd / release
            out[i] = (s * gain).toInt().coerceIn(-32768, 32767).toShort()
            pos += step
        }
        return out
    }

    /**
     * Catálogo completo: extrae de la ROM el motor (tablas de SFX) y el banco de
     * muestras, y devuelve un clip PCM por cada [Event] que se pueda resolver.
     * `null` si la ROM no contiene los datos de SMW esperados.
     */
    fun build(rom: ByteArray, header: SnesHeader, outRate: Int = DEFAULT_OUTPUT_RATE): Map<Event, PcmClip>? {
        val bank = SmwSoundFx.extract(rom, header) ?: return null
        val delta = SmwMusic.deltaOf(header)
        val engine = SmwMusic.readNspc(rom, SmwMusic.ENGINE, delta) ?: return null
        val aram = ByteArray(SmwMusic.ARAM_SIZE)
        SmwMusic.applyBlocks(aram, engine)
        val out = LinkedHashMap<Event, PcmClip>()
        for (event in Event.entries) {
            clipFor(aram, bank, event, outRate)?.let { out[event] = it }
        }
        return out.ifEmpty { null }
    }
}
