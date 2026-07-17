package com.ofa.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand accents (same in both themes)
val AccentGreen = Color(0xFF00C853)
val AccentTeal = Color(0xFF1BCECA)
val AccentPurple = Color(0xFF8B5CF6)
val Connected = Color(0xFF00C853)
val Disconnected = Color(0xFF6E7681)
val Connecting = Color(0xFFFFB300)
val ErrorColor = Color(0xFFFF5252)
val WarningColor = Color(0xFFFFB300)

@Immutable
data class OfaExtraColors(
    val pageBackground: Color,
    val cardSurface: Color,
    val cardSurfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalOfaColors = staticCompositionLocalOf {
    OfaExtraColors(
        pageBackground = Color(0xFF0D1117),
        cardSurface = Color(0xFF161B22),
        cardSurfaceVariant = Color(0xFF21262D),
        textPrimary = Color(0xFFE6EDF3),
        textSecondary = Color(0xFF8B949E)
    )
}

// Backward-compatible aliases used across screens (resolve from current theme)
val DarkBackground: Color
    @Composable get() = LocalOfaColors.current.pageBackground
val DarkSurface: Color
    @Composable get() = LocalOfaColors.current.cardSurface
val DarkSurfaceVariant: Color
    @Composable get() = LocalOfaColors.current.cardSurfaceVariant
val TextWhite: Color
    @Composable get() = LocalOfaColors.current.textPrimary
val TextGray: Color
    @Composable get() = LocalOfaColors.current.textSecondary

private val DarkExtra = OfaExtraColors(
    pageBackground = Color(0xFF0D1117),
    cardSurface = Color(0xFF161B22),
    cardSurfaceVariant = Color(0xFF21262D),
    textPrimary = Color(0xFFE6EDF3),
    textSecondary = Color(0xFF8B949E)
)

private val LightExtra = OfaExtraColors(
    pageBackground = Color(0xFFF6F8FA),
    cardSurface = Color.White,
    cardSurfaceVariant = Color(0xFFEEF1F4),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF656D76)
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentTeal,
    tertiary = AccentPurple,
    background = DarkExtra.pageBackground,
    surface = DarkExtra.cardSurface,
    surfaceVariant = DarkExtra.cardSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkExtra.textPrimary,
    onSurface = DarkExtra.textPrimary,
    error = ErrorColor,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGreen,
    secondary = AccentTeal,
    tertiary = AccentPurple,
    background = LightExtra.pageBackground,
    surface = LightExtra.cardSurface,
    surfaceVariant = LightExtra.cardSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightExtra.textPrimary,
    onSurface = LightExtra.textPrimary,
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun OFAVPNTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extras = if (darkTheme) DarkExtra else LightExtra
    CompositionLocalProvider(LocalOfaColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OfaTypography,
            content = content
        )
    }
}
