# OFA VPN — پرامپت کامل برای Arena.ai

## معرفی پروژه

OFA VPN یک اپلیکیشن VPN اندروید با Kotlin + Jetpack Compose است که از Xray-core و aether استفاده می‌کند. مخزن GitHub: `https://github.com/noob-coder-clude/ofa-vpn`

هدف: یک APK واحد که کاربر فقط دکمه اتصال را بزند و VPN وصل شود — بدون Termux، بدون تنظیم دستی.

## ساختار فعلی پروژه

### فایل‌های اصلی:
```
app/src/main/java/com/ofa/vpn/
├── MainActivity.kt
├── OFAVPN.kt                          (Application + Hilt)
├── core/
│   ├── ConfigParser.kt                 (ساخت JSON config برای Xray)
│   ├── PingManager.kt                  (انتخاب هوشمند سرور)
│   ├── XrayCore.kt                     (اجرای باینری xray)
│   ├── aether/AetherManager.kt         (اجرای باینری aether)
│   └── update/UpdateManager.kt         (آپدیت داخلی APK)
├── data/
│   ├── local/  (Room: ServerDao, SubscriptionDao, AppDatabase, SettingsRepository)
│   ├── model/  (Server, Subscription, ConnectionMode, ConnectionHealth)
│   └── remote/ (SubFetcher, SubscriptionRepository)
├── di/        (DatabaseModule, NetworkModule)
├── service/
│   ├── VpnConnectionService.kt         (VpnService — مدیریت اتصال)
│   └── VpnConnectionManager.kt         (companion object — fallback chain)
└── ui/
    ├── HomeViewModel.kt, OFAVPNViewModel.kt
    ├── OFAVPNApp.kt                    (NavHost)
    ├── navigation/NavRoutes.kt
    ├── screens/ (Home, Servers, Settings, Splash, Terms)
    └── theme/ (Color, GamingTheme, ModeTheme, Theme, Type)
```

### پیکربندی Gradle:
- `compileSdk = 34`, `minSdk = 29`, `targetSdk = 34`
- Kotlin 1.9.20, Compose compiler 1.5.4
- Hilt + KSP برای DI
- `applicationId = "com.ofa.vpn"`
- Java/Kotlin target 17
- R8 + minify فعال برای debug و release

## 🐛 باگ‌های شناخته‌شده (اولویت بالا)

### ۱. اتصال کار نمی‌کند (CRITICAL)
**علت:** `XrayCore.kt` به `libxray.so` از `nativeLibraryDir` وابسته است، ولی این باینری داخل پروژه نیست. همچنین `VpnConnectionService` تون (TUN interface) را به Xray وصل نمی‌کند — `tun2socks` پیاده‌سازی نشده.

**راه‌حل مورد نیاز:**
- اضافه کردن باینری `xray` (arm64) به `app/src/main/jniLibs/arm64-v8a/libxray.so`
- پیاده‌سازی `tun2socks`: ترافیک TUN interface را به SOCKS5 inbound Xray روی `127.0.0.1:10808` هدایت کند
- می‌توان از `hev-socks5-tunnel` یا `tun2socks` (Go) استفاده کرد
- یا اگر باینری جدا سخته است، حداقل `VpnService.Builder` تنظیم شود و trafic را با `protect()` از VPN loop محافظت کند

### ۲. دکمه + در صفحه سرورها کار نمی‌کند
**علت:** `ServersScreen.kt` یک FAB دارد ولی به درستی به صفحه Settings (صفحه import ساب) متصل نیست.

**راه‌حل:** FAB باید به `SettingsScreen` ناوبری کند و بعد از وارد کردن URL ساب، سرورها هم در صفحه Servers نمایش داده شوند.

### ۳. انتخاب سرور و اتصال اتفاق نمی‌افتد
**علت:** `HomeViewModel.setServersFromRepo()` سرورها را از DB می‌خواند و به `VpnConnectionManager.setFallbackChain` تزریق می‌کند، ولی `connect()` قبل از `setServersFromRepo()` صدا زده می‌شه و سرویس خالی شروع می‌شه.

**راه‌حل:** ترتیب درست:
1. `setServersFromRepo()` (async — منتظر بمان)
2. بعد `startVpnService()`

### ۴. vmess:// پارس نمی‌شود
**علت:** `SubFetcher.parseUri()` برای vmess از `URL` جاوا استفاده می‌کند که با فرمت vmess (base64 JSON) سازگار نیست. vmess:// در واقع base64 یک JSON است، نه URL معمولی.

**راه‌حل:** اگر خط با `vmess://` شروع شد، base64 decode کنید و JSON را پارس کنید (فیلدها: `add`, `port`, `id`, `aid`, `net`, `type`, `host`, `path`, `tls`, `sni`, `ps`).

### ۵. Play Protect خطا می‌گیرد
**علت:** APK با debug signing و `usesCleartextTraffic="true"` و فراخوانی ProcessBuilder باینری خارجی — Google Play Protect این را مشکوک می‌بیند.

**راه‌حل:**
- `usesCleartextTraffic="false"` (مگر اینکه واقعا لازم باشد)
- اضافه کردن `android:networkSecurityConfig` با policy واضح
- در release، APK را با key امضا کنید
- اضافه کردن ProGuard rules برای کلاس‌های مهم تا R8 نبالند

## ✨ ویژگی‌های جدید مورد نیاز

### ۶. انتخاب زبان (فارسی/انگلیسی)
- صفحه انتخاب زبان در Onboarding
- `strings.xml` برای `fa` و `en`
- تمام متن‌های欸getString() از resource خوانده شوند
- زبان با `SettingsRepository` ذخیره شود و در `MainActivity` اعمال شود (`LocaleListCompat`)

### ۷. فونت Madika
- فونت‌ها در `res/font/` قرار دارند (`madika_*.ttf`, `madika_family.xml`)
- باید در `Type.kt` به عنوان `FontFamily` تعریف و برای همه text استفاده شود
- RTL را پشتیبانی کند

### ۸. انیمیشن‌ها و افکت‌ها
- **Gaming Mode:** حلقه آتشین چرخان (golden-time) روی دکمه اتصال — `GamingTheme.kt` وجود دارد ولی کامل نیست
- **اتصال:** انیمیشن ripple/spread روی دکمه وقتی CONNECTING
- **Splash:** انیمیشن fade از "OFA VPN" به "One For All" — `SplashScreen.kt` وجود دارد ولی ساده است
- **لگو/شفافیت:** لوگو شفاف و glassmorphism UI
- **انتخاب mode:** انیمیشن مثنویس بین AUTO/WEB/SOCIAL/GAMING/PROXY/AETHER

### ۹. لوگو/آیکون برنامه
- `ic_launcher.xml` فعلی ساده است
- باید وکتور SVG حرفه‌ای: سپر با حلقه سبز و نماد "One For All"
- سازگار با adaptive icon (foreground + background)
- روی پس‌زمینه‌های تیره و روشن خوب دیده شود

### ۱۰. Onboarding
- صفحات: خوش‌آمدگویی → انتخاب زبان → توضیح VPN → افزودن ساب → آماده
- فقط اولین بار نمایش داده شود (با `SettingsRepository`)
- بعد از روی `TermsScreen` برود

### ۱۱. آپدیت داخلی aether
- `UpdateManager.kt` برای APK وجود دارد
- اضافه کردن: دانلود باینری `aether-arm64` از URL و جایگزینی در `filesDir`
- `AetherManager.replaceBinary()` وجود دارد — فقط UI و trigger اضافه شود

### ۱۲. UI سازگار با همه گوشی‌ها
- استفاده از `WindowSizeClass` برای responsive layout
- تست روی tablet و گوشی کوچک
- پشتیبانی حالت تیرک و روشن
- RTL کامل وقتی زبان فارسی است

## 📋 دستورالعمل برای Arena.ai

۱. مخزن را clone کن: `git clone https://github.com/noob-coder-clude/ofa-vpn.git`
۲. همه باگ‌های بالا را رفع کن (اولویت: ۱ > ۳ > ۲ > ۴ > ۵ > بقیه)
۳. ویژگی‌های جدید را اضافه کن
۴. TypeScript/JSON config‌ها را با تست بساز و verify کن
۵. commit بزن و push کن

### نکات مهم:
- **R8/ProGuard:** کلاس‌های `VpnConnectionService`, `AetherManager`, `XrayCore` را در `proguard-rules.pro` keep کن
- **نام پکیج:** `com.ofa.vpn` را تغییر نده
- **Hilt:** `@AndroidEntryPoint` برای Activity و `@HiltViewModel` برای ViewModel رعایت شود
- **RTL:** وقتی زبان فارسی است، کل layout RTL شود (`LocalLayoutDirection`)
- **فونت Madika:** برای همه متن‌ها، حتی دکمه‌ها و notification
- **باینری libxray.so:** اگر نمی‌توانی باینری اضافه کنی، حداقل کد را طوری بنویس که اگر باینری موجود نبود، پیام واضح بدهد (نه silently fail)

### proguard-rules.pro پیشنهادی:
```
-keep class com.ofa.vpn.service.VpnConnectionService { *; }
-keep class com.ofa.vpn.core.XrayCore { *; }
-keep class com.ofa.vpn.core.aether.AetherManager { *; }
-keep class com.ofa.vpn.core.ConfigParser { *; }
-keep class com.ofa.vpn.data.model.** { *; }
-keep class com.ofa.vpn.data.local.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keep class org.json.** { *; }
```

### تست نهایی:
پس از همه تغییرات، `./gradlew assembleDebug` باید بدون خطا build شود و APK تولید شود.
