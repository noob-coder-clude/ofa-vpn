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
            when {
                uri.startsWith("vmess://") -> parseVmess(uri, subId, index)
                uri.startsWith("vless://") -> parseVlessOrTrojan(uri, subId, index, "vless")
                uri.startsWith("trojan://") -> parseVlessOrTrojan(uri, subId, index, "trojan")
                uri.startsWith("hysteria2://") || uri.startsWith("hy2://") ->
                    parseVlessOrTrojan(uri, subId, index, "hysteria2")
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * vmess:// فرمت: base64(json) — نه URL
     * JSON: {"v":"2","ps":"name","add":"host","port":"443","id":"uuid","aid":"0",
     *        "net":"ws","type":"none","host":"","path":"/","tls":"tls","sni":""}
     */
    private fun parseVmess(uri: String, subId: Long, index: Int): Server? {
        return try {
            val b64 = uri.removePrefix("vmess://").trim()
            // base64 decode — بعضی ساب‌ها padding ندارن
            val padded = b64 + "=".repeat((4 - b64.length % 4) % 4)
            val json = String(Base64.getDecoder().decode(padded))
            val obj = JSONObject(json)

            val address = obj.getString("add")
            val port = obj.optInt("port", obj.optString("port", "443").toIntOrNull() ?: 443)
            val uuid = obj.getString("id")
            val remark = obj.optString("ps", "")
            val network = obj.optString("net", "tcp")
            val tls = obj.optString("tls", "")
            val sni = obj.optString("sni", obj.optString("host", ""))
            val host = obj.optString("host", "")
            val path = obj.optString("path", "/")

            // ساخت rawUri برای ConfigParser
            val rawUri = "vmess://$b64"

            Server(
                name = if (remark.isNotEmpty()) remark else "$address:$port",
                protocol = "vmess",
                address = address,
                port = port,
                uuid = uuid,
                remark = remark,
                rawUri = rawUri,
                subscriptionId = subId
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * vless:// و trojan:// و hysteria2:// — فرمت URL دارن
     */
    private fun parseVlessOrTrojan(uri: String, subId: Long, index: Int, protocol: String): Server? {
        return try {
            val cleanUri = uri
                .replace("vless://", "https://")
                .replace("trojan://", "https://")
                .replace("hysteria2://", "https://")
                .replace("hy2://", "https://")

            val url = URL(cleanUri)

            val query = url.query ?: ""
            val params = query.split("&")
                .map { it.split("=", limit = 2) }
                .filter { it.size == 2 }
                .associate { (k, v) -> k to v }

            val remark = params["remarks"] ?: params["remark"] ?: params["sni"] ?: ""

            // برای trojan، uuid = password (بخش userinfo URL)
            val uuid = when (protocol) {
                "trojan" -> url.userInfo ?: params["password"] ?: ""
                else -> params["id"] ?: params["password"] ?: ""
            }

            Server(
                name = if (remark.isNotEmpty()) URLDecoder.decode(remark, "UTF-8")
                      else "${url.host}:${if (url.port > 0) url.port else defaultPort(protocol)}",
                protocol = protocol,
                address = url.host,
                port = if (url.port > 0) url.port else defaultPort(protocol),
                uuid = uuid,
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
