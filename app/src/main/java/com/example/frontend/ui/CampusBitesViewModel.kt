package com.example.frontend.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.PopularidadRepository
import com.example.frontend.data.SpotsRepository
import com.example.frontend.data.remote.ApiClient
import com.example.frontend.data.sampleSpots
import com.example.frontend.data.telemetry.TelemetriaCargas
import com.example.frontend.model.PeerReview
import com.example.frontend.model.PopularidadPorHora
import com.example.frontend.model.RankingHora
import com.example.frontend.model.Spot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.frontend.data.BusquedasPorDiaRepository
import com.example.frontend.model.BusquedasPorDiaSemana

data class EstadoDetalle(
    val spot: Spot,
    val cargando: Boolean = true,
    val error: String? = null,
)

// Estado de la feature "Popular right now" (respuesta de la BQ3 para la hora actual del celular).
data class EstadoPopularidad(
    val ahora: RankingHora? = null,
    val cargando: Boolean = true,
    val error: Boolean = false,
)

// Dueño del estado de la app. La UI solo observa y envía eventos.
class CampusBitesViewModel(
    private val repository: SpotsRepository = SpotsRepository(ApiClient.api),
    private val telemetria: TelemetriaCargas = TelemetriaCargas(ApiClient.api),
    private val popularidadRepository: PopularidadRepository = PopularidadRepository(ApiClient.api),
    private val busquedasPorDiaRepository: BusquedasPorDiaRepository = BusquedasPorDiaRepository(ApiClient.api)
) : ViewModel() {

    // Los guardados viven en el dispositivo; arrancan con los del diseño original.
    private var savedIds by mutableStateOf(sampleSpots.filter { it.isSaved }.map { it.id }.toSet())
    private var catalogo by mutableStateOf<List<Spot>>(emptyList())

    val spots: List<Spot> get() = catalogo.map { it.copy(isSaved = it.id in savedIds) }

    var cargandoLista by mutableStateOf(true)
        private set
    var modoRespaldo by mutableStateOf(false)
        private set
    var detalle by mutableStateOf<EstadoDetalle?>(null)
        private set
    var popularidad by mutableStateOf(EstadoPopularidad())
        private set

    private var detalleJob: Job? = null

    init {
        cargarSitios()
        cargarPopularidad()
    }

    fun cargarSitios() {
        cargandoLista = true
        viewModelScope.launch {
            val resultado = repository.listarSitios()
            catalogo = resultado.spots
            modoRespaldo = resultado.desdeRespaldo
            cargandoLista = false
        }
    }

    // BQ3: pide al backend el ranking (vistas + busquedas) de la hora en la que esta el celular.
    fun cargarPopularidad(forzar: Boolean = false) {
        popularidad = popularidad.copy(cargando = true, error = false)
        viewModelScope.launch {
            try {
                popularidad = EstadoPopularidad(ahora = popularidadRepository.rankingAhora(forzar), cargando = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin red no hay ranking: la seccion se oculta y la app sigue funcionando.
                popularidad = popularidad.copy(cargando = false, error = true)
            }
        }
    }

    // Para la pantalla "Popular by hour": todas las horas con actividad.
    suspend fun popularidadPorHoras(): PopularidadPorHora = popularidadRepository.rankingPorHoras()

    suspend fun busquedasPorDiaSemana(): BusquedasPorDiaSemana = busquedasPorDiaRepository.cargar()

    fun estaGuardado(id: String) = id in savedIds

    fun marcarGuardado(id: String) {
        savedIds = if (id in savedIds) savedIds - id else savedIds + id
    }

    fun buscarSitio(id: String): Spot? = spots.firstOrNull { it.id == id }

    // El usuario escribio en el buscador y eligio este restaurante: se cuenta como "busqueda" (BQ3)
    // y luego se abre como cualquier otro (lo que ademas genera la vista de pagina).
    fun abrirDesdeBusqueda(spot: Spot) {
        telemetria.registrarBusqueda(spot.id)
        abrirSitio(spot)
    }

    // Una "carga de la pagina del restaurante" = desde que el usuario la abre
    // hasta que la info completa (menu + reseñas) llega del backend, o falla.
    fun abrirSitio(spot: Spot) {
        detalle = EstadoDetalle(spot = spot.copy(isSaved = spot.id in savedIds))
        detalleJob?.cancel()
        detalleJob = viewModelScope.launch {
            val inicio = SystemClock.elapsedRealtime()
            try {
                val completo = repository.obtenerSitio(spot.id)
                telemetria.registrar(spot.id, SystemClock.elapsedRealtime() - inicio, error = null)
                detalle = EstadoDetalle(spot = completo.copy(isSaved = spot.id in savedIds), cargando = false)
            } catch (e: CancellationException) {
                throw e // el usuario salio antes de terminar: no es una carga completada ni fallida
            } catch (e: Exception) {
                telemetria.registrar(spot.id, SystemClock.elapsedRealtime() - inicio, error = e)
                detalle = EstadoDetalle(spot = spot, cargando = false, error = "Couldn't load this spot. Check your connection.")
            }
        }
    }

    fun reintentarDetalle() {
        detalle?.let { abrirSitio(it.spot) }
    }

    fun cerrarDetalle() {
        detalleJob?.cancel()
        detalle = null
        // La vista que acaba de hacer el usuario ya es telemetria: refrescamos el ranking
        // para que "Popular right now" refleje la actividad mas reciente.
        cargarPopularidad(forzar = true)
    }

    // Las reseñas escritas en la app viven en memoria mientras el backend no las persista.
    fun agregarResena(spotId: String, estrellas: Int, texto: String) {
        val nueva = PeerReview(
            authorName = "Julian Bierez",
            initials = "JB",
            program = "Engineering, Sem 6",
            stars = estrellas,
            text = texto,
            dinedAgo = "Dined today",
            helpfulCount = 0,
        )
        fun Spot.conResena() = copy(reviews = listOf(nueva) + reviews, totalReviews = totalReviews + 1)
        catalogo = catalogo.map { if (it.id == spotId) it.conResena() else it }
        detalle = detalle?.let { if (it.spot.id == spotId) it.copy(spot = it.spot.conResena()) else it }
    }
}
