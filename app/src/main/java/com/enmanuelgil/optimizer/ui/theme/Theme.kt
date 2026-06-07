package com.enmanuelgil.optimizer.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1E88E5)
val PrimaryDark = Color(0xFF0D47A1)
val AccentGreen = Color(0xFF00E676)
val AccentOrange = Color(0xFFFF6D00)
val AccentRed = Color(0xFFFF1744)
val BackgroundDark = Color(0xFF0A0E1A)
val SurfaceDark = Color(0xFF141928)
val CardDark = Color(0xFF1E2538)
val TextPrimary = Color(0xFFE8EAF6)
val TextSecondary = Color(0xFF9FA8DA)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    secondary = AccentGreen,
    tertiary = AccentOrange,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
)

@Composable
fun PhoneOptimizerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
