package com.ofa.vpn.data.remote

import com.ofa.vpn.data.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLDecoder
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * دریافت محتوای ساب از پنل 3x-ui / Marzban / X4G / PasarGuard
 *
 * پشتیبانی از فرمت‌ها:
 *  - plain-text: لیست URI ها (vless://, vmess://, trojan://, hysteria2://)
 *  - base64: رمز شده (مثل ساب استاندارد v2rayNG/شادوساکت)
 *  - JSON: خروجی پنل Marzban / 3x-ui (آرایه‌ای از آبجکت‌ها)
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
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("[") || trimmed.startsWith("{") -> parseJson(trimmed, subscriptionId)
            trimmed.contains("\n") && trimmed.contains("://") -> parsePlainText(trimmed, subscriptionId)
            trimmed.contains("://") -> parsePlainText(trimmed, subscriptionId)
            else -> parseBase64(trimmed, subscriptionId)
        }
    }

    // ---------- Plain text ----------

    private fun parsePlainText(raw: String, subId: Long): List<Server> {
        return raw.lines()
            .map { it.trim() }
            .filter { it.contains("://") }
            .mapIndexed { index, uri -> parseUri(uri, subId, index) }
            .filterNotNull()
            .toList()
    }

    // ---------- Base64 ----------

    private fun parseBase64(raw: String, subId: Long): List<Server> {
        return try {
            val normalized = raw.replace("\n", "").replace(" ", "")
            val decoded = String(Base64.getDecoder().decode(normalized))
            parsePlainText(decoded, subId)
        } catch (e: Exception) {
            // شاید چند لایه base64 باشه
            try {
                val decoded = String(Base64.getDecoder().decode(raw.trim()))
                parsePlainText(decoded, subId)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    // ---------- JSON (Marzban / 3x-ui) ----------

    private fun parseJson(raw: String, subId: Long): List<Server> {
        return try {
            val root = JSONObject(raw)

            // فرمت 3x-ui / Marzban: { "outbounds": [ { "tag": "proxy", "settings": {...} } ] }
            if (root.has("outbounds")) {
                val outbounds = root.getJSONArray("outbounds")
                val servers = mutableListOf<Server>()
                for (i in 0 until outbounds.length()) {
                    val ob = outbounds.getJSONObject(i)
                    val tag = ob.optString("tag", "")
                    if (tag == "proxy" || ob.optString("protocol").isNotEmpty()) {
                        val protocol = ob.optString("protocol", "vless")
                        val settings = ob.optJSONObject("settings")
                        val vnext = settings?.optJSONArray("vnext")?.optJSONObject(0)
                        val address = vnext?.optString("address") ?: ""
                        val port = vnext?.optInt("port") ?: 443
                        val user = vnext?.optJSONArray("users")?.optJSONObject(0)
                        val uuid = user?.optString("id") ?: user?.optString("password") ?: ""

                        servers.add(
                            Server(
                                name = "Server ${servers.size + 1}",
                                protocol = protocol,
                                address = address,
                                port = port,
                                uuid = uuid,
                                remark = "",
                                rawUri = "",
                                subscriptionId = subId
                            )
                        )
                    }
                }
                servers
            }
            // فرمت آرایه ساده: [ { "name": "...", "type": "vless", ... } ]
            else if (raw.trim().startsWith("[")) {
                val arr = JSONArray(raw)
                val servers = mutableListOf<Server>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val protocol = obj.optString("type", obj.optString("protocol", "vless"))
                    val address = obj.optString("server", obj.optString("address", ""))
                    val port = obj.optInt("port", 443)
                    val uuid = obj.optString("id", obj.optString("uuid", obj.optString("password", "")))

                    servers.add(
                        Server(
                            name = obj.optString("remark", obj.optString("name", "Server ${i + 1}")),
                            protocol = protocol,
                            address = address,
                            port = port,
                            uuid = uuid,
                            remark = obj.optString("remark", ""),
                            rawUri = "",
                            subscriptionId = subId
                        )
                    )
                }
                servers
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------- URI parsing ----------

    private fun parseUri(uri: String, subId: Long, index: Int): Server? {
        return try {
            val cleanUri = uri.replace("vmess://", "https://")
                .replace("vless://", "https://")
                .replace("trojan://", "https://")
                .replace("hysteria2://", "https://")

            val url = URL(cleanUri)

            val protocol = when {
                uri.startsWith("vless://") -> "vless"
                uri.startsWith("trojan://") -> "trojan"
                uri.startsWith("vmess://") -> "vmess"
                uri.startsWith("hysteria2://") -> "hysteria2"
                else -> "unknown"
            }

            val query = url.query ?: ""
            val params = query.split("&")
                .map { it.split("=", limit = 2) }
                .filter { it.size == 2 }
                .associate { (k, v) -> k to v }

            val remark = params["remarks"] ?: params["remark"] ?: params["sni"] ?: ""

            Server(
                name = if (remark.isNotEmpty()) URLDecoder.decode(remark, "UTF-8") else "${url.host}:${if (url.port > 0) url.port else defaultPort(protocol)}",
                protocol = protocol,
                address = url.host,
                port = if (url.port > 0) url.port else defaultPort(protocol),
                uuid = params["id"] ?: params["password"] ?: "",
                remark = if (remark.isNotEmpty()) URLDecoder.decode(remark, "UTF-8") else "",
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
