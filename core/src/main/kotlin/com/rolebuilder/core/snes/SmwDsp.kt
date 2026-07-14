package com.rolebuilder.core.snes

/**
 * Emulador puro-Kotlin del **S-DSP** del SNES (chip de audio de la APU), portado
 * fielmente desde `snes/dsp.c` de la reimplementación C de *Super Mario World*
 * (snesrev/smw). No requiere emular la CPU SPC700: es solo el *mixer* de audio.
 *
 * Modela los 8 canales de voz con:
 *  - Decodificación **BRR** (bloques de 9 bytes) desde la ARAM.
 *  - Interpolación **Gaussiana** de 4 puntos por paso de tono.
 *  - Envolvente **ADSR** y modos **GAIN**.
 *  - **Ruido** LFSR, **modulación de tono** (PMON) y **eco** con filtro FIR de 8 taps.
 *
 * Uso: se construye compartiendo la imagen de 64 KiB de la ARAM (donde viven las
 * muestras BRR y el búfer de eco). El secuenciador escribe registros con [write]
 * y por cada muestra de salida (32000 Hz) se llama a [cycle]; tras cada [cycle]
 * los campos [outL]/[outR] contienen la muestra estéreo de 16 bits con signo.
 *
 * Ver [SmwMusicRenderer] para el driver que combina este DSP con el secuenciador.
 */
class SmwDsp(
    /** Imagen de la RAM del APU (64 KiB). Compartida con el secuenciador. */
    private val apuRam: ByteArray,
) {
    /** Registros DSP espejo (0x80 bytes), como en el hardware. */
    val regs = IntArray(0x80)

    /** DEBUG: si es true, no se escriben las muestras de eco en la ARAM (para aislar
     *  si el buffer de eco está corrompiendo las muestras BRR). */
    var debugDisableEchoWrites = false

    /** DEBUG: máscara de canales AUDIBLES (bit i = canal i suena). 0xFF = todos. */
    var debugChannelMask = 0xFF

    /** DEBUG: rango [inicio, fin) de ARAM que el eco ha tocado con escrituras. */
    var debugEchoLo = 0x10000
        private set
    var debugEchoHi = 0
        private set

    /** Última muestra de salida estéreo (16 bits con signo) tras [cycle]. */
    var outL = 0
        private set
    var outR = 0
        private set

    private class Channel {
        var pitch = 0
        var pitchCounter = 0
        var pitchModulation = false
        val decodeBuffer = IntArray(19)
        var srcn = 0
        var decodeOffset = 0
        var previousFlags = 0
        var old = 0
        var older = 0
        var useNoise = false
        val adsrRates = IntArray(4)
        var rateCounter = 0
        var adsrState = 0
        var sustainLevel = 0
        var useGain = false
        var gainMode = 0
        var directGain = false
        var gainValue = 0
        var gain = 0
        var keyOn = false
        var keyOff = false
        var sampleOut = 0
        var volumeL = 0
        var volumeR = 0
        var echoEnable = false
    }

    private val channel = Array(8) { Channel() }

    private var dirPage = 0
    private var evenCycle = false
    private var mute = true
    private var resetFlag = true
    private var masterVolumeL = 0
    private var masterVolumeR = 0
    private var noiseSample = -0x4000
    private var noiseRate = 0
    private var noiseCounter = 0
    private var echoWrites = false
    private var echoVolumeL = 0
    private var echoVolumeR = 0
    private var feedbackVolume = 0
    private var echoBufferAdr = 0
    private var echoDelay = 1
    private var echoRemain = 1
    private var echoBufferIndex = 0
    private var firBufferIndex = 0
    private val firValues = IntArray(8)
    private val firBufferL = IntArray(8)
    private val firBufferR = IntArray(8)

    init { reset() }

    fun reset() {
        regs.fill(0)
        regs[0x7c] = 0xff
        for (c in channel) {
            c.pitch = 0; c.pitchCounter = 0; c.pitchModulation = false
            c.decodeBuffer.fill(0); c.srcn = 0; c.decodeOffset = 0
            c.previousFlags = 0; c.old = 0; c.older = 0; c.useNoise = false
            c.adsrRates.fill(0); c.rateCounter = 0; c.adsrState = 0
            c.sustainLevel = 0; c.useGain = false; c.gainMode = 0
            c.directGain = false; c.gainValue = 0; c.gain = 0
            c.keyOn = false; c.keyOff = false; c.sampleOut = 0
            c.volumeL = 0; c.volumeR = 0; c.echoEnable = false
        }
        dirPage = 0; evenCycle = false; mute = true; resetFlag = true
        masterVolumeL = 0; masterVolumeR = 0
        noiseSample = -0x4000; noiseRate = 0; noiseCounter = 0
        echoWrites = false; echoVolumeL = 0; echoVolumeR = 0; feedbackVolume = 0
        echoBufferAdr = 0; echoDelay = 1; echoRemain = 1; echoBufferIndex = 0
        firBufferIndex = 0
        firValues.fill(0); firBufferL.fill(0); firBufferR.fill(0)
    }

    /** Avanza el DSP un ciclo de muestra; deja el resultado en [outL]/[outR]. */
    fun cycle() {
        var totalL = 0
        var totalR = 0
        for (i in 0 until 8) {
            cycleChannel(i)
            if (debugChannelMask and (1 shl i) == 0) continue // DEBUG: canal silenciado
            totalL += (channel[i].sampleOut * channel[i].volumeL) shr 6
            totalR += (channel[i].sampleOut * channel[i].volumeR) shr 6
            totalL = clamp16(totalL)
            totalR = clamp16(totalR)
        }
        totalL = clamp16((totalL * masterVolumeL) shr 7)
        totalR = clamp16((totalR * masterVolumeR) shr 7)
        handleEcho()
        outL = clamp16(totalL + echoAddL)
        outR = clamp16(totalR + echoAddR)
        if (mute) { outL = 0; outR = 0 }
        handleNoise()
        evenCycle = !evenCycle
    }

    private var echoAddL = 0
    private var echoAddR = 0

    private fun handleEcho() {
        val adr = echoBufferAdr + echoBufferIndex * 4
        firBufferL[firBufferIndex] =
            ((ram(adr) or (ram(adr + 1) shl 8)).toShort().toInt()) shr 1
        firBufferR[firBufferIndex] =
            ((ram(adr + 2) or (ram(adr + 3) shl 8)).toShort().toInt()) shr 1
        var sumL = 0
        var sumR = 0
        for (i in 0 until 8) {
            sumL += (firBufferL[(firBufferIndex + i + 1) and 0x7] * firValues[i]) shr 6
            sumR += (firBufferR[(firBufferIndex + i + 1) and 0x7] * firValues[i]) shr 6
            if (i == 6) {
                sumL = (sumL and 0xffff).toShort().toInt()
                sumR = (sumR and 0xffff).toShort().toInt()
            }
        }
        sumL = clamp16(sumL)
        sumR = clamp16(sumR)
        echoAddL = (sumL * echoVolumeL) shr 7
        echoAddR = (sumR * echoVolumeR) shr 7
        var inL = 0
        var inR = 0
        for (i in 0 until 8) {
            if (channel[i].echoEnable) {
                inL += (channel[i].sampleOut * channel[i].volumeL) shr 6
                inR += (channel[i].sampleOut * channel[i].volumeR) shr 6
                inL = clamp16(inL)
                inR = clamp16(inR)
            }
        }
        inL += (sumL * feedbackVolume) shr 7
        inR += (sumR * feedbackVolume) shr 7
        inL = clamp16(inL) and 0xfffe
        inR = clamp16(inR) and 0xfffe
        if (echoWrites && !debugDisableEchoWrites) {
            setRam(adr, inL and 0xff)
            setRam(adr + 1, (inL shr 8) and 0xff)
            setRam(adr + 2, inR and 0xff)
            setRam(adr + 3, (inR shr 8) and 0xff)
            if ((adr and 0xffff) < debugEchoLo) debugEchoLo = adr and 0xffff
            if ((adr and 0xffff) + 4 > debugEchoHi) debugEchoHi = (adr and 0xffff) + 4
        }
        firBufferIndex = (firBufferIndex + 1) and 7
        echoBufferIndex++
        echoRemain--
        if (echoRemain == 0) {
            echoRemain = echoDelay
            echoBufferIndex = 0
        }
    }

    private fun cycleChannel(ch: Int) {
        val c = channel[ch]
        var pitch = c.pitch
        if (ch > 0 && c.pitchModulation) {
            val factor = (channel[ch - 1].sampleOut shr 4) + 0x400
            pitch = (pitch * factor) shr 10
            if (pitch > 0x3fff) pitch = 0x3fff
        }
        val newCounter = c.pitchCounter + pitch
        if (newCounter > 0xffff) {
            decodeBrr(ch)
        }
        c.pitchCounter = newCounter and 0xffff
        var sample: Int
        if (c.useNoise) {
            sample = noiseSample
        } else {
            sample = getSample(ch, c.pitchCounter shr 12, (c.pitchCounter shr 4) and 0xff)
        }
        if (resetFlag) {
            c.adsrState = 4
            c.gain = 0
        }
        val doingDirectGain = c.adsrState != 4 && c.useGain && c.directGain
        val rate = if (c.adsrState == 4) 0 else c.adsrRates[c.adsrState]
        if (c.adsrState != 4 && !doingDirectGain && rate != 0) {
            c.rateCounter++
        }
        if (c.adsrState == 4 || (!doingDirectGain && c.rateCounter >= rate && rate != 0)) {
            if (c.adsrState != 4) c.rateCounter = 0
            handleGain(ch)
        }
        if (doingDirectGain) c.gain = c.gainValue
        regs[(ch shl 4) or 8] = c.gain shr 4
        sample = (sample * c.gain) shr 11
        regs[(ch shl 4) or 9] = (sample shr 7) and 0xff
        c.sampleOut = sample.toShort().toInt()
    }

    private fun handleGain(ch: Int) {
        val c = channel[ch]
        when (c.adsrState) {
            0 -> { // attack
                val rate = c.adsrRates[0]
                c.gain += if (rate == 1) 1024 else 32
                if (c.gain >= 0x7e0) c.adsrState = 1
                if (c.gain > 0x7ff) c.gain = 0x7ff
            }
            1 -> { // decay
                c.gain -= ((c.gain - 1) shr 8) + 1
                if (c.gain < c.sustainLevel) c.adsrState = 2
            }
            2 -> { // sustain
                c.gain -= ((c.gain - 1) shr 8) + 1
            }
            3 -> when (c.gainMode) { // gain
                0 -> { c.gain -= 32; if (c.gain > 0x7ff) c.gain = 0 }
                1 -> { c.gain -= ((c.gain - 1) shr 8) + 1 }
                2 -> { c.gain += 32; if (c.gain > 0x7ff) c.gain = 0x7ff }
                3 -> { c.gain += if (c.gain < 0x600) 32 else 8; if (c.gain > 0x7ff) c.gain = 0x7ff }
            }
            4 -> { // release
                c.gain -= 8
                if (c.gain > 0x7ff) c.gain = 0
            }
        }
    }

    private fun getSample(ch: Int, sampleNum: Int, offset: Int): Int {
        val b = channel[ch].decodeBuffer
        val news = b[sampleNum + 3]
        val olds = b[sampleNum + 2]
        val olders = b[sampleNum + 1]
        val oldests = b[sampleNum]
        var out = (GAUSS[0xff - offset] * oldests) shr 10
        out += (GAUSS[0x1ff - offset] * olders) shr 10
        out += (GAUSS[0x100 + offset] * olds) shr 10
        out = (out and 0xffff).toShort().toInt()
        out += (GAUSS[offset] * news) shr 10
        out = clamp16(out)
        return out shr 1
    }

    private fun decodeBrr(ch: Int) {
        val c = channel[ch]
        c.decodeBuffer[0] = c.decodeBuffer[16]
        c.decodeBuffer[1] = c.decodeBuffer[17]
        c.decodeBuffer[2] = c.decodeBuffer[18]
        if (c.previousFlags == 1 || c.previousFlags == 3) {
            val samplePointer = dirPage + 4 * c.srcn
            c.decodeOffset = ram(samplePointer + 2) or (ram(samplePointer + 3) shl 8)
            if (c.previousFlags == 1) {
                c.adsrState = 4
                c.gain = 0
            }
            regs[0x7c] = regs[0x7c] or (1 shl ch)
        }
        val header = ram(c.decodeOffset); c.decodeOffset = (c.decodeOffset + 1) and 0xffff
        val shift = header shr 4
        val filter = (header and 0xc) shr 2
        c.previousFlags = header and 0x3
        var curByte = 0
        var old = c.old
        var older = c.older
        for (i in 0 until 16) {
            var s: Int
            if (i and 1 != 0) {
                s = curByte and 0xf
            } else {
                curByte = ram(c.decodeOffset); c.decodeOffset = (c.decodeOffset + 1) and 0xffff
                s = curByte shr 4
            }
            if (s > 7) s -= 16
            s = if (shift <= 0xc) (s shl shift) shr 1 else (s shr 3) shl 12
            when (filter) {
                1 -> s += old + ((-old) shr 4)
                2 -> s += 2 * old + ((3 * -old) shr 5) - older + (older shr 4)
                3 -> s += 2 * old + ((13 * -old) shr 6) - older + ((3 * older) shr 4)
            }
            s = clamp16(s)
            s = (((s and 0x7fff) shl 1).toShort().toInt()) shr 1
            older = old
            old = s
            c.decodeBuffer[i + 3] = s
        }
        c.older = older
        c.old = old
    }

    private fun handleNoise() {
        if (noiseRate != 0) noiseCounter++
        if (noiseCounter >= noiseRate && noiseRate != 0) {
            val bit = (noiseSample and 1) xor ((noiseSample shr 1) and 1)
            noiseSample = ((noiseSample shr 1) and 0x3fff) or (bit shl 14)
            noiseSample = (((noiseSample and 0x7fff) shl 1).toShort().toInt()) shr 1
            noiseCounter = 0
        }
    }

    fun read(adr: Int): Int = regs[adr and 0x7f]

    /** Escribe el registro DSP [adr] (0x00..0x7f) con [value] (0..255). */
    fun write(adr: Int, value: Int) {
        val reg = adr and 0x7f
        val ch = reg shr 4
        val v = value and 0xff
        when (reg) {
            0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70 -> channel[ch].volumeL = v.toByte().toInt()
            0x01, 0x11, 0x21, 0x31, 0x41, 0x51, 0x61, 0x71 -> channel[ch].volumeR = v.toByte().toInt()
            0x02, 0x12, 0x22, 0x32, 0x42, 0x52, 0x62, 0x72 ->
                channel[ch].pitch = (channel[ch].pitch and 0x3f00) or v
            0x03, 0x13, 0x23, 0x33, 0x43, 0x53, 0x63, 0x73 ->
                channel[ch].pitch = ((channel[ch].pitch and 0x00ff) or (v shl 8)) and 0x3fff
            0x04, 0x14, 0x24, 0x34, 0x44, 0x54, 0x64, 0x74 -> channel[ch].srcn = v
            0x05, 0x15, 0x25, 0x35, 0x45, 0x55, 0x65, 0x75 -> {
                channel[ch].adsrRates[0] = RATES[(v and 0xf) * 2 + 1]
                channel[ch].adsrRates[1] = RATES[((v and 0x70) shr 4) * 2 + 16]
                channel[ch].useGain = (v and 0x80) == 0
            }
            0x06, 0x16, 0x26, 0x36, 0x46, 0x56, 0x66, 0x76 -> {
                channel[ch].adsrRates[2] = RATES[v and 0x1f]
                channel[ch].sustainLevel = (((v and 0xe0) shr 5) + 1) * 0x100
            }
            0x07, 0x17, 0x27, 0x37, 0x47, 0x57, 0x67, 0x77 -> {
                channel[ch].directGain = (v and 0x80) == 0
                if (v and 0x80 != 0) {
                    channel[ch].gainMode = (v and 0x60) shr 5
                    channel[ch].adsrRates[3] = RATES[v and 0x1f]
                } else {
                    channel[ch].gainValue = (v and 0x7f) * 16
                }
            }
            0x0c -> masterVolumeL = v.toByte().toInt()
            0x1c -> masterVolumeR = v.toByte().toInt()
            0x2c -> echoVolumeL = v.toByte().toInt()
            0x3c -> echoVolumeR = v.toByte().toInt()
            0x4c -> { // KON
                for (i in 0 until 8) {
                    if (v and (1 shl i) != 0) {
                        val c = channel[i]
                        c.previousFlags = 0
                        val samplePointer = dirPage + 4 * c.srcn
                        c.decodeOffset = ram(samplePointer) or (ram(samplePointer + 1) shl 8)
                        c.decodeBuffer.fill(0)
                        c.gain = 0
                        c.adsrState = if (c.useGain) 3 else 0
                    }
                }
            }
            0x5c -> { // KOF
                for (i in 0 until 8) {
                    if (v and (1 shl i) != 0) channel[i].adsrState = 4
                }
            }
            0x6c -> { // FLG
                resetFlag = v and 0x80 != 0
                mute = v and 0x40 != 0
                echoWrites = (v and 0x20) == 0
                noiseRate = RATES[v and 0x1f]
            }
            0x7c -> { regs[reg] = 0; return } // ENDx: any write clears
            0x0d -> feedbackVolume = v.toByte().toInt()
            0x2d -> for (i in 0 until 8) channel[i].pitchModulation = v and (1 shl i) != 0
            0x3d -> for (i in 0 until 8) channel[i].useNoise = v and (1 shl i) != 0
            0x4d -> for (i in 0 until 8) channel[i].echoEnable = v and (1 shl i) != 0
            0x5d -> dirPage = v shl 8
            0x6d -> echoBufferAdr = v shl 8
            0x7d -> { echoDelay = (v and 0xf) * 512; if (echoDelay == 0) echoDelay = 1 }
            0x0f, 0x1f, 0x2f, 0x3f, 0x4f, 0x5f, 0x6f, 0x7f -> firValues[ch] = v.toByte().toInt()
        }
        regs[reg] = v
    }

    private fun ram(adr: Int): Int = apuRam[adr and 0xffff].toInt() and 0xff
    private fun setRam(adr: Int, value: Int) { apuRam[adr and 0xffff] = value.toByte() }

    private fun clamp16(v: Int): Int = when {
        v < -0x8000 -> -0x8000
        v > 0x7fff -> 0x7fff
        else -> v
    }

    companion object {
        private val RATES = intArrayOf(
            0, 2048, 1536, 1280, 1024, 768, 640, 512,
            384, 320, 256, 192, 160, 128, 96, 80,
            64, 48, 40, 32, 24, 20, 16, 12,
            10, 8, 6, 5, 4, 3, 2, 1,
        )

        private val GAUSS = intArrayOf(
            0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000,
            0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x002, 0x002, 0x002, 0x002, 0x002,
            0x002, 0x002, 0x003, 0x003, 0x003, 0x003, 0x003, 0x004, 0x004, 0x004, 0x004, 0x004, 0x005, 0x005, 0x005, 0x005,
            0x006, 0x006, 0x006, 0x006, 0x007, 0x007, 0x007, 0x008, 0x008, 0x008, 0x009, 0x009, 0x009, 0x00A, 0x00A, 0x00A,
            0x00B, 0x00B, 0x00B, 0x00C, 0x00C, 0x00D, 0x00D, 0x00E, 0x00E, 0x00F, 0x00F, 0x00F, 0x010, 0x010, 0x011, 0x011,
            0x012, 0x013, 0x013, 0x014, 0x014, 0x015, 0x015, 0x016, 0x017, 0x017, 0x018, 0x018, 0x019, 0x01A, 0x01B, 0x01B,
            0x01C, 0x01D, 0x01D, 0x01E, 0x01F, 0x020, 0x020, 0x021, 0x022, 0x023, 0x024, 0x024, 0x025, 0x026, 0x027, 0x028,
            0x029, 0x02A, 0x02B, 0x02C, 0x02D, 0x02E, 0x02F, 0x030, 0x031, 0x032, 0x033, 0x034, 0x035, 0x036, 0x037, 0x038,
            0x03A, 0x03B, 0x03C, 0x03D, 0x03E, 0x040, 0x041, 0x042, 0x043, 0x045, 0x046, 0x047, 0x049, 0x04A, 0x04C, 0x04D,
            0x04E, 0x050, 0x051, 0x053, 0x054, 0x056, 0x057, 0x059, 0x05A, 0x05C, 0x05E, 0x05F, 0x061, 0x063, 0x064, 0x066,
            0x068, 0x06A, 0x06B, 0x06D, 0x06F, 0x071, 0x073, 0x075, 0x076, 0x078, 0x07A, 0x07C, 0x07E, 0x080, 0x082, 0x084,
            0x086, 0x089, 0x08B, 0x08D, 0x08F, 0x091, 0x093, 0x096, 0x098, 0x09A, 0x09C, 0x09F, 0x0A1, 0x0A3, 0x0A6, 0x0A8,
            0x0AB, 0x0AD, 0x0AF, 0x0B2, 0x0B4, 0x0B7, 0x0BA, 0x0BC, 0x0BF, 0x0C1, 0x0C4, 0x0C7, 0x0C9, 0x0CC, 0x0CF, 0x0D2,
            0x0D4, 0x0D7, 0x0DA, 0x0DD, 0x0E0, 0x0E3, 0x0E6, 0x0E9, 0x0EC, 0x0EF, 0x0F2, 0x0F5, 0x0F8, 0x0FB, 0x0FE, 0x101,
            0x104, 0x107, 0x10B, 0x10E, 0x111, 0x114, 0x118, 0x11B, 0x11E, 0x122, 0x125, 0x129, 0x12C, 0x130, 0x133, 0x137,
            0x13A, 0x13E, 0x141, 0x145, 0x148, 0x14C, 0x150, 0x153, 0x157, 0x15B, 0x15F, 0x162, 0x166, 0x16A, 0x16E, 0x172,
            0x176, 0x17A, 0x17D, 0x181, 0x185, 0x189, 0x18D, 0x191, 0x195, 0x19A, 0x19E, 0x1A2, 0x1A6, 0x1AA, 0x1AE, 0x1B2,
            0x1B7, 0x1BB, 0x1BF, 0x1C3, 0x1C8, 0x1CC, 0x1D0, 0x1D5, 0x1D9, 0x1DD, 0x1E2, 0x1E6, 0x1EB, 0x1EF, 0x1F3, 0x1F8,
            0x1FC, 0x201, 0x205, 0x20A, 0x20F, 0x213, 0x218, 0x21C, 0x221, 0x226, 0x22A, 0x22F, 0x233, 0x238, 0x23D, 0x241,
            0x246, 0x24B, 0x250, 0x254, 0x259, 0x25E, 0x263, 0x267, 0x26C, 0x271, 0x276, 0x27B, 0x280, 0x284, 0x289, 0x28E,
            0x293, 0x298, 0x29D, 0x2A2, 0x2A6, 0x2AB, 0x2B0, 0x2B5, 0x2BA, 0x2BF, 0x2C4, 0x2C9, 0x2CE, 0x2D3, 0x2D8, 0x2DC,
            0x2E1, 0x2E6, 0x2EB, 0x2F0, 0x2F5, 0x2FA, 0x2FF, 0x304, 0x309, 0x30E, 0x313, 0x318, 0x31D, 0x322, 0x326, 0x32B,
            0x330, 0x335, 0x33A, 0x33F, 0x344, 0x349, 0x34E, 0x353, 0x357, 0x35C, 0x361, 0x366, 0x36B, 0x370, 0x374, 0x379,
            0x37E, 0x383, 0x388, 0x38C, 0x391, 0x396, 0x39B, 0x39F, 0x3A4, 0x3A9, 0x3AD, 0x3B2, 0x3B7, 0x3BB, 0x3C0, 0x3C5,
            0x3C9, 0x3CE, 0x3D2, 0x3D7, 0x3DC, 0x3E0, 0x3E5, 0x3E9, 0x3ED, 0x3F2, 0x3F6, 0x3FB, 0x3FF, 0x403, 0x408, 0x40C,
            0x410, 0x415, 0x419, 0x41D, 0x421, 0x425, 0x42A, 0x42E, 0x432, 0x436, 0x43A, 0x43E, 0x442, 0x446, 0x44A, 0x44E,
            0x452, 0x455, 0x459, 0x45D, 0x461, 0x465, 0x468, 0x46C, 0x470, 0x473, 0x477, 0x47A, 0x47E, 0x481, 0x485, 0x488,
            0x48C, 0x48F, 0x492, 0x496, 0x499, 0x49C, 0x49F, 0x4A2, 0x4A6, 0x4A9, 0x4AC, 0x4AF, 0x4B2, 0x4B5, 0x4B7, 0x4BA,
            0x4BD, 0x4C0, 0x4C3, 0x4C5, 0x4C8, 0x4CB, 0x4CD, 0x4D0, 0x4D2, 0x4D5, 0x4D7, 0x4D9, 0x4DC, 0x4DE, 0x4E0, 0x4E3,
            0x4E5, 0x4E7, 0x4E9, 0x4EB, 0x4ED, 0x4EF, 0x4F1, 0x4F3, 0x4F5, 0x4F6, 0x4F8, 0x4FA, 0x4FB, 0x4FD, 0x4FF, 0x500,
            0x502, 0x503, 0x504, 0x506, 0x507, 0x508, 0x50A, 0x50B, 0x50C, 0x50D, 0x50E, 0x50F, 0x510, 0x511, 0x511, 0x512,
            0x513, 0x514, 0x514, 0x515, 0x516, 0x516, 0x517, 0x517, 0x517, 0x518, 0x518, 0x518, 0x518, 0x518, 0x519, 0x519,
        )
    }
}
