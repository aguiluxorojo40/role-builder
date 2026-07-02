package com.rolebuilder.core.io

import com.rolebuilder.core.engine.GameState
import java.io.File

/**
 * Guardado y carga de partidas multi-ranura. Cada ranura (1..[SLOT_COUNT])
 * es un archivo `save_<slot>.json` dentro del directorio de guardados.
 * El formato antiguo de una sola ranura (`slot1.json`) se migra a la
 * ranura 1 la primera vez que se lista o carga.
 */
object SaveIo {

    /** Número de ranuras de guardado disponibles. */
    const val SLOT_COUNT = 3

    /** Nombre del archivo del formato antiguo de una sola ranura. */
    private const val LEGACY_FILE = "slot1.json"

    /** Archivo de la ranura [slot] dentro de [dir]. */
    fun slotFile(dir: File, slot: Int): File = File(dir, "save_$slot.json")

    /** Guarda [state] en la ranura [slot] (1..[SLOT_COUNT]). */
    fun save(dir: File, slot: Int, state: GameState) {
        require(slot in 1..SLOT_COUNT) { "Ranura fuera de rango: $slot" }
        dir.mkdirs()
        slotFile(dir, slot).writeText(ProjectIo.json.encodeToString(GameState.serializer(), state))
    }

    /** Carga la ranura [slot], o null si no existe o no puede leerse. */
    fun load(dir: File, slot: Int): GameState? {
        migrateLegacySave(dir)
        val file = slotFile(dir, slot)
        if (!file.exists()) return null
        return runCatching {
            ProjectIo.json.decodeFromString(GameState.serializer(), file.readText())
        }.getOrNull()
    }

    /** Resumen de una ranura ocupada, para el menú de cargar/guardar. */
    data class SlotInfo(
        val slot: Int,
        val level: Int,
        val gold: Int,
        val mapId: Int,
        val playTimeSeconds: Float,
        /** Fecha de modificación del archivo, en milisegundos de época. */
        val modifiedAtMillis: Long,
    )

    /** Lista solo las ranuras existentes (y legibles), ordenadas por número. */
    fun listSlots(dir: File): List<SlotInfo> {
        migrateLegacySave(dir)
        return (1..SLOT_COUNT).mapNotNull { slot ->
            val file = slotFile(dir, slot)
            if (!file.exists()) return@mapNotNull null
            val state = runCatching {
                ProjectIo.json.decodeFromString(GameState.serializer(), file.readText())
            }.getOrNull() ?: return@mapNotNull null
            SlotInfo(
                slot = slot,
                level = state.level,
                gold = state.gold,
                mapId = state.mapId,
                playTimeSeconds = state.playTimeSeconds,
                modifiedAtMillis = file.lastModified(),
            )
        }
    }

    /** Migra el guardado del formato antiguo (una sola ranura) a la ranura 1. */
    private fun migrateLegacySave(dir: File) {
        val legacy = File(dir, LEGACY_FILE)
        if (!legacy.exists()) return
        val target = slotFile(dir, 1)
        if (target.exists()) {
            legacy.delete() // La ranura 1 ya existe: el archivo nuevo manda.
        } else {
            legacy.renameTo(target)
        }
    }
}
