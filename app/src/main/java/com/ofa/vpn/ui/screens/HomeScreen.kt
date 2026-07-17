package com.ofa.vpn.ui.screens

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ofa.vpn.R
import com.ofa.vpn.data.model.ConnectionMode
import com.ofa.vpn.data.model.ConnectionState
import com.ofa.vpn.service.VpnConnectionService
import com.ofa.vpn.ui.HomeViewModel
import com.ofa.vpn.ui.navigation.NavRoutes
import com.ofa.vpn.ui.theme.DarkBackground
import com.ofa.vpn.ui.theme.DarkSurface
import com.ofa.vpn.ui.theme.DarkSurfaceVariant
import com.ofa.vpn.ui.theme.TextGray
import com.ofa.vpn.ui.theme.TextWhite
import com.ofa.vpn.ui.theme.modeVisual

/**
 * Home tab — ExpressVPN-style minimal UI with OFA branding
 *
 * States:
 *  - Disconnected: gray shield icon
 *  - Connected: green/cyan shield icon + glow
 *  - Gaming connected: GT fire logo + rotating ring
 */
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onVpnPermissionRequest: ((Intent, () -> Unit) -> Unit)? = null,
    homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val vpnState by VpnConnectionService.state.collectAsStateWithLifecycle()
    val activeServer by VpnConnectionService.activeServer.collectAsStateWithLifecycle()
    val selectedMode by homeViewModel.selectedMode.collectAsStateWithLifecycle()

    val isConnected = vpnState == ConnectionState.CONNECTED
    val isBusy = vpnState == ConnectionState.CONNECTING || vpnState == ConnectionState.RECONNECTING
    val isError = vpnState == ConnectionState.ERROR
    val isGaming = isConnected && selectedMode == ConnectionMode.GAMING

    val statusText = when (vpnState) {
        ConnectionState.DISCONNECTED -> "قطع شده"
        ConnectionState.CONNECTING -> "در حال اتصال..."
        ConnectionState.CONNECTED -> "متصل"
        ConnectionState.RECONNECTING -> "اتصال مجدد..."
        ConnectionState.ERROR -> "خطا"
    }

    val visual = modeVisual(selectedMode)
    val accent = if (isConnected) visual.primary else TextWhite

    // Background gradient — respects light/dark theme
    val pageBg = DarkBackground
    val bgBrush = Brush.radialGradient(
        colors = listOf(
            if (isConnected) visual.bgCenter else pageBg,
            pageBg
        ),
        radius = 900f
    )

    // Rotating ring for gaming mode
    val transition = rememberInfiniteTransition(label = "ring")
    val ringAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAngle"
    )

    // Pulse for connected glow
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top: title + status ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = "OFA VPN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        isError -> Color(0xFFFF5252)
                        isConnected -> visual.primary
                        else -> TextGray
                    }
                )
                if (activeServer != null && isConnected) {
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = activeServer!!.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }

            // ── Center: connect button with effects ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Gaming: rotating fire ring
                    if (isGaming) {
                        Image(
                            painter = painterResource(R.drawable.ic_gaming_gt),
                            contentDescription = "GT Gaming",
                            modifier = Modifier
                                .size(180.dp)
                                .rotate(ringAngle)
                        )
                    }

                    // Connected: glow ring
                    if (isConnected && !isGaming) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            visual.primary.copy(alpha = pulseAlpha),
                                            Color.Transparent
                                        ),
                                        radius = 160f
                                    )
                                )
                        )
                    }

                    // Main button
                    Button(
                        onClick = {
                            if (isConnected || isBusy) {
                                homeViewModel.disconnect()
                            } else {
                                homeViewModel.setServersFromRepo()
                                val intent = homeViewModel.connect()
                                if (intent != null && onVpnPermissionRequest != null) {
                                    onVpnPermissionRequest(intent) { homeViewModel.connectAfterPermission() }
                                }
                            }
                        },
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface,
                            contentColor = accent,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextGray
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isConnected) 12.dp else 8.dp,
                            pressedElevation = 16.dp
                        )
                    ) {
                        // Icon based on state
                        if (isGaming) {
                            // GT text inside button
                            Text(
                                text = "GT",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFFFFD23F),
                                fontWeight = FontWeight.Black
                            )
                        } else if (isConnected) {
                            Image(
                                painter = painterResource(R.drawable.ic_connected),
                                contentDescription = "Connected",
                                modifier = Modifier.size(60.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_disconnected),
                                contentDescription = "Disconnected",
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(top = 20.dp))
                Text(
                    text = if (isConnected) "قطع اتصال" else "اتصال",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Bottom: quick actions ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    color = DarkSurface,
                    onClick = { onNavigate(NavRoutes.SERVERS) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("سرورها", color = TextWhite)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    color = DarkSurface,
                    onClick = { onNavigate(NavRoutes.SETTINGS) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("تنظیمات", color = TextWhite)
                    }
                }
            }
        }
    }
}
