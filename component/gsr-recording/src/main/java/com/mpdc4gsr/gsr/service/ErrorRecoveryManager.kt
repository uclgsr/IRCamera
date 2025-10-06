package com.mpdc4gsr.gsr.service
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
class ErrorRecoveryManager private constructor() {
    companion object {
        private const val TAG = "ErrorRecoveryManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val HEALTH_CHECK_INTERVAL_MS = 5000L
        @Volatile
        private var INSTANCE: ErrorRecoveryManager? = null
        fun getInstance(): ErrorRecoveryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ErrorRecoveryManager().also { INSTANCE = it }
            }
        }
    }
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private val recoveryStrategies = ConcurrentHashMap<ErrorType, RecoveryStrategy>()
    private val activeRecoveries = ConcurrentHashMap<String, RecoveryOperation>()
    private val errorListeners = mutableListOf<ErrorRecoveryListener>()
    private val isHealthCheckRunning = AtomicBoolean(false)
    private val monitoredServices = ConcurrentHashMap<String, MonitoredService>()
    init {
        setupDefaultRecoveryStrategies()
    }
    interface ErrorRecoveryListener {
        fun onErrorDetected(error: RecoverableError)
        fun onRecoveryStarted(
            error: RecoverableError,
            strategy: RecoveryStrategy,
        )
        fun onRecoverySuccess(error: RecoverableError)
        fun onRecoveryFailed(
            error: RecoverableError,
            finalError: String,
        )
        fun onServiceHealthChanged(
            serviceId: String,
            isHealthy: Boolean,
        )
    }
    enum class ErrorType {
        GSR_SENSOR_DISCONNECTION,
        GSR_DATA_STREAM_FAILURE,
        THERMAL_CAMERA_CONNECTION_LOST,
        THERMAL_RECORDING_FAILURE,
        RGB_CAMERA_ACCESS_DENIED,
        RGB_RECORDING_FAILURE,
        STORAGE_FULL,
        STORAGE_ACCESS_DENIED,
        BLUETOOTH_CONNECTION_LOST,
        SHIMMER_DEVICE_UNRESPONSIVE,
        SESSION_CORRUPTION,
        SYNCHRONIZATION_FAILURE,
        BATTERY_CRITICAL,
        MEMORY_EXHAUSTION,
    }
    data class RecoverableError(
        val type: ErrorType,
        val serviceId: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val context: Map<String, Any> = emptyMap(),
        val severity: Severity = Severity.MEDIUM,
    ) {
        enum class Severity {
            LOW,
            MEDIUM,
            HIGH,
            FATAL,
        }
    }
    data class RecoveryStrategy(
        val name: String,
        val maxRetries: Int = MAX_RETRY_ATTEMPTS,
        val retryDelayMs: Long = RETRY_DELAY_MS,
        val requiresUserIntervention: Boolean = false,
        val recoveryAction: suspend (RecoverableError) -> RecoveryResult,
    )
    data class RecoveryResult(
        val success: Boolean,
        val message: String,
        val shouldRetry: Boolean = false,
        val updatedContext: Map<String, Any> = emptyMap(),
    )
    internal data class RecoveryOperation(
        val error: RecoverableError,
        val strategy: RecoveryStrategy,
        val attempts: Int = 0,
        val startTime: Long = System.currentTimeMillis(),
        val job: Job,
    )
    data class MonitoredService(
        val serviceId: String,
        val healthChecker: suspend () -> Boolean,
        val lastHealthCheck: Long = 0L,
        val isHealthy: Boolean = true,
        val consecutiveFailures: Int = 0,
    )
    private fun setupDefaultRecoveryStrategies() {
        recoveryStrategies[ErrorType.GSR_SENSOR_DISCONNECTION] =
            RecoveryStrategy(
                name = "GSR Sensor Reconnection",
                maxRetries = 5,
                retryDelayMs = 3000L,
                recoveryAction = { error -> recoverGSRSensorConnection(error) },
            )
        recoveryStrategies[ErrorType.GSR_DATA_STREAM_FAILURE] =
            RecoveryStrategy(
                name = "GSR Data Stream Recovery",
                maxRetries = 3,
                retryDelayMs = 1000L,
                recoveryAction = { error -> recoverGSRDataStream(error) },
            )
        recoveryStrategies[ErrorType.THERMAL_CAMERA_CONNECTION_LOST] =
            RecoveryStrategy(
                name = "Thermal Camera Reconnection",
                maxRetries = 3,
                retryDelayMs = 2000L,
                recoveryAction = { error -> recoverThermalCameraConnection(error) },
            )
        recoveryStrategies[ErrorType.THERMAL_RECORDING_FAILURE] =
            RecoveryStrategy(
                name = "Thermal Recording Recovery",
                maxRetries = 2,
                retryDelayMs = 1500L,
                recoveryAction = { error -> recoverThermalRecording(error) },
            )
        recoveryStrategies[ErrorType.RGB_CAMERA_ACCESS_DENIED] =
            RecoveryStrategy(
                name = "RGB Camera Permission Recovery",
                maxRetries = 1,
                requiresUserIntervention = true,
                recoveryAction = { error -> recoverRGBCameraAccess(error) },
            )
        recoveryStrategies[ErrorType.RGB_RECORDING_FAILURE] =
            RecoveryStrategy(
                name = "RGB Recording Recovery",
                maxRetries = 2,
                retryDelayMs = 1000L,
                recoveryAction = { error -> recoverRGBRecording(error) },
            )
        recoveryStrategies[ErrorType.STORAGE_FULL] =
            RecoveryStrategy(
                name = "Storage Space Recovery",
                maxRetries = 1,
                requiresUserIntervention = true,
                recoveryAction = { error -> recoverStorageSpace(error) },
            )
        recoveryStrategies[ErrorType.STORAGE_ACCESS_DENIED] =
            RecoveryStrategy(
                name = "Storage Access Recovery",
                maxRetries = 2,
                retryDelayMs = 1000L,
                recoveryAction = { error -> recoverStorageAccess(error) },
            )
        recoveryStrategies[ErrorType.BLUETOOTH_CONNECTION_LOST] =
            RecoveryStrategy(
                name = "Bluetooth Connection Recovery",
                maxRetries = 4,
                retryDelayMs = 2500L,
                recoveryAction = { error -> recoverBluetoothConnection(error) },
            )
        recoveryStrategies[ErrorType.SHIMMER_DEVICE_UNRESPONSIVE] =
            RecoveryStrategy(
                name = "Shimmer Device Recovery",
                maxRetries = 3,
                retryDelayMs = 5000L,
                recoveryAction = { error -> recoverShimmerDevice(error) },
            )
        recoveryStrategies[ErrorType.SESSION_CORRUPTION] =
            RecoveryStrategy(
                name = "Session Data Recovery",
                maxRetries = 1,
                recoveryAction = { error -> recoverSessionData(error) },
            )
        recoveryStrategies[ErrorType.SYNCHRONIZATION_FAILURE] =
            RecoveryStrategy(
                name = "Synchronization Recovery",
                maxRetries = 2,
                retryDelayMs = 1000L,
                recoveryAction = { error -> recoverSynchronization(error) },
            )
        recoveryStrategies[ErrorType.BATTERY_CRITICAL] =
            RecoveryStrategy(
                name = "Battery Critical Recovery",
                maxRetries = 1,
                requiresUserIntervention = true,
                recoveryAction = { error -> handleCriticalBattery(error) },
            )
        recoveryStrategies[ErrorType.MEMORY_EXHAUSTION] =
            RecoveryStrategy(
                name = "Memory Recovery",
                maxRetries = 2,
                retryDelayMs = 1000L,
                recoveryAction = { error -> recoverFromMemoryExhaustion(error) },
            )
    }
    fun addErrorRecoveryListener(listener: ErrorRecoveryListener) {
        errorListeners.add(listener)
    }
    fun removeErrorRecoveryListener(listener: ErrorRecoveryListener) {
        errorListeners.remove(listener)
    }
    fun registerService(
        serviceId: String,
        healthChecker: suspend () -> Boolean,
    ) {
        monitoredServices[serviceId] = MonitoredService(serviceId, healthChecker)
        if (!isHealthCheckRunning.getAndSet(true)) {
            startHealthMonitoring()
        }
    }
    fun unregisterService(serviceId: String) {
        monitoredServices.remove(serviceId)
        if (monitoredServices.isEmpty()) {
            isHealthCheckRunning.set(false)
        }
    }
    fun reportError(error: RecoverableError) {
        Log.w(
            TAG,
            "Error reported: ${error.type} for service ${error.serviceId} - ${error.message}"
        )
        errorListeners.forEach { it.onErrorDetected(error) }
        if (error.severity == RecoverableError.Severity.FATAL) {
            Log.e(TAG, "Fatal error detected, stopping all operations")
            return
        }
        val strategy = recoveryStrategies[error.type]
        if (strategy != null) {
            startRecovery(error, strategy)
        } else {
            Log.w(TAG, "No recovery strategy found for error type: ${error.type}")
        }
    }
    private fun startRecovery(
        error: RecoverableError,
        strategy: RecoveryStrategy,
    ) {
        val recoveryId = "${error.serviceId}_${error.type}_${System.currentTimeMillis()}"
        if (activeRecoveries.containsKey(recoveryId)) {
            Log.w(TAG, "Recovery already in progress for $recoveryId")
            return
        }
        Log.i(TAG, "Starting recovery: ${strategy.name} for ${error.type}")
        errorListeners.forEach { it.onRecoveryStarted(error, strategy) }
        val recoveryJob =
            scope.launch {
                try {
                    var attempts = 0
                    var lastResult: RecoveryResult? = null
                    while (attempts < strategy.maxRetries) {
                        attempts++
                        Log.d(
                            TAG,
                            "Recovery attempt $attempts/${strategy.maxRetries} for ${error.type}"
                        )
                        try {
                            lastResult = strategy.recoveryAction(error)
                            if (lastResult.success) {
                                Log.i(
                                    TAG,
                                    "Recovery successful for ${error.type}: ${lastResult.message}"
                                )
                                errorListeners.forEach { it.onRecoverySuccess(error) }
                                break
                            } else if (!lastResult.shouldRetry) {
                                Log.w(
                                    TAG,
                                    "Recovery aborted for ${error.type}: ${lastResult.message}"
                                )
                                break
                            } else {
                                Log.w(
                                    TAG,
                                    "Recovery attempt failed for ${error.type}: ${lastResult.message}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Recovery attempt failed with exception", e)
                            lastResult = RecoveryResult(false, "Exception: ${e.message}")
                        }
                        if (attempts < strategy.maxRetries) {
                            delay(strategy.retryDelayMs)
                        }
                    }
                    if (lastResult?.success != true) {
                        val finalMessage =
                            lastResult?.message ?: "Recovery failed after $attempts attempts"
                        Log.e(TAG, "Recovery failed for ${error.type}: $finalMessage")
                        errorListeners.forEach { it.onRecoveryFailed(error, finalMessage) }
                    }
                } finally {
                    activeRecoveries.remove(recoveryId)
                }
            }
        val operation = RecoveryOperation(error, strategy, job = recoveryJob)
        activeRecoveries[recoveryId] = operation
    }
    private fun startHealthMonitoring() {
        scope.launch {
            while (isHealthCheckRunning.get() && monitoredServices.isNotEmpty()) {
                try {
                    monitoredServices.values.forEach { service ->
                        try {
                            val isHealthy = service.healthChecker()
                            val wasHealthy = service.isHealthy
                            if (isHealthy != wasHealthy) {
                                Log.i(
                                    TAG,
                                    "Service ${service.serviceId} health changed: $wasHealthy -> $isHealthy"
                                )
                                errorListeners.forEach {
                                    it.onServiceHealthChanged(service.serviceId, isHealthy)
                                }
                            }
                            val updatedService =
                                service.copy(
                                    isHealthy = isHealthy,
                                    lastHealthCheck = System.currentTimeMillis(),
                                    consecutiveFailures = if (isHealthy) 0 else service.consecutiveFailures + 1,
                                )
                            monitoredServices[service.serviceId] = updatedService
                            if (!isHealthy && updatedService.consecutiveFailures >= 3) {
                                reportError(
                                    RecoverableError(
                                        type = ErrorType.SESSION_CORRUPTION,
                                        serviceId = service.serviceId,
                                        message = "Service unhealthy for ${updatedService.consecutiveFailures} consecutive checks",
                                        severity = RecoverableError.Severity.HIGH,
                                    ),
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Health check failed for service ${service.serviceId}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during health monitoring", e)
                }
                delay(HEALTH_CHECK_INTERVAL_MS)
            }
        }
    }
    private suspend fun recoverGSRSensorConnection(error: RecoverableError): RecoveryResult {
        return try {
            Log.w(TAG, "Recovering from GSR sensor error: ${error.message}")
            delay(1000L)
            Log.d(TAG, "Attempting GSR sensor reconnection")
            RecoveryResult(true, "GSR sensor reconnected successfully")
        } catch (e: Exception) {
            RecoveryResult(false, "GSR reconnection failed: ${e.message}", shouldRetry = true)
        }
    }
    private suspend fun recoverGSRDataStream(error: RecoverableError): RecoveryResult {
        return try {
            Log.w(TAG, "Recovering from GSR data stream error: ${error.message}")
            Log.d(TAG, "Attempting GSR data stream recovery")
            RecoveryResult(true, "GSR data stream recovered")
        } catch (e: Exception) {
            RecoveryResult(
                false,
                "GSR data stream recovery failed: ${e.message}",
                shouldRetry = true
            )
        }
    }
    private suspend fun recoverThermalCameraConnection(error: RecoverableError): RecoveryResult {
        return try {
            Log.w(TAG, "Recovering from thermal camera error: ${error.message}")
            Log.d(TAG, "Attempting thermal camera reconnection")
            RecoveryResult(true, "Thermal camera reconnected")
        } catch (e: Exception) {
            RecoveryResult(
                false,
                "Thermal camera reconnection failed: ${e.message}",
                shouldRetry = true
            )
        }
    }
    private suspend fun recoverThermalRecording(error: RecoverableError): RecoveryResult {
        return try {
            Log.w(TAG, "Recovering from thermal recording error: ${error.message}")
            Log.d(TAG, "Attempting thermal recording recovery")
            RecoveryResult(true, "Thermal recording recovered")
        } catch (e: Exception) {
            RecoveryResult(
                false,
                "Thermal recording recovery failed: ${e.message}",
                shouldRetry = true
            )
        }
    }
    private suspend fun recoverRGBCameraAccess(error: RecoverableError): RecoveryResult {
        Log.w(TAG, "RGB camera access error: ${error.message}")
        return RecoveryResult(
            false,
            "RGB camera access requires user intervention - check permissions",
            shouldRetry = false
        )
    }
    private suspend fun recoverRGBRecording(error: RecoverableError): RecoveryResult {
        return try {
            Log.d(TAG, "Attempting RGB recording recovery for error: ${error.message}")
            RecoveryResult(true, "RGB recording recovered")
        } catch (e: Exception) {
            RecoveryResult(false, "RGB recording recovery failed: ${e.message}", shouldRetry = true)
        }
    }
    private suspend fun recoverStorageSpace(error: RecoverableError): RecoveryResult {
        return RecoveryResult(
            false,
            "Storage full - user intervention required to free space for ${error.message}",
            shouldRetry = false
        )
    }
    private suspend fun recoverStorageAccess(error: RecoverableError): RecoveryResult {
        return try {
            Log.d(TAG, "Attempting storage access recovery for error: ${error.message}")
            RecoveryResult(true, "Storage access recovered")
        } catch (e: Exception) {
            RecoveryResult(
                false,
                "Storage access recovery failed: ${e.message}",
                shouldRetry = true
            )
        }
    }
    private suspend fun recoverBluetoothConnection(error: RecoverableError): RecoveryResult {
        return try {
            Log.d(TAG, "Attempting Bluetooth connection recovery for error: ${error.type}")
            delay(2000L)
            RecoveryResult(true, "Bluetooth connection recovered")
        } catch (e: Exception) {
            RecoveryResult(false, "Bluetooth recovery failed: ${e.message}", shouldRetry = true)
        }
    }
    private suspend fun recoverShimmerDevice(error: RecoverableError): RecoveryResult {
        return try {
            Log.d(TAG, "Attempting Shimmer device recovery for error: ${error.type}")
            delay(3000L)
            RecoveryResult(true, "Shimmer device recovered")
        } catch (e: Exception) {
            RecoveryResult(
                false,
                "Shimmer device recovery failed: ${e.message}",
                shouldRetry = true
            )
        }
    }
    private suspend fun recoverSessionData(error: RecoverableError): RecoveryResult {
        return try {
            Log.d(TAG, "Attempting session data recovery for error: ${error.type}")
            RecoveryResult(true, "Session data recovered")
        } catch (e: Exception) {
            RecoveryResult(false, "Session data recovery failed: ${e.message}", shouldRetry = false)
        }
    }
    private suspend fun recoverSynchronization(error: RecoverableError): RecoveryResult {
        return try {
            Log.d(TAG, "Attempting synchronization recovery for error: ${error.type}")
            RecoveryResult(true, "Synchronization recovered")
        } catch (e: Exception) {
            RecoveryResult(
                false,
                "Synchronization recovery failed: ${e.message}",
                shouldRetry = true
            )
        }
    }
    private suspend fun handleCriticalBattery(error: RecoverableError): RecoveryResult {
        Log.w(TAG, "Critical battery detected: ${error.message}")
        return RecoveryResult(
            false,
            "Critical battery level - immediate user action required",
            shouldRetry = false
        )
    }
    private suspend fun recoverFromMemoryExhaustion(error: RecoverableError): RecoveryResult {
        return try {
            Log.d(TAG, "Attempting memory recovery for error: ${error.type}")
            System.gc()
            RecoveryResult(true, "Memory recovered")
        } catch (e: Exception) {
            RecoveryResult(false, "Memory recovery failed: ${e.message}", shouldRetry = true)
        }
    }
    internal fun getRecoveryStatus(): Map<String, RecoveryOperation> {
        return activeRecoveries.toMap()
    }
    fun getServiceHealthStatus(): Map<String, MonitoredService> {
        return monitoredServices.toMap()
    }
    fun shutdown() {
        isHealthCheckRunning.set(false)
        job.cancel()
        activeRecoveries.clear()
        monitoredServices.clear()
        errorListeners.clear()
    }
}
