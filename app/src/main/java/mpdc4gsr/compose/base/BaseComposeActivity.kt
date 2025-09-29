package mpdc4gsr.compose.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.bean.event.SocketStateEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DeviceConnectEvent
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import mpdc4gsr.compose.theme.IRCameraTheme
import mpdc4gsr.viewmodel.BaseViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Base Compose Activity that provides:
 * - Theme integration
 * - EventBus registration (backward compatibility)
 * - Language handling
 * - Connection state handling
 * - ViewModel integration patterns
 */
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
            IRCameraTheme {
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
        val connectionState by viewModel.connectionState.collectAsState()

        LaunchedEffect(connectionState) {
            when (connectionState) {
                is mpdc4gsr.viewmodel.ConnectionState.Connected -> onDeviceConnected()
                is mpdc4gsr.viewmodel.ConnectionState.Disconnected -> onDeviceDisconnected()
                else -> { /* Handle other states */
                }
            }
        }
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