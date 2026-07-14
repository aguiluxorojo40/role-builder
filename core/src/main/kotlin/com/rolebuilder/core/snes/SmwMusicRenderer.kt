package com.rolebuilder.core.snes

/**
 * Renderizador de MÚSICA de *Super Mario World* 100% en Kotlin, sin NDK.
 *
 * Combina:
 *  - El **secuenciador N-SPC** de SMW (portado del *music path* de
 *    `smw_spc_player.c` de snesrev/smw): lee la canción de la ARAM, avanza
 *    patrones/notas por canal y traduce todo a escrituras de registros DSP.
 *  - El [SmwDsp] (S-DSP portado de `dsp.c`): sintetiza el PCM final leyendo las
 *    muestras BRR de la misma imagen ARAM.
 *
 * No emula la CPU SPC700: reimplementa directamente la lógica del motor de
 * sonido, exactamente como hace la reimplementación C de referencia.
 *
 * ## Uso
 * ```
 * val aram = SmwMusic.assembleAram(rom, delta, SmwMusic.LEVEL_MUSIC)!!
 * val r = SmwMusicRenderer(aram)
 * r.selectSong(songId)           // id 1-based tal y como lo usa el motor
 * val pcm = r.render(seconds = 8) // estéreo intercalado, 16-bit, 32000 Hz
 * ```
 *
 * El SFX (`Sfx0/1/3_Process`) se OMITE deliberadamente: los efectos ya suenan por
 * otra vía en la app; aquí solo interesa la música, reduciendo el alcance.
 *
 * Rango dinámico: salida a [SAMPLE_RATE] Hz, estéreo, PCM16 con signo.
 */
class SmwMusicRenderer(aram: ByteArray) {

    /** Imagen de la RAM del APU (64 KiB): motor + muestras + banco de música. */
    private val ram = ByteArray(0x10000)
    private val dsp = SmwDsp(ram)

    /** DEBUG: desactiva las escrituras del eco en la ARAM (para aislar corrupción). */
    var debugDisableEchoWrites: Boolean
        get() = dsp.debugDisableEchoWrites
        set(v) { dsp.debugDisableEchoWrites = v }

    /** DEBUG: rango [lo, hi) de ARAM que el eco ha tocado con sus escrituras. */
    fun debugEchoRange(): Pair<Int, Int> = dsp.debugEchoLo to dsp.debugEchoHi

    /** DEBUG: máscara de canales audibles (bit i = canal i suena). */
    var debugChannelMask: Int
        get() = dsp.debugChannelMask
        set(v) { dsp.debugChannelMask = v }

    init {
        require(aram.size == 0x10000) { "La ARAM debe medir 64 KiB" }
        System.arraycopy(aram, 0, ram, 0, ram.size)
        // OJO: initialize() se llama en un init block AL FINAL de la clase, cuando ya
        // están construidos channel[] y el resto del estado (orden de inicialización).
    }

    // ------------------------------------------------------------- estado motor

    private class Channel(val index: Int) {
        var patternCurPtr = 0
        var noteTicksLeft = 0
        var volumeFadeTicks = 0
        var panNumTicks = 0
        var pitchSlideLength = 0
        var pitchSlideDelayLeft = 0
        var vibratoHoldCount = 0
        var vibDepth = 0
        var tremoloHoldCount = 0
        var tremoloDepth = 0
        var subroutineNumLoops = 0
        var instrumentId = 0
        var noteKeyoffTicksLeft = 0
        var vibratoChangeCount = 0
        var noteLength = 0
        var noteGateOffFixedpt = 0
        var instrumentPitchBase = 0
        var channelVolumeMaster = 0
        var channelVolume = 0
        var volumeFadeAddpertick = 0
        var volumeFadeTarget = 0
        var panValue = 0
        var panAddPerTick = 0
        var panTargetValue = 0
        var panFlagWithPhaseInvert = 0
        var pitch = 0
        var pitchAddPerTick = 0
        var pitchTarget = 0
        var fineTune = 0
        var pitchEnvelopeNumTicks = 0
        var pitchEnvelopeDelay = 0
        var pitchEnvelopeDirection = 0
        var pitchEnvelopeSlideValue = 0
        var vibratoCount = 0
        var vibratoRate = 0
        var vibratoDelayTicks = 0
        var vibratoFadeNumTicks = 0
        var vibratoFadeAddPerTick = 0
        var vibDepthOrig = 0
        var tremoloCount = 0
        var tremoloDelayTicks = 0
        var finalVolume = 0
        var savedPatternPtr = 0
        var patternStartPtr = 0
    }

    private val channel = Array(8) { Channel(it) }

    private val inputPorts = IntArray(4)
    private val portToSnes = IntArray(4)
    private val lastValueFromSnes = IntArray(4)
    private val newValueFromSnes = IntArray(4)

    private var counterSf0c = 0
    private var isChanOn = 0
    private var chanBitFlags = 0
    private var musicPtrToplevel = 0
    private var blockCount = 0
    private var globalTransposition = 0
    private var curChanBit = 0
    private var keyOn = 0
    private var mainTempoAccum = 0
    private var tempo = 0
    private var tempoFadeNumTicks = 0
    private var tempoFadeFinal = 0
    private var tempoFadeAdd = 0
    private var masterVolume = 0
    private var masterVolumeFadeTicks = 0
    private var masterVolumeFadeTarget = 0
    private var masterVolumeFadeAddPerTick = 0
    private var volDirty = 0
    private var echoVolumeFadeTicks = 0
    private var echoVolumeLeft = 0
    private var echoVolumeRight = 0
    private var echoVolumeFadeAddLeft = 0
    private var echoVolumeFadeAddRight = 0
    private var echoVolumeFadeTargetLeft = 0
    private var echoVolumeFadeTargetRight = 0
    private var smwTempoIncrease = 0
    private var smwPauseMusic = 0
    private var smwPlayerOnYoshi = 0
    private var echoChannels = 0
    private var didAffectVolumePitchFlag = 0

    private var timerCycles = 0

    // Ahora que todo el estado (channel[], puertos, contadores…) está construido,
    // arranca el motor: carga variables por defecto en RAM y prepara el DSP.
    init {
        initialize()
    }

    // ------------------------------------------------------------- API pública

    /** Selecciona la canción [id] (1-based) escribiendo el puerto de entrada 2. */
    fun selectSong(id: Int) {
        inputPorts[2] = id and 0xff
    }

    /** ¿Sigue sonando una canción? (false cuando el motor apaga el puerto). */
    fun isPlaying(): Boolean = portToSnes[2] != 0

    /**
     * Renderiza [samples] muestras estéreo y las escribe en [out] (intercalado
     * L,R) a partir de [outOffset]. Devuelve el número de shorts escritos.
     */
    fun renderInto(out: ShortArray, outOffset: Int, samples: Int): Int {
        var o = outOffset
        for (s in 0 until samples) {
            if (timerCycles >= 64) {
                spcLoopPart2(timerCycles shr 6)
                timerCycles = timerCycles and 63
            }
            dsp.cycle()
            out[o++] = dsp.outL.toShort()
            out[o++] = dsp.outR.toShort()
            timerCycles++
        }
        return samples * 2
    }

    /** Renderiza [seconds] segundos de PCM estéreo intercalado (16-bit, 32 kHz). */
    fun render(seconds: Int): ShortArray = renderSamples(seconds * SAMPLE_RATE)

    /** Renderiza exactamente [samples] muestras estéreo. */
    fun renderSamples(samples: Int): ShortArray {
        val out = ShortArray(samples * 2)
        renderInto(out, 0, samples)
        return out
    }

    // ------------------------------------------------------------- inicialización

    private fun initialize() {
        dsp.reset()
        spcReset()
    }

    private fun spcReset() {
        java.util.Arrays.fill(ram, 0, 0x500, 0)
        for (c in channel) resetChannel(c)
        resetPlayerFields()
        inputPorts.fill(0)
        for (i in 11 downTo 0) dspWrite(DEF_DSP_REGS[i], DEF_DSP_VALUES[i])
        tempo = setHi(tempo, 0x36)
    }

    private fun resetChannel(c: Channel) {
        c.patternCurPtr = 0; c.noteTicksLeft = 0; c.volumeFadeTicks = 0; c.panNumTicks = 0
        c.pitchSlideLength = 0; c.pitchSlideDelayLeft = 0; c.vibratoHoldCount = 0; c.vibDepth = 0
        c.tremoloHoldCount = 0; c.tremoloDepth = 0; c.subroutineNumLoops = 0; c.instrumentId = 0
        c.noteKeyoffTicksLeft = 0; c.vibratoChangeCount = 0; c.noteLength = 0; c.noteGateOffFixedpt = 0
        c.instrumentPitchBase = 0; c.channelVolumeMaster = 0; c.channelVolume = 0; c.volumeFadeAddpertick = 0
        c.volumeFadeTarget = 0; c.panValue = 0; c.panAddPerTick = 0; c.panTargetValue = 0
        c.panFlagWithPhaseInvert = 0; c.pitch = 0; c.pitchAddPerTick = 0; c.pitchTarget = 0; c.fineTune = 0
        c.pitchEnvelopeNumTicks = 0; c.pitchEnvelopeDelay = 0; c.pitchEnvelopeDirection = 0
        c.pitchEnvelopeSlideValue = 0; c.vibratoCount = 0; c.vibratoRate = 0; c.vibratoDelayTicks = 0
        c.vibratoFadeNumTicks = 0; c.vibratoFadeAddPerTick = 0; c.vibDepthOrig = 0; c.tremoloCount = 0
        c.tremoloDelayTicks = 0; c.finalVolume = 0; c.savedPatternPtr = 0; c.patternStartPtr = 0
    }

    private fun resetPlayerFields() {
        portToSnes.fill(0); lastValueFromSnes.fill(0); newValueFromSnes.fill(0)
        counterSf0c = 0; isChanOn = 0; chanBitFlags = 0; musicPtrToplevel = 0; blockCount = 0
        globalTransposition = 0; curChanBit = 0; keyOn = 0; mainTempoAccum = 0; tempo = 0
        tempoFadeNumTicks = 0; tempoFadeFinal = 0; tempoFadeAdd = 0; masterVolume = 0
        masterVolumeFadeTicks = 0; masterVolumeFadeTarget = 0; masterVolumeFadeAddPerTick = 0
        volDirty = 0; echoVolumeFadeTicks = 0; echoVolumeLeft = 0; echoVolumeRight = 0
        echoVolumeFadeAddLeft = 0; echoVolumeFadeAddRight = 0; echoVolumeFadeTargetLeft = 0
        echoVolumeFadeTargetRight = 0; smwTempoIncrease = 0; smwPauseMusic = 0; smwPlayerOnYoshi = 0
        echoChannels = 0; didAffectVolumePitchFlag = 0; timerCycles = 0
    }

    // ------------------------------------------------------------- driver de tick

    private fun spcLoopPart2(ticks: Int) {
        // Bloque SFX omitido intencionadamente (solo música).
        val prod = (ticks * hi(tempo)) and 0xff
        val t = mainTempoAccum + prod
        mainTempoAccum = t and 0xff
        if (t >= 256) {
            if (smwPauseMusic == 0) musicProcess()
            readPortFromSnes(2)
        } else if (portToSnes[2] != 0) {
            curChanBit = 0x80
            for (i in 7 downTo 0) {
                if (hi(channel[i].patternCurPtr) != 0) handlePanAndSweep(channel[i])
                curChanBit = curChanBit shr 1
            }
        }
    }

    private fun readPortFromSnes(port: Int) {
        val old = lastValueFromSnes[port]
        lastValueFromSnes[port] = inputPorts[port]
        newValueFromSnes[port] = if (inputPorts[port] != old) inputPorts[port] else 0
    }

    // ------------------------------------------------------------- Music_Process

    private fun musicProcess() {
        if (portToSnes[2] != 0) {
            if ((portToSnes[2] == 6 || (portToSnes[2] and 0xfc) == 0) && smwPlayerOnYoshi == 0) {
                dspWrite(KOF, 0x20)
                isChanOn = isChanOn or 0x20
            } else {
                isChanOn = isChanOn and 0x20.inv() and 0xff
            }
        }
        val cmd = newValueFromSnes[2]
        if (cmd.toByte() <= 0) {
            if (cmd.toByte() < 0) {
                masterVolumeFadeTicks = 0xf0
                masterVolumeFadeTarget = 0
                masterVolumeFadeAddPerTick = spcDivHelper(-hi(masterVolume), 0xf0)
            }
            if (counterSf0c != 0) {
                if ((--counterSf0c) != 0) return
                musicLoopAndNotes(enterAtLabelA = false)
                return
            }
            if (portToSnes[2] != 0) {
                musicLoopAndNotes(enterAtLabelA = true)
                return
            }
            return
        }

        // Nueva orden de canción (cmd 1..127).
        if (cmd == 0x16 || cmd == 0x10 || cmd == 0xf || cmd in 9..12) smwTempoIncrease = 0
        portToSnes[2] = cmd
        counterSf0c = 2
        musicPtrToplevel = word(0x1360 + (cmd - 1) * 2)
        for (i in 7 downTo 0) {
            val c = channel[i]
            c.panValue = setHi(c.panValue, 10)
            c.channelVolume = setHi(c.channelVolume, 0xff)
            c.fineTune = 0
            c.panNumTicks = 0
            c.volumeFadeTicks = 0
            c.vibDepth = 0
            c.tremoloDepth = 0
            c.subroutineNumLoops = 0
            c.instrumentId = 0
        }
        masterVolumeFadeTicks = 0
        echoVolumeFadeTicks = 0
        tempoFadeNumTicks = 0
        globalTransposition = 0
        masterVolume = setHi(masterVolume, 0xc0)
        tempo = setHi(tempo, 0x36)
        for (c in channel) { c.pitchEnvelopeNumTicks = 0; c.pitchEnvelopeDelay = 0 }
        dspWrite(KOF, isChanOn.inv() and 0xff)
    }

    /**
     * Porta el bloque `Music_loop_1` + `label_a`: carga el siguiente bloque de
     * patrones (si toca) y procesa un tick de cada canal. Si [enterAtLabelA],
     * entra directo a procesar sin recargar bloque (música ya corriendo).
     */
    private fun musicLoopAndNotes(enterAtLabelA: Boolean) {
        var skipLoad = enterAtLabelA
        outer@ while (true) {
            if (!skipLoad) {
                // Music_loop_1
                if (musicPtrToplevel == 0) return
                var t: Int
                while (true) {
                    t = word(musicPtrToplevel)
                    musicPtrToplevel = (musicPtrToplevel + 2) and 0xffff
                    if ((t shr 8) != 0) break
                    if (t == 0) {
                        portToSnes[2] = 0
                        dspWrite(KOF, isChanOn.inv() and 0xff)
                        return
                    }
                    blockCount = (blockCount - 1) and 0xff
                    if (blockCount == 0) {
                        musicPtrToplevel = (musicPtrToplevel + 2) and 0xffff
                    } else {
                        if (blockCount and 0x80 != 0) blockCount = t
                        musicPtrToplevel = word(musicPtrToplevel)
                    }
                }
                for (i in 7 downTo 0) channel[i].patternCurPtr = word(t + i * 2)
                curChanBit = 0x80
                for (i in 7 downTo 0) {
                    if (hi(channel[i].patternCurPtr) != 0) {
                        channel[i].noteTicksLeft = 1
                        if (channel[i].instrumentId == 0) channelSetInstrument(channel[i], 1)
                    }
                    curChanBit = curChanBit shr 1
                }
            }
            skipLoad = false

            // label_a
            keyOn = 0
            var restart = false
            for (i in 0 until 8) {
                curChanBit = 1 shl i
                val c = channel[i]
                if (hi(c.patternCurPtr) == 0) continue
                if ((--c.noteTicksLeft) != 0) {
                    handleNoteTick(c)
                    continue
                }
                // command loop
                var broke = false
                while (true) {
                    var cmd = ram(c.patternCurPtr); c.patternCurPtr = (c.patternCurPtr + 1) and 0xffff
                    if (cmd == 0) {
                        if (c.subroutineNumLoops == 0) { restart = true; break }
                        c.subroutineNumLoops = (c.subroutineNumLoops - 1) and 0xff
                        c.patternCurPtr = if (c.subroutineNumLoops == 0) c.savedPatternPtr else c.patternStartPtr
                        continue
                    }
                    if (cmd and 0x80 == 0) {
                        c.noteLength = cmd
                        cmd = ram(c.patternCurPtr); c.patternCurPtr = (c.patternCurPtr + 1) and 0xffff
                        if (cmd and 0x80 == 0) {
                            c.noteGateOffFixedpt = NOTE_GATE_OFF_PCT[(cmd shr 4) and 7]
                            c.channelVolumeMaster = NOTE_VOL[cmd and 0xf]
                            volDirty = volDirty or curChanBit
                            cmd = ram(c.patternCurPtr); c.patternCurPtr = (c.patternCurPtr + 1) and 0xffff
                        }
                    }
                    if (cmd >= 0xda) {
                        handleEffect(c, cmd)
                        continue
                    }
                    if (isChanOn and curChanBit == 0) playNote(c, cmd)
                    c.noteTicksLeft = c.noteLength
                    val tt = (c.noteTicksLeft * c.noteGateOffFixedpt) shr 8
                    c.noteKeyoffTicksLeft = if (tt != 0) tt else 1
                    broke = true
                    break
                }
                if (restart) break
                @Suppress("UNUSED_EXPRESSION") broke
            }
            if (restart) continue@outer
            break
        }

        // cola del label_a
        if (tempoFadeNumTicks != 0) {
            tempoFadeNumTicks = (tempoFadeNumTicks - 1) and 0xff
            tempo = if (tempoFadeNumTicks == 0) (tempoFadeFinal shl 8) else (tempo + tempoFadeAdd) and 0xffff
        }
        if (echoVolumeFadeTicks != 0) {
            echoVolumeFadeTicks = (echoVolumeFadeTicks - 1) and 0xff
            if (echoVolumeFadeTicks == 0) {
                echoVolumeLeft = echoVolumeLeft and 0xff00
                echoVolumeRight = echoVolumeRight and 0xff00
            } else {
                echoVolumeLeft = (echoVolumeLeft + echoVolumeFadeAddLeft) and 0xffff
                echoVolumeRight = (echoVolumeRight + echoVolumeFadeAddRight) and 0xffff
            }
            setEchoVolume()
        }
        if (masterVolumeFadeTicks != 0) {
            masterVolumeFadeTicks = (masterVolumeFadeTicks - 1) and 0xff
            masterVolume = if (masterVolumeFadeTicks == 0) (masterVolumeFadeTarget shl 8)
            else (masterVolume + masterVolumeFadeAddPerTick) and 0xffff
            volDirty = 0xff
        }
        curChanBit = 0x80
        for (i in 7 downTo 0) {
            if (hi(channel[i].patternCurPtr) != 0) chanHandleTick(channel[i])
            curChanBit = curChanBit shr 1
        }
        volDirty = 0
        writeKeyOn(keyOn and isChanOn.inv())
    }

    // ------------------------------------------------------------- notas y tono

    private fun playNote(c: Channel, noteIn: Int) {
        var note = noteIn
        if (note >= 0xd0) {
            c.instrumentId = note
            val addr = (note - 0xd0) * 6 + 0x5fa5
            if (channelSetInstrumentEx(c, addr)) return
            note = ram(addr + 5)
        } else if (note >= 0xc6) {
            return
        }
        c.pitch = (((note and 0x7f) + globalTransposition) shl 8 or c.fineTune) and 0xffff
        c.vibratoCount = 0
        c.tremoloCount = 0
        c.vibratoHoldCount = 0
        c.vibratoChangeCount = 0
        c.tremoloHoldCount = 0
        volDirty = volDirty or curChanBit
        keyOn = keyOn or curChanBit
        c.pitchSlideLength = c.pitchEnvelopeNumTicks
        if (c.pitchSlideLength != 0) {
            c.pitchSlideDelayLeft = c.pitchEnvelopeDelay
            if (c.pitchEnvelopeDirection == 0) c.pitch = (c.pitch - (c.pitchEnvelopeSlideValue shl 8)) and 0xffff
            computePitchAdd(c, (c.pitch shr 8) + c.pitchEnvelopeSlideValue)
        }
        writePitch(c, c.pitch)
    }

    private fun writePitch(c: Channel, pitchIn: Int) {
        var pitch = pitchIn and 0xffff
        if ((pitch shr 8) >= 0x34) {
            pitch = (pitch + ((pitch shr 8) - 0x34)) and 0xffff
        } else if ((pitch shr 8) < 0x13) {
            pitch = (pitch + (((((pitch shr 8) - 0x13) * 2) and 0xff) - 256)) and 0xffff
        }
        var t = computePeriod(pitch shr 8)
        val t1 = computePeriod((pitch shr 8) + 1)
        val td = (t1 - t) and 0xffff
        t = (t + (td shr 8) * (pitch and 0xff)) and 0xffff
        t = (t + (((td and 0xff) * (pitch and 0xff)) shr 8)) and 0xffff
        t = (t * c.instrumentPitchBase) and 0xffff
        if (curChanBit and isChanOn == 0) {
            val reg = c.index * 16
            dspWrite(reg + V0PITCHL, t and 0xff)
            dspWrite(reg + V0PITCHH, t shr 8)
            if (debugCapturePitch) debugPitchWrites.add((c.index shl 16) or (t and 0x3fff))
        }
    }

    private fun computePeriod(v: Int): Int {
        val pp = v and 0x7f
        var q = pp / 12
        val r = pp % 12
        var t = BASE_NOTE_FREQS[r]
        while (q != 6) { t = t shr 1; q++ }
        return t and 0xffff
    }

    private fun writeKeyOn(a: Int) {
        dspWrite(KOF, 0)
        dspWrite(KON, a and 0xff)
    }

    private fun channelSetInstrument(c: Channel, instrument: Int) {
        c.instrumentId = instrument
        channelSetInstrumentEx(c, 0x5F46 + (instrument - 1) * 5)
    }

    private fun channelSetInstrumentEx(c: Channel, addr: Int): Boolean {
        if (isChanOn and curChanBit != 0) return true
        chanBitFlags = chanBitFlags and curChanBit.inv() and 0xff
        dspWrite(NON, chanBitFlags)
        val reg = c.index * 16
        dspWrite(reg + V0SRCN, ram(addr))
        dspWrite(reg + V0ADSR1, ram(addr + 1))
        dspWrite(reg + V0ADSR2, ram(addr + 2))
        dspWrite(reg + V0GAIN, ram(addr + 3))
        c.instrumentPitchBase = ram(addr + 4)
        return false
    }

    private fun computePitchAdd(c: Channel, pitch: Int) {
        c.pitchTarget = pitch and 0x7f
        c.pitchAddPerTick = spcDivHelper(c.pitchTarget - (c.pitch shr 8), c.pitchSlideLength)
    }

    // ------------------------------------------------------------- efectos

    private fun handleEffect(c: Channel, effect: Int) {
        when (effect) {
            0xda -> channelSetInstrument(c, ramNext(c) + 1)
            0xdb -> {
                val arg = ramNext(c)
                c.panValue = (arg and 0x1f) shl 8
                c.panFlagWithPhaseInvert = arg and 0xc0
                volDirty = volDirty or curChanBit
            }
            0xdc -> {
                c.panNumTicks = ramNext(c)
                c.panTargetValue = ramNext(c)
                c.panAddPerTick = spcDivHelper(c.panTargetValue - (c.panValue shr 8), c.panNumTicks)
            }
            0xde -> {
                c.vibratoDelayTicks = ramNext(c)
                c.vibratoFadeNumTicks = 0
                c.vibratoRate = ramNext(c)
                c.vibDepth = ramNext(c)
            }
            0xdf -> c.vibDepth = 0
            0xe0 -> { masterVolume = ramNext(c) shl 8; volDirty = 0xff }
            0xe1 -> {
                masterVolumeFadeTicks = ramNext(c)
                masterVolumeFadeTarget = ramNext(c)
                masterVolumeFadeAddPerTick = spcDivHelper(masterVolumeFadeTarget - (masterVolume shr 8), masterVolumeFadeTicks)
            }
            0xe2 -> tempo = (ramNext(c) + smwTempoIncrease + 1) shl 8
            0xe3 -> {
                tempoFadeNumTicks = ramNext(c)
                tempoFadeFinal = ramNext(c) + smwTempoIncrease + 1
                tempoFadeAdd = spcDivHelper(tempoFadeFinal - (tempo shr 8), tempoFadeNumTicks)
            }
            0xe4 -> globalTransposition = ramNext(c)
            0xe5 -> {
                c.tremoloDelayTicks = ramNext(c)
                c.tremoloDepth = ramNext(c)
            }
            0xe6 -> c.tremoloDepth = 0
            0xe7 -> { c.channelVolume = ramNext(c) shl 8; volDirty = volDirty or curChanBit }
            0xe8 -> {
                c.volumeFadeTicks = ramNext(c)
                c.volumeFadeTarget = ramNext(c)
                c.volumeFadeAddpertick = spcDivHelper(c.volumeFadeTarget - (c.channelVolume shr 8), c.volumeFadeTicks)
            }
            0xe9 -> {
                var t = ramNext(c)
                t = t or (ramNext(c) shl 8)
                c.subroutineNumLoops = ramNext(c)
                c.savedPatternPtr = c.patternCurPtr
                c.patternCurPtr = t
                c.patternStartPtr = t
            }
            0xea -> {
                c.vibratoFadeNumTicks = ramNext(c)
                c.vibDepthOrig = c.vibDepth
                c.vibratoFadeAddPerTick = if (c.vibratoFadeNumTicks != 0) c.vibDepth / c.vibratoFadeNumTicks else 0xff
            }
            0xeb -> { c.pitchEnvelopeDirection = 1; pitchEnv(c) }
            0xec -> { c.pitchEnvelopeDirection = 0; pitchEnv(c) }
            0xee -> c.fineTune = ramNext(c)
            0xef -> {
                echoChannels = ramNext(c)
                dspWrite(EON, echoChannels)
                echoVolumeLeft = ramNext(c) shl 8
                echoVolumeRight = ramNext(c) shl 8
                dspWrite(FLG, 0)
                setEchoVolume()
            }
            0xf0 -> { echoChannels = 0; setEchoOff() }
            0xf1 -> {
                dspWrite(EDL, ramNext(c))
                dspWrite(EFB, ramNext(c))
                val base = ramNext(c) * 8
                for (i in 0 until 8) dspWrite(FIR0 + i * 16, ECHO_FIR_PARAMS[base + i])
            }
            0xf2 -> {
                echoVolumeFadeTicks = ramNext(c)
                echoVolumeFadeTargetLeft = ramNext(c)
                echoVolumeFadeTargetRight = ramNext(c)
                echoVolumeFadeAddLeft = spcDivHelper(echoVolumeFadeTargetLeft - (echoVolumeLeft shr 8), echoVolumeFadeTicks)
                echoVolumeFadeAddRight = spcDivHelper(echoVolumeFadeTargetRight - (echoVolumeRight shr 8), echoVolumeFadeTicks)
            }
            // otros: no implementado (tremolo avanzado, etc.) -> no-op
        }
    }

    private fun pitchEnv(c: Channel) {
        c.pitchEnvelopeDelay = ramNext(c)
        c.pitchEnvelopeNumTicks = ramNext(c)
        c.pitchEnvelopeSlideValue = ramNext(c)
    }

    // ------------------------------------------------------------- tick de canal

    private fun chanHandleTick(c: Channel) {
        if (c.volumeFadeTicks != 0) {
            volDirty = volDirty or curChanBit
            c.volumeFadeTicks = (c.volumeFadeTicks - 1) and 0xff
            c.channelVolume = chanDoAnyFade(c.channelVolume, c.volumeFadeAddpertick, c.volumeFadeTarget, c.volumeFadeTicks)
        }
        if (c.tremoloDepth != 0) {
            if (c.tremoloDelayTicks == c.tremoloHoldCount) {
                volDirty = volDirty or curChanBit
                if (c.tremoloCount and 0x80 != 0 && c.tremoloDepth == 0xff) c.tremoloCount = 0x80
                calcTremolo(c)
            } else {
                c.tremoloHoldCount = (c.tremoloHoldCount + 1) and 0xff
                calcFinalVolume(c, c.channelVolumeMaster)
            }
        } else {
            calcFinalVolume(c, c.channelVolumeMaster)
        }
        if (c.panNumTicks != 0) {
            c.panNumTicks = (c.panNumTicks - 1) and 0xff
            c.panValue = chanDoAnyFade(c.panValue, c.panAddPerTick, c.panTargetValue, c.panNumTicks)
        } else if (volDirty and curChanBit == 0) {
            return
        }
        writeVolumeToDsp(c, c.panValue)
    }

    private fun writeVolumeToDsp(c: Channel, volumeIn: Int) {
        if (isChanOn and curChanBit != 0) return
        var volume = volumeIn and 0xffff
        for (i in 0 until 2) {
            var j = volume shr 8
            if (j > 20) j = 20
            var t = (VOLUME_TABLE[j] + ((VOLUME_TABLE[j + 1] - VOLUME_TABLE[j]) * (volume and 0xff) shr 8)) and 0xff
            t = (t * c.finalVolume shr 8) and 0xff
            if (((c.panFlagWithPhaseInvert shl i) and 0x80) != 0) t = (-t) and 0xff
            dspWrite(V0VOLL + i + c.index * 16, t)
            volume = (0x1400 - volume) and 0xffff
        }
    }

    private fun handleNoteTick(c: Channel) {
        c.noteKeyoffTicksLeft = (c.noteKeyoffTicksLeft - 1) and 0xff
        if (c.noteKeyoffTicksLeft == 0 || c.noteTicksLeft == 2) {
            if (wantWriteKof(c) && (curChanBit and isChanOn) == 0) dspWrite(KOF, curChanBit)
        }
        didAffectVolumePitchFlag = 0
        if (c.pitchSlideLength == 0 || (isChanOn and curChanBit != 0)) {
            if (ram(c.patternCurPtr) != 0xdd) {
                afterPitchThing(c)
                return
            }
            if (curChanBit and isChanOn != 0) {
                c.patternCurPtr = (c.patternCurPtr + 4) and 0xffff
            } else {
                c.patternCurPtr = (c.patternCurPtr + 1) and 0xffff
                c.pitchSlideDelayLeft = ramNext(c)
                c.pitchSlideLength = ramNext(c)
                computePitchAdd(c, ramNext(c) + globalTransposition)
            }
        }
        if (c.pitchSlideDelayLeft != 0) {
            c.pitchSlideDelayLeft = (c.pitchSlideDelayLeft - 1) and 0xff
        } else if (isChanOn and curChanBit == 0) {
            didAffectVolumePitchFlag = 0x80
            c.pitchSlideLength = (c.pitchSlideLength - 1) and 0xff
            c.pitch = chanDoAnyFade(c.pitch, c.pitchAddPerTick, c.pitchTarget, c.pitchSlideLength)
        }
        afterPitchThing(c)
    }

    private fun afterPitchThing(c: Channel) {
        val pitch = c.pitch
        if (c.vibDepth != 0) {
            if (c.vibratoDelayTicks == c.vibratoHoldCount) {
                if (c.vibratoFadeNumTicks != 0) {
                    if (c.vibratoChangeCount == c.vibratoFadeNumTicks) {
                        c.vibDepth = c.vibDepthOrig
                    } else {
                        val base = if (c.vibratoChangeCount == 0) 0 else c.vibDepth
                        c.vibratoChangeCount = (c.vibratoChangeCount + 1) and 0xff
                        c.vibDepth = (base + c.vibratoFadeAddPerTick) and 0xff
                    }
                }
                c.vibratoCount = (c.vibratoCount + c.vibratoRate) and 0xff
                calcVibratoAddPitch(c, pitch, c.vibratoCount)
                return
            }
            c.vibratoHoldCount = (c.vibratoHoldCount + 1) and 0xff
        }
        if (didAffectVolumePitchFlag != 0) writePitch(c, pitch)
    }

    private fun calcVibratoAddPitch(c: Channel, pitch: Int, value: Int) {
        var t = (value shl 2) and 0xffff
        if (t and 0x100 != 0) t = t xor 0xff
        val r = if (c.vibDepth >= 0xf1) (t and 0xff) * (c.vibDepth and 0xf)
        else ((t and 0xff) * c.vibDepth) shr 8
        writePitch(c, (pitch + (if (value and 0x80 != 0) -r else r)) and 0xffff)
    }

    private fun calcTremolo(c: Channel) {
        // No implementado en el motor de referencia (assert). No-op.
        calcFinalVolume(c, c.channelVolumeMaster)
    }

    private fun handlePanAndSweep(c: Channel) {
        didAffectVolumePitchFlag = 0
        var volume = c.panValue
        if (c.panNumTicks != 0) {
            didAffectVolumePitchFlag = 0x80
            volume = (volume + mainTempoAccum * signed16(c.panAddPerTick) / 256) and 0xffff
        }
        if (didAffectVolumePitchFlag != 0) writeVolumeToDsp(c, volume)
        didAffectVolumePitchFlag = 0
        var pitch = c.pitch
        if (c.pitchSlideLength != 0 && c.pitchSlideDelayLeft == 0) {
            didAffectVolumePitchFlag = 0x80
            pitch = (pitch + mainTempoAccum * signed16(c.pitchAddPerTick) / 256) and 0xffff
        }
        if (c.vibDepth != 0 && c.vibratoDelayTicks == c.vibratoHoldCount) {
            calcVibratoAddPitch(c, pitch, ((mainTempoAccum * c.vibratoRate) shr 8) + c.vibratoCount)
            return
        }
        if (didAffectVolumePitchFlag != 0) writePitch(c, pitch)
    }

    private fun calcFinalVolume(c: Channel, vol: Int) {
        var t = (vol * (c.channelVolume shr 8)) shr 8
        t = ((masterVolume shr 8) * t) shr 8
        c.finalVolume = (t * t shr 8) and 0xff
    }

    // ------------------------------------------------------------- eco

    private fun setEchoVolume() {
        dspWrite(EVOLL, echoVolumeLeft shr 8)
        dspWrite(EVOLR, echoVolumeRight shr 8)
    }

    private fun setEchoOff() {
        echoVolumeLeft = 0
        echoVolumeRight = 0
        setEchoVolume()
        dspWrite(FLG, 0x20)
    }

    // ------------------------------------------------------------- utilidades

    private fun wantWriteKof(c: Channel): Boolean {
        var ptr = c.patternCurPtr
        while (true) {
            var cmd = ram(ptr); ptr = (ptr + 1) and 0xffff
            if (cmd == 0) return true
            while (cmd and 0x80 == 0) { cmd = ram(ptr); ptr = (ptr + 1) and 0xffff }
            if (cmd == 0xc6) return false
            if (cmd < 0xda) return true
            ptr = (ptr + EFFECT_BYTE_LENGTH[cmd - 0xda] - 1) and 0xffff
        }
    }

    private fun chanDoAnyFade(cur: Int, add: Int, target: Int, cont: Int): Int =
        if (cont == 0) (target shl 8) and 0xffff else (cur + add) and 0xffff

    private fun spcDivHelper(aIn: Int, b: Int): Int {
        val orgA = aIn
        var a = aIn
        if (a and 0x100 != 0) a = -a
        val bb = b and 0xff
        val q = if (bb != 0) (a and 0xff) / bb else 0xff
        val r = if (bb != 0) (a and 0xff) % bb else (a and 0xff)
        val t = (q shl 8) + (if (bb != 0) ((r shl 8) / bb) and 0xff else 0xff)
        return (if (orgA and 0x100 != 0) -t else t) and 0xffff
    }

    /** DEBUG: cuenta bits de KEYON escritos (note-ons) para medir stutter/retrigger. */
    var debugKeyonBits = 0
        private set

    /** DEBUG: captura de los valores de registro PITCH del DSP (canal<<16 | pitch14). */
    var debugCapturePitch = false
    val debugPitchWrites = ArrayList<Int>()

    private fun dspWrite(reg: Int, value: Int) {
        if ((reg and 0x7f) == 0x4C) debugKeyonBits += Integer.bitCount(value and 0xff)
        dsp.write(reg and 0x7f, value and 0xff)
    }

    private fun ram(a: Int): Int = ram[a and 0xffff].toInt() and 0xff
    private fun ramNext(c: Channel): Int {
        val v = ram(c.patternCurPtr)
        c.patternCurPtr = (c.patternCurPtr + 1) and 0xffff
        return v
    }
    private fun word(a: Int): Int = ram(a) or (ram(a + 1) shl 8)
    private fun hi(x: Int): Int = (x shr 8) and 0xff
    private fun setHi(x: Int, v: Int): Int = (x and 0xff) or ((v and 0xff) shl 8)
    private fun signed16(x: Int): Int = x.toShort().toInt()

    companion object {
        /** Frecuencia de muestreo nativa del DSP (Hz). */
        const val SAMPLE_RATE = 32000

        // Registros DSP relevantes.
        private const val V0VOLL = 0x00
        private const val V0PITCHL = 0x02
        private const val V0PITCHH = 0x03
        private const val V0SRCN = 0x04
        private const val V0ADSR1 = 0x05
        private const val V0ADSR2 = 0x06
        private const val V0GAIN = 0x07
        private const val MVOLL = 0x0C
        private const val EFB = 0x0D
        private const val FIR0 = 0x0F
        private const val MVOLR = 0x1C
        private const val EVOLL = 0x2C
        private const val PMON = 0x2D
        private const val EVOLR = 0x3C
        private const val NON = 0x3D
        private const val KON = 0x4C
        private const val EON = 0x4D
        private const val KOF = 0x5C
        private const val DIR = 0x5D
        private const val FLG = 0x6C
        private const val ESA = 0x6D
        private const val EDL = 0x7D

        private val DEF_DSP_REGS = intArrayOf(MVOLL, MVOLR, EVOLL, EVOLR, FLG, EFB, PMON, NON, EON, DIR, ESA, EDL)
        private val DEF_DSP_VALUES = intArrayOf(0x7F, 0x7F, 0, 0, 0x2F, 0x60, 0, 0, 0, 0x80, 0x60, 2)

        private val BASE_NOTE_FREQS = intArrayOf(4286, 4541, 4811, 5097, 5400, 5721, 6061, 6422, 6804, 7208, 7637, 8091)
        private val NOTE_VOL = intArrayOf(8, 18, 27, 36, 44, 53, 62, 71, 81, 90, 98, 107, 125, 143, 161, 179)
        private val NOTE_GATE_OFF_PCT = intArrayOf(51, 102, 128, 153, 179, 204, 230, 255)
        private val VOLUME_TABLE = intArrayOf(0, 1, 3, 7, 13, 21, 30, 41, 52, 66, 81, 94, 103, 110, 115, 119, 122, 124, 125, 126, 127, 127)
        private val EFFECT_BYTE_LENGTH = intArrayOf(
            2, 2, 3, 4, 4, 1, 2, 3, 2, 3, 2, 4, 1, 2, 3, 4,
            2, 4, 4, 1, 2, 4, 1, 4, 4,
        )
        private val ECHO_FIR_PARAMS = intArrayOf(
            -1, 8, 23, 36, 36, 23, 8, -1,
            127, 0, 0, 0, 0, 0, 0, 0,
        )
    }
}
