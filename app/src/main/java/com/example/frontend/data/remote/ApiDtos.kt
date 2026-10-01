package com.example.frontend.data.remote

// Contratos JSON del backend (mismos nombres camelCase que expone la API).

data class MenuItemDto(
    val emoji: String,
    val emojiBackground: String,
    val name: String,
    val description: String,
    val price: String,
    val isStudentPick: Boolean,
)

data class ReviewDto(
    val authorName: String,
    val initials: String,
    val program: String,
    val stars: Int,
    val text: String,
    val dinedAgo: String,
    val helpfulCount: Int,
)

data class SpotDto(
    val id: String,
    val emoji: String,
    val emojiBackground: String,
    val emojiBorder: String,
    val name: String,
    val subtitle: String,
    val rating: Double,
    val price: String,
    val distance: String,
    val walkMinutes: Int,
    val isBudget: Boolean,
    val isVegetarian: Boolean,
    val isHighProtein: Boolean,
    val affinityPercent: Int,
    val category: String,
    val noteLabel: String,
    val note: String,
    val uniCardPerk: String?,
    val totalReviews: Int,
    // Solo vienen en GET /spots/{id}.
    val menu: List<MenuItemDto>? = null,
    val reviews: List<ReviewDto>? = null,
)

data class PageLoadEventDto(
    val eventId: String,
    val screen: String,
    val spotId: String?,
    val durationMs: Long,
    val success: Boolean,
    val httpStatus: Int?,
    val errorType: String?,
    val deviceModel: String,
    val osName: String,
    val osVersion: String,
    val platform: String,
    val appVersion: String,
    val sessionId: String,
    val occurredAt: String,
)

data class PageLoadBatchDto(val events: List<PageLoadEventDto>)

data class IngestResultDto(val accepted: Int, val duplicates: Int)
