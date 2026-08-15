package com.rolebuilder.core.engine

import com.rolebuilder.core.model.EquipSlot
import com.rolebuilder.core.model.ItemEffect

/**
 * Orden de la capa de UI al motor.
 *
 * El motor corre en el hilo de render y la UI en el suyo. Hasta ahora
 * `useItem()`, `equip()` y las compras de la tienda escribían `state.items`
 * desde el hilo de UI mientras el motor recorría esa misma colección desde el
 * de render: `@Volatile` arreglaba la visibilidad, no la atomicidad, y el
 * final del camino era una ConcurrentModificationException o un inventario
 * incoherente. Ahora la UI encola una de estas órdenes y **solo el hilo del
 * motor escribe el estado**, al principio de [RpgEngine.tick].
 *
 * El reparto de responsabilidades es a propósito:
 *
 * - **Validar** ([isValid]) es una lectura pura del estado público, sin mutar
 *   nada. Se hace en el hilo que pide la orden para poder contestarle en el
 *   acto y sin mentirle: si el objeto no existe, no queda ninguno o la vida ya
 *   está llena, la orden ni siquiera se encola.
 * - **Aplicar** es cosa del motor, que vuelve a validar antes de ejecutar:
 *   entre encolar y aplicar el mundo pudo cambiar (dos "usar" de la última
 *   poción en el mismo frame, un enemigo que te dejó sin vida). Esa lectura
 *   desde otro hilo es optimista por definición; la que manda es la del motor.
 */
internal sealed interface EngineCommand {

    /** ¿Tiene sentido la orden con el estado actual? No muta nada. */
    fun isValid(engine: RpgEngine): Boolean

    /** Usar un objeto del inventario desde el menú. */
    data class UseItem(val itemId: Int) : EngineCommand {
        override fun isValid(engine: RpgEngine): Boolean {
            val item = engine.data.database.item(itemId) ?: return false
            if (engine.state.itemCount(itemId) <= 0) return false
            return when (item.effect) {
                ItemEffect.HEAL_HP -> engine.state.hp < engine.state.maxHp
                ItemEffect.NONE, ItemEffect.KEY -> false
            }
        }
    }

    /** Equipar en una ranura, o desequiparla con [itemId] nulo. */
    data class Equip(val slot: EquipSlot, val itemId: Int?) : EngineCommand {
        override fun isValid(engine: RpgEngine): Boolean {
            // Desequipar siempre vale: como mucho no hay nada que quitar.
            val id = itemId ?: return true
            val item = engine.data.database.item(id) ?: return false
            return item.equipSlot == slot && engine.state.itemCount(id) > 0
        }
    }

    /** Comprar una unidad en la tienda abierta. */
    data class Buy(val itemId: Int) : EngineCommand {
        override fun isValid(engine: RpgEngine): Boolean {
            val item = engine.data.database.item(itemId) ?: return false
            return engine.state.gold >= item.price
        }
    }

    /** Vender una unidad a mitad de precio. */
    data class Sell(val itemId: Int) : EngineCommand {
        override fun isValid(engine: RpgEngine): Boolean {
            val item = engine.data.database.item(itemId) ?: return false
            return item.price > 0 && engine.state.itemCount(itemId) > 0
        }
    }

    /** Cerrar la tienda; el intérprete que la abrió continúa. */
    object CloseShop : EngineCommand {
        override fun isValid(engine: RpgEngine): Boolean = engine.shop != null
    }

    /** Cerrar la caja de mensaje actual. */
    object DismissMessage : EngineCommand {
        override fun isValid(engine: RpgEngine): Boolean = engine.message != null
    }

    /** Elegir una opción del menú de elecciones. */
    data class SelectChoice(val index: Int) : EngineCommand {
        override fun isValid(engine: RpgEngine): Boolean = engine.choices != null
    }
}
