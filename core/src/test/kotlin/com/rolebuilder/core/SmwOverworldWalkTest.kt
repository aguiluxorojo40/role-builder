package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwOverworldWalk
import com.rolebuilder.core.snes.SnesDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CAMINOS del overworld ([SmwOverworldWalk]). Gated a la ROM local.
 *
 * Lo que se comprueba aquí no es que el código "haga algo", sino que reproduce el principio
 * REAL de Super Mario World: partida nueva en la casa de Yoshi y, andando, YOSHI'S ISLAND 1 a
 * la izquierda y YOSHI'S ISLAND 2 a la derecha. Si el port se rompe, esto lo canta.
 */
class SmwOverworldWalkTest {

    /** Nº de nivel de las casillas de Yoshi's Island (ver KDoc de SmwOverworldLevels). */
    private val YOSHIS_HOUSE = 40
    private val YOSHIS_ISLAND_1 = 41
    private val YOSHIS_ISLAND_2 = 42

    @Test
    fun aNewGameStartsAtYoshisHouseWithTwoWaysOut() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0

        val start = SmwOverworldWalk.newGameStart(rom, delta)
        assertEquals(1, start.submap, "SMW empieza en el submapa de Yoshi's Island")
        assertEquals(6, start.x, "columna de salida")
        assertEquals(7, start.y, "fila de salida")

        val tiles = SmwOverworldWalk.layer1(rom, delta)
        val levels = SmwOverworldWalk.levelNumbers(tiles)
        val here = levels[SmwOverworldWalk.position(start.submap, start.x, start.y)]
        assertEquals(YOSHIS_HOUSE, here, "la casilla de salida es la casa de Yoshi")

        val settings = SmwOverworldWalk.newGameSettings(rom, delta)
        assertEquals(0x03, settings[YOSHIS_HOUSE], "izquierda y derecha abiertas, nada más")
        assertTrue(SmwOverworldWalk.canLeave(settings, here, SmwOverworldWalk.DIR_LEFT))
        assertTrue(SmwOverworldWalk.canLeave(settings, here, SmwOverworldWalk.DIR_RIGHT))
        assertFalse(SmwOverworldWalk.canLeave(settings, here, SmwOverworldWalk.DIR_UP))
        assertFalse(SmwOverworldWalk.canLeave(settings, here, SmwOverworldWalk.DIR_DOWN))
    }

    @Test
    fun walkingFromYoshisHouseReachesTheFirstTwoLevels() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val tiles = SmwOverworldWalk.layer1(rom, delta)
        val levels = SmwOverworldWalk.levelNumbers(tiles)
        val start = SmwOverworldWalk.newGameStart(rom, delta)

        val left = SmwOverworldWalk.walk(
            tiles, levels, start.submap, start.x, start.y, SmwOverworldWalk.DIR_LEFT,
        )
        assertNotNull(left)
        assertEquals(YOSHIS_ISLAND_1, left.endLevel, "a la izquierda está YOSHI'S ISLAND 1")
        assertEquals(3, left.endX); assertEquals(8, left.endY)
        assertEquals(4, left.steps.size, "son cuatro casillas de camino")

        val right = SmwOverworldWalk.walk(
            tiles, levels, start.submap, start.x, start.y, SmwOverworldWalk.DIR_RIGHT,
        )
        assertNotNull(right)
        assertEquals(YOSHIS_ISLAND_2, right.endLevel, "a la derecha está YOSHI'S ISLAND 2")
        assertEquals(9, right.endX); assertEquals(8, right.endY)
        assertEquals(4, right.steps.size)

        // Todas las casillas del recorrido menos la última son CAMINO, no parada. Si esto
        // fallara, Mario se estaría plantando a mitad de camino.
        for (s in left.steps.dropLast(1)) {
            assertTrue(s.tile <= SmwOverworldWalk.PATH_TILE_MAX, "0x%02X es camino".format(s.tile))
        }
        assertTrue(SmwOverworldWalk.isStop(left.steps.last().tile), "la última es parada")
    }

    @Test
    fun theWalkTurnsCornersInsteadOfGoingStraight() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val tiles = SmwOverworldWalk.layer1(rom, delta)
        val start = SmwOverworldWalk.newGameStart(rom, delta)

        // El camino a YOSHI'S ISLAND 1 no es una línea recta: sale a la izquierda y acaba
        // una fila más abajo. Que la Y cambie demuestra que el giro está portado.
        val levels = SmwOverworldWalk.levelNumbers(tiles)
        val left = SmwOverworldWalk.walk(
            tiles, levels, start.submap, start.x, start.y, SmwOverworldWalk.DIR_LEFT,
        )
        assertNotNull(left)
        assertTrue(left.endY != start.y, "el camino gira: acaba en otra fila")
        assertTrue(left.endX < start.x, "y sí, hacia la izquierda")
    }

    @Test
    fun theFirstStepDoesNotTurn() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val tiles = SmwOverworldWalk.layer1(rom, delta)
        val levels = SmwOverworldWalk.levelNumbers(tiles)
        val start = SmwOverworldWalk.newGameStart(rom, delta)

        // Encima de la casa de Yoshi la tesela no es pisable, así que hacia arriba NO se
        // mueve — aunque desde ahí sí haya caminos a los lados. Es la diferencia entre
        // comprobar el destino antes de arrancar (lo que hace el juego) y dejar que el
        // buscador de esquinas se lleve a Mario por donde no era.
        assertFalse(
            SmwOverworldWalk.isWalkable(SmwOverworldWalk.tileAt(tiles, start.submap, start.x, start.y - 1)),
            "encima de la casa de Yoshi no hay camino",
        )
        assertNull(
            SmwOverworldWalk.walk(tiles, levels, start.submap, start.x, start.y, SmwOverworldWalk.DIR_UP),
            "hacia arriba no se anda",
        )
    }

    @Test
    fun beatingALevelUnlocksARealDirection() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0

        // YOSHI'S ISLAND 2 abre hacia ARRIBA por su salida normal, y arriba está YI 3.
        val bit = SmwOverworldWalk.unlockedDirection(rom, delta, YOSHIS_ISLAND_2, exitAction = 1)
        assertEquals(SmwOverworldWalk.dirBit(SmwOverworldWalk.DIR_UP), bit, "YI2 abre hacia arriba")

        val tiles = SmwOverworldWalk.layer1(rom, delta)
        val levels = SmwOverworldWalk.levelNumbers(tiles)
        val settings = SmwOverworldWalk.newGameSettings(rom, delta)
        settings[YOSHIS_ISLAND_2] = settings[YOSHIS_ISLAND_2] or bit

        val pos = levels.indexOf(YOSHIS_ISLAND_2)
        assertTrue(pos > 0, "YI2 está en el mapa")
        val x = pos % 0x100 % 16 + ((pos % 0x400) / 0x100 % 2) * 16
        val y = pos % 0x100 / 16 + ((pos % 0x400) / 0x100 / 2) * 16
        assertTrue(
            SmwOverworldWalk.canLeave(settings, YOSHIS_ISLAND_2, SmwOverworldWalk.DIR_UP),
            "tras superarlo se puede subir",
        )
        val up = SmwOverworldWalk.walk(tiles, levels, 1, x, y, SmwOverworldWalk.DIR_UP)
        assertNotNull(up)
        assertEquals(39, up.endLevel, "arriba de YOSHI'S ISLAND 2 está YOSHI'S ISLAND 3")
    }

    @Test
    fun eventsOpenPathsInTheWalkableLayer() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0

        val plain = SmwOverworldWalk.layer1(rom, delta)
        val withAll = SmwOverworldWalk.layer1WithEvents(rom, delta) { true }
        assertFalse(plain.contentEquals(withAll), "los eventos cambian la capa por la que se anda")

        // Con NINGÚN evento disparado la capa tiene que quedarse igual que la de la ROM: si
        // no, estaríamos abriendo caminos que el jugador no se ha ganado.
        val none = SmwOverworldWalk.layer1WithEvents(rom, delta) { false }
        assertTrue(plain.contentEquals(none), "sin eventos, la capa es la de la ROM tal cual")

        // La numeración de niveles va SIEMPRE sobre la capa sin eventos, porque el juego
        // numera antes de aplicarlos. Aquí se ve por qué importa: con todos los eventos
        // disparados un evento convierte la casilla 0x275 (0x5D) en 0x81 y ya no cuenta como
        // nivel, así que numerar sobre esa capa daría 91 y correría un puesto todos los
        // nombres de ahí en adelante.
        assertEquals(92, SmwOverworldWalk.levelNumbers(plain).max(), "la ROM tiene 92 niveles")
        assertEquals(91, SmwOverworldWalk.levelNumbers(withAll).max(), "por eso NO se numera aquí")
        assertEquals(92, SmwOverworldWalk.levelNumbers(rom, delta).max(), "la buena usa la capa base")
    }

    @Test
    fun arrivingOpensTheWayBack() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val tiles = SmwOverworldWalk.layer1(rom, delta)
        val levels = SmwOverworldWalk.levelNumbers(tiles)
        val settings = SmwOverworldWalk.newGameSettings(rom, delta)
        val start = SmwOverworldWalk.newGameStart(rom, delta)

        val left = SmwOverworldWalk.walk(tiles, levels, start.submap, start.x, start.y, SmwOverworldWalk.DIR_LEFT)
        assertNotNull(left)
        assertEquals(0, settings[left.endLevel], "YI1 empieza sin ninguna salida abierta")

        // Al llegar, el juego abre la dirección CONTRARIA a la que traías.
        settings[left.endLevel] = settings[left.endLevel] or
            SmwOverworldWalk.dirBit(SmwOverworldWalk.opposite(left.endDirection))
        val back = SmwOverworldWalk.walk(
            tiles, levels, start.submap, left.endX, left.endY,
            SmwOverworldWalk.opposite(left.endDirection),
        )
        assertNotNull(back)
        assertEquals(40, back.endLevel, "se vuelve a la casa de Yoshi")
    }

    @Test
    fun pathExitsConnectTheMainMapToTheSubmaps() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val exits = SmwOverworldWalk.pathExits(rom, delta)
        assertEquals(SmwOverworldWalk.EXIT_COUNT, exits.size, "hay 14 salidas de camino")

        // La primera es la tubería de Yoshi's Island: del mapa principal (2,20) al submapa 1.
        val yi = SmwOverworldWalk.exitAt(exits, submap = 0, x = 2, y = 20)
        assertNotNull(yi)
        assertEquals(1, yi.dstSubmap, "lleva al submapa 1 (Yoshi's Island)")
        assertEquals(4, yi.dstX); assertEquals(0, yi.dstY)

        // Y hay vuelta: desde el submapa se puede volver al mapa principal. Si no, habría
        // mundos sin salida y el juego no se podría recorrer entero.
        val backToMain = exits.filter { it.srcSubmap != 0 && it.dstSubmap == 0 }
        assertTrue(backToMain.isNotEmpty(), "hay salidas que devuelven al mapa principal")

        // Cada submapa alcanzable desde el mapa principal tiene su vuelta.
        val reachable = exits.filter { it.srcSubmap == 0 }.map { it.dstSubmap }.toSet()
        val returning = exits.filter { it.dstSubmap == 0 }.map { it.srcSubmap }.toSet()
        assertTrue(returning.containsAll(reachable), "de todo submapa alcanzable se puede volver")
    }

    @Test
    fun walkingOntoAPipeTriggersTheExit() {
        val rom = findRom() ?: return
        val delta = SnesDecoder.parseHeader(rom).headerOffset - 0x7FC0
        val tiles = SmwOverworldWalk.layer1(rom, delta)
        val levels = SmwOverworldWalk.levelNumbers(tiles)
        val exits = SmwOverworldWalk.pathExits(rom, delta)

        // La tubería de ida a Yoshi's Island está en el mapa principal (2,20). Sin marcarla
        // como parada, el recorrido pasaría de largo; con ella, Mario se planta en la boca.
        val stops = SmwOverworldWalk.exitStops(exits, submap = 0)
        assertTrue(stops.contains(SmwOverworldWalk.position(0, 2, 20)), "la boca de la tubería para")

        // Un recorrido que llegue a (2,20) debe pararse ahí y ofrecer su salida.
        val exit = SmwOverworldWalk.exitAt(exits, 0, 2, 20)
        assertNotNull(exit)
        assertEquals(1, exit.dstSubmap)
        // La casilla de destino es pisable: se llega a un sitio real, no al vacío.
        assertTrue(
            SmwOverworldWalk.isWalkable(SmwOverworldWalk.tileAt(tiles, exit.dstSubmap, exit.dstX, exit.dstY)),
            "se llega a una casilla real del submapa",
        )
        assertTrue(levels.isNotEmpty())
    }

    @Test
    fun positionMatchesTheGamesFormula() {
        // CalculateOverworldPlayerPosition: pantallas en 2×2 y +0x400 en el área de submapas.
        assertEquals(0, SmwOverworldWalk.position(0, 0, 0))
        assertEquals(0x0F, SmwOverworldWalk.position(0, 15, 0))
        assertEquals(0x100, SmwOverworldWalk.position(0, 16, 0), "columna 16 = pantalla de al lado")
        assertEquals(0x200, SmwOverworldWalk.position(0, 0, 16), "fila 16 = pantalla de abajo")
        assertEquals(0x300, SmwOverworldWalk.position(0, 16, 16))
        assertEquals(0x400, SmwOverworldWalk.position(1, 0, 0), "el área de submapas suma 0x400")
        assertEquals(0x76, SmwOverworldWalk.position(0, 6, 7))
    }

    @Test
    fun directionBitsMatchTheJoypadLayout() {
        // Los bits son los del mando: bit0 derecha, bit1 izquierda, bit2 abajo, bit3 arriba.
        assertEquals(1, SmwOverworldWalk.dirBit(SmwOverworldWalk.DIR_RIGHT))
        assertEquals(2, SmwOverworldWalk.dirBit(SmwOverworldWalk.DIR_LEFT))
        assertEquals(4, SmwOverworldWalk.dirBit(SmwOverworldWalk.DIR_DOWN))
        assertEquals(8, SmwOverworldWalk.dirBit(SmwOverworldWalk.DIR_UP))
        assertEquals(SmwOverworldWalk.DIR_DOWN, SmwOverworldWalk.opposite(SmwOverworldWalk.DIR_UP))
        assertEquals(SmwOverworldWalk.DIR_RIGHT, SmwOverworldWalk.opposite(SmwOverworldWalk.DIR_LEFT))
    }

    private fun findRom(): ByteArray? {
        val candidates = listOf(
            System.getenv("SMW_ROM"),
            "/tmp/claude-0/-home-user-role-builder/97c51e21-49f4-59ff-af37-f321a2c64985/" +
                "scratchpad/smw_work/rom/Super Mario World (USA).sfc",
        )
        for (p in candidates) if (p != null && File(p).isFile) return File(p).readBytes()
        return null
    }
}
