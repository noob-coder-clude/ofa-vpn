package com.ofa.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Custom color palette
val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF161B22)
val DarkSurfaceVariant = Color(0xFF21262D)
val AccentGreen = Color(0xFF00C853)
val AccentTeal = Color(0xFF1BCECA)
val AccentPurple = Color(0xFF8B5CF6)
val TextWhite = Color(0xFFE6EDF3)
val TextGray = Color(0xFF8B949E)
val Connected = Color(0xFF00C853)
val Disconnected = Color(0xFF6E7681)
val Connecting = Color(0xFFFFB300)
val ErrorRed = Color(0xFFFF5252)
val WarningOrange = Color(0xFFFFB300)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentTeal,
    tertiary = AccentPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGreen,
    secondary = AccentTeal,
    tertiary = AccentPurple,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun OfaVpnTheme(
    darkTheme: Boolean = true, // Force dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OfaTypography,
        content = content
    )
}
