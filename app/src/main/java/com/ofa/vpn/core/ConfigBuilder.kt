package com.ofa.vpn.core

import com.ofa.vpn.data.local.ServerEntity
import org.json.JSONObject
import android.util.Base64

object ConfigBuilder {
    fun build(server: ServerEntity): String {
        val outbound = JSONObject()
        when (server.protocol) {
            "vmess" -> {
                val b64 = server.rawUri.removePrefix("vmess://")
                val json = JSONObject(String(Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)))
                
                outbound.put("protocol", "vmess")
                val settings = JSONObject()
                val vnext = JSONObject()
                vnext.put("address", server.address)
                vnext.put("port", server.port)
                val user = JSONObject()
                user.put("id", server.uuid)
                user.put("alterId", json.optInt("aid", 0))
                user.put("security", "auto")
                vnext.put("users", listOf(user))
                settings.put("vnext", listOf(vnext))
                outbound.put("settings", settings)

                val streamSettings = JSONObject()
                streamSettings.put("network", json.optString("net", "tcp"))
                streamSettings.put("security", json.optString("tls", "none"))
                if (json.optString("tls") == "tls") {
                    streamSettings.put("tlsSettings", JSONObject().put("serverName", json.optString("sni", server.address)))
                }
                if (json.optString("net") == "ws") {
                    val wsSettings = JSONObject()
                    wsSettings.put("path", json.optString("path", "/"))
                    wsSettings.put("headers", JSONObject().put("Host", json.optString("host", server.address)))
                    streamSettings.put("wsSettings", wsSettings)
                }
                outbound.put("streamSettings", streamSettings)
            }
            "vless", "trojan" -> {
                outbound.put("protocol", server.protocol)
                val settings = JSONObject()
                val serverObj = JSONObject()
                serverObj.put("address", server.address)
                serverObj.put("port", server.port)
                if (server.protocol == "vless") {
                    val user = JSONObject().put("id", server.uuid).put("encryption", "none")
                    settings.put("clients", listOf(user))
                    settings.put("decryption", "none")
                } else {
                    settings.put("password", server.uuid)
                }
                serverObj.put("method", "chacha20")
                settings.put("servers", listOf(serverObj))
                outbound.put("settings", settings)
            }
        }

        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))
        
        val inbound = JSONObject()
        inbound.put("port", 10808)
        inbound.put("protocol", "socks")
        inbound.put("listen", "127.0.0.1")
        inbound.put("settings", JSONObject().put("udp", true))
        root.put("inbounds", listOf(inbound))
        
        root.put("outbounds", listOf(outbound))
        return root.toString()
    }
}