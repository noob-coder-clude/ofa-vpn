package com.ofa.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ofa.vpn.core.ConfigBuilder
import com.ofa.vpn.core.XrayCore
import com.ofa.vpn.data.local.ServerEntity
import com.google.gson.Gson

class VpnConnectionService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayCore: XrayCore? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                val serverJson = intent.getStringExtra("server_config")
                if (serverJson != null) {
                    val server = Gson().fromJson(serverJson, ServerEntity::class.java)
                    startVpn(server)
                } else {
                    Log.e("VpnService", "No server config provided!")
                    stopSelf()
                }
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
            builder.addRoute("0.0.0.0/0", 0)
            builder.addDnsServer("1.1.1.1")
            builder.setMtu(1500)
            vpnInterface = builder.establish()

            if (vpnInterface == null) { stopSelf(); return }

            val config = ConfigBuilder.build(server)
            xrayCore = XrayCore(this)
            if (xrayCore!!.start(config)) {
                Log.i("VpnService", "VPN Started & Xray connected to ${server.name}")
                // Note: tun2socks is needed here for real traffic routing in Phase 4
            } else {
                Log.e("VpnService", "Xray failed to start.")
                stopVpn()
            }
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            xrayCore?.stop()
            xrayCore = null
            vpnInterface?.close()
            vpnInterface = null
            Log.i("VpnService", "VPN Stopped.")
        } catch (e: Exception) {} 
        finally { stopSelf() }
    }

    override fun protect(socket: java.net.Socket): Boolean { return super.protect(socket) }
    override fun protect(socket: java.net.DatagramSocket): Boolean { return super.protect(socket) }
}