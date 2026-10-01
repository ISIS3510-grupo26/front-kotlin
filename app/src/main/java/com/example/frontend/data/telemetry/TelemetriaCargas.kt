package com.example.frontend.data.telemetry

import android.os.Build
import com.example.frontend.BuildConfig
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.PageLoadBatchDto
import com.example.frontend.data.remote.PageLoadEventDto
import com.google.gson.JsonParseException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

// Mide cada carga de la pagina de restaurante y la reporta al backend en lotes,
// en segundo plano, sin bloquear la UI. Es la fuente de datos de las business questions.
class TelemetriaCargas(private val api: CampusBitesApi) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val pendientes = ArrayDeque<PageLoadEventDto>()
    private val sessionId = UUID.randomUUID().toString()

    fun registrar(spotId: String, durationMs: Long, error: Throwable?) {
        val evento = PageLoadEventDto(
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
            occurredAt = ahoraIso8601(),
        )
        scope.launch {
            lock.withLock {
                pendientes.addLast(evento)
                while (pendientes.size > MAX_PENDIENTES) pendientes.removeFirst()
            }
            enviarPendientes()
        }
    }

    // Si el envio falla, los eventos quedan en cola y se reintentan con el siguiente evento.
    // El backend descarta duplicados por eventId, asi que reintentar es seguro.
    private suspend fun enviarPendientes() = lock.withLock {
        if (pendientes.isEmpty()) return@withLock
        val lote = pendientes.toList()
        try {
            api.enviarCargas(PageLoadBatchDto(lote))
            repeat(lote.size) { pendientes.removeFirst() }
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val SCREEN_RESTAURANT_DETAIL = "restaurant_detail"
        const val MAX_PENDIENTES = 500

        fun clasificarError(e: Throwable): String = when (e) {
            is HttpException -> "HTTP_${e.code()}"
            is SocketTimeoutException -> "TIMEOUT"
            is UnknownHostException, is java.net.ConnectException -> "NO_CONNECTION"
            is JsonParseException, is IllegalArgumentException -> "PARSE_ERROR"
            is IOException -> "NETWORK_ERROR"
            else -> "UNKNOWN"
        }

        // java.time requiere API 26; minSdk es 24.
        fun ahoraIso8601(): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date())

        fun modeloDispositivo(): String {
            val fabricante = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            return if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
            else "$fabricante ${Build.MODEL}"
        }
    }
}
