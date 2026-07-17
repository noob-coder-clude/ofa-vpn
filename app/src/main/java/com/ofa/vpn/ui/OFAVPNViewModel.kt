package com.ofa.vpn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ofa.vpn.data.model.Server
import com.ofa.vpn.data.remote.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
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
 *  - app settings (theme, auto-connect, subscription URL)
 *  - one-shot navigation events consumed by the host scaffold
 *
 * Phase 4 — bottom navigation + Servers/Settings screens.
 */
@HiltViewModel
class OFAVPNViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    // ---- Servers ---------------------------------------------------------
    val servers: StateFlow<List<Server>> =
        subscriptionRepository.getAllServers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedServerId = MutableStateFlow<Long?>(null)
    val selectedServerId: StateFlow<Long?> = _selectedServerId.asStateFlow()

    val selectedServer: StateFlow<Server?> = combine(
        servers,
        _selectedServerId
    ) { list, id -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- Settings --------------------------------------------------------
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _autoConnect = MutableStateFlow(false)
    val autoConnect: StateFlow<Boolean> = _autoConnect.asStateFlow()

    private val _subscriptionUrl = MutableStateFlow("")
    val subscriptionUrl: StateFlow<String> = _subscriptionUrl.asStateFlow()

    // ---- Navigation events (one-shot) -----------------------------------
    sealed interface NavEvent {
        /** Request the host to switch to a bottom-nav destination. */
        data class Navigate(val route: String) : NavEvent

        /** Request adding servers from the current subscription URL. */
        object AddFromSubscription : NavEvent

        /** Transient user-facing message. */
        data class ShowMessage(val message: String) : NavEvent
    }

    private val _navEvents = Channel<NavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    // ---- Actions ---------------------------------------------------------

    fun selectServer(server: Server) {
        _selectedServerId.value = server.id
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
        _isDarkTheme.value = enabled
    }

    fun setAutoConnect(enabled: Boolean) {
        _autoConnect.value = enabled
    }

    fun setSubscriptionUrl(url: String) {
        _subscriptionUrl.value = url
    }

    /**
     * Add servers from the subscription URL currently typed in settings.
     * Persists a new subscription row and refreshes it.
     */
    fun addFromSubscription() {
        val url = _subscriptionUrl.value.trim()
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
