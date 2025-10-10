package mpdc4gsr.feature.dashboard.ui

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import mpdc4gsr.core.designsystem.navigation.UnifiedNavHost

class UnifiedComposeActivity : BaseComposeActivity<UnifiedComposeViewModel>() {
    override fun createViewModel(): UnifiedComposeViewModel = viewModels<UnifiedComposeViewModel>().value

    @Composable
    override fun Content(viewModel: UnifiedComposeViewModel) {
        UnifiedNavHost()
    }
}

class UnifiedComposeViewModel : BaseViewModel() {
    // This activity primarily handles navigation, so minimal ViewModel needed
    // Future enhancements could include:
    // - Global app state management
    // - User preferences
    // - Authentication state
    // - Network connectivity monitoring
}

