package mpdc4gsr.feature.dashboard.presentation

import mpdc4gsr.core.recording.session.SessionInfo
import mpdc4gsr.feature.dashboard.domain.model.GsrDataState
import mpdc4gsr.feature.dashboard.domain.model.SensorState
import mpdc4gsr.feature.dashboard.domain.model.SessionState
import mpdc4gsr.feature.dashboard.domain.repository.GsrConnectionState
import mpdc4gsr.feature.dashboard.domain.repository.NetworkConnectionState
import mpdc4gsr.feature.connectivity.data.NetworkClient

data class MainUiState(
    val isLoading: Boolean = false,
    val currentPage: Int = PAGE_MAIN,
    val gsrConnectionState: GsrConnectionState = GsrConnectionState.DISCONNECTED,
    val gsrBatteryLevel: Int? = null,
    val gsrDataState: GsrDataState = GsrDataState(),
    val networkConnectionState: NetworkConnectionState = NetworkConnectionState.DISCONNECTED,
    val connectedControllerInfo: NetworkClient.ControllerInfo? = null,
    val sessionState: SessionState = SessionState.IDLE,
    val currentSession: SessionInfo? = null,
    val sensorOverview: SensorOverviewState = SensorOverviewState(),
    val isRemoteTriggered: Boolean = false,
    val exposureLocked: Boolean = false,
    val focusLocked: Boolean = false,
    val exposureCompensation: Float = 0f,
) {
    companion object {
        const val PAGE_GALLERY = 0
        const val PAGE_MAIN = 1
        const val PAGE_SETTINGS = 2
        const val PAGE_PROFILE = 3
    }

    val isRecording: Boolean
        get() = sessionState == SessionState.RECORDING
}

data class SensorOverviewState(
    val gsr: SensorState = SensorState(),
    val thermal: SensorState = SensorState(),
    val rgb: SensorState = SensorState(),
)

data class RecordingSessionConfig(
    val sessionId: String? = null,
    val participantId: String? = null,
    val studyName: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

sealed class MainUiAction {
    data class SelectPage(
        val index: Int,
    ) : MainUiAction()

    object BackPressed : MainUiAction()

    object StartGsrConnection : MainUiAction()

    data class PerformGsrAction(
        val action: mpdc4gsr.core.designsystem.model.GSRAction,
    ) : MainUiAction()

    object StartNetworkDiscovery : MainUiAction()

    data class PerformThermalAction(
        val action: mpdc4gsr.core.designsystem.model.ThermalAction,
    ) : MainUiAction()

    data class PerformCameraAction(
        val action: mpdc4gsr.core.designsystem.model.CameraAction,
    ) : MainUiAction()

    data class StartRecording(
        val config: RecordingSessionConfig = RecordingSessionConfig(),
    ) : MainUiAction()

    object StopRecording : MainUiAction()

    data class SetRemoteTriggered(
        val isRemote: Boolean,
    ) : MainUiAction()

    data class LockExposure(
        val locked: Boolean,
    ) : MainUiAction()

    data class LockFocus(
        val locked: Boolean,
    ) : MainUiAction()

    data class SetExposureCompensation(
        val value: Float,
    ) : MainUiAction()

    object ResetCameraControls : MainUiAction()
}

