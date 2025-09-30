package mpdc4gsr.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.components.TitleBarAction
import mpdc4gsr.compose.components.sensors.GSRSensorCard
import mpdc4gsr.compose.components.sensors.ThermalSensorCard
import mpdc4gsr.compose.components.sensors.RGBCameraSensorCard
import mpdc4gsr.compose.components.sensors.UnifiedSensorStatus
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Unified Sensor Dashboard - Comprehensive view of all sensor modalities
 * Replaces SensorDashboardFragment with modern Compose implementation
 * Includes GSR, Thermal IR, and RGB Camera sensors in a unified interface
 */
@Composable
fun UnifiedSensorDashboard(
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onSensorClick: (SensorType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sample sensor states - will be connected to actual ViewModels
    var gsrState by remember { mutableStateOf(SensorState.Connected) }
    var thermalState by remember { mutableStateOf(SensorState.Streaming) }
    var rgbState by remember { mutableStateOf(SensorState.Connected) }
    var unifiedState by remember { mutableStateOf(UnifiedSystemState.Active) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e)) // Match theme background
    ) {
        // Title bar with dashboard icon and settings
        TitleBar(
            title = "Sensor Dashboard",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Sensor Settings",
                onClick = onSettingsClick
            )
        }

        // Scrollable sensor content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Unified system status overview
            UnifiedSensorStatus(
                systemState = unifiedState,
                activeSensors = listOf(
                    SensorInfo(SensorType.GSR, gsrState),
                    SensorInfo(SensorType.ThermalIR, thermalState),
                    SensorInfo(SensorType.RGBCamera, rgbState)
                ),
                onSystemAction = { action ->
                    when (action) {
                        is SystemAction.StartRecording -> {
                            unifiedState = UnifiedSystemState.Recording
                        }

                        is SystemAction.StopRecording -> {
                            unifiedState = UnifiedSystemState.Active
                        }

                        is SystemAction.Synchronize -> {
                            // Trigger sensor synchronization
                        }
                    }
                }
            )

            // Individual sensor cards
            GSRSensorCard(
                state = gsrState,
                onStateChange = { gsrState = it },
                onClick = { onSensorClick(SensorType.GSR) },
                onAction = { action ->
                    when (action) {
                        is GSRAction.Connect -> gsrState = SensorState.Connecting
                        is GSRAction.Disconnect -> gsrState = SensorState.Disconnected
                        is GSRAction.StartStream -> gsrState = SensorState.Streaming
                        is GSRAction.StopStream -> gsrState = SensorState.Connected
                        is GSRAction.ConfigureDevice -> { /* Handle device configuration */ }
                    }
                }
            )

            ThermalSensorCard(
                state = thermalState,
                onStateChange = { thermalState = it },
                onClick = { onSensorClick(SensorType.ThermalIR) },
                onAction = { action ->
                    when (action) {
                        is ThermalAction.Connect -> thermalState = SensorState.Connecting
                        is ThermalAction.Disconnect -> thermalState = SensorState.Disconnected
                        is ThermalAction.StartPreview -> thermalState = SensorState.Streaming
                        is ThermalAction.StopPreview -> thermalState = SensorState.Connected
                        is ThermalAction.Calibrate -> { /* Handle calibration */ }
                    }
                }
            )

            RGBCameraSensorCard(
                state = rgbState,
                onStateChange = { rgbState = it },
                onClick = { onSensorClick(SensorType.RGBCamera) },
                onAction = { action ->
                    when (action) {
                        is CameraAction.Connect -> rgbState = SensorState.Connecting
                        is CameraAction.Disconnect -> rgbState = SensorState.Disconnected
                        is CameraAction.StartPreview -> rgbState = SensorState.Streaming
                        is CameraAction.StopPreview -> rgbState = SensorState.Connected
                        is CameraAction.SetResolution -> { /* Handle resolution change */ }
                    }
                }
            )
        }
    }
}

/**
 * Sensor type enumeration
 */
enum class SensorType {
    GSR,
    ThermalIR,
    RGBCamera
}

/**
 * Sensor state enumeration
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
 * Unified system state
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
 * System-level actions
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

@Preview(showBackground = true)
@Composable
private fun UnifiedSensorDashboardPreview() {
    IRCameraTheme {
        UnifiedSensorDashboard()
    }
}