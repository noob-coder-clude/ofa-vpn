# ╔════════════════════════════════════════════════════════════════╗
# ║  OFA VPN — aether cross-compile for Android (arm64)             ║
# ║  این اسکریپت رو در یک سلول Colab اجرا کن                        ║
# ║  خروجی: /content/aether-arm64 (باینری اندروید)                  ║
# ╚══════════════════════════════════════════════�
import os, subprocess, textwrap

script = r'''
set -e
export DEBIAN_FRONTEND=noninteractive
echo "=== 1. System deps ==="
sudo apt-get update -qq
sudo apt-get install -y -qq curl git build-essential cmake clang \
  pkg-config libssl-dev protobuf-compiler unzip openjdk-17-jdk

echo "=== 2. Rust ==="
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
source "$HOME/.cargo/env"

echo "=== 3. Android NDK ==="
cd /content
NDK_VER=r25c
NDK_ZIP=android-ndk-$NDK_VER-linux.zip
wget -q https://dl.google.com/android/repository/$NDK_ZIP
unzip -q $NDK_ZIP
export ANDROID_NDK_HOME=/content/android-ndk-$NDK_VER
export NDK=$ANDROID_NDK_HOME

echo "=== 4. Rust target ==="
rustup target add aarch64-linux-android

cat > /content/.cargo/config.toml <<EOF
[target.aarch64-linux-android]
linker = "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang"
ar = "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"
EOF

echo "=== 5. Clone aether + quiche ==="
cd /content
rm -rf Aether quiche
git clone --depth 1 https://github.com/CluvexStudio/Aether.git
git clone --depth 1 https://github.com/cloudflare/quiche.git

echo "=== 6. Build ==="
cd /content/Aether/aether
export CC_aarch64_linux_android=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang
export AR_aarch64_linux_android=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=$CC_aarch64_linux_android
cargo build --target aarch64-linux-android --release 2>&1 | tail -25

echo "=== 7. Output ==="
ls -lh /content/Aether/aether/target/aarch64-linux-android/release/aether
cp /content/Aether/aether/target/aarch64-linux-android/release/aether /content/aether-arm64
echo "DONE: /content/aether-arm64"
'''
with open('/content/build_aether.sh','w') as f:
    f.write(script)
print("اسکریپت نوشته شد: /content/build_aether.sh")
print("برای اجرا این رو بزن:")
print()
print("!bash /content/build_aether.sh 2>&1 | tail -40")
