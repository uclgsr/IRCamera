package mpdc4gsr.network

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity

/**
 * Modern Device Pairing Activity using Jetpack Compose
 * Migrated from traditional View-based implementation to Compose
 * 
 * Features:
 * - Network device discovery
 * - BLE device pairing
 * - Real-time connection status
 * - Modern Material3 UI
 */
class DevicePairingComposeActivity : BaseComposeActivity<DevicePairingViewModel>() {

    companion object {
        private const val TAG = "DevicePairingComposeActivity"

        fun start(context: Context) {
            val intent = Intent(context, DevicePairingComposeActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): DevicePairingViewModel {
        val viewModel: DevicePairingViewModel by viewModels()
        // Initialize the viewModel with context
        viewModel.initialize(this)
        return viewModel
    }

    @Composable
    override fun Content(viewModel: DevicePairingViewModel) {
        DevicePairingScreen(
            viewModel = viewModel,
            onBackClick = { finish() }
        )
    }

    override fun onDeviceConnected() {
        super.onDeviceConnected()
        // Handle device connection events if needed
    }

    override fun onDeviceDisconnected() {
        super.onDeviceDisconnected()
        // Handle device disconnection events if needed
    }
}