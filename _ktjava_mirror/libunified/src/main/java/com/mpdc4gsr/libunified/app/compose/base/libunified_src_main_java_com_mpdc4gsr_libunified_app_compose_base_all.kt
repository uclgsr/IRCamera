// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base\BaseComposeActivity.kt =====

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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\base\BaseComposeFragment.kt =====

package com.mpdc4gsr.libunified.app.compose.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

abstract class BaseComposeFragment<VM : ViewModel> : Fragment() {
    abstract fun createViewModel(): VM

    @Composable
    abstract fun Content(viewModel: VM)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Use ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            // to ensure proper cleanup when fragment is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LibUnifiedTheme {
                    Content(createViewModel())
                }
            }
        }
    }

    open fun onFragmentCreated() {
        // Default implementation does nothing
        // Override in subclasses for specific initialization
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated()
    }
}

abstract class EnhancedBaseComposeFragment<VM : ViewModel> : BaseComposeFragment<VM>() {
    open val handlesBackPress: Boolean = false
    open fun onBackPressed(): Boolean {
        return false
    }

    open fun onFragmentDestroyed() {
        // Default implementation does nothing
        // Override in subclasses for specific cleanup
    }

    override fun onDestroyView() {
        onFragmentDestroyed()
        super.onDestroyView()
    }
}

abstract class BaseThermalComposeFragment<VM : ViewModel> : EnhancedBaseComposeFragment<VM>() {
    open fun onThermalFragmentCreated() {
        // Thermal-specific initialization
    }

    open fun onThermalDeviceStateChanged(connected: Boolean) {
        // Default implementation does nothing
    }

    override fun onFragmentCreated() {
        super.onFragmentCreated()
        onThermalFragmentCreated()
    }
}


