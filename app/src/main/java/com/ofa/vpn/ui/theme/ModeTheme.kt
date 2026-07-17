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
 * تم هر حالت اتصال — رنگ، درخشش، براش پس‌زمینه
 *
 * Gaming:  نارنجی-شعله‌ای (آتیشی)
 * Auto:    سبز نئون (پیش‌فرض)
 * Web:     آبی-فیروزه‌ای
 * Social:  بنفش-صورتی
 * Proxy:   طلایی-مسی
 */
data class ModeVisual(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val bgCenter: Color,
    val bgEdge: Color,
    val ringBrush: Brush,
    val bgBrush: Brush,
    val label: String,
    val shortLabel: String
)

@Composable
fun modeVisual(mode: com.ofa.vpn.data.model.ConnectionMode): ModeVisual {
    return when (mode) {
        com.ofa.vpn.data.model.ConnectionMode.GAMING -> ModeVisual(
            name = "Gaming",
            primary = Color(0xFFFF6B1A),
            secondary = Color(0xFFFF2D2D),
            glow = Color(0xFFFFD23F),
            bgCenter = Color(0xFFFF6B1A).copy(alpha = 0.25f),
            bgEdge = Color(0xFF1A0A00),
            ringBrush = Brush.sweepGradient(listOf(
                Color(0xFFFFD23F), Color(0xFFFF6B1A),
                Color(0xFFFF2D2D), Color(0xFFFF6B1A), Color(0xFFFFD23F)
            )),
            bgBrush = Brush.radialGradient(
                listOf(Color(0xFFFF6B1A).copy(alpha = 0.22f), Color(0xFF1A0A00), Color.Black),
                radius = 900f
            ),
            label = "گیمینگ",
            shortLabel = "GT"
        )
        com.ofa.vpn.data.model.ConnectionMode.AUTO -> ModeVisual(
            name = "Auto",
            primary = Color(0xFF00E676),
            secondary = Color(0xFF00C853),
            glow = Color(0xFF69F0AE),
            bgCenter = Color(0xFF00E676).copy(alpha = 0.18f),
            bgEdge = Color(0xFF002B14),
            ringBrush = Brush.sweepGradient(listOf(
                Color(0xFF69F0AE), Color(0xFF00E676),
                Color(0xFF00C853), Color(0xFF00E676), Color(0xFF69F0AE)
            )),
            bgBrush = Brush.radialGradient(
                listOf(Color(0xFF00E676).copy(alpha = 0.15f), Color(0xFF002B14), Color.Black),
                radius = 900f
            ),
            label = "خودکار",
            shortLabel = "AUTO"
        )
        com.ofa.vpn.data.model.ConnectionMode.WEB -> ModeVisual(
            name = "Web",
            primary = Color(0xFF00B0FF),
            secondary = Color(0xFF2979FF),
            glow = Color(0xFF82B1FF),
            bgCenter = Color(0xFF00B0FF).copy(alpha = 0.18f),
            bgEdge = Color(0xFF001122),
            ringBrush = Brush.sweepGradient(listOf(
                Color(0xFF82B1FF), Color(0xFF00B0FF),
                Color(0xFF2979FF), Color(0xFF00B0FF), Color(0xFF82B1FF)
            )),
            bgBrush = Brush.radialGradient(
                listOf(Color(0xFF00B0FF).copy(alpha = 0.15f), Color(0xFF001122), Color.Black),
                radius = 900f
            ),
            label = "وب",
            shortLabel = "WEB"
        )
        com.ofa.vpn.data.model.ConnectionMode.SOCIAL -> ModeVisual(
            name = "Social",
            primary = Color(0xFFE040FB),
            secondary = Color(0xFFAA00FF),
            glow = Color(0xFFFF80AB),
            bgCenter = Color(0xFFE040FB).copy(alpha = 0.18f),
            bgEdge = Color(0xFF1A0022),
            ringBrush = Brush.sweepGradient(listOf(
                Color(0xFFFF80AB), Color(0xFFE040FB),
                Color(0xFFAA00FF), Color(0xFFE040FB), Color(0xFFFF80AB)
            )),
            bgBrush = Brush.radialGradient(
                listOf(Color(0xFFE040FB).copy(alpha = 0.15f), Color(0xFF1A0022), Color.Black),
                radius = 900f
            ),
            label = "سوشیال",
            shortLabel = "SOC"
        )
        com.ofa.vpn.data.model.ConnectionMode.PROXY -> ModeVisual(
            name = "Proxy",
            primary = Color(0xFFFFB300),
            secondary = Color(0xFFFF8F00),
            glow = Color(0xFFFFE082),
            bgCenter = Color(0xFFFFB300).copy(alpha = 0.18f),
            bgEdge = Color(0xFF1A1100),
            ringBrush = Brush.sweepGradient(listOf(
                Color(0xFFFFE082), Color(0xFFFFB300),
                Color(0xFFFF8F00), Color(0xFFFFB300), Color(0xFFFFE082)
            )),
            bgBrush = Brush.radialGradient(
                listOf(Color(0xFFFFB300).copy(alpha = 0.15f), Color(0xFF1A1100), Color.Black),
                radius = 900f
            ),
            label = "پروکسی",
            shortLabel = "PROXY"
        )
    }
}

/** انیمیشن چرخش حلقه — برای حالت متصل */
@Composable
fun rememberRingRotation(): Float {
    val transition = rememberInfiniteTransition(label = "ringRotation")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    return angle
}

/** انیمیشن پالس درخشش — برای حالت متصل */
@Composable
fun rememberPulseAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return alpha
}

/** انیمیشن ذرات جرقه — آفست تصادفی */
@Composable
fun rememberSparkOffset(seed: Int): Float {
    val transition = rememberInfiniteTransition(label = "spark_$seed")
    val offset by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800 + seed * 150),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset_$seed"
    )
    return offset
}
