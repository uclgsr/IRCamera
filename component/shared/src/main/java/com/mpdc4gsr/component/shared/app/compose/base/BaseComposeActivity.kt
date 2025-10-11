package com.mpdc4gsr.component.shared.app.compose.base

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.shared.app.event.DeviceEventManager
import com.mpdc4gsr.component.shared.app.tools.AppLanguageUtils
import com.mpdc4gsr.component.shared.app.tools.ConstantLanguages
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseComposeActivity<VM : ViewModel> : ComponentActivity() {
    protected abstract fun createViewModel(): VM

    @Composable
    protected abstract fun Content(viewModel: VM)

    protected open fun onDeviceConnected() {}

    protected open fun onDeviceDisconnected() {}

    protected var deviceConnectionActive: Boolean = false
        private set
    protected var connectedUsbDevice: UsbDevice? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibSharedTheme {
                val viewModel = createViewModel()
                Content(viewModel)
                HandleConnectionEvents(viewModel)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase,
                ConstantLanguages.ENGLISH,
            ),
        )
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
}



