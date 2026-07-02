package com.rolebuilder.core.engine

import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.Project
import com.rolebuilder.core.model.event.Direction
import kotlinx.serialization.Serializable

/**
 * Estado persistente de una partida: es lo que se guarda y carga.
 * El resto del estado del motor (entidades, proyectiles...) se
 * reconstruye a partir del mapa actual.
 */
@Serializable
data class GameState(
    val switches: MutableMap<Int, Boolean> = mutableMapOf(),
    val variables: MutableMap<Int, Int> = mutableMapOf(),
    /** Self-switches por evento, clave "mapId:eventId:letra". */
    val selfSwitches: MutableMap<String, Boolean> = mutableMapOf(),
    /** Inventario: itemId -> cantidad. */
    val items: MutableMap<Int, Int> = mutableMapOf(),
    var hp: Int = 1,
    var maxHp: Int = 1,
    var mapId: Int = 1,
    var x: Float = 0f,
    var y: Float = 0f,
    var dir: Direction = Direction.DOWN,
    var playTimeSeconds: Float = 0f,
) {
    fun switchOn(id: Int): Boolean = switches[id] == true

    fun variable(id: Int): Int = variables[id] ?: 0

    fun itemCount(id: Int): Int = items[id] ?: 0

    fun addItem(id: Int, delta: Int) {
        val count = (itemCount(id) + delta).coerceAtLeast(0)
        if (count == 0) items.remove(id) else items[id] = count
    }

    fun selfSwitch(mapId: Int, eventId: Int, key: String): Boolean =
        selfSwitches[selfSwitchKey(mapId, eventId, key)] == true

    companion object {
        fun selfSwitchKey(mapId: Int, eventId: Int, key: String): String = "$mapId:$eventId:$key"

        fun newGame(project: Project, database: Database): GameState {
            val actor = database.actor(project.playerActorId)
            val hp = actor?.maxHp ?: 1
            return GameState(
                hp = hp,
                maxHp = hp,
                mapId = project.startMapId,
                x = project.startX + 0.5f,
                y = project.startY + 0.5f,
            )
        }
    }
}
