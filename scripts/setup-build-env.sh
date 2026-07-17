#!/usr/bin/env bash
#
# setup-build-env.sh — نصب Android SDK + NDK روی سرور لینوکی (اوبونتو/دبیان)
# اجرا با: sudo bash setup-build-env.sh
#
set -euo pipefail

echo "🔧 نصب پیش‌نیازها ..."
apt-get update
apt-get install -y openjdk-17-jdk wget unzip git curl python3

# متغیرهای محیطی
ANDROID_HOME="/opt/android-sdk"
CMDLINE_VER="11076708"  # commandlinetools-linux
NDK_VER="r25c"

echo "📥 دانلود Android command-line tools ..."
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd /tmp
wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VER}_latest.zip" -O cmdtools.zip
unzip -o cmdtools.zip -d "$ANDROID_HOME/cmdline-tools" >/dev/null
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" 2>/dev/null || true
rm -f cmdtools.zip

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

echo "📦 نصب SDK packages ..."
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null
sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0" \
  "ndk;$NDK_VER"

echo "📝 تنظیم متغیرهای محیطی ..."
cat >> /etc/profile.d/android.sh <<EOF
export ANDROID_HOME=$ANDROID_HOME
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH
EOF

echo "✅ محیط بیلد آماده شد"
echo "   ANDROID_HOME=$ANDROID_HOME"
echo "   NDK: \$ANDROID_HOME/ndk/$NDK_VER"
echo ""
echo "مرحله بعد: bash scripts/fetch-native.sh"
echo "سپس: ./gradlew assembleDebug"
