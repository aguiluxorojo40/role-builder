package com.rolebuilder.core.model

import com.rolebuilder.core.model.event.MapEvent
import kotlinx.serialization.Serializable

/** Tile vacío en una capa. */
const val EMPTY_TILE = -1

/**
 * Un mapa de tiles. Cada capa es una lista aplanada por filas de tamaño width*height
 * con índices dentro del tileset ([EMPTY_TILE] = sin tile).
 * La capa 0 es el suelo, la capa 1 decoración/superior.
 */
@Serializable
data class GameMap(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val tilesetId: Int = 1,
    val layers: List<List<Int>>,
    val events: List<MapEvent> = emptyList(),
    val spawns: List<EnemySpawn> = emptyList(),
    /**
     * Enemigos de plataformas colocados en el mapa (id de sprite SMW + celda de 16px).
     * Los consume el motor de Platform Builder; el RPG los ignora.
     */
    val platformEnemies: List<PlatformEnemyMark> = emptyList(),
    /**
     * Warps de plataformas: tuberías/puertas que llevan a otro mapa del proyecto. Los
     * consume el motor de Platform Builder para enlazar los sub-niveles de un nivel SMW.
     */
    val platformWarps: List<MapWarp> = emptyList(),
    /**
     * Ítems de plataformas colocados en el mapa (moneda/meta + celda de 16px). Los
     * instancia el motor de Platform Builder; el RPG los ignora.
     */
    val platformItems: List<PlatformItemMark> = emptyList(),
    /** Pista de música de fondo del mapa (ver MusicTracks); null = silencio. */
    val bgm: String? = null,
    /** Clima ambiental al entrar al mapa. */
    val weather: Weather = Weather.NONE,
    /** Capas de parallax (cielo lejano, niebla...) que dibuja el runtime. */
    val parallaxLayers: List<ParallaxLayer> = emptyList(),
    /**
     * Zonas conectadas por los bordes: id del mapa vecino en cada dirección
     * (null = borde cerrado). Al cruzar el borde caminando, el jugador pasa
     * al mapa vecino por el lado opuesto, conservando su fila/columna.
     */
    val edgeNorth: Int? = null,
    val edgeSouth: Int? = null,
    val edgeWest: Int? = null,
    val edgeEast: Int? = null,
) {
    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun tileAt(layer: Int, x: Int, y: Int): Int =
        if (!inBounds(x, y)) EMPTY_TILE else layers[layer][y * width + x]

    /** Copia del mapa con un tile cambiado (para el editor). */
    fun withTile(layer: Int, x: Int, y: Int, tile: Int): GameMap {
        if (!inBounds(x, y)) return this
        val newLayer = layers[layer].toMutableList().also { it[y * width + x] = tile }
        return copy(layers = layers.mapIndexed { i, l -> if (i == layer) newLayer else l })
    }

    fun eventAt(x: Int, y: Int): MapEvent? = events.firstOrNull { it.x == x && it.y == y }

    fun nextEventId(): Int = (events.maxOfOrNull { it.id } ?: 0) + 1

    /** Redimensiona conservando el contenido existente (para el editor). */
    fun resized(newWidth: Int, newHeight: Int, fillTile: Int = EMPTY_TILE): GameMap {
        val newLayers = layers.mapIndexed { index, layer ->
            List(newWidth * newHeight) { i ->
                val x = i % newWidth
                val y = i / newWidth
                if (inBounds(x, y)) layer[y * width + x]
                else if (index == 0) fillTile else EMPTY_TILE
            }
        }
        return copy(
            width = newWidth,
            height = newHeight,
            layers = newLayers,
            events = events.filter { it.x < newWidth && it.y < newHeight },
            spawns = spawns.filter { it.x < newWidth && it.y < newHeight },
            platformEnemies = platformEnemies.filter { it.x < newWidth && it.y < newHeight },
            platformItems = platformItems.filter { it.x < newWidth && it.y < newHeight },
        )
    }

    companion object {
        const val LAYER_COUNT = 2

        fun empty(id: Int, name: String, width: Int, height: Int, tilesetId: Int = 1, fillTile: Int = 0): GameMap =
            GameMap(
                id = id,
                name = name,
                width = width,
                height = height,
                tilesetId = tilesetId,
                layers = listOf(
                    List(width * height) { fillTile },
                    List(width * height) { EMPTY_TILE },
                ),
            )
    }
}

/**
 * Capa de parallax: una imagen del proyecto repetida en mosaico que se
 * desplaza a distinta velocidad que el mapa, para dar profundidad de
 * diorama al estilo HD-2D.
 */
@Serializable
data class ParallaxLayer(
    /** Imagen PNG del proyecto (se repite en mosaico; 16 px = 1 casilla). */
    val image: String,
    /**
     * Cuánto acompaña a la cámara: 0 = pegada a la pantalla (cielo muy
     * lejano), 1 = pegada al mapa (se mueve como los tiles).
     */
    val factor: Float = 0.5f,
    /** Desplazamiento automático en casillas/segundo. */
    val autoX: Float = 0f,
    val autoY: Float = 0f,
    /** true = se dibuja por encima de la escena (niebla); false = fondo. */
    val above: Boolean = false,
    /** Opacidad 0..1. */
    val alpha: Float = 1f,
)

/** Punto de aparición de un enemigo colocado en el editor. */
@Serializable
data class EnemySpawn(
    val enemyId: Int,
    val x: Int,
    val y: Int,
)

/**
 * Warp de plataformas en la celda (x,y): al entrar (según [input]: 0=abajo/tubería,
 * 1=arriba/puerta, 2=lado/tubería horizontal) lleva al mapa [destMapId] del proyecto,
 * a la celda ([destX],[destY]). Enlaza los sub-niveles de un nivel SMW importado.
 */
@Serializable
data class MapWarp(
    val x: Int,
    val y: Int,
    val input: Int,
    val destMapId: Int,
    val destX: Int,
    val destY: Int,
)

/**
 * Enemigo de plataformas: id de sprite de SMW (0x00..0xFF, como lo entrega
 * [com.rolebuilder.core.snes.SmwSprites]) colocado en la celda (x,y) de 16px.
 * El motor lo instancia como entidad con gravedad que patrulla y se puede pisar.
 */
@Serializable
data class PlatformEnemyMark(
    val spriteId: Int,
    val x: Int,
    val y: Int,
)

/** Tipo de ítem de plataformas colocable en el editor. */
@Serializable
enum class PlatformItemType {
    /** Moneda: se recoge al tocarla y suma al contador. */
    COIN,

    /** Meta / bandera: al tocarla se completa el nivel. */
    GOAL,
}

/**
 * Un ítem de plataformas (moneda o meta) en la celda (x,y) de 16px. El motor de
 * Platform Builder lo instancia; las monedas se recogen y la meta gana el nivel.
 */
@Serializable
data class PlatformItemMark(
    val type: PlatformItemType,
    val x: Int,
    val y: Int,
)
