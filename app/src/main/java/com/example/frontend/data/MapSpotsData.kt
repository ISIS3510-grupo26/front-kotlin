package com.example.frontend.data

import com.example.frontend.model.MapSpot
import com.example.frontend.model.Spot
import com.example.frontend.model.TiempoEspera
import kotlin.math.abs

// Antes los pines eran una lista escrita a mano: posiciones inventadas y dos restaurantes ("Taco Fiesta
// Truck", "Ramen Corner") que no existen en el catalogo, asi que tocarlos no abria nada. Ahora salen del
// mismo catalogo que el feed (red, copia de Room o catalogo empaquetado), por eso el mapa abre sin red.

// Un restaurante con estas coordenadas no se puede dibujar: falta el dato, viene en 0,0 (valor por
// defecto de un dato mal cargado) o esta fuera de rango. Son los casos que la BQ14 cuenta como invalid_coordinates.
fun coordenadaValida(lat: Double?, lng: Double?): Boolean =
    lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0 && !(abs(lat) < 1e-6 && abs(lng) < 1e-6)

// Pines ordenados por compatibilidad: el #1 es el que mejor encaja con el perfil de gusto.
fun pinesDelMapa(spots: List<Spot>, tiempos: Map<String, TiempoEspera>): List<MapSpot> {
    val ubicados = spots.filter { coordenadaValida(it.latitude, it.longitude) }

    return ubicados
        .sortedWith(compareByDescending<Spot> { it.affinityPercent }.thenByDescending { it.rating })
        .mapIndexed { i, spot ->
            val tiempo = tiempos[spot.id]
            MapSpot(
                spotId = spot.id,
                emoji = spot.emoji,
                name = spot.name,
                affinity = spot.affinityPercent,
                rating = spot.rating,
                price = spot.price,
                walkLabel = spot.distance,
                location = spot.location,
                rank = i + 1,
                lat = spot.latitude!!,
                lng = spot.longitude!!,
                minutosEspera = tiempo?.minutos?.takeIf { tiempo.esReciente() },
            )
        }
}
