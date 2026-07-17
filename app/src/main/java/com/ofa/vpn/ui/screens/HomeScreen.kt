package com.ofa.vpn.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ofa.vpn.data.model.ConnectionState
import com.ofa.vpn.service.VpnConnectionService
import com.ofa.vpn.ui.HomeViewModel
import com.ofa.vpn.ui.navigation.NavRoutes
import com.ofa.vpn.ui.theme.DarkBackground
import com.ofa.vpn.ui.theme.DarkSurface
import com.ofa.vpn.ui.theme.TextGray
import com.ofa.vpn.ui.theme.TextWhite

/**
 * Home tab — ExpressVPN-style minimal UI
 *
 * - Dark background with subtle gradient
 * - Large circular connect button in center
 * - Status text above button
 * - Server name below button
 * - Bottom navigation: Servers | Settings
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

    val statusText = when (vpnState) {
        ConnectionState.DISCONNECTED -> "قطع شده"
        ConnectionState.CONNECTING -> "در حال اتصال..."
        ConnectionState.CONNECTED -> "متصل"
        ConnectionState.RECONNECTING -> "اتصال مجدد..."
        ConnectionState.ERROR -> "خطا"
    }

    val visual = com.ofa.vpn.ui.theme.modeVisual(selectedMode)
    val accent = if (isConnected) visual.primary else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DarkBackground, Color.Black),
                    radius = 900f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: status + server
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
                    color = if (isError) Color(0xFFFF5252) else TextGray
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

            // Center: big connect button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
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
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = accent
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Security else Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.padding(top = 20.dp))
                Text(
                    text = if (isConnected) "قطع اتصال" else "اتصال",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bottom: quick actions
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
                            imageVector = Icons.Filled.Security,
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
