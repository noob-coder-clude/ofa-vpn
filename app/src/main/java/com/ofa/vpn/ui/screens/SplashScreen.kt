package com.ofa.vpn.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofa.vpn.R
import com.ofa.vpn.ui.theme.AccentGreen
import com.ofa.vpn.ui.theme.AccentTeal
import com.ofa.vpn.ui.theme.DarkBackground
import com.ofa.vpn.ui.theme.TextGray
import com.ofa.vpn.ui.theme.TextWhite
import kotlinx.coroutines.delay

/**
 * Splash on app launch:
 *  1. Logo + "OFA VPN" fade/scale in
 *  2. "One For All" slowly appears below (medium weight)
 *  3. Then hands off to main UI
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        // Logo appears first
        logoAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))

        // Title shortly after
        delay(120)
        titleAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))

        // Tagline fades in slowly
        delay(280)
        taglineAlpha.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))

        // Hold a beat, then go
        delay(650)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AccentGreen.copy(alpha = 0.08f),
                        DarkBackground
                    ),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "OFA VPN",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "OFA VPN",
                style = MaterialTheme.typography.headlineLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "One For All",
                style = MaterialTheme.typography.titleMedium,
                color = AccentTeal,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "یک برای همه",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.alpha(taglineAlpha.value * 0.85f)
            )
        }
    }
}
