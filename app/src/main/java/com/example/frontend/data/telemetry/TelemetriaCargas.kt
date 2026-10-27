package com.example.frontend.data.telemetry

import android.os.Build
import com.example.frontend.BuildConfig
import com.example.frontend.data.Dependencias
import com.example.frontend.data.aIso8601
import com.example.frontend.data.local.EventoPendienteEntidad
import com.example.frontend.data.local.PendientesDao
import com.example.frontend.data.remote.PageLoadEventDto
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Mide cada carga de la pagina de restaurante y la deja en la base local; TrabajoSincronizacion
// la envia al backend en lotes cuando hay red. Nada de esto bloquea la UI.
// Es la fuente de datos de las business questions.
class TelemetriaCargas(
    private val pendientes: PendientesDao = Dependencias.baseDatos.pendientesDao(),
    private val alEncolar: () -> Unit = Dependencias::programarSincronizacion,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val sessionId = UUID.randomUUID().toString()

    // Vista de pagina de restaurante (BQ1, BQ2 y BQ3): cuanto tardo y si fallo.
    fun registrar(spotId: String, durationMs: Long, error: Throwable?) {
        encolar(
            PageLoadEventDto(
                eventId = UUID.randomUUID().toString(),
                screen = SCREEN_RESTAURANT_DETAIL,
                spotId = spotId,
                durationMs = durationMs,
                success = error == null,
                httpStatus = if (error == null) 200 else (error as? HttpException)?.code(),
                errorType = error?.let(::clasificarError),
                deviceModel = modeloDispositivo(),
                osName = "Android",
                osVersion = Build.VERSION.RELEASE,
                platform = "android-kotlin",
                appVersion = BuildConfig.VERSION_NAME,
                sessionId = sessionId,
                occurredAt = aIso8601(),
            )
        )
    }

    // Busqueda (BQ3): el usuario escribio en el buscador y eligio este restaurante.
    // Usa el mismo contrato y la misma cola; el backend lo distingue por screen = "search".
    fun registrarBusqueda(spotId: String) {
        encolar(
            PageLoadEventDto(
                eventId = UUID.randomUUID().toString(),
                screen = SCREEN_SEARCH,
                spotId = spotId,
                durationMs = 0,
                success = true,
                httpStatus = null,
                errorType = null,
                deviceModel = modeloDispositivo(),
                osName = "Android",
                osVersion = Build.VERSION.RELEASE,
                platform = "android-kotlin",
                appVersion = BuildConfig.VERSION_NAME,
                sessionId = sessionId,
                occurredAt = aIso8601(),
            )
        )
    }

    // Antes la cola vivia en memoria y se perdia al cerrar la app. Ahora se escribe en disco (Room)
    // en un hilo de IO y WorkManager la envia apenas haya conexion, aunque la app ya este cerrada.
    private fun encolar(evento: PageLoadEventDto) {
        scope.launch {
            pendientes.encolarEvento(EventoPendienteEntidad(evento.eventId, gson.toJson(evento), System.currentTimeMillis()))
            pendientes.recortarEventos(MAX_PENDIENTES)
            alEncolar()
        }
    }

    private companion object {
        const val SCREEN_RESTAURANT_DETAIL = "restaurant_detail"
        const val SCREEN_SEARCH = "search"
        const val MAX_PENDIENTES = 2000

        fun clasificarError(e: Throwable): String = when (e) {
            is HttpException -> "HTTP_${e.code()}"
            is SocketTimeoutException -> "TIMEOUT"
            is UnknownHostException, is java.net.ConnectException -> "NO_CONNECTION"
            is JsonParseException, is IllegalArgumentException -> "PARSE_ERROR"
            is IOException -> "NETWORK_ERROR"
            else -> "UNKNOWN"
        }

        fun modeloDispositivo(): String {
            val fabricante = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            return if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
            else "$fabricante ${Build.MODEL}"
        }
    }
}
