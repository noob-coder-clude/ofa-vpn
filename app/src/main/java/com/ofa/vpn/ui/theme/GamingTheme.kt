package com.ofa.vpn.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * رنگ‌ها و براش‌های مخصوص حالت Gaming
 *
 * تم شعله‌وار: نارنجی → قرمز → زرد (مثل ویدیو مرجع "GT")
 */
object GamingTheme {
    val FireOrange = Color(0xFFFF6B1A)
    val FireRed = Color(0xFFFF2D2D)
    val FireYellow = Color(0xFFFFD23F)
    val FireDeep = Color(0xFF1A0A00)
    val EmberOrange = Color(0xFFFF8C42)

    /** براش پس‌زمینه گیمینگ — درخشش رادیال نارنجی روی مشکی */
    val backgroundBrush: Brush
        @Composable
        get() = Brush.radialGradient(
            colors = listOf(
                FireOrange.copy(alpha = 0.25f),
                FireDeep,
                Color.Black
            ),
            radius = 800f
        )

    /** حلقه بیرونی شعله‌وار (برای drawCircle/الگوها) */
    val ringBrush: Brush
        @Composable
        get() = Brush.sweepGradient(
            colors = listOf(
                FireYellow,
                FireOrange,
                FireRed,
                FireOrange,
                FireYellow
            )
        )
}

// rememberRingRotation and rememberPulseAlpha moved to ModeTheme.kt to avoid duplicate declarations
