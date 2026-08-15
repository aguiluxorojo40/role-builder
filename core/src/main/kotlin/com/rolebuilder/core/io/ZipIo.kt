package com.rolebuilder.core.io

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * El zip que se está importando pide más espacio —o más ficheros— de los que puede
 * necesitar un proyecto de esta app. Se lanza **mientras** se descomprime, no al final,
 * para que un "zip-bomba" (unos pocos KB que se expanden a gigas) no llegue a llenar el
 * almacenamiento del dispositivo. Ver los topes en [ZipIo].
 */
class ZipDemasiadoGrandeException(message: String) : Exception(message)

/**
 * Exporta e importa proyectos como archivos .zip para compartirlos.
 * El zip contiene la carpeta del proyecto tal cual (JSON + imágenes),
 * sin las partidas guardadas.
 */
object ZipIo {

    private const val SAVES_DIR = "saves"
    private const val BYTES_POR_MB = 1024 * 1024
    private const val BUFFER_BYTES = 64 * 1024

    /**
     * Tope de bytes **descomprimidos** que se aceptan al importar un proyecto.
     *
     * Calibrado con lo que pesa de verdad un proyecto de esta app, no a ojo:
     * - la plantilla (`app/src/main/assets/default_project`): 43 KB en 13 ficheros;
     * - el caso pesado real es importar en lote los sub-niveles de SMW: cada nivel deja
     *   un mapa JSON de hasta 512×27 casillas por dos capas —unos 0,45 MB con el
     *   `prettyPrint` de [ProjectIo.json]— más su atlas PNG, y son del orden de un
     *   centenar. Es decir, decenas de MB para el proyecto más gordo que la app sabe
     *   construir.
     *
     * 512 MiB deja un margen de ~5× sobre ese caso pesado (para que nadie pierda un
     * proyecto legítimo por esto) y a la vez acota lo que un zip hostil puede escribir
     * antes de que se aborte y se borre.
     */
    const val MAX_BYTES_DESCOMPRIMIDOS: Long = 512L * BYTES_POR_MB

    /**
     * Tope de **entradas** del zip. La plantilla trae 13 ficheros y una importación
     * completa de SMW ronda las 250 (un mapa y un atlas por sub-nivel), así que 4.000 es
     * más de un orden de magnitud de margen. Va aparte del tope de bytes porque el ataque
     * es distinto: un millón de entradas vacías no gasta espacio pero sí inodos y tiempo,
     * y ninguna de ellas dispararía el contador de bytes.
     */
    const val MAX_ENTRADAS = 4_000

    // NO hay tope de RATIO de compresión, y es una decisión medida, no un olvido: los
    // datos de esta app son JSON con `prettyPrint`, un entero por línea, y comprimen de
    // escándalo. Un mapa legítimo de 200×200 vacío da ratio ~494:1, y deflate no puede
    // pasar de ~1032:1 en un solo paso (y aquí no se descomprime en cascada: solo se
    // abre el zip de fuera). La ventana entre "proyecto legítimo" y "máximo teórico" es
    // de menos de 2×, demasiado estrecha para distinguir sin rechazar trabajo real del
    // usuario. Quien cuenta de verdad es el tope de bytes, que sí es infalsificable.

    /** Comprime el proyecto en [output]. No incluye la carpeta de partidas. */
    fun exportProject(projectDir: File, output: OutputStream) {
        require(File(projectDir, ProjectIo.PROJECT_FILE).exists()) {
            "La carpeta no contiene un proyecto (${ProjectIo.PROJECT_FILE})"
        }
        ZipOutputStream(output.buffered()).use { zip ->
            projectDir.walkTopDown()
                .filter { it.isFile && exportable(it.relativeTo(projectDir).invariantSeparatorsPath) }
                .forEach { file ->
                    zip.putNextEntry(ZipEntry(file.relativeTo(projectDir).invariantSeparatorsPath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    /**
     * Qué viaja en el zip. Se quedan fuera:
     * - `saves/`: las partidas son de este dispositivo y de este jugador, no del proyecto;
     * - los temporales del guardado atómico ([ProjectIo.TMP_SUFFIX]): si la app murió a
     *   mitad de una escritura queda un `.json.tmp` truncado, y meterlo en el zip es
     *   regalarle al que lo importe un fichero a medias que nadie sabrá de dónde salió.
     */
    private fun exportable(relative: String): Boolean =
        !relative.startsWith("$SAVES_DIR/") && !relative.endsWith(ProjectIo.TMP_SUFFIX)

    /**
     * Descomprime un proyecto en [destDir] (que no debe existir o estar vacío) y devuelve
     * el nombre del proyecto importado.
     *
     * El zip lo ha hecho **otro**: se valida todo lo que trae. Rechaza rutas maliciosas
     * (zip-slip), acota el tamaño descomprimido ([maxBytes]) y el número de entradas
     * ([maxEntradas]), y exige que lo descomprimido se pueda cargar como proyecto
     * completo. Ante cualquier fallo borra [destDir]: un import a medias es peor que no
     * importar, porque deja una carpeta que la app enseña como proyecto y no lo es.
     *
     * [maxBytes] y [maxEntradas] son parámetros —no constantes escondidas— para que los
     * tests puedan ejercitar el tope de verdad sin fabricar medio giga de basura.
     */
    fun importProject(
        input: InputStream,
        destDir: File,
        maxBytes: Long = MAX_BYTES_DESCOMPRIMIDOS,
        maxEntradas: Int = MAX_ENTRADAS,
    ): String {
        // Importar sobre una carpeta con contenido sería destructivo por partida doble:
        // mezclaría dos proyectos y, si algo fallara, la limpieza de más abajo se llevaría
        // por delante lo que ya había. Mejor negarse antes de tocar nada.
        require(destDir.esDestinoLimpio()) {
            "La carpeta de destino ya tiene contenido: ${destDir.name}"
        }
        destDir.mkdirs()
        try {
            extraer(input, destDir, maxBytes, maxEntradas)
            // Validación: debe poder cargarse como proyecto completo.
            return ProjectIo.loadFull(destDir).project.name
        } catch (e: Exception) {
            destDir.deleteRecursively()
            throw e
        }
    }

    /** Destino válido para importar: o no existe, o es una carpeta vacía. */
    private fun File.esDestinoLimpio(): Boolean =
        !exists() || isDirectory && listFiles().isNullOrEmpty()

    /** Vuelca todas las entradas del zip en [destDir] respetando los topes. */
    private fun extraer(input: InputStream, destDir: File, maxBytes: Long, maxEntradas: Int) {
        val canonicalDest = destDir.canonicalFile
        var totalBytes = 0L
        var entradas = 0
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entradas++
                comprobarEntradas(entradas, maxEntradas)
                if (!entry.isDirectory) {
                    val target = destinoDe(destDir, canonicalDest, entry)
                    target.parentFile?.mkdirs()
                    totalBytes = copiarConTope(zip, target, totalBytes, maxBytes)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun comprobarEntradas(entradas: Int, maxEntradas: Int) {
        if (entradas > maxEntradas) {
            throw ZipDemasiadoGrandeException(
                "El archivo trae más de $maxEntradas ficheros: no parece un proyecto de esta app.",
            )
        }
    }

    /**
     * Ruta de destino de [entry] dentro de [destDir], comprobada contra **zip-slip**: una
     * entrada llamada `../../otra_cosa` escribiría fuera de la carpeta del proyecto. La
     * comparación es por ruta CANÓNICA (que resuelve `..` y los enlaces), nunca por texto.
     */
    private fun destinoDe(destDir: File, canonicalDest: File, entry: ZipEntry): File {
        val target = File(destDir, entry.name)
        require(target.canonicalFile.toPath().startsWith(canonicalDest.toPath())) {
            "Entrada de zip no permitida: ${entry.name}"
        }
        return target
    }

    /**
     * Copia la entrada actual de [zip] en [target] contando los bytes **según salen** del
     * descompresor, y aborta en cuanto se pasa de [maxBytes]. Devuelve el total acumulado.
     *
     * Deliberadamente NO se mira `ZipEntry.size`: es un campo de cabecera que escribe quien
     * fabrica el zip, o sea el atacante, y puede declarar 1 KB para traer 4 GB. Además, en
     * los zips escritos en streaming —los que produce [exportProject] sin ir más lejos— el
     * tamaño ni siquiera está: vale -1 hasta que la entrada se ha leído entera. Un tope
     * basado en la cabecera sería a la vez engañable e inútil.
     */
    private fun copiarConTope(zip: ZipInputStream, target: File, yaEscritos: Long, maxBytes: Long): Long {
        var total = yaEscritos
        val buffer = ByteArray(BUFFER_BYTES)
        target.outputStream().buffered().use { out ->
            while (true) {
                val leidos = zip.read(buffer)
                if (leidos <= 0) break
                total += leidos
                if (total > maxBytes) throw ZipDemasiadoGrandeException(mensajeTope(maxBytes))
                out.write(buffer, 0, leidos)
            }
        }
        return total
    }

    private fun mensajeTope(maxBytes: Long): String =
        "El archivo se descomprime a más de ${maxBytes / BYTES_POR_MB} MB: se ha cancelado " +
            "la importación para no llenar el almacenamiento del dispositivo."
}
