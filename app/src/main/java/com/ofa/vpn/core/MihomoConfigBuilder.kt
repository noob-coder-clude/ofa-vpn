package com.ofa.vpn.core

import com.ofa.vpn.data.local.ServerEntity
import org.json.JSONObject
import android.util.Base64

object MihomoConfigBuilder {
    fun build(server: ServerEntity, tunFd: Int): String {
        val proxyYaml = StringBuilder()
        
        // تبدیل سرور به فرمت Mihomo Proxy
        when (server.protocol) {
            "vmess" -> {
                val b64 = server.rawUri.removePrefix("vmess://")
                val json = JSONObject(String(Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)))
                
                proxyYaml.append("  - name: \"${server.name}\"\n")
                proxyYaml.append("    type: vmess\n")
                proxyYaml.append("    server: ${server.address}\n")
                proxyYaml.append("    port: ${server.port}\n")
                proxyYaml.append("    uuid: ${server.uuid}\n")
                proxyYaml.append("    alterId: ${json.optInt("aid", 0)}\n")
                proxyYaml.append("    cipher: auto\n")
                if (json.optString("tls") == "tls") {
                    proxyYaml.append("    tls: true\n")
                    proxyYaml.append("    servername: ${json.optString("sni", server.address)}\n")
                }
                if (json.optString("net") == "ws") {
                    proxyYaml.append("    network: ws\n")
                    proxyYaml.append("    ws-opts:\n")
                    proxyYaml.append("      path: \"${json.optString("path", "/")}\"\n")
                    proxyYaml.append("      headers:\n")
                    proxyYaml.append("        Host: ${json.optString("host", server.address)}\n")
                }
            }
            "vless", "trojan" -> {
                // اگه از قبل JSON بود (مثل کانفیگ xhttp)
                if (server.rawUri.trim().startsWith("{")) {
                    val json = JSONObject(server.rawUri)
                    proxyYaml.append("  - name: \"${json.optString("tag", server.name)}\"\n")
                    proxyYaml.append("    type: ${json.optString("type")}\n")
                    proxyYaml.append("    server: ${json.optString("server")}\n")
                    proxyYaml.append("    port: ${json.optInt("server_port", 443)}\n")
                    proxyYaml.append("    uuid: ${json.optString("uuid")}\n")
                    val tls = json.optJSONObject("tls")
                    if (tls != null && tls.optBoolean("enabled")) {
                        proxyYaml.append("    tls: true\n")
                        proxyYaml.append("    servername: ${tls.optString("server_name")}\n")
                        val utls = tls.optJSONObject("utls")
                        if (utls != null && utls.optBoolean("enabled")) {
                            proxyYaml.append("    client-fingerprint: ${utls.optString("fingerprint", "chrome")}\n")
                        }
                    }
                    val transport = json.optJSONObject("transport")
                    if (transport != null) {
                        proxyYaml.append("    network: ${transport.optString("type")}\n")
                    }
                } else {
                    val parsed = java.net.URI(server.rawUri)
                    proxyYaml.append("  - name: \"${server.name}\"\n")
                    proxyYaml.append("    type: ${server.protocol}\n")
                    proxyYaml.append("    server: ${server.address}\n")
                    proxyYaml.append("    port: ${server.port}\n")
                    proxyYaml.append("    password: ${server.uuid}\n")
                    proxyYaml.append("    sni: ${server.address}\n")
                    proxyYaml.append("    skip-cert-verify: true\n")
                }
            }
        }

        return """
mixed-port: 7890
allow-lan: false
mode: rule
log-level: warning
tun:
  enable: true
  stack: system
  device: "fd://$tunFd"
  auto-route: true
  auto-detect-interface: false
  dns-hijack:
    - any:53
dns:
  enable: true
  listen: 0.0.0.0:1053
  nameserver:
    - 1.1.1.1
proxies:
 $proxyYaml
proxy-groups:
  - name: "OFA-VPN"
    type: select
    proxies:
      - "${server.name}"
rules:
  - MATCH,OFA-VPN
""".trimIndent()
    }
}