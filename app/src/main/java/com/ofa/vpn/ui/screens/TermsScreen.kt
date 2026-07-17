package com.ofa.vpn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ofa.vpn.ui.theme.AccentGreen
import com.ofa.vpn.ui.theme.DarkSurface
import com.ofa.vpn.ui.theme.TextGray
import com.ofa.vpn.ui.theme.TextWhite

/**
 * صفحه قوانین و حریم خصوصی
 *
 * هدف: شفافیت کامل — OFA VPN فقط یه کلاینت محلیه،
 * هیچ داده‌ای جمع‌آوری نمی‌کنه.
 */
@Composable
fun TermsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Top bar سفارشی (بدون TopAppBar experimental)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            contentColor = TextWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = TextWhite
                    )
                }
                Text(
                    text = "قوانین و حریم خصوصی",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.width(24.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Section(
                title = "OFA VPN چیست؟",
                body = "OFA VPN یک کلاینت سبک و متن‌باز برای اندرویده. هدفش فقط اینه که " +
                    "اتصال به سرورهای شخصی شما (VLESS / VMess / Trojan و غیره) رو " +
                    "بدون دردسر و با چند تا کلیک فراهم کنه. هیچ سرور اختصاصی‌ای " +
                    "از طرف ما وجود نداره — شما خودتون ساب‌ها یا لینک‌هاتون رو وارد می‌کنید."
            )

            Section(
                title = "جمع‌آوری اطلاعات",
                body = "ما هیچ اطلاعات شخصی‌ای جمع‌آوری نمی‌کنیم. اپ روی دستگاه شماست و " +
                    "همه‌ی تنظیمات (سرورها، ساب‌ها، حالت اتصال) فقط توی حافظه‌ی محلی " +
                    "گوشی شما ذخیره می‌شه. هیچ سروری برای دریافت این داده‌ها وجود نداره."
            )

            Section(
                title = "ترافیک و لاگ",
                body = "ترافیک شبکه فقط از طریق سروری که خودتون معرفی کردید عبور می‌کنه. " +
                    "OFA VPN هیچ لاگی از سایت‌هایی که باز می‌کنید نگه نمی‌داره، هیچ " +
                    "tracking یا analytics شخص ثالث نداره و هیچ تبلیغاتی نشون نمی‌ده."
            )

            Section(
                title = "شبکه و دسترسی‌ها",
                body = "اپ فقط دسترسی‌های ضروری رو می‌گیره: اینترنت (برای اتصال)، " +
                    "مجوز VPN (برای ساخت تونل) و اعلان‌ها (برای نمایش وضعیت). " +
                    "هیچ دسترسی به مخاطبین، پیام‌ها، عکس‌ها یا موقعیت مکانی نداره."
            )

            Section(
                title = "مسئولیت کاربر",
                body = "شما مسئول محتوایی هستید که از طریق این ابزار منتقل می‌شه. " +
                    "OFA VPN ابزاری خنثی برای اتصاله و هیچ مسئولیتی در قبال " +
                    "استفاده‌ی غیرقانونی نمی‌پذیره."
            )

            Section(
                title = "متن‌باز",
                body = "کد کامل اپ روی GitHub منتشر شده و هر کسی می‌تونه بررسی کنه که " +
                    "هیچ کد مخرب یا جمع‌آوری‌کننده‌ی داده‌ای وجود نداره."
            )

            Text(
                text = "نسخه ۱.۰ — آخرین به‌روزرسانی: ۱۴۰۵",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AccentGreen,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite
        )
    }
}
