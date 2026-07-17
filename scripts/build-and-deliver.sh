#!/usr/bin/env bash
#
# build-and-deliver.sh
# بیلد کامل OFA VPN روی سرور لینوکس — بدون Docker
#
set -uo pipefail

# ── رنگ‌ها ──
G='\033[0;32m'; Y='\033[1;33m'; R='\033[0;31m'; N='\033[0m'
ok()   { echo -e "${G}✅ $1${N}"; }
warn() { echo -e "${Y}⚠️  $1${N}"; }
err()  { echo -e "${R}❌ $1${N}"; }

# ── مسیرها و نسخه‌ها ──
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="/opt/android-sdk"
NDK_VER="r25c"
NDK="${SDK}/ndk/${NDK_VER}"
XRAY_VER="v25.7.23"
T2S_VER="v2.8.3"
OUTPUT_DIR="/tmp/ofa-output"
CMDLINE_TOOLS="${SDK}/cmdline-tools/latest/bin"

export ANDROID_HOME="${SDK}"
export ANDROID_SDK_ROOT="${SDK}"
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="${CMDLINE_TOOLS}:${SDK}/platform-tools:${NDK}:${JAVA_HOME}/bin:${PATH}"

# ─ـ helper: ادامه حتی اگه yes pipe بشه ──
run_sdk() {
    yes 2>/dev/null | sdkmanager --sdk_root="${SDK}" "$@" 2>/dev/null || true
}

# ── ۱. نصب پیش‌نیازها ──
ok "نصب پیش‌نیازهای سیستم ..."
apt-get update -qq
apt-get install -y -qq openjdk-17-jdk wget unzip curl git python3 >/dev/null 2>&1

# ── ۲. Android SDK + NDK ──
if [ ! -d "${CMDLINE_TOOLS}" ]; then
    ok "دانلود Android SDK command-line tools ..."
    mkdir -p "${SDK}/cmdline-tools"
    cd /tmp
    wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O cmdtools.zip
    unzip -o cmdtools.zip -d "${SDK}/cmdline-tools" >/dev/null 2>&1
    mv "${SDK}/cmdline-tools/cmdline-tools" "${SDK}/cmdline-tools/latest" 2>/dev/null || true
    rm -f cmdtools.zip
    cd "${ROOT}"
fi

# چک اینکه sdkmanager وجود داره
if ! command -v sdkmanager &>/dev/null; then
    if [ -f "${CMDLINE_TOOLS}/sdkmanager" ]; then
        export PATH="${CMDLINE_TOOLS}:${PATH}"
    else
        err "sdkmanager پیدا نشه!"
        exit 1
    fi
fi

ok "قبول لایسنس‌ها ..."
run_sdk --licenses

ok "نصب SDK packages ..."
run_sdk "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;${NDK_VER}"

# ── ۳. local.properties ──
ok "نوشتن local.properties ..."
cat > "${ROOT}/local.properties" <<EOF
sdk.dir=${SDK}
ndk.dir=${NDK}
EOF

# ── ۴. دانلود native libs ──
JNILIBS="${ROOT}/app/src/main/jniLibs/arm64-v8a"
ASSETS="${ROOT}/app/src/main/assets"
mkdir -p "${JNILIBS}" "${ASSETS}"

if [ ! -f "${JNILIBS}/libxray.so" ]; then
    ok "دانلود libxray.so (${XRAY_VER}) ..."
    cd /tmp
    wget -q "https://github.com/XTLS/Xray-core/releases/download/${XRAY_VER}/Xray-android-arm64-v8a.zip" -O xray.zip
    unzip -o xray.zip -d /tmp/xray-ext >/dev/null 2>&1
    cp /tmp/xray-ext/xray "${JNILIBS}/libxray.so" 2>/dev/null || cp /tmp/xray-ext/libxray.so "${JNILIBS}/libxray.so" 2>/dev/null || true
    chmod +x "${JNILIBS}/libxray.so"
    rm -rf /tmp/xray-ext xray.zip
    ok "libxray.so آماده شد"
fi

if [ ! -f "${JNILIBS}/libtun2socks.so" ]; then
    ok "دانلود libtun2socks.so (${T2S_VER}) ..."
    cd /tmp
    if wget -q "https://github.com/xjasonlyu/tun2socks/releases/download/${T2S_VER}/tun2socks-android-arm64.zip" -O t2s.zip; then
        unzip -o t2s.zip -d /tmp/t2s-ext >/dev/null 2>&1
        BIN=$(find /tmp/t2s-ext -type f -name "tun2socks*" ! -name "*.zip" | head -1)
        if [ -n "${BIN}" ]; then
            cp "${BIN}" "${JNILIBS}/libtun2socks.so"
            chmod +x "${JNILIBS}/libtun2socks.so"
            ok "libtun2socks.so آماده شد"
        else
            warn "باینری tun2socks پیدا نشه — بدونش ادامه می‌دیم"
        fi
        rm -rf /tmp/t2s-ext t2s.zip
    else
        warn "دانلود tun2socks ناموفق — بدونش ادامه می‌دیم"
        rm -f t2s.zip
    fi
fi

# ── ۵. دانلود GEO assets ──
if [ ! -f "${ASSETS}/geoip.dat" ]; then
    ok "دانلود geoip.dat ..."
    curl -fsSL "https://github.com/XTLS/Xray-core/releases/download/${XRAY_VER}/geoip.dat" -o "${ASSETS}/geoip.dat"
fi
if [ ! -f "${ASSETS}/geosite.dat" ]; then
    ok "دانلود geosite.dat ..."
    curl -fsSL "https://github.com/XTLS/Xray-core/releases/download/${XRAY_VER}/geosite.dat" -o "${ASSETS}/geosite.dat"
fi
ok "GEO assets آماده شد"

# ── ۶. Gradle wrapper ──
if [ ! -f "${ROOT}/gradle/wrapper/gradle-wrapper.jar" ]; then
    ok "ساخت gradle-wrapper.jar ..."
    cd "${ROOT}"
    wget -q "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -O /tmp/gradle.zip
    unzip -q /tmp/gradle.zip -d /opt
    /opt/gradle-8.9/bin/gradle wrapper --gradle-version 8.9 --distribution-type bin 2>/dev/null
    rm -f /tmp/gradle.zip
fi

# ── ۷. بیلد APK ──
ok "شروع بیلد APK ..."
cd "${ROOT}"
chmod +x gradlew
./gradlew assembleDebug --no-daemon --stacktrace

# ── ۸. کپی خروجی ──
APK_SRC="${ROOT}/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "${APK_SRC}" ]; then
    mkdir -p "${OUTPUT_DIR}"
    cp "${APK_SRC}" "${OUTPUT_DIR}/ofa-vpn-debug.apk"
    APK_SIZE=$(du -h "${OUTPUT_DIR}/ofa-vpn-debug.apk" | cut -f1)
    ok "APK ساخته شد!"
    ok "مسیر: ${OUTPUT_DIR}/ofa-vpn-debug.apk"
    ok "حجم: ${APK_SIZE}"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📦  برای دانلود APK:"
    echo "    scp root@<server>:${OUTPUT_DIR}/ofa-vpn-debug.apk ."
    echo ""
    echo "یا python server:"
    echo "    cd ${OUTPUT_DIR} && python3 -m http.server 8888"
    echo "    باز کن: http://<server-ip>:8888/ofa-vpn-debug.apk"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    err "APK ساخته نشه! logها رو ببین"
    exit 1
fi
