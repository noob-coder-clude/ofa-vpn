package com.ofa.vpn.core.aether

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدیریت باینری aether داخل APK
 *
 *  - باینری در assets/aether-arm64 بسته‌بندی شده
 *  - اولین اجرا: از assets به filesDir/aether کپی می‌شه + chmod +x
 *  - aether به عنوان process جدا اجرا می‌شه (SOCKS5 روی 127.0.0.1:1819)
 *  - Xray به اون SOCKS5 وصل می‌شه
 */
@Singleton
class AetherManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "OFA_Aether"
        private const val ASSET_NAME = "aether-arm64"
        private const val BIN_NAME = "aether"
        const val SOCKS_HOST = "127.0.0.1"
        const val SOCKS_PORT = 1819
    }

    private var process: Process? = null
    private lateinit var binFile: File

    /**
     * استخراج باینری از assets (فقط اولین بار یا وقتی آپدیت بشه)
     */
    suspend fun extractIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        binFile = File(context.filesDir, BIN_NAME)
        if (binFile.exists() && binFile.canExecute() && binFile.length() > 100_000) {
            return@withContext true
        }
        try {
            context.assets.open(ASSET_NAME).use { input ->
                binFile.outputStream().use { out ->
                    input.copyTo(out)
                }
            }
            binFile.setExecutable(true)
            Log.i(TAG, "aether extracted: ${binFile.absolutePath} (${binFile.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "extract failed: ${e.message}", e)
            false
        }
    }

    /**
     * اجرای aether با تنظیمات پیش‌فرض (MASQUE + SOCKS5 محلی)
     */
    suspend fun start(
        protocol: String = "masque",
        noize: String = "firewall",
        scan: String = "balanced"
    ): Boolean = withContext(Dispatchers.IO) {
        if (!::binFile.isInitialized) {
            if (!extractIfNeeded()) return@withContext false
        }
        if (process?.isAlive == true) {
            Log.i(TAG, "aether already running")
            return@withContext true
        }
        try {
            val cmd = arrayOf(
                binFile.absolutePath,
                "--$protocol",
                "--scan", scan,
                "--noize", noize,
                "--bind", "$SOCKS_HOST:$SOCKS_PORT"
            )
            Log.i(TAG, "starting aether: ${cmd.joinToString(" ")}")
            process = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .directory(context.filesDir)
                .start()
            true
        } catch (e: Exception) {
            Log.e(TAG, "start failed: ${e.message}", e)
            false
        }
    }

    /**
     * توقف aether
     */
    fun stop() {
        try {
            process?.destroy()
            process = null
            Log.i(TAG, "aether stopped")
        } catch (e: Exception) {
            Log.e(TAG, "stop failed: ${e.message}", e)
        }
    }

    fun isRunning(): Boolean = process?.isAlive == true

    /**
     * جایگزینی باینری با نسخه جدید (آپدیت داخلی)
     */
    suspend fun replaceBinary(newBinary: File): Boolean = withContext(Dispatchers.IO) {
        stop()
        try {
            val target = File(context.filesDir, BIN_NAME)
            newBinary.inputStream().use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            }
            target.setExecutable(true)
            Log.i(TAG, "aether binary replaced: ${target.length()} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "replace failed: ${e.message}", e)
            false
        }
    }

    fun getBinaryFile(): File = binFile
}
