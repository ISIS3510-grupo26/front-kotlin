package com.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.Spot
import com.example.frontend.model.SpotCategory
import com.example.frontend.ui.components.SpotCard
import com.example.frontend.ui.theme.AppColors

@Composable
fun SavedPlacesScreen(
    spots: List<Spot>,
    onToggleSaved: (String) -> Unit,
    onOpenSpot: (Spot) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<SpotCategory?>(null) }
    val saved = spots.filter { it.isSaved }
    val filtered = selectedCategory?.let { category -> saved.filter { it.category == category } } ?: saved

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    "Saved Places",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                )
                Text(
                    "Your favorite campus spots • ${saved.size} places saved",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.muted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        FilterPill(
                            label = "All Saved",
                            count = saved.size,
                            active = selectedCategory == null,
                            onClick = { selectedCategory = null },
                        )
                    }
                    items(SpotCategory.entries) { category ->
                        FilterPill(
                            label = category.label,
                            count = saved.count { it.category == category },
                            active = selectedCategory == category,
                            onClick = { selectedCategory = category },
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No saved places in this category yet.", color = AppColors.muted)
                }
            }
        } else {
            items(filtered, key = { it.id }) { spot ->
                SpotCard(
                    spot = spot,
                    onToggleSaved = { onToggleSaved(spot.id) },
                    onClick = { onOpenSpot(spot) },
                )
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, count: Int, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(if (active) AppColors.tomato else AppColors.card, RoundedCornerShape(999.dp))
            .then(
                if (!active) Modifier.border(1.dp, AppColors.border, RoundedCornerShape(999.dp)) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Color.White else AppColors.espresso,
        )
        Row(
            modifier = Modifier
                .padding(start = 6.dp)
                .background(
                    if (active) Color.White.copy(alpha = 0.2f) else AppColors.tomatoLight,
                    RoundedCornerShape(999.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                "$count",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else AppColors.tomato,
            )
        }
    }
}
