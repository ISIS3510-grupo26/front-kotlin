package com.example.frontend.model

// Actividad de un restaurante en una hora del dia (respuesta de la BQ3).
data class ActividadSitio(
    val rank: Int,
    val spotId: String,
    val name: String,
    val emoji: String,
    val pageViews: Int,   // vistas de la pagina del restaurante
    val searches: Int,    // veces elegido desde el buscador
    val total: Int,       // pageViews + searches: criterio del ranking
)

// Ranking de restaurantes en una hora local concreta.
data class RankingHora(
    val hour: Int,
    val totalPageViews: Int,
    val totalSearches: Int,
    val sitios: List<ActividadSitio>,
) {
    val etiquetaHora: String get() = "%02d:00–%02d:00".format(hour, (hour + 1) % 24)
}

// Respuesta completa de la BQ3: una entrada por hora con actividad (o solo la hora pedida).
data class PopularidadPorHora(
    val days: Int,
    val horas: List<RankingHora>,
)
