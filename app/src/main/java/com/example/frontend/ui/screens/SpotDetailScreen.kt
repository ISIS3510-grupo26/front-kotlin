package com.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.frontend.model.MenuItem
import com.example.frontend.model.PeerReview
import com.example.frontend.model.Spot
import com.example.frontend.ui.theme.AppColors

@Composable
fun SpotDetailScreen(
    spot: Spot,
    onToggleSaved: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSaved by remember(spot.id) { mutableStateOf(spot.isSaved) }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.cream,
        bottomBar = {
            BottomActionBar(
                isSaved = isSaved,
                walkLabel = spot.distance.replace(" walk", ""),
                onToggleSaved = {
                    isSaved = !isSaved
                    onToggleSaved(spot.id)
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            item { Header(spot = spot, onBack = onBack) }
            if (spot.uniCardPerk != null) {
                item {
                    Spacer(Modifier.size(4.dp))
                    PerkCard(text = spot.uniCardPerk)
                    Spacer(Modifier.size(22.dp))
                }
            }
            item {
                SectionTitle(
                    title = "Full Campus Menu",
                    subtitle = "Affordable student rates with valid UniCard",
                ) {
                    CountPill("${spot.menu.size} items")
                }
                Spacer(Modifier.size(12.dp))
            }
            items(spot.menu) { item ->
                MenuTile(item)
                Spacer(Modifier.size(10.dp))
            }
            if (spot.menu.isEmpty()) {
                item { Text("Menu coming soon.", color = AppColors.muted, modifier = Modifier.padding(vertical = 12.dp)) }
            }
            item {
                Spacer(Modifier.size(4.dp))
                SectionTitle(
                    title = "Student Peer Reviews",
                    subtitle = "Community feedback from verified campus diners",
                ) {
                    Text("See all ${spot.totalReviews}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.tomato)
                }
                Spacer(Modifier.size(12.dp))
            }
            items(spot.reviews) { review ->
                ReviewCard(review)
                Spacer(Modifier.size(10.dp))
            }
            if (spot.reviews.isEmpty()) {
                item { Text("No reviews yet. Be the first!", color = AppColors.muted, modifier = Modifier.padding(vertical = 12.dp)) }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun Header(spot: Spot, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.espresso)
        }
        Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spot.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .background(AppColors.tomatoLight, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.tomato, modifier = Modifier.size(12.dp))
                    Text(" ${spot.rating}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.tomato)
                }
            }
            Text(
                "${spot.cuisine} • ${spot.distance} from ${spot.location}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.muted,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        IconButton(onClick = { }) {
            Icon(Icons.Filled.Share, contentDescription = "Share", tint = AppColors.espresso)
        }
    }
}

@Composable
private fun PerkCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.mintLight, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.mint.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(AppColors.mint, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "UniCard Campus Perk",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.background(AppColors.mint, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("10% OFF", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
            Text(text, fontSize = 12.sp, color = AppColors.muted, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso, modifier = Modifier.weight(1f))
            trailing()
        }
        Text(subtitle, fontSize = 12.sp, color = AppColors.muted)
    }
}

@Composable
private fun CountPill(label: String) {
    Box(modifier = Modifier.background(AppColors.tomatoLight, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.tomato)
    }
}

@Composable
private fun MenuTile(item: MenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(item.emojiBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.emoji, fontSize = 32.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            if (item.isStudentPick) {
                Row(
                    modifier = Modifier
                        .background(AppColors.mint, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Text(" Student Pick", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.size(4.dp))
            }
            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
            Text(item.description, fontSize = 11.sp, color = AppColors.muted)
            Spacer(Modifier.size(6.dp))
            Row {
                Text(item.price, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.tomato)
                Text(" COP", fontSize = 10.sp, color = AppColors.muted)
            }
        }
        IconButton(
            onClick = { },
            modifier = Modifier.size(34.dp).background(AppColors.tomato, CircleShape),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add ${item.name}", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ReviewCard(review: PeerReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(36.dp).background(AppColors.tomatoLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(review.initials, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.tomato)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(review.authorName, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                    Row(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .background(AppColors.mintLight, RoundedCornerShape(999.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = AppColors.mint, modifier = Modifier.size(10.dp))
                        Text(" Verified Diner", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.mint)
                    }
                }
                Text(review.program, fontSize = 11.sp, color = AppColors.muted)
            }
            Stars(count = review.stars)
        }
        Text(
            "\"${review.text}\"",
            fontSize = 12.5.sp,
            color = AppColors.espresso,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(review.dinedAgo, fontSize = 11.sp, color = AppColors.muted, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(13.dp))
            Text(" Helpful (${review.helpfulCount})", fontSize = 11.sp, color = AppColors.muted)
        }
    }
}

@Composable
private fun Stars(count: Int) {
    Row {
        repeat(5) { i ->
            Icon(
                imageVector = if (i < count) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = AppColors.tomato,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun BottomActionBar(isSaved: Boolean, walkLabel: String, onToggleSaved: () -> Unit) {
    Surface(color = AppColors.card, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onToggleSaved,
                modifier = Modifier.size(48.dp).background(AppColors.tomatoLight, RoundedCornerShape(14.dp)),
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Toggle saved",
                    tint = AppColors.tomato,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).size(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.tomato, contentColor = Color.White),
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Get Walking Directions ($walkLabel)", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
