package com.example.telecinema.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TeleCinemaTheme(
    themeKey: String = "gold",
    content: @Composable () -> Unit
) {
    val accent = getThemeAccent(themeKey)
    val colorScheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        primaryContainer = accent.copy(alpha = 0.25f),
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = Color.Black,
        background = DarkBackground,
        onBackground = TextPrimary,
        surface = DarkSurface,
        onSurface = TextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = DarkCardBorder,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
