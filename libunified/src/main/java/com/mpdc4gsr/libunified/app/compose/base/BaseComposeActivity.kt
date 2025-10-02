package com.mpdc4gsr.libunified.app.compose.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.bean.event.SocketStateEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DeviceConnectEvent
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


abstract class BaseComposeActivity<VM : BaseViewModel> : ComponentActivity() {

    protected abstract fun createViewModel(): VM

    @Composable
    protected abstract fun Content(viewModel: VM)

    protected open fun onDeviceConnected() {}
    protected open fun onDeviceDisconnected() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this)

        setContent {
            LibUnifiedTheme {
                val viewModel = createViewModel()
                Content(viewModel)

                // Handle connection state changes in Compose
                HandleConnectionEvents(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase,
                ConstantLanguages.ENGLISH
            )
        )
    }

    @Composable
    private fun HandleConnectionEvents(viewModel: VM) {
        // Generic connection event handling can be added here
        // Subclasses can override onDeviceConnected/onDeviceDisconnected for specific behavior
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onUSBLineStateChange(event: DeviceConnectEvent) {
        if (event.isConnect) {
            onDeviceConnected()
        } else {
            onDeviceDisconnected()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onSocketStateChange(event: SocketStateEvent) {
        // Handle socket state changes - can be overridden by subclasses
    }
}