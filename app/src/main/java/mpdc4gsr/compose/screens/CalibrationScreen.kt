package mpdc4gsr.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.components.*
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Calibration Screen - System calibration and alignment tools
 */
@Composable
fun CalibrationScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var autoCalibration by remember { mutableStateOf(true) }

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
                    onClick = { /* Start thermal calibration */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Calibrated",
                    value = "3 days ago"
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
                    checked = autoCalibration,
                    onCheckedChange = { autoCalibration = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { /* Start GSR calibration */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Calibration")
                }
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
                    onClick = { /* Start alignment */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Alignment")
                }
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
