package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.ui.theme.AppColors

enum class BottomTab(val label: String, val icon: ImageVector) {
    FOR_YOU("For You", Icons.Filled.Home),
    MAP("Map", Icons.Filled.Map),
    SAVED("Saved", Icons.Filled.Bookmark),
    PROFILE("Profile", Icons.Filled.Person),
}

@Composable
fun CampusBottomNavBar(
    current: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = AppColors.card, shadowElevation = 8.dp, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BottomTab.entries.forEach { tab ->
                NavItem(tab = tab, active = tab == current, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun NavItem(tab: BottomTab, active: Boolean, onClick: () -> Unit) {
    val color = if (active) AppColors.tomato else AppColors.espresso
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = if (active) {
                Modifier
                    .background(AppColors.tomatoLight, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            } else Modifier,
        ) {
            Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier)
        }
        Text(
            tab.label,
            fontSize = 11.sp,
            color = color,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
