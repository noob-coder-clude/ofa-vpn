package com.ofa.vpn.core

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class UpdateManager(private val context: Context) {
    
    private val downloadedCore: File get() = File(context.filesDir, "mihomo_core_updated")
    
    // مسیری که برنامه باید برای اجرای Mihomo استفاده کنه
    fun getActiveCorePath(): String {
        return if (downloadedCore.exists()) {
            downloadedCore.absolutePath
        } else {
            File(context.applicationInfo.nativeLibraryDir, "libmihomo.so").absolutePath
        }
    }

    fun downloadAndInstallCore(callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                // ۱. گرفتن اطلاعات آخرین ریلیز از گیت‌هاب
                val apiUrl = "https://api.github.com/repos/MetaCubeX/mihomo/releases/latest"
                val apiConn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "OFA-VPN-Updater")
                }
                
                if (apiConn.responseCode != HttpURLConnection.HTTP_OK) {
                    callback(false, "API Error: ${apiConn.responseCode}")
                    return@Thread
                }
                
                val apiResponse = apiConn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(apiResponse)
                val assets = json.getJSONArray("assets")
                
                var downloadUrl: String? = null
                var versionTag = json.optString("tag_name", "unknown")
                
                // ۲. پیدا کردن لینک دانلود نسخه arm64
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.contains("android") && name.contains("arm64") && name.endsWith(".gz")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                
                if (downloadUrl == null) {
                    callback(false, "arm64 binary not found in release $versionTag")
                    return@Thread
                }

                // ۳. دانلود و استخراج فایل GZ
                val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "OFA-VPN-Updater")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    GZIPInputStream(conn.inputStream).use { gzipInput ->
                        FileOutputStream(downloadedCore).use { output ->
                            gzipInput.copyTo(output)
                        }
                    }
                    downloadedCore.setExecutable(true, true)
                    Log.i("UpdateManager", "Mihomo updated to $versionTag at ${downloadedCore.absolutePath}")
                    callback(true, "Mihomo updated to $versionTag!")
                } else {
                    callback(false, "Download Error: ${conn.responseCode}")
                }
                conn.disconnect()
                
            } catch (e: Exception) {
                Log.e("UpdateManager", "Download failed", e)
                callback(false, "Error: ${e.message}")
            }
        }.start()
    }
}