package com.ofa.vpn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // نمایشی: "Germany - Frankfurt"
    val protocol: String,               // vless, trojan, vmess, hysteria2
    val address: String,                // host یا IP
    val port: Int,
    val uuid: String,                   // UUID یا password
    val remark: String,                 // توضیحات از پنل
    val rawUri: String,                 // کل URI خام (vless://...)
    val subscriptionId: Long = 0,       // کدوم ساب
    val ping: Int = 0,                  // latency به ms
    val isFavorite: Boolean = false,
    val lastConnected: Long = 0          // timestamp آخرین اتصال
)
