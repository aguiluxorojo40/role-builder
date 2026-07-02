package com.rolebuilder.core.io

import com.rolebuilder.core.engine.GameState
import java.io.File

/** Guardado y carga de partidas (un archivo JSON por ranura). */
object SaveIo {

    fun save(file: File, state: GameState) {
        file.parentFile?.mkdirs()
        file.writeText(ProjectIo.json.encodeToString(GameState.serializer(), state))
    }

    fun load(file: File): GameState =
        ProjectIo.json.decodeFromString(GameState.serializer(), file.readText())

    fun exists(file: File): Boolean = file.exists()
}
