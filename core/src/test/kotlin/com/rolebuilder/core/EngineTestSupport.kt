package com.rolebuilder.core

import com.rolebuilder.core.engine.RpgEngine
import com.rolebuilder.core.io.LoadedProject
import com.rolebuilder.core.model.Database
import com.rolebuilder.core.model.DefaultProjectFactory
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Project
import java.io.File
import kotlin.random.Random

/** Construye un motor de pruebas sobre mapas en memoria. */
object EngineTestSupport {

    fun project(maps: List<GameMap>, startX: Int = 5, startY: Int = 5, database: Database = DefaultProjectFactory.defaultDatabase()): LoadedProject {
        val project = Project(
            name = "test",
            startMapId = maps.first().id,
            startX = startX,
            startY = startY,
            mapIds = maps.map { it.id },
        )
        return LoadedProject(File("."), project, database, maps.associateBy { it.id })
    }

    fun engine(maps: List<GameMap>, startX: Int = 5, startY: Int = 5, seed: Int = 42): RpgEngine {
        val data = project(maps, startX, startY)
        return RpgEngine(data, random = Random(seed))
    }

    /** Avanza el motor en pasos fijos de 1/60 s. */
    fun RpgEngine.run(seconds: Float) {
        var t = 0f
        val dt = 1f / 60f
        while (t < seconds) {
            tick(dt)
            t += dt
        }
    }
}
