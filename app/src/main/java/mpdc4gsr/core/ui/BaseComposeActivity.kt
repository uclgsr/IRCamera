package mpdc4gsr.core.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.bean.event.SocketStateEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DeviceConnectEvent
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import mpdc4gsr.core.ui.theme.IRCameraTheme
import androidx.lifecycle.ViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Base Compose Activity that provides:
 * - Theme integration
 * - EventBus registration (backward compatibility)
 * - Language handling
 * - ViewModel integration patterns
 *
 * @param VM The type of [androidx.lifecycle.ViewModel] used by this activity.
 *           Any subclass of [ViewModel] is supported. There is no dependency on project-specific
 *           BaseViewModel APIs; implementations should not assume the presence of any APIs beyond
 *           those provided by [ViewModel] itself.
 *           Subclasses are responsible for providing a [ViewModel] via [createViewModel] and
 *           implementing [Content] to display UI using the provided [ViewModel].
 */
abstract class BaseComposeActivity<VM : ViewModel> : FragmentActivity() {

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