package com.ofa.vpn.service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ofa.vpn.core.XrayCore
class VpnConnectionService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayCore: XrayCore? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) { "START" -> startVpn(); "STOP" -> stopVpn() }
        return START_STICKY
    }
    private fun startVpn() {
        if (vpnInterface != null) return
        try {
            val builder = Builder()
            builder.setSession("OFA_VPN_Session"); builder.addAddress("10.0.0.2", 30); builder.addRoute("0.0.0.0/0", 0); builder.addDnsServer("1.1.1.1"); builder.setMtu(1500)
            vpnInterface = builder.establish()
            if (vpnInterface == null) { stopSelf(); return }
            val dummyConfig = "{\"inbounds\":[{\"port\":10808,\"protocol\":\"socks\",\"settings\":{\"udp\":true}}]}"
            xrayCore = XrayCore(this)
            if (xrayCore!!.start(dummyConfig)) Log.i("VpnService", "VPN Started") else stopVpn()
        } catch (e: Exception) { stopVpn() }
    }
    private fun stopVpn() { try { xrayCore?.stop(); xrayCore=null; vpnInterface?.close(); vpnInterface=null } catch (e: Exception) {} finally { stopSelf() } }
    override fun protect(socket: java.net.Socket): Boolean { return super.protect(socket) }
    override fun protect(socket: java.net.DatagramSocket): Boolean { return super.protect(socket) }
}