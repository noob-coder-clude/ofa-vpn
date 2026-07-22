package com.ofa.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ofa.vpn.core.ConfigBuilder
import com.ofa.vpn.core.XrayCore
import com.ofa.vpn.data.local.ServerEntity
import com.google.gson.Gson
import java.io.File

class VpnConnectionService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayCore: XrayCore? = null
    private var tun2socksProcess: Process? = null

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
            builder.setSession("OFA_VPN_Session")
            builder.addAddress("10.0.0.2", 30)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer("1.1.1.1")
            builder.setMtu(1500)
            vpnInterface = builder.establish()

            if (vpnInterface == null) { stopSelf(); return }

            // ۲. اجرای Xray
            val config = ConfigBuilder.build(server)
            xrayCore = XrayCore(this)
            if (!xrayCore!!.start(config)) {
                Log.e("VpnService", "Xray failed to start.")
                stopVpn()
                return
            }

            // ۳. اجرای tun2socks
            val tun2socksBinary = File(applicationInfo.nativeLibraryDir, "libtun2socks.so")
            if (tun2socksBinary.exists()) {
                val tunFd = vpnInterface!!.detachFd()
                
                // باینری xjasonlyu/tun2socks با سوئیچ -fd شناسه رو میگیره
                val pb = ProcessBuilder(
                    tun2socksBinary.absolutePath,
                    "-device", "fd://$tunFd",
                    "-proxy", "socks5://127.0.0.1:10808",
                    "-loglevel", "warning"
                ).redirectErrorStream(true)
                
                tun2socksProcess = pb.start()
                
                Thread { tun2socksProcess?.inputStream?.bufferedReader()?.use { r -> var l: String?; while (r.readLine().also { l = it } != null) Log.i("Tun2Socks", l ?: "") } }.start()
                Log.i("VpnService", "tun2socks started with FD: $tunFd")
            } else {
                Log.e("VpnService", "libtun2socks.so not found!")
            }

        } catch (e: Exception) {
            Log.e("VpnService", "Error starting VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            tun2socksProcess?.destroy()
            tun2socksProcess = null
            xrayCore?.stop()
            xrayCore = null
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