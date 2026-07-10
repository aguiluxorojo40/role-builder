package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwSfxCatalog
import com.rolebuilder.core.snes.SmwSoundFx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests SIN ROM del catálogo de SFX de SMW: selección de muestra por instrumento,
 * recorrido de secuencias, cálculo de tono ([SmwSfxCatalog.dspPitch]) y re-muestreo.
 * Toda la ARAM y el banco de muestras son sintéticos (formato del motor N-SPC), sin
 * ningún byte de la ROM. Los valores de tono/periodo están calculados a mano con las
 * mismas fórmulas `ComputePeriod`/`WritePitch` que documenta la clase.
 */
class SmwSfxCatalogTest {

    private fun w16(a: ByteArray, at: Int, v: Int) {
        a[at] = (v and 0xFF).toByte()
        a[at + 1] = ((v ushr 8) and 0xFF).toByte()
    }

    /** Escribe una entrada de instrumento (9 bytes) con su SRCN y pitch base. */
    private fun putInstrument(aram: ByteArray, instr: Int, srcn: Int, pitchBase: Int) {
        val base = SmwSfxCatalog.INSTRUMENT_TABLE + instr * 9
        aram[base + 4] = srcn.toByte()   // SRCN
        aram[base + 8] = pitchBase.toByte() // pitch base
    }

    private fun sample(index: Int, pcm: ShortArray) =
        SmwSoundFx.BrrSample(
            index = index, aramStart = 0x8100, aramLoop = 0x8100,
            brrByteLength = 9, pcm = pcm, loopStartSample = -1, hasLoop = false,
        )

    // ----------------------------------------------------------------- computePeriod

    @Test
    fun `computePeriod baja octava desplazando la frecuencia base`() {
        // pp=0x30 (48): q=4,r=0 -> base[0]=4286, dos shr -> 1071.
        assertEquals(1071, SmwSfxCatalog.computePeriod(0x30))
        // pp=0x31 (49): q=4,r=1 -> base[1]=4541, dos shr -> 1135.
        assertEquals(1135, SmwSfxCatalog.computePeriod(0x31))
        // pp=0x48 (72): q=6,r=0 -> sin shift -> 4286.
        assertEquals(4286, SmwSfxCatalog.computePeriod(0x48))
        // pp=0x24 (36): q=3,r=0 -> tres shr -> 535.
        assertEquals(535, SmwSfxCatalog.computePeriod(0x24))
    }

    // ----------------------------------------------------------------------- dspPitch

    @Test
    fun `dspPitch combina periodo de nota y pitch base`() {
        // nota 0x30 (sin recorte de rango, fracción 0): t0=1071.
        // pitchBase 0x04 -> 1071*4 = 4284 (≈0x1000, tono casi nativo).
        assertEquals(4284, SmwSfxCatalog.dspPitch(0x30, 0x04))
        // pitchBase 0x02 -> 1071*2 = 2142 (aprox. una octava por debajo).
        assertEquals(2142, SmwSfxCatalog.dspPitch(0x30, 0x02))
    }

    @Test
    fun `dspPitch recorta a 14 bits`() {
        val p = SmwSfxCatalog.dspPitch(0x60, 0x40)
        assertTrue(p in 0..0x3FFF, "el tono del DSP cabe en 14 bits, fue $p")
    }

    // --------------------------------------------------------------- resampleLinear

    @Test
    fun `resampleLinear a la misma frecuencia copia la senal`() {
        val src = shortArrayOf(1, 2, 3, 4)
        val out = SmwSfxCatalog.resampleLinear(src, 32000f, 32000f)
        assertEquals(src.toList(), out.toList())
    }

    @Test
    fun `resampleLinear a la mitad reduce el numero de muestras y conserva extremos`() {
        val src = shortArrayOf(0, 100, 200, 300)
        val out = SmwSfxCatalog.resampleLinear(src, 100f, 50f)
        assertEquals(2, out.size)
        assertEquals(0.toShort(), out.first())
        assertEquals(300.toShort(), out.last())
    }

    @Test
    fun `resampleLinear al doble interpola y conserva extremos`() {
        val src = shortArrayOf(0, 10)
        val out = SmwSfxCatalog.resampleLinear(src, 100f, 200f)
        assertEquals(4, out.size)
        assertEquals(0.toShort(), out.first())
        assertEquals(10.toShort(), out.last())
    }

    @Test
    fun `resampleLinear con entrada vacia o frecuencia invalida devuelve vacio`() {
        assertEquals(0, SmwSfxCatalog.resampleLinear(ShortArray(0), 1f, 1f).size)
        assertEquals(0, SmwSfxCatalog.resampleLinear(shortArrayOf(1, 2), 0f, 1f).size)
    }

    // -------------------------------------------------------- resolución de secuencia

    @Test
    fun `resolveSequenceVoice toma el instrumento y la primera nota`() {
        val aram = ByteArray(0x10000)
        // Secuencia en 0x6000: len=0x10, DA 05 (instrumento 5), nota 0x84.
        val seq = 0x6000
        aram[seq] = 0x10
        aram[seq + 1] = 0xDA.toByte()
        aram[seq + 2] = 0x05
        aram[seq + 3] = 0x84.toByte()
        val voice = SmwSfxCatalog.resolveSequenceVoice(aram, seq)
        assertNotNull(voice)
        assertEquals(5, voice.instrument)
        assertEquals(0x84, voice.note)
    }

    @Test
    fun `resolveSequenceVoice devuelve null si la secuencia termina sin nota`() {
        val aram = ByteArray(0x10000)
        aram[0x6000] = 0x00 // fin inmediato
        assertNull(SmwSfxCatalog.resolveSequenceVoice(aram, 0x6000))
    }

    @Test
    fun `resolveVoice sigue la tabla de punteros de Sfx3`() {
        val aram = ByteArray(0x10000)
        val seq = 0x6100
        // Puntero de Sfx3 id 1 -> seq.
        w16(aram, SmwSfxCatalog.SFX3_PTR_TABLE + 1 * 2, seq)
        aram[seq] = 0xDA.toByte()
        aram[seq + 1] = 0x07
        aram[seq + 2] = 0x90.toByte()
        val voice = SmwSfxCatalog.resolveVoice(aram, SmwSfxCatalog.SfxSource(3, 1))
        assertNotNull(voice)
        assertEquals(7, voice.instrument)
        assertEquals(0x90, voice.note)
    }

    @Test
    fun `resolveVoice del banco 1 usa los efectos cableados`() {
        val aram = ByteArray(0x10000)
        val jump = SmwSfxCatalog.resolveVoice(aram, SmwSfxCatalog.SfxSource(1, 1))
        assertNotNull(jump)
        assertEquals(8, jump.instrument) // salto: instrumento 8, nota 0xB2
        assertEquals(0xB2, jump.note)
    }

    // ----------------------------------------------------------------------- clipFor

    @Test
    fun `clipFor resuelve muestra por instrumento y produce PCM a la frecuencia pedida`() {
        val aram = ByteArray(0x10000)
        // COIN = Sfx3 id 1. Secuencia: DA 02 (instrumento 2), DD 30 (nota 0x30).
        val seq = 0x6200
        w16(aram, SmwSfxCatalog.SFX3_PTR_TABLE + 1 * 2, seq)
        aram[seq] = 0xDA.toByte()
        aram[seq + 1] = 0x02
        aram[seq + 2] = 0xDD.toByte()
        aram[seq + 3] = 0x30
        // Instrumento 2 -> muestra SRCN=3, pitchBase 0x04 (tono casi nativo).
        putInstrument(aram, 2, srcn = 3, pitchBase = 0x04)

        // Banco con una muestra en el índice 3 (256 muestras a 32000 Hz).
        val pcm = ShortArray(256) { (it % 50 - 25).toShort() }
        val bank = SmwSoundFx.SoundBank(listOf(sample(3, pcm)), 0x10000)

        val clip = SmwSfxCatalog.clipFor(aram, bank, SmwSfxCatalog.Event.COIN, outRate = 22050)
        assertNotNull(clip)
        assertEquals(SmwSfxCatalog.Event.COIN, clip.event)
        assertEquals(22050, clip.sampleRate)
        assertTrue(clip.pcm.isNotEmpty())
        // pitch≈4284/4096 -> tono efectivo ≈ 32000*1.046 = 33473 Hz; a 22050 el clip
        // se acorta respecto a las 256 muestras nativas.
        assertTrue(clip.pcm.size < pcm.size, "re-muestreado a menor frecuencia acorta el clip")
    }

    @Test
    fun `clipFor devuelve null si la muestra del instrumento no esta en el banco`() {
        val aram = ByteArray(0x10000)
        val seq = 0x6300
        w16(aram, SmwSfxCatalog.SFX3_PTR_TABLE + 1 * 2, seq)
        aram[seq] = 0xDA.toByte()
        aram[seq + 1] = 0x02
        aram[seq + 2] = 0xDD.toByte()
        aram[seq + 3] = 0x30
        putInstrument(aram, 2, srcn = 9, pitchBase = 0x04) // SRCN 9 no está en el banco
        val bank = SmwSoundFx.SoundBank(listOf(sample(0, shortArrayOf(1, 2, 3))), 0x10000)
        assertNull(SmwSfxCatalog.clipFor(aram, bank, SmwSfxCatalog.Event.COIN))
    }
}
