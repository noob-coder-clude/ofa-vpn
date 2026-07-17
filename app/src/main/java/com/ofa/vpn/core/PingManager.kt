package com.ofa.vpn.core

import com.ofa.vpn.data.local.SettingsRepository
import com.ofa.vpn.data.model.ConnectionMode
import com.ofa.vpn.data.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

/**
 * تست تاخیر (پینگ TCP) + انتخاب هوشمند سرور
 *
 * "هوشمند" = ترکیب ۳ سیگنال:
 *  1. پینگ فعلی (TCP connect latency)
 *  2. تاریخچه اتصال موفق (lastConnected) — سروری که قبلاً جواب داده، اعتماد بیشتر
 *  3. اولویت کاربر (isFavorite)
 *
 * این یه مدل ML سنگین نیست؛ یه heuristic وزن‌دار سبکه که روی
 * دستگاه اجرا می‌شه و با هر بار استفاده بهتر می‌شه (یادگیری محلی).
 */
class PingManager @Inject constructor() {

    companion object {
        const val TIMEOUT_MS = 3000
        const val UNREACHABLE = 9999
    }

    /**
     * پینگ همه سرورها به صورت موازی + ذخیره پینگ تو خود آبجکت
     */
    suspend fun pingAll(servers: List<Server>): List<Server> = coroutineScope {
        servers.map { server ->
            async(Dispatchers.IO) {
                server.copy(ping = tcpPing(server.address, server.port))
            }
        }.awaitAll()
    }

    suspend fun tcpPing(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            val start = System.currentTimeMillis()
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            (System.currentTimeMillis() - start).toInt()
        } catch (e: Exception) {
            UNREACHABLE
        } finally {
            try { socket?.close() } catch (e: Exception) { /* ignore */ }
        }
    }

    /**
     * امتیازدهی هوشمند (۰..۱۰۰، بیشتر = بهتر)
     *
     *  - پینگ کم → امتیاز بالا
     *  - اتصال موفق قبلی (lastConnected اخیر) → امتیاز اضافه
     *  - favorite → امتیاز ثابت +
     */
    private fun score(server: Server, now: Long): Double {
        // پینگ: نرمال‌سازی به بازه ۰..۶۰ امتیاز
        val pingScore = when {
            server.ping >= UNREACHABLE -> 0.0
            server.ping <= 50 -> 60.0
            server.ping >= 400 -> 0.0
            else -> 60.0 * (1 - (server.ping - 50) / 350.0)
        }

        // تاریخچه: هرچه آخرین اتصال موفق نزدیک‌تر باشه، امتیاز بیشتر
        val historyScore = if (server.lastConnected > 0) {
            val ageHours = (now - server.lastConnected) / 3_600_000.0
            val decay = kotlin.math.exp(-ageHours / 72.0) // نصف بعد از ۳ روز
            25.0 * decay
        } else 0.0

        val favScore = if (server.isFavorite) 15.0 else 0.0

        return pingScore + historyScore + favScore
    }

    /**
     * انتخاب بهترین سرور بر اساس mode + یادگیری محلی
     */
    suspend fun selectBest(
        servers: List<Server>,
        mode: ConnectionMode,
        now: Long = System.currentTimeMillis()
    ): Server? {
        if (servers.isEmpty()) return null

        val pinged = pingAll(servers)
        val reachable = pinged.filter { it.ping < UNREACHABLE }

        // اگه پینگ TCP بلاک بود، به تاریخچه اتکا کن
        val pool = if (reachable.isEmpty()) pinged else reachable

        return when (mode) {
            ConnectionMode.GAMING -> {
                // گیمینگ: پینگ مطلق حرف اول رو می‌زنه
                pool.minByOrNull { it.ping }
            }
            ConnectionMode.SOCIAL, ConnectionMode.WEB -> {
                // پایداری > سرعت: امتیاز ترکیبی با وزن تاریخچه بیشتر
                pool.maxByOrNull { score(it, now) * 0.7 + (if (it.isFavorite) 15 else 0) }
            }
            else -> {
                // AUTO / PROXY: امتیاز کامل
                pool.maxByOrNull { score(it, now) }
            }
        }
    }

    /**
     * لیست fallback مرتب‌شده بر اساس امتیاز (برای زمان قطعی)
     */
    suspend fun getFallbackChain(
        servers: List<Server>,
        mode: ConnectionMode,
        now: Long = System.currentTimeMillis()
    ): List<Server> {
        val pinged = pingAll(servers)
        val pool = pinged.filter { it.ping < UNREACHABLE }.ifEmpty { pinged }
        return pool.sortedByDescending { score(it, now) }
    }
}
