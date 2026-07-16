package com.ofa.vpn.data.model

/**
 * وضعیت اتصال VPN
 * based on REAL connection status, not just ping
 */
enum class ConnectionState {
    DISCONNECTED,      // قطع
    CONNECTING,        // در حال اتصال
    CONNECTED,          // وصل شده
    RECONNECTING,       // در حال وصل مجدد (fallback)
    ERROR               // خطا
}

/**
 * وضعیت سلامت اتصال
 * این قلب OFA VPN هست — بر اساس این تصمیم میگیره
 */
data class ConnectionHealth(
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val currentServer: Server? = null,
    val ping: Int = 0,
    val downloadSpeed: Long = 0,        // bytes/s
    val uploadSpeed: Long = 0,          // bytes/s
    val totalDownload: Long = 0,        // bytes
    val totalUpload: Long = 0,          // bytes
    val connectedSince: Long = 0,        // timestamp
    val errorMessage: String? = null
)
