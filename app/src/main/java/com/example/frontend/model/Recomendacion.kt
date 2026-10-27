package com.example.frontend.model

// BQ3: un restaurante cercano, bien calificado, que el estudiante todavia no ha probado.
data class Recomendacion(
    val id: String,
    val nombre: String,
    val emoji: String,
    val rating: Double,
    val metros: Int,
    val minutosCaminando: Int,
)
