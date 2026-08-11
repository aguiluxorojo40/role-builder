package com.rolebuilder.core

import com.rolebuilder.core.snes.SmwBlockAction
import com.rolebuilder.core.snes.SmwBlockBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests del clasificador de comportamiento de bloques (moneda). */
class SmwBlockBehaviorTest {

    @Test
    fun `la moneda es el byte bajo 0x2B del plano de block-code`() {
        assertEquals(SmwBlockAction.COIN, SmwBlockBehavior.classify(0x2B))
        assertEquals(SmwBlockAction.COIN, SmwBlockBehavior.classify(0x02B)) // alto 0
    }

    @Test
    fun `los bloques golpeables 0x21-0x24 son bloques interrogante`() {
        for (lo in 0x21..0x24) {
            assertEquals(SmwBlockAction.QUESTION, SmwBlockBehavior.classify(lo), "0x${lo.toString(16)}")
        }
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x20))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x25))
    }

    @Test
    fun `la dragon coin son los bytes bajos 0x2D-0x2E apilados`() {
        assertEquals(SmwBlockAction.DRAGON_COIN, SmwBlockBehavior.classify(0x2D)) // mitad abajo
        assertEquals(SmwBlockAction.DRAGON_COIN, SmwBlockBehavior.classify(0x2E)) // mitad arriba
        // El 0x2C NO es "nada": es la tercera MONEDA suelta. `RunPlayerBlockCode_00F309`
        // ($00:F309) entra con 0x2A <= j < 0x2F y manda a `GiveCoins_OneCoin()` todo lo que
        // sea j < 0x2D, o sea 0x2A, 0x2B y 0x2C.
        assertEquals(SmwBlockAction.COIN, SmwBlockBehavior.classify(0x2C))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x2F))
        // El mismo byte bajo en el plano de TERRENO no es una moneda dragon: ahi 0x2D cae en
        // el rango de bloques GOLPEABLES (0x11..0x2D, $00:F160).
        assertEquals(SmwBlockAction.QUESTION, SmwBlockBehavior.classify(0x12D))
    }

    @Test
    fun `en el plano de TERRENO lo interactivo son los bloques GOLPEABLES`() {
        // Esto ANTES daba NONE para todo el plano alto, y era el fallo: los bloques `?` de
        // SMW viven aqui, no en el de block code. `RunPlayerBlockCode` exige el byte ALTO
        // distinto de cero (`if (v2)`) y el bajo en 0x11..0x6D antes de llamar a
        // `CheckIfBlockWasHit` ($00:F160), que se queda con 0x11..0x2D (`a -= 17; if (a >= 0x1D)`).
        // Con el criterio viejo no se reconocia NI UNO: 7 en YOSHI'S ISLAND 1, 17 en el 2 y
        // 9 en el 3, incluido el que suelta el HUEVO DE YOSHI.
        assertEquals(SmwBlockAction.QUESTION, SmwBlockBehavior.classify(0x111)) // primero del rango
        assertEquals(SmwBlockAction.QUESTION, SmwBlockBehavior.classify(0x124)) // el `?` de YI2/YI3
        assertEquals(SmwBlockAction.QUESTION, SmwBlockBehavior.classify(0x12D)) // ultimo del rango
        // Y el rango es EXACTO: fuera de el, el terreno es terreno.
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x110))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x12E))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x100)) // terreno solido
        // Lo que se RECOGE sigue siendo solo del plano de block code: una moneda del plano
        // de terreno no existe, ahi ese byte es un bloque golpeable.
        assertEquals(SmwBlockAction.QUESTION, SmwBlockBehavior.classify(0x12B))
    }

    @Test
    fun `la luna 3-UP se recoge, y hasta ahora era decorado`() {
        // `RunPlayerBlockCode_00F309` ($00:F309): `if (j != 110) return`, y lo que hace es
        // subir `unusedram_3up_moons_counter` y marcar `flag_collected_moons` del nivel.
        // Hay una en YOSHI'S ISLAND 1, en (198,4) sobre la fila de nubes: se veia y no se
        // podia coger.
        assertEquals(SmwBlockAction.MOON_3UP, SmwBlockBehavior.classify(0x6E))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x6D))
        // El 0x6F NO es "nada", y esta linea llego a decir que si: es el PRIMER punto de
        // control del 1-UP escondido ($00:F28C empieza justo ahi, `v1 = j - 111`). La luna
        // y los cuatro puntos son vecinos en la tabla, y ese es todo el limite que hay.
        assertEquals(SmwBlockAction.CHECKPOINT_1UP_1, SmwBlockBehavior.classify(0x6F))
    }

    @Test
    fun `la cinta del punto intermedio es el 0x38 del plano de BLOCK CODE`() {
        // Los dos extremos de la ROM dicen el mismo plano:
        //  - la dibuja `ExtObj46_MidwayBar` ($0D:A68E) con
        //    `SetMap16HighByteForCurrentObject_Page00(...)` + `SetMap16LowByte(v2, 0x38)`;
        //  - la reconoce `RunPlayerBlockCode_00F2C9` ($00:F2C9, `if (j == 56)`), a la que
        //    solo se llega desde las ramas con el byte ALTO a 0 (via $00:F2C2).
        assertEquals(SmwBlockAction.MIDWAY_TAPE, SmwBlockBehavior.classify(0x38))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x37))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x39))
    }

    @Test
    fun `el 0x38 del plano de TERRENO es una boca de tuberia, no la cinta`() {
        // Confundir los planos es exactamente el fallo que hubo con las tuberias: con el
        // byte alto != 0, el 0x37/0x38 son las dos mitades de una boca VERTICAL
        // (`RunPlayerBlockCode_00F3E9`, $00:F3E9) y las lleva SmwWarpTiles, no este
        // clasificador. Si esto diera MIDWAY_TAPE, cada tuberia del juego seria un punto
        // intermedio: hay 2 casillas asi en YOSHI'S ISLAND 1, 2 en el 2 y 2 en el 3.
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x138))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x137))
    }

    @Test
    fun `fuera de rango y aire no son interactivos`() {
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x25)) // aire
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(-1))
        assertEquals(SmwBlockAction.NONE, SmwBlockBehavior.classify(0x200))
    }

    @Test
    fun `los ordinales NO se pueden reordenar`() {
        // Se guardan en el tileset del proyecto (`platformBlockActions`), asi que cambiarlos
        // reinterpretaria en silencio los bloques de todos los mapas ya importados. Los
        // valores nuevos van al final.
        assertEquals(0, SmwBlockAction.NONE.ordinal)
        assertEquals(1, SmwBlockAction.COIN.ordinal)
        assertEquals(2, SmwBlockAction.QUESTION.ordinal)
        assertEquals(3, SmwBlockAction.DRAGON_COIN.ordinal)
        assertEquals(4, SmwBlockAction.MOON_3UP.ordinal)
        assertEquals(5, SmwBlockAction.MIDWAY_TAPE.ordinal)
    }
}
