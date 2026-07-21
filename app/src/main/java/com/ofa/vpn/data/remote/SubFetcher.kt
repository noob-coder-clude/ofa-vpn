package com.ofa.vpn.data.remote
import com.ofa.vpn.core.ConfigParser
import com.ofa.vpn.data.local.ServerEntity
import java.net.HttpURLConnection
import java.net.URL
object SubFetcher {
    fun fetchAndParse(subUrl: String): List<ServerEntity> {
        val conn = (URL(subUrl).openConnection() as HttpURLConnection).apply { requestMethod="GET"; connectTimeout=15000; readTimeout=15000; setRequestProperty("User-Agent", "OFA-VPN/1.0") }
        try { if (conn.responseCode == HttpURLConnection.HTTP_OK) { return ConfigParser.parseSubscription(conn.inputStream.bufferedReader().use { it.readText() }) } else { throw Exception("HTTP Error: ${conn.responseCode}") } } finally { conn.disconnect() }
    }
}