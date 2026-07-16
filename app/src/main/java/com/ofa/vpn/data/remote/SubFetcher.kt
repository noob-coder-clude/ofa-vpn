package com.ofa.vpn.data.remote

import com.ofa.vpn.data.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * دریافت محتوای ساب از پنل 3x-ui / Marzban / Railway
 * پشتیبانی از: plain-text, base64, JSON
 */
class SubFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    /**
     * دانلود محتوای خام ساب
     */
    suspend fun fetchRaw(subUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(subUrl)
                .addHeader("User-Agent", "OFA-VPN/0.0.1")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                } else {
                    Result.success(response.body?.string() ?: "")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * پارس محتوای خام به لیست Server
     */
    fun parse(raw: String, subscriptionId: Long): List<Server> {
        return when {
            raw.trim().startsWith("[") || raw.trim().startsWith("{") -> parseJson(raw, subscriptionId)
            raw.contains("\n") && raw.contains("://") -> parsePlainText(raw, subscriptionId)
            else -> parseBase64(raw, subscriptionId)
        }
    }

    private fun parsePlainText(raw: String, subId: Long): List<Server> {
        return raw.lines()
            .filter { it.contains("://") }
            .mapIndexed { index, uri ->
                parseUri(uri, subId, index)
            }
            .filterNotNull()
            .toList()
    }

    private fun parseBase64(raw: String, subId: Long): List<Server> {
        try {
            val decoded = String(android.util.Base64.decode(raw.trim(), android.util.Base64.DEFAULT))
            return parsePlainText(decoded, subId)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun parseJson(raw: String, subId: Long): List<Server> {
        // TODO: implement JSON parsing for Marzban format
        return emptyList()
    }

    private fun parseUri(uri: String, subId: Long, index: Int): Server? {
        return try {
            val url = URL(uri.replace("vless://", "https://")
                .replace("trojan://", "https://")
                .replace("vmess://", "https://")
                .replace("hysteria2://", "https://"))

            val protocol = when {
                uri.startsWith("vless://") -> "vless"
                uri.startsWith("trojan://") -> "trojan"
                uri.startsWith("vmess://") -> "vmess"
                uri.startsWith("hysteria2://") -> "hysteria2"
                else -> "unknown"
            }

            val query = url.query ?: ""
            val params = query.split("&").associate { it.split("=").let { (k, v) -> k to v } }

            Server(
                name = params["remarks"]?.let { URLDecoder.decode(it, "UTF-8") }
                    ?: url.host + ":" + url.port
                    ?: "Server $index",
                protocol = protocol,
                address = url.host,
                port = if (url.port > 0) url.port else defaultPort(protocol),
                uuid = params["id"] ?: params["password"] ?: "",
                remark = params["remarks"]?.let { URLDecoder.decode(it, "UTF-8") } ?: "",
                rawUri = uri,
                subscriptionId = subId
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun defaultPort(protocol: String): Int = when (protocol) {
        "vless" -> 443
        "trojan" -> 443
        "vmess" -> 443
        "hysteria2" -> 443
        else -> 443
    }
}