package mpdc4gsr.feature.network.data

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.data.SensorRecorder
import mpdc4gsr.core.data.model.SessionMetadata
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class MainRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val RGB_SENSOR_NAME = "RGB"
        private const val THERMAL_SENSOR_NAME = "Thermal"
        private const val GSR_SENSOR_NAME = "GSR"
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
    private val recordingSettingsRepository =
        mpdc4gsr.feature.settings.data.RecordingSettingsRepository.getInstance(context)

    fun addSensorRecorder(name: String, recorder: SensorRecorder) {
        sensorRecorders[name] = recorder
    }

    suspend fun startRecording(
        sessionId: String? = null,
        enabledSensors: List<String> = listOf(RGB_SENSOR_NAME, THERMAL_SENSOR_NAME, GSR_SENSOR_NAME)
    ): Boolean {
        return withContext(Dispatchers.IO) {
                if (_isRecording.get()) {
                    return@withContext true
                }
                val settings = recordingSettingsRepository.getSettings()
                    TAG,
                    "Starting recording with settings: simultaneousRecording=${settings.simultaneousRecording}, timestampSync=${settings.timestampSync}"
                )
                _recordingStateFlow.value = MainRecordingState.STARTING
                if (getAvailableSpaceGB() < 1.0) {
                    _recordingStateFlow.value = MainRecordingState.ERROR
                    return@withContext false
                }
                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId)
                var sensorsStarted = 0
                val isSimultaneous = settings.simultaneousRecording
                for (sensorName in enabledSensors) {
                    val sensor = sensorRecorders[sensorName]
                    if (sensor != null) {
                            val sensorDir = File(sessionDir.rootDir, sensorName.lowercase())
                            sensorDir.mkdirs()
                            sessionMetadata?.let { meta ->
                                val success = sensor.startRecording(sensorDir.absolutePath, meta)
                                if (success) {
                                    activeRecorders[sensorName] = true
                                    sensorsStarted++
                                }
                            }
                            if (!isSimultaneous && sensorsStarted > 0) {
                                delay(100)
                            }
                        }
                    }
                }
                if (sensorsStarted > 0) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = MainRecordingState.RECORDING
                    return@withContext true
                } else {
                    _recordingStateFlow.value = MainRecordingState.ERROR
                    return@withContext false
                }
                _recordingStateFlow.value = MainRecordingState.ERROR
                return@withContext false
            }
        }
    }

    suspend fun stopRecording(): Boolean {
        return withContext(Dispatchers.IO) {
                if (!_isRecording.get()) {
                    return@withContext true
                }
                _recordingStateFlow.value = MainRecordingState.STOPPING
                _isRecording.set(false)
                for ((sensorName, isActive) in activeRecorders) {
                    if (isActive) {
                            sensorRecorders[sensorName]?.stopRecording()
                        }
                    }
                }
                activeRecorders.clear()
                sessionMetadata = null
                _recordingStateFlow.value = MainRecordingState.IDLE
                return@withContext true
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
        return (
            val sessionDir = File(context.filesDir, "sessions")
            sessionDir.freeSpace / (1024.0 * 1024.0 * 1024.0)
            RecordingConstants.FALLBACK_AVAILABLE_SPACE_GB
        }
    }
}