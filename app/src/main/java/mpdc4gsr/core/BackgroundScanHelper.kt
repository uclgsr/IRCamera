package mpdc4gsr.core

import android.content.Context
import android.content.Intent
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler

/**
 * Simple helper functions for managing background device scanning
 * Can be used from any activity or service throughout the app
 */
object BackgroundScanHelper {

    private const val TAG = "BackgroundScanHelper"

    /**
     * Start background device scanning service
     * This will start a foreground service that continuously scans for BLE devices
     */
    fun startBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Starting background device scanning")
            val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                action = BackgroundDeviceScanningService.ACTION_START_SCANNING
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start background scanning", e)
        }
    }

    /**
     * Stop background device scanning service
     */
    fun stopBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Stopping background device scanning")
            val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                action = BackgroundDeviceScanningService.ACTION_STOP_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop background scanning", e)
        }
    }

    /**
     * Pause background device scanning (keeps service running but stops scanning)
     */
    fun pauseBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Pausing background device scanning")
            val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                action = BackgroundDeviceScanningService.ACTION_PAUSE_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to pause background scanning", e)
        }
    }

    /**
     * Resume background device scanning
     */
    fun resumeBackgroundScanning(context: Context) {
        try {
            AppLogger.i(TAG, "Resuming background device scanning")
            val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                action = BackgroundDeviceScanningService.ACTION_RESUME_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to resume background scanning", e)
        }
    }
}