package com.example.frontend.model

data class MapSpot(
    val emoji: String,
    val name: String,
    val affinity: Int,
    val rating: Double,
    val price: String,
    val walkLabel: String,
    val location: String,
    val rank: Int,
    val dx: Float,
    val dy: Float,
)
