package com.ofa.vpn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import com.ofa.vpn.ui.OFAVPNApp

/**
 * نقطه ورود اپ — ثبت شده در AndroidManifest با package com.ofa.vpn
 *
 * مسئولیت‌ها:
 *  - میزبانی Compose UI
 *  - مدیریت مجوز VPN (VpnService.prepare)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingPermissionAction?.invoke()
        }
        pendingPermissionAction = null
    }

    private var pendingPermissionAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OFAVPNApp(
                onVpnPermissionRequest = { intent, onGranted ->
                    pendingPermissionAction = onGranted
                    vpnPermissionLauncher.launch(intent)
                }
            )
        }
    }
}
