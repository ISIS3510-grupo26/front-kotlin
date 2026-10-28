package com.example.frontend.model

// Un pin del Taste Map. dx y dy son la posicion dentro del mapa (0 a 1) y salen de las coordenadas del
// restaurante, no de valores escritos a mano.
data class MapSpot(
    val spotId: String,
    val emoji: String,
    val name: String,
    val affinity: Int,          // compatibilidad con el perfil de gusto
    val rating: Double,
    val price: String,
    val walkLabel: String,
    val location: String,
    val rank: Int,              // 1 = el mas compatible
    val dx: Float,
    val dy: Float,
    val minutosEspera: Int?,    // ultima espera reciente (BQ4); null si no hay reporte reciente
)
