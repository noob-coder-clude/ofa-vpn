package com.ofa.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ofa.vpn.core.MihomoConfigBuilder
import com.ofa.vpn.data.local.ServerEntity
import com.google.gson.Gson
import java.io.File

class VpnConnectionService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var mihomoProcess: Process? = null

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
            val builder = Builder()
            builder.setSession("OFA_VPN_Session")
            builder.addAddress("10.0.0.2", 30)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer("1.1.1.1")
            builder.setMtu(1500)
            vpnInterface = builder.establish()

            if (vpnInterface == null) { stopSelf(); return }

            val tunFd = vpnInterface!!.detachFd()
            val mihomoBinary = File(applicationInfo.nativeLibraryDir, "libmihomo.so")
            
            if (mihomoBinary.exists()) {
                val workDir = File(filesDir, "mihomo_data")
                workDir.mkdirs()
                val configFile = File(workDir, "config.yaml")
                
                val configYaml = MihomoConfigBuilder.build(server, tunFd)
                configFile.writeText(configYaml)
                
                val pb = ProcessBuilder(
                    mihomoBinary.absolutePath, 
                    "-f", configFile.absolutePath, 
                    "-d", workDir.absolutePath
                ).redirectErrorStream(true)
                
                mihomoProcess = pb.start()
                
                Thread { mihomoProcess?.inputStream?.bufferedReader()?.use { r -> var l: String?; while (r.readLine().also { l = it } != null) Log.i("Mihomo", l ?: "") } }.start()
                Log.i("VpnService", "Mihomo started with TUN FD: $tunFd")
            } else {
                Log.e("VpnService", "libmihomo.so not found!")
                stopVpn()
            }

        } catch (e: Exception) {
            Log.e("VpnService", "Error starting VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            mihomoProcess?.destroy()
            mihomoProcess = null
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