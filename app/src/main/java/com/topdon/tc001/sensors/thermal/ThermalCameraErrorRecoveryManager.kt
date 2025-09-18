package com.topdon.tc001.sensors.thermal

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced Thermal Camera Error Recovery Manager
 * Implements TODO requirement: "Refine error recovery for the thermal camera" and
 * "Ensure the UI clearly notifies the user of this fallback and attempt automatic reconnection"
 */
class ThermalCameraErrorRecoveryManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val thermalRecorder: ThermalCameraRecorder
) {
    companion object {
        private const val TAG = "ThermalErrorRecovery"
        
        // Recovery timing constants
        private const val DEVICE_CHECK_INTERVAL_MS = 5000L // Check every 5 seconds
        private const val MAX_RECONNECTION_ATTEMPTS = 10
        private const val INITIAL_RECONNECTION_DELAY_MS = 2000L // 2 seconds
        private const val MAX_RECONNECTION_DELAY_MS = 30000L // 30 seconds
        private const val CONNECTION_TIMEOUT_MS = 15000L // 15 seconds
        
        // Error detection thresholds
        private const val MAX_CONSECUTIVE_FRAME_FAILURES = 5
        private const val FRAME_TIMEOUT_MS = 5000L // 5 seconds without frames
        private const val SIMULATION_MODE_TIMEOUT_MS = 60000L // 1 minute in simulation before retry
    }

    // Recovery state management
    private val isRecoveryActive = AtomicBoolean(false)
    private val reconnectionAttempts = AtomicInteger(0)
    private val consecutiveFrameFailures = AtomicInteger(0)
    private val lastFrameTime = AtomicLong(0)
    private val lastReconnectionAttempt = AtomicLong(0)
    
    // Recovery jobs
    private var deviceMonitorJob: Job? = null
    private var reconnectionJob: Job? = null
    
    // Device state tracking
    private var lastKnownDevice: UsbDevice? = null
    private var currentErrorState: ThermalErrorState = ThermalErrorState.NORMAL
    private var isSimulationModeActive = false
    
    // Event listener for UI notifications
    private var errorEventListener: ThermalErrorEventListener? = null

    init {
        startDeviceMonitoring()
    }

    /**
     * Interface for thermal error event notifications
     */
    interface ThermalErrorEventListener {
        fun onThermalCameraDisconnected(device: UsbDevice?)
        fun onThermalCameraReconnected(device: UsbDevice)
        fun onSimulationModeActivated(reason: String)
        fun onSimulationModeDeactivated()
        fun onReconnectionAttempt(attempt: Int, maxAttempts: Int)
        fun onReconnectionFailed(reason: String)
        fun onErrorStateChanged(state: ThermalErrorState)
    }

    /**
     * Set event listener for UI notifications
     */
    fun setErrorEventListener(listener: ThermalErrorEventListener?) {
        errorEventListener = listener
    }

    /**
     * Start comprehensive device monitoring for error detection
     */
    private fun startDeviceMonitoring() {
        deviceMonitorJob?.cancel()
        deviceMonitorJob = lifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    monitorThermalCameraHealth()
                    delay(DEVICE_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in device monitoring", e)
                    delay(10000) // Back off on errors
                }
            }
        }
        
        Log.i(TAG, "Thermal camera error recovery monitoring started")
    }

    /**
     * Monitor thermal camera health and detect issues
     */
    private suspend fun monitorThermalCameraHealth() {
        val currentTime = System.currentTimeMillis()
        
        // Check device connection status
        val isDeviceConnected = thermalRecorder.isIRCameraConnected
        val hasUsbPermission = thermalRecorder.hasUsbPermission
        val isInSimulationMode = thermalRecorder.isSimulationMode
        
        // Update simulation mode state
        if (isInSimulationMode != isSimulationModeActive) {
            isSimulationModeActive = isInSimulationMode
            
            if (isInSimulationMode) {
                Log.w(TAG, "⚠️ Thermal camera entered simulation mode")
                updateErrorState(ThermalErrorState.SIMULATION_MODE)
                errorEventListener?.onSimulationModeActivated("Device disconnected or unavailable")
                
                // Schedule reconnection attempt after simulation timeout
                scheduleReconnectionAttempt()
                
            } else {
                Log.i(TAG, "✅ Thermal camera exited simulation mode")
                updateErrorState(ThermalErrorState.NORMAL)
                errorEventListener?.onSimulationModeDeactivated()
                resetReconnectionState()
            }
        }
        
        // Check for device disconnection
        if (!isDeviceConnected && !isInSimulationMode) {
            Log.w(TAG, "Thermal camera device disconnection detected")
            handleDeviceDisconnection()
        }
        
        // Check for frame timeout (no data received)
        if (thermalRecorder.isRecording && !isInSimulationMode) {
            val lastFrameReceived = lastFrameTime.get()
            if (lastFrameReceived > 0 && (currentTime - lastFrameReceived) > FRAME_TIMEOUT_MS) {
                Log.w(TAG, "Thermal camera frame timeout detected (${currentTime - lastFrameReceived}ms)")
                handleFrameTimeout()
            }
        }
        
        // Check for permission issues
        if (isDeviceConnected && !hasUsbPermission) {
            Log.w(TAG, "Thermal camera USB permission lost")
            updateErrorState(ThermalErrorState.PERMISSION_DENIED)
            errorEventListener?.onErrorStateChanged(ThermalErrorState.PERMISSION_DENIED)
        }
    }

    /**
     * Handle device disconnection with automatic recovery attempt
     */
    private suspend fun handleDeviceDisconnection() {
        if (currentErrorState == ThermalErrorState.DISCONNECTED) {
            return // Already handling disconnection
        }
        
        Log.w(TAG, "Handling thermal camera disconnection")
        updateErrorState(ThermalErrorState.DISCONNECTED)
        
        val previousDevice = lastKnownDevice
        errorEventListener?.onThermalCameraDisconnected(previousDevice)
        
        // Attempt automatic reconnection
        scheduleReconnectionAttempt()
    }

    /**
     * Handle frame timeout - likely communication issues
     */
    private suspend fun handleFrameTimeout() {
        val failureCount = consecutiveFrameFailures.incrementAndGet()
        
        Log.w(TAG, "Frame timeout detected - consecutive failures: $failureCount")
        
        if (failureCount >= MAX_CONSECUTIVE_FRAME_FAILURES) {
            Log.e(TAG, "Too many consecutive frame failures - triggering recovery")
            updateErrorState(ThermalErrorState.COMMUNICATION_ERROR)
            
            // Reset failure counter and attempt recovery
            consecutiveFrameFailures.set(0)
            scheduleReconnectionAttempt()
        }
    }

    /**
     * Schedule automatic reconnection attempt
     */
    private suspend fun scheduleReconnectionAttempt() {
        if (isRecoveryActive.get()) {
            Log.d(TAG, "Recovery already active, skipping new attempt")
            return
        }
        
        val currentAttempts = reconnectionAttempts.get()
        if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {
            Log.e(TAG, "Maximum reconnection attempts reached - giving up")
            updateErrorState(ThermalErrorState.RECOVERY_FAILED)
            errorEventListener?.onReconnectionFailed("Maximum attempts exceeded")
            return
        }
        
        isRecoveryActive.set(true)
        
        reconnectionJob?.cancel()
        reconnectionJob = lifecycleOwner.lifecycleScope.launch {
            attemptThermalCameraReconnection()
        }
    }

    /**
     * Attempt thermal camera reconnection with exponential backoff
     */
    private suspend fun attemptThermalCameraReconnection() {
        val attemptNumber = reconnectionAttempts.incrementAndGet()
        val currentTime = System.currentTimeMillis()
        
        // Calculate backoff delay
        val backoffDelay = minOf(
            INITIAL_RECONNECTION_DELAY_MS * (1 shl (attemptNumber - 1)), // Exponential backoff
            MAX_RECONNECTION_DELAY_MS
        )
        
        Log.i(TAG, "Attempting thermal camera reconnection #$attemptNumber after ${backoffDelay}ms delay")
        errorEventListener?.onReconnectionAttempt(attemptNumber, MAX_RECONNECTION_ATTEMPTS)
        
        // Wait for backoff delay
        delay(backoffDelay)
        
        try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                val reconnectionSuccess = performThermalCameraReconnection()
                
                if (reconnectionSuccess) {
                    Log.i(TAG, "✅ Thermal camera reconnection successful!")
                    handleSuccessfulReconnection()
                } else {
                    Log.w(TAG, "❌ Thermal camera reconnection failed")
                    handleFailedReconnection()
                }
            }
            
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Thermal camera reconnection timed out")
            handleFailedReconnection()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during thermal camera reconnection", e)
            handleFailedReconnection()
            
        } finally {
            isRecoveryActive.set(false)
            lastReconnectionAttempt.set(currentTime)
        }
    }

    /**
     * Perform the actual thermal camera reconnection
     */
    private suspend fun performThermalCameraReconnection(): Boolean {
        return try {
            Log.d(TAG, "Performing thermal camera reconnection sequence")
            
            // Step 1: Re-scan for USB devices
            delay(1000) // Brief delay for device enumeration
            
            // Step 2: Check if thermal camera is available
            val isAvailable = thermalRecorder.checkThermalCameraAvailability()
            if (!isAvailable) {
                Log.w(TAG, "No thermal camera device found during reconnection")
                return false
            }
            
            // Step 3: Attempt to reinitialize thermal camera
            val reinitSuccess = thermalRecorder.reinitializeThermalCamera()
            if (!reinitSuccess) {
                Log.w(TAG, "Failed to reinitialize thermal camera")
                return false
            }
            
            // Step 4: If recording was active, restart thermal recording
            if (thermalRecorder.isRecording) {
                Log.d(TAG, "Restarting thermal recording on reconnected device")
                val restartSuccess = thermalRecorder.restartThermalRecording()
                if (!restartSuccess) {
                    Log.w(TAG, "Failed to restart recording on reconnected device")
                    return false
                }
            }
            
            // Step 5: Verify device is working
            delay(2000) // Allow time for stability
            val isWorking = thermalRecorder.isIRCameraConnected && !thermalRecorder.isSimulationMode
            
            if (isWorking) {
                Log.i(TAG, "Thermal camera reconnection verified successful")
                return true
            } else {
                Log.w(TAG, "Thermal camera reconnection verification failed")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during thermal camera reconnection", e)
            false
        }
    }

    /**
     * Handle successful reconnection
     */
    private fun handleSuccessfulReconnection() {
        Log.i(TAG, "Thermal camera successfully reconnected")
        
        // Reset recovery state
        resetReconnectionState()
        updateErrorState(ThermalErrorState.NORMAL)
        
        // Notify UI
        lastKnownDevice?.let { device ->
            errorEventListener?.onThermalCameraReconnected(device)
        }
        
        // Reset frame failure tracking
        consecutiveFrameFailures.set(0)
        lastFrameTime.set(System.currentTimeMillis())
    }

    /**
     * Handle failed reconnection attempt
     */
    private fun handleFailedReconnection() {
        val currentAttempts = reconnectionAttempts.get()
        
        Log.w(TAG, "Thermal camera reconnection attempt $currentAttempts failed")
        
        if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {
            Log.e(TAG, "All thermal camera reconnection attempts exhausted")
            updateErrorState(ThermalErrorState.RECOVERY_FAILED)
            errorEventListener?.onReconnectionFailed("All reconnection attempts failed")
        } else {
            Log.i(TAG, "Will retry thermal camera reconnection (${MAX_RECONNECTION_ATTEMPTS - currentAttempts} attempts remaining)")
            updateErrorState(ThermalErrorState.DISCONNECTED)
            
            // Schedule next attempt
            lifecycleOwner.lifecycleScope.launch {
                delay(5000) // Brief delay before next attempt
                if (!isRecoveryActive.get()) {
                    scheduleReconnectionAttempt()
                }
            }
        }
    }

    /**
     * Reset reconnection state after successful recovery
     */
    private fun resetReconnectionState() {
        reconnectionAttempts.set(0)
        consecutiveFrameFailures.set(0)
        isRecoveryActive.set(false)
    }

    /**
     * Update error state and emit events
     */
    private fun updateErrorState(newState: ThermalErrorState) {
        if (currentErrorState != newState) {
            val previousState = currentErrorState
            currentErrorState = newState
            
            Log.i(TAG, "Thermal error state changed: $previousState -> $newState")
            errorEventListener?.onErrorStateChanged(newState)
            
            // Emit EventBus event for broader app notification
            EventBus.getDefault().post(ThermalErrorStateChangedEvent(previousState, newState))
        }
    }

    /**
     * Notify that a frame was received (resets failure counters)
     */
    fun onFrameReceived() {
        lastFrameTime.set(System.currentTimeMillis())
        consecutiveFrameFailures.set(0)
    }

    /**
     * Get current recovery status
     */
    fun getRecoveryStatus(): ThermalRecoveryStatus {
        return ThermalRecoveryStatus(
            errorState = currentErrorState,
            isRecoveryActive = isRecoveryActive.get(),
            reconnectionAttempts = reconnectionAttempts.get(),
            maxReconnectionAttempts = MAX_RECONNECTION_ATTEMPTS,
            consecutiveFrameFailures = consecutiveFrameFailures.get(),
            lastFrameTime = lastFrameTime.get(),
            isSimulationModeActive = isSimulationModeActive
        )
    }

    /**
     * Force manual reconnection attempt
     */
    fun forceReconnectionAttempt() {
        lifecycleOwner.lifecycleScope.launch {
            Log.i(TAG, "Manual thermal camera reconnection requested")
            reconnectionAttempts.set(0) // Reset attempts for manual retry
            scheduleReconnectionAttempt()
        }
    }

    /**
     * Cleanup recovery manager
     */
    fun cleanup() {
        deviceMonitorJob?.cancel()
        reconnectionJob?.cancel()
        errorEventListener = null
        Log.i(TAG, "Thermal camera error recovery manager cleaned up")
    }

    /**
     * Thermal error states
     */
    enum class ThermalErrorState {
        NORMAL,
        DISCONNECTED,
        PERMISSION_DENIED,
        COMMUNICATION_ERROR,
        SIMULATION_MODE,
        RECOVERY_FAILED
    }

    /**
     * Recovery status data class
     */
    data class ThermalRecoveryStatus(
        val errorState: ThermalErrorState,
        val isRecoveryActive: Boolean,
        val reconnectionAttempts: Int,
        val maxReconnectionAttempts: Int,
        val consecutiveFrameFailures: Int,
        val lastFrameTime: Long,
        val isSimulationModeActive: Boolean
    )

    /**
     * EventBus event for thermal error state changes
     */
    data class ThermalErrorStateChangedEvent(
        val previousState: ThermalErrorState,
        val newState: ThermalErrorState
    )
}