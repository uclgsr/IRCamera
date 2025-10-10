package mpdc4gsr.core

import android.content.Context
import android.content.Intent

object BackgroundScanHelper {
    private const val TAG = "BackgroundScanHelper"

    fun startBackgroundScanning(context: Context) {
        try {
            val intent =
                Intent(context, BackgroundDeviceScanningService::class.java).apply {
                    action = BackgroundDeviceScanningService.ACTION_START_SCANNING
                }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("BackgroundScanHelper", "Unexpected Exception in BackgroundScanHelper catch block", e)
        }
    }

    fun stopBackgroundScanning(context: Context) {
        try {
            val intent =
                Intent(context, BackgroundDeviceScanningService::class.java).apply {
                    action = BackgroundDeviceScanningService.ACTION_STOP_SCANNING
                }
            context.startService(intent)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("BackgroundScanHelper", "Unexpected Exception in BackgroundScanHelper catch block", e)
        }
    }

    fun pauseBackgroundScanning(context: Context) {
        try {
            val intent =
                Intent(context, BackgroundDeviceScanningService::class.java).apply {
                    action = BackgroundDeviceScanningService.ACTION_PAUSE_SCANNING
                }
            context.startService(intent)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("BackgroundScanHelper", "Unexpected Exception in BackgroundScanHelper catch block", e)
        }
    }

    fun resumeBackgroundScanning(context: Context) {
        try {
            val intent =
                Intent(context, BackgroundDeviceScanningService::class.java).apply {
                    action = BackgroundDeviceScanningService.ACTION_RESUME_SCANNING
                }
            context.startService(intent)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("BackgroundScanHelper", "Unexpected Exception in BackgroundScanHelper catch block", e)
        }
    }
}
