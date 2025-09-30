package mpdc4gsr.ui_components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.mpdc4gsr.module.user.fragment.MineFragmentCompose

/**
 * Wrapper fragment that delegates to MineFragmentCompose
 * This allows MainActivityLegacy to use MineFragment while the actual implementation is in Compose
 */
class MineFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Delegate to the Compose implementation
        val mineFragmentCompose = MineFragmentCompose()
        
        // Transfer arguments if any
        mineFragmentCompose.arguments = arguments
        
        // Use childFragmentManager to add the compose fragment
        childFragmentManager.beginTransaction()
            .replace(android.R.id.content, mineFragmentCompose)
            .commit()
        
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
    }
}
