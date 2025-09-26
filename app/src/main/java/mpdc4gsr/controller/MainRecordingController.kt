package mpdc4gsr.controller

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.utils.SessionDirectoryManager
import mpdc4gsr.controller.RecordingConstants
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean


class MainRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "MainRecordingController"
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
        sensorRecorders[name] = recorder    }


    suspend fun startRecording(
        sessionId: String? = null,
        enabledSensors: List<String> = listOf("RGB", "Thermal", "Shimmer")
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {                    return@withContext true
                }                _recordingStateFlow.value = MainRecordingState.STARTING


                if (getAvailableSpaceGB() < 1.0) {                    _recordingStateFlow.value = MainRecordingState.ERROR
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
                                    sensorsStarted++                                }
                            }
                        } catch (e: Exception) {                        }
                    }
                }

                if (sensorsStarted > 0) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = MainRecordingState.RECORDING                    return@withContext true
                } else {                    _recordingStateFlow.value = MainRecordingState.ERROR
                    return@withContext false
                }

            } catch (e: Exception) {                _recordingStateFlow.value = MainRecordingState.ERROR
                return@withContext false
            }
        }
    }


    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isRecording.get()) {
                    return@withContext true
                }                _recordingStateFlow.value = MainRecordingState.STOPPING
                _isRecording.set(false)


                for ((sensorName, isActive) in activeRecorders) {
                    if (isActive) {
                        try {
                            sensorRecorders[sensorName]?.stopRecording()                        } catch (e: Exception) {                        }
                    }
                }

                activeRecorders.clear()
                sessionMetadata = null
                _recordingStateFlow.value = MainRecordingState.IDLE                return@withContext true

            } catch (e: Exception) {                return@withContext false
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
            RecordingConstants.FALLBACK_AVAILABLE_SPACE_GB
        }
    }
}