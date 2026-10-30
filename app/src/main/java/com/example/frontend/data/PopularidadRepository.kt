package com.example.frontend.data

import android.os.SystemClock
import android.util.LruCache
import com.example.frontend.data.contexto.ContextoHorario
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.SpotViewsByHourDto
import com.example.frontend.model.ActividadSitio
import com.example.frontend.model.PopularidadPorHora
import com.example.frontend.model.RankingHora
import com.google.gson.Gson
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Repository de la BQ3 ("vistas + busquedas por hora"). Es la unica puerta de la UI hacia el endpoint
// de analitica y aplica la tactica cache-aside: cada respuesta se guarda unos minutos en memoria para
// no pegarle al backend cada vez que el usuario vuelve a "For You".
class PopularidadRepository(
    private val api: CampusBitesApi,
    private val contexto: ContextoHorario = ContextoHorario.Dispositivo,
    private val ttlMs: Long = 5 * 60_000L,
    carpeta: File = Dependencias.carpetaArchivos,
) {
    private class Entrada(val valor: PopularidadPorHora, val guardadoEn: Long)

    // Lo que se escribe en el archivo: la ultima respuesta buena de "todas las horas" y cuando llego.
    private class Guardado(val guardadoEn: Long, val respuesta: SpotViewsByHourDto)

    // LruCache en lugar del HashMap que habia. Las claves posibles son 25 ("hora-0" a "hora-23" y "todas"),
    // y el HashMap nunca soltaba ninguna: cada hora que pasaba con la app abierta dejaba una entrada vencida
    // en memoria. Tamaño 3: en un momento dado solo se usan 2 claves, la hora actual (seccion "Popular right
    // now") y "todas" (pantalla "Popular by hour"). La tercera es margen para el cambio de hora: cuando entra
    // "hora-14", la que sale es "hora-13", que ya nadie pide y por eso es la menos usada, y no "todas".
    // Con tamaño 2, si el estudiante miro el feed despues de la pantalla, saldria "todas" y la hora vieja
    // se quedaria ocupando el cupo. Cada entrada pesa pocos KB (24 horas x 5 restaurantes como mucho).
    // El TTL sigue igual: el LRU limita cuantas entradas hay, el TTL decide si una entrada sigue fresca.
    // LruCache ya es thread-safe (sus metodos son synchronized), asi que el Mutex sobraba.
    private val cache = LruCache<String, Entrada>(MAX_ENTRADAS)

    private val gson = Gson()
    private val archivo = File(carpeta, "popularidad_por_hora.json")

    // Ranking de la hora en la que esta el celular AHORA (contexto). Devuelve una sola hora.
    suspend fun rankingAhora(forzar: Boolean = false): RankingHora {
        val hora = contexto.horaLocal()
        val datos = obtener(clave = "hora-$hora", forzar = forzar) {
            api.vistasPorHora(hour = hora, tzOffsetMinutes = contexto.desfaseZonaHorariaMinutos())
        }
        return datos.horas.firstOrNull() ?: RankingHora(hora, 0, 0, emptyList())
    }

    // Sin red: la hora actual sacada del ultimo ranking guardado, y cuando se guardo. null si nunca hubo uno.
    // Con esto "Popular right now" sigue en el feed offline y deja entrar a "Popular by hour".
    suspend fun rankingAhoraGuardado(): Pair<RankingHora, Long>? {
        val datos = leerArchivo() ?: return null
        val hora = contexto.horaLocal()
        return (datos.horas.firstOrNull { it.hour == hora } ?: RankingHora(hora, 0, 0, emptyList())) to datos.guardadoEn!!
    }

    // Las 24 horas (las que tengan actividad), para la pantalla "Popular by hour". Cada respuesta buena
    // se guarda en un archivo: sin red la pantalla muestra ese ultimo ranking (con guardadoEn) en vez de un error.
    suspend fun rankingPorHoras(forzar: Boolean = false): PopularidadPorHora = try {
        obtener(clave = "todas", forzar = forzar) {
            api.vistasPorHora(hour = null, tzOffsetMinutes = contexto.desfaseZonaHorariaMinutos())
                .also { escribirArchivo(Guardado(System.currentTimeMillis(), it)) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        leerArchivo() ?: throw e
    }

    private suspend fun obtener(clave: String, forzar: Boolean, pedir: suspend () -> SpotViewsByHourDto): PopularidadPorHora {
        val ahora = SystemClock.elapsedRealtime()
        if (!forzar) {
            cache.get(clave)?.let { if (ahora - it.guardadoEn < ttlMs) return it.valor }
        }
        val fresco = pedir().toModel()
        cache.put(clave, Entrada(fresco, ahora))
        return fresco
    }

    fun limpiarCache() = cache.evictAll()

    // Archivo local: sobrevive a cerrar la app. Se lee y escribe en un hilo de I/O, nunca en el de la UI.
    private suspend fun leerArchivo(): PopularidadPorHora? = withContext(Dispatchers.IO) {
        if (!archivo.exists()) return@withContext null
        try {
            val guardado = gson.fromJson(archivo.readText(), Guardado::class.java)
            guardado.respuesta.toModel().copy(guardadoEn = guardado.guardadoEn)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun escribirArchivo(guardado: Guardado) = withContext(Dispatchers.IO) {
        archivo.writeText(gson.toJson(guardado))
    }

    private companion object {
        const val MAX_ENTRADAS = 3
    }
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
