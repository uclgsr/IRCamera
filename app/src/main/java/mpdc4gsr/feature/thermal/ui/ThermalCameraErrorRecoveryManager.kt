package mpdc4gsr.feature.thermal.ui

import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ThermalCameraErrorRecoveryManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val thermalRecorder: ThermalCameraRecorder
) {
    companion object {
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
                    monitorThermalCameraHealth()
                    delay(DEVICE_CHECK_INTERVAL_MS)
                    delay(10000)
                }
            }
        }
    }

    private suspend fun monitorThermalCameraHealth() {
        val currentTime = System.currentTimeMillis()
        val isDeviceConnected = thermalRecorder.isIRCameraConnected
        val hasUsbPermission = thermalRecorder.hasUsbPermission
        val isInSimulationMode = thermalRecorder.isSimulationMode
        if (isInSimulationMode != isSimulationModeActive) {
            isSimulationModeActive = isInSimulationMode
            if (isInSimulationMode) {
                updateErrorState(ThermalErrorState.SIMULATION_MODE)
                errorEventListener?.onSimulationModeActivated("Device disconnected or unavailable")
                scheduleReconnectionAttempt()
            } else {
                updateErrorState(ThermalErrorState.NORMAL)
                errorEventListener?.onSimulationModeDeactivated()
                resetReconnectionState()
            }
        }
        if (!isDeviceConnected && !isInSimulationMode) {
            handleDeviceDisconnection()
        }
        if (thermalRecorder.isRecording && !isInSimulationMode) {
            val lastFrameReceived = lastFrameTime.get()
            if (lastFrameReceived > 0 && (currentTime - lastFrameReceived) > FRAME_TIMEOUT_MS) {
                    TAG,
                    "Thermal camera frame timeout detected (${currentTime - lastFrameReceived}ms)"
                )
                handleFrameTimeout()
            }
        }
        if (isDeviceConnected && !hasUsbPermission) {
            updateErrorState(ThermalErrorState.PERMISSION_DENIED)
            errorEventListener?.onErrorStateChanged(ThermalErrorState.PERMISSION_DENIED)
        }
    }

    private suspend fun handleDeviceDisconnection() {
        if (currentErrorState == ThermalErrorState.DISCONNECTED) {
            return
        }
        updateErrorState(ThermalErrorState.DISCONNECTED)
        val previousDevice = lastKnownDevice
        errorEventListener?.onThermalCameraDisconnected(previousDevice)
        scheduleReconnectionAttempt()
    }

    private suspend fun handleFrameTimeout() {
        val failureCount = consecutiveFrameFailures.incrementAndGet()
        if (failureCount >= MAX_CONSECUTIVE_FRAME_FAILURES) {
            updateErrorState(ThermalErrorState.COMMUNICATION_ERROR)
            consecutiveFrameFailures.set(0)
            scheduleReconnectionAttempt()
        }
    }

    private suspend fun scheduleReconnectionAttempt() {
        if (isRecoveryActive.get()) {
            return
        }
        val currentAttempts = reconnectionAttempts.get()
        if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {
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
            TAG,
            "Attempting thermal camera reconnection #$attemptNumber after ${backoffDelay}ms delay"
        )
        errorEventListener?.onReconnectionAttempt(attemptNumber, MAX_RECONNECTION_ATTEMPTS)
        delay(backoffDelay)
            withTimeout(CONNECTION_TIMEOUT_MS) {
                val reconnectionSuccess = performThermalCameraReconnection()
                if (reconnectionSuccess) {
                    handleSuccessfulReconnection()
                } else {
                    handleFailedReconnection()
                }
            }
            handleFailedReconnection()
            handleFailedReconnection()
            isRecoveryActive.set(false)
            lastReconnectionAttempt.set(currentTime)
        }
    }

    private suspend fun performThermalCameraReconnection(): Boolean {
        return (
            delay(1000)
            val isAvailable = thermalRecorder.checkThermalCameraAvailability()
            if (!isAvailable) {
                return false
            }
            val reinitSuccess = thermalRecorder.reinitializeThermalCamera()
            if (!reinitSuccess) {
                return false
            }
            if (thermalRecorder.isRecording) {
                val restartSuccess = thermalRecorder.restartThermalRecording()
                if (!restartSuccess) {
                    return false
                }
            }
            delay(2000)
            val isWorking = thermalRecorder.isIRCameraConnected && !thermalRecorder.isSimulationMode
            if (isWorking) {
                return true
            } else {
                return false
            }
            false
        }
    }

    private fun handleSuccessfulReconnection() {
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
        if (currentAttempts >= MAX_RECONNECTION_ATTEMPTS) {
            updateErrorState(ThermalErrorState.RECOVERY_FAILED)
            errorEventListener?.onReconnectionFailed("All reconnection attempts failed")
        } else {
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
            reconnectionAttempts.set(0)
            scheduleReconnectionAttempt()
        }
    }

    fun cleanup() {
        deviceMonitorJob?.cancel()
        reconnectionJob?.cancel()
        errorEventListener = null
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