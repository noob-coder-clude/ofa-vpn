package com.ofa.vpn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ofa.vpn.ui.OFAVPNViewModel
import com.ofa.vpn.data.model.Server
import com.ofa.vpn.ui.theme.AccentGreen
import com.ofa.vpn.ui.theme.AccentPurple
import com.ofa.vpn.ui.theme.DarkSurface
import com.ofa.vpn.ui.theme.DarkSurfaceVariant
import com.ofa.vpn.ui.theme.TextGray
import com.ofa.vpn.ui.theme.TextWhite

/**
 * Servers tab — list of imported servers.
 *
 * Features:
 *  - Server card: name, ping, protocol, favorite toggle
 *  - Click to select (marks as active)
 *  - FAB to add servers from subscription (navigates to Settings)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    viewModel: OFAVPNViewModel,
    onNavigate: (String) -> Unit
) {
    val servers by viewModel.servers.collectAsState()
    val selectedServerId by viewModel.selectedServerId.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is OFAVPNViewModel.NavEvent.Navigate -> onNavigate(event.route)
                is OFAVPNViewModel.NavEvent.ShowMessage -> {
                    // placeholder for snackbar
                }
                OFAVPNViewModel.NavEvent.AddFromSubscription -> {
                    onNavigate(com.ofa.vpn.ui.navigation.NavRoutes.SETTINGS)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (servers.isEmpty()) {
            EmptyServersState(onAddClick = {
                onNavigate(com.ofa.vpn.ui.navigation.NavRoutes.SETTINGS)
            })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Servers (${servers.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        isSelected = server.id == selectedServerId,
                        onSelect = { viewModel.selectServer(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // FAB — add from subscription
        FloatingActionButton(
            onClick = { onNavigate(com.ofa.vpn.ui.navigation.NavRoutes.SETTINGS) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = AccentGreen,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add from subscription",
                tint = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerCard(
    server: Server,
    isSelected: Boolean,
    onSelect: (Server) -> Unit,
    onToggleFavorite: (Server) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        onClick = { onSelect(server) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = if (isSelected) AccentGreen else TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) AccentGreen else TextWhite,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProtocolChip(protocol = server.protocol)
                    PingChip(ping = server.ping)
                    if (server.address.isNotEmpty()) {
                        Text(
                            text = server.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }

            IconButton(onClick = { onToggleFavorite(server) }) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (server.isFavorite) Color(0xFFFFB300) else TextGray
                )
            }
        }
    }
}

@Composable
private fun EmptyServersState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Public,
            contentDescription = null,
            tint = TextGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No servers yet",
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add a subscription to import your servers.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = onAddClick,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentGreen),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text("Add Subscription", fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
    }
}

@Composable
private fun ProtocolChip(protocol: String) {
    val color = when (protocol.lowercase()) {
        "vless" -> AccentGreen
        "trojan" -> AccentPurple
        "vmess" -> Color(0xFF1BCECA)
        "hysteria2" -> Color(0xFFFFB300)
        else -> TextGray
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.18f))
    ) {
        Text(
            text = protocol.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PingChip(ping: Int) {
    val color = when {
        ping in 1..150 -> AccentGreen
        ping in 151..400 -> Color(0xFFFFB300)
        else -> TextGray
    }
    val label = if (ping > 0) "$ping ms" else "— ms"
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
