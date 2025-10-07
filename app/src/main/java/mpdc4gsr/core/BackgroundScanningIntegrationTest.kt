package mpdc4gsr.core

import android.content.Context
import mpdc4gsr.core.utils.AppLogger

class BackgroundScanningIntegrationTest {
    companion object {
        private const val TAG = "BackgroundScanIntegration"
    }

    fun startScanningOnAppLaunch(context: Context) {
        AppLogger.i(TAG, "Starting background scanning on app launch")
        // Option 1: Using the simple helper
        BackgroundScanHelper.startBackgroundScanning(context)
        // Option 2: Using the full manager for more control
        val manager = BackgroundScanningManager(context)
        manager.startBackgroundScanning()
    }

    fun pauseScanningDuringActiveUse(context: Context) {
        AppLogger.i(TAG, "Pausing background scanning during active use")
        BackgroundScanHelper.pauseBackgroundScanning(context)
    }

    fun resumeScanningOnBackground(context: Context) {
        AppLogger.i(TAG, "Resuming background scanning when app goes to background")
        BackgroundScanHelper.resumeBackgroundScanning(context)
    }

    fun stopScanningOnUserRequest(context: Context) {
        AppLogger.i(TAG, "Stopping background scanning on user request")
        BackgroundScanHelper.stopBackgroundScanning(context)
    }

    fun integrateWithExistingBleWorkflow(context: Context) {
        AppLogger.i(TAG, "Integrating background scanning with existing BLE workflow")
        // Start background scanning
        BackgroundScanHelper.startBackgroundScanning(context)
        // The BackgroundDeviceScanningService will:
        // 1. Use the existing BleDeviceManager for device discovery
        // 2. Respect the existing GSR device filtering
        // 3. Maintain device information that can be accessed later
        // 4. Provide notifications about discovered devices
        AppLogger.i(TAG, "Background scanning integrated successfully")
    }

    fun useInRecordingSession(context: Context) {
        AppLogger.i(TAG, "Using background scanning in recording session")
        // Start background scanning before recording session
        BackgroundScanHelper.startBackgroundScanning(context)
        // During recording, pause to save battery
        BackgroundScanHelper.pauseBackgroundScanning(context)
        // After recording, resume scanning
        BackgroundScanHelper.resumeBackgroundScanning(context)
    }

    fun batteryAwareScanning(context: Context, batteryLevel: Int) {
        AppLogger.i(TAG, "Implementing battery-aware scanning, battery level: $batteryLevel%")
        when {
            batteryLevel > 50 -> {
                // High battery - normal scanning
                BackgroundScanHelper.startBackgroundScanning(context)
                AppLogger.i(TAG, "Normal background scanning enabled (battery > 50%)")
            }

            batteryLevel > 20 -> {
                // Medium battery - start but pause frequently
                BackgroundScanHelper.startBackgroundScanning(context)
                // The service will automatically use longer intervals when no devices found
                AppLogger.i(TAG, "Conservative background scanning enabled (battery 20-50%)")
            }

            else -> {
                // Low battery - disable background scanning
                BackgroundScanHelper.stopBackgroundScanning(context)
                AppLogger.i(TAG, "Background scanning disabled due to low battery (< 20%)")
            }
        }
    }
}