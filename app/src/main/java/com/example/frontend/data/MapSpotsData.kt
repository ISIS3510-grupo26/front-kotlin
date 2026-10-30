package com.example.frontend.data

import com.example.frontend.model.MapSpot
import com.example.frontend.model.Spot
import com.example.frontend.model.TiempoEspera
import kotlin.math.abs

// Antes los pines eran una lista escrita a mano: posiciones inventadas y dos restaurantes ("Taco Fiesta
// Truck", "Ramen Corner") que no existen en el catalogo, asi que tocarlos no abria nada. Ahora salen del
// mismo catalogo que el feed (red, copia de Room o catalogo empaquetado), por eso el mapa abre sin red.

// Margenes del area donde caen los pines (fraccion del mapa). Abajo se deja espacio para la tarjeta del
// lugar seleccionado, que tapa mas o menos el ultimo tercio.
private const val MARGEN_X = 0.12f
private const val ARRIBA = 0.12f
private const val ABAJO = 0.62f

// Un restaurante con estas coordenadas no se puede dibujar: falta el dato, viene en 0,0 (valor por
// defecto de un dato mal cargado) o esta fuera de rango. Son los casos que la BQ14 cuenta como invalid_coordinates.
fun coordenadaValida(lat: Double?, lng: Double?): Boolean =
    lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0 && !(abs(lat) < 1e-6 && abs(lng) < 1e-6)

// Proyecta lat/lng al rectangulo del mapa: el norte queda arriba y el oeste a la izquierda. El campus mide
// unos cientos de metros, asi que una proyeccion lineal sobre la caja que encierra a todos alcanza.
fun pinesDelMapa(spots: List<Spot>, tiempos: Map<String, TiempoEspera>): List<MapSpot> {
    val ubicados = spots.filter { coordenadaValida(it.latitude, it.longitude) }
    if (ubicados.isEmpty()) return emptyList()

    val minLat = ubicados.minOf { it.latitude!! }
    val maxLat = ubicados.maxOf { it.latitude!! }
    val minLng = ubicados.minOf { it.longitude!! }
    val maxLng = ubicados.maxOf { it.longitude!! }
    // Con un solo restaurante (o todos en el mismo punto) la caja mide 0: se evita dividir por cero.
    val altoLat = (maxLat - minLat).coerceAtLeast(1e-4)
    val anchoLng = (maxLng - minLng).coerceAtLeast(1e-4)

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
                dx = MARGEN_X + ((spot.longitude!! - minLng) / anchoLng).toFloat() * (1 - 2 * MARGEN_X),
                dy = ARRIBA + ((maxLat - spot.latitude!!) / altoLat).toFloat() * (ABAJO - ARRIBA),
                minutosEspera = tiempo?.minutos?.takeIf { tiempo.esReciente() },
            )
        }
}
