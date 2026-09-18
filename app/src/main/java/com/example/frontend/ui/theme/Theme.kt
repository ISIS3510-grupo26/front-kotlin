package com.example.frontend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CampusBitesColorScheme = lightColorScheme(
    primary = AppColors.tomato,
    secondary = AppColors.mint,
    background = AppColors.cream,
    surface = AppColors.card,
    onPrimary = AppColors.card,
    onSecondary = AppColors.card,
    onBackground = AppColors.espresso,
    onSurface = AppColors.espresso,
)

@Composable
fun FrontEndTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CampusBitesColorScheme,
        typography = Typography,
        content = content
    )
}
