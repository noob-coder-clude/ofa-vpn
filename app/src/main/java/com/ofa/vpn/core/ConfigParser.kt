package com.ofa.vpn.core

import com.ofa.vpn.data.model.ConnectionMode
import com.ofa.vpn.data.model.Server
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

/**
 * ساخت JSON config برای Xray-core از روی Server + ConnectionMode
 *
 * هر mode روتینگ متفاوتی داره:
 *  - AUTO   : روتینگ پیش‌فرض، همه ترافیک از تونل
 *  - WEB    : DNS override، اولویت مرورگر
 *  - SOCIAL : split tunneling — فقط اپ‌های سوشیال از VPN
 *  - GAMING : UDP relay، بدون DNS hijack، کم‌ترین تاخیر
 *  - PROXY  : متود ترکیبی
 *
 * خروجی: رشته JSON آماده برای پاس دادن به Xray-core
 */
class ConfigParser {

    companion object {
        // اپ‌های سوشیال برای mode SOCIAL (package name)
        val SOCIAL_APPS = listOf(
            "org.telegram.messenger",
            "com.instagram.android",
            "com.whatsapp",
            "com.twitter.android",
            "com.facebook.katana",
            "com.zhiliaoapp.musically" // TikTok
        )

        const val LOCAL_SOCKS_PORT = 10808
        const val LOCAL_HTTP_PORT = 10809
    }

    /**
     * ساخت config کامل Xray
     */
    fun build(server: Server, mode: ConnectionMode): String {
        val root = JSONObject()

        root.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        // ورودی‌ها: SOCKS + HTTP روی localhost که VpnService بهشون tun رو وصل می‌کنه
        root.put("inbounds", buildInbounds(mode))

        // خروجی: سرور اصلی + direct + block
        root.put("outbounds", buildOutbounds(server, mode))

        // روتینگ بر اساس mode
        root.put("routing", buildRouting(mode))

        // DNS
        root.put("dns", buildDns(mode))

        return root.toString(2)
    }

    // ---------- Inbounds ----------

    private fun buildInbounds(mode: ConnectionMode): JSONArray {
        val arr = JSONArray()

        // SOCKS inbound (اصلی — tun2socks بهش وصل می‌شه)
        arr.put(JSONObject().apply {
            put("tag", "socks-in")
            put("port", LOCAL_SOCKS_PORT)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)          // برای گیمینگ لازمه
                put("ip", "127.0.0.1")
            })
            put("sniffing", JSONObject().apply {
                // برای GAMING سنیفینگ خاموش تا تاخیر کم بشه
                put("enabled", mode != ConnectionMode.GAMING)
                put("destOverride", JSONArray().apply {
                    put("http"); put("tls"); put("quic")
                })
            })
        })

        // HTTP inbound
        arr.put(JSONObject().apply {
            put("tag", "http-in")
            put("port", LOCAL_HTTP_PORT)
            put("listen", "127.0.0.1")
            put("protocol", "http")
        })

        return arr
    }

    // ---------- Outbounds ----------

    private fun buildOutbounds(server: Server, mode: ConnectionMode): JSONArray {
        val arr = JSONArray()

        // outbound اصلی: proxy
        arr.put(buildProxyOutbound(server))

        // direct — برای ترافیکی که نباید از تونل بره
        arr.put(JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
            put("settings", JSONObject())
        })

        // block — برای تبلیغات و ترافیک ناخواسته
        arr.put(JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
            put("settings", JSONObject())
        })

        return arr
    }

    /**
     * ساخت outbound از روی نوع پروتکل سرور
     * از rawUri کامل پارس می‌کنه تا هیچ کانفیگی رد نشه
     */
    private fun buildProxyOutbound(server: Server): JSONObject {
        return when (server.protocol.lowercase()) {
            "vless" -> buildVlessOutbound(server)
            "vmess" -> buildVmessOutbound(server)
            "trojan" -> buildTrojanOutbound(server)
            "hysteria2" -> buildHysteria2Outbound(server)
            else -> buildVlessOutbound(server) // fallback
        }
    }

    private fun buildVlessOutbound(server: Server): JSONObject {
        val params = parseUriParams(server.rawUri)

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", server.address)
                    put("port", server.port)
                    put("users", JSONArray().put(JSONObject().apply {
                        put("id", server.uuid)
                        put("encryption", params["encryption"] ?: "none")
                        put("flow", params["flow"] ?: "")
                    }))
                }))
            })
            put("streamSettings", buildStreamSettings(params))
        }
    }

    private fun buildVmessOutbound(server: Server): JSONObject {
        val params = parseUriParams(server.rawUri)
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vmess")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", server.address)
                    put("port", server.port)
                    put("users", JSONArray().put(JSONObject().apply {
                        put("id", server.uuid)
                        put("alterId", (params["alterId"] ?: "0").toIntOrNull() ?: 0)
                        put("security", params["security"] ?: "auto")
                    }))
                }))
            })
            put("streamSettings", buildStreamSettings(params))
        }
    }

    private fun buildTrojanOutbound(server: Server): JSONObject {
        val params = parseUriParams(server.rawUri)
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "trojan")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", server.address)
                    put("port", server.port)
                    put("password", server.uuid)
                }))
            })
            put("streamSettings", buildStreamSettings(params))
        }
    }

    private fun buildHysteria2Outbound(server: Server): JSONObject {
        // Xray-core از hysteria2 مستقیم پشتیبانی نمی‌کنه؛ اینجا ساختار پایه
        // در نسخه sing-box این کامل‌تر می‌شه
        val params = parseUriParams(server.rawUri)
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "hysteria2")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", server.address)
                    put("port", server.port)
                    put("password", server.uuid)
                }))
            })
            put("streamSettings", JSONObject().apply {
                put("security", "tls")
                put("tlsSettings", JSONObject().apply {
                    put("serverName", params["sni"] ?: server.address)
                    put("allowInsecure", params["insecure"] == "1")
                })
            })
        }
    }

    /**
     * تنظیمات لایه انتقال + امنیت (TLS/Reality/WS/gRPC/TCP)
     */
    private fun buildStreamSettings(params: Map<String, String>): JSONObject {
        val network = params["type"] ?: "tcp"
        val security = params["security"] ?: "none"

        return JSONObject().apply {
            put("network", network)
            put("security", security)

            // امنیت
            when (security) {
                "reality" -> put("realitySettings", JSONObject().apply {
                    put("serverName", params["sni"] ?: "")
                    put("fingerprint", params["fp"] ?: "chrome")
                    put("publicKey", params["pbk"] ?: "")
                    put("shortId", params["sid"] ?: "")
                    put("spiderX", params["spx"] ?: "/")
                })
                "tls" -> put("tlsSettings", JSONObject().apply {
                    put("serverName", params["sni"] ?: params["host"] ?: "")
                    put("fingerprint", params["fp"] ?: "chrome")
                    put("allowInsecure", params["allowInsecure"] == "1")
                    params["alpn"]?.let { alpn ->
                        put("alpn", JSONArray().apply {
                            alpn.split(",").forEach { put(it) }
                        })
                    }
                })
            }

            // لایه انتقال
            when (network) {
                "ws" -> put("wsSettings", JSONObject().apply {
                    put("path", params["path"]?.let { decode(it) } ?: "/")
                    put("headers", JSONObject().apply {
                        put("Host", params["host"] ?: "")
                    })
                })
                "grpc" -> put("grpcSettings", JSONObject().apply {
                    put("serviceName", params["serviceName"] ?: params["path"] ?: "")
                    put("multiMode", params["mode"] == "multi")
                })
                "tcp" -> {
                    if (params["headerType"] == "http") {
                        put("tcpSettings", JSONObject().apply {
                            put("header", JSONObject().apply {
                                put("type", "http")
                                put("request", JSONObject().apply {
                                    put("path", JSONArray().put(params["path"] ?: "/"))
                                    put("headers", JSONObject().apply {
                                        put("Host", JSONArray().put(params["host"] ?: ""))
                                    })
                                })
                            })
                        })
                    }
                }
            }
        }
    }

    // ---------- Routing ----------

    private fun buildRouting(mode: ConnectionMode): JSONObject {
        val rules = JSONArray()

        // بلاک تبلیغات (همه mode ها)
        rules.put(JSONObject().apply {
            put("type", "field")
            put("outboundTag", "block")
            put("domain", JSONArray().put("geosite:category-ads-all"))
        })

        when (mode) {
            ConnectionMode.GAMING -> {
                // ترافیک ایران مستقیم، بقیه از پروکسی — تاخیر کم
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("domain", JSONArray().put("geosite:category-ir"))
                })
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("ip", JSONArray().put("geoip:ir").put("geoip:private"))
                })
            }
            ConnectionMode.WEB -> {
                // سایت‌های ایرانی مستقیم
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("domain", JSONArray().put("geosite:category-ir"))
                })
            }
            ConnectionMode.SOCIAL -> {
                // فقط دامنه سوشیال از پروکسی، بقیه مستقیم
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "proxy")
                    put("domain", JSONArray().apply {
                        put("geosite:telegram")
                        put("geosite:instagram")
                        put("geosite:twitter")
                        put("geosite:facebook")
                    })
                })
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("network", "tcp,udp")
                })
            }
            else -> {
                // AUTO / PROXY: ایران مستقیم، بقیه پروکسی
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("ip", JSONArray().put("geoip:ir").put("geoip:private"))
                })
            }
        }

        return JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            put("rules", rules)
        }
    }

    // ---------- DNS ----------

    private fun buildDns(mode: ConnectionMode): JSONObject {
        return JSONObject().apply {
            val servers = JSONArray()
            when (mode) {
                ConnectionMode.GAMING -> {
                    // DNS hijack خاموش — سریع‌ترین
                    servers.put("1.1.1.1")
                    servers.put("8.8.8.8")
                }
                else -> {
                    servers.put("https://1.1.1.1/dns-query")
                    servers.put("8.8.8.8")
                    servers.put(JSONObject().apply {
                        put("address", "223.5.5.5")
                        put("domains", JSONArray().put("geosite:category-ir"))
                    })
                }
            }
            put("servers", servers)
        }
    }

    // ---------- Helpers ----------

    private fun parseUriParams(rawUri: String): Map<String, String> {
        return try {
            val queryStart = rawUri.indexOf('?')
            if (queryStart == -1) return emptyMap()
            val fragmentStart = rawUri.indexOf('#')
            val query = if (fragmentStart > queryStart) {
                rawUri.substring(queryStart + 1, fragmentStart)
            } else {
                rawUri.substring(queryStart + 1)
            }
            query.split("&")
                .mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun decode(s: String): String = try {
        URLDecoder.decode(s, "UTF-8")
    } catch (e: Exception) {
        s
    }
}
