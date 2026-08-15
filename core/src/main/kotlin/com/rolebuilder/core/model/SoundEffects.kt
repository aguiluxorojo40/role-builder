package com.rolebuilder.core.model

/**
 * Los efectos de sonido que `:core` puede pedir, en un solo sitio.
 *
 * Esto existe por un fallo concreto, y de los caros de encontrar (AUDITORIA §2.1):
 * el motor encolaba `"coin"` y `"levelup"` como texto suelto, la tabla de la app no
 * los tenía registrados y esos efectos **se quedaban mudos en silencio** — sin error,
 * sin log, sin nada. Los tests tampoco lo veían: los de `:core` comprobaban su propia
 * cola y los de `:app` su propia tabla, pero nadie comprobaba que las dos coincidieran.
 *
 * Con los nombres aquí, el motor y el proyecto por defecto dejan de escribir literales
 * y la app construye su tabla a partir de [ALL], así que ya no hay dos listas que
 * puedan separarse: hay una. Añadir un efecto nuevo es añadirlo aquí, y entonces
 * `AudioContractTest` de `:app` exige que también se sepa sintetizar.
 *
 * Ojo con el alcance: `EventCommand.PlaySound` acepta **texto libre**, así que un
 * proyecto editado a mano puede pedir un sonido que no esté en esta lista. Eso es
 * legítimo y la app lo resuelve con un pitido genérico audible. [ALL] es lo que
 * `:core` promete pedir, no todo lo que puede llegar a sonar.
 */
object SoundEffects {

    const val ATTACK = "attack"
    const val HIT = "hit"
    const val HURT = "hurt"
    const val DEFEAT = "defeat"
    const val PICKUP = "pickup"
    const val SELECT = "select"
    const val CHEST = "chest"
    const val HEAL = "heal"
    const val SHOOT = "shoot"
    const val COIN = "coin"
    const val LEVELUP = "levelup"

    /**
     * Todo lo que `:core` puede pedir: lo que encola el motor en combate y menús,
     * más lo que dispara el proyecto por defecto con `PlaySound` (el cofre).
     */
    val ALL: List<String> = listOf(
        ATTACK, HIT, HURT, DEFEAT, PICKUP,
        SELECT, CHEST, HEAL, SHOOT, COIN, LEVELUP,
    )
}
