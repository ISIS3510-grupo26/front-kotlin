package com.example.frontend.model

import androidx.collection.MutableObjectIntMap

// La lista que pintan For You, Saved y el mapa: el catalogo con el corazon, la compatibilidad real (BQ6)
// y las reseñas que siguen en la cola del celular.
//
// Micro-optimizacion (Sprint 4), siguiendo la clase de performance bugs:
//  - "Avoid creating unnecessary objects inside a loop": antes el mapa spotId -> compatibilidad se armaba
//    de nuevo para CADA restaurante (un getter leido dentro del map), o sea N mapas por calculo.
//    Ahora se arma una sola vez, antes del loop.
//  - "Prefer primitive types over wrappers" y "memory friendly data structures": ese mapa era un
//    Map<String, Int> (un Integer con boxing y un objeto Entry por cada par). MutableObjectIntMap guarda
//    los int en un arreglo plano, sin boxing ni Entry.
//  - "Use indexed loops": se recorre por indice, sin crear un Iterator, y el resultado se crea ya con su
//    tamaño final para que el ArrayList no crezca y copie su arreglo interno.
//  - Antes se hacian dos copy() por restaurante cuando habia reseñas locales; ahora uno solo.
fun armarSpotsParaUi(
    catalogo: List<Spot>,
    guardados: Set<String>,
    ranking: List<SitioMejorParaTi>,
    perfil: TasteProfile,
    resenasLocales: Map<String, List<PeerReview>>,
): List<Spot> {
    val compatibilidades = MutableObjectIntMap<String>(ranking.size)
    for (i in ranking.indices) {
        val sitio = ranking[i]
        compatibilidades[sitio.candidato.spotId] = sitio.compatibilidad
    }
    val resultado = ArrayList<Spot>(catalogo.size)
    for (i in catalogo.indices) {
        val spot = catalogo[i]
        val propias = resenasLocales[spot.id]
        resultado.add(
            spot.copy(
                isSaved = spot.id in guardados,
                // getOrElse es inline: la lambda no crea un objeto.
                affinityPercent = compatibilidades.getOrElse(spot.id) { perfil.compatibilidad(spot) },
                reviews = if (propias.isNullOrEmpty()) spot.reviews else propias + spot.reviews,
                totalReviews = spot.totalReviews + (propias?.size ?: 0),
            )
        )
    }
    return resultado
}
