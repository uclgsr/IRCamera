package mpdc4gsr.core

import android.content.Context
import android.content.Intent
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import mpdc4gsr.feature.system.service.BackgroundScanService

object BackgroundScanHelper {
    private const val TAG = "BackgroundScanHelper"

    fun startBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Starting background device scanning")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_START_SCANNING
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start background scanning", e)
        }
    }

    fun stopBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Stopping background device scanning")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_STOP_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop background scanning", e)
        }
    }

    fun pauseBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Pausing background device scanning")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_PAUSE_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to pause background scanning", e)
        }
    }

    fun resumeBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Resuming background device scanning")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_RESUME_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to resume background scanning", e)
        }
    }
}