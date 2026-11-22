package com.example.frontend

import androidx.compose.ui.graphics.Color
import com.example.frontend.data.coordenadaValida
import com.example.frontend.data.pinesDelMapa
import com.example.frontend.data.remote.esDireccionLocal
import com.example.frontend.model.CandidatoMejorParaTi
import com.example.frontend.model.FormulaMejorParaTi
import com.example.frontend.model.Spot
import com.example.frontend.model.SpotCategory
import com.example.frontend.model.TasteProfile
import com.example.frontend.model.rankearMejorParaTi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Los numeros esperados son los mismos de tests/test_best_for_you.py del backend: si alguien cambia la
// formula en un lado y no en el otro, alguna de las dos suites falla.
class MejorParaTiTest {

    private fun candidato(id: String, cocina: String, vegetariano: Boolean, barato: Boolean, espera: Int?, reciente: Boolean = true) =
        CandidatoMejorParaTi(id, id, "🍽", cocina, 4.5, barato, vegetariano, espera, if (espera == null) null else 10, reciente && espera != null)

    @Test
    fun puntajeDeEspera() {
        assertEquals(100, FormulaMejorParaTi.puntajeEspera(0, true))
        assertEquals(50, FormulaMejorParaTi.puntajeEspera(20, true))
        assertEquals(0, FormulaMejorParaTi.puntajeEspera(60, true))
        assertEquals(93, FormulaMejorParaTi.puntajeEspera(3, true)) // 92.5 redondea hacia arriba, igual que el backend
        assertEquals(50, FormulaMejorParaTi.puntajeEspera(null, false))
        assertEquals(50, FormulaMejorParaTi.puntajeEspera(5, false)) // reporte viejo: neutro
    }

    @Test
    fun compatibilidadSoloCuentaLoQueElPerfilLleno() {
        val soloHamburguesas = TasteProfile(budget = "", dietary = emptySet(), cuisines = setOf("Artisan Burgers"))
        assertEquals(100, soloHamburguesas.compatibilidad("Burgers & Fries", "Conda de Bons", esVegetariano = false, esBarato = false))
        val baratoYHamburguesas = TasteProfile(budget = "Budget-friendly", dietary = emptySet(), cuisines = setOf("Artisan Burgers"))
        assertEquals(71, baratoYHamburguesas.compatibilidad("Burgers & Fries", "Conda de Bons", false, false))
        val veganoSinPresupuesto = TasteProfile(budget = "Treat yourself", dietary = setOf("Vegan"), cuisines = emptySet())
        assertEquals(40, veganoSinPresupuesto.compatibilidad("Burgers & Fries", "Conda de Bons", false, false))
        val nadaQueMedir = TasteProfile(budget = "", dietary = setOf("Halal"), cuisines = emptySet())
        assertEquals(50, nadaQueMedir.compatibilidad("Burgers & Fries", "Conda de Bons", false, false))
        val pizzaVegana = TasteProfile(budget = "", dietary = setOf("Vegan"), cuisines = setOf("Wood-Fired Pizza"))
        assertEquals(63, pizzaVegana.compatibilidad("Pizza", "X", esVegetariano = false, esBarato = false)) // 62.5
    }

    @Test
    fun rankingMezclaEsperaYGusto() {
        val perfil = TasteProfile(budget = "Budget-friendly", dietary = setOf("Vegetarian"), cuisines = setOf("Crepas & Bowls"))
        val ranking = rankearMejorParaTi(
            listOf(
                candidato("la-esquina-burger-lab", "Burgers & Craft Shakes", vegetariano = false, barato = false, espera = 4),
                candidato("green-bowl-co", "Healthy Bowls", vegetariano = true, barato = true, espera = 36),
                candidato("poodle-pizza-slices", "Wood-Fired Pizza", vegetariano = true, barato = true, espera = null),
            ),
            perfil,
        )
        assertEquals(listOf("green-bowl-co", "poodle-pizza-slices", "la-esquina-burger-lab"), ranking.map { it.candidato.spotId })
        assertEquals(55.0, ranking[0].puntaje, 0.001)
        assertEquals(50.0, ranking[1].puntaje, 0.001)
        assertEquals(45.0, ranking[2].puntaje, 0.001)
        assertEquals(listOf(1, 2, 3), ranking.map { it.rank })
    }

    @Test
    fun enEmpateGanaLaEsperaConocidaMasCorta() {
        val perfil = TasteProfile(budget = "", dietary = emptySet(), cuisines = emptySet())
        val ranking = rankearMejorParaTi(
            listOf(
                candidato("sin-reporte", "Tacos", false, false, espera = null),
                candidato("veinte-min", "Tacos", false, false, espera = 20),
            ),
            perfil,
        )
        // Los dos tienen 50 de espera y 50 de gusto; el que tiene un dato real va primero.
        assertEquals("veinte-min", ranking.first().candidato.spotId)
    }

    @Test
    fun pinesUsanLasCoordenadasRealesYDescartanLasInvalidas() {
        val spots = listOf(
            spot("norte-oeste", lat = 4.6045, lng = -74.0670, afinidad = 70),
            spot("sur-este", lat = 4.5998, lng = -74.0646, afinidad = 90),
            spot("en-cero", lat = 0.0, lng = 0.0, afinidad = 99),
            spot("sin-dato", lat = null, lng = null, afinidad = 99),
        )
        val pines = pinesDelMapa(spots, emptyMap())
        assertEquals(listOf("sur-este", "norte-oeste"), pines.map { it.spotId }) // el mas compatible es el #1
        assertEquals(listOf(1, 2), pines.map { it.rank })
        val noroeste = pines.first { it.spotId == "norte-oeste" }
        assertEquals(4.6045, noroeste.lat, 1e-9)
        assertEquals(-74.0670, noroeste.lng, 1e-9)
        assertFalse(coordenadaValida(91.0, 0.5))
    }

    @Test
    fun soloSePidePermisoParaBackendsEnLaRedLocal() {
        assertTrue(esDireccionLocal("http://10.0.2.2:8000/"))
        assertTrue(esDireccionLocal("http://192.168.1.20:8000/"))
        assertTrue(esDireccionLocal("http://172.20.0.5:8000/"))
        assertFalse(esDireccionLocal("http://127.0.0.1:8000/"))
        assertFalse(esDireccionLocal("https://campusbites.onrender.com/"))
    }

    private fun spot(id: String, lat: Double?, lng: Double?, afinidad: Int) = Spot(
        id = id, emoji = "🍽", emojiBackground = Color.White, emojiBorder = Color.White, name = id,
        subtitle = "Food • Campus", rating = 4.0, price = "$", distance = "3 min walk", walkMinutes = 3,
        latitude = lat, longitude = lng, isBudget = true, isVegetarian = false, isHighProtein = false,
        affinityPercent = afinidad, category = SpotCategory.FOOD_TRUCKS, noteLabel = "", note = "",
    )
}
