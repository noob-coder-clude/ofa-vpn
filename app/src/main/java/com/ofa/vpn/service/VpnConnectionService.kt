package com.ofa.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ofa.vpn.data.local.ServerEntity
import com.google.gson.Gson
import java.io.File

class VpnConnectionService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var aetherProcess: Process? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                val serverJson = intent.getStringExtra("server_config")
                if (serverJson != null) {
                    val server = Gson().fromJson(serverJson, ServerEntity::class.java)
                    startVpn(server)
                } else { stopSelf() }
            }
            "STOP" -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(server: ServerEntity) {
        if (vpnInterface != null) return
        try {
            // ۱. ساخت رابط TUN
            val builder = Builder()
            builder.setSession("OFA_Aether_Session")
            builder.addAddress("10.0.0.2", 30)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer("1.1.1.1")
            builder.setMtu(1500)
            vpnInterface = builder.establish()

            if (vpnInterface == null) { stopSelf(); return }

            val tunFd = vpnInterface!!.detachFd()
            val aetherBinary = File(applicationInfo.nativeLibraryDir, "libaether.so")
            
            if (aetherBinary.exists()) {
                aetherBinary.setExecutable(true, true)
                val workDir = File(filesDir, "aether_data")
                workDir.mkdirs()
                
                // ذخیره کانفیگ سرور (موقعیت اتصال Aether به کانفیگ بستگی داره)
                val configFile = File(workDir, "config.json")
                configFile.writeText(server.rawUri)
                
                // اجرای Aether (میتونه با سوئیچ‌های خاص خودش اجرا بشه)
                val pb = ProcessBuilder(
                    aetherBinary.absolutePath, 
                    "-f", configFile.absolutePath, 
                    "-d", workDir.absolutePath,
                    "-fd", tunFd.toString() // پاس دادن شناسه TUN
                ).redirectErrorStream(true)
                
                aetherProcess = pb.start()
                
                // خوندن لاگ‌های Aether
                Thread { aetherProcess?.inputStream?.bufferedReader()?.use { r -> var l: String?; while (r.readLine().also { l = it } != null) Log.i("AetherCore", l ?: "") } }.start()
                Log.i("VpnService", "Aether started with TUN FD: $tunFd")
            } else {
                Log.e("VpnService", "libaether.so not found!")
                stopVpn()
            }

        } catch (e: Exception) {
            Log.e("VpnService", "Error starting VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            aetherProcess?.destroy()
            aetherProcess = null
            vpnInterface?.close()
            vpnInterface = null
            Log.i("VpnService", "VPN Stopped completely.")
        } catch (e: Exception) {} 
        finally { stopSelf() }
    }

    override fun onDestroy() { stopVpn(); super.onDestroy() }
    override fun protect(socket: java.net.Socket): Boolean { return super.protect(socket) }
    override fun protect(socket: java.net.DatagramSocket): Boolean { return super.protect(socket) }
}