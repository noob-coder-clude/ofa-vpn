package com.ofa.vpn.core
import android.util.Base64
import com.ofa.vpn.data.local.ServerEntity
import org.json.JSONObject
import java.net.URLDecoder
object ConfigParser {
    fun parseSubscription(rawContent: String): List<ServerEntity> {
        val servers = mutableListOf<ServerEntity>()
        val decodedContent = try { if (!rawContent.startsWith("vmess://") && !rawContent.startsWith("vless://")) String(Base64.decode(rawContent.replace("\\n", ""), Base64.DEFAULT)) else rawContent } catch (e: Exception) { rawContent }
        decodedContent.split("\\n").forEach { line ->
            val uri = line.trim()
            if (uri.isEmpty()) return@forEach
            try { when { uri.startsWith("vmess://") -> parseVmess(uri)?.let { servers.add(it) }; uri.startsWith("vless://") -> parseVless(uri)?.let { servers.add(it) } } } catch (e: Exception) {}
        }
        return servers
    }
    private fun parseVmess(uri: String): ServerEntity? {
        val b64 = uri.removePrefix("vmess://")
        val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
        return ServerEntity(name=json.optString("ps","VMess"), protocol="vmess", address=json.optString("add",""), port=json.optInt("port",0), uuid=json.optString("id",""), rawUri=uri)
    }
    private fun parseVless(uri: String): ServerEntity? {
        val parsed = java.net.URI(uri)
        return ServerEntity(name=URLDecoder.decode(parsed.fragment?:"VLESS","UTF-8"), protocol="vless", address=parsed.host?:return null, port=parsed.port.takeIf{it!=-1}?:443, uuid=parsed.userInfo?:return null, rawUri=uri)
    }
}