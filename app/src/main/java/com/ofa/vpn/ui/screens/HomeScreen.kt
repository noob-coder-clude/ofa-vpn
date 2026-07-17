package com.ofa.vpn.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.ofa.vpn.ui.theme.rememberPulseAlpha
import com.ofa.vpn.ui.theme.rememberRingRotation
import com.ofa.vpn.ui.theme.rememberSparkOffset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Home tab — دکمه اتصال اصلی + نمایش وضعیت + انتخاب mode
 *
 * وقتی متصل بشه، انیمیشن حلقه چرخون + ذرات جرقه + درخشش
 * بر اساس mode فعال، رنگ‌ها تغییر می‌کنن
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

    val visual = modeVisual(selectedMode)
    val isConnected = vpnState == ConnectionState.CONNECTED

    val statusText = when (vpnState) {
        ConnectionState.DISCONNECTED -> "قطع شده"
        ConnectionState.CONNECTING -> "در حال اتصال..."
        ConnectionState.CONNECTED -> "متصل"
        ConnectionState.RECONNECTING -> "اتصال مجدد..."
        ConnectionState.ERROR -> "خطا"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isConnected) visual.bgBrush
                else Brush.radialGradient(
                    listOf(DarkBackground.copy(alpha = 0.6f), Color.Black),
                    radius = 900f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            // عنوان
            Text(
                text = "OFA VPN",
                style = MaterialTheme.typography.headlineLarge,
                color = if (isConnected) visual.primary else TextWhite,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "One For All",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // دکمه اتصال با افکت
            ConnectButton(
                state = vpnState,
                mode = selectedMode,
                statusText = statusText,
                serverName = activeServer?.name,
                onConnect = {
                    homeViewModel.setServersFromRepo()
                    val intent = homeViewModel.connect()
                    if (intent != null && onVpnPermissionRequest != null) {
                        onVpnPermissionRequest(intent) { homeViewModel.connectAfterPermission() }
                    }
                },
                onDisconnect = { homeViewModel.disconnect() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // سرور فعال
            AnimatedVisibility(
                visible = activeServer != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                if (activeServer != null) {
                    ServerInfoCard(activeServer!!, visual.primary)
                }
            }

            // انتخاب mode
            ModeSelector(
                selectedMode = selectedMode,
                onSelect = { homeViewModel.selectMode(it) },
                isConnected = isConnected
            )

            // میانبرها
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "سرورها",
                    onClick = { onNavigate(NavRoutes.SERVERS) },
                    modifier = Modifier.weight(1f),
                    accent = if (isConnected) visual.primary else TextGray
                )
                SecondaryButton(
                    text = "تنظیمات",
                    onClick = { onNavigate(NavRoutes.SETTINGS) },
                    modifier = Modifier.weight(1f),
                    accent = if (isConnected) visual.primary else TextGray
                )
            }
        }
    }
}

// ---------- دکمه اتصال اصلی با افکت حلقه + جرقه ----------

@Composable
private fun ConnectButton(
    state: ConnectionState,
    mode: ConnectionMode,
    statusText: String,
    serverName: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected = state == ConnectionState.CONNECTED
    val isBusy = state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING
    val isError = state == ConnectionState.ERROR

    val statusColor = when (state) {
        ConnectionState.CONNECTED -> modeVisual(mode).primary
        ConnectionState.CONNECTING -> modeVisual(mode).primary.copy(alpha = 0.6f)
        ConnectionState.RECONNECTING -> modeVisual(mode).primary.copy(alpha = 0.6f)
        ConnectionState.ERROR -> Color(0xFFFF5252)
        ConnectionState.DISCONNECTED -> TextGray
    }

    val visual = modeVisual(mode)
    val ringRotation = rememberRingRotation()
    val pulseAlpha = rememberPulseAlpha()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // حلقه بیرونی شعله‌وار — فقط وقتی متصل
            if (isConnected) {
                // هاله بیرونی
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(visual.ringBrush)
                        .rotate(ringRotation)
                )

                // هاله داخلی درخشش
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    visual.glow.copy(alpha = pulseAlpha * 0.3f),
                                    Color.Transparent
                                ),
                                radius = 200f
                            )
                        )
                )

                // ذرات جرقه
                SparkParticles(
                    color = visual.glow,
                    modifier = Modifier.size(200.dp)
                )
            }

            // حلقه داخلی (همیشه)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                statusColor.copy(alpha = if (isConnected) 0.3f else 0.15f),
                                Color(0xFF0A0A0A)
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        color = if (isConnected) visual.primary.copy(alpha = pulseAlpha) else statusColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )

            // دکمه واقعی
            Button(
                onClick = {
                    if (isConnected || isBusy) onDisconnect() else onConnect()
                },
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D0D0D),
                    contentColor = if (isConnected) visual.primary else Color.White
                ),
                enabled = !isError || !isBusy
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isConnected && mode == ConnectionMode.GAMING) {
                        // متن GT برای گیمینگ
                        Text(
                            text = "GT",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = visual.primary
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Filled.Security else Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = if (isConnected) visual.primary else ContentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isConnected) "قطع" else "اتصال",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isConnected) visual.primary else Color.White
                    )
                }
            }
        }

        // وضعیت
        Text(
            text = if (isConnected) "${statusText} — ${visual.label}" else statusText,
            style = MaterialTheme.typography.titleSmall,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )

        // اسم سرور
        if (serverName != null && isConnected) {
            Text(
                text = serverName,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

// ---------- ذرات جرقه ----------

@Composable
private fun SparkParticles(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringRadius = size.minDimension / 2f

        // ۸ ذره در زاویه‌های مختلف
        for (i in 0 until 8) {
            val baseAngle = (i * 45f)
            val rad = Math.toRadians(baseAngle.toDouble()).toFloat()

            // آفست تصادفی با استفاده از تکثیر
            val t = (System.nanoTime() / (800_000_000L + i * 100_000_000L)).toFloat()
            val fract = (t % 1.0f)

            val dist = ringRadius * (0.85f + fract * 0.25f)
            val x = center.x + cos(rad) * dist
            val y = center.y + sin(rad) * dist
            val radius = (3f - fract * 2.5f).coerceAtLeast(0.5f)
            val alpha = (1f - fract).coerceIn(0f, 1f)

            drawCircle(
                color = color.copy(alpha = alpha * 0.8f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

// ---------- کارت سرور فعال ----------

@Composable
private fun ServerInfoCard(
    server: com.ofa.vpn.data.model.Server,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(text = server.protocol.uppercase(), color = accent)
                    InfoChip(
                        text = "${server.ping} ms",
                        color = if (server.ping in 0..150) Color(0xFF00E676) else Color(0xFFFFB300)
                    )
                }
            }
        }
    }
}

// ---------- انتخاب mode ----------

@Composable
private fun ModeSelector(
    selectedMode: ConnectionMode,
    onSelect: (ConnectionMode) -> Unit,
    isConnected: Boolean
) {
    val selectedVisual = modeVisual(selectedMode)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ConnectionMode.values().forEach { mode ->
            val visual = modeVisual(mode)
            FilterChip(
                selected = mode == selectedMode,
                onClick = { onSelect(mode) },
                label = { Text("${mode.icon} ${visual.label}") },
                shape = RoundedCornerShape(12.dp),
                colors = AssistChipDefaults.filterChipColors(
                    selectedContainerColor = visual.primary.copy(alpha = if (isConnected) 0.25f else 0.15f),
                    selectedLabelColor = visual.primary
                )
            )
        }
    }
}

// ---------- اجزای کوچک ----------

private val ContentColor = Color.White

@Composable
private fun InfoChip(text: String, color: Color) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = TextGray
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
    ) {
        Text(text, color = accent)
    }
}
