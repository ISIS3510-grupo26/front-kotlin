package com.example.frontend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frontend.model.Spot
import com.example.frontend.ui.theme.AppColors

@Composable
fun SpotList(
    spots: List<Spot>,
    emptyMessage: String,
    onToggleSaved: (String) -> Unit,
    onOpenSpot: (Spot) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (spots.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, color = AppColors.muted)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(spots, key = Spot::id) { spot ->
            SpotCard(
                spot = spot,
                onToggleSaved = { onToggleSaved(spot.id) },
                onClick = { onOpenSpot(spot) },
            )
        }
    }
}
