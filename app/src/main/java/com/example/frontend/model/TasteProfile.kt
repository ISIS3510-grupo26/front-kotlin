package com.example.frontend.model

data class TasteProfile(
    val faculty: String = "Engineering & Tech",
    val budget: String = "Budget-friendly",
    val dietary: Set<String> = setOf("Vegetarian"),
    val cuisines: Set<String> = setOf("Artisan Burgers", "Crepas & Bowls"),
    val spice: String = "Medium Heat",
) {
    val chips: List<String>
        get() = buildList {
            add(faculty)
            add(budget)
            addAll(dietary)
            addAll(cuisines)
            add(spice)
        }

    val prefiereVegetariano: Boolean get() = dietary.contains("Vegan") || dietary.contains("Vegetarian")
    val prefiereBarato: Boolean get() = budget == "Budget-friendly"

    fun puntaje(spot: Spot): Int {
        var score = spot.affinityPercent
        if (prefiereVegetariano && spot.isVegetarian) score += 12
        if (prefiereBarato && spot.isBudget) score += 8
        if (cuisines.any { spot.cuisine.contains(it.substringAfterLast(' '), ignoreCase = true) }) score += 5
        return score
    }
}
