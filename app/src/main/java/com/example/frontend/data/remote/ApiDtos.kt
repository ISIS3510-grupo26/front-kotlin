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
    // Coordenadas para el mapa. Las copias de Room guardadas antes de que el backend las mandara no las
    // traen; Gson deja null y el mapa usa las del catalogo empaquetado.
    val latitude: Double? = null,
    val longitude: Double? = null,
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

// ---------- BQ3: vistas de pagina + busquedas por hora (GET /analytics/spot-views-by-hour) ----------

data class SpotHourlyActivityDto(
    val rank: Int,
    val spotId: String,
    val name: String,
    val emoji: String,
    val pageViews: Int,
    val searches: Int,
    val total: Int,
)

data class HourlyRankingDto(
    val hour: Int,
    val totalPageViews: Int,
    val totalSearches: Int,
    val spots: List<SpotHourlyActivityDto>,
)

data class SpotViewsByHourDto(
    val question: String,
    val days: Int,
    val tzOffsetMinutes: Int,
    val hours: List<HourlyRankingDto>,
)

data class WeekdaySearchCountDto(
    val dayOfWeek: Int, // 0 = Monday ... 6 = Sunday
    val dayName: String,
    val searches: Int,
)

data class SearchesByWeekdayDto(
    val question: String,
    val days: Int,
    val tzOffsetMinutes: Int,
    val totalSearches: Int,
    val byDay: List<WeekdaySearchCountDto>,
)

// ---------- BQ4: tiempos de espera reportados ----------

data class WaitTimeReportDto(
    val reportId: String,
    val minutes: Int,
    val platform: String,
    val reportedAt: String,
)

data class WaitTimeReportResultDto(
    val spotId: String,
    val minutes: Int,
    val reportedAt: String,
    val duplicate: Boolean,
)

data class LastWaitTimeDto(
    val spotId: String,
    val name: String,
    val emoji: String,
    val minutes: Int?,
    val reportedAt: String?,
    val minutesAgo: Int?,
    val isRecent: Boolean,
    val recentReports: Int,
)

data class LastWaitTimesDto(
    val question: String,
    val freshMinutes: Int,
    val spots: List<LastWaitTimeDto>,
)

// ---------- Reseñas del usuario (POST /spots/{id}/reviews) ----------

data class ReviewInDto(
    val stars: Int,
    val text: String,
)

data class ReviewCreatedDto(
    val spotId: String,
    val stars: Int,
    val text: String,
    val rating: Double,
    val totalReviews: Int,
)

// ---------- BQ6: menor espera + compatibilidad con el perfil (GET /recommendations/best-for-you) ----------

data class BestForYouSpotDto(
    val rank: Int,
    val spotId: String,
    val name: String,
    val emoji: String,
    val cuisine: String,
    val rating: Double,
    val isBudget: Boolean,
    val isVegetarian: Boolean,
    val isHighProtein: Boolean,
    val waitMinutes: Int?,
    val minutesAgo: Int?,
    val isRecent: Boolean,
    val recentReports: Int,
    val waitScore: Int,
    val tasteMatch: Int?,
    val score: Double,
)

data class BestForYouDto(
    val question: String,
    val freshMinutes: Int,
    val maxWaitMinutes: Int,
    val personalized: Boolean,
    val spots: List<BestForYouSpotDto>,
)

// ---------- Autenticacion (backend de Camilo: /api/v1/auth) ----------

data class CredencialesDto(
    val email: String,
    val password: String,
)

data class AuthTokenDto(
    val userId: String,
    val email: String,
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val expiresAt: String,
)

data class UsuarioDto(
    val userId: String,
    val email: String,
)

// ---------- BQ3 (backend de Esteban): cercanos mejor calificados que el usuario no ha probado ----------

data class NearbyPickDto(
    val id: String,
    val name: String,
    val emoji: String,
    val rating: Double,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val walkMinutes: Double,
)
