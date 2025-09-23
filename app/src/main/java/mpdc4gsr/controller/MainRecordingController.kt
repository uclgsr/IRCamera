package mpdc4gsr.controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.kotlinx.coroutines.CoroutineScope
import com.kotlinx.coroutines.Dispatchers
import com.kotlinx.coroutines.SupervisorJob
import com.kotlinx.coroutines.flow.MutableStateFlow
import com.kotlinx.coroutines.flow.StateFlow
import com.kotlinx.coroutines.flow.asStateFlow
import com.kotlinx.coroutines.withContext
import com.mpdc4gsr.data.SessionMetadata
import com.mpdc4gsr.sensors.SensorRecorder
import com.mpdc4gsr.util.SessionDirectoryManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean


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


    private val _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()


    private val sensorRecorders = ConcurrentHashMap<String, SensorRecorder>()
    private val activeRecorders = ConcurrentHashMap<String, Boolean>()


    private val sessionDirectoryManager = SessionDirectoryManager(context)
    private var sessionMetadata: SessionMetadata? = null


    private val _recordingStateFlow = MutableStateFlow(MainRecordingState.IDLE)
    val recordingStateFlow: StateFlow<MainRecordingState> = _recordingStateFlow.asStateFlow()

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun addSensorRecorder(name: String, recorder: SensorRecorder) {
        sensorRecorders[name] = recorder
        Log.d(TAG, "Added sensor recorder: $name")
    }


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
                _recordingStateFlow.value = MainRecordingState.STARTING


                if (getAvailableSpaceGB() < 1.0) {
                    Log.e(TAG, "Insufficient storage space")
                    _recordingStateFlow.value = MainRecordingState.ERROR
                    return@withContext false
                }


                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId)


                var sensorsStarted = 0
                for (sensorName in enabledSensors) {
                    val sensor = sensorRecorders[sensorName]
                    if (sensor != null) {
                        try {
                            val sensorDir = File(sessionDir.rootDir, sensorName.lowercase())
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
                    _recordingStateFlow.value = MainRecordingState.RECORDING
                    Log.i(TAG, "Recording started with $sensorsStarted sensors")
                    return@withContext true
                } else {
                    Log.e(TAG, "No sensors started successfully")
                    _recordingStateFlow.value = MainRecordingState.ERROR
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                _recordingStateFlow.value = MainRecordingState.ERROR
                return@withContext false
            }
        }
    }


    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    return@withContext true
                }

                Log.i(TAG, "Stopping recording")
                _recordingStateFlow.value = MainRecordingState.STOPPING
                _isRecording.set(false)


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
                _recordingStateFlow.value = MainRecordingState.IDLE

                Log.i(TAG, "Recording stopped successfully")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                return@withContext false
            }
        }
    }


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
            FALLBACK_AVAILABLE_SPACE_GB
        }
    }
}


data class SimpleRecordingStatus(
    val isRecording: Boolean,
    val activeSensors: Int,
    val totalSensors: Int,
    val state: MainRecordingState
)


enum class MainRecordingState {
    IDLE, STARTING, RECORDING, STOPPING, ERROR
}