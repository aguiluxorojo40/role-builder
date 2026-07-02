package com.rolebuilder.core.engine

import com.rolebuilder.core.model.event.Condition
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.SpriteRef

/**
 * Ejecuta listas de [EventCommand] paso a paso. No usa hilos: el motor llama
 * a [update] en cada tick y el intérprete avanza hasta encontrar un comando
 * bloqueante (mensaje, espera, ruta de movimiento...).
 */
class Interpreter(private val engine: RpgEngine) {

    private class Frame(val commands: List<EventCommand>, var index: Int = 0)

    private val stack = ArrayDeque<Frame>()

    /** Evento que lanzó la ejecución (para self-switches y EraseEvent). */
    var source: EventEntity? = null
        private set

    /** Mapa en el que empezó la ejecución (los self-switches lo usan). */
    private var sourceMapId = 0

    private var waitTimer = 0f
    private var waitingMessage = false
    private var waitingChoice: EventCommand.ShowChoices? = null
    private var waitingMoveTarget: Int? = null
    private var waitingShop = false

    val running: Boolean get() = stack.isNotEmpty()

    fun start(commands: List<EventCommand>, source: EventEntity?) {
        stack.clear()
        waitTimer = 0f
        waitingMessage = false
        waitingChoice = null
        waitingMoveTarget = null
        waitingShop = false
        this.source = source
        sourceMapId = engine.state.mapId
        if (commands.isNotEmpty()) stack.addLast(Frame(commands))
    }

    fun stop() {
        stack.clear()
        waitingMessage = false
        waitingChoice = null
        waitingMoveTarget = null
        waitingShop = false
        source = null
    }

    /** Llamado por el motor cuando el jugador cierra la caja de mensaje. */
    fun onMessageDismissed() {
        waitingMessage = false
    }

    /** Llamado por el motor cuando la UI cierra la tienda. */
    fun onShopClosed() {
        waitingShop = false
    }

    /** Llamado por el motor cuando el jugador elige una opción. */
    fun onChoiceSelected(index: Int) {
        val choices = waitingChoice ?: return
        waitingChoice = null
        choices.choices.getOrNull(index)?.let { choice ->
            if (choice.commands.isNotEmpty()) stack.addLast(Frame(choice.commands))
        }
    }

    fun update(dt: Float) {
        if (!running) return
        if (waitTimer > 0f) {
            waitTimer -= dt
            if (waitTimer > 0f) return
        }
        if (waitingMessage || waitingChoice != null || waitingShop) return
        waitingMoveTarget?.let { target ->
            if (engine.isRouteActive(target)) return
            waitingMoveTarget = null
        }

        var guard = 0
        while (running && guard++ < 1000) {
            val frame = stack.last()
            if (frame.index >= frame.commands.size) {
                stack.removeLast()
                continue
            }
            when (execute(frame.commands[frame.index])) {
                StepResult.CONTINUE -> frame.index++
                StepResult.BLOCKED_AFTER -> { frame.index++; return }
                StepResult.RETRY_LATER -> return
            }
        }
    }

    private enum class StepResult { CONTINUE, BLOCKED_AFTER, RETRY_LATER }

    private fun execute(cmd: EventCommand): StepResult {
        val state = engine.state
        return when (cmd) {
            is EventCommand.ShowText -> {
                if (!engine.tryShowMessage(cmd.text, cmd.speaker, this)) return StepResult.RETRY_LATER
                waitingMessage = true
                StepResult.BLOCKED_AFTER
            }

            is EventCommand.ShowChoices -> {
                if (!engine.tryShowChoices(cmd.choices.map { it.text }, this)) return StepResult.RETRY_LATER
                waitingChoice = cmd
                StepResult.BLOCKED_AFTER
            }

            is EventCommand.SetSwitch -> {
                state.switches[cmd.switchId] = cmd.value
                StepResult.CONTINUE
            }

            is EventCommand.SetSelfSwitch -> {
                source?.let {
                    state.selfSwitches[GameState.selfSwitchKey(sourceMapId, it.event.id, cmd.key)] = cmd.value
                }
                StepResult.CONTINUE
            }

            is EventCommand.SetVariable -> {
                val old = state.variable(cmd.variableId)
                state.variables[cmd.variableId] = when (cmd.op) {
                    EventCommand.SetVariable.Op.SET -> cmd.value
                    EventCommand.SetVariable.Op.ADD -> old + cmd.value
                    EventCommand.SetVariable.Op.SUB -> old - cmd.value
                }
                StepResult.CONTINUE
            }

            is EventCommand.ConditionalBranch -> {
                val branch = if (engine.evaluate(cmd.condition)) cmd.then else cmd.otherwise
                if (branch.isNotEmpty()) stack.addLast(Frame(branch))
                StepResult.CONTINUE
            }

            is EventCommand.TransferPlayer -> {
                engine.transferPlayer(cmd.mapId, cmd.x, cmd.y, cmd.direction)
                StepResult.CONTINUE
            }

            is EventCommand.Wait -> {
                waitTimer = cmd.seconds
                StepResult.BLOCKED_AFTER
            }

            is EventCommand.MoveRoute -> {
                val target = engine.resolveRouteTarget(cmd.target, source)
                engine.startMoveRoute(target, cmd.steps)
                if (cmd.waitForCompletion) {
                    waitingMoveTarget = target
                    StepResult.BLOCKED_AFTER
                } else {
                    StepResult.CONTINUE
                }
            }

            is EventCommand.ChangeItems -> {
                state.addItem(cmd.itemId, cmd.delta)
                StepResult.CONTINUE
            }

            is EventCommand.ChangeHp -> {
                engine.changePlayerHp(cmd.delta)
                StepResult.CONTINUE
            }

            is EventCommand.ChangeGold -> {
                engine.gainGold(cmd.delta)
                StepResult.CONTINUE
            }

            is EventCommand.ChangeExp -> {
                engine.gainExp(cmd.delta)
                StepResult.CONTINUE
            }

            is EventCommand.OpenShop -> {
                if (!engine.tryOpenShop(cmd.itemIds, this)) return StepResult.RETRY_LATER
                waitingShop = true
                StepResult.BLOCKED_AFTER
            }

            is EventCommand.PlaySound -> {
                engine.soundQueue.add(cmd.name)
                StepResult.CONTINUE
            }

            EventCommand.EraseEvent -> {
                source?.erased = true
                StepResult.CONTINUE
            }

            is EventCommand.PlayMusic -> {
                engine.setBgm(cmd.track)
                StepResult.CONTINUE
            }

            EventCommand.StopMusic -> {
                engine.setBgm(null)
                StepResult.CONTINUE
            }

            is EventCommand.ChangeEventSprite -> {
                source?.let { entity ->
                    entity.spriteOverridden = true
                    entity.spriteOverride = cmd.image?.let { SpriteRef(it, cmd.direction) }
                }
                StepResult.CONTINUE
            }

            is EventCommand.SetVariableRandom -> {
                state.variables[cmd.variableId] = engine.randomInt(cmd.min, cmd.max)
                StepResult.CONTINUE
            }
        }
    }
}

/** Evaluación de condiciones compartida por intérprete y páginas de evento. */
fun RpgEngine.evaluate(condition: Condition): Boolean = when (condition.kind) {
    Condition.Kind.SWITCH -> state.switchOn(condition.id) == condition.expected
    Condition.Kind.SELF_SWITCH ->
        state.selfSwitch(state.mapId, condition.id, condition.key) == condition.expected
    Condition.Kind.VARIABLE_AT_LEAST -> state.variable(condition.id) >= condition.value
    Condition.Kind.HAS_ITEM -> (state.itemCount(condition.id) > 0) == condition.expected
    Condition.Kind.GOLD_AT_LEAST -> state.gold >= condition.value
}
