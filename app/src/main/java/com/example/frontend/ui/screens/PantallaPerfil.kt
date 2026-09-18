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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.ui.theme.AppColors

private data class TasteTag(val label: String, val active: Boolean)
private data class Badge(val emoji: String, val label: String, val color: Color, val unlocked: Boolean)

private val tasteTags = listOf(
    TasteTag("Burgers", true),
    TasteTag("Vegetarian Friendly", true),
    TasteTag("Medium Spice", true),
    TasteTag("Quick Bites", false),
    TasteTag("Under \$18.000 COP", false),
)

private val badges = listOf(
    Badge("🍔", "Burger Scout", AppColors.tomato, true),
    Badge("✓", "Verified Diner", AppColors.mint, true),
    Badge("🏆", "Budget Master", AppColors.tomato, true),
    Badge("🌙", "Night Owl", AppColors.muted, false),
    Badge("☕", "Coffee Addict", AppColors.muted, false),
    Badge("🌮", "Taco Tuesday", AppColors.muted, false),
    Badge("🥗", "Green Eater", AppColors.muted, false),
    Badge("⭐", "Top Reviewer", AppColors.muted, false),
)

@Composable
fun PantallaPerfil(savedCount: Int, modifier: Modifier = Modifier) {
    val unlocked = badges.count { it.unlocked }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Profile",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = AppColors.espresso)
                }
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                FotoPerfil()
                Spacer(Modifier.size(14.dp))
                Text("Julian Bierez", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                Text("Universidad de los Andes", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.muted)
            }
        }
        item {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                TituloSeccion(title = "Taste Profile") {
                    Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.tomato)
                }
                Spacer(Modifier.size(10.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tasteTags.forEach { tag -> ChipGusto(tag.label, tag.active) }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaDato(value = "13", label = "Places\nTasted", modifier = Modifier.weight(1f))
                TarjetaDato(value = "$savedCount", label = "Saved\nSpots", modifier = Modifier.weight(1f))
                TarjetaDato(value = "#3", label = "Campus\nBadges", valueColor = AppColors.tomato, modifier = Modifier.weight(1f))
            }
        }
        item {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                TituloSeccion(title = "Achievements & Badges") {
                    Text("$unlocked of ${badges.size} Unlocked", fontSize = 12.sp, color = AppColors.muted)
                }
                Spacer(Modifier.size(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(badges) { badge -> Insignia(badge) }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilaOpcion(
                    icon = Icons.Filled.RestaurantMenu,
                    iconColor = AppColors.tomato,
                    iconBackground = AppColors.tomatoLight,
                    title = "Dietary Preferences",
                )
                FilaOpcion(
                    icon = Icons.Filled.CreditCard,
                    iconColor = AppColors.mint,
                    iconBackground = AppColors.mintLight,
                    title = "Campus Dining Card (UniCard)",
                    subtitle = "Connected • \$42.500 COP balance",
                )
                FilaOpcion(
                    icon = Icons.Filled.NotificationsNone,
                    iconColor = AppColors.espresso,
                    iconBackground = AppColors.surface,
                    title = "Notifications",
                )
            }
        }
    }
}

@Composable
private fun FotoPerfil() {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(AppColors.surface, CircleShape)
                .border(4.dp, AppColors.card, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(52.dp))
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(AppColors.mint, CircleShape)
                .border(3.dp, AppColors.cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun TituloSeccion(title: String, trailing: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.espresso,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun ChipGusto(label: String, active: Boolean) {
    Box(
        modifier = Modifier
            .background(if (active) AppColors.tomato else AppColors.card, RoundedCornerShape(999.dp))
            .then(if (!active) Modifier.border(1.dp, AppColors.border, RoundedCornerShape(999.dp)) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else AppColors.espresso)
    }
}

@Composable
private fun TarjetaDato(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = AppColors.espresso) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Spacer(Modifier.size(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Insignia(badge: Badge) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(if (badge.unlocked) AppColors.card else AppColors.surface, CircleShape)
                .border(2.dp, if (badge.unlocked) badge.color else AppColors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (badge.unlocked) {
                Text(badge.emoji, fontSize = if (badge.emoji == "✓") 22.sp else 24.sp, color = badge.color, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.size(6.dp))
        Text(
            badge.label,
            maxLines = 1,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (badge.unlocked) AppColors.espresso else AppColors.muted,
        )
    }
}

@Composable
private fun FilaOpcion(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .clickable { }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBackground, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.espresso)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.mint)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppColors.muted)
    }
}
