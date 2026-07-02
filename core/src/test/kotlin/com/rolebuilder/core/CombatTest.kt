package com.rolebuilder.core

import com.rolebuilder.core.EngineTestSupport.engine
import com.rolebuilder.core.EngineTestSupport.run
import com.rolebuilder.core.model.EnemySpawn
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Tiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatTest {

    private fun mapWithSlime(x: Int, y: Int): GameMap =
        GameMap.empty(1, "m", 14, 14, fillTile = Tiles.GRASS)
            .copy(spawns = listOf(EnemySpawn(enemyId = 1, x = x, y = y)))

    @Test
    fun `melee attack damages adjacent enemy`() {
        val engine = engine(listOf(mapWithSlime(6, 5)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f) // mirar a la derecha
        engine.inputX = 0f

        val slime = engine.enemies.first()
        val hp0 = slime.hp
        engine.pressAction()
        engine.tick(1 / 60f)
        assertTrue(slime.hp < hp0, "el slime recibió daño (${slime.hp} < $hp0)")
        assertTrue(engine.effects.isNotEmpty(), "hay número de daño flotante")
    }

    @Test
    fun `enemy dies after enough hits and disappears`() {
        val engine = engine(listOf(mapWithSlime(6, 5)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.inputX = 0f

        repeat(10) {
            engine.pressAction()
            engine.run(0.5f) // deja pasar cooldown e invulnerabilidad
        }
        assertTrue(engine.enemies.isEmpty(), "el slime murió y fue retirado")
    }

    @Test
    fun `chasing enemy hurts player on contact`() {
        val engine = engine(listOf(mapWithSlime(8, 5)), startX = 5, startY = 5)
        val hp0 = engine.state.hp
        engine.run(4f) // el slime persigue y alcanza al jugador quieto
        assertTrue(engine.state.hp < hp0, "el jugador recibió daño (${engine.state.hp} < $hp0)")
    }

    @Test
    fun `player invulnerability limits damage rate`() {
        val engine = engine(listOf(mapWithSlime(6, 5)), startX = 5, startY = 5)
        val hp0 = engine.state.hp
        engine.run(1.0f)
        val damage = hp0 - engine.state.hp
        // Con 1 s de invulnerabilidad, en 1 s solo caben 1-2 golpes.
        assertTrue(damage in 1..2 * engine.enemies.first().def.attack, "daño acotado: $damage")
    }

    @Test
    fun `projectile skill hits distant enemy`() {
        val engine = engine(listOf(mapWithSlime(9, 5)), startX = 5, startY = 5)
        engine.inputX = 1f
        engine.tick(1 / 60f)
        engine.inputX = 0f

        val slime = engine.enemies.first()
        val hp0 = slime.hp
        engine.pressSecondary()
        engine.run(1f)
        assertTrue(slime.hp < hp0, "el proyectil alcanzó al slime")
        assertTrue(engine.projectiles.isEmpty(), "el proyectil se consumió")
    }

    @Test
    fun `game over when hp reaches zero`() {
        val engine = engine(listOf(mapWithSlime(6, 5)), startX = 5, startY = 5)
        engine.state.hp = 1
        engine.run(3f)
        assertEquals(0, engine.state.hp)
        assertTrue(engine.gameOver)
    }

    @Test
    fun `use potion heals and consumes item`() {
        val engine = engine(listOf(GameMap.empty(1, "m", 10, 10, fillTile = Tiles.GRASS)))
        engine.state.addItem(1, 2)
        engine.state.hp = 5
        assertTrue(engine.useItem(1))
        assertEquals(15, engine.state.hp)
        assertEquals(1, engine.state.itemCount(1))
        // Con la vida llena no se puede usar.
        engine.state.hp = engine.state.maxHp
        assertTrue(!engine.useItem(1))
        assertEquals(1, engine.state.itemCount(1))
    }
}
