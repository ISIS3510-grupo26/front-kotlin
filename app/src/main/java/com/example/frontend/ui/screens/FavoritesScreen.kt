package com.example.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.Spot
import com.example.frontend.model.SpotCategory
import com.example.frontend.ui.components.PillChip
import com.example.frontend.ui.components.SpotList
import com.example.frontend.ui.theme.AppColors

// null representa "All Saved".
private val categoryTabs: List<SpotCategory?> = listOf(null) + SpotCategory.entries

@Composable
fun FavoritesScreen(
    spots: List<Spot>,
    onToggleSaved: (String) -> Unit,
    onOpenSpot: (Spot) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf<SpotCategory?>(null) }

    val favorites = spots.filter { it.isSaved }
    val countByCategory = favorites.groupingBy { it.category }.eachCount()
    val visible = selected?.let { category -> favorites.filter { it.category == category } } ?: favorites

    Column(modifier = modifier.fillMaxSize()) {
        FavoritesHeader(
            total = favorites.size,
            countFor = { category -> category?.let { countByCategory[it] ?: 0 } ?: favorites.size },
            selected = selected,
            onSelect = { selected = it },
        )
        SpotList(
            spots = visible,
            emptyMessage = "No saved places in this category yet.",
            onToggleSaved = onToggleSaved,
            onOpenSpot = onOpenSpot,
        )
    }
}

@Composable
private fun FavoritesHeader(
    total: Int,
    countFor: (SpotCategory?) -> Int,
    selected: SpotCategory?,
    onSelect: (SpotCategory?) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            "Saved Places",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.espresso,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Your favorite campus spots • $total places saved",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.muted,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(categoryTabs) { category ->
                PillChip(
                    text = category?.label ?: "All Saved",
                    count = countFor(category),
                    selected = selected == category,
                    onClick = { onSelect(category) },
                )
            }
        }
    }
}
