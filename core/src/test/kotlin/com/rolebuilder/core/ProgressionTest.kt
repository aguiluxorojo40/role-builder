package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.engine.RpgEngine
import com.rolebuilder.core.model.DefaultProjectFactory
import com.rolebuilder.core.model.EnemySpawn
import com.rolebuilder.core.model.EquipSlot
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Item
import com.rolebuilder.core.model.Tiles
import com.rolebuilder.core.model.event.Condition
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MapEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de la Fase 5: niveles y experiencia, equipo, oro y tienda.
 * El actor por defecto: maxHp 20, attack 5, defense 1, hpPerLevel 4,
 * attackPerLevel 1, defensePerLevel 1, expBase 8, maxLevel 20.
 */
class ProgressionTest {

    private fun emptyMap(): GameMap = GameMap.empty(1, "m", 12, 12, fillTile = Tiles.GRASS)

    private fun map(vararg events: MapEvent): GameMap = emptyMap().copy(events = events.toList())

    private fun actionEvent(x: Int, y: Int, vararg commands: EventCommand, id: Int = 1): MapEvent =
        MapEvent(id = id, x = x, y = y, pages = listOf(EventPage(trigger = EventTrigger.ACTION_BUTTON, commands = commands.toList())))

    /** Coloca el motor delante de un evento en (6,5) mirando a la derecha. */
    private fun engineFacing(vararg events: MapEvent): RpgEngine {
        val engine = engine(listOf(map(*events)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.inputX = 0f
        return engine
    }

    // ---- experiencia y niveles ----------------------------------------------

    @Test
    fun `gain exp levels up several times with stat growth and healing`() {
        val engine = engine(listOf(emptyMap()))
        engine.state.hp = 10

        // Umbrales: nivel 1→2 = 8, nivel 2→3 = 16. Con 30 exp: nivel 3 y sobran 6.
        engine.gainExp(30)

        assertEquals(3, engine.state.level)
        assertEquals(6, engine.state.exp)
        assertEquals(20 + 4 * 2, engine.state.maxHp, "maxHp crece 4 por nivel")
        assertEquals(10 + 4 * 2, engine.state.hp, "el HP ganado se cura")
        assertEquals(5 + 2, engine.effectiveAttack)
        assertEquals(1 + 2, engine.effectiveDefense)
        assertTrue(engine.soundQueue.contains("levelup"))
        assertEquals("¡Nivel 3!", engine.notice)
    }

    @Test
    fun `exp below threshold does not level up`() {
        val engine = engine(listOf(emptyMap()))
        engine.gainExp(7)
        assertEquals(1, engine.state.level)
        assertEquals(7, engine.state.exp)
        assertNull(engine.notice)
        assertFalse(engine.soundQueue.contains("levelup"))
    }

    @Test
    fun `level is capped at maxLevel`() {
        val engine = engine(listOf(emptyMap()))
        engine.gainExp(100000)
        assertEquals(20, engine.state.level)
        assertEquals(0, engine.state.exp, "en el nivel máximo no hay progreso")
        engine.gainExp(50)
        assertEquals(20, engine.state.level)
        assertEquals(0, engine.state.exp)
    }

    @Test
    fun `negative exp is ignored`() {
        val engine = engine(listOf(emptyMap()))
        engine.gainExp(5)
        engine.gainExp(-99)
        assertEquals(5, engine.state.exp)
        assertEquals(1, engine.state.level)
    }

    // ---- recompensas de combate ----------------------------------------------

    @Test
    fun `killing an enemy grants exp and gold`() {
        val map = emptyMap().copy(spawns = listOf(EnemySpawn(enemyId = 1, x = 6, y = 5)))
        val engine = engine(listOf(map), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.inputX = 0f

        repeat(10) {
            engine.pressAction()
            engine.run(0.5f)
        }
        assertTrue(engine.enemies.isEmpty(), "el slime murió")
        assertEquals(3, engine.state.exp, "expReward del slime")
        assertEquals(2, engine.state.gold, "goldReward del slime")
    }

    // ---- equipo ----------------------------------------------------------------

    @Test
    fun `equip and unequip weapon swaps with inventory and changes stats`() {
        val engine = engine(listOf(emptyMap()))
        engine.state.addItem(3, 1) // Espada de hierro (+2 ataque)

        assertTrue(engine.equip(EquipSlot.WEAPON, 3))
        assertEquals(3, engine.state.weaponItemId)
        assertEquals(0, engine.state.itemCount(3), "equipar consume 1 del inventario")
        assertEquals(5 + 2, engine.effectiveAttack)

        assertTrue(engine.equip(EquipSlot.WEAPON, null))
        assertNull(engine.state.weaponItemId)
        assertEquals(1, engine.state.itemCount(3), "desequipar devuelve el item")
        assertEquals(5, engine.effectiveAttack)
    }

    @Test
    fun `equip validates slot existence and inventory`() {
        val engine = engine(listOf(emptyMap()))
        engine.state.addItem(3, 1) // espada (WEAPON)

        assertFalse(engine.equip(EquipSlot.ARMOR, 3), "el slot no coincide")
        assertFalse(engine.equip(EquipSlot.WEAPON, 99), "el item no existe")
        assertFalse(engine.equip(EquipSlot.WEAPON, 4), "no está en el inventario")
        assertFalse(engine.equip(EquipSlot.WEAPON, 1), "la poción no es equipo")
        assertNull(engine.state.weaponItemId)
    }

    @Test
    fun `equipping over another weapon returns the old one to inventory`() {
        val db = DefaultProjectFactory.defaultDatabase()
        val dagger = Item(id = 9, name = "Daga", consumable = false, price = 10, equipSlot = EquipSlot.WEAPON, attackBonus = 1)
        val data = EngineTestSupport.project(listOf(emptyMap()), database = db.copy(items = db.items + dagger))
        val engine = RpgEngine(data)
        engine.state.addItem(3, 1)
        engine.state.addItem(9, 1)

        assertTrue(engine.equip(EquipSlot.WEAPON, 3))
        assertTrue(engine.equip(EquipSlot.WEAPON, 9))
        assertEquals(9, engine.state.weaponItemId)
        assertEquals(1, engine.state.itemCount(3), "la espada volvió al inventario")
        assertEquals(0, engine.state.itemCount(9))
        assertEquals(5 + 1, engine.effectiveAttack)
    }

    @Test
    fun `maxHp bonus equipment raises and lowers hp consistently`() {
        val db = DefaultProjectFactory.defaultDatabase()
        val amulet = Item(id = 9, name = "Amuleto", consumable = false, price = 50, equipSlot = EquipSlot.ARMOR, maxHpBonus = 5)
        val data = EngineTestSupport.project(listOf(emptyMap()), database = db.copy(items = db.items + amulet))
        val engine = RpgEngine(data)
        engine.state.addItem(9, 1)

        assertTrue(engine.equip(EquipSlot.ARMOR, 9))
        assertEquals(25, engine.state.maxHp)
        assertEquals(25, engine.state.hp, "si maxHp sube, el HP sube en la misma cantidad")

        assertTrue(engine.equip(EquipSlot.ARMOR, null))
        assertEquals(20, engine.state.maxHp)
        assertEquals(20, engine.state.hp, "si maxHp baja, el HP se recorta al máximo")
    }

    @Test
    fun `equipment defense reduces damage taken`() {
        // Slime pegado al jugador: ataque 2 - defensa efectiva.
        val map = emptyMap().copy(spawns = listOf(EnemySpawn(enemyId = 1, x = 6, y = 5)))
        val engine = engine(listOf(map), startX = 5, startY = 5)
        engine.state.addItem(4, 1) // Escudo de cuero (+1 defensa)
        assertTrue(engine.equip(EquipSlot.ARMOR, 4))
        assertEquals(1 + 1, engine.effectiveDefense)

        val hp0 = engine.state.hp
        engine.run(1.0f)
        // Con defensa 2 y ataque 2, cada golpe hace el mínimo de 1.
        assertTrue(hp0 - engine.state.hp in 1..2, "daño mínimo con escudo: ${hp0 - engine.state.hp}")
    }

    // ---- oro y tienda ----------------------------------------------------------

    @Test
    fun `gain gold never goes below zero`() {
        val engine = engine(listOf(emptyMap()))
        engine.gainGold(10)
        engine.gainGold(-25)
        assertEquals(0, engine.state.gold)
    }

    @Test
    fun `buy item with exact gold and fail without gold`() {
        val engine = engine(listOf(emptyMap()))
        engine.state.gold = 30

        assertTrue(engine.buyItem(3), "Espada de hierro a 30 monedas")
        assertEquals(0, engine.state.gold)
        assertEquals(1, engine.state.itemCount(3))
        assertTrue(engine.soundQueue.contains("coin"))

        assertFalse(engine.buyItem(3), "sin oro no se puede comprar")
        assertEquals(1, engine.state.itemCount(3))
    }

    @Test
    fun `sell item at half price rounding down`() {
        val engine = engine(listOf(emptyMap()))
        engine.state.addItem(1, 2) // Poción, precio 10

        assertTrue(engine.sellItem(1))
        assertEquals(5, engine.state.gold)
        assertEquals(1, engine.state.itemCount(1))

        // La llave no tiene precio: no se puede vender.
        engine.state.addItem(2, 1)
        assertFalse(engine.sellItem(2))

        // Sin existencias no se puede vender (el equipo equipado está fuera del inventario).
        assertTrue(engine.sellItem(1))
        assertFalse(engine.sellItem(1))
        assertEquals(10, engine.state.gold)
    }

    @Test
    fun `open shop blocks the interpreter until the ui closes it`() {
        val event = actionEvent(
            6, 5,
            EventCommand.OpenShop(itemIds = listOf(3, 4, 1)),
            EventCommand.SetSwitch(1, true),
        )
        val engine = engineFacing(event)
        engine.pressAction()
        engine.run(0.3f)

        val shop = engine.shop
        assertNotNull(shop)
        assertEquals(listOf(3, 4, 1), shop.itemIds)
        assertTrue(engine.uiBlocked, "la tienda bloquea la UI")
        assertFalse(engine.state.switchOn(1), "el intérprete espera a que se cierre la tienda")

        engine.closeShop()
        engine.run(0.1f)
        assertNull(engine.shop)
        assertTrue(engine.state.switchOn(1))
    }

    @Test
    fun `change gold and exp commands and gold condition via interpreter`() {
        val event = actionEvent(
            6, 5,
            EventCommand.ChangeGold(50),
            EventCommand.ChangeExp(8),
            EventCommand.ConditionalBranch(
                condition = Condition(Condition.Kind.GOLD_AT_LEAST, value = 40),
                then = listOf(EventCommand.SetSwitch(1, true)),
                otherwise = listOf(EventCommand.SetSwitch(2, true)),
            ),
            EventCommand.ChangeGold(-100),
            EventCommand.ConditionalBranch(
                condition = Condition(Condition.Kind.GOLD_AT_LEAST, value = 40),
                then = listOf(EventCommand.SetSwitch(3, true)),
                otherwise = listOf(EventCommand.SetSwitch(4, true)),
            ),
        )
        val engine = engineFacing(event)
        engine.pressAction()
        engine.run(0.3f)

        assertEquals(0, engine.state.gold, "el oro no baja de 0")
        assertEquals(2, engine.state.level, "ChangeExp(8) sube al nivel 2")
        assertTrue(engine.state.switchOn(1), "GOLD_AT_LEAST con oro suficiente")
        assertFalse(engine.state.switchOn(2))
        assertFalse(engine.state.switchOn(3))
        assertTrue(engine.state.switchOn(4), "GOLD_AT_LEAST con oro insuficiente")
    }
}
