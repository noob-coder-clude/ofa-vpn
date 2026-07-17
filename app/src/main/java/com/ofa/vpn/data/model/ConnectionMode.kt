package com.ofa.vpn.data.model

enum class ConnectionMode {
    AUTO,       // پیش‌فرض — بهترین سرور بر اساس یادگیری
    WEB,        // وبگردی — پایداری
    SOCIAL,     // سوشیال — اپ‌های خاص
    GAMING,     // گیمینگ — کم‌ترین پینگ
    PROXY,      // پروکسی — عبور ساده
    AETHER      // aether (anti-DPI) — tunnel مستقل با SOCKS5 داخلی
}
