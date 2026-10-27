package com.example.frontend.data

import com.example.frontend.data.local.CatalogoDao
import com.example.frontend.data.local.PendientesDao
import com.example.frontend.data.local.ReporteEsperaPendienteEntidad
import com.example.frontend.data.local.TiempoEsperaEntidad
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.LastWaitTimeDto
import com.example.frontend.model.TiempoEspera
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

// BQ4 (tipo 2): "What is the last reported waiting time for each restaurant?".
// Lee el ultimo tiempo de cada restaurante y guarda los reportes del usuario, aunque no haya red.
class TiemposEsperaRepository(
    private val api: CampusBitesApi,
    private val catalogo: CatalogoDao = Dependencias.baseDatos.catalogoDao(),
    private val pendientes: PendientesDao = Dependencias.baseDatos.pendientesDao(),
    private val alEncolar: () -> Unit = Dependencias::programarSincronizacion,
) {
    data class Resultado(val tiempos: Map<String, TiempoEspera>, val desdeCopiaLocal: Boolean)

    suspend fun cargar(): Resultado {
        val desdeRed = try {
            api.ultimosTiemposEspera().spots.map { it.toEntidad() }.also { catalogo.guardarTiemposEspera(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        val tiempos = (desdeRed ?: catalogo.tiemposEspera()).associate { it.spotId to it.toModel() }
        return Resultado(conReportesPendientes(tiempos), desdeCopiaLocal = desdeRed == null)
    }

    // El reporte siempre pasa primero por la base local: si no hay red no se pierde, y TrabajoSincronizacion
    // lo envia despues con su hora real. El reportId evita que un reenvio cuente doble en el backend.
    suspend fun reportar(spotId: String, minutos: Int): TiempoEspera {
        val reporte = ReporteEsperaPendienteEntidad(UUID.randomUUID().toString(), spotId, minutos, System.currentTimeMillis())
        pendientes.encolarReporte(reporte)
        alEncolar()
        return reporte.toModel()
    }

    // Un reporte que todavia no llega al backend es mas nuevo que lo que el backend sabe: se muestra ese.
    private suspend fun conReportesPendientes(tiempos: Map<String, TiempoEspera>): Map<String, TiempoEspera> {
        val resultado = tiempos.toMutableMap()
        for (reporte in pendientes.reportes()) {
            val actual = resultado[reporte.spotId]
            if (actual?.reportadoEn == null || reporte.reportadoEn > actual.reportadoEn) {
                resultado[reporte.spotId] = reporte.toModel().copy(reportesRecientes = (actual?.reportesRecientes ?: 0) + 1)
            }
        }
        return resultado
    }
}

private fun LastWaitTimeDto.toEntidad() = TiempoEsperaEntidad(
    spotId = spotId,
    minutos = minutes,
    reportadoEn = desdeIso8601(reportedAt),
    reportesRecientes = recentReports,
)

private fun TiempoEsperaEntidad.toModel() = TiempoEspera(
    spotId = spotId,
    minutos = minutos,
    reportadoEn = reportadoEn,
    reportesRecientes = reportesRecientes,
)

private fun ReporteEsperaPendienteEntidad.toModel() = TiempoEspera(
    spotId = spotId,
    minutos = minutos,
    reportadoEn = reportadoEn,
    reportesRecientes = 1,
    pendienteDeEnvio = true,
)
