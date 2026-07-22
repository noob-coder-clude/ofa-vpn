package com.ofa.vpn.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class UpdateManager(private val context: Context) {
    
    private val downloadedCore: File get() = File(context.filesDir, "xray_core_updated")
    
    // مسیری که برنامه باید برای اجرای Xray استفاده کنه
    fun getActiveCorePath(): String {
        return if (downloadedCore.exists()) {
            downloadedCore.absolutePath
        } else {
            // اگه آپدیت نبود، همون فایل پیش‌فرض که با برنامه اومده رو اجرا کن
            File(context.applicationInfo.nativeLibraryDir, "libxray.so").absolutePath
        }
    }

    fun downloadAndInstallCore(callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                val coreUrl = "https://github.com/XTLS/Xray-core/releases/download/v1.8.10/Xray-android-arm64-v8a.zip"
                val url = URL(coreUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "v2rayNG/1.8.5")

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val zipStream = ZipInputStream(conn.inputStream)
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        // فایل اجرایی داخل زیپ معمولا اسمش xray هست
                        if (entry.name == "xray" || entry.name.endsWith("/xray")) {
                            val outputFile = downloadedCore
                            FileOutputStream(outputFile).use { output -> zipStream.copyTo(output) }
                            outputFile.setExecutable(true, true)
                            Log.i("UpdateManager", "Core updated successfully to ${outputFile.absolutePath}")
                            callback(true, "Core updated successfully!")
                            zipStream.close()
                            return@Thread
                        }
                        entry = zipStream.nextEntry
                    }
                    zipStream.close()
                    callback(false, "Xray binary not found in ZIP.")
                } else {
                    callback(false, "HTTP Error: ${conn.responseCode}")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("UpdateManager", "Download failed", e)
                callback(false, "Error: ${e.message}")
            }
        }.start()
    }
}