package com.ofa.vpn.data.remote
import com.ofa.vpn.core.ConfigParser
import com.ofa.vpn.data.local.ServerEntity
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLException

object SubFetcher {
    fun fetchAndParse(subUrl: String): List<ServerEntity> {
        val conn = (URL(subUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            // استفاده از User-Agent معروف برای دور زدن محدودیت‌های سرور
            setRequestProperty("User-Agent", "v2rayNG/1.8.5")
        }
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val rawContent = conn.inputStream.bufferedReader().use { it.readText() }
                return ConfigParser.parseSubscription(rawContent)
            } else {
                throw Exception("HTTP $code (Server blocked request)")
            }
        } catch (e: SSLException) {
            throw Exception("SSL Error: ${e.message}")
        } catch (e: java.net.UnknownHostException) {
            throw Exception("DNS Error (Cannot resolve host)")
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("Timeout (No internet or blocked)")
        } catch (e: Exception) {
            throw Exception("${e.javaClass.simpleName}: ${e.message}")
        } finally {
            conn.disconnect()
        }
    }
}