package mpdc4gsr.feature.capture.thermal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

@AndroidEntryPoint
class ThermalComposeActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SCREEN_TYPE = "screen_type"
        const val SCREEN_CAMERA = "camera"
        const val SCREEN_SETTINGS = "settings"
        const val SCREEN_CALIBRATION = "calibration"
        const val SCREEN_GALLERY = "gallery"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenType = intent.getStringExtra(EXTRA_SCREEN_TYPE) ?: SCREEN_CAMERA

        setContent {
            IRCameraTheme {
                ThermalScreenRouter(
                    screenType = screenType,
                    onBackClick = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ThermalScreenRouter(
    screenType: String,
    onBackClick: () -> Unit,
) {
    when (screenType) {
        ThermalComposeActivity.SCREEN_CAMERA -> {
            ThermalCameraScreen(
                onBackClick = onBackClick,
                onNavigateToSettings = { /* Navigate to settings */ },
                onNavigateToGallery = { /* Navigate to gallery */ },
            )
        }

        ThermalComposeActivity.SCREEN_SETTINGS -> {
            ThermalSettingsScreen(
                onBackClick = onBackClick,
            )
        }

        ThermalComposeActivity.SCREEN_CALIBRATION -> {
            ThermalCalibrationScreen(
                onBackClick = onBackClick,
            )
        }

        ThermalComposeActivity.SCREEN_GALLERY -> {
            ThermalGalleryScreen(
                onBackClick = onBackClick,
            )
        }

        else -> {
            ThermalCameraScreen(
                onBackClick = onBackClick,
                onNavigateToSettings = { /* Navigate to settings */ },
                onNavigateToGallery = { /* Navigate to gallery */ },
            )
        }
    }
}
