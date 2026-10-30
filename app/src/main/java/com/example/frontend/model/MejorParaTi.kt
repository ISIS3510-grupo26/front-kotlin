package com.example.frontend.model

import kotlin.math.roundToInt

// BQ6: un restaurante con su ultima espera reportada (datos de la BQ4). Todavia sin perfil de gusto.
data class CandidatoMejorParaTi(
    val spotId: String,
    val nombre: String,
    val emoji: String,
    val cocina: String,
    val rating: Double,
    val esBarato: Boolean,
    val esVegetariano: Boolean,
    val minutosEspera: Int?,   // null = nadie ha reportado
    val minutosAtras: Int?,
    val esReciente: Boolean,   // si es viejo, la espera no cuenta en el puntaje
)

// Un restaurante ya rankeado para el perfil del estudiante.
data class SitioMejorParaTi(
    val candidato: CandidatoMejorParaTi,
    val compatibilidad: Int,   // 0-100 con el perfil de gusto
    val puntajeEspera: Int,    // 0-100: menos espera, mas puntos
    val puntaje: Double,       // mezcla de los dos; criterio del ranking
    val rank: Int,
) {
    val esperaConocida: Int? get() = candidato.minutosEspera?.takeIf { candidato.esReciente }
}

// Misma formula que el backend (app/services/best_for_you_service.py). Se repite aqui porque el perfil
// no sale del celular: el backend manda las esperas y el ranking se arma en el telefono.
object FormulaMejorParaTi {
    const val MAX_MINUTOS_ESPERA = 40
    const val PESO_ESPERA = 0.5

    fun puntajeEspera(minutos: Int?, esReciente: Boolean): Int =
        if (minutos == null || !esReciente) TasteProfile.NEUTRO
        else (100 * (1 - minutos / MAX_MINUTOS_ESPERA.toDouble())).roundToInt().coerceAtLeast(0)
}

// Trabajo de CPU puro (sin red ni disco): el ViewModel lo corre en Dispatchers.Default.
// Orden: mayor puntaje; si empatan, la espera conocida mas corta y despues el mejor calificado.
fun rankearMejorParaTi(candidatos: List<CandidatoMejorParaTi>, perfil: TasteProfile): List<SitioMejorParaTi> =
    candidatos
        .map { c ->
            val compatibilidad = perfil.compatibilidad(c.cocina, c.nombre, c.esVegetariano, c.esBarato)
            val espera = FormulaMejorParaTi.puntajeEspera(c.minutosEspera, c.esReciente)
            val puntaje = FormulaMejorParaTi.PESO_ESPERA * espera + (1 - FormulaMejorParaTi.PESO_ESPERA) * compatibilidad
            SitioMejorParaTi(c, compatibilidad, espera, puntaje, rank = 0)
        }
        .sortedWith(
            compareByDescending<SitioMejorParaTi> { it.puntaje }
                .thenBy { it.esperaConocida ?: Int.MAX_VALUE }
                .thenByDescending { it.candidato.rating }
                .thenBy { it.candidato.spotId },
        )
        .mapIndexed { i, sitio -> sitio.copy(rank = i + 1) }
