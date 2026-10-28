package com.example.frontend.data

import com.example.frontend.data.remote.BestForYouSpotDto
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.model.CandidatoMejorParaTi
import com.example.frontend.model.Spot
import com.example.frontend.model.TiempoEspera
import kotlin.coroutines.cancellation.CancellationException

enum class OrigenMejorParaTi { RED, COPIA_LOCAL }

// BQ6 (tipo 2): "restaurantes con menor tiempo de espera y mayor compatibilidad con mis gustos".
// Este repository solo trae los candidatos con su espera; el gusto lo pone el ViewModel con el perfil
// que esta en DataStore. Asi el perfil nunca viaja al backend y la seccion sigue funcionando sin red.
class MejorParaTiRepository(
    private val api: CampusBitesApi,
    private val hayRed: () -> Boolean = { Dependencias.monitorConectividad.hayRed.value },
) {

    data class Resultado(val candidatos: List<CandidatoMejorParaTi>, val origen: OrigenMejorParaTi)

    // Network first. Sin red se arma lo mismo con el catalogo y los tiempos de espera que ya estan en el
    // celular (Room, BQ4), asi que el ranking se puede calcular aunque el backend no responda.
    suspend fun candidatos(catalogo: List<Spot>, tiemposLocales: Map<String, TiempoEspera>): Resultado {
        // Si el sistema ya dice que no hay red, no vale la pena esperar a que la peticion falle.
        if (!hayRed()) return desdeElCelular(catalogo, tiemposLocales)
        return try {
            val desdeRed = api.mejorParaTi().spots.map { it.toModel() }
            Resultado(desdeRed.map { it.conReportePropio(tiemposLocales[it.spotId]) }, OrigenMejorParaTi.RED)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            desdeElCelular(catalogo, tiemposLocales)
        }
    }

    private fun desdeElCelular(catalogo: List<Spot>, tiemposLocales: Map<String, TiempoEspera>) =
        Resultado(catalogo.map { it.aCandidato(tiemposLocales[it.id]) }, OrigenMejorParaTi.COPIA_LOCAL)
}

// Un reporte hecho en este celular que todavia no llega al backend es mas nuevo que lo que el backend sabe.
private fun CandidatoMejorParaTi.conReportePropio(propio: TiempoEspera?): CandidatoMejorParaTi =
    if (propio?.pendienteDeEnvio == true) {
        copy(minutosEspera = propio.minutos, minutosAtras = propio.minutosAtras(), esReciente = propio.esReciente())
    } else {
        this
    }

private fun BestForYouSpotDto.toModel() = CandidatoMejorParaTi(
    spotId = spotId,
    nombre = name,
    emoji = emoji,
    cocina = cuisine,
    rating = rating,
    esBarato = isBudget,
    esVegetariano = isVegetarian,
    minutosEspera = waitMinutes,
    minutosAtras = minutesAgo,
    esReciente = isRecent,
)

private fun Spot.aCandidato(tiempo: TiempoEspera?) = CandidatoMejorParaTi(
    spotId = id,
    nombre = name,
    emoji = emoji,
    cocina = cuisine,
    rating = rating,
    esBarato = isBudget,
    esVegetariano = isVegetarian,
    minutosEspera = tiempo?.minutos,
    minutosAtras = tiempo?.minutosAtras(),
    esReciente = tiempo?.esReciente() ?: false,
)
