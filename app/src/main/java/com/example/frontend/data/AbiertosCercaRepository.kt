package com.example.frontend.data

import android.util.LruCache
import com.example.frontend.data.contexto.ContextoHorario
import com.example.frontend.data.gusto.AlmacenGusto
import com.example.frontend.data.local.AbiertoCercaEntidad
import com.example.frontend.data.local.FavoritosDao
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.NearbyFavoriteDto
import com.example.frontend.data.ubicacion.Ubicacion
import com.example.frontend.model.AbiertoCerca
import com.example.frontend.model.ReglasAbiertosCerca
import java.io.IOException
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class OrigenAbiertos { RED, MEMORIA, COPIA_LOCAL }

// BQ5 (tipo 2): "Which restaurants in the user's favorite list are open now and within a 15 minute walk?".
// El backend (Camilo, Sprint 2) cruza los favoritos con el horario de cada restaurante y la distancia;
// aqui se decide de donde sale la respuesta: memoria, red o la copia de Room.
class AbiertosCercaRepository(
    private val api: CampusBitesApi,
    private val dao: FavoritosDao,
    private val almacenGusto: AlmacenGusto,
    private val contexto: ContextoHorario = ContextoHorario.Dispositivo,
    private val hayRed: () -> Boolean = { Dependencias.monitorConectividad.hayRed.value },
) {
    data class Resultado(val lista: List<AbiertoCerca>, val origen: OrigenAbiertos, val guardadoEn: Long)

    private class Entrada(val resultado: Resultado, val venceEn: Long)

    // Cache LRU en memoria. La clave es usuario + celda de ~110 m (lat/lng con 3 decimales) + los favoritos:
    // si el estudiante no se movio ni cambio sus favoritos, la respuesta es la misma. 4 entradas: los pocos
    // sitios del campus desde donde se consulta en un rato (salon, biblioteca, cafeteria, la entrada).
    // Cada entrada vence a los 3 minutos o cuando cierre el primero de la lista, lo que pase antes: un
    // restaurante que cerro no puede seguir saliendo como "abierto" solo porque estaba en memoria.
    private val cache = LruCache<String, Entrada>(MAX_ENTRADAS)

    suspend fun cargar(
        userId: String,
        ubicacion: Ubicacion,
        favoritos: Set<String>,
        coordenadas: Map<String, Pair<Double, Double>>,
        forzar: Boolean = false,
    ): Resultado {
        val clave = clave(userId, ubicacion, favoritos)
        val ahora = System.currentTimeMillis()
        if (!forzar) {
            cache.get(clave)?.takeIf { ahora < it.venceEn }?.let { return it.resultado.copy(origen = OrigenAbiertos.MEMORIA) }
        }
        // Si el sistema ya sabe que no hay red, no se espera a que la peticion falle por timeout.
        if (!hayRed()) return desdeElCelular(userId, ubicacion, favoritos) ?: throw IOException("sin red")
        val respuesta = try {
            api.favoritosAbiertosCerca(
                userId, ubicacion.lat, ubicacion.lng,
                ReglasAbiertosCerca.MAX_MINUTOS_CAMINANDO, contexto.desfaseZonaHorariaMinutos(),
            )
        } catch (e: IOException) {
            // Sin red (o el backend no contesta): la ultima respuesta, filtrada con la hora de ahora.
            // Un error HTTP no entra aqui: es una falla real y no se disfraza de "offline".
            return desdeElCelular(userId, ubicacion, favoritos) ?: throw e
        }
        val lista = withContext(Dispatchers.Default) {
            respuesta.mapNotNull { it.toModel(coordenadas[it.id], ahora, contexto.desfaseZonaHorariaMinutos()) }
        }
        val resultado = Resultado(lista, OrigenAbiertos.RED, ahora)
        val primerCierre = lista.minOfOrNull { it.cierraEnMillis } ?: Long.MAX_VALUE
        cache.put(clave, Entrada(resultado, minOf(ahora + VIGENCIA_MS, primerCierre)))
        guardarCopia(userId, lista, ahora)
        return resultado
    }

    private suspend fun guardarCopia(userId: String, lista: List<AbiertoCerca>, ahora: Long) = withContext(Dispatchers.IO) {
        // SQLite guarda NaN como NULL y la columna es NOT NULL: los que no tienen coordenadas no se guardan.
        dao.reemplazarAbiertos(lista.filter { !it.lat.isNaN() && !it.lng.isNaN() }.map { it.aEntidad(userId, ahora) })
        almacenGusto.guardarMomentoAbiertos(userId, ahora)
    }

    // null si nunca hubo una respuesta para este usuario en este celular.
    private suspend fun desdeElCelular(userId: String, ubicacion: Ubicacion, favoritos: Set<String>): Resultado? {
        val (guardadoEn, filas) = withContext(Dispatchers.IO) {
            almacenGusto.momentoAbiertos(userId) to dao.abiertosGuardados(userId)
        }
        if (guardadoEn == null) return null
        // Recalcular distancias y filtrar es CPU: va en Default, no en IO ni en Main.
        val lista = withContext(Dispatchers.Default) {
            ReglasAbiertosCerca.filtrarSinRed(filas.map { it.aModelo() }, favoritos, ubicacion.lat, ubicacion.lng, System.currentTimeMillis())
        }
        return Resultado(lista, OrigenAbiertos.COPIA_LOCAL, guardadoEn)
    }

    // Al cerrar sesion: la respuesta dice donde estaba el estudiante y que le gusta.
    suspend fun olvidar() = withContext(Dispatchers.IO) {
        cache.evictAll()
        dao.borrarAbiertos()
        almacenGusto.olvidarMomentoAbiertos()
    }

    private fun clave(userId: String, u: Ubicacion, favoritos: Set<String>) =
        String.format(Locale.US, "%s@%.3f,%.3f#%d", userId, u.lat, u.lng, favoritos.hashCode())

    private companion object {
        const val MAX_ENTRADAS = 4
        const val VIGENCIA_MS = 3 * 60_000L
    }
}

// Las coordenadas vienen del catalogo (la respuesta de la BQ5 no las trae). Sin coordenadas no se podria
// recalcular la distancia sin red, asi que ese restaurante se muestra en linea pero no se guarda.
private fun NearbyFavoriteDto.toModel(coordenada: Pair<Double, Double>?, ahora: Long, desfase: Int) = AbiertoCerca(
    spotId = id,
    nombre = name,
    emoji = emoji,
    lat = coordenada?.first ?: Double.NaN,
    lng = coordenada?.second ?: Double.NaN,
    metros = distanceMeters,
    minutosCaminando = ceil(walkMinutes).toInt().coerceAtLeast(1),
    cierraA = closesAt,
    cierraEnMillis = ReglasAbiertosCerca.cierreEnMillis(closesAt, ahora, desfase),
)

private fun AbiertoCerca.aEntidad(userId: String, guardadoEn: Long) =
    AbiertoCercaEntidad(spotId, userId, nombre, emoji, lat, lng, metros, minutosCaminando, cierraA, cierraEnMillis, guardadoEn)

private fun AbiertoCercaEntidad.aModelo() =
    AbiertoCerca(spotId, nombre, emoji, lat, lng, metros, minutos, cierraA, cierraEnMillis)
