# Dockerfile — بیلد کامل OFA VPN Android APK
# خروجی: /tmp/ofa-output/ofa-vpn-debug.apk
# 
# اجرا: docker compose up --build
# یا:    docker build -t ofa-builder . && docker run -v $(pwd)/output:/tmp/ofa-output ofa-builder

FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk

ARG NDK_VER=r25c
ARG XRAY_VER=v25.7.23
ARG T2S_VER=v2.8.3

ENV PATH=${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/ndk/${NDK_VER}:/usr/lib/jvm/java-17-openjdk-amd64/bin:${PATH}

# ── ۱. نصب پیش‌نیازها ──
RUN apt-get update -qq && apt-get install -y -qq --no-install-recommends \
    openjdk-17-jdk \
    wget \
    unzip \
    curl \
    git \
    python3 \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# ── ۲. Android SDK ──
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && cd /tmp \
    && wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O cmdtools.zip \
    && unzip -o cmdtools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm -f cmdtools.zip

RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses >/dev/null 2>&1 \
    && sdkmanager --sdk_root=${ANDROID_HOME} \
        "platform-tools" \
        "platforms;android-34" \
        "build-tools;34.0.0" \
        "ndk;${NDK_VER}" >/dev/null 2>&1

# ── ۳. کپی پروژه ──
WORKDIR /workspace/ofa-vpn
COPY . .

# ── ۴. دانلود native libs + GEO ──
RUN mkdir -p app/src/main/jniLibs/arm64-v8a app/src/main/assets \
    # libxray.so
    && cd /tmp \
    && wget -q "https://github.com/XTLS/Xray-core/releases/download/${XRAY_VER}/Xray-android-arm64-v8a.zip" -O xray.zip \
    && unzip -o xray.zip -d /tmp/xray-ext >/dev/null \
    && cp /tmp/xray-ext/xray app/src/main/jniLibs/arm64-v8a/libxray.so \
    && chmod +x app/src/main/jniLibs/arm64-v8a/libxray.so \
    && rm -rf /tmp/xray-ext xray.zip \
    # libtun2socks.so
    && wget -q "https://github.com/xjasonlyu/tun2socks/releases/download/${T2S_VER}/tun2socks-android-arm64.zip" -O t2s.zip \
    && unzip -o t2s.zip -d /tmp/t2s-ext >/dev/null \
    && BIN=$(find /tmp/t2s-ext -type f -name "tun2socks*" ! -name "*.zip" | head -1) \
    && cp "${BIN}" app/src/main/jniLibs/arm64-v8a/libtun2socks.so \
    && chmod +x app/src/main/jniLibs/arm64-v8a/libtun2socks.so \
    && rm -rf /tmp/t2s-ext t2s.zip \
    # GEO assets
    && curl -fsSL "https://github.com/XTLS/Xray-core/releases/download/${XRAY_VER}/geoip.dat" -o app/src/main/assets/geoip.dat \
    && curl -fsSL "https://github.com/XTLS/Xray-core/releases/download/${XRAY_VER}/geosite.dat" -o app/src/main/assets/geosite.dat

# ── ۵. local.properties ──
RUN echo "sdk.dir=${ANDROID_HOME}\nndk.dir=${ANDROID_HOME}/ndk/${NDK_VER}" > local.properties

# ── ۶. Gradle wrapper (اگه نبود) ──
RUN if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then \
        wget -q "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -O /tmp/gradle.zip \
        && unzip -q /tmp/gradle.zip -d /opt \
        && /opt/gradle-8.9/bin/gradle wrapper --gradle-version 8.9 --distribution-type bin \
        && rm -f /tmp/gradle.zip; \
    fi

# ── ۷. بیلد ──
RUN chmod +x gradlew \
    && ./gradlew assembleDebug --no-daemon --stacktrace

# ── ۸. کپی خروجی ──
RUN mkdir -p /tmp/ofa-output \
    && cp app/build/outputs/apk/debug/app-debug.apk /tmp/ofa-output/ofa-vpn-debug.apk \
    && echo "✅ APK ساخته شد: /tmp/ofa-output/ofa-vpn-debug.apk" \
    && ls -lh /tmp/ofa-output/ofa-vpn-debug.apk
