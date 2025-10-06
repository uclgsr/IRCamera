package com.mpdc4gsr.libunified.app.compose.base
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
abstract class BaseComposeActivity<VM : BaseViewModel> : ComponentActivity() {
    protected abstract fun createViewModel(): VM
    @Composable
    protected abstract fun Content(viewModel: VM)
    protected open fun onDeviceConnected() {}
    protected open fun onDeviceDisconnected() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibUnifiedTheme {
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
                ConstantLanguages.ENGLISH
            )
        )
    }
    @Composable
    private fun HandleConnectionEvents(viewModel: VM) {
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                DeviceEventManager.deviceConnectionState.collectLatest { state ->
                    state?.let {
                        if (it.isConnected) {
                            onDeviceConnected()
                        } else {
                            onDeviceDisconnected()
                        }
                    }
                }
            }
        }
    }
}