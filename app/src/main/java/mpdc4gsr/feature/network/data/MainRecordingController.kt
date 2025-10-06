package mpdc4gsr.feature.network.data
import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.data.SensorRecorder
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
class MainRecordingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "MainRecordingController"
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
        AppLogger.d(TAG, "Added sensor recorder: $name")
    }
    suspend fun startRecording(
        sessionId: String? = null,
        enabledSensors: List<String> = listOf(RGB_SENSOR_NAME, THERMAL_SENSOR_NAME, GSR_SENSOR_NAME)
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    AppLogger.w(TAG, "Recording already in progress")
                    return@withContext true
                }
                val settings = recordingSettingsRepository.getSettings()
                Log.i(
                    TAG,
                    "Starting recording with settings: simultaneousRecording=${settings.simultaneousRecording}, timestampSync=${settings.timestampSync}"
                )
                AppLogger.i(TAG, "Starting simple recording")
                _recordingStateFlow.value = MainRecordingState.STARTING
                if (getAvailableSpaceGB() < 1.0) {
                    AppLogger.e(TAG, "Insufficient storage space")
                    _recordingStateFlow.value = MainRecordingState.ERROR
                    return@withContext false
                }
                val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
                val sessionDir = sessionDirectoryManager.createSessionDirectory(finalSessionId)
                sessionMetadata = SessionMetadata.createSessionStart(finalSessionId)
                var sensorsStarted = 0
                val isSimultaneous = settings.simultaneousRecording
                AppLogger.i(TAG, "Starting sensors ${if (isSimultaneous) "simultaneously" else "sequentially"}")
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
                                    AppLogger.i(TAG, "Started sensor: $sensorName")
                                }
                            }
                            if (!isSimultaneous && sensorsStarted > 0) {
                                delay(100)
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Failed to start sensor $sensorName", e)
                        }
                    }
                }
                if (sensorsStarted > 0) {
                    _isRecording.set(true)
                    _recordingStateFlow.value = MainRecordingState.RECORDING
                    AppLogger.i(TAG, "Recording started with $sensorsStarted sensors")
                    return@withContext true
                } else {
                    AppLogger.e(TAG, "No sensors started successfully")
                    _recordingStateFlow.value = MainRecordingState.ERROR
                    return@withContext false
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start recording", e)
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
                AppLogger.i(TAG, "Stopping recording")
                _recordingStateFlow.value = MainRecordingState.STOPPING
                _isRecording.set(false)
                for ((sensorName, isActive) in activeRecorders) {
                    if (isActive) {
                        try {
                            sensorRecorders[sensorName]?.stopRecording()
                            AppLogger.i(TAG, "Stopped sensor: $sensorName")
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Error stopping sensor $sensorName", e)
                        }
                    }
                }
                activeRecorders.clear()
                sessionMetadata = null
                _recordingStateFlow.value = MainRecordingState.IDLE
                AppLogger.i(TAG, "Recording stopped successfully")
                return@withContext true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to stop recording", e)
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
            RecordingConstants.FALLBACK_AVAILABLE_SPACE_GB
        }
    }
}