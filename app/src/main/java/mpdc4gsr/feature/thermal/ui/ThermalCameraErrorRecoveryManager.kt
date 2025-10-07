package mpdc4gsr.feature.thermal.ui

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import mpdc4gsr.core.utils.AppLogger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ThermalCameraErrorRecoveryManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val thermalRecorder: ThermalCameraRecorder
) {
    companion object {
        private const val TAG = "ThermalErrorRecovery"
        private const val DEVICE_CHECK_INTERVAL_MS = 5000L
        private const val MAX_RECONNECTION_ATTEMPTS = 10
        private const val INITIAL_RECONNECTION_DELAY_MS = 2000L
        private const val MAX_RECONNECTION_DELAY_MS = 30000L
        private const val CONNECTION_TIMEOUT_MS = 15000L
        private const val MAX_CONSECUTIVE_FRAME_FAILURES = 5
        private const val FRAME_TIMEOUT_MS = 5000L
        private const val SIMULATION_MODE_TIMEOUT_MS = 60000L
    }

    private val isRecoveryActive = AtomicBoolean(false)
    private val reconnectionAttempts = AtomicInteger(0)
    private val consecutiveFrameFailures = AtomicInteger(0)
    private val lastFrameTime = AtomicLong(0)
    private val lastReconnectionAttempt = AtomicLong(0)
    private var deviceMonitorJob: Job? = null
    private var reconnectionJob: Job? = null
    private var lastKnownDevice: UsbDevice? = null
    private var currentErrorState: ThermalErrorState = ThermalErrorState.NORMAL
    private var isSimulationModeActive = false
    private var errorEventListener: ThermalErrorEventListener? = null

    init {
        startDeviceMonitoring()
    }

    interface ThermalErrorEventListener {
        fun onThermalCameraDisconnected(device: UsbDevice?)
        fun onThermalCameraReconnected(device: UsbDevice)
        fun onSimulationModeActivated(reason: String)
        fun onSimulationModeDeactivated()
        fun onReconnectionAttempt(attempt: Int, maxAttempts: Int)
        fun onReconnectionFailed(reason: String)
        fun onErrorStateChanged(state: ThermalErrorState)
    }

    fun setErrorEventListener(listener: ThermalErrorEventListener?) {
        errorEventListener = listener
    }

    private fun startDeviceMonitoring() {
        deviceMonitorJob?.cancel()
        deviceMonitorJob = lifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    monitorThermalCameraHealth()
                    delay(DEVICE_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error in device monitoring", e)
                    delay(10000)
                }
            }
        }
        AppLogger.i(TAG, "Thermal camera error recovery monitoring started")
    }

    private suspend fun monitorThermalCameraHealth() {
        val currentTime = System.currentTimeMillis()
        val isDeviceConnected = thermalRecorder.isIRCameraConnected
        val hasUsbPermission = thermalRecorder.hasUsbPermission
        val isInSimulationMode = thermalRecorder.isSimulationMode
        if (isInSimulationMode != isSimulationModeActive) {
            isSimulationModeActive = isInSimulationMode
            if (isInSimulationMode) {
                AppLogger.w(TAG, " Thermal camera entered simulation mode")
                updateErrorState(ThermalErrorState.SIMULATION_MODE)
                errorEventListener?.onSimulationModeActivated("Device disconnected or unavailable")
                scheduleReconnectionAttempt()
            } else {
                AppLogger.i(TAG, " Thermal camera exited simulation mode")
                updateErrorState(ThermalErrorState.NORMAL)
                errorEventListener?.onSimulationModeDeactivated()
                resetReconnectionState()
            }
        }
        if (!isDeviceConnected && !isInSimulationMode) {
            AppLogger.w(TAG, "Thermal camera device disconnection detected")
            handleDeviceDisconnection()
        }
        if (thermalRecorder.isRecording && !isInSimulationMode) {
            val lastFrameReceived = lastFrameTime.get()
            if (lastFrameReceived > 0 && (currentTime - lastFrameReceived) > FRAME_TIMEOUT_MS) {
                Log.w(
                    TAG,
                    "Thermal camera frame timeout detected (${currentTime - lastFrameReceived}ms)"
                )
                handleFrameTimeout()
            }
        }
        if (isDeviceConnected && !hasUsbPermission) {
            AppLogger.w(TAG, "Thermal camera USB permission lost")
            updateErrorState(ThermalErrorState.PERMISSION_DENIED)
            errorEventListener?.onErrorStateChanged(ThermalErrorState.PERMISSION_DENIED)
        }
    }

    private suspend fun handleDeviceDisconnection() {
        if (currentErrorState == ThermalErrorState.DISCONNECTED) {
            return
        }
        AppLogger.w(TAG, "Handling thermal camera disconnection")
        updateErrorState(ThermalErrorState.DISCONNECTED)
        val previousDevice = lastKnownDevice
        errorEventListener?.onThermalCameraDisconnected(previousDevice)
        scheduleReconnectionAttempt()
    }

    private suspend fun handleFrameTimeout() {
        val failureCount = consecutiveFrameFailures.incrementAndGet()
        AppLogger.w(TAG, "Frame timeout detected - consecutive failures: $failureCount")
        if (failureCount >= MAX_CONSECUTIVE_FRAME_FAILURES) {
            AppLogger.e(TAG, "Too many consecutive frame failures - triggering recovery")
            updateErrorState(ThermalErrorState.COMMUNICATION_ERROR)
            consecutiveFrameFailures.set(0)
            scheduleReconnectionAttempt()
        }
    }

    private suspend fun scheduleReconnectionAttempt() {
        if (isRecoveryActive.get()) {
            AppLogger.d(TAG, "Recovery already active, skipping new attempt")
            return
        }
        val currentAttempts = reconnectionAttempts.get()
        if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {
            AppLogger.e(TAG, "Maximum reconnection attempts reached - giving up")
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

    private suspend fun attemptThermalCameraReconnection() {
        val attemptNumber = reconnectionAttempts.incrementAndGet()
        val currentTime = System.currentTimeMillis()
        val backoffDelay = minOf(
            INITIAL_RECONNECTION_DELAY_MS * (1 shl (attemptNumber - 1)),
            MAX_RECONNECTION_DELAY_MS
        )
        Log.i(
            TAG,
            "Attempting thermal camera reconnection #$attemptNumber after ${backoffDelay}ms delay"
        )
        errorEventListener?.onReconnectionAttempt(attemptNumber, MAX_RECONNECTION_ATTEMPTS)
        delay(backoffDelay)
        try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                val reconnectionSuccess = performThermalCameraReconnection()
                if (reconnectionSuccess) {
                    AppLogger.i(TAG, " Thermal camera reconnection successful!")
                    handleSuccessfulReconnection()
                } else {
                    AppLogger.w(TAG, " Thermal camera reconnection failed")
                    handleFailedReconnection()
                }
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.w(TAG, "Thermal camera reconnection timed out")
            handleFailedReconnection()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during thermal camera reconnection", e)
            handleFailedReconnection()
        } finally {
            isRecoveryActive.set(false)
            lastReconnectionAttempt.set(currentTime)
        }
    }

    private suspend fun performThermalCameraReconnection(): Boolean {
        return try {
            AppLogger.d(TAG, "Performing thermal camera reconnection sequence")
            delay(1000)
            val isAvailable = thermalRecorder.checkThermalCameraAvailability()
            if (!isAvailable) {
                AppLogger.w(TAG, "No thermal camera device found during reconnection")
                return false
            }
            val reinitSuccess = thermalRecorder.reinitializeThermalCamera()
            if (!reinitSuccess) {
                AppLogger.w(TAG, "Failed to reinitialize thermal camera")
                return false
            }
            if (thermalRecorder.isRecording) {
                AppLogger.d(TAG, "Restarting thermal recording on reconnected device")
                val restartSuccess = thermalRecorder.restartThermalRecording()
                if (!restartSuccess) {
                    AppLogger.w(TAG, "Failed to restart recording on reconnected device")
                    return false
                }
            }
            delay(2000)
            val isWorking = thermalRecorder.isIRCameraConnected && !thermalRecorder.isSimulationMode
            if (isWorking) {
                AppLogger.i(TAG, "Thermal camera reconnection verified successful")
                return true
            } else {
                AppLogger.w(TAG, "Thermal camera reconnection verification failed")
                return false
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception during thermal camera reconnection", e)
            false
        }
    }

    private fun handleSuccessfulReconnection() {
        AppLogger.i(TAG, "Thermal camera successfully reconnected")
        resetReconnectionState()
        updateErrorState(ThermalErrorState.NORMAL)
        lastKnownDevice?.let { device ->
            errorEventListener?.onThermalCameraReconnected(device)
        }
        consecutiveFrameFailures.set(0)
        lastFrameTime.set(System.currentTimeMillis())
    }

    private fun handleFailedReconnection() {
        val currentAttempts = reconnectionAttempts.get()
        AppLogger.w(TAG, "Thermal camera reconnection attempt $currentAttempts failed")
        if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {
            AppLogger.e(TAG, "All thermal camera reconnection attempts exhausted")
            updateErrorState(ThermalErrorState.RECOVERY_FAILED)
            errorEventListener?.onReconnectionFailed("All reconnection attempts failed")
        } else {
            Log.i(
                TAG,
                "Will retry thermal camera reconnection (${MAX_RECONNECTION_ATTEMPTS - currentAttempts} attempts remaining)"
            )
            updateErrorState(ThermalErrorState.DISCONNECTED)
            lifecycleOwner.lifecycleScope.launch {
                delay(5000)
                if (!isRecoveryActive.get()) {
                    scheduleReconnectionAttempt()
                }
            }
        }
    }

    private fun resetReconnectionState() {
        reconnectionAttempts.set(0)
        consecutiveFrameFailures.set(0)
        isRecoveryActive.set(false)
    }

    private fun updateErrorState(newState: ThermalErrorState) {
        if (currentErrorState != newState) {
            val previousState = currentErrorState
            currentErrorState = newState
            AppLogger.i(TAG, "Thermal error state changed: $previousState -> $newState")
            errorEventListener?.onErrorStateChanged(newState)
        }
    }

    fun onFrameReceived() {
        lastFrameTime.set(System.currentTimeMillis())
        consecutiveFrameFailures.set(0)
    }

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

    fun forceReconnectionAttempt() {
        lifecycleOwner.lifecycleScope.launch {
            AppLogger.i(TAG, "Manual thermal camera reconnection requested")
            reconnectionAttempts.set(0)
            scheduleReconnectionAttempt()
        }
    }

    fun cleanup() {
        deviceMonitorJob?.cancel()
        reconnectionJob?.cancel()
        errorEventListener = null
        AppLogger.i(TAG, "Thermal camera error recovery manager cleaned up")
    }

    enum class ThermalErrorState {
        NORMAL,
        DISCONNECTED,
        PERMISSION_DENIED,
        COMMUNICATION_ERROR,
        SIMULATION_MODE,
        RECOVERY_FAILED
    }

    data class ThermalRecoveryStatus(
        val errorState: ThermalErrorState,
        val isRecoveryActive: Boolean,
        val reconnectionAttempts: Int,
        val maxReconnectionAttempts: Int,
        val consecutiveFrameFailures: Int,
        val lastFrameTime: Long,
        val isSimulationModeActive: Boolean
    )

    data class ThermalErrorStateChangedEvent(
        val previousState: ThermalErrorState,
        val newState: ThermalErrorState
    )
}