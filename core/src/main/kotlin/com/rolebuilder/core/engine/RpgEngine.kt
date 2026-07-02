package com.rolebuilder.core.engine

import com.rolebuilder.core.io.LoadedProject
import com.rolebuilder.core.model.Actor
import com.rolebuilder.core.model.EnemyBehavior
import com.rolebuilder.core.model.EquipSlot
import com.rolebuilder.core.model.GameMap
import com.rolebuilder.core.model.Item
import com.rolebuilder.core.model.ItemEffect
import com.rolebuilder.core.model.Skill
import com.rolebuilder.core.model.SkillKind
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MoveStep
import com.rolebuilder.core.model.event.MoveType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/** Caja de mensaje visible (la UI la muestra y llama a dismissMessage). */
data class MessageBox(val text: String, val speaker: String)

/** Tienda abierta (la UI la muestra y llama a buyItem/sellItem/closeShop). */
class ShopSession(val itemIds: List<Int>)

/**
 * Motor de juego completo e independiente del renderizado. La app Android
 * llama a [tick] cada frame, alimenta el input (joystick + botones) y dibuja
 * el estado público (mapa, entidades, mensajes, efectos).
 */
class RpgEngine(
    val data: LoadedProject,
    val state: GameState = GameState.newGame(data.project, data.database),
    private val random: Random = Random.Default,
) {
    lateinit var currentMap: GameMap
        private set
    lateinit var tileset: Tileset
        private set

    val actor: Actor =
        data.database.actor(data.project.playerActorId) ?: Actor(id = 0)

    val player = PlayerState()
    val events = mutableListOf<EventEntity>()
    val enemies = mutableListOf<EnemyEntity>()
    val projectiles = mutableListOf<Projectile>()
    val drops = mutableListOf<DropEntity>()
    val effects = mutableListOf<HitEffect>()

    /** Sonidos pendientes de reproducir; la app los consume cada frame.
     *  Cola concurrente: la UI puede encolar desde otro hilo. */
    val soundQueue = java.util.concurrent.ConcurrentLinkedQueue<String>()

    /** Se pone a true al cambiar de mapa para que el renderer se reconstruya. */
    var mapChanged = true

    /** Pista de música de fondo actual (null = silencio). La app la observa
     *  cada frame y reproduce/detiene el audio correspondiente. */
    var currentBgm: String? = null
        private set

    var gameOver = false
        private set

    /** Pausa total del mundo (menú abierto). */
    var paused = false

    // ---- UI de mensajes -----------------------------------------------------

    var message: MessageBox? = null
        private set
    var choices: List<String>? = null
        private set
    private var messageOwner: Interpreter? = null
    private var choiceOwner: Interpreter? = null

    /** Tienda abierta, o null. La UI la dibuja y la cierra con [closeShop]. */
    var shop: ShopSession? = null
        private set
    private var shopOwner: Interpreter? = null

    /** Aviso puntual para la UI (p. ej. "¡Nivel 5!"); la UI lo consume poniéndolo a null. */
    var notice: String? = null

    // ---- efectos de pantalla (el renderer los lee cada frame) ----------------

    /**
     * Tinte RGBA del frame actual, interpolado linealmente hacia el destino
     * persistente (state.tintR..tintA). Tamaño 4. Al cargar partida arranca
     * ya en el destino, sin transición.
     */
    val tintCurrent: FloatArray =
        floatArrayOf(state.tintR, state.tintG, state.tintB, state.tintA)
    private val tintStart = FloatArray(4)
    private var tintElapsed = 0f
    private var tintDuration = 0f

    /** Color RGB del destello actual (válido mientras [flashIntensity] > 0). */
    val flashColor: FloatArray = floatArrayOf(1f, 1f, 1f)

    /** Intensidad del destello: 1 al lanzarlo, decae linealmente a 0 en tick. */
    var flashIntensity: Float = 0f
    private var flashDuration = 0.3f

    /** Segundos restantes de temblor de cámara (el renderer decide la amplitud). */
    var shakeTimeLeft: Float = 0f

    // ---- input (lo escribe la capa de UI) -----------------------------------

    /** Ejes del joystick virtual, en [-1, 1]. */
    var inputX = 0f
    var inputY = 0f
    private var actionPressed = false
    private var secondaryPressed = false

    /** Botón A: interactuar con un evento o atacar. */
    fun pressAction() { actionPressed = true }

    /** Botón B: habilidad secundaria (proyectil). */
    fun pressSecondary() { secondaryPressed = true }

    // ---- intérpretes ---------------------------------------------------------

    private val mainInterpreter = Interpreter(this)
    private val parallelInterpreters = mutableMapOf<Int, Interpreter>()

    /** true mientras una escena bloquea al jugador (evento en curso, mensaje o tienda). */
    val uiBlocked: Boolean
        get() = mainInterpreter.running || message != null || choices != null || shop != null

    init {
        loadMap(state.mapId, state.x, state.y, state.dir)
        syncMaxHp()
    }

    // =========================================================================
    // Mapa y transferencias
    // =========================================================================

    private fun loadMap(mapId: Int, x: Float, y: Float, dir: Direction) {
        val map = data.maps[mapId]
            ?: data.maps.values.firstOrNull()
            ?: error("El proyecto no tiene mapas")
        currentMap = map
        tileset = data.database.tileset(map.tilesetId)
            ?: data.database.tilesets.firstOrNull()
            ?: Tileset(id = 0, name = "?", image = "")

        state.mapId = map.id
        // El mapa impone su presentación; los comandos pueden sobreescribirla después.
        state.weather = map.weather
        player.x = x
        player.y = y
        player.dir = dir
        player.moveQueue.clear()
        player.hasMoveTarget = false

        events.clear()
        events.addAll(map.events.map { EventEntity(it) })
        enemies.clear()
        enemies.addAll(
            map.spawns.mapNotNull { spawn ->
                data.database.enemy(spawn.enemyId)?.let { EnemyEntity(spawn, it) }
            },
        )
        projectiles.clear()
        drops.clear()
        effects.clear()
        parallelInterpreters.clear()
        refreshEventPages()
        currentBgm = map.bgm
        mapChanged = true
    }

    fun transferPlayer(mapId: Int, x: Int, y: Int, direction: Direction?) {
        val dir = direction ?: player.dir
        loadMap(mapId, x + 0.5f, y + 0.5f, dir)
    }

    /** Cambia la música de fondo (PlayMusic/StopMusic; null = silencio). */
    internal fun setBgm(track: String?) {
        currentBgm = track
    }

    /** Entero aleatorio en el rango [min], [max] con el Random inyectado (si min > max, devuelve min). */
    internal fun randomInt(min: Int, max: Int): Int =
        if (min >= max) min else random.nextInt(min, max + 1)

    // =========================================================================
    // Tick principal
    // =========================================================================

    fun tick(dt: Float) {
        if (gameOver || paused) return
        state.playTimeSeconds += dt

        // Los efectos de pantalla avanzan SIEMPRE, incluso con la UI bloqueada
        // (un evento puede hacer FlashScreen + ShowText y verse el destello).
        updateScreenEffects(dt)

        refreshEventPages()
        if (!uiBlocked) {
            handleActionButton()
            handleSecondaryButton()
        } else {
            actionPressed = false
            secondaryPressed = false
        }
        startAutorunIfIdle()
        mainInterpreter.update(dt)
        updateParallelInterpreters(dt)

        val blocked = uiBlocked
        updatePlayer(dt, blocked)
        updateEvents(dt)

        if (!blocked) {
            updateEnemies(dt)
            updateProjectiles(dt)
            updateDrops()
        }

        effects.removeAll { effect -> effect.timer -= dt; effect.timer <= 0f }

        syncStateFromPlayer()
        if (state.hp <= 0) gameOver = true
    }

    private fun syncStateFromPlayer() {
        state.x = player.x
        state.y = player.y
        state.dir = player.dir
    }

    // =========================================================================
    // Páginas y disparadores de eventos
    // =========================================================================

    private fun refreshEventPages() {
        for (entity in events) {
            if (entity.erased) {
                entity.page = null
                continue
            }
            val index = entity.event.pages.indexOfLast { pageConditionsMet(entity, it.conditions) }
            if (index != entity.pageIndex) {
                entity.pageIndex = index
                entity.page = entity.event.pages.getOrNull(index)
                entity.moveQueue.clear()
                entity.hasMoveTarget = false
                entity.spriteOverride = null
                entity.spriteOverridden = false
                entity.page?.sprite?.let { entity.dir = it.direction }
            }
        }
    }

    private fun pageConditionsMet(
        entity: EventEntity,
        c: com.rolebuilder.core.model.event.PageConditions,
    ): Boolean {
        c.switchId?.let { if (!state.switchOn(it)) return false }
        c.selfSwitch?.let { if (!state.selfSwitch(state.mapId, entity.event.id, it)) return false }
        c.variableId?.let { if (state.variable(it) < c.variableAtLeast) return false }
        c.itemId?.let { if (state.itemCount(it) <= 0) return false }
        return true
    }

    private fun startAutorunIfIdle() {
        if (mainInterpreter.running) return
        val autorun = events.firstOrNull { it.page?.trigger == EventTrigger.AUTORUN && !it.erased }
        if (autorun != null) {
            mainInterpreter.start(autorun.page!!.commands, autorun)
        }
    }

    private fun updateParallelInterpreters(dt: Float) {
        for (entity in events) {
            val page = entity.page ?: continue
            if (page.trigger != EventTrigger.PARALLEL || entity.erased) continue
            val interp = parallelInterpreters.getOrPut(entity.event.id) { Interpreter(this) }
            if (!interp.running) interp.start(page.commands, entity)
            interp.update(dt)
        }
        // Descartar intérpretes de páginas que dejaron de ser paralelas.
        parallelInterpreters.entries.removeAll { (id, interp) ->
            val entity = events.firstOrNull { it.event.id == id }
            val stillParallel = entity?.page?.trigger == EventTrigger.PARALLEL && entity.erased.not()
            if (!stillParallel) interp.stop()
            !stillParallel
        }
    }

    private fun handleActionButton() {
        if (!actionPressed) return
        actionPressed = false

        // 1) Evento en la casilla del jugador o en la casilla que mira.
        val target = eventAtTile(player.tileX, player.tileY)?.takeIf { it.canInteract() }
            ?: eventAtTile(player.tileX + player.dir.dx, player.tileY + player.dir.dy)
                ?.takeIf { it.canInteract() }
        if (target != null) {
            target.dir = player.dir.opposite
            mainInterpreter.start(target.page!!.commands, target)
            return
        }

        // 2) Sin evento delante: atacar.
        data.database.skill(actor.attackSkillId)?.let { performSkill(it) }
    }

    private fun EventEntity.canInteract(): Boolean =
        !erased && page?.trigger == EventTrigger.ACTION_BUTTON && page!!.commands.isNotEmpty()

    private fun handleSecondaryButton() {
        if (!secondaryPressed) return
        secondaryPressed = false
        val skillId = actor.secondarySkillId ?: return
        data.database.skill(skillId)?.let { performSkill(it) }
    }

    private fun triggerTouchIfAny(tileX: Int, tileY: Int) {
        if (mainInterpreter.running) return
        val entity = eventAtTile(tileX, tileY) ?: return
        val page = entity.page ?: return
        if (page.trigger != EventTrigger.PLAYER_TOUCH || entity.touchLatch || entity.erased) return
        entity.touchLatch = true
        mainInterpreter.start(page.commands, entity)
    }

    fun eventAtTile(x: Int, y: Int): EventEntity? =
        events.firstOrNull { !it.erased && it.page != null && it.tileX == x && it.tileY == y }

    // =========================================================================
    // Colisiones
    // =========================================================================

    fun isTilePassable(x: Int, y: Int): Boolean {
        if (!currentMap.inBounds(x, y)) return false
        for (layer in 0 until currentMap.layers.size) {
            if (!tileset.isPassable(currentMap.tileAt(layer, x, y))) return false
        }
        return true
    }

    /** ¿Puede una caja de personaje ocupar el centro (cx, cy)? */
    fun boxFits(cx: Float, cy: Float, ignoreEvents: Boolean = false): Boolean {
        val half = CHAR_HALF_BOX
        val minX = floor(cx - half).toInt()
        val maxX = floor(cx + half).toInt()
        val minY = floor(cy - half).toInt()
        val maxY = floor(cy + half).toInt()
        for (ty in minY..maxY) {
            for (tx in minX..maxX) {
                if (!isTilePassable(tx, ty)) return false
                if (!ignoreEvents) {
                    val e = eventAtTile(tx, ty)
                    if (e != null && e.page?.passable == false) return false
                }
            }
        }
        return true
    }

    /**
     * Mueve una caja separando ejes (permite deslizarse por las paredes).
     * Devuelve el par de posiciones finales.
     */
    private fun moveBox(cx: Float, cy: Float, dx: Float, dy: Float): Pair<Float, Float> {
        var nx = cx
        var ny = cy
        if (dx != 0f && boxFits(cx + dx, cy)) nx = cx + dx
        if (dy != 0f && boxFits(nx, cy + dy)) ny = cy + dy
        return nx to ny
    }

    // =========================================================================
    // Jugador
    // =========================================================================

    private fun updatePlayer(dt: Float, blocked: Boolean) {
        val p = player
        p.attackCooldown = (p.attackCooldown - dt).coerceAtLeast(0f)
        p.secondaryCooldown = (p.secondaryCooldown - dt).coerceAtLeast(0f)
        p.attackFlash = (p.attackFlash - dt).coerceAtLeast(0f)
        p.invulnTime = (p.invulnTime - dt).coerceAtLeast(0f)

        // Ruta forzada por MoveRoute (cinemáticas): tiene prioridad.
        if (p.routeActive) {
            updatePlayerRoute(dt)
            return
        }

        if (p.knockbackTime > 0f) {
            p.knockbackTime -= dt
            val (nx, ny) = moveBox(p.x, p.y, p.knockbackVx * dt, p.knockbackVy * dt)
            p.x = nx
            p.y = ny
            return
        }

        if (blocked) {
            p.moving = false
            return
        }

        val ix = inputX
        val iy = inputY
        val len = sqrt(ix * ix + iy * iy)
        if (len < 0.15f) {
            p.moving = false
            return
        }
        val nx = ix / maxOf(1f, len)
        val ny = iy / maxOf(1f, len)
        val speed = actor.moveSpeed
        val beforeTileX = p.tileX
        val beforeTileY = p.tileY
        val (px, py) = moveBox(p.x, p.y, nx * speed * dt, ny * speed * dt)
        p.moving = px != p.x || py != p.y
        p.x = px
        p.y = py
        if (p.moving) p.animTime += dt

        p.dir = when {
            abs(ix) >= abs(iy) && ix < 0 -> Direction.LEFT
            abs(ix) >= abs(iy) -> Direction.RIGHT
            iy < 0 -> Direction.UP
            else -> Direction.DOWN
        }

        // Disparadores por contacto: al entrar en la casilla del evento...
        if (p.tileX != beforeTileX || p.tileY != beforeTileY) {
            triggerTouchIfAny(p.tileX, p.tileY)
        }
        // ...o al chocar con un evento no atravesable de frente.
        if (!p.moving) {
            triggerTouchIfAny(p.tileX + p.dir.dx, p.tileY + p.dir.dy)
        }
        // Liberar el latch de los eventos que el jugador ya no pisa/empuja.
        for (e in events) {
            if (e.touchLatch) {
                val adjacent = (e.tileX == p.tileX && e.tileY == p.tileY) ||
                    (e.tileX == p.tileX + p.dir.dx && e.tileY == p.tileY + p.dir.dy && !p.moving)
                if (!adjacent) e.touchLatch = false
            }
        }
    }

    private fun updatePlayerRoute(dt: Float) {
        val p = player
        if (p.stepWait > 0f) {
            p.stepWait -= dt
            return
        }
        if (p.hasMoveTarget) {
            val speed = GRID_MOVE_SPEED * dt
            p.x = approach(p.x, p.moveTargetX, speed)
            p.y = approach(p.y, p.moveTargetY, speed)
            p.animTime += dt
            p.moving = true
            if (p.x == p.moveTargetX && p.y == p.moveTargetY) {
                p.hasMoveTarget = false
                p.moving = false
            }
            return
        }
        val step = p.moveQueue.removeFirstOrNull() ?: return
        when (step) {
            MoveStep.TURN_UP -> p.dir = Direction.UP
            MoveStep.TURN_DOWN -> p.dir = Direction.DOWN
            MoveStep.TURN_LEFT -> p.dir = Direction.LEFT
            MoveStep.TURN_RIGHT -> p.dir = Direction.RIGHT
            MoveStep.WAIT_HALF_SECOND -> p.stepWait = 0.5f
            else -> {
                val dir = step.toDirection()
                p.dir = dir
                val tx = p.tileX + dir.dx
                val ty = p.tileY + dir.dy
                if (isTilePassable(tx, ty) && eventAtTile(tx, ty)?.page?.passable != false) {
                    p.moveTargetX = tx + 0.5f
                    p.moveTargetY = ty + 0.5f
                    p.hasMoveTarget = true
                }
            }
        }
    }

    fun changePlayerHp(delta: Int) {
        state.hp = (state.hp + delta).coerceIn(0, state.maxHp)
        if (delta < 0) {
            effects.add(HitEffect(player.x, player.y, -delta, HitEffect.Kind.DAMAGE_PLAYER))
        } else if (delta > 0) {
            effects.add(HitEffect(player.x, player.y, delta, HitEffect.Kind.HEAL))
        }
    }

    /** Usa un objeto del inventario (desde el menú). Devuelve true si se usó. */
    fun useItem(itemId: Int): Boolean {
        val item = data.database.item(itemId) ?: return false
        if (state.itemCount(itemId) <= 0) return false
        when (item.effect) {
            ItemEffect.HEAL_HP -> {
                if (state.hp >= state.maxHp) return false
                changePlayerHp(item.power)
            }
            ItemEffect.NONE, ItemEffect.KEY -> return false
        }
        if (item.consumable) state.addItem(itemId, -1)
        soundQueue.add("heal")
        return true
    }

    // =========================================================================
    // Progresión: nivel, experiencia y stats efectivas
    // =========================================================================

    /** PV máximos efectivos = base + crecimiento por nivel + bonus del equipo. */
    val effectiveMaxHp: Int
        get() = actor.maxHp + actor.hpPerLevel * (state.level - 1) + equipBonus { it.maxHpBonus }

    /** Ataque efectivo = base + crecimiento por nivel + bonus del equipo. */
    val effectiveAttack: Int
        get() = actor.attack + actor.attackPerLevel * (state.level - 1) + equipBonus { it.attackBonus }

    /** Defensa efectiva = base + crecimiento por nivel + bonus del equipo. */
    val effectiveDefense: Int
        get() = actor.defense + actor.defensePerLevel * (state.level - 1) + equipBonus { it.defenseBonus }

    private inline fun equipBonus(selector: (Item) -> Int): Int {
        var total = 0
        state.weaponItemId?.let { id -> data.database.item(id)?.let { total += selector(it) } }
        state.armorItemId?.let { id -> data.database.item(id)?.let { total += selector(it) } }
        return total
    }

    /**
     * Sincroniza state.maxHp con [effectiveMaxHp]: si el máximo sube, el HP
     * actual sube en la misma cantidad; si baja, se recorta al nuevo máximo.
     */
    private fun syncMaxHp() {
        val newMax = effectiveMaxHp
        val diff = newMax - state.maxHp
        state.maxHp = newMax
        state.hp = if (diff > 0) (state.hp + diff).coerceAtMost(newMax) else state.hp.coerceAtMost(newMax)
    }

    /**
     * Acumula experiencia y sube de nivel mientras se alcance el umbral
     * (expBase * nivel actual), respetando maxLevel. Al subir aplica el
     * crecimiento de stats, cura el HP ganado, encola el sonido "levelup"
     * y publica [notice]. Las cantidades negativas o nulas se ignoran.
     */
    fun gainExp(amount: Int) {
        if (amount <= 0) return
        state.exp += amount
        var leveled = false
        while (state.level < actor.maxLevel && state.exp >= actor.expBase * state.level) {
            state.exp -= actor.expBase * state.level
            state.level++
            leveled = true
        }
        // En el nivel máximo no hay progreso hacia el siguiente.
        if (state.level >= actor.maxLevel) state.exp = 0
        if (leveled) {
            syncMaxHp()
            soundQueue.add("levelup")
            notice = "¡Nivel ${state.level}!"
        }
    }

    /** Suma oro (delta puede ser negativo); el total nunca baja de 0. */
    fun gainGold(delta: Int) {
        state.gold = (state.gold + delta).coerceAtLeast(0)
    }

    // =========================================================================
    // Equipo
    // =========================================================================

    /**
     * Equipa [itemId] en [slot] (null = desequipar). Equipar consume 1 unidad
     * del inventario y devuelve al inventario lo que estuviera equipado.
     * Devuelve false si el item no existe, no está en el inventario o su
     * equipSlot no coincide con [slot].
     */
    fun equip(slot: EquipSlot, itemId: Int?): Boolean {
        if (itemId != null) {
            val item = data.database.item(itemId) ?: return false
            if (item.equipSlot != slot) return false
            if (state.itemCount(itemId) <= 0) return false
        }
        val previous = when (slot) {
            EquipSlot.WEAPON -> state.weaponItemId
            EquipSlot.ARMOR -> state.armorItemId
        }
        if (itemId != null) state.addItem(itemId, -1)
        if (previous != null) state.addItem(previous, 1)
        when (slot) {
            EquipSlot.WEAPON -> state.weaponItemId = itemId
            EquipSlot.ARMOR -> state.armorItemId = itemId
        }
        syncMaxHp()
        return true
    }

    // =========================================================================
    // Tienda
    // =========================================================================

    /** Abre la tienda para el intérprete [owner]; falla si ya hay una abierta. */
    internal fun tryOpenShop(itemIds: List<Int>, owner: Interpreter): Boolean {
        if (shop != null) return false
        shop = ShopSession(itemIds)
        shopOwner = owner
        return true
    }

    /** La UI cierra la tienda; el intérprete que la abrió continúa. */
    fun closeShop() {
        if (shop == null) return
        shop = null
        shopOwner?.onShopClosed()
        shopOwner = null
    }

    /** Compra 1 unidad de [itemId]. Devuelve false si no existe o falta oro. */
    fun buyItem(itemId: Int): Boolean {
        val item = data.database.item(itemId) ?: return false
        if (state.gold < item.price) return false
        state.gold -= item.price
        state.addItem(itemId, 1)
        soundQueue.add("coin")
        return true
    }

    /**
     * Vende 1 unidad de [itemId] a mitad de precio (redondeando abajo).
     * Solo si hay existencias en el inventario y el precio es > 0; el equipo
     * equipado está fuera del inventario, así que no puede venderse.
     */
    fun sellItem(itemId: Int): Boolean {
        val item = data.database.item(itemId) ?: return false
        if (item.price <= 0) return false
        if (state.itemCount(itemId) <= 0) return false
        state.addItem(itemId, -1)
        state.gold += item.price / 2
        soundQueue.add("coin")
        return true
    }

    // =========================================================================
    // Combate
    // =========================================================================

    private fun performSkill(skill: Skill) {
        val p = player
        when (skill.kind) {
            SkillKind.MELEE -> {
                if (p.attackCooldown > 0f) return
                p.attackCooldown = skill.cooldownSeconds
                p.attackFlash = ATTACK_SWING_SECONDS
                p.attackDir = p.dir
                soundQueue.add("attack")
                val reach = skill.range
                val cx = p.x + p.dir.dx * (0.5f + reach / 2f)
                val cy = p.y + p.dir.dy * (0.5f + reach / 2f)
                val halfW = if (p.dir.dx != 0) reach / 2f + 0.3f else 0.8f
                val halfH = if (p.dir.dy != 0) reach / 2f + 0.3f else 0.8f
                for (enemy in enemies) {
                    if (!enemy.alive || enemy.invulnTime > 0f) continue
                    if (abs(enemy.x - cx) <= halfW && abs(enemy.y - cy) <= halfH) {
                        val damage = maxOf(1, effectiveAttack + skill.power - enemy.def.defense)
                        damageEnemy(enemy, damage, p.dir.dx.toFloat(), p.dir.dy.toFloat(), skill.knockback)
                    }
                }
            }

            SkillKind.PROJECTILE -> {
                if (p.secondaryCooldown > 0f) return
                p.secondaryCooldown = skill.cooldownSeconds
                soundQueue.add("shoot")
                projectiles.add(
                    Projectile(
                        x = p.x + p.dir.dx * 0.6f,
                        y = p.y + p.dir.dy * 0.6f,
                        dx = p.dir.dx.toFloat(),
                        dy = p.dir.dy.toFloat(),
                        speed = skill.projectileSpeed,
                        damage = maxOf(1, effectiveAttack + skill.power),
                        maxRange = skill.range,
                        knockback = skill.knockback,
                        fromPlayer = true,
                    ),
                )
            }
        }
    }

    private fun damageEnemy(enemy: EnemyEntity, damage: Int, knockDx: Float, knockDy: Float, knockback: Float) {
        enemy.hp -= damage
        enemy.invulnTime = 0.3f
        enemy.knockbackTime = 0.15f
        enemy.knockbackVx = knockDx * knockback / 0.15f
        enemy.knockbackVy = knockDy * knockback / 0.15f
        effects.add(HitEffect(enemy.x, enemy.y, damage, HitEffect.Kind.DAMAGE_ENEMY))
        soundQueue.add("hit")
        if (enemy.hp <= 0) {
            enemy.alive = false
            soundQueue.add("defeat")
            gainExp(enemy.def.expReward)
            gainGold(enemy.def.goldReward)
            val dropId = enemy.def.dropItemId
            if (dropId != null && random.nextFloat() < enemy.def.dropChance) {
                drops.add(DropEntity(enemy.x, enemy.y, dropId))
            }
        }
    }

    private fun damagePlayer(amount: Int, fromX: Float, fromY: Float) {
        if (player.invulnTime > 0f) return
        val damage = maxOf(1, amount - effectiveDefense)
        state.hp = (state.hp - damage).coerceAtLeast(0)
        player.invulnTime = 1f
        player.knockbackTime = 0.15f
        val dx = player.x - fromX
        val dy = player.y - fromY
        val len = maxOf(0.001f, sqrt(dx * dx + dy * dy))
        player.knockbackVx = dx / len * 3f
        player.knockbackVy = dy / len * 3f
        effects.add(HitEffect(player.x, player.y, damage, HitEffect.Kind.DAMAGE_PLAYER))
        soundQueue.add("hurt")
    }

    private fun updateEnemies(dt: Float) {
        for (enemy in enemies) {
            if (!enemy.alive) {
                enemy.deathTimer -= dt
                continue
            }
            enemy.invulnTime = (enemy.invulnTime - dt).coerceAtLeast(0f)

            if (enemy.knockbackTime > 0f) {
                enemy.knockbackTime -= dt
                val (nx, ny) = moveBox(enemy.x, enemy.y, enemy.knockbackVx * dt, enemy.knockbackVy * dt)
                enemy.x = nx
                enemy.y = ny
                continue
            }

            val dx = player.x - enemy.x
            val dy = player.y - enemy.y
            val dist = sqrt(dx * dx + dy * dy)

            var vx = 0f
            var vy = 0f
            when (enemy.def.behavior) {
                EnemyBehavior.STILL -> Unit
                EnemyBehavior.WANDER -> {
                    enemy.wanderTimer -= dt
                    if (enemy.wanderTimer <= 0f) {
                        enemy.wanderTimer = 1f + random.nextFloat() * 1.5f
                        if (random.nextFloat() < 0.35f) {
                            enemy.wanderDx = 0f
                            enemy.wanderDy = 0f
                        } else {
                            val angle = random.nextFloat() * (2f * Math.PI.toFloat())
                            enemy.wanderDx = kotlin.math.cos(angle)
                            enemy.wanderDy = kotlin.math.sin(angle)
                        }
                    }
                    vx = enemy.wanderDx
                    vy = enemy.wanderDy
                }
                EnemyBehavior.CHASE -> {
                    if (dist <= enemy.def.sightRange && dist > 0.05f) {
                        vx = dx / dist
                        vy = dy / dist
                    }
                }
            }

            if (vx != 0f || vy != 0f) {
                val speed = enemy.def.moveSpeed
                val (nx, ny) = moveBox(enemy.x, enemy.y, vx * speed * dt, vy * speed * dt)
                enemy.moving = nx != enemy.x || ny != enemy.y
                enemy.x = nx
                enemy.y = ny
                enemy.animTime += dt
                enemy.dir = when {
                    abs(vx) >= abs(vy) && vx < 0 -> Direction.LEFT
                    abs(vx) >= abs(vy) -> Direction.RIGHT
                    vy < 0 -> Direction.UP
                    else -> Direction.DOWN
                }
            } else {
                enemy.moving = false
            }

            // Daño por contacto.
            if (enemy.def.touchDamage &&
                abs(enemy.x - player.x) < 0.6f && abs(enemy.y - player.y) < 0.6f
            ) {
                damagePlayer(enemy.def.attack, enemy.x, enemy.y)
            }
        }
        enemies.removeAll { !it.alive && it.deathTimer <= 0f }
    }

    private fun updateProjectiles(dt: Float) {
        for (proj in projectiles) {
            if (!proj.alive) continue
            val step = proj.speed * dt
            proj.x += proj.dx * step
            proj.y += proj.dy * step
            proj.traveled += step
            if (proj.traveled >= proj.maxRange || !isTilePassable(proj.x.toInt(), proj.y.toInt())) {
                proj.alive = false
                continue
            }
            if (proj.fromPlayer) {
                for (enemy in enemies) {
                    if (!enemy.alive || enemy.invulnTime > 0f) continue
                    if (abs(enemy.x - proj.x) < 0.5f && abs(enemy.y - proj.y) < 0.5f) {
                        damageEnemy(enemy, proj.damage, proj.dx, proj.dy, proj.knockback)
                        proj.alive = false
                        break
                    }
                }
            } else if (abs(player.x - proj.x) < 0.5f && abs(player.y - proj.y) < 0.5f) {
                damagePlayer(proj.damage, proj.x - proj.dx, proj.y - proj.dy)
                proj.alive = false
            }
        }
        projectiles.removeAll { !it.alive }
    }

    private fun updateDrops() {
        for (drop in drops) {
            if (abs(drop.x - player.x) < 0.5f && abs(drop.y - player.y) < 0.5f) {
                drop.alive = false
                state.addItem(drop.itemId, 1)
                effects.add(HitEffect(drop.x, drop.y, 1, HitEffect.Kind.ITEM))
                soundQueue.add("pickup")
            }
        }
        drops.removeAll { !it.alive }
    }

    // =========================================================================
    // Eventos: movimiento
    // =========================================================================

    private fun updateEvents(dt: Float) {
        for (entity in events) {
            if (entity.erased || entity.page == null) continue
            if (entity.stepWait > 0f) {
                entity.stepWait -= dt
                continue
            }
            if (entity.hasMoveTarget) {
                val speed = GRID_MOVE_SPEED * dt
                entity.x = approach(entity.x, entity.moveTargetX + 0.5f, speed)
                entity.y = approach(entity.y, entity.moveTargetY + 0.5f, speed)
                entity.animTime += dt
                entity.moving = true
                if (entity.x == entity.moveTargetX + 0.5f && entity.y == entity.moveTargetY + 0.5f) {
                    entity.tileX = entity.moveTargetX
                    entity.tileY = entity.moveTargetY
                    entity.hasMoveTarget = false
                    entity.moving = false
                }
                continue
            }
            entity.moving = false

            val step = entity.moveQueue.removeFirstOrNull()
            if (step != null) {
                applyEventStep(entity, step)
                continue
            }

            // Movimiento aleatorio de la página (NPCs que pasean).
            if (entity.page?.moveType == MoveType.RANDOM && !uiBlocked) {
                entity.randomMoveTimer -= dt
                if (entity.randomMoveTimer <= 0f) {
                    entity.randomMoveTimer = 1f + random.nextFloat() * 2f
                    val dir = Direction.entries[random.nextInt(4)]
                    applyEventStep(
                        entity,
                        when (dir) {
                            Direction.UP -> MoveStep.UP
                            Direction.DOWN -> MoveStep.DOWN
                            Direction.LEFT -> MoveStep.LEFT
                            Direction.RIGHT -> MoveStep.RIGHT
                        },
                    )
                }
            }
        }
    }

    private fun applyEventStep(entity: EventEntity, step: MoveStep) {
        when (step) {
            MoveStep.TURN_UP -> entity.dir = Direction.UP
            MoveStep.TURN_DOWN -> entity.dir = Direction.DOWN
            MoveStep.TURN_LEFT -> entity.dir = Direction.LEFT
            MoveStep.TURN_RIGHT -> entity.dir = Direction.RIGHT
            MoveStep.WAIT_HALF_SECOND -> entity.stepWait = 0.5f
            else -> {
                val dir = step.toDirection()
                entity.dir = dir
                val tx = entity.tileX + dir.dx
                val ty = entity.tileY + dir.dy
                if (canEventEnter(entity, tx, ty)) {
                    entity.moveTargetX = tx
                    entity.moveTargetY = ty
                    entity.hasMoveTarget = true
                }
            }
        }
    }

    private fun canEventEnter(entity: EventEntity, tx: Int, ty: Int): Boolean {
        if (!isTilePassable(tx, ty)) return false
        val other = eventAtTile(tx, ty)
        if (other != null && other !== entity) return false
        if (player.tileX == tx && player.tileY == ty) return false
        return true
    }

    // ---- soporte de MoveRoute ------------------------------------------------

    fun resolveRouteTarget(target: Int, source: EventEntity?): Int = when (target) {
        EventCommand.MoveRoute.TARGET_SELF -> source?.event?.id ?: EventCommand.MoveRoute.TARGET_PLAYER
        else -> target
    }

    fun startMoveRoute(target: Int, steps: List<MoveStep>) {
        if (target == EventCommand.MoveRoute.TARGET_PLAYER) {
            player.moveQueue.clear()
            player.moveQueue.addAll(steps)
        } else {
            events.firstOrNull { it.event.id == target }?.let {
                it.moveQueue.clear()
                it.moveQueue.addAll(steps)
            }
        }
    }

    fun isRouteActive(target: Int): Boolean =
        if (target == EventCommand.MoveRoute.TARGET_PLAYER) {
            player.routeActive
        } else {
            events.firstOrNull { it.event.id == target }?.routeActive == true
        }

    // =========================================================================
    // Efectos de pantalla: tinte, destello y temblor
    // =========================================================================

    /**
     * Inicia una transición gradual del tinte de pantalla desde el tinte
     * actual hasta el RGBA dado (0..1) en [seconds]. El destino se persiste
     * en el estado; con seconds <= 0 el cambio es inmediato.
     */
    fun startTint(r: Float, g: Float, b: Float, a: Float, seconds: Float) {
        state.tintR = r
        state.tintG = g
        state.tintB = b
        state.tintA = a
        if (seconds <= 0f) {
            snapTintToTarget()
            return
        }
        tintCurrent.copyInto(tintStart)
        tintElapsed = 0f
        tintDuration = seconds
    }

    /** Lanza un destello del color RGB dado que decae a nada en [seconds]. */
    fun startFlash(r: Float, g: Float, b: Float, seconds: Float) {
        flashColor[0] = r
        flashColor[1] = g
        flashColor[2] = b
        flashDuration = maxOf(0.01f, seconds)
        flashIntensity = 1f
    }

    /** Inicia un temblor de cámara de [seconds] segundos. */
    fun startShake(seconds: Float) {
        shakeTimeLeft = maxOf(shakeTimeLeft, seconds)
    }

    private fun snapTintToTarget() {
        tintCurrent[0] = state.tintR
        tintCurrent[1] = state.tintG
        tintCurrent[2] = state.tintB
        tintCurrent[3] = state.tintA
        tintDuration = 0f
    }

    private fun updateScreenEffects(dt: Float) {
        if (tintDuration > 0f) {
            tintElapsed += dt
            if (tintElapsed >= tintDuration) {
                snapTintToTarget()
            } else {
                val t = tintElapsed / tintDuration
                tintCurrent[0] = tintStart[0] + (state.tintR - tintStart[0]) * t
                tintCurrent[1] = tintStart[1] + (state.tintG - tintStart[1]) * t
                tintCurrent[2] = tintStart[2] + (state.tintB - tintStart[2]) * t
                tintCurrent[3] = tintStart[3] + (state.tintA - tintStart[3]) * t
            }
        }
        flashIntensity = (flashIntensity - dt / flashDuration).coerceAtLeast(0f)
        shakeTimeLeft = (shakeTimeLeft - dt).coerceAtLeast(0f)
    }

    // =========================================================================
    // Mensajes y elecciones
    // =========================================================================

    internal fun tryShowMessage(text: String, speaker: String, owner: Interpreter): Boolean {
        if (message != null || choices != null) return false
        message = MessageBox(text, speaker)
        messageOwner = owner
        return true
    }

    internal fun tryShowChoices(options: List<String>, owner: Interpreter): Boolean {
        if (message != null || choices != null) return false
        choices = options
        choiceOwner = owner
        return true
    }

    /** La UI cierra la caja de mensaje actual. */
    fun dismissMessage() {
        if (message == null) return
        message = null
        messageOwner?.onMessageDismissed()
        messageOwner = null
    }

    /** La UI selecciona una opción del menú de elecciones. */
    fun selectChoice(index: Int) {
        if (choices == null) return
        choices = null
        choiceOwner?.onChoiceSelected(index)
        choiceOwner = null
        soundQueue.add("select")
    }
}

private fun approach(value: Float, target: Float, maxDelta: Float): Float = when {
    value < target -> minOf(target, value + maxDelta)
    value > target -> maxOf(target, value - maxDelta)
    else -> value
}

private fun MoveStep.toDirection(): Direction = when (this) {
    MoveStep.UP, MoveStep.TURN_UP -> Direction.UP
    MoveStep.DOWN, MoveStep.TURN_DOWN -> Direction.DOWN
    MoveStep.LEFT, MoveStep.TURN_LEFT -> Direction.LEFT
    else -> Direction.RIGHT
}
