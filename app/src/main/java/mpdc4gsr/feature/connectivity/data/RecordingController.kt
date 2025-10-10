package mpdc4gsr.feature.connectivity.data

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.common.logging.StructuredLogger

typealias DataTriggerSource = mpdc4gsr.feature.connectivity.data.TriggerSource
typealias DataSessionState = mpdc4gsr.feature.connectivity.data.SessionState

class RecordingController(
    private val context: Context,
    @Suppress("unused") private val lifecycleOwner: LifecycleOwner? = null,
) {
    enum class TriggerSource {
        LOCAL_UI,
        REMOTE_PC,
        LOCAL_NOTIFICATION,
        CRASH_RECOVERY,
    }

    enum class SessionState {
        IDLE,
        STARTING,
        RECORDING,
        STOPPING,
        STOPPED_COMPLETED,
        STOPPED_FAILED,
        STOPPED_INCOMPLETE,
    }

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingStateFlow: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _sensorStatusFlow =
        MutableStateFlow(
            defaultSensorStatuses(),
        )
    val sensorStatusFlow: StateFlow<List<SensorStatusInfo>> = _sensorStatusFlow.asStateFlow()

    private val _errorFlow = MutableSharedFlow<RecordingError>(extraBufferCapacity = 16)
    val errorFlow: SharedFlow<RecordingError> = _errorFlow.asSharedFlow()

    private var sessionState: SessionState = SessionState.IDLE
    private var lastTriggerSource: TriggerSource = TriggerSource.LOCAL_UI
    private var sessionStartTime: Long = 0L
    private var currentSessionId: String? = null
    private var attachedNetworkServer: NetworkServer? = null

    val isRecording: Boolean
        get() = _recordingState.value == RecordingState.RECORDING

    suspend fun initializeSensors(): Boolean {
        _sensorStatusFlow.emit(defaultSensorStatuses())
        return true
    }

    fun getAvailableSensors(): List<SensorStatusInfo> = _sensorStatusFlow.value

    suspend fun startRecording(
        sessionId: String = generateSessionId(),
        triggerSource: TriggerSource = TriggerSource.LOCAL_UI,
    ): Boolean {
        if (isRecording) {
            return false
        }

        _recordingState.emit(RecordingState.STARTING)
        currentSessionId = sessionId
        sessionStartTime = System.currentTimeMillis()
        lastTriggerSource = triggerSource
        sessionState = SessionState.RECORDING
        _sensorStatusFlow.emit(
            _sensorStatusFlow.value.map { status -> status.copy(isActive = true) },
        )
        _recordingState.emit(RecordingState.RECORDING)
        return true
    }

    suspend fun stopRecording(triggerSource: TriggerSource = TriggerSource.LOCAL_UI): Boolean {
        if (!isRecording) {
            return false
        }

        _recordingState.emit(RecordingState.STOPPING)
        sessionState =
            when (triggerSource) {
                TriggerSource.CRASH_RECOVERY -> SessionState.STOPPED_INCOMPLETE
                else -> SessionState.STOPPED_COMPLETED
            }
        _sensorStatusFlow.emit(
            _sensorStatusFlow.value.map { status -> status.copy(isActive = false) },
        )
        _recordingState.emit(RecordingState.STOPPED)
        return true
    }

    fun generateSessionManifest(): SessionManifest {
        val stopTime =
            if (sessionState == SessionState.STOPPED_COMPLETED ||
                sessionState == SessionState.STOPPED_FAILED ||
                sessionState == SessionState.STOPPED_INCOMPLETE
            ) {
                System.currentTimeMillis()
            } else {
                null
            }
        val duration = stopTime?.let { it - sessionStartTime }

        return SessionManifest(
            sessionId = currentSessionId ?: generateSessionId(),
            startTime = sessionStartTime,
            stopTime = stopTime,
            duration = duration,
            triggerSource = mapTriggerSource(lastTriggerSource),
            sensorActivitySummary =
                _sensorStatusFlow.value.associate { status ->
                    status.sensorId to
                        SensorActivityInfo(
                            sensorName = status.sensorId,
                            wasActive = status.isActive,
                            startedSuccessfully = status.isHealthy,
                            finalStatus = if (status.isActive) "ACTIVE" else "INACTIVE",
                        )
                },
            sessionState = mapSessionState(sessionState),
        )
    }

    fun addSyncMarker(markerType: String, timestampNs: Long) {
        StructuredLogger.logInfo(
            component = "RecordingController",
            event = "sync_marker",
            details =
                mapOf(
                    "marker_type" to markerType,
                    "timestamp_ns" to timestampNs,
                ),
        )
    }

    fun attachNetworkServer(server: NetworkServer) {
        attachedNetworkServer = server
    }

    fun detachNetworkServer(server: NetworkServer? = null) {
        if (server == null || server == attachedNetworkServer) {
            attachedNetworkServer = null
        }
    }

    fun cleanup() {
        _recordingState.value = RecordingState.IDLE
        sessionState = SessionState.IDLE
        currentSessionId = null
        sessionStartTime = 0L
        _sensorStatusFlow.value = defaultSensorStatuses()
    }

    fun getCurrentSessionId(): String? = currentSessionId

    internal fun getAttachedNetworkServer(): NetworkServer? = attachedNetworkServer

    private fun defaultSensorStatuses(): List<SensorStatusInfo> =
        listOf(
            SensorStatusInfo(
                sensorId = "thermal_camera",
                isActive = false,
                isHealthy = true,
                lastSampleTime = 0L,
                samplesRecorded = 0,
                errorCount = 0,
            ),
            SensorStatusInfo(
                sensorId = "rgb_camera",
                isActive = false,
                isHealthy = true,
                lastSampleTime = 0L,
                samplesRecorded = 0,
                errorCount = 0,
            ),
            SensorStatusInfo(
                sensorId = "gsr_sensor",
                isActive = false,
                isHealthy = true,
                lastSampleTime = 0L,
                samplesRecorded = 0,
                errorCount = 0,
            ),
        )

    private fun mapTriggerSource(source: TriggerSource): DataTriggerSource =
        when (source) {
            TriggerSource.REMOTE_PC -> DataTriggerSource.REMOTE_PC
            TriggerSource.LOCAL_NOTIFICATION -> DataTriggerSource.LOCAL_NOTIFICATION
            TriggerSource.CRASH_RECOVERY -> DataTriggerSource.CRASH_RECOVERY
            TriggerSource.LOCAL_UI -> DataTriggerSource.LOCAL_UI
        }

    private fun mapSessionState(state: SessionState): DataSessionState =
        when (state) {
            SessionState.IDLE -> DataSessionState.IDLE
            SessionState.STARTING -> DataSessionState.STARTING
            SessionState.RECORDING -> DataSessionState.RECORDING
            SessionState.STOPPING -> DataSessionState.STOPPING
            SessionState.STOPPED_COMPLETED -> DataSessionState.COMPLETED
            SessionState.STOPPED_FAILED -> DataSessionState.FAILED
            SessionState.STOPPED_INCOMPLETE -> DataSessionState.STOPPED_INCOMPLETE
        }

    private fun generateSessionId(): String = "session_${System.currentTimeMillis()}"
}
