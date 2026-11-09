package com.example.frontend.data.conectividad

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.frontend.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

// Dice si la app tiene conexion de verdad. La app no pregunta cada vez si hay red: el sistema avisa los
// cambios, cada peticion cuenta si llego o fallo, y el ViewModel reacciona (reintenta lo que habia fallado).
//
// Antes solo se creia lo que decia el NetworkCallback, y eso falla en los dos sentidos: con wifi conectado
// pero sin internet el sistema dice "hay red" (por eso el login no mostraba el aviso), y el emulador a veces
// dice "no hay red" mientras las peticiones si llegan. Ahora manda la evidencia real:
//   - cualquier respuesta del backend (aunque sea un error 4xx/5xx) = hay conexion;
//   - una peticion que falla por red (IOException) = no hay conexion, y se prueba GET /health cada 5 s
//     hasta que conteste;
//   - el NetworkCallback solo dispara una prueba inmediata cuando el sistema ve que la red cambio.
class MonitorConectividad(context: Context, private val scope: CoroutineScope) {

    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Arranca con lo que dice el sistema; la primera prueba de /health lo confirma o lo corrige.
    private val _hayRed = MutableStateFlow(redActualConInternet())
    val hayRed: StateFlow<Boolean> = _hayRed.asStateFlow()

    // Cliente aparte, con timeouts cortos y sin interceptores: solo sirve para la prueba de /health.
    private val clientePrueba = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
    private var sondeo: Job? = null

    init {
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                if (tieneInternet(capacidades) != _hayRed.value) probarAhora()
            }

            override fun onLost(network: Network) {
                probarAhora()
            }
        })
        // Al abrir la app se comprueba una vez: si no hay salida, el aviso aparece desde la primera pantalla.
        probarAhora()
    }

    // Las llama InterceptorConectividad en cada peticion de Retrofit.
    fun avisarFallaDeRed() {
        _hayRed.value = false
        empezarSondeo()
    }

    fun avisarRespuesta() {
        _hayRed.value = true
    }

    // En el scope de IO de la app (Dependencias.scopeApp), nunca en Main: la prueba hace una peticion.
    private fun probarAhora() {
        scope.launch {
            if (responde()) avisarRespuesta() else avisarFallaDeRed()
        }
    }

    // Mientras no haya conexion se prueba cada 5 s. Cuando el backend contesta, el ciclo termina.
    private fun empezarSondeo() {
        if (sondeo?.isActive == true) return
        sondeo = scope.launch {
            while (isActive && !_hayRed.value) {
                delay(INTERVALO_SONDEO_MS)
                if (responde()) avisarRespuesta()
            }
        }
    }

    private fun responde(): Boolean = try {
        clientePrueba.newCall(Request.Builder().url(BuildConfig.API_BASE_URL + "health").build()).execute().use { true }
    } catch (e: Exception) {
        false
    }

    private fun redActualConInternet(): Boolean =
        manager.getNetworkCapabilities(manager.activeNetwork)?.let(::tieneInternet) ?: false

    // No se exige VALIDATED: el emulador y algunas redes de laboratorio nunca la marcan aunque el backend
    // si responda. La prueba de /health es la que confirma que hay salida.
    private fun tieneInternet(capacidades: NetworkCapabilities) =
        capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

    companion object {
        private const val INTERVALO_SONDEO_MS = 5_000L
    }
}
