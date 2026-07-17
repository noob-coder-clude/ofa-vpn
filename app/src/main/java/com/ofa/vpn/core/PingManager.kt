package com.ofa.vpn.core

import com.ofa.vpn.data.model.ConnectionMode
import com.ofa.vpn.data.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * تست تاخیر (پینگ TCP) به سرورها + انتخاب بهترین
 *
 * فلسفه OFA VPN:
 *  - پینگ فقط معیار ثانویه‌ست
 *  - وضعیت واقعی اتصال معیار اصلیه (تو VpnConnectionService چک می‌شه)
 *  - اینجا فقط مرتب‌سازی اولیه انجام می‌شه
 */
class PingManager {

    companion object {
        const val TIMEOUT_MS = 3000
        const val UNREACHABLE = 9999   // سرورهای غیرقابل دسترس
    }

    /**
     * پینگ همه سرورها به صورت موازی
     * برمی‌گردونه لیست مرتب‌شده بر اساس پینگ (کم به زیاد)
     */
    suspend fun pingAll(servers: List<Server>): List<Server> = coroutineScope {
        servers.map { server ->
            async(Dispatchers.IO) {
                server.copy(ping = tcpPing(server.address, server.port))
            }
        }.awaitAll()
            .sortedBy { it.ping }
    }

    /**
     * پینگ TCP یک سرور — زمان برقراری connection رو اندازه می‌گیره
     */
    suspend fun tcpPing(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            val start = System.currentTimeMillis()
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            val elapsed = (System.currentTimeMillis() - start).toInt()
            elapsed
        } catch (e: Exception) {
            UNREACHABLE
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * انتخاب بهترین سرور بر اساس mode
     */
    suspend fun selectBest(servers: List<Server>, mode: ConnectionMode): Server? {
        if (servers.isEmpty()) return null

        val pinged = pingAll(servers)
        val reachable = pinged.filter { it.ping < UNREACHABLE }

        // اگه هیچ سروری در دسترس نبود، اولین سرور رو برگردون (شاید پینگ TCP بلاک باشه)
        if (reachable.isEmpty()) return pinged.firstOrNull()

        return when (mode) {
            ConnectionMode.GAMING -> {
                // گیمینگ: کم‌ترین تاخیر مطلق
                reachable.minByOrNull { it.ping }
            }
            ConnectionMode.SOCIAL, ConnectionMode.WEB -> {
                // پایداری مهم‌تر از سرعت — سرور با پینگ منطقی و favorite اولویت
                reachable.filter { it.ping < 300 }
                    .sortedWith(compareByDescending<Server> { it.isFavorite }
                        .thenBy { it.ping })
                    .firstOrNull() ?: reachable.first()
            }
            else -> {
                // AUTO / PROXY: کم‌ترین پینگ، با اولویت favorite
                reachable.sortedWith(
                    compareByDescending<Server> { it.isFavorite }.thenBy { it.ping }
                ).firstOrNull() ?: reachable.first()
            }
        }
    }

    /**
     * لیست سرورهای fallback مرتب‌شده — اگه سرور اصلی قطع شد
     * (بدون looping کورکورانه — این لیست از پیش مرتب‌شده‌ست)
     */
    suspend fun getFallbackChain(servers: List<Server>, mode: ConnectionMode): List<Server> {
        val pinged = pingAll(servers)
        return pinged.filter { it.ping < UNREACHABLE }
            .ifEmpty { pinged }
    }
}
