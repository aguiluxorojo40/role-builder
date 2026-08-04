package com.rolebuilder.core.tools

import com.rolebuilder.core.engine.platformer.EnemySeed
import com.rolebuilder.core.engine.platformer.PlatformerEngine
import com.rolebuilder.core.engine.platformer.PlatformerTuning
import com.rolebuilder.core.snes.SmwSolidity
import java.io.File

/**
 * Traza el movimiento de un enemigo en NUESTRO motor, en el mismo formato CSV que
 * `scripts/enemy_trace.py` saca del emulador, para poder compararlos con
 * `scripts/enemy_compare.py`.
 *
 * Es la mitad "nuestra" del arnés de comparación: la del emulador da la verdad del juego,
 * esta da lo que hacemos nosotros, y la diferencia sale como número en vez de como opinión.
 *
 * Uso:
 *   ./gradlew :core:traceEnemy --args="--id 0x37 --frames 600 --out ours_boo.csv"
 *
 * Opciones:
 *   --id      Id de sprite SMW del enemigo (0x37 Boo, 0x38/0x39 Eerie, 0x15 Cheep…).
 *   --frames  Cuántos frames simular (600 = 10 s a 60 fps).
 *   --out     CSV de salida.
 *   --floor   Fila de suelo sólido (por defecto sin suelo, para los flotantes).
 *   --ex/--ey Casilla donde nace el enemigo.  --px/--py  Casilla donde nace Mario.
 */
object EnemyTrace {

    @JvmStatic
    fun main(args: Array<String>) {
        val o = parse(args)
        val id = (o["id"] ?: "0x37").let { if (it.startsWith("0x", true)) it.drop(2).toInt(16) else it.toInt() }
        val frames = o["frames"]?.toIntOrNull() ?: 600
        val out = o["out"] ?: "ours_enemy.csv"
        val floor = o["floor"]?.toIntOrNull()
        val ex = o["ex"]?.toIntOrNull() ?: 14
        val ey = o["ey"]?.toIntOrNull() ?: 6
        val px = o["px"]?.toIntOrNull() ?: 5
        val py = o["py"]?.toIntOrNull() ?: 6

        val cols = 64
        val rows = 20
        val engine = PlatformerEngine(
            cols = cols, rows = rows,
            // Sin suelo por defecto: así se ve si el enemigo flota o se cae.
            solidityAt = { _, r -> if (floor != null && r >= floor) SmwSolidity.SOLID else SmwSolidity.NONE },
            startPixelX = px * 16, startPixelY = py * 16,
            tuning = PlatformerTuning(
                jumpSpeed = -5f, gravityFall = 0.375f, gravityHold = 0.1875f, maxFallSpeed = 4f,
                maxWalkSpeed = 1.5f, maxRunSpeed = 3f, runAccel = 0.2f, friction = 0.3f,
            ),
            enemySeeds = listOf(EnemySeed(ex * 16, ey * 16, id)),
        )

        val sb = StringBuilder("frame,slot,id,x,y,vx,vy,status\n")
        val e = engine.enemies.firstOrNull() ?: run {
            System.err.println("El motor no creó ningún enemigo para el id 0x${id.toString(16)}.")
            return
        }
        var prevX = e.x
        var prevY = e.y
        for (f in 0 until frames) {
            engine.tick()
            if (!e.alive) break
            // vx/vy REALES observadas (diferencia de posición): comparable con las del
            // emulador sin depender de cómo guarde cada uno su velocidad interna.
            val vx = e.x - prevX
            val vy = e.y - prevY
            prevX = e.x; prevY = e.y
            sb.append("$f,0,$id,${e.x},${e.y},$vx,$vy,1\n")
        }
        File(out).writeText(sb.toString())
        println("Traza de NUESTRO motor: id 0x${id.toString(16)} · $frames frames → $out")
        println("Compárala con la del emulador:  python3 scripts/enemy_compare.py --real real.csv --ours $out --id 0x${id.toString(16)}")
    }

    private fun parse(args: Array<String>): Map<String, String> {
        val m = HashMap<String, String>()
        var i = 0
        while (i < args.size) {
            val a = args[i]
            if (a.startsWith("--")) {
                val k = a.removePrefix("--")
                val v = args.getOrNull(i + 1)
                if (v != null && !v.startsWith("--")) { m[k] = v; i++ } else m[k] = "true"
            }
            i++
        }
        return m
    }
}
