package com.ofa.vpn.core

import android.util.Base64
import com.ofa.vpn.data.local.ServerEntity
import org.json.JSONObject
import java.net.URLDecoder

object ConfigParser {

    fun parseSubscription(rawContent: String): List<ServerEntity> {
        val servers = mutableListOf<ServerEntity>()
        
        val decodedContent = try {
            // اگه با vmess شروع نمیشه، احتمالا Base64 هست
            if (!rawContent.startsWith("vmess://") && !rawContent.startsWith("vless://")) {
                // پاک کردن کاراکترهای اضافی و فاصله‌ها
                val cleanB64 = rawContent.replace("\n", "").replace(" ", "").trim()
                // استفاده از URL_SAFE برای دیکود کردن دقیق‌تر
                String(Base64.decode(cleanB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            } else {
                rawContent
            }
        } catch (e: Exception) {
            // اگه دیکود فیل شد، همون متن خام رو نگه دار
            rawContent
        }

        decodedContent.split("\n").forEach { line ->
            val uri = line.trim()
            if (uri.isEmpty()) return@forEach

            try {
                when {
                    uri.startsWith("vmess://") -> parseVmess(uri)?.let { servers.add(it) }
                    uri.startsWith("vless://") -> parseVless(uri)?.let { servers.add(it) }
                }
            } catch (e: Exception) {
                println("Failed to parse: $uri")
            }
        }
        return servers
    }

    private fun parseVmess(uri: String): ServerEntity? {
        val b64 = uri.removePrefix("vmess://")
        val jsonStr = String(Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        val json = JSONObject(jsonStr)

        return ServerEntity(
            name = json.optString("ps", "Unknown VMess"),
            protocol = "vmess",
            address = json.optString("add", ""),
            port = json.optInt("port", 0),
            uuid = json.optString("id", ""),
            rawUri = uri
        )
    }

    private fun parseVless(uri: String): ServerEntity? {
        val parsed = java.net.URI(uri)
        val name = URLDecoder.decode(parsed.fragment ?: "Unknown VLESS", "UTF-8")
        val host = parsed.host ?: return null
        val port = parsed.port.takeIf { it != -1 } ?: 443
        val uuid = parsed.userInfo ?: return null

        return ServerEntity(
            name = name,
            protocol = "vless",
            address = host,
            port = port,
            uuid = uuid,
            rawUri = uri
        )
    }
}
