package com.ofa.vpn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                   // "Railway 3x-ui"
    val url: String,                    // https://my-panel.up.railway.app/sub/xxx
    val lastUpdate: Long = 0,           // timestamp آخرین آپدیت
    val serverCount: Int = 0,           // تعداد کانفیگ‌های پارس شده
    val isEnabled: Boolean = true
)
