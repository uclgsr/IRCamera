package mpdc4gsr.compose.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.compose.theme.IRCameraTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

/**
 * Base Compose Fragment that enables gradual migration from traditional Fragments
 * Provides:
 * - Compose integration within Fragment lifecycle
 * - Theme consistency
 * - ViewModel integration
 * - Interoperability with existing Fragment-based navigation
 */
abstract class BaseComposeFragment<VM : BaseViewModel> : Fragment() {

    protected abstract fun createViewModel(): VM

    @Composable
    protected abstract fun Content(viewModel: VM)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                IRCameraTheme {
                    val viewModel = createViewModel()
                    Content(viewModel)
                }
            }
        }
    }
}

/**
 * Simplified version for Composables that don't need ViewModels
 */
abstract class SimpleComposeFragment : Fragment() {

    @Composable
    protected abstract fun Content()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                IRCameraTheme {
                    Content()
                }
            }
        }
    }
}