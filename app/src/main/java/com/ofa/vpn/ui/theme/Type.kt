package com.ofa.vpn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ofa.vpn.R

/**
 * خانواده فونت Madika — قابل استفاده برای فارسی، انگلیسی، آلمانی
 *
 * وزن‌های موجود:
 *  - madika_light    (300) → Caption, زیرنویس، جزئیات محو
 *  - madika_regular  (400) → Body, متن معمولی
 *  - madika_bold     (700) → Title, دکمه‌ها، عناوین
 *  - madika_black   (900) → Headline/Display, عناوین اصلی
 *
 * قوانین گرافیک RAIL:
 *  - Hierarchy: Heavy → Light (Black عنوان، Regular بدنه، Light جزئیات)
 *  - کنتراست وزن: عنوان Black + بدنه Regular = خوانایی بالا
 *  - سایز: بزرگ → کوچک (32sp → 11sp)
 *  - Letter spacing: بزرگ‌تر → کوچک‌تر (صفحه نمایش)
 */
val MadikaFontFamily = FontFamily(
    Font(R.font.madika_light, FontWeight.Light),
    Font(R.font.madika_regular, FontWeight.Normal),
    Font(R.font.madika_bold, FontWeight.Bold),
    Font(R.font.madika_black, FontWeight.Black)
)

val OfaTypography = Typography(
    // ===== Display / Headline — Black (900) =====
    // فقط عناوین اصلی: "OFA VPN", عناوین صفحات
    headlineLarge = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // ===== Title — Bold (700) =====
    // دکمه‌ها، عناوین کارت‌ها، عناوین بخش‌ها
    titleLarge = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),

    // ===== Body — Regular (400) =====
    // متن معمولی، توضیحات، لیست سرورها
    bodyLarge = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp,
        lineHeight = 16.sp
    ),

    // ===== Label — Medium/Bold/Light (500-700-300) =====
    // Chips, tags, برچسب‌ها
    labelLarge = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MadikaFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp
    )
)
