package com.carxust.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun scheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.18f),
    onPrimaryContainer = Color.White,
    secondary = CarCyan,
    onSecondary = CarBlack,
    tertiary = CarBlue,
    background = CarBackground,
    onBackground = CarText,
    surface = CarSurface,
    onSurface = CarText,
    surfaceVariant = CarSurfaceElevated,
    onSurfaceVariant = CarTextMuted,
    outline = CarBorder,
    error = CarRed,
    onError = Color.White
)

@Composable
fun CarXustTheme(
    accent: Color = CarRed,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = scheme(accent),
        typography = CarXustTypography,
        content = content
    )
}
