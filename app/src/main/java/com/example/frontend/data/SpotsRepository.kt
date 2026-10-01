package com.example.frontend.data

import androidx.compose.ui.graphics.Color
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.MenuItemDto
import com.example.frontend.data.remote.ReviewDto
import com.example.frontend.data.remote.SpotDto
import com.example.frontend.model.MenuItem
import com.example.frontend.model.PeerReview
import com.example.frontend.model.Spot
import com.example.frontend.model.SpotCategory
import kotlin.coroutines.cancellation.CancellationException

// Unica puerta de la UI hacia los datos: decide si vienen de la red o del respaldo local.
class SpotsRepository(private val api: CampusBitesApi) {

    data class ResultadoLista(val spots: List<Spot>, val desdeRespaldo: Boolean)

    // Si el backend no responde, la app sigue usable con el catalogo empaquetado.
    suspend fun listarSitios(): ResultadoLista = try {
        ResultadoLista(api.listarSitios().map { it.toModel() }, desdeRespaldo = false)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoLista(sampleSpots.map { it.copy(isSaved = false) }, desdeRespaldo = true)
    }

    // Sin respaldo: el fallo se propaga para que la UI lo muestre y la telemetria lo cuente.
    suspend fun obtenerSitio(id: String): Spot = api.obtenerSitio(id).toModel()
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
