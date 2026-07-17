#!/usr/bin/env bash
#
# fetch-native.sh — دانلود لیب‌های native و فایل‌های GEO برای OFA VPN
# اجرا روی سرور لینوکی (اوبونتو/دبیان) قبل از بیلد اندروید
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JNILIBS="$ROOT/app/src/main/jniLibs/arm64-v8a"
ASSETS="$ROOT/app/src/main/assets"

echo "📁 مسیر پروژه: $ROOT"

# ۱. ساخت پوشه‌ها
mkdir -p "$JNILIBS" "$ASSETS"

# ۲. دانلود libxray.so (آخرین release رسمی Xray-core)
#    نام فایل توی release: xray-android-arm64-v8a.zip
XRAY_VER="v25.7.23"
XRAY_ZIP=$(mktemp /tmp/xray-XXXX.zip)
echo "⬇️  دانلود Xray-core $XRAY_VER ..."
curl -fSL "https://github.com/XTLS/Xray-core/releases/download/$XRAY_VER/Xray-android-arm64-v8a.zip" -o "$XRAY_ZIP"

echo "📦 استخراج libxray.so ..."
# توی zip اسم فایل xray (بدون پسوند .so) هست — کپی به libxray.so
unzip -o "$XRAY_ZIP" -d /tmp/xray-extract >/dev/null
cp /tmp/xray-extract/xray "$JNILIBS/libxray.so"
chmod +x "$JNILIBS/libxray.so"
rm -rf /tmp/xray-extract "$XRAY_ZIP"
echo "✅ libxray.so آماده شد"

# ۳. دانلود libtun2socks.so
#    منبع: xjasonlyu/tun2socks release (android arm64)
T2S_VER="v2.8.3"
T2S_ZIP=$(mktemp /tmp/t2s-XXXX.zip)
echo "⬇️  دانلود tun2socks $T2S_VER ..."
if curl -fSL "https://github.com/xjasonlyu/tun2socks/releases/download/$T2S_VER/tun2socks-android-arm64.zip" -o "$T2S_ZIP"; then
  unzip -o "$T2S_ZIP" -d /tmp/t2s-extract >/dev/null
  # پیدا کردن باینری (اسم ممکنه tun2socks یا tun2socks-android باشه)
  BIN=$(find /tmp/t2s-extract -type f -name "tun2socks*" ! -name "*.zip" | head -1)
  cp "$BIN" "$JNILIBS/libtun2socks.so"
  chmod +x "$JNILIBS/libtun2socks.so"
  rm -rf /tmp/t2s-extract "$T2S_ZIP"
  echo "✅ libtun2socks.so آماده شد"
else
  echo "⚠️  دانلود tun2socks ناموفق — سرویس بدونش هم کار می‌کنه (routing مستقیم)"
  rm -f "$T2S_ZIP"
fi

# ۴. دانلود فایل‌های GEO
echo "⬇️  دانلود geoip.dat ..."
curl -fSL "https://github.com/XTLS/Xray-core/releases/download/$XRAY_VER/geoip.dat" -o "$ASSETS/geoip.dat"
echo "⬇️  دانلود geosite.dat ..."
curl -fSL "https://github.com/XTLS/Xray-core/releases/download/$XRAY_VER/geosite.dat" -o "$ASSETS/geosite.dat"
echo "✅ فایل‌های GEO آماده شدن"

echo ""
echo "🎉 همه چی آماده‌ست:"
echo "   $JNILIBS/libxray.so"
echo "   $JNILIBS/libtun2socks.so (اختیاری)"
echo "   $ASSETS/geoip.dat"
echo "   $ASSETS/geosite.dat"
echo ""
echo "حالا می‌تونی بیلد کنی: ./gradlew assembleDebug"
