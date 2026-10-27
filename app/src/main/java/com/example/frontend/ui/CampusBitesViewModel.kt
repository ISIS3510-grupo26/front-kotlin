package com.example.frontend.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.data.Dependencias
import com.example.frontend.data.OrigenRecomendaciones
import com.example.frontend.data.RecomendacionesRepository
import com.example.frontend.data.sesion.Sesion
import com.example.frontend.data.sesion.SesionRepository
import com.example.frontend.data.sesion.mensajeErrorSesion
import com.example.frontend.data.ubicacion.OrigenUbicacion
import com.example.frontend.data.ubicacion.ProveedorUbicacion
import com.example.frontend.model.Recomendacion
import com.example.frontend.data.OrigenDatos
import com.example.frontend.data.PopularidadRepository
import com.example.frontend.data.SpotsRepository
import com.example.frontend.data.TiemposEsperaRepository
import com.example.frontend.data.conectividad.MonitorConectividad
import com.example.frontend.data.remote.ApiClient
import com.example.frontend.data.remote.TokenActual
import com.example.frontend.data.sampleSpots
import com.example.frontend.data.telemetry.TelemetriaCargas
import com.example.frontend.model.PeerReview
import com.example.frontend.model.PopularidadPorHora
import com.example.frontend.model.RankingHora
import com.example.frontend.model.Spot
import com.example.frontend.model.TiempoEspera
import com.example.frontend.model.textoHaceCuanto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.frontend.data.BusquedasPorDiaRepository
import com.example.frontend.model.BusquedasPorDiaSemana

data class EstadoDetalle(
    val spot: Spot,
    val cargando: Boolean = true,
    val error: String? = null,
    val aviso: String? = null,          // p. ej. "Offline · showing info saved 10 min ago"
    val actualizando: Boolean = false,  // se ve la copia local mientras llega la version nueva
)

// BQ3 "Picks near you": recomendaciones cercanas que el estudiante no ha probado.
data class EstadoRecomendaciones(
    val lista: List<Recomendacion> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,           // p. ej. "Offline · picks from 20 min ago"
    val conGps: Boolean = false,         // false = se uso el centro del campus (sin permiso o GPS apagado)
    val cargadoUnaVez: Boolean = false,
)

// Estado de la pantalla de login/registro.
data class EstadoLogin(
    val cargando: Boolean = false,
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
    private val telemetria: TelemetriaCargas = TelemetriaCargas(),
    private val popularidadRepository: PopularidadRepository = PopularidadRepository(ApiClient.api),
    private val busquedasPorDiaRepository: BusquedasPorDiaRepository = BusquedasPorDiaRepository(ApiClient.api),
    private val tiemposEsperaRepository: TiemposEsperaRepository = TiemposEsperaRepository(ApiClient.api),
    private val monitor: MonitorConectividad = Dependencias.monitorConectividad,
    private val sesionRepository: SesionRepository = SesionRepository(ApiClient.api, Dependencias.almacenSesion),
    private val recomendacionesRepository: RecomendacionesRepository = RecomendacionesRepository(ApiClient.api),
    private val ubicacion: ProveedorUbicacion = Dependencias.proveedorUbicacion,
) : ViewModel() {

    // Los guardados viven en el dispositivo; arrancan con los del diseño original.
    private var savedIds by mutableStateOf(sampleSpots.filter { it.isSaved }.map { it.id }.toSet())
    private var catalogo by mutableStateOf<List<Spot>>(emptyList())

    // Reseñas escritas en este celular. Se guardan aparte para que no desaparezcan cuando
    // el detalle se vuelve a pedir al backend (que todavia no las tiene).
    private var resenasLocales by mutableStateOf<Map<String, List<PeerReview>>>(emptyMap())

    val spots: List<Spot> get() = catalogo.map { it.conResenasLocales().copy(isSaved = it.id in savedIds) }

    var cargandoLista by mutableStateOf(true)
        private set
    var origenCatalogo by mutableStateOf(OrigenDatos.RED)
        private set
    private var catalogoGuardadoEn by mutableStateOf<Long?>(null)
    val modoRespaldo: Boolean get() = origenCatalogo != OrigenDatos.RED

    var hayConexion by mutableStateOf(monitor.hayRed.value)
        private set

    // BQ4: ultimo tiempo de espera de cada restaurante (spotId -> tiempo).
    var tiemposEspera by mutableStateOf<Map<String, TiempoEspera>>(emptyMap())
        private set

    // El corazon y las reseñas locales se aplican al leer, asi siempre muestran el estado actual.
    private var estadoDetalle by mutableStateOf<EstadoDetalle?>(null)
    val detalle: EstadoDetalle?
        get() = estadoDetalle?.let { it.copy(spot = it.spot.conResenasLocales().copy(isSaved = it.spot.id in savedIds)) }

    var popularidad by mutableStateOf(EstadoPopularidad())
        private set

    // Sesion abierta (null = sin login). Sale de DataStore, asi que sobrevive a cerrar la app.
    var sesion by mutableStateOf<Sesion?>(null)
        private set
    var login by mutableStateOf(EstadoLogin())
        private set
    var recomendaciones by mutableStateOf(EstadoRecomendaciones())
        private set

    private var detalleJob: Job? = null
    private var recomendacionesJob: Job? = null

    init {
        cargarSitios()
        cargarPopularidad()
        cargarTiemposEspera()
        observarConexion()
        observarSincronizacion()
        restaurarSesion()
        observarSesion()
    }

    // Texto del aviso de la parte de arriba; null si todo viene de la red.
    val avisoConexion: String?
        get() = when {
            origenCatalogo == OrigenDatos.COPIA_LOCAL -> {
                val hace = catalogoGuardadoEn?.let { textoHaceCuanto(((System.currentTimeMillis() - it) / 60_000L).toInt()) }
                "Offline: showing the catalog saved ${hace ?: "earlier"}. Tap to retry."
            }
            origenCatalogo == OrigenDatos.CATALOGO_EMPAQUETADO -> "Offline: showing the bundled catalog. Tap to retry."
            !hayConexion -> "You're offline. Your reports will be sent when you reconnect."
            else -> null
        }

    fun cargarSitios() {
        cargandoLista = true
        viewModelScope.launch {
            val resultado = repository.listarSitios()
            catalogo = resultado.spots
            origenCatalogo = resultado.origen
            catalogoGuardadoEn = resultado.guardadoEn
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

    // BQ4: con red trae los tiempos del backend; sin red usa los ultimos guardados.
    fun cargarTiemposEspera() {
        viewModelScope.launch {
            tiemposEspera = tiemposEsperaRepository.cargar().tiempos
        }
    }

    fun reportarEspera(spotId: String, minutos: Int) {
        viewModelScope.launch {
            val anterior = tiemposEspera[spotId]
            val nuevo = tiemposEsperaRepository.reportar(spotId, minutos)
            tiemposEspera = tiemposEspera + (spotId to nuevo.copy(reportesRecientes = (anterior?.reportesRecientes ?: 0) + 1))
        }
    }

    // Conectividad eventual: cuando vuelve la red se reintenta lo que habia quedado con datos viejos
    // o con error, sin que el usuario tenga que tocar "Retry".
    private fun observarConexion() {
        viewModelScope.launch {
            monitor.hayRed.collect { hay ->
                val volvio = hay && !hayConexion
                hayConexion = hay
                if (volvio) alVolverLaConexion()
            }
        }
    }

    private fun alVolverLaConexion() {
        if (modoRespaldo) cargarSitios()
        if (sesion != null && (recomendaciones.error != null || recomendaciones.aviso != null)) cargarRecomendaciones(forzar = true)
        val actual = estadoDetalle
        if (actual != null && (actual.error != null || actual.aviso != null)) reintentarDetalle()
        if (popularidad.error) cargarPopularidad(forzar = true)
        cargarTiemposEspera()
        Dependencias.programarSincronizacion()
    }

    // Cuando se terminan de enviar los reportes pendientes, se refrescan los tiempos
    // para que deje de decir "Sending your report...".
    private fun observarSincronizacion() {
        viewModelScope.launch {
            Dependencias.sincronizador.ultimaSincronizacion.collect {
                if (tiemposEspera.values.any { it.pendienteDeEnvio }) cargarTiemposEspera()
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
        detalleJob?.cancel()
        estadoDetalle = EstadoDetalle(spot = spot)
        detalleJob = viewModelScope.launch {
            val inicio = SystemClock.elapsedRealtime()
            // Stale-while-revalidate: si ya se habia abierto antes, la copia local se ve al instante
            // mientras se pide la version nueva.
            val guardado = repository.detalleGuardado(spot.id)
            if (guardado != null) estadoDetalle = EstadoDetalle(guardado.spot, cargando = false, actualizando = true)
            try {
                val completo = repository.obtenerSitio(spot.id)
                telemetria.registrar(spot.id, SystemClock.elapsedRealtime() - inicio, error = null)
                estadoDetalle = EstadoDetalle(spot = completo, cargando = false)
            } catch (e: CancellationException) {
                throw e // el usuario salio antes de terminar: no es una carga completada ni fallida
            } catch (e: Exception) {
                // La falla se cuenta igual (BQ2), aunque el usuario si alcance a ver la copia local.
                telemetria.registrar(spot.id, SystemClock.elapsedRealtime() - inicio, error = e)
                estadoDetalle = if (guardado != null) {
                    val hace = textoHaceCuanto(((System.currentTimeMillis() - guardado.guardadoEn) / 60_000L).toInt())
                    EstadoDetalle(guardado.spot, cargando = false, aviso = "Offline · showing info saved $hace")
                } else {
                    EstadoDetalle(spot = spot, cargando = false, error = "Couldn't load this spot. Check your connection.")
                }
            }
        }
    }

    fun reintentarDetalle() {
        estadoDetalle?.let { abrirSitio(it.spot) }
    }

    fun cerrarDetalle() {
        detalleJob?.cancel()
        estadoDetalle = null
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
        resenasLocales = resenasLocales + (spotId to listOf(nueva) + resenasLocales[spotId].orEmpty())
    }

    private fun Spot.conResenasLocales(): Spot {
        val propias = resenasLocales[id].orEmpty()
        return if (propias.isEmpty()) this else copy(reviews = propias + reviews, totalReviews = totalReviews + propias.size)
    }

    // ---------- Autenticacion ----------

    private fun restaurarSesion() {
        viewModelScope.launch { sesion = sesionRepository.restaurar() }
    }

    // Si la sesion se borra desde otro lado (401 del backend, token vencido), la UI se entera aqui.
    private fun observarSesion() {
        viewModelScope.launch {
            Dependencias.almacenSesion.sesion.collect { guardada ->
                // El token tiene que estar puesto antes de la primera peticion que necesita sesion.
                if (guardada != null && !guardada.vencida()) TokenActual.valor = guardada.token
                val antes = sesion
                sesion = guardada
                if (guardada == null && antes != null) recomendaciones = EstadoRecomendaciones()
                if (guardada != null && antes?.userId != guardada.userId) cargarRecomendaciones()
            }
        }
    }

    fun entrar(email: String, password: String, registrando: Boolean, alTerminar: () -> Unit) {
        if (!hayConexion) {
            login = EstadoLogin(error = MENSAJE_LOGIN_OFFLINE)
            return
        }
        login = EstadoLogin(cargando = true)
        viewModelScope.launch {
            try {
                sesion = if (registrando) sesionRepository.registrarse(email, password)
                else sesionRepository.iniciarSesion(email, password)
                login = EstadoLogin()
                alTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                login = EstadoLogin(error = mensajeErrorSesion(e, registrando))
            }
        }
    }

    fun limpiarErrorLogin() {
        login = EstadoLogin()
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            sesionRepository.cerrarSesion()
            recomendacionesRepository.olvidar()
            sesion = null
            recomendaciones = EstadoRecomendaciones()
        }
    }

    // ---------- BQ3: Picks near you ----------

    // Multi-threading: esta corrutina corre en Main (viewModelScope). Adentro, withContext(IO) abre dos
    // corrutinas hijas en paralelo: leer el archivo local y leer el sensor de ubicacion. Con el archivo se
    // pinta algo de inmediato; con la ubicacion se pide la respuesta nueva. Cada asignacion de estado vuelve a Main.
    fun cargarRecomendaciones(forzar: Boolean = false) {
        val usuario = sesion?.userId ?: return
        recomendacionesJob?.cancel()
        recomendaciones = recomendaciones.copy(cargando = true, error = null)
        recomendacionesJob = viewModelScope.launch {
            val (punto, guardadas) = withContext(Dispatchers.IO) {
                val lecturaUbicacion = async { ubicacion.obtener() }
                val lecturaArchivo = async { recomendacionesRepository.leerArchivo(usuario) }
                lecturaUbicacion.await() to lecturaArchivo.await()
            }
            val conGps = punto.origen == OrigenUbicacion.GPS
            if (guardadas != null && recomendaciones.lista.isEmpty()) {
                recomendaciones = recomendaciones.copy(lista = guardadas.lista, conGps = conGps)
            }
            recomendaciones = try {
                val resultado = recomendacionesRepository.cargar(usuario, punto, forzar)
                val aviso = if (resultado.origen == OrigenRecomendaciones.ARCHIVO) {
                    val hace = textoHaceCuanto(((System.currentTimeMillis() - resultado.guardadoEn) / 60_000L).toInt())
                    "Offline · picks from $hace"
                } else {
                    null
                }
                EstadoRecomendaciones(lista = resultado.lista, conGps = conGps, cargadoUnaVez = true, aviso = aviso)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EstadoRecomendaciones(
                    conGps = conGps,
                    cargadoUnaVez = true,
                    error = if (hayConexion) "Couldn't load picks near you." else MENSAJE_PICKS_OFFLINE,
                )
            }
        }
    }

    // La UI avisa cuando el estudiante responde el permiso de ubicacion: se recalcula con la estrategia nueva.
    fun alCambiarPermisoUbicacion() {
        if (sesion != null) cargarRecomendaciones(forzar = true)
    }

    private companion object {
        const val MENSAJE_LOGIN_OFFLINE = "You're offline. Signing in needs a connection; you can keep browsing meanwhile."
        const val MENSAJE_PICKS_OFFLINE = "You're offline. Picks near you will load when you reconnect."
    }
}
