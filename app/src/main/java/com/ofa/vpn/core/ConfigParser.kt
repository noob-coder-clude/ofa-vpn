package com.ofa.vpn.core

import android.util.Base64
import com.ofa.vpn.data.local.ServerEntity
import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml
import java.net.URLDecoder

object ConfigParser {

    fun parseSubscription(rawContent: String): List<ServerEntity> {
        val servers = mutableListOf<ServerEntity>()
        val cleanContent = rawContent.trim()

        try {
            when {
                // ۱. پشتیبانی از فرمت Clash (YAML)
                cleanContent.contains("proxies:") -> parseClashYaml(cleanContent, servers)
                
                // ۲. پشتیبانی از فرمت JSON
                cleanContent.startsWith("[") || cleanContent.startsWith("{") -> parseJson(cleanContent, servers)
                
                // ۳. پشتیبانی از Base64 و متن خام
                else -> parseBase64OrPlain(cleanContent, servers)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return servers
    }

    private fun parseClashYaml(content: String, servers: MutableList<ServerEntity>) {
        val yaml = Yaml()
        val data = yaml.load<Map<String, Any>>(content)
        val proxies = data["proxies"] as? List<Map<String, Any>> ?: return
        
        for (proxy in proxies) {
            try {
                val type = proxy["type"] as? String ?: continue
                val name = proxy["name"] as? String ?: "Unknown"
                val server = proxy["server"] as? String ?: continue
                val port = (proxy["port"] as? Int) ?: continue

                if (type == "vmess") {
                    // تبدیل کلش به فرمت VMess Base64 برای استفاده در Xray
                    val vmessJson = JSONObject()
                    vmessJson.put("v", "2")
                    vmessJson.put("ps", name)
                    vmessJson.put("add", server)
                    vmessJson.put("port", port)
                    vmessJson.put("id", proxy["uuid"] as? String ?: "")
                    vmessJson.put("aid", (proxy["alterId"] as? Int) ?: 0)
                    vmessJson.put("net", proxy["network"] as? String ?: "tcp")
                    vmessJson.put("tls", if (proxy["tls"] as? Boolean == true) "tls" else "")
                    vmessJson.put("sni", proxy["servername"] as? String ?: server)
                    
                    val b64 = Base64.encodeToString(vmessJson.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                    val rawUri = "vmess://$b64"
                    servers.add(ServerEntity(name=name, protocol="vmess", address=server, port=port, uuid=vmessJson.optString("id"), rawUri=rawUri))
                } 
                else if (type == "vless") {
                    val uuid = proxy["uuid"] as? String ?: continue
                    val rawUri = "vless://$uuid@$server:$port?encryption=none#${URLDecoder.decode(name, "UTF-8")}"
                    servers.add(ServerEntity(name=name, protocol="vless", address=server, port=port, uuid=uuid, rawUri=rawUri))
                }
            } catch (e: Exception) {}
        }
    }

    private fun parseJson(content: String, servers: MutableList<ServerEntity>) {
        if (content.startsWith("[")) {
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                if (item != null) {
                    // اگر آیتم‌ها خودشون فرمت VMess JSON داشتن
                    if (item.has("add") && item.has("id")) {
                        val b64 = Base64.encodeToString(item.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                        servers.add(ServerEntity(name=item.optString("ps","VMess"), protocol="vmess", address=item.optString("add"), port=item.optInt("port"), uuid=item.optString("id"), rawUri="vmess://$b64"))
                    }
                } else {
                    val str = array.optString(i)
                    if (str.startsWith("vmess://") || str.startsWith("vless://")) parseBase64OrPlain(str, servers)
                }
            }
        }
    }

    private fun parseBase64OrPlain(content: String, servers: MutableList<ServerEntity>) {
        val decodedContent = try {
            if (!content.startsWith("vmess://") && !content.startsWith("vless://")) {
                String(Base64.decode(content.replace("\\n", "").replace(" ", "").trim(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            } else { content }
        } catch (e: Exception) { content }

        val lines = decodedContent.split("\\n", "\r\n", "\n", "\r")
        lines.forEach { line ->
            val uri = line.trim()
            if (uri.isEmpty()) return@forEach
            try {
                when {
                    uri.startsWith("vmess://") -> parseVmess(uri)?.let { servers.add(it) }
                    uri.startsWith("vless://") -> parseVless(uri)?.let { servers.add(it) }
                    uri.startsWith("trojan://") -> parseTrojan(uri)?.let { servers.add(it) }
                }
            } catch (e: Exception) {}
        }
    }

    private fun parseVmess(uri: String): ServerEntity? {
        val b64 = uri.removePrefix("vmess://")
        val jsonStr = String(Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        val json = JSONObject(jsonStr)
        return ServerEntity(name=json.optString("ps","VMess"), protocol="vmess", address=json.optString("add",""), port=json.optInt("port",0), uuid=json.optString("id",""), rawUri=uri)
    }

    private fun parseVless(uri: String): ServerEntity? {
        val parsed = java.net.URI(uri)
        return ServerEntity(name=URLDecoder.decode(parsed.fragment?:"VLESS","UTF-8"), protocol="vless", address=parsed.host?:return null, port=parsed.port.takeIf{it!=-1}?:443, uuid=parsed.userInfo?:return null, rawUri=uri)
    }
    
    private fun parseTrojan(uri: String): ServerEntity? {
        val parsed = java.net.URI(uri)
        return ServerEntity(name=URLDecoder.decode(parsed.fragment?:"Trojan","UTF-8"), protocol="trojan", address=parsed.host?:return null, port=parsed.port.takeIf{it!=-1}?:443, uuid=parsed.userInfo?:return null, rawUri=uri)
    }
}