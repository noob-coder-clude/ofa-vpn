package com.ofa.vpn.service

import com.ofa.vpn.data.model.Server

/**
 * Singleton bridge بین UI و VpnConnectionService
 *
 * UI سرورها رو اینجا می‌ذاره (قبل از اتصال)،
 * سرویس از اینجا می‌خونه تا زنجیره fall-back رو بدونه.
 */
object VpnConnectionManager {
    @Volatile
    private var chain: List<Server> = emptyList()

    fun setFallbackChain(servers: List<Server>) {
        chain = servers
    }

    fun getFallbackChain(): List<Server> = chain

    fun clear() {
        chain = emptyList()
    }
}
