package mpdc4gsr.controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.util.SessionDirectoryManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class ComprehensiveRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val permissionManager: PermissionManager
) {
    companion object {
        private const val TAG = "ComprehensiveRecordingController"
        private const val FALLBACK_AVAILABLE_SPACE_GB = 10.0
        private const val RGB_STORAGE_MB_PER_MIN = 50.0
        private const val THERMAL_STORAGE_MB_PER_MIN = 5.0
        private const val SHIMMER_STORAGE_MB_PER_MIN = 1.0
        private const val MIN_STORAGE_SPACE_GB = 1.0
        private const val SESSION_TIMEOUT_MS = 30000L
    }


    private val _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()


    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val activeRecorders = ConcurrentHashMap<String, Boolean>()
    private val sensorHealthStatus = ConcurrentHashMap<String, SensorHealthInfo>()
    private val reconnectionAttempts = ConcurrentHashMap<String, Int>()


    private val sessionDirectoryManager = SessionDirectoryManager(context)
    private var sessionMetadata: SessionMetadata? = null
    private var currentSessionId: String? = null
    private val sessionStartTime = AtomicLong(0)


    private val _recordingStateFlow = MutableStateFlow(RecordingState.IDLE)
    val recordingStateFlow: StateFlow<RecordingState> = _recordingStateFlow.asStateFlow()

    private val _sensorStatusFlow = MutableStateFlow(emptyMap<String, SensorStatus>())
    val sensorStatusFlow: StateFlow<Map<String, SensorStatus>> = _sensorStatusFlow.asStateFlow()

    private val _recordingStatsFlow = MutableStateFlow(RecordingStats.empty())
    val recordingStatsFlow: StateFlow<RecordingStats> = _recordingStatsFlow.asStateFlow()

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    private var crashRecoveryMarker: File? = null


    fun addSensorRecorder(name: String, recorder: SensorRecorder) {
        sensorRecorders[name] = recorder
        sensorHealthStatus[name] = SensorHealthInfo(
            name = name,
            isHealthy = true,
            lastHealthCheck = System.currentTimeMillis(),
            consecutiveFailures = 0
        )
        Log.d(TAG, "Added sensor recorder with health monitoring: $name")
        updateSensorStatusFlow()
    }


    suspend fun startRecording(
        sessionId: String? = null,
        enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer"),
        estimatedDurationMinutes: Int = 30
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Recording already in progress")
                    return@withContext true
                }

                Log.i(TAG, "Starting comprehensive recording with validation")
                _recordingStateFlow.value = RecordingState.STARTING


                val validationResult = validateRecordingPrerequisites(enabledSensors, estimatedDurationMinutes)
                if (!validationResult.isValid) {
                    Log.e(TAG, "Prerequisites validation failed: ${validationResult.failureReason}")
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }


                if (!requestRequiredPermissions(enabledSensors)) {
                    Log.e(TAG, "Failed to obtain required permissions")
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }


                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)

                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId).copy(
                    experimentalConditions = mapOf(
                        "estimatedDurationMinutes" to estimatedDurationMinutes,
                        "enabledSensors" to enabledSensors
                    )
                )

                currentSessionId = finalSessionId
                sessionStartTime.set(System.currentTimeMillis())


                createCrashRecoveryMarker(finalSessionId)


                var sensorsStarted = 0
                val sensorResults = mutableMapOf<String, Boolean>()

                for (sensorName in enabledSensors) {
                    val sensor = sensorRecorders[sensorName]
                    if (sensor != null) {
                        try {
                            val sensorDir = File(sessionDir.rootDir, sensorName.lowercase())
                            sensorDir.mkdirs()

                            sessionMetadata?.let { meta ->
                                val success = sensor.startRecording(sensorDir.absolutePath, meta)
                                sensorResults[sensorName] = success

                                if (success) {
                                    activeRecorders[sensorName] = true
                                    sensorsStarted++
                                    updateSensorHealth(sensorName, true)
                                    Log.i(TAG, "✅ Started sensor: $sensorName")
                                } else {
                                    updateSensorHealth(sensorName, false)
                                    Log.w(TAG, "❌ Failed to start sensor: $sensorName")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception starting sensor $sensorName", e)
                            updateSensorHealth(sensorName, false)
                            sensorResults[sensorName] = false
                        }
                    } else {
                        Log.w(TAG, "⚠️ Sensor recorder not found: $sensorName")
                        sensorResults[sensorName] = false
                    }
                }


                if (sensorsStarted > 0) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = RecordingState.RECORDING


                    startHealthMonitoring()
                    startStatisticsUpdates()

                    Log.i(TAG, "🚀 Recording started successfully with $sensorsStarted/$enabledSensors.size sensors")
                    Log.i(
                        TAG,
                        "📊 Sensor status: ${sensorResults.entries.joinToString { "${it.key}=${if (it.value) "✅" else "❌"}" }}"
                    )

                    return@withContext true
                } else {
                    Log.e(TAG, "❌ No sensors started successfully - aborting recording")
                    cleanupFailedRecording()
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Critical error starting recording", e)
                cleanupFailedRecording()
                _recordingStateFlow.value = RecordingState.ERROR
                return@withContext false
            }
        }
    }


    private suspend fun validateRecordingPrerequisites(
        enabledSensors: List<String>,
        estimatedDurationMinutes: Int
    ): ValidationResult {
        try {

            val availableSpaceGB = getAvailableSpaceGB()
            val estimatedSpaceGB = estimateSessionSize(enabledSensors, estimatedDurationMinutes) / 1024.0

            if (availableSpaceGB < estimatedSpaceGB + MIN_STORAGE_SPACE_GB) {
                return ValidationResult(
                    false,
                    "Insufficient storage: ${String.format("%.1f", availableSpaceGB)}GB available, " +
                            "${String.format("%.1f", estimatedSpaceGB + MIN_STORAGE_SPACE_GB)}GB required"
                )
            }


            val unavailableSensors = enabledSensors.filter { sensorRecorders[it] == null }
            if (unavailableSensors.isNotEmpty()) {
                return ValidationResult(
                    false,
                    "Sensors not available: ${unavailableSensors.joinToString()}"
                )
            }


            val unhealthySensors = enabledSensors.filter {
                sensorHealthStatus[it]?.isHealthy == false
            }
            if (unhealthySensors.isNotEmpty()) {
                Log.w(TAG, "⚠️ Sensors with health issues: ${unhealthySensors.joinToString()}")

            }

            return ValidationResult(true, "All prerequisites validated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error during prerequisite validation", e)
            return ValidationResult(false, "Validation error: ${e.message}")
        }
    }


    private suspend fun requestRequiredPermissions(enabledSensors: List<String>): Boolean {
        return try {
            var allPermissionsGranted = true


            if (enabledSensors.contains("RGB")) {
                if (!permissionManager.requestCameraPermissions()) {
                    Log.w(TAG, "Camera permissions not granted")
                    allPermissionsGranted = false
                }
            }


            if (enabledSensors.contains("Shimmer")) {
                if (!permissionManager.requestBluetoothPermissions()) {
                    Log.w(TAG, "Bluetooth permissions not granted")
                    allPermissionsGranted = false
                }
            }


            if (!permissionManager.requestStoragePermissions()) {
                Log.w(TAG, "Storage permissions not granted")

            }

            allPermissionsGranted
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permissions", e)
            false
        }
    }


    private fun estimateSessionSize(enabledSensors: List<String>, durationMinutes: Int): Double {
        var estimatedMB = 0.0

        for (sensor in enabledSensors) {
            when (sensor.uppercase()) {
                "RGB" -> estimatedMB += durationMinutes * RGB_STORAGE_MB_PER_MIN
                "THERMAL" -> estimatedMB += durationMinutes * THERMAL_STORAGE_MB_PER_MIN
                "SHIMMER" -> estimatedMB += durationMinutes * SHIMMER_STORAGE_MB_PER_MIN
            }
        }

        return estimatedMB
    }


    private fun createCrashRecoveryMarker(sessionId: String) {
        try {
            crashRecoveryMarker = File(context.filesDir, "crash_recovery_$sessionId.marker")
            crashRecoveryMarker?.writeText("RECORDING_ACTIVE:$sessionId:${System.currentTimeMillis()}")
            Log.d(TAG, "Created crash recovery marker for session: $sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create crash recovery marker", e)
        }
    }


    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    return@withContext true
                }

                Log.i(TAG, "Stopping comprehensive recording")
                _recordingStateFlow.value = RecordingState.STOPPING
                _isRecording.set(false)


                val stopResults = mutableMapOf<String, Boolean>()
                for ((sensorName, isActive) in activeRecorders) {
                    if (isActive) {
                        try {
                            sensorRecorders[sensorName]?.stopRecording()
                            stopResults[sensorName] = true
                            Log.i(TAG, "✅ Stopped sensor: $sensorName")
                        } catch (e: Exception) {
                            Log.w(TAG, "❌ Error stopping sensor $sensorName", e)
                            stopResults[sensorName] = false
                        }
                    }
                }


                activeRecorders.clear()
                reconnectionAttempts.clear()


                crashRecoveryMarker?.let {
                    if (it.exists()) {
                        it.delete()
                        Log.d(TAG, "Removed crash recovery marker")
                    }
                }


                sessionMetadata?.let { metadata ->
                    // Note: SessionMetadata is immutable, so we can't update end timestamp here
                    // This would need to be handled when creating the final session metadata
                }

                sessionMetadata = null
                currentSessionId = null
                _recordingStateFlow.value = RecordingState.IDLE

                Log.i(TAG, "🏁 Recording stopped successfully")
                Log.i(
                    TAG,
                    "📊 Stop results: ${stopResults.entries.joinToString { "${it.key}=${if (it.value) "✅" else "❌"}" }}"
                )

                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                return@withContext false
            }
        }
    }


    private fun startHealthMonitoring() {
        recordingScope.launch {
            while (_isRecording.get()) {
                try {
                    for ((sensorName, isActive) in activeRecorders) {
                        if (isActive) {
                            checkSensorHealth(sensorName)
                        }
                    }
                    updateSensorStatusFlow()
                    delay(5000)
                } catch (e: Exception) {
                    Log.w(TAG, "Error during health monitoring", e)
                    delay(10000)
                }
            }
        }
    }


    private fun startStatisticsUpdates() {
        recordingScope.launch {
            while (_isRecording.get()) {
                try {
                    updateRecordingStats()
                    delay(2000)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating statistics", e)
                    delay(5000)
                }
            }
        }
    }

    private fun updateSensorHealth(sensorName: String, isHealthy: Boolean) {
        val currentHealth = sensorHealthStatus[sensorName] ?: return
        val updatedHealth = currentHealth.copy(
            isHealthy = isHealthy,
            lastHealthCheck = System.currentTimeMillis(),
            consecutiveFailures = if (isHealthy) 0 else currentHealth.consecutiveFailures + 1
        )
        sensorHealthStatus[sensorName] = updatedHealth
    }

    private fun updateSensorStatusFlow() {
        val statusMap = sensorHealthStatus.mapValues { (name, health) ->
            SensorStatus(
                name = name,
                isActive = activeRecorders[name] ?: false,
                isHealthy = health.isHealthy,
                lastUpdate = health.lastHealthCheck
            )
        }
        _sensorStatusFlow.value = statusMap
    }

    private fun updateRecordingStats() {
        val currentTime = System.currentTimeMillis()
        val duration = if (sessionStartTime.get() > 0) currentTime - sessionStartTime.get() else 0

        val stats = RecordingStats(
            isRecording = _isRecording.get(),
            sessionId = currentSessionId,
            durationMs = duration,
            activeSensors = activeRecorders.count { it.value },
            totalSensors = sensorRecorders.size,
            healthySensors = sensorHealthStatus.count { it.value.isHealthy },
            recordingState = _recordingStateFlow.value
        )

        _recordingStatsFlow.value = stats
    }

    private fun checkSensorHealth(sensorName: String) {

        // This is a placeholder - in production you'd check sensor-specific metrics
        val sensor = sensorRecorders[sensorName]
        val isHealthy = sensor?.isRecording == true
        updateSensorHealth(sensorName, isHealthy)
    }

    private fun cleanupFailedRecording() {
        activeRecorders.clear()
        sessionMetadata = null
        currentSessionId = null
        crashRecoveryMarker?.delete()
    }

    private fun getAvailableSpaceGB(): Double {
        return try {
            val sessionDir = File(context.filesDir, "sessions")
            sessionDir.freeSpace / (1024.0 * 1024.0 * 1024.0)
        } catch (e: Exception) {
            FALLBACK_AVAILABLE_SPACE_GB
        }
    }
}


data class ValidationResult(val isValid: Boolean, val failureReason: String)

data class SensorHealthInfo(
    val name: String,
    val isHealthy: Boolean,
    val lastHealthCheck: Long,
    val consecutiveFailures: Int
)

data class SensorStatus(
    val name: String,
    val isActive: Boolean,
    val isHealthy: Boolean,
    val lastUpdate: Long
)

data class RecordingStats(
    val isRecording: Boolean,
    val sessionId: String?,
    val durationMs: Long,
    val activeSensors: Int,
    val totalSensors: Int,
    val healthySensors: Int,
    val recordingState: RecordingState
) {
    companion object {
        fun empty() = RecordingStats(
            false, null, 0, 0, 0, 0, RecordingState.IDLE
        )
    }
}

enum class RecordingState {
    IDLE, STARTING, RECORDING, STOPPING, ERROR
}