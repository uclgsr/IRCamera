package mpdc4gsr.permissions

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity

/**
 * Enhanced Permission Request Activity using Jetpack Compose
 * Migrated from traditional View-based implementation to Compose
 * 
 * Features:
 * - Modern Material3 permission interface
 * - Real-time permission status updates
 * - Comprehensive permission testing
 * - Activity logging with auto-scroll
 * - Enhanced UX with visual status indicators
 */
class PermissionRequestComposeActivity : BaseComposeActivity<PermissionRequestViewModel>() {

    companion object {
        private const val TAG = "PermissionRequestComposeActivity"

        fun start(context: Context) {
            val intent = Intent(context, PermissionRequestComposeActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): PermissionRequestViewModel {
        val viewModel: PermissionRequestViewModel by viewModels()
        // Initialize the viewModel with context
        viewModel.initialize(this)
        return viewModel
    }

    @Composable
    override fun Content(viewModel: PermissionRequestViewModel) {
        PermissionRequestScreen(
            viewModel = viewModel,
            onBackClick = { finish() }
        )
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission status when activity resumes
        // This handles cases where user grants permissions in system settings
        if (::createViewModel.isInitialized) {
            createViewModel().updatePermissionStatus()
        }
    }
}