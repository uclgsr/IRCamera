package mpdc4gsr.core.ui

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
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
abstract class HiltComposeActivity : ComponentActivity() {
    
    @Composable
    protected abstract fun Content()
    
    protected open fun onDeviceConnected() {}
    protected open fun onDeviceDisconnected() {}
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibUnifiedTheme {
                Content()
                HandleConnectionEvents()
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
    private fun HandleConnectionEvents() {
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
