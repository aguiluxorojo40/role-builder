package com.rolebuilder.editor.event

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rolebuilder.core.model.event.Condition
import com.rolebuilder.core.model.event.Direction
import com.rolebuilder.core.model.event.EventCommand
import com.rolebuilder.core.model.event.EventPage
import com.rolebuilder.core.model.event.EventTrigger
import com.rolebuilder.core.model.event.MapEvent
import com.rolebuilder.core.model.event.MoveType
import com.rolebuilder.core.model.event.PageConditions
import com.rolebuilder.core.model.event.SpriteRef
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.widgets.DropdownField
import com.rolebuilder.editor.widgets.FloatField
import com.rolebuilder.editor.widgets.IntField

/**
 * Editor completo de un evento del mapa: páginas, condiciones, apariencia,
 * disparador y lista de comandos. Los cambios se aplican en vivo.
 */
@Composable
fun EventEditorDialog(
    state: EditorState,
    event: MapEvent,
    onChange: (MapEvent?) -> Unit,
    onDismiss: () -> Unit,
) {
    var pageIndex by remember(event.id) { mutableIntStateOf(0) }
    val page = event.pages.getOrNull(pageIndex) ?: event.pages.last().also { pageIndex = event.pages.size - 1 }

    fun updatePage(newPage: EventPage) {
        onChange(event.copy(pages = event.pages.mapIndexed { i, p -> if (i == pageIndex) newPage else p }))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            // ---------- cabecera ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = event.name,
                    onValueChange = { onChange(event.copy(name = it)) },
                    label = { Text("Nombre del evento (${event.x}, ${event.y})") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onChange(null) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Borrar evento", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }

            // ---------- páginas ----------
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                event.pages.forEachIndexed { i, _ ->
                    FilterChip(
                        selected = pageIndex == i,
                        onClick = { pageIndex = i },
                        label = { Text("Página ${i + 1}") },
                    )
                }
                IconButton(onClick = {
                    onChange(event.copy(pages = event.pages + EventPage()))
                    pageIndex = event.pages.size
                }) { Icon(Icons.Filled.Add, contentDescription = "Añadir página") }
                if (event.pages.size > 1) {
                    IconButton(onClick = {
                        val pages = event.pages.filterIndexed { i, _ -> i != pageIndex }
                        pageIndex = pageIndex.coerceAtMost(pages.size - 1)
                        onChange(event.copy(pages = pages))
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Borrar página") }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ---------- apariencia y comportamiento ----------
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownField(
                        label = "Sprite",
                        options = listOf<String?>(null) + state.imageNames(),
                        selected = page.sprite?.image,
                        optionLabel = { it ?: "(invisible)" },
                        onSelect = { image ->
                            updatePage(page.copy(sprite = image?.let { SpriteRef(it, page.sprite?.direction ?: Direction.DOWN) }))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (page.sprite != null) {
                        DropdownField(
                            label = "Mirando",
                            options = Direction.entries,
                            selected = page.sprite?.direction,
                            optionLabel = { it.spanish() },
                            onSelect = { updatePage(page.copy(sprite = page.sprite?.copy(direction = it))) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownField(
                        label = "Disparador",
                        options = EventTrigger.entries,
                        selected = page.trigger,
                        optionLabel = { it.spanish() },
                        onSelect = { updatePage(page.copy(trigger = it)) },
                        modifier = Modifier.weight(1f),
                    )
                    DropdownField(
                        label = "Movimiento",
                        options = MoveType.entries,
                        selected = page.moveType,
                        optionLabel = { if (it == MoveType.FIXED) "Quieto" else "Aleatorio" },
                        onSelect = { updatePage(page.copy(moveType = it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = page.passable,
                        onCheckedChange = { updatePage(page.copy(passable = it)) },
                    )
                    Text("  Se puede atravesar")
                }

                HorizontalDivider()

                // ---------- condiciones de la página ----------
                Text("Condiciones de la página", style = MaterialTheme.typography.titleSmall)
                PageConditionsEditor(state, page.conditions) { updatePage(page.copy(conditions = it)) }

                HorizontalDivider()

                // ---------- comandos ----------
                Text("Comandos", style = MaterialTheme.typography.titleSmall)
                CommandListEditor(
                    state = state,
                    commands = page.commands,
                    onChange = { updatePage(page.copy(commands = it)) },
                )
            }
        }
    }
}

// =============================================================================
// Condiciones de página
// =============================================================================

@Composable
private fun PageConditionsEditor(
    state: EditorState,
    conditions: PageConditions,
    onChange: (PageConditions) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DropdownField(
                label = "Switch activado",
                options = listOf<Int?>(null) + state.project.switchNames.indices.map { it + 1 },
                selected = conditions.switchId,
                optionLabel = { it?.let { id -> state.switchLabel(id) } ?: "(ninguno)" },
                onSelect = { onChange(conditions.copy(switchId = it)) },
                modifier = Modifier.weight(1f),
            )
            DropdownField(
                label = "Self-switch activado",
                options = listOf(null, "A", "B", "C", "D"),
                selected = conditions.selfSwitch,
                optionLabel = { it ?: "(ninguno)" },
                onSelect = { onChange(conditions.copy(selfSwitch = it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            DropdownField(
                label = "Variable",
                options = listOf<Int?>(null) + state.project.variableNames.indices.map { it + 1 },
                selected = conditions.variableId,
                optionLabel = { it?.let { id -> state.variableLabel(id) } ?: "(ninguna)" },
                onSelect = { onChange(conditions.copy(variableId = it)) },
                modifier = Modifier.weight(1f),
            )
            if (conditions.variableId != null) {
                IntField(
                    label = "≥ valor",
                    value = conditions.variableAtLeast,
                    onChange = { onChange(conditions.copy(variableAtLeast = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
            DropdownField(
                label = "Tiene objeto",
                options = listOf<Int?>(null) + state.database.items.map { it.id },
                selected = conditions.itemId,
                optionLabel = { id -> id?.let { state.database.item(it)?.name ?: "Objeto $it" } ?: "(ninguno)" },
                onSelect = { onChange(conditions.copy(itemId = it)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// =============================================================================
// Lista de comandos (recursiva para ramas)
// =============================================================================

@Composable
fun CommandListEditor(
    state: EditorState,
    commands: List<EventCommand>,
    onChange: (List<EventCommand>) -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        commands.forEachIndexed { index, command ->
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        command.summary(state),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        if (index > 0) onChange(commands.swapped(index, index - 1))
                    }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir") }
                    IconButton(onClick = {
                        if (index < commands.size - 1) onChange(commands.swapped(index, index + 1))
                    }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar") }
                    IconButton(onClick = { editingIndex = index }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { onChange(commands.filterIndexed { i, _ -> i != index }) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                    }
                }
            }
        }
        TextButton(onClick = { showPicker = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" Añadir comando")
        }
    }

    if (showPicker) {
        CommandPickerDialog(
            onPick = { command ->
                showPicker = false
                onChange(commands + command)
                editingIndex = commands.size
            },
            onDismiss = { showPicker = false },
        )
    }

    editingIndex?.let { index ->
        commands.getOrNull(index)?.let { command ->
            CommandEditorDialog(
                state = state,
                command = command,
                onDone = { updated ->
                    onChange(commands.mapIndexed { i, c -> if (i == index) updated else c })
                    editingIndex = null
                },
                onDismiss = { editingIndex = null },
            )
        }
    }
}

private fun <T> List<T>.swapped(a: Int, b: Int): List<T> =
    toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }

// =============================================================================
// Selector de tipo de comando
// =============================================================================

private data class CommandOption(val label: String, val create: () -> EventCommand)

private val COMMAND_OPTIONS = listOf(
    CommandOption("Mostrar texto") { EventCommand.ShowText("") },
    CommandOption("Mostrar elecciones") {
        EventCommand.ShowChoices(listOf(EventCommand.ShowChoices.Choice("Sí"), EventCommand.ShowChoices.Choice("No")))
    },
    CommandOption("Cambiar switch") { EventCommand.SetSwitch(1, true) },
    CommandOption("Cambiar self-switch") { EventCommand.SetSelfSwitch("A", true) },
    CommandOption("Cambiar variable") { EventCommand.SetVariable(1) },
    CommandOption("Condición (si...)") { EventCommand.ConditionalBranch(Condition(Condition.Kind.SWITCH, 1)) },
    CommandOption("Teletransportar jugador") { EventCommand.TransferPlayer(1, 0, 0) },
    CommandOption("Esperar") { EventCommand.Wait(1f) },
    CommandOption("Ruta de movimiento") { EventCommand.MoveRoute(steps = emptyList()) },
    CommandOption("Dar/quitar objeto") { EventCommand.ChangeItems(1, 1) },
    CommandOption("Cambiar HP del jugador") { EventCommand.ChangeHp(-1) },
    CommandOption("Cambiar oro") { EventCommand.ChangeGold(10) },
    CommandOption("Dar experiencia") { EventCommand.ChangeExp(5) },
    CommandOption("Abrir tienda") { EventCommand.OpenShop(emptyList()) },
    CommandOption("Reproducir sonido") { EventCommand.PlaySound("chest") },
    CommandOption("Borrar este evento") { EventCommand.EraseEvent },
)

@Composable
private fun CommandPickerDialog(onPick: (EventCommand) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir comando") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                COMMAND_OPTIONS.forEach { option ->
                    TextButton(
                        onClick = { onPick(option.create()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(option.label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// =============================================================================
// Editores de parámetros por comando
// =============================================================================

@Composable
private fun CommandEditorDialog(
    state: EditorState,
    command: EventCommand,
    onDone: (EventCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    var edited by remember { mutableStateOf(command) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(edited.typeName(), style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CommandFields(state, edited) { edited = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onDone(edited) }) { Text("Aceptar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun CommandFields(
    state: EditorState,
    command: EventCommand,
    onChange: (EventCommand) -> Unit,
) {
    when (command) {
        is EventCommand.ShowText -> {
            OutlinedTextField(
                value = command.speaker,
                onValueChange = { onChange(command.copy(speaker = it)) },
                label = { Text("Nombre (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = command.text,
                onValueChange = { onChange(command.copy(text = it)) },
                label = { Text("Texto") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is EventCommand.ShowChoices -> {
            command.choices.forEachIndexed { index, choice ->
                Card {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = choice.text,
                                onValueChange = { text ->
                                    onChange(command.copy(choices = command.choices.mapIndexed { i, c -> if (i == index) c.copy(text = text) else c }))
                                },
                                label = { Text("Opción ${index + 1}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (command.choices.size > 2) {
                                IconButton(onClick = {
                                    onChange(command.copy(choices = command.choices.filterIndexed { i, _ -> i != index }))
                                }) { Icon(Icons.Filled.Delete, contentDescription = "Quitar opción") }
                            }
                        }
                        Text("Al elegirla:", style = MaterialTheme.typography.labelMedium)
                        CommandListEditor(
                            state = state,
                            commands = choice.commands,
                            onChange = { newCommands ->
                                onChange(command.copy(choices = command.choices.mapIndexed { i, c -> if (i == index) c.copy(commands = newCommands) else c }))
                            },
                        )
                    }
                }
            }
            if (command.choices.size < 4) {
                TextButton(onClick = {
                    onChange(command.copy(choices = command.choices + EventCommand.ShowChoices.Choice("Opción ${command.choices.size + 1}")))
                }) { Text("+ Añadir opción") }
            }
        }

        is EventCommand.SetSwitch -> {
            DropdownField(
                label = "Switch",
                options = state.project.switchNames.indices.map { it + 1 },
                selected = command.switchId,
                optionLabel = { state.switchLabel(it) },
                onSelect = { onChange(command.copy(switchId = it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = command.value, onCheckedChange = { onChange(command.copy(value = it)) })
                Text(if (command.value) "  Activar (ON)" else "  Desactivar (OFF)")
            }
        }

        is EventCommand.SetSelfSwitch -> {
            DropdownField(
                label = "Self-switch",
                options = listOf("A", "B", "C", "D"),
                selected = command.key,
                optionLabel = { it },
                onSelect = { onChange(command.copy(key = it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = command.value, onCheckedChange = { onChange(command.copy(value = it)) })
                Text(if (command.value) "  Activar (ON)" else "  Desactivar (OFF)")
            }
        }

        is EventCommand.SetVariable -> {
            DropdownField(
                label = "Variable",
                options = state.project.variableNames.indices.map { it + 1 },
                selected = command.variableId,
                optionLabel = { state.variableLabel(it) },
                onSelect = { onChange(command.copy(variableId = it)) },
            )
            DropdownField(
                label = "Operación",
                options = EventCommand.SetVariable.Op.entries,
                selected = command.op,
                optionLabel = {
                    when (it) {
                        EventCommand.SetVariable.Op.SET -> "= asignar"
                        EventCommand.SetVariable.Op.ADD -> "+ sumar"
                        EventCommand.SetVariable.Op.SUB -> "− restar"
                    }
                },
                onSelect = { onChange(command.copy(op = it)) },
            )
            IntField("Valor", command.value, { onChange(command.copy(value = it)) })
        }

        is EventCommand.ConditionalBranch -> {
            ConditionEditor(state, command.condition) { onChange(command.copy(condition = it)) }
            HorizontalDivider()
            Text("Si se cumple:", style = MaterialTheme.typography.labelLarge)
            CommandListEditor(state, command.then) { onChange(command.copy(then = it)) }
            HorizontalDivider()
            Text("Si NO se cumple:", style = MaterialTheme.typography.labelLarge)
            CommandListEditor(state, command.otherwise) { onChange(command.copy(otherwise = it)) }
        }

        is EventCommand.TransferPlayer -> {
            DropdownField(
                label = "Mapa",
                options = state.mapList.map { it.id },
                selected = command.mapId,
                optionLabel = { id -> state.map(id)?.let { "${it.id}: ${it.name}" } ?: "Mapa $id" },
                onSelect = { onChange(command.copy(mapId = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField("X", command.x, { onChange(command.copy(x = it)) }, Modifier.weight(1f))
                IntField("Y", command.y, { onChange(command.copy(y = it)) }, Modifier.weight(1f))
            }
            DropdownField(
                label = "Mirando",
                options = listOf<Direction?>(null) + Direction.entries,
                selected = command.direction,
                optionLabel = { it?.spanish() ?: "(mantener)" },
                onSelect = { onChange(command.copy(direction = it)) },
            )
        }

        is EventCommand.Wait -> {
            FloatField("Segundos", command.seconds, { onChange(command.copy(seconds = it)) })
        }

        is EventCommand.MoveRoute -> {
            DropdownField(
                label = "Objetivo",
                options = listOf(EventCommand.MoveRoute.TARGET_SELF, EventCommand.MoveRoute.TARGET_PLAYER) +
                    (state.currentMap?.events?.map { it.id } ?: emptyList()),
                selected = command.target,
                optionLabel = {
                    when (it) {
                        EventCommand.MoveRoute.TARGET_SELF -> "Este evento"
                        EventCommand.MoveRoute.TARGET_PLAYER -> "Jugador"
                        else -> state.currentMap?.events?.firstOrNull { e -> e.id == it }?.name ?: "Evento $it"
                    }
                },
                onSelect = { onChange(command.copy(target = it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = command.waitForCompletion, onCheckedChange = { onChange(command.copy(waitForCompletion = it)) })
                Text("  Esperar a que termine")
            }
            Text("Pasos: " + command.steps.joinToString(" → ") { it.spanish() }.ifBlank { "(sin pasos)" })
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                com.rolebuilder.core.model.event.MoveStep.entries.forEach { step ->
                    FilterChip(
                        selected = false,
                        onClick = { onChange(command.copy(steps = command.steps + step)) },
                        label = { Text(step.spanish()) },
                    )
                }
            }
            if (command.steps.isNotEmpty()) {
                TextButton(onClick = { onChange(command.copy(steps = command.steps.dropLast(1))) }) {
                    Text("Quitar último paso")
                }
            }
        }

        is EventCommand.ChangeItems -> {
            DropdownField(
                label = "Objeto",
                options = state.database.items.map { it.id },
                selected = command.itemId,
                optionLabel = { state.database.item(it)?.name ?: "Objeto $it" },
                onSelect = { onChange(command.copy(itemId = it)) },
            )
            IntField("Cantidad (+ dar, − quitar)", command.delta, { onChange(command.copy(delta = it)) })
        }

        is EventCommand.ChangeHp -> {
            IntField("HP (+ curar, − dañar)", command.delta, { onChange(command.copy(delta = it)) })
        }

        is EventCommand.ChangeGold -> {
            IntField("Oro (+ dar, − quitar)", command.delta, { onChange(command.copy(delta = it)) })
            Text("El oro del grupo nunca baja de 0.", style = MaterialTheme.typography.bodySmall)
        }

        is EventCommand.ChangeExp -> {
            IntField("Cantidad de experiencia", command.delta, { onChange(command.copy(delta = it.coerceAtLeast(0))) })
            Text("Puede hacer subir de nivel al jugador.", style = MaterialTheme.typography.bodySmall)
        }

        is EventCommand.OpenShop -> {
            Text("En venta (en este orden):", style = MaterialTheme.typography.labelLarge)
            if (command.itemIds.isEmpty()) {
                Text("(ningún objeto todavía)", style = MaterialTheme.typography.bodySmall)
            }
            command.itemIds.forEachIndexed { index, itemId ->
                val item = state.database.item(itemId)
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${item?.name ?: "Objeto $itemId"} · ${item?.price ?: 0} oro",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            if (index > 0) onChange(command.copy(itemIds = command.itemIds.swapped(index, index - 1)))
                        }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir") }
                        IconButton(onClick = {
                            if (index < command.itemIds.size - 1) onChange(command.copy(itemIds = command.itemIds.swapped(index, index + 1)))
                        }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar") }
                        IconButton(onClick = {
                            onChange(command.copy(itemIds = command.itemIds.filterIndexed { i, _ -> i != index }))
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Quitar") }
                    }
                }
            }
            HorizontalDivider()
            Text("Catálogo de la base de datos:", style = MaterialTheme.typography.labelLarge)
            state.database.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.id in command.itemIds,
                        onCheckedChange = { checked ->
                            onChange(
                                command.copy(
                                    itemIds = if (checked) command.itemIds + item.id
                                    else command.itemIds.filter { it != item.id },
                                ),
                            )
                        },
                    )
                    Text("${item.name} · ${item.price} oro", modifier = Modifier.weight(1f))
                }
            }
        }

        is EventCommand.PlaySound -> {
            DropdownField(
                label = "Sonido",
                options = listOf("attack", "hit", "hurt", "defeat", "pickup", "select", "chest", "heal", "shoot"),
                selected = command.name,
                optionLabel = { it },
                onSelect = { onChange(command.copy(name = it)) },
            )
        }

        EventCommand.EraseEvent -> {
            Text("El evento desaparecerá hasta que se recargue el mapa.")
        }
    }
}

@Composable
private fun ConditionEditor(
    state: EditorState,
    condition: Condition,
    onChange: (Condition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropdownField(
            label = "Tipo de condición",
            options = Condition.Kind.entries,
            selected = condition.kind,
            optionLabel = {
                when (it) {
                    Condition.Kind.SWITCH -> "Switch"
                    Condition.Kind.SELF_SWITCH -> "Self-switch"
                    Condition.Kind.VARIABLE_AT_LEAST -> "Variable ≥ valor"
                    Condition.Kind.HAS_ITEM -> "Tiene objeto"
                    Condition.Kind.GOLD_AT_LEAST -> "Oro ≥ cantidad"
                }
            },
            onSelect = { onChange(condition.copy(kind = it)) },
        )
        when (condition.kind) {
            Condition.Kind.SWITCH -> {
                DropdownField(
                    label = "Switch",
                    options = state.project.switchNames.indices.map { it + 1 },
                    selected = condition.id,
                    optionLabel = { state.switchLabel(it) },
                    onSelect = { onChange(condition.copy(id = it)) },
                )
                ExpectedToggle(condition.expected, "está ON", "está OFF") { onChange(condition.copy(expected = it)) }
            }
            Condition.Kind.SELF_SWITCH -> {
                DropdownField(
                    label = "Self-switch (de este evento)",
                    options = listOf("A", "B", "C", "D"),
                    selected = condition.key,
                    optionLabel = { it },
                    onSelect = { onChange(condition.copy(key = it)) },
                )
                ExpectedToggle(condition.expected, "está ON", "está OFF") { onChange(condition.copy(expected = it)) }
            }
            Condition.Kind.VARIABLE_AT_LEAST -> {
                DropdownField(
                    label = "Variable",
                    options = state.project.variableNames.indices.map { it + 1 },
                    selected = condition.id,
                    optionLabel = { state.variableLabel(it) },
                    onSelect = { onChange(condition.copy(id = it)) },
                )
                IntField("Valor mínimo", condition.value, { onChange(condition.copy(value = it)) })
            }
            Condition.Kind.HAS_ITEM -> {
                DropdownField(
                    label = "Objeto",
                    options = state.database.items.map { it.id },
                    selected = condition.id,
                    optionLabel = { state.database.item(it)?.name ?: "Objeto $it" },
                    onSelect = { onChange(condition.copy(id = it)) },
                )
                ExpectedToggle(condition.expected, "lo tiene", "NO lo tiene") { onChange(condition.copy(expected = it)) }
            }
            Condition.Kind.GOLD_AT_LEAST -> {
                IntField("Oro mínimo", condition.value, { onChange(condition.copy(value = it.coerceAtLeast(0))) })
            }
        }
    }
}

@Composable
private fun ExpectedToggle(value: Boolean, onLabel: String, offLabel: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = value, onCheckedChange = onChange)
        Text("  " + if (value) onLabel else offLabel)
    }
}

// =============================================================================
// Textos
// =============================================================================

fun EditorState.switchLabel(id: Int): String {
    val name = project.switchNames.getOrNull(id - 1).orEmpty()
    return "%04d%s".format(id, if (name.isBlank()) "" else ": $name")
}

fun EditorState.variableLabel(id: Int): String {
    val name = project.variableNames.getOrNull(id - 1).orEmpty()
    return "%04d%s".format(id, if (name.isBlank()) "" else ": $name")
}

fun Direction.spanish(): String = when (this) {
    Direction.DOWN -> "Abajo"
    Direction.LEFT -> "Izquierda"
    Direction.RIGHT -> "Derecha"
    Direction.UP -> "Arriba"
}

fun EventTrigger.spanish(): String = when (this) {
    EventTrigger.ACTION_BUTTON -> "Botón de acción"
    EventTrigger.PLAYER_TOUCH -> "Al tocarlo"
    EventTrigger.AUTORUN -> "Automático (bloquea)"
    EventTrigger.PARALLEL -> "Paralelo (fondo)"
}

fun com.rolebuilder.core.model.event.MoveStep.spanish(): String = when (this) {
    com.rolebuilder.core.model.event.MoveStep.UP -> "↑"
    com.rolebuilder.core.model.event.MoveStep.DOWN -> "↓"
    com.rolebuilder.core.model.event.MoveStep.LEFT -> "←"
    com.rolebuilder.core.model.event.MoveStep.RIGHT -> "→"
    com.rolebuilder.core.model.event.MoveStep.TURN_UP -> "Mirar ↑"
    com.rolebuilder.core.model.event.MoveStep.TURN_DOWN -> "Mirar ↓"
    com.rolebuilder.core.model.event.MoveStep.TURN_LEFT -> "Mirar ←"
    com.rolebuilder.core.model.event.MoveStep.TURN_RIGHT -> "Mirar →"
    com.rolebuilder.core.model.event.MoveStep.WAIT_HALF_SECOND -> "Esperar ½s"
}

private fun EventCommand.typeName(): String = when (this) {
    is EventCommand.ShowText -> "Mostrar texto"
    is EventCommand.ShowChoices -> "Mostrar elecciones"
    is EventCommand.SetSwitch -> "Cambiar switch"
    is EventCommand.SetSelfSwitch -> "Cambiar self-switch"
    is EventCommand.SetVariable -> "Cambiar variable"
    is EventCommand.ConditionalBranch -> "Condición"
    is EventCommand.TransferPlayer -> "Teletransportar jugador"
    is EventCommand.Wait -> "Esperar"
    is EventCommand.MoveRoute -> "Ruta de movimiento"
    is EventCommand.ChangeItems -> "Dar/quitar objeto"
    is EventCommand.ChangeHp -> "Cambiar HP"
    is EventCommand.ChangeGold -> "Cambiar oro"
    is EventCommand.ChangeExp -> "Dar experiencia"
    is EventCommand.OpenShop -> "Abrir tienda"
    is EventCommand.PlaySound -> "Reproducir sonido"
    EventCommand.EraseEvent -> "Borrar este evento"
}

fun EventCommand.summary(state: EditorState): String = when (this) {
    is EventCommand.ShowText -> "💬 " + (if (speaker.isBlank()) "" else "[$speaker] ") + text.take(40)
    is EventCommand.ShowChoices -> "❓ Elecciones: " + choices.joinToString(" / ") { it.text }
    is EventCommand.SetSwitch -> "🔘 Switch ${state.switchLabel(switchId)} = ${if (value) "ON" else "OFF"}"
    is EventCommand.SetSelfSwitch -> "🔒 Self-switch $key = ${if (value) "ON" else "OFF"}"
    is EventCommand.SetVariable -> {
        val op = when (this.op) {
            EventCommand.SetVariable.Op.SET -> "="
            EventCommand.SetVariable.Op.ADD -> "+="
            EventCommand.SetVariable.Op.SUB -> "−="
        }
        "🔢 Variable ${state.variableLabel(variableId)} $op $value"
    }
    is EventCommand.ConditionalBranch -> "🔀 Si… (${then.size} / ${otherwise.size} comandos)"
    is EventCommand.TransferPlayer -> "🚪 Ir a mapa $mapId ($x, $y)"
    is EventCommand.Wait -> "⏱ Esperar ${seconds}s"
    is EventCommand.MoveRoute -> "🚶 Ruta (${steps.size} pasos)"
    is EventCommand.ChangeItems -> "🎒 ${state.database.item(itemId)?.name ?: "Objeto $itemId"} ${if (delta >= 0) "+$delta" else "$delta"}"
    is EventCommand.ChangeHp -> "❤ HP ${if (delta >= 0) "+$delta" else "$delta"}"
    is EventCommand.ChangeGold -> "🪙 Oro ${if (delta >= 0) "+$delta" else "$delta"}"
    is EventCommand.ChangeExp -> "⭐ Exp +$delta"
    is EventCommand.OpenShop -> {
        val names = itemIds.map { state.database.item(it)?.name ?: "Objeto $it" }
        "🛒 Tienda: " + when {
            names.isEmpty() -> "(vacía)"
            names.size > 3 -> names.take(3).joinToString(", ") + "…"
            else -> names.joinToString(", ")
        }
    }
    is EventCommand.PlaySound -> "🔊 $name"
    EventCommand.EraseEvent -> "🗑 Borrar este evento"
}
