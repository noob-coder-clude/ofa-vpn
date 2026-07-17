package com.ofa.vpn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ofa.vpn.core.update.UpdateManager
import com.ofa.vpn.data.local.SettingsRepository
import com.ofa.vpn.data.model.Server
import com.ofa.vpn.data.remote.SubscriptionRepository
import com.ofa.vpn.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared ViewModel for the whole app shell.
 *
 * Holds:
 *  - the list of servers (from the repository Flow)
 *  - the currently selected server
 *  - app settings (theme, auto-connect, subscription URL) persisted via DataStore
 *  - one-shot navigation events consumed by the host scaffold
 */
@HiltViewModel
class OFAVPNViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

    // ---- Servers ---------------------------------------------------------
    val servers: StateFlow<List<Server>> =
        subscriptionRepository.getAllServers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedServerIdFlow = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val selectedServerId: StateFlow<Long?> =
        selectedServerIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServer: StateFlow<Server?> = combine(
        servers,
        selectedServerIdFlow
    ) { list, id -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- Settings (persisted) --------------------------------------------
    val isDarkTheme: StateFlow<Boolean> = settingsRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoConnect: StateFlow<Boolean> = settingsRepository.autoConnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val subscriptionUrl: StateFlow<String> = settingsRepository.subscriptionUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // ---- Navigation events (one-shot) -----------------------------------
    sealed interface NavEvent {
        data class Navigate(val route: String) : NavEvent
        object AddFromSubscription : NavEvent
        data class ShowMessage(val message: String) : NavEvent
    }

    private val _navEvents = Channel<NavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    // ---- Update (in-app) ------------------------------------------------
    private val _updateInfo = kotlinx.coroutines.flow.MutableStateFlow<UpdateManager.UpdateInfo?>(null)
    val updateInfo: kotlinx.coroutines.flow.StateFlow<UpdateManager.UpdateInfo?> = _updateInfo

    fun checkUpdate() {
        viewModelScope.launch {
            runCatching {
                val result = updateManager.checkUpdate()
                _updateInfo.value = result.info.takeIf { result.hasUpdate }
            }.onFailure {
                _navEvents.send(NavEvent.ShowMessage("Update check failed"))
            }
        }
    }

    fun downloadUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            runCatching {
                val ok = updateManager.downloadAndInstall(info.apkUrl)
                if (!ok) _navEvents.send(NavEvent.ShowMessage("Download failed"))
            }.onFailure {
                _navEvents.send(NavEvent.ShowMessage("Update failed"))
            }
        }
    }

    // ---- Actions ---------------------------------------------------------

    fun selectServer(server: Server) {
        selectedServerIdFlow.value = server.id
        viewModelScope.launch {
            _navEvents.send(NavEvent.ShowMessage("Selected: ${server.name}"))
        }
    }

    fun toggleFavorite(server: Server) {
        viewModelScope.launch {
            val updated = server.copy(isFavorite = !server.isFavorite)
            subscriptionRepository.updateServer(updated)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(enabled)
        }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoConnect(enabled)
        }
    }

    fun setSubscriptionUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setSubscriptionUrl(url)
        }
    }

    fun addFromSubscription() {
        val url = subscriptionUrl.value.trim()
        if (url.isEmpty()) {
            viewModelScope.launch {
                _navEvents.send(NavEvent.ShowMessage("Enter a subscription URL first"))
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                val name = deriveName(url)
                val id = subscriptionRepository.addSubscription(url, name)
                val sub = subscriptionRepository.getSubscriptionById(id)
                if (sub != null) {
                    subscriptionRepository.refresh(sub).getOrThrow()
                }
            }.onSuccess {
                _navEvents.send(NavEvent.ShowMessage("Servers imported"))
                _navEvents.send(NavEvent.Navigate(NavRoutes.SERVERS))
            }.onFailure { e ->
                _navEvents.send(NavEvent.ShowMessage("Import failed: ${e.message}"))
            }
        }
    }

    private fun deriveName(url: String): String {
        return runCatching {
            val host = url.substringAfter("://").substringBefore("/")
            if (host.isNotEmpty()) host else "Subscription"
        }.getOrDefault("Subscription")
    }
}
