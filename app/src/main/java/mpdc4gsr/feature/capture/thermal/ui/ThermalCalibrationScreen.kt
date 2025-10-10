package mpdc4gsr.feature.capture.thermal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.components.settings.SettingsCard
import mpdc4gsr.core.designsystem.components.settings.SettingsRow
import mpdc4gsr.core.designsystem.components.settings.SettingsToggle
import mpdc4gsr.core.designsystem.theme.IRCameraTheme
import mpdc4gsr.feature.capture.thermal.presentation.CalibrationViewModel

@Composable
fun ThermalCalibrationScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: CalibrationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.calibrationSettings.collectAsStateWithLifecycle()
    val calibrationInfo by viewModel.calibrationInfo.collectAsStateWithLifecycle()
    val progress by viewModel.calibrationProgress.collectAsStateWithLifecycle()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFF16131e)),
    ) {
        TitleBar(
            title = "Calibration",
            showBackButton = true,
            onBackClick = onBackClick,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Thermal Camera Calibration
            SettingsCard(
                title = "Thermal Camera Calibration",
                icon = Icons.Default.Thermostat,
            ) {
                Text(
                    text = "Calibrate temperature readings for accuracy",
                    color = Color.Gray,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startThermalCalibration() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !progress.isRunning,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (progress.isRunning) "Capturing..." else "Start Calibration")
                }
                if (progress.isRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress.captured.toFloat() / progress.target.coerceAtLeast(1).toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Captured ${progress.captured} of ${progress.target}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                    )
                }
                progress.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Calibrated",
                    value = calibrationInfo.thermalLastCalibrated,
                )
                SettingsRow(
                    label = "Calibration Folder",
                    value = calibrationInfo.thermalLastDirectory ?: "Not captured",
                )
                progress.lastSavedPath?.let { path ->
                    SettingsRow(
                        label = "Last Capture",
                        value = File(path).name,
                    )
                }
            }
            // GSR Sensor Calibration
            SettingsCard(
                title = "GSR Sensor Calibration",
                icon = Icons.Default.Sensors,
            ) {
                SettingsToggle(
                    label = "Auto Calibration",
                    description = "Automatically calibrate before each recording",
                    checked = settings.autoCalibration,
                    onCheckedChange = { viewModel.updateAutoCalibration(it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startGSRCalibration() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Calibrated",
                    value = calibrationInfo.gsrLastCalibrated,
                )
            }
            // Camera Alignment
            SettingsCard(
                title = "Camera Alignment",
                icon = Icons.Default.CenterFocusWeak,
            ) {
                Text(
                    text = "Align RGB and thermal cameras for synchronized capture",
                    color = Color.Gray,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startCameraAlignment() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Alignment")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Aligned",
                    value = calibrationInfo.cameraLastAligned,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThermalCalibrationScreenPreview() {
    IRCameraTheme {
        ThermalCalibrationScreen()
    }
}





