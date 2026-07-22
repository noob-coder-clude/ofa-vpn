package com.ofa.vpn.core

import android.util.Base64
import com.ofa.vpn.data.local.ServerEntity
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

object ConfigParser {

    fun parseSubscription(rawContent: String): List<ServerEntity> {
        val servers = mutableListOf<ServerEntity>()
        val cleanContent = rawContent.trim()

        try {
            when {
                // ۱. پشتیبانی از فرمت Clash با Regex
                cleanContent.contains("proxies:") -> parseClashWithRegex(cleanContent, servers)
                
                // ۲. پشتیبانی از JSON (اعم از آرایه و آبجکت)
                cleanContent.startsWith("[") || cleanContent.startsWith("{") -> parseJson(cleanContent, servers)
                
                // ۳. پشتیبانی از Base64 و متن خام
                else -> parseBase64OrPlain(cleanContent, servers)
            }
        } catch (e: Exception) {
            throw Exception("Parse Error: ${e.message}")
        }
        return servers
    }

    private fun parseClashWithRegex(content: String, servers: MutableList<ServerEntity>) {
        val proxyRegex = Regex("(?s)- \\{.*?name: (.*?)\\n.*?type: (.*?)\\n.*?server: (.*?)\\n.*?port: (.*?)\\n.*?(?:uuid: (.*?)\\n)?.*?\\}")
        proxyRegex.findAll(content).forEach { match ->
            try {
                val name = match.groupValues[1].trim().replace("\"", "")
                val type = match.groupValues[2].trim()
                val server = match.groupValues[3].trim()
                val port = match.groupValues[4].trim().toIntOrNull() ?: return@forEach
                val uuid = match.groupValues[5].trim()

                if (type == "vmess" || type == "vless") {
                    val rawUri = if (type == "vmess") {
                        val vmessJson = JSONObject()
                        vmessJson.put("v", "2"); vmessJson.put("ps", name); vmessJson.put("add", server); vmessJson.put("port", port)
                        vmessJson.put("id", uuid); vmessJson.put("aid", 0); vmessJson.put("net", "tcp"); vmessJson.put("tls", "")
                        "vmess://" + Base64.encodeToString(vmessJson.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                    } else {
                        "vless://$uuid@$server:$port?encryption=none#$name"
                    }
                    servers.add(ServerEntity(name=name, protocol=type, address=server, port=port, uuid=uuid, rawUri=rawUri))
                }
            } catch (e: Exception) {}
        }
    }

    private fun parseJson(content: String, servers: MutableList<ServerEntity>) {
        val root = JSONObject(content)
        
        // اگه فرمت استاندارد کانفیگ Xray بود (داشتن outbounds)
        if (root.has("outbounds")) {
            val outbounds = root.getJSONArray("outbounds")
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                val type = outbound.optString("type")
                if (type == "vmess" || type == "vless" || type == "trojan") {
                    val name = outbound.optString("tag", "Unknown JSON")
                    val serverAddr = outbound.optString("server")
                    val serverPort = outbound.optInt("server_port", 443)
                    val uuid = outbound.optString("uuid")
                    
                    // ذخیره کل JSON به عنوان rawUri برای استفاده مستقیم در ConfigBuilder
                    servers.add(ServerEntity(name=name, protocol=type, address=serverAddr, port=serverPort, uuid=uuid, rawUri=outbound.toString()))
                }
            }
        } 
        // اگه فرمت آرایه‌ای از VMess بود
        else if (root.has("add") && root.has("id")) {
            val b64 = Base64.encodeToString(root.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            servers.add(ServerEntity(name=root.optString("ps","VMess"), protocol="vmess", address=root.optString("add"), port=root.optInt("port"), uuid=root.optString("id"), rawUri="vmess://$b64"))
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