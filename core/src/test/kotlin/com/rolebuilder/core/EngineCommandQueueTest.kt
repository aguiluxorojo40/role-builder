package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.model.EquipSlot
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tiles
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La cola de órdenes UI→motor (AUDITORIA §4): el motor tickea en el hilo de render
 * mientras la UI usa objetos, equipa y compra desde el suyo.
 *
 * Estos tests existen porque el arreglo, sin ellos, sería justo lo que el §3 critica:
 * un cambio que *dice* resolver una carrera de datos y que nadie comprueba. Un test
 * secuencial no serviría —pasaría igual con el código roto de antes—, así que los de
 * aquí o bien usan de verdad dos hilos, o bien fijan el momento EXACTO en que una
 * orden surte efecto, que es lo que el diseño promete.
 *
 * Objetos de la base por defecto: 1 = Poción (cura 10, precio 10), 3 = Espada
 * (arma, 30), 4 = Escudo (armadura, 20).
 */
class EngineCommandQueueTest {

    private fun mapaLlano(): GameMap = GameMap.empty(1, "m", 14, 14, fillTile = Tiles.GRASS)

    // =========================================================================
    // Cuándo se aplican las órdenes
    // =========================================================================

    @Test
    fun `una orden de otro hilo no toca el estado hasta el siguiente tick`() {
        // El corazón del diseño: solo el hilo del motor escribe el estado. Este test
        // es determinista porque el join garantiza que la orden ya está encolada
        // antes de mirar, sin depender de ningún tiempo de espera.
        val engine = engine(listOf(mapaLlano()))
        engine.state.hp = 5
        engine.state.addItem(1, 1)

        var aceptada = false
        thread { aceptada = engine.useItem(1) }.join()

        assertTrue(aceptada, "la petición era válida y debe aceptarse")
        assertEquals(5, engine.state.hp, "el hilo de UI NO puede haber curado por su cuenta")
        assertEquals(1, engine.state.itemCount(1), "ni haber gastado la poción")

        engine.tick(1 / 60f)

        assertEquals(15, engine.state.hp, "el motor aplica la orden al tickear")
        assertEquals(0, engine.state.itemCount(1), "y ahí es donde se gasta la poción")
    }

    @Test
    fun `desde el propio hilo del motor la orden surte efecto en el acto`() {
        // La suite y la UI antes del primer frame llaman desde el hilo dueño: ahí
        // se drena en el acto, y por eso los 690 tests que ya existían siguen
        // valiendo sin tocarlos.
        val engine = engine(listOf(mapaLlano()))
        engine.state.hp = 5
        engine.state.addItem(1, 1)

        assertTrue(engine.useItem(1))

        assertEquals(15, engine.state.hp, "en el hilo del motor no hace falta esperar al tick")
    }

    @Test
    fun `una orden que deja de ser valida entre encolar y aplicar se descarta`() {
        // Entre pedir y aplicar el mundo cambia. La validación del hilo de UI es
        // optimista por definición; la que manda es la del motor.
        val engine = engine(listOf(mapaLlano()))
        engine.state.hp = 5
        engine.state.addItem(1, 1)

        var aceptada = false
        thread { aceptada = engine.useItem(1) }.join()
        assertTrue(aceptada, "al pedirla era válida: había una poción")

        // Se gasta la poción por otra vía antes de que el motor drene.
        engine.state.addItem(1, -1)
        engine.tick(1 / 60f)

        assertEquals(5, engine.state.hp, "sin poción, la orden se descarta en vez de curar de la nada")
    }

    @Test
    fun `las ordenes se aplican tambien con el motor en pausa`() {
        // El menú de pausa es JUSTO donde se usan objetos y se equipa: si el drenaje
        // viviera detrás del return de la pausa, esas órdenes se quedarían colgadas
        // hasta cerrar el menú, o para siempre si el jugador sale desde él.
        val engine = engine(listOf(mapaLlano()))
        engine.state.hp = 5
        engine.state.addItem(1, 1)
        engine.paused = true

        thread { engine.useItem(1) }.join()
        engine.tick(1 / 60f)

        assertEquals(15, engine.state.hp, "en pausa también se drenan las órdenes")
    }

    // =========================================================================
    // Concurrencia de verdad
    // =========================================================================

    @Test
    fun `varios hilos encolando mientras el motor tickea no rompen nada`() {
        // La ConcurrentModificationException que este diseño viene a evitar: antes,
        // la UI escribía state.items desde su hilo mientras el motor lo recorría.
        val engine = engine(listOf(mapaLlano()))
        engine.state.hp = 1
        engine.state.gold = 10_000
        engine.state.addItem(1, 500)
        engine.state.addItem(3, 10)
        engine.state.addItem(4, 10)

        val fallos = ConcurrentLinkedQueue<Throwable>()
        val enMarcha = CountDownLatch(1)
        val hilos = (1..4).map { n ->
            thread {
                try {
                    enMarcha.await()
                    repeat(200) {
                        when (n) {
                            1 -> engine.useItem(1)
                            2 -> engine.equip(EquipSlot.WEAPON, 3)
                            3 -> engine.equip(EquipSlot.ARMOR, 4)
                            else -> engine.buyItem(1)
                        }
                    }
                } catch (e: Throwable) {
                    fallos.add(e)
                }
            }
        }

        enMarcha.countDown()
        repeat(400) { engine.tick(1 / 60f) }
        hilos.forEach { it.join() }
        repeat(50) { engine.tick(1 / 60f) } // drena lo que quedase pendiente

        assertTrue(fallos.isEmpty(), "hilos rotos: ${fallos.map { it::class.simpleName to it.message }}")
    }

    @Test
    fun `leer el inventario publicado mientras el motor tickea nunca revienta`() {
        // La UI dibuja el inventario cada frame desde su hilo. Por eso el motor
        // publica una foto inmutable: recorrer state.items directamente es la
        // carrera de datos original.
        val engine = engine(listOf(mapaLlano()))
        engine.state.gold = 10_000
        engine.state.addItem(1, 50)

        val fallos = ConcurrentLinkedQueue<Throwable>()
        val seguir = AtomicBoolean(true)
        val lector = thread {
            try {
                while (seguir.get()) {
                    // Recorrerlo entero es lo que reventaría con un mapa mutable.
                    engine.inventory.forEach { it.itemId + it.count }
                }
            } catch (e: Throwable) {
                fallos.add(e)
            }
        }

        repeat(300) { i ->
            if (i % 3 == 0) engine.buyItem(1) else engine.state.addItem(1, 1)
            engine.tick(1 / 60f)
        }
        seguir.set(false)
        lector.join()

        assertTrue(fallos.isEmpty(), "leer el inventario publicado falló: ${fallos.firstOrNull()}")
    }

    @Test
    fun `el oro nunca queda negativo aunque se compre desde varios hilos`() {
        // La prueba de que quien manda es la re-validación del motor: cuatro hilos
        // pidiendo compras de 10 sobre 50 de oro. Si el motor no revalidara al
        // aplicar, el oro se iría por debajo de cero.
        val engine = engine(listOf(mapaLlano()))
        engine.state.gold = 50

        val hilos = (1..4).map {
            thread { repeat(100) { engine.buyItem(1) } }
        }
        repeat(200) { engine.tick(1 / 60f) }
        hilos.forEach { it.join() }
        repeat(50) { engine.tick(1 / 60f) }

        assertFalse(engine.state.gold < 0, "el oro quedó en ${engine.state.gold}")
        // Lo comprado y lo gastado tienen que cuadrar: 10 de oro por poción.
        assertEquals(50, engine.state.gold + engine.state.itemCount(1) * 10, "oro y pociones no cuadran")
    }
}
