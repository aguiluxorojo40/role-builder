package com.rolebuilder.core

import com.rolebuilder.core.snes.SnesDecoder
import com.rolebuilder.core.snes.SnesGameRecipes
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * El parseo del fondo devolvía `emptyList()` para TODO, mezclando "este nivel no tiene Layer 2"
 * con "el parser ha fallado". Eso destruía la capacidad de diagnosticar: no se podía saber
 * cuántos niveles fallan ni por qué, así que cualquier arreglo de los offsets [PROBABLE] habría
 * sido especulativo. Estos tests fijan que los tres casos ya se distinguen.
 */
class BgParseDiagnosticTest {

    private fun header() = SnesDecoder.parseHeader(ByteArray(0x10000))

    @Test
    fun `la auditoria no revienta con datos que no son SMW`() {
        val rows = SnesGameRecipes.auditLayer2(ByteArray(0x80000), header(), 0x000..0x00F)
        // Cabecera + una fila por slot pedido.
        assertTrue(rows.size == 17, "debe informar de todos los slots: ${rows.size}")
        assertTrue(rows[0].startsWith("nivel"), "primera fila = cabecera de columnas")
    }

    @Test
    fun `cada fila dice el estado, no solo un vacio mudo`() {
        val rows = SnesGameRecipes.auditLayer2(ByteArray(0x80000), header(), 0x000..0x005)
        val datos = rows.drop(1)
        assertTrue(datos.isNotEmpty())
        // Ninguna fila puede quedarse sin veredicto: OK, sin fondo o ERROR.
        assertTrue(
            datos.all { it.contains("OK") || it.contains("sin fondo") || it.contains("ERROR") },
            "toda fila debe llevar estado explícito: $datos",
        )
    }

    @Test
    fun `el resumen cuenta los tres casos y los valores de isBg`() {
        val s = SnesGameRecipes.auditLayer2Summary(ByteArray(0x80000), header())
        assertTrue(s.contains("OK="), "debe contar los OK: $s")
        assertTrue(s.contains("sin fondo="), "debe contar los sin fondo: $s")
        assertTrue(s.contains("ERROR="), "debe contar los errores: $s")
        assertTrue(s.contains("isBg"), "debe volcar los valores de isBg vistos: $s")
    }
}
