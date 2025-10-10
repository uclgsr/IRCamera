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
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            // Use ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            // to ensure proper cleanup when fragment is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LibUnifiedTheme {
                    Content(createViewModel())
                }
            }
        }

    open fun onFragmentCreated() {
        // Default implementation does nothing
        // Override in subclasses for specific initialization
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated()
    }
}

abstract class EnhancedBaseComposeFragment<VM : ViewModel> : BaseComposeFragment<VM>() {
    open val handlesBackPress: Boolean = false

    open fun onBackPressed(): Boolean = false

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
