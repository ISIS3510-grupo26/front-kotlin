package com.example.frontend.data

import android.util.LruCache
import androidx.compose.ui.graphics.Color
import com.example.frontend.data.local.CatalogoDao
import com.example.frontend.data.local.DetalleGuardadoEntidad
import com.example.frontend.data.local.SitioGuardadoEntidad
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.MenuItemDto
import com.example.frontend.data.remote.ReviewDto
import com.example.frontend.data.remote.SpotDto
import com.example.frontend.model.MenuItem
import com.example.frontend.model.PeerReview
import com.example.frontend.model.Spot
import com.example.frontend.model.SpotCategory
import com.google.gson.Gson
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// De donde salio lo que se esta mostrando. La UI lo usa para avisar al usuario.
enum class OrigenDatos { RED, COPIA_LOCAL, CATALOGO_EMPAQUETADO }

// Unica puerta de la UI hacia los datos: decide si vienen de la red, de la copia local o del respaldo.
class SpotsRepository(
    private val api: CampusBitesApi,
    private val catalogo: CatalogoDao = Dependencias.baseDatos.catalogoDao(),
) {
    private val gson = Gson()

    // Cache LRU en memoria para el detalle: reabrir un restaurante de esta sesion no lee disco ni JSON.
    // 20 detalles (~1 KB de modelo cada uno) cubren mas de lo que un estudiante abre en una sesion;
    // al pasar de 20 sale el que se uso hace mas tiempo. Orden: memoria -> Room -> red.
    private val detallesEnMemoria = LruCache<String, DetalleGuardado>(MAX_DETALLES_MEMORIA)

    data class ResultadoLista(val spots: List<Spot>, val origen: OrigenDatos, val guardadoEn: Long? = null) {
        val desdeRespaldo: Boolean get() = origen != OrigenDatos.RED
    }

    data class DetalleGuardado(val spot: Spot, val guardadoEn: Long)

    // Network first: siempre se intenta la red, y cada respuesta buena se guarda en Room.
    // Sin red se muestra la ultima copia guardada; el catalogo empaquetado solo queda para
    // la primera vez que se abre la app sin haberse conectado nunca.
    suspend fun listarSitios(): ResultadoLista = try {
        val dtos = api.listarSitios()
        val ahora = System.currentTimeMillis()
        catalogo.reemplazarCatalogo(dtos.mapIndexed { i, dto -> SitioGuardadoEntidad(dto.id, i, gson.toJson(dto), ahora) })
        ResultadoLista(convertir(dtos), OrigenDatos.RED, ahora)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val guardados = catalogo.sitios()
        if (guardados.isNotEmpty()) {
            val dtos = guardados.map { gson.fromJson(it.json, SpotDto::class.java) }
            ResultadoLista(convertir(dtos), OrigenDatos.COPIA_LOCAL, guardados.minOf { it.guardadoEn })
        } else {
            ResultadoLista(sampleSpots.map { it.copy(isSaved = false) }, OrigenDatos.CATALOGO_EMPAQUETADO)
        }
    }

    // El fallo se propaga para que la telemetria lo cuente (BQ2); la UI decide si usa la copia local.
    suspend fun obtenerSitio(id: String): Spot {
        val dto = api.obtenerSitio(id)
        val ahora = System.currentTimeMillis()
        catalogo.guardarDetalle(DetalleGuardadoEntidad(id, gson.toJson(dto), ahora))
        val spot = withContext(Dispatchers.Default) { dto.toModel() }
        detallesEnMemoria.put(id, DetalleGuardado(spot, ahora))
        return spot
    }

    suspend fun detalleGuardado(id: String): DetalleGuardado? =
        detallesEnMemoria.get(id) ?: catalogo.detalle(id)?.let { guardado ->
            val spot = withContext(Dispatchers.Default) { gson.fromJson(guardado.json, SpotDto::class.java).toModel() }
            DetalleGuardado(spot, guardado.guardadoEn).also { detallesEnMemoria.put(id, it) }
        }

    // Convertir JSON a modelos (parsear colores, enums) es trabajo de CPU: va en Default, no en el hilo de la UI.
    private suspend fun convertir(dtos: List<SpotDto>): List<Spot> =
        withContext(Dispatchers.Default) { dtos.map { it.toModel() } }

    private companion object {
        const val MAX_DETALLES_MEMORIA = 20
    }
}

private fun parseColor(hex: String): Color =
    Color(android.graphics.Color.parseColor(hex))

private fun SpotDto.toModel() = Spot(
    id = id,
    emoji = emoji,
    emojiBackground = parseColor(emojiBackground),
    emojiBorder = parseColor(emojiBorder),
    name = name,
    subtitle = subtitle,
    rating = rating,
    price = price,
    distance = distance,
    walkMinutes = walkMinutes,
    isBudget = isBudget,
    isVegetarian = isVegetarian,
    isHighProtein = isHighProtein,
    affinityPercent = affinityPercent,
    category = SpotCategory.valueOf(category),
    noteLabel = noteLabel,
    note = note,
    uniCardPerk = uniCardPerk,
    menu = menu.orEmpty().map { it.toModel() },
    reviews = reviews.orEmpty().map { it.toModel() },
    totalReviews = totalReviews,
)

private fun MenuItemDto.toModel() = MenuItem(
    emoji = emoji,
    emojiBackground = parseColor(emojiBackground),
    name = name,
    description = description,
    price = price,
    isStudentPick = isStudentPick,
)

private fun ReviewDto.toModel() = PeerReview(
    authorName = authorName,
    initials = initials,
    program = program,
    stars = stars,
    text = text,
    dinedAgo = dinedAgo,
    helpfulCount = helpfulCount,
)
