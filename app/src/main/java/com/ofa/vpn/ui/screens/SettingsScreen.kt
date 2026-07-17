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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ofa.vpn.ui.OFAVPNViewModel
import com.ofa.vpn.ui.theme.AccentGreen
import com.ofa.vpn.ui.theme.AccentPurple
import com.ofa.vpn.ui.theme.AccentTeal
import com.ofa.vpn.ui.theme.DarkSurface
import com.ofa.vpn.ui.theme.DarkSurfaceVariant
import com.ofa.vpn.ui.theme.TextGray
import com.ofa.vpn.ui.theme.TextWhite

/**
 * Settings tab.
 *
 * Exposes:
 *  - Dark / light theme toggle
 *  - Auto-connect toggle
 *  - Subscription URL input + import action
 *  - About section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: OFAVPNViewModel,
    onBack: () -> Unit,
    onNavigateTerms: () -> Unit = {}
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val autoConnect by viewModel.autoConnect.collectAsState()
    val subscriptionUrl by viewModel.subscriptionUrl.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is OFAVPNViewModel.NavEvent.ShowMessage -> {
                    // placeholder for snackbar/toast
                }
                OFAVPNViewModel.NavEvent.AddFromSubscription -> Unit
                is OFAVPNViewModel.NavEvent.Navigate -> Unit
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", color = TextWhite) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance
            SectionTitle("Appearance")
            ToggleRow(
                title = "Dark theme",
                description = "Use dark mode for better night-time usage",
                checked = isDarkTheme,
                onToggle = { viewModel.setDarkTheme(it) }
            )

            // Connection
            SectionTitle("Connection")
            ToggleRow(
                title = "Auto-connect",
                description = "Connect automatically when app starts",
                checked = autoConnect,
                onToggle = { viewModel.setAutoConnect(it) }
            )

            // Subscription
            SectionTitle("Subscription")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Subscription URL",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentPurple
                    )
                    Text(
                        text = "Paste the URL provided by your panel. OFA will download, parse and import your servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    var localUrl by remember(subscriptionUrl) { mutableStateOf(subscriptionUrl) }
                    TextField(
                        value = localUrl,
                        onValueChange = {
                            localUrl = it
                            viewModel.setSubscriptionUrl(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("https://panel.example/sub/xxx", color = TextGray)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Button(
                        onClick = { viewModel.addFromSubscription() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("Import Servers", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // About
            SectionTitle("About")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text(text = "OFA VPN", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                    }
                    Text(
                        text = "Version 0.0.1",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Text(
                        text = "One For All — Simple, fast, reliable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Text(
                        text = "Supports VLESS, Trojan, VMess, Hysteria2.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // لینک قوانین و حریم خصوصی
                    androidx.compose.material3.TextButton(
                        onClick = { onBack(); onNavigateTerms() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "قوانین و حریم خصوصی",
                            color = AccentTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = AccentTeal,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentGreen,
                    checkedTrackColor = AccentGreen.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }
    }
}
