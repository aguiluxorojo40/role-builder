package com.rolebuilder.core

import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.ZipIo
import com.rolebuilder.core.model.DefaultProjectFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZipIoTest {

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "rb-zip-${System.nanoTime()}").apply { mkdirs() }

    private fun writeTemplate(dir: File) {
        ProjectIo.saveProject(dir, DefaultProjectFactory.defaultProject("Compartido"))
        ProjectIo.saveDatabase(dir, DefaultProjectFactory.defaultDatabase())
        DefaultProjectFactory.maps().forEach { ProjectIo.saveMap(dir, it) }
        ProjectIo.imageFile(dir, "tileset.png").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun `export and import round trip`() {
        val source = tempDir()
        val dest = File(tempDir(), "importado")
        try {
            writeTemplate(source)
            // Una partida guardada NO debe viajar en el zip.
            File(source, "saves/slot1.json").apply {
                parentFile?.mkdirs()
                writeText("{}")
            }

            val bytes = ByteArrayOutputStream().also { ZipIo.exportProject(source, it) }.toByteArray()
            val name = ZipIo.importProject(ByteArrayInputStream(bytes), dest)

            assertEquals("Compartido", name)
            val loaded = ProjectIo.loadFull(dest)
            assertEquals(2, loaded.maps.size)
            assertTrue(ProjectIo.imageFile(dest, "tileset.png").exists())
            assertFalse(File(dest, "saves/slot1.json").exists(), "las partidas no se exportan")
        } finally {
            source.deleteRecursively()
            dest.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `import rejects zip slip entries and cleans up`() {
        val dest = File(tempDir(), "victima")
        val evil = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("../fuera.txt"))
                zip.write("mal".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        try {
            assertFailsWith<IllegalArgumentException> {
                ZipIo.importProject(ByteArrayInputStream(evil), dest)
            }
            assertFalse(dest.exists(), "se limpia el destino tras el fallo")
            assertFalse(File(dest.parentFile, "fuera.txt").exists())
        } finally {
            dest.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `import rejects zip without a valid project`() {
        val dest = File(tempDir(), "invalido")
        val notAProject = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("cualquiera.txt"))
                zip.write("hola".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        try {
            assertFailsWith<Exception> {
                ZipIo.importProject(ByteArrayInputStream(notAProject), dest)
            }
            assertFalse(dest.exists())
        } finally {
            dest.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `import rejects project with corrupt map layers and cleans up`() {
        // Un zip con project.json válido pero un mapa estructuralmente roto
        // no debe quedarse instalado a medias.
        val source = tempDir()
        val dest = File(tempDir(), "corrupto")
        try {
            writeTemplate(source)
            File(source, "maps/map_1.json").writeText(
                """{"id":1,"name":"rota","width":5,"height":5,"layers":[[0],[  -1]]}""",
            )
            val bytes = ByteArrayOutputStream().also { ZipIo.exportProject(source, it) }.toByteArray()
            assertFailsWith<Exception> {
                ZipIo.importProject(ByteArrayInputStream(bytes), dest)
            }
            assertFalse(dest.exists(), "el destino se limpia si el proyecto no carga")
        } finally {
            source.deleteRecursively()
            dest.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `export requires a project folder`() {
        val empty = tempDir()
        try {
            assertFailsWith<IllegalArgumentException> {
                ZipIo.exportProject(empty, ByteArrayOutputStream())
            }
        } finally {
            empty.deleteRecursively()
        }
    }
}
