package mpdc4gsr.core.ui.model

enum class SensorType {
    GSR,
    ThermalIR,
    RGBCamera
}

enum class SensorState {
    Disconnected,
    Connecting,
    Connected,
    Streaming,
    Error,
    Simulation
}

enum class UnifiedSystemState {
    Inactive,
    Active,
    Recording,
    Error
}

data class SensorInfo(
    val type: SensorType,
    val state: SensorState,
    val metadata: Map<String, String> = emptyMap()
)

sealed class SystemAction {
    object StartRecording : SystemAction()
    object StopRecording : SystemAction()
    object Synchronize : SystemAction()
}

sealed class GSRAction {
    object Connect : GSRAction()
    object Disconnect : GSRAction()
    object StartStream : GSRAction()
    object StopStream : GSRAction()
    data class ConfigureDevice(val deviceId: String) : GSRAction()
}

sealed class ThermalAction {
    object Connect : ThermalAction()
    object Disconnect : ThermalAction()
    object StartPreview : ThermalAction()
    object StopPreview : ThermalAction()
    object Calibrate : ThermalAction()
    object OpenSettings : ThermalAction()
}

sealed class CameraAction {
    object Connect : CameraAction()
    object Disconnect : CameraAction()
    object StartPreview : CameraAction()
    object StopPreview : CameraAction()
    data class SetResolution(val width: Int, val height: Int) : CameraAction()
}
