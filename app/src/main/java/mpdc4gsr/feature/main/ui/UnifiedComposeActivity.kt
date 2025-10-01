package mpdc4gsr.feature.main.ui

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import mpdc4gsr.core.ui.BaseComposeActivity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import mpdc4gsr.core.ui.navigation.UnifiedNavHost

/**
 * Unified Compose Activity - Modern Navigation Entry Point
 *
 * This activity demonstrates the culmination of the modernization effort:
 * - Uses shared BaseComposeActivity from libunified
 * - Implements unified navigation system
 * - Provides seamless access to all modernized components
 * - Serves as the primary entry point for the modern Compose app
 */
class UnifiedComposeActivity : BaseComposeActivity<UnifiedComposeViewModel>() {

    override fun createViewModel(): UnifiedComposeViewModel {
        return viewModels<UnifiedComposeViewModel>().value
    }

    @Composable
    override fun Content(viewModel: UnifiedComposeViewModel) {
        UnifiedNavHost()
    }
}

/**
 * Simple ViewModel for the unified activity
 */
class UnifiedComposeViewModel : BaseViewModel() {
    // This activity primarily handles navigation, so minimal ViewModel needed
    // Future enhancements could include:
    // - Global app state management
    // - User preferences
    // - Authentication state
    // - Network connectivity monitoring
}