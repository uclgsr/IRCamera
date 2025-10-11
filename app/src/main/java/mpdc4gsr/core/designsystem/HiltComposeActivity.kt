package mpdc4gsr.core.designsystem

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.shared.app.event.DeviceEventManager
import com.mpdc4gsr.component.shared.app.tools.AppLanguageUtils
import com.mpdc4gsr.component.shared.app.tools.ConstantLanguages
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
abstract class HiltComposeActivity : ComponentActivity() {
    @Composable
    protected abstract fun Content()

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
                Content()
                HandleConnectionEvents()
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
    private fun HandleConnectionEvents() {
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



