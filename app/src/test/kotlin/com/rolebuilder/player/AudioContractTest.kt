package com.rolebuilder.player

import com.rolebuilder.core.model.SoundEffects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El contrato de sonido entre `:core` y `:app`, que hasta ahora no vigilaba nadie.
 *
 * Es la prueba que faltaba cuando "coin" y "levelup" se quedaron mudos (AUDITORIA
 * §2.1): el motor los encolaba, la tabla de la app no los tenía y la app los
 * descartaba **en silencio**. Los tests existentes no podían verlo porque cada
 * módulo probaba su propia mitad — `:core` su cola, `:app` su tabla— y el fallo
 * estaba justo en medio, en que las dos mitades no coincidían.
 *
 * Corre en JVM pura, sin dispositivo: por eso [SoundSynth] está separado de
 * `SoundFx`, que es quien toca Android.
 */
class AudioContractTest {

    @Test
    fun `la app cubre todos los efectos que core puede pedir`() {
        val sinCubrir = SoundEffects.ALL - SoundSynth.NAMES.toSet()
        assertTrue(
            "core pide efectos que la app no registra y sonarían MUDOS: $sinCubrir",
            sinCubrir.isEmpty(),
        )
    }

    /**
     * El de arriba es hoy tautológico —`NAMES` **es** `SoundEffects.ALL`— y está
     * a propósito: si alguien vuelve a escribir la lista a mano en la app, deja
     * de serlo y este test recupera su mordida sin que haya que acordarse de él.
     *
     * El que de verdad muerde hoy es este: que cada nombre tenga su propia forma
     * de onda. Añadir una constante a `SoundEffects` sin su rama en `synthesize`
     * compila, no rompe nada visible y suena a pitido genérico en el juego. Aquí
     * se ve.
     */
    @Test
    fun `cada efecto tiene su propia sintesis y no cae en el pitido generico`() {
        val generico = SoundSynth.GENERICO
        val caidos = SoundEffects.ALL.filter { SoundSynth.synthesize(it).contentEquals(generico) }
        assertTrue(
            "estos efectos no tienen rama propia en synthesize() y suenan a pitido: $caidos",
            caidos.isEmpty(),
        )
    }

    @Test
    fun `un nombre libre desconocido cae en el pitido generico en vez de en silencio`() {
        // EventCommand.PlaySound acepta texto libre: un proyecto editado a mano
        // puede pedir cualquier cosa, y lo que NO debe hacer es enmudecer.
        val pcm = SoundSynth.synthesize("un-sonido-que-no-existe")
        assertTrue("un nombre desconocido no puede producir silencio", pcm.isNotEmpty())
        assertTrue(pcm.contentEquals(SoundSynth.GENERICO))
    }

    @Test
    fun `no hay nombres repetidos ni vacios en la lista de core`() {
        assertEquals(
            "hay efectos repetidos en SoundEffects.ALL",
            SoundEffects.ALL.size.toLong(),
            SoundEffects.ALL.toSet().size.toLong(),
        )
        assertFalse("hay un efecto con nombre vacío", SoundEffects.ALL.any { it.isBlank() })
    }

    @Test
    fun `el wav generado tiene cabecera RIFF valida y el tamano declarado`() {
        val pcm = SoundSynth.synthesize(SoundEffects.COIN)
        val wav = SoundSynth.wav(pcm)

        assertEquals("RIFF", String(wav.copyOfRange(0, 4)))
        assertEquals("WAVE", String(wav.copyOfRange(8, 12)))
        // 44 bytes de cabecera + PCM16: si esto se desalinea, SoundPool no carga
        // el fichero y el efecto enmudece igual que en el bug original.
        assertEquals((44 + pcm.size * 2).toLong(), wav.size.toLong())
    }
}
