package com.example.telecinema.theme

import androidx.compose.ui.graphics.Color

val GoldAccent = Color(0xFFE5B13D)
val BlueAccent = Color(0xFF0088CC)
val GreenAccent = Color(0xFF4CAF50)
val PurpleAccent = Color(0xFF9C57FF)
val RedAccent = Color(0xFFE53935)

val DarkBackground = Color(0xFF0B0F14)
val DarkSurface = Color(0xFF151B23)
val DarkSurfaceVariant = Color(0xFF1B2430)
val DarkCardBorder = Color(0xFF242E3B)

val TextPrimary = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF8B949E)
val TextMuted = Color(0xFF6E7681)

fun getThemeAccent(themeKey: String): Color {
    return when (themeKey.lowercase()) {
        "blue" -> BlueAccent
        "green" -> GreenAccent
        "purple" -> PurpleAccent
        "red" -> RedAccent
        else -> GoldAccent
    }
}
