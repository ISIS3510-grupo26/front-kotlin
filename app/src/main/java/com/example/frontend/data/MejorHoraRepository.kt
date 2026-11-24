package com.example.frontend.data

import android.util.LruCache
import com.example.frontend.data.contexto.ContextoHorario
import com.example.frontend.data.local.EsperaPorHoraEntidad
import com.example.frontend.data.local.EsperasPorHoraDao
import com.example.frontend.data.remote.BestTimeToGoDto
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.model.ReglasMejorHora
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

enum class OrigenMejorHora { RED, MEMORIA, COPIA_LOCAL }

// BQ8 "Best time to go" (Sprint 4, Tomas). Decide de donde sale la respuesta: memoria, red o Room.
// Devuelve las filas crudas (restaurante + hora + promedio); la mejor hora se calcula en el ViewModel
// con ReglasMejorHora, asi el calculo es el mismo con red y sin red.
class MejorHoraRepository(
    private val api: CampusBitesApi,
    private val dao: EsperasPorHoraDao,
    private val contexto: ContextoHorario = ContextoHorario.Dispositivo,
    private val hayRed: () -> Boolean = { Dependencias.monitorConectividad.hayRed.value },
) {
    data class Resultado(val filas: List<ReglasMejorHora.Fila>, val origen: OrigenMejorHora, val guardadoEn: Long)

    private class Entrada(val resultado: Resultado, val venceEn: Long)

    // Cache LRU en memoria, una entrada por ventana ("7 dias" y "28 dias" en la pantalla) y zona horaria
    // (las horas dependen de ella). Tamaño 2 porque en uso normal solo existen esas dos claves: cambiar de
    // pestaña no vuelve a pedir nada, y si el celular cambia de zona la clave vieja es la que sale.
    // Vence a los 10 minutos: los promedios de 7 o 28 dias casi no cambian en ese tiempo, pero un reporte
    // nuevo debe llegar a verse.
    private val cache = LruCache<String, Entrada>(MAX_ENTRADAS)

    suspend fun cargar(ventanaDias: Int, forzar: Boolean = false): Resultado {
        val ahora = System.currentTimeMillis()
        val desfase = contexto.desfaseZonaHorariaMinutos()
        val clave = "$ventanaDias@$desfase"
        if (!forzar) {
            cache.get(clave)?.takeIf { ahora < it.venceEn }?.let { return it.resultado.copy(origen = OrigenMejorHora.MEMORIA) }
        }
        // Sin red segun el sistema: directo a la copia, sin esperar el timeout de la peticion.
        if (!hayRed()) return desdeElCelular(ventanaDias) ?: throw IOException("sin red y sin copia")

        // Corrutinas anidadas en IO: la peticion y la lectura de la copia de Room arrancan a la vez.
        // Si la red falla, la copia ya esta leida y la pantalla no espera una segunda lectura.
        return coroutineScope {
            val deLaRed = async(Dispatchers.IO) {
                runCatching { api.mejorHoraParaIr(ventanaDias, desfase) }
            }
            val copia = async(Dispatchers.IO) { dao.filas(ventanaDias) }
            val respuesta = deLaRed.await()
            respuesta.exceptionOrNull()?.let { error ->
                if (error is CancellationException) throw error
                // Solo una falla de red usa la copia. Un error HTTP (500) es una falla real y se muestra.
                if (error !is IOException) throw error
                val guardadas = copia.await()
                if (guardadas.isEmpty()) throw error
                return@coroutineScope Resultado(guardadas.map { it.aFila() }, OrigenMejorHora.COPIA_LOCAL, guardadas.maxOf { it.guardadoEn })
            }
            copia.cancel()
            val filas = withContext(Dispatchers.Default) { respuesta.getOrThrow().aFilas() }
            val resultado = Resultado(filas, OrigenMejorHora.RED, ahora)
            cache.put(clave, Entrada(resultado, ahora + VIGENCIA_MS))
            withContext(Dispatchers.IO) { dao.reemplazar(ventanaDias, filas.map { it.aEntidad(ventanaDias, ahora) }) }
            resultado
        }
    }

    // null si nunca se guardo esta ventana en este celular.
    private suspend fun desdeElCelular(ventanaDias: Int): Resultado? {
        val guardadas = withContext(Dispatchers.IO) { dao.filas(ventanaDias) }
        if (guardadas.isEmpty()) return null
        return Resultado(guardadas.map { it.aFila() }, OrigenMejorHora.COPIA_LOCAL, guardadas.maxOf { it.guardadoEn })
    }

    private companion object {
        const val MAX_ENTRADAS = 2
        const val VIGENCIA_MS = 10 * 60_000L
    }
}

private fun BestTimeToGoDto.aFilas(): List<ReglasMejorHora.Fila> {
    val filas = ArrayList<ReglasMejorHora.Fila>()
    for (h in byHour) filas.add(ReglasMejorHora.Fila(ReglasMejorHora.CAMPUS, "Campus", "🏫", h.hour, h.avgMinutes, h.reports))
    for (spot in spots) {
        for (h in spot.byHour) filas.add(ReglasMejorHora.Fila(spot.spotId, spot.name, spot.emoji, h.hour, h.avgMinutes, h.reports))
    }
    return filas
}

private fun ReglasMejorHora.Fila.aEntidad(ventanaDias: Int, guardadoEn: Long) =
    EsperaPorHoraEntidad(ventanaDias, spotId, nombre, emoji, hora, promedio, reportes, guardadoEn)

private fun EsperaPorHoraEntidad.aFila() = ReglasMejorHora.Fila(spotId, nombre, emoji, hora, promedioMinutos, reportes)
