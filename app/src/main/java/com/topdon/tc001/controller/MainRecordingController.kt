package com.topdon.tc001.controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.util.SessionDirectoryManager
import com.topdon.tc001.data.SessionMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Main Recording Controller for MVP
 */
class MainRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "MainRecordingController"
        private const val FALLBACK_AVAILABLE_SPACE_GB = 10.0
        private const val RGB_STORAGE_MB_PER_MIN = 50.0
        private const val THERMAL_STORAGE_MB_PER_MIN = 5.0
        private const val SHIMMER_STORAGE_MB_PER_MIN = 1.0
    }

    // Recording state
    private val _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()

    // Sensor management
    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val activeRecorders = ConcurrentHashMap<String, Boolean>()

    // Session management
    private val sessionDirectoryManager = SessionDirectoryManager(context)
    private var sessionMetadata: SessionMetadata? = null

    // Flows for status updates
    private val _recordingStateFlow = MutableStateFlow(RecordingState.IDLE)
    val recordingStateFlow: StateFlow<RecordingState> = _recordingStateFlow.asStateFlow()

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Add a sensor recorder
     */
    fun addSensorRecorder(name: String, recorder: SensorRecorder) {
        sensorRecorders[name] = recorder
        Log.d(TAG, "Added sensor recorder: $name")
    }

    /**
     * Simple start recording - MVP version
     */
    suspend fun startRecording(
        sessionId: String? = null,
        enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer")
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Recording already in progress")
                    return@withContext true
                }

                Log.i(TAG, "Starting simple recording")
                _recordingStateFlow.value = RecordingState.STARTING

                // Simple storage check
                if (getAvailableSpaceGB() < 1.0) {
                    Log.e(TAG, "Insufficient storage space")
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }

                // Create session
                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId)

                // Start sensors
                var sensorsStarted = 0
                for (sensorName in enabledSensors) {
                    val sensor = sensorRecorders[sensorName]
                    if (sensor != null) {
                        try {
                            val sensorDir = File(sessionDir, sensorName.lowercase())
                            sensorDir.mkdirs()
                            
                        sessionMetadata?.let { meta ->
                            val success = sensor.startRecording(sensorDir.absolutePath, meta)
                            if (success) {
                                activeRecorders[sensorName] = true
                                sensorsStarted++
                                Log.i(TAG, "Started sensor: $sensorName")
                            }
                        }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to start sensor $sensorName", e)
                        }
                    }
                }

                if (sensorsStarted > 0) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = RecordingState.RECORDING
                    Log.i(TAG, "Recording started with $sensorsStarted sensors")
                    return@withContext true
                } else {
                    Log.e(TAG, "No sensors started successfully")
                    _recordingStateFlow.value = RecordingState.ERROR
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                _recordingStateFlow.value = RecordingState.ERROR
                return@withContext false
            }
        }
    }

    /**
     * Simple stop recording - MVP version
     */
    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    return@withContext true
                }

                Log.i(TAG, "Stopping recording")
                _recordingStateFlow.value = RecordingState.STOPPING
                _isRecording.set(false)

                // Stop all active sensors
                for ((sensorName, isActive) in activeRecorders) {
                    if (isActive) {
                        try {
                            sensorRecorders[sensorName]?.stopRecording()
                            Log.i(TAG, "Stopped sensor: $sensorName")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error stopping sensor $sensorName", e)
                        }
                    }
                }

                activeRecorders.clear()
                sessionMetadata = null
                _recordingStateFlow.value = RecordingState.IDLE

                Log.i(TAG, "Recording stopped successfully")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                return@withContext false
            }
        }
    }

    /**
     * Get simple recording status
     */
    fun getRecordingStatus(): SimpleRecordingStatus {
        val activeSensors = activeRecorders.count { it.value }
        return SimpleRecordingStatus(
            isRecording = _isRecording.get(),
            activeSensors = activeSensors,
            totalSensors = sensorRecorders.size,
            state = _recordingStateFlow.value
        )
    }

    private fun getAvailableSpaceGB(): Double {
        return try {
            val sessionDir = File(context.filesDir, "sessions")
            sessionDir.freeSpace / (1024.0 * 1024.0 * 1024.0)
        } catch (e: Exception) {
            FALLBACK_AVAILABLE_SPACE_GB // Assume 10GB available on error
        }
    }
}

/**
 * Simple recording status for MVP
 */
data class SimpleRecordingStatus(
    val isRecording: Boolean,
    val activeSensors: Int,
    val totalSensors: Int,
    val state: RecordingState
)

/**
 * Simple recording states
 */
enum class RecordingState {
    IDLE, STARTING, RECORDING, STOPPING, ERROR
}