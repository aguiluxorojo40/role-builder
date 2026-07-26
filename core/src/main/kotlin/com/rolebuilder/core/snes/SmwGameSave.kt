package com.rolebuilder.core.snes

import com.rolebuilder.core.io.ProjectIo
import kotlinx.serialization.Serializable
import java.io.File

/**
 * PARTIDA de Super Mario World: el estado que sobrevive a cerrar la app.
 *
 * ## Por qué existe (y por qué está separada del editor)
 * La app tiene dos oficios que conviene no mezclar:
 *
 *  - **EDITAR y PROBAR** — el Platform Builder. Se abre un nivel, se toca, se lanza para ver
 *    cómo queda. Ahí una partida guardada estorba: cada prueba debe empezar limpia y poder
 *    verlo todo abierto.
 *  - **JUGAR** — el juego de verdad: título → mapa del mundo → nivel → vuelta al mapa con el
 *    camino nuevo abierto, y que al día siguiente siga donde lo dejaste.
 *
 * Este fichero es el estado del **segundo**. El modo prueba del overworld nunca lo toca.
 *
 * ## Qué se guarda
 * La progresión real de SMW no es un contador de niveles: es un **banderín por evento de
 * mapa**. Cada nivel superado dispara su evento ([SmwOverworld.translevelEvents]) y ese evento
 * es el que redibuja el mapa abriendo un camino. Por eso lo que se persiste es
 * [firedEvents] — el conjunto de eventos disparados — y no un número.
 *
 * [completedTranslevels] va aparte a propósito: hay niveles con evento repetido o sin evento
 * ([SmwOverworld.EVENT_NONE]), así que "qué caminos he abierto" y "qué niveles he pasado" son
 * dos preguntas distintas y ambas hacen falta (la segunda, para el marcador de completado).
 */
@Serializable
data class SmwGameSave(
    /** Ranura a la que pertenece (1..[MAX_SLOTS]). */
    val slot: Int = 1,
    /** Eventos de mapa ya disparados: son los que abren los caminos. */
    val firedEvents: Set<Int> = emptySet(),
    /** Translevels superados (para el recuento de progreso). */
    val completedTranslevels: Set<Int> = emptySet(),
    /** Dónde está Mario: [MAIN_MAP] o el índice de submapa 0..[SmwOverworld.SUBMAP_COUNT]-1. */
    val submap: Int = START_SUBMAP,
    /** Casilla del overworld (índice 0..0x7FF en la capa 1), o null si aún no se ha movido. */
    val position: Int? = null,
    val lives: Int = START_LIVES,
    val coins: Int = 0,
    /** Marca de tiempo del último guardado, para ordenar las ranuras. */
    val savedAtEpochMs: Long = 0L,
) {

    /** ¿Partida recién empezada (nada superado)? */
    val isNew: Boolean get() = firedEvents.isEmpty() && completedTranslevels.isEmpty()

    /**
     * Predicado que espera [SmwOverworld.overworldTilemapWithEvents]: qué eventos están
     * activos. Es la traducción directa de la partida al dibujo del mapa.
     */
    fun activeEvents(): (Int) -> Boolean = { e -> e in firedEvents }

    /**
     * Devuelve la partida tras SUPERAR el translevel [translevel], disparando su evento de
     * mapa [event] si lo tiene. Si el evento es [SmwOverworld.EVENT_NONE] solo se apunta el
     * nivel: hay casillas que no abren camino ninguno.
     */
    fun withLevelBeaten(translevel: Int, event: Int): SmwGameSave = copy(
        completedTranslevels = completedTranslevels + translevel,
        firedEvents = if (event == SmwOverworld.EVENT_NONE) firedEvents else firedEvents + event,
    )

    /** Mueve a Mario a la casilla [position] del mapa [submap]. */
    fun movedTo(submap: Int, position: Int): SmwGameSave =
        copy(submap = submap, position = position)

    companion object {
        /** Valor de [submap] que significa "en el mapa principal" (pantallas 0-3). */
        const val MAIN_MAP = -1

        /**
         * Dónde empieza una partida nueva. SMW arranca en **Yoshi's Island**, que es el
         * submapa 1 (ver el KDoc de [SmwOverworldLevels]). La casilla exacta de salida la
         * fija una tabla del juego que todavía no está portada, así que [position] empieza en
         * null y quien dibuje el mapa resuelve la casilla del primer nivel jugable. Cuando se
         * porte la tabla, esto pasa a ser el valor real y nada más cambia.
         */
        const val START_SUBMAP = 1

        /** Vidas de una partida nueva, como el juego. */
        const val START_LIVES = 5

        /** Cuántas ranuras de guardado ofrece la app (SMW tiene tres). */
        const val MAX_SLOTS = 3
    }
}

/**
 * Lectura y escritura de las [SmwGameSave] en disco, **una ranura por fichero**.
 *
 * Vive aparte del guardado del ARPG ([com.rolebuilder.core.io.SaveIo]) porque son dos juegos
 * distintos con dos estados distintos; compartir el fichero solo daría sorpresas.
 */
object SmwSaveIo {

    /** Nombre de la carpeta dentro del almacenamiento privado de la app. */
    const val DIR = "smw_saves"

    /** Fichero de la ranura [slot] dentro de [dir]. */
    fun file(dir: File, slot: Int): File = File(dir, "slot_$slot.json")

    /** Carga la ranura [slot], o null si no existe o está corrupta. */
    fun load(dir: File, slot: Int): SmwGameSave? {
        val f = file(dir, slot)
        if (!f.isFile) return null
        return runCatching {
            ProjectIo.json.decodeFromString(SmwGameSave.serializer(), f.readText())
        }.getOrNull()
    }

    /** Guarda [save] en su ranura, sellando la marca de tiempo. Devuelve lo escrito. */
    fun save(dir: File, save: SmwGameSave): SmwGameSave {
        val stamped = save.copy(savedAtEpochMs = System.currentTimeMillis())
        dir.mkdirs()
        file(dir, stamped.slot).writeText(
            ProjectIo.json.encodeToString(SmwGameSave.serializer(), stamped),
        )
        return stamped
    }

    /** Borra la ranura [slot]. Cierto si había algo que borrar. */
    fun delete(dir: File, slot: Int): Boolean = file(dir, slot).delete()

    /**
     * Estado de las [SmwGameSave.MAX_SLOTS] ranuras: índice 0 = ranura 1. Un null es una
     * ranura vacía, así el selector del título puede dibujarlas todas sin más lógica.
     */
    fun slots(dir: File): List<SmwGameSave?> =
        (1..SmwGameSave.MAX_SLOTS).map { load(dir, it) }
}
