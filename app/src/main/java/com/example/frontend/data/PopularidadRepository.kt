package com.example.frontend.data

import android.os.SystemClock
import com.example.frontend.data.contexto.ContextoHorario
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.SpotViewsByHourDto
import com.example.frontend.model.ActividadSitio
import com.example.frontend.model.PopularidadPorHora
import com.example.frontend.model.RankingHora
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Repository de la BQ3 ("vistas + busquedas por hora"). Es la unica puerta de la UI hacia el endpoint
// de analitica y aplica la tactica cache-aside: cada respuesta se guarda unos minutos en memoria para
// no pegarle al backend cada vez que el usuario vuelve a "For You".
class PopularidadRepository(
    private val api: CampusBitesApi,
    private val contexto: ContextoHorario = ContextoHorario.Dispositivo,
    private val ttlMs: Long = 5 * 60_000L,
) {
    private class Entrada(val valor: PopularidadPorHora, val guardadoEn: Long)

    private val lock = Mutex()
    private val cache = HashMap<String, Entrada>()

    // Ranking de la hora en la que esta el celular AHORA (contexto). Devuelve una sola hora.
    suspend fun rankingAhora(forzar: Boolean = false): RankingHora {
        val hora = contexto.horaLocal()
        val datos = obtener(clave = "hora-$hora", forzar = forzar) {
            api.vistasPorHora(hour = hora, tzOffsetMinutes = contexto.desfaseZonaHorariaMinutos())
        }
        return datos.horas.firstOrNull() ?: RankingHora(hora, 0, 0, emptyList())
    }

    // Las 24 horas (las que tengan actividad), para la pantalla "Popular by hour".
    suspend fun rankingPorHoras(forzar: Boolean = false): PopularidadPorHora =
        obtener(clave = "todas", forzar = forzar) {
            api.vistasPorHora(hour = null, tzOffsetMinutes = contexto.desfaseZonaHorariaMinutos())
        }

    private suspend fun obtener(clave: String, forzar: Boolean, pedir: suspend () -> SpotViewsByHourDto): PopularidadPorHora {
        val ahora = SystemClock.elapsedRealtime()
        if (!forzar) {
            lock.withLock { cache[clave] }?.let { if (ahora - it.guardadoEn < ttlMs) return it.valor }
        }
        val fresco = pedir().toModel()
        lock.withLock { cache[clave] = Entrada(fresco, ahora) }
        return fresco
    }

    suspend fun limpiarCache() = lock.withLock { cache.clear() }
}

private fun SpotViewsByHourDto.toModel() = PopularidadPorHora(
    days = days,
    horas = hours.map { h ->
        RankingHora(
            hour = h.hour,
            totalPageViews = h.totalPageViews,
            totalSearches = h.totalSearches,
            sitios = h.spots.map {
                ActividadSitio(
                    rank = it.rank,
                    spotId = it.spotId,
                    name = it.name,
                    emoji = it.emoji,
                    pageViews = it.pageViews,
                    searches = it.searches,
                    total = it.total,
                )
            },
        )
    },
)
