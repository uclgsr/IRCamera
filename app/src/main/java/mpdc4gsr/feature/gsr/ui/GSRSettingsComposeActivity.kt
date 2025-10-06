package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel

/**
 * GSRSettingsComposeActivity - Compose Migration Demonstration
 *
 * This demonstrates the next phase of modernization:
 * - Migration from traditional BaseViewModelActivity to shared BaseComposeActivity
 * - Modern Compose UI with Material 3 components
 * - Preserved ViewModel and business logic
 * - Enhanced user experience with consistent theming
 */
class GSRSettingsComposeActivity : BaseComposeActivity<GSRSettingsViewModel>() {

    companion object {
        private const val TAG = "GSRSettingsComposeActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsComposeActivity::class.java))
        }
    }

    override fun createViewModel(): GSRSettingsViewModel {
        return viewModels<GSRSettingsViewModel>().value
    }

    @Composable
    override fun Content(viewModel: GSRSettingsViewModel) {
        GSRSettingsScreen(
            onBackClick = { finish() },
            viewModel = viewModel
        )
    }
}
