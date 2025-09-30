package mpdc4gsr.ui_components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.user.fragment.MineFragmentCompose
import com.mpdc4gsr.module.user.viewmodel.MineFragmentViewModel

/**
 * Wrapper fragment that delegates to MineFragmentCompose
 * This allows MainActivityLegacy to use MineFragment while the actual implementation is in Compose
 */
class MineFragment : Fragment() {

    private val viewModel: MineFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Return a ComposeView that renders the Compose content directly
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LibUnifiedTheme {
                    MineFragmentComposeContent(viewModel)
                }
            }
        }
    }

    @Composable
    private fun MineFragmentComposeContent(viewModel: MineFragmentViewModel) {
        // Render the MineFragmentCompose content
        val composeFragment = MineFragmentCompose()
        composeFragment.Content(viewModel)
    }
}
