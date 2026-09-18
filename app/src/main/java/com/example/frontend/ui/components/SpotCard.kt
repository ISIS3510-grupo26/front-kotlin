package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.Spot
import com.example.frontend.ui.theme.AppColors

/**
 * The single card representation used everywhere a [Spot] is shown, in both
 * the For You feed and Saved Places. Tapping the heart toggles
 * `Spot.isSaved` — which is the same flag Saved Places filters on — so
 * saving a spot from the feed makes it show up there immediately.
 */
@Composable
fun SpotCard(
    spot: Spot,
    onToggleSaved: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.espresso.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (spot.isSaved) {
                    Pill(color = AppColors.mint) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Saved", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Pill(color = AppColors.tomato) {
                        Text("${spot.affinityPercent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                IconButton(onClick = onToggleSaved) {
                    Icon(
                        imageVector = if (spot.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (spot.isSaved) "Remove from saved" else "Save this spot",
                        tint = AppColors.tomato,
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(spot.emojiBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, spot.emojiBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(spot.emoji, fontSize = 28.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        spot.name,
                        maxLines = 1,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.espresso,
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(spot.subtitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.muted)
                    Spacer(Modifier.size(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.tomato, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${spot.rating}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.tomato)
                        MetaDot()
                        Text(spot.price, fontSize = 12.sp, color = AppColors.espresso)
                        MetaDot()
                        Text(spot.distance, fontSize = 12.sp, color = AppColors.muted)
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .border(
                        androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppColors.tomato)) {
                            append("${spot.noteLabel}: ")
                        }
                        append(spot.note)
                    },
                    fontSize = 12.sp,
                    color = AppColors.espresso,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun Pill(color: Color, content: RowScopeReceiver) {
    Row(
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun MetaDot() {
    Text(" • ", color = AppColors.muted, fontSize = 12.sp)
}

private typealias RowScopeReceiver = @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
