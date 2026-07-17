package com.rolebuilder.editor.database

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rolebuilder.core.model.Actor
import com.rolebuilder.core.model.Enemy
import com.rolebuilder.core.model.EnemyBehavior
import com.rolebuilder.core.model.EquipSlot
import com.rolebuilder.core.model.Item
import com.rolebuilder.core.model.ItemEffect
import com.rolebuilder.core.model.Skill
import com.rolebuilder.core.model.SkillKind
import com.rolebuilder.core.model.TileAnimation
import com.rolebuilder.core.model.Tileset
import com.rolebuilder.editor.EditorState
import com.rolebuilder.editor.loadImageBitmap
import com.rolebuilder.editor.widgets.DropdownField
import com.rolebuilder.editor.widgets.FloatField
import com.rolebuilder.editor.widgets.IntField

/** Base de datos del proyecto: pestañas de actores, enemigos, objetos, habilidades y tileset. */
@Composable
fun DatabaseTab(state: EditorState) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Actores", "Enemigos", "Objetos", "Habilidades", "Tileset")

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> ActorList(state)
            1 -> EnemyList(state)
            2 -> ItemList(state)
            3 -> SkillList(state)
            else -> TilesetEditor(state)
        }
    }
}

// =============================================================================
// Listas genéricas
// =============================================================================

@Composable
private fun <T> EntityList(
    entries: List<T>,
    title: (T) -> String,
    subtitle: (T) -> String,
    onAdd: () -> Unit,
    onDelete: (T) -> Unit,
    onEdit: (T) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(entries) { entry ->
            Card(modifier = Modifier.clickable { onEdit(entry) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title(entry), style = MaterialTheme.typography.titleSmall)
                        Text(subtitle(entry), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                    }
                }
            }
        }
        item {
            TextButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" Añadir")
            }
        }
    }
}

@Composable
private fun EditDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                content()
            }
            Button(onClick = onDismiss) { Text("Hecho") }
        }
    }
}

// =============================================================================
// Actores
// =============================================================================

@Composable
private fun ActorList(state: EditorState) {
    var editing by remember { mutableStateOf<Int?>(null) }
    val db = state.database

    EntityList(
        entries = db.actors,
        title = { "${it.id}: ${it.name}" },
        subtitle = { "PV ${it.maxHp} · ATQ ${it.attack} · DEF ${it.defense}" },
        onAdd = {
            val id = (db.actors.maxOfOrNull { it.id } ?: 0) + 1
            state.updateDatabase(db.copy(actors = db.actors + Actor(id = id, name = "Actor $id")))
        },
        onDelete = { actor ->
            if (db.actors.size > 1) state.updateDatabase(db.copy(actors = db.actors - actor))
        },
        onEdit = { editing = it.id },
    )

    editing?.let { id ->
        val actor = state.database.actor(id) ?: return
        fun update(a: Actor) = state.updateDatabase(state.database.copy(actors = state.database.actors.map { if (it.id == id) a else it }))
        EditDialog("Actor ${actor.id}", onDismiss = { editing = null }) {
            OutlinedTextField(actor.name, { update(actor.copy(name = it)) }, label = { Text("Nombre") }, singleLine = true)
            DropdownField("Sprite", state.imageNames(), actor.sprite, { it }, { update(actor.copy(sprite = it)) })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField("PV máx", actor.maxHp, { update(actor.copy(maxHp = it.coerceAtLeast(1))) }, Modifier.weight(1f))
                IntField("Ataque", actor.attack, { update(actor.copy(attack = it)) }, Modifier.weight(1f))
                IntField("Defensa", actor.defense, { update(actor.copy(defense = it)) }, Modifier.weight(1f))
            }
            FloatField("Velocidad (casillas/s)", actor.moveSpeed, { update(actor.copy(moveSpeed = it)) })
            DropdownField(
                "Ataque (botón A)",
                state.database.skills.map { it.id },
                actor.attackSkillId,
                { state.database.skill(it)?.name ?: "Habilidad $it" },
                { update(actor.copy(attackSkillId = it)) },
            )
            Text("Progresión por nivel", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField("PV/nivel", actor.hpPerLevel, { update(actor.copy(hpPerLevel = it)) }, Modifier.weight(1f))
                IntField("ATQ/nivel", actor.attackPerLevel, { update(actor.copy(attackPerLevel = it)) }, Modifier.weight(1f))
                IntField("DEF/nivel", actor.defensePerLevel, { update(actor.copy(defensePerLevel = it)) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField("Exp base", actor.expBase, { update(actor.copy(expBase = it.coerceAtLeast(1))) }, Modifier.weight(1f))
                IntField("Nivel máximo", actor.maxLevel, { update(actor.copy(maxLevel = it.coerceIn(1, 99))) }, Modifier.weight(1f))
            }
            DropdownField(
                "Habilidad secundaria (botón B)",
                listOf<Int?>(null) + state.database.skills.map { it.id },
                actor.secondarySkillId,
                { id2 -> id2?.let { state.database.skill(it)?.name ?: "Habilidad $it" } ?: "(ninguna)" },
                { update(actor.copy(secondarySkillId = it)) },
            )
        }
    }
}

// =============================================================================
// Enemigos
// =============================================================================

@Composable
private fun EnemyList(state: EditorState) {
    var editing by remember { mutableStateOf<Int?>(null) }
    val db = state.database

    EntityList(
        entries = db.enemies,
        title = { "${it.id}: ${it.name}" },
        subtitle = { "PV ${it.maxHp} · ATQ ${it.attack} · ${it.behavior.spanish()}" },
        onAdd = {
            val id = (db.enemies.maxOfOrNull { it.id } ?: 0) + 1
            state.updateDatabase(db.copy(enemies = db.enemies + Enemy(id = id, name = "Enemigo $id")))
        },
        onDelete = { state.updateDatabase(db.copy(enemies = db.enemies - it)) },
        onEdit = { editing = it.id },
    )

    editing?.let { id ->
        val enemy = state.database.enemy(id) ?: return
        fun update(e: Enemy) = state.updateDatabase(state.database.copy(enemies = state.database.enemies.map { if (it.id == id) e else it }))
        EditDialog("Enemigo ${enemy.id}", onDismiss = { editing = null }) {
            OutlinedTextField(enemy.name, { update(enemy.copy(name = it)) }, label = { Text("Nombre") }, singleLine = true)
            DropdownField("Sprite", state.imageNames(), enemy.sprite, { it }, { update(enemy.copy(sprite = it)) })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField("PV", enemy.maxHp, { update(enemy.copy(maxHp = it.coerceAtLeast(1))) }, Modifier.weight(1f))
                IntField("Ataque", enemy.attack, { update(enemy.copy(attack = it)) }, Modifier.weight(1f))
                IntField("Defensa", enemy.defense, { update(enemy.copy(defense = it)) }, Modifier.weight(1f))
            }
            FloatField("Velocidad (casillas/s)", enemy.moveSpeed, { update(enemy.copy(moveSpeed = it)) })
            DropdownField(
                "Comportamiento",
                EnemyBehavior.entries,
                enemy.behavior,
                { it.spanish() },
                { update(enemy.copy(behavior = it)) },
            )
            IntField("Rango de visión (casillas)", enemy.sightRange, { update(enemy.copy(sightRange = it)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(enemy.touchDamage, { update(enemy.copy(touchDamage = it)) })
                Text("  Daña al tocar al jugador")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField("Exp que da", enemy.expReward, { update(enemy.copy(expReward = it)) }, Modifier.weight(1f))
                IntField("Oro que da", enemy.goldReward, { update(enemy.copy(goldReward = it)) }, Modifier.weight(1f))
            }
            DropdownField(
                "Objeto que suelta",
                listOf<Int?>(null) + state.database.items.map { it.id },
                enemy.dropItemId,
                { id2 -> id2?.let { state.database.item(it)?.name ?: "Objeto $it" } ?: "(nada)" },
                { update(enemy.copy(dropItemId = it)) },
            )
            if (enemy.dropItemId != null) {
                FloatField("Probabilidad (0-1)", enemy.dropChance, { update(enemy.copy(dropChance = it.coerceIn(0f, 1f))) })
            }
        }
    }
}

private fun EnemyBehavior.spanish(): String = when (this) {
    EnemyBehavior.STILL -> "Quieto"
    EnemyBehavior.WANDER -> "Deambula"
    EnemyBehavior.CHASE -> "Persigue"
}

// =============================================================================
// Objetos
// =============================================================================

@Composable
private fun ItemList(state: EditorState) {
    var editing by remember { mutableStateOf<Int?>(null) }
    val db = state.database

    EntityList(
        entries = db.items,
        title = { "${it.id}: ${it.name}" },
        subtitle = { it.description.ifBlank { it.effect.spanish() } },
        onAdd = {
            val id = (db.items.maxOfOrNull { it.id } ?: 0) + 1
            state.updateDatabase(db.copy(items = db.items + Item(id = id, name = "Objeto $id")))
        },
        onDelete = { state.updateDatabase(db.copy(items = db.items - it)) },
        onEdit = { editing = it.id },
    )

    editing?.let { id ->
        val item = state.database.item(id) ?: return
        fun update(i: Item) = state.updateDatabase(state.database.copy(items = state.database.items.map { if (it.id == id) i else it }))
        EditDialog("Objeto ${item.id}", onDismiss = { editing = null }) {
            OutlinedTextField(item.name, { update(item.copy(name = it)) }, label = { Text("Nombre") }, singleLine = true)
            OutlinedTextField(item.description, { update(item.copy(description = it)) }, label = { Text("Descripción") })
            DropdownField("Efecto", ItemEffect.entries, item.effect, { it.spanish() }, { update(item.copy(effect = it)) })
            if (item.effect == ItemEffect.HEAL_HP) {
                IntField("PV que cura", item.power, { update(item.copy(power = it)) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(item.consumable, { update(item.copy(consumable = it)) })
                Text("  Se consume al usarlo")
            }
            IntField("Precio en tienda (0 = no se vende)", item.price, { update(item.copy(price = it.coerceAtLeast(0))) })
            DropdownField(
                "Ranura de equipo",
                listOf<EquipSlot?>(null) + EquipSlot.entries,
                item.equipSlot,
                { slot -> when (slot) { EquipSlot.WEAPON -> "Arma"; EquipSlot.ARMOR -> "Armadura"; null -> "(no equipable)" } },
                { update(item.copy(equipSlot = it)) },
            )
            if (item.equipSlot != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField("+ATQ", item.attackBonus, { update(item.copy(attackBonus = it)) }, Modifier.weight(1f))
                    IntField("+DEF", item.defenseBonus, { update(item.copy(defenseBonus = it)) }, Modifier.weight(1f))
                    IntField("+PV máx", item.maxHpBonus, { update(item.copy(maxHpBonus = it)) }, Modifier.weight(1f))
                }
            }
        }
    }
}

private fun ItemEffect.spanish(): String = when (this) {
    ItemEffect.NONE -> "Sin efecto (objeto clave)"
    ItemEffect.HEAL_HP -> "Cura PV"
    ItemEffect.KEY -> "Llave"
}

// =============================================================================
// Habilidades
// =============================================================================

@Composable
private fun SkillList(state: EditorState) {
    var editing by remember { mutableStateOf<Int?>(null) }
    val db = state.database

    EntityList(
        entries = db.skills,
        title = { "${it.id}: ${it.name}" },
        subtitle = { "${if (it.kind == SkillKind.MELEE) "Cuerpo a cuerpo" else "Proyectil"} · potencia ${it.power}" },
        onAdd = {
            val id = (db.skills.maxOfOrNull { it.id } ?: 0) + 1
            state.updateDatabase(db.copy(skills = db.skills + Skill(id = id, name = "Habilidad $id")))
        },
        onDelete = { if (db.skills.size > 1) state.updateDatabase(db.copy(skills = db.skills - it)) },
        onEdit = { editing = it.id },
    )

    editing?.let { id ->
        val skill = state.database.skill(id) ?: return
        fun update(s: Skill) = state.updateDatabase(state.database.copy(skills = state.database.skills.map { if (it.id == id) s else it }))
        EditDialog("Habilidad ${skill.id}", onDismiss = { editing = null }) {
            OutlinedTextField(skill.name, { update(skill.copy(name = it)) }, label = { Text("Nombre") }, singleLine = true)
            DropdownField(
                "Tipo",
                SkillKind.entries,
                skill.kind,
                { if (it == SkillKind.MELEE) "Cuerpo a cuerpo" else "Proyectil" },
                { update(skill.copy(kind = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntField("Potencia", skill.power, { update(skill.copy(power = it)) }, Modifier.weight(1f))
                FloatField("Alcance", skill.range, { update(skill.copy(range = it)) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FloatField("Cooldown (s)", skill.cooldownSeconds, { update(skill.copy(cooldownSeconds = it)) }, Modifier.weight(1f))
                FloatField("Retroceso", skill.knockback, { update(skill.copy(knockback = it)) }, Modifier.weight(1f))
            }
            if (skill.kind == SkillKind.PROJECTILE) {
                FloatField("Velocidad proyectil", skill.projectileSpeed, { update(skill.copy(projectileSpeed = it)) })
            }
        }
    }
}

// =============================================================================
// Tileset: paso (colisión) y tiles de pie (2.5D)
// =============================================================================

@Composable
private fun TilesetEditor(state: EditorState) {
    val tileset = state.database.tilesets.firstOrNull()
    if (tileset == null) {
        Text("No hay tileset.", modifier = Modifier.padding(16.dp))
        return
    }
    val bitmap = remember(tileset.image) { loadImageBitmap(state.projectDir, tileset.image) }
    // 0 = editar paso (colisión), 1 = tiles de pie (billboard 2.5D), 2 = animaciones.
    var mode by remember { mutableIntStateOf(0) }
    // Tile cuya animación se está editando en el diálogo, o null.
    var animTile by remember { mutableStateOf<Int?>(null) }

    fun updateTileset(new: Tileset) = state.updateDatabase(
        state.database.copy(
            tilesets = state.database.tilesets.map { if (it.id == tileset.id) new else it },
        ),
    )

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0 },
                label = { Text("Paso") },
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1 },
                label = { Text("De pie (2.5D)") },
            )
            FilterChip(
                selected = mode == 2,
                onClick = { mode = 2 },
                label = { Text("Animación") },
            )
        }
        Text(
            when (mode) {
                0 -> "Toca un tile para alternar si se puede caminar sobre él (verde = paso libre, rojo = bloqueado)."
                1 -> "Toca un tile para alternar si el diorama 2.5D lo dibuja de pie cuando está " +
                    "en la capa 2 (azul = de pie, gris = plano). Ideal para árboles, arbustos, puertas o rocas."
                else -> "Toca un tile para editar su ANIMACIÓN (naranja = animado): fotogramas por los " +
                    "que cicla y a qué velocidad, con vista previa en vivo. Las importadas de SMW " +
                    "(monedas, bloques ?, agua) también se pueden retocar aquí."
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(tileset.columns),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items((0 until tileset.tileCount).toList()) { tile ->
                val passable = tileset.isPassable(tile)
                val standing = tile in tileset.standingTiles
                val animated = tileset.animations.any { it.baseTile == tile }
                TileCell(
                    bitmap = bitmap,
                    tileset = tileset,
                    tile = tile,
                    borderColor = when (mode) {
                        0 -> if (passable) Color(0xFF4CAF50) else Color(0xFFE53935)
                        1 -> if (standing) Color(0xFF42A5F5) else Color(0xFF616161)
                        else -> if (animated) Color(0xFFFF9800) else Color(0xFF616161)
                    },
                    onClick = {
                        when (mode) {
                            0 -> {
                                val newPassable = tileset.passable.toMutableList().also {
                                    while (it.size < tileset.tileCount) it.add(true)
                                    it[tile] = !passable
                                }
                                updateTileset(tileset.copy(passable = newPassable))
                            }
                            1 -> {
                                val newStanding =
                                    if (standing) tileset.standingTiles - tile
                                    else (tileset.standingTiles + tile).sorted()
                                updateTileset(tileset.copy(standingTiles = newStanding))
                            }
                            else -> animTile = tile
                        }
                    },
                )
            }
        }
    }

    animTile?.let { tile ->
        TileAnimationDialog(
            tileset = tileset,
            bitmap = bitmap,
            tile = tile,
            onUpdate = ::updateTileset,
            onDismiss = { animTile = null },
        )
    }
}

/**
 * Editor de la ANIMACIÓN de un tile: vista previa EN VIVO (cicla como en el juego, a
 * 60 fps de referencia), lista de fotogramas en orden (tocar uno lo quita), añadir
 * fotogramas eligiéndolos del tileset, y la velocidad en fotogramas de juego por
 * paso. Guardar sustituye la entrada del tile en `tileset.animations`; "Quitar" la
 * elimina. Es la misma estructura ([TileAnimation]) que hornea la importación de SMW,
 * así que sus animaciones también se pueden retocar aquí.
 */
@Composable
private fun TileAnimationDialog(
    tileset: Tileset,
    bitmap: ImageBitmap?,
    tile: Int,
    onUpdate: (Tileset) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = tileset.animations.firstOrNull { it.baseTile == tile }
    var frames by remember { mutableStateOf(existing?.frames ?: listOf(tile)) }
    var period by remember { mutableIntStateOf(existing?.periodFrames ?: 8) }
    var picking by remember { mutableStateOf(false) }

    EditDialog("Animación del tile $tile", onDismiss = onDismiss) {
        if (picking) {
            Text(
                "Toca el tile a AÑADIR como siguiente fotograma:",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(tileset.columns),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.heightIn(max = 380.dp),
            ) {
                items((0 until tileset.tileCount).toList()) { t ->
                    TileCell(
                        bitmap = bitmap, tileset = tileset, tile = t,
                        borderColor = Color(0xFF616161),
                        onClick = { frames = frames + t; picking = false },
                    )
                }
            }
            TextButton(onClick = { picking = false }) { Text("Cancelar") }
            return@EditDialog
        }

        // Vista previa EN VIVO: cicla los fotogramas a la velocidad elegida.
        var previewIdx by remember { mutableIntStateOf(0) }
        LaunchedEffect(frames, period) {
            previewIdx = 0
            while (true) {
                kotlinx.coroutines.delay((period * 1000L / 60L).coerceAtLeast(16L))
                if (frames.isNotEmpty()) previewIdx = (previewIdx + 1) % frames.size
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TileCell(
                bitmap = bitmap, tileset = tileset,
                tile = frames.getOrElse(previewIdx) { tile },
                borderColor = Color(0xFFFF9800),
                onClick = {},
            )
            Text(
                "  Vista previa · cada $period fot. (${"%.2f".format(period / 60f)} s por fotograma)",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            "Fotogramas en orden (toca uno para QUITARLO):",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            frames.forEachIndexed { i, f ->
                TileCell(
                    bitmap = bitmap, tileset = tileset, tile = f,
                    borderColor = Color(0xFF616161),
                    onClick = { if (frames.size > 1) frames = frames.filterIndexed { j, _ -> j != i } },
                )
            }
        }
        TextButton(onClick = { picking = true }) { Text("＋ Añadir fotograma") }

        Text("Velocidad", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = period.toFloat(),
            onValueChange = { period = it.toInt().coerceIn(2, 30) },
            valueRange = 2f..30f,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val newAnim = TileAnimation(baseTile = tile, frames = frames, periodFrames = period)
                onUpdate(
                    tileset.copy(
                        animations = tileset.animations.filterNot { it.baseTile == tile } + newAnim,
                    ),
                )
                onDismiss()
            }) { Text("Guardar") }
            if (existing != null) {
                TextButton(onClick = {
                    onUpdate(tileset.copy(animations = tileset.animations.filterNot { it.baseTile == tile }))
                    onDismiss()
                }) { Text("Quitar animación") }
            }
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    }
}

@Composable
private fun TileCell(
    bitmap: ImageBitmap?,
    tileset: Tileset,
    tile: Int,
    borderColor: Color,
    onClick: () -> Unit,
) {
    val col = tile % tileset.columns
    val row = tile / tileset.columns
    Canvas(
        modifier = Modifier
            .size(40.dp)
            .border(2.dp, borderColor)
            .clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(col * tileset.tileSize, row * tileset.tileSize),
                srcSize = IntSize(tileset.tileSize, tileset.tileSize),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            )
        }
    }
}
