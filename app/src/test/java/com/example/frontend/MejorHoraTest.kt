package com.example.frontend

import com.example.frontend.model.ReglasMejorHora
import com.example.frontend.model.ReglasMejorHora.Fila
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// BQ8: las reglas que la app corre sobre las filas de Room (con y sin red).
class MejorHoraTest {

    private fun nitro(hora: Int, promedio: Double, reportes: Int) = Fila("nitro-coffee", "Nitro", "☕", hora, promedio, reportes)

    @Test
    fun `la mejor y la peor hora ignoran las horas con pocos reportes`() {
        val filas = listOf(nitro(8, 3.0, 1), nitro(10, 6.0, 4), nitro(12, 25.0, 5), nitro(15, 8.0, 3))
        val sitio = ReglasMejorHora.armar(filas, horaActual = 7).sitios.single()
        assertEquals(10, sitio.mejor?.hora)       // las 8 am tienen un solo reporte
        assertEquals(12, sitio.peor?.hora)
        assertEquals(13, sitio.reportes)
    }

    @Test
    fun `la proxima hora tranquila sale desde la hora actual`() {
        val filas = listOf(nitro(9, 5.0, 3), nitro(12, 25.0, 3), nitro(15, 9.0, 2), nitro(17, 20.0, 2))
        val sitio = ReglasMejorHora.armar(filas, horaActual = 13).sitios.single()
        assertEquals(9, sitio.mejor?.hora)
        assertEquals(15, sitio.proximaTranquila?.hora) // 9 min: a menos de 5 min de la mejor
        assertNull(ReglasMejorHora.armar(filas, horaActual = 18).sitios.single().proximaTranquila)
    }

    @Test
    fun `el campus va aparte y los restaurantes se ordenan por su mejor espera`() {
        val filas = listOf(
            Fila(ReglasMejorHora.CAMPUS, "Campus", "🏫", 10, 7.0, 9),
            Fila("burger", "Burger", "🍔", 11, 12.0, 4),
            nitro(10, 4.0, 5),
        )
        val resumen = ReglasMejorHora.armar(filas, horaActual = 8)
        assertEquals(10, resumen.campus?.mejor?.hora)
        assertEquals(listOf("nitro-coffee", "burger"), resumen.sitios.map { it.spotId })
        assertEquals(9, resumen.totalReportes)
    }

    @Test
    fun `texto de la hora en formato de 12 horas`() {
        assertEquals("12 pm", ReglasMejorHora.textoHora(12))
        assertEquals("8 am", ReglasMejorHora.textoHora(8))
        assertEquals("12 am", ReglasMejorHora.textoHora(0))
    }
}
