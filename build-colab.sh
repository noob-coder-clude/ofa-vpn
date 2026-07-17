#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export ANDROID_HOME=${ANDROID_HOME:-/content/android-sdk}
export NDK_ROOT=${NDK_ROOT:-${ANDROID_HOME}/ndk/r25c}
export PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/ndk/r25c/bin

info() { echo -e "\033[34m[info]\033[0m $*"; }
error() { echo -e "\033[31m[error]\033[0m $*" >&2; }
warn()  { echo -e "\033[33m[warn]\033[0m $*"; }
success(){ echo -e "\033[32m[success]\033[0m $*"; }

info "بررسی ابزارهای مورد نیاز"
for cmd in git unzip wget curl; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    apt-get update -qq
    apt-get install -y --no-install-recommends "$cmd"
    info "✅ $cmd نصب شد"
  fi
done

REPO_DIR="/content/ofa-vpn"
if [ -d "$REPO_DIR/.git" ]; then
  info "مخزن در /content/ofa-vpn موجود است – در حال دریافت به‌روزرسانی‌ها"
  cd "$REPO_DIR"
  git pull origin master || warn "دریافت خودکار با شکست مواجه شد؛ ادامه می‌دهیم"
else
  info "در حال کلون مخزن از GitHub (مسیر بهینه Colab)"
  mkdir -p /content
  git clone https://github.com/noob-coder-clude/ofa-vpn.git "$REPO_DIR" || { error "کلون شکست خورد"; exit 1; }
fi
cd "$REPO_DIR"

info "دریافت فایل‌های بومی و GEO assets"
mkdir -p app/src/main/jniLibs/arm64-v8a app/src/main/assets

wget -q https://github.com/XTLS/Xray-core/releases/download/v1.8.2/xray-android-arm64-v8a.zip -O /tmp/xray.zip 2>/dev/null || \
wget -q https://github.com/XTLS/Xray-core/releases/latest/download/xray-android-arm64-v8a.zip -O /tmp/xray.zip 2>/dev/null || true

if [ -f /tmp/xray.zip ]; then
  mkdir -p /tmp/xray-pkg
  unzip -q /tmp/xray.zip -d /tmp/xray-pkg 2>/dev/null || true
  if [ -f /tmp/xray-pkg/libxray.so ]; then
    cp -v /tmp/xray-pkg/libxray.so app/src/main/jniLibs/arm64-v8a/
  elif [ -f /tmp/xray-pkg/xray ]; then
    cp -v /tmp/xray-pkg/xray app/src/main/jniLibs/arm64-v8a/libxray.so
  fi
  rm -rf /tmp/xray-pkg /tmp/xray.zip
fi

chmod +x app/src/main/jniLibs/arm64-v8a/*.so 2>/dev/null || true

wget -q https://raw.githubusercontent.com/XTLS/Xray-install/main/geoip.dat -O app/src/main/assets/geoip.dat 2>/dev/null || true
wget -q https://raw.githubusercontent.com/XTLS/Xray-install/main/geosite.dat -O app/src/main/assets/geosite.dat 2>/dev/null || true
info "✅ فایل‌های بومی و GEO آماده شدند"

info "اضافه کردن منابع مورد نیاز (strings/colors/themes)"
cat > app/src/main/res/values/strings.xml <<'EOF'
<resources>
    <string name="app_name">OFA VPN</string>
</resources>
EOF

cat > app/src/main/res/values/colors.xml <<'EOF'
<resources>
    <color name="colorPrimary">#FF6D1B</color>
    <color name="colorPrimaryVariant">#FF8C42</color>
    <color name="textOnPrimary">#FFFFFF</color>
    <color name="colorSecondary">#00C853</color>
    <color name="textOnSecondary">#FFFFFF</color>
</resources>
EOF

cat > app/src/main/res/values/themes.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.OFAVPN" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryVariant">@color/colorPrimaryVariant</item>
        <item name="colorOnPrimary">@color/textOnPrimary</item>
        <item name="colorSecondary">@color/colorSecondary</item>
        <item name="colorOnSecondary">@color/textOnSecondary</item>
    </style>
</resources>
EOF

info="اصلاح AndroidManifest.xml (حذف package، تغییر label)"
sed -i 's/package="com.ofa.vpn"//g' app/src/main/AndroidManifest.xml
sed -i 's|android:label="OFA VPN"|android:label="@string/app_name"|g' app/src/main/AndroidManifest.xml

info="در حال بیلد ./gradlew assembleDebug"
chmod +x gradlew
if ./gradlew assembleDebug; then
  success "بیلد موفق"
else
  error "بیلد Gradle با شکست مواجه شد"
  exit 1
fi

APK_PATH=$(find . -name "app-debug.apk" -type f 2>/dev/null | head -1)
if [ -n "$APK_PATH" ]; then
  echo "✅ APK در دسترس است: $APK_PATH"
  cp -v "$APK_PATH" /tmp/ofa-vpn-debug.apk
  echo "📦 کپی شده به /tmp/ofa-vpn-debug.apk"
else
  error "APK پیدا نشد"
  exit 1
fi

success="تمام کارهای ساختمانی تکمیل شد! 🎉"
