package com.rolebuilder.core.model.event

import kotlinx.serialization.Serializable

/**
 * Un evento colocado en el mapa (NPC, cofre, puerta, trigger invisible...).
 * Como en RPG Maker, tiene páginas: la página activa es la ÚLTIMA cuyas
 * condiciones se cumplen; si ninguna se cumple, el evento está inactivo.
 */
@Serializable
data class MapEvent(
    val id: Int,
    val name: String = "",
    val x: Int,
    val y: Int,
    val pages: List<EventPage> = listOf(EventPage()),
)

@Serializable
data class EventPage(
    val conditions: PageConditions = PageConditions(),
    /** null = evento invisible. */
    val sprite: SpriteRef? = null,
    val trigger: EventTrigger = EventTrigger.ACTION_BUTTON,
    val passable: Boolean = false,
    val moveType: MoveType = MoveType.FIXED,
    val commands: List<EventCommand> = emptyList(),
)

/** Todas las condiciones no nulas deben cumplirse para activar la página. */
@Serializable
data class PageConditions(
    val switchId: Int? = null,
    /** Clave de self-switch ("A".."D") que debe estar activada. */
    val selfSwitch: String? = null,
    val variableId: Int? = null,
    val variableAtLeast: Int = 0,
    /** El grupo debe poseer este objeto. */
    val itemId: Int? = null,
)

@Serializable
data class SpriteRef(
    val image: String,
    val direction: Direction = Direction.DOWN,
)

enum class EventTrigger {
    /** Al pulsar el botón de acción mirando al evento. */
    ACTION_BUTTON,

    /** Al pisar/chocar con el evento. */
    PLAYER_TOUCH,

    /** Se ejecuta solo, bloqueando el juego, mientras la página esté activa. */
    AUTORUN,

    /** Se ejecuta en segundo plano en bucle mientras la página esté activa. */
    PARALLEL,
}

enum class MoveType { FIXED, RANDOM }

enum class Direction {
    DOWN, LEFT, RIGHT, UP;

    val dx: Int get() = when (this) { LEFT -> -1; RIGHT -> 1; else -> 0 }
    val dy: Int get() = when (this) { UP -> -1; DOWN -> 1; else -> 0 }

    val opposite: Direction get() = when (this) {
        DOWN -> UP; UP -> DOWN; LEFT -> RIGHT; RIGHT -> LEFT
    }
}
