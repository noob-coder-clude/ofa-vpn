package com.ofa.vpn.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ofa.vpn.ui.navigation.NavRoutes
import com.ofa.vpn.ui.screens.HomeScreen
import com.ofa.vpn.ui.screens.SettingsScreen
import com.ofa.vpn.ui.screens.ServersScreen
import com.ofa.vpn.ui.screens.TermsScreen
import com.ofa.vpn.ui.theme.AccentGreen
import com.ofa.vpn.ui.theme.DarkSurface
import com.ofa.vpn.ui.theme.OFAVPNTheme
import com.ofa.vpn.ui.theme.TextGray
import com.ofa.vpn.ui.theme.TextWhite

/**
 * OFAVPNApp — bottom navigation shell with 3 tabs:
 *  - Home (دکمه اتصال)
 *  - Servers (لیست سرورها)
 *  - Settings (تنظیمات)
 */
@Composable
fun OFAVPNApp(
    onVpnPermissionRequest: ((Intent, () -> Unit) -> Unit)? = null
) {
    val viewModel: OFAVPNViewModel = hiltViewModel()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    OFAVPNTheme(darkTheme = isDarkTheme) {
        var currentRoute by remember { mutableStateOf(NavRoutes.HOME) }

        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onRouteSelected = { currentRoute = it }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                when (currentRoute) {
                    NavRoutes.HOME -> HomeScreen(
                        onNavigate = { currentRoute = it },
                        onVpnPermissionRequest = onVpnPermissionRequest
                    )
                    NavRoutes.SERVERS -> ServersScreen(
                        viewModel = viewModel,
                        onNavigate = { currentRoute = it }
                    )
                    NavRoutes.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentRoute = NavRoutes.HOME },
                        onNavigateTerms = { currentRoute = NavRoutes.TERMS }
                    )
                    NavRoutes.TERMS -> TermsScreen(
                        onBack = { currentRoute = NavRoutes.SETTINGS }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String,
    onRouteSelected: (String) -> Unit
) {
    val items = listOf(
        Triple(NavRoutes.HOME, "خانه", Icons.Filled.Home),
        Triple(NavRoutes.SERVERS, "سرورها", Icons.Filled.List),
        Triple(NavRoutes.SETTINGS, "تنظیمات", Icons.Filled.Settings)
    )

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextWhite
    ) {
        items.forEach { (route, label, icon) ->
            val selected = route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onRouteSelected(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) AccentGreen else TextGray
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (selected) AccentGreen else TextGray
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}
