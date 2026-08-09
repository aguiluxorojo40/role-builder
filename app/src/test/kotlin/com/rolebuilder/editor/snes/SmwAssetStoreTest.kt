package com.rolebuilder.editor.snes

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PRIMEROS tests del módulo `app`. Hasta ahora tenía CERO: los 66 tests vivían en `core`, así
 * que el horneado y el almacén —justo donde estaban los bugs que llegaron al usuario (cuadros
 * rojos en vez de enemigos, música muda)— no tenían ninguna red. Cada fallo visual lo
 * descubría él jugando, no el CI.
 *
 * Se prueba la lógica sobre carpetas de verdad (java.io.File), sin Context ni Bitmap, así que
 * corren en JVM pura: rápidos y sin emulador.
 */
class SmwAssetStoreTest {

    private val tmp: File = Files.createTempDirectory("smwstore").toFile()

    @After fun cleanup() { tmp.deleteRecursively() }

    private fun put(rel: String, bytes: ByteArray = byteArrayOf(1, 2, 3)) {
        val f = File(tmp, rel)
        f.parentFile?.mkdirs()
        f.writeBytes(bytes)
    }

    @Test
    fun `almacen vacio no esta horneado`() {
        assertFalse(SmwAssetStore.isBakedAt(tmp))
    }

    /**
     * EL BUG DE LA MÚSICA MUDA: un almacén horneado con una versión ANTERIOR tenía
     * `enemies.png`, así que `isBaked` decía "true" y nunca se re-horneaba — la música nueva
     * no llegaba jamás. Aquí queda fijado: versión vieja = NO horneado.
     */
    @Test
    fun `un almacen de version anterior cuenta como NO horneado`() {
        put("sprites/enemies.png")
        put(".bake_version", "1".toByteArray())
        assertFalse(
            "con version antigua debe re-hornear, si no los assets nuevos no llegan",
            SmwAssetStore.isBakedAt(tmp),
        )
    }

    @Test
    fun `con la version actual si esta horneado`() {
        put("sprites/enemies.png")
        put(".bake_version", SmwAssetStore.bakeVersion().toString().toByteArray())
        assertTrue(SmwAssetStore.isBakedAt(tmp))
    }

    @Test
    fun `sin marca de version tampoco cuenta como horneado`() {
        put("sprites/enemies.png") // fichero presente pero sin sellar
        assertFalse(SmwAssetStore.isBakedAt(tmp))
    }

    /**
     * EL BUG DE LOS CUADROS ROJOS: si `sprites/enemies.png` no está, el editor pinta un cuadro
     * rojo. El estado tiene que DECIRLO, no callar.
     */
    @Test
    fun `el estado delata que faltan los enemigos`() {
        put("sprites/mario.png")
        put("sprites/coin.png")
        val estado = SmwAssetStore.statusAt(tmp).toMap()
        assertEquals(false, estado["enemigos"])
        assertEquals(true, estado["Mario"])
        assertEquals(true, estado["moneda"])
        assertEquals(false, estado["música"])
    }

    @Test
    fun `el estado da todo OK cuando el almacen esta completo`() {
        put("sprites/enemies.png"); put("sprites/mario.png"); put("sprites/coin.png")
        put("sprites/big/big_a0.png"); put("music/level.aram")
        assertTrue(
            "con todo presente no debe quedar ningún ✗",
            SmwAssetStore.statusAt(tmp).all { it.second },
        )
    }

    @Test
    fun `los sprites grandes se leen por su id hexadecimal`() {
        put("sprites/big/big_a0.png")   // Bowser
        put("sprites/big/big_29.png")   // Koopaling
        put("sprites/big/no_es_valido.png")
        val big = SmwAssetStore.bigSpriteFilesAt(tmp)
        assertEquals(setOf(0xA0, 0x29), big.keys)
    }

    /** El parte del horneado debe EXPLICAR el fallo, no devolver un 0 mudo. */
    @Test
    fun `el parte del horneado dice que fallo y por que`() {
        val report = SmwAssetStore.BakeReport(
            listOf(
                SmwAssetStore.BakeStep("sprites", 5, true, "5 ficheros"),
                SmwAssetStore.BakeStep("música", 0, false, "la ROM no ensambla el banco"),
            ),
        )
        assertFalse(report.ok)
        assertEquals(5, report.count)
        assertEquals(1, report.failures.size)
        val s = report.summary()
        assertTrue("debe nombrar el paso fallido: $s", s.contains("música"))
        assertTrue("debe dar el motivo: $s", s.contains("no ensambla"))
    }

    @Test
    fun `un horneado sin fallos se resume como completo`() {
        val report = SmwAssetStore.BakeReport(
            listOf(SmwAssetStore.BakeStep("sprites", 7, true, "7 ficheros")),
        )
        assertTrue(report.ok)
        assertEquals(7, report.count)
        assertTrue(report.summary().contains("7"))
    }

    @Test
    fun `la version de horneado sube cuando cambia la geometria del atlas`() {
        // El atlas de enemigos tiene UNA COLUMNA por id de curatedIds, y el renderer calcula
        // el ancho de columna como 1/curatedIds.size. Si se anaden ids sin subir BAKE_VERSION,
        // el almacen viejo no se re-hornea y el atlas cacheado tiene menos columnas de las que
        // el renderer da por hechas: se desplazan TODAS y todos los enemigos salen cortados.
        //
        // Este test ata las dos cosas: al cambiar el numero de ids curados hay que subir la
        // version. Si falla, sube BAKE_VERSION y actualiza el numero de aqui.
        val idsEsperados = 37
        val versionEsperada = 9
        // JUnit4: el mensaje va PRIMERO.
        assertEquals(
            "cambio el numero de enemigos curados: sube BAKE_VERSION y este numero",
            idsEsperados,
            com.rolebuilder.core.snes.SmwEnemyGraphics.curatedIds.size,
        )
        assertEquals(versionEsperada, SmwAssetStore.bakeVersion())
    }

    @Test
    fun `la version de horneado sube tambien cuando cambian los enemigos de DIBUJO PROPIO`() {
        // El mismo peligro por otra puerta, y esta no la cubria el test de arriba: los ids
        // de dibujo propio se hornean como sprites GRANDES sueltos (big_<id>.png). Anadir
        // uno sin subir BAKE_VERSION no descuadra el atlas, pero deja el sprite nuevo sin
        // hornear en cualquier movil que ya tuviera almacen: el enemigo sigue saliendo
        // como rectangulo aunque el catalogo lo tenga.
        val idsEsperados = 26
        assertEquals(
            "cambio el numero de enemigos con dibujo propio: sube BAKE_VERSION y este numero",
            idsEsperados,
            com.rolebuilder.core.snes.SmwEnemyGraphics.customEnemyIds.size,
        )
        assertEquals(9, SmwAssetStore.bakeVersion())
    }
}
