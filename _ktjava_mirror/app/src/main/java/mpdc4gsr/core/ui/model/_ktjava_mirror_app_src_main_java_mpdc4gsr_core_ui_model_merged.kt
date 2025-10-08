// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\core\ui\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\core\ui\model\app_src_main_java_mpdc4gsr_core_ui_model_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\core\ui\model' subtree
// Files: 1; Generated 2025-10-07 23:07:38


// ===== app\src\main\java\mpdc4gsr\core\ui\model\SensorModels.kt =====

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