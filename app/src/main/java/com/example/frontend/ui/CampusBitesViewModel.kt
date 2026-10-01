package com.example.frontend.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.SpotsRepository
import com.example.frontend.data.remote.ApiClient
import com.example.frontend.data.sampleSpots
import com.example.frontend.data.telemetry.TelemetriaCargas
import com.example.frontend.model.Spot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class EstadoDetalle(
    val spot: Spot,
    val cargando: Boolean = true,
    val error: String? = null,
)

// Dueño del estado de la app. La UI solo observa y envía eventos.
class CampusBitesViewModel(
    private val repository: SpotsRepository = SpotsRepository(ApiClient.api),
    private val telemetria: TelemetriaCargas = TelemetriaCargas(ApiClient.api),
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

    private var detalleJob: Job? = null

    init {
        cargarSitios()
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

    fun estaGuardado(id: String) = id in savedIds

    fun marcarGuardado(id: String) {
        savedIds = if (id in savedIds) savedIds - id else savedIds + id
    }

    // Una "carga de la pagina del restaurante" = desde que el usuario la abre
    // hasta que la info completa (menu + reseñas) llega del backend, o falla.
    fun abrirSitio(spot: Spot) {
        detalle = EstadoDetalle(spot = spot)
        detalleJob?.cancel()
        detalleJob = viewModelScope.launch {
            val inicio = SystemClock.elapsedRealtime()
            try {
                val completo = repository.obtenerSitio(spot.id)
                telemetria.registrar(spot.id, SystemClock.elapsedRealtime() - inicio, error = null)
                detalle = EstadoDetalle(spot = completo, cargando = false)
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
    }
}
