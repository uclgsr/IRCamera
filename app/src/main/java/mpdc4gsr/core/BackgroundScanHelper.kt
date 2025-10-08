package mpdc4gsr.core

import android.content.Context
import android.content.Intent
import android.util.Log
import mpdc4gsr.feature.system.service.BackgroundScanService
object BackgroundScanHelper {
    private const val TAG = "BackgroundScanHelper"

    fun startBackgroundScanning(context: Context) {
        try {
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_START_SCANNING
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
        }
    }

    fun stopBackgroundScanning(context: Context) {
        try {
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_STOP_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
        }
    }

    fun pauseBackgroundScanning(context: Context) {
        try {
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_PAUSE_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
        }
    }

    fun resumeBackgroundScanning(context: Context) {
        try {
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_RESUME_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
        }
    }
}