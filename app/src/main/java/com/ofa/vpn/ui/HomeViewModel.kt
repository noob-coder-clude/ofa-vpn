package com.ofa.vpn.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ofa.vpn.data.model.ConnectionMode
import com.ofa.vpn.data.model.ConnectionState
import com.ofa.vpn.data.model.Server
import com.ofa.vpn.data.remote.SubscriptionRepository
import com.ofa.vpn.service.VpnConnectionManager
import com.ofa.vpn.service.VpnConnectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel صفحه Home — پل بین UI و VpnConnectionService
 *
 * مسئولیت‌ها:
 *  - دریافت مجوز VPN از کاربر
 *  - شروع/توقف سرویس
 *  - تزریق سرورها به VpnConnectionManager قبل از اتصال
 *  - نمایش وضعیت اتصال (از companion flows سرویس)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val subscriptionRepository: SubscriptionRepository
) : AndroidViewModel(application) {

    private val _vpnState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val vpnState: StateFlow<ConnectionState> = _vpnState.asStateFlow()

    private val _activeServer = MutableStateFlow<Server?>(null)
    val activeServer: StateFlow<Server?> = _activeServer.asStateFlow()

    private val _selectedMode = MutableStateFlow(ConnectionMode.AUTO)
    val selectedMode: StateFlow<ConnectionMode> = _selectedMode.asStateFlow()

    init {
        viewModelScope.launch {
            VpnConnectionService.state.collect { _vpnState.value = it }
        }
        viewModelScope.launch {
            VpnConnectionService.activeServer.collect { _activeServer.value = it }
        }
    }

    /**
     * درخواست اتصال — اگه مجوز VPN نداره، Intent برمی‌گرده
     * اگه مجوز داره، مستقیم شروع می‌شه
     */
    fun connect(): Intent? {
        val ctx = getApplication<Application>()
        val intent = VpnService.prepare(ctx)
        if (intent != null) {
            return intent
        }
        startVpnService(ctx)
        return null
    }

    /**
     * بعد از دریافت مجوز VPN — از سمت UI صدا زده می‌شه
     */
    fun connectAfterPermission() {
        startVpnService(getApplication())
    }

    fun disconnect() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, VpnConnectionService::class.java).apply {
            action = VpnConnectionService.ACTION_DISCONNECT
        })
    }

    fun selectMode(mode: ConnectionMode) {
        _selectedMode.value = mode
    }

    /**
     * گرفتن همه سرورها از DB و تزریق به VpnConnectionManager
     * قبل از اتصال صدا زده می‌شه
     */
    fun setServersFromRepo() {
        viewModelScope.launch {
            val servers = subscriptionRepository.getAllServers().first()
            VpnConnectionManager.setFallbackChain(servers)
        }
    }

    private fun startVpnService(ctx: Context) {
        // اول سرورها رو تزریق کن
        viewModelScope.launch {
            val servers = subscriptionRepository.getAllServers().first()
            VpnConnectionManager.setFallbackChain(servers)

            // بعد سرویس رو شروع کن
            ctx.startService(Intent(ctx, VpnConnectionService::class.java).apply {
                action = VpnConnectionService.ACTION_CONNECT
                putExtra(VpnConnectionService.EXTRA_MODE, _selectedMode.value.name)
            })
        }
    }
}
