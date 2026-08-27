package com.huqi.noveltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Sage,
    background = Paper,
    surface = Color.White,
    surfaceVariant = AccentSoft,
    onBackground = Ink,
    onSurface = Ink
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB39DFF),
    onPrimary = Color(0xFF1B1B22),
    secondary = Color(0xFF80C080),
    background = Color(0xFF16161B),
    surface = Color(0xFF1F1F27),
    surfaceVariant = Color(0xFF2A2440),
    onBackground = Color(0xFFEDE7FF),
    onSurface = Color(0xFFEDE7FF)
)

@Composable
fun NovelTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
