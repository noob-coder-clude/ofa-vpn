package com.ofa.vpn.data.model

/**
 * ۵ حالت اتصال OFA VPN
 * One For All — یه کلیک، همه‌چیز خودکار
 */
enum class ConnectionMode(
    val labelFa: String,
    val labelEn: String,
    val labelDe: String,
    val icon: String,
    val description: String
) {
    AUTO(
        labelFa = "خودکار",
        labelEn = "Auto",
        labelDe = "Automatik",
        icon = "✨",
        description = "بهترین سرور به صورت خودکار انتخاب میشه"
    ),
    WEB(
        labelFa = "وب‌گردی",
        labelEn = "Web Browsing",
        labelDe = "Web-Browsing",
        icon = "🌐",
        description = "پینگ پایین، پایداری بالا، مرورگر اولویت"
    ),
    SOCIAL(
        labelFa = "سوشیال مدیا",
        labelEn = "Social Media",
        labelDe = "Soziale Medien",
        icon = "📱",
        description = "تلگرام، اینستاگرام، واتساپ — split tunneling"
    ),
    GAMING(
        labelFa = "گیمینگ",
        labelEn = "Gaming",
        labelDe = "Gaming",
        icon = "🎮",
        description = "UDP relay، کم‌ترین تاخیر، DNS hijack خاموش"
    ),
    PROXY(
        labelFa = "پروکسی",
        labelEn = "Proxy Mode",
        labelDe = "Proxy-Modus",
        icon = "🔀",
        description = "متود ترکیبی با ریپوهای اضافه"
    )
}
