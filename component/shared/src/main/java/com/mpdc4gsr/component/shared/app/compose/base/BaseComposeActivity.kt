package com.mpdc4gsr.component.shared.app.compose.base

import android.content.Context
import android.hardware.usb.UsbDevice
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.shared.app.event.DeviceEventManager
import com.mpdc4gsr.component.shared.app.permissions.FeaturePermissionArea
import com.mpdc4gsr.component.shared.app.tools.AppLanguageUtils
import com.mpdc4gsr.component.shared.app.tools.ConstantLanguages
import com.mpdc4gsr.component.shared.app.lms.weiget.TToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

abstract class BaseComposeActivity<VM : ViewModel> : ComponentActivity() {
    protected abstract fun createViewModel(): VM

    @Composable
    protected abstract fun Content(viewModel: VM)

    protected open fun onDeviceConnected() {}

    protected open fun onDeviceDisconnected() {}

    protected open val requiredPermissionAreas: Set<FeaturePermissionArea> =
        setOf(FeaturePermissionArea.THERMAL_IR)

    protected var deviceConnectionActive: Boolean = false
        private set
    protected var connectedUsbDevice: UsbDevice? = null
        private set
    private var permissionRequestAttempted = false
    private var pendingPermissionMessage: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                handlePermissionResult()
            }
        enableEdgeToEdge()
        setContent {
            LibSharedTheme {
                val viewModel = createViewModel()
                Content(viewModel)
                HandleConnectionEvents(viewModel)
            }
        }
        requestPermissionsIfNeeded(force = true)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase,
                ConstantLanguages.ENGLISH,
            ),
        )
    }

    override fun onResume() {
        super.onResume()
        requestPermissionsIfNeeded()
    }

    @Composable
    private fun HandleConnectionEvents(viewModel: VM) {
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                DeviceEventManager.deviceConnectionState.collectLatest { state ->
                    state?.let {
                        if (it.isConnected) {
                            deviceConnectionActive = true
                            connectedUsbDevice = it.device
                            onDeviceConnected()
                        } else {
                            deviceConnectionActive = false
                            connectedUsbDevice = null
                            onDeviceDisconnected()
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded(force: Boolean = false) {
        if (requiredPermissionAreas.isEmpty()) return
        val missing = gatherMissingPermissions()
        if (missing.isEmpty()) {
            permissionRequestAttempted = false
            return
        }
        if (!force && permissionRequestAttempted) return
        permissionRequestAttempted = true
        pendingPermissionMessage =
            requiredPermissionAreas
                .joinToString(", ") { it.title }
                .takeIf { it.isNotBlank() }
                ?.let { "Permissions needed for: $it" }
        permissionLauncher.launch(missing.toTypedArray())
    }

    private fun gatherMissingPermissions(): List<String> =
        requiredPermissionAreas
            .flatMap { it.allPermissions() }
            .distinct()
            .filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }

    private fun handlePermissionResult() {
        val missing = gatherMissingPermissions()
        permissionRequestAttempted = missing.isNotEmpty()
        if (permissionRequestAttempted) {
            pendingPermissionMessage?.let { TToast.shortToast(this, it) }
        } else {
            pendingPermissionMessage = null
        }
    }
}
