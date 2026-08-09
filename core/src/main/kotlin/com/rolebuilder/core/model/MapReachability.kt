package com.rolebuilder.core.model

import com.rolebuilder.core.model.event.EventCommand

/**
 * Utilidad para detectar mapas "huérfanos" (inalcanzables desde el mapa inicial).
 * Recorre los eventos de cada mapa buscando comandos TransferPlayer y las
 * conexiones de zona por los bordes para construir un grafo de accesibilidad.
 */
object MapReachability {

    /**
     * Devuelve los IDs de mapas que no son alcanzables desde [startMapId] siguiendo
     * los comandos TransferPlayer en los eventos de cada mapa y las conexiones
     * de borde (edgeNorth/South/West/East).
     *
     * @param startMapId ID del mapa inicial del proyecto.
     * @param maps todos los mapas del proyecto indexados por ID.
     * @return conjunto de IDs de mapas que no son alcanzables (puede estar vacío).
     */
    fun findOrphanMaps(startMapId: Int, maps: Map<Int, GameMap>): Set<Int> {
        if (maps.size <= 1) return emptySet()

        val reachable = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()

        if (startMapId in maps) {
            queue.add(startMapId)
            reachable.add(startMapId)
        }

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val map = maps[currentId] ?: continue

            val targets = extractTransferTargets(map) +
                listOfNotNull(map.edgeNorth, map.edgeSouth, map.edgeWest, map.edgeEast)
            for (targetId in targets) {
                if (targetId !in reachable && targetId in maps) {
                    reachable.add(targetId)
                    queue.add(targetId)
                }
            }
        }

        return maps.keys - reachable
    }

    /**
     * Extrae todos los mapId destino de comandos TransferPlayer en los eventos del mapa,
     * incluyendo comandos anidados dentro de ShowChoices y ConditionalBranch.
     */
    private fun extractTransferTargets(map: GameMap): Set<Int> {
        val targets = mutableSetOf<Int>()
        for (event in map.events) {
            for (page in event.pages) {
                collectTransfers(page.commands, targets)
            }
        }
        return targets
    }

    private fun collectTransfers(commands: List<EventCommand>, targets: MutableSet<Int>) {
        for (cmd in commands) {
            when (cmd) {
                is EventCommand.TransferPlayer -> targets.add(cmd.mapId)
                is EventCommand.ShowChoices -> {
                    for (choice in cmd.choices) {
                        collectTransfers(choice.commands, targets)
                    }
                }
                is EventCommand.ConditionalBranch -> {
                    collectTransfers(cmd.then, targets)
                    collectTransfers(cmd.otherwise, targets)
                }
                else -> { /* no nested commands */ }
            }
        }
    }
}
