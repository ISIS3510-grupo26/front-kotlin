package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.ui.theme.AppColors

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        label = {
            Text(
                text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
        },
        trailingIcon = count?.let { { CountBadge(it, selected) } },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AppColors.card,
            labelColor = AppColors.espresso,
            selectedContainerColor = AppColors.tomato,
            selectedLabelColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.border,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CountBadge(count: Int, selected: Boolean) {
    Text(
        "$count",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else AppColors.tomato,
        modifier = Modifier
            .background(
                if (selected) Color.White.copy(alpha = 0.2f) else AppColors.tomatoLight,
                CircleShape,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
