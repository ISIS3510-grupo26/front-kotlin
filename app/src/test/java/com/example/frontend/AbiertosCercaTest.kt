package com.example.frontend

import com.example.frontend.data.aplicarPendientes
import com.example.frontend.data.local.FavoritoPendienteEntidad
import com.example.frontend.model.AbiertoCerca
import com.example.frontend.model.ReglasAbiertosCerca
import com.example.frontend.model.textoCierraEn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbiertosCercaTest {

    // 2026-11-19 15:00 en Bogota (UTC-5) = 20:00 UTC.
    private val ahora = 1_795_118_400_000L
    private val bogota = -300

    private fun abierto(id: String, lat: Double, lng: Double, cierraEn: Long) =
        AbiertoCerca(id, id, "🍔", lat, lng, metros = 0, minutosCaminando = 1, cierraA = "20:00", cierraEnMillis = cierraEn)

    @Test
    fun elCierreDeHoyQuedaHoyYElQueCruzaMedianocheQuedaManana() {
        val hora = 60 * 60_000L
        assertEquals(ahora + 5 * hora, ReglasAbiertosCerca.cierreEnMillis("20:00", ahora, bogota))
        // 02:00 ya paso hoy: es la franja que cruza la medianoche, cierra manana.
        assertEquals(ahora + 11 * hora, ReglasAbiertosCerca.cierreEnMillis("02:00", ahora, bogota))
    }

    @Test
    fun laDistanciaUsaLaMismaVelocidadQueElBackend() {
        // ~111 m por milesima de grado de latitud.
        assertEquals(800.0, ReglasAbiertosCerca.metrosEntre(4.6000, -74.0658, 4.6072, -74.0658), 5.0)
        // 80 m/min y redondeo hacia arriba, como hace la app Flutter con walkMinutes.
        assertEquals(10, ReglasAbiertosCerca.minutosCaminando(800.0))
        assertEquals(11, ReglasAbiertosCerca.minutosCaminando(801.0))
        assertEquals(1, ReglasAbiertosCerca.minutosCaminando(5.0))
    }

    @Test
    fun sinRedSoloQuedanLosFavoritosQueSiguenAbiertosYCerca() {
        val guardados = listOf(
            abierto("cerca-abierto", 4.6018, -74.0658, cierraEn = ahora + 60_000),
            abierto("ya-cerro", 4.6018, -74.0658, cierraEn = ahora - 60_000),
            abierto("ya-no-es-favorito", 4.6018, -74.0658, cierraEn = ahora + 60_000),
            abierto("lejos", 4.6200, -74.0658, cierraEn = ahora + 60_000),   // ~2 km
        )
        val favoritos = setOf("cerca-abierto", "ya-cerro", "lejos")
        val lista = ReglasAbiertosCerca.filtrarSinRed(guardados, favoritos, 4.6020, -74.0660, ahora)
        assertEquals(listOf("cerca-abierto"), lista.map { it.spotId })
        assertTrue(lista.single().metros < 50)
    }

    @Test
    fun loQueSeHizoSinRedGanaSobreElServidor() {
        val pendientes = listOf(
            FavoritoPendienteEntidad("u", "nuevo", guardar = true, eventId = "e1", cambiadoEn = 1),
            FavoritoPendienteEntidad("u", "quitado", guardar = false, eventId = "e2", cambiadoEn = 2),
        )
        assertEquals(setOf("viejo", "nuevo"), aplicarPendientes(setOf("viejo", "quitado"), pendientes))
    }

    @Test
    fun textoDelCierre() {
        assertEquals("closes in 25 min", textoCierraEn(ahora + 25 * 60_000, ahora))
        assertEquals("closes in 2 h", textoCierraEn(ahora + 120 * 60_000, ahora))
        assertEquals("closes in 1 h 10 min", textoCierraEn(ahora + 70 * 60_000, ahora))
    }
}
