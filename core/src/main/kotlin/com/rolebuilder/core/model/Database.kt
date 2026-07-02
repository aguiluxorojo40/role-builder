package com.rolebuilder.core.model

import kotlinx.serialization.Serializable

/** Base de datos del proyecto: definiciones compartidas por todos los mapas. */
@Serializable
data class Database(
    val actors: List<Actor> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val items: List<Item> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val tilesets: List<Tileset> = emptyList(),
) {
    fun actor(id: Int): Actor? = actors.firstOrNull { it.id == id }
    fun enemy(id: Int): Enemy? = enemies.firstOrNull { it.id == id }
    fun item(id: Int): Item? = items.firstOrNull { it.id == id }
    fun skill(id: Int): Skill? = skills.firstOrNull { it.id == id }
    fun tileset(id: Int): Tileset? = tilesets.firstOrNull { it.id == id }
}

/** Personaje jugable. */
@Serializable
data class Actor(
    val id: Int,
    val name: String = "Héroe",
    /** Hoja de sprites en images/ (3 columnas x 4 filas: abajo, izquierda, derecha, arriba). */
    val sprite: String = "hero.png",
    val maxHp: Int = 20,
    val attack: Int = 5,
    val defense: Int = 1,
    /** Casillas por segundo. */
    val moveSpeed: Float = 4f,
    val attackSkillId: Int = 1,
)

@Serializable
data class Enemy(
    val id: Int,
    val name: String = "Enemigo",
    val sprite: String = "slime.png",
    val maxHp: Int = 6,
    val attack: Int = 3,
    val defense: Int = 0,
    val moveSpeed: Float = 2f,
    val behavior: EnemyBehavior = EnemyBehavior.CHASE,
    /** Distancia en casillas a la que detecta al jugador. */
    val sightRange: Int = 5,
    /** Daño por contacto con el jugador. */
    val touchDamage: Boolean = true,
    val dropItemId: Int? = null,
    val dropChance: Float = 0.3f,
)

enum class EnemyBehavior { STILL, WANDER, CHASE }

@Serializable
data class Item(
    val id: Int,
    val name: String = "Objeto",
    val description: String = "",
    val effect: ItemEffect = ItemEffect.NONE,
    /** Cantidad del efecto (p. ej. HP curados). */
    val power: Int = 0,
    /** Se consume al usarse desde el menú. */
    val consumable: Boolean = true,
)

enum class ItemEffect { NONE, HEAL_HP, KEY }

/** Habilidad de combate de acción (ataque cuerpo a cuerpo o proyectil). */
@Serializable
data class Skill(
    val id: Int,
    val name: String = "Ataque",
    val kind: SkillKind = SkillKind.MELEE,
    /** Se suma al ataque del usuario para calcular el daño. */
    val power: Int = 0,
    /** Alcance en casillas (melee: profundidad del golpe; proyectil: distancia máxima). */
    val range: Float = 1f,
    val cooldownSeconds: Float = 0.4f,
    /** Velocidad del proyectil en casillas/segundo. */
    val projectileSpeed: Float = 8f,
    /** Retroceso aplicado al objetivo, en casillas. */
    val knockback: Float = 0.5f,
)

enum class SkillKind { MELEE, PROJECTILE }
