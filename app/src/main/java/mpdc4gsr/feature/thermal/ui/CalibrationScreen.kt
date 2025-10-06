package mpdc4gsr.feature.thermal.ui
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.thermal.presentation.CalibrationViewModel

@Composable
fun CalibrationScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: CalibrationViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.calibrationSettings.collectAsState()
    val calibrationInfo by viewModel.calibrationInfo.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Calibration",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thermal Camera Calibration
            SettingsCard(
                title = "Thermal Camera Calibration",
                icon = Icons.Default.Thermostat
            ) {
                Text(
                    text = "Calibrate temperature readings for accuracy",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startThermalCalibration() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Calibrated",
                    value = calibrationInfo.thermalLastCalibrated
                )
            }
            // GSR Sensor Calibration
            SettingsCard(
                title = "GSR Sensor Calibration",
                icon = Icons.Default.Sensors
            ) {
                SettingsToggle(
                    label = "Auto Calibration",
                    description = "Automatically calibrate before each recording",
                    checked = settings.autoCalibration,
                    onCheckedChange = { viewModel.updateAutoCalibration(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startGSRCalibration() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Calibrated",
                    value = calibrationInfo.gsrLastCalibrated
                )
            }
            // Camera Alignment
            SettingsCard(
                title = "Camera Alignment",
                icon = Icons.Default.CenterFocusWeak
            ) {
                Text(
                    text = "Align RGB and thermal cameras for synchronized capture",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startCameraAlignment() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Alignment")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Aligned",
                    value = calibrationInfo.cameraLastAligned
                )
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun CalibrationScreenPreview() {
    IRCameraTheme {
        CalibrationScreen()
    }
}
