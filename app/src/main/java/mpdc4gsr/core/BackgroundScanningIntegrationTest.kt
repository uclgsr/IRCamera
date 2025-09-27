package mpdc4gsr.core

import android.content.Context
import android.util.Log

/**
 * Integration test and example usage for the Background Device Scanning system
 * This demonstrates how to properly use the background scanning in different scenarios
 */
class BackgroundScanningIntegrationTest {
    
    companion object {
        private const val TAG = "BackgroundScanIntegration"
    }
    
    /**
     * Example: Start background scanning when app is launched
     */
    fun startScanningOnAppLaunch(context: Context) {
        Log.i(TAG, "Starting background scanning on app launch")
        
        // Option 1: Using the simple helper
        BackgroundScanHelper.startBackgroundScanning(context)
        
        // Option 2: Using the full manager for more control
        val manager = BackgroundScanningManager(context)
        manager.startBackgroundScanning()
    }
    
    /**
     * Example: Pause scanning when user is actively using the app
     */
    fun pauseScanningDuringActiveUse(context: Context) {
        Log.i(TAG, "Pausing background scanning during active use")
        BackgroundScanHelper.pauseBackgroundScanning(context)
    }
    
    /**
     * Example: Resume scanning when app goes to background
     */
    fun resumeScanningOnBackground(context: Context) {
        Log.i(TAG, "Resuming background scanning when app goes to background")
        BackgroundScanHelper.resumeBackgroundScanning(context)
    }
    
    /**
     * Example: Stop scanning when user explicitly disables it
     */
    fun stopScanningOnUserRequest(context: Context) {
        Log.i(TAG, "Stopping background scanning on user request")
        BackgroundScanHelper.stopBackgroundScanning(context)
    }
    
    /**
     * Example: Integration with existing BLE workflow
     */
    fun integrateWithExistingBleWorkflow(context: Context) {
        Log.i(TAG, "Integrating background scanning with existing BLE workflow")
        
        // Start background scanning
        BackgroundScanHelper.startBackgroundScanning(context)
        
        // The BackgroundDeviceScanningService will:
        // 1. Use the existing BleDeviceManager for device discovery
        // 2. Respect the existing GSR device filtering
        // 3. Maintain device information that can be accessed later
        // 4. Provide notifications about discovered devices
        
        Log.i(TAG, "Background scanning integrated successfully")
    }
    
    /**
     * Example: Usage in recording session workflow
     */
    fun useInRecordingSession(context: Context) {
        Log.i(TAG, "Using background scanning in recording session")
        
        // Start background scanning before recording session
        BackgroundScanHelper.startBackgroundScanning(context)
        
        // During recording, pause to save battery
        BackgroundScanHelper.pauseBackgroundScanning(context)
        
        // After recording, resume scanning
        BackgroundScanHelper.resumeBackgroundScanning(context)
    }
    
    /**
     * Example: Battery-aware scanning
     */
    fun batteryAwareScanning(context: Context, batteryLevel: Int) {
        Log.i(TAG, "Implementing battery-aware scanning, battery level: $batteryLevel%")
        
        when {
            batteryLevel > 50 -> {
                // High battery - normal scanning
                BackgroundScanHelper.startBackgroundScanning(context)
                Log.i(TAG, "Normal background scanning enabled (battery > 50%)")
            }
            batteryLevel > 20 -> {
                // Medium battery - start but pause frequently
                BackgroundScanHelper.startBackgroundScanning(context)
                // The service will automatically use longer intervals when no devices found
                Log.i(TAG, "Conservative background scanning enabled (battery 20-50%)")
            }
            else -> {
                // Low battery - disable background scanning
                BackgroundScanHelper.stopBackgroundScanning(context)
                Log.i(TAG, "Background scanning disabled due to low battery (< 20%)")
            }
        }
    }
}