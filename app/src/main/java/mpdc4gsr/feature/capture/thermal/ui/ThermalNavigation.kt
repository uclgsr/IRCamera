package mpdc4gsr.feature.capture.thermal.ui

import android.content.Context
import android.content.Intent

object ThermalNavigation {
    fun navigateToCamera(context: Context) {
        val intent =
            Intent(context, ThermalComposeActivity::class.java).apply {
                putExtra(ThermalComposeActivity.EXTRA_SCREEN_TYPE, ThermalComposeActivity.SCREEN_CAMERA)
            }
        context.startActivity(intent)
    }

    fun navigateToSettings(context: Context) {
        val intent =
            Intent(context, ThermalComposeActivity::class.java).apply {
                putExtra(ThermalComposeActivity.EXTRA_SCREEN_TYPE, ThermalComposeActivity.SCREEN_SETTINGS)
            }
        context.startActivity(intent)
    }

    fun navigateToCalibration(context: Context) {
        val intent =
            Intent(context, ThermalComposeActivity::class.java).apply {
                putExtra(ThermalComposeActivity.EXTRA_SCREEN_TYPE, ThermalComposeActivity.SCREEN_CALIBRATION)
            }
        context.startActivity(intent)
    }

    fun navigateToGallery(context: Context) {
        val intent =
            Intent(context, ThermalComposeActivity::class.java).apply {
                putExtra(ThermalComposeActivity.EXTRA_SCREEN_TYPE, ThermalComposeActivity.SCREEN_GALLERY)
            }
        context.startActivity(intent)
    }
}
