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
import com.example.frontend.data.MejorParaTiRepository
import com.example.frontend.data.OrigenMejorParaTi
import com.example.frontend.data.ResenasRepository
import com.example.frontend.data.gusto.AlmacenGusto
import com.example.frontend.data.local.Borrador
import com.example.frontend.data.local.ResenaPendienteEntidad
import com.example.frontend.model.SitioMejorParaTi
import com.example.frontend.model.TasteProfile
import com.example.frontend.model.rankearMejorParaTi

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

// BQ6 "Best for you right now": todos los restaurantes ordenados por espera + gusto.
data class EstadoMejorParaTi(
    val lista: List<SitioMejorParaTi> = emptyList(),
    val cargando: Boolean = false,
    val aviso: String? = null,           // p. ej. "Offline · using wait times saved on this phone"
)

// Estado de la feature "Popular right now" (respuesta de la BQ3 para la hora actual del celular).
data class EstadoPopularidad(
    val ahora: RankingHora? = null,
    val cargando: Boolean = true,
    val error: Boolean = false,
    val guardadoEn: Long? = null,        // con valor: sin red, es el ultimo ranking guardado en el celular
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
    private val almacenGusto: AlmacenGusto = Dependencias.almacenGusto,
    private val mejorParaTiRepository: MejorParaTiRepository = MejorParaTiRepository(ApiClient.api),
    private val resenasRepository: ResenasRepository = ResenasRepository(),
) : ViewModel() {

    // Los guardados viven en DataStore (sobreviven a cerrar la app). Mientras se leen, y si el estudiante
    // nunca ha tocado un corazon, se usan los del diseño original.
    private var savedIds by mutableStateOf(sampleSpots.filter { it.isSaved }.map { it.id }.toSet())
    private var catalogo by mutableStateOf<List<Spot>>(emptyList())

    // Reseñas escritas en este celular que todavia esperan en la cola de Room. Se muestran aparte para
    // que no desaparezcan cuando el detalle se vuelve a pedir al backend (que todavia no las tiene).
    private var resenasLocales by mutableStateOf<Map<String, List<PeerReview>>>(emptyMap())

    // Perfil de gusto guardado en DataStore; la pantalla Taste Profile lo edita sin necesitar red.
    var perfilGusto by mutableStateOf(TasteProfile())
        private set

    var mejorParaTi by mutableStateOf(EstadoMejorParaTi())
        private set

    // spotId -> compatibilidad con el perfil, sacada del ultimo ranking de la BQ6.
    private val compatibilidades: Map<String, Int>
        get() = mejorParaTi.lista.associate { it.candidato.spotId to it.compatibilidad }

    // affinityPercent deja de ser el numero escrito a mano en el catalogo: es la compatibilidad real.
    // Mientras el primer ranking no ha terminado se calcula directo con el perfil (son 7 restaurantes).
    val spots: List<Spot>
        get() = catalogo.map { spot ->
            spot.conResenasLocales().copy(
                isSaved = spot.id in savedIds,
                affinityPercent = compatibilidades[spot.id] ?: perfilGusto.compatibilidad(spot),
            )
        }

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
    private var mejorParaTiJob: Job? = null

    init {
        cargarSitios()
        cargarPopularidad()
        cargarTiemposEspera()
        observarConexion()
        observarSincronizacion()
        restaurarSesion()
        observarSesion()
        observarGusto()
        observarResenasPendientes()
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
            // Sin red la BQ6 se arma con este catalogo, asi que se calcula cuando ya esta cargado.
            cargarMejorParaTi()
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
                // Sin red se usa el ultimo ranking guardado; si nunca hubo uno, la seccion se oculta.
                val guardado = popularidadRepository.rankingAhoraGuardado()
                popularidad = popularidad.copy(
                    ahora = guardado?.first ?: popularidad.ahora,
                    guardadoEn = guardado?.second,
                    cargando = false,
                    error = true,
                )
            }
        }
    }

    // BQ4: con red trae los tiempos del backend; sin red usa los ultimos guardados.
    fun cargarTiemposEspera() {
        viewModelScope.launch {
            tiemposEspera = tiemposEsperaRepository.cargar().tiempos
            // Si la BQ6 se armo (o se esta armando) con los tiempos locales, se recalcula con estos.
            if (mejorParaTi.aviso != null || mejorParaTiJob?.isActive == true) cargarMejorParaTi()
        }
    }

    fun reportarEspera(spotId: String, minutos: Int) {
        viewModelScope.launch {
            val anterior = tiemposEspera[spotId]
            val nuevo = tiemposEsperaRepository.reportar(spotId, minutos)
            tiemposEspera = tiemposEspera + (spotId to nuevo.copy(reportesRecientes = (anterior?.reportesRecientes ?: 0) + 1))
            // Una espera nueva cambia el ranking de la BQ6 aunque todavia no haya llegado al backend.
            cargarMejorParaTi()
        }
    }

    // ---------- BQ6: Best for you right now ----------

    // Multi-threading: la corrutina arranca en Main (viewModelScope). Adentro, withContext(IO) lanza dos
    // async en paralelo: leer el perfil de gusto de DataStore y pedir la BQ6 a la red (con la copia local
    // de respaldo). No dependen una de la otra, asi que esperar las dos juntas tarda lo que tarde la mas
    // lenta y no la suma. Rankear es solo CPU (puntajes y orden), por eso va en Dispatchers.Default.
    // Al salir de cada withContext se vuelve a Main, que es donde se asigna el estado que lee Compose.
    fun cargarMejorParaTi() {
        mejorParaTiJob?.cancel()
        mejorParaTi = mejorParaTi.copy(cargando = true)
        val catalogoActual = catalogo
        val tiemposActuales = tiemposEspera
        mejorParaTiJob = viewModelScope.launch {
            val (perfil, resultado) = withContext(Dispatchers.IO) {
                val lecturaPerfil = async { almacenGusto.perfilActual() }
                val lecturaBq6 = async { mejorParaTiRepository.candidatos(catalogoActual, tiemposActuales) }
                lecturaPerfil.await() to lecturaBq6.await()
            }
            val ranking = withContext(Dispatchers.Default) { rankearMejorParaTi(resultado.candidatos, perfil) }
            mejorParaTi = EstadoMejorParaTi(
                lista = ranking,
                aviso = if (resultado.origen == OrigenMejorParaTi.COPIA_LOCAL) MENSAJE_MEJOR_PARA_TI_OFFLINE else null,
            )
        }
    }

    // El perfil vive en DataStore: aqui solo se refleja en la UI cada vez que cambia.
    private fun observarGusto() {
        viewModelScope.launch {
            almacenGusto.perfil.collect { perfilGusto = it }
        }
        viewModelScope.launch {
            almacenGusto.favoritos.collect { guardados -> if (guardados != null) savedIds = guardados }
        }
    }

    // Guardar no necesita red. Al terminar se recalcula la BQ6 con el perfil nuevo.
    fun guardarPerfilGusto(perfil: TasteProfile) {
        perfilGusto = perfil
        viewModelScope.launch {
            almacenGusto.guardarPerfil(perfil)
            cargarMejorParaTi()
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
        if (modoRespaldo) cargarSitios() else if (mejorParaTi.aviso != null) cargarMejorParaTi()
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
                if (tiemposEspera.values.any { it.pendienteDeEnvio }) {
                    cargarTiemposEspera()
                    cargarMejorParaTi()
                }
            }
        }
    }

    // Para la pantalla "Popular by hour": todas las horas con actividad.
    suspend fun popularidadPorHoras(): PopularidadPorHora = popularidadRepository.rankingPorHoras()

    suspend fun busquedasPorDiaSemana(): BusquedasPorDiaSemana = busquedasPorDiaRepository.cargar()

    fun estaGuardado(id: String) = id in savedIds

    fun marcarGuardado(id: String) {
        val nuevos = if (id in savedIds) savedIds - id else savedIds + id
        savedIds = nuevos
        viewModelScope.launch { almacenGusto.guardarFavoritos(nuevos) }
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

    // ---------- Reseñas (Write a Review) ----------

    // La reseña entra primero a la cola de Room: con o sin red queda a salvo y se ve de una vez en el detalle.
    // SincronizadorPendientes la envia cuando haya conexion y sesion.
    fun publicarResena(spotId: String, estrellas: Int, texto: String) {
        viewModelScope.launch { resenasRepository.publicar(spotId, estrellas, texto) }
    }

    suspend fun leerBorrador(spotId: String): Borrador? = resenasRepository.leerBorrador(spotId)

    suspend fun guardarBorrador(spotId: String, estrellas: Int, texto: String) =
        resenasRepository.guardarBorrador(spotId, estrellas, texto)

    // Cuando una reseña sale de la cola es porque el backend ya la tiene. Si el estudiante esta viendo ese
    // restaurante, se vuelve a pedir el detalle para que aparezca como reseña publicada.
    private fun observarResenasPendientes() {
        viewModelScope.launch {
            var antes = emptySet<String>()
            resenasRepository.pendientes.collect { cola ->
                resenasLocales = cola.groupBy { it.spotId }.mapValues { (_, lista) -> lista.map { it.aPeerReview() } }
                val ahora = cola.map { it.spotId }.toSet()
                val enviadas = antes - ahora
                antes = ahora
                estadoDetalle?.spot?.id?.takeIf { it in enviadas }?.let(::refrescarDetalle)
            }
        }
    }

    // Igual que abrirSitio pero sin cronometro: no es una carga que pidio el usuario, asi que no va a la telemetria.
    private fun refrescarDetalle(spotId: String) {
        viewModelScope.launch {
            try {
                val completo = repository.obtenerSitio(spotId)
                if (estadoDetalle?.spot?.id == spotId) estadoDetalle = EstadoDetalle(spot = completo, cargando = false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Si falla, se queda la version que ya se esta mostrando.
            }
        }
    }

    private fun ResenaPendienteEntidad.aPeerReview() = PeerReview(
        authorName = "You",
        initials = sesion?.email?.take(2)?.uppercase() ?: "ME",
        program = if (sesion == null) "Sign in to post it" else "Waiting to be posted",
        stars = estrellas,
        text = texto,
        dinedAgo = textoHaceCuanto(((System.currentTimeMillis() - creadaEn) / 60_000L).toInt()),
        helpfulCount = 0,
    )

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
                if (guardada != null && antes?.userId != guardada.userId) {
                    cargarRecomendaciones()
                    // Las reseñas que se escribieron sin sesion ya tienen a nombre de quien publicarse.
                    Dependencias.programarSincronizacion()
                }
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
        const val MENSAJE_MEJOR_PARA_TI_OFFLINE = "Offline · using the wait times saved on this phone"
    }
}
