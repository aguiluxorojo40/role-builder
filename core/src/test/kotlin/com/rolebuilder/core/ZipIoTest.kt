package com.rolebuilder.core

import com.rolebuilder.core.io.ProjectIo
import com.rolebuilder.core.io.ZipDemasiadoGrandeException
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

    // =========================================================================
    // Topes de tamaño (zip-bomba)
    // =========================================================================

    /** Zip con [entradas] ficheros de [bytesPorEntrada] bytes de ceros (comprimen muchísimo). */
    private fun zipDeCeros(entradas: Int, bytesPorEntrada: Int): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                repeat(entradas) { i ->
                    zip.putNextEntry(ZipEntry("relleno_$i.bin"))
                    zip.write(ByteArray(bytesPorEntrada))
                    zip.closeEntry()
                }
            }
        }.toByteArray()

    @Test
    fun `el tope cuenta lo descomprimido, no lo que ocupa el zip`() {
        // Esto ES una zip-bomba en pequeño: un zip diminuto que se expande mucho.
        // Si el tope mirase el tamaño del archivo —o el `size` de la cabecera, que
        // lo escribe quien manda el zip— este pasaría el filtro y llenaría el disco.
        val dest = File(tempDir(), "bomba")
        val bomba = zipDeCeros(entradas = 1, bytesPorEntrada = 4 * 1024 * 1024)
        val tope = 256L * 1024

        try {
            assertTrue(
                bomba.size < tope,
                "el zip comprimido (${bomba.size} B) debe caber bajo el tope para que la prueba valga",
            )
            assertFailsWith<ZipDemasiadoGrandeException> {
                ZipIo.importProject(ByteArrayInputStream(bomba), dest, maxBytes = tope)
            }
            assertFalse(dest.exists(), "un import abortado no puede dejar la carpeta a medias")
        } finally {
            dest.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `rechaza un zip con demasiadas entradas aunque no pesen`() {
        // Ataque distinto al de los bytes: un millón de ficheros vacíos no dispara el
        // contador de bytes, pero sí gasta inodos y tiempo. Por eso el tope va aparte.
        val dest = File(tempDir(), "muchas")
        val muchas = zipDeCeros(entradas = 50, bytesPorEntrada = 0)

        try {
            assertFailsWith<ZipDemasiadoGrandeException> {
                ZipIo.importProject(ByteArrayInputStream(muchas), dest, maxEntradas = 10)
            }
            assertFalse(dest.exists())
        } finally {
            dest.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `un proyecto legitimo sigue importandose con los topes por defecto`() {
        // La no-regresión que de verdad importa: de nada sirve blindar el import si de
        // paso rechaza el trabajo del usuario. Los topes reales, sin tocar.
        val source = tempDir()
        val dest = File(tempDir(), "legitimo")
        try {
            writeTemplate(source)
            val bytes = ByteArrayOutputStream().also { ZipIo.exportProject(source, it) }.toByteArray()

            assertEquals("Compartido", ZipIo.importProject(ByteArrayInputStream(bytes), dest))
            assertEquals(2, ProjectIo.loadFull(dest).maps.size)
        } finally {
            source.deleteRecursively()
            dest.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `se niega a importar sobre una carpeta que ya tiene cosas`() {
        // Importar encima mezclaría dos proyectos, y si algo fallara la limpieza se
        // llevaría por delante lo que ya había. Negarse ANTES de tocar nada.
        val source = tempDir()
        val ocupado = tempDir()
        try {
            writeTemplate(source)
            File(ocupado, "mio.txt").writeText("trabajo del usuario")
            val bytes = ByteArrayOutputStream().also { ZipIo.exportProject(source, it) }.toByteArray()

            assertFailsWith<IllegalArgumentException> {
                ZipIo.importProject(ByteArrayInputStream(bytes), ocupado)
            }
            assertTrue(File(ocupado, "mio.txt").exists(), "no puede tocar lo que ya había")
        } finally {
            source.deleteRecursively()
            ocupado.deleteRecursively()
        }
    }

    @Test
    fun `los temporales del guardado atomico no viajan en el zip`() {
        // Un .json.tmp es una escritura a medias (la app murió mientras guardaba).
        // Meterlo en el zip le regala al que importe un fichero truncado.
        val source = tempDir()
        val dest = File(tempDir(), "sin-temporales")
        try {
            writeTemplate(source)
            File(source, ProjectIo.PROJECT_FILE + ProjectIo.TMP_SUFFIX).writeText("{ truncado")

            val bytes = ByteArrayOutputStream().also { ZipIo.exportProject(source, it) }.toByteArray()
            ZipIo.importProject(ByteArrayInputStream(bytes), dest)

            assertFalse(
                File(dest, ProjectIo.PROJECT_FILE + ProjectIo.TMP_SUFFIX).exists(),
                "los temporales no se exportan",
            )
        } finally {
            source.deleteRecursively()
            dest.parentFile?.deleteRecursively()
        }
    }
}
