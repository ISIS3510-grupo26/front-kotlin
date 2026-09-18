package com.example.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.FeedFilterType
import com.example.frontend.model.Spot
import com.example.frontend.ui.components.FilterChipPill
import com.example.frontend.ui.components.SpotCard
import com.example.frontend.ui.theme.AppColors

@Composable
fun ForYouScreen(
    spots: List<Spot>,
    onToggleSaved: (String) -> Unit,
    onOpenSpot: (Spot) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeFilters by remember { mutableStateOf(setOf(FeedFilterType.IN_A_RUSH)) }
    val feed = spots.filter { spot -> activeFilters.all { spot.matchesFilter(it) } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    "CampusBites",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.tomato,
                )
                Text(
                    "Campus food,\ntailored for you",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                    lineHeight = 32.sp,
                )
                Text(
                    "Curated by what students with your taste and schedule love",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(FeedFilterType.entries) { filter ->
                        FilterChipPill(
                            label = filter.label,
                            active = activeFilters.contains(filter),
                            onClick = {
                                activeFilters = if (activeFilters.contains(filter)) {
                                    activeFilters - filter
                                } else {
                                    activeFilters + filter
                                }
                            },
                        )
                    }
                }
            }
        }

        if (feed.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No spots match these filters yet.", color = AppColors.muted)
                }
            }
        } else {
            items(feed, key = { it.id }) { spot ->
                SpotCard(
                    spot = spot,
                    onToggleSaved = { onToggleSaved(spot.id) },
                    onClick = { onOpenSpot(spot) },
                )
            }
        }
    }
}
