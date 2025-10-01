package mpdc4gsr.core.ui.model

/**
 * Sensor type enumeration for multi-modal sensor system
 */
enum class SensorType {
    GSR,
    ThermalIR,
    RGBCamera
}

/**
 * Sensor state enumeration representing connection and operational status
 */
enum class SensorState {
    Disconnected,
    Connecting,
    Connected,
    Streaming,
    Error,
    Simulation
}

/**
 * Unified system state for coordinated multi-sensor recording
 */
enum class UnifiedSystemState {
    Inactive,
    Active,
    Recording,
    Error
}

/**
 * Sensor information data class
 */
data class SensorInfo(
    val type: SensorType,
    val state: SensorState,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * System-level actions for unified sensor control
 */
sealed class SystemAction {
    object StartRecording : SystemAction()
    object StopRecording : SystemAction()
    object Synchronize : SystemAction()
}

/**
 * GSR-specific actions
 */
sealed class GSRAction {
    object Connect : GSRAction()
    object Disconnect : GSRAction()
    object StartStream : GSRAction()
    object StopStream : GSRAction()
    data class ConfigureDevice(val deviceId: String) : GSRAction()
}

/**
 * Thermal camera-specific actions
 */
sealed class ThermalAction {
    object Connect : ThermalAction()
    object Disconnect : ThermalAction()
    object StartPreview : ThermalAction()
    object StopPreview : ThermalAction()
    object Calibrate : ThermalAction()
}

/**
 * RGB camera-specific actions
 */
sealed class CameraAction {
    object Connect : CameraAction()
    object Disconnect : CameraAction()
    object StartPreview : CameraAction()
    object StopPreview : CameraAction()
    data class SetResolution(val width: Int, val height: Int) : CameraAction()
}
