package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.ui.theme.AppColors

@Composable
fun FilterChipPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        color = if (active) Color.White else AppColors.espresso,
        modifier = modifier
            .background(
                color = if (active) AppColors.tomato else AppColors.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(999.dp),
            )
            .then(
                if (!active) {
                    Modifier.border(1.dp, AppColors.espresso.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
                } else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
