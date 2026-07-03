package com.rolebuilder.core

import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.MapReachability
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MapEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class MapReachabilityTest {

    private fun emptyMap(id: Int, name: String = "Map $id") =
        GameMap.empty(id, name, 10, 10)

    private fun mapWithTransfer(id: Int, targetMapId: Int, name: String = "Map $id"): GameMap {
        val event = MapEvent(
            id = 1, name = "door", x = 5, y = 5,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(EventCommand.TransferPlayer(targetMapId, 1, 1)),
                ),
            ),
        )
        return emptyMap(id, name).copy(events = listOf(event))
    }

    @Test
    fun `single map has no orphans`() {
        val maps = mapOf(1 to emptyMap(1))
        assertEquals(emptySet<Int>(), MapReachability.findOrphanMaps(1, maps))
    }

    @Test
    fun `two maps linked are both reachable`() {
        val map1 = mapWithTransfer(1, targetMapId = 2)
        val map2 = emptyMap(2)
        val maps = mapOf(1 to map1, 2 to map2)
        assertEquals(emptySet<Int>(), MapReachability.findOrphanMaps(1, maps))
    }

    @Test
    fun `unlinked map is orphan`() {
        val map1 = emptyMap(1)
        val map2 = emptyMap(2)
        val maps = mapOf(1 to map1, 2 to map2)
        assertEquals(setOf(2), MapReachability.findOrphanMaps(1, maps))
    }

    @Test
    fun `chain of transfers makes all reachable`() {
        val map1 = mapWithTransfer(1, targetMapId = 2)
        val map2 = mapWithTransfer(2, targetMapId = 3)
        val map3 = emptyMap(3)
        val maps = mapOf(1 to map1, 2 to map2, 3 to map3)
        assertEquals(emptySet<Int>(), MapReachability.findOrphanMaps(1, maps))
    }

    @Test
    fun `transfer inside conditional branch is detected`() {
        val event = MapEvent(
            id = 1, name = "npc", x = 3, y = 3,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.ConditionalBranch(
                            condition = com.rolebuilder.core.model.event.Condition(
                                kind = com.rolebuilder.core.model.event.Condition.Kind.SWITCH,
                                id = 1,
                            ),
                            then = listOf(EventCommand.TransferPlayer(2, 0, 0)),
                            otherwise = emptyList(),
                        ),
                    ),
                ),
            ),
        )
        val map1 = emptyMap(1).copy(events = listOf(event))
        val map2 = emptyMap(2)
        val maps = mapOf(1 to map1, 2 to map2)
        assertEquals(emptySet<Int>(), MapReachability.findOrphanMaps(1, maps))
    }

    @Test
    fun `transfer inside choice branch is detected`() {
        val event = MapEvent(
            id = 1, name = "npc", x = 3, y = 3,
            pages = listOf(
                EventPage(
                    trigger = EventTrigger.ACTION_BUTTON,
                    commands = listOf(
                        EventCommand.ShowChoices(
                            choices = listOf(
                                EventCommand.ShowChoices.Choice(
                                    text = "Ir al mapa 2",
                                    commands = listOf(EventCommand.TransferPlayer(2, 0, 0)),
                                ),
                                EventCommand.ShowChoices.Choice(text = "Cancelar"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val map1 = emptyMap(1).copy(events = listOf(event))
        val map2 = emptyMap(2)
        val maps = mapOf(1 to map1, 2 to map2)
        assertEquals(emptySet<Int>(), MapReachability.findOrphanMaps(1, maps))
    }

    @Test
    fun `multiple orphans detected`() {
        val map1 = mapWithTransfer(1, targetMapId = 2)
        val map2 = emptyMap(2)
        val map3 = emptyMap(3)
        val map4 = emptyMap(4)
        val maps = mapOf(1 to map1, 2 to map2, 3 to map3, 4 to map4)
        assertEquals(setOf(3, 4), MapReachability.findOrphanMaps(1, maps))
    }
}
