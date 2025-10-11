package mpdc4gsr.feature.capture.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.feature.capture.gsr.presentation.GSRSettingsViewModel

@AndroidEntryPoint
class GSRSettingsComposeActivity : BaseComposeActivity<GSRSettingsViewModel>() {
    companion object {
        private const val TAG = "GSRSettingsComposeActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsComposeActivity::class.java))
        }
    }

    override fun createViewModel(): GSRSettingsViewModel = viewModels<GSRSettingsViewModel>().value

    @Composable
    override fun Content(viewModel: GSRSettingsViewModel) {
        LibSharedTheme {
            GSRSettingsScreen(
                onBackClick = { finish() },
                viewModel = viewModel,
            )
        }
    }
}



