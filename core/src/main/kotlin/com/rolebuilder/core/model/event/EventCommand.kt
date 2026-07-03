package com.rolebuilder.core.model.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Comandos de evento tipo RPG Maker. Son datos serializables: el editor los
 * construye visualmente y el intérprete del motor los ejecuta.
 */
@Serializable
sealed class EventCommand {

    /** Muestra una caja de mensaje y espera a que el jugador la cierre. */
    @Serializable
    @SerialName("showText")
    data class ShowText(val text: String, val speaker: String = "") : EventCommand()

    /** Muestra opciones; ejecuta la rama de la opción elegida. */
    @Serializable
    @SerialName("showChoices")
    data class ShowChoices(val choices: List<Choice>) : EventCommand() {
        @Serializable
        data class Choice(val text: String, val commands: List<EventCommand> = emptyList())
    }

    @Serializable
    @SerialName("setSwitch")
    data class SetSwitch(val switchId: Int, val value: Boolean) : EventCommand()

    /** Self-switch del evento que ejecuta el comando (clave "A".."D"). */
    @Serializable
    @SerialName("setSelfSwitch")
    data class SetSelfSwitch(val key: String = "A", val value: Boolean = true) : EventCommand()

    @Serializable
    @SerialName("setVariable")
    data class SetVariable(val variableId: Int, val op: Op = Op.SET, val value: Int = 0) : EventCommand() {
        enum class Op { SET, ADD, SUB }
    }

    @Serializable
    @SerialName("conditional")
    data class ConditionalBranch(
        val condition: Condition,
        val then: List<EventCommand> = emptyList(),
        val otherwise: List<EventCommand> = emptyList(),
    ) : EventCommand()

    /** Teletransporta al jugador a otro mapa/posición. */
    @Serializable
    @SerialName("transfer")
    data class TransferPlayer(
        val mapId: Int,
        val x: Int,
        val y: Int,
        val direction: Direction? = null,
    ) : EventCommand()

    @Serializable
    @SerialName("wait")
    data class Wait(val seconds: Float) : EventCommand()

    /**
     * Mueve a un personaje paso a paso.
     * [target]: [TARGET_SELF] = el propio evento, [TARGET_PLAYER] = jugador,
     * cualquier otro valor = id de evento del mapa actual.
     */
    @Serializable
    @SerialName("moveRoute")
    data class MoveRoute(
        val target: Int = TARGET_SELF,
        val steps: List<MoveStep>,
        val waitForCompletion: Boolean = true,
    ) : EventCommand() {
        companion object {
            const val TARGET_SELF = 0
            const val TARGET_PLAYER = -1
        }
    }

    @Serializable
    @SerialName("changeItems")
    data class ChangeItems(val itemId: Int, val delta: Int) : EventCommand()

    /** Cambia el HP del jugador (positivo cura, negativo daña). */
    @Serializable
    @SerialName("changeHp")
    data class ChangeHp(val delta: Int) : EventCommand()

    @Serializable
    @SerialName("playSound")
    data class PlaySound(val name: String) : EventCommand()

    /** Borra el evento que lo ejecuta hasta que se recargue el mapa. */
    @Serializable
    @SerialName("eraseEvent")
    data object EraseEvent : EventCommand()

    /** Reproduce una pista de música de fondo en bucle (ver MusicTracks). */
    @Serializable
    @SerialName("playMusic")
    data class PlayMusic(val track: String) : EventCommand()

    /** Detiene la música de fondo actual (silencio). */
    @Serializable
    @SerialName("stopMusic")
    data object StopMusic : EventCommand()

    /**
     * Cambia el sprite del evento que lo ejecuta hasta que cambie su página
     * activa. [image] null = el evento se vuelve invisible.
     */
    @Serializable
    @SerialName("changeEventSprite")
    data class ChangeEventSprite(val image: String? = null, val direction: Direction = Direction.DOWN) : EventCommand()

    /** Asigna a la variable un valor aleatorio entre [min] y [max], ambos incluidos. */
    @Serializable
    @SerialName("setVariableRandom")
    data class SetVariableRandom(val variableId: Int, val min: Int = 0, val max: Int = 100) : EventCommand()
}

/** Condición evaluable por ConditionalBranch y por las páginas del editor. */
@Serializable
data class Condition(
    val kind: Kind,
    /** Id de switch/variable/item según [kind]. */
    val id: Int = 0,
    /** Clave del self-switch cuando kind == SELF_SWITCH. */
    val key: String = "A",
    /** Umbral cuando kind == VARIABLE_AT_LEAST. */
    val value: Int = 0,
    /** Valor esperado para SWITCH / SELF_SWITCH / HAS_ITEM. */
    val expected: Boolean = true,
) {
    enum class Kind { SWITCH, SELF_SWITCH, VARIABLE_AT_LEAST, HAS_ITEM }
}

enum class MoveStep {
    UP, DOWN, LEFT, RIGHT,
    TURN_UP, TURN_DOWN, TURN_LEFT, TURN_RIGHT,
    WAIT_HALF_SECOND,
}
