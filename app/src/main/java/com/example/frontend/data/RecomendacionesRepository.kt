package com.example.frontend.data

import android.util.LruCache
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.NearbyPickDto
import com.example.frontend.data.ubicacion.Ubicacion
import com.example.frontend.model.Recomendacion
import com.google.gson.Gson
import java.io.File
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class OrigenRecomendaciones { RED, MEMORIA, ARCHIVO }

// BQ3 (tipo 2): "What are the nearest restaurants with the highest average rating that the user hasn't
// tried yet?". El backend (Esteban) hace el filtrado y el orden; aqui se decide de donde salen los datos.
class RecomendacionesRepository(
    private val api: CampusBitesApi,
    carpeta: File = Dependencias.carpetaArchivos,
) {
    data class Resultado(
        val lista: List<Recomendacion>,
        val origen: OrigenRecomendaciones,
        val guardadoEn: Long,
    )

    // Lo que se escribe en el archivo: la ultima respuesta buena, de quien y cuando.
    private data class Guardado(val userId: String, val guardadoEn: Long, val picks: List<NearbyPickDto>)

    private val gson = Gson()
    private val archivo = File(carpeta, "recomendaciones_cercanas.json")

    // Cache LRU en memoria. Clave = usuario + "celda" de ~110 m (lat/lng con 3 decimales): si el estudiante
    // se mueve poco, la respuesta es la misma y no vale la pena pedirla otra vez. 8 entradas alcanzan para
    // los lugares por los que se mueve en un dia (salon, biblioteca, cafeteria); al llenarse sale la menos usada.
    private val enMemoria = LruCache<String, Resultado>(MAX_CELDAS)

    suspend fun cargar(userId: String, ubicacion: Ubicacion, forzar: Boolean = false): Resultado {
        val clave = clave(userId, ubicacion)
        val ahora = System.currentTimeMillis()
        if (!forzar) {
            enMemoria.get(clave)?.takeIf { ahora - it.guardadoEn < VIGENCIA_MS }?.let {
                return it.copy(origen = OrigenRecomendaciones.MEMORIA)
            }
        }
        return try {
            val picks = api.recomendacionesCercanas(userId, ubicacion.lat, ubicacion.lng)
            val resultado = Resultado(picks.map { it.toModel() }, OrigenRecomendaciones.RED, ahora)
            enMemoria.put(clave, resultado)
            escribirArchivo(Guardado(userId, ahora, picks))
            resultado
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red: lo ultimo que se guardo en el archivo, si es de este mismo usuario.
            leerArchivo(userId) ?: throw e
        }
    }

    // Archivo local: sobrevive a cerrar la app. Se lee y escribe en un hilo de I/O, nunca en el de la UI.
    suspend fun leerArchivo(userId: String): Resultado? = withContext(Dispatchers.IO) {
        if (!archivo.exists()) return@withContext null
        val guardado = try {
            gson.fromJson(archivo.readText(), Guardado::class.java)
        } catch (_: Exception) {
            null
        }
        guardado?.takeIf { it.userId == userId }
            ?.let { Resultado(it.picks.map { p -> p.toModel() }, OrigenRecomendaciones.ARCHIVO, it.guardadoEn) }
    }

    private suspend fun escribirArchivo(guardado: Guardado) = withContext(Dispatchers.IO) {
        archivo.writeText(gson.toJson(guardado))
    }

    // Al cerrar sesion se borra: las recomendaciones dicen donde estuvo el usuario y que ha probado.
    suspend fun olvidar() = withContext(Dispatchers.IO) {
        enMemoria.evictAll()
        archivo.delete()
    }

    private fun clave(userId: String, u: Ubicacion) =
        String.format(Locale.US, "%s@%.3f,%.3f", userId, u.lat, u.lng)

    private companion object {
        const val MAX_CELDAS = 8
        const val VIGENCIA_MS = 3 * 60_000L
    }
}

private fun NearbyPickDto.toModel() = Recomendacion(
    id = id,
    nombre = name,
    emoji = emoji,
    rating = rating,
    metros = distanceMeters,
    minutosCaminando = ceil(walkMinutes).toInt().coerceAtLeast(1),
)
