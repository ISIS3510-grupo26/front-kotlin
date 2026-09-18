package com.example.frontend.model

import androidx.compose.ui.graphics.Color

enum class SpotCategory(val label: String) {
    FOOD_TRUCKS("Food Trucks"),
    STUDY_SPOTS("Study Spots"),
}

enum class FeedFilterType(val label: String) {
    IN_A_RUSH("⚡ In a rush (<10m)"),
    BUDGET("\$ Budget"),
    SHORT_WALK("≤ 5 min walk"),
    VEGETARIAN("Vegetarian"),
    HIGH_PROTEIN("High Protein"),
}

data class MenuItem(
    val emoji: String,
    val emojiBackground: Color,
    val name: String,
    val description: String,
    val price: String,
    val isStudentPick: Boolean = false,
)

data class PeerReview(
    val authorName: String,
    val initials: String,
    val program: String,
    val stars: Int,
    val text: String,
    val dinedAgo: String,
    val helpfulCount: Int,
)

/**
 * A single campus food spot. This is the one model behind every card in the
 * app: the For You feed and Saved Places both render the same [Spot]s, just
 * filtered differently. `isSaved` is the source of truth for whether a spot
 * shows up in Saved Places, and is toggled by the heart button on the card
 * regardless of which screen it's tapped from.
 */
data class Spot(
    val id: String,
    val emoji: String,
    val emojiBackground: Color,
    val emojiBorder: Color,
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
    val category: SpotCategory,
    val noteLabel: String,
    val note: String,
    val isSaved: Boolean = false,
    val uniCardPerk: String? = null,
    val menu: List<MenuItem> = emptyList(),
    val reviews: List<PeerReview> = emptyList(),
    val totalReviews: Int = 0,
) {
    val cuisine: String get() = subtitle.split(" • ").first()
    val location: String get() = subtitle.split(" • ").last()

    fun matchesFilter(filter: FeedFilterType): Boolean = when (filter) {
        FeedFilterType.IN_A_RUSH -> walkMinutes < 10
        FeedFilterType.BUDGET -> isBudget
        FeedFilterType.SHORT_WALK -> walkMinutes <= 5
        FeedFilterType.VEGETARIAN -> isVegetarian
        FeedFilterType.HIGH_PROTEIN -> isHighProtein
    }
}
