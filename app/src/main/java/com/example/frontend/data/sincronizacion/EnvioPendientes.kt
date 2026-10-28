package com.example.frontend.data.sincronizacion

import com.example.frontend.data.aIso8601
import com.example.frontend.data.local.PendientesDao
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.PageLoadBatchDto
import com.example.frontend.data.remote.PageLoadEventDto
import com.example.frontend.data.remote.WaitTimeReportDto
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

// Envia al backend todo lo que quedo en la base local: eventos de telemetria y reportes de espera.
// Lo usan los dos caminos de sincronizacion: la app abierta (apenas hay red) y WorkManager (app cerrada).
class SincronizadorPendientes(
    private val api: CampusBitesApi,
    private val pendientes: PendientesDao,
) {
    private val gson = Gson()
    // Si la app y WorkManager arrancan a la vez, el segundo espera: asi no se envia el mismo lote dos veces.
    private val lock = Mutex()

    // Cambia cada vez que se termina de enviar todo; el ViewModel lo escucha para refrescar la pantalla.
    private val _ultimaSincronizacion = MutableStateFlow(0L)
    val ultimaSincronizacion: StateFlow<Long> = _ultimaSincronizacion.asStateFlow()

    // Corrutinas anidadas en I/O: los eventos y los reportes van a endpoints distintos y no dependen
    // uno del otro, asi que se envian en paralelo (dos async dentro de esta corrutina) y se esperan ambos.
    suspend fun enviarTodo(): Boolean = lock.withLock {
        val todoEnviado = coroutineScope {
            val eventos = async(Dispatchers.IO) { enviarEventos() }
            val reportes = async(Dispatchers.IO) { enviarReportesEspera() }
            eventos.await() and reportes.await()
        }
        if (todoEnviado) _ultimaSincronizacion.value = System.currentTimeMillis()
        todoEnviado
    }

    // Lotes de 100: menos peticiones (y menos bateria) que una por evento.
    private suspend fun enviarEventos(): Boolean {
        while (true) {
            val lote = pendientes.eventos(TAMANO_LOTE)
            if (lote.isEmpty()) return true
            try {
                api.enviarCargas(PageLoadBatchDto(lote.map { gson.fromJson(it.json, PageLoadEventDto::class.java) }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                // 4xx: el lote esta mal formado y reintentarlo no lo arregla. 5xx: el servidor fallo, se reintenta.
                // 401 tampoco se descarta: es la sesion que vencio, no el lote; se envia cuando haya sesion o sin token.
                if (e.code() >= 500 || e.code() == 401) return false
            } catch (e: IOException) {
                return false
            }
            // El backend ignora eventId repetidos, asi que borrar despues de enviar es seguro aunque se repita.
            pendientes.borrarEventos(lote.map { it.eventId })
        }
    }

    private suspend fun enviarReportesEspera(): Boolean {
        var todoEnviado = true
        for (reporte in pendientes.reportes()) {
            try {
                api.reportarEspera(
                    reporte.spotId,
                    WaitTimeReportDto(
                        reportId = reporte.reportId,
                        minutes = reporte.minutos,
                        platform = PLATAFORMA,
                        reportedAt = aIso8601(reporte.reportadoEn),
                    ),
                )
                pendientes.borrarReporte(reporte.reportId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() >= 500 || e.code() == 401) todoEnviado = false else pendientes.borrarReporte(reporte.reportId)
            } catch (e: IOException) {
                todoEnviado = false
            }
        }
        return todoEnviado
    }

    private companion object {
        const val TAMANO_LOTE = 100
        const val PLATAFORMA = "android-kotlin"
    }
}
