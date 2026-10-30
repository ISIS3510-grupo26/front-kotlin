package com.example.frontend.model

// Un pin del Taste Map. lat y lng son las coordenadas reales del restaurante (backend o catalogo
// empaquetado); el mapa de OpenStreetMap lo dibuja en su sitio dentro de Uniandes.
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
    val lat: Double,
    val lng: Double,
    val minutosEspera: Int?,    // ultima espera reciente (BQ4); null si no hay reporte reciente
)
