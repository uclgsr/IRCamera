package mpdc4gsr.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.designsystem.components.common.NavigationBreadcrumb
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.components.common.TitleBarAction
import mpdc4gsr.core.designsystem.components.sensors.GSRSensorCard
import mpdc4gsr.core.designsystem.components.sensors.RGBCameraSensorCard
import mpdc4gsr.core.designsystem.components.sensors.ThermalSensorCard
import mpdc4gsr.core.designsystem.components.sensors.UnifiedSensorStatus
import mpdc4gsr.core.designsystem.model.CameraAction
import mpdc4gsr.core.designsystem.model.GSRAction
import mpdc4gsr.core.designsystem.model.SensorInfo
import mpdc4gsr.core.designsystem.model.SensorState
import mpdc4gsr.core.designsystem.model.SensorType
import mpdc4gsr.core.designsystem.model.SystemAction
import mpdc4gsr.core.designsystem.model.ThermalAction
import mpdc4gsr.core.designsystem.model.UnifiedSystemState
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

@Composable
fun UnifiedSensorDashboard(
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onSensorClick: (SensorType) -> Unit = {},
    onCameraSettingsClick: () -> Unit = {},
    onGSRSettingsClick: () -> Unit = {},
    onThermalSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Sensor states - showing disconnected until actual ViewModels are connected
    var gsrState by remember { mutableStateOf(SensorState.Disconnected) }
    var thermalState by remember { mutableStateOf(SensorState.Disconnected) }
    var rgbState by remember { mutableStateOf(SensorState.Disconnected) }
    var unifiedState by remember { mutableStateOf(UnifiedSystemState.Inactive) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFF16131e)),
    ) {
        // Title bar with user-friendly name
        TitleBar(
            title = "Sensor Overview",
            showBackButton = true,
            onBackClick = onBackClick,
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Sensor Settings",
                onClick = onSettingsClick,
            )
        }
        // Breadcrumb navigation for context
        NavigationBreadcrumb(
            currentScreen = "Sensor Overview",
            previousScreen = "Home",
        )
        // Scrollable sensor content
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Unified system status overview
            UnifiedSensorStatus(
                systemState = unifiedState,
                activeSensors =
                    listOf(
                        SensorInfo(SensorType.GSR, gsrState),
                        SensorInfo(SensorType.ThermalIR, thermalState),
                        SensorInfo(SensorType.RGBCamera, rgbState),
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
                },
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
                        is GSRAction.ConfigureDevice -> {
                        }
                    }
                },
                onSettingsClick = onGSRSettingsClick,
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
                        is ThermalAction.Calibrate -> {
                        }

                        is ThermalAction.OpenSettings -> {
                        }
                    }
                },
                onSettingsClick = onThermalSettingsClick,
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
                        is CameraAction.SetResolution -> {
                        }
                    }
                },
                onSettingsClick = onCameraSettingsClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnifiedSensorDashboardPreview() {
    IRCameraTheme {
        UnifiedSensorDashboard()
    }
}

