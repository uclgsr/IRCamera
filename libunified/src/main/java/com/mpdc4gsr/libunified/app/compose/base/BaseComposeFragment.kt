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

/**
 * BaseComposeFragment - Foundation class for all Compose-based fragments
 *
 * This class provides:
 * - Consistent Compose integration pattern
 * - Theme integration with LibUnifiedTheme
 * - ViewModel integration with lifecycle management
 * - Proper composition strategy for fragment lifecycle
 * - Foundation for hybrid Fragment-Compose architecture
 */
abstract class BaseComposeFragment<VM : ViewModel> : Fragment() {

    /**
     * Create and return the ViewModel for this fragment
     * Override this method to provide your specific ViewModel
     */
    abstract fun createViewModel(): VM

    /**
     * Define the Compose content for this fragment
     * This is where you build your Compose UI
     */
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

    /**
     * Optional method to handle fragment-specific initialization
     * Override if you need to perform additional setup
     */
    open fun onFragmentCreated() {
        // Default implementation does nothing
        // Override in subclasses for specific initialization
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated()
    }
}

/**
 * Enhanced BaseComposeFragment with additional functionality
 * for fragments that need more complex integration
 */
abstract class EnhancedBaseComposeFragment<VM : ViewModel> : BaseComposeFragment<VM>() {

    /**
     * Flag to indicate if this fragment should handle back press
     */
    open val handlesBackPress: Boolean = false

    /**
     * Handle back press events
     * Override this method if handlesBackPress is true
     */
    open fun onBackPressed(): Boolean {
        return false
    }

    /**
     * Optional method for fragments that need to perform cleanup
     */
    open fun onFragmentDestroyed() {
        // Default implementation does nothing
        // Override in subclasses for specific cleanup
    }

    override fun onDestroyView() {
        onFragmentDestroyed()
        super.onDestroyView()
    }
}

/**
 * Specialized BaseComposeFragment for thermal imaging fragments
 * Provides thermal-specific functionality and theming
 */
abstract class BaseThermalComposeFragment<VM : ViewModel> : EnhancedBaseComposeFragment<VM>() {

    /**
     * Thermal-specific initialization
     * Override for thermal fragment setup
     */
    open fun onThermalFragmentCreated() {
        // Thermal-specific initialization
    }

    /**
     * Handle thermal device events
     * Override to respond to thermal device state changes
     */
    open fun onThermalDeviceStateChanged(connected: Boolean) {
        // Default implementation does nothing
    }

    override fun onFragmentCreated() {
        super.onFragmentCreated()
        onThermalFragmentCreated()
    }
}