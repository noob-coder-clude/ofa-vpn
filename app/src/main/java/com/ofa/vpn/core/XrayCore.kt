package com.ofa.vpn.core

import android.content.Context
import android.util.Log
import java.io.File

class XrayCore(private val context: Context) {
    private var process: Process? = null
    private val updateManager = UpdateManager(context)
    
    private val xrayExecutable: File get() = File(updateManager.getActiveCorePath())
    private val assetDir: File get() = File(context.filesDir, "xray_data")
    private val configJson: File get() = File(assetDir, "config.json")

    init { assetDir.mkdirs() }

    fun start(config: String): Boolean {
        if (!xrayExecutable.exists()) {
            Log.e("XrayCore", "FATAL: Xray binary not found at ${xrayExecutable.absolutePath}")
            return false
        }
        
        try {
            xrayExecutable.setExecutable(true, true)
        } catch (e: Exception) {}

        configJson.writeText(config)
        return try {
            val pb = ProcessBuilder().command(xrayExecutable.absolutePath, "run", "-c", configJson.absolutePath).directory(assetDir).redirectErrorStream(true)
            process = pb.start()
            Thread { process?.inputStream?.bufferedReader()?.use { r -> var l: String?; while (r.readLine().also { l = it } != null) Log.i("XrayLog", l ?: "") } }.start()
            Thread.sleep(500)
            process?.isAlive == true
        } catch (e: Exception) { Log.e("XrayCore", "Failed", e); false }
    }

    fun stop() { process?.let { if (it.isAlive) it.destroy() }; process = null }
}