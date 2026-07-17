package com.ofa.vpn.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * آپدیت داخلی برنامه — بدون نیاز به بازار/Google Play
 *
 * جریان:
 *  1. checkUpdate() → دریافت JSON از UPDATE_URL
 *  2. مقایسه versionCode با نصب‌شده
 *  3. اگه جدیدتر بود → downloadAndInstall() لینک رو می‌گیره و APK رو نصب می‌کنه
 *
 * فرمت JSON سرور:
 * {
 *   "versionCode": 3,
 *   "versionName": "0.0.3",
 *   "apkUrl": "https://example.com/ofa-vpn-debug.apk",
 *   "notes": "رفع باگ + سرورهای جدید",
 *   "minRequired": 2
 * }
 */
@Singleton
class UpdateManager @Inject constructor(
    private val context: Context,
    private val client: OkHttpClient
) {
    companion object {
        // TODO: آدرس ریپو خودت رو بذار (مثلاً release لیست GitHub یا سرور خودت)
        const val UPDATE_URL = "https://raw.githubusercontent.com/noob-coder-clude/ofa-vpn/master/update.json"
        const val UPDATE_CHANNEL = "debug" // debug | release
    }

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val notes: String,
        val minRequired: Int
    )

    data class UpdateResult(
        val hasUpdate: Boolean,
        val info: UpdateInfo?,
        val currentCode: Int
    )

    suspend fun checkUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        val currentCode = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
                .longVersionCode.toInt()
        } catch (e: Exception) {
            0
        }

        try {
            val req = Request.Builder().url(UPDATE_URL).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext UpdateResult(false, null, currentCode)

            val json = JSONObject(resp.body?.string() ?: "{}")
            val info = UpdateInfo(
                versionCode = json.optInt("versionCode", 0),
                versionName = json.optString("versionName", ""),
                apkUrl = json.optString("apkUrl", ""),
                notes = json.optString("notes", ""),
                minRequired = json.optInt("minRequired", 0)
            )
            UpdateResult(info.versionCode > currentCode, info, currentCode)
        } catch (e: Exception) {
            UpdateResult(false, null, currentCode)
        }
    }

    /**
     * دانلود APK و اجرای installer
     * نیاز به مجوز REQUEST_INSTALL_PACKAGES (در manifest اضافه شده)
     */
    suspend fun downloadAndInstall(apkUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(apkUrl).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext false

            val file = File(context.externalCacheDir, "ofa-update.apk")
            file.outputStream().use { out ->
                resp.body?.byteStream()?.copyTo(out)
            }

            installApk(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun installApk(file: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
