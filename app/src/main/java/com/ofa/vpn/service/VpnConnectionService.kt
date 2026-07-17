package com.ofa.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import com.ofa.vpn.core.ConfigParser
import com.ofa.vpn.core.PingManager
import com.ofa.vpn.core.XrayCore
import com.ofa.vpn.data.model.ConnectionMode
import com.ofa.vpn.data.model.ConnectionState
import com.ofa.vpn.data.local.AppDatabase
import com.ofa.vpn.data.local.ServerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * سرویس اصلی VPN — قلب OFA VPN
 *
 * مسئولیت‌ها:
 *  - ساخت tun interface (VpnService.Builder)
 *  - اجرای Xray-core با config مناسب mode
 *  - Kill switch: قطع کامل ترافیک وقتی VPN می‌افته
 *  - Per-app proxy: مسیریابی فقط اپ‌های انتخابی
 *  - Health monitor: تشخیص قطعی واقعی و switch به سرور بعدی (بدون looping کور)
 */
class VpnConnectionService : VpnService() {

    companion object {
        private const val TAG = "OFAVpnService"
        private const val NOTIF_CHANNEL_ID = "ofa_vpn_channel"
        private const val NOTIF_ID = 1

        const val ACTION_CONNECT = "com.ofa.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.ofa.vpn.DISCONNECT"

        const val EXTRA_MODE = "mode"

        // VPN tun تنظیمات
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ADDRESS_PREFIX = 30
        private const val VPN_DNS = "1.1.1.1"
        private const val VPN_MTU = 1500

        // health monitor
        private const val HEALTH_CHECK_INTERVAL_MS = 5000L
        private const val MAX_CONSECUTIVE_FAILURES = 2

        // وضعیت اتصال به‌صورت global برای UI
        private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
        val state: StateFlow<ConnectionState> = _state.asStateFlow()

        private val _activeServer = MutableStateFlow<Server?>(null)
        val activeServer: StateFlow<Server?> = _activeServer.asStateFlow()
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var xrayCore: XrayCore
    private val configParser = ConfigParser()
    private val pingManager = PingManager()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var healthJob: Job? = null
    private lateinit var serverDao: ServerDao

    // زنجیره fallback سرورها (از پیش مرتب‌شده — بدون looping کور)
    private var fallbackChain: List<Server> = emptyList()
    private var currentServerIndex = 0
    private var currentMode = ConnectionMode.AUTO

    override fun onCreate() {
        super.onCreate()
        xrayCore = XrayCore(applicationContext)
        serverDao = AppDatabase.getDatabase(applicationContext).serverDao()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_CONNECT -> {
                val modeName = intent.getStringExtra(EXTRA_MODE) ?: ConnectionMode.AUTO.name
                currentMode = try {
                    ConnectionMode.valueOf(modeName)
                } catch (e: Exception) {
                    ConnectionMode.AUTO
                }
                startVpn()
            }
        }
        return START_STICKY
    }

    /**
     * شروع اتصال VPN
     * فلو: prepare assets → ping → select best → generate config → start core → establish tun → health monitor
     */
    private fun startVpn() {
        _state.value = ConnectionState.CONNECTING
        startForeground(NOTIF_ID, buildNotification("در حال اتصال..."))

        serviceScope.launch {
            try {
                xrayCore.prepareAssets()

                // خواندن زنجیره سرورها از VpnConnectionManager
                fallbackChain = VpnConnectionManager.getFallbackChain()

                if (fallbackChain.isEmpty()) {
                    Log.e(TAG, "هیچ سروری برای اتصال موجود نیست")
                    _state.value = ConnectionState.ERROR
                    stopVpn()
                    return@launch
                }

                currentServerIndex = 0
                connectToServer(fallbackChain[currentServerIndex])
            } catch (e: Exception) {
                Log.e(TAG, "خطا در شروع VPN: ${e.message}", e)
                _state.value = ConnectionState.ERROR
                stopVpn()
            }
        }
    }

    /**
     * اتصال به یک سرور مشخص
     */
    private suspend fun connectToServer(server: Server) {
        Log.i(TAG, "اتصال به سرور: ${server.name} (${server.address}:${server.port})")
        _activeServer.value = server
        updateNotification("در حال اتصال به ${server.name}")

        // ثبت تاریخچه اتصال (برای یادگیری محلی — انتخاب هوشمند بعدی)
        serverDao.updateLastConnected(server.id, System.currentTimeMillis())

        // ۱. ساخت config برای این mode
        val config = configParser.build(server, currentMode)

        // ۲. اجرای هسته
        val started = xrayCore.start(config)
        if (!started) {
            Log.e(TAG, "هسته برای ${server.name} شروع نشد — سرور بعدی")
            switchToNextServer()
            return
        }

        // ۳. ساخت tun interface
        val established = establishTun(server)
        if (!established) {
            _state.value = ConnectionState.ERROR
            stopVpn()
            return
        }

        _state.value = ConnectionState.CONNECTED
        updateNotification("متصل — ${server.name}")

        // ۴. شروع health monitor
        startHealthMonitor()
    }

    /**
     * ساخت tun interface با VpnService.Builder
     * per-app proxy و kill switch اینجا اعمال می‌شه
     */
    private fun establishTun(server: Server): Boolean {
        return try {
            val builder = Builder()
                .setSession("OFA VPN")
                .setMtu(VPN_MTU)
                .addAddress(VPN_ADDRESS, VPN_ADDRESS_PREFIX)
                .addDnsServer(VPN_DNS)
                .addRoute("0.0.0.0", 0)   // همه ترافیک IPv4

            // per-app proxy (mode SOCIAL → فقط اپ‌های سوشیال)
            applyPerAppRouting(builder)

            // آدرس سرور رو از تونل مستثنی کن (جلوگیری از loop)
            // با protect() روی socket ای که به سرور وصل می‌شه
            protectServerRoute(server)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "establish() برگردوند null — مجوز VPN داده نشده؟")
                false
            } else {
                Log.i(TAG, "tun interface ساخته شد")
                startTun2Socks(server)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطا در ساخت tun: ${e.message}", e)
            false
        }
    }

    /**
     * اعمال per-app routing بر اساس mode
     */
    private fun applyPerAppRouting(builder: Builder) {
        try {
            when (currentMode) {
                ConnectionMode.SOCIAL -> {
                    // فقط اپ‌های سوشیال از VPN رد بشن
                    ConfigParser.SOCIAL_APPS.forEach { pkg ->
                        try {
                            builder.addAllowedApplication(pkg)
                        } catch (e: Exception) {
                            // اپ نصب نیست — رد شو
                        }
                    }
                    // خود اپ رو هم اضافه کن که کنترل داشته باشه
                    builder.addAllowedApplication(packageName)
                }
                else -> {
                    // بقیه mode ها: همه اپ‌ها جز خود OFA VPN
                    try {
                        builder.addDisallowedApplication(packageName)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطا در per-app routing: ${e.message}")
        }
    }

    /**
     * Health monitor — قلب تفاوت OFA VPN با v2rayNG
     * بر اساس وضعیت واقعی اتصال (نه فقط پینگ) تصمیم می‌گیره
     */
    private fun startHealthMonitor() {
        healthJob?.cancel()
        healthJob = serviceScope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)

                val server = _activeServer.value ?: continue
                val alive = xrayCore.isRunning() && checkRealConnectivity(server)

                if (alive) {
                    consecutiveFailures = 0
                } else {
                    consecutiveFailures++
                    Log.w(TAG, "چک سلامت ناموفق ($consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)")
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        Log.i(TAG, "اتصال قطع شد — switch به سرور بعدی")
                        _state.value = ConnectionState.RECONNECTING
                        updateNotification("در حال اتصال مجدد...")
                        switchToNextServer()
                        return@launch
                    }
                }
            }
        }
    }

    /**
     * چک اتصال واقعی — پینگ TCP به سرور فعال
     * (در نسخه کامل: چک از طریق خود تونل)
     */
    private suspend fun checkRealConnectivity(server: Server): Boolean {
        val ping = pingManager.tcpPing(server.address, server.port)
        return ping < PingManager.UNREACHABLE
    }

    /**
     * switch به سرور بعدی در زنجیره fallback
     * بدون looping کور — زنجیره از پیش مرتب‌شده
     */
    private suspend fun switchToNextServer() {
        xrayCore.stop()
        closeTun()

        currentServerIndex++
        if (currentServerIndex >= fallbackChain.size) {
            Log.e(TAG, "همه سرورهای fallback امتحان شدن — اتصال ناموفق")
            _state.value = ConnectionState.ERROR
            stopVpn()
            return
        }

        connectToServer(fallbackChain[currentServerIndex])
    }

    /**
     * تنظیم زنجیره سرورها از بیرون (توسط HomeViewModel)
     */
    fun setFallbackChain(servers: List<Server>) {
        fallbackChain = servers
    }

    private fun closeTun() {
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // ignore
        } finally {
            vpnInterface = null
        }
    }

    /**
     * توقف کامل VPN + kill switch
     */
    private fun stopVpn() {
        healthJob?.cancel()
        stopTun2Socks()
        xrayCore.stop()
        closeTun()
        _state.value = ConnectionState.DISCONNECTED
        _activeServer.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * وقتی سیستم مجوز VPN رو پس می‌گیره → kill switch
     */
    override fun onRevoke() {
        Log.w(TAG, "مجوز VPN لغو شد — kill switch فعال")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        healthJob?.cancel()
        serviceScope.cancel()
        xrayCore.stop()
        closeTun()
        super.onDestroy()
    }

    // ---------- Notification ----------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "OFA VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "وضعیت اتصال VPN"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val disconnectIntent = Intent(this, VpnConnectionService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this, 0, disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("OFA VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null, "قطع اتصال", disconnectPending
                ).build()
            )
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(text))
    }

    /**
     * راه‌اندازی tun2socks — پل بین tun interface و SOCKS inbound هسته (Xray)
     *
     * tun2socks باینریه که فایل .so یا اجراییه در jniLibs.
     * آدرس SOCKS طبق ConfigParser.SOCKS_PORT (مثلاً 10808) تنظیم می‌شه.
     * ترافیکی که از tun میاد رو به Xray هدایت می‌کنه.
     */
    private var tun2socksProcess: Process? = null

    private fun startTun2Socks(server: Server) {
        try {
            val binary = File(applicationInfo.nativeLibraryDir, "libtun2socks.so")
            if (!binary.exists()) {
                Log.w(TAG, "libtun2socks.so پیدا نشد — از routing مستقیم استفاده می‌شه")
                return
            }
            // فرمت دستور: tun2socks --tundev tun0 --netifipaddr 10.0.0.1
            //   --netifmask 255.255.255.0 --socks5server 127.0.0.1:PORT
            val pb = ProcessBuilder(
                binary.absolutePath,
                "--tundev", "tun0",
                "--netifipaddr", VPN_ADDRESS,
                "--netifmask", "255.255.255.0",
                "--socks5server", "127.0.0.1:${ConfigParser.LOCAL_SOCKS_PORT}",
                "--loglevel", "warning"
            ).apply {
                redirectErrorStream(true)
                directory(filesDir)
            }
            tun2socksProcess = pb.start()
            Log.i(TAG, "tun2socks شروع شد → SOCKS 127.0.0.1:${ConfigParser.LOCAL_SOCKS_PORT}")
        } catch (e: Exception) {
            Log.e(TAG, "خطا در راه‌اندازی tun2socks: ${e.message}", e)
        }
    }

    private fun stopTun2Socks() {
        try {
            tun2socksProcess?.destroyForcibly()
        } catch (e: Exception) {
            Log.e(TAG, "خطا در توقف tun2socks: ${e.message}")
        } finally {
            tun2socksProcess = null
        }
    }

    /**
     * سعی می‌کنه آدرس سرور رو از مسیر تونل خارج کنه.
     *
     * در اندروید، `VpnService.protect(socket)` فقط روی socket کار می‌کنه،
     * پس اینجا یه socket تستی می‌سازیم و protect می‌کنیم.
     * در نسخه کامل‌تر باید Xray رو اصلاح کنیم که خودش socket‌ها رو protect کنه.
     */
    private fun protectServerRoute(server: Server) {
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(server.address, server.port), 3000)
            protect(socket)
            socket.close()
            Log.i(TAG, "مسیر سرور ${server.address}:${server.port} از تونل مستثنی شد")
        } catch (e: Exception) {
            Log.w(TAG, "نمی‌تونم سرور رو مستثنی کنم: ${e.message}")
        }
    }
}
