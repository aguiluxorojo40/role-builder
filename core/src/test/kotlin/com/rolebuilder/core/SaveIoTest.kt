package com.rolebuilder.core

import com.rolebuilder.core.engine.GameState
import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.SaveIo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveIoTest {

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "rb-save-${System.nanoTime()}")

    private inline fun withDir(block: (File) -> Unit) {
        val dir = tempDir()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `save and load round trip per slot`() {
        withDir { dir ->
            val state = GameState(hp = 7, maxHp = 20, gold = 33, level = 4, mapId = 2, playTimeSeconds = 12.5f)
            SaveIo.save(dir, 2, state)
            assertEquals(state, SaveIo.load(dir, 2))
            assertNull(SaveIo.load(dir, 1), "las otras ranuras siguen vacías")
            assertNull(SaveIo.load(dir, 3))
        }
    }

    @Test
    fun `load returns null for missing slot`() {
        withDir { dir ->
            assertNull(SaveIo.load(dir, 1))
        }
    }

    @Test
    fun `listSlots returns metadata of existing slots only`() {
        withDir { dir ->
            SaveIo.save(dir, 1, GameState(level = 2, gold = 10, mapId = 1, playTimeSeconds = 5f))
            SaveIo.save(dir, 3, GameState(level = 9, gold = 250, mapId = 4, playTimeSeconds = 99f))

            val slots = SaveIo.listSlots(dir)
            assertEquals(listOf(1, 3), slots.map { it.slot })

            val first = slots.first()
            assertEquals(2, first.level)
            assertEquals(10, first.gold)
            assertEquals(1, first.mapId)
            assertEquals(5f, first.playTimeSeconds)
            assertTrue(first.modifiedAtMillis > 0, "usa el mtime del archivo")

            val last = slots.last()
            assertEquals(9, last.level)
            assertEquals(250, last.gold)
            assertEquals(4, last.mapId)
        }
    }

    @Test
    fun `listSlots of empty dir is empty`() {
        withDir { dir ->
            assertTrue(SaveIo.listSlots(dir).isEmpty())
        }
    }

    @Test
    fun `legacy single-slot save migrates to slot 1 on list`() {
        withDir { dir ->
            dir.mkdirs()
            val legacy = GameState(level = 6, gold = 77, mapId = 3, playTimeSeconds = 42f)
            File(dir, "slot1.json").writeText(ProjectIo.json.encodeToString(GameState.serializer(), legacy))

            val slots = SaveIo.listSlots(dir)
            assertEquals(1, slots.size)
            assertEquals(1, slots.first().slot)
            assertEquals(6, slots.first().level)
            assertFalse(File(dir, "slot1.json").exists(), "el archivo antiguo se renombra")
            assertTrue(File(dir, "save_1.json").exists())
        }
    }

    @Test
    fun `legacy single-slot save migrates to slot 1 on load`() {
        withDir { dir ->
            dir.mkdirs()
            val legacy = GameState(level = 6, gold = 77, mapId = 3)
            File(dir, "slot1.json").writeText(ProjectIo.json.encodeToString(GameState.serializer(), legacy))

            val loaded = SaveIo.load(dir, 1)
            assertNotNull(loaded)
            assertEquals(legacy, loaded)
            assertFalse(File(dir, "slot1.json").exists())
        }
    }

    @Test
    fun `legacy file yields to an existing slot 1`() {
        withDir { dir ->
            SaveIo.save(dir, 1, GameState(level = 8))
            File(dir, "slot1.json").writeText("{}")

            val loaded = SaveIo.load(dir, 1)
            assertNotNull(loaded)
            assertEquals(8, loaded.level, "el archivo nuevo manda sobre el antiguo")
            assertFalse(File(dir, "slot1.json").exists())
        }
    }
}
