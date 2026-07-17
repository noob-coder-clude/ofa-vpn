# ╔══════════════════════════════════════════════════════════════╗
# ║  OFA VPN - Colab Build Script (مستقیم از GitHub اجرا شود)    ║
# ║  فقط این فایل رو کپی کن و در یک سلول Colab اجرا کن            ║
# ╚══════════════════════════════════════════════════════════════╝

# ─── گام ۱: Mount کردن Google Drive ───────────────────────────
from google.colab import drive
import os, subprocess, sys

drive.mount('/content/drive')
print("✅ Google Drive متصل شد")

# ─── گام ۲: مسیر پروژه روی Drive ──────────────────────────────
PROJECT_DIR = "/content/drive/MyDrive/ofa-vpn"
os.makedirs(PROJECT_DIR, exist_ok=True)

# ─── گام ۳: کلون یا آپدیت پروژه ───────────────────────────────
os.chdir(PROJECT_DIR)
if not os.path.exists(os.path.join(PROJECT_DIR, ".git")):
    print("📥 کلون کردن پروژه...")
    subprocess.run(f"git clone https://github.com/noob-coder-clude/ofa-vpn.git .", shell=True, check=True)
else:
    print("🔄 آپدیت پروژه...")
    subprocess.run("git pull origin master", shell=True, check=True)

# ─── گام ۴: اصلاح مسیرها در اسکریپت بیلد (اگر لازم باشه) ────────
# build-colab.sh از متغیر REPO_DIR استفاده می‌کند؛ مطمئن شو تنظیمه
build_script = os.path.join(PROJECT_DIR, "build-colab.sh")
if os.path.exists(build_script):
    # جایگزینی مسیر پیش‌فرض با مسیر فعلی روی Drive
    with open(build_script, "r") as f:
        content = f.read()
    # اگر REPO_DIR هاردکد شده، عوضش کن
    if "/content/ofa-vpn" in content:
        content = content.replace("/content/ofa-vpn", PROJECT_DIR)
        with open(build_script, "w") as f:
            f.write(content)
        print("🔧 مسیر پروژه در build-colab.sh اصلاح شد")

# ─── گام ۵: اجرای بیلد ────────────────────────────────────────
print("🚀 شروع بیلد...")
result = subprocess.run(
    f"bash {build_script}",
    shell=True,
    capture_output=False
)

# ─── گام ۶: بررسی نتیجه ───────────────────────────────────────
apk_path = os.path.join(PROJECT_DIR, "app/build/outputs/apk/debug/app-debug.apk")
if os.path.exists(apk_path):
    size_mb = os.path.getsize(apk_path) / (1024 * 1024)
    print(f"\n✅✅✅ بیلد موفق!")
    print(f"📦 APK آماده است: {apk_path}")
    print(f"📏 حجم: {size_mb:.1f} MB")
    print(f"\nبرای دانلود: از منوی Files سمت چپ Colab برو به:")
    print(f"   MyDrive → ofa-vpn → app → build → outputs → apk → debug → app-debug.apk")
else:
    print("\n❌ بیلد شکست خورد. لاگ بالا رو چک کن یا ۲۰ خط آخر رو بفرست.")
    sys.exit(1)
