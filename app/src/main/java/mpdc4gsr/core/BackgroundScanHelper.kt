package mpdc4gsr.core

import android.content.Context
import android.content.Intent
import mpdc4gsr.feature.system.service.BackgroundScanService

object BackgroundScanHelper {

    fun startBackgroundScanning(context: Context) {
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = BackgroundScanService.ACTION_START_SCANNING
        }
        context.startForegroundService(intent)
    }

    fun stopBackgroundScanning(context: Context) {
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = BackgroundScanService.ACTION_STOP_SCANNING
        }
        context.startService(intent)
    }

    fun pauseBackgroundScanning(context: Context) {
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = BackgroundScanService.ACTION_PAUSE_SCANNING
        }
        context.startService(intent)
    }

    fun resumeBackgroundScanning(context: Context) {
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = BackgroundScanService.ACTION_RESUME_SCANNING
        }
        context.startService(intent)
    }
}