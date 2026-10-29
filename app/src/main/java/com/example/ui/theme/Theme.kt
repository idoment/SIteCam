package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmberAccent,
    onPrimary = NavyDark,
    primaryContainer = AmberDark,
    onPrimaryContainer = Color.White,
    secondary = AmberLight,
    onSecondary = NavyDark,
    background = NavyPrimary,
    onBackground = TextWhite,
    surface = NavySurface,
    onSurface = TextWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = BorderOutline,
    error = GpsRed,
    onError = Color.White
)

@Composable
fun SiteCamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
