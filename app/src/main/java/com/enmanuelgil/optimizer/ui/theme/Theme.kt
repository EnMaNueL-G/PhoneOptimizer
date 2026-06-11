package com.enmanuelgil.optimizer.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta alineada al ícono: azul → verde sobre negro azulado.
val PrimaryBlue = Color(0xFF22A7F0)   // azul brillante del anillo
val PrimaryDark = Color(0xFF0D47A1)
val AccentGreen = Color(0xFF3DDC84)   // verde Android del ícono
val AccentOrange = Color(0xFFFF6D00)
val AccentRed = Color(0xFFFF1744)
val BackgroundDark = Color(0xFF070B14)  // fondo del ícono
val SurfaceDark = Color(0xFF0E1422)
val CardDark = Color(0xFF161E2E)
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
