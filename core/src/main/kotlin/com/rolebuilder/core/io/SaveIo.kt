package com.rolebuilder.core.io

import com.rolebuilder.core.engine.GameState
import com.rolebuilder.core.io.ProjectIo.writeTextAtomic
import java.io.File

/** Guardado y carga de partidas (un archivo JSON por ranura). */
object SaveIo {

    fun save(file: File, state: GameState) {
        state.savedAtEpochMs = System.currentTimeMillis()
        file.parentFile?.mkdirs()
        file.writeTextAtomic(ProjectIo.json.encodeToString(GameState.serializer(), state))
    }

    fun load(file: File): GameState =
        ProjectIo.json.decodeFromString(GameState.serializer(), file.readText())

    fun exists(file: File): Boolean = file.exists()
}
