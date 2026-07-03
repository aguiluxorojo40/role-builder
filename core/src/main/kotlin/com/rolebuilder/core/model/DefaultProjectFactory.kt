package com.rolebuilder.core.model

import com.rolebuilder.core.model.event.Condition
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MapEvent
import com.rolebuilder.core.model.event.MoveType
import com.rolebuilder.core.model.event.PageConditions
import com.rolebuilder.core.model.event.SpriteRef

/** Índices dentro del tileset por defecto (8x8 tiles de 16 px). */
object Tiles {
    const val GRASS = 0
    const val FLOWERS = 1
    const val DIRT = 2
    const val SAND = 3
    const val WATER = 4
    const val TREE = 5
    const val BUSH = 6
    const val ROCK = 7
    const val WALL = 8
    const val WOOD_FLOOR = 9
    const val STONE_FLOOR = 10
    const val BRIDGE = 11
    const val DOOR_CLOSED = 12
    const val DOOR_OPEN = 13
    const val STAIRS = 14
    const val ROOF = 15

    val IMPASSABLE = setOf(WATER, TREE, BUSH, ROCK, WALL, DOOR_CLOSED, ROOF)
}

/**
 * Construye el contenido del proyecto plantilla: base de datos con actor,
 * enemigos, objetos y habilidades básicas, y un mapa inicial jugable con
 * un NPC, un cofre y enemigos de ejemplo.
 */
object DefaultProjectFactory {

    const val TILESET_IMAGE = "tileset.png"

    fun defaultTileset(): Tileset {
        val count = 8 * 8
        return Tileset(
            id = 1,
            name = "Campo",
            image = TILESET_IMAGE,
            tileSize = 16,
            columns = 8,
            rows = 8,
            passable = List(count) { it !in Tiles.IMPASSABLE },
        )
    }

    fun defaultDatabase(): Database = Database(
        actors = listOf(
            Actor(id = 1, name = "Héroe", sprite = "hero.png", maxHp = 20, attack = 5, defense = 1, moveSpeed = 4.5f, attackSkillId = 1, secondarySkillId = 2),
        ),
        enemies = listOf(
            Enemy(id = 1, name = "Slime", sprite = "slime.png", maxHp = 6, attack = 2, defense = 0, moveSpeed = 1.8f, behavior = EnemyBehavior.CHASE, sightRange = 5, dropItemId = 1, dropChance = 0.3f),
            Enemy(id = 2, name = "Murciélago", sprite = "bat.png", maxHp = 4, attack = 3, defense = 0, moveSpeed = 3f, behavior = EnemyBehavior.WANDER, sightRange = 6),
        ),
        items = listOf(
            Item(id = 1, name = "Poción", description = "Recupera 10 PV.", effect = ItemEffect.HEAL_HP, power = 10),
            Item(id = 2, name = "Llave", description = "Abre una puerta cerrada.", effect = ItemEffect.KEY, consumable = false),
        ),
        skills = listOf(
            Skill(id = 1, name = "Espada", kind = SkillKind.MELEE, power = 0, range = 1f, cooldownSeconds = 0.35f, knockback = 0.6f),
            Skill(id = 2, name = "Disparo", kind = SkillKind.PROJECTILE, power = 2, range = 6f, cooldownSeconds = 0.8f, projectileSpeed = 9f),
        ),
        tilesets = listOf(defaultTileset()),
    )

    /** Todos los mapas del proyecto plantilla. */
    fun maps(): List<GameMap> = listOf(starterMap(), cabinMap())

    /** Mapa inicial 20x15: pradera con estanque, camino, cabaña, NPC, cofre y enemigos. */
    fun starterMap(): GameMap {
        var map = GameMap.empty(id = 1, name = "Pradera", width = 20, height = 15, tilesetId = 1, fillTile = Tiles.GRASS)

        // Borde de árboles.
        for (x in 0 until map.width) {
            map = map.withTile(0, x, 0, Tiles.TREE).withTile(0, x, map.height - 1, Tiles.TREE)
        }
        for (y in 0 until map.height) {
            map = map.withTile(0, 0, y, Tiles.TREE).withTile(0, map.width - 1, y, Tiles.TREE)
        }
        // Estanque.
        for (y in 3..5) for (x in 13..17) map = map.withTile(0, x, y, Tiles.WATER)
        // Camino de tierra horizontal.
        for (x in 1..18) map = map.withTile(0, x, 8, Tiles.DIRT)
        // Cabaña con puerta (evento de transferencia al mapa 2).
        for (x in 14..17) {
            map = map.withTile(0, x, 10, Tiles.ROOF).withTile(0, x, 11, Tiles.WALL)
        }
        map = map.withTile(0, 15, 11, Tiles.DOOR_CLOSED)
        // Decoración.
        listOf(3 to 3, 5 to 11, 9 to 4, 2 to 9).forEach { (x, y) ->
            map = map.withTile(1, x, y, Tiles.FLOWERS)
        }
        map = map.withTile(0, 4, 5, Tiles.BUSH).withTile(0, 11, 11, Tiles.ROCK)

        val npc = MapEvent(
            id = 1,
            name = "Aldeano",
            x = 8,
            y = 6,
            pages = listOf(
                EventPage(
                    sprite = SpriteRef("npc.png", Direction.DOWN),
                    trigger = EventTrigger.ACTION_BUTTON,
                    moveType = MoveType.RANDOM,
                    commands = listOf(
                        EventCommand.ShowText("¡Bienvenido a Role Builder!", speaker = "Aldeano"),
                        EventCommand.ShowText("Toca una casilla en el editor para crear eventos como yo.", speaker = "Aldeano"),
                    ),
                ),
            ),
        )

        val chest = MapEvent(
            id = 2,
            name = "Cofre",
            x = 3,
            y = 12,
            pages = listOf(
                EventPage(
                    sprite = SpriteRef("chest.png", Direction.DOWN),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.PlaySound("chest"),
                        EventCommand.ChangeItems(itemId = 1, delta = 1),
                        EventCommand.ShowText("¡Has encontrado una Poción!"),
                        EventCommand.SetSelfSwitch("A", true),
                    ),
                ),
                EventPage(
                    conditions = PageConditions(selfSwitch = "A"),
                    sprite = SpriteRef("chest.png", Direction.UP),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.ConditionalBranch(
                            condition = Condition(Condition.Kind.HAS_ITEM, id = 1),
                            then = listOf(EventCommand.ShowText("El cofre está vacío.")),
                            otherwise = listOf(EventCommand.ShowText("El cofre está vacío...")),
                        ),
                    ),
                ),
            ),
        )

        val door = MapEvent(
            id = 3,
            name = "Puerta de la cabaña",
            x = 15,
            y = 11,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.PLAYER_TOUCH,
                    commands = listOf(
                        EventCommand.PlaySound("select"),
                        EventCommand.TransferPlayer(mapId = 2, x = 4, y = 6, direction = Direction.UP),
                    ),
                    // Farolillo del puesto: luz cálida de muestra del estilo HD-2D.
                    lightRadius = 2.5f,
                ),
            ),
        )

        return map.copy(
            events = listOf(npc, chest, door),
            spawns = listOf(
                EnemySpawn(enemyId = 1, x = 10, y = 5),
                EnemySpawn(enemyId = 1, x = 6, y = 3),
                EnemySpawn(enemyId = 2, x = 12, y = 12),
            ),
            bgm = MusicTracks.FIELD,
        )
    }

    /** Interior de la cabaña (mapa 2), con salida, cofre y cartel. */
    fun cabinMap(): GameMap {
        var map = GameMap.empty(id = 2, name = "Cabaña", width = 10, height = 8, tilesetId = 1, fillTile = Tiles.WOOD_FLOOR)

        for (x in 0 until map.width) {
            map = map.withTile(0, x, 0, Tiles.WALL).withTile(0, x, map.height - 1, Tiles.WALL)
        }
        for (y in 0 until map.height) {
            map = map.withTile(0, 0, y, Tiles.WALL).withTile(0, map.width - 1, y, Tiles.WALL)
        }
        map = map.withTile(0, 4, map.height - 1, Tiles.DOOR_OPEN)

        val exit = MapEvent(
            id = 1,
            name = "Salida",
            x = 4,
            y = 7,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.PLAYER_TOUCH,
                    passable = true,
                    commands = listOf(
                        EventCommand.TransferPlayer(mapId = 1, x = 15, y = 12, direction = Direction.DOWN),
                    ),
                ),
            ),
        )

        val cabinChest = MapEvent(
            id = 2,
            name = "Cofre de la cabaña",
            x = 8,
            y = 1,
            pages = listOf(
                EventPage(
                    sprite = SpriteRef("chest.png", Direction.DOWN),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.PlaySound("chest"),
                        EventCommand.ChangeItems(itemId = 1, delta = 1),
                        EventCommand.ShowText("¡Has encontrado una Poción!"),
                        EventCommand.SetSelfSwitch("A", true),
                    ),
                ),
                EventPage(
                    conditions = PageConditions(selfSwitch = "A"),
                    sprite = SpriteRef("chest.png", Direction.UP),
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(EventCommand.ShowText("El cofre está vacío.")),
                ),
            ),
        )

        val sign = MapEvent(
            id = 3,
            name = "Cartel",
            x = 1,
            y = 1,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.ShowText("Dulce hogar. Pisa la puerta para salir.", speaker = "Cartel"),
                    ),
                ),
            ),
        )

        return map.copy(events = listOf(exit, cabinChest, sign), bgm = MusicTracks.VILLAGE)
    }

    fun defaultProject(name: String): Project = Project(
        name = name,
        startMapId = 1,
        startX = 5,
        startY = 8,
        playerActorId = 1,
        mapIds = listOf(1, 2),
    )
}
