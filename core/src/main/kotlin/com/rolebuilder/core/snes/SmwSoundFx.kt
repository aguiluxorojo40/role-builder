package com.rolebuilder.core.snes

/**
 * Extractor de las MUESTRAS DE SONIDO (BRR) de Super Mario World y decodificador
 * BRR→PCM en Kotlin puro.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * DÓNDE VIVE EL SONIDO DE SMW (investigación sobre snesrev/smw y la ROM USA)
 * ─────────────────────────────────────────────────────────────────────────────
 * El audio de SMW corre en la APU (SPC700 + DSP). La CPU principal solo SUBE
 * bloques de datos a la RAM de audio (ARAM, 64 KB) y luego dispara sonidos
 * escribiendo IDs en los puertos APUI00..APUI03. En la desensamblada:
 *
 *   HandleSPCUploads_UploadSPCEngine()  -> sube `kSpcEngine`   (motor N-SPC)
 *   HandleSPCUploads_UploadSamples()    -> sube `kSpcSamples`  (MUESTRAS BRR)  <—
 *   ...MusicBank()                      -> sube secuencias de música
 *
 * `assets/compile_resources.py` de snesrev/smw fija las direcciones exactas
 * (direcciones SNES LoROM que su `get_bytes` traduce a offset de fichero):
 *
 *   kSpcEngine             = get_bytes(0xe8000, 6321)   -> SNES $0E:8000
 *   kSpcSamples            = get_bytes(0xf8000, 28538)  -> SNES $0F:8000   <— muestras
 *   kSpcOverworldMusicBank = get_bytes(0xe98b1, 5667)   -> SNES $0E:98B1
 *   kSpcLevelMusicBank     = get_bytes(0xeaed6, 16899)  -> SNES $0E:AED6
 *   kSpcCreditsMusicBank   = get_bytes(0x3e400, 6624)   -> SNES $03:E400
 *
 * FORMATO DE SUBIDA (estándar N-SPC / IPL): el bloque es una secuencia de
 * sub-bloques `[len:16 LE][destinoARAM:16 LE][len bytes de datos]`, terminada por
 * `[00 00]` (y opcionalmente un punto de entrada de 16 bits). Reconstruyendo la
 * ARAM desde `kSpcSamples` aparecen dos sub-bloques:
 *
 *   destino ARAM $8000, 80 bytes   -> DIRECTORIO DE MUESTRAS (registro DSP DIR = $80)
 *   destino ARAM $8100, 28448 bytes-> DATOS BRR de todas las muestras
 *
 * DIRECTORIO DE MUESTRAS (lo que lee el DSP por su registro DIR): 20 entradas de
 * 4 bytes `{inicioBRR:16 LE, inicioBucle:16 LE}`. Estas 20 muestras son el banco
 * compartido del que tiran TANTO los instrumentos de la música COMO los efectos
 * de sonido (el motor toca notas sobre estas muestras aplicando tono/barrido).
 *
 * CÓMO SE DISPARAN LOS SFX: la CPU escribe el id del efecto en APUI00 (SFX banco 1)
 * o APUI03 (SFX banco 3) — ver `SmwVectorNMI` (io_sound_ch1/ch2/ch3) y las rutinas
 * `Sfx0_Process`/`Sfx1_Process`/`Sfx3_Process` del motor SPC. Cada id indexa una
 * secuencia de notas dentro del motor que referencia una de estas muestras. La
 * lista id→muestra vive en el bloque `kSpcEngine`, fuera del alcance de este
 * extractor; aquí extraemos y decodificamos el BANCO DE MUESTRAS, que es la
 * materia prima de los SFX y los instrumentos.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * DECODIFICACIÓN BRR (bloque de 9 bytes)
 * ─────────────────────────────────────────────────────────────────────────────
 * Cada bloque son 9 bytes: 1 de cabecera + 8 de datos (16 nibbles de 4 bits con
 * signo, la muestra más antigua en el nibble alto del primer byte).
 *   cabecera: bits 7-4 = shift/range (0..12 válidos), bits 3-2 = filtro (0..3),
 *             bit 1 = flag de bucle, bit 0 = flag de FIN (último bloque).
 * Para cada nibble s con signo (-8..7):
 *   si range<=12:  s = (s << range) >> 1
 *   si range>12 :  s = (s and 0x7FF.inv())    // se satura a -2048 o 0
 * Luego el filtro IIR sobre las dos últimas salidas p1, p2 (coeficientes enteros
 * del DSP del SNES):
 *   f0: s += 0
 *   f1: s += p1 + ((-p1) >> 4)
 *   f2: s += (p1<<1) + ((-p1*3) >> 5) - p2 + (p2 >> 4)
 *   f3: s += (p1<<1) + ((-p1*13) >> 6) - p2 + ((p2*3) >> 4)
 * y se satura a 16 bits con signo. Es el algoritmo de snesbrr, el de referencia
 * para conversores BRR↔WAV. La muestra decodificada suena a su tono NATIVO a
 * 32000 Hz (paso de tono del DSP = 0x1000); el juego la repitcha en cada disparo.
 */
object SmwSoundFx {

    /** Dirección SNES LoROM del bloque de subida de muestras (`kSpcSamples`, $0F:8000). */
    const val SAMPLES_SNES_ADDR = 0x0F8000

    /** Longitud del bloque `kSpcSamples` en la ROM USA vanilla. */
    const val SAMPLES_BLOCK_LEN = 28538

    /** Registro DSP DIR de SMW: el directorio de muestras se sube a ARAM $8000. */
    const val SAMPLE_DIR_ARAM = 0x8000

    /** Tono nativo de una muestra BRR en el DSP del SNES (paso de tono 0x1000). */
    const val NATIVE_SAMPLE_RATE = 32000

    private const val BLOCK_BYTES = 9

    /**
     * Una muestra BRR decodificada a PCM de 16 bits con signo.
     *
     * @property index            índice en el directorio (0..N-1).
     * @property aramStart         dirección ARAM del primer bloque BRR.
     * @property aramLoop          dirección ARAM del bloque de bucle.
     * @property brrByteLength     tamaño en bytes de los datos BRR (múltiplo de 9).
     * @property pcm               PCM 16-bit con signo, tono nativo.
     * @property loopStartSample   índice de muestra PCM donde empieza el bucle
     *                             (o -1 si la muestra no está marcada como bucle).
     * @property hasLoop           true si el último bloque BRR tiene el flag de bucle.
     * @property sampleRate        Hz del PCM devuelto (siempre [NATIVE_SAMPLE_RATE]).
     */
    class BrrSample(
        val index: Int,
        val aramStart: Int,
        val aramLoop: Int,
        val brrByteLength: Int,
        val pcm: ShortArray,
        val loopStartSample: Int,
        val hasLoop: Boolean,
        val sampleRate: Int = NATIVE_SAMPLE_RATE,
    ) {
        /** Muestras de una sola vez, sin bucle: típicamente percusión y golpes de SFX. */
        val isOneShot: Boolean get() = !hasLoop
    }

    /** Catálogo completo de muestras extraído del banco de sonido de la ROM. */
    class SoundBank(
        val samples: List<BrrSample>,
        /** Copia de la imagen ARAM reconstruida (útil para depurar). */
        val aramImageSize: Int,
    )

    // ------------------------------------------------------------- extracción ROM

    /**
     * Localiza el bloque `kSpcSamples` en la ROM (mapeo LoROM), reconstruye la
     * imagen ARAM, lee el directorio de muestras y decodifica cada muestra BRR a
     * PCM. Devuelve null si la ROM no cubre el bloque o no tiene pinta de SMW
     * vanilla (directorio inválido).
     */
    fun extract(rom: ByteArray, header: SnesHeader): SoundBank? {
        val delta = header.headerOffset - 0x7FC0
        return extractAt(rom, snesToPc(SAMPLES_SNES_ADDR, delta))
    }

    /** Igual que [extract] pero con el offset de fichero (PC) del bloque ya resuelto. */
    fun extractAt(rom: ByteArray, blockPc: Int): SoundBank? {
        if (blockPc < 0 || blockPc + 4 > rom.size) return null
        val aram = ByteArray(0x10000)
        var dirDest = -1
        var dirLen = 0
        var p = blockPc
        // Reconstruye la ARAM aplicando cada sub-bloque [len][dest][datos].
        while (p + 4 <= rom.size) {
            val len = u16(rom, p)
            if (len == 0) break // terminador
            val dest = u16(rom, p + 2)
            p += 4
            if (p + len > rom.size) return null
            for (i in 0 until len) aram[(dest + i) and 0xFFFF] = rom[p + i]
            if (dest == SAMPLE_DIR_ARAM) {
                dirDest = dest
                dirLen = len
            }
            p += len
        }
        if (dirDest < 0 || dirLen < 4) return null

        val count = dirLen / 4
        val samples = ArrayList<BrrSample>(count)
        for (i in 0 until count) {
            val e = SAMPLE_DIR_ARAM + i * 4
            val start = u16(aram, e)
            val loop = u16(aram, e + 2)
            // Cordura: inicio dentro de la zona de datos y por debajo del bucle.
            if (start < SAMPLE_DIR_ARAM + dirLen || start > 0xFFFF - BLOCK_BYTES) return null
            samples.add(decodeSampleFromAram(aram, i, start, loop))
        }
        return SoundBank(samples, aram.size)
    }

    /**
     * Decodifica una muestra completa desde la imagen ARAM: recorre bloques de 9
     * bytes a partir de [start] hasta el bloque con flag de FIN, y calcula el
     * índice PCM del punto de bucle a partir de [loop].
     */
    private fun decodeSampleFromAram(aram: ByteArray, index: Int, start: Int, loop: Int): BrrSample {
        // Cuenta bloques hasta el flag de FIN (bit 0 de la cabecera).
        var end = start
        while (end + BLOCK_BYTES <= aram.size) {
            val h = aram[end].toInt() and 0xFF
            end += BLOCK_BYTES
            if (h and 0x01 != 0) break
        }
        val brrLen = end - start
        val lastHeader = aram[end - BLOCK_BYTES].toInt() and 0xFF
        val hasLoop = lastHeader and 0x02 != 0
        val pcm = decodeBrr(aram, start, end)
        // Cada bloque BRR = 16 muestras PCM. El punto de bucle en muestras:
        val loopStartSample = if (hasLoop && loop >= start) ((loop - start) / BLOCK_BYTES) * 16 else -1
        return BrrSample(index, start, loop, brrLen, pcm, loopStartSample, hasLoop)
    }

    // ------------------------------------------------------------- decodificador

    /**
     * Decodifica una tira de bloques BRR de 9 bytes en `data[start until end)` a
     * PCM de 16 bits con signo. Se detiene en `end` o antes si un bloque tiene el
     * flag de FIN. [prev1]/[prev2] son las dos muestras previas para el filtro IIR
     * (0 al empezar una muestra nueva). Función pura, ideal para tests.
     */
    fun decodeBrr(data: ByteArray, start: Int, end: Int = data.size, prev1: Int = 0, prev2: Int = 0): ShortArray {
        val out = ArrayList<Short>((end - start) / BLOCK_BYTES * 16 + 16)
        var p1 = prev1
        var p2 = prev2
        var pos = start
        while (pos + BLOCK_BYTES <= end) {
            val header = data[pos].toInt() and 0xFF
            val range = header ushr 4
            val filter = (header ushr 2) and 0x03
            for (bi in 0 until 8) {
                val byte = data[pos + 1 + bi].toInt() and 0xFF
                var half = 0
                while (half < 2) {
                    val nib = if (half == 0) (byte ushr 4) else (byte and 0x0F)
                    var s = if (nib >= 8) nib - 16 else nib // nibble con signo -8..7
                    s = if (range <= 12) (s shl range) shr 1 else (s and 0x7FF.inv())
                    when (filter) {
                        1 -> s += p1 + ((-p1) shr 4)
                        2 -> s += (p1 shl 1) + ((-(p1 * 3)) shr 5) - p2 + (p2 shr 4)
                        3 -> s += (p1 shl 1) + ((-(p1 * 13)) shr 6) - p2 + ((p2 * 3) shr 4)
                        // filtro 0: sin realimentación
                    }
                    s = clamp16(s)
                    out.add(s.toShort())
                    p2 = p1
                    p1 = s
                    half++
                }
            }
            pos += BLOCK_BYTES
            if (header and 0x01 != 0) break // flag de FIN
        }
        val arr = ShortArray(out.size)
        for (i in out.indices) arr[i] = out[i]
        return arr
    }

    // --------------------------------------------------------------------- utils

    /** Satura a entero de 16 bits con signo, como el DSP del SNES. */
    private fun clamp16(v: Int): Int = when {
        v > 32767 -> 32767
        v < -32768 -> -32768
        else -> v
    }

    private fun u16(a: ByteArray, i: Int): Int = (a[i].toInt() and 0xFF) or ((a[i + 1].toInt() and 0xFF) shl 8)

    /** LoROM: PC = banco*0x8000 + (dirección & 0x7FFF) + delta SMC. */
    private fun snesToPc(snesAddr: Int, delta: Int): Int {
        val bank = (snesAddr ushr 16) and 0xFF
        return bank * 0x8000 + (snesAddr and 0x7FFF) + delta
    }
}
