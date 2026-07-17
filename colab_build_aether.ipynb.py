# ╔════════════════════════════════════════════════════════════════════╗
# ║  OFA VPN — aether cross-compile (یک‌پارچه + خودکار + لاگ هوشمند)      ║
# ║  فقط این سلول رو اجرا کن. بقیه‌اش خودکاره.                          ║
# ║  خروجی: /content/aether-arm64  +  دانلود خودکار                       ║
# ╚════════════════════════════════════════════════════════════════════╝
import os, sys, subprocess, textwrap, time, json, traceback

STEP = {"n": 0, "name": ""}
LOG = []
def step(name):
    STEP["n"] += 1
    STEP["name"] = name
    print(f"\n{'='*60}\n[{STEP['n']}] {name} ...\n{'='*60}")

def ok(msg=""):
    print(f"  ✅ {msg}" if msg else "  ✅ done")

def run(cmd, timeout=1800, shell=False, env=None):
    """اجرا با لاگ هوشمند — خطا رو تشخیص می‌ده و متوقف می‌کنه"""
    print(f"  $ {' '.join(cmd) if isinstance(cmd, list) else cmd}")
    try:
        r = subprocess.run(
            cmd, shell=shell, timeout=timeout,
            capture_output=True, text=True,
            env={**os.environ, **(env or {})}
        )
        out = (r.stdout or "") + (r.stderr or "")
        # لاگ هوشمند: فقط خطاها و هشدارها رو نشون بده
        for line in out.splitlines():
            l = line.strip().lower()
            if any(k in l for k in ["error", "cannot", "failed", "fatal", "panic", "denied", "not found", "no such"]):
                print(f"  ⚠️  {line.strip()[:160]}")
        if r.returncode != 0:
            print(f"  ❌ خطا (کد {r.returncode})")
            # ۱۰ خط آخر خروجی برای دیباگ
            tail = [l for l in out.splitlines() if l.strip()][-10:]
            for t in tail:
                print(f"     └ {t[:160]}")
            raise RuntimeError(f"step '{STEP['name']}' failed")
        return r.stdout
    except subprocess.TimeoutExpired:
        print(f"  ⏱️  timeout ({timeout}s)")
        raise
    except Exception as e:
        print(f"  ❌ exception: {e}")
        raise

def download_if(path, label):
    if os.path.exists(path) and os.path.getsize(path) > 100_000:
        try:
            from google.colab import files
            files.download(path)
            print(f"  📥 {label} دانلود شد: {path}")
            return True
        except Exception as e:
            print(f"  ⚠️  دانلود خودکار نشد (دستی دانلود کن): {e}")
            print(f"     from google.colab import files; files.download('{path}')")
            return False
    else:
        print(f"  ❌ فایل خروجی پیدا نشد: {path}")
        return False

# ──────────────────────────────────────────────────────────────
def main():
    t0 = time.time()
    try:
        # ۱. ابزارهای سیستم
        step("نصب ابزارهای سیستم (apt)")
        run(["sudo","apt-get","update","-qq"], timeout=300)
        run(["sudo","apt-get","install","-y","-qq","curl","git","build-essential",
             "cmake","clang","pkg-config","libssl-dev","protobuf-compiler",
             "unzip","openjdk-17-jdk"], timeout=900)

        # ۲. Rust
        step("نصب Rust")
        rustup_path = os.path.expanduser("~/.cargo/bin/rustup")
        cargo = os.path.expanduser("~/.cargo/bin/cargo")
        if not os.path.exists(cargo):
            run(["bash","-c",
                 "curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y"],
                timeout=300)
        else:
            ok("Rust قبلاً نصب بود")
        env = {"PATH": os.path.expanduser("~/.cargo/bin") + ":" + os.environ.get("PATH","")}

        # ۳. Android NDK
        step("دانلود + استخراج Android NDK")
        NDK_VER = "r25c"
        ndk_dir = f"/content/android-ndk-{NDK_VER}"
        if not os.path.isdir(ndk_dir):
            run(["wget","-q",f"https://dl.google.com/android/repository/android-ndk-{NDK_VER}-linux.zip"],
                timeout=600)
            run(["unzip","-q",f"android-ndk-{NDK_VER}-linux.zip"], timeout=300)
        else:
            ok("NDK قبلاً استخراج شده")
        ndk = ndk_dir
        prebuilt = f"{ndk}/toolchains/llvm/prebuilt/linux-x86_64"

        # ۴. تنظیم target + cargo config
        step("تنظیم Rust target (aarch64-linux-android)")
        run([rustup_path,"target","add","aarch64-linux-android"], timeout=300, env=env)
        cfg = os.path.expanduser("~/.cargo/config.toml")
        with open(cfg,"w") as f:
            f.write(textwrap.dedent(f"""
            [target.aarch64-linux-android]
            linker = "{prebuilt}/bin/aarch64-linux-android30-clang"
            ar = "{prebuilt}/bin/llvm-ar"
            """))
        ok(cfg)

        # ۵. Clone aether + quiche
        step("کلون aether + quiche")
        if not os.path.isdir("/content/Aether"):
            run(["git","clone","--depth","1","https://github.com/CluvexStudio/Aether.git"],
                timeout=300)
        else:
            ok("Aether موجوده")
        if not os.path.isdir("/content/quiche"):
            run(["git","clone","--depth","1","https://github.com/cloudflare/quiche.git"],
                timeout=300)
        else:
            ok("quiche موجوده")

        # ۶. Build
        step("کامپایل aether برای arm64-android (زمان‌بر)")
        build_env = {
            **env,
            "ANDROID_NDK_HOME": ndk,
            "NDK": ndk,
            "CC_aarch64_linux_android": f"{prebuilt}/bin/aarch64-linux-android30-clang",
            "AR_aarch64_linux_android": f"{prebuilt}/bin/llvm-ar",
            "CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER": f"{prebuilt}/bin/aarch64-linux-android30-clang",
        }
        run([cargo,"build","--target","aarch64-linux-android","--release"],
            timeout=2400, env=build_env,
            cwd="/content/Aether/aether")

        # ۷. خروجی
        step("آماده‌سازی خروجی")
        src = "/content/Aether/aether/target/aarch64-linux-android/release/aether"
        dst = "/content/aether-arm64"
        if not os.path.exists(src):
            raise RuntimeError(f"باینری ساخته نشد: {src}")
        run(["cp", src, dst], timeout=60)
        size = os.path.getsize(dst)
        if size < 100_000:
            raise RuntimeError(f"باینری خیلی کوچیکه ({size} bytes) — احتمالاً خراب")
        ok(f"{dst}  ({size/1024:.0f} KB)")

        # ۸. دانلود
        step("دانلود باینری")
        download_if(dst, "aether-arm64")

        # خلاصه
        dt = time.time() - t0
        print(f"\n{'#'*60}")
        print(f"#  ✅ موفق!  زمان: {dt/60:.1f} دقیقه")
        print(f"#  خروجی: {dst}")
        print(f"#  حجم:   {size/1024:.0f} KB")
        print(f"#  بعد: فایل رو بذار تو app/src/main/assets/aether-arm64")
        print(f"{'#'*60}")

    except Exception as e:
        dt = time.time() - t0
        print(f"\n{'!'*60}")
        print(f"!  ❌ شکست در مرحله [{STEP['n']}] {STEP['name']}")
        print(f"!  زمان سپری‌شده: {dt/60:.1f} دقیقه")
        print(f"!  خطا: {e}")
        print(f"{'!'*60}")
        traceback.print_exc()
        # راهنمای حل مشکل
        err = str(e).lower()
        print("\n💡 راهنما:")
        if "timeout" in err:
            print("   - زمان تموم شد. سلول رو دوباره اجرا کن (مراحل تکرار می‌شه ولی سریع‌تر)")
        elif "network" in err or "connection" in err:
            print("   - مشکل اینترنت/دسترسی. چک کن که Colab متصل باشه")
        elif "ndk" in err:
            print("   - NDK مشکل داره. سلول رو restart کن و دوباره اجرا کن")
        elif "rust" in err or "cargo" in err:
            print("   - مشکل Rust. '!rm -rf ~/.cargo' بزن و دوباره اجرا کن")
        else:
            print("   - خطای ناشناخته. لاگ بالا رو برای ادمین بفرست")
        raise

main()
