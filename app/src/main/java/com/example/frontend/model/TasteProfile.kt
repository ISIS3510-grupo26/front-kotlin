package com.example.frontend.model

import kotlin.math.roundToInt

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

    // Compatibilidad de 0 a 100 con un restaurante. Reemplaza el "Taste Match" que venia escrito a mano en el
    // catalogo (affinityPercent): ahora sale de lo que el estudiante eligio en Taste Profile.
    // Es la misma formula de taste_match en el backend (app/services/best_for_you_service.py): solo cuentan
    // las partes del perfil que aplican. Cocina 50, dieta 30 y presupuesto 20 puntos. Facultad y picante
    // no se usan porque el catalogo no tiene ese dato de los restaurantes.
    fun compatibilidad(cocina: String, nombre: String, esVegetariano: Boolean, esBarato: Boolean): Int {
        var puntos = 0
        var posibles = 0
        if (cuisines.isNotEmpty()) {
            posibles += PUNTOS_COCINA
            val texto = "$cocina $nombre".lowercase()
            if (cuisines.any { c -> palabrasClave(c).any { texto.contains(it) } }) puntos += PUNTOS_COCINA
        }
        if (prefiereVegetariano) {
            posibles += PUNTOS_DIETA
            if (esVegetariano) puntos += PUNTOS_DIETA
        }
        if (budget.isNotBlank()) {
            posibles += PUNTOS_PRESUPUESTO
            // Solo "Budget-friendly" restringe; con "Moderate" o "Treat yourself" cualquier precio sirve.
            if (!prefiereBarato || esBarato) puntos += PUNTOS_PRESUPUESTO
        }
        return if (posibles == 0) NEUTRO else (puntos * 100.0 / posibles).roundToInt()
    }

    fun compatibilidad(spot: Spot): Int = compatibilidad(spot.cuisine, spot.name, spot.isVegetarian, spot.isBudget)

    companion object {
        const val NEUTRO = 50
        private const val PUNTOS_COCINA = 50
        private const val PUNTOS_DIETA = 30
        private const val PUNTOS_PRESUPUESTO = 20

        // Las cocinas del perfil no se llaman igual que las del catalogo: "Artisan Burgers" tiene que
        // encontrar "Burgers & Fries" y "Specialty Coffee" a "Café & Bakery".
        private val PALABRAS_COCINA = mapOf(
            "Artisan Burgers" to listOf("burger"),
            "Crepas & Bowls" to listOf("bowl", "crepa", "crepe"),
            "Specialty Coffee" to listOf("coffee", "café", "cafe"),
            "Asian Wok" to listOf("wok", "asian", "ramen", "sushi"),
        )

        private fun palabrasClave(cocina: String): List<String> =
            PALABRAS_COCINA[cocina]
                ?: cocina.trim().split(' ').lastOrNull()?.lowercase()?.trimEnd('s')?.takeIf { it.isNotEmpty() }?.let(::listOf)
                ?: emptyList()
    }
}
