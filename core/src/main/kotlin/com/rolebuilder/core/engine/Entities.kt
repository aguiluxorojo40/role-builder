package com.rolebuilder.core.engine

import com.rolebuilder.core.model.Enemy
import com.rolebuilder.core.model.EnemySpawn
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.MapEvent
import com.rolebuilder.core.model.event.MoveStep

/** Semi-lado de la caja de colisión de personajes, en casillas. */
const val CHAR_HALF_BOX = 0.35f

/** Velocidad de los movimientos por casillas de eventos, en casillas/segundo. */
const val GRID_MOVE_SPEED = 3f

/**
 * Estado en tiempo de ejecución del jugador. Posición = centro, en
 * coordenadas de casilla (5.5 = centro de la casilla 5).
 */
class PlayerState {
    var x = 0f
    var y = 0f
    var dir = Direction.DOWN
    var moving = false
    var animTime = 0f

    var attackCooldown = 0f
    var secondaryCooldown = 0f

    /** Tiempo restante del destello de ataque (para el renderer). */
    var attackFlash = 0f
    var invulnTime = 0f
    var knockbackTime = 0f
    var knockbackVx = 0f
    var knockbackVy = 0f

    /** Ruta forzada por un comando MoveRoute; bloquea el input. */
    val moveQueue = ArrayDeque<MoveStep>()
    var moveTargetX = 0f
    var moveTargetY = 0f
    var hasMoveTarget = false
    var stepWait = 0f

    val routeActive: Boolean
        get() = moveQueue.isNotEmpty() || hasMoveTarget || stepWait > 0f

    val tileX: Int get() = x.toInt()
    val tileY: Int get() = y.toInt()
}

/** Enemigo vivo sobre el mapa. */
class EnemyEntity(val spawn: EnemySpawn, val def: Enemy) {
    var x = spawn.x + 0.5f
    var y = spawn.y + 0.5f
    var dir = Direction.DOWN
    var hp = def.maxHp
    var alive = true

    /** Al morir se mantiene visible un instante para el destello de muerte. */
    var deathTimer = 0.3f
    var moving = false
    var animTime = 0f

    var invulnTime = 0f
    var knockbackTime = 0f
    var knockbackVx = 0f
    var knockbackVy = 0f

    /** WANDER: dirección actual y tiempo hasta el próximo cambio. */
    var wanderTimer = 0f
    var wanderDx = 0f
    var wanderDy = 0f
}

/** Evento del mapa en ejecución (se mueve por casillas). */
class EventEntity(val event: MapEvent) {
    var tileX = event.x
    var tileY = event.y
    var x = event.x + 0.5f
    var y = event.y + 0.5f
    var dir = Direction.DOWN
    var erased = false
    var moving = false
    var animTime = 0f

    /** Página activa según las condiciones; null = inactivo/invisible. */
    var page: EventPage? = null
    var pageIndex = -1

    /** Pasos pendientes de un MoveRoute o del movimiento aleatorio. */
    val moveQueue = ArrayDeque<MoveStep>()
    var moveTargetX = 0
    var moveTargetY = 0
    var hasMoveTarget = false
    var stepWait = 0f
    var randomMoveTimer = 0f

    /** Evita re-disparar PLAYER_TOUCH mientras el jugador siga encima. */
    var touchLatch = false

    val routeActive: Boolean
        get() = moveQueue.isNotEmpty() || hasMoveTarget || stepWait > 0f
}

/** Proyectil en vuelo (de una habilidad PROJECTILE). */
class Projectile(
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    val speed: Float,
    val damage: Int,
    val maxRange: Float,
    val knockback: Float,
    val fromPlayer: Boolean,
) {
    var traveled = 0f
    var alive = true
}

/** Objeto soltado por un enemigo, pendiente de recoger. */
class DropEntity(var x: Float, var y: Float, val itemId: Int) {
    var alive = true
}

/** Efecto visual efímero (número de daño, curación...). */
class HitEffect(val x: Float, val y: Float, val amount: Int, val kind: Kind) {
    enum class Kind { DAMAGE_ENEMY, DAMAGE_PLAYER, HEAL, ITEM }

    var timer = 0.8f
}
