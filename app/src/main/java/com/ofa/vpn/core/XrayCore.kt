package com.ofa.vpn.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper هسته Xray-core
 *
 * دو روش اجرا پشتیبانی می‌شه:
 *  1. Process-based: باینری xray در assets → اجرا با ProcessBuilder
 *  2. JNI (libxray.so): در نسخه بعدی
 *
 * این کلاس فقط lifecycle هسته رو مدیریت می‌کنه:
 *  start(config) → اجرا با config JSON
 *  stop()        → توقف
 *  isRunning()   → وضعیت
 *
 * نکته: config توسط ConfigParser ساخته می‌شه
 */
class XrayCore(private val context: Context) {

    companion object {
        private const val TAG = "XrayCore"
        private const val XRAY_BINARY = "libxray.so"   // در jniLibs قرار می‌گیره
        private const val CONFIG_FILE = "xray_config.json"
        private const val GEOIP_FILE = "geoip.dat"
        private const val GEOSITE_FILE = "geosite.dat"
    }

    private var process: Process? = null
    @Volatile
    private var running = false

    /**
     * مسیر باینری xray در پوشه native lib
     * (باینری به صورت libxray.so پکیج می‌شه تا Android اجازه اجرا بده)
     */
    private val xrayBinaryPath: String
        get() = File(context.applicationInfo.nativeLibraryDir, XRAY_BINARY).absolutePath

    private val configPath: String
        get() = File(context.filesDir, CONFIG_FILE).absolutePath

    /**
     * آماده‌سازی فایل‌های asset (geoip/geosite) در filesDir
     * فقط بار اول کپی می‌شه
     */
    suspend fun prepareAssets() = withContext(Dispatchers.IO) {
        try {
            copyAssetIfNeeded(GEOIP_FILE)
            copyAssetIfNeeded(GEOSITE_FILE)
        } catch (e: Exception) {
            Log.e(TAG, "خطا در آماده‌سازی asset: ${e.message}")
        }
    }

    private fun copyAssetIfNeeded(name: String) {
        val dest = File(context.filesDir, name)
        if (dest.exists() && dest.length() > 0) return
        try {
            context.assets.open(name).use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "asset $name موجود نیست (اختیاری): ${e.message}")
        }
    }

    /**
     * اجرای هسته با config
     * @param configJson خروجی ConfigParser.build()
     * @return true اگه با موفقیت شروع شد
     */
    suspend fun start(configJson: String): Boolean = withContext(Dispatchers.IO) {
        if (running) {
            Log.w(TAG, "هسته از قبل در حال اجراست")
            return@withContext true
        }

        try {
            // نوشتن config روی دیسک
            File(configPath).writeText(configJson)

            val binary = File(xrayBinaryPath)
            if (!binary.exists()) {
                Log.e(TAG, "باینری xray پیدا نشد: $xrayBinaryPath")
                return@withContext false
            }

            // اجرا: xray run -c config.json
            val pb = ProcessBuilder(
                xrayBinaryPath,
                "run",
                "-c", configPath
            ).apply {
                // متغیرهای محیطی برای مسیر asset
                environment()["XRAY_LOCATION_ASSET"] = context.filesDir.absolutePath
                redirectErrorStream(true)
                directory(context.filesDir)
            }

            process = pb.start()
            running = true

            // لاگ خروجی هسته در thread جدا
            Thread {
                try {
                    process?.inputStream?.bufferedReader()?.forEachLine { line ->
                        Log.d(TAG, "xray: $line")
                    }
                } catch (e: Exception) {
                    // process خاتمه یافت
                }
            }.start()

            // چک کوتاه که process بلافاصله کرش نکنه
            Thread.sleep(300)
            if (process?.isAlive != true) {
                running = false
                Log.e(TAG, "هسته بلافاصله بعد از شروع کرش کرد")
                return@withContext false
            }

            Log.i(TAG, "هسته Xray با موفقیت شروع شد")
            true
        } catch (e: Exception) {
            running = false
            Log.e(TAG, "خطا در شروع هسته: ${e.message}", e)
            false
        }
    }

    /**
     * توقف هسته
     */
    fun stop() {
        try {
            process?.destroy()
            // اگه بعد از مهلت هنوز زنده بود، اجباری کشته بشه
            if (process?.isAlive == true) {
                Thread.sleep(500)
                process?.destroyForcibly()
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطا در توقف هسته: ${e.message}")
        } finally {
            process = null
            running = false
            Log.i(TAG, "هسته Xray متوقف شد")
        }
    }

    fun isRunning(): Boolean = running && process?.isAlive == true
}
